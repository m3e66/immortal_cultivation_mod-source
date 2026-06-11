package com.example.immortal_cultivation_mod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ZhenshanPalmEntity extends Entity {
    private static final int LIFETIME = 100;
    private static final float DAMAGE = 100.0F;
    private static final int PARTICLE_BLOCK_HIT_LIMIT = 3;
    private static final double HAND_WIDTH_SCALE = 3.15D;
    private static final double HAND_HEIGHT_SCALE = 2.55D;
    private static final double PARTICLE_TOUCH_RADIUS = 0.34D;
    private static final int STARTUP_GRACE_TICKS = 3;
    private static final double[][] PALM_POINTS = {
            {-0.72D, -0.65D}, {-0.36D, -0.75D}, {0.0D, -0.78D}, {0.36D, -0.72D}, {0.72D, -0.58D},
            {-0.9D, -0.34D}, {-0.54D, -0.38D}, {-0.18D, -0.42D}, {0.18D, -0.4D}, {0.54D, -0.34D}, {0.9D, -0.22D},
            {-0.94D, -0.02D}, {-0.58D, -0.04D}, {-0.22D, -0.06D}, {0.14D, -0.04D}, {0.5D, 0.0D}, {0.82D, 0.08D},
            {-0.78D, 0.3D}, {-0.42D, 0.28D}, {-0.08D, 0.26D}, {0.26D, 0.28D}, {0.58D, 0.32D},
            {-0.52D, 0.58D}, {-0.18D, 0.58D}, {0.16D, 0.6D}, {0.48D, 0.58D},
            {-0.48D, 0.88D}, {-0.18D, 0.9D}, {0.12D, 0.92D}, {0.42D, 0.86D},
            {-0.56D, 1.18D}, {-0.18D, 1.28D}, {0.18D, 1.24D}, {0.56D, 1.06D},
            {-0.58D, 1.48D}, {-0.18D, 1.62D}, {0.18D, 1.54D}, {0.58D, 1.28D},
            {1.0D, 0.28D}, {1.18D, 0.44D}, {1.36D, 0.58D}, {1.54D, 0.7D},
            {1.02D, 0.02D}, {1.25D, 0.16D}, {1.48D, 0.26D}
    };
    private static final double[][] FINGER_TIPS = {
            {-0.58D, 1.48D}, {-0.18D, 1.62D}, {0.18D, 1.54D}, {0.58D, 1.28D}, {1.54D, 0.7D}
    };
    private final Set<UUID> damagedMobs = new HashSet<>();
    private final int[] particleBlockHits = new int[PALM_POINTS.length];
    private UUID casterId;

    public ZhenshanPalmEntity(EntityType<? extends ZhenshanPalmEntity> entityType, Level level) {
        super(entityType, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public ZhenshanPalmEntity(Level level, LivingEntity caster) {
        this(ModEntities.ZHENSHAN_PALM.get(), level);
        this.casterId = caster.getUUID();
        Vec3 look = caster.getLookAngle().normalize();
        Vec3 start = caster.position().add(0.0D, caster.getEyeHeight() - 0.25D, 0.0D).add(look.scale(2.6D));
        moveTo(start.x, start.y, start.z, caster.getYRot(), caster.getXRot());
        setDeltaMovement(look.scale(0.85D));
    }

    @Override
    public void tick() {
        noPhysics = true;
        setNoGravity(true);
        super.tick();
        setPos(getX() + getDeltaMovement().x, getY() + getDeltaMovement().y, getZ() + getDeltaMovement().z);

        if (tickCount > LIFETIME) {
            discard();
            return;
        }

        Vec3 forward = getDeltaMovement().normalize();
        if (forward.lengthSqr() < 0.001D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = forward.cross(up).normalize();
        if (right.lengthSqr() < 0.001D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        }
        up = right.cross(forward).normalize();

        Set<BlockPos> touchedBlocksThisTick = new HashSet<>();
        int activeParticles = 0;
        for (int i = 0; i < PALM_POINTS.length; i++) {
            if (particleBlockHits[i] >= PARTICLE_BLOCK_HIT_LIMIT) {
                continue;
            }

            activeParticles++;
            double[] point = PALM_POINTS[i];
            Vec3 pos = handPoint(point, right, up);
            BlockPos blockPos = BlockPos.containing(pos);
            boolean touchesBlock = tickCount > STARTUP_GRACE_TICKS && touchesBreakableBlock(blockPos);
            if (level().isClientSide) {
                if (touchesBlock) {
                    particleBlockHits[i]++;
                    if (particleBlockHits[i] >= PARTICLE_BLOCK_HIT_LIMIT) {
                        continue;
                    }
                }
                level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()),
                        pos.x, pos.y, pos.z,
                        (random.nextDouble() - 0.5D) * 0.08D,
                        (random.nextDouble() - 0.5D) * 0.08D,
                        (random.nextDouble() - 0.5D) * 0.08D);
                if (random.nextFloat() < 0.25F) {
                    level().addParticle(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 0.0D, 0.02D, 0.0D);
                }
            } else {
                if (touchesBlock) {
                    touchedBlocksThisTick.add(blockPos);
                    particleBlockHits[i]++;
                }
                if (tickCount > STARTUP_GRACE_TICKS) {
                    damageTouchedMob(pos);
                }
            }
        }

        if (!level().isClientSide && !touchedBlocksThisTick.isEmpty()) {
            for (BlockPos pos : touchedBlocksThisTick) {
                breakTouchedBlock(pos);
            }
        }

        if (level().isClientSide) {
            for (double[] point : FINGER_TIPS) {
                if (isParticlePointDisabled(point)) {
                    continue;
                }
                Vec3 pos = handPoint(point, right, up);
                level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
                        pos.x, pos.y, pos.z,
                        (random.nextDouble() - 0.5D) * 0.04D,
                        0.02D,
                        (random.nextDouble() - 0.5D) * 0.04D);
            }
        } else if (activeParticles == 0) {
            discard();
        }
    }

    private boolean isParticlePointDisabled(double[] point) {
        for (int i = 0; i < PALM_POINTS.length; i++) {
            if (PALM_POINTS[i][0] == point[0] && PALM_POINTS[i][1] == point[1]) {
                return particleBlockHits[i] >= PARTICLE_BLOCK_HIT_LIMIT;
            }
        }
        return false;
    }

    private Vec3 handPoint(double[] point, Vec3 right, Vec3 up) {
        return position().add(right.scale(point[0] * HAND_WIDTH_SCALE)).add(up.scale(point[1] * HAND_HEIGHT_SCALE));
    }

    private boolean damageTouchedMob(Vec3 pos) {
        AABB bounds = new AABB(pos, pos).inflate(PARTICLE_TOUCH_RADIUS);
        for (Mob mob : level().getEntitiesOfClass(Mob.class, bounds, mob ->
                !mob.getUUID().equals(casterId) && !damagedMobs.contains(mob.getUUID()))) {
            damagedMobs.add(mob.getUUID());
            mob.hurt(damageSources().magic(), DAMAGE);
            return true;
        }
        return false;
    }

    private boolean breakTouchedBlock(BlockPos pos) {
        if (!touchesBreakableBlock(pos)) {
            return false;
        }
        level().destroyBlock(pos, true, this);
        return true;
    }

    private boolean touchesBreakableBlock(BlockPos pos) {
        var state = level().getBlockState(pos);
        return !state.isAir() && state.getDestroySpeed(level(), pos) >= 0.0F;
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
}
