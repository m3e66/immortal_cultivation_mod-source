package com.example.immortal_cultivation_mod.item;

import software.bernie.geckolib.renderer.GeoArmorRenderer;

public final class RigidSheathArmorRenderer extends GeoArmorRenderer<RigidSheathItem> {
    public RigidSheathArmorRenderer(RigidSheathItem.Kind kind, boolean loaded) {
        super(new RigidSheathModel(kind, loaded));
    }
}
