package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.effect.PhotonEffects;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.ForcefieldCenterBehavior;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class FireballTalismanProjectileEntity extends ThrowableItemProjectile {
    private static final double CURVE_DURATION_TICKS = 20.0D;
    private static final double OUTWARD_CURVE_BLOCKS = 8.0D;

    private double startX;
    private double startY;
    private double startZ;
    private double targetX;
    private double targetY;
    private double targetZ;
    private double outwardX;
    private double outwardY;
    private double outwardZ;
    private double speed = 1.45D;
    private Optional<UUID> targetEntity = Optional.empty();
    private int photonEffectAttempts;
    private int launchDelayTicks;
    private int flightTicks;

    public FireballTalismanProjectileEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    public FireballTalismanProjectileEntity(Level level, LivingEntity shooter) {
        super(ModEntities.FIREBALL_TALISMAN_PROJECTILE.get(), shooter, level);
        setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.FIREBALL_TALISMAN.get();
    }

    @Override
    public void tick() {
        setNoGravity(true);
        if (!level().isClientSide && launchDelayTicks > 0) {
            launchDelayTicks--;
            setDeltaMovement(Vec3.ZERO);
            super.tick();
            return;
        }
        if (!level().isClientSide) {
            steer();
            if (isRemoved()) {
                return;
            }
        }
        super.tick();
        if (!level().isClientSide) {
            if (photonEffectAttempts < 5) {
                photonEffectAttempts++;
                PhotonEffects.fireballTalismanTrail(this);
            }
        }
    }

    private void steer() {
        flightTicks++;
        Vec3 target = currentTargetPosition();
        if (flightTicks > 12 && position().distanceToSqr(target) < 1.25D) {
            if (ForcefieldCenterBehavior.interceptProjectileImpact(this, target)) {
                return;
            }
            hitLockedTarget();
            return;
        }

        double phase = Math.min(1.0D, flightTicks / CURVE_DURATION_TICKS);
        Vec3 desired = curvePoint(target, phase).subtract(position());
        if (phase >= 1.0D || desired.lengthSqr() < 0.001D) {
            desired = target.subtract(position());
        }
        if (desired.lengthSqr() > 0.001D) {
            double stepSpeed = Math.min(speed, Math.max(0.08D, desired.length()));
            setDeltaMovement(desired.normalize().scale(stepSpeed));
        }

        if (flightTicks > 100) {
            discard();
        }
    }

    private Vec3 curvePoint(Vec3 target, double phase) {
        Vec3 start = new Vec3(startX, startY, startZ);
        Vec3 outward = new Vec3(outwardX, outwardY, outwardZ);
        Vec3 firstControl = start.add(outward);
        Vec3 secondControl = target.add(outward.scale(0.35D));
        double inverse = 1.0D - phase;
        return start.scale(inverse * inverse * inverse)
                .add(firstControl.scale(3.0D * inverse * inverse * phase))
                .add(secondControl.scale(3.0D * inverse * phase * phase))
                .add(target.scale(phase * phase * phase));
    }

    private Vec3 currentTargetPosition() {
        if (level() instanceof ServerLevel serverLevel && targetEntity.isPresent()) {
            Entity target = serverLevel.getEntity(targetEntity.get());
            if (target instanceof LivingEntity living && living.isAlive()) {
                Vec3 pos = living.position().add(0.0D, living.getBbHeight() * 0.55D, 0.0D);
                targetX = pos.x;
                targetY = pos.y;
                targetZ = pos.z;
            }
        }
        return new Vec3(targetX, targetY, targetZ);
    }

    private void hitLockedTarget() {
        if (level() instanceof ServerLevel serverLevel && targetEntity.isPresent()) {
            Entity target = serverLevel.getEntity(targetEntity.get());
            if (target instanceof LivingEntity living && living.isAlive() && living != getOwner()) {
                hitEntity(living);
                return;
            }
        }
        explodeAndDiscard(new Vec3(targetX, targetY, targetZ));
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!level().isClientSide) {
            LivingEntity living = ModelBonePartEntity.resolveLiving(result.getEntity());
            if (living != null && living != getOwner()) {
                hitEntity(living);
            } else {
                discard();
            }
        }
    }

    private void hitEntity(LivingEntity target) {
        target.hurt(damageSources().thrown(this, getOwner()), 18.0F);
        target.igniteForSeconds(4);
        level().explode(this, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 2.4F, false, Level.ExplosionInteraction.BLOCK);
        discard();
    }

    private void explodeAndDiscard(Vec3 position) {
        level().explode(this, position.x, position.y, position.z, 2.4F, false, Level.ExplosionInteraction.BLOCK);
        discard();
    }

    @Override
    protected void onHit(HitResult result) {
        if (result.getType() == HitResult.Type.BLOCK) {
            if (!level().isClientSide) {
                explodeAndDiscard(result.getLocation());
            }
            return;
        }
        super.onHit(result);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!level().isClientSide) {
            explodeAndDiscard(result.getLocation());
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            PhotonEffects.removeFireballTalismanTrail(this);
        }
        super.remove(reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble("StartX", startX);
        tag.putDouble("StartY", startY);
        tag.putDouble("StartZ", startZ);
        tag.putDouble("TargetX", targetX);
        tag.putDouble("TargetY", targetY);
        tag.putDouble("TargetZ", targetZ);
        tag.putDouble("OutwardX", outwardX);
        tag.putDouble("OutwardY", outwardY);
        tag.putDouble("OutwardZ", outwardZ);
        tag.putDouble("Speed", speed);
        tag.putInt("LaunchDelayTicks", launchDelayTicks);
        tag.putInt("FlightTicks", flightTicks);
        targetEntity.ifPresent(uuid -> tag.putUUID("TargetEntity", uuid));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        startX = tag.getDouble("StartX");
        startY = tag.getDouble("StartY");
        startZ = tag.getDouble("StartZ");
        targetX = tag.getDouble("TargetX");
        targetY = tag.getDouble("TargetY");
        targetZ = tag.getDouble("TargetZ");
        outwardX = tag.getDouble("OutwardX");
        outwardY = tag.getDouble("OutwardY");
        outwardZ = tag.getDouble("OutwardZ");
        speed = tag.contains("Speed") ? tag.getDouble("Speed") : 1.45D;
        launchDelayTicks = tag.getInt("LaunchDelayTicks");
        flightTicks = tag.getInt("FlightTicks");
        targetEntity = tag.hasUUID("TargetEntity") ? Optional.of(tag.getUUID("TargetEntity")) : Optional.empty();
        setNoGravity(true);
    }

    public void configure(Vec3 target, Vec3 outward, double speed, int launchDelayTicks, LivingEntity targetEntity) {
        startX = getX();
        startY = getY();
        startZ = getZ();
        targetX = target.x;
        targetY = target.y;
        targetZ = target.z;
        Vec3 fixedOutward = outward.lengthSqr() < 0.001D
                ? new Vec3(0.0D, OUTWARD_CURVE_BLOCKS, 0.0D)
                : outward.normalize().scale(OUTWARD_CURVE_BLOCKS);
        outwardX = fixedOutward.x;
        outwardY = fixedOutward.y;
        outwardZ = fixedOutward.z;
        this.speed = Math.max(0.5D, speed);
        this.launchDelayTicks = Math.max(0, launchDelayTicks);
        this.targetEntity = targetEntity == null ? Optional.empty() : Optional.of(targetEntity.getUUID());
        setNoGravity(true);
    }
}
