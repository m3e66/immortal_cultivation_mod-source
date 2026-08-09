package com.example.immortal_cultivation_mod.item;

import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class HeavyIronBracerItemRenderer extends GeoItemRenderer<HeavyIronBracerItem> {
    private final HeavyIronBracerDebugItemModel debugModel = new HeavyIronBracerDebugItemModel();
    private final YaoyuanFistBoneItemModel yaoyuanFistBoneModel = new YaoyuanFistBoneItemModel();

    public HeavyIronBracerItemRenderer() {
        super(new HeavyIronBracerItemModel());
    }

    @Override
    public GeoModel<HeavyIronBracerItem> getGeoModel() {
        if (getCurrentItemStack().is(ModItems.YAOYUAN_FIST_BONE.get())) {
            return yaoyuanFistBoneModel;
        }
        return HeavyIronBracerItem.isDebugStack(getCurrentItemStack()) ? debugModel : super.getGeoModel();
    }
}
