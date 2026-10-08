package com.example.immortal_cultivation_mod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Marks a critically wounded body as stable enough to accept emergency healing. */
public final class StabilizedEffect extends MobEffect {
    public StabilizedEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xB54A42);
    }
}
