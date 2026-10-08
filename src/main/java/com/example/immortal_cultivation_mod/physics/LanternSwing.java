package com.example.immortal_cultivation_mod.physics;

import net.minecraft.world.phys.Vec3;

/** Damped pendulum driven by motion of the hand, with bounded angles and no idle jitter. */
public final class LanternSwing {
    public static final double MAX_ANGLE=32;
    private Vec3 anchor,velocity=Vec3.ZERO;
    private double time,pitch,roll,pitchSpeed,rollSpeed;
    public double pitch(){return pitch;}
    public double roll(){return roll;}
    public void advance(double now,Vec3 next,Vec3 right,Vec3 forward) {
        double dt=now-time;
        if(anchor==null || dt<0 || dt>.25 || next.distanceToSqr(anchor)>16) {
            anchor=next;velocity=Vec3.ZERO;time=now;pitch=roll=pitchSpeed=rollSpeed=0;return;
        }
        if(dt<=1e-6)return;
        Vec3 measured=next.subtract(anchor).scale(1/dt);
        Vec3 filtered=velocity.lerp(measured,1-Math.exp(-12*dt));
        Vec3 acceleration=filtered.subtract(velocity).scale(1/dt);
        double targetPitch=Math.clamp(-acceleration.dot(forward)*2.6,-24,24);
        double targetRoll=Math.clamp(acceleration.dot(right)*2.6,-24,24);
        int steps=Math.max(1,(int)Math.ceil(dt*120));double step=dt/steps;
        for(int i=0;i<steps;i++) {
            pitchSpeed+=(32*(targetPitch-pitch)-4.8*pitchSpeed)*step;
            rollSpeed+=(32*(targetRoll-roll)-4.8*rollSpeed)*step;
            pitch=Math.clamp(pitch+pitchSpeed*step,-MAX_ANGLE,MAX_ANGLE);
            roll=Math.clamp(roll+rollSpeed*step,-MAX_ANGLE,MAX_ANGLE);
        }
        anchor=next;velocity=filtered;time=now;
    }
}
