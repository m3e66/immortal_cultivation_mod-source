package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.network.ModPayloads;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerShieldManager {
    private static final Map<UUID, Map<String, ShieldValue>> SHIELDS = new ConcurrentHashMap<>();

    private PlayerShieldManager() {
    }

    public static void set(ServerPlayer player, String source, float amount, float max) {
        UUID playerId = player.getUUID();
        if (amount <= 0.0F || max <= 0.0F) {
            clear(player, source);
            return;
        }
        SHIELDS.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>())
                .put(source, new ShieldValue(amount, max));
        sync(player);
    }

    public static void clear(ServerPlayer player, String source) {
        Map<String, ShieldValue> playerShields = SHIELDS.get(player.getUUID());
        if (playerShields != null) {
            playerShields.remove(source);
            if (playerShields.isEmpty()) {
                SHIELDS.remove(player.getUUID());
            }
        }
        sync(player);
    }

    public static void clearAll(ServerPlayer player) {
        SHIELDS.remove(player.getUUID());
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        Map<String, ShieldValue> playerShields = SHIELDS.get(player.getUUID());
        float amount = 0.0F;
        float max = 0.0F;
        if (playerShields != null) {
            for (ShieldValue shield : playerShields.values()) {
                amount += Math.max(0.0F, shield.amount());
                max += Math.max(0.0F, shield.max());
            }
        }
        PacketDistributor.sendToPlayer(player, new ModPayloads.ClientboundShieldDataPayload(amount, max));
    }

    private record ShieldValue(float amount, float max) {
    }
}
