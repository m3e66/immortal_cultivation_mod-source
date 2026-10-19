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
        withScale(2.0F);
        shadowRadius = 0.36F;
    }

    @Override
    public software.bernie.geckolib.util.Color getRenderColor(PhantomVeilFoxCloneEntity entity, float partialTick, int packedLight) {
        float fade = Math.min(1, (entity.tickCount + partialTick) / 8F);
        float variation = .42F + .06F * (float)Math.sin((entity.tickCount + partialTick) * .08 + entity.getId());
        return software.bernie.geckolib.util.Color.ofRGBA(.91F, .94F, .93F, variation * fade);
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
        poseStack.pushPose();
        float scale = entity.renderScale();
        poseStack.scale(scale, scale, scale);
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        poseStack.popPose();
    }
}
