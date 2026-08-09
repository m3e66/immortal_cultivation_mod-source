package com.example.immortal_cultivation_mod.magicritual.worldgen;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

public class ModPlacedFeatures {
   public static final ResourceKey<PlacedFeature> SPIRIT_ORE_VEIN_PLACED_KEY = ResourceKey.create(
      Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "spirit_ore_vein")
   );

   public static void bootstrap(BootstrapContext<PlacedFeature> context) {
      Holder<ConfiguredFeature<?, ?>> configuredFeature = context.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(ModConfiguredFeatures.SPIRIT_ORE_VEIN_KEY);
      List<PlacementModifier> modifiers = List.of(
         RarityFilter.onAverageOnceEvery(4),
         InSquarePlacement.spread(),
         HeightRangePlacement.uniform(VerticalAnchor.absolute(80), VerticalAnchor.absolute(120)),
         BiomeFilter.biome()
      );
      PlacementUtils.register(context, SPIRIT_ORE_VEIN_PLACED_KEY, configuredFeature, modifiers);
   }
}
