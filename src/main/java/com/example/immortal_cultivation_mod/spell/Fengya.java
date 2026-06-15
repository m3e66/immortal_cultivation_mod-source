package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Fengya {
    private record ConeShape(int depth, double outerRadius) {
    }

    private record ConeCandidate(BlockPos pos, double distanceForward, double distanceFromCenter) {
    }

    private Fengya() {
    }

    public static void toggle(ServerPlayer player) {
        var data = ModAttachments.getData(player);
        if (!CultivationMethods.isPokongJue(data.activeCultivationMethod())) {
            return;
        }
        if (player.hasEffect(ModEffects.FENGYA)) {
            player.removeEffect(ModEffects.FENGYA);
            ServerEvents.syncPlayerData(player);
            return;
        }
        player.addEffect(new MobEffectInstance(ModEffects.FENGYA, MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
        ServerEvents.syncPlayerData(player);
    }

    public static boolean release(ServerPlayer player, Vec3 target, boolean strongRequested) {
        if (!player.hasEffect(ModEffects.FENGYA) || !(player.level() instanceof ServerLevel level)) {
            return false;
        }

        var data = ModAttachments.getData(player);
        boolean strong = strongRequested && canUseStrongPunch(data);
        int cost = qiCost(data, strong);
        if (!ServerEvents.spendQiOrBlood(player, data, cost)) {
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
            return true;
        }
        ServerEvents.syncPlayerData(player);

        int limit = strong ? 1024 : blockLimit(player);
        float damage = damage(data, strong);
        Vec3 origin = player.getEyePosition();
        Vec3 forward = target.subtract(origin);
        if (forward.lengthSqr() < 0.01D) {
            forward = player.getLookAngle();
        }
        forward = forward.normalize();

        int destroyed = 0;
        ConeShape shape = coneShape(limit);
        Vec3 end = origin.add(forward.scale(shape.depth()));
        double searchRadius = shape.outerRadius() + 1.0D;
        BlockPos min = BlockPos.containing(
                Math.min(origin.x, end.x) - searchRadius,
                Math.min(origin.y, end.y) - searchRadius,
                Math.min(origin.z, end.z) - searchRadius);
        BlockPos max = BlockPos.containing(
                Math.max(origin.x, end.x) + searchRadius,
                Math.max(origin.y, end.y) + searchRadius,
                Math.max(origin.z, end.z) + searchRadius);

        List<ConeCandidate> candidates = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            Vec3 blockCenter = Vec3.atCenterOf(pos);
            Vec3 fromOrigin = blockCenter.subtract(origin);
            double distanceForward = fromOrigin.dot(forward);
            if (distanceForward < 0.65D || distanceForward > shape.depth()) {
                continue;
            }

            Vec3 centerLine = origin.add(forward.scale(distanceForward));
            double radiusHere = 0.35D + (shape.outerRadius() - 0.35D) * (distanceForward / shape.depth());
            double distanceFromCenter = blockCenter.distanceToSqr(centerLine);
            if (distanceFromCenter > radiusHere * radiusHere) {
                continue;
            }
            candidates.add(new ConeCandidate(pos.immutable(), distanceForward, distanceFromCenter));
        }

        candidates.sort(Comparator
                .comparingDouble(ConeCandidate::distanceForward)
                .thenComparingDouble(ConeCandidate::distanceFromCenter));

        for (ConeCandidate candidate : candidates) {
            if (destroyed >= limit) {
                break;
            }
            BlockPos pos = candidate.pos();
            var state = level.getBlockState(pos);
            if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F) {
                continue;
            }
            if (level.destroyBlock(pos, false, player)) {
                destroyed++;
                level.sendParticles(ParticleTypes.CLOUD,
                        pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                        5, 0.22D, 0.22D, 0.22D, 0.05D);
            }
        }

        for (int step = 1; step <= shape.depth(); step++) {
            Vec3 center = origin.add(forward.scale(step));
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z, 2, 0.25D, 0.25D, 0.25D, 0.0D);
        }
        damageEntities(level, player, origin, forward, shape, damage);

        Vec3 burst = origin.add(forward.scale(2.0D));
        level.sendParticles(ParticleTypes.CLOUD, burst.x, burst.y, burst.z, 35, 0.7D, 0.45D, 0.7D, 0.18D);
        if (destroyed <= 0) {
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".fengya_no_blocks"));
        }
        return true;
    }

    private static ConeShape coneShape(int limit) {
        return switch (limit) {
            case 16 -> new ConeShape(8, 2.7D);
            case 64 -> new ConeShape(12, 4.7D);
            case 1024 -> new ConeShape(56, 13.0D);
            default -> new ConeShape(18, 8.0D);
        };
    }

    private static void damageEntities(ServerLevel level, ServerPlayer player, Vec3 origin, Vec3 forward, ConeShape shape, float damage) {
        Vec3 end = origin.add(forward.scale(shape.depth()));
        double searchRadius = shape.outerRadius() + 1.0D;
        AABB box = new AABB(origin, end).inflate(searchRadius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, entity -> entity != player && entity.isAlive())) {
            Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            Vec3 fromOrigin = targetCenter.subtract(origin);
            double distanceForward = fromOrigin.dot(forward);
            if (distanceForward < 0.65D || distanceForward > shape.depth()) {
                continue;
            }

            Vec3 centerLine = origin.add(forward.scale(distanceForward));
            double radiusHere = 0.35D + (shape.outerRadius() - 0.35D) * (distanceForward / shape.depth());
            if (targetCenter.distanceToSqr(centerLine) > radiusHere * radiusHere) {
                continue;
            }

            target.hurt(player.damageSources().magic(), damage);
            level.sendParticles(ParticleTypes.CLOUD,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    12, 0.35D, 0.35D, 0.35D, 0.08D);
        }
    }

    private static int blockLimit(ServerPlayer player) {
        int stage = CultivationLevels.getStageIndex(ModAttachments.getData(player).cultivationLevel());
        int zhuji = CultivationLevels.getStageIndex(CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_EARLY);
        int jindan = CultivationLevels.getStageIndex(CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY);
        if (stage >= jindan) {
            return 256;
        }
        if (stage >= zhuji) {
            return 64;
        }
        return 16;
    }

    private static int qiCost(ModAttachments.CultivationData data, boolean strong) {
        int maxQi = Math.max(1, CultivationLevels.getLevelDef(data.cultivationLevel()).maxQi() + data.maxQiBonus());
        return Math.max(1, (int) Math.ceil(maxQi * (strong ? 0.3D : 0.1D)));
    }

    private static float damage(ModAttachments.CultivationData data, boolean strong) {
        if (strong) {
            return 640.0F;
        }
        int stage = CultivationLevels.getStageIndex(data.cultivationLevel());
        int zhuji = CultivationLevels.getStageIndex(CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_EARLY);
        int jindan = CultivationLevels.getStageIndex(CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY);
        if (stage >= jindan) {
            return 320.0F;
        }
        if (stage >= zhuji) {
            return 40.0F;
        }
        return 10.0F;
    }

    private static boolean canUseStrongPunch(ModAttachments.CultivationData data) {
        int stage = CultivationLevels.getStageIndex(data.cultivationLevel());
        int jindan = CultivationLevels.getStageIndex(CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY);
        return stage >= jindan;
    }
}
