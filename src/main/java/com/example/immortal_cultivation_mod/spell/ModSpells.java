package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.BodyTypes;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public static final String XUANYAN_HUTI = "xuanyan_huti";
    public static final String LIUFENG_XIEJIN = "liufeng_xiejin";
    public static final String XUESHA_HUSHEN = "xuesha_hushen";
    public static final String JINGLING_FANZHAO = "jingling_fanzhao";
    public static final String YUANGANG_HUJIE = "yuangang_hujie";
    public static final String JINMANG_CHUANSHA = "jinmang_chuansha";
    public static final String JINFENG_ZHI = "jinfeng_zhi";
    public static final String WANREN_CHUANXIN = "wanren_chuanxin";
    public static final String JINGJI_TUSHENG = "jingji_tusheng";
    public static final String WANTENG_JIAOSHA = "wanteng_jiaosha";
    public static final String YANBAO_DAN = "yanbao_dan";
    public static final String CHIYAN_LONGJUAN = "chiyan_longjuan";
    public static final String TIANYUN_FENCHENG = "tianyun_fencheng";
    public static final String LUOHUA_CHANSHEN = "luohua_chanshen";
    public static final String BAIHUA_ZANG = "baihua_zang";
    public static final String POKONG_JIANHEN = "pokong_jianhen";
    public static final String SANJIAN_ZHUIMING = "sanjian_zhuiming";
    public static final String DIYAN_QIANG = "diyan_qiang";
    public static final String SHANBENG_YIN = "shanbeng_yin";
    public static final String SHUIREN_LIANZHAN = "shuiren_lianzhan";
    public static final String CANGLANG_CHONG = "canglang_chong";
    public static final String LEISHAN = "leishan";
    public static final String WULEI_HONGDING = "wulei_hongding";
    public static final String BINGZHUI_YU = "bingzhui_yu";
    public static final String HANYU_FENGDI = "hanyu_fengdi";
    public static final String YAOGUANG_SHAN = "yaoguang_shan";
    public static final String TIANGUANG_GUANSHA = "tianguang_guansha";
    public static final String YINGXI = "yingxi";
    public static final String TIANFENG_ZHUI = "tianfeng_zhui";
    public static final String MULING_SHENGXI = "muling_shengxi";
    public static final String XUE_GUIYUAN_SHU = "xue_guiyuan_shu";
    public static final String SHENGXI_BAOFA = "shengxi_baofa";
    public static final String KONGSHI_SHU = "kongshi_shu";
    public static final String HANJING_SUOZHUA = "hanjing_suozhua";
    public static final String SHUANGTIAN_QI = "shuangtian_qi";
    public static final String SUISHUANG_LINGXIAO = "suishuang_lingxiao";
    public static final String YUQI_SHU = "yuqi_shu";
    public static final String XUYING_TA = "xuying_ta";
    public static final String DUANLIU_KONGDUN = "duanliu_kongdun";
    public static final String YIHEN_CI = "yihen_ci";
    public static final String CUIGU_LIANSHEN = "cuigu_lianshen";
    public static final String JINHUO_XINLIAO = "jinhuo_xinliao";
    public static final String SUIGUI_XINGYUN = "suigui_xingyun";
    public static final String DONGTIAN_JIELING = "dongtian_jieling";
    public static final String SHIXU_CHILIU = "shixu_chiliu";
    public static final String LIANSHEN_SHU = "lianshen_shu";
    public static final String SUOKONG_SHU = "suokong_shu";
    public static final String YANGYAN_FUTI = "yangyan_futi";
    public static final String DARI_ZHEN = "dari_zhen";
    public static final String LIEYANG_BU = "lieyang_bu";
    public static final String RILUN = "rilun";
    public static final String JIYANG_YIKE = "jiyang_yike";
    public static final String LEIXIAO_GUANYUN = "leixiao_guanyun";
    public static final String JIUXIAO_FENGLEI_YU = "jiuxiao_fenglei_yu";
    public static final String XUANYUE_BI = "xuanyue_bi";
    public static final String JINYAN_SHENGFENG = "jinyan_shengfeng";
    public static final String SHANSUO_ZHENYU = "shansuo_zhenyu";
    public static final String WANFENG_YUEZANG = "wanfeng_yuezang";
    public static final String DONGLIU_SUO = "dongliu_suo";
    public static final String CANGYUAN_HONGLIU = "cangyuan_hongliu";
    public static final String HANYUAN_FENGJIE = "hanyuan_fengjie";
    public static final String WANMU_SHENG = "wanmu_sheng";
    public static final String QINGTENG_FU = "qingteng_fu";
    public static final String YINFU_SHU = "yinfu_shu";
    public static final String BIHUO_JUE = "bihuo_jue";

    private static final Set<String> NEW_SPELLS_WITHOUT_REALM_REQUIREMENT = Set.of(
            YANGYAN_FUTI,
            DARI_ZHEN,
            LIEYANG_BU,
            RILUN,
            JIYANG_YIKE);

    private static final Set<String> PREPARED_CAST_SPELLS = Set.of(
            FIREBALL,
            YINLEI_JUE,
            WULEI_ZHENGFA,
            LIUGUANG_JIANYING,
            SLIDING_WATER,
            LINGZHI_BULLET,
            WIND_BLADE,
            SMOKE_ART,
            MICHEN_ZHANG,
            IGNITE_FLARE,
            ZHENSHAN_PALM,
            ABSORB_CULTIVATION,
            TUNTIAN,
            DINGSHEN,
            HANJING_SUOZHUA,
            SHUANGTIAN_QI,
            YUQI_SHU,
            KONGSHI_SHU,
            DUANLIU_KONGDUN,
            YIHEN_CI,
            JINHUO_XINLIAO,
            SUIGUI_XINGYUN,
            SUOKONG_SHU,
            DARI_ZHEN,
            LIEYANG_BU,
            RILUN,
            JIYANG_YIKE,
            QINGTENG_FU);

    /** Offensive and hostile-control spells that are selected first and released with R. */
    private static final Set<String> ATTACK_SPELLS = Set.of(
            FIREBALL,
            LINGBENG,
            IGNITE_FLARE,
            ZHENSHAN_PALM,
            LIGHT_BEAM_ATTACK,
            LINGZHI_BULLET,
            JINMANG_CHUANSHA,
            WIND_BLADE,
            SMOKE_ART,
            MICHEN_ZHANG,
            SLIDING_WATER,
            ZIBAO,
            ABSORB_CULTIVATION,
            TUNTIAN,
            DINGSHEN,
            YINLEI_JUE,
            WULEI_ZHENGFA,
            LIUGUANG_JIANYING,
            JINFENG_ZHI,
            WANREN_CHUANXIN,
            JINGJI_TUSHENG,
            WANTENG_JIAOSHA,
            YANBAO_DAN,
            CHIYAN_LONGJUAN,
            TIANYUN_FENCHENG,
            LUOHUA_CHANSHEN,
            BAIHUA_ZANG,
            POKONG_JIANHEN,
            SANJIAN_ZHUIMING,
            DIYAN_QIANG,
            SHANBENG_YIN,
            SHUIREN_LIANZHAN,
            CANGLANG_CHONG,
            LEISHAN,
            WULEI_HONGDING,
            BINGZHUI_YU,
            HANYU_FENGDI,
            YAOGUANG_SHAN,
            TIANGUANG_GUANSHA,
            YINGXI,
            TIANFENG_ZHUI,
            DARI_ZHEN,
            LEIXIAO_GUANYUN,
            JIUXIAO_FENGLEI_YU,
            JINYAN_SHENGFENG,
            SHANSUO_ZHENYU,
            WANFENG_YUEZANG,
            DONGLIU_SUO,
            CANGYUAN_HONGLIU,
            HANYUAN_FENGJIE,
            QINGTENG_FU,
            HANJING_SUOZHUA,
            SHUANGTIAN_QI,
            YIHEN_CI,
            JINHUO_XINLIAO,
            SUIGUI_XINGYUN,
            SUOKONG_SHU);

    private static final Map<String, SpellDef> SPELLS = createSpells();
    private static final Map<String, Integer> NPC_LEARNING_TRUST = Map.ofEntries(
            Map.entry(QI_GATHERING, 5),
            Map.entry(CLEANSE, 6),
            Map.entry(BEAM, 7),
            Map.entry(FIREBALL, 8),
            Map.entry(LINGZHI_BULLET, 9),
            Map.entry(WIND_BLADE, 10),
            Map.entry(WIND_STEP, 11),
            Map.entry(SMOKE_ART, 12),
            Map.entry(HUTI_QI, 13),
            Map.entry(REGENERATION, 14),
            Map.entry(MICHEN_ZHANG, 16),
            Map.entry(SLIDING_WATER, 17),
            Map.entry(IGNITE_FLARE, 18),
            Map.entry(SPIRIT_SIGHT, 19),
            Map.entry(YINLEI_JUE, 23),
            Map.entry(LINGBENG, 24),
            Map.entry(YUFENG_JUE, 25),
            Map.entry(EARTH_ESCAPE, 27),
            Map.entry(DIELANG_SHIELD, 28),
            Map.entry(KONGSHI_SHU, 29),
            Map.entry(YUQI_SHU, 34),
            Map.entry(GUSHI_SHIELD, 35),
            Map.entry(XUANYAN_HUTI, 36),
            Map.entry(LIUFENG_XIEJIN, 38),
            Map.entry(XUESHA_HUSHEN, 55),
            Map.entry(JINGLING_FANZHAO, 68),
            Map.entry(YUANGANG_HUJIE, 82),
            Map.entry(JINMANG_CHUANSHA, 54),
            Map.entry(JINFENG_ZHI, 10),
            Map.entry(WANREN_CHUANXIN, 48),
            Map.entry(JINGJI_TUSHENG, 18),
            Map.entry(WANTENG_JIAOSHA, 44),
            Map.entry(YANBAO_DAN, 24),
            Map.entry(CHIYAN_LONGJUAN, 50),
            Map.entry(TIANYUN_FENCHENG, 72),
            Map.entry(LUOHUA_CHANSHEN, 30),
            Map.entry(BAIHUA_ZANG, 58),
            Map.entry(POKONG_JIANHEN, 20),
            Map.entry(SANJIAN_ZHUIMING, 42),
            Map.entry(DIYAN_QIANG, 22),
            Map.entry(SHANBENG_YIN, 62),
            Map.entry(SHUIREN_LIANZHAN, 11),
            Map.entry(CANGLANG_CHONG, 36),
            Map.entry(LEISHAN, 25),
            Map.entry(WULEI_HONGDING, 64),
            Map.entry(BINGZHUI_YU, 30),
            Map.entry(HANYU_FENGDI, 52),
            Map.entry(YAOGUANG_SHAN, 20),
            Map.entry(TIANGUANG_GUANSHA, 66),
            Map.entry(YINGXI, 28),
            Map.entry(TIANFENG_ZHUI, 50),
            Map.entry(MULING_SHENGXI, 34),
            Map.entry(XUE_GUIYUAN_SHU, 40),
            Map.entry(SHENGXI_BAOFA, 42),
            Map.entry(WULEI_ZHENGFA, 42),
            Map.entry(XUYING_TA, 43),
            Map.entry(DINGSHEN, 49),
            Map.entry(LIGHT_BEAM_ATTACK, 50),
            Map.entry(SIFANG_JIE, 51),
            Map.entry(DUANLIU_KONGDUN, 52),
            Map.entry(YIHEN_CI, 53),
            Map.entry(WEIYA, 58),
            Map.entry(LIUGUANG_JIANYING, 59),
            Map.entry(ZHENSHAN_PALM, 60),
            Map.entry(DONGTIAN_JIELING, 61),
            Map.entry(ZIBAO, 64),
            Map.entry(JINHUO_XINLIAO, 66),
            Map.entry(SUIGUI_XINGYUN, 67),
            Map.entry(SHIXU_CHILIU, 72),
            Map.entry(FENGYA, 88),
            Map.entry(CUIGU_LIANSHEN, 89),
            Map.entry(HANJING_SUOZHUA, 90),
            Map.entry(SHUANGTIAN_QI, 92),
            Map.entry(SUISHUANG_LINGXIAO, 93),
            Map.entry(ABSORB_CULTIVATION, 94),
            Map.entry(TUNTIAN, 96)
    );

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
        spells.put(XUANYAN_HUTI, new SpellDef(
                XUANYAN_HUTI,
                "xuanyan_huti",
                CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_EARLY,
                120,
                SpiritRoots.EARTH,
                "earth",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_zhenshan_palm.png")
        ));
        spells.put(LIUFENG_XIEJIN, new SpellDef(
                LIUFENG_XIEJIN,
                "liufeng_xiejin",
                CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_MID,
                180,
                SpiritRoots.WIND,
                "earth",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_wind_blade.png")
        ));
        spells.put(XUESHA_HUSHEN, new SpellDef(
                XUESHA_HUSHEN,
                "xuesha_hushen",
                CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY,
                550,
                "blood",
                "earth",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_lingbeng.png")
        ));
        spells.put(JINGLING_FANZHAO, new SpellDef(
                JINGLING_FANZHAO,
                "jingling_fanzhao",
                CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_LATE,
                1200,
                SpiritRoots.LIGHT,
                "earth",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_beam.png")
        ));
        spells.put(YUANGANG_HUJIE, new SpellDef(
                YUANGANG_HUJIE,
                "yuangang_hujie",
                CultivationLevels.REALM_YUANYING + CultivationLevels.STAGE_EARLY,
                3000,
                "",
                "heaven",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_spirit_sight.png")
        ));
        spells.put(JINMANG_CHUANSHA, new SpellDef(
                JINMANG_CHUANSHA,
                "jinmang_chuansha",
                CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY,
                600,
                SpiritRoots.METAL,
                "earth",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/spell_lingzhi_bullet.png")
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
        spells.put(CUIGU_LIANSHEN, new SpellDef(
                CUIGU_LIANSHEN,
                "cuigu_lianshen",
                "",
                0,
                "",
                "heaven",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/mob_effect/unknown.png")
        ));
        spells.put(JINHUO_XINLIAO, new SpellDef(
                JINHUO_XINLIAO,
                "jinhuo_xinliao",
                CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_MID,
                2000,
                SpiritRoots.FIRE,
                "earth",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/mob_effect/unknown.png")
        ));
        spells.put(SUIGUI_XINGYUN, new SpellDef(
                SUIGUI_XINGYUN,
                "suigui_xingyun",
                CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_MID,
                1500,
                SpiritRoots.LIGHT,
                "earth",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/mob_effect/unknown.png")
        ));
        spells.put(DONGTIAN_JIELING, new SpellDef(
                DONGTIAN_JIELING,
                "dongtian_jieling",
                CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY,
                0,
                "",
                "earth",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/mob_effect/unknown.png")
        ));
        spells.put(SHIXU_CHILIU, new SpellDef(
                SHIXU_CHILIU,
                "shixu_chiliu",
                CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_LATE,
                500,
                "",
                "earth",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/mob_effect/unknown.png")
        ));
        spells.put(LIANSHEN_SHU, new SpellDef(
                LIANSHEN_SHU,
                "lianshen_shu",
                CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY,
                0,
                "",
                "earth",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/mob_effect/unknown.png")
        ));
        spells.put(SUOKONG_SHU, new SpellDef(
                SUOKONG_SHU,
                "suokong_shu",
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY,
                100,
                "",
                "heaven",
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/mob_effect/unknown.png")
        ));
        putTechnique(spells, JINFENG_ZHI,
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_MID,
                30, SpiritRoots.METAL, "human");
        putTechnique(spells, WANREN_CHUANXIN,
                CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_MID,
                1100, SpiritRoots.METAL, "earth");
        putTechnique(spells, JINGJI_TUSHENG,
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_LATE,
                80, SpiritRoots.WOOD, "human");
        putTechnique(spells, WANTENG_JIAOSHA,
                CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY,
                650, SpiritRoots.WOOD, "earth");
        putTechnique(spells, YANBAO_DAN,
                CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_EARLY,
                120, SpiritRoots.FIRE, "earth");
        putTechnique(spells, CHIYAN_LONGJUAN,
                CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_MID,
                1250, SpiritRoots.FIRE, "earth");
        putTechnique(spells, TIANYUN_FENCHENG,
                CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_LATE,
                2800, SpiritRoots.FIRE + "," + SpiritRoots.EARTH, "earth");
        putTechnique(spells, LUOHUA_CHANSHEN,
                CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_EARLY,
                140, SpiritRoots.WOOD + "," + SpiritRoots.WIND, "earth");
        putTechnique(spells, BAIHUA_ZANG,
                CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_MID,
                1200, SpiritRoots.WOOD + "," + SpiritRoots.WIND, "earth");
        putTechnique(spells, POKONG_JIANHEN,
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_LATE,
                85, SpiritRoots.METAL + ",sword", "human");
        putTechnique(spells, SANJIAN_ZHUIMING,
                CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_LATE,
                380, SpiritRoots.METAL + ",sword", "earth");
        putTechnique(spells, DIYAN_QIANG,
                CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_EARLY,
                110, SpiritRoots.EARTH, "earth");
        putTechnique(spells, SHANBENG_YIN,
                CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_LATE,
                1850, SpiritRoots.EARTH, "earth");
        putTechnique(spells, SHUIREN_LIANZHAN,
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_MID,
                35, SpiritRoots.WATER, "human");
        putTechnique(spells, CANGLANG_CHONG,
                CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_LATE,
                360, SpiritRoots.WATER, "earth");
        putTechnique(spells, LEISHAN,
                CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_EARLY,
                140, SpiritRoots.THUNDER, "earth");
        putTechnique(spells, WULEI_HONGDING,
                CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_LATE,
                1700, SpiritRoots.THUNDER, "earth");
        putTechnique(spells, BINGZHUI_YU,
                CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_MID,
                230, SpiritRoots.ICE, "earth");
        putTechnique(spells, HANYU_FENGDI,
                CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_MID,
                1300, SpiritRoots.ICE, "earth");
        putTechnique(spells, YAOGUANG_SHAN,
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_LATE,
                75, SpiritRoots.LIGHT, "human");
        putTechnique(spells, TIANGUANG_GUANSHA,
                CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_LATE,
                1600, SpiritRoots.LIGHT, "earth");
        putTechnique(spells, YINGXI,
                CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_EARLY,
                130, SpiritRoots.DARK, "earth");
        putTechnique(spells, TIANFENG_ZHUI,
                CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_MID,
                1200, SpiritRoots.WIND, "earth");
        putTechnique(spells, MULING_SHENGXI,
                CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_MID,
                240, SpiritRoots.WOOD, "earth");
        putTechnique(spells, XUE_GUIYUAN_SHU,
                CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_LATE,
                320, "blood," + SpiritRoots.WOOD, "earth");
        putTechnique(spells, SHENGXI_BAOFA,
                CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_LATE,
                480, SpiritRoots.WOOD, "earth");
        putTechnique(spells, YANGYAN_FUTI,
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY,
                150, SpiritRoots.FIRE + "," + SpiritRoots.LIGHT, "heaven");
        putTechnique(spells, DARI_ZHEN,
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY,
                450, SpiritRoots.FIRE + "," + SpiritRoots.LIGHT, "heaven");
        putTechnique(spells, LIEYANG_BU,
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY,
                250, SpiritRoots.FIRE + "," + SpiritRoots.LIGHT, "heaven");
        putTechnique(spells, RILUN,
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY,
                800, SpiritRoots.FIRE + "," + SpiritRoots.LIGHT, "heaven");
        putTechnique(spells, JIYANG_YIKE,
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY,
                0, SpiritRoots.FIRE + "," + SpiritRoots.LIGHT, "heaven");
        putTechnique(spells, LEIXIAO_GUANYUN,
                CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY,
                700, SpiritRoots.WIND + "," + SpiritRoots.THUNDER, "heaven");
        putTechnique(spells, JIUXIAO_FENGLEI_YU,
                CultivationLevels.REALM_YUANYING + CultivationLevels.STAGE_EARLY,
                2800, SpiritRoots.WIND + "," + SpiritRoots.THUNDER, "heaven");
        putTechnique(spells, XUANYUE_BI,
                CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_EARLY,
                200, SpiritRoots.METAL + "," + SpiritRoots.EARTH, "heaven");
        putTechnique(spells, JINYAN_SHENGFENG,
                CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_LATE,
                300, SpiritRoots.METAL + "," + SpiritRoots.EARTH, "heaven");
        putTechnique(spells, SHANSUO_ZHENYU,
                CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY,
                650, SpiritRoots.EARTH, "heaven");
        putTechnique(spells, WANFENG_YUEZANG,
                CultivationLevels.REALM_YUANYING + CultivationLevels.STAGE_EARLY,
                2400, SpiritRoots.METAL + "," + SpiritRoots.EARTH, "heaven");
        putTechnique(spells, DONGLIU_SUO,
                CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_LATE,
                300, SpiritRoots.WATER + "," + SpiritRoots.ICE, "heaven");
        putTechnique(spells, CANGYUAN_HONGLIU,
                CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY,
                700, SpiritRoots.WATER + "," + SpiritRoots.ICE, "heaven");
        putTechnique(spells, HANYUAN_FENGJIE,
                CultivationLevels.REALM_YUANYING + CultivationLevels.STAGE_EARLY,
                2500, SpiritRoots.WATER + "," + SpiritRoots.ICE, "heaven");
        putTechnique(spells, WANMU_SHENG,
                "", 650, SpiritRoots.WOOD, "earth");
        putTechnique(spells, QINGTENG_FU,
                "", 300, SpiritRoots.WOOD, "earth");
        for (String abilityId : UndeadAbilitySystem.playerAbilityIds()) {
            putTechnique(spells, abilityId, "", 0, SpiritRoots.DARK, "innate");
        }
        putTechnique(spells, "jietian_duoyun_shu",
                CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_LATE, 1800, "none", "heaven");
        putTechnique(spells, "bijie_shu",
                CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_LATE, 500, "none", "earth");
        putTechnique(spells, "guanyun_shu", "", 100, "none", "earth");
        putTechnique(spells, YINFU_SHU,
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_MID, 35, "none", "earth");
        putTechnique(spells, BIHUO_JUE,
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_LATE, 60, "none", "earth");
        removeRealmRequirements(spells);
        return Map.copyOf(spells);
    }

    private static void removeRealmRequirements(Map<String, SpellDef> spells) {
        for (String spellId : NEW_SPELLS_WITHOUT_REALM_REQUIREMENT) {
            SpellDef spell = spells.get(spellId);
            if (spell == null) {
                continue;
            }
            spells.put(spellId, new SpellDef(
                    spell.id(),
                    spell.translationKey(),
                    "",
                    spell.qiCost(),
                    spell.element(),
                    spell.tier(),
                    spell.icon()));
        }
    }

    private static void putTechnique(
            Map<String, SpellDef> spells,
            String id,
            String requiredLevel,
            int qiCost,
            String element,
            String tier) {
        spells.put(id, new SpellDef(
                id,
                id,
                requiredLevel,
                qiCost,
                element,
                tier,
                ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "textures/mob_effect/unknown.png")));
    }

    public static List<SpellDef> all() {
        return List.copyOf(SPELLS.values());
    }

    public static SpellDef get(String id) {
        return SPELLS.get(normalizeId(id));
    }

    public static int npcLearningTrustRequired(String id) {
        return NPC_LEARNING_TRUST.getOrDefault(normalizeId(id), 100);
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
        if ("jietian_duoyun_shu".equals(normalized)) return 6000;
        if ("bijie_shu".equals(normalized)) return 4800;
        if ("guanyun_shu".equals(normalized)) return 100;
        if (YINFU_SHU.equals(normalized)) return 600;
        if (BIHUO_JUE.equals(normalized)) return 900;
        int undeadAbilityCooldown = UndeadAbilitySystem.playerAbilityCooldownTicks(normalized);
        if (undeadAbilityCooldown > 0) {
            return undeadAbilityCooldown;
        }
        int elementalCooldown = switch (normalized) {
            case JINFENG_ZHI -> 3;
            case WANREN_CHUANXIN -> 12;
            case JINGJI_TUSHENG -> 6;
            case WANTENG_JIAOSHA -> 13;
            case YANBAO_DAN, DIYAN_QIANG -> 5;
            case CHIYAN_LONGJUAN -> 15;
            case TIANYUN_FENCHENG -> 30;
            case LUOHUA_CHANSHEN -> 8;
            case BAIHUA_ZANG -> 15;
            case POKONG_JIANHEN -> 5;
            case SANJIAN_ZHUIMING -> 10;
            case SHANBENG_YIN -> 18;
            case SHUIREN_LIANZHAN -> 4;
            case CANGLANG_CHONG -> 9;
            case LEISHAN, YINGXI -> 7;
            case WULEI_HONGDING -> 17;
            case BINGZHUI_YU, YAOGUANG_SHAN -> 8;
            case HANYU_FENGDI -> 16;
            case TIANGUANG_GUANSHA -> 14;
            case TIANFENG_ZHUI -> 13;
            case MULING_SHENGXI -> 12;
            case XUE_GUIYUAN_SHU -> 14;
            case SHENGXI_BAOFA -> 18;
            case YANGYAN_FUTI -> 8;
            case DARI_ZHEN -> 12;
            case LIEYANG_BU -> 7;
            case RILUN -> 15;
            case JIYANG_YIKE -> 30;
            case LEIXIAO_GUANYUN -> 13;
            case JIUXIAO_FENGLEI_YU -> 25;
            case XUANYUE_BI -> 7;
            case JINYAN_SHENGFENG -> 8;
            case SHANSUO_ZHENYU -> 14;
            case WANFENG_YUEZANG -> 24;
            case DONGLIU_SUO -> 9;
            case CANGYUAN_HONGLIU -> 14;
            case HANYUAN_FENGJIE -> 24;
            case WANMU_SHENG -> 18;
            case QINGTENG_FU -> 9;
            default -> 0;
        };
        if (elementalCooldown > 0) {
            return elementalCooldown * 20;
        }
        if (JINHUO_XINLIAO.equals(normalized)) {
            return 20 * 60;
        }
        if (SUIGUI_XINGYUN.equals(normalized)) {
            return 20 * 20;
        }
        if (DONGTIAN_JIELING.equals(normalized)) {
            return 20 * 60 * 10;
        }
        if (LIANSHEN_SHU.equals(normalized)) {
            return 20 * 60 * 60;
        }
        if (SUOKONG_SHU.equals(normalized)) {
            return 20 * 30;
        }
        if (REGENERATION.equals(normalized)) {
            return 20;
        }
        if (XUANYAN_HUTI.equals(normalized)) {
            return 20 * 8;
        }
        if (LIUFENG_XIEJIN.equals(normalized)) {
            return 20 * 10;
        }
        if (XUESHA_HUSHEN.equals(normalized)) {
            return 20 * 14;
        }
        if (JINGLING_FANZHAO.equals(normalized)) {
            return 20 * 16;
        }
        if (YUANGANG_HUJIE.equals(normalized)) {
            return 20 * 20;
        }
        if (JINMANG_CHUANSHA.equals(normalized)) {
            return 20 * 8;
        }
        if (HUTI_QI.equals(normalized)
                || DIELANG_SHIELD.equals(normalized)
                || SIFANG_JIE.equals(normalized)
                || GUSHI_SHIELD.equals(normalized)) {
            return 10;
        }
        return 10;
    }

    /** Spells whose existing client flow prepares and charges before release. */
    public static boolean requiresPreparation(String spellId) {
        String normalized = normalizeId(spellId);
        return UndeadAbilitySystem.requiresPlayerPreparation(normalized)
                || PREPARED_CAST_SPELLS.contains(normalized)
                || ATTACK_SPELLS.contains(normalized);
    }

    public static boolean isAttackSpell(String spellId) {
        String normalized = normalizeId(spellId);
        return ATTACK_SPELLS.contains(normalized);
    }

    public static String cooldownText(String spellId) {
        String normalized = normalizeId(spellId);
        if (CUIGU_LIANSHEN.equals(normalized)) {
            return "ceil(5% Max HP / 10) min";
        }
        if (DONGTIAN_JIELING.equals(normalized)) {
            return "10 min";
        }
        if (LIANSHEN_SHU.equals(normalized)) {
            return "1 hour";
        }
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
        if ("YinfuShu".equalsIgnoreCase(id)
                || "yinfu_shu".equalsIgnoreCase(id)
                || "yinfu shu".equalsIgnoreCase(id)
                || "引福术".equals(id)) {
            return YINFU_SHU;
        }
        if ("BihuoJue".equalsIgnoreCase(id)
                || "bihuo_jue".equalsIgnoreCase(id)
                || "bihuo jue".equalsIgnoreCase(id)
                || "避祸诀".equals(id)) {
            return BIHUO_JUE;
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
        if ("XuanyanHuti".equalsIgnoreCase(id) || "xuanyan_huti".equalsIgnoreCase(id) || "xuanyan huti".equalsIgnoreCase(id) || "\u7384\u5ca9\u62a4\u4f53".equals(id)) {
            return XUANYAN_HUTI;
        }
        if ("LiufengXiejin".equalsIgnoreCase(id) || "liufeng_xiejin".equalsIgnoreCase(id) || "liufeng xiejin".equalsIgnoreCase(id) || "\u6d41\u98ce\u5378\u52b2".equals(id)) {
            return LIUFENG_XIEJIN;
        }
        if ("XueshaHushen".equalsIgnoreCase(id) || "xuesha_hushen".equalsIgnoreCase(id) || "xuesha hushen".equalsIgnoreCase(id) || "\u8840\u715e\u62a4\u8eab".equals(id)) {
            return XUESHA_HUSHEN;
        }
        if ("JinglingFanzhao".equalsIgnoreCase(id) || "jingling_fanzhao".equalsIgnoreCase(id) || "jingling fanzhao".equalsIgnoreCase(id) || "\u955c\u7075\u8fd4\u7167".equals(id)) {
            return JINGLING_FANZHAO;
        }
        if ("YuangangHujie".equalsIgnoreCase(id) || "yuangang_hujie".equalsIgnoreCase(id) || "yuangang hujie".equalsIgnoreCase(id) || "\u5143\u7f61\u62a4\u754c".equals(id)) {
            return YUANGANG_HUJIE;
        }
        if ("JinmangChuansha".equalsIgnoreCase(id) || "jinmang_chuansha".equalsIgnoreCase(id) || "jinmang chuansha".equalsIgnoreCase(id) || "\u91d1\u8292\u7a7f\u6740".equals(id)) {
            return JINMANG_CHUANSHA;
        }
        String compact = id.trim().toLowerCase(java.util.Locale.ROOT).replace(' ', '_');
        String elemental = switch (compact) {
            case "jinfeng_zhi", "\u91d1\u950b\u6307" -> JINFENG_ZHI;
            case "wanren_chuanxin", "\u4e07\u5203\u7a7f\u5fc3" -> WANREN_CHUANXIN;
            case "jingji_tusheng", "\u8346\u68d8\u7a81\u751f" -> JINGJI_TUSHENG;
            case "wanteng_jiaosha", "\u4e07\u85e4\u7ede\u6740" -> WANTENG_JIAOSHA;
            case "yanbao_dan", "\u708e\u7206\u5f39" -> YANBAO_DAN;
            case "chiyan_longjuan", "\u8d64\u708e\u9f99\u5377" -> CHIYAN_LONGJUAN;
            case "tianyun_fencheng", "\u5929\u9668\u711a\u57ce" -> TIANYUN_FENCHENG;
            case "yangyan_futi", "\u9633\u708e\u9644\u4f53" -> YANGYAN_FUTI;
            case "dari_zhen", "riyun", "\u5927\u65e5\u9707", "\u65e5\u9668" -> DARI_ZHEN;
            case "lieyang_bu", "\u70c8\u9633\u6b65" -> LIEYANG_BU;
            case "rilun", "\u65e5\u8f6e" -> RILUN;
            case "jiyang_yike", "\u6781\u9633\u4e00\u523b" -> JIYANG_YIKE;
            case "leixiao_guanyun", "\u96f7\u5578\u8d2f\u4e91" -> LEIXIAO_GUANYUN;
            case "jiuxiao_fenglei_yu", "\u4e5d\u9704\u98ce\u96f7\u57df" -> JIUXIAO_FENGLEI_YU;
            case "xuanyue_bi", "\u7384\u5cb3\u58c1" -> XUANYUE_BI;
            case "jinyan_shengfeng", "\u91d1\u5ca9\u751f\u950b" -> JINYAN_SHENGFENG;
            case "shansuo_zhenyu", "\u5c71\u9501\u9547\u57df" -> SHANSUO_ZHENYU;
            case "wanfeng_yuezang", "\u4e07\u950b\u5cb3\u846c" -> WANFENG_YUEZANG;
            case "dongliu_suo", "\u51bb\u6d41\u9501" -> DONGLIU_SUO;
            case "cangyuan_hongliu", "\u6ca7\u6e0a\u6d2a\u6d41" -> CANGYUAN_HONGLIU;
            case "hanyuan_fengjie", "\u5bd2\u6e0a\u5c01\u754c" -> HANYUAN_FENGJIE;
            case "wanmu_sheng", "\u4e07\u6728\u751f" -> WANMU_SHENG;
            case "qingteng_fu", "\u9752\u85e4\u7f1a" -> QINGTENG_FU;
            case "luohua_chanshen", "\u843d\u82b1\u7f20\u8eab" -> LUOHUA_CHANSHEN;
            case "baihua_zang", "\u767e\u82b1\u846c" -> BAIHUA_ZANG;
            case "pokong_jianhen", "\u7834\u7a7a\u5251\u75d5" -> POKONG_JIANHEN;
            case "sanjian_zhuiming", "\u4e09\u5251\u8ffd\u547d" -> SANJIAN_ZHUIMING;
            case "diyan_qiang", "\u5730\u5ca9\u67aa" -> DIYAN_QIANG;
            case "shanbeng_yin", "\u5c71\u5d29\u5370" -> SHANBENG_YIN;
            case "shuiren_lianzhan", "\u6c34\u5203\u8fde\u65a9" -> SHUIREN_LIANZHAN;
            case "canglang_chong", "\u6ca7\u6d6a\u51b2" -> CANGLANG_CHONG;
            case "leishan", "\u96f7\u95ea" -> LEISHAN;
            case "wulei_hongding", "\u4e94\u96f7\u8f70\u9876" -> WULEI_HONGDING;
            case "bingzhui_yu", "\u51b0\u9525\u96e8" -> BINGZHUI_YU;
            case "hanyu_fengdi", "\u5bd2\u72f1\u5c01\u5730" -> HANYU_FENGDI;
            case "yaoguang_shan", "\u66dc\u5149\u95ea" -> YAOGUANG_SHAN;
            case "tianguang_guansha", "\u5929\u5149\u8d2f\u6740" -> TIANGUANG_GUANSHA;
            case "yingxi", "\u5f71\u88ad" -> YINGXI;
            case "tianfeng_zhui", "\u5929\u98ce\u5760" -> TIANFENG_ZHUI;
            case "muling_shengxi", "mulingshengxi", "\u6728\u7075\u751f\u606f" -> MULING_SHENGXI;
            case "xue_guiyuan_shu", "xueguiyuanshu", "\u8840\u5f52\u5143\u672f" -> XUE_GUIYUAN_SHU;
            case "shengxi_baofa", "shengxibaofa", "\u751f\u606f\u7206\u53d1" -> SHENGXI_BAOFA;
            default -> "";
        };
        if (!elemental.isEmpty()) {
            return elemental;
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
        if ("CuiguLianshen".equalsIgnoreCase(id) || "cuigu_lianshen".equalsIgnoreCase(id) || "cuigu lianshen".equalsIgnoreCase(id) || "\u6dec\u9aa8\u70bc\u8eab".equals(id)) {
            return CUIGU_LIANSHEN;
        }
        if ("JinhuoXinliao".equalsIgnoreCase(id) || "jinhuo_xinliao".equalsIgnoreCase(id)
                || "jinhuo xinliao".equalsIgnoreCase(id) || "\u70ec\u706b\u5fc3\u71ce".equals(id)) {
            return JINHUO_XINLIAO;
        }
        if ("SuiguiXingyun".equalsIgnoreCase(id) || "suigui_xingyun".equalsIgnoreCase(id)
                || "suigui xingyun".equalsIgnoreCase(id) || "\u788e\u8f68\u661f\u9668".equals(id)) {
            return SUIGUI_XINGYUN;
        }
        if ("DongtianJieling".equalsIgnoreCase(id) || "dongtian_jieling".equalsIgnoreCase(id)
                || "dongtian jieling".equalsIgnoreCase(id) || "\u6d1e\u5929\u501f\u7075".equals(id)) {
            return DONGTIAN_JIELING;
        }
        if ("ShixuChiliu".equalsIgnoreCase(id) || "shixu_chiliu".equalsIgnoreCase(id)
                || "shixu chiliu".equalsIgnoreCase(id) || "\u65f6\u5e8f\u8fdf\u6d41".equals(id)) {
            return SHIXU_CHILIU;
        }
        if ("SuokongShu".equalsIgnoreCase(id) || "suokong_shu".equalsIgnoreCase(id)
                || "suokong shu".equalsIgnoreCase(id) || "\u9501\u7a7a\u672f".equals(id)) {
            return SUOKONG_SHU;
        }
        return id;
    }

    public static boolean meetsRequirement(String currentLevel, SpellDef spell) {
        if (WEIYA.equals(spell.id()) || ZIBAO.equals(spell.id()) || DONGTIAN_JIELING.equals(spell.id())) {
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
        if (spell.requiredLevel().isBlank()) {
            return true;
        }
        return CultivationLevels.getStageIndex(currentLevel) >= CultivationLevels.getStageIndex(spell.requiredLevel());
    }

    public static boolean isInnateKnown(String spellId, String currentLevel) {
        String normalized = normalizeId(spellId);
        return ((WEIYA.equals(normalized) || ZIBAO.equals(normalized) || DONGTIAN_JIELING.equals(normalized))
                && isJindanOrHigher(currentLevel))
                || (YUQI_SHU.equals(normalized)
                && CultivationLevels.getStageIndex(currentLevel) >= CultivationLevels.getStageIndex(CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_EARLY));
    }

    public static boolean isInnateKnown(String spellId, ModAttachments.CultivationData data) {
        String normalized = normalizeId(spellId);
        if (data == null) {
            return false;
        }
        if (WEIYA.equals(normalized) || ZIBAO.equals(normalized) || DONGTIAN_JIELING.equals(normalized)) {
            return isJindanOrHigher(data.cultivationLevel());
        }
        if (YUQI_SHU.equals(normalized)) {
            return meetsRequirement(data.cultivationLevel(), get(YUQI_SHU));
        }
        if (SUOKONG_SHU.equals(normalized)) {
            return BodyTypes.VOID_BODY.equals(data.bodyType());
        }
        if (CUIGU_LIANSHEN.equals(normalized)) {
            return CultivationMethods.isCuiguLianshenJue(data.activeCultivationMethod());
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
        if (SolarTechniques.isTechnique(normalized)) {
            return CultivationMethods.isDariBatiJue(data.activeCultivationMethod());
        }
        if (GangtingTechniques.isTechnique(normalized)) {
            return CultivationMethods.isGangtingXiaokongLu(data.activeCultivationMethod())
                    && meetsRequirement(data.cultivationLevel(), get(normalized));
        }
        if (XuanyueTechniques.isTechnique(normalized)) {
            return CultivationMethods.isXuanyueSuifengJue(data.activeCultivationMethod())
                    && meetsRequirement(data.cultivationLevel(), get(normalized));
        }
        if (HanlanTechniques.isTechnique(normalized)) {
            return CultivationMethods.isHanlanNingyuanJing(data.activeCultivationMethod())
                    && meetsRequirement(data.cultivationLevel(), get(normalized));
        }
        if (ChangqingTechniques.isTechnique(normalized)) {
            return CultivationMethods.isChangqingJue(data.activeCultivationMethod())
                    && meetsRequirement(data.cultivationLevel(), get(normalized));
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

    public static boolean isRandomRewardEligible(String spellId) {
        String normalized = normalizeId(spellId);
        return !UndeadAbilitySystem.isPlayerAbility(normalized)
                && !WEIYA.equals(normalized)
                && !ZIBAO.equals(normalized)
                && !DONGTIAN_JIELING.equals(normalized)
                && !ABSORB_CULTIVATION.equals(normalized)
                && !TUNTIAN.equals(normalized)
                && !FENGYA.equals(normalized)
                && !YUQI_SHU.equals(normalized)
                && !CUIGU_LIANSHEN.equals(normalized)
                && !LIANSHEN_SHU.equals(normalized)
                && !SUOKONG_SHU.equals(normalized)
                && !HANJING_SUOZHUA.equals(normalized)
                && !SHUANGTIAN_QI.equals(normalized)
                && !SUISHUANG_LINGXIAO.equals(normalized)
                && !SolarTechniques.isTechnique(normalized)
                && !GangtingTechniques.isTechnique(normalized)
                && !XuanyueTechniques.isTechnique(normalized)
                && !HanlanTechniques.isTechnique(normalized)
                && !ChangqingTechniques.isTechnique(normalized);
    }

    public static boolean isTimedToggle(String spellId) {
        String normalized = normalizeId(spellId);
        return EARTH_ESCAPE.equals(normalized)
                || QI_GATHERING.equals(normalized)
                || SPIRIT_SIGHT.equals(normalized)
                || WIND_STEP.equals(normalized)
                || YUFENG_JUE.equals(normalized)
                || XUYING_TA.equals(normalized)
                || SHIXU_CHILIU.equals(normalized)
                || WEIYA.equals(normalized)
                || FENGYA.equals(normalized)
                || SUISHUANG_LINGXIAO.equals(normalized);
    }

    public static List<String> innateSpellIds() {
        ArrayList<String> ids = new ArrayList<>(List.of(
                WEIYA, ZIBAO, DONGTIAN_JIELING, ABSORB_CULTIVATION, TUNTIAN, FENGYA,
                HANJING_SUOZHUA, SHUANGTIAN_QI, SUISHUANG_LINGXIAO, YUQI_SHU, CUIGU_LIANSHEN,
                SUOKONG_SHU, YANGYAN_FUTI, DARI_ZHEN, LIEYANG_BU, RILUN, JIYANG_YIKE,
                LEIXIAO_GUANYUN, JIUXIAO_FENGLEI_YU,
                XUANYUE_BI, JINYAN_SHENGFENG, SHANSUO_ZHENYU, WANFENG_YUEZANG,
                DONGLIU_SUO, CANGYUAN_HONGLIU, HANYUAN_FENGJIE,
                WANMU_SHENG, QINGTENG_FU));
        ids.addAll(UndeadAbilitySystem.playerAbilityIds());
        return List.copyOf(ids);
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
