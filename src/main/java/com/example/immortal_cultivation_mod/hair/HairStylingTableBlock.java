package com.example.immortal_cultivation_mod.hair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class HairStylingTableBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape BASE_SHAPE = createShape();
    private static final VoxelShape EAST_SHAPE = rotateShape(BASE_SHAPE);
    private static final VoxelShape SOUTH_SHAPE = rotateShape(EAST_SHAPE);
    private static final VoxelShape WEST_SHAPE = rotateShape(SOUTH_SHAPE);

    private static VoxelShape createShape() {
        // Broad tabletop, four straight legs, and one mirror slab. Decorative tools and carved
        // details do not need separate collision boxes; the space below the tabletop stays open.
        return Shapes.or(
                box(.2, 8.35, .54, 15.8, 12.65, 15.3),
                box(1.29, 0, 1.69, 2.91, 8.35, 3.31),
                box(13.09, 0, 1.69, 14.71, 8.35, 3.31),
                box(1.29, 0, 12.59, 2.91, 8.35, 14.21),
                box(13.09, 0, 12.59, 14.71, 8.35, 14.21),
                box(1.45, 12.65, 12.53, 14.55, 27.07, 13.925)).optimize();
    }

    public HairStylingTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return switch (s.getValue(FACING)) {
            case EAST -> EAST_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> BASE_SHAPE;
        };
    }

    private static VoxelShape rotateShape(VoxelShape source) {
        VoxelShape[] result = {Shapes.empty()};
        source.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                result[0] = Shapes.or(result[0], Shapes.box(
                        1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)));
        return result[0].optimize();
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new HairMenu(id, inv, pos), Component.translatable("hair.table")));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
