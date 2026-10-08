package com.example.immortal_cultivation_mod.magicritual.center.behavior;

import com.example.immortal_cultivation_mod.magicritual.center.CenterContext;
import com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport;
import com.example.immortal_cultivation_mod.magicritual.center.CenterFuelController;
import com.example.immortal_cultivation_mod.magicritual.center.CenterTargetPolicy;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterBehavior;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public final class SanctuaryCenterBehavior implements MagicCenterBehavior {
   public void serverTick(CenterContext context) {
      if (context.center().shouldTriggerEvery(context.level().getGameTime(), 20)) {
         for(LivingEntity entity : CenterBehaviorUtil.livingEntities(context)) {
            if (CenterTargetPolicy.receivesBeneficialEffect(context.center(), entity)) {
               entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 50, 1, true, false, true));
               entity.setRemainingFireTicks(Math.max(0, entity.getRemainingFireTicks() - 40));
               removeMinorEffect(entity, MobEffects.POISON);
               removeMinorEffect(entity, MobEffects.WEAKNESS);
               CenterEffectSupport.restoreQi(entity, Math.max(1, CenterEffectSupport.maxQi(entity) / 100));
               context.level().sendParticles(ParticleTypes.HAPPY_VILLAGER, entity.getX(), entity.getY() + (double)entity.getBbHeight() * 0.55, entity.getZ(), 5, 0.3, 0.45, 0.3, 0.01);
            }
         }

      }
   }

   private static void removeMinorEffect(LivingEntity entity, Holder<MobEffect> effect) {
      MobEffectInstance instance = entity.getEffect(effect);
      if (instance != null && instance.getAmplifier() == 0) {
         entity.removeEffect(effect);
      }

   }

   public int getFuelCostPerMinute(CenterContext context) {
      return CenterFuelController.baseRadiusCost(context.center().getRadius()) * 2;
   }

   public boolean supportsHostileMobSetting() {
      return true;
   }

   public boolean supportsPassiveMobSetting() {
      return true;
   }
}
