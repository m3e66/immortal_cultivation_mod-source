package com.example.immortal_cultivation_mod.guidance;

import java.util.List;

/** Current-feature readings. Existing chapter/objective IDs remain stable for old saves. */
public final class DaoGuidanceUpdates {
    private DaoGuidanceUpdates() {}
    private static final List<DaoGuidanceContent.Chapter> CHAPTERS = List.of(
            new DaoGuidanceContent.Chapter("qi_blast", true, List.of(
                    new DaoGuidanceContent.Objective("step_1", "read:qi_blast:step_1", 1),
                    new DaoGuidanceContent.Objective("step_2", "read:qi_blast:step_2", 1),
                    new DaoGuidanceContent.Objective("step_3", "read:qi_blast:step_3", 1),
                    new DaoGuidanceContent.Objective("step_4", "read:qi_blast:step_4", 1),
                    new DaoGuidanceContent.Objective("step_5", "read:qi_blast:step_5", 1),
                    new DaoGuidanceContent.Objective("step_6", "read:qi_blast:step_6", 1),
                    new DaoGuidanceContent.Objective("step_7", "read:qi_blast:step_7", 1),
                    new DaoGuidanceContent.Objective("step_8", "read:qi_blast:step_8", 1),
                    new DaoGuidanceContent.Objective("step_9", "read:qi_blast:step_9", 1),
                    new DaoGuidanceContent.Objective("step_10", "read:qi_blast:step_10", 1),
                    new DaoGuidanceContent.Objective("step_11", "read:qi_blast:step_11", 1),
                    new DaoGuidanceContent.Objective("step_12", "read:qi_blast:step_12", 1),
                    new DaoGuidanceContent.Objective("step_13", "read:qi_blast:step_13", 1),
                    new DaoGuidanceContent.Objective("step_14", "read:qi_blast:step_14", 1),
                    new DaoGuidanceContent.Objective("step_15", "read:qi_blast:step_15", 1),
                    new DaoGuidanceContent.Objective("step_16", "read:qi_blast:step_16", 1),
                    new DaoGuidanceContent.Objective("step_17", "read:qi_blast:step_17", 1),
                    new DaoGuidanceContent.Objective("step_18", "read:qi_blast:step_18", 1),
                    new DaoGuidanceContent.Objective("step_19", "read:qi_blast:step_19", 1),
                    new DaoGuidanceContent.Objective("step_20", "read:qi_blast:step_20", 1),
                    new DaoGuidanceContent.Objective("step_21", "read:qi_blast:step_21", 1),
                    new DaoGuidanceContent.Objective("step_22", "read:qi_blast:step_22", 1))),
            new DaoGuidanceContent.Chapter("weapon_binding", true, List.of(
                    new DaoGuidanceContent.Objective("step_1", "read:weapon_binding:step_1", 1),
                    new DaoGuidanceContent.Objective("step_2", "read:weapon_binding:step_2", 1),
                    new DaoGuidanceContent.Objective("step_3", "read:weapon_binding:step_3", 1))),
            new DaoGuidanceContent.Chapter("sword_control", true, List.of(
                    new DaoGuidanceContent.Objective("step_1", "read:sword_control:step_1", 1),
                    new DaoGuidanceContent.Objective("step_2", "read:sword_control:step_2", 1),
                    new DaoGuidanceContent.Objective("step_3", "read:sword_control:step_3", 1))),
            new DaoGuidanceContent.Chapter("life_card_battle", true, List.of(
                    new DaoGuidanceContent.Objective("step_1", "read:life_card_battle:step_1", 1),
                    new DaoGuidanceContent.Objective("step_2", "read:life_card_battle:step_2", 1),
                    new DaoGuidanceContent.Objective("step_3", "read:life_card_battle:step_3", 1),
                    new DaoGuidanceContent.Objective("step_4", "read:life_card_battle:step_4", 1),
                    new DaoGuidanceContent.Objective("step_5", "read:life_card_battle:step_5", 1))),
            new DaoGuidanceContent.Chapter("annual_plans", true, List.of(
                    new DaoGuidanceContent.Objective("step_1", "read:annual_plans:step_1", 1),
                    new DaoGuidanceContent.Objective("step_2", "read:annual_plans:step_2", 1),
                    new DaoGuidanceContent.Objective("step_3", "read:annual_plans:step_3", 1))),
            new DaoGuidanceContent.Chapter("life_inheritance", true, List.of(
                    new DaoGuidanceContent.Objective("step_1", "read:life_inheritance:step_1", 1),
                    new DaoGuidanceContent.Objective("step_2", "read:life_inheritance:step_2", 1),
                    new DaoGuidanceContent.Objective("step_3", "read:life_inheritance:step_3", 1))),
            new DaoGuidanceContent.Chapter("npc_growth", true, List.of(
                    new DaoGuidanceContent.Objective("step_1", "read:npc_growth:step_1", 1),
                    new DaoGuidanceContent.Objective("step_2", "read:npc_growth:step_2", 1),
                    new DaoGuidanceContent.Objective("step_3", "read:npc_growth:step_3", 1),
                    new DaoGuidanceContent.Objective("step_4", "read:npc_growth:step_4", 1))),
            new DaoGuidanceContent.Chapter("exploration_tools", true, List.of(
                    new DaoGuidanceContent.Objective("step_1", "read:exploration_tools:step_1", 1),
                    new DaoGuidanceContent.Objective("step_2", "read:exploration_tools:step_2", 1),
                    new DaoGuidanceContent.Objective("step_3", "read:exploration_tools:step_3", 1))),
            new DaoGuidanceContent.Chapter("annihilation_curse", true, List.of(
                    new DaoGuidanceContent.Objective("step_1", "read:annihilation_curse:step_1", 1),
                    new DaoGuidanceContent.Objective("step_2", "read:annihilation_curse:step_2", 1),
                    new DaoGuidanceContent.Objective("step_3", "read:annihilation_curse:step_3", 1),
                    new DaoGuidanceContent.Objective("step_4", "read:annihilation_curse:step_4", 1),
                    new DaoGuidanceContent.Objective("step_5", "read:annihilation_curse:step_5", 1))),
            new DaoGuidanceContent.Chapter("settings_controls", true, List.of(
                    new DaoGuidanceContent.Objective("step_1", "read:settings_controls:step_1", 1),
                    new DaoGuidanceContent.Objective("step_2", "read:settings_controls:step_2", 1),
                    new DaoGuidanceContent.Objective("step_3", "read:settings_controls:step_3", 1),
                    new DaoGuidanceContent.Objective("step_4", "read:settings_controls:step_4", 1),
                    new DaoGuidanceContent.Objective("step_5", "read:settings_controls:step_5", 1),
                    new DaoGuidanceContent.Objective("step_6", "read:settings_controls:step_6", 1),
                    new DaoGuidanceContent.Objective("step_7", "read:settings_controls:step_7", 1))));
    public static List<DaoGuidanceContent.Chapter> chapters() { return CHAPTERS; }

    /** Discovery explains a feature; only an explicit acknowledgement completes a reading. */
    public static List<String> relatedChapters(String action) {
        return switch (action) {
            case "open_stats" -> List.of("settings_controls");
            case "external_qi", "reach_jindan" -> List.of("qi_blast");
            case "weapon_attack", "weapon_technique", "guide_restriction_added" -> List.of("weapon_binding");
            case "start_flight", "recall_weapon", "flight_tick" -> List.of("sword_control");
            case "guide_life_started", "guide_life_choice", "guide_life_event" -> List.of("life_card_battle", "annual_plans", "life_inheritance");
            case "guide_life_settled", "guide_life_finished" -> List.of("life_inheritance");
            case "talk_npc", "trade_npc", "accept_quest" -> List.of("npc_growth");
            case "detect_vein", "reach_vein" -> List.of("exploration_tools");
            case "annihilation_acquired", "annihilation_infected" -> List.of("annihilation_curse");
            default -> List.of();
        };
    }
}
