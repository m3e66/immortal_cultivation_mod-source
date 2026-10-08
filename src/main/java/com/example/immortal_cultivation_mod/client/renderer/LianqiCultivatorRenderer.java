package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.client.model.LianqiCultivatorModel;
import com.example.immortal_cultivation_mod.entity.LianqiCultivatorEntity;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class LianqiCultivatorRenderer extends HumanoidMobRenderer<LianqiCultivatorEntity, LianqiCultivatorModel> {
   public LianqiCultivatorRenderer(EntityRendererProvider.Context context) {
      super(context, new LianqiCultivatorModel(context.bakeLayer(ModelLayers.PLAYER_SLIM)), 0.5F);
      this.addLayer(new HumanoidArmorLayer(this, new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)), new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
      this.addLayer(new ItemInHandLayer(this, context.getItemInHandRenderer()));
   }

   public ResourceLocation getTextureLocation(LianqiCultivatorEntity entity) {
      return NpcProfile.skinTexture(entity, "lianqi");
   }
}
