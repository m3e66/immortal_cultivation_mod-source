package com.example.immortal_cultivation_mod.magicritual.center.behavior;

import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.magicritual.center.CenterContext;
import com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport;
import com.example.immortal_cultivation_mod.magicritual.center.CenterFuelController;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterBehavior;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;

public final class ScorchingCenterBehavior implements MagicCenterBehavior {
   private static final float MAX_HEALTH_BONUS_DAMAGE = 50.0F;

   public void serverTick(CenterContext context) {
      if (context.center().shouldTriggerEvery(context.level().getGameTime(), 20)) {
         int attackCount = context.center().getAttackCount();

         for(LivingEntity entity : CenterBehaviorUtil.livingEntities(context)) {
            if (CenterBehaviorUtil.hostile(context, entity)) {
               boolean fireAffinity = CenterEffectSupport.hasMethodElement(entity, "\u706b");
               float damage = 2.0F + Math.min(50.0F, entity.getMaxHealth() * 0.005F);
               float attackDamage = (fireAffinity ? damage * 0.5F : damage) * (float)attackCount;
               entity.hurt(entity.damageSources().magic(), attackDamage);
               int fireTicks = (fireAffinity ? 30 : 60) * attackCount;
               entity.setRemainingFireTicks(Math.max(entity.getRemainingFireTicks(), fireTicks));
               entity.removeEffect(ModEffects.FROZEN);
               entity.removeEffect(ModEffects.FROZEN_QI);
               int baseDrain = Math.max(1, CenterEffectSupport.maxQi(entity) / (fireAffinity ? 200 : 100));
               int drain = (int)Math.min(2147483647L, (long)baseDrain * (long)attackCount);
               int shortage = drain - CenterEffectSupport.drainQi(entity, drain);
               if (shortage > 0) {
                  entity.hurt(entity.damageSources().magic(), Math.max(1.0F, (float)shortage));
               }

               context.level().sendParticles(ParticleTypes.FLAME, entity.getX(), entity.getY() + 0.1, entity.getZ(), Math.min(96, 12 * attackCount), 0.3, 0.12, 0.3, 0.025);
            }
         }

      }
   }

   public int getFuelCostPerMinute(CenterContext context) {
      return CenterFuelController.baseRadiusCost(context.center().getRadius()) * 3;
   }

   public boolean supportsHostileMobSetting() {
      return true;
   }

   public boolean supportsPassiveMobSetting() {
      return true;
   }
}
