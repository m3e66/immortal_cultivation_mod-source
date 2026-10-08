package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.entity.XuyingShadowEntity;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class XuyingShadowRenderer extends HumanoidMobRenderer<XuyingShadowEntity, HumanoidModel<XuyingShadowEntity>> {
   private static final float ALPHA = 0.22F;

   public XuyingShadowRenderer(EntityRendererProvider.Context context) {
      super(context, new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_SLIM)), 0.0F);
   }

   public void render(XuyingShadowEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      ((HumanoidModel)this.model).young = false;
      ((HumanoidModel)this.model).attackTime = 0.0F;
      ((HumanoidModel)this.model).riding = false;
      ((HumanoidModel)this.model).crouching = entity.isCrouching();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.22F);
      super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   protected RenderType getRenderType(XuyingShadowEntity entity, boolean bodyVisible, boolean translucent, boolean glowing) {
      return RenderType.entityTranslucent(this.getTextureLocation(entity));
   }

   public ResourceLocation getTextureLocation(XuyingShadowEntity entity) {
      GameProfile profile = new GameProfile((UUID)entity.ownerUuid().orElse(Util.NIL_UUID), "");
      return Minecraft.getInstance().getSkinManager().getInsecureSkin(profile).texture();
   }
}
