package com.example.immortal_cultivation_mod.magicritual.effect.custom;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class BleedingEffect extends MobEffect {
   public BleedingEffect(MobEffectCategory category, int color) {
      super(category, color);
   }

   public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
      return duration % 20 == 0;
   }

   public boolean applyEffectTick(LivingEntity entity, int amplifier) {
      entity.hurt(entity.damageSources().magic(), 2.0F);
      return true;
   }
}
