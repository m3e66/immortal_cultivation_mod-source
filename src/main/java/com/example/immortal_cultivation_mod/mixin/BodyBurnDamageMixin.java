package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.attachment.BodyTypeEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class BodyBurnDamageMixin {
    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private DamageSource immortalCultivation$bodyBurnOwner(DamageSource source) {
        return BodyTypeEffects.burnSource((LivingEntity)(Object)this, source);
    }
}
