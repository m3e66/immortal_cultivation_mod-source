package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.combat.ThoughtControlGuard;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Covers direct effect queries used by other mods, recovery and the forceAddEffect bypass. */
@Mixin(LivingEntity.class)
public abstract class ThoughtControlEffectMixin {
    @Shadow @Final private Map<Holder<MobEffect>, MobEffectInstance> activeEffects;

    @Inject(method = "hasEffect", at = @At("HEAD"), cancellable = true)
    private void immortalCultivation$thoughtsBeforeEffectCheck(Holder<MobEffect> effect, CallbackInfoReturnable<Boolean> ci) {
        if (ThoughtControlGuard.shouldSuppress((LivingEntity)(Object)this, activeEffects.get(effect))) ci.setReturnValue(false);
    }

    @Inject(method = "getEffect", at = @At("HEAD"), cancellable = true)
    private void immortalCultivation$thoughtsBeforeEffectRead(Holder<MobEffect> effect, CallbackInfoReturnable<MobEffectInstance> ci) {
        if (ThoughtControlGuard.shouldSuppress((LivingEntity)(Object)this, activeEffects.get(effect))) ci.setReturnValue(null);
    }

    @Inject(method = "forceAddEffect", at = @At("HEAD"), cancellable = true)
    private void immortalCultivation$thoughtsBeforeForcedEffect(MobEffectInstance effect, Entity source, CallbackInfo ci) {
        LivingEntity target = (LivingEntity)(Object)this;
        if (!target.level().isClientSide && ThoughtControlGuard.resistEffect(target, effect, source)) ci.cancel();
    }
}
