package com.example.immortal_cultivation_mod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Controller block for a wall-sized medicine cabinet.
 *
 * <p>The placed controller is the bottom-right footprint cell.  The remaining
 * cells are {@link MedicineCabinetPartBlock} instances which forward precise
 * hit coordinates and all destruction back to this controller.</p>
 */
public final class MedicineCabinetBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final ThreadLocal<Boolean> CLEANING_PARTS =
            ThreadLocal.withInitial(() -> false);

    private static final VoxelShape NORTH_SHAPE = Block.box(0, 0, 0, 16, 16, 10);
    private static final VoxelShape SOUTH_SHAPE = Block.box(0, 0, 6, 16, 16, 16);
    private static final VoxelShape WEST_SHAPE = Block.box(0, 0, 0, 10, 16, 16);
    private static final VoxelShape EAST_SHAPE = Block.box(6, 0, 0, 16, 16, 16);

    private final Variant variant;

    public MedicineCabinetBlock(Variant variant, Properties properties) {
        super(properties);
        this.variant = variant;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public Variant variant() {
        return variant;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos controllerPos = context.getClickedPos();
        if (!canPlaceFootprint(context.getLevel(), controllerPos, facing, context.getPlayer())) {
            return null;
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    private boolean canPlaceFootprint(Level level, BlockPos controllerPos, Direction facing,
                                      @Nullable Player player) {
        for (int row = 0; row < variant.blockHeight(); row++) {
            for (int column = 0; column < variant.blockWidth(); column++) {
                BlockPos partPos = partPos(controllerPos, facing, variant, column, row);
                if (partPos.equals(controllerPos)) continue;
                if (partPos.getY() < level.getMinBuildHeight()
                        || partPos.getY() >= level.getMaxBuildHeight()
                        || !level.getWorldBorder().isWithinBounds(partPos)
                        || !level.getBlockState(partPos).canBeReplaced()
                        || player != null && !level.mayInteract(player, partPos)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) return;

        Direction facing = state.getValue(FACING);
        boolean failed = false;
        for (int row = 0; row < variant.blockHeight() && !failed; row++) {
            for (int column = 0; column < variant.blockWidth(); column++) {
                BlockPos partPos = partPos(pos, facing, variant, column, row);
                if (partPos.equals(pos)) continue;
                BlockState partState = ModBlocks.MEDICINE_CABINET_PART.get()
                        .defaultBlockState()
                        .setValue(MedicineCabinetPartBlock.FACING, facing)
                        .setValue(MedicineCabinetPartBlock.COLUMN, column)
                        .setValue(MedicineCabinetPartBlock.ROW, row)
                        .setValue(MedicineCabinetPartBlock.LARGE,
                                variant == Variant.LARGE);
                if (!level.setBlock(partPos, partState, Block.UPDATE_ALL)) {
                    failed = true;
                    break;
                }
            }
        }
        if (failed) {
            level.destroyBlock(pos, true, placer);
        }
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

    static VoxelShape shapeFor(Direction facing) {
        return switch (facing) {
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MedicineCabinetBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        int column = variant.blockWidth() - 1;
        return interact(state, level, pos, pos, column, 0, variant, player, hitResult);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hitResult) {
        int column = variant.blockWidth() - 1;
        InteractionResult result = interact(
                state, level, pos, pos, column, 0, variant, player, hitResult);
        return result.consumesAction()
                ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    static InteractionResult interact(BlockState clickedState, Level level, BlockPos clickedPos,
                                      BlockPos controllerPos, int blockColumn, int blockRow,
                                      Variant variant, Player player,
                                      BlockHitResult hitResult) {
        Direction facing = clickedState.getValue(FACING);
        if (hitResult.getDirection() != facing) {
            return InteractionResult.PASS;
        }

        int drawer = drawerAtHit(
                clickedPos, blockColumn, blockRow, facing, variant, hitResult);
        if (drawer < 0) return InteractionResult.PASS;
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(controllerPos) instanceof MedicineCabinetBlockEntity cabinet) {
            if (cabinet.activeDrawer() == drawer) {
                cabinet.openDrawer(serverPlayer, drawer);
            } else {
                cabinet.pullDrawer(drawer);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static int drawerAtHit(BlockPos clickedPos, int blockColumn, int blockRow,
                                   Direction facing, Variant variant,
                                   BlockHitResult hitResult) {
        double fraction = switch (facing.getClockWise()) {
            case EAST -> hitResult.getLocation().x - clickedPos.getX();
            case WEST -> clickedPos.getX() + 1.0D - hitResult.getLocation().x;
            case SOUTH -> hitResult.getLocation().z - clickedPos.getZ();
            case NORTH -> clickedPos.getZ() + 1.0D - hitResult.getLocation().z;
            default -> 0.5D;
        };
        fraction = Math.max(0.0D, Math.min(0.999999D, fraction));
        double cabinetX = blockColumn + fraction;
        int footprintDrawerColumn = Math.min(variant.drawerColumns() - 1,
                (int) Math.floor(cabinetX * variant.drawerColumns()
                        / variant.blockWidth()));
        // Footprint columns run from the cabinet's visual right toward its
        // controller on the visual left, while Model Kit drawer bones are
        // numbered left-to-right. Convert between those two coordinate spaces.
        int drawerColumn = variant.drawerColumns() - 1
                - footprintDrawerColumn;

        double fractionY = hitResult.getLocation().y - clickedPos.getY();
        fractionY = Math.max(0.0D, Math.min(0.999999D, fractionY));
        int drawerRow = Math.min(variant.drawerRows() - 1,
                (int) Math.floor((blockRow + fractionY) * variant.drawerRows()
                        / variant.blockHeight()));
        return drawerRow * variant.drawerColumns() + drawerColumn;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                            boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof MedicineCabinetBlockEntity cabinet) {
                Containers.dropContents(level, pos, cabinet);
                level.updateNeighbourForOutputSignal(pos, this);
            }
            removeParts(level, pos, state.getValue(FACING), variant);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static void removeParts(Level level, BlockPos controllerPos, Direction facing,
                                    Variant variant) {
        if (level.isClientSide) return;
        CLEANING_PARTS.set(true);
        try {
            for (int row = 0; row < variant.blockHeight(); row++) {
                for (int column = 0; column < variant.blockWidth(); column++) {
                    BlockPos partPos = partPos(
                            controllerPos, facing, variant, column, row);
                    if (!partPos.equals(controllerPos)
                            && level.getBlockState(partPos)
                            .is(ModBlocks.MEDICINE_CABINET_PART.get())) {
                        level.removeBlock(partPos, false);
                    }
                }
            }
        } finally {
            CLEANING_PARTS.set(false);
        }
    }

    static boolean isCleaningParts() {
        return CLEANING_PARTS.get();
    }

    static BlockPos partPos(BlockPos controllerPos, Direction facing, Variant variant,
                            int column, int row) {
        int distanceLeft = variant.blockWidth() - 1 - column;
        return controllerPos.relative(facing.getCounterClockWise(), distanceLeft).above(row);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide || type != ModBlockEntities.MEDICINE_CABINET.get()) {
            return null;
        }
        return (tickLevel, tickPos, tickState, blockEntity) ->
                MedicineCabinetBlockEntity.clientTick(
                        tickLevel, tickPos, tickState,
                        (MedicineCabinetBlockEntity) blockEntity);
    }

    public enum Variant {
        SMALL(2, 1, 3, 1, "medicine_cabinet_small"),
        LARGE(5, 3, 9, 3, "medicine_cabinet_large");

        private final int blockWidth;
        private final int blockHeight;
        private final int drawerColumns;
        private final int drawerRows;
        private final String modelName;

        Variant(int blockWidth, int blockHeight, int drawerColumns,
                int drawerRows, String modelName) {
            this.blockWidth = blockWidth;
            this.blockHeight = blockHeight;
            this.drawerColumns = drawerColumns;
            this.drawerRows = drawerRows;
            this.modelName = modelName;
        }

        public int blockWidth() {
            return blockWidth;
        }

        public int blockHeight() {
            return blockHeight;
        }

        public int drawerColumns() {
            return drawerColumns;
        }

        public int drawerRows() {
            return drawerRows;
        }

        public int drawerCount() {
            return drawerColumns * drawerRows;
        }

        public int slotCount() {
            return drawerCount() * MedicineCabinetBlockEntity.SLOTS_PER_DRAWER;
        }

        public String modelName() {
            return modelName;
        }
    }
}
