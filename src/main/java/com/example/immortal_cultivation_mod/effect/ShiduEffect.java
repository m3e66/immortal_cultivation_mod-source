package com.example.immortal_cultivation_mod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class ShiduEffect extends MobEffect {
    public ShiduEffect() {
        super(MobEffectCategory.HARMFUL, 0x2F7F29);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        entity.hurt(entity.damageSources().magic(), 5.0F * (amplifier + 1));
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int interval = Math.max(5, 25 >> amplifier);
        return duration % interval == 0;
    }
}
