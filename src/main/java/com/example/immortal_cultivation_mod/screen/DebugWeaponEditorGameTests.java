package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.item.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder("immortal_cultivation_mod")
@PrefixGameTestTemplate(false)
public final class DebugWeaponEditorGameTests {
    private static final class EditorPlayer implements AutoCloseable {
        final ServerPlayer player;
        final io.netty.channel.embedded.EmbeddedChannel channel;
        EditorPlayer(GameTestHelper helper) {
            player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                    new GameProfile(UUID.randomUUID(), "InstantRankTest"),
                    net.minecraft.server.level.ClientInformation.createDefault());
            var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
            new net.minecraft.server.network.ServerGamePacketListenerImpl(player.server, connection, player,
                    net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet) { }
            };
            var position = helper.absolutePos(new net.minecraft.core.BlockPos(3, 2, 3));
            player.setPos(position.getX() + .5, position.getY(), position.getZ() + .5);
        }
        @Override public void close() {
            var data = WeaponTribulationData.get(player.server);
            var owned = data.trials.values().stream().filter(trial -> trial.owner.equals(player.getUUID()))
                    .map(trial -> trial.id).toList();
            owned.forEach(data.trials::remove); data.setDirty();
            for (var level : player.server.getAllLevels()) for (var entity : level.getAllEntities()) {
                if (entity instanceof ItemEntity item) {
                    UUID token = WeaponTribulation.token(item.getItem());
                    if (token != null && owned.contains(token)) item.discard();
                }
            }
            player.discard(); channel.close();
        }
    }

    @GameTest(template = "fate_test")
    public static void weaponEditorEditsRealSlotAndReturnsOnce(GameTestHelper h) {
        var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "WeaponEditor"));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.DEBUG_STAT_EDITOR.get()));
        var menu = new DebugWeaponEditorMenu(7, player.getInventory()); player.containerMenu = menu;
        var stack = new ItemStack(ModItems.DAO.get()); menu.getSlot(0).set(stack);
        h.assertTrue(!menu.getSlot(0).mayPlace(new ItemStack(Items.DIRT)), "Editor accepts weapons only");
        int max = stack.getMaxDamage(); menu.edit(player, 2, 0, 17);
        h.assertTrue(stack.getMaxDamage() == max && stack.getMaxDamage() - stack.getDamageValue() == 17, "Current durability changes without changing maximum");
        menu.edit(player, 1, WeaponRestrictionSystem.RestrictionType.QI_STORAGE.ordinal(), 10);
        menu.edit(player, 0, 0, 1);
        h.assertTrue(WeaponRestrictionSystem.tier(stack) == 1 && WeaponRestrictionSystem.tierProgress(stack) == 10, "Rank adjustment preserves restriction progress");
        menu.edit(player, 1, WeaponRestrictionSystem.RestrictionType.QI_STORAGE.ordinal(), -1);
        h.assertTrue(WeaponRestrictionSystem.count(stack, WeaponRestrictionSystem.RestrictionType.QI_STORAGE) == 9, "Quick removal updates layers");
        menu.edit(player, 2, 0, Integer.MAX_VALUE);
        h.assertTrue(stack.getDamageValue() == 0 && stack.getMaxDamage() == max, "Durability input clamps to maximum");
        menu.edit(player, 2, 0, -1);
        h.assertTrue(stack.getMaxDamage() - stack.getDamageValue() == 1, "Zero input cannot destroy inserted weapon");
        menu.removed(player); menu.removed(player);
        int copies = 0;
        for (var item : player.getInventory().items) if (item.is(ModItems.DAO.get())) copies += item.getCount();
        h.assertTrue(copies == 1 && menu.weapon().isEmpty(), "Closing returns exactly one original weapon");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void weaponEditorRejectsMissingToolAndClosedMenu(GameTestHelper h) {
        var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "EditorPermissions"));
        var menu = new DebugWeaponEditorMenu(8, player.getInventory()); player.containerMenu = menu;
        var stack = new ItemStack(ModItems.DAO.get()); menu.getSlot(0).set(stack);
        menu.edit(player, 0, 0, 1);
        h.assertTrue(WeaponRestrictionSystem.tier(stack) == 0, "Without debug item edits are rejected");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.DEBUG_STAT_EDITOR.get()));
        player.containerMenu = player.inventoryMenu; menu.edit(player, 0, 0, 1);
        h.assertTrue(WeaponRestrictionSystem.tier(stack) == 0, "Closed-menu edits are rejected");
        menu.removed(player); h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void mergedRestrictionRemovalUpdatesCombatAndDurability(GameTestHelper h) {
        var stack = new ItemStack(ModItems.DAO.get()); int baseMax = stack.getMaxDamage();
        int[] layers = new int[WeaponRestrictionSystem.RestrictionType.values().length];
        layers[WeaponRestrictionSystem.RestrictionType.SHARPNESS.ordinal()] = 36;
        layers[WeaponRestrictionSystem.RestrictionType.STURDY.ordinal()] = 36;
        WeaponRestrictionSystem.addRestrictions(stack, layers); WeaponRestrictionSystem.evolve(stack);
        double damage = QiInfusedWeapon.baseAttackDamage(stack);
        WeaponRestrictionSystem.debugAdjustLayers(stack, WeaponRestrictionSystem.RestrictionType.SHARPNESS, -36);
        WeaponRestrictionSystem.debugAdjustLayers(stack, WeaponRestrictionSystem.RestrictionType.STURDY, -36);
        h.assertTrue(QiInfusedWeapon.baseAttackDamage(stack) < damage && stack.getMaxDamage() == baseMax, "Removing completed restrictions removes their baked damage and durability effects");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void completedCycleDebugRankAdvancesImmediatelyAndClampsWithoutTrial(GameTestHelper h) {
        try (var fixture = new EditorPlayer(h)) {
            var player = fixture.player;
            var stack = new ItemStack(ModItems.DAO.get());
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("Instant rank original"));
            int[] layers = new int[WeaponRestrictionSystem.RestrictionType.values().length];
            for (var type : new WeaponRestrictionSystem.RestrictionType[]{
                    WeaponRestrictionSystem.RestrictionType.SHARPNESS,
                    WeaponRestrictionSystem.RestrictionType.STURDY,
                    WeaponRestrictionSystem.RestrictionType.QI_STORAGE}) layers[type.ordinal()] = 24;
            h.assertTrue(WeaponRestrictionSystem.addRestrictions(stack, layers), "Weapon starts with a complete mixed 72-layer cycle");
            stack.setDamageValue(23);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var attachment = com.example.immortal_cultivation_mod.attachment.ModAttachments.getData(player);
            com.example.immortal_cultivation_mod.attachment.ModAttachments.setData(player, attachment.withQi(500));
            QiInfusedWeapon.inject(player);
            double qi = QiInfusedWeapon.getQi(stack), damage = QiInfusedWeapon.baseAttackDamage(stack);
            int maxDurability = stack.getMaxDamage();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DEBUG_STAT_EDITOR.get()));
            var menu = new DebugWeaponEditorMenu(9, player.getInventory()); player.containerMenu = menu;
            menu.getSlot(0).set(stack); menu.edit(player, 0, 0, 1);
            h.assertTrue(menu.weapon() == stack && WeaponRestrictionSystem.tier(stack) == 1
                            && WeaponRestrictionSystem.tierProgress(stack) == 0 && !WeaponTribulation.active(stack),
                    "Debug rank+ advances the original complete-cycle weapon immediately inside the editor");
            for (var type : WeaponRestrictionSystem.RestrictionType.values()) {
                h.assertTrue(WeaponRestrictionSystem.count(stack, type) == 0
                                && WeaponRestrictionSystem.mergedCount(stack, type) == layers[type.ordinal()],
                        "Instant advancement merges the exact existing restriction count: " + type);
            }
            h.assertTrue(Math.abs(QiInfusedWeapon.baseAttackDamage(stack) - damage * 1.24) < .00001
                            && stack.getMaxDamage() == maxDurability && stack.getDamageValue() == 23
                            && qi > 0 && QiInfusedWeapon.getQi(stack) == qi
                            && stack.get(DataComponents.CUSTOM_NAME).equals(Component.literal("Instant rank original")),
                    "Instant rank bakes sharpness once and preserves durability, stored Qi and identity data");
            for (int i = 0; i < WeaponRestrictionSystem.MAX_TIER + 2; i++) menu.edit(player, 0, 0, Integer.MAX_VALUE);
            menu.edit(player, 1, WeaponRestrictionSystem.RestrictionType.QI_STORAGE.ordinal(), 72);
            menu.edit(player, 0, 0, 1);
            h.assertTrue(menu.weapon() == stack && WeaponRestrictionSystem.tier(stack) == WeaponRestrictionSystem.MAX_TIER
                            && WeaponRestrictionSystem.tierProgress(stack) == 72
                            && WeaponRestrictionSystem.mergedCount(stack, WeaponRestrictionSystem.RestrictionType.QI_STORAGE) == 24,
                    "Debug rank clamps at maximum without remerging or losing the final 72-layer cycle");
            h.assertTrue(WeaponTribulationData.get(player.server).trials.values().stream()
                            .noneMatch(trial -> trial.owner.equals(player.getUUID())),
                    "No debug rank action creates a lightning trial");
            for (var entity : h.getLevel().getAllEntities()) if (entity instanceof ItemEntity item && item.isAlive()) {
                h.assertTrue(!Component.literal("Instant rank original").equals(item.getItem().get(DataComponents.CUSTOM_NAME)),
                        "Debug rank never drops or duplicates the edited weapon");
            }
            menu.removed(player);
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void instantDebugRankCannotAlterAlreadyTribulatingWeapon(GameTestHelper h) {
        try (var fixture = new EditorPlayer(h)) {
            var player = fixture.player;
            var stack = new ItemStack(ModItems.DAO.get());
            int[] layers = new int[WeaponRestrictionSystem.RestrictionType.values().length];
            layers[WeaponRestrictionSystem.RestrictionType.QI_STORAGE.ordinal()] = 72;
            WeaponRestrictionSystem.addRestrictions(stack, layers); stack.setDamageValue(17);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.DEBUG_STAT_EDITOR.get()));
            h.assertTrue(WeaponTribulation.start(player, stack), "A normal held weapon starts an actual trial");
            UUID id = WeaponTribulation.token(stack);
            var original = stack.copy();
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            var menu = new DebugWeaponEditorMenu(10, player.getInventory()); player.containerMenu = menu;
            menu.getSlot(0).set(stack);
            menu.edit(player, 0, 0, 1);
            menu.edit(player, 1, WeaponRestrictionSystem.RestrictionType.QI_STORAGE.ordinal(), -1);
            menu.edit(player, 2, 0, Integer.MAX_VALUE);
            h.assertTrue(menu.weapon() == stack && ItemStack.isSameItemSameComponents(original, stack)
                            && id.equals(WeaponTribulation.token(stack))
                            && WeaponTribulationData.get(player.server).trials.containsKey(id),
                    "Instant debug rank and other edits cannot bypass or alter an already active trial");
            menu.removed(player);
        }
        h.succeed();
    }
}
