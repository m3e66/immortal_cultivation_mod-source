package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.entity.TalismanWardEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class TalismanWardRenderer extends EntityRenderer<TalismanWardEntity> {
   private static final ResourceLocation WHITE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");
   private static final ResourceLocation TALISMAN = ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "textures/item/talisman.png");

   public TalismanWardRenderer(EntityRendererProvider.Context context) {
      super(context);
      this.shadowRadius = 0.0F;
   }

   public void render(TalismanWardEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
      Matrix4f pose = poseStack.last().pose();
      VertexConsumer vertices = buffers.getBuffer(RenderType.entityTranslucentEmissive(WHITE));
      float fade = Mth.clamp(1.0F - Math.max(0.0F, entity.ageProgress(partialTick) - 0.9F) / 0.1F, 0.0F, 1.0F);
      switch (entity.kind()) {
         case SUPPRESSION:
            renderSuppression(vertices, pose, (float)entity.tickCount + partialTick, fade);
            this.renderAnchoredPaper(poseStack, buffers, (float)entity.tickCount + partialTick, fade);
            break;
         case FIVE_MOUNTAINS:
            renderMountains(vertices, pose, (float)entity.tickCount + partialTick, fade);
            break;
         case LIGHTNING_STAIN:
            renderStain(vertices, pose, (float)entity.tickCount + partialTick, fade);
            break;
         case FORBIDDEN_AIR:
            renderForbiddenAir(vertices, pose, (float)entity.tickCount + partialTick, fade);
            renderGroundPaper(poseStack, buffers, fade);
      }

   }

   private static void renderGroundPaper(PoseStack poseStack, MultiBufferSource buffers, float fade) {
      poseStack.pushPose();
      poseStack.translate((double)0.0F, 0.028, (double)0.0F);
      Matrix4f pose = poseStack.last().pose();
      VertexConsumer paper = buffers.getBuffer(RenderType.entityTranslucentEmissive(TALISMAN));
      paper.addVertex(pose, -0.34F, 0.0F, -0.62F).setColor(0.82F, 0.63F, 0.24F, fade).setUv(0.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0.0F, 1.0F, 0.0F);
      paper.addVertex(pose, 0.34F, 0.0F, -0.62F).setColor(0.82F, 0.63F, 0.24F, fade).setUv(1.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0.0F, 1.0F, 0.0F);
      paper.addVertex(pose, 0.34F, 0.0F, 0.62F).setColor(0.82F, 0.63F, 0.24F, fade).setUv(1.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0.0F, 1.0F, 0.0F);
      paper.addVertex(pose, -0.34F, 0.0F, 0.62F).setColor(0.82F, 0.63F, 0.24F, fade).setUv(0.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0.0F, 1.0F, 0.0F);
      poseStack.popPose();
   }

   private void renderAnchoredPaper(PoseStack poseStack, MultiBufferSource buffers, float age, float fade) {
      poseStack.pushPose();
      poseStack.translate((double)0.0F, (double)1.25F + (double)Mth.sin(age * 0.08F) * 0.035, (double)0.0F);
      poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
      Matrix4f pose = poseStack.last().pose();
      VertexConsumer paper = buffers.getBuffer(RenderType.entityTranslucentEmissive(TALISMAN));
      paperVertex(paper, pose, -0.34F, -0.62F, 0.0F, 1.0F, fade);
      paperVertex(paper, pose, 0.34F, -0.62F, 1.0F, 1.0F, fade);
      paperVertex(paper, pose, 0.34F, 0.62F, 1.0F, 0.0F, fade);
      paperVertex(paper, pose, -0.34F, 0.62F, 0.0F, 0.0F, fade);
      poseStack.popPose();
   }

   private static void paperVertex(VertexConsumer vertex, Matrix4f pose, float x, float y, float u, float v, float alpha) {
      vertex.addVertex(pose, x, y, 0.0F).setColor(1.0F, 0.94F, 0.68F, alpha).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0.0F, 0.0F, 1.0F);
   }

   private static void renderSuppression(VertexConsumer v, Matrix4f pose, float age, float fade) {
      float tremble = Mth.sin(age * 0.31F) * 0.035F;
      ring(v, pose, 6.0F + tremble, 0.055F, 0.72F, 0.55F, 0.18F, 0.42F * fade);
      ring(v, pose, 3.2F, 0.03F, 0.92F, 0.76F, 0.35F, 0.24F * fade);

      for(int side = 0; side < 4; ++side) {
         float angle = (float)side * ((float)Math.PI / 2F);
         Vec3 base = new Vec3((double)Mth.cos(angle) * 3.15, 0.02, (double)Mth.sin(angle) * 3.15);
         beam(v, pose, base, base.add((double)0.0F, 2.8, (double)0.0F), 0.026F, 0.87F, 0.68F, 0.22F, 0.62F * fade);
         beam(v, pose, Vec3.ZERO, base, 0.018F, 0.76F, 0.56F, 0.18F, 0.3F * fade);
      }

   }

   private static void renderMountains(VertexConsumer v, Matrix4f pose, float age, float fade) {
      float pulse = 0.7F + Mth.sin(age * 0.08F) * 0.08F;

      for(int seal = 0; seal < 5; ++seal) {
         float angle = ((float)Math.PI * 2F) * (float)seal / 5.0F + age * 0.002F;
         Vec3 radial = new Vec3((double)Mth.cos(angle), (double)0.0F, (double)Mth.sin(angle));
         Vec3 tangent = new Vec3(-radial.z, (double)0.0F, radial.x);
         Vec3 center = radial.scale((double)2.25F).add((double)0.0F, 1.05, (double)0.0F);
         Vec3 left = center.add(tangent.scale(-0.48)).add((double)0.0F, -0.44, (double)0.0F);
         Vec3 right = center.add(tangent.scale(0.48)).add((double)0.0F, -0.44, (double)0.0F);
         Vec3 top = center.add((double)0.0F, 0.62, (double)0.0F);
         triangle(v, pose, left, right, top, 0.72F, 0.52F, 0.18F, pulse * fade);
         triangle(v, pose, right, left, top, 0.96F, 0.78F, 0.32F, pulse * 0.55F * fade);
         beam(v, pose, Vec3.ZERO, radial.scale((double)2.25F), 0.025F, 0.6F, 0.4F, 0.12F, 0.42F * fade);
      }

      ring(v, pose, 1.18F, 0.035F, 0.88F, 0.67F, 0.24F, 0.42F * fade);
   }

   private static void renderStain(VertexConsumer v, Matrix4f pose, float age, float fade) {
      int points = 18;
      Vec3 center = new Vec3((double)0.0F, 0.006, (double)0.0F);

      for(int index = 0; index < points; ++index) {
         float a0 = ((float)Math.PI * 2F) * (float)index / (float)points;
         float a1 = ((float)Math.PI * 2F) * (float)(index + 1) / (float)points;
         float r0 = 0.82F + (float)(index * 17 & 3) * 0.09F;
         float r1 = 0.82F + (float)((index + 1) * 17 & 3) * 0.09F;
         triangle(v, pose, center, new Vec3((double)(Mth.cos(a0) * r0), 0.006, (double)(Mth.sin(a0) * r0)), new Vec3((double)(Mth.cos(a1) * r1), 0.006, (double)(Mth.sin(a1) * r1)), 0.035F, 0.04F, 0.05F, 0.72F * fade);
      }

      for(int branch = 0; branch < 7; ++branch) {
         float angle = (float)branch * ((float)Math.PI * 2F) / 7.0F + age * 0.001F;
         beam(v, pose, center, new Vec3((double)Mth.cos(angle) * 1.35, 0.012, (double)Mth.sin(angle) * 1.35), 0.022F, 0.4F, 0.62F, 1.0F, 0.34F * fade);
      }

   }

   private static void renderForbiddenAir(VertexConsumer v, Matrix4f pose, float age, float fade) {
      float fieldRadius = 24.0F;
      float activation = Mth.clamp(age / 6.0F, 0.0F, 1.0F);
      float pulse = 0.8F + Mth.sin(age * 0.14F) * 0.12F;
      ring(v, pose, fieldRadius * activation, 0.085F, 0.66F, 0.47F, 0.12F, 0.5F * fade);

      for(int inner = 1; inner <= 3; ++inner) {
         ring(v, pose, fieldRadius * (float)inner / 4.0F * activation, 0.032F, 0.78F, 0.57F, 0.17F, (0.14F + (float)inner * 0.025F) * fade);
      }

      for(int glyph = 0; glyph < 16; ++glyph) {
         float angle = (float)glyph * ((float)Math.PI * 2F) / 16.0F;
         Vec3 radial = new Vec3((double)Mth.cos(angle), (double)0.0F, (double)Mth.sin(angle));
         Vec3 tangent = new Vec3(-radial.z, (double)0.0F, radial.x);
         Vec3 center = radial.scale((double)fieldRadius * 0.915 * (double)activation);
         Vec3 left = center.add(tangent.scale(-0.46));
         Vec3 right = center.add(tangent.scale(0.46));
         Vec3 tip = center.add(radial.scale(0.74));
         triangle(v, pose, left, right, tip, 0.74F, 0.54F, 0.17F, 0.34F * pulse * fade);
      }

      for(int line = 0; line < 20; ++line) {
         float angle = (float)line * 2.3999631F + 0.7F;
         float radius = 3.0F + (float)(line * 37 % 71) / 71.0F * (fieldRadius - 4.5F);
         Vec3 top = new Vec3((double)(Mth.cos(angle) * radius), (double)9.0F + (double)(line % 4) * (double)3.0F, (double)(Mth.sin(angle) * radius));
         Vec3 bottom = new Vec3(top.x, 0.08, top.z);
         beam(v, pose, top, bottom, 0.012F, 0.78F, 0.6F, 0.24F, 0.075F * fade);
      }

   }

   private static void ring(VertexConsumer v, Matrix4f pose, float radius, float width, float red, float green, float blue, float alpha) {
      int segments = Mth.clamp(Mth.ceil(radius * 6.0F), 64, 192);

      for(int i = 0; i < segments; ++i) {
         float a0 = ((float)Math.PI * 2F) * (float)i / (float)segments;
         float a1 = ((float)Math.PI * 2F) * (float)(i + 1) / (float)segments;
         vertex(v, pose, new Vec3((double)(Mth.cos(a0) * (radius - width)), 0.01, (double)(Mth.sin(a0) * (radius - width))), red, green, blue, alpha);
         vertex(v, pose, new Vec3((double)(Mth.cos(a1) * (radius - width)), 0.01, (double)(Mth.sin(a1) * (radius - width))), red, green, blue, alpha);
         vertex(v, pose, new Vec3((double)(Mth.cos(a1) * (radius + width)), 0.01, (double)(Mth.sin(a1) * (radius + width))), red, green, blue, alpha * 0.35F);
         vertex(v, pose, new Vec3((double)(Mth.cos(a0) * (radius + width)), 0.01, (double)(Mth.sin(a0) * (radius + width))), red, green, blue, alpha * 0.35F);
      }

   }

   private static void beam(VertexConsumer v, Matrix4f pose, Vec3 start, Vec3 end, float width, float red, float green, float blue, float alpha) {
      Vec3 direction = end.subtract(start).normalize();
      Vec3 side = direction.cross(new Vec3((double)0.0F, (double)1.0F, (double)0.0F));
      if (side.lengthSqr() < 0.001) {
         side = new Vec3((double)1.0F, (double)0.0F, (double)0.0F);
      }

      side = side.normalize().scale((double)width);
      vertex(v, pose, start.subtract(side), red, green, blue, alpha);
      vertex(v, pose, start.add(side), red, green, blue, alpha);
      vertex(v, pose, end.add(side), red, green, blue, 0.0F);
      vertex(v, pose, end.subtract(side), red, green, blue, 0.0F);
   }

   private static void triangle(VertexConsumer v, Matrix4f pose, Vec3 a, Vec3 b, Vec3 c, float red, float green, float blue, float alpha) {
      vertex(v, pose, a, red, green, blue, alpha);
      vertex(v, pose, b, red, green, blue, alpha);
      vertex(v, pose, c, red, green, blue, alpha);
      vertex(v, pose, c, red, green, blue, alpha);
   }

   private static void vertex(VertexConsumer v, Matrix4f pose, Vec3 point, float red, float green, float blue, float alpha) {
      v.addVertex(pose, (float)point.x, (float)point.y, (float)point.z).setColor(red, green, blue, alpha).setUv(0.5F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0.0F, 1.0F, 0.0F);
   }

   public ResourceLocation getTextureLocation(TalismanWardEntity entity) {
      return WHITE;
   }
}
