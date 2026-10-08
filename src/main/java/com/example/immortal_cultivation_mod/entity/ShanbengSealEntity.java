package com.example.immortal_cultivation_mod.entity;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Centered visual model attached to a 山崩印 area. */
public final class ShanbengSealEntity extends Entity implements GeoEntity {
    private static final RawAnimation HOVER =
            RawAnimation.begin().thenLoop("animation.shanbeng_seal.hover");

    private static final EntityDataAccessor<Optional<UUID>> DATA_PARENT_UUID =
            SynchedEntityData.defineId(
                    ShanbengSealEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DATA_PARENT_ID =
            SynchedEntityData.defineId(
                    ShanbengSealEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ShanbengSealEntity(EntityType<? extends ShanbengSealEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
        noCulling = true;
    }

    public static ShanbengSealEntity create(
            ServerLevel level,
            ElementalTechniqueAreaEntity parent) {
        ShanbengSealEntity seal = ModEntities.SHANBENG_SEAL.get().create(level);
        if (seal == null) {
            return null;
        }
        seal.entityData.set(DATA_PARENT_UUID, Optional.of(parent.getUUID()));
        seal.entityData.set(DATA_PARENT_ID, parent.getId());
        seal.moveTo(parent.getX(), parent.getY() + 0.03D, parent.getZ(), 0.0F, 0.0F);
        return seal;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_PARENT_UUID, Optional.empty());
        builder.define(DATA_PARENT_ID, -1);
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        noPhysics = true;
        setDeltaMovement(Vec3.ZERO);

        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Entity parent = parentEntity();
        if (!(parent instanceof ElementalTechniqueAreaEntity area)
                || !area.isAlive()
                || area.kind() != ElementalTechniqueAreaEntity.Kind.SHANBENG_YIN) {
            discard();
            return;
        }
        entityData.set(DATA_PARENT_ID, area.getId());
        moveTo(area.getX(), area.getY() + 0.03D, area.getZ(), 0.0F, 0.0F);
    }

    public Entity parentEntity() {
        Entity parent = level().getEntity(entityData.get(DATA_PARENT_ID));
        UUID parentUuid = parentUuid();
        if (parent == null && level() instanceof ServerLevel serverLevel && parentUuid != null) {
            parent = serverLevel.getEntity(parentUuid);
            if (parent != null) {
                entityData.set(DATA_PARENT_ID, parent.getId());
            }
        }
        return parent;
    }

    public UUID parentUuid() {
        return entityData.get(DATA_PARENT_UUID).orElse(null);
    }

    public boolean belongsTo(UUID parentUuid) {
        return parentUuid != null && parentUuid.equals(parentUuid());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        UUID parentUuid = parentUuid();
        if (parentUuid != null) {
            tag.putUUID("Parent", parentUuid);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(DATA_PARENT_UUID,
                tag.hasUUID("Parent") ? Optional.of(tag.getUUID("Parent")) : Optional.empty());
        entityData.set(DATA_PARENT_ID, -1);
        setNoGravity(true);
        noPhysics = true;
        noCulling = true;
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
                this, "main", 0, state -> state.setAndContinue(HOVER)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
