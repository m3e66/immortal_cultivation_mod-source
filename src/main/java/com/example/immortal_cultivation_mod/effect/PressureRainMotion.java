package com.example.immortal_cultivation_mod.effect;

import net.minecraft.world.phys.Vec3;

/** A rain drop is held at a world position; gravity resumes after the field releases it. */
public final class PressureRainMotion {
    private PressureRainMotion() {}
    public record Step(Vec3 position, double velocity) {}
    public static Step advance(Vec3 position, double velocity, boolean held) {
        if (held) return new Step(position, 0);
        double falling = Math.max(-.7, velocity - .035);
        return new Step(position.add(0, falling, 0), falling);
    }
}
