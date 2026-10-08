package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.magicritual.center.behavior.ForcefieldCenterBehavior;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class CrimsonFlameFireballEntity extends ThrowableItemProjectile {
    private static final double EXPLOSION_RADIUS = 2.6D;
    private static final float CENTER_DAMAGE = 150.0F;
    private static final int MAX_LIFETIME = 120;

    public CrimsonFlameFireballEntity(EntityType<? extends ThrowableItemProjectile> entityType,
                                      Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    public CrimsonFlameFireballEntity(Level level, LivingEntity owner) {
        super(ModEntities.CRIMSON_FLAME_FIREBALL.get(), owner, level);
        setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.FIRE_CHARGE;
    }

    @Override
    public void tick() {
        setNoGravity(true);
        super.tick();
        if (!level().isClientSide && tickCount > MAX_LIFETIME) {
            discard();
            return;
        }
        if (level().isClientSide) {
            Vec3 movement = getDeltaMovement();
            level().addParticle(ParticleTypes.FLAME,
                    getX(), getY(), getZ(),
                    -movement.x * 0.05D, -movement.y * 0.05D, -movement.z * 0.05D);
            level().addParticle(ParticleTypes.SMOKE,
                    getX() - movement.x * 0.15D,
                    getY() - movement.y * 0.15D,
                    getZ() - movement.z * 0.15D,
                    0.0D, 0.0D, 0.0D);
            if (tickCount % 3 == 0) {
                level().addParticle(ParticleTypes.LAVA,
                        getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        Entity target = ModelBonePartEntity.resolveParent(entity);
        if (target == getOwner() || target instanceof CrimsonScaleFlameLizardEntity) {
            return false;
        }
        return super.canHitEntity(entity);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(level() instanceof ServerLevel serverLevel) || !isAlive()) {
            return;
        }

        Vec3 center = result.getLocation();
        AABB area = new AABB(center, center).inflate(EXPLOSION_RADIUS);
        Entity owner = getOwner();
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                living -> living.isAlive()
                        && living != owner
                        && !(living instanceof CrimsonScaleFlameLizardEntity))) {
            double distance = target.position()
                    .add(0.0D, target.getBbHeight() * 0.5D, 0.0D)
                    .distanceTo(center);
            if (distance > EXPLOSION_RADIUS) {
                continue;
            }
            float distanceScale = (float)Math.max(0.25D, 1.0D - distance / EXPLOSION_RADIUS);
            float damage = CENTER_DAMAGE * distanceScale;
            Entity policySource = owner == null ? this : owner;
            Vec3 attackOrigin = owner == null
                    ? center : owner.getBoundingBox().getCenter();
            if (ForcefieldCenterBehavior.interceptAttackPath(
                    policySource, attackOrigin,
                    target.getBoundingBox().getCenter(), damage)) {
                continue;
            }
            target.hurt(damageSources().thrown(this, owner), damage);
            target.igniteForSeconds(5.0F);
            Vec3 away = target.position().subtract(center);
            target.knockback(0.9D * distanceScale, -away.x, -away.z);
        }

        if (owner instanceof CrimsonScaleFlameLizardEntity lizard) {
            BeastMoveBlockBreaker.breakSphere(serverLevel, lizard, center,
                    2.4D, 1.8D, 24, 6.0F);
        }

        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        serverLevel.sendParticles(ParticleTypes.FLAME,
                center.x, center.y, center.z, 48, 0.85D, 0.85D, 0.85D, 0.12D);
        serverLevel.sendParticles(ParticleTypes.LAVA,
                center.x, center.y, center.z, 18, 0.65D, 0.65D, 0.65D, 0.08D);
        serverLevel.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 0.9F, 1.25F);
        discard();
    }
}
