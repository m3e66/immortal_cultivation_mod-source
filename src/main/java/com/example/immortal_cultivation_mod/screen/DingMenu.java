package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.block.DingBlockEntity;
import com.example.immortal_cultivation_mod.item.ForgingSystem;
import com.example.immortal_cultivation_mod.item.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class DingMenu extends AbstractContainerMenu {

    public static final int IMAGE_WIDTH = 360;
    public static final int IMAGE_HEIGHT = 288;
    public static final int YUJIAN_SLOT_X = 53;
    public static final int YUJIAN_SLOT_Y = 94;
    public static final int RESULT_SLOT_X = 291;
    public static final int RESULT_SLOT_Y = 94;
    public static final int MATERIAL_START_X = 114;
    public static final int MATERIAL_START_Y = 89;
    public static final int MATERIAL_SLOT_SPACING = 22;
    public static final int PLAYER_INV_X = 99;
    public static final int PLAYER_INV_Y = 187;

    private final Container container;
    private final ContainerData data;
    private final DingBlockEntity ding;

    public DingMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(DingBlockEntity.SLOT_COUNT), new SimpleContainerData(9), null);
    }

    public DingMenu(int containerId, Inventory inventory, DingBlockEntity ding) {
        this(containerId, inventory, ding, new ContainerData() {
            @Override
            public int get(int index) {
                return ding.data(index);
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 9;
            }
        }, ding);
    }

    private DingMenu(int containerId, Inventory inventory, Container container, ContainerData data, DingBlockEntity ding) {
        super(ModScreens.DING.get(), containerId);
        this.container = container;
        this.data = data;
        this.ding = ding;

        addSlot(new Slot(container, DingBlockEntity.YUJIAN_SLOT, YUJIAN_SLOT_X, YUJIAN_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !forging() && ForgingSystem.recipe(stack) != null;
            }

            @Override
            public boolean mayPickup(Player player) {
                return !forging();
            }
        });

        for (int i = 0; i < DingBlockEntity.MATERIAL_COUNT; i++) {
            int materialIndex = i;
            int slotX = MATERIAL_START_X + (i % 5) * MATERIAL_SLOT_SPACING;
            int slotY = MATERIAL_START_Y + (i / 5) * MATERIAL_SLOT_SPACING;

            addSlot(new Slot(container, DingBlockEntity.MATERIAL_START + i, slotX, slotY) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return !forging()
                            && materialIndex < materialSlotCount()
                            && isCurrentRecipeIngredient(stack);
                }

                @Override
                public boolean mayPickup(Player player) {
                    return !forging();
                }

                @Override
                public boolean isActive() {
                    return materialIndex < materialSlotCount();
                }
            });
        }

        addSlot(new Slot(container, DingBlockEntity.RESULT_SLOT, RESULT_SLOT_X, RESULT_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        int invY = PLAYER_INV_Y;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, PLAYER_INV_X + col * 18, invY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, PLAYER_INV_X + col * 18, invY + 58));
        }
        addDataSlots(data);
    }

    public void startForging(Player player) {
        if (ding != null) {
            ding.startForging(player);
        }
    }

    public void heatPulse(Player player) {
        if (ding != null) {
            ding.heatPulse(player);
        }
    }

    public int temperature() { return data.get(0); }
    public int targetTemperature() { return data.get(1); }
    public int progress() { return data.get(2); }
    public int maxProgress() { return Math.max(1, data.get(3)); }
    public boolean forging() { return data.get(4) != 0; }
    public int lowTemp() { return data.get(5); }
    public int highTemp() { return data.get(6); }
    public int fireCost() { return data.get(7); }
    public int materialSlotCount() {
        int syncedCount = data.get(8);
        return syncedCount <= 0
                ? DingBlockEntity.MATERIAL_COUNT
                : Math.min(DingBlockEntity.MATERIAL_COUNT, syncedCount);
    }


    private boolean isCurrentRecipeIngredient(ItemStack stack) {
        ForgingSystem.ForgingRecipe recipe = ForgingSystem.recipe(
                container.getItem(DingBlockEntity.YUJIAN_SLOT));
        return ForgingSystem.isIngredientForRecipe(recipe, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < DingBlockEntity.SLOT_COUNT) {
                if (!moveItemStackTo(stack, DingBlockEntity.SLOT_COUNT, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (ForgingSystem.recipe(stack) != null) {
                if (!moveItemStackTo(stack, DingBlockEntity.YUJIAN_SLOT, DingBlockEntity.YUJIAN_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (isCurrentRecipeIngredient(stack)) {
                if (!moveItemStackTo(stack, DingBlockEntity.MATERIAL_START,
                        DingBlockEntity.MATERIAL_START + materialSlotCount(), false)) {
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
        }
        return result;
    }
}
