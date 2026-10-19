package com.example.immortal_cultivation_mod.compat.jei;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Display-only acquisition data. No JEI classes, safe to verify on a dedicated server. */
public record AcquisitionRecipe(ResourceLocation id, Kind kind, Component sourceName,
                                ItemStack sourceIcon, ItemStack result, List<Component> details,
                                List<ItemStack> relatedItems) {
    public enum Kind { MOB, BLOCK, CHEST, FLAME }

    public AcquisitionRecipe {
        details = List.copyOf(details);
        relatedItems = List.copyOf(relatedItems);
    }
}
