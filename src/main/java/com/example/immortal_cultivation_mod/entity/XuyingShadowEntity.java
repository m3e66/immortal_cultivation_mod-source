package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.event.ServerEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.Optional;
import java.util.UUID;

public class XuyingShadowEntity extends PathfinderMob {
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(XuyingShadowEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Float> FROZEN_YAW =
            SynchedEntityData.defineId(XuyingShadowEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FROZEN_BODY_YAW =
            SynchedEntityData.defineId(XuyingShadowEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FROZEN_HEAD_YAW =
            SynchedEntityData.defineId(XuyingShadowEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FROZEN_X_ROT =
            SynchedEntityData.defineId(XuyingShadowEntity.class, EntityDataSerializers.FLOAT);

    public XuyingShadowEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        xpReward = 0;
        setNoAi(true);
    }

    public XuyingShadowEntity(Level level, Player owner) {
        this(ModEntities.XUYING_SHADOW.get(), level);
        entityData.set(OWNER_UUID, Optional.of(owner.getUUID()));
        entityData.set(FROZEN_YAW, owner.getYRot());
        entityData.set(FROZEN_BODY_YAW, owner.yBodyRot);
        entityData.set(FROZEN_HEAD_YAW, owner.getYHeadRot());
        entityData.set(FROZEN_X_ROT, owner.getXRot());
        moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
        setYBodyRot(owner.yBodyRot);
        setYHeadRot(owner.getYHeadRot());
        setPose(owner.getPose());
        setShiftKeyDown(owner.isShiftKeyDown());
        setSprinting(owner.isSprinting());
        setSwimming(owner.isSwimming());
        setHealth(1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER_UUID, Optional.empty());
        builder.define(FROZEN_YAW, 0.0F);
        builder.define(FROZEN_BODY_YAW, 0.0F);
        builder.define(FROZEN_HEAD_YAW, 0.0F);
        builder.define(FROZEN_X_ROT, 0.0F);
    }

    public Optional<UUID> ownerUuid() {
        return entityData.get(OWNER_UUID);
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
        lockPose();
        if (!level().isClientSide && tickCount > 20 * 20) {
            discard();
        }
    }

    @Override
    protected void tickDeath() {
        if (level() instanceof ServerLevel level && deathTime == 0) {
            explodeThoughts(level);
        }
        discard();
    }

    private void explodeThoughts(ServerLevel level) {
        level.explode(this, getX(), getY(0.5D), getZ(), 1.2F, Level.ExplosionInteraction.NONE);
        for (Player player : level.getEntitiesOfClass(Player.class, new AABB(blockPosition()).inflate(2.0D), Player::isAlive)) {
            ServerEvents.adjustThoughts(player, -10);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ownerUuid().ifPresent(uuid -> tag.putUUID("Owner", uuid));
        tag.putFloat("FrozenYaw", entityData.get(FROZEN_YAW));
        tag.putFloat("FrozenBodyYaw", entityData.get(FROZEN_BODY_YAW));
        tag.putFloat("FrozenHeadYaw", entityData.get(FROZEN_HEAD_YAW));
        tag.putFloat("FrozenXRot", entityData.get(FROZEN_X_ROT));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            entityData.set(OWNER_UUID, Optional.of(tag.getUUID("Owner")));
        }
        entityData.set(FROZEN_YAW, tag.getFloat("FrozenYaw"));
        entityData.set(FROZEN_BODY_YAW, tag.getFloat("FrozenBodyYaw"));
        entityData.set(FROZEN_HEAD_YAW, tag.getFloat("FrozenHeadYaw"));
        entityData.set(FROZEN_X_ROT, tag.getFloat("FrozenXRot"));
        lockPose();
    }

    private void lockPose() {
        float yaw = entityData.get(FROZEN_YAW);
        setYRot(yaw);
        yRotO = yaw;
        yBodyRot = entityData.get(FROZEN_BODY_YAW);
        yBodyRotO = yBodyRot;
        setYHeadRot(entityData.get(FROZEN_HEAD_YAW));
        yHeadRotO = getYHeadRot();
        setXRot(entityData.get(FROZEN_X_ROT));
        xRotO = getXRot();
    }
}
