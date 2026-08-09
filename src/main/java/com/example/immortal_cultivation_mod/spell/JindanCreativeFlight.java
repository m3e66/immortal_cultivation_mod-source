package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.FlyingSwordEntity;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import com.example.immortal_cultivation_mod.particle.MethodQiParticleEffects;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

public final class JindanCreativeFlight {
    /** Default Golden Core flight travel speed in world blocks per tick. */
    public static final double BASE_TRAVEL_SPEED = 0.54D;
    private static final int QI_PER_SECOND = 100;
    private static final int MAX_SCROLL_STEPS_PER_PACKET = 8;
    private static final float DEFAULT_FLYING_SPEED = 0.05F;
    private static final float MIN_FLYING_SPEED = 0.025F;
    private static final float MAX_FLYING_SPEED = 1.00F;
    private static final float FLYING_SPEED_STEP = 0.025F;
    private static final float ELYTRA_POSE_SPEED = DEFAULT_FLYING_SPEED * 10.0F;
    private static final int FAST_FLIGHT_TRAIL_POINTS = 6;
    private static final double FAST_FLIGHT_MIN_MOVEMENT_SQR = 0.01D;
    private static final String ENABLED_TAG = "ImmortalCultivationJindanCreativeFlight";
    private static final String GRANTED_TAG = "ImmortalCultivationJindanCreativeFlightGranted";
    private static final String WAS_AIRBORNE_TAG = "ImmortalCultivationJindanCreativeFlightAirborne";
    private static final String SPEED_TAG = "ImmortalCultivationJindanCreativeFlightSpeed";
    private static final String ORIGINAL_SPEED_TAG = "ImmortalCultivationJindanCreativeFlightOriginalSpeed";
    private static final String EXTERNAL_SPEED_SUSPENDED_TAG =
            "ImmortalCultivationJindanCreativeFlightSpeedSuspended";
    private static final String CONTROL_SYNC_INITIALIZED_TAG =
            "ImmortalCultivationJindanFlightControlSyncInitialized";
    private static final String CONTROL_ACTIVE_TAG =
            "ImmortalCultivationJindanFlightControlActive";

    private JindanCreativeFlight() {
    }

    public static void setEnabled(ServerPlayer player, boolean enabled) {
        player.getPersistentData().putBoolean(ENABLED_TAG, enabled);
        syncControlActive(player, false);
        if (!enabled && !hasExternalFlight(player)) {
            revokeIfOwned(player);
        }
    }

    public static void tick(ServerPlayer player) {
        // Another flight system may have snapshotted Jindan's grant and speed. Leave that
        // ownership intact until it finishes so its eventual restore cannot leak mayfly.
        if (hasExternalFlight(player)) {
            suspendSelectedSpeedForExternalFlight(player);
            syncControlActive(player, false);
            return;
        }
        player.getPersistentData().remove(EXTERNAL_SPEED_SUSPENDED_TAG);

        boolean enabled = player.getPersistentData().getBoolean(ENABLED_TAG);
        boolean eligible = CultivationLevels.getRealmIndex(
                ModAttachments.getData(player).cultivationLevel()) >= 3;
        if (!enabled || !eligible) {
            revokeIfOwned(player);
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            revokeIfOwned(player);
            return;
        }

        var data = ModAttachments.getData(player);
        if (data.qi() < QI_PER_SECOND) {
            suspendForNoQi(player);
            return;
        }

        boolean ownsFlight = player.getPersistentData().getBoolean(GRANTED_TAG);
        if (!ownsFlight && player.getAbilities().mayfly) {
            syncControlActive(player, false);
            return;
        }

        boolean abilitiesChanged = false;
        if (!ownsFlight) {
            player.getPersistentData().putFloat(
                    ORIGINAL_SPEED_TAG, player.getAbilities().getFlyingSpeed());
            player.getAbilities().mayfly = true;
            player.getPersistentData().putBoolean(GRANTED_TAG, true);
            abilitiesChanged = true;
        } else if (!player.getPersistentData().contains(ORIGINAL_SPEED_TAG)) {
            player.getPersistentData().putFloat(
                    ORIGINAL_SPEED_TAG, player.getAbilities().getFlyingSpeed());
        }
        if (!player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            abilitiesChanged = true;
        }
        syncControlActive(player, true);

        float selectedSpeed = selectedSpeed(player);
        if (Math.abs(player.getAbilities().getFlyingSpeed() - selectedSpeed) > 0.0001F) {
            player.getAbilities().setFlyingSpeed(selectedSpeed);
            abilitiesChanged = true;
        }

        if (player.onGround()) {
            if (player.getPersistentData().getBoolean(WAS_AIRBORNE_TAG)
                    && player.getAbilities().flying) {
                player.getAbilities().flying = false;
                abilitiesChanged = true;
            }
            player.getPersistentData().remove(WAS_AIRBORNE_TAG);
        } else {
            player.getPersistentData().putBoolean(WAS_AIRBORNE_TAG, true);
        }

        if (player.getAbilities().flying && isFastFlightSpeed(selectedSpeed)) {
            emitFastFlightTrail(player);
        }

        if (player.getAbilities().flying && player.tickCount % 20 == 0) {
            ModAttachments.setData(player, data.withQi(data.qi() - QI_PER_SECOND));
            ServerEvents.syncPlayerData(player);
        }

        if (abilitiesChanged) {
            player.onUpdateAbilities();
        }
    }

