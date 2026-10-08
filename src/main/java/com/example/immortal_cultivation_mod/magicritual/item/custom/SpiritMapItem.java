package com.example.immortal_cultivation_mod.magicritual.item.custom;

import com.example.immortal_cultivation_mod.magicritual.poi.ModPoiTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class SpiritMapItem extends MapItem {
   public SpiritMapItem(Item.Properties properties) {
      super(properties);
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (stack.has(DataComponents.MAP_ID)) {
         return InteractionResultHolder.pass(stack);
      } else if (level.isClientSide) {
         return InteractionResultHolder.consume(stack);
      } else {
         ServerLevel serverLevel = (ServerLevel)level;
         MapItemSavedData mapData = MapItemSavedData.createFresh(player.getX(), player.getZ(), (byte)1, true, true, level.dimension());
         MapId mapId = level.getFreeMapId();
         level.setMapData(mapId, mapData);
         stack.set(DataComponents.MAP_ID, mapId);
         addSpiritVeinDecorations(serverLevel, mapData);
         return InteractionResultHolder.success(stack);
      }
   }

   public void inventoryTick(ItemStack stack, Level level, Entity entity, int itemSlot, boolean isSelected) {
      super.inventoryTick(stack, level, entity, itemSlot, isSelected);
      if (!level.isClientSide && level.getGameTime() % 100L == 0L) {
         MapItemSavedData data = MapItem.getSavedData(stack, level);
         if (data instanceof MapItemSavedData) {
            addSpiritVeinDecorations((ServerLevel)level, data);
         }
      }

   }

   private static void addSpiritVeinDecorations(ServerLevel level, MapItemSavedData mapData) {
      int range = 64;
      BlockPos centerPos = new BlockPos(mapData.centerX, 0, mapData.centerZ);
      level.getPoiManager().getInRange((holder) -> holder.value() == ModPoiTypes.SPIRIT_VEIN_CENTER_POI.get(), centerPos, range * 2, Occupancy.ANY).forEach((record) -> {
         BlockPos veinPos = record.getPos();
         mapData.addDecoration(MapDecorationTypes.RED_MARKER, level, "spirit_vein_" + veinPos.getX() + "_" + veinPos.getY() + "_" + veinPos.getZ(), (double)veinPos.getX() + (double)0.5F, (double)veinPos.getZ() + (double)0.5F, (double)0.0F, (Component)null);
      });
   }
}
