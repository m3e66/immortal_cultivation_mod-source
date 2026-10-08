package com.example.immortal_cultivation_mod.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Short-lived synced anchor for the expanding 曜光闪 shell. */
public final class YaoguangFlashEntity extends Entity {
    public static final int LIFETIME = 10;
    private static final EntityDataAccessor<Float> DATA_RADIUS =
            SynchedEntityData.defineId(YaoguangFlashEntity.class, EntityDataSerializers.FLOAT);

    public YaoguangFlashEntity(EntityType<? extends YaoguangFlashEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
        noCulling = true;
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        noPhysics = true;
        if (!level().isClientSide && tickCount >= LIFETIME) {
            discard();
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RADIUS, 6.0F);
    }

    public void setRadius(float radius) {
        entityData.set(DATA_RADIUS, Math.max(0.1F, radius));
    }

    public float radius() {
        return Math.max(0.1F, entityData.get(DATA_RADIUS));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setRadius(tag.contains("Radius") ? tag.getFloat("Radius") : 6.0F);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Radius", radius());
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }
}
