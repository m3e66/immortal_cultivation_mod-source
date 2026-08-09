package com.example.immortal_cultivation_mod.magicritual.datagen;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider.TagLookup;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

public class ModItemTagsProvider extends ItemTagsProvider {
   public ModItemTagsProvider(
      PackOutput output,
      CompletableFuture<Provider> lookupProvider,
      CompletableFuture<TagLookup<Block>> blockTags,
      @Nullable ExistingFileHelper existingFileHelper
   ) {
      super(output, lookupProvider, blockTags, "immortal_cultivation_mod", existingFileHelper);
   }

   protected void addTags(Provider provider) {
   }
}
