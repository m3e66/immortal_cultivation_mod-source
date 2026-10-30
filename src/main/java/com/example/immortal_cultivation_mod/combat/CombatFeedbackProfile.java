package com.example.immortal_cultivation_mod.combat;

import net.minecraft.util.Mth;

/** Shared, bounded severity rules for server events and client effect budgets. */
public final class CombatFeedbackProfile {
    public static final float HEAVY = .45F;
    public static final float MASSIVE = .80F;
    public static final float LAUNCH_DAMAGE = 32F;

    private CombatFeedbackProfile() {}

    public static boolean damagingImpact(float damage) {
        return Float.isFinite(damage) && damage >= LAUNCH_DAMAGE;
    }

    /** Absolute confirmed HP loss drives launch speed, hang time, and smoke consistently. */
    public static float launchIntensity(float damage) {
        if (!damagingImpact(damage)) return 0;
        return Mth.clamp((float) (.45 + .55 * Math.log(damage / (double) LAUNCH_DAMAGE) / Math.log(128)), .45F, 1F);
    }

    public static float hitIntensity(float damage, float maximumHealth) {
        if (!Float.isFinite(damage) || damage <= 0) return 0;
        float health = Float.isFinite(maximumHealth) ? Math.max(20, maximumHealth) : 20;
        // Absolute force matters at cultivation health scales; percentage keeps ordinary fights readable.
        double absolute = Math.log1p(damage / 8.0) / Math.log(101.0);
        double relative = Math.min(.60, damage / health * .80);
        return Mth.clamp((float) Math.max(.12 + absolute * .88, relative), .12F, 1F);
    }

    public static float landingIntensity(double descent, double downwardSpeed, int airTicks) {
        if (!Double.isFinite(descent) || !Double.isFinite(downwardSpeed)
                || airTicks < 4 || descent < 2.5 || downwardSpeed < .25) return 0;
        return Mth.clamp((float) (.18 + (descent - 2.5) * .055), .18F, 1F);
    }

    public static boolean heavy(float intensity) { return intensity >= HEAVY; }
    public static boolean massive(float intensity) { return intensity >= MASSIVE; }
}
