package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.block.GeoRockBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GeoRockModel extends GeoModel<GeoRockBlockEntity> {
   public ResourceLocation getModelResource(GeoRockBlockEntity animatable) {
      String modelName = animatable.modelName().endsWith("_spell_scroll_block") ? "realm_spell_scroll_block" : animatable.modelName();
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "geo/block/" + modelName + ".geo.json");
   }

   public ResourceLocation getTextureResource(GeoRockBlockEntity animatable) {
      String textureName = animatable.modelName().endsWith("_spell_scroll_block") ? "zhuji_spell_scroll_block" : animatable.modelName();
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "textures/block/" + textureName + ".png");
   }

   public ResourceLocation getAnimationResource(GeoRockBlockEntity animatable) {
      String var10000;
      switch (animatable.modelName()) {
         case "qingtong_yinhuo_ding" -> var10000 = "qingtong_yinhuo_ding";
         case "cu_tie_xiaoding" -> var10000 = "cu_tie_xiaoding";
         case "chi_jin_sanyang_ding" -> var10000 = "chi_jin_sanyang_ding";
         default -> var10000 = "geo_rock";
      }

      String animation = var10000;
      return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "animations/block/" + animation + ".animation.json");
   }
}
