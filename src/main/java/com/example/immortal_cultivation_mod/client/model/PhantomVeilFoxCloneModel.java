package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.entity.PhantomVeilFoxCloneEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class PhantomVeilFoxCloneModel extends GeoModel<PhantomVeilFoxCloneEntity> {
    private static final ResourceLocation MODEL = id("geo/entity/phantom_veil_fox.geo.json");
    private static final ResourceLocation TEXTURE = id("textures/entity/phantom_veil_fox.png");
    private static final ResourceLocation ANIMATION = id("animations/entity/phantom_veil_fox.animation.json");

    @Override
    public ResourceLocation getModelResource(PhantomVeilFoxCloneEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(PhantomVeilFoxCloneEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(PhantomVeilFoxCloneEntity animatable) {
        return ANIMATION;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", path);
    }
}
