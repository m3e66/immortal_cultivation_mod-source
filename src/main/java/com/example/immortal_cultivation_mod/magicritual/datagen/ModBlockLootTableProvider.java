package com.example.immortal_cultivation_mod.magicritual.datagen;

import com.example.immortal_cultivation_mod.magicritual.block.ModBlocks;
import com.example.immortal_cultivation_mod.magicritual.item.ModItems;
import java.util.Set;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModBlockLootTableProvider extends BlockLootSubProvider {
   protected ModBlockLootTableProvider(Provider lookupProvider) {
      super(Set.of(), FeatureFlags.DEFAULT_FLAGS, lookupProvider);
   }

   protected void generate() {
      this.add((Block)ModBlocks.SPIRIT_ORE.get(), this.createOreDrop((Block)ModBlocks.SPIRIT_ORE.get(), (Item)ModItems.SPIRIT_STONE.get()));
   }

   protected Iterable<Block> getKnownBlocks() {
      return ModBlocks.BLOCKS
         .getEntries()
         .stream()
         .map(e -> (Block)e.get())
         .filter(
            b -> b != ModBlocks.MAGIC_CENTER_BLOCK.get()
                  && b != ModBlocks.MAGIC_CIRCLE_BLOCK.get()
                  && b != ModBlocks.SPIRIT_VEIN_CENTER.get()
                  && b != ModBlocks.COMBO_BLOCK.get()
         )
         .toList();
   }
}
