package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.entity.XuanjinZhengEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class XuanjinZhengModel extends GeoModel<XuanjinZhengEntity> {
    @Override public ResourceLocation getModelResource(XuanjinZhengEntity e) {
        return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod","geo/entity/xuanjin_zheng"+(e.seal()<6?"_seal_"+e.seal():"")+".geo.json");
    }
    @Override public ResourceLocation getTextureResource(XuanjinZhengEntity e) {
        return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod","textures/entity/xuanjin_zheng.png");
    }
    @Override public ResourceLocation getAnimationResource(XuanjinZhengEntity e) {
        return ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod","animations/entity/xuanjin_zheng.animation.json");
    }
}
