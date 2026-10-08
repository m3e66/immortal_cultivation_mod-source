package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.client.model.ThunderhornRockBadgerModel;
import com.example.immortal_cultivation_mod.entity.ThunderhornRockBadgerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class ThunderhornRockBadgerRenderer extends GeoEntityRenderer<ThunderhornRockBadgerEntity> {
   public ThunderhornRockBadgerRenderer(EntityRendererProvider.Context context) {
      super(context, new ThunderhornRockBadgerModel());
      this.withScale(1.0F);
      this.shadowRadius = 1.2F;
   }
}
