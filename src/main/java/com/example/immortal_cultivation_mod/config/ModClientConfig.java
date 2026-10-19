package com.example.immortal_cultivation_mod.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ModClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLE_JINDAN_FLIGHT_MOUSE_WHEEL;
    public static final ModConfigSpec.BooleanValue ENABLE_EXTERNAL_QI_SIZE_MOUSE_WHEEL;
    public static final ModConfigSpec.BooleanValue ENABLE_MOD_SKY;
    public static final ModConfigSpec.BooleanValue PILL_TOXICITY_OVERLAY;
    public static final ModConfigSpec.DoubleValue PILL_TOXICITY_OPACITY;
    public static final ModConfigSpec.IntValue LIFE_CARD_ANIMATION;
    public static final ModConfigSpec.IntValue BLAST_QI_QUALITY, BLAST_QI_PARTICLE_BUDGET;
    public static final ModConfigSpec.DoubleValue BLAST_QI_OPACITY;
    public record HudCoordinates(ModConfigSpec.DoubleValue x, ModConfigSpec.DoubleValue y) {}
    public static final java.util.Map<HudElement, HudCoordinates> HUD_POSITIONS =
            new java.util.EnumMap<>(HudElement.class);

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("sky");
        ENABLE_MOD_SKY=builder.comment("Show the mod's Overworld night sky and custom Sun/Moon. Applies immediately; shader packs remain enabled.")
                .translation("screen.immortal_cultivation_mod.settings.mod_sky").define("enableModSky",true);
        builder.pop();
        builder.push("blastQiVisuals");
        BLAST_QI_QUALITY=builder.comment("Qi visual quality: 0 low, 1 balanced, 2 high. Combat is unchanged.")
                .translation("config.immortal_cultivation_mod.qi_quality").defineInRange("quality",2,0,2);
        BLAST_QI_PARTICLE_BUDGET=builder.comment("Maximum additional Qi particles per client tick, shared by all actors.")
                .translation("config.immortal_cultivation_mod.qi_particles").defineInRange("particleBudget",48,0,96);
        BLAST_QI_OPACITY=builder.comment("Opacity of Qi forms. Interiors stay translucent.")
                .translation("config.immortal_cultivation_mod.qi_opacity").defineInRange("opacity",.9,0.,1.);
        builder.pop();
        builder.push("controls");
        ENABLE_JINDAN_FLIGHT_MOUSE_WHEEL = builder
                .comment("Allow the mouse wheel to change Golden Core flight speed while actively flying.")
                .define("enableGoldenCoreFlightMouseWheel", true);
        ENABLE_EXTERNAL_QI_SIZE_MOUSE_WHEEL = builder
                .comment("Allow Shift + mouse wheel to change external Qi size while holding a weapon. Disabled by default.")
                .define("enableExternalQiSizeMouseWheel", false);
        builder.pop();
        builder.push("pillToxicity");
        PILL_TOXICITY_OVERLAY=builder.comment("Disable only the screen-edge warning; server mechanics remain active.")
                .translation("toxicity.config.enabled").define("screenEffects",true);
        PILL_TOXICITY_OPACITY=builder.comment("Opacity of all toxicity visuals, including warnings; 0 to 1, default 0.6.")
                .translation("toxicity.config.opacity").defineInRange("opacity",.6,0.,1.);
        builder.pop();
        builder.push("lifeCardBattle");
        LIFE_CARD_ANIMATION=builder.comment("Card dice animations: 0 normal, 1 fast, 2 minimal.").defineInRange("animationSpeed",0,0,2);
        builder.pop();
        builder.push("hudLayout");
        for (HudElement element : HudElement.values()) {
            builder.push(element.id());
            var x = builder.comment("Horizontal position: -1 uses the default; 0 left, 1 right.")
                    .defineInRange("x", -1.0, -1.0, 1.0);
            var y = builder.comment("Vertical position: -1 uses the default; 0 top, 1 bottom.")
                    .defineInRange("y", -1.0, -1.0, 1.0);
            HUD_POSITIONS.put(element, new HudCoordinates(x, y));
            builder.pop();
        }
        builder.pop();
        SPEC = builder.build();
    }

    private ModClientConfig() {
    }

    public static boolean enableJindanFlightMouseWheel() {
        return ENABLE_JINDAN_FLIGHT_MOUSE_WHEEL.get();
    }

    public static boolean enableModSky() {
        return ENABLE_MOD_SKY.get();
    }

    public static void setModSky(boolean enabled) {
        ENABLE_MOD_SKY.set(enabled);
        SPEC.save();
    }

    public static boolean enableExternalQiSizeMouseWheel() {
        return ENABLE_EXTERNAL_QI_SIZE_MOUSE_WHEEL.get();
    }

    public static void setExternalQiSizeMouseWheel(boolean enabled) {
        ENABLE_EXTERNAL_QI_SIZE_MOUSE_WHEEL.set(enabled);
        SPEC.save();
    }

    public static void setPillToxicityOverlay(boolean enabled) {
        PILL_TOXICITY_OVERLAY.set(enabled);
        SPEC.save();
    }

    public static void setPillToxicityOpacity(double opacity) {
        PILL_TOXICITY_OPACITY.set(Math.max(0, Math.min(1, opacity)));
        SPEC.save();
    }
}
