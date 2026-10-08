package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.entity.MethodTechniqueAreaEntity;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import com.example.immortal_cultivation_mod.entity.SpellImpactParticles;
import com.example.immortal_cultivation_mod.entity.SpellTriggerProjectileEntity;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.ForcefieldCenterBehavior;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Real executions granted by 寒澜凝渊经. */
public final class HanlanTechniques {
    private static final Set<String> SPELLS = Set.of(
            ModSpells.DONGLIU_SUO,
            ModSpells.CANGYUAN_HONGLIU,
            ModSpells.HANYUAN_FENGJIE);

    private HanlanTechniques() {
    }

    public static boolean isTechnique(String spellId) {
        return SPELLS.contains(ModSpells.normalizeId(spellId));
    }

    public static boolean canCast(ServerPlayer player, String spellId) {
        if (!isTechnique(spellId)) return false;
        if (!CultivationMethods.isHanlanNingyuanJing(
                ModAttachments.getData(player).activeCultivationMethod())) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".hanlan_wrong_method"), true);
            return false;
        }
        return true;
    }

    public static boolean cast(
            ServerPlayer player, String spellId, float chargeScale,
            Consumer<Entity> projectileHook) {
        if (!canCast(player, spellId)) return false;
        return castActor(player, null, spellId, chargeScale, projectileHook);
    }

    public static boolean cast(PathfinderMob caster, LivingEntity target, String spellId) {
        return HanlanNingyuanJing.hasMethod(caster)
                && castActor(caster, target, spellId, SpellCharge.NORMAL, entity -> { });
    }

    private static boolean castActor(
            LivingEntity caster, LivingEntity target, String spellId, float chargeScale,
            Consumer<Entity> projectileHook) {
        String normalized = ModSpells.normalizeId(spellId);
        if (!SPELLS.contains(normalized) || !(caster.level() instanceof ServerLevel level)) {
            return false;
        }
        Vec3 look = target == null
                ? caster.getLookAngle().normalize()
                : target.getBoundingBox().getCenter().subtract(caster.getEyePosition()).normalize();
        float environment = HanlanNingyuanJing.environmentalStrength(
                level, caster.blockPosition());
        if (ModSpells.DONGLIU_SUO.equals(normalized)) {
            SpellTriggerProjectileEntity projectile = new SpellTriggerProjectileEntity(
                    level, caster, normalized, chargeScale);
            Vec3 start = caster.getEyePosition().add(look.scale(0.72D));
            projectile.setPos(start.x, start.y, start.z);
            projectile.shoot(look.x, look.y, look.z, 2.15F, 0.0F);
            projectileHook.accept(projectile);
            castingVfx(level, caster, false);
            return level.addFreshEntity(projectile);
        }
        if (ModSpells.CANGYUAN_HONGLIU.equals(normalized)) {
            Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
            if (horizontal.lengthSqr() < 1.0E-6D) horizontal = new Vec3(0.0D, 0.0D, 1.0D);
            horizontal = horizontal.normalize();
            BlockPos ground = findSurface(level, caster.blockPosition().above(3), 8);
            Vec3 start = Vec3.atCenterOf(ground.above()).add(horizontal.scale(1.25D));
            MethodTechniqueAreaEntity flood = MethodTechniqueAreaEntity.create(
                    level, caster, MethodTechniqueAreaEntity.Kind.CANGYUAN_FLOOD,
                    start, horizontal, chargeScale, environment);
            if (flood == null) return false;
            projectileHook.accept(flood);
            castingVfx(level, caster, false);
            level.playSound(null, caster.blockPosition(), SoundEvents.BUCKET_EMPTY,
                    SoundSource.PLAYERS, 1.15F, 0.72F);
            return level.addFreshEntity(flood);
        }
        if (ModSpells.HANYUAN_FENGJIE.equals(normalized)) {
            BlockPos ground = findSurface(level, caster.blockPosition().above(4), 10);
            MethodTechniqueAreaEntity domain = MethodTechniqueAreaEntity.create(
                    level, caster, MethodTechniqueAreaEntity.Kind.HANYUAN_DOMAIN,
                    Vec3.atCenterOf(ground.above()), look, chargeScale, environment);
            if (domain == null) return false;
            projectileHook.accept(domain);
            castingVfx(level, caster, true);
            level.playSound(null, caster.blockPosition(), SoundEvents.GLASS_BREAK,
                    SoundSource.PLAYERS, 1.2F, 0.62F);
            return level.addFreshEntity(domain);
        }
        return false;
    }

    public static void activateTriggeredSpell(
            ServerLevel level, LivingEntity caster, String spellId,
            Vec3 impact, LivingEntity hitTarget, float chargeScale) {
        if (!ModSpells.DONGLIU_SUO.equals(ModSpells.normalizeId(spellId))) return;
        Vec3 center = hitTarget == null ? impact : hitTarget.position();
        BlockPos impactPos = BlockPos.containing(center);
        boolean spread = hitTarget != null && HanlanNingyuanJing.isStandingInWater(hitTarget)
                || level.getFluidState(impactPos).is(net.minecraft.tags.FluidTags.WATER)
                || level.getFluidState(impactPos.below()).is(net.minecraft.tags.FluidTags.WATER);
        if (spread) HanlanNingyuanJing.freezeWaterAround(level, caster,
                hitTarget == null ? impactPos : hitTarget.blockPosition(), 4);
        AABB bounds = new AABB(center, center).inflate(spread ? 4.5D : 2.0D, 3.0D,
                spread ? 4.5D : 2.0D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, bounds,
                target -> target != caster && NpcProfile.isCombatEnemy(caster, target)
                        && (target == hitTarget || spread))) {
            if (ForcefieldCenterBehavior.interceptAttackPath(caster, caster.getEyePosition(),
                    target.getBoundingBox().getCenter(), 320.0F)) continue;
            boolean water = HanlanNingyuanJing.isStandingInWater(target);
            boolean damp = HanlanNingyuanJing.isDamp(target) || water;
            target.hurt(level.damageSources().indirectMagic(caster, caster),
                    SpellDamageHelper.damage(caster,
                            SpellCharge.damage(damp ? 320.0F : 220.0F, chargeScale),
                            ModSpells.get(ModSpells.DONGLIU_SUO)));
            target.addEffect(new MobEffectInstance(ModEffects.TECHNIQUE_SLOW,
                    damp ? 80 : 60, damp ? 69 : 44,
                    false, true, true), caster);
            target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 20,
                    target.getTicksFrozen() + (damp ? 80 : 35)));
            if (damp) {
                target.addEffect(new MobEffectInstance(ModEffects.FROZEN,
                        water ? 30 : 20, 0, false, true, true), caster);
            }
            level.sendParticles(ModParticles.METHOD_QI_ICE.get(),
                    target.getX(), target.getY() + target.getBbHeight() * 0.35D, target.getZ(),
                    30, 0.32D, target.getBbHeight() * 0.42D, 0.32D, 0.06D);
            emitFreezeClimb(level, target, damp, water);
        }
        SpellImpactParticles.shockRing(level, center.add(0.0D, 0.1D, 0.0D),
                ModParticles.METHOD_QI_ICE.get(), 0.7D, spread ? 52 : 32, 0.22D);
        level.sendParticles(ParticleTypes.SNOWFLAKE,
                center.x, center.y + 0.4D, center.z,
                spread ? 65 : 38, spread ? 2.8D : 1.1D, 0.8D,
                spread ? 2.8D : 1.1D, 0.08D);
        level.playSound(null, BlockPos.containing(center), SoundEvents.GLASS_BREAK,
                SoundSource.PLAYERS, 1.0F, 1.42F);
    }

    private static void emitFreezeClimb(
            ServerLevel level, LivingEntity target, boolean damp, boolean water) {
        Vec3 feet = target.position().add(0.0D, 0.08D, 0.0D);
        SpellImpactParticles.shockRing(level, feet,
                water ? ModParticles.METHOD_QI_WATER.get()
                        : ModParticles.METHOD_QI_ICE.get(),
                water ? 1.35D : damp ? 0.82D : 0.52D,
                water ? 44 : damp ? 32 : 22, 0.08D);
        int branches = water ? 9 : damp ? 7 : 4;
        double climb = target.getBbHeight() * (water ? 0.90D : damp ? 0.72D : 0.46D);
        for (int branch = 0; branch < branches; branch++) {
            double angle = Math.PI * 2.0D * branch / branches + branch * 0.23D;
            double radius = target.getBbWidth() * (0.38D + (branch & 1) * 0.10D);
            int points = water ? 9 : 6;
            for (int pointIndex = 0; pointIndex < points; pointIndex++) {
                double t = pointIndex / (double) Math.max(1, points - 1);
                double twist = angle + t * (damp ? 0.55D : 0.25D);
                Vec3 point = feet.add(
                        Math.cos(twist) * radius * (1.0D - t * 0.34D),
                        climb * t,
                        Math.sin(twist) * radius * (1.0D - t * 0.34D));
                level.sendParticles(pointIndex % 3 == 0
                                ? ParticleTypes.SNOWFLAKE
                                : ModParticles.METHOD_QI_ICE.get(),
                        point.x, point.y, point.z, 1,
                        0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        if (damp) {
            level.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                    target.getX(), target.getY() + climb * 0.55D, target.getZ(),
                    water ? 34 : 20,
                    target.getBbWidth() * 0.42D, climb * 0.38D,
                    target.getBbWidth() * 0.42D, 0.08D);
        }
    }

    public static void tickTriggerProjectile(SpellTriggerProjectileEntity projectile) {
        if (!ModSpells.DONGLIU_SUO.equals(projectile.spellId())
                || !(projectile.level() instanceof ServerLevel level)) return;
        Vec3 motion = projectile.getDeltaMovement();
        Vec3 direction = motion.lengthSqr() < 1.0E-6D
                ? new Vec3(0.0D, 0.0D, 1.0D) : motion.normalize();
        Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-6D) side = new Vec3(1.0D, 0.0D, 0.0D);
        else side = side.normalize();
        for (int i = -2; i <= 2; i++) {
            Vec3 point = projectile.position().add(side.scale(i * 0.12D));
            level.sendParticles(i == 0 ? ModParticles.METHOD_QI_ICE.get()
                            : ParticleTypes.SNOWFLAKE,
                    point.x, point.y, point.z, 1,
                    -direction.x * 0.02D, -0.005D, -direction.z * 0.02D, 0.0D);
        }
    }

    public static float triggerBarrierDamage(String spellId, float chargeScale) {
        float base = switch (ModSpells.normalizeId(spellId)) {
            case ModSpells.DONGLIU_SUO -> 320.0F;
            case ModSpells.CANGYUAN_HONGLIU -> 420.0F;
            case ModSpells.HANYUAN_FENGJIE -> 180.0F;
            default -> 0.0F;
        };
        return SpellCharge.damage(base, chargeScale);
    }

    public static float qiCostMultiplier(LivingEntity caster) {
        if (!(caster.level() instanceof ServerLevel level)) return 1.0F;
        float strength = HanlanNingyuanJing.environmentalStrength(level, caster.blockPosition());
        if (strength >= 0.80F) return 0.70F;
        if (strength >= 0.40F) return 0.80F;
        if (strength >= 0.15F) return 0.90F;
        return 1.0F;
    }

    private static BlockPos findSurface(ServerLevel level, BlockPos origin, int search) {
        for (int offset = search; offset >= -search; offset--) {
            BlockPos ground = origin.offset(0, offset, 0);
            BlockState state = level.getBlockState(ground);
            if (!state.isAir() && state.getFluidState().isEmpty()
                    && state.isFaceSturdy(level, ground, Direction.UP)) return ground;
        }
        return origin.below();
    }

    private static void castingVfx(
            ServerLevel level, LivingEntity caster, boolean domain) {
        Vec3 center = caster.getBoundingBox().getCenter();
        level.sendParticles(ModParticles.METHOD_QI_WATER.get(),
                center.x, center.y, center.z,
                domain ? 30 : 16, 0.52D, 0.75D, 0.52D, 0.05D);
        level.sendParticles(ModParticles.METHOD_QI_ICE.get(),
                center.x, center.y, center.z,
                domain ? 38 : 18, 0.58D, 0.85D, 0.58D, 0.055D);
        if (domain) {
            SpellImpactParticles.shockRing(level, caster.position().add(0.0D, 0.1D, 0.0D),
                    ModParticles.METHOD_QI_ICE.get(), 0.8D, 50, 0.28D);
        }
    }
}
