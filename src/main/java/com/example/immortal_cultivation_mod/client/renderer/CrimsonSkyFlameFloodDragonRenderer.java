package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.client.model.CrimsonSkyFlameFloodDragonModel;
import com.example.immortal_cultivation_mod.entity.CrimsonSkyFlameFloodDragonEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class CrimsonSkyFlameFloodDragonRenderer
        extends GeoEntityRenderer<CrimsonSkyFlameFloodDragonEntity> {
    public CrimsonSkyFlameFloodDragonRenderer(EntityRendererProvider.Context context) {
        super(context, new CrimsonSkyFlameFloodDragonModel());
        withScale(CrimsonSkyFlameFloodDragonEntity.MODEL_SCALE);
        shadowRadius = 2.4F * CrimsonSkyFlameFloodDragonEntity.MODEL_SCALE;
    }
}
