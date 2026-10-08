package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class RigidSheathModel extends GeoModel<RigidSheathItem> {
    private final RigidSheathItem.Kind kind;
    private final boolean loaded;

    public RigidSheathModel(RigidSheathItem.Kind kind, boolean loaded) {
        this.kind = kind;
        this.loaded = loaded;
    }

    @Override
    public ResourceLocation getModelResource(RigidSheathItem animatable) {
        String type = kind == RigidSheathItem.Kind.DAO ? "dao" : "sword";
        String state = loaded ? "with_weapon" : "empty";
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "geo/item/" + type + "_sheath_" + state + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(RigidSheathItem animatable) {
        return kind == RigidSheathItem.Kind.DAO
                ? ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "textures/item/armor/dao_sheath.png")
                : ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID, "textures/item/armor/sword_sheath.png");
    }

    @Override
    public ResourceLocation getAnimationResource(RigidSheathItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "animations/item/cloth_sheath.animation.json");
    }
}
