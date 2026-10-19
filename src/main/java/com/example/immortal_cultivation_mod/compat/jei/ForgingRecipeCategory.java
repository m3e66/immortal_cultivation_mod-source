package com.example.immortal_cultivation_mod.compat.jei;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.item.ForgingSystem;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class ForgingRecipeCategory implements IRecipeCategory<ForgingJeiRecipe> {
    private static final int WIDTH = 176;
    private static final int HEIGHT = 176;
    /** Keep ingredient slots clear of the flame and recipe arrow. */
    private static final int MATERIAL_COLUMNS = 5;

    private final IDrawable icon;

    public ForgingRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(ModBlocks.QINGTONG_YINHUO_DING.get());
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<ForgingJeiRecipe> getRecipeType() {
        return ForgingJeiPlugin.FORGING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei." + ImmortalCultivationMod.MODID + ".category.forging");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(
            IRecipeLayoutBuilder builder,
            ForgingJeiRecipe displayRecipe,
            IFocusGroup focuses) {
        ForgingSystem.ForgingRecipe recipe = displayRecipe.forgingRecipe();

        builder.addInputSlot(1, 1)
                .setStandardSlotBackground()
                .setSlotName("jade_slip")
                .addItemStack(displayRecipe.jadeSlip());

        List<Map.Entry<Item, Integer>> materials = new ArrayList<>(recipe.required().entrySet());
        materials.sort(Comparator.comparing(entry ->
                BuiltInRegistries.ITEM.getKey(entry.getKey()).toString()));

        int slotIndex = 0;
        for (Map.Entry<Item, Integer> material : materials) {
            ItemStack sample = new ItemStack(material.getKey());
            int remaining = material.getValue();
            while (remaining > 0) {
                int count = ForgingSystem.canStackInForgingRecipe(sample)
                        ? Math.min(remaining, sample.getMaxStackSize())
                        : 1;
                addMaterialSlot(
                        builder,
                        slotIndex,
                        List.of(sample.copyWithCount(count)),
                        "material_" + slotIndex);
                slotIndex++;
                remaining -= count;
            }
        }

        int remainingSpiritStones = recipe.spiritStoneCount();
        ItemStack lowSpiritStone = new ItemStack(
                com.example.immortal_cultivation_mod.magicritual.item.ModItems
                        .SPIRIT_STONE.get());
        while (remainingSpiritStones > 0) {
            int count = Math.min(
                    remainingSpiritStones, lowSpiritStone.getMaxStackSize());
            List<ItemStack> spiritStones = List.of(
                    lowSpiritStone.copyWithCount(count),
                    new ItemStack(
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems
                                    .MEDIUM_SPIRIT_STONE.get()).copyWithCount(count),
                    new ItemStack(
                            com.example.immortal_cultivation_mod.magicritual.item.ModItems
                                    .HIGH_SPIRIT_STONE.get()).copyWithCount(count));
                addMaterialSlot(builder, slotIndex, spiritStones, "spirit_stone_" + slotIndex)
                        .addRichTooltipCallback((slotView, tooltip) ->
                                tooltip.add(Component.translatable(
                                        "tooltip." + ImmortalCultivationMod.MODID
                                                + ".forging_yujian.any_spirit_stone")));
                slotIndex++;
            remainingSpiritStones -= count;
        }

        builder.addOutputSlot(151, 31)
                .setOutputSlotBackground()
                .setSlotName("result")
                .addItemStack(displayRecipe.result());
        // Acquisition should also be discoverable from an item's JEI Uses view.
        builder.addInvisibleIngredients(mezz.jei.api.recipe.RecipeIngredientRole.INPUT)
                .addItemStack(displayRecipe.result());
    }

    private static mezz.jei.api.gui.builder.IRecipeSlotBuilder addMaterialSlot(
            IRecipeLayoutBuilder builder,
            int slotIndex,
            List<ItemStack> stacks,
            String slotName) {
        int x = 1 + (slotIndex % MATERIAL_COLUMNS) * 18;
        int y = 27 + (slotIndex / MATERIAL_COLUMNS) * 18;
        return builder.addInputSlot(x, y)
                .setStandardSlotBackground()
                .setSlotName(slotName)
                .addItemStacks(stacks);
    }

    @Override
    public void createRecipeExtras(
            IRecipeExtrasBuilder extras,
            ForgingJeiRecipe displayRecipe,
            IFocusGroup focuses) {
        ForgingSystem.ForgingRecipe recipe = displayRecipe.forgingRecipe();

        extras.addAnimatedRecipeFlame(200).setPosition(100, 31);
        extras.addRecipeArrow().setPosition(119, 31);

        extras.addText(
                        Component.translatable(
                                        "jei." + ImmortalCultivationMod.MODID + ".forging.jade_slip")
                                .withStyle(ChatFormatting.DARK_GRAY),
                        90, 10)
                .setPosition(22, 4);

        extras.addText(
                        Component.translatable(
                                        "jei." + ImmortalCultivationMod.MODID + ".forging.temperature",
                                        recipe.averageTemp())
                                .withStyle(ChatFormatting.DARK_GRAY),
                        WIDTH - 2, 10)
                .setPosition(1, 140);

        Component fire = recipe.requiredFireType().isBlank()
                ? Component.translatable(
                        "jei." + ImmortalCultivationMod.MODID + ".forging.any_fire")
                : Component.translatable(
                        "tooltip." + ImmortalCultivationMod.MODID
                                + ".forged.fire." + recipe.requiredFireType());
        extras.addText(
                        Component.translatable(
                                        "jei." + ImmortalCultivationMod.MODID + ".forging.fire",
                                        fire)
                                .withStyle(ChatFormatting.DARK_GRAY),
                        WIDTH - 2, 10)
                .setPosition(1, 152);

        String processKey = recipe.directOutput()
                ? "direct_output"
                : "water_tempering";
        extras.addText(
                        Component.translatable(
                                        "jei." + ImmortalCultivationMod.MODID
                                                + ".forging." + processKey)
                                .withStyle(ChatFormatting.DARK_GRAY),
                        WIDTH - 2, 10)
                .setPosition(1, 164);
    }

    @Override
    public ResourceLocation getRegistryName(ForgingJeiRecipe recipe) {
        return recipe.id();
    }
}
