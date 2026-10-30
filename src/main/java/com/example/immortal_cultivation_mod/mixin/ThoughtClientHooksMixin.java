package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.client.ThoughtInputGuard;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.Input;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.InputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Runs after the whole event dispatch, including listeners registered at LOWEST priority. */
@Mixin(value=ClientHooks.class,remap=false,priority=500)
public abstract class ThoughtClientHooksMixin {
    @Unique private static final ThreadLocal<ThoughtInputGuard.Movement> immortalCultivation$movement=new ThreadLocal<>();
    @Inject(method="onMovementInputUpdate",at=@At("HEAD"))
    private static void immortalCultivation$captureMovement(Player player,Input input,CallbackInfo ci) {
        immortalCultivation$movement.set(ThoughtInputGuard.Movement.capture(input));
    }
    @Inject(method="onMovementInputUpdate",at=@At("RETURN"))
    private static void immortalCultivation$restoreMovement(Player player,Input input,CallbackInfo ci) {
        var before=immortalCultivation$movement.get();immortalCultivation$movement.remove();
        ThoughtInputGuard.restoreMovement(before,input);
    }
    @Inject(method="onClickInput",at=@At("RETURN"))
    private static void immortalCultivation$restoreInteraction(int button,KeyMapping key,InteractionHand hand,
            CallbackInfoReturnable<InputEvent.InteractionKeyMappingTriggered> ci) {
        ThoughtInputGuard.finishInteraction(ci.getReturnValue());
    }
    @Inject(method="onMouseButtonPre",at=@At("RETURN"),cancellable=true)
    private static void immortalCultivation$restoreMouse(int button,int action,int mods,CallbackInfoReturnable<Boolean> ci) {
        ThoughtInputGuard.onKeyEvent(com.mojang.blaze3d.platform.InputConstants.Type.MOUSE.getOrCreate(button), action);
        if(ci.getReturnValue() && action==1 && ThoughtInputGuard.resist())ci.setReturnValue(false);
    }
}
