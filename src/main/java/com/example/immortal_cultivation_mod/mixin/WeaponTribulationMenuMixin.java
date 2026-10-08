package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.item.WeaponTribulation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Inventory rearrangement is allowed; hiding trial weapons in containers or cloning is not. */
@Mixin(AbstractContainerMenu.class)
public abstract class WeaponTribulationMenuMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void protectTrialItem(int slotId, int button, ClickType click, Player player, CallbackInfo ci) {
        var menu = (AbstractContainerMenu)(Object)this;
        boolean carriedActive = WeaponTribulation.active(menu.getCarried());
        boolean swappedActive = click == ClickType.SWAP && button >= 0 && button < player.getInventory().getContainerSize()
                && WeaponTribulation.active(player.getInventory().getItem(button));
        boolean active = carriedActive || swappedActive;
        if (slotId >= 0 && slotId < menu.slots.size()) active |= WeaponTribulation.active(menu.getSlot(slotId).getItem());
        if (active && (click == ClickType.CLONE || !(menu instanceof InventoryMenu))) {
            ci.cancel();
        } else if (menu instanceof InventoryMenu && slotId >= 0 && slotId < menu.slots.size()
                && menu.getSlot(slotId).container != player.getInventory()
                && (swappedActive || carriedActive && (click == ClickType.PICKUP || click == ClickType.QUICK_CRAFT))) {
            // Crafting inputs are outside tracked inventory custody. Retrieval remains allowed.
            ci.cancel();
        }
    }
}
