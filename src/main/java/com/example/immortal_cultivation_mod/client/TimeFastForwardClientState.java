package com.example.immortal_cultivation_mod.client;

import net.minecraft.client.Minecraft;

/** Client render state fed by the server's accelerated day-time packets. */
public final class TimeFastForwardClientState {
    private static final double DAY_TICKS = 24_000.0D;
    private static final double NANOS_PER_GAME_TICK = 50_000_000.0D;
    private static final double MAX_FRAME_TICKS = 2.0D;
    private static final double POSITION_RESPONSE = 7.0D;
    private static final double VELOCITY_RESPONSE = 18.0D;
    private static final double HARD_RESET_DISTANCE = DAY_TICKS * 2.0D;
    private static final int BASE_STEP_TICKS = 10;
    private static final int MAX_SPEED_MULTIPLIER = 5;
    private static final long PACKET_TIMEOUT_NANOS = 1_000_000_000L;
    private static final long TRAIL_FADE_NANOS = 1_800_000_000L;
    private static final Object VISUAL_CLOCK_LOCK = new Object();
    private static volatile boolean active;
    private static volatile int stepTicks;
    private static volatile long lastPacketNanos;
    private static volatile float fadeStartMultiplier;
    private static volatile long fadeStartNanos;
    private static long packetDayTime;
    private static long packetAnchorNanos;
    private static boolean hasPacketAnchor;
    private static double visualDayTime = Double.NaN;
    private static double visualVelocity = 1.0D;
    private static long lastVisualNanos;

    private TimeFastForwardClientState() {
    }

    public static void accept(long dayTime, int step, boolean isActive) {
        long now = System.nanoTime();
        float currentTrail = trailLengthMultiplier(now);
        if (isActive) {
            stepTicks = Math.max(1, step);
            lastPacketNanos = now;
            fadeStartMultiplier = 0.0F;
            fadeStartNanos = 0L;
            active = true;
        } else {
            active = false;
            stepTicks = 0;
            lastPacketNanos = 0L;
            fadeStartMultiplier = currentTrail;
            fadeStartNanos = now;
        }
        synchronized (VISUAL_CLOCK_LOCK) {
            packetDayTime = dayTime;
            packetAnchorNanos = now;
            hasPacketAnchor = true;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            minecraft.level.setDayTime(dayTime);
        }
    }

    public static boolean isActive() {
        return active
                && System.nanoTime() - lastPacketNanos < PACKET_TIMEOUT_NANOS;
    }

    public static float trailLengthMultiplier() {
        return trailLengthMultiplier(System.nanoTime());
    }

    public static float interpolatedTimeOfDay(long dayTime, float partialTick) {
        long now = System.nanoTime();
        boolean activeNow = isActiveAt(now);
        double rawTarget = dayTime + partialTick;

        synchronized (VISUAL_CLOCK_LOCK) {
            double targetVelocity = activeNow
                    ? Math.max(1, stepTicks)
                    : 1.0D;
            double targetTime = rawTarget;
            if (activeNow && hasPacketAnchor) {
                double packetAgeTicks = Math.max(0.0D,
                        (now - packetAnchorNanos) / NANOS_PER_GAME_TICK);
                targetTime = packetDayTime
                        + packetAgeTicks * targetVelocity;
            }

            if (Double.isNaN(visualDayTime)
                    || Math.abs(targetTime - visualDayTime)
                    > HARD_RESET_DISTANCE) {
                visualDayTime = targetTime;
                visualVelocity = targetVelocity;
                lastVisualNanos = now;
                return wrapDayTime(visualDayTime);
            }

            long elapsedNanos = Math.max(0L, now - lastVisualNanos);
            double elapsedTicks = Math.min(MAX_FRAME_TICKS,
                    elapsedNanos / NANOS_PER_GAME_TICK);
            double elapsedSeconds = elapsedTicks / 20.0D;
            lastVisualNanos = now;

            if (elapsedTicks > 0.0D) {
                double velocityBlend = 1.0D - Math.exp(
                        -VELOCITY_RESPONSE * elapsedSeconds);
                visualVelocity += (targetVelocity - visualVelocity)
                        * velocityBlend;
                visualDayTime += visualVelocity * elapsedTicks;

                double positionBlend = 1.0D - Math.exp(
                        -POSITION_RESPONSE * elapsedSeconds);
                visualDayTime += (targetTime - visualDayTime)
                        * positionBlend;
            }
            return wrapDayTime(visualDayTime);
        }
    }

    private static boolean isActiveAt(long now) {
        return active && now - lastPacketNanos < PACKET_TIMEOUT_NANOS;
    }

    private static float wrapDayTime(double dayTime) {
        double wrapped = dayTime % DAY_TICKS;
        if (wrapped < 0.0D) {
            wrapped += DAY_TICKS;
        }
        return (float) wrapped;
    }

    private static float trailLengthMultiplier(long now) {
        if (active) {
            float multiplier = speedMultiplier(stepTicks);
            long packetAge = now - lastPacketNanos;
            if (packetAge < PACKET_TIMEOUT_NANOS) {
                return multiplier;
            }
            return fade(multiplier, packetAge - PACKET_TIMEOUT_NANOS);
        }
        if (fadeStartNanos == 0L || fadeStartMultiplier <= 0.0F) {
            return 0.0F;
        }
        return fade(fadeStartMultiplier, now - fadeStartNanos);
    }

    private static float speedMultiplier(int step) {
        if (step <= 1) {
            return 0.0F;
        }
        return Math.min(MAX_SPEED_MULTIPLIER,
                step / (float) BASE_STEP_TICKS);
    }

    private static float fade(float startMultiplier, long elapsedNanos) {
        float progress = Math.max(0.0F, Math.min(1.0F,
                elapsedNanos / (float) TRAIL_FADE_NANOS));
        float smoothProgress = progress * progress * (3.0F - 2.0F * progress);
        return startMultiplier * (1.0F - smoothProgress);
    }
}
