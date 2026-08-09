package com.example.immortal_cultivation_mod.magicritual.block.custom;

import com.example.immortal_cultivation_mod.magicritual.block.ModBlocks;
import com.example.immortal_cultivation_mod.magicritual.effect.ModEffects;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.joml.Vector3f;

public class ComboBlockEntity extends BlockEntity {
   private static final int EFFECT_DURATION = 100;
   private static final int EFFECT_INTERVAL = 20;
   private static final int CENTER_RESCAN_TICKS = 100;
   private List<BlockPos> cachedCenterPositions = List.of();
   private long nextCenterScanTick;
   public static final ComboBlockEntity.PatternEntry[] FOG_PATTERN = new ComboBlockEntity.PatternEntry[]{
      new ComboBlockEntity.PatternEntry(-1, -1, CircleType.ATTACK),
      new ComboBlockEntity.PatternEntry(-1, 1, CircleType.DEFENSE),
      new ComboBlockEntity.PatternEntry(1, 1, CircleType.ATTACK)
   };
   public static final ComboBlockEntity.PatternEntry[] INVERTED_PATTERN = new ComboBlockEntity.PatternEntry[]{
      new ComboBlockEntity.PatternEntry(0, -1, CircleType.DEFENSE),
      new ComboBlockEntity.PatternEntry(-1, 1, CircleType.DEFENSE),
      new ComboBlockEntity.PatternEntry(1, 1, CircleType.DEFENSE)
   };
   public static final ComboBlockEntity.PatternEntry[] FORCEFIELD_PATTERN = new ComboBlockEntity.PatternEntry[]{
      new ComboBlockEntity.PatternEntry(1, -1, CircleType.DEFENSE),
      new ComboBlockEntity.PatternEntry(1, 1, CircleType.DEFENSE),
      new ComboBlockEntity.PatternEntry(-1, -1, CircleType.DEFENSE),
      new ComboBlockEntity.PatternEntry(-1, 1, CircleType.DEFENSE)
   };

   public static ComboBlockEntity.PatternEntry[] getPattern(ComboType comboType) {
      return switch (comboType) {
         case FOG -> FOG_PATTERN;
         case INVERTED -> INVERTED_PATTERN;
         case FORCEFIELD -> FORCEFIELD_PATTERN;
      };
   }

