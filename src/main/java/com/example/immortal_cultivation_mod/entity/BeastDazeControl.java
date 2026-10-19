package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.combat.ColdTideFreeze;
import net.minecraft.world.entity.LivingEntity;

/** Shared gate for beast moves and reactive abilities while dazed or fully frozen. */
public final class BeastDazeControl {
    private BeastDazeControl() {
    }

    public static boolean blocksMoves(LivingEntity beast) {
        return beast.hasEffect(ModEffects.DAZE)||ColdTideFreeze.isFrozen(beast);
    }
}
