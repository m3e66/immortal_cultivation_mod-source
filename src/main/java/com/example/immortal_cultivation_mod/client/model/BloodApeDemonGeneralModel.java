package com.example.immortal_cultivation_mod.client.model;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.BloodApeDemonGeneralEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class BloodApeDemonGeneralModel extends GeoModel<BloodApeDemonGeneralEntity> {
    @Override
    public ResourceLocation getModelResource(BloodApeDemonGeneralEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "geo/entity/xueyuan_yaojiang.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BloodApeDemonGeneralEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "textures/entity/xueyuan_yaojiang.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BloodApeDemonGeneralEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID,
                "animations/entity/xueyuan_yaojiang.animation.json");
    }
}
