package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.entity.KongmingBeetleEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class KongmingBeetleModel extends GeoModel<KongmingBeetleEntity> {
   public ResourceLocation getModelResource(KongmingBeetleEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "geo/entity/kongming_beetle.geo.json");
   }

   public ResourceLocation getTextureResource(KongmingBeetleEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "textures/entity/kongming_beetle.png");
   }

   public ResourceLocation getAnimationResource(KongmingBeetleEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "animations/entity/kongming_beetle.animation.json");
   }
}
