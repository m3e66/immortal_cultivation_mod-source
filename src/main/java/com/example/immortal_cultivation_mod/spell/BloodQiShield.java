package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.entity.DefensePiercingProjectile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BloodQiShield {
    private static final String SHIELD_SOURCE = "blood_qi_pill";
    private static final Map<UUID, ShieldState> SHIELDS = new ConcurrentHashMap<>();

    private BloodQiShield() {
    }

    public static void grant(ServerPlayer player) {
        float added = Math.max(1.0F, player.getMaxHealth() * 0.05F);
        ShieldState existing = SHIELDS.get(player.getUUID());
        float remaining = added;
        float max = added;
        if (existing != null) {
            remaining += existing.remaining();
            max += existing.max();
        }
        SHIELDS.put(player.getUUID(), new ShieldState(remaining, max));
        PlayerShieldManager.set(player, SHIELD_SOURCE, remaining, max);
    }

    public static float absorb(ServerPlayer player, float damage) {
        return absorb(player, null, damage);
    }

    public static float absorb(ServerPlayer player, DamageSource source, float damage) {
        ShieldState state = SHIELDS.get(player.getUUID());
        if (state == null || damage <= 0.0F) {
            return damage;
        }
        float multiplier = DefensePiercingProjectile.shieldMultiplier(source);
        float absorbed = Math.min(damage * multiplier, state.remaining());
        float remaining = state.remaining() - absorbed;
        if (remaining <= 0.0F) {
            clear(player);
        } else {
            SHIELDS.put(player.getUUID(), new ShieldState(remaining, state.max()));
            PlayerShieldManager.set(player, SHIELD_SOURCE, remaining, state.max());
        }
        return Math.max(0.0F, damage - absorbed / multiplier);
    }

    public static void clear(ServerPlayer player) {
        SHIELDS.remove(player.getUUID());
        PlayerShieldManager.clear(player, SHIELD_SOURCE);
    }

    private record ShieldState(float remaining, float max) {
    }
}
