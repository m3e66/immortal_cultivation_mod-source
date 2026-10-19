package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.ForcefieldCenterBehavior;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Electrically accelerated stone from 雷石飞射. */
public final class ThunderRockProjectileEntity extends ThrowableItemProjectile {
    // Visuals use synchronized BeastAttackVfx; server combat and sounds remain unchanged.
    private static final float DAMAGE = 32.0F;
    private UUID trackingTarget;
    private boolean predictive;

    public ThunderRockProjectileEntity(
            EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    public ThunderRockProjectileEntity(
            Level level, ThunderhornRockBadgerEntity owner,
            LivingEntity target, boolean predictive) {
        super(ModEntities.THUNDER_ROCK_PROJECTILE.get(), owner, level);
        this.trackingTarget = target == null ? null : target.getUUID();
        this.predictive = predictive;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.THUNDER_PATTERNED_ROCK_PLATE.get();
    }

    @Override
    public void tick() {
        if (!level().isClientSide && predictive && tickCount > 3 && tickCount < 22) {
            LivingEntity target = resolveTarget();
            if (target != null && target.isAlive()) {
                Vec3 predicted = target.getBoundingBox().getCenter()
                        .add(target.getDeltaMovement().scale(5.0D));
                Vec3 aim = predicted.subtract(position());
                if (aim.lengthSqr() > 0.01D) {
                    double speed = Math.max(0.9D, getDeltaMovement().length());
                    Vec3 steered = getDeltaMovement().scale(0.91D)
                            .add(aim.normalize().scale(speed * 0.09D));
                    setDeltaMovement(steered.normalize().scale(speed));
                    hasImpulse = true;
                }
            }
        }
        super.tick();
        if (level().isClientSide) {
            Vec3 motion = getDeltaMovement();

        } else if (tickCount > 80) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        Entity resolved = ModelBonePartEntity.resolveParent(entity);
        return resolved != getOwner()
                && !(resolved instanceof ThunderhornRockBadgerEntity)
                && super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!(level() instanceof ServerLevel level)) return;
        LivingEntity target = ModelBonePartEntity.resolveLiving(result.getEntity());
        if (target != null && canDamage(target)) {
            target.hurt(damageSources().thrown(this, getOwner()), DAMAGE);
            target.addEffect(new MobEffectInstance(
                    ModEffects.DAZE, 4, 0, false, true, true), getOwner());
        }
        impact(level, result.getLocation());
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (level() instanceof ServerLevel level) {
            impact(level, result.getLocation());
        }
        discard();
    }

    private boolean canDamage(LivingEntity target) {
        Entity owner = getOwner();
        Entity source = owner == null ? this : owner;
        Vec3 origin = owner == null ? position() : owner.getBoundingBox().getCenter();
        return !ForcefieldCenterBehavior.interceptAttackPath(
                source, origin, target.getBoundingBox().getCenter(), DAMAGE);
    }

    private LivingEntity resolveTarget() {
        if (trackingTarget == null || !(level() instanceof ServerLevel level)) return null;
        Entity entity = level.getEntity(trackingTarget);
        return entity instanceof LivingEntity living ? living : null;
    }

    private void impact(ServerLevel level, Vec3 point) {
        level.playSound(null, blockPosition(), SoundEvents.STONE_BREAK,
                SoundSource.HOSTILE, 0.75F, 1.08F + random.nextFloat() * 0.2F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Predictive", predictive);
        if (trackingTarget != null) tag.putUUID("TrackingTarget", trackingTarget);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        predictive = tag.getBoolean("Predictive");
        trackingTarget = tag.hasUUID("TrackingTarget")
                ? tag.getUUID("TrackingTarget") : null;
    }
}
