package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.entity.FrostspineCaveBearEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public final class FrostspineCaveBearModel extends GeoModel<FrostspineCaveBearEntity> {
   private static final ResourceLocation MODEL = id("geo/entity/frostspine_cave_bear.geo.json");
   private static final ResourceLocation TEXTURE = id("textures/entity/frostspine_cave_bear.png");
   private static final ResourceLocation ANIMATION = id("animations/entity/frostspine_cave_bear.animation.json");

   public ResourceLocation getModelResource(FrostspineCaveBearEntity animatable) {
      return MODEL;
   }

   public ResourceLocation getTextureResource(FrostspineCaveBearEntity animatable) {
      return TEXTURE;
   }

   public ResourceLocation getAnimationResource(FrostspineCaveBearEntity animatable) {
      return ANIMATION;
   }

   public void setCustomAnimations(FrostspineCaveBearEntity animatable, long instanceId, AnimationState<FrostspineCaveBearEntity> state) {
      super.setCustomAnimations(animatable, instanceId, state);

      for(int i = 0; i < 4; ++i) {
         int ridgeIndex = i;
         this.getBone("ridge_" + ridgeIndex).ifPresent((bone) -> bone.setHidden(!animatable.hasRidge(ridgeIndex)));
      }
      this.getBone("ridge_0_head").ifPresent((bone) -> bone.setHidden(!animatable.hasRidge(0)));
      this.getBone("ridge_0_neck").ifPresent((bone) -> bone.setHidden(!animatable.hasRidge(0)));

   }

   private static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", path);
   }
}
