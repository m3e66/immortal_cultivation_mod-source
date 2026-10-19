package com.example.immortal_cultivation_mod.spiritual;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.magicritual.block.ModBlocks;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_sense_performance")
@PrefixGameTestTemplate(false)
public final class SpiritualSensePerformanceGameTests {
    private static final class Fixture implements AutoCloseable {
        final ServerLevel level;
        final ServerPlayer player;
        final EmbeddedChannel channel;
        final Map<BlockPos, BlockState> changed = new LinkedHashMap<>();

        Fixture(GameTestHelper helper) {
            level = helper.getLevel();
            player = new ServerPlayer(level.getServer(), level,
                    new GameProfile(UUID.randomUUID(), "sense-test"), ClientInformation.createDefault());
            channel = new EmbeddedChannel(new Connection(PacketFlow.SERVERBOUND));
            var connection = (Connection) channel.pipeline().last();
            new ServerGamePacketListenerImpl(player.server, connection, player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
                @Override public void send(Packet<?> packet) {}
            };
            player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(3, 2, 3))));
            player.tickCount = 1;
        }

        void block(BlockPos pos, BlockState state) {
            changed.putIfAbsent(pos, level.getBlockState(pos));
            level.setBlock(pos, state, 3);
        }

        @Override public void close() {
            player.setServerLevel(level);
            changed.forEach((pos, state) -> level.setBlock(pos, state, 3));
            ServerEvents.onPlayerLogout(new PlayerEvent.PlayerLoggedOutEvent(player));
            player.discard();
            channel.close();
        }
    }

    @GameTest(template = "fate_test")
    public static void ambientScanKeepsCubicBoundariesAndInvalidatesMovedOrDimensionCache(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            BlockPos center = fixture.player.blockPosition();
            int before = ServerEvents.refreshAmbientQi(fixture.player).spiritVeinCenters();
            var vein = ModBlocks.SPIRIT_VEIN_CENTER.get().defaultBlockState();
            fixture.block(center.offset(0, 1, 0), vein);
            fixture.block(center.offset(48, 48, 48), vein);
            fixture.block(center.offset(49, 0, 0), vein);
            fixture.block(center.offset(0, 49, 0), vein);
            fixture.block(center.offset(0, 0, 49), vein);
            fixture.block(center.offset(1, 0, 0), Blocks.CHEST.defaultBlockState());
            var result = ServerEvents.refreshAmbientQi(fixture.player);
            helper.assertTrue(result.spiritVeinCenters() == before + 2 && result.value() == (before + 2) * 25,
                    "Scan counts exact inclusive cube corners; excludes outside axes and unrelated block entities");
            fixture.player.setPos(fixture.player.position().add(160, 0, 0));
            helper.assertTrue(ServerEvents.ambientQiValue(fixture.player) == 0,
                    "Moving before the five-second expiry cannot reuse the old location's Qi");
            fixture.player.setPos(Vec3.atCenterOf(center));
            helper.assertTrue(ServerEvents.ambientQiValue(fixture.player) == result.value(), "Returning refreshes Qi");
            var destination = fixture.level.getServer().getLevel(Level.NETHER);
            helper.assertTrue(destination != null, "Dimension fixture available");
            fixture.player.setServerLevel(destination);
            helper.assertTrue(ServerEvents.ambientQiValue(fixture.player) == 0,
                    "Cross-dimension movement cannot reuse overworld Qi");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void fogRevealFindsLoadedEntitiesAtInclusiveBoundary(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            BlockPos center = fixture.player.blockPosition();
            int before = ServerEvents.startFogMirrorReveal(fixture.player);
            var fog = ModBlocks.FOG_ESCAPE_BLOCK.get().defaultBlockState();
            fixture.block(center.offset(0, 1, 0), fog);
            fixture.block(center.offset(32, 32, 32), fog);
            fixture.block(center.offset(33, 0, 0), fog);
            fixture.block(center.offset(0, 33, 0), fog);
            fixture.block(center.offset(1, 0, 0), Blocks.CHEST.defaultBlockState());
            helper.assertTrue(ServerEvents.startFogMirrorReveal(fixture.player) == before + 2,
                    "Fog query preserves radius and ignores unrelated or outside block entities");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void interferenceRayHitsLoadedWallsWithoutLoadingDistantTerrain(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            BlockPos wall = new BlockPos(fixture.player.blockPosition().getX() + 3, 200,
                    fixture.player.blockPosition().getZ());
            fixture.block(wall, Blocks.STONE.defaultBlockState());
            Vec3 start = Vec3.atCenterOf(wall.offset(-3, 0, 0));
            var view = new LoadedSightView(fixture.level);
            var collision = view.clip(new ClipContext(start, start.add(8, 0, 0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, fixture.player));
            helper.assertTrue(collision.getType() == HitResult.Type.BLOCK && collision.getBlockPos().equals(wall),
                    "Loaded wall still blocks vision");
            Vec3 emptyStart = start.add(0, 10, 0);
            Vec3 target = emptyStart.add(4096, 0, 0);
            var source = fixture.level.getChunkSource();
            int targetX = ((int) target.x) >> 4;
            int targetZ = ((int) target.z) >> 4;
            helper.assertTrue(source.getChunkNow(targetX, targetZ) == null, "Ray ends in unloaded terrain");
            int before = source.getLoadedChunksCount();
            var miss = new LoadedSightView(fixture.level).clip(new ClipContext(emptyStart, target,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, fixture.player));
            helper.assertTrue(miss.getType() == HitResult.Type.MISS
                            && source.getLoadedChunksCount() == before
                            && source.getChunkNow(targetX, targetZ) == null,
                    "Long sense ray never generates or waits for terrain");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void spiritSightRefreshesOncePerSecondAndStillChargesAndStops(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            var player = fixture.player;
            ModAttachments.setData(player, ModAttachments.getData(player).withQi(100));
            ServerEvents.toggleSpiritSight(player);
            helper.assertTrue(ModAttachments.getData(player).qi() == 95, "Activation still costs five Qi");
            for (int tick = 1; tick < 20; tick++) {
                player.tickCount = tick;
                player.getEffect(MobEffects.NIGHT_VISION).tick(player, () -> {});
                player.getEffect(ModEffects.SPIRIT_SIGHT).tick(player, () -> {});
                sightTick(player);
            }
            helper.assertTrue(player.getEffect(MobEffects.NIGHT_VISION).getDuration() == 241
                            && player.getEffect(ModEffects.SPIRIT_SIGHT).getDuration() == 241
                            && ModAttachments.getData(player).qi() == 95,
                    "Non-charge ticks do not rebuild buffs; night vision stays above flicker threshold");
            player.tickCount = 20;
            sightTick(player);
            helper.assertTrue(player.getEffect(MobEffects.NIGHT_VISION).getDuration() == 260
                            && ModAttachments.getData(player).qi() == 90,
                    "One-second tick charges five Qi and refreshes buffs");
            ModAttachments.setData(player, ModAttachments.getData(player).withQi(0));
            player.tickCount = 40;
            sightTick(player);
            helper.assertTrue(!player.hasEffect(MobEffects.NIGHT_VISION) && !player.hasEffect(ModEffects.SPIRIT_SIGHT),
                    "No Qi ends spirit sight instead of refreshing buffs for free");
        }
        helper.succeed();
    }

    private static void sightTick(ServerPlayer player) {
        try {
            var method = ServerEvents.class.getDeclaredMethod("handleSpiritSightTick", ServerPlayer.class,
                    ModAttachments.CultivationData.class);
            method.setAccessible(true);
            method.invoke(null, player, ModAttachments.getData(player));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
