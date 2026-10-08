package com.example.immortal_cultivation_mod.release;

import com.example.immortal_cultivation_mod.item.ForgingSystem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Checks the actual runtime after temporarily archiving the physical ship feature. */
@GameTestHolder("immortal_cultivation_content_availability")
@PrefixGameTestTemplate(false)
public final class ArchivedFlyingShipGameTests {
    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", path);
    }

    @GameTest(template = "fate_test")
    public static void shipAndAllItsComponentsAreAbsentFromLiveRegistries(GameTestHelper helper) {
        for (String path : new String[]{"lingzhou", "lingzhou_yujian", "lingzhou_core_low",
                "lingzhou_core_medium", "lingzhou_core_high", "lingzhou_core_sect", "lingzhou_helm",
                "lingzhou_core_low_yujian", "lingzhou_core_medium_yujian", "lingzhou_core_high_yujian",
                "lingzhou_core_sect_yujian"}) {
            helper.assertTrue(!BuiltInRegistries.ITEM.containsKey(id(path)), "Archived ship item is absent: " + path);
            helper.assertTrue(!BuiltInRegistries.BLOCK.containsKey(id(path)), "Archived ship block is absent: " + path);
        }
        helper.assertTrue(!BuiltInRegistries.ENTITY_TYPE.containsKey(id("lingzhou_ship")), "Ship entity is absent");
        helper.assertTrue(!BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(id("lingzhou_core")), "Core block entity is absent");
        helper.assertTrue(!BuiltInRegistries.MENU.containsKey(id("lingzhou_core")), "Core menu is absent");
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void shipRecipesAreUnavailableWhileOrdinaryForgingRemains(GameTestHelper helper) {
        for (String path : new String[]{"lingzhou", "lingzhou_core_low", "lingzhou_core_medium",
                "lingzhou_core_high", "lingzhou_core_sect"}) {
            helper.assertTrue(ForgingSystem.recipeById(path) == null, "Archived forging recipe is absent: " + path);
        }
        helper.assertTrue(ForgingSystem.recipeById("flying_sword") != null, "Flying swords remain forgeable");
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id("flame_vessel")), "Flame vessels remain available");
        helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(id("chi_jin_sanyang_ding")), "Three-Sun Ding remains available");
        helper.succeed();
    }
}
