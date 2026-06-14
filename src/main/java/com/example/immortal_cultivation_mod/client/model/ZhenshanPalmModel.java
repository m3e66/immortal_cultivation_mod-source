package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.ZhenshanPalmEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ZhenshanPalmModel extends GeoModel<ZhenshanPalmEntity> {
    @Override
    public ResourceLocation getModelResource(ZhenshanPalmEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "geo/entity/zhenshan_palm.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ZhenshanPalmEntity animatable) {
        return ResourceLocation.withDefaultNamespace("textures/block/dirt.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ZhenshanPalmEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "animations/entity/zhenshan_palm.animation.json");
    }
}
