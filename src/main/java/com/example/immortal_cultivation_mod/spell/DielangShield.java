package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.effect.NativeSpellEffects;
import com.example.immortal_cultivation_mod.entity.DefensePiercingProjectile;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DielangShield {
    private static final String SHIELD_SOURCE = "dielang_shield";
    private static final int MAX_STACK = 3;
    private static final int[] COSTS = {50, 100, 150};
    private static final float[] CAPACITY = {12.0F, 36.0F, 108.0F};
    private static final Map<UUID, ShieldState> SHIELDS = new ConcurrentHashMap<>();

    private DielangShield() {
    }

    public static boolean cast(ServerPlayer player) {
        var data = ModAttachments.getData(player);
        if (!ServerEvents.spendSpellQiOrBlood(player, data, nextCost(player))) {
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
            return false;
        }
        activateNext(player);
        ServerEvents.syncPlayerData(player);
        return true;
    }

    public static int nextCost(LivingEntity entity) {
        ShieldState current = SHIELDS.get(entity.getUUID());
        int stack = current == null ? 1 : Math.min(MAX_STACK, current.stack() + 1);
        return COSTS[stack - 1];
    }

    /** Activates the same shield stack/capacity used by players after the caller pays the cost. */
    public static boolean activateNext(LivingEntity entity) {
        ShieldState current = SHIELDS.get(entity.getUUID());
        int nextStack = current == null ? 1 : Math.min(MAX_STACK, current.stack() + 1);
        ShieldState updated = new ShieldState(nextStack, CAPACITY[nextStack - 1]);
        SHIELDS.put(entity.getUUID(), updated);
        if (entity.level() instanceof ServerLevel level) {
            NativeSpellEffects.waterShield(level, entity, nextStack);
        }
        sync(entity);
        return true;
    }

    public static float absorb(ServerPlayer player, float damage) {
        return absorb((LivingEntity) player, null, damage);
    }

    public static float absorb(LivingEntity entity, float damage) {
        return absorb(entity, null, damage);
    }

    public static float absorb(LivingEntity entity, DamageSource source, float damage) {
        ShieldState shield = SHIELDS.get(entity.getUUID());
        if (shield == null || shield.remaining() <= 0.0F || damage <= 0.0F) {
            return damage;
        }

        float multiplier = DefensePiercingProjectile.shieldMultiplier(source) * com.example.immortal_cultivation_mod.attachment.BodyTypeEffects.shieldMultiplier(source);
        float absorbed = Math.min(damage * multiplier, shield.remaining());
        float remainingShield = shield.remaining() - absorbed;
        float remainingDamage = Math.max(0.0F, damage - absorbed / multiplier);
        if (remainingShield <= 0.0F) {
            SHIELDS.remove(entity.getUUID());
            com.example.immortal_cultivation_mod.combat.CombatFeedbackEvents.shieldBreak(entity, source, absorbed);
            if (entity.level() instanceof ServerLevel level) {
                NativeSpellEffects.removeWaterShield(level, entity);
            }
        } else {
            SHIELDS.put(entity.getUUID(), new ShieldState(shield.stack(), remainingShield));
        }
        sync(entity);
        return remainingDamage;
    }

    public static void tick(ServerPlayer player) {
        tick((LivingEntity) player);
    }

    public static void tick(LivingEntity entity) {
        ShieldState shield = SHIELDS.get(entity.getUUID());
        if (shield != null && shield.remaining() > 0.0F && entity.tickCount % 10 == 0
                && entity.level() instanceof ServerLevel level) {
            NativeSpellEffects.waterShield(level, entity, shield.stack());
        }
    }

    public static boolean hasShield(LivingEntity entity) {
        ShieldState shield = SHIELDS.get(entity.getUUID());
        return shield != null && shield.remaining() > 0.0F;
    }

    public static float remaining(LivingEntity entity) {
        ShieldState shield = SHIELDS.get(entity.getUUID());
        return shield == null ? 0.0F : Math.max(0.0F, shield.remaining());
    }

    public static void clear(ServerPlayer player) {
        clear((LivingEntity) player);
    }

    public static void clear(LivingEntity entity) {
        SHIELDS.remove(entity.getUUID());
        if (entity.level() instanceof ServerLevel level) {
            NativeSpellEffects.removeWaterShield(level, entity);
        }
        if (entity instanceof ServerPlayer player) {
            PlayerShieldManager.clear(player, SHIELD_SOURCE);
        }
    }

    public static void sync(ServerPlayer player) {
        sync((LivingEntity) player);
    }

    private static void sync(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        ShieldState shield = SHIELDS.get(entity.getUUID());
        float amount = shield == null ? 0.0F : Math.max(0.0F, shield.remaining());
        float max = shield == null ? 0.0F : CAPACITY[Math.max(0, Math.min(CAPACITY.length - 1, shield.stack() - 1))];
        PlayerShieldManager.set(player, SHIELD_SOURCE, amount, max);
    }

    private record ShieldState(int stack, float remaining) {
    }
}
