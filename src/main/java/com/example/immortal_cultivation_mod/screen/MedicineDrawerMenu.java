package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.block.MedicineCabinetBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Three cabinet slots plus the player's inventory. */
public final class MedicineDrawerMenu extends AbstractContainerMenu {
    public static final int DRAWER_SLOT_COUNT = 3;
    public static final int DRAWER_X = 62;
    public static final int DRAWER_Y = 38;
    public static final int PLAYER_X = 8;
    public static final int PLAYER_Y = 84;

    private final Container drawer;
    private final MedicineCabinetBlockEntity cabinet;
    private final int drawerIndex;

    public MedicineDrawerMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(DRAWER_SLOT_COUNT), null, -1);
    }

    public MedicineDrawerMenu(int containerId, Inventory inventory, Container drawer,
                              MedicineCabinetBlockEntity cabinet, int drawerIndex) {
        super(ModScreens.MEDICINE_DRAWER.get(), containerId);
        checkContainerSize(drawer, DRAWER_SLOT_COUNT);
        this.drawer = drawer;
        this.cabinet = cabinet;
        this.drawerIndex = drawerIndex;
        drawer.startOpen(inventory.player);

        for (int slot = 0; slot < DRAWER_SLOT_COUNT; slot++) {
            addSlot(new Slot(drawer, slot, DRAWER_X + slot * 18, DRAWER_Y));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        PLAYER_X + column * 18, PLAYER_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column,
                    PLAYER_X + column * 18, PLAYER_Y + 58));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return drawer.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < DRAWER_SLOT_COUNT) {
                if (!moveItemStackTo(stack, DRAWER_SLOT_COUNT, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, DRAWER_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        drawer.stopOpen(player);
        if (cabinet != null) {
            cabinet.closeDrawer(drawerIndex);
        }
    }
}

