package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ZhenhunBellItemModel extends GeoModel<ZhenhunBellItem> {
    @Override
    public ResourceLocation getModelResource(ZhenhunBellItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "geo/item/zhenhun_bell.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ZhenhunBellItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/item/zhenhun_bell.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ZhenhunBellItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "animations/item/zhenhun_bell.animation.json");
    }
}
