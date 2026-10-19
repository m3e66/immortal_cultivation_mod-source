package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.spell.JindanCreativeFlight;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Reference-style camera warp for Golden Core high-speed flight and dives. */
public final class JindanFlightEffects {
    private static final String SPEED_EFFECT_NAME = "jindan_speed";

    private static final double FOV_PER_SPEED_LAYER = 0.20D;
    private static final double MAX_PERSISTENT_FOV_BOOST = 8.0D;
    private static final double MAX_FLIGHT_FOV = 115.0D;
    private static final int SPEED_BURST_DURATION_TICKS = 8;
    private static final int DIVE_START_DURATION_TICKS = 8;
    private static final int DIVE_IMPACT_DURATION_TICKS = 9;
    private static final float DIVE_MIN_PITCH = 65.0F;
    private static final double DIVE_MIN_DESCENT_SPEED = 0.02D;
    private static final double DIVE_VERTICAL_DOMINANCE = 1.35D;
    private static final float FAST_FLIGHT_POSE_ENTER_STEP = 1.0F / 8.0F;
    private static final float FAST_FLIGHT_POSE_EXIT_STEP = 1.0F / 6.0F;

    private static boolean registered;
    private static boolean controlActive;
    private static boolean fastFlightMode;
    private static double fastFlightDistance;
    private static float previousFastFlightPoseBlend;
    private static float fastFlightPoseBlend;
    private static boolean diving;
    private static float selectedSpeed = Float.NaN;
    private static double currentFovBoost;
    private static float currentWarpStrength;
    private static int speedBurstTicks;
    private static int speedBurstLayers;
    private static int diveBurstTicks;
    private static int diveBurstDuration;
    private static boolean diveImpact;
    private static float diveIntensity = 1.0F;

    private JindanFlightEffects() {
    }

    public static void register() {
        if (registered) return;
        registered = true;
        NeoForge.EVENT_BUS.addListener(JindanFlightEffects::onClientTick);
        NeoForge.EVENT_BUS.addListener(JindanFlightEffects::onComputeFov);
        NeoForge.EVENT_BUS.addListener(JindanFlightEffects::onCameraAngles);
    }

    public static void setControlActive(boolean active) {
        controlActive = active;
        if (!active) {
            releasePostEffect(Minecraft.getInstance());
            resetTransientState();
        }
    }

    public static void updateSpeed(float speed, int changedLayers) {
        if (Float.isFinite(speed)) {
            selectedSpeed = Mth.clamp(
                    speed,
                    JindanCreativeFlight.minFlyingSpeed(),
                    JindanCreativeFlight.maxFlyingSpeed()
                            * com.example.immortal_cultivation_mod.entity.FlyingSwordEntity.FLIGHT_SPEED_MULTIPLIER);
        }
        if (changedLayers > 0) {
            speedBurstLayers = Mth.clamp(changedLayers, 1, 8);
            speedBurstTicks = SPEED_BURST_DURATION_TICKS;
        }
    }

    public static void triggerDive(boolean impact, float intensity) {
        diveImpact = impact;
        diveIntensity = Mth.clamp(
                Float.isFinite(intensity) ? intensity : 1.0F,
                0.5F,
                2.0F);
        diveBurstDuration = impact
                ? DIVE_IMPACT_DURATION_TICKS
                : DIVE_START_DURATION_TICKS;
        diveBurstTicks = diveBurstDuration;
        if (impact) {
            diving = false;
        }
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            releasePostEffect(minecraft);
            resetAllState();
            return;
        }
        previousFastFlightPoseBlend = fastFlightPoseBlend;

        boolean goldenCoreFlight = controlActive
                && ClientData.jindanFlightControlActive()
                && !ClientData.livingCorpse()
                && minecraft.player.getAbilities().flying
                && !minecraft.player.isCreative()
                && !minecraft.player.isSpectator();

