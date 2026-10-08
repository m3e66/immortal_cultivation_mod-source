package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.combat.CombatStateManager;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Runtime rules for 大日霸体诀. All resource and combat decisions are server authoritative. */
public final class DariBatiJue {
    static final String YANGYAN_UNTIL = "immortal_cultivation_mod:yangyan_until";
    static final String YANGYAN_CHARGE = "immortal_cultivation_mod:yangyan_charge";
    static final String EXTREME_UNTIL = "immortal_cultivation_mod:jiyang_until";
    static final String LEAP_ACTIVE = "immortal_cultivation_mod:lieyang_leap";
    static final String LEAP_AIRBORNE = "immortal_cultivation_mod:lieyang_airborne";
    static final String LEAP_CHARGE = "immortal_cultivation_mod:lieyang_charge";
    static final String RILUN_HOLD_UNTIL = "immortal_cultivation_mod:rilun_hold_until";
    static final String RILUN_CHARGE = "immortal_cultivation_mod:rilun_charge";

    private static final ResourceLocation MAX_HEALTH_MODIFIER = id("dari_bati_max_health");
    private static final ResourceLocation MOVEMENT_MODIFIER = id("dari_bati_movement_speed");
    private static final ResourceLocation KNOCKBACK_MODIFIER = id("dari_bati_knockback_resistance");
    private static final ResourceLocation REACH_MODIFIER = id("dari_bati_reach");
    private static final DustParticleOptions SOLAR_GOLD =
            new DustParticleOptions(new Vector3f(1.0F, 0.64F, 0.04F), 1.15F);
    private static final DustParticleOptions SOLAR_WHITE =
            new DustParticleOptions(new Vector3f(1.0F, 0.95F, 0.62F), 1.30F);

    private DariBatiJue() {
    }

    public static void tick(ServerPlayer player) {
        ModAttachments.CultivationData data = ModAttachments.getData(player);
        boolean active = CultivationMethods.isDariBatiJue(data.activeCultivationMethod());
        if (!active) {
            if (isExtreme(player)) {
                finishExtreme(player);
            }
            clearRuntimeState(player);
            if (player.tickCount % 20 == 0 && CultivationMethods.solarPower(data) > 0.0F) {
                ModAttachments.setData(player, CultivationMethods.withSolarPower(
                        data, Math.max(0.0F, CultivationMethods.solarPower(data) - 1.0F)));
            }
            return;
        }

        if (player.tickCount % 5 == 0) {
            updateSolarPower(player, data);
            data = ModAttachments.getData(player);
        }

        long now = player.serverLevel().getGameTime();
        if (isExtreme(player) && player.getPersistentData().getLong(EXTREME_UNTIL) <= now) {
            finishExtreme(player);
            data = ModAttachments.getData(player);
        }

        float solar = CultivationMethods.solarPower(data);
        boolean peak = isDariLinshen(player, data);
        boolean extreme = isExtreme(player);
        applyAttributes(player, solar, peak, extreme);
        updateBodyMarker(player, solar, peak, extreme);

        if (solar >= 100.0F) {
            player.clearFire();
            player.setRemainingFireTicks(0);
        }
        if (player.tickCount % 20 == 0 && solar >= 35.0F && player.getHealth() < player.getMaxHealth()) {
            float regen = player.getMaxHealth() * (0.002F + solar / 100.0F * 0.003F);
            if (peak) regen = player.getMaxHealth() * 0.0075F;
            if (extreme) regen = player.getMaxHealth() * 0.01F;
            player.heal(Math.max(1.0F, regen));
        }

        tickTechniqueStates(player, solar, peak, extreme);
        emitAura(player, solar, peak, extreme);
        if (solar >= 70.0F && player.tickCount % 20 == 0) {
            meltNearbySnow(player);
            deterWeakMonsters(player);
        }
        if ((peak || extreme) && player.tickCount % 3 == 0) {
            burnWeakProjectiles(player, extreme ? 4.0D : 2.8D);
        }
    }

