package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.entity.PhantomVeilFoxEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public final class PhantomVeilFoxModel extends GeoModel<PhantomVeilFoxEntity> {
    private static final ResourceLocation MODEL = id("geo/entity/phantom_veil_fox.geo.json");
    private static final ResourceLocation TEXTURE = id("textures/entity/phantom_veil_fox.png");
    private static final ResourceLocation ANIMATION = id("animations/entity/phantom_veil_fox.animation.json");

    @Override
    public ResourceLocation getModelResource(PhantomVeilFoxEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(PhantomVeilFoxEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(PhantomVeilFoxEntity animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(PhantomVeilFoxEntity animatable, long instanceId,
                                    AnimationState<PhantomVeilFoxEntity> state) {
        super.setCustomAnimations(animatable, instanceId, state);
        getBone("spirit_mask").ifPresent(
                bone -> bone.setHidden(!animatable.isSpiritMaskVisible()));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", path);
    }
}
