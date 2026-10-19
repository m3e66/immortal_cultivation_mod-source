package com.example.immortal_cultivation_mod.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Specialist merchants reserve physical stock slots and share normal NPC trading. */
public interface StockedCultivationMerchant {
    int stockSlotCount();
    boolean sellsStock(ItemStack stack);
    void ensureMerchantStock(CompoundTag profile);
}