    private static void updateSolarPower(ServerPlayer player, ModAttachments.CultivationData data) {
        float current = CultivationMethods.solarPower(data);
        int proficiency = data.methodProficiency(CultivationMethods.DARI_BATI_JUE);
        int cap = CultivationMethods.solarPowerCap(data);
        if (player.hasEffect(ModEffects.XUYANG)) {
            cap = Math.min(cap, 50);
        }

        float target = naturalSolarTarget(player, proficiency);
        target = Math.min(cap, target);
        float gainPerTick = proficiency >= 1_000 ? 0.11F : 0.0875F;
        float lossPerTick = proficiency >= 1_000 ? 0.045F : 0.075F;
        float next = current < target
                ? Math.min(target, current + gainPerTick)
                : Math.max(target, current - lossPerTick);
        if (next > cap) {
            next = Math.max(cap, next - lossPerTick);
        }
        if (Math.abs(next - current) >= 0.005F) {
            ModAttachments.setData(player, CultivationMethods.withSolarPower(data, next));
        }
    }

    private static float naturalSolarTarget(ServerPlayer player, int proficiency) {
        ServerLevel level = player.serverLevel();
        if (!level.dimensionType().hasSkyLight() || !level.canSeeSky(player.blockPosition().above())) {
            return 0.0F;
        }
        long time = Math.floorMod(level.getDayTime() + (proficiency >= 1_000 ? 300L : 0L), 24_000L);
        double daylight = Math.sin(Math.PI * time / 12_000.0D);
        float base = daylight > 0.0D
                ? 10.0F + 90.0F * (float) daylight
                : 10.0F * (1.0F + (float) daylight);
        float weather;
        if (level.isThundering()) {
            weather = proficiency >= 1_000 ? 0.55F : 0.30F;
        } else if (level.isRaining()) {
            weather = proficiency >= 1_000 ? 0.80F : 0.60F;
        } else {
            weather = 1.0F;
        }
        return Mth.clamp(base * weather, 0.0F, 100.0F);
    }

    public static boolean isDariLinshen(ServerPlayer player, ModAttachments.CultivationData data) {
        if (!CultivationMethods.isDariBatiJue(data.activeCultivationMethod())
                || data.methodProficiency(CultivationMethods.DARI_BATI_JUE) < 500
                || CultivationMethods.solarPower(data) < 98.0F
                || !player.serverLevel().dimensionType().hasSkyLight()
                || !player.serverLevel().canSeeSky(player.blockPosition().above())) {
            return false;
        }
        long time = Math.floorMod(player.serverLevel().getDayTime(), 24_000L);
        int margin = 600;
        return time >= 6_000 - margin && time <= 6_000 + margin
                && !player.serverLevel().isThundering();
    }

    public static boolean isExtreme(LivingEntity entity) {
        return entity.getPersistentData().getLong(EXTREME_UNTIL) > entity.level().getGameTime();
    }

    public static boolean isHoldingSun(LivingEntity entity) {
        return entity.getPersistentData().getLong(RILUN_HOLD_UNTIL)
                > entity.level().getGameTime();
    }

    /** Entry point for future yang treasures and formations. */
    public static void addSolarPower(ServerPlayer player, float amount) {
        if (amount <= 0.0F) return;
        ModAttachments.CultivationData data = ModAttachments.getData(player);
        if (!CultivationMethods.isDariBatiJue(data.activeCultivationMethod())) return;
        int cap = player.hasEffect(ModEffects.XUYANG)
                ? Math.min(50, CultivationMethods.solarPowerCap(data))
                : CultivationMethods.solarPowerCap(data);
        ModAttachments.setData(player, CultivationMethods.withSolarPower(data,
                Math.min(cap, CultivationMethods.solarPower(data) + amount)));
    }

    public static boolean hasYangyan(LivingEntity entity) {
        return entity.getPersistentData().getLong(YANGYAN_UNTIL) > entity.level().getGameTime();
    }

    public static float adjustIncomingDamage(LivingEntity target, DamageSource source, float amount) {
        if (amount <= 0.0F) {
            return amount;
        }
        LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
        if (attacker instanceof ServerPlayer player && source.is(DamageTypes.PLAYER_ATTACK)) {
            ModAttachments.CultivationData attackerData = ModAttachments.getData(player);
            if (CultivationMethods.isDariBatiJue(attackerData.activeCultivationMethod())) {
                amount *= physicalDamageMultiplier(player, attackerData);
            }
            if (player.hasEffect(ModEffects.XUYANG)) {
                amount *= 0.70F;
            }
        }
        if (!(target instanceof ServerPlayer player)) {
            return amount;
        }
        ModAttachments.CultivationData data = ModAttachments.getData(player);
        if (!CultivationMethods.isDariBatiJue(data.activeCultivationMethod())) {
            return amount;
        }
        float solar = CultivationMethods.solarPower(data);
        boolean peak = isDariLinshen(player, data);
        boolean extreme = isExtreme(player);
        if (source.is(DamageTypeTags.IS_FIRE)) {
            float resistance = fireResistance(solar, peak || extreme);
            if (resistance >= 1.0F) {
                player.clearFire();
                return 0.0F;
            }
            amount *= 1.0F - resistance;
        }
        if (!source.is(DamageTypeTags.BYPASSES_RESISTANCE)
                && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            amount *= 1.0F - defenseBonus(solar, peak || extreme);
            if (extreme && !source.is(DamageTypes.PLAYER_ATTACK)
                    && !source.is(DamageTypes.MOB_ATTACK)) {
                amount *= 0.50F;
            }
        }
        return amount;
    }

