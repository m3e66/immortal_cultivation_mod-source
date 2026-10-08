package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.entity.FuchenBindingPlumeEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class FuchenBindingPlumeModel extends GeoModel<FuchenBindingPlumeEntity> {
   public ResourceLocation getModelResource(FuchenBindingPlumeEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "geo/entity/fuchen_binding_plume.geo.json");
   }

   public ResourceLocation getTextureResource(FuchenBindingPlumeEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "textures/entity/fuchen_binding_plume.png");
   }

   public ResourceLocation getAnimationResource(FuchenBindingPlumeEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "animations/entity/fuchen_binding_plume.animation.json");
   }
}
