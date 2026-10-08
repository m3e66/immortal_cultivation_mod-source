package com.example.immortal_cultivation_mod.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

import javax.annotation.Nullable;

/**
 * A server-authoritative collision segment attached to a GeckoLib model bone.
 * Parts are never spawned or saved independently; NeoForge exposes them through the parent.
 */
public final class ModelBonePartEntity<T extends Entity> extends PartEntity<T> {
    private final String boneName;
    private EntityDimensions dimensions;

    public ModelBonePartEntity(T parent, String boneName, float width, float height) {
        super(parent);
        this.boneName = boneName;
        this.dimensions = EntityDimensions.scalable(width, height);
        refreshDimensions();
    }

    public String boneName() {
        return boneName;
    }

    /** Resizes dynamic multipart shells without recreating their network IDs. */
    public void resize(float width, float height) {
        float safeWidth = Math.max(0.01F, width);
        float safeHeight = Math.max(0.01F, height);
        if (Math.abs(dimensions.width() - safeWidth) < 1.0E-4F
                && Math.abs(dimensions.height() - safeHeight) < 1.0E-4F) {
            return;
        }
        dimensions = EntityDimensions.scalable(safeWidth, safeHeight);
        refreshDimensions();
    }

    /** Resolves a ray-traced bone segment to the real entity that owns it. */
    public static Entity resolveParent(Entity entity) {
        Entity resolved = entity;
        while (resolved instanceof ModelBonePartEntity<?> part) {
            resolved = part.getParent();
        }
        return resolved;
    }

    /** Resolves a bone segment and returns its living parent when applicable. */
    @Nullable
    public static LivingEntity resolveLiving(Entity entity) {
        Entity resolved = resolveParent(entity);
        return resolved instanceof LivingEntity living ? living : null;
    }

    /** Places this part using a model-space center rather than Entity's feet position. */
    public void moveCenterTo(Vec3 center) {
        setPos(center.x, center.y - dimensions.height() * 0.5D, center.z);
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

    @Override
    public boolean isPickable() {
        return getParent().isAlive();
    }

    @Nullable
    @Override
    public ItemStack getPickResult() {
        return getParent().getPickResult();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return !isInvulnerableTo(source) && getParent().hurt(source, amount);
    }

    @Override
    public boolean is(Entity entity) {
        return this == entity || getParent() == entity;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return dimensions;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
