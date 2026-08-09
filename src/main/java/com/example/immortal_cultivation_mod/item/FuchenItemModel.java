package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class FuchenItemModel extends GeoModel<FuchenItem> {
    @Override
    public ResourceLocation getModelResource(FuchenItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "geo/item/fuchen.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(FuchenItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "textures/item/fuchen.png");
    }

    @Override
    public ResourceLocation getAnimationResource(FuchenItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID,
                "animations/item/fuchen.animation.json");
    }
}
