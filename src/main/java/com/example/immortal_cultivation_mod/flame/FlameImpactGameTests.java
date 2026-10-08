package com.example.immortal_cultivation_mod.flame;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;
import com.example.immortal_cultivation_mod.entity.ModEntities;
import com.example.immortal_cultivation_mod.network.FlameImpactPayload;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ImmortalCultivationMod.MODID)
@PrefixGameTestTemplate(false)
public final class FlameImpactGameTests {
    private FlameImpactGameTests() { }

    @GameTest(template = "fate_test")
    public static void impactExpansionMatchesActualRealmAndPartialChargeRadius(GameTestHelper h) {
        float initial = .9F;
        for (float realmRadius : new float[] {3, 5, 15, 60}) {
            for (float charge : new float[] {.1F, .5F, 1}) {
                float target = Math.max(1, realmRadius * charge);
                h.assertTrue(Math.abs(FlameImpactShape.expansionRadius(0, initial, target) - initial) < .0001,
                        "Impact starts at the projectile's size before expanding");
                h.assertTrue(FlameImpactShape.expansionRadius(1, initial, target) < initial,
                        "The first impact tick compresses the shell before the shockwave");
                h.assertTrue(Math.abs(FlameImpactShape.expansionRadius(FlameImpactShape.EXPAND_END, initial, target)
                                - target) < .0001,
                        "The expanding impact reaches the exact server crater radius, including partial charges");
                h.assertTrue(FlameImpactShape.expansionRadius(FlameImpactShape.REVEAL_END, initial, target) == 0,
                        "The giant shell ends before long-lived crater aftermath begins");
            }
        }
        h.assertTrue(FlameImpactShape.EXPAND_END == 5 && FlameImpactShape.REVEAL_END == 30
                        && FlameImpactShape.AFTERMATH_END == 160 && FlameImpactShape.EXPIRE == 600,
                "Expansion, reveal, aftermath and thirty-second expiry retain their intended timing");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void impactPhasesKeepDistinctFiniteGeometryWithinRenderBudgets(GameTestHelper h) {
        var identities = new HashSet<Long>();
        for (FlameKind kind : FlameKind.values()) {
            if (!kind.special()) continue;
            for (float radius : new float[] {1, 1.5F, 2.5F, 3, 5, 7.5F, 15, 30, 60}) {
                for (int detail : new int[] {1, 2}) {
                    for (float age : new float[] {0, 1, 5, 16, 30, 90, 160, 420, 599, 600}) {
                        var stats = geometry(kind, age, radius, 1234567L, detail);
                        h.assertTrue(stats.valid, "Impact geometry remains finite, local and safely sized: " + kind);
                        h.assertTrue(stats.lines <= FlameImpactShape.MAX_LINES
                                        && stats.cores <= FlameImpactShape.MAX_CORES
                                        && stats.leaves <= FlameImpactShape.MAX_LEAVES,
                                "All impact phases respect the fixed render budget, including sixty-block craters");
                        h.assertTrue(age >= FlameImpactShape.EXPIRE ? stats.visible == 0 : stats.visible > 0,
                                "Every active phase remains visible, then all geometry fades at thirty seconds");
                    }
                }
            }
            var expanded = geometry(kind, 5, 15, 1234567L, 2);
            var aftermath = geometry(kind, 90, 15, 1234567L, 2);
            var repeat = geometry(kind, 90, 15, 1234567L, 2);
            h.assertTrue(expanded.fingerprint != aftermath.fingerprint,
                    "Crater aftermath changes the silhouette instead of keeping a giant stationary fireball");
            h.assertTrue(aftermath.fingerprint == repeat.fingerprint,
                    "The same impact seed and age produce stable geometry rather than frame-to-frame flicker");
            h.assertTrue(identities.add(aftermath.fingerprint),
                    "All eight flame elements retain distinct aftermath shapes and colors");
        }
        h.assertTrue(FlameImpactShape.intensity(0) > FlameImpactShape.intensity(420)
                        && FlameImpactShape.intensity(600) == 0 && FlameImpactShape.intensity(620) == 0,
                "Long-lived aftermath cools and fully expires instead of leaking permanent glow");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void impactPacketPreservesTerrainMaterialAndRespectsTerrainChoices(GameTestHelper h) {
        BlockPos pos = h.absolutePos(new BlockPos(3, 3, 3));
        h.getLevel().setBlockAndUpdate(pos, Blocks.RED_SANDSTONE.defaultBlockState());
        Vec3 center = Vec3.atCenterOf(pos).add(0, .05, 0);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        try (var user = new User(h, false)) {
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
            try {
                for (FlameKind kind : FlameKind.values()) {
                    for (float charge : new float[] {.5F, 1}) {
                        var shot = shot(h, user.player, kind, charge);
                        try {
                            var packet = FlameImpactPayload.prepare(shot, hit, center).orElseThrow();
                            h.assertTrue(packet.radius() == Math.max(1, kind.destructionRadius() * charge)
                                            && packet.charge() == charge && packet.flame().equals(kind.id())
                                            && packet.dimension().equals(h.getLevel().dimension().location())
                                            && packet.center().equals(center) && packet.impactId().equals(shot.getUUID()),
                                    "Impact packet uses the real server flame, charge, dimension, anchor and crater radius");
                            h.assertTrue(packet.terrainDamaged()
                                            && packet.terrainState() == Block.getId(Blocks.RED_SANDSTONE.defaultBlockState()),
                                    "Packet captures the original terrain material before crater removal");
                            FlameImpactPayload.CODEC.encode(buffer, packet);
                            h.assertTrue(packet.equals(FlameImpactPayload.CODEC.decode(buffer)) && buffer.readableBytes() == 0,
                                    "Packet round trip preserves independent effect identity, seed and all terrain metadata");
                            buffer.clear();
                            for (var category : new BlockBreakingSettings.Category[] {
                                    BlockBreakingSettings.Category.FLAME_VESSELS, BlockBreakingSettings.Category.ALL}) {
                                BlockBreakingSettings.setDisabledMask(user.player, category.bit());
                                var harmless = FlameImpactPayload.prepare(shot, hit, center).orElseThrow();
                                h.assertTrue(!harmless.terrainDamaged() && harmless.radius() == shot.blastRadius(),
                                        "Disabled terrain effects show only the actual combat blast, never a fake giant crater");
                            }
                            BlockBreakingSettings.setDisabledMask(user.player, 0);
                        } finally { shot.discard(); }
                    }
                }
                var shot = shot(h, user.player, FlameKind.HEAVENLY, 1);
                try {
                    user.player.getAbilities().mayBuild = false;
                    h.assertTrue(!FlameImpactPayload.prepare(shot, hit, center).orElseThrow().terrainDamaged(),
                            "Non-building players cannot advertise destroyed terrain");
                    user.player.getAbilities().mayBuild = true;
                    var target = new net.minecraft.world.entity.animal.Cow(EntityType.COW, h.getLevel());
                    h.assertTrue(FlameImpactPayload.prepare(shot, new EntityHitResult(target), center).isEmpty(),
                            "Entity hits cannot create block-impact aftermath");
                    shot.setCharge(0);
                    h.assertTrue(FlameImpactPayload.prepare(shot, hit, center).isEmpty(),
                            "Tap shots cannot create charged impact aftermath");
                    shot.setCharge(1);
                    user.player.setHealth(0);
                    h.assertTrue(FlameImpactPayload.prepare(shot, hit, center).isEmpty(),
                            "An invalid caster cannot create an authoritative effect");
                } finally { shot.discard(); }
            } finally { buffer.release(); }
        }
        h.succeed();
    }

    @GameTest(template = "fate_test", batch = "flame_impact_packets")
    public static void acceptedBlockImpactSendsOnceBeforeRemovalAndCancelledImpactSendsNothing(GameTestHelper h) {
        // Keep the real destructive fixture above other simultaneous GameTests.
        BlockPos pos = h.absolutePos(new BlockPos(3, 3, 3)).atY(h.getLevel().getMaxBuildHeight() - 24);
        try (var user = new User(h, true)) {
            user.player.setPos(Vec3.atCenterOf(pos).add(-3, 1, 0));
            h.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            user.watchTerrain = pos;
            var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            var accepted = shot(h, user.player, FlameKind.CRIMSON, 1);
            accepted.onHit(hit);
            h.assertTrue(accepted.isRemoved() && h.getLevel().getBlockState(pos).isAir(),
                    "The gameplay projectile disappears after completing the accepted impact");
            h.assertTrue(user.visuals.size() == 1 && user.originalTerrainAtSend,
                    "Accepted block impact sends exactly one effect before terrain destruction");
            var effect = user.visuals.getFirst();
            h.assertTrue(effect.impactId().equals(accepted.getUUID()) && effect.terrainDamaged()
                            && effect.radius() == 5 && effect.terrainState() == Block.getId(Blocks.STONE.defaultBlockState()),
                    "Independent packet retains the removed shot's identity and original impact material");
            accepted.onHit(hit);
            h.assertTrue(user.visuals.size() == 1, "Reentrant or duplicate callbacks cannot restart the effect");

            h.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            var cancelled = shot(h, user.player, FlameKind.CRIMSON, 1);
            Consumer<ExplosionEvent.Start> reject = event -> {
                if (event.getExplosion().getDirectSourceEntity() == cancelled) event.setCanceled(true);
            };
            NeoForge.EVENT_BUS.addListener(reject);
            try {
                cancelled.onHit(hit);
                h.assertTrue(user.visuals.size() == 1 && h.getLevel().getBlockState(pos).is(Blocks.STONE),
                        "A cancelled explosion has neither destructive aftermath VFX nor terrain damage");
            } finally { NeoForge.EVENT_BUS.unregister(reject); cancelled.discard(); }

            var victim = new net.minecraft.world.entity.animal.Cow(EntityType.COW, h.getLevel());
            victim.setPos(Vec3.atCenterOf(pos).add(0, 2, 0));
            victim.setNoAi(true);
            victim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);
            victim.setHealth(200);
            h.getLevel().addFreshEntity(victim);
            var entityHit = shot(h, user.player, FlameKind.CRIMSON, 1);
            try {
                entityHit.onHit(new EntityHitResult(victim));
                h.assertTrue(entityHit.isRemoved() && victim.getHealth() < 200
                                && SpiritFlameBurnEffect.kind(victim) == FlameKind.CRIMSON && user.visuals.size() == 1,
                        "Successful enemy impacts retain combat behavior without block-only crater aftermath: "
                                + "health=" + victim.getHealth() + " removed=" + entityHit.isRemoved()
                                + " visuals=" + user.visuals.size() + " indexed="
                                + h.getLevel().getEntities(entityHit, victim.getBoundingBox()).contains(victim));
            } finally { entityHit.discard(); victim.discard(); }

            h.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            var tap = shot(h, user.player, FlameKind.CRIMSON, 0);
            try {
                tap.onHit(hit);
                h.assertTrue(tap.isRemoved() && user.visuals.size() == 1,
                        "An ordinary tap shot never creates charged crater aftermath");
            } finally { tap.discard(); h.getLevel().setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState()); }
        } finally { h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void impactPacketsRejectInvalidGeometryAndBoundOversizedVisuals(GameTestHelper h) {
        var dimension = h.getLevel().dimension().location();
        UUID identity = UUID.randomUUID();
        int state = Block.getId(Blocks.STONE.defaultBlockState());
        var bounded = new FlameImpactPayload(dimension, identity, FlameKind.HEAVENLY.id(), Vec3.ZERO,
                10000, 20, 7, state, true);
        h.assertTrue(bounded.radius() == 60 && bounded.charge() == 1,
                "Finite oversized effect inputs cannot create unbounded geometry or overcharge visuals");
        var invalidCenters = new Vec3[] {new Vec3(Double.NaN, 0, 0),
                new Vec3(0, Double.POSITIVE_INFINITY, 0), new Vec3(30_000_001, 0, 0)};
        for (Vec3 center : invalidCenters) {
            boolean rejected = false;
            try {
                new FlameImpactPayload(dimension, identity, FlameKind.CRIMSON.id(), center, 5, 1, 7, state, true);
            } catch (IllegalArgumentException expected) { rejected = true; }
            h.assertTrue(rejected, "Invalid world anchors are rejected before reaching a client renderer");
        }
        boolean unknownFlame = false, unknownTerrain = false, nonfiniteRadius = false;
        try {
            new FlameImpactPayload(dimension, identity, "unknown_fire", Vec3.ZERO, 5, 1, 7, state, true);
        } catch (IllegalArgumentException expected) { unknownFlame = true; }
        try {
            new FlameImpactPayload(dimension, identity, FlameKind.CRIMSON.id(), Vec3.ZERO, 5, 1, 7, -1, true);
        } catch (IllegalArgumentException expected) { unknownTerrain = true; }
        try {
            new FlameImpactPayload(dimension, identity, FlameKind.CRIMSON.id(), Vec3.ZERO, Float.NaN, 1, 7, state, true);
        } catch (IllegalArgumentException expected) { nonfiniteRadius = true; }
        h.assertTrue(unknownFlame && unknownTerrain && nonfiniteRadius,
                "Unknown flame/material and non-finite radius cannot enter the visual state");
        h.succeed();
    }

    private static FlameShotEntity shot(GameTestHelper h, ServerPlayer owner, FlameKind kind, float charge) {
        var shot = new FlameShotEntity(ModEntities.FLAME_SHOT.get(), h.getLevel());
        shot.setOwner(owner);
        shot.setKind(kind);
        shot.setCharge(charge);
        shot.setPos(owner.position().add(0, 1, 0));
        h.getLevel().addFreshEntity(shot);
        return shot;
    }

    private static Geometry geometry(FlameKind kind, float age, float radius, long seed, int detail) {
        var stats = new Geometry(radius);
        FlameImpactShape.emit(kind, age, radius, seed, detail, stats);
        return stats;
    }

    private static final class Geometry implements ChargedFlameShape.Sink {
        private final double bound;
        int lines, cores, leaves, visible;
        boolean valid = true;
        long fingerprint = 7;
        Geometry(float radius) { bound = radius + .001; }
        private void point(Vec3 point) {
            valid &= Double.isFinite(point.lengthSqr()) && point.length() <= bound;
            fingerprint = fingerprint * 31 + point.hashCode();
        }
        private void style(float size, float maximum, int color, float alpha) {
            valid &= Float.isFinite(size) && size > 0 && size <= maximum + .001F
                    && Float.isFinite(alpha) && alpha >= 0 && alpha <= 1;
            if (alpha > 0) visible++;
            fingerprint = fingerprint * 31 + color;
            fingerprint = fingerprint * 31 + Float.floatToIntBits(alpha);
        }
        @Override public void line(Vec3 from, Vec3 to, float width, int color, float alpha) {
            point(from); point(to); style(width, 3, color, alpha);
            valid &= from.length() + width <= bound && to.length() + width <= bound;
            lines++;
        }
        @Override public void core(Vec3 center, float radius, int color, float alpha) {
            point(center); style(radius, FlameImpactShape.MAX_TARGET_RADIUS, color, alpha);
            valid &= center.length() + radius <= bound;
            cores++;
        }
        @Override public void leaf(Vec3 center, Vec3 direction, float size, int color, float alpha) {
            point(center); style(size, 3, color, alpha);
            valid &= Double.isFinite(direction.lengthSqr()) && center.length() + size <= bound;
            leaves++;
        }
    }

    private static final class User implements AutoCloseable {
        final ServerPlayer player;
        final EmbeddedChannel channel;
        final List<FlameImpactPayload> visuals = new ArrayList<>();
        final List<ServerPlayer> onlinePlayers;
        final Map<UUID, ServerPlayer> playersByUuid;
        BlockPos watchTerrain;
        boolean originalTerrainAtSend;

        User(GameTestHelper h, boolean online) {
            player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(),
                    new GameProfile(UUID.randomUUID(), "flame-impact"), ClientInformation.createDefault());
            var connection = new Connection(PacketFlow.SERVERBOUND);
            channel = new EmbeddedChannel(connection);
            new ServerGamePacketListenerImpl(player.server, connection, player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
                @Override public void send(Packet<?> packet) {
                    if (packet instanceof ClientboundCustomPayloadPacket custom
                            && custom.payload() instanceof FlameImpactPayload impact) {
                        visuals.add(impact);
                        originalTerrainAtSend = watchTerrain != null
                                && h.getLevel().getBlockState(watchTerrain).is(Blocks.STONE);
                    }
                }
            };
            player.setGameMode(GameType.SURVIVAL);
            player.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 3, 2))));
            onlinePlayers = online ? playerListField(player.server.getPlayerList(), "players") : null;
            playersByUuid = online ? playerListField(player.server.getPlayerList(), "playersByUUID") : null;
            if (online) {
                onlinePlayers.add(player);
                playersByUuid.put(player.getUUID(), player);
                h.getLevel().addNewPlayer(player);
            }
        }

        @SuppressWarnings("unchecked")
        private static <T> T playerListField(PlayerList list, String name) {
            try {
                var field = PlayerList.class.getDeclaredField(name);
                field.setAccessible(true);
                return (T) field.get(list);
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError("Cannot register the impact packet-recording GameTest player", exception);
            }
        }

        @Override public void close() {
            if (onlinePlayers != null) onlinePlayers.remove(player);
            if (playersByUuid != null) playersByUuid.remove(player.getUUID(), player);
            player.discard();
            channel.close();
        }
    }
}
