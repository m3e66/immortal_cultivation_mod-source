package com.example.immortal_cultivation_mod.magicritual.center;

import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlockEntity;
import com.example.immortal_cultivation_mod.magicritual.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

public final class CenterFuelController {
   public static final long ENERGY_PER_STONE = 1L;
   private static final long TICKS_PER_MINUTE = 20L * 60L;

   private CenterFuelController() {
   }

   public static boolean consumeActiveTick(CenterContext context, int costPerMinute) {
      MagicCenterBlockEntity center = context.center();
      if (!center.isEnabled()) {
         return false;
      }
      if (center.hasInfiniteFuel()) {
         center.setFuelCostRemainder(0L);
         return true;
      }
      int cost = Math.max(0, costPerMinute);
      if (cost == 0) {
         center.setFuelCostRemainder(0L);
         return true;
      }
      if (!center.hasFuel()) {
         return false;
      }

      long accumulatedCost = center.getFuelCostRemainder() + cost;
      long due = accumulatedCost / TICKS_PER_MINUTE;
      if (due == 0L) {
         center.setFuelCostRemainder(accumulatedCost);
         return true;
      }
      while (center.getEnergy() < due) {
         ItemStack consumed = center.getFuelSlot().extractItem(0, 1, false);
         if (consumed.isEmpty()) {
            return false;
         }
         center.addEnergy(fuelValue(consumed));
         if (MagicCenterTypeRegistry.FORCEFIELD.equals(center.getCenterTypeId())) {
            center.addBarrierHealth(barrierHealthFor(consumed));
         }
      }
      center.setEnergy(center.getEnergy() - due);
      center.setFuelCostRemainder(accumulatedCost % TICKS_PER_MINUTE);
      return true;
   }

   public static int barrierHealthFor(ItemStack stack) {
      if (stack.is(ModItems.HIGH_SPIRIT_STONE.get())) {
         return 1_000;
      }
      if (stack.is(ModItems.MEDIUM_SPIRIT_STONE.get())) {
         return 100;
      }
      return stack.is(ModItems.SPIRIT_STONE.get()) ? 10 : 0;
   }

   public static int fuelValue(ItemStack stack) {
      if (stack.is(ModItems.HIGH_SPIRIT_STONE.get())) {
         return 100;
      }
      if (stack.is(ModItems.MEDIUM_SPIRIT_STONE.get())) {
         return 10;
      }
      return stack.is(ModItems.SPIRIT_STONE.get()) ? 1 : 0;
   }

   public static void pullFromAdjacentInventories(CenterContext context) {
      MagicCenterBlockEntity center = context.center();
      if (center.getFuelSlot().getStackInSlot(0).getCount()
            >= center.getFuelSlot().getSlotLimit(0)) {
         return;
      }
      for (Direction direction : Direction.values()) {
         BlockPos adjacentPos = context.pos().relative(direction);
         BlockEntity adjacent = context.level().getBlockEntity(adjacentPos);
         if (adjacent == null) {
            continue;
         }
         IItemHandler handler = context.level().getCapability(
               Capabilities.ItemHandler.BLOCK, adjacentPos, direction.getOpposite());
         if (handler == null) {
            continue;
         }
         for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack simulated = handler.extractItem(slot, 64, true);
            if (simulated.isEmpty() || !ModItems.isSpiritStone(simulated.getItem())) {
               continue;
            }
            ItemStack extracted = handler.extractItem(slot, simulated.getCount(), false);
            ItemStack remainder = center.getFuelSlot().insertItem(0, extracted, false);
            if (!remainder.isEmpty()) {
               handler.insertItem(slot, remainder, false);
            }
            if (center.getFuelSlot().getStackInSlot(0).getCount()
                  >= center.getFuelSlot().getSlotLimit(0)) {
               return;
            }
         }
      }
   }

   public static int baseRadiusCost(int radius) {
      int normalizedRadius = MagicCenterBlockEntity.normalizeRadius(radius);
      return switch (normalizedRadius) {
         case 3 -> 8;
         case 5 -> 16;
         case 7 -> 32;
         case 9 -> 64;
         case 11 -> 128;
         case 13 -> 256;
         default -> (int)Math.round(256.0D * Math.pow(normalizedRadius / 13.0D, 3.0D));
      };
   }
}
