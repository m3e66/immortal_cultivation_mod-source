package com.example.immortal_cultivation_mod.compat;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Makes walls understand the visual height of Ultramarine's three-block-tall
 * doors. Those doors occupy only their center block while their models extend
 * one block above and below, so vanilla wall checks otherwise see air at the
 * top and bottom.
 */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class UltramarineDoorWallCompat {
    private static final ResourceLocation CARVED_WOODEN_DOOR =
            ResourceLocation.fromNamespaceAndPath(
                    "ultramarine", "carved_wooden_door");
    private static final ResourceLocation SONG_WOODEN_DOOR =
            ResourceLocation.fromNamespaceAndPath(
                    "ultramarine", "song_wooden_door");
    private static final Direction[] HORIZONTAL_DIRECTIONS = {
            Direction.NORTH, Direction.EAST,
            Direction.SOUTH, Direction.WEST
    };
    private static final Set<PendingRefresh> PENDING_REFRESHES =
            ConcurrentHashMap.newKeySet();

    private UltramarineDoorWallCompat() {
    }

    /** Adds low wall arms where a tall door model occupies virtual air. */
    public static BlockState addVirtualDoorConnections(
            BlockState wallState, LevelReader level, BlockPos wallPos) {
        if (!(wallState.getBlock() instanceof WallBlock)) {
            return wallState;
        }

        BlockState result = wallState;
        boolean hasVirtualConnection = false;
        for (Direction direction : HORIZONTAL_DIRECTIONS) {
            BlockPos neighborPos = wallPos.relative(direction);
            if (!isTallDoor(level.getBlockState(neighborPos.above()))
                    && !isTallDoor(level.getBlockState(neighborPos.below()))) {
                continue;
            }

            hasVirtualConnection = true;
            EnumProperty<WallSide> property = wallProperty(direction);
            if (result.getValue(property) == WallSide.NONE) {
                result = result.setValue(property, WallSide.LOW);
            }
        }

        return hasVirtualConnection
                ? result.setValue(WallBlock.UP, true)
                : result;
    }

    public static boolean isTallDoor(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return CARVED_WOODEN_DOOR.equals(id) || SONG_WOODEN_DOOR.equals(id);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDoorPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!event.isCanceled()
                && event.getLevel() instanceof ServerLevel level
                && isTallDoor(event.getPlacedBlock())) {
            queueRefresh(level, event.getPos());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDoorBroken(BlockEvent.BreakEvent event) {
        if (!event.isCanceled()
                && event.getLevel() instanceof ServerLevel level
                && isTallDoor(event.getState())) {
            queueRefresh(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        for (BlockPos pos : event.getAffectedBlocks()) {
            if (isTallDoor(level.getBlockState(pos))) {
                queueRefresh(level, pos);
            }
        }
    }

    @SubscribeEvent
    public static void processPendingRefreshes(ServerTickEvent.Post event) {
        if (PENDING_REFRESHES.isEmpty()) {
            return;
        }
        List<PendingRefresh> pending = List.copyOf(PENDING_REFRESHES);
        PENDING_REFRESHES.removeAll(pending);
        for (PendingRefresh refresh : pending) {
            ServerLevel level = event.getServer().getLevel(refresh.dimension());
            if (level != null && level.isLoaded(refresh.doorPos())) {
                refreshWallsAroundDoor(level, refresh.doorPos());
            }
        }
    }

    /**
     * Repairs already-placed walls when an existing world is first opened or
     * when a player approaches a door that came from structure placement.
     */
    @SubscribeEvent
    public static void refreshNearbyExistingDoors(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.tickCount % 100 != 0) {
            return;
        }

        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = center.getY() - 8; y <= center.getY() + 8; y++) {
            for (int x = center.getX() - 16; x <= center.getX() + 16; x++) {
                for (int z = center.getZ() - 16; z <= center.getZ() + 16; z++) {
                    cursor.set(x, y, z);
                    if (isTallDoor(level.getBlockState(cursor))) {
                        refreshWallsAroundDoor(level, cursor.immutable());
                    }
                }
            }
        }
    }

    private static void queueRefresh(ServerLevel level, BlockPos doorPos) {
        PENDING_REFRESHES.add(new PendingRefresh(
                level.dimension(), doorPos.immutable()));
    }

    private static void refreshWallsAroundDoor(
            ServerLevel level, BlockPos doorPos) {
        for (int yOffset = -1; yOffset <= 1; yOffset++) {
            BlockPos visualSection = doorPos.offset(0, yOffset, 0);
            for (Direction direction : HORIZONTAL_DIRECTIONS) {
                refreshWall(level, visualSection.relative(direction));
            }
        }
    }

    private static void refreshWall(ServerLevel level, BlockPos wallPos) {
        BlockState original = level.getBlockState(wallPos);
        if (!(original.getBlock() instanceof WallBlock)) {
            return;
        }

        BlockState refreshed = original;
        for (Direction direction : HORIZONTAL_DIRECTIONS) {
            BlockPos neighborPos = wallPos.relative(direction);
            refreshed = refreshed.updateShape(
                    direction, level.getBlockState(neighborPos),
                    level, wallPos, neighborPos);
        }
        refreshed = addVirtualDoorConnections(refreshed, level, wallPos);
        if (refreshed != original) {
            level.setBlock(wallPos, refreshed, Block.UPDATE_ALL);
        }
    }

    private static EnumProperty<WallSide> wallProperty(Direction direction) {
        return switch (direction) {
            case NORTH -> WallBlock.NORTH_WALL;
            case EAST -> WallBlock.EAST_WALL;
            case SOUTH -> WallBlock.SOUTH_WALL;
            case WEST -> WallBlock.WEST_WALL;
            default -> throw new IllegalArgumentException(
                    "Wall connection direction must be horizontal");
        };
    }

    private record PendingRefresh(
            ResourceKey<Level> dimension, BlockPos doorPos) {
    }
}
