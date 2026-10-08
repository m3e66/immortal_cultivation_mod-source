package com.example.immortal_cultivation_mod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;

/** Spiritwood requires dry planting space and dry, valid sapling soil. */
public final class LingmuSaplingBlock extends SaplingBlock {
    public LingmuSaplingBlock(TreeGrower grower, Properties properties) {
        super(grower, properties);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getFluidState(pos).isEmpty()
                && level.getFluidState(pos.below()).isEmpty()
                && super.canSurvive(state, level, pos);
    }
}
