package com.example.immortal_cultivation_mod.effect;

import com.example.immortal_cultivation_mod.particle.ModParticles;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

final class UndeadEffectParticles {
    private static final DustParticleOptions CORPSE_GREEN =
            new DustParticleOptions(new Vector3f(0.15F, 0.38F, 0.12F), 0.75F);
    private static final DustParticleOptions CORPSE_BLACK =
            new DustParticleOptions(new Vector3f(0.018F, 0.026F, 0.021F), 0.78F);
    private static final DustParticleOptions COLD_CYAN =
            new DustParticleOptions(new Vector3f(0.18F, 0.72F, 0.67F), 0.62F);
    private static final DustParticleOptions DROUGHT_RED =
            new DustParticleOptions(new Vector3f(0.62F, 0.10F, 0.035F), 0.9F);

    private UndeadEffectParticles() {
    }

    static void corpsePoison(ServerLevel level, LivingEntity entity) {
        Vec3 forward = entity.getLookAngle().normalize();
        Vec3 mouth = entity.getEyePosition().add(forward.scale(0.18D)).add(0.0D, -0.10D, 0.0D);
        directed(level, CORPSE_BLACK, mouth,
                forward.scale(0.018D).add(0.0D, -0.008D, 0.0D));
        directed(level, CORPSE_GREEN, mouth.add(0.0D, -0.03D, 0.0D),
                forward.scale(0.012D).add(0.0D, 0.006D, 0.0D));
        int leaks = 5;
        for (int i = 0; i < leaks; i++) {
            double side = (i % 2 == 0 ? -1.0D : 1.0D) * entity.getBbWidth()
                    * (0.18D + 0.09D * (i % 3));
            Vec3 wound = entity.position().add(side,
                    entity.getBbHeight() * (0.25D + 0.13D * i),
                    (level.random.nextDouble() - 0.5D) * entity.getBbWidth() * 0.45D);
            directed(level, i % 2 == 0 ? CORPSE_GREEN : ModParticles.BLACK_FLAME_ASH.get(),
                    wound, new Vec3((level.random.nextDouble() - 0.5D) * 0.012D,
                            0.012D + level.random.nextDouble() * 0.016D,
                            (level.random.nextDouble() - 0.5D) * 0.012D));
        }
    }

    static void corpseBurn(ServerLevel level, LivingEntity entity, boolean drought) {
        int patches = drought ? 8 : 4;
        for (int i = 0; i < patches; i++) {
            double angle = Math.PI * 2.0D * i / patches + entity.getId() * 0.41D;
            double radius = entity.getBbWidth() * (0.18D + 0.18D * (i % 2));
            Vec3 patch = entity.position().add(Math.cos(angle) * radius,
                    entity.getBbHeight() * (drought ? 0.08D + 0.055D * (i % 3)
                            : 0.24D + 0.14D * (i % 4)),
                    Math.sin(angle) * radius);
            ParticleOptions particle = drought
                    ? (i % 3 == 0 ? ModParticles.BLACK_FLAME_CORE.get() : DROUGHT_RED)
                    : (i % 2 == 0 ? ModParticles.BLACK_FLAME_CORE.get() : COLD_CYAN);
            directed(level, particle, patch,
                    new Vec3(0.0D, drought ? 0.018D : 0.006D, 0.0D));
            if (drought && i % 2 == 0) {
                directed(level, ParticleTypes.WHITE_SMOKE, patch,
                        new Vec3(0.0D, 0.045D, 0.0D));
            }
        }
        if (!drought) {
            level.sendParticles(CORPSE_BLACK, entity.getX(),
                    entity.getY() + entity.getBbHeight() * 0.52D, entity.getZ(),
                    4, entity.getBbWidth() * 0.24D, entity.getBbHeight() * 0.22D,
                    entity.getBbWidth() * 0.24D, 0.0D);
        }
    }

    private static void directed(ServerLevel level, ParticleOptions particle,
                                 Vec3 position, Vec3 velocity) {
        level.sendParticles(particle, position.x, position.y, position.z,
                0, velocity.x, velocity.y, velocity.z, 1.0D);
    }
}
