package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.entity.ZhenshanPalmEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ZhenshanPalmModel extends GeoModel<ZhenshanPalmEntity> {
   public ResourceLocation getModelResource(ZhenshanPalmEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "geo/entity/zhenshan_palm.geo.json");
   }

   public ResourceLocation getTextureResource(ZhenshanPalmEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "textures/entity/zhenshan_palm.png");
   }

   public ResourceLocation getAnimationResource(ZhenshanPalmEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "animations/entity/zhenshan_palm.animation.json");
   }
}
