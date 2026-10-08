package com.example.immortal_cultivation_mod.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Explosion.class)
public interface ExternalExplosionAccessor {
    @Accessor("damageSource") DamageSource externalPower$damageSource();
}
