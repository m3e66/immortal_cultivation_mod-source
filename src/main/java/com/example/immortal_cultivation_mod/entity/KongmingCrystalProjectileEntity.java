package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.ForcefieldCenterBehavior;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** A real falling spatial-crystal projectile used by 空冥甲虫. */
public final class KongmingCrystalProjectileEntity extends ThrowableItemProjectile {
    private static final float DIRECT_DAMAGE = 85.0F;
    private static final float SPLASH_DAMAGE = 55.0F;
    private static final double SPLASH_RADIUS = 1.65D;
    private static final int STUCK_LIFETIME = 45;

    private boolean stuck;
    private int stuckTicks;

    public KongmingCrystalProjectileEntity(
            EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    public KongmingCrystalProjectileEntity(Level level, LivingEntity owner) {
        super(ModEntities.KONGMING_CRYSTAL_PROJECTILE.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.KONGMING_STONE.get();
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

        super.tick();
        if (level().isClientSide) {
            level().addParticle(ModParticles.METHOD_QI_DARK.get(),
                    getX(), getY(), getZ(), 0.0D, -0.015D, 0.0D);
            if ((tickCount & 1) == 0) {
                level().addParticle(ParticleTypes.PORTAL,
                        getX(), getY(), getZ(),
                        (random.nextDouble() - 0.5D) * 0.08D,
                        0.01D,
                        (random.nextDouble() - 0.5D) * 0.08D);
            }
        } else if (tickCount > 100) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        Entity resolved = ModelBonePartEntity.resolveParent(entity);
        return resolved != getOwner()
                && !(resolved instanceof KongmingBeetleEntity)
                && (resolved instanceof Player || resolved instanceof CultivationNpc)
                && super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity directTarget = ModelBonePartEntity.resolveLiving(result.getEntity());
        Vec3 impact = result.getLocation();
        if (directTarget != null && canDamage(level, directTarget, DIRECT_DAMAGE)) {
            directTarget.hurt(damageSources().thrown(this, getOwner()), DIRECT_DAMAGE);
            directTarget.addEffect(new MobEffectInstance(
                    ModEffects.DAZE, 10, 0, false, true, true));
        }
        damageSplash(level, impact, directTarget);
        finishImpact(level, impact);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!(level() instanceof ServerLevel level)) {
            stickAt(result);
            return;
        }
        Vec3 impact = result.getLocation();
        if (routeBlocked(impact)) {
            blockedImpact(level, impact);
            discard();
            return;
        }
        damageSplash(level, impact, null);
        impactParticles(level, impact);
        level.playSound(null, result.getBlockPos(), SoundEvents.AMETHYST_BLOCK_HIT,
                SoundSource.HOSTILE, 0.8F, 0.65F + random.nextFloat() * 0.2F);
        stickAt(result);
    }

    private void damageSplash(ServerLevel level, Vec3 impact, LivingEntity directTarget) {
        AABB area = new AABB(impact, impact).inflate(SPLASH_RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class, area,
                candidate -> candidate != directTarget
                        && candidate != getOwner()
                        && (candidate instanceof Player || candidate instanceof CultivationNpc))) {
            if (target.getBoundingBox().distanceToSqr(impact) > SPLASH_RADIUS * SPLASH_RADIUS
                    || !canDamage(level, target, SPLASH_DAMAGE)) {
                continue;
            }
            target.hurt(damageSources().thrown(this, getOwner()), SPLASH_DAMAGE);
            Vec3 away = target.position().subtract(impact);
            target.knockback(0.55D, -away.x, -away.z);
        }
    }

    private boolean canDamage(ServerLevel level, LivingEntity target, float damage) {
        Entity owner = getOwner();
        Entity source = owner == null ? this : owner;
        Vec3 origin = owner == null
                ? position().subtract(getDeltaMovement())
                : owner.getBoundingBox().getCenter();
        return !ForcefieldCenterBehavior.interceptAttackPath(
                source, origin, target.getBoundingBox().getCenter(), damage);
    }

    private boolean routeBlocked(Vec3 destination) {
        return getOwner() instanceof LivingEntity owner
                && CenterEffectSupport.isForcefieldRouteBlocked(
                        owner, owner.getBoundingBox().getCenter(), destination);
    }

    private void stickAt(BlockHitResult result) {
        Vec3 normal = Vec3.atLowerCornerOf(result.getDirection().getNormal()).scale(0.06D);
        Vec3 position = result.getLocation().add(normal);
        setPos(position.x, position.y, position.z);
        setDeltaMovement(Vec3.ZERO);
        setNoGravity(true);
        noPhysics = true;
        stuck = true;
        stuckTicks = 0;
    }

    private void finishImpact(ServerLevel level, Vec3 impact) {
        impactParticles(level, impact);
        level.playSound(null, blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK,
                SoundSource.HOSTILE, 1.0F, 0.72F + random.nextFloat() * 0.2F);
        discard();
    }

    private void blockedImpact(ServerLevel level, Vec3 impact) {
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                impact.x, impact.y, impact.z,
                10, 0.2D, 0.2D, 0.2D, 0.05D);
        level.playSound(null, blockPosition(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                SoundSource.HOSTILE, 0.45F, 1.65F);
    }

    private void impactParticles(ServerLevel level, Vec3 impact) {
        level.sendParticles(ModParticles.METHOD_QI_DARK.get(),
                impact.x, impact.y, impact.z,
                24, 0.65D, 0.35D, 0.65D, 0.055D);
        level.sendParticles(ParticleTypes.PORTAL,
                impact.x, impact.y, impact.z,
                30, 0.75D, 0.45D, 0.75D, 0.18D);
        level.sendParticles(ParticleTypes.END_ROD,
                impact.x, impact.y, impact.z,
                8, 0.35D, 0.25D, 0.35D, 0.045D);
    }
}
