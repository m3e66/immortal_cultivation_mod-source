package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.client.ThoughtInputGuard;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=KeyboardInput.class,priority=500)
public abstract class ThoughtKeyboardInputMixin {
    @Unique private ThoughtInputGuard.Movement immortalCultivation$physicalMovement;
    @Inject(method="tick",at=@At("HEAD"))
    private void immortalCultivation$capturePhysicalInput(boolean slow,float factor,CallbackInfo ci) {
        immortalCultivation$physicalMovement=ThoughtInputGuard.keyboardMovement(slow,factor);
    }
    @Inject(method="tick",at=@At("RETURN"))
    private void immortalCultivation$restoreSuppressedInput(boolean slow,float factor,CallbackInfo ci) {
        ThoughtInputGuard.restoreMovement(immortalCultivation$physicalMovement,(Input)(Object)this);
    }
}
