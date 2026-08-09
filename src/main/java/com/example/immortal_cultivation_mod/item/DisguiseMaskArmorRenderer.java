package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.ClientData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class DisguiseMaskArmorRenderer extends GeoArmorRenderer<DisguiseMaskItem> {
    public DisguiseMaskArmorRenderer() {
        super(new DefaultedItemGeoModel<>(ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "armor/disguise_mask")));
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
        setAllBonesVisible(false);
        if (currentSlot == EquipmentSlot.HEAD && ClientData.debugEyeEnabled()) {
            setBoneVisible(this.head, true);
        }
    }
}
