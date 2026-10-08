package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.FlyingSwordEntity;
import com.example.immortal_cultivation_mod.screen.QiPouchMenu;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The server-side, fully ticking miniature world owned by a qi pouch.
 *
 * <p>Each pouch receives a stable, widely separated plot in a dedicated flat
 * dimension.  The GUI mirrors the complete five-block-high volume, while all
 * placed blocks, block entities, scheduled ticks, redstone, recipes, and
 * modded menus continue to use the normal Minecraft implementations.</p>
 */
public final class QiPouchWorld {
    public static final int MAX_SIDE = 30;
    public static final int HEIGHT = 5;
    public static final int LAYER_SLOTS = MAX_SIDE * MAX_SIDE;
    public static final int MAX_CELLS = LAYER_SLOTS * HEIGHT;

    private static final int FLOOR_Y = 5;
    private static final int PLOT_PITCH = 64;
    private static final int PLOT_COORD_MASK = (1 << 19) - 1;
    private static final int PLOT_COORD_CENTER = 1 << 18;
    private static final String MIGRATED_TAG =
            "immortal_cultivation_mod:qi_pouch_real_world_v1";
    private static final String ARTIFACTS_TAG =
            "immortal_cultivation_mod:qi_pouch_artifacts";
    private static final String ARTIFACT_CELL_TAG = "Cell";
    private static final String ARTIFACT_STACK_TAG = "Stack";
    private static final String LEGACY_ITEMS_TAG = "Items";
    private static final String DISPLAY_PISTON_HEAD_TAG =
            "immortal_cultivation_mod:qi_pouch_piston_head";

    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "qi_pouch"));

    private static final Map<UUID, RemoteSession> REMOTE_SESSIONS =
            new ConcurrentHashMap<>();

    private QiPouchWorld() {
    }

    public static int sideForPurity(int purity) {
        int value = Math.max(1, Math.min(100, purity));
        if (value <= 10) return 2;
        if (value <= 20) return 3;
        if (value <= 30) return 4;
        if (value <= 40) return 5;
        if (value <= 50) return 6;
        if (value <= 60) return 7;
        if (value <= 70) return 8;
        if (value <= 80) return 10;
        if (value <= 90) return 12;
        if (value <= 95) return 15;
        if (value <= 99) return 20;
        return 30;
    }

    public static int side(ItemStack pouch) {
        return sideForPurity(QiPouchStorage.purity(pouch));
    }

    /**
     * Returns every real chest block in the pouch's active build volume.
     * Double chests intentionally contribute both 27-slot halves, matching
     * the amount of chest blocks the player placed.
     */
    public static List<ChestInventory> chestInventories(
            ItemStack pouch, ServerPlayer player) {
        UUID pouchId = QiPouchStorage.ensureIdentity(pouch, player);
        ServerLevel level = level(player);
        if (pouchId == null || level == null) return List.of();

        ensurePlotLoaded(level, pouchId);
        int activeSide = side(pouch);
        List<ChestInventory> chests = new ArrayList<>();
        for (int layer = 0; layer < HEIGHT; layer++) {
            for (int row = 0; row < activeSide; row++) {
                for (int column = 0; column < activeSide; column++) {
                    int menuCell = row * MAX_SIDE + column;
                    BlockPos pos = cellPos(
                            pouchId, activeSide, menuCell, layer);
                    if (!(level.getBlockState(pos).getBlock()
                            instanceof ChestBlock)) continue;
                    if (level.getBlockEntity(pos)
                            instanceof Container container) {
                        chests.add(new ChestInventory(
                                globalCell(pouchId, pos), container));
                    }
                }
            }
        }
        return chests;
    }

    public static NonNullList<ItemStack> loadVolume(
            ItemStack pouch, ServerPlayer player) {
        NonNullList<ItemStack> cells = NonNullList.withSize(
                MAX_CELLS, ItemStack.EMPTY);
        UUID pouchId = QiPouchStorage.ensureIdentity(pouch, player);
        ServerLevel level = level(player);
        if (pouchId == null || level == null) return cells;

        ensurePlotLoaded(level, pouchId);
        migrateLegacy(pouch, player, level, pouchId);
        int activeSide = side(pouch);
        QiPouchBackupData backups = QiPouchBackupData.get(player.server);
        boolean recoverMissing = backups.beginRecovery(pouchId);
        Map<Integer, ItemStack> artifacts = loadArtifacts(
                pouch, player.registryAccess());
        for (int layer = 0; layer < HEIGHT; layer++) {
            for (int row = 0; row < activeSide; row++) {
                for (int column = 0; column < activeSide; column++) {
                    int layerCell = row * MAX_SIDE + column;
                    int volumeCell = layer * LAYER_SLOTS + layerCell;
                    BlockPos pos = cellPos(pouchId, activeSide,
                            layerCell, layer);
                    int storageCell = globalCell(pouchId, pos);
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir()) {
                        // A moving piston has no item form either. Mirror the
                        // state carried by its block entity so pushed blocks
                        // and the extending head remain visible in the GUI.
                        if (state.is(Blocks.MOVING_PISTON)
                                && level.getBlockEntity(pos)
                                instanceof PistonMovingBlockEntity moving) {
                            state = moving.getMovedState();
                        }
                        cells.set(volumeCell, displayStateStack(state));
                        ItemStack snapshot = snapshotCellStack(level, pos);
                        if (!snapshot.isEmpty()) {
                            backups.put(pouchId, storageCell, snapshot);
                        }
                        continue;
                    }
                    ItemStack artifact = artifacts.get(storageCell);
                    if (artifact != null && !artifact.isEmpty()) {
                        cells.set(volumeCell, artifact.copyWithCount(1));
                        backups.put(pouchId, storageCell, artifact);
                        continue;
                    }

                    // The real pouch world is authoritative while it is
                    // healthy. Only the first read after server start may use
                    // the SavedData copy to reconstruct a cell whose chunk did
                    // not survive or was not ready during reload.
                    if (recoverMissing) {
                        ItemStack backup = backups.get(
                                pouchId, storageCell);
                        if (!backup.isEmpty()
                                && QiPouchStorage.isFloorItem(backup)) {
                            ItemStack recovered = placeCell(
                                    pouch, player, layer, layerCell, backup);
                            if (!recovered.isEmpty()) {
                                cells.set(volumeCell,
                                        recovered.copyWithCount(1));
                            }
                        }
                    } else {
                        // Tracks intentional in-session removals and moving
                        // blocks, preventing stale backups from resurrecting
                        // them on a later restart.
                        backups.remove(pouchId, storageCell);
                    }
                }
            }
        }
        return cells;
    }

    public static ItemStack placeCell(
            ItemStack pouch, ServerPlayer player, int layer,
            int menuCell, ItemStack source) {
        return placeCell(pouch, player, layer, menuCell, source,
                player.getDirection());
    }

    /**
     * Places a block as though a virtual player inside the pouch were facing
     * {@code placerDirection}. The pouch screen supplies its camera direction,
     * so rotating the miniature world also changes new blocks' orientation.
     */
    public static ItemStack placeCell(
            ItemStack pouch, ServerPlayer player, int layer,
            int menuCell, ItemStack source, Direction placerDirection) {
        UUID pouchId = QiPouchStorage.ensureIdentity(pouch, player);
        ServerLevel level = level(player);
        int activeSide = side(pouch);
        if (pouchId == null || level == null
                || !validMenuCell(menuCell, activeSide)
                || layer < 0 || layer >= HEIGHT
                || !QiPouchStorage.isFloorItem(source)) {
            return ItemStack.EMPTY;
        }
        ensurePlotLoaded(level, pouchId);
        BlockPos pos = cellPos(pouchId, activeSide, menuCell, layer);
        if (!level.getBlockState(pos).isAir()
                || hasArtifact(pouch, player.registryAccess(),
                globalCell(pouchId, pos))) {
            return ItemStack.EMPTY;
        }

        if (!(source.getItem() instanceof BlockItem blockItem)) {
            ItemStack artifact = source.copyWithCount(1);
            if (FlyingSwordEntity.isFlyingWeapon(artifact)) {
                QiPouchStorage.linkArtifact(
                        pouch, artifact, player.getUUID());
            }
            putArtifact(pouch, player.registryAccess(),
                    globalCell(pouchId, pos), artifact);
            QiPouchBackupData.get(player.server).put(
                    pouchId, globalCell(pouchId, pos), artifact);
            return artifact;
        }

        Block block = blockItem.getBlock();
        BlockState state = source.getOrDefault(
                        DataComponents.BLOCK_STATE,
                        BlockItemStateProperties.EMPTY)
                .apply(block.defaultBlockState());
        BlockItemStateProperties specified = source.getOrDefault(
                DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
        Direction horizontalPlacerDirection = placerDirection != null
                && placerDirection.getAxis().isHorizontal()
                        ? placerDirection : player.getDirection();
        Direction blockFacing = horizontalPlacerDirection.getOpposite();
        // Use properties instead of block base classes. This also correctly
        // covers stairs, doors, trapdoors, furnaces, crafting utilities, and
        // modded blocks that expose vanilla facing properties.
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                && specified.get(BlockStateProperties.HORIZONTAL_FACING) == null) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING,
                    blockFacing);
        } else if (state.hasProperty(BlockStateProperties.FACING)
                && specified.get(BlockStateProperties.FACING) == null) {
            state = state.setValue(BlockStateProperties.FACING,
                    blockFacing);
        }
        if (state.getBlock() instanceof ChestBlock
                && state.hasProperty(ChestBlock.TYPE)) {
            state = state.setValue(ChestBlock.TYPE, ChestType.SINGLE);
        }
        if (!level.setBlock(pos, state, Block.UPDATE_ALL)) {
            return ItemStack.EMPTY;
        }
        state = connectPlacedChest(level, pos, level.getBlockState(pos));
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            CustomData customData = source.getOrDefault(
                    DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
            if (!customData.isEmpty()) {
                customData.loadInto(blockEntity, level.registryAccess());
            }
            blockEntity.applyComponentsFromItemStack(source);
            blockEntity.setChanged();
        }
        BlockState placedState = level.getBlockState(pos);
        placedState.getBlock().setPlacedBy(
                level, pos, placedState, player, source.copyWithCount(1));
        level.updateNeighborsAt(pos, placedState.getBlock());
        ItemStack placed = snapshotCellStack(level, pos);
        if (!placed.isEmpty()) {
            QiPouchBackupData.get(player.server).put(
                    pouchId, globalCell(pouchId, pos), placed);
        }
        return placed;
    }

    public static ItemStack removeCell(
            ItemStack pouch, ServerPlayer player, int layer, int menuCell) {
        UUID pouchId = QiPouchStorage.pouchId(pouch);
        ServerLevel level = level(player);
        int activeSide = side(pouch);
        if (pouchId == null || level == null
                || !validMenuCell(menuCell, activeSide)
                || layer < 0 || layer >= HEIGHT) {
            return ItemStack.EMPTY;
        }
        BlockPos pos = cellPos(pouchId, activeSide, menuCell, layer);
        int globalCell = globalCell(pouchId, pos);
        ItemStack artifact = removeArtifact(
                pouch, player.registryAccess(), globalCell);
        if (!artifact.isEmpty()) {
            QiPouchBackupData.get(player.server).remove(
                    pouchId, globalCell);
            return artifact;
        }

        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return ItemStack.EMPTY;
        ItemStack removed = stateStack(state);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null && !removed.isEmpty()) {
            blockEntity.saveToItem(removed, level.registryAccess());
            if (blockEntity instanceof Container container) {
                container.clearContent();
                blockEntity.setChanged();
            }
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.updateNeighborsAt(pos, state.getBlock());
        QiPouchBackupData.get(player.server).remove(
                pouchId, globalCell);
        return removed;
    }

    /**
     * Cycles the clicked block's directional state like a restricted debug
     * stick. Only direction/rotation properties are changed; unrelated block
     * properties (powered, open, waterlogged, and so on) are left untouched.
     */
    public static boolean cycleDirection(
            ItemStack pouch, ServerPlayer player, int layer, int menuCell) {
        UUID pouchId = QiPouchStorage.pouchId(pouch);
        ServerLevel level = level(player);
        int activeSide = side(pouch);
        if (pouchId == null || level == null
                || !validMenuCell(menuCell, activeSide)
                || layer < 0 || layer >= HEIGHT) {
            return false;
        }

        ensurePlotLoaded(level, pouchId);
        BlockPos pos = cellPos(pouchId, activeSide, menuCell, layer);
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;

        // Rotating an extended piston would orphan its moving head. Let it
        // retract first, then it can be turned normally.
        if (state.is(Blocks.PISTON_HEAD)
                || state.is(Blocks.MOVING_PISTON)
                || ((state.is(Blocks.PISTON) || state.is(Blocks.STICKY_PISTON))
                && state.hasProperty(BlockStateProperties.EXTENDED)
                && state.getValue(BlockStateProperties.EXTENDED))) {
            return false;
        }

        state = detachDoubleChest(level, pos, state);
        Property<?> property = directionalProperty(state);
        if (property == null) return false;
        BlockState rotated = cycleProperty(state, property);
        if (rotated == state || !level.setBlock(pos, rotated, Block.UPDATE_ALL)) {
            return false;
        }

        connectPlacedChest(level, pos, level.getBlockState(pos));
        updateRedstoneAround(level, pos);
        ItemStack snapshot = snapshotCellStack(level, pos);
        if (!snapshot.isEmpty()) {
            QiPouchBackupData.get(player.server).put(
                    pouchId, globalCell(pouchId, pos), snapshot);
        }
        return true;
    }

    public static boolean openCell(
            ItemStack pouch, ServerPlayer player, int layer, int menuCell) {
        UUID pouchId = QiPouchStorage.pouchId(pouch);
        ServerLevel level = level(player);
        int activeSide = side(pouch);
        if (pouchId == null || level == null
                || !validMenuCell(menuCell, activeSide)
                || layer < 0 || layer >= HEIGHT) {
            return false;
        }
        BlockPos pos = cellPos(pouchId, activeSide, menuCell, layer);
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;

        AbstractContainerMenu before = player.containerMenu;
        REMOTE_SESSIONS.put(player.getUUID(),
                new RemoteSession(pouchId, pos.immutable()));
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(pos), Direction.UP, pos, false);
        InteractionResult result = state.useWithoutItem(level, player, hit);
        if (player.containerMenu == before) {
            REMOTE_SESSIONS.remove(player.getUUID());
            return result.consumesAction();
        }
        return true;
    }

    public static boolean insertArtifact(
            ItemStack pouch, ServerPlayer owner, ItemStack artifact) {
        UUID pouchId = QiPouchStorage.pouchId(pouch);
        ServerLevel level = level(owner);
        if (pouchId == null || level == null || artifact.isEmpty()) return false;
        int activeSide = side(pouch);
        ensurePlotLoaded(level, pouchId);
        Map<Integer, ItemStack> artifacts = loadArtifacts(
                pouch, owner.registryAccess());
        for (int layer = 0; layer < HEIGHT; layer++) {
            for (int row = 0; row < activeSide; row++) {
                for (int column = 0; column < activeSide; column++) {
                    int cell = row * MAX_SIDE + column;
                    BlockPos pos = cellPos(pouchId, activeSide, cell, layer);
                    int globalCell = globalCell(pouchId, pos);
                    if (level.getBlockState(pos).isAir()
                            && !artifacts.containsKey(globalCell)) {
                        ItemStack stored = artifact.copyWithCount(1);
                        QiPouchStorage.linkArtifact(
                                pouch, stored, owner.getUUID());
                        artifacts.put(globalCell, stored);
                        saveArtifacts(pouch, artifacts, owner.registryAccess());
                        QiPouchBackupData.get(owner.server).put(
                                pouchId, globalCell, stored);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isRemoteInteraction(
            Player player, Level level, BlockPos pos) {
        if (player == null || level == null || pos == null
                || !DIMENSION.equals(level.dimension())) return false;
        RemoteSession session = REMOTE_SESSIONS.get(player.getUUID());
        if (session == null) return false;
        BlockPos origin = plotOrigin(session.pouchId());
        int x = pos.getX() - origin.getX();
        int y = pos.getY() - origin.getY();
        int z = pos.getZ() - origin.getZ();
        return x >= 0 && x < MAX_SIDE
                && z >= 0 && z < MAX_SIDE
                && y >= 0 && y < HEIGHT;
    }

    public static boolean blocksSessionPouchMove(
            Player player, AbstractContainerMenu menu,
            int slotId, int button, ClickType clickType) {
        RemoteSession session = REMOTE_SESSIONS.get(player.getUUID());
        if (session == null || menu instanceof QiPouchMenu) return false;
        if (samePouch(menu.getCarried(), session.pouchId())) return true;
        if (slotId >= 0 && slotId < menu.slots.size()
                && samePouch(menu.getSlot(slotId).getItem(),
                session.pouchId())) return true;
        return clickType == ClickType.SWAP
                && button >= 0
                && button < player.getInventory().getContainerSize()
                && samePouch(player.getInventory().getItem(button),
                session.pouchId());
    }

    public static void tickPlayer(ServerPlayer player) {
        RemoteSession session = REMOTE_SESSIONS.get(player.getUUID());
        if (session == null) return;
        if (!player.isAlive()) {
            REMOTE_SESSIONS.remove(player.getUUID());
            return;
        }
        if (player.containerMenu instanceof QiPouchMenu) {
            REMOTE_SESSIONS.remove(player.getUUID());
            return;
        }
        if (player.containerMenu != player.inventoryMenu) return;
        ItemStack pouch = QiPouchStorage.findPouch(player, session.pouchId());
        if (pouch.isEmpty()) {
            REMOTE_SESSIONS.remove(player.getUUID());
            return;
        }
        player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (containerId, inventory, openedPlayer) ->
                        new QiPouchMenu(containerId, inventory, pouch),
                net.minecraft.network.chat.Component.translatable(
                        "container." + ImmortalCultivationMod.MODID + ".qi_pouch")));
    }

    private static ServerLevel level(ServerPlayer player) {
        return player.server.getLevel(DIMENSION);
    }

    private static boolean samePouch(ItemStack stack, UUID pouchId) {
        return !stack.isEmpty() && stack.is(ModItems.QI_POUCH.get())
                && pouchId.equals(QiPouchStorage.pouchId(stack));
    }

    private static boolean validMenuCell(int menuCell, int side) {
        if (menuCell < 0 || menuCell >= LAYER_SLOTS) return false;
        int row = menuCell / MAX_SIDE;
        int column = menuCell % MAX_SIDE;
        return row < side && column < side;
    }

    private static BlockPos plotOrigin(UUID pouchId) {
        int plotX = ((int)pouchId.getMostSignificantBits() & PLOT_COORD_MASK)
                - PLOT_COORD_CENTER;
        int plotZ = ((int)pouchId.getLeastSignificantBits() & PLOT_COORD_MASK)
                - PLOT_COORD_CENTER;
        return new BlockPos(plotX * PLOT_PITCH, FLOOR_Y,
                plotZ * PLOT_PITCH);
    }

    private static BlockPos cellPos(
            UUID pouchId, int side, int menuCell, int layer) {
        int row = menuCell / MAX_SIDE;
        int column = menuCell % MAX_SIDE;
        int offset = (MAX_SIDE - side) / 2;
        return plotOrigin(pouchId).offset(
                offset + column, layer, offset + row);
    }

    private static int globalCell(UUID pouchId, BlockPos pos) {
        BlockPos origin = plotOrigin(pouchId);
        int x = pos.getX() - origin.getX();
        int y = pos.getY() - origin.getY();
        int z = pos.getZ() - origin.getZ();
        return y * LAYER_SLOTS + z * MAX_SIDE + x;
    }

    private static void ensurePlotLoaded(ServerLevel level, UUID pouchId) {
        BlockPos origin = plotOrigin(pouchId);
        ChunkPos minimum = new ChunkPos(origin);
        ChunkPos maximum = new ChunkPos(origin.offset(
                MAX_SIDE - 1, 0, MAX_SIDE - 1));
        for (int chunkX = minimum.x; chunkX <= maximum.x; chunkX++) {
            for (int chunkZ = minimum.z; chunkZ <= maximum.z; chunkZ++) {
                level.setChunkForced(chunkX, chunkZ, true);
                level.getChunk(chunkX, chunkZ);
            }
        }
        if (!level.getBlockState(origin.below()).is(Blocks.GRASS_BLOCK)) {
            for (int x = 0; x < MAX_SIDE; x++) {
                for (int z = 0; z < MAX_SIDE; z++) {
                    BlockPos floor = origin.offset(x, -1, z);
                    level.setBlock(floor.below(),
                            Blocks.STONE.defaultBlockState(), Block.UPDATE_NONE);
                    level.setBlock(floor,
                            Blocks.GRASS_BLOCK.defaultBlockState(),
                            Block.UPDATE_NONE);
                }
            }
        }
    }

    private static void migrateLegacy(
            ItemStack pouch, ServerPlayer player, ServerLevel level,
            UUID pouchId) {
        CompoundTag root = pouch.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (root.getBoolean(MIGRATED_TAG)) return;
        NonNullList<ItemStack> legacy = NonNullList.withSize(
                QiPouchStorage.LEGACY_SLOTS, ItemStack.EMPTY);
        if (root.contains(LEGACY_ITEMS_TAG)) {
            net.minecraft.world.ContainerHelper.loadAllItems(
                    root, legacy, player.registryAccess());
        }
        root.remove(LEGACY_ITEMS_TAG);
        root.putBoolean(MIGRATED_TAG, true);
        pouch.set(DataComponents.CUSTOM_DATA, CustomData.of(root));

        int activeSide = side(pouch);
        int cursor = 0;
        for (ItemStack legacyStack : legacy) {
            if (legacyStack.isEmpty()) continue;
            if (!QiPouchStorage.isFloorItem(legacyStack)) {
                QiPouchStorage.giveOrDrop(player, legacyStack);
                continue;
            }
            ItemStack remaining = legacyStack.copy();
            while (!remaining.isEmpty()) {
                boolean placed = false;
                while (cursor < activeSide * activeSide * HEIGHT) {
                    int layer = cursor / (activeSide * activeSide);
                    int withinLayer = cursor++ % (activeSide * activeSide);
                    int row = withinLayer / activeSide;
                    int column = withinLayer % activeSide;
                    int menuCell = row * MAX_SIDE + column;
                    BlockPos pos = cellPos(pouchId, activeSide, menuCell, layer);
                    if (!level.getBlockState(pos).isAir()) continue;
                    if (!placeCell(pouch, player, layer,
                            menuCell, remaining).isEmpty()) {
                        remaining.shrink(1);
                        placed = true;
                        break;
                    }
                }
                if (!placed) break;
            }
            if (!remaining.isEmpty()) {
                QiPouchStorage.giveOrDrop(player, remaining);
            }
        }
    }

    private static BlockState connectPlacedChest(
            ServerLevel level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof ChestBlock)
                || !state.hasProperty(ChestBlock.FACING)
                || !state.hasProperty(ChestBlock.TYPE)) return state;
        Direction facing = state.getValue(ChestBlock.FACING);
        for (ChestType type : new ChestType[]{ChestType.LEFT, ChestType.RIGHT}) {
            Direction connection = type == ChestType.LEFT
                    ? facing.getClockWise() : facing.getCounterClockWise();
            BlockPos neighborPos = pos.relative(connection);
            BlockState neighbor = level.getBlockState(neighborPos);
            if (neighbor.is(state.getBlock())
                    && neighbor.hasProperty(ChestBlock.TYPE)
                    && neighbor.getValue(ChestBlock.TYPE) == ChestType.SINGLE
                    && neighbor.getValue(ChestBlock.FACING) == facing) {
                BlockState connected = state.setValue(ChestBlock.TYPE, type);
                level.setBlock(pos, connected, Block.UPDATE_CLIENTS);
                level.setBlock(neighborPos,
                        neighbor.setValue(ChestBlock.TYPE, type.getOpposite()),
                        Block.UPDATE_CLIENTS);
                level.updateNeighborsAt(pos, state.getBlock());
                level.updateNeighborsAt(neighborPos, neighbor.getBlock());
                return level.getBlockState(pos);
            }
        }
        return state;
    }

    private static BlockState detachDoubleChest(
            ServerLevel level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof ChestBlock)
                || !state.hasProperty(ChestBlock.FACING)
                || !state.hasProperty(ChestBlock.TYPE)
                || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return state;
        }
        Direction facing = state.getValue(ChestBlock.FACING);
        ChestType type = state.getValue(ChestBlock.TYPE);
        Direction connection = type == ChestType.LEFT
                ? facing.getClockWise() : facing.getCounterClockWise();
        BlockPos neighborPos = pos.relative(connection);
        BlockState neighbor = level.getBlockState(neighborPos);
        if (neighbor.is(state.getBlock())
                && neighbor.hasProperty(ChestBlock.TYPE)
                && neighbor.getValue(ChestBlock.TYPE) == type.getOpposite()) {
            level.setBlock(neighborPos,
                    neighbor.setValue(ChestBlock.TYPE, ChestType.SINGLE),
                    Block.UPDATE_ALL);
        }
        BlockState single = state.setValue(
                ChestBlock.TYPE, ChestType.SINGLE);
        level.setBlock(pos, single, Block.UPDATE_ALL);
        return level.getBlockState(pos);
    }

    private static Property<?> directionalProperty(BlockState state) {
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return BlockStateProperties.HORIZONTAL_FACING;
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return BlockStateProperties.FACING;
        }
        // Hopper and some modded blocks use a separate property object but
        // still expose the conventional "facing" state name.
        for (Property<?> property : state.getProperties()) {
            if ("facing".equals(property.getName())) return property;
        }
        if (state.hasProperty(BlockStateProperties.ROTATION_16)) {
            return BlockStateProperties.ROTATION_16;
        }
        if (state.hasProperty(BlockStateProperties.AXIS)) {
            return BlockStateProperties.AXIS;
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
            return BlockStateProperties.HORIZONTAL_AXIS;
        }
        return null;
    }

    private static <T extends Comparable<T>> BlockState cycleProperty(
            BlockState state, Property<T> property) {
        return state.cycle(property);
    }

    /** Rechecks the full local redstone neighborhood after a virtual edit. */
    private static void updateRedstoneAround(ServerLevel level, BlockPos pos) {
        Block block = level.getBlockState(pos).getBlock();
        level.updateNeighborsAt(pos, block);
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            level.updateNeighborsAt(
                    neighborPos, level.getBlockState(neighborPos).getBlock());
        }
    }

    private static ItemStack stateStack(BlockState state) {
        if (state.isAir() || state.getBlock().asItem() == Items.AIR) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(state.getBlock().asItem());
        return applyStateProperties(stack, state);
    }

    /** Includes block-entity inventory/components in the persistent fallback. */
    private static ItemStack snapshotCellStack(
            ServerLevel level, BlockPos pos) {
        BlockState worldState = level.getBlockState(pos);
        boolean movingPiston = worldState.is(Blocks.MOVING_PISTON);
        BlockState itemState = worldState;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (movingPiston
                && blockEntity instanceof PistonMovingBlockEntity moving) {
            itemState = moving.getMovedState();
        }
        ItemStack snapshot = stateStack(itemState);
        if (!snapshot.isEmpty() && blockEntity != null && !movingPiston) {
            blockEntity.saveToItem(snapshot, level.registryAccess());
        }
        return snapshot;
    }

    /**
     * Creates a menu-synchronizable representation of a real block state.
     * Piston heads deliberately have no obtainable item, so a marked piston
     * stack is used only as a transport envelope for its exact state.
     */
    private static ItemStack displayStateStack(BlockState state) {
        if (!state.is(Blocks.PISTON_HEAD)) {
            return stateStack(state);
        }
        ItemStack stack = applyStateProperties(
                new ItemStack(Items.PISTON), state);
        CompoundTag root = stack.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        root.putBoolean(DISPLAY_PISTON_HEAD_TAG, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        return stack;
    }

    /** Resolves the exact world state encoded in a pouch display stack. */
    public static BlockState displayState(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
            return Blocks.AIR.defaultBlockState();
        }
        CompoundTag root = stack.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        Block block = root.getBoolean(DISPLAY_PISTON_HEAD_TAG)
                ? Blocks.PISTON_HEAD : blockItem.getBlock();
        return stack.getOrDefault(
                        DataComponents.BLOCK_STATE,
                        BlockItemStateProperties.EMPTY)
                .apply(block.defaultBlockState());
    }

    private static ItemStack applyStateProperties(
            ItemStack stack, BlockState state) {
        BlockItemStateProperties properties = BlockItemStateProperties.EMPTY;
        for (Property<?> property : state.getProperties()) {
            properties = withStateProperty(properties, state, property);
        }
        if (!properties.isEmpty()) {
            stack.set(DataComponents.BLOCK_STATE, properties);
        }
        return stack;
    }

    private static <T extends Comparable<T>> BlockItemStateProperties withStateProperty(
            BlockItemStateProperties properties, BlockState state,
            Property<T> property) {
        return properties.with(property, state);
    }

    private static Map<Integer, ItemStack> loadArtifacts(
            ItemStack pouch, HolderLookup.Provider registries) {
        Map<Integer, ItemStack> artifacts = new HashMap<>();
        CompoundTag root = pouch.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag list = root.getList(ARTIFACTS_TAG, Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag entry = list.getCompound(index);
            int cell = entry.getInt(ARTIFACT_CELL_TAG);
            if (cell < 0 || cell >= MAX_CELLS
                    || !entry.contains(ARTIFACT_STACK_TAG)) continue;
            ItemStack stack = ItemStack.parse(
                    registries, entry.get(ARTIFACT_STACK_TAG))
                    .orElse(ItemStack.EMPTY);
            if (!stack.isEmpty()) artifacts.put(cell, stack);
        }
        return artifacts;
    }

    private static void saveArtifacts(
            ItemStack pouch, Map<Integer, ItemStack> artifacts,
            HolderLookup.Provider registries) {
        CompoundTag root = pouch.getOrDefault(
                DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag list = new ListTag();
        artifacts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    if (entry.getValue().isEmpty()) return;
                    CompoundTag artifact = new CompoundTag();
                    artifact.putInt(ARTIFACT_CELL_TAG, entry.getKey());
                    artifact.put(ARTIFACT_STACK_TAG,
                            entry.getValue().save(registries));
                    list.add(artifact);
                });
        if (list.isEmpty()) root.remove(ARTIFACTS_TAG);
        else root.put(ARTIFACTS_TAG, list);
        pouch.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    private static boolean hasArtifact(
            ItemStack pouch, HolderLookup.Provider registries, int cell) {
        return loadArtifacts(pouch, registries).containsKey(cell);
    }

    private static void putArtifact(
            ItemStack pouch, HolderLookup.Provider registries,
            int cell, ItemStack stack) {
        Map<Integer, ItemStack> artifacts = loadArtifacts(pouch, registries);
        artifacts.put(cell, stack.copyWithCount(1));
        saveArtifacts(pouch, artifacts, registries);
    }

    private static ItemStack removeArtifact(
            ItemStack pouch, HolderLookup.Provider registries, int cell) {
        Map<Integer, ItemStack> artifacts = loadArtifacts(pouch, registries);
        ItemStack removed = artifacts.remove(cell);
        if (removed == null || removed.isEmpty()) return ItemStack.EMPTY;
        saveArtifacts(pouch, artifacts, registries);
        return removed;
    }

    private record RemoteSession(UUID pouchId, BlockPos pos) {
    }

    /** A stable plot cell paired with its live 27-slot chest inventory. */
    public record ChestInventory(int cell, Container container) {
    }
}
