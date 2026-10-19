package com.example.immortal_cultivation_mod.compat.jei;

import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.item.WoolStripDyeingRecipe;
import com.example.immortal_cultivation_mod.item.WoolStrippingRecipe;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immortal_cultivation_jei_wool")
@PrefixGameTestTemplate(false)
public final class WoolStripJeiGameTests {
    @GameTest(template = "fate_test")
    public static void allColorViewsMatchActualCraftingAndRetainShears(GameTestHelper h) {
        var recipes = WoolStripJeiRecipes.collect();
        h.assertTrue(recipes.size() == 32 && new HashSet<>(recipes.stream().map(r -> r.id()).toList()).size() == 32,
                "JEI receives distinct cutting and dyeing views for all 16 strip colors");
        var cutting = new WoolStrippingRecipe(CraftingBookCategory.MISC);
        var dyeing = new WoolStripDyeingRecipe(CraftingBookCategory.MISC);
        for (DyeColor color : DyeColor.values()) {
            var wool = BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(color.getName() + "_wool"));
            var cutInput = CraftingInput.of(2, 1, List.of(new ItemStack(wool), new ItemStack(Items.SHEARS)));
            var cutView = recipes.stream().filter(r -> r.id().getPath().equals("jei/wool_stripping/" + color.getName()))
                    .findFirst().orElseThrow().value();
            h.assertTrue(cutting.matches(cutInput, h.getLevel()) && cutView.matches(cutInput, h.getLevel())
                            && ItemStack.matches(cutView.getResultItem(h.getLevel().registryAccess()),
                                    cutting.assemble(cutInput, h.getLevel().registryAccess())),
                    "JEI cutting output matches the actual three-strip colored result: " + color);
            var remainder = cutView.getRemainingItems(cutInput).get(1);
            h.assertTrue(remainder.is(Items.SHEARS) && remainder.getDamageValue() == 1,
                    "Cutting view retains shears with the same one-durability cost as the real recipe");
            var dyeView = recipes.stream().filter(r -> r.id().getPath().equals("jei/wool_strip_dyeing/" + color.getName()))
                    .findFirst().orElseThrow().value();
            for (DyeColor source : DyeColor.values()) {
                var input = CraftingInput.of(2, 1, List.of(new ItemStack(ModItems.woolStrip(source)),
                        new ItemStack(DyeItem.byColor(color))));
                h.assertTrue(dyeView.matches(input, h.getLevel()) && dyeing.matches(input, h.getLevel())
                                && ItemStack.matches(dyeView.getResultItem(h.getLevel().registryAccess()),
                                        dyeing.assemble(input, h.getLevel().registryAccess())),
                        "Every displayed dyeing input color produces the real selected output color");
            }
        }
        h.succeed();
    }
    private WoolStripJeiGameTests() {}
}
