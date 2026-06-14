package com.example.immortal_cultivation_mod.entity;

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

    public static void flare(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 18, 0.30D, 0.25D, 0.30D, 0.07D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 12, 0.24D, 0.20D, 0.24D, 0.10D);
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

    public static void smoke(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, 30, 0.55D, 0.40D, 0.55D, 0.05D);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y, pos.z, 8, 0.40D, 0.25D, 0.40D, 0.02D);
    }

    public static void wind(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y, pos.z, 5, 0.35D, 0.20D, 0.35D, 0.0D);
        level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 22, 0.50D, 0.30D, 0.50D, 0.12D);
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
}
