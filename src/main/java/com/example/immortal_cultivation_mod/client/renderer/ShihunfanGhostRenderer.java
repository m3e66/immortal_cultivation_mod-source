package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.client.model.ShihunfanGhostModel;
import com.example.immortal_cultivation_mod.entity.ShihunfanGhostEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class ShihunfanGhostRenderer extends HumanoidMobRenderer<ShihunfanGhostEntity, PlayerModel<ShihunfanGhostEntity>> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "textures/entity/zhuji3.png");

   public ShihunfanGhostRenderer(EntityRendererProvider.Context context) {
      super(context, new ShihunfanGhostModel(context.bakeLayer(ModelLayers.PLAYER_SLIM)), 0.0F);
   }

   public void render(ShihunfanGhostEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      RenderSystem.setShaderColor(0.28F, 0.52F, 0.48F, 0.22F);
      super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
      if (buffer instanceof MultiBufferSource.BufferSource source) {
         source.endBatch(this.getRenderType(entity, true, true, false));
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   protected RenderType getRenderType(ShihunfanGhostEntity entity, boolean bodyVisible, boolean translucent, boolean glowing) {
      return RenderType.entityTranslucent(TEXTURE);
   }

   public ResourceLocation getTextureLocation(ShihunfanGhostEntity entity) {
      return TEXTURE;
   }
}
