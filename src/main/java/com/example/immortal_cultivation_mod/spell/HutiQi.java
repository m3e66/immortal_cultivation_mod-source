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
import com.example.immortal_cultivation_mod.entity.JinmangChuanshaProjectileEntity;
import com.example.immortal_cultivation_mod.entity.ElementalTechniqueProjectileEntity;
import com.example.immortal_cultivation_mod.entity.LingzhiBulletProjectileEntity;
import com.example.immortal_cultivation_mod.entity.SlidingWaterProjectileEntity;
import com.example.immortal_cultivation_mod.entity.SmokeProjectileEntity;
import com.example.immortal_cultivation_mod.entity.WindBladeProjectileEntity;
import com.example.immortal_cultivation_mod.entity.YihenCiProjectileEntity;
import com.example.immortal_cultivation_mod.entity.XinliaoFireballEntity;
import com.example.immortal_cultivation_mod.entity.StarfallProjectileEntity;
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
    private static final int QI_PER_SECOND = 1;
    // This entry tracks the toggle independently of the current shield capacity.
    private static final Map<UUID, Long> NEXT_PAYMENT_DUE = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> SHIELDS = new ConcurrentHashMap<>();

    private HutiQi() {
    }

    public static boolean cast(ServerPlayer player) {
        if (isActive(player)) {
            clear(player);
            return true;
        }
        if (!formShield(player)) {
            player.sendSystemMessage(Component.translatable("message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
            return false;
        }
        return true;
    }

    private static boolean formShield(ServerPlayer player) {
        if (!ServerEvents.spendSpellQiOrBlood(player, ModAttachments.getData(player), COST)) {
            return false;
        }
        SHIELDS.put(player.getUUID(), CAPACITY);
        NEXT_PAYMENT_DUE.put(player.getUUID(), player.level().getGameTime() + 20);
        refreshEffect(player);
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

    public static boolean isActive(ServerPlayer player) {
        return NEXT_PAYMENT_DUE.containsKey(player.getUUID());
    }

    public static void tick(ServerPlayer player) {
        if (!isActive(player)) return;
        if (!player.isAlive() || player.isSpectator()) {
            clear(player);
            return;
        }
        long now = player.level().getGameTime();
        if (now >= NEXT_PAYMENT_DUE.getOrDefault(player.getUUID(), now)) {
            if (!SHIELDS.containsKey(player.getUUID())) {
                if (!formShield(player)) {
                    // Stay enabled while waiting for enough Qi. Retry without message spam.
                    NEXT_PAYMENT_DUE.put(player.getUUID(), now + 20);
                }
            } else if (ServerEvents.spendSustainedSpellQiOrBlood(player,
                    ModAttachments.getData(player), QI_PER_SECOND, ModSpells.HUTI_QI)) {
                NEXT_PAYMENT_DUE.put(player.getUUID(), now + 20);
                ServerEvents.syncPlayerData(player);
            } else {
                removeShield(player);
                NEXT_PAYMENT_DUE.put(player.getUUID(), now + 20);
            }
        }
        refreshEffect(player);
    }

    private static void refreshEffect(ServerPlayer player) {
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                com.example.immortal_cultivation_mod.effect.ModEffects.HUTI_QI, 40, 0, false, false, true));
    }

    public static float absorb(ServerPlayer player, DamageSource source, float damage) {
        Float shield = SHIELDS.get(player.getUUID());
        if (shield == null || shield <= 0.0F || damage <= 0.0F || isCultivatorSpellDamage(source)) {
            return damage;
        }

        float multiplier = com.example.immortal_cultivation_mod.attachment.BodyTypeEffects.shieldMultiplier(source);
        float absorbed = Math.min(damage * multiplier, shield);
        float remainingShield = shield - absorbed;
        float remainingDamage = damage - absorbed / multiplier;
        if (remainingShield <= 0.0F) {
            removeShield(player);
            // Finish this hit against the old shield; never absorb overflow with a free refill.
            NEXT_PAYMENT_DUE.put(player.getUUID(), player.level().getGameTime() + 1);
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
        NEXT_PAYMENT_DUE.remove(player.getUUID());
        player.removeEffect(com.example.immortal_cultivation_mod.effect.ModEffects.HUTI_QI);
        removeShield(player);
    }

    private static void removeShield(ServerPlayer player) {
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
                || direct instanceof XinliaoFireballEntity
                || direct instanceof StarfallProjectileEntity
                || direct instanceof LightningProjectileEntity
                || direct instanceof JinmangChuanshaProjectileEntity
                || direct instanceof ElementalTechniqueProjectileEntity
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
