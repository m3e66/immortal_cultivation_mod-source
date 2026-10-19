package com.example.immortal_cultivation_mod.compat.jei;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.item.WoolStrippingRecipe;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/** Fixed JEI views of dynamic recipes; the server's actual crafting rules remain unchanged. */
public final class WoolStripJeiRecipes {
    public static List<RecipeHolder<CraftingRecipe>> collect() {
        var recipes = new ArrayList<RecipeHolder<CraftingRecipe>>();
        Ingredient strips = Ingredient.of(java.util.Arrays.stream(DyeColor.values())
                .map(ModItems::woolStrip).toArray(net.minecraft.world.item.Item[]::new));
        for (DyeColor color : DyeColor.values()) {
            var wool = BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(color.getName() + "_wool"));
            var cutting = new ShapelessRecipe("", CraftingBookCategory.MISC,
                    new ItemStack(ModItems.woolStrip(color), 3),
                    NonNullList.of(Ingredient.EMPTY, Ingredient.of(wool), Ingredient.of(Items.SHEARS))) {
                @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
                    return new WoolStrippingRecipe(CraftingBookCategory.MISC).getRemainingItems(input);
                }
            };
            recipes.add(new RecipeHolder<>(id("wool_stripping/" + color.getName()), cutting));
            var dyeing = new ShapelessRecipe("", CraftingBookCategory.MISC,
                    new ItemStack(ModItems.woolStrip(color)),
                    NonNullList.of(Ingredient.EMPTY, strips, Ingredient.of(DyeItem.byColor(color))));
            recipes.add(new RecipeHolder<>(id("wool_strip_dyeing/" + color.getName()), dyeing));
        }
        return List.copyOf(recipes);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "jei/" + path);
    }

    private WoolStripJeiRecipes() {}
}
