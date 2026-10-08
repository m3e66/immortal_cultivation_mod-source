package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.client.model.KongmingBeetleModel;
import com.example.immortal_cultivation_mod.entity.KongmingBeetleEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class KongmingBeetleRenderer extends GeoEntityRenderer<KongmingBeetleEntity> {
   public KongmingBeetleRenderer(EntityRendererProvider.Context context) {
      super(context, new KongmingBeetleModel());
      this.withScale(1.0F);
      this.shadowRadius = 0.9F;
   }
}
