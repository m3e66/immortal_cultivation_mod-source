package com.example.immortal_cultivation_mod.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

public class JindanCloneEntity extends PathfinderMob {
    private static final EntityDataAccessor<Optional<UUID>> PLAYER_UUID =
            SynchedEntityData.defineId(JindanCloneEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> PLAYER_NAME =
            SynchedEntityData.defineId(JindanCloneEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> FROZEN_YAW =
            SynchedEntityData.defineId(JindanCloneEntity.class, EntityDataSerializers.FLOAT);

    public JindanCloneEntity(EntityType<? extends JindanCloneEntity> entityType, Level level) {
        super(entityType, level);
        xpReward = 0;
        setNoAi(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1024.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    public void copyFrom(ServerPlayer player) {
        entityData.set(PLAYER_UUID, Optional.of(player.getUUID()));
        entityData.set(PLAYER_NAME, player.getGameProfile().getName());
        entityData.set(FROZEN_YAW, player.getYRot());
        setCustomName(player.getDisplayName().copy());
        setCustomNameVisible(true);
        lockFacing();
    }

    public Optional<UUID> playerUuid() {
        return entityData.get(PLAYER_UUID);
    }

    public GameProfile getGameProfile() {
        return new GameProfile(playerUuid().orElse(Util.NIL_UUID), entityData.get(PLAYER_NAME));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PLAYER_UUID, Optional.empty());
        builder.define(PLAYER_NAME, "");
        builder.define(FROZEN_YAW, 0.0F);
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        lockFacing();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        playerUuid().ifPresent(uuid -> tag.putUUID("PlayerUuid", uuid));
        tag.putString("PlayerName", entityData.get(PLAYER_NAME));
        tag.putFloat("FrozenYaw", entityData.get(FROZEN_YAW));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("PlayerUuid")) {
            entityData.set(PLAYER_UUID, Optional.of(tag.getUUID("PlayerUuid")));
        }
        entityData.set(PLAYER_NAME, tag.getString("PlayerName"));
        entityData.set(FROZEN_YAW, tag.getFloat("FrozenYaw"));
        setNoAi(true);
        setPersistenceRequired();
        lockFacing();
    }

    private void lockFacing() {
        float yaw = entityData.get(FROZEN_YAW);
        setYRot(yaw);
        yRotO = yaw;
        yBodyRot = yaw;
        yBodyRotO = yaw;
        setYHeadRot(yaw);
        yHeadRotO = yaw;
        setXRot(0.0F);
        xRotO = 0.0F;
    }
}
