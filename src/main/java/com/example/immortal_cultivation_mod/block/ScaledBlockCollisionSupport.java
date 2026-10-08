package com.example.immortal_cultivation_mod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * Supplies the portions of scaled model collision shapes that lie too far from their
 * anchor block for vanilla's {@code BlockCollisions} cursor to discover.
 *
 * <p>Vanilla searches only a one-block shell around an entity. That works for ordinary
 * blocks, but a 5x model can extend four blocks horizontally and roughly ten blocks
 * upward from its single placed block. Searching loaded chunks for the model's existing
 * block entity keeps the detailed generated shape intact without placing proxy blocks
 * into the world.</p>
 */
public final class ScaledBlockCollisionSupport {
    private static final double CURSOR_EPSILON = 1.0E-7D;
    private static final int MAX_HORIZONTAL_ANCHOR_DISTANCE = GeoRockBlock.MAX_SCALE + 1;

    private ScaledBlockCollisionSupport() {
    }

    public static List<Collision> findMissingCollisions(
            CollisionGetter collisionGetter,
            AABB queryBox,
            CollisionContext context,
            boolean onlySuffocatingBlocks) {
        if (!(collisionGetter instanceof Level level)) {
            return List.of();
        }

        int minChunkX = SectionPos.blockToSectionCoord(
                Mth.floor(queryBox.minX) - MAX_HORIZONTAL_ANCHOR_DISTANCE);
        int maxChunkX = SectionPos.blockToSectionCoord(
                Mth.floor(queryBox.maxX) + MAX_HORIZONTAL_ANCHOR_DISTANCE);
        int minChunkZ = SectionPos.blockToSectionCoord(
                Mth.floor(queryBox.minZ) - MAX_HORIZONTAL_ANCHOR_DISTANCE);
        int maxChunkZ = SectionPos.blockToSectionCoord(
                Mth.floor(queryBox.maxZ) + MAX_HORIZONTAL_ANCHOR_DISTANCE);

        VoxelShape queryShape = Shapes.create(queryBox);
        List<Collision> collisions = new ArrayList<>();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                ChunkAccess chunk = level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                if (!(chunk instanceof LevelChunk levelChunk)) {
                    continue;
                }
                collectChunkCollisions(
                        level,
                        levelChunk,
                        queryBox,
                        queryShape,
                        context,
                        onlySuffocatingBlocks,
                        collisions);
            }
        }
        return collisions;
    }

    private static void collectChunkCollisions(
            Level level,
            LevelChunk chunk,
            AABB queryBox,
            VoxelShape queryShape,
            CollisionContext context,
            boolean onlySuffocatingBlocks,
            List<Collision> collisions) {
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            if (!(blockEntity instanceof GeoRockBlockEntity) || blockEntity.isRemoved()) {
                continue;
            }

            BlockPos anchor = blockEntity.getBlockPos();
            BlockState state = chunk.getBlockState(anchor);
            if (!(state.getBlock() instanceof GeoRockBlock)
                    || !state.hasProperty(GeoRockBlock.SCALE)
                    || vanillaCursorVisits(anchor, queryBox, state)) {
                continue;
            }
            if (onlySuffocatingBlocks && !state.isSuffocating(level, anchor)) {
                continue;
            }

            VoxelShape worldShape = state.getCollisionShape(level, anchor, context).move(
                    anchor.getX(), anchor.getY(), anchor.getZ());
            if (!worldShape.isEmpty()
                    && Shapes.joinIsNotEmpty(worldShape, queryShape, BooleanOp.AND)) {
                collisions.add(new Collision(anchor.immutable(), worldShape));
            }
        }
    }

    /** Mirrors the bounds and face/edge rules used by vanilla's Cursor3D. */
    private static boolean vanillaCursorVisits(
            BlockPos anchor, AABB queryBox, BlockState state) {
        int minX = Mth.floor(queryBox.minX - CURSOR_EPSILON) - 1;
        int maxX = Mth.floor(queryBox.maxX + CURSOR_EPSILON) + 1;
        int minY = Mth.floor(queryBox.minY - CURSOR_EPSILON) - 1;
        int maxY = Mth.floor(queryBox.maxY + CURSOR_EPSILON) + 1;
        int minZ = Mth.floor(queryBox.minZ - CURSOR_EPSILON) - 1;
        int maxZ = Mth.floor(queryBox.maxZ + CURSOR_EPSILON) + 1;

        int x = anchor.getX();
        int y = anchor.getY();
        int z = anchor.getZ();
        if (x < minX || x > maxX || y < minY || y > maxY || z < minZ || z > maxZ) {
            return false;
        }

        int boundaryCount = 0;
        if (x == minX || x == maxX) boundaryCount++;
        if (y == minY || y == maxY) boundaryCount++;
        if (z == minZ || z == maxZ) boundaryCount++;
        return boundaryCount == 0
                || boundaryCount == 1 && state.hasLargeCollisionShape();
    }

    public record Collision(BlockPos anchor, VoxelShape shape) {
    }
}
