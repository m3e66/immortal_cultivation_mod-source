package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ForgingYujianItem extends Item {
    public ForgingYujianItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        var recipe = ForgingSystem.recipe(stack);
        if (recipe != null) {
            tooltip.add(Component.translatable("tooltip." + ImmortalCultivationMod.MODID + ".forging_yujian.materials").withStyle(ChatFormatting.GRAY));
            List<Map.Entry<Item, Integer>> materials = new ArrayList<>(recipe.required().entrySet());
            materials.sort(Comparator.comparing(entry -> entry.getKey().getDescriptionId()));
            int slot = 1;
            for (var entry : materials) {
                ItemStack sample = new ItemStack(entry.getKey());
                int remaining = entry.getValue();
                while (remaining > 0) {
                    int count = ForgingSystem.canStackInForgingRecipe(sample)
                            ? Math.min(remaining, sample.getMaxStackSize())
                            : 1;
                    tooltip.add(Component.literal("  " + slot++ + ". ")
                            .append(Component.translatable(entry.getKey().getDescriptionId()))
                            .append(count > 1
                                    ? Component.literal(" ×" + count)
                                    : Component.empty())
                            .withStyle(ChatFormatting.DARK_AQUA));
                    remaining -= count;
                }
            }
            int remainingSpiritStones = recipe.spiritStoneCount();
            ItemStack spiritStone = new ItemStack(
                    com.example.immortal_cultivation_mod.magicritual.item.ModItems
                            .SPIRIT_STONE.get());
            while (remainingSpiritStones > 0) {
                int count = Math.min(
                        remainingSpiritStones, spiritStone.getMaxStackSize());
                tooltip.add(Component.literal("  " + slot++ + ". ")
                        .append(Component.translatable(
                                "tooltip." + ImmortalCultivationMod.MODID + ".forging_yujian.any_spirit_stone"))
                        .append(count > 1
                                ? Component.literal(" ×" + count)
                                : Component.empty())
                        .withStyle(ChatFormatting.DARK_AQUA));
                remainingSpiritStones -= count;
            }
            tooltip.add(Component.translatable("tooltip." + ImmortalCultivationMod.MODID + ".forging_yujian.temp", recipe.averageTemp())
                    .withStyle(ChatFormatting.RED));
            if (!recipe.requiredFireType().isBlank()) {
                tooltip.add(Component.translatable(
                                "tooltip." + ImmortalCultivationMod.MODID + ".forging_yujian.required_fire",
                                Component.translatable("tooltip." + ImmortalCultivationMod.MODID
                                        + ".forged.fire." + recipe.requiredFireType()))
                        .withStyle(ChatFormatting.DARK_RED));
            }
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
