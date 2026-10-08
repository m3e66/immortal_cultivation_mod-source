package com.example.immortal_cultivation_mod.flame;

import com.example.immortal_cultivation_mod.entity.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A persistent, stationary flame essence. Combat cannot destroy or move it. */
public final class SpiritFlameEntity extends Entity {
    private static final EntityDataAccessor<String> KIND = SynchedEntityData.defineId(
            SpiritFlameEntity.class, EntityDataSerializers.STRING);

    public SpiritFlameEntity(EntityType<? extends SpiritFlameEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    public FlameKind kind() { return FlameKind.fromId(entityData.get(KIND)); }
    public void setKind(FlameKind kind) { entityData.set(KIND, kind.id()); }

    public static SpiritFlameEntity spawn(ServerLevel level, Vec3 position, FlameKind kind) {
        if (kind == null || !kind.special() || !Double.isFinite(position.x)
                || !Double.isFinite(position.y) || !Double.isFinite(position.z)
                || !level.isInWorldBounds(net.minecraft.core.BlockPos.containing(position))
                || !level.getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(position))) return null;
        var flame = new SpiritFlameEntity(ModEntities.SPIRIT_FLAME.get(), level);
        flame.setKind(kind);
        flame.setPos(position);
        return level.addFreshEntity(flame) ? flame : null;
    }

    @Override public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        clearFire();
    }

    @Override public InteractionResult interact(Player player, InteractionHand hand) {
        if (!(player.getItemInHand(hand).getItem() instanceof FlameVesselItem)) return InteractionResult.PASS;
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        return player instanceof net.minecraft.server.level.ServerPlayer server
                && FlameVesselItem.capture(server, player.getItemInHand(hand), this)
                ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public boolean isInvulnerableTo(DamageSource source) { return true; }
    @Override public boolean isAttackable() { return false; }
    @Override public boolean isPickable() { return true; }
    @Override public boolean isPushable() { return false; }
    @Override public void push(double x, double y, double z) { }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(KIND, FlameKind.CRIMSON.id()); }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { tag.putString("FlameKind", kind().id()); }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        FlameKind saved = FlameKind.fromId(tag.getString("FlameKind"));
        setKind(saved.special() ? saved : FlameKind.CRIMSON);
    }
}
