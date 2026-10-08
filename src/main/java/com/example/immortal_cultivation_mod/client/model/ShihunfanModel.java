package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.entity.ShihunfanEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ShihunfanModel extends GeoModel<ShihunfanEntity> {
   public ResourceLocation getModelResource(ShihunfanEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "geo/entity/shihunfan.geo.json");
   }

   public ResourceLocation getTextureResource(ShihunfanEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "textures/entity/shihunfan.png");
   }

   public ResourceLocation getAnimationResource(ShihunfanEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "animations/entity/shihunfan.animation.json");
   }
}
