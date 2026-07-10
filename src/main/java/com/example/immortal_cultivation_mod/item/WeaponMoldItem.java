package com.example.immortal_cultivation_mod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class WeaponMoldItem extends Item {
    public WeaponMoldItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Component stats = ForgingSystem.moldTooltip(stack);
        if (!stats.getString().isEmpty()) {
            tooltip.add(stats.copy().withStyle(ChatFormatting.GOLD));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
