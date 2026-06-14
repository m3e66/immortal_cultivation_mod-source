package com.example.immortal_cultivation_mod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.example.immortal_cultivation_mod.client.model.ZhenshanPalmModel;
import com.example.immortal_cultivation_mod.entity.ZhenshanPalmEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ZhenshanPalmRenderer extends GeoEntityRenderer<ZhenshanPalmEntity> {
    private static final float MODEL_WIDTH_UNITS = 45.0F;
    private static final float MODEL_HEIGHT_UNITS = 56.0F;
    private static final float MODEL_UNITS_PER_BLOCK = 16.0F;

    public ZhenshanPalmRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ZhenshanPalmModel());
        withScale(particleHandWidthScale(), particleHandHeightScale());
        this.shadowRadius = 0.0F;
    }

    @Override
    protected void applyRotations(ZhenshanPalmEntity animatable, PoseStack poseStack, float ageInTicks,
                                  float rotationYaw, float partialTick, float nativeScale) {
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
        poseStack.mulPose(Axis.XP.rotationDegrees(animatable.getXRot()));
    }

    private static float particleHandWidthScale() {
        double particleWidth = (ZhenshanPalmEntity.HAND_POINT_MAX_X - ZhenshanPalmEntity.HAND_POINT_MIN_X)
                * ZhenshanPalmEntity.HAND_WIDTH_SCALE;
        return (float) (particleWidth / (MODEL_WIDTH_UNITS / MODEL_UNITS_PER_BLOCK));
    }

    private static float particleHandHeightScale() {
        double particleHeight = (ZhenshanPalmEntity.HAND_POINT_MAX_Y - ZhenshanPalmEntity.HAND_POINT_MIN_Y)
                * ZhenshanPalmEntity.HAND_HEIGHT_SCALE;
        return (float) (particleHeight / (MODEL_HEIGHT_UNITS / MODEL_UNITS_PER_BLOCK));
    }
}
