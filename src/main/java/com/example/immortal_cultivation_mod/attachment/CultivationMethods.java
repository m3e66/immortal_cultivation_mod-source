package com.example.immortal_cultivation_mod.attachment;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;

public class CultivationMethods {
    public static final String NONE = "";
    public static final String BASIC_BREATHING = "basic_breathing";
    public static final String CLEAR_HEART = "clear_heart";
    public static final String BLOOD_DEMON_JINDAN = "blood_demon_jindan";
    public static final String BLOOD_DEMON_YUANYING = "blood_demon_yuanying";
    public static final String BLOOD_DEMON_HUASHEN = "blood_demon_huashen";
    public static final String REINCARNATION_TRUE_ART = "reincarnation_true_art";
    public static final String TUNTIAN_DEMON_ART = "tuntian_demon_art";
    public static final String POKONG_JUE = "pokong_jue";
    public static final String CHANGQING_JUE = "changqing_jue";
    public static final String FENTIAN_LIFE_RENEWAL = "fentian_life_renewal";
    public static final String HANTI_BINGQIN = "hanti_bingqin";

    public record MethodDef(String id, String nameKey, String element, String tier, String limitLevel, boolean bloodDemon) {}

    private static final Map<String, MethodDef> METHODS = Map.ofEntries(
            Map.entry(
            BASIC_BREATHING, new MethodDef(BASIC_BREATHING, "method.immortal_cultivation_mod.basic_breathing", "any", "human",
                    CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_LATE, false)),
            Map.entry(
            CLEAR_HEART, new MethodDef(CLEAR_HEART, "method.immortal_cultivation_mod.clear_heart", "any", "earth",
                    CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_LATE, false)),
            Map.entry(
            BLOOD_DEMON_JINDAN, new MethodDef(BLOOD_DEMON_JINDAN, "method.immortal_cultivation_mod.blood_demon_jindan", "any", "human",
                    CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_LATE, true)),
            Map.entry(
            BLOOD_DEMON_YUANYING, new MethodDef(BLOOD_DEMON_YUANYING, "method.immortal_cultivation_mod.blood_demon_yuanying", "any", "earth",
                    CultivationLevels.REALM_YUANYING + CultivationLevels.STAGE_LATE, true)),
            Map.entry(
            BLOOD_DEMON_HUASHEN, new MethodDef(BLOOD_DEMON_HUASHEN, "method.immortal_cultivation_mod.blood_demon_huashen", "any", "heaven",
                    CultivationLevels.REALM_HUASHEN + CultivationLevels.STAGE_LATE, true)),
            Map.entry(
            REINCARNATION_TRUE_ART, new MethodDef(REINCARNATION_TRUE_ART, "method.immortal_cultivation_mod.reincarnation_true_art", SpiritRoots.DARK, "heaven",
                    CultivationLevels.REALM_HUASHEN + CultivationLevels.STAGE_LATE, false)),
            Map.entry(
            TUNTIAN_DEMON_ART, new MethodDef(TUNTIAN_DEMON_ART, "method.immortal_cultivation_mod.tuntian_demon_art", SpiritRoots.DARK, "heaven",
                    CultivationLevels.REALM_HUASHEN + CultivationLevels.STAGE_LATE, false)),
            Map.entry(
            POKONG_JUE, new MethodDef(POKONG_JUE, "method.immortal_cultivation_mod.pokong_jue", SpiritRoots.WIND, "earth",
                    CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_LATE, false)),
            Map.entry(
            CHANGQING_JUE, new MethodDef(CHANGQING_JUE, "method.immortal_cultivation_mod.changqing_jue", SpiritRoots.WATER + "," + SpiritRoots.EARTH, "earth",
                    CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_LATE, false)),
            Map.entry(
            FENTIAN_LIFE_RENEWAL, new MethodDef(FENTIAN_LIFE_RENEWAL, "method.immortal_cultivation_mod.fentian_life_renewal", SpiritRoots.FIRE, "earth",
                    CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_LATE, false)),
            Map.entry(
            HANTI_BINGQIN, new MethodDef(HANTI_BINGQIN, "method.immortal_cultivation_mod.hanti_bingqin", SpiritRoots.ICE, "heaven",
                    CultivationLevels.REALM_HUASHEN + CultivationLevels.STAGE_LATE, false))
    );

