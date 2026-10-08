package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.world.CosmicVoidGravity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Scales the real vanilla gravity value instead of correcting motion afterward. */
@Mixin(Entity.class)
public abstract class EntityGravityMixin {
    @Inject(
            method = "getGravity()D",
            at = @At("RETURN"),
            cancellable = true)
    private void immortalCultivation$cosmicVoidGravity(
            CallbackInfoReturnable<Double> callback) {
        Entity entity = (Entity)(Object)this;
        callback.setReturnValue(CosmicVoidGravity.adjustGravity(
                entity, callback.getReturnValue()));
    }
}
