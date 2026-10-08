package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.client.model.PhantomVeilFoxCloneModel;
import com.example.immortal_cultivation_mod.entity.PhantomVeilFoxCloneEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class PhantomVeilFoxCloneRenderer
        extends GeoEntityRenderer<PhantomVeilFoxCloneEntity> {
    public PhantomVeilFoxCloneRenderer(EntityRendererProvider.Context context) {
        super(context, new PhantomVeilFoxCloneModel());
        withScale(1.0F);
        shadowRadius = 0.18F;
    }

    @Override
    public RenderType getRenderType(PhantomVeilFoxCloneEntity animatable,
                                    ResourceLocation texture,
                                    MultiBufferSource bufferSource,
                                    float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public void render(PhantomVeilFoxCloneEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        RenderSystem.setShaderColor(0.86F, 0.68F, 1.0F, 0.58F);
        poseStack.pushPose();
        float scale = entity.renderScale();
        poseStack.scale(scale, scale, scale);
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        poseStack.popPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