    private static float physicalDamageMultiplier(
            ServerPlayer player, ModAttachments.CultivationData data) {
        if (isExtreme(player)) return 4.0F;
        if (isDariLinshen(player, data)) return 3.0F;
        float solar = CultivationMethods.solarPower(data);
        float bonus;
        if (solar <= 10.0F) bonus = solar * 0.01F;
        else if (solar <= 35.0F) bonus = lerp(solar, 10.0F, 35.0F, 0.10F, 0.35F);
        else if (solar <= 70.0F) bonus = lerp(solar, 35.0F, 70.0F, 0.35F, 0.70F);
        else bonus = lerp(solar, 70.0F, 100.0F, 0.70F, 1.50F);
        return 1.0F + bonus;
    }

    private static float defenseBonus(float solar, boolean peak) {
        if (peak) return 0.80F;
        if (solar <= 10.0F) return solar * 0.005F;
        if (solar <= 35.0F) return lerp(solar, 10, 35, 0.05F, 0.20F);
        if (solar <= 70.0F) return lerp(solar, 35, 70, 0.20F, 0.35F);
        return lerp(solar, 70, 100, 0.35F, 0.60F);
    }

    private static float fireResistance(float solar, boolean peak) {
        if (peak || solar >= 100.0F) return 1.0F;
        if (solar < 10.0F) return 0.0F;
        if (solar <= 35.0F) return lerp(solar, 10, 35, 0.0F, 0.25F);
        return lerp(solar, 35, 70, 0.25F, 0.75F);
    }

