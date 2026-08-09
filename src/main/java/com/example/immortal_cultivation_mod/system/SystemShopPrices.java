package com.example.immortal_cultivation_mod.system;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.item.CultivationItemRarity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class SystemShopPrices {
    private static final int MAX_PRICE = 1_000_000;
    private static final Map<String, Integer> BASE_PRICES = createBasePrices();
    private static volatile PriceState state;

    private SystemShopPrices() {
    }

    public static int price(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        PriceState current = state();
        if (current == null) {
            return fallbackPrice(stack.getItem());
        }
        return current.prices().getOrDefault(
                stack.getItem(), fallbackPrice(stack.getItem()));
    }

    public static void invalidate() {
        state = null;
        SystemShop.invalidateCatalog();
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        invalidate();
    }

    private static PriceState state() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }
        RecipeManager manager = server.getRecipeManager();
        int recipeCount = manager.getRecipes().size();
        PriceState current = state;
        if (current != null
                && current.manager() == manager
                && current.recipeCount() == recipeCount) {
            return current;
        }
        synchronized (SystemShopPrices.class) {
            current = state;
            if (current == null
                    || current.manager() != manager
                    || current.recipeCount() != recipeCount) {
                Resolver resolver = new Resolver(server, manager);
                Map<Item, Integer> prices = new HashMap<>();
                for (Item item : BuiltInRegistries.ITEM) {
                    prices.put(item, resolver.resolvePrice(item));
                }
                state = new PriceState(
                        manager,
                        recipeCount,
                        Map.copyOf(prices));
            }
            return state;
        }
    }

    private static Map<String, Integer> createBasePrices() {
        Map<String, Integer> values = new HashMap<>();

        putVanilla(values, 1,
                "stone", "cobblestone", "deepslate", "cobbled_deepslate",
                "granite", "diorite", "andesite", "tuff", "calcite",
                "dripstone_block", "pointed_dripstone", "dirt", "grass_block",
                "sand", "red_sand", "gravel", "clay_ball", "mud",
                "netherrack", "soul_sand", "soul_soil", "basalt", "blackstone",
                "end_stone", "snowball", "ice", "moss_block", "moss_carpet",
                "vine", "glow_lichen", "lily_pad", "seagrass", "kelp",
                "bamboo", "cactus", "sugar_cane", "wheat", "wheat_seeds",
                "carrot", "potato", "poisonous_potato", "beetroot",
                "beetroot_seeds", "melon_slice", "melon_seeds", "pumpkin_seeds",
                "sweet_berries", "brown_mushroom", "red_mushroom", "apple",
                "egg", "feather", "ink_sac", "rotten_flesh", "flint",
                "string", "spider_eye", "bone_meal", "white_dye", "black_dye",
                "red_dye", "green_dye", "brown_dye", "blue_dye", "purple_dye",
                "cyan_dye", "light_gray_dye", "gray_dye", "pink_dye",
                "lime_dye", "yellow_dye", "light_blue_dye", "magenta_dye",
                "orange_dye", "allium", "azure_bluet", "blue_orchid",
                "cornflower", "dandelion", "lily_of_the_valley", "oxeye_daisy",
                "poppy", "red_tulip", "orange_tulip", "white_tulip",
                "pink_tulip", "torchflower", "torchflower_seeds", "pitcher_pod",
                "small_dripleaf", "big_dripleaf", "hanging_roots", "mangrove_roots",
                "dead_bush", "fern", "short_grass", "large_fern", "tall_grass");

        putVanilla(values, 2,
                "raw_copper", "copper_ingot", "copper_ore", "deepslate_copper_ore",
                "lapis_lazuli", "lapis_ore", "deepslate_lapis_ore", "quartz",
                "nether_quartz_ore", "bone", "leather", "rabbit", "rabbit_hide",
                "chicken", "cod", "salmon", "tropical_fish", "beef", "porkchop",
                "mutton", "glow_berries", "cocoa_beans", "crimson_fungus",
                "warped_fungus", "honeycomb", "prismarine_shard", "scute",
                "armadillo_scute", "goat_horn", "nautilus_shell_fragment");

        putVanilla(values, 3,
                "raw_iron", "iron_ingot", "iron_ore", "deepslate_iron_ore",
                "glow_ink_sac", "pufferfish", "rabbit_foot", "prismarine_crystals",
                "nether_wart", "chorus_fruit", "gunpowder", "slime_ball",
                "clay", "sponge", "wet_sponge", "honey_bottle");

        putVanilla(values, 4,
                "coal", "coal_ore", "deepslate_coal_ore", "amethyst_shard",
                "redstone_ore", "deepslate_redstone_ore", "glowstone_dust",
                "obsidian", "cobweb", "pumpkin", "chorus_flower", "sea_pickle",
                "turtle_egg", "sniffer_egg");

        putVanilla(values, 6,
                "raw_gold", "gold_ingot", "gold_ore", "deepslate_gold_ore",
                "nether_gold_ore", "ancient_debris", "netherite_scrap",
                "blaze_powder", "phantom_membrane", "magma_cream");

        putVanilla(values, 8,
                "emerald", "emerald_ore", "deepslate_emerald_ore",
                "crying_obsidian", "rabbit_foot", "sculk", "sculk_vein",
                "ochre_froglight", "verdant_froglight", "pearlescent_froglight");

        putVanilla(values, 12,
                "ender_pearl", "echo_shard", "disc_fragment_5", "dragon_breath",
                "trial_key", "breeze_rod", "shulker_shell");

        putVanilla(values, 16,
                "diamond", "diamond_ore", "deepslate_diamond_ore", "blaze_rod",
                "name_tag", "saddle", "ominous_bottle", "ominous_trial_key",
                "music_disc_13", "music_disc_cat", "music_disc_blocks",
                "music_disc_chirp", "music_disc_far", "music_disc_mall",
                "music_disc_mellohi", "music_disc_stal", "music_disc_strad",
                "music_disc_ward", "music_disc_11", "music_disc_wait",
                "music_disc_otherside", "music_disc_5", "music_disc_pigstep",
                "music_disc_relic", "music_disc_creator", "music_disc_creator_music_box",
                "music_disc_precipice");

        putVanilla(values, 20,
                "ghast_tear", "nautilus_shell", "heart_of_the_sea");

        putVanilla(values, 24,
                "iron_horse_armor", "golden_horse_armor", "wolf_armor");

        putVanilla(values, 32,
                "diamond_horse_armor", "creeper_head", "zombie_head",
                "skeleton_skull", "wither_skeleton_skull", "piglin_head",
                "player_head", "heavy_core");

        putVanilla(values, 64,
                "nether_star", "trident", "heart_of_the_sea");

        putVanilla(values, 128,
                "enchanted_golden_apple", "elytra", "totem_of_undying");

        putVanilla(values, 256, "dragon_egg");

        putVanilla(values, 4,
                "oak_log", "spruce_log", "birch_log", "jungle_log", "acacia_log",
                "dark_oak_log", "mangrove_log", "cherry_log", "pale_oak_log",
                "crimson_stem", "warped_stem", "oak_wood", "spruce_wood",
                "birch_wood", "jungle_wood", "acacia_wood", "dark_oak_wood",
                "mangrove_wood", "cherry_wood", "pale_oak_wood", "crimson_hyphae",
                "warped_hyphae", "stripped_oak_log", "stripped_spruce_log",
                "stripped_birch_log", "stripped_jungle_log", "stripped_acacia_log",
                "stripped_dark_oak_log", "stripped_mangrove_log", "stripped_cherry_log",
                "stripped_pale_oak_log", "stripped_crimson_stem", "stripped_warped_stem",
                "stripped_oak_wood", "stripped_spruce_wood", "stripped_birch_wood",
                "stripped_jungle_wood", "stripped_acacia_wood", "stripped_dark_oak_wood",
                "stripped_mangrove_wood", "stripped_cherry_wood", "stripped_pale_oak_wood",
                "stripped_crimson_hyphae", "stripped_warped_hyphae");

        putVanilla(values, 1,
                "oak_leaves", "spruce_leaves", "birch_leaves", "jungle_leaves",
                "acacia_leaves", "dark_oak_leaves", "mangrove_leaves", "cherry_leaves",
                "pale_oak_leaves", "azalea_leaves", "flowering_azalea_leaves");

        putVanilla(values, 2,
                "oak_sapling", "spruce_sapling", "birch_sapling", "jungle_sapling",
                "acacia_sapling", "dark_oak_sapling", "mangrove_propagule",
                "cherry_sapling", "pale_oak_sapling", "azalea", "flowering_azalea");

        putVanilla(values, 4,
                "white_wool", "orange_wool", "magenta_wool", "light_blue_wool",
                "yellow_wool", "lime_wool", "pink_wool", "gray_wool",
                "light_gray_wool", "cyan_wool", "purple_wool", "blue_wool",
                "brown_wool", "green_wool", "red_wool", "black_wool");

        putVanilla(values, 10,
                "water_bucket", "milk_bucket", "powder_snow_bucket");

        putVanilla(values, 14, "lava_bucket");

        putVanilla(values, 16,
                "cod_bucket", "salmon_bucket", "pufferfish_bucket",
                "tropical_fish_bucket", "tadpole_bucket", "axolotl_bucket");

        putVanilla(values, 4,
                "angler_pottery_sherd", "archer_pottery_sherd", "arms_up_pottery_sherd",
                "blade_pottery_sherd", "brewer_pottery_sherd", "burn_pottery_sherd",
                "danger_pottery_sherd", "explorer_pottery_sherd", "flow_pottery_sherd",
                "friend_pottery_sherd", "guster_pottery_sherd", "heart_pottery_sherd",
                "heartbreak_pottery_sherd", "howl_pottery_sherd", "miner_pottery_sherd",
                "mourner_pottery_sherd", "plenty_pottery_sherd", "prize_pottery_sherd",
                "scrape_pottery_sherd", "sheaf_pottery_sherd", "shelter_pottery_sherd",
                "skull_pottery_sherd", "snort_pottery_sherd");

        values.put(ImmortalCultivationMod.MODID + ":spirit_stone", 10);
        values.put(ImmortalCultivationMod.MODID + ":medium_spirit_stone", 100);
        values.put(ImmortalCultivationMod.MODID + ":high_spirit_stone", 1_000);
        values.put(ImmortalCultivationMod.MODID + ":qingling_grass", 10);
        values.put(ImmortalCultivationMod.MODID + ":chiyan_flower", 20);
        values.put(ImmortalCultivationMod.MODID + ":ninglu_zhi", 30);
        values.put(ImmortalCultivationMod.MODID + ":jinwen_fruit", 40);
        values.put(ImmortalCultivationMod.MODID + ":xuanbing_lotus", 60);
        values.put(ImmortalCultivationMod.MODID + ":jingtie_motherstone", 300);
        values.put(ImmortalCultivationMod.MODID + ":yunwen_silver_crystal", 400);
        values.put(ImmortalCultivationMod.MODID + ":hanpo_jade", 600);
        values.put(ImmortalCultivationMod.MODID + ":moxuan_iron", 800);
        values.put(ImmortalCultivationMod.MODID + ":qingtong_yinhuo_ding", 20_000);
        values.put(ImmortalCultivationMod.MODID + ":chi_lin", 100);
        values.put(ImmortalCultivationMod.MODID + ":huo_xi_blood", 10);
        values.put(ImmortalCultivationMod.MODID + ":jindan_yaodan", 10_000);
        values.put(ImmortalCultivationMod.MODID + ":chiyan_bone", 9_000);

        values.put(ImmortalCultivationMod.MODID + ":fireball_scroll", 100); // 火球术玉简
        values.put(ImmortalCultivationMod.MODID + ":lingbeng_scroll", 500); // 灵崩玉简
        values.put(ImmortalCultivationMod.MODID + ":regeneration_scroll", 300); // 回春术玉简
        values.put(ImmortalCultivationMod.MODID + ":beam_scroll", 100); // 光束术玉简
        values.put(ImmortalCultivationMod.MODID + ":earth_escape_scroll", 500); // 遁地术玉简
        values.put(ImmortalCultivationMod.MODID + ":cleanse_scroll", 100); // 清洁咒玉简
        values.put(ImmortalCultivationMod.MODID + ":qi_gathering_scroll", 100); // 引灵诀玉简
        values.put(ImmortalCultivationMod.MODID + ":ignite_flare_scroll", 150); // 燃火诀玉简
        values.put(ImmortalCultivationMod.MODID + ":spirit_sight_scroll", 250); // 灵视玉简
        values.put(ImmortalCultivationMod.MODID + ":zhenshan_palm_scroll", 12_800); // 震山掌玉简
        values.put(ImmortalCultivationMod.MODID + ":light_beam_attack_scroll", 2_000); // 光球术玉简
        values.put(ImmortalCultivationMod.MODID + ":dielang_shield_scroll", 1_500); // 叠浪盾玉简
        values.put(ImmortalCultivationMod.MODID + ":lingzhi_bullet_scroll", 100); // 灵指弹玉简
        values.put(ImmortalCultivationMod.MODID + ":wind_blade_scroll", 100); // 风刃玉简
        values.put(ImmortalCultivationMod.MODID + ":wind_step_scroll", 200); // 踏风步玉简
        values.put(ImmortalCultivationMod.MODID + ":yufeng_jue_scroll", 1_000); // 御风诀玉简
        values.put(ImmortalCultivationMod.MODID + ":smoke_art_scroll", 300); // 迷烟术玉简
        values.put(ImmortalCultivationMod.MODID + ":huti_qi_scroll", 500); // 护体气玉简
        values.put(ImmortalCultivationMod.MODID + ":michen_zhang_scroll", 800); // 迷尘障玉简
        values.put(ImmortalCultivationMod.MODID + ":sliding_water_scroll", 800); // 滑水术玉简
        values.put(ImmortalCultivationMod.MODID + ":dingshen_scroll", 1_000); // 定身咒玉简
        values.put(ImmortalCultivationMod.MODID + ":yinlei_jue_scroll", 1_500); // 引雷诀玉简
        values.put(ImmortalCultivationMod.MODID + ":wulei_zhengfa_scroll", 3_000); // 五雷正法玉简
        values.put(ImmortalCultivationMod.MODID + ":liuguang_jianying_scroll", 5_000); // 流光剑影玉简
        values.put(ImmortalCultivationMod.MODID + ":sifang_jie_scroll", 8_000); // 四方界玉简
        values.put(ImmortalCultivationMod.MODID + ":gushi_shield_scroll", 2_000); // 固石盾玉简
        values.put(ImmortalCultivationMod.MODID + ":kongshi_shu_scroll", 1_000); // 控尸术玉简
        values.put(ImmortalCultivationMod.MODID + ":xuying_ta_scroll", 10_000); // 虚影踏玉简
        values.put(ImmortalCultivationMod.MODID + ":duanliu_kongdun_scroll", 12_000); // 断流空遁玉简
        values.put(ImmortalCultivationMod.MODID + ":yihen_ci_scroll", 8_000); // 一痕刺玉简
        values.put(ImmortalCultivationMod.MODID + ":jinhuo_xinliao_scroll", 15_000); // 烬火心燎玉简
        values.put(ImmortalCultivationMod.MODID + ":suigui_xingyun_scroll", 18_000); // 随轨行云玉简
        values.put(ImmortalCultivationMod.MODID + ":shixu_chiliu_scroll", 25_000); // 时序滞流玉简
        values.put(ImmortalCultivationMod.MODID + ":enlightenment_pill", 500);
        values.put(ImmortalCultivationMod.MODID + ":breakthrough_pill", 500);
        values.put(ImmortalCultivationMod.MODID + ":body_tempering_pill", 10_000);
        values.put(ImmortalCultivationMod.MODID + ":blood_qi_pill", 1_000);
        values.put(ImmortalCultivationMod.MODID + ":restriction_insight", 5_000);
        values.put(ImmortalCultivationMod.MODID + ":fog_revealing_mirror", 5_000);
        values.put(ImmortalCultivationMod.MODID + ":yinyang_compass", 7_000);
        values.put(ImmortalCultivationMod.MODID + ":zhenpan", 5_000);
        values.put(ImmortalCultivationMod.MODID + ":blank_formation_flag", 3_000);
        values.put(ImmortalCultivationMod.MODID + ":duration_flag", 3_000);
        values.put(ImmortalCultivationMod.MODID + ":amplifier_flag", 3_000);
        values.put(ImmortalCultivationMod.MODID + ":spirit_gathering_flag", 3_000);
        values.put(ImmortalCultivationMod.MODID + ":haste_flag", 3_000);
        values.put(ImmortalCultivationMod.MODID + ":qi_return_flag", 3_000);
        values.put(ImmortalCultivationMod.MODID + ":zhenshi_talisman", 1_280);
        values.put(ImmortalCultivationMod.MODID + ":zhenhun_bell", 2_000);
        values.put(ImmortalCultivationMod.MODID + ":yinhun_gong", 1_500);
        values.put(ImmortalCultivationMod.MODID + ":fengyan_talisman", 1_000);
        values.put(ImmortalCultivationMod.MODID + ":fireball_talisman", 1_000);
        values.put(ImmortalCultivationMod.MODID + ":blood", 10);
        values.put(ImmortalCultivationMod.MODID + ":blood_crystal", 100);
        values.put(ImmortalCultivationMod.MODID + ":nuomi", 5);
        values.put(ImmortalCultivationMod.MODID + ":nuomi_seed", 5);
        values.put(ImmortalCultivationMod.MODID + ":nuomi_zhou", 20);
        values.put(ImmortalCultivationMod.MODID + ":modou_line", 150);
        values.put(ImmortalCultivationMod.MODID + ":taomu_sword", 500);
        values.put(ImmortalCultivationMod.MODID + ":red_string", 10);
        values.put(ImmortalCultivationMod.MODID + ":copper_coin", 1);
        values.put(ImmortalCultivationMod.MODID + ":bigu_pill", 40);
        values.put(ImmortalCultivationMod.MODID + ":low_qi_recovery_pill", 80);
        values.put(ImmortalCultivationMod.MODID + ":middle_qi_recovery_pill", 600);
        values.put(ImmortalCultivationMod.MODID + ":high_qi_recovery_pill", 1_600);
        values.put(ImmortalCultivationMod.MODID + ":low_yangjian_stone", 80);
        values.put(ImmortalCultivationMod.MODID + ":middle_yangjian_stone", 800);
        values.put(ImmortalCultivationMod.MODID + ":high_yangjian_stone", 5_000);
        values.put(ImmortalCultivationMod.MODID + ":low_sword_grindstone", 80);
        values.put(ImmortalCultivationMod.MODID + ":middle_sword_grindstone", 800);
        values.put(ImmortalCultivationMod.MODID + ":high_sword_grindstone", 5_000);
        values.put(ImmortalCultivationMod.MODID + ":low_saber_grindstone", 80);
        values.put(ImmortalCultivationMod.MODID + ":middle_saber_grindstone", 800);
        values.put(ImmortalCultivationMod.MODID + ":high_saber_grindstone", 5_000);
        values.put(ImmortalCultivationMod.MODID + ":family_token", 5_000);
        values.put(ImmortalCultivationMod.MODID + ":royal_token", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":protective_jade_pendant", 25_000);
        values.put(ImmortalCultivationMod.MODID + ":lethal_poison", 50);
        values.put(ImmortalCultivationMod.MODID + ":wanted_order", 10);
        values.put(ImmortalCultivationMod.MODID + ":disguise_mask", 2_000);
        values.put(ImmortalCultivationMod.MODID + ":corpse_refining_secret", 600_000); // 九幽炼尸诀
        values.put(ImmortalCultivationMod.MODID + ":copper_coin_sword", 100);
        values.put(ImmortalCultivationMod.MODID + ":coffin", 5_000);
        values.put(ImmortalCultivationMod.MODID + ":flag", 1_000);
        values.put(ImmortalCultivationMod.MODID + ":xiujian", 2_500);
        values.put(ImmortalCultivationMod.MODID + ":blindfold", 250);
        values.put(ImmortalCultivationMod.MODID + ":blindfold2", 500);
        values.put(ImmortalCultivationMod.MODID + ":straw_hat", 100);
        values.put(ImmortalCultivationMod.MODID + ":jade_pendant", 50_000);
        values.put(ImmortalCultivationMod.MODID + ":daofu_chestplate", 500);
        values.put(ImmortalCultivationMod.MODID + ":flying_sword", 5_000);
        values.put(ImmortalCultivationMod.MODID + ":feijian_yujian", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":dao_yujian", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":jingtie_spear_yujian", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":heavy_iron_bracer_yujian", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":throwing_knife_yujian", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":flying_needle_yujian", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":zhenpan_yujian", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":blank_formation_flag_yujian", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":duration_flag_yujian", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":amplifier_flag_yujian", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":spirit_gathering_flag_yujian", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":haste_flag_yujian", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":qi_return_flag_yujian", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":dao", 5_000);
        values.put(ImmortalCultivationMod.MODID + ":jingtie_spear", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":throwing_knife", 3_000);
        values.put(ImmortalCultivationMod.MODID + ":flying_needle", 2_000);
        values.put(ImmortalCultivationMod.MODID + ":tiegu_shan", 5_000);
        values.put(ImmortalCultivationMod.MODID + ":fuchen", 3_000);
        values.put(ImmortalCultivationMod.MODID + ":whip", 3_000);
        values.put(ImmortalCultivationMod.MODID + ":heavy_iron_bracer", 5_000);
        values.put(ImmortalCultivationMod.MODID + ":sword_mold", 4_000);
        values.put(ImmortalCultivationMod.MODID + ":saber_mold", 4_000);
        values.put(ImmortalCultivationMod.MODID + ":spear_mold", 4_000);
        values.put(ImmortalCultivationMod.MODID + ":weapon_mold", 4_000);
        values.put(ImmortalCultivationMod.MODID + ":basic_breathing_method", 100); // 基础吐纳法
        values.put(ImmortalCultivationMod.MODID + ":clear_heart_method", 100); // 清心诀
        values.put(ImmortalCultivationMod.MODID + ":blood_demon_jindan_method", 12_800); // 血魔功（金丹）
        values.put(ImmortalCultivationMod.MODID + ":blood_demon_yuanying_method", 325_000); // 血魔功（元婴）
        values.put(ImmortalCultivationMod.MODID + ":blood_demon_huashen_method", 500_000); // 血魔功（化神）
        values.put(ImmortalCultivationMod.MODID + ":reincarnation_true_art_method", 750_000); // 轮回真决
        values.put(ImmortalCultivationMod.MODID + ":tuntian_demon_art_method", 750_000); // 吞天魔功
        values.put(ImmortalCultivationMod.MODID + ":pokong_jue_method", 22_800); // 破空诀
        values.put(ImmortalCultivationMod.MODID + ":changqing_jue_method", 12_800); // 长青诀
        values.put(ImmortalCultivationMod.MODID + ":fentian_life_renewal_method", 32_800); // 焚天续命功
        values.put(ImmortalCultivationMod.MODID + ":hanti_bingqin_method", 25_000); // 寒啼冰擒功
        values.put(ImmortalCultivationMod.MODID + ":cuigu_lianshen_method", 48_000); // 淬骨炼神诀
        values.put(ImmortalCultivationMod.MODID + ":lingjiao_scale", 500);
        values.put(ImmortalCultivationMod.MODID + ":shihunfan", 10_000);
        values.put(ImmortalCultivationMod.MODID + ":yuanhun_bottle", 1000);
        values.put(ImmortalCultivationMod.MODID + ":white_wool_strip", 1);
        values.put(ImmortalCultivationMod.MODID + ":orange_wool_strip", 1);
        values.put(ImmortalCultivationMod.MODID + ":magenta_wool_strip", 1);
        values.put(ImmortalCultivationMod.MODID + ":light_blue_wool_strip", 1);
        values.put(ImmortalCultivationMod.MODID + ":yellow_wool_strip", 1);
        values.put(ImmortalCultivationMod.MODID + ":lime_wool_strip", 1);
        values.put(ImmortalCultivationMod.MODID + ":pink_wool_strip", 1);
        values.put(ImmortalCultivationMod.MODID + ":gray_wool_strip", 1);
        values.put(ImmortalCultivationMod.MODID + ":light_gray_wool_strip", 1);
        values.put(ImmortalCultivationMod.MODID + ":cyan_wool_strip", 1);
        values.put(ImmortalCultivationMod.MODID + ":purple_wool_strip", 1);
        values.put(ImmortalCultivationMod.MODID + ":blue_wool_strip", 1);
        values.put(ImmortalCultivationMod.MODID + ":brown_wool_strip", 1);
        values.put(ImmortalCultivationMod.MODID + ":green_wool_strip", 1);
        values.put(ImmortalCultivationMod.MODID + ":red_wool_strip", 1);
        values.put(ImmortalCultivationMod.MODID + ":black_wool_strip", 1);
        values.put(ImmortalCultivationMod.MODID + ":neijia_chestplate", 500);
        values.put(ImmortalCultivationMod.MODID + ":zhongyi", 500);
        values.put(ImmortalCultivationMod.MODID + ":spirit_vein_center", 10000);

        values.put(ImmortalCultivationMod.MODID + ":flowing_spirit_crystal", 6000);
        values.put(ImmortalCultivationMod.MODID + ":lingmu_flagpole", 200);
        values.put(ImmortalCultivationMod.MODID + ":patterned_cloth", 500);
        values.put(ImmortalCultivationMod.MODID + ":ritual_dagger", 3000);
        values.put(ImmortalCultivationMod.MODID + ":spirit_guiding_thread", 500);
        values.put(ImmortalCultivationMod.MODID + ":spirit_map", 1000);
        values.put(ImmortalCultivationMod.MODID + ":spirit_silkworm_silk", 400);
        values.put(ImmortalCultivationMod.MODID + ":spirit_storage_crystal", 4000);
        values.put(ImmortalCultivationMod.MODID + ":zhenwen_jade", 500);
        values.put(ImmortalCultivationMod.MODID + ":dunwu_stele", 500000);
        values.put(ImmortalCultivationMod.MODID + ":lingmu", 300);
        values.put(ImmortalCultivationMod.MODID + ":lingmu_leaves", 10);
        values.put(ImmortalCultivationMod.MODID + ":lingmu_log", 100);
        values.put(ImmortalCultivationMod.MODID + ":restriction_table", 100);
        values.put(ImmortalCultivationMod.MODID + ":spirit_ore", 50);
        values.put(ImmortalCultivationMod.MODID + ":yuanying_yaodan", 300000);
        values.put(ImmortalCultivationMod.MODID + ":yanjiao_horn", 100000);
        values.put(ImmortalCultivationMod.MODID + ":yaoyuan_fist_bone", 300000);
        values.put(ImmortalCultivationMod.MODID + ":yaoyuan_bone", 20000);
        values.put(ImmortalCultivationMod.MODID + ":xuesha_hide", 40000);

        return Map.copyOf(values);
    }

    private static void putVanilla(
            Map<String, Integer> values,
            int price,
            String... paths) {
        for (String path : paths) {
            values.put("minecraft:" + path, price);
        }
    }

    private static int fallbackPrice(Item item) {
        if (item == Items.AIR) {
            return 0;
        }
        ItemStack stack = item.getDefaultInstance();
        int value = Math.max(1, CultivationItemRarity.giftValue(stack));
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id != null && "minecraft".equals(id.getNamespace())) {
            value = Math.max(1, value / 2);
        }
        return Mth.clamp(value, 1, MAX_PRICE);
    }

    private static long safeAdd(long left, long right) {
        if (left >= MAX_PRICE || right >= MAX_PRICE) {
            return MAX_PRICE;
        }
        return Math.min(MAX_PRICE, left + right);
    }

    private static long safeMultiply(long value, int count) {
        if (value <= 0L || count <= 0) {
            return 0L;
        }
        if (value > MAX_PRICE / (long) count) {
            return MAX_PRICE;
        }
        return value * count;
    }

    private static int ceilDivide(long value, int divisor) {
        if (value <= 0L || divisor <= 0) {
            return 0;
        }
        return Mth.clamp(
                (int) Math.min(
                        MAX_PRICE,
                        (value + divisor - 1L) / divisor),
                1,
                MAX_PRICE);
    }

    private record PriceState(
            RecipeManager manager,
            int recipeCount,
            Map<Item, Integer> prices) {
    }

    private static final class Resolver {
        private final MinecraftServer server;
        private final Map<Item, List<Recipe<?>>> recipesByOutput = new HashMap<>();
        private final Map<Item, Integer> resolved = new HashMap<>();
        private final Set<Item> resolving = new HashSet<>();

        private Resolver(MinecraftServer server, RecipeManager manager) {
            this.server = server;
            for (RecipeHolder<?> holder : manager.getRecipes()) {
                Recipe<?> recipe = holder.value();
                ItemStack output;
                try {
                    output = recipe.getResultItem(server.registryAccess());
                } catch (RuntimeException exception) {
                    continue;
                }
                if (output.isEmpty() || output.getCount() <= 0) {
                    continue;
                }
                recipesByOutput
                        .computeIfAbsent(output.getItem(), ignored -> new ArrayList<>())
                        .add(recipe);
            }
        }

        private int resolvePrice(Item item) {
            if (item == Items.AIR) {
                return 0;
            }
            Integer cached = resolved.get(item);
            if (cached != null) {
                return cached;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            Integer base = id == null ? null : BASE_PRICES.get(id.toString());
            if (base != null) {
                int value = Mth.clamp(base, 0, MAX_PRICE);
                resolved.put(item, value);
                return value;
            }
            if (!resolving.add(item)) {
                return 0;
            }
            int best = Integer.MAX_VALUE;
            for (Recipe<?> recipe : recipesByOutput.getOrDefault(item, List.of())) {
                int value = recipePrice(recipe);
                if (value > 0 && value < best) {
                    best = value;
                }
            }
            resolving.remove(item);
            int result = best == Integer.MAX_VALUE
                    ? fallbackPrice(item)
                    : Mth.clamp(best, 1, MAX_PRICE);
            resolved.put(item, result);
            return result;
        }

        private int recipePrice(Recipe<?> recipe) {
            ItemStack output;
            List<Ingredient> ingredients;
            try {
                output = recipe.getResultItem(server.registryAccess());
                ingredients = recipe.getIngredients();
            } catch (RuntimeException exception) {
                return 0;
            }
            if (output.isEmpty() || output.getCount() <= 0 || ingredients.isEmpty()) {
                return 0;
            }
            long total = 0L;
            int usedIngredients = 0;
            for (Ingredient ingredient : ingredients) {
                if (ingredient == null || ingredient.isEmpty()) {
                    continue;
                }
                long cheapest = Long.MAX_VALUE;
                for (ItemStack option : ingredient.getItems()) {
                    if (option.isEmpty()) {
                        continue;
                    }
                    int optionPrice = resolvePrice(option.getItem());
                    if (optionPrice <= 0) {
                        continue;
                    }
                    long candidate = safeMultiply(
                            optionPrice,
                            Math.max(1, option.getCount()));
                    if (candidate > 0L && candidate < cheapest) {
                        cheapest = candidate;
                    }
                }
                if (cheapest == Long.MAX_VALUE) {
                    return 0;
                }
                total = safeAdd(total, cheapest);
                usedIngredients++;
            }
            if (usedIngredients == 0 || total <= 0L) {
                return 0;
            }
            return ceilDivide(total, output.getCount());
        }
    }
}
