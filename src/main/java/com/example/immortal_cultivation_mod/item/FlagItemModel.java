package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class FlagItemModel extends GeoModel<FlagItem> {
    @Override
    public ResourceLocation getModelResource(FlagItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "geo/item/flag.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(FlagItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/item/flag.png");
    }

    @Override
    public ResourceLocation getAnimationResource(FlagItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "animations/item/flag.animation.json");
    }
}
