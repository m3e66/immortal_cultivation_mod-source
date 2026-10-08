package com.example.immortal_cultivation_mod.entity;

import net.minecraft.world.damagesource.DamageSource;

/** Combat metadata shared by real spell projectiles that pierce defenses. */
public interface DefensePiercingProjectile {
    /** Fraction of armor effectiveness ignored, in the inclusive range 0..1. */
    float defenseBypass();

    /** Multiplier applied to shield durability/energy consumption. */
    default float shieldDamageMultiplier() {
        return 1.0F;
    }

    static DefensePiercingProjectile from(DamageSource source) {
        return source != null && source.getDirectEntity() instanceof DefensePiercingProjectile projectile
                ? projectile : null;
    }

    static float defenseBypass(DamageSource source) {
        DefensePiercingProjectile projectile = from(source);
        return projectile == null ? com.example.immortal_cultivation_mod.companion.XuanjinCombat.penetration(source)
                : Math.max(0.0F, Math.min(1.0F, projectile.defenseBypass()));
    }

    static float shieldMultiplier(DamageSource source) {
        DefensePiercingProjectile projectile = from(source);
        return projectile == null ? 1.0F
                : Math.max(0.0F, projectile.shieldDamageMultiplier());
    }
}
