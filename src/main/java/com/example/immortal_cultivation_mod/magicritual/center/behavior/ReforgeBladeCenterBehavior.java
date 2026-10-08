package com.example.immortal_cultivation_mod.magicritual.center.behavior;

import com.example.immortal_cultivation_mod.magicritual.center.CenterContext;
import com.example.immortal_cultivation_mod.magicritual.center.CenterFuelController;
import com.example.immortal_cultivation_mod.magicritual.center.MagicCenterBehavior;

public final class ReforgeBladeCenterBehavior implements MagicCenterBehavior {
   public void serverTick(CenterContext context) {
      if (context.center().shouldTriggerEvery(context.level().getGameTime(), 20)) {
         context.center().repairReforgeWeapon(1);
      }

   }

   public int getFuelCostPerMinute(CenterContext context) {
      return CenterFuelController.baseRadiusCost(context.center().getRadius());
   }
}
