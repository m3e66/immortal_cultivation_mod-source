package com.example.immortal_cultivation_mod.item;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class ClothSheathThinArmorRenderer extends GeoArmorRenderer<ClothSheathItem> {
    public ClothSheathThinArmorRenderer() {
        super(new ClothSheathItemModel());
    }

    @Override
    public ResourceLocation getTextureLocation(ClothSheathItem animatable) {
        return ClothSheathItem.woolTexture(getCurrentStack());
    }
}
