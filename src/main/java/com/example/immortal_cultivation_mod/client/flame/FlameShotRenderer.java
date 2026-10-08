package com.example.immortal_cultivation_mod.client.flame;

import com.example.immortal_cultivation_mod.client.ShaderPackCompatibility;
import com.example.immortal_cultivation_mod.flame.FlameShotEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class FlameShotRenderer extends EntityRenderer<FlameShotEntity> {
    public FlameShotRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    @Override
    public void render(FlameShotEntity entity, float yaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight) {
        if (ShaderPackCompatibility.isRenderingShadowPass()) return;
        float size = 0.42F * (1 + Mth.sin((entity.tickCount + partialTick) * 0.55F) * 0.06F);
        poseStack.pushPose();
        poseStack.translate(0, entity.getBbHeight() * 0.5F, 0);
        if (entity.charge() > 0) {
            ChargedFlameRenderer.projectile(entity, partialTick, poseStack, buffers);
        } else {
            poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
            FlameRenderTypes.billboard(buffers.getBuffer(FlameRenderTypes.entity(entity.kind())), poseStack.last().pose(),
                    size, 1.0F);
        }
        poseStack.popPose();
        if (entity.charge() > 0) ChargedFlameRenderer.trail(entity, partialTick, poseStack, buffers);
        super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override public boolean shouldRender(FlameShotEntity entity, net.minecraft.client.renderer.culling.Frustum frustum,
            double x, double y, double z) {
        return entity.shouldRender(x,y,z) && frustum.isVisible(entity.getBoundingBox().inflate(entity.charge()>0 ? 6 : .5));
    }

    @Override
    public ResourceLocation getTextureLocation(FlameShotEntity entity) {
        return FlameRenderTypes.texture(entity.kind());
    }
}
