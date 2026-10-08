package com.example.immortal_cultivation_mod.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.control.MoveControl;

/** Smooth ordinary walking while retaining vanilla path, jumping and strafe checks. */
final class NpcMoveControl extends MoveControl {
    private float walkingSpeed;

    NpcMoveControl(PathfinderMob mob) {
        super(mob);
    }

    @Override
    protected float rotlerp(float from, float to, float maximumChange) {
        return super.rotlerp(from, to, Math.min(maximumChange, 18));
    }

    @Override
    public void tick() {
        Operation previous = operation;
        super.tick();
        if (previous == Operation.MOVE_TO && operation != Operation.JUMPING
                && mob.onGround() && !mob.isInWaterOrBubble() && !mob.isInLava()) {
            if (mob.zza == 0) {
                walkingSpeed = 0;
                return;
            }
            float heading = (float) Math.toDegrees(Math.atan2(-(wantedX - mob.getX()), wantedZ - mob.getZ()));
            // Slow down at sharp corners instead of cutting across the path at full speed.
            float corner = Mth.clamp(1 - Math.abs(Mth.wrapDegrees(heading - mob.getYRot())) / 90, .15F, 1);
            float target = mob.getSpeed() * corner;
            walkingSpeed = Mth.approach(walkingSpeed, target, Math.max(.015F, mob.getSpeed() * .2F));
            mob.setSpeed(walkingSpeed);
        } else {
            // A stopped navigation/action must never gain extra forward motion from easing.
            walkingSpeed = previous == Operation.WAIT ? 0 : mob.getSpeed();
        }
    }
}
