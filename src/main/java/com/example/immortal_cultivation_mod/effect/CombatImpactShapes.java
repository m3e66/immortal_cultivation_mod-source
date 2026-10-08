package com.example.immortal_cultivation_mod.effect;

import net.minecraft.world.phys.Vec3;

/** Stable world-space geometry and timing shared by combat feedback and its regression tests. */
public final class CombatImpactShapes {
    public enum Stage { FLASH, SHOCKWAVE, CRACK, DEBRIS, DUST, FALLING_ENERGY }
    public record Basis(Vec3 forward, Vec3 right, Vec3 up) {}

    private CombatImpactShapes() {}

    public static boolean finite(Vec3 vector) {
        return vector != null && Double.isFinite(vector.x) && Double.isFinite(vector.y)
                && Double.isFinite(vector.z);
    }

    public static Basis basis(Vec3 direction) {
        Vec3 forward = finite(direction) && direction.lengthSqr() > 1e-8
                ? direction.normalize() : new Vec3(0, 0, 1);
        Vec3 reference = Math.abs(forward.y) > .95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = reference.cross(forward).normalize();
        return new Basis(forward, right, forward.cross(right).normalize());
    }

    public static double delay(Stage stage, float intensity) {
        if (intensity < .8F) return stage == Stage.FALLING_ENERGY ? 6 : 0;
        return switch (stage) {
            case FLASH -> 0;
            case SHOCKWAVE -> 1; // One Minecraft tick is 0.05 seconds.
            case CRACK -> 2;
            case DEBRIS -> 3;
            case DUST -> 4;
            case FALLING_ENERGY -> 6;
        };
    }

    public static double elapsed(Stage stage, float intensity, double age) {
        return age - delay(stage, intensity);
    }

    public static Vec3 ringPoint(Vec3 center, Basis basis, double radius, double angle) {
        return center.add(basis.right.scale(Math.cos(angle) * radius))
                .add(basis.up.scale(Math.sin(angle) * radius));
    }

    /** Attack energy carries through the target; it never turns toward the viewing camera. */
    public static Vec3 behind(Vec3 contact, Basis basis, double targetWidth, double distance) {
        return contact.add(basis.forward.scale(Math.max(.25, targetWidth) + Math.max(0, distance)));
    }

    /** Deterministic variation: no per-frame random changes or moving impact origin. */
    public static double variation(int seed, int index) {
        int hash = seed ^ (index * 0x9e3779b9);
        hash ^= hash >>> 16;
        hash *= 0x7feb352d;
        hash ^= hash >>> 15;
        hash *= 0x846ca68b;
        hash ^= hash >>> 16;
        return (hash & 0x7fffffff) / (double) Integer.MAX_VALUE;
    }
}
