package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.block.DeadSkeletonBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class DeadSkeletonModel extends GeoModel<DeadSkeletonBlockEntity> {
   private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "geo/block/dead_skeleton.geo.json");
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "textures/block/dead_skeleton.png");
   private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "animations/block/dead_skeleton.animation.json");

   public ResourceLocation getModelResource(DeadSkeletonBlockEntity animatable) {
      return MODEL;
   }

   public ResourceLocation getTextureResource(DeadSkeletonBlockEntity animatable) {
      return TEXTURE;
   }

   public ResourceLocation getAnimationResource(DeadSkeletonBlockEntity animatable) {
      return ANIMATION;
   }
}
