package com.example.immortal_cultivation_mod.event;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.item.ForgingSystem;
import com.example.immortal_cultivation_mod.item.QiInfusedWeapon;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = ImmortalCultivationMod.MODID, value = Dist.CLIENT)
public final class ClientItemEvents {
    private ClientItemEvents() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (QiInfusedWeapon.getQi(event.getItemStack()) > 0.0D) {
            event.getToolTip().add(QiInfusedWeapon.tooltip(event.getItemStack()));
        }
        if (ForgingSystem.isForgeMaterial(event.getItemStack())) {
            event.getToolTip().add(ForgingSystem.purityTooltip(event.getItemStack()));
            if (Screen.hasShiftDown()) {
                event.getToolTip().add(ForgingSystem.materialBuffTooltip(event.getItemStack()));
            } else {
                event.getToolTip().add(ForgingSystem.shiftForBuffTooltip());
            }
        }
        var moldStats = ForgingSystem.moldTooltip(event.getItemStack());
        if (!moldStats.getString().isEmpty()) {
            event.getToolTip().add(ForgingSystem.isForgedWeapon(event.getItemStack())
                    ? ForgingSystem.forgedWeaponTooltip(event.getItemStack())
                    : moldStats);
            if (Screen.hasShiftDown()) {
                event.getToolTip().addAll(ForgingSystem.forgedDetailTooltips(event.getItemStack()));
            } else {
                event.getToolTip().add(ForgingSystem.shiftForBuffTooltip());
            }
        }
    }
}
