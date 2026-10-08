package com.example.immortal_cultivation_mod.client.renderer;

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

public class SystemManifestRenderer extends EntityRenderer<SystemManifestEntity> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "textures/gui/system_windows.png");
   private static final float ATLAS = 1254.0F;
   private static final float U0 = 0.011164274F;
   private static final float V0 = 0.024720892F;
   private static final float U1 = 0.29346094F;
   private static final float V1 = 0.20255183F;

   public SystemManifestRenderer(EntityRendererProvider.Context context) {
      super(context);
   }

   public void render(SystemManifestEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      if (CultivationLevels.getRealmIndex(ClientData.cultivationData.cultivationLevel()) >= 5) {
         poseStack.pushPose();
         poseStack.translate((double)0.0F, 0.9, (double)0.0F);
         poseStack.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
         poseStack.scale(2.3F, 1.45F, 1.0F);
         Matrix4f pose = poseStack.last().pose();
         VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
         vertex(vertices, pose, -0.5F, -0.5F, 0.29346094F, 0.20255183F);
         vertex(vertices, pose, 0.5F, -0.5F, 0.011164274F, 0.20255183F);
         vertex(vertices, pose, 0.5F, 0.5F, 0.011164274F, 0.024720892F);
         vertex(vertices, pose, -0.5F, 0.5F, 0.29346094F, 0.024720892F);
         poseStack.popPose();
         super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
      }
   }

   private static void vertex(VertexConsumer vertices, Matrix4f pose, float x, float y, float u, float v) {
      vertices.addVertex(pose, x, y, 0.0F).setColor(255, 255, 255, 232).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0.0F, 1.0F, 0.0F);
   }

   public ResourceLocation getTextureLocation(SystemManifestEntity entity) {
      return TEXTURE;
   }
}
