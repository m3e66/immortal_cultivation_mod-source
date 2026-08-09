package com.example.immortal_cultivation_mod.magicritual.center;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** A moving real attack whose hit logic must be stopped before crossing a forcefield. */
public interface ForcefieldInterceptable {
   @Nullable
   Entity forcefieldOwner();

   Vec3 forcefieldMovement();

   float forcefieldBarrierDamage();

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
      return Math.abs(normal.x) * box.getXsize() * 0.5D
            + Math.abs(normal.y) * box.getYsize() * 0.5D
            + Math.abs(normal.z) * box.getZsize() * 0.5D;
   }

   default double forcefieldMaximumSupport() {
      AABB box = ((Entity)this).getBoundingBox();
      double halfX = box.getXsize() * 0.5D;
      double halfY = box.getYsize() * 0.5D;
      double halfZ = box.getZsize() * 0.5D;
      return Math.sqrt(halfX * halfX + halfY * halfY + halfZ * halfZ);
   }
}
