package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.item.UtilityTalismanItem;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Short-lived visual paper for non-attack talismans. Papers share an evenly
 * spaced ring, face their owner, pause for 0.1 seconds, and then burn upward.
 */
public final class TalismanActivationEntity extends Entity {
    public static final int FLOAT_TICKS = 20;
    public static final int BURN_TICKS = 12;
    public static final int LIFETIME_TICKS = FLOAT_TICKS + BURN_TICKS;
    public static final float VISUAL_SIZE = 0.92F;

    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID =
            SynchedEntityData.defineId(
                    TalismanActivationEntity.class,
                    EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DATA_KIND =
            SynchedEntityData.defineId(
                    TalismanActivationEntity.class,
                    EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET_ENTITY_ID =
            SynchedEntityData.defineId(
                    TalismanActivationEntity.class,
                    EntityDataSerializers.INT);

    @Nullable
    private BlockPos targetPos;
    private boolean effectApplied;

    public TalismanActivationEntity(
            EntityType<? extends TalismanActivationEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
        noCulling = true;
    }

    public static void spawn(
            ServerLevel level, ServerPlayer owner,
            UtilityTalismanItem.Kind kind, @Nullable BlockPos targetPos) {
        spawn(level, owner, kind, targetPos, null);
    }

    public static void spawn(
            ServerLevel level, ServerPlayer owner,
            UtilityTalismanItem.Kind kind, @Nullable BlockPos targetPos,
            @Nullable LivingEntity targetEntity) {
        TalismanActivationEntity paper =
                ModEntities.TALISMAN_ACTIVATION.get().create(level);
        if (paper == null) {
            UtilityTalismanItem.applyActivatedEffect(
                    level, owner, kind, targetPos,
                    targetEntity == null ? -1 : targetEntity.getId());
            return;
        }
        paper.entityData.set(DATA_OWNER_UUID, Optional.of(owner.getUUID()));
        paper.entityData.set(DATA_KIND, kind.ordinal());
        paper.entityData.set(DATA_TARGET_ENTITY_ID,
                targetEntity == null ? -1 : targetEntity.getId());
        paper.targetPos = targetPos == null ? null : targetPos.immutable();
        Vec3 forward = horizontalForward(owner);
        Vec3 initial = owner.position().add(forward.scale(1.18D)).add(
                0.0D, owner.getBbHeight() * 0.58D, 0.0D);
        paper.setPos(initial.x, initial.y, initial.z);
        level.addFreshEntity(paper);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER_UUID, Optional.empty());
        builder.define(DATA_KIND, UtilityTalismanItem.Kind.CLEAN_DUST.ordinal());
        builder.define(DATA_TARGET_ENTITY_ID, -1);
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        noPhysics = true;

        Player owner = owner();
        if (owner == null || !owner.isAlive() || owner.isRemoved()) {
            if (!level().isClientSide) discard();
            return;
        }

        if (!level().isClientSide) {
            updateBalancedPosition(owner);
            ServerLevel serverLevel = (ServerLevel) level();
            if (kind() == UtilityTalismanItem.Kind.VOID_ESCAPE
                    && owner instanceof ServerPlayer serverPlayer) {
                com.example.immortal_cultivation_mod.item.VoidEscapeTalismanLogic
                        .emitPreparation(serverLevel, serverPlayer, tickCount);
            }
            int effectTick = kind() == UtilityTalismanItem.Kind.DIM_LIGHT
                    ? LIFETIME_TICKS - 1 : FLOAT_TICKS;
            if (!effectApplied && tickCount >= effectTick
                    && owner instanceof ServerPlayer serverPlayer) {
                effectApplied = true;
                UtilityTalismanItem.applyActivatedEffect(
                        serverLevel, serverPlayer, kind(), targetPos,
                        targetEntityId());
                serverLevel.playSound(null, blockPosition(),
                        SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS,
                        0.65F, 1.18F + random.nextFloat() * 0.14F);
            }
            if (tickCount >= FLOAT_TICKS) {
                emitBurningEdge(serverLevel, owner);
            }
            if (tickCount >= LIFETIME_TICKS) {
                serverLevel.sendParticles(ParticleTypes.WHITE_ASH,
                        getX(), getY() + VISUAL_SIZE * 0.46D, getZ(),
                        7, 0.22D, 0.08D, 0.05D, 0.018D);
                serverLevel.playSound(null, blockPosition(),
                        SoundEvents.CANDLE_EXTINGUISH, SoundSource.PLAYERS,
                        0.35F, 1.45F + random.nextFloat() * 0.15F);
                discard();
            }
        }
    }

    private void updateBalancedPosition(Player owner) {
        List<TalismanActivationEntity> papers = level().getEntitiesOfClass(
                TalismanActivationEntity.class,
                owner.getBoundingBox().inflate(4.0D),
                paper -> paper.ownerUuid()
                        .filter(owner.getUUID()::equals).isPresent());
        papers.sort(Comparator.comparingInt(Entity::getId));
        int index = papers.indexOf(this);
        if (index < 0) {
            index = 0;
            papers = List.of(this);
        }

        Vec3 forward = horizontalForward(owner);
        double baseAngle = Mth.atan2(forward.z, forward.x);
        int groupAge = papers.stream()
                .mapToInt(paper -> paper.tickCount)
                .max().orElse(tickCount);
        float orbitProgress = Mth.clamp(
                groupAge / (float) FLOAT_TICKS, 0.0F, 1.0F);
        float easedOrbit = orbitProgress * orbitProgress
                * (3.0F - 2.0F * orbitProgress);
        double angle = baseAngle
                + Mth.TWO_PI * index / Math.max(1, papers.size())
                + Mth.TWO_PI * easedOrbit;
        double radius = 1.18D;
        double bob = Mth.sin((level().getGameTime() + getId() * 3L)
                * 0.34F) * 0.045D;
        Vec3 position = owner.position().add(
                Math.cos(angle) * radius,
                owner.getBbHeight() * 0.58D + bob,
                Math.sin(angle) * radius);
        setPos(position.x, position.y, position.z);
    }

    private void emitBurningEdge(ServerLevel level, Player owner) {
        if (kind() == UtilityTalismanItem.Kind.VOID_ESCAPE) {
            emitVoidEscapeDissolve(level, owner);
            return;
        }
        float progress = burnProgress(0.0F);
        double edgeY = getY() - VISUAL_SIZE * 0.5D
                + VISUAL_SIZE * progress;

        Vec3 towardOwner = owner.getBoundingBox().getCenter()
                .subtract(position());
        towardOwner = new Vec3(towardOwner.x, 0.0D, towardOwner.z);
        if (towardOwner.lengthSqr() < 1.0E-5D) {
            towardOwner = horizontalForward(owner).scale(-1.0D);
        } else {
            towardOwner = towardOwner.normalize();
        }
        Vec3 acrossPaper = new Vec3(
                towardOwner.z, 0.0D, -towardOwner.x);

        // Trace the whole advancing edge instead of emitting one flame clump
        // at the paper's center. Slight height variation matches the renderer's
        // irregular char line and makes the sheet visibly burn bottom-up.
        int flamePoints = 5;
        for (int pointIndex = 0; pointIndex < flamePoints; pointIndex++) {
            double fraction = (pointIndex + 0.5D) / flamePoints - 0.5D;
            double sideways = fraction * VISUAL_SIZE * 0.90D;
            double jagged = Math.sin(getId() * 0.71D
                    + pointIndex * 1.83D + tickCount * 0.42D) * 0.024D;
            Vec3 point = position()
                    .add(acrossPaper.scale(sideways))
                    .add(towardOwner.scale(0.018D));
            double x = point.x;
            double y = edgeY + jagged;
            double z = point.z;
            level.sendParticles(ParticleTypes.SMALL_FLAME,
                    x, y, z, 1,
                    0.014D, 0.028D, 0.014D, 0.003D);
            if ((tickCount + pointIndex) % 3 == 0) {
                level.sendParticles(ParticleTypes.FLAME,
                        x, y + 0.025D, z, 1,
                        0.008D, 0.018D, 0.008D, 0.002D);
            }
        }

        if ((tickCount & 1) == 0) {
            double ashSide = (random.nextDouble() - 0.5D)
                    * VISUAL_SIZE * 0.82D;
            Vec3 ash = position().add(acrossPaper.scale(ashSide));
            level.sendParticles(
                    kind() == UtilityTalismanItem.Kind.DIM_LIGHT
                            || kind() == UtilityTalismanItem.Kind.VOID_ESCAPE
                            ? ParticleTypes.WHITE_ASH : ParticleTypes.ASH,
                    ash.x, edgeY + 0.055D, ash.z,
                    2, 0.045D, 0.035D, 0.025D, 0.012D);
            level.sendParticles(ParticleTypes.SMOKE,
                    ash.x, edgeY + 0.035D, ash.z,
                    1, 0.025D, 0.025D, 0.018D, 0.006D);
        }
    }

    /** 遁空符 collapses from all four edges into cyan-white spatial fragments. */
    private void emitVoidEscapeDissolve(ServerLevel level, Player owner) {
        double progress = burnProgress(0.0F);
        Vec3 towardOwner = owner.getBoundingBox().getCenter().subtract(position());
        towardOwner = new Vec3(towardOwner.x, 0.0D, towardOwner.z);
        if (towardOwner.lengthSqr() < 1.0E-5D) {
            towardOwner = horizontalForward(owner).scale(-1.0D);
        } else {
            towardOwner = towardOwner.normalize();
        }
        Vec3 acrossPaper = new Vec3(towardOwner.z, 0.0D, -towardOwner.x);
        double half = VISUAL_SIZE * 0.47D;
        double inward = half * (1.0D - progress);
        for (int sample = 0; sample < 4; sample++) {
            double along = (sample / 3.0D - 0.5D) * VISUAL_SIZE * 0.82D;
            for (int side : new int[] {-1, 1}) {
                Vec3 verticalEdge = position().add(acrossPaper.scale(side * inward));
                level.sendParticles(ParticleTypes.END_ROD,
                        verticalEdge.x, getY() + along, verticalEdge.z,
                        1, 0.01D, 0.018D, 0.01D, 0.002D);
                Vec3 horizontalEdge = position().add(acrossPaper.scale(along));
                level.sendParticles(ParticleTypes.WHITE_ASH,
                        horizontalEdge.x, getY() + side * inward, horizontalEdge.z,
                        1, 0.012D, 0.018D, 0.012D, 0.008D);
            }
        }
    }

    private static Vec3 horizontalForward(Player owner) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, owner.getYRot());
        return new Vec3(forward.x, 0.0D, forward.z).normalize();
    }

    public float burnProgress(float partialTick) {
        return Mth.clamp(
                (tickCount + partialTick - FLOAT_TICKS) / BURN_TICKS,
                0.0F, 1.0F);
    }

    public Optional<UUID> ownerUuid() {
        return entityData.get(DATA_OWNER_UUID);
    }

    @Nullable
    public Player owner() {
        return ownerUuid().map(level()::getPlayerByUUID).orElse(null);
    }

    public UtilityTalismanItem.Kind kind() {
        return UtilityTalismanItem.Kind.byId(entityData.get(DATA_KIND));
    }

    public int targetEntityId() {
        return entityData.get(DATA_TARGET_ENTITY_ID);
    }

    @Nullable
    public Entity targetEntity() {
        int id = targetEntityId();
        return id < 0 ? null : level().getEntity(id);
    }

    public boolean effectApplied() {
        return effectApplied;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // Transient visual entities are never saved.
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // Transient visual entities are never saved.
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
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
