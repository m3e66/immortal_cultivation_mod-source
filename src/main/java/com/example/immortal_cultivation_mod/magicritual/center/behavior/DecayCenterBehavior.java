package com.example.immortal_cultivation_mod.magicritual.center.behavior;

import com.example.immortal_cultivation_mod.magicritual.center.CenterContext;
import com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport;
import com.example.immortal_cultivation_mod.magicritual.center.CenterFuelController;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterBehavior;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

public final class DecayCenterBehavior implements MagicCenterBehavior {
   private static final DustParticleOptions BLOOD_MIST = new DustParticleOptions(new Vector3f(0.42F, 0.02F, 0.04F), 1.15F);

   public void serverTick(CenterContext context) {
      if (context.center().shouldTriggerEvery(context.level().getGameTime(), 20)) {
         int attackCount = context.center().getAttackCount();

         for(LivingEntity entity : CenterBehaviorUtil.livingEntities(context)) {
            if (CenterBehaviorUtil.hostile(context, entity)) {
               boolean resistant = CenterEffectSupport.hasBloodOrCorpseAffinity(entity);
               int duration = resistant ? 30 : 50;
               entity.addEffect(new MobEffectInstance(MobEffects.WITHER, duration, attackCount - 1, true, false, true));
               entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, true, false, true));
               context.level().sendParticles(BLOOD_MIST, entity.getX(), entity.getY() + (double)entity.getBbHeight() * (double)0.5F, entity.getZ(), Math.min(96, (resistant ? 3 : 8) * attackCount), 0.35, 0.45, 0.35, 0.015);
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
