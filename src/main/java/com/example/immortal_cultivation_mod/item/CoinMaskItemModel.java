package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CoinMaskItemModel extends GeoModel<CoinMaskItem> {
    @Override
    public ResourceLocation getModelResource(CoinMaskItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "geo/item/coin_mask.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CoinMaskItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/item/coin_mask.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CoinMaskItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "animations/item/coin_mask.animation.json");
    }
}
