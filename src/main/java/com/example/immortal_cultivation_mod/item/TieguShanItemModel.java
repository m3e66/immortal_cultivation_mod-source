package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TieguShanItemModel extends GeoModel<TieguShanItem> {
    @Override
    public ResourceLocation getModelResource(TieguShanItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "geo/item/tiegu_shan.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TieguShanItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "textures/item/tiegu_shan.png");
    }

    @Override
    public ResourceLocation getAnimationResource(TieguShanItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "animations/item/tiegu_shan.animation.json");
    }
}
