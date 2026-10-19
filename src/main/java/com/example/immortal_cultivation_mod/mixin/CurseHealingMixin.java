package com.example.immortal_cultivation_mod.mixin;

import com.example.immortal_cultivation_mod.curse.AnnihilationCurse;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Also covers direct setHealth healing used by pills, commands and other mods. */
@Mixin(LivingEntity.class)
public abstract class CurseHealingMixin {
    @Inject(method="setHealth",at=@At("HEAD"),cancellable=true)
    private void immortalCurse$blockHealing(float health,CallbackInfo ci) {
        if(AnnihilationCurse.blockHealthIncrease((LivingEntity)(Object)this,health))ci.cancel();
    }
}
