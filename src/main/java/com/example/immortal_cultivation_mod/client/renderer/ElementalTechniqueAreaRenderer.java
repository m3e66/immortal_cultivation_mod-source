package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.entity.ElementalTechniqueAreaEntity;
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

public final class ElementalTechniqueAreaRenderer extends EntityRenderer<ElementalTechniqueAreaEntity> {
   private static final ResourceLocation WHITE_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");

   public ElementalTechniqueAreaRenderer(EntityRendererProvider.Context context) {
      super(context);
      this.shadowRadius = 0.0F;
   }

   public void render(ElementalTechniqueAreaEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      try (var support = com.example.immortal_cultivation_mod.client.SpellGlowBuffers.support(com.example.immortal_cultivation_mod.client.SpellRibbonRenderer.spell(entity) != null)) {
      renderTechniqueGeometry(entity, partialTick, poseStack, buffer);
      ItemStack model = entity.modelStack();
      if (model.isEmpty()) {
         super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
      } else {
         poseStack.pushPose();
         poseStack.translate((double)0.0F, entity.modelYOffset(partialTick), (double)0.0F);
         poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
         if (entity.kind() == ElementalTechniqueAreaEntity.Kind.WANTENG_JIAOSHA) {
            poseStack.mulPose(Axis.YP.rotationDegrees(((float)entity.tickCount + partialTick) * 1.2F));
         }

         float detailScale = com.example.immortal_cultivation_mod.client.SpellRibbonRenderer.spell(entity) != null ? .38F : .72F;
         float scale = entity.modelScale();
         float growth = entity.modelGrowth(partialTick);
         float modelWidth = entity.fullyCharged() && entity.kind() == ElementalTechniqueAreaEntity.Kind.DIYAN_QIANG ? 1.3F : 1.0F;
         poseStack.scale(scale * growth * modelWidth * detailScale, scale * growth * detailScale, scale * growth * modelWidth * detailScale);
         Minecraft.getInstance().getItemRenderer().renderStatic(model, ItemDisplayContext.FIXED, 15728880, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
         poseStack.popPose();
         super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
      }
   }
   }

   private static void renderTechniqueGeometry(ElementalTechniqueAreaEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffer) {
      float age = (float)entity.tickCount + partialTick;
      float horizontalScale = 1.0F;
      float verticalScale = 1.0F;
      if (entity.fullyCharged()) {
         float var10000;
         switch (entity.kind()) {
            case JINGJI_TUSHENG:
            case WANTENG_JIAOSHA:
               var10000 = 1.2F;
               break;
            case CHIYAN_LONGJUAN:
            case BINGZHUI_YU:
               var10000 = 1.25F;
               break;
            case SHANBENG_YIN:
               var10000 = 1.3333334F;
               break;
            case CANGLANG_CHONG:
               var10000 = 1.2857143F;
               break;
            case WULEI_HONGDING:
            case TIANGUANG_GUANSHA:
               var10000 = 1.5F;
               break;
            case HANYU_FENGDI:
               var10000 = 1.2857143F;
               break;
            case TIANFENG_ZHUI:
               var10000 = 1.4F;
               break;
            default:
               var10000 = 1.0F;
         }

         horizontalScale = var10000;
         if (entity.kind() == ElementalTechniqueAreaEntity.Kind.CHIYAN_LONGJUAN) {
            verticalScale = 1.25F;
         }
      }

      poseStack.pushPose();
      poseStack.scale(horizontalScale, verticalScale, horizontalScale);
      switch (entity.kind()) {
         case JINGJI_TUSHENG -> renderThornTelegraph(age, poseStack, buffer);
         case WANTENG_JIAOSHA -> renderVineDomain(age, poseStack, buffer);
         case CHIYAN_LONGJUAN -> renderFireTornado(age, poseStack, buffer);
         case BINGZHUI_YU -> renderIceRainCloud(age, poseStack, buffer);
         case SHANBENG_YIN -> renderMountainSeal(entity, age, poseStack, buffer);
         case CANGLANG_CHONG -> renderWavePressure(entity, age, poseStack, buffer);
         case WULEI_HONGDING -> renderLightningMark(age, poseStack, buffer);
         case TIANGUANG_GUANSHA -> renderHeavenlyBeam(entity, age, poseStack, buffer);
         case HANYU_FENGDI -> renderFrozenDomain(age, poseStack, buffer);
         case TIANFENG_ZHUI -> renderWindPressure(age, poseStack, buffer);
         case DIYAN_QIANG -> renderEarthSpearTelegraph(age, poseStack, buffer);
         case JIUXIAO_FENGLEI_YU -> renderFengleiStorm(entity, age, poseStack, buffer);
      }

      poseStack.popPose();
   }

   private static void renderThornTelegraph(float age, PoseStack poseStack, MultiBufferSource buffer) {
      float warning = 1.0F - Mth.clamp(age / 7.0F, 0.0F, 1.0F);
      if (warning > 0.0F) {
         renderRing(poseStack, buffer, 1.18F, 1.28F, 0.025F, 0.16F, 0.78F, 0.23F, 0.62F * warning, 40, age * 2.0F);
         renderRadials(poseStack, buffer, 0.18F, 1.18F, 0.026F, 0.035F, 10, age * -1.1F, 0.08F, 0.55F, 0.12F, 0.46F * warning);
      }

      if (age >= 6.0F && age < 14.0F) {
         float pulse = Mth.clamp((age - 6.0F) / 8.0F, 0.0F, 1.0F);
         renderRing(poseStack, buffer, 0.35F + pulse * 1.8F, 0.44F + pulse * 1.8F, 0.045F, 0.42F, 1.0F, 0.38F, 0.4F * (1.0F - pulse), 40, 0.0F);
      }

   }

   private static void renderVineDomain(float age, PoseStack poseStack, MultiBufferSource buffer) {
      float appear = Mth.clamp(age / 10.0F, 0.0F, 1.0F);
      renderDisc(poseStack, buffer, 4.95F, 0.016F, 0.04F, 0.2F, 0.035F, 0.1F * appear, 64);
      renderRing(poseStack, buffer, 4.78F, 5.0F, 0.022F, 0.18F, 0.86F, 0.2F, 0.62F * appear, 64, age * 0.7F);
      renderRing(poseStack, buffer, 2.35F, 2.46F, 0.024F, 0.08F, 0.62F, 0.12F, 0.38F * appear, 56, -age * 1.1F);
      renderRadials(poseStack, buffer, 0.35F, 4.7F, 0.025F, 0.045F, 14, age * 0.28F, 0.1F, 0.62F, 0.1F, 0.35F * appear);
      if (age >= 9.0F) {
         float active = Mth.clamp((age - 9.0F) / 8.0F, 0.0F, 1.0F);

         for(int arm = 0; arm < 6; ++arm) {
            renderHelix(poseStack, buffer, 0.95F + (float)arm * 0.45F, 2.2F + (float)(arm % 3) * 0.65F, 1.35F, age * (1.8F + (float)arm * 0.08F) + (float)arm * 60.0F, 0.045F, 16, 0.1F, 0.74F, 0.16F, 0.28F * active);
         }
      }

   }

   private static void renderFireTornado(float age, PoseStack poseStack, MultiBufferSource buffer) {
      float growth = Mth.clamp(age / 8.0F, 0.0F, 1.0F);
      float pulse = 0.86F + Mth.sin(age * 0.45F) * 0.14F;
      renderDisc(poseStack, buffer, 5.9F * growth, 0.012F, 0.42F, 0.025F, 0.008F, 0.055F * growth, 56);
      renderRing(poseStack, buffer, 5.65F * growth, 6.0F * growth, 0.022F, 1.0F, 0.12F, 0.015F, 0.42F * growth * pulse, 64, -age * 2.8F);
      renderRadials(poseStack, buffer, 1.55F * growth, 5.7F * growth, 0.026F, 0.075F, 18, age * 2.1F, 1.0F, 0.16F, 0.018F, 0.4F * growth);
      renderRing(poseStack, buffer, 1.55F * growth, 1.82F * growth, 0.03F, 1.0F, 0.1F, 0.015F, 0.65F * growth, 48, age * 4.8F);
      renderCylinder(poseStack, buffer, 0.58F * growth, 7.9F * growth, 0.18F, 0.0F, 0.0F, 0.32F * growth * pulse, 28, age * 0.02F);

      for(int arm = 0; arm < 6; ++arm) {
         renderTaperedHelix(poseStack, buffer, 0.72F * growth, 3.15F * growth, 8.0F * growth, 3.0F + (float)arm * 0.35F, age * (5.1F + (float)arm * 0.18F) + (float)arm * 60.0F, 0.13F, 38, arm % 2 == 0 ? 1.0F : 0.94F, arm % 2 == 0 ? 0.2F : 0.06F, 0.012F, 0.48F * growth);
      }

      for(int band = 0; band < 4; ++band) {
         float y = (1.15F + (float)band * 1.85F) * growth;
         float radius = (1.05F + (float)band * 0.58F) * growth;
         renderRingAtHeight(poseStack, buffer, radius - 0.1F, radius + 0.12F, y, 1.0F, band % 2 == 0 ? 0.23F : 0.08F, 0.012F, 0.31F * growth * pulse, 44, age * (4.0F + (float)band * 0.55F) + (float)band * 37.0F);
      }

      renderCylinder(poseStack, buffer, 3.0F * growth, 7.8F * growth, 1.0F, 0.14F, 0.015F, 0.045F * growth, 32, -age * 0.015F);
   }

   private static void renderEarthSpearTelegraph(float age, PoseStack poseStack, MultiBufferSource buffer) {
      if (age < 7.0F) {
         float pulse = 0.45F + 0.3F * Mth.sin(age * 1.8F);
         renderRing(poseStack, buffer, 1.05F, 1.16F, 0.024F, 0.76F, 0.43F, 0.08F, pulse, 38, -age * 2.0F);
         renderRadials(poseStack, buffer, 0.1F, 1.02F, 0.026F, 0.035F, 9, age * 0.8F, 0.52F, 0.25F, 0.035F, 0.55F);
      } else if (age < 14.0F) {
         float progress = (age - 7.0F) / 7.0F;
         renderRing(poseStack, buffer, 0.25F + progress * 1.5F, 0.34F + progress * 1.5F, 0.045F, 0.85F, 0.51F, 0.1F, 0.4F * (1.0F - progress), 36, 0.0F);
      }

   }

   private static void renderMountainSeal(ElementalTechniqueAreaEntity entity, float age, PoseStack poseStack, MultiBufferSource buffer) {
      float impactTick = entity.fullyCharged() ? 40.0F : 30.0F;
      float warning = Mth.clamp(age / Math.max(1.0F, impactTick - 12.0F), 0.0F, 1.0F);
      renderDisc(poseStack, buffer, 5.9F, 0.014F, 0.11F, 0.065F, 0.018F, 0.12F * warning, 72);
      renderRing(poseStack, buffer, 5.72F, 6.0F, 0.022F, 0.82F, 0.48F, 0.08F, 0.72F * warning, 72, age * 0.55F);
      renderRing(poseStack, buffer, 4.35F, 4.48F, 0.024F, 1.0F, 0.67F, 0.16F, 0.42F * warning, 64, -age * 0.85F);
      renderRadials(poseStack, buffer, 2.5F, 5.55F, 0.025F, 0.065F, 12, age * 0.18F, 0.73F, 0.42F, 0.08F, 0.35F * warning);
      if (age < impactTick) {
         renderRingAtHeight(poseStack, buffer, 2.7F, 2.9F, 9.6F, 0.9F, 0.55F, 0.12F, 0.42F, 52, -age * 1.3F);
      } else if (age < impactTick + 12.0F) {
         float impact = (age - impactTick) / 12.0F;
         renderRing(poseStack, buffer, 0.9F + impact * 6.8F, 1.2F + impact * 6.8F, 0.055F, 0.78F, 0.48F, 0.12F, 0.68F * (1.0F - impact), 72, 0.0F);
      }

   }

   private static void renderWavePressure(ElementalTechniqueAreaEntity entity, float age, PoseStack poseStack, MultiBufferSource buffer) {
      float pulse = 0.55F + 0.1F * Mth.sin(age * 0.9F);
      renderRing(poseStack, buffer, 2.6F, 3.35F, 0.04F, 0.1F, 0.65F, 1.0F, 0.12F * pulse, 48, age * 1.5F);
      poseStack.pushPose();
      poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
      poseStack.translate((double)0.0F, 0.15, (double)-1.25F);
      renderDisc(poseStack, buffer, 3.2F, 0.0F, 0.08F, 0.48F, 0.92F, 0.075F, 48);
      poseStack.translate((double)0.0F, 0.018, -0.72);
      renderRing(poseStack, buffer, 2.25F, 3.0F, 0.0F, 0.62F, 0.9F, 1.0F, 0.16F * pulse, 48, -age * 2.0F);
      poseStack.popPose();
   }

   private static void renderLightningMark(float age, PoseStack poseStack, MultiBufferSource buffer) {
      int completedStrikes = age >= 60.0F ? 5 : (age >= 49.0F ? 4 : (age >= 38.0F ? 3 : (age >= 27.0F ? 2 : (age >= 16.0F ? 1 : 0))));
      float charge = age < 16.0F ? 0.45F + age / 32.0F : Math.min(1.0F, 0.7F + (float)completedStrikes * 0.06F);
      renderRing(poseStack, buffer, 1.72F, 1.92F, 0.026F, 0.48F, 0.28F, 1.0F, charge, 52, age * 2.2F);
      renderRing(poseStack, buffer, 0.92F, 1.08F, 0.028F, 0.65F, 0.72F, 1.0F, charge * 0.72F, 40, -age * 3.0F);
      renderRadials(poseStack, buffer, 0.18F, 1.58F, 0.03F, 0.045F, 6, age * 1.4F, 0.55F, 0.38F, 1.0F, 0.48F);
      int[] strikes = new int[]{16, 27, 38, 49, 60};
      float strikeFade = 0.0F;

      for(int strike : strikes) {
         float since = age - (float)strike;
         if (since >= 0.0F && since < 3.5F) {
            strikeFade = Math.max(strikeFade, 1.0F - since / 3.5F);
         }
      }

      if (age < 16.0F) {
         renderCylinder(poseStack, buffer, 0.035F, 11.0F, 0.6F, 0.78F, 1.0F, 0.6F, 10, 0.0F);
      }

      if (strikeFade > 0.0F) {
         renderCylinder(poseStack, buffer, 0.26F, 13.0F, 0.82F, 0.94F, 1.0F, 0.92F * strikeFade, 12, age * 0.1F);
         renderCylinder(poseStack, buffer, 0.58F, 12.5F, 0.32F, 0.2F, 1.0F, 0.28F * strikeFade, 16, -age * 0.08F);
      }

   }

   private static void renderIceRainCloud(float age, PoseStack poseStack, MultiBufferSource buffer) {
      float appear = Mth.clamp(age / 6.0F, 0.0F, 1.0F);
      renderRingAtHeight(poseStack, buffer, 3.55F, 4.05F, 6.1F, 0.58F, 0.9F, 1.0F, 0.34F * appear, 56, age * 0.7F);
      renderDiscAtHeight(poseStack, buffer, 3.75F, 6.08F, 0.45F, 0.78F, 1.0F, 0.075F * appear, 56);
      renderRing(poseStack, buffer, 3.75F, 4.05F, 0.025F, 0.52F, 0.88F, 1.0F, 0.28F * appear, 56, -age * 0.5F);
   }

   private static void renderFrozenDomain(float age, PoseStack poseStack, MultiBufferSource buffer) {
      float appear = Mth.clamp(age / 8.0F, 0.0F, 1.0F);
      renderDisc(poseStack, buffer, 6.95F, 0.014F, 0.32F, 0.74F, 1.0F, 0.105F * appear, 72);
      renderRing(poseStack, buffer, 6.78F, 7.0F, 0.022F, 0.55F, 0.91F, 1.0F, 0.62F * appear, 72, -age * 0.45F);
      renderRing(poseStack, buffer, 3.4F, 3.54F, 0.024F, 0.7F, 0.96F, 1.0F, 0.33F * appear, 60, age * 0.7F);
      renderRadials(poseStack, buffer, 0.2F, 6.55F, 0.025F, 0.035F, 18, age * 0.09F, 0.48F, 0.82F, 1.0F, 0.24F * appear);
   }

   private static void renderHeavenlyBeam(ElementalTechniqueAreaEntity entity, float age, PoseStack poseStack, MultiBufferSource buffer) {
      float warningTime = entity.fullyCharged() ? 30.0F : 20.0F;
      if (age < warningTime) {
         float charge = Mth.clamp(age / warningTime, 0.0F, 1.0F);
         renderRing(poseStack, buffer, 1.72F, 1.92F, 0.026F, 1.0F, 0.82F, 0.28F, 0.74F * charge, 56, age * 1.8F);
         renderRing(poseStack, buffer, 0.94F, 1.08F, 0.028F, 1.0F, 0.96F, 0.7F, 0.58F * charge, 44, -age * 2.6F);
         renderCylinder(poseStack, buffer, 0.032F, 24.0F, 1.0F, 0.96F, 0.72F, 0.68F * charge, 10, 0.0F);
         renderRingAtHeight(poseStack, buffer, 3.7F, 4.1F, 17.0F, 1.0F, 0.86F, 0.35F, 0.52F * charge, 64, -age * 1.1F);
         renderRingAtHeight(poseStack, buffer, 2.4F, 2.62F, 17.05F, 1.0F, 0.98F, 0.74F, 0.42F * charge, 52, age * 1.8F);
      } else {
         float fade = 1.0F - Mth.clamp((age - warningTime - 4.0F) / 4.0F, 0.0F, 1.0F);
         renderCylinder(poseStack, buffer, 1.42F, 32.0F, 1.0F, 1.0F, 0.92F, 0.76F * fade, 28, 0.0F);
         renderCylinder(poseStack, buffer, 1.92F, 31.5F, 1.0F, 0.74F, 0.16F, 0.24F * fade, 32, -age * 0.02F);
         renderRing(poseStack, buffer, 1.65F, 2.05F, 0.04F, 1.0F, 0.84F, 0.3F, 0.72F * fade, 60, age * 2.0F);
         renderRadials(poseStack, buffer, 0.25F, 1.55F, 0.042F, 0.05F, 12, -age * 1.3F, 1.0F, 0.93F, 0.58F, 0.48F * fade);
      }
   }

   private static void renderWindPressure(float age, PoseStack poseStack, MultiBufferSource buffer) {
      if(age>=44)return;
      float warning = Mth.clamp(age / 18.0F, 0.0F, 1.0F);
      renderRing(poseStack, buffer, 4.75F, 5.0F, 0.022F, 0.56F, 0.94F, 0.72F, 0.5F * warning, 64, age * 0.85F);
      if (age < 20.0F) {
         for(int ring = 0; ring < 3; ++ring) {
            float progress = age / 20.0F;
            float radius = 3.8F - (float)ring * 0.72F - progress * 0.7F;
            renderRingAtHeight(poseStack, buffer, radius - 0.1F, radius + 0.1F, 6.0F + (float)ring * 2.0F, 0.72F, 1.0F, 0.86F, 0.3F + progress * 0.28F, 56, age * (1.7F + (float)ring * 0.4F) * (ring % 2 == 0 ? 1.0F : -1.0F));
         }
      } else {
         float fade = 1.0F - Mth.clamp((age - 20.0F) / 16.0F, 0.0F, 1.0F);
         renderCylinder(poseStack, buffer, 4.55F, 11.0F, 0.72F, 1.0F, 0.88F, 0.1F * fade, 40, age * 0.02F);
         renderRing(poseStack, buffer, 0.7F + (age - 20.0F) * 0.42F, 1.0F + (age - 20.0F) * 0.42F, 0.05F, 0.7F, 1.0F, 0.84F, 0.62F * fade, 64, 0.0F);
      }

   }

   private static void renderFengleiStorm(ElementalTechniqueAreaEntity entity, float age, PoseStack poseStack, MultiBufferSource buffer) {
      boolean harmony = entity.momentumSnapshot() >= 100.0F;
      float radius = harmony ? 12.0F : 10.0F;
      float appear = Mth.clamp(age / 9.0F, 0.0F, 1.0F);
      float ending = age < 145.0F ? 1.0F : 1.0F - Mth.clamp((age - 145.0F) / 15.0F, 0.0F, 1.0F);
      float intensity = appear * ending;
      renderDisc(poseStack, buffer, radius, 0.012F, 0.015F, 0.09F, 0.13F, 0.11F * intensity, 96);
      if (age < 12.0F) {
         float expansion = Mth.clamp(age / 12.0F, 0.0F, 1.0F);
         renderRing(poseStack, buffer, Math.max(0.0F, expansion * radius - 0.48F), expansion * radius, 0.052F, 0.36F, 0.94F, 1.0F, 0.86F * (1.0F - expansion * 0.45F), 96, age * 2.6F);
      }

      renderRing(poseStack, buffer, radius * 0.91F, radius, 0.025F, 0.1F, 0.88F, 1.0F, 0.58F * intensity, 96, age * (harmony ? 1.8F : 1.1F));
      renderRing(poseStack, buffer, radius * 0.46F, radius * 0.5F, 0.028F, 0.18F, 0.66F, 1.0F, 0.31F * intensity, 72, -age * 1.7F);
      renderRadials(poseStack, buffer, 0.85F, radius * 0.88F, 0.03F, 0.045F, harmony ? 24 : 18, -age * 1.25F, 0.13F, 0.76F, 1.0F, 0.32F * intensity);
      renderHelix(poseStack, buffer, radius * 0.78F, 5.0F, 1.4F, age * 2.8F, 0.075F, 84, 0.18F, 0.9F, 1.0F, 0.34F * intensity);
      renderHelix(poseStack, buffer, radius * 0.6F, 6.6F, -1.8F, -age * 3.7F, 0.065F, 92, 0.28F, 0.72F, 1.0F, 0.28F * intensity);
      renderHelix(poseStack, buffer, radius * 0.38F, 7.4F, 2.2F, age * 4.5F, 0.052F, 92, harmony ? 0.58F : 0.24F, 0.88F, 1.0F, (harmony ? 0.43F : 0.24F) * intensity);
      float cloudSpin = age * (age >= 145.0F ? 4.5F : 0.75F);
      renderRingAtHeight(poseStack, buffer, 2.15F, radius * 0.88F, 7.25F, 0.035F, 0.07F, 0.16F, 0.42F * intensity, 96, cloudSpin);
      renderRingAtHeight(poseStack, buffer, 2.75F, radius, 8.0F, 0.055F, 0.1F, 0.22F, 0.34F * intensity, 96, -cloudSpin * 0.72F);
      renderRingAtHeight(poseStack, buffer, 1.65F, radius * 0.73F, 8.55F, 0.14F, 0.28F, 0.42F, 0.22F * intensity, 80, cloudSpin * 1.18F);
      int interval = harmony ? 10 : 20;
      int strikeAge = entity.tickCount % interval;
      if (strikeAge <= 3 && age < 145.0F) {
         int strikeIndex = entity.tickCount / interval;
         float angle = (float)strikeIndex * 2.39996F;
         float strikeRadius = radius * (0.28F + (float)(strikeIndex % 4) * 0.14F);
         poseStack.pushPose();
         poseStack.translate(Mth.cos(angle) * strikeRadius, 0.04F, Mth.sin(angle) * strikeRadius);
         float flash = 1.0F - (float)strikeAge / 4.0F;
         renderRing(poseStack, buffer, 0.35F, 1.15F, 0.025F, 0.38F, 0.82F, 1.0F, 0.56F * flash, 42, age * 5.0F);
         renderCylinder(poseStack, buffer, 0.16F + flash * 0.12F, 8.1F, 0.72F, 0.92F, 1.0F, 0.84F * flash, 14, age * 0.3F);
         if (harmony) {
            renderRing(poseStack, buffer, 0.4F + (float)(3 - strikeAge) * 0.55F, 0.65F + (float)(3 - strikeAge) * 0.7F, 0.035F, 0.18F, 0.88F, 1.0F, 0.42F * flash, 52, 0.0F);
         }

         poseStack.popPose();
      }

      if (age >= 149.0F) {
         float collapse = Mth.clamp((age - 149.0F) / 5.0F, 0.0F, 1.0F);
         float fade = 1.0F - Mth.clamp((age - 155.0F) / 5.0F, 0.0F, 1.0F);
         renderCylinder(poseStack, buffer, 0.35F + collapse * 0.85F, 9.2F, 0.72F, 0.94F, 1.0F, 0.82F * fade, 20, -age * 0.5F);
         renderRing(poseStack, buffer, 0.8F + collapse * radius * 0.82F, 1.1F + collapse * radius, 0.055F, 0.22F, 0.92F, 1.0F, 0.7F * fade, 96, 0.0F);
      }

   }

   private static void renderDisc(PoseStack poseStack, MultiBufferSource buffer, float radius, float y, float red, float green, float blue, float alpha, int segments) {
      renderRing(poseStack, buffer, 0.0F, radius, y, red, green, blue, alpha, segments, 0.0F);
   }

   private static void renderDiscAtHeight(PoseStack poseStack, MultiBufferSource buffer, float radius, float y, float red, float green, float blue, float alpha, int segments) {
      renderDisc(poseStack, buffer, radius, y, red, green, blue, alpha, segments);
   }

   private static void renderRingAtHeight(PoseStack poseStack, MultiBufferSource buffer, float innerRadius, float outerRadius, float y, float red, float green, float blue, float alpha, int segments, float rotationDegrees) {
      renderRing(poseStack, buffer, innerRadius, outerRadius, y, red, green, blue, alpha, segments, rotationDegrees);
   }

   private static void renderRing(PoseStack poseStack, MultiBufferSource buffer, float innerRadius, float outerRadius, float y, float red, float green, float blue, float alpha, int segments, float rotationDegrees) {
      if (!(outerRadius <= 0.0F) && !(alpha <= 0.001F)) {
         Matrix4f pose = poseStack.last().pose();
         VertexConsumer vertices = com.example.immortal_cultivation_mod.client.SpellGlowBuffers.get(buffer);
         float phase = rotationDegrees * ((float)Math.PI / 180F);

         for(int segment = 0; segment < segments; ++segment) {
            float a0 = phase + ((float)Math.PI * 2F) * (float)segment / (float)segments;
            float a1 = phase + ((float)Math.PI * 2F) * (float)(segment + 1) / (float)segments;
            glowVertex(vertices, pose, Mth.cos(a0) * innerRadius, y, Mth.sin(a0) * innerRadius, 0.0F, 0.0F, red, green, blue, alpha);
            glowVertex(vertices, pose, Mth.cos(a1) * innerRadius, y, Mth.sin(a1) * innerRadius, 0.0F, 1.0F, red, green, blue, alpha);
            glowVertex(vertices, pose, Mth.cos(a1) * outerRadius, y, Mth.sin(a1) * outerRadius, 1.0F, 1.0F, red, green, blue, alpha);
            glowVertex(vertices, pose, Mth.cos(a0) * outerRadius, y, Mth.sin(a0) * outerRadius, 1.0F, 0.0F, red, green, blue, alpha);
         }

      }
   }

   private static void renderRadials(PoseStack poseStack, MultiBufferSource buffer, float innerRadius, float outerRadius, float y, float halfWidth, int count, float rotationDegrees, float red, float green, float blue, float alpha) {
      Matrix4f pose = poseStack.last().pose();
      VertexConsumer vertices = com.example.immortal_cultivation_mod.client.SpellGlowBuffers.get(buffer);
      float phase = rotationDegrees * ((float)Math.PI / 180F);

      for(int i = 0; i < count; ++i) {
         float angle = phase + ((float)Math.PI * 2F) * (float)i / (float)count;
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
         VertexConsumer vertices = com.example.immortal_cultivation_mod.client.SpellGlowBuffers.get(buffer);

         for(int segment = 0; segment < segments; ++segment) {
            float a0 = phase + ((float)Math.PI * 2F) * (float)segment / (float)segments;
            float a1 = phase + ((float)Math.PI * 2F) * (float)(segment + 1) / (float)segments;
            float x0 = Mth.cos(a0) * radius;
            float z0 = Mth.sin(a0) * radius;
            float x1 = Mth.cos(a1) * radius;
            float z1 = Mth.sin(a1) * radius;
            glowVertex(vertices, pose, x0, 0.0F, z0, 0.0F, 1.0F, red, green, blue, alpha * 0.15F);
            glowVertex(vertices, pose, x1, 0.0F, z1, 1.0F, 1.0F, red, green, blue, alpha * 0.15F);
            glowVertex(vertices, pose, x1, height, z1, 1.0F, 0.0F, red, green, blue, alpha);
            glowVertex(vertices, pose, x0, height, z0, 0.0F, 0.0F, red, green, blue, alpha);
         }

      }
   }

   private static void renderHelix(PoseStack poseStack, MultiBufferSource buffer, float radius, float height, float turns, float phaseDegrees, float halfWidth, int segments, float red, float green, float blue, float alpha) {
      renderTaperedHelix(poseStack, buffer, radius, radius, height, turns, phaseDegrees, halfWidth, segments, red, green, blue, alpha);
   }

   private static void renderTaperedHelix(PoseStack poseStack, MultiBufferSource buffer, float baseRadius, float topRadius, float height, float turns, float phaseDegrees, float halfWidth, int segments, float red, float green, float blue, float alpha) {
      Matrix4f pose = poseStack.last().pose();
      VertexConsumer vertices = com.example.immortal_cultivation_mod.client.SpellGlowBuffers.get(buffer);
      float phase = phaseDegrees * ((float)Math.PI / 180F);

      for(int segment = 0; segment < segments; ++segment) {
         float t0 = (float)segment / (float)segments;
         float t1 = (float)(segment + 1) / (float)segments;
         float a0 = phase + ((float)Math.PI * 2F) * turns * t0;
         float a1 = phase + ((float)Math.PI * 2F) * turns * t1;
         float r0 = Mth.lerp(t0, baseRadius, topRadius);
         float r1 = Mth.lerp(t1, baseRadius, topRadius);
         float x0 = Mth.cos(a0) * r0;
         float z0 = Mth.sin(a0) * r0;
         float x1 = Mth.cos(a1) * r1;
         float z1 = Mth.sin(a1) * r1;
         float sx0 = -Mth.sin(a0) * halfWidth;
         float sz0 = Mth.cos(a0) * halfWidth;
         float sx1 = -Mth.sin(a1) * halfWidth;
         float sz1 = Mth.cos(a1) * halfWidth;
         glowVertex(vertices, pose, x0 + sx0, t0 * height, z0 + sz0, 0.0F, t0, red, green, blue, alpha);
         glowVertex(vertices, pose, x0 - sx0, t0 * height, z0 - sz0, 1.0F, t0, red, green, blue, alpha);
         glowVertex(vertices, pose, x1 - sx1, t1 * height, z1 - sz1, 1.0F, t1, red, green, blue, alpha * 0.65F);
         glowVertex(vertices, pose, x1 + sx1, t1 * height, z1 + sz1, 0.0F, t1, red, green, blue, alpha * 0.65F);
      }

   }

   private static void glowVertex(VertexConsumer vertices, Matrix4f pose, float x, float y, float z, float u, float v, float red, float green, float blue, float alpha) {
      vertices.addVertex(pose, x, y, z).setColor(red, green, blue, alpha).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(0.0F, 1.0F, 0.0F);
   }

   public ResourceLocation getTextureLocation(ElementalTechniqueAreaEntity entity) {
      return TextureAtlas.LOCATION_BLOCKS;
   }
}
