package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.entity.VaultGuardianEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class VaultGuardianModel extends GeoModel<VaultGuardianEntity> {
   private static final ResourceLocation MODEL = id("geo/entity/vault_guardian.geo.json");
   private static final ResourceLocation ANIMATION = id("animations/entity/vault_guardian.animation.json");
   private static final ResourceLocation TEXTURE = id("textures/entity/vault_guardian.png");

   public ResourceLocation getModelResource(VaultGuardianEntity animatable) {
      return MODEL;
   }

   public ResourceLocation getTextureResource(VaultGuardianEntity animatable) {
      return TEXTURE;
   }

   public ResourceLocation getAnimationResource(VaultGuardianEntity animatable) {
      return ANIMATION;
   }

   private static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", path);
   }
}
