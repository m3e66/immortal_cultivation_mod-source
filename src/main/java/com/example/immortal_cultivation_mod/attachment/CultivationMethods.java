package com.example.immortal_cultivation_mod.attachment;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.HashMap;
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
    public static final String CUIGU_LIANSHEN_JUE = "cuigu_lianshen_jue";
    public static final String JIUYOU_LIANSHI_JUE = "jiuyou_lianshi_jue";
    public static final String DARI_BATI_JUE = "dari_bati_jue";
    public static final String GANGTING_XIAOKONG_LU = "gangting_xiaokong_lu";
    public static final String XUANYUE_SUIFENG_JUE = "xuanyue_suifeng_jue";
    public static final String HANLAN_NINGYUAN_JING = "hanlan_ningyuan_jing";
    /** Stored as hundredths of one percent inside the already-persistent proficiency map. */
    private static final String SOLAR_POWER_KEY = "__dari_solar_power";
    /** Stored as hundredths of one point inside the already-persistent proficiency map. */
    private static final String FENGLEI_MOMENTUM_KEY = "__gangting_fenglei_momentum";
    /** Stored as hundredths of one point inside the already-persistent proficiency map. */
    private static final String CHANGQING_LIFE_FORCE_KEY = "__changqing_life_force";

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
            CHANGQING_JUE, new MethodDef(CHANGQING_JUE, "method.immortal_cultivation_mod.changqing_jue", SpiritRoots.WOOD, "earth",
                    CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_LATE, false)),
            Map.entry(
            FENTIAN_LIFE_RENEWAL, new MethodDef(FENTIAN_LIFE_RENEWAL, "method.immortal_cultivation_mod.fentian_life_renewal", SpiritRoots.FIRE, "earth",
                    CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_LATE, false)),
            Map.entry(
            HANTI_BINGQIN, new MethodDef(HANTI_BINGQIN, "method.immortal_cultivation_mod.hanti_bingqin", SpiritRoots.ICE, "heaven",
                    CultivationLevels.REALM_HUASHEN + CultivationLevels.STAGE_LATE, false)),
            Map.entry(
            CUIGU_LIANSHEN_JUE, new MethodDef(CUIGU_LIANSHEN_JUE, "method.immortal_cultivation_mod.cuigu_lianshen_jue", "none", "heaven",
                    CultivationLevels.REALM_HUASHEN + CultivationLevels.STAGE_EARLY, false)),
            Map.entry(
            JIUYOU_LIANSHI_JUE, new MethodDef(JIUYOU_LIANSHI_JUE, "method.immortal_cultivation_mod.jiuyou_lianshi_jue", SpiritRoots.DARK, "heaven",
                    CultivationLevels.REALM_HUASHEN + CultivationLevels.STAGE_EARLY, false)),
            Map.entry(
            DARI_BATI_JUE, new MethodDef(DARI_BATI_JUE, "method.immortal_cultivation_mod.dari_bati_jue", SpiritRoots.FIRE + "," + SpiritRoots.LIGHT, "heaven",
                    CultivationLevels.REALM_HUASHEN + CultivationLevels.STAGE_LATE, false)),
            Map.entry(
            GANGTING_XIAOKONG_LU, new MethodDef(GANGTING_XIAOKONG_LU, "method.immortal_cultivation_mod.gangting_xiaokong_lu", SpiritRoots.WIND + "," + SpiritRoots.THUNDER, "heaven",
                    CultivationLevels.REALM_YUANYING + CultivationLevels.STAGE_LATE, false)),
            Map.entry(
            XUANYUE_SUIFENG_JUE, new MethodDef(XUANYUE_SUIFENG_JUE, "method.immortal_cultivation_mod.xuanyue_suifeng_jue", SpiritRoots.METAL + "," + SpiritRoots.EARTH, "heaven",
                    CultivationLevels.REALM_HUASHEN + CultivationLevels.STAGE_EARLY, false)),
            Map.entry(
            HANLAN_NINGYUAN_JING, new MethodDef(HANLAN_NINGYUAN_JING, "method.immortal_cultivation_mod.hanlan_ningyuan_jing", SpiritRoots.WATER + "," + SpiritRoots.ICE, "heaven",
                    CultivationLevels.REALM_YUANYING + CultivationLevels.STAGE_LATE, false))
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
                METHODS.get(HANTI_BINGQIN),
                METHODS.get(CUIGU_LIANSHEN_JUE),
                METHODS.get(JIUYOU_LIANSHI_JUE),
                METHODS.get(DARI_BATI_JUE),
                METHODS.get(GANGTING_XIAOKONG_LU),
                METHODS.get(XUANYUE_SUIFENG_JUE),
                METHODS.get(HANLAN_NINGYUAN_JING)
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

    public static boolean isCuiguLianshenJue(String id) {
        return CUIGU_LIANSHEN_JUE.equals(id);
    }

    public static boolean isJiuyouLianshiJue(String id) {
        return JIUYOU_LIANSHI_JUE.equals(id);
    }

    public static boolean isDariBatiJue(String id) {
        return DARI_BATI_JUE.equals(id);
    }

    public static boolean isGangtingXiaokongLu(String id) {
        return GANGTING_XIAOKONG_LU.equals(id);
    }

    public static boolean isXuanyueSuifengJue(String id) {
        return XUANYUE_SUIFENG_JUE.equals(id);
    }

    public static boolean isHanlanNingyuanJing(String id) {
        return HANLAN_NINGYUAN_JING.equals(id);
    }

    public static float solarPower(ModAttachments.CultivationData data) {
        if (data == null) {
            return 0.0F;
        }
        return Math.max(0, Math.min(10_000,
                data.methodProficiencies().getOrDefault(SOLAR_POWER_KEY, 0))) / 100.0F;
    }

    public static ModAttachments.CultivationData withSolarPower(
            ModAttachments.CultivationData data, float solarPower) {
        Map<String, Integer> updated = new HashMap<>(data.methodProficiencies());
        updated.put(SOLAR_POWER_KEY, Math.max(0, Math.min(10_000,
                Math.round(solarPower * 100.0F))));
        return data.withMethodProficiencies(updated);
    }

    public static float fengleiMomentum(ModAttachments.CultivationData data) {
        if (data == null) {
            return 0.0F;
        }
        return Math.max(0, Math.min(10_000,
                data.methodProficiencies().getOrDefault(FENGLEI_MOMENTUM_KEY, 0))) / 100.0F;
    }

    public static ModAttachments.CultivationData withFengleiMomentum(
            ModAttachments.CultivationData data, float momentum) {
        Map<String, Integer> updated = new HashMap<>(data.methodProficiencies());
        updated.put(FENGLEI_MOMENTUM_KEY, Math.max(0, Math.min(10_000,
                Math.round(momentum * 100.0F))));
        return data.withMethodProficiencies(updated);
    }

    public static float changqingLifeForce(ModAttachments.CultivationData data) {
        if (data == null) {
            return 0.0F;
        }
        return Math.max(0, Math.min(10_000,
                data.methodProficiencies().getOrDefault(CHANGQING_LIFE_FORCE_KEY, 0))) / 100.0F;
    }

    public static ModAttachments.CultivationData withChangqingLifeForce(
            ModAttachments.CultivationData data, float lifeForce) {
        Map<String, Integer> updated = new HashMap<>(data.methodProficiencies());
        updated.put(CHANGQING_LIFE_FORCE_KEY, Math.max(0, Math.min(10_000,
                Math.round(lifeForce * 100.0F))));
        return data.withMethodProficiencies(updated);
    }

    public static int solarPowerCap(ModAttachments.CultivationData data) {
        int proficiency = data == null ? 0 : data.methodProficiency(DARI_BATI_JUE);
        if (proficiency >= 500) {
            return 100;
        }
        return proficiency >= 100 ? 80 : 60;
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
        if ("any".equals(def.element()) || "none".equals(def.element())) {
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
