package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.entity.SolarTechniqueModelEntity;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class SolarTechniqueModelRenderer extends EntityRenderer<SolarTechniqueModelEntity> {
   public SolarTechniqueModelRenderer(EntityRendererProvider.Context context) {
      super(context);
      this.shadowRadius = 0.0F;
   }

   public void render(SolarTechniqueModelEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      Minecraft minecraft = Minecraft.getInstance();
      if (entity.mode() == 1 && minecraft.player != null && minecraft.options.getCameraType().isFirstPerson()) {
         Optional var10000 = entity.ownerUuid();
         UUID var10001 = minecraft.player.getUUID();
         Objects.requireNonNull(var10001);
         if (var10000.filter(var10001::equals).isPresent()) {
            return;
         }
      }

      float age = (float)entity.tickCount + partialTick;
      if (entity.mode() == 0) {
         float pulse = 0.62F * (0.96F + 0.06F * Mth.sin(age * 0.55F));
         poseStack.pushPose();
         poseStack.mulPose(Axis.YP.rotationDegrees(age * 6.5F));
         poseStack.mulPose(Axis.XP.rotationDegrees(20.0F + age * 1.8F));
         poseStack.scale(pulse, pulse, pulse);
         renderModel(ModItems.SPELL_MODEL_SOLAR_ORB.toStack(), entity, poseStack, buffer);
         poseStack.popPose();
         poseStack.pushPose();
         poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
         poseStack.mulPose(Axis.ZP.rotationDegrees(-age * 8.0F));
         poseStack.scale(0.5F, 0.5F, 0.5F);
         renderModel(ModItems.SPELL_MODEL_SOLAR_HALO.toStack(), entity, poseStack, buffer);
         poseStack.popPose();
      } else {
         float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
         float pulse = 0.72F * (0.98F + 0.035F * Mth.sin(age * 0.28F));
         poseStack.pushPose();
         poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
         poseStack.mulPose(Axis.ZP.rotationDegrees(age * 1.35F));
         poseStack.scale(pulse, pulse, pulse);
         renderModel(ModItems.SPELL_MODEL_SOLAR_HALO.toStack(), entity, poseStack, buffer);
         poseStack.popPose();
      }

      super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
   }

   private static void renderModel(ItemStack stack, SolarTechniqueModelEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
      Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, 15728880, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
   }

   public ResourceLocation getTextureLocation(SolarTechniqueModelEntity entity) {
      return TextureAtlas.LOCATION_BLOCKS;
   }
}
