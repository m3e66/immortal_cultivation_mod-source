package com.example.immortal_cultivation_mod.magicritual.datagen;

import com.example.immortal_cultivation_mod.magicritual.worldgen.ModConfiguredFeatures;
import com.example.immortal_cultivation_mod.magicritual.worldgen.ModPlacedFeatures;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;

public class ModWorldGenProvider extends DatapackBuiltinEntriesProvider {
   public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
      .add(Registries.CONFIGURED_FEATURE, ModConfiguredFeatures::bootstrap)
      .add(Registries.PLACED_FEATURE, ModPlacedFeatures::bootstrap);

   public ModWorldGenProvider(PackOutput output, CompletableFuture<Provider> registries) {
      super(output, registries, BUILDER, Set.of("immortal_cultivation_mod"));
   }
}
