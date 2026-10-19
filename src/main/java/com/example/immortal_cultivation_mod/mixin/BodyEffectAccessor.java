package com.example.immortal_cultivation_mod.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MobEffectInstance.class)
public interface BodyEffectAccessor {
    @Accessor("duration") void immortalCultivation$duration(int ticks);
    @Accessor("hiddenEffect") MobEffectInstance immortalCultivation$hidden();
    @Accessor("hiddenEffect") void immortalCultivation$hidden(MobEffectInstance hidden);
}
