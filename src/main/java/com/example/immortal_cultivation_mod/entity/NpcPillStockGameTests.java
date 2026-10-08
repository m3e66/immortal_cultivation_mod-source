package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.alchemy.AlchemyChemistry;
import com.example.immortal_cultivation_mod.alchemy.AlchemyPillItem;
import com.example.immortal_cultivation_mod.alchemy.PillData;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_mod")
@PrefixGameTestTemplate(false)
public final class NpcPillStockGameTests {
    @GameTest(template = "fate_test")
    public static void everyNewFormulaHasVariedValidSavedStats(GameTestHelper h) {
        for (var formula : AlchemyChemistry.FORMULAS) {
            var variants = new HashSet<String>();
            for (int seed = 0; seed < 48; seed++) {
                var pill = NpcPillStock.randomBatch(formula, 3, new Random(seed));
                h.assertTrue(pill.getItem() instanceof AlchemyPillItem && pill.getCount() == 3,
                        "NPC batches are real pills, never failed ash");
                var data = PillData.raw(pill);
                h.assertTrue(data.getDouble("Stability") >= 20 && !data.getCompound("Elements").isEmpty()
                                && !PillData.effects(pill).isEmpty(), "Pills carry valid chemistry stats");
                var parsed = ItemStack.parseOptional(h.getLevel().registryAccess(),
                        (CompoundTag) pill.saveOptional(h.getLevel().registryAccess()));
                h.assertTrue(ItemStack.isSameItemSameComponents(pill, parsed), "Item serialization preserves rolled stats");
                variants.add(data.toString());
            }
            h.assertTrue(variants.size() > 20, "Independent NPC batches vary for " + formula.id());
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void npcPillStockSurvivesReloadWithoutRefill(GameTestHelper h) {
        for (var type : java.util.List.of(ModEntities.FANREN_NPC.get(), ModEntities.LIANQI_CULTIVATOR.get(),
                ModEntities.ZHUJI_CULTIVATOR.get(), ModEntities.JINDAN_CULTIVATOR.get())) {
            var npc = type.create(h.getLevel());
            String archetype = ((CultivationNpc) npc).npcArchetype();
            NpcProfile.ensure(npc, archetype);
            var inventory = NpcProfile.inventory(npc);
            int newPills = 0;
            for (var stack : inventory) {
                if (stack.getItem() instanceof AlchemyPillItem pill) {
                    h.assertTrue(!PillData.raw(stack).isEmpty(), "NPC alchemy pills have rolled stats");
                    if (!pill.formula.equals("huiling")) newPills++;
                }
            }
            h.assertTrue(newPills > 0, "Each NPC tier receives new pills");
            var profile = NpcProfile.cultivationProfile(npc);
            var before = profile.getList("Inventory", 10).copy();
            NpcProfile.ensure(npc, archetype);
            h.assertTrue(before.equals(profile.getList("Inventory", 10)), "Repeated ensure does not reroll or duplicate stock");
            for (int i = 0; i < inventory.size(); i++) {
                if (inventory.get(i).getItem() instanceof AlchemyPillItem) inventory.set(i, ItemStack.EMPTY);
            }
            NpcProfile.writeInventory(npc, profile, inventory);
            var saved = new CompoundTag();
            NpcProfile.save(npc, saved);
            var loaded = type.create(h.getLevel());
            loaded.setUUID(npc.getUUID());
            NpcProfile.load(loaded, saved);
            NpcProfile.ensure(loaded, archetype);
            h.assertTrue(NpcProfile.inventory(loaded).stream().noneMatch(s -> s.getItem() instanceof AlchemyPillItem),
                    "Used or traded pills do not respawn after loading");
            npc.discard(); loaded.discard();
        }
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void existingNpcUpdatePreservesCraftedPillsAndFullInventory(GameTestHelper h) {
        var npc = ModEntities.LIANQI_CULTIVATOR.get().create(h.getLevel());
        NpcProfile.ensure(npc, npc.npcArchetype());
        var profile = NpcProfile.cultivationProfile(npc);
        var inventory = new ArrayList<>(NpcProfile.inventory(npc));
        for (int i = 0; i < inventory.size(); i++) inventory.set(i, new ItemStack(Items.DIAMOND, 64));
        var crafted = NpcPillStock.randomBatch(AlchemyChemistry.formula("huichun"), 5, new Random(73));
        inventory.set(0, crafted.copy());
        NpcProfile.writeInventory(npc, profile, inventory);
        profile.remove(NpcPillStock.VERSION);
        NpcProfile.ensure(npc, npc.npcArchetype());
        var after = NpcProfile.inventory(npc);
        h.assertTrue(ItemStack.isSameItemSameComponents(after.getFirst(), crafted)
                && after.getFirst().getCount() == 5, "Existing crafted pill stats and quantity remain intact");
        for (int i = 1; i < after.size(); i++) h.assertTrue(after.get(i).is(Items.DIAMOND)
                && after.get(i).getCount() == 64, "Full inventory never loses existing items to new stock");
        npc.discard(); h.succeed();
    }
}
