package com.example.immortal_cultivation_mod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class CultivationVineBlock extends VineBlock {
    public CultivationVineBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (state.getValue(getPropertyForFace(direction))) {
                BlockPos supportPos = pos.relative(direction.getOpposite());
                BlockState supportState = level.getBlockState(supportPos);
                if (supportState.isFaceSturdy(level, supportPos, direction)) {
                    return true;
                }
            }
        }

        BlockPos above = pos.above();
        return level.getBlockState(above).is(this);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}