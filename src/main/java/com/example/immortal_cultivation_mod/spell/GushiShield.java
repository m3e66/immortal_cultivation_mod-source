package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.entity.DingshenProjectileEntity;
import com.example.immortal_cultivation_mod.entity.DuanliuKongdunProjectileEntity;
import com.example.immortal_cultivation_mod.entity.FireballProjectileEntity;
import com.example.immortal_cultivation_mod.entity.IgniteFlareProjectileEntity;
import com.example.immortal_cultivation_mod.entity.LingzhiBulletProjectileEntity;
import com.example.immortal_cultivation_mod.entity.SlidingWaterProjectileEntity;
import com.example.immortal_cultivation_mod.entity.SmokeProjectileEntity;
import com.example.immortal_cultivation_mod.entity.WindBladeProjectileEntity;
import com.example.immortal_cultivation_mod.entity.YihenCiProjectileEntity;
import com.example.immortal_cultivation_mod.entity.ZhenshanPalmEntity;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GushiShield {
    private static final String SHIELD_SOURCE = "gushi_shield";
    private static final int COST = 120;
    private static final float CAPACITY = 240.0F;
    private static final Map<UUID, Float> SHIELDS = new ConcurrentHashMap<>();

    private GushiShield() {
    }

    public static boolean cast(ServerPlayer player) {
        var data = ModAttachments.getData(player);
        if (!ServerEvents.spendQiOrBlood(player, data, COST)) {
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
            return false;
        }
        SHIELDS.put(player.getUUID(), CAPACITY);
        PlayerShieldManager.set(player, SHIELD_SOURCE, CAPACITY, CAPACITY);
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), player.getY() + 1.0D, player.getZ(),
                    45, 0.65D, 0.8D, 0.65D, 0.04D);
            level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 1.2D, player.getZ(),
                    35, 0.55D, 0.7D, 0.55D, 0.02D);
        }
        ServerEvents.syncPlayerData(player);
        return true;
    }

    public static float absorb(ServerPlayer player, DamageSource source, float damage) {
        Float shield = SHIELDS.get(player.getUUID());
        if (shield == null || shield <= 0.0F || damage <= 0.0F || !canBlock(source)) {
            return damage;
        }

        float absorbed = Math.min(damage, shield);
        float remainingShield = shield - absorbed;
        float remainingDamage = damage - absorbed;
        if (remainingShield <= 0.0F) {
            clear(player);
        } else {
            SHIELDS.put(player.getUUID(), remainingShield);
            PlayerShieldManager.set(player, SHIELD_SOURCE, remainingShield, CAPACITY);
        }
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 1.0D, player.getZ(),
                    18, 0.5D, 0.6D, 0.5D, 0.03D);
        }
        return remainingDamage;
    }

    public static void clear(ServerPlayer player) {
        SHIELDS.remove(player.getUUID());
        PlayerShieldManager.clear(player, SHIELD_SOURCE);
    }

    private static boolean canBlock(DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (direct instanceof FireballProjectileEntity
                || direct instanceof IgniteFlareProjectileEntity
                || direct instanceof LingzhiBulletProjectileEntity
                || direct instanceof WindBladeProjectileEntity
                || direct instanceof SmokeProjectileEntity
                || direct instanceof SlidingWaterProjectileEntity
                || direct instanceof DingshenProjectileEntity
                || direct instanceof DuanliuKongdunProjectileEntity
                || direct instanceof YihenCiProjectileEntity
                || direct instanceof ZhenshanPalmEntity) {
            return true;
        }
        return source.is(DamageTypes.PLAYER_ATTACK)
                || source.is(DamageTypes.MOB_ATTACK)
                || source.is(DamageTypes.ARROW)
                || source.is(DamageTypes.TRIDENT)
                || source.is(DamageTypes.MAGIC)
                || source.is(DamageTypes.INDIRECT_MAGIC)
                || source.is(DamageTypes.FIREBALL)
                || source.is(DamageTypes.ON_FIRE)
                || source.is(DamageTypes.IN_FIRE)
                || source.is(DamageTypeTags.IS_PROJECTILE)
                || source.is(DamageTypeTags.IS_EXPLOSION);
    }
}
