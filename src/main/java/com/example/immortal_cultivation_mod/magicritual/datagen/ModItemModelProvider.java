package com.example.immortal_cultivation_mod.magicritual.datagen;

import com.example.immortal_cultivation_mod.magicritual.block.ModBlocks;
import com.example.immortal_cultivation_mod.magicritual.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
   public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
      super(output, "immortal_cultivation_mod", existingFileHelper);
   }

   protected void registerModels() {
      this.basicItem((Item)ModItems.RITUAL_DAGGER.get());
      this.basicItem((Item)ModItems.SPIRIT_STONE.get());
      this.basicItem((Item)ModItems.MEDIUM_SPIRIT_STONE.get());
      this.basicItem((Item)ModItems.HIGH_SPIRIT_STONE.get());
      this.basicItem(((Block)ModBlocks.MAGIC_CENTER_BLOCK.get()).asItem());
      this.basicItem((Item)ModItems.SPIRIT_MAP.get());
   }
}
