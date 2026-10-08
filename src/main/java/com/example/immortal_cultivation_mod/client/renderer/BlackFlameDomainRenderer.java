package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.entity.BlackFlameDomainEntity;
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

public final class BlackFlameDomainRenderer extends EntityRenderer<BlackFlameDomainEntity> {
   private static final ResourceLocation WHITE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
   private static final RenderType FLAME_RENDER_TYPE;
   private static final int MIN_LONGITUDE_SEGMENTS = 16;
   private static final int MAX_LONGITUDE_SEGMENTS = 36;
   private static final int MIN_LATITUDE_SEGMENTS = 8;
   private static final int MAX_LATITUDE_SEGMENTS = 18;

   public BlackFlameDomainRenderer(EntityRendererProvider.Context context) {
      super(context);
      this.shadowRadius = 0.0F;
   }

   public void render(BlackFlameDomainEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      float radius = Math.max(0.35F, entity.visualRadius(partialTick));
      float age = (float)entity.tickCount + partialTick;
      float pulse = 0.94F + Mth.sin(age * 0.21F) * 0.035F + Mth.sin(age * 0.071F) * 0.025F;
      Matrix4f matrix = poseStack.last().pose();
      VertexConsumer flame = buffer.getBuffer(FLAME_RENDER_TYPE);
      renderBrokenSphere(flame, matrix, radius * pulse, age, 0.12F, 0.009F, 0.22F, 0.2F, 0);
      renderBrokenSphere(flame, matrix, radius * (0.965F - pulse * 0.012F), -age * 0.73F, 0.5F, 0.04F, 0.86F, 0.28F, 3);
      renderGroundRing(flame, matrix, radius * 0.91F, radius * 1.015F, 0.026F, age * 0.02F, 0.48F, 0.025F, 0.82F, 0.44F);
      renderGroundRing(flame, matrix, radius * 0.55F, radius * 0.59F, 0.032F, -age * 0.031F, 0.22F, 0.01F, 0.38F, 0.2F);
      renderGroundRing(flame, matrix, radius * 0.16F, radius * 0.25F, 0.038F, age * 0.046F, 0.66F, 0.045F, 0.96F, 0.36F);
      renderBoundaryFlames(flame, matrix, radius, age);
      super.render(entity, entityYaw, partialTick, poseStack, buffer, 15728880);
   }

   private static void renderBrokenSphere(VertexConsumer vertices, Matrix4f matrix, float radius, float age, float red, float green, float blue, float alpha, int phaseOffset) {
      int animationStep = Mth.floor(age * 0.1F);
      int longitudeSegments = Mth.clamp(Mth.ceil(radius * 0.82F), 16, 36);
      int latitudeSegments = Mth.clamp(Mth.ceil(radius * 0.41F), 8, 18);

      for(int latitude = 0; latitude < latitudeSegments; ++latitude) {
         float v0 = (float)latitude / (float)latitudeSegments;
         float v1 = (float)(latitude + 1) / (float)latitudeSegments;
         float theta0 = (-(float)Math.PI / 2F) + v0 * (float)Math.PI;
         float theta1 = (-(float)Math.PI / 2F) + v1 * (float)Math.PI;

         for(int longitude = 0; longitude < longitudeSegments; ++longitude) {
            int fragment = Math.floorMod(longitude * 11 + latitude * 17 + animationStep + phaseOffset, 17);
            if (fragment != 0 && fragment != 5 && fragment != 11 && fragment != 16) {
               float phi0 = (float)longitude * ((float)Math.PI * 2F) / (float)longitudeSegments + age * 0.0035F;
               float phi1 = (float)(longitude + 1) * ((float)Math.PI * 2F) / (float)longitudeSegments + age * 0.0035F;
               float ripple0 = 1.0F + Mth.sin((float)longitude * 1.7F + (float)latitude * 0.8F + age * 0.11F) * 0.012F;
               float ripple1 = 1.0F + Mth.sin((float)(longitude + 1) * 1.7F + (float)latitude * 0.8F + age * 0.11F) * 0.012F;
               Point p00 = spherePoint(radius * ripple0, theta0, phi0);
               Point p10 = spherePoint(radius * ripple1, theta0, phi1);
               Point p11 = spherePoint(radius * ripple1, theta1, phi1);
               Point p01 = spherePoint(radius * ripple0, theta1, phi0);
               float flicker = 0.76F + 0.24F * Mth.sin(age * 0.19F + (float)longitude * 0.83F - (float)latitude * 0.57F);
               quad(vertices, matrix, p00, p10, p11, p01, red, green, blue, alpha * flicker);
            }
         }
      }

   }

   private static Point spherePoint(float radius, float theta, float phi) {
      float horizontal = Mth.cos(theta) * radius;
      return new Point(Mth.cos(phi) * horizontal, Mth.sin(theta) * radius, Mth.sin(phi) * horizontal);
   }

