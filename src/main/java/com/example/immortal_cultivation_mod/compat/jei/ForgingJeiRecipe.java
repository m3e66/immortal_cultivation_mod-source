package com.example.immortal_cultivation_mod.compat.jei;

import com.example.immortal_cultivation_mod.item.ForgingSystem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record ForgingJeiRecipe(
        ResourceLocation id,
        ItemStack jadeSlip,
        ForgingSystem.ForgingRecipe forgingRecipe,
        ItemStack result) {
}