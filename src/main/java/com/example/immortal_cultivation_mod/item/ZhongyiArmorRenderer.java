package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class ZhongyiArmorRenderer extends GeoArmorRenderer<ZhongyiItem> {
    public ZhongyiArmorRenderer() {
        super(new DefaultedItemGeoModel<>(
                ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID,
                        "armor/zhongyi")));
    }
}
