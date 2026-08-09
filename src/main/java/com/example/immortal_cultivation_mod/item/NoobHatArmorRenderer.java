package com.example.immortal_cultivation_mod.item;

import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class NoobHatArmorRenderer extends GeoArmorRenderer<NoobHatItem> {
    public NoobHatArmorRenderer() {
        super(new NoobHatItemModel());
    }

    @Override
    public GeoBone getHeadBone(GeoModel<NoobHatItem> model) {
        return model.getBone("root").orElse(null);
    }
}
