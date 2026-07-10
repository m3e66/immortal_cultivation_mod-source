package com.example.immortal_cultivation_mod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GudiaoScratchEntity extends Entity {
    private static final EntityDataAccessor<Float> DATA_FORWARD_X =
            SynchedEntityData.defineId(GudiaoScratchEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_FORWARD_Y =
            SynchedEntityData.defineId(GudiaoScratchEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_FORWARD_Z =
            SynchedEntityData.defineId(GudiaoScratchEntity.class, EntityDataSerializers.FLOAT);
    private static final int LIFETIME = 24;
    private static final float DAMAGE = 70.0F;
    private static final double TOUCH_RADIUS = 0.45D;
    private static final double[][] SCRATCH_POINTS = {
            {-2.6D, 1.4D}, {-1.8D, 0.9D}, {-1.0D, 0.4D}, {-0.2D, -0.1D}, {0.6D, -0.6D}, {1.4D, -1.1D}, {2.2D, -1.6D},
            {-2.2D, 0.4D}, {-1.4D, 0.0D}, {-0.6D, -0.4D}, {0.2D, -0.8D}, {1.0D, -1.2D}, {1.8D, -1.6D}, {2.6D, -2.0D},
            {-2.4D, -0.6D}, {-1.6D, -0.9D}, {-0.8D, -1.2D}, {0.0D, -1.5D}, {0.8D, -1.8D}, {1.6D, -2.1D}, {2.4D, -2.4D}
    };

    private final Set<UUID> damaged = new HashSet<>();
    private UUID casterId;

    public GudiaoScratchEntity(EntityType<? extends GudiaoScratchEntity> entityType, Level level) {
        super(entityType, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public GudiaoScratchEntity(Level level, LivingEntity caster, LivingEntity target) {
        this(ModEntities.GUDIAO_SCRATCH.get(), level);
        casterId = caster.getUUID();
        Vec3 direction = target.position().add(0.0D, target.getBbHeight() * 0.45D, 0.0D)
                .subtract(caster.position().add(0.0D, caster.getBbHeight() * 0.55D, 0.0D));
        if (direction.lengthSqr() < 0.01D) {
            direction = caster.getLookAngle();
        }
        direction = direction.normalize();
        Vec3 start = caster.position().add(0.0D, caster.getBbHeight() * 0.55D, 0.0D).add(direction.scale(1.4D));
        moveTo(start.x, start.y, start.z, caster.getYRot(), caster.getXRot());
        setForward(direction);
        setDeltaMovement(direction.scale(0.95D));
    }

    @Override
    public void tick() {
        super.tick();
        noPhysics = true;
        setNoGravity(true);
        setPos(getX() + getDeltaMovement().x, getY() + getDeltaMovement().y, getZ() + getDeltaMovement().z);
        if (tickCount > LIFETIME) {
            discard();
            return;
        }

        Vec3 forward = forward();
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = forward.cross(up).normalize();
        if (right.lengthSqr() < 0.001D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        }
        up = right.cross(forward).normalize();

        for (double[] point : SCRATCH_POINTS) {
            Vec3 pos = position().add(right.scale(point[0])).add(up.scale(point[1]));
            if (level().isClientSide) {
                level().addParticle(ParticleTypes.CRIT, pos.x, pos.y, pos.z,
                        (random.nextDouble() - 0.5D) * 0.08D, 0.02D, (random.nextDouble() - 0.5D) * 0.08D);
                level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.RED_SAND.defaultBlockState()),
                        pos.x, pos.y, pos.z, 0.0D, 0.0D, 0.0D);
            } else {
                damageAt(pos);
                breakAt(pos);
                if (level() instanceof ServerLevel serverLevel && tickCount % 2 == 0) {
                    serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y, pos.z, 1, 0.04D, 0.04D, 0.04D, 0.0D);
                    serverLevel.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 1, 0.03D, 0.03D, 0.03D, 0.05D);
                }
            }
        }
    }

    private void damageAt(Vec3 pos) {
        AABB bounds = new AABB(pos, pos).inflate(TOUCH_RADIUS);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, bounds,
                living -> !living.getUUID().equals(casterId) && !damaged.contains(living.getUUID()))) {
            damaged.add(target.getUUID());
            target.hurt(damageSources().magic(), DAMAGE);
        }
    }

    private void breakAt(Vec3 pos) {
        BlockPos blockPos = BlockPos.containing(pos);
        var state = level().getBlockState(blockPos);
        if (!state.isAir() && state.getDestroySpeed(level(), blockPos) >= 0.0F) {
            level().destroyBlock(blockPos, false, this);
        }
    }

    private Vec3 forward() {
        Vec3 forward = new Vec3(entityData.get(DATA_FORWARD_X), entityData.get(DATA_FORWARD_Y), entityData.get(DATA_FORWARD_Z));
        return forward.lengthSqr() < 0.001D ? new Vec3(0.0D, 0.0D, 1.0D) : forward.normalize();
    }

    private void setForward(Vec3 forward) {
        Vec3 normalized = forward.lengthSqr() < 0.001D ? new Vec3(0.0D, 0.0D, 1.0D) : forward.normalize();
        entityData.set(DATA_FORWARD_X, (float) normalized.x);
        entityData.set(DATA_FORWARD_Y, (float) normalized.y);
        entityData.set(DATA_FORWARD_Z, (float) normalized.z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_FORWARD_X, 0.0F);
        builder.define(DATA_FORWARD_Y, 0.0F);
        builder.define(DATA_FORWARD_Z, 1.0F);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
