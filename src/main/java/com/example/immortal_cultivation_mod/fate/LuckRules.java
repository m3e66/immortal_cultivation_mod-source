package com.example.immortal_cultivation_mod.fate;

public final class LuckRules {
    private LuckRules() {}
    public static int clamp(int luck) { return Math.max(0, Math.min(100, luck)); }
    public static String state(int luck) {
        int n = clamp(luck);
        return n <= 10 ? "forsaken" : n <= 25 ? "cursed" : n <= 40 ? "unlucky"
                : n <= 59 ? "ordinary" : n <= 74 ? "fortunate" : n <= 89 ? "great"
                : n <= 99 ? "favored" : "destined";
    }
    public static double rareMultiplier(int luck) {
        int n = clamp(luck);
        return n <= 75 ? 0.5 + n * 0.01 : 1.25 + (n - 75) * 0.014;
    }
    public static double rareChance(double base, int luck) {
        if (base <= 0 || base >= 1) return Math.max(0, Math.min(1, base));
        return Math.min(0.95, base * rareMultiplier(luck));
    }
    public static double opportunityChance(int luck) {
        return rareChance(0.12, luck);
    }
    public static int theftAmount(int caster, int target, boolean boss) {
        if (target <= 26 || caster >= 95) return 0;
        int amount = Math.max(2, Math.min(12, (int)Math.floor((target - 25) * 0.20)));
        if (boss) amount /= 2;
        return Math.max(0, Math.min(amount, Math.min(95 - caster, target - 25)));
    }
    public static int theftKarma(int amount, int targetBefore) {
        return amount <= 0 ? 0 : 15 + amount * 3
                + (targetBefore >= 100 ? 40 : targetBefore >= 90 ? 20 : targetBefore >= 75 ? 10 : 0);
    }
    public static double misfortuneChance(int luck) {
        int n = clamp(luck);
        if (n <= 5) return 0.80;
        if (n <= 15) return 0.80 - (n - 5) * 0.030;
        if (n <= 30) return 0.50 - (n - 15) * (0.26 / 15);
        if (n <= 50) return 0.24 - (n - 30) * 0.007;
        return Math.max(0.02, 0.10 - (n - 50) * 0.0016);
    }
    public static double avoidanceChance(double base, int severity) {
        if (base <= 0) return 0;
        return Math.min(base, Math.max(0.01, base * (severity == 2 ? 0.80 : severity == 1 ? 0.65 : 0.50)));
    }
    public static double misfortuneWardChance(double base) {
        if (base <= 0) return 0;
        return Math.min(1.0, base * 0.75);
    }
    public static boolean rescueEligible(int victim, int attacker) {
        return clamp(victim) > clamp(attacker);
    }
}
