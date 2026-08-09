package com.example.immortal_cultivation_mod.magicritual.item.custom;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class DurationFlagItemModel extends GeoModel<DurationFlagItem> {
    @Override
    public ResourceLocation getModelResource(DurationFlagItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID,
                "geo/item/" + animatable.textureName() + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DurationFlagItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID,
                "textures/item/" + animatable.textureName() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(DurationFlagItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "animations/item/duration_flag.animation.json");
    }
}
