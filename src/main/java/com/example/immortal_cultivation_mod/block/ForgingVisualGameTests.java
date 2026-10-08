package com.example.immortal_cultivation_mod.block;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.item.ForgingSystem;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.network.ForgingVisualPayload;
import com.example.immortal_cultivation_mod.screen.DingMenu;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_mod")
@PrefixGameTestTemplate(false)
public final class ForgingVisualGameTests {
    private static final BlockPos FORGE_POS = new BlockPos(3, 2, 3);
    private static final BlockPos OTHER_FORGE_POS = new BlockPos(6, 2, 3);

    @GameTest(template = "fate_test")
    public static void closingStopsPoseImmediatelyWhileBatchContinuesAndReopeningResumes(GameTestHelper helper) {
        var ding = forge(helper, FORGE_POS, 95);
        try (var forger = new Forger(helper)) {
            var menu = open(helper, forger, ding);
            menu.startForging(forger.player);
            helper.assertTrue(menu.forging() && ding.data(2) == 0,
                    "A real paid batch starts with its owner's forge menu open");
            assertLastVisual(helper, forger, ding, true, "Starting the open forge sends an active pose");
            int progress = ding.data(2);
            forger.visuals.clear();

            forger.player.closeContainer();
            assertLastVisual(helper, forger, ding, false, "Closing the menu immediately sends the pose stop packet");
            helper.assertTrue(forger.player.containerMenu == forger.player.inventoryMenu
                            && ding.data(4) == 1 && ding.data(2) == progress,
                    "Native menu close keeps the existing batch and its progress");
            forger.visuals.clear();
            tick(helper, ding, 30);
            helper.assertTrue(ding.data(4) == 1 && ding.data(2) == progress + 30
                            && ding.getItem(DingBlockEntity.RESULT_SLOT).isEmpty(),
                    "Closed-menu forging continues melting the same batch");
            helper.assertTrue(forger.visuals.stream().noneMatch(ForgingVisualPayload::active),
                    "Further forge ticks cannot restart the closed menu's pose");
            helper.assertTrue(!ModAttachments.getData(forger.player).isMeditating(),
                    "Forge animation does not enable actual cultivation meditation");

            var reopened = open(helper, forger, ding);
            helper.assertTrue(reopened.forging() && reopened.progress() == progress + 30,
                    "Reopening exposes the original running batch rather than starting a replacement");
            tick(helper, ding, 1);
            assertLastVisual(helper, forger, ding, true, "Reopening the same running forge resumes its pose");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void onlyOwnersExactForgeMenuActivatesOrClearsItsPose(GameTestHelper helper) {
        var ding = forge(helper, FORGE_POS, 95);
        var other = forge(helper, OTHER_FORGE_POS, 95);
        try (var forger = new Forger(helper); var spectator = new Forger(helper)) {
            var unrelated = open(helper, forger, other);
            helper.assertTrue(unrelated.usesForge(other) && !unrelated.usesForge(ding),
                    "An actual forge menu identifies its exact block entity");
            ding.startForging(forger.player);
            tick(helper, ding, 1);
            helper.assertTrue(ding.data(4) == 1 && forger.visuals.isEmpty(),
                    "Opening another nearby forge cannot animate the running batch's owner");

            open(helper, forger, ding);
            tick(helper, ding, 1);
            assertLastVisual(helper, forger, ding, true, "The matching owner menu enables its running batch pose");
            forger.visuals.clear();
            open(helper, spectator, ding);
            spectator.player.closeContainer();
            helper.assertTrue(forger.visuals.isEmpty(),
                    "Another viewer closing the same forge cannot clear the owner's pose");

            open(helper, forger, other);
            assertLastVisual(helper, forger, ding, false, "Switching menus closes and clears the original forge pose");
            forger.visuals.clear();
            tick(helper, ding, 12);
            helper.assertTrue(ding.data(4) == 1 && forger.visuals.stream().noneMatch(ForgingVisualPayload::active),
                    "The other forge's open menu cannot restart the original batch pose");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void realCompletionAndFireInterruptionStopPoseBeforeFurtherTicks(GameTestHelper helper) {
        try (var forger = new Forger(helper)) {
            var completed = forge(helper, FORGE_POS, 100);
            var menu = open(helper, forger, completed);
            menu.startForging(forger.player);
            assertLastVisual(helper, forger, completed, true, "Successful batch begins with its forge pose");
            forger.visuals.clear();
            tick(helper, completed, menu.maxProgress());
            helper.assertTrue(!menu.forging() && completed.getItem(DingBlockEntity.RESULT_SLOT).is(ModItems.CRIMSON_FLAME_SEED.get()),
                    "Guaranteed-purity ingredients complete through real forge ticks");
            assertLastVisual(helper, forger, completed, false, "Completing the batch stops the forge pose");
            forger.visuals.clear();
            tick(helper, completed, 12);
            helper.assertTrue(forger.visuals.isEmpty(), "Completed forging cannot refresh its old pose");

            var interrupted = forge(helper, OTHER_FORGE_POS, 95);
            open(helper, forger, interrupted).startForging(forger.player);
            assertLastVisual(helper, forger, interrupted, true, "Second actual batch begins with its own pose");
            forger.visuals.clear();
            helper.setBlock(OTHER_FORGE_POS.below(), Blocks.AIR);
            tick(helper, interrupted, 1);
            helper.assertTrue(interrupted.data(4) == 0 && interrupted.getItem(DingBlockEntity.RESULT_SLOT).is(ModItems.ASHES.get()),
                    "Removing the real fire ends the batch through the interruption path");
            assertLastVisual(helper, forger, interrupted, false, "Fire interruption immediately stops its pose");
            forger.visuals.clear();
            tick(helper, interrupted, 12);
            helper.assertTrue(forger.visuals.isEmpty(), "Failed forging cannot refresh its old pose");
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void forgeCloseAndInterruptionPreserveExplicitCultivationMeditation(GameTestHelper helper) {
        var ding = forge(helper, FORGE_POS, 95);
        try (var forger = new Forger(helper)) {
            ModAttachments.setData(forger.player, ModAttachments.getData(forger.player)
                    .withMeditating(true).withCultivationProgress(12345));
            var before = ModAttachments.getData(forger.player);
            open(helper, forger, ding).startForging(forger.player);
            assertLastVisual(helper, forger, ding, true, "Forge pose also works for an explicitly meditating player");
            forger.visuals.clear();
            forger.player.closeContainer();
            assertLastVisual(helper, forger, ding, false, "Forge menu close still stops only its own visual");
            forger.visuals.clear();
            tick(helper, ding, 12);
            helper.assertTrue(forger.visuals.stream().noneMatch(ForgingVisualPayload::active),
                    "Actual meditation does not keep the closed forge's pose packet active");
            helper.setBlock(FORGE_POS.below(), Blocks.AIR);
            tick(helper, ding, 1);
            var after = ModAttachments.getData(forger.player);
            helper.assertTrue(ding.data(4) == 0 && after.isMeditating()
                            && after.cultivationProgress() == before.cultivationProgress()
                            && after.activeCultivationMethod().equals(before.activeCultivationMethod()),
                    "Closing and interrupting the forge preserve the player's explicit meditation and cultivation state");
        }
        helper.succeed();
    }

    private static DingMenu open(GameTestHelper helper, Forger forger, DingBlockEntity ding) {
        if (ding.hasLid()) ding.setLidOpen(true);
        helper.assertTrue(forger.player.openMenu(ding).isPresent(), "Open the genuine server-side Ding menu");
        helper.assertTrue(forger.player.containerMenu instanceof DingMenu, "The player owns a real Ding menu");
        return (DingMenu) forger.player.containerMenu;
    }

    private static void tick(GameTestHelper helper, DingBlockEntity ding, int ticks) {
        for (int i = 0; i < ticks; i++) {
            DingBlockEntity.serverTick(helper.getLevel(), ding.getBlockPos(), ding.getBlockState(), ding);
        }
    }

    private static void assertLastVisual(GameTestHelper helper, Forger forger, DingBlockEntity ding,
            boolean active, String message) {
        helper.assertTrue(!forger.visuals.isEmpty(), message + ": packet was delivered to the actual player");
        var payload = forger.visuals.getLast();
        helper.assertTrue(payload.active() == active && payload.player().equals(forger.player.getUUID())
                        && payload.dimension().equals(helper.getLevel().dimension().location())
                        && payload.forgeDimension().equals(helper.getLevel().dimension().location())
                        && payload.forge().equals(ding.getBlockPos()),
                message + ": packet identifies the correct player, world, and forge");
    }

    private static DingBlockEntity forge(GameTestHelper helper, BlockPos position, int purity) {
        helper.setBlock(position.below().below(), Blocks.STONE);
        helper.setBlock(position.below(), Blocks.CAMPFIRE);
        helper.setBlock(position, ModBlocks.QINGTONG_YINHUO_DING.get());
        var ding = (DingBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(position));
        ding.setItem(DingBlockEntity.YUJIAN_SLOT, new ItemStack(ModItems.CRIMSON_FLAME_SEED_YUJIAN.get()));
        var recipe = ForgingSystem.recipe(ding.getItem(DingBlockEntity.YUJIAN_SLOT));
        var inputs = new ArrayList<ItemStack>();
        recipe.required().forEach((item, count) -> inputs.add(new ItemStack(item, count)));
        inputs.add(new ItemStack(com.example.immortal_cultivation_mod.magicritual.item.ModItems.HIGH_SPIRIT_STONE.get(),
                recipe.spiritStoneCount()));
        for (int i = 0; i < inputs.size(); i++) {
            var data = new CompoundTag();
            data.putInt(ForgingSystem.TAG_PURITY, purity);
            inputs.get(i).set(DataComponents.CUSTOM_DATA, CustomData.of(data));
            ding.setItem(DingBlockEntity.MATERIAL_START + i, inputs.get(i));
        }
        return ding;
    }

    private static final class Forger implements AutoCloseable {
        final ServerPlayer player;
        final EmbeddedChannel channel;
        final List<ForgingVisualPayload> visuals = new ArrayList<>();
        final List<ServerPlayer> onlinePlayers;
        final Map<UUID, ServerPlayer> playersByUuid;

        Forger(GameTestHelper helper) {
            player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                    new GameProfile(UUID.randomUUID(), "forge-visual"), ClientInformation.createDefault());
            var connection = new Connection(PacketFlow.SERVERBOUND);
            channel = new EmbeddedChannel(connection);
            new ServerGamePacketListenerImpl(player.server, connection, player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
                @Override
                public void send(Packet<?> packet) {
                    if (packet instanceof ClientboundCustomPayloadPacket custom
                            && custom.payload() instanceof ForgingVisualPayload visual
                            && visual.player().equals(player.getUUID())) visuals.add(visual);
                }
            };
            var position = helper.absolutePos(FORGE_POS);
            player.setPos(position.getX() + .5, position.getY() + .5, position.getZ() + .5);
            ModAttachments.setData(player, ModAttachments.getData(player).withQi(20000));
            onlinePlayers = playerListField(player.server.getPlayerList(), "players");
            playersByUuid = playerListField(player.server.getPlayerList(), "playersByUUID");
            onlinePlayers.add(player);
            playersByUuid.put(player.getUUID(), player);
            // Register the real entity tracker so tracking-and-self packets reach this connection.
            // Tests finish synchronously, before ordinary world/player ticks can alter the fixture.
            helper.getLevel().addNewPlayer(player);
        }

        @SuppressWarnings("unchecked")
        private static <T> T playerListField(PlayerList list, String name) {
            try {
                var field = PlayerList.class.getDeclaredField(name);
                field.setAccessible(true);
                return (T) field.get(list);
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError("Cannot register the packet-recording GameTest player", exception);
            }
        }

        @Override
        public void close() {
            if (player.containerMenu != player.inventoryMenu) player.closeContainer();
            onlinePlayers.remove(player);
            playersByUuid.remove(player.getUUID(), player);
            player.discard();
            channel.close();
        }
    }
}
