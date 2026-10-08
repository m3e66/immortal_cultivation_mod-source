package com.example.immortal_cultivation_mod.client.flame;

import com.example.immortal_cultivation_mod.client.ShaderPackCompatibility;
import com.example.immortal_cultivation_mod.flame.SpiritFlameEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Floating energy geometry; animation never moves the stationary server capture target. */
public final class SpiritFlameRenderer extends EntityRenderer<SpiritFlameEntity> {
    public SpiritFlameRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    @Override
    public void render(SpiritFlameEntity entity, float yaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight) {
        if (ShaderPackCompatibility.isRenderingShadowPass()) return;
        float time = entity.level().getGameTime() % 240 + partialTick
                + Math.floorMod(entity.getUUID().hashCode(), 240);
        float bob = FlameVfxRenderer.bob(entity.kind(), time);
        double distance = entity.distanceToSqr(entityRenderDispatcher.camera.getPosition());
        if (distance > 48 * 48) return;
        poseStack.pushPose();
        poseStack.translate(0, 0.65F + bob, 0);
        FlameVfxRenderer.render(entity.kind(), time, poseStack, buffers, distance, false);
        FlameHeatHaze.observe(entity.kind(), entity.position().add(0, .65 + bob, 0));
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SpiritFlameEntity entity) {
        return FlameRenderTypes.texture(entity.kind());
    }
}
