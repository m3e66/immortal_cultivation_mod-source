package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MichenZhang {
    private static final int COST = 20;
    private static final double RADIUS = 24.0D;
    private static final int DURATION_TICKS = 20 * 15;
    private static final int PARTICLE_TICKS = 20 * 10;
    private static final Map<UUID, DustCloud> CLOUDS = new ConcurrentHashMap<>();

    private MichenZhang() {
    }

    public static boolean cast(ServerPlayer player) {
        return cast(player, 1.0F);
    }

    public static boolean cast(ServerPlayer player, float chargeScale) {
        var data = ModAttachments.getData(player);
        float scale = Math.max(1.0F, Math.min(2.0F, chargeScale));
        int cost = Math.max(1, Math.round(COST * (1.0F + 4.0F * (scale - 1.0F))));
        if (!ServerEvents.spendQiOrBlood(player, data, cost)) {
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
            return false;
        }
        if (player.level() instanceof ServerLevel level) {
            Vec3 center = player.position().add(0.0D, 0.6D, 0.0D);
            CLOUDS.put(player.getUUID(), new DustCloud(level.dimension(), center, PARTICLE_TICKS, scale));
            emitDust(level, center, scale);
            affectTargets(player, level, center, DURATION_TICKS, scale);
        }
        ServerEvents.syncPlayerData(player);
        return true;
    }

    public static void tick(ServerPlayer player) {
        DustCloud cloud = CLOUDS.get(player.getUUID());
        if (cloud == null) {
            return;
        }
        if (cloud.ticksLeft() <= 0 || !cloud.dimension().equals(player.level().dimension())) {
            CLOUDS.remove(player.getUUID());
            return;
        }
        if (player.level() instanceof ServerLevel level && player.tickCount % 5 == 0) {
            emitDust(level, cloud.center(), cloud.scale());
            affectTargets(player, level, cloud.center(), 60, cloud.scale());
        }
        CLOUDS.put(player.getUUID(), new DustCloud(cloud.dimension(), cloud.center(), cloud.ticksLeft() - 1, cloud.scale()));
    }

    private static void emitDust(ServerLevel level, Vec3 center, float scale) {
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                center.x, center.y, center.z,
                260, 9.0D * scale, 1.6D, 9.0D * scale, 0.025D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                center.x, center.y + 0.35D, center.z,
                220, 8.0D * scale, 1.4D, 8.0D * scale, 0.04D);
        level.sendParticles(ParticleTypes.SMOKE,
                center.x, center.y + 0.2D, center.z,
                180, 9.5D * scale, 1.1D, 9.5D * scale, 0.035D);
        level.sendParticles(ParticleTypes.POOF,
                center.x, center.y - 0.1D, center.z,
                95, 7.0D * scale, 0.75D, 7.0D * scale, 0.035D);
    }

    private static void affectTargets(ServerPlayer player, ServerLevel level, Vec3 center, int durationTicks, float scale) {
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(RADIUS * scale),
                entity -> entity.isAlive() && !entity.getUUID().equals(player.getUUID()))) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, durationTicks, 0, false, false, true));
            if (target instanceof Mob mob && mob.getTarget() != null) {
                mob.setTarget(null);
                mob.getNavigation().stop();
            }
        }
    }

    private record DustCloud(ResourceKey<Level> dimension, Vec3 center, int ticksLeft, float scale) {
    }
}
