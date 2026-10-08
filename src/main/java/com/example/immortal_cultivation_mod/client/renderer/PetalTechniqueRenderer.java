package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.entity.PetalTechniqueEntity;
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

public final class PetalTechniqueRenderer extends EntityRenderer<PetalTechniqueEntity> {
   private static final ResourceLocation WHITE_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");
   private static final float[][] FLOWER_COLORS = new float[][]{{1.0F, 0.52F, 0.72F}, {1.0F, 0.93F, 0.94F}, {0.92F, 0.22F, 0.34F}, {0.78F, 0.6F, 1.0F}};

   public PetalTechniqueRenderer(EntityRendererProvider.Context context) {
      super(context);
      this.shadowRadius = 0.0F;
   }

   public void render(PetalTechniqueEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      float age = (float)entity.tickCount + partialTick;
      poseStack.pushPose();
      if (entity.kind() == PetalTechniqueEntity.Kind.LUOHUA_CHANSHEN) {
         renderLuohua(entity, age, poseStack, buffer);
      } else {
         renderBaihua(entity, age, poseStack, buffer);
      }

      poseStack.popPose();
      super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
   }

   private static void renderLuohua(PetalTechniqueEntity entity, float age, PoseStack poseStack, MultiBufferSource buffer) {
      Matrix4f pose = poseStack.last().pose();
      VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucentEmissive(WHITE_TEXTURE));
      if (!entity.orbiting() && age < 8.0F) {
         float bloom = Mth.clamp(age / 7.0F, 0.12F, 1.0F);
         renderFlower(vertices, pose, 0.0F, 0.05F, 0.0F, 0.56F * bloom, age * 0.08F, 1.0F, 0.54F, 0.74F, 0.92F);
         renderRing(vertices, pose, 0.18F + bloom * 0.58F, 0.022F, 0.42F, 1.0F, 0.56F, 0.68F * (1.0F - bloom * 0.35F));
         renderEnergyStem(vertices, pose, age, bloom);

         for(int petal = 0; petal < 8; ++petal) {
            float angle = age * 0.12F + ((float)Math.PI * 2F) * (float)petal / 8.0F;
            float radius = 0.55F + (float)petal * 0.035F;
            petal(vertices, pose, Mth.cos(angle) * radius, 0.04F + Mth.sin(angle * 1.7F) * 0.24F, Mth.sin(angle) * radius, angle + ((float)Math.PI / 2F), 0.2F, 0.1F, 1.0F, 0.72F, 0.84F, 0.62F);
         }

      } else if (!entity.orbiting()) {
         float phase = age * 0.38F;

         for(int petalIndex = 0; petalIndex < 28; ++petalIndex) {
            float t = (float)petalIndex / 27.0F;
            float angle = phase + (float)petalIndex * 1.73F;
            float radius = 0.16F + t * 0.62F;
            float[] color = FLOWER_COLORS[petalIndex % 2];
            petal(vertices, pose, -t * 2.25F + Mth.cos(angle) * radius, Mth.sin(angle * 1.31F) * radius, Mth.sin(angle) * radius, angle, 0.24F - t * 0.06F, 0.095F, color[0], color[1], color[2], 0.78F * (1.0F - t * 0.38F));
         }

         renderVerticalWindRing(vertices, pose, -0.68F, 0.72F, 0.42F, 1.0F, 0.58F, 0.52F);
      } else {
         int effectAge = entity.effectAge();
         boolean frozen = effectAge >= 28 && effectAge < 31;
         float acceleration = 1.0F + (float)entity.lastWave() * 0.26F;

         for(int layer = 0; layer < 3; ++layer) {
            int petals = layer == 1 ? 18 : 15;
            float layerRadius = 0.82F + (float)layer * 0.18F;
            float tilt = (float)(layer - 1) * 0.55F;

            for(int index = 0; index < petals; ++index) {
               float base = ((float)Math.PI * 2F) * (float)index / (float)petals;
               float direction = layer == 1 ? -1.0F : 1.0F;
               float angle = frozen ? base : base + (float)effectAge * 0.22F * acceleration * direction;
               float y = Mth.sin(angle * (float)(layer + 1)) * (0.38F + (float)layer * 0.16F);
               float[] color = FLOWER_COLORS[(index + layer) % 2];
               petal(vertices, pose, Mth.cos(angle) * layerRadius, y, Mth.sin(angle) * layerRadius, frozen ? angle + (float)Math.PI : angle + ((float)Math.PI / 2F) + tilt, 0.22F, 0.085F, color[0], color[1], color[2], frozen ? 0.96F : 0.78F);
            }
         }

         if (effectAge >= 28) {
            float flash = effectAge < 31 ? 0.44F : Mth.clamp((36.0F - (float)effectAge) / 5.0F, 0.0F, 1.0F);
            renderFlower(vertices, pose, 0.0F, -0.88F, 0.0F, 1.35F, -age * 0.05F, 1.0F, 0.58F, 0.78F, flash * 0.62F);
         }

      }
   }

   private static void renderBaihua(PetalTechniqueEntity entity, float age, PoseStack poseStack, MultiBufferSource buffer) {
      Matrix4f pose = poseStack.last().pose();
      VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucentEmissive(WHITE_TEXTURE));
      float radius = entity.fieldRadius();
      float formation = Mth.clamp(age / 14.0F, 0.0F, 1.0F);
      float afterFade = age <= 100.0F ? 1.0F : 1.0F - Mth.clamp((age - 100.0F) / 40.0F, 0.0F, 1.0F);
      renderVineFormation(vertices, pose, radius, age, (age < 24.0F ? formation : 1.0F) * afterFade);
      if (age < 24.0F) {
         for(int flower = 0; flower < 34; ++flower) {
            float angle = (float)flower * 2.3999631F;
            float distance = radius * 0.15F + radius * 0.78F * ((float)(flower * 37 % 97) / 96.0F);
            float grow = Mth.clamp(formation * 1.45F - (float)flower / 52.0F, 0.0F, 1.0F);
            float sway = Mth.sin(age * 0.09F + (float)flower) * 0.08F;
            float[] color = FLOWER_COLORS[flower % FLOWER_COLORS.length];
            renderFlower(vertices, pose, Mth.cos(angle) * distance, 0.06F + sway, Mth.sin(angle) * distance, (0.24F + (float)(flower % 4) * 0.035F) * grow, angle + sway, color[0], color[1], color[2], 0.88F * grow);
         }

         renderRing(vertices, pose, radius * (0.22F + formation * 0.78F), 0.045F, 0.35F, 1.0F, 0.48F, 0.42F * formation);
      } else {
         boolean finalConvergence = age >= 96.0F && age <= 103.0F;
         float convergence = finalConvergence ? Mth.clamp((age - 96.0F) / 4.0F, 0.0F, 1.0F) : 0.0F;
         float stormFade = age <= 103.0F ? 1.0F : afterFade;
         int petalCount = age <= 103.0F ? 108 : 58;

         for(int index = 0; index < petalCount; ++index) {
            int layer = index % 3;
            float base = ((float)Math.PI * 2F) * (float)index / (float)petalCount * 3.0F;
            float direction = layer == 1 ? -1.0F : 1.0F;
            float wavePulse = 1.0F + (float)entity.lastWave() * 0.055F;
            float angle = finalConvergence ? base : base + age * (0.075F + (float)layer * 0.025F) * direction * wavePulse;
            float radialBand = 0.22F + (float)(index * 43 % 101) / 100.0F * 0.78F;
            float petalRadius = radius * radialBand * (1.0F - convergence * 0.82F);
            float var10000;
            switch (layer) {
               case 0 -> var10000 = 0.32F + Mth.sin(angle * 2.0F) * 0.22F;
               case 1 -> var10000 = 1.35F + Mth.sin(angle * 1.4F + (float)index) * 0.75F;
               default -> var10000 = 3.2F + Mth.sin(angle * 1.7F) * 1.5F;
            }

            float y = var10000;
            if (age > 103.0F) {
               y += (float)(index * 17 % 40) * 0.12F - (age - 103.0F) * 0.07F;
            }

            float[] color = FLOWER_COLORS[index % FLOWER_COLORS.length];
            petal(vertices, pose, Mth.cos(angle) * petalRadius, y, Mth.sin(angle) * petalRadius, finalConvergence ? angle + (float)Math.PI : angle + ((float)Math.PI / 2F), layer == 2 ? 0.25F : 0.2F, layer == 2 ? 0.095F : 0.078F, color[0], color[1], color[2], stormFade * (finalConvergence ? 0.96F : 0.72F));
         }

         renderRing(vertices, pose, radius * 0.98F, 0.055F, 0.38F, 1.0F, 0.52F, 0.58F * stormFade);
         renderRing(vertices, pose, radius * 0.56F, 0.035F, 1.0F, 0.48F, 0.72F, 0.42F * stormFade);
         if (finalConvergence) {
            renderFlower(vertices, pose, 0.0F, 0.1F, 0.0F, radius * (0.72F + convergence * 0.26F), -age * 0.018F, 1.0F, 0.7F, 0.86F, 0.22F + convergence * 0.46F);
         }

      }
   }

   private static void renderVineFormation(VertexConsumer vertices, Matrix4f pose, float radius, float age, float alpha) {
      if (!(alpha <= 0.001F)) {
         for(int branch = 0; branch < 18; ++branch) {
            float angle = ((float)Math.PI * 2F) * (float)branch / 18.0F + Mth.sin((float)branch * 1.7F) * 0.16F;
            float start = radius * 0.08F;
            float end = radius * (0.62F + (float)(branch % 5) * 0.075F);
            float bend = Mth.sin((float)branch * 2.31F + age * 0.015F) * 0.16F;
            vineSegment(vertices, pose, angle, start, end * 0.58F, 0.035F, 0.22F, 0.96F, 0.4F, alpha * 0.65F);
            vineSegment(vertices, pose, angle + bend, end * 0.54F, end, 0.022F, 0.35F, 1.0F, 0.52F, alpha * 0.52F);
         }

      }
   }

   private static void renderFlower(VertexConsumer vertices, Matrix4f pose, float x, float y, float z, float radius, float rotation, float red, float green, float blue, float alpha) {
      for(int petalIndex = 0; petalIndex < 5; ++petalIndex) {
         float angle = rotation + ((float)Math.PI * 2F) * (float)petalIndex / 5.0F;
         petal(vertices, pose, x + Mth.cos(angle) * radius * 0.42F, y, z + Mth.sin(angle) * radius * 0.42F, angle, radius * 0.82F, radius * 0.34F, red, green, blue, alpha);
      }

      diamond(vertices, pose, x, y + 0.012F, z, radius * 0.23F, radius * 0.23F, 1.0F, 0.82F, 0.26F, alpha);
   }

   private static void petal(VertexConsumer vertices, Matrix4f pose, float x, float y, float z, float angle, float length, float halfWidth, float red, float green, float blue, float alpha) {
      float radialX = Mth.cos(angle);
      float radialZ = Mth.sin(angle);
      float sideX = -radialZ;
      glowVertex(vertices, pose, x - radialX * length * 0.34F, y, z - radialZ * length * 0.34F, red, green, blue, alpha * 0.52F);
      glowVertex(vertices, pose, x + sideX * halfWidth, y + halfWidth * 0.16F, z + radialX * halfWidth, red, green, blue, alpha);
      glowVertex(vertices, pose, x + radialX * length * 0.72F, y, z + radialZ * length * 0.72F, 1.0F, Math.min(1.0F, green + 0.18F), Math.min(1.0F, blue + 0.14F), alpha);
      glowVertex(vertices, pose, x - sideX * halfWidth, y - halfWidth * 0.16F, z - radialX * halfWidth, red, green, blue, alpha);
   }

   private static void diamond(VertexConsumer vertices, Matrix4f pose, float x, float y, float z, float radiusX, float radiusZ, float red, float green, float blue, float alpha) {
      glowVertex(vertices, pose, x - radiusX, y, z, red, green, blue, alpha);
      glowVertex(vertices, pose, x, y, z + radiusZ, red, green, blue, alpha);
      glowVertex(vertices, pose, x + radiusX, y, z, red, green, blue, alpha);
      glowVertex(vertices, pose, x, y, z - radiusZ, red, green, blue, alpha);
   }

   private static void renderRing(VertexConsumer vertices, Matrix4f pose, float radius, float halfWidth, float red, float green, float blue, float alpha) {
      for(int segment = 0; segment < 64; ++segment) {
         float a0 = ((float)Math.PI * 2F) * (float)segment / 64.0F;
         float a1 = ((float)Math.PI * 2F) * (float)(segment + 1) / 64.0F;
         glowVertex(vertices, pose, Mth.cos(a0) * (radius - halfWidth), 0.015F, Mth.sin(a0) * (radius - halfWidth), red, green, blue, alpha);
         glowVertex(vertices, pose, Mth.cos(a1) * (radius - halfWidth), 0.015F, Mth.sin(a1) * (radius - halfWidth), red, green, blue, alpha);
         glowVertex(vertices, pose, Mth.cos(a1) * (radius + halfWidth), 0.015F, Mth.sin(a1) * (radius + halfWidth), red, green, blue, alpha);
         glowVertex(vertices, pose, Mth.cos(a0) * (radius + halfWidth), 0.015F, Mth.sin(a0) * (radius + halfWidth), red, green, blue, alpha);
      }

   }

   private static void renderVerticalWindRing(VertexConsumer vertices, Matrix4f pose, float x, float radius, float red, float green, float blue, float alpha) {
      for(int segment = 0; segment < 48; ++segment) {
         float a0 = ((float)Math.PI * 2F) * (float)segment / 48.0F;
         float a1 = ((float)Math.PI * 2F) * (float)(segment + 1) / 48.0F;
         float halfWidth = 0.025F;
         glowVertex(vertices, pose, x, Mth.cos(a0) * (radius - halfWidth), Mth.sin(a0) * (radius - halfWidth), red, green, blue, alpha);
         glowVertex(vertices, pose, x, Mth.cos(a1) * (radius - halfWidth), Mth.sin(a1) * (radius - halfWidth), red, green, blue, alpha);
         glowVertex(vertices, pose, x, Mth.cos(a1) * (radius + halfWidth), Mth.sin(a1) * (radius + halfWidth), red, green, blue, alpha);
         glowVertex(vertices, pose, x, Mth.cos(a0) * (radius + halfWidth), Mth.sin(a0) * (radius + halfWidth), red, green, blue, alpha);
      }

   }

   private static void renderEnergyStem(VertexConsumer vertices, Matrix4f pose, float age, float bloom) {
      for(int segment = 0; segment < 16; ++segment) {
         float y0 = -0.72F + (float)segment * 0.045F;
         float y1 = -0.72F + (float)(segment + 1) * 0.045F;
         float a0 = age * 0.12F + (float)segment * 0.42F;
         float a1 = age * 0.12F + (float)(segment + 1) * 0.42F;
         float r = 0.07F + bloom * 0.025F;
         float width = 0.018F;
         glowVertex(vertices, pose, Mth.cos(a0) * r - width, y0, Mth.sin(a0) * r, 0.34F, 1.0F, 0.48F, 0.62F);
         glowVertex(vertices, pose, Mth.cos(a1) * r - width, y1, Mth.sin(a1) * r, 0.34F, 1.0F, 0.48F, 0.62F);
         glowVertex(vertices, pose, Mth.cos(a1) * r + width, y1, Mth.sin(a1) * r, 0.52F, 1.0F, 0.62F, 0.42F);
         glowVertex(vertices, pose, Mth.cos(a0) * r + width, y0, Mth.sin(a0) * r, 0.52F, 1.0F, 0.62F, 0.42F);
      }

   }

   private static void vineSegment(VertexConsumer vertices, Matrix4f pose, float angle, float start, float end, float halfWidth, float red, float green, float blue, float alpha) {
      float sideX = -Mth.sin(angle) * halfWidth;
      float sideZ = Mth.cos(angle) * halfWidth;
      float sx = Mth.cos(angle) * start;
      float sz = Mth.sin(angle) * start;
      float ex = Mth.cos(angle) * end;
      float ez = Mth.sin(angle) * end;
      glowVertex(vertices, pose, sx - sideX, 0.01F, sz - sideZ, red, green, blue, alpha);
      glowVertex(vertices, pose, ex - sideX, 0.01F, ez - sideZ, red, green, blue, alpha * 0.4F);
      glowVertex(vertices, pose, ex + sideX, 0.01F, ez + sideZ, red, green, blue, alpha * 0.4F);
      glowVertex(vertices, pose, sx + sideX, 0.01F, sz + sideZ, red, green, blue, alpha);
   }

   private static void glowVertex(VertexConsumer vertices, Matrix4f pose, float x, float y, float z, float red, float green, float blue, float alpha) {
      vertices.addVertex(pose, x, y, z).setColor(red, green, blue, alpha).setUv(0.5F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0.0F, 1.0F, 0.0F);
   }

   public ResourceLocation getTextureLocation(PetalTechniqueEntity entity) {
      return WHITE_TEXTURE;
   }
}
