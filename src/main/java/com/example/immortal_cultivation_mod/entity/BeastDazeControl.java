package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.effect.ModEffects;
import net.minecraft.world.entity.LivingEntity;

/** Shared gate for cultivation-beast moves that must stop while dazed. */
public final class BeastDazeControl {
    private BeastDazeControl() {
    }

    public static boolean blocksMoves(LivingEntity beast) {
        return beast.hasEffect(ModEffects.DAZE);
    }
}
