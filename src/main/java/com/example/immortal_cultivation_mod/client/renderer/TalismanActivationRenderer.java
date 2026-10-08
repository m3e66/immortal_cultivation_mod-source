package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.entity.TalismanActivationEntity;
import com.example.immortal_cultivation_mod.item.UtilityTalismanItem;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class TalismanActivationRenderer extends EntityRenderer<TalismanActivationEntity> {
   private static final int EDGE_SEGMENTS = 12;
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "textures/item/talisman.png");
   private static final ResourceLocation WHITE_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");

   public TalismanActivationRenderer(EntityRendererProvider.Context context) {
      super(context);
      this.shadowRadius = 0.0F;
   }

   public void render(TalismanActivationEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      Player owner = entity.owner();
      if (owner != null) {
         Vec3 entityPosition = new Vec3(Mth.lerp((double)partialTick, entity.xo, entity.getX()), Mth.lerp((double)partialTick, entity.yo, entity.getY()), Mth.lerp((double)partialTick, entity.zo, entity.getZ()));
         Vec3 ownerPosition = new Vec3(Mth.lerp((double)partialTick, owner.xo, owner.getX()), Mth.lerp((double)partialTick, owner.yo, owner.getY()) + (double)owner.getBbHeight() * 0.58, Mth.lerp((double)partialTick, owner.zo, owner.getZ()));
         Vec3 towardOwner = ownerPosition.subtract(entityPosition);
         float inwardYaw = (float)Math.toDegrees(Mth.atan2(towardOwner.x, towardOwner.z));
         float burn = entity.burnProgress(partialTick);
         float half = 0.46F;
         if (!(burn >= 0.999F)) {
            if (entity.kind() == UtilityTalismanItem.Kind.MARK_ENTITY) {
               renderMarkingLink(entity, partialTick, entityPosition, poseStack.last().pose(), buffer);
            }

            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(inwardYaw));
            Matrix4f pose = poseStack.last().pose();
            renderRemainingPaper(entity, partialTick, burn, half, pose, buffer);
            if (entity.kind() == UtilityTalismanItem.Kind.MARK_ENTITY) {
               renderMarkingRunes(entity, partialTick, half, pose, buffer);
            }

            if (burn > 0.001F) {
               renderBurnFront(entity, partialTick, burn, half, pose, buffer);
            }

            poseStack.popPose();
            super.render(entity, entityYaw, partialTick, poseStack, buffer, 15728880);
         }
      }
   }

   private static void renderMarkingLink(TalismanActivationEntity paper, float partialTick, Vec3 paperPosition, Matrix4f pose, MultiBufferSource buffer) {
      Entity target = paper.targetEntity();
      if (target != null && target.isAlive()) {
         Vec3 targetPosition = new Vec3(Mth.lerp((double)partialTick, target.xo, target.getX()), Mth.lerp((double)partialTick, target.yo, target.getY()) + (double)target.getBbHeight() * 0.72, Mth.lerp((double)partialTick, target.zo, target.getZ()));
         Vec3 end = targetPosition.subtract(paperPosition);
         double length = end.length();
         if (!(length < 0.05)) {
            Vec3 direction = end.scale((double)1.0F / length);
            Vec3 side = direction.cross(new Vec3((double)0.0F, (double)1.0F, (double)0.0F));
            if (side.lengthSqr() < 1.0E-5) {
               side = direction.cross(new Vec3((double)1.0F, (double)0.0F, (double)0.0F));
            }

            side = side.normalize();
            Vec3 secondSide = direction.cross(side).normalize();
            VertexConsumer glow = buffer.getBuffer(RenderType.entityTranslucentEmissive(WHITE_TEXTURE));
            float age = (float)paper.tickCount + partialTick;
            int segments = Math.max(3, Math.min(28, Mth.ceil(length / (double)5.0F)));

            for(int segment = 0; segment < segments; ++segment) {
               if ((segment + Mth.floor(age * 0.35F) & 3) != 3) {
                  double startProgress = (double)segment / (double)segments;
                  double endProgress = Math.min((double)1.0F, startProgress + 0.7 / (double)segments);
                  Vec3 start = end.scale(startProgress);
                  Vec3 stop = end.scale(endProgress);
                  float pulse = 0.56F + 0.44F * Mth.sin(age * 0.22F - (float)segment * 0.71F);
                  markRibbon(glow, pose, start, stop, side, 0.018F, 1.0F, 0.84F, 0.34F, 0.17F + pulse * 0.18F);
                  markRibbon(glow, pose, start, stop, secondSide, 0.01F, 0.52F, 0.94F, 1.0F, 0.1F + pulse * 0.12F);
               }
            }

         }
      }
   }

   private static void renderMarkingRunes(TalismanActivationEntity entity, float partialTick, float half, Matrix4f pose, MultiBufferSource buffer) {
      VertexConsumer glow = buffer.getBuffer(RenderType.entityTranslucentEmissive(WHITE_TEXTURE));
      float age = (float)entity.tickCount + partialTick;
      int litStrokes = Mth.clamp(Mth.floor(age / 3.0F) + 1, 1, 7);
      float[][] strokes = new float[][]{{-0.18F, 0.22F, 0.18F, 0.22F}, {-0.11F, 0.1F, 0.12F, 0.1F}, {-0.16F, -0.02F, 0.16F, -0.02F}, {-0.09F, -0.14F, 0.13F, -0.14F}, {-0.2F, -0.27F, 0.03F, -0.27F}, {0.02F, -0.35F, 0.2F, -0.35F}, {-0.15F, 0.34F, 0.07F, 0.34F}};

      for(int index = 0; index < litStrokes; ++index) {
         float[] stroke = strokes[index];
         float y = Mth.clamp(stroke[1], -half, half);
         float x0 = Mth.clamp(stroke[0], -half, half);
         float x1 = Mth.clamp(stroke[2], -half, half);
         float flicker = 0.72F + 0.28F * Mth.sin(age * 0.55F + (float)index);
         glowStrip(glow, pose, x0, x1, y, y, 0.011F, 0.011F, 1.0F, 0.76F, 0.22F, 0.48F * flicker, 0.012F);
      }

   }

   private static void markRibbon(VertexConsumer vertices, Matrix4f pose, Vec3 start, Vec3 end, Vec3 side, float width, float red, float green, float blue, float alpha) {
      Vec3 offset = side.scale((double)width);
      Vec3 startA = start.add(offset);
      Vec3 startB = start.subtract(offset);
      Vec3 endA = end.add(offset.scale(0.35));
      Vec3 endB = end.subtract(offset.scale(0.35));
      coloredVertex(vertices, pose, (float)startA.x, (float)startA.y, (float)startA.z, 0.0F, 0.0F, red, green, blue, 0.0F);
      coloredVertex(vertices, pose, (float)startB.x, (float)startB.y, (float)startB.z, 1.0F, 0.0F, red, green, blue, alpha);
      coloredVertex(vertices, pose, (float)endB.x, (float)endB.y, (float)endB.z, 1.0F, 1.0F, red, green, blue, alpha * 0.72F);
      coloredVertex(vertices, pose, (float)endA.x, (float)endA.y, (float)endA.z, 0.0F, 1.0F, red, green, blue, 0.0F);
   }

   private static void renderRemainingPaper(TalismanActivationEntity entity, float partialTick, float burn, float half, Matrix4f pose, MultiBufferSource buffer) {
      VertexConsumer paper = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

      for(int segment = 0; segment < 12; ++segment) {
         float x0 = Mth.lerp((float)segment / 12.0F, -half, half);
         float x1 = Mth.lerp((float)(segment + 1) / 12.0F, -half, half);
         float u0 = (float)segment / 12.0F;
         float u1 = (float)(segment + 1) / 12.0F;
         float y0 = burnEdgeY(entity, partialTick, burn, half, segment);
         float y1 = burnEdgeY(entity, partialTick, burn, half, segment + 1);
         float v0 = Mth.clamp((half - y0) / (half * 2.0F), 0.0F, 1.0F);
         float v1 = Mth.clamp((half - y1) / (half * 2.0F), 0.0F, 1.0F);
         vertex(paper, pose, x0, half, 0.0F, u0, 0.0F);
         vertex(paper, pose, x0, y0, 0.0F, u0, v0);
         vertex(paper, pose, x1, y1, 0.0F, u1, v1);
         vertex(paper, pose, x1, half, 0.0F, u1, 0.0F);
      }

   }

   private static void renderBurnFront(TalismanActivationEntity entity, float partialTick, float burn, float half, Matrix4f pose, MultiBufferSource buffer) {
      VertexConsumer charred = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));

      for(int segment = 0; segment < 12; ++segment) {
         float x0 = Mth.lerp((float)segment / 12.0F, -half, half);
         float x1 = Mth.lerp((float)(segment + 1) / 12.0F, -half, half);
         float u0 = (float)segment / 12.0F;
         float u1 = (float)(segment + 1) / 12.0F;
         float y0 = burnEdgeY(entity, partialTick, burn, half, segment);
         float y1 = burnEdgeY(entity, partialTick, burn, half, segment + 1);
         float charTop0 = Math.min(half, y0 + 0.052F);
         float charTop1 = Math.min(half, y1 + 0.052F);
         float edgeV0 = Mth.clamp((half - y0) / (half * 2.0F), 0.0F, 1.0F);
         float edgeV1 = Mth.clamp((half - y1) / (half * 2.0F), 0.0F, 1.0F);
         float topV0 = Mth.clamp((half - charTop0) / (half * 2.0F), 0.0F, 1.0F);
         float topV1 = Mth.clamp((half - charTop1) / (half * 2.0F), 0.0F, 1.0F);
         coloredVertex(charred, pose, x0, charTop0, 0.003F, u0, topV0, 70, 24, 8, 225);
         coloredVertex(charred, pose, x0, y0, 0.003F, u0, edgeV0, 28, 8, 2, 245);
         coloredVertex(charred, pose, x1, y1, 0.003F, u1, edgeV1, 28, 8, 2, 245);
         coloredVertex(charred, pose, x1, charTop1, 0.003F, u1, topV1, 70, 24, 8, 225);
      }

      VertexConsumer glow = buffer.getBuffer(RenderType.entityTranslucentEmissive(WHITE_TEXTURE));

      for(int segment = 0; segment < 12; ++segment) {
         float x0 = Mth.lerp((float)segment / 12.0F, -half, half);
         float x1 = Mth.lerp((float)(segment + 1) / 12.0F, -half, half);
         float y0 = burnEdgeY(entity, partialTick, burn, half, segment);
         float y1 = burnEdgeY(entity, partialTick, burn, half, segment + 1);
         glowStrip(glow, pose, x0, x1, y0, y1, 0.034F, 0.018F, 1.0F, 0.25F, 0.025F, 0.46F, 0.005F);
         glowStrip(glow, pose, x0, x1, y0, y1, 0.014F, 0.008F, 1.0F, 0.86F, 0.3F, 0.92F, 0.008F);
      }

      int tongues = 7;
      float age = (float)entity.tickCount + partialTick;

      for(int tongue = 0; tongue < tongues; ++tongue) {
         float fraction = ((float)tongue + 0.5F) / (float)tongues;
         float centerX = Mth.lerp(fraction, -half, half);
         int edgeIndex = Mth.clamp(Math.round(fraction * 12.0F), 0, 12);
         float baseY = burnEdgeY(entity, partialTick, burn, half, edgeIndex);
         float wave = 0.5F + 0.5F * Mth.sin(age * (0.72F + (float)tongue * 0.035F) + (float)tongue * 1.91F + (float)entity.getId() * 0.37F);
         float width = 0.025F + wave * 0.026F;
         float height = 0.065F + wave * 0.095F;
         float sway = Mth.sin(age * 0.43F + (float)tongue * 1.37F) * 0.024F;
         flameTongue(glow, pose, centerX, baseY, width, height, sway, 1.0F, 0.2F, 0.015F, 0.68F, 0.01F);
         flameTongue(glow, pose, centerX, baseY + 0.002F, width * 0.47F, height * 0.62F, sway * 0.55F, 1.0F, 0.9F, 0.42F, 0.92F, 0.013F);
      }

   }

   private static float burnEdgeY(TalismanActivationEntity entity, float partialTick, float burn, float half, int edgeIndex) {
      float base = -half + half * 2.0F * burn;
      float edgeFade = Math.min(1.0F, burn * 6.0F) * Math.min(1.0F, (1.0F - burn) * 6.0F);
      float age = (float)entity.tickCount + partialTick;
      float phase = (float)entity.getId() * 0.61F + (float)edgeIndex * 1.73F + age * 0.11F;
      float variation = (Mth.sin(phase) * 0.025F + Mth.sin(phase * 2.13F + 0.7F) * 0.012F) * edgeFade;
      return Mth.clamp(base + variation, -half, half);
   }

   private static void glowStrip(VertexConsumer vertices, Matrix4f pose, float x0, float x1, float y0, float y1, float above, float below, float red, float green, float blue, float alpha, float z) {
      coloredVertex(vertices, pose, x0, y0 + above, z, 0.0F, 0.0F, red, green, blue, alpha);
      coloredVertex(vertices, pose, x0, y0 - below, z, 0.0F, 1.0F, red, green, blue, alpha * 0.35F);
      coloredVertex(vertices, pose, x1, y1 - below, z, 1.0F, 1.0F, red, green, blue, alpha * 0.35F);
      coloredVertex(vertices, pose, x1, y1 + above, z, 1.0F, 0.0F, red, green, blue, alpha);
   }

   private static void flameTongue(VertexConsumer vertices, Matrix4f pose, float centerX, float baseY, float width, float height, float sway, float red, float green, float blue, float alpha, float z) {
      float tipX = centerX + sway;
      coloredVertex(vertices, pose, tipX - width * 0.08F, baseY + height, z, 0.45F, 0.0F, red, green, blue, 0.0F);
      coloredVertex(vertices, pose, centerX - width, baseY - 0.012F, z, 0.0F, 1.0F, red, green, blue, alpha);
      coloredVertex(vertices, pose, centerX + width, baseY - 0.012F, z, 1.0F, 1.0F, red, green, blue, alpha);
      coloredVertex(vertices, pose, tipX + width * 0.08F, baseY + height, z, 0.55F, 0.0F, red, green, blue, 0.0F);
   }

   private static void vertex(VertexConsumer vertices, Matrix4f pose, float x, float y, float z, float u, float v) {
      coloredVertex(vertices, pose, x, y, z, u, v, 255, 255, 255, 255);
   }

   private static void coloredVertex(VertexConsumer vertices, Matrix4f pose, float x, float y, float z, float u, float v, int red, int green, int blue, int alpha) {
      vertices.addVertex(pose, x, y, z).setColor(red, green, blue, alpha).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0.0F, 0.0F, 1.0F);
   }

   private static void coloredVertex(VertexConsumer vertices, Matrix4f pose, float x, float y, float z, float u, float v, float red, float green, float blue, float alpha) {
      vertices.addVertex(pose, x, y, z).setColor(red, green, blue, alpha).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0.0F, 0.0F, 1.0F);
   }

   public ResourceLocation getTextureLocation(TalismanActivationEntity entity) {
      return TEXTURE;
   }
}