   private static void renderGroundRing(VertexConsumer vertices, Matrix4f matrix, float innerRadius, float outerRadius, float y, float rotation, float red, float green, float blue, float alpha) {
      int segments = Mth.clamp(Mth.ceil(outerRadius * 1.25F), 36, 72);

      for(int segment = 0; segment < segments; ++segment) {
         if (Math.floorMod(segment + Mth.floor(rotation * 7.0F), 11) != 3) {
            float a0 = (float)segment * ((float)Math.PI * 2F) / (float)segments + rotation;
            float a1 = (float)(segment + 1) * ((float)Math.PI * 2F) / (float)segments + rotation;
            Point p00 = new Point(Mth.cos(a0) * innerRadius, y, Mth.sin(a0) * innerRadius);
            Point p10 = new Point(Mth.cos(a0) * outerRadius, y, Mth.sin(a0) * outerRadius);
            Point p11 = new Point(Mth.cos(a1) * outerRadius, y, Mth.sin(a1) * outerRadius);
            Point p01 = new Point(Mth.cos(a1) * innerRadius, y, Mth.sin(a1) * innerRadius);
            quad(vertices, matrix, p00, p10, p11, p01, red, green, blue, alpha);
         }
      }

   }

   private static void renderBoundaryFlames(VertexConsumer vertices, Matrix4f matrix, float radius, float age) {
      int flames = Mth.clamp(Mth.ceil(radius * 0.92F), 12, 44);

      for(int flame = 0; flame < flames; ++flame) {
         float angle = (float)flame * ((float)Math.PI * 2F) / (float)flames + Mth.sin(age * 0.018F + (float)flame * 1.31F) * 0.1F;
         float wave = 0.5F + 0.5F * Mth.sin(age * 0.24F + (float)flame * 2.17F);
         float height = Mth.clamp(radius * 0.055F, 0.42F, 1.85F) * (0.68F + wave * 0.62F);
         float baseWidth = Mth.clamp(radius * 0.022F, 0.12F, 0.68F);
         float baseRadius = radius * (0.91F + Mth.sin((float)flame * 3.19F + age * 0.07F) * 0.025F);
         float tangentX = -Mth.sin(angle);
         float tangentZ = Mth.cos(angle);
         float radialX = Mth.cos(angle);
         float radialZ = Mth.sin(angle);
         Point left = new Point(radialX * baseRadius - tangentX * baseWidth, 0.03F, radialZ * baseRadius - tangentZ * baseWidth);
         Point right = new Point(radialX * baseRadius + tangentX * baseWidth, 0.03F, radialZ * baseRadius + tangentZ * baseWidth);
         float tipRadius = baseRadius - baseWidth * (0.6F + wave * 0.8F);
         Point tipRight = new Point(radialX * tipRadius + tangentX * baseWidth * 0.1F, height * 0.93F, radialZ * tipRadius + tangentZ * baseWidth * 0.1F);
         Point tipLeft = new Point(radialX * tipRadius - tangentX * baseWidth * 0.1F, height, radialZ * tipRadius - tangentZ * baseWidth * 0.1F);
         float alpha = 0.34F + wave * 0.22F;
         quad(vertices, matrix, left, right, tipRight, tipLeft, 0.58F, 0.025F, 0.93F, alpha);
         Point inner = new Point(radialX * (baseRadius - baseWidth), 0.035F, radialZ * (baseRadius - baseWidth));
         Point outer = new Point(radialX * (baseRadius + baseWidth), 0.035F, radialZ * (baseRadius + baseWidth));
         Point tipOuter = new Point(radialX * tipRadius, height * 0.91F, radialZ * tipRadius);
         quad(vertices, matrix, inner, outer, tipOuter, tipOuter, 0.18F, 0.006F, 0.3F, alpha * 0.74F);
      }

   }

   private static void quad(VertexConsumer vertices, Matrix4f matrix, Point a, Point b, Point c, Point d, float red, float green, float blue, float alpha) {
      vertex(vertices, matrix, a, 0.0F, 0.0F, red, green, blue, alpha);
      vertex(vertices, matrix, b, 1.0F, 0.0F, red, green, blue, alpha);
      vertex(vertices, matrix, c, 1.0F, 1.0F, red, green, blue, alpha);
      vertex(vertices, matrix, d, 0.0F, 1.0F, red, green, blue, alpha);
   }

   private static void vertex(VertexConsumer vertices, Matrix4f matrix, Point point, float u, float v, float red, float green, float blue, float alpha) {
      vertices.addVertex(matrix, point.x, point.y, point.z).setColor(red, green, blue, alpha).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0.0F, 1.0F, 0.0F);
   }

   public ResourceLocation getTextureLocation(BlackFlameDomainEntity entity) {
      return WHITE_TEXTURE;
   }

   static {
      FLAME_RENDER_TYPE = RenderType.entityTranslucentEmissive(WHITE_TEXTURE);
   }

   private static record Point(float x, float y, float z) {
   }
}
