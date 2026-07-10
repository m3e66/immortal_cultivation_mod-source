package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attribute.ModAttributes;
import com.mojang.authlib.GameProfile;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class CultivatorCorpseEntity extends PathfinderMob {
    private static final EntityDataAccessor<Optional<UUID>> PLAYER_UUID =
            SynchedEntityData.defineId(CultivatorCorpseEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> PLAYER_NAME =
            SynchedEntityData.defineId(CultivatorCorpseEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> CORPSE_YAW =
            SynchedEntityData.defineId(CultivatorCorpseEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> RAISED =
            SynchedEntityData.defineId(CultivatorCorpseEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SEALED =
            SynchedEntityData.defineId(CultivatorCorpseEntity.class, EntityDataSerializers.BOOLEAN);

    public CultivatorCorpseEntity(EntityType<? extends CultivatorCorpseEntity> entityType, Level level) {
        super(entityType, level);
        setNoAi(true);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(ModAttributes.GRUDGE, 0.0D)
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.ATTACK_SPEED, 4.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 0.0D)
                .add(Attributes.LUCK, 0.0D)
                .add(Attributes.SCALE, 1.0D)
                .add(Attributes.STEP_HEIGHT, 0.0D)
                .add(Attributes.SAFE_FALL_DISTANCE, 3.0D)
                .add(Attributes.FALL_DAMAGE_MULTIPLIER, 1.0D)
                .add(Attributes.GRAVITY, 0.08D)
                .add(Attributes.JUMP_STRENGTH, 0.0D)
                .add(Attributes.OXYGEN_BONUS, 0.0D)
                .add(Attributes.BURNING_TIME, 1.0D)
                .add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 0.0D)
                .add(Attributes.WATER_MOVEMENT_EFFICIENCY, 0.0D)
                .add(Attributes.MOVEMENT_EFFICIENCY, 0.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0D);
    }

    public void copyFrom(ServerPlayer player) {
        entityData.set(PLAYER_UUID, Optional.of(player.getUUID()));
        entityData.set(PLAYER_NAME, player.getGameProfile().getName());
        entityData.set(CORPSE_YAW, player.getYRot());

        setPos(player.getX(), player.getY(), player.getZ());
        lockFacing();

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setItemSlot(slot, player.getItemBySlot(slot).copy());
            setDropChance(slot, 0.0F);
        }

        copyAttributesFrom(player);
        setHealth(Math.max(1.0F, getMaxHealth() * 0.5F));
    }

    public Optional<UUID> getPlayerUuid() {
        return entityData.get(PLAYER_UUID);
    }

    public String getPlayerName() {
        return entityData.get(PLAYER_NAME);
    }

    public GameProfile getGameProfile() {
        return new GameProfile(getPlayerUuid().orElse(Util.NIL_UUID), getPlayerName());
    }

    public float getCorpseYaw() {
        return entityData.get(CORPSE_YAW);
    }

    public boolean isRaised() {
        return entityData.get(RAISED);
    }

    public void setRaised(boolean raised) {
        entityData.set(RAISED, raised);
        setNoAi(!raised || isSealed());
        refreshDimensions();
    }

    public boolean isSealed() {
        return entityData.get(SEALED);
    }

    public void setSealed(boolean sealed) {
        entityData.set(SEALED, sealed);
        setNoAi(sealed || !isRaised());
        refreshDimensions();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.18D, true));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PLAYER_UUID, Optional.empty());
        builder.define(PLAYER_NAME, "");
        builder.define(CORPSE_YAW, 0.0F);
        builder.define(RAISED, false);
        builder.define(SEALED, false);
    }

    @Override
    public void tick() {
        setNoGravity(false);
        super.tick();
        if (!isRaised() || isSealed()) {
            applyPinnedGravity();
            lockFacing();
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!isRaised() || isSealed()) {
            Vec3 movement = getDeltaMovement();
            setDeltaMovement(0.0D, movement.y, 0.0D);
            lockFacing();
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        getPlayerUuid().ifPresent(uuid -> tag.putUUID("PlayerUuid", uuid));
        tag.putString("PlayerName", getPlayerName());
        tag.putFloat("CorpseYaw", getCorpseYaw());
        tag.putBoolean("Raised", isRaised());
        tag.putBoolean("Sealed", isSealed());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("PlayerUuid")) {
            entityData.set(PLAYER_UUID, Optional.of(tag.getUUID("PlayerUuid")));
        }
        entityData.set(PLAYER_NAME, tag.getString("PlayerName"));
        entityData.set(CORPSE_YAW, tag.getFloat("CorpseYaw"));
        entityData.set(RAISED, tag.getBoolean("Raised"));
        entityData.set(SEALED, tag.getBoolean("Sealed"));
        setNoAi(!isRaised() || isSealed());
        setNoGravity(false);
        setPersistenceRequired();
        lockFacing();
    }

    private void applyPinnedGravity() {
        if (onGround()) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        double y = Math.max(-3.92D, getDeltaMovement().y - 0.08D);
        Vec3 falling = new Vec3(0.0D, y, 0.0D);
        setDeltaMovement(falling);
        move(MoverType.SELF, falling);
        setDeltaMovement(0.0D, y * 0.98D, 0.0D);
    }

    private void copyAttributesFrom(ServerPlayer player) {
        for (AttributeInstance source : player.getAttributes().getSyncableAttributes()) {
            AttributeInstance target = getAttribute(source.getAttribute());
            if (target == null) {
                continue;
            }
            target.setBaseValue(source.getBaseValue());
            source.getModifiers().forEach(target::addPermanentModifier);
        }
    }

    private void lockFacing() {
        float yaw = getCorpseYaw();
        setYRot(yaw);
        setXRot(0.0F);
        yBodyRot = yaw;
        yBodyRotO = yaw;
        yHeadRot = yaw;
        yHeadRotO = yaw;
    }
}
