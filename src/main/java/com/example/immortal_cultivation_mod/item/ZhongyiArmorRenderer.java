package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class ZhongyiArmorRenderer extends GeoArmorRenderer<ZhongyiItem> {
    private static final ResourceLocation[] TEXTURES = ClothingWear.textures("zhongyi");

    public ZhongyiArmorRenderer() {
        super(new DefaultedItemGeoModel<>(
                ResourceLocation.fromNamespaceAndPath(
                        ImmortalCultivationMod.MODID,
                        "armor/zhongyi")));
    }

    @Override
    public ResourceLocation getTextureLocation(ZhongyiItem animatable) {
        return TEXTURES[ClothingWear.stage(getCurrentStack())];
    }
}
