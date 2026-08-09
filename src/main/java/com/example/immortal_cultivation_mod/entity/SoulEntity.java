package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SoulEntity extends PathfinderMob {
    public static final int LIFETIME_TICKS = 20 * 60 * 3;

    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(SoulEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> OWNER_NAME =
            SynchedEntityData.defineId(SoulEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> TEXTURE =
            SynchedEntityData.defineId(SoulEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SKIN_VALUE =
            SynchedEntityData.defineId(SoulEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SKIN_SIGNATURE =
            SynchedEntityData.defineId(SoulEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> SOUL_YAW =
            SynchedEntityData.defineId(SoulEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SOUL_VALUE =
            SynchedEntityData.defineId(SoulEntity.class, EntityDataSerializers.FLOAT);

    private long expiresAt;

    public SoulEntity(EntityType<? extends SoulEntity> entityType, Level level) {
        super(entityType, level);
        xpReward = 0;
        setNoAi(true);
        setNoGravity(true);
        noPhysics = true;
        setInvulnerable(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    public void copyFrom(ServerPlayer player) {
        entityData.set(OWNER_UUID, Optional.of(player.getUUID()));
        entityData.set(OWNER_NAME, player.getGameProfile().getName());
        entityData.set(TEXTURE, "");
        Property skin = player.getGameProfile().getProperties().get("textures").stream()
                .findFirst()
                .orElse(null);
        entityData.set(SKIN_VALUE, skin == null ? "" : skin.value());
        entityData.set(SKIN_SIGNATURE,
                skin == null || skin.signature() == null ? "" : skin.signature());
        entityData.set(SOUL_VALUE, (float) valueFor(player));
        initializePosition(player);
    }

    public void copyFromNpc(LivingEntity npc, ResourceLocation texture) {
        entityData.set(OWNER_UUID, Optional.empty());
        entityData.set(OWNER_NAME, npc.getName().getString());
        entityData.set(TEXTURE, texture == null ? "" : texture.toString());
        entityData.set(SKIN_VALUE, "");
        entityData.set(SKIN_SIGNATURE, "");
        entityData.set(SOUL_VALUE, (float) valueFor(npc));
        initializePosition(npc);
    }

    private static float valueForRealm(String realm) {
        return switch (CultivationLevels.getRealmIndex(realm)) {
            case 0 -> 0.1F;
            case 1 -> 1.0F;
            case 2 -> 20.0F;
            default -> 100.0F;
        };
    }

    public static double valueFor(LivingEntity entity) {
        String realm;
        if (entity instanceof ServerPlayer player) {
            realm = ModAttachments.getData(player).cultivationLevel();
        } else if (entity instanceof PathfinderMob mob && mob instanceof CultivationNpc cultivator) {
            realm = cultivator.npcRealm();
        } else {
            realm = CultivationLevels.REALM_MORTAL;
        }
        return valueForRealm(realm);
    }

    public double soulValue() {
        return Math.max(0.1D, entityData.get(SOUL_VALUE));
    }

    private void initializePosition(LivingEntity entity) {
        entityData.set(SOUL_YAW, entity.getYRot());
        moveTo(entity.getX(), entity.getY() + 0.08D, entity.getZ(), entity.getYRot(), 0.0F);
        expiresAt = level().getGameTime() + LIFETIME_TICKS;
        lockFacing();
    }

    public Optional<UUID> ownerUuid() {
        return entityData.get(OWNER_UUID);
    }

    public String textureId() {
        return entityData.get(TEXTURE);
    }

    public GameProfile getGameProfile() {
        GameProfile profile = new GameProfile(
                ownerUuid().orElse(Util.NIL_UUID), entityData.get(OWNER_NAME));
        String value = entityData.get(SKIN_VALUE);
        if (!value.isBlank()) {
            String signature = entityData.get(SKIN_SIGNATURE);
            profile.getProperties().put("textures", signature.isBlank()
                    ? new Property("textures", value)
                    : new Property("textures", value, signature));
        }
        return profile;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER_UUID, Optional.empty());
        builder.define(OWNER_NAME, "");
        builder.define(TEXTURE, "");
        builder.define(SKIN_VALUE, "");
        builder.define(SKIN_SIGNATURE, "");
        builder.define(SOUL_YAW, 0.0F);
        builder.define(SOUL_VALUE, 0.1F);
    }

    @Override
    public void tick() {
        super.tick();
        setNoAi(true);
        setNoGravity(true);
        noPhysics = true;
        setDeltaMovement(Vec3.ZERO);
        lockFacing();
        if (!level().isClientSide && level().getGameTime() >= expiresAt) {
            discard();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
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
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ownerUuid().ifPresent(uuid -> tag.putUUID("OwnerUuid", uuid));
        tag.putString("OwnerName", entityData.get(OWNER_NAME));
        tag.putString("Texture", textureId());
        tag.putString("SkinValue", entityData.get(SKIN_VALUE));
        tag.putString("SkinSignature", entityData.get(SKIN_SIGNATURE));
        tag.putFloat("SoulYaw", entityData.get(SOUL_YAW));
        tag.putDouble("SoulValue", soulValue());
        tag.putLong("ExpiresAt", expiresAt);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("OwnerUuid")) {
            entityData.set(OWNER_UUID, Optional.of(tag.getUUID("OwnerUuid")));
        }
        entityData.set(OWNER_NAME, tag.getString("OwnerName"));
        entityData.set(TEXTURE, tag.getString("Texture"));
        entityData.set(SKIN_VALUE, tag.getString("SkinValue"));
        entityData.set(SKIN_SIGNATURE, tag.getString("SkinSignature"));
        entityData.set(SOUL_YAW, tag.getFloat("SoulYaw"));
        entityData.set(SOUL_VALUE, Math.max(0.1F, tag.getFloat("SoulValue")));
        expiresAt = tag.contains("ExpiresAt") ? tag.getLong("ExpiresAt")
                : level().getGameTime() + LIFETIME_TICKS;
        setNoAi(true);
        setNoGravity(true);
        noPhysics = true;
        lockFacing();
    }

    private void lockFacing() {
        float yaw = entityData.get(SOUL_YAW);
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
