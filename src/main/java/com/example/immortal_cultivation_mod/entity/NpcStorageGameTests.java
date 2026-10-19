package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.item.ModItems;
import java.util.ArrayList;
import java.util.Collections;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_npc_storage")
@PrefixGameTestTemplate(false)
public final class NpcStorageGameTests {
    @GameTest(template = "fate_test")
    public static void repeatedNpcReadsAvoidItemCodecs(GameTestHelper h) {
        var mob = ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());
        try {
            NpcProfile.ensure(mob, NpcProfile.JINDAN);
            var profile = NpcProfile.cultivationProfile(mob);
            profile.putString("Identity", "wanted");
            NpcProfile.ensure(mob, NpcProfile.JINDAN);
            NpcProfile.inventory(mob);
            var saved = profile.get("Inventory");
            long decoded = NpcInventoryCache.decodedStacks(), encoded = NpcInventoryCache.encodedStacks();
            for (int i = 0; i < 500; i++) {
                mob.npcQi(); mob.npcRealm(); NpcProfile.inventory(mob);
            }
            h.assertTrue(NpcInventoryCache.decodedStacks() == decoded && NpcInventoryCache.encodedStacks() == encoded,
                    "500 repeated profile/inventory reads perform zero additional item decodes or encodes");
            h.assertTrue(profile.get("Inventory") == saved, "Unchanged wanted stock keeps its existing saved inventory");
            System.out.println("NPC_STORAGE: 500 warmed Qi/realm/inventory read cycles; additional item decodes=0, encodes=0");
        } finally { mob.discard(); }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void inventoryEditsStayDetachedAndPersistImmediately(GameTestHelper h) {
        var mob = ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());
        try {
            var profile = NpcProfile.cultivationProfile(mob);
            var input = new ArrayList<>(Collections.nCopies(27, ItemStack.EMPTY));
            input.set(0, new ItemStack(Items.DIAMOND, 8));
            NpcProfile.writeInventory(mob, profile, input);
            input.getFirst().shrink(1);
            h.assertTrue(NpcProfile.inventory(mob).getFirst().getCount() == 8, "Caller cannot mutate committed cache through its old list");
            var edit = NpcProfile.inventory(mob); edit.getFirst().shrink(3);
            h.assertTrue(NpcProfile.inventory(mob).getFirst().getCount() == 8, "Uncommitted reads remain detached");
            NpcProfile.writeInventory(mob, profile, edit);
            var disk = new CompoundTag(); NpcProfile.save(mob, disk);
            var restored = ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());
            try {
                NpcProfile.load(restored, disk);
                h.assertTrue(NpcProfile.inventory(restored).getFirst().getCount() == 5,
                        "Committed changes survive the existing save/load format immediately");
            } finally { restored.discard(); }
        } finally { mob.discard(); }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void rawNbtChangesAndReloadInvalidateDecodedInventory(GameTestHelper h) {
        var mob = ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());
        try {
            var profile = NpcProfile.cultivationProfile(mob);
            NpcProfile.writeInventory(mob, profile, java.util.List.of(new ItemStack(Items.DIAMOND, 8)));
            NpcProfile.inventory(mob);
            profile.getList("Inventory", 10).getCompound(0).getCompound("Stack").putInt("count", 3);
            h.assertTrue(NpcProfile.inventory(mob).getFirst().getCount() == 3,
                    "In-place command or mod NBT edits invalidate old cached stacks");
            var saved = new CompoundTag(); NpcProfile.save(mob, saved);
            NpcProfile.writeInventory(mob, profile, java.util.List.of(new ItemStack(Items.EMERALD, 2)));
            NpcProfile.load(mob, saved);
            var restored = NpcProfile.inventory(mob).getFirst();
            h.assertTrue(restored.is(Items.DIAMOND) && restored.getCount() == 3, "Reload cannot retain the previous live inventory");
            NpcProfile.cultivationProfile(mob).getList("Inventory", 10).clear();
            h.assertTrue(NpcProfile.inventory(mob).stream().allMatch(ItemStack::isEmpty), "Deleted raw inventory cannot reappear from cache");
        } finally { mob.discard(); }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void equippedWeaponChangesSaveWithoutRewritingUnchangedStock(GameTestHelper h) {
        var mob = ModEntities.JINDAN_CULTIVATOR.get().create(h.getLevel());
        try {
            NpcProfile.ensure(mob, NpcProfile.JINDAN);
            var profile = NpcProfile.cultivationProfile(mob);
            profile.putString("Identity", "sword_cultivator");
            NpcProfile.writeInventory(mob, profile, java.util.List.of(new ItemStack(ModItems.FLYING_SWORD.get())));
            mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            NpcProfile.equipCombatWeapon(mob);
            h.assertTrue(!mob.getMainHandItem().isEmpty(), "Fixture equips a real stored weapon");
            NpcProfile.syncEquippedCombatWeapon(mob);
            long before = NpcInventoryCache.encodedStacks();
            for (int i = 0; i < 100; i++) NpcProfile.syncEquippedCombatWeapon(mob);
            h.assertTrue(NpcInventoryCache.encodedStacks() == before, "100 unchanged equipment syncs perform zero item encodes");
            mob.getMainHandItem().setDamageValue(7); NpcProfile.syncEquippedCombatWeapon(mob);
            h.assertTrue(NpcProfile.inventory(mob).getFirst().getDamageValue() == 7, "Changed live weapon components persist");
            mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY); NpcProfile.syncEquippedCombatWeapon(mob);
            h.assertTrue(NpcProfile.inventory(mob).getFirst().isEmpty(), "Broken equipment is removed, never resurrected by cache");
        } finally { mob.discard(); }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void savedPillBatchesRemainStable(GameTestHelper h) { NpcPillStockGameTests.npcPillStockSurvivesReloadWithoutRefill(h); }
    @GameTest(template = "fate_test")
    public static void existingFullInventoriesMigrateSafely(GameTestHelper h) { NpcPillStockGameTests.existingNpcUpdatePreservesCraftedPillsAndFullInventory(h); }
    @GameTest(template = "npc_combat_arena")
    public static void combatConsumesOnePill(GameTestHelper h) { NpcBattleReferenceGameTests.healingPillPreparesThenConsumesExactBatchOnce(h); }
}
