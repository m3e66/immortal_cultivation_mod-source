package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.FuchenBindingPlumeEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class FuchenBindingPlumeModel extends GeoModel<FuchenBindingPlumeEntity> {
    @Override
    public ResourceLocation getModelResource(FuchenBindingPlumeEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID,
                "geo/entity/fuchen_binding_plume.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(FuchenBindingPlumeEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID,
                "textures/entity/fuchen_binding_plume.png");
    }

    @Override
    public ResourceLocation getAnimationResource(FuchenBindingPlumeEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID,
                "animations/entity/fuchen_binding_plume.animation.json");
    }
}
