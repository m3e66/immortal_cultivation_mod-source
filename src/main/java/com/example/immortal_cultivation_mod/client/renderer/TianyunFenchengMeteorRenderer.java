package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.entity.TianyunFenchengMeteorEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public final class TianyunFenchengMeteorRenderer extends EntityRenderer<TianyunFenchengMeteorEntity> {
   private static final ResourceLocation WHITE_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");
   private static final ResourceLocation MAGMA_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/magma.png");

   public TianyunFenchengMeteorRenderer(EntityRendererProvider.Context context) {
      super(context);
      this.shadowRadius = 0.0F;
   }

   public void render(TianyunFenchengMeteorEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      try (var support = com.example.immortal_cultivation_mod.client.SpellGlowBuffers.support(com.example.immortal_cultivation_mod.client.SpellRibbonRenderer.spell(entity) != null)) {
      float age = (float)entity.tickCount + partialTick;
      if (entity.impacted()) {
         renderAftermath(entity, poseStack, buffer);
      } else {
         renderWarning(entity, age, partialTick, poseStack, buffer);
         renderMeteor(entity, age, poseStack, buffer);
      }

      super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
   }
   }

   private static void renderMeteor(TianyunFenchengMeteorEntity entity, float age, PoseStack poseStack, MultiBufferSource buffer) {
      float formation = Mth.clamp((age + 1.0F) / 10.0F, 0.18F, 1.0F);
      float radius = entity.meteorRadius() * formation * .45F * com.example.immortal_cultivation_mod.client.SpellProjectileVisuals.SCALE;
      float pulse = 0.93F + Mth.sin(age * 0.82F) * 0.07F;
      poseStack.pushPose();
      poseStack.mulPose(Axis.YP.rotationDegrees(age * 4.8F));
      poseStack.mulPose(Axis.XP.rotationDegrees(13.0F + Mth.sin(age * 0.08F) * 7.0F));
      renderRockSphere(poseStack, buffer, radius, age);
      renderSphere(poseStack, buffer, WHITE_TEXTURE, radius * 1.055F, 1.0F, 0.16F, 0.015F, 0.19F * pulse);
      renderSphere(poseStack, buffer, WHITE_TEXTURE, radius * 0.92F, 1.0F, 0.64F, 0.12F, 0.11F * pulse);
      poseStack.popPose();
      poseStack.pushPose();
      renderFireTail(poseStack, buffer, radius, age);
      poseStack.popPose();
   }

   private static void renderWarning(TianyunFenchengMeteorEntity entity, float age, float partialTick, PoseStack poseStack, MultiBufferSource buffer) {
      if (!(age < (float)entity.warningStartTick())) {
         float currentY = (float)Mth.lerp((double)partialTick, entity.yo, entity.getY());
         float relativeGroundY = (float)entity.targetY() - currentY + 0.045F;
         float warningProgress = Mth.clamp((age - (float)entity.warningStartTick()) / Math.max(1.0F, (float)(entity.impactTick() - entity.warningStartTick())), 0.0F, 1.0F);
         float pulse = 0.78F + 0.22F * Mth.sin(age * 1.45F);
         float radius = entity.blastRadius();
         poseStack.pushPose();
         poseStack.translate((double)0.0F, (double)relativeGroundY, (double)0.0F);
         renderDisc(poseStack, buffer, radius, 0.95F, 0.025F, 0.0F, (0.07F + warningProgress * 0.08F) * pulse);
         renderRing(poseStack, buffer, radius, 0.17F, 1.0F, 0.16F, 0.015F, 0.88F * pulse);
         renderRing(poseStack, buffer, radius * 0.72F, 0.075F, 1.0F, 0.44F, 0.04F, 0.58F * pulse);
         renderRing(poseStack, buffer, radius * (0.24F + 0.22F * warningProgress), 0.09F, 1.0F, 0.78F, 0.18F, 0.74F * pulse);
         renderRadials(poseStack, buffer, radius, age, 1.0F, 0.21F, 0.02F, 0.62F * pulse);
         renderSkyColumn(poseStack, buffer, radius * 0.88F, Math.min(38.0F, Math.max(14.0F, -relativeGroundY)), 1.0F, 0.035F, 0.0F, 0.032F + warningProgress * 0.026F);
         poseStack.popPose();
      }
   }

   private static void renderAftermath(TianyunFenchengMeteorEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
      int lifetime = entity.fullyCharged() ? 220 : 120;
      float fade = 1.0F - Mth.clamp((float)entity.impactAge() / (float)lifetime, 0.0F, 1.0F);
      float pulse = 0.78F + Mth.sin((float)entity.tickCount * 0.36F) * 0.22F;
      float radius = entity.blastRadius();
      poseStack.pushPose();
      poseStack.translate((double)0.0F, 0.06, (double)0.0F);
      renderRing(poseStack, buffer, radius * 0.88F, 0.08F, 0.95F, 0.08F, 0.0F, 0.36F * fade * pulse);
      renderCracks(poseStack, buffer, radius, entity.getId(), 1.0F, 0.12F, 0.0F, 0.82F * fade * pulse);
      poseStack.popPose();
   }

   private static void renderRockSphere(PoseStack poseStack, MultiBufferSource buffer, float radius, float age) {
      Matrix4f pose = poseStack.last().pose();
      VertexConsumer vertices = buffer.getBuffer(RenderType.entityCutoutNoCull(MAGMA_TEXTURE));
      int longitudes = 24;
      int latitudes = 12;

      for(int latitude = 0; latitude < latitudes; ++latitude) {
         float lat0 = (-(float)Math.PI / 2F) + (float)Math.PI * (float)latitude / (float)latitudes;
         float lat1 = (-(float)Math.PI / 2F) + (float)Math.PI * (float)(latitude + 1) / (float)latitudes;
         float v0 = (float)latitude / (float)latitudes;
         float v1 = (float)(latitude + 1) / (float)latitudes;

         for(int longitude = 0; longitude < longitudes; ++longitude) {
            float lon0 = ((float)Math.PI * 2F) * (float)longitude / (float)longitudes;
            float lon1 = ((float)Math.PI * 2F) * (float)(longitude + 1) / (float)longitudes;
            float u0 = (float)longitude / (float)longitudes;
            float u1 = (float)(longitude + 1) / (float)longitudes;
            rockVertex(vertices, pose, radius, lat0, lon0, u0, v0, age);
            rockVertex(vertices, pose, radius, lat1, lon0, u0, v1, age);
            rockVertex(vertices, pose, radius, lat1, lon1, u1, v1, age);
            rockVertex(vertices, pose, radius, lat0, lon1, u1, v0, age);
         }
      }

   }

   private static void rockVertex(VertexConsumer vertices, Matrix4f pose, float radius, float latitude, float longitude, float u, float v, float age) {
      float cosLatitude = Mth.cos(latitude);
      float nx = cosLatitude * Mth.cos(longitude);
      float ny = Mth.sin(latitude);
      float nz = cosLatitude * Mth.sin(longitude);
      float roughness = 0.91F + 0.09F * Mth.sin(longitude * 5.0F + latitude * 7.0F + age * 0.025F);
      vertices.addVertex(pose, nx * radius * roughness, ny * radius * roughness, nz * radius * roughness).setColor(1.0F, 0.58F, 0.35F, 1.0F).setUv(u * 2.0F, v * 2.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(nx, ny, nz);
   }

   private static void renderSphere(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation texture, float radius, float red, float green, float blue, float alpha) {
      Matrix4f pose = poseStack.last().pose();
      VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucentEmissive(texture));
      int longitudes = 20;
      int latitudes = 10;

      for(int latitude = 0; latitude < latitudes; ++latitude) {
         float lat0 = (-(float)Math.PI / 2F) + (float)Math.PI * (float)latitude / (float)latitudes;
         float lat1 = (-(float)Math.PI / 2F) + (float)Math.PI * (float)(latitude + 1) / (float)latitudes;

         for(int longitude = 0; longitude < longitudes; ++longitude) {
            float lon0 = ((float)Math.PI * 2F) * (float)longitude / (float)longitudes;
            float lon1 = ((float)Math.PI * 2F) * (float)(longitude + 1) / (float)longitudes;
            sphereVertex(vertices, pose, radius, lat0, lon0, (float)longitude / (float)longitudes, (float)latitude / (float)latitudes, red, green, blue, alpha);
            sphereVertex(vertices, pose, radius, lat1, lon0, (float)longitude / (float)longitudes, (float)(latitude + 1) / (float)latitudes, red, green, blue, alpha);
            sphereVertex(vertices, pose, radius, lat1, lon1, (float)(longitude + 1) / (float)longitudes, (float)(latitude + 1) / (float)latitudes, red, green, blue, alpha);
            sphereVertex(vertices, pose, radius, lat0, lon1, (float)(longitude + 1) / (float)longitudes, (float)latitude / (float)latitudes, red, green, blue, alpha);
         }
      }

   }

   private static void sphereVertex(VertexConsumer vertices, Matrix4f pose, float radius, float latitude, float longitude, float u, float v, float red, float green, float blue, float alpha) {
      float cosLatitude = Mth.cos(latitude);
      float nx = cosLatitude * Mth.cos(longitude);
      float ny = Mth.sin(latitude);
      float nz = cosLatitude * Mth.sin(longitude);
      vertices.addVertex(pose, nx * radius, ny * radius, nz * radius).setColor(red, green, blue, alpha).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(nx, ny, nz);
   }

   private static void renderFireTail(PoseStack poseStack, MultiBufferSource buffer, float radius, float age) {
      float pulse = 0.84F + 0.16F * Mth.sin(age * 0.9F);
      renderCone(poseStack, buffer, radius * 0.8F, radius * 5.2F, 1.0F, 0.08F, 0.0F, 0.18F * pulse);
      renderCone(poseStack, buffer, radius * 0.46F, radius * 3.8F, 1.0F, 0.68F, 0.08F, 0.24F * pulse);
   }

   private static void renderCone(PoseStack poseStack, MultiBufferSource buffer, float baseRadius, float height, float red, float green, float blue, float alpha) {
      Matrix4f pose = poseStack.last().pose();
      VertexConsumer vertices = com.example.immortal_cultivation_mod.client.SpellGlowBuffers.get(buffer);
      int segments = 24;

      for(int segment = 0; segment < segments; ++segment) {
         float a0 = ((float)Math.PI * 2F) * (float)segment / (float)segments;
         float a1 = ((float)Math.PI * 2F) * (float)(segment + 1) / (float)segments;
         vertex(vertices, pose, Mth.cos(a0) * baseRadius, baseRadius * 0.2F, Mth.sin(a0) * baseRadius, red, green, blue, alpha);
         vertex(vertices, pose, Mth.cos(a1) * baseRadius, baseRadius * 0.2F, Mth.sin(a1) * baseRadius, red, green, blue, alpha);
         vertex(vertices, pose, Mth.cos(a1) * baseRadius * 0.08F, height, Mth.sin(a1) * baseRadius * 0.08F, red, green, blue, 0.0F);
         vertex(vertices, pose, Mth.cos(a0) * baseRadius * 0.08F, height, Mth.sin(a0) * baseRadius * 0.08F, red, green, blue, 0.0F);
      }

   }

   private static void renderDisc(PoseStack poseStack, MultiBufferSource buffer, float radius, float red, float green, float blue, float alpha) {
      renderFlatRing(poseStack, buffer, 0.0F, radius, red, green, blue, alpha, 64);
   }

   private static void renderRing(PoseStack poseStack, MultiBufferSource buffer, float radius, float halfWidth, float red, float green, float blue, float alpha) {
      renderFlatRing(poseStack, buffer, Math.max(0.0F, radius - halfWidth), radius + halfWidth, red, green, blue, alpha, 64);
   }

   private static void renderFlatRing(PoseStack poseStack, MultiBufferSource buffer, float inner, float outer, float red, float green, float blue, float alpha, int segments) {
      Matrix4f pose = poseStack.last().pose();
      VertexConsumer vertices = com.example.immortal_cultivation_mod.client.SpellGlowBuffers.get(buffer);

      for(int segment = 0; segment < segments; ++segment) {
         float a0 = ((float)Math.PI * 2F) * (float)segment / (float)segments;
         float a1 = ((float)Math.PI * 2F) * (float)(segment + 1) / (float)segments;
         vertex(vertices, pose, Mth.cos(a0) * inner, 0.0F, Mth.sin(a0) * inner, red, green, blue, alpha);
         vertex(vertices, pose, Mth.cos(a1) * inner, 0.0F, Mth.sin(a1) * inner, red, green, blue, alpha);
         vertex(vertices, pose, Mth.cos(a1) * outer, 0.0F, Mth.sin(a1) * outer, red, green, blue, alpha);
         vertex(vertices, pose, Mth.cos(a0) * outer, 0.0F, Mth.sin(a0) * outer, red, green, blue, alpha);
      }

   }

   private static void renderRadials(PoseStack poseStack, MultiBufferSource buffer, float radius, float age, float red, float green, float blue, float alpha) {
      Matrix4f pose = poseStack.last().pose();
      VertexConsumer vertices = com.example.immortal_cultivation_mod.client.SpellGlowBuffers.get(buffer);

      for(int line = 0; line < 12; ++line) {
         float angle = ((float)Math.PI * 2F) * (float)line / 12.0F + age * 0.006F;
         float width = 0.045F;
         float inner = radius * 0.18F;
         float sideX = -Mth.sin(angle) * width;
         float sideZ = Mth.cos(angle) * width;
         float innerX = Mth.cos(angle) * inner;
         float innerZ = Mth.sin(angle) * inner;
         float outerX = Mth.cos(angle) * radius;
         float outerZ = Mth.sin(angle) * radius;
         vertex(vertices, pose, innerX - sideX, 0.004F, innerZ - sideZ, red, green, blue, alpha);
         vertex(vertices, pose, outerX - sideX, 0.004F, outerZ - sideZ, red, green, blue, alpha * 0.2F);
         vertex(vertices, pose, outerX + sideX, 0.004F, outerZ + sideZ, red, green, blue, alpha * 0.2F);
         vertex(vertices, pose, innerX + sideX, 0.004F, innerZ + sideZ, red, green, blue, alpha);
      }

   }

   private static void renderCracks(PoseStack poseStack, MultiBufferSource buffer, float radius, int seed, float red, float green, float blue, float alpha) {
      Matrix4f pose = poseStack.last().pose();
      VertexConsumer vertices = com.example.immortal_cultivation_mod.client.SpellGlowBuffers.get(buffer);

      for(int crack = 0; crack < 22; ++crack) {
         float angle = ((float)Math.PI * 2F) * (float)crack / 22.0F + Mth.sin((float)(seed + crack * 17) * 0.83F) * 0.16F;
         float start = radius * (0.08F + (float)(crack % 4) * 0.035F);
         float end = radius * (0.58F + (float)(crack % 5) * 0.08F);
         float bend = Mth.sin((float)seed * 0.21F + (float)crack * 2.17F) * 0.18F;
         drawCrackSegment(vertices, pose, angle, start, end * 0.62F, 0.045F, red, green, blue, alpha);
         drawCrackSegment(vertices, pose, angle + bend, end * 0.58F, end, 0.026F, red, green, blue, alpha * 0.72F);
      }

   }

   private static void drawCrackSegment(VertexConsumer vertices, Matrix4f pose, float angle, float start, float end, float halfWidth, float red, float green, float blue, float alpha) {
      float sideX = -Mth.sin(angle) * halfWidth;
      float sideZ = Mth.cos(angle) * halfWidth;
      float sx = Mth.cos(angle) * start;
      float sz = Mth.sin(angle) * start;
      float ex = Mth.cos(angle) * end;
      float ez = Mth.sin(angle) * end;
      vertex(vertices, pose, sx - sideX, 0.0F, sz - sideZ, red, green, blue, alpha);
      vertex(vertices, pose, ex - sideX, 0.0F, ez - sideZ, red, green, blue, 0.0F);
      vertex(vertices, pose, ex + sideX, 0.0F, ez + sideZ, red, green, blue, 0.0F);
      vertex(vertices, pose, sx + sideX, 0.0F, sz + sideZ, red, green, blue, alpha);
   }

   private static void renderSkyColumn(PoseStack poseStack, MultiBufferSource buffer, float radius, float height, float red, float green, float blue, float alpha) {
      Matrix4f pose = poseStack.last().pose();
      VertexConsumer vertices = com.example.immortal_cultivation_mod.client.SpellGlowBuffers.get(buffer);
      int segments = 32;

      for(int segment = 0; segment < segments; ++segment) {
         float a0 = ((float)Math.PI * 2F) * (float)segment / (float)segments;
         float a1 = ((float)Math.PI * 2F) * (float)(segment + 1) / (float)segments;
         vertex(vertices, pose, Mth.cos(a0) * radius, 0.0F, Mth.sin(a0) * radius, red, green, blue, alpha);
         vertex(vertices, pose, Mth.cos(a1) * radius, 0.0F, Mth.sin(a1) * radius, red, green, blue, alpha);
         vertex(vertices, pose, Mth.cos(a1) * radius * 0.7F, height, Mth.sin(a1) * radius * 0.7F, red, green, blue, 0.0F);
         vertex(vertices, pose, Mth.cos(a0) * radius * 0.7F, height, Mth.sin(a0) * radius * 0.7F, red, green, blue, 0.0F);
      }

   }

   private static void vertex(VertexConsumer vertices, Matrix4f pose, float x, float y, float z, float red, float green, float blue, float alpha) {
      vertices.addVertex(pose, x, y, z).setColor(red, green, blue, alpha).setUv(0.5F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0.0F, 1.0F, 0.0F);
   }

   public ResourceLocation getTextureLocation(TianyunFenchengMeteorEntity entity) {
      return MAGMA_TEXTURE;
   }
}
