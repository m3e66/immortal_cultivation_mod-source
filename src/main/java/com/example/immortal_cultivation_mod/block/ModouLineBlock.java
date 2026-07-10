package com.example.immortal_cultivation_mod.block;

import com.example.immortal_cultivation_mod.spell.UndeadControl;
import com.example.immortal_cultivation_mod.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ModouLineBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final VoxelShape X_SHAPE = Block.box(0.0D, 0.0D, 6.5D, 16.0D, 1.0D, 9.5D);
    private static final VoxelShape Z_SHAPE = Block.box(6.5D, 0.0D, 0.0D, 9.5D, 1.0D, 16.0D);

    public ModouLineBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.X ? X_SHAPE : Z_SHAPE;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living && UndeadControl.isUndeadServantType(living)) {
            if (UndeadControl.ignoresModouLine(living)) {
                level.removeBlock(pos, false);
                return;
            }
            UndeadRepel.bounceFrom(living, pos);
            UndeadControl.stunFromRepel(living, 20);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5D, pos.getY() + 0.08D, pos.getZ() + 0.5D,
                        22, 0.35D, 0.05D, 0.35D, 0.02D);
            }
            living.hurt(level.damageSources().inFire(), 4.0F);
            living.igniteForSeconds(2);
        }
        super.entityInside(state, level, pos, entity);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        if (!level.isClientSide && !hasBothAnchors(level, pos, state.getValue(AXIS))) {
            Block.popResource(level, pos, ModItems.MODOU_LINE.get().getDefaultInstance());
            removeWholeLine(level, pos, state.getValue(AXIS));
        }
        super.neighborChanged(state, level, pos, block, fromPos, moving);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!level.isClientSide() && level instanceof Level realLevel && !hasBothAnchors(realLevel, pos, state.getValue(AXIS))) {
            Block.popResource(realLevel, pos, ModItems.MODOU_LINE.get().getDefaultInstance());
            removeWholeLine(realLevel, pos, state.getValue(AXIS));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90, COUNTERCLOCKWISE_90 -> state.cycle(AXIS);
            default -> state;
        };
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state;
    }

    private static boolean hasBothAnchors(Level level, BlockPos pos, Direction.Axis axis) {
        Direction positive = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        Direction negative = positive.getOpposite();
        return findAnchor(level, pos, positive, axis) && findAnchor(level, pos, negative, axis);
    }

    private static boolean findAnchor(Level level, BlockPos start, Direction direction, Direction.Axis axis) {
        BlockPos cursor = start.relative(direction);
        for (int i = 0; i < 16; i++) {
            BlockState state = level.getBlockState(cursor);
            if (state.getBlock() instanceof ModouLineBlock && state.getValue(AXIS) == axis) {
                cursor = cursor.relative(direction);
                continue;
            }
            return !state.isAir() && state.isCollisionShapeFullBlock(level, cursor);
        }
        return false;
    }

    private static void removeWholeLine(Level level, BlockPos pos, Direction.Axis axis) {
        removeLineDirection(level, pos, axis, axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH);
        removeLineDirection(level, pos, axis, axis == Direction.Axis.X ? Direction.WEST : Direction.NORTH);
        if (level.getBlockState(pos).getBlock() instanceof ModouLineBlock) {
            level.removeBlock(pos, false);
        }
    }

    private static void removeLineDirection(Level level, BlockPos pos, Direction.Axis axis, Direction direction) {
        BlockPos cursor = pos.relative(direction);
        while (level.getBlockState(cursor).getBlock() instanceof ModouLineBlock
                && level.getBlockState(cursor).getValue(AXIS) == axis) {
            level.removeBlock(cursor, false);
            cursor = cursor.relative(direction);
        }
    }
}
