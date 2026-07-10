package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.entity.AbsorbCultivationProjectileEntity;
import com.example.immortal_cultivation_mod.entity.DingshenProjectileEntity;
import com.example.immortal_cultivation_mod.entity.DuanliuKongdunProjectileEntity;
import com.example.immortal_cultivation_mod.entity.FireballProjectileEntity;
import com.example.immortal_cultivation_mod.entity.IgniteFlareProjectileEntity;
import com.example.immortal_cultivation_mod.entity.LightBeamProjectileEntity;
import com.example.immortal_cultivation_mod.entity.LightningProjectileEntity;
import com.example.immortal_cultivation_mod.entity.LingzhiBulletProjectileEntity;
import com.example.immortal_cultivation_mod.entity.SlidingWaterProjectileEntity;
import com.example.immortal_cultivation_mod.entity.SmokeProjectileEntity;
import com.example.immortal_cultivation_mod.entity.WindBladeProjectileEntity;
import com.example.immortal_cultivation_mod.entity.YihenCiProjectileEntity;
import com.example.immortal_cultivation_mod.entity.ZhenshanPalmEntity;
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

public final class HutiQi {
    private static final String SHIELD_SOURCE = "huti_qi";
    private static final int COST = 10;
    private static final float CAPACITY = 20.0F;
    private static final Map<UUID, Float> SHIELDS = new ConcurrentHashMap<>();

    private HutiQi() {
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
            level.sendParticles(ParticleTypes.CLOUD,
                    player.getX(), player.getY() + 1.0D, player.getZ(),
                    36, 0.55D, 0.75D, 0.55D, 0.035D);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    player.getX(), player.getY() + 1.0D, player.getZ(),
                    2, 0.35D, 0.25D, 0.35D, 0.0D);
        }
        ServerEvents.syncPlayerData(player);
        return true;
    }

    public static float absorb(ServerPlayer player, DamageSource source, float damage) {
        Float shield = SHIELDS.get(player.getUUID());
        if (shield == null || shield <= 0.0F || damage <= 0.0F || isCultivatorSpellDamage(source)) {
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
            level.sendParticles(ParticleTypes.POOF,
                    player.getX(), player.getY() + 1.0D, player.getZ(),
                    12, 0.45D, 0.55D, 0.45D, 0.02D);
        }
        return remainingDamage;
    }

    public static void clear(ServerPlayer player) {
        SHIELDS.remove(player.getUUID());
        PlayerShieldManager.clear(player, SHIELD_SOURCE);
    }

    private static boolean isCultivatorSpellDamage(DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (direct instanceof FireballProjectileEntity
                || direct instanceof IgniteFlareProjectileEntity
                || direct instanceof LightBeamProjectileEntity
                || direct instanceof LingzhiBulletProjectileEntity
                || direct instanceof WindBladeProjectileEntity
                || direct instanceof SmokeProjectileEntity
                || direct instanceof SlidingWaterProjectileEntity
                || direct instanceof AbsorbCultivationProjectileEntity
                || direct instanceof DingshenProjectileEntity
                || direct instanceof DuanliuKongdunProjectileEntity
                || direct instanceof YihenCiProjectileEntity
                || direct instanceof LightningProjectileEntity
                || direct instanceof ZhenshanPalmEntity) {
            return true;
        }
        return source.is(DamageTypes.MAGIC)
                || source.is(DamageTypes.INDIRECT_MAGIC)
                || source.is(DamageTypes.FIREBALL)
                || source.is(DamageTypes.ON_FIRE)
                || source.is(DamageTypes.IN_FIRE)
                || source.is(DamageTypes.LIGHTNING_BOLT)
                || source.is(DamageTypeTags.IS_EXPLOSION);
    }
}
