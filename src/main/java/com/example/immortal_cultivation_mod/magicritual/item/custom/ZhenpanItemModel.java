package com.example.immortal_cultivation_mod.magicritual.item.custom;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class ZhenpanItemModel extends GeoModel<ZhenpanItem> {
    @Override
    public ResourceLocation getModelResource(ZhenpanItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "geo/item/zhenpan.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ZhenpanItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "textures/item/zhenpan.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ZhenpanItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "animations/item/zhenpan.animation.json");
    }
}
