package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.item.QiPouchWorld;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps real remote block-entity inventories open without weakening normal distance checks. */
@Mixin(Container.class)
public interface QiPouchContainerValidityMixin {
    @Inject(
            method = "stillValidBlockEntity(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/player/Player;)Z",
            at = @At("HEAD"), cancellable = true)
    private static void immortalCultivation$allowRemotePouchContainer(
            BlockEntity blockEntity, Player player,
            CallbackInfoReturnable<Boolean> callback) {
        if (blockEntity.getLevel() != null
                && QiPouchWorld.isRemoteInteraction(player,
                blockEntity.getLevel(), blockEntity.getBlockPos())) {
            callback.setReturnValue(true);
        }
    }
}