        float speed = Float.isFinite(selectedSpeed)
                ? selectedSpeed
                : minecraft.player.getAbilities().getFlyingSpeed();
        var movement = minecraft.player.getDeltaMovement();
        boolean qualifyingFastFlight = goldenCoreFlight
                && !minecraft.player.onGround()
                && JindanCreativeFlight.isFastFlightMovement(movement);
        if (qualifyingFastFlight) {
            fastFlightDistance += movement.length();
            fastFlightMode = fastFlightDistance + 1.0E-4D
                    >= JindanCreativeFlight.fastFlightEntryDistance();
        } else {
            fastFlightDistance = 0.0D;
            fastFlightMode = false;
        }
        updateFastFlightPoseBlend(minecraft.player);

        int raisedLayers = raisedLayers(speed);
        int availableLayers = Math.max(1, Math.round(
                (JindanCreativeFlight.maxFlyingSpeed()
                        - JindanCreativeFlight.defaultFlyingSpeed())
                        / JindanCreativeFlight.flyingSpeedStep()));
        float layerProgress = Mth.clamp(
                raisedLayers / (float) availableLayers,
                0.0F,
                1.0F);

        double horizontalSpeed = Math.sqrt(
                movement.x * movement.x + movement.z * movement.z);
        boolean divingNow = goldenCoreFlight
                && JindanCreativeFlight.isDownwardSmashSpeed(speed)
                && movement.y <= -DIVE_MIN_DESCENT_SPEED
                && (minecraft.player.isShiftKeyDown()
                || minecraft.player.getXRot() >= DIVE_MIN_PITCH
                || -movement.y >= horizontalSpeed * DIVE_VERTICAL_DOMINANCE);
        diving = divingNow;

        double targetFovBoost = fastFlightMode
                ? Math.min(MAX_PERSISTENT_FOV_BOOST,
                raisedLayers * FOV_PER_SPEED_LAYER)
                : 0.0D;
        if (divingNow) {
            targetFovBoost += Mth.clamp(
                    3.0D + (-movement.y - DIVE_MIN_DESCENT_SPEED) * 4.0D,
                    3.0D,
                    7.0D);
        }

        float targetWarpStrength = 0.0F;
        if (fastFlightMode) {
            float movementWarp = Mth.clamp(
                    ((float) movement.length() - 0.16F) / 1.25F,
                    0.0F,
                    1.0F);
            float motionGate = Mth.clamp(
                    ((float) movement.length() - 0.035F) / 0.18F,
                    0.0F,
                    1.0F);
            targetWarpStrength = Math.max(
                    layerProgress * 0.88F,
                    movementWarp * 0.72F) * motionGate;
        }
        if (divingNow) {
            targetWarpStrength = Math.max(targetWarpStrength,
                    Mth.clamp(0.76F + (float) (-movement.y) * 0.16F,
                            0.76F, 1.0F));
        }
        if (speedBurstTicks > 0) {
            targetWarpStrength = Math.max(targetWarpStrength,
                    Math.min(1.0F, 0.68F + speedBurstLayers * 0.04F));
        }
        if (diveBurstTicks > 0) {
            targetWarpStrength = Math.max(targetWarpStrength,
                    diveImpact ? 1.0F : 0.88F);
        }

        double fovResponse = targetFovBoost > currentFovBoost ? 0.28D : 0.16D;
        currentFovBoost += (targetFovBoost - currentFovBoost) * fovResponse;
        if (targetFovBoost == 0.0D && Math.abs(currentFovBoost) < 0.01D) {
            currentFovBoost = 0.0D;
        }

        float warpResponse = targetWarpStrength > currentWarpStrength ? 0.42F : 0.18F;
        currentWarpStrength += (targetWarpStrength - currentWarpStrength) * warpResponse;
        if (targetWarpStrength == 0.0F && currentWarpStrength < 0.005F) {
            currentWarpStrength = 0.0F;
        }

        updatePostEffect(minecraft);

