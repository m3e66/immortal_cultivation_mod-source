package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ForgingYujianItem extends Item {
    public ForgingYujianItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        var recipe = ForgingSystem.recipe(stack);
        if (recipe != null) {
            tooltip.add(Component.translatable("tooltip." + ImmortalCultivationMod.MODID + ".forging_yujian.materials").withStyle(ChatFormatting.GRAY));
            for (var entry : recipe.required().entrySet()) {
                tooltip.add(Component.literal("  ")
                        .append(Component.translatable(entry.getKey().getDescriptionId()))
                        .append(Component.literal(" x" + entry.getValue()))
                        .withStyle(ChatFormatting.DARK_AQUA));
            }
            tooltip.add(Component.translatable("tooltip." + ImmortalCultivationMod.MODID + ".forging_yujian.temp", recipe.averageTemp())
                    .withStyle(ChatFormatting.RED));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
