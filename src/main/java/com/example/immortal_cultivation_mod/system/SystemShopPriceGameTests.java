package com.example.immortal_cultivation_mod.system;

import com.example.immortal_cultivation_mod.alchemy.AlchemyChemistry;
import com.example.immortal_cultivation_mod.alchemy.AlchemyForgeRecipes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder("immortal_cultivation_system_prices")
@PrefixGameTestTemplate(false)
public final class SystemShopPriceGameTests {
    private static ItemStack stack(GameTestHelper helper, String path) {
        var id = ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", path);
        var item = BuiltInRegistries.ITEM.get(id);
        helper.assertTrue(item != Items.AIR, "Registered item: " + path);
        return item.getDefaultInstance();
    }

    private static int shopPrice(GameTestHelper helper, String path) {
        var item = stack(helper, path);
        String id = BuiltInRegistries.ITEM.getKey(item.getItem()).toString();
        var entry = SystemShop.catalog().stream().filter(e -> e.id().equals(id)).findFirst();
        helper.assertTrue(entry.isPresent(), "New item is purchasable: " + path);
        int price = SystemShopPrices.price(item);
        helper.assertTrue(entry.orElseThrow().price() == price, "Catalog uses actual price: " + path);
        helper.assertTrue(SystemShop.isSellable(item), "Default item can be recycled: " + path);
        helper.assertTrue(SystemShop.dailyStock(id, price, 1) != 0, "New item receives daily stock: " + path);
        return price;
    }

    @GameTest(template = "fate_test")
    public static void flamePricesFollowRealmAndAppearInShop(GameTestHelper helper) {
        SystemShopPrices.invalidate();
        int previous = shopPrice(helper, "flame_vessel");
        for (String id : List.of("crimson_flame_seed", "earth_flame_seed", "wood_flame_heart",
                "solar_flame_seed", "nether_flame_seed", "thunder_flame_seed", "dragon_flame_seed",
                "heavenly_flame_seed")) {
            int price = shopPrice(helper, id);
            helper.assertTrue(price > previous, "Stronger flame costs more: " + id);
            previous = price;
        }
        shopPrice(helper, "crimson_flame_seed_yujian");
        shopPrice(helper, "dragon_flame_seed_yujian");
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void beastMaterialsHaveUsableEconomicValues(GameTestHelper helper) {
        for (String path : List.of("spirit_devouring_rat_hide", "spirit_scent_whisker", "iron_scale_plate",
                "crimson_venom_sac", "scorpion_tail_stinger", "wind_feather", "spirit_deer_antler",
                "ink_armor_plate", "mineral_marrow", "profound_iron_armor_core", "ember_feather",
                "fire_owl_claw", "flame_radiance_bone", "xuanjin_seal")) {
            helper.assertTrue(shopPrice(helper, path) >= 100, "Materials use explicit value, not rarity fallback: " + path);
        }
        helper.assertTrue(shopPrice(helper, "profound_iron_armor_core") > shopPrice(helper, "ink_armor_plate"),
                "Rare armor core is worth more than common plate");
        helper.assertTrue(shopPrice(helper, "flame_radiance_bone") > shopPrice(helper, "ember_feather"),
                "Rare owl bone is worth more than common feathers");
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void generatedPillsAndEveryFormulaHavePrices(GameTestHelper helper) {
        for (var formula : AlchemyChemistry.FORMULAS) {
            String pill = formula.id().equals("huiling") ? "qi_recovery" : formula.id();
            int pillPrice = shopPrice(helper, pill + "_pill");
            helper.assertTrue(pillPrice >= 80, "Pill has explicit value: " + pill);
            helper.assertTrue(shopPrice(helper, formula.id() + "_formula") > pillPrice,
                    "Reusable formula costs more than one pill: " + formula.id());
        }
        for (String id : AlchemyForgeRecipes.LEGACY_REQUIRED.keySet()) {
            helper.assertTrue(shopPrice(helper, id + "_formula") > shopPrice(helper, id + "_pill"),
                    "Legacy formula also priced: " + id);
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void newWeaponsAndEquipmentReachCatalog(GameTestHelper helper) {
        for (String path : List.of("black_iron_jian", "black_iron_greatsword", "moon_crescent_halberd",
                "spirit_silk_whip", "soul_hook", "wind_feather_fan", "thunderwood_sword", "flame_pattern_dao",
                "earthbreaker_hammer", "seven_star_sword", "cu_tie_xiaoding", "chi_jin_sanyang_ding",
                "qingtong_yinhuo_ding_yujian", "chi_jin_sanyang_ding_yujian", "jade_pill_bottle",
                "jade_pill_bottle_yujian", "hair_styling_table", "cultivation_hairpin", "cloth_sheath",
                "sword_sheath", "dao_sheath", "medicine_cabinet_small", "medicine_cabinet_large")) {
            helper.assertTrue(shopPrice(helper, path) >= 100, "Equipment has explicit price: " + path);
        }
        helper.assertTrue(shopPrice(helper, "chi_jin_sanyang_ding") > shopPrice(helper, "qingtong_yinhuo_ding"),
                "Higher-grade ding costs more");
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void existingPricesAndShopRestrictionsRemain(GameTestHelper helper) {
        helper.assertTrue(shopPrice(helper, "qingtong_yinhuo_ding") == 20_000, "Copper ding price retained");
        helper.assertTrue(shopPrice(helper, "flying_sword") == 5_000, "Flying sword price retained");
        helper.assertTrue(shopPrice(helper, "qi_recovery_pill") == 80, "Recovery pill price retained");
        helper.assertTrue(SystemShopPrices.price(ItemStack.EMPTY) == 0, "Empty stack has no value");
        for (String path : List.of("system", "mysterious_ring", "debug_stat_editor", "vault_guardian_spawn_egg")) {
            String id = BuiltInRegistries.ITEM.getKey(stack(helper, path).getItem()).toString();
            helper.assertTrue(SystemShop.catalog().stream().noneMatch(e -> e.id().equals(id)),
                    "Excluded content remains unavailable: " + path);
        }
        helper.succeed();
    }
}
