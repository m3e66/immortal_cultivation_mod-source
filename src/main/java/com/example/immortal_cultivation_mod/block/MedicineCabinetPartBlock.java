package com.example.immortal_cultivation_mod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Invisible footprint cell belonging to a medicine-cabinet controller. */
public final class MedicineCabinetPartBlock extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty COLUMN = IntegerProperty.create("column", 0, 4);
    public static final IntegerProperty ROW = IntegerProperty.create("row", 0, 2);
    public static final BooleanProperty LARGE = BooleanProperty.create("large");

    public MedicineCabinetPartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(COLUMN, 0)
                .setValue(ROW, 0)
                .setValue(LARGE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, COLUMN, ROW, LARGE);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return MedicineCabinetBlock.shapeFor(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return MedicineCabinetBlock.shapeFor(state.getValue(FACING));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        return interact(state, level, pos, player, hitResult);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hitResult) {
        InteractionResult result = interact(state, level, pos, player, hitResult);
        return result.consumesAction()
                ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private InteractionResult interact(BlockState state, Level level, BlockPos pos,
                                       Player player, BlockHitResult hitResult) {
        MedicineCabinetBlock.Variant variant = state.getValue(LARGE)
                ? MedicineCabinetBlock.Variant.LARGE
                : MedicineCabinetBlock.Variant.SMALL;
        BlockPos controllerPos = controllerPos(state, pos, variant);
        if (!(level.getBlockState(controllerPos).getBlock() instanceof MedicineCabinetBlock)) {
            return InteractionResult.PASS;
        }
        return MedicineCabinetBlock.interact(
                state, level, pos, controllerPos,
                state.getValue(COLUMN), state.getValue(ROW), variant,
                player, hitResult);
    }

    static BlockPos controllerPos(BlockState state, BlockPos partPos,
                                  MedicineCabinetBlock.Variant variant) {
        Direction facing = state.getValue(FACING);
        int distanceRight = variant.blockWidth() - 1 - state.getValue(COLUMN);
        return partPos.relative(facing.getClockWise(), distanceRight)
                .below(state.getValue(ROW));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                            boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && !MedicineCabinetBlock.isCleaningParts()) {
            MedicineCabinetBlock.Variant variant = state.getValue(LARGE)
                    ? MedicineCabinetBlock.Variant.LARGE
                    : MedicineCabinetBlock.Variant.SMALL;
            BlockPos controllerPos = controllerPos(state, pos, variant);
            if (level.getBlockState(controllerPos).getBlock() instanceof MedicineCabinetBlock) {
                level.destroyBlock(controllerPos, true);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}

