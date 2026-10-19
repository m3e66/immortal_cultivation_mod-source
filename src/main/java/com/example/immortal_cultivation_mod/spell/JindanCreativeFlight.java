package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.FlyingSwordEntity;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Locale;

public final class JindanCreativeFlight {
    /** Default Golden Core flight travel speed in world blocks per tick. */
    public static final double BASE_TRAVEL_SPEED = 0.54D;
    private static final int QI_PER_SECOND = 100;
    private static final double QI_COST_PER_RAISED_LAYER = 0.10D;
    private static final int MAX_SCROLL_STEPS_PER_PACKET = 8;
    private static final float VANILLA_FLYING_SPEED = 0.05F;
    private static final float DEFAULT_FLYING_SPEED = VANILLA_FLYING_SPEED * 2.0F;
    private static final float MIN_FLYING_SPEED = 0.025F;
    private static final float MAX_FLYING_SPEED = 1.00F;
    private static final float FLYING_SPEED_STEP = 0.025F;
    private static final float FLIGHT_MODE_SPEED = VANILLA_FLYING_SPEED * 10.0F;
    private static final float DOWNWARD_SMASH_SPEED = VANILLA_FLYING_SPEED * 15.0F;
    private static final double FAST_FLIGHT_MIN_BLOCKS_PER_SECOND = 5.0D;
    private static final double FAST_FLIGHT_MIN_MOVEMENT =
            FAST_FLIGHT_MIN_BLOCKS_PER_SECOND / 20.0D;
    private static final double FAST_FLIGHT_MIN_MOVEMENT_SQR =
            FAST_FLIGHT_MIN_MOVEMENT * FAST_FLIGHT_MIN_MOVEMENT;
    private static final double FAST_FLIGHT_MAX_ASCENT_SPEED = 0.04D;
    private static final double FAST_FLIGHT_ENTRY_DISTANCE = 10.0D;
    private static final int PERIODIC_FLIGHT_BOOM_INTERVAL_TICKS = 3 * 20;
    private static final float DIVE_MIN_PITCH = 65.0F;
    private static final double DIVE_MIN_DESCENT_SPEED = 0.12D;
    private static final double DIVE_SHIFT_MIN_DESCENT_SPEED = 0.02D;
    private static final double DIVE_PULL_OUT_SPEED = -0.04D;
    private static final double DIVE_VERTICAL_DOMINANCE = 1.35D;
    private static final int DIVE_REENTRY_COOLDOWN_TICKS = 18;
    private static final int DIVE_LANDING_GRACE_TICKS = 10;
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
    private static final String FAST_FLIGHT_DISTANCE_TAG =
            "ImmortalCultivationJindanFastFlightDistance";
    private static final String FAST_FLIGHT_ACTIVE_TAG =
            "ImmortalCultivationJindanFastFlightActive";
    private static final String PERIODIC_FLIGHT_BOOM_TICKS_TAG =
            "ImmortalCultivationJindanMaxSpeedBoomTicks";
    private static final String DIVE_ACTIVE_TAG =
            "ImmortalCultivationJindanFlightDiveActive";
    private static final String DIVE_PEAK_SPEED_TAG =
            "ImmortalCultivationJindanFlightDivePeakSpeed";
    private static final String DIVE_COOLDOWN_TAG =
            "ImmortalCultivationJindanFlightDiveCooldown";
    private static final String DIVE_AGE_TAG =
            "ImmortalCultivationJindanFlightDiveAge";
    private static final String DIVE_LANDING_GRACE_TAG =
            "ImmortalCultivationJindanFlightDiveLandingGrace";
    private static final DustParticleOptions FLIGHT_WHITE =
            new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 1.75F);
    private JindanCreativeFlight() {
    }

    public static float defaultFlyingSpeed() {
        return DEFAULT_FLYING_SPEED;
    }

    public static float minFlyingSpeed() {
        return MIN_FLYING_SPEED;
    }

    public static float maxFlyingSpeed() {
        return MAX_FLYING_SPEED;
    }

    /** Realm caps apply to the selected base speed; sword flight multiplies afterward. */
    public static float maxFlyingSpeedForRealm(int realm) {
        return VANILLA_FLYING_SPEED * (realm >= 5 ? 20.0F : realm == 4 ? 10.0F : 5.0F);
    }

    public static boolean canUseSwordFlight(ServerPlayer player) {
        return player.isAlive() && !player.isCreative() && !player.isSpectator()
                && !player.isPassenger() && !hasExternalFlight(player)
                && player.getPersistentData().getBoolean(ENABLED_TAG)
                && player.getPersistentData().getBoolean(GRANTED_TAG)
                && CultivationLevels.getRealmIndex(ModAttachments.getData(player).cultivationLevel()) >= 3;
    }

    public static float effectiveFlyingSpeed(ServerPlayer player) {
        float base = selectedSpeed(player);
        return base * (FlyingSwordEntity.flightSword(player) != null
                ? FlyingSwordEntity.FLIGHT_SPEED_MULTIPLIER : 1.0F);
    }

    public static float flyingSpeedStep() {
        return FLYING_SPEED_STEP;
    }

    public static void setEnabled(ServerPlayer player, boolean enabled) {
        player.getPersistentData().putBoolean(ENABLED_TAG, enabled);
        syncControlActive(player, false);
        if (!enabled) FlyingSwordEntity.stopSwordFlight(player);
        if (!enabled && !hasExternalFlight(player)) {
            revokeIfOwned(player);
        }
    }


    public static void tick(ServerPlayer player) {
        update(player, true);
    }

    /** Recompute a toggled blade bonus without advancing effects or charging a second upkeep tick. */
    public static void refreshFlightAbilities(ServerPlayer player) {
        update(player, false);
    }

    private static void update(ServerPlayer player, boolean advanceFlight) {
        // Another flight system may have snapshotted Jindan's grant and speed. Leave that
        // ownership intact until it finishes so its eventual restore cannot leak mayfly.
        if (hasExternalFlight(player)) {
            resetDiveState(player);
            resetFastFlightState(player);
            resetPeriodicFlightBoom(player);
            suspendSelectedSpeedForExternalFlight(player);
            syncControlActive(player, false);
            return;
        }
        player.getPersistentData().remove(EXTERNAL_SPEED_SUSPENDED_TAG);

        if (isUndeadFlightAvailable(player)) {
            resetDiveState(player);
            resetFastFlightState(player);
            resetPeriodicFlightBoom(player);
            boolean abilitiesChanged = false;
            syncControlActive(player, true);
            float selectedSpeed = effectiveFlyingSpeed(player);
            if (Math.abs(player.getAbilities().getFlyingSpeed() - selectedSpeed) > 0.0001F) {
                player.getAbilities().setFlyingSpeed(selectedSpeed);
                abilitiesChanged = true;
            }
            if (abilitiesChanged) {
                player.onUpdateAbilities();
            }
            return;
        }

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
        float selectedSpeed = effectiveFlyingSpeed(player);
        int layeredQiCost = qiCostPerSecond(selectedSpeed);
        int flightCost = com.example.immortal_cultivation_mod.entity.TalismanWardEntity
                .adjustFlightQiCost(player, layeredQiCost);
        int estimatedCost = SpiritSealSystem.adjustSustainQiCost(
                player, flightCost);
        if (!ServerEvents.canAffordQiOrBlood(data, estimatedCost)) {
            suspendForNoQi(player);
            return;
        }

        boolean ownsFlight = player.getPersistentData().getBoolean(GRANTED_TAG);
        if (!ownsFlight && player.getAbilities().mayfly) {
            resetDiveState(player);
            resetFastFlightState(player);
            resetPeriodicFlightBoom(player);
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

        if (Math.abs(player.getAbilities().getFlyingSpeed() - selectedSpeed) > 0.0001F) {
            player.getAbilities().setFlyingSpeed(selectedSpeed);
            ModPayloads.syncJindanFlightSpeed(player, selectedSpeed, 0);
            abilitiesChanged = true;
        }

        if (advanceFlight) {
            boolean fastFlightMode = tickFastFlightMode(player, selectedSpeed);
            tickPeriodicFlightBoom(player, selectedSpeed, fastFlightMode);
            tickDive(player, selectedSpeed,
                    player.getAbilities().flying
                            && isDownwardSmashSpeed(selectedSpeed));
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

        // The continuous wake is synchronized by CultivationFlightVisuals and rendered as client ribbons.

        if (advanceFlight && player.getAbilities().flying && player.tickCount % 20 == 0) {
            if (!ServerEvents.spendSustainedQiOrBlood(
                    player, data, flightCost)) {
                suspendForNoQi(player);
                return;
            }
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
        boolean undeadFlight = isUndeadFlightAvailable(player);
        boolean jindanFlight = player.getPersistentData().getBoolean(ENABLED_TAG)
                && player.getPersistentData().getBoolean(GRANTED_TAG)
                && realmIndex >= 3;
        if (requestedSteps == 0
                || (!jindanFlight && !undeadFlight)
                || !player.getAbilities().flying
                || player.isCreative()
                || player.isSpectator()
                || hasExternalFlight(player)) {
            return;
        }

        int steps = Mth.clamp(
                requestedSteps, -MAX_SCROLL_STEPS_PER_PACKET, MAX_SCROLL_STEPS_PER_PACKET);
        float currentSpeed = selectedSpeed(player);
        float nextSpeed = clampSelectedSpeed(player, currentSpeed + steps * FLYING_SPEED_STEP);
        int changedLayers = 0;
        if (Math.abs(nextSpeed - currentSpeed) > 0.0001F) {
            changedLayers = Math.round((nextSpeed - currentSpeed) / FLYING_SPEED_STEP);
            player.getPersistentData().putFloat(SPEED_TAG, nextSpeed);
            player.getAbilities().setFlyingSpeed(effectiveFlyingSpeed(player));
            player.onUpdateAbilities();

            if (jindanFlight && !undeadFlight) {
                ModPayloads.syncJindanFlightSpeed(player, effectiveFlyingSpeed(player), changedLayers);
                if (changedLayers > 0) {
                    emitSpeedLayerBurst(player, changedLayers, effectiveFlyingSpeed(player));
                }
            }
        }

        String multiplier = String.format(
                Locale.ROOT, "%.1f", effectiveFlyingSpeed(player) / VANILLA_FLYING_SPEED);
        player.displayClientMessage(Component.translatable(
                "message.immortal_cultivation_mod.jindan_flight_speed",
                multiplier,
                qiCostPerSecond(effectiveFlyingSpeed(player))), true);
    }

    private static int qiCostPerSecond(float selectedSpeed) {
        int raisedLayers = Math.max(0, Math.round(
                (selectedSpeed - DEFAULT_FLYING_SPEED) / FLYING_SPEED_STEP));
        return Mth.ceil(QI_PER_SECOND
                * (1.0D + raisedLayers * QI_COST_PER_RAISED_LAYER));
    }

    private static boolean tickFastFlightMode(ServerPlayer player, float selectedSpeed) {
        var persistentData = player.getPersistentData();
        boolean wasActive = persistentData.getBoolean(FAST_FLIGHT_ACTIVE_TAG);
        Vec3 movement = player.getDeltaMovement();
        boolean qualifying = player.getAbilities().flying
                && !player.onGround()
                && isFastFlightMovement(movement);
        if (!qualifying) {
            resetFastFlightState(player);
            return false;
        }

        double travelled = persistentData.getDouble(FAST_FLIGHT_DISTANCE_TAG)
                + movement.length();
        persistentData.putDouble(FAST_FLIGHT_DISTANCE_TAG, travelled);
        if (travelled + 1.0E-4D < FAST_FLIGHT_ENTRY_DISTANCE) {
            persistentData.remove(FAST_FLIGHT_ACTIVE_TAG);
            return false;
        }

        persistentData.putBoolean(FAST_FLIGHT_ACTIVE_TAG, true);
        if (!wasActive) {
            // A qualifying x15 straight-down entry has its own stronger smash boom below.
            if (!isDownwardSmashSpeed(selectedSpeed)
                    || !isStraightDownDive(player, movement)) {
                emitSpeedLayerBurst(player, 1, selectedSpeed);
            }
        }
        return true;
    }

    /** Active x15+ fast flight breaks the sound barrier behind the player every three seconds. */
    private static void tickPeriodicFlightBoom(
            ServerPlayer player, float selectedSpeed, boolean fastFlightMode) {
        var persistentData = player.getPersistentData();
        if (!fastFlightMode || !isDownwardSmashSpeed(selectedSpeed)) {
            resetPeriodicFlightBoom(player);
            return;
        }
        int boomTicks = persistentData.getInt(PERIODIC_FLIGHT_BOOM_TICKS_TAG) + 1;
        if (boomTicks >= PERIODIC_FLIGHT_BOOM_INTERVAL_TICKS) {
            emitPeriodicFlightBoom(player, selectedSpeed);
            boomTicks = 0;
        }
        persistentData.putInt(PERIODIC_FLIGHT_BOOM_TICKS_TAG, boomTicks);
    }

    private static void tickDive(
            ServerPlayer player, float selectedSpeed, boolean downwardSmashEnabled) {
        var persistentData = player.getPersistentData();
        int cooldown = persistentData.getInt(DIVE_COOLDOWN_TAG);
        if (cooldown > 0) {
            persistentData.putInt(DIVE_COOLDOWN_TAG, cooldown - 1);
        } else {
            persistentData.remove(DIVE_COOLDOWN_TAG);
        }

        boolean active = persistentData.getBoolean(DIVE_ACTIVE_TAG);
        Vec3 movement = player.getDeltaMovement();
        // Arm from the actual descend input instead of relying on the last vertical
        // velocity sample. Minecraft can zero that sample on the collision tick.
        boolean shiftSmash = downwardSmashEnabled
                && player.isShiftKeyDown()
                && !player.onGround();
        if (shiftSmash) {
            persistentData.putInt(DIVE_LANDING_GRACE_TAG, DIVE_LANDING_GRACE_TICKS);
        } else {
            int landingGrace = persistentData.getInt(DIVE_LANDING_GRACE_TAG);
            if (landingGrace > 1) {
                persistentData.putInt(DIVE_LANDING_GRACE_TAG, landingGrace - 1);
            } else {
                persistentData.remove(DIVE_LANDING_GRACE_TAG);
            }
        }
        int landingGrace = persistentData.getInt(DIVE_LANDING_GRACE_TAG);
        if ((active || landingGrace > 0)
                && (player.onGround() || isDiveImpactImminent(player, movement))) {
            float peakSpeed = Math.max(
                    persistentData.getFloat(DIVE_PEAK_SPEED_TAG),
                    (float) Math.max(0.0D, -movement.y));
            emitDiveImpact(player, diveIntensity(peakSpeed, selectedSpeed));
            clearDiveState(player, false);
            persistentData.remove(DIVE_LANDING_GRACE_TAG);
            return;
        }
        if (player.onGround()) {
            clearDiveState(player, false);
            persistentData.remove(DIVE_LANDING_GRACE_TAG);
            return;
        }

        if (!downwardSmashEnabled) {
            clearDiveState(player, false);
            return;
        }

        if (active) {
            if (movement.y >= DIVE_PULL_OUT_SPEED) {
                clearDiveState(player, false);
                return;
            }
            float descentSpeed = (float) -movement.y;
            float peakSpeed = Math.max(
                    persistentData.getFloat(DIVE_PEAK_SPEED_TAG), descentSpeed);
            persistentData.putFloat(DIVE_PEAK_SPEED_TAG, peakSpeed);
            int diveAge = persistentData.getInt(DIVE_AGE_TAG) + 1;
            persistentData.putInt(DIVE_AGE_TAG, diveAge);
            emitDiveTrail(player, diveIntensity(peakSpeed, selectedSpeed), diveAge);
            return;
        }

        if (cooldown > 0 || !isStraightDownDive(player, movement)) {
            return;
        }

        float descentSpeed = (float) -movement.y;
        float intensity = diveIntensity(descentSpeed, selectedSpeed);
        persistentData.putBoolean(DIVE_ACTIVE_TAG, true);
        persistentData.putFloat(DIVE_PEAK_SPEED_TAG, descentSpeed);
        persistentData.putInt(DIVE_COOLDOWN_TAG, DIVE_REENTRY_COOLDOWN_TICKS);
        persistentData.putInt(DIVE_AGE_TAG, 0);
        emitDiveStart(player, intensity);
        emitDiveTrail(player, intensity, 0);
        ModPayloads.syncJindanFlightDive(player, false, intensity);
    }

    private static boolean isStraightDownDive(ServerPlayer player, Vec3 movement) {
        if (player.isShiftKeyDown()
                && movement.y <= -DIVE_SHIFT_MIN_DESCENT_SPEED) {
            return true;
        }
        if (movement.y > -DIVE_MIN_DESCENT_SPEED) {
            return false;
        }
        double horizontalSpeed = Math.sqrt(
                movement.x * movement.x + movement.z * movement.z);
        return player.getXRot() >= DIVE_MIN_PITCH
                || -movement.y >= horizontalSpeed * DIVE_VERTICAL_DOMINANCE;
    }

    private static boolean isDiveImpactImminent(ServerPlayer player, Vec3 movement) {
        if (player.onGround()) {
            return true;
        }
        if (movement.y > 0.01D) {
            return false;
        }
        double lookAhead = Mth.clamp(
                Math.max(0.0D, -movement.y) + 0.30D,
                0.30D,
                1.50D);
        return !player.serverLevel().noCollision(
                player,
                player.getBoundingBox().move(0.0D, -lookAhead, 0.0D));
    }

    private static float diveIntensity(float descentSpeed, float selectedSpeed) {
        float speedProgress = Mth.clamp(
                (selectedSpeed - MIN_FLYING_SPEED) / (MAX_FLYING_SPEED - MIN_FLYING_SPEED),
                0.0F, 1.0F);
        return Mth.clamp(
                0.85F + Math.max(0.0F, descentSpeed - (float) DIVE_MIN_DESCENT_SPEED)
                        * 0.75F + speedProgress * 0.35F,
                0.85F, 2.0F);
    }

    private static void emitDiveStart(ServerPlayer player, float intensity) {
        Vec3 center = player.position().add(0.0D, player.getBbHeight() * 0.52D, 0.0D);
        Vec3 movement = player.getDeltaMovement();
        Vec3 direction = movement.lengthSqr() > 0.001D
                ? movement.normalize()
                : new Vec3(0.0D, -1.0D, 0.0D);
        Vec3 ringCenter = center.subtract(direction.scale(0.72D));

        emitSonicShockwave(player, ringCenter, direction, intensity);

        playSonicFlightBoom(player, 1.15F * intensity, 0.76F, false);
    }

    private static void emitDiveTrail(ServerPlayer player, float intensity, int diveAge) {
        Vec3 movement = player.getDeltaMovement();
        if (movement.lengthSqr() < 0.001D) {
            return;
        }
        Vec3 direction = movement.normalize();
        Vec3 origin = player.position().add(0.0D, player.getBbHeight() * 0.48D, 0.0D);

        int pulse = Math.floorMod(diveAge, 18);
        if (pulse == 5 || pulse == 14) {
            Vec3 ringCenter = origin.subtract(direction.scale(0.82D));
            emitSonicShockwave(player, ringCenter, direction, intensity * 0.78F);
        }
    }

    private static void emitDiveImpact(ServerPlayer player, float intensity) {
        Vec3 impact = diveImpactPoint(player);

        emitSmashShockwave(player, impact.add(0.0D, 0.10D, 0.0D), intensity);

        playSonicFlightBoom(player, 1.45F * intensity, 0.60F, true);
        ModPayloads.syncJindanFlightDive(player, true, intensity);
    }

    private static Vec3 diveImpactPoint(ServerPlayer player) {
        Vec3 start = new Vec3(
                player.getX(),
                player.getBoundingBox().minY + 0.20D,
                player.getZ());
        Vec3 end = start.add(0.0D, -4.0D, 0.0D);
        var hit = player.serverLevel().clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player));
        if (hit.getType() != HitResult.Type.MISS) {
            return hit.getLocation().add(0.0D, 0.06D, 0.0D);
        }
        return player.position().add(0.0D, 0.08D, 0.0D);
    }

    private static void emitSonicShockwave(
            ServerPlayer player, Vec3 center, Vec3 direction, float intensity) {
        Vec3 normal = direction.lengthSqr() > 0.001D
                ? direction.normalize()
                : new Vec3(0.0D, 0.0D, 1.0D);
        float encodedIntensity = Mth.clamp(intensity, 0.75F, 2.25F);
        player.serverLevel().sendParticles(
                ModParticles.JINDAN_SONIC_RING.get(),
                center.x, center.y, center.z,
                0,
                normal.x * encodedIntensity,
                normal.y * encodedIntensity,
                normal.z * encodedIntensity,
                1.0D);
    }

    private static void emitSmashShockwave(
            ServerPlayer player, Vec3 center, float intensity) {
        player.serverLevel().sendParticles(
                ModParticles.JINDAN_SMASH_RING.get(),
                center.x, center.y, center.z,
                0,
                Mth.clamp(intensity, 0.75F, 2.25F),
                0.0D,
                0.0D,
                1.0D);
    }

    private static void emitFlightRing(
            ServerPlayer player,
            Vec3 center,
            Vec3 direction,
            int points,
            double radius,
            double outwardSpeed,
            DustParticleOptions color,
            boolean addEndRodGlow) {
        Vec3 normal = direction.lengthSqr() > 0.001D
                ? direction.normalize()
                : new Vec3(0.0D, -1.0D, 0.0D);
        Vec3 side = perpendicularTo(normal);
        Vec3 localUp = side.cross(normal).normalize();

        for (int point = 0; point < points; point++) {
            double angle = Math.PI * 2.0D * point / points;
            Vec3 radial = side.scale(Math.cos(angle)).add(localUp.scale(Math.sin(angle)));
            Vec3 ringPoint = center.add(radial.scale(radius));
            player.serverLevel().sendParticles(color,
                    ringPoint.x, ringPoint.y, ringPoint.z,
                    0, radial.x, radial.y, radial.z, outwardSpeed);
            if (addEndRodGlow && point % 3 == 0) {
                player.serverLevel().sendParticles(ParticleTypes.END_ROD,
                        ringPoint.x, ringPoint.y, ringPoint.z,
                        0, radial.x, radial.y, radial.z, outwardSpeed * 0.72D);
            }
        }
    }

    private static void emitWhiteLances(
            ServerPlayer player,
            Vec3 origin,
            Vec3 direction,
            int lanceCount,
            double length) {
        Vec3 normal = direction.lengthSqr() > 0.001D
                ? direction.normalize()
                : new Vec3(0.0D, -1.0D, 0.0D);
        Vec3 side = perpendicularTo(normal);
        Vec3 localUp = side.cross(normal).normalize();
        int segments = 6;
        for (int lance = 0; lance < lanceCount; lance++) {
            double centered = lance - (lanceCount - 1) * 0.5D;
            Vec3 offset = side.scale(centered * 0.13D)
                    .add(localUp.scale(Math.sin(lance * 2.17D) * 0.16D));
            for (int segment = 0; segment < segments; segment++) {
                double distance = 0.28D + length * segment / (segments - 1.0D);
                Vec3 point = origin.subtract(normal.scale(distance)).add(offset);
                player.serverLevel().sendParticles(
                        segment % 3 == 0 ? ParticleTypes.END_ROD : FLIGHT_WHITE,
                        point.x, point.y, point.z,
                        1, 0.012D, 0.012D, 0.012D, 0.003D);
            }
        }
    }

    private static Vec3 perpendicularTo(Vec3 direction) {
        Vec3 reference = Math.abs(direction.y) > 0.90D
                ? new Vec3(1.0D, 0.0D, 0.0D)
                : new Vec3(0.0D, 1.0D, 0.0D);
        return direction.cross(reference).normalize();
    }

    private static void playSonicFlightBoom(
            ServerPlayer player, float volume, float pitch, boolean impact) {
        ServerLevel level = player.serverLevel();
        float clampedPitch = Mth.clamp(pitch, 0.50F, 1.80F);
        level.playSound(null, player.blockPosition(),
                SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS,
                volume * 0.82F, Mth.clamp(clampedPitch + 0.20F, 0.50F, 2.0F));
        level.playSound(null, player.blockPosition(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS,
                volume, clampedPitch);
        level.playSound(null, player.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS,
                volume * (impact ? 1.05F : 0.72F),
                Mth.clamp(clampedPitch * (impact ? 0.86F : 1.18F), 0.50F, 2.0F));
    }

    private static void clearDiveState(ServerPlayer player, boolean clearCooldown) {
        player.getPersistentData().remove(DIVE_ACTIVE_TAG);
        player.getPersistentData().remove(DIVE_PEAK_SPEED_TAG);
        player.getPersistentData().remove(DIVE_AGE_TAG);
        if (clearCooldown) {
            player.getPersistentData().remove(DIVE_COOLDOWN_TAG);
        }
    }

    private static void resetDiveState(ServerPlayer player) {
        clearDiveState(player, true);
        player.getPersistentData().remove(DIVE_LANDING_GRACE_TAG);
    }

    private static void resetFastFlightState(ServerPlayer player) {
        player.getPersistentData().remove(FAST_FLIGHT_DISTANCE_TAG);
        player.getPersistentData().remove(FAST_FLIGHT_ACTIVE_TAG);
    }

    private static void resetPeriodicFlightBoom(ServerPlayer player) {
        player.getPersistentData().remove(PERIODIC_FLIGHT_BOOM_TICKS_TAG);
    }

    private static void emitPeriodicFlightBoom(ServerPlayer player, float selectedSpeed) {
        Vec3 direction = player.getDeltaMovement().lengthSqr() > 0.001D
                ? player.getDeltaMovement().normalize()
                : player.getLookAngle();
        Vec3 center = player.position().add(0.0D, player.getBbHeight() * 0.52D, 0.0D);
        Vec3 ringCenter = center.subtract(direction.scale(1.25D));
        float speedProgress = Mth.clamp(
                (selectedSpeed - DOWNWARD_SMASH_SPEED)
                        / (MAX_FLYING_SPEED - DOWNWARD_SMASH_SPEED),
                0.0F, 1.0F);
        float strength = Mth.lerp(speedProgress, 1.3F, 1.65F);

        emitSonicShockwave(player, ringCenter, direction, strength);
        playSonicFlightBoom(
                player,
                Mth.lerp(speedProgress, 1.26F, 1.42F),
                Mth.lerp(speedProgress, 0.82F, 0.72F),
                false);
    }

    private static void emitSpeedLayerBurst(
            ServerPlayer player, int increasedLayers, float selectedSpeed) {
        Vec3 center = player.position().add(0.0D, player.getBbHeight() * 0.52D, 0.0D);
        float speedProgress = Mth.clamp(
                (selectedSpeed - MIN_FLYING_SPEED) / (MAX_FLYING_SPEED - MIN_FLYING_SPEED),
                0.0F, 1.0F);
        float strength = Mth.clamp(
                0.85F + increasedLayers * 0.12F + speedProgress * 0.35F,
                0.9F, 1.8F);
        Vec3 direction = player.getDeltaMovement().lengthSqr() > 0.001D
                ? player.getDeltaMovement().normalize()
                : player.getLookAngle();
        Vec3 ringCenter = center.subtract(direction.scale(0.72D));

        emitSonicShockwave(player, ringCenter, direction, strength);

        float boomPitch = Mth.lerp(speedProgress, 1.05F, 0.72F);
        playSonicFlightBoom(player, 0.9F + strength * 0.28F, boomPitch, false);
    }

    public static void resetControlSync(ServerPlayer player) {
        player.getPersistentData().remove(CONTROL_SYNC_INITIALIZED_TAG);
    }

    /** True only for flight supplied by cultivation mechanics, never vanilla creative flight. */
    public static boolean isCultivationFlightActive(ServerPlayer player) {
        if (player == null || player.isCreative() || player.isSpectator()) {
            return false;
        }
        return player.hasEffect(ModEffects.YUFENG_JUE)
                || player.hasEffect(ModEffects.FROST_FLIGHT)
                || (isUndeadFlightAvailable(player) && player.getAbilities().flying)
                || (player.getPersistentData().getBoolean(ENABLED_TAG)
                && player.getPersistentData().getBoolean(GRANTED_TAG)
                && player.getAbilities().flying);
    }

    public static double fastFlightEntryDistance() {
        return FAST_FLIGHT_ENTRY_DISTANCE;
    }

    public static boolean isFastFlightSpeed(float speed) {
        return speed + 0.0001F >= FLIGHT_MODE_SPEED;
    }

    public static boolean isFastFlightMovement(Vec3 movement) {
        return movement != null
                && movement.lengthSqr() >= FAST_FLIGHT_MIN_MOVEMENT_SQR
                && movement.y <= FAST_FLIGHT_MAX_ASCENT_SPEED;
    }

    public static boolean isDownwardSmashSpeed(float speed) {
        return speed + 0.0001F >= DOWNWARD_SMASH_SPEED;
    }

    private static void revokeIfOwned(ServerPlayer player) {
        FlyingSwordEntity.stopSwordFlight(player);
        resetDiveState(player);
        resetFastFlightState(player);
        resetPeriodicFlightBoom(player);
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
        return clampSelectedSpeed(player, player.getPersistentData().getFloat(SPEED_TAG));
    }

    private static float clampSelectedSpeed(ServerPlayer player, float speed) {
        float cap = isUndeadFlightAvailable(player) ? MAX_FLYING_SPEED
                : maxFlyingSpeedForRealm(CultivationLevels.getRealmIndex(ModAttachments.getData(player).cultivationLevel()));
        return Math.min(cap, clampSpeed(speed));
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
        return player.hasEffect(ModEffects.YUFENG_JUE);
    }

    private static boolean isUndeadFlightAvailable(ServerPlayer player) {
        return player != null
                && !player.isCreative()
                && !player.isSpectator()
                && player.getAbilities().mayfly
                && UndeadControl.hasUndeadPowers(player)
                && UndeadControl.isAtLeastRank(player, "飞僵");
    }
}
