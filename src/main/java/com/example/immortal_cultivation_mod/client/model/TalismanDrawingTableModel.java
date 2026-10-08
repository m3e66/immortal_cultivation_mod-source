package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.block.TalismanDrawingTableBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class TalismanDrawingTableModel extends GeoModel<TalismanDrawingTableBlockEntity> {
   private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "geo/block/talisman_drawing_table.geo.json");
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "textures/block/talisman_drawing_table.png");
   private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "animations/block/talisman_drawing_table.animation.json");

   public ResourceLocation getModelResource(TalismanDrawingTableBlockEntity animatable) {
      return MODEL;
   }

   public ResourceLocation getTextureResource(TalismanDrawingTableBlockEntity animatable) {
      return TEXTURE;
   }

   public ResourceLocation getAnimationResource(TalismanDrawingTableBlockEntity animatable) {
      return ANIMATION;
   }
}
