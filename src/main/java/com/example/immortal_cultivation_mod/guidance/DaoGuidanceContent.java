package com.example.immortal_cultivation_mod.guidance;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared, data-only definition of the 引道 progression.  Both sides use the
 * same ordered definitions; completion itself remains server authoritative.
 */
public final class DaoGuidanceContent {
    public static final String FIND_ENLIGHTENMENT_PILL =
            "find_enlightenment_pill";
    public static final String OPEN_STATS = "open_stats";
    public static final String ENTER_LIANQI = "enter_lianqi";
    public static final String MEDITATE_QI = "meditate_qi";
    public static final String ACTIVATE_METHOD = "activate_method";
    public static final String LEARN_SPELL = "learn_spell";
    public static final String EQUIP_SPELL = "equip_spell";
    public static final String CAST_SPELL = "cast_spell";
    public static final String CHARGED_SPELL = "charged_spell";
    public static final String ACTIVATE_SENSE = "activate_sense";
    public static final String DETECT_HIDDEN_LIVING = "detect_hidden_living";
    public static final String WEAPON_ATTACK = "weapon_attack";
    public static final String WEAPON_TECHNIQUE = "weapon_technique";
    public static final String RECOGNIZE_ATTACK = "recognize_attack";
    public static final String EXTERNAL_QI = "external_qi";
    public static final String MANAGE_QI = "manage_qi";
    public static final String EQUIP_ITEM = "equip_item";
    public static final String OPEN_POUCH = "open_pouch";
    public static final String POUCH_PLACE = "pouch_place";
    public static final String POUCH_REMOVE = "pouch_remove";
    public static final String DETECT_VEIN = "detect_vein";
    public static final String REACH_VEIN = "reach_vein";
    public static final String OPEN_DING = "open_ding";
    public static final String FORGE_MATERIAL = "forge_material";
    public static final String FORGE_MOLD = "forge_mold";
    public static final String FORGE_HEAT = "forge_heat";
    public static final String FORGE_MELT = "forge_melt";
    public static final String FORGE_COMPLETE = "forge_complete";
    public static final String READ_RECIPE_SLIP = "read_recipe_slip";
    public static final String PLACE_CENTER = "place_center";
    public static final String BUILD_PATTERN = "build_pattern";
    public static final String ACTIVATE_FORMATION = "activate_formation";
    public static final String OPEN_FLAG_GUI = "open_flag_gui";
    public static final String INSERT_FLAG = "insert_flag";
    public static final String TALK_NPC = "talk_npc";
    public static final String TRADE_NPC = "trade_npc";
    public static final String ACCEPT_QUEST = "accept_quest";
    public static final String BREAKTHROUGH_READY = "breakthrough_ready";
    public static final String BREAKTHROUGH_ATTEMPT = "breakthrough_attempt";
    public static final String BREAKTHROUGH_SUCCESS = "breakthrough_success";
    public static final String REACH_ZHUJI = "reach_zhuji";
    public static final String FOUNDATION_CONTROL = "foundation_control";
    public static final String START_FLIGHT = "start_flight";
    public static final String FLIGHT_TICK = "flight_tick";
    public static final String RECALL_WEAPON = "recall_weapon";
    public static final String REACH_JINDAN = "reach_jindan";
    public static final String JINDAN_SENSE = "jindan_sense";
    public static final String ENTER_INNER_SPACE = "enter_inner_space";
    public static final String DISCOVER_CORPSE_PATH = "discover_corpse_path";

    public static final String ENLIGHTENMENT = "enlightenment";
    public static final String INITIAL_PATH = "initial_path";
    public static final String QI_ENTRY = "qi_entry";
    public static final String FIRST_SPELLS = "first_spells";
    public static final String SPIRITUAL_SENSE = "spiritual_sense";
    public static final String CULTIVATOR_COMBAT = "cultivator_combat";
    public static final String STORAGE_EQUIPMENT = "storage_equipment";
    public static final String SPIRIT_VEIN = "spirit_vein";
    public static final String FORGING = "forging";
    public static final String JADE_SLIPS = "jade_slips";
    public static final String FORMATIONS = "formations";
    public static final String FLAGS = "flags";
    public static final String NPC_WORLD = "npc_world";
    public static final String BREAKTHROUGH = "breakthrough";
    public static final String FOUNDATION = "foundation";
    public static final String FLIGHT = "flight";
    public static final String JINDAN = "jindan";
    public static final String CORPSE_PATH = "corpse_path";

