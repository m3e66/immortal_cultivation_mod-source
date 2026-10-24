package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.combat.CombatStateManager;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import com.example.immortal_cultivation_mod.entity.CultivatorCorpseEntity;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import com.example.immortal_cultivation_mod.entity.TemporaryTerrainEntity;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Server-authoritative combat kit shared by ranked zombies and raised corpses. */
public final class UndeadAbilitySystem {
    public static final String TAG_NO_LOOT = "icmUndeadAbilityNoLoot";
    public static final String TAG_NO_SUMMON = "icmUndeadAbilityNoSummon";

    private static final String TAG_PREFIX = "icmUndeadAbility.";
    private static final String TAG_CAST = TAG_PREFIX + "cast";
    private static final String TAG_CAST_END = TAG_PREFIX + "castEnd";
    private static final String TAG_CAST_TARGET = TAG_PREFIX + "castTarget";
    private static final String TAG_CAST_NEXT_PULSE = TAG_PREFIX + "castNextPulse";
    private static final String TAG_COMBAT_START = TAG_PREFIX + "combatStart";
    private static final String TAG_JIANGSHEN_UNTIL = TAG_PREFIX + "jiangshenUntil";
    private static final String TAG_JIANGSHEN_VFX_ACTIVE = TAG_PREFIX + "jiangshenVfxActive";
    private static final String TAG_JIANGSHEN_COUNTER_UNTIL = TAG_PREFIX + "jiangshenCounterUntil";
    private static final String TAG_TRAVEL_INVULN_UNTIL = TAG_PREFIX + "travelInvulnUntil";
    private static final String TAG_POUNCE_UNTIL = TAG_PREFIX + "pounceUntil";
    private static final String TAG_POUNCE_HIT = TAG_PREFIX + "pounceHit";
    private static final String TAG_POUNCE_TARGET = TAG_PREFIX + "pounceTarget";
    private static final String TAG_BONE_GROWTH_UNTIL = TAG_PREFIX + "boneGrowthUntil";
    private static final String TAG_BONE_GROWTH_PAID = TAG_PREFIX + "boneGrowthPaid";
    private static final String TAG_BONE_GROWTH_SHIELD = TAG_PREFIX + "boneGrowthShield";
    private static final String TAG_BONE_GROWTH_SHIELD_MAX = TAG_PREFIX + "boneGrowthShieldMax";
    private static final String TAG_FIRE_CLAWS_UNTIL = TAG_PREFIX + "fireClawsUntil";
    private static final String TAG_HOT_UNTIL = TAG_PREFIX + "hotUntil";
    private static final String TAG_HOT_NEXT = TAG_PREFIX + "hotNext";
    private static final String TAG_HOT_AMOUNT = TAG_PREFIX + "hotAmount";
    private static final String TAG_SUMMON_EXPIRES = TAG_PREFIX + "summonExpires";
    private static final String TAG_COPY_EXPIRES = TAG_PREFIX + "copyExpires";
    private static final String TAG_COPY = TAG_PREFIX + "copy";
    private static final String TAG_COPY_OWNER = TAG_PREFIX + "copyOwner";
    private static final String TAG_COPY_OFFSET_X = TAG_PREFIX + "copyOffsetX";
    private static final String TAG_COPY_OFFSET_Z = TAG_PREFIX + "copyOffsetZ";
    private static final String TAG_DOMAIN_DAMAGE_UNTIL = TAG_PREFIX + "domainDamageUntil";
    private static final String TAG_DOMAIN_DAMAGE_MULTIPLIER = TAG_PREFIX + "domainDamageMultiplier";
    private static final String TAG_IMMORTAL_BONES_UNTIL = TAG_PREFIX + "immortalBonesUntil";
    private static final String TAG_IMMORTAL_BONES_VFX_ACTIVE = TAG_PREFIX + "immortalBonesVfxActive";
    private static final String TAG_BONE_DELAYED_DAMAGE = TAG_PREFIX + "boneDelayedDamage";
    private static final String TAG_BONE_DELAYED_UNTIL = TAG_PREFIX + "boneDelayedUntil";
    private static final String TAG_BONE_DELAYED_NEXT = TAG_PREFIX + "boneDelayedNext";
    private static final String TAG_BONE_UNITY_UNTIL = TAG_PREFIX + "boneUnityUntil";
    private static final String TAG_BONE_UNITY_ARMOR = TAG_PREFIX + "boneUnityArmor";
    private static final String TAG_CAGE_UNTIL = TAG_PREFIX + "cageUntil";
    private static final String TAG_CAGE_X = TAG_PREFIX + "cageX";
    private static final String TAG_CAGE_Y = TAG_PREFIX + "cageY";
    private static final String TAG_CAGE_Z = TAG_PREFIX + "cageZ";
    private static final String TAG_HEAL_PENALTY_UNTIL = TAG_PREFIX + "healPenaltyUntil";
    private static final String TAG_HEAL_PENALTY = TAG_PREFIX + "healPenalty";
    private static final String TAG_QI_PENALTY_UNTIL = TAG_PREFIX + "qiPenaltyUntil";
    private static final String TAG_QI_PENALTY = TAG_PREFIX + "qiPenalty";
    private static final String TAG_ELEMENT_DOMAIN_UNTIL = TAG_PREFIX + "elementDomainUntil";
    private static final String TAG_ABILITY_DAMAGE = TAG_PREFIX + "dealingAbilityDamage";
    private static final String TAG_LAST_MAJOR_STAGGER = TAG_PREFIX + "lastMajorStagger";
    private static final String TAG_CORRUPT_FIELD_TOGGLE = TAG_PREFIX + "corruptFieldToggle";
    private static final String BONE_GROWTH_SHIELD_SOURCE = "undead_bone_growth";

    private static final String BREATH = "shiqi_tuxi";
    private static final String POUNCE = "shiying_pusha";
    private static final String ROAR = "shihou";
    private static final String CORRUPT_FIELD = "fudi_shiyu";
    private static final String DEVOUR_CORPSE = "tunshi_huisheng";
    private static final String SHADOW_ESCAPE = "yinshi_dun";
    private static final String BONE_GROWTH = "shigu_baosheng";
    private static final String FIRE_CLAWS = "shihuo_fuzhao";
    private static final String SUMMON = "zhaoshi";
    private static final String MASS_DEVOUR = "wanshi_guiqi";
    private static final String FUSHI_DOMAIN = "shiyu_tianmu";
    private static final String LIFE_DRAIN = "shesheng_duoxue";
    private static final String SHADOW_COPIES = "shiying_fenshen";
    private static final String EARTH_CORRUPTION = "dimai_shihua";
    private static final String IMMORTAL_BONES = "bumie_gushen";
    private static final String BONE_PRISON = "guyu_fengshen";
    private static final String BONE_RESHAPE = "shigu_zaishu";
    private static final String BONE_UNITY = "wangu_guiyi";
    private static final String HANBA_DOMAIN = "chidi_qianli";
    private static final String BLOOD_DROUGHT = "fenxue_hanqi";
    private static final String ELEMENT_DOMAIN = "tianhan_lingyu";
    private static final String BA_FIRE_BURST = "bahuo_linshi";
    private static final List<String> ABILITY_IDS = List.of(
            BREATH, POUNCE, ROAR, CORRUPT_FIELD, DEVOUR_CORPSE,
            SHADOW_ESCAPE, BONE_GROWTH, FIRE_CLAWS, SUMMON,
            MASS_DEVOUR, FUSHI_DOMAIN, LIFE_DRAIN, SHADOW_COPIES,
            EARTH_CORRUPTION, IMMORTAL_BONES, BONE_PRISON, BONE_RESHAPE,
            BONE_UNITY, HANBA_DOMAIN, BLOOD_DROUGHT, ELEMENT_DOMAIN, BA_FIRE_BURST);

