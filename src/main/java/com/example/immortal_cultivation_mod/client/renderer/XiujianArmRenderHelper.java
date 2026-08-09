package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.item.XiujianItemRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class XiujianArmRenderHelper {
    private static final float ARM_SCALE = 0.48F;

    private XiujianArmRenderHelper() {
    }

    public static void render(
            XiujianItemRenderer renderer,
            ItemStack stack,
            HumanoidModel<?> model,
            HumanoidArm armSide,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        ModelPart arm = armSide == HumanoidArm.RIGHT
                ? model.rightArm
                : model.leftArm;
        arm.translateAndRotate(poseStack);
        poseStack.translate(0.2D, 0.0D, -0.05D);
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        poseStack.scale(ARM_SCALE, ARM_SCALE, ARM_SCALE);
        renderer.renderByItem(
                stack,
                ItemDisplayContext.FIXED,
                poseStack,
                buffer,
                packedLight,
                OverlayTexture.NO_OVERLAY);
    }
}
