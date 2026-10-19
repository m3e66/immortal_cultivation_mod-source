package com.example.immortal_cultivation_mod.compat.jei;

import com.example.immortal_cultivation_mod.entity.BeastSpawnRules;
import com.example.immortal_cultivation_mod.flame.FlameVesselItem;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_jei_acquisition")
@PrefixGameTestTemplate(false)
public final class AcquisitionCatalogGameTests {
    @GameTest(template = "fate_test", timeoutTicks = 100)
    public static void shippedCatalogCoversEveryWildBeastOresPlantsAndChestSources(GameTestHelper h) {
        var recipes = AcquisitionCatalog.collect();
        h.assertTrue(recipes.size() > 250, "Shipped loot and bonus tables generate a substantial acquisition catalog");
        h.assertTrue(new HashSet<>(recipes.stream().map(AcquisitionRecipe::id).toList()).size() == recipes.size(),
                "Every acquisition recipe has a unique registry ID");
        for (var entity : BeastSpawnRules.rules().keySet()) {
            for (int realm : com.example.immortal_cultivation_mod.entity.BeastRealms.variants(entity)) {
                h.assertTrue(recipes.stream().anyMatch(recipe -> recipe.kind() == AcquisitionRecipe.Kind.MOB
                                && recipe.sourceIcon().is(net.minecraft.world.item.SpawnEggItem.byId(entity))
                                && com.example.immortal_cultivation_mod.item.BeastSpawnEggItem.selectedRealm(recipe.sourceIcon()) == realm),
                        "Every wild beast variant has JEI drop sources: " + BuiltInRegistries.ENTITY_TYPE.getKey(entity) + " realm=" + realm);
            }
        }
        h.assertTrue(recipes.stream().anyMatch(recipe -> recipe.kind() == AcquisitionRecipe.Kind.BLOCK
                        && recipe.result().is(com.example.immortal_cultivation_mod.magicritual.item.ModItems.SPIRIT_STONE.get())),
                "Spirit stones can be looked up as ore drops");
        h.assertTrue(recipes.stream().anyMatch(recipe -> recipe.kind() == AcquisitionRecipe.Kind.BLOCK
                        && BuiltInRegistries.ITEM.getKey(recipe.result().getItem()).getPath().equals("nuomi")),
                "Plant harvests are registered alongside ores");
        h.assertTrue(recipes.stream().anyMatch(recipe -> recipe.kind() == AcquisitionRecipe.Kind.CHEST
                        && recipe.result().is(ModItems.ENLIGHTENMENT_PILL.get())), "Cave chest acquisitions include the enlightenment pill");
        h.assertTrue(recipes.stream().noneMatch(recipe -> recipe.result().isEmpty() || recipe.sourceIcon().isEmpty()),
                "All recipes have registered, nonempty source and result icons");
        h.succeed();
    }

    @GameTest(template = "fate_test", timeoutTicks = 100)
    public static void bonusLootUsesActualMobsAndStructuresRatherThanFakeBonusSources(GameTestHelper h) {
        var recipes = AcquisitionCatalog.collect();
        h.assertTrue(recipes.stream().anyMatch(recipe -> recipe.kind() == AcquisitionRecipe.Kind.MOB
                        && recipe.id().getPath().contains("entities/blood_ape_demon_general/")
                        && BuiltInRegistries.ITEM.getKey(recipe.result().getItem()).getPath().equals("high_sword_grindstone")
                        && recipe.details().contains(AcquisitionCatalog.text("weight", 3, 20))),
                "Boss bonus grindstone appears under the blood ape, including empty-entry weight");
        h.assertTrue(recipes.stream().anyMatch(recipe -> recipe.kind() == AcquisitionRecipe.Kind.CHEST
                        && recipe.id().getPath().contains("minecraft/chests/ancient_city/")),
                "Mod bonus loot can be acquired in vanilla ancient city chests");
        h.assertTrue(recipes.stream().noneMatch(recipe -> recipe.id().getPath().matches(
                        "acquisition/immortal_cultivation_mod/(chests|entities)/[^/]*_bonus/.*")),
                "Bonus tables themselves are never shown as actual mobs or buildings");
        h.succeed();
    }

    @GameTest(template = "fate_test", timeoutTicks = 100)
    public static void nestedAlternativesRetainToolConditionsChancesAndZeroCountRanges(GameTestHelper h) {
        var json = JsonParser.parseString("""
                {"pools":[{"rolls":{"min":0,"max":2},"entries":[
                  {"type":"minecraft:alternatives","children":[
                    {"type":"minecraft:item","name":"minecraft:stone","conditions":[
                      {"condition":"minecraft:match_tool","predicate":{"enchantment":"minecraft:silk_touch"}}]},
                    {"type":"minecraft:item","name":"minecraft:diamond","conditions":[
                      {"condition":"minecraft:random_chance","chance":0.05}],"functions":[
                      {"function":"minecraft:set_count","count":{"min":0,"max":3}},
                      {"function":"minecraft:apply_bonus","enchantment":"minecraft:fortune"}]}]}]}]}
                """).getAsJsonObject();
        List<AcquisitionRecipe> recipes = new ArrayList<>();
        var table = ResourceLocation.parse("minecraft:blocks/stone");
        AcquisitionCatalog.appendTable(recipes, table, table, json);
        h.assertTrue(recipes.size() == 2, "Alternative item entries are both discoverable");
        var diamond = recipes.get(1);
        h.assertTrue(diamond.details().contains(AcquisitionCatalog.text("count", "0–3"))
                        && diamond.details().contains(AcquisitionCatalog.text("rolls", "0–2")), "Zero-valued count and roll bounds remain intact");
        h.assertTrue(diamond.details().contains(AcquisitionCatalog.text("chance", "5.00%")), "Conditional rare-drop chance is displayed");
        h.assertTrue(diamond.details().contains(AcquisitionCatalog.text("not", net.minecraft.network.chat.Component.empty().append(AcquisitionCatalog.text("silk"))))
                        && diamond.details().contains(AcquisitionCatalog.text("fortune_bonus")),
                "Fallback correctly excludes Silk Touch while preserving Fortune bonuses");
        h.succeed();
    }

    @GameTest(template = "fate_test", timeoutTicks = 100)
    public static void allEightFlamesHaveDistinctCapturedBottlesAndHonestAcquisitionDescriptions(GameTestHelper h) {
        var recipes = AcquisitionCatalog.flames();
        h.assertTrue(recipes.size() == 8, "All eight special flames have acquisition pages");
        var kinds = new HashSet<com.example.immortal_cultivation_mod.flame.FlameKind>();
        for (var recipe : recipes) {
            var kind = FlameVesselItem.flame(recipe.result());
            h.assertTrue(kind != null && kinds.add(kind), "Every result has a unique filled-vessel subtype");
            h.assertTrue(recipe.relatedItems().size() == 1 && recipe.details().contains(AcquisitionCatalog.text("essence_not_item")),
                    "Seed lookup points to capture instructions without claiming natural seeds drop as items");
        }
        h.succeed();
    }
}
