package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SwordCaseItemModel extends GeoModel<SwordCaseItem> {
    @Override
    public ResourceLocation getModelResource(SwordCaseItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "geo/item/sword_case.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SwordCaseItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "textures/item/armor/sword_case.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SwordCaseItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "animations/item/sword_case.animation.json");
    }
}
