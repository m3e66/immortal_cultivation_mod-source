package com.example.immortal_cultivation_mod.combat;

/** Pure timing rules shared by camera feedback and regression tests. */
public final class CombatFeedbackTiming {
    private long start = Long.MIN_VALUE;
    private long nextAllowed = Long.MIN_VALUE;
    public static final int IMPULSE_TICKS = 7;
    public static final int QUIET_TICKS = 9;

    public boolean trigger(long tick) {
        if (tick < nextAllowed) return false;
        start = tick;
        nextAllowed = tick + IMPULSE_TICKS + QUIET_TICKS;
        return true;
    }

    public double age(long tick, double partial) {
        return start == Long.MIN_VALUE ? Double.POSITIVE_INFINITY : tick - start + partial;
    }

    public float value(long tick, double partial) {
        return decay(age(tick, partial), IMPULSE_TICKS);
    }

    public void clear() { start = nextAllowed = Long.MIN_VALUE; }

    public static float decay(double age, double lifetime) {
        if (!Double.isFinite(age) || age < 0 || age >= lifetime || lifetime <= 0) return 0;
        double remaining = 1 - age / lifetime;
        return (float) (remaining * remaining);
    }

    public static float distanceWeight(double distance, double radius) {
        if (!Double.isFinite(distance) || distance < 0 || radius <= 0) return 0;
        double remaining = Math.max(0, 1 - distance / radius);
        return (float) (remaining * remaining);
    }
}
