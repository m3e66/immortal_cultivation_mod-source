package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.client.model.CultivationBestiaryBeastModel;
import com.example.immortal_cultivation_mod.entity.CultivationBestiaryBeastEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class CultivationBestiaryBeastRenderer
        extends GeoEntityRenderer<CultivationBestiaryBeastEntity> {
    public CultivationBestiaryBeastRenderer(EntityRendererProvider.Context context) {
        super(context, new CultivationBestiaryBeastModel());
        shadowRadius = 1.1F;
    }
}