   public ComboBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.COMBO_BE.get(), pos, state);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, ComboBlockEntity be) {
      if (level.isClientSide) {
         be.spawnParticles(level, pos, state);
      } else if (level.getGameTime() % 20L == 0L) {
         ComboType comboType = (ComboType)state.getValue(ComboBlock.COMBO_TYPE);
         if (!isPatternValid(level, pos, comboType)) {
            level.destroyBlock(pos, false);
         } else {
            if (be.hasFueledMagicCenter(level, pos)) {
               applyEffect(level, pos, comboType);
            }
         }
      }
   }

   public static boolean isPatternValid(Level level, BlockPos pos, ComboType type) {
      ComboBlockEntity.PatternEntry[] entries = getPattern(type);

      for (int rotation = 0; rotation < 4; rotation++) {
         if (isPatternValidAtRotation(level, pos, entries, rotation)) {
            return true;
         }
      }

      return false;
   }

   private static boolean isPatternValidAtRotation(Level level, BlockPos pos, ComboBlockEntity.PatternEntry[] entries, int rotation) {
      for (ComboBlockEntity.PatternEntry entry : entries) {
         boolean found = false;
         int dx = rotateDx(entry.dx(), entry.dz(), rotation);
         int dz = rotateDz(entry.dx(), entry.dz(), rotation);

         for (int y = -1; y <= 1; y++) {
            BlockPos checkPos = pos.offset(dx, y, dz);
            BlockState blockState = level.getBlockState(checkPos);
            if (blockState.is((Block)ModBlocks.MAGIC_CIRCLE_BLOCK.get()) && blockState.getValue(MagicCircleBlock.CIRCLE_TYPE) == entry.type()) {
               found = true;
               break;
            }
         }

         if (!found) {
            return false;
         }
      }

      return true;
   }

   private static int rotateDx(int dx, int dz, int rotation) {
      return switch (rotation) {
         case 1 -> -dz;
         case 2 -> -dx;
         case 3 -> dz;
         default -> dx;
      };
   }

   private static int rotateDz(int dx, int dz, int rotation) {
      return switch (rotation) {
         case 1 -> dx;
         case 2 -> -dz;
         case 3 -> -dx;
         default -> dz;
      };
   }

   public static boolean isCirclePartOfActiveCombo(Level level, BlockPos circlePos) {
      BlockState circleState = level.getBlockState(circlePos);
      if (!circleState.is((Block)ModBlocks.MAGIC_CIRCLE_BLOCK.get())) {
         return false;
      }

      CircleType circleType = (CircleType)circleState.getValue(MagicCircleBlock.CIRCLE_TYPE);
      for (ComboType comboType : ComboType.values()) {
         ComboBlockEntity.PatternEntry[] entries = getPattern(comboType);

         for (ComboBlockEntity.PatternEntry entry : entries) {
            if (entry.type() != circleType) {
               continue;
            }

            for (int rotation = 0; rotation < 4; rotation++) {
               int dx = rotateDx(entry.dx(), entry.dz(), rotation);
               int dz = rotateDz(entry.dx(), entry.dz(), rotation);
               BlockPos expectedSPos = circlePos.offset(-dx, 0, -dz);

               for (int y = -1; y <= 1; y++) {
                  BlockPos checkPos = expectedSPos.offset(0, y, 0);
                  BlockState state = level.getBlockState(checkPos);
                  if (state.is((Block)ModBlocks.COMBO_BLOCK.get())
                     && state.getValue(ComboBlock.COMBO_TYPE) == comboType
                     && isPatternValidAtRotation(level, checkPos, entries, rotation)) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   private boolean hasFueledMagicCenter(Level level, BlockPos comboPos) {
      long gameTime = level.getGameTime();
      if (gameTime >= nextCenterScanTick) {
         List<BlockPos> positions = new ArrayList<>();
         for (MagicCenterBlockEntity center : MagicCenterBlockEntity.getLoadedCentersNear(
               level, comboPos, MagicCenterBlockEntity.MAX_RADIUS)) {
            if (Math.abs(center.getBlockPos().getY() - comboPos.getY()) <= 3) {
               positions.add(center.getBlockPos().immutable());
            }
         }
         cachedCenterPositions = List.copyOf(positions);
         nextCenterScanTick = gameTime + CENTER_RESCAN_TICKS;
      }

      for (BlockPos centerPos : cachedCenterPositions) {
         if (!level.hasChunkAt(centerPos)
               || !(level.getBlockEntity(centerPos) instanceof MagicCenterBlockEntity center)) {
            continue;
         }
         int dx = centerPos.getX() - comboPos.getX();
         int dz = centerPos.getZ() - comboPos.getZ();
         int radius = center.getRadius();
         if (dx * dx + dz * dz <= radius * radius && center.hasFuel()) {
            return true;
         }
      }
      return false;
   }

   private static void applyEffect(Level level, BlockPos pos, ComboType type) {
      DeferredHolder<MobEffect, MobEffect> effect = switch (type) {
         case FOG -> ModEffects.FOG;
         case INVERTED -> ModEffects.INVERTED_CONTROL;
         case FORCEFIELD -> null;
      };
      if (effect != null) {
         AABB area = MagicCircleBlockEntity.createEffectColumn(level, pos);

         for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            entity.addEffect(new MobEffectInstance(effect, 100, 0, false, true, true));
         }
      }
   }

   private void spawnParticles(Level level, BlockPos pos, BlockState state) {
      ComboType type = (ComboType)state.getValue(ComboBlock.COMBO_TYPE);

      double[][] points = switch (type) {
         case FOG -> MagicCenterBlockEntity.FOG_POINTS;
         case INVERTED -> MagicCenterBlockEntity.INVERTED_POINTS;
         case FORCEFIELD -> MagicCenterBlockEntity.FORCEFIELD_POINTS;
      };

      Vector3f color = switch (type) {
         case FOG -> new Vector3f(1.0F, 1.0F, 1.0F);
         case INVERTED -> new Vector3f(0.5F, 0.0F, 0.5F);
         case FORCEFIELD -> new Vector3f(0.2F, 0.6F, 1.0F);
      };
      double cx = (double)pos.getX() + 0.5;
      double cy = (double)pos.getY() + 0.1;
      double cz = (double)pos.getZ() + 0.5;
      long time = level.getGameTime();
      double angle = (double)time * 0.8 % 360.0;
      double rad = Math.toRadians(angle);
      double cos = Math.cos(rad);
      double sin = Math.sin(rad);
      double scale = 1.0;
      DustParticleOptions particle = new DustParticleOptions(color, 0.25F);

      for (double[] p : points) {
         double px = p[0] * scale;
         double pz = p[1] * scale;
         double rx = px * cos - pz * sin;
         double rz = px * sin + pz * cos;
         level.addParticle(particle, cx + rx, cy, cz + rz, 0.0, 0.0, 0.0);
      }
   }

   public static record PatternEntry(int dx, int dz, CircleType type) {
   }
}
