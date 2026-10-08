package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.item.WeaponTribulation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class WeaponTribulationHopperMixin {
    @Inject(method = "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/entity/item/ItemEntity;)Z", at = @At("HEAD"), cancellable = true)
    private static void preventTrialCapture(Container container, ItemEntity item, CallbackInfoReturnable<Boolean> cir) {
        if (WeaponTribulation.active(item.getItem())) cir.setReturnValue(false);
    }
}
