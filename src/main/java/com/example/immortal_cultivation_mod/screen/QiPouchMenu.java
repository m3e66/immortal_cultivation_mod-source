package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.item.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class QiPouchMenu extends AbstractContainerMenu {
    public static final int ROWS = 12;
    public static final int COLUMNS = 9;
    public static final int SLOT_COUNT = ROWS * COLUMNS;
    private static final String ITEMS_TAG = "Items";
    private final ItemStack pouchStack;
    private final QiPouchContainer pouchContainer;

    public QiPouchMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, findPouch(inventory.player));
    }

    public QiPouchMenu(int containerId, Inventory inventory, ItemStack pouchStack) {
        super(ModScreens.QI_POUCH.get(), containerId);
        this.pouchStack = pouchStack;
        this.pouchContainer = new QiPouchContainer(pouchStack, inventory.player);

        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                addSlot(new PouchSlot(pouchContainer, row * COLUMNS + column, 8 + column * 18, 18 + row * 18));
            }
        }

        int playerInventoryY = 32 + ROWS * 18;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, playerInventoryY + row * 18));
            }
        }

        int hotbarY = playerInventoryY + 58;
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, hotbarY));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return !pouchStack.isEmpty() && pouchStack.is(ModItems.QI_POUCH.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < SLOT_COUNT) {
                if (!moveItemStackTo(stack, SLOT_COUNT, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!stack.is(ModItems.QI_POUCH.get()) && !moveItemStackTo(stack, 0, SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        pouchContainer.save();
    }

    private static ItemStack findPouch(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.is(ModItems.QI_POUCH.get())) {
            return mainHand;
        }
        ItemStack offHand = player.getOffhandItem();
        if (offHand.is(ModItems.QI_POUCH.get())) {
            return offHand;
        }
        return ItemStack.EMPTY;
    }

    private static class PouchSlot extends Slot {
        PouchSlot(SimpleContainer container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !stack.is(ModItems.QI_POUCH.get());
        }
    }

    private static class QiPouchContainer extends SimpleContainer {
        private final ItemStack stack;
        private final Player player;

        QiPouchContainer(ItemStack stack, Player player) {
            super(SLOT_COUNT);
            this.stack = stack;
            this.player = player;
            load();
        }

        @Override
        public void setChanged() {
            super.setChanged();
            save();
        }

        void load() {
            if (stack.isEmpty()) {
                return;
            }
            NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (tag.contains(ITEMS_TAG)) {
                ContainerHelper.loadAllItems(tag, items, player.registryAccess());
            }
            for (int i = 0; i < SLOT_COUNT; i++) {
                setItem(i, items.get(i));
            }
        }

        void save() {
            if (stack.isEmpty()) {
                return;
            }
            NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
            for (int i = 0; i < SLOT_COUNT; i++) {
                items.set(i, getItem(i));
            }
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            ContainerHelper.saveAllItems(tag, items, player.registryAccess());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }
}
