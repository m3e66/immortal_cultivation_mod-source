package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ShihunfanItemModel extends GeoModel<ShihunfanItem> {
    @Override
    public ResourceLocation getModelResource(ShihunfanItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "geo/item/shihunfan.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ShihunfanItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "textures/entity/shihunfan.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ShihunfanItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "animations/item/shihunfan.animation.json");
    }
}
