package com.example.immortal_cultivation_mod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;

public class ShiduEffect extends MobEffect {
    public ShiduEffect() {
        super(MobEffectCategory.HARMFUL, 0x2F7F29);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        entity.hurt(com.example.immortal_cultivation_mod.attachment.BodyTypeEffects.poisonSource(entity, entity.damageSources().magic()), 4.0F * (amplifier + 1));
        if (entity.level() instanceof ServerLevel level) {
            UndeadEffectParticles.corpsePoison(level, entity);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }
}
