package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.client.BlackFlameClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Collapses vanilla living models toward their center during a 玄焰 death. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(
            method = "setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
            at = @At("HEAD"))
    private void immortalCultivation$blackFlameCollapse(
            LivingEntity entity,
            PoseStack poseStack,
            float ageInTicks,
            float rotationYaw,
            float partialTick,
            float scale,
            CallbackInfo ci) {
        BlackFlameClient.DeathAppearance appearance =
                BlackFlameClient.deathAppearance(entity, partialTick);
        if (appearance != null && appearance.scale() < 0.999F) {
            poseStack.scale(appearance.scale(), appearance.scale(), appearance.scale());
        }
    }
}
