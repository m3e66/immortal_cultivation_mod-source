package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;

import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.ForcefieldCenterBehavior;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** A real burning-scale projectile launched by 炎鳞爆. */
public final class CrimsonDragonScaleProjectileEntity extends ThrowableItemProjectile {
    // Visuals use synchronized BeastAttackVfx; server combat and sounds remain unchanged.
    private static final float DAMAGE = 280.0F;

    public CrimsonDragonScaleProjectileEntity(
            EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public CrimsonDragonScaleProjectileEntity(Level level, LivingEntity owner) {
        super(ModEntities.CRIMSON_DRAGON_SCALE_PROJECTILE.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.CHI_LIN.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > 80) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        Entity target = ModelBonePartEntity.resolveParent(entity);
        return target != getOwner()
                && !(target instanceof CrimsonSkyFlameFloodDragonEntity)
                && !(target instanceof CrimsonScaleFlameLizardEntity)
                && !(target instanceof EscapedYuanyingCoreEntity)
                && super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        LivingEntity target = ModelBonePartEntity.resolveLiving(result.getEntity());
        if (!level().isClientSide && target != null) {
            Entity owner = getOwner();
            Entity policySource = owner == null ? this : owner;
            var attackOrigin = owner == null
                    ? position() : owner.getBoundingBox().getCenter();
            if (ForcefieldCenterBehavior.interceptAttackPath(
                    policySource, attackOrigin,
                    target.getBoundingBox().getCenter(), DAMAGE)) {
                finishImpact(result);
                return;
            }
            target.hurt(damageSources().thrown(this, getOwner()), DAMAGE);
            target.igniteForSeconds(6.0F);
        }
        finishImpact(result);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!level().isClientSide
                && level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            Direction face = result.getDirection();
            var firePos = result.getBlockPos().relative(face);
            Entity owner = getOwner();
            boolean blocked = owner instanceof LivingEntity living
                    && CenterEffectSupport.isForcefieldRouteBlocked(
                            living, living.getBoundingBox().getCenter(),
                            net.minecraft.world.phys.Vec3.atCenterOf(firePos));
            if (!blocked && level().getBlockState(firePos).isAir()
                    && BlockBreakingSettings.enabled(owner == null ? this : owner,
                    BlockBreakingSettings.Category.COMPANIONS)) {
                level().setBlockAndUpdate(firePos, Blocks.FIRE.defaultBlockState());
            }
        }
        finishImpact(result);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
    }

    private void finishImpact(HitResult result) {
        if (level() instanceof ServerLevel serverLevel) {
            if (getOwner() instanceof CrimsonSkyFlameFloodDragonEntity dragon)
                BeastMoveBlockBreaker.breakSphere(serverLevel, dragon, result.getLocation(), 1.3, 1.1, 16, 12);
            serverLevel.playSound(null, blockPosition(), SoundEvents.FIRECHARGE_USE,
                    SoundSource.HOSTILE, 0.65F, 1.45F);
            discard();
        }
    }
}
