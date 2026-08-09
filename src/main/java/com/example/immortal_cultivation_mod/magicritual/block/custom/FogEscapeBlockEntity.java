package com.example.immortal_cultivation_mod.magicritual.block.custom;

import com.example.immortal_cultivation_mod.magicritual.effect.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import org.jetbrains.annotations.Nullable;

public class FogEscapeBlockEntity extends BlockEntity {
   private static final String TAG_RETURN_DIMENSION = "return_dimension";
   private static final String TAG_RETURN_X = "return_x";
   private static final String TAG_RETURN_Y = "return_y";
   private static final String TAG_RETURN_Z = "return_z";
   private static final String TAG_RETURN_YAW = "return_yaw";
   private static final String TAG_RETURN_PITCH = "return_pitch";
   private static final String TAG_SOURCE_CENTER = "source_center";
   private static final String TAG_SOURCE_RADIUS = "source_radius";
   private ResourceKey<Level> returnDimension = Level.OVERWORLD;
   private double returnX;
   private double returnY;
   private double returnZ;
   private float returnYaw;
   private float returnPitch;
   private boolean hasSourceCenter;
   private BlockPos sourceCenter = BlockPos.ZERO;
   private int sourceRadius;

   public FogEscapeBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.FOG_ESCAPE_BE.get(), pos, state);
   }

   public void setReturnLocation(ServerPlayer player, @Nullable BlockPos sourceCenter,
                                 int sourceRadius) {
      this.returnDimension = player.level().dimension();
      this.returnX = player.getX();
      this.returnY = player.getY();
      this.returnZ = player.getZ();
      this.returnYaw = player.getYRot();
      this.returnPitch = player.getXRot();
      this.hasSourceCenter = sourceCenter != null && sourceRadius > 0;
      this.sourceCenter = this.hasSourceCenter ? sourceCenter.immutable() : BlockPos.ZERO;
      this.sourceRadius = this.hasSourceCenter ? sourceRadius : 0;
      this.setChanged();
   }

   public void teleportPlayerBack(ServerPlayer player) {
      MinecraftServer server = player.getServer();
      if (server != null) {
         ServerLevel returnLevel = server.getLevel(this.returnDimension);
         if (returnLevel == null) {
            returnLevel = server.overworld();
         }
         BlockPos returnPos = this.findOutsideMagicCircle(returnLevel);
         player.removeEffect(ModEffects.FOG);
         player.teleportTo(
            returnLevel, (double)returnPos.getX() + 0.5, (double)returnPos.getY(), (double)returnPos.getZ() + 0.5, this.returnYaw, this.returnPitch
         );
      }
   }

   private BlockPos findOutsideMagicCircle(ServerLevel level) {
      BlockPos originalPos = BlockPos.containing(this.returnX, this.returnY, this.returnZ);
      if (this.hasSourceCenter) {
         return this.findOutsideCenter(level, this.sourceCenter, this.sourceRadius);
      }
      BlockPos nearestCenter = null;
      int nearestDistance = Integer.MAX_VALUE;
      int nearestRadius = 0;

      int highestCenterY = Math.min(originalPos.getY(), level.getMaxBuildHeight() - 1);
      for (MagicCenterBlockEntity center : MagicCenterBlockEntity.getLoadedCentersNear(
            level, originalPos, MagicCenterBlockEntity.MAX_RADIUS)) {
         BlockPos centerPos = center.getBlockPos();
         if (centerPos.getY() > highestCenterY || centerPos.getY() < level.getMinBuildHeight()) {
            continue;
         }
         int dx = centerPos.getX() - originalPos.getX();
         int dz = centerPos.getZ() - originalPos.getZ();
         int radius = center.getRadius();
         int distance = dx * dx + dz * dz;
         if (distance <= radius * radius && distance < nearestDistance) {
            nearestCenter = centerPos;
            nearestDistance = distance;
            nearestRadius = radius;
         }
      }

      if (nearestCenter == null) {
         return safeSurfacePos(level, originalPos.getX(), originalPos.getZ());
      }
      return this.findOutsideCenter(level, nearestCenter, nearestRadius);
   }

   private BlockPos findOutsideCenter(ServerLevel level, BlockPos center, int radius) {
      double offsetX = this.returnX - ((double)center.getX() + 0.5D);
      double offsetZ = this.returnZ - ((double)center.getZ() + 0.5D);
      double length = Math.sqrt(offsetX * offsetX + offsetZ * offsetZ);
      if (length < 0.001D) {
         offsetX = 1.0D;
         offsetZ = 0.0D;
         length = 1.0D;
      }

      int escapeDistance = radius + 2;
      int targetX = (int)Math.floor(center.getX() + 0.5D
            + offsetX / length * escapeDistance);
      int targetZ = (int)Math.floor(center.getZ() + 0.5D
            + offsetZ / length * escapeDistance);
      return safeSurfacePos(level, targetX, targetZ);
   }

   private static BlockPos safeSurfacePos(ServerLevel level, int x, int z) {
      int y = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z);

      while (!level.getBlockState(new BlockPos(x, y, z)).isAir() || !level.getBlockState(new BlockPos(x, y + 1, z)).isAir()) {
         y++;
      }

      return new BlockPos(x, y, z);
   }

   protected void saveAdditional(CompoundTag tag, Provider registries) {
      super.saveAdditional(tag, registries);
      tag.putString("return_dimension", this.returnDimension.location().toString());
      tag.putDouble("return_x", this.returnX);
      tag.putDouble("return_y", this.returnY);
      tag.putDouble("return_z", this.returnZ);
      tag.putFloat("return_yaw", this.returnYaw);
      tag.putFloat("return_pitch", this.returnPitch);
      if (this.hasSourceCenter) {
         tag.putLong(TAG_SOURCE_CENTER, this.sourceCenter.asLong());
         tag.putInt(TAG_SOURCE_RADIUS, this.sourceRadius);
      }
   }

   protected void loadAdditional(CompoundTag tag, Provider registries) {
      super.loadAdditional(tag, registries);
      ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString("return_dimension"));
      if (dimensionId == null) {
         dimensionId = Level.OVERWORLD.location();
      }

      this.returnDimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
      this.returnX = tag.getDouble("return_x");
      this.returnY = tag.getDouble("return_y");
      this.returnZ = tag.getDouble("return_z");
      this.returnYaw = tag.getFloat("return_yaw");
      this.returnPitch = tag.getFloat("return_pitch");
      this.hasSourceCenter = tag.contains(TAG_SOURCE_CENTER)
            && tag.getInt(TAG_SOURCE_RADIUS) > 0;
      this.sourceCenter = this.hasSourceCenter
            ? BlockPos.of(tag.getLong(TAG_SOURCE_CENTER)) : BlockPos.ZERO;
      this.sourceRadius = this.hasSourceCenter ? tag.getInt(TAG_SOURCE_RADIUS) : 0;
   }
}
