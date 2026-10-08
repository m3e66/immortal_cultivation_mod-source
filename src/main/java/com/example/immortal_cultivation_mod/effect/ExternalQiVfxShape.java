package com.example.immortal_cultivation_mod.effect;

import net.minecraft.world.phys.Vec3;

/** World-space blade geometry using the projectile's collision axes. */
public final class ExternalQiVfxShape {
    public static final double MAX_RADIUS = 32.0D;
    public static final BladeMask ALL_SECTIONS = new BladeMask(-1L, -1L);

    /** Two packed words keep up to 128 independent pieces cheap to synchronize. */
    public record BladeMask(long low, long high) {
        public boolean active(int section) {
            return section < 64 ? (low & (1L << section)) != 0 : (high & (1L << (section - 64))) != 0;
        }
        public BladeMask without(int section) {
            return section < 64 ? new BladeMask(low & ~(1L << section), high)
                    : new BladeMask(low, high & ~(1L << (section - 64)));
        }
        public BladeMask with(int section) {
            return section < 64 ? new BladeMask(low | (1L << section), high)
                    : new BladeMask(low, high | (1L << (section - 64)));
        }
        public BladeMask and(BladeMask other) {
            return new BladeMask(low & other.low, high & other.high);
        }
        public boolean empty() { return low == 0 && high == 0; }
    }
    private ExternalQiVfxShape() {}

    /** One invisible simulation particle per visible blade strip, batched in one projectile. */
    public static int sectionCount(double radius) {
        return 36;
    }

    public static double particleStart(double radius, int particle) {
        return -1 + 2.0 * particle / sectionCount(radius);
    }

    public static double particleEnd(double radius, int particle) {
        return particleStart(radius, particle + 1);
    }

    public static int section(double radius, double t) {
        int count = sectionCount(radius);
        return Math.clamp((int) Math.floor((t + 1) * count * .5), 0, count - 1);
    }

    public static BladeMask allSections(double radius) {
        int count = sectionCount(radius);
        return new BladeMask(count >= 64 ? -1L : (1L << count) - 1,
                count == 128 ? -1L : count > 64 ? (1L << (count - 64)) - 1 : 0);
    }

    public static boolean sectionActive(BladeMask mask, double radius, double t) {
        return mask.active(section(radius, t));
    }

    /** Thick blade body, shared by rendering, collision, and terrain sampling. */
    public static double halfThickness(double radius) {
        return (.8D + Math.clamp(radius, 0, MAX_RADIUS) * .10D) * Math.min(1, Math.max(0, radius));
    }

    public static double tipExtension(double radius) {
        return halfThickness(radius) * 2;
    }

    public static double bladeReach(double radius) {
        return radius + tipExtension(radius);
    }

    /** Shared pointed silhouette: thick middle, then long triangular ends. */
    public static double bladeTaper(double radius, double t) {
        double reach = bladeReach(radius);
        double distance = Math.abs(t) * reach;
        double shoulder = radius * .6;
        if (distance <= shoulder) {
            double body = distance / Math.max(.001, radius);
            return Math.max(0, 1 - body * body);
        }
        return .64 * Math.clamp((reach - distance) / (reach - shoulder), 0, 1);
    }

    public static Vec3 right(Vec3 forward) {
        Vec3 right = new Vec3(0, 1, 0).cross(forward);
        return right.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : right.normalize();
    }

    public static Vec3 axis(Vec3 forward, int stage) {
        Vec3 right = right(forward), up = forward.cross(right).normalize();
        return switch (Math.floorMod(stage, 3)) {
            case 0 -> right.add(up).normalize();
            case 1 -> right.subtract(up).normalize();
            default -> right;
        };
    }

    public static Vec3 crescent(Vec3 center, Vec3 forward, Vec3 axis, double radius, double t) {
        t = Math.clamp(t, -1, 1);
        // The sharpened center follows the real projectile; tips curve backwards within its swept volume.
        return center.add(axis.scale(t * bladeReach(radius))).subtract(forward.scale(Math.min(.9, radius * .22) * t * t));
    }

    public static int color(String particle) {
        return switch (particle) {
            case "method_qi_metal" -> 0xf4d48a;
            case "method_qi_wood" -> 0x83dc9a;
            case "method_qi_water" -> 0x66afe8;
            case "method_qi_fire" -> 0xff9655;
            case "method_qi_earth" -> 0xd7ac78;
            case "method_qi_thunder" -> 0xb5a2fa;
            case "method_qi_ice" -> 0xadeaf6;
            case "method_qi_wind" -> 0x8fe0c7;
            case "method_qi_light" -> 0xffe9af;
            case "method_qi_dark" -> 0x9a84c9;
            case "method_qi_blood" -> 0xe45b70;
            default -> 0x94d9e9;
        };
    }
}
