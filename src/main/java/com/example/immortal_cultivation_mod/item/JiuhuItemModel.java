package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class JiuhuItemModel extends GeoModel<JiuhuItem> {
    @Override
    public ResourceLocation getModelResource(JiuhuItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID,
                "geo/item/jiuhu.geo.json"
        );
    }

    @Override
    public ResourceLocation getTextureResource(JiuhuItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID,
                "textures/item/jiuhu.png"
        );
    }

    @Override
    public ResourceLocation getAnimationResource(JiuhuItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID,
                "animations/item/jiuhu.animation.json"
        );
    }
}