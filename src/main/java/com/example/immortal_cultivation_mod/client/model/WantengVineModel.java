package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.entity.WantengVineEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class WantengVineModel extends GeoModel<WantengVineEntity> {
   public ResourceLocation getModelResource(WantengVineEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "geo/entity/wanteng_vine.geo.json");
   }

   public ResourceLocation getTextureResource(WantengVineEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "textures/block/lingmu_log.png");
   }

   public ResourceLocation getAnimationResource(WantengVineEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "animations/entity/wanteng_vine.animation.json");
   }
}
