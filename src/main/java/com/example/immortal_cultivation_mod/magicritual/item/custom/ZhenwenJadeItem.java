package com.example.immortal_cultivation_mod.magicritual.item.custom;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class ZhenwenJadeItem extends Item {
   public ZhenwenJadeItem(Item.Properties properties) {
      super(properties);
   }

   public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("tooltip.immortal_cultivation_mod.zhenwen_jade").withStyle(ChatFormatting.AQUA));
      super.appendHoverText(stack, context, tooltip, flag);
   }
}
