package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.item.QiPouchWorld;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps normal workstation menus valid while their real block is inside a pouch plot. */
@Mixin(AbstractContainerMenu.class)
public abstract class QiPouchMenuValidityMixin {
    @Inject(method = "stillValid", at = @At("HEAD"), cancellable = true)
    private static void immortalCultivation$allowRemotePouchMenu(
            ContainerLevelAccess access, Player player, Block targetBlock,
            CallbackInfoReturnable<Boolean> callback) {
        boolean pouchBlock = access.evaluate(
                (level, pos) -> QiPouchWorld.isRemoteInteraction(
                        player, level, pos), false);
        if (pouchBlock) callback.setReturnValue(true);
    }

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void immortalCultivation$keepOpenPouchOutOfItsOwnWorld(
            int slotId, int button, ClickType clickType, Player player,
            CallbackInfo callback) {
        if (QiPouchWorld.blocksSessionPouchMove(player,
                (AbstractContainerMenu)(Object)this,
                slotId, button, clickType)) {
            callback.cancel();
        }
    }
}
