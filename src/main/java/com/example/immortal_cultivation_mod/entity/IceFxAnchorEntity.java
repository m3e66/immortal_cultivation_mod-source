package com.example.immortal_cultivation_mod.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class IceFxAnchorEntity extends Entity {
    private static final int LIFETIME = 30;

    public IceFxAnchorEntity(EntityType<? extends IceFxAnchorEntity> entityType, Level level) {
        super(entityType, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    public void tick() {
        noPhysics = true;
        setNoGravity(true);
        super.tick();
        if (!level().isClientSide && tickCount > LIFETIME) {
            discard();
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
