package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.AzureSpiritSilkwormEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class AzureSpiritSilkwormModel extends GeoModel<AzureSpiritSilkwormEntity> {
    @Override
    public ResourceLocation getModelResource(AzureSpiritSilkwormEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID,
                "geo/entity/azure_spirit_silkworm.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AzureSpiritSilkwormEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID,
                "textures/entity/azure_spirit_silkworm.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AzureSpiritSilkwormEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID,
                "animations/entity/azure_spirit_silkworm.animation.json");
    }
}
