package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ImmortalCultivationMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> IMMORTAL_TAB =
            CREATIVE_TABS.register("immortal_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + ImmortalCultivationMod.MODID))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.BREAKTHROUGH_PILL.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {

                        output.accept(ModItems.FIREBALL_SCROLL.get());
                        output.accept(ModItems.LINGBENG_SCROLL.get());
                        output.accept(ModItems.REGENERATION_SCROLL.get());
                        output.accept(ModItems.BEAM_SCROLL.get());
                        output.accept(ModItems.EARTH_ESCAPE_SCROLL.get());
                        output.accept(ModItems.CLEANSE_SCROLL.get());
                        output.accept(ModItems.QI_GATHERING_SCROLL.get());
                        output.accept(ModItems.IGNITE_FLARE_SCROLL.get());
                        output.accept(ModItems.SPIRIT_SIGHT_SCROLL.get());
                        output.accept(ModItems.ZHENSHAN_PALM_SCROLL.get());
                        output.accept(ModItems.LIGHT_BEAM_ATTACK_SCROLL.get());
                        output.accept(ModItems.DIELANG_SHIELD_SCROLL.get());
                        output.accept(ModItems.LINGZHI_BULLET_SCROLL.get());
                        output.accept(ModItems.WIND_BLADE_SCROLL.get());
                        output.accept(ModItems.WIND_STEP_SCROLL.get());
                        output.accept(ModItems.YUFENG_JUE_SCROLL.get());
                        output.accept(ModItems.SMOKE_ART_SCROLL.get());
                        output.accept(ModItems.HUTI_QI_SCROLL.get());
                        output.accept(ModItems.MICHEN_ZHANG_SCROLL.get());
                        output.accept(ModItems.SLIDING_WATER_SCROLL.get());
                        output.accept(ModItems.DINGSHEN_SCROLL.get());
                        output.accept(ModItems.YINLEI_JUE_SCROLL.get());
                        output.accept(ModItems.WULEI_ZHENGFA_SCROLL.get());
                        output.accept(ModItems.LIUGUANG_JIANYING_SCROLL.get());
                        output.accept(ModItems.SIFANG_JIE_SCROLL.get());
                        output.accept(ModItems.GUSHI_SHIELD_SCROLL.get());
                        output.accept(ModItems.KONGSHI_SHU_SCROLL.get());
                        output.accept(ModItems.XUYING_TA_SCROLL.get());
                        output.accept(ModItems.DUANLIU_KONGDUN_SCROLL.get());
                        output.accept(ModItems.YIHEN_CI_SCROLL.get());
                        output.accept(ModItems.JINHUO_XINLIAO_SCROLL.get());
                        output.accept(ModItems.SUIGUI_XINGYUN_SCROLL.get());
                        output.accept(ModItems.SHIXU_CHILIU_SCROLL.get());
                        output.accept(ModItems.QI_POUCH.get());
                        output.accept(ModItems.DEBUG_STAT_EDITOR.get());
                        output.accept(ModItems.RESTRICTION_INSIGHT.get());
                        output.accept(ModItems.FOG_REVEALING_MIRROR.get());
                        output.accept(ModItems.YINYANG_COMPASS.get());
                        output.accept(ModItems.BLOOD.get());
                        output.accept(ModItems.BLOOD_CRYSTAL.get());
                        output.accept(ModItems.CHI_LIN.get());
                        output.accept(ModItems.HUO_XI_BLOOD.get());
                        output.accept(ModItems.JINDAN_YAODAN.get());
                        output.accept(ModItems.CHIYAN_BONE.get());
                        output.accept(ModItems.YUANYING_YAODAN.get());
                        output.accept(ModItems.YANJIAO_HORN.get());
                        output.accept(ModItems.YAOYUAN_BONE.get());
                        output.accept(ModItems.XUESHA_HIDE.get());
                        output.accept(ModItems.NUOMI.get());
                        output.accept(ModItems.NUOMI_SEED.get());
                        output.accept(ModItems.NUOMI_ZHOU.get());
                        output.accept(ModItems.MODOU_LINE.get());
                        output.accept(ModItems.RED_STRING.get());
                        output.accept(ModItems.COPPER_COIN.get());
                        output.accept(ModItems.FAMILY_TOKEN.get());
                        output.accept(ModItems.ROYAL_TOKEN.get());
                        output.accept(ModItems.WANTED_ORDER.get());
                        output.accept(ModItems.LETHAL_POISON.get());
                        output.accept(ModItems.FEIJIAN_YUJIAN.get());
                        output.accept(ModItems.DAO_YUJIAN.get());
                        output.accept(ModItems.JINGTIE_SPEAR_YUJIAN.get());
                        output.accept(ModItems.HEAVY_IRON_BRACER_YUJIAN.get());
                        output.accept(ModItems.THROWING_KNIFE_YUJIAN.get());
                        output.accept(ModItems.FLYING_NEEDLE_YUJIAN.get());
                        output.accept(ModItems.ZHENPAN_YUJIAN.get());
                        output.accept(ModItems.BLANK_FORMATION_FLAG_YUJIAN.get());
                        output.accept(ModItems.DURATION_FLAG_YUJIAN.get());
                        output.accept(ModItems.AMPLIFIER_FLAG_YUJIAN.get());
                        output.accept(ModItems.SPIRIT_GATHERING_FLAG_YUJIAN.get());
                        output.accept(ModItems.HASTE_FLAG_YUJIAN.get());
                        output.accept(ModItems.QI_RETURN_FLAG_YUJIAN.get());
                        output.accept(ModItems.BASIC_BREATHING_METHOD.get());
                        output.accept(ModItems.CLEAR_HEART_METHOD.get());
                        output.accept(ModItems.BLOOD_DEMON_JINDAN_METHOD.get());
                        output.accept(ModItems.BLOOD_DEMON_YUANYING_METHOD.get());
                        output.accept(ModItems.BLOOD_DEMON_HUASHEN_METHOD.get());
                        output.accept(ModItems.REINCARNATION_TRUE_ART_METHOD.get());
                        output.accept(ModItems.TUNTIAN_DEMON_ART_METHOD.get());
                        output.accept(ModItems.POKONG_JUE_METHOD.get());
                        output.accept(ModItems.CHANGQING_JUE_METHOD.get());
                        output.accept(ModItems.FENTIAN_LIFE_RENEWAL_METHOD.get());
                        output.accept(ModItems.HANTI_BINGQIN_METHOD.get());
                        output.accept(ModItems.CUIGU_LIANSHEN_METHOD.get());
                        output.accept(ModItems.CORPSE_REFINING_SECRET.get());
                        output.accept(ModItems.SHIHUNFAN.get());
                        output.accept(ModItems.YUANHUN_BOTTLE.get());
                        output.accept(ModItems.GREAT_CREATION_SHARD.get());

                    }).build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> IMMORTAL_WEAPONS_TAB =
            CREATIVE_TABS.register("immortal_weapons_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + ImmortalCultivationMod.MODID + ".weapons"))
                    .withTabsBefore(IMMORTAL_TAB.getKey())
                    .icon(() -> ModItems.FLYING_SWORD.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.TAOMU_SWORD.get());
                        output.accept(ModItems.COPPER_COIN_SWORD.get());
                        output.accept(ModItems.FLYING_SWORD.get());
                        output.accept(ModItems.DAO.get());
                        output.accept(ModItems.JINGTIE_SPEAR.get());
                        output.accept(ModItems.THROWING_KNIFE.get());
                        output.accept(ModItems.FLYING_NEEDLE.get());
                        output.accept(ModItems.TIEGU_SHAN.get());
                        output.accept(ModItems.FUCHEN.get());
                        output.accept(ModItems.WHIP.get());
                        output.accept(ModItems.HEAVY_IRON_BRACER.get());
                        output.accept(ModItems.YAOYUAN_FIST_BONE.get());
                        output.accept(ModItems.SWORD_MOLD.get());
                        output.accept(ModItems.SABER_MOLD.get());
                        output.accept(ModItems.SPEAR_MOLD.get());
                        output.accept(ModItems.WEAPON_MOLD.get());
                        output.accept(ModItems.LOW_YANGJIAN_STONE.get());
                        output.accept(ModItems.MIDDLE_YANGJIAN_STONE.get());
                        output.accept(ModItems.HIGH_YANGJIAN_STONE.get());
                        output.accept(ModItems.LOW_SWORD_GRINDSTONE.get());
                        output.accept(ModItems.MIDDLE_SWORD_GRINDSTONE.get());
                        output.accept(ModItems.HIGH_SWORD_GRINDSTONE.get());
                        output.accept(ModItems.LOW_SABER_GRINDSTONE.get());
                        output.accept(ModItems.MIDDLE_SABER_GRINDSTONE.get());
                        output.accept(ModItems.HIGH_SABER_GRINDSTONE.get());
                    }).build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> IMMORTAL_ACCESSORIES_TAB =
            CREATIVE_TABS.register("immortal_accessories_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + ImmortalCultivationMod.MODID + ".accessories"))
                    .withTabsBefore(IMMORTAL_WEAPONS_TAB.getKey())
                    .icon(() -> ModItems.JADE_PENDANT.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.ZHENHUN_BELL.get());
                        output.accept(ModItems.YINHUN_GONG.get());
                        output.accept(ModItems.COFFIN.get());
                        output.accept(ModItems.FLAG.get());
                        output.accept(ModItems.CLOTH_SHEATH.get());
                        output.accept(ModItems.SWORD_CASE.get());
                        output.accept(ModItems.XIUJIAN.get());
                        output.accept(ModItems.BLINDFOLD.get());
                        output.accept(ModItems.BLINDFOLD2.get());
                        output.accept(ModItems.STRAW_HAT.get());
                        output.accept(ModItems.JADE_PENDANT.get());
                        output.accept(ModItems.DAOFU_CHESTPLATE.get());
                        output.accept(ModItems.NEIJIA_CHESTPLATE.get());
                        output.accept(ModItems.ZHONGYI.get());
                        output.accept(ModItems.DISGUISE_MASK.get());
                        output.accept(ModItems.PROTECTIVE_JADE_PENDANT.get());
                        output.accept(ModItems.MYSTERIOUS_RING.get());
                        output.accept(ModItems.SYSTEM.get());
                        output.accept(ModItems.WHITE_WOOL_STRIP.get());
                        output.accept(ModItems.ORANGE_WOOL_STRIP.get());
                        output.accept(ModItems.MAGENTA_WOOL_STRIP.get());
                        output.accept(ModItems.LIGHT_BLUE_WOOL_STRIP.get());
                        output.accept(ModItems.YELLOW_WOOL_STRIP.get());
                        output.accept(ModItems.LIME_WOOL_STRIP.get());
                        output.accept(ModItems.PINK_WOOL_STRIP.get());
                        output.accept(ModItems.GRAY_WOOL_STRIP.get());
                        output.accept(ModItems.LIGHT_GRAY_WOOL_STRIP.get());
                        output.accept(ModItems.CYAN_WOOL_STRIP.get());
                        output.accept(ModItems.PURPLE_WOOL_STRIP.get());
                        output.accept(ModItems.BLUE_WOOL_STRIP.get());
                        output.accept(ModItems.BROWN_WOOL_STRIP.get());
                        output.accept(ModItems.GREEN_WOOL_STRIP.get());
                        output.accept(ModItems.RED_WOOL_STRIP.get());
                        output.accept(ModItems.BLACK_WOOL_STRIP.get());
                    }).build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> IMMORTAL_TALISMANS_TAB =
            CREATIVE_TABS.register("immortal_talismans_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + ImmortalCultivationMod.MODID + ".talismans"))
                    .withTabsBefore(IMMORTAL_ACCESSORIES_TAB.getKey())
                    .icon(() -> ModItems.ZHENSHI_TALISMAN.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.ZHENSHI_TALISMAN.get());
                        output.accept(ModItems.FENGYAN_TALISMAN.get());
                        output.accept(ModItems.FIREBALL_TALISMAN.get());
                    }).build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> IMMORTAL_PILLS_TAB =
            CREATIVE_TABS.register("immortal_pills_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + ImmortalCultivationMod.MODID + ".pills"))
                    .withTabsBefore(IMMORTAL_TALISMANS_TAB.getKey())
                    .icon(() -> ModItems.BREAKTHROUGH_PILL.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.ENLIGHTENMENT_PILL.get());
                        output.accept(ModItems.BREAKTHROUGH_PILL.get());
                        output.accept(ModItems.SMALL_LEVEL_UP.get());
                        output.accept(ModItems.BIG_LEVEL_UP.get());
                        output.accept(ModItems.BIGU_PILL.get());
                        output.accept(ModItems.LOW_QI_RECOVERY_PILL.get());
                        output.accept(ModItems.MIDDLE_QI_RECOVERY_PILL.get());
                        output.accept(ModItems.HIGH_QI_RECOVERY_PILL.get());
                        output.accept(ModItems.BODY_TEMPERING_PILL.get());
                        output.accept(ModItems.BLOOD_QI_PILL.get());
                    }).build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> IMMORTAL_BLOCKS_TAB =
            CREATIVE_TABS.register("immortal_blocks_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + ImmortalCultivationMod.MODID + ".blocks"))
                    .withTabsBefore(IMMORTAL_PILLS_TAB.getKey())
                    .icon(() -> ModBlocks.QINGLING_GRASS.get().asItem().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.QINGLING_GRASS.get());
                        output.accept(ModBlocks.CHIYAN_FLOWER.get());
                        output.accept(ModBlocks.NINGLU_ZHI.get());
                        output.accept(ModBlocks.XUANBING_LOTUS.get());
                        output.accept(ModBlocks.ZIYUN_VINE.get());
                        output.accept(ModBlocks.JINWEN_FRUIT.get());
                        output.accept(ModBlocks.SHIHUN_GRASS.get());
                        output.accept(ModBlocks.JIUYE_XIANLAN.get());
                        output.accept(ModBlocks.LINGMU.get());
                        output.accept(ModBlocks.LINGMU_LOG.get());
                        output.accept(ModBlocks.LINGMU_LEAVES.get());
                        output.accept(ModBlocks.QIANNIAN_LINGMU_HEART.get());
                        output.accept(ModBlocks.FUGU_TAI.get());
                        output.accept(ModBlocks.HUANXIA_SI.get());
                        output.accept(ModBlocks.JINGTIE_MOTHERSTONE.get());
                        output.accept(ModBlocks.YUNWEN_SILVER_CRYSTAL.get());
                        output.accept(ModBlocks.CHITONG_SUI.get());
                        output.accept(ModBlocks.HANPO_JADE.get());
                        output.accept(ModBlocks.MOXUAN_IRON.get());
                        output.accept(ModBlocks.LIUJIN_SAND.get());
                        output.accept(ModBlocks.BISUI_STONE.get());
                        output.accept(ModItems.LINGJIAO_SCALE.get());
                        output.accept(ModBlocks.STAR_DUST_STONE.get());
                        output.accept(ModBlocks.CU_TIE_XIAODING.get());
                        output.accept(ModBlocks.QINGTONG_YINHUO_DING.get());
                        output.accept(ModBlocks.RESTRICTION_TABLE.get());
                        output.accept(ModBlocks.DUNWU_STELE.get());

                    }).build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> IMMORTAL_SPAWN_EGGS_TAB =
            CREATIVE_TABS.register("immortal_spawn_eggs_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + ImmortalCultivationMod.MODID + ".spawn_eggs"))
                    .withTabsBefore(IMMORTAL_BLOCKS_TAB.getKey())
                    .icon(() -> ModItems.ZHUJI_CULTIVATOR_SPAWN_EGG.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.ZHUJI_CULTIVATOR_SPAWN_EGG.get());
                        output.accept(ModItems.JINDAN_CULTIVATOR_SPAWN_EGG.get());
                        output.accept(ModItems.LIUSAN_SPAWN_EGG.get());
                        output.accept(ModItems.FANREN_NPC_SPAWN_EGG.get());
                        output.accept(ModItems.CRIMSON_SCALE_FLAME_LIZARD_SPAWN_EGG.get());
                        output.accept(ModItems.CRIMSON_SKY_FLAME_FLOOD_DRAGON_SPAWN_EGG.get());
                        output.accept(ModItems.BLOOD_APE_DEMON_GENERAL_SPAWN_EGG.get());
                        output.accept(ModItems.AZURE_SPIRIT_SILKWORM_SPAWN_EGG.get());
                    }).build());
}
