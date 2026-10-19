package com.example.immortal_cultivation_mod.guidance;

import java.util.List;

/** Player-facing side lessons; read-only objectives never impersonate gameplay. */
public final class DaoGuidanceExpansion {
    private DaoGuidanceExpansion() {}
    private static final List<DaoGuidanceContent.Chapter> CHAPTERS = List.of(
            new DaoGuidanceContent.Chapter("learning_puzzles", true, List.of(
                    new DaoGuidanceContent.Objective("step_1", "read:learning_puzzles:step_1", 1),
                    new DaoGuidanceContent.Objective("step_2", "read:learning_puzzles:step_2", 1),
                    new DaoGuidanceContent.Objective("step_3", "read:learning_puzzles:step_3", 1),
                    new DaoGuidanceContent.Objective("step_4", "read:learning_puzzles:step_4", 1),
                    new DaoGuidanceContent.Objective("step_5", "read:learning_puzzles:step_5", 1))),
            new DaoGuidanceContent.Chapter("pill_toxicity",true,List.of(
                    new DaoGuidanceContent.Objective("step_1","read:pill_toxicity:step_1",1),
                    new DaoGuidanceContent.Objective("step_2","read:pill_toxicity:step_2",1),
                    new DaoGuidanceContent.Objective("step_3","read:pill_toxicity:step_3",1),
                    new DaoGuidanceContent.Objective("step_4","read:pill_toxicity:step_4",1),
                    new DaoGuidanceContent.Objective("step_5","read:pill_toxicity:step_5",1))),
            new DaoGuidanceContent.Chapter("special_flames", true, List.of(
                    new DaoGuidanceContent.Objective("step_1", "guide_flame_seen", 1),
                    new DaoGuidanceContent.Objective("step_2", "read:special_flames:step_2", 1),
                    new DaoGuidanceContent.Objective("step_3", "read:special_flames:step_3", 1),
                    new DaoGuidanceContent.Objective("step_4", "read:special_flames:step_4", 1),
                    new DaoGuidanceContent.Objective("step_5", "guide_special_forge", 1))),
            new DaoGuidanceContent.Chapter("flame_vessel", true, List.of(
                    new DaoGuidanceContent.Objective("step_1", "guide_vessel_obtained", 1),
                    new DaoGuidanceContent.Objective("step_2", "guide_flame_captured", 1),
                    new DaoGuidanceContent.Objective("step_3", "read:flame_vessel:step_3", 1),
                    new DaoGuidanceContent.Objective("step_4", "guide_flame_released", 1),
                    new DaoGuidanceContent.Objective("step_5", "guide_flame_shot", 1),
                    new DaoGuidanceContent.Objective("step_6", "guide_flame_charged", 1),
                    new DaoGuidanceContent.Objective("step_7", "guide_giant_shot", 1))),
            new DaoGuidanceContent.Chapter("spirit_forging", true, List.of(
                    new DaoGuidanceContent.Objective("step_1", "guide_forge_fire", 1),
                    new DaoGuidanceContent.Objective("step_2", "read:spirit_forging:step_2", 1),
                    new DaoGuidanceContent.Objective("step_3", "read:spirit_forging:step_3", 1),
                    new DaoGuidanceContent.Objective("step_4", "read:spirit_forging:step_4", 1),
                    new DaoGuidanceContent.Objective("step_5", "guide_special_forge", 1))),
            new DaoGuidanceContent.Chapter("giant_fireball", true, List.of(
                    new DaoGuidanceContent.Objective("step_1", "guide_flame_charged", 1),
                    new DaoGuidanceContent.Objective("step_2", "guide_giant_impact", 1),
                    new DaoGuidanceContent.Objective("step_3", "read:giant_fireball:step_3", 1),
                    new DaoGuidanceContent.Objective("step_4", "read:giant_fireball:step_4", 1),
                    new DaoGuidanceContent.Objective("step_5", "read:giant_fireball:step_5", 1))),
            new DaoGuidanceContent.Chapter("weapon_growth", true, List.of(
                    new DaoGuidanceContent.Objective("step_1", "read:weapon_growth:step_1", 1),
                    new DaoGuidanceContent.Objective("step_2", "guide_restriction_added", 1),
                    new DaoGuidanceContent.Objective("step_3", "guide_restriction_cycle", 1),
                    new DaoGuidanceContent.Objective("step_4", "guide_weapon_trial_started", 1),
                    new DaoGuidanceContent.Objective("step_5", "guide_weapon_trial_started", 1))),
            new DaoGuidanceContent.Chapter("weapon_trial", true, List.of(
                    new DaoGuidanceContent.Objective("step_1", "guide_weapon_trial_started", 1),
                    new DaoGuidanceContent.Objective("step_2", "guide_weapon_trial_held", 1),
                    new DaoGuidanceContent.Objective("step_3", "guide_weapon_trial_survived", 1),
                    new DaoGuidanceContent.Objective("step_4", "read:weapon_trial:step_4", 1),
                    new DaoGuidanceContent.Objective("step_5", "read:weapon_trial:step_5", 1),
                    new DaoGuidanceContent.Objective("step_6", "read:weapon_trial:step_6", 1))),
            new DaoGuidanceContent.Chapter("weapon_shards", true, List.of(
                    new DaoGuidanceContent.Objective("step_1", "guide_shard_obtained", 1),
                    new DaoGuidanceContent.Objective("step_2", "read:weapon_shards:step_2", 1),
                    new DaoGuidanceContent.Objective("step_3", "guide_shards_complete", 1),
                    new DaoGuidanceContent.Objective("step_4", "guide_reforge_ready", 1),
                    new DaoGuidanceContent.Objective("step_5", "guide_weapon_reforged", 1),
                    new DaoGuidanceContent.Objective("step_6", "guide_shard_forge", 1))),
            new DaoGuidanceContent.Chapter("core_recovery", true, List.of(
                    new DaoGuidanceContent.Objective("step_1", "guide_core_failed", 1),
                    new DaoGuidanceContent.Objective("step_2", "read:core_recovery:step_2", 1),
                    new DaoGuidanceContent.Objective("step_3", "read:core_recovery:step_3", 1),
                    new DaoGuidanceContent.Objective("step_4", "guide_core_ready", 1),
                    new DaoGuidanceContent.Objective("step_5", "guide_core_retry", 1))),
            new DaoGuidanceContent.Chapter("pill_use", true, List.of(
                    new DaoGuidanceContent.Objective("step_1", "guide_pill_obtained", 1),
                    new DaoGuidanceContent.Objective("step_2", "read:pill_use:step_2", 1),
                    new DaoGuidanceContent.Objective("step_3", "guide_pill_used", 1),
                    new DaoGuidanceContent.Objective("step_4", "guide_pill_effect", 1),
                    new DaoGuidanceContent.Objective("step_5", "read:pill_use:step_5", 1),
                    new DaoGuidanceContent.Objective("step_6", "read:pill_use:step_6", 1))),
            new DaoGuidanceContent.Chapter("thought_resistance", true, List.of(
                    new DaoGuidanceContent.Objective("step_1", "guide_control_pressure", 1),
                    new DaoGuidanceContent.Objective("step_2", "guide_thought_spent", 1),
                    new DaoGuidanceContent.Objective("step_3", "read:thought_resistance:step_3", 1),
                    new DaoGuidanceContent.Objective("step_4", "read:thought_resistance:step_4", 1),
                    new DaoGuidanceContent.Objective("step_5", "guide_thought_restored", 1),
                    new DaoGuidanceContent.Objective("step_6", "read:thought_resistance:step_6", 1))),
            new DaoGuidanceContent.Chapter("life_simulation", true, List.of(
                    new DaoGuidanceContent.Objective("step_1", "guide_life_started", 1),
                    new DaoGuidanceContent.Objective("step_2", "guide_life_choice", 1),
                    new DaoGuidanceContent.Objective("step_3", "guide_life_event", 3),
                    new DaoGuidanceContent.Objective("step_4", "read:life_simulation:step_4", 1),
                    new DaoGuidanceContent.Objective("step_5", "guide_life_settled", 1),
                    new DaoGuidanceContent.Objective("step_6", "guide_life_finished", 1))),
            new DaoGuidanceContent.Chapter("hair_styling", true, List.of(
                    new DaoGuidanceContent.Objective("step_1", "guide_hair_opened", 1),
                    new DaoGuidanceContent.Objective("step_2", "read:hair_styling:step_2", 1),
                    new DaoGuidanceContent.Objective("step_3", "read:hair_styling:step_3", 1),
                    new DaoGuidanceContent.Objective("step_4", "read:hair_styling:step_4", 1),
                    new DaoGuidanceContent.Objective("step_5", "guide_hair_saved", 1))),
            new DaoGuidanceContent.Chapter("accessories", true, List.of(
                    new DaoGuidanceContent.Objective("step_1", "read:accessories:step_1", 1),
                    new DaoGuidanceContent.Objective("step_2", "guide_coin_equipped", 1),
                    new DaoGuidanceContent.Objective("step_3", "guide_ring_triggered", 1),
                    new DaoGuidanceContent.Objective("step_4", "guide_pendant_triggered", 1),
                    new DaoGuidanceContent.Objective("step_5", "guide_leaf_triggered", 1),
                    new DaoGuidanceContent.Objective("step_6", "read:accessories:step_6", 1))));
    private static final List<DaoGuidanceContent.Chapter> ALL_CHAPTERS = java.util.stream.Stream.concat(
            CHAPTERS.stream(), DaoGuidanceUpdates.chapters().stream()).toList();
    public static List<DaoGuidanceContent.Chapter> chapters() { return ALL_CHAPTERS; }
    public static boolean isReading(String action) {
        return action != null && action.startsWith("read:")
                && DaoGuidanceContent.chapters().stream().flatMap(c -> c.objectives().stream())
                .anyMatch(o -> o.action().equals(action));
    }
    public static String chapterFor(String action) {
        return ALL_CHAPTERS.stream().filter(c -> c.objectives().stream()
                .anyMatch(o -> o.action().equals(action))).map(DaoGuidanceContent.Chapter::id)
                .findFirst().orElse("");
    }
}
