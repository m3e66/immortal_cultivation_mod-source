package com.example.immortal_cultivation_mod.system;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;

/** Inheritance sources belong to the simulated life, never to its real-world host. */
final class LifeSimulationInheritance {
    static final int VERSION = 2;

    private LifeSimulationInheritance() {}

    static boolean canLearnSpell(CompoundTag life, String id) {
        var spell = ModSpells.get(id);
        return life.getInt("Realm") > 0 && !"none".equals(life.getString("SpiritualRoot"))
                && spell != null && !ModSpells.innateSpellIds().contains(id)
                && CultivationLevels.getStageIndex(spell.requiredLevel()) <= life.getInt("Realm");
    }

    /** Inherit completed stages; unfinished progress has not yet become a lasting attainment. */
    static long cultivation(CompoundTag life) {
        int realm = Mth.clamp(life.getInt("Realm"), 0, CultivationLevels.allLevels().size() - 1);
        if (realm == 0 || "none".equals(life.getString("SpiritualRoot"))) return 0;
        long total = 0;
        for (int stage = 1; stage < realm; stage++) total += required(stage);
        return total;
    }

    private static long required(int stage) {
        return Math.max(0, CultivationLevels.getLevelDef(CultivationLevels.allLevels().get(stage)).qiNeeded());
    }

    static long progressGain(CompoundTag reward) {
        long base = Math.max(0, reward.getLong("SourceCultivation"));
        int percent = Mth.clamp(reward.getInt("Percent"), 2, 30);
        return base / 100 * percent + base % 100 * percent / 100;
    }

    record CultivationCarry(String level, long progress, long gained) {}

    /** Inheritance alone crosses ordinary stage caps, retaining every point until the final stage. */
    static CultivationCarry carry(String level, long progress, long gain) {
        var levels = CultivationLevels.allLevels();
        int stage = Mth.clamp(CultivationLevels.getStageIndex(level), 0, levels.size() - 1);
        long current = Mth.clamp(progress, 0, required(stage));
        long remaining = Math.max(0, gain);
        while (remaining > 0) {
            long consumed = Math.min(remaining, required(stage) - current);
            current += consumed;
            remaining -= consumed;
            if (current < required(stage) || stage == levels.size() - 1) break;
            stage++;
            current = 0;
        }
        return new CultivationCarry(levels.get(stage), current, Math.max(0, gain) - remaining);
    }

    /** Canonical, source-backed options. The client selects an index, never supplies item quantities. */
    static ListTag options(CompoundTag life, String category) {
        ListTag options = new ListTag();
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        if (category.equals("spell")) {
            for (var entry : life.getList("DiscoveredSpells", Tag.TAG_STRING)) {
                String id = entry.getAsString();
                if (!canLearnSpell(life, id) || !seen.add(id)) continue;
                CompoundTag option = reward(category);
                option.putString("Spell", id);
                options.add(option);
            }
        } else if (category.equals("materials")) {
            for (var entry : life.getList("Inventory", Tag.TAG_COMPOUND)) {
                CompoundTag item = (CompoundTag) entry;
                ResourceLocation id = ResourceLocation.tryParse(item.getString("Id"));
                if (item.getInt("Count") <= 0 || id == null || !BuiltInRegistries.ITEM.containsKey(id)
                        || BuiltInRegistries.ITEM.get(id) == Items.AIR || !seen.add(id.toString())) continue;
                CompoundTag option = reward(category);
                option.putString("Item", id.toString());
                option.putInt("Count", Math.min(BuiltInRegistries.ITEM.get(id).getDefaultInstance().getMaxStackSize(), item.getInt("Count")));
                options.add(option);
            }
        }
        return options;
    }

    static int optionIndex(ListTag options, CompoundTag selected) {
        String field = selected.getString("Id").equals("spell") ? "Spell" : "Item";
        for (int i = 0; i < options.size(); i++) {
            if (options.getCompound(i).getString(field).equals(selected.getString(field))) return i;
        }
        return 0;
    }

    static ListTag generate(CompoundTag life) {
        RandomSource random = RandomSource.create(life.getLong("Seed") ^ life.getLong("Score") ^ 0xA54FF53A5F1D36F1L);
        ListTag rewards = new ListTag();
        CompoundTag progress = reward("progress");
        progress.putInt("Percent", Mth.clamp(2 + life.getInt("HighestRealm") / 2 + life.getCompound("StoryJournal").size(), 2, 30));
        progress.putLong("SourceCultivation", cultivation(life));
        rewards.add(progress);

        for (String category : new String[]{"spell", "materials"}) {
            ListTag options = options(life, category);
            rewards.add(options.isEmpty() ? reward(category) : options.getCompound(random.nextInt(options.size())).copy());
        }
        if (life.getList("Upgrades", Tag.TAG_STRING).stream().anyMatch(t -> t.getAsString().equals("inheritance_expansion"))) {
            CompoundTag blessing = reward("blessing");
            blessing.putString("Blessing", "fortune");
            rewards.add(blessing);
        }
        return rewards;
    }

    private static CompoundTag reward(String id) {
        CompoundTag reward = new CompoundTag();
        reward.putString("Id", id);
        return reward;
    }

    static boolean validSource(CompoundTag life, CompoundTag reward) {
        return switch (reward.getString("Id")) {
            case "progress" -> reward.getLong("SourceCultivation") == cultivation(life);
            case "spell" -> canLearnSpell(life, reward.getString("Spell"))
                    && life.getList("DiscoveredSpells", Tag.TAG_STRING).stream().anyMatch(t -> t.getAsString().equals(reward.getString("Spell")));
            case "materials" -> {
                ResourceLocation id = ResourceLocation.tryParse(reward.getString("Item"));
                yield id != null && BuiltInRegistries.ITEM.containsKey(id) && BuiltInRegistries.ITEM.get(id) != Items.AIR
                        && reward.getInt("Count") > 0 && life.getList("Inventory", Tag.TAG_COMPOUND).stream().anyMatch(t -> {
                            CompoundTag item = (CompoundTag) t;
                            return item.getString("Id").equals(reward.getString("Item")) && item.getInt("Count") >= reward.getInt("Count");
                        });
            }
            case "blessing" -> life.getList("Upgrades", Tag.TAG_STRING).stream().anyMatch(t -> t.getAsString().equals("inheritance_expansion"));
            default -> false;
        };
    }

    static void normalize(CompoundTag life) {
        if (life.getBoolean("Finished") && !life.getBoolean("RewardClaimed") && life.getInt("InheritanceSourceVersion") < VERSION) {
            life.put("InheritanceRewards", generate(life));
            // Old reserved receipts can contain invented items/spells or host-based cultivation.
            // No delivery took place while pending, so reopen the corrected source-backed choices.
            life.remove("PendingInheritance");
            life.remove("ClaimId");
            life.remove("ClaimTransaction");
            life.putInt("InheritanceSourceVersion", VERSION);
        }
    }
}
