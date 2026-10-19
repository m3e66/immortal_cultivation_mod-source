package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.attachment.BodyTypeEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.world.effect.PoisonMobEffect")
public abstract class BodyPoisonMixin {
    @Redirect(method = "applyEffectTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean immortalCultivation$poisonAttacker(LivingEntity target, DamageSource source, float amount) {
        return target.hurt(BodyTypeEffects.poisonSource(target, source), amount);
    }
}
