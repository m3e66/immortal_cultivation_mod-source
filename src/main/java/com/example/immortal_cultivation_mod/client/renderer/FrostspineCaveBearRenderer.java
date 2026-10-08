package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.client.model.FrostspineCaveBearModel;
import com.example.immortal_cultivation_mod.entity.FrostspineCaveBearEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class FrostspineCaveBearRenderer extends GeoEntityRenderer<FrostspineCaveBearEntity> {
   public FrostspineCaveBearRenderer(EntityRendererProvider.Context context) {
      super(context, new FrostspineCaveBearModel());
      this.withScale(1.0F);
      this.shadowRadius = 1.45F;
   }
}
