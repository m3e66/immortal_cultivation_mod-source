package com.example.immortal_cultivation_mod.entity;

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
        if (level().isClientSide) {
            level().addParticle(ModParticles.DRAGON_EMBER.get(),
                    getX(), getY(), getZ(),
                    -getDeltaMovement().x * 0.12D,
                    -getDeltaMovement().y * 0.12D,
                    -getDeltaMovement().z * 0.12D);
            if ((tickCount & 1) == 0) {
                level().addParticle(ModParticles.DRAGON_SCALE_SPARK.get(),
                        getX(), getY(), getZ(),
                        -getDeltaMovement().x * 0.05D,
                        -getDeltaMovement().y * 0.05D,
                        -getDeltaMovement().z * 0.05D);
            }
        } else if (tickCount > 80) {
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
            if (!blocked && level().getBlockState(firePos).isAir()) {
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
            serverLevel.sendParticles(ModParticles.DRAGON_EMBER.get(),
                    result.getLocation().x, result.getLocation().y, result.getLocation().z,
                    18, 0.34D, 0.34D, 0.34D, 0.09D);
            serverLevel.sendParticles(ModParticles.DRAGON_SCALE_SPARK.get(),
                    result.getLocation().x, result.getLocation().y, result.getLocation().z,
                    12, 0.26D, 0.26D, 0.26D, 0.16D);
            serverLevel.playSound(null, blockPosition(), SoundEvents.FIRECHARGE_USE,
                    SoundSource.HOSTILE, 0.65F, 1.45F);
            discard();
        }
    }
}
