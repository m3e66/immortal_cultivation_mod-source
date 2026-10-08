package com.example.immortal_cultivation_mod.spiritual;

import java.util.Map;

/**
 * Central balance list for player detection and hiding.
 *
 * <p>Change the numbers in this file to rebalance Spiritual Sense. Detection
 * multipliers describe what is being sensed. Hiding multipliers describe what
 * the target is currently using to conceal themselves. Multiple hiding sources
 * multiply together.</p>
 */
public final class SpiritualSenseMultipliers {
    /** Raw player detection = this base + mental stat * DETECTION_PER_MENTAL. */
    public static final double DETECTION_BASE = 66.0D;
    public static final double DETECTION_PER_MENTAL = 15.0D;

    /** Raw player hiding = this base + mental stat * HIDING_PER_MENTAL. */
    public static final double HIDING_BASE = 58.0D;
    public static final double HIDING_PER_MENTAL = 15.0D;

    /**
     * Per small-realm advantage. A detector one stage higher gains x1.12;
     * one stage lower receives x(1/1.12). Only applies to targets with realms.
     */
    public static final double DETECTION_REALM_MULTIPLIER_PER_STAGE = 1.12D;

    /**
     * Per small-realm advantage for the hiding player. A hider one stage
     * higher gains x1.12; one stage lower receives x(1/1.12).
     */
    public static final double HIDING_REALM_MULTIPLIER_PER_STAGE = 1.12D;

    /** Prevents extreme realm gaps from producing unusable floating values. */
    public static final double MIN_REALM_MULTIPLIER = 0.10D;
    public static final double MAX_REALM_MULTIPLIER = 10.0D;

    /**
     * Hiding luck multiplier is luck / 50. Therefore luck 50 = x1,
     * luck 100 = x2, and luck 0 is limited to MIN_LUCK_MULTIPLIER.
     */
    public static final double LUCK_FOR_X1_HIDING = 50.0D;
    public static final double MIN_LUCK_MULTIPLIER = 0.10D;
    public static final double MAX_LUCK_MULTIPLIER = 2.0D;

    /** One point of a persistent percentage bonus contributes one percent. */
    public static final double PERSISTENT_BONUS_PER_POINT = 0.01D;

    // Other detection multipliers used after the raw mental-stat value.
    public static final double METHOD_PROFICIENCY_MAX_BONUS = 0.24D;
    public static final double METHOD_PROFICIENCY_LOG_SCALE = 0.055D;
    public static final double CLEAR_HEART_METHOD = 1.10D;
    public static final double EXPANSION_FOCUS_MIN = 0.76D;
    public static final double EXPANSION_FOCUS_RANGE = 0.24D;
    public static final double SCAN_FOCUS_MIN = 0.82D;
    public static final double SCAN_FOCUS_RANGE = 0.18D;
    public static final double DISTANCE_EFFICIENCY_MIN = 0.38D;
    public static final double DISTANCE_EFFICIENCY_RANGE = 0.62D;
    public static final double DISTANCE_CURVE_POWER = 1.35D;

    // Directional-focus multipliers.
    public static final double DIRECTIONAL_AT_SAME_POINT = 1.45D;
    public static final double DIRECTIONAL_TIGHT_AIM = 1.55D;
    public static final double DIRECTIONAL_LOOSE_AIM = 1.16D;
    public static final double DIRECTIONAL_OUTSIDE_VIEW = 0.58D;

    // Solid-material interference multipliers.
    public static final double NORMAL_SOLID_INTERFERENCE = 0.82D;
    public static final double DENSE_SOLID_INTERFERENCE = 0.62D;
    public static final double SPIRIT_BLOCKING_INTERFERENCE = 0.32D;

    /** List of every detection target multiplier. */
    public static final Map<DetectionTarget, Double> DETECTION = Map.ofEntries(
            Map.entry(DetectionTarget.NORMAL_LIVING, 1.00D),
            Map.entry(DetectionTarget.HOSTILE, 1.00D),
            Map.entry(DetectionTarget.CULTIVATOR, 1.00D),
            Map.entry(DetectionTarget.DROPPED_TREASURE, 1.00D),
            Map.entry(DetectionTarget.SPIRITUAL_TREASURE_BLOCK, 1.00D),
            Map.entry(DetectionTarget.SPIRIT_VEIN, 1.00D),
            Map.entry(DetectionTarget.FORMATION, 1.00D),
            Map.entry(DetectionTarget.FORMATION_NODE, 1.00D),
            Map.entry(DetectionTarget.SOUL, 1.00D));

    /** List of every hiding-source multiplier. Multiple active entries stack. */
    public static final Map<HidingSource, Double> HIDING = Map.ofEntries(
            Map.entry(HidingSource.INVISIBLE, 1.70D),
            Map.entry(HidingSource.SNEAKING, 1.12D),
            Map.entry(HidingSource.UNDER_COVER, 1.10D),
            Map.entry(HidingSource.IN_WATER, 1.06D),
            Map.entry(HidingSource.DISGUISE_MASK, 1.38D),
            Map.entry(HidingSource.MUFFLE_TALISMAN, 1.10D),
            Map.entry(HidingSource.ACTIVELY_USING_QI, 0.70D),
            Map.entry(HidingSource.EXPOSED_BY_ACTION, 0.86D));

    private SpiritualSenseMultipliers() {
    }

    public static double detection(DetectionTarget target) {
        return DETECTION.getOrDefault(target, 1.0D);
    }

    public static double hiding(HidingSource source) {
        return HIDING.getOrDefault(source, 1.0D);
    }

    public static double realmMultiplier(int stageDifference, double perStage) {
        double value = Math.pow(Math.max(1.0D, perStage), stageDifference);
        return Math.max(MIN_REALM_MULTIPLIER,
                Math.min(MAX_REALM_MULTIPLIER, value));
    }

    public static double luckMultiplier(int luck) {
        double value = Math.max(0, luck) / LUCK_FOR_X1_HIDING;
        return Math.max(MIN_LUCK_MULTIPLIER,
                Math.min(MAX_LUCK_MULTIPLIER, value));
    }

    public enum DetectionTarget {
        NORMAL_LIVING,
        HOSTILE,
        CULTIVATOR,
        DROPPED_TREASURE,
        SPIRITUAL_TREASURE_BLOCK,
        SPIRIT_VEIN,
        FORMATION,
        FORMATION_NODE,
        SOUL
    }

    public enum HidingSource {
        INVISIBLE,
        SNEAKING,
        UNDER_COVER,
        IN_WATER,
        DISGUISE_MASK,
        MUFFLE_TALISMAN,
        ACTIVELY_USING_QI,
        EXPOSED_BY_ACTION
    }
}