    public static void copyPersistentData(Player original, Player copy) {
        if (original.getPersistentData().getBoolean(ENABLED_TAG)) {
            copy.getPersistentData().putBoolean(ENABLED_TAG, true);
        }
        if (original.getPersistentData().contains(SPEED_TAG)) {
            copy.getPersistentData().putFloat(
                    SPEED_TAG, clampSpeed(original.getPersistentData().getFloat(SPEED_TAG)));
        }
    }

    public static void adjustSpeed(ServerPlayer player, int requestedSteps) {
        int realmIndex = CultivationLevels.getRealmIndex(
                ModAttachments.getData(player).cultivationLevel());
        if (requestedSteps == 0
                || !player.getPersistentData().getBoolean(ENABLED_TAG)
                || !player.getPersistentData().getBoolean(GRANTED_TAG)
                || realmIndex < 3
                || !player.getAbilities().flying
                || player.isCreative()
                || player.isSpectator()
                || hasExternalFlight(player)) {
            return;
        }

        int steps = Mth.clamp(
                requestedSteps, -MAX_SCROLL_STEPS_PER_PACKET, MAX_SCROLL_STEPS_PER_PACKET);
        float currentSpeed = selectedSpeed(player);
        float nextSpeed = clampSpeed(currentSpeed + steps * FLYING_SPEED_STEP);
        if (Math.abs(nextSpeed - currentSpeed) > 0.0001F) {
            player.getPersistentData().putFloat(SPEED_TAG, nextSpeed);
            player.getAbilities().setFlyingSpeed(nextSpeed);
            player.onUpdateAbilities();
        }

        String multiplier = String.format(
                Locale.ROOT, "%.1f", nextSpeed / DEFAULT_FLYING_SPEED);
        player.displayClientMessage(Component.translatable(
                "message.immortal_cultivation_mod.jindan_flight_speed", multiplier), true);
    }

    public static void resetControlSync(ServerPlayer player) {
        player.getPersistentData().remove(CONTROL_SYNC_INITIALIZED_TAG);
    }

    public static boolean shouldUseElytraPose(Player player) {
        if (player == null
                || player.isFallFlying()
                || !player.getAbilities().flying
                || !isFastFlightSpeed(player.getAbilities().getFlyingSpeed())
                || player.hasEffect(ModEffects.YUFENG_JUE)
                || player.getVehicle() instanceof FlyingSwordEntity) {
            return false;
        }
        return true;
    }

    private static boolean isFastFlightSpeed(float speed) {
        return speed + 0.0001F >= ELYTRA_POSE_SPEED;
    }

    private static void emitFastFlightTrail(ServerPlayer player) {
        Vec3 movement = player.getDeltaMovement();
        if (movement.lengthSqr() < FAST_FLIGHT_MIN_MOVEMENT_SQR) {
            return;
        }

        List<SimpleParticleType> particles =
                MethodQiParticleEffects.particlesForPlayer(player);
        if (particles.isEmpty()) {
            return;
        }

        Vec3 direction = movement.normalize();
        Vec3 trailOrigin = player.position()
                .add(0.0D, player.getBbHeight() * 0.48D, 0.0D);
        double trailLength = Mth.clamp(movement.length() * 1.5D, 1.2D, 3.0D);
        for (int pointIndex = 0; pointIndex < FAST_FLIGHT_TRAIL_POINTS; pointIndex++) {
            double distanceBehind = 0.25D
                    + trailLength * pointIndex / (FAST_FLIGHT_TRAIL_POINTS - 1.0D);
            Vec3 point = trailOrigin.subtract(direction.scale(distanceBehind));
            SimpleParticleType particle = particles.get(
                    Math.floorMod(player.tickCount + pointIndex, particles.size()));
            player.serverLevel().sendParticles(
                    particle,
                    point.x,
                    point.y,
                    point.z,
                    1,
                    0.025D,
                    0.025D,
                    0.025D,
                    0.006D);
        }
    }

