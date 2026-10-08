package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;

import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import com.example.immortal_cultivation_mod.entity.SpellImpactParticles;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Shared 潮湿 and environmental-water logic for 寒澜凝渊经. */
public final class HanlanNingyuanJing {
    private HanlanNingyuanJing() {
    }

    public static boolean hasMethod(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            return CultivationMethods.isHanlanNingyuanJing(
                    ModAttachments.getData(player).activeCultivationMethod());
        }
        if (entity instanceof PathfinderMob mob
                && entity instanceof CultivationNpc npc) {
            return CultivationMethods.HANLAN_NINGYUAN_JING.equals(
                    NpcProfile.methodId(mob, npc.npcArchetype()));
        }
        return false;
    }

    public static void applyDamp(LivingEntity target, int duration, LivingEntity source) {
        if (target == null || !target.isAlive()) return;
        boolean newlyApplied = !target.hasEffect(ModEffects.DAMP);
        target.clearFire();
        target.setRemainingFireTicks(0);
        target.addEffect(new MobEffectInstance(
                ModEffects.DAMP, Math.max(20, duration), 0,
                false, true, false), source);
        if (target.level() instanceof ServerLevel level) {
            level.sendParticles(ModParticles.METHOD_QI_WATER.get(),
                    target.getX(), target.getY() + target.getBbHeight() * 0.48D,
                    target.getZ(), 8, target.getBbWidth() * 0.40D,
                    target.getBbHeight() * 0.28D, target.getBbWidth() * 0.40D, 0.025D);
            if (newlyApplied) {
                SpellImpactParticles.shockRing(level,
                        target.position().add(0.0D, 0.06D, 0.0D),
                        ModParticles.METHOD_QI_WATER.get(), 0.42D, 24, 0.055D);
            }
        }
    }

    public static void tickDampVfx(LivingEntity target) {
        if (!isDamp(target) || target.tickCount % 6 != 0
                || !(target.level() instanceof ServerLevel level)) return;
        double phase = target.tickCount * 0.19D + target.getId() * 0.73D;
        double radius = Math.max(0.24D, target.getBbWidth() * 0.58D);
        for (int drop = 0; drop < 3; drop++) {
            double angle = phase + drop * Math.PI * 2.0D / 3.0D;
            double y = target.getY() + 0.18D
                    + target.getBbHeight() * (0.18D + drop * 0.25D);
            level.sendParticles(ModParticles.METHOD_QI_WATER.get(),
                    target.getX() + Math.cos(angle) * radius, y,
                    target.getZ() + Math.sin(angle) * radius,
                    1, 0.0D, -0.008D, 0.0D, 0.0D);
        }
        if (target.onGround() && target.tickCount % 12 == 0) {
            level.sendParticles(ParticleTypes.SPLASH,
                    target.getX(), target.getY() + 0.03D, target.getZ(),
                    4, target.getBbWidth() * 0.35D, 0.025D,
                    target.getBbWidth() * 0.35D, 0.025D);
        }
    }

    public static void applyDampFromWater(
            LivingEntity caster, LivingEntity target, int duration) {
        if (hasMethod(caster)) {
            applyDamp(target, duration, caster);
        }
    }

    public static boolean isDamp(LivingEntity target) {
        return target != null && target.hasEffect(ModEffects.DAMP);
    }

    public static boolean isStandingInWater(LivingEntity target) {
        return target != null && (target.isInWaterOrBubble()
                || target.level().getFluidState(target.blockPosition()).is(FluidTags.WATER)
                || target.level().getFluidState(target.blockPosition().below()).is(FluidTags.WATER));
    }

    public static float environmentalStrength(ServerLevel level, BlockPos center) {
        float score = level.isRainingAt(center.above()) ? 0.42F : 0.0F;
        int moistureBlocks = 0;
        for (int x = -6; x <= 6; x += 2) {
            for (int z = -6; z <= 6; z += 2) {
                for (int y = -2; y <= 2; y++) {
                    BlockState state = level.getBlockState(center.offset(x, y, z));
                    if (state.getFluidState().is(FluidTags.WATER)
                            || state.is(Blocks.ICE)
                            || state.is(Blocks.PACKED_ICE)
                            || state.is(Blocks.BLUE_ICE)
                            || state.is(Blocks.FROSTED_ICE)
                            || state.is(Blocks.SNOW)
                            || state.is(Blocks.SNOW_BLOCK)) {
                        moistureBlocks++;
                    }
                }
            }
        }
        score += Math.min(0.58F, moistureBlocks / 35.0F);
        int dampTargets = level.getEntitiesOfClass(
                LivingEntity.class, new AABB(center).inflate(8.0D),
                living -> living.hasEffect(ModEffects.DAMP)).size();
        score += Math.min(0.25F, dampTargets * 0.08F);
        return Mth.clamp(score, 0.0F, 1.0F);
    }

    public static float qiCostMultiplier(ServerPlayer player) {
        float strength = environmentalStrength(player.serverLevel(), player.blockPosition());
        if (strength >= 0.80F) return 0.70F;
        if (strength >= 0.40F) return 0.80F;
        if (strength >= 0.15F) return 0.90F;
        return 1.0F;
    }

    public static int freezeWaterAround(
            ServerLevel level, BlockPos center, int radius) {
        return freezeWaterAround(level, null, center, radius);
    }

    public static int freezeWaterAround(
            ServerLevel level, LivingEntity caster, BlockPos center, int radius) {
        if (!BlockBreakingSettings.enabled(caster,
                BlockBreakingSettings.Category.TECHNIQUES)) return 0;
        int frozen = 0;
        int radiusSqr = radius * radius;
        for (BlockPos cursor : BlockPos.betweenClosed(
                center.offset(-radius, -2, -radius),
                center.offset(radius, 2, radius))) {
            int dx = cursor.getX() - center.getX();
            int dz = cursor.getZ() - center.getZ();
            if (dx * dx + dz * dz > radiusSqr
                    || !level.getFluidState(cursor).is(FluidTags.WATER)) {
                continue;
            }
            if (caster == null) {
                // Preserve the ownerless utility overload used by world simulation and tests.
                level.setBlockAndUpdate(cursor, Blocks.FROSTED_ICE.defaultBlockState());
                frozen++;
            } else if (TerrainTransforms.replace(level, cursor, Blocks.FROSTED_ICE.defaultBlockState(), caster,
                    BlockBreakingSettings.Category.TECHNIQUES)) frozen++;
        }
        if (frozen > 0) {
            level.sendParticles(ParticleTypes.SNOWFLAKE,
                    center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D,
                    Math.min(80, frozen * 2), radius * 0.55D, 0.8D,
                    radius * 0.55D, 0.06D);
        }
        return frozen;
    }
}
