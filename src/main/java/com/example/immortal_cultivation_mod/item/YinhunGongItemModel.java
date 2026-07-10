package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class YinhunGongItemModel extends GeoModel<YinhunGongItem> {
    @Override
    public ResourceLocation getModelResource(YinhunGongItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "geo/item/yinhun_gong.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(YinhunGongItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/item/yinhun_gong_geo.png");
    }

    @Override
    public ResourceLocation getAnimationResource(YinhunGongItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "animations/item/yinhun_gong.animation.json");
    }
}
