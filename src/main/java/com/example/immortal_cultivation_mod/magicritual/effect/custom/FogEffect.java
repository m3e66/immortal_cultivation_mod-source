package com.example.immortal_cultivation_mod.magicritual.effect.custom;

import com.example.immortal_cultivation_mod.magicritual.block.ModBlocks;
import com.example.immortal_cultivation_mod.magicritual.block.custom.FogEscapeBlockEntity;
import com.example.immortal_cultivation_mod.magicritual.effect.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import org.jetbrains.annotations.Nullable;

public class FogEffect extends MobEffect {
   private static final ResourceKey<Level> EMPTY_DIMENSION = ResourceKey.create(
         Registries.DIMENSION,
         ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "empty"));
   private static final int RANDOM_COORD_RANGE = 100_000;
   private static final int ESCAPE_BLOCK_RADIUS = 5;

   public FogEffect(MobEffectCategory category, int color) {
      super(category, color);
   }

   public static void applyFromCenter(ServerPlayer player, BlockPos centerPos,
                                      int centerRadius) {
      player.addEffect(new MobEffectInstance(ModEffects.FOG, 60, 0,
            true, false, true));
      if (!player.level().dimension().equals(EMPTY_DIMENSION)) {
         teleportToEmptyDimension(player, centerPos, centerRadius);
      }
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
      return true;
   }

   @Override
   public boolean applyEffectTick(LivingEntity entity, int amplifier) {
      if (!(entity instanceof ServerPlayer player)) {
         return true;
      }
      if (player.level().dimension().equals(EMPTY_DIMENSION)) {
         player.addEffect(new MobEffectInstance(ModEffects.FOG, 80, amplifier,
               true, false, true));
      } else {
         teleportToEmptyDimension(player, null, 0);
      }
      return true;
   }

   private static void teleportToEmptyDimension(ServerPlayer player,
                                                @Nullable BlockPos sourceCenter,
                                                int sourceRadius) {
      MinecraftServer server = player.getServer();
      if (server == null) {
         return;
      }
      ServerLevel emptyLevel = server.getLevel(EMPTY_DIMENSION);
      if (emptyLevel == null) {
         return;
      }

      RandomSource random = player.getRandom();
      int x = random.nextInt(RANDOM_COORD_RANGE * 2 + 1) - RANDOM_COORD_RANGE;
      int z = random.nextInt(RANDOM_COORD_RANGE * 2 + 1) - RANDOM_COORD_RANGE;
      int y = safeSurfaceY(emptyLevel, x, z);
      BlockPos escapePos = findEscapeBlockPos(emptyLevel, random, x, y, z);
      emptyLevel.setBlock(escapePos,
            ((Block)ModBlocks.FOG_ESCAPE_BLOCK.get()).defaultBlockState(), 3);
      if (emptyLevel.getBlockEntity(escapePos) instanceof FogEscapeBlockEntity escape) {
         escape.setReturnLocation(player, sourceCenter, sourceRadius);
      }
      player.teleportTo(emptyLevel, x + 0.5D, y, z + 0.5D,
            player.getYRot(), player.getXRot());
   }

   private static BlockPos findEscapeBlockPos(ServerLevel level, RandomSource random,
                                               int x, int y, int z) {
      for (int attempt = 0; attempt < 32; attempt++) {
         int dx = random.nextInt(ESCAPE_BLOCK_RADIUS * 2 + 1) - ESCAPE_BLOCK_RADIUS;
         int dz = random.nextInt(ESCAPE_BLOCK_RADIUS * 2 + 1) - ESCAPE_BLOCK_RADIUS;
         int distanceSquared = dx * dx + dz * dz;
         if (distanceSquared == 0
               || distanceSquared > ESCAPE_BLOCK_RADIUS * ESCAPE_BLOCK_RADIUS) {
            continue;
         }
         int escapeX = x + dx;
         int escapeZ = z + dz;
         int escapeY = safeSurfaceY(level, escapeX, escapeZ);
         BlockPos position = new BlockPos(escapeX, escapeY, escapeZ);
         if (level.getBlockState(position).isAir()) {
            return position;
         }
      }
      return new BlockPos(x + ESCAPE_BLOCK_RADIUS, y, z);
   }

   private static int safeSurfaceY(ServerLevel level, int x, int z) {
      int y = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z);
      while (!level.getBlockState(new BlockPos(x, y, z)).isAir()
            || !level.getBlockState(new BlockPos(x, y + 1, z)).isAir()) {
         y++;
      }
      return y;
   }
}
