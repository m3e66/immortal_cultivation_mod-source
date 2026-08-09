package com.example.immortal_cultivation_mod.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ModServerConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ZHENPAN_QI_FIRST;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("magicRitual");
        ZHENPAN_QI_FIRST = builder
                .comment("When true, temporary Formation Plate centers spend player qi before inventory spirit stones. The default spends spirit stones first.")
                .define("formationPlateQiFirst", false);
        builder.pop();
        SPEC = builder.build();
    }

    private ModServerConfig() {
    }

    public static boolean zhenpanQiFirst() {
        return ZHENPAN_QI_FIRST.get();
    }
}
