package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.effect.PhotonEffects;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class LiuguangJianyingProjectileEntity extends ThrowableItemProjectile {
    private static final double REDIRECT_DISTANCE = 10.0D;
    private static final double REDIRECT_DISTANCE_SQR = REDIRECT_DISTANCE * REDIRECT_DISTANCE;
    private static final int MAX_REDIRECTS = 20;
    private static final float DAMAGE = 50.0F;
    private static final float SPEED = 2.45F;

    private int redirects;
    private int lifeTicks;
    private int photonTicks;
    private Vec3 lastRedirectPos = Vec3.ZERO;

    public LiuguangJianyingProjectileEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    public LiuguangJianyingProjectileEntity(Level level, LivingEntity shooter) {
        super(ModEntities.LIUGUANG_JIANYING_PROJECTILE.get(), shooter, level);
        setNoGravity(true);
        lastRedirectPos = position();
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SNOWBALL;
    }

    @Override
    public void tick() {
        setNoGravity(true);
        baseTick();
        lifeTicks++;
        if (lastRedirectPos == Vec3.ZERO) {
            lastRedirectPos = position();
        }

        if (level().isClientSide) {
            Vec3 movement = getDeltaMovement();
            level().addParticle(ParticleTypes.END_ROD, getX(), getY(), getZ(),
                    -movement.x * 0.05D, -movement.y * 0.05D, -movement.z * 0.05D);
            return;
        }

        if (photonTicks++ % 10 == 0) {
            PhotonEffects.liuguangJianyingProjectile(this);
        }

        Vec3 movement = getDeltaMovement();
        if (movement.lengthSqr() < 0.0001D) {
            redirectRandom();
            return;
        }

        Entity owner = getOwner();
        Vec3 start = position();
        Vec3 end = start.add(movement);
        if (owner != null && shouldRedirectAtRadius(owner, end)) {
            redirectNotAwayFromOwner(owner);
            return;
        }

        if (position().distanceToSqr(lastRedirectPos) >= REDIRECT_DISTANCE_SQR) {
            redirectRandom();
            return;
        }

        HitResult blockHit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        EntityHitResult entityHit = findEntityHit(start, end);
        HitResult hit = blockHit;
        if (entityHit != null) {
            double entityDist = start.distanceToSqr(entityHit.getLocation());
            double blockDist = blockHit.getType() == HitResult.Type.MISS ? Double.MAX_VALUE : start.distanceToSqr(blockHit.getLocation());
            if (entityDist <= blockDist) {
                hit = entityHit;
            }
        }
        if (hit.getType() != HitResult.Type.MISS) {
            setPos(hit.getLocation().x, hit.getLocation().y, hit.getLocation().z);
            if (hit instanceof EntityHitResult entityHitResult) {
                hitEntity(entityHitResult);
            } else if (hit instanceof BlockHitResult blockHitResult) {
                bounceFromBlock(blockHitResult);
            }
            return;
        }

        setPos(end.x, end.y, end.z);
        hasImpulse = true;

        if (lifeTicks > 20 * 20) {
            discardProjectile();
        }
    }

    private EntityHitResult findEntityHit(Vec3 start, Vec3 end) {
        AABB search = new AABB(start, end).inflate(0.45D);
        Entity closest = null;
        Vec3 closestPos = null;
        double closestDistance = Double.MAX_VALUE;
        for (Entity entity : level().getEntities(this, search, this::canHitEntity)) {
            AABB box = entity.getBoundingBox().inflate(0.3D);
            var clip = box.clip(start, end);
            if (clip.isEmpty()) {
                continue;
            }
            double distance = start.distanceToSqr(clip.get());
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = entity;
                closestPos = clip.get();
            }
        }
        return closest == null ? null : new EntityHitResult(closest, closestPos);
    }

    private boolean shouldRedirectAtRadius(Entity owner, Vec3 nextPos) {
        double currentDistance = distanceToSqr(owner);
        double nextDistance = nextPos.distanceToSqr(owner.position());
        return (currentDistance > REDIRECT_DISTANCE_SQR || nextDistance > REDIRECT_DISTANCE_SQR)
                && nextDistance >= currentDistance;
    }

    private void hitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        Entity owner = getOwner();
        if (owner == null || !target.getUUID().equals(owner.getUUID())) {
            target.hurt(damageSources().thrown(this, owner), DAMAGE);
        }
        redirectRandom();
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        Entity owner = getOwner();
        return (owner == null || !entity.getUUID().equals(owner.getUUID())) && super.canHitEntity(entity);
    }

    private void redirectRandom() {
        Vec3 current = getDeltaMovement();
        Vec3 randomDirection = new Vec3(
                random.nextDouble() - 0.5D,
                (random.nextDouble() - 0.35D) * 0.65D,
                random.nextDouble() - 0.5D
        );
        if (randomDirection.lengthSqr() < 0.001D) {
            randomDirection = current.lengthSqr() < 0.001D ? new Vec3(0.0D, 0.0D, 1.0D) : current.reverse();
        }
        redirect(randomDirection.normalize());
    }

    private void redirectNotAwayFromOwner(Entity owner) {
        Vec3 toOwner = owner.position().add(0.0D, owner.getEyeHeight() * 0.65D, 0.0D).subtract(position());
        if (toOwner.lengthSqr() < 0.001D) {
            redirectRandom();
            return;
        }
        Vec3 inward = toOwner.normalize();
        Vec3 candidate = Vec3.ZERO;
        for (int i = 0; i < 8; i++) {
            Vec3 randomDirection = new Vec3(
                    random.nextDouble() - 0.5D,
                    (random.nextDouble() - 0.5D) * 0.8D,
                    random.nextDouble() - 0.5D
            );
            if (randomDirection.lengthSqr() < 0.001D) {
                continue;
            }
            candidate = randomDirection.normalize();
            if (candidate.dot(inward) >= 0.12D) {
                break;
            }
            candidate = candidate.subtract(inward.scale(candidate.dot(inward))).normalize().add(inward.scale(0.25D)).normalize();
            break;
        }
        if (candidate.lengthSqr() < 0.001D) {
            candidate = inward;
        }
        redirect(candidate);
    }

    private void bounceFromBlock(BlockHitResult result) {
        Vec3 incoming = getDeltaMovement();
        if (incoming.lengthSqr() < 0.001D) {
            redirectRandom();
            return;
        }
        Direction face = result.getDirection();
        Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
        Vec3 reflected = incoming.subtract(normal.scale(2.0D * incoming.dot(normal)));
        if (reflected.lengthSqr() < 0.001D) {
            reflected = incoming.reverse();
        }
        setPos(getX() + normal.x * 0.08D, getY() + normal.y * 0.08D, getZ() + normal.z * 0.08D);
        redirect(reflected.normalize());
    }

    private void redirectToward(Vec3 target) {
        Vec3 direction = target.subtract(position());
        if (direction.lengthSqr() < 0.001D) {
            redirectRandom();
            return;
        }
        redirect(direction.normalize());
    }

    private void redirect(Vec3 direction) {
        redirects++;
        if (redirects >= MAX_REDIRECTS) {
            discardProjectile();
            return;
        }
        lastRedirectPos = position();
        setDeltaMovement(direction.scale(SPEED));
        hasImpulse = true;
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 12, 0.15D, 0.15D, 0.15D, 0.08D);
        }
    }

    private void discardProjectile() {
        if (level().isClientSide || isRemoved()) {
            return;
        }
        PhotonEffects.removeLiuguangJianyingProjectile(this);
        discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            PhotonEffects.removeLiuguangJianyingProjectile(this);
        }
        super.remove(reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Redirects", redirects);
        tag.putInt("LifeTicks", lifeTicks);
        tag.putDouble("LastRedirectX", lastRedirectPos.x);
        tag.putDouble("LastRedirectY", lastRedirectPos.y);
        tag.putDouble("LastRedirectZ", lastRedirectPos.z);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        redirects = tag.getInt("Redirects");
        lifeTicks = tag.getInt("LifeTicks");
        lastRedirectPos = new Vec3(tag.getDouble("LastRedirectX"), tag.getDouble("LastRedirectY"), tag.getDouble("LastRedirectZ"));
    }
}
