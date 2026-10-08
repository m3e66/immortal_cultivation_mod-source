package com.example.immortal_cultivation_mod.event;

import java.util.UUID;
import com.mojang.authlib.GameProfile;
import com.example.immortal_cultivation_mod.attachment.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.*;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@GameTestHolder("immortal_cultivation_mod")
@PrefixGameTestTemplate(false)
public final class JindanFailureGameTests {
    private static final class Player implements AutoCloseable {
        final ServerPlayer player;
        final io.netty.channel.embedded.EmbeddedChannel channel;
        Player(GameTestHelper helper) {
            player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                    new GameProfile(UUID.randomUUID(), "core-failure"), ClientInformation.createDefault());
            var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
            new net.minecraft.server.network.ServerGamePacketListenerImpl(player.server, connection, player,
                    net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet) { }
            };
            var pos = helper.absolutePos(new net.minecraft.core.BlockPos(3, 2, 3)); player.setPos(pos.getX(), pos.getY(), pos.getZ());
        }
        @Override public void close() { JindanTribulation.clear(player); player.discard(); channel.close(); }
    }
    @GameTest(template = "fate_test")
    public static void halfStepRequirementsAndOrderingRemainCorrect(GameTestHelper h) {
        String half = CultivationLevels.REALM_HALF_JINDAN, late = CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_LATE;
        h.assertTrue(CultivationLevels.getTotalQiNeeded(half) == CultivationLevels.getTotalQiNeeded(late) * 2
                && CultivationLevels.getTotalQiNeeded(half) == 160000, "Half-step progress costs double late Foundation Establishment");
        h.assertTrue(!CultivationLevels.isMortal(half) && CultivationLevels.getRealmIndex(half) == 2
                && CultivationLevels.getStageIndex(half) < CultivationLevels.getStageIndex(CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY),
                "Half-step remains below full Golden Core, never falls back to mortal");
        h.assertTrue(CultivationLevels.getNextStage(half).equals(CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY)
                && CultivationLevels.getNextStage(late).equals(CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY),
                "Only failed tribulation grants half-step; normal stage progression is unchanged");
        h.assertTrue(CultivationLevels.getLevelDef(half).maxHp() == CultivationLevels.getLevelDef(late).maxHp(), "Half-step retains Foundation Establishment stats");
        h.succeed();
    }
    @GameTest(template = "fate_test")
    public static void realTribulationDeathCreatesPersistentCrackedCore(GameTestHelper h) {
        try (var p = new Player(h); var clone = new Player(h)) {
            var before = ModAttachments.getData(p.player).withCultivationLevel(CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY)
                    .withCultivationProgress(12345).withQi(999).withMeditating(true).withAgePenalty(290);
            ModAttachments.setData(p.player, before); ServerEvents.setJindanQuality(p.player, "jipin");
            JindanTribulation.start(p.player, "jipin");
            p.player.setHealth(0); p.player.die(h.getLevel().damageSources().lightningBolt());
            var after = ModAttachments.getData(p.player);
            h.assertTrue(CultivationLevels.REALM_HALF_JINDAN.equals(after.cultivationLevel()) && after.cultivationProgress() == 0
                    && !after.isMeditating() && !JindanTribulation.isActive(p.player),
                    "Actual tribulation death applies half-step failure before respawn copy");
            h.assertTrue(after.agePenalty() < CultivationLevels.getLevelDef(after.cultivationLevel()).maxAge(),
                    "Downgrade cannot immediately trigger lifespan reset and erase half-step failure");
            h.assertTrue(ServerEvents.getJindanQuality(p.player).equals("liedan"), "Tribulation death changes quality to Cracked Core");
            var serialized = ModAttachments.CultivationData.CODEC.encodeStart(NbtOps.INSTANCE, after).getOrThrow();
            var restored = ModAttachments.CultivationData.CODEC.parse(NbtOps.INSTANCE, serialized).getOrThrow();
            ModAttachments.setData(clone.player, restored);
            ServerEvents.onPlayerClone(new PlayerEvent.Clone(clone.player, p.player, true));
            h.assertTrue(CultivationLevels.REALM_HALF_JINDAN.equals(ModAttachments.getData(clone.player).cultivationLevel())
                    && ServerEvents.getJindanQuality(clone.player).equals("liedan"), "Saved attachment and cloned persistent data retain half-step and cracked quality");
        }
        h.succeed();
    }
    @GameTest(template = "fate_test")
    public static void protectedDeathAndOrdinaryDeathDoNotCreateHalfStep(GameTestHelper h) {
        try (var p = new Player(h)) {
            String golden = CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY;
            ModAttachments.setData(p.player, ModAttachments.getData(p.player).withCultivationLevel(golden));
            ServerEvents.setJindanQuality(p.player, "shangpin"); JindanTribulation.start(p.player, "shangpin");
            var event = new LivingDeathEvent(p.player, h.getLevel().damageSources().magic()); event.setCanceled(true);
            ServerEvents.onPlayerDeath(event);
            h.assertTrue(JindanTribulation.isActive(p.player) && golden.equals(ModAttachments.getData(p.player).cultivationLevel()),
                    "Canceled death leaves active trial and realm intact");
            JindanTribulation.clear(p.player);
            h.assertTrue(!JindanTribulation.failOnDeath(p.player) && golden.equals(ModAttachments.getData(p.player).cultivationLevel())
                    && ServerEvents.getJindanQuality(p.player).equals("shangpin"), "Death outside tribulation does not downgrade Golden Core");
        }
        h.succeed();
    }
    @GameTest(template = "fate_test")
    public static void halfStepCanRetryOnlyAfterDoubleRequirement(GameTestHelper h) {
        try (var p = new Player(h)) {
            var data = ModAttachments.getData(p.player).withCultivationLevel(CultivationLevels.REALM_HALF_JINDAN)
                    .withQi(1_000_000).withCultivationProgress(159999).withActiveCultivationMethod(com.example.immortal_cultivation_mod.attachment.CultivationMethods.POKONG_JUE);
            ModAttachments.setData(p.player, data); ServerEvents.setJindanQuality(p.player, "liedan");
            ServerEvents.tryBreakthrough(p.player);
            h.assertTrue(!p.player.getPersistentData().getBoolean("ImmortalCultivationPendingJindanBreakthrough"), "Insufficient half-step progress cannot begin retry");
            ModAttachments.setData(p.player, data.withCultivationProgress(160000)); ServerEvents.tryBreakthrough(p.player);
            h.assertTrue(p.player.getPersistentData().getBoolean("ImmortalCultivationPendingJindanBreakthrough"), "Full double requirement opens Golden Core challenge again");
            ServerEvents.completeJindanBreakthrough(p.player, 1F, 0, 1);
            h.assertTrue(JindanTribulation.isActive(p.player) && (CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY)
                    .equals(ModAttachments.getData(p.player).cultivationLevel()), "Half-step retry starts a fresh Golden Core tribulation");
            var current = ModAttachments.getData(p.player); ServerEvents.tryBreakthrough(p.player);
            h.assertTrue(ModAttachments.getData(p.player).equals(current), "Active tribulation cannot be bypassed by further breakthrough");
        }
        h.succeed();
    }
}
