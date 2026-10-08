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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import org.jetbrains.annotations.Nullable;

public class FogEffect extends MobEffect {
   private static final ResourceKey<Level> EMPTY_DIMENSION;
   private static final int RANDOM_COORD_RANGE = 100000;
   private static final int ESCAPE_BLOCK_RADIUS = 5;

   public FogEffect(MobEffectCategory category, int color) {
      super(category, color);
   }

   public static void applyFromCenter(ServerPlayer player, BlockPos centerPos, int centerRadius) {
      player.addEffect(new MobEffectInstance(ModEffects.FOG, 60, 0, true, false, true));
      if (!player.level().dimension().equals(EMPTY_DIMENSION)) {
         teleportToEmptyDimension(player, centerPos, centerRadius);
      }

   }

   public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
      return true;
   }

   public boolean applyEffectTick(LivingEntity entity, int amplifier) {
      if (entity instanceof ServerPlayer player) {
         if (player.level().dimension().equals(EMPTY_DIMENSION)) {
            player.addEffect(new MobEffectInstance(ModEffects.FOG, 80, amplifier, true, false, true));
         } else {
            teleportToEmptyDimension(player, (BlockPos)null, 0);
         }

         return true;
      } else {
         return true;
      }
   }

   private static void teleportToEmptyDimension(ServerPlayer player, @Nullable BlockPos sourceCenter, int sourceRadius) {
      MinecraftServer server = player.getServer();
      if (server != null) {
         ServerLevel emptyLevel = server.getLevel(EMPTY_DIMENSION);
         if (emptyLevel != null) {
            RandomSource random = player.getRandom();
            int x = random.nextInt(200001) - 100000;
            int z = random.nextInt(200001) - 100000;
            int y = safeSurfaceY(emptyLevel, x, z);
            BlockPos escapePos = findEscapeBlockPos(emptyLevel, random, x, y, z);
            emptyLevel.setBlock(escapePos, ((Block)ModBlocks.FOG_ESCAPE_BLOCK.get()).defaultBlockState(), 3);
            BlockEntity var11 = emptyLevel.getBlockEntity(escapePos);
            if (var11 instanceof FogEscapeBlockEntity) {
               FogEscapeBlockEntity escape = (FogEscapeBlockEntity)var11;
               escape.setReturnLocation(player, sourceCenter, sourceRadius);
            }

            player.teleportTo(emptyLevel, (double)x + (double)0.5F, (double)y, (double)z + (double)0.5F, player.getYRot(), player.getXRot());
         }
      }
   }

   private static BlockPos findEscapeBlockPos(ServerLevel level, RandomSource random, int x, int y, int z) {
      for(int attempt = 0; attempt < 32; ++attempt) {
         int dx = random.nextInt(11) - 5;
         int dz = random.nextInt(11) - 5;
         int distanceSquared = dx * dx + dz * dz;
         if (distanceSquared != 0 && distanceSquared <= 25) {
            int escapeX = x + dx;
            int escapeZ = z + dz;
            int escapeY = safeSurfaceY(level, escapeX, escapeZ);
            BlockPos position = new BlockPos(escapeX, escapeY, escapeZ);
            if (level.getBlockState(position).isAir()) {
               return position;
            }
         }
      }

      return new BlockPos(x + 5, y, z);
   }

   private static int safeSurfaceY(ServerLevel level, int x, int z) {
      int y;
      for(y = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z); !level.getBlockState(new BlockPos(x, y, z)).isAir() || !level.getBlockState(new BlockPos(x, y + 1, z)).isAir(); ++y) {
      }

      return y;
   }

   static {
      EMPTY_DIMENSION = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "empty"));
   }
}
