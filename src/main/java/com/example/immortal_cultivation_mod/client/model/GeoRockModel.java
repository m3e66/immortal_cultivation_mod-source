package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.block.GeoRockBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GeoRockModel extends GeoModel<GeoRockBlockEntity> {
    @Override
    public ResourceLocation getModelResource(GeoRockBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,
                "geo/block/" + animatable.modelName() + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GeoRockBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,
                "textures/block/" + animatable.modelName() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(GeoRockBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,
                "animations/block/geo_rock.animation.json");
    }
}
