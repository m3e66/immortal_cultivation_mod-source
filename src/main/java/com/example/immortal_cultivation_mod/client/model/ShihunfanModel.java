package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.ShihunfanEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ShihunfanModel extends GeoModel<ShihunfanEntity> {
    @Override
    public ResourceLocation getModelResource(ShihunfanEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "geo/entity/shihunfan.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ShihunfanEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "textures/entity/shihunfan.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ShihunfanEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "animations/entity/shihunfan.animation.json");
    }
}
