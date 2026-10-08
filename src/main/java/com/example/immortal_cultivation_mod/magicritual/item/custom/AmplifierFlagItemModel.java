package com.example.immortal_cultivation_mod.magicritual.item.custom;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class AmplifierFlagItemModel extends GeoModel<AmplifierFlagItem> {
   public ResourceLocation getModelResource(AmplifierFlagItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "geo/item/amplifier_flag.geo.json");
   }

   public ResourceLocation getTextureResource(AmplifierFlagItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "textures/item/amplifier_flag.png");
   }

   public ResourceLocation getAnimationResource(AmplifierFlagItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "animations/item/duration_flag.animation.json");
   }
}
