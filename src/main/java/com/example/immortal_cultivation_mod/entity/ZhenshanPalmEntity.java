package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.magicritual.center.ForcefieldInterceptable;
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
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ZhenshanPalmEntity extends Entity implements GeoEntity, ForcefieldInterceptable {
    private static final EntityDataAccessor<Float> DATA_FORWARD_X =
            SynchedEntityData.defineId(ZhenshanPalmEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_FORWARD_Y =
            SynchedEntityData.defineId(ZhenshanPalmEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_FORWARD_Z =
            SynchedEntityData.defineId(ZhenshanPalmEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SIZE_SCALE =
            SynchedEntityData.defineId(ZhenshanPalmEntity.class, EntityDataSerializers.FLOAT);
    private static final int LIFETIME = 100;
    private static final float DAMAGE = 225.0F;
    private static final double PARTICLE_TOUCH_RADIUS = 0.34D;
    private static final int STARTUP_GRACE_TICKS = 3;
    public static final double HAND_WIDTH_SCALE = ZhenshanPalmShape.UNITS_TO_BLOCKS;
    public static final double HAND_HEIGHT_SCALE = ZhenshanPalmShape.UNITS_TO_BLOCKS;
    public static final double HAND_POINT_MIN_X = -10.0D;
    public static final double HAND_POINT_MAX_X = 20.0D;
    public static final double HAND_POINT_MIN_Y = -12.0D;
    public static final double HAND_POINT_MAX_Y = 44.0D;
    private static final int COLLISION_POINT_STRIDE = 1;
    private static final int MAX_BLOCKS_BROKEN_PER_TICK = 96;
    private static final int ACCENT_PARTICLE_INTERVAL = 3;
    private static final int FINGER_ACCENT_INTERVAL = 4;
    private static final int EARTH_BURSTS_PER_TICK = 1;

    private static final double[][] PALM_POINTS = ZhenshanPalmShape.collisionPoints();
    private static final double[][] FINGER_TIPS = {
            {7.85, 40}, {2.5, 44}, {-3.05, 40}, {-8.1, 34}, {17.75, 15}
    };

    private final Set<UUID> damagedMobs = new HashSet<>();
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private UUID casterId;

    public ZhenshanPalmEntity(EntityType<? extends ZhenshanPalmEntity> entityType, Level level) {
        super(entityType, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    public Entity forcefieldOwner() {
        return resolveCaster();
    }

    @Override
    public Vec3 forcefieldMovement() {
        return getDeltaMovement();
    }

    @Override
    public float forcefieldBarrierDamage() {
        LivingEntity owner = resolveCaster();
        return owner == null ? DAMAGE
                : com.example.immortal_cultivation_mod.spell.SpellDamageHelper.damage(
                        owner,
                        DAMAGE,
                        com.example.immortal_cultivation_mod.spell.ModSpells.get(
                                com.example.immortal_cultivation_mod.spell.ModSpells.ZHENSHAN_PALM));
    }

    @Override
    public void forcefieldBounce(Vec3 reflectedMovement) {
        if (reflectedMovement.lengthSqr() > 1.0E-9D) {
            setForward(reflectedMovement);
            updateRotationFromMovement(reflectedMovement);
        }
        setDeltaMovement(reflectedMovement);
        hurtMarked = true;
        hasImpulse = true;
    }

    @Override
    public double forcefieldSupport(Vec3 normal) {
        Vec3 forward = forward();
        Vec3 right = ZhenshanPalmShape.right(forward);
        Vec3 up = right.cross(forward).normalize();
        double scale = sizeScale();
        double rightReach = Math.max(Math.abs(HAND_POINT_MIN_X), Math.abs(HAND_POINT_MAX_X))
                * HAND_WIDTH_SCALE * scale + PARTICLE_TOUCH_RADIUS;
        double upReach = Math.max(Math.abs(HAND_POINT_MIN_Y), Math.abs(HAND_POINT_MAX_Y))
                * HAND_HEIGHT_SCALE * scale + PARTICLE_TOUCH_RADIUS;
        return Math.abs(normal.dot(forward)) * PARTICLE_TOUCH_RADIUS
                + Math.abs(normal.dot(right)) * rightReach
                + Math.abs(normal.dot(up)) * upReach;
    }

    @Override
    public double forcefieldMaximumSupport() {
        double scale = sizeScale();
        double rightReach = Math.max(Math.abs(HAND_POINT_MIN_X), Math.abs(HAND_POINT_MAX_X))
                * HAND_WIDTH_SCALE * scale + PARTICLE_TOUCH_RADIUS;
        double upReach = Math.max(Math.abs(HAND_POINT_MIN_Y), Math.abs(HAND_POINT_MAX_Y))
                * HAND_HEIGHT_SCALE * scale + PARTICLE_TOUCH_RADIUS;
        return Math.sqrt(PARTICLE_TOUCH_RADIUS * PARTICLE_TOUCH_RADIUS
                + rightReach * rightReach + upReach * upReach);
    }

    public ZhenshanPalmEntity(Level level, LivingEntity caster) {
        this(level, caster, 1.0F);
    }

    public ZhenshanPalmEntity(Level level, LivingEntity caster, float chargeScale) {
        this(ModEntities.ZHENSHAN_PALM.get(), level);
        this.casterId = caster.getUUID();
        NpcProfile.markNpcOwnedEffect(this, caster);
        setSizeScale(0.5F * Math.max(1.0F, Math.min(2.0F, chargeScale)));
        Vec3 look = caster.getLookAngle().normalize();
        Vec3 start = caster.position().add(0.0D, caster.getEyeHeight() - 0.25D, 0.0D).add(look.scale(2.6D));
        moveTo(start.x, start.y, start.z, caster.getYRot(), caster.getXRot());
        setDeltaMovement(look.scale(0.85D));
        setForward(look);
        updateRotationFromMovement(look);
    }

    @Override
    public void tick() {
        noPhysics = true;
        setNoGravity(true);
        if (!level().isClientSide) {
            LivingEntity caster = resolveCaster();
            NpcProfile.markNpcOwnedEffect(this, caster);
            if (NpcProfile.isOrphanedNpcEffect(this, caster)) {
                discard();
                return;
            }
        }
        super.tick();
        setPos(getX() + getDeltaMovement().x, getY() + getDeltaMovement().y, getZ() + getDeltaMovement().z);
        updateRotationFromMovement(forward());

        if (tickCount > LIFETIME) {
            discard();
            return;
        }

        Vec3 forward = forward();
        Vec3 right = ZhenshanPalmShape.right(forward);
        Vec3 up = right.cross(forward).normalize();

        if (level().isClientSide) {
            tickClientAccentVfx(right, up);
            return;
        }

        if (tickCount <= STARTUP_GRACE_TICKS) {
            return;
        }

        damageTouchedMobs(right, up);
        Set<BlockPos> touchedBlocksThisTick = new HashSet<>();
        for (int index = 0; index < PALM_POINTS.length; index += COLLISION_POINT_STRIDE) {
            BlockPos blockPos = BlockPos.containing(handPoint(PALM_POINTS[index], right, up));
            if (touchesBreakableBlock(blockPos)) {
                touchedBlocksThisTick.add(blockPos);
            }
        }
        for (double[] fingerTip : FINGER_TIPS) {
            BlockPos blockPos = BlockPos.containing(handPoint(fingerTip, right, up));
            if (touchesBreakableBlock(blockPos)) {
                touchedBlocksThisTick.add(blockPos);
            }
        }

        int broken = 0;
        int earthBursts = 0;
        for (BlockPos pos : touchedBlocksThisTick) {
            if (broken >= MAX_BLOCKS_BROKEN_PER_TICK) {
                break;
            }
            if (breakTouchedBlock(pos, earthBursts < EARTH_BURSTS_PER_TICK)) {
                broken++;
                earthBursts++;
            }
        }
    }

    /** The model carries the palm silhouette; these few motes only sell its earthen motion. */
    private void tickClientAccentVfx(Vec3 right, Vec3 up) {
        if (tickCount % ACCENT_PARTICLE_INTERVAL == 0) {
            int spacing = Math.max(1, PALM_POINTS.length / 3);
            for (int accent = 0; accent < 3; accent++) {
                int index = Math.floorMod(tickCount * 37 + accent * spacing, PALM_POINTS.length);
                Vec3 pos = handPoint(PALM_POINTS[index], right, up);
                level().addParticle(earthParticle(index), pos.x, pos.y, pos.z,
                        (random.nextDouble() - 0.5D) * 0.035D,
                        (random.nextDouble() - 0.5D) * 0.035D,
                        (random.nextDouble() - 0.5D) * 0.035D);
            }
        }
        if (tickCount % FINGER_ACCENT_INTERVAL == 0) {
            for (int index = 0; index < FINGER_TIPS.length; index++) {
                Vec3 pos = handPoint(FINGER_TIPS[index], right, up);
                level().addParticle(fingerParticle(index), pos.x, pos.y, pos.z,
                        (random.nextDouble() - 0.5D) * 0.025D,
                        0.015D,
                        (random.nextDouble() - 0.5D) * 0.025D);
            }
        }
    }

    private BlockParticleOption earthParticle(int index) {
        return switch (Math.floorMod(index + tickCount, 5)) {
            case 0 -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState());
            case 1 -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COARSE_DIRT.defaultBlockState());
            case 2 -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GRAVEL.defaultBlockState());
            case 3 -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState());
            default -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState());
        };
    }

    private BlockParticleOption fingerParticle(int index) {
        return switch (Math.floorMod(index + tickCount, 4)) {
            case 0 -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState());
            case 1 -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.RED_SAND.defaultBlockState());
            case 2 -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GRAVEL.defaultBlockState());
            default -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState());
        };
    }

    private Vec3 handPoint(double[] point, Vec3 right, Vec3 up) {
        double scale = sizeScale();
        return position().add(right.scale(point[0] * HAND_WIDTH_SCALE * scale)).add(up.scale(point[1] * HAND_HEIGHT_SCALE * scale));
    }

    private float sizeScale() {
        return Math.max(0.25F, entityData.get(DATA_SIZE_SCALE));
    }

    public float visualScale() {
        return sizeScale();
    }

    private void setSizeScale(float scale) {
        entityData.set(DATA_SIZE_SCALE, Math.max(0.25F, scale));
    }

    public Vec3 forward() {
        Vec3 forward = new Vec3(entityData.get(DATA_FORWARD_X), entityData.get(DATA_FORWARD_Y), entityData.get(DATA_FORWARD_Z));
        if (forward.lengthSqr() < 0.001D) {
            forward = getDeltaMovement();
        }
        if (forward.lengthSqr() < 0.001D) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        return forward.normalize();
    }

    private void setForward(Vec3 forward) {
        Vec3 normalized = forward.lengthSqr() < 0.001D ? new Vec3(0.0D, 0.0D, 1.0D) : forward.normalize();
        entityData.set(DATA_FORWARD_X, (float) normalized.x);
        entityData.set(DATA_FORWARD_Y, (float) normalized.y);
        entityData.set(DATA_FORWARD_Z, (float) normalized.z);
    }

    private void updateRotationFromMovement(Vec3 movement) {
        if (movement.lengthSqr() < 0.001D) {
            return;
        }

        Vec3 direction = movement.normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(direction.x, direction.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z)));
        setYRot(yaw);
        setXRot(pitch);
        yRotO = yaw;
        xRotO = pitch;
    }

    private void damageTouchedMobs(Vec3 right, Vec3 up) {
        LivingEntity owner = resolveCaster();
        NpcProfile.markNpcOwnedEffect(this, owner);
        if (NpcProfile.isOrphanedNpcEffect(this, owner)) {
            return;
        }
        LivingEntity resolvedOwner = owner;
        double searchRadius = forcefieldMaximumSupport() + 2.0D;
        AABB bounds = new AABB(position(), position()).inflate(searchRadius);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, bounds, living ->
                !living.getUUID().equals(casterId)
                        && !damagedMobs.contains(living.getUUID())
                        && (resolvedOwner == null
                        || (resolvedOwner instanceof CultivationNpc
                        ? NpcProfile.canNpcEffectHit(this, resolvedOwner, living)
                        : !NpcProfile.areCombatAllies(resolvedOwner, living))))) {
            AABB targetBounds = target.getBoundingBox().inflate(PARTICLE_TOUCH_RADIUS);
            boolean touched = false;
            for (int index = 0; index < PALM_POINTS.length; index += COLLISION_POINT_STRIDE) {
                if (targetBounds.contains(handPoint(PALM_POINTS[index], right, up))) {
                    touched = true;
                    break;
                }
            }
            if (!touched) {
                continue;
            }
            damagedMobs.add(target.getUUID());
            float damage = DAMAGE;

            if (owner != null) {
                damage = com.example.immortal_cultivation_mod.spell.SpellDamageHelper.damage(
                        owner,
                        DAMAGE,
                        com.example.immortal_cultivation_mod.spell.ModSpells.get(
                                com.example.immortal_cultivation_mod.spell.ModSpells.ZHENSHAN_PALM
                        )
                );
            }

            target.hurt(owner == null
                    ? damageSources().magic()
                    : owner.damageSources().indirectMagic(this, owner), damage);
            if (level() instanceof ServerLevel serverLevel) {
                SpellImpactParticles.earth(serverLevel, target.getBoundingBox().getCenter());
            }
        }
    }

    private LivingEntity resolveCaster() {
        if (casterId != null && level() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(casterId) instanceof LivingEntity livingOwner) {
            return livingOwner;
        }
        return null;
    }

    private boolean breakTouchedBlock(BlockPos pos, boolean showImpactParticles) {
        if (!touchesBreakableBlock(pos)) {
            return false;
        }
        if (showImpactParticles && level() instanceof ServerLevel serverLevel) {
            SpellImpactParticles.earth(serverLevel, Vec3.atCenterOf(pos));
        }
        return com.example.immortal_cultivation_mod.spell.SpellDestruction.breakNow(level(), pos, false, resolveCaster());
    }

    private boolean touchesBreakableBlock(BlockPos pos) {
        var state = level().getBlockState(pos);
        return !state.isAir() && state.getDestroySpeed(level(), pos) >= 0.0F;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_FORWARD_X, 0.0F);
        builder.define(DATA_FORWARD_Y, 0.0F);
        builder.define(DATA_FORWARD_Z, 1.0F);
        builder.define(DATA_SIZE_SCALE, 0.5F);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setSizeScale(tag.contains("SizeScale") ? tag.getFloat("SizeScale") : 0.5F);
        if (tag.hasUUID("Caster")) {
            casterId = tag.getUUID("Caster");
        }
        setForward(new Vec3(
                tag.getDouble("ForwardX"),
                tag.getDouble("ForwardY"),
                tag.getDouble("ForwardZ")));
        long[] damaged = tag.getLongArray("DamagedMobs");
        for (int index = 0; index + 1 < damaged.length; index += 2) {
            damagedMobs.add(new UUID(damaged[index], damaged[index + 1]));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("SizeScale", sizeScale());
        if (casterId != null) {
            tag.putUUID("Caster", casterId);
        }
        Vec3 forward = forward();
        tag.putDouble("ForwardX", forward.x);
        tag.putDouble("ForwardY", forward.y);
        tag.putDouble("ForwardZ", forward.z);
        long[] damaged = new long[damagedMobs.size() * 2];
        int index = 0;
        for (UUID uuid : damagedMobs) {
            damaged[index++] = uuid.getMostSignificantBits();
            damaged[index++] = uuid.getLeastSignificantBits();
        }
        tag.putLongArray("DamagedMobs", damaged);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
