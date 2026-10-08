package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.client.flame.BurningFlameVisuals;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class BurningFlameRenderMixin {
    @Inject(method = "renderFlame", at = @At("HEAD"), cancellable = true)
    private void cultivation$burnFlame(PoseStack poses, MultiBufferSource buffers, Entity entity,
            Quaternionf rotation, CallbackInfo callback) {
        if (entity instanceof LivingEntity living && BurningFlameVisuals.color(living) != -1) {
            BurningFlameVisuals.render(poses, buffers, living, rotation);
            callback.cancel();
        }
    }
}
