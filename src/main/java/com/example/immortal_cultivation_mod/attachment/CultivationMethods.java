package com.example.immortal_cultivation_mod.attachment;

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

    public record MethodDef(String id, String nameKey, String element, String tier, String limitLevel, boolean bloodDemon) {}

    private static final Map<String, MethodDef> METHODS = Map.of(
            BASIC_BREATHING, new MethodDef(BASIC_BREATHING, "method.immortal_cultivation_mod.basic_breathing", "any", "human",
                    CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_LATE, false),
            CLEAR_HEART, new MethodDef(CLEAR_HEART, "method.immortal_cultivation_mod.clear_heart", "any", "earth",
                    CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_LATE, false),
            BLOOD_DEMON_JINDAN, new MethodDef(BLOOD_DEMON_JINDAN, "method.immortal_cultivation_mod.blood_demon_jindan", "any", "human",
                    CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_LATE, true),
            BLOOD_DEMON_YUANYING, new MethodDef(BLOOD_DEMON_YUANYING, "method.immortal_cultivation_mod.blood_demon_yuanying", "any", "earth",
                    CultivationLevels.REALM_YUANYING + CultivationLevels.STAGE_LATE, true),
            BLOOD_DEMON_HUASHEN, new MethodDef(BLOOD_DEMON_HUASHEN, "method.immortal_cultivation_mod.blood_demon_huashen", "any", "heaven",
                    CultivationLevels.REALM_HUASHEN + CultivationLevels.STAGE_LATE, true),
            REINCARNATION_TRUE_ART, new MethodDef(REINCARNATION_TRUE_ART, "method.immortal_cultivation_mod.reincarnation_true_art", SpiritRoots.DARK, "heaven",
                    CultivationLevels.REALM_HUASHEN + CultivationLevels.STAGE_LATE, false),
            TUNTIAN_DEMON_ART, new MethodDef(TUNTIAN_DEMON_ART, "method.immortal_cultivation_mod.tuntian_demon_art", SpiritRoots.DARK, "heaven",
                    CultivationLevels.REALM_HUASHEN + CultivationLevels.STAGE_LATE, false)
    );

    public static MethodDef get(String id) {
        return METHODS.get(id);
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
        if (data.spiritRoots().size() == 1 && data.spiritRoots().contains(def.element())) {
            return 200;
        }
        if (data.spiritRoots().contains(def.element())) {
            return 100;
        }
        return 10;
    }
}
