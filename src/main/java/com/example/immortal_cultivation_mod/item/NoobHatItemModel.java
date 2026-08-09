package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class NoobHatItemModel extends GeoModel<NoobHatItem> {
    @Override
    public ResourceLocation getModelResource(NoobHatItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "geo/item/noob_hat.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(NoobHatItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "textures/item/noob_hat.png");
    }

    @Override
    public ResourceLocation getAnimationResource(NoobHatItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "animations/item/noob_hat.animation.json");
    }
}
