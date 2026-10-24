package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.effect.NativeSpellEffects;
import com.example.immortal_cultivation_mod.entity.CultivationNpc;
import com.example.immortal_cultivation_mod.entity.DingshenProjectileEntity;
import com.example.immortal_cultivation_mod.entity.FireballProjectileEntity;
import com.example.immortal_cultivation_mod.entity.IgniteFlareProjectileEntity;
import com.example.immortal_cultivation_mod.entity.JinmangChuanshaProjectileEntity;
import com.example.immortal_cultivation_mod.entity.LightningProjectileEntity;
import com.example.immortal_cultivation_mod.entity.LingzhiBulletProjectileEntity;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import com.example.immortal_cultivation_mod.entity.SlidingWaterProjectileEntity;
import com.example.immortal_cultivation_mod.entity.SmokeProjectileEntity;
import com.example.immortal_cultivation_mod.entity.SpellImpactParticles;
import com.example.immortal_cultivation_mod.entity.WindBladeProjectileEntity;
import com.example.immortal_cultivation_mod.entity.XinliaoFireballEntity;
import com.example.immortal_cultivation_mod.entity.ZhenshanPalmEntity;
import com.example.immortal_cultivation_mod.sound.ModSounds;
import java.util.function.Consumer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Actor-neutral spell effects shared by player payload handling and cultivator AI.
 * Validation, qi payment, learned-spell checks, and cooldowns stay with the caller;
 * this class owns the real projectile/effect implementation.
 */
public final class CultivatorSpellExecution {
    private static final Consumer<Entity> NO_PROJECTILE_HOOK = ignored -> { };

    private CultivatorSpellExecution() {
    }

    public static boolean fireball(LivingEntity caster, LivingEntity target, float chargeScale) {
        return fireball(caster, target, chargeScale, NO_PROJECTILE_HOOK);
    }

    public static boolean fireball(
            LivingEntity caster,
            LivingEntity target,
            float chargeScale,
            Consumer<Entity> projectileHook) {
        FireballProjectileEntity projectile = new FireballProjectileEntity(caster.level(), caster);
        projectile.setChargeScale(areaScale(chargeScale));
        if (!launch(caster, target, projectile, 1.5F, 1.0F, projectileHook)) {
            return false;
        }
        caster.level().playSound(
                null,
                caster.getX(),
                caster.getY(),
                caster.getZ(),
                ModSounds.FIREBALL_CAST.get(),
                caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE,
                1.0F,
                1.0F);
        return true;
    }

    public static boolean igniteFlare(LivingEntity caster, LivingEntity target, float chargeScale) {
        return igniteFlare(caster, target, chargeScale, NO_PROJECTILE_HOOK);
    }

    public static boolean igniteFlare(
            LivingEntity caster,
            LivingEntity target,
            float chargeScale,
            Consumer<Entity> projectileHook) {
        IgniteFlareProjectileEntity projectile = new IgniteFlareProjectileEntity(caster.level(), caster);
        projectile.getPersistentData().putFloat("ChargeScale", areaScale(chargeScale));
        return launch(caster, target, projectile, 1.8F, 0.0F, projectileHook);
    }

    public static boolean lingzhiBullet(LivingEntity caster, LivingEntity target, float chargeScale) {
        return lingzhiBullet(caster, target, chargeScale, NO_PROJECTILE_HOOK);
    }

    public static boolean lingzhiBullet(
            LivingEntity caster,
            LivingEntity target,
            float chargeScale,
            Consumer<Entity> projectileHook) {
        LingzhiBulletProjectileEntity projectile = new LingzhiBulletProjectileEntity(caster.level(), caster);
        projectile.getPersistentData().putFloat("ChargeScale", areaScale(chargeScale));
        return launch(caster, target, projectile, 2.1F, 0.0F, projectileHook);
    }

    public static boolean jinmangChuansha(LivingEntity caster, LivingEntity target) {
        return jinmangChuansha(caster, target, NO_PROJECTILE_HOOK);
    }

