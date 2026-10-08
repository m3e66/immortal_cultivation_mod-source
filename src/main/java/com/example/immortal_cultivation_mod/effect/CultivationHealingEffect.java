package com.example.immortal_cultivation_mod.effect;

import com.example.immortal_cultivation_mod.particle.ModParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Exact once-per-second healing used by the new cultivation recovery arts. */
public final class CultivationHealingEffect extends MobEffect {
    public enum Kind {
        WOOD_BREATH,
        BLOOD_RETURN
    }

    private final Kind kind;

    public CultivationHealingEffect(Kind kind, int color) {
        super(MobEffectCategory.BENEFICIAL, color);
        this.kind = kind;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        float amount = switch (kind) {
            case WOOD_BREATH -> entity.isOnFire() ? 8.75F : 17.5F;
            case BLOOD_RETURN -> 20.0F;
        };
        amount *= 1.0F + Math.max(0, amplifier) * 0.15F;
        entity.heal(amount);
        if (entity.level() instanceof ServerLevel level) {
            ParticleOptions particle = kind == Kind.WOOD_BREATH
                    ? ModParticles.METHOD_QI_WOOD.get() : ModParticles.METHOD_QI_BLOOD.get();
            level.sendParticles(particle,
                    entity.getX(), entity.getY() + entity.getBbHeight() * 0.55D, entity.getZ(),
                    16, entity.getBbWidth() * 0.45D, entity.getBbHeight() * 0.35D,
                    entity.getBbWidth() * 0.45D, 0.035D);
        }
        return true;
    }
}
