package com.example.immortal_cultivation_mod.system;

import com.example.immortal_cultivation_mod.alchemy.AlchemyPillItem;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/** Saved expedition finds belong to the simulated character, never the real player's inventory. */
public final class LifeStoryLoot {
    private LifeStoryLoot() {}

    private static boolean knows(CompoundTag life, String list, String id) {
        return life.getList(list, Tag.TAG_STRING).stream().anyMatch(t -> t.getAsString().equals(id));
    }

    static List<String> spells(CompoundTag life) {
        return ModSpells.all().stream().filter(s -> LifeSimulationInheritance.canLearnSpell(life, s.id()))
                .map(ModSpells.SpellDef::id).filter(id -> !knows(life, "DiscoveredSpells", id)).sorted().toList();
    }

    static int methodTier(String tier) { return switch (tier) { case "heaven" -> 7; case "earth" -> 4; default -> 1; }; }

    static List<String> methods(CompoundTag life) {
        int realm = life.getInt("Realm");
        if (realm <= 0 || life.getString("SpiritualRoot").equals("none")) return List.of();
        return CultivationMethods.allMethods().stream()
                .filter(m -> methodTier(m.tier()) <= realm && CultivationLevels.getStageIndex(m.limitLevel()) >= realm)
                .map(CultivationMethods.MethodDef::id).filter(id -> !knows(life, "LearnedMethods", id)).sorted().toList();
    }

    static void prepare(CompoundTag life) {
        CompoundTag story = life.getCompound("Story");
        if (!story.getCompound("Definition").getBoolean("loot_encounter") || story.contains("LootOffers")) return;
        RandomSource random = RandomSource.create(life.getLong("Seed") ^ ((long) life.getInt("Realm") << 32)
                ^ story.getString("Id").hashCode() ^ 0x4C4F4F54L);
        CompoundTag offers = new CompoundTag();
        var pills = BuiltInRegistries.ITEM.stream().filter(item -> item instanceof AlchemyPillItem)
                .map(item -> BuiltInRegistries.ITEM.getKey(item).toString()).sorted().toList();
        var newPills = pills.stream().filter(id -> LifeSimulationBattle.itemCount(life, id) == 0).toList();
        offer(offers, "pill", newPills.isEmpty() ? pills : newPills, random, 2);
        offer(offers, "spell", spells(life), random, 1);
        offer(offers, "method", methods(life), random, 1);
        story.put("LootOffers", offers);
    }

    private static void offer(CompoundTag offers, String kind, List<String> ids, RandomSource random, int count) {
        if (ids.isEmpty()) return;
        CompoundTag reward = new CompoundTag();
        reward.putString("Kind", kind); reward.putString("Id", ids.get(random.nextInt(ids.size()))); reward.putInt("Count", count);
        offers.put(kind, reward);
    }

    static CompoundTag offer(CompoundTag life, String kind) {
        return life.getCompound("Story").getCompound("LootOffers").getCompound(kind);
    }

    static void grant(CompoundTag life, String kind) {
        CompoundTag story = life.getCompound("Story"), reward = offer(life, kind);
        if (story.contains("LootReceipt") || reward.isEmpty()) return;
        String id = reward.getString("Id");
        switch (kind) {
            case "pill" -> {
                ResourceLocation key = ResourceLocation.tryParse(id);
                if (key == null || !(BuiltInRegistries.ITEM.get(key) instanceof AlchemyPillItem)) return;
                LifeSimulationSystem.addSimulationItem(life, id, reward.getInt("Count"));
            }
            case "spell" -> {
                if (!spells(life).contains(id)) return;
                LifeSimulationSystem.applyEffect(life, "spell:" + id);
            }
            case "method" -> {
                if (!methods(life).contains(id)) return;
                LifeSimulationSystem.applyEffect(life, "method:" + id);
            }
            default -> { return; }
        }
        story.put("LootReceipt", reward.copy());
        LifeSimulationSystem.addLog(life, switch (kind) {
            case "pill" -> BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)).getDescriptionId();
            case "spell" -> "spell.immortal_cultivation_mod." + ModSpells.get(id).translationKey();
            default -> CultivationMethods.get(id).nameKey();
        });
    }

    public static Component description(CompoundTag reward) {
        String id = reward.getString("Id");
        Component name = switch (reward.getString("Kind")) {
            case "pill" -> BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)).getDescription();
            case "spell" -> ModSpells.get(id).name();
            default -> Component.translatable(CultivationMethods.get(id).nameKey());
        };
        return Component.translatable("life.loot.reward", name, reward.getInt("Count"));
    }
}
