package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.effect.PhotonEffects;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HantiBingqin {
    private static final Map<UUID, Boolean> FROST_FLIGHT_ACTIVE = new ConcurrentHashMap<>();

    private HantiBingqin() {
    }

    public static void castClaw(ServerPlayer player) {
        castClaw(player, 1.0F);
    }

    public static void castClaw(ServerPlayer player, float chargeScale) {
        var data = ModAttachments.getData(player);
        if (!CultivationMethods.isHantiBingqin(data.activeCultivationMethod())) {
            return;
        }
        float scale = Math.max(1.0F, Math.min(2.0F, chargeScale));
        if (!spendChargedPercentQi(player, data, 3, scale)) {
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
            return;
        }

        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        AABB box = player.getBoundingBox().expandTowards(look.scale(5.0D * scale)).inflate(1.0D * scale);
        LivingEntity target = findClawTarget(player, start, look, box, 5.5D * scale);

        Vec3 dash = look.scale(1.35D * scale);
        player.setDeltaMovement(player.getDeltaMovement().add(dash.x, 0.08D, dash.z));
        player.hurtMarked = true;

        if (player.level() instanceof ServerLevel level) {
            Vec3 hand = player.position().add(look.scale(0.8D)).add(0.0D, 1.1D, 0.0D);
            level.sendParticles(ParticleTypes.SNOWFLAKE, hand.x, hand.y, hand.z, 34, 0.25D, 0.25D, 0.25D, 0.06D);
            level.sendParticles(ParticleTypes.CRIT, hand.x, hand.y, hand.z, 12, 0.18D, 0.18D, 0.18D, 0.02D);
        }

        if (target != null) {
            applyRealmScaledFreeze(player, target, ModEffects.FROZEN_QI, true);
            target.hurt(player.damageSources().magic(), 12.0F);
        }
        ServerEvents.syncPlayerData(player);
    }

    public static void toggleFrostFlight(ServerPlayer player) {
        UUID id = player.getUUID();
        if (FROST_FLIGHT_ACTIVE.remove(id) != null) {
            stopFrostFlight(player);
            return;
        }

        var data = ModAttachments.getData(player);
        if (!CultivationMethods.isHantiBingqin(data.activeCultivationMethod())) {
            return;
        }
        if (!spendPercentQi(player, data, 5)) {
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
            return;
        }

        FROST_FLIGHT_ACTIVE.put(id, true);
        player.startFallFlying();
        player.addEffect(new MobEffectInstance(ModEffects.FROST_FLIGHT, 40, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false, true));
        if (player.level() instanceof ServerLevel) {
            PhotonEffects.frostFlightStart(player);
            PhotonEffects.frostFlightTrail(player);
        }
        ServerEvents.syncPlayerData(player);
    }

    public static void tick(ServerPlayer player) {
        if (!FROST_FLIGHT_ACTIVE.containsKey(player.getUUID())) {
            return;
        }

        player.addEffect(new MobEffectInstance(ModEffects.FROST_FLIGHT, 40, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false, true));
        if (!player.onGround()) {
            player.startFallFlying();
        }
        player.fallDistance = 0.0F;
        player.resetFallDistance();

        if (player.level() instanceof ServerLevel level && player.tickCount % 3 == 0) {
            Vec3 pos = player.position().add(0.0D, 0.2D, 0.0D);
            level.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y, pos.z, 12, 0.45D, 0.12D, 0.45D, 0.02D);
            level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 4, 0.35D, 0.08D, 0.35D, 0.01D);
        }
        if (player.tickCount % 5 == 0) {
            PhotonEffects.frostFlightTrail(player);
        }

        if (player.tickCount % 20 != 0) {
            return;
        }

        var data = ModAttachments.getData(player);
        if (!spendPercentQi(player, data, 5)) {
            stopFrostFlight(player);
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
            return;
        }
        ServerEvents.syncPlayerData(player);
    }

    public static void clear(ServerPlayer player) {
        FROST_FLIGHT_ACTIVE.remove(player.getUUID());
        player.removeEffect(ModEffects.FROST_FLIGHT);
        PhotonEffects.frostFlightStop(player);
    }

    private static void stopFrostFlight(ServerPlayer player) {
        clear(player);
        ServerEvents.syncPlayerData(player);
    }

    public static void applyCryImpact(ServerPlayer caster, Vec3 pos) {
        applyCryImpact(caster, pos, 1.0F);
    }

    public static void applyCryImpact(ServerPlayer caster, Vec3 pos, float chargeScale) {
        if (!(caster.level() instanceof ServerLevel level)) {
            return;
        }
        float scale = Math.max(1.0F, Math.min(2.0F, chargeScale));

        PhotonEffects.iceImpact(level, pos.x, pos.y + 1.0D, pos.z);
        level.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y, pos.z, 80, 2.2D * scale, 0.7D, 2.2D * scale, 0.08D);
        level.sendParticles(ParticleTypes.POOF, pos.x, pos.y, pos.z, 25, 1.2D * scale, 0.35D, 1.2D * scale, 0.04D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(5.0D * scale),
                entity -> entity.isAlive() && !entity.getUUID().equals(caster.getUUID()))) {
            applyRealmScaledFreeze(caster, target, ModEffects.FROZEN, true);
            applyRealmScaledFreeze(caster, target, ModEffects.DAZE, false);
        }
    }

    public static void applyRealmScaledFreeze(ServerPlayer caster, LivingEntity target,
                                              net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
                                              boolean addSlowness) {
        int duration = freezeDuration(ModAttachments.getData(caster).cultivationLevel(), targetLevel(target));
        if (duration <= 0) {
            return;
        }
        target.addEffect(new MobEffectInstance(effect, duration, 0, false, true, true));
        if (addSlowness) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, duration >= 200 ? 9 : 4, false, true, true));
        }
    }

    private static LivingEntity findClawTarget(ServerPlayer player, Vec3 start, Vec3 look, AABB box, double maxForwardDistance) {
        return player.level().getEntitiesOfClass(LivingEntity.class, box, entity ->
                        entity.isAlive() && !entity.getUUID().equals(player.getUUID()))
                .stream()
                .filter(entity -> isInClawCone(entity, start, look, maxForwardDistance))
                .min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)))
                .orElseGet(() -> player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(2.0D),
                                entity -> entity.isAlive() && !entity.getUUID().equals(player.getUUID()))
                        .stream()
                        .filter(entity -> entity.position().subtract(player.position()).dot(look) > -0.35D)
                        .min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)))
                        .orElse(null));
    }

    private static boolean isInClawCone(LivingEntity target, Vec3 start, Vec3 look, double maxForwardDistance) {
        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
        Vec3 toTarget = center.subtract(start);
        double forwardDistance = toTarget.dot(look);
        if (forwardDistance < 0.0D || forwardDistance > maxForwardDistance) {
            return false;
        }
        Vec3 closest = start.add(look.scale(forwardDistance));
        double allowedRadius = Math.max(1.15D, target.getBbWidth() * 0.5D + 0.9D);
        return center.distanceToSqr(closest) <= allowedRadius * allowedRadius;
    }

    private static int freezeDuration(String casterLevel, String targetLevel) {
        int casterRealm = CultivationLevels.getRealmIndex(casterLevel);
        int targetRealm = CultivationLevels.getRealmIndex(targetLevel);
        if (targetRealm > casterRealm) {
            return 0;
        }

        int casterStage = CultivationLevels.getStageIndex(casterLevel);
        int targetStage = CultivationLevels.getStageIndex(targetLevel);
        if (targetRealm == casterRealm) {
            int smallLevelDiff = casterStage - targetStage;
            if (smallLevelDiff <= 0) {
                return 0;
            }
            if (smallLevelDiff == 1) {
                return 20 * 3;
            }
            return 20 * 10;
        }

        int realmDiff = casterRealm - targetRealm;
        if (realmDiff == 1) {
            return 20 * 5;
        }
        return 20 * 10;
    }

    private static String targetLevel(LivingEntity target) {
        if (target instanceof Player player) {
            return ModAttachments.getData(player).cultivationLevel();
        }
        return CultivationLevels.REALM_MORTAL;
    }

    private static boolean spendPercentQi(ServerPlayer player, ModAttachments.CultivationData data, int percent) {
        int maxQi = Math.max(1, CultivationLevels.getLevelDef(data.cultivationLevel()).maxQi() + data.maxQiBonus());
        int cost = Math.max(1, maxQi * percent / 100);
        return ServerEvents.spendQiOrBlood(player, data, cost);
    }

    private static boolean spendChargedPercentQi(ServerPlayer player, ModAttachments.CultivationData data, int percent, float chargeScale) {
        int maxQi = Math.max(1, CultivationLevels.getLevelDef(data.cultivationLevel()).maxQi() + data.maxQiBonus());
        int baseCost = Math.max(1, maxQi * percent / 100);
        int cost = Math.max(1, Math.round(baseCost * (1.0F + 4.0F * (chargeScale - 1.0F))));
        return ServerEvents.spendQiOrBlood(player, data, cost);
    }

}
