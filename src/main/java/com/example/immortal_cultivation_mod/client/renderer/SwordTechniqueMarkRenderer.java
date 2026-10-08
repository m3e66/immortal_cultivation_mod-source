package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.entity.SwordTechniqueMarkEntity;
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

public final class SwordTechniqueMarkRenderer extends EntityRenderer<SwordTechniqueMarkEntity> {
   private static final ResourceLocation WHITE_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");

   public SwordTechniqueMarkRenderer(EntityRendererProvider.Context context) {
      super(context);
      this.shadowRadius = 0.0F;
   }

   public void render(SwordTechniqueMarkEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      float age = (float)entity.tickCount + partialTick;
      VertexConsumer vertices = com.example.immortal_cultivation_mod.client.SpellGlowBuffers.get(buffer);
      poseStack.pushPose();
      if (entity.mode() == SwordTechniqueMarkEntity.Mode.CUT) {
         renderCut(entity, age, poseStack, vertices);
      } else {
         renderFate(entity, age, poseStack, vertices);
      }

      poseStack.popPose();
      super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
   }

   private static void renderCut(SwordTechniqueMarkEntity entity, float age, PoseStack poseStack, VertexConsumer vertices) {
      poseStack.mulPose(Axis.YP.rotationDegrees(entity.roll()));
      poseStack.mulPose(Axis.ZP.rotationDegrees(-18.0F + entity.roll() * 0.18F));
      Matrix4f pose = poseStack.last().pose();
      float life = Mth.clamp(1.0F - age / 7.0F, 0.0F, 1.0F);
      float flash = Mth.clamp(1.0F - Math.abs(age - 2.0F) / 1.7F, 0.0F, 1.0F);
      slash(vertices, pose, 1.15F + flash * 0.35F, 0.025F, 1.0F, 1.0F, 0.96F, 0.88F * life);
      slash(vertices, pose, 1.36F + flash * 0.28F, 0.09F, 1.0F, 0.68F, 0.1F, 0.26F * life * (0.35F + flash));
      if (flash > 0.01F) {
         ring(vertices, pose, 0.28F + flash * 0.54F, 0.025F, 1.0F, 0.82F, 0.28F, flash * 0.68F);
      }

   }

   private static void renderFate(SwordTechniqueMarkEntity entity, float age, PoseStack poseStack, VertexConsumer vertices) {
      poseStack.mulPose(Axis.YP.rotationDegrees(age * 4.0F));
      Matrix4f pose = poseStack.last().pose();
      float pulse = 0.82F + Mth.sin(age * 0.46F) * 0.15F;
      float radius = 0.3F + (float)entity.hitCount() * 0.08F;
      ringHorizontal(vertices, pose, radius, 0.02F, 0.92F, 0.96F, 1.0F, 0.66F * pulse);
      ringHorizontal(vertices, pose, radius + 0.11F, 0.012F, 1.0F, 0.72F, 0.15F, 0.32F * pulse);

      for(int rune = 0; rune < entity.hitCount(); ++rune) {
         float angle = ((float)Math.PI * 2F) * (float)rune / (float)Math.max(1, entity.hitCount());
         smallSword(vertices, pose, Mth.cos(angle) * (radius + 0.18F), 0.03F + Mth.sin(age * 0.3F + (float)rune) * 0.035F, Mth.sin(angle) * (radius + 0.18F), angle, 0.34F, 0.9F, 0.96F, 1.0F, 0.8F);
      }

      if (entity.finalBurst()) {
         float burst = Mth.clamp(age % 10.0F / 8.0F, 0.0F, 1.0F);

         for(int cut = 0; cut < 3; ++cut) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees((float)cut * 120.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-28.0F + (float)cut * 25.0F));
            slash(vertices, poseStack.last().pose(), 0.65F + burst * 0.75F, 0.032F, 1.0F, 0.96F, 0.78F, 0.82F * (1.0F - burst));
            poseStack.popPose();
         }

