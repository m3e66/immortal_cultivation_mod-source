package com.example.immortal_cultivation_mod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Makes model parts outside a ding's anchor block interactive, including its native size. */
public final class ScaledDingInteractionSupport {
    private static final int ANCHOR_SEARCH_MARGIN = GeoRockBlock.MAX_SCALE + 1;
    private static final double HIT_EPSILON_SQUARED = 0.25D;
    private static final double OCCLUSION_EPSILON_SQUARED = 1.0E-6D;
    private static final double REACH_TOLERANCE = 0.75D;

    private ScaledDingInteractionSupport() {
    }

    @Nullable
    public static BlockHitResult findNearestScaledDing(
            Level level, Vec3 start, Vec3 end,
            CollisionContext context, double maximumDistanceSquared) {
        AABB rayBounds = new AABB(start, end);
        int minChunkX = SectionPos.blockToSectionCoord(
                (int) Math.floor(rayBounds.minX) - ANCHOR_SEARCH_MARGIN);
        int maxChunkX = SectionPos.blockToSectionCoord(
                (int) Math.floor(rayBounds.maxX) + ANCHOR_SEARCH_MARGIN);
        int minChunkZ = SectionPos.blockToSectionCoord(
                (int) Math.floor(rayBounds.minZ) - ANCHOR_SEARCH_MARGIN);
        int maxChunkZ = SectionPos.blockToSectionCoord(
                (int) Math.floor(rayBounds.maxZ) + ANCHOR_SEARCH_MARGIN);

        BlockHitResult nearest = null;
        double nearestDistanceSquared = maximumDistanceSquared;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                ChunkAccess chunk = level.getChunk(
                        chunkX, chunkZ, ChunkStatus.FULL, false);
                if (!(chunk instanceof LevelChunk levelChunk)) continue;

                for (BlockEntity blockEntity
                        : levelChunk.getBlockEntities().values()) {
                    if (blockEntity.isRemoved()) continue;
                    BlockPos anchor = blockEntity.getBlockPos();
                    BlockState state = levelChunk.getBlockState(anchor);
                    if (!(state.getBlock() instanceof DingBlock)
                            || !state.hasProperty(GeoRockBlock.SCALE)) {
                        continue;
                    }

                    BlockHitResult hit = state.getShape(
                            level, anchor, context).clip(start, end, anchor);
                    if (hit == null) continue;
                    double distanceSquared = start.distanceToSqr(
                            hit.getLocation());
                    if (distanceSquared <= nearestDistanceSquared) {
                        nearest = hit;
                        nearestDistanceSquared = distanceSquared;
                    }
                }
            }
        }
        return nearest;
    }

    public static boolean interact(
            ServerPlayer player, BlockPos anchor, Vec3 claimedHit) {
        if (!finite(claimedHit)) return false;
        Level level = player.serverLevel();
        if (!level.hasChunkAt(anchor)
                || !level.getWorldBorder().isWithinBounds(anchor)) {
            return false;
        }

        BlockState state = level.getBlockState(anchor);
        if (!(state.getBlock() instanceof DingBlock)
                || !state.hasProperty(GeoRockBlock.SCALE)) {
            return false;
        }

        Vec3 start = player.getEyePosition();
        double allowedReach = player.blockInteractionRange()
                + REACH_TOLERANCE;
        if (start.distanceToSqr(claimedHit)
                > allowedReach * allowedReach) {
            return false;
        }
        Vec3 ray = claimedHit.subtract(start);
        if (ray.lengthSqr() < 1.0E-8D) return false;

        Vec3 validationEnd = claimedHit.add(
                ray.normalize().scale(0.08D));
        VoxelShape shape = state.getShape(
                level, anchor, CollisionContext.of(player));
        BlockHitResult verifiedHit = shape.clip(
                start, validationEnd, anchor);
        if (verifiedHit == null
                || verifiedHit.getLocation().distanceToSqr(claimedHit)
                > HIT_EPSILON_SQUARED) {
            return false;
        }

        BlockHitResult normalHit = level.clip(new ClipContext(
                start, validationEnd,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player));
        if (normalHit.getType() != HitResult.Type.MISS
                && !normalHit.getBlockPos().equals(anchor)
                && start.distanceToSqr(normalHit.getLocation())
                + OCCLUSION_EPSILON_SQUARED
                < start.distanceToSqr(verifiedHit.getLocation())) {
            return false;
        }

        InteractionResult result = state.useWithoutItem(
                level, player, verifiedHit);
        return result.consumesAction();
    }

    private static boolean finite(Vec3 value) {
        return Double.isFinite(value.x)
                && Double.isFinite(value.y)
                && Double.isFinite(value.z);
    }
}
