package com.example.immortal_cultivation_mod.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Sealed basin support, using the same 2-model-unit collision grid as the existing hulls. */
final class DingSealedFloorShapes {

    static VoxelShape iron() {
        return Shapes.or(
                Block.box(-2, 5.5125, -2, 18, 6.66, 18)
        ).optimize();
    }

    static VoxelShape bronze() {
        return Shapes.or(
                Block.box(4, 9.15, -4, 12, 10.25, 20),
                Block.box(0, 9.15, -2, 4, 10.25, 18),
                Block.box(12, 9.15, -2, 16, 10.25, 18),
                Block.box(-2, 9.15, 0, 0, 10.25, 16),
                Block.box(16, 9.15, 0, 18, 10.25, 16),
                Block.box(-4, 9.15, 4, -2, 10.25, 12),
                Block.box(18, 9.15, 4, 20, 10.25, 12)
        ).optimize();
    }

    static VoxelShape sanyang() {
        return Shapes.or(
                Block.box(2, 8.8, -6, 14, 10.02, 22),
                Block.box(-2, 8.8, -4, 2, 10.02, 20),
                Block.box(14, 8.8, -4, 18, 10.02, 20),
                Block.box(-4, 8.8, -2, -2, 10.02, 18),
                Block.box(18, 8.8, -2, 20, 10.02, 18),
                Block.box(-6, 8.8, 2, -4, 10.02, 14),
                Block.box(20, 8.8, 2, 22, 10.02, 14)
        ).optimize();
    }
}
