package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.CrimsonSkyFlameFloodDragonEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class CrimsonSkyFlameFloodDragonModel
        extends GeoModel<CrimsonSkyFlameFloodDragonEntity> {
    @Override
    public ResourceLocation getModelResource(CrimsonSkyFlameFloodDragonEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,
                "geo/entity/crimson_sky_flame_flood_dragon.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CrimsonSkyFlameFloodDragonEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,
                "textures/entity/crimson_sky_flame_flood_dragon.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CrimsonSkyFlameFloodDragonEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,
                "animations/entity/crimson_sky_flame_flood_dragon.animation.json");
    }
}
