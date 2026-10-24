package com.example.immortal_cultivation_mod.entity;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/** Physical, pathfinding-based flanks and short contact pounces; never digs or teleports. */
public final class WolfPackGoal extends Goal {
    private static final String PIN_COOLDOWN = "CultivationWolfPinUntil";
    private static final String POUNCE_COOLDOWN = "CultivationWolfPackPounceUntil";
    private final Wolf wolf;
    private LivingEntity target;
    private List<Wolf> pack = List.of();
    private long nextPackScan, nextPath, nextHowl, nextBite, nextPounce;
    private int pounceTicks;
    private boolean pounceHit;
    private Vec3 lastTargetPosition, travel = new Vec3(0, 0, 1);
    private Vec3 targetMotion = Vec3.ZERO, lastSeenPosition, lastDestination;
    private long lastSeen;
    private long lastSample;

    public WolfPackGoal(Wolf wolf) {
        this.wolf = wolf;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }
    @Override public boolean canUse() {
        return WolfPackBehavior.wildAdult(wolf) && !BeastDazeControl.blocksMoves(wolf)
                && WolfPackBehavior.validTarget(wolf, wolf.getTarget()) && wolf.distanceToSqr(wolf.getTarget()) < 32 * 32;
    }
    @Override public boolean canContinueToUse() { return canUse() && wolf.getTarget() == target; }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void start() {
        target = wolf.getTarget();
        lastTargetPosition = target.position();
        lastSample = wolf.level().getGameTime();
        lastSeen = lastSample;
        lastSeenPosition = target.position();
        targetMotion = Vec3.ZERO;
        lastDestination = null;
        // Every packmate uses the same frame even while the prey stands still.
        travel = target.getLookAngle().multiply(1, 0, 1).normalize();
        if (travel.lengthSqr() < .001) travel = new Vec3(0, 0, 1);
        nextPackScan = nextPath = 0;
        nextPounce = Math.max(nextPounce, lastSample + 12 + Math.floorMod(wolf.getUUID().hashCode(), 20));
    }
    @Override public void stop() {
        wolf.getNavigation().stop();
        if (!WolfPackBehavior.validTarget(wolf, wolf.getTarget())) wolf.setTarget(null);
        target = null; pack = List.of(); pounceTicks = 0;
    }
    @Override public void tick() {
        if (!canContinueToUse()) { stop(); return; }
        long now = wolf.level().getGameTime();
        boolean visible = wolf.hasLineOfSight(target);
        if (visible) {
            lastSeen = now;
            lastSeenPosition = target.position();
        } else {
            // Search the last observed position, never track unseen prey through terrain indefinitely.
            if (now - lastSeen > 80) { wolf.setTarget(null); stop(); return; }
            if (now >= nextPath) {
                moveSafely(lastSeenPosition, 1.05);
                nextPath = now + 20;
            }
            pounceTicks = 0;
            return;
        }
        wolf.getLookControl().setLookAt(target, 30, 30);
        if (now >= nextPackScan) {
            pack = WolfPackBehavior.combatPack(wolf, target);
            if (WolfPackBehavior.leader(pack) == wolf && now >= nextHowl) {
                if (WolfPackBehavior.rally(wolf, target) > 0) {
                    wolf.playSound(SoundEvents.WOLF_HOWL, .8F, .95F);
                    if (wolf instanceof SilverMistWolfEntity silver) silver.packHowl();
                }
                nextHowl = now + 120;
                pack = WolfPackBehavior.combatPack(wolf, target);
            }
            nextPackScan = now + 20;
        }
        int size = Math.max(1, pack.size());
        int slot = Math.max(0, pack.indexOf(wolf));
        boolean retreating = size > 1 && wolf.getHealth() <= wolf.getMaxHealth() * .35F;
        boolean attacking = WolfPackBehavior.attackTurn(pack, wolf, now);
        if (now - lastSample >= 10) {
            Vec3 delta = target.position().subtract(lastTargetPosition).multiply(1, 0, 1);
            // Server players do not reliably expose walking velocity. Measure their actual positions.
            if (delta.lengthSqr() < 144) {
                targetMotion = delta.scale(1.0 / Math.max(1, now - lastSample));
                if (delta.lengthSqr() > .04) travel = travel.scale(.35).add(delta.normalize().scale(.65)).normalize();
            } else targetMotion = Vec3.ZERO;
            lastTargetPosition = target.position(); lastSample = now;
        }
        if (pounceTicks > 0) {
            pounceTicks--;
            if (!pounceHit && wolf.getBoundingBox().inflate(.3).intersects(target.getBoundingBox())
                    && wolf.hasLineOfSight(target)) {
                pounceHit = true;
                if (bite(now) && target.getPersistentData().getLong(PIN_COOLDOWN) <= now) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, false, false));
                    target.getPersistentData().putLong(PIN_COOLDOWN, now + 40);
                    target.knockback(.3, wolf.getX() - target.getX(), wolf.getZ() - target.getZ());
                }
            }
            return;
        }
        if (attacking && tryPounce(now)) return;
        if (attacking && now >= nextBite && wolf.isWithinMeleeAttackRange(target)) bite(now);
        if (now >= nextPath) {
            Vec3 lead = WolfPackBehavior.escapeLead(targetMotion);
            int lane = 1;
            for (int i = 0; i < slot; i++) if (!WolfPackBehavior.attackTurn(pack, pack.get(i), now)) lane++;
            Vec3 slotPos = attacking ? target.position().add(lead.scale(.35))
                    : WolfPackBehavior.formationSlot(target.position().add(lead), travel, size, lane);
            if (retreating) {
                Vec3 away = wolf.position().subtract(target.position()).multiply(1, 0, 1);
                if (away.lengthSqr() < .01) away = travel.scale(-1);
                slotPos = target.position().add(away.normalize().scale(6));
            }
            if (!attacking) {
                Vec3 separation = Vec3.ZERO;
                for (Wolf other : pack) if (other != wolf && other.isAlive() && other.distanceToSqr(wolf) < 2.25) {
                    Vec3 away = wolf.position().subtract(other.position()).multiply(1, 0, 1);
                    if (away.lengthSqr() > .001) separation = separation.add(away.normalize().scale(.6));
                }
                if (separation.lengthSqr() > 1) separation = separation.normalize();
                slotPos = slotPos.add(separation);
            }
            boolean moved = moveSafely(slotPos, attacking ? 1.2 : retreating ? 1.3 : 1.15);
            if (!moved) {
                // Try the neighboring lane instead of collapsing the whole pack onto its prey.
                Vec3 side = new Vec3(-travel.z, 0, travel.x).scale(slot % 2 == 0 ? 1.5 : -1.5);
                moved = moveSafely(slotPos.add(side), 1.1);
            }
            if (!moved) wolf.getNavigation().stop();
            nextPath = now + 10;
        }
    }

    private boolean bite(long now) {
        if (now < nextBite || !WolfPackBehavior.validTarget(wolf, target) || BeastDazeControl.blocksMoves(wolf)) return false;
        nextBite = now + 25;
        wolf.swing(InteractionHand.MAIN_HAND);
        return wolf.doHurtTarget(target);
    }

    /** Return false when obstructed, over a pit, or still recovering from the preceding pounce. */
    boolean tryPounce(long now) {
        if (now < nextPounce || !wolf.onGround() || wolf.isInWaterOrBubble() || !wolf.hasLineOfSight(target)
                || target.getPersistentData().getLong(POUNCE_COOLDOWN) > now
                || BeastDazeControl.blocksMoves(wolf)
                || Math.abs(target.getY() - wolf.getY()) > 1.25) return false;
        double distance = wolf.distanceTo(target);
        if (distance < 2 || distance > 4.5 || safeGround(target.position()) == null) return false;
        Vec3 flat = target.position().subtract(wolf.position()).multiply(1, 0, 1);
        for (int i = 1; i <= 4; i++) {
            Vec3 offset = flat.scale(i / 4.0).add(0, Math.sin(i * Math.PI / 5) * .65, 0);
            if (!wolf.level().noCollision(wolf, wolf.getBoundingBox().move(offset))) return false;
            // Do not leap a ravine merely because its far edge is walkable.
            if (safeGround(wolf.position().add(flat.scale(i / 4.0))) == null) return false;
        }
        wolf.getNavigation().stop();
        target.getPersistentData().putLong(POUNCE_COOLDOWN, now + 16);
        Vec3 impulse = flat.normalize().scale(Math.min(.7, distance * .18));
        wolf.setDeltaMovement(impulse.x, .42, impulse.z);
        wolf.hasImpulse = true;
        if (wolf instanceof SilverMistWolfEntity silver) silver.packPounce();
        pounceTicks = 12; pounceHit = false;
        nextPounce = now + 60 + Math.floorMod(wolf.getUUID().hashCode(), 20);
        return true;
    }

    private boolean moveSafely(Vec3 desired, double speed) {
        if (lastDestination != null && lastDestination.distanceToSqr(desired) < 1
                && !wolf.getNavigation().isDone()) return true;
        BlockPos safe = safeGround(desired);
        Path path = safe == null ? null : wolf.getNavigation().createPath(safe, 0);
        if (path == null || !path.canReach()) return false;
        lastDestination = desired;
        return wolf.getNavigation().moveTo(path, speed);
    }

    private BlockPos safeGround(Vec3 desired) {
        BlockPos center = BlockPos.containing(desired);
        for (int y : new int[]{0, 1, -1, 2, -2}) {
            BlockPos pos = center.offset(0, y, 0);
            if (!wolf.level().hasChunkAt(pos) || !wolf.level().getWorldBorder().isWithinBounds(pos)
                    || !wolf.level().getFluidState(pos).isEmpty()
                    || !wolf.level().getBlockState(pos.below()).isFaceSturdy(wolf.level(), pos.below(), Direction.UP)) continue;
            Vec3 destination = Vec3.atBottomCenterOf(pos);
            if (wolf.level().noCollision(wolf, wolf.getBoundingBox().move(destination.subtract(wolf.position())))) return pos;
        }
        return null;
    }
}
