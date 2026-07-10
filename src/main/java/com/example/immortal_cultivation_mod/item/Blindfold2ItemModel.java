package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class Blindfold2ItemModel extends GeoModel<Blindfold2Item> {
    @Override
    public ResourceLocation getModelResource(Blindfold2Item animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "geo/item/blindfold2.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Blindfold2Item animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/item/blindfold.png");
    }

    @Override
    public ResourceLocation getAnimationResource(Blindfold2Item animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "animations/item/blindfold.animation.json");
    }
}