         ringHorizontal(vertices, pose, 0.38F + burst * 0.85F, 0.035F, 1.0F, 0.64F, 0.08F, 0.72F * (1.0F - burst));
      }

   }

   private static void smallSword(VertexConsumer vertices, Matrix4f pose, float cx, float cy, float cz, float angle, float length, float red, float green, float blue, float alpha) {
      float dx = Mth.cos(angle) * length;
      float dz = Mth.sin(angle) * length;
      float sx = -Mth.sin(angle) * 0.035F;
      float sz = Mth.cos(angle) * 0.035F;
      quad(vertices, pose, cx - dx * 0.5F - sx, cy, cz - dz * 0.5F - sz, cx - dx * 0.5F + sx, cy, cz - dz * 0.5F + sz, cx + dx * 0.34F + sx * 0.3F, cy, cz + dz * 0.34F + sz * 0.3F, cx + dx * 0.5F, cy, cz + dz * 0.5F, red, green, blue, alpha);
   }

   private static void slash(VertexConsumer vertices, Matrix4f pose, float halfLength, float halfWidth, float red, float green, float blue, float alpha) {
      quadFade(vertices, pose, -halfLength, -halfWidth, 0.0F, halfLength, -halfWidth * 0.16F, 0.0F, halfLength, halfWidth * 0.16F, 0.0F, -halfLength, halfWidth, 0.0F, red, green, blue, alpha);
      quadFade(vertices, pose, -halfLength, 0.0F, -halfWidth, halfLength, 0.0F, -halfWidth * 0.16F, halfLength, 0.0F, halfWidth * 0.16F, -halfLength, 0.0F, halfWidth, red, green, blue, alpha * 0.62F);
   }

   private static void ring(VertexConsumer vertices, Matrix4f pose, float radius, float width, float red, float green, float blue, float alpha) {
      for(int segment = 0; segment < 40; ++segment) {
         float a0 = ((float)Math.PI * 2F) * (float)segment / 40.0F;
         float a1 = ((float)Math.PI * 2F) * (float)(segment + 1) / 40.0F;
         quad(vertices, pose, Mth.cos(a0) * (radius - width), Mth.sin(a0) * (radius - width), 0.0F, Mth.cos(a1) * (radius - width), Mth.sin(a1) * (radius - width), 0.0F, Mth.cos(a1) * (radius + width), Mth.sin(a1) * (radius + width), 0.0F, Mth.cos(a0) * (radius + width), Mth.sin(a0) * (radius + width), 0.0F, red, green, blue, alpha);
      }

   }

   private static void ringHorizontal(VertexConsumer vertices, Matrix4f pose, float radius, float width, float red, float green, float blue, float alpha) {
      for(int segment = 0; segment < 40; ++segment) {
         float a0 = ((float)Math.PI * 2F) * (float)segment / 40.0F;
         float a1 = ((float)Math.PI * 2F) * (float)(segment + 1) / 40.0F;
         quad(vertices, pose, Mth.cos(a0) * (radius - width), 0.0F, Mth.sin(a0) * (radius - width), Mth.cos(a1) * (radius - width), 0.0F, Mth.sin(a1) * (radius - width), Mth.cos(a1) * (radius + width), 0.0F, Mth.sin(a1) * (radius + width), Mth.cos(a0) * (radius + width), 0.0F, Mth.sin(a0) * (radius + width), red, green, blue, alpha);
      }

   }

   private static void quad(VertexConsumer vertices, Matrix4f pose, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float red, float green, float blue, float alpha) {
      vertex(vertices, pose, x0, y0, z0, red, green, blue, alpha);
      vertex(vertices, pose, x1, y1, z1, red, green, blue, alpha);
      vertex(vertices, pose, x2, y2, z2, red, green, blue, alpha);
      vertex(vertices, pose, x3, y3, z3, red, green, blue, alpha);
   }

   private static void quadFade(VertexConsumer vertices, Matrix4f pose, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float red, float green, float blue, float alpha) {
      vertex(vertices, pose, x0, y0, z0, red, green, blue, 0.0F);
      vertex(vertices, pose, x1, y1, z1, red, green, blue, 0.0F);
      vertex(vertices, pose, x2, y2, z2, red, green, blue, alpha);
      vertex(vertices, pose, x3, y3, z3, red, green, blue, alpha);
   }

   private static void vertex(VertexConsumer vertices, Matrix4f pose, float x, float y, float z, float red, float green, float blue, float alpha) {
      vertices.addVertex(pose, x, y, z).setColor(red, green, blue, alpha).setUv(0.5F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0.0F, 1.0F, 0.0F);
   }

   public ResourceLocation getTextureLocation(SwordTechniqueMarkEntity entity) {
      return WHITE_TEXTURE;
   }
}
