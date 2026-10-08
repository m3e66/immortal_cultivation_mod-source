package com.example.immortal_cultivation_mod.magicritual.center;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public interface ForcefieldInterceptable {
   @Nullable Entity forcefieldOwner();

   Vec3 forcefieldMovement();

   float forcefieldBarrierDamage();

   default boolean forcefieldPassThrough() { return false; }

   default void forcefieldBounce(Vec3 reflectedMovement) {
      Entity entity = (Entity)this;
      entity.setDeltaMovement(reflectedMovement);
      entity.hurtMarked = true;
      entity.hasImpulse = true;
   }

   default Vec3 forcefieldCenter() {
      return ((Entity)this).position();
   }

   default double forcefieldSupport(Vec3 normal) {
      AABB box = ((Entity)this).getBoundingBox();
      return Math.abs(normal.x) * box.getXsize() * (double)0.5F + Math.abs(normal.y) * box.getYsize() * (double)0.5F + Math.abs(normal.z) * box.getZsize() * (double)0.5F;
   }

   default double forcefieldMaximumSupport() {
      AABB box = ((Entity)this).getBoundingBox();
      double halfX = box.getXsize() * (double)0.5F;
      double halfY = box.getYsize() * (double)0.5F;
      double halfZ = box.getZsize() * (double)0.5F;
      return Math.sqrt(halfX * halfX + halfY * halfY + halfZ * halfZ);
   }
}
