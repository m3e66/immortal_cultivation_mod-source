package com.example.immortal_cultivation_mod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;

/** Spiritwood matures slowly and requires dry, valid sapling soil. */
public final class LingmuSaplingBlock extends SaplingBlock {
    private static final int NATURAL_GROWTH_INTERVAL = 8;
    private static final int ASSISTED_GROWTH_INTERVAL = 4;

    public LingmuSaplingBlock(TreeGrower grower, Properties properties) {
        super(grower, properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(NATURAL_GROWTH_INTERVAL) == 0) {
            super.randomTick(state, level, pos, random);
        }
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        // Both ordinary bone meal and cultivation techniques use this gate.
        return random.nextInt(ASSISTED_GROWTH_INTERVAL) == 0
                && super.isBonemealSuccess(level, random, pos, state);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getFluidState(pos).isEmpty()
                && level.getFluidState(pos.below()).isEmpty()
                && super.canSurvive(state, level, pos);
    }
}
