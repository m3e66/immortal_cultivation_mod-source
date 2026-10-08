package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.client.model.HotelNpcModel;
import com.example.immortal_cultivation_mod.entity.HotelNpcEntity;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class HotelNpcRenderer extends HumanoidMobRenderer<HotelNpcEntity, HotelNpcModel> {
   public HotelNpcRenderer(EntityRendererProvider.Context context) {
      super(context, new HotelNpcModel(context.bakeLayer(ModelLayers.PLAYER_SLIM)), 0.5F);
   }

   public ResourceLocation getTextureLocation(HotelNpcEntity entity) {
      return NpcProfile.skinTexture(entity, "fanren");
   }
}