    public static boolean jinmangChuansha(
            LivingEntity caster,
            LivingEntity target,
            Consumer<Entity> projectileHook) {
        JinmangChuanshaProjectileEntity projectile =
                new JinmangChuanshaProjectileEntity(caster.level(), caster);
        boolean launched = launch(caster, target, projectile, 4.5F, 0.0F, projectileHook);
        if (launched && caster.level() instanceof ServerLevel level) {
            Vec3 start = caster.getEyePosition();
            Vec3 desiredEnd = target == null
                    ? start.add(caster.getLookAngle().scale(24.0D))
                    : predictedPosition(caster, target, 0.35D);
            Vec3 offset = desiredEnd.subtract(start);
            double length = Math.min(24.0D, offset.length());
            Vec3 direction = offset.lengthSqr() < 1.0E-6D
                    ? caster.getLookAngle() : offset.normalize();
            for (double distance = 0.6D; distance <= length; distance += 0.75D) {
                Vec3 point = start.add(direction.scale(distance));
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        point.x, point.y, point.z, 1,
                        0.005D, 0.005D, 0.005D, 0.0D);
            }
        }
        return launched;
    }

    public static boolean windBlade(LivingEntity caster, LivingEntity target, float chargeScale) {
        return windBlade(caster, target, chargeScale, NO_PROJECTILE_HOOK);
    }

    public static boolean windBlade(
            LivingEntity caster,
            LivingEntity target,
            float chargeScale,
            Consumer<Entity> projectileHook) {
        WindBladeProjectileEntity projectile = new WindBladeProjectileEntity(caster.level(), caster);
        projectile.getPersistentData().putFloat("ChargeScale", areaScale(chargeScale));
        return launch(caster, target, projectile, 1.9F, 0.0F, projectileHook);
    }

    public static boolean slidingWater(LivingEntity caster, LivingEntity target, float chargeScale) {
        return slidingWater(caster, target, chargeScale, NO_PROJECTILE_HOOK);
    }

    public static boolean slidingWater(
            LivingEntity caster,
            LivingEntity target,
            float chargeScale,
            Consumer<Entity> projectileHook) {
        SlidingWaterProjectileEntity projectile = new SlidingWaterProjectileEntity(caster.level(), caster);
        projectile.getPersistentData().putFloat("ChargeScale", areaScale(chargeScale));
        return launch(caster, target, projectile, 1.6F, 0.0F, projectileHook);
    }

    public static boolean smokeArt(LivingEntity caster, LivingEntity target, float chargeScale) {
        return smokeArt(caster, target, chargeScale, NO_PROJECTILE_HOOK);
    }

    public static boolean smokeArt(
            LivingEntity caster,
            LivingEntity target,
            float chargeScale,
            Consumer<Entity> projectileHook) {
        SmokeProjectileEntity projectile = new SmokeProjectileEntity(caster.level(), caster);
        projectile.getPersistentData().putFloat("ChargeScale", areaScale(chargeScale));
        return launch(caster, target, projectile, 1.4F, 0.2F, projectileHook);
    }

    public static boolean dingshen(LivingEntity caster, LivingEntity target, float chargeScale) {
        return dingshen(caster, target, chargeScale, NO_PROJECTILE_HOOK);
    }

    public static boolean dingshen(
            LivingEntity caster,
            LivingEntity target,
            float chargeScale,
            Consumer<Entity> projectileHook) {
        DingshenProjectileEntity projectile = new DingshenProjectileEntity(caster.level(), caster);
        projectile.getPersistentData().putFloat("ChargeScale", areaScale(chargeScale));
        return launch(caster, target, projectile, 1.9F, 0.0F, projectileHook);
    }

    public static boolean lightning(
            LivingEntity caster,
            LivingEntity target,
            boolean greater,
            float chargeScale) {
        return lightning(caster, target, greater, chargeScale, NO_PROJECTILE_HOOK);
    }

    public static boolean lightning(
            LivingEntity caster,
            LivingEntity target,
            boolean greater,
            float chargeScale,
            Consumer<Entity> projectileHook) {
        LightningProjectileEntity projectile = new LightningProjectileEntity(caster.level(), caster, greater);
        projectile.setChargeScale(areaScale(chargeScale));
        boolean launched = launch(
                caster,
                target,
                projectile,
                greater ? 3.2F : 2.4F,
                greater ? 0.2F : 0.4F,
                projectileHook);
        if (launched && caster.level() instanceof ServerLevel serverLevel) {
            SpellImpactParticles.lightning(
                    serverLevel,
                    caster.getEyePosition().add(caster.getLookAngle().scale(0.9D)));
        }
        return launched;
    }

    public static boolean zhenshanPalm(LivingEntity caster, float chargeScale) {
        return zhenshanPalm(caster, chargeScale, NO_PROJECTILE_HOOK);
    }

    public static boolean zhenshanPalm(
            LivingEntity caster,
            float chargeScale,
            Consumer<Entity> projectileHook) {
        ZhenshanPalmEntity palm = new ZhenshanPalmEntity(caster.level(), caster, areaScale(chargeScale));
        projectileHook.accept(palm);
        return caster.level().addFreshEntity(palm);
    }

    public static boolean jinhuoXinliao(LivingEntity caster, LivingEntity target, float chargeScale) {
        return jinhuoXinliao(caster, target, chargeScale, NO_PROJECTILE_HOOK);
    }

    public static boolean jinhuoXinliao(
            LivingEntity caster,
            LivingEntity target,
            float chargeScale,
            Consumer<Entity> projectileHook) {
        XinliaoFireballEntity projectile = new XinliaoFireballEntity(caster.level(), caster, target);
        projectile.setVisualScale(areaScale(chargeScale));
        Vec3 origin = caster.getEyePosition().add(0.0D, 0.25D, 0.0D);
        projectile.setPos(origin.x, origin.y, origin.z);
        if (target == null) {
            projectile.setDeltaMovement(Vec3.ZERO);
            projectileHook.accept(projectile);
            return caster.level().addFreshEntity(projectile);
        }
        return launch(caster, target, projectile, 1.35F, 0.0F, projectileHook);
    }

    public static boolean liuguangJianying(LivingEntity caster, float chargeScale) {
        return LiuguangJianying.cast(caster, chargeScale);
    }

    public static boolean suiguiXingyun(LivingEntity caster, LivingEntity target, float chargeScale) {
        return SuiguiXingyun.cast(
                caster, target, areaScale(chargeScale), NO_PROJECTILE_HOOK);
    }

    public static boolean lightBeamVolley(LivingEntity caster, LivingEntity target) {
        Vec3 direction = target == null
                ? caster.getLookAngle()
                : predictedPosition(caster, target, 0.35D).subtract(caster.getEyePosition());
        return LightBeamAttack.prepareAndFireVolley(caster, direction);
    }

    public static boolean applyRegeneration(LivingEntity caster) {
        return applyRegeneration(caster, ModSpells.REGENERATION);
    }

    public static boolean canApplyRegeneration(LivingEntity caster) {
        return caster.isAlive() && caster.getHealth() < caster.getMaxHealth()
                && !caster.hasEffect(ModEffects.SPRING_RECOVERY) && !caster.hasEffect(MobEffects.HEAL);
    }

    public static boolean applyRegeneration(LivingEntity caster, String spellId) {
        if (!canApplyRegeneration(caster)) {
            return false;
        }
        if (!ModSpells.REGENERATION.equals(ModSpells.normalizeId(spellId))
                && !HealingTechniques.isTechnique(spellId)) return false;
        return caster.addEffect(new MobEffectInstance(ModEffects.SPRING_RECOVERY, 20,
                com.example.immortal_cultivation_mod.attachment.CultivationLevels
                        .getStageIndex(ModSpells.realmLevel(spellId))));
    }

    public static boolean applyQiGathering(LivingEntity caster) {
        if (caster.hasEffect(ModEffects.QI_GATHERING)) {
            return false;
        }
        caster.addEffect(new MobEffectInstance(
                ModEffects.QI_GATHERING,
                MobEffectInstance.INFINITE_DURATION,
                0,
                false,
                false,
                true));
        if (caster.level() instanceof ServerLevel serverLevel && !(caster instanceof ServerPlayer)) {
            serverLevel.sendParticles(
                    ParticleTypes.ENCHANT,
                    caster.getX(),
                    caster.getY() + caster.getBbHeight() * 0.55D,
                    caster.getZ(),
                    24,
                    0.45D,
                    0.5D,
                    0.45D,
                    0.035D);
        }
        return true;
    }

    public static boolean applyCleanse(LivingEntity caster) {
        var harmful = caster.getActiveEffects().stream()
                .filter(effect -> effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
                .map(MobEffectInstance::getEffect)
                .toList();
        if (harmful.isEmpty()) {
            return false;
        }
        harmful.forEach(caster::removeEffect);
        return true;
    }

    public static boolean applyLingbeng(LivingEntity caster, int durationTicks) {
        MobEffectInstance current = caster.getEffect(ModEffects.LINGBENG);
        int amplifier = current == null ? 0 : Math.min(9, current.getAmplifier() + 1);
        int duration = Math.max(20, durationTicks) + (current == null ? 0 : current.getDuration());
        caster.addEffect(new MobEffectInstance(
                ModEffects.LINGBENG,
                duration,
                amplifier,
                false,
                false,
                true));
        return true;
    }

    public static boolean releaseLingbeng(LivingEntity caster, Vec3 impact) {
        MobEffectInstance lingbeng = caster.getEffect(ModEffects.LINGBENG);
        if (lingbeng == null || !(caster.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        float radius = 2.0F + Math.min(9, lingbeng.getAmplifier());
        caster.removeEffect(ModEffects.LINGBENG);
        NativeSpellEffects.lingbengExplosion(serverLevel, impact, radius);
        if (caster instanceof CultivationNpc) {
            com.example.immortal_cultivation_mod.spell.SpellDestruction.explode(serverLevel,caster, null, npcEnemyOnlyExplosion(caster), impact,
                    radius, false, Level.ExplosionInteraction.BLOCK);
        } else {
            com.example.immortal_cultivation_mod.spell.SpellDestruction.explode(caster.level(),
                    caster,
                    impact.x,
                    impact.y,
                    impact.z,
                    radius,
                    Level.ExplosionInteraction.BLOCK);
        }
        return true;
    }

    private static ExplosionDamageCalculator npcEnemyOnlyExplosion(LivingEntity owner) {
        return new ExplosionDamageCalculator() {
            private boolean canAffect(Entity entity) {
                return entity instanceof LivingEntity target && NpcProfile.isCombatEnemy(owner, target);
            }

            @Override
            public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
                return canAffect(entity);
            }

            @Override
            public float getKnockbackMultiplier(Entity entity) {
                return canAffect(entity) ? super.getKnockbackMultiplier(entity) : 0.0F;
            }
        };
    }

    private static boolean launch(
            LivingEntity caster,
            LivingEntity target,
            Projectile projectile,
            float speed,
            float inaccuracy,
            Consumer<Entity> projectileHook) {
        if (target == null) {
            projectile.shootFromRotation(caster, caster.getXRot(), caster.getYRot(), 0.0F, speed, inaccuracy);
        } else {
            Vec3 aim = predictedPosition(caster, target, 0.35D).subtract(projectile.position());
            projectile.shoot(aim.x, aim.y, aim.z, speed, inaccuracy);
        }
        projectileHook.accept(projectile);
        return caster.level().addFreshEntity(projectile);
    }

    private static Vec3 predictedPosition(LivingEntity caster, LivingEntity target, double lead) {
        double distance = Math.sqrt(caster.distanceToSqr(target));
        double scaledLead = Math.min(1.25D, lead + distance * 0.012D);
        return target.getEyePosition().add(target.getDeltaMovement().scale(scaledLead));
    }

    private static float areaScale(float chargeScale) {
        return Math.max(1.0F, Math.min(2.0F, chargeScale));
    }
}
