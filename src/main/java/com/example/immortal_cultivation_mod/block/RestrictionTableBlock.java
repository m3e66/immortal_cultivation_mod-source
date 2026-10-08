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
    private static final VoxelShape SHAPE = Shapes.or(
            // Low plinth and layered cabinet base.
            Block.box(0.15D, 0.0D, 0.15D, 15.85D, 2.3D, 15.85D),

            // Four cabinet corner posts.
            Block.box(0.5D, 1.85D, 0.5D, 3.0D, 6.65D, 3.0D),
            Block.box(13.0D, 1.85D, 0.5D, 15.5D, 6.65D, 3.0D),
            Block.box(0.5D, 1.85D, 13.0D, 3.0D, 6.65D, 15.5D),
            Block.box(13.0D, 1.85D, 13.0D, 15.5D, 6.65D, 15.5D),

            // Framed cabinet sides between the posts.
            Block.box(2.8D, 2.15D, 0.68D, 13.2D, 5.85D, 1.68D),
            Block.box(2.8D, 2.15D, 14.32D, 13.2D, 5.85D, 15.32D),
            Block.box(0.68D, 2.15D, 2.8D, 1.68D, 5.85D, 13.2D),
            Block.box(14.32D, 2.15D, 2.8D, 15.32D, 5.85D, 13.2D),

            // Main tabletop slab.
            Block.box(0.25D, 6.4D, 0.25D, 15.75D, 7.8D, 15.75D),

            // Raised outer rim and central restriction plate.
            Block.box(2.5D, 7.6D, 0.25D, 13.5D, 9.1D, 1.75D),
            Block.box(2.5D, 7.6D, 14.25D, 13.5D, 9.1D, 15.75D),
            Block.box(0.25D, 7.6D, 2.5D, 1.75D, 9.1D, 13.5D),
            Block.box(14.25D, 7.6D, 2.5D, 15.75D, 9.1D, 13.5D),
            Block.box(2.75D, 7.52D, 2.75D, 13.25D, 8.52D, 13.25D),

            // Front control housing protruding slightly beyond the cabinet.
            Block.box(4.3D, 2.7D, -0.02D, 11.7D, 5.3D, 0.98D),
            Block.box(6.75D, 4.05D, -0.58D, 9.25D, 6.25D, 0.42D)
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
