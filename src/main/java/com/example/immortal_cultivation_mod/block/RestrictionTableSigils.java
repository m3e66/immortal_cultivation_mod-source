package com.example.immortal_cultivation_mod.block;

import java.util.List;
import net.minecraft.world.phys.Vec3;

/** Stable per-table artwork selection and inward-facing, smoothly orbiting sigil poses. */
public final class RestrictionTableSigils {
    public static final List<String> ARTWORK = List.of("armor_breaking", "qi_storage", "sharpness",
            "spirit_devouring", "spirit_gathering", "sturdy", "swift_flight", "protection",
            "fire_protection", "blast_protection", "projectile_protection", "feather_falling");
    public static final int COUNT = 5;
    public static final float HALF_SIZE = .19F;
    public static final double RADIUS = .84D;

    public record Pose(Vec3 center, Vec3 tangent, Vec3 up, Vec3 inwardNormal, int textureIndex) {}

    public static Pose sample(long seed, int slot, double ticks) {
        if (slot < 0 || slot >= COUNT) throw new IllegalArgumentException("Invalid sigil slot");
        double phase = (mix(seed) >>> 11) * (Math.PI * 2 / 0x1.0p53);
        double angle = phase + slot * Math.PI * 2 / COUNT + ticks * Math.PI * 2 / 400;
        double cos = Math.cos(angle), sin = Math.sin(angle);
        double y = 1.03D + .05D * Math.sin(ticks * Math.PI * 2 / 200 + slot * Math.PI * 2 / COUNT);
        int texture = (int)Math.floorMod(mix(seed + slot * 0x9E3779B97F4A7C15L), ARTWORK.size());
        Vec3 center = new Vec3(.5D + cos * RADIUS, y, .5D + sin * RADIUS);
        Vec3 tangent = new Vec3(-sin, 0, cos);
        Vec3 normal = new Vec3(.5D, .86D, .5D).subtract(center).normalize();
        return new Pose(center, tangent, normal.cross(tangent).normalize(), normal, texture);
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private RestrictionTableSigils() {}
}
