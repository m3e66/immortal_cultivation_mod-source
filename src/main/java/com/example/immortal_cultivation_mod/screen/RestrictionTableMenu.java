package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.block.RestrictionTableBlockEntity;
import com.example.immortal_cultivation_mod.item.WeaponRestrictionSystem;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class RestrictionTableMenu extends AbstractContainerMenu {
    public static final int IMAGE_WIDTH = 360;
    public static final int IMAGE_HEIGHT = 287;
    public static final int WEAPON_X = 171;
    public static final int WEAPON_Y = 63;
    public static final int PLAYER_INV_X = 99;
    public static final int PLAYER_INV_Y = 205;

    private final Container container;
    private final ContainerData data;
    private final RestrictionTableBlockEntity table;

    public RestrictionTableMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(RestrictionTableBlockEntity.SLOT_COUNT),
                new SimpleContainerData(3), null);
    }

    public RestrictionTableMenu(int containerId, Inventory inventory, RestrictionTableBlockEntity table) {
        this(containerId, inventory, table, new ContainerData() {
            @Override
            public int get(int index) {
                return table.data(index);
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 3;
            }
        }, table);
    }

    private RestrictionTableMenu(int containerId, Inventory inventory, Container container,
                                 ContainerData data, RestrictionTableBlockEntity table) {
        super(ModScreens.RESTRICTION_TABLE.get(), containerId);
        this.container = container;
        this.data = data;
        this.table = table;

        addSlot(new Slot(container, RestrictionTableBlockEntity.WEAPON_SLOT, WEAPON_X, WEAPON_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return WeaponRestrictionSystem.isWeapon(stack) && !active();
            }

            @Override
            public boolean mayPickup(Player player) {
                return !active();
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        PLAYER_INV_X + column * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, PLAYER_INV_X + column * 18, PLAYER_INV_Y + 58));
        }
        addDataSlots(data);
    }

    public void startPuzzle(Player player, int[] additions) {
        if (table != null && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            table.startPuzzle(serverPlayer, additions);
        }
    }

    public void evolve(Player player) {
        if (table != null && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            table.evolveWeapon(serverPlayer);
        }
    }

    public int tier() {
        return data.get(0);
    }

    public int progress() {
        return data.get(1);
    }

    public boolean active() {
        return data.get(2) != 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        if (index == 0) {
            if (active() || !moveItemStackTo(stack, 1, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (WeaponRestrictionSystem.isWeapon(stack)) {
            if (!moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }
}
