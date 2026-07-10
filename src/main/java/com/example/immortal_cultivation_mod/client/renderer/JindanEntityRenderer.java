package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.JindanEntity;
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

public class JindanEntityRenderer extends EntityRenderer<JindanEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/jindan_button.png");

    public JindanEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(JindanEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 1.15D, 0.0D);
        poseStack.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(1.35F, 1.35F, 1.35F);

        Matrix4f pose = poseStack.last().pose();
        VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        vertex(vertices, pose, -0.5F, -0.5F, 0.0F, 1.0F, 1.0F, packedLight);
        vertex(vertices, pose, 0.5F, -0.5F, 0.0F, 0.0F, 1.0F, packedLight);
        vertex(vertices, pose, 0.5F, 0.5F, 0.0F, 0.0F, 0.0F, packedLight);
        vertex(vertices, pose, -0.5F, 0.5F, 0.0F, 1.0F, 0.0F, packedLight);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f pose, float x, float y, float z, float u, float v, int light) {
        vertices.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 245)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(JindanEntity entity) {
        return TEXTURE;
    }
}
