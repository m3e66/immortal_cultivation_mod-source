package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.entity.ThunderhornRockBadgerEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public final class ThunderhornRockBadgerModel extends GeoModel<ThunderhornRockBadgerEntity> {
   private static final ResourceLocation MODEL = id("geo/entity/thunderhorn_rock_badger.geo.json");
   private static final ResourceLocation TEXTURE = id("textures/entity/thunderhorn_rock_badger.png");
   private static final ResourceLocation ANIMATION = id("animations/entity/thunderhorn_rock_badger.animation.json");

   public ResourceLocation getModelResource(ThunderhornRockBadgerEntity animatable) {
      return MODEL;
   }

   public ResourceLocation getTextureResource(ThunderhornRockBadgerEntity animatable) {
      return TEXTURE;
   }

   public ResourceLocation getAnimationResource(ThunderhornRockBadgerEntity animatable) {
      return ANIMATION;
   }

   public void setCustomAnimations(ThunderhornRockBadgerEntity animatable, long instanceId, AnimationState<ThunderhornRockBadgerEntity> animationState) {
      super.setCustomAnimations(animatable, instanceId, animationState);
      this.getBone("plate_front").ifPresent((bone) -> bone.setHidden(!animatable.hasPlate(0)));
      this.getBone("plate_middle").ifPresent((bone) -> bone.setHidden(!animatable.hasPlate(1)));
      this.getBone("plate_rear").ifPresent((bone) -> bone.setHidden(!animatable.hasPlate(2)));
   }

   private static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", path);
   }
}
