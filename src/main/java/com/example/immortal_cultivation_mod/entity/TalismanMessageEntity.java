package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.magicritual.center.ForcefieldInterceptable;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Real two-stage courier used by 传音符 and 千里传讯符. */
public final class TalismanMessageEntity extends ThrowableItemProjectile
        implements ForcefieldInterceptable {
    private static final EntityDataAccessor<Optional<UUID>> DATA_TARGET =
            SynchedEntityData.defineId(
                    TalismanMessageEntity.class,
                    EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> DATA_LONG_RANGE =
            SynchedEntityData.defineId(
                    TalismanMessageEntity.class, EntityDataSerializers.BOOLEAN);
    private static final double VANISH_DISTANCE = 20.0D;
    private static final double FLIGHT_SPEED = 1.55D;

    private Vec3 origin = Vec3.ZERO;
    private Vec3 outwardEnd = Vec3.ZERO;
    private int phase;
    private int phaseTicks;
    private String message = "";
    private String senderName = "";

    public TalismanMessageEntity(
            EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noCulling = true;
    }

    private TalismanMessageEntity(Level level, LivingEntity owner) {
        super(ModEntities.TALISMAN_MESSAGE.get(), owner, level);
        setNoGravity(true);
        noCulling = true;
    }

    public static boolean spawn(
            ServerLevel level, ServerPlayer sender, ServerPlayer target,
            String message, boolean longRange) {
        TalismanMessageEntity courier =
                new TalismanMessageEntity(level, sender);
        Vec3 look = sender.getLookAngle().normalize();
        Vec3 start = sender.getEyePosition().add(look.scale(0.65D));
        courier.setPos(start.x, start.y, start.z);
        courier.origin = start;
        courier.outwardEnd = start.add(look.scale(VANISH_DISTANCE));
        courier.entityData.set(DATA_TARGET, Optional.of(target.getUUID()));
        courier.entityData.set(DATA_LONG_RANGE, longRange);
        courier.message = message;
        courier.senderName = sender.getGameProfile().getName();
        courier.setDeltaMovement(look.scale(FLIGHT_SPEED));
        return level.addFreshEntity(courier);
    }

    @Override
    protected Item getDefaultItem() {
        return longRange()
                ? ModItems.QIANLI_CHUANXUN_TALISMAN.get()
                : ModItems.CHUANYIN_TALISMAN.get();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TARGET, Optional.empty());
        builder.define(DATA_LONG_RANGE, false);
    }

    @Override
    public void tick() {
        setNoGravity(true);
        if (!level().isClientSide) {
            phaseTicks++;
            if (phase == 0
                    && (position().distanceToSqr(origin)
                            >= VANISH_DISTANCE * VANISH_DISTANCE
                    || phaseTicks >= 30)) {
                reappearNearTarget();
            } else if (phase == 1) {
                guideToRecipient();
            }
            if (tickCount > 160) {
                fail("传音符未能抵达目标。");
                return;
            }
        }
        super.tick();
        if (level().isClientSide && (tickCount & 1) == 0) {
            level().addParticle(ParticleTypes.END_ROD,
                    getX(), getY(), getZ(),
                    -getDeltaMovement().x * 0.035D,
                    -getDeltaMovement().y * 0.035D,
                    -getDeltaMovement().z * 0.035D);
        }
    }

    private void reappearNearTarget() {
        ServerLevel level = (ServerLevel) level();
        ServerPlayer target = target(level);
        if (target == null || !target.isAlive()) {
            fail("目标已不在此界。");
            return;
        }
        level.sendParticles(ParticleTypes.END_ROD,
                getX(), getY(), getZ(),
                12, 0.18D, 0.18D, 0.18D, 0.025D);
        Vec3 targetCenter = target.position().add(
                0.0D, target.getBbHeight() * 0.55D, 0.0D);
        Vec3 approach = origin.subtract(targetCenter);
        if (approach.lengthSqr() < 0.01D) {
            approach = target.getLookAngle().scale(-1.0D);
        }
        Vec3 entry = targetCenter.add(
                approach.normalize().scale(VANISH_DISTANCE));
        setPos(entry.x, entry.y, entry.z);
        setDeltaMovement(targetCenter.subtract(entry)
                .normalize().scale(FLIGHT_SPEED));
        phase = 1;
        phaseTicks = 0;
        level.sendParticles(ParticleTypes.END_ROD,
                entry.x, entry.y, entry.z,
                12, 0.18D, 0.18D, 0.18D, 0.025D);
    }

    private void guideToRecipient() {
        ServerLevel level = (ServerLevel) level();
        ServerPlayer target = target(level);
        if (target == null || !target.isAlive()) {
            fail("目标已不在此界。");
            return;
        }
        Vec3 targetCenter = target.position().add(
                0.0D, target.getBbHeight() * 0.55D, 0.0D);
        Vec3 delta = targetCenter.subtract(position());
        if (delta.lengthSqr() <= 1.45D * 1.45D) {
            deliver(target);
            return;
        }
        setDeltaMovement(delta.normalize().scale(FLIGHT_SPEED));
    }

    private void deliver(ServerPlayer target) {
        target.sendSystemMessage(Component.literal(
                "[传音] " + senderName + "：" + message));
        if (getOwner() instanceof ServerPlayer sender) {
            sender.displayClientMessage(Component.literal(
                    "传音已送达 " + target.getGameProfile().getName()), true);
        }
        ((ServerLevel) level()).sendParticles(ParticleTypes.END_ROD,
                target.getX(),
                target.getY() + target.getBbHeight() * 0.55D,
                target.getZ(), 20, 0.38D, 0.55D, 0.38D, 0.025D);
        discard();
    }

    private void fail(String reason) {
        if (getOwner() instanceof ServerPlayer sender) {
            sender.displayClientMessage(Component.literal(reason), true);
        }
        discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!level().isClientSide
                && result.getEntity() == target((ServerLevel) level())) {
            deliver((ServerPlayer) result.getEntity());
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!level().isClientSide) {
            fail("传音符受到阻隔，未能送达。");
        }
    }

    @Nullable
    private ServerPlayer target(ServerLevel level) {
        return entityData.get(DATA_TARGET)
                .map(level.getServer().getPlayerList()::getPlayer)
                .orElse(null);
    }

    public boolean longRange() {
        return entityData == null ? false : entityData.get(DATA_LONG_RANGE);
    }

    @Override
    public Entity forcefieldOwner() {
        return getOwner();
    }

    @Override
    public Vec3 forcefieldMovement() {
        return getDeltaMovement();
    }

    @Override
    public float forcefieldBarrierDamage() {
        return 1.0F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        entityData.get(DATA_TARGET)
                .ifPresent(uuid -> tag.putUUID("Target", uuid));
        tag.putBoolean("LongRange", longRange());
        tag.putDouble("OriginX", origin.x);
        tag.putDouble("OriginY", origin.y);
        tag.putDouble("OriginZ", origin.z);
        tag.putDouble("EndX", outwardEnd.x);
        tag.putDouble("EndY", outwardEnd.y);
        tag.putDouble("EndZ", outwardEnd.z);
        tag.putInt("Phase", phase);
        tag.putInt("PhaseTicks", phaseTicks);
        tag.putString("Message", message);
        tag.putString("SenderName", senderName);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_TARGET, tag.hasUUID("Target")
                ? Optional.of(tag.getUUID("Target")) : Optional.empty());
        entityData.set(DATA_LONG_RANGE, tag.getBoolean("LongRange"));
        origin = new Vec3(tag.getDouble("OriginX"),
                tag.getDouble("OriginY"), tag.getDouble("OriginZ"));
        outwardEnd = new Vec3(tag.getDouble("EndX"),
                tag.getDouble("EndY"), tag.getDouble("EndZ"));
        phase = tag.getInt("Phase");
        phaseTicks = tag.getInt("PhaseTicks");
        message = tag.getString("Message");
        senderName = tag.getString("SenderName");
        setNoGravity(true);
        noCulling = true;
    }
}
