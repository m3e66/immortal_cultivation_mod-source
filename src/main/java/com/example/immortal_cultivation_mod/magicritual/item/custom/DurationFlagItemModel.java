package com.example.immortal_cultivation_mod.magicritual.item.custom;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class DurationFlagItemModel extends GeoModel<DurationFlagItem> {
   public ResourceLocation getModelResource(DurationFlagItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "geo/item/" + animatable.textureName() + ".geo.json");
   }

   public ResourceLocation getTextureResource(DurationFlagItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "textures/item/" + animatable.textureName() + ".png");
   }

   public ResourceLocation getAnimationResource(DurationFlagItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "animations/item/duration_flag.animation.json");
   }
}
