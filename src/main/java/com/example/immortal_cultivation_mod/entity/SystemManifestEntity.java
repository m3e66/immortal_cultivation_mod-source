package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.system.OuterSystemManager;
import com.example.immortal_cultivation_mod.system.OuterSystemType;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SystemManifestEntity extends PathfinderMob {
    private static final EntityDataAccessor<Optional<UUID>> OWNER =
            SynchedEntityData.defineId(
                    SystemManifestEntity.class,
                    EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> SYSTEM_TYPE =
            SynchedEntityData.defineId(
                    SystemManifestEntity.class,
                    EntityDataSerializers.STRING);

    public SystemManifestEntity(
            EntityType<? extends SystemManifestEntity> type, Level level) {
        super(type, level);
        xpReward = 0;
        setNoAi(true);
        setNoGravity(true);
        noPhysics = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    public void initialize(ServerPlayer owner, OuterSystemType type) {
        entityData.set(OWNER, Optional.of(owner.getUUID()));
        entityData.set(SYSTEM_TYPE, type.id());
        setHealth(getMaxHealth());
        follow(owner);
    }

    public Optional<UUID> ownerId() {
        return entityData.get(OWNER);
    }

    public OuterSystemType systemType() {
        return OuterSystemType.byId(entityData.get(SYSTEM_TYPE));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER, Optional.empty());
        builder.define(SYSTEM_TYPE, OuterSystemType.KILL_GROWTH.id());
    }

    @Override
    public void tick() {
        super.tick();
        setNoAi(true);
        setNoGravity(true);
        noPhysics = true;
        setDeltaMovement(Vec3.ZERO);
        if (level().isClientSide) {
            return;
        }
        Optional<UUID> ownerUuid = ownerId();
        if (ownerUuid.isEmpty()
                || !(level().getServer().getPlayerList().getPlayer(
                        ownerUuid.get()) instanceof ServerPlayer owner)
                || !owner.isAlive()
                || owner.level() != level()
                || !OuterSystemManager.isScreenOpen(owner)) {
            discard();
            return;
        }
        follow(owner);
    }

    private void follow(ServerPlayer owner) {
        Vec3 look = owner.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 1.0E-5D) {
            horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        }
        horizontal = horizontal.normalize();
        Vec3 position = owner.position()
                .add(horizontal.scale(2.35D))
                .add(0.0D, 1.0D, 0.0D);
        moveTo(position.x, position.y, position.z, owner.getYRot(), 0.0F);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!(source.getEntity() instanceof ServerPlayer attacker)
                || CultivationLevels.getRealmIndex(
                        ModAttachments.getData(attacker).cultivationLevel()) < 5) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) {
            OuterSystemManager.destroySystem(this);
        }
        super.die(source);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ownerId().ifPresent(uuid -> tag.putUUID("Owner", uuid));
        tag.putString("SystemType", entityData.get(SYSTEM_TYPE));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            entityData.set(OWNER, Optional.of(tag.getUUID("Owner")));
        }
        entityData.set(SYSTEM_TYPE, tag.getString("SystemType"));
        setNoAi(true);
        setNoGravity(true);
        noPhysics = true;
    }
}
