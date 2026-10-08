package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.compat.UltramarineDoorWallCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds virtual wall connections for Ultramarine's overhanging door models. */
@Mixin(WallBlock.class)
public abstract class WallUltramarineDoorConnectionMixin {
    @Inject(method = "getStateForPlacement", at = @At("RETURN"),
            cancellable = true)
    private void immortalCultivation$connectPlacedWallToTallDoor(
            BlockPlaceContext context,
            CallbackInfoReturnable<BlockState> callback) {
        BlockState state = callback.getReturnValue();
        if (state != null) {
            callback.setReturnValue(
                    UltramarineDoorWallCompat.addVirtualDoorConnections(
                            state, context.getLevel(), context.getClickedPos()));
        }
    }

    @Inject(
            method = "updateShape(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("RETURN"), cancellable = true)
    private void immortalCultivation$connectUpdatedWallToTallDoor(
            BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos,
            CallbackInfoReturnable<BlockState> callback) {
        callback.setReturnValue(
                UltramarineDoorWallCompat.addVirtualDoorConnections(
                        callback.getReturnValue(), level, pos));
    }
}
