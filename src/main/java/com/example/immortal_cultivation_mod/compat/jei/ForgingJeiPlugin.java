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
import mezz.jei.api.registration.ISubtypeRegistration;
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
    public static final RecipeType<AcquisitionRecipe> MOB_DROPS = acquisitionType("mob_drops");
    public static final RecipeType<AcquisitionRecipe> BLOCK_DROPS = acquisitionType("block_drops");
    public static final RecipeType<AcquisitionRecipe> CHEST_LOOT = acquisitionType("chest_loot");
    public static final RecipeType<AcquisitionRecipe> FLAME_CAPTURE = acquisitionType("flame_capture");

    private static RecipeType<AcquisitionRecipe> acquisitionType(String path) {
        return RecipeType.create(ImmortalCultivationMod.MODID, path, AcquisitionRecipe.class);
    }

    private static final ResourceLocation PLUGIN_ID =
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "forging_jei");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var helper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new ForgingRecipeCategory(helper),
                new AcquisitionRecipeCategory(helper, MOB_DROPS, AcquisitionRecipe.Kind.MOB, new ItemStack(Items.ROTTEN_FLESH)),
                new AcquisitionRecipeCategory(helper, BLOCK_DROPS, AcquisitionRecipe.Kind.BLOCK, new ItemStack(Items.IRON_PICKAXE)),
                new AcquisitionRecipeCategory(helper, CHEST_LOOT, AcquisitionRecipe.Kind.CHEST, new ItemStack(Items.CHEST)),
                new AcquisitionRecipeCategory(helper, FLAME_CAPTURE, AcquisitionRecipe.Kind.FLAME,
                        new ItemStack(com.example.immortal_cultivation_mod.item.ModItems.FLAME_VESSEL.get())));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(mezz.jei.api.constants.RecipeTypes.CRAFTING, WoolStripJeiRecipes.collect());
        registration.addRecipes(FORGING, collectRecipes());
        var acquisition = AcquisitionCatalog.collect();
        registration.addRecipes(MOB_DROPS, acquisition.stream().filter(r -> r.kind() == AcquisitionRecipe.Kind.MOB).toList());
        registration.addRecipes(BLOCK_DROPS, acquisition.stream().filter(r -> r.kind() == AcquisitionRecipe.Kind.BLOCK).toList());
        registration.addRecipes(CHEST_LOOT, acquisition.stream().filter(r -> r.kind() == AcquisitionRecipe.Kind.CHEST).toList());
        registration.addRecipes(FLAME_CAPTURE, acquisition.stream().filter(r -> r.kind() == AcquisitionRecipe.Kind.FLAME).toList());
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        // Ignore random captured-entity UUIDs; match bottles by the flame actually inside.
        registration.registerSubtypeInterpreter(com.example.immortal_cultivation_mod.item.ModItems.FLAME_VESSEL.get(),
                new mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter<ItemStack>() {
                    @Override public Object getSubtypeData(ItemStack stack, mezz.jei.api.ingredients.subtypes.UidContext context) {
                        var flame = com.example.immortal_cultivation_mod.flame.FlameVesselItem.flame(stack);
                        return flame == null ? "empty" : flame.id();
                    }
                    @Override public String getLegacyStringSubtypeInfo(ItemStack stack, mezz.jei.api.ingredients.subtypes.UidContext context) {
                        return (String) getSubtypeData(stack, context);
                    }
                });
    }

    @Override
    public void registerExtraIngredients(mezz.jei.api.registration.IExtraIngredientRegistration registration) {
        registration.addExtraItemStacks(AcquisitionCatalog.flames().stream().map(AcquisitionRecipe::result).toList());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(
                FORGING,
                ModBlocks.CU_TIE_XIAODING.get(),
                ModBlocks.QINGTONG_YINHUO_DING.get(),
                ModBlocks.CHI_JIN_SANYANG_DING.get());
        registration.addRecipeCatalysts(FLAME_CAPTURE, com.example.immortal_cultivation_mod.item.ModItems.FLAME_VESSEL.get());
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
