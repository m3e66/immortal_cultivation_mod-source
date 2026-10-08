package com.example.immortal_cultivation_mod.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Lethal spiritual corpse fire; unlike vanilla fire, water does not clear it. */
public final class CorpseBurnEffect extends MobEffect {
    private final float damagePerSecond;

    public CorpseBurnEffect(int color, float damagePerSecond) {
        super(MobEffectCategory.HARMFUL, color);
        this.damagePerSecond = damagePerSecond;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) {
            entity.hurt(entity.damageSources().magic(), damagePerSecond * (amplifier + 1));
            if (entity.level() instanceof ServerLevel level) {
                UndeadEffectParticles.corpseBurn(level, entity, damagePerSecond >= 100.0F);
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }
}
