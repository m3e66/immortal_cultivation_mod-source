package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.effect.PhotonEffects;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DielangShield {
    private static final int MAX_STACK = 3;
    private static final int[] COSTS = {30, 60, 90};
    private static final float[] CAPACITY = {10.0F, 30.0F, 90.0F};
    private static final Map<UUID, ShieldState> SHIELDS = new ConcurrentHashMap<>();

    private DielangShield() {
    }

    public static boolean cast(ServerPlayer player) {
        ShieldState current = SHIELDS.get(player.getUUID());
        int nextStack = current == null ? 1 : Math.min(MAX_STACK, current.stack() + 1);
        if (current != null && current.stack() >= MAX_STACK) {
            nextStack = MAX_STACK;
        }

        var data = ModAttachments.getData(player);
        if (!ServerEvents.spendQiOrBlood(player, data, COSTS[nextStack - 1])) {
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
            return false;
        }

        ShieldState updated = new ShieldState(nextStack, CAPACITY[nextStack - 1]);
        SHIELDS.put(player.getUUID(), updated);
        PhotonEffects.waterShield(player, nextStack);
        ServerEvents.syncPlayerData(player);
        return true;
    }

    public static float absorb(ServerPlayer player, float damage) {
        ShieldState shield = SHIELDS.get(player.getUUID());
        if (shield == null || shield.remaining() <= 0.0F || damage <= 0.0F) {
            return damage;
        }

        float absorbed = Math.min(damage, shield.remaining());
        float remainingShield = shield.remaining() - absorbed;
        float remainingDamage = damage - absorbed;
        if (remainingShield <= 0.0F) {
            SHIELDS.remove(player.getUUID());
            PhotonEffects.removeWaterShield(player);
        } else {
            SHIELDS.put(player.getUUID(), new ShieldState(shield.stack(), remainingShield));
        }
        return remainingDamage;
    }

    public static void tick(ServerPlayer player) {
        ShieldState shield = SHIELDS.get(player.getUUID());
        if (shield != null && shield.remaining() > 0.0F && player.tickCount % 40 == 0) {
            PhotonEffects.waterShield(player, shield.stack());
        }
    }

    private record ShieldState(int stack, float remaining) {
    }
}
