package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.event.SmallSectProtection;
import com.example.immortal_cultivation_mod.event.DeadRuinProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops non-player destruction APIs from bypassing a living Small Sect
 * guardian. This covers spell/external-Qi terrain damage and destruction
 * invoked by other mods in addition to the normal break and explosion events.
 */
@Mixin(Level.class)
public abstract class SmallSectBlockProtectionMixin {
    @Inject(
            method = "destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z",
            at = @At("HEAD"), cancellable = true)
    private void immortalCultivation$protectFromDestroy(
            BlockPos pos, boolean dropBlock, Entity breaker, int recursionLeft,
            CallbackInfoReturnable<Boolean> callback) {
        if (immortalCultivation$isProtected(pos)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(
            method = "removeBlock(Lnet/minecraft/core/BlockPos;Z)Z",
            at = @At("HEAD"), cancellable = true)
    private void immortalCultivation$protectFromRemove(
            BlockPos pos, boolean moving,
            CallbackInfoReturnable<Boolean> callback) {
        if (immortalCultivation$isProtected(pos)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(
            method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"), cancellable = true)
    private void immortalCultivation$protectFromBlockReplacement(
            BlockPos pos, BlockState newState, int flags, int recursionLeft,
            CallbackInfoReturnable<Boolean> callback) {
        Level level = (Level) (Object) this;
        BlockState currentState = level.getBlockState(pos);
        boolean replacesOriginalBlock = !currentState.isAir()
                && currentState.getBlock() != newState.getBlock();
        if (replacesOriginalBlock
                && !immortalCultivation$isAllowedWaterRemoval(currentState, newState)
                && immortalCultivation$isProtected(pos)) {
            callback.setReturnValue(false);
        }
    }

    /**
     * Water is not a structural block. Both an empty bucket collecting a
     * source and a fluid cleanup tick replacing flowing water with air must be
     * allowed without weakening protection for the surrounding solid blocks.
     */
    private boolean immortalCultivation$isAllowedWaterRemoval(
            BlockState currentState, BlockState newState) {
        return currentState.is(Blocks.WATER) && newState.isAir();
    }

    private boolean immortalCultivation$isProtected(BlockPos pos) {
        Level level = (Level) (Object) this;
        return level instanceof ServerLevel serverLevel
                && (SmallSectProtection.isLocked(serverLevel, pos)
                || DeadRuinProtection.isLocked(serverLevel, pos));
    }
}
