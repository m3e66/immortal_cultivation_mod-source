package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

public final class ClientTargetLock {
    private static final double LOCK_RANGE = 64.0D;
    private static final float CAMERA_TURN_SPEED = 0.12F;
    private static final float OLD_ROTATION_CATCHUP = 0.28F;
    private static final float MAX_YAW_STEP = 5.5F;
    private static final float MAX_PITCH_STEP = 3.75F;
    private static final double TARGET_POINT_SMOOTHING = 0.35D;
    private static UUID lockedTargetId;
    private static Vec3 smoothedLookPoint;

    private ClientTargetLock() {
    }

    public static void toggleLookingAt(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.screen != null) {
            return;
        }

        Entity target = findLookTarget(mc);
        if (target == null) {
            clear(mc, true);
            return;
        }

        if (target.getUUID().equals(lockedTargetId)) {
            clear(mc, true);
            return;
        }

        lockedTargetId = target.getUUID();
        smoothedLookPoint = null;
        mc.player.displayClientMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID + ".target_locked",
                target.getDisplayName()), true);
    }

    public static Optional<Entity> getLockedTarget(Minecraft mc) {
        if (lockedTargetId == null || mc.level == null) {
            return Optional.empty();
        }
        Entity target = null;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity.getUUID().equals(lockedTargetId)) {
                target = entity;
                break;
            }
        }
        if (target == null || !target.isAlive() || target instanceof ItemEntity || target.isRemoved()) {
            lockedTargetId = null;
            return Optional.empty();
        }
        return Optional.of(target);
    }

    public static boolean hasLockedTarget(Minecraft mc) {
        return getLockedTarget(mc).isPresent();
    }

    public static void tickCamera(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.screen != null) {
            return;
        }

        Optional<Entity> target = getLockedTarget(mc);
        if (target.isEmpty()) {
            return;
        }

        Vec3 eye = mc.player.getEyePosition();
        Entity entity = target.get();
        Vec3 targetPos = entity.getBoundingBox().getCenter().add(0.0D, entity.getBbHeight() * 0.15D, 0.0D);
        smoothedLookPoint = smoothedLookPoint == null
                ? targetPos
                : smoothedLookPoint.lerp(targetPos, TARGET_POINT_SMOOTHING);
        targetPos = smoothedLookPoint;
        Vec3 delta = targetPos.subtract(eye);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        if (horizontal < 0.0001D && Math.abs(delta.y) < 0.0001D) {
            return;
        }

        float targetYaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0D);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(delta.y, horizontal));
        float yaw = approachAngle(mc.player.getYRot(), targetYaw, MAX_YAW_STEP, CAMERA_TURN_SPEED);
        float pitch = approachAngle(mc.player.getXRot(), targetPitch, MAX_PITCH_STEP, CAMERA_TURN_SPEED);

        mc.player.setYRot(yaw);
        mc.player.setXRot(clamp(pitch, -90.0F, 90.0F));
        mc.player.yRotO = approachAngle(mc.player.yRotO, yaw, MAX_YAW_STEP, OLD_ROTATION_CATCHUP);
        mc.player.xRotO = approachAngle(mc.player.xRotO, pitch, MAX_PITCH_STEP, OLD_ROTATION_CATCHUP);
    }

    public static void clear(Minecraft mc, boolean showMessage) {
        boolean hadTarget = lockedTargetId != null;
        lockedTargetId = null;
        smoothedLookPoint = null;
        if (showMessage && hadTarget && mc.player != null) {
            mc.player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".target_lock_cleared"), true);
        }
    }

    private static Entity findLookTarget(Minecraft mc) {
        if (mc.hitResult instanceof EntityHitResult entityHit && canLock(mc, entityHit.getEntity())) {
            return entityHit.getEntity();
        }

        Vec3 start = mc.player.getEyePosition();
        Vec3 end = start.add(mc.player.getViewVector(1.0F).scale(LOCK_RANGE));
        HitResult blockHit = mc.level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        double maxDistance = blockHit.getType() == HitResult.Type.MISS
                ? LOCK_RANGE * LOCK_RANGE
                : start.distanceToSqr(blockHit.getLocation());
        AABB searchBox = mc.player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);

        return mc.level.getEntities(mc.player, searchBox, entity -> canLock(mc, entity)).stream()
                .map(entity -> hitDistance(start, end, entity).map(distance -> new TargetCandidate(entity, distance)).orElse(null))
                .filter(candidate -> candidate != null && candidate.distance() <= maxDistance)
                .min(Comparator.comparingDouble(TargetCandidate::distance))
                .map(TargetCandidate::entity)
                .orElse(null);
    }

    private static boolean canLock(Minecraft mc, Entity entity) {
        return entity != null
                && entity != mc.player
                && !(entity instanceof ItemEntity)
                && entity.isAlive()
                && !entity.isSpectator()
                && entity.isPickable();
    }

    private static Optional<Double> hitDistance(Vec3 start, Vec3 end, Entity entity) {
        AABB box = entity.getBoundingBox().inflate(entity.getPickRadius() + 0.25D);
        return box.clip(start, end).map(start::distanceToSqr);
    }

    private static float approachAngle(float current, float target, float maxStep, float smoothing) {
        float delta = wrapDegrees(target - current);
        float step = clamp(delta * smoothing, -maxStep, maxStep);
        return current + step;
    }

    private static float wrapDegrees(float value) {
        value %= 360.0F;
        if (value >= 180.0F) {
            value -= 360.0F;
        }
        if (value < -180.0F) {
            value += 360.0F;
        }
        return value;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private record TargetCandidate(Entity entity, double distance) {
    }
}
