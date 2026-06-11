package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.effect.PhotonEffects;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class LightBeamProjectileEntity extends ThrowableItemProjectile {
    public static final int PROJECTILES_PER_STACK = 6;
    public static final int MAX_STACKS = 3;
    private static final int MAX_BLOCKS_BROKEN = 24;
    private static final EntityDataAccessor<Boolean> DATA_WAITING =
            SynchedEntityData.defineId(LightBeamProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_RING =
            SynchedEntityData.defineId(LightBeamProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SLOT =
            SynchedEntityData.defineId(LightBeamProjectileEntity.class, EntityDataSerializers.INT);

    private UUID casterId;
    private int photonEffectAttempts;
    private int flightTicks;
    private int blocksBroken;
    private Vec3 firedDirection = Vec3.ZERO;

    public LightBeamProjectileEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    public LightBeamProjectileEntity(Level level, LivingEntity caster, int ring, int slot) {
        super(ModEntities.LIGHT_BEAM_PROJECTILE.get(), caster, level);
        this.casterId = caster.getUUID();
        setRing(ring);
        setSlot(slot);
        setWaiting(true);
        setNoGravity(true);
        setPos(waitingPosition(caster));
    }

    @Override
    protected Item getDefaultItem() {
        return Items.AIR;
    }

    public boolean isWaiting() {
        return entityData.get(DATA_WAITING);
    }

    public UUID getCasterId() {
        return casterId;
    }

    public void fireInDirection(Vec3 direction) {
        setWaiting(false);
        firedDirection = direction.normalize();
        shoot(firedDirection.x, firedDirection.y, firedDirection.z, 2.4F, 0.0F);
        flightTicks = 0;
        blocksBroken = 0;
        photonEffectAttempts = 0;
        noPhysics = false;
        setNoGravity(true);
    }

    @Override
    public void tick() {
        setNoGravity(true);
        noPhysics = isWaiting();
        if (isWaiting()) {
            Entity owner = getOwner();
            if (!(owner instanceof LivingEntity caster) || !owner.isAlive()) {
                discard();
                return;
            }
            setDeltaMovement(Vec3.ZERO);
            Vec3 pos = waitingPosition(caster);
            setPos(pos);
        }

        super.tick();

        if (isWaiting() && getOwner() instanceof LivingEntity caster) {
            Vec3 pos = waitingPosition(caster);
            setPos(pos);
        }

        if (level().isClientSide) {
            addLightParticles();
        } else if (photonEffectAttempts < 20) {
            photonEffectAttempts++;
            PhotonEffects.lightBeamProjectile(this);
        }

        if (!isWaiting()) {
            flightTicks++;
        }

        if (!isWaiting() && flightTicks > 80) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return !entity.getUUID().equals(casterId) && super.canHitEntity(entity);
    }

    @Override
    protected void onHit(HitResult result) {
        if (isWaiting()) {
            return;
        }
        super.onHit(result);
        if (!level().isClientSide && result.getType() != HitResult.Type.BLOCK) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (isWaiting() || result.getEntity().getUUID().equals(casterId)) {
            return;
        }

        float damage = 50.0F;
        if (getOwner() instanceof net.minecraft.world.entity.player.Player player) {
            var data = ModAttachments.getData(player);
            damage += data.magicAttack();
            damage *= (float) SpiritRoots.damageMultiplier(data, ModSpells.get(ModSpells.LIGHT_BEAM_ATTACK));
        }
        result.getEntity().hurt(damageSources().thrown(this, getOwner()), damage);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (isWaiting() || level().isClientSide) {
            return;
        }

        blocksBroken += breakImpactBlocks(result.getBlockPos());
        if (blocksBroken >= MAX_BLOCKS_BROKEN) {
            discard();
            return;
        }
        Vec3 direction = firedDirection.lengthSqr() > 0.0D ? firedDirection : getDeltaMovement().normalize();
        shoot(direction.x, direction.y, direction.z, 2.4F, 0.0F);
        setPos(position().add(direction.scale(0.45D)));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_WAITING, true);
        builder.define(DATA_RING, 0);
        builder.define(DATA_SLOT, 0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (casterId != null) {
            tag.putUUID("caster", casterId);
        }
        tag.putInt("ring", getRing());
        tag.putInt("slot", getSlot());
        tag.putBoolean("waiting", isWaiting());
        tag.putInt("flightTicks", flightTicks);
        tag.putInt("blocksBroken", blocksBroken);
        tag.putDouble("firedX", firedDirection.x);
        tag.putDouble("firedY", firedDirection.y);
        tag.putDouble("firedZ", firedDirection.z);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("caster")) {
            casterId = tag.getUUID("caster");
        }
        setRing(tag.getInt("ring"));
        setSlot(tag.getInt("slot"));
        setWaiting(tag.getBoolean("waiting"));
        flightTicks = tag.getInt("flightTicks");
        blocksBroken = tag.getInt("blocksBroken");
        firedDirection = new Vec3(tag.getDouble("firedX"), tag.getDouble("firedY"), tag.getDouble("firedZ"));
    }

    private void setWaiting(boolean waiting) {
        entityData.set(DATA_WAITING, waiting);
    }

    private int getRing() {
        return entityData.get(DATA_RING);
    }

    private void setRing(int ring) {
        entityData.set(DATA_RING, ring);
    }

    private int getSlot() {
        return entityData.get(DATA_SLOT);
    }

    private void setSlot(int slot) {
        entityData.set(DATA_SLOT, slot);
    }

    private void addLightParticles() {
        Vec3 movement = getDeltaMovement();
        int count = isWaiting() ? 2 : 8;
        for (int i = 0; i < count; i++) {
            double spread = isWaiting() ? 0.06D : 0.12D;
            double back = isWaiting() ? 0.0D : i * 0.18D;
            level().addParticle(ParticleTypes.ELECTRIC_SPARK,
                    getX() - movement.x * back + (random.nextDouble() - 0.5D) * spread,
                    getY() - movement.y * back + (random.nextDouble() - 0.5D) * spread,
                    getZ() - movement.z * back + (random.nextDouble() - 0.5D) * spread,
                    movement.x * 0.02D,
                    movement.y * 0.02D,
                    movement.z * 0.02D);
        }
        level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
    }

    private int breakImpactBlocks(BlockPos center) {
        int broken = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
            if (blocksBroken + broken >= MAX_BLOCKS_BROKEN) {
                break;
            }
            BlockState state = level().getBlockState(pos);
            if (!state.isAir() && state.getDestroySpeed(level(), pos) >= 0.0F) {
                level().destroyBlock(pos, true, getOwner());
                broken++;
            }
        }
        return broken;
    }

    private Vec3 waitingPosition(LivingEntity caster) {
        Vec3 look = caster.getLookAngle().normalize();
        Vec3 back = new Vec3(-look.x, 0.0D, -look.z);
        if (back.lengthSqr() < 0.001D) {
            back = new Vec3(0.0D, 0.0D, 1.0D);
        }
        back = back.normalize();
        Vec3 right = new Vec3(0.0D, 1.0D, 0.0D).cross(back).normalize();
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        double radius = 0.9D + getRing() * 0.55D;
        double angle = Math.PI / 2.0D + Math.PI * 2.0D * getSlot() / PROJECTILES_PER_STACK;
        Vec3 center = caster.position().add(0.0D, caster.getEyeHeight() * 0.75D, 0.0D).add(back.scale(1.45D + getRing() * 0.2D));
        return center.add(right.scale(Math.cos(angle) * radius)).add(up.scale(Math.sin(angle) * radius));
    }
}
