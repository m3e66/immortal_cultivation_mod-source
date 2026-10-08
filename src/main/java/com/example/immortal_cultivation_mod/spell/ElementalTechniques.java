package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.ElementalTechniqueAreaEntity;
import com.example.immortal_cultivation_mod.entity.ElementalTechniqueProjectileEntity;
import com.example.immortal_cultivation_mod.entity.ModelBonePartEntity;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import com.example.immortal_cultivation_mod.entity.PetalTechniqueEntity;
import com.example.immortal_cultivation_mod.entity.SpellImpactParticles;
import com.example.immortal_cultivation_mod.entity.SpellTriggerProjectileEntity;
import com.example.immortal_cultivation_mod.entity.SwordTechniqueProjectileEntity;
import com.example.immortal_cultivation_mod.entity.TianyunFenchengMeteorEntity;
import com.example.immortal_cultivation_mod.entity.YaoguangFlashEntity;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.ForcefieldCenterBehavior;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Actor-neutral execution for the elemental spell collection. */
public final class ElementalTechniques {
    public static final String LIGHTNING_DASH_PROTECTION_UNTIL =
            "ImmortalCultivationLightningDashProtectionUntil";
    public static final String LIGHTNING_DASH_DAMAGE_REDUCTION =
            "ImmortalCultivationLightningDashDamageReduction";
    private static final Consumer<Entity> NO_PROJECTILE_HOOK = ignored -> { };
    private static final Set<String> SPELLS = Set.of(
            ModSpells.JINFENG_ZHI,
            ModSpells.WANREN_CHUANXIN,
            ModSpells.JINGJI_TUSHENG,
            ModSpells.WANTENG_JIAOSHA,
            ModSpells.YANBAO_DAN,
            ModSpells.CHIYAN_LONGJUAN,
            ModSpells.TIANYUN_FENCHENG,
            ModSpells.LUOHUA_CHANSHEN,
            ModSpells.BAIHUA_ZANG,
            ModSpells.POKONG_JIANHEN,
            ModSpells.SANJIAN_ZHUIMING,
            ModSpells.DIYAN_QIANG,
            ModSpells.SHANBENG_YIN,
            ModSpells.SHUIREN_LIANZHAN,
            ModSpells.CANGLANG_CHONG,
            ModSpells.LEISHAN,
            ModSpells.WULEI_HONGDING,
            ModSpells.BINGZHUI_YU,
            ModSpells.HANYU_FENGDI,
            ModSpells.YAOGUANG_SHAN,
            ModSpells.TIANGUANG_GUANSHA,
            ModSpells.YINGXI,
            ModSpells.TIANFENG_ZHUI);

    private ElementalTechniques() {
    }

    public static boolean isTechnique(String spellId) {
        return SPELLS.contains(ModSpells.normalizeId(spellId));
    }

    public static boolean canCast(LivingEntity caster, LivingEntity suppliedTarget, String spellId) {
        return canCast(caster, suppliedTarget, spellId, SpellCharge.NORMAL);
    }

    public static boolean canCast(
            LivingEntity caster,
            LivingEntity suppliedTarget,
            String spellId,
            float chargeScale) {
        String normalized = ModSpells.normalizeId(spellId);
        if (!SPELLS.contains(normalized)) {
            return false;
        }
        if (ModSpells.YINGXI.equals(normalized)) {
            return true;
        }
        if (ModSpells.WULEI_HONGDING.equals(normalized)) {
            return true;
        }
        if (ModSpells.LUOHUA_CHANSHEN.equals(normalized)) {
            return true;
        }
        if (ModSpells.POKONG_JIANHEN.equals(normalized)) {
            return isSwordWeapon(caster);
        }
        if (ModSpells.SANJIAN_ZHUIMING.equals(normalized)) {
            return isSwordWeapon(caster);
        }
        return true;
    }

    public static boolean cast(LivingEntity caster, LivingEntity target, String spellId) {
        return cast(caster, target, spellId, SpellCharge.NORMAL, NO_PROJECTILE_HOOK);
    }

    public static boolean cast(
            LivingEntity caster,
            LivingEntity suppliedTarget,
            String spellId,
            Consumer<Entity> projectileHook) {
        return cast(caster, suppliedTarget, spellId, SpellCharge.NORMAL, projectileHook);
    }

