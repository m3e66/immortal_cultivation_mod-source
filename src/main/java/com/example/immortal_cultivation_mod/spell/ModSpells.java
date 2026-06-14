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
    public static final String SLIDING_WATER = "sliding_water";
    public static final String WEIYA = "weiya";
    public static final String ABSORB_CULTIVATION = "absorb_cultivation";
    public static final String TUNTIAN = "tuntian";
    public static final String DINGSHEN = "dingshen";
    public static final String YINLEI_JUE = "yinlei_jue";
    public static final String WULEI_ZHENGFA = "wulei_zhengfa";

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
                "",
                0,
                SpiritRoots.DARK,
                "human",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_lingbeng.png")
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
                CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_MID,
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
        return Map.copyOf(spells);
    }

    public static List<SpellDef> all() {
        return SPELLS.values().stream().toList();
    }

    public static SpellDef get(String id) {
        return SPELLS.get(normalizeId(id));
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
        if ("SlidingWater".equalsIgnoreCase(id) || "sliding_water".equalsIgnoreCase(id) || "sliding water".equalsIgnoreCase(id) || "\u6ed1\u6c34\u672f".equals(id)) {
            return SLIDING_WATER;
        }
        if ("Weiya".equalsIgnoreCase(id) || "weiya".equalsIgnoreCase(id) || "\u5a01\u538b".equals(id)) {
            return WEIYA;
        }
        if ("AbsorbCultivation".equalsIgnoreCase(id) || "absorb_cultivation".equalsIgnoreCase(id) || "absorb cultivation".equalsIgnoreCase(id) || "\u5438\u53d6".equals(id)) {
            return ABSORB_CULTIVATION;
        }
        if ("Tuntian".equalsIgnoreCase(id) || "tuntian".equalsIgnoreCase(id) || "\u541e\u5929".equals(id)) {
            return TUNTIAN;
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
        return id;
    }

    public static boolean meetsRequirement(String currentLevel, SpellDef spell) {
        if (WEIYA.equals(spell.id())) {
            return !CultivationLevels.isMortal(currentLevel);
        }
        if (ABSORB_CULTIVATION.equals(spell.id())) {
            return !CultivationLevels.isMortal(currentLevel);
        }
        if (TUNTIAN.equals(spell.id())) {
            return !CultivationLevels.isMortal(currentLevel);
        }
        return CultivationLevels.getStageIndex(currentLevel) >= CultivationLevels.getStageIndex(spell.requiredLevel());
    }

    public static boolean isInnateKnown(String spellId, String currentLevel) {
        return WEIYA.equals(normalizeId(spellId)) && !CultivationLevels.isMortal(currentLevel);
    }

    public static boolean isInnateKnown(String spellId, ModAttachments.CultivationData data) {
        String normalized = normalizeId(spellId);
        if (WEIYA.equals(normalized)) {
            return data != null && !CultivationLevels.isMortal(data.cultivationLevel());
        }
        if (data == null) {
            return false;
        }
        if (ABSORB_CULTIVATION.equals(normalized)) {
            return CultivationMethods.isReincarnationTrueArt(data.activeCultivationMethod());
        }
        return TUNTIAN.equals(normalized) && CultivationMethods.isTuntianDemonArt(data.activeCultivationMethod());
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
