package com.example.immortal_cultivation_mod.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ModClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLE_JINDAN_FLIGHT_MOUSE_WHEEL;
    public static final ModConfigSpec.BooleanValue ENABLE_EXTERNAL_QI_SIZE_MOUSE_WHEEL;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("controls");
        ENABLE_JINDAN_FLIGHT_MOUSE_WHEEL = builder
                .comment("Allow the mouse wheel to change Golden Core flight speed while actively flying.")
                .define("enableGoldenCoreFlightMouseWheel", true);
        ENABLE_EXTERNAL_QI_SIZE_MOUSE_WHEEL = builder
                .comment("Allow the mouse wheel to change external Qi size while holding a weapon.")
                .define("enableExternalQiSizeMouseWheel", true);
        builder.pop();
        SPEC = builder.build();
    }

    private ModClientConfig() {
    }

    public static boolean enableJindanFlightMouseWheel() {
        return ENABLE_JINDAN_FLIGHT_MOUSE_WHEEL.get();
    }

    public static boolean enableExternalQiSizeMouseWheel() {
        return ENABLE_EXTERNAL_QI_SIZE_MOUSE_WHEEL.get();
    }

    public static void setExternalQiSizeMouseWheel(boolean enabled) {
        ENABLE_EXTERNAL_QI_SIZE_MOUSE_WHEEL.set(enabled);
        SPEC.save();
    }
}
