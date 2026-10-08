package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.client.SpiritualSenseClient;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.animatable.GeoAnimatable;

public final class SpiritualReconstructionLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
   public SpiritualReconstructionLayer(RenderLayerParent<T, M> parent) {
      super(parent);
   }

   public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
      if (!(entity instanceof GeoAnimatable)) {
         SpiritualSenseClient.Reconstruction reconstruction = SpiritualSenseClient.reconstructionFor(entity.getId());
         if (reconstruction != null) {
            RenderType fallback = SpiritualReconstructionRenderer.fallbackRenderType();
            VertexConsumer fallbackVertices = bufferSource.getBuffer(fallback);
            this.getParentModel().renderToBuffer(poseStack, fallbackVertices, 15728880, OverlayTexture.NO_OVERLAY, SpiritualReconstructionRenderer.fallbackArgb(reconstruction));
            if (SpiritualReconstructionRenderer.isReady()) {
               ResourceLocation texture = this.getTextureLocation(entity);
               RenderType renderType = SpiritualReconstructionRenderer.renderType(texture);
               VertexConsumer vertices = bufferSource.getBuffer(renderType);
               this.getParentModel().renderToBuffer(poseStack, vertices, SpiritualReconstructionRenderer.encodedLight(reconstruction), SpiritualReconstructionRenderer.encodedOverlay(reconstruction), reconstruction.argb());
            }
         }
      }
   }
}
