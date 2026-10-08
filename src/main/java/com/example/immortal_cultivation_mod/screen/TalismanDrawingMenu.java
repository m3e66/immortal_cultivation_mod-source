package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.block.TalismanDrawingTableBlockEntity;
import com.example.immortal_cultivation_mod.item.TalismanFormulaItem;
import com.example.immortal_cultivation_mod.item.TalismanMaterialItem;
import com.example.immortal_cultivation_mod.item.TalismanRecipes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class TalismanDrawingMenu extends AbstractContainerMenu {
    public static final int IMAGE_WIDTH = 480;
    public static final int IMAGE_HEIGHT = 344;
    public static final int FORMULA_X = 402;
    public static final int FORMULA_Y = 72;
    public static final int PAPER_X = 366;
    public static final int PAPER_Y = 104;
    public static final int INK_X = 438;
    public static final int INK_Y = 104;
    public static final int OUTPUT_X = 402;
    public static final int OUTPUT_Y = 140;
    public static final int PLAYER_INV_X = 159;
    public static final int PLAYER_INV_Y = 258;

    private final Container container;
    private final ContainerData data;
    private final TalismanDrawingTableBlockEntity table;

    public TalismanDrawingMenu(int containerId, Inventory inventory) {
        this(containerId, inventory,
                new SimpleContainer(TalismanDrawingTableBlockEntity.SLOT_COUNT),
                new SimpleContainerData(4), null);
    }

    public TalismanDrawingMenu(int containerId, Inventory inventory,
                               TalismanDrawingTableBlockEntity table) {
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
                return 4;
            }
        }, table);
    }

    private TalismanDrawingMenu(int containerId, Inventory inventory, Container container,
                                ContainerData data, TalismanDrawingTableBlockEntity table) {
        super(ModScreens.TALISMAN_DRAWING.get(), containerId);
        this.container = container;
        this.data = data;
        this.table = table;

        addSlot(new Slot(container, TalismanDrawingTableBlockEntity.FORMULA_SLOT,
                FORMULA_X, FORMULA_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof TalismanFormulaItem
                        && TalismanRecipes.byId(TalismanFormulaItem.recipeId(stack)) != null;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(container, TalismanDrawingTableBlockEntity.PAPER_SLOT, PAPER_X, PAPER_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return TalismanMaterialItem.isPaper(stack);
            }
        });
        addSlot(new Slot(container, TalismanDrawingTableBlockEntity.INK_SLOT, INK_X, INK_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return TalismanMaterialItem.isInk(stack);
            }
        });
        addSlot(new Slot(container, TalismanDrawingTableBlockEntity.OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public int getMaxStackSize() {
                return TalismanDrawingTableBlockEntity.OUTPUT_STACK_LIMIT;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        PLAYER_INV_X + column * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column,
                    PLAYER_INV_X + column * 18, PLAYER_INV_Y + 58));
        }
        addDataSlots(data);
    }

    public String formulaId() {
        return TalismanFormulaItem.recipeId(slots.getFirst().getItem());
    }

    public void submit(Player player, String recipeId, List<Integer> trace, int qiControl,
                       boolean instant) {
        if (table != null && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            table.draw(serverPlayer, recipeId, trace, qiControl, instant);
        }
    }

    public void beginDrawing(net.minecraft.server.level.ServerPlayer player, String recipeId) {
        if (table != null) table.beginDrawing(player, recipeId);
    }

    public void cancelDrawing(net.minecraft.server.level.ServerPlayer player) {
        if (table != null) table.cancelDrawing(player);
    }

    public int resultNonce() {
        return data.get(0);
    }

    public int result() {
        return data.get(1);
    }

    public int accuracy() {
        return data.get(2);
    }

    public int quality() {
        return data.get(3);
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (table != null && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            table.cancelDrawing(serverPlayer);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 4) {
            if (!moveItemStackTo(stack, 4, slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof TalismanFormulaItem) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (TalismanMaterialItem.isPaper(stack)) {
            if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
        } else if (TalismanMaterialItem.isInk(stack)) {
            if (!moveItemStackTo(stack, 2, 3, false)) return ItemStack.EMPTY;
        } else if (index < 31) {
            if (!moveItemStackTo(stack, 31, 40, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 4, 31, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
