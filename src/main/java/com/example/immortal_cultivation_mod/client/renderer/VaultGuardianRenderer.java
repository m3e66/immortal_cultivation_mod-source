package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.client.model.VaultGuardianModel;
import com.example.immortal_cultivation_mod.entity.VaultGuardianEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class VaultGuardianRenderer extends BeastGeoRenderer<VaultGuardianEntity> {
   private static final float MODEL_SCALE = 2.301F;

   public VaultGuardianRenderer(EntityRendererProvider.Context context) {
      super(context, new VaultGuardianModel());
      this.shadowRadius = 2.64F;
   }

   public RenderType getRenderType(VaultGuardianEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
      return RenderType.entityCutoutNoCull(texture);
   }

   public void preRender(PoseStack poseStack, VaultGuardianEntity animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int renderColor) {
      float scale = 2.301F * (animatable.isJindanVariant() ? 1.035F : 1.0F);
      poseStack.scale(scale, scale, scale);
      super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, renderColor);
   }
}
