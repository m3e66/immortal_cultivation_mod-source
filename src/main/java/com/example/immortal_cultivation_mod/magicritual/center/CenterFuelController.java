package com.example.immortal_cultivation_mod.magicritual.center;

import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlockEntity;
import com.example.immortal_cultivation_mod.magicritual.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.items.IItemHandler;

public final class CenterFuelController {
   public static final long ENERGY_PER_STONE = 1L;
   private static final long TICKS_PER_MINUTE = 1200L;

   private CenterFuelController() {
   }

   public static boolean consumeActiveTick(CenterContext context, int costPerMinute) {
      MagicCenterBlockEntity center = context.center();
      if (!center.isEnabled()) {
         return false;
      } else if (center.hasInfiniteFuel()) {
         center.setFuelCostRemainder(0L);
         return true;
      } else {
         int cost = Math.max(0, costPerMinute);
         if (cost == 0) {
            center.setFuelCostRemainder(0L);
            return true;
         } else if (!center.hasFuel()) {
            return false;
         } else {
            long accumulatedCost = center.getFuelCostRemainder() + (long)cost;
            long due = accumulatedCost / 1200L;
            if (due == 0L) {
               center.setFuelCostRemainder(accumulatedCost);
               return true;
            } else {
               while(center.getEnergy() < due) {
                  ItemStack consumed = center.getFuelSlot().extractItem(0, 1, false);
                  if (consumed.isEmpty()) {
                     return false;
                  }

                  center.addEnergy((long)fuelValue(consumed));
                  if (MagicCenterTypeRegistry.FORCEFIELD.equals(center.getCenterTypeId())) {
                     center.addBarrierHealth((float)barrierHealthFor(consumed));
                  }
               }

               center.setEnergy(center.getEnergy() - due);
               center.setFuelCostRemainder(accumulatedCost % 1200L);
               return true;
            }
         }
      }
   }

   public static int barrierHealthFor(ItemStack stack) {
      if (stack.is((Item)ModItems.HIGH_SPIRIT_STONE.get())) {
         return 1000;
      } else if (stack.is((Item)ModItems.MEDIUM_SPIRIT_STONE.get())) {
         return 100;
      } else {
         return stack.is((Item)ModItems.SPIRIT_STONE.get()) ? 10 : 0;
      }
   }

   public static int fuelValue(ItemStack stack) {
      if (stack.is((Item)ModItems.HIGH_SPIRIT_STONE.get())) {
         return 100;
      } else if (stack.is((Item)ModItems.MEDIUM_SPIRIT_STONE.get())) {
         return 10;
      } else {
         return stack.is((Item)ModItems.SPIRIT_STONE.get()) ? 1 : 0;
      }
   }

   public static void pullFromAdjacentInventories(CenterContext context) {
      MagicCenterBlockEntity center = context.center();
      if (center.getFuelSlot().getStackInSlot(0).getCount() < center.getFuelSlot().getSlotLimit(0)) {
         for(Direction direction : Direction.values()) {
            BlockPos adjacentPos = context.pos().relative(direction);
            BlockEntity adjacent = context.level().getBlockEntity(adjacentPos);
            if (adjacent != null) {
               IItemHandler handler = (IItemHandler)context.level().getCapability(ItemHandler.BLOCK, adjacentPos, direction.getOpposite());
               if (handler != null) {
                  for(int slot = 0; slot < handler.getSlots(); ++slot) {
                     ItemStack simulated = handler.extractItem(slot, 64, true);
                     if (!simulated.isEmpty() && ModItems.isSpiritStone(simulated.getItem())) {
                        ItemStack extracted = handler.extractItem(slot, simulated.getCount(), false);
                        ItemStack remainder = center.getFuelSlot().insertItem(0, extracted, false);
                        if (!remainder.isEmpty()) {
                           handler.insertItem(slot, remainder, false);
                        }

                        if (center.getFuelSlot().getStackInSlot(0).getCount() >= center.getFuelSlot().getSlotLimit(0)) {
                           return;
                        }
                     }
                  }
               }
            }
         }

      }
   }

   public static int baseRadiusCost(int radius) {
      int normalizedRadius = MagicCenterBlockEntity.normalizeRadius(radius);
      int var10000;
      switch (normalizedRadius) {
         case 3:
            var10000 = 8;
            break;
         case 4:
         case 6:
         case 8:
         case 10:
         case 12:
         default:
            var10000 = (int)Math.round((double)256.0F * Math.pow((double)normalizedRadius / (double)13.0F, (double)3.0F));
            break;
         case 5:
            var10000 = 16;
            break;
         case 7:
            var10000 = 32;
            break;
         case 9:
            var10000 = 64;
            break;
         case 11:
            var10000 = 128;
            break;
         case 13:
            var10000 = 256;
      }

      return var10000;
   }
}
