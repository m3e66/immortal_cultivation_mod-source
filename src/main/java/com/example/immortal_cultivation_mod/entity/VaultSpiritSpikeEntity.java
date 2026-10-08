package com.example.immortal_cultivation_mod.entity;

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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Dark-metal rune spike launched from a vault guardian's control arm. */
public final class VaultSpiritSpikeEntity extends ThrowableItemProjectile {
    private static final int MAX_LIFETIME = 100;
    private static final int STUCK_LIFETIME = 35;

    private float damage = 45.0F;
    private UUID trackingTarget;
    private boolean homing;
    private boolean stuck;
    private int stuckTicks;

    public VaultSpiritSpikeEntity(
            EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    public VaultSpiritSpikeEntity(
            Level level, VaultGuardianEntity owner, LivingEntity target,
            float damage, boolean homing) {
        super(ModEntities.VAULT_SPIRIT_SPIKE.get(), owner, level);
        this.damage = damage;
        this.homing = homing;
        this.trackingTarget = target == null ? null : target.getUUID();
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.XUANDUAN_IRON_SHARD.get();
    }

    @Override
    public void tick() {
        if (stuck) {
            setNoGravity(true);
            noPhysics = true;
            setDeltaMovement(Vec3.ZERO);
            if (!level().isClientSide && ++stuckTicks >= STUCK_LIFETIME) {
                discard();
            }
            return;
        }

        if (!level().isClientSide && homing && tickCount > 4 && tickCount < 48) {
            LivingEntity target = resolveTarget();
            if (target != null && target.isAlive()) {
                Vec3 aim = target.getBoundingBox().getCenter().subtract(position());
                if (aim.lengthSqr() > 0.01D) {
                    double speed = Math.max(0.72D, getDeltaMovement().length());
                    Vec3 steered = getDeltaMovement().scale(0.82D)
                            .add(aim.normalize().scale(speed * 0.18D));
                    setDeltaMovement(steered.normalize().scale(speed));
                    hasImpulse = true;
                }
            }
        }

        super.tick();
        if (level().isClientSide) {
            Vec3 motion = getDeltaMovement();
            level().addParticle(ModParticles.METHOD_QI_METAL.get(),
                    getX(), getY(), getZ(),
                    -motion.x * 0.08D, -motion.y * 0.08D, -motion.z * 0.08D);
            if ((tickCount & 1) == 0) {
                level().addParticle(ParticleTypes.FLAME,
                        getX(), getY(), getZ(),
                        -motion.x * 0.025D, -motion.y * 0.025D, -motion.z * 0.025D);
            }
        } else if (tickCount >= MAX_LIFETIME) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        Entity resolved = ModelBonePartEntity.resolveParent(entity);
        return resolved != getOwner()
                && !(resolved instanceof VaultGuardianEntity)
                && super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity target = ModelBonePartEntity.resolveLiving(result.getEntity());
        if (target != null && canDamage(target)) {
            target.hurt(damageSources().thrown(this, getOwner()), damage);
            target.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, 12, 5, false, false, true),
                    getOwner());
        }
        impact(level, result.getLocation());
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        Vec3 normal = Vec3.atLowerCornerOf(result.getDirection().getNormal()).scale(0.04D);
        Vec3 point = result.getLocation().add(normal);
        setPos(point.x, point.y, point.z);
        setDeltaMovement(Vec3.ZERO);
        setNoGravity(true);
        noPhysics = true;
        stuck = true;
        stuckTicks = 0;
        if (level() instanceof ServerLevel level) {
            impact(level, result.getLocation());
        }
    }

    private boolean canDamage(LivingEntity target) {
        Entity owner = getOwner();
        Entity source = owner == null ? this : owner;
        Vec3 origin = owner == null ? position() : owner.getBoundingBox().getCenter();
        return !ForcefieldCenterBehavior.interceptAttackPath(
                source, origin, target.getBoundingBox().getCenter(), damage);
    }

    private LivingEntity resolveTarget() {
        if (trackingTarget == null || !(level() instanceof ServerLevel level)) {
            return null;
        }
        Entity entity = level.getEntity(trackingTarget);
        return entity instanceof LivingEntity living ? living : null;
    }

    private void impact(ServerLevel level, Vec3 point) {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                point.x, point.y, point.z,
                10, 0.22D, 0.22D, 0.22D, 0.12D);
        level.sendParticles(ModParticles.METHOD_QI_METAL.get(),
                point.x, point.y, point.z,
                8, 0.18D, 0.18D, 0.18D, 0.06D);
        level.playSound(null, blockPosition(), SoundEvents.TRIDENT_HIT_GROUND,
                SoundSource.HOSTILE, 0.7F, 1.45F + random.nextFloat() * 0.18F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putBoolean("Homing", homing);
        tag.putBoolean("Stuck", stuck);
        tag.putInt("StuckTicks", stuckTicks);
        if (trackingTarget != null) {
            tag.putUUID("TrackingTarget", trackingTarget);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = Math.max(0.0F, tag.getFloat("Damage"));
        homing = tag.getBoolean("Homing");
        stuck = tag.getBoolean("Stuck");
        stuckTicks = Math.max(0, tag.getInt("StuckTicks"));
        trackingTarget = tag.hasUUID("TrackingTarget")
                ? tag.getUUID("TrackingTarget") : null;
    }
}
