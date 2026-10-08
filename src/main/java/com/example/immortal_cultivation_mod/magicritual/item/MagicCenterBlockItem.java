package com.example.immortal_cultivation_mod.magicritual.item;

import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterTypeRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;

public final class MagicCenterBlockItem extends BlockItem {
   public MagicCenterBlockItem(Block block, Item.Properties properties) {
      super(block, properties);
   }

   public Component getName(ItemStack stack) {
      CompoundTag tag = ((CustomData)stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY)).copyTag();
      String customName = tag.getString("CustomNameText");
      if (!customName.isBlank()) {
         return Component.literal(customName);
      } else {
         ResourceLocation typeId = ResourceLocation.tryParse(tag.getString("CenterType"));
         return (Component)(typeId != null && MagicCenterTypeRegistry.contains(typeId) ? Component.translatable(MagicCenterTypeRegistry.getOrBlank(typeId).translationKey()) : super.getName(stack));
      }
   }
}
