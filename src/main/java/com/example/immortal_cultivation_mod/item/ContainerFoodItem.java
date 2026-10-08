package com.example.immortal_cultivation_mod.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Edible or drinkable inn food that returns its bowl or bottle. */
public class ContainerFoodItem extends Item {
    private final FoodProperties foodProperties;
    private final Item emptyContainer;
    private final UseAnim useAnimation;

    public ContainerFoodItem(
            Properties properties,
            FoodProperties foodProperties,
            Item emptyContainer,
            UseAnim useAnimation) {
        super(properties.food(foodProperties));
        this.foodProperties = foodProperties;
        this.emptyContainer = emptyContainer;
        this.useAnimation = useAnimation;
    }

    @Override
    public FoodProperties getFoodProperties(
            ItemStack stack, @Nullable LivingEntity entity) {
        return foodProperties;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return useAnimation;
    }

    @Override
    public ItemStack finishUsingItem(
            ItemStack stack,
            Level level,
            LivingEntity entity) {
        boolean creative = entity instanceof Player player
                && player.getAbilities().instabuild;
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (creative) {
            return result;
        }

        ItemStack container = new ItemStack(emptyContainer);
        if (result.isEmpty()) {
            return container;
        }
        if (entity instanceof Player player) {
            if (!player.getInventory().add(container)) {
                player.drop(container, false);
            }
        } else if (!level.isClientSide) {
            entity.spawnAtLocation(container);
        }
        return result;
    }
}
