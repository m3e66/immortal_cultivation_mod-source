package com.example.immortal_cultivation_mod.flame;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** Shared, deterministic crater silhouette. Its irregular edge always stays inside the realm radius. */
public final class FlameCraterShape {
    public static long seed(UUID identity) {
        return identity.getMostSignificantBits() ^ Long.rotateLeft(identity.getLeastSignificantBits(), 17);
    }

    /** Vertical half-height for a block column, or NaN outside the blast's uneven footprint. */
    public static double reach(Vec3 center, BlockPos column, double radius, long seed) {
        double x = column.getX() + .5 - center.x, z = column.getZ() + .5 - center.z;
        double phase = (mix(seed) >>> 11) * (Math.PI * 2 / 0x1.0p53);
        double angle = Math.atan2(z, x);
        double grain = noise(x / Math.max(1, radius * .14), z / Math.max(1, radius * .14), seed);
        double edge = Math.clamp(.87 + .075 * Math.sin(angle * 3 + phase)
                + .055 * Math.sin(angle * 5 - phase * 1.7) + .035 * grain, .7, 1);
        double remaining = radius * radius - (x * x + z * z) / (edge * edge);
        return remaining < 0 ? Double.NaN : Math.sqrt(remaining);
    }

    public static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }

    private static double noise(double x, double z, long seed) {
        int ix = (int)Math.floor(x), iz = (int)Math.floor(z);
        double fx = x - ix, fz = z - iz;
        fx = fx * fx * (3 - 2 * fx); fz = fz * fz * (3 - 2 * fz);
        double a = grain(ix, iz, seed), b = grain(ix + 1, iz, seed);
        double c = grain(ix, iz + 1, seed), d = grain(ix + 1, iz + 1, seed);
        return (a + (b - a) * fx) * (1 - fz) + (c + (d - c) * fx) * fz;
    }

    private static double grain(int x, int z, long seed) {
        return (mix(seed ^ x * 73428767L ^ z * 912931L) >>> 11) * (2 / 0x1.0p53) - 1;
    }

    private FlameCraterShape() {}
}
