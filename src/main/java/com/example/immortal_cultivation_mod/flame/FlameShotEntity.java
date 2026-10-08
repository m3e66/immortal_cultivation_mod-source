package com.example.immortal_cultivation_mod.flame;

import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;

import com.example.immortal_cultivation_mod.entity.ModelBonePartEntity;
import com.example.immortal_cultivation_mod.entity.NpcProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;

/** A short-lived, owner-authoritative flare; its placed flame has the vessel's forge properties. */
public final class FlameShotEntity extends ThrowableItemProjectile {
    private static final EntityDataAccessor<String> KIND = SynchedEntityData.defineId(
            FlameShotEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> CHARGE = SynchedEntityData.defineId(
            FlameShotEntity.class, EntityDataSerializers.FLOAT);
    private boolean impacted;
    ChargedFlameImpact activeImpact;
    public FlameShotEntity(EntityType<? extends FlameShotEntity> type, Level level) { super(type, level); setNoGravity(true); }
    public FlameKind kind() { return FlameKind.fromId(entityData.get(KIND)); }
    public void setKind(FlameKind kind) { entityData.set(KIND, kind.id()); }
    public float charge() { return entityData.get(CHARGE); }
    public void setCharge(float value) { entityData.set(CHARGE, Float.isFinite(value) ? Math.clamp(value, 0, 1) : 0); }
    public float powerMultiplier() { return 1 + 2 * charge(); }
    public float blastRadius() { return destructionRadius(); }
    public float destructionRadius() { return charge() > 0 ? Math.max(1, kind().destructionRadius() * charge()) : 0; }
    @Override public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(powerMultiplier());
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (CHARGE.equals(key)) refreshDimensions();
    }
    @Override protected Item getDefaultItem() { return Items.BLAZE_POWDER; }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder); builder.define(KIND, FlameKind.CRIMSON.id()); builder.define(CHARGE, 0F);
    }
    @Override public void tick() {
        if (charge() > 0 && getDeltaMovement().lengthSqr() > .000001) {
            setDeltaMovement(getDeltaMovement().normalize().scale(ChargedFlameShape.flightSpeed(charge(), tickCount)));
            if (!level().isClientSide) hasImpulse = true;
        }
        super.tick();
        if (!level().isClientSide && (tickCount > 80 || !(getOwner() instanceof ServerPlayer player)
                || !player.isAlive() || player.isSpectator() || player.level() != level())) discard();
        if (level().isClientSide && charge() == 0 && tickCount % 2 == 0) level().addParticle(FlameParticles.type(kind()),
                getX(), getY(), getZ(), 0, .015, 0);
    }
    @Override protected boolean canHitEntity(Entity entity) {
        return canDamageTarget(entity) && super.canHitEntity(entity);
    }
    boolean canDamageTarget(Entity entity) {
        Entity target = ModelBonePartEntity.resolveParent(entity);
        if (!(getOwner() instanceof ServerPlayer owner) || !owner.isAlive() || owner.isSpectator()
                || owner.level() != level() || !(target instanceof LivingEntity living)
                || living == owner || !living.isAlive() || living.isInvulnerable() || living.isSpectator()
                || owner.isAlliedTo(living) || NpcProfile.areCombatAllies(owner, living)
                || !owner.canAttack(living)) return false;
        if (living instanceof Player player && (!owner.server.isPvpAllowed() || player.isCreative()
                || !owner.canHarmPlayer(player))) return false;
        if (living instanceof net.minecraft.world.entity.OwnableEntity owned
                && owner.getUUID().equals(owned.getOwnerUUID())) return false;
        return true;
    }
    @Override protected void onHitEntity(EntityHitResult hit) {
        if (!(level() instanceof ServerLevel server) || !(getOwner() instanceof ServerPlayer owner)) return;
        Entity target = ModelBonePartEntity.resolveParent(hit.getEntity());
        // Recheck after hit events: PvP, alliances, and invulnerability may have changed in flight.
        if (!canHitEntity(hit.getEntity()) || target.fireImmune()) return;
        if (target.hurt(server.damageSources().source(DamageTypes.FIREBALL, this, owner), kind().shotDamage() * powerMultiplier()))
            SpiritFlameBurnEffect.ignite((LivingEntity) target, kind());
    }
    @Override protected void onHitBlock(BlockHitResult hit) {
        placeFire(hit.getBlockPos().relative(hit.getDirection()), hit.getDirection());
    }
    public boolean placeFire(BlockPos pos, Direction face) {
        if (!(level() instanceof ServerLevel server) || !(getOwner() instanceof ServerPlayer player)
                || !player.isAlive() || player.isSpectator() || player.level() != server
                || !BlockBreakingSettings.enabled(player,
                BlockBreakingSettings.Category.FLAME_VESSELS)
                || !player.getAbilities().mayBuild || !kind().special() || !server.hasChunkAt(pos)
                || !server.isInWorldBounds(pos) || !server.getWorldBorder().isWithinBounds(pos)
                || !server.mayInteract(player, pos) || !player.mayUseItemAt(pos, face, new ItemStack(com.example.immortal_cultivation_mod.item.ModItems.FLAME_VESSEL.get()))
                || !server.getBlockState(pos).isAir() || !server.getFluidState(pos).isEmpty()) return false;
        var state = kind().block().defaultBlockState();
        if (!state.canSurvive(server, pos)) return false;
        var snapshot = BlockSnapshot.create(server.dimension(), server, pos);
        if (!server.setBlock(pos, state, Block.UPDATE_CLIENTS)) return false;
        if (EventHooks.onBlockPlace(player, snapshot, face)) { snapshot.restore(); return false; }
        server.updateNeighborsAt(pos, state.getBlock());
        return true;
    }
    @Override protected void onHit(HitResult hit) {
        if (impacted || isRemoved() || hit.getType() == HitResult.Type.MISS) return;
        impacted = true;
        if (charge() > 0) ChargedFlameImpact.explode(this, hit);
        else super.onHit(hit);
        if (level() instanceof ServerLevel server) {
            if (charge() == 0 || hit.getType() != HitResult.Type.BLOCK)
                server.sendParticles(FlameParticles.type(kind()), hit.getLocation().x, hit.getLocation().y,
                        hit.getLocation().z, 8 + (int) (charge() * 16), .15 * powerMultiplier(),
                        .15 * powerMultiplier(), .15 * powerMultiplier(), .02);
            discard();
        }
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putString("FlameKind", kind().id()); tag.putInt("FlameAge", tickCount); tag.putFloat("FlameCharge", charge()); }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); setKind(FlameKind.fromId(tag.getString("FlameKind"))); tickCount = Math.max(0, tag.getInt("FlameAge")); setCharge(tag.getFloat("FlameCharge"));
    }
}
