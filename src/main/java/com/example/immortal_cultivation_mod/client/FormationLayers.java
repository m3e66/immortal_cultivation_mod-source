package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.block.RestrictionTableSigils;
import com.example.immortal_cultivation_mod.magicritual.block.custom.CircleType;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Shared, cached artwork and independent rotation for world and GUI formations. */
public final class FormationLayers {
    public static final int COUNT = 3;
    private static final double[] DEGREES_PER_TICK = {0.45, -0.60, 1.05};
    private static final List<ResourceLocation> BASIC = textures("particle/magic_center_glyph");
    private static final Map<String, List<ResourceLocation>> CENTERS = centers();
    private static final Map<String, List<ResourceLocation>> RESTRICTIONS = restrictions();
    private static final List<ResourceLocation> ATTACK = textures("particle/magic_circle_glyph");
    private static final List<ResourceLocation> DEFENSE = textures("particle/magic_circle_defense_glyph");
    private static final List<ResourceLocation> REGENERATION = textures("particle/magic_circle_regeneration_glyph");

    private FormationLayers() {}

    public static List<ResourceLocation> center(ResourceLocation type) {
        return type != null && "immortal_cultivation_mod".equals(type.getNamespace())
                ? CENTERS.getOrDefault(type.getPath(), BASIC) : BASIC;
    }

    public static List<ResourceLocation> restriction(String type) {
        return RESTRICTIONS.get(type);
    }

    public static List<ResourceLocation> circle(CircleType type) {
        return switch (type) {
            case DEFENSE -> DEFENSE;
            case REGENERATION -> REGENERATION;
            default -> ATTACK;
        };
    }

    public static float angle(double ticks, int layer) {
        // Reduce each angle before converting to float, avoiding jitter in old worlds.
        return (float)((ticks * DEGREES_PER_TICK[layer]) % 360.0);
    }

    private static List<ResourceLocation> textures(String stem) {
        return java.util.stream.IntStream.rangeClosed(1, COUNT)
                .mapToObj(layer -> ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod",
                        "textures/" + stem + "_" + layer + ".png")).toList();
    }

    private static Map<String, List<ResourceLocation>> centers() {
        Map<String, List<ResourceLocation>> result = new HashMap<>();
        for (String type : List.of("fog", "inverted", "forcefield", "scorching", "sanctuary", "frost",
                "decay", "thunder", "detection", "reforge_blade", "teleport")) {
            result.put(type, textures("particle/magic_center_" + type));
        }
        return Map.copyOf(result);
    }

    private static Map<String, List<ResourceLocation>> restrictions() {
        Map<String, List<ResourceLocation>> result = new HashMap<>();
        for (String type : RestrictionTableSigils.ARTWORK) {
            result.put(type, textures("gui/restriction/" + type));
        }
        return Map.copyOf(result);
    }
}