    public static MethodDef get(String id) {
        return METHODS.get(id);
    }

    public static List<MethodDef> allMethods() {
        return List.of(
                METHODS.get(BASIC_BREATHING),
                METHODS.get(CLEAR_HEART),
                METHODS.get(BLOOD_DEMON_JINDAN),
                METHODS.get(BLOOD_DEMON_YUANYING),
                METHODS.get(BLOOD_DEMON_HUASHEN),
                METHODS.get(REINCARNATION_TRUE_ART),
                METHODS.get(TUNTIAN_DEMON_ART),
                METHODS.get(POKONG_JUE),
                METHODS.get(CHANGQING_JUE),
                METHODS.get(FENTIAN_LIFE_RENEWAL),
                METHODS.get(HANTI_BINGQIN)
        );
    }

    public static Component methodProficiencyDescription(ModAttachments.CultivationData data, String methodId) {
        int uses = data == null ? 0 : data.methodProficiency(methodId);
        int threshold = nextMethodProficiencyThreshold(uses);
        return Component.translatable("screen." + ImmortalCultivationMod.MODID + ".method_proficiency",
                methodProficiencyLevel(data, methodId), uses, threshold);
    }

    public static Component methodProficiencyLevel(ModAttachments.CultivationData data, String methodId) {
        int uses = data == null ? 0 : data.methodProficiency(methodId);
        return Component.translatable("screen." + ImmortalCultivationMod.MODID + "." + methodProficiencyTierKey(uses));
    }

    private static String methodProficiencyTierKey(int uses) {
        if (uses >= 10_000) {
            return "spell_proficiency_peak";
        }
        if (uses >= 1_000) {
            return "spell_proficiency_perfected";
        }
        if (uses >= 500) {
            return "spell_proficiency_great";
        }
        return uses >= 100 ? "spell_proficiency_minor" : "spell_proficiency_beginner";
    }

    private static int nextMethodProficiencyThreshold(int uses) {
        if (uses >= 10_000) {
            return 10_000;
        }
        if (uses >= 1_000) {
            return 10_000;
        }
        if (uses >= 500) {
            return 1_000;
        }
        return uses >= 100 ? 500 : 100;
    }

    public static boolean isBloodDemon(String id) {
        MethodDef def = get(id);
        return def != null && def.bloodDemon();
    }

    public static boolean isReincarnationTrueArt(String id) {
        return REINCARNATION_TRUE_ART.equals(id);
    }

    public static boolean isTuntianDemonArt(String id) {
        return TUNTIAN_DEMON_ART.equals(id);
    }

    public static boolean isPokongJue(String id) {
        return POKONG_JUE.equals(id);
    }

    public static boolean isChangqingJue(String id) {
        return CHANGQING_JUE.equals(id);
    }

    public static boolean isFentianLifeRenewal(String id) {
        return FENTIAN_LIFE_RENEWAL.equals(id);
    }

    public static boolean isHantiBingqin(String id) {
        return HANTI_BINGQIN.equals(id);
    }

    public static boolean canGainProgress(String methodId, String cultivationLevel) {
        MethodDef def = get(methodId);
        if (def == null) {
            return false;
        }
        return CultivationLevels.getStageIndex(cultivationLevel) < CultivationLevels.getStageIndex(def.limitLevel());
    }

    public static boolean isAtOrPastLimit(String methodId, String cultivationLevel) {
        MethodDef def = get(methodId);
        if (def == null) {
            return true;
        }
        return CultivationLevels.getStageIndex(cultivationLevel) >= CultivationLevels.getStageIndex(def.limitLevel());
    }

    public static int progressMultiplierPercent(ModAttachments.CultivationData data) {
        MethodDef def = get(data.activeCultivationMethod());
        if (def == null) {
            return 0;
        }
        if ("any".equals(def.element())) {
            return switch (def.tier()) {
                case "earth" -> 120;
                case "heaven" -> 150;
                case "immortal" -> 200;
                case "divine" -> 300;
                default -> 100;
            };
        }
        List<String> elements = List.of(def.element().split(","));
        if (data.spiritRoots().size() == 1 && elements.contains(data.spiritRoots().getFirst())) {
            return 200;
        }
        if (data.spiritRoots().stream().anyMatch(elements::contains)) {
            return 100;
        }
        return 10;
    }
}
