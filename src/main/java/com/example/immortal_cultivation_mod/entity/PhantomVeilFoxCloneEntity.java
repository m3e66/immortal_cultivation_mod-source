package com.example.immortal_cultivation_mod.entity;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** A one-hit, no-damage body used by the Phantom Veil Fox's illusions. */
public final class PhantomVeilFoxCloneEntity extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(PhantomVeilFoxCloneEntity.class,
                    EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> TARGET_UUID =
            SynchedEntityData.defineId(PhantomVeilFoxCloneEntity.class,
                    EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> ATTACKING =
            SynchedEntityData.defineId(PhantomVeilFoxCloneEntity.class,
                    EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> RENDER_SCALE =
            SynchedEntityData.defineId(PhantomVeilFoxCloneEntity.class,
                    EntityDataSerializers.FLOAT);

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin()
            .thenLoop("animation.phantom_veil_fox.idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin()
            .thenLoop("animation.phantom_veil_fox.walk");
    private static final RawAnimation RUN_ANIM = RawAnimation.begin()
            .thenLoop("animation.phantom_veil_fox.run");
    private static final RawAnimation POUNCE_ANIM = RawAnimation.begin()
            .thenPlay("animation.phantom_veil_fox.pounce");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int remainingLife = 80;
    private int fakeAttackTicks;
    private int movementRefresh;
    private final double orbitPhase = Math.random() * Math.PI * 2.0D;

    public PhantomVeilFoxCloneEntity(
            EntityType<? extends PhantomVeilFoxCloneEntity> type, Level level) {
        super(type, level);
        xpReward = 0;
        noCulling = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.62D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    public void configure(PhantomVeilFoxEntity owner, LivingEntity target, int lifetime) {
        entityData.set(OWNER_UUID, Optional.of(owner.getUUID()));
        entityData.set(TARGET_UUID, target == null ? Optional.empty() : Optional.of(target.getUUID()));
        remainingLife = Math.max(1, lifetime);
        entityData.set(RENDER_SCALE, owner.realmTier().renderScale());
        setYRot(owner.getYRot());
    }

    public Optional<UUID> ownerUuid() {
        return entityData.get(OWNER_UUID);
    }

    public float renderScale() {
        return entityData.get(RENDER_SCALE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER_UUID, Optional.empty());
        builder.define(TARGET_UUID, Optional.empty());
        builder.define(ATTACKING, false);
        builder.define(RENDER_SCALE, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (tickCount % 5 == 0) {
                level().addParticle(ParticleTypes.WITCH,
                        getX() + (random.nextDouble() - 0.5D) * 0.5D,
                        getY() + 1.15D,
                        getZ() + (random.nextDouble() - 0.5D) * 0.5D,
                        0.0D, 0.01D, 0.0D);
            }
            return;
        }
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        PhantomVeilFoxEntity owner = owner(serverLevel);
        if (owner == null || !owner.isAlive() || --remainingLife <= 0) {
            breakApart(serverLevel, false, null);
            return;
        }

        LivingEntity target = resolveTarget(serverLevel, owner);
        if (target == null || !target.isAlive()) {
            getNavigation().moveTo(owner, 1.05D);
            return;
        }

        if (--movementRefresh <= 0) {
            movementRefresh = 8 + random.nextInt(5);
            double angle = orbitPhase + tickCount * 0.075D;
            double radius = 2.4D + random.nextDouble() * 1.8D;
            double x = target.getX() + Math.cos(angle) * radius;
            double z = target.getZ() + Math.sin(angle) * radius;
            getNavigation().moveTo(x, target.getY(), z, 1.18D);
        }
        getLookControl().setLookAt(target, 55.0F, 55.0F);

        if (fakeAttackTicks > 0) {
            fakeAttackTicks--;
            if (fakeAttackTicks == 0) {
                entityData.set(ATTACKING, false);
            }
        } else if (distanceToSqr(target) <= 12.25D && random.nextInt(22) == 0) {
            fakeAttackTicks = 12;
            entityData.set(ATTACKING, true);
            swing(getUsedItemHand());
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ(),
                    5, 0.25D, 0.35D, 0.25D, 0.02D);
        }
    }

    private LivingEntity resolveTarget(ServerLevel level, PhantomVeilFoxEntity owner) {
        Optional<UUID> targetId = entityData.get(TARGET_UUID);
        if (targetId.isPresent() && level.getEntity(targetId.get()) instanceof LivingEntity living) {
            return living;
        }
        LivingEntity target = owner.getTarget();
        if (target != null) {
            entityData.set(TARGET_UUID, Optional.of(target.getUUID()));
        }
        return target;
    }

    private PhantomVeilFoxEntity owner(ServerLevel level) {
        Optional<UUID> ownerId = ownerUuid();
        return ownerId.isPresent() && level.getEntity(ownerId.get()) instanceof PhantomVeilFoxEntity fox
                ? fox : null;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || !(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        PhantomVeilFoxEntity owner = owner(serverLevel);
        if (owner != null && source.getEntity() == owner) {
            return false;
        }
        breakApart(serverLevel, source.getEntity() != null, source.getEntity());
        return true;
    }

    private void breakApart(ServerLevel level, boolean struck, Entity attacker) {
        level.sendParticles(ParticleTypes.WITCH, getX(), getY() + 0.7D, getZ(),
                18, 0.5D, 0.65D, 0.5D, 0.05D);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 0.7D, getZ(),
                10, 0.4D, 0.5D, 0.4D, 0.04D);
        PhantomVeilFoxEntity owner = owner(level);
        if (struck && owner != null) {
            owner.onCloneDestroyed(attacker);
        }
        discard();
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ownerUuid().ifPresent(uuid -> tag.putUUID("Owner", uuid));
        entityData.get(TARGET_UUID).ifPresent(uuid -> tag.putUUID("Target", uuid));
        tag.putInt("Life", remainingLife);
        tag.putFloat("RenderScale", renderScale());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            entityData.set(OWNER_UUID, Optional.of(tag.getUUID("Owner")));
        }
        if (tag.hasUUID("Target")) {
            entityData.set(TARGET_UUID, Optional.of(tag.getUUID("Target")));
        }
        remainingLife = Math.max(1, tag.getInt("Life"));
        if (tag.contains("RenderScale")) {
            entityData.set(RENDER_SCALE, tag.getFloat("RenderScale"));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 2, state -> {
            if (entityData.get(ATTACKING)) {
                return state.setAndContinue(POUNCE_ANIM);
            }
            return state.isMoving()
                    ? state.setAndContinue(getDeltaMovement().horizontalDistanceSqr() > 0.08D
                            ? RUN_ANIM : WALK_ANIM)
                    : state.setAndContinue(IDLE_ANIM);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
