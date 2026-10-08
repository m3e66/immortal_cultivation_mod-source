package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.entity.YaoguangFlashEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public final class YaoguangFlashRenderer extends EntityRenderer<YaoguangFlashEntity> {
   private static final ResourceLocation WHITE_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");

   public YaoguangFlashRenderer(EntityRendererProvider.Context context) {
      super(context);
      this.shadowRadius = 0.0F;
   }

   public void render(YaoguangFlashEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      try (var support = com.example.immortal_cultivation_mod.client.SpellGlowBuffers.support(com.example.immortal_cultivation_mod.client.SpellRibbonRenderer.spell(entity) != null)) {
      float age = (float)entity.tickCount + partialTick;
      float progress = Mth.clamp(age / 8.0F, 0.0F, 1.0F);
      float eased = 1.0F - (1.0F - progress) * (1.0F - progress) * (1.0F - progress);
      float radius = 0.35F + eased * (entity.radius() - 0.35F);
      float fade = 1.0F - Mth.clamp((age - 2.0F) / 8.0F, 0.0F, 1.0F);
      float pulse = 0.92F + 0.08F * Mth.sin(age * 1.8F);
      poseStack.pushPose();
      renderSphere(poseStack, buffer, radius, 1.0F, 0.92F, 0.56F, 0.19F * fade * pulse);
      renderSphere(poseStack, buffer, radius * 0.94F, 1.0F, 1.0F, 0.88F, 0.075F * fade);
      renderEquator(poseStack, buffer, radius * 1.015F, 1.0F, 0.78F, 0.18F, 0.72F * fade);
      poseStack.popPose();
      super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
   }
   }

   private static void renderSphere(PoseStack poseStack, MultiBufferSource buffer, float radius, float red, float green, float blue, float alpha) {
      Matrix4f pose = poseStack.last().pose();
      VertexConsumer vertices = com.example.immortal_cultivation_mod.client.SpellGlowBuffers.get(buffer);
      int longitudeSegments = 24;
      int latitudeSegments = 12;

      for(int latitudeIndex = 0; latitudeIndex < latitudeSegments; ++latitudeIndex) {
         float latitude0 = (-(float)Math.PI / 2F) + (float)Math.PI * (float)latitudeIndex / (float)latitudeSegments;
         float latitude1 = (-(float)Math.PI / 2F) + (float)Math.PI * (float)(latitudeIndex + 1) / (float)latitudeSegments;
         float v0 = (float)latitudeIndex / (float)latitudeSegments;
         float v1 = (float)(latitudeIndex + 1) / (float)latitudeSegments;

         for(int longitudeIndex = 0; longitudeIndex < longitudeSegments; ++longitudeIndex) {
            float longitude0 = ((float)Math.PI * 2F) * (float)longitudeIndex / (float)longitudeSegments;
            float longitude1 = ((float)Math.PI * 2F) * (float)(longitudeIndex + 1) / (float)longitudeSegments;
            float u0 = (float)longitudeIndex / (float)longitudeSegments;
            float u1 = (float)(longitudeIndex + 1) / (float)longitudeSegments;
            sphereVertex(vertices, pose, radius, latitude0, longitude0, u0, v0, red, green, blue, alpha);
            sphereVertex(vertices, pose, radius, latitude1, longitude0, u0, v1, red, green, blue, alpha);
            sphereVertex(vertices, pose, radius, latitude1, longitude1, u1, v1, red, green, blue, alpha);
            sphereVertex(vertices, pose, radius, latitude0, longitude1, u1, v0, red, green, blue, alpha);
         }
      }

   }

   private static void renderEquator(PoseStack poseStack, MultiBufferSource buffer, float radius, float red, float green, float blue, float alpha) {
      Matrix4f pose = poseStack.last().pose();
      VertexConsumer vertices = com.example.immortal_cultivation_mod.client.SpellGlowBuffers.get(buffer);
      float halfWidth = 0.055F;

      for(int segment = 0; segment < 48; ++segment) {
         float angle0 = ((float)Math.PI * 2F) * (float)segment / 48.0F;
         float angle1 = ((float)Math.PI * 2F) * (float)(segment + 1) / 48.0F;
         vertex(vertices, pose, Mth.cos(angle0) * (radius - halfWidth), 0.0F, Mth.sin(angle0) * (radius - halfWidth), red, green, blue, alpha);
         vertex(vertices, pose, Mth.cos(angle1) * (radius - halfWidth), 0.0F, Mth.sin(angle1) * (radius - halfWidth), red, green, blue, alpha);
         vertex(vertices, pose, Mth.cos(angle1) * (radius + halfWidth), 0.0F, Mth.sin(angle1) * (radius + halfWidth), red, green, blue, alpha);
         vertex(vertices, pose, Mth.cos(angle0) * (radius + halfWidth), 0.0F, Mth.sin(angle0) * (radius + halfWidth), red, green, blue, alpha);
      }

   }

   private static void sphereVertex(VertexConsumer vertices, Matrix4f pose, float radius, float latitude, float longitude, float u, float v, float red, float green, float blue, float alpha) {
      float cosLatitude = Mth.cos(latitude);
      float normalX = cosLatitude * Mth.cos(longitude);
      float normalY = Mth.sin(latitude);
      float normalZ = cosLatitude * Mth.sin(longitude);
      vertices.addVertex(pose, normalX * radius, normalY * radius, normalZ * radius).setColor(red, green, blue, alpha).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(normalX, normalY, normalZ);
   }

   private static void vertex(VertexConsumer vertices, Matrix4f pose, float x, float y, float z, float red, float green, float blue, float alpha) {
      vertices.addVertex(pose, x, y, z).setColor(red, green, blue, alpha).setUv(0.5F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0.0F, 1.0F, 0.0F);
   }

   public ResourceLocation getTextureLocation(YaoguangFlashEntity entity) {
      return WHITE_TEXTURE;
   }
}
