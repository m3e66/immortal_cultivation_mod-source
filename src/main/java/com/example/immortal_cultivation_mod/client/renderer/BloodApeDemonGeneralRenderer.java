package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.client.model.BloodApeDemonGeneralModel;
import com.example.immortal_cultivation_mod.entity.BloodApeDemonGeneralEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class BloodApeDemonGeneralRenderer
        extends GeoEntityRenderer<BloodApeDemonGeneralEntity> {
    public BloodApeDemonGeneralRenderer(EntityRendererProvider.Context context) {
        super(context, new BloodApeDemonGeneralModel());
        shadowRadius = 0.95F;
    }
}
