package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SifangJie {
    private static final int COST = 220;
    private static final int DURATION_TICKS = 20 * 20;
    private static final double RADIUS = 5.5D;
    private static final Map<UUID, Integer> BARRIERS = new ConcurrentHashMap<>();

    private SifangJie() {
    }

    public static boolean cast(ServerPlayer player) {
        var data = ModAttachments.getData(player);
        if (!ServerEvents.spendQiOrBlood(player, data, COST)) {
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
            return false;
        }
        BARRIERS.put(player.getUUID(), DURATION_TICKS);
        if (player.level() instanceof ServerLevel level) {
            emitBarrier(level, player);
            cleanseProtected(level, player);
        }
        ServerEvents.syncPlayerData(player);
        return true;
    }

    public static void tick(ServerPlayer player) {
        Integer ticks = BARRIERS.get(player.getUUID());
        if (ticks == null) {
            return;
        }
        if (ticks <= 0) {
            BARRIERS.remove(player.getUUID());
            return;
        }
        if (player.level() instanceof ServerLevel level && player.tickCount % 10 == 0) {
            emitBarrier(level, player);
            cleanseProtected(level, player);
        }
        BARRIERS.put(player.getUUID(), ticks - 1);
    }

    public static void clear(ServerPlayer player) {
        BARRIERS.remove(player.getUUID());
    }

    private static void emitBarrier(ServerLevel level, ServerPlayer player) {
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.1D, player.getZ(),
                42, RADIUS * 0.45D, 0.8D, RADIUS * 0.45D, 0.015D);
        level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0D, player.getZ(),
                36, RADIUS * 0.4D, 0.7D, RADIUS * 0.4D, 0.02D);
    }

    private static void cleanseProtected(ServerLevel level, ServerPlayer player) {
        for (Player target : level.getEntitiesOfClass(Player.class,
                new AABB(player.position(), player.position()).inflate(RADIUS),
                entity -> entity.isAlive())) {
            target.removeEffect(MobEffects.POISON);
            target.removeEffect(MobEffects.BLINDNESS);
            target.removeEffect(MobEffects.DARKNESS);
            target.removeEffect(MobEffects.CONFUSION);
        }
    }
}
