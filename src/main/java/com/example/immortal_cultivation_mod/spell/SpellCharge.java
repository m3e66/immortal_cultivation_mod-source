package com.example.immortal_cultivation_mod.spell;

/** Shared, server-authoritative scaling for prepared spell charge. */
public final class SpellCharge {
    public static final float NORMAL = 1.0F;
    public static final float MAXIMUM = 2.0F;

    private SpellCharge() {
    }

    public static float clamp(float chargeScale) {
        if (!Float.isFinite(chargeScale)) {
            return NORMAL;
        }
        return Math.max(NORMAL, Math.min(MAXIMUM, chargeScale));
    }

    public static boolean isFull(float chargeScale) {
        return clamp(chargeScale) >= MAXIMUM - 0.001F;
    }

    /** Linear Qi scaling: 100% = x1 and 200% = x2. */
    public static int qiCost(int baseCost, float chargeScale) {
        if (baseCost <= 0) {
            return 0;
        }
        return Math.max(1, Math.round(baseCost * clamp(chargeScale)));
    }

    /** Linear damage scaling: 100% = x1 and 200% = x1.5. */
    public static float damageMultiplier(float chargeScale) {
        return 1.0F + 0.5F * (clamp(chargeScale) - 1.0F);
    }

    public static float damage(float baseDamage, float chargeScale) {
        return baseDamage * damageMultiplier(chargeScale);
    }
}
