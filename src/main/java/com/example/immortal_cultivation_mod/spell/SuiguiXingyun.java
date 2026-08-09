package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.entity.StarfallProjectileEntity;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class SuiguiXingyun {
    private static final double AIM_RANGE = 96.0D;
    private static final double IMPACT_SPREAD = 18.0D;
    private static final DustParticleOptions GOLD_DUST =
            new DustParticleOptions(new Vector3f(1.0F, 0.72F, 0.12F), 1.35F);
    private static final DustParticleOptions WHITE_DUST =
            new DustParticleOptions(new Vector3f(0.92F, 0.96F, 1.0F), 0.9F);

    private SuiguiXingyun() {
    }

    public static boolean cast(ServerPlayer caster, float chargeScale) {
        HitResult hit = caster.pick(AIM_RANGE, 0.0F, false);
        Vec3 target = hit.getType() == HitResult.Type.MISS
                ? caster.getEyePosition().add(caster.getLookAngle().scale(48.0D))
                : hit.getLocation();
        return castAt(caster, target, chargeScale);
    }

    public static boolean castAt(LivingEntity caster, Vec3 target, float chargeScale) {
        if (!(caster.level() instanceof ServerLevel level)) {
            return false;
        }
        float scale = Mth.clamp(chargeScale, 1.0F, 2.0F);
        int projectileCount = 5 + Math.round((scale - 1.0F) * 5.0F);
        Vec3 center = new Vec3(target.x, target.y + 0.2D, target.z);
        spawnCompressionVfx(level, caster);
        spawnTargetVfx(level, center, scale);

        double goldenAngle = Math.PI * (3.0D - Math.sqrt(5.0D));
        for (int i = 0; i < projectileCount; i++) {
            double radius = i == 0 ? 0.0D
                    : IMPACT_SPREAD * Math.sqrt((double) i / Math.max(1, projectileCount - 1));
            double angle = i * goldenAngle + caster.getYRot() * Mth.DEG_TO_RAD;
            double impactX = center.x + Math.cos(angle) * radius;
            double impactZ = center.z + Math.sin(angle) * radius;
            double spawnY = Math.min(level.getMaxBuildHeight() - 3.0D,
                    Math.max(center.y + 42.0D, caster.getY() + 34.0D) + (i % 3) * 2.5D);
            double approachAngle = angle + Math.PI;
            Vec3 spawnPosition = new Vec3(
                    impactX + Math.cos(approachAngle) * 14.0D,
                    spawnY,
                    impactZ + Math.sin(approachAngle) * 14.0D);
            Vec3 impactPosition = new Vec3(impactX, center.y, impactZ);
            Vec3 velocity = impactPosition.subtract(spawnPosition).normalize().scale(2.25D);

            StarfallProjectileEntity projectile = new StarfallProjectileEntity(level, caster);
            projectile.setPos(spawnPosition.x, spawnPosition.y, spawnPosition.z);
            projectile.setDeltaMovement(velocity);
            level.addFreshEntity(projectile);
        }
        return true;
    }

    private static void spawnCompressionVfx(ServerLevel level, LivingEntity caster) {
        Vec3 center = caster.position().add(0.0D, caster.getBbHeight() * 0.58D, 0.0D);
        for (int ring = 0; ring < 3; ring++) {
            double radius = 1.25D + ring * 0.55D;
            double height = center.y + (ring - 1) * 0.42D;
            for (int i = 0; i < 24; i++) {
                double angle = Math.PI * 2.0D * i / 24.0D + ring * 0.38D;
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;
                Vec3 inward = center.subtract(x, height, z).normalize().scale(0.16D);
                level.sendParticles(ring == 1 ? GOLD_DUST : WHITE_DUST,
                        x, height, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                if (i % 3 == 0) {
                    level.sendParticles(ParticleTypes.END_ROD, x, height, z, 1,
                            inward.x, inward.y, inward.z, 0.15D);
                }
            }
        }
        level.sendParticles(ParticleTypes.GLOW, center.x, center.y, center.z,
                55, 0.9D, 1.0D, 0.9D, 0.12D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z,
                42, 0.75D, 0.95D, 0.75D, 0.18D);
        level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z,
                2, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static void spawnTargetVfx(ServerLevel level, Vec3 center, float scale) {
        level.sendParticles(GOLD_DUST, center.x, center.y + 0.2D, center.z,
                Math.round(70.0F * scale), 7.5D, 0.25D, 7.5D, 0.0D);
        level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 0.35D, center.z,
                Math.round(38.0F * scale), 6.0D, 0.4D, 6.0D, 0.06D);
    }
}
