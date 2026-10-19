package com.example.immortal_cultivation_mod.entity;

import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Three-second damaging ember patch left by the fire owl's feather impacts. */
public final class CultivationBeastFirePatchEntity extends Entity {
    private UUID ownerId;
    private float damagePerSecond = 20.0F;
    private int lifeTicks = 60;
    private double radius = 1.35D;

    public CultivationBeastFirePatchEntity(
            EntityType<? extends CultivationBeastFirePatchEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
        noCulling = true;
    }

    public void configure(Entity owner, float damagePerSecond, int lifeTicks, double radius) {
        ownerId = owner == null ? null : owner.getUUID();
        this.damagePerSecond = Math.max(0.0F, damagePerSecond);
        this.lifeTicks = Math.max(1, lifeTicks);
        this.radius = Math.max(0.25D, radius);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (--lifeTicks <= 0) {
            discard();
            return;
        }
        if (!(level() instanceof ServerLevel server)) return;
        if (tickCount % 20 != 0) return;
        Entity owner = ownerId == null ? null : server.getEntity(ownerId);
        AABB area = getBoundingBox().inflate(radius, 0.8D, radius);
        for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, area,
                target -> target.isAlive() && target != owner
                        && !(target instanceof CultivationBestiaryBeastEntity))) {
            if (target.hurt(damageSources().indirectMagic(this, owner), damagePerSecond)) {
                target.igniteForSeconds(2.0F);
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        damagePerSecond = Math.max(0.0F, tag.getFloat("DamagePerSecond"));
        lifeTicks = Math.max(1, tag.getInt("LifeTicks"));
        radius = Math.max(0.25D, tag.getDouble("Radius"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) tag.putUUID("Owner", ownerId);
        tag.putFloat("DamagePerSecond", damagePerSecond);
        tag.putInt("LifeTicks", lifeTicks);
        tag.putDouble("Radius", radius);
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
