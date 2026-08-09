package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.item.ForgingSystem;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.item.QiInfusedWeapon;
import com.example.immortal_cultivation_mod.item.ThrowingKnifeItem;
import com.example.immortal_cultivation_mod.item.WeaponRestrictionSystem;
import com.example.immortal_cultivation_mod.particle.MethodQiParticleEffects;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class ThrowingKnifeEntity extends ThrowableItemProjectile {
    private static final int BLOCKS_BEFORE_EXPLOSION = 5;
    private static final EntityDataAccessor<Boolean> HEAVY =
            SynchedEntityData.defineId(
                    ThrowingKnifeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> STUCK =
            SynchedEntityData.defineId(
                    ThrowingKnifeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> STUCK_YAW =
            SynchedEntityData.defineId(
                    ThrowingKnifeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> STUCK_PITCH =
            SynchedEntityData.defineId(
                    ThrowingKnifeEntity.class, EntityDataSerializers.FLOAT);

    private ItemStack storedKnifeStack = ItemStack.EMPTY;
    private int blocksBroken;
    private double launchQi;

    public ThrowingKnifeEntity(
            EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    public ThrowingKnifeEntity(
            Level level,
            ServerPlayer owner,
            ItemStack stack,
            boolean heavy) {
        super(ModEntities.THROWING_KNIFE.get(), owner, level);
        setOwner(owner);
        setStoredKnifeStack(stack);
        entityData.set(HEAVY, heavy);
        launchQi = QiInfusedWeapon.getQi(storedKnifeStack);
        Vec3 origin = owner.getEyePosition()
                .add(owner.getLookAngle().normalize().scale(0.72D))
                .add(0.0D, -0.12D, 0.0D);
        setPos(origin.x, origin.y, origin.z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HEAVY, false);
        builder.define(STUCK, false);
        builder.define(STUCK_YAW, 0.0F);
        builder.define(STUCK_PITCH, 0.0F);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.THROWING_KNIFE.get();
    }

    public boolean isHeavyThrow() {
        return entityData.get(HEAVY);
    }

    public boolean isStuck() {
        return entityData.get(STUCK);
    }

    public float getStuckYaw() {
        return entityData.get(STUCK_YAW);
    }

    public float getStuckPitch() {
        return entityData.get(STUCK_PITCH);
    }

    public void setStoredKnifeStack(ItemStack stack) {
        storedKnifeStack = stack.isEmpty()
                ? new ItemStack(ModItems.THROWING_KNIFE.get())
                : stack.copyWithCount(1);
        setItem(storedKnifeStack);
    }

    @Override
    public Component getName() {
        return hasCustomName() ? super.getName() : pickupStack().getHoverName();
    }

    @Override
    public void tick() {
        if (isStuck()) {
            setNoGravity(true);
            noPhysics = true;
            setDeltaMovement(Vec3.ZERO);
        }
        super.tick();
        if (isStuck()) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }

        updateFlightRotation(getDeltaMovement());
        if (level() instanceof ServerLevel serverLevel) {
            emitQiTrail(serverLevel);
        }
    }

    private void updateFlightRotation(Vec3 movement) {
        if (movement.lengthSqr() < 0.0001D) {
            return;
        }
        double horizontal = Math.sqrt(
                movement.x * movement.x + movement.z * movement.z);
        setYRot((float) (Mth.atan2(-movement.x, movement.z)
                * (180.0D / Math.PI)));
        setXRot((float) (Mth.atan2(movement.y, horizontal)
                * (180.0D / Math.PI)));
        yRotO = getYRot();
        xRotO = getXRot();
    }

    private void emitQiTrail(ServerLevel level) {
        List<SimpleParticleType> particles = getOwner() instanceof ServerPlayer player
                ? MethodQiParticleEffects.particlesForPlayer(player)
                : List.of(ModParticles.METHOD_QI_NEUTRAL.get());
        Vec3 movement = getDeltaMovement();
        Vec3 direction = movement.lengthSqr() < 0.0001D
                ? new Vec3(0.0D, 0.0D, 1.0D)
                : movement.normalize();
        for (int index = 0; index < 7; index++) {
            Vec3 point = position().subtract(direction.scale(index * 0.16D));
            SimpleParticleType particle =
                    particles.get((tickCount + index) % particles.size());
            level.sendParticles(
                    particle,
                    point.x,
                    point.y,
                    point.z,
                    1,
                    0.018D,
                    0.018D,
                    0.018D,
                    0.008D);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide || isStuck()) {
            return;
        }
        Entity target = ModelBonePartEntity.resolveParent(result.getEntity());
        if (target == getOwner()) {
            return;
        }

        float baseDamage = (float) Math.max(
                4.0D, QiInfusedWeapon.baseAttackDamage(storedKnifeStack));
        float injectedQiDamage =
                QiInfusedWeapon.injectedQiDamage(storedKnifeStack);
        float damage = baseDamage + injectedQiDamage;
        if (target instanceof LivingEntity living) {
            damage = ForgingSystem.adjustWeaponDamage(
                    storedKnifeStack, living, damage);
            damage = WeaponRestrictionSystem.adjustWeaponDamage(
                    storedKnifeStack, living, damage);
        }
        boolean damaged = target.hurt(
                damageSources().thrown(this, getOwner()),
                damage);
        if (damaged && injectedQiDamage > 0.0F) {
            QiInfusedWeapon.consumeInjectedQiDamage(
                    storedKnifeStack,
                    injectedQiDamage);
            setItem(storedKnifeStack);
        }
        if (target instanceof LivingEntity living) {
            ForgingSystem.applyWeaponHitEffects(storedKnifeStack, living);
            WeaponRestrictionSystem.applyWeaponHitEffects(storedKnifeStack, living);
        }
        stickAt(result.getLocation(), getDeltaMovement());
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (level().isClientSide || isStuck()) {
            return;
        }
        if (!isHeavyThrow()) {
            stickAt(result.getLocation(), getDeltaMovement());
            return;
        }

        BlockPos blockPos = result.getBlockPos();
        BlockState state = level().getBlockState(blockPos);
        Entity owner = getOwner();
        boolean canBreak = !state.isAir()
                && state.getDestroySpeed(level(), blockPos) >= 0.0F
                && (!(owner instanceof ServerPlayer player)
                || player.mayUseItemAt(
                        blockPos, result.getDirection(), storedKnifeStack));
        if (!canBreak || !level().destroyBlock(blockPos, false, owner)) {
            stickAt(result.getLocation(), getDeltaMovement());
            return;
        }

        blocksBroken++;
        if (blocksBroken >= BLOCKS_BEFORE_EXPLOSION) {
            explodeAndStick(result.getLocation(), getDeltaMovement());
        } else {
            Vec3 movement = getDeltaMovement();
            if (movement.lengthSqr() > 0.0001D) {
                setPos(result.getLocation().add(movement.normalize().scale(0.18D)));
            }
        }
    }

    private void explodeAndStick(Vec3 location, Vec3 movement) {
        stickAt(location, movement);
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        float radius = Mth.clamp(
                3.0F + (float) Math.sqrt(Math.max(0.0D, launchQi)) / 4.0F,
                3.0F,
                18.0F);
        serverLevel.sendParticles(
                ParticleTypes.EXPLOSION_EMITTER,
                location.x,
                location.y,
                location.z,
                1,
                0.0D,
                0.0D,
                0.0D,
                0.0D);
        Entity source = getOwner() == null ? this : getOwner();
        serverLevel.explode(
                source,
                location.x,
                location.y,
                location.z,
                radius,
                false,
                Level.ExplosionInteraction.BLOCK);
    }

    private void stickAt(Vec3 location, Vec3 movement) {
        updateFlightRotation(movement);
        entityData.set(STUCK, true);
        entityData.set(STUCK_YAW, getYRot());
        entityData.set(STUCK_PITCH, getXRot());
        noPhysics = true;
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
        setPos(location.x, location.y, location.z);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = pickupStack();
        ItemStack held = player.getItemInHand(hand);
        if (ThrowingKnifeItem.tryStoreKnife(
                held, stack, player.registryAccess())) {
            player.getInventory().setChanged();
            discard();
            return InteractionResult.CONSUME;
        }
        if (!player.getInventory().add(stack)) {
            level().addFreshEntity(new ItemEntity(
                    level(), getX(), getY(), getZ(), stack));
        }
        discard();
        return InteractionResult.CONSUME;
    }

    @Override
    public void playerTouch(Player player) {
        // Thrown knives are recovered by right-clicking, never by walking over them.
    }

    private ItemStack pickupStack() {
        if (!storedKnifeStack.isEmpty()) {
            return storedKnifeStack.copyWithCount(1);
        }
        ItemStack displayed = getItem();
        return displayed.isEmpty()
                ? new ItemStack(ModItems.THROWING_KNIFE.get())
                : displayed.copyWithCount(1);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Heavy", isHeavyThrow());
        tag.putBoolean("Stuck", isStuck());
        tag.putFloat("StuckYaw", getStuckYaw());
        tag.putFloat("StuckPitch", getStuckPitch());
        tag.putInt("BlocksBroken", blocksBroken);
        tag.putDouble("LaunchQi", launchQi);
        if (!storedKnifeStack.isEmpty()) {
            tag.put("KnifeStack",
                    storedKnifeStack.saveOptional(registryAccess()));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(HEAVY, tag.getBoolean("Heavy"));
        entityData.set(STUCK, tag.getBoolean("Stuck"));
        entityData.set(STUCK_YAW, tag.getFloat("StuckYaw"));
        entityData.set(STUCK_PITCH, tag.getFloat("StuckPitch"));
        blocksBroken = tag.getInt("BlocksBroken");
        launchQi = tag.getDouble("LaunchQi");
        if (tag.contains("KnifeStack")) {
            storedKnifeStack = ItemStack.parseOptional(
                    registryAccess(), tag.getCompound("KnifeStack"));
        }
        if (storedKnifeStack.isEmpty()) {
            storedKnifeStack = new ItemStack(ModItems.THROWING_KNIFE.get());
        }
        setItem(storedKnifeStack);
        if (isStuck()) {
            noPhysics = true;
            setNoGravity(true);
            setDeltaMovement(Vec3.ZERO);
        }
    }
}
