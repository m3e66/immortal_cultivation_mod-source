package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.entity.MethodTechniqueAreaEntity;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public final class MethodTechniqueAreaRenderer extends EntityRenderer<MethodTechniqueAreaEntity> {
   private static final ResourceLocation WHITE_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");

   public MethodTechniqueAreaRenderer(EntityRendererProvider.Context context) {
      super(context);
      this.shadowRadius = 0.0F;
   }

   public void render(MethodTechniqueAreaEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      float age = (float)entity.tickCount + partialTick;
      float ending = endFade(age, entity.visualDuration(), entity.kind() == MethodTechniqueAreaEntity.Kind.HANYUAN_DOMAIN ? 22.0F : 10.0F);
      switch (entity.kind()) {
         case JINYAN_SPIKES -> renderJinyan(entity, age, ending, poseStack, buffer);
         case SHANSUO_DOMAIN -> renderShansuo(entity, age, ending, poseStack, buffer);
         case WANFENG_BURIAL -> renderWanfeng(entity, age, ending, poseStack, buffer);
         case CANGYUAN_FLOOD -> renderFlood(entity, age, ending, poseStack, buffer);
         case HANYUAN_DOMAIN -> renderHanyuan(entity, age, ending, poseStack, buffer);
         case XUANYUE_WALL -> renderWall(entity, age, ending, poseStack, buffer);
         case CHANGQING_GROWTH -> renderChangqingGrowth(age, ending, poseStack, buffer);
         case CHANGQING_VINES -> renderChangqingVines(age, ending, poseStack, buffer);
      }

      super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
   }

   private static void renderJinyan(MethodTechniqueAreaEntity entity, float age, float ending, PoseStack poseStack, MultiBufferSource buffer) {
      float appear = Mth.clamp(age / 6.0F, 0.0F, 1.0F) * ending;
      renderDisc(poseStack, buffer, 4.25F, 0.018F, 0.12F, 0.065F, 0.015F, 0.14F * appear, 64);
      renderRing(poseStack, buffer, 3.85F, 4.18F, 0.028F, 1.0F, 0.69F, 0.12F, 0.58F * appear, 64, age * 1.7F);
      renderRadials(poseStack, buffer, 0.35F, 3.75F, 0.032F, 0.045F, 14, age * 0.33F, 1.0F, 0.72F, 0.18F, 0.48F * appear);

      for(int blade = 0; blade < 14; ++blade) {
         float growth = Mth.clamp((age - (float)blade * 0.62F) / 4.0F, 0.0F, 1.0F) * ending;
         if (!(growth <= 0.01F)) {
            float angle = (float)blade * 2.39996F;
            float radius = 0.8F + (float)(blade % 5) * 0.72F;
            float scale = (0.72F + (float)(blade % 3) * 0.16F) * growth;
            renderVerticalModel(ModItems.SPELL_MODEL_METAL_BLADE.toStack(), entity, poseStack, buffer, Mth.cos(angle) * radius, -0.38F + growth * 0.75F, Mth.sin(angle) * radius, angle * (180F / (float)Math.PI), (blade & 1) == 0 ? -8.0F : 8.0F, scale, 2300 + blade);
         }
      }

   }

   private static void renderWall(MethodTechniqueAreaEntity entity, float age, float ending, PoseStack poseStack, MultiBufferSource buffer) {
      int realm = Math.max(0, Math.round(entity.power()));
      int width = 5 + realm * 2;
      int height = 4 + realm;
      float appear = Mth.clamp(age / 12.0F, 0.0F, 1.0F) * ending;
      poseStack.pushPose();
      poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
      renderWallVeins(poseStack, buffer, (float)width * 0.52F, (float)height, age, appear, realm);

      for(int column = 0; column < width; ++column) {
         float delay = Math.abs((float)column - (float)(width - 1) * 0.5F) * 0.8F;
         float growth = Mth.clamp((age - delay) / 7.0F, 0.0F, 1.0F) * ending;
         if (!(growth <= 0.01F)) {
            float x = (float)column - (float)(width - 1) * 0.5F;
            renderVerticalModel(ModItems.SPELL_MODEL_EARTH_SPEAR.toStack(), entity, poseStack, buffer, x, -1.05F + growth * 1.12F, -0.58F, (float)column * 23.0F, (float)(column % 3 - 1) * 5.0F, (1.25F + (float)(column % 3) * 0.14F) * growth, 1800 + column);
         }
      }

      if (realm >= 1) {
         int wingLength = realm + 2;

         for(int sign : new int[]{-1, 1}) {
            float edgeX = (float)(sign * width) * 0.5F;
            renderWallSideVeins(poseStack, buffer, edgeX, sign, wingLength, (float)height - 1.0F, age, appear, realm);

            for(int depth = 1; depth <= wingLength; ++depth) {
               float delay = (float)width * 0.38F + (float)depth * 0.95F;
               float growth = Mth.clamp((age - delay) / 7.0F, 0.0F, 1.0F) * ending;
               if (!(growth <= 0.01F)) {
                  renderVerticalModel(ModItems.SPELL_MODEL_EARTH_SPEAR.toStack(), entity, poseStack, buffer, edgeX, -0.98F + growth * 1.06F, (float)(-depth), sign > 0 ? 90.0F : -90.0F, (float)(depth % 3 - 1) * 5.0F, (1.18F + (float)(depth % 2) * 0.16F) * growth, 1900 + sign * 50 + depth);
               }
            }
         }
      }

      poseStack.popPose();
      renderRing(poseStack, buffer, 0.5F + appear * 2.8F, 0.8F + appear * 3.2F, 0.035F, 0.78F, 0.48F, 0.08F, 0.38F * (1.0F - Mth.clamp(age / 18.0F, 0.0F, 1.0F)) * ending, 56, 0.0F);
   }

   private static void renderShansuo(MethodTechniqueAreaEntity entity, float age, float ending, PoseStack poseStack, MultiBufferSource buffer) {
      float radius = 6.0F + Math.max(0.0F, entity.power() - 1.0F) * 1.5F;
      float appear = Mth.clamp(age / 8.0F, 0.0F, 1.0F) * ending;
      renderDisc(poseStack, buffer, radius, 0.014F, 0.1F, 0.055F, 0.012F, 0.18F * appear, 80);

      for(int ring = 0; ring < 4; ++ring) {
         float outer = radius * (0.3F + (float)ring * 0.22F);
         renderRing(poseStack, buffer, outer - 0.08F, outer, 0.025F, 0.82F, 0.54F, 0.1F, (0.34F - (float)ring * 0.035F) * appear, 72, (ring & 1) == 0 ? age * 0.45F : -age * 0.38F);
      }

      renderRadials(poseStack, buffer, 0.45F, radius * 0.92F, 0.03F, 0.07F, 16, -age * 0.18F, 0.7F, 0.42F, 0.08F, 0.34F * appear);

      for(int band = 0; band < 4; ++band) {
         float cycle = Mth.frac(age * 0.035F + (float)band * 0.25F);
         float y = 6.5F * (1.0F - cycle) + 0.35F;
         float bandRadius = radius * (0.78F - (float)band * 0.055F);
         renderRing(poseStack, buffer, bandRadius - 0.13F, bandRadius, y, 0.88F, 0.62F, 0.16F, 0.25F * appear * (0.45F + cycle * 0.55F), 80, age * (band % 2 == 0 ? 0.8F : -0.65F));
      }

      renderCylinder(poseStack, buffer, radius * 0.96F, 3.4F, 0.42F, 0.24F, 0.05F, 0.055F * appear, 72, age * 0.01F);
   }

   private static void renderWanfeng(MethodTechniqueAreaEntity entity, float age, float ending, PoseStack poseStack, MultiBufferSource buffer) {
      float warning = Mth.clamp(age / 14.0F, 0.0F, 1.0F) * ending;
      float collapse = Mth.clamp((age - 88.0F) / 16.0F, 0.0F, 1.0F);
      float rubbleFade = 1.0F - Mth.clamp((age - 106.0F) / 18.0F, 0.0F, 1.0F);
      renderDisc(poseStack, buffer, 15.0F, 0.014F, 0.055F, 0.035F, 0.018F, 0.15F * warning, 96);
      renderRing(poseStack, buffer, 14.35F, 14.9F, 0.027F, 0.78F, 0.48F, 0.08F, 0.68F * warning, 96, age * 0.45F);
      renderRadials(poseStack, buffer, 0.55F, 14.1F, 0.031F, 0.075F, 24, age * 0.16F, 0.92F, 0.57F, 0.09F, 0.5F * warning);
      if (age < 14.0F) {
         float gathering = Mth.clamp(age / 14.0F, 0.0F, 1.0F);

         for(int fracture = 0; fracture < 3; ++fracture) {
            float outer = 3.5F + (float)fracture * 4.65F + gathering * 0.9F;
            renderRing(poseStack, buffer, outer - 0.055F, outer, 0.038F, 0.78F, 0.45F, 0.07F, (0.24F + (float)fracture * 0.06F) * gathering, 72, fracture % 2 == 0 ? age * 0.18F : -age * 0.14F);
         }
      }

      for(int pillar = 0; pillar < 18; ++pillar) {
         float growth = Mth.clamp((age - 10.0F - (float)pillar * 0.72F) / 9.0F, 0.0F, 1.0F) * ending;
         if (!(growth <= 0.01F)) {
            float angle = ((float)Math.PI * 2F) * (float)pillar / 18.0F;
            float radius = 11.8F + (float)(pillar & 1) * 1.45F;
            float currentRadius = Mth.lerp(collapse, radius, 0.85F + (float)(pillar % 3) * 0.22F);
            float tilt = (float)(pillar % 3 - 1) * 6.5F + collapse * 67.0F;
            ItemStack var10000;
            switch (pillar % 3) {
               case 0 -> var10000 = ModItems.SPELL_MODEL_MOUNTAIN_PILLAR.toStack();
               case 1 -> var10000 = ModItems.SPELL_MODEL_MOUNTAIN_CLIFF.toStack();
               default -> var10000 = ModItems.SPELL_MODEL_MOUNTAIN_SPIRE.toStack();
            }

            ItemStack mountain = var10000;
            float var30;
            switch (pillar % 3) {
               case 0 -> var30 = 1.82F;
               case 1 -> var30 = 1.68F;
               default -> var30 = 1.74F;
            }

            float baseScale = var30;
            renderVerticalModel(mountain, entity, poseStack, buffer, Mth.cos(angle) * currentRadius, -1.35F + growth * 1.55F - collapse * 0.42F, Mth.sin(angle) * currentRadius, -angle * (180F / (float)Math.PI) + collapse * (pillar % 2 == 0 ? 12.0F : -12.0F), tilt, baseScale * growth * rubbleFade, 4100 + pillar);
         }
      }

      if (age >= 36.0F && age < 116.0F) {
         float metalFade = Mth.clamp((age - 36.0F) / 8.0F, 0.0F, 1.0F) * rubbleFade * ending;

         for(int blade = 0; blade < 54; ++blade) {
            float growth = Mth.clamp((age - 39.0F - (float)blade * 0.22F) / 5.0F, 0.0F, 1.0F) * metalFade;
            if (!(growth <= 0.01F)) {
               float angle = ((float)Math.PI * 2F) * (float)blade / 54.0F + (float)(blade % 3) * 0.075F;
               float radius = 2.2F + (float)(blade % 9) * 1.42F;
               float currentRadius = Mth.lerp(collapse, radius, 0.25F + (float)(blade % 4) * 0.13F);
               float height = 0.3F + (float)(blade % 5) * 0.82F + collapse * 0.45F;
               ItemStack bladeModel = ModItems.SPELL_MODEL_WANFENG_METAL_BLOOM.toStack();
               renderVerticalModel(bladeModel, entity, poseStack, buffer, Mth.cos(angle) * currentRadius, height, Mth.sin(angle) * currentRadius, -angle * (180F / (float)Math.PI) + 90.0F + collapse * (float)blade * 13.0F, 28.0F + (float)(blade % 4) * 9.0F + collapse * 18.0F, (0.66F + (float)(blade % 4) * 0.08F) * growth, 4700 + blade);
            }
         }
      }

      if (age >= 88.0F) {
         float collapseRadius = Mth.lerp(collapse, 14.85F, 0.55F);
         renderRing(poseStack, buffer, Math.max(0.0F, collapseRadius - 0.34F), collapseRadius, 0.06F, 0.92F, 0.62F, 0.16F, 0.82F * (1.0F - collapse) * ending, 96, -age * 0.8F);
      }

      if (age >= 101.0F && age <= 112.0F) {
         float rise = Mth.clamp((age - 101.0F) / 3.0F, 0.0F, 1.0F);
         float fade = 1.0F - Mth.clamp((age - 106.0F) / 6.0F, 0.0F, 1.0F);
         renderVerticalModel(ModItems.SPELL_MODEL_WANFENG_METAL_BLOOM.toStack(), entity, poseStack, buffer, 0.0F, -0.25F + rise * 1.4F, 0.0F, age * 8.0F, 0.0F, 4.2F * rise * fade, 5250);
         renderRing(poseStack, buffer, 0.2F, 2.8F * rise, 0.09F, 1.0F, 0.78F, 0.25F, 0.78F * fade, 72, age * 2.0F);
      }

   }

   private static void renderFlood(MethodTechniqueAreaEntity entity, float age, float ending, PoseStack poseStack, MultiBufferSource buffer) {
      float appear = Mth.clamp(age / 4.0F, 0.0F, 1.0F) * ending;
      float empowered = Mth.clamp(entity.power(), 0.0F, 1.0F);
      poseStack.pushPose();
      poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
      poseStack.translate((double)0.0F, -0.12, 0.22);
      poseStack.scale(6.45F + empowered * 0.75F, (3.75F + empowered * 0.85F) * appear, 3.25F + empowered * 0.42F);
      renderModel(ModItems.SPELL_MODEL_WAVE.toStack(), entity, poseStack, buffer, 6100);
      poseStack.popPose();
      poseStack.pushPose();
      poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
      renderWaterSheet(poseStack, buffer, 4.7F + empowered, 3.4F, 0.06F, 0.34F, 0.72F, 0.3F * appear);

      for(int crest = 0; crest < 3; ++crest) {
         float y = 2.9F + (float)crest * 0.38F;
         renderHorizontalBand(poseStack, buffer, 4.6F + empowered * 0.55F, -0.2F + (float)crest * 0.18F, y, 0.78F, 0.94F, 1.0F, (0.25F - (float)crest * 0.045F) * appear);
      }

      poseStack.popPose();
   }

   private static void renderHanyuan(MethodTechniqueAreaEntity entity, float age, float ending, PoseStack poseStack, MultiBufferSource buffer) {
      float front = age < 16.0F ? age / 16.0F : 1.0F;
      float radius = 40.0F * Mth.clamp(front, 0.0F, 1.0F);
      float appear = Mth.clamp(age / 8.0F, 0.0F, 1.0F) * ending;
      renderDisc(poseStack, buffer, Math.max(0.2F, radius), 0.012F, 0.012F, 0.065F, 0.14F, 0.12F * appear, 112);
      renderRing(poseStack, buffer, Math.max(0.0F, radius - 1.0F), radius, 0.028F, 0.62F, 0.92F, 1.0F, 0.74F * appear, 112, age * 0.42F);
      if (age >= 14.0F) {
         renderRing(poseStack, buffer, 10.5F, 12.0F, 0.022F, 0.04F, 0.18F, 0.42F, 0.34F * ending, 88, -age * 0.12F);
         renderRing(poseStack, buffer, 21.0F, 22.2F, 0.024F, 0.07F, 0.3F, 0.62F, 0.27F * ending, 104, age * 0.1F);
         renderRadials(poseStack, buffer, 2.0F, 38.0F, 0.026F, 0.075F, 24, age * 0.035F, 0.16F, 0.52F, 0.86F, 0.24F * ending);
         renderCylinder(poseStack, buffer, 39.5F, 1.25F, 0.52F, 0.86F, 1.0F, 0.042F * ending, 112, -age * 0.002F);

         for(int spike = 0; spike < 18; ++spike) {
            float angle = ((float)Math.PI * 2F) * (float)spike / 18.0F;
            float spikeRadius = 39.0F + (float)(spike % 3 - 1) * 0.75F;
            float scale = 1.8F + (float)(spike % 4) * 0.52F;
            renderVerticalModel(ModItems.SPELL_MODEL_ICE_SPIKE.toStack(), entity, poseStack, buffer, Mth.cos(angle) * spikeRadius, scale * 0.22F, Mth.sin(angle) * spikeRadius, angle * (180F / (float)Math.PI), (float)(spike % 3 - 1) * 7.0F, scale * ending, 7200 + spike);
         }
      }

      if (age >= (float)entity.visualDuration() - 22.0F) {
         float retreat = Mth.clamp(((float)entity.visualDuration() - age) / 22.0F, 0.0F, 1.0F);
         float retreatRadius = 40.0F * retreat;
         renderRing(poseStack, buffer, Math.max(0.0F, retreatRadius - 0.85F), retreatRadius, 0.055F, 0.72F, 0.94F, 1.0F, 0.72F * (1.0F - Math.abs(retreat - 0.5F)) * 1.4F, 112, -age * 0.8F);
      }

   }

   private static void renderChangqingGrowth(float age, float ending, PoseStack poseStack, MultiBufferSource buffer) {
      float appear = Mth.clamp(age / 10.0F, 0.0F, 1.0F) * ending;
      float breath = 0.86F + Mth.sin(age * 0.075F) * 0.14F;
      renderDisc(poseStack, buffer, 9.0F, 0.014F, 0.025F, 0.22F, 0.055F, 0.095F * appear, 88);

      for(int ring = 0; ring < 3; ++ring) {
         float outer = 3.1F + (float)ring * 2.75F + Mth.sin(age * (0.025F + (float)ring * 0.006F) + (float)ring * 1.8F) * 0.22F;
         float rotation = ring % 2 == 0 ? age * 0.16F : -age * 0.11F;
         renderRing(poseStack, buffer, outer - 0.07F, outer, 0.025F + (float)ring * 0.003F, ring == 1 ? 0.92F : 0.17F, ring == 1 ? 0.7F : 0.76F, ring == 1 ? 0.18F : 0.22F, (0.25F - (float)ring * 0.025F) * appear * breath, 80, rotation);
      }

      renderRadials(poseStack, buffer, 0.45F, 8.55F, 0.031F, 0.026F, 18, age * 0.045F, 0.35F, 0.9F, 0.28F, 0.24F * appear);
      renderCylinder(poseStack, buffer, 8.9F, 1.15F, 0.3F, 0.9F, 0.32F, 0.035F * appear, 88, age * 0.0015F);
   }

   private static void renderChangqingVines(float age, float ending, PoseStack poseStack, MultiBufferSource buffer) {
      float appear = Mth.clamp(age / 6.0F, 0.0F, 1.0F) * ending;
      float tighten = 4.65F - Mth.clamp(age / 40.0F, 0.0F, 1.0F) * 0.65F;
      renderDisc(poseStack, buffer, 5.0F, 0.016F, 0.018F, 0.16F, 0.035F, 0.11F * appear, 72);
      renderRing(poseStack, buffer, tighten - 0.13F, tighten, 0.038F, 0.24F, 0.84F, 0.22F, 0.48F * appear, 72, -age * 0.62F);
      renderRing(poseStack, buffer, 2.4F, 2.48F, 0.045F, 0.94F, 0.71F, 0.18F, 0.31F * appear, 56, age * 0.38F);
      renderRadials(poseStack, buffer, 0.32F, 4.7F, 0.035F, 0.045F, 12, -age * 0.21F, 0.14F, 0.64F, 0.16F, 0.38F * appear);
   }

   private static float endFade(float age, int duration, float fadeTicks) {
      return age < (float)duration - fadeTicks ? 1.0F : 1.0F - Mth.clamp((age - ((float)duration - fadeTicks)) / fadeTicks, 0.0F, 1.0F);
   }

   private static void renderVerticalModel(ItemStack stack, MethodTechniqueAreaEntity entity, PoseStack poseStack, MultiBufferSource buffer, float x, float y, float z, float yaw, float tilt, float scale, int seed) {
      if (!(scale <= 0.01F)) {
         poseStack.pushPose();
         poseStack.translate(x, y, z);
         poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
         poseStack.mulPose(Axis.ZP.rotationDegrees(tilt));
         poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
         poseStack.scale(scale, scale, scale);
         renderModel(stack, entity, poseStack, buffer, seed);
         poseStack.popPose();
      }
   }

   private static void renderModel(ItemStack stack, MethodTechniqueAreaEntity entity, PoseStack poseStack, MultiBufferSource buffer, int seed) {
      Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, 15728880, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId() + seed);
   }

   private static void renderWallVeins(PoseStack poseStack, MultiBufferSource buffer, float halfWidth, float height, float age, float alpha, int realm) {
      Matrix4f pose = poseStack.last().pose();
      VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucentEmissive(WHITE_TEXTURE));
      float z = -0.66F;
      int veins = 7 + realm * 2;

      for(int vein = 0; vein < veins; ++vein) {
         float x = Mth.lerp((float)vein / (float)Math.max(1, veins - 1), -halfWidth, halfWidth);
         float sway = Mth.sin((float)vein * 2.17F + age * 0.035F) * 0.28F;
         float width = 0.022F + (float)realm * 0.005F;
         glowVertex(vertices, pose, x - width, 0.1F, z, 0.0F, 0.0F, 0.72F, 0.4F, 0.06F, alpha * 0.18F);
         glowVertex(vertices, pose, x + width, 0.1F, z, 1.0F, 0.0F, 0.72F, 0.4F, 0.06F, alpha * 0.18F);
         glowVertex(vertices, pose, x + sway + width, height * 0.92F, z, 1.0F, 1.0F, 1.0F, 0.72F, 0.13F, alpha * 0.72F);
         glowVertex(vertices, pose, x + sway - width, height * 0.92F, z, 0.0F, 1.0F, 1.0F, 0.72F, 0.13F, alpha * 0.72F);
      }

      for(int seam = 1; (float)seam < height; ++seam) {
         float y = (float)seam + Mth.sin((float)seam * 1.91F) * 0.18F;
         float width = 0.028F;
         glowVertex(vertices, pose, -halfWidth, y - width, z, 0.0F, 0.0F, 0.62F, 0.31F, 0.04F, alpha * 0.1F);
         glowVertex(vertices, pose, halfWidth, y - width, z, 1.0F, 0.0F, 0.62F, 0.31F, 0.04F, alpha * 0.1F);
         glowVertex(vertices, pose, halfWidth, y + width, z, 1.0F, 1.0F, 0.96F, 0.66F, 0.1F, alpha * 0.42F);
         glowVertex(vertices, pose, -halfWidth, y + width, z, 0.0F, 1.0F, 0.96F, 0.66F, 0.1F, alpha * 0.42F);
      }

   }

   private static void renderWallSideVeins(PoseStack poseStack, MultiBufferSource buffer, float edgeX, int sign, int wingLength, float height, float age, float alpha, int realm) {
      Matrix4f pose = poseStack.last().pose();
      VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucentEmissive(WHITE_TEXTURE));
      float x = edgeX + (float)sign * 0.61F;
      int veins = Math.max(3, wingLength * 2 + realm);

      for(int vein = 0; vein < veins; ++vein) {
         float fraction = (float)vein / (float)Math.max(1, veins - 1);
         float z = Mth.lerp(fraction, -0.42F, (float)(-wingLength) - 0.52F);
         float sway = Mth.sin((float)vein * 2.31F + age * 0.035F) * 0.24F;
         float width = 0.022F + (float)realm * 0.004F;
         glowVertex(vertices, pose, x, 0.1F, z - width, 0.0F, 0.0F, 0.72F, 0.4F, 0.06F, alpha * 0.18F);
         glowVertex(vertices, pose, x, 0.1F, z + width, 1.0F, 0.0F, 0.72F, 0.4F, 0.06F, alpha * 0.18F);
         glowVertex(vertices, pose, x, height * 0.92F, z + sway + width, 1.0F, 1.0F, 1.0F, 0.72F, 0.13F, alpha * 0.72F);
         glowVertex(vertices, pose, x, height * 0.92F, z + sway - width, 0.0F, 1.0F, 1.0F, 0.72F, 0.13F, alpha * 0.72F);
      }

      for(int seam = 1; (float)seam < height; ++seam) {
         float y = (float)seam + Mth.sin((float)seam * 1.73F + (float)sign) * 0.16F;
         float width = 0.028F;
         glowVertex(vertices, pose, x, y - width, -0.42F, 0.0F, 0.0F, 0.62F, 0.31F, 0.04F, alpha * 0.1F);
         glowVertex(vertices, pose, x, y + width, -0.42F, 1.0F, 0.0F, 0.96F, 0.66F, 0.1F, alpha * 0.42F);
         glowVertex(vertices, pose, x, y + width, (float)(-wingLength) - 0.52F, 1.0F, 1.0F, 0.96F, 0.66F, 0.1F, alpha * 0.42F);
         glowVertex(vertices, pose, x, y - width, (float)(-wingLength) - 0.52F, 0.0F, 1.0F, 0.62F, 0.31F, 0.04F, alpha * 0.1F);
      }

   }

   private static void renderWaterSheet(PoseStack poseStack, MultiBufferSource buffer, float halfWidth, float length, float red, float green, float blue, float alpha) {
      Matrix4f pose = poseStack.last().pose();
      VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucentEmissive(WHITE_TEXTURE));
      glowVertex(vertices, pose, -halfWidth, 0.02F, -0.5F, 0.0F, 0.0F, red, green, blue, alpha);
      glowVertex(vertices, pose, halfWidth, 0.02F, -0.5F, 1.0F, 0.0F, red, green, blue, alpha);
      glowVertex(vertices, pose, halfWidth, 0.02F, length, 1.0F, 1.0F, red, green, blue, 0.0F);
      glowVertex(vertices, pose, -halfWidth, 0.02F, length, 0.0F, 1.0F, red, green, blue, 0.0F);
   }

   private static void renderHorizontalBand(PoseStack poseStack, MultiBufferSource buffer, float halfWidth, float z, float y, float red, float green, float blue, float alpha) {
      Matrix4f pose = poseStack.last().pose();
      VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucentEmissive(WHITE_TEXTURE));
      float thickness = 0.11F;
      glowVertex(vertices, pose, -halfWidth, y - thickness, z, 0.0F, 0.0F, red, green, blue, 0.0F);
      glowVertex(vertices, pose, halfWidth, y - thickness, z, 1.0F, 0.0F, red, green, blue, 0.0F);
      glowVertex(vertices, pose, halfWidth, y + thickness, z, 1.0F, 1.0F, red, green, blue, alpha);
      glowVertex(vertices, pose, -halfWidth, y + thickness, z, 0.0F, 1.0F, red, green, blue, alpha);
   }

   private static void renderDisc(PoseStack poseStack, MultiBufferSource buffer, float radius, float y, float red, float green, float blue, float alpha, int segments) {
      renderRing(poseStack, buffer, 0.0F, radius, y, red, green, blue, alpha, segments, 0.0F);
   }

   private static void renderRing(PoseStack poseStack, MultiBufferSource buffer, float innerRadius, float outerRadius, float y, float red, float green, float blue, float alpha, int segments, float rotationDegrees) {
      if (!(outerRadius <= 0.0F) && !(alpha <= 0.001F)) {
         Matrix4f pose = poseStack.last().pose();
         VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucentEmissive(WHITE_TEXTURE));
         float rotation = rotationDegrees * ((float)Math.PI / 180F);

         for(int segment = 0; segment < segments; ++segment) {
            float a0 = rotation + ((float)Math.PI * 2F) * (float)segment / (float)segments;
            float a1 = rotation + ((float)Math.PI * 2F) * (float)(segment + 1) / (float)segments;
            glowVertex(vertices, pose, Mth.cos(a0) * innerRadius, y, Mth.sin(a0) * innerRadius, 0.0F, 1.0F, red, green, blue, alpha * 0.55F);
            glowVertex(vertices, pose, Mth.cos(a1) * innerRadius, y, Mth.sin(a1) * innerRadius, 1.0F, 1.0F, red, green, blue, alpha * 0.55F);
            glowVertex(vertices, pose, Mth.cos(a1) * outerRadius, y, Mth.sin(a1) * outerRadius, 1.0F, 0.0F, red, green, blue, alpha);
            glowVertex(vertices, pose, Mth.cos(a0) * outerRadius, y, Mth.sin(a0) * outerRadius, 0.0F, 0.0F, red, green, blue, alpha);
         }

      }
   }

   private static void renderRadials(PoseStack poseStack, MultiBufferSource buffer, float innerRadius, float outerRadius, float y, float halfWidth, int count, float rotationDegrees, float red, float green, float blue, float alpha) {
      Matrix4f pose = poseStack.last().pose();
      VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucentEmissive(WHITE_TEXTURE));
      float rotation = rotationDegrees * ((float)Math.PI / 180F);

      for(int ray = 0; ray < count; ++ray) {
         float angle = rotation + ((float)Math.PI * 2F) * (float)ray / (float)count;
         float dx = Mth.cos(angle);
         float dz = Mth.sin(angle);
         float sx = -dz * halfWidth;
         float sz = dx * halfWidth;
         glowVertex(vertices, pose, dx * innerRadius + sx, y, dz * innerRadius + sz, 0.0F, 0.0F, red, green, blue, alpha);
         glowVertex(vertices, pose, dx * innerRadius - sx, y, dz * innerRadius - sz, 1.0F, 0.0F, red, green, blue, alpha);
         glowVertex(vertices, pose, dx * outerRadius - sx, y, dz * outerRadius - sz, 1.0F, 1.0F, red, green, blue, 0.0F);
         glowVertex(vertices, pose, dx * outerRadius + sx, y, dz * outerRadius + sz, 0.0F, 1.0F, red, green, blue, 0.0F);
      }

   }

   private static void renderCylinder(PoseStack poseStack, MultiBufferSource buffer, float radius, float height, float red, float green, float blue, float alpha, int segments, float phase) {
      if (!(radius <= 0.0F) && !(height <= 0.0F) && !(alpha <= 0.001F)) {
         Matrix4f pose = poseStack.last().pose();
         VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucentEmissive(WHITE_TEXTURE));

         for(int segment = 0; segment < segments; ++segment) {
            float a0 = phase + ((float)Math.PI * 2F) * (float)segment / (float)segments;
            float a1 = phase + ((float)Math.PI * 2F) * (float)(segment + 1) / (float)segments;
            float x0 = Mth.cos(a0) * radius;
            float z0 = Mth.sin(a0) * radius;
            float x1 = Mth.cos(a1) * radius;
            float z1 = Mth.sin(a1) * radius;
            glowVertex(vertices, pose, x0, 0.0F, z0, 0.0F, 1.0F, red, green, blue, alpha * 0.1F);
            glowVertex(vertices, pose, x1, 0.0F, z1, 1.0F, 1.0F, red, green, blue, alpha * 0.1F);
            glowVertex(vertices, pose, x1, height, z1, 1.0F, 0.0F, red, green, blue, alpha);
            glowVertex(vertices, pose, x0, height, z0, 0.0F, 0.0F, red, green, blue, alpha);
         }

      }
   }

   private static void glowVertex(VertexConsumer vertices, Matrix4f pose, float x, float y, float z, float u, float v, float red, float green, float blue, float alpha) {
      vertices.addVertex(pose, x, y, z).setColor(red, green, blue, alpha).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0.0F, 1.0F, 0.0F);
   }

   public ResourceLocation getTextureLocation(MethodTechniqueAreaEntity entity) {
      return TextureAtlas.LOCATION_BLOCKS;
   }
}
