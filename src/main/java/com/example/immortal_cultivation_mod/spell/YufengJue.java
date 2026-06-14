package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class YufengJue {
    private static final int QI_PER_SECOND = 30;
    private static final int MAX_BLOCK_DISTANCE = 20;
    private static final Map<UUID, Boolean> ACTIVE = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> GRANTED_MAYFLY = new ConcurrentHashMap<>();

    private YufengJue() {
    }

    public static void toggle(ServerPlayer player) {
        UUID id = player.getUUID();
        if (ACTIVE.remove(id) != null) {
            stop(player);
            return;
        }

        var data = ModAttachments.getData(player);
        if (!ServerEvents.spendQiOrBlood(player, data, QI_PER_SECOND)) {
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
            return;
        }

        ACTIVE.put(id, true);
        enableFlight(player);
        ServerEvents.syncPlayerData(player);
    }

    public static void tick(ServerPlayer player) {
        if (!ACTIVE.containsKey(player.getUUID())) {
            return;
        }

        if (!hasBlockWithinDistance(player, MAX_BLOCK_DISTANCE)) {
            stop(player);
            return;
        }

        enableFlight(player);
        addWindParticles(player);
        player.fallDistance = 0.0F;
        player.resetFallDistance();

        if (player.tickCount % 20 != 0) {
            return;
        }

        var data = ModAttachments.getData(player);
        if (!ServerEvents.spendQiOrBlood(player, data, QI_PER_SECOND)) {
            stop(player);
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
            return;
        }

        ServerEvents.syncPlayerData(player);
    }

    public static void clear(ServerPlayer player) {
        ACTIVE.remove(player.getUUID());
        disableFlight(player);
    }

    private static void stop(ServerPlayer player) {
        clear(player);
        ServerEvents.syncPlayerData(player);
    }

    private static void enableFlight(ServerPlayer player) {
        if (!player.getAbilities().mayfly) {
            GRANTED_MAYFLY.put(player.getUUID(), true);
            player.getAbilities().mayfly = true;
        }
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
    }

    private static void disableFlight(ServerPlayer player) {
        if (GRANTED_MAYFLY.remove(player.getUUID()) != null && !player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        } else if (!player.getAbilities().mayfly && player.getAbilities().flying) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    private static boolean hasBlockWithinDistance(ServerPlayer player, int maxDistance) {
        BlockPos base = player.blockPosition();
        for (int i = 0; i <= maxDistance; i++) {
            BlockPos pos = base.below(i);
            if (pos.getY() < player.level().getMinBuildHeight()) {
                return false;
            }
            BlockState state = player.level().getBlockState(pos);
            if (!state.isAir() && !state.getCollisionShape(player.level(), pos).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static void addWindParticles(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || player.tickCount % 3 != 0) {
            return;
        }
        Vec3 pos = player.position().add(0.0D, 0.15D, 0.0D);
        level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 8, 0.45D, 0.12D, 0.45D, 0.03D);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y + 0.2D, pos.z, 2, 0.35D, 0.10D, 0.35D, 0.0D);
    }
}
