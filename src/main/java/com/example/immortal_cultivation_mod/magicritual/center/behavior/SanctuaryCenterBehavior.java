package com.example.immortal_cultivation_mod.magicritual.center.behavior;

import com.example.immortal_cultivation_mod.magicritual.center.CenterContext;
import com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport;
import com.example.immortal_cultivation_mod.magicritual.center.CenterFuelController;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterBehavior;
import com.example.immortal_cultivation_mod.magicritual.center.CenterTargetPolicy;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public final class SanctuaryCenterBehavior implements MagicCenterBehavior {
   @Override
   public void serverTick(CenterContext context) {
      if (!context.center().shouldTriggerEvery(context.level().getGameTime(), 20)) {
         return;
      }
      for (LivingEntity entity : CenterBehaviorUtil.livingEntities(context)) {
         if (!CenterTargetPolicy.receivesBeneficialEffect(context.center(), entity)) {
            continue;
         }
         entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 50, 1, true, false, true));
         entity.setRemainingFireTicks(Math.max(0, entity.getRemainingFireTicks() - 40));
         removeMinorEffect(entity, MobEffects.POISON);
         removeMinorEffect(entity, MobEffects.WEAKNESS);
         CenterEffectSupport.restoreQi(entity, Math.max(1, CenterEffectSupport.maxQi(entity) / 100));
         context.level().sendParticles(ParticleTypes.HAPPY_VILLAGER, entity.getX(),
               entity.getY() + entity.getBbHeight() * 0.55D, entity.getZ(),
               5, 0.3D, 0.45D, 0.3D, 0.01D);
      }
   }

   private static void removeMinorEffect(LivingEntity entity,
                                         net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
      MobEffectInstance instance = entity.getEffect(effect);
      if (instance != null && instance.getAmplifier() == 0) {
         entity.removeEffect(effect);
      }
   }

   @Override
   public int getFuelCostPerMinute(CenterContext context) {
      return CenterFuelController.baseRadiusCost(context.center().getRadius()) * 2;
   }

   @Override public boolean supportsHostileMobSetting() { return true; }
   @Override public boolean supportsPassiveMobSetting() { return true; }
}
