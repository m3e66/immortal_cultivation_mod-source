package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class HeavyIronBracerChestplateRenderer
        extends GeoArmorRenderer<HeavyIronBracerChestplateItem> {
    public HeavyIronBracerChestplateRenderer() {
        super(new DefaultedItemGeoModel<>(ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "armor/heavy_iron_bracer_chestplate")));
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
        setAllBonesVisible(false);
        if (currentSlot == EquipmentSlot.CHEST) {
            setBoneVisible(this.rightArm, true);
            setBoneVisible(this.leftArm, true);
        }
    }
}
