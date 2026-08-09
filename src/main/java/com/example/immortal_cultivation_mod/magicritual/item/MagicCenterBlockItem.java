package com.example.immortal_cultivation_mod.magicritual.item;

import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterTypeRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;

public final class MagicCenterBlockItem extends BlockItem {
   public MagicCenterBlockItem(Block block, Properties properties) {
      super(block, properties);
   }

   @Override
   public Component getName(ItemStack stack) {
      CompoundTag tag = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).copyTag();
      String customName = tag.getString("CustomNameText");
      if (!customName.isBlank()) {
         return Component.literal(customName);
      }
      ResourceLocation typeId = ResourceLocation.tryParse(tag.getString("CenterType"));
      if (typeId != null && MagicCenterTypeRegistry.contains(typeId)) {
         return Component.translatable(MagicCenterTypeRegistry.getOrBlank(typeId).translationKey());
      }
      return super.getName(stack);
   }
}
