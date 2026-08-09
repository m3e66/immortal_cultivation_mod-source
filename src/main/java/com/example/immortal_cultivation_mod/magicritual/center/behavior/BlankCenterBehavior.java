package com.example.immortal_cultivation_mod.magicritual.center.behavior;

import com.example.immortal_cultivation_mod.magicritual.block.custom.MagicCenterBlockEntity;
import com.example.immortal_cultivation_mod.magicritual.center.CenterContext;
import com.example.immortal_cultivation_mod.magicritual.center.CenterFuelController;
import com.example.immortal_cultivation_mod.magicritual.center.CenterTargetPolicy;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterBehavior;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

public final class BlankCenterBehavior implements MagicCenterBehavior {
   private static final int MAX_AMPLIFIER = 4;

   @Override
   public void serverTick(CenterContext context) {
      if (!context.center().shouldTriggerEvery(context.level().getGameTime(), 20)) {
         return;
      }
      MagicCenterBlockEntity center = context.center();
      MagicCenterBlockEntity.CircleCounts counts = center.getPassiveCircleCounts();
      double radius = center.getRadius();
      AABB bounds = new AABB(context.pos()).inflate(radius);
      double radiusSquared = radius * radius;
      for (LivingEntity entity : context.level().getEntitiesOfClass(LivingEntity.class, bounds)) {
         if (entity.position().distanceToSqr(center.centerPoint()) > radiusSquared
               || !CenterTargetPolicy.receivesBlankBenefit(center, entity)) {
            continue;
         }
         apply(entity, counts.attack(), MobEffects.DAMAGE_BOOST);
         apply(entity, counts.defense(), MobEffects.DAMAGE_RESISTANCE);
         apply(entity, counts.regeneration(), MobEffects.REGENERATION);
      }
   }

   private static void apply(LivingEntity entity, int count,
                             net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
      if (count <= 0) {
         return;
      }
      int amplifier = Math.min(MAX_AMPLIFIER, count - 1);
      entity.addEffect(new MobEffectInstance(effect, 40, amplifier, true, false, true));
   }

   @Override
   public int getFuelCostPerMinute(CenterContext context) {
      context.center().refreshPassiveCircleCacheIfNeeded(false);
      MagicCenterBlockEntity.CircleCounts counts = context.center().getPassiveCircleCounts();
      return CenterFuelController.baseRadiusCost(context.center().getRadius())
            + (counts.attack() + counts.defense() + counts.regeneration()) * 10;
   }

   @Override
   public boolean supportsHostileMobSetting() {
      return true;
   }

   @Override
   public boolean supportsPassiveMobSetting() {
      return true;
   }
}
