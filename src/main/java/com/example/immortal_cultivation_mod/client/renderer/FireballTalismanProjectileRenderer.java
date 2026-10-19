package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.entity.FireballTalismanProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class FireballTalismanProjectileRenderer extends ThrownItemRenderer<FireballTalismanProjectileEntity> {
   private static final ResourceLocation WHITE_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");

   public FireballTalismanProjectileRenderer(EntityRendererProvider.Context context) {
      super(context);
      this.shadowRadius = 0.0F;
   }

   public void render(FireballTalismanProjectileEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      if (entity.impacted()) {
         renderImpact(entity, partialTick, poseStack, buffer);
      } else {
         poseStack.pushPose();
         com.example.immortal_cultivation_mod.client.SpellProjectileVisuals.enlarge(poseStack);
         renderFlightTrail(entity, poseStack, buffer);
         float pulse = 1.28F + Mth.sin(((float)entity.tickCount + partialTick) * 0.72F) * 0.08F;
         poseStack.pushPose();
         poseStack.scale(pulse, pulse, pulse);
         super.render(entity, entityYaw, partialTick, poseStack, buffer, 15728880);
         poseStack.popPose();
         poseStack.popPose();
      }
   }

   private static void renderFlightTrail(FireballTalismanProjectileEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
      Vec3 motion = entity.getDeltaMovement();
      if (!(motion.lengthSqr() < 0.0025)) {
         Vec3 direction = motion.normalize();
         Vec3 side = direction.cross(new Vec3((double)0.0F, (double)1.0F, (double)0.0F));
         if (side.lengthSqr() < 0.001) {
            side = direction.cross(new Vec3((double)1.0F, (double)0.0F, (double)0.0F));
         }

         side = side.normalize();
         Vec3 secondSide = direction.cross(side).normalize();
         double length = 3.8 + Math.min(2.2, motion.length() * 0.9);
         Vec3 tail = direction.scale(-length);
         Matrix4f pose = poseStack.last().pose();
         VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucentEmissive(WHITE_TEXTURE));
         float red = entity.trailRed();
         float green = entity.trailGreen();
         float blue = entity.trailBlue();
         float coreRed = Mth.lerp(0.72F, red, 1.0F);
         float coreGreen = Mth.lerp(0.72F, green, 1.0F);
         float coreBlue = Mth.lerp(0.72F, blue, 1.0F);
         if (entity.attackType() == FireballTalismanProjectileEntity.AttackType.SPIRIT_SEAL) {
            Vec3 shortTail = tail.scale(0.78);

            for(int chain = 0; chain < 4; ++chain) {
               double offsetA = chain < 2 ? -0.2 : 0.2;
               double offsetB = (chain & 1) == 0 ? -0.2 : 0.2;
               Vec3 offset = side.scale(offsetA).add(secondSide.scale(offsetB));
               ribbon(vertices, pose, offset, shortTail.add(offset), side, 0.026F, red, green, blue, 0.82F);
            }

         } else {
            ribbon(vertices, pose, Vec3.ZERO, tail, side, 0.31F, red, green, blue, 0.54F);
            ribbon(vertices, pose, Vec3.ZERO, tail, secondSide, 0.31F, red, green, blue, 0.42F);
            ribbon(vertices, pose, Vec3.ZERO, tail.scale(0.82), side, 0.105F, coreRed, coreGreen, coreBlue, 0.96F);
            ribbon(vertices, pose, Vec3.ZERO, tail.scale(0.82), secondSide, 0.105F, coreRed, coreGreen, coreBlue, 0.78F);
         }
      }
   }

   private static void renderImpact(FireballTalismanProjectileEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffer) {
      float age = (float)entity.impactAge() + partialTick;
      if (entity.attackType() == FireballTalismanProjectileEntity.AttackType.SPIRIT_SEAL) {
         renderSpiritSealImpact(entity, age, poseStack, buffer);
      } else if (entity.attackType() == FireballTalismanProjectileEntity.AttackType.BLAST_WIND) {
         renderBlastWindImpact(entity, age, poseStack, buffer);
      } else {
         float progress = Mth.clamp(age / (float)entity.impactVfxTicks(), 0.0F, 1.0F);
         float expansion = Mth.sin(Mth.clamp(progress * 1.45F, 0.0F, 1.0F) * ((float)Math.PI / 2F));
         float fade = Mth.clamp(1.0F - progress, 0.0F, 1.0F);
         float flash = Mth.clamp(1.0F - progress * 2.25F, 0.0F, 1.0F);
         Matrix4f pose = poseStack.last().pose();
         VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucentEmissive(WHITE_TEXTURE));
         float red = entity.trailRed();
         float green = entity.trailGreen();
         float blue = entity.trailBlue();
         float coreRed = Mth.lerp(0.78F, red, 1.0F);
         float coreGreen = Mth.lerp(0.78F, green, 1.0F);
         float coreBlue = Mth.lerp(0.78F, blue, 1.0F);
         sphere(vertices, pose, 0.35F + expansion * 2.15F, red, green, blue, 0.24F * fade);
         sphere(vertices, pose, 0.22F + expansion * 1.2F, Mth.lerp(0.4F, red, 1.0F), Mth.lerp(0.4F, green, 1.0F), Mth.lerp(0.4F, blue, 1.0F), 0.46F * fade);
         sphere(vertices, pose, 0.16F + expansion * 0.68F, coreRed, coreGreen, coreBlue, 0.86F * Math.max(flash, fade * 0.34F));
         float ringRadius = 0.35F + expansion * 5.4F;
         flatRing(vertices, pose, ringRadius, 0.095F + fade * 0.08F, red, green, blue, 0.68F * fade);
         flatRing(vertices, pose, ringRadius * 0.62F, 0.055F, coreRed, coreGreen, coreBlue, 0.45F * fade);

         for(int ray = 0; ray < 22; ++ray) {
            Vec3 direction = burstDirection(entity.getId(), ray);
            float variation = (float)(entity.getId() * 31 + ray * 17 & 7) / 7.0F;
            double rayLength = (2.8 + (double)variation * 4.2) * (double)expansion;
            Vec3 start = direction.scale(0.18 + (double)expansion * 0.28);
            Vec3 end = direction.scale(rayLength);
            beam(vertices, pose, start, end, 0.105F + variation * 0.055F, red, green, blue, 0.52F * fade);
            if ((ray & 1) == 0) {
               beam(vertices, pose, start, end.scale(0.88), 0.035F, coreRed, coreGreen, coreBlue, 0.92F * fade);
            }
         }

      }
   }

   private static void renderSpiritSealImpact(FireballTalismanProjectileEntity entity, float age, PoseStack poseStack, MultiBufferSource buffer) {
      float progress = Mth.clamp(age / (float)entity.impactVfxTicks(), 0.0F, 1.0F);
      float fade = 1.0F - progress;
      Matrix4f pose = poseStack.last().pose();
      VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucentEmissive(WHITE_TEXTURE));
      float red = entity.trailRed();
      float green = entity.trailGreen();
      float blue = entity.trailBlue();
      sphere(vertices, pose, 0.12F + progress * 0.24F, red, green, blue, 0.55F * fade);
      flatRing(vertices, pose, 0.4F + progress * 0.52F, 0.035F, red, green, blue, 0.65F * fade);
      Vec3 center = new Vec3((double)0.0F, 0.02, (double)0.0F);
      beam(vertices, pose, center, new Vec3(-0.72, 0.18, (double)0.0F), 0.025F, red, green, blue, 0.7F * fade);
      beam(vertices, pose, center, new Vec3(0.72, 0.18, (double)0.0F), 0.025F, red, green, blue, 0.7F * fade);
      beam(vertices, pose, center, new Vec3((double)0.0F, 0.9, (double)0.0F), 0.025F, red, green, blue, 0.7F * fade);
      beam(vertices, pose, center, new Vec3((double)0.0F, -0.62, (double)0.0F), 0.025F, red, green, blue, 0.7F * fade);
   }

   private static void renderBlastWindImpact(FireballTalismanProjectileEntity entity, float age, PoseStack poseStack, MultiBufferSource buffer) {
      Matrix4f pose = poseStack.last().pose();
      VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucentEmissive(WHITE_TEXTURE));
      float red = entity.trailRed();
      float green = entity.trailGreen();
      float blue = entity.trailBlue();
      if (age < 7.0F) {
         float pulse = 0.14F + Mth.sin(age * 1.4F) * 0.025F;
         sphere(vertices, pose, pulse, red, green, blue, 0.22F);
      } else if (age < 15.0F) {
         float compression = Mth.clamp((age - 7.0F) / 8.0F, 0.0F, 1.0F);
         float radius = Mth.lerp(compression, 4.2F, 0.2F);
         sphere(vertices, pose, radius, red, green, blue, 0.055F + compression * 0.16F);
         flatRing(vertices, pose, radius, 0.055F, red, green, blue, 0.32F + compression * 0.34F);
         flatRing(vertices, pose, radius * 0.72F, 0.035F, 0.82F, 1.0F, 1.0F, 0.28F + compression * 0.25F);
      } else {
         float release = Mth.clamp((age - 15.0F) / Math.max(1.0F, (float)entity.impactVfxTicks() - 15.0F), 0.0F, 1.0F);
         float expansion = Mth.sin(release * ((float)Math.PI / 2F));
         float fade = 1.0F - release;
         sphere(vertices, pose, 0.15F + expansion * 4.1F, red, green, blue, 0.16F * fade);
         flatRing(vertices, pose, 0.3F + expansion * 5.1F, 0.13F, red, green, blue, 0.76F * fade);
         flatRing(vertices, pose, 0.2F + expansion * 3.7F, 0.055F, 0.84F, 1.0F, 1.0F, 0.6F * fade);
      }
   }

   private static Vec3 burstDirection(int seed, int index) {
      double angle = (double)index * 2.399963229728653 + (double)seed * 0.173;
      double heightBand = (double)(index % 5);
      double y = heightBand == (double)0.0F ? 0.82 : 0.06 + heightBand * 0.115;
      if ((index + seed) % 7 == 0) {
         y = -0.12;
      }

      double horizontal = Math.sqrt(Math.max((double)0.0F, (double)1.0F - y * y));
      return (new Vec3(Math.cos(angle) * horizontal, y, Math.sin(angle) * horizontal)).normalize();
   }

   private static void ribbon(VertexConsumer vertices, Matrix4f pose, Vec3 head, Vec3 tail, Vec3 side, float width, float red, float green, float blue, float alpha) {
      Vec3 headSide = side.scale((double)width);
      Vec3 tailSide = side.scale((double)(width * 0.08F));
      vertex(vertices, pose, head.subtract(headSide), red, green, blue, alpha);
      vertex(vertices, pose, head.add(headSide), red, green, blue, alpha);
      vertex(vertices, pose, tail.add(tailSide), red, green, blue, 0.0F);
      vertex(vertices, pose, tail.subtract(tailSide), red, green, blue, 0.0F);
   }

   private static void beam(VertexConsumer vertices, Matrix4f pose, Vec3 start, Vec3 end, float width, float red, float green, float blue, float alpha) {
      Vec3 direction = end.subtract(start).normalize();
      Vec3 side = direction.cross(new Vec3((double)0.0F, (double)1.0F, (double)0.0F));
      if (side.lengthSqr() < 0.001) {
         side = direction.cross(new Vec3((double)1.0F, (double)0.0F, (double)0.0F));
      }

      side = side.normalize();
      Vec3 secondSide = direction.cross(side).normalize();
      ribbon(vertices, pose, start, end, side, width, red, green, blue, alpha);
      ribbon(vertices, pose, start, end, secondSide, width, red, green, blue, alpha * 0.82F);
   }

   private static void flatRing(VertexConsumer vertices, Matrix4f pose, float radius, float halfWidth, float red, float green, float blue, float alpha) {
      float inner = Math.max(0.0F, radius - halfWidth);
      float outer = radius + halfWidth;
      int segments = 48;

      for(int segment = 0; segment < segments; ++segment) {
         float a0 = ((float)Math.PI * 2F) * (float)segment / (float)segments;
         float a1 = ((float)Math.PI * 2F) * (float)(segment + 1) / (float)segments;
         vertex(vertices, pose, new Vec3((double)(Mth.cos(a0) * inner), (double)0.0F, (double)(Mth.sin(a0) * inner)), red, green, blue, alpha);
         vertex(vertices, pose, new Vec3((double)(Mth.cos(a1) * inner), (double)0.0F, (double)(Mth.sin(a1) * inner)), red, green, blue, alpha);
         vertex(vertices, pose, new Vec3((double)(Mth.cos(a1) * outer), (double)0.0F, (double)(Mth.sin(a1) * outer)), red, green, blue, alpha * 0.25F);
         vertex(vertices, pose, new Vec3((double)(Mth.cos(a0) * outer), (double)0.0F, (double)(Mth.sin(a0) * outer)), red, green, blue, alpha * 0.25F);
      }

   }

   private static void sphere(VertexConsumer vertices, Matrix4f pose, float radius, float red, float green, float blue, float alpha) {
      int longitudes = 16;
      int latitudes = 8;

      for(int latitude = 0; latitude < latitudes; ++latitude) {
         float lat0 = (-(float)Math.PI / 2F) + (float)Math.PI * (float)latitude / (float)latitudes;
         float lat1 = (-(float)Math.PI / 2F) + (float)Math.PI * (float)(latitude + 1) / (float)latitudes;

         for(int longitude = 0; longitude < longitudes; ++longitude) {
            float lon0 = ((float)Math.PI * 2F) * (float)longitude / (float)longitudes;
            float lon1 = ((float)Math.PI * 2F) * (float)(longitude + 1) / (float)longitudes;
            sphereVertex(vertices, pose, radius, lat0, lon0, red, green, blue, alpha);
            sphereVertex(vertices, pose, radius, lat1, lon0, red, green, blue, alpha);
            sphereVertex(vertices, pose, radius, lat1, lon1, red, green, blue, alpha);
            sphereVertex(vertices, pose, radius, lat0, lon1, red, green, blue, alpha);
         }
      }

   }

   private static void sphereVertex(VertexConsumer vertices, Matrix4f pose, float radius, float latitude, float longitude, float red, float green, float blue, float alpha) {
      float cosLatitude = Mth.cos(latitude);
      Vec3 point = new Vec3((double)(cosLatitude * Mth.cos(longitude) * radius), (double)(Mth.sin(latitude) * radius), (double)(cosLatitude * Mth.sin(longitude) * radius));
      vertex(vertices, pose, point, red, green, blue, alpha);
   }

   private static void vertex(VertexConsumer vertices, Matrix4f pose, Vec3 point, float red, float green, float blue, float alpha) {
      vertices.addVertex(pose, (float)point.x, (float)point.y, (float)point.z).setColor(red, green, blue, alpha).setUv(0.5F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0.0F, 1.0F, 0.0F);
   }
}
