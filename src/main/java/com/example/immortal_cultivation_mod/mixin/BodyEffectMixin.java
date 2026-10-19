package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.attachment.BodyTypeEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class BodyEffectMixin {
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void immortalCultivation$rejectBodyDebuff(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> ci) {
        if (BodyTypeEffects.rejectEffect((LivingEntity)(Object)this, effect)) ci.setReturnValue(false);
    }
    @Inject(method = "forceAddEffect", at = @At("HEAD"), cancellable = true)
    private void immortalCultivation$rejectForcedBodyDebuff(MobEffectInstance effect, Entity source, CallbackInfo ci) {
        if (BodyTypeEffects.rejectEffect((LivingEntity)(Object)this, effect)) ci.cancel();
    }
    @ModifyVariable(method = {
        "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", "forceAddEffect"
    }, at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private MobEffectInstance immortalCultivation$shortenBodyDebuff(MobEffectInstance effect) {
        return BodyTypeEffects.shortenEffect((LivingEntity)(Object)this, effect) ? immortalCultivation$copyShortened(effect) : effect;
    }
    @Unique
    private static MobEffectInstance immortalCultivation$copyShortened(MobEffectInstance effect) {
        MobEffectInstance copy = new MobEffectInstance(effect);
        var accessor = (BodyEffectAccessor)(Object)copy;
        if (effect.getDuration() > 0) accessor.immortalCultivation$duration(Math.max(1, (int)((long)effect.getDuration() * 3 / 4)));
        MobEffectInstance hidden = ((BodyEffectAccessor)(Object)effect).immortalCultivation$hidden();
        if (hidden != null) accessor.immortalCultivation$hidden(immortalCultivation$copyShortened(hidden));
        return copy;
    }
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("RETURN"))
    private void immortalCultivation$bodyEffectSource(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> ci) {
        if (ci.getReturnValue()) BodyTypeEffects.effectAdded((LivingEntity)(Object)this, effect, source);
    }
    @Inject(method = "forceAddEffect", at = @At("TAIL"))
    private void immortalCultivation$forcedBodyEffectSource(MobEffectInstance effect, Entity source, CallbackInfo ci) {
        BodyTypeEffects.effectAdded((LivingEntity)(Object)this, effect, source);
    }
}
