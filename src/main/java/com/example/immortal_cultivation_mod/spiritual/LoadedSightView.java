package com.example.immortal_cultivation_mod.spiritual;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;

/** Raycast only through loaded terrain; cache the current chunk along the ray. */
final class LoadedSightView implements BlockGetter {
    private final ServerLevel level;
    private int chunkX = Integer.MIN_VALUE;
    private int chunkZ = Integer.MIN_VALUE;
    private LevelChunk chunk;

    LoadedSightView(ServerLevel level) { this.level = level; }

    private LevelChunk chunk(BlockPos position) {
        int x = position.getX() >> 4;
        int z = position.getZ() >> 4;
        if (x != chunkX || z != chunkZ) {
            chunkX = x;
            chunkZ = z;
            chunk = level.getChunkSource().getChunkNow(x, z);
        }
        return chunk;
    }

    @Override public BlockState getBlockState(BlockPos position) {
        LevelChunk loaded = chunk(position);
        return loaded == null ? Blocks.AIR.defaultBlockState() : loaded.getBlockState(position);
    }

    @Override public FluidState getFluidState(BlockPos position) {
        return getBlockState(position).getFluidState();
    }

    @Override public BlockEntity getBlockEntity(BlockPos position) {
        LevelChunk loaded = chunk(position);
        return loaded == null ? null : loaded.getBlockEntities().get(position);
    }

    @Override public int getHeight() { return level.getHeight(); }
    @Override public int getMinBuildHeight() { return level.getMinBuildHeight(); }
}
