package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import com.example.immortal_cultivation_mod.entity.SpellImpactParticles;
import com.example.immortal_cultivation_mod.entity.SpellTriggerProjectileEntity;
import com.example.immortal_cultivation_mod.entity.SolarTechniqueModelEntity;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Real executions for the five techniques granted by 大日霸体诀. */
public final class SolarTechniques {
    private static final float EXTREME_YANG_REQUIRED_SOLAR = 99.0F;
    private static final Set<String> SPELLS = Set.of(
            ModSpells.YANGYAN_FUTI,
            ModSpells.DARI_ZHEN,
            ModSpells.LIEYANG_BU,
            ModSpells.RILUN,
            ModSpells.JIYANG_YIKE);
    private static final DustParticleOptions GOLD =
            new DustParticleOptions(new Vector3f(1.0F, 0.63F, 0.04F), 1.35F);
    private static final DustParticleOptions WHITE_GOLD =
            new DustParticleOptions(new Vector3f(1.0F, 0.96F, 0.64F), 1.65F);
    private static final double RIYUN_RADIUS = 10.0D;
    private static final int RIYUN_COLLAPSE_TICKS = 3;
    private static final int RIYUN_SECOND_PULSE_TICK = 14;
    private static final ExplosionDamageCalculator RIYUN_TERRAIN_ONLY =
            new ExplosionDamageCalculator() {
                @Override
                public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
                    return false;
                }

                @Override
                public float getKnockbackMultiplier(Entity entity) {
                    return 0.0F;
                }

