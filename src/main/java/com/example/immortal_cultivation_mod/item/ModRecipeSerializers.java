package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ImmortalCultivationMod.MODID);

    public static final Supplier<RecipeSerializer<WoolStrippingRecipe>> WOOL_STRIPPING =
            RECIPE_SERIALIZERS.register("wool_stripping",
                    () -> new SimpleCraftingRecipeSerializer<>(WoolStrippingRecipe::new));
    public static final Supplier<RecipeSerializer<WoolStripDyeingRecipe>> WOOL_STRIP_DYEING =
            RECIPE_SERIALIZERS.register("wool_strip_dyeing",
                    () -> new SimpleCraftingRecipeSerializer<>(WoolStripDyeingRecipe::new));
    public static final Supplier<RecipeSerializer<ClothSheathRecipe>> CLOTH_SHEATH =
            RECIPE_SERIALIZERS.register("cloth_sheath",
                    () -> new SimpleCraftingRecipeSerializer<>(ClothSheathRecipe::new));

    private ModRecipeSerializers() {
    }
}
