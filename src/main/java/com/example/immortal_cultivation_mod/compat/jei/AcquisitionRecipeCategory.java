package com.example.immortal_cultivation_mod.compat.jei;

import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class AcquisitionRecipeCategory implements IRecipeCategory<AcquisitionRecipe> {
    private static final int WIDTH = 176, HEIGHT = 164;
    private final RecipeType<AcquisitionRecipe> type;
    private final AcquisitionRecipe.Kind kind;
    private final IDrawable icon;

    public AcquisitionRecipeCategory(IGuiHelper helper, RecipeType<AcquisitionRecipe> type,
                                     AcquisitionRecipe.Kind kind, ItemStack icon) {
        this.type = type;
        this.kind = kind;
        this.icon = helper.createDrawableItemStack(icon);
    }

    @Override public RecipeType<AcquisitionRecipe> getRecipeType() { return type; }
    @Override public Component getTitle() { return AcquisitionCatalog.text("category." + kind.name().toLowerCase(java.util.Locale.ROOT)); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return WIDTH; }
    @Override public int getHeight() { return HEIGHT; }
    @Override public ResourceLocation getRegistryName(AcquisitionRecipe recipe) { return recipe.id(); }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AcquisitionRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.CATALYST, 10, 24).setStandardSlotBackground()
                .addItemStack(recipe.sourceIcon())
                .addRichTooltipCallback((slot, tooltip) -> tooltip.add(recipe.sourceName()));
        if (kind == AcquisitionRecipe.Kind.FLAME) {
            builder.addInputSlot(48, 24).setStandardSlotBackground()
                    .addItemLike(com.example.immortal_cultivation_mod.item.ModItems.FLAME_VESSEL.get());
        } else {
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStack(recipe.sourceIcon());
        }
        builder.addOutputSlot(147, 24).setOutputSlotBackground().addItemStack(recipe.result())
                .addRichTooltipCallback((slot, tooltip) -> recipe.details().forEach(tooltip::add));
        // User wants acquisition visible from Uses (U) too. These are lookup aliases,
        // not consumed ingredients, and never add fictitious visible input slots.
        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStack(recipe.result())
                .addItemStacks(recipe.relatedItems());
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStacks(recipe.relatedItems());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder extras, AcquisitionRecipe recipe, IFocusGroup focuses) {
        extras.addText(recipe.sourceName(), WIDTH, 18).setPosition(0, 0)
                .setTextAlignment(mezz.jei.api.gui.placement.HorizontalAlignment.CENTER);
        extras.addRecipeArrow().setPosition(104, 24);
        List<FormattedText> details = new ArrayList<>();
        details.add(AcquisitionCatalog.text("action." + kind.name().toLowerCase(java.util.Locale.ROOT)));
        details.addAll(recipe.details());
        extras.addScrollBoxWidget(WIDTH, HEIGHT - 54, 0, 54).setContents(details);
    }
}
