package com.example.immortal_cultivation_mod.entity;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Calm, warm-white light created by 微光符. */
public final class SpiritualLightEntity extends Entity {
    public static final int MIN_LIFETIME = 2 * 60 * 20;
    public static final int MAX_LIFETIME = 3 * 60 * 20;
    public static final int LIGHT_LEVEL = 12;

    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID =
            SynchedEntityData.defineId(
                    SpiritualLightEntity.class,
                    EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> DATA_ANCHORED =
            SynchedEntityData.defineId(
                    SpiritualLightEntity.class,
                    EntityDataSerializers.BOOLEAN);

    @Nullable
    private BlockPos anchorPos;
    @Nullable
    private BlockPos lightBlockPos;
    private int ageTicks;
    private int lifetimeTicks = MAX_LIFETIME;

    public SpiritualLightEntity(
            EntityType<? extends SpiritualLightEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
        noCulling = true;
    }

    public static void spawn(
            ServerLevel level, ServerPlayer owner,
            @Nullable BlockPos anchorPos) {
        SpiritualLightEntity light =
                ModEntities.SPIRITUAL_LIGHT.get().create(level);
        if (light == null) return;
        boolean anchored = anchorPos != null;
        light.entityData.set(DATA_OWNER_UUID, Optional.of(owner.getUUID()));
        light.entityData.set(DATA_ANCHORED, anchored);
        light.anchorPos = anchored ? anchorPos.immutable() : null;
        light.lifetimeTicks = Mth.nextInt(
                level.random, MIN_LIFETIME, MAX_LIFETIME);

        Vec3 initial = anchored
                ? Vec3.atCenterOf(anchorPos)
                : followTarget(owner, level.getGameTime());
        light.setPos(initial.x, initial.y, initial.z);
        level.addFreshEntity(light);
        level.sendParticles(ParticleTypes.END_ROD,
                initial.x, initial.y, initial.z,
                10, 0.12D, 0.12D, 0.12D, 0.018D);
        level.playSound(null, BlockPos.containing(initial),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                0.42F, 1.65F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER_UUID, Optional.empty());
        builder.define(DATA_ANCHORED, false);
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        noPhysics = true;
        if (level().isClientSide) return;

        ServerLevel serverLevel = (ServerLevel) level();
        ageTicks++;
        if (ageTicks >= lifetimeTicks) {
            extinguish(serverLevel);
            return;
        }

        Player owner = owner();
        if (anchored()) {
            if (anchorPos == null) {
                extinguish(serverLevel);
                return;
            }
            Vec3 anchor = Vec3.atCenterOf(anchorPos);
            setPos(anchor.x, anchor.y, anchor.z);
        } else {
            if (owner == null || !owner.isAlive() || owner.isRemoved()) {
                extinguish(serverLevel);
                return;
            }
            Vec3 target = followTarget(owner, serverLevel.getGameTime());
            Vec3 current = position();
            Vec3 next = current.distanceToSqr(target) > 36.0D
                    ? target : current.lerp(target, 0.14D);
            setPos(next.x, next.y, next.z);
        }

        if (serverLevel.getFluidState(blockPosition()).is(FluidTags.WATER)
                || (!anchored() && owner != null && owner.isInWaterOrBubble())) {
            extinguish(serverLevel);
            return;
        }

        updateLightBlock(serverLevel);
        if (tickCount % 8 == 0) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double radius = 0.18D + random.nextDouble() * 0.13D;
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    getX() + Math.cos(angle) * radius,
                    getY() + (random.nextDouble() - 0.5D) * 0.22D,
                    getZ() + Math.sin(angle) * radius,
                    1, 0.0D, 0.0D, 0.0D, 0.002D);
        }
    }

    private void updateLightBlock(ServerLevel level) {
        BlockPos desired = findAirPosition(level, blockPosition());
        if (desired == null || desired.equals(lightBlockPos)) return;
        clearLightBlock(level);
        if (level.getBlockState(desired).isAir()) {
            level.setBlock(desired,
                    Blocks.LIGHT.defaultBlockState()
                            .setValue(LightBlock.LEVEL, LIGHT_LEVEL),
                    Block.UPDATE_CLIENTS);
            lightBlockPos = desired.immutable();
        }
    }

    @Nullable
    private static BlockPos findAirPosition(ServerLevel level, BlockPos origin) {
        if (level.getBlockState(origin).isAir()
                || level.getBlockState(origin).is(Blocks.LIGHT)) {
            return origin;
        }
        Direction[] directions = {
                Direction.UP, Direction.NORTH, Direction.SOUTH,
                Direction.EAST, Direction.WEST, Direction.DOWN
        };
        for (Direction direction : directions) {
            BlockPos candidate = origin.relative(direction);
            if (level.getBlockState(candidate).isAir()
                    || level.getBlockState(candidate).is(Blocks.LIGHT)) {
                return candidate;
            }
        }
        return null;
    }

    private void clearLightBlock(ServerLevel level) {
        if (lightBlockPos == null
                || !level.getBlockState(lightBlockPos).is(Blocks.LIGHT)) {
            lightBlockPos = null;
            return;
        }
        AABB search = new AABB(lightBlockPos).inflate(0.25D);
        boolean shared = !level.getEntitiesOfClass(
                SpiritualLightEntity.class, search,
                other -> other != this
                        && lightBlockPos.equals(other.lightBlockPos)
                        && !other.isRemoved()).isEmpty();
        if (!shared) {
            level.removeBlock(lightBlockPos, false);
        }
        lightBlockPos = null;
    }

    private void extinguish(ServerLevel level) {
        clearLightBlock(level);
        level.sendParticles(ParticleTypes.SMOKE,
                getX(), getY(), getZ(),
                5, 0.08D, 0.08D, 0.08D, 0.008D);
        level.playSound(null, blockPosition(),
                SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS,
                0.35F, 1.55F);
        discard();
    }

    private static Vec3 followTarget(Player owner, long gameTime) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, owner.getYRot());
        forward = new Vec3(forward.x, 0.0D, forward.z).normalize();
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        double bob = Mth.sin((gameTime + owner.getId() * 5L) * 0.09F)
                * 0.075D;
        return owner.position()
                .add(right.scale(0.58D))
                .add(forward.scale(-0.16D))
                .add(0.0D, owner.getBbHeight() * 0.82D + bob, 0.0D);
    }

    public boolean anchored() {
        return entityData.get(DATA_ANCHORED);
    }

    public Optional<UUID> ownerUuid() {
        return entityData.get(DATA_OWNER_UUID);
    }

    @Nullable
    public Player owner() {
        return ownerUuid().map(level()::getPlayerByUUID).orElse(null);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(DATA_OWNER_UUID,
                tag.hasUUID("Owner")
                        ? Optional.of(tag.getUUID("Owner"))
                        : Optional.empty());
        entityData.set(DATA_ANCHORED, tag.getBoolean("Anchored"));
        anchorPos = tag.contains("AnchorPos")
                ? BlockPos.of(tag.getLong("AnchorPos")) : null;
        ageTicks = Math.max(0, tag.getInt("Age"));
        lifetimeTicks = Math.max(MIN_LIFETIME, tag.getInt("Lifetime"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        ownerUuid().ifPresent(uuid -> tag.putUUID("Owner", uuid));
        tag.putBoolean("Anchored", anchored());
        if (anchorPos != null) tag.putLong("AnchorPos", anchorPos.asLong());
        tag.putInt("Age", ageTicks);
        tag.putInt("Lifetime", lifetimeTicks);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && level() instanceof ServerLevel serverLevel) {
            clearLightBlock(serverLevel);
        }
        super.remove(reason);
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
