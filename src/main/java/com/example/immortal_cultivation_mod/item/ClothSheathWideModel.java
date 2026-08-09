package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;

public class ClothSheathWideModel extends ClothSheathItemModel {
    @Override
    public ResourceLocation getModelResource(ClothSheathItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "geo/item/cloth_sheath_wide.geo.json");
    }
}
