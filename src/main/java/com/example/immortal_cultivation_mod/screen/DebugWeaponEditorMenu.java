package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.item.WeaponRestrictionSystem;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class DebugWeaponEditorMenu extends AbstractContainerMenu {
    private final SimpleContainer weapon = new SimpleContainer(1);
    private boolean returned;

    public DebugWeaponEditorMenu(int id, Inventory inventory) {
        super(ModScreens.DEBUG_WEAPON_EDITOR.get(), id);
        addSlot(new Slot(weapon, 0, 14, 61) {
            @Override public boolean mayPlace(ItemStack stack) { return WeaponRestrictionSystem.isWeapon(stack); }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + row * 9 + 9, 79 + col * 18, 238 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 79 + col * 18, 298));
    }

    public static boolean authorized(Player player) {
        return player.getMainHandItem().is(ModItems.DEBUG_STAT_EDITOR.get())
                || player.getOffhandItem().is(ModItems.DEBUG_STAT_EDITOR.get());
    }

    public ItemStack weapon() { return weapon.getItem(0); }

    /** Server packet actions: 0 rank delta, 1 restriction delta, 2 remaining durability. */
    public void edit(Player player, int action, int type, int value) {
        if (player.level().isClientSide || player.containerMenu != this || !stillValid(player)) return;
        ItemStack stack = weapon();
        if (!WeaponRestrictionSystem.isWeapon(stack) || com.example.immortal_cultivation_mod.item.WeaponTribulation.active(stack)) return;
        switch (action) {
            // Debug rank edits deliberately bypass breakthrough trials, including completed cycles.
            case 0 -> WeaponRestrictionSystem.debugSetTier(stack, WeaponRestrictionSystem.tier(stack) + Math.clamp(value, -1, 1));
            case 1 -> {
                var types = WeaponRestrictionSystem.RestrictionType.values();
                if (type < 0 || type >= types.length) return;
                WeaponRestrictionSystem.debugAdjustLayers(stack, types[type], value);
            }
            case 2 -> {
                if (!stack.isDamageableItem()) return;
                stack.setDamageValue(stack.getMaxDamage() - Math.clamp(value, 1, stack.getMaxDamage()));
            }
            default -> { return; }
        }
        weapon.setChanged(); broadcastChanges();
    }

    @Override public boolean stillValid(Player player) { return !returned && authorized(player); }

    @Override public void removed(Player player) {
        super.removed(player);
        if (!returned && !player.level().isClientSide) { returned = true; clearContainer(player, weapon); }
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!WeaponRestrictionSystem.isWeapon(stack) || !moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
