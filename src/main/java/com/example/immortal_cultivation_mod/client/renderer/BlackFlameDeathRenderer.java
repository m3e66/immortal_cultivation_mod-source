package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.client.BlackFlameClient;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.event.GeoRenderEvent;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class BlackFlameDeathRenderer {
   private static final ResourceLocation WHITE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
   private static final RenderType DARK_BODY;

   private BlackFlameDeathRenderer() {
   }

   public static void onGeoEntityPost(GeoRenderEvent.Entity.Post event) {
      Entity entity = event.getEntity();
      if (entity instanceof LivingEntity && entity instanceof GeoAnimatable) {
         BlackFlameClient.DeathAppearance appearance = BlackFlameClient.deathAppearance(entity, event.getPartialTick());
         if (appearance != null && appearance.argb() != 0) {
            GeoEntityRenderer renderer = event.getRenderer();
            VertexConsumer vertices = event.getBufferSource().getBuffer(DARK_BODY);
            renderer.reRender(event.getModel(), event.getPoseStack(), event.getBufferSource(), (GeoAnimatable)entity, DARK_BODY, vertices, event.getPartialTick(), 15728880, OverlayTexture.NO_OVERLAY, appearance.argb());
         }
      }
   }

   static {
      DARK_BODY = RenderType.entityTranslucentEmissive(WHITE_TEXTURE);
   }

   public static final class Layer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
      public Layer(RenderLayerParent<T, M> parent) {
         super(parent);
      }

      public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
         if (!(entity instanceof GeoAnimatable)) {
            BlackFlameClient.DeathAppearance appearance = BlackFlameClient.deathAppearance(entity, partialTick);
            if (appearance != null && appearance.argb() != 0) {
               VertexConsumer vertices = bufferSource.getBuffer(BlackFlameDeathRenderer.DARK_BODY);
               this.getParentModel().renderToBuffer(poseStack, vertices, 15728880, OverlayTexture.NO_OVERLAY, appearance.argb());
            }
         }
      }
   }
}
