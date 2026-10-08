package com.example.immortal_cultivation_mod.magicritual.poi;

import com.example.immortal_cultivation_mod.magicritual.block.ModBlocks;
import com.google.common.collect.ImmutableSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModPoiTypes {
   public static final DeferredRegister<PoiType> POI_TYPES;
   public static final DeferredHolder<PoiType, PoiType> SPIRIT_VEIN_CENTER_POI;

   public static void register(IEventBus eventBus) {
      POI_TYPES.register(eventBus);
   }

   static {
      POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, "immortal_cultivation_mod");
      SPIRIT_VEIN_CENTER_POI = POI_TYPES.register("spirit_vein_center", () -> new PoiType(ImmutableSet.copyOf(((Block)ModBlocks.SPIRIT_VEIN_CENTER.get()).getStateDefinition().getPossibleStates()), 0, 1));
   }
}