    public static void onMeleeDamageApplied(
            ServerPlayer player, LivingEntity target, DamageSource source, float damage) {
        if (damage <= 0.0F || !source.is(DamageTypes.PLAYER_ATTACK)) {
            return;
        }
        ModAttachments.CultivationData data = ModAttachments.getData(player);
        if (!CultivationMethods.isDariBatiJue(data.activeCultivationMethod())) {
            return;
        }
        float solar = CultivationMethods.solarPower(data);
        ServerLevel level = player.serverLevel();
        Vec3 center = target.getBoundingBox().getCenter();
        if (solar >= 35.0F) {
            SolarTechniques.emitRing(level, center, 0.8D + solar / 100.0D, 22,
                    ModParticles.METHOD_QI_LIGHT.get());
        }
        if (solar >= 35.0F) {
            Vec3 away = target.position().subtract(player.position());
            double strength = solar <= 70.0F
                    ? Mth.lerp((solar - 35.0F) / 35.0F, 0.05D, 0.40D)
                    : Mth.lerp((solar - 70.0F) / 30.0F, 0.40D, 1.0D);
            if (isExtreme(player)) strength = 1.30D;
            target.knockback(strength, -away.x, -away.z);
        }
        if (hasYangyan(player)) {
            float extra = SpellCharge.damage(
                    Math.max(12.0F, damage * (0.20F + solar / 250.0F)),
                    player.getPersistentData().getFloat(YANGYAN_CHARGE));
            target.hurt(player.damageSources().indirectMagic(player, player), extra);
            target.igniteForSeconds(2.0F + solar / 25.0F);
            level.sendParticles(ModParticles.METHOD_QI_FIRE.get(), center.x, center.y, center.z,
                    18, 0.35D, 0.45D, 0.35D, 0.08D);
        }
        boolean peak = isDariLinshen(player, data);
        if ((peak || isExtreme(player)) && CombatStateManager.isHeavyAttack(source)) {
            double radius = isExtreme(player) ? 5.0D : 3.0D;
            float shockDamage = damage * (isExtreme(player) ? 0.65F : 0.40F);
            for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class,
                    target.getBoundingBox().inflate(radius),
                    living -> living != player && living != target
                            && NpcProfile.isCombatEnemy(player, living))) {
                nearby.hurt(player.damageSources().indirectMagic(player, player), shockDamage);
                Vec3 away = nearby.position().subtract(center);
                nearby.knockback(isExtreme(player) ? 1.2D : 0.8D, -away.x, -away.z);
            }
            SolarTechniques.emitRing(level, center, radius, 56,
                    ModParticles.METHOD_QI_FIRE.get());
        }
    }

    private static void applyAttributes(ServerPlayer player, float solar, boolean peak, boolean extreme) {
        double speed = solar <= 10.0F ? solar * 0.005D
                : solar <= 35.0F ? lerp(solar, 10, 35, 0.05F, 0.15F)
                : solar <= 70.0F ? lerp(solar, 35, 70, 0.15F, 0.20F)
                : lerp(solar, 70, 100, 0.20F, 0.30F);
        double health = solar < 70.0F ? 0.0D : lerp(solar, 70, 100, 0.0F, 0.50F);
        double knockback = solar <= 10.0F ? solar * 0.005D
                : solar <= 70.0F ? lerp(solar, 10, 70, 0.05F, 0.40F) : 1.0D;
        if (peak || extreme) {
            health = 0.75D;
            knockback = 1.0D;
        }
        updateModifier(player, Attributes.MOVEMENT_SPEED, MOVEMENT_MODIFIER, speed,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        updateModifier(player, Attributes.MAX_HEALTH, MAX_HEALTH_MODIFIER, health,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        updateModifier(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_MODIFIER, knockback,
                AttributeModifier.Operation.ADD_VALUE);
        updateModifier(player, Attributes.ENTITY_INTERACTION_RANGE, REACH_MODIFIER,
                peak || extreme ? 1.0D : 0.0D, AttributeModifier.Operation.ADD_VALUE);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void updateModifier(
            LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id,
            double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) return;
        AttributeModifier existing = instance.getModifier(id);
        if (existing != null && Math.abs(existing.amount() - amount) < 0.0001D) return;
        if (Math.abs(amount) <= 0.0001D) {
            if (existing != null) instance.removeModifier(id);
        } else {
            // Replacing this in one operation is important for MAX_HEALTH. Removing
            // the old bonus first briefly drops max health to its base value, which
            // clamps current health and makes normal Solar Power decay look like a hit.
            instance.addOrUpdateTransientModifier(
                    new AttributeModifier(id, amount, operation));
        }
    }

    private static void updateBodyMarker(ServerPlayer player, float solar, boolean peak, boolean extreme) {
        if (solar < 70.0F && !peak && !extreme) {
            player.removeEffect(ModEffects.SOLAR_BODY);
            return;
        }
        int amplifier = extreme ? 2 : peak ? 1 : 0;
        MobEffectInstance current = player.getEffect(ModEffects.SOLAR_BODY);
        if (current == null || current.getAmplifier() != amplifier || current.getDuration() < 15) {
            player.addEffect(new MobEffectInstance(ModEffects.SOLAR_BODY, 40, amplifier,
                    false, false, false));
        }
    }

    private static void tickTechniqueStates(ServerPlayer player, float solar, boolean peak, boolean extreme) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (hasYangyan(player) && player.tickCount % 2 == 0) {
            Vec3 hand = player.getEyePosition().add(player.getLookAngle().scale(0.45D)).add(0.0D, -0.45D, 0.0D);
            level.sendParticles(ModParticles.METHOD_QI_FIRE.get(), hand.x, hand.y, hand.z,
                    3, 0.16D + solar / 500.0D, 0.16D, 0.16D, 0.02D);
            level.sendParticles(SOLAR_WHITE, hand.x, hand.y, hand.z,
                    1, 0.10D, 0.10D, 0.10D, 0.0D);
        } else if (!hasYangyan(player)) {
            player.getPersistentData().remove(YANGYAN_UNTIL);
            player.getPersistentData().remove(YANGYAN_CHARGE);
        }
        if (player.getPersistentData().getBoolean(LEAP_ACTIVE)) {
            if (!player.onGround()) player.getPersistentData().putBoolean(LEAP_AIRBORNE, true);
            Vec3 back = player.position().subtract(player.getDeltaMovement().normalize().scale(0.45D))
                    .add(0.0D, 0.5D, 0.0D);
            level.sendParticles(ModParticles.METHOD_QI_LIGHT.get(), back.x, back.y, back.z,
                    6, 0.18D, 0.20D, 0.18D, 0.06D);
            level.sendParticles(ParticleTypes.FLAME, back.x, back.y, back.z,
                    4, 0.16D, 0.16D, 0.16D, 0.035D);
            player.resetFallDistance();
            if (player.onGround() && player.getPersistentData().getBoolean(LEAP_AIRBORNE)) {
                player.getPersistentData().remove(LEAP_ACTIVE);
                player.getPersistentData().remove(LEAP_AIRBORNE);
                SolarTechniques.landingShockwave(player, 2.5D + solar / 50.0D,
                        45.0F + solar, player.getPersistentData().getFloat(LEAP_CHARGE));
                player.getPersistentData().remove(LEAP_CHARGE);
            }
        }
        if (player.getPersistentData().getLong(RILUN_HOLD_UNTIL) > now) {
            SolarTechniques.tickHeldSun(player, solar);
        } else {
            player.getPersistentData().remove(RILUN_HOLD_UNTIL);
            player.getPersistentData().remove(RILUN_CHARGE);
        }
        if (extreme) {
            if (player.tickCount % 10 == 0) {
                for (LivingEntity enemy : level.getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(1.7D),
                        target -> target != player && NpcProfile.isCombatEnemy(player, target))) {
                    enemy.hurt(player.damageSources().indirectMagic(player, player), 35.0F);
                    enemy.igniteForSeconds(2.0F);
                }
            }
            if (player.tickCount % 2 == 0) {
                SolarTechniques.emitHalo(level,
                        player.getEyePosition().add(player.getLookAngle().scale(-0.18D)),
                        player.getLookAngle(), 0.85D, 20, player.tickCount * 0.08D);
            }
        }
    }

    private static void emitAura(ServerPlayer player, float solar, boolean peak, boolean extreme) {
        if (solar < 10.0F || player.tickCount % (solar >= 70.0F ? 2 : 6) != 0) return;
        ServerLevel level = player.serverLevel();
        double width = player.getBbWidth() * (extreme ? 1.2D : 0.8D);
        int count = extreme ? 7 : peak ? 5 : solar >= 70.0F ? 3 : 1;
        level.sendParticles(solar >= 70.0F ? ModParticles.METHOD_QI_LIGHT.get() : SOLAR_GOLD,
                player.getX(), player.getY() + player.getBbHeight() * 0.55D, player.getZ(),
                count, width, player.getBbHeight() * 0.48D, width, 0.018D);
        if (solar >= 70.0F && player.tickCount % 5 == 0) {
            level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 0.12D, player.getZ(),
                    2, 0.28D, 0.05D, 0.28D, 0.015D);
        }
        if (solar >= 70.0F && player.tickCount % 4 == 0) {
            Vec3 look = player.getLookAngle().normalize();
            Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
            if (right.lengthSqr() < 1.0E-5D) right = new Vec3(1.0D, 0.0D, 0.0D);
            else right = right.normalize();
            Vec3 eyes = player.getEyePosition().add(look.scale(0.12D));
            for (double side : new double[]{-0.11D, 0.11D}) {
                Vec3 eye = eyes.add(right.scale(side));
                level.sendParticles(SOLAR_WHITE, eye.x, eye.y, eye.z,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        if ((peak || extreme) && player.isSprinting()) {
            Vec3 velocity = player.getDeltaMovement();
            Vec3 trail = player.position().subtract(velocity.lengthSqr() > 0.001D
                    ? velocity.normalize().scale(0.75D) : Vec3.ZERO).add(0.0D, 0.35D, 0.0D);
            level.sendParticles(ModParticles.METHOD_QI_FIRE.get(), trail.x, trail.y, trail.z,
                    extreme ? 8 : 4, 0.20D, 0.18D, 0.20D, 0.035D);
        }
        if (extreme && player.onGround() && player.tickCount % 4 == 0) {
            SolarTechniques.emitRing(level, player.position().add(0.0D, 0.04D, 0.0D),
                    0.8D, 18, SOLAR_GOLD);
            for (int ray = 0; ray < 7; ray++) {
                double angle = ray * Math.PI * 2.0D / 7.0D + player.getId() * 0.17D;
                for (int point = 1; point <= 3; point++) {
                    double distance = point * 0.55D;
                    Vec3 crack = player.position().add(
                            Math.cos(angle) * distance, 0.045D, Math.sin(angle) * distance);
                    level.sendParticles(SOLAR_GOLD, crack.x, crack.y, crack.z,
                            1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
        }
    }

    private static void meltNearbySnow(ServerPlayer player) {
        if (!BlockBreakingSettings.enabled(player,
                BlockBreakingSettings.Category.TECHNIQUES)) return;
        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();
        for (BlockPos cursor : BlockPos.betweenClosed(center.offset(-2, -1, -2), center.offset(2, 2, 2))) {
            if (level.getBlockState(cursor).is(Blocks.SNOW)
                    || level.getBlockState(cursor).is(Blocks.SNOW_BLOCK)) {
                BlockPos pos = cursor.immutable();
                BlockBreakingSettings.runWithSource(
                        BlockBreakingSettings.Category.TECHNIQUES, player,
                        () -> SpellDestruction.breakBlock(level, pos, false, player));
            } else if (level.getBlockState(cursor).is(Blocks.ICE)) {
                TerrainTransforms.replace(level, cursor, Blocks.WATER.defaultBlockState(), player,
                        BlockBreakingSettings.Category.TECHNIQUES);
            }
        }
    }

    private static void deterWeakMonsters(ServerPlayer player) {
        for (Monster monster : player.serverLevel().getEntitiesOfClass(
                Monster.class, player.getBoundingBox().inflate(6.0D),
                mob -> mob.getTarget() == player && mob.getMaxHealth() <= 40.0F)) {
            monster.setTarget(null);
            monster.getNavigation().stop();
            Vec3 away = monster.position().subtract(player.position());
            if (away.lengthSqr() > 0.01D) {
                monster.setDeltaMovement(monster.getDeltaMovement().add(away.normalize().scale(0.12D)));
            }
        }
    }

    private static void burnWeakProjectiles(ServerPlayer player, double radius) {
        AABB area = player.getBoundingBox().inflate(radius);
        for (Projectile projectile : player.serverLevel().getEntitiesOfClass(
                Projectile.class, area,
                p -> p instanceof AbstractArrow || p instanceof ThrowableProjectile)) {
            if (projectile.getOwner() == player) continue;
            if (projectile.getOwner() instanceof LivingEntity owner
                    && !NpcProfile.isCombatEnemy(player, owner)) continue;
            Vec3 point = projectile.position();
            player.serverLevel().sendParticles(ModParticles.METHOD_QI_FIRE.get(),
                    point.x, point.y, point.z, 8, 0.15D, 0.15D, 0.15D, 0.05D);
            projectile.discard();
        }
    }

    private static void finishExtreme(ServerPlayer player) {
        player.getPersistentData().remove(EXTREME_UNTIL);
        ModAttachments.CultivationData data = ModAttachments.getData(player);
        data = CultivationMethods.withSolarPower(data, 20.0F);
        data = data.withQi(Math.max(0, data.qi() / 2));
        ModAttachments.setData(player, data);
        player.addEffect(new MobEffectInstance(ModEffects.XUYANG, 20 * 30, 0,
                false, true, true));
        ServerEvents.syncPlayerData(player);
    }

    private static void clearRuntimeState(ServerPlayer player) {
        updateModifier(player, Attributes.MOVEMENT_SPEED, MOVEMENT_MODIFIER, 0.0D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        updateModifier(player, Attributes.MAX_HEALTH, MAX_HEALTH_MODIFIER, 0.0D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        updateModifier(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_MODIFIER, 0.0D,
                AttributeModifier.Operation.ADD_VALUE);
        updateModifier(player, Attributes.ENTITY_INTERACTION_RANGE, REACH_MODIFIER, 0.0D,
                AttributeModifier.Operation.ADD_VALUE);
        player.removeEffect(ModEffects.SOLAR_BODY);
        player.getPersistentData().remove(YANGYAN_UNTIL);
        player.getPersistentData().remove(YANGYAN_CHARGE);
        player.getPersistentData().remove(RILUN_HOLD_UNTIL);
        player.getPersistentData().remove(RILUN_CHARGE);
        player.getPersistentData().remove(LEAP_ACTIVE);
        player.getPersistentData().remove(LEAP_AIRBORNE);
        player.getPersistentData().remove(LEAP_CHARGE);
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static float lerp(float value, float from, float to, float low, float high) {
        return Mth.lerp(Mth.clamp((value - from) / (to - from), 0.0F, 1.0F), low, high);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, path);
    }
}
