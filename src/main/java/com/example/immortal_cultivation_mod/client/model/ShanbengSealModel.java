package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.entity.ShanbengSealEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class ShanbengSealModel extends GeoModel<ShanbengSealEntity> {
   public ResourceLocation getModelResource(ShanbengSealEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "geo/entity/shanbeng_seal.geo.json");
   }

   public ResourceLocation getTextureResource(ShanbengSealEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/tuff.png");
   }

   public ResourceLocation getAnimationResource(ShanbengSealEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "animations/entity/shanbeng_seal.animation.json");
   }
}
