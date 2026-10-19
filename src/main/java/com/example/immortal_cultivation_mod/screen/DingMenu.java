package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.block.DingBlockEntity;
import com.example.immortal_cultivation_mod.alchemy.AlchemyForgeRecipes;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
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
import net.minecraft.network.chat.Component;

public class DingMenu extends AbstractContainerMenu {

    public static final int IMAGE_WIDTH = 360;
    public static final int IMAGE_HEIGHT = 308;
    public static final int YUJIAN_SLOT_X = 48;
    public static final int YUJIAN_SLOT_Y = 94;
    public static final int RESULT_SLOT_X = 296;
    public static final int RESULT_SLOT_Y = 94;
    public static final int MATERIAL_START_X = 91;
    public static final int MATERIAL_START_Y = 89;
    public static final int MATERIAL_SLOT_SPACING = 18;
    public static final int PLAYER_INV_X = 99;
    public static final int PLAYER_INV_Y = 207;

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
        container.startOpen(inventory.player);

        addSlot(new Slot(container, DingBlockEntity.YUJIAN_SLOT, YUJIAN_SLOT_X, YUJIAN_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !forging() && ForgingSystem.recipe(stack) != null;
            }

            @Override
            public boolean mayPickup(Player player) {
                return !forging();
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                return 1;
            }
        });

        for (int i = 0; i < DingBlockEntity.MATERIAL_COUNT; i++) {
            int materialIndex = i;
            int slotX = MATERIAL_START_X + (i % 10) * MATERIAL_SLOT_SPACING;
            int slotY = MATERIAL_START_Y + (i / 10) * MATERIAL_SLOT_SPACING;

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
                    // Old saves can still contain items beyond the new capacity.
                    // Expose those slots for retrieval, but never accept new inputs.
                    return materialIndex < materialSlotCount() || hasItem();
                }

                @Override
                public int getMaxStackSize(ItemStack stack) {
                    return ForgingSystem.canStackInForgingRecipe(stack)
                            ? super.getMaxStackSize(stack)
                            : 1;
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

    public boolean usesForge(DingBlockEntity forge) {
        return ding == forge;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
        if (ding != null) ding.stopViewing(player);
    }

    public int temperature() { return data.get(0); }
    public int targetTemperature() { return data.get(1); }
    public int progress() { return data.get(2); }
    public int maxProgress() { return Math.max(1, data.get(3)); }
    public boolean forging() { return data.get(4) != 0; }
    public int lowTemp() { return data.get(5); }
    public int highTemp() { return data.get(6); }
    public int fireCost() { return data.get(7); }
    public String craftProficiencyKey() {
        ForgingSystem.ForgingRecipe recipe = ForgingSystem.recipe(container.getItem(DingBlockEntity.YUJIAN_SLOT));
        return AlchemyForgeRecipes.proficiencyKey(recipe == null ? null : recipe.id());
    }

    public Component craftProficiencyLabel(ModAttachments.CultivationData stats) {
        String proficiency = craftProficiencyKey();
        String skill = proficiency.equals(com.example.immortal_cultivation_mod.item.FateAlchemyFormulaItem.PROFICIENCY)
                ? "alchemy" : "forging";
        return Component.translatable("screen.immortal_cultivation_mod.ding." + skill + "_proficiency",
                CultivationMethods.methodProficiencyLevel(stats, proficiency),
                stats == null ? 0 : stats.methodProficiency(proficiency));
    }
    public int materialSlotCount() {
        int syncedCount = data.get(8);
        return syncedCount <= 0
                ? DingBlockEntity.LARGE_FORGE_SLOTS
                : Math.min(DingBlockEntity.GOLDEN_CORE_FORGE_SLOTS, syncedCount);
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
