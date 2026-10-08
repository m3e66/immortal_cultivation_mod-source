package com.example.immortal_cultivation_mod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** 潮湿 is a non-damaging elemental setup state used by water and ice techniques. */
public final class DampEffect extends MobEffect {
    public DampEffect() {
        super(MobEffectCategory.NEUTRAL, 0x2F88C9);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 10 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.isOnFire()) {
            entity.clearFire();
            entity.setRemainingFireTicks(0);
        }
        return true;
    }
}
