package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.entity.XuanyanShieldPlateEntity;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;

public final class XuanyanShieldPlateRenderer extends EntityRenderer<XuanyanShieldPlateEntity> {
   public XuanyanShieldPlateRenderer(EntityRendererProvider.Context context) {
      super(context);
      this.shadowRadius = 0.0F;
   }

   public void render(XuanyanShieldPlateEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      poseStack.pushPose();
      poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw));
      poseStack.mulPose(Axis.XP.rotationDegrees(-7.0F));
      float var10000;
      switch (entity.plateIndex()) {
         case 0 -> var10000 = -5.0F;
         case 1 -> var10000 = 3.0F;
         default -> var10000 = 6.0F;
      }

      float shardLean = var10000;
      poseStack.mulPose(Axis.ZP.rotationDegrees(shardLean));
      poseStack.scale(1.22F, 1.22F, 1.22F);
      Minecraft.getInstance().getItemRenderer().renderStatic(ModItems.SPELL_MODEL_XUANYAN_PLATE.toStack(), ItemDisplayContext.FIXED, 15728880, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
      poseStack.popPose();
      super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
   }

   public ResourceLocation getTextureLocation(XuanyanShieldPlateEntity entity) {
      return TextureAtlas.LOCATION_BLOCKS;
   }
}
