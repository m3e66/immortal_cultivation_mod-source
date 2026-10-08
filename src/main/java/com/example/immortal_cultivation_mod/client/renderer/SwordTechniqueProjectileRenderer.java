package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.entity.SwordTechniqueProjectileEntity;
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
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class SwordTechniqueProjectileRenderer extends EntityRenderer<SwordTechniqueProjectileEntity> {
   private static final ResourceLocation WHITE_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");

   public SwordTechniqueProjectileRenderer(EntityRendererProvider.Context context) {
      super(context);
      this.shadowRadius = 0.0F;
   }

   public void render(SwordTechniqueProjectileEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      try (var support = com.example.immortal_cultivation_mod.client.SpellGlowBuffers.support(com.example.immortal_cultivation_mod.client.SpellRibbonRenderer.spell(entity) != null)) {
      float age = (float)entity.tickCount + partialTick;
      Vec3 direction = entity.getDeltaMovement();
      if (direction.lengthSqr() < 1.0E-6) {
         direction = entity.plannedDirection();
      }

      poseStack.pushPose();
      orientToDirection(poseStack, direction);
      poseStack.mulPose(Axis.ZP.rotationDegrees(entity.roll()));
      if (entity.kind() == SwordTechniqueProjectileEntity.Kind.POKONG_JIANHEN) {
         renderSpaceScar(entity, age, poseStack, buffer);
      } else {
         renderPursuitSword(entity, age, poseStack, buffer);
      }

      poseStack.popPose();
      super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
   }
   }

   private static void orientToDirection(PoseStack poseStack, Vec3 direction) {
      if (!(direction.lengthSqr() < 1.0E-6)) {
         double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
         float yaw = (float)(Mth.atan2(-direction.x, direction.z) * (double)(180F / (float)Math.PI));
         float pitch = (float)(Mth.atan2(direction.y, horizontal) * (double)(180F / (float)Math.PI));
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
         poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
      }
   }

   private static void renderSpaceScar(SwordTechniqueProjectileEntity entity, float age, PoseStack poseStack, MultiBufferSource buffer) {
      VertexConsumer vertices = com.example.immortal_cultivation_mod.client.SpellGlowBuffers.get(buffer);
      Matrix4f pose = poseStack.last().pose();
      boolean waiting = !entity.launched();
      float pulse = 0.76F + Mth.sin(age * 1.9F) * 0.18F;
      if (waiting) {
         bladePlane(vertices, pose, 0.0F, 0.0F, -0.48F, 0.035F, 1.18F, 1.0F, 0.98F, 0.88F, pulse);
         bladePlaneVertical(vertices, pose, 0.0F, 0.0F, -0.48F, 0.022F, 1.18F, 0.72F, 0.82F, 1.0F, pulse * 0.42F);
         ring(vertices, pose, 0.36F + pulse * 0.08F, 0.018F, 1.0F, 0.74F, 0.16F, 0.5F);
      } else {
         bladePlane(vertices, pose, 0.0F, 0.0F, 0.18F, 0.025F, 0.72F, 1.0F, 1.0F, 0.96F, 0.98F);
         bladePlaneVertical(vertices, pose, 0.0F, 0.0F, 0.18F, 0.018F, 0.72F, 0.72F, 0.86F, 1.0F, 0.74F);
         taperedRibbon(vertices, pose, 2.35F, 0.035F, 1.0F, 0.9F, 0.52F, 0.78F);
         taperedRibbonVertical(vertices, pose, 1.7F, 0.052F, 0.64F, 0.78F, 1.0F, 0.3F);
      }
   }

   private static void renderPursuitSword(SwordTechniqueProjectileEntity entity, float age, PoseStack poseStack, MultiBufferSource buffer) {
      VertexConsumer vertices = com.example.immortal_cultivation_mod.client.SpellGlowBuffers.get(buffer);
      Matrix4f pose = poseStack.last().pose();
      boolean waiting = !entity.launched();
      float pulse = 0.88F + Mth.sin(age * 0.72F + (float)entity.swordIndex() * 2.1F) * 0.1F;
      float scale = entity.marked() ? 1.12F : 1.0F;
      renderSword(vertices, pose, scale, pulse);
      renderSwordAura(vertices, pose, age, entity.swordIndex(), waiting, entity.marked());
      if (!waiting) {
         taperedRibbon(vertices, pose, 2.1F, 0.075F, 0.88F, 0.94F, 1.0F, entity.marked() ? 0.68F : 0.46F);
         taperedRibbonVertical(vertices, pose, 1.55F, 0.048F, 1.0F, 0.7F, 0.14F, entity.marked() ? 0.46F : 0.28F);
      }

   }

   private static void renderSword(VertexConsumer vertices, Matrix4f pose, float scale, float pulse) {
      float tip = 0.92F * scale;
      float shoulder = 0.58F * scale;
      float base = -0.42F * scale;
      float blade = 0.105F * scale;
      quad(vertices, pose, -blade, 0.0F, base, blade, 0.0F, base, blade * 0.45F, 0.0F, shoulder, 0.0F, 0.0F, tip, 0.92F, 0.96F, 1.0F, 0.96F * pulse);
      quad(vertices, pose, blade, 0.0F, base, 0.0F, 0.0F, tip, -blade * 0.45F, 0.0F, shoulder, -blade, 0.0F, base, 0.58F, 0.76F, 1.0F, 0.76F);
      quad(vertices, pose, 0.0F, -blade, base, 0.0F, blade, base, 0.0F, blade * 0.45F, shoulder, 0.0F, 0.0F, tip, 1.0F, 0.98F, 0.82F, 0.88F);
      quad(vertices, pose, 0.0F, blade, base, 0.0F, 0.0F, tip, 0.0F, -blade * 0.45F, shoulder, 0.0F, -blade, base, 0.7F, 0.84F, 1.0F, 0.72F);
      boxCross(vertices, pose, -0.44F * scale, 0.24F * scale, 0.036F * scale, 1.0F, 0.72F, 0.12F, 0.96F);
      boxCross(vertices, pose, -0.62F * scale, 0.035F * scale, 0.17F * scale, 0.18F, 0.22F, 0.3F, 0.94F);
      ringAtZ(vertices, pose, -0.82F * scale, 0.09F * scale, 0.018F, 1.0F, 0.82F, 0.3F, 0.82F);
   }

   private static void renderSwordAura(VertexConsumer vertices, Matrix4f pose, float age, int index, boolean waiting, boolean marked) {
      float rotation = age * (0.12F + (float)index * 0.018F);
      float radius = waiting ? 0.46F : 0.28F;

      for(int mote = 0; mote < 6; ++mote) {
         float angle = rotation + ((float)Math.PI * 2F) * (float)mote / 6.0F;
         float x = Mth.cos(angle) * radius;
         float y = Mth.sin(angle) * radius;
         float z = -0.2F + (float)mote * 0.17F;
         float size = marked ? 0.048F : 0.032F;
         quad(vertices, pose, x - size, y - size, z, x + size, y - size, z, x + size, y + size, z, x - size, y + size, z, 1.0F, 0.78F, 0.2F, waiting ? 0.7F : 0.42F);
      }

      if (waiting) {
         ringAtZ(vertices, pose, -0.18F, 0.54F, 0.018F, 0.88F, 0.94F, 1.0F, 0.36F);
      }

   }

   private static void boxCross(VertexConsumer vertices, Matrix4f pose, float z, float halfLength, float halfWidth, float red, float green, float blue, float alpha) {
      quad(vertices, pose, -halfLength, -halfWidth, z, halfLength, -halfWidth, z, halfLength, halfWidth, z, -halfLength, halfWidth, z, red, green, blue, alpha);
      quad(vertices, pose, -halfWidth, -halfLength, z, halfWidth, -halfLength, z, halfWidth, halfLength, z, -halfWidth, halfLength, z, red, green, blue, alpha * 0.78F);
   }

   private static void bladePlane(VertexConsumer vertices, Matrix4f pose, float x, float y, float z, float halfWidth, float halfLength, float red, float green, float blue, float alpha) {
      quad(vertices, pose, x - halfWidth, y, z - halfLength, x + halfWidth, y, z - halfLength, x + halfWidth * 0.2F, y, z + halfLength, x - halfWidth * 0.2F, y, z + halfLength, red, green, blue, alpha);
   }

   private static void bladePlaneVertical(VertexConsumer vertices, Matrix4f pose, float x, float y, float z, float halfWidth, float halfLength, float red, float green, float blue, float alpha) {
      quad(vertices, pose, x, y - halfWidth, z - halfLength, x, y + halfWidth, z - halfLength, x, y + halfWidth * 0.2F, z + halfLength, x, y - halfWidth * 0.2F, z + halfLength, red, green, blue, alpha);
   }

   private static void taperedRibbon(VertexConsumer vertices, Matrix4f pose, float length, float halfWidth, float red, float green, float blue, float alpha) {
      quadFade(vertices, pose, -halfWidth, 0.0F, -0.18F, halfWidth, 0.0F, -0.18F, halfWidth * 0.1F, 0.0F, -length, -halfWidth * 0.1F, 0.0F, -length, red, green, blue, alpha);
   }

   private static void taperedRibbonVertical(VertexConsumer vertices, Matrix4f pose, float length, float halfWidth, float red, float green, float blue, float alpha) {
      quadFade(vertices, pose, 0.0F, -halfWidth, -0.18F, 0.0F, halfWidth, -0.18F, 0.0F, halfWidth * 0.1F, -length, 0.0F, -halfWidth * 0.1F, -length, red, green, blue, alpha);
   }

   private static void ring(VertexConsumer vertices, Matrix4f pose, float radius, float width, float red, float green, float blue, float alpha) {
      ringAtZ(vertices, pose, 0.0F, radius, width, red, green, blue, alpha);
   }

   private static void ringAtZ(VertexConsumer vertices, Matrix4f pose, float z, float radius, float width, float red, float green, float blue, float alpha) {
      for(int segment = 0; segment < 32; ++segment) {
         float a0 = ((float)Math.PI * 2F) * (float)segment / 32.0F;
         float a1 = ((float)Math.PI * 2F) * (float)(segment + 1) / 32.0F;
         quad(vertices, pose, Mth.cos(a0) * (radius - width), Mth.sin(a0) * (radius - width), z, Mth.cos(a1) * (radius - width), Mth.sin(a1) * (radius - width), z, Mth.cos(a1) * (radius + width), Mth.sin(a1) * (radius + width), z, Mth.cos(a0) * (radius + width), Mth.sin(a0) * (radius + width), z, red, green, blue, alpha);
      }

   }

   private static void quad(VertexConsumer vertices, Matrix4f pose, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float red, float green, float blue, float alpha) {
      vertex(vertices, pose, x0, y0, z0, red, green, blue, alpha);
      vertex(vertices, pose, x1, y1, z1, red, green, blue, alpha);
      vertex(vertices, pose, x2, y2, z2, red, green, blue, alpha);
      vertex(vertices, pose, x3, y3, z3, red, green, blue, alpha);
   }

   private static void quadFade(VertexConsumer vertices, Matrix4f pose, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float red, float green, float blue, float alpha) {
      vertex(vertices, pose, x0, y0, z0, red, green, blue, alpha);
      vertex(vertices, pose, x1, y1, z1, red, green, blue, alpha);
      vertex(vertices, pose, x2, y2, z2, red, green, blue, 0.0F);
      vertex(vertices, pose, x3, y3, z3, red, green, blue, 0.0F);
   }

   private static void vertex(VertexConsumer vertices, Matrix4f pose, float x, float y, float z, float red, float green, float blue, float alpha) {
      vertices.addVertex(pose, x, y, z).setColor(red, green, blue, alpha).setUv(0.5F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0.0F, 1.0F, 0.0F);
   }

   public ResourceLocation getTextureLocation(SwordTechniqueProjectileEntity entity) {
      return WHITE_TEXTURE;
   }
}
