package com.example.immortal_cultivation_mod.entity;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class FuchenBindingPlumeEntity extends Entity implements GeoEntity {
    private static final int FALLBACK_LIFETIME_TICKS = 65;
    private static final RawAnimation WRAP_AND_HOLD =
            RawAnimation.begin()
                    .thenPlay("animation.fuchen_binding_plume.wrap")
                    .thenLoop("animation.fuchen_binding_plume.idle");

    private static final EntityDataAccessor<Integer> OWNER_ID =
            SynchedEntityData.defineId(
                    FuchenBindingPlumeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID =
            SynchedEntityData.defineId(
                    FuchenBindingPlumeEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private UUID ownerUuid;
    private UUID targetUuid;
    private int lifetimeTicks = FALLBACK_LIFETIME_TICKS;

    public FuchenBindingPlumeEntity(
            EntityType<? extends FuchenBindingPlumeEntity> type,
            Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    public static FuchenBindingPlumeEntity create(
            ServerLevel level,
            LivingEntity owner,
            LivingEntity target,
            int lifetimeTicks) {
        FuchenBindingPlumeEntity plume = ModEntities.FUCHEN_BINDING_PLUME.get().create(level);
        if (plume == null) {
            return null;
        }
        plume.ownerUuid = owner.getUUID();
        plume.targetUuid = target.getUUID();
        plume.entityData.set(OWNER_ID, owner.getId());
        plume.entityData.set(TARGET_ID, target.getId());
        plume.lifetimeTicks = Math.max(1, lifetimeTicks);
        plume.moveTo(target.getX(), target.getY(), target.getZ(), target.getYRot(), 0.0F);
        return plume;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER_ID, -1);
        builder.define(TARGET_ID, -1);
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        noPhysics = true;
        setDeltaMovement(Vec3.ZERO);

        Entity target = getTargetEntity();
        if (!(target instanceof LivingEntity livingTarget) || !livingTarget.isAlive()) {
            if (!level().isClientSide) {
                discard();
            }
            return;
        }

        moveTo(
                livingTarget.getX(),
                livingTarget.getY(),
                livingTarget.getZ(),
                livingTarget.getYRot(),
                0.0F);
        setYRot(livingTarget.getYRot());
        yRotO = livingTarget.yRotO;

        if (!level().isClientSide && --lifetimeTicks <= 0) {
            discard();
        }
    }

    public Entity getOwnerEntity() {
        Entity owner = level().getEntity(entityData.get(OWNER_ID));
        if (owner == null && level() instanceof ServerLevel serverLevel && ownerUuid != null) {
            owner = serverLevel.getEntity(ownerUuid);
            if (owner != null) {
                entityData.set(OWNER_ID, owner.getId());
            }
        }
        return owner;
    }

    public Entity getTargetEntity() {
        Entity target = level().getEntity(entityData.get(TARGET_ID));
        if (target == null && level() instanceof ServerLevel serverLevel && targetUuid != null) {
            target = serverLevel.getEntity(targetUuid);
            if (target != null) {
                entityData.set(TARGET_ID, target.getId());
            }
        }
        return target;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerUuid != null) {
            tag.putUUID("Owner", ownerUuid);
        }
        if (targetUuid != null) {
            tag.putUUID("Target", targetUuid);
        }
        tag.putInt("Lifetime", lifetimeTicks);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        targetUuid = tag.hasUUID("Target") ? tag.getUUID("Target") : null;
        lifetimeTicks = Math.max(1, tag.getInt("Lifetime"));
        setNoGravity(true);
        noPhysics = true;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                this,
                "main",
                0,
                state -> state.setAndContinue(WRAP_AND_HOLD)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
