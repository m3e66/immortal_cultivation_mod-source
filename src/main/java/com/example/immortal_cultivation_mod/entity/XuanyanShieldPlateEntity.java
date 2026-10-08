package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.spell.CultivationShieldSpells;
import java.util.List;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** One of the three OBJ-backed stone plates orbiting a 玄岩护体 caster. */
public final class XuanyanShieldPlateEntity extends Entity {
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(XuanyanShieldPlateEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DATA_INDEX =
            SynchedEntityData.defineId(XuanyanShieldPlateEntity.class, EntityDataSerializers.INT);

    public XuanyanShieldPlateEntity(EntityType<? extends XuanyanShieldPlateEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER, Optional.empty());
        builder.define(DATA_INDEX, 0);
    }

    public UUID ownerId() {
        return entityData.get(DATA_OWNER).orElse(null);
    }

    public int plateIndex() {
        return Math.max(0, Math.min(2, entityData.get(DATA_INDEX)));
    }

    private void configure(ServerPlayer owner, int index) {
        entityData.set(DATA_OWNER, Optional.of(owner.getUUID()));
        entityData.set(DATA_INDEX, Math.max(0, Math.min(2, index)));
        updateOrbit(owner);
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        noPhysics = true;
        UUID ownerId = ownerId();
        Player owner = ownerId == null ? null : level().getPlayerByUUID(ownerId);
        if (!level().isClientSide) {
            if (owner == null || !owner.isAlive()
                    || CultivationShieldSpells.xuanyanPlateCount(ownerId) <= plateIndex()) {
                discard();
                return;
            }
        }
        if (owner != null) {
            updateOrbit(owner);
        }
    }

    private void updateOrbit(Player owner) {
        double angle = level().getGameTime() * 0.045D
                + plateIndex() * Math.PI * 2.0D / 3.0D;
        Vec3 radial = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
        Vec3 center = owner.position().add(0.0D, owner.getBbHeight() * 0.52D, 0.0D);
        Vec3 point = center.add(radial.scale(1.15D));
        setPos(point.x, point.y, point.z);
        setYRot((float) Math.toDegrees(Math.atan2(-radial.x, radial.z)));
    }

    public static void ensurePlates(ServerPlayer owner) {
        ServerLevel level = owner.serverLevel();
        AABB search = owner.getBoundingBox().inflate(6.0D);
        List<XuanyanShieldPlateEntity> existing = level.getEntitiesOfClass(
                XuanyanShieldPlateEntity.class, search,
                plate -> owner.getUUID().equals(plate.ownerId()));
        boolean[] found = new boolean[3];
        for (XuanyanShieldPlateEntity plate : existing) {
            int index = plate.plateIndex();
            if (found[index]) {
                plate.discard();
            } else {
                found[index] = true;
            }
        }
        for (int index = 0; index < 3; index++) {
            if (found[index]) {
                continue;
            }
            XuanyanShieldPlateEntity plate = ModEntities.XUANYAN_SHIELD_PLATE.get().create(level);
            if (plate != null) {
                plate.configure(owner, index);
                level.addFreshEntity(plate);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(DATA_OWNER,
                tag.hasUUID("Owner") ? Optional.of(tag.getUUID("Owner")) : Optional.empty());
        entityData.set(DATA_INDEX, Math.max(0, Math.min(2, tag.getInt("PlateIndex"))));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        UUID ownerId = ownerId();
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putInt("PlateIndex", plateIndex());
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
