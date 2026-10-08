package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.entity.AzureSpiritSilkwormEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class AzureSpiritSilkwormModel extends GeoModel<AzureSpiritSilkwormEntity> {
   public ResourceLocation getModelResource(AzureSpiritSilkwormEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "geo/entity/azure_spirit_silkworm.geo.json");
   }

   public ResourceLocation getTextureResource(AzureSpiritSilkwormEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "textures/entity/azure_spirit_silkworm.png");
   }

   public ResourceLocation getAnimationResource(AzureSpiritSilkwormEntity animatable) {
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "animations/entity/azure_spirit_silkworm.animation.json");
   }
}
