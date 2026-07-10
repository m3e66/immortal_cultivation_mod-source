package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.UUID;

public class DuanliuKongdunDomeEntity extends Entity {
    public static final double RADIUS = 10.0D;
    private static final int LIFETIME_TICKS = 20 * 60;
    private static final int QI_DRAIN_PERCENT_PER_SECOND = 3;
    private static final DustParticleOptions BLUE_DUST = new DustParticleOptions(new Vector3f(0.15F, 0.55F, 1.0F), 1.25F);
    private static final DustParticleOptions PALE_DUST = new DustParticleOptions(new Vector3f(0.65F, 0.9F, 1.0F), 0.85F);
    private UUID ownerUuid;

    public DuanliuKongdunDomeEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public DuanliuKongdunDomeEntity(Level level, ServerPlayer owner, Vec3 pos) {
        this(ModEntities.DUANLIU_KONGDUN_DOME.get(), level);
        ownerUuid = owner.getUUID();
        setPos(pos.x, pos.y, pos.z);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (tickCount > LIFETIME_TICKS || ownerDead(level)) {
            discard();
            return;
        }
        tickEntities(level);
        tickProjectiles(level);
        if (tickCount % 5 == 0) {
            renderDome(level);
        }
    }

    private boolean ownerDead(ServerLevel level) {
        return ownerUuid != null && (level.getPlayerByUUID(ownerUuid) == null || !level.getPlayerByUUID(ownerUuid).isAlive());
    }

    private void tickEntities(ServerLevel level) {
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(RADIUS + 2.0D))) {
            Vec3 center = position();
            Vec3 offset = entity.position().subtract(center);
            double dist = offset.length();
            boolean inside = dist < RADIUS;
            String key = insideKey();
            if (tickCount <= 1 && inside) {
                entity.getPersistentData().putBoolean(key, true);
            }
            boolean wasInside = entity.getPersistentData().getBoolean(key);
            if (inside && wasInside) {
                if (entity instanceof ServerPlayer player) {
                    drainPlayerQi(player);
                }
            } else if (inside) {
                pushToBoundary(level, entity, center, offset, RADIUS + 0.35D);
            } else if (wasInside) {
                pushToBoundary(level, entity, center, offset, RADIUS - 0.35D);
            } else {
                entity.getPersistentData().putBoolean(key, false);
            }
        }
    }

    private void drainPlayerQi(ServerPlayer player) {
        if (player.tickCount % 20 == 0) {
            var data = ModAttachments.getData(player);
            int maxQi = Math.max(1, CultivationLevels.getLevelDef(data.cultivationLevel()).maxQi() + data.maxQiBonus());
            ServerEvents.spendQiOrBlood(player, data, Math.max(1, maxQi * QI_DRAIN_PERCENT_PER_SECOND / 100));
            ServerEvents.syncPlayerData(player);
        }
    }

    private String insideKey() {
        return "DuanliuKongdunInside_" + getUUID();
    }

    private void pushToBoundary(ServerLevel level, LivingEntity entity, Vec3 center, Vec3 offset, double radius) {
        Vec3 dir = offset.lengthSqr() < 0.001D ? entity.getLookAngle().scale(-1.0D).normalize() : offset.normalize();
        Vec3 locked = center.add(dir.scale(radius));
        entity.teleportTo(locked.x, entity.getY(), locked.z);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.hurtMarked = true;
        level.sendParticles(ParticleTypes.SPLASH, entity.getX(), entity.getY() + 1.0D, entity.getZ(),
                34, 0.35D, 0.6D, 0.35D, 0.08D);
        level.sendParticles(BLUE_DUST, entity.getX(), entity.getY() + 1.0D, entity.getZ(),
                22, 0.35D, 0.55D, 0.35D, 0.0D);
    }

    private void tickProjectiles(ServerLevel level) {
        for (Entity entity : level.getEntities(this, getBoundingBox().inflate(RADIUS + 1.5D),
                entity -> entity instanceof Projectile || entity instanceof ZhenshanPalmEntity || entity instanceof GudiaoScratchEntity)) {
            double dist = entity.position().distanceTo(position());
            if (Math.abs(dist - RADIUS) <= 1.0D) {
                entity.discard();
            }
        }
    }

    private void renderDome(ServerLevel level) {
        int points = 160;
        double[] rings = {0.15D, 0.9D, 1.65D, 2.4D, 3.15D, 4.05D};
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0D * i) / points;
            double x = getX() + Math.cos(angle) * RADIUS;
            double z = getZ() + Math.sin(angle) * RADIUS;
            for (int j = 0; j < rings.length; j++) {
                level.sendParticles(j % 2 == 0 ? BLUE_DUST : PALE_DUST, x, getY() + rings[j], z,
                        1, 0.025D, 0.035D, 0.025D, 0.0D);
            }
            if (i % 2 == 0) {
                level.sendParticles(ParticleTypes.SPLASH, x, getY() + 1.8D, z, 2, 0.05D, 0.28D, 0.05D, 0.015D);
            }
            if (i % 4 == 0) {
                level.sendParticles(ParticleTypes.BUBBLE_POP, x, getY() + 2.6D, z, 2, 0.08D, 0.65D, 0.08D, 0.02D);
                level.sendParticles(ParticleTypes.FALLING_WATER, x, getY() + 4.1D, z, 1, 0.05D, 0.12D, 0.05D, 0.0D);
            }
        }
        level.sendParticles(BLUE_DUST, getX(), getY() + 5.0D, getZ(), 70, RADIUS * 0.55D, 0.75D, RADIUS * 0.55D, 0.0D);
        level.sendParticles(PALE_DUST, getX(), getY() + 3.2D, getZ(), 90, RADIUS * 0.48D, 1.4D, RADIUS * 0.48D, 0.0D);
        level.sendParticles(ParticleTypes.BUBBLE_POP, getX(), getY() + 2.0D, getZ(), 90, RADIUS * 0.65D, 1.8D, RADIUS * 0.65D, 0.035D);
        level.sendParticles(ParticleTypes.FALLING_WATER, getX(), getY() + 3.8D, getZ(), 70, RADIUS * 0.55D, 0.5D, RADIUS * 0.55D, 0.0D);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            ownerUuid = tag.getUUID("Owner");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerUuid != null) {
            tag.putUUID("Owner", ownerUuid);
        }
    }
}
