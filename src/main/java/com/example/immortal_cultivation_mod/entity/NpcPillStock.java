package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.alchemy.AlchemyChemistry;
import com.example.immortal_cultivation_mod.alchemy.AlchemyPillItem;
import com.example.immortal_cultivation_mod.alchemy.PillData;
import com.example.immortal_cultivation_mod.fate.FateCrafting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import static com.example.immortal_cultivation_mod.alchemy.AlchemyChemistry.*;

/** One-time, saved medicinal stock; opening an inventory never rerolls a batch. */
final class NpcPillStock {
    static final String VERSION = "AlchemyStockVersion";

    private NpcPillStock() {}

    static void ensure(PathfinderMob npc, CompoundTag profile, String archetype) {
        if (profile.getInt(VERSION) >= 1) return;
        Random random = new Random(npc.getUUID().getMostSignificantBits()
                ^ npc.getUUID().getLeastSignificantBits() ^ 0x50494C4C42415443L);
        List<ItemStack> inventory = NpcProfile.inventory(npc);
        for (ItemStack stack : inventory) randomizeUncrafted(stack, random);
        int tier = NpcProfile.JINDAN.equals(archetype) ? 3 : NpcProfile.ZHUJI.equals(archetype) ? 2
                : NpcProfile.LIANQI.equals(archetype) ? 1 : 0;
        List<Formula> pool = new ArrayList<>(FORMULAS);
        // Huiling already occurs in the old kits; these slots introduce the new varieties.
        pool.removeIf(formula -> formula.id().equals("huiling"));
        Collections.shuffle(pool, random);
        int batches = tier + 1;
        int next = 0;
        for (int slot = 0; slot < inventory.size() && next < batches; slot++) {
            if (!inventory.get(slot).isEmpty()) continue;
            inventory.set(slot, randomBatch(pool.get(next++), 1 + random.nextInt(tier + 2), random));
        }
        NpcProfile.writeInventory(npc, profile, inventory);
        profile.putInt(VERSION, 1);
    }

    private static void randomizeUncrafted(ItemStack stack, Random random) {
        if (stack.isEmpty()) return;
        if (stack.getItem() instanceof AlchemyPillItem pill) {
            if (!PillData.raw(stack).isEmpty()) return;
            ItemStack rolled = randomBatch(formula(pill.formula), stack.getCount(), random);
            CompoundTag tags = FateCrafting.tags(stack);
            tags.put(PillData.KEY, PillData.raw(rolled));
            FateCrafting.save(stack, tags);
        } else if (FateCrafting.pill(stack)) {
            CompoundTag tags = FateCrafting.tags(stack);
            // Legacy pills use their existing quality bonuses rather than new, unread stats.
            if (!tags.contains(FateCrafting.PILL)) {
                int roll = random.nextInt(100);
                tags.putInt(FateCrafting.PILL, roll < 60 ? 0 : roll < 85 ? 1 : roll < 97 ? 2 : 3);
                FateCrafting.save(stack, tags);
            }
        }
    }

    static ItemStack randomBatch(Formula formula, int count, Random random) {
        Batch batch = brew(formula, formula.required());
        List<String> herbs = new ArrayList<>(AlchemyChemistry.PLANTS.keySet());
        for (int attempt = 0; attempt < 12; attempt++) {
            var ingredients = new TreeMap<>(formula.required());
            ingredients.merge("harmonizing_herb", 1 + random.nextInt(3), Integer::sum);
            ingredients.merge(herbs.get(random.nextInt(herbs.size())), 1 + random.nextInt(2), Integer::sum);
            Batch candidate = brew(formula, ingredients);
            if (!candidate.failed()) {
                batch = candidate;
                break;
            }
        }
        // All base recipes must remain usable even if optional additives fail.
        if (batch.failed()) throw new IllegalStateException("Invalid NPC pill recipe: " + formula.id());
        var doses = new EnumMap<Effect, Dose>(Effect.class);
        for (Effect effect : Effect.values()) {
            Dose dose = batch.effects().get(effect);
            if (dose == null) continue;
            boolean flag = switch (effect) {
                case CLEAR_FIRE, NIGHT_VISION, MIND_SHIELD, WATER_BREATHING,
                        IRON_BONES, WITHER_SHIELD -> true;
                default -> false;
            };
            double amount = flag ? dose.amount() : round(dose.amount() * (.8 + random.nextDouble() * .4));
            int ticks = dose.ticks() == 0 ? 0 : Math.max(20,
                    Math.min(72000, (int) Math.round(dose.ticks() * (.8 + random.nextDouble() * .4))));
            doses.put(effect, new Dose(amount, ticks));
        }
        return PillData.create(new Batch(formula.id(), count, batch.elements(), doses,
                batch.stability(), batch.conflict(), batch.quality(), false));
    }
}
