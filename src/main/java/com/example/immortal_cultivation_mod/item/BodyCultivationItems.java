package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.spell.BloodQiShield;
import com.example.immortal_cultivation_mod.spell.BodyCultivation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class BodyCultivationItems {
    private BodyCultivationItems() {
    }

    public static final class BodyTemperingPillItem extends com.example.immortal_cultivation_mod.alchemy.LegacyPillItem {
        public BodyTemperingPillItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                    && BodyCultivation.useBodyTemperingPill(serverPlayer)
                    && !player.getAbilities().instabuild) {
                com.example.immortal_cultivation_mod.fate.FateCrafting.consumedPill(serverPlayer, stack);
                stack.shrink(1);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

    }

    public static final class BloodQiPillItem extends com.example.immortal_cultivation_mod.alchemy.LegacyPillItem {
        public BloodQiPillItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                BloodQiShield.grant(serverPlayer);
                com.example.immortal_cultivation_mod.fate.FateCrafting.consumedPill(serverPlayer, stack);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

    }
}
