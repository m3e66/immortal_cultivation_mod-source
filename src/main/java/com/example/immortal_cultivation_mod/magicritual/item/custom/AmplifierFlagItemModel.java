package com.example.immortal_cultivation_mod.magicritual.item.custom;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class AmplifierFlagItemModel extends GeoModel<AmplifierFlagItem> {
    @Override
    public ResourceLocation getModelResource(AmplifierFlagItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "geo/item/amplifier_flag.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AmplifierFlagItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "textures/item/amplifier_flag.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AmplifierFlagItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "animations/item/duration_flag.animation.json");
    }
}
