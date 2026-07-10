package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BlindfoldItemModel extends GeoModel<BlindfoldItem> {
    @Override
    public ResourceLocation getModelResource(BlindfoldItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "geo/item/blindfold.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BlindfoldItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/item/blindfold.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BlindfoldItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "animations/item/blindfold.animation.json");
    }
}
