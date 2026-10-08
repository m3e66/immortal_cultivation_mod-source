package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GeoRockBlockItemModel extends GeoModel<GeoRockBlockItem> {
    @Override
    public ResourceLocation getModelResource(GeoRockBlockItem animatable) {
        String modelName = animatable.modelName().endsWith("_spell_scroll_block")
                ? "realm_spell_scroll_block"
                : animatable.modelName();
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,
                "geo/block/" + modelName + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GeoRockBlockItem animatable) {
        String textureName = animatable.modelName().endsWith("_spell_scroll_block")
                ? "zhuji_spell_scroll_block"
                : animatable.modelName();
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,
                "textures/block/" + textureName + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(GeoRockBlockItem animatable) {
        String animation = switch (animatable.modelName()) {
            case "qingtong_yinhuo_ding" -> "qingtong_yinhuo_ding";
            case "cu_tie_xiaoding" -> "cu_tie_xiaoding";
            case "chi_jin_sanyang_ding" -> "chi_jin_sanyang_ding";
            case "restriction_table" -> "restriction_table";
            default -> "geo_rock";
        };
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,
                "animations/block/" + animation + ".animation.json");
    }
}
