package com.example.immortal_cultivation_mod.client;

import net.minecraft.nbt.CompoundTag;

/** Client presentation only. Mechanical stages and penalties remain server-authoritative. */
public final class PillToxicityVisualState {
    private static final String[] STAGES = {"Stage0", "Stage1", "Stage2", "Stage3", "Stage4"};
    public static void targets(double value, CompoundTag rules, float[] result) {
        float previous = 1;
        for (int i = 0; i < 4; i++) {
            double start = rules.getCompound(STAGES[i + 1]).getInt("Threshold");
            double end = i == 3 ? 100 : rules.getCompound(STAGES[i + 2]).getInt("Threshold");
            float amount = smooth((float)((value - start) / Math.max(1, end - start)));
            if (i > 0) result[i - 1] = previous * (1 - amount);
            previous = amount;
        }
        result[3] = previous;
    }

    public static void advance(float[] weights, float[] targets, double seconds) {
        float blend = (float)-Math.expm1(-Math.max(0, seconds) / .45);
        for (int i = 0; i < weights.length; i++) {
            weights[i] += (targets[i] - weights[i]) * blend;
            if (Math.abs(weights[i] - targets[i]) < .00001F) weights[i] = targets[i];
        }
    }

    public static float smooth(float value) {
        float t = Math.max(0, Math.min(1, value));
        return t * t * (3 - 2 * t);
    }

    /** First synchronization sets a baseline. Recovered stages must be stable before repeating a warning. */
    public static final class Warning {
        private boolean initialized;
        private int previous, warned;
        private double lowerSince = -1, started = -100, lastWarning = -100;

        public boolean accept(int stage, double now) {
            if (!initialized) {
                initialized = true;
                previous = warned = stage;
                return false;
            }
            if (stage < previous && stage < warned && lowerSince < 0) lowerSince = now;
            boolean recovered = lowerSince >= 0 && now - lowerSince >= 2;
            boolean trigger = stage > previous && stage > 0
                    && (stage > warned || recovered && now - lastWarning >= 5);
            if (trigger) {
                started = lastWarning = now;
                warned = stage;
            }
            if (stage >= warned) lowerSince = -1;
            previous = stage;
            return trigger;
        }

        public float opacity(double now) {
            double age = now - started;
            if (age < 0 || age >= 3.8) return 0;
            return smooth((float)(age / .6)) * (1 - smooth((float)((age - 2.6) / 1.2)));
        }
    }

    private PillToxicityVisualState() {}
}
