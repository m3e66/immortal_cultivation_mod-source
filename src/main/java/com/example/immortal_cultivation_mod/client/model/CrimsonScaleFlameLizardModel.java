package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.entity.CrimsonScaleFlameLizardEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class CrimsonScaleFlameLizardModel extends GeoModel<CrimsonScaleFlameLizardEntity> {
   public ResourceLocation getModelResource(CrimsonScaleFlameLizardEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "geo/entity/crimson_scale_flame_lizard.geo.json");
   }

   public ResourceLocation getTextureResource(CrimsonScaleFlameLizardEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "textures/entity/crimson_scale_flame_lizard.png");
   }

   public ResourceLocation getAnimationResource(CrimsonScaleFlameLizardEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "animations/entity/crimson_scale_flame_lizard.animation.json");
   }
}
