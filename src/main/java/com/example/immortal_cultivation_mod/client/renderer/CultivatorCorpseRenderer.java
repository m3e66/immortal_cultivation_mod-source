package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.client.model.CultivatorCorpseModel;
import com.example.immortal_cultivation_mod.entity.CultivatorCorpseEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

public class CultivatorCorpseRenderer extends LivingEntityRenderer<CultivatorCorpseEntity, CultivatorCorpseModel> {
   public CultivatorCorpseRenderer(EntityRendererProvider.Context context) {
      super(context, new CultivatorCorpseModel(context.bakeLayer(ModelLayers.PLAYER_SLIM)), 0.0F);
      this.addLayer(new HumanoidArmorLayer(this, new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)), new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
      this.addLayer(new ZhenshiSealHelmetLayer(this));
   }

   public ResourceLocation getTextureLocation(CultivatorCorpseEntity entity) {
      return !entity.getTextureId().isBlank() ? ResourceLocation.parse(entity.getTextureId()) : Minecraft.getInstance().getSkinManager().getInsecureSkin(entity.getGameProfile()).texture();
   }

   protected float getBob(CultivatorCorpseEntity livingBase, float partialTicks) {
      return 0.0F;
   }

   protected void setupRotations(CultivatorCorpseEntity entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks, float scale) {
      if (entity.isRaised()) {
         super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks, scale);
      } else {
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getCorpseYaw()));
         poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
         // Center the two-block body on its flat collision box and rest its back on the ground.
         poseStack.translate(0.0F, -1.0F, -0.25F);
      }
   }
}
