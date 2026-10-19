package com.example.immortal_cultivation_mod.item;

import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import java.util.List;

public class FlyingSwordItem extends SwordItem {
    public FlyingSwordItem(Properties properties) {
        super(Tiers.NETHERITE, properties.durability(Tiers.NETHERITE.getUses())
                .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 6.0F, -2.4F)));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.immortal_cultivation_mod.sword_ride",
                Component.keybind("key.immortal_cultivation_mod.ride_flying_sword")));
    }
}
