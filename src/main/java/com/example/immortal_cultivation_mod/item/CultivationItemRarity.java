package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class CultivationItemRarity {
    private static final Map<String, Integer> ITEM_RARITY_NUMBERS = createItemRarityNumbers();

    private CultivationItemRarity() {
    }

    public static boolean isModItem(ItemStack stack) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key != null && ImmortalCultivationMod.MODID.equals(key.getNamespace());
    }

    public static Tier of(ItemStack stack) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key != null && ImmortalCultivationMod.MODID.equals(key.getNamespace())
                && ForgingSystem.isForgedWeapon(stack)) {
            return Tier.RARE;
        }
        Integer listed = key == null ? null : ITEM_RARITY_NUMBERS.get(key.toString());
        if (listed != null) {
            return tierForNumber(listed);
        }
        if (key == null || !ImmortalCultivationMod.MODID.equals(key.getNamespace())) {
            return switch (stack.getRarity()) {
                case COMMON -> Tier.COMMON;
                case UNCOMMON -> Tier.UNCOMMON;
                case RARE -> Tier.RARE;
                case EPIC -> Tier.EPIC;
            };
        }
        return Tier.COMMON;
    }

    public static int giftValue(ItemStack stack) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Integer listed = key == null ? null : ITEM_RARITY_NUMBERS.get(key.toString());
        if (listed != null) {
            return ForgingSystem.isForgedWeapon(stack) ? Math.max(Tier.RARE.giftValue(), listed) : listed;
        }

        int value = switch (stack.getRarity()) {
            case COMMON -> 2;
            case UNCOMMON -> 7;
            case RARE -> 22;
            case EPIC -> 70;
        };
        if (stack.has(DataComponents.FOOD)) {
            value = Math.min(value, 3);
        }
        if (stack.isDamageableItem()) {
            value += Math.min(30, stack.getMaxDamage() / 100);
        }
        if (stack.isEnchanted()) {
            value += Math.max(1, value / 2);
        }
        return value;
    }

    public static Component tooltip(ItemStack stack) {
        Tier tier = of(stack);
        Component rarity = Component.translatable(
                "rarity." + ImmortalCultivationMod.MODID + "." + tier.key()).withStyle(tier.color());
        return Component.translatable("tooltip." + ImmortalCultivationMod.MODID + ".rarity",
                        rarity, giftValue(stack))
                .withStyle(ChatFormatting.DARK_GRAY);
    }

    public static Map<String, Integer> listedNumbers() {
        return ITEM_RARITY_NUMBERS;
    }

    private static Tier tierForNumber(int number) {
        if (number >= Tier.EPIC.giftValue()) {
            return Tier.EPIC;
        }
        if (number >= Tier.RARE.giftValue()) {
            return Tier.RARE;
        }
        if (number >= Tier.UNCOMMON.giftValue()) {
            return Tier.UNCOMMON;
        }
        return Tier.COMMON;
    }

    private static Map<String, Integer> createItemRarityNumbers() {
        Map<String, Integer> values = new HashMap<>();

        putMod(values, 0,
                "debug_stat_editor", "zhenshi_seal_helmet", "heavy_iron_bracer_chestplate_debug",
                "yaoyuan_fist_bone_chestplate_debug",
                "zhuji_cultivator_spawn_egg", "jindan_cultivator_spawn_egg",
                "fanren_npc_spawn_egg",
                "crimson_scale_flame_lizard_spawn_egg",
                "crimson_sky_flame_flood_dragon_spawn_egg",
                "blood_ape_demon_general_spawn_egg",
                "azure_spirit_silkworm_spawn_egg");

        putMod(values, 2,
                "blood", "nuomi", "nuomi_seed", "red_string", "copper_coin",
                "white_wool_strip", "orange_wool_strip", "magenta_wool_strip",
                "light_blue_wool_strip", "yellow_wool_strip", "lime_wool_strip",
                "pink_wool_strip", "gray_wool_strip", "light_gray_wool_strip",
                "cyan_wool_strip", "purple_wool_strip", "blue_wool_strip",
                "brown_wool_strip", "green_wool_strip", "red_wool_strip",
                "black_wool_strip", "nuomi_plant", "qingling_grass",
                "cloth_sheath", "xiujian", "wanted_order");

        putMod(values, 5,
                "nuomi_zhou", "bigu_pill", "low_qi_recovery_pill",
                "low_yangjian_stone", "low_sword_grindstone", "low_saber_grindstone",
                "lethal_poison", "chiyan_flower", "ninglu_zhi", "ziyun_vine",
                "jinwen_fruit", "blindfold", "blindfold2", "straw_hat", "flag");

        putMod(values, 10,
                "xuanbing_lotus", "shihun_grass", "huanxia_si",
                "modou_line", "taomu_sword", "copper_coin_sword",
                "fengyan_talisman", "fireball_talisman",
                "cu_tie_xiaoding", "qingtong_yinhuo_ding",
                "sword_mold", "saber_mold", "spear_mold", "weapon_mold",
                "daofu_chestplate","enlightenment_pill");

        putMod(values, 20,
                "fireball_scroll", "beam_scroll", "cleanse_scroll",
                "qi_gathering_scroll", "spirit_sight_scroll",
                "lingzhi_bullet_scroll", "wind_blade_scroll",
                "wind_step_scroll", "smoke_art_scroll",
                "ignite_flare_scroll",
                "basic_breathing_method",
                "body_tempering_pill", "blood_qi_pill",
                "blood_crystal", "zhenhun_bell", "yinhun_gong");

        putMod(values, 35,
                "lingbeng_scroll", "earth_escape_scroll",
                "yufeng_jue_scroll", "yinlei_jue_scroll",
                "huti_qi_scroll", "michen_zhang_scroll",
                "sliding_water_scroll",
                "regeneration_scroll",
                "light_beam_attack_scroll",
                "kongshi_shu_scroll",
                "clear_heart_method",
                "middle_qi_recovery_pill",
                "middle_yangjian_stone",
                "middle_sword_grindstone",
                "middle_saber_grindstone",
                "qi_pouch", "breakthrough_pill",
                "zhenshi_talisman");

        putMod(values, 50,
                "jingtie_motherstone", "yunwen_silver_crystal",
                "lingjiao_scale", "bisui_stone", "hanpo_jade",
                "moxuan_iron", "liujin_sand", "chitong_sui",
                "flying_sword", "dao", "jingtie_spear", "tiegu_shan", "whip",
                "flying_needle",
                "heavy_iron_bracer_yujian",
                "small_level_up",
                "jiuye_xianlan", "family_token", "disguise_mask","star_dust_stone",
                "sword_case");

        putMod(values, 75,
                "dielang_shield_scroll",
                "zhenshan_palm_scroll",
                "dingshen_scroll",
                "wulei_zhengfa_scroll",
                "sifang_jie_scroll",
                "gushi_shield_scroll",
                "xuying_ta_scroll",
                "duanliu_kongdun_scroll",
                "yihen_ci_scroll",
                "jinhuo_xinliao_scroll",
                "suigui_xingyun_scroll",
                "shixu_chiliu_scroll",
                "liuguang_jianying_scroll",
                "yinyang_compass", "coffin", "fugu_tai",
                "high_qi_recovery_pill",
                "high_yangjian_stone",
                "high_sword_grindstone",
                "high_saber_grindstone",
                "big_level_up",
                "royal_token");

        putMod(values, 100,
                "blood_demon_jindan_method",
                "corpse_refining_secret",
                "qiannian_lingmu_heart");

        putMod(values, 150,
                "pokong_jue_method",
                "changqing_jue_method",
                "fentian_life_renewal_method",
                "blood_demon_yuanying_method",
                "feijian_yujian", "dao_yujian", "jingtie_spear_yujian",
                "flying_needle_yujian",
                "protective_jade_pendant", "fog_revealing_mirror");

        putMod(values, 250,
                "cuigu_lianshen_method",
                "hanti_bingqin_method");

        putMod(values, 350,
                "blood_demon_huashen_method");

        putMod(values, 500,
                "reincarnation_true_art_method",
                "tuntian_demon_art_method", "jade_pendant");

        put(values, 0,
                "minecraft:dirt", "minecraft:cobblestone", "minecraft:rotten_flesh",
                "minecraft:poisonous_potato", "minecraft:stick");
        put(values, 1, "minecraft:diamond");
        put(values, 3, "minecraft:golden_apple");
        put(values, 5, "minecraft:netherite_ingot");
        put(values, 8, "minecraft:enchanted_golden_apple");
        put(values, 10, "minecraft:nether_star");

        put(values, 10,
                "immortal_cultivation_mod:spirit_stone", "immortal_cultivation_mod:low_grade_spirit_stone",
                "immortal_cultivation_mod:low_spirit_stone", "immortal_cultivation_mod:lower_grade_spirit_stone");
        put(values, 100,
                "immortal_cultivation_mod:middle_grade_spirit_stone", "immortal_cultivation_mod:medium_grade_spirit_stone",
                "immortal_cultivation_mod:mid_grade_spirit_stone", "immortal_cultivation_mod:middle_spirit_stone",
                "immortal_cultivation_mod:medium_spirit_stone");
        put(values, 1_000,
                "immortal_cultivation_mod:high_grade_spirit_stone", "immortal_cultivation_mod:high_spirit_stone",
                "immortal_cultivation_mod:upper_grade_spirit_stone", "immortal_cultivation_mod:upper_spirit_stone");

        return Map.copyOf(values);
    }

    private static void putMod(Map<String, Integer> values, int number, String... paths) {
        for (String path : paths) {
            values.put(ImmortalCultivationMod.MODID + ":" + path, number);
        }
    }

    private static void put(Map<String, Integer> values, int number, String... itemIds) {
        for (String itemId : itemIds) {
            values.put(itemId, number);
        }
    }

    public enum Tier {
        COMMON("common", 2, ChatFormatting.GRAY),
        UNCOMMON("uncommon", 10, ChatFormatting.YELLOW),
        RARE("rare", 50, ChatFormatting.AQUA),
        EPIC("epic", 500, ChatFormatting.LIGHT_PURPLE);

        private final String key;
        private final int giftValue;
        private final ChatFormatting color;

        Tier(String key, int giftValue, ChatFormatting color) {
            this.key = key;
            this.giftValue = giftValue;
            this.color = color;
        }

        public String key() {
            return key;
        }

        public int giftValue() {
            return giftValue;
        }

        public ChatFormatting color() {
            return color;
        }
    }
}
