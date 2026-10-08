package com.example.immortal_cultivation_mod.entity;

/** Shared, server-authoritative formulas for asteroid size, movement, and damage. */
public final class AsteroidMechanics {
    public static final float MIN_SIZE = 0.1F;
    public static final float MAX_SIZE = 5.0F;
    public static final float MIN_ACTIVE_SIZE = 0.001F;
    public static final float BLOCKS_PER_SIZE = 10.0F;

    public static final double TICKS_PER_SECOND = 20.0D;
    public static final double DRIFT_SPEED_BLOCKS_PER_SECOND = 5.0D;
    public static final double DRIFT_SPEED_BLOCKS_PER_TICK =
            DRIFT_SPEED_BLOCKS_PER_SECOND / TICKS_PER_SECOND;
    public static final double IMPACT_THRESHOLD_BLOCKS_PER_SECOND = 6.0D;
    public static final double IMPACT_THRESHOLD_BLOCKS_PER_TICK =
            IMPACT_THRESHOLD_BLOCKS_PER_SECOND / TICKS_PER_SECOND;
    public static final double STRONG_HIT_SPEED_BLOCKS_PER_SECOND = 10.0D;
    public static final double STRONG_HIT_SPEED_BLOCKS_PER_TICK =
            STRONG_HIT_SPEED_BLOCKS_PER_SECOND / TICKS_PER_SECOND;

    public static final float COLLISION_DAMAGE_PER_BLOCK_PER_SECOND = 1_000.0F;
    public static final float STRONG_HIT_DAMAGE_PER_SIZE = 1_000.0F;
    public static final float DAMAGE_PER_SIZE_LOST = 10_000.0F;

    private AsteroidMechanics() {
    }

    public static float clampSize(float size) {
        if (!Float.isFinite(size)) {
            return MIN_SIZE;
        }
        return Math.max(MIN_SIZE, Math.min(MAX_SIZE, size));
    }

    /** Damaged asteroids may become smaller than the 0.1x natural-spawn floor. */
    public static float clampActiveSize(float size) {
        if (!Float.isFinite(size)) {
            return MIN_SIZE;
        }
        return Math.max(MIN_ACTIVE_SIZE, Math.min(MAX_SIZE, size));
    }

    public static float diameter(float size) {
        return clampActiveSize(size) * BLOCKS_PER_SIZE;
    }

    public static double blocksPerTick(double blocksPerSecond) {
        return blocksPerSecond / TICKS_PER_SECOND;
    }

    public static double blocksPerSecond(double blocksPerTick) {
        return blocksPerTick * TICKS_PER_SECOND;
    }

    public static boolean isDamagingImpact(double speedBlocksPerSecond) {
        return Double.isFinite(speedBlocksPerSecond)
                && speedBlocksPerSecond >= IMPACT_THRESHOLD_BLOCKS_PER_SECOND;
    }

    public static float collisionDamage(double speedBlocksPerSecond) {
        if (!Double.isFinite(speedBlocksPerSecond) || speedBlocksPerSecond <= 0.0D) {
            return 0.0F;
        }
        double damage = speedBlocksPerSecond * COLLISION_DAMAGE_PER_BLOCK_PER_SECOND;
        return (float) Math.min(Float.MAX_VALUE, damage);
    }

    public static float strongHitThreshold(float preHitSize) {
        return clampActiveSize(preHitSize) * STRONG_HIT_DAMAGE_PER_SIZE;
    }

    public static boolean isStrongHit(float damage, float preHitSize) {
        return Float.isFinite(damage)
                && damage >= strongHitThreshold(preHitSize);
    }

    public static float sizeLost(float damage) {
        if (!Float.isFinite(damage) || damage <= 0.0F) {
            return 0.0F;
        }
        return damage / DAMAGE_PER_SIZE_LOST;
    }

    public static float sizeAfterDamage(float preHitSize, float damage) {
        float remaining = clampActiveSize(preHitSize) - sizeLost(damage);
        return remaining < MIN_ACTIVE_SIZE ? 0.0F : remaining;
    }
}
