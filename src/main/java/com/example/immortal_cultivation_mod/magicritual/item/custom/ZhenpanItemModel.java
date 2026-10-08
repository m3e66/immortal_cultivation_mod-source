package com.example.immortal_cultivation_mod.magicritual.item.custom;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class ZhenpanItemModel extends GeoModel<ZhenpanItem> {
   public ResourceLocation getModelResource(ZhenpanItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "geo/item/zhenpan.geo.json");
   }

   public ResourceLocation getTextureResource(ZhenpanItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "textures/item/zhenpan.png");
   }

   public ResourceLocation getAnimationResource(ZhenpanItem animatable) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "animations/item/zhenpan.animation.json");
   }
}
