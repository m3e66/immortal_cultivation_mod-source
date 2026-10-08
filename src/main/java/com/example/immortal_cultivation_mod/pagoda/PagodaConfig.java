package com.example.immortal_cultivation_mod.pagoda;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class PagodaConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue DURATION, COOLDOWN, MAX_PRISONERS, QI_COST, TARGET_COST, UPKEEP;
    public static final ModConfigSpec.DoubleValue RADIUS, BOSS_MULTIPLIER, PRESSURE_DAMAGE, BREAKOUT_SPEED, EXTERNAL_SCALE;
    public static final ModConfigSpec.BooleanValue CAPTURE_PLAYERS, CAPTURE_BOSSES;
    static {
        var b = new ModConfigSpec.Builder(); b.push("pagoda");
        RADIUS=b.defineInRange("captureRadius",10.,1.,32.);
        DURATION=b.defineInRange("durationSeconds",60,5,600);
        COOLDOWN=b.defineInRange("cooldownSeconds",90,1,3600);
        MAX_PRISONERS=b.defineInRange("maximumPrisoners",16,1,64);
        QI_COST=b.defineInRange("baseQiCost",300,0,1000000);
        UPKEEP=b.comment("Qi per second while a pagoda is deployed; insufficient Qi recalls it safely.").defineInRange("pagodaQiPerSecond",5,0,1000000);
        TARGET_COST=b.defineInRange("qiCostPerTarget",25,0,1000000);
        BOSS_MULTIPLIER=b.defineInRange("bossBreakPowerMultiplier",2.,1.,100.);
        PRESSURE_DAMAGE=b.defineInRange("pressureDamagePerSecond",1.,0.,1000.);
        BREAKOUT_SPEED=b.defineInRange("breakoutSpeed",1.,.01,100.);
        EXTERNAL_SCALE=b.comment("Additional multiplier for external entities; existing external-power scaling also applies.")
                .defineInRange("externalBreakPowerScale",1.,.01,100.);
        CAPTURE_PLAYERS=b.define("allowPlayerCapture",false);
        CAPTURE_BOSSES=b.define("allowBossCapture",true);
        b.pop(); SPEC=b.build();
    }
    private PagodaConfig() {}
}
