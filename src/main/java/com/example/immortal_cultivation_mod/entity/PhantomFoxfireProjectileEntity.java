package com.example.immortal_cultivation_mod.entity;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** One of the five curved, lightly homing flames created by 迷心狐火. */
public final class PhantomFoxfireProjectileEntity extends ThrowableItemProjectile {
    // Visuals use synchronized BeastAttackVfx; server combat and sounds remain unchanged.
    private static final EntityDataAccessor<Optional<UUID>> TARGET_UUID =
            SynchedEntityData.defineId(PhantomFoxfireProjectileEntity.class,
                    EntityDataSerializers.OPTIONAL_UUID);
    private UUID castId;
    private int curveDirection;

    public PhantomFoxfireProjectileEntity(
            EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public PhantomFoxfireProjectileEntity(Level level, PhantomVeilFoxEntity owner) {
        super(ModEntities.PHANTOM_FOXFIRE.get(), owner, level);
        setNoGravity(true);
    }

    public void configure(LivingEntity target, UUID castId, int curveDirection) {
        entityData.set(TARGET_UUID,
                target == null ? Optional.empty() : Optional.of(target.getUUID()));
        this.castId = castId;
        this.curveDirection = Integer.compare(curveDirection, 0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TARGET_UUID, Optional.empty());
    }

    @Override
    protected Item getDefaultItem() {
        return Items.FIRE_CHARGE;
    }

    @Override
    public void tick() {
        setNoGravity(true);
        if (!level().isClientSide) {
            homeTowardTarget();
        }
        super.tick();
        if (!level().isClientSide && tickCount > 90) {
            impactParticles();
            discard();
        }
    }

    private void homeTowardTarget() {
        LivingEntity target = resolveTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        Vec3 current = getDeltaMovement();
        double speed = Math.max(0.85D, current.length());
        Vec3 desired = target.getEyePosition().subtract(position()).normalize();
        Vec3 side = new Vec3(-desired.z, 0.0D, desired.x)
                .scale(curveDirection * Math.max(0.0D, 1.0D - tickCount / 35.0D) * 0.22D);
        Vec3 steered = current.lengthSqr() < 1.0E-5D
                ? desired
                : current.normalize().scale(0.82D).add(desired.scale(0.18D)).add(side);
        setDeltaMovement(steered.normalize().scale(speed));
    }

    private LivingEntity resolveTarget() {
        Optional<UUID> targetId = entityData.get(TARGET_UUID);
        return targetId.isPresent() && level() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(targetId.get()) instanceof LivingEntity living
                ? living : null;
    }


    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity hit = result.getEntity();
        Entity owner = getOwner();
        if (!(hit instanceof LivingEntity target) || hit == owner
                || hit instanceof PhantomVeilFoxCloneEntity) {
            return;
        }
        if (owner instanceof PhantomVeilFoxEntity fox
                && target.hurt(damageSources().indirectMagic(this, fox), fox.techniqueDamage(180.0F))) {
            fox.recordFoxfireHit(target, castId);
        }
        impactParticles();
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        impactParticles();
        discard();
    }

    private void impactParticles() {
        if (level() instanceof ServerLevel serverLevel) {
            if (getOwner() instanceof PhantomVeilFoxEntity fox && BeastMoveBlockBreaker.destructiveRealm(fox) >= 3)
                BeastMoveBlockBreaker.breakSphere(serverLevel, fox, position(), 1.5, 1.2, 24, 8);
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return target != getOwner() && !(target instanceof PhantomVeilFoxCloneEntity)
                && super.canHitEntity(target);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        entityData.get(TARGET_UUID).ifPresent(uuid -> tag.putUUID("Target", uuid));
        if (castId != null) {
            tag.putUUID("Cast", castId);
        }
        tag.putInt("Curve", curveDirection);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Target")) {
            entityData.set(TARGET_UUID, Optional.of(tag.getUUID("Target")));
        }
        castId = tag.hasUUID("Cast") ? tag.getUUID("Cast") : null;
        curveDirection = tag.getInt("Curve");
    }
}
