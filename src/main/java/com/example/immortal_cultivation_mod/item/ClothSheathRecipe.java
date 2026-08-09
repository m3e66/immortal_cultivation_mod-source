package com.example.immortal_cultivation_mod.item;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class ClothSheathRecipe extends CustomRecipe {
    public ClothSheathRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return input.width() >= 3 && input.height() >= 3
                && (matchesPattern(input, false) || matchesPattern(input, true));
    }

    private boolean matchesPattern(CraftingInput input, boolean mirrored) {
        DyeColor color = null;
        for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
                ItemStack stack = input.getItem(x, y);
                boolean expected = x < 3 && y < 3 && expectedAt(x, y, mirrored);
                if (expected != !stack.isEmpty()) {
                    return false;
                }
                if (!expected) {
                    continue;
                }
                if (!(stack.getItem() instanceof WoolStripItem strip)) {
                    return false;
                }
                if (color == null) {
                    color = strip.color();
                } else if (color != strip.color()) {
                    return false;
                }
            }
        }
        return color != null;
    }

    private boolean expectedAt(int x, int y, boolean mirrored) {
        if (y == 0) {
            return x == 1;
        }
        if (y == 1) {
            return x == 0 || x == 2;
        }
        return mirrored ? x == 1 || x == 2 : x == 0 || x == 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        for (ItemStack stack : input.items()) {
            if (stack.getItem() instanceof WoolStripItem strip) {
                ItemStack sheath = new ItemStack(ModItems.CLOTH_SHEATH.get());
                ClothSheathItem.setColor(sheath, strip.color());
                return sheath;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.CLOTH_SHEATH.get();
    }
}
