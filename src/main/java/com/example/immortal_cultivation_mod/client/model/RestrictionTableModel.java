package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.block.RestrictionTableBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class RestrictionTableModel extends GeoModel<RestrictionTableBlockEntity> {
   private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "geo/block/restriction_table.geo.json");
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "textures/block/restriction_table.png");
   private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "animations/block/restriction_table.animation.json");

   public ResourceLocation getModelResource(RestrictionTableBlockEntity animatable) {
      return MODEL;
   }

   public ResourceLocation getTextureResource(RestrictionTableBlockEntity animatable) {
      return TEXTURE;
   }

   public ResourceLocation getAnimationResource(RestrictionTableBlockEntity animatable) {
      return ANIMATION;
   }
}
