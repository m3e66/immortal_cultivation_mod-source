package com.example.immortal_cultivation_mod.worldgen;

import com.example.immortal_cultivation_mod.magicritual.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

/** Adds exposed spirit ore to the two long walls of marked mineshaft corridors. */
public final class AbandonedSpiritMineDecorator {
    private static final int SPIRIT_ORE_THRESHOLD = 5_571;

    private AbandonedSpiritMineDecorator() {
    }

    public static void decorateCorridor(WorldGenLevel level, StructurePiece corridor,
                                        BoundingBox generationBox) {
        Direction orientation = corridor.getOrientation();
        if (orientation == null) {
            return;
        }

        BoundingBox corridorBox = corridor.getBoundingBox();
        if (orientation.getAxis() == Direction.Axis.Z) {
            decorateZWall(level, generationBox, corridorBox.minX() - 1,
                    corridorBox.minX(), corridorBox);
            decorateZWall(level, generationBox, corridorBox.maxX() + 1,
                    corridorBox.maxX(), corridorBox);
        } else {
            decorateXWall(level, generationBox, corridorBox.minZ() - 1,
                    corridorBox.minZ(), corridorBox);
            decorateXWall(level, generationBox, corridorBox.maxZ() + 1,
                    corridorBox.maxZ(), corridorBox);
        }
    }

    private static void decorateZWall(WorldGenLevel level, BoundingBox generationBox,
                                      int wallX, int insideX,
                                      BoundingBox corridorBox) {
        BlockPos.MutableBlockPos wallPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos insidePos = new BlockPos.MutableBlockPos();
        for (int z = corridorBox.minZ(); z <= corridorBox.maxZ(); z++) {
            for (int y = corridorBox.minY(); y <= corridorBox.maxY(); y++) {
                wallPos.set(wallX, y, z);
                insidePos.set(insideX, y, z);
                tryPlaceOre(level, generationBox, wallPos, insidePos);
            }
        }
    }

    private static void decorateXWall(WorldGenLevel level, BoundingBox generationBox,
                                      int wallZ, int insideZ,
                                      BoundingBox corridorBox) {
        BlockPos.MutableBlockPos wallPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos insidePos = new BlockPos.MutableBlockPos();
        for (int x = corridorBox.minX(); x <= corridorBox.maxX(); x++) {
            for (int y = corridorBox.minY(); y <= corridorBox.maxY(); y++) {
                wallPos.set(x, y, wallZ);
                insidePos.set(x, y, insideZ);
                tryPlaceOre(level, generationBox, wallPos, insidePos);
            }
        }
    }

    private static void tryPlaceOre(WorldGenLevel level, BoundingBox generationBox,
                                    BlockPos wallPos, BlockPos insidePos) {
        if (!generationBox.isInside(wallPos)
                || !level.getBlockState(insidePos).isAir()
                || !shouldPlaceSpiritOre(wallPos)) {
            return;
        }

        BlockState wallState = level.getBlockState(wallPos);
        if (wallState.is(BlockTags.BASE_STONE_OVERWORLD)) {
            level.setBlock(wallPos, ModBlocks.SPIRIT_ORE.get().defaultBlockState(), 2);
        }
    }

    private static boolean shouldPlaceSpiritOre(BlockPos pos) {
        long hash = pos.asLong() ^ 0x6A09E667F3BCC909L;
        hash ^= hash >>> 33;
        hash *= 0xFF51AFD7ED558CCDL;
        hash ^= hash >>> 33;
        return (hash & 0xFFFFL) < SPIRIT_ORE_THRESHOLD;
    }
}
