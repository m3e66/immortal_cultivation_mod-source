package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class XiujianItemModel extends GeoModel<XiujianItem> {
    @Override
    public ResourceLocation getModelResource(XiujianItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID,
                "geo/item/xiujian.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(XiujianItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID,
                "textures/item/armor/xiujian.png");
    }

    @Override
    public ResourceLocation getAnimationResource(XiujianItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID,
                "animations/item/xiujian.animation.json");
    }
}
