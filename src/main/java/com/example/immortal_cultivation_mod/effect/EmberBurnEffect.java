package com.example.immortal_cultivation_mod.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Three-second spiritual flame used by the Ember-Feather Fire Owl's heat burst. */
public final class EmberBurnEffect extends MobEffect {
    private static final float DAMAGE_PER_SECOND = 30.0F;

    public EmberBurnEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF6A20);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) {
            entity.hurt(entity.damageSources().inFire(), DAMAGE_PER_SECOND * (amplifier + 1));
            if (entity.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.FLAME,
                        entity.getX(), entity.getY() + entity.getBbHeight() * 0.55D, entity.getZ(),
                        9, entity.getBbWidth() * 0.35D, entity.getBbHeight() * 0.25D,
                        entity.getBbWidth() * 0.35D, 0.035D);
                level.sendParticles(ParticleTypes.SMOKE,
                        entity.getX(), entity.getY() + entity.getBbHeight() * 0.65D, entity.getZ(),
                        3, entity.getBbWidth() * 0.25D, entity.getBbHeight() * 0.18D,
                        entity.getBbWidth() * 0.25D, 0.015D);
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }
}
