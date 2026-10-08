package com.example.immortal_cultivation_mod.magicritual.center.behavior;

import com.example.immortal_cultivation_mod.magicritual.center.CenterContext;
import com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class CenterBehaviorUtil {
   private CenterBehaviorUtil() {
   }

   static List<LivingEntity> livingEntities(CenterContext context) {
      return context.level().getEntitiesOfClass(LivingEntity.class, effectColumn(context), (entity) -> entity.isAlive() && insideEffectColumn(context, entity));
   }

   static AABB effectColumn(CenterContext context) {
      Vec3 center = context.center().centerPoint();
      double radius = (double)context.center().getRadius();
      return new AABB(center.x - radius, (double)context.pos().getY(), center.z - radius, center.x + radius, (double)context.level().getMaxBuildHeight(), center.z + radius);
   }

   static boolean insideEffectColumn(CenterContext context, Entity entity) {
      Vec3 center = context.center().centerPoint();
      double dx = entity.getX() - center.x;
      double dz = entity.getZ() - center.z;
      double radius = (double)context.center().getRadius();
      return entity.getY() >= (double)context.pos().getY() && dx * dx + dz * dz <= radius * radius;
   }

   static boolean hostile(CenterContext context, LivingEntity entity) {
      return CenterEffectSupport.isHostileTarget(context.center(), entity);
   }
}