    public static boolean cast(
            LivingEntity caster,
            LivingEntity suppliedTarget,
            String spellId,
            float chargeScale,
            Consumer<Entity> projectileHook) {
        if (!(caster.level() instanceof ServerLevel level)) {
            return false;
        }
        float charge = SpellCharge.clamp(chargeScale);
        boolean fullCharge = SpellCharge.isFull(charge);
        String normalized = ModSpells.normalizeId(spellId);
        if ((ModSpells.POKONG_JIANHEN.equals(normalized)
                || ModSpells.SANJIAN_ZHUIMING.equals(normalized))
                && !isSwordWeapon(caster)) {
            return false;
        }
        LivingEntity target = validEnemyTarget(caster, suppliedTarget);
        if (!usesTriggerProjectile(normalized)) {
            ElementalSpellVfx.cast(level, caster, target, normalized);
        }
        return switch (normalized) {
            case ModSpells.JINFENG_ZHI -> projectile(level, caster, target,
                    ElementalTechniqueProjectileEntity.Kind.JINFENG_ZHI,
                    fullCharge ? 4.80F : 3.65F, 0.0F, charge, projectileHook);
            case ModSpells.WANREN_CHUANXIN -> castSixBlades(
                    level, caster, target, charge, projectileHook);
            case ModSpells.JINGJI_TUSHENG -> triggerProjectile(
                    level, caster, target, normalized, charge, projectileHook);
            case ModSpells.WANTENG_JIAOSHA -> triggerProjectile(
                    level, caster, target, normalized, charge, projectileHook);
            case ModSpells.YANBAO_DAN -> projectile(level, caster, target,
                    ElementalTechniqueProjectileEntity.Kind.YANBAO_DAN,
                    1.65F, 0.0F, charge, projectileHook);
            case ModSpells.CHIYAN_LONGJUAN -> triggerProjectile(
                    level, caster, target, normalized, charge, projectileHook);
            case ModSpells.TIANYUN_FENCHENG,
                    ModSpells.LUOHUA_CHANSHEN -> triggerProjectile(
                    level, caster, target, normalized, charge, projectileHook);
            case ModSpells.BAIHUA_ZANG -> triggerProjectile(
                    level, caster, target, normalized, charge, projectileHook);
            case ModSpells.POKONG_JIANHEN -> castSpaceScar(
                    level, caster, target, charge, projectileHook);
            case ModSpells.SANJIAN_ZHUIMING -> triggerProjectile(
                    level, caster, target, normalized, charge, projectileHook);
            case ModSpells.DIYAN_QIANG -> triggerProjectile(
                    level, caster, target, normalized, charge, projectileHook);
            case ModSpells.SHANBENG_YIN -> triggerProjectile(
                    level, caster, target, normalized, charge, projectileHook);
            case ModSpells.SHUIREN_LIANZHAN -> castWaterCrescents(
                    level, caster, target, charge, projectileHook);
            case ModSpells.CANGLANG_CHONG -> castWave(level, caster, charge, projectileHook);
            case ModSpells.LEISHAN -> castLightningDash(level, caster, charge);
            case ModSpells.WULEI_HONGDING -> triggerProjectile(
                    level, caster, target, normalized, charge, projectileHook);
            case ModSpells.BINGZHUI_YU -> triggerProjectile(
                    level, caster, target, normalized, charge, projectileHook);
            case ModSpells.HANYU_FENGDI -> spawnArea(level, caster, caster.position(),
                    caster.getLookAngle(), null,
                    ElementalTechniqueAreaEntity.Kind.HANYU_FENGDI, charge, projectileHook);
            case ModSpells.YAOGUANG_SHAN -> castLightFlash(level, caster, charge);
            case ModSpells.TIANGUANG_GUANSHA -> triggerProjectile(
                    level, caster, target, normalized, charge, projectileHook);
            case ModSpells.YINGXI -> triggerProjectile(
                    level, caster, target, normalized, charge, projectileHook);
            case ModSpells.TIANFENG_ZHUI -> triggerProjectile(
                    level, caster, target, normalized, charge, projectileHook);
            default -> false;
        };
    }

    private static boolean projectile(
            ServerLevel level,
            LivingEntity caster,
            LivingEntity target,
            ElementalTechniqueProjectileEntity.Kind kind,
            float speed,
            float inaccuracy,
            float chargeScale,
            Consumer<Entity> hook) {
        ElementalTechniqueProjectileEntity projectile =
                new ElementalTechniqueProjectileEntity(level, caster, kind);
        projectile.setChargeScale(chargeScale);
        Vec3 start = caster.getEyePosition().add(caster.getLookAngle().scale(0.55D));
        projectile.setPos(start.x, start.y, start.z);
        Vec3 aim = target == null
                ? caster.getLookAngle()
                : target.getEyePosition().add(target.getDeltaMovement().scale(0.35D)).subtract(start);
        projectile.shoot(aim.x, aim.y, aim.z, speed, inaccuracy);
        hook.accept(projectile);
        return level.addFreshEntity(projectile);
    }

    private static boolean triggerProjectile(
            ServerLevel level,
            LivingEntity caster,
            LivingEntity target,
            String spellId,
            float chargeScale,
            Consumer<Entity> hook) {
        SpellTriggerProjectileEntity projectile =
                new SpellTriggerProjectileEntity(level, caster, spellId, chargeScale);
        Vec3 start = caster.getEyePosition().add(caster.getLookAngle().scale(0.55D));
        projectile.setPos(start.x, start.y, start.z);
        Vec3 aim = target == null
                ? caster.getLookAngle()
                : target.getEyePosition().add(target.getDeltaMovement().scale(0.35D)).subtract(start);
        projectile.shoot(aim.x, aim.y, aim.z, 3.25F, 0.0F);
        hook.accept(projectile);
        return level.addFreshEntity(projectile);
    }

