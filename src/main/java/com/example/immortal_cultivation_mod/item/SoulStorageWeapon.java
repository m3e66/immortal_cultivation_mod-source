package com.example.immortal_cultivation_mod.item;

import net.minecraft.world.item.ItemStack;

public interface SoulStorageWeapon {
    double storedSouls(ItemStack stack);

    void setStoredSouls(ItemStack stack, double amount);

    double soulCapacity(ItemStack stack);
}
