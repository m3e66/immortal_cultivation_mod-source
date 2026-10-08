package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.item.QiPouchStorage;
import com.example.immortal_cultivation_mod.item.QiPouchWorld;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Menu mirror for the pouch's complete, real five-block-high pocket plot. */
public class QiPouchMenu extends AbstractContainerMenu
        implements QiPouchStorage.OpenPouchAccess {
    public static final int ROWS = QiPouchWorld.MAX_SIDE;
    public static final int COLUMNS = QiPouchWorld.MAX_SIDE;
    public static final int SLOT_COUNT = QiPouchWorld.MAX_CELLS;
    public static final int CHEST_COLUMNS = 18;
    public static final int CHEST_ROWS = 10;
    public static final int CHEST_SLOT_COUNT = CHEST_COLUMNS * CHEST_ROWS;
    public static final int CHEST_SLOT_START = SLOT_COUNT;
    public static final int PLAYER_SLOT_START =
            CHEST_SLOT_START + CHEST_SLOT_COUNT;
    public static final int CHEST_X = 31;
    public static final int CHEST_Y = 82;
    public static final int PLAYER_X = 394;
    public static final int PLAYER_Y = 111;
    public static final int HOTBAR_Y = 177;
    public static final int INTERACT_BUTTON_BASE = 0;
    public static final int REMOVE_BUTTON_BASE = 5_000;
    public static final int PLACE_BUTTON_BASE = 10_000;
    public static final int PLACE_DIRECTION_COUNT = 4;
    public static final int CYCLE_DIRECTION_BUTTON_BASE = 30_000;
    public static final int BUILD_TAB_BUTTON = 40_000;
    public static final int CHEST_TAB_BUTTON = 40_001;
    public static final int CHEST_PREVIOUS_BUTTON = 40_002;
    public static final int CHEST_NEXT_BUTTON = 40_003;
    private static final int DATA_SIDE = 0;
    private static final int DATA_TAB = 1;
    private static final int DATA_CHEST_PAGE = 2;
    private static final int DATA_CHEST_PAGES = 3;
    private static final int DATA_CHEST_COUNT = 4;
    private static final int DATA_CHEST_VISIBLE_SLOTS = 5;
    private static final int TAB_BUILD = 0;
    private static final int TAB_CHESTS = 1;
    private static final int HIDDEN_FLOOR_SLOT_X = -10_000;
    private static final int HIDDEN_FLOOR_SLOT_Y = -10_000;

    private final ItemStack pouchStack;
    private final QiPouchContainer pouchContainer;
    private final PouchChestContainer chestContainer;
    private final ContainerData viewData;
    private int refreshTicker;

    public QiPouchMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, findPouch(inventory.player));
    }

    public QiPouchMenu(
            int containerId, Inventory inventory, ItemStack pouchStack) {
        super(ModScreens.QI_POUCH.get(), containerId);
        this.pouchStack = pouchStack;
        this.viewData = new SimpleContainerData(6);
        this.viewData.set(DATA_SIDE, QiPouchWorld.side(pouchStack));
        this.viewData.set(DATA_TAB, QiPouchStorage.opensOnChestTab(pouchStack)
                ? TAB_CHESTS : TAB_BUILD);
        this.pouchContainer = new QiPouchContainer(
                pouchStack, inventory.player, viewData);
        this.chestContainer = new PouchChestContainer(
                pouchStack, inventory.player, viewData);

        for (int layer = 0; layer < QiPouchWorld.HEIGHT; layer++) {
            for (int row = 0; row < ROWS; row++) {
                for (int column = 0; column < COLUMNS; column++) {
                    int cell = volumeCell(layer, row, column);
                    addSlot(new PouchSlot(
                            pouchContainer, cell,
                            HIDDEN_FLOOR_SLOT_X - column * 18,
                            HIDDEN_FLOOR_SLOT_Y - (row + layer * ROWS) * 18));
                }
            }
        }
        for (int row = 0; row < CHEST_ROWS; row++) {
            for (int column = 0; column < CHEST_COLUMNS; column++) {
                int chestSlot = column + row * CHEST_COLUMNS;
                addSlot(new PouchChestSlot(
                        chestContainer, chestSlot,
                        CHEST_X + column * 18,
                        CHEST_Y + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new PouchInventorySlot(
                        inventory, column + row * 9 + 9,
                        PLAYER_X + column * 18,
                        PLAYER_Y + row * 18, pouchStack));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new PouchInventorySlot(
                    inventory, column,
                    PLAYER_X + column * 18, HOTBAR_Y, pouchStack));
        }
        addDataSlots(viewData);
        if (inventory.player instanceof ServerPlayer serverPlayer) {
            com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.discover(
                    serverPlayer,
                    com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent.STORAGE_EQUIPMENT);
            com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.record(
                    serverPlayer,
                    com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent.OPEN_POUCH);
        }
    }

    public int side() {
        return Math.max(2, Math.min(
                QiPouchWorld.MAX_SIDE, viewData.get(DATA_SIDE)));
    }

    public boolean isChestTab() {
        return viewData.get(DATA_TAB) == TAB_CHESTS;
    }

    public int chestPage() {
        return Math.max(0, viewData.get(DATA_CHEST_PAGE));
    }

    public int chestPages() {
        return Math.max(1, viewData.get(DATA_CHEST_PAGES));
    }

    public int chestCount() {
        return Math.max(0, viewData.get(DATA_CHEST_COUNT));
    }

    public int chestVisibleSlotCount() {
        return Math.max(0, viewData.get(DATA_CHEST_VISIBLE_SLOTS));
    }

    /** Applies one search over every slot in every linked chest. */
    public void setChestSearch(String query) {
        chestContainer.setSearch(query);
        broadcastChanges();
    }

    public static int volumeCell(int layer, int row, int column) {
        return layer * QiPouchWorld.LAYER_SLOTS
                + row * COLUMNS + column;
    }

    public static int layerOf(int volumeCell) {
        return volumeCell / QiPouchWorld.LAYER_SLOTS;
    }

    public static int layerCellOf(int volumeCell) {
        return Math.floorMod(volumeCell, QiPouchWorld.LAYER_SLOTS);
    }

    @Override
    public boolean stillValid(Player player) {
        return pouchContainer.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        if (index < SLOT_COUNT) {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return ItemStack.EMPTY;
            }
            ItemStack removed = pouchContainer.removeWorldItem(index);
            if (removed.isEmpty()) return ItemStack.EMPTY;
            ItemStack result = removed.copy();
            if (!moveItemStackTo(
                    removed, PLAYER_SLOT_START, slots.size(), true)) {
                if (!pouchContainer.restoreWorldItem(index, result)) {
                    // A failed rollback must never delete the item. Return it
                    // to the player (or drop it if the inventory is full).
                    QiPouchStorage.giveOrDrop(serverPlayer, result);
                }
                return ItemStack.EMPTY;
            }
            if (!removed.isEmpty()) {
                QiPouchStorage.giveOrDrop(serverPlayer, removed);
            }
            return result;
        }

        ItemStack source = slot.getItem();
        ItemStack result = source.copy();
        if (index < PLAYER_SLOT_START) {
            if (!isChestTab() || !moveItemStackTo(
                    source, PLAYER_SLOT_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            if (source.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            return result;
        }

        if (isChestTab()) {
            if (!moveItemStackTo(
                    source, CHEST_SLOT_START,
                    PLAYER_SLOT_START, false)) {
                return ItemStack.EMPTY;
            }
            if (source.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            return result;
        }

        if (!QiPouchStorage.isFloorItem(source)) return ItemStack.EMPTY;
        if (!pouchContainer.placeFirstAvailable(source)) {
            return ItemStack.EMPTY;
        }
        // Placement succeeds before the source is consumed. Vanilla's generic
        // slot mover consumes first and could therefore erase an item whenever
        // a real-world placement was rejected.
        source.shrink(1);
        if (source.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer)) return false;
        if (id == BUILD_TAB_BUTTON) {
            viewData.set(DATA_TAB, TAB_BUILD);
            QiPouchStorage.setOpensOnChestTab(pouchStack, false);
            broadcastChanges();
            return true;
        }
        if (id == CHEST_TAB_BUTTON) {
            viewData.set(DATA_TAB, TAB_CHESTS);
            QiPouchStorage.setOpensOnChestTab(pouchStack, true);
            chestContainer.refreshPage();
            broadcastChanges();
            return true;
        }
        if (id == CHEST_PREVIOUS_BUTTON && isChestTab()) {
            chestContainer.changePage(-1);
            broadcastChanges();
            return true;
        }
        if (id == CHEST_NEXT_BUTTON && isChestTab()) {
            chestContainer.changePage(1);
            broadcastChanges();
            return true;
        }
        if (id >= PLACE_BUTTON_BASE
                && id < PLACE_BUTTON_BASE
                        + SLOT_COUNT * PLACE_DIRECTION_COUNT) {
            int placementData = id - PLACE_BUTTON_BASE;
            int cell = placementData % SLOT_COUNT;
            Direction placerDirection = decodePlacementDirection(
                    placementData / SLOT_COUNT);
            boolean placed = placeCarriedInCell(cell, placerDirection);
            if (placed) {
                com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.record(
                        serverPlayer,
                        com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent.POUCH_PLACE);
            }
            return placed;
        }
        if (id >= REMOVE_BUTTON_BASE
                && id < REMOVE_BUTTON_BASE + SLOT_COUNT) {
            boolean removed = removeCell(serverPlayer, id - REMOVE_BUTTON_BASE);
            if (removed) {
                com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.record(
                        serverPlayer,
                        com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent.POUCH_REMOVE);
            }
            return removed;
        }
        if (id >= CYCLE_DIRECTION_BUTTON_BASE
                && id < CYCLE_DIRECTION_BUTTON_BASE + SLOT_COUNT) {
            int cell = id - CYCLE_DIRECTION_BUTTON_BASE;
            boolean changed = QiPouchWorld.cycleDirection(
                    pouchStack, serverPlayer,
                    layerOf(cell), layerCellOf(cell));
            if (changed) {
                pouchContainer.refreshFromWorld();
                broadcastChanges();
            }
            return changed;
        }
        if (id >= INTERACT_BUTTON_BASE
                && id < INTERACT_BUTTON_BASE + SLOT_COUNT) {
            int cell = id - INTERACT_BUTTON_BASE;
            return QiPouchWorld.openCell(
                    pouchStack, serverPlayer,
                    layerOf(cell), layerCellOf(cell));
        }
        return false;
    }

    private boolean placeCarriedInCell(
            int cell, Direction placerDirection) {
        ItemStack carried = getCarried();
        if (carried.isEmpty() || !QiPouchStorage.isFloorItem(carried)) {
            return false;
        }
        ItemStack placed = pouchContainer.placeWorldItem(
                cell, carried, placerDirection);
        if (placed.isEmpty()) return false;
        carried.shrink(1);
        setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
        return true;
    }

    /** Matches Minecraft's four horizontal player-yaw quadrants. */
    private static Direction decodePlacementDirection(int index) {
        return switch (Math.floorMod(index, PLACE_DIRECTION_COUNT)) {
            case 0 -> Direction.SOUTH;
            case 1 -> Direction.WEST;
            case 2 -> Direction.NORTH;
            default -> Direction.EAST;
        };
    }

    private boolean removeCell(ServerPlayer player, int cell) {
        ItemStack removed = pouchContainer.removeWorldItem(cell);
        if (removed.isEmpty()) return false;
        QiPouchStorage.giveOrDrop(player, removed);
        return true;
    }

    @Override
    public void broadcastChanges() {
        // Pistons and other short redstone transitions finish in only a few
        // ticks. Mirror them promptly so the miniature world visibly moves.
        if (++refreshTicker % 2 == 0) {
            pouchContainer.refreshFromWorld();
        }
        if (refreshTicker % 10 == 0) {
            chestContainer.refreshPage();
        }
        super.broadcastChanges();
    }

    @Override
    public UUID pouchId() {
        return QiPouchStorage.pouchId(pouchStack);
    }

    @Override
    public boolean insertRecalled(ItemStack artifact, ServerPlayer owner) {
        boolean inserted = QiPouchWorld.insertArtifact(
                pouchStack, owner, artifact);
        if (inserted) pouchContainer.refreshFromWorld();
        return inserted;
    }

    public boolean isFloorSlot(Slot slot) {
        return slot != null && slot.index >= 0
                && slot.index < SLOT_COUNT;
    }

    public boolean isActiveCell(int cell) {
        return pouchContainer.validCell(cell);
    }

    private static ItemStack findPouch(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.is(ModItems.QI_POUCH.get())) return mainHand;
        ItemStack offHand = player.getOffhandItem();
        if (offHand.is(ModItems.QI_POUCH.get())) return offHand;
        for (ItemStack candidate : player.getInventory().items) {
            if (candidate.is(ModItems.QI_POUCH.get())) return candidate;
        }
        return ItemStack.EMPTY;
    }

    private static final class PouchSlot extends Slot {
        private final QiPouchContainer pouch;

        PouchSlot(QiPouchContainer container, int slot, int x, int y) {
            super(container, slot, x, y);
            this.pouch = container;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return pouch.validCell(index) && getItem().isEmpty()
                    && QiPouchStorage.isFloorItem(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return 1;
        }
    }

    private static final class PouchChestSlot extends Slot {
        private final PouchChestContainer chests;
        private final int chestSlot;

        PouchChestSlot(
                PouchChestContainer container, int slot, int x, int y) {
            super(container, slot, x, y);
            this.chests = container;
            this.chestSlot = slot;
        }

        @Override
        public boolean isActive() {
            return chests.isSlotActive(chestSlot);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isActive() && chests.canPlaceItem(chestSlot, stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            return isActive() && super.mayPickup(player);
        }
    }

    private static final class PouchInventorySlot extends Slot {
        private final ItemStack openedPouch;

        PouchInventorySlot(
                Inventory inventory, int slot, int x, int y,
                ItemStack openedPouch) {
            super(inventory, slot, x, y);
            this.openedPouch = openedPouch;
        }

        @Override
        public boolean mayPickup(Player player) {
            return !isOpenedPouch(getItem()) && super.mayPickup(player);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !isOpenedPouch(stack) && super.mayPlace(stack);
        }

        private boolean isOpenedPouch(ItemStack stack) {
            if (!stack.is(ModItems.QI_POUCH.get())) return false;
            UUID openedId = QiPouchStorage.pouchId(openedPouch);
            UUID candidateId = QiPouchStorage.pouchId(stack);
            return openedPouch == stack
                    || openedId != null && openedId.equals(candidateId);
        }
    }

    /**
     * One continuous, searchable window over every live chest slot in the
     * pouch. Nothing is copied into separate storage: every click delegates
     * back to the corresponding real chest inventory.
     */
    private static final class PouchChestContainer extends SimpleContainer {
        private final ItemStack pouch;
        private final Player player;
        private final ContainerData viewData;
        private List<QiPouchWorld.ChestInventory> chests = List.of();
        private List<ChestBinding> bindings = List.of();
        private String searchQuery = "";
        private boolean loading;

        PouchChestContainer(
                ItemStack pouch, Player player, ContainerData viewData) {
            super(CHEST_SLOT_COUNT);
            this.pouch = pouch;
            this.player = player;
            this.viewData = viewData;
            refreshPage();
        }

        void refreshPage() {
            if (!(player instanceof ServerPlayer serverPlayer)
                    || pouch.isEmpty()) return;
            chests = QiPouchWorld.chestInventories(pouch, serverPlayer);
            bindings = buildBindings();
            int pages = Math.max(1,
                    (bindings.size() + CHEST_SLOT_COUNT - 1)
                            / CHEST_SLOT_COUNT);
            int page = Math.max(0, Math.min(
                    viewData.get(DATA_CHEST_PAGE), pages - 1));
            viewData.set(DATA_CHEST_PAGE, page);
            viewData.set(DATA_CHEST_PAGES, pages);
            viewData.set(DATA_CHEST_COUNT, chests.size());
            viewData.set(DATA_CHEST_VISIBLE_SLOTS, bindings.size());

            loading = true;
            for (int slot = 0; slot < CHEST_SLOT_COUNT; slot++) {
                ChestBinding binding = binding(slot);
                ItemStack next = binding == null
                        ? ItemStack.EMPTY
                        : binding.inventory().getItem(binding.slot());
                super.setItem(slot, next);
            }
            loading = false;
        }

        void setSearch(String query) {
            String next = query == null ? "" : query.strip();
            if (next.length() > 64) next = next.substring(0, 64);
            next = next.toLowerCase(Locale.ROOT);
            if (next.equals(searchQuery)) return;
            searchQuery = next;
            viewData.set(DATA_CHEST_PAGE, 0);
            refreshPage();
        }

        void changePage(int amount) {
            if (!(player instanceof ServerPlayer) || amount == 0) return;
            int page = Math.max(0, Math.min(
                    viewData.get(DATA_CHEST_PAGE) + amount,
                    Math.max(0, viewData.get(DATA_CHEST_PAGES) - 1)));
            if (page == viewData.get(DATA_CHEST_PAGE)) return;
            viewData.set(DATA_CHEST_PAGE, page);
            refreshPage();
        }

        boolean isSlotActive(int slot) {
            if (viewData.get(DATA_TAB) != TAB_CHESTS
                    || slot < 0 || slot >= CHEST_SLOT_COUNT) return false;
            int absoluteSlot = viewData.get(DATA_CHEST_PAGE)
                    * CHEST_SLOT_COUNT + slot;
            return absoluteSlot < viewData.get(DATA_CHEST_VISIBLE_SLOTS);
        }

        @Override
        public boolean canPlaceItem(int index, ItemStack stack) {
            if (!isSlotActive(index) || isOpenedPouch(stack)) return false;
            if (!(player instanceof ServerPlayer)) return true;
            ChestBinding binding = binding(index);
            return binding != null
                    && binding.inventory().canPlaceItem(
                    binding.slot(), stack);
        }

        @Override
        public void setItem(int index, ItemStack stack) {
            if (loading || !(player instanceof ServerPlayer)) {
                super.setItem(index, stack);
                return;
            }
            ChestBinding binding = binding(index);
            if (binding == null || isOpenedPouch(stack)) return;
            binding.inventory().setItem(binding.slot(), stack);
            binding.inventory().setChanged();
            loading = true;
            super.setItem(index,
                    binding.inventory().getItem(binding.slot()));
            loading = false;
        }

        @Override
        public ItemStack removeItem(int index, int count) {
            if (!(player instanceof ServerPlayer)) {
                return super.removeItem(index, count);
            }
            ChestBinding binding = binding(index);
            if (binding == null || count <= 0) return ItemStack.EMPTY;
            ItemStack removed = binding.inventory().removeItem(
                    binding.slot(), count);
            binding.inventory().setChanged();
            syncSlot(index, binding);
            return removed;
        }

        @Override
        public ItemStack removeItemNoUpdate(int index) {
            if (!(player instanceof ServerPlayer)) {
                return super.removeItemNoUpdate(index);
            }
            ChestBinding binding = binding(index);
            if (binding == null) return ItemStack.EMPTY;
            ItemStack removed = binding.inventory().removeItemNoUpdate(
                    binding.slot());
            binding.inventory().setChanged();
            syncSlot(index, binding);
            return removed;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (loading || !(player instanceof ServerPlayer)) return;
            int first = viewData.get(DATA_CHEST_PAGE) * CHEST_SLOT_COUNT;
            int last = Math.min(first + CHEST_SLOT_COUNT, bindings.size());
            for (int index = first; index < last; index++) {
                bindings.get(index).inventory().setChanged();
            }
        }

        private void syncSlot(int index, ChestBinding binding) {
            loading = true;
            super.setItem(index,
                    binding.inventory().getItem(binding.slot()));
            loading = false;
        }

        private ChestBinding binding(int visibleSlot) {
            if (visibleSlot < 0 || visibleSlot >= CHEST_SLOT_COUNT) {
                return null;
            }
            int absoluteSlot = viewData.get(DATA_CHEST_PAGE)
                    * CHEST_SLOT_COUNT + visibleSlot;
            return absoluteSlot >= 0 && absoluteSlot < bindings.size()
                    ? bindings.get(absoluteSlot) : null;
        }

        private List<ChestBinding> buildBindings() {
            List<ChestBinding> next = new ArrayList<>();
            for (QiPouchWorld.ChestInventory chest : chests) {
                Container inventory = chest.container();
                int slots = Math.min(27, inventory.getContainerSize());
                for (int slot = 0; slot < slots; slot++) {
                    ItemStack stack = inventory.getItem(slot);
                    if (searchQuery.isEmpty() || matchesSearch(stack)) {
                        next.add(new ChestBinding(inventory, slot));
                    }
                }
            }
            return List.copyOf(next);
        }

        private boolean matchesSearch(ItemStack stack) {
            if (stack.isEmpty()) return false;
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem())
                    .toString().toLowerCase(Locale.ROOT);
            String name = stack.getHoverName().getString()
                    .toLowerCase(Locale.ROOT);
            String haystack = name + " " + itemId + " "
                    + itemId.replace('_', ' ');
            for (String term : searchQuery.split("\\s+")) {
                if (!term.isEmpty() && !haystack.contains(term)) return false;
            }
            return true;
        }

        private boolean isOpenedPouch(ItemStack stack) {
            if (!stack.is(ModItems.QI_POUCH.get())) return false;
            UUID openedId = QiPouchStorage.pouchId(pouch);
            UUID candidateId = QiPouchStorage.pouchId(stack);
            return pouch == stack
                    || openedId != null && openedId.equals(candidateId);
        }

        private record ChestBinding(Container inventory, int slot) {
        }
    }

    public static final class QiPouchContainer extends SimpleContainer {
        private final ItemStack pouch;
        private final Player player;
        private final ContainerData viewData;
        private boolean loading;

        QiPouchContainer(
                ItemStack pouch, Player player, ContainerData viewData) {
            super(SLOT_COUNT);
            this.pouch = pouch;
            this.player = player;
            this.viewData = viewData;
            refreshFromWorld();
        }

        int side() {
            return Math.max(2, Math.min(
                    QiPouchWorld.MAX_SIDE, viewData.get(DATA_SIDE)));
        }

        boolean validCell(int index) {
            if (index < 0 || index >= SLOT_COUNT) return false;
            int layer = layerOf(index);
            int layerCell = layerCellOf(index);
            int row = layerCell / COLUMNS;
            int column = layerCell % COLUMNS;
            return layer >= 0 && layer < QiPouchWorld.HEIGHT
                    && row < side() && column < side();
        }

        void refreshFromWorld() {
            if (!(player instanceof ServerPlayer serverPlayer)
                    || pouch.isEmpty()) return;
            NonNullList<ItemStack> loaded = QiPouchWorld.loadVolume(
                    pouch, serverPlayer);
            loading = true;
            for (int cell = 0; cell < SLOT_COUNT; cell++) {
                ItemStack next = loaded.get(cell);
                if (!ItemStack.matches(getItems().get(cell), next)) {
                    super.setItem(cell, next);
                }
            }
            loading = false;
        }

        @Override
        public void setItem(int index, ItemStack stack) {
            if (loading) {
                super.setItem(index, stack);
                return;
            }
            // Clientbound menu synchronization also reaches this container through
            // setItem(). The client must mirror that authoritative stack instead of
            // rejecting it simply because its player is a LocalPlayer.
            if (!(player instanceof ServerPlayer)) {
                if (index >= 0 && index < SLOT_COUNT) {
                    loading = true;
                    super.setItem(index, stack);
                    loading = false;
                }
                return;
            }
            if (!validCell(index)) return;
            if (stack.isEmpty()) {
                removeWorldItem(index);
                return;
            }
            if (!getItem(index).isEmpty()
                    || !QiPouchStorage.isFloorItem(stack)) return;
            placeWorldItem(index, stack);
        }

        ItemStack placeWorldItem(int index, ItemStack source) {
            Direction direction = player.getDirection();
            return placeWorldItem(index, source, direction);
        }

        ItemStack placeWorldItem(
                int index, ItemStack source, Direction placerDirection) {
            if (!(player instanceof ServerPlayer serverPlayer)
                    || !validCell(index) || source.isEmpty()
                    || !getItem(index).isEmpty()
                    || !QiPouchStorage.isFloorItem(source)) {
                return ItemStack.EMPTY;
            }
            ItemStack placed = QiPouchWorld.placeCell(
                    pouch, serverPlayer,
                    layerOf(index), layerCellOf(index), source,
                    placerDirection);
            if (placed.isEmpty()) return ItemStack.EMPTY;
            loading = true;
            super.setItem(index, placed);
            loading = false;
            // Chest placement may also change the neighboring half from SINGLE
            // to LEFT/RIGHT. Mirror the complete real-world state immediately so
            // the client renders one joined double chest on the same click.
            refreshFromWorld();
            return placed;
        }

        boolean placeFirstAvailable(ItemStack source) {
            if (!(player instanceof ServerPlayer) || source.isEmpty()
                    || !QiPouchStorage.isFloorItem(source)) return false;
            for (int index = 0; index < SLOT_COUNT; index++) {
                if (!validCell(index) || !getItem(index).isEmpty()) continue;
                if (!placeWorldItem(index, source).isEmpty()) return true;
            }
            return false;
        }

        @Override
        public ItemStack removeItemNoUpdate(int index) {
            return removeWorldItem(index);
        }

        @Override
        public ItemStack removeItem(int index, int count) {
            return count <= 0
                    ? ItemStack.EMPTY : removeWorldItem(index);
        }

        ItemStack removeWorldItem(int index) {
            if (!(player instanceof ServerPlayer serverPlayer)
                    || !validCell(index) || getItem(index).isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack removed = QiPouchWorld.removeCell(
                    pouch, serverPlayer,
                    layerOf(index), layerCellOf(index));
            if (!removed.isEmpty()) {
                loading = true;
                super.setItem(index, ItemStack.EMPTY);
                loading = false;
            }
            return removed;
        }

        boolean restoreWorldItem(int index, ItemStack stack) {
            if (!(player instanceof ServerPlayer serverPlayer)
                    || stack.isEmpty() || !validCell(index)) return false;
            ItemStack placed = QiPouchWorld.placeCell(
                    pouch, serverPlayer,
                    layerOf(index), layerCellOf(index), stack);
            if (!placed.isEmpty()) {
                loading = true;
                super.setItem(index, placed);
                loading = false;
                return true;
            }
            return false;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (!loading && player instanceof ServerPlayer) {
                refreshFromWorld();
            }
        }

        @Override
        public boolean stillValid(Player checkingPlayer) {
            if (pouch.isEmpty() || !pouch.is(ModItems.QI_POUCH.get())
                    || !checkingPlayer.isAlive()) return false;
            return !(checkingPlayer instanceof ServerPlayer serverPlayer)
                    || QiPouchStorage.playerHasPouch(
                    serverPlayer, pouchId());
        }

        UUID pouchId() {
            return QiPouchStorage.pouchId(pouch);
        }
    }
}
