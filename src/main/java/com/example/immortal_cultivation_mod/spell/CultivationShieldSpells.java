package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.combat.CombatStateManager;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import com.example.immortal_cultivation_mod.entity.CrimsonDragonScaleProjectileEntity;
import com.example.immortal_cultivation_mod.entity.FireballProjectileEntity;
import com.example.immortal_cultivation_mod.entity.FireballTalismanProjectileEntity;
import com.example.immortal_cultivation_mod.entity.FlyingNeedleEntity;
import com.example.immortal_cultivation_mod.entity.FlyingSwordEntity;
import com.example.immortal_cultivation_mod.entity.LightBeamProjectileEntity;
import com.example.immortal_cultivation_mod.entity.LightningProjectileEntity;
import com.example.immortal_cultivation_mod.entity.JinmangChuanshaProjectileEntity;
import com.example.immortal_cultivation_mod.entity.DefensePiercingProjectile;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import com.example.immortal_cultivation_mod.entity.TianyunFenchengMeteorEntity;
import com.example.immortal_cultivation_mod.entity.StarfallProjectileEntity;
import com.example.immortal_cultivation_mod.entity.ThrowingKnifeEntity;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.item.WeaponRestrictionSystem;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Server-authoritative implementation for the five cultivation shield techniques. */
public final class CultivationShieldSpells {
    private static final String EARTH_SOURCE = ModSpells.XUANYAN_HUTI;
    private static final String WIND_SOURCE = ModSpells.LIUFENG_XIEJIN;
    private static final String BLOOD_SOURCE = ModSpells.XUESHA_HUSHEN;
    private static final String MIRROR_SOURCE = ModSpells.JINGLING_FANZHAO;
    private static final ResourceLocation EARTH_SLOW = ResourceLocation.fromNamespaceAndPath(
            ImmortalCultivationMod.MODID, "xuanyan_huti_slow");

    private static final int EARTH_COST = 120;
    private static final int EARTH_DURATION = 20 * 25;
    private static final float EARTH_CAPACITY = 100.0F;
    private static final int WIND_ACTIVATION_COST = 180;
    private static final int WIND_SUSTAIN_COST = 15;
    private static final float WIND_CAPACITY = 120.0F;
    private static final int BLOOD_COST = 550;
    private static final int MIRROR_COST = 1_200;
    private static final int MIRROR_REFLECTION_COST = 200;
    private static final int MIRROR_DURATION = 20 * 20;
    private static final float MIRROR_CAPACITY = 1_000.0F;
    private static final String MIRROR_DAMAGE_SCALE = "ImmortalCultivationMirrorDamageScale";
    private static final String MIRROR_LAST_REFLECTOR = "ImmortalCultivationMirrorLastReflector";
    private static final String MIRROR_LAST_REFLECT_TICK = "ImmortalCultivationMirrorLastReflectTick";
    private static final String NPC_OWNED_AT_LAUNCH = "ImmortalCultivationNpcOwnedAtLaunch";
    private static final String MIRROR_FAILED_VFX_UNTIL =
            "ImmortalCultivationMirrorFailedVfxUntil";

    private static final DustParticleOptions EARTH_GOLD = new DustParticleOptions(
            new Vector3f(0.82F, 0.52F, 0.10F), 1.05F);
    private static final DustParticleOptions WIND_PALE = new DustParticleOptions(
            new Vector3f(0.48F, 0.95F, 0.68F), 0.78F);
    private static final DustParticleOptions BLOOD_DARK = new DustParticleOptions(
            new Vector3f(0.22F, 0.008F, 0.018F), 1.15F);
    private static final DustParticleOptions BLOOD_BRIGHT = new DustParticleOptions(
            new Vector3f(0.92F, 0.025F, 0.045F), 0.92F);
    private static final DustParticleOptions MIRROR_GOLD = new DustParticleOptions(
            new Vector3f(1.0F, 0.82F, 0.30F), 0.82F);
    private static final DustParticleOptions MIRROR_WHITE = new DustParticleOptions(
            new Vector3f(0.92F, 0.97F, 1.0F), 0.68F);
    private static final DustParticleOptions DOMAIN_BLUE = new DustParticleOptions(
            new Vector3f(0.45F, 0.82F, 1.0F), 0.92F);
    private static final DustParticleOptions DOMAIN_WHITE = new DustParticleOptions(
            new Vector3f(0.88F, 0.97F, 1.0F), 0.72F);

    private static final Map<UUID, ShieldState> EARTH = new ConcurrentHashMap<>();
    private static final Map<UUID, ShieldState> WIND = new ConcurrentHashMap<>();
    private static final Map<UUID, ShieldState> BLOOD = new ConcurrentHashMap<>();
    private static final Map<UUID, ShieldState> MIRROR = new ConcurrentHashMap<>();
    private static final Map<UUID, DomainState> DOMAINS = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingReflection> PENDING_REFLECTIONS =
            new ConcurrentHashMap<>();
    private static final Map<UUID, DomainAfterimage> DOMAIN_AFTERIMAGES =
            new ConcurrentHashMap<>();

    private CultivationShieldSpells() {
    }

    public static boolean castXuanyanHuti(ServerPlayer player) {
        if (!pay(player, EARTH_COST)) {
            return false;
        }
        long expires = player.serverLevel().getGameTime() + EARTH_DURATION;
        ShieldState state = new ShieldState(
                EARTH_CAPACITY, EARTH_CAPACITY, expires,
                player.serverLevel().getGameTime(), shieldCenter(player));
        EARTH.put(player.getUUID(), state);
        applyEarthSlow(player, true);
        PlayerShieldManager.set(player, EARTH_SOURCE, EARTH_CAPACITY, EARTH_CAPACITY);
        emitEarthCast(player.serverLevel(), player);
        ServerEvents.syncPlayerData(player);
        return true;
    }

    public static boolean castLiufengXiejin(ServerPlayer player) {
        if (WIND.containsKey(player.getUUID())) {
            stopWind(player, true, true);
            return true;
        }
        if (!pay(player, WIND_ACTIVATION_COST)) {
            return false;
        }
        WIND.put(player.getUUID(), new ShieldState(
                WIND_CAPACITY, WIND_CAPACITY, Long.MAX_VALUE,
                player.serverLevel().getGameTime(), shieldCenter(player)));
        PlayerShieldManager.set(player, WIND_SOURCE, WIND_CAPACITY, WIND_CAPACITY);
        emitWindCast(player.serverLevel(), player);
        ServerEvents.syncPlayerData(player);
        return true;
    }

    public static boolean castXueshaHushen(ServerPlayer player) {
        float hpRatio = player.getHealth() / Math.max(1.0F, player.getMaxHealth());
        float capacity = hpRatio < 0.20F ? 750.0F
                : hpRatio < 0.40F ? 650.0F
                : hpRatio <= 0.70F ? 575.0F : 500.0F;
        if (!pay(player, BLOOD_COST)) {
            return false;
        }
        player.setHealth(Math.max(0.01F, player.getHealth() * 0.95F));
        BLOOD.put(player.getUUID(), new ShieldState(
                capacity, capacity, Long.MAX_VALUE,
                player.serverLevel().getGameTime(), shieldCenter(player)));
        PlayerShieldManager.set(player, BLOOD_SOURCE, capacity, capacity);
        emitBloodCast(player.serverLevel(), player);
        ServerEvents.syncPlayerData(player);
        return true;
    }

    public static boolean castJinglingFanzhao(ServerPlayer player) {
        if (!pay(player, MIRROR_COST)) {
            return false;
        }
        long expires = player.serverLevel().getGameTime() + MIRROR_DURATION;
        MIRROR.put(player.getUUID(), new ShieldState(
                MIRROR_CAPACITY, MIRROR_CAPACITY, expires,
                player.serverLevel().getGameTime(), shieldCenter(player)));
        PlayerShieldManager.set(player, MIRROR_SOURCE, MIRROR_CAPACITY, MIRROR_CAPACITY);
        emitMirrorCast(player.serverLevel(), player);
        ServerEvents.syncPlayerData(player);
        return true;
    }

    public static boolean castYuangangHujie(ServerPlayer player) {
        if (DOMAINS.containsKey(player.getUUID())) {
            stopDomain(player, true, false);
            return true;
        }
        DomainStats stats = domainStats(player);
        if (!pay(player, stats.activationCost())) {
            return false;
        }
        DomainState state = new DomainState(
                stats.capacity(), stats.capacity(), stats.radius(), stats.sustainCost(),
                player.serverLevel().getGameTime());
        DOMAINS.put(player.getUUID(), state);
        syncDomainViewers(player, state);
        emitDomainCast(player.serverLevel(), player, state);
        ServerEvents.syncPlayerData(player);
        return true;
    }

    public static boolean handlesOwnCooldown(String spellId) {
        String normalized = ModSpells.normalizeId(spellId);
        return ModSpells.LIUFENG_XIEJIN.equals(normalized)
                || ModSpells.YUANGANG_HUJIE.equals(normalized);
    }

    public static boolean isSustainedShieldActive(ServerPlayer player, String spellId) {
        String normalized = ModSpells.normalizeId(spellId);
        return ModSpells.LIUFENG_XIEJIN.equals(normalized) && WIND.containsKey(player.getUUID())
                || ModSpells.YUANGANG_HUJIE.equals(normalized) && DOMAINS.containsKey(player.getUUID());
    }

    public static boolean blocksNaturalQiRegen(Player player) {
        return DOMAINS.containsKey(player.getUUID());
    }

    public static float reflectedDamage(DamageSource source, float amount) {
        Entity direct = source.getDirectEntity();
        if (direct == null || !direct.getPersistentData().contains(MIRROR_DAMAGE_SCALE)) {
            return amount;
        }
        return amount * Mth.clamp(direct.getPersistentData().getFloat(MIRROR_DAMAGE_SCALE), 0.0F, 1.0F);
    }

