package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ClothSheathItemModel extends GeoModel<ClothSheathItem> {
    @Override
    public ResourceLocation getModelResource(ClothSheathItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "geo/item/cloth_sheath_thin.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ClothSheathItem animatable) {
        return ResourceLocation.withDefaultNamespace("textures/block/white_wool.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ClothSheathItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "animations/item/cloth_sheath.animation.json");
    }
}
