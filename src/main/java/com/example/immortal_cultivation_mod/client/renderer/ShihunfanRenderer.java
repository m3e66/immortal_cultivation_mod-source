package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.client.model.ShihunfanModel;
import com.example.immortal_cultivation_mod.entity.ShihunfanEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ShihunfanRenderer extends GeoEntityRenderer<ShihunfanEntity> {
   public ShihunfanRenderer(EntityRendererProvider.Context context) {
      super(context, new ShihunfanModel());
      this.shadowRadius = 0.45F;
   }
}
