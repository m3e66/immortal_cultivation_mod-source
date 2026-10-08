package com.example.immortal_cultivation_mod.externalpower;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ExternalPowerConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, VANILLA, DEBUG, DEBUG_ALIAS;
    public static final ModConfigSpec.IntValue MIN_TIER, MAX_TIER, BURST_WINDOW;
    public static final ModConfigSpec.DoubleValue ARMOR, EFFECTS, INVULNERABILITY;
    public static final ModConfigSpec.ConfigValue<List<? extends Double>> MULTIPLIERS, THRESHOLDS;
    public static final List<Double> DEFAULT_MULTIPLIERS = List.of(.001, .01, .05, .25, 1., 1.25, 1.5);
    public static final List<Double> DEFAULT_THRESHOLDS = List.of(10.,30.,100.,300.,1000.,3000.,10000.,100000.,1000000.,100000000.);
    static {
        var b = new ModConfigSpec.Builder();
        ENABLED = b.define("enableExternalPowerScaling", true);
        VANILLA = b.comment("Opt in to vanilla damage; identifiable cultivation attacks always remain excluded.").define("scaleVanillaDamage", false);
        MIN_TIER = b.defineInRange("minimumExternalTier", 0, 0, 64);
        MAX_TIER = b.defineInRange("maximumAutomaticTier", 12, 0, 64);
        MULTIPLIERS = b.comment("Seven multipliers in order: tier difference <= -4, -3, -2, -1, 0, +1, >= +2.")
                .defineList("tierDifferenceMultipliers", DEFAULT_MULTIPLIERS, ExternalPowerConfig::nonnegative);
        THRESHOLDS = b.comment("Ten strictly ascending positive raw-damage thresholds. Scoring interpolates in logarithmic space; raw damage alone cannot exceed automatic tier 2.")
                .defineList("rawDamageThresholds", DEFAULT_THRESHOLDS, v -> nonnegative(v) && ((Number)v).doubleValue() > 0);
        ARMOR = b.defineInRange("bypassArmorScore", 1., 0., 32.);
        EFFECTS = b.comment("Additional penetration for bypassing effects or enchantments; not added twice when both tags exist.").defineInRange("bypassEffectsScore", .5, 0., 32.);
        INVULNERABILITY = b.defineInRange("bypassInvulnerabilityScore", 2., 0., 32.);
        BURST_WINDOW = b.comment("Maximum gap in ticks between related hits. Bursts also expire after four windows, at most 200 ticks. Does not suppress any hit.")
                .defineInRange("burstDetectionWindow", 4, 1, 50);
        DEBUG = b.define("debugExternalPower", false);
        DEBUG_ALIAS = b.comment("Alias for debugExternalPower; either flag enables analysis logging.").define("externalPowerDebug", false);
        SPEC = b.build();
    }
    private ExternalPowerConfig() {}
    private static boolean nonnegative(Object v) { return v instanceof Number n && Double.isFinite(n.doubleValue()) && n.doubleValue() >= 0; }
    public record Settings(boolean enabled, boolean vanilla, int minimum, int maximum, List<Double> multipliers,
                           List<Double> thresholds, double armor, double effects, double invulnerability, int burstWindow, boolean debug) {
        public double multiplier(int difference) { return multipliers.get(Math.clamp(difference, -4, 2) + 4); }
    }
    public static Settings defaults() { return new Settings(true,false,0,12,DEFAULT_MULTIPLIERS,DEFAULT_THRESHOLDS,1,.5,2,4,false); }
    public static Settings read() {
        List<Double> multipliers = MULTIPLIERS.get().stream().map(Number::doubleValue).toList();
        List<Double> thresholds = THRESHOLDS.get().stream().map(Number::doubleValue).toList();
        if (multipliers.size() != 7) multipliers = DEFAULT_MULTIPLIERS;
        boolean ordered = thresholds.size() == 10;
        for (int i=1;i<thresholds.size();i++) ordered &= thresholds.get(i)>thresholds.get(i-1);
        if (!ordered) thresholds = DEFAULT_THRESHOLDS;
        return new Settings(ENABLED.get(), VANILLA.get(), MIN_TIER.get(), Math.max(MIN_TIER.get(),MAX_TIER.get()),
                multipliers, thresholds, ARMOR.get(), EFFECTS.get(), INVULNERABILITY.get(), BURST_WINDOW.get(), DEBUG.get() || DEBUG_ALIAS.get());
    }
}
