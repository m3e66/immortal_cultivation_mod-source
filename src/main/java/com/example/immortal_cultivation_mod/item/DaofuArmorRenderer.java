package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class DaofuArmorRenderer extends GeoArmorRenderer<DaofuChestplateItem> {
    public DaofuArmorRenderer() {
        super(new DefaultedItemGeoModel<>(
                ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "armor/daofu")));
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
        super.applyBoneVisibilityBySlot(currentSlot);

        if (currentSlot == EquipmentSlot.CHEST) {
            setBoneVisible(this.rightLeg, true);
            setBoneVisible(this.leftLeg, true);
            setBoneVisible(this.rightBoot, true);
            setBoneVisible(this.leftBoot, true);
        }
    }
}
