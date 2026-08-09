package com.example.immortal_cultivation_mod.item;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.Map;

public class WoolStrippingRecipe extends CustomRecipe {
    private static final Map<Item, DyeColor> WOOL_COLORS = Map.ofEntries(
            Map.entry(Items.WHITE_WOOL, DyeColor.WHITE),
            Map.entry(Items.ORANGE_WOOL, DyeColor.ORANGE),
            Map.entry(Items.MAGENTA_WOOL, DyeColor.MAGENTA),
            Map.entry(Items.LIGHT_BLUE_WOOL, DyeColor.LIGHT_BLUE),
            Map.entry(Items.YELLOW_WOOL, DyeColor.YELLOW),
            Map.entry(Items.LIME_WOOL, DyeColor.LIME),
            Map.entry(Items.PINK_WOOL, DyeColor.PINK),
            Map.entry(Items.GRAY_WOOL, DyeColor.GRAY),
            Map.entry(Items.LIGHT_GRAY_WOOL, DyeColor.LIGHT_GRAY),
            Map.entry(Items.CYAN_WOOL, DyeColor.CYAN),
            Map.entry(Items.PURPLE_WOOL, DyeColor.PURPLE),
            Map.entry(Items.BLUE_WOOL, DyeColor.BLUE),
            Map.entry(Items.BROWN_WOOL, DyeColor.BROWN),
            Map.entry(Items.GREEN_WOOL, DyeColor.GREEN),
            Map.entry(Items.RED_WOOL, DyeColor.RED),
            Map.entry(Items.BLACK_WOOL, DyeColor.BLACK));

    public WoolStrippingRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int wool = 0;
        int shears = 0;
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(ItemTags.WOOL)) {
                wool++;
            } else if (stack.is(Items.SHEARS)) {
                shears++;
            } else {
                return false;
            }
        }
        return wool == 1 && shears == 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        for (ItemStack stack : input.items()) {
            if (stack.is(ItemTags.WOOL)) {
                DyeColor color = WOOL_COLORS.getOrDefault(stack.getItem(), DyeColor.WHITE);
                return new ItemStack(ModItems.woolStrip(color), 3);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.is(Items.SHEARS)) {
                continue;
            }
            ItemStack shears = stack.copyWithCount(1);
            shears.setDamageValue(shears.getDamageValue() + 1);
            if (shears.getDamageValue() < shears.getMaxDamage()) {
                remaining.set(i, shears);
            }
        }
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.WOOL_STRIPPING.get();
    }
}
