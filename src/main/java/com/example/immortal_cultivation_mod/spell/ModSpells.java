package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModSpells {
    public static final String FIREBALL = "fireball";
    public static final String LINGBENG = "lingbeng";
    public static final String REGENERATION = "regeneration";
    public static final String BEAM = "beam";
    public static final String EARTH_ESCAPE = "earth_escape";
    public static final String CLEANSE = "cleanse";
    public static final String QI_GATHERING = "qi_gathering";
    public static final String IGNITE_FLARE = "ignite_flare";
    public static final String SPIRIT_SIGHT = "spirit_sight";
    public static final String ZHENSHAN_PALM = "zhenshan_palm";
    public static final String LIGHT_BEAM_ATTACK = "light_beam_attack";
    public static final String DIELANG_SHIELD = "dielang_shield";
    public static final String LINGZHI_BULLET = "lingzhi_bullet";
    public static final String WIND_BLADE = "wind_blade";
    public static final String WIND_STEP = "wind_step";
    public static final String YUFENG_JUE = "yufeng_jue";
    public static final String SMOKE_ART = "smoke_art";
    public static final String HUTI_QI = "huti_qi";
    public static final String MICHEN_ZHANG = "michen_zhang";
    public static final String SLIDING_WATER = "sliding_water";
    public static final String WEIYA = "weiya";
    public static final String ZIBAO = "zibao";
    public static final String ABSORB_CULTIVATION = "absorb_cultivation";
    public static final String TUNTIAN = "tuntian";
    public static final String FENGYA = "fengya";
    public static final String DINGSHEN = "dingshen";
    public static final String YINLEI_JUE = "yinlei_jue";
    public static final String WULEI_ZHENGFA = "wulei_zhengfa";
    public static final String LIUGUANG_JIANYING = "liuguang_jianying";
    public static final String SIFANG_JIE = "sifang_jie";
    public static final String GUSHI_SHIELD = "gushi_shield";
    public static final String KONGSHI_SHU = "kongshi_shu";
    public static final String HANJING_SUOZHUA = "hanjing_suozhua";
    public static final String SHUANGTIAN_QI = "shuangtian_qi";
    public static final String SUISHUANG_LINGXIAO = "suishuang_lingxiao";
    public static final String YUQI_SHU = "yuqi_shu";
    public static final String XUYING_TA = "xuying_ta";
    public static final String DUANLIU_KONGDUN = "duanliu_kongdun";
    public static final String YIHEN_CI = "yihen_ci";

    private static final Map<String, SpellDef> SPELLS = createSpells();

    private static Map<String, SpellDef> createSpells() {
        Map<String, SpellDef> spells = new LinkedHashMap<>();
        spells.put(FIREBALL, new SpellDef(
                FIREBALL,
                "fireball",
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY,
                10,
                SpiritRoots.FIRE,
                "human",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_fireball.png")
        ));
        spells.put(WEIYA, new SpellDef(
                WEIYA,
                "weiya",
                CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY,
                0,
                SpiritRoots.DARK,
                "human",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_lingbeng.png")
        ));
        spells.put(ZIBAO, new SpellDef(
                ZIBAO,
                "zibao",
                CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY,
                0,
                "",
                "human",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_ignite_flare.png")
        ));
        spells.put(ABSORB_CULTIVATION, new SpellDef(
                ABSORB_CULTIVATION,
                "absorb_cultivation",
                "",
                80,
                SpiritRoots.DARK,
                "heaven",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_lingbeng.png")
        ));
        spells.put(TUNTIAN, new SpellDef(
                TUNTIAN,
                "tuntian",
                "",
                0,
                SpiritRoots.DARK,
                "heaven",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_tuntian.png")
        ));
        spells.put(FENGYA, new SpellDef(
                FENGYA,
                "fengya",
                "",
                0,
                SpiritRoots.WIND,
                "earth",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_fengya.png")
        ));
        spells.put(DINGSHEN, new SpellDef(
                DINGSHEN,
                "dingshen",
                CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_LATE,
                200,
                SpiritRoots.METAL,
                "earth",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_dingshen.png")
        ));
        spells.put(LINGBENG, new SpellDef(
                LINGBENG,
                "lingbeng",
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_LATE,
                85,
                SpiritRoots.THUNDER,
                "human",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_lingbeng.png")
        ));
        spells.put(YINLEI_JUE, new SpellDef(
                YINLEI_JUE,
                "yinlei_jue",
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_LATE,
                40,
                SpiritRoots.THUNDER,
                "human",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_yinlei_jue.png")
        ));
        spells.put(WULEI_ZHENGFA, new SpellDef(
                WULEI_ZHENGFA,
                "wulei_zhengfa",
                CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_MID,
                120,
                SpiritRoots.THUNDER,
                "earth",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_wulei_zhengfa.png")
        ));
        spells.put(LIUGUANG_JIANYING, new SpellDef(
                LIUGUANG_JIANYING,
                "liuguang_jianying",
                CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY,
                300,
                SpiritRoots.METAL,
                "earth",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_spirit_sight.png")
        ));
        spells.put(REGENERATION, new SpellDef(
                REGENERATION,
                "regeneration",
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_MID,
                24,
                SpiritRoots.WOOD,
                "human",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_regeneration.png")
        ));
        spells.put(BEAM, new SpellDef(
                BEAM,
                "beam",
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY,
                30,
                SpiritRoots.LIGHT,
                "human",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_beam.png")
        ));
        spells.put(EARTH_ESCAPE, new SpellDef(
                EARTH_ESCAPE,
                "earth_escape",
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_LATE,
                0,
                SpiritRoots.EARTH,
                "human",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_earth_escape.png")
        ));
        spells.put(CLEANSE, new SpellDef(
                CLEANSE,
                "cleanse",
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY,
                10,
                SpiritRoots.WATER,
                "human",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_cleanse.png")
        ));
        spells.put(QI_GATHERING, new SpellDef(
                QI_GATHERING,
                "qi_gathering",
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY,
                0,
                SpiritRoots.WIND,
                "human",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_qi_gathering.png")
        ));
        spells.put(LINGZHI_BULLET, new SpellDef(
                LINGZHI_BULLET,
                "lingzhi_bullet",
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY,
                5,
                SpiritRoots.WIND,
                "human",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_qi_gathering.png")
        ));
        spells.put(WIND_BLADE, new SpellDef(
                WIND_BLADE,
                "wind_blade",
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY,
                5,
                SpiritRoots.WIND,
                "human",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_qi_gathering.png")
        ));
        spells.put(WIND_STEP, new SpellDef(
                WIND_STEP,
                "wind_step",
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY,
                5,
                SpiritRoots.WIND,
                "human",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_wind_step.png")
        ));
        spells.put(YUFENG_JUE, new SpellDef(
                YUFENG_JUE,
                "yufeng_jue",
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_LATE,
                30,
                SpiritRoots.WIND,
                "human",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_yufeng_jue.png")
        ));
        spells.put(SMOKE_ART, new SpellDef(
                SMOKE_ART,
                "smoke_art",
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY,
                15,
                SpiritRoots.WIND,
                "human",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_qi_gathering.png")
        ));
        spells.put(HUTI_QI, new SpellDef(
                HUTI_QI,
                "huti_qi",
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY,
                10,
                SpiritRoots.WIND,
                "human",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_wind_step.png")
        ));
        spells.put(MICHEN_ZHANG, new SpellDef(
                MICHEN_ZHANG,
                "michen_zhang",
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_MID,
                20,
                SpiritRoots.WIND,
                "human",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_qi_gathering.png")
        ));
        spells.put(SLIDING_WATER, new SpellDef(
                SLIDING_WATER,
                "sliding_water",
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY,
                30,
                SpiritRoots.WATER,
                "human",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_cleanse.png")
        ));
        spells.put(IGNITE_FLARE, new SpellDef(
                IGNITE_FLARE,
                "ignite_flare",
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY,
                10,
                SpiritRoots.FIRE,
                "human",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_ignite_flare.png")
        ));
        spells.put(SPIRIT_SIGHT, new SpellDef(
                SPIRIT_SIGHT,
                "spirit_sight",
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY,
                5,
                SpiritRoots.LIGHT,
                "human",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_spirit_sight.png")
        ));
        spells.put(ZHENSHAN_PALM, new SpellDef(
                ZHENSHAN_PALM,
                "zhenshan_palm",
                CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY,
                390,
                SpiritRoots.EARTH,
                "earth",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_zhenshan_palm.png")
        ));
        spells.put(LIGHT_BEAM_ATTACK, new SpellDef(
                LIGHT_BEAM_ATTACK,
                "light_beam_attack",
                CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_LATE,
                90,
                SpiritRoots.LIGHT,
                "human",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_beam.png")
        ));
        spells.put(DIELANG_SHIELD, new SpellDef(
                DIELANG_SHIELD,
                "dielang_shield",
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_LATE,
                50,
                SpiritRoots.WATER,
                "earth",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_cleanse.png")
        ));
        spells.put(SIFANG_JIE, new SpellDef(
                SIFANG_JIE,
                "sifang_jie",
                CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_LATE,
                220,
                "",
                "earth",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_spirit_sight.png")
        ));
        spells.put(GUSHI_SHIELD, new SpellDef(
                GUSHI_SHIELD,
                "gushi_shield",
                CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_EARLY,
                120,
                SpiritRoots.EARTH,
                "earth",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_zhenshan_palm.png")
        ));
        spells.put(KONGSHI_SHU, new SpellDef(
                KONGSHI_SHU,
                "kongshi_shu",
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_LATE,
                30,
                SpiritRoots.DARK,
                "human",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_lingbeng.png")
        ));
        spells.put(HANJING_SUOZHUA, new SpellDef(
                HANJING_SUOZHUA,
                "hanjing_suozhua",
                "",
                0,
                SpiritRoots.ICE,
                "heaven",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_cleanse.png")
        ));
        spells.put(SHUANGTIAN_QI, new SpellDef(
                SHUANGTIAN_QI,
                "shuangtian_qi",
                "",
                0,
                SpiritRoots.ICE,
                "heaven",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_cleanse.png")
        ));
        spells.put(SUISHUANG_LINGXIAO, new SpellDef(
                SUISHUANG_LINGXIAO,
                "suishuang_lingxiao",
                "",
                0,
                SpiritRoots.ICE,
                "heaven",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_yufeng_jue.png")
        ));
        spells.put(YUQI_SHU, new SpellDef(
                YUQI_SHU,
                "yuqi_shu",
                CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_EARLY,
                0,
                SpiritRoots.METAL,
                "earth",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_spirit_sight.png")
        ));
        spells.put(XUYING_TA, new SpellDef(
                XUYING_TA,
                "xuying_ta",
                CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_MID,
                50,
                SpiritRoots.DARK,
                "earth",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/mob_effect/unknown.png")
        ));
        spells.put(DUANLIU_KONGDUN, new SpellDef(
                DUANLIU_KONGDUN,
                "duanliu_kongdun",
                CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_LATE,
                1200,
                SpiritRoots.WATER,
                "earth",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/mob_effect/unknown.png")
        ));
        spells.put(YIHEN_CI, new SpellDef(
                YIHEN_CI,
                "yihen_ci",
                CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_LATE,
                100,
                SpiritRoots.DARK,
                "earth",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/mob_effect/unknown.png")
        ));
        return Map.copyOf(spells);
    }

    public static List<SpellDef> all() {
        return SPELLS.values().stream().toList();
    }

    public static SpellDef get(String id) {
        return SPELLS.get(normalizeId(id));
    }

    public static Component spellProficiencyDescription(ModAttachments.CultivationData data, String spellId) {
        int uses = data == null ? 0 : data.spellProficiency(normalizeId(spellId));
        int threshold = nextSpellProficiencyThreshold(uses);
        return Component.translatable("screen." + ImmortalCultivationMod.MODID + ".spell_proficiency",
                spellProficiencyLevel(data, spellId), uses, threshold);
    }

    public static Component spellProficiencyLevel(ModAttachments.CultivationData data, String spellId) {
        int uses = data == null ? 0 : data.spellProficiency(normalizeId(spellId));
        return Component.translatable("screen." + ImmortalCultivationMod.MODID + "." + spellProficiencyTierKey(uses));
    }

    private static String spellProficiencyTierKey(int uses) {
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

    private static int nextSpellProficiencyThreshold(int uses) {
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

    public static int cooldownTicks(String spellId) {
        String normalized = normalizeId(spellId);
        if (REGENERATION.equals(normalized)) {
            return 20;
        }
        if (HUTI_QI.equals(normalized)
                || DIELANG_SHIELD.equals(normalized)
                || SIFANG_JIE.equals(normalized)
                || GUSHI_SHIELD.equals(normalized)) {
            return 10;
        }
        return 10;
    }

    public static String cooldownText(String spellId) {
        int ticks = cooldownTicks(spellId);
        if (ticks % 20 == 0) {
            return (ticks / 20) + "s";
        }
        return String.format(java.util.Locale.ROOT, "%.1fs", ticks / 20.0F);
    }

    public static String normalizeId(String id) {
        if (id == null) {
            return "";
        }
        if ("Lingbeng".equalsIgnoreCase(id) || "lingbeng".equalsIgnoreCase(id) || "\u7075\u5d29".equals(id)) {
            return LINGBENG;
        }
        if ("Fireball".equalsIgnoreCase(id) || "fireball".equalsIgnoreCase(id) || "\u706b\u7403\u672f".equals(id)) {
            return FIREBALL;
        }
        if ("Regeneration".equalsIgnoreCase(id) || "regen".equalsIgnoreCase(id) || "regeneration".equalsIgnoreCase(id) || "\u56de\u6625\u672f".equals(id)) {
            return REGENERATION;
        }
        if ("Beam".equalsIgnoreCase(id) || "beam".equalsIgnoreCase(id) || "\u5149\u675f\u672f".equals(id)) {
            return BEAM;
        }
        if ("Movement".equalsIgnoreCase(id) || "earth_escape".equalsIgnoreCase(id) || "earth escape".equalsIgnoreCase(id) || "\u9041\u5730\u672f".equals(id)) {
            return EARTH_ESCAPE;
        }
        if ("Cleanse".equalsIgnoreCase(id) || "cleanse".equalsIgnoreCase(id) || "\u6e05\u6d01\u5492".equals(id)) {
            return CLEANSE;
        }
        if ("QiGathering".equalsIgnoreCase(id) || "qi_gathering".equalsIgnoreCase(id) || "qi gathering".equalsIgnoreCase(id) || "\u5f15\u7075\u8bc0".equals(id)) {
            return QI_GATHERING;
        }
        if ("LingzhiBullet".equalsIgnoreCase(id) || "lingzhi_bullet".equalsIgnoreCase(id) || "lingzhi bullet".equalsIgnoreCase(id) || "\u7075\u6307\u5f39".equals(id)) {
            return LINGZHI_BULLET;
        }
        if ("WindBlade".equalsIgnoreCase(id) || "wind_blade".equalsIgnoreCase(id) || "wind blade".equalsIgnoreCase(id) || "\u98ce\u5203".equals(id)) {
            return WIND_BLADE;
        }
        if ("WindStep".equalsIgnoreCase(id) || "wind_step".equalsIgnoreCase(id) || "wind step".equalsIgnoreCase(id) || "\u8e0f\u98ce\u6b65".equals(id)) {
            return WIND_STEP;
        }
        if ("YufengJue".equalsIgnoreCase(id) || "yufeng_jue".equalsIgnoreCase(id) || "yufeng jue".equalsIgnoreCase(id) || "\u5fa1\u98ce\u8bc0".equals(id)) {
            return YUFENG_JUE;
        }
        if ("SmokeArt".equalsIgnoreCase(id) || "smoke_art".equalsIgnoreCase(id) || "smoke art".equalsIgnoreCase(id) || "\u8ff7\u70df\u672f".equals(id)) {
            return SMOKE_ART;
        }
        if ("HutiQi".equalsIgnoreCase(id) || "huti_qi".equalsIgnoreCase(id) || "huti qi".equalsIgnoreCase(id) || "\u62a4\u4f53\u6c14".equals(id)) {
            return HUTI_QI;
        }
        if ("MichenZhang".equalsIgnoreCase(id) || "michen_zhang".equalsIgnoreCase(id) || "michen zhang".equalsIgnoreCase(id) || "\u8ff7\u5c18\u969c".equals(id)) {
            return MICHEN_ZHANG;
        }
        if ("SlidingWater".equalsIgnoreCase(id) || "sliding_water".equalsIgnoreCase(id) || "sliding water".equalsIgnoreCase(id) || "\u6ed1\u6c34\u672f".equals(id)) {
            return SLIDING_WATER;
        }
        if ("Weiya".equalsIgnoreCase(id) || "weiya".equalsIgnoreCase(id) || "\u5a01\u538b".equals(id)) {
            return WEIYA;
        }
        if ("Zibao".equalsIgnoreCase(id) || "zibao".equalsIgnoreCase(id) || "self_explosion".equalsIgnoreCase(id) || "self explosion".equalsIgnoreCase(id) || "\u81ea\u7206".equals(id)) {
            return ZIBAO;
        }
        if ("AbsorbCultivation".equalsIgnoreCase(id) || "absorb_cultivation".equalsIgnoreCase(id) || "absorb cultivation".equalsIgnoreCase(id) || "\u5438\u53d6".equals(id)) {
            return ABSORB_CULTIVATION;
        }
        if ("Tuntian".equalsIgnoreCase(id) || "tuntian".equalsIgnoreCase(id) || "\u541e\u5929".equals(id)) {
            return TUNTIAN;
        }
        if ("Fengya".equalsIgnoreCase(id) || "fengya".equalsIgnoreCase(id) || "\u98ce\u538b".equals(id)) {
            return FENGYA;
        }
        if ("Dingshen".equalsIgnoreCase(id) || "dingshen".equalsIgnoreCase(id) || "ding shen".equalsIgnoreCase(id) || "\u5b9a\u8eab\u5492".equals(id)) {
            return DINGSHEN;
        }
        if ("YinleiJue".equalsIgnoreCase(id) || "yinlei_jue".equalsIgnoreCase(id) || "yinlei jue".equalsIgnoreCase(id) || "\u5f15\u96f7\u8bc0".equals(id)) {
            return YINLEI_JUE;
        }
        if ("WuleiZhengfa".equalsIgnoreCase(id) || "wulei_zhengfa".equalsIgnoreCase(id) || "wulei zhengfa".equalsIgnoreCase(id) || "\u4e94\u96f7\u6b63\u6cd5".equals(id)) {
            return WULEI_ZHENGFA;
        }
        if ("LiuguangJianying".equalsIgnoreCase(id) || "liuguang_jianying".equalsIgnoreCase(id) || "liuguang jianying".equalsIgnoreCase(id) || "\u6d41\u5149\u5251\u5f71".equals(id)) {
            return LIUGUANG_JIANYING;
        }
        if ("IgniteFlare".equalsIgnoreCase(id) || "ignite_flare".equalsIgnoreCase(id) || "ignite flare".equalsIgnoreCase(id) || "\u71c3\u706b\u8bc0".equals(id)) {
            return IGNITE_FLARE;
        }
        if ("SpiritSight".equalsIgnoreCase(id) || "spirit_sight".equalsIgnoreCase(id) || "spirit sight".equalsIgnoreCase(id) || "\u7075\u89c6".equals(id)) {
            return SPIRIT_SIGHT;
        }
        if ("ZhenshanPalm".equalsIgnoreCase(id) || "zhenshan_palm".equalsIgnoreCase(id) || "zhenshan palm".equalsIgnoreCase(id) || "\u9707\u5c71\u638c".equals(id)) {
            return ZHENSHAN_PALM;
        }
        if ("LightBeamAttack".equalsIgnoreCase(id) || "light_beam_attack".equalsIgnoreCase(id) || "light beam attack".equalsIgnoreCase(id)) {
            return LIGHT_BEAM_ATTACK;
        }
        if ("DielangShield".equalsIgnoreCase(id) || "dielang_shield".equalsIgnoreCase(id) || "dielang shield".equalsIgnoreCase(id) || "\u53e0\u6d6a\u76fe".equals(id)) {
            return DIELANG_SHIELD;
        }
        if ("SifangJie".equalsIgnoreCase(id) || "sifang_jie".equalsIgnoreCase(id) || "sifang jie".equalsIgnoreCase(id) || "\u56db\u65b9\u754c".equals(id)) {
            return SIFANG_JIE;
        }
        if ("GushiShield".equalsIgnoreCase(id) || "gushi_shield".equalsIgnoreCase(id) || "gushi shield".equalsIgnoreCase(id) || "\u56fa\u77f3\u76fe".equals(id)) {
            return GUSHI_SHIELD;
        }
        if ("KongshiShu".equalsIgnoreCase(id) || "kongshi_shu".equalsIgnoreCase(id) || "kongshi shu".equalsIgnoreCase(id) || "\u63a7\u5c38\u672f".equals(id)) {
            return KONGSHI_SHU;
        }
        if ("HanjingSuozhua".equalsIgnoreCase(id) || "hanjing_suozhua".equalsIgnoreCase(id) || "hanjing suozhua".equalsIgnoreCase(id) || "\u5bd2\u6676\u9501\u722a".equals(id)) {
            return HANJING_SUOZHUA;
        }
        if ("ShuangtianQi".equalsIgnoreCase(id) || "shuangtian_qi".equalsIgnoreCase(id) || "shuangtian qi".equalsIgnoreCase(id) || "\u971c\u5929\u6ce3".equals(id)) {
            return SHUANGTIAN_QI;
        }
        if ("SuishuangLingxiao".equalsIgnoreCase(id) || "suishuang_lingxiao".equalsIgnoreCase(id) || "suishuang lingxiao".equalsIgnoreCase(id) || "\u788e\u971c\u51cc\u9704".equals(id)) {
            return SUISHUANG_LINGXIAO;
        }
        if ("YuqiShu".equalsIgnoreCase(id) || "yuqi_shu".equalsIgnoreCase(id) || "yuqi shu".equalsIgnoreCase(id) || "\u5fa1\u5668\u672f".equals(id)) {
            return YUQI_SHU;
        }
        if ("XuyingTa".equalsIgnoreCase(id) || "xuying_ta".equalsIgnoreCase(id) || "xuying ta".equalsIgnoreCase(id) || "\u589f\u5f71\u8e0f".equals(id)) {
            return XUYING_TA;
        }
        if ("DuanliuKongdun".equalsIgnoreCase(id) || "duanliu_kongdun".equalsIgnoreCase(id) || "duanliu kongdun".equalsIgnoreCase(id) || "\u65ad\u6d41\u7a7a\u76fe".equals(id)) {
            return DUANLIU_KONGDUN;
        }
        if ("YihenCi".equalsIgnoreCase(id) || "yihen_ci".equalsIgnoreCase(id) || "yihen ci".equalsIgnoreCase(id) || "\u5fc6\u75d5\u523a".equals(id)) {
            return YIHEN_CI;
        }
        return id;
    }

    public static boolean meetsRequirement(String currentLevel, SpellDef spell) {
        if (WEIYA.equals(spell.id()) || ZIBAO.equals(spell.id())) {
            return isJindanOrHigher(currentLevel);
        }
        if (ABSORB_CULTIVATION.equals(spell.id())) {
            return !CultivationLevels.isMortal(currentLevel);
        }
        if (TUNTIAN.equals(spell.id())) {
            return !CultivationLevels.isMortal(currentLevel);
        }
        if (FENGYA.equals(spell.id())) {
            return !CultivationLevels.isMortal(currentLevel);
        }
        if (HANJING_SUOZHUA.equals(spell.id()) || SHUANGTIAN_QI.equals(spell.id()) || SUISHUANG_LINGXIAO.equals(spell.id())) {
            return !CultivationLevels.isMortal(currentLevel);
        }
        if (YUQI_SHU.equals(spell.id())) {
            return CultivationLevels.getStageIndex(currentLevel) >= CultivationLevels.getStageIndex(spell.requiredLevel());
        }
        return CultivationLevels.getStageIndex(currentLevel) >= CultivationLevels.getStageIndex(spell.requiredLevel());
    }

    public static boolean isInnateKnown(String spellId, String currentLevel) {
        String normalized = normalizeId(spellId);
        return ((WEIYA.equals(normalized) || ZIBAO.equals(normalized)) && isJindanOrHigher(currentLevel))
                || (YUQI_SHU.equals(normalized)
                && CultivationLevels.getStageIndex(currentLevel) >= CultivationLevels.getStageIndex(CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_EARLY));
    }

    public static boolean isInnateKnown(String spellId, ModAttachments.CultivationData data) {
        String normalized = normalizeId(spellId);
        if (data == null) {
            return false;
        }
        if (WEIYA.equals(normalized) || ZIBAO.equals(normalized)) {
            return isJindanOrHigher(data.cultivationLevel());
        }
        if (YUQI_SHU.equals(normalized)) {
            return meetsRequirement(data.cultivationLevel(), get(YUQI_SHU));
        }
        if (ABSORB_CULTIVATION.equals(normalized)) {
            return CultivationMethods.isReincarnationTrueArt(data.activeCultivationMethod());
        }
        if (TUNTIAN.equals(normalized)) {
            return CultivationMethods.isTuntianDemonArt(data.activeCultivationMethod());
        }
        if (FENGYA.equals(normalized)) {
            return CultivationMethods.isPokongJue(data.activeCultivationMethod());
        }
        return isHantiBingqinSpellUnlocked(normalized, data);
    }

    public static boolean isHantiBingqinSpellUnlocked(String spellId, ModAttachments.CultivationData data) {
        String normalized = normalizeId(spellId);
        if (data == null || !CultivationMethods.isHantiBingqin(data.activeCultivationMethod())) {
            return false;
        }
        int required = requiredHantiBingqinProficiency(normalized);
        return required >= 0 && data.methodProficiency(CultivationMethods.HANTI_BINGQIN) >= required;
    }

    public static int requiredHantiBingqinProficiency(String spellId) {
        String normalized = normalizeId(spellId);
        if (HANJING_SUOZHUA.equals(normalized)) {
            return 100;
        }
        if (SHUANGTIAN_QI.equals(normalized)) {
            return 500;
        }
        if (SUISHUANG_LINGXIAO.equals(normalized)) {
            return 1_000;
        }
        return -1;
    }

    public static boolean isComboEligible(String spellId) {
        String normalized = normalizeId(spellId);
        return !WEIYA.equals(normalized)
                && !ZIBAO.equals(normalized)
                && !ABSORB_CULTIVATION.equals(normalized)
                && !TUNTIAN.equals(normalized)
                && !FENGYA.equals(normalized)
                && !YUQI_SHU.equals(normalized)
                && !HANJING_SUOZHUA.equals(normalized)
                && !SHUANGTIAN_QI.equals(normalized)
                && !SUISHUANG_LINGXIAO.equals(normalized);
    }

    public static boolean isComboTimedToggle(String spellId) {
        String normalized = normalizeId(spellId);
        return EARTH_ESCAPE.equals(normalized)
                || QI_GATHERING.equals(normalized)
                || SPIRIT_SIGHT.equals(normalized)
                || WIND_STEP.equals(normalized)
                || YUFENG_JUE.equals(normalized)
                || XUYING_TA.equals(normalized);
    }

    public static boolean isTimedToggle(String spellId) {
        String normalized = normalizeId(spellId);
        return isComboTimedToggle(normalized)
                || WEIYA.equals(normalized)
                || FENGYA.equals(normalized)
                || SUISHUANG_LINGXIAO.equals(normalized)
                || XUYING_TA.equals(normalized);
    }

    public static List<String> innateSpellIds() {
        return List.of(WEIYA, ZIBAO, ABSORB_CULTIVATION, TUNTIAN, FENGYA, HANJING_SUOZHUA, SHUANGTIAN_QI, SUISHUANG_LINGXIAO, YUQI_SHU);
    }

    private static boolean isJindanOrHigher(String currentLevel) {
        return CultivationLevels.getStageIndex(currentLevel) >= CultivationLevels.getStageIndex(CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY);
    }

    public record SpellDef(String id, String translationKey, String requiredLevel, int qiCost, String element, String tier, ResourceLocation icon) {
        public Component name() {
            return Component.translatable("spell." + ImmortalCultivationMod.MODID + "." + translationKey);
        }

        public Component description() {
            return Component.translatable("spell." + ImmortalCultivationMod.MODID + "." + translationKey + ".description");
        }
    }
}
