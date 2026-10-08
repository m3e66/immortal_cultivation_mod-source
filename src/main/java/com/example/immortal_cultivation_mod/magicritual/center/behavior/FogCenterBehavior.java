package com.example.immortal_cultivation_mod.magicritual.center.behavior;

import com.example.immortal_cultivation_mod.magicritual.center.CenterContext;
import com.example.immortal_cultivation_mod.magicritual.center.CenterFuelController;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterBehavior;
import com.example.immortal_cultivation_mod.magicritual.effect.ModEffects;
import com.example.immortal_cultivation_mod.magicritual.effect.custom.FogEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public final class FogCenterBehavior implements MagicCenterBehavior {
   public void serverTick(CenterContext context) {
      if (context.center().shouldTriggerEvery(context.level().getGameTime(), 20)) {
         for(LivingEntity entity : CenterBehaviorUtil.livingEntities(context)) {
            if (context.center().isAffectedByCircle(entity)) {
               if (entity instanceof ServerPlayer) {
                  ServerPlayer player = (ServerPlayer)entity;
                  FogEffect.applyFromCenter(player, context.pos(), context.center().getRadius());
               } else {
                  entity.addEffect(new MobEffectInstance(ModEffects.FOG, 60, 0, true, false, true));
               }
            }
         }

      }
   }

   public int getFuelCostPerMinute(CenterContext context) {
      return (int)Math.ceil((double)CenterFuelController.baseRadiusCost(context.center().getRadius()) * (double)1.5F);
   }

   public boolean supportsHostileMobSetting() {
      return true;
   }

   public boolean supportsPassiveMobSetting() {
      return true;
   }
}
