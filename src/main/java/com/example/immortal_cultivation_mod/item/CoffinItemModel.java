package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CoffinItemModel extends GeoModel<CoffinItem> {
    @Override
    public ResourceLocation getModelResource(CoffinItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "geo/item/coffin.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CoffinItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/item/coffin.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CoffinItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "animations/item/coffin.animation.json");
    }
}
