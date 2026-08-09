package com.example.immortal_cultivation_mod.item;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class ClothSheathWideArmorRenderer extends GeoArmorRenderer<ClothSheathItem> {
    public ClothSheathWideArmorRenderer() {
        super(new ClothSheathWideModel());
    }

    @Override
    public ResourceLocation getTextureLocation(ClothSheathItem animatable) {
        return ClothSheathItem.woolTexture(getCurrentStack());
    }
}
