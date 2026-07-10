package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.block.ModouLineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ModouLineItem extends Item {
    private static final String TAG_HAS_FIRST = "ModouHasFirst";
    private static final String TAG_X = "ModouFirstX";
    private static final String TAG_Y = "ModouFirstY";
    private static final String TAG_Z = "ModouFirstZ";
    private static final int MAX_LINE_BLOCKS = 15;

    public ModouLineItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

        if (!tag.getBoolean(TAG_HAS_FIRST)) {
            tag.putBoolean(TAG_HAS_FIRST, true);
            tag.putInt(TAG_X, clicked.getX());
            tag.putInt(TAG_Y, clicked.getY());
            tag.putInt(TAG_Z, clicked.getZ());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        BlockPos first = new BlockPos(tag.getInt(TAG_X), tag.getInt(TAG_Y), tag.getInt(TAG_Z));
        stack.remove(DataComponents.CUSTOM_DATA);
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!canAnchor(level, first) || !canAnchor(level, clicked)) {
            return InteractionResult.FAIL;
        }
        Direction.Axis axis;
        int distance;
        if (first.getY() == clicked.getY() && first.getZ() == clicked.getZ() && first.getX() != clicked.getX()) {
            axis = Direction.Axis.X;
            distance = Math.abs(clicked.getX() - first.getX());
        } else if (first.getY() == clicked.getY() && first.getX() == clicked.getX() && first.getZ() != clicked.getZ()) {
            axis = Direction.Axis.Z;
            distance = Math.abs(clicked.getZ() - first.getZ());
        } else {
            return InteractionResult.FAIL;
        }
        int lineBlocks = distance - 1;
        if (lineBlocks < 1 || lineBlocks > MAX_LINE_BLOCKS) {
            return InteractionResult.FAIL;
        }
        Direction direction = axis == Direction.Axis.X
                ? (clicked.getX() > first.getX() ? Direction.EAST : Direction.WEST)
                : (clicked.getZ() > first.getZ() ? Direction.SOUTH : Direction.NORTH);
        BlockPos cursor = first.relative(direction);
        for (int i = 0; i < lineBlocks; i++) {
            if (!level.getBlockState(cursor).canBeReplaced()) {
                return InteractionResult.FAIL;
            }
            cursor = cursor.relative(direction);
        }
        BlockState line = ModBlocks.MODOU_LINE.get().defaultBlockState().setValue(ModouLineBlock.AXIS, axis);
        cursor = first.relative(direction);
        for (int i = 0; i < lineBlocks; i++) {
            level.setBlock(cursor, line, 3);
            cursor = cursor.relative(direction);
        }
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    private static boolean canAnchor(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && state.isCollisionShapeFullBlock(level, pos);
    }
}
