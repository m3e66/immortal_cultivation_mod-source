package com.example.immortal_cultivation_mod.magicritual.center.behavior;

import com.example.immortal_cultivation_mod.magicritual.center.CenterContext;
import com.example.immortal_cultivation_mod.magicritual.center.CenterFuelController;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterBehavior;
import com.example.immortal_cultivation_mod.magicritual.effect.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public final class InvertedCenterBehavior implements MagicCenterBehavior {
   @Override
   public void serverTick(CenterContext context) {
      if (!context.center().shouldTriggerEvery(context.level().getGameTime(), 20)) {
         return;
      }
      for (LivingEntity entity : CenterBehaviorUtil.livingEntities(context)) {
         if (context.center().isAffectedByCircle(entity)) {
            entity.addEffect(new MobEffectInstance(ModEffects.INVERTED_CONTROL, 60, 0,
                  true, false, true));
         }
      }
   }

   @Override
   public int getFuelCostPerMinute(CenterContext context) {
      return CenterFuelController.baseRadiusCost(context.center().getRadius()) * 2;
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
