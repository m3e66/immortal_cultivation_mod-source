package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GeoRockBlockItemModel extends GeoModel<GeoRockBlockItem> {
    @Override
    public ResourceLocation getModelResource(GeoRockBlockItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,
                "geo/block/" + animatable.modelName() + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GeoRockBlockItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,
                "textures/block/" + animatable.modelName() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(GeoRockBlockItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,
                "animations/block/geo_rock.animation.json");
    }
}
