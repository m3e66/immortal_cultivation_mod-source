package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.client.CultivationFlightTrailRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class FlightTrailEntityRenderMixin {
    @Inject(
            method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            cancellable = true)
    private void immortalCultivation$hideFlightTrailPlayer(
            Entity entity,
            double x,
            double y,
            double z,
            float rotationYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            CallbackInfo ci) {
        // Skip the dispatcher as well as render layers so no shadow, fire or name remains.
        if (entity instanceof Player player
                && CultivationFlightTrailRenderer.shouldHidePlayer(player)) {
            ci.cancel();
        }
    }
}