    /** The outer domain is consumed before personal shields. */
    public static float absorbDomain(LivingEntity target, DamageSource source, float damage) {
        if (damage <= 0.0F || !(target.level() instanceof ServerLevel level)) {
            return damage;
        }
        for (Map.Entry<UUID, DomainState> entry : DOMAINS.entrySet()) {
            ServerPlayer caster = serverPlayer(level, entry.getKey());
            DomainState state = entry.getValue();
            if (caster == null || !caster.isAlive() || !insideDomain(caster, target, state)
                    || !isProtectedAlly(caster, target) || isFriendlySource(caster, source)) {
                continue;
            }
            float multiplier = shieldBreakingMultiplier(source);
            float shieldPressure = damage * multiplier;
            float consumed = Math.min(state.remaining, shieldPressure);
            float blockedDamage = consumed / multiplier;
            state.remaining -= consumed;
            Vec3 impact = domainImpactPoint(caster, target, source, state.radius);
            state.recordHit(impact, level.getGameTime());
            syncDomainViewers(caster, state);
            emitDomainImpact(level, caster, impact, state);
            if (state.remaining <= 0.0F) {
                stopDomain(caster, true, true);
            }
            return Math.max(0.0F, damage - blockedDamage);
        }
        return damage;
    }

    public static float absorbPersonal(ServerPlayer player, DamageSource source, float damage) {
        float remainingDamage = damage;
        remainingDamage = absorbEarth(player, source, remainingDamage);
        remainingDamage = absorbWind(player, source, remainingDamage);
        remainingDamage = absorbBlood(player, source, remainingDamage);
        remainingDamage = absorbMirror(player, source, remainingDamage);
        return remainingDamage;
    }

    public static float knockbackMultiplier(ServerPlayer player) {
        if (EARTH.containsKey(player.getUUID())) {
            return 0.70F;
        }
        if (WIND.containsKey(player.getUUID())) {
            return 0.80F;
        }
        return 1.0F;
    }

    public static void tick(ServerPlayer player) {
        long now = player.serverLevel().getGameTime();
        tickDomainAfterimage(player, now);
        ShieldState earth = EARTH.get(player.getUUID());
        if (earth != null) {
            if (!player.isAlive() || now >= earth.expiresAt) {
                clearEarth(player, false);
            } else {
                applyEarthSlow(player, true);
                if (player.tickCount % 3 == 0) {
                    emitEarthShield(player.serverLevel(), player, earth, now);
                }
            }
        } else {
            applyEarthSlow(player, false);
        }

        ShieldState wind = WIND.get(player.getUUID());
        if (wind != null) {
            if (!player.isAlive()) {
                stopWind(player, false, false);
            } else if (player.tickCount % 20 == 0
                    && !ServerEvents.spendSustainedSpellQiOrBlood(
                    player, ModAttachments.getData(player), WIND_SUSTAIN_COST, ModSpells.LIUFENG_XIEJIN)) {
                stopWind(player, true, true);
            } else {
                if (player.tickCount % 2 == 0) {
                    emitWindShield(player.serverLevel(), player, wind, now);
                }
                if (player.tickCount % 20 == 0) {
                    ServerEvents.syncPlayerData(player);
                }
            }
        }

        ShieldState blood = BLOOD.get(player.getUUID());
        if (blood != null) {
            if (!player.isAlive()) {
                BLOOD.remove(player.getUUID());
                PlayerShieldManager.clear(player, BLOOD_SOURCE);
            } else if (player.tickCount % 3 == 0) {
                emitBloodShield(player.serverLevel(), player, blood, now);
            }
        }

        ShieldState mirror = MIRROR.get(player.getUUID());
        if (mirror != null) {
            if (!player.isAlive() || now >= mirror.expiresAt) {
                clearMirror(player, false);
            } else if (player.tickCount % 3 == 0) {
                emitMirrorShield(player.serverLevel(), player, mirror, now);
            }
        }

        DomainState domain = DOMAINS.get(player.getUUID());
        if (domain != null) {
            if (!player.isAlive()) {
                stopDomain(player, false, false);
            } else if (player.tickCount % 20 == 0
                    && !ServerEvents.spendSustainedSpellQiOrBlood(
                    player, ModAttachments.getData(player), domain.sustainCost, ModSpells.YUANGANG_HUJIE)) {
                stopDomain(player, true, true);
            } else {
                if (player.tickCount % 4 == 0) {
                    emitDomainShield(player.serverLevel(), player, domain, now);
                }
                if (player.tickCount % 10 == 0) {
                    syncDomainViewers(player, domain);
                }
                if (player.tickCount % 20 == 0) {
                    ServerEvents.syncPlayerData(player);
                }
            }
        }
    }

    /** Called before projectile movement so fast projectiles cannot tunnel through a shield. */
    public static void interceptProjectile(Projectile projectile) {
        if (!(projectile.level() instanceof ServerLevel level) || !projectile.isAlive()) {
            return;
        }
        // Personal shields still absorb the eventual spell damage, but a city-sized
        // meteor cannot be bent, suspended, or mirrored like an ordinary projectile.
        if (projectile instanceof TianyunFenchengMeteorEntity) {
            return;
        }
        Vec3 velocity = projectile.getDeltaMovement();
        long now = level.getGameTime();
        if ((now & 63L) == 0L) {
            PENDING_REFLECTIONS.entrySet().removeIf(
                    entry -> entry.getValue().releaseAt + 20L < now);
        }

        PendingReflection pending = PENDING_REFLECTIONS.get(projectile.getUUID());
        if (pending != null) {
            if (now < pending.releaseAt) {
                projectile.setDeltaMovement(Vec3.ZERO);
                projectile.hurtMarked = true;
                ServerPlayer reflector = serverPlayer(level, pending.reflectorId);
                if (reflector != null) {
                    emitMirrorSuspension(level, reflector, projectile.position());
                }
                return;
            }
            PENDING_REFLECTIONS.remove(projectile.getUUID());
            ServerPlayer reflector = serverPlayer(level, pending.reflectorId);
            if (reflector == null || !reflector.isAlive()) {
                projectile.setDeltaMovement(pending.incomingVelocity);
                return;
            }
            projectile.setDeltaMovement(pending.incomingVelocity);
            reflectProjectile(projectile, reflector, now, pending.incomingVelocity);
            emitMirrorReflectionSuccess(level, reflector, projectile.position(), pending.originalOwnerId);
            return;
        }

        if (velocity.lengthSqr() < 1.0E-6D) {
            return;
        }

        for (UUID casterId : MIRROR.keySet()) {
            ServerPlayer caster = serverPlayer(level, casterId);
            ShieldState state = MIRROR.get(casterId);
            if (caster == null || state == null || now >= state.expiresAt
                    || !isEnemyProjectile(caster, projectile)
                    || !isReflectableSpellProjectile(projectile)
                    || !willEnterSphere(projectile.position(), velocity, shieldCenter(caster), 1.75D)
                    || recentlyReflectedBy(projectile, caster, now)) {
                continue;
            }
            Vec3 impact = sphereImpact(caster, projectile.position(), 1.25D);
            if (!ServerEvents.spendSpellQiOrBlood(caster, ModAttachments.getData(caster), MIRROR_REFLECTION_COST)) {
                if (projectile.getPersistentData().getLong(MIRROR_FAILED_VFX_UNTIL) < now) {
                    projectile.getPersistentData().putLong(MIRROR_FAILED_VFX_UNTIL, now + 8L);
                    emitMirrorReflectionFailure(level, caster, impact);
                }
                continue;
            }
            state.recordHit(impact, now);
            Entity oldOwner = projectile.getOwner();
            PENDING_REFLECTIONS.put(projectile.getUUID(), new PendingReflection(
                    caster.getUUID(), now + 2L, velocity,
                    oldOwner == null ? null : oldOwner.getUUID()));
            projectile.setDeltaMovement(Vec3.ZERO);
            projectile.hurtMarked = true;
            ServerEvents.syncPlayerData(caster);
            emitMirrorReflectionStart(level, caster, impact);
            return;
        }

        for (UUID casterId : WIND.keySet()) {
            ServerPlayer caster = serverPlayer(level, casterId);
            if (caster == null || !isEnemyProjectile(caster, projectile)
                    || !willEnterSphere(projectile.position(), velocity, shieldCenter(caster), 1.9D)) {
                continue;
            }
            float chance = isPhysicalProjectile(projectile) ? 0.80F
                    : isStrongProjectile(projectile) ? 0.0F : 0.40F;
            String tag = "ImmortalCultivationWindDeflect_" + caster.getUUID();
            if (chance > 0.0F && projectile.getPersistentData().getLong(tag) < now
                    && level.random.nextFloat() < chance) {
                Vec3 oldVelocity = projectile.getDeltaMovement();
                Vec3 impact = sphereImpact(caster, projectile.position(), 1.25D);
                deflectProjectile(projectile, caster);
                projectile.getPersistentData().putLong(tag, now + 8L);
                emitWindDeflection(level, caster, impact, oldVelocity,
                        projectile.getDeltaMovement());
                return;
            }
        }

        for (Map.Entry<UUID, DomainState> entry : DOMAINS.entrySet()) {
            ServerPlayer caster = serverPlayer(level, entry.getKey());
            DomainState state = entry.getValue();
            if (caster == null || !isEnemyProjectile(caster, projectile)
                    || !willEnterSphere(projectile.position(), projectile.getDeltaMovement(),
                    domainCenter(caster), state.radius)) {
                continue;
            }
            String tag = "ImmortalCultivationYuangangSlow_" + caster.getUUID();
            if (projectile.getPersistentData().getLong(tag) < now) {
                projectile.setDeltaMovement(projectile.getDeltaMovement().scale(
                        isStrongProjectile(projectile) ? 0.75D : 0.50D));
                projectile.hurtMarked = true;
                projectile.getPersistentData().putLong(tag, now + 20L);
                emitDomainProjectileSlow(level, caster, state, projectile);
            }
        }
    }

