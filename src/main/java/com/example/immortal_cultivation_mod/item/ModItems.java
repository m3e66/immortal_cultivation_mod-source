package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.FireballTalismanProjectileEntity;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ImmortalCultivationMod.MODID);
    static {
        for (String old : java.util.List.of("zhuji_vault_guardian_spawn_egg", "jindan_vault_guardian_spawn_egg")) {
            ITEMS.addAlias(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, old),
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "vault_guardian_spawn_egg"));
        }
        for (String old : java.util.List.of("zhuji_phantom_veil_fox_spawn_egg", "jindan_phantom_veil_fox_spawn_egg")) {
            ITEMS.addAlias(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, old),
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "phantom_veil_fox_spawn_egg"));
        }
    }

    private static final Map<String, DeferredItem<Item>> TALISMAN_FORMULAS = new LinkedHashMap<>();

    public static final DeferredItem<Item> RECIPE_MERCHANT_SPAWN_EGG = registerItem("recipe_merchant_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(
                    com.example.immortal_cultivation_mod.entity.ModEntities.RECIPE_MERCHANT,
                    0x52334A, 0xD7AB53, new Item.Properties()));

    public static final DeferredItem<Item> SPELL_MERCHANT_SPAWN_EGG = registerItem("spell_merchant_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(
                    com.example.immortal_cultivation_mod.entity.ModEntities.SPELL_MERCHANT,
                    0x543876, 0xB7E4CD, new Item.Properties()));

    public static final DeferredItem<Item> FIREBALL_SCROLL = registerItem("fireball_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.FIREBALL));

    public static final DeferredItem<Item> LINGBENG_SCROLL = registerItem("lingbeng_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.LINGBENG));

    public static final DeferredItem<Item> REGENERATION_SCROLL = registerItem("regeneration_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.REGENERATION));

    public static final DeferredItem<Item> BEAM_SCROLL = registerItem("beam_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.BEAM));

    public static final DeferredItem<Item> EARTH_ESCAPE_SCROLL = registerItem("earth_escape_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.EARTH_ESCAPE));

    public static final DeferredItem<Item> CLEANSE_SCROLL = registerItem("cleanse_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.CLEANSE));

    public static final DeferredItem<Item> QI_GATHERING_SCROLL = registerItem("qi_gathering_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.QI_GATHERING));

    public static final DeferredItem<Item> IGNITE_FLARE_SCROLL = registerItem("ignite_flare_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.IGNITE_FLARE));

    public static final DeferredItem<Item> SPIRIT_SIGHT_SCROLL = registerItem("spirit_sight_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.SPIRIT_SIGHT));

    public static final DeferredItem<Item> ZHENSHAN_PALM_SCROLL = registerItem("zhenshan_palm_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.ZHENSHAN_PALM));

    public static final DeferredItem<Item> LIGHT_BEAM_ATTACK_SCROLL = registerItem("light_beam_attack_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.LIGHT_BEAM_ATTACK));

    public static final DeferredItem<Item> DIELANG_SHIELD_SCROLL = registerItem("dielang_shield_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.DIELANG_SHIELD));

    public static final DeferredItem<Item> LINGZHI_BULLET_SCROLL = registerItem("lingzhi_bullet_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.LINGZHI_BULLET));

    public static final DeferredItem<Item> JINMANG_CHUANSHA_SCROLL = registerItem("jinmang_chuansha_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.JINMANG_CHUANSHA));

    public static final DeferredItem<Item> JINFENG_ZHI_SCROLL = registerItem("jinfeng_zhi_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.JINFENG_ZHI));
    public static final DeferredItem<Item> WANREN_CHUANXIN_SCROLL = registerItem("wanren_chuanxin_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.WANREN_CHUANXIN));
    public static final DeferredItem<Item> JINGJI_TUSHENG_SCROLL = registerItem("jingji_tusheng_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.JINGJI_TUSHENG));
    public static final DeferredItem<Item> WANTENG_JIAOSHA_SCROLL = registerItem("wanteng_jiaosha_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.WANTENG_JIAOSHA));
    public static final DeferredItem<Item> YANBAO_DAN_SCROLL = registerItem("yanbao_dan_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.YANBAO_DAN));
    public static final DeferredItem<Item> CHIYAN_LONGJUAN_SCROLL = registerItem("chiyan_longjuan_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.CHIYAN_LONGJUAN));
    public static final DeferredItem<Item> TIANYUN_FENCHENG_SCROLL = registerItem("tianyun_fencheng_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.TIANYUN_FENCHENG));
    public static final DeferredItem<Item> LUOHUA_CHANSHEN_SCROLL = registerItem("luohua_chanshen_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.LUOHUA_CHANSHEN));
    public static final DeferredItem<Item> BAIHUA_ZANG_SCROLL = registerItem("baihua_zang_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.BAIHUA_ZANG));
    public static final DeferredItem<Item> POKONG_JIANHEN_SCROLL = registerItem("pokong_jianhen_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.POKONG_JIANHEN));
    public static final DeferredItem<Item> SANJIAN_ZHUIMING_SCROLL = registerItem("sanjian_zhuiming_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.SANJIAN_ZHUIMING));
    public static final DeferredItem<Item> DIYAN_QIANG_SCROLL = registerItem("diyan_qiang_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.DIYAN_QIANG));
    public static final DeferredItem<Item> SHANBENG_YIN_SCROLL = registerItem("shanbeng_yin_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.SHANBENG_YIN));
    public static final DeferredItem<Item> SHUIREN_LIANZHAN_SCROLL = registerItem("shuiren_lianzhan_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.SHUIREN_LIANZHAN));
    public static final DeferredItem<Item> CANGLANG_CHONG_SCROLL = registerItem("canglang_chong_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.CANGLANG_CHONG));
    public static final DeferredItem<Item> LEISHAN_SCROLL = registerItem("leishan_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.LEISHAN));
    public static final DeferredItem<Item> WULEI_HONGDING_SCROLL = registerItem("wulei_hongding_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.WULEI_HONGDING));
    public static final DeferredItem<Item> BINGZHUI_YU_SCROLL = registerItem("bingzhui_yu_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.BINGZHUI_YU));
    public static final DeferredItem<Item> HANYU_FENGDI_SCROLL = registerItem("hanyu_fengdi_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.HANYU_FENGDI));
    public static final DeferredItem<Item> YAOGUANG_SHAN_SCROLL = registerItem("yaoguang_shan_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.YAOGUANG_SHAN));
    public static final DeferredItem<Item> TIANGUANG_GUANSHA_SCROLL = registerItem("tianguang_guansha_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.TIANGUANG_GUANSHA));
    public static final DeferredItem<Item> YINGXI_SCROLL = registerItem("yingxi_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.YINGXI));
    public static final DeferredItem<Item> TIANFENG_ZHUI_SCROLL = registerItem("tianfeng_zhui_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.TIANFENG_ZHUI));
    public static final DeferredItem<Item> MULING_SHENGXI_SCROLL = registerItem("muling_shengxi_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.MULING_SHENGXI));
    public static final DeferredItem<Item> XUE_GUIYUAN_SHU_SCROLL = registerItem("xue_guiyuan_shu_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.XUE_GUIYUAN_SHU));
    public static final DeferredItem<Item> SHENGXI_BAOFA_SCROLL = registerItem("shengxi_baofa_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.SHENGXI_BAOFA));

    // Renderer-only items. They are intentionally absent from creative tabs and the System shop.
    public static final DeferredItem<Item> SPELL_MODEL_METAL_NEEDLE = registerItem("spell_model_metal_needle",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SPELL_MODEL_METAL_BLADE = registerItem("spell_model_metal_blade",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SPELL_MODEL_THORNS = registerItem("spell_model_thorns",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SPELL_MODEL_VINES = registerItem("spell_model_vines",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SPELL_MODEL_EARTH_SPEAR = registerItem("spell_model_earth_spear",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SPELL_MODEL_MOUNTAIN_SEAL = registerItem("spell_model_mountain_seal",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SPELL_MODEL_MOUNTAIN_PILLAR = registerItem("spell_model_mountain_pillar",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SPELL_MODEL_MOUNTAIN_CLIFF = registerItem("spell_model_mountain_cliff",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SPELL_MODEL_MOUNTAIN_SPIRE = registerItem("spell_model_mountain_spire",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SPELL_MODEL_METAL_SPEAR = registerItem("spell_model_metal_spear",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SPELL_MODEL_METAL_BROAD_BLADE = registerItem("spell_model_metal_broad_blade",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SPELL_MODEL_METAL_HOOK_BLADE = registerItem("spell_model_metal_hook_blade",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SPELL_MODEL_WANFENG_METAL_BLOOM = registerItem("spell_model_wanfeng_metal_bloom",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SPELL_MODEL_WATER_CRESCENT = registerItem("spell_model_water_crescent",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SPELL_MODEL_WAVE = registerItem("spell_model_wave",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SPELL_MODEL_ICE_SPIKE = registerItem("spell_model_ice_spike",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SPELL_MODEL_XUANYAN_PLATE = registerItem("spell_model_xuanyan_plate",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SPELL_MODEL_SOLAR_ORB = registerItem("spell_model_solar_orb",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SPELL_MODEL_SOLAR_HALO = registerItem("spell_model_solar_halo",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> WIND_BLADE_SCROLL = registerItem("wind_blade_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.WIND_BLADE));

    public static final DeferredItem<Item> WIND_STEP_SCROLL = registerItem("wind_step_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.WIND_STEP));

    public static final DeferredItem<Item> YUFENG_JUE_SCROLL = registerItem("yufeng_jue_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.YUFENG_JUE));

    public static final DeferredItem<Item> SMOKE_ART_SCROLL = registerItem("smoke_art_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.SMOKE_ART));

    public static final DeferredItem<Item> HUTI_QI_SCROLL = registerItem("huti_qi_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.HUTI_QI));

    public static final DeferredItem<Item> MICHEN_ZHANG_SCROLL = registerItem("michen_zhang_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.MICHEN_ZHANG));

    public static final DeferredItem<Item> SLIDING_WATER_SCROLL = registerItem("sliding_water_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.SLIDING_WATER));


    public static final DeferredItem<Item> DINGSHEN_SCROLL = registerItem("dingshen_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.DINGSHEN));

    public static final DeferredItem<Item> YINLEI_JUE_SCROLL = registerItem("yinlei_jue_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.YINLEI_JUE));

    public static final DeferredItem<Item> WULEI_ZHENGFA_SCROLL = registerItem("wulei_zhengfa_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.WULEI_ZHENGFA));

    public static final DeferredItem<Item> LIUGUANG_JIANYING_SCROLL = registerItem("liuguang_jianying_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.LIUGUANG_JIANYING));

    public static final DeferredItem<Item> SIFANG_JIE_SCROLL = registerItem("sifang_jie_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.SIFANG_JIE));

    public static final DeferredItem<Item> GUSHI_SHIELD_SCROLL = registerItem("gushi_shield_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.GUSHI_SHIELD));

    public static final DeferredItem<Item> XUANYAN_HUTI_SCROLL = registerItem("xuanyan_huti_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.XUANYAN_HUTI));

    public static final DeferredItem<Item> LIUFENG_XIEJIN_SCROLL = registerItem("liufeng_xiejin_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.LIUFENG_XIEJIN));

    public static final DeferredItem<Item> XUESHA_HUSHEN_SCROLL = registerItem("xuesha_hushen_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.XUESHA_HUSHEN));

    public static final DeferredItem<Item> JINGLING_FANZHAO_SCROLL = registerItem("jingling_fanzhao_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.JINGLING_FANZHAO));

    public static final DeferredItem<Item> YUANGANG_HUJIE_SCROLL = registerItem("yuangang_hujie_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.YUANGANG_HUJIE));

    public static final DeferredItem<Item> KONGSHI_SHU_SCROLL = registerItem("kongshi_shu_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.KONGSHI_SHU));

    public static final DeferredItem<Item> XUYING_TA_SCROLL = registerItem("xuying_ta_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.XUYING_TA));

    public static final DeferredItem<Item> DUANLIU_KONGDUN_SCROLL = registerItem("duanliu_kongdun_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.DUANLIU_KONGDUN));

    public static final DeferredItem<Item> YIHEN_CI_SCROLL = registerItem("yihen_ci_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.YIHEN_CI));

    public static final DeferredItem<Item> JINHUO_XINLIAO_SCROLL = registerItem("jinhuo_xinliao_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.JINHUO_XINLIAO));

    public static final DeferredItem<Item> SUIGUI_XINGYUN_SCROLL = registerItem("suigui_xingyun_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.SUIGUI_XINGYUN));

    public static final DeferredItem<Item> SHIXU_CHILIU_SCROLL = registerItem("shixu_chiliu_scroll",
            () -> new SpellScrollItem(new Item.Properties().stacksTo(1), ModSpells.SHIXU_CHILIU));

    public static final DeferredItem<Item> LIANQI_CULTIVATOR_SPAWN_EGG = registerItem("lianqi_cultivator_spawn_egg",
            () -> new SpawnEggItem(com.example.immortal_cultivation_mod.entity.ModEntities.LIANQI_CULTIVATOR.get(), 0x355B47, 0x9EDB8F, new Item.Properties()));

    public static final DeferredItem<Item> ZHUJI_CULTIVATOR_SPAWN_EGG = registerItem("zhuji_cultivator_spawn_egg",
            () -> new SpawnEggItem(com.example.immortal_cultivation_mod.entity.ModEntities.ZHUJI_CULTIVATOR.get(), 0x3E2D45, 0x74B6FF, new Item.Properties()));

    public static final DeferredItem<Item> JINDAN_CULTIVATOR_SPAWN_EGG = registerItem("jindan_cultivator_spawn_egg",
            () -> new SpawnEggItem(com.example.immortal_cultivation_mod.entity.ModEntities.JINDAN_CULTIVATOR.get(), 0x4C3924, 0xF6C64A, new Item.Properties()));


    public static final DeferredItem<Item> FANREN_NPC_SPAWN_EGG = registerItem("fanren_npc_spawn_egg",
            () -> new SpawnEggItem(com.example.immortal_cultivation_mod.entity.ModEntities.FANREN_NPC.get(), 0x8B6B4A, 0xD8C59C, new Item.Properties()));

    public static final DeferredItem<Item> HOTEL_NPC_SPAWN_EGG = registerItem("hotel_npc_spawn_egg",
            () -> new SpawnEggItem(com.example.immortal_cultivation_mod.entity.ModEntities.HOTEL_NPC.get(),
                    0x573521, 0xD6A85F, new Item.Properties()));

    public static final DeferredItem<Item> CRIMSON_SCALE_FLAME_LIZARD_SPAWN_EGG =
            registerItem("crimson_scale_flame_lizard_spawn_egg",
                    () -> new BeastSpawnEggItem(
                            com.example.immortal_cultivation_mod.entity.ModEntities.CRIMSON_SCALE_FLAME_LIZARD.get(),
                            0x8A1E18, 0xFF8C24, new Item.Properties()));

    public static final DeferredItem<Item> CRIMSON_SKY_FLAME_FLOOD_DRAGON_SPAWN_EGG =
            registerItem("crimson_sky_flame_flood_dragon_spawn_egg",
                    () -> new BeastSpawnEggItem(
                            com.example.immortal_cultivation_mod.entity.ModEntities
                                    .CRIMSON_SKY_FLAME_FLOOD_DRAGON.get(),
                            0x3A0704, 0xFF5A0A, new Item.Properties()));

    public static final DeferredItem<Item> XUANJIN_SEAL = registerItem("xuanjin_seal",
            () -> new XuanjinSealItem(new Item.Properties()));
    public static final DeferredItem<Item> XUANJIN_ZHENG_SPAWN_EGG = registerItem("xuanjin_zheng_spawn_egg",
            () -> new SpawnEggItem(com.example.immortal_cultivation_mod.entity.ModEntities.XUANJIN_ZHENG.get(),
                    0x191B21, 0xED6328, new Item.Properties()));

    public static final DeferredItem<Item> BLOOD_APE_DEMON_GENERAL_SPAWN_EGG =
            registerItem("blood_ape_demon_general_spawn_egg",
                    () -> new BeastSpawnEggItem(
                            com.example.immortal_cultivation_mod.entity.ModEntities
                                    .BLOOD_APE_DEMON_GENERAL.get(),
                            0x35100D, 0xB83228, new Item.Properties()));

    public static final DeferredItem<Item> VAULT_GUARDIAN_SPAWN_EGG =
            registerItem("vault_guardian_spawn_egg",
                    () -> new BeastSpawnEggItem(
                            com.example.immortal_cultivation_mod.entity.ModEntities.VAULT_GUARDIAN.get(),
                            0x111315, 0xFF8A24, new Item.Properties()));

    public static final DeferredItem<Item> AZURE_SPIRIT_SILKWORM_SPAWN_EGG =
            registerItem("azure_spirit_silkworm_spawn_egg",
                    () -> new BeastSpawnEggItem(
                            com.example.immortal_cultivation_mod.entity.ModEntities.AZURE_SPIRIT_SILKWORM.get(),
                            0x66DDE0, 0xE8FFFF, new Item.Properties()));

    public static final DeferredItem<Item> KONGMING_BEETLE_SPAWN_EGG =
            registerItem("kongming_beetle_spawn_egg",
                    () -> new BeastSpawnEggItem(
                            com.example.immortal_cultivation_mod.entity.ModEntities.KONGMING_BEETLE.get(),
                            0x252633, 0x7654D8, new Item.Properties()));

    public static final DeferredItem<Item> THUNDERHORN_ROCK_BADGER_SPAWN_EGG =
            registerItem("thunderhorn_rock_badger_spawn_egg",
                    () -> new BeastSpawnEggItem(
                            com.example.immortal_cultivation_mod.entity.ModEntities
                                    .THUNDERHORN_ROCK_BADGER.get(),
                            0x242421, 0xE2A53E, new Item.Properties()));

    public static final DeferredItem<Item> FROSTSPINE_CAVE_BEAR_SPAWN_EGG =
            registerItem("frostspine_cave_bear_spawn_egg",
                    () -> new BeastSpawnEggItem(
                            com.example.immortal_cultivation_mod.entity.ModEntities
                                    .FROSTSPINE_CAVE_BEAR.get(),
                            0x1C2631, 0x8FDFFF, new Item.Properties()));

    public static final DeferredItem<Item> GREEN_PATTERNED_SPIRIT_DEVOURING_RAT_SPAWN_EGG =
            registerItem("green_patterned_spirit_devouring_rat_spawn_egg",
                    () -> new BeastSpawnEggItem(com.example.immortal_cultivation_mod.entity.ModEntities
                            .GREEN_PATTERNED_SPIRIT_DEVOURING_RAT.get(),
                            0x171A18, 0x55D83E, new Item.Properties()));

    public static final DeferredItem<Item> RED_BACKED_IRON_SCALE_SCORPION_SPAWN_EGG =
            registerItem("red_backed_iron_scale_scorpion_spawn_egg",
                    () -> new BeastSpawnEggItem(com.example.immortal_cultivation_mod.entity.ModEntities
                            .RED_BACKED_IRON_SCALE_SCORPION.get(),
                            0x161616, 0xE13A1C, new Item.Properties()));

    public static final DeferredItem<Item> WIND_FEATHER_CLIFF_DEER_SPAWN_EGG =
            registerItem("wind_feather_cliff_deer_spawn_egg",
                    () -> new BeastSpawnEggItem(com.example.immortal_cultivation_mod.entity.ModEntities
                            .WIND_FEATHER_CLIFF_DEER.get(),
                            0xC8C8BE, 0x56C6C3, new Item.Properties()));

    public static final DeferredItem<Item> INK_ARMORED_STONE_DEVOURING_TORTOISE_SPAWN_EGG =
            registerItem("ink_armored_stone_devouring_tortoise_spawn_egg",
                    () -> new BeastSpawnEggItem(com.example.immortal_cultivation_mod.entity.ModEntities
                            .INK_ARMORED_STONE_DEVOURING_TORTOISE.get(),
                            0x1B1B1B, 0xB38A3F, new Item.Properties()));

    public static final DeferredItem<Item> EMBER_FEATHER_FIRE_OWL_SPAWN_EGG =
            registerItem("ember_feather_fire_owl_spawn_egg",
                    () -> new BeastSpawnEggItem(com.example.immortal_cultivation_mod.entity.ModEntities
                            .EMBER_FEATHER_FIRE_OWL.get(),
                            0x171414, 0xFF5A16, new Item.Properties()));
    public static final DeferredItem<Item> PHANTOM_VEIL_FOX_SPAWN_EGG =
            registerItem("phantom_veil_fox_spawn_egg",
                    () -> new BeastSpawnEggItem(
                            com.example.immortal_cultivation_mod.entity.ModEntities
                                    .PHANTOM_VEIL_FOX.get(),
                            0xAAA8B5, 0x995CD3, new Item.Properties()));

    public static final DeferredItem<Item> ENLIGHTENMENT_PILL = registerItem("enlightenment_pill",
            () -> new EnlightenmentPillItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> BREAKTHROUGH_PILL = registerItem("breakthrough_pill",
            () -> new com.example.immortal_cultivation_mod.alchemy.LegacyPillItem(new Item.Properties().stacksTo(16)));



    public static final DeferredItem<Item> BODY_TEMPERING_PILL = registerItem("body_tempering_pill",
            () -> new BodyCultivationItems.BodyTemperingPillItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> BLOOD_QI_PILL = registerItem("blood_qi_pill",
            () -> new BodyCultivationItems.BloodQiPillItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> QI_POUCH = registerItem("qi_pouch",
            () -> new QiPouchItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> DEBUG_STAT_EDITOR = registerItem("debug_stat_editor",
            () -> new DebugStatEditorItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> DEBUG_TIME_CONTROLLER = registerItem("debug_time_controller",
            () -> new DebugTimeControllerItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> TRAINING_DUMMY = registerItem("training_dummy",
            () -> new TrainingDummyItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> DUMMY_CONTROL_STICK = registerItem("dummy_control_stick",
            () -> new DummyControlStickItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> RESTRICTION_INSIGHT = registerItem("restriction_insight",
            () -> new RestrictionInsightItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> FOG_REVEALING_MIRROR = registerItem("fog_revealing_mirror",
            () -> new FogRevealingMirrorItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> YINYANG_COMPASS = registerItem("yinyang_compass",
            () -> new YinYangCompassItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> ZHENSHI_TALISMAN = registerItem("zhenshi_talisman",
            () -> new TalismanItem(new Item.Properties().stacksTo(16),
                    TalismanItem.Tier.TWO));

    public static final DeferredItem<Item> ZHENSHI_SEAL_HELMET = registerItem("zhenshi_seal_helmet",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> ZHENHUN_BELL = registerItem("zhenhun_bell",
            () -> new ZhenhunBellItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> YINHUN_GONG = registerItem("yinhun_gong",
            () -> new YinhunGongItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> FENGYAN_TALISMAN = registerItem("fengyan_talisman",
            () -> new TalismanItem(new Item.Properties().stacksTo(16),
                    TalismanItem.Tier.TWO));

    public static final DeferredItem<Item> FIREBALL_TALISMAN = registerItem("fireball_talisman",
            () -> new AttackTalismanItem(new Item.Properties().stacksTo(16),
                    FireballTalismanProjectileEntity.AttackType.FIREBALL,
                    TalismanItem.Tier.ONE));

    // Beginner talismans.
    public static final DeferredItem<Item> QINGCHEN_TALISMAN = utilityTalisman(
            "qingchen_talisman", UtilityTalismanItem.Kind.CLEAN_DUST,
            TalismanItem.Tier.ONE);
    public static final DeferredItem<Item> WEIGUANG_TALISMAN = utilityTalisman(
            "weiguang_talisman", UtilityTalismanItem.Kind.DIM_LIGHT,
            TalismanItem.Tier.ONE);
    public static final DeferredItem<Item> LIANSHENG_TALISMAN = utilityTalisman(
            "liansheng_talisman", UtilityTalismanItem.Kind.MUFFLE_SOUND,
            TalismanItem.Tier.ONE);
    // Intermediate talismans.
    public static final DeferredItem<Item> BOJIN_HUFU = utilityTalisman(
            "bojin_hufu", UtilityTalismanItem.Kind.THIN_GOLD_SHIELD,
            TalismanItem.Tier.TWO);
    public static final DeferredItem<Item> BIXIE_ANHUN_TALISMAN = utilityTalisman(
            "bixie_anhun_talisman", UtilityTalismanItem.Kind.CALM_SOUL,
            TalismanItem.Tier.THREE);
    public static final DeferredItem<Item> XINGHUO_TALISMAN = attackTalisman(
            "xinghuo_talisman", FireballTalismanProjectileEntity.AttackType.SPARK,
            TalismanItem.Tier.TWO);
    public static final DeferredItem<Item> FENGREN_TALISMAN = attackTalisman(
            "fengren_talisman", FireballTalismanProjectileEntity.AttackType.WIND_BLADE,
            TalismanItem.Tier.ONE);
    public static final DeferredItem<Item> CHANSI_KUN_TALISMAN = attackTalisman(
            "chansi_kun_talisman", FireballTalismanProjectileEntity.AttackType.BINDING_SILK,
            TalismanItem.Tier.TWO);
    public static final DeferredItem<Item> DUANTU_SHENXING_TALISMAN = utilityTalisman(
            "duantu_shenxing_talisman", UtilityTalismanItem.Kind.SHORT_DIVINE_TRAVEL,
            TalismanItem.Tier.TWO);
    public static final DeferredItem<Item> XIUFU_TALISMAN = utilityTalisman(
            "xiufu_talisman", UtilityTalismanItem.Kind.REPAIR_ITEM,
            TalismanItem.Tier.TWO);
    public static final DeferredItem<Item> CHUANYIN_TALISMAN = utilityTalisman(
            "chuanyin_talisman", UtilityTalismanItem.Kind.VOICE_MESSAGE,
            TalismanItem.Tier.TWO);
    public static final DeferredItem<Item> ZHENRAO_TALISMAN = utilityTalisman(
            "zhenrao_talisman", UtilityTalismanItem.Kind.STABILIZE_DISTURBANCE,
            TalismanItem.Tier.THREE);
    public static final DeferredItem<Item> BIAOJI_TALISMAN = utilityTalisman(
            "biaoji_talisman", UtilityTalismanItem.Kind.MARK_ENTITY,
            TalismanItem.Tier.THREE);
    public static final DeferredItem<Item> DUNKONG_TALISMAN = utilityTalisman(
            "dunkong_talisman", UtilityTalismanItem.Kind.VOID_ESCAPE,
            TalismanItem.Tier.THREE);
    public static final DeferredItem<Item> HUABING_TALISMAN = utilityTalisman(
            "huabing_talisman", UtilityTalismanItem.Kind.MANIFEST_WEAPON,
            TalismanItem.Tier.THREE);

    public static final DeferredItem<Item> BAOFENG_TALISMAN = attackTalisman(
            "baofeng_talisman", FireballTalismanProjectileEntity.AttackType.BLAST_WIND,
            TalismanItem.Tier.THREE);
    public static final DeferredItem<Item> FENGLING_TALISMAN = attackTalisman(
            "fengling_talisman", FireballTalismanProjectileEntity.AttackType.SPIRIT_SEAL,
            TalismanItem.Tier.FOUR);
    public static final DeferredItem<Item> JINKONG_TALISMAN = registerItem(
            "jinkong_talisman", () -> new ForbiddenAirTalismanItem(
                    new Item.Properties().stacksTo(16)));

    // Advanced talismans.
    public static final DeferredItem<Item> JINGUANG_HUTI_TALISMAN = utilityTalisman(
            "jinguang_huti_talisman", UtilityTalismanItem.Kind.GOLDEN_BODY_SHIELD,
            TalismanItem.Tier.FOUR);
    public static final DeferredItem<Item> WUYUE_BIE_TALISMAN = utilityTalisman(
            "wuyue_bie_talisman", UtilityTalismanItem.Kind.FIVE_MOUNTAINS_WARD,
            TalismanItem.Tier.SIX);
    public static final DeferredItem<Item> WULEI_XIAOFA_TALISMAN = attackTalisman(
            "wulei_xiaofa_talisman", FireballTalismanProjectileEntity.AttackType.FIVE_THUNDER,
            TalismanItem.Tier.THREE);
    public static final DeferredItem<Item> HUOYU_FENXIE_TALISMAN = attackTalisman(
            "huoyu_fenxie_talisman", FireballTalismanProjectileEntity.AttackType.FIRE_PRISON,
            TalismanItem.Tier.THREE);
    public static final DeferredItem<Item> QIANLI_CHUANXUN_TALISMAN = utilityTalisman(
            "qianli_chuanxun_talisman", UtilityTalismanItem.Kind.THOUSAND_LI_MESSAGE,
            TalismanItem.Tier.FIVE);
    public static final DeferredItem<Item> POZHANG_KAISHAN_TALISMAN = attackTalisman(
            "pozhang_kaishan_talisman", FireballTalismanProjectileEntity.AttackType.BARRIER_BREAKER,
            TalismanItem.Tier.FOUR);
    public static final DeferredItem<Item> XUANYAN_FENJIE_TALISMAN = attackTalisman(
            "xuanyan_fenjie_talisman", FireballTalismanProjectileEntity.AttackType.BLACK_FLAME,
            TalismanItem.Tier.SIX);

    /** Internal, ephemeral output of 化兵符; intentionally omitted from creative tabs. */
    public static final DeferredItem<Item> MANIFESTED_TALISMAN_SWORD = registerItem(
            "manifested_talisman_sword",
            () -> new ManifestedWeaponItem(new Item.Properties()));

    // Talisman-drawing papers. Higher tiers may substitute for every lower-tier recipe.
    public static final DeferredItem<Item> ORDINARY_YELLOW_TALISMAN_PAPER = registerItem(
            "ordinary_yellow_talisman_paper", () -> new TalismanMaterialItem(
                    new Item.Properties().stacksTo(64), TalismanMaterialItem.Kind.PAPER, 1));
    public static final DeferredItem<Item> LINGSANG_TALISMAN_PAPER = registerItem(
            "lingsang_talisman_paper", () -> new TalismanMaterialItem(
                    new Item.Properties().stacksTo(64), TalismanMaterialItem.Kind.PAPER, 2));
    public static final DeferredItem<Item> RED_VEINED_MULBERRY_PAPER = registerItem(
            "red_veined_mulberry_paper", () -> new TalismanMaterialItem(
                    new Item.Properties().stacksTo(64), TalismanMaterialItem.Kind.PAPER, 3));
    public static final DeferredItem<Item> JADE_TALISMAN_PAPER = registerItem(
            "jade_talisman_paper", () -> new TalismanMaterialItem(
                    new Item.Properties().stacksTo(64), TalismanMaterialItem.Kind.PAPER, 4));
    public static final DeferredItem<Item> CELESTIAL_SILKWORM_TALISMAN_PAPER = registerItem(
            "celestial_silkworm_talisman_paper", () -> new TalismanMaterialItem(
                    new Item.Properties().stacksTo(64), TalismanMaterialItem.Kind.PAPER, 5));

    // Talisman inks. Their registered tier is the maximum formula tier they can carry.
    public static final DeferredItem<Item> MUNDANE_CINNABAR_INK = registerItem(
            "mundane_cinnabar_ink", () -> new TalismanMaterialItem(
                    new Item.Properties().stacksTo(64), TalismanMaterialItem.Kind.INK, 1));
    public static final DeferredItem<Item> SPIRITUAL_CINNABAR_INK = registerItem(
            "spiritual_cinnabar_ink", () -> new TalismanMaterialItem(
                    new Item.Properties().stacksTo(64), TalismanMaterialItem.Kind.INK, 2));
    public static final DeferredItem<Item> MYSTERIOUS_SPIRIT_INK = registerItem(
            "mysterious_spirit_ink", () -> new TalismanMaterialItem(
                    new Item.Properties().stacksTo(64), TalismanMaterialItem.Kind.INK, 3));
    public static final DeferredItem<Item> GOLD_VEINED_SPIRIT_INK = registerItem(
            "gold_veined_spirit_ink", () -> new TalismanMaterialItem(
                    new Item.Properties().stacksTo(64), TalismanMaterialItem.Kind.INK, 4));
    public static final DeferredItem<Item> JADE_MARROW_TALISMAN_INK = registerItem(
            "jade_marrow_talisman_ink", () -> new TalismanMaterialItem(
                    new Item.Properties().stacksTo(64), TalismanMaterialItem.Kind.INK, 5));
    public static final DeferredItem<Item> SPIRIT_MARROW_TREASURE_INK = registerItem(
            "spirit_marrow_treasure_ink", () -> new TalismanMaterialItem(
                    new Item.Properties().stacksTo(64), TalismanMaterialItem.Kind.INK, 6));
    public static final DeferredItem<Item> HEAVENLY_SPIRIT_MYSTIC_INK = registerItem(
            "heavenly_spirit_mystic_ink", () -> new TalismanMaterialItem(
                    new Item.Properties().stacksTo(64), TalismanMaterialItem.Kind.INK, 7));
    public static final DeferredItem<Item> DAO_MARROW_INK = registerItem(
            "dao_marrow_ink", () -> new TalismanMaterialItem(
                    new Item.Properties().stacksTo(64), TalismanMaterialItem.Kind.INK, 8));

    /** Legacy NBT-based formula item. Kept registered so existing world saves remain valid. */
    public static final DeferredItem<Item> TALISMAN_FORMULA = registerItem(
            "talisman_formula", () -> new TalismanFormulaItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    // Individual talisman formula items, ordered exactly like TalismanRecipes.
    public static final DeferredItem<Item> QINGCHEN_TALISMAN_FORMULA = talismanFormula("qingchen_talisman");
    public static final DeferredItem<Item> WEIGUANG_TALISMAN_FORMULA = talismanFormula("weiguang_talisman");
    public static final DeferredItem<Item> LIANSHENG_TALISMAN_FORMULA = talismanFormula("liansheng_talisman");
    public static final DeferredItem<Item> FIREBALL_TALISMAN_FORMULA = talismanFormula("fireball_talisman");
    public static final DeferredItem<Item> FENGREN_TALISMAN_FORMULA = talismanFormula("fengren_talisman");

    public static final DeferredItem<Item> XINGHUO_TALISMAN_FORMULA = talismanFormula("xinghuo_talisman");
    public static final DeferredItem<Item> BOJIN_HUFU_FORMULA = talismanFormula("bojin_hufu");
    public static final DeferredItem<Item> CHANSI_KUN_TALISMAN_FORMULA = talismanFormula("chansi_kun_talisman");
    public static final DeferredItem<Item> DUANTU_SHENXING_TALISMAN_FORMULA = talismanFormula("duantu_shenxing_talisman");
    public static final DeferredItem<Item> CHUANYIN_TALISMAN_FORMULA = talismanFormula("chuanyin_talisman");
    public static final DeferredItem<Item> ZHENSHI_TALISMAN_FORMULA = talismanFormula("zhenshi_talisman");
    public static final DeferredItem<Item> FENGYAN_TALISMAN_FORMULA = talismanFormula("fengyan_talisman");
    public static final DeferredItem<Item> XIUFU_TALISMAN_FORMULA = talismanFormula("xiufu_talisman");

    public static final DeferredItem<Item> BIXIE_ANHUN_TALISMAN_FORMULA = talismanFormula("bixie_anhun_talisman");
    public static final DeferredItem<Item> ZHENRAO_TALISMAN_FORMULA = talismanFormula("zhenrao_talisman");
    public static final DeferredItem<Item> BAOFENG_TALISMAN_FORMULA = talismanFormula("baofeng_talisman");
    public static final DeferredItem<Item> HUOYU_FENXIE_TALISMAN_FORMULA = talismanFormula("huoyu_fenxie_talisman");
    public static final DeferredItem<Item> WULEI_XIAOFA_TALISMAN_FORMULA = talismanFormula("wulei_xiaofa_talisman");
    public static final DeferredItem<Item> BIAOJI_TALISMAN_FORMULA = talismanFormula("biaoji_talisman");
    public static final DeferredItem<Item> DUNKONG_TALISMAN_FORMULA = talismanFormula("dunkong_talisman");
    public static final DeferredItem<Item> HUABING_TALISMAN_FORMULA = talismanFormula("huabing_talisman");

    public static final DeferredItem<Item> JINGUANG_HUTI_TALISMAN_FORMULA = talismanFormula("jinguang_huti_talisman");
    public static final DeferredItem<Item> FENGLING_TALISMAN_FORMULA = talismanFormula("fengling_talisman");
    public static final DeferredItem<Item> POZHANG_KAISHAN_TALISMAN_FORMULA = talismanFormula("pozhang_kaishan_talisman");

    public static final DeferredItem<Item> QIANLI_CHUANXUN_TALISMAN_FORMULA = talismanFormula("qianli_chuanxun_talisman");

    public static final DeferredItem<Item> XUANYAN_FENJIE_TALISMAN_FORMULA = talismanFormula("xuanyan_fenjie_talisman");
    public static final DeferredItem<Item> WUYUE_BIE_TALISMAN_FORMULA = talismanFormula("wuyue_bie_talisman");
    public static final DeferredItem<Item> JINKONG_TALISMAN_FORMULA = talismanFormula("jinkong_talisman");

    public static final DeferredItem<Item> BLOOD = registerItem("blood",
            () -> new BloodItem(new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> BLOOD_CRYSTAL = registerItem("blood_crystal",
            () -> new BloodCrystalItem(new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> WEAPON_SHARD = registerItem("weapon_shard",
            () -> new WeaponShardItem(new Item.Properties()));
    public static final DeferredItem<Item> ASHES = registerItem("ashes",
            () -> new Item(new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> FLAME_VESSEL = registerItem("flame_vessel",
            () -> new com.example.immortal_cultivation_mod.flame.FlameVesselItem(new Item.Properties()));
    public static final DeferredItem<Item> CRIMSON_FLAME_SEED = registerItem("crimson_flame_seed",
            () -> new com.example.immortal_cultivation_mod.flame.FlameSeedItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON),
                    com.example.immortal_cultivation_mod.flame.FlameKind.CRIMSON));
    public static final DeferredItem<Item> EARTH_FLAME_SEED = registerItem("earth_flame_seed",
            () -> new com.example.immortal_cultivation_mod.flame.FlameSeedItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON),
                    com.example.immortal_cultivation_mod.flame.FlameKind.EARTH));
    public static final DeferredItem<Item> WOOD_FLAME_HEART = registerItem("wood_flame_heart",
            () -> new com.example.immortal_cultivation_mod.flame.FlameSeedItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE),
                    com.example.immortal_cultivation_mod.flame.FlameKind.WOOD));
    public static final DeferredItem<Item> SOLAR_FLAME_SEED = registerItem("solar_flame_seed",
            () -> new com.example.immortal_cultivation_mod.flame.FlameSeedItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE),
                    com.example.immortal_cultivation_mod.flame.FlameKind.SOLAR));
    public static final DeferredItem<Item> NETHER_FLAME_SEED = registerItem("nether_flame_seed",
            () -> new com.example.immortal_cultivation_mod.flame.FlameSeedItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE),
                    com.example.immortal_cultivation_mod.flame.FlameKind.NETHER));
    public static final DeferredItem<Item> THUNDER_FLAME_SEED = registerItem("thunder_flame_seed",
            () -> new com.example.immortal_cultivation_mod.flame.FlameSeedItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE),
                    com.example.immortal_cultivation_mod.flame.FlameKind.THUNDER));
    public static final DeferredItem<Item> DRAGON_FLAME_SEED = registerItem("dragon_flame_seed",
            () -> new com.example.immortal_cultivation_mod.flame.FlameSeedItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC),
                    com.example.immortal_cultivation_mod.flame.FlameKind.DRAGON));
    public static final DeferredItem<Item> HEAVENLY_FLAME_SEED = registerItem("heavenly_flame_seed",
            () -> new com.example.immortal_cultivation_mod.flame.FlameSeedItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC),
                    com.example.immortal_cultivation_mod.flame.FlameKind.HEAVENLY));
    public static final DeferredItem<Item> DRAGON_FLAME_SEED_YUJIAN = registerItem("dragon_flame_seed_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> CRIMSON_FLAME_SEED_YUJIAN = registerItem("crimson_flame_seed_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> CHI_LIN = registerItem("chi_lin",
            () -> new BeastMaterialItem(new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON), 3));

    public static final DeferredItem<Item> HUO_XI_BLOOD = registerItem("huo_xi_blood",
            () -> new BeastMaterialItem(new Item.Properties().stacksTo(64), 3));

    public static final DeferredItem<Item> LIANQI_YAODAN = registerItem("lianqi_yaodan",
            () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> JINDAN_YAODAN = registerItem("jindan_yaodan",
            () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    public static final DeferredItem<Item> CHIYAN_BONE = registerItem("chiyan_bone",
            () -> new BeastMaterialItem(new Item.Properties().stacksTo(64).rarity(Rarity.RARE), 3));

    public static final DeferredItem<Item> YUANYING_YAODAN = registerItem("yuanying_yaodan",
            () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));

    public static final DeferredItem<Item> HUASHEN_YAODAN = registerItem("huashen_yaodan",
            () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));

    public static final DeferredItem<Item> LIANXU_YAODAN = registerItem("lianxu_yaodan",
            () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));

    public static final DeferredItem<Item> HETI_YAODAN = registerItem("heti_yaodan",
            () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));

    public static final DeferredItem<Item> DACHENG_YAODAN = registerItem("dacheng_yaodan",
            () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));

    public static final DeferredItem<Item> DUJIE_YAODAN = registerItem("dujie_yaodan",
            () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));

    public static final DeferredItem<Item> YANJIAO_HORN = registerItem("yanjiao_horn",
            () -> new BeastMaterialItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 4));

    public static final DeferredItem<Item> YAOYUAN_BONE = registerItem("yaoyuan_bone",
            () -> new BeastMaterialItem(new Item.Properties().stacksTo(64).rarity(Rarity.RARE), 4));

    public static final DeferredItem<Item> XUESHA_HIDE = registerItem("xuesha_hide",
            () -> new BeastMaterialItem(new Item.Properties().stacksTo(64).rarity(Rarity.RARE), 4));

    public static final DeferredItem<Item> PHANTOM_VEIL_FUR = registerItem("phantom_veil_fur",
            () -> new PhantomVeilFurItem(new Item.Properties().stacksTo(64).rarity(Rarity.EPIC)));

    public static final DeferredItem<Item> KONGMING_STONE = registerItem("kongming_stone",
            () -> new BeastMaterialItem(new Item.Properties().stacksTo(64).rarity(Rarity.RARE), 2));

    public static final DeferredItem<Item> ROCK_BADGER_HIDE = registerItem("rock_badger_hide",
            () -> new BeastMaterialItem(new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON), 2));

    public static final DeferredItem<Item> THUNDER_PATTERNED_ROCK_PLATE =
            registerItem("thunder_patterned_rock_plate",
                    () -> new BeastMaterialItem(new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON), 2));

    public static final DeferredItem<Item> ZHUJI_YAODAN = registerItem("zhuji_yaodan",
            () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    public static final DeferredItem<Item> THUNDER_HORN = registerItem("thunder_horn",
            () -> new BeastMaterialItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 2));

    public static final DeferredItem<Item> FROSTSPINE_HIDE = registerItem("frostspine_hide",
            () -> new BeastMaterialItem(new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON), 2));

    public static final DeferredItem<Item> SPIRIT_DEVOURING_RAT_HIDE =
            registerItem("spirit_devouring_rat_hide",
                    () -> new BeastMaterialItem(new Item.Properties().stacksTo(64).rarity(Rarity.COMMON), 1));
    public static final DeferredItem<Item> SPIRIT_SCENT_WHISKER =
            registerItem("spirit_scent_whisker",
                    () -> new BeastMaterialItem(new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON), 1));
    public static final DeferredItem<Item> IRON_SCALE_PLATE = registerItem("iron_scale_plate",
            () -> new BeastMaterialItem(new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON), 1));
    public static final DeferredItem<Item> CRIMSON_VENOM_SAC = registerItem("crimson_venom_sac",
            () -> new BeastMaterialItem(new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON), 1));
    public static final DeferredItem<Item> SCORPION_TAIL_STINGER = registerItem("scorpion_tail_stinger",
            () -> new BeastMaterialItem(new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON), 1));
    public static final DeferredItem<Item> WIND_FEATHER = registerItem("wind_feather",
            () -> new BeastMaterialItem(new Item.Properties().stacksTo(64).rarity(Rarity.RARE), 2));
    public static final DeferredItem<Item> SPIRIT_DEER_ANTLER = registerItem("spirit_deer_antler",
            () -> new BeastMaterialItem(new Item.Properties().stacksTo(32).rarity(Rarity.RARE), 2));
    public static final DeferredItem<Item> INK_ARMOR_PLATE = registerItem("ink_armor_plate",
            () -> new BeastMaterialItem(new Item.Properties().stacksTo(64).rarity(Rarity.RARE), 2));
    public static final DeferredItem<Item> MINERAL_MARROW = registerItem("mineral_marrow",
            () -> new BeastMaterialItem(new Item.Properties().stacksTo(64).rarity(Rarity.RARE), 2));
    public static final DeferredItem<Item> PROFOUND_IRON_ARMOR_CORE =
            registerItem("profound_iron_armor_core",
                    () -> new BeastMaterialItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC), 2));
    public static final DeferredItem<Item> EMBER_FEATHER = registerItem("ember_feather",
            () -> new BeastMaterialItem(new Item.Properties().stacksTo(64).rarity(Rarity.RARE), 3));
    public static final DeferredItem<Item> FIRE_OWL_CLAW = registerItem("fire_owl_claw",
            () -> new BeastMaterialItem(new Item.Properties().stacksTo(32).rarity(Rarity.RARE), 3));
    public static final DeferredItem<Item> FLAME_RADIANCE_BONE = registerItem("flame_radiance_bone",
            () -> new BeastMaterialItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC), 3));
    public static final DeferredItem<Item> MORTAL_BEAST_MEAT = registerItem("mortal_beast_meat",
            () -> new Item(new Item.Properties().stacksTo(64).food(
                    new FoodProperties.Builder().nutrition(6)
                            .saturationModifier(0.55F).build())));

    public static final DeferredItem<Item> LIANQI_BEAST_MEAT = registerItem("lianqi_beast_meat",
            () -> new Item(new Item.Properties().stacksTo(64).food(
                    new FoodProperties.Builder().nutrition(6)
                            .saturationModifier(0.55F).build())));

    public static final DeferredItem<Item> ZHUJI_BEAST_MEAT = registerItem("zhuji_beast_meat",
            () -> new Item(new Item.Properties().stacksTo(64).food(
                    new FoodProperties.Builder().nutrition(6)
                            .saturationModifier(0.55F).build())));

    public static final DeferredItem<Item> JINDAN_BEAST_MEAT = registerItem("jindan_beast_meat",
            () -> new Item(new Item.Properties().stacksTo(64).food(
                    new FoodProperties.Builder().nutrition(6)
                            .saturationModifier(0.55F).build())));

    public static final DeferredItem<Item> YUANYING_BEAST_MEAT = registerItem("yuanying_beast_meat",
            () -> new Item(new Item.Properties().stacksTo(64).food(
                    new FoodProperties.Builder().nutrition(6)
                            .saturationModifier(0.55F).build())));

    public static final DeferredItem<Item> HUASHEN_BEAST_MEAT = registerItem("huashen_beast_meat",
            () -> new Item(new Item.Properties().stacksTo(64).food(
                    new FoodProperties.Builder().nutrition(6)
                            .saturationModifier(0.55F).build())));

    public static final DeferredItem<Item> LIANXU_BEAST_MEAT = registerItem("lianxu_beast_meat",
            () -> new Item(new Item.Properties().stacksTo(64).food(
                    new FoodProperties.Builder().nutrition(6)
                            .saturationModifier(0.55F).build())));

    public static final DeferredItem<Item> HETI_BEAST_MEAT = registerItem("heti_beast_meat",
            () -> new Item(new Item.Properties().stacksTo(64).food(
                    new FoodProperties.Builder().nutrition(6)
                            .saturationModifier(0.55F).build())));

    public static final DeferredItem<Item> DACHENG_BEAST_MEAT = registerItem("dacheng_beast_meat",
            () -> new Item(new Item.Properties().stacksTo(64).food(
                    new FoodProperties.Builder().nutrition(6)
                            .saturationModifier(0.55F).build())));

    public static final DeferredItem<Item> DUJIE_BEAST_MEAT = registerItem("dujie_beast_meat",
            () -> new Item(new Item.Properties().stacksTo(64).food(
                    new FoodProperties.Builder().nutrition(6)
                            .saturationModifier(0.55F).build())));

    public static final DeferredItem<Item> COLD_MARROW = registerItem("cold_marrow",
            () -> new BeastMaterialItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), 2));

    public static final DeferredItem<Item> XUANDUAN_IRON_SHARD = registerItem(
            "xuanduan_iron_shard",
            () -> new Item(new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> SPIRIT_GUIDING_GEAR = registerItem(
            "spirit_guiding_gear",
            () -> new Item(new Item.Properties().stacksTo(64).rarity(Rarity.RARE)));

    public static final DeferredItem<Item> DAMAGED_FORGING_FURNACE_CORE = registerItem(
            "damaged_forging_furnace_core",
            () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));

    public static final DeferredItem<Item> LINGMU_CLASP = registerItem("lingmu_clasp",
            () -> new Item(new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> NUOMI = registerItem("nuomi",
            () -> new BlockItem(ModBlocks.NUOMI_DUST.get(), new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> NUOMI_SEED = registerItem("nuomi_seed",
            () -> new ItemNameBlockItem(ModBlocks.NUOMI_PLANT.get(), new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> NUOMI_ZHOU = registerItem("nuomi_zhou",
            () -> new NuomiSoupItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> MODOU_LINE = registerItem("modou_line",
            () -> new ModouLineItem(new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> TAOMU_SWORD = registerItem("taomu_sword",
            () -> new TaomuSwordItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> RED_STRING = registerItem("red_string",
            () -> new Item(new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> COPPER_COIN = registerItem("copper_coin",
            () -> new Item(new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> WINE = registerItem("wine",
            () -> new ContainerFoodItem(
                    new Item.Properties().stacksTo(16),
                    wineFood(),
                    Items.GLASS_BOTTLE,
                    UseAnim.DRINK));

    public static final DeferredItem<Item> STEAMED_BUN = registerItem("steamed_bun",
            () -> new HotelFoodItem(
                    new Item.Properties(), steamedBunFood()));

    public static final DeferredItem<Item> NOODLE_BOWL = registerItem("noodle_bowl",
            () -> new ContainerFoodItem(
                    new Item.Properties().stacksTo(16),
                    noodleBowlFood(),
                    Items.BOWL,
                    UseAnim.EAT));

    public static final DeferredItem<Item> BRAISED_PORK = registerItem("braised_pork",
            () -> new HotelFoodItem(
                    new Item.Properties(), braisedPorkFood()));

    public static final DeferredItem<Item> ROAST_DUCK = registerItem("roast_duck",
            () -> new HotelFoodItem(
                    new Item.Properties(), roastDuckFood()));

    private static FoodProperties wineFood() {
        return new FoodProperties.Builder()
                .nutrition(1)
                .saturationModifier(0.1F)
                .alwaysEdible()
                .effect(() -> new MobEffectInstance(
                        MobEffects.DAMAGE_BOOST, 20 * 20, 0), 1.0F)
                .effect(() -> new MobEffectInstance(
                        MobEffects.CONFUSION, 8 * 20, 0), 0.20F)
                .build();
    }

    private static FoodProperties steamedBunFood() {
        return new FoodProperties.Builder()
                .nutrition(5)
                .saturationModifier(0.6F)
                .alwaysEdible()
                .build();
    }

    private static FoodProperties noodleBowlFood() {
        return new FoodProperties.Builder()
                .nutrition(8)
                .saturationModifier(0.7F)
                .alwaysEdible()
                .effect(() -> new MobEffectInstance(
                        MobEffects.REGENERATION, 8 * 20, 0), 1.0F)
                .build();
    }

    private static FoodProperties braisedPorkFood() {
        return new FoodProperties.Builder()
                .nutrition(10)
                .saturationModifier(0.9F)
                .alwaysEdible()
                .effect(() -> new MobEffectInstance(
                        MobEffects.DAMAGE_RESISTANCE, 30 * 20, 0), 1.0F)
                .build();
    }

    private static FoodProperties roastDuckFood() {
        return new FoodProperties.Builder()
                .nutrition(9)
                .saturationModifier(0.8F)
                .alwaysEdible()
                .effect(() -> new MobEffectInstance(
                        MobEffects.MOVEMENT_SPEED, 30 * 20, 0), 1.0F)
                .build();
    }

    public static final DeferredItem<Item> BIGU_PILL = registerItem("bigu_pill",
            () -> new IdentityItems.BiguPillItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> QI_RECOVERY_PILL = registerItem("qi_recovery_pill",
            () -> new com.example.immortal_cultivation_mod.alchemy.AlchemyPillItem("huiling"));
    public static final DeferredItem<Item> LOW_QI_RECOVERY_PILL = QI_RECOVERY_PILL;
    public static final DeferredItem<Item> MIDDLE_QI_RECOVERY_PILL = QI_RECOVERY_PILL;
    public static final DeferredItem<Item> HIGH_QI_RECOVERY_PILL = QI_RECOVERY_PILL;
    public static final DeferredItem<Item> LOW_YANGJIAN_STONE = registerItem("low_yangjian_stone",
            () -> new Item(new Item.Properties().durability(16)));
    public static final DeferredItem<Item> MIDDLE_YANGJIAN_STONE = registerItem("middle_yangjian_stone",
            () -> new Item(new Item.Properties().durability(32)));
    public static final DeferredItem<Item> HIGH_YANGJIAN_STONE = registerItem("high_yangjian_stone",
            () -> new Item(new Item.Properties().durability(64)));
    public static final DeferredItem<Item> LOW_SWORD_GRINDSTONE = registerItem("low_sword_grindstone",
            () -> new Item(new Item.Properties().durability(16)));
    public static final DeferredItem<Item> MIDDLE_SWORD_GRINDSTONE = registerItem("middle_sword_grindstone",
            () -> new Item(new Item.Properties().durability(32)));
    public static final DeferredItem<Item> HIGH_SWORD_GRINDSTONE = registerItem("high_sword_grindstone",
            () -> new Item(new Item.Properties().durability(64)));
    public static final DeferredItem<Item> LOW_SABER_GRINDSTONE = registerItem("low_saber_grindstone",
            () -> new Item(new Item.Properties().durability(16)));
    public static final DeferredItem<Item> MIDDLE_SABER_GRINDSTONE = registerItem("middle_saber_grindstone",
            () -> new Item(new Item.Properties().durability(32)));
    public static final DeferredItem<Item> HIGH_SABER_GRINDSTONE = registerItem("high_saber_grindstone",
            () -> new Item(new Item.Properties().durability(64)));
    public static final DeferredItem<Item> FAMILY_TOKEN = registerItem("family_token",
            () -> new IdentityItems.IdentityTokenItem(new Item.Properties().stacksTo(1), "clan_heir"));
    public static final DeferredItem<Item> ROYAL_TOKEN = registerItem("royal_token",
            () -> new IdentityItems.IdentityTokenItem(new Item.Properties().stacksTo(1), "royal"));
    public static final DeferredItem<Item> PROTECTIVE_JADE_PENDANT = registerItem("protective_jade_pendant",
            () -> new IdentityItems.ProtectivePendantItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> BLACK_TORTOISE_HEARTGUARD_MIRROR =
            registerItem("black_tortoise_heartguard_mirror",
                    () -> new BlackTortoiseHeartguardMirrorItem(
                            new Item.Properties().durability(6_000).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> WATER_AVOIDING_PEARL = registerItem("water_avoiding_pearl",
            () -> new CultivationAccessoryItem(new Item.Properties().durability(6_000),
                    CultivationAccessoryItem.Kind.WATER_AVOIDING_PEARL));
    public static final DeferredItem<Item> FORTUNE_MIRROR = registerItem("fortune_mirror",
            () -> new FortuneArtifactItem(new Item.Properties().stacksTo(1), FortuneArtifactItem.Kind.MIRROR));
    public static final DeferredItem<Item> FORTUNE_MIRROR_YUJIAN = registerItem("fortune_mirror_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> PRECISE_FORTUNE_MIRROR_YUJIAN = registerItem("precise_fortune_mirror_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> FORTUNE_GUARD_COIN_YUJIAN = registerItem("fortune_guard_coin_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> FORTUNE_LAMP_YUJIAN = registerItem("fortune_lamp_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> PRECISE_FORTUNE_MIRROR = registerItem("precise_fortune_mirror",
            () -> new FortuneArtifactItem(new Item.Properties().stacksTo(1), FortuneArtifactItem.Kind.PRECISE_MIRROR));
    public static final DeferredItem<Item> FORTUNE_GUARD_COIN = registerItem("fortune_guard_coin",
            () -> new FortuneArtifactItem(new Item.Properties().stacksTo(1), FortuneArtifactItem.Kind.COIN));
    public static final DeferredItem<Item> TRIBULATION_DOLL = registerItem("tribulation_doll",
            () -> new TribulationDollItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), false));
    public static final DeferredItem<Item> TRIBULATION_DOLL_YUJIAN = registerItem("tribulation_doll_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> PURIFIED_TRIBULATION_DOLL = registerItem("purified_tribulation_doll",
            () -> new TribulationDollItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), true));
    public static final DeferredItem<Item> PURIFIED_TRIBULATION_DOLL_YUJIAN = registerItem("purified_tribulation_doll_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> SOUL_ANCHOR_PEARL = registerItem("soul_anchor_pearl",
            () -> new CultivationAccessoryItem(new Item.Properties().stacksTo(1),
                    CultivationAccessoryItem.Kind.SOUL_ANCHOR_PEARL));
    public static final DeferredItem<Item> SHIELD_BREAKER_RING = registerItem("shield_breaker_ring",
            () -> new CultivationAccessoryItem(new Item.Properties().stacksTo(1),
                    CultivationAccessoryItem.Kind.SHIELD_BREAKER_RING));
    public static final DeferredItem<Item> BLOOD_FANG_PENDANT = registerItem("blood_fang_pendant",
            () -> new CultivationAccessoryItem(new Item.Properties().stacksTo(1),
                    CultivationAccessoryItem.Kind.BLOOD_FANG_PENDANT));
    public static final DeferredItem<Item> ENLIGHTENMENT_LEAF = registerItem("enlightenment_leaf",
            () -> new CultivationAccessoryItem(new Item.Properties().stacksTo(1),
                    CultivationAccessoryItem.Kind.ENLIGHTENMENT_LEAF));
    public static final DeferredItem<Item> LETHAL_POISON = registerItem("lethal_poison",
            () -> new IdentityItems.LethalPoisonItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> WANTED_ORDER = registerItem("wanted_order",
            () -> new WantedOrderItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> DISGUISE_MASK = registerItem("disguise_mask",
            () -> new DisguiseMaskItem(new Item.Properties()));
    public static final DeferredItem<Item> CORPSE_REFINING_SECRET = registerItem("corpse_refining_secret",
            () -> new CultivationMethodItem(new Item.Properties().stacksTo(1), CultivationMethods.JIUYOU_LIANSHI_JUE));

    public static final DeferredItem<Item> COPPER_COIN_SWORD = registerItem("copper_coin_sword",
            () -> new CopperCoinSwordItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> COFFIN = registerItem("coffin",
            () -> new CoffinItem(new Item.Properties()));

    public static final DeferredItem<Item> FLAG = registerItem("flag",
            () -> new FlagItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> CLOTH_SHEATH = registerItem("cloth_sheath",
            () -> new ClothSheathItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> SWORD_SHEATH = registerItem("sword_sheath",
            () -> new RigidSheathItem(new Item.Properties().stacksTo(1), RigidSheathItem.Kind.SWORD));
    public static final DeferredItem<Item> DAO_SHEATH = registerItem("dao_sheath",
            () -> new RigidSheathItem(new Item.Properties().stacksTo(1), RigidSheathItem.Kind.DAO));
    public static final DeferredItem<Item> SWORD_CASE = registerItem("sword_case",
            () -> new SwordCaseItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> XIUJIAN = registerItem("xiujian",
            () -> new XiujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> WHITE_WOOL_STRIP = woolStrip("white", DyeColor.WHITE);
    public static final DeferredItem<Item> ORANGE_WOOL_STRIP = woolStrip("orange", DyeColor.ORANGE);
    public static final DeferredItem<Item> MAGENTA_WOOL_STRIP = woolStrip("magenta", DyeColor.MAGENTA);
    public static final DeferredItem<Item> LIGHT_BLUE_WOOL_STRIP = woolStrip("light_blue", DyeColor.LIGHT_BLUE);
    public static final DeferredItem<Item> YELLOW_WOOL_STRIP = woolStrip("yellow", DyeColor.YELLOW);
    public static final DeferredItem<Item> LIME_WOOL_STRIP = woolStrip("lime", DyeColor.LIME);
    public static final DeferredItem<Item> PINK_WOOL_STRIP = woolStrip("pink", DyeColor.PINK);
    public static final DeferredItem<Item> GRAY_WOOL_STRIP = woolStrip("gray", DyeColor.GRAY);
    public static final DeferredItem<Item> LIGHT_GRAY_WOOL_STRIP = woolStrip("light_gray", DyeColor.LIGHT_GRAY);
    public static final DeferredItem<Item> CYAN_WOOL_STRIP = woolStrip("cyan", DyeColor.CYAN);
    public static final DeferredItem<Item> PURPLE_WOOL_STRIP = woolStrip("purple", DyeColor.PURPLE);
    public static final DeferredItem<Item> BLUE_WOOL_STRIP = woolStrip("blue", DyeColor.BLUE);
    public static final DeferredItem<Item> BROWN_WOOL_STRIP = woolStrip("brown", DyeColor.BROWN);
    public static final DeferredItem<Item> GREEN_WOOL_STRIP = woolStrip("green", DyeColor.GREEN);
    public static final DeferredItem<Item> RED_WOOL_STRIP = woolStrip("red", DyeColor.RED);
    public static final DeferredItem<Item> BLACK_WOOL_STRIP = woolStrip("black", DyeColor.BLACK);

    public static final DeferredItem<Item> COIN_MASK = registerItem("coin_mask",
            () -> new CoinMaskItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> BLINDFOLD = registerItem("blindfold",
            () -> new BlindfoldItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> BLINDFOLD2 = registerItem("blindfold2",
            () -> new Blindfold2Item(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> STRAW_HAT = registerItem("straw_hat",
            () -> new StrawHatItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> NOOB_HAT = registerItem("noob_hat",
            () -> new NoobHatItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> JADE_PENDANT = registerItem("jade_pendant",
            () -> new JadePendantItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> DAOFU_CHESTPLATE = registerItem("daofu_chestplate",
            () -> new DaofuChestplateItem(new Item.Properties()));

    public static final DeferredItem<Item> NEIJIA_CHESTPLATE = registerItem("neijia_chestplate",
            () -> new NeijiaChestplateItem(new Item.Properties()));

    public static final DeferredItem<Item> ZHONGYI = registerItem("zhongyi",
            () -> new ZhongyiItem(new Item.Properties()));

    public static final DeferredItem<Item> DOUPENG = registerItem("doupeng",
            () -> new CultivationGarmentItem("doupeng", "cloak"));

    public static final DeferredItem<Item> SHENYI = registerItem("shenyi",
            () -> new CultivationGarmentItem("shenyi", "inner_clothes"));

    public static final DeferredItem<Item> HECHANG = registerItem("hechang",
            () -> new CultivationGarmentItem("hechang", "cloak"));

    public static final DeferredItem<Item> YUNJIAN = registerItem("yunjian",
            () -> new CultivationGarmentItem("yunjian", "outer_clothes"));

    public static final DeferredItem<Item> CULTIVATION_FAPAO = registerItem("cultivation_fapao",
            () -> new CultivationGarmentItem("cultivation_fapao", "outer_clothes"));

    public static final DeferredItem<Item> YUANLING_PAO = registerItem("yuanling_pao",
            () -> new CultivationGarmentItem("yuanling_pao", "outer_clothes"));

    public static final DeferredItem<Item> HUFU = registerItem("hufu",
            () -> new CultivationGarmentItem("hufu", "inner_clothes"));

    public static final DeferredItem<Item> MIANSHA = registerItem("miansha",
            () -> new CultivationGarmentItem("miansha", "head_accessory"));

    public static final DeferredItem<Item> JIANGPA = registerItem("jiangpa",
            () -> new CultivationGarmentItem("jiangpa", "head_accessory"));

    public static final DeferredItem<Item> FUTOU = registerItem("futou",
            () -> new CultivationGarmentItem("futou", "head_accessory"));

    public static final DeferredItem<Item> WUSHA_HAT = registerItem("wusha_hat",
            () -> new CultivationGarmentItem("wusha_hat", "head_accessory"));

    public static final DeferredItem<Item> SONG_WUSHA_HAT = registerItem("song_wusha_hat",
            () -> new CultivationGarmentItem("song_wusha_hat", "head_accessory"));

    public static final DeferredItem<Item> CHANFU_JIN = registerItem("chanfu_jin",
            () -> new CultivationGarmentItem("chanfu_jin", "head_accessory"));

    public static final DeferredItem<Item> EMBROIDERED_FOREHEAD_BAND = registerItem("embroidered_forehead_band",
            () -> new CultivationGarmentItem("embroidered_forehead_band", "head_accessory"));

    public static final DeferredItem<Item> VEILED_DOULI = registerItem("veiled_douli",
            () -> new CultivationGarmentItem("veiled_douli", "head_accessory"));

    public static final DeferredItem<Item> FANGKOU_QIAOJIAN_LV = registerItem("fangkou_qiaojian_lv",
            () -> new CultivationGarmentItem("fangkou_qiaojian_lv", "boots"));

    public static final DeferredItem<Item> FLYING_SWORD = registerItem("flying_sword",
            () -> new FlyingSwordItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> FEIJIAN_YUJIAN = registerItem("feijian_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> QINGTONG_YINHUO_DING_YUJIAN = registerItem("qingtong_yinhuo_ding_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> CHI_JIN_SANYANG_DING_YUJIAN = registerItem("chi_jin_sanyang_ding_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> DAO_YUJIAN = registerItem("dao_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> JINGTIE_SPEAR_YUJIAN = registerItem("jingtie_spear_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> HEAVY_IRON_BRACER_YUJIAN = registerItem("heavy_iron_bracer_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> THROWING_KNIFE_YUJIAN = registerItem("throwing_knife_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> FLYING_NEEDLE_YUJIAN = registerItem("flying_needle_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> TIEGU_SHAN_YUJIAN = registerItem("tiegu_shan_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> FUCHEN_YUJIAN = registerItem("fuchen_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> WHIP_YUJIAN = registerItem("whip_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> SHIHUNFAN_YUJIAN = registerItem("shihunfan_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> ZHENPAN_YUJIAN = registerItem("zhenpan_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> BLANK_FORMATION_FLAG_YUJIAN = registerItem("blank_formation_flag_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> DURATION_FLAG_YUJIAN = registerItem("duration_flag_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> AMPLIFIER_FLAG_YUJIAN = registerItem("amplifier_flag_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> SPIRIT_GATHERING_FLAG_YUJIAN = registerItem("spirit_gathering_flag_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> HASTE_FLAG_YUJIAN = registerItem("haste_flag_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> QI_RETURN_FLAG_YUJIAN = registerItem("qi_return_flag_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> QI_POUCH_YUJIAN = registerItem("qi_pouch_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> SWORD_CASE_YUJIAN = registerItem("sword_case_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> BLOOD_FANG_PENDANT_YUJIAN = registerItem("blood_fang_pendant_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> WATER_AVOIDING_PEARL_YUJIAN = registerItem("water_avoiding_pearl_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> JADE_PENDANT_YUJIAN = registerItem("jade_pendant_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> SHIELD_BREAKER_RING_YUJIAN = registerItem("shield_breaker_ring_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> DISGUISE_MASK_YUJIAN = registerItem("disguise_mask_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> BLACK_TORTOISE_HEARTGUARD_MIRROR_YUJIAN =
            registerItem("black_tortoise_heartguard_mirror_yujian",
                    () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> ZHENHUN_BELL_YUJIAN = registerItem("zhenhun_bell_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> YINHUN_GONG_YUJIAN = registerItem("yinhun_gong_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> FOG_REVEALING_MIRROR_YUJIAN = registerItem("fog_revealing_mirror_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> YINYANG_COMPASS_YUJIAN = registerItem("yinyang_compass_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> YUANHUN_BOTTLE_YUJIAN = registerItem("yuanhun_bottle_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> DAO = registerItem("dao",
            () -> new DaoItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> JINGTIE_SPEAR = registerItem("jingtie_spear",
            () -> new JingtieSpearItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> THROWING_KNIFE = registerItem("throwing_knife",
            () -> new ThrowingKnifeItem(new Item.Properties()
                    .stacksTo(1)));
    public static final DeferredItem<Item> FLYING_NEEDLE = registerItem("flying_needle",
            () -> new FlyingNeedleItem(new Item.Properties()
                    .stacksTo(1)));

    public static final DeferredItem<Item> BLACK_IRON_JIAN = registerItem("black_iron_jian",
            () -> new HigherWeaponItem(HigherWeaponSpec.BLACK_IRON_JIAN,new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> BLACK_IRON_JIAN_YUJIAN = registerItem("black_iron_jian_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> BLACK_IRON_GREATSWORD = registerItem("black_iron_greatsword",
            () -> new HigherWeaponItem(HigherWeaponSpec.BLACK_IRON_GREATSWORD,new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> BLACK_IRON_GREATSWORD_YUJIAN = registerItem("black_iron_greatsword_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> MOON_CRESCENT_HALBERD = registerItem("moon_crescent_halberd",
            () -> new HigherWeaponItem(HigherWeaponSpec.MOON_CRESCENT_HALBERD,new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> MOON_CRESCENT_HALBERD_YUJIAN = registerItem("moon_crescent_halberd_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> SPIRIT_SILK_WHIP = registerItem("spirit_silk_whip",
            () -> new HigherWeaponGeoItem(HigherWeaponSpec.SPIRIT_SILK_WHIP,new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> SPIRIT_SILK_WHIP_YUJIAN = registerItem("spirit_silk_whip_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> SOUL_HOOK = registerItem("soul_hook",
            () -> new HigherWeaponGeoItem(HigherWeaponSpec.SOUL_HOOK,new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> SOUL_HOOK_YUJIAN = registerItem("soul_hook_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> WIND_FEATHER_FAN = registerItem("wind_feather_fan",
            () -> new HigherWeaponItem(HigherWeaponSpec.WIND_FEATHER_FAN,new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> WIND_FEATHER_FAN_YUJIAN = registerItem("wind_feather_fan_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> THUNDERWOOD_SWORD = registerItem("thunderwood_sword",
            () -> new HigherWeaponItem(HigherWeaponSpec.THUNDERWOOD_SWORD,new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> THUNDERWOOD_SWORD_YUJIAN = registerItem("thunderwood_sword_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> FLAME_PATTERN_DAO = registerItem("flame_pattern_dao",
            () -> new HigherWeaponItem(HigherWeaponSpec.FLAME_PATTERN_DAO,new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> FLAME_PATTERN_DAO_YUJIAN = registerItem("flame_pattern_dao_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> EARTHBREAKER_HAMMER = registerItem("earthbreaker_hammer",
            () -> new HigherWeaponGeoItem(HigherWeaponSpec.EARTHBREAKER_HAMMER,new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> EARTHBREAKER_HAMMER_YUJIAN = registerItem("earthbreaker_hammer_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> SEVEN_STAR_SWORD = registerItem("seven_star_sword",
            () -> new HigherWeaponItem(HigherWeaponSpec.SEVEN_STAR_SWORD,new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> SEVEN_STAR_SWORD_YUJIAN = registerItem("seven_star_sword_yujian",
            () -> new ForgingYujianItem(new Item.Properties().stacksTo(1)));


    public static final DeferredItem<Item> TIEGU_SHAN = registerItem("tiegu_shan",
            () -> new TieguShanItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> FUCHEN = registerItem("fuchen",
            () -> new FuchenItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> WHIP = registerItem("whip",
            () -> new WhipItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> HEAVY_IRON_BRACER = registerItem("heavy_iron_bracer",
            () -> new HeavyIronBracerItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> YAOYUAN_FIST_BONE = registerItem("yaoyuan_fist_bone",
            () -> new HeavyIronBracerItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), 5_000, 500.0F));
    public static final DeferredItem<Item> HEAVY_IRON_BRACER_CHESTPLATE_DEBUG =
            registerItem("heavy_iron_bracer_chestplate_debug",
                    () -> new HeavyIronBracerChestplateItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> YAOYUAN_FIST_BONE_CHESTPLATE_DEBUG =
            registerItem("yaoyuan_fist_bone_chestplate_debug",
                    () -> new YaoyuanFistBoneChestplateItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> SWORD_MOLD = registerItem("sword_mold",
            () -> new WeaponMoldItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> SABER_MOLD = registerItem("saber_mold",
            () -> new WeaponMoldItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> SPEAR_MOLD = registerItem("spear_mold",
            () -> new WeaponMoldItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> WEAPON_MOLD = registerItem("weapon_mold",
            () -> new WeaponMoldItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> BASIC_BREATHING_METHOD = registerItem("basic_breathing_method",
            () -> new CultivationMethodItem(new Item.Properties().stacksTo(1), CultivationMethods.BASIC_BREATHING));

    public static final DeferredItem<Item> CLEAR_HEART_METHOD = registerItem("clear_heart_method",
            () -> new CultivationMethodItem(new Item.Properties().stacksTo(1), CultivationMethods.CLEAR_HEART));

    public static final DeferredItem<Item> BLOOD_DEMON_JINDAN_METHOD = registerItem("blood_demon_jindan_method",
            () -> new CultivationMethodItem(new Item.Properties().stacksTo(1), CultivationMethods.BLOOD_DEMON_JINDAN));

    public static final DeferredItem<Item> BLOOD_DEMON_YUANYING_METHOD = registerItem("blood_demon_yuanying_method",
            () -> new CultivationMethodItem(new Item.Properties().stacksTo(1), CultivationMethods.BLOOD_DEMON_YUANYING));

    public static final DeferredItem<Item> BLOOD_DEMON_HUASHEN_METHOD = registerItem("blood_demon_huashen_method",
            () -> new CultivationMethodItem(new Item.Properties().stacksTo(1), CultivationMethods.BLOOD_DEMON_HUASHEN));

    public static final DeferredItem<Item> REINCARNATION_TRUE_ART_METHOD = registerItem("reincarnation_true_art_method",
            () -> new CultivationMethodItem(new Item.Properties().stacksTo(1), CultivationMethods.REINCARNATION_TRUE_ART));

    public static final DeferredItem<Item> TUNTIAN_DEMON_ART_METHOD = registerItem("tuntian_demon_art_method",
            () -> new CultivationMethodItem(new Item.Properties().stacksTo(1), CultivationMethods.TUNTIAN_DEMON_ART));

    public static final DeferredItem<Item> POKONG_JUE_METHOD = registerItem("pokong_jue_method",
            () -> new CultivationMethodItem(new Item.Properties().stacksTo(1), CultivationMethods.POKONG_JUE));

    public static final DeferredItem<Item> CHANGQING_JUE_METHOD = registerItem("changqing_jue_method",
            () -> new CultivationMethodItem(new Item.Properties().stacksTo(1), CultivationMethods.CHANGQING_JUE));

    public static final DeferredItem<Item> FENTIAN_LIFE_RENEWAL_METHOD = registerItem("fentian_life_renewal_method",
            () -> new CultivationMethodItem(new Item.Properties().stacksTo(1), CultivationMethods.FENTIAN_LIFE_RENEWAL));

    public static final DeferredItem<Item> HANTI_BINGQIN_METHOD = registerItem("hanti_bingqin_method",
            () -> new CultivationMethodItem(new Item.Properties().stacksTo(1), CultivationMethods.HANTI_BINGQIN));

    public static final DeferredItem<Item> CUIGU_LIANSHEN_METHOD = registerItem("cuigu_lianshen_method",
            () -> new CultivationMethodItem(new Item.Properties().stacksTo(1), CultivationMethods.CUIGU_LIANSHEN_JUE));

    public static final DeferredItem<Item> DARI_BATI_METHOD = registerItem("dari_bati_method",
            () -> new CultivationMethodItem(new Item.Properties().stacksTo(1), CultivationMethods.DARI_BATI_JUE));

    public static final DeferredItem<Item> GANGTING_XIAOKONG_METHOD = registerItem("gangting_xiaokong_method",
            () -> new CultivationMethodItem(new Item.Properties().stacksTo(1), CultivationMethods.GANGTING_XIAOKONG_LU));

    public static final DeferredItem<Item> XUANYUE_SUIFENG_METHOD = registerItem("xuanyue_suifeng_method",
            () -> new CultivationMethodItem(new Item.Properties().stacksTo(1), CultivationMethods.XUANYUE_SUIFENG_JUE));

    public static final DeferredItem<Item> HANLAN_NINGYUAN_METHOD = registerItem("hanlan_ningyuan_method",
            () -> new CultivationMethodItem(new Item.Properties().stacksTo(1), CultivationMethods.HANLAN_NINGYUAN_JING));

    public static final DeferredItem<Item> LINGJIAO_SCALE = registerItem("lingjiao_scale",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SHIHUNFAN = registerItem("shihunfan",
            () -> new ShihunfanItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> YUANHUN_BOTTLE = registerItem("yuanhun_bottle",
            () -> new YuanhunBottleItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> MYSTERIOUS_RING = registerItem("mysterious_ring",
            () -> new MysteriousRingItem(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.EPIC)));

    public static final DeferredItem<Item> SYSTEM = registerItem("system",
            () -> new OuterSystemItem(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.EPIC)));

    public static final DeferredItem<Item> LOTTERY_TICKET =
            registerItem("lottery_ticket",
                    () -> new Item(new Item.Properties()
                            .stacksTo(64)
                            .rarity(Rarity.RARE)));

    public static final DeferredItem<Item> GREAT_CREATION_SHARD =
            registerItem("great_creation_shard",
                    () -> new GreatCreationShardItem(new Item.Properties()
                            .stacksTo(16)
                            .rarity(Rarity.EPIC)));


    public static DeferredItem<Item> registerItem(String name, Supplier<Item> itemSupplier) {
        return ITEMS.register(name, itemSupplier);
    }

    private static DeferredItem<Item> talismanFormula(String recipeId) {
        DeferredItem<Item> formula = registerItem(recipeId + "_formula",
                () -> new TalismanFormulaItem(
                        new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), recipeId));
        TALISMAN_FORMULAS.put(recipeId, formula);
        return formula;
    }

    public static Item talismanFormulaFor(String recipeId) {
        DeferredItem<Item> formula = TALISMAN_FORMULAS.get(recipeId);
        return formula == null ? TALISMAN_FORMULA.get() : formula.get();
    }

    private static DeferredItem<Item> utilityTalisman(
            String name, UtilityTalismanItem.Kind kind,
            TalismanItem.Tier tier) {
        return registerItem(name, () -> new UtilityTalismanItem(
                new Item.Properties().stacksTo(16), kind, tier));
    }

    private static DeferredItem<Item> attackTalisman(
            String name, FireballTalismanProjectileEntity.AttackType attackType,
            TalismanItem.Tier tier) {
        return registerItem(name, () -> new AttackTalismanItem(
                new Item.Properties().stacksTo(16), attackType, tier));
    }

    private static DeferredItem<Item> woolStrip(String name, DyeColor color) {
        return registerItem(name + "_wool_strip",
                () -> new WoolStripItem(new Item.Properties().stacksTo(64), color));
    }

    public static Item woolStrip(DyeColor color) {
        return switch (color) {
            case WHITE -> WHITE_WOOL_STRIP.get();
            case ORANGE -> ORANGE_WOOL_STRIP.get();
            case MAGENTA -> MAGENTA_WOOL_STRIP.get();
            case LIGHT_BLUE -> LIGHT_BLUE_WOOL_STRIP.get();
            case YELLOW -> YELLOW_WOOL_STRIP.get();
            case LIME -> LIME_WOOL_STRIP.get();
            case PINK -> PINK_WOOL_STRIP.get();
            case GRAY -> GRAY_WOOL_STRIP.get();
            case LIGHT_GRAY -> LIGHT_GRAY_WOOL_STRIP.get();
            case CYAN -> CYAN_WOOL_STRIP.get();
            case PURPLE -> PURPLE_WOOL_STRIP.get();
            case BLUE -> BLUE_WOOL_STRIP.get();
            case BROWN -> BROWN_WOOL_STRIP.get();
            case GREEN -> GREEN_WOOL_STRIP.get();
            case RED -> RED_WOOL_STRIP.get();
            case BLACK -> BLACK_WOOL_STRIP.get();
        };
    }

    public static class SpellScrollItem extends Item {
        private final String spellId;

        public SpellScrollItem(Properties properties, String spellId) {
            super(properties);
            this.spellId = spellId;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
            if (level.isClientSide) {
                com.example.immortal_cultivation_mod.client.ClientPayloadHandlers.openScrollLearning(spellId);
            }
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
        }

        @Override
        public Component getName(ItemStack stack) {
            return super.getName(stack).copy().withStyle(elementNameColor());
        }

        private ChatFormatting elementNameColor() {
            ModSpells.SpellDef spell = ModSpells.get(spellId);
            String element = spell == null ? "" : SpiritRoots.primaryElement(spell.element());
            return switch (element) {
                case SpiritRoots.METAL -> ChatFormatting.GOLD;
                case SpiritRoots.WOOD -> ChatFormatting.GREEN;
                case SpiritRoots.WATER -> ChatFormatting.AQUA;
                case SpiritRoots.FIRE -> ChatFormatting.RED;
                case SpiritRoots.EARTH -> ChatFormatting.YELLOW;
                case SpiritRoots.THUNDER -> ChatFormatting.LIGHT_PURPLE;
                case SpiritRoots.ICE -> ChatFormatting.BLUE;
                case SpiritRoots.WIND -> ChatFormatting.DARK_AQUA;
                case SpiritRoots.YANG -> ChatFormatting.WHITE;
                case SpiritRoots.YIN -> ChatFormatting.DARK_PURPLE;
                default -> ChatFormatting.GRAY;
            };
        }

        public String spellId() {
            return spellId;
        }
    }

    public static class EnlightenmentPillItem extends com.example.immortal_cultivation_mod.alchemy.LegacyPillItem {
        public EnlightenmentPillItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.isClientSide) {
                com.example.immortal_cultivation_mod.client.ClientPayloadHandlers.openSpiritRootSelectionOrMessage(player,
                        Component.translatable("message." + ImmortalCultivationMod.MODID + ".enlightenment_already_cultivating"));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }

    public static class LevelUpItem extends Item {
        private final int progressAmount;
        private final boolean fillToRequirement;

        public LevelUpItem(Properties properties, int progressAmount, boolean fillToRequirement) {
            super(properties);
            this.progressAmount = progressAmount;
            this.fillToRequirement = fillToRequirement;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer sp) {
                var data = ModAttachments.getData(sp);
                long need = CultivationLevels.getTotalQiNeeded(data.cultivationLevel());
                long progress = fillToRequirement ? need : Math.min(need, data.cultivationProgress() + com.example.immortal_cultivation_mod.attachment.BodyTypeEffects.cultivationGain(sp, progressAmount));
                ModAttachments.setData(sp, data.withCultivationProgress(progress));
                if (!sp.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".level_up_progress", progress, need));
                com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }

    public static class QiPouchItem extends Item {
        private static final int OPEN_COST = 10;

        public QiPouchItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide && player instanceof ServerPlayer sp) {
                boolean wasUnknown = !QiPouchStorage.hasPurity(stack);
                int purity = QiPouchStorage.ensurePurity(stack, sp);
                if (wasUnknown) {
                    sp.displayClientMessage(Component.translatable(
                            "message." + ImmortalCultivationMod.MODID
                                    + ".qi_pouch.purity_determined",
                            purity, QiPouchWorld.sideForPurity(purity),
                            QiPouchWorld.sideForPurity(purity),
                            QiPouchWorld.HEIGHT), true);
                }
                var data = ModAttachments.getData(sp);
                if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, OPEN_COST)) {
                    sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
                    return InteractionResultHolder.fail(stack);
                }

                com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
                sp.openMenu(new SimpleMenuProvider(
                        (containerId, inventory, openedPlayer) ->
                                new com.example.immortal_cultivation_mod.screen.QiPouchMenu(containerId, inventory, stack),
                        Component.translatable("container." + ImmortalCultivationMod.MODID + ".qi_pouch")
                ));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        @Override
        public void appendHoverText(
                ItemStack stack, TooltipContext context,
                List<Component> tooltip, TooltipFlag flag) {
            if (!QiPouchStorage.hasPurity(stack)) {
                tooltip.add(Component.translatable(
                                "tooltip." + ImmortalCultivationMod.MODID
                                        + ".material_purity_unknown")
                        .withStyle(ChatFormatting.GRAY));
            } else {
                int purity = QiPouchStorage.purity(stack);
                int side = QiPouchWorld.sideForPurity(purity);
                tooltip.add(Component.translatable(
                                "tooltip." + ImmortalCultivationMod.MODID
                                        + ".material_purity", purity)
                        .withStyle(ChatFormatting.AQUA));
                tooltip.add(Component.translatable(
                                "tooltip." + ImmortalCultivationMod.MODID
                                        + ".qi_pouch_space",
                                side, side, QiPouchWorld.HEIGHT)
                        .withStyle(ChatFormatting.DARK_GREEN));
            }
            super.appendHoverText(stack, context, tooltip, flag);
        }
    }

    public static class DebugStatEditorItem extends Item {
        public DebugStatEditorItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                com.example.immortal_cultivation_mod.network.ModPayloads.openDebugStatEditor(
                        serverPlayer, serverPlayer);
            }
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
        }

        @Override
        public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                       net.minecraft.world.entity.LivingEntity target,
                                                       InteractionHand hand) {
            if (!player.isShiftKeyDown()) {
                return super.interactLivingEntity(stack, player, target, hand);
            }

            if (target instanceof Player) {
                if (!player.level().isClientSide
                        && player instanceof ServerPlayer viewer
                        && target instanceof ServerPlayer targetPlayer) {
                    com.example.immortal_cultivation_mod.network.ModPayloads.openDebugStatEditor(
                            viewer, targetPlayer);
                }
                return InteractionResult.sidedSuccess(player.level().isClientSide);
            }

            if (target instanceof net.minecraft.world.entity.PathfinderMob mob
                    && target instanceof com.example.immortal_cultivation_mod.entity.CultivationNpc) {
                if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                    com.example.immortal_cultivation_mod.entity.NpcProfile.openDebugRelationship(
                            mob, serverPlayer);
                }
                return InteractionResult.sidedSuccess(player.level().isClientSide);
            }
            return super.interactLivingEntity(stack, player, target, hand);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            Player player = context.getPlayer();
            Level level = context.getLevel();
            net.minecraft.core.BlockPos pos = context.getClickedPos();
            if (player == null || !player.isShiftKeyDown()
                    || !level.getBlockState(pos).is(ModBlocks.DUNWU_STELE.get())) {
                return super.useOn(context);
            }
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                    && level.getBlockEntity(pos)
                    instanceof com.example.immortal_cultivation_mod.block.GeoRockBlockEntity stele) {
                com.example.immortal_cultivation_mod.network.ModPayloads.openDunwuSteleEditor(
                        serverPlayer, pos, stele.steleSpellId());
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

    }

    public static class CultivationMethodItem extends Item {
        private final String methodId;

        public CultivationMethodItem(Properties properties, String methodId) {
            super(properties);
            this.methodId = methodId;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.isClientSide) {
                com.example.immortal_cultivation_mod.client.ClientPayloadHandlers.openMethodLearning(methodId);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }

    public static class BloodItem extends Item {
        public BloodItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide && player instanceof ServerPlayer sp) {
                if (!CultivationAccessoryItem.consumeBloodItem(
                        sp, stack, 1, sp.isShiftKeyDown())) {
                    return InteractionResultHolder.fail(stack);
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }

    public static class BloodCrystalItem extends Item {
        public BloodCrystalItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide && player instanceof ServerPlayer sp
                    && !CultivationAccessoryItem.consumeBloodItem(
                    sp, stack, 5, sp.isShiftKeyDown())) {
                return InteractionResultHolder.fail(stack);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }

    public static class FogRevealingMirrorItem extends Item {
        private static final int QI_COST = 50;

        public FogRevealingMirrorItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide && player instanceof ServerPlayer sp) {
                var data = ModAttachments.getData(sp);
                if (!com.example.immortal_cultivation_mod.event.ServerEvents.spendQiOrBlood(sp, data, QI_COST)) {
                    sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
                    return InteractionResultHolder.fail(stack);
                }

                int found = com.example.immortal_cultivation_mod.event.ServerEvents.startFogMirrorReveal(sp);
                var ambientQi = com.example.immortal_cultivation_mod.event.ServerEvents.refreshAmbientQi(sp);
                sp.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".fog_mirror_found",
                        found, ambientQi.value(), ambientQi.spiritVeinCenters()));
                com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(sp);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }

}
