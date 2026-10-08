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
import net.minecraft.world.level.block.entity.BlockEntityType;
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
   public static final PatternEntry[] FOG_PATTERN;
   public static final PatternEntry[] INVERTED_PATTERN;
   public static final PatternEntry[] FORCEFIELD_PATTERN;

   public static PatternEntry[] getPattern(ComboType comboType) {
      PatternEntry[] var10000;
      switch (comboType) {
         case FOG -> var10000 = FOG_PATTERN;
         case INVERTED -> var10000 = INVERTED_PATTERN;
         case FORCEFIELD -> var10000 = FORCEFIELD_PATTERN;
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   public ComboBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlockEntities.COMBO_BE.get(), pos, state);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, ComboBlockEntity be) {
      if (level.isClientSide) {
         be.spawnParticles(level, pos, state);
      } else if (level.getGameTime() % 20L == 0L) {
         ComboType comboType = (ComboType)state.getValue(ComboBlock.COMBO_TYPE);
         if (!isPatternValid(level, pos, comboType)) {
            level.destroyBlock(pos, false);
         } else if (be.hasFueledMagicCenter(level, pos)) {
            applyEffect(level, pos, comboType);
         }
      }

   }

   public static boolean isPatternValid(Level level, BlockPos pos, ComboType type) {
      PatternEntry[] entries = getPattern(type);

      for(int rotation = 0; rotation < 4; ++rotation) {
         if (isPatternValidAtRotation(level, pos, entries, rotation)) {
            return true;
         }
      }

      return false;
   }

   private static boolean isPatternValidAtRotation(Level level, BlockPos pos, PatternEntry[] entries, int rotation) {
      for(PatternEntry entry : entries) {
         boolean found = false;
         int dx = rotateDx(entry.dx(), entry.dz(), rotation);
         int dz = rotateDz(entry.dx(), entry.dz(), rotation);

         for(int y = -1; y <= 1; ++y) {
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
      int var10000;
      switch (rotation) {
         case 1 -> var10000 = -dz;
         case 2 -> var10000 = -dx;
         case 3 -> var10000 = dz;
         default -> var10000 = dx;
      }

      return var10000;
   }

   private static int rotateDz(int dx, int dz, int rotation) {
      int var10000;
      switch (rotation) {
         case 1 -> var10000 = dx;
         case 2 -> var10000 = -dz;
         case 3 -> var10000 = -dx;
         default -> var10000 = dz;
      }

      return var10000;
   }

   public static boolean isCirclePartOfActiveCombo(Level level, BlockPos circlePos) {
      BlockState circleState = level.getBlockState(circlePos);
      if (!circleState.is((Block)ModBlocks.MAGIC_CIRCLE_BLOCK.get())) {
         return false;
      } else {
         CircleType circleType = (CircleType)circleState.getValue(MagicCircleBlock.CIRCLE_TYPE);

         for(ComboType comboType : ComboType.values()) {
            PatternEntry[] entries = getPattern(comboType);

            for(PatternEntry entry : entries) {
               if (entry.type() == circleType) {
                  for(int rotation = 0; rotation < 4; ++rotation) {
                     int dx = rotateDx(entry.dx(), entry.dz(), rotation);
                     int dz = rotateDz(entry.dx(), entry.dz(), rotation);
                     BlockPos expectedSPos = circlePos.offset(-dx, 0, -dz);

                     for(int y = -1; y <= 1; ++y) {
                        BlockPos checkPos = expectedSPos.offset(0, y, 0);
                        BlockState state = level.getBlockState(checkPos);
                        if (state.is((Block)ModBlocks.COMBO_BLOCK.get()) && state.getValue(ComboBlock.COMBO_TYPE) == comboType && isPatternValidAtRotation(level, checkPos, entries, rotation)) {
                           return true;
                        }
                     }
                  }
               }
            }
         }

         return false;
      }
   }

   private boolean hasFueledMagicCenter(Level level, BlockPos comboPos) {
      long gameTime = level.getGameTime();
      if (gameTime >= this.nextCenterScanTick) {
         List<BlockPos> positions = new ArrayList();

         for(MagicCenterBlockEntity center : MagicCenterBlockEntity.getLoadedCentersNear(level, comboPos, 303)) {
            if (Math.abs(center.getBlockPos().getY() - comboPos.getY()) <= 3) {
               positions.add(center.getBlockPos().immutable());
            }
         }

         this.cachedCenterPositions = List.copyOf(positions);
         this.nextCenterScanTick = gameTime + 100L;
      }

      for(BlockPos centerPos : this.cachedCenterPositions) {
         if (level.hasChunkAt(centerPos)) {
            BlockEntity var8 = level.getBlockEntity(centerPos);
            if (var8 instanceof MagicCenterBlockEntity) {
               MagicCenterBlockEntity center = (MagicCenterBlockEntity)var8;
               int dx = centerPos.getX() - comboPos.getX();
               int dz = centerPos.getZ() - comboPos.getZ();
               int radius = center.getRadius();
               if (dx * dx + dz * dz <= radius * radius && center.hasFuel()) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private static void applyEffect(Level level, BlockPos pos, ComboType type) {
      DeferredHolder var10000;
      switch (type) {
         case FOG -> var10000 = ModEffects.FOG;
         case INVERTED -> var10000 = ModEffects.INVERTED_CONTROL;
         case FORCEFIELD -> var10000 = null;
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      DeferredHolder<MobEffect, MobEffect> effect = var10000;
      if (effect != null) {
         AABB area = MagicCircleBlockEntity.createEffectColumn(level, pos);

         for(LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            entity.addEffect(new MobEffectInstance(effect, 100, 0, false, true, true));
         }
      }

   }

   private void spawnParticles(Level level, BlockPos pos, BlockState state) {
      ComboType type = (ComboType)state.getValue(ComboBlock.COMBO_TYPE);
      double[][] var10000;
      switch (type) {
         case FOG -> var10000 = MagicCenterBlockEntity.FOG_POINTS;
         case INVERTED -> var10000 = MagicCenterBlockEntity.INVERTED_POINTS;
         case FORCEFIELD -> var10000 = MagicCenterBlockEntity.FORCEFIELD_POINTS;
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      double[][] points = var10000;
      Vector3f var38;
      switch (type) {
         case FOG -> var38 = new Vector3f(1.0F, 1.0F, 1.0F);
         case INVERTED -> var38 = new Vector3f(0.5F, 0.0F, 0.5F);
         case FORCEFIELD -> var38 = new Vector3f(0.2F, 0.6F, 1.0F);
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      Vector3f color = var38;
      double cx = (double)pos.getX() + (double)0.5F;
      double cy = (double)pos.getY() + 0.1;
      double cz = (double)pos.getZ() + (double)0.5F;
      long time = level.getGameTime();
      double angle = (double)time * 0.8 % (double)360.0F;
      double rad = Math.toRadians(angle);
      double cos = Math.cos(rad);
      double sin = Math.sin(rad);
      double scale = (double)1.0F;
      DustParticleOptions particle = new DustParticleOptions(color, 0.25F);

      for(double[] p : points) {
         double px = p[0] * scale;
         double pz = p[1] * scale;
         double rx = px * cos - pz * sin;
         double rz = px * sin + pz * cos;
         level.addParticle(particle, cx + rx, cy, cz + rz, (double)0.0F, (double)0.0F, (double)0.0F);
      }

   }

   static {
      FOG_PATTERN = new PatternEntry[]{new PatternEntry(-1, -1, CircleType.ATTACK), new PatternEntry(-1, 1, CircleType.DEFENSE), new PatternEntry(1, 1, CircleType.ATTACK)};
      INVERTED_PATTERN = new PatternEntry[]{new PatternEntry(0, -1, CircleType.DEFENSE), new PatternEntry(-1, 1, CircleType.DEFENSE), new PatternEntry(1, 1, CircleType.DEFENSE)};
      FORCEFIELD_PATTERN = new PatternEntry[]{new PatternEntry(1, -1, CircleType.DEFENSE), new PatternEntry(1, 1, CircleType.DEFENSE), new PatternEntry(-1, -1, CircleType.DEFENSE), new PatternEntry(-1, 1, CircleType.DEFENSE)};
   }

   public static record PatternEntry(int dx, int dz, CircleType type) {
   }
}