    private static boolean castSixBlades(
            ServerLevel level,
            LivingEntity caster,
            LivingEntity suppliedTarget,
            float chargeScale,
            Consumer<Entity> hook) {
        boolean fullCharge = SpellCharge.isFull(chargeScale);
        LivingEntity target = validEnemyTarget(caster, suppliedTarget);
        UUID castId = UUID.randomUUID();
        Vec3 look = caster.getLookAngle().normalize();
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        if (right.lengthSqr() < 1.0E-5D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 up = right.cross(look).normalize();
        boolean spawned = false;
        for (int index = 0; index < 6; index++) {
            double angle = Math.PI * 2.0D * index / 6.0D;
            Vec3 offset = right.scale(Math.cos(angle) * 1.35D)
                    .add(up.scale(Math.sin(angle) * 1.35D));
            Vec3 position = caster.getEyePosition().add(look.scale(0.25D)).add(offset);
            ElementalTechniqueProjectileEntity blade =
                    new ElementalTechniqueProjectileEntity(
                            level, caster, ElementalTechniqueProjectileEntity.Kind.WANREN_CHUANXIN);
            blade.setChargeScale(chargeScale);
            blade.setPos(position.x, position.y, position.z);
            blade.configureBlade(castId, target, 8 + index * 2);
            hook.accept(blade);
            spawned |= level.addFreshEntity(blade);
        }
        level.playSound(null, caster.blockPosition(), SoundEvents.IRON_TRAPDOOR_OPEN,
                caster instanceof net.minecraft.server.level.ServerPlayer
                        ? SoundSource.PLAYERS : SoundSource.HOSTILE,
                1.0F, 1.65F);
        return spawned;
    }

    private static boolean castWaterCrescents(
            ServerLevel level,
            LivingEntity caster,
            LivingEntity target,
            float chargeScale,
            Consumer<Entity> hook) {
        boolean fullCharge = SpellCharge.isFull(chargeScale);
        boolean spawned = false;
        for (int index = -1; index <= 1; index++) {
            ElementalTechniqueProjectileEntity projectile =
                    new ElementalTechniqueProjectileEntity(
                            level, caster, ElementalTechniqueProjectileEntity.Kind.SHUIREN_LIANZHAN);
            projectile.setChargeScale(chargeScale);
            Vec3 start = caster.getEyePosition().add(caster.getLookAngle().scale(0.55D));
            projectile.setPos(start.x, start.y + index * 0.10D, start.z);
            Vec3 aim = target == null
                    ? caster.getLookAngle()
                    : target.getEyePosition().subtract(start);
            aim = aim.normalize().yRot((float) Math.toRadians(
                    index * (fullCharge ? 2.25D : 4.5D)));
            projectile.shoot(aim.x, aim.y, aim.z, fullCharge ? 2.75F : 2.15F, 0.0F);
            hook.accept(projectile);
            spawned |= level.addFreshEntity(projectile);
        }
        return spawned;
    }

    private static boolean activateMeteor(
            ServerLevel level,
            LivingEntity caster,
            Vec3 impact,
            float chargeScale) {
        Vec3 ground = groundPosition(caster, impact);
        TianyunFenchengMeteorEntity meteor =
                new TianyunFenchengMeteorEntity(level, caster, ground, chargeScale);
        return level.addFreshEntity(meteor);
    }

    private static boolean castLuohua(
            ServerLevel level,
            LivingEntity caster,
            LivingEntity target,
            float chargeScale,
            Consumer<Entity> hook) {
        PetalTechniqueEntity petals =
                PetalTechniqueEntity.createLuohua(level, caster, target, chargeScale);
        if (petals == null) {
            return false;
        }
        hook.accept(petals);
        return level.addFreshEntity(petals);
    }

    private static boolean castSpaceScar(
            ServerLevel level,
            LivingEntity caster,
            LivingEntity target,
            float chargeScale,
            Consumer<Entity> hook) {
        if (!isSwordWeapon(caster)) {
            return false;
        }
        if (target != null && caster.distanceToSqr(target) <= 3.35D * 3.35D
                && caster.hasLineOfSight(target)) {
            caster.swing(InteractionHand.MAIN_HAND, true);
            if (caster instanceof Player player) {
                player.attack(target);
            } else if (caster instanceof Mob mob) {
                mob.doHurtTarget(target);
            }
        }
        int phase = Math.floorMod((int) (level.getGameTime() + caster.getId()), 3);
        float roll = (phase - 1) * 24.0F - caster.getXRot() * 0.22F;
        SwordTechniqueProjectileEntity scar =
                SwordTechniqueProjectileEntity.createPokong(
                        level, caster, target, chargeScale, roll);
        hook.accept(scar);
        return level.addFreshEntity(scar);
    }

    private static boolean castPursuitSwords(
            ServerLevel level,
            LivingEntity caster,
            LivingEntity target,
            float chargeScale,
            Consumer<Entity> hook) {
        if (!isSwordWeapon(caster)) {
            return false;
        }
        UUID castId = UUID.randomUUID();
        boolean spawned = false;
        for (int index = 0; index < 3; index++) {
            SwordTechniqueProjectileEntity sword =
                    SwordTechniqueProjectileEntity.createPursuitSword(
                            level, caster, target, chargeScale, castId, index);
            hook.accept(sword);
            spawned |= level.addFreshEntity(sword);
        }
        return spawned;
    }

    private static boolean castWave(
            ServerLevel level,
            LivingEntity caster,
            float chargeScale,
            Consumer<Entity> hook) {
        Vec3 direction = horizontal(caster.getLookAngle());
        Vec3 start = caster.position().add(direction.scale(1.5D));
        return spawnArea(level, caster, start, direction, null,
                ElementalTechniqueAreaEntity.Kind.CANGLANG_CHONG, chargeScale, hook);
    }

    private static boolean spawnArea(
            ServerLevel level,
            LivingEntity caster,
            Vec3 position,
            Vec3 direction,
            LivingEntity target,
            ElementalTechniqueAreaEntity.Kind kind,
            float chargeScale,
            Consumer<Entity> hook) {
        ElementalTechniqueAreaEntity effect = ElementalTechniqueAreaEntity.create(
                level, caster, kind, position, direction, target, chargeScale);
        if (effect == null) {
            return false;
        }
        hook.accept(effect);
        return level.addFreshEntity(effect);
    }

    private static boolean usesTriggerProjectile(String spellId) {
        return switch (ModSpells.normalizeId(spellId)) {
            case ModSpells.JINGJI_TUSHENG,
                    ModSpells.WANTENG_JIAOSHA,
                    ModSpells.CHIYAN_LONGJUAN,
                    ModSpells.TIANYUN_FENCHENG,
                    ModSpells.LUOHUA_CHANSHEN,
                    ModSpells.BAIHUA_ZANG,
                    ModSpells.SANJIAN_ZHUIMING,
                    ModSpells.DIYAN_QIANG,
                    ModSpells.SHANBENG_YIN,
                    ModSpells.WULEI_HONGDING,
                    ModSpells.BINGZHUI_YU,
                    ModSpells.TIANGUANG_GUANSHA,
                    ModSpells.YINGXI,
                    ModSpells.TIANFENG_ZHUI -> true;
            default -> false;
        };
    }

    /** Activates the original spell logic only after its invisible projectile hits. */
    public static boolean activateTriggeredSpell(
            ServerLevel level,
            LivingEntity caster,
            String spellId,
            Vec3 impact,
            LivingEntity hitTarget) {
        return activateTriggeredSpell(
                level, caster, spellId, impact, hitTarget, SpellCharge.NORMAL);
    }

    public static boolean activateTriggeredSpell(
            ServerLevel level,
            LivingEntity caster,
            String spellId,
            Vec3 impact,
            LivingEntity hitTarget,
            float chargeScale) {
        float charge = SpellCharge.clamp(chargeScale);
        String normalized = ModSpells.normalizeId(spellId);
        LivingEntity usefulTarget = hitTarget != null
                && NpcProfile.isCombatEnemy(caster, hitTarget) ? hitTarget : null;
        return switch (normalized) {
            case ModSpells.JINGJI_TUSHENG -> activateArea(level, caster,
                    centeredGroundImpact(impact, usefulTarget), usefulTarget,
                    ElementalTechniqueAreaEntity.Kind.JINGJI_TUSHENG, true, normalized, charge);
            case ModSpells.WANTENG_JIAOSHA -> activateArea(level, caster, impact, usefulTarget,
                    ElementalTechniqueAreaEntity.Kind.WANTENG_JIAOSHA, true, normalized, charge);
            case ModSpells.CHIYAN_LONGJUAN -> activateArea(level, caster, impact, usefulTarget,
                    ElementalTechniqueAreaEntity.Kind.CHIYAN_LONGJUAN, true, normalized, charge);
            case ModSpells.TIANYUN_FENCHENG -> activateMeteor(
                    level, caster, centeredGroundImpact(impact, usefulTarget), charge);
            case ModSpells.LUOHUA_CHANSHEN -> usefulTarget != null
                    && castLuohua(level, caster, usefulTarget, charge, NO_PROJECTILE_HOOK);
            case ModSpells.BAIHUA_ZANG -> activateBaihua(
                    level, caster, impact, normalized, charge);
            case ModSpells.SANJIAN_ZHUIMING -> usefulTarget != null
                    && castPursuitSwords(
                    level, caster, usefulTarget, charge, NO_PROJECTILE_HOOK);
            case ModSpells.DIYAN_QIANG -> activateArea(level, caster,
                    centeredGroundImpact(impact, usefulTarget), usefulTarget,
                    ElementalTechniqueAreaEntity.Kind.DIYAN_QIANG, true, normalized, charge);
            case ModSpells.SHANBENG_YIN -> activateArea(level, caster,
                    centeredGroundImpact(impact, usefulTarget), usefulTarget,
                    ElementalTechniqueAreaEntity.Kind.SHANBENG_YIN, true, normalized, charge);
            case ModSpells.WULEI_HONGDING -> usefulTarget != null
                    && activateArea(level, caster, usefulTarget.position(), usefulTarget,
                    ElementalTechniqueAreaEntity.Kind.WULEI_HONGDING, true, normalized, charge);
            case ModSpells.BINGZHUI_YU -> activateArea(level, caster, impact, usefulTarget,
                    ElementalTechniqueAreaEntity.Kind.BINGZHUI_YU, true, normalized, charge);
            case ModSpells.TIANGUANG_GUANSHA -> activateArea(level, caster, impact, usefulTarget,
                    ElementalTechniqueAreaEntity.Kind.TIANGUANG_GUANSHA, true, normalized, charge);
            case ModSpells.YINGXI -> usefulTarget != null
                    && activateShadowStrike(level, caster, usefulTarget, normalized, charge);
            case ModSpells.TIANFENG_ZHUI -> activateArea(level, caster, impact, usefulTarget,
                    ElementalTechniqueAreaEntity.Kind.TIANFENG_ZHUI, false, normalized, charge);
            default -> false;
        };
    }

    private static boolean activateArea(
            ServerLevel level,
            LivingEntity caster,
            Vec3 impact,
            LivingEntity target,
            ElementalTechniqueAreaEntity.Kind kind,
            boolean ground,
            String spellId,
            float chargeScale) {
        Vec3 position = ground ? groundPosition(caster, impact) : impact;
        boolean spawned = spawnArea(level, caster, position, caster.getLookAngle(),
                target, kind, chargeScale, NO_PROJECTILE_HOOK);
        if (spawned) {
            ElementalSpellVfx.castAtImpact(level, caster, position, spellId);
        }
        return spawned;
    }

    private static boolean activateBaihua(
            ServerLevel level,
            LivingEntity caster,
            Vec3 impact,
            String spellId,
            float chargeScale) {
        Vec3 position = groundPosition(caster, impact);
        PetalTechniqueEntity field =
                PetalTechniqueEntity.createBaihua(level, caster, position, chargeScale);
        boolean spawned = field != null && level.addFreshEntity(field);
        if (spawned) {
            ElementalSpellVfx.castAtImpact(level, caster, position, spellId);
        }
        return spawned;
    }

    private static Vec3 centeredGroundImpact(Vec3 impact, LivingEntity target) {
        return target == null ? impact : target.position();
    }

    private static boolean activateShadowStrike(
            ServerLevel level,
            LivingEntity caster,
            LivingEntity target,
            String spellId,
            float chargeScale) {
        if (findShadowDestination(
                level, caster, target, SpellCharge.isFull(chargeScale)) == null) {
            return false;
        }
        ElementalSpellVfx.castAtImpact(level, caster, target.getBoundingBox().getCenter(), spellId);
        return castShadowStrike(level, caster, target, chargeScale);
    }

    public static float triggerBarrierDamage(String spellId) {
        return triggerBarrierDamage(spellId, SpellCharge.NORMAL);
    }

    public static float triggerBarrierDamage(String spellId, float chargeScale) {
        float base = switch (ModSpells.normalizeId(spellId)) {
            case ModSpells.SHANBENG_YIN -> 900.0F;
            case ModSpells.TIANYUN_FENCHENG -> 1600.0F;
            case ModSpells.BAIHUA_ZANG -> 640.0F;
            case ModSpells.TIANGUANG_GUANSHA -> 950.0F;
            case ModSpells.TIANFENG_ZHUI -> 600.0F;
            case ModSpells.YINGXI -> 100.0F;
            default -> 180.0F;
        };
        return SpellCharge.damage(base, chargeScale);
    }

    private static boolean castLightningDash(
            ServerLevel level, LivingEntity caster, float chargeScale) {
        boolean fullCharge = SpellCharge.isFull(chargeScale);
        double dashRange = (fullCharge ? 12.0D : 8.0D)
                * (GangtingXiaokongLu.momentum(caster) >= 50.0F ? 1.25D : 1.0D);
        Vec3 start = caster.position();
        Vec3 look = caster.getLookAngle().normalize();
        Vec3 eye = caster.getEyePosition();
        BlockHitResult block = level.clip(new ClipContext(
                eye, eye.add(look.scale(dashRange)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        double distance = block.getType() == HitResult.Type.BLOCK
                ? Math.max(0.0D, eye.distanceTo(block.getLocation()) - 0.65D) : dashRange;
        Vec3 end = start.add(look.scale(distance));
        if (ForcefieldCenterBehavior.interceptAttackPath(caster, eye,
                end.add(0.0D, caster.getEyeHeight(), 0.0D), 55.0F)) {
            return true;
        }
        AABB path = caster.getBoundingBox().expandTowards(end.subtract(start)).inflate(0.65D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, path,
                target -> target != caster && NpcProfile.isCombatEnemy(caster, target)
                        && target.getBoundingBox().inflate(0.55D).clip(start, end).isPresent())) {
            if (com.example.immortal_cultivation_mod.combat.CombatFeedbackEvents.hurtWithFlags(
                    target, caster.damageSources().indirectMagic(caster, caster),
                    SpellDamageHelper.damage(caster,
                            SpellCharge.damage(55.0F, chargeScale),
                            ModSpells.get(ModSpells.LEISHAN)),
                    com.example.immortal_cultivation_mod.network.CombatFeedbackPayloads.LIGHTNING)) {
                Vec3 center = target.getBoundingBox().getCenter();
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        center.x, center.y, center.z,
                        22, 0.36D, target.getBbHeight() * 0.36D, 0.36D, 0.14D);
                level.sendParticles(ModParticles.METHOD_QI_THUNDER.get(),
                        center.x, center.y, center.z,
                        16, 0.30D, target.getBbHeight() * 0.32D, 0.30D, 0.09D);
            }
        }
        caster.getPersistentData().putLong(
                LIGHTNING_DASH_PROTECTION_UNTIL, level.getGameTime() + 6L);
        caster.getPersistentData().putFloat(
                LIGHTNING_DASH_DAMAGE_REDUCTION, fullCharge ? 0.70F : 0.50F);
        caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,
                6, 0, false, false, false));
        emitLine(level, start.add(0.0D, caster.getBbHeight() * 0.5D, 0.0D),
                end.add(0.0D, caster.getBbHeight() * 0.5D, 0.0D),
                ModParticles.METHOD_QI_THUNDER.get(), 0.22D);
        emitLightningAfterimages(level, caster, start, end);
        caster.teleportTo(end.x, end.y, end.z);
        com.example.immortal_cultivation_mod.combat.CombatFeedbackEvents.dashTravel(caster, start, end, fullCharge ? .8F : .5F);
        caster.setDeltaMovement(look.scale(fullCharge ? 0.78D : 0.45D));
        caster.hurtMarked = true;
        level.playSound(null, caster.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT,
                caster instanceof net.minecraft.server.level.ServerPlayer
                        ? SoundSource.PLAYERS : SoundSource.HOSTILE,
                0.8F, 1.7F);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                end.x, end.y + 0.08D, end.z,
                18, 0.45D, 0.12D, 0.45D, 0.12D);
        return true;
    }

    private static void emitLightningAfterimages(
            ServerLevel level, LivingEntity caster, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        for (int image = 1; image <= 3; image++) {
            Vec3 base = start.add(delta.scale(image / 4.0D));
            for (int row = 0; row < 7; row++) {
                double y = caster.getBbHeight() * row / 7.0D;
                double width = row < 2 ? 0.24D : row > 5 ? 0.14D : 0.34D;
                level.sendParticles(ModParticles.METHOD_QI_THUNDER.get(),
                        base.x, base.y + y, base.z,
                        2, width, 0.025D, width, 0.0D);
            }
        }
        Vec3 previous = start.add(0.0D, caster.getBbHeight() * 0.52D, 0.0D);
        for (int segment = 1; segment <= 16; segment++) {
            double progress = segment / 16.0D;
            Vec3 next = start.lerp(end, progress)
                    .add((level.random.nextDouble() - 0.5D) * 0.28D,
                            caster.getBbHeight() * 0.52D
                                    + (level.random.nextDouble() - 0.5D) * 0.22D,
                            (level.random.nextDouble() - 0.5D) * 0.28D);
            emitLine(level, previous, next, ParticleTypes.ELECTRIC_SPARK, 0.12D);
            previous = next;
        }
    }

    private static boolean castLightFlash(
            ServerLevel level, LivingEntity caster, float chargeScale) {
        boolean fullCharge = SpellCharge.isFull(chargeScale);
        double radius = fullCharge ? 8.0D : 6.0D;
        YaoguangFlashEntity flash = com.example.immortal_cultivation_mod.entity.ModEntities
                .YAOGUANG_FLASH.get().create(level);
        if (flash != null) {
            Vec3 center = caster.getBoundingBox().getCenter();
            flash.setRadius((float) radius);
            flash.moveTo(center.x, center.y, center.z);
            level.addFreshEntity(flash);
        }
        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class, caster.getBoundingBox().inflate(radius),
                target -> target != caster && NpcProfile.isCombatEnemy(caster, target))) {
            float base = isDark(target) ? 50.0F : 20.0F;
            if (!target.hurt(caster.damageSources().indirectMagic(caster, caster),
                    SpellDamageHelper.damage(caster,
                            SpellCharge.damage(base, chargeScale),
                            ModSpells.get(ModSpells.YAOGUANG_SHAN)))) {
                continue;
            }
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,
                    fullCharge ? 80 : 50, 0, false, true, true));
            level.sendParticles(ModParticles.METHOD_QI_LIGHT.get(),
                    target.getX(), target.getEyeY() + 0.16D, target.getZ(),
                    7, 0.30D, 0.18D, 0.30D, 0.025D);
            if (target instanceof Mob mob && mob.getTarget() == caster) {
                mob.setTarget(null);
                mob.getNavigation().stop();
            }
            if (fullCharge) {
                boolean controlled = target.addEffect(new MobEffectInstance(
                        ModEffects.DAZE, 20, 0, false, true, true), caster);
                if (controlled) target.stopUsingItem();
                if (controlled && target instanceof ServerPlayer player) {
                    com.example.immortal_cultivation_mod.combat.CombatStateManager
                            .interruptSpellPreparation(player);
                }
                if (target instanceof com.example.immortal_cultivation_mod.entity.CultivationNpc npc) {
                    npc.endNpcCast();
                }
            }
            if (isDark(target)) {
                level.sendParticles(ModParticles.METHOD_QI_LIGHT.get(),
                        target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                        18, 0.32D, target.getBbHeight() * 0.35D, 0.32D, 0.075D);
                level.sendParticles(ParticleTypes.SMOKE,
                        target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ(),
                        10, 0.28D, target.getBbHeight() * 0.30D, 0.28D, 0.035D);
            }
        }
        level.sendParticles(ParticleTypes.FLASH,
                caster.getX(), caster.getEyeY(), caster.getZ(), 3, 0.3D, 0.3D, 0.3D, 0.0D);
        level.sendParticles(ModParticles.METHOD_QI_LIGHT.get(),
                caster.getX(), caster.getEyeY(), caster.getZ(), 80, 3.0D, 2.0D, 3.0D, 0.22D);
        SpellImpactParticles.shockRing(level,
                caster.position().add(0.0D, caster.getBbHeight() * 0.50D, 0.0D),
                ModParticles.METHOD_QI_LIGHT.get(), 0.45D, 42, 0.31D);
        level.playSound(null, caster.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                caster instanceof net.minecraft.server.level.ServerPlayer
                        ? SoundSource.PLAYERS : SoundSource.HOSTILE,
                1.0F, 1.8F);
        return true;
    }

    private static boolean castShadowStrike(
            ServerLevel level,
            LivingEntity caster,
            LivingEntity target,
            float chargeScale) {
        boolean fullCharge = SpellCharge.isFull(chargeScale);
        Vec3 destination = findShadowDestination(level, caster, target, fullCharge);
        if (destination == null) {
            return false;
        }
        Vec3 source = caster.getEyePosition();
        Vec3 targetEye = target.getEyePosition();
        if (ForcefieldCenterBehavior.interceptAttackPath(caster, source, targetEye, 100.0F)) {
            return true;
        }
        Vec3 old = caster.position();
        level.sendParticles(ModParticles.METHOD_QI_DARK.get(),
                old.x, old.y + 0.08D, old.z,
                24, 0.38D, 0.08D, 0.38D, 0.025D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                old.x, old.y + 0.15D, old.z,
                12, 0.32D, 0.12D, 0.32D, 0.02D);
        caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,
                fullCharge ? 10 : 6, 0, false, false, false));
        caster.teleportTo(destination.x, destination.y, destination.z);
        com.example.immortal_cultivation_mod.combat.CombatFeedbackEvents.spatialTravel(caster, old, destination, fullCharge ? .8F : .55F);
        caster.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,
                target.getEyePosition());
        Vec3 targetToCaster = horizontal(caster.position().subtract(target.position()));
        boolean behind = horizontal(target.getLookAngle()).dot(targetToCaster) < -0.45D;
        float base = behind ? 100.0F : 70.0F;
        target.hurt(caster.damageSources().indirectMagic(caster, caster),
                SpellDamageHelper.damage(caster,
                        SpellCharge.damage(base, chargeScale),
                        ModSpells.get(ModSpells.YINGXI)));
        Vec3 shadowStart = old.add(0.0D, 0.08D, 0.0D);
        Vec3 shadowEnd = destination.add(0.0D, 0.08D, 0.0D);
        emitLine(level, shadowStart, shadowEnd,
                ModParticles.METHOD_QI_DARK.get(), 0.55D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                destination.x, destination.y + 0.8D, destination.z,
                20, 0.35D, 0.65D, 0.35D, 0.04D);
        Vec3 strikeCenter = target.getBoundingBox().getCenter();
        Vec3 side = horizontal(target.getLookAngle())
                .cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        Vec3 slashStart = strikeCenter.subtract(side.scale(0.75D)).add(0.0D, -0.35D, 0.0D);
        Vec3 slashEnd = strikeCenter.add(side.scale(0.75D)).add(0.0D, 0.35D, 0.0D);
        emitLine(level, slashStart, slashEnd, ModParticles.METHOD_QI_DARK.get(), 0.10D);
        if (behind) {
            level.sendParticles(ModParticles.METHOD_QI_DARK.get(),
                    strikeCenter.x, strikeCenter.y, strikeCenter.z,
                    24, 0.38D, 0.48D, 0.38D, 0.12D);
        }
        return true;
    }

    private static Vec3 findShadowDestination(
            ServerLevel level,
            LivingEntity caster,
            LivingEntity target,
            boolean expandedSearch) {
        Vec3 source = caster.getEyePosition();
        Vec3 targetEye = target.getEyePosition();
        BlockHitResult obstruction = level.clip(new ClipContext(
                source, targetEye, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        if (!expandedSearch && obstruction.getType() == HitResult.Type.BLOCK
                && source.distanceToSqr(obstruction.getLocation())
                + 0.25D < source.distanceToSqr(targetEye)) {
            return null;
        }
        Vec3 destination = target.position().subtract(horizontal(target.getLookAngle()).scale(1.35D));
        Vec3 movement = destination.subtract(caster.position());
        if (level.noCollision(caster, caster.getBoundingBox().move(movement))) {
            return destination;
        }
        destination = target.position().add(
                horizontal(caster.position().subtract(target.position())).scale(1.2D));
        movement = destination.subtract(caster.position());
        if (level.noCollision(caster, caster.getBoundingBox().move(movement))) {
            return destination;
        }
        if (!expandedSearch) {
            return null;
        }

        Vec3 forward = horizontal(target.getLookAngle());
        for (int index = 0; index < 8; index++) {
            Vec3 offset = forward.yRot((float) (Math.PI * 2.0D * index / 8.0D))
                    .scale(1.45D);
            Vec3 candidate = target.position().add(offset);
            Vec3 candidateMovement = candidate.subtract(caster.position());
            if (!level.noCollision(caster, caster.getBoundingBox().move(candidateMovement))) {
                continue;
            }
            Vec3 candidateEye = candidate.add(0.0D, caster.getEyeHeight(), 0.0D);
            BlockHitResult path = level.clip(new ClipContext(
                    source, candidateEye,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
            if (path.getType() != HitResult.Type.BLOCK
                    || source.distanceToSqr(path.getLocation()) + 0.25D
                    >= source.distanceToSqr(candidateEye)) {
                return candidate;
            }
        }
        return null;
    }

    private static LivingEntity validEnemyTarget(
            LivingEntity caster, LivingEntity suppliedTarget) {
        return suppliedTarget != null && suppliedTarget.isAlive()
                && suppliedTarget.level() == caster.level()
                && NpcProfile.isCombatEnemy(caster, suppliedTarget)
                ? suppliedTarget : null;
    }

    private static boolean isSwordWeapon(LivingEntity caster) {
        var stack = caster.getMainHandItem();
        if (stack.isEmpty()) {
            return false;
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        boolean saber = path.equals("dao") || path.startsWith("dao_")
                || path.endsWith("_dao") || path.contains("saber");
        return !saber && (stack.is(ItemTags.SWORDS)
                || path.contains("sword") || path.contains("jian")
                || path.contains("feijian"));
    }

    private static Vec3 groundPosition(LivingEntity caster, Vec3 point) {
        BlockPos cursor = BlockPos.containing(point);
        for (int step = 0; step < 16 && cursor.getY() > caster.level().getMinBuildHeight(); step++) {
            if (!caster.level().getBlockState(cursor).getCollisionShape(caster.level(), cursor).isEmpty()) {
                return new Vec3(point.x, cursor.getY() + 1.0D, point.z);
            }
            cursor = cursor.below();
        }
        return point;
    }

    private static void emitLine(
            ServerLevel level, Vec3 start, Vec3 end, ParticleOptions particle, double spacing) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 1.0E-5D) {
            return;
        }
        Vec3 direction = delta.normalize();
        for (double distance = 0.0D; distance <= length; distance += spacing) {
            Vec3 point = start.add(direction.scale(distance));
            level.sendParticles(particle, point.x, point.y, point.z,
                    1, 0.01D, 0.01D, 0.01D, 0.0D);
        }
    }

    private static Vec3 horizontal(Vec3 direction) {
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
        return horizontal.lengthSqr() < 1.0E-6D
                ? new Vec3(0.0D, 0.0D, 1.0D) : horizontal.normalize();
    }

    private static boolean isDark(LivingEntity target) {
        if (target.getType().is(EntityTypeTags.UNDEAD)) {
            return true;
        }
        if (target instanceof net.minecraft.world.entity.player.Player player) {
            return com.example.immortal_cultivation_mod.attachment.ModAttachments
                    .getData(player).spiritRoots()
                    .contains(com.example.immortal_cultivation_mod.attachment.SpiritRoots.DARK);
        }
        return target instanceof net.minecraft.world.entity.PathfinderMob mob
                && target instanceof com.example.immortal_cultivation_mod.entity.CultivationNpc npc
                && NpcProfile.spiritRootIds(mob, npc.npcArchetype())
                .contains(com.example.immortal_cultivation_mod.attachment.SpiritRoots.DARK);
    }
}