    private static void revokeIfOwned(ServerPlayer player) {
        syncControlActive(player, false);
        player.getPersistentData().remove(WAS_AIRBORNE_TAG);
        if (!player.getPersistentData().getBoolean(GRANTED_TAG)
                || hasExternalFlight(player)) {
            return;
        }
        player.getPersistentData().remove(GRANTED_TAG);
        player.getPersistentData().remove(EXTERNAL_SPEED_SUSPENDED_TAG);
        restoreOriginalSpeed(player);
        if (!player.isCreative() && !player.isSpectator() && !hasExternalFlight(player)) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
        }
        player.onUpdateAbilities();
    }

    private static void suspendForNoQi(ServerPlayer player) {
        revokeIfOwned(player);
    }

    private static float selectedSpeed(ServerPlayer player) {
        if (!player.getPersistentData().contains(SPEED_TAG)) {
            return DEFAULT_FLYING_SPEED;
        }
        return clampSpeed(player.getPersistentData().getFloat(SPEED_TAG));
    }

    private static float clampSpeed(float speed) {
        if (!Float.isFinite(speed)) {
            return DEFAULT_FLYING_SPEED;
        }
        return Mth.clamp(speed, MIN_FLYING_SPEED, MAX_FLYING_SPEED);
    }

    private static void restoreOriginalSpeed(ServerPlayer player) {
        float originalSpeed = originalSpeed(player);
        player.getPersistentData().remove(ORIGINAL_SPEED_TAG);
        player.getAbilities().setFlyingSpeed(
                Float.isFinite(originalSpeed) ? originalSpeed : DEFAULT_FLYING_SPEED);
    }

    private static void suspendSelectedSpeedForExternalFlight(ServerPlayer player) {
        if (!player.getPersistentData().getBoolean(GRANTED_TAG)
                || player.getVehicle() instanceof FlyingSwordEntity
                || !player.hasEffect(ModEffects.YUFENG_JUE)
                || player.getPersistentData().getBoolean(EXTERNAL_SPEED_SUSPENDED_TAG)) {
            return;
        }

        player.getPersistentData().putBoolean(EXTERNAL_SPEED_SUSPENDED_TAG, true);
        float originalSpeed = originalSpeed(player);
        if (Math.abs(player.getAbilities().getFlyingSpeed() - originalSpeed) > 0.0001F) {
            player.getAbilities().setFlyingSpeed(originalSpeed);
            player.onUpdateAbilities();
        }
    }

    private static float originalSpeed(ServerPlayer player) {
        float originalSpeed = player.getPersistentData().contains(ORIGINAL_SPEED_TAG)
                ? player.getPersistentData().getFloat(ORIGINAL_SPEED_TAG)
                : DEFAULT_FLYING_SPEED;
        return Float.isFinite(originalSpeed) ? originalSpeed : DEFAULT_FLYING_SPEED;
    }

    private static void syncControlActive(ServerPlayer player, boolean active) {
        boolean initialized = player.getPersistentData().getBoolean(CONTROL_SYNC_INITIALIZED_TAG);
        boolean previous = player.getPersistentData().getBoolean(CONTROL_ACTIVE_TAG);
        if (initialized && previous == active) {
            return;
        }
        player.getPersistentData().putBoolean(CONTROL_SYNC_INITIALIZED_TAG, true);
        player.getPersistentData().putBoolean(CONTROL_ACTIVE_TAG, active);
        ModPayloads.syncJindanFlightControlState(player, active);
    }

    private static boolean hasExternalFlight(ServerPlayer player) {
        return player.hasEffect(ModEffects.YUFENG_JUE)
                || player.getVehicle() instanceof FlyingSwordEntity;
    }
}
