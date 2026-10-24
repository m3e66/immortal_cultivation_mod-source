package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.entity.SilverMistWolfEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class SilverMistWolfModel extends GeoModel<SilverMistWolfEntity> {
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod",path);}
    @Override public ResourceLocation getModelResource(SilverMistWolfEntity wolf){return id("geo/entity/silver_mist_wolf.geo.json");}
    @Override public ResourceLocation getTextureResource(SilverMistWolfEntity wolf){return id("textures/entity/silver_mist_wolf.png");}
    @Override public ResourceLocation getAnimationResource(SilverMistWolfEntity wolf){return id("animations/entity/silver_mist_wolf.animation.json");}
}
