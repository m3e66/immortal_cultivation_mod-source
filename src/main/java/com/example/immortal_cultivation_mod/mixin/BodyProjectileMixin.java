package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.attachment.BodyTypeEffects;
import com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public abstract class BodyProjectileMixin {
    @Inject(method = "setOwner", at = @At("TAIL"))
    private void immortalCultivation$saveSpellElement(Entity owner, CallbackInfo ci) {
        Projectile projectile = (Projectile)(Object)this;
        if (projectile.level().isClientSide) return;
        String element = CenterEffectSupport.currentSpellElement(owner);
        projectile.getPersistentData().remove(BodyTypeEffects.PROJECTILE_ELEMENT);
        if (!element.isBlank()) projectile.getPersistentData().putString(BodyTypeEffects.PROJECTILE_ELEMENT, element);
    }
}
