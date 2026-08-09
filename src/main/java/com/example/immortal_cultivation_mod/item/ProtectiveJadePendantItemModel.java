package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ProtectiveJadePendantItemModel extends GeoModel<IdentityItems.ProtectivePendantItem> {
    @Override
    public ResourceLocation getModelResource(IdentityItems.ProtectivePendantItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "geo/item/protective_jade_pendant.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(IdentityItems.ProtectivePendantItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "textures/item/protective_jade_pendant.png");
    }

    @Override
    public ResourceLocation getAnimationResource(IdentityItems.ProtectivePendantItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "animations/item/jade_pendant.animation.json");
    }
}
