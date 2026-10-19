package com.example.immortal_cultivation_mod.event;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.item.CultivationItemRarity;
import com.example.immortal_cultivation_mod.item.ForgingSystem;
import com.example.immortal_cultivation_mod.item.QiInfusedWeapon;
import com.example.immortal_cultivation_mod.item.WeaponRestrictionSystem;
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
        if (event.getItemStack().is(
                com.example.immortal_cultivation_mod.magicritual.item.ModItems.ZHENPAN.get())) {
            return;
        }
        if (com.example.immortal_cultivation_mod.entity.FlyingSwordEntity.isFlyingWeapon(event.getItemStack())) {
            event.getToolTip().add(net.minecraft.network.chat.Component.translatable(
                    "tooltip.immortal_cultivation_mod.sword_control",
                    net.minecraft.network.chat.Component.keybind("key.immortal_cultivation_mod.release_weapon")));
            if (!(event.getItemStack().getItem() instanceof com.example.immortal_cultivation_mod.item.FlyingSwordItem)) {
                event.getToolTip().add(net.minecraft.network.chat.Component.translatable(
                        "tooltip.immortal_cultivation_mod.sword_ride",
                        net.minecraft.network.chat.Component.keybind("key.immortal_cultivation_mod.ride_flying_sword")));
            }
        }
        var binding = com.example.immortal_cultivation_mod.item.WeaponBinding.tooltip(event.getItemStack());
        if (!binding.getString().isEmpty()) event.getToolTip().add(binding);
        boolean higherWeapon = event.getItemStack().getItem() instanceof com.example.immortal_cultivation_mod.item.HigherWeaponItem;
        boolean weapon = WeaponRestrictionSystem.isWeapon(event.getItemStack());
        // Keep the name first, then the same rank/restriction header for every weapon.
        if (weapon) {
            event.getToolTip().addAll(Math.min(1, event.getToolTip().size()),
                    WeaponRestrictionSystem.tooltips(event.getItemStack()));
        }
        if (!higherWeapon && ClientData.debugEyeEnabled()) {
            event.getToolTip().add(CultivationItemRarity.tooltip(event.getItemStack()));
        }
        // Old and new weapons share this exact Qi tooltip path and visibility rule.
        QiInfusedWeapon.appendStoredQiTooltips(event.getItemStack(), event.getToolTip());
        if (higherWeapon) {
            if (event.getItemStack().is(com.example.immortal_cultivation_mod.item.ModItems.SEVEN_STAR_SWORD.get())) {
                event.getToolTip().add(com.example.immortal_cultivation_mod.item.SevenStarPattern.tooltip(event.getItemStack()));
            }
            return;
        }
        if (ForgingSystem.isPurityMaterial(event.getItemStack())) {
            event.getToolTip().add(ForgingSystem.purityTooltip(event.getItemStack()));
            if (ForgingSystem.isForgeMaterial(event.getItemStack())) {
                if (Screen.hasShiftDown()) {
                    event.getToolTip().add(ForgingSystem.materialBuffTooltip(event.getItemStack()));
                } else {
                    event.getToolTip().add(ForgingSystem.shiftForBuffTooltip());
                }
            }
        }
        var moldStats = ForgingSystem.moldTooltip(event.getItemStack());
        if (!moldStats.getString().isEmpty()) {
            event.getToolTip().add(ForgingSystem.isFinishedForgedItem(event.getItemStack())
                    ? ForgingSystem.forgedWeaponTooltip(event.getItemStack())
                    : moldStats);
            if (Screen.hasShiftDown()) {
                event.getToolTip().addAll(ForgingSystem.forgedDetailTooltips(event.getItemStack()));
            } else {
                event.getToolTip().add(ForgingSystem.shiftForBuffTooltip());
            }
        }
        var sharpened = com.example.immortal_cultivation_mod.item.IdentityItems.weaponBuffTooltip(event.getItemStack());
        if (!sharpened.getString().isEmpty()) {
            event.getToolTip().add(sharpened);
        }
        if (!weapon) event.getToolTip().addAll(WeaponRestrictionSystem.tooltips(event.getItemStack()));
    }
}