    private static final List<Chapter> CHAPTERS = List.of(
            chapter(ENLIGHTENMENT,
                    objective("find_pill", FIND_ENLIGHTENMENT_PILL, 1)),
            chapter(INITIAL_PATH,
                    objective("check_self", OPEN_STATS, 1),
                    objective("enter_lianqi", ENTER_LIANQI, 1)),
            chapter(QI_ENTRY,
                    objective("absorb_qi", MEDITATE_QI, 100),
                    objective("run_method", ACTIVATE_METHOD, 1)),
            chapter(FIRST_SPELLS,
                    objective("learn_spell", LEARN_SPELL, 1),
                    objective("equip_spell", EQUIP_SPELL, 1),
                    objective("cast_spell", CAST_SPELL, 1),
                    objective("charged_spell", CHARGED_SPELL, 1),
                    objective("proficiency", CAST_SPELL, 5)),
            chapter(SPIRITUAL_SENSE,
                    objective("release_sense", ACTIVATE_SENSE, 1),
                    objective("sense_hidden_living", DETECT_HIDDEN_LIVING, 1)),
            chapter(CULTIVATOR_COMBAT,
                    objective("weapon_attack", WEAPON_ATTACK, 1),
                    objective("weapon_technique", WEAPON_TECHNIQUE, 1),
                    objective("combat_spell", CAST_SPELL, 1),
                    objective("combat_charge", CHARGED_SPELL, 1),
                    objective("qi_management", MANAGE_QI, 1),
                    objective("recognize_attack", RECOGNIZE_ATTACK, 1)),
            chapter(STORAGE_EQUIPMENT,
                    objective("equip_item", EQUIP_ITEM, 1),
                    objective("equip_accessory", "guide_accessory_equipped", 1),
                    objective("open_pouch", OPEN_POUCH, 1),
                    objective("pouch_place", POUCH_PLACE, 1),
                    objective("pouch_remove", POUCH_REMOVE, 1)),
            chapter(SPIRIT_VEIN,
                    objective("detect_vein", DETECT_VEIN, 1),
                    objective("reach_vein", REACH_VEIN, 1)),
            chapter(FORGING,
                    objective("open_ding", OPEN_DING, 1),
                    objective("forge_material", FORGE_MATERIAL, 1),
                    objective("forge_mold", FORGE_MOLD, 1),
                    objective("check_flame", "read:forging:check_flame", 1),
                    objective("supply_flame", "guide_forge_fire", 1),
                    objective("forge_heat", FORGE_HEAT, 1),
                    objective("forge_melt", FORGE_MELT, 1),
                    objective("forge_complete", FORGE_COMPLETE, 1)),
            chapter(JADE_SLIPS,
                    objective("read_recipe_slip", READ_RECIPE_SLIP, 1)),
            chapter(FORMATIONS,
                    objective("place_center", PLACE_CENTER, 1),
                    objective("build_pattern", BUILD_PATTERN, 1),
                    objective("activate_formation", ACTIVATE_FORMATION, 1)),
            chapter(FLAGS,
                    objective("open_flag_gui", OPEN_FLAG_GUI, 1),
                    objective("insert_flag", INSERT_FLAG, 1)),
            chapter(NPC_WORLD,
                    objective("talk_npc", TALK_NPC, 1),
                    objective("trade_npc", TRADE_NPC, 1),
                    objective("accept_quest", ACCEPT_QUEST, 1)),
            chapter(BREAKTHROUGH,
                    objective("breakthrough_ready", BREAKTHROUGH_READY, 1),
                    objective("breakthrough_attempt", BREAKTHROUGH_ATTEMPT, 1),
                    objective("breakthrough_success", BREAKTHROUGH_SUCCESS, 1),
                    objective("consequences", "read:breakthrough:consequences", 1)),
            chapter(FOUNDATION,
                    objective("reach_zhuji", REACH_ZHUJI, 1),
                    objective("external_qi", EXTERNAL_QI, 1),
                    objective("foundation_control", FOUNDATION_CONTROL, 1)),
            chapter(FLIGHT,
                    objective("start_flight", START_FLIGHT, 1),
                    objective("sustain_flight", FLIGHT_TICK, 10),
                    objective("recall_weapon", RECALL_WEAPON, 1)),
            chapter(JINDAN,
                    objective("reach_jindan", REACH_JINDAN, 1),
                    objective("jindan_sense", JINDAN_SENSE, 1),
                    objective("inner_space", ENTER_INNER_SPACE, 1)),
            new Chapter(CORPSE_PATH, true, List.of(
                    objective("discover_corpse_path", DISCOVER_CORPSE_PATH, 1)))
    );

    private static final Map<String, Chapter> BY_ID;

    static {
        Map<String, Chapter> chapters = new LinkedHashMap<>();
        for (Chapter chapter : chapters()) chapters.put(chapter.id(), chapter);
        BY_ID = Map.copyOf(chapters);
    }

    private DaoGuidanceContent() {
    }

    private static Chapter chapter(String id, Objective... objectives) {
        return new Chapter(id, false, List.of(objectives));
    }

    private static Objective objective(String id, String action, int target) {
        return new Objective(id, action, Math.max(1, target));
    }

    public static List<Chapter> chapters() {
        return java.util.stream.Stream.concat(CHAPTERS.stream(),
                DaoGuidanceExpansion.chapters().stream()).toList();
    }

    public static Chapter chapter(String id) {
        return BY_ID.get(id);
    }

    public static String chapterKey(String chapterId, String suffix) {
        return "guidance." + ImmortalCultivationMod.MODID + ".chapter."
                + chapterId + "." + suffix;
    }

    public static String objectiveKey(String chapterId, String objectiveId,
                                      String suffix) {
        return "guidance." + ImmortalCultivationMod.MODID + ".chapter."
                + chapterId + ".objective." + objectiveId + "." + suffix;
    }

    public record Objective(String id, String action, int target) {
    }

    public record Chapter(String id, boolean secret, List<Objective> objectives) {
        public Chapter {
            objectives = List.copyOf(objectives);
        }
    }
}
