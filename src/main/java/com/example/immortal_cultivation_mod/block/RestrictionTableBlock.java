package com.example.immortal_cultivation_mod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class RestrictionTableBlock extends Block implements EntityBlock {
    // Matches all 22 structural cuboids, including GeckoLib's reflected X coordinates.
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2.82500D, 0.00000D, 1.70000D, 13.17500D, 1.20000D, 15.30000D),
            Block.box(3.12500D, 1.20000D, 2.10000D, 12.87500D, 1.90000D, 14.90000D),
            Block.box(3.65000D, 1.90000D, 3.65000D, 12.35000D, 3.10000D, 14.15000D),
            Block.box(4.51250D, 3.10000D, 4.80000D, 11.48750D, 4.00000D, 13.00000D),
            Block.box(5.33750D, 4.00000D, 5.55000D, 10.66250D, 5.10000D, 12.25000D),
            Block.box(5.93750D, 5.10000D, 6.00000D, 10.06250D, 8.50000D, 11.80000D),
            Block.box(5.52500D, 8.47500D, 5.65000D, 10.47500D, 9.12500D, 12.15000D),
            Block.box(5.07500D, 9.10000D, 5.30000D, 10.92500D, 10.30000D, 12.50000D),
            Block.box(4.73750D, 10.25000D, 6.05000D, 11.26250D, 13.55000D, 12.75000D),
            Block.box(4.55000D, 13.43000D, 6.03750D, 11.26250D, 13.65000D, 12.75000D),
            Block.box(4.73750D, 11.25000D, 12.75000D, 11.26250D, 13.55000D, 13.65000D),
            Block.box(4.73750D, 11.25000D, 14.70000D, 11.26250D, 13.55000D, 15.80000D),
            Block.box(7.13750D, 11.25000D, 13.65000D, 11.26250D, 13.55000D, 14.70000D),
            Block.box(4.73750D, 11.25000D, 13.65000D, 6.31250D, 13.55000D, 14.70000D),
            Block.box(6.31250D, 11.25000D, 13.65000D, 7.13750D, 11.41000D, 14.70000D),
            Block.box(5.64383D, 10.72568D, 1.22321D, 10.35617D, 13.30669D, 2.80535D),
            Block.box(6.11139D, 10.92843D, 0.10893D, 9.88861D, 13.08289D, 1.69107D),
            Block.box(6.61531D, 11.22974D, -1.00536D, 9.38468D, 12.90358D, 0.57678D),
            Block.box(7.18108D, 11.64946D, -2.11964D, 8.81892D, 12.74518D, -0.53750D),
            Block.box(7.51056D, 11.94393D, -2.67679D, 8.48944D, 12.67123D, -2.09465D),
            Block.box(4.73750D, 10.25000D, 4.05000D, 11.26250D, 13.55000D, 6.05000D),
            Block.box(5.42000D, 10.69583D, 2.33750D, 10.58000D, 13.50000D, 5.10000D)
    ).optimize();

    public RestrictionTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RestrictionTableBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof RestrictionTableBlockEntity table) {
            serverPlayer.openMenu(table);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                            boolean movedByPiston) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof RestrictionTableBlockEntity table) {
            Containers.dropContents(level, pos, table);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return level.isClientSide ? null : (tickLevel, pos, tickState, blockEntity) -> {
            if (blockEntity instanceof RestrictionTableBlockEntity table) {
                RestrictionTableBlockEntity.serverTick(tickLevel, pos, table);
            }
        };
    }
}
