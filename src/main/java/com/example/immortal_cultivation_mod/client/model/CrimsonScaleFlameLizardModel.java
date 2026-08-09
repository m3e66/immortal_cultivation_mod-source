package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.CrimsonScaleFlameLizardEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class CrimsonScaleFlameLizardModel extends GeoModel<CrimsonScaleFlameLizardEntity> {
    @Override
    public ResourceLocation getModelResource(CrimsonScaleFlameLizardEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID,
                "geo/entity/crimson_scale_flame_lizard.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CrimsonScaleFlameLizardEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID,
                "textures/entity/crimson_scale_flame_lizard.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CrimsonScaleFlameLizardEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID,
                "animations/entity/crimson_scale_flame_lizard.animation.json");
    }
}
