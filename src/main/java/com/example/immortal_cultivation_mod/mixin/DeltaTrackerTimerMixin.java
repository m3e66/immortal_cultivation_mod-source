package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.client.TemporalSlowflowClient;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Quantizes only rendered interpolation; client ticks, input, and networking keep running. */
@Mixin(DeltaTracker.Timer.class)
public abstract class DeltaTrackerTimerMixin {
    @Inject(
            method = "getGameTimeDeltaPartialTick",
            at = @At("RETURN"),
            cancellable = true)
    private void immortalCultivation$holdTemporalFrames(
            boolean runsNormally,
            CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(TemporalSlowflowClient.quantizePartialTick(
                cir.getReturnValue()));
    }
}
