package com.example.immortal_cultivation_mod.worldgen;

/** Tracks mineshaft pieces created by the abandoned spirit mine codec. */
public final class AbandonedSpiritMineGeneration {
    private static final ThreadLocal<Integer> DEPTH = new ThreadLocal<>();

    private AbandonedSpiritMineGeneration() {
    }

    static void begin() {
        Integer depth = DEPTH.get();
        DEPTH.set(depth == null ? 1 : depth + 1);
    }

    static void end() {
        Integer currentDepth = DEPTH.get();
        int depth = currentDepth == null ? 0 : currentDepth - 1;
        if (depth <= 0) {
            DEPTH.remove();
        } else {
            DEPTH.set(depth);
        }
    }

    public static boolean isActive() {
        Integer depth = DEPTH.get();
        return depth != null && depth > 0;
    }
}
