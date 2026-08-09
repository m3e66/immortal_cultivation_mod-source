package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.entity.FlyingNeedleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

public class FlyingNeedleRenderer
        extends EntityRenderer<FlyingNeedleEntity> {
    public FlyingNeedleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(
            FlyingNeedleEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        poseStack.pushPose();
        Vec3 movement = entity.getDeltaMovement();
        if (movement.lengthSqr() > 0.0001D) {
            double horizontal = Math.sqrt(
                    movement.x * movement.x
                            + movement.z * movement.z);
            float yaw = (float) (Mth.atan2(
                    -movement.x, movement.z)
                    * (180.0D / Math.PI));
            float pitch = (float) (Mth.atan2(
                    movement.y, horizontal)
                    * (180.0D / Math.PI));
            poseStack.mulPose(
                    Axis.YP.rotationDegrees(180.0F - yaw));
            poseStack.mulPose(
                    Axis.XP.rotationDegrees(pitch - 90.0F));
        }
        poseStack.scale(1.35F, 1.35F, 0.7F);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                entity.getItem(),
                ItemDisplayContext.FIXED,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId());
        poseStack.popPose();
        super.render(
                entity,
                entityYaw,
                partialTick,
                poseStack,
                buffer,
                packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(
            FlyingNeedleEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
