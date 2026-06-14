package com.example.immortal_cultivation_mod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import org.jetbrains.annotations.Nullable;

public class CultivationVineBlock extends VineBlock {
    public CultivationVineBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState existingState = context.getLevel().getBlockState(context.getClickedPos());
        boolean replacingSameBlock = existingState.is(this);
        BlockState state = replacingSameBlock ? existingState : defaultBlockState();

        for (Direction direction : context.getNearestLookingDirections()) {
            if (direction == Direction.DOWN) {
                continue;
            }

            BooleanProperty property = getPropertyForFace(direction);
            if (property != null && !state.getValue(property) && canSupportAtFace(context.getLevel(), context.getClickedPos(), direction)) {
                return state.setValue(property, true);
            }
        }

        return replacingSameBlock ? state : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(UP)) {
            if (canSupportAtFace(level, pos, Direction.UP)) {
                return true;
            }
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (state.getValue(getPropertyForFace(direction))) {
                if (canSupportAtFace(level, pos, direction)) {
                    return true;
                }
            }
        }

        BlockPos above = pos.above();
        return level.getBlockState(above).is(this);
    }

    private boolean canSupportAtFace(LevelReader level, BlockPos pos, Direction direction) {
        BlockPos supportPos = pos.relative(direction);
        BlockState supportState = level.getBlockState(supportPos);
        return supportState.isFaceSturdy(level, supportPos, direction.getOpposite());
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}
