package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class StrawHatItemModel extends GeoModel<StrawHatItem> {
    @Override
    public ResourceLocation getModelResource(StrawHatItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "geo/item/straw_hat.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(StrawHatItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/item/straw_hat.png");
    }

    @Override
    public ResourceLocation getAnimationResource(StrawHatItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "animations/item/straw_hat.animation.json");
    }
}
