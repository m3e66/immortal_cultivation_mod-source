package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.spell.ElementalSpellVfx;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class SpellImpactParticles {
    private SpellImpactParticles() {
    }

    public static void fire(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 28, 0.45D, 0.35D, 0.45D, 0.08D);
        level.sendParticles(ParticleTypes.LAVA, pos.x, pos.y, pos.z, 10, 0.35D, 0.25D, 0.35D, 0.04D);
        level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 18, 0.45D, 0.30D, 0.45D, 0.05D);
    }

    public static void fireExplosion(ServerLevel level, Vec3 pos, double radius) {
        // The white-hot core arrives first; smoke is deliberately lighter and slower.
        level.sendParticles(ParticleTypes.FLASH,
                pos.x, pos.y, pos.z, 2, 0.12D, 0.12D, 0.12D, 0.0D);
        level.sendParticles(ParticleTypes.FLAME,
                pos.x, pos.y, pos.z, 42, 0.55D, 0.45D, 0.55D, 0.20D);
        level.sendParticles(ParticleTypes.FLAME,
                pos.x, pos.y, pos.z, 36, 0.60D, 0.50D, 0.60D, 0.23D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                pos.x, pos.y + 0.15D, pos.z, 18, 0.65D, 0.38D, 0.65D, 0.075D);
        expandingRing(level, pos.add(0.0D, 0.12D, 0.0D),
                ParticleTypes.FLAME, radius, 32, 0.22D);
        expandingRing(level, pos.add(0.0D, 0.07D, 0.0D),
                ParticleTypes.CLOUD, radius * 0.92D, 28, 0.18D);
    }

    public static void flare(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z, 8, 0.18D, 0.15D, 0.18D, 0.035D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 3, 0.14D, 0.12D, 0.14D, 0.04D);
    }

    public static void light(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 26, 0.45D, 0.45D, 0.45D, 0.10D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 35, 0.55D, 0.45D, 0.55D, 0.14D);
        level.sendParticles(ParticleTypes.FLASH, pos.x, pos.y, pos.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    public static void wood(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 22, 0.35D, 0.35D, 0.35D, 0.16D);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y, pos.z, 14, 0.30D, 0.25D, 0.30D, 0.06D);
    }

    public static void water(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.SPLASH, pos.x, pos.y, pos.z, 36, 0.55D, 0.25D, 0.55D, 0.12D);
        level.sendParticles(ParticleTypes.FALLING_WATER, pos.x, pos.y + 0.25D, pos.z, 20, 0.45D, 0.20D, 0.45D, 0.03D);
    }

    public static void waterSlash(ServerLevel level, Vec3 pos, Vec3 direction) {
        Vec3 forward = direction.lengthSqr() < 1.0E-6D
                ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
        Vec3 side = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-6D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            side = side.normalize();
        }
        for (int i = -5; i <= 5; i++) {
            double t = i / 5.0D;
            Vec3 point = pos.add(side.scale(t * 0.7D)).add(0.0D,
                    0.36D - Math.abs(t) * 0.25D, 0.0D);
            ElementalSpellVfx.directed(level,
                    i % 2 == 0 ? ParticleTypes.SPLASH : ParticleTypes.FALLING_WATER,
                    point, forward.scale(0.12D).add(side.scale(t * 0.035D)));
        }
    }

    public static void smoke(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, 30, 0.55D, 0.40D, 0.55D, 0.05D);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y, pos.z, 8, 0.40D, 0.25D, 0.40D, 0.02D);
    }

    public static void wind(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y, pos.z, 5, 0.35D, 0.20D, 0.35D, 0.0D);
        level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 22, 0.50D, 0.30D, 0.50D, 0.12D);
    }

    public static void shockRing(
            ServerLevel level, Vec3 pos, net.minecraft.core.particles.ParticleOptions particle,
            double radius, int points, double speed) {
        expandingRing(level, pos, particle, radius, points, speed);
    }

    public static void earth(ServerLevel level, Vec3 pos) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()),
                pos.x, pos.y, pos.z, 24, 0.45D, 0.35D, 0.45D, 0.12D);
        level.sendParticles(ParticleTypes.POOF, pos.x, pos.y, pos.z, 10, 0.35D, 0.25D, 0.35D, 0.05D);
    }

    public static void lightning(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 55, 1.0D, 0.8D, 1.0D, 0.28D);
        level.sendParticles(ParticleTypes.WAX_OFF, pos.x, pos.y + 0.2D, pos.z, 24, 0.75D, 0.5D, 0.75D, 0.18D);
        level.sendParticles(ParticleTypes.FLASH, pos.x, pos.y, pos.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static void expandingRing(
            ServerLevel level, Vec3 pos, net.minecraft.core.particles.ParticleOptions particle,
            double radius, int points, double speed) {
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;
            Vec3 radial = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
            ElementalSpellVfx.directed(level, particle,
                    pos.add(radial.scale(radius * 0.22D)), radial.scale(speed));
        }
    }
}