        if (speedBurstTicks > 0) {
            speedBurstTicks--;
        }
        if (diveBurstTicks > 0) {
            diveBurstTicks--;
        }
    }

    private static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (!event.usedConfiguredFov() || !isLocalPlayerCamera(event)) return;

        double speedBurst = speedBurstEnvelope(event.getPartialTick())
                * (2.5D + speedBurstLayers * 0.45D);
        double diveBurst = 0.0D;
        if (diveBurstTicks > 0) {
            float envelope = diveBurstEnvelope(event.getPartialTick());
            if (diveImpact) {
                float progress = diveBurstProgress(event.getPartialTick());
                float rebound = smoothStep(Mth.clamp(
                        (progress - 0.18F) / 0.32F,
                        0.0F,
                        1.0F));
                diveBurst = envelope
                        * Mth.lerp(rebound, -4.0D, 2.0D)
                        * diveIntensity;
            } else {
                diveBurst = envelope * 5.0D * diveIntensity;
            }
        }

        double boost = Mth.clamp(
                currentFovBoost + speedBurst + diveBurst,
                -4.0D,
                12.0D);
        double maximumFov = Math.max(event.getFOV(), MAX_FLIGHT_FOV);
        event.setFOV(Mth.clamp(
                event.getFOV() + boost,
                30.0D,
                maximumFov));
    }

    private static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if ((speedBurstTicks <= 0 && diveBurstTicks <= 0 && !diving)
                || !isLocalPlayerCamera(event)) {
            return;
        }

        float pitch = event.getPitch();
        float roll = event.getRoll();
        if (speedBurstTicks > 0) {
            double elapsed = speedBurstElapsed(event.getPartialTick());
            float envelope = speedBurstEnvelope(event.getPartialTick());
            float strength = envelope * (0.55F + speedBurstLayers * 0.11F);
            pitch += (float) Math.sin(elapsed * 2.65D) * strength;
            roll += (float) Math.cos(elapsed * 2.15D) * strength * 0.70F;
        }
        if (diveBurstTicks > 0) {
            double elapsed = diveBurstElapsed(event.getPartialTick());
            float envelope = diveBurstEnvelope(event.getPartialTick());
            float strength = envelope * diveIntensity * (diveImpact ? 3.35F : 1.45F);
            pitch += (float) Math.sin(elapsed * (diveImpact ? 3.05D : 1.90D))
                    * strength;
            roll += (float) Math.cos(elapsed * (diveImpact ? 2.55D : 1.55D))
                    * strength * 0.60F;
        }

        float tilt = Minecraft.getInstance().options.damageTiltStrength().get().floatValue();
        event.setPitch(event.getPitch() + Mth.clamp(pitch - event.getPitch(), -1.25F, 1.25F) * tilt);
        event.setRoll(event.getRoll() + Mth.clamp(roll - event.getRoll(), -1.25F, 1.25F) * tilt);
    }

    private static void updatePostEffect(Minecraft minecraft) {
        // The old full-screen radial-warp post chain produced large black
        // polygons on some sky/framebuffer combinations. Flight keeps its
        // particles, camera impulse, and restrained FOV response without it.
        releasePostEffect(minecraft);
    }

    private static void releasePostEffect(Minecraft minecraft) {
        if (minecraft != null && isOurPostEffect(minecraft.gameRenderer.currentEffect())) {
            minecraft.gameRenderer.shutdownEffect();
        }
    }

    private static boolean isOurPostEffect(PostChain effect) {
        return effect != null && effect.getName().contains(SPEED_EFFECT_NAME);
    }

    private static int raisedLayers(float speed) {
        return Math.max(0, Math.round(
                (speed - JindanCreativeFlight.defaultFlyingSpeed())
                        / JindanCreativeFlight.flyingSpeedStep()));
    }

    /** Eased local pose weight for real Golden Core fast flight, not vanilla Elytra. */
    public static float fastFlightPoseProgress(Player player, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (player == null || player != minecraft.player || player.isFallFlying()
                || com.example.immortal_cultivation_mod.entity.FlyingSwordEntity.flightSword(player) != null) {
            return 0.0F;
        }
        float interpolated = Mth.lerp(
                Mth.clamp(partialTick, 0.0F, 1.0F),
                previousFastFlightPoseBlend,
                fastFlightPoseBlend);
        return smoothStep(interpolated);
    }

    /** Compatibility gate for callers that only need to know whether any blend remains. */
    public static boolean shouldUseFastFlightPose(Player player) {
        return fastFlightPoseProgress(player, 1.0F) > 0.001F;
    }

    private static void updateFastFlightPoseBlend(Player player) {
        boolean entering = fastFlightMode && !player.isFallFlying()
                && com.example.immortal_cultivation_mod.entity.FlyingSwordEntity.flightSword(player) == null;
        if (entering) {
            fastFlightPoseBlend = Math.min(
                    1.0F,
                    fastFlightPoseBlend + FAST_FLIGHT_POSE_ENTER_STEP);
        } else {
            fastFlightPoseBlend = Math.max(
                    0.0F,
                    fastFlightPoseBlend - FAST_FLIGHT_POSE_EXIT_STEP);
        }
    }

    private static boolean isLocalPlayerCamera(ViewportEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null
                && event.getCamera().getEntity() == minecraft.player;
    }

    private static float speedBurstEnvelope(double partialTick) {
        if (speedBurstTicks <= 0) return 0.0F;
        double elapsed = speedBurstElapsed(partialTick);
        float attack = smoothStep(Mth.clamp((float) (elapsed / 2.0D), 0.0F, 1.0F));
        float decay = smoothStep(Mth.clamp(
                (float) ((elapsed - 2.0D) / (SPEED_BURST_DURATION_TICKS - 2.0D)),
                0.0F,
                1.0F));
        return attack * (1.0F - decay);
    }

    private static double speedBurstElapsed(double partialTick) {
        return SPEED_BURST_DURATION_TICKS
                - Math.max(0.0D, speedBurstTicks - partialTick);
    }

    private static float diveBurstEnvelope(double partialTick) {
        if (diveBurstTicks <= 0 || diveBurstDuration <= 0) return 0.0F;
        double elapsed = diveBurstElapsed(partialTick);
        double attackTicks = diveImpact ? 1.25D : 2.0D;
        float attack = smoothStep(Mth.clamp(
                (float) (elapsed / attackTicks),
                0.0F,
                1.0F));
        float decay = smoothStep(Mth.clamp(
                (float) ((elapsed - attackTicks)
                        / Math.max(1.0D, diveBurstDuration - attackTicks)),
                0.0F,
                1.0F));
        return attack * (1.0F - decay);
    }

    private static float diveBurstProgress(double partialTick) {
        return Mth.clamp(
                (float) (diveBurstElapsed(partialTick)
                        / Math.max(1, diveBurstDuration)),
                0.0F,
                1.0F);
    }

    private static double diveBurstElapsed(double partialTick) {
        return diveBurstDuration - Math.max(0.0D, diveBurstTicks - partialTick);
    }

    private static float smoothStep(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private static void resetTransientState() {
        selectedSpeed = Float.NaN;
        fastFlightMode = false;
        fastFlightDistance = 0.0D;
        currentFovBoost = 0.0D;
        currentWarpStrength = 0.0F;
        speedBurstTicks = 0;
        speedBurstLayers = 0;
        diveBurstTicks = 0;
        diveBurstDuration = 0;
        diving = false;
        diveImpact = false;
        diveIntensity = 1.0F;
    }

    private static void resetAllState() {
        controlActive = false;
        resetTransientState();
        previousFastFlightPoseBlend = 0.0F;
        fastFlightPoseBlend = 0.0F;
    }
}
