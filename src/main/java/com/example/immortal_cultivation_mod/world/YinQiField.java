package com.example.immortal_cultivation_mod.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public final class YinQiField {
    public static final int MIN_VALUE = -15;
    public static final int MAX_VALUE = 15;
    private static final double ZERO_THRESHOLD = 0.80D;
    private static final double GATE_SCALE = 192.0D;
    private static final double VALUE_SCALE = 256.0D;
    private static final Map<ResourceKey<Level>, Map<UUID, AuraSource>> AURAS = new HashMap<>();

    private YinQiField() {
    }

    public static int sample(ServerLevel level, BlockPos pos) {
        return Math.max(sample(level.getSeed(), pos.getX(), pos.getZ()), auraValue(level, pos));
    }

    public static void updateAura(ServerLevel level, UUID sourceId, BlockPos center, int radius, int centerValue, int durationTicks) {
        AURAS.computeIfAbsent(level.dimension(), ignored -> new HashMap<>())
                .put(sourceId, new AuraSource(center.immutable(), Math.max(1, radius),
                        Mth.clamp(centerValue, 0, MAX_VALUE), level.getGameTime() + Math.max(1, durationTicks)));
    }

    public static List<Integer> sampleGrid(ServerLevel level, BlockPos center, int size, int step) {
        int safeSize = Mth.clamp(size, 3, 65);
        if (safeSize % 2 == 0) {
            safeSize++;
        }
        int safeStep = Mth.clamp(step, 4, 128);
        int half = safeSize / 2;
        List<Integer> values = new ArrayList<>(safeSize * safeSize);
        long seed = level.getSeed();
        for (int z = -half; z <= half; z++) {
            for (int x = -half; x <= half; x++) {
                BlockPos pos = new BlockPos(center.getX() + x * safeStep, center.getY(), center.getZ() + z * safeStep);
                values.add(Math.max(sample(seed, pos.getX(), pos.getZ()), auraValue(level, pos)));
            }
        }
        return values;
    }

    private static int auraValue(ServerLevel level, BlockPos pos) {
        Map<UUID, AuraSource> sources = AURAS.get(level.dimension());
        if (sources == null || sources.isEmpty()) {
            return 0;
        }
        long now = level.getGameTime();
        sources.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
        int value = 0;
        for (AuraSource source : sources.values()) {
            int distance = Math.max(
                    Math.abs(pos.getX() - source.center().getX()),
                    Math.abs(pos.getZ() - source.center().getZ())
            );
            if (distance > source.radius()) {
                continue;
            }
            int aura = source.centerValue() - (int) Math.floor(distance * (source.centerValue() / (double) source.radius()));
            value = Math.max(value, Mth.clamp(aura, 1, source.centerValue()));
        }
        return value;
    }

    public static List<Integer> sampleQiGrid(ServerLevel level, BlockPos center, int size, int step) {
        int safeSize = Mth.clamp(size, 3, 65);
        if (safeSize % 2 == 0) {
            safeSize++;
        }
        int safeStep = Mth.clamp(step, 4, 128);
        int half = safeSize / 2;
        List<Integer> values = new ArrayList<>(safeSize * safeSize);
        long seed = level.getSeed();
        for (int z = -half; z <= half; z++) {
            for (int x = -half; x <= half; x++) {
                values.add(sampleQi(seed, center.getX() + x * safeStep, center.getZ() + z * safeStep));
            }
        }
        return values;
    }

    private static int sample(long seed, int blockX, int blockZ) {
        double gate = valueNoise01(seed, blockX / GATE_SCALE, blockZ / GATE_SCALE, 0x26B357);
        if (gate < ZERO_THRESHOLD) {
            return 0;
        }
        double signed = fbmSigned(seed, blockX / VALUE_SCALE, blockZ / VALUE_SCALE);
        int value = Mth.clamp((int) Math.round(signed * MAX_VALUE), MIN_VALUE, MAX_VALUE);
        if (value == 0) {
            value = signed >= 0.0D ? 1 : -1;
        }
        return value;
    }

    private static int sampleQi(long seed, int blockX, int blockZ) {
        double gate = valueNoise01(seed, blockX / GATE_SCALE, blockZ / GATE_SCALE, 0x71A9FF);
        if (gate < ZERO_THRESHOLD) {
            return 0;
        }
        double value = valueNoise01(seed, blockX / VALUE_SCALE, blockZ / VALUE_SCALE, 0x51F1A1);
        return Mth.clamp((int) Math.round((0.20D + value * 0.80D) * MAX_VALUE), 1, MAX_VALUE);
    }

    private static double fbmSigned(long seed, double x, double z) {
        double total = 0.0D;
        double amplitude = 1.0D;
        double amplitudeSum = 0.0D;
        double frequency = 1.0D;
        for (int octave = 0; octave < 4; octave++) {
            total += (valueNoise01(seed, x * frequency, z * frequency, 0x5EED + octave * 0x9E37) * 2.0D - 1.0D) * amplitude;
            amplitudeSum += amplitude;
            amplitude *= 0.5D;
            frequency *= 2.0D;
        }
        return Mth.clamp(total / amplitudeSum, -1.0D, 1.0D);
    }

    private static double valueNoise01(long seed, double x, double z, int salt) {
        int x0 = Mth.floor(x);
        int z0 = Mth.floor(z);
        int x1 = x0 + 1;
        int z1 = z0 + 1;
        double tx = smooth(x - x0);
        double tz = smooth(z - z0);
        double a = hash01(seed, x0, z0, salt);
        double b = hash01(seed, x1, z0, salt);
        double c = hash01(seed, x0, z1, salt);
        double d = hash01(seed, x1, z1, salt);
        return Mth.lerp(tz, Mth.lerp(tx, a, b), Mth.lerp(tx, c, d));
    }

    private static double smooth(double value) {
        return value * value * (3.0D - 2.0D * value);
    }

    private static double hash01(long seed, int x, int z, int salt) {
        long value = seed;
        value ^= (long) x * 0x9E3779B97F4A7C15L;
        value ^= (long) z * 0xC2B2AE3D27D4EB4FL;
        value ^= (long) salt * 0x165667B19E3779F9L;
        value = splitMix64(value);
        return (value >>> 11) * 0x1.0p-53;
    }

    private static long splitMix64(long value) {
        value += 0x9E3779B97F4A7C15L;
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private record AuraSource(BlockPos center, int radius, int centerValue, long expiresAt) {
    }
}
