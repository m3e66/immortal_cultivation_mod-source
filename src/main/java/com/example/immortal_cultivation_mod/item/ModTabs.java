package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModTabs {
   private static final List<String> SPELL_ELEMENT_ORDER = List.of("\u91d1", "\u6728", "\u6c34", "\u706b", "\u571f", "\u96f7", "\u98ce", "\u51b0", "\u9633", "\u9634", "blood");
   public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS;
   public static final DeferredHolder<CreativeModeTab, CreativeModeTab> IMMORTAL_TAB;
   public static final DeferredHolder<CreativeModeTab, CreativeModeTab> IMMORTAL_WEAPONS_TAB;
   public static final DeferredHolder<CreativeModeTab, CreativeModeTab> IMMORTAL_ACCESSORIES_TAB;
   public static final DeferredHolder<CreativeModeTab, CreativeModeTab> IMMORTAL_TALISMANS_TAB;
   public static final DeferredHolder<CreativeModeTab, CreativeModeTab> IMMORTAL_PILLS_TAB;
   public static final DeferredHolder<CreativeModeTab, CreativeModeTab> IMMORTAL_CULTIVATION_TAB;
   public static final DeferredHolder<CreativeModeTab, CreativeModeTab> IMMORTAL_MAGIC_CIRCLE_TAB;
   public static final DeferredHolder<CreativeModeTab, CreativeModeTab> IMMORTAL_BLOCKS_TAB;
   public static final DeferredHolder<CreativeModeTab, CreativeModeTab> IMMORTAL_SPAWN_EGGS_TAB;

   private static void acceptSpellScrollsByElement(CreativeModeTab.Output output) {
      List<SpellScrollEntry> scrolls = List.of(new SpellScrollEntry("fireball", (ItemLike)ModItems.FIREBALL_SCROLL.get()), new SpellScrollEntry("lingbeng", (ItemLike)ModItems.LINGBENG_SCROLL.get()), new SpellScrollEntry("regeneration", (ItemLike)ModItems.REGENERATION_SCROLL.get()), new SpellScrollEntry("beam", (ItemLike)ModItems.BEAM_SCROLL.get()), new SpellScrollEntry("earth_escape", (ItemLike)ModItems.EARTH_ESCAPE_SCROLL.get()), new SpellScrollEntry("cleanse", (ItemLike)ModItems.CLEANSE_SCROLL.get()), new SpellScrollEntry("qi_gathering", (ItemLike)ModItems.QI_GATHERING_SCROLL.get()), new SpellScrollEntry("ignite_flare", (ItemLike)ModItems.IGNITE_FLARE_SCROLL.get()), new SpellScrollEntry("spirit_sight", (ItemLike)ModItems.SPIRIT_SIGHT_SCROLL.get()), new SpellScrollEntry("zhenshan_palm", (ItemLike)ModItems.ZHENSHAN_PALM_SCROLL.get()), new SpellScrollEntry("light_beam_attack", (ItemLike)ModItems.LIGHT_BEAM_ATTACK_SCROLL.get()), new SpellScrollEntry("dielang_shield", (ItemLike)ModItems.DIELANG_SHIELD_SCROLL.get()), new SpellScrollEntry("lingzhi_bullet", (ItemLike)ModItems.LINGZHI_BULLET_SCROLL.get()), new SpellScrollEntry("jinmang_chuansha", (ItemLike)ModItems.JINMANG_CHUANSHA_SCROLL.get()), new SpellScrollEntry("jinfeng_zhi", (ItemLike)ModItems.JINFENG_ZHI_SCROLL.get()), new SpellScrollEntry("wanren_chuanxin", (ItemLike)ModItems.WANREN_CHUANXIN_SCROLL.get()), new SpellScrollEntry("jingji_tusheng", (ItemLike)ModItems.JINGJI_TUSHENG_SCROLL.get()), new SpellScrollEntry("wanteng_jiaosha", (ItemLike)ModItems.WANTENG_JIAOSHA_SCROLL.get()), new SpellScrollEntry("yanbao_dan", (ItemLike)ModItems.YANBAO_DAN_SCROLL.get()), new SpellScrollEntry("chiyan_longjuan", (ItemLike)ModItems.CHIYAN_LONGJUAN_SCROLL.get()), new SpellScrollEntry("tianyun_fencheng", (ItemLike)ModItems.TIANYUN_FENCHENG_SCROLL.get()), new SpellScrollEntry("luohua_chanshen", (ItemLike)ModItems.LUOHUA_CHANSHEN_SCROLL.get()), new SpellScrollEntry("baihua_zang", (ItemLike)ModItems.BAIHUA_ZANG_SCROLL.get()), new SpellScrollEntry("pokong_jianhen", (ItemLike)ModItems.POKONG_JIANHEN_SCROLL.get()), new SpellScrollEntry("sanjian_zhuiming", (ItemLike)ModItems.SANJIAN_ZHUIMING_SCROLL.get()), new SpellScrollEntry("diyan_qiang", (ItemLike)ModItems.DIYAN_QIANG_SCROLL.get()), new SpellScrollEntry("shanbeng_yin", (ItemLike)ModItems.SHANBENG_YIN_SCROLL.get()), new SpellScrollEntry("shuiren_lianzhan", (ItemLike)ModItems.SHUIREN_LIANZHAN_SCROLL.get()), new SpellScrollEntry("canglang_chong", (ItemLike)ModItems.CANGLANG_CHONG_SCROLL.get()), new SpellScrollEntry("leishan", (ItemLike)ModItems.LEISHAN_SCROLL.get()), new SpellScrollEntry("wulei_hongding", (ItemLike)ModItems.WULEI_HONGDING_SCROLL.get()), new SpellScrollEntry("bingzhui_yu", (ItemLike)ModItems.BINGZHUI_YU_SCROLL.get()), new SpellScrollEntry("hanyu_fengdi", (ItemLike)ModItems.HANYU_FENGDI_SCROLL.get()), new SpellScrollEntry("yaoguang_shan", (ItemLike)ModItems.YAOGUANG_SHAN_SCROLL.get()), new SpellScrollEntry("tianguang_guansha", (ItemLike)ModItems.TIANGUANG_GUANSHA_SCROLL.get()), new SpellScrollEntry("yingxi", (ItemLike)ModItems.YINGXI_SCROLL.get()), new SpellScrollEntry("tianfeng_zhui", (ItemLike)ModItems.TIANFENG_ZHUI_SCROLL.get()), new SpellScrollEntry("muling_shengxi", (ItemLike)ModItems.MULING_SHENGXI_SCROLL.get()), new SpellScrollEntry("xue_guiyuan_shu", (ItemLike)ModItems.XUE_GUIYUAN_SHU_SCROLL.get()), new SpellScrollEntry("shengxi_baofa", (ItemLike)ModItems.SHENGXI_BAOFA_SCROLL.get()), new SpellScrollEntry("wind_blade", (ItemLike)ModItems.WIND_BLADE_SCROLL.get()), new SpellScrollEntry("wind_step", (ItemLike)ModItems.WIND_STEP_SCROLL.get()), new SpellScrollEntry("yufeng_jue", (ItemLike)ModItems.YUFENG_JUE_SCROLL.get()), new SpellScrollEntry("smoke_art", (ItemLike)ModItems.SMOKE_ART_SCROLL.get()), new SpellScrollEntry("huti_qi", (ItemLike)ModItems.HUTI_QI_SCROLL.get()), new SpellScrollEntry("michen_zhang", (ItemLike)ModItems.MICHEN_ZHANG_SCROLL.get()), new SpellScrollEntry("sliding_water", (ItemLike)ModItems.SLIDING_WATER_SCROLL.get()), new SpellScrollEntry("dingshen", (ItemLike)ModItems.DINGSHEN_SCROLL.get()), new SpellScrollEntry("yinlei_jue", (ItemLike)ModItems.YINLEI_JUE_SCROLL.get()), new SpellScrollEntry("wulei_zhengfa", (ItemLike)ModItems.WULEI_ZHENGFA_SCROLL.get()), new SpellScrollEntry("liuguang_jianying", (ItemLike)ModItems.LIUGUANG_JIANYING_SCROLL.get()), new SpellScrollEntry("sifang_jie", (ItemLike)ModItems.SIFANG_JIE_SCROLL.get()), new SpellScrollEntry("gushi_shield", (ItemLike)ModItems.GUSHI_SHIELD_SCROLL.get()), new SpellScrollEntry("xuanyan_huti", (ItemLike)ModItems.XUANYAN_HUTI_SCROLL.get()), new SpellScrollEntry("liufeng_xiejin", (ItemLike)ModItems.LIUFENG_XIEJIN_SCROLL.get()), new SpellScrollEntry("xuesha_hushen", (ItemLike)ModItems.XUESHA_HUSHEN_SCROLL.get()), new SpellScrollEntry("jingling_fanzhao", (ItemLike)ModItems.JINGLING_FANZHAO_SCROLL.get()), new SpellScrollEntry("yuangang_hujie", (ItemLike)ModItems.YUANGANG_HUJIE_SCROLL.get()), new SpellScrollEntry("kongshi_shu", (ItemLike)ModItems.KONGSHI_SHU_SCROLL.get()), new SpellScrollEntry("xuying_ta", (ItemLike)ModItems.XUYING_TA_SCROLL.get()), new SpellScrollEntry("duanliu_kongdun", (ItemLike)ModItems.DUANLIU_KONGDUN_SCROLL.get()), new SpellScrollEntry("yihen_ci", (ItemLike)ModItems.YIHEN_CI_SCROLL.get()), new SpellScrollEntry("jinhuo_xinliao", (ItemLike)ModItems.JINHUO_XINLIAO_SCROLL.get()), new SpellScrollEntry("suigui_xingyun", (ItemLike)ModItems.SUIGUI_XINGYUN_SCROLL.get()), new SpellScrollEntry("shixu_chiliu", (ItemLike)ModItems.SHIXU_CHILIU_SCROLL.get()));
      scrolls.stream().sorted(Comparator.comparingInt((entry) -> spellElementOrder(entry.spellId()))).forEach((entry) -> output.accept(entry.item()));
   }

   private static int spellElementOrder(String spellId) {
      ModSpells.SpellDef spell = ModSpells.get(spellId);
      if (spell != null && spell.element() != null && !spell.element().isBlank()) {
         int best = SPELL_ELEMENT_ORDER.size();

         for(String element : spell.element().split(",")) {
            int index = SPELL_ELEMENT_ORDER.indexOf(element.trim());
            if (index >= 0) {
               best = Math.min(best, index);
            }
         }

         return best;
      } else {
         return SPELL_ELEMENT_ORDER.size();
      }
   }

   private static ResourceLocation tabId(String path) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", path);
   }

   static {
      CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "immortal_cultivation_mod");
      IMMORTAL_TAB = CREATIVE_TABS.register("immortal_tab", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.immortal_cultivation_mod.drops")).icon(() -> ((Item)ModItems.ZHUJI_YAODAN.get()).getDefaultInstance()).displayItems((parameters, output) -> {
            output.accept((ItemLike)ModItems.DEBUG_STAT_EDITOR.get());
            output.accept((ItemLike)ModItems.DEBUG_TIME_CONTROLLER.get());
            output.accept((ItemLike)ModItems.TRAINING_DUMMY.get());
            output.accept((ItemLike)ModItems.DUMMY_CONTROL_STICK.get());
            output.accept((ItemLike)ModItems.RESTRICTION_INSIGHT.get());
            output.accept((ItemLike)ModItems.BLOOD.get());
            output.accept((ItemLike)ModItems.BLOOD_CRYSTAL.get());
            output.accept((ItemLike)ModItems.ASHES.get());
            output.accept((ItemLike)ModItems.WEAPON_SHARD.get());
            output.accept((ItemLike)ModItems.LIANQI_YAODAN.get());
            output.accept((ItemLike)ModItems.ZHUJI_YAODAN.get());
            output.accept((ItemLike)ModItems.JINDAN_YAODAN.get());
            output.accept((ItemLike)ModItems.YUANYING_YAODAN.get());
            output.accept((ItemLike)ModItems.HUASHEN_YAODAN.get());
            output.accept((ItemLike)ModItems.LIANXU_YAODAN.get());
            output.accept((ItemLike)ModItems.HETI_YAODAN.get());
            output.accept((ItemLike)ModItems.DACHENG_YAODAN.get());
            output.accept((ItemLike)ModItems.DUJIE_YAODAN.get());
            output.accept((ItemLike)ModItems.CHI_LIN.get());
            output.accept((ItemLike)ModItems.HUO_XI_BLOOD.get());
            output.accept((ItemLike)ModItems.CHIYAN_BONE.get());
            output.accept((ItemLike)ModItems.YANJIAO_HORN.get());
            output.accept((ItemLike)ModItems.YAOYUAN_BONE.get());
            output.accept((ItemLike)ModItems.XUESHA_HIDE.get());
            output.accept((ItemLike)ModItems.PHANTOM_VEIL_FUR.get());
            output.accept((ItemLike)ModItems.KONGMING_STONE.get());
            output.accept((ItemLike)ModItems.ROCK_BADGER_HIDE.get());
            output.accept((ItemLike)ModItems.THUNDER_PATTERNED_ROCK_PLATE.get());
            output.accept((ItemLike)ModItems.THUNDER_HORN.get());
            output.accept((ItemLike)ModItems.FROSTSPINE_HIDE.get());
            output.accept((ItemLike)ModItems.SPIRIT_DEVOURING_RAT_HIDE.get());
            output.accept((ItemLike)ModItems.SPIRIT_SCENT_WHISKER.get());
            output.accept((ItemLike)ModItems.IRON_SCALE_PLATE.get());
            output.accept((ItemLike)ModItems.CRIMSON_VENOM_SAC.get());
            output.accept((ItemLike)ModItems.SCORPION_TAIL_STINGER.get());
            output.accept((ItemLike)ModItems.WIND_FEATHER.get());
            output.accept((ItemLike)ModItems.SPIRIT_DEER_ANTLER.get());
            output.accept((ItemLike)ModItems.INK_ARMOR_PLATE.get());
            output.accept((ItemLike)ModItems.MINERAL_MARROW.get());
            output.accept((ItemLike)ModItems.PROFOUND_IRON_ARMOR_CORE.get());
            output.accept((ItemLike)ModItems.EMBER_FEATHER.get());
            output.accept((ItemLike)ModItems.FIRE_OWL_CLAW.get());
            output.accept((ItemLike)ModItems.FLAME_RADIANCE_BONE.get());
            output.accept((ItemLike)ModItems.MORTAL_BEAST_MEAT.get());
            output.accept((ItemLike)ModItems.LIANQI_BEAST_MEAT.get());
            output.accept((ItemLike)ModItems.ZHUJI_BEAST_MEAT.get());
            output.accept((ItemLike)ModItems.JINDAN_BEAST_MEAT.get());
            output.accept((ItemLike)ModItems.YUANYING_BEAST_MEAT.get());
            output.accept((ItemLike)ModItems.HUASHEN_BEAST_MEAT.get());
            output.accept((ItemLike)ModItems.LIANXU_BEAST_MEAT.get());
            output.accept((ItemLike)ModItems.HETI_BEAST_MEAT.get());
            output.accept((ItemLike)ModItems.DACHENG_BEAST_MEAT.get());
            output.accept((ItemLike)ModItems.DUJIE_BEAST_MEAT.get());
            output.accept((ItemLike)ModItems.COLD_MARROW.get());
            output.accept((ItemLike)ModItems.XUANDUAN_IRON_SHARD.get());
            output.accept((ItemLike)ModItems.SPIRIT_GUIDING_GEAR.get());
            output.accept((ItemLike)ModItems.DAMAGED_FORGING_FURNACE_CORE.get());
            output.accept((ItemLike)ModItems.LINGMU_CLASP.get());
            output.accept((ItemLike)ModItems.LINGJIAO_SCALE.get());
            output.accept((ItemLike)ModItems.GREAT_CREATION_SHARD.get());
            output.accept((ItemLike)com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_SILKWORM_SILK.get());
            output.accept((ItemLike)com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_GUIDING_THREAD.get());
            output.accept((ItemLike)com.example.immortal_cultivation_mod.magicritual.item.ModItems.LINGMU_FLAGPOLE.get());
            output.accept((ItemLike)com.example.immortal_cultivation_mod.magicritual.item.ModItems.FLOWING_SPIRIT_CRYSTAL.get());
            output.accept((ItemLike)com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_STORAGE_CRYSTAL.get());
            output.accept((ItemLike)com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENWEN_JADE.get());
            output.accept((ItemLike)com.example.immortal_cultivation_mod.magicritual.item.ModItems.PATTERNED_CLOTH.get());
            output.accept((ItemLike)com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_MAP.get());
            output.accept((ItemLike)ModItems.NUOMI.get());
            output.accept((ItemLike)ModItems.NUOMI_SEED.get());
            output.accept((ItemLike)ModItems.NUOMI_ZHOU.get());
            output.accept((ItemLike)ModItems.RED_STRING.get());
            output.accept((ItemLike)ModItems.COPPER_COIN.get());
            output.accept((ItemLike)ModItems.WINE.get());
            output.accept((ItemLike)ModItems.STEAMED_BUN.get());
            output.accept((ItemLike)ModItems.NOODLE_BOWL.get());
            output.accept((ItemLike)ModItems.BRAISED_PORK.get());
            output.accept((ItemLike)ModItems.ROAST_DUCK.get());
            output.accept((ItemLike)ModItems.FAMILY_TOKEN.get());
            output.accept((ItemLike)ModItems.ROYAL_TOKEN.get());
            output.accept((ItemLike)ModItems.WANTED_ORDER.get());
            output.accept((ItemLike)ModItems.LETHAL_POISON.get());
         }).build());
      IMMORTAL_WEAPONS_TAB = CREATIVE_TABS.register("immortal_weapons_tab", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.immortal_cultivation_mod.weapons")).withTabsBefore(new ResourceLocation[]{tabId("immortal_tab")}).icon(() -> ((Item)ModItems.FLYING_SWORD.get()).getDefaultInstance()).displayItems((parameters, output) -> {
            output.accept((ItemLike)ModItems.TAOMU_SWORD.get());
            output.accept((ItemLike)ModItems.COPPER_COIN_SWORD.get());
            output.accept((ItemLike)ModItems.FLYING_SWORD.get());
            output.accept((ItemLike)ModItems.DAO.get());
            output.accept((ItemLike)ModItems.JINGTIE_SPEAR.get());
            output.accept((ItemLike)ModItems.THROWING_KNIFE.get());
            output.accept((ItemLike)ModItems.FLYING_NEEDLE.get());
            output.accept((ItemLike)ModItems.TIEGU_SHAN.get());
            output.accept(ModItems.BLACK_IRON_JIAN.get());
            output.accept(ModItems.BLACK_IRON_GREATSWORD.get());
            output.accept(ModItems.MOON_CRESCENT_HALBERD.get());
            output.accept(ModItems.SPIRIT_SILK_WHIP.get());
            output.accept(ModItems.SOUL_HOOK.get());
            output.accept(ModItems.WIND_FEATHER_FAN.get());
            output.accept(ModItems.THUNDERWOOD_SWORD.get());
            output.accept(ModItems.FLAME_PATTERN_DAO.get());
            output.accept(ModItems.EARTHBREAKER_HAMMER.get());
            output.accept(ModItems.SEVEN_STAR_SWORD.get());

            output.accept((ItemLike)ModItems.FUCHEN.get());
            output.accept((ItemLike)ModItems.WHIP.get());
            output.accept((ItemLike)ModItems.HEAVY_IRON_BRACER.get());
            output.accept((ItemLike)ModItems.YAOYUAN_FIST_BONE.get());
            output.accept((ItemLike)ModItems.SHIHUNFAN.get());
            output.accept((ItemLike)ModItems.MODOU_LINE.get());
            output.accept((ItemLike)ModItems.FOG_REVEALING_MIRROR.get());
            output.accept((ItemLike)ModItems.YINYANG_COMPASS.get());
            output.accept((ItemLike)ModItems.ZHENHUN_BELL.get());
            output.accept((ItemLike)ModItems.YINHUN_GONG.get());
            output.accept((ItemLike)ModItems.YUANHUN_BOTTLE.get());
            output.accept((ItemLike)com.example.immortal_cultivation_mod.magicritual.item.ModItems.RITUAL_DAGGER.get());
            output.accept((ItemLike)ModItems.LOW_YANGJIAN_STONE.get());
            output.accept((ItemLike)ModItems.MIDDLE_YANGJIAN_STONE.get());
            output.accept((ItemLike)ModItems.HIGH_YANGJIAN_STONE.get());
            output.accept((ItemLike)ModItems.LOW_SWORD_GRINDSTONE.get());
            output.accept((ItemLike)ModItems.MIDDLE_SWORD_GRINDSTONE.get());
            output.accept((ItemLike)ModItems.HIGH_SWORD_GRINDSTONE.get());
            output.accept((ItemLike)ModItems.LOW_SABER_GRINDSTONE.get());
            output.accept((ItemLike)ModItems.MIDDLE_SABER_GRINDSTONE.get());
            output.accept((ItemLike)ModItems.HIGH_SABER_GRINDSTONE.get());
            output.accept((ItemLike)ModItems.SWORD_MOLD.get());
            output.accept((ItemLike)ModItems.SABER_MOLD.get());
            output.accept((ItemLike)ModItems.SPEAR_MOLD.get());
            output.accept((ItemLike)ModItems.WEAPON_MOLD.get());
            output.accept((ItemLike)ModItems.FEIJIAN_YUJIAN.get());
            output.accept(ModItems.QINGTONG_YINHUO_DING_YUJIAN.get());
            output.accept(ModItems.CHI_JIN_SANYANG_DING_YUJIAN.get());
            output.accept((ItemLike)ModItems.DAO_YUJIAN.get());
            output.accept((ItemLike)ModItems.JINGTIE_SPEAR_YUJIAN.get());
            output.accept((ItemLike)ModItems.HEAVY_IRON_BRACER_YUJIAN.get());
            output.accept((ItemLike)ModItems.THROWING_KNIFE_YUJIAN.get());
            output.accept((ItemLike)ModItems.FLYING_NEEDLE_YUJIAN.get());
            output.accept((ItemLike)ModItems.TIEGU_SHAN_YUJIAN.get());
            output.accept(ModItems.BLACK_IRON_JIAN_YUJIAN.get());
            output.accept(ModItems.BLACK_IRON_GREATSWORD_YUJIAN.get());
            output.accept(ModItems.MOON_CRESCENT_HALBERD_YUJIAN.get());
            output.accept(ModItems.SPIRIT_SILK_WHIP_YUJIAN.get());
            output.accept(ModItems.SOUL_HOOK_YUJIAN.get());
            output.accept(ModItems.WIND_FEATHER_FAN_YUJIAN.get());
            output.accept(ModItems.THUNDERWOOD_SWORD_YUJIAN.get());
            output.accept(ModItems.FLAME_PATTERN_DAO_YUJIAN.get());
            output.accept(ModItems.EARTHBREAKER_HAMMER_YUJIAN.get());
            output.accept(ModItems.SEVEN_STAR_SWORD_YUJIAN.get());

            output.accept((ItemLike)ModItems.FUCHEN_YUJIAN.get());
            output.accept((ItemLike)ModItems.WHIP_YUJIAN.get());
            output.accept((ItemLike)ModItems.SHIHUNFAN_YUJIAN.get());
            output.accept((ItemLike)ModItems.ZHENHUN_BELL_YUJIAN.get());
            output.accept((ItemLike)ModItems.YINHUN_GONG_YUJIAN.get());
            output.accept((ItemLike)ModItems.FOG_REVEALING_MIRROR_YUJIAN.get());
            output.accept((ItemLike)ModItems.YINYANG_COMPASS_YUJIAN.get());
            output.accept((ItemLike)ModItems.YUANHUN_BOTTLE_YUJIAN.get());
         }).build());
      IMMORTAL_ACCESSORIES_TAB = CREATIVE_TABS.register("immortal_accessories_tab", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.immortal_cultivation_mod.accessories")).withTabsBefore(new ResourceLocation[]{tabId("immortal_weapons_tab")}).icon(() -> ((Item)ModItems.JADE_PENDANT.get()).getDefaultInstance()).displayItems((parameters, output) -> {
            output.accept((ItemLike)ModItems.QI_POUCH.get());
            output.accept((ItemLike)ModItems.COFFIN.get());
            output.accept((ItemLike)ModItems.FLAG.get());
            output.accept((ItemLike)ModItems.CLOTH_SHEATH.get());
            output.accept((ItemLike)ModItems.SWORD_SHEATH.get());
            output.accept((ItemLike)ModItems.DAO_SHEATH.get());
            output.accept((ItemLike)ModItems.SWORD_CASE.get());
            output.accept((ItemLike)ModItems.XIUJIAN.get());
            output.accept((ItemLike)ModItems.COIN_MASK.get());
            output.accept((ItemLike)ModItems.BLINDFOLD.get());
            output.accept((ItemLike)ModItems.BLINDFOLD2.get());
            output.accept((ItemLike)ModItems.STRAW_HAT.get());
            output.accept((ItemLike)ModItems.NOOB_HAT.get());
            output.accept((ItemLike)ModItems.JADE_PENDANT.get());
            output.accept((ItemLike)ModItems.DAOFU_CHESTPLATE.get());
            output.accept((ItemLike)ModItems.NEIJIA_CHESTPLATE.get());
            output.accept((ItemLike)ModItems.ZHONGYI.get());
            output.accept(ModItems.DOUPENG.get());
            output.accept(ModItems.SHENYI.get());
            output.accept(ModItems.HECHANG.get());
            output.accept(ModItems.YUNJIAN.get());
            output.accept(ModItems.CULTIVATION_FAPAO.get());
            output.accept(ModItems.YUANLING_PAO.get());
            output.accept(ModItems.HUFU.get());
            output.accept(ModItems.MIANSHA.get());
                        output.accept(ModItems.JIANGPA.get());
                        output.accept(ModItems.FUTOU.get());
                        output.accept(ModItems.WUSHA_HAT.get());
                        output.accept(ModItems.SONG_WUSHA_HAT.get());
                        output.accept(ModItems.CHANFU_JIN.get());
                        output.accept(ModItems.EMBROIDERED_FOREHEAD_BAND.get());
                        output.accept(ModItems.VEILED_DOULI.get());
                        output.accept(ModItems.FANGKOU_QIAOJIAN_LV.get());
            output.accept((ItemLike)ModItems.DISGUISE_MASK.get());
            output.accept((ItemLike)ModItems.PROTECTIVE_JADE_PENDANT.get());
            output.accept((ItemLike)ModItems.BLACK_TORTOISE_HEARTGUARD_MIRROR.get());
            output.accept((ItemLike)ModItems.WATER_AVOIDING_PEARL.get());
            output.accept((ItemLike)ModItems.SOUL_ANCHOR_PEARL.get());
            output.accept(ModItems.TRIBULATION_DOLL.get());
            output.accept(ModItems.FORTUNE_MIRROR.get());
            output.accept(ModItems.PRECISE_FORTUNE_MIRROR.get());
            output.accept(ModItems.FORTUNE_GUARD_COIN.get());
            output.accept(com.example.immortal_cultivation_mod.block.ModBlocks.FORTUNE_LAMP.get());
            output.accept(ModBlocks.SEALED_CRADLE_BOTTLE.get());
            output.accept(ModItems.PURIFIED_TRIBULATION_DOLL.get());
            output.accept(ModItems.TRIBULATION_DOLL_YUJIAN.get());
            output.accept(ModItems.FORTUNE_MIRROR_YUJIAN.get());
            output.accept(ModItems.PRECISE_FORTUNE_MIRROR_YUJIAN.get());
            output.accept(ModItems.FORTUNE_GUARD_COIN_YUJIAN.get());
            output.accept(ModItems.FORTUNE_LAMP_YUJIAN.get());
            output.accept(ModItems.PURIFIED_TRIBULATION_DOLL_YUJIAN.get());

            output.accept((ItemLike)ModItems.SHIELD_BREAKER_RING.get());
            output.accept((ItemLike)ModItems.BLOOD_FANG_PENDANT.get());
            output.accept((ItemLike)ModItems.ENLIGHTENMENT_LEAF.get());
            output.accept((ItemLike)ModItems.MYSTERIOUS_RING.get());
            output.accept((ItemLike)ModItems.SYSTEM.get());
            output.accept((ItemLike)ModItems.QI_POUCH_YUJIAN.get());
            output.accept((ItemLike)ModItems.SWORD_CASE_YUJIAN.get());
            output.accept((ItemLike)ModItems.BLOOD_FANG_PENDANT_YUJIAN.get());
            output.accept((ItemLike)ModItems.WATER_AVOIDING_PEARL_YUJIAN.get());
            output.accept((ItemLike)ModItems.JADE_PENDANT_YUJIAN.get());
            output.accept((ItemLike)ModItems.SHIELD_BREAKER_RING_YUJIAN.get());
            output.accept((ItemLike)ModItems.DISGUISE_MASK_YUJIAN.get());
            output.accept((ItemLike)ModItems.BLACK_TORTOISE_HEARTGUARD_MIRROR_YUJIAN.get());
            output.accept((ItemLike)ModItems.WHITE_WOOL_STRIP.get());
            output.accept((ItemLike)ModItems.ORANGE_WOOL_STRIP.get());
            output.accept((ItemLike)ModItems.MAGENTA_WOOL_STRIP.get());
            output.accept((ItemLike)ModItems.LIGHT_BLUE_WOOL_STRIP.get());
            output.accept((ItemLike)ModItems.YELLOW_WOOL_STRIP.get());
            output.accept((ItemLike)ModItems.LIME_WOOL_STRIP.get());
            output.accept((ItemLike)ModItems.PINK_WOOL_STRIP.get());
            output.accept((ItemLike)ModItems.GRAY_WOOL_STRIP.get());
            output.accept((ItemLike)ModItems.LIGHT_GRAY_WOOL_STRIP.get());
            output.accept((ItemLike)ModItems.CYAN_WOOL_STRIP.get());
            output.accept((ItemLike)ModItems.PURPLE_WOOL_STRIP.get());
            output.accept((ItemLike)ModItems.BLUE_WOOL_STRIP.get());
            output.accept((ItemLike)ModItems.BROWN_WOOL_STRIP.get());
            output.accept((ItemLike)ModItems.GREEN_WOOL_STRIP.get());
            output.accept((ItemLike)ModItems.RED_WOOL_STRIP.get());
            output.accept((ItemLike)ModItems.BLACK_WOOL_STRIP.get());
         }).build());
      IMMORTAL_TALISMANS_TAB = CREATIVE_TABS.register("immortal_talismans_tab", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.immortal_cultivation_mod.talismans")).withTabsBefore(new ResourceLocation[]{tabId("immortal_accessories_tab")}).icon(() -> ((Item)ModItems.ZHENSHI_TALISMAN.get()).getDefaultInstance()).displayItems((parameters, output) -> {
            output.accept((ItemLike)ModItems.ORDINARY_YELLOW_TALISMAN_PAPER.get());
            output.accept((ItemLike)ModItems.LINGSANG_TALISMAN_PAPER.get());
            output.accept((ItemLike)ModItems.RED_VEINED_MULBERRY_PAPER.get());
            output.accept((ItemLike)ModItems.JADE_TALISMAN_PAPER.get());
            output.accept((ItemLike)ModItems.CELESTIAL_SILKWORM_TALISMAN_PAPER.get());
            output.accept((ItemLike)ModItems.MUNDANE_CINNABAR_INK.get());
            output.accept((ItemLike)ModItems.SPIRITUAL_CINNABAR_INK.get());
            output.accept((ItemLike)ModItems.MYSTERIOUS_SPIRIT_INK.get());
            output.accept((ItemLike)ModItems.GOLD_VEINED_SPIRIT_INK.get());
            output.accept((ItemLike)ModItems.JADE_MARROW_TALISMAN_INK.get());
            output.accept((ItemLike)ModItems.SPIRIT_MARROW_TREASURE_INK.get());
            output.accept((ItemLike)ModItems.HEAVENLY_SPIRIT_MYSTIC_INK.get());
            output.accept((ItemLike)ModItems.DAO_MARROW_INK.get());

            for(TalismanRecipes.Def recipe : TalismanRecipes.all()) {
               output.accept((ItemLike)recipe.output().get());
            }

            for(TalismanRecipes.Def recipe : TalismanRecipes.all()) {
               output.accept(TalismanFormulaItem.create(recipe.id()));
            }

            output.accept((ItemLike)ModItems.TALISMAN_FORMULA.get());
         }).build());
      IMMORTAL_PILLS_TAB = CREATIVE_TABS.register("immortal_pills_tab", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.immortal_cultivation_mod.pills")).withTabsBefore(new ResourceLocation[]{tabId("immortal_talismans_tab")}).icon(() -> ((Item)ModItems.BREAKTHROUGH_PILL.get()).getDefaultInstance()).displayItems((parameters, output) -> {
            output.accept((ItemLike)ModItems.ENLIGHTENMENT_PILL.get());
            output.accept((ItemLike)ModItems.BREAKTHROUGH_PILL.get());
            output.accept((ItemLike)ModItems.BODY_TEMPERING_PILL.get());
            output.accept((ItemLike)ModItems.BLOOD_QI_PILL.get());
            output.accept((ItemLike)ModItems.BIGU_PILL.get());
            output.accept((ItemLike)ModItems.QI_RECOVERY_PILL.get());
            com.example.immortal_cultivation_mod.alchemy.AlchemyContent.PILLS.values().forEach(pill -> output.accept(pill.get()));
            output.accept(com.example.immortal_cultivation_mod.alchemy.AlchemyContent.BOTTLE.get());
            output.accept(com.example.immortal_cultivation_mod.alchemy.AlchemyContent.BOTTLE_SLIP.get());
            com.example.immortal_cultivation_mod.alchemy.AlchemyContent.FORMULAS.values().forEach(formula -> output.accept(formula.get()));
            com.example.immortal_cultivation_mod.alchemy.AlchemyContent.LEGACY_FORMULAS.values().forEach(formula -> output.accept(formula.get()));
            output.accept((ItemLike)ModBlocks.QINGLING_GRASS.get());
            com.example.immortal_cultivation_mod.farming.SpiritFarming.SEEDS.values().forEach(seed->output.accept(seed.get()));
            output.accept(com.example.immortal_cultivation_mod.farming.SpiritFarming.FARMLAND_ITEM.get());
            output.accept((ItemLike)ModBlocks.CHIYAN_FLOWER.get());
            output.accept((ItemLike)ModBlocks.NINGLU_ZHI.get());
            output.accept((ItemLike)ModBlocks.XUANBING_LOTUS.get());
            output.accept((ItemLike)ModBlocks.ZIYUN_VINE.get());
            output.accept((ItemLike)ModBlocks.JINWEN_FRUIT.get());
            output.accept((ItemLike)ModBlocks.SHIHUN_GRASS.get());
            output.accept((ItemLike)ModBlocks.JIUYE_XIANLAN.get());
            com.example.immortal_cultivation_mod.alchemy.AlchemyContent.EXTRA_PLANTS.values().forEach(plant -> output.accept(plant.get()));
            output.accept((ItemLike)ModBlocks.FUGU_TAI.get());
            output.accept((ItemLike)ModBlocks.HUANXIA_SI.get());
         }).build());
      IMMORTAL_CULTIVATION_TAB = CREATIVE_TABS.register("immortal_cultivation_tab", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.immortal_cultivation_mod.cultivation")).withTabsBefore(new ResourceLocation[]{tabId("immortal_pills_tab")}).icon(() -> ((Item)ModItems.BASIC_BREATHING_METHOD.get()).getDefaultInstance()).displayItems((parameters, output) -> {
            output.accept((ItemLike)ModItems.BASIC_BREATHING_METHOD.get());
            output.accept((ItemLike)ModItems.CLEAR_HEART_METHOD.get());
            output.accept((ItemLike)ModItems.BLOOD_DEMON_JINDAN_METHOD.get());
            output.accept((ItemLike)ModItems.BLOOD_DEMON_YUANYING_METHOD.get());
            output.accept((ItemLike)ModItems.BLOOD_DEMON_HUASHEN_METHOD.get());
            output.accept((ItemLike)ModItems.REINCARNATION_TRUE_ART_METHOD.get());
            output.accept((ItemLike)ModItems.TUNTIAN_DEMON_ART_METHOD.get());
            output.accept((ItemLike)ModItems.POKONG_JUE_METHOD.get());
            output.accept((ItemLike)ModItems.CHANGQING_JUE_METHOD.get());
            output.accept((ItemLike)ModItems.FENTIAN_LIFE_RENEWAL_METHOD.get());
            output.accept((ItemLike)ModItems.HANTI_BINGQIN_METHOD.get());
            output.accept((ItemLike)ModItems.CUIGU_LIANSHEN_METHOD.get());
            output.accept((ItemLike)ModItems.DARI_BATI_METHOD.get());
            output.accept((ItemLike)ModItems.GANGTING_XIAOKONG_METHOD.get());
            output.accept((ItemLike)ModItems.XUANYUE_SUIFENG_METHOD.get());
            output.accept((ItemLike)ModItems.HANLAN_NINGYUAN_METHOD.get());
            output.accept((ItemLike)ModItems.CORPSE_REFINING_SECRET.get());
            acceptSpellScrollsByElement(output);
         }).build());
      IMMORTAL_MAGIC_CIRCLE_TAB = CREATIVE_TABS.register("immortal_magic_circle_tab", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.immortal_cultivation_mod.magic_circle")).withTabsBefore(new ResourceLocation[]{tabId("immortal_cultivation_tab")}).icon(() -> ((Item)com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENPAN.get()).getDefaultInstance()).displayItems((parameters, output) -> {
            output.accept((ItemLike)com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_STONE.get());
            output.accept((ItemLike)com.example.immortal_cultivation_mod.magicritual.item.ModItems.MEDIUM_SPIRIT_STONE.get());
            output.accept((ItemLike)com.example.immortal_cultivation_mod.magicritual.item.ModItems.HIGH_SPIRIT_STONE.get());
            output.accept((ItemLike)com.example.immortal_cultivation_mod.magicritual.item.ModItems.INFINITE_SPIRIT_STONE.get());
            output.accept((ItemLike)com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENPAN.get());
            output.accept((ItemLike)com.example.immortal_cultivation_mod.magicritual.item.ModItems.BLANK_FORMATION_FLAG.get());
            output.accept((ItemLike)com.example.immortal_cultivation_mod.magicritual.item.ModItems.DURATION_FLAG.get());
            output.accept((ItemLike)com.example.immortal_cultivation_mod.magicritual.item.ModItems.AMPLIFIER_FLAG.get());
            output.accept((ItemLike)com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_GATHERING_FLAG.get());
            output.accept((ItemLike)com.example.immortal_cultivation_mod.magicritual.item.ModItems.HASTE_FLAG.get());
            output.accept((ItemLike)com.example.immortal_cultivation_mod.magicritual.item.ModItems.QI_RETURN_FLAG.get());
            output.accept((ItemLike)ModItems.ZHENPAN_YUJIAN.get());
            output.accept((ItemLike)ModItems.BLANK_FORMATION_FLAG_YUJIAN.get());
            output.accept((ItemLike)ModItems.DURATION_FLAG_YUJIAN.get());
            output.accept((ItemLike)ModItems.AMPLIFIER_FLAG_YUJIAN.get());
            output.accept((ItemLike)ModItems.SPIRIT_GATHERING_FLAG_YUJIAN.get());
            output.accept((ItemLike)ModItems.HASTE_FLAG_YUJIAN.get());
            output.accept((ItemLike)ModItems.QI_RETURN_FLAG_YUJIAN.get());
         }).build());
      IMMORTAL_BLOCKS_TAB = CREATIVE_TABS.register("immortal_blocks_tab", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.immortal_cultivation_mod.blocks")).withTabsBefore(new ResourceLocation[]{tabId("immortal_magic_circle_tab")}).icon(() -> ((Block)ModBlocks.QINGLING_GRASS.get()).asItem().getDefaultInstance()).displayItems((parameters, output) -> {
            output.accept((ItemLike)ModBlocks.NUOMI_PLANT.get());
            output.accept(com.example.immortal_cultivation_mod.farming.SpiritFarming.FARMLAND_ITEM.get());
            output.accept((ItemLike)ModBlocks.LINGMU.get());
            output.accept((ItemLike)ModBlocks.LINGMU_LOG.get());
            output.accept((ItemLike)ModBlocks.LINGMU_LEAVES.get());
            output.accept((ItemLike)ModBlocks.QIANNIAN_LINGMU_HEART.get());
            output.accept((ItemLike)ModBlocks.JINGTIE_MOTHERSTONE.get());
            output.accept((ItemLike)ModBlocks.YUNWEN_SILVER_CRYSTAL.get());
            output.accept((ItemLike)ModBlocks.CHITONG_SUI.get());
            output.accept((ItemLike)ModBlocks.HANPO_JADE.get());
            output.accept((ItemLike)ModBlocks.MOXUAN_IRON.get());
            output.accept((ItemLike)ModBlocks.LIUJIN_SAND.get());
            output.accept((ItemLike)ModBlocks.BISUI_STONE.get());
            output.accept((ItemLike)ModBlocks.STAR_DUST_STONE.get());
            output.accept((ItemLike)ModBlocks.VOID_STONE.get());
            output.accept((ItemLike)com.example.immortal_cultivation_mod.magicritual.block.ModBlocks.SPIRIT_ORE.get());
            output.accept((ItemLike)com.example.immortal_cultivation_mod.magicritual.block.ModBlocks.MAGIC_CENTER_BLOCK.get());
            output.accept((ItemLike)com.example.immortal_cultivation_mod.magicritual.block.ModBlocks.MAGIC_CIRCLE_BLOCK.get());
            output.accept((ItemLike)ModBlocks.OAK_PLANK_WALL.get());
            output.accept((ItemLike)ModBlocks.SPRUCE_PLANK_WALL.get());
            output.accept((ItemLike)ModBlocks.BIRCH_PLANK_WALL.get());
            output.accept((ItemLike)ModBlocks.JUNGLE_PLANK_WALL.get());
            output.accept((ItemLike)ModBlocks.ACACIA_PLANK_WALL.get());
            output.accept((ItemLike)ModBlocks.DARK_OAK_PLANK_WALL.get());
            output.accept((ItemLike)ModBlocks.MANGROVE_PLANK_WALL.get());
            output.accept((ItemLike)ModBlocks.CHERRY_PLANK_WALL.get());
            output.accept((ItemLike)ModBlocks.BAMBOO_PLANK_WALL.get());
            output.accept((ItemLike)ModBlocks.CRIMSON_PLANK_WALL.get());
            output.accept((ItemLike)ModBlocks.WARPED_PLANK_WALL.get());
            output.accept((ItemLike)ModBlocks.CU_TIE_XIAODING.get());
            output.accept((ItemLike)ModBlocks.QINGTONG_YINHUO_DING.get());
            output.accept(ModBlocks.CHI_JIN_SANYANG_DING.get());
            output.accept(ModItems.FLAME_VESSEL.get());
            for (var flame : com.example.immortal_cultivation_mod.flame.FlameKind.values()) {
                if (!flame.special()) continue;
                var vessel = new net.minecraft.world.item.ItemStack(ModItems.FLAME_VESSEL.get());
                com.example.immortal_cultivation_mod.flame.FlameVesselItem.fill(vessel, flame, java.util.UUID.randomUUID());
                output.accept(vessel);
            }
            output.accept(ModItems.CRIMSON_FLAME_SEED.get());
            output.accept(ModItems.EARTH_FLAME_SEED.get());
            output.accept(ModItems.WOOD_FLAME_HEART.get());
            output.accept(ModItems.SOLAR_FLAME_SEED.get());
            output.accept(ModItems.NETHER_FLAME_SEED.get());
            output.accept(ModItems.THUNDER_FLAME_SEED.get());
            output.accept(ModItems.DRAGON_FLAME_SEED.get());
            output.accept(ModItems.HEAVENLY_FLAME_SEED.get());
            output.accept(ModItems.DRAGON_FLAME_SEED_YUJIAN.get());
            output.accept(ModItems.CRIMSON_FLAME_SEED_YUJIAN.get());
            output.accept((ItemLike)ModBlocks.RESTRICTION_TABLE.get());
            output.accept((ItemLike)ModBlocks.TALISMAN_DRAWING_TABLE.get());
            output.accept((ItemLike)ModBlocks.MEDICINE_CABINET_SMALL.get());
            output.accept((ItemLike)ModBlocks.MEDICINE_CABINET_LARGE.get());
            output.accept((ItemLike)ModBlocks.DUNWU_STELE.get());
            output.accept((ItemLike)ModBlocks.LIANQI_SPELL_SCROLL_BLOCK.get());
            output.accept((ItemLike)ModBlocks.ZHUJI_SPELL_SCROLL_BLOCK.get());
            output.accept((ItemLike)ModBlocks.JINDAN_SPELL_SCROLL_BLOCK.get());
            output.accept((ItemLike)ModBlocks.YUANYING_SPELL_SCROLL_BLOCK.get());
            output.accept((ItemLike)ModBlocks.HUASHEN_SPELL_SCROLL_BLOCK.get());
            output.accept((ItemLike)ModBlocks.LIANXU_SPELL_SCROLL_BLOCK.get());
            output.accept((ItemLike)ModBlocks.HETI_SPELL_SCROLL_BLOCK.get());
            output.accept((ItemLike)ModBlocks.DACHENG_SPELL_SCROLL_BLOCK.get());
            output.accept((ItemLike)ModBlocks.DUJIE_SPELL_SCROLL_BLOCK.get());
            output.accept((ItemLike)ModBlocks.DEAD_SKELETON.get());
         }).build());
      IMMORTAL_SPAWN_EGGS_TAB = CREATIVE_TABS.register("immortal_spawn_eggs_tab", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.immortal_cultivation_mod.spawn_eggs")).withTabsBefore(new ResourceLocation[]{tabId("immortal_blocks_tab")}).icon(() -> ((Item)ModItems.LIANQI_CULTIVATOR_SPAWN_EGG.get()).getDefaultInstance()).displayItems((parameters, output) -> {
            output.accept((ItemLike)ModItems.LIANQI_CULTIVATOR_SPAWN_EGG.get());
            output.accept((ItemLike)ModItems.ZHUJI_CULTIVATOR_SPAWN_EGG.get());
            output.accept((ItemLike)ModItems.JINDAN_CULTIVATOR_SPAWN_EGG.get());
            output.accept((ItemLike)ModItems.FANREN_NPC_SPAWN_EGG.get());
            output.accept((ItemLike)ModItems.HOTEL_NPC_SPAWN_EGG.get());
            output.accept((ItemLike)ModItems.RECIPE_MERCHANT_SPAWN_EGG.get());
            output.accept((ItemLike)ModItems.SPELL_MERCHANT_SPAWN_EGG.get());
            output.accept((ItemLike)ModItems.CRIMSON_SCALE_FLAME_LIZARD_SPAWN_EGG.get());
            output.accept((ItemLike)ModItems.CRIMSON_SKY_FLAME_FLOOD_DRAGON_SPAWN_EGG.get());
            output.accept((ItemLike)ModItems.BLOOD_APE_DEMON_GENERAL_SPAWN_EGG.get());
            output.accept(ModItems.XUANJIN_SEAL.get());
            output.accept(ModItems.XUANJIN_ZHENG_SPAWN_EGG.get());
            output.accept((ItemLike)ModItems.VAULT_GUARDIAN_SPAWN_EGG.get());
            output.accept((ItemLike)ModItems.AZURE_SPIRIT_SILKWORM_SPAWN_EGG.get());
            output.accept((ItemLike)ModItems.KONGMING_BEETLE_SPAWN_EGG.get());
            output.accept((ItemLike)ModItems.THUNDERHORN_ROCK_BADGER_SPAWN_EGG.get());
            output.accept((ItemLike)ModItems.FROSTSPINE_CAVE_BEAR_SPAWN_EGG.get());
            output.accept((ItemLike)ModItems.SILVER_MIST_WOLF_SPAWN_EGG.get());
            output.accept((ItemLike)ModItems.GREEN_PATTERNED_SPIRIT_DEVOURING_RAT_SPAWN_EGG.get());
            output.accept((ItemLike)ModItems.RED_BACKED_IRON_SCALE_SCORPION_SPAWN_EGG.get());
            output.accept((ItemLike)ModItems.WIND_FEATHER_CLIFF_DEER_SPAWN_EGG.get());
            output.accept((ItemLike)ModItems.INK_ARMORED_STONE_DEVOURING_TORTOISE_SPAWN_EGG.get());
            output.accept((ItemLike)ModItems.EMBER_FEATHER_FIRE_OWL_SPAWN_EGG.get());
            output.accept((ItemLike)ModItems.PHANTOM_VEIL_FOX_SPAWN_EGG.get());
         }).build());
   }

   private static record SpellScrollEntry(String spellId, ItemLike item) {
   }
}
