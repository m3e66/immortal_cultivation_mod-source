package com.example.immortal_cultivation_mod.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Entity.class)
public abstract class BodyLightningMixin {
    @Redirect(method = "thunderHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean immortalCultivation$ownedLightning(Entity target, DamageSource source, float amount, ServerLevel level, LightningBolt lightning) {
        return target.hurt(lightning.getCause() == null ? source
                : new DamageSource(source.typeHolder(), lightning, lightning.getCause()), amount);
    }
}
