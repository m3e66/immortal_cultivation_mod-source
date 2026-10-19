package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.attachment.BodyTypeEffects;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class BodyBurnResetMixin {
    @Inject(method = "setRemainingFireTicks", at = @At("HEAD"))
    private void immortalCultivation$extinguishBodyBurn(int ticks, CallbackInfo ci) {
        if (ticks <= 0) ((Entity)(Object)this).getPersistentData().remove(BodyTypeEffects.BURN_OWNER);
    }
}
