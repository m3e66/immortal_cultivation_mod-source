package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.block.MedicineCabinetBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public final class MedicineCabinetModel extends GeoModel<MedicineCabinetBlockEntity> {
   private static ResourceLocation resource(String folder, String name, String suffix) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", folder + "/" + name + suffix);
   }

   public ResourceLocation getModelResource(MedicineCabinetBlockEntity animatable) {
      return resource("geo/block", animatable.variant().modelName(), ".geo.json");
   }

   public ResourceLocation getTextureResource(MedicineCabinetBlockEntity animatable) {
      return resource("textures/block", animatable.variant().modelName(), ".png");
   }

   public ResourceLocation getAnimationResource(MedicineCabinetBlockEntity animatable) {
      return resource("animations/block", animatable.variant().modelName(), ".animation.json");
   }

   public void setCustomAnimations(MedicineCabinetBlockEntity animatable, long instanceId, AnimationState<MedicineCabinetBlockEntity> animationState) {
      super.setCustomAnimations(animatable, instanceId, animationState);
      float partialTick = animationState.getPartialTick();

      for(int drawer = 0; drawer < animatable.variant().drawerCount(); ++drawer) {
         float pull = animatable.drawerProgress(drawer, partialTick);
         this.getBone("drawer_" + drawer).ifPresent((bone) -> bone.setPosZ(-5.0F * pull));
      }

   }
}
