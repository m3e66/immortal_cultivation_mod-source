package com.example.immortal_cultivation_mod.magicritual.center.behavior;

import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.magicritual.center.CenterContext;
import com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport;
import com.example.immortal_cultivation_mod.magicritual.center.CenterFuelController;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterBehavior;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public final class FrostCenterBehavior implements MagicCenterBehavior {
   public void serverTick(CenterContext context) {
      if (context.center().shouldTriggerEvery(context.level().getGameTime(), 20)) {
         int attackCount = context.center().getAttackCount();

         for(LivingEntity entity : CenterBehaviorUtil.livingEntities(context)) {
            if (CenterBehaviorUtil.hostile(context, entity)) {
               boolean resistant = CenterEffectSupport.hasMethodElement(entity, "\u51b0");
               entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, resistant ? 0 : 1, true, false, true));
               entity.clearFire();
               int freezeIncrease = 40 * attackCount;
               entity.setTicksFrozen(Math.min(entity.getTicksRequiredToFreeze() + 80, entity.getTicksFrozen() + freezeIncrease));
               int frozenTicks = entity.getTicksFrozen();
               int amplifier = Math.min(2, frozenTicks / 100);
               entity.addEffect(new MobEffectInstance(ModEffects.FROZEN_QI, 60, amplifier, true, false, true));
               if (frozenTicks >= 100) {
                  entity.hurt(entity.damageSources().freeze(), (resistant ? 1.0F : 2.0F) * (float)attackCount);
               }

               context.level().sendParticles(ParticleTypes.SNOWFLAKE, entity.getX(), entity.getY() + (double)entity.getBbHeight() * (double)0.5F, entity.getZ(), Math.min(96, 8 * attackCount), 0.35, 0.45, 0.35, 0.02);
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
