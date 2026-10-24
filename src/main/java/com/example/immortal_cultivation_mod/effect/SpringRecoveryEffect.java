package com.example.immortal_cultivation_mod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** One second of spring recovery; amplifier stores the spell's authored cultivation stage. */
public final class SpringRecoveryEffect extends MobEffect {
    public SpringRecoveryEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xF82423);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        entity.heal(4.0F * com.example.immortal_cultivation_mod.spell.HealingTechniques
                .realmHealingScale(amplifier));
        return true;
    }
}
