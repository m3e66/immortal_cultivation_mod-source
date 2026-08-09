package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class YaoyuanFistBoneItemModel extends GeoModel<HeavyIronBracerItem> {
    @Override
    public ResourceLocation getModelResource(HeavyIronBracerItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "geo/item/yaoyuan_fist_bone.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HeavyIronBracerItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "textures/item/yaoyuan_fist_bone.png");
    }

    @Override
    public ResourceLocation getAnimationResource(HeavyIronBracerItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "animations/item/yaoyuan_fist_bone.animation.json");
    }
}
