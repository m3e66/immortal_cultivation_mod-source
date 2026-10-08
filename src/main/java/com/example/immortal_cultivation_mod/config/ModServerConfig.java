package com.example.immortal_cultivation_mod.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ModServerConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ZHENPAN_QI_FIRST;
    public static final ModConfigSpec.BooleanValue SHOW_QI_USE_PARTICLES;
    public static final ModConfigSpec.BooleanValue ENABLE_DROUGHT_ZOMBIE_DOMAIN;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("magicRitual");
        ZHENPAN_QI_FIRST = builder
                .comment("When true, temporary Formation Plate centers spend player qi before inventory spirit stones. The default spends spirit stones first.")
                .define("formationPlateQiFirst", false);
        builder.pop();
        builder.push("visuals");
        SHOW_QI_USE_PARTICLES = builder
                .comment("Show the automatic cultivation-method particles around a player whenever that player spends qi. Spell-specific VFX are unaffected.")
                .define("showQiUseParticles", true);
        builder.pop();
        builder.push("undead");
        ENABLE_DROUGHT_ZOMBIE_DOMAIN = builder
                .comment("Allow Hanba drought terrain drying and the Scorched Earth domain. Players can additionally disable their own drought domain in the in-game settings screen.")
                .define("enableDroughtZombieDomain", true);
        builder.pop();
        SPEC = builder.build();
    }

    private ModServerConfig() {
    }

    public static boolean zhenpanQiFirst() {
        return ZHENPAN_QI_FIRST.get();
    }

    public static boolean showQiUseParticles() {
        return SHOW_QI_USE_PARTICLES.get();
    }

    public static boolean droughtZombieDomainEnabled() {
        return ENABLE_DROUGHT_ZOMBIE_DOMAIN.get();
    }
}
