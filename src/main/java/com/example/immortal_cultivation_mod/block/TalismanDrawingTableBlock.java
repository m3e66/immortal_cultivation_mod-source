package com.example.immortal_cultivation_mod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Workstation for selecting a learned formula, tracing it, then injecting Qi. */
public final class TalismanDrawingTableBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape NORTH_SHAPE = createNorthShape();
    private static final VoxelShape EAST_SHAPE = rotateHorizontal(NORTH_SHAPE, Direction.EAST);
    private static final VoxelShape SOUTH_SHAPE = rotateHorizontal(NORTH_SHAPE, Direction.SOUTH);
    private static final VoxelShape WEST_SHAPE = rotateHorizontal(NORTH_SHAPE, Direction.WEST);

    public TalismanDrawingTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Follow the player's look direction exactly rather than facing back at them.
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TalismanDrawingTableBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof TalismanDrawingTableBlockEntity table) {
            serverPlayer.openMenu(table);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                            boolean movedByPiston) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof TalismanDrawingTableBlockEntity table) {
            Containers.dropContents(level, pos, table);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static VoxelShape shapeFor(Direction facing) {
        return switch (facing) {
            case EAST -> EAST_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    private static VoxelShape createNorthShape() {
        return Shapes.or(
                // Four feet and legs.
                Block.box(0.5D, 0.0D, 0.5D, 3.5D, 10.5D, 3.5D),
                Block.box(12.5D, 0.0D, 0.5D, 15.5D, 10.5D, 3.5D),
                Block.box(0.5D, 0.0D, 12.5D, 3.5D, 10.5D, 15.5D),
                Block.box(12.5D, 0.0D, 12.5D, 15.5D, 10.5D, 15.5D),

                // Lower wooden rails between the legs.
                Block.box(2.75D, 1.925D, 0.925D, 13.25D, 3.175D, 2.175D),
                Block.box(2.75D, 1.925D, 13.825D, 13.25D, 3.175D, 15.075D),
                Block.box(0.925D, 1.925D, 2.75D, 2.175D, 3.175D, 13.25D),
                Block.box(13.825D, 1.925D, 2.75D, 15.075D, 3.175D, 13.25D),

                // Main tabletop and its raised edge.
                Block.box(0.0D, 10.5D, 0.0D, 16.0D, 12.7D, 16.0D),
                Block.box(0.0D, 12.65D, 0.0D, 16.0D, 13.65D, 1.1D),
                Block.box(0.0D, 12.65D, 14.9D, 16.0D, 13.65D, 16.0D),
                Block.box(0.0D, 12.65D, 0.55D, 1.1D, 13.65D, 15.45D),
                Block.box(14.9D, 12.65D, 0.55D, 16.0D, 13.65D, 15.45D),

                // Ink/tool trays and brush rack above the tabletop.
                Block.box(10.4D, 13.05D, 9.9D, 14.9D, 15.15D, 13.5D),
                Block.box(10.8D, 13.25D, 3.15D, 14.5D, 15.15D, 6.85D),
                Block.box(0.5D, 13.55D, 8.4D, 5.5D, 16.55D, 11.6D),
                Block.box(1.45D, 16.55D, 9.5D, 2.45D, 20.0D, 10.5D),
                Block.box(3.45D, 16.55D, 9.5D, 4.45D, 20.0D, 10.5D)
        ).optimize();
    }

    private static VoxelShape rotateHorizontal(VoxelShape source, Direction facing) {
        if (facing == Direction.NORTH) {
            return source;
        }

        VoxelShape[] rotated = {Shapes.empty()};
        source.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            VoxelShape box = switch (facing) {
                case EAST -> Shapes.box(
                        1.0D - maxZ, minY, minX,
                        1.0D - minZ, maxY, maxX);
                case SOUTH -> Shapes.box(
                        1.0D - maxX, minY, 1.0D - maxZ,
                        1.0D - minX, maxY, 1.0D - minZ);
                case WEST -> Shapes.box(
                        minZ, minY, 1.0D - maxX,
                        maxZ, maxY, 1.0D - minX);
                default -> Shapes.box(minX, minY, minZ, maxX, maxY, maxZ);
            };
            rotated[0] = Shapes.or(rotated[0], box);
        });
        return rotated[0].optimize();
    }
}
