package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class JadePendantItemModel extends GeoModel<JadePendantItem> {
    @Override
    public ResourceLocation getModelResource(JadePendantItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "geo/item/jade_pendant.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(JadePendantItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/item/jade_pendant.png");
    }

    @Override
    public ResourceLocation getAnimationResource(JadePendantItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "animations/item/jade_pendant.animation.json");
    }
}
