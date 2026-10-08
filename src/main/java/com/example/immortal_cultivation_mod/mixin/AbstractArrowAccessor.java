package com.example.immortal_cultivation_mod.mixin;

import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Keeps arrows embedded in a moving virtual block instead of falling through air. */
@Mixin(AbstractArrow.class)
public interface AbstractArrowAccessor {
    @Accessor("inGround")
    void immortalCultivation$setInGround(boolean inGround);
}
