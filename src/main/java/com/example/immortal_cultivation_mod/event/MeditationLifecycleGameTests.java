package com.example.immortal_cultivation_mod.event;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_meditation_lifecycle")
@PrefixGameTestTemplate(false)
public final class MeditationLifecycleGameTests {
    private static final class Fixture implements AutoCloseable {
        final ServerPlayer player;
        final EmbeddedChannel channel;

        Fixture(GameTestHelper helper, UUID id) {
            player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                    new GameProfile(id, "meditation-test"), ClientInformation.createDefault());
            var connection = new Connection(PacketFlow.SERVERBOUND);
            channel = new EmbeddedChannel(connection);
            new ServerGamePacketListenerImpl(player.server, connection, player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
                @Override public void send(Packet<?> packet) {}
            };
            BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
            player.setPos(pos.getX(), pos.getY(), pos.getZ());
            player.tickCount = 1;
            player.setOnGround(true);
        }

        void meditate() { ServerEvents.handleToggleMeditate(player); }
        void tick() { ServerEvents.onPlayerTickPre(new PlayerTickEvent.Pre(player)); }

        @Override public void close() {
            anchors().remove(player.getUUID());
            player.discard();
            channel.close();
        }
    }

    private static Map<?, ?> anchors() {
        try {
            var field = ServerEvents.class.getDeclaredField("MEDITATION_ANCHORS");
            field.setAccessible(true);
            return (Map<?, ?>) field.get(null);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    @GameTest(template = "fate_test")
    public static void deathStopsMeditationAndDeadTicksCannotRecreatePositionLock(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper, UUID.randomUUID())) {
            fixture.meditate();
            fixture.player.setHealth(0);
            ServerEvents.onPlayerDeath(new LivingDeathEvent(fixture.player, fixture.player.damageSources().generic()));
            fixture.tick();
            helper.assertTrue(!ModAttachments.getData(fixture.player).isMeditating()
                            && !anchors().containsKey(fixture.player.getUUID())
                            && fixture.player.getPose() == Pose.STANDING,
                    "Death ends meditation; death-screen ticks cannot re-anchor the player");
            // A stale flag from another lifecycle handler must not re-enable dead-player locking.
            ModAttachments.setData(fixture.player, ModAttachments.getData(fixture.player).withMeditating(true));
            fixture.tick();
            helper.assertTrue(!ModAttachments.getData(fixture.player).isMeditating()
                            && !anchors().containsKey(fixture.player.getUUID()),
                    "Dead-player tick rejects even a stale meditation flag");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void copiedMeditationCannotPullRespawnedPlayerBackToOldPosition(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        try (Fixture original = new Fixture(helper, id); Fixture replacement = new Fixture(helper, id)) {
            original.meditate();
            var before = ModAttachments.getData(original.player).withCultivationProgress(123).withQi(40);
            ModAttachments.setData(replacement.player, before);
            Vec3 spawn = original.player.position().add(12, 0, 6);
            replacement.player.setPos(spawn);
            replacement.player.setPose(Pose.SITTING);
            ServerEvents.onPlayerClone(new PlayerEvent.Clone(replacement.player, original.player, true));
            replacement.tick();
            var after = ModAttachments.getData(replacement.player);
            helper.assertTrue(!after.isMeditating() && after.cultivationProgress() == 123 && after.qi() == 40
                            && replacement.player.getPose() != Pose.SITTING
                            && replacement.player.position().distanceToSqr(spawn) < 0.0001,
                    "Death clone preserves cultivation but clears meditation and the shared UUID's old anchor");
            // Respawn also handles stale meditation if attachment copying bypassed clone cleanup.
            replacement.meditate();
            Vec3 moved = spawn.add(6, 0, 0);
            replacement.player.setPos(moved);
            ServerEvents.onPlayerRespawn(new PlayerEvent.PlayerRespawnEvent(replacement.player, false));
            replacement.tick();
            helper.assertTrue(!ModAttachments.getData(replacement.player).isMeditating()
                            && replacement.player.position().distanceToSqr(moved) < 0.0001
                            && !anchors().containsKey(id),
                    "Respawn clears stale state before the first living-player tick can teleport");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void repeatedDamageKeepsMeditationActive(GameTestHelper helper) throws ReflectiveOperationException {
        try (Fixture fixture = new Fixture(helper, UUID.randomUUID())) {
            var spawnImmunity = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            spawnImmunity.setAccessible(true);
            spawnImmunity.setInt(fixture.player, 0);
            fixture.meditate();
            for (int hit = 0; hit < 3; hit++) {
                fixture.player.invulnerableTime = 0;
                float before = fixture.player.getHealth();
                fixture.player.hurt(fixture.player.damageSources().generic(), 1);
                helper.assertTrue(fixture.player.getHealth() < before, "Each hit must deal real damage");
                fixture.tick();
                helper.assertTrue(ModAttachments.getData(fixture.player).isMeditating()
                                && fixture.player.getPose() == Pose.SITTING
                                && anchors().containsKey(fixture.player.getUUID()),
                        "Repeated damage keeps the meditation session, pose and anchor active");
            }
            fixture.meditate();
            helper.assertTrue(!ModAttachments.getData(fixture.player).isMeditating(),
                    "Player can still stop meditation manually after taking damage");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void livingMeditationStillResistsKnockbackAndCanRestartAfterRespawn(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper, UUID.randomUUID())) {
            ServerEvents.onPlayerRespawn(new PlayerEvent.PlayerRespawnEvent(fixture.player, false));
            fixture.meditate();
            Vec3 anchor = fixture.player.position();
            fixture.player.knockback(1.0, 1.0, 0.0);
            fixture.player.setPos(anchor.add(0.5, 0, 0));
            fixture.tick();
            helper.assertTrue(ModAttachments.getData(fixture.player).isMeditating()
                            && fixture.player.getDeltaMovement().equals(Vec3.ZERO)
                            && fixture.player.position().distanceToSqr(anchor) < 0.0001
                            && fixture.player.getPose() == Pose.SITTING,
                    "Living meditation keeps its position and knockback protection; respawn permits a fresh session");
        }
        helper.succeed();
    }
}
