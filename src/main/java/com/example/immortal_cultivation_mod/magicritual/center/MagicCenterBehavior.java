package com.example.immortal_cultivation_mod.magicritual.center;

public interface MagicCenterBehavior {
   void serverTick(CenterContext var1);

   default void clientTick(CenterContext context) {
   }

   int getFuelCostPerMinute(CenterContext var1);

   default boolean supportsHostileMobSetting() {
      return false;
   }

   default boolean supportsPassiveMobSetting() {
      return false;
   }

   default boolean supportsPlayerExclusions() {
      return true;
   }
}
