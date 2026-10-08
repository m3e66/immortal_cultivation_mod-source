package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.spell.DariBatiJue;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Synced anchor for player-attached OBJ models used by solar techniques. */
public final class SolarTechniqueModelEntity extends Entity {
    public static final int HELD_SUN = 0;
    public static final int EXTREME_HALO = 1;

    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID =
            SynchedEntityData.defineId(
                    SolarTechniqueModelEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DATA_MODE =
            SynchedEntityData.defineId(
                    SolarTechniqueModelEntity.class, EntityDataSerializers.INT);

    public SolarTechniqueModelEntity(
            EntityType<? extends SolarTechniqueModelEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
        noCulling = true;
    }

    public static void spawnAttached(ServerLevel level, ServerPlayer owner, int mode) {
        for (SolarTechniqueModelEntity existing : level.getEntitiesOfClass(
                SolarTechniqueModelEntity.class,
                owner.getBoundingBox().inflate(4.0D),
                model -> model.mode() == mode
                        && model.ownerUuid().filter(owner.getUUID()::equals).isPresent())) {
            existing.discard();
        }
        SolarTechniqueModelEntity model = new SolarTechniqueModelEntity(
                ModEntities.SOLAR_TECHNIQUE_MODEL.get(), level);
        model.entityData.set(DATA_OWNER_UUID, Optional.of(owner.getUUID()));
        model.entityData.set(DATA_MODE, mode == EXTREME_HALO ? EXTREME_HALO : HELD_SUN);
        model.attachTo(owner);
        level.addFreshEntity(model);
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        noPhysics = true;
        if (level().isClientSide) {
            return;
        }
        ServerPlayer owner = ownerUuid()
                .map(uuid -> ((ServerLevel) level()).getServer()
                        .getPlayerList().getPlayer(uuid))
                .orElse(null);
        if (owner == null || !owner.isAlive() || owner.isRemoved()) {
            discard();
            return;
        }
        boolean active = mode() == HELD_SUN
                ? DariBatiJue.isHoldingSun(owner)
                : DariBatiJue.isExtreme(owner);
        if (!active) {
            discard();
            return;
        }
        attachTo(owner);
    }

    private void attachTo(ServerPlayer owner) {
        Vec3 position;
        if (mode() == HELD_SUN) {
            position = owner.getEyePosition()
                    .add(owner.getLookAngle().scale(0.70D))
                    .add(0.0D, 0.48D, 0.0D);
        } else {
            Vec3 forward = Vec3.directionFromRotation(0.0F, owner.yBodyRot);
            position = owner.position()
                    .add(forward.scale(-0.38D))
                    .add(0.0D, owner.getBbHeight() * 0.72D, 0.0D);
        }
        setPos(position.x, position.y, position.z);
        setYRot(owner.yBodyRot);
        yRotO = owner.yBodyRot;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER_UUID, Optional.empty());
        builder.define(DATA_MODE, HELD_SUN);
    }

    public Optional<UUID> ownerUuid() {
        return entityData.get(DATA_OWNER_UUID);
    }

    public int mode() {
        return entityData.get(DATA_MODE);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(DATA_OWNER_UUID,
                tag.hasUUID("Owner") ? Optional.of(tag.getUUID("Owner")) : Optional.empty());
        entityData.set(DATA_MODE, tag.getInt("Mode") == EXTREME_HALO
                ? EXTREME_HALO : HELD_SUN);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        ownerUuid().ifPresent(uuid -> tag.putUUID("Owner", uuid));
        tag.putInt("Mode", mode());
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
