package com.example.immortal_cultivation_mod.release;

import com.example.immortal_cultivation_mod.item.ForgingSystem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_content_availability")
@PrefixGameTestTemplate(false)
public final class ArchivedArtifactRealmsGameTests {
    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", path);
    }

    @GameTest(template = "fate_test")
    public static void artifactItemsEntityAndDimensionAreAbsent(GameTestHelper h) {
        for (String item : new String[]{"baota", "jin_hulu", "baota_yujian", "jin_hulu_yujian"})
            h.assertTrue(!BuiltInRegistries.ITEM.containsKey(id(item)), "Archived item is absent: " + item);
        h.assertTrue(!BuiltInRegistries.ENTITY_TYPE.containsKey(id("pagoda")), "Tower entity is absent");
        h.assertTrue(!h.getLevel().registryAccess().registryOrThrow(Registries.DIMENSION_TYPE)
                .containsKey(id("pagoda_interior")), "Artifact interior dimension type is absent");
        h.assertTrue(h.getLevel().getServer().levelKeys().stream().noneMatch(key ->
                key.location().getNamespace().equals("immortal_cultivation_mod")
                        && key.location().getPath().contains("pagoda")), "Artifact dimensions are absent");
        h.succeed();
    }

    @GameTest(template = "fate_test")
    public static void artifactForgingIsRemovedAndOtherContentRemains(GameTestHelper h) {
        for (String recipe : new String[]{"baota", "jin_hulu"})
            h.assertTrue(ForgingSystem.recipeById(recipe) == null, "Archived forging recipe is absent: " + recipe);
        for (String retained : new String[]{"dao", "flame_vessel", "weapon_shard", "black_iron_jian"})
            h.assertTrue(BuiltInRegistries.ITEM.containsKey(id(retained)), "Unrelated item remains: " + retained);
        h.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(id("spirit_flame")), "Fire seeds remain");
        h.assertTrue(ForgingSystem.recipeById("flying_sword") != null, "Weapon forging remains");
        h.assertTrue(ForgingSystem.recipeById("black_iron_jian") != null, "Higher weapon forging remains");
        h.succeed();
    }
}
