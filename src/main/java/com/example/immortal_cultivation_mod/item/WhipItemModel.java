package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class WhipItemModel extends GeoModel<WhipItem> {
    @Override
    public ResourceLocation getModelResource(WhipItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "geo/item/whip.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(WhipItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "textures/item/whip.png");
    }

    @Override
    public ResourceLocation getAnimationResource(WhipItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID,
                "animations/item/whip.animation.json");
    }
}
