package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.ElementalTechniqueAreaEntity;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import com.example.immortal_cultivation_mod.entity.SpellImpactParticles;
import com.example.immortal_cultivation_mod.entity.SpellTriggerProjectileEntity;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.ForcefieldCenterBehavior;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Real spell executions granted by 罡霆啸空录. */
public final class GangtingTechniques {
    private static final Set<String> SPELLS = Set.of(
            ModSpells.LEIXIAO_GUANYUN,
            ModSpells.JIUXIAO_FENGLEI_YU);

    private GangtingTechniques() {
    }

    public static boolean isTechnique(String spellId) {
        return SPELLS.contains(ModSpells.normalizeId(spellId));
    }

    public static boolean isPiercingProjectile(String spellId) {
        return ModSpells.LEIXIAO_GUANYUN.equals(ModSpells.normalizeId(spellId));
    }

    public static boolean canCast(ServerPlayer player, String spellId) {
        String normalized = ModSpells.normalizeId(spellId);
        if (!SPELLS.contains(normalized)) return false;
        ModAttachments.CultivationData data = ModAttachments.getData(player);
        if (!CultivationMethods.isGangtingXiaokongLu(data.activeCultivationMethod())) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".gangting_wrong_method"), true);
            return false;
        }
        return true;
    }

    public static boolean cast(
            ServerPlayer player, String spellId, float chargeScale,
            Consumer<Entity> projectileHook) {
        String normalized = ModSpells.normalizeId(spellId);
        if (!canCast(player, normalized)) return false;
        float charge = SpellCharge.clamp(chargeScale);
        float momentum = GangtingXiaokongLu.momentum(player);
        boolean cast = switch (normalized) {
            case ModSpells.LEIXIAO_GUANYUN -> castLeixiao(
                    player, charge, momentum, projectileHook);
            case ModSpells.JIUXIAO_FENGLEI_YU -> castDomain(
                    player, charge, momentum, projectileHook);
            default -> false;
        };
        if (!cast) return false;
        if (ModSpells.JIUXIAO_FENGLEI_YU.equals(normalized)) {
            GangtingXiaokongLu.consumeMomentum(player, 35.0F);
        }
        return true;
    }

    private static boolean castLeixiao(
            ServerPlayer player, float charge, float momentum,
            Consumer<Entity> projectileHook) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(look.scale(0.75D));
        SpellTriggerProjectileEntity projectile = new SpellTriggerProjectileEntity(
                level, player, ModSpells.LEIXIAO_GUANYUN, charge);
        projectile.setMomentumSnapshot(momentum);
        projectile.setPos(start.x, start.y, start.z);
        projectile.shoot(look.x, look.y, look.z,
                SpellCharge.isFull(charge) ? 4.2F : 3.55F, 0.0F);
        projectileHook.accept(projectile);
        emitCastingCorridor(level, start, look, 0.58D, momentum);
        level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(),
                SoundSource.PLAYERS, 1.0F, 1.75F);
        return level.addFreshEntity(projectile);
    }

    private static boolean castDomain(
            ServerPlayer player, float charge, float momentum,
            Consumer<Entity> projectileHook) {
        if (player.isShiftKeyDown()) {
            return spawnDomain(player.serverLevel(), player, player.position(),
                    charge, momentum);
        }
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(look.scale(0.72D));
        SpellTriggerProjectileEntity projectile = new SpellTriggerProjectileEntity(
                level, player, ModSpells.JIUXIAO_FENGLEI_YU, charge);
        projectile.setMomentumSnapshot(momentum);
        projectile.setPos(start.x, start.y, start.z);
        projectile.shoot(look.x, look.y, look.z, 1.8F, 0.0F);
        projectileHook.accept(projectile);
        emitDomainCasting(level, start, look, momentum);
        level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(),
                SoundSource.PLAYERS, 0.85F, 1.45F);
        return level.addFreshEntity(projectile);
    }

    public static void activateTriggeredSpell(
            ServerLevel level, LivingEntity caster, String spellId, Vec3 impact,
            LivingEntity hitTarget, float chargeScale, float momentum) {
        if (ModSpells.JIUXIAO_FENGLEI_YU.equals(ModSpells.normalizeId(spellId))) {
            Vec3 center = hitTarget == null ? impact : hitTarget.position();
            spawnDomain(level, caster, center, chargeScale, momentum);
        }
    }

    private static boolean spawnDomain(
            ServerLevel level, LivingEntity caster, Vec3 center,
            float chargeScale, float momentum) {
        ElementalTechniqueAreaEntity domain = ElementalTechniqueAreaEntity.create(
                level, caster, ElementalTechniqueAreaEntity.Kind.JIUXIAO_FENGLEI_YU,
                center, caster.getLookAngle(), null, chargeScale);
        if (domain == null) return false;
        domain.setMomentumSnapshot(momentum);
        SpellImpactParticles.shockRing(level, center.add(0.0D, 0.12D, 0.0D),
                ModParticles.METHOD_QI_WIND.get(), 0.85D, 38, 0.24D);
        level.sendParticles(ModParticles.METHOD_QI_THUNDER.get(),
                center.x, center.y + 1.0D, center.z,
                35, 1.2D, 1.0D, 1.2D, 0.10D);
        return level.addFreshEntity(domain);
    }

    public static void tickTriggerProjectile(SpellTriggerProjectileEntity projectile) {
        String spellId = projectile.spellId();
        if (!isTechnique(spellId) || !(projectile.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 motion = projectile.getDeltaMovement();
        Vec3 direction = motion.lengthSqr() < 1.0E-6D
                ? new Vec3(0.0D, 0.0D, 1.0D) : motion.normalize();
        Vec3 right = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-6D) right = new Vec3(1.0D, 0.0D, 0.0D);
        else right = right.normalize();
        Vec3 up = right.cross(direction).normalize();
        int arms = ModSpells.LEIXIAO_GUANYUN.equals(spellId) ? 3 : 6;
        double radius = ModSpells.LEIXIAO_GUANYUN.equals(spellId) ? 0.24D : 0.48D;
        double phase = projectile.tickCount * 0.58D;
        for (int i = 0; i < arms; i++) {
            double angle = phase + Math.PI * 2.0D * i / arms;
            Vec3 point = projectile.position()
                    .add(right.scale(Math.cos(angle) * radius))
                    .add(up.scale(Math.sin(angle) * radius));
            level.sendParticles(i % 2 == 0 ? ModParticles.METHOD_QI_WIND.get()
                            : ModParticles.METHOD_QI_THUNDER.get(),
                    point.x, point.y, point.z, 1,
                    0.0D, 0.0D, 0.0D, 0.0D);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                projectile.getX(), projectile.getY(), projectile.getZ(),
                projectile.momentumSnapshot() >= 75.0F ? 3 : 1,
                0.08D, 0.08D, 0.08D, 0.035D);
        if (ModSpells.JIUXIAO_FENGLEI_YU.equals(spellId)
                && projectile.tickCount % 3 == 0) {
            level.sendParticles(ParticleTypes.GUST,
                    projectile.getX(), projectile.getY(), projectile.getZ(),
                    1, 0.10D, 0.10D, 0.10D, 0.0D);
        }
    }

    public static void hitPiercingProjectile(
            ServerLevel level, LivingEntity caster,
            SpellTriggerProjectileEntity projectile,
            LivingEntity target, int penetrationIndex) {
        float base = 520.0F * (float) Math.pow(0.82D, penetrationIndex);
        float adjusted = damageWithSnapshot(caster, base,
                ModSpells.LEIXIAO_GUANYUN, projectile.chargeScale(),
                projectile.momentumSnapshot());
        if (!target.hurt(level.damageSources().indirectMagic(projectile, caster), adjusted)) {
            return;
        }
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                20, 1, false, true, true));
        target.addEffect(new MobEffectInstance(ModEffects.DAZE,
                8, 0, false, true, true));
        Vec3 center = target.getBoundingBox().getCenter();
        level.sendParticles(ModParticles.METHOD_QI_THUNDER.get(),
                center.x, center.y, center.z, 22,
                0.32D, target.getBbHeight() * 0.36D, 0.32D, 0.14D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                center.x, center.y, center.z, 18,
                0.30D, target.getBbHeight() * 0.34D, 0.30D, 0.16D);
        level.sendParticles(ParticleTypes.FLASH,
                center.x, center.y, center.z, 1,
                0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.SMOKE,
                center.x, center.y, center.z, 10,
                target.getBbWidth() * 0.22D,
                target.getBbHeight() * 0.30D,
                target.getBbWidth() * 0.22D, 0.015D);
        emitAxisRing(level, center, projectile.getDeltaMovement(),
                0.95D, 30, ModParticles.METHOD_QI_WIND.get(), 0.09D);
        maybeBranch(level, caster, target, projectile, adjusted);
        if (projectile.momentumSnapshot() >= 100.0F) {
            thunderGust(level, caster, center, 3.2D, 0.34D, 40.0F);
        }
        level.playSound(null, target.blockPosition(), SoundEvents.TRIDENT_HIT,
                SoundSource.PLAYERS, 0.9F, 1.7F);
    }

    private static void maybeBranch(
            ServerLevel level, LivingEntity caster, LivingEntity struck,
            SpellTriggerProjectileEntity projectile, float primaryDamage) {
        float momentum = projectile.momentumSnapshot();
        if (momentum < 50.0F) return;
        float chance = momentum >= 100.0F ? 1.0F : momentum >= 75.0F ? 0.65F : 0.30F;
        if (level.random.nextFloat() > chance) return;
        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class, struck.getBoundingBox().inflate(5.0D),
                target -> target != caster && target != struck && target.isAlive()
                        && NpcProfile.isCombatEnemy(caster, target)
                        && !ForcefieldCenterBehavior.interceptAttackPath(
                        caster, struck.getBoundingBox().getCenter(),
                        target.getBoundingBox().getCenter(), primaryDamage * 0.55F));
        LivingEntity branch = candidates.stream()
                .min(Comparator.comparingDouble(struck::distanceToSqr)).orElse(null);
        if (branch == null) return;
        branch.hurt(level.damageSources().indirectMagic(projectile, caster),
                primaryDamage * 0.55F);
        emitWindSpiral(level, struck.getBoundingBox().getCenter(),
                branch.getBoundingBox().getCenter());
        ElementalSpellVfx.line(level, struck.getBoundingBox().getCenter(),
                branch.getBoundingBox().getCenter(), ParticleTypes.ELECTRIC_SPARK, 0.18D);
        ElementalSpellVfx.line(level, struck.getBoundingBox().getCenter(),
                branch.getBoundingBox().getCenter(),
                ModParticles.METHOD_QI_THUNDER.get(), 0.28D);
    }

    public static int maximumPiercedTargets(float momentum) {
        return momentum >= 100.0F ? 6 : momentum >= 75.0F ? 5 : 4;
    }

    public static float triggerBarrierDamage(String spellId, float chargeScale) {
        float base = ModSpells.JIUXIAO_FENGLEI_YU.equals(ModSpells.normalizeId(spellId))
                ? 650.0F : 520.0F;
        return SpellCharge.damage(base, chargeScale);
    }

    public static float damageWithSnapshot(
            LivingEntity caster, float base, String spellId,
            float chargeScale, float momentumSnapshot) {
        float damage = SpellDamageHelper.damage(caster,
                SpellCharge.damage(base, chargeScale), ModSpells.get(spellId));
        float liveMultiplier = GangtingXiaokongLu.thunderPowerMultiplier(caster);
        float snapshotMultiplier = momentumSnapshot >= 100.0F ? 1.25F
                : momentumSnapshot >= 75.0F ? 1.15F : 1.0F;
        return damage * snapshotMultiplier / Math.max(1.0F, liveMultiplier);
    }

    public static void thunderGust(
            ServerLevel level, LivingEntity caster, Vec3 center,
            double radius, double strength, float damage) {
        SpellImpactParticles.shockRing(level, center.add(0.0D, 0.10D, 0.0D),
                ModParticles.METHOD_QI_WIND.get(), 0.65D, 34, 0.30D);
        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class, new AABB(center, center).inflate(radius),
                target -> target != caster && NpcProfile.isCombatEnemy(caster, target))) {
            if (ForcefieldCenterBehavior.interceptAttackPath(
                    caster, center, target.getBoundingBox().getCenter(), damage)) {
                continue;
            }
            if (damage > 0.0F) {
                com.example.immortal_cultivation_mod.combat.CombatFeedbackEvents.hurtWithFlags(
                        target, level.damageSources().indirectMagic(caster, caster), damage,
                        com.example.immortal_cultivation_mod.network.CombatFeedbackPayloads.LIGHTNING);
            }
            Vec3 away = target.position().subtract(center);
            if (away.lengthSqr() < 1.0E-5D) {
                away = new Vec3(0.0D, 0.0D, 1.0D);
            }
            away = away.normalize();
            target.push(away.x * strength, 0.18D, away.z * strength);
            target.hurtMarked = true;
        }
    }

    private static void emitCastingCorridor(
            ServerLevel level, Vec3 center, Vec3 axis,
            double radius, float momentum) {
        axis = axis.lengthSqr() < 1.0E-6D
                ? new Vec3(0.0D, 0.0D, 1.0D) : axis.normalize();
        Vec3 right = axis.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-6D) right = new Vec3(1.0D, 0.0D, 0.0D);
        else right = right.normalize();
        Vec3 up = right.cross(axis).normalize();
        double[] corridor = {0.35D, 1.5D, 3.1D, 5.3D, 8.2D, 11.8D, 16.0D, 20.5D};
        for (int ring = 0; ring < corridor.length; ring++) {
            Vec3 ringCenter = center.add(axis.scale(corridor[ring]));
            int points = ring < 3 ? 22 : 16;
            double ringRadius = radius * (0.52D + ring * 0.055D);
            for (int i = 0; i < points; i++) {
                double angle = Math.PI * 2.0D * i / points + ring * 0.42D;
                Vec3 point = ringCenter.add(right.scale(Math.cos(angle) * ringRadius))
                        .add(up.scale(Math.sin(angle) * ringRadius));
                level.sendParticles((i + ring) % 3 == 0
                                ? ModParticles.METHOD_QI_THUNDER.get()
                                : ModParticles.METHOD_QI_WIND.get(),
                        point.x, point.y, point.z, 1,
                        -right.x * Math.cos(angle) * 0.035D
                                - up.x * Math.sin(angle) * 0.035D,
                        -right.y * Math.cos(angle) * 0.035D
                                - up.y * Math.sin(angle) * 0.035D,
                        -right.z * Math.cos(angle) * 0.035D
                                - up.z * Math.sin(angle) * 0.035D, 0.0D);
            }
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                center.x, center.y, center.z,
                Mth.clamp(8 + Math.round(momentum / 10.0F), 8, 18),
                radius * 0.55D, radius * 0.55D, radius * 0.55D, 0.12D);
    }

    private static void emitDomainCasting(
            ServerLevel level, Vec3 center, Vec3 axis, float momentum) {
        for (int ring = 0; ring < 3; ring++) {
            emitAxisRing(level, center.add(axis.normalize().scale(ring * 0.22D)), axis,
                    0.42D + ring * 0.20D, 24,
                    ring == 1 ? ModParticles.METHOD_QI_THUNDER.get()
                            : ModParticles.METHOD_QI_WIND.get(),
                    0.025D);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                center.x, center.y, center.z,
                Mth.clamp(6 + Math.round(momentum / 15.0F), 6, 14),
                0.44D, 0.44D, 0.44D, 0.09D);
    }

    public static void finishLeixiao(
            ServerLevel level, Vec3 center, Vec3 axis, float momentum) {
        for (int ring = 0; ring < 4; ring++) {
            emitAxisRing(level, center.add(axis.scale(ring * 0.14D)), axis,
                    0.45D + ring * 0.28D, 26,
                    ModParticles.METHOD_QI_WIND.get(), 0.10D + ring * 0.025D);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                center.x, center.y, center.z,
                momentum >= 75.0F ? 28 : 18,
                0.42D, 0.42D, 0.42D, 0.14D);
        level.playSound(null, net.minecraft.core.BlockPos.containing(center),
                SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS,
                1.15F, 1.55F);
    }

    private static void emitAxisRing(
            ServerLevel level, Vec3 center, Vec3 axis,
            double radius, int points, net.minecraft.core.particles.ParticleOptions particle,
            double outwardSpeed) {
        Vec3 direction = axis.lengthSqr() < 1.0E-6D
                ? new Vec3(0.0D, 0.0D, 1.0D) : axis.normalize();
        Vec3 right = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-6D) right = new Vec3(1.0D, 0.0D, 0.0D);
        else right = right.normalize();
        Vec3 up = right.cross(direction).normalize();
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;
            Vec3 radial = right.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
            Vec3 point = center.add(radial.scale(radius));
            level.sendParticles(particle, point.x, point.y, point.z, 1,
                    radial.x * outwardSpeed, radial.y * outwardSpeed,
                    radial.z * outwardSpeed, 0.0D);
        }
    }

    private static void emitWindSpiral(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 line = end.subtract(start);
        double length = line.length();
        if (length < 1.0E-4D) return;
        Vec3 axis = line.normalize();
        Vec3 right = axis.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-6D) right = new Vec3(1.0D, 0.0D, 0.0D);
        else right = right.normalize();
        Vec3 up = right.cross(axis).normalize();
        int samples = Math.max(12, (int) Math.ceil(length / 0.18D));
        for (int i = 0; i <= samples; i++) {
            double t = i / (double) samples;
            double angle = t * Math.PI * 5.0D;
            double radius = 0.18D * (1.0D - t * 0.55D);
            Vec3 point = start.add(line.scale(t))
                    .add(right.scale(Math.cos(angle) * radius))
                    .add(up.scale(Math.sin(angle) * radius));
            level.sendParticles(ModParticles.METHOD_QI_WIND.get(),
                    point.x, point.y, point.z, 1,
                    0.0D, 0.0D, 0.0D, 0.0D);
        }
    }
}
