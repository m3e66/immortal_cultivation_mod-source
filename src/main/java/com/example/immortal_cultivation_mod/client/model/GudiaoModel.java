package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.GudiaoEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GudiaoModel extends GeoModel<GudiaoEntity> {
    @Override
    public ResourceLocation getModelResource(GudiaoEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "geo/entity/gudiao.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GudiaoEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/entity/gudiao.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GudiaoEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "animations/entity/gudiao.animation.json");
    }
}
