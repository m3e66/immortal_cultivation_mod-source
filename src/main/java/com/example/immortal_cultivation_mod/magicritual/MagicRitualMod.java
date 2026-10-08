package com.example.immortal_cultivation_mod.magicritual;

import com.example.immortal_cultivation_mod.magicritual.block.ModBlocks;
import com.example.immortal_cultivation_mod.magicritual.block.custom.ModBlockEntities;
import com.example.immortal_cultivation_mod.magicritual.datagen.DataGenerators;
import com.example.immortal_cultivation_mod.magicritual.effect.ModEffects;
import com.example.immortal_cultivation_mod.magicritual.item.ModItems;
import com.example.immortal_cultivation_mod.magicritual.network.ModPayloads;
import com.example.immortal_cultivation_mod.magicritual.poi.ModPoiTypes;
import com.example.immortal_cultivation_mod.magicritual.ui.ModClientEvents;
import com.example.immortal_cultivation_mod.magicritual.ui.ModMenuTypes;
import com.example.immortal_cultivation_mod.magicritual.worldgen.ModFeatures;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;

public final class MagicRitualMod {
   public static final String MODID = "immortal_cultivation_mod";

   private MagicRitualMod() {
   }

   public static void register(IEventBus modEventBus) {
      ModItems.ITEMS.register(modEventBus);
      ModBlocks.BLOCKS.register(modEventBus);
      ModBlockEntities.register(modEventBus);
      ModMenuTypes.register(modEventBus);
      ModEffects.register(modEventBus);
      ModFeatures.register(modEventBus);
      ModPoiTypes.register(modEventBus);
      modEventBus.addListener(ModPayloads::register);
      modEventBus.addListener(DataGenerators::gatherData);
      if (FMLEnvironment.dist == Dist.CLIENT) {
         ModClientEvents.register(modEventBus);
      }
   }
}