                @Override
                public boolean shouldBlockExplode(
                        Explosion explosion,
                        BlockGetter level,
                        BlockPos pos,
                        BlockState state,
                        float power) {
                    return super.shouldBlockExplode(
                            explosion, level, pos, state, power);
                }
            };

    private SolarTechniques() {
    }

    public static boolean isTechnique(String spellId) {
        return SPELLS.contains(ModSpells.normalizeId(spellId));
    }

    public static boolean canCast(ServerPlayer player, String spellId) {
        String normalized = ModSpells.normalizeId(spellId);
        if (!SPELLS.contains(normalized)) return false;
        ModAttachments.CultivationData data = ModAttachments.getData(player);
        if (!CultivationMethods.isDariBatiJue(data.activeCultivationMethod())) {
            message(player, "solar_wrong_method");
            return false;
        }
        float required = switch (normalized) {
            case ModSpells.YANGYAN_FUTI -> 30.0F;
            case ModSpells.LIEYANG_BU -> 40.0F;
            case ModSpells.DARI_ZHEN -> 50.0F;
            case ModSpells.RILUN -> 70.0F;
            case ModSpells.JIYANG_YIKE -> EXTREME_YANG_REQUIRED_SOLAR;
            default -> 101.0F;
        };
        // Use the same whole-percent value shown by the HUD. Previously the
        // HUD could round 99.99 to 100 while the server still rejected it.
        if (Math.round(CultivationMethods.solarPower(data))
                < Math.round(required)) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".solar_too_low",
                    Math.round(required)), true);
            return false;
        }
        if (player.hasEffect(ModEffects.XUYANG)
                && (ModSpells.DARI_ZHEN.equals(normalized)
                || ModSpells.RILUN.equals(normalized)
                || ModSpells.JIYANG_YIKE.equals(normalized))) {
            message(player, "solar_exhausted");
            return false;
        }
        if (ModSpells.JIYANG_YIKE.equals(normalized)) {
            if (data.methodProficiency(CultivationMethods.DARI_BATI_JUE) < 1_000) {
                message(player, "solar_requires_perfected");
                return false;
            }
            if (!DariBatiJue.isDariLinshen(player, data)) {
                message(player, "solar_requires_noon");
                return false;
            }
        }
        return true;
    }

    public static boolean cast(
            ServerPlayer player, String spellId, float chargeScale, Consumer<Entity> projectileHook) {
        String normalized = ModSpells.normalizeId(spellId);
        if (!canCast(player, normalized)) return false;
        float charge = SpellCharge.clamp(chargeScale);
        float solar = CultivationMethods.solarPower(ModAttachments.getData(player));
        return switch (normalized) {
            case ModSpells.YANGYAN_FUTI -> castYangyan(player, solar, charge);
            case ModSpells.DARI_ZHEN -> castRiyun(
                    player, solar, charge, projectileHook);
            case ModSpells.LIEYANG_BU -> castLieyangBu(player, solar, charge);
            case ModSpells.RILUN -> castRilun(player, solar, charge, projectileHook);
            case ModSpells.JIYANG_YIKE -> castExtreme(player);
            default -> false;
        };
    }

    private static boolean castYangyan(ServerPlayer player, float solar, float charge) {
        ServerLevel level = player.serverLevel();
        player.getPersistentData().putLong(DariBatiJue.YANGYAN_UNTIL, level.getGameTime() + 200L);
        player.getPersistentData().putFloat(DariBatiJue.YANGYAN_CHARGE, charge);
        Vec3 center = player.getBoundingBox().getCenter();
        emitRing(level, center, 1.1D + solar / 150.0D, 34, ModParticles.METHOD_QI_FIRE.get());
        level.sendParticles(WHITE_GOLD, center.x, center.y, center.z,
                28, 0.48D, 0.75D, 0.48D, 0.06D);
        level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE,
                SoundSource.PLAYERS, 1.0F, 1.35F);
        return true;
    }

    private static boolean castRiyun(
            ServerPlayer player,
            float solar,
            float charge,
            Consumer<Entity> projectileHook) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 right = new Vec3(0.0D, 1.0D, 0.0D).cross(look);
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 hand = player.getEyePosition()
                .add(look.scale(0.58D))
                .add(right.scale(0.38D))
                .add(0.0D, 0.28D, 0.0D);
        emitRiyunPalmSun(level, hand, look, right, 0.52D,
                player.tickCount * 0.14D);

        SpellTriggerProjectileEntity projectile =
                new SpellTriggerProjectileEntity(
                        level, player, ModSpells.DARI_ZHEN, charge);
        projectile.setPos(hand.x, hand.y, hand.z);
        projectile.shoot(
                look.x,
                look.y,
                look.z,
                SpellCharge.isFull(charge) ? 1.48F : 1.22F,
                0.0F);
        projectileHook.accept(projectile);
        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.FIRECHARGE_USE,
                SoundSource.PLAYERS,
                1.25F,
                0.72F);
        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS,
                0.9F,
                1.65F);
        return level.addFreshEntity(projectile);
    }

    private static boolean castLieyangBu(ServerPlayer player, float solar, float charge) {
        Vec3 look = player.getLookAngle().normalize();
        double distanceScale = (1.05D + solar / 90.0D) * (SpellCharge.isFull(charge) ? 1.25D : 1.0D);
        Vec3 launch = new Vec3(look.x, Math.max(0.28D, look.y + 0.18D), look.z)
                .normalize().scale(distanceScale);
        player.setDeltaMovement(launch);
        com.example.immortal_cultivation_mod.combat.CombatFeedbackEvents.dashStarted(player, launch, .65F, 22);
        player.hurtMarked = true;
        player.resetFallDistance();
        player.getPersistentData().putBoolean(DariBatiJue.LEAP_ACTIVE, true);
        player.getPersistentData().putBoolean(DariBatiJue.LEAP_AIRBORNE, false);
        player.getPersistentData().putFloat(DariBatiJue.LEAP_CHARGE, charge);
        emitRing(player.serverLevel(), player.position().add(0.0D, 0.12D, 0.0D),
                1.25D, 30, ModParticles.METHOD_QI_LIGHT.get());
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_2.value(),
                SoundSource.PLAYERS, 1.0F, 1.45F);
        return true;
    }

    private static boolean castRilun(
            ServerPlayer player, float solar, float charge, Consumer<Entity> projectileHook) {
        if (player.isShiftKeyDown()) {
            player.getPersistentData().putLong(DariBatiJue.RILUN_HOLD_UNTIL,
                    player.serverLevel().getGameTime() + (SpellCharge.isFull(charge) ? 300L : 200L));
            player.getPersistentData().putFloat(DariBatiJue.RILUN_CHARGE, charge);
            SolarTechniqueModelEntity.spawnAttached(
                    player.serverLevel(), player, SolarTechniqueModelEntity.HELD_SUN);
            tickHeldSun(player, solar);
            return true;
        }
        ServerLevel level = player.serverLevel();
        SpellTriggerProjectileEntity projectile =
                new SpellTriggerProjectileEntity(level, player, ModSpells.RILUN, charge);
        Vec3 start = player.getEyePosition().add(player.getLookAngle().scale(0.85D));
        projectile.setPos(start.x, start.y, start.z);
        projectile.shoot(player.getLookAngle().x, player.getLookAngle().y,
                player.getLookAngle().z, SpellCharge.isFull(charge) ? 1.05F : 0.82F, 0.0F);
        projectileHook.accept(projectile);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS, 1.1F, 1.55F);
        return level.addFreshEntity(projectile);
    }

    private static boolean castExtreme(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        player.getPersistentData().putLong(DariBatiJue.EXTREME_UNTIL, level.getGameTime() + 200L);
        SolarTechniqueModelEntity.spawnAttached(
                level, player, SolarTechniqueModelEntity.EXTREME_HALO);
        Vec3 center = player.getBoundingBox().getCenter();
        level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z,
                3, 0.15D, 0.20D, 0.15D, 0.0D);
        level.sendParticles(WHITE_GOLD, center.x, center.y, center.z,
                90, 0.75D, 1.15D, 0.75D, 0.16D);
        emitRing(level, player.position().add(0.0D, 0.10D, 0.0D),
                3.0D, 80, ModParticles.METHOD_QI_FIRE.get());
        level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE,
                SoundSource.PLAYERS, 1.5F, 0.75F);
        return true;
    }

    public static void tickHeldSun(ServerPlayer player, float solar) {
        ServerLevel level = player.serverLevel();
        float charge = SpellCharge.clamp(player.getPersistentData().getFloat(DariBatiJue.RILUN_CHARGE));
        Vec3 center = player.getEyePosition().add(player.getLookAngle().scale(0.70D)).add(0.0D, 0.48D, 0.0D);
        if (player.tickCount % 2 == 0) {
            emitSphere(level, center, 0.42D + solar / 260.0D,
                    ModParticles.METHOD_QI_LIGHT.get(), GOLD, player.tickCount * 0.08D);
        }
        level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z,
                3, 0.20D, 0.20D, 0.20D, 0.01D);
        if (player.tickCount % 20 != 0) return;
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 40, 0,
                false, false, true));
        double radius = (3.5D + solar / 35.0D) * (SpellCharge.isFull(charge) ? 1.2D : 1.0D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(radius),
                living -> living != player && NpcProfile.isCombatEnemy(player, living))) {
            target.hurt(player.damageSources().indirectMagic(player, player),
                    SpellDamageHelper.damage(player,
                            SpellCharge.damage(18.0F + solar * 0.22F, charge),
                            ModSpells.get(ModSpells.RILUN)));
            target.igniteForSeconds(2.0F);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0,
                    false, false, true));
        }
        meltSnowAndIce(level, player, BlockPos.containing(center), (int) Math.ceil(radius));
    }

    public static void tickTriggerProjectile(SpellTriggerProjectileEntity projectile) {
        if (!(projectile.level() instanceof ServerLevel level)) {
            return;
        }
        if (ModSpells.DARI_ZHEN.equals(projectile.spellId())) {
            emitRiyunFlight(level, projectile);
            return;
        }
        if (!ModSpells.RILUN.equals(projectile.spellId())) {
            return;
        }
        if (projectile.tickCount % 2 != 0) return;
        Vec3 center = projectile.position();
        emitSphere(level, center, SpellCharge.isFull(projectile.chargeScale()) ? 1.15D : 0.82D,
                ModParticles.METHOD_QI_LIGHT.get(), GOLD, projectile.tickCount * 0.13D);
        level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z,
                5, 0.24D, 0.24D, 0.24D, 0.02D);
    }

    public static void activateTriggeredSpell(
            ServerLevel level, LivingEntity caster, String spellId, Vec3 impact,
            LivingEntity directTarget, float chargeScale) {
        String normalized = ModSpells.normalizeId(spellId);
        float solar = solarPowerFor(caster);
        if (ModSpells.DARI_ZHEN.equals(normalized)) {
            explodeRiyun(level, caster, impact, solar, chargeScale);
            return;
        }
        if (!ModSpells.RILUN.equals(normalized)) return;
        double radius = (4.0D + solar / 50.0D) * (SpellCharge.isFull(chargeScale) ? 1.25D : 1.0D);
        float damage = SpellDamageHelper.damage(caster,
                SpellCharge.damage(260.0F + solar * 3.0F, chargeScale),
                ModSpells.get(ModSpells.RILUN));
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(impact, impact).inflate(radius),
                living -> living != caster && NpcProfile.isCombatEnemy(caster, living))) {
            if (target.hurt(caster.damageSources().indirectMagic(caster, caster), damage)) {
                target.igniteForSeconds(4.0F + solar / 25.0F);
                Vec3 away = target.position().subtract(impact);
                target.knockback(0.55D + solar / 150.0D, -away.x, -away.z);
            }
        }
        SpellImpactParticles.fireExplosion(level, impact, radius);
        emitRing(level, impact.add(0.0D, 0.12D, 0.0D), radius, 80,
                ModParticles.METHOD_QI_LIGHT.get());
        meltSnowAndIce(level, caster, BlockPos.containing(impact), (int) Math.ceil(radius));
        level.playSound(null, BlockPos.containing(impact), SoundEvents.GENERIC_EXPLODE.value(),
                caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE,
                1.6F, 1.1F);
    }

    public static float triggerBarrierDamage(String spellId, float chargeScale) {
        String normalized = ModSpells.normalizeId(spellId);
        if (ModSpells.DARI_ZHEN.equals(normalized)) {
            return SpellCharge.damage(760.0F, chargeScale);
        }
        return ModSpells.RILUN.equals(normalized)
                ? SpellCharge.damage(560.0F, chargeScale)
                : 0.0F;
    }

    public static float solarPowerFor(LivingEntity caster) {
        return caster instanceof ServerPlayer player
                ? Mth.clamp(
                        CultivationMethods.solarPower(
                                ModAttachments.getData(player)),
                        0.0F,
                        100.0F)
                : 100.0F;
    }

    public static void tickRiyunCharge(ServerPlayer player, long elapsedTicks) {
        ModAttachments.CultivationData data = ModAttachments.getData(player);
        if (!CultivationMethods.isDariBatiJue(data.activeCultivationMethod())
                || CultivationMethods.solarPower(data) < 50.0F
                || player.hasEffect(ModEffects.XUYANG)) {
            return;
        }
        if (player.tickCount % 2 != 0) {
            return;
        }
        Vec3 look = player.getLookAngle().normalize();
        Vec3 right = new Vec3(0.0D, 1.0D, 0.0D).cross(look);
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 center = player.getEyePosition()
                .add(look.scale(0.58D))
                .add(right.scale(0.38D))
                .add(0.0D, 0.28D, 0.0D);
        double progress = Mth.clamp(elapsedTicks / 40.0D, 0.0D, 1.0D);
        double radius = 0.24D + progress * 0.30D;
        emitRiyunPalmSun(
                player.serverLevel(),
                center,
                look,
                right,
                radius,
                player.tickCount * (0.12D + progress * 0.08D));
        if (Math.floorMod(elapsedTicks, 6L) <= 1L) {
            emitSolarSigil(
                    player.serverLevel(), center.add(0.0D, -0.32D, 0.0D),
                    1.08D + progress * 0.22D, 0.11D, 8);
            emitSolarSigil(
                    player.serverLevel(), center.add(0.0D, -0.30D, 0.0D),
                    0.72D + progress * 0.16D, -0.16D, 8);
        }

        Vec3 up = right.cross(look).normalize();
        for (int ray = 0; ray < 6; ray++) {
            double angle = ray * Math.PI / 3.0D
                    + player.tickCount * 0.09D;
            Vec3 source = center
                    .add(right.scale(Math.cos(angle) * (radius + 0.55D)))
                    .add(up.scale(Math.sin(angle) * (radius + 0.55D)));
            directed(
                    player.serverLevel(),
                    ray % 2 == 0
                            ? ModParticles.SOLAR_EMBER.get()
                            : GOLD,
                    source,
                    center.subtract(source).normalize().scale(0.08D));
        }
    }

    public static void tickRiyunImpact(
            SpellTriggerProjectileEntity projectile) {
        if (!(projectile.level() instanceof ServerLevel level)
                || !(projectile.getOwner() instanceof LivingEntity caster)) {
            projectile.discard();
            return;
        }
        int age = projectile.solarImpactTicks();
        Vec3 center = projectile.position();
        float solar = projectile.solarPowerSnapshot();
        float charge = projectile.chargeScale();

        if (age <= RIYUN_COLLAPSE_TICKS) {
            double radius = Mth.lerp(
                    age / (double) RIYUN_COLLAPSE_TICKS,
                    SpellCharge.isFull(charge) ? 0.72D : 0.58D,
                    0.06D);
            emitSphere(
                    level,
                    center,
                    radius,
                    WHITE_GOLD,
                    ModParticles.METHOD_QI_LIGHT.get(),
                    projectile.tickCount * 0.31D);
            emitSolarCore(
                    level,
                    center,
                    Math.max(0.16D, radius * 1.32D),
                    projectile.tickCount * 0.07D,
                    4);
            level.sendParticles(
                    ModParticles.SOLAR_EMBER.get(),
                    center.x,
                    center.y,
                    center.z,
                    12,
                    radius,
                    radius,
                    radius,
                    0.045D);
            double ringRadius = radius * 1.55D + 0.08D;
            emitOrbitRing(
                    level, WHITE_GOLD, center, new Vec3(0.0D, 1.0D, 0.0D),
                    ringRadius, 16, projectile.tickCount * 0.48D);
            emitOrbitRing(
                    level, GOLD, center, new Vec3(1.0D, 0.0D, 0.0D),
                    ringRadius * 0.82D, 14, -projectile.tickCount * 0.42D);
            emitRiyunInwardCollapse(level, center, age, projectile.tickCount);
            if (age == RIYUN_COLLAPSE_TICKS) {
                level.sendParticles(
                        ParticleTypes.FLASH,
                        center.x,
                        center.y,
                        center.z,
                        1,
                        0.0D,
                        0.0D,
                        0.0D,
                        0.0D);
            }
            return;
        }

        if (age == RIYUN_COLLAPSE_TICKS + 1) {
            explodeRiyun(level, caster, center, solar, charge);
        }
        if (age > RIYUN_COLLAPSE_TICKS + 1 && age <= 12) {
            emitRiyunDomeWave(
                    level,
                    center,
                    solar,
                    (age - RIYUN_COLLAPSE_TICKS - 1) / 8.0D);
        }
        if (age >= 6 && age <= 18 && age % 2 == 0) {
            emitRiyunLightPillars(level, center, age);
        }
        if (solar >= 99.5F && age == RIYUN_SECOND_PULSE_TICK) {
            delayedRiyunPulse(level, caster, center, charge);
        }
        if (age % 10 == 0) {
            emitRiyunMark(level, center, solar, age);
        }
        if (age % 20 == 0) {
            burnRiyunMark(level, caster, center, solar, charge);
        }

        int lifetime = solar >= 99.5F ? 160 : solar >= 70.0F ? 120 : 80;
        if (age >= lifetime) {
            projectile.discard();
        }
    }

    private static void explodeRiyun(
            ServerLevel level,
            LivingEntity caster,
            Vec3 center,
            float solar,
            float chargeScale) {
        float solarProgress = Mth.clamp((solar - 50.0F) / 50.0F, 0.0F, 1.0F);
        float baseDamage = 200.0F + solar * 4.0F;
        float damage = SpellDamageHelper.damage(
                caster,
                SpellCharge.damage(baseDamage, chargeScale),
                ModSpells.get(ModSpells.DARI_ZHEN));
        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(center, center).inflate(RIYUN_RADIUS),
                living -> living != caster
                        && NpcProfile.isCombatEnemy(caster, living))) {
            Vec3 offset = target.getBoundingBox().getCenter().subtract(center);
            double distance = offset.length();
            if (distance > RIYUN_RADIUS) {
                continue;
            }
            float falloff = Mth.lerp(
                    (float) (distance / RIYUN_RADIUS), 1.0F, 0.65F);
            if (!target.hurt(
                    caster.damageSources().indirectMagic(caster, caster),
                    damage * falloff)) {
                continue;
            }
            target.igniteForSeconds(solar >= 99.5F ? 9.0F
                    : solar >= 70.0F ? 7.0F : 4.0F);
            Vec3 radial = new Vec3(offset.x, 0.0D, offset.z);
            if (radial.lengthSqr() < 1.0E-6D) {
                radial = new Vec3(1.0D, 0.0D, 0.0D);
            } else {
                radial = radial.normalize();
            }
            double knockback = 0.95D + solarProgress * 0.95D;
            target.setDeltaMovement(
                    target.getDeltaMovement().scale(0.25D)
                            .add(radial.scale(knockback))
                            .add(0.0D, 0.30D + solarProgress * 0.25D, 0.0D));
            target.hasImpulse = true;
            target.hurtMarked = true;
            if (distance <= 3.25D) {
                target.addEffect(new MobEffectInstance(
                        ModEffects.DAZE,
                        solar >= 99.5F ? 45 : solar >= 70.0F ? 36 : 28,
                        0,
                        false,
                        true,
                        true));
            }
        }

        float explosionPower = (solar >= 99.5F ? 7.25F
                : solar >= 70.0F ? 5.75F : 4.5F)
                + (SpellCharge.isFull(chargeScale) ? 1.25F : 0.0F);
        BlockBreakingSettings.runWithSource(
                BlockBreakingSettings.Category.TECHNIQUES, caster,
                () -> com.example.immortal_cultivation_mod.spell.SpellDestruction.explode(level,
                caster,
                null,
                RIYUN_TERRAIN_ONLY,
                center,
                explosionPower,
                true,
                Level.ExplosionInteraction.BLOCK));
        emitRiyunImpact(level, center, solar, chargeScale);
        emitRiyunGroundFormation(level, center, solar);
        meltSnowAndIce(level, caster, BlockPos.containing(center), 10);
        level.playSound(
                null,
                BlockPos.containing(center),
                SoundEvents.GENERIC_EXPLODE.value(),
                caster instanceof ServerPlayer
                        ? SoundSource.PLAYERS
                        : SoundSource.HOSTILE,
                2.3F,
                0.72F);
        level.playSound(
                null,
                BlockPos.containing(center),
                SoundEvents.LIGHTNING_BOLT_THUNDER,
                caster instanceof ServerPlayer
                        ? SoundSource.PLAYERS
                        : SoundSource.HOSTILE,
                1.25F,
                1.55F);
    }

    private static void delayedRiyunPulse(
            ServerLevel level,
            LivingEntity caster,
            Vec3 center,
            float chargeScale) {
        float damage = SpellDamageHelper.damage(
                caster,
                SpellCharge.damage(290.0F, chargeScale),
                ModSpells.get(ModSpells.DARI_ZHEN));
        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(center, center).inflate(RIYUN_RADIUS),
                living -> living != caster
                        && NpcProfile.isCombatEnemy(caster, living))) {
            if (target.getBoundingBox().getCenter().distanceToSqr(center)
                    > RIYUN_RADIUS * RIYUN_RADIUS) {
                continue;
            }
            Vec3 radial = target.position().subtract(center)
                    .multiply(1.0D, 0.0D, 1.0D);
            if (radial.lengthSqr() < 1.0E-6D) {
                radial = new Vec3(1.0D, 0.0D, 0.0D);
            } else {
                radial = radial.normalize();
            }
            target.hurt(
                    caster.damageSources().indirectMagic(caster, caster),
                    damage);
            target.setDeltaMovement(
                    target.getDeltaMovement().scale(0.2D)
                            .add(radial.scale(2.3D))
                            .add(0.0D, 0.70D, 0.0D));
            target.hasImpulse = true;
            target.hurtMarked = true;
            target.igniteForSeconds(5.0F);
        }
        level.sendParticles(
                ParticleTypes.FLASH,
                center.x,
                center.y + 0.35D,
                center.z,
                3,
                0.15D,
                0.15D,
                0.15D,
                0.0D);
        emitSolarCore(level, center.add(0.0D, 0.35D, 0.0D), 2.8D, 0.18D, 8);
        SpellImpactParticles.shockRing(
                level,
                center.add(0.0D, 0.16D, 0.0D),
                WHITE_GOLD,
                RIYUN_RADIUS,
                96,
                0.62D);
        emitSolarSigil(
                level,
                riyunGroundCenter(level, center),
                -12.0D,
                -0.08D,
                20);
        emitSolarBurst(level, center.add(0.0D, 0.25D, 0.0D), 56, 1.25D);
        emitRiyunDome(level, center, RIYUN_RADIUS, 4, 1.18D);
        emitRiyunLightPillars(level, center, RIYUN_SECOND_PULSE_TICK);
        level.playSound(
                null,
                BlockPos.containing(center),
                SoundEvents.BEACON_DEACTIVATE,
                caster instanceof ServerPlayer
                        ? SoundSource.PLAYERS
                        : SoundSource.HOSTILE,
                1.4F,
                1.75F);
    }

    private static void burnRiyunMark(
            ServerLevel level,
            LivingEntity caster,
            Vec3 center,
            float solar,
            float chargeScale) {
        double radius = solar >= 99.5F ? 6.0D : solar >= 70.0F ? 5.0D : 4.0D;
        float damage = SpellDamageHelper.damage(
                caster,
                SpellCharge.damage(12.0F + solar * 0.18F, chargeScale),
                ModSpells.get(ModSpells.DARI_ZHEN));
        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(center, center).inflate(radius, 2.0D, radius),
                living -> living != caster
                        && NpcProfile.isCombatEnemy(caster, living))) {
            Vec3 offset = target.getBoundingBox().getCenter().subtract(center);
            if (offset.x * offset.x + offset.z * offset.z > radius * radius) {
                continue;
            }
            target.hurt(
                    caster.damageSources().indirectMagic(caster, caster),
                    damage);
            target.igniteForSeconds(2.0F);
        }
    }

    private static void emitRiyunPalmSun(
            ServerLevel level,
            Vec3 center,
            Vec3 look,
            Vec3 right,
            double radius,
            double rotation) {
        Vec3 up = right.cross(look);
        if (up.lengthSqr() < 1.0E-6D) {
            up = new Vec3(0.0D, 1.0D, 0.0D);
        } else {
            up = up.normalize();
        }
        level.sendParticles(
                WHITE_GOLD,
                center.x,
                center.y,
                center.z,
                7,
                radius * 0.22D,
                radius * 0.22D,
                radius * 0.22D,
                0.0D);
        emitSolarCore(level, center, radius * 1.18D, rotation * 0.035D, 5);
        level.sendParticles(
                ModParticles.SOLAR_EMBER.get(),
                center.x,
                center.y,
                center.z,
                9,
                radius * 0.48D,
                radius * 0.48D,
                radius * 0.48D,
                0.035D);
        emitOrbitRing(
                level,
                ModParticles.METHOD_QI_LIGHT.get(),
                center,
                look,
                radius * 1.25D,
                12,
                rotation);
        emitOrbitRing(
                level,
                GOLD,
                center,
                right,
                radius * 1.42D,
                12,
                -rotation * 0.82D);
        emitOrbitRing(
                level,
                WHITE_GOLD,
                center,
                up,
                radius * 1.58D,
                12,
                rotation * 1.24D);
        emitSolarCorona(
                level,
                center,
                look,
                radius * 1.02D,
                10,
                rotation * 0.74D);
    }

    private static void emitRiyunFlight(
            ServerLevel level,
            SpellTriggerProjectileEntity projectile) {
        Vec3 center = projectile.position();
        Vec3 movement = projectile.getDeltaMovement();
        Vec3 direction = movement.lengthSqr() < 1.0E-6D
                ? new Vec3(0.0D, 0.0D, 1.0D)
                : movement.normalize();
        double sphereRadius = SpellCharge.isFull(projectile.chargeScale())
                ? 0.72D
                : 0.58D;
        emitSolarCore(
                level,
                center,
                sphereRadius * 1.28D,
                projectile.tickCount * 0.045D,
                4);
        level.sendParticles(
                WHITE_GOLD,
                center.x,
                center.y,
                center.z,
                5,
                0.10D,
                0.10D,
                0.10D,
                0.0D);
        level.sendParticles(
                ModParticles.SOLAR_EMBER.get(),
                center.x,
                center.y,
                center.z,
                8,
                sphereRadius * 0.55D,
                sphereRadius * 0.55D,
                sphereRadius * 0.55D,
                0.055D);
        emitRiyunFlightRibbons(
                level, center, direction, sphereRadius, projectile.tickCount);
        level.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                center.x,
                center.y,
                center.z,
                4,
                0.38D,
                0.38D,
                0.38D,
                0.12D);
        if (projectile.tickCount % 2 == 0) {
            emitOrbitRing(
                    level,
                    WHITE_GOLD,
                    center,
                    direction,
                    sphereRadius * 1.24D,
                    14,
                    projectile.tickCount * 0.24D);
            emitOrbitRing(
                    level,
                    GOLD,
                    center,
                    direction,
                    sphereRadius * 1.58D,
                    18,
                    -projectile.tickCount * 0.19D);
            emitSolarCorona(
                    level,
                    center,
                    direction,
                    sphereRadius * 1.05D,
                    12,
                    projectile.tickCount * 0.31D);
            level.sendParticles(
                    ParticleTypes.GUST,
                    center.x,
                    center.y,
                    center.z,
                    1,
                    sphereRadius,
                    sphereRadius,
                    sphereRadius,
                    0.02D);
        }
    }

    private static void emitRiyunImpact(
            ServerLevel level,
            Vec3 center,
            float solar,
            float chargeScale) {
        int scaleBonus = solar >= 99.5F ? 2 : solar >= 70.0F ? 1 : 0;
        level.sendParticles(
                ParticleTypes.FLASH,
                center.x,
                center.y + 0.5D,
                center.z,
                4 + scaleBonus,
                0.35D,
                0.35D,
                0.35D,
                0.0D);
        emitSolarCore(
                level,
                center.add(0.0D, 0.45D, 0.0D),
                3.2D + scaleBonus * 0.45D,
                0.22D,
                8);
        level.sendParticles(
                WHITE_GOLD,
                center.x,
                center.y + 0.45D,
                center.z,
                72 + scaleBonus * 18,
                1.15D,
                1.15D,
                1.15D,
                0.18D);
        level.sendParticles(
                ModParticles.METHOD_QI_LIGHT.get(),
                center.x,
                center.y + 0.35D,
                center.z,
                94 + scaleBonus * 24,
                2.1D,
                1.8D,
                2.1D,
                0.24D);
        level.sendParticles(
                ModParticles.SOLAR_EMBER.get(),
                center.x,
                center.y + 0.25D,
                center.z,
                108 + scaleBonus * 28,
                2.8D,
                1.5D,
                2.8D,
                0.28D);
        level.sendParticles(
                ParticleTypes.FLAME,
                center.x,
                center.y + 0.20D,
                center.z,
                58 + scaleBonus * 18,
                3.2D,
                1.25D,
                3.2D,
                0.22D);
        emitRiyunDome(
                level,
                center,
                1.8D,
                3,
                solar >= 99.5F ? 1.22D : 1.0D);
        emitSolarBurst(
                level,
                center.add(0.0D, 0.35D, 0.0D),
                72 + scaleBonus * 18,
                0.85D + scaleBonus * 0.16D);
        emitRiyunDebris(level, center, 46 + scaleBonus * 14);
        SpellImpactParticles.shockRing(
                level,
                center.add(0.0D, 0.16D, 0.0D),
                ModParticles.METHOD_QI_LIGHT.get(),
                RIYUN_RADIUS,
                SpellCharge.isFull(chargeScale) ? 120 : 96,
                0.58D);
        SpellImpactParticles.shockRing(
                level,
                center.add(0.0D, 0.08D, 0.0D),
                GOLD,
                RIYUN_RADIUS * 0.82D,
                72,
                0.42D);
        for (int ray = 0; ray < 18; ray++) {
            double angle = ray * Math.PI * 2.0D / 18.0D;
            double distance = 1.6D + ray % 4 * 1.25D;
            Vec3 source = center.add(
                    Math.cos(angle) * distance,
                    0.18D,
                    Math.sin(angle) * distance);
            directed(
                    level,
                    ray % 3 == 0
                            ? WHITE_GOLD
                            : ModParticles.METHOD_QI_LIGHT.get(),
                    source,
                    new Vec3(0.0D, 0.54D + ray % 5 * 0.10D, 0.0D));
        }
    }

    private static void emitRiyunInwardCollapse(
            ServerLevel level,
            Vec3 center,
            int age,
            int phase) {
        int points = 14;
        double shellRadius = 2.15D - age * 0.30D;
        double goldenAngle = Math.PI * (3.0D - Math.sqrt(5.0D));
        for (int pointIndex = 0; pointIndex < points; pointIndex++) {
            double y = 1.0D - (pointIndex + 0.5D) * 2.0D / points;
            double horizontal = Math.sqrt(Math.max(0.0D, 1.0D - y * y));
            double angle = pointIndex * goldenAngle + phase * 0.16D;
            Vec3 direction = new Vec3(
                    Math.cos(angle) * horizontal,
                    y,
                    Math.sin(angle) * horizontal);
            Vec3 source = center.add(direction.scale(shellRadius));
            directed(
                    level,
                    pointIndex % 4 == 0
                            ? ModParticles.METHOD_QI_LIGHT.get()
                            : ModParticles.SOLAR_EMBER.get(),
                    source,
                    direction.scale(-(0.16D + age * 0.035D)));
        }
    }

    private static void emitRiyunDomeWave(
            ServerLevel level,
            Vec3 center,
            float solar,
            double progress) {
        double clamped = Mth.clamp(progress, 0.0D, 1.0D);
        double eased = 1.0D - (1.0D - clamped) * (1.0D - clamped);
        emitRiyunDome(
                level,
                center,
                Math.max(1.8D, RIYUN_RADIUS * eased),
                3,
                solar >= 99.5F ? 1.22D : solar >= 70.0F ? 1.10D : 1.0D);
    }

    private static void emitRiyunGroundFormation(
            ServerLevel level,
            Vec3 impact,
            float solar) {
        Vec3 ground = riyunGroundCenter(level, impact);
        int lifetime = solar >= 99.5F ? 160 : solar >= 70.0F ? 120 : 80;
        emitSolarSigil(level, ground, 9.8D, 0.012D, lifetime);
        emitSolarSigil(level, ground.add(0.0D, 0.012D, 0.0D),
                6.4D, -0.021D, lifetime);
        emitSolarSigil(level, ground.add(0.0D, 0.024D, 0.0D),
                -10.5D, 0.055D, 18);
        emitSolarSigil(level, ground.add(0.0D, 0.036D, 0.0D),
                -13.0D, -0.036D, 26);
    }

    private static void emitRiyunLightPillars(
            ServerLevel level,
            Vec3 impact,
            int phase) {
        Vec3 ground = riyunGroundCenter(level, impact);
        for (int ray = 0; ray < 12; ray++) {
            double angle = ray * Math.PI / 6.0D + phase * 0.17D;
            double distance = 1.2D + (ray % 4) * 1.65D;
            Vec3 source = ground.add(
                    Math.cos(angle) * distance,
                    0.08D,
                    Math.sin(angle) * distance);
            directed(
                    level,
                    ray % 3 == 0
                            ? ModParticles.SOLAR_EMBER.get()
                            : ParticleTypes.END_ROD,
                    source,
                    new Vec3(0.0D, 0.42D + (ray % 5) * 0.11D, 0.0D));
        }
    }

    private static void emitRiyunDebris(
            ServerLevel level,
            Vec3 impact,
            int count) {
        Vec3 ground = riyunGroundCenter(level, impact);
        BlockPos groundPos = BlockPos.containing(ground).below();
        BlockState state = level.getBlockState(groundPos);
        if (!state.isAir()) {
            level.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, state),
                    impact.x,
                    ground.y + 0.28D,
                    impact.z,
                    count,
                    3.6D,
                    0.65D,
                    3.6D,
                    0.42D);
        }
        level.sendParticles(
                ParticleTypes.LARGE_SMOKE,
                impact.x,
                ground.y + 0.65D,
                impact.z,
                18,
                2.6D,
                0.45D,
                2.6D,
                0.075D);
    }

    private static void emitSolarBurst(
            ServerLevel level,
            Vec3 center,
            int count,
            double speed) {
        level.sendParticles(
                ModParticles.SOLAR_EMBER.get(),
                center.x,
                center.y,
                center.z,
                count,
                0.72D,
                0.54D,
                0.72D,
                speed * 0.34D);
        for (int ray = 0; ray < 18; ray++) {
            double y = -0.08D + (ray % 5) * 0.18D;
            double horizontal = Math.sqrt(Math.max(0.0D, 1.0D - y * y));
            double angle = ray * Math.PI * 2.0D / 18.0D;
            Vec3 direction = new Vec3(
                    Math.cos(angle) * horizontal,
                    y,
                    Math.sin(angle) * horizontal).normalize();
            directed(
                    level,
                    ray % 3 == 0
                            ? ParticleTypes.ELECTRIC_SPARK
                            : ModParticles.SOLAR_EMBER.get(),
                    center.add(direction.scale(0.35D)),
                    direction.scale(speed * (0.62D + (ray % 4) * 0.09D)));
        }
    }

    private static void emitSolarCorona(
            ServerLevel level,
            Vec3 center,
            Vec3 normal,
            double radius,
            int rays,
            double rotation) {
        Vec3 axis = normal.lengthSqr() < 1.0E-6D
                ? new Vec3(0.0D, 0.0D, 1.0D)
                : normal.normalize();
        Vec3 helper = Math.abs(axis.y) < 0.85D
                ? new Vec3(0.0D, 1.0D, 0.0D)
                : new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 first = axis.cross(helper).normalize();
        Vec3 second = axis.cross(first).normalize();
        for (int ray = 0; ray < rays; ray++) {
            double angle = ray * Math.PI * 2.0D / rays + rotation;
            Vec3 radial = first.scale(Math.cos(angle))
                    .add(second.scale(Math.sin(angle)));
            Vec3 source = center.add(radial.scale(radius));
            directed(
                    level,
                    ray % 3 == 0 ? WHITE_GOLD : ModParticles.SOLAR_EMBER.get(),
                    source,
                    radial.scale(0.035D + (ray % 4) * 0.008D));
            if (ray % 2 == 0) {
                Vec3 tip = center.add(radial.scale(radius * 1.62D));
                level.sendParticles(
                        WHITE_GOLD,
                        tip.x,
                        tip.y,
                        tip.z,
                        1,
                        0.0D,
                        0.0D,
                        0.0D,
                        0.0D);
            }
        }
    }

    private static void emitRiyunFlightRibbons(
            ServerLevel level,
            Vec3 center,
            Vec3 direction,
            double sphereRadius,
            int phase) {
        Vec3 helper = Math.abs(direction.y) < 0.85D
                ? new Vec3(0.0D, 1.0D, 0.0D)
                : new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 side = direction.cross(helper).normalize();
        Vec3 up = direction.cross(side).normalize();
        Vec3 trailCenter = center.subtract(direction.scale(0.48D));
        level.sendParticles(
                ModParticles.SOLAR_EMBER.get(),
                trailCenter.x,
                trailCenter.y,
                trailCenter.z,
                7,
                0.22D,
                0.22D,
                0.22D,
                0.05D);
        if (phase % 2 != 0) {
            return;
        }
        for (int strand = 0; strand < 2; strand++) {
            for (int pointIndex = 1; pointIndex <= 5; pointIndex++) {
                double angle = phase * 0.34D + strand * Math.PI
                        + pointIndex * 0.78D;
                double ribbonRadius = sphereRadius * (0.48D + pointIndex * 0.045D);
                Vec3 point = center
                        .subtract(direction.scale(0.18D + pointIndex * 0.30D))
                        .add(side.scale(Math.cos(angle) * ribbonRadius))
                        .add(up.scale(Math.sin(angle) * ribbonRadius));
                directed(
                        level,
                        (pointIndex + strand) % 3 == 0
                                ? WHITE_GOLD
                                : ModParticles.SOLAR_EMBER.get(),
                        point,
                        direction.scale(-0.045D));
            }
        }
    }

    private static void emitSolarSigil(
            ServerLevel level,
            Vec3 center,
            double size,
            double spin,
            int lifetime) {
        level.sendParticles(
                ModParticles.SOLAR_SIGIL.get(),
                center.x,
                center.y,
                center.z,
                0,
                size,
                spin,
                lifetime,
                1.0D);
    }

    private static void emitSolarCore(
            ServerLevel level,
            Vec3 center,
            double size,
            double spin,
            int lifetime) {
        level.sendParticles(
                ModParticles.SOLAR_CORE.get(),
                center.x,
                center.y,
                center.z,
                0,
                size,
                spin,
                lifetime,
                1.0D);
    }

    private static void emitRiyunDome(
            ServerLevel level,
            Vec3 center,
            double radius,
            int layers,
            double heightScale) {
        for (int layerIndex = 0; layerIndex <= layers; layerIndex++) {
            double elevation = Math.PI * 0.5D * layerIndex / layers;
            double ringRadius = Math.cos(elevation) * radius;
            double y = Math.sin(elevation) * radius * 0.58D * heightScale;
            int points = Math.max(
                    10,
                    (int) Math.ceil(42.0D * Math.max(0.24D, Math.cos(elevation))));
            for (int pointIndex = 0; pointIndex < points; pointIndex++) {
                double angle = pointIndex * Math.PI * 2.0D / points
                        + layerIndex * 0.19D;
                Vec3 point = center.add(
                        Math.cos(angle) * ringRadius,
                        y,
                        Math.sin(angle) * ringRadius);
                level.sendParticles(
                        (pointIndex + layerIndex) % 3 == 0
                                ? WHITE_GOLD
                                : (pointIndex + layerIndex) % 3 == 1
                                        ? ModParticles.METHOD_QI_LIGHT.get()
                                        : ModParticles.SOLAR_EMBER.get(),
                        point.x,
                        point.y,
                        point.z,
                        1,
                        0.0D,
                        0.0D,
                        0.0D,
                        0.0D);
            }
        }
    }

    private static void emitRiyunMark(
            ServerLevel level,
            Vec3 impact,
            float solar,
            int age) {
        Vec3 center = riyunGroundCenter(level, impact);
        double radius = solar >= 99.5F ? 5.5D : solar >= 70.0F ? 4.8D : 4.0D;
        int lifetime = solar >= 99.5F ? 160 : solar >= 70.0F ? 120 : 80;
        double fade = Mth.clamp(1.0D - age / (double) lifetime, 0.12D, 1.0D);
        int ringPoints = Math.max(12, (int) Math.round(28.0D * fade));
        for (int pointIndex = 0; pointIndex < ringPoints; pointIndex++) {
            double angle = pointIndex * Math.PI * 2.0D / ringPoints;
            Vec3 point = center.add(
                    Math.cos(angle) * radius,
                    0.0D,
                    Math.sin(angle) * radius);
            level.sendParticles(
                    pointIndex % 3 == 0 ? WHITE_GOLD : GOLD,
                    point.x,
                    point.y,
                    point.z,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D);
        }
        for (int ray = 0; ray < 8; ray++) {
            double angle = ray * Math.PI / 4.0D;
            for (int pointIndex = 1; pointIndex <= 3; pointIndex++) {
                double distance = radius * pointIndex / 3.0D;
                Vec3 point = center.add(
                        Math.cos(angle) * distance,
                        0.0D,
                        Math.sin(angle) * distance);
                level.sendParticles(
                        ray % 2 == 0
                                ? ModParticles.METHOD_QI_LIGHT.get()
                                : GOLD,
                        point.x,
                        point.y,
                        point.z,
                        1,
                        0.0D,
                        0.0D,
                        0.0D,
                        0.0D);
            }
            if (age % 20 == 0) {
                Vec3 ember = center.add(
                        Math.cos(angle) * radius * 0.72D,
                        0.08D,
                        Math.sin(angle) * radius * 0.72D);
                directed(
                        level,
                        ModParticles.SOLAR_EMBER.get(),
                        ember,
                        new Vec3(
                                Math.cos(angle) * 0.025D,
                                0.11D,
                                Math.sin(angle) * 0.025D));
            }
        }
    }

    private static Vec3 riyunGroundCenter(
            ServerLevel level,
            Vec3 impact) {
        BlockPos cursor = BlockPos.containing(impact).above();
        for (int depth = 0; depth <= 12; depth++) {
            BlockPos below = cursor.below();
            if (!level.getBlockState(below).isAir()) {
                return new Vec3(
                        impact.x,
                        below.getY() + 1.035D,
                        impact.z);
            }
            cursor = below;
        }
        return impact.add(0.0D, 0.035D, 0.0D);
    }

    private static void emitOrbitRing(
            ServerLevel level,
            ParticleOptions particle,
            Vec3 center,
            Vec3 normal,
            double radius,
            int points,
            double rotation) {
        Vec3 axis = normal.lengthSqr() < 1.0E-6D
                ? new Vec3(0.0D, 1.0D, 0.0D)
                : normal.normalize();
        Vec3 helper = Math.abs(axis.y) < 0.85D
                ? new Vec3(0.0D, 1.0D, 0.0D)
                : new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 first = axis.cross(helper).normalize();
        Vec3 second = axis.cross(first).normalize();
        for (int pointIndex = 0; pointIndex < points; pointIndex++) {
            double angle = pointIndex * Math.PI * 2.0D / points + rotation;
            Vec3 point = center
                    .add(first.scale(Math.cos(angle) * radius))
                    .add(second.scale(Math.sin(angle) * radius));
            level.sendParticles(
                    particle,
                    point.x,
                    point.y,
                    point.z,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D);
        }
    }

    private static void directed(
            ServerLevel level,
            ParticleOptions particle,
            Vec3 position,
            Vec3 velocity) {
        level.sendParticles(
                particle,
                position.x,
                position.y,
                position.z,
                0,
                velocity.x,
                velocity.y,
                velocity.z,
                1.0D);
    }

    public static void landingShockwave(
            ServerPlayer player, double radius, float baseDamage, float chargeScale) {
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position();
        float charge = SpellCharge.clamp(chargeScale);
        float damage = SpellDamageHelper.damage(player, SpellCharge.damage(baseDamage, charge),
                ModSpells.get(ModSpells.LIEYANG_BU));
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                living -> living != player && NpcProfile.isCombatEnemy(player, living))) {
            if (target.hurt(player.damageSources().indirectMagic(player, player), damage)) {
                Vec3 away = target.position().subtract(center);
                target.knockback(0.65D, -away.x, -away.z);
            }
        }
        emitRing(level, center.add(0.0D, 0.10D, 0.0D), radius, 46,
                ModParticles.METHOD_QI_FIRE.get());
        level.sendParticles(ParticleTypes.POOF, center.x, center.y + 0.12D, center.z,
                24, radius * 0.35D, 0.16D, radius * 0.35D, 0.10D);
    }

    public static void emitRing(
            ServerLevel level, Vec3 center, double radius, int points, ParticleOptions particle) {
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;
            Vec3 point = center.add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
            level.sendParticles(particle, point.x, point.y, point.z,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    public static void emitHalo(
            ServerLevel level, Vec3 center, Vec3 normal, double radius,
            int points, double rotation) {
        Vec3 forward = normal.lengthSqr() < 1.0E-6D ? new Vec3(0, 0, 1) : normal.normalize();
        Vec3 right = forward.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1.0E-6D) right = new Vec3(1, 0, 0);
        else right = right.normalize();
        Vec3 up = right.cross(forward).normalize();
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points + rotation;
            Vec3 point = center.add(right.scale(Math.cos(angle) * radius))
                    .add(up.scale(Math.sin(angle) * radius));
            level.sendParticles(i % 3 == 0 ? WHITE_GOLD : ModParticles.METHOD_QI_LIGHT.get(),
                    point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
    }

    private static void emitSphere(
            ServerLevel level, Vec3 center, double radius,
            ParticleOptions first, ParticleOptions second, double rotation) {
        int points = 28;
        double goldenAngle = Math.PI * (3.0D - Math.sqrt(5.0D));
        for (int i = 0; i < points; i++) {
            double y = 1.0D - (i / (double) (points - 1)) * 2.0D;
            double ringRadius = Math.sqrt(Math.max(0.0D, 1.0D - y * y));
            double angle = goldenAngle * i + rotation;
            Vec3 point = center.add(Math.cos(angle) * ringRadius * radius,
                    y * radius, Math.sin(angle) * ringRadius * radius);
            level.sendParticles(i % 2 == 0 ? first : second,
                    point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
    }

    private static void meltSnowAndIce(ServerLevel level, LivingEntity caster, BlockPos center, int radius) {
        if (!BlockBreakingSettings.enabled(caster,
                BlockBreakingSettings.Category.TECHNIQUES)) return;
        for (BlockPos cursor : BlockPos.betweenClosed(
                center.offset(-radius, -1, -radius), center.offset(radius, 2, radius))) {
            if (cursor.distSqr(center) > (double) radius * radius) continue;
            if (level.getBlockState(cursor).is(Blocks.SNOW)
                    || level.getBlockState(cursor).is(Blocks.SNOW_BLOCK)) {
                BlockPos pos = cursor.immutable();
                BlockBreakingSettings.runWithSource(
                        BlockBreakingSettings.Category.TECHNIQUES, caster,
                        () -> SpellDestruction.breakBlock(level, pos, false, caster));
            } else if (level.getBlockState(cursor).is(Blocks.ICE)
                    || level.getBlockState(cursor).is(Blocks.FROSTED_ICE)) {
                TerrainTransforms.replace(level, cursor, Blocks.WATER.defaultBlockState(), caster,
                        BlockBreakingSettings.Category.TECHNIQUES);
            }
        }
    }

    private static void message(ServerPlayer player, String suffix) {
        player.displayClientMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID + "." + suffix), true);
    }
}
