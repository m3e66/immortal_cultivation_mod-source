package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.entity.FlyingSwordEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;

public class FlyingSwordRenderer extends EntityRenderer<FlyingSwordEntity> {
    public FlyingSwordRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(FlyingSwordEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        if (entity.isControlled()) {
            float angle = entity.getFormationAngle();

            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getYRot()));
            poseStack.mulPose(Axis.ZP.rotationDegrees(angle));
            poseStack.translate(0.0F, 0.01F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(260.0F));
            poseStack.scale(0.68F, 0.68F, 1.0F);
        } else if (entity.isShot() || entity.isStuck()) {
            float yaw = entity.isStuck() ? entity.getStuckYaw() : entity.getYRot();
            float pitch = entity.isStuck() ? entity.getStuckPitch() : entity.getXRot();

            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch - 90.0F));

            float embedDepth = entity.isStuck() ? 0.35F : 0.0F;
            poseStack.translate(0.0F, 0.0F, -embedDepth);
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getYRot()));
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        }

        poseStack.scale(4.25F, 4.25F, 1.0F);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                entity.getItem(),
                ItemDisplayContext.FIXED,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId()
        );

        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(FlyingSwordEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
