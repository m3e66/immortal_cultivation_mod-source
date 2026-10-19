package com.example.immortal_cultivation_mod.network;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.item.RestrictionInsightItem;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@GameTestHolder("immortal_cultivation_learning_sessions")
@PrefixGameTestTemplate(false)
public final class LearningSessionGameTests {
    private static final class Fixture implements AutoCloseable {
        final ServerPlayer player;
        final List<ModPayloads.ClientboundOpenLearningGamePayload> opened = new ArrayList<>();
        final EmbeddedChannel channel;
        Fixture(GameTestHelper helper) {
            player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                    new GameProfile(UUID.randomUUID(), "learning-session"), ClientInformation.createDefault());
            var connection = new Connection(PacketFlow.SERVERBOUND);
            channel = new EmbeddedChannel(connection);
            new ServerGamePacketListenerImpl(player.server, connection, player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
                @Override public void send(Packet<?> packet) {
                    if (packet instanceof ClientboundCustomPayloadPacket custom
                            && custom.payload() instanceof ModPayloads.ClientboundOpenLearningGamePayload game) {
                        opened.add(game);
                    }
                }
            };
            BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
            player.setPos(pos.getX(), pos.getY(), pos.getZ());
            player.getInventory().add(new ItemStack(ModItems.RESTRICTION_INSIGHT.get(), 3));
        }
        ModPayloads.ClientboundOpenLearningGamePayload latest() { return opened.getLast(); }
        void complete(long token, int score) { handle("handleCompleteLearningGameOnServer",
                new ModPayloads.ServerboundCompleteLearningGamePayload(token, score)); }
        void cancel(long token) { handle("handleCancelLearningGameOnServer",
                new ModPayloads.ServerboundCancelLearningGamePayload(token)); }
        void handle(String method, Object payload) {
            IPayloadContext context = (IPayloadContext) Proxy.newProxyInstance(
                    IPayloadContext.class.getClassLoader(), new Class<?>[]{IPayloadContext.class},
                    (proxy, call, args) -> {
                        if (call.getName().equals("player")) return player;
                        if (call.getName().equals("enqueueWork")) {
                            ((Runnable) args[0]).run();
                            return CompletableFuture.completedFuture(null);
                        }
                        throw new AssertionError("Unexpected context call: " + call.getName());
                    });
            try {
                var handler = ModPayloads.ModPayloadsHandler.class.getDeclaredMethod(method, payload.getClass(), IPayloadContext.class);
                handler.setAccessible(true);
                handler.invoke(null, payload, context);
            } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
        }
        @Override public void close() {
            ModPayloads.clearPendingLearning(player);
            player.discard();
            channel.close();
        }
    }

    @GameTest(template = "fate_test")
    public static void reopeningNeverBlocksAndStalePacketsCannotCancelOrRewardNewSession(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            helper.assertTrue(ModPayloads.beginRestrictionInsightLearning(fixture.player), "First insight opens");
            var first = fixture.latest();
            helper.assertTrue(ModPayloads.beginRestrictionInsightLearning(fixture.player), "Active or interrupted learning never blocks reopening");
            var latest = fixture.latest();
            helper.assertTrue(first.token() != latest.token(), "Reopening replaces the old authorization token");
            fixture.cancel(first.token());
            fixture.complete(first.token(), first.requiredPoints());
            fixture.complete(latest.token(), latest.requiredPoints() - 1);
            helper.assertTrue(RestrictionInsightItem.hintCount(fixture.player) == 0, "Stale tokens and insufficient scores cannot award hints");
            fixture.complete(latest.token(), latest.requiredPoints());
            helper.assertTrue(RestrictionInsightItem.hintCount(fixture.player) == 1
                            && fixture.player.getInventory().countItem(ModItems.RESTRICTION_INSIGHT.get()) == 2,
                    "Late cancellation leaves new learning valid; completion consumes exactly one insight");
            fixture.complete(latest.token(), latest.requiredPoints());
            helper.assertTrue(RestrictionInsightItem.hintCount(fixture.player) == 1, "Replayed completion cannot award a second hint");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void allSpellLearningSourcesSendCurrentRealmAndReopeningRefreshesIt(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            String firstRealm = CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY;
            var data = ModAttachments.getData(fixture.player).withCultivationLevel(firstRealm).withKnownSpells(List.of());
            ModAttachments.setData(fixture.player, data);
            var spell = ModSpells.all().stream().filter(candidate -> !ModSpells.isInnateKnown(candidate.id(), data)
                    && ModSpells.meetsRequirement(firstRealm, candidate)).findFirst().orElseThrow();
            fixture.handle("handleLearnSpellOnServer", new ModPayloads.ServerboundLearnSpellPayload(spell.id()));
            helper.assertTrue(fixture.latest().playerLevel().equals(firstRealm), "Scroll packet includes authoritative learner realm");
            ModPayloads.beginNpcLearning(fixture.player, -1, "spell", spell.id(), spell.requiredLevel());
            helper.assertTrue(fixture.latest().playerLevel().equals(firstRealm), "NPC packet includes learner realm, not teacher realm");
            helper.assertTrue(ModPayloads.beginDunwuSteleLearning(fixture.player, spell.id())
                            && fixture.latest().playerLevel().equals(firstRealm), "Stele packet includes current learner realm");
            var previous = fixture.latest();
            String nextRealm = CultivationLevels.REALM_YUANYING + CultivationLevels.STAGE_EARLY;
            ModAttachments.setData(fixture.player, ModAttachments.getData(fixture.player).withCultivationLevel(nextRealm));
            ModPayloads.beginNpcLearning(fixture.player, -1, "spell", spell.id(), spell.requiredLevel());
            helper.assertTrue(fixture.latest().playerLevel().equals(nextRealm) && previous.playerLevel().equals(firstRealm),
                    "New session refreshes realm without changing an already-opened puzzle snapshot");

            var lianshen = ModAttachments.getData(fixture.player).withKnownSpells(List.of(ModSpells.LIANSHEN_SHU));
            lianshen = lianshen.withQi(lianshen.maxQi());
            ModAttachments.setData(fixture.player, lianshen);
            try {
                var begin = ModPayloads.ModPayloadsHandler.class.getDeclaredMethod("beginLianshenSpellPuzzle",
                        ServerPlayer.class, ModAttachments.CultivationData.class);
                begin.setAccessible(true);
                helper.assertTrue((boolean) begin.invoke(null, fixture.player, lianshen)
                                && fixture.latest().playerLevel().equals(nextRealm), "Lianshen puzzle uses the same current-realm packet");
            } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void learningPacketCodecPreservesRealmAndExistingSessionFields(GameTestHelper helper) {
        var expected = new ModPayloads.ClientboundOpenLearningGamePayload("spell", ModSpells.FIREBALL,
                5_000, 0.5F, 0.75F, true,
                CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_LATE, Long.MIN_VALUE + 17);
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            ModPayloads.ClientboundOpenLearningGamePayload.STREAM_CODEC.encode(buffer, expected);
            helper.assertTrue(expected.equals(ModPayloads.ClientboundOpenLearningGamePayload.STREAM_CODEC.decode(buffer))
                            && !buffer.isReadable(), "Codec round trip retains learner realm, talent, help and authorization token");
        } finally { buffer.release(); }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void restrictionSteleAndNpcTeachingDoNotBlockOneAnother(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            var data = ModAttachments.getData(fixture.player);
            var spell = ModSpells.all().stream().filter(candidate -> CultivationLevels.getStageIndex(candidate.requiredLevel()) > 0)
                    .filter(candidate -> !ModSpells.isInnateKnown(candidate.id(), data)).findFirst().orElseThrow();
            ModAttachments.setData(fixture.player, data.withCultivationLevel(spell.requiredLevel()).withKnownSpells(List.of()));
            helper.assertTrue(ModPayloads.beginRestrictionInsightLearning(fixture.player), "Insight opens before stele");
            helper.assertTrue(ModPayloads.beginDunwuSteleLearning(fixture.player, spell.id()), "Stele opens while an old insight session exists");
            helper.assertTrue(fixture.latest().targetId().equals(spell.id()), "Stele opens the requested spell");
            helper.assertTrue(ModPayloads.beginRestrictionInsightLearning(fixture.player), "Insight opens while an old stele session exists");
            ModPayloads.beginNpcLearning(fixture.player, -1, "spell", spell.id(), spell.requiredLevel());
            helper.assertTrue(ModPayloads.beginRestrictionInsightLearning(fixture.player), "NPC teaching cannot block an insight item");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void deathRespawnAndLoginClearOldTokensAndAllowImmediateReopening(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper); Fixture clone = new Fixture(helper)) {
            ModPayloads.beginRestrictionInsightLearning(fixture.player);
            var beforeDeath = fixture.latest();
            fixture.player.setHealth(0);
            ServerEvents.onPlayerDeath(new LivingDeathEvent(fixture.player, fixture.player.damageSources().generic()));
            fixture.player.setHealth(fixture.player.getMaxHealth());
            fixture.complete(beforeDeath.token(), beforeDeath.requiredPoints());
            helper.assertTrue(RestrictionInsightItem.hintCount(fixture.player) == 0, "Death invalidates pre-death completion even after health is restored");
            helper.assertTrue(ModPayloads.beginRestrictionInsightLearning(fixture.player), "Learning reopens immediately after death");
            var beforeClone = fixture.latest();
            ModPayloads.beginRestrictionInsightLearning(clone.player);
            var cloneToken = clone.latest();
            ServerEvents.onPlayerClone(new PlayerEvent.Clone(clone.player, fixture.player, true));
            fixture.complete(beforeClone.token(), beforeClone.requiredPoints());
            clone.complete(cloneToken.token(), cloneToken.requiredPoints());
            helper.assertTrue(RestrictionInsightItem.hintCount(fixture.player) == 0
                            && RestrictionInsightItem.hintCount(clone.player) == 0, "Respawn clears original and replacement session tokens");
            helper.assertTrue(ModPayloads.beginRestrictionInsightLearning(clone.player), "Replacement player can reopen learning immediately");
            var beforeLogin = clone.latest();
            ServerEvents.onPlayerLogin(new PlayerEvent.PlayerLoggedInEvent(clone.player));
            clone.complete(beforeLogin.token(), beforeLogin.requiredPoints());
            helper.assertTrue(RestrictionInsightItem.hintCount(clone.player) == 0, "Reconnect invalidates leftover learning tokens");
        }
        helper.succeed();
    }
}
