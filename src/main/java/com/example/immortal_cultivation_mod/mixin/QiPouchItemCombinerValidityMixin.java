package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.item.QiPouchWorld;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps anvil, grindstone, and smithing menus open for their real pouch blocks. */
@Mixin(ItemCombinerMenu.class)
public abstract class QiPouchItemCombinerValidityMixin {
    @Shadow
    @Final
    protected ContainerLevelAccess access;

    @Inject(method = "stillValid", at = @At("HEAD"), cancellable = true)
    private void immortalCultivation$allowRemotePouchCombiner(
            Player player, CallbackInfoReturnable<Boolean> callback) {
        boolean pouchBlock = access.evaluate(
                (level, pos) -> QiPouchWorld.isRemoteInteraction(
                        player, level, pos), false);
        if (pouchBlock) callback.setReturnValue(true);
    }
}
