package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.client.model.GudiaoModel;
import com.example.immortal_cultivation_mod.entity.GudiaoEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GudiaoRenderer extends GeoEntityRenderer<GudiaoEntity> {
    public GudiaoRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GudiaoModel());
        withScale(1.25F);
        this.shadowRadius = 0.7F;
    }
}
