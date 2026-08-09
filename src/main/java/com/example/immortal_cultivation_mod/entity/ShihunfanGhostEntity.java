package com.example.immortal_cultivation_mod.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class ShihunfanGhostEntity extends PathfinderMob {
    private static final int LIFETIME_TICKS = 36;
    private static final double SPEED = 0.82D;
    private static final DustParticleOptions GHOST_DUST =
            new DustParticleOptions(new Vector3f(0.18F, 0.48F, 0.43F), 0.8F);

    private Vec3 target = Vec3.ZERO;

    public ShihunfanGhostEntity(EntityType<? extends ShihunfanGhostEntity> type, Level level) {
        super(type, level);
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

    public void initialize(Vec3 start, Vec3 target) {
        this.target = target;
        moveTo(start.x, start.y, start.z, 0.0F, 0.0F);
    }

    @Override
    public void tick() {
        super.tick();
        setNoAi(true);
        setNoGravity(true);
        noPhysics = true;
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 offset = target.subtract(position());
        if (tickCount > LIFETIME_TICKS || offset.lengthSqr() < 0.45D) {
            discard();
            return;
        }
        Vec3 motion = offset.normalize().scale(SPEED);
        setDeltaMovement(motion);
        setPos(position().add(motion));
        setYRot((float) (Math.toDegrees(Math.atan2(-motion.x, motion.z))));
        yRotO = getYRot();

        if (tickCount % 2 == 0) {
            level.sendParticles(GHOST_DUST, getX(), getY() + 0.9D, getZ(),
                    1, 0.12D, 0.28D, 0.12D, 0.015D);
        }
        if (tickCount % 4 == 0) {
            level.sendParticles(ParticleTypes.SOUL, getX(), getY() + 0.9D, getZ(),
                    1, 0.1D, 0.22D, 0.1D, 0.018D);
            level.sendParticles(ParticleTypes.ASH, getX(), getY() + 0.75D, getZ(),
                    1, 0.14D, 0.24D, 0.14D, 0.015D);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble("TargetX", target.x);
        tag.putDouble("TargetY", target.y);
        tag.putDouble("TargetZ", target.z);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        target = new Vec3(tag.getDouble("TargetX"), tag.getDouble("TargetY"), tag.getDouble("TargetZ"));
        setNoAi(true);
        setNoGravity(true);
        noPhysics = true;
        setInvulnerable(true);
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
}
