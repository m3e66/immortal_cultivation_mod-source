package com.example.immortal_cultivation_mod.compat.jei;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.item.ForgingSystem;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@JeiPlugin
public final class ForgingJeiPlugin implements IModPlugin {
    public static final RecipeType<ForgingJeiRecipe> FORGING =
            RecipeType.create(ImmortalCultivationMod.MODID, "forging", ForgingJeiRecipe.class);

    private static final ResourceLocation PLUGIN_ID =
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "forging_jei");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new ForgingRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(FORGING, collectRecipes());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(
                FORGING,
                ModBlocks.CU_TIE_XIAODING.get(),
                ModBlocks.QINGTONG_YINHUO_DING.get(),
                ModBlocks.CHI_JIN_SANYANG_DING.get());
    }

    private static List<ForgingJeiRecipe> collectRecipes() {
        List<ForgingJeiRecipe> recipes = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            if (!itemId.getNamespace().equals(ImmortalCultivationMod.MODID)
                    || !(item instanceof com.example.immortal_cultivation_mod.item.ForgingYujianItem)) {
                continue;
            }

            ItemStack jadeSlip = new ItemStack(item);
            ForgingSystem.ForgingRecipe forgingRecipe = ForgingSystem.recipe(jadeSlip);
            if (forgingRecipe == null) {
                continue;
            }

            ItemLike resultItem = ForgingSystem.resultItem(forgingRecipe);
            if (resultItem.asItem() == Items.AIR) {
                continue;
            }

            ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID,
                    "forging/" + forgingRecipe.id());
            recipes.add(new ForgingJeiRecipe(
                    recipeId,
                    jadeSlip,
                    forgingRecipe,
                    com.example.immortal_cultivation_mod.alchemy.AlchemyForgeRecipes.isPill(forgingRecipe.id())
                            ?com.example.immortal_cultivation_mod.alchemy.PillData.create(
                                com.example.immortal_cultivation_mod.alchemy.AlchemyChemistry.brew(
                                    com.example.immortal_cultivation_mod.alchemy.AlchemyChemistry.formula(forgingRecipe.id().substring(8)),
                                    com.example.immortal_cultivation_mod.alchemy.AlchemyChemistry.formula(forgingRecipe.id().substring(8)).required()))
                            :new ItemStack(resultItem)));
        }

        recipes.sort(Comparator.comparing(recipe -> recipe.id().toString()));
        return List.copyOf(recipes);
    }
}
