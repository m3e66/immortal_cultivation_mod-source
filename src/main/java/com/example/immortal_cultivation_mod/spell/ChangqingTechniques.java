package com.example.immortal_cultivation_mod.spell;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.MethodTechniqueAreaEntity;
import com.example.immortal_cultivation_mod.entity.SpellTriggerProjectileEntity;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Real method techniques granted only while 长青诀 is active. */
public final class ChangqingTechniques {
    private static final Set<String> SPELLS = Set.of(
            ModSpells.WANMU_SHENG,
            ModSpells.QINGTENG_FU);

    private ChangqingTechniques() {
    }

    public static boolean isTechnique(String spellId) {
        return SPELLS.contains(ModSpells.normalizeId(spellId));
    }

    public static boolean canCast(ServerPlayer player, String spellId) {
        if (!isTechnique(spellId)) return false;
        if (!ChangqingJue.hasMethod(player)) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".changqing_wrong_method"), true);
            return false;
        }
        if (ModSpells.WANMU_SHENG.equals(ModSpells.normalizeId(spellId))) {
            return player.serverLevel().getEntitiesOfClass(
                    MethodTechniqueAreaEntity.class,
                    player.getBoundingBox().inflate(10.0D),
                    area -> area.kind() == MethodTechniqueAreaEntity.Kind.CHANGQING_GROWTH
                            && area.forcefieldOwner() == player).isEmpty();
        }
        return true;
    }

    public static boolean cast(
            ServerPlayer player, String spellId, float chargeScale,
            Consumer<Entity> projectileHook) {
        if (!canCast(player, spellId)) return false;
        return castActor(player, null, spellId, chargeScale, projectileHook);
    }

    public static boolean cast(
            PathfinderMob caster, LivingEntity target, String spellId) {
        return ChangqingJue.hasMethod(caster)
                && castActor(caster, target, spellId, SpellCharge.NORMAL, ignored -> { });
    }

    private static boolean castActor(
            LivingEntity caster, LivingEntity target, String spellId,
            float chargeScale, Consumer<Entity> projectileHook) {
        if (!(caster.level() instanceof ServerLevel level)) return false;
        String normalized = ModSpells.normalizeId(spellId);
        if (ModSpells.WANMU_SHENG.equals(normalized)) {
            BlockPos ground = findSurface(level, caster.blockPosition().above(3), 8);
            MethodTechniqueAreaEntity field = MethodTechniqueAreaEntity.create(
                    level, caster, MethodTechniqueAreaEntity.Kind.CHANGQING_GROWTH,
                    Vec3.atCenterOf(ground.above()), caster.getLookAngle(),
                    chargeScale, caster instanceof ServerPlayer player
                            ? ChangqingJue.lifeForce(player) : 60.0F);
            if (field == null) return false;
            projectileHook.accept(field);
            castingVfx(level, caster, true);
            level.playSound(null, caster.blockPosition(), SoundEvents.AZALEA_LEAVES_PLACE,
                    SoundSource.PLAYERS, 1.1F, 0.82F);
            return level.addFreshEntity(field);
        }
        if (ModSpells.QINGTENG_FU.equals(normalized)) {
            Vec3 direction = target == null
                    ? caster.getLookAngle().normalize()
                    : target.getBoundingBox().getCenter()
                            .subtract(caster.getEyePosition()).normalize();
            SpellTriggerProjectileEntity projectile = new SpellTriggerProjectileEntity(
                    level, caster, normalized, chargeScale);
            Vec3 start = caster.getEyePosition().add(direction.scale(0.72D));
            projectile.setPos(start.x, start.y, start.z);
            projectile.shoot(direction.x, direction.y, direction.z, 1.85F, 0.0F);
            projectileHook.accept(projectile);
            castingVfx(level, caster, false);
            return level.addFreshEntity(projectile);
        }
        return false;
    }

    public static void activateTriggeredSpell(
            ServerLevel level, LivingEntity caster, String spellId,
            Vec3 impact, LivingEntity hitTarget, float chargeScale) {
        if (!ModSpells.QINGTENG_FU.equals(ModSpells.normalizeId(spellId))) return;
        Vec3 target = hitTarget == null ? impact : hitTarget.position();
        BlockPos ground = findSurface(level, BlockPos.containing(target).above(3), 9);
        MethodTechniqueAreaEntity vines = MethodTechniqueAreaEntity.create(
                level, caster, MethodTechniqueAreaEntity.Kind.CHANGQING_VINES,
                Vec3.atCenterOf(ground.above()), caster.getLookAngle(),
                chargeScale, 1.0F);
        if (vines != null) {
            level.addFreshEntity(vines);
            level.playSound(null, ground, SoundEvents.ROOTED_DIRT_PLACE,
                    SoundSource.PLAYERS, 1.0F, 0.74F);
        }
    }

    public static void tickTriggerProjectile(SpellTriggerProjectileEntity projectile) {
        if (!ModSpells.QINGTENG_FU.equals(projectile.spellId())
                || !(projectile.level() instanceof ServerLevel level)) return;
        Vec3 motion = projectile.getDeltaMovement();
        Vec3 direction = motion.lengthSqr() < 1.0E-6D
                ? new Vec3(0.0D, 0.0D, 1.0D) : motion.normalize();
        Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-6D) side = new Vec3(1.0D, 0.0D, 0.0D);
        else side = side.normalize();
        for (int i = -1; i <= 1; i++) {
            Vec3 point = projectile.position().add(side.scale(i * 0.11D));
            level.sendParticles(ModParticles.METHOD_QI_WOOD.get(),
                    point.x, point.y, point.z, 0,
                    -direction.x * 0.018D,
                    0.004D + Math.abs(i) * 0.003D,
                    -direction.z * 0.018D, 1.0D);
        }
    }

    public static float triggerBarrierDamage(String spellId, float chargeScale) {
        return ModSpells.QINGTENG_FU.equals(ModSpells.normalizeId(spellId))
                ? SpellCharge.damage(80.0F, chargeScale) : 0.0F;
    }

    private static BlockPos findSurface(ServerLevel level, BlockPos origin, int search) {
        for (int offset = search; offset >= -search; offset--) {
            BlockPos ground = origin.offset(0, offset, 0);
            BlockState state = level.getBlockState(ground);
            if (!state.isAir() && state.getFluidState().isEmpty()
                    && state.isFaceSturdy(level, ground, Direction.UP)) {
                return ground;
            }
        }
        return origin.below();
    }

    private static void castingVfx(
            ServerLevel level, LivingEntity caster, boolean growth) {
        Vec3 center = caster.getBoundingBox().getCenter();
        level.sendParticles(ModParticles.METHOD_QI_WOOD.get(),
                center.x, center.y, center.z,
                growth ? 30 : 16, 0.55D, 0.75D, 0.55D, 0.045D);
        if (growth) {
            level.sendParticles(ModParticles.METHOD_QI_YANG.get(),
                    center.x, center.y, center.z,
                    8, 0.38D, 0.60D, 0.38D, 0.025D);
        }
    }
}
