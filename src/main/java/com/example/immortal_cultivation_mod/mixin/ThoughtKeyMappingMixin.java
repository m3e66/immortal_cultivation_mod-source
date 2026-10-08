package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.client.ThoughtInputGuard;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=KeyMapping.class,priority=500)
public abstract class ThoughtKeyMappingMixin {
    @Inject(method="setDown",at=@At("HEAD"),cancellable=true)
    private void immortalCultivation$resistKeyRelease(boolean down,CallbackInfo ci) {
        if(!down && ThoughtInputGuard.keepKeyDown((KeyMapping)(Object)this))ci.cancel();
    }
    @Inject(method="release",at=@At("HEAD"),cancellable=true)
    private void immortalCultivation$resistForcedRelease(CallbackInfo ci) {
        if(ThoughtInputGuard.keepKeyDown((KeyMapping)(Object)this))ci.cancel();
    }
    @Inject(method="isDown",at=@At("RETURN"),cancellable=true)
    private void immortalCultivation$resistFalseKeyState(CallbackInfoReturnable<Boolean> ci) {
        if(!ci.getReturnValue() && ThoughtInputGuard.keepKeyDown((KeyMapping)(Object)this))ci.setReturnValue(true);
    }
    @Inject(method="consumeClick",at=@At("RETURN"),cancellable=true)
    private void immortalCultivation$resistLostClick(CallbackInfoReturnable<Boolean> ci) {
        ci.setReturnValue(ThoughtInputGuard.finishClick((KeyMapping)(Object)this,ci.getReturnValue()));
    }
    @Inject(method="setKey",at=@At("HEAD"),cancellable=true)
    private void immortalCultivation$resistUnbind(InputConstants.Key key,CallbackInfo ci) {
        if(ThoughtInputGuard.guardUnbind((KeyMapping)(Object)this,key))ci.cancel();
    }
}
