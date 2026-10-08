package com.example.immortal_cultivation_mod.block;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import com.example.immortal_cultivation_mod.event.DeadRuinProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A persistent cultivator skeleton. Its editable GeckoLib model uses the
 * vanilla skeleton texture; the block state stores its direction and one of
 * three dead poses.
 */
public final class DeadSkeletonBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty POSE = IntegerProperty.create("pose", 0, 2);
    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 4.0D, 15.0D);

    public DeadSkeletonBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POSE, 0));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(POSE, context.getLevel().getRandom().nextInt(3));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POSE);
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
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
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
        return new DeadSkeletonBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.DEAD_SKELETON.get()) {
            return null;
        }
        return (tickLevel, tickPos, tickState, blockEntity) ->
                DeadSkeletonBlockEntity.serverTick(
                        tickLevel, tickPos, tickState,
                        (DeadSkeletonBlockEntity) blockEntity);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (tryCycleRealm(level, pos, player)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        openInventory(level, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hitResult) {
        return tryCycleRealm(level, pos, player)
                ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : openInventory(level, pos, player);
    }

    private static ItemInteractionResult openInventory(
            Level level, BlockPos pos, Player player) {
        if (!level.isClientSide
                && player instanceof ServerPlayer serverPlayer) {
            DeadRuinProtection.skeletonOpened(serverPlayer.serverLevel(), pos, serverPlayer);
            if (level.getBlockEntity(pos) instanceof DeadSkeletonBlockEntity skeleton) {
                serverPlayer.openMenu(skeleton);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean tryCycleRealm(Level level, BlockPos pos, Player player) {
        if (!player.isShiftKeyDown()) {
            return false;
        }
        // Match the other Debug Eye block controls: acknowledge the predicted
        // click on the client, but keep permission and state changes server-side.
        if (level.isClientSide) {
            return true;
        }
        if (!(player instanceof ServerPlayer serverPlayer)
                || !ModPayloads.isDebugEyeEnabled(serverPlayer)
                || !(level.getBlockEntity(pos) instanceof DeadSkeletonBlockEntity skeleton)) {
            return false;
        }

        String realm = skeleton.cycleRealm();
        serverPlayer.displayClientMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID + ".dead_skeleton_realm", realm), true);
        return true;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos,
                            BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos)
                instanceof DeadSkeletonBlockEntity skeleton) {
            Containers.dropContents(level, pos, skeleton);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