    private static final DustParticleOptions CORPSE_GREEN =
            new DustParticleOptions(new Vector3f(0.11F, 0.35F, 0.13F), 0.9F);
    private static final DustParticleOptions SICK_GREEN =
            new DustParticleOptions(new Vector3f(0.34F, 0.55F, 0.12F), 0.72F);
    private static final DustParticleOptions CORPSE_BLACK =
            new DustParticleOptions(new Vector3f(0.035F, 0.045F, 0.04F), 1.0F);
    private static final DustParticleOptions CORPSE_RED =
            new DustParticleOptions(new Vector3f(0.34F, 0.035F, 0.025F), 0.9F);
    private static final DustParticleOptions BONE_WHITE =
            new DustParticleOptions(new Vector3f(0.78F, 0.76F, 0.65F), 0.9F);
    private static final DustParticleOptions DROUGHT_RED =
            new DustParticleOptions(new Vector3f(0.72F, 0.12F, 0.025F), 1.1F);
    private static final ResourceLocation BONE_GROWTH_REACH_MODIFIER =
            ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "bone_growth_reach");
    private static final ResourceLocation BONE_GROWTH_SPEED_MODIFIER =
            ResourceLocation.fromNamespaceAndPath("immortal_cultivation_mod", "bone_growth_speed");

    private UndeadAbilitySystem() {
    }

    public static List<String> playerAbilityIds() {
        return ABILITY_IDS;
    }

    public static boolean isPlayerAbility(String rawId) {
        return ABILITY_IDS.contains(normalizeAbilityId(rawId));
    }

    public static boolean requiresPlayerPreparation(String rawId) {
        return switch (normalizeAbilityId(rawId)) {
            case BREATH, POUNCE, ROAR, FIRE_CLAWS, SUMMON,
                    LIFE_DRAIN, BONE_PRISON, BA_FIRE_BURST -> true;
            default -> false;
        };
    }

    public static boolean canPlayerUseAbility(boolean livingCorpse, long grudge, String rawId) {
        String id = normalizeAbilityId(rawId);
        if (!livingCorpse || !ABILITY_IDS.contains(id)) {
            return false;
        }
        return grudge >= requiredGrudge(id);
    }

    public static boolean canPlayerUseAbility(Player player, String rawId) {
        return player != null && canPlayerUseAbility(
                UndeadControl.hasUndeadPowers(player), UndeadControl.grudge(player), rawId);
    }

    public static int playerAbilityCooldownTicks(String rawId) {
        return switch (normalizeAbilityId(rawId)) {
            case BREATH -> 8 * 20;
            case POUNCE -> 7 * 20;
            case ROAR -> 13 * 20;
            case CORRUPT_FIELD -> 0;
            case DEVOUR_CORPSE -> 25 * 20;
            case SHADOW_ESCAPE -> 12 * 20;
            case BONE_GROWTH -> 24 * 20;
            case FIRE_CLAWS -> 17 * 20;
            case SUMMON -> 35 * 20;
            case MASS_DEVOUR -> 40 * 20;
            case FUSHI_DOMAIN -> 45 * 20;
            case LIFE_DRAIN -> 20 * 20;
            case SHADOW_COPIES -> 30 * 20;
            case EARTH_CORRUPTION -> 50 * 20;
            case IMMORTAL_BONES -> 40 * 20;
            case BONE_PRISON -> 28 * 20;
            case BONE_RESHAPE -> 60 * 20;
            case BONE_UNITY -> 70 * 20;
            case HANBA_DOMAIN -> 90 * 20;
            case BLOOD_DROUGHT -> 24 * 20;
            case ELEMENT_DOMAIN -> 60 * 20;
            case BA_FIRE_BURST -> 45 * 20;
            default -> 0;
        };
    }

    private static long requiredGrudge(String id) {
        return switch (id) {
            case BREATH -> 100L;
            case POUNCE, ROAR -> 500L;
            case CORRUPT_FIELD, DEVOUR_CORPSE -> 2_500L;
            case SHADOW_ESCAPE, BONE_GROWTH, FIRE_CLAWS, SUMMON -> 12_500L;
            case MASS_DEVOUR -> 62_500L;
            case FUSHI_DOMAIN, LIFE_DRAIN, SHADOW_COPIES, EARTH_CORRUPTION -> 312_500L;
            case IMMORTAL_BONES, BONE_PRISON, BONE_RESHAPE, BONE_UNITY -> 1_562_500L;
            case HANBA_DOMAIN, BLOOD_DROUGHT, ELEMENT_DOMAIN, BA_FIRE_BURST -> 7_812_500L;
            default -> Long.MAX_VALUE;
        };
    }

    private static String normalizeAbilityId(String rawId) {
        if (rawId == null) {
            return "";
        }
        String id = rawId.trim().toLowerCase(Locale.ROOT);
        int namespace = id.indexOf(':');
        return namespace >= 0 ? id.substring(namespace + 1) : id;
    }

    private static LivingEntity findPlayerTarget(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        LivingEntity closest = null;
        double closestDistanceSqr = Double.MAX_VALUE;
        for (Entity entity : player.serverLevel().getAllEntities()) {
            if (!(entity instanceof LivingEntity living)
                    || !entity.isPickable()
                    || entity.isSpectator()
                    || !validEnemy(player, living)
                    || !player.hasLineOfSight(living)) {
                continue;
            }
            AABB hitbox = entity.getBoundingBox().inflate(entity.getPickRadius() + 0.25D);
            Vec3 toCenter = hitbox.getCenter().subtract(eye);
            double alongRay = toCenter.dot(look);
            if (alongRay <= 0.0D || alongRay * alongRay >= closestDistanceSqr) {
                continue;
            }
            Vec3 rayEnd = eye.add(look.scale(alongRay + entity.getBbWidth()
                    + entity.getBbHeight() + 1.0D));
            var intersection = hitbox.clip(eye, rayEnd);
            if (intersection.isEmpty()) {
                continue;
            }
            double distanceSqr = eye.distanceToSqr(intersection.get());
            if (distanceSqr < closestDistanceSqr) {
                closestDistanceSqr = distanceSqr;
                closest = living;
            }
        }
        return closest;
    }

    private static boolean beginPlayerCast(ServerPlayer player, String id, int castTicks,
                                           LivingEntity target, long now) {
        int cooldown = playerAbilityCooldownTicks(id);
        if (!ABILITY_IDS.contains(id)) {
            return false;
        }
        startCast(player, id, castTicks, target, now, cooldown);
        return true;
    }

    public static boolean castPlayerAbility(ServerPlayer player, String rawId) {
        String id = normalizeAbilityId(rawId);
        if (!canPlayerUseAbility(player, id)
                || UndeadControl.isSealed(player)
                || !player.isAlive()
                || !player.getPersistentData().getString(TAG_CAST).isBlank()) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (CORRUPT_FIELD.equals(id)
                && player.getPersistentData().getBoolean(TAG_CORRUPT_FIELD_TOGGLE)) {
            stopDomain(player, CORRUPT_FIELD);
            player.getPersistentData().remove(TAG_CORRUPT_FIELD_TOGGLE);
            return true;
        }
        if (!ready(player, id, now)) {
            return false;
        }

        LivingEntity target = findPlayerTarget(player);
        return switch (id) {
            case BREATH -> beginPlayerCast(player, id, 12, null, now);
            case POUNCE -> target != null && beginPlayerCast(player, id, 7, target, now);
            case ROAR -> beginPlayerCast(player, id, 16, null, now);
            case CORRUPT_FIELD -> beginPlayerCast(player, id, 20, null, now);
            case DEVOUR_CORPSE -> corpseCount(level, player.position(), 8.0D) > 0
                    && beginPlayerCast(player, id, 30, null, now);
            case SHADOW_ESCAPE -> {
                castShadowEscape(level, player, target, now);
                yield true;
            }
            case BONE_GROWTH -> beginPlayerCast(player, id, 20, null, now);
            case FIRE_CLAWS -> {
                activateFireClaws(level, player, now);
                yield true;
            }
            case SUMMON -> target != null && beginPlayerCast(player, id, 40, target, now);
            case MASS_DEVOUR -> corpseCount(level, player.position(), 16.0D) > 0
                    && beginPlayerCast(player, id, 40, null, now);
            case FUSHI_DOMAIN -> beginPlayerCast(player, id, 40, null, now);
            case LIFE_DRAIN -> target != null && beginPlayerCast(player, id, 60, target, now);
            case SHADOW_COPIES -> {
                if (target == null) {
                    yield false;
                }
                castShadowCopies(level, player, target, now);
                yield true;
            }
            case EARTH_CORRUPTION -> beginPlayerCast(player, id, 20, null, now);
            case IMMORTAL_BONES -> {
                activateImmortalBones(level, player, now);
                yield true;
            }
            case BONE_PRISON -> target != null && beginPlayerCast(player, id, 24, target, now);
            case BONE_RESHAPE -> player.getHealth() <= player.getMaxHealth() * 0.50F
                    && beginPlayerCast(player, id, 50, null, now);
            case BONE_UNITY -> corpseCount(level, player.position(), 24.0D) > 0
                    && beginPlayerCast(player, id, 32, null, now);
            case HANBA_DOMAIN -> UndeadControl.isDroughtDomainEnabled(player)
                    && beginPlayerCast(player, id, 60, null, now);
            case BLOOD_DROUGHT -> beginPlayerCast(player, id, 8, null, now);
            case ELEMENT_DOMAIN -> beginPlayerCast(player, id, 20, null, now);
            case BA_FIRE_BURST -> target != null && beginPlayerCast(player, id, 40, target, now);
            default -> false;
        };
    }

    /** Called for every living entity so victim-side constraints keep working. */
    public static void tickEntityState(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        CompoundTag tag = entity.getPersistentData();

        if (tag.getLong(TAG_CAGE_UNTIL) > now) {
            enforceBoneCage(level, entity, tag);
        }
        if (tag.getLong(TAG_BONE_DELAYED_UNTIL) > now
                && tag.getFloat(TAG_BONE_DELAYED_DAMAGE) > 0.0F
                && now >= tag.getLong(TAG_BONE_DELAYED_NEXT)) {
            float remaining = tag.getFloat(TAG_BONE_DELAYED_DAMAGE);
            long ticksLeft = Math.max(1L, tag.getLong(TAG_BONE_DELAYED_UNTIL) - now);
            float pulse = Math.min(remaining, remaining / Math.max(1.0F, ticksLeft / 20.0F));
            tag.putFloat(TAG_BONE_DELAYED_DAMAGE, Math.max(0.0F, remaining - pulse));
            tag.putLong(TAG_BONE_DELAYED_NEXT, now + 20L);
            entity.hurt(entity.damageSources().magic(), pulse);
            level.sendParticles(BONE_WHITE, entity.getX(), entity.getY() + entity.getBbHeight() * 0.55D,
                    entity.getZ(), 12, 0.3D, 0.5D, 0.3D, 0.01D);
        }
        if (entity.hasEffect(ModEffects.SHIDU) && entity.tickCount % 8 == 0) {
            level.sendParticles(CORPSE_GREEN, entity.getX(), entity.getY() + entity.getBbHeight() * 0.65D,
                    entity.getZ(), 2, 0.2D, 0.25D, 0.2D, 0.005D);
        }
    }

    /** Called from UndeadControl after rank stats have been applied. */
    public static void tickUndead(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)
                || UndeadControl.isSealed(entity)) {
            return;
        }
        long now = level.getGameTime();
        CompoundTag tag = entity.getPersistentData();

        if (tag.getBoolean(TAG_COPY)) {
            tickCopy(level, entity, now);
            return;
        }
        if (tag.getBoolean(TAG_JIANGSHEN_VFX_ACTIVE)
                && tag.getLong(TAG_JIANGSHEN_UNTIL) <= now) {
            tag.remove(TAG_JIANGSHEN_VFX_ACTIVE);
            UndeadAbilityVfx.hardenedRelease(level, entity);
        }
        if (tag.getLong(TAG_SUMMON_EXPIRES) > 0L && now >= tag.getLong(TAG_SUMMON_EXPIRES)) {
            ashDisappear(level, entity);
            entity.discard();
            return;
        }

        tickOngoing(level, entity, now);
        if (tickCast(level, entity, now)) {
            return;
        }
        if (tag.getLong(TAG_JIANGSHEN_UNTIL) > now) {
            freeze(entity);
            jiangshenParticles(level, entity);
            return;
        }
        if (entity instanceof Player) {
            return;
        }
        if (!(entity instanceof Mob mob)) {
            return;
        }
        LivingEntity target = combatTarget(mob);
        if (target == null) {
            tag.remove(TAG_COMBAT_START);
            return;
        }
        tag.putLong(TAG_COMBAT_START, tag.contains(TAG_COMBAT_START)
                ? tag.getLong(TAG_COMBAT_START) : now);
        if ((entity.tickCount + entity.getId()) % 10 == 0) {
            chooseAbility(level, mob, target, now);
        }
    }

    public static float adjustIncomingDamage(
            LivingEntity target, DamageSource source, float amount) {
        if (amount <= 0.0F || !UndeadControl.hasUndeadPowers(target)) {
            return amount;
        }
        long now = target.level().getGameTime();
        CompoundTag tag = target.getPersistentData();
        if (isDangerousIncoming(target, source, amount)) {
            tag.putLong(TAG_LAST_MAJOR_STAGGER, now);
            String cast = tag.getString(TAG_CAST);
            if (DEVOUR_CORPSE.equals(cast) || MASS_DEVOUR.equals(cast)
                    || LIFE_DRAIN.equals(cast) || BONE_RESHAPE.equals(cast)) {
                clearCast(tag);
            }
        }
        if (tag.getLong(TAG_TRAVEL_INVULN_UNTIL) > now || tag.getBoolean(TAG_COPY)) {
            if (tag.getBoolean(TAG_COPY) && target.level() instanceof ServerLevel level) {
                ashDisappear(level, target);
                target.discard();
            }
            return 0.0F;
        }
        if (tag.getLong(TAG_IMMORTAL_BONES_UNTIL) > now) {
            float prevented = amount * 0.70F;
            tag.putFloat(TAG_BONE_DELAYED_DAMAGE,
                    tag.getFloat(TAG_BONE_DELAYED_DAMAGE) + prevented * 0.40F);
            if (target.level() instanceof ServerLevel level) {
                UndeadAbilityVfx.immortalBoneImpact(level, target, source.getSourcePosition());
            }
            return amount - prevented;
        }
        if (tag.getLong(TAG_JIANGSHEN_UNTIL) > now) {
            if (target.level() instanceof ServerLevel level) {
                UndeadAbilityVfx.hardenedImpact(level, target, source.getSourcePosition());
            }
            return amount * 0.40F;
        }
        if (UndeadControl.isAtLeastRank(target, "白僵")
                && ready(target, "jiangshen", now)
                && isDangerousIncoming(target, source, amount)) {
            setCooldown(target, "jiangshen", now, 14 * 20L);
            tag.putLong(TAG_JIANGSHEN_UNTIL, now + 24L);
            tag.putLong(TAG_JIANGSHEN_COUNTER_UNTIL, now + 64L);
            tag.putBoolean(TAG_JIANGSHEN_VFX_ACTIVE, true);
            clearCast(tag);
            if (target.level() instanceof ServerLevel level) {
                jiangshenStartParticles(level, target);
            }
            return amount * 0.40F;
        }
        if (tag.getLong(TAG_BONE_GROWTH_UNTIL) > now) {
            amount *= 0.85F;
            amount = absorbBoneGrowthShield(target, amount);
            if (target.level() instanceof ServerLevel level) {
                UndeadAbilityVfx.boneGrowthImpact(level, target);
            }
        }
        if (tag.getLong(TAG_BONE_UNITY_UNTIL) > now) {
            amount *= 1.0F - Mth.clamp(tag.getFloat(TAG_BONE_UNITY_ARMOR), 0.0F, 0.75F);
        }
        return amount;
    }

    public static float adjustOutgoingDamage(
            LivingEntity attacker, LivingEntity target, DamageSource source, float amount) {
        if (amount <= 0.0F || !UndeadControl.hasUndeadPowers(attacker)) {
            return amount;
        }
        CompoundTag tag = attacker.getPersistentData();
        long now = attacker.level().getGameTime();
        // Channelled abilities mark their own damage so the casting lock cannot cancel it.
        if (tag.getBoolean(TAG_ABILITY_DAMAGE)) {
            return amount;
        }
        if (tag.getLong(TAG_JIANGSHEN_UNTIL) > now || !tag.getString(TAG_CAST).isBlank()) {
            return 0.0F;
        }
        if (!isDirectMelee(source, attacker)) {
            return amount;
        }
        if (tag.getLong(TAG_JIANGSHEN_COUNTER_UNTIL) > now) {
            amount *= 1.50F;
            tag.remove(TAG_JIANGSHEN_COUNTER_UNTIL);
            target.addEffect(new MobEffectInstance(ModEffects.DINGSHEN, 10, 0, false, true, true), attacker);
        }
        if (tag.getLong(TAG_BONE_GROWTH_UNTIL) > now) {
            amount *= 1.25F;
            Vec3 away = target.position().subtract(attacker.position());
            if (away.horizontalDistanceSqr() > 0.0001D) {
                Vec3 direction = away.normalize();
                target.push(direction.x * 0.35D, 0.08D, direction.z * 0.35D);
                target.hurtMarked = true;
            }
        }
        if (tag.getLong(TAG_DOMAIN_DAMAGE_UNTIL) > now) {
            amount *= Math.max(1.0F, tag.getFloat(TAG_DOMAIN_DAMAGE_MULTIPLIER));
        }
        if (tag.getLong(TAG_FIRE_CLAWS_UNTIL) > now) {
            amount += 120.0F;
            target.addEffect(new MobEffectInstance(ModEffects.CORPSE_FIRE, 4 * 20, 0,
                    false, true, true), attacker);
            if (attacker.level() instanceof ServerLevel level) {
                UndeadAbilityVfx.fireClawHit(level, attacker, target);
            }
        }
        return amount;
    }

    private static float absorbBoneGrowthShield(LivingEntity target, float damage) {
        if (damage <= 0.0F) {
            return damage;
        }
        CompoundTag tag = target.getPersistentData();
        float shield = Math.max(0.0F, tag.getFloat(TAG_BONE_GROWTH_SHIELD));
        if (shield <= 0.0F) {
            return damage;
        }
        float absorbed = Math.min(damage, shield);
        float remaining = shield - absorbed;
        if (remaining > 0.0F) {
            tag.putFloat(TAG_BONE_GROWTH_SHIELD, remaining);
        } else {
            tag.remove(TAG_BONE_GROWTH_SHIELD);
        }
        syncBoneGrowthShield(target);
        return damage - absorbed;
    }

    private static void syncBoneGrowthShield(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        CompoundTag tag = entity.getPersistentData();
        float amount = Math.max(0.0F, tag.getFloat(TAG_BONE_GROWTH_SHIELD));
        float maximum = Math.max(amount, tag.getFloat(TAG_BONE_GROWTH_SHIELD_MAX));
        if (amount > 0.0F && maximum > 0.0F) {
            PlayerShieldManager.set(player, BONE_GROWTH_SHIELD_SOURCE, amount, maximum);
        } else {
            PlayerShieldManager.clear(player, BONE_GROWTH_SHIELD_SOURCE);
        }
    }

    public static boolean isDirectMelee(DamageSource source, LivingEntity attacker) {
        return source.getDirectEntity() == attacker
                && (source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.PLAYER_ATTACK));
    }

    public static boolean isUndeadQiMelee(DamageSource source) {
        if (!(source.getEntity() instanceof LivingEntity attacker)) {
            return false;
        }
        return UndeadControl.hasUndeadPowers(attacker)
                && UndeadControl.isAtLeastRank(attacker, "飞僵")
                && isDirectMelee(source, attacker);
    }

    public static float healingMultiplier(LivingEntity entity) {
        float multiplier = 1.0F;
        if (entity.hasEffect(ModEffects.SHIDU)) {
            multiplier = Math.min(multiplier, 0.85F);
        }
        if (entity.hasEffect(ModEffects.CORPSE_FIRE)) {
            multiplier = Math.min(multiplier, 0.70F);
        }
        if (entity.hasEffect(ModEffects.BA_FIRE)) {
            multiplier = Math.min(multiplier, 0.50F);
        }
        CompoundTag tag = entity.getPersistentData();
        if (tag.getLong(TAG_HEAL_PENALTY_UNTIL) > entity.level().getGameTime()) {
            multiplier = Math.min(multiplier, 1.0F - tag.getFloat(TAG_HEAL_PENALTY));
        } else {
            tag.remove(TAG_HEAL_PENALTY);
            tag.remove(TAG_HEAL_PENALTY_UNTIL);
        }
        return Mth.clamp(multiplier, 0.0F, 1.0F);
    }

    public static int adjustQiRegen(LivingEntity entity, int amount) {
        float multiplier = 1.0F;
        if (entity.hasEffect(ModEffects.SHIDU)) {
            multiplier *= 0.90F;
        }
        if (entity.hasEffect(ModEffects.MERIDIAN_CORPSE_STAGNATION)) {
            multiplier *= 0.70F;
        }
        CompoundTag tag = entity.getPersistentData();
        if (tag.getLong(TAG_QI_PENALTY_UNTIL) > entity.level().getGameTime()) {
            multiplier *= 1.0F - tag.getFloat(TAG_QI_PENALTY);
        } else {
            tag.remove(TAG_QI_PENALTY);
            tag.remove(TAG_QI_PENALTY_UNTIL);
        }
        return Math.max(0, Mth.floor(amount * multiplier));
    }

    public static float spellChargeSpeedMultiplier(LivingEntity entity) {
        float multiplier = 1.0F;
        if (entity.hasEffect(ModEffects.JINGSHI)) {
            multiplier *= 0.85F;
        }
        if (entity.hasEffect(ModEffects.MERIDIAN_CORPSE_STAGNATION)) {
            multiplier *= 0.85F;
        }
        if (entity.hasEffect(ModEffects.CORPSE_MIGHT)) {
            multiplier *= 0.75F;
        }
        return multiplier;
    }

    public static void adjustKnockback(LivingEntity entity,
                                       net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent event) {
        long now = entity.level().getGameTime();
        if (entity.getPersistentData().getLong(TAG_JIANGSHEN_UNTIL) > now) {
            event.setStrength(0.0F);
        } else if (entity.getPersistentData().getLong(TAG_POUNCE_UNTIL) > now) {
            event.setStrength(event.getStrength() * 0.50F);
        }
    }

    public static boolean suppressDrops(LivingEntity entity) {
        return entity.getPersistentData().getBoolean(TAG_NO_LOOT)
                || entity.getPersistentData().getBoolean(TAG_COPY);
    }

    private static void chooseAbility(ServerLevel level, Mob mob, LivingEntity target, long now) {
        double distance = mob.distanceTo(target);
        int nearbyEnemies = enemies(level, mob, 10.0D).size();
        long combatTicks = now - mob.getPersistentData().getLong(TAG_COMBAT_START);

        if (UndeadControl.isAtLeastRank(mob, "旱魃")) {
            if (distance <= 12.0D && ready(mob, BLOOD_DROUGHT, now)
                    && (mob.getHealth() < mob.getMaxHealth() * 0.75F || mob.getRandom().nextFloat() < 0.35F)) {
                startCast(mob, BLOOD_DROUGHT, 8, target, now, 24 * 20L);
                return;
            }
            if (distance <= 30.0D && ready(mob, BA_FIRE_BURST, now)) {
                startCast(mob, BA_FIRE_BURST, 40, target, now, 45 * 20L);
                return;
            }
            if (UndeadControl.isDroughtDomainEnabled(mob)
                    && ready(mob, HANBA_DOMAIN, now)
                    && (nearbyEnemies >= 2 || combatTicks >= 100L)) {
                startCast(mob, HANBA_DOMAIN, 60, target, now, 90 * 20L);
                return;
            }
            if (ready(mob, ELEMENT_DOMAIN, now) && distance <= 30.0D) {
                startCast(mob, ELEMENT_DOMAIN, 20, target, now, 60 * 20L);
                return;
            }
        }

        if (UndeadControl.isAtLeastRank(mob, "不化骨")) {
            if (mob.getHealth() <= mob.getMaxHealth() * 0.50F && ready(mob, BONE_RESHAPE, now)) {
                startCast(mob, BONE_RESHAPE, 50, target, now, 60 * 20L);
                return;
            }
            if (mob.getHealth() <= mob.getMaxHealth() * 0.72F && ready(mob, BONE_UNITY, now)
                    && corpseCount(level, mob.position(), 24.0D) > 0) {
                startCast(mob, BONE_UNITY, 32, target, now, 70 * 20L);
                return;
            }
            if (distance <= 18.0D && ready(mob, BONE_PRISON, now)) {
                startCast(mob, BONE_PRISON, 24, target, now, 28 * 20L);
                return;
            }
            if (mob.getHealth() <= mob.getMaxHealth() * 0.70F && ready(mob, IMMORTAL_BONES, now)) {
                activateImmortalBones(level, mob, now);
                return;
            }
        }

        if (UndeadControl.isAtLeastRank(mob, "伏尸")) {
            if (distance >= 4.0D && distance <= 14.0D && ready(mob, LIFE_DRAIN, now)) {
                startCast(mob, LIFE_DRAIN, 60, target, now, 20 * 20L);
                return;
            }
            if (ready(mob, FUSHI_DOMAIN, now) && (nearbyEnemies >= 2 || combatTicks >= 100L)) {
                startCast(mob, FUSHI_DOMAIN, 40, target, now, 45 * 20L);
                return;
            }
            if (ready(mob, EARTH_CORRUPTION, now) && combatTicks >= 140L) {
                startCast(mob, EARTH_CORRUPTION, 20, target, now, 50 * 20L);
                return;
            }
            if (mob.getHealth() <= mob.getMaxHealth() * 0.65F && ready(mob, SHADOW_COPIES, now)) {
                castShadowCopies(level, mob, target, now);
                return;
            }
        }

        if (UndeadControl.isAtLeastRank(mob, "游尸")) {
            if (mob.getHealth() <= mob.getMaxHealth() * 0.60F && ready(mob, MASS_DEVOUR, now)
                    && corpseCount(level, mob.position(), 16.0D) > 0) {
                startCast(mob, MASS_DEVOUR, 40, target, now, 40 * 20L);
                return;
            }
        }

        if (UndeadControl.isAtLeastRank(mob, "飞僵")) {
            if (mob.getHealth() <= mob.getMaxHealth() * 0.60F && ready(mob, SUMMON, now)
                    && !mob.getPersistentData().getBoolean(TAG_NO_SUMMON)) {
                startCast(mob, SUMMON, 40, target, now, 35 * 20L);
                return;
            }
            if (distance <= 3.5D && ready(mob, FIRE_CLAWS, now)) {
                activateFireClaws(level, mob, now);
                return;
            }
            if (distance <= 5.0D && mob.getHealth() <= mob.getMaxHealth() * 0.70F
                    && ready(mob, BONE_GROWTH, now)) {
                startCast(mob, BONE_GROWTH, 20, target, now, 24 * 20L);
                return;
            }
            if ((distance > 8.0D || nearbyEnemies >= 3) && ready(mob, SHADOW_ESCAPE, now)) {
                castShadowEscape(level, mob, target, now);
                return;
            }
        }

        if (UndeadControl.isAtLeastRank(mob, "跳僵")) {
            if (mob.getHealth() <= mob.getMaxHealth() * 0.50F && ready(mob, DEVOUR_CORPSE, now)
                    && corpseCount(level, mob.position(), 8.0D) > 0) {
                startCast(mob, DEVOUR_CORPSE, 30, target, now, 25 * 20L);
                return;
            }
            if (combatTicks >= 120L && distance <= 6.0D
                    && ready(mob, CORRUPT_FIELD, now)
                    && mob.getPersistentData().getLong(domainKey(CORRUPT_FIELD, "until")) <= now) {
                startCast(mob, CORRUPT_FIELD, 20, target, now, 0L);
                return;
            }
        }

        if (UndeadControl.isAtLeastRank(mob, "黑僵")) {
            boolean charging = target instanceof ServerPlayer player
                    && CombatStateManager.isPreparingSpell(player);
            if (distance <= 6.0D && ready(mob, ROAR, now) && (nearbyEnemies >= 2 || charging)) {
                startCast(mob, ROAR, 16, target, now, 13 * 20L);
                return;
            }
            if (distance >= 5.0D && distance <= 9.0D && ready(mob, POUNCE, now)) {
                startCast(mob, POUNCE, 7, target, now, 7 * 20L);
                return;
            }
        }

        if (UndeadControl.isAtLeastRank(mob, "白僵") && distance >= 2.0D && distance <= 5.0D
                && mob.hasLineOfSight(target) && ready(mob, BREATH, now)) {
            startCast(mob, BREATH, 12, target, now, 8 * 20L);
        }
    }

    private static void tickOngoing(ServerLevel level, LivingEntity entity, long now) {
        CompoundTag tag = entity.getPersistentData();
        tickPounce(level, entity, now, tag);
        tickHealingOverTime(level, entity, now, tag);
        tickTimedBuffs(level, entity, now, tag);
        tickDomain(level, entity, CORRUPT_FIELD, 5.0D, now);
        tickDomain(level, entity, FUSHI_DOMAIN, 18.0D, now);
        tickDomain(level, entity, EARTH_CORRUPTION, 20.0D, now);
        tickDomain(level, entity, HANBA_DOMAIN, 40.0D, now);
        tickDomain(level, entity, BLOOD_DROUGHT, 12.0D, now);
        tickDomain(level, entity, ELEMENT_DOMAIN, 30.0D, now);
    }

    private static boolean tickCast(ServerLevel level, LivingEntity entity, long now) {
        CompoundTag tag = entity.getPersistentData();
        String cast = tag.getString(TAG_CAST);
        if (cast.isBlank()) {
            return false;
        }
        if (!ABILITY_IDS.contains(cast)) {
            clearCast(tag);
            return false;
        }
        LivingEntity target = castTarget(level, tag);
        if (requiresLivingTarget(cast) && !validEnemy(entity, target)) {
            clearCast(tag);
            return false;
        }
        if (LIFE_DRAIN.equals(cast)) {
            if (target == null || !entity.hasLineOfSight(target)
                    || tag.getLong(TAG_LAST_MAJOR_STAGGER) > 0L
                    && tag.getLong(TAG_LAST_MAJOR_STAGGER) + 10L >= now) {
                clearCast(tag);
                return false;
            }
            if (now >= tag.getLong(TAG_CAST_NEXT_PULSE)) {
                if (abilityDamage(entity, target, 350.0F)) {
                    entity.heal(175.0F);
                    markPenalty(target, 0.30F, 0.0F, now + 5 * 20L);
                }
                drainStreamParticles(level, target, entity);
                tag.putLong(TAG_CAST_NEXT_PULSE, now + 20L);
            }
        }
        if ((DEVOUR_CORPSE.equals(cast) || MASS_DEVOUR.equals(cast) || BONE_UNITY.equals(cast))
                && entity.tickCount % 4 == 0) {
            double radius = DEVOUR_CORPSE.equals(cast) ? 8.0D
                    : BONE_UNITY.equals(cast) ? 24.0D : 16.0D;
            List<CultivatorCorpseEntity> corpses = usableCorpses(level, entity.position(), radius);
            int maximum = DEVOUR_CORPSE.equals(cast) ? corpses.size()
                    : BONE_UNITY.equals(cast) ? 12 : 8;
            if (corpses.size() > maximum) {
                corpses = new ArrayList<>(corpses.subList(0, maximum));
            }
            UndeadAbilityVfx.corpseChannel(level, corpses, entity, BONE_UNITY.equals(cast));
        }
        freeze(entity);
        castTelegraph(level, entity, target, cast, tag.getLong(TAG_CAST_END) - now);
        if (now < tag.getLong(TAG_CAST_END)) {
            return true;
        }
        clearCast(tag);
        resolveCast(level, entity, target, cast, now);
        return true;
    }

    private static void startCast(LivingEntity caster, String id, int castTicks,
                                  LivingEntity target, long now, long cooldownTicks) {
        CompoundTag tag = caster.getPersistentData();
        tag.putString(TAG_CAST, id);
        tag.putLong(TAG_CAST_END, now + castTicks);
        tag.putLong(TAG_CAST_NEXT_PULSE, now + 20L);
        if (target != null) {
            tag.putUUID(TAG_CAST_TARGET, target.getUUID());
        } else {
            tag.remove(TAG_CAST_TARGET);
        }
        setCooldown(caster, id, now, cooldownTicks);
        if (caster instanceof Mob mob) {
            mob.getNavigation().stop();
        }
    }

    private static void clearCast(CompoundTag tag) {
        tag.remove(TAG_CAST);
        tag.remove(TAG_CAST_END);
        tag.remove(TAG_CAST_TARGET);
        tag.remove(TAG_CAST_NEXT_PULSE);
    }

    private static LivingEntity castTarget(ServerLevel level, CompoundTag tag) {
        if (!tag.hasUUID(TAG_CAST_TARGET)) {
            return null;
        }
        Entity entity = level.getEntity(tag.getUUID(TAG_CAST_TARGET));
        return entity instanceof LivingEntity living ? living : null;
    }

    private static boolean requiresLivingTarget(String cast) {
        return POUNCE.equals(cast) || LIFE_DRAIN.equals(cast) || BONE_PRISON.equals(cast)
                || BA_FIRE_BURST.equals(cast);
    }

    private static void resolveCast(ServerLevel level, LivingEntity caster,
                                    LivingEntity target, String cast, long now) {
        switch (cast) {
            case BREATH -> castCorpseBreath(level, caster);
            case POUNCE -> beginPounce(level, caster, target, now);
            case ROAR -> castCorpseRoar(level, caster, 6.0D, 45.0F, false, now);
            case CORRUPT_FIELD -> {
                abilityDamageArea(level, caster, caster.position(), 5.0D, 60.0F, false);
                if (caster instanceof ServerPlayer) {
                    caster.getPersistentData().putBoolean(TAG_CORRUPT_FIELD_TOGGLE, true);
                    startDomain(caster, CORRUPT_FIELD, caster.position(), Long.MAX_VALUE);
                } else {
                    startDomain(caster, CORRUPT_FIELD, caster.position(), now + 8 * 20L);
                }
            }
            case DEVOUR_CORPSE -> consumeCorpsesInRadius(level, caster, now);
            case BONE_GROWTH -> activateBoneGrowth(level, caster, now);
            case SUMMON -> summonZombies(level, caster, target, now);
            case MASS_DEVOUR -> consumeManyCorpses(level, caster, 16.0D, 8, 0.03F, now, false);
            case FUSHI_DOMAIN -> {
                abilityDamageArea(level, caster, caster.position(), 18.0D, 1_400.0F, false);
                startDomain(caster, FUSHI_DOMAIN, caster.position(), now + 12 * 20L);
                temporarilyCorruptTerrain(level, caster, caster.position(), 18, FUSHI_DOMAIN, 12 * 20);
            }
            case EARTH_CORRUPTION -> {
                startDomain(caster, EARTH_CORRUPTION, caster.position(), now + 15 * 20L);
                temporarilyCorruptTerrain(level, caster, caster.position(), 20,
                        EARTH_CORRUPTION, 15 * 20);
            }
            case BONE_PRISON -> castBonePrison(level, caster, target, now);
            case BONE_RESHAPE -> castBoneReshape(level, caster);
            case BONE_UNITY -> consumeManyCorpses(level, caster, 24.0D, 12, 0.04F, now, true);
            case HANBA_DOMAIN -> {
                if (UndeadControl.isDroughtDomainEnabled(caster)) {
                    abilityDamageArea(level, caster, caster.position(), 40.0D,
                            10_000_000.0F, false);
                    startDomain(caster, HANBA_DOMAIN, caster.position(), now + 20 * 20L);
                }
            }
            case BLOOD_DROUGHT -> startDomain(
                    caster, BLOOD_DROUGHT, caster.position(), now + 6 * 20L);
            case ELEMENT_DOMAIN -> startDomain(
                    caster, ELEMENT_DOMAIN, caster.position(), now + 15 * 20L);
            case BA_FIRE_BURST -> castBaFire(level, caster, target, now);
            default -> {
            }
        }
    }

    private static void castTelegraph(ServerLevel level, LivingEntity caster,
                                      LivingEntity target, String cast, long ticksLeft) {
        UndeadAbilityVfx.castTelegraph(level, caster, target, cast, ticksLeft);
    }

    private static boolean abilityDamage(LivingEntity caster, LivingEntity target, float damage) {
        if (!validEnemy(caster, target) || damage <= 0.0F) {
            return false;
        }
        CompoundTag tag = caster.getPersistentData();
        tag.putBoolean(TAG_ABILITY_DAMAGE, true);
        try {
            return target.hurt(caster.damageSources().indirectMagic(caster, caster), damage);
        } finally {
            tag.remove(TAG_ABILITY_DAMAGE);
        }
    }

    private static void castCorpseBreath(ServerLevel level, LivingEntity caster) {
        Vec3 origin = caster.getEyePosition().add(0.0D, -0.18D, 0.0D);
        Vec3 forward = caster.getLookAngle().normalize();
        for (LivingEntity target : coneTargets(level, caster, origin, forward, 5.0D, 70.0D)) {
            if (abilityDamage(caster, target, 18.0F)) {
                target.addEffect(new MobEffectInstance(ModEffects.SHIDU, 5 * 20, 0,
                        false, true, true), caster);
                UndeadAbilityVfx.corpseBreathHit(level, target, caster);
            }
        }
        UndeadAbilityVfx.corpseBreath(level, caster);
    }

    private static void beginPounce(ServerLevel level, LivingEntity caster,
                                    LivingEntity target, long now) {
        if (target == null) {
            return;
        }
        Vec3 direction = target.getEyePosition().subtract(caster.position()).normalize();
        caster.setDeltaMovement(direction.x * 1.15D, Math.max(0.28D, direction.y * 0.55D + 0.22D),
                direction.z * 1.15D);
        com.example.immortal_cultivation_mod.combat.CombatFeedbackEvents.dashStarted(caster, direction, .5F, 16);
        caster.hurtMarked = true;
        CompoundTag tag = caster.getPersistentData();
        long travelTicks = caster instanceof ServerPlayer
                ? Math.max(10L, Mth.ceil(caster.distanceTo(target) / 0.70D) + 6L)
                : 10L;
        tag.putLong(TAG_POUNCE_UNTIL, now + travelTicks);
        tag.putUUID(TAG_POUNCE_TARGET, target.getUUID());
        tag.putBoolean(TAG_POUNCE_HIT, false);
        UndeadAbilityVfx.pounceStart(level, caster);
    }

    private static void castCorpseRoar(ServerLevel level, LivingEntity caster,
                                       double radius, float damage, boolean king, long now) {
        for (LivingEntity target : enemies(level, caster, radius)) {
            if (!abilityDamage(caster, target, damage)) {
                continue;
            }
            Vec3 away = target.position().subtract(caster.position()).normalize();
            target.push(away.x * (king ? 1.0D : 0.65D), 0.22D, away.z * (king ? 1.0D : 0.65D));
            target.hurtMarked = true;
            if (target instanceof ServerPlayer player) {
                CombatStateManager.interruptSpellPreparation(player);
            }
            target.addEffect(new MobEffectInstance(king ? ModEffects.CORPSE_MIGHT : ModEffects.JINGSHI,
                    king ? 5 * 20 : 3 * 20, 0, false, true, true), caster);
            target.addEffect(new MobEffectInstance(ModEffects.DAZE, king ? 12 : 8,
                    0, false, false, true), caster);
        }
        if (!king) {
            for (Mob ally : level.getEntitiesOfClass(Mob.class, caster.getBoundingBox().inflate(20.0D),
                    mob -> mob != caster && UndeadControl.hasUndeadPowers(mob))) {
                LivingEntity casterTarget = caster instanceof Mob mob ? mob.getTarget() : null;
                if (validEnemy(ally, casterTarget)) {
                    ally.setTarget(casterTarget);
                }
            }
        }
        UndeadAbilityVfx.roar(level, caster, radius, king);
    }

    private static void castShadowEscape(ServerLevel level, LivingEntity caster,
                                         LivingEntity target, long now) {
        Vec3 old = caster.position();
        Vec3 preferred = target == null
                ? caster.position().add(caster.getLookAngle().normalize().scale(10.0D))
                : target.position().subtract(target.getLookAngle().scale(2.0D));
        Vec3 destination = findTeleportDestination(level, caster, preferred, 10.0D);
        if (destination.distanceToSqr(old) < 0.01D) {
            setCooldown(caster, SHADOW_ESCAPE, now, 20L);
            return;
        }
        UndeadAbilityVfx.shadowTrail(level, old, destination);
        caster.teleportTo(destination.x, destination.y, destination.z);
        com.example.immortal_cultivation_mod.combat.CombatFeedbackEvents.spatialTravel(caster, old, destination, .65F);
        caster.getPersistentData().putLong(TAG_TRAVEL_INVULN_UNTIL, now + 5L);
        caster.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        setCooldown(caster, SHADOW_ESCAPE, now, 12 * 20L);
    }

    private static void activateBoneGrowth(ServerLevel level, LivingEntity caster, long now) {
        CompoundTag tag = caster.getPersistentData();
        tag.putLong(TAG_BONE_GROWTH_UNTIL, now + 8 * 20L);
        tag.putBoolean(TAG_BONE_GROWTH_PAID, false);
        float shield = Math.max(1.0F, caster.getMaxHealth() * 0.15F);
        tag.putFloat(TAG_BONE_GROWTH_SHIELD, shield);
        tag.putFloat(TAG_BONE_GROWTH_SHIELD_MAX, shield);
        syncBoneGrowthShield(caster);
        updateModifier(caster, Attributes.ENTITY_INTERACTION_RANGE,
                BONE_GROWTH_REACH_MODIFIER, 0.75D,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE);
        updateModifier(caster, Attributes.MOVEMENT_SPEED,
                BONE_GROWTH_SPEED_MODIFIER, -0.10D,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        UndeadAbilityVfx.boneGrowth(level, caster);
    }

    private static void activateFireClaws(ServerLevel level, LivingEntity caster, long now) {
        caster.getPersistentData().putLong(TAG_FIRE_CLAWS_UNTIL, now + 10 * 20L);
        setCooldown(caster, FIRE_CLAWS, now, 17 * 20L);
        UndeadAbilityVfx.fireClaws(level, caster, true);
    }

    private static void summonZombies(ServerLevel level, LivingEntity caster,
                                      LivingEntity target, long now) {
        boolean elite = UndeadControl.grudge(caster) >= 40_000L && caster.getRandom().nextFloat() < 0.30F;
        String[] levels = {
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY,
                CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY,
                elite ? CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_LATE
                        : CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_LATE
        };
        List<Zombie> summons = new ArrayList<>(levels.length);
        for (int i = 0; i < levels.length; i++) {
            double angle = Math.PI * 2.0D * i / levels.length;
            Vec3 spawn = caster.position().add(Math.cos(angle) * 2.2D, 0.0D, Math.sin(angle) * 2.2D);
            Zombie zombie = EntityType.ZOMBIE.create(level);
            if (zombie == null) {
                continue;
            }
            zombie.moveTo(spawn.x, spawn.y, spawn.z, caster.getYRot(), 0.0F);
            UndeadControl.initializeNpcSummon(zombie, caster, levels[i], target);
            zombie.getPersistentData().putBoolean(TAG_NO_LOOT, true);
            zombie.getPersistentData().putBoolean(TAG_NO_SUMMON, true);
            zombie.getPersistentData().putLong(TAG_SUMMON_EXPIRES, now + 30 * 20L);
            level.addFreshEntity(zombie);
            summons.add(zombie);
            UndeadAbilityVfx.summonSeal(level, spawn, i);
        }
        UndeadAbilityVfx.summonConnection(level, caster, summons);
    }

    private static void consumeCorpsesInRadius(ServerLevel level, LivingEntity caster, long now) {
        List<CultivatorCorpseEntity> corpses = usableCorpses(level, caster.position(), 8.0D);
        if (corpses.isEmpty()) {
            return;
        }
        corpseStreams(level, corpses, caster, false);
        for (CultivatorCorpseEntity corpse : corpses) {
            corpse.discard();
        }
        caster.heal(caster.getMaxHealth() * 0.15F);
        CompoundTag tag = caster.getPersistentData();
        tag.putLong(TAG_HOT_UNTIL, now + 5 * 20L);
        tag.putLong(TAG_HOT_NEXT, now + 20L);
        tag.putFloat(TAG_HOT_AMOUNT, caster.getMaxHealth() * 0.02F);
        caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,
                6 * 20, 0, false, false, true));
        caster.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED,
                6 * 20, 0, false, false, true));
    }

    private static void consumeManyCorpses(ServerLevel level, LivingEntity caster, double radius,
                                           int maximum, float healFraction, long now,
                                           boolean boneUnity) {
        List<CultivatorCorpseEntity> corpses = usableCorpses(level, caster.position(), radius);
        if (corpses.size() > maximum) {
            corpses = new ArrayList<>(corpses.subList(0, maximum));
        }
        if (corpses.isEmpty()) {
            return;
        }
        corpseStreams(level, corpses, caster, boneUnity);
        int count = corpses.size();
        for (CultivatorCorpseEntity corpse : corpses) {
            corpse.discard();
        }
        caster.heal(caster.getMaxHealth() * healFraction * count);
        if (boneUnity) {
            CompoundTag tag = caster.getPersistentData();
            tag.putLong(TAG_BONE_UNITY_UNTIL, now + 10 * 20L);
            tag.putFloat(TAG_BONE_UNITY_ARMOR, Math.min(0.36F, count * 0.03F));
        }
    }

    private static void castShadowCopies(ServerLevel level, LivingEntity caster,
                                         LivingEntity target, long now) {
        setCooldown(caster, SHADOW_COPIES, now, 30 * 20L);
        List<Zombie> copies = new ArrayList<>(3);
        for (int i = 0; i < 3; i++) {
            double angle = Math.PI * 2.0D * i / 3.0D + caster.getRandom().nextDouble() * 0.35D;
            double offsetX = Math.cos(angle) * 1.8D;
            double offsetZ = Math.sin(angle) * 1.8D;
            Vec3 spawn = caster.position().add(offsetX, 0.0D, offsetZ);
            Zombie copy = EntityType.ZOMBIE.create(level);
            if (copy == null) {
                continue;
            }
            copy.moveTo(spawn.x, spawn.y, spawn.z, caster.getYRot(), caster.getXRot());
            UndeadControl.initializeNpcSummon(copy, caster, UndeadControl.levelFor(caster), target);
            copy.getPersistentData().putBoolean(TAG_COPY, true);
            copy.getPersistentData().putBoolean(TAG_NO_LOOT, true);
            copy.getPersistentData().putBoolean(TAG_NO_SUMMON, true);
            copy.getPersistentData().putLong(TAG_COPY_EXPIRES, now + 8 * 20L);
            copy.getPersistentData().putUUID(TAG_COPY_OWNER, caster.getUUID());
            copy.getPersistentData().putDouble(TAG_COPY_OFFSET_X, offsetX);
            copy.getPersistentData().putDouble(TAG_COPY_OFFSET_Z, offsetZ);
            copy.setHealth(1.0F);
            copy.setNoAi(true);
            level.addFreshEntity(copy);
            copies.add(copy);
            UndeadAbilityVfx.shadowCopy(level, spawn, copy.getBbHeight());
        }
        UndeadAbilityVfx.shadowCopy(level, caster.position(), caster.getBbHeight());
        if (!copies.isEmpty()) {
            for (Mob enemy : level.getEntitiesOfClass(Mob.class,
                    caster.getBoundingBox().inflate(18.0D), mob -> mob.getTarget() == caster)) {
                enemy.setTarget(copies.get(enemy.getRandom().nextInt(copies.size())));
            }
        }
    }

    private static void activateImmortalBones(ServerLevel level, LivingEntity caster, long now) {
        CompoundTag tag = caster.getPersistentData();
        tag.putLong(TAG_IMMORTAL_BONES_UNTIL, now + 6 * 20L);
        tag.putBoolean(TAG_IMMORTAL_BONES_VFX_ACTIVE, true);
        tag.putFloat(TAG_BONE_DELAYED_DAMAGE, 0.0F);
        setCooldown(caster, IMMORTAL_BONES, now, 40 * 20L);
        UndeadAbilityVfx.immortalBones(level, caster);
    }

    private static void castBonePrison(ServerLevel level, LivingEntity caster,
                                       LivingEntity target, long now) {
        if (target == null) {
            return;
        }
        Vec3 center = target.position();
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(5.0D), living -> validEnemy(caster, living))) {
            abilityDamage(caster, victim, 500_000.0F);
            CompoundTag tag = victim.getPersistentData();
            tag.putLong(TAG_CAGE_UNTIL, now + 3 * 20L);
            tag.putDouble(TAG_CAGE_X, center.x);
            tag.putDouble(TAG_CAGE_Y, center.y);
            tag.putDouble(TAG_CAGE_Z, center.z);
        }
        UndeadAbilityVfx.bonePrison(level, center);
    }

    private static void castBoneReshape(ServerLevel level, LivingEntity caster) {
        caster.heal(caster.getMaxHealth() * 0.35F);
        caster.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        caster.removeEffect(MobEffects.WEAKNESS);
        UndeadAbilityVfx.boneReshape(level, caster);
    }

    private static void castBaFire(ServerLevel level, LivingEntity caster,
                                   LivingEntity target, long now) {
        if (target == null) {
            return;
        }
        Vec3 center = target.position();
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(12.0D), living -> validEnemy(caster, living))) {
            double distance = victim.position().distanceTo(center);
            float damage = distance <= 4.0D ? 4_500_000.0F : 2_500_000.0F;
            if (abilityDamage(caster, victim, damage)) {
                victim.addEffect(new MobEffectInstance(ModEffects.BA_FIRE,
                        6 * 20, 0, false, true, true), caster);
                markPenalty(victim, 0.50F, 0.0F, now + 6 * 20L);
            }
        }
        UndeadAbilityVfx.baFire(level, center, 12.0D);
    }

    private static void tickPounce(ServerLevel level, LivingEntity caster,
                                   long now, CompoundTag tag) {
        long until = tag.getLong(TAG_POUNCE_UNTIL);
        if (until <= 0L) {
            return;
        }
        if (now > until) {
            tag.remove(TAG_POUNCE_UNTIL);
            tag.remove(TAG_POUNCE_TARGET);
            tag.remove(TAG_POUNCE_HIT);
            return;
        }
        UndeadAbilityVfx.pounceTrail(level, caster);
        if (!tag.hasUUID(TAG_POUNCE_TARGET) || tag.getBoolean(TAG_POUNCE_HIT)) {
            return;
        }
        Entity found = level.getEntity(tag.getUUID(TAG_POUNCE_TARGET));
        if (!(found instanceof LivingEntity target) || !validEnemy(caster, target)) {
            return;
        }
        Vec3 toward = target.getEyePosition().subtract(caster.getEyePosition());
        if (toward.lengthSqr() > 0.01D) {
            Vec3 direction = toward.normalize();
            Vec3 current = caster.getDeltaMovement();
            caster.setDeltaMovement(direction.x * 1.15D,
                    Math.max(current.y, direction.y * 0.45D), direction.z * 1.15D);
            caster.hurtMarked = true;
        }
        if (caster.getBoundingBox().inflate(0.55D).intersects(target.getBoundingBox())
                || caster.distanceTo(target) <= 1.55D) {
            tag.putBoolean(TAG_POUNCE_HIT, true);
            if (abilityDamage(caster, target, 45.0F)) {
                Vec3 away = target.position().subtract(caster.position()).normalize();
                target.push(away.x * 0.72D, 0.28D, away.z * 0.72D);
                target.addEffect(new MobEffectInstance(ModEffects.DAZE,
                        8, 0, false, false, true), caster);
                target.hurtMarked = true;
            }
            UndeadAbilityVfx.pounceImpact(level, target.position());
        }
    }

    private static void tickHealingOverTime(ServerLevel level, LivingEntity entity,
                                            long now, CompoundTag tag) {
        if (tag.getLong(TAG_HOT_UNTIL) <= now || now < tag.getLong(TAG_HOT_NEXT)) {
            return;
        }
        entity.heal(tag.getFloat(TAG_HOT_AMOUNT));
        tag.putLong(TAG_HOT_NEXT, now + 20L);
        UndeadAbilityVfx.healingPulse(level, entity);
    }

    private static void tickTimedBuffs(ServerLevel level, LivingEntity entity,
                                       long now, CompoundTag tag) {
        long boneGrowthUntil = tag.getLong(TAG_BONE_GROWTH_UNTIL);
        if (boneGrowthUntil > now) {
            updateModifier(entity, Attributes.ENTITY_INTERACTION_RANGE,
                    BONE_GROWTH_REACH_MODIFIER, 0.75D,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE);
            updateModifier(entity, Attributes.MOVEMENT_SPEED,
                    BONE_GROWTH_SPEED_MODIFIER, -0.10D,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            if (entity.tickCount % 4 == 0) {
                level.sendParticles(BONE_WHITE, entity.getX(),
                        entity.getY() + entity.getBbHeight() * 0.55D, entity.getZ(),
                        5, 0.38D, 0.55D, 0.38D, 0.01D);
            }
        } else if (boneGrowthUntil > 0L && !tag.getBoolean(TAG_BONE_GROWTH_PAID)) {
            tag.putBoolean(TAG_BONE_GROWTH_PAID, true);
            tag.remove(TAG_BONE_GROWTH_SHIELD);
            tag.remove(TAG_BONE_GROWTH_SHIELD_MAX);
            syncBoneGrowthShield(entity);
            updateModifier(entity, Attributes.ENTITY_INTERACTION_RANGE,
                    BONE_GROWTH_REACH_MODIFIER, 0.0D,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE);
            updateModifier(entity, Attributes.MOVEMENT_SPEED,
                    BONE_GROWTH_SPEED_MODIFIER, 0.0D,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            entity.setHealth(Math.max(1.0F, entity.getHealth() - entity.getMaxHealth() * 0.05F));
            UndeadAbilityVfx.boneGrowthEnd(level, entity);
        }
        if (boneGrowthUntil > now && entity instanceof ServerPlayer && entity.tickCount % 20 == 0) {
            syncBoneGrowthShield(entity);
        }
        if (tag.getLong(TAG_FIRE_CLAWS_UNTIL) > now && entity.tickCount % 3 == 0) {
            UndeadAbilityVfx.fireClaws(level, entity, false);
        }
        long immortalUntil = tag.getLong(TAG_IMMORTAL_BONES_UNTIL);
        if (immortalUntil > now && entity.tickCount % 4 == 0) {
            level.sendParticles(BONE_WHITE, entity.getX(),
                    entity.getY() + entity.getBbHeight() * 0.52D, entity.getZ(),
                    8, 0.35D, 0.65D, 0.35D, 0.005D);
        } else if (immortalUntil > 0L && immortalUntil <= now) {
            if (tag.getBoolean(TAG_IMMORTAL_BONES_VFX_ACTIVE)) {
                tag.remove(TAG_IMMORTAL_BONES_VFX_ACTIVE);
                UndeadAbilityVfx.immortalBonesEnd(level, entity);
            }
            if (tag.getFloat(TAG_BONE_DELAYED_DAMAGE) > 0.0F
                    && tag.getLong(TAG_BONE_DELAYED_UNTIL) <= now) {
                tag.putLong(TAG_BONE_DELAYED_UNTIL, now + 8 * 20L);
                tag.putLong(TAG_BONE_DELAYED_NEXT, now + 20L);
            }
            tag.remove(TAG_IMMORTAL_BONES_UNTIL);
        }
    }

    private static void startDomain(LivingEntity caster, String id, Vec3 center, long until) {
        CompoundTag tag = caster.getPersistentData();
        tag.putLong(domainKey(id, "until"), until);
        tag.putDouble(domainKey(id, "x"), center.x);
        tag.putDouble(domainKey(id, "y"), center.y);
        tag.putDouble(domainKey(id, "z"), center.z);
        tag.putLong(domainKey(id, "next"), caster.level().getGameTime());
    }

    private static void stopDomain(LivingEntity caster, String id) {
        CompoundTag tag = caster.getPersistentData();
        tag.remove(domainKey(id, "until"));
        tag.remove(domainKey(id, "x"));
        tag.remove(domainKey(id, "y"));
        tag.remove(domainKey(id, "z"));
        tag.remove(domainKey(id, "next"));
    }

    private static String domainKey(String id, String suffix) {
        return TAG_PREFIX + "domain." + id + "." + suffix;
    }

    private static void tickDomain(ServerLevel level, LivingEntity caster,
                                   String id, double radius, long now) {
        CompoundTag tag = caster.getPersistentData();
        long until = tag.getLong(domainKey(id, "until"));
        if (until <= now) {
            return;
        }
        if (HANBA_DOMAIN.equals(id) && !UndeadControl.isDroughtDomainEnabled(caster)) {
            stopDomain(caster, id);
            return;
        }
        Vec3 center;
        if (CORRUPT_FIELD.equals(id) && caster instanceof ServerPlayer player) {
            if (!tag.getBoolean(TAG_CORRUPT_FIELD_TOGGLE)
                    || !canPlayerUseAbility(player, CORRUPT_FIELD)) {
                tag.remove(TAG_CORRUPT_FIELD_TOGGLE);
                stopDomain(caster, id);
                return;
            }
            center = caster.position();
            tag.putDouble(domainKey(id, "x"), center.x);
            tag.putDouble(domainKey(id, "y"), center.y);
            tag.putDouble(domainKey(id, "z"), center.z);
        } else {
            center = new Vec3(tag.getDouble(domainKey(id, "x")),
                    tag.getDouble(domainKey(id, "y")), tag.getDouble(domainKey(id, "z")));
        }
        if ((caster.tickCount + caster.getId()) % 5 == 0) {
            domainParticles(level, id, center, radius, caster.getRandom());
        }
        if (now < tag.getLong(domainKey(id, "next"))) {
            return;
        }
        tag.putLong(domainKey(id, "next"), now + 20L);
        if (HANBA_DOMAIN.equals(id)) {
            UndeadControl.dryDroughtDomainPulse(level, center, (int) radius, now, caster);
        }
        List<LivingEntity> living = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(radius), entity -> entity.isAlive()
                        && entity.position().distanceToSqr(center) <= radius * radius);
        for (LivingEntity target : living) {
            if (target == caster || UndeadControl.hasUndeadPowers(target)) {
                applyFriendlyDomain(id, target, now);
            } else if (validEnemy(caster, target)) {
                applyHostileDomain(level, id, caster, target, now);
            }
        }
    }

    private static void temporarilyCorruptTerrain(ServerLevel level, LivingEntity caster, Vec3 center,
                                                   int radius, String domain, int lifetimeTicks) {
        if (!BlockBreakingSettings.enabled(caster,
                BlockBreakingSettings.Category.UNDEAD_DOMAINS)) return;
        Map<BlockPos, BlockState> replacements = new LinkedHashMap<>();
        int centerX = Mth.floor(center.x);
        int centerY = Mth.floor(center.y);
        int centerZ = Mth.floor(center.z);
        int radiusSquared = radius * radius;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radiusSquared) {
                    continue;
                }
                int x = centerX + dx;
                int z = centerZ + dz;
                for (int y = centerY + 3; y >= centerY - 12; y--) {
                    cursor.set(x, y, z);
                    BlockState state = level.getBlockState(cursor);
                    if (!isNaturalDomainSurface(state)
                            || !level.getBlockState(cursor.above()).canBeReplaced()) {
                        continue;
                    }
                    int variant = Math.floorMod(x * 73_428_767 ^ z * 91_293_121, 7);
                    BlockState replacement;
                    if (FUSHI_DOMAIN.equals(domain)) {
                        replacement = variant == 0 ? Blocks.SCULK.defaultBlockState()
                                : variant <= 2 ? Blocks.SOUL_SAND.defaultBlockState()
                                : Blocks.SOUL_SOIL.defaultBlockState();
                    } else {
                        replacement = variant <= 1 ? Blocks.SCULK.defaultBlockState()
                                : variant <= 3 ? Blocks.MUD.defaultBlockState()
                                : Blocks.ROOTED_DIRT.defaultBlockState();
                    }
                    replacements.put(cursor.immutable(), replacement);
                    break;
                }
            }
        }
        BlockBreakingSettings.runWithSource(
                BlockBreakingSettings.Category.UNDEAD_DOMAINS, caster,
                () -> TemporaryTerrainEntity.replaceAndRestore(level, replacements, lifetimeTicks));
    }

    private static boolean isNaturalDomainSurface(BlockState state) {
        return state.is(BlockTags.DIRT)
                || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.SAND)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.MUD)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.SNOW_BLOCK);
    }

    private static void applyFriendlyDomain(String id, LivingEntity target, long now) {
        CompoundTag tag = target.getPersistentData();
        switch (id) {
            case CORRUPT_FIELD -> {
                target.heal(20.0F);
                target.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED,
                        30, 0, false, false, false));
            }
            case FUSHI_DOMAIN -> {
                target.heal(100.0F);
                tag.putLong(TAG_DOMAIN_DAMAGE_UNTIL, now + 30L);
                tag.putFloat(TAG_DOMAIN_DAMAGE_MULTIPLIER, 1.20F);
                accelerateCooldowns(target, 3L, now);
            }
            case EARTH_CORRUPTION -> {
                target.heal(100.0F);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,
                        30, 0, false, false, false));
                tag.putLong(TAG_DOMAIN_DAMAGE_UNTIL, now + 30L);
                tag.putFloat(TAG_DOMAIN_DAMAGE_MULTIPLIER, 1.25F);
            }
            default -> {
            }
        }
        if (target.level() instanceof ServerLevel level) {
            UndeadAbilityVfx.domainAllyPulse(level, target);
        }
    }

    private static void applyHostileDomain(ServerLevel level, String id, LivingEntity caster,
                                           LivingEntity target, long now) {
        switch (id) {
            case CORRUPT_FIELD -> {
                abilityDamage(caster, target, 12.0F);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        30, 0, false, false, true), caster);
                markPenalty(target, 0.25F, 0.20F, now + 30L);
            }
            case FUSHI_DOMAIN -> {
                abilityDamage(caster, target, 120.0F);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        30, 0, false, false, true), caster);
                markPenalty(target, 0.40F, 0.35F, now + 30L);
            }
            case EARTH_CORRUPTION -> {
                markPenalty(target, 0.30F, 0.25F, now + 30L);
                level.sendParticles(CORPSE_GREEN, target.getX(), target.getY() + 0.15D,
                        target.getZ(), 7, 0.35D, 0.08D, 0.35D, 0.01D);
            }
            case HANBA_DOMAIN -> {
                float multiplier = usesMethodElement(target, SpiritRoots.FIRE) ? 0.70F : 1.0F;
                abilityDamage(caster, target, 500_000.0F * multiplier);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        30, 0, false, false, true), caster);
                target.removeEffect(ModEffects.DAMP);
                target.removeEffect(ModEffects.FROZEN);
                markPenalty(target, 0.50F, 0.0F, now + 30L);
            }
            case BLOOD_DROUGHT -> {
                abilityDamage(caster, target, 500_000.0F);
                target.removeEffect(ModEffects.DAMP);
                drainQi(target, 0.05F, false);
                markPenalty(target, 0.60F, 0.0F, now + 30L);
            }
            case ELEMENT_DOMAIN -> {
                target.getPersistentData().putLong(TAG_ELEMENT_DOMAIN_UNTIL, now + 30L);
                markPenalty(target, 0.30F, 0.0F, now + 30L);
                if (usesMethodElement(target, SpiritRoots.WATER)
                        || usesMethodElement(target, SpiritRoots.ICE)) {
                    drainQi(target, 0.02F, true);
                }
            }
            default -> {
            }
        }
        UndeadAbilityVfx.domainVictim(level, id, target);
    }

    private static void domainParticles(ServerLevel level, String id, Vec3 center,
                                        double radius, net.minecraft.util.RandomSource random) {
        UndeadAbilityVfx.domain(level, id, center, radius);
    }

    private static void accelerateCooldowns(LivingEntity entity, long ticks, long now) {
        CompoundTag tag = entity.getPersistentData();
        for (String id : ABILITY_IDS) {
            String key = TAG_PREFIX + "cooldown." + id;
            long until = tag.getLong(key);
            if (until > now) {
                tag.putLong(key, Math.max(now, until - ticks));
            }
        }
    }

    private static void markPenalty(LivingEntity target, float healingPenalty,
                                    float qiPenalty, long until) {
        CompoundTag tag = target.getPersistentData();
        long now = target.level().getGameTime();
        if (healingPenalty > 0.0F) {
            float current = tag.getLong(TAG_HEAL_PENALTY_UNTIL) > now
                    ? tag.getFloat(TAG_HEAL_PENALTY) : 0.0F;
            tag.putFloat(TAG_HEAL_PENALTY, Math.max(current, healingPenalty));
            tag.putLong(TAG_HEAL_PENALTY_UNTIL,
                    Math.max(tag.getLong(TAG_HEAL_PENALTY_UNTIL), until));
        }
        if (qiPenalty > 0.0F) {
            float current = tag.getLong(TAG_QI_PENALTY_UNTIL) > now
                    ? tag.getFloat(TAG_QI_PENALTY) : 0.0F;
            tag.putFloat(TAG_QI_PENALTY, Math.max(current, qiPenalty));
            tag.putLong(TAG_QI_PENALTY_UNTIL,
                    Math.max(tag.getLong(TAG_QI_PENALTY_UNTIL), until));
        }
    }

    private static void drainQi(LivingEntity target, float fraction, boolean maximum) {
        if (target instanceof ServerPlayer player) {
            ModAttachments.CultivationData data = ModAttachments.getData(player);
            int maxQi = Math.max(0, data.maxQi());
            int basis = maximum ? maxQi : data.qi();
            int drain = Math.max(0, Mth.floor(basis * fraction));
            ModAttachments.setData(player, data.withQi(Math.max(0, data.qi() - drain)));
        } else if (target instanceof PathfinderMob mob && target instanceof CultivationNpc npc) {
            String archetype = npc.npcArchetype();
            int basis = maximum ? NpcProfile.maxQi(mob, archetype) : NpcProfile.qi(mob, archetype);
            NpcProfile.drainQi(mob, archetype, Math.max(0, Mth.floor(basis * fraction)));
        }
    }

    private static boolean usesMethodElement(LivingEntity target, String element) {
        String methodId = "";
        if (target instanceof ServerPlayer player) {
            methodId = ModAttachments.getData(player).activeCultivationMethod();
        } else if (target instanceof PathfinderMob mob && target instanceof CultivationNpc npc) {
            methodId = NpcProfile.methodId(mob, npc.npcArchetype());
        }
        CultivationMethods.MethodDef method = CultivationMethods.get(methodId);
        if (method == null || method.element() == null) {
            return false;
        }
        for (String candidate : method.element().split(",")) {
            if (element.equals(candidate.trim())) {
                return true;
            }
        }
        return false;
    }

    private static int realmIndex(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            return CultivationLevels.getRealmIndex(ModAttachments.getData(player).cultivationLevel());
        }
        if (entity instanceof PathfinderMob mob && entity instanceof CultivationNpc npc) {
            return CultivationLevels.getRealmIndex(NpcProfile.realm(mob, npc.npcArchetype()));
        }
        if (UndeadControl.hasUndeadPowers(entity)) {
            return CultivationLevels.getRealmIndex(UndeadControl.levelFor(entity));
        }
        return 0;
    }

    private static List<LivingEntity> enemies(ServerLevel level, LivingEntity caster, double radius) {
        return level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(radius),
                target -> validEnemy(caster, target)
                        && caster.position().distanceToSqr(target.position()) <= radius * radius);
    }

    private static List<LivingEntity> coneTargets(ServerLevel level, LivingEntity caster,
                                                   Vec3 origin, Vec3 forward,
                                                   double range, double angleDegrees) {
        double minimumDot = Math.cos(Math.toRadians(angleDegrees * 0.5D));
        return level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(range), target -> {
            if (!validEnemy(caster, target) || !caster.hasLineOfSight(target)) {
                return false;
            }
            Vec3 toTarget = target.getEyePosition().subtract(origin);
            return toTarget.lengthSqr() <= range * range
                    && toTarget.lengthSqr() > 0.001D
                    && toTarget.normalize().dot(forward) >= minimumDot;
        });
    }

    private static void abilityDamageArea(ServerLevel level, LivingEntity caster, Vec3 center,
                                          double radius, float damage, boolean includeUndead) {
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(radius), living -> living.isAlive()
                        && living != caster
                        && living.position().distanceToSqr(center) <= radius * radius
                        && (includeUndead || validEnemy(caster, living)))) {
            abilityDamage(caster, target, damage);
        }
    }

    private static List<CultivatorCorpseEntity> usableCorpses(ServerLevel level,
                                                              Vec3 center, double radius) {
        List<CultivatorCorpseEntity> corpses = level.getEntitiesOfClass(
                CultivatorCorpseEntity.class, new AABB(center, center).inflate(radius),
                corpse -> corpse.isAlive() && !corpse.isRaised()
                        && corpse.position().distanceToSqr(center) <= radius * radius);
        corpses.sort(Comparator.comparingDouble(corpse -> corpse.position().distanceToSqr(center)));
        return corpses;
    }

    private static int corpseCount(ServerLevel level, Vec3 center, double radius) {
        return usableCorpses(level, center, radius).size();
    }

    private static void corpseStreams(ServerLevel level, List<CultivatorCorpseEntity> corpses,
                                      LivingEntity caster, boolean boneUnity) {
        UndeadAbilityVfx.corpseStreams(level, corpses, caster, boneUnity);
    }

    private static void drainStreamParticles(ServerLevel level, LivingEntity source,
                                             LivingEntity destination) {
        UndeadAbilityVfx.drainStream(level, source, destination);
    }

    private static Vec3 findTeleportDestination(ServerLevel level, LivingEntity caster,
                                                 Vec3 preferred, double range) {
        Vec3 origin = caster.position();
        List<Vec3> candidates = new ArrayList<>();
        candidates.add(preferred);
        for (int i = 0; i < 12; i++) {
            double angle = Math.PI * 2.0D * i / 12.0D;
            double distance = range * (0.45D + (i % 3) * 0.2D);
            candidates.add(origin.add(Math.cos(angle) * distance, 0.0D,
                    Math.sin(angle) * distance));
        }
        for (Vec3 candidate : candidates) {
            BlockPos ground = BlockPos.containing(candidate);
            for (int y = 2; y >= -2; y--) {
                BlockPos feet = ground.offset(0, y, 0);
                Vec3 destination = Vec3.atBottomCenterOf(feet);
                if (level.getBlockState(feet).isAir()
                        && level.getBlockState(feet.above()).isAir()
                        && !level.getBlockState(feet.below()).isAir()
                        && shadowPathAllowed(level, origin, destination)
                        && level.noCollision(caster, caster.getBoundingBox()
                        .move(destination.subtract(caster.position())))) {
                    return destination;
                }
            }
        }
        return origin;
    }

    /** The shadow may cross one occupied block column, but not a thicker wall. */
    private static boolean shadowPathAllowed(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        int steps = Math.max(1, Mth.ceil(delta.length() * 5.0D));
        int solidColumns = 0;
        int lastSolidX = Integer.MIN_VALUE;
        int lastSolidZ = Integer.MIN_VALUE;
        for (int i = 1; i < steps; i++) {
            Vec3 point = start.add(delta.scale(i / (double) steps));
            BlockPos feet = BlockPos.containing(point.x, point.y + 0.1D, point.z);
            boolean blocked = !level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                    || !level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty();
            if (!blocked || feet.getX() == lastSolidX && feet.getZ() == lastSolidZ) {
                continue;
            }
            lastSolidX = feet.getX();
            lastSolidZ = feet.getZ();
            if (++solidColumns > 1) {
                return false;
            }
        }
        return true;
    }

    private static void shadowTrail(ServerLevel level, Vec3 start, Vec3 end) {
        UndeadAbilityVfx.shadowTrail(level, start, end);
    }

    private static void sphereBurst(ServerLevel level, Vec3 center, double radius,
                                    ParticleOptions particle, int count) {
        for (int i = 0; i < count; i++) {
            double y = 1.0D - 2.0D * (i + 0.5D) / count;
            double circle = Math.sqrt(Math.max(0.0D, 1.0D - y * y));
            double angle = Math.PI * (3.0D - Math.sqrt(5.0D)) * i;
            level.sendParticles(particle,
                    center.x + Math.cos(angle) * circle * radius,
                    center.y + y * radius,
                    center.z + Math.sin(angle) * circle * radius,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void enforceBoneCage(ServerLevel level, LivingEntity entity, CompoundTag tag) {
        Vec3 center = new Vec3(tag.getDouble(TAG_CAGE_X), tag.getDouble(TAG_CAGE_Y),
                tag.getDouble(TAG_CAGE_Z));
        Vec3 offset = entity.position().subtract(center);
        double horizontal = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
        if (horizontal > 4.35D) {
            Vec3 edge = new Vec3(offset.x, 0.0D, offset.z).normalize().scale(4.25D);
            entity.teleportTo(center.x + edge.x, Math.max(center.y, entity.getY()), center.z + edge.z);
        }
        Vec3 movement = entity.getDeltaMovement();
        entity.setDeltaMovement(movement.x * 0.25D, movement.y, movement.z * 0.25D);
        if (entity.tickCount % 5 == 0) {
            ring(level, center.add(0.0D, 0.15D, 0.0D), BONE_WHITE, 4.7D, 18, 0.0D);
        }
    }

    private static void tickCopy(ServerLevel level, LivingEntity copy, long now) {
        if (now >= copy.getPersistentData().getLong(TAG_COPY_EXPIRES)) {
            ashDisappear(level, copy);
            copy.discard();
            return;
        }
        AttributeInstance damage = copy.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null) {
            damage.setBaseValue(0.0D);
        }
        if (copy instanceof Mob mob) {
            mob.setNoAi(true);
        }
        CompoundTag tag = copy.getPersistentData();
        if (tag.hasUUID(TAG_COPY_OWNER)) {
            Entity owner = level.getEntity(tag.getUUID(TAG_COPY_OWNER));
            if (owner instanceof LivingEntity livingOwner && livingOwner.isAlive()) {
                Vec3 desired = livingOwner.position().add(
                        tag.getDouble(TAG_COPY_OFFSET_X), 0.0D,
                        tag.getDouble(TAG_COPY_OFFSET_Z));
                AABB moved = copy.getBoundingBox().move(desired.subtract(copy.position()));
                if (level.noCollision(copy, moved)) {
                    copy.teleportTo(desired.x, desired.y, desired.z);
                }
                copy.setYRot(livingOwner.getYRot());
                copy.setXRot(livingOwner.getXRot());
                copy.setYBodyRot(livingOwner.yBodyRot);
                copy.setYHeadRot(livingOwner.yHeadRot);
            }
        }
        copy.setDeltaMovement(Vec3.ZERO);
        if (copy.tickCount % 4 == 0) {
            level.sendParticles(CORPSE_BLACK, copy.getX(), copy.getY() + copy.getBbHeight() * 0.5D,
                    copy.getZ(), 3, 0.25D, 0.45D, 0.25D, 0.0D);
        }
    }

    private static LivingEntity combatTarget(Mob mob) {
        LivingEntity target = mob.getTarget();
        if (validEnemy(mob, target)) {
            return target;
        }
        target = mob.getLastHurtByMob();
        return validEnemy(mob, target) ? target : null;
    }

    private static boolean validEnemy(LivingEntity caster, LivingEntity target) {
        if (target == null || !target.isAlive() || target == caster
                || caster.isAlliedTo(target) || UndeadControl.hasUndeadPowers(target)) {
            return false;
        }
        CompoundTag casterTag = caster.getPersistentData();
        if (casterTag.hasUUID(UndeadControl.TAG_OWNER)
                && casterTag.getUUID(UndeadControl.TAG_OWNER).equals(target.getUUID())) {
            return false;
        }
        CompoundTag targetTag = target.getPersistentData();
        return !casterTag.hasUUID(UndeadControl.TAG_OWNER)
                || !targetTag.hasUUID(UndeadControl.TAG_OWNER)
                || !casterTag.getUUID(UndeadControl.TAG_OWNER)
                .equals(targetTag.getUUID(UndeadControl.TAG_OWNER));
    }

    private static boolean ready(LivingEntity entity, String id, long now) {
        return entity.getPersistentData().getLong(TAG_PREFIX + "cooldown." + id) <= now;
    }

    private static void setCooldown(LivingEntity entity, String id, long now, long ticks) {
        if (entity instanceof net.minecraft.world.entity.player.Player player) ticks = com.example.immortal_cultivation_mod.attachment.BodyTypeEffects.cooldown(player, (int)Math.min(Integer.MAX_VALUE, ticks));
        entity.getPersistentData().putLong(TAG_PREFIX + "cooldown." + id, now + ticks);
    }

    private static void updateModifier(
            LivingEntity entity,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            ResourceLocation id, double amount,
            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation operation) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        net.minecraft.world.entity.ai.attributes.AttributeModifier existing = instance.getModifier(id);
        if (existing != null && Math.abs(existing.amount() - amount) < 0.0001D) {
            return;
        }
        if (existing != null) {
            instance.removeModifier(id);
        }
        if (Math.abs(amount) > 0.0001D) {
            instance.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                    id, amount, operation));
        }
    }

    private static boolean isDangerousIncoming(LivingEntity target, DamageSource source, float amount) {
        return amount >= Math.max(8.0F, target.getMaxHealth() * 0.08F)
                || source.getDirectEntity() instanceof Projectile;
    }

    private static void freeze(LivingEntity entity) {
        if (entity instanceof Mob mob) {
            mob.getNavigation().stop();
        }
        Vec3 movement = entity.getDeltaMovement();
        entity.setDeltaMovement(0.0D, Math.min(movement.y, 0.0D), 0.0D);
        entity.hurtMarked = true;
    }

    private static void ashDisappear(ServerLevel level, LivingEntity entity) {
        level.sendParticles(ParticleTypes.ASH, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D,
                entity.getZ(), 24, 0.35D, 0.55D, 0.35D, 0.025D);
        UndeadAbilityVfx.ashDisappear(level, entity);
    }

    private static void jiangshenStartParticles(ServerLevel level, LivingEntity entity) {
        UndeadAbilityVfx.hardenedShell(level, entity, true);
    }

    private static void jiangshenParticles(ServerLevel level, LivingEntity entity) {
        if (entity.tickCount % 4 == 0) {
            UndeadAbilityVfx.hardenedShell(level, entity, false);
        }
    }

    private static void ring(ServerLevel level, Vec3 center, ParticleOptions particle,
                             double radius, int count, double ySpeed) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count;
            level.sendParticles(particle, center.x + Math.cos(angle) * radius, center.y,
                    center.z + Math.sin(angle) * radius, 1, 0.0D, ySpeed, 0.0D, 0.0D);
        }
    }
}
