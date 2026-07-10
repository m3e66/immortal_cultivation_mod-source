package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.entity.FireballProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public class ScaledFireballRenderer extends ThrownItemRenderer<FireballProjectileEntity> {
    public ScaledFireballRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(FireballProjectileEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float scale = 1.35F + (entity.chargeScale() - 1.0F) * 1.65F;
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        poseStack.popPose();
    }
}
