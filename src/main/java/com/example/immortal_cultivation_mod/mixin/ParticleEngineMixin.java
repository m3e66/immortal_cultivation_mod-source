package com.example.immortal_cultivation_mod.mixin;

import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {
    private static final int MOD_PARTICLES_PER_LAYER = 65_536;

    @ModifyConstant(method = "lambda$tick$11", constant = @Constant(intValue = 16_384))
    private static int immortalCultivation$increaseParticleCap(int original) {
        return MOD_PARTICLES_PER_LAYER;
    }
}
