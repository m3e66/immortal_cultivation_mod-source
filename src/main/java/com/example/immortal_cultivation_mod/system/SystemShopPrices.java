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
    private static final int MAX_PRICE = 1_000_000_000;
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
        values.put(ImmortalCultivationMod.MODID + ":lianqi_yaodan", 100);
        values.put(ImmortalCultivationMod.MODID + ":jindan_yaodan", 10_000);
        values.put(ImmortalCultivationMod.MODID + ":chiyan_bone", 9_000);
        values.put(ImmortalCultivationMod.MODID + ":xuanduan_iron_shard", 2_000);
        values.put(ImmortalCultivationMod.MODID + ":spirit_guiding_gear", 6_000);
        values.put(ImmortalCultivationMod.MODID + ":damaged_forging_furnace_core", 30_000);

        // Foundation Establishment - beast materials
        values.put(ImmortalCultivationMod.MODID + ":rock_badger_hide", 500);
        values.put(ImmortalCultivationMod.MODID + ":thunder_patterned_rock_plate", 800);
        values.put(ImmortalCultivationMod.MODID + ":zhuji_yaodan", 1_000);
        values.put(ImmortalCultivationMod.MODID + ":thunder_horn", 800);
        values.put(ImmortalCultivationMod.MODID + ":frostspine_hide", 650);
        // Beast meat scales by one major realm step (10x per tier).
        values.put(ImmortalCultivationMod.MODID + ":lianqi_beast_meat", 35);
        values.put(ImmortalCultivationMod.MODID + ":zhuji_beast_meat", 350);
        values.put(ImmortalCultivationMod.MODID + ":jindan_beast_meat", 3_500);
        values.put(ImmortalCultivationMod.MODID + ":yuanying_beast_meat", 35_000);
        values.put(ImmortalCultivationMod.MODID + ":huashen_beast_meat", 350_000);
        values.put(ImmortalCultivationMod.MODID + ":lianxu_beast_meat", 3_500_000);
        values.put(ImmortalCultivationMod.MODID + ":heti_beast_meat", 35_000_000);
        values.put(ImmortalCultivationMod.MODID + ":dacheng_beast_meat", 350_000_000);
        values.put(ImmortalCultivationMod.MODID + ":dujie_beast_meat", 1_000_000_000);
        values.put(ImmortalCultivationMod.MODID + ":cold_marrow", 900);


        // Qi Refining - Early
        values.put(ImmortalCultivationMod.MODID + ":beam_scroll", 100); // 光束术玉简
        values.put(ImmortalCultivationMod.MODID + ":cleanse_scroll", 100); // 清洁咒玉简
        values.put(ImmortalCultivationMod.MODID + ":fireball_scroll", 100); // 火球术玉简
        values.put(ImmortalCultivationMod.MODID + ":huti_qi_scroll", 500); // 护体气玉简
        values.put(ImmortalCultivationMod.MODID + ":ignite_flare_scroll", 150); // 燃火诀玉简
        values.put(ImmortalCultivationMod.MODID + ":lingzhi_bullet_scroll", 100); // 灵指弹玉简
        values.put(ImmortalCultivationMod.MODID + ":qi_gathering_scroll", 100); // 引灵诀玉简
        values.put(ImmortalCultivationMod.MODID + ":sliding_water_scroll", 800); // 滑水术玉简
        values.put(ImmortalCultivationMod.MODID + ":smoke_art_scroll", 300); // 迷烟术玉简
        values.put(ImmortalCultivationMod.MODID + ":spirit_sight_scroll", 250); // 灵视玉简
        values.put(ImmortalCultivationMod.MODID + ":wind_blade_scroll", 100); // 风刃玉简
        values.put(ImmortalCultivationMod.MODID + ":wind_step_scroll", 200); // 踏风步玉简

        // Qi Refining - Middle
        values.put(ImmortalCultivationMod.MODID + ":jinfeng_zhi_scroll", 400); // 金锋指玉简
        values.put(ImmortalCultivationMod.MODID + ":michen_zhang_scroll", 300); // 迷尘障玉简
        values.put(ImmortalCultivationMod.MODID + ":regeneration_scroll", 800); // 回春术玉简
        values.put(ImmortalCultivationMod.MODID + ":shuiren_lianzhan_scroll", 500); // 水刃连斩玉简

        // Qi Refining - Late
        values.put(ImmortalCultivationMod.MODID + ":dielang_shield_scroll", 1_500); // 叠浪盾玉简
        values.put(ImmortalCultivationMod.MODID + ":earth_escape_scroll", 500); // 遁地术玉简
        values.put(ImmortalCultivationMod.MODID + ":jingji_tusheng_scroll", 1_500); // 荆棘突生玉简
        values.put(ImmortalCultivationMod.MODID + ":kongshi_shu_scroll", 2_000); // 控尸术玉简
        values.put(ImmortalCultivationMod.MODID + ":lingbeng_scroll", 500); // 灵崩玉简
        values.put(ImmortalCultivationMod.MODID + ":pokong_jianhen_scroll", 2_500); // 破空剑痕玉简
        values.put(ImmortalCultivationMod.MODID + ":yaoguang_shan_scroll", 1_500); // 曜光闪玉简
        values.put(ImmortalCultivationMod.MODID + ":yinlei_jue_scroll", 1_500); // 引雷诀玉简
        values.put(ImmortalCultivationMod.MODID + ":yufeng_jue_scroll", 3_000); // 御风诀玉简

        // Qi Refining - Talismans (Tier One)
        values.put(ImmortalCultivationMod.MODID + ":fengren_talisman", 200); // 风刃符
        values.put(ImmortalCultivationMod.MODID + ":fireball_talisman", 500);
        values.put(ImmortalCultivationMod.MODID + ":liansheng_talisman", 300); // 敛声符
        values.put(ImmortalCultivationMod.MODID + ":qingchen_talisman", 100); // 清尘符
        values.put(ImmortalCultivationMod.MODID + ":weiguang_talisman", 100); // 微光符

        // Foundation Establishment - Early
        values.put(ImmortalCultivationMod.MODID + ":diyan_qiang_scroll", 2_000); // 地岩枪玉简
        values.put(ImmortalCultivationMod.MODID + ":gushi_shield_scroll", 3_000); // 固石盾玉简
        values.put(ImmortalCultivationMod.MODID + ":leishan_scroll", 3_000); // 雷闪玉简
        values.put(ImmortalCultivationMod.MODID + ":luohua_chanshen_scroll", 2_000); // 落花缠身玉简
        values.put(ImmortalCultivationMod.MODID + ":xuanyan_huti_scroll", 3_000); // 玄岩护体玉简
        values.put(ImmortalCultivationMod.MODID + ":yanbao_dan_scroll", 2_000); // 炎爆弹玉简
        values.put(ImmortalCultivationMod.MODID + ":yingxi_scroll", 2_800); // 影袭玉简

        // Foundation Establishment - Middle
        values.put(ImmortalCultivationMod.MODID + ":bingzhui_yu_scroll", 2_500); // 冰锥雨玉简
        values.put(ImmortalCultivationMod.MODID + ":liufeng_xiejin_scroll", 8_000); // 流风卸劲玉简
        values.put(ImmortalCultivationMod.MODID + ":muling_shengxi_scroll", 5_000); // 木灵生息玉简
        values.put(ImmortalCultivationMod.MODID + ":wulei_zhengfa_scroll", 3_000); // 五雷正法玉简
        values.put(ImmortalCultivationMod.MODID + ":xuying_ta_scroll", 4_500); // 虚影踏玉简

        // Foundation Establishment - Late
        values.put(ImmortalCultivationMod.MODID + ":canglang_chong_scroll", 7_000); // 沧浪冲玉简
        values.put(ImmortalCultivationMod.MODID + ":dingshen_scroll", 1_000); // 定身咒玉简
        values.put(ImmortalCultivationMod.MODID + ":duanliu_kongdun_scroll", 12_000); // 断流空遁玉简
        values.put(ImmortalCultivationMod.MODID + ":light_beam_attack_scroll", 2_000); // 光球术玉简
        values.put(ImmortalCultivationMod.MODID + ":sanjian_zhuiming_scroll", 8_000); // 三剑追命玉简
        values.put(ImmortalCultivationMod.MODID + ":shengxi_baofa_scroll", 9_000); // 生息爆发玉简
        values.put(ImmortalCultivationMod.MODID + ":sifang_jie_scroll", 8_000); // 四方界玉简
        values.put(ImmortalCultivationMod.MODID + ":xue_guiyuan_shu_scroll", 10_000); // 血归元术玉简
        values.put(ImmortalCultivationMod.MODID + ":yihen_ci_scroll", 8_000); // 一痕刺玉简
        values.put(ImmortalCultivationMod.MODID + ":basic_breathing_method", 100); // 基础吐纳法
        values.put(ImmortalCultivationMod.MODID + ":clear_heart_method", 100); // 清心诀

        // Foundation Establishment - Talismans (Tier Two)
        values.put(ImmortalCultivationMod.MODID + ":bojin_hufu", 6_000); // 薄金护符
        values.put(ImmortalCultivationMod.MODID + ":chansi_kun_talisman", 6_000); // 缠丝困符
        values.put(ImmortalCultivationMod.MODID + ":chuanyin_talisman", 4_000); // 传音符
        values.put(ImmortalCultivationMod.MODID + ":duantu_shenxing_talisman", 3_000); // 短途神行符
        values.put(ImmortalCultivationMod.MODID + ":fengyan_talisman", 3_000);
        values.put(ImmortalCultivationMod.MODID + ":xinghuo_talisman", 3_000); // 星火符
        values.put(ImmortalCultivationMod.MODID + ":xiufu_talisman", 5_000); // 修复符
        values.put(ImmortalCultivationMod.MODID + ":zhenshi_talisman", 3_000);

        // Golden Core - Early
        values.put(ImmortalCultivationMod.MODID + ":jinmang_chuansha_scroll", 9_000); // 金芒穿杀玉简
        values.put(ImmortalCultivationMod.MODID + ":liuguang_jianying_scroll", 10_000); // 流光剑影玉简
        values.put(ImmortalCultivationMod.MODID + ":wanteng_jiaosha_scroll", 8_000); // 万藤绞杀玉简
        values.put(ImmortalCultivationMod.MODID + ":xuesha_hushen_scroll", 18_000); // 血煞护身玉简
        values.put(ImmortalCultivationMod.MODID + ":zhenshan_palm_scroll", 12_000); // 震山掌玉简

        // Golden Core - Middle
        values.put(ImmortalCultivationMod.MODID + ":baihua_zang_scroll", 13_000); // 百花葬玉简
        values.put(ImmortalCultivationMod.MODID + ":chiyan_longjuan_scroll", 15_000); // 赤炎龙卷玉简
        values.put(ImmortalCultivationMod.MODID + ":hanyu_fengdi_scroll", 15_000); // 寒狱封地玉简
        values.put(ImmortalCultivationMod.MODID + ":jinhuo_xinliao_scroll", 25_000); // 烬火心燎玉简
        values.put(ImmortalCultivationMod.MODID + ":suigui_xingyun_scroll", 18_000); // 随轨行云玉简
        values.put(ImmortalCultivationMod.MODID + ":tianfeng_zhui_scroll", 18_000); // 天风坠玉简
        values.put(ImmortalCultivationMod.MODID + ":wanren_chuanxin_scroll", 16_000); // 万刃穿心玉简

        // Golden Core - Late
        values.put(ImmortalCultivationMod.MODID + ":jingling_fanzhao_scroll", 30_000); // 镜灵返照玉简
        values.put(ImmortalCultivationMod.MODID + ":shanbeng_yin_scroll", 25_000); // 山崩印玉简
        values.put(ImmortalCultivationMod.MODID + ":shixu_chiliu_scroll", 25_000); // 时序滞流玉简
        values.put(ImmortalCultivationMod.MODID + ":tianguang_guansha_scroll", 35_000); // 天光贯杀玉简
        values.put(ImmortalCultivationMod.MODID + ":tianyun_fencheng_scroll", 35_000); // 天陨焚城玉简
        values.put(ImmortalCultivationMod.MODID + ":wulei_hongding_scroll", 20_000); // 五雷轰顶玉简
        values.put(ImmortalCultivationMod.MODID + ":blood_demon_jindan_method", 32_000); // 血魔功（金丹）
        values.put(ImmortalCultivationMod.MODID + ":changqing_jue_method", 32_000); // 长青诀
        values.put(ImmortalCultivationMod.MODID + ":fentian_life_renewal_method", 52_000); // 焚天续命功
        values.put(ImmortalCultivationMod.MODID + ":pokong_jue_method", 40_000); // 破空诀

        // Golden Core - Talismans (Tier Three)
        values.put(ImmortalCultivationMod.MODID + ":baofeng_talisman", 20_000); // 爆风符
        values.put(ImmortalCultivationMod.MODID + ":biaoji_talisman", 15_000); // 标记符
        values.put(ImmortalCultivationMod.MODID + ":bixie_anhun_talisman", 30_000); // 避邪安魂符
        values.put(ImmortalCultivationMod.MODID + ":dunkong_talisman", 28_000); // 遁空符
        values.put(ImmortalCultivationMod.MODID + ":huabing_talisman", 10_000); // 化兵符
        values.put(ImmortalCultivationMod.MODID + ":huoyu_fenxie_talisman", 25_000); // 火狱焚邪符
        values.put(ImmortalCultivationMod.MODID + ":wulei_xiaofa_talisman", 20_000); // 五雷小伐符
        values.put(ImmortalCultivationMod.MODID + ":zhenrao_talisman", 15_000); // 镇扰符

        // Nascent Soul - Early
        values.put(ImmortalCultivationMod.MODID + ":yuangang_hujie_scroll", 225_000); // 元罡护界玉简

        // Nascent Soul - Late
        values.put(ImmortalCultivationMod.MODID + ":blood_demon_yuanying_method", 325_000); // 血魔功（元婴）
        values.put(ImmortalCultivationMod.MODID + ":gangting_xiaokong_method", 525_000); // 罡霆啸空录
        values.put(ImmortalCultivationMod.MODID + ":hanlan_ningyuan_method", 525_000); // 寒澜凝渊经

        // Nascent Soul - Talismans (Tier Four)
        values.put(ImmortalCultivationMod.MODID + ":fengling_talisman", 325_000); // 封灵符
        values.put(ImmortalCultivationMod.MODID + ":jinguang_huti_talisman", 380_000); // 金光护体符
        values.put(ImmortalCultivationMod.MODID + ":pozhang_kaishan_talisman", 420_000); // 破障开山符

        // Soul Formation - Early
        values.put(ImmortalCultivationMod.MODID + ":cuigu_lianshen_method", 48_000); // 淬骨炼神诀
        values.put(ImmortalCultivationMod.MODID + ":xuanyue_suifeng_method", 65_000); // 玄岳碎锋诀

        // Soul Formation - Late
        values.put(ImmortalCultivationMod.MODID + ":blood_demon_huashen_method", 500_000); // 血魔功（化神）
        values.put(ImmortalCultivationMod.MODID + ":dari_bati_method", 950_000); // 大日霸体诀
        values.put(ImmortalCultivationMod.MODID + ":hanti_bingqin_method", 550_000); // 寒啼冰擒功
        values.put(ImmortalCultivationMod.MODID + ":reincarnation_true_art_method", 750_000); // 轮回真决
        values.put(ImmortalCultivationMod.MODID + ":tuntian_demon_art_method", 750_000); // 吞天魔功

        // Soul Formation - Talismans (Tier Five)
        values.put(ImmortalCultivationMod.MODID + ":qianli_chuanxun_talisman", 550_000); // 千里传讯符

        // Void Refinement - Talismans (Tier Six)
        values.put(ImmortalCultivationMod.MODID + ":wuyue_bie_talisman", 1_000_000); // 五岳辟厄符
        values.put(ImmortalCultivationMod.MODID + ":xuanyan_fenjie_talisman", 1_500_000); // 玄焰焚界符

        // Ancient Talisman
        values.put(ImmortalCultivationMod.MODID + ":jinkong_talisman", 150_000_000); // 禁空符

        values.put(ImmortalCultivationMod.MODID + ":ordinary_yellow_talisman_paper", 10); // 普通黄符纸
        values.put(ImmortalCultivationMod.MODID + ":lingsang_talisman_paper", 100); // 灵桑符纸
        values.put(ImmortalCultivationMod.MODID + ":red_veined_mulberry_paper", 1_000); // 赤纹桑皮纸
        values.put(ImmortalCultivationMod.MODID + ":jade_talisman_paper", 10_000); // 玉笺符纸
        values.put(ImmortalCultivationMod.MODID + ":celestial_silkworm_talisman_paper", 100_000); // 天蚕灵笺
        values.put(ImmortalCultivationMod.MODID + ":mundane_cinnabar_ink", 20); // 凡朱墨
        values.put(ImmortalCultivationMod.MODID + ":spiritual_cinnabar_ink", 200); // 灵朱墨
        values.put(ImmortalCultivationMod.MODID + ":mysterious_spirit_ink", 2_000); // 玄灵墨
        values.put(ImmortalCultivationMod.MODID + ":gold_veined_spirit_ink", 20_000); // 金纹灵墨
        values.put(ImmortalCultivationMod.MODID + ":jade_marrow_talisman_ink", 200_000); // 玉髓符墨
        values.put(ImmortalCultivationMod.MODID + ":spirit_marrow_treasure_ink", 1_000_000); // 灵髓宝墨
        values.put(ImmortalCultivationMod.MODID + ":heavenly_spirit_mystic_ink", 5_000_000); // 天灵玄墨
        values.put(ImmortalCultivationMod.MODID + ":dao_marrow_ink", 20_000_000); // 道髓墨

        values.put(ImmortalCultivationMod.MODID + ":enlightenment_pill", 500);
        values.put(ImmortalCultivationMod.MODID + ":breakthrough_pill", 500);
        values.put(ImmortalCultivationMod.MODID + ":body_tempering_pill", 10_000);
        values.put(ImmortalCultivationMod.MODID + ":blood_qi_pill", 1_000);
        values.put(ImmortalCultivationMod.MODID + ":restriction_insight", 5_000);
        values.put(ImmortalCultivationMod.MODID + ":fog_revealing_mirror", 7_500);
        values.put(ImmortalCultivationMod.MODID + ":yinyang_compass", 8_500);
        values.put(ImmortalCultivationMod.MODID + ":zhenpan", 5_000);
        values.put(ImmortalCultivationMod.MODID + ":blank_formation_flag", 3_000);
        values.put(ImmortalCultivationMod.MODID + ":duration_flag", 3_000);
        values.put(ImmortalCultivationMod.MODID + ":amplifier_flag", 3_000);
        values.put(ImmortalCultivationMod.MODID + ":spirit_gathering_flag", 3_000);
        values.put(ImmortalCultivationMod.MODID + ":haste_flag", 3_000);
        values.put(ImmortalCultivationMod.MODID + ":qi_return_flag", 3_000);
        values.put(ImmortalCultivationMod.MODID + ":zhenhun_bell", 2_600);
        values.put(ImmortalCultivationMod.MODID + ":yinhun_gong", 2_000);
        values.put(ImmortalCultivationMod.MODID + ":blood", 10);
        values.put(ImmortalCultivationMod.MODID + ":blood_crystal", 100);
        values.put(ImmortalCultivationMod.MODID + ":ashes", 1);
        values.put(ImmortalCultivationMod.MODID + ":nuomi", 5);
        values.put(ImmortalCultivationMod.MODID + ":nuomi_seed", 5);
        values.put(ImmortalCultivationMod.MODID + ":nuomi_zhou", 20);
        values.put(ImmortalCultivationMod.MODID + ":modou_line", 150);
        values.put(ImmortalCultivationMod.MODID + ":taomu_sword", 500);
        values.put(ImmortalCultivationMod.MODID + ":red_string", 10);
        values.put(ImmortalCultivationMod.MODID + ":copper_coin", 1);

        // Mortal inn food (priced directly in copper coins by mortal NPCs)
        values.put(ImmortalCultivationMod.MODID + ":steamed_bun", 2);
        values.put(ImmortalCultivationMod.MODID + ":wine", 4);
        values.put(ImmortalCultivationMod.MODID + ":noodle_bowl", 5);
        values.put(ImmortalCultivationMod.MODID + ":braised_pork", 7);
        values.put(ImmortalCultivationMod.MODID + ":roast_duck", 8);

        values.put(ImmortalCultivationMod.MODID + ":bigu_pill", 40);
        values.put(ImmortalCultivationMod.MODID + ":qi_recovery_pill", 80);
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
        values.put(ImmortalCultivationMod.MODID + ":black_tortoise_heartguard_mirror", 35_000);
        values.put(ImmortalCultivationMod.MODID + ":water_avoiding_pearl", 2_000);
        values.put(ImmortalCultivationMod.MODID + ":soul_anchor_pearl", 25_000);
        values.put(ImmortalCultivationMod.MODID + ":tribulation_doll", 15_000);
        values.put(ImmortalCultivationMod.MODID + ":fortune_mirror", 18_000);
        values.put(ImmortalCultivationMod.MODID + ":precise_fortune_mirror", 40_000);
        values.put(ImmortalCultivationMod.MODID + ":fortune_guard_coin", 14_000);
        values.put(ImmortalCultivationMod.MODID + ":fortune_lamp", 45_000);
        values.put(ImmortalCultivationMod.MODID + ":fortune_mirror_yujian", 20_000);
        values.put(ImmortalCultivationMod.MODID + ":precise_fortune_mirror_yujian", 65_000);
        values.put(ImmortalCultivationMod.MODID + ":fortune_guard_coin_yujian", 22_000);
        values.put(ImmortalCultivationMod.MODID + ":fortune_lamp_yujian", 40_000);
        values.put(ImmortalCultivationMod.MODID + ":purified_tribulation_doll", 25_000);
        values.put(ImmortalCultivationMod.MODID + ":tribulation_doll_yujian", 35_000);
        values.put(ImmortalCultivationMod.MODID + ":purified_tribulation_doll_yujian", 45_000);

        values.put(ImmortalCultivationMod.MODID + ":shield_breaker_ring", 15_000);
        values.put(ImmortalCultivationMod.MODID + ":blood_fang_pendant", 12_000);
        values.put(ImmortalCultivationMod.MODID + ":enlightenment_leaf", 50_000);
        values.put(ImmortalCultivationMod.MODID + ":lethal_poison", 50);
        values.put(ImmortalCultivationMod.MODID + ":wanted_order", 10);
        values.put(ImmortalCultivationMod.MODID + ":disguise_mask", 2_000);
        values.put(ImmortalCultivationMod.MODID + ":corpse_refining_secret", 600_000);
        values.put(ImmortalCultivationMod.MODID + ":copper_coin_sword", 100);
        values.put(ImmortalCultivationMod.MODID + ":coffin", 5_000);
        values.put(ImmortalCultivationMod.MODID + ":flag", 1_000);
        values.put(ImmortalCultivationMod.MODID + ":sword_case", 10_000);
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
        values.put(ImmortalCultivationMod.MODID + ":tiegu_shan_yujian", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":fuchen_yujian", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":whip_yujian", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":black_iron_jian_yujian", 32000);
        values.put(ImmortalCultivationMod.MODID + ":black_iron_greatsword_yujian", 32000);
        values.put(ImmortalCultivationMod.MODID + ":moon_crescent_halberd_yujian", 32000);
        values.put(ImmortalCultivationMod.MODID + ":spirit_silk_whip_yujian", 32000);
        values.put(ImmortalCultivationMod.MODID + ":soul_hook_yujian", 56000);
        values.put(ImmortalCultivationMod.MODID + ":wind_feather_fan_yujian", 56000);
        values.put(ImmortalCultivationMod.MODID + ":thunderwood_sword_yujian", 56000);
        values.put(ImmortalCultivationMod.MODID + ":flame_pattern_dao_yujian", 56000);
        values.put(ImmortalCultivationMod.MODID + ":earthbreaker_hammer_yujian", 56000);
        values.put(ImmortalCultivationMod.MODID + ":seven_star_sword_yujian", 180000);
        values.put(ImmortalCultivationMod.MODID + ":shihunfan_yujian", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":blood_fang_pendant_yujian", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":water_avoiding_pearl_yujian", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":jade_pendant_yujian", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":shield_breaker_ring_yujian", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":disguise_mask_yujian", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":black_tortoise_heartguard_mirror_yujian", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":zhenpan_yujian", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":blank_formation_flag_yujian", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":duration_flag_yujian", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":amplifier_flag_yujian", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":spirit_gathering_flag_yujian", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":haste_flag_yujian", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":qi_return_flag_yujian", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":sword_case_yujian", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":zhenhun_bell_yujian", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":yinhun_gong_yujian", 8_000);
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
        values.put(ImmortalCultivationMod.MODID + ":lingjiao_scale", 500);
        values.put(ImmortalCultivationMod.MODID + ":shihunfan", 10_000);
        values.put(ImmortalCultivationMod.MODID + ":yuanhun_bottle", 1_800);
        values.put(ImmortalCultivationMod.MODID + ":fog_revealing_mirror_yujian", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":yinyang_compass_yujian", 8_000);
        values.put(ImmortalCultivationMod.MODID + ":yuanhun_bottle_yujian", 8_000);
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
        values.put(ImmortalCultivationMod.MODID + ":spirit_vein_center", 100_000);
        values.put(ImmortalCultivationMod.MODID + ":qi_pouch", 10_000);


        values.put(ImmortalCultivationMod.MODID + ":flowing_spirit_crystal", 6000);
        values.put(ImmortalCultivationMod.MODID + ":lingmu_flagpole", 200);
        values.put(ImmortalCultivationMod.MODID + ":patterned_cloth", 500);
        values.put(ImmortalCultivationMod.MODID + ":ritual_dagger", 3_000);
        values.put(ImmortalCultivationMod.MODID + ":spirit_guiding_thread", 500);
        values.put(ImmortalCultivationMod.MODID + ":spirit_map", 1_000);
        values.put(ImmortalCultivationMod.MODID + ":spirit_silkworm_silk", 400);
        values.put(ImmortalCultivationMod.MODID + ":spirit_storage_crystal", 4_000);
        values.put(ImmortalCultivationMod.MODID + ":zhenwen_jade", 500);
        values.put(ImmortalCultivationMod.MODID + ":dunwu_stele", 500_000);
        values.put(ImmortalCultivationMod.MODID + ":lingmu", 300);
        values.put(ImmortalCultivationMod.MODID + ":lingmu_leaves", 10);
        values.put(ImmortalCultivationMod.MODID + ":lingmu_log", 100);
        values.put(ImmortalCultivationMod.MODID + ":restriction_table", 100);
        values.put(ImmortalCultivationMod.MODID + ":spirit_ore", 50);
        values.put(ImmortalCultivationMod.MODID + ":yuanying_yaodan", 300_000);
        values.put(ImmortalCultivationMod.MODID + ":huashen_yaodan", 3_000_000);
        values.put(ImmortalCultivationMod.MODID + ":lianxu_yaodan", 30_000_000);
        values.put(ImmortalCultivationMod.MODID + ":heti_yaodan", 300_000_000);
        values.put(ImmortalCultivationMod.MODID + ":dacheng_yaodan", 1_000_000_000);
        values.put(ImmortalCultivationMod.MODID + ":dujie_yaodan", 1_000_000_000);
        values.put(ImmortalCultivationMod.MODID + ":yanjiao_horn", 100_000);
        values.put(ImmortalCultivationMod.MODID + ":yaoyuan_fist_bone", 300_000);
        values.put(ImmortalCultivationMod.MODID + ":yaoyuan_bone", 20_000);
        values.put(ImmortalCultivationMod.MODID + ":xuesha_hide", 40_000);
        values.put(ImmortalCultivationMod.MODID + ":phantom_veil_fur", 60_000);
        values.put(ImmortalCultivationMod.MODID + ":kongming_stone", 5_000); // 空冥石
        values.put(ImmortalCultivationMod.MODID + ":lingmu_clasp", 500); // 灵木扣
        values.put(ImmortalCultivationMod.MODID + ":qi_pouch_yujian", 8_000); // 储物袋炼器玉简
        // Formula prices are 130% of their matching talisman price.
        // 一阶符方
        putTalismanFormulaPrice(values, "qingchen_talisman");
        putTalismanFormulaPrice(values, "weiguang_talisman");
        putTalismanFormulaPrice(values, "liansheng_talisman");
        putTalismanFormulaPrice(values, "fireball_talisman");
        putTalismanFormulaPrice(values, "fengren_talisman");
        // 二阶符方
        putTalismanFormulaPrice(values, "xinghuo_talisman");
        putTalismanFormulaPrice(values, "bojin_hufu");
        putTalismanFormulaPrice(values, "chansi_kun_talisman");
        putTalismanFormulaPrice(values, "duantu_shenxing_talisman");
        putTalismanFormulaPrice(values, "chuanyin_talisman");
        putTalismanFormulaPrice(values, "zhenshi_talisman");
        putTalismanFormulaPrice(values, "fengyan_talisman");
        putTalismanFormulaPrice(values, "xiufu_talisman");
        // 三阶符方
        putTalismanFormulaPrice(values, "bixie_anhun_talisman");
        putTalismanFormulaPrice(values, "zhenrao_talisman");
        putTalismanFormulaPrice(values, "baofeng_talisman");
        putTalismanFormulaPrice(values, "huoyu_fenxie_talisman");
        putTalismanFormulaPrice(values, "wulei_xiaofa_talisman");
        putTalismanFormulaPrice(values, "biaoji_talisman");
        putTalismanFormulaPrice(values, "dunkong_talisman");
        putTalismanFormulaPrice(values, "huabing_talisman");
        // 四阶符方
        putTalismanFormulaPrice(values, "jinguang_huti_talisman");
        putTalismanFormulaPrice(values, "fengling_talisman");
        putTalismanFormulaPrice(values, "pozhang_kaishan_talisman");
        // 五阶符方
        putTalismanFormulaPrice(values, "qianli_chuanxun_talisman");
        // 六阶与古符符方
        putTalismanFormulaPrice(values, "xuanyan_fenjie_talisman");
        putTalismanFormulaPrice(values, "wuyue_bie_talisman");
        putTalismanFormulaPrice(values, "jinkong_talisman");
        values.put(ImmortalCultivationMod.MODID + ":talisman_drawing_table", 1_000); // 画符台
        values.put(ImmortalCultivationMod.MODID + ":void_stone", 5); // 虚空石

        // New cultivation content: explicit prices instead of rarity-only fallback.
        putCultivation(values, 1_000, "flame_vessel");
        putCultivation(values, 20_000, "crimson_flame_seed");
        putCultivation(values, 40_000, "earth_flame_seed");
        putCultivation(values, 100_000, "wood_flame_heart");
        putCultivation(values, 200_000, "solar_flame_seed");
        putCultivation(values, 350_000, "nether_flame_seed");
        putCultivation(values, 1_000_000, "thunder_flame_seed");
        putCultivation(values, 2_000_000, "dragon_flame_seed");
        putCultivation(values, 5_000_000, "heavenly_flame_seed");
        putCultivation(values, 8_000, "crimson_flame_seed_yujian");
        putCultivation(values, 180_000, "dragon_flame_seed_yujian");

        putCultivation(values, 5, "mortal_beast_meat");
        putCultivation(values, 300, "weapon_shard");
        putCultivation(values, 100, "spirit_devouring_rat_hide");
        putCultivation(values, 350, "spirit_scent_whisker");
        putCultivation(values, 200, "iron_scale_plate");
        putCultivation(values, 300, "crimson_venom_sac");
        putCultivation(values, 900, "scorpion_tail_stinger");
        putCultivation(values, 1_200, "wind_feather", "ink_armor_plate", "mineral_marrow");
        putCultivation(values, 3_000, "spirit_deer_antler");
        putCultivation(values, 8_000, "profound_iron_armor_core");
        putCultivation(values, 10_000, "ember_feather");
        putCultivation(values, 20_000, "fire_owl_claw");
        putCultivation(values, 50_000, "flame_radiance_bone");
        putCultivation(values, 300_000, "xuanjin_seal");

        putCultivation(values, 16_000, "black_iron_jian", "black_iron_greatsword",
                "moon_crescent_halberd", "spirit_silk_whip");
        putCultivation(values, 40_000, "soul_hook", "wind_feather_fan", "thunderwood_sword",
                "flame_pattern_dao", "earthbreaker_hammer");
        putCultivation(values, 100_000, "seven_star_sword");
        putCultivation(values, 150, "cloth_sheath");
        putCultivation(values, 800, "sword_sheath", "dao_sheath");
        putCultivation(values, 500, "coin_mask", "hufu", "hechang", "fugu_tai");
        putCultivation(values, 300, "shenyi", "doupeng", "yunjian", "fangkou_qiaojian_lv");
        putCultivation(values, 3_000, "cultivation_fapao");
        putCultivation(values, 5_000, "yuanling_pao");
        putCultivation(values, 200, "miansha", "jiangpa", "futou", "wusha_hat", "song_wusha_hat",
                "chanfu_jin", "embroidered_forehead_band", "veiled_douli");
        for (var style : com.example.immortal_cultivation_mod.hair.HairStyle.values()) {
            putCultivation(values, 250, style.id());
        }
        putCultivation(values, 100, "hair_ribbon", "decorative_hair_beads");
        putCultivation(values, 400, "cultivation_hairpin", "jade_hair_ornament");
        putCultivation(values, 800, "hair_crown", "hair_styling_table");

        putCultivation(values, 1_500, "cu_tie_xiaoding");
        putCultivation(values, 150_000, "chi_jin_sanyang_ding");
        putCultivation(values, 20_000, "qingtong_yinhuo_ding_yujian");
        putCultivation(values, 200_000, "chi_jin_sanyang_ding_yujian");
        putCultivation(values, 400, "medicine_cabinet_small");
        putCultivation(values, 1_200, "medicine_cabinet_large");
        putCultivation(values, 100, "huanxia_si", "liujin_sand");
        putCultivation(values, 300, "jade_pill_bottle");
        putCultivation(values, 3_000, "jade_pill_bottle_yujian");
        putCultivation(values, 30, "lingquan_leaf");
        putCultivation(values, 80, "ningling_flower", "harmonizing_herb", "catalyst_blossom");

        putCultivation(values, 1600, "qingling_pill", "bishui_pill");
        putCultivation(values, 1200, "huichun_pill");
        putCultivation(values, 1500, "chiyan_pill");
        putCultivation(values, 2000, "bingxin_pill");
        putCultivation(values, 3000, "guyuan_pill", "bixie_pill");
        putCultivation(values, 6000, "yangshen_pill");
        putCultivation(values, 8000, "juling_pill");
        putCultivation(values, 1000, "mingmu_pill");
        putCultivation(values, 4000, "dingshen_pill", "tiegu_pill");
        putCultivation(values, 3500, "pozhang_pill");
        putCultivation(values, 5000, "xumai_pill");
        putCultivation(values, 2500, "ningqi_pill");
        putCultivation(values, 3000, "qingdu_pill");
        for (String pill : List.of("bigu", "body_tempering", "blood_qi", "enlightenment", "breakthrough",
                "qingling", "huichun", "chiyan", "bingxin", "guyuan", "yangshen", "juling", "huiling",
                "mingmu", "dingshen", "bishui", "pozhang", "xumai", "tiegu", "bixie", "ningqi", "qingdu")) {
            String base = pill.equals("huiling") ? "qi_recovery" : pill;
            putCultivation(values, values.get(ImmortalCultivationMod.MODID + ":" + base + "_pill") * 10,
                    pill + "_formula");
        }
        putCultivation(values, 100, "jietian_duoyun_shu_scroll");
        putCultivation(values, 100, "bijie_shu_scroll");
        putCultivation(values, 100, "guanyun_shu_scroll");
        putCultivation(values, 100, "yinfu_shu_scroll");
        putCultivation(values, 100, "bihuo_jue_scroll");

        return Map.copyOf(values);
    }

    private static void putCultivation(Map<String, Integer> values, int price, String... paths) {
        for (String path : paths) {
            values.put(ImmortalCultivationMod.MODID + ":" + path, price);
        }
    }

    private static void putVanilla(
            Map<String, Integer> values,
            int price,
            String... paths) {
        for (String path : paths) {
            values.put("minecraft:" + path, price);
        }
    }

    private static void putTalismanFormulaPrice(
            Map<String, Integer> values, String talismanPath) {
        String talismanId = ImmortalCultivationMod.MODID + ":" + talismanPath;
        Integer talismanPrice = values.get(talismanId);
        if (talismanPrice == null) {
            throw new IllegalStateException("Missing talisman price for " + talismanId);
        }
        int formulaPrice = Math.toIntExact(
                (talismanPrice.longValue() * 13L + 9L) / 10L);
        values.put(talismanId + "_formula", formulaPrice);
    }

    private static int fallbackPrice(Item item) {
        if (item == Items.AIR) {
            return 0;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        Integer base = id == null ? null : BASE_PRICES.get(id.toString());
        if (base != null) {
            return Mth.clamp(base, 0, MAX_PRICE);
        }
        ItemStack stack = item.getDefaultInstance();
        int value = Math.max(1, CultivationItemRarity.giftValue(stack));
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