    public static void clear(ServerPlayer player) {
        clearEarth(player, false);
        WIND.remove(player.getUUID());
        BLOOD.remove(player.getUUID());
        MIRROR.remove(player.getUUID());
        PlayerShieldManager.clear(player, WIND_SOURCE);
        PlayerShieldManager.clear(player, BLOOD_SOURCE);
        PlayerShieldManager.clear(player, MIRROR_SOURCE);
        stopDomain(player, false, false);
    }

    private static boolean pay(ServerPlayer player, int cost) {
        if (!ServerEvents.spendSpellQiOrBlood(player, ModAttachments.getData(player), cost)) {
            player.sendSystemMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".not_enough_qi"));
            return false;
        }
        return true;
    }

    private static float absorbEarth(ServerPlayer player, DamageSource source, float damage) {
        ShieldState state = EARTH.get(player.getUUID());
        if (state == null || damage <= 0.0F) {
            return damage;
        }
        float multiplier = shieldBreakingMultiplier(source);
        float pressure = damage * multiplier;
        float consumed = Math.min(state.remaining, pressure);
        float oldRemaining = state.remaining;
        state.remaining -= consumed;
        float blocked = consumed / multiplier;
        Vec3 impact = impactPoint(player, source, 1.15D);
        state.recordHit(impact, player.serverLevel().getGameTime());
        int oldPlates = plateCount(oldRemaining);
        int newPlates = plateCount(state.remaining);
        if (player.level() instanceof ServerLevel level) {
            if (newPlates < oldPlates && newPlates > 0) {
                emitEarthPlateBreak(level, player, impact,
                        oldPlates - newPlates, state.remaining / state.max);
            } else {
                emitEarthHit(level, player, impact, state.remaining / state.max);
            }
        }
        if (state.remaining <= 0.0F) {
            clearEarth(player, true);
        } else {
            PlayerShieldManager.set(player, EARTH_SOURCE, state.remaining, state.max);
        }
        return Math.max(0.0F, damage - blocked);
    }

    private static float absorbBlood(ServerPlayer player, DamageSource source, float damage) {
        ShieldState state = BLOOD.get(player.getUUID());
        if (state == null || damage <= 0.0F) {
            return damage;
        }
        float multiplier = directShieldMultiplier(source);
        float consumed = Math.min(damage * multiplier, state.remaining);
        state.remaining -= consumed;
        Vec3 impact = impactPoint(player, source, 1.15D);
        state.recordHit(impact, player.serverLevel().getGameTime());
        emitBloodHit(player.serverLevel(), player, impact, state.remaining / state.max);
        if (state.remaining <= 0.0F) {
            BLOOD.remove(player.getUUID());
            com.example.immortal_cultivation_mod.combat.CombatFeedbackEvents.shieldBreak(player, source, consumed);
            PlayerShieldManager.clear(player, BLOOD_SOURCE);
            bloodBurst(player);
        } else {
            PlayerShieldManager.set(player, BLOOD_SOURCE, state.remaining, state.max);
        }
        return Math.max(0.0F, damage - consumed / multiplier);
    }

    private static float absorbWind(ServerPlayer player, DamageSource source, float damage) {
        ShieldState state = WIND.get(player.getUUID());
        if (state == null || damage <= 0.0F) {
            return damage;
        }
        float multiplier = directShieldMultiplier(source);
        float consumed = Math.min(damage * multiplier, state.remaining);
        state.remaining -= consumed;
        Vec3 impact = impactPoint(player, source, 1.25D);
        state.recordHit(impact, player.serverLevel().getGameTime());
        emitWindHit(player.serverLevel(), player, impact,
                source.getDirectEntity() instanceof Projectile,
                state.remaining / state.max);
        if (state.remaining <= 0.0F) {
            stopWind(player, true, true);
        } else {
            PlayerShieldManager.set(player, WIND_SOURCE, state.remaining, state.max);
        }
        return Math.max(0.0F, damage - consumed / multiplier);
    }

    private static float absorbMirror(ServerPlayer player, DamageSource source, float damage) {
        ShieldState state = MIRROR.get(player.getUUID());
        if (state == null || damage <= 0.0F) {
            return damage;
        }
        float multiplier = directShieldMultiplier(source);
        float consumed = Math.min(damage * multiplier, state.remaining);
        state.remaining -= consumed;
        Vec3 impact = impactPoint(player, source, 1.25D);
        state.recordHit(impact, player.serverLevel().getGameTime());
        emitMirrorHit(player.serverLevel(), player, impact, state.remaining / state.max);
        if (state.remaining <= 0.0F) {
            clearMirror(player, true);
        } else {
            PlayerShieldManager.set(player, MIRROR_SOURCE, state.remaining, state.max);
        }
        return Math.max(0.0F, damage - consumed / multiplier);
    }

    private static float absorbSimple(
            ServerPlayer player,
            Map<UUID, ShieldState> shields,
            String source,
            DamageSource damageSource,
            float damage,
            Runnable onBreak,
            ParticleOptions particle) {
        ShieldState state = shields.get(player.getUUID());
        if (state == null || damage <= 0.0F) {
            return damage;
        }
        float multiplier = directShieldMultiplier(damageSource);
        float consumed = Math.min(damage * multiplier, state.remaining);
        state.remaining -= consumed;
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(particle, player.getX(), player.getY() + 1.0D, player.getZ(),
                    10, 0.75D, 0.75D, 0.75D, 0.025D);
        }
        if (state.remaining <= 0.0F) {
            onBreak.run();
        } else {
            PlayerShieldManager.set(player, source, state.remaining, state.max);
        }
        return Math.max(0.0F, damage - consumed / multiplier);
    }

    private static float shieldBreakingMultiplier(DamageSource source) {
        float multiplier = CombatStateManager.isHeavyAttack(source) ? 1.50F : 1.0F;
        multiplier = Math.max(multiplier, directShieldMultiplier(source));
        if (source.getEntity() instanceof LivingEntity attacker
                && WeaponRestrictionSystem.count(attacker.getMainHandItem(),
                WeaponRestrictionSystem.RestrictionType.ARMOR_BREAKING) > 0) {
            multiplier = Math.max(multiplier, 1.25F);
        }
        return multiplier * com.example.immortal_cultivation_mod.attachment.BodyTypeEffects.shieldMultiplier(source);
    }

    private static float directShieldMultiplier(DamageSource source) {
        return DefensePiercingProjectile.shieldMultiplier(source);
    }

    private static void clearEarth(ServerPlayer player, boolean shatter) {
        ShieldState removed = EARTH.remove(player.getUUID());
        applyEarthSlow(player, false);
        PlayerShieldManager.clear(player, EARTH_SOURCE);
        if (shatter && removed != null) {
            com.example.immortal_cultivation_mod.combat.CombatFeedbackEvents.shieldBreak(player, null, 40);
            emitEarthFinalBreak(player.serverLevel(), player);
        }
    }

    private static void clearMirror(ServerPlayer player, boolean shatter) {
        ShieldState removed = MIRROR.remove(player.getUUID());
        PlayerShieldManager.clear(player, MIRROR_SOURCE);
        if (shatter && removed != null) {
            com.example.immortal_cultivation_mod.combat.CombatFeedbackEvents.shieldBreak(player, null, 40);
            emitMirrorBreak(player.serverLevel(), player);
        }
    }

    private static void stopWind(ServerPlayer player, boolean cooldown, boolean collapse) {
        if (WIND.remove(player.getUUID()) == null) {
            return;
        }
        PlayerShieldManager.clear(player, WIND_SOURCE);
        if (collapse) {
            com.example.immortal_cultivation_mod.combat.CombatFeedbackEvents.shieldBreak(player, null, 40);
            emitWindBreak(player.serverLevel(), player);
        }
        if (cooldown) {
            ModPayloads.ModPayloadsHandler.startExternalSpellCooldown(player, ModSpells.LIUFENG_XIEJIN);
        }
    }

    private static void stopDomain(ServerPlayer caster, boolean cooldown, boolean collapse) {
        DomainState state = DOMAINS.remove(caster.getUUID());
        if (state == null) {
            return;
        }
        com.example.immortal_cultivation_mod.network.SpellRibbonPayload.send(caster.serverLevel(),"yuangang_hujie",caster.position(),Vec3.ZERO,caster.getId(),0,1);
        clearDomainViewers(caster, state);
        if (collapse) {
            com.example.immortal_cultivation_mod.combat.CombatFeedbackEvents.shieldBreak(caster, null, 200);
            DOMAIN_AFTERIMAGES.put(caster.getUUID(), new DomainAfterimage(
                    domainCenter(caster), state.radius, caster.serverLevel().getGameTime() + 20L,
                    caster.serverLevel().dimension()));
            collapseDomain(caster, state);
        }
        if (cooldown) {
            ModPayloads.ModPayloadsHandler.startExternalSpellCooldown(caster, ModSpells.YUANGANG_HUJIE);
        }
    }

    private static void bloodBurst(ServerPlayer caster) {
        if (!(caster.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 center = shieldCenter(caster);
        emitSphere(level, BLOOD_DARK, center, 0.42D, 28, caster.tickCount * 0.12D);
        emitBloodSphere(level, caster, 82, 2.15D);
        for (int ring = 0; ring < 4; ring++) {
            emitHorizontalRing(level, ring % 2 == 0 ? BLOOD_DARK : BLOOD_BRIGHT,
                    caster.position().add(0.0D, 0.12D + ring * 0.05D, 0.0D),
                    1.1D + ring * 0.72D, 24 + ring * 4, ring * 0.37D);
        }
        level.sendParticles(ModParticles.METHOD_QI_BLOOD.get(), center.x, center.y, center.z,
                110, 2.2D, 0.75D, 2.2D, 0.22D);
        level.sendParticles(BLOOD_DARK, center.x, center.y, center.z,
                72, 1.8D, 0.62D, 1.8D, 0.18D);
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                caster.getX(), caster.getY() + 1.0D, caster.getZ(), 24, 1.5D, 0.8D, 1.5D, 0.18D);
        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class, caster.getBoundingBox().inflate(4.0D),
                target -> target != caster && target.isAlive() && !isProtectedAlly(caster, target))) {
            Vec3 push = target.position().subtract(caster.position());
            if (push.horizontalDistanceSqr() > 1.0E-6D) {
                push = new Vec3(push.x, 0.0D, push.z).normalize().scale(1.1D);
                target.push(push.x, 0.25D, push.z);
                target.hurtMarked = true;
            }
            target.hurt(caster.damageSources().indirectMagic(caster, caster), 120.0F);
            interrupt(target);
        }
    }

    private static void collapseDomain(ServerPlayer caster, DomainState state) {
        ServerLevel level = caster.serverLevel();
        double radius = state.radius + 2.0D;
        Vec3 center = domainCenter(caster);
        emitDomainSphere(level, caster, state, 120);
        emitSphere(level, DOMAIN_WHITE, center, state.radius * 0.92D, 84,
                caster.tickCount * 0.045D);
        for (int ring = 0; ring < 5; ring++) {
            emitHorizontalRing(level, ring % 2 == 0 ? DOMAIN_WHITE : DOMAIN_BLUE,
                    caster.position().add(0.0D, 0.10D, 0.0D),
                    state.radius * (0.35D + ring * 0.22D), 34 + ring * 7,
                    ring * 0.28D);
        }
        level.sendParticles(ModParticles.METHOD_QI_NEUTRAL.get(), center.x, center.y, center.z,
                420, state.radius * 0.82D, state.radius * 0.62D,
                state.radius * 0.82D, 0.32D);
        level.sendParticles(DOMAIN_WHITE, center.x, center.y, center.z,
                180, state.radius * 0.72D, state.radius * 0.54D,
                state.radius * 0.72D, 0.26D);
        level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z,
                120, state.radius * 0.70D, state.radius * 0.52D,
                state.radius * 0.70D, 0.22D);
        level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE,
                caster.getX(), caster.getY() + 1.0D, caster.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        float minorDamage = Math.max(20.0F, state.max * 0.025F);
        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class, caster.getBoundingBox().inflate(radius),
                target -> target != caster && target.isAlive() && !isProtectedAlly(caster, target))) {
            if (target.distanceToSqr(domainCenter(caster)) > radius * radius) {
                continue;
            }
            Vec3 push = target.position().subtract(caster.position());
            if (push.horizontalDistanceSqr() > 1.0E-6D) {
                push = new Vec3(push.x, 0.0D, push.z).normalize().scale(1.6D);
                target.push(push.x, 0.35D, push.z);
                target.hurtMarked = true;
            }
            target.hurt(caster.damageSources().indirectMagic(caster, caster), minorDamage);
            interrupt(target);
        }
    }

    private static void interrupt(LivingEntity target) {
        if (target instanceof ServerPlayer player) {
            CombatStateManager.cancelSpellPreparation(player);
        }
        if (target instanceof CultivationNpc npc) {
            npc.endNpcCast();
        }
    }

    private static void applyEarthSlow(ServerPlayer player, boolean active) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        AttributeModifier current = speed.getModifier(EARTH_SLOW);
        if (active && current == null) {
            speed.addTransientModifier(new AttributeModifier(
                    EARTH_SLOW, -0.05D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else if (!active && current != null) {
            speed.removeModifier(EARTH_SLOW);
        }
    }

    private static DomainStats domainStats(ServerPlayer player) {
        CultivationLevels.LevelDef level = CultivationLevels.getLevelDef(
                ModAttachments.getData(player).cultivationLevel());
        if (CultivationLevels.STAGE_EARLY.equals(level.stage())) {
            return new DomainStats(3_000, 180, 2_000.0F, 6.0D);
        }
        if (CultivationLevels.STAGE_MID.equals(level.stage())) {
            return new DomainStats(5_000, 300, 3_500.0F, 7.0D);
        }
        return new DomainStats(7_500, 500, 6_000.0F, 8.0D);
    }

    private static boolean insideDomain(ServerPlayer caster, LivingEntity target, DomainState state) {
        return caster.level() == target.level()
                && target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D)
                .distanceToSqr(domainCenter(caster)) <= state.radius * state.radius;
    }

    private static boolean isProtectedAlly(LivingEntity caster, LivingEntity target) {
        return NpcProfile.areCombatAllies(caster, target);
    }

    private static boolean isFriendlySource(ServerPlayer caster, DamageSource source) {
        LivingEntity attacker = NpcProfile.damageAttacker(source);
        return attacker != null && isProtectedAlly(caster, attacker);
    }

    private static boolean isEnemyProjectile(ServerPlayer caster, Projectile projectile) {
        Entity owner = projectile.getOwner();
        return owner != caster && (!(owner instanceof LivingEntity living)
                || !isProtectedAlly(caster, living));
    }

    private static boolean isPhysicalProjectile(Projectile projectile) {
        return projectile instanceof AbstractArrow
                || projectile instanceof ThrowingKnifeEntity
                || projectile instanceof FlyingNeedleEntity
                || projectile instanceof FlyingSwordEntity
                || projectile instanceof CrimsonDragonScaleProjectileEntity;
    }

    private static boolean isStrongProjectile(Projectile projectile) {
        return projectile instanceof FireballProjectileEntity
                || projectile instanceof FireballTalismanProjectileEntity
                || projectile instanceof LightBeamProjectileEntity
                || projectile instanceof LightningProjectileEntity
                || projectile instanceof StarfallProjectileEntity
                || projectile instanceof JinmangChuanshaProjectileEntity;
    }

    private static boolean isReflectableSpellProjectile(Projectile projectile) {
        return !isPhysicalProjectile(projectile);
    }

    private static boolean recentlyReflectedBy(Projectile projectile, ServerPlayer caster, long now) {
        return projectile.getPersistentData().hasUUID(MIRROR_LAST_REFLECTOR)
                && projectile.getPersistentData().getUUID(MIRROR_LAST_REFLECTOR).equals(caster.getUUID())
                && now - projectile.getPersistentData().getLong(MIRROR_LAST_REFLECT_TICK) < 10L;
    }

    private static void reflectProjectile(
            Projectile projectile, ServerPlayer reflector, long now, Vec3 incomingVelocity) {
        Entity oldOwner = projectile.getOwner();
        double speed = Math.max(0.2D, incomingVelocity.length());
        Vec3 direction = oldOwner != null && oldOwner.isAlive()
                ? oldOwner.getBoundingBox().getCenter().subtract(shieldCenter(reflector)).normalize()
                : incomingVelocity.normalize().scale(-1.0D);
        Vec3 start = shieldCenter(reflector).add(direction.scale(2.0D));
        projectile.setPos(start.x, start.y, start.z);
        projectile.setOwner(reflector);
        projectile.getPersistentData().remove(NPC_OWNED_AT_LAUNCH);
        projectile.setDeltaMovement(direction.scale(speed));
        projectile.hurtMarked = true;
        float previousScale = projectile.getPersistentData().contains(MIRROR_DAMAGE_SCALE)
                ? projectile.getPersistentData().getFloat(MIRROR_DAMAGE_SCALE) : 1.0F;
        projectile.getPersistentData().putFloat(MIRROR_DAMAGE_SCALE, previousScale * 0.80F);
        projectile.getPersistentData().putUUID(MIRROR_LAST_REFLECTOR, reflector.getUUID());
        projectile.getPersistentData().putLong(MIRROR_LAST_REFLECT_TICK, now);
    }

    private static void deflectProjectile(Projectile projectile, ServerPlayer caster) {
        Vec3 center = shieldCenter(caster);
        Vec3 away = projectile.position().subtract(center);
        if (away.lengthSqr() < 1.0E-6D) {
            away = projectile.getDeltaMovement().scale(-1.0D);
        }
        away = away.normalize();
        Vec3 side = new Vec3(-away.z, 0.15D, away.x).normalize();
        if (caster.getRandom().nextBoolean()) {
            side = side.scale(-1.0D);
        }
        double speed = Math.max(0.15D, projectile.getDeltaMovement().length());
        Vec3 direction = away.scale(0.7D).add(side.scale(0.7D)).normalize();
        Vec3 start = center.add(direction.scale(2.1D));
        projectile.setPos(start.x, start.y, start.z);
        projectile.setDeltaMovement(direction.scale(speed * 0.9D));
        projectile.hurtMarked = true;
    }

    private static boolean willEnterSphere(Vec3 start, Vec3 velocity, Vec3 center, double radius) {
        double lengthSqr = velocity.lengthSqr();
        if (lengthSqr < 1.0E-8D || velocity.dot(center.subtract(start)) <= 0.0D) {
            return false;
        }
        double t = Mth.clamp(center.subtract(start).dot(velocity) / lengthSqr, 0.0D, 1.0D);
        Vec3 closest = start.add(velocity.scale(t));
        return closest.distanceToSqr(center) <= radius * radius
                || start.distanceToSqr(center) <= radius * radius;
    }

    private static Vec3 shieldCenter(LivingEntity entity) {
        return entity.position().add(0.0D, entity.getBbHeight() * 0.52D, 0.0D);
    }

    private static Vec3 domainCenter(LivingEntity entity) {
        return entity.position().add(0.0D, 1.0D, 0.0D);
    }

    private static int plateCount(float remaining) {
        return remaining <= 0.0F ? 0 : remaining <= 33.0F ? 1 : remaining <= 66.0F ? 2 : 3;
    }

    /** Number of intact OBJ plates still backed by the caster's real earth shield. */
    public static int xuanyanPlateCount(UUID playerId) {
        ShieldState state = EARTH.get(playerId);
        return state == null ? 0 : plateCount(state.remaining);
    }

    private static Vec3 impactPoint(LivingEntity target, DamageSource source, double radius) {
        Vec3 sourcePosition = source.getSourcePosition();
        if (sourcePosition == null && source.getDirectEntity() != null) {
            sourcePosition = source.getDirectEntity().getBoundingBox().getCenter();
        }
        if (sourcePosition == null) {
            sourcePosition = shieldCenter(target).subtract(target.getLookAngle());
        }
        return sphereImpact(target, sourcePosition, radius);
    }

    private static Vec3 domainImpactPoint(
            ServerPlayer caster, LivingEntity protectedTarget, DamageSource source, double radius) {
        Vec3 center = domainCenter(caster);
        Vec3 sourcePosition = source.getSourcePosition();
        if (sourcePosition == null && source.getDirectEntity() != null) {
            sourcePosition = source.getDirectEntity().getBoundingBox().getCenter();
        }
        if (sourcePosition == null) {
            sourcePosition = protectedTarget.getBoundingBox().getCenter();
        }
        Vec3 direction = sourcePosition.subtract(center);
        if (direction.lengthSqr() < 1.0E-6D) {
            direction = protectedTarget.getLookAngle();
        }
        return center.add(direction.normalize().scale(radius));
    }

    private static Vec3 sphereImpact(LivingEntity target, Vec3 sourcePosition, double radius) {
        Vec3 center = shieldCenter(target);
        Vec3 direction = sourcePosition.subtract(center);
        if (direction.lengthSqr() < 1.0E-6D) {
            direction = target.getLookAngle();
        }
        return center.add(direction.normalize().scale(radius));
    }

    private static void emitEarthCast(ServerLevel level, ServerPlayer player) {
        Vec3 ground = player.position().add(0.0D, 0.08D, 0.0D);
        for (int ring = 0; ring < 3; ring++) {
            emitHorizontalRing(level, ring == 1 ? EARTH_GOLD : ModParticles.METHOD_QI_EARTH.get(),
                    ground, 0.65D + ring * 0.42D, 14 + ring * 4, ring * 0.45D);
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()),
                player.getX(), player.getY() + 0.15D, player.getZ(), 24,
                1.15D, 0.08D, 1.15D, 0.22D);
        for (int stream = 0; stream < 3; stream++) {
            for (int point = 0; point < 13; point++) {
                double angle = stream * Math.PI * 2.0D / 3.0D + point * 0.58D;
                double radius = 0.95D - point * 0.025D;
                Vec3 p = player.position().add(Math.cos(angle) * radius,
                        0.12D + point * 0.15D, Math.sin(angle) * radius);
                point(level, point % 3 == 0 ? EARTH_GOLD : ModParticles.METHOD_QI_EARTH.get(), p);
            }
        }
    }

    private static void emitEarthShield(
            ServerLevel level, ServerPlayer player, ShieldState state, long now) {
        Vec3 targetCenter = shieldCenter(player);
        state.visualCenter = state.visualCenter.distanceToSqr(targetCenter) > 16.0D
                ? targetCenter : state.visualCenter.lerp(targetCenter, 0.32D);
        int plates = plateCount(state.remaining);
        double phase = (now - state.startedAt) * (plates == 1 ? 0.115D : 0.045D);
        boolean recentHit = now - state.lastHitAt <= 12L;
        for (int plate = 0; plate < plates; plate++) {
            double angle = phase + plate * Math.PI * 2.0D / Math.max(1, plates);
            Vec3 radial = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
            if (recentHit && plate == 0) {
                Vec3 hitDirection = state.lastHit.subtract(state.visualCenter);
                hitDirection = new Vec3(hitDirection.x, 0.0D, hitDirection.z);
                if (hitDirection.lengthSqr() > 1.0E-5D) {
                    radial = hitDirection.normalize();
                }
            }
            Vec3 tangent = new Vec3(-radial.z, 0.0D, radial.x);
            double tilt = plates == 1 ? 0.0D : (plate - 1) * 0.22D;
            Vec3 up = new Vec3(radial.x * tilt, 1.0D, radial.z * tilt).normalize();
            Vec3 center = state.visualCenter.add(radial.scale(1.16D))
                    .add(0.0D, plate == 1 ? 0.12D : plate == 2 ? -0.08D : 0.0D, 0.0D);
            for (int x = -3; x <= 3; x++) {
                for (int y = -3; y <= 3; y++) {
                    if (Math.abs(x) + Math.abs(y) > 5 || ((x + y + player.tickCount) & 1) != 0) {
                        continue;
                    }
                    Vec3 p = center.add(tangent.scale(x * 0.14D)).add(up.scale(y * 0.18D));
                    point(level, ModParticles.METHOD_QI_EARTH.get(), p);
                    if ((x == 0 || y == 0) && (x + y + plate) % 3 == 0) {
                        point(level, EARTH_GOLD, p.add(radial.scale(0.018D)));
                    }
                }
            }
            int cracks = state.ratio() < 0.34F ? 8 : state.ratio() < 0.67F ? 4 : 2;
            for (int crack = 0; crack < cracks; crack++) {
                double offset = (crack - cracks * 0.5D) * 0.095D;
                point(level, EARTH_GOLD, center.add(tangent.scale(offset))
                        .add(up.scale(Math.sin(crack * 1.7D) * 0.34D)));
            }
            level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST,
                            Blocks.STONE.defaultBlockState()),
                    center.x, center.y - 0.45D, center.z, plates == 1 ? 4 : 2,
                    0.36D, 0.05D, 0.36D, 0.015D);
        }
    }

    private static void emitEarthHit(
            ServerLevel level, ServerPlayer player, Vec3 impact, float ratio) {
        emitOrientedRing(level, EARTH_GOLD, impact,
                impact.subtract(shieldCenter(player)), 0.34D, 12);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()),
                impact.x, impact.y, impact.z, ratio < 0.35F ? 13 : 7,
                0.22D, 0.25D, 0.22D, 0.12D);
        level.sendParticles(ModParticles.METHOD_QI_EARTH.get(), impact.x, impact.y, impact.z,
                8, 0.20D, 0.20D, 0.20D, 0.06D);
    }

    private static void emitEarthPlateBreak(
            ServerLevel level, ServerPlayer player, Vec3 impact, int broken, float ratio) {
        emitEarthHit(level, player, impact, ratio);
        emitEarthShatter(level, impact, 24 * Math.max(1, broken));
        emitOrientedRing(level, ModParticles.METHOD_QI_EARTH.get(), impact,
                impact.subtract(shieldCenter(player)), 0.68D, 18);
    }

    private static void emitEarthFinalBreak(ServerLevel level, ServerPlayer player) {
        Vec3 center = shieldCenter(player);
        emitEarthShatter(level, center, 62);
        level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST,
                        Blocks.STONE.defaultBlockState()),
                center.x, center.y, center.z, 36, 1.25D, 0.85D, 1.25D, 0.12D);
        level.sendParticles(EARTH_GOLD, center.x, center.y, center.z,
                42, 1.1D, 0.8D, 1.1D, 0.08D);
    }

    private static void emitWindCast(ServerLevel level, ServerPlayer player) {
        Vec3 center = shieldCenter(player);
        level.sendParticles(ModParticles.METHOD_QI_WIND.get(), center.x, center.y, center.z,
                34, 1.0D, 0.9D, 1.0D, 0.10D);
        for (int ring = 0; ring < 5; ring++) {
            emitHorizontalRing(level, ring % 2 == 0 ? WIND_PALE : ModParticles.METHOD_QI_WIND.get(),
                    center.add(0.0D, (ring - 2) * 0.27D, 0.0D),
                    0.75D + ring * 0.09D, 17, ring * 0.7D);
        }
        level.sendParticles(ParticleTypes.GUST, center.x, center.y, center.z,
                5, 0.55D, 0.65D, 0.55D, 0.05D);
    }

    private static void emitWindShield(
            ServerLevel level, ServerPlayer player, ShieldState state, long now) {
        Vec3 center = shieldCenter(player);
        float ratio = state.ratio();
        if (ratio >= 0.25F || (now & 3L) != 0L) {
            emitSphere(level, WIND_PALE, center, 1.28D, ratio < 0.25F ? 7 : 12,
                    now * 0.075D);
        }
        int streams = ratio < 0.25F ? 3 : 5;
        for (int stream = 0; stream < streams; stream++) {
            if (ratio < 0.25F && ((now / 2L + stream) % 4L == 0L)) {
                continue;
            }
            for (int pointIndex = 0; pointIndex < 5; pointIndex++) {
                double angle = now * (0.16D + stream * 0.008D)
                        + stream * Math.PI * 2.0D / streams + pointIndex * 0.28D;
                double y = center.y - 0.68D + ((pointIndex + stream * 2) % 6) * 0.27D;
                double radius = 1.02D + 0.10D * Math.sin(now * 0.08D + stream);
                Vec3 p = new Vec3(center.x + Math.cos(angle) * radius, y,
                        center.z + Math.sin(angle) * radius);
                point(level, pointIndex == 2 ? WIND_PALE : ModParticles.METHOD_QI_WIND.get(), p);
            }
        }
        if (now % 6L == 0L) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,
                            Blocks.OAK_LEAVES.defaultBlockState()),
                    center.x, center.y, center.z, 2, 1.05D, 0.65D, 1.05D, 0.08D);
            level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST,
                            Blocks.DIRT.defaultBlockState()),
                    center.x, center.y, center.z, 2, 1.15D, 0.45D, 1.15D, 0.09D);
        }
    }

    private static void emitWindHit(
            ServerLevel level, ServerPlayer player, Vec3 impact, boolean projectile, float ratio) {
        Vec3 normal = impact.subtract(shieldCenter(player));
        emitOrientedRing(level, WIND_PALE, impact, normal, projectile ? 0.48D : 0.64D,
                projectile ? 15 : 10);
        emitCrescent(level, ModParticles.METHOD_QI_WIND.get(), impact, normal, 0.72D, 12);
        level.sendParticles(projectile ? ParticleTypes.GUST : ParticleTypes.POOF,
                impact.x, impact.y, impact.z, projectile ? 4 : 9,
                0.24D, 0.22D, 0.24D, 0.08D);
        if (ratio < 0.25F) {
            level.sendParticles(WIND_PALE, impact.x, impact.y, impact.z,
                    12, 0.48D, 0.42D, 0.48D, 0.10D);
        }
    }

    private static void emitWindDeflection(
            ServerLevel level, ServerPlayer caster, Vec3 impact, Vec3 incoming, Vec3 outgoing) {
        Vec3 in = incoming.lengthSqr() < 1.0E-6D ? caster.getLookAngle() : incoming.normalize();
        Vec3 out = outgoing.lengthSqr() < 1.0E-6D ? in.scale(-1.0D) : outgoing.normalize();
        Vec3 start = impact.subtract(in.scale(1.35D));
        Vec3 end = impact.add(out.scale(1.55D));
        Vec3 control = impact.add(new Vec3(-in.z, 0.18D, in.x).normalize().scale(0.48D));
        emitBezier(level, ModParticles.METHOD_QI_WIND.get(), start, control, end, 13);
        emitBezier(level, WIND_PALE, start, impact, end, 9);
        emitOrientedRing(level, WIND_PALE, impact, incoming, 0.42D, 13);
        level.sendParticles(ParticleTypes.GUST, impact.x, impact.y, impact.z,
                3, 0.18D, 0.18D, 0.18D, 0.03D);
    }

    private static void emitWindBreak(ServerLevel level, ServerPlayer player) {
        Vec3 center = shieldCenter(player);
        for (int ring = 0; ring < 4; ring++) {
            emitHorizontalRing(level, ring % 2 == 0 ? WIND_PALE : ModParticles.METHOD_QI_WIND.get(),
                    center.add(0.0D, (ring - 1.5D) * 0.33D, 0.0D),
                    1.25D + ring * 0.32D, 22, ring * 0.5D);
        }
        emitWindBurst(level, player, 54);
        level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST,
                        Blocks.DIRT.defaultBlockState()),
                center.x, center.y, center.z, 28, 1.8D, 0.35D, 1.8D, 0.18D);
    }

    private static void emitBloodCast(ServerLevel level, ServerPlayer player) {
        Vec3 center = shieldCenter(player);
        level.sendParticles(BLOOD_DARK, center.x, center.y, center.z,
                36, 0.45D, 0.85D, 0.45D, 0.05D);
        level.sendParticles(ModParticles.METHOD_QI_BLOOD.get(), center.x, center.y, center.z,
                46, 0.68D, 0.9D, 0.68D, 0.07D);
        Vec3 chest = player.position().add(0.0D, player.getBbHeight() * 0.64D, 0.0D);
        emitLine(level, BLOOD_BRIGHT, chest.add(-0.28D, 0.0D, 0.0D),
                chest.add(0.28D, 0.0D, 0.0D), 0.08D);
        emitLine(level, BLOOD_BRIGHT, chest.add(0.0D, -0.42D, 0.0D),
                chest.add(0.0D, 0.45D, 0.0D), 0.09D);
        emitBloodSphere(level, player, 28, 1.10D);
    }

    private static void emitBloodShield(
            ServerLevel level, ServerPlayer player, ShieldState state, long now) {
        Vec3 center = shieldCenter(player);
        float hpRatio = player.getHealth() / Math.max(1.0F, player.getMaxHealth());
        float shieldRatio = state.ratio();
        int density = hpRatio < 0.20F ? 25 : hpRatio < 0.40F ? 20 : 15;
        emitSphere(level, hpRatio < 0.40F ? BLOOD_DARK : ModParticles.METHOD_QI_BLOOD.get(),
                center, 1.07D, density, now * 0.09D);
        int veins = hpRatio < 0.40F ? 4 : 2;
        for (int vein = 0; vein < veins; vein++) {
            double angle = now * (0.04D + vein * 0.008D) + vein * 2.1D;
            Vec3 start = center.add(Math.cos(angle) * 1.02D, -0.65D + vein * 0.34D,
                    Math.sin(angle) * 1.02D);
            Vec3 end = center.add(Math.cos(angle + 0.65D) * 1.02D, 0.52D - vein * 0.12D,
                    Math.sin(angle + 0.65D) * 1.02D);
            emitLine(level, vein % 2 == 0 ? BLOOD_BRIGHT : BLOOD_DARK, start, end, 0.24D);
        }
        if (shieldRatio < 0.35F) {
            Vec3 leak = state.lastHitAt == Long.MIN_VALUE ? center.add(1.05D, 0.0D, 0.0D) : state.lastHit;
            level.sendParticles(BLOOD_BRIGHT, leak.x, leak.y, leak.z,
                    5, 0.22D, 0.30D, 0.22D, 0.08D);
        }
        if (hpRatio < 0.20F && now % 12L < 3L) {
            emitSphere(level, BLOOD_BRIGHT, center, 1.22D, 22, now * 0.12D);
            level.sendParticles(ModParticles.METHOD_QI_BLOOD.get(), center.x, center.y, center.z,
                    10, 0.75D, 0.65D, 0.75D, 0.08D);
        }
    }

    private static void emitBloodHit(
            ServerLevel level, ServerPlayer player, Vec3 impact, float ratio) {
        Vec3 normal = impact.subtract(shieldCenter(player));
        emitOrientedRing(level, BLOOD_BRIGHT, impact, normal, 0.46D, 15);
        level.sendParticles(ModParticles.METHOD_QI_BLOOD.get(), impact.x, impact.y, impact.z,
                16, 0.42D, 0.34D, 0.42D, 0.12D);
        level.sendParticles(BLOOD_DARK, impact.x, impact.y, impact.z,
                ratio < 0.35F ? 18 : 9, 0.35D, 0.30D, 0.35D, 0.08D);
    }

    private static void emitMirrorCast(ServerLevel level, ServerPlayer player) {
        Vec3 center = shieldCenter(player);
        Vec3 previous = null;
        for (int fragment = 0; fragment < 12; fragment++) {
            double angle = fragment * Math.PI * 2.0D / 12.0D;
            Vec3 p = center.add(Math.cos(angle) * 1.28D,
                    Math.sin(angle * 2.0D) * 0.58D, Math.sin(angle) * 1.28D);
            level.sendParticles(fragment % 2 == 0 ? MIRROR_WHITE : ModParticles.METHOD_QI_YANG.get(),
                    p.x, p.y, p.z, 3, 0.08D, 0.08D, 0.08D, 0.025D);
            if (previous != null) {
                emitLine(level, MIRROR_GOLD, previous, p, 0.20D);
            }
            previous = p;
        }
        emitSphere(level, MIRROR_WHITE, center, 1.24D, 34, player.tickCount * 0.04D);
    }

    private static void emitMirrorShield(
            ServerLevel level, ServerPlayer player, ShieldState state, long now) {
        Vec3 center = shieldCenter(player);
        float ratio = state.ratio();
        if (ratio >= 0.25F || now % 6L != 0L) {
            emitSphere(level, MIRROR_WHITE, center, 1.25D, ratio < 0.25F ? 8 : 13,
                    now * 0.035D);
        }
        for (int fragment = 0; fragment < 5; fragment++) {
            double angle = now * 0.035D + fragment * Math.PI * 2.0D / 5.0D;
            Vec3 p = center.add(Math.cos(angle) * 1.42D,
                    Math.sin(angle * 1.7D + fragment) * 0.66D,
                    Math.sin(angle) * 1.42D);
            point(level, fragment % 2 == 0 ? MIRROR_GOLD : ModParticles.METHOD_QI_YANG.get(), p);
        }
        int cracks = Math.min(7, Math.max(0, (int) ((1.0F - ratio) * 9.0F)));
        Vec3 crackCenter = state.lastHitAt == Long.MIN_VALUE
                ? center.add(1.24D, 0.0D, 0.0D) : state.lastHit;
        Vec3 normal = crackCenter.subtract(center);
        Vec3[] basis = planeBasis(normal);
        for (int crack = 0; crack < cracks; crack++) {
            double angle = crack * 2.399D + now * 0.006D;
            Vec3 end = crackCenter.add(basis[0].scale(Math.cos(angle) * (0.22D + crack * 0.025D)))
                    .add(basis[1].scale(Math.sin(angle) * (0.22D + crack * 0.025D)));
            emitLine(level, crack % 2 == 0 ? MIRROR_WHITE : MIRROR_GOLD,
                    crackCenter, end, 0.10D);
        }
        if (ratio < 0.25F && (now & 3L) == 0L) {
            level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z,
                    7, 1.15D, 0.8D, 1.15D, 0.03D);
        }
    }

    private static void emitMirrorHit(
            ServerLevel level, ServerPlayer player, Vec3 impact, float ratio) {
        Vec3 normal = impact.subtract(shieldCenter(player));
        emitOrientedRing(level, MIRROR_WHITE, impact, normal, 0.48D, 16);
        emitOrientedRing(level, MIRROR_GOLD, impact, normal, 0.25D, 10);
        Vec3[] basis = planeBasis(normal);
        for (int crack = 0; crack < (ratio < 0.35F ? 7 : 4); crack++) {
            double angle = crack * Math.PI * 2.0D / 7.0D;
            Vec3 end = impact.add(basis[0].scale(Math.cos(angle) * 0.52D))
                    .add(basis[1].scale(Math.sin(angle) * 0.52D));
            emitLine(level, MIRROR_WHITE, impact, end, 0.11D);
        }
    }

    private static void emitMirrorReflectionStart(
            ServerLevel level, ServerPlayer caster, Vec3 impact) {
        Vec3 normal = impact.subtract(shieldCenter(caster));
        emitReflectionSymbol(level, impact, normal, false);
        level.sendParticles(ParticleTypes.FLASH, impact.x, impact.y, impact.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static void emitMirrorSuspension(
            ServerLevel level, ServerPlayer caster, Vec3 projectilePosition) {
        emitOrientedRing(level, MIRROR_WHITE, projectilePosition,
                projectilePosition.subtract(shieldCenter(caster)), 0.24D, 8);
    }

    private static void emitMirrorReflectionSuccess(
            ServerLevel level, ServerPlayer caster, Vec3 projectilePosition, UUID originalOwnerId) {
        level.sendParticles(ParticleTypes.FLASH, projectilePosition.x, projectilePosition.y,
                projectilePosition.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ModParticles.METHOD_QI_YANG.get(), projectilePosition.x,
                projectilePosition.y, projectilePosition.z, 34, 0.55D, 0.55D, 0.55D, 0.13D);
        level.sendParticles(MIRROR_GOLD, projectilePosition.x, projectilePosition.y,
                projectilePosition.z, 20, 0.42D, 0.42D, 0.42D, 0.10D);
        if (originalOwnerId != null) {
            Entity originalOwner = level.getEntity(originalOwnerId);
            if (originalOwner != null) {
                emitLine(level, MIRROR_WHITE, projectilePosition,
                        originalOwner.getBoundingBox().getCenter(), 0.42D);
            }
        }
    }

    private static void emitMirrorReflectionFailure(
            ServerLevel level, ServerPlayer caster, Vec3 impact) {
        Vec3 normal = impact.subtract(shieldCenter(caster));
        emitReflectionSymbol(level, impact, normal, true);
        level.sendParticles(MIRROR_WHITE, impact.x, impact.y, impact.z,
                24, 0.45D, 0.45D, 0.45D, 0.11D);
    }

    private static void emitMirrorBreak(ServerLevel level, ServerPlayer player) {
        Vec3 center = shieldCenter(player);
        emitSphere(level, MIRROR_WHITE, center, 1.25D, 54, player.tickCount * 0.04D);
        level.sendParticles(ModParticles.METHOD_QI_YANG.get(), center.x, center.y, center.z,
                180, 1.45D, 1.15D, 1.45D, 0.18D);
        level.sendParticles(MIRROR_GOLD, center.x, center.y, center.z,
                96, 1.25D, 1.0D, 1.25D, 0.14D);
        level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        for (int ring = 0; ring < 3; ring++) {
            emitHorizontalRing(level, MIRROR_WHITE, center,
                    0.8D + ring * 0.55D, 22, ring * 0.35D);
        }
    }

    private static void emitReflectionSymbol(
            ServerLevel level, Vec3 center, Vec3 normal, boolean broken) {
        emitOrientedRing(level, broken ? MIRROR_WHITE : MIRROR_GOLD,
                center, normal, 0.52D, broken ? 11 : 18);
        emitOrientedRing(level, MIRROR_WHITE, center, normal, 0.27D, 12);
        Vec3[] basis = planeBasis(normal);
        for (int ray = 0; ray < 6; ray++) {
            if (broken && ray % 2 == 0) {
                continue;
            }
            double angle = ray * Math.PI / 3.0D;
            Vec3 end = center.add(basis[0].scale(Math.cos(angle) * 0.65D))
                    .add(basis[1].scale(Math.sin(angle) * 0.65D));
            emitLine(level, broken ? MIRROR_WHITE : MIRROR_GOLD, center, end, 0.14D);
        }
    }

    private static void emitDomainCast(
            ServerLevel level, ServerPlayer caster, DomainState state) {
        Vec3 center = domainCenter(caster);
        com.example.immortal_cultivation_mod.network.SpellRibbonPayload.send(level,"yuangang_hujie",center,new Vec3(0,0,1),caster.getId(),10,(float)state.radius);
        level.sendParticles(ModParticles.METHOD_QI_NEUTRAL.get(), center.x, center.y, center.z,
                72, 0.8D, 1.25D, 0.8D, 0.16D);
        level.sendParticles(DOMAIN_WHITE, center.x, center.y + 1.6D, center.z,
                38, 0.18D, 1.8D, 0.18D, 0.08D);
        for (int ring = 1; ring <= 4; ring++) {
            emitHorizontalRing(level, ring % 2 == 0 ? DOMAIN_BLUE : DOMAIN_WHITE,
                    caster.position().add(0.0D, 0.08D, 0.0D),
                    state.radius * ring / 4.0D, 18 + ring * 5, ring * 0.42D);
        }
        for (int shell = 1; shell <= 3; shell++) {
            emitSphere(level, shell == 3 ? DOMAIN_WHITE : ModParticles.METHOD_QI_NEUTRAL.get(),
                    center, state.radius * shell / 3.0D, 20 + shell * 7, shell * 0.55D);
        }
    }

    private static void emitDomainShield(
            ServerLevel level, ServerPlayer caster, DomainState state, long now) {
        Vec3 center = domainCenter(caster);
        com.example.immortal_cultivation_mod.network.SpellRibbonPayload.send(level,"yuangang_hujie",center,new Vec3(0,0,1),caster.getId(),10,(float)state.radius);
        float ratio = state.ratio();
        emitDomainSphere(level, caster, state, ratio < 0.15F ? 38 : 24);
        emitHorizontalRing(level, ratio < 0.15F ? DOMAIN_WHITE : DOMAIN_BLUE,
                caster.position().add(0.0D, 0.07D, 0.0D), state.radius, 32, now * 0.025D);
        double latitude = Math.sin(now * 0.035D) * state.radius * 0.62D;
        double latitudeRadius = Math.sqrt(Math.max(0.0D,
                state.radius * state.radius - latitude * latitude));
        emitHorizontalRing(level, DOMAIN_BLUE, center.add(0.0D, latitude, 0.0D),
                latitudeRadius, 22, -now * 0.03D);
        level.sendParticles(ModParticles.METHOD_QI_NEUTRAL.get(),
                center.x, caster.getY() + 0.2D, center.z, 8,
                state.radius * 0.62D, 0.08D, state.radius * 0.62D, 0.04D);

        int condition = ratio > 0.70F ? 0 : ratio > 0.40F ? 2 : ratio > 0.15F ? 5 : 9;
        Vec3 crackCenter = state.lastHitAt == Long.MIN_VALUE
                ? center.add(state.radius, 0.0D, 0.0D) : state.lastHit;
        Vec3 normal = crackCenter.subtract(center);
        Vec3[] basis = planeBasis(normal);
        for (int crack = 0; crack < condition; crack++) {
            double angle = crack * 2.399D + now * 0.003D;
            Vec3 start = crackCenter.add(basis[0].scale(Math.cos(angle) * crack * 0.09D))
                    .add(basis[1].scale(Math.sin(angle) * crack * 0.09D));
            Vec3 end = start.add(basis[0].scale(Math.cos(angle + 0.7D) * (0.35D + crack * 0.05D)))
                    .add(basis[1].scale(Math.sin(angle + 0.7D) * (0.35D + crack * 0.05D)));
            emitLine(level, ratio < 0.15F ? DOMAIN_WHITE : DOMAIN_BLUE, start, end, 0.18D);
        }
        if (ratio < 0.40F) {
            level.sendParticles(DOMAIN_BLUE, crackCenter.x, crackCenter.y, crackCenter.z,
                    ratio < 0.15F ? 18 : 7, 0.65D, 0.65D, 0.65D, 0.09D);
        }
        if (ratio < 0.15F && now % 8L < 4L) {
            emitSphere(level, DOMAIN_WHITE, center,
                    state.radius + 0.10D * Math.sin(now), 28, now * 0.08D);
            level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z,
                    13, state.radius * 0.75D, state.radius * 0.55D,
                    state.radius * 0.75D, 0.08D);
        }
    }

    private static void emitDomainProjectileSlow(
            ServerLevel level, ServerPlayer caster, DomainState state, Projectile projectile) {
        Vec3 impact = sphereImpact(caster, projectile.position(), state.radius);
        Vec3 normal = projectile.getDeltaMovement().lengthSqr() < 1.0E-6D
                ? impact.subtract(domainCenter(caster)) : projectile.getDeltaMovement();
        emitOrientedRing(level, DOMAIN_BLUE, impact, normal, 0.72D, 18);
        emitOrientedRing(level, DOMAIN_WHITE, projectile.position(), normal, 0.30D, 11);
        Vec3 direction = normal.normalize();
        for (int stream = 0; stream < 3; stream++) {
            Vec3 start = projectile.position().subtract(direction.scale(0.35D + stream * 0.2D));
            Vec3 end = projectile.position().subtract(direction.scale(1.15D + stream * 0.25D));
            emitLine(level, stream == 1 ? DOMAIN_WHITE : DOMAIN_BLUE, start, end, 0.16D);
        }
    }

    private static void emitDomainImpact(
            ServerLevel level, ServerPlayer caster, Vec3 hit, DomainState state) {
        float ratio = state.ratio();
        Vec3 normal = hit.subtract(domainCenter(caster));
        emitOrientedRing(level, ratio < 0.15F ? DOMAIN_WHITE : DOMAIN_BLUE,
                hit, normal, ratio < 0.40F ? 1.25D : 0.85D, ratio < 0.40F ? 24 : 17);
        level.sendParticles(ratio < 0.15F ? ParticleTypes.END_ROD
                        : ModParticles.METHOD_QI_NEUTRAL.get(),
                hit.x, hit.y, hit.z, ratio < 0.15F ? 26 : 12,
                0.55D, 0.65D, 0.55D, 0.08D);
        if (ratio < 0.70F) {
            Vec3[] basis = planeBasis(normal);
            for (int crack = 0; crack < (ratio < 0.40F ? 7 : 3); crack++) {
                double angle = crack * 2.1D;
                Vec3 end = hit.add(basis[0].scale(Math.cos(angle) * 0.95D))
                        .add(basis[1].scale(Math.sin(angle) * 0.95D));
                emitLine(level, DOMAIN_WHITE, hit, end, 0.17D);
            }
        }
    }

    private static void tickDomainAfterimage(ServerPlayer caster, long now) {
        DomainAfterimage afterimage = DOMAIN_AFTERIMAGES.get(caster.getUUID());
        if (afterimage == null) {
            return;
        }
        if (now >= afterimage.expiresAt || !caster.serverLevel().dimension().equals(afterimage.dimension)) {
            DOMAIN_AFTERIMAGES.remove(caster.getUUID());
            return;
        }
        if ((now & 1L) != 0L) {
            return;
        }
        float fade = (afterimage.expiresAt - now) / 20.0F;
        emitHorizontalRing(caster.serverLevel(), fade > 0.45F ? DOMAIN_WHITE : DOMAIN_BLUE,
                afterimage.center.add(0.0D, -0.92D, 0.0D),
                afterimage.radius * (0.96D + fade * 0.04D), 34, now * 0.018D);
        for (int spoke = 0; spoke < 8; spoke++) {
            double angle = spoke * Math.PI / 4.0D + now * 0.004D;
            Vec3 end = afterimage.center.add(Math.cos(angle) * afterimage.radius,
                    -0.92D, Math.sin(angle) * afterimage.radius);
            emitLine(caster.serverLevel(), DOMAIN_BLUE,
                    afterimage.center.add(0.0D, -0.92D, 0.0D), end, 0.65D);
        }
    }

    private static void emitBloodSphere(ServerLevel level, ServerPlayer player, int count, double radius) {
        emitSphere(level, ModParticles.METHOD_QI_BLOOD.get(), shieldCenter(player), radius, count,
                player.tickCount * 0.055D);
    }

    private static void emitDomainSphere(ServerLevel level, ServerPlayer caster, DomainState state, int count) {
        emitSphere(level, ModParticles.METHOD_QI_NEUTRAL.get(), domainCenter(caster), state.radius,
                count, caster.tickCount * 0.018D);
    }

    private static void emitSphere(
            ServerLevel level, ParticleOptions particle, Vec3 center,
            double radius, int count, double phase) {
        double goldenAngle = Math.PI * (3.0D - Math.sqrt(5.0D));
        for (int i = 0; i < count; i++) {
            double y = 1.0D - 2.0D * (i + 0.5D) / count;
            double horizontal = Math.sqrt(Math.max(0.0D, 1.0D - y * y));
            double angle = i * goldenAngle + phase;
            point(level, particle, new Vec3(
                    center.x + Math.cos(angle) * horizontal * radius,
                    center.y + y * radius,
                    center.z + Math.sin(angle) * horizontal * radius));
        }
    }

    private static void emitHorizontalRing(
            ServerLevel level, ParticleOptions particle, Vec3 center,
            double radius, int count, double phase) {
        for (int i = 0; i < count; i++) {
            double angle = phase + i * Math.PI * 2.0D / count;
            point(level, particle, center.add(Math.cos(angle) * radius, 0.0D,
                    Math.sin(angle) * radius));
        }
    }

    private static void emitOrientedRing(
            ServerLevel level, ParticleOptions particle, Vec3 center,
            Vec3 normal, double radius, int count) {
        Vec3[] basis = planeBasis(normal);
        for (int i = 0; i < count; i++) {
            double angle = i * Math.PI * 2.0D / count;
            Vec3 p = center.add(basis[0].scale(Math.cos(angle) * radius))
                    .add(basis[1].scale(Math.sin(angle) * radius));
            point(level, particle, p);
        }
    }

    private static void emitCrescent(
            ServerLevel level, ParticleOptions particle, Vec3 center,
            Vec3 normal, double radius, int count) {
        Vec3[] basis = planeBasis(normal);
        for (int i = 0; i < count; i++) {
            double angle = -Math.PI * 0.72D + i * Math.PI * 1.44D / Math.max(1, count - 1);
            Vec3 p = center.add(basis[0].scale(Math.cos(angle) * radius))
                    .add(basis[1].scale(Math.sin(angle) * radius));
            point(level, particle, p);
        }
    }

    private static Vec3[] planeBasis(Vec3 normal) {
        Vec3 n = normal.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 0.0D, 1.0D) : normal.normalize();
        Vec3 helper = Math.abs(n.y) < 0.85D
                ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 first = n.cross(helper).normalize();
        Vec3 second = n.cross(first).normalize();
        return new Vec3[]{first, second};
    }

    private static void emitLine(
            ServerLevel level, ParticleOptions particle, Vec3 start, Vec3 end, double spacing) {
        double distance = start.distanceTo(end);
        int points = Math.max(2, Math.min(80, Mth.ceil(distance / Math.max(0.04D, spacing))));
        for (int i = 0; i <= points; i++) {
            point(level, particle, start.lerp(end, i / (double) points));
        }
    }

    private static void emitBezier(
            ServerLevel level, ParticleOptions particle, Vec3 start,
            Vec3 control, Vec3 end, int points) {
        for (int i = 0; i <= points; i++) {
            double t = i / (double) points;
            double inverse = 1.0D - t;
            Vec3 p = start.scale(inverse * inverse)
                    .add(control.scale(2.0D * inverse * t)).add(end.scale(t * t));
            point(level, particle, p);
        }
    }

    private static void point(ServerLevel level, ParticleOptions particle, Vec3 position) {
        level.sendParticles(particle, position.x, position.y, position.z,
                1, 0.008D, 0.008D, 0.008D, 0.0D);
    }

    private static void emitEarthShatter(ServerLevel level, Vec3 position, int count) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()),
                position.x, position.y, position.z, count, 0.8D, 0.8D, 0.8D, 0.16D);
        level.sendParticles(ModParticles.METHOD_QI_EARTH.get(),
                position.x, position.y, position.z, Math.max(6, count / 2), 0.8D, 0.8D, 0.8D, 0.06D);
    }

    private static void emitWindBurst(ServerLevel level, LivingEntity entity, int count) {
        level.sendParticles(ModParticles.METHOD_QI_WIND.get(),
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(),
                count, 0.9D, 0.8D, 0.9D, 0.10D);
        level.sendParticles(ParticleTypes.GUST,
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(),
                Math.max(2, count / 8), 0.5D, 0.5D, 0.5D, 0.05D);
    }

    private static void syncDomainViewers(ServerPlayer caster, DomainState state) {
        Set<UUID> current = new HashSet<>();
        for (ServerPlayer player : caster.serverLevel().players()) {
            if (insideDomain(caster, player, state) && isProtectedAlly(caster, player)) {
                current.add(player.getUUID());
                PlayerShieldManager.set(player, domainSource(caster), state.remaining, state.max);
            }
        }
        for (UUID oldViewer : new HashSet<>(state.viewers)) {
            if (!current.contains(oldViewer)) {
                ServerPlayer player = serverPlayer(caster.serverLevel(), oldViewer);
                if (player != null) {
                    PlayerShieldManager.clear(player, domainSource(caster));
                }
            }
        }
        state.viewers.clear();
        state.viewers.addAll(current);
    }

    private static void clearDomainViewers(ServerPlayer caster, DomainState state) {
        for (UUID viewer : state.viewers) {
            ServerPlayer player = serverPlayer(caster.serverLevel(), viewer);
            if (player != null) {
                PlayerShieldManager.clear(player, domainSource(caster));
            }
        }
        state.viewers.clear();
    }

    private static String domainSource(ServerPlayer caster) {
        return ModSpells.YUANGANG_HUJIE + ":" + caster.getUUID();
    }

    private static ServerPlayer serverPlayer(ServerLevel level, UUID playerId) {
        Player player = level.getPlayerByUUID(playerId);
        return player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    private static final class ShieldState {
        private float remaining;
        private final float max;
        private final long expiresAt;
        private final long startedAt;
        private Vec3 visualCenter;
        private Vec3 lastHit = Vec3.ZERO;
        private long lastHitAt = Long.MIN_VALUE;

        private ShieldState(
                float remaining, float max, long expiresAt, long startedAt, Vec3 visualCenter) {
            this.remaining = remaining;
            this.max = max;
            this.expiresAt = expiresAt;
            this.startedAt = startedAt;
            this.visualCenter = visualCenter;
        }

        private void recordHit(Vec3 hit, long gameTime) {
            lastHit = hit;
            lastHitAt = gameTime;
        }

        private float ratio() {
            return max <= 0.0F ? 0.0F : Mth.clamp(remaining / max, 0.0F, 1.0F);
        }
    }

    private static final class DomainState {
        private float remaining;
        private final float max;
        private final double radius;
        private final int sustainCost;
        private final long startedAt;
        private Vec3 lastHit = Vec3.ZERO;
        private long lastHitAt = Long.MIN_VALUE;
        private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();

        private DomainState(
                float remaining, float max, double radius, int sustainCost, long startedAt) {
            this.remaining = remaining;
            this.max = max;
            this.radius = radius;
            this.sustainCost = sustainCost;
            this.startedAt = startedAt;
        }

        private void recordHit(Vec3 hit, long gameTime) {
            lastHit = hit;
            lastHitAt = gameTime;
        }

        private float ratio() {
            return max <= 0.0F ? 0.0F : Mth.clamp(remaining / max, 0.0F, 1.0F);
        }
    }

    private record PendingReflection(
            UUID reflectorId, long releaseAt, Vec3 incomingVelocity, UUID originalOwnerId) {
    }

    private record DomainAfterimage(
            Vec3 center, double radius, long expiresAt, ResourceKey<Level> dimension) {
    }

    private record DomainStats(int activationCost, int sustainCost, float capacity, double radius) {
    }
}
