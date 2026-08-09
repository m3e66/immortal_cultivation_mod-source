package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.client.model.AzureSpiritSilkwormModel;
import com.example.immortal_cultivation_mod.entity.AzureSpiritSilkwormEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class AzureSpiritSilkwormRenderer
        extends GeoEntityRenderer<AzureSpiritSilkwormEntity> {
    public AzureSpiritSilkwormRenderer(EntityRendererProvider.Context context) {
        super(context, new AzureSpiritSilkwormModel());
        withScale(AzureSpiritSilkwormEntity.MODEL_SCALE);
        shadowRadius = 0.28F;
    }
}
