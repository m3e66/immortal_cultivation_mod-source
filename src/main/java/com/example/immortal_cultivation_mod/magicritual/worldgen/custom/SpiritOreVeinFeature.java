package com.example.immortal_cultivation_mod.magicritual.worldgen.custom;

import com.example.immortal_cultivation_mod.magicritual.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class SpiritOreVeinFeature extends Feature<NoneFeatureConfiguration> {
   public SpiritOreVeinFeature() {
      super(NoneFeatureConfiguration.CODEC);
   }

   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
      WorldGenLevel level = context.level();
      RandomSource random = context.random();
      int y = -100 + random.nextInt(101);
      BlockPos origin = new BlockPos(context.origin().getX(), y, context.origin().getZ());
      int oreCount = 200 + random.nextInt(51);
      int radius = 8;
      int placed = 0;
      int attempts = 0;
      int maxAttempts = oreCount * 4;

      while (placed < oreCount && attempts < maxAttempts) {
         attempts++;
         BlockPos placePos = origin.offset(
            random.nextInt(radius * 2 + 1) - radius, random.nextInt(radius * 2 + 1) - radius, random.nextInt(radius * 2 + 1) - radius
         );
         if (placePos.getY() <= 0 && !(Math.sqrt(placePos.distSqr(origin)) > (double)radius - 0.5)) {
            BlockState target = level.getBlockState(placePos);
            if (target.is(BlockTags.BASE_STONE_OVERWORLD)) {
               if (random.nextInt(50) == 0) {
                  level.setBlock(placePos, ((Block)ModBlocks.SPIRIT_VEIN_CENTER.get()).defaultBlockState(), 3);
               } else {
                  level.setBlock(placePos, ((Block)ModBlocks.SPIRIT_ORE.get()).defaultBlockState(), 3);
               }

               placed++;
            }
         }
      }

      if (placed > 0) {
         placeSurfaceSpiritwood(level, random, context.origin());
      }
      return placed > 0;
   }

   private static void placeSurfaceSpiritwood(WorldGenLevel level, RandomSource random,
                                               BlockPos origin) {
      BlockState lingmu = com.example.immortal_cultivation_mod.block.ModBlocks.LINGMU.get()
            .defaultBlockState();
      int targetCount = 3 + random.nextInt(5);
      int placed = 0;
      for (int attempt = 0; attempt < 24 && placed < targetCount; attempt++) {
         int x = origin.getX() + random.nextInt(25) - 12;
         int z = origin.getZ() + random.nextInt(25) - 12;
         int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
         BlockPos plantPos = new BlockPos(x, y, z);
         BlockState ground = level.getBlockState(plantPos.below());
         boolean validGround = ground.is(Blocks.GRASS_BLOCK)
               || ground.is(Blocks.DIRT)
               || ground.is(Blocks.COARSE_DIRT)
               || ground.is(Blocks.PODZOL)
               || ground.is(Blocks.ROOTED_DIRT)
               || ground.is(Blocks.MOSS_BLOCK)
               || ground.is(Blocks.MUD);
         if (validGround && level.getBlockState(plantPos).isAir()
               && lingmu.canSurvive(level, plantPos)) {
            level.setBlock(plantPos, lingmu, 3);
            placed++;
         }
      }
   }
}
