package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class HeavyIronBracerDebugItemModel extends GeoModel<HeavyIronBracerItem> {
    @Override
    public ResourceLocation getModelResource(HeavyIronBracerItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "geo/item/heavy_iron_bracer_debug.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HeavyIronBracerItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "textures/item/heavy_iron_bracer.png");
    }

    @Override
    public ResourceLocation getAnimationResource(HeavyIronBracerItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "animations/item/heavy_iron_bracer.animation.json");
    }
}
