package com.example.immortal_cultivation_mod.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Hotel food whose edible data remains available even on legacy NPC stacks. */
public class HotelFoodItem extends Item {
    private final FoodProperties foodProperties;

    public HotelFoodItem(Properties properties, FoodProperties foodProperties) {
        super(properties.food(foodProperties));
        this.foodProperties = foodProperties;
    }

    @Override
    public FoodProperties getFoodProperties(
            ItemStack stack, @Nullable LivingEntity entity) {
        return foodProperties;
    }
}
