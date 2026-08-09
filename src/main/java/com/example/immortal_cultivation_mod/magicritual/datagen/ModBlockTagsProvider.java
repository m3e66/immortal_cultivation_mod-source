package com.example.immortal_cultivation_mod.magicritual.datagen;

import com.example.immortal_cultivation_mod.magicritual.block.ModBlocks;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

public class ModBlockTagsProvider extends BlockTagsProvider {
   public ModBlockTagsProvider(PackOutput output, CompletableFuture<Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
      super(output, lookupProvider, "immortal_cultivation_mod", existingFileHelper);
   }

   protected void addTags(Provider provider) {
      this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add((Block)ModBlocks.SPIRIT_ORE.get());
      this.tag(BlockTags.NEEDS_IRON_TOOL).add((Block)ModBlocks.SPIRIT_ORE.get());
   }
}
