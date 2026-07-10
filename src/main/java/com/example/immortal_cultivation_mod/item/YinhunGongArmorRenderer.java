package com.example.immortal_cultivation_mod.item;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class YinhunGongArmorRenderer extends GeoArmorRenderer<YinhunGongItem> {
    public YinhunGongArmorRenderer() {
        super(new YinhunGongItemModel());
    }

    @Override
    public RenderType getRenderType(YinhunGongItem animatable, ResourceLocation texture,
                                    MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutout(texture);
    }
}
