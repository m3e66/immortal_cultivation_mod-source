package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.entity.SystemManifestEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class SystemManifestRenderer
        extends EntityRenderer<SystemManifestEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID,
                    "textures/gui/system_windows.png");
    private static final float ATLAS = 1254.0F;
    private static final float U0 = 14.0F / ATLAS;
    private static final float V0 = 31.0F / ATLAS;
    private static final float U1 = (14.0F + 354.0F) / ATLAS;
    private static final float V1 = (31.0F + 223.0F) / ATLAS;

    public SystemManifestRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(
            SystemManifestEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        if (CultivationLevels.getRealmIndex(
                ClientData.cultivationData.cultivationLevel()) < 5) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.9D, 0.0D);
        poseStack.mulPose(
                Minecraft.getInstance().gameRenderer
                        .getMainCamera().rotation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(2.3F, 1.45F, 1.0F);

        Matrix4f pose = poseStack.last().pose();
        VertexConsumer vertices = buffer.getBuffer(
                RenderType.entityTranslucent(TEXTURE));
        vertex(vertices, pose, -0.5F, -0.5F, U1, V1);
        vertex(vertices, pose, 0.5F, -0.5F, U0, V1);
        vertex(vertices, pose, 0.5F, 0.5F, U0, V0);
        vertex(vertices, pose, -0.5F, 0.5F, U1, V0);
        poseStack.popPose();
        super.render(
                entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void vertex(
            VertexConsumer vertices,
            Matrix4f pose,
            float x,
            float y,
            float u,
            float v) {
        vertices.addVertex(pose, x, y, 0.0F)
                .setColor(255, 255, 255, 232)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0x00F000F0)
                .setNormal(0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(SystemManifestEntity entity) {
        return TEXTURE;
    }
}
