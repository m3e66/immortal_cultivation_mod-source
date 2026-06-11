package com.example.immortal_cultivation_mod.client.renderer;

import com.example.immortal_cultivation_mod.block.GeoRockBlockEntity;
import com.example.immortal_cultivation_mod.client.model.GeoRockModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class GeoRockBlockRenderer extends GeoBlockRenderer<GeoRockBlockEntity> {
    public GeoRockBlockRenderer(BlockEntityRendererProvider.Context context) {
        super(new GeoRockModel());
    }
}
