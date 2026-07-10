package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.effect.PhotonEffects;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.item.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class FlyingSwordEntity extends ThrowableItemProjectile {
    private static final int CONTROL_QI_COST_PER_SECOND = 3;
    private static final int SHOOT_QI_COST = 20;
    private static final float SHOOT_SPEED = 2.0F;
    private static final double OWNER_DROP_DISTANCE_SQR = 30.0D * 30.0D;
    private static final double BLADE_LENGTH = 2.7D;
    private static final double BLADE_HALF_WIDTH = 0.16D;
    private static final double LOOSE_SWORD_SPACING = 1.15D;
    private static final double LOOSE_SWORD_SPACING_SQR = LOOSE_SWORD_SPACING * LOOSE_SWORD_SPACING;
    private static final int LOOSE_SWORD_SEARCH_RINGS = 6;
    private static final double RIDE_SPEED = 1.35D;
    private static final Map<UUID, FlightSnapshot> RIDE_FLIGHT_GRANTS = new HashMap<>();

    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(FlyingSwordEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> CONTROLLED =
            SynchedEntityData.defineId(FlyingSwordEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DISABLED =
            SynchedEntityData.defineId(FlyingSwordEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SHOT =
            SynchedEntityData.defineId(FlyingSwordEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> STUCK =
            SynchedEntityData.defineId(FlyingSwordEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> WEAPON_DAMAGE =
            SynchedEntityData.defineId(FlyingSwordEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> SWORD_INDEX =
            SynchedEntityData.defineId(FlyingSwordEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TOTAL_CONTROLLED_SWORDS =
            SynchedEntityData.defineId(FlyingSwordEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> FORMATION_ANGLE =
            SynchedEntityData.defineId(FlyingSwordEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> STUCK_YAW =
            SynchedEntityData.defineId(FlyingSwordEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> STUCK_PITCH =
            SynchedEntityData.defineId(FlyingSwordEntity.class, EntityDataSerializers.FLOAT);

    private int qiTickCounter;
    private int swordHealth = 20;
    private int photonEffectAttempts;
    private int shotTick = -1;
    private ItemStack storedSwordStack = ItemStack.EMPTY;

    public FlyingSwordEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
        setNoGravity(false);
    }

    public FlyingSwordEntity(Level level, ServerPlayer owner, Vec3 pos) {
        super(ModEntities.FLYING_SWORD.get(), owner, level);
        setOwner(owner);
        setOwnerUuid(owner.getUUID());
        setPos(pos.x, pos.y, pos.z);
        setNoGravity(false);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER_UUID, Optional.empty());
        builder.define(CONTROLLED, false);
        builder.define(DISABLED, false);
        builder.define(SHOT, false);
        builder.define(STUCK, false);
        builder.define(WEAPON_DAMAGE, 10.0F);
        builder.define(SWORD_INDEX, 0);
        builder.define(TOTAL_CONTROLLED_SWORDS, 1);
        builder.define(FORMATION_ANGLE, 0.0F);
        builder.define(STUCK_YAW, 0.0F);
        builder.define(STUCK_PITCH, 0.0F);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.FLYING_SWORD.get();
    }

    public Optional<UUID> ownerUuid() {
        return entityData.get(OWNER_UUID);
    }

    public UUID getOwnerUuid() {
        return ownerUuid().orElse(null);
    }

    public void setOwnerUuid(UUID uuid) {
        entityData.set(OWNER_UUID, Optional.ofNullable(uuid));
    }

    public boolean isControlled() {
        return entityData.get(CONTROLLED);
    }

    public boolean isDisabled() {
        return entityData.get(DISABLED);
    }

    public boolean isShot() {
        return entityData.get(SHOT);
    }

    public boolean isStuck() {
        return entityData.get(STUCK);
    }

    public float getWeaponDamage() {
        return entityData.get(WEAPON_DAMAGE);
    }

    public void setWeaponDamage(float damage) {
        entityData.set(WEAPON_DAMAGE, Math.max(1.0F, damage));
    }

    public void setStoredSwordStack(ItemStack stack) {
        storedSwordStack = stack.isEmpty() ? new ItemStack(ModItems.FLYING_SWORD.get()) : stack.copyWithCount(1);
        setItem(storedSwordStack);
        setWeaponDamage(baseAttackDamage(storedSwordStack));
    }

    @Override
    public Component getName() {
        if (hasCustomName()) {
            return super.getName();
        }
        ItemStack stack = pickupSwordStack();
        return stack.isEmpty() ? super.getName() : stack.getHoverName();
    }

    private ItemStack pickupSwordStack() {
        if (!storedSwordStack.isEmpty()) {
            return storedSwordStack.copyWithCount(1);
        }
        ItemStack item = getItem();
        return item.isEmpty() ? new ItemStack(ModItems.FLYING_SWORD.get()) : item.copyWithCount(1);
    }

    public int getSwordIndex() {
        return entityData.get(SWORD_INDEX);
    }

    public int getTotalControlledSwords() {
        return entityData.get(TOTAL_CONTROLLED_SWORDS);
    }

    public float getFormationAngle() {
        return entityData.get(FORMATION_ANGLE);
    }

    public float getStuckYaw() {
        return entityData.get(STUCK_YAW);
    }

    public float getStuckPitch() {
        return entityData.get(STUCK_PITCH);
    }

    @Override
    public void tick() {
        if (isVehicle()) {
            tickRidingSword();
            return;
        }

        if (isControlled()) {
            if (level().isClientSide) {
                tickControlledClient();
            } else {
                tickControlled();
            }
            return;
        }

        if (isStuck()) {
            tickStuckSword();
            return;
        }

        if (!isShot()) {
            tickLooseSword();
            return;
        }

        tickShotSword();
    }

    private void tickStuckSword() {
        baseTick();
        noPhysics = true;
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
    }

    private void tickControlledClient() {
        baseTick();
        noPhysics = true;
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
    }

    private void tickControlled() {
        ServerPlayer owner = ownerPlayer();
        if (owner == null || !owner.isAlive()) {
            landAt(position());
            return;
        }
        if (tooFarFromOwner(owner)) {
            dropStraightDown();
            return;
        }

        qiTickCounter++;
        if (qiTickCounter >= 20) {
            qiTickCounter = 0;
            var data = ModAttachments.getData(owner);
            if (!ServerEvents.spendQiOrBlood(owner, data, CONTROL_QI_COST_PER_SECOND)) {
                landAt(position());
                ServerEvents.syncPlayerData(owner);
                return;
            }
            ServerEvents.syncPlayerData(owner);
        }

        List<FlyingSwordEntity> swords = controlledSwords(owner);
        int index = Math.max(0, swords.indexOf(this));
        int total = Math.max(1, swords.size());
        double angle = formationAngle(owner, index, total);
        Vec3 target = orbitPosition(owner, index, total);

        entityData.set(SWORD_INDEX, index);
        entityData.set(TOTAL_CONTROLLED_SWORDS, total);
        entityData.set(FORMATION_ANGLE, (float) Math.toDegrees(angle));
        noPhysics = true;
        setNoGravity(true);

        Vec3 toTarget = target.subtract(position());
        if (toTarget.lengthSqr() > 0.0025D) {
            Vec3 step = toTarget.scale(0.22D);
            if (step.length() > 0.85D) {
                step = step.normalize().scale(0.85D);
            }
            setDeltaMovement(step);
            setPos(getX() + step.x, getY() + step.y, getZ() + step.z);
        } else {
            setDeltaMovement(Vec3.ZERO);
            setPos(target.x, target.y, target.z);
        }

        setYRot(owner.getYRot());
        setXRot(0.0F);
    }

    private void tickLooseSword() {
        baseTick();
        noPhysics = false;
        setNoGravity(false);

        Vec3 movement = getDeltaMovement();
        if (!onGround()) {
            movement = movement.add(0.0D, -0.08D, 0.0D);
        }
        move(MoverType.SELF, movement);

        if (!level().isClientSide) {
            pushAwayFromLooseSwords();
        }

        if (onGround() || horizontalCollision || verticalCollision) {
            setDeltaMovement(Vec3.ZERO);
        } else {
            setDeltaMovement(movement.scale(0.98D));
        }
    }

    private void tickRidingSword() {
        baseTick();
        noPhysics = true;
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
        PhotonEffects.removeFlyingSwordProjectile(this);

        Entity passenger = getFirstPassenger();
        if (!(passenger instanceof ServerPlayer rider) || !rider.isAlive() || !rider.getUUID().equals(getOwnerUuid())) {
            if (!level().isClientSide) {
                ejectPassengers();
            }
            return;
        }
        if (tooFarFromOwner(rider)) {
            dropStraightDown();
            return;
        }

        entityData.set(CONTROLLED, true);
        entityData.set(SHOT, false);
        entityData.set(STUCK, false);
        enableRideFlight(rider);

        Vec3 look = rider.getLookAngle();
        Vec3 forward = horizontal(look);
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        Vec3 movement = forward.scale(rider.zza).add(right.scale(rider.xxa * 0.75D));
        if (Math.abs(rider.zza) > 0.01F) {
            movement = movement.add(0.0D, look.y * rider.zza, 0.0D);
        }
        if (movement.lengthSqr() > 0.0001D) {
            movement = movement.normalize().scale(RIDE_SPEED);
        }

        setYRot(rider.getYRot());
        setXRot(rider.getXRot());
        setDeltaMovement(movement);
        setPos(getX() + movement.x, getY() + movement.y, getZ() + movement.z);
        rider.fallDistance = 0.0F;
        rider.resetFallDistance();
    }

    private void pushAwayFromLooseSwords() {
        AABB search = getBoundingBox().inflate(1.2D);
        for (FlyingSwordEntity other : level().getEntitiesOfClass(FlyingSwordEntity.class, search)) {
            if (other == this || other.isControlled() || other.isShot()) {
                continue;
            }
            Vec3 away = position().subtract(other.position());
            away = new Vec3(away.x, 0.0D, away.z);
            if (away.lengthSqr() < 0.0001D) {
                double seed = (getId() * 37.0D) % 360.0D;
                away = new Vec3(Math.cos(Math.toRadians(seed)), 0.0D, Math.sin(Math.toRadians(seed)));
            }
            double distSqr = away.lengthSqr();
            if (distSqr < LOOSE_SWORD_SPACING_SQR) {
                double dist = Math.sqrt(Math.max(0.0001D, distSqr));
                Vec3 push = away.normalize().scale((LOOSE_SWORD_SPACING - dist) * 0.55D);
                setPos(getX() + push.x, getY(), getZ() + push.z);
            }
        }
    }

    public boolean canBeControlledBy(ServerPlayer player) {
        UUID owner = getOwnerUuid();
        return !isDisabled() && !isControlled() && owner != null && owner.equals(player.getUUID());
    }

    private void control(ServerPlayer owner) {
        if (!canBeControlledBy(owner)) {
            return;
        }
        setOwner(owner);
        setOwnerUuid(owner.getUUID());
        entityData.set(CONTROLLED, true);
        entityData.set(SHOT, false);
        entityData.set(STUCK, false);
        PhotonEffects.removeFlyingSwordProjectile(this);
        setNoGravity(true);
        noPhysics = true;
        setDeltaMovement(Vec3.ZERO);
        qiTickCounter = 0;
        photonEffectAttempts = 0;
    }

    private void fire(ServerPlayer owner) {
        if (!isControlled() || isDisabled() || !owner.getUUID().equals(getOwnerUuid())) {
            return;
        }
        shotTick = tickCount;
        Vec3 look = owner.getLookAngle().normalize();
        setOwner(owner);
        entityData.set(CONTROLLED, false);
        entityData.set(SHOT, true);
        entityData.set(STUCK, false);
        photonEffectAttempts = 0;
        setNoGravity(true);
        noPhysics = true;
        setPos(owner.getEyePosition().add(look.scale(BLADE_LENGTH * 0.55D)));
        setShotRotation(look);
        PhotonEffects.flyingSwordProjectile(this);
        photonEffectAttempts = 1;
        shoot(look.x, look.y, look.z, SHOOT_SPEED, 0.0F);
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        UUID owner = getOwnerUuid();
        if (!isShot()) {
            return false;
        }
        if (owner != null && entity.getUUID().equals(owner)) {
            return false;
        }
        if (entity instanceof FlyingSwordEntity otherSword && owner != null && owner.equals(otherSword.getOwnerUuid())) {
            return false;
        }
        return super.canHitEntity(entity);
    }

    @Override
    protected void onHit(HitResult result) {
        if (!isShot() || level().isClientSide) {
            return;
        }
        if (result instanceof EntityHitResult entityHit) {
            hitEntityAndStick(entityHit);
        } else if (result instanceof BlockHitResult blockHit) {
            hitBlockAndStick(blockHit);
        }
    }

    private void tickShotSword() {
        baseTick();
        noPhysics = true;
        setNoGravity(true);
        ServerPlayer owner = ownerPlayer();
        if (owner != null && tooFarFromOwner(owner)) {
            dropStraightDown();
            return;
        }
        if (!level().isClientSide && photonEffectAttempts < 600) {
            photonEffectAttempts++;
            PhotonEffects.flyingSwordProjectile(this);
        }

        Vec3 motion = getDeltaMovement();
        if (motion.lengthSqr() < 0.0001D) {
            landAt(position());
            return;
        }

        setShotRotation(motion.normalize());
        Vec3 start = position();
        Vec3 end = start.add(motion);

        HitResult blockHit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        EntityHitResult entityHit = findBladeEntityHit(start, end, motion);

        HitResult hit = blockHit;
        if (entityHit != null) {
            double entityDist = start.distanceToSqr(entityHit.getLocation());
            double blockDist = blockHit.getType() == HitResult.Type.MISS ? Double.MAX_VALUE : start.distanceToSqr(blockHit.getLocation());
            if (entityDist <= blockDist) {
                hit = entityHit;
            }
        }

        if (hit.getType() != HitResult.Type.MISS) {
            onHit(hit);
            return;
        }

        setPos(end.x, end.y, end.z);
        if (shotTick >= 0 && tickCount - shotTick > 100) {
            landAt(position());
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!isShot() || level().isClientSide) {
            return;
        }
        hitEntityAndStick(result);
    }

    private void hitEntityAndStick(EntityHitResult result) {
        Entity target = result.getEntity();
        UUID owner = getOwnerUuid();
        if (owner != null && owner.equals(target.getUUID())) {
            return;
        }
        if (target instanceof LivingEntity living) {
            living.invulnerableTime = 0;
        }
        Entity shooter = getOwner();
        DamageSource source = shooter instanceof Player player
                ? damageSources().playerAttack(player)
                : damageSources().thrown(this, shooter);
        target.hurt(source, damageAgainst(target));
        if (target instanceof LivingEntity living) {
            com.example.immortal_cultivation_mod.item.ForgingSystem.applyWeaponHitEffects(storedSwordStack, living);
        }
        stickAt(result.getLocation(), getDeltaMovement());
    }

    private EntityHitResult findBladeEntityHit(Vec3 start, Vec3 end, Vec3 motion) {
        Vec3 direction = motion.normalize();
        Vec3 tailStart = start.subtract(direction.scale(BLADE_LENGTH));
        Vec3 tailEnd = end.subtract(direction.scale(BLADE_LENGTH));
        AABB search = new AABB(tailStart, end).minmax(new AABB(tailEnd, start)).inflate(BLADE_HALF_WIDTH);
        return ProjectileUtil.getEntityHitResult(level(), this, tailStart, end, search, this::canHitEntity);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!isShot() || level().isClientSide) {
            return;
        }
        hitBlockAndStick(result);
    }

    private void hitBlockAndStick(BlockHitResult result) {
        Vec3 normal = Vec3.atLowerCornerOf(result.getDirection().getNormal()).scale(0.03D);
        stickAt(result.getLocation().subtract(getDeltaMovement().normalize().scale(0.02D)).add(normal), getDeltaMovement());
    }

    private void landAt(Vec3 pos) {
        PhotonEffects.removeFlyingSwordProjectile(this);
        entityData.set(CONTROLLED, false);
        entityData.set(SHOT, false);
        entityData.set(STUCK, false);
        photonEffectAttempts = 0;
        noPhysics = false;
        setNoGravity(false);
        setDeltaMovement(Vec3.ZERO);
        Vec3 openPos = level().isClientSide ? pos : findOpenLooseSwordPosition(pos);
        setPos(openPos.x, openPos.y + 0.02D, openPos.z);
    }

    private void dropStraightDown() {
        if (!level().isClientSide) {
            ejectPassengers();
        }
        PhotonEffects.removeFlyingSwordProjectile(this);
        entityData.set(CONTROLLED, false);
        entityData.set(SHOT, false);
        entityData.set(STUCK, false);
        photonEffectAttempts = 0;
        qiTickCounter = 0;
        noPhysics = false;
        setNoGravity(false);
        setDeltaMovement(0.0D, -0.85D, 0.0D);
        hurtMarked = true;
    }

    private boolean tooFarFromOwner(ServerPlayer owner) {
        return distanceToSqr(owner) > OWNER_DROP_DISTANCE_SQR;
    }

    private void stickAt(Vec3 pos, Vec3 shotMotion) {
        PhotonEffects.removeFlyingSwordProjectile(this);
        entityData.set(CONTROLLED, false);
        entityData.set(SHOT, false);
        entityData.set(STUCK, true);
        photonEffectAttempts = 0;
        noPhysics = true;
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
        if (shotMotion.lengthSqr() > 0.0001D) {
            setShotRotation(shotMotion.normalize());
        }
        setPos(pos.x, pos.y, pos.z);
    }

    private void setShotRotation(Vec3 direction) {
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float yaw = (float) (Math.atan2(-direction.x, direction.z) * 180.0D / Math.PI);
        float pitch = (float) (Math.atan2(direction.y, horizontal) * 180.0D / Math.PI);
        entityData.set(STUCK_YAW, yaw);
        entityData.set(STUCK_PITCH, pitch);
        setYRot(yaw);
        setXRot(pitch);
        yRotO = getYRot();
        xRotO = getXRot();
    }

    private Vec3 findOpenLooseSwordPosition(Vec3 origin) {
        if (isLooseSwordSpotOpen(origin)) {
            return origin;
        }
        for (int ring = 1; ring <= LOOSE_SWORD_SEARCH_RINGS; ring++) {
            double radius = LOOSE_SWORD_SPACING * ring;
            int samples = Math.max(8, ring * 8);
            for (int i = 0; i < samples; i++) {
                double angle = Math.PI * 2.0D * i / samples;
                Vec3 candidate = origin.add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
                if (isLooseSwordSpotOpen(candidate)) {
                    return candidate;
                }
            }
        }
        return origin;
    }

    private boolean isLooseSwordSpotOpen(Vec3 pos) {
        AABB spot = looseSwordSeparationBox(pos);
        if (!level().noCollision(this, spot)) {
            return false;
        }
        for (FlyingSwordEntity other : level().getEntitiesOfClass(FlyingSwordEntity.class, spot.inflate(0.03D))) {
            if (other != this && !other.isControlled() && !other.isShot()) {
                return false;
            }
        }
        return true;
    }

    private AABB looseSwordSeparationBox(Vec3 pos) {
        double half = LOOSE_SWORD_SPACING * 0.5D;
        return new AABB(pos.x - half, pos.y - 0.1D, pos.z - half, pos.x + half, pos.y + 0.5D, pos.z + half);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        UUID owner = getOwnerUuid();
        if (owner != null && !owner.equals(player.getUUID())) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer
                && player.isShiftKeyDown()
                && isControlled()
                && controlledSwords(serverPlayer).size() == 1
                && getPassengers().isEmpty()) {
            serverPlayer.startRiding(this, true);
            enableRideFlight(serverPlayer);
            return InteractionResult.CONSUME;
        }
        if (player.isShiftKeyDown()) {
            return InteractionResult.CONSUME;
        }
        ItemStack stack = pickupSwordStack();
        if (!player.getInventory().add(stack)) {
            level().addFreshEntity(new ItemEntity(level(), getX(), getY(), getZ(), stack));
        }
        if (player instanceof ServerPlayer serverPlayer) {
            disableRideFlight(serverPlayer);
        }
        PhotonEffects.removeFlyingSwordProjectile(this);
        discard();
        return InteractionResult.CONSUME;
    }

    @Override
    public void playerTouch(Player player) {
        // Right-click pickup only. Walking over the weapon entity must not collect it.
    }

    public static boolean interactLookingAt(ServerPlayer player, boolean ride) {
        FlyingSwordEntity sword = findInteractableSword(player);
        if (sword == null) {
            return false;
        }
        return sword.handlePlayerInteract(player, ride);
    }

    private static FlyingSwordEntity findInteractableSword(ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(8.0D));
        AABB search = player.getBoundingBox().expandTowards(player.getLookAngle().scale(8.0D)).inflate(1.25D);
        FlyingSwordEntity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (FlyingSwordEntity sword : player.level().getEntitiesOfClass(FlyingSwordEntity.class, search)) {
            if (!player.getUUID().equals(sword.getOwnerUuid())) {
                continue;
            }
            Optional<Vec3> clip = sword.visualInteractionBox().clip(start, end);
            if (clip.isEmpty()) {
                continue;
            }
            double dist = start.distanceToSqr(clip.get());
            if (dist < closestDist) {
                closestDist = dist;
                closest = sword;
            }
        }
        return closest;
    }

    private AABB visualInteractionBox() {
        Vec3 center = position();
        if (isShot() || isStuck()) {
            Vec3 direction = shotDirection();
            Vec3 tail = center.subtract(direction.scale(BLADE_LENGTH));
            return new AABB(tail, center).inflate(0.45D);
        }
        return getBoundingBox().inflate(1.1D, 1.1D, 1.1D);
    }

    private Vec3 shotDirection() {
        double yaw = Math.toRadians(getStuckYaw());
        double pitch = Math.toRadians(getStuckPitch());
        double horizontal = Math.cos(pitch);
        return new Vec3(-Math.sin(yaw) * horizontal, Math.sin(pitch), Math.cos(yaw) * horizontal).normalize();
    }

    private boolean handlePlayerInteract(ServerPlayer player, boolean ride) {
        if (!player.getUUID().equals(getOwnerUuid())) {
            return false;
        }
        if (ride && isControlled() && controlledSwords(player).size() == 1 && getPassengers().isEmpty()) {
            player.startRiding(this, true);
            enableRideFlight(player);
            return true;
        }
        if (ride) {
            return false;
        }
        ItemStack stack = pickupSwordStack();
        if (!player.getInventory().add(stack)) {
            level().addFreshEntity(new ItemEntity(level(), getX(), getY(), getZ(), stack));
        }
        disableRideFlight(player);
        PhotonEffects.removeFlyingSwordProjectile(this);
        discard();
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved()) {
            return true;
        }
        swordHealth -= Math.max(1, (int) Math.ceil(amount));
        if (swordHealth <= 0) {
            entityData.set(DISABLED, true);
            PhotonEffects.removeFlyingSwordProjectile(this);
            landAt(position());
        }
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID owner = getOwnerUuid();
        if (owner != null) {
            tag.putUUID("OwnerUuid", owner);
        }
        tag.putBoolean("Controlled", isControlled());
        tag.putBoolean("Disabled", isDisabled());
        tag.putBoolean("Shot", isShot());
        tag.putBoolean("Stuck", isStuck());
        tag.putInt("SwordHealth", swordHealth);
        tag.putFloat("WeaponDamage", getWeaponDamage());
        tag.putInt("SwordIndex", getSwordIndex());
        tag.putInt("TotalControlledSwords", getTotalControlledSwords());
        tag.putFloat("FormationAngle", getFormationAngle());
        tag.putFloat("StuckYaw", getStuckYaw());
        tag.putFloat("StuckPitch", getStuckPitch());
        if (!storedSwordStack.isEmpty()) {
            tag.put("SwordStack", storedSwordStack.saveOptional(registryAccess()));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("OwnerUuid")) {
            setOwnerUuid(tag.getUUID("OwnerUuid"));
        }
        entityData.set(CONTROLLED, tag.getBoolean("Controlled"));
        entityData.set(DISABLED, tag.getBoolean("Disabled"));
        entityData.set(SHOT, tag.getBoolean("Shot"));
        entityData.set(STUCK, tag.getBoolean("Stuck"));
        swordHealth = tag.contains("SwordHealth") ? tag.getInt("SwordHealth") : 20;
        entityData.set(WEAPON_DAMAGE, tag.contains("WeaponDamage") ? tag.getFloat("WeaponDamage") : 10.0F);
        entityData.set(SWORD_INDEX, tag.getInt("SwordIndex"));
        entityData.set(TOTAL_CONTROLLED_SWORDS, Math.max(1, tag.getInt("TotalControlledSwords")));
        entityData.set(FORMATION_ANGLE, tag.getFloat("FormationAngle"));
        entityData.set(STUCK_YAW, tag.getFloat("StuckYaw"));
        entityData.set(STUCK_PITCH, tag.getFloat("StuckPitch"));
        if (tag.contains("SwordStack")) {
            storedSwordStack = ItemStack.parseOptional(registryAccess(), tag.getCompound("SwordStack"));
        }
        if (storedSwordStack.isEmpty()) {
            storedSwordStack = new ItemStack(ModItems.FLYING_SWORD.get());
        }
        setItem(storedSwordStack);
    }

    public static boolean releaseFromHand(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        InteractionHand hand = InteractionHand.MAIN_HAND;
        if (!isFlyingWeapon(stack)) {
            stack = player.getOffhandItem();
            hand = InteractionHand.OFF_HAND;
        }
        if (!isFlyingWeapon(stack)) {
            return recallNearby(player, 30.0D) > 0;
        }

        Vec3 look = player.getLookAngle().normalize();
        Vec3 pos = player.getEyePosition().add(look.scale(0.9D));
        FlyingSwordEntity sword = new FlyingSwordEntity(player.level(), player, pos);
        sword.setStoredSwordStack(stack);
        sword.setDeltaMovement(look.scale(0.25D));
        player.level().addFreshEntity(sword);
        if (!player.getAbilities().instabuild) {
            player.getItemInHand(hand).shrink(1);
        }
        return true;
    }

    private static int recallNearby(ServerPlayer player, double radius) {
        int controlled = 0;
        int remaining = maxControlledSwords(player) - controlledSwords(player).size();
        if (remaining <= 0) {
            return 0;
        }
        List<FlyingSwordEntity> swords = new ArrayList<>(player.level().getEntitiesOfClass(
                FlyingSwordEntity.class,
                player.getBoundingBox().inflate(radius),
                sword -> sword.canBeControlledBy(player)
        ));
        swords.sort(Comparator.comparingDouble(sword -> sword.distanceToSqr(player)));
        for (FlyingSwordEntity sword : swords) {
            sword.control(player);
            controlled++;
            if (controlled >= remaining) {
                break;
            }
        }
        return controlled;
    }

    public static boolean controlLookedAt(ServerPlayer player) {
        if (controlledSwords(player).size() >= maxControlledSwords(player)) {
            return false;
        }
        if (isControlAllMode(player)) {
            return controlNearby(player) > 0;
        }

        HitResult hit = player.pick(8.0D, 0.0F, false);
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(8.0D));
        AABB search = player.getBoundingBox().expandTowards(player.getLookAngle().scale(8.0D)).inflate(1.0D);
        FlyingSwordEntity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (FlyingSwordEntity sword : player.level().getEntitiesOfClass(FlyingSwordEntity.class, search)) {
            if (!sword.canBeControlledBy(player)) {
                continue;
            }
            Optional<Vec3> clip = sword.getBoundingBox().inflate(0.5D).clip(start, end);
            if (clip.isEmpty()) {
                continue;
            }
            double dist = start.distanceToSqr(clip.get());
            if (dist < closestDist) {
                closestDist = dist;
                closest = sword;
            }
        }
        if (closest != null && (hit.getType() != HitResult.Type.BLOCK || closestDist <= start.distanceToSqr(hit.getLocation()) + 0.5D)) {
            closest.control(player);
            return true;
        }
        return false;
    }

    public static boolean isControlAllMode(ServerPlayer player) {
        return ModAttachments.getData(player).yuqiControlAllMode();
    }

    public static boolean toggleControlAllMode(ServerPlayer player) {
        boolean next = !isControlAllMode(player);
        setControlAllMode(player, next);
        return next;
    }

    public static void setControlAllMode(ServerPlayer player, boolean allMode) {
        ModAttachments.setData(player, ModAttachments.getData(player).withYuqiControlAllMode(allMode));
    }

    public static int controlNearby(ServerPlayer player) {
        int controlled = 0;
        int remaining = maxControlledSwords(player) - controlledSwords(player).size();
        if (remaining <= 0) {
            return 0;
        }
        for (FlyingSwordEntity sword : player.level().getEntitiesOfClass(FlyingSwordEntity.class, player.getBoundingBox().inflate(3.0D))) {
            if (sword.canBeControlledBy(player)) {
                sword.control(player);
                controlled++;
                if (controlled >= remaining) {
                    break;
                }
            }
        }
        return controlled;
    }

    public static boolean shootControlled(ServerPlayer player, boolean shootSix) {
        List<FlyingSwordEntity> swords = controlledSwords(player);
        if (swords.isEmpty()) {
            return false;
        }
        int count = shootSix ? Math.min(6, swords.size()) : 1;
        boolean fired = false;
        for (int i = 0; i < count; i++) {
            var data = ModAttachments.getData(player);
            if (!ServerEvents.spendQiOrBlood(player, data, SHOOT_QI_COST)) {
                break;
            }
            swords.get(i).fire(player);
            fired = true;
        }
        if (fired) {
            ServerEvents.syncPlayerData(player);
        }
        return fired;
    }

    private static List<FlyingSwordEntity> controlledSwords(ServerPlayer owner) {
        List<FlyingSwordEntity> swords = new ArrayList<>();
        if (!(owner.level() instanceof ServerLevel level)) {
            return swords;
        }
        for (FlyingSwordEntity sword : level.getEntitiesOfClass(FlyingSwordEntity.class, owner.getBoundingBox().inflate(16.0D))) {
            if (sword.isControlled() && !sword.isDisabled() && owner.getUUID().equals(sword.getOwnerUuid())) {
                swords.add(sword);
            }
        }
        swords.sort(Comparator.comparingInt(FlyingSwordEntity::getId));
        return swords;
    }

    private static int maxControlledSwords(ServerPlayer player) {
        int realm = CultivationLevels.getRealmIndex(ModAttachments.getData(player).cultivationLevel());
        if (realm == 2) {
            return 1;
        }
        if (realm == 3) {
            return 10;
        }
        if (realm > 3) {
            return Integer.MAX_VALUE;
        }
        return 0;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && passenger instanceof ServerPlayer player && player.getUUID().equals(getOwnerUuid());
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction callback) {
        callback.accept(passenger, getX(), getY() + 0.45D, getZ());
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (passenger instanceof ServerPlayer player) {
            disableRideFlight(player);
        }
    }

    private static void enableRideFlight(ServerPlayer player) {
        RIDE_FLIGHT_GRANTS.computeIfAbsent(player.getUUID(), id -> new FlightSnapshot(
                player.getAbilities().mayfly,
                player.getAbilities().flying,
                player.getAbilities().getFlyingSpeed()
        ));
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.getAbilities().setFlyingSpeed(0.22F);
        player.onUpdateAbilities();
    }

    private static void disableRideFlight(ServerPlayer player) {
        FlightSnapshot snapshot = RIDE_FLIGHT_GRANTS.remove(player.getUUID());
        if (snapshot == null) {
            return;
        }
        player.getAbilities().mayfly = player.isCreative() || player.isSpectator() || snapshot.mayfly();
        player.getAbilities().flying = (player.isCreative() || player.isSpectator() || snapshot.mayfly()) && snapshot.flying();
        player.getAbilities().setFlyingSpeed(snapshot.flyingSpeed());
        player.onUpdateAbilities();
    }

    private record FlightSnapshot(boolean mayfly, boolean flying, float flyingSpeed) {
    }

    private ServerPlayer ownerPlayer() {
        UUID owner = getOwnerUuid();
        if (owner == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.getServer().getPlayerList().getPlayer(owner);
    }

    private static Vec3 orbitPosition(ServerPlayer owner, int index, int count) {
        Vec3 look = horizontal(owner.getLookAngle());
        Vec3 back = look.scale(-1.0D);
        Vec3 right = new Vec3(0.0D, 1.0D, 0.0D).cross(back).normalize();
        Vec3 center = owner.position().add(0.0D, owner.getEyeHeight() * 0.82D, 0.0D).add(back.scale(1.55D));
        int wheelCount = Math.max(1, count);
        double radius = count <= 1 ? 0.0D : Math.min(1.75D, 1.10D + wheelCount * 0.045D);
        double angle = formationAngle(owner, index, wheelCount);
        return center.add(right.scale(Math.cos(angle) * radius)).add(0.0D, Math.sin(angle) * radius, 0.0D);
    }

    private static double formationAngle(ServerPlayer owner, int index, int count) {
        int wheelCount = Math.max(1, count);
        return Math.PI / 2.0D + owner.tickCount * 0.06D + Math.PI * 2.0D * index / wheelCount;
    }

    private static Vec3 horizontal(Vec3 vec) {
        Vec3 horizontal = new Vec3(vec.x, 0.0D, vec.z);
        return horizontal.lengthSqr() < 0.001D ? new Vec3(0.0D, 0.0D, 1.0D) : horizontal.normalize();
    }

    private static float baseAttackDamage(ItemStack stack) {
        double damage = 1.0D;
        boolean hasAttackDamage = false;
        ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (!entry.attribute().is(Attributes.ATTACK_DAMAGE)) {
                continue;
            }
            if (entry.slot() != EquipmentSlotGroup.ANY && entry.slot() != EquipmentSlotGroup.MAINHAND) {
                continue;
            }
            hasAttackDamage = true;
            AttributeModifier modifier = entry.modifier();
            damage = switch (modifier.operation()) {
                case ADD_VALUE -> damage + modifier.amount();
                case ADD_MULTIPLIED_BASE, ADD_MULTIPLIED_TOTAL -> damage * (1.0D + modifier.amount());
            };
        }
        return hasAttackDamage ? (float) Math.max(1.0D, damage) : 10.0F;
    }

    private float damageAgainst(Entity target) {
        float damage = getWeaponDamage();
        if (target instanceof LivingEntity living) {
            damage = com.example.immortal_cultivation_mod.item.ForgingSystem.adjustWeaponDamage(storedSwordStack, living, damage);
        }
        if (target instanceof LivingEntity living
                && com.example.immortal_cultivation_mod.spell.UndeadControl.isUndeadServantType(living)) {
            if (storedSwordStack.is(ModItems.TAOMU_SWORD.get())) {
                damage *= 1.5F;
            } else if (storedSwordStack.is(ModItems.COPPER_COIN_SWORD.get())) {
                damage *= 1.7F;
            }
        }
        return damage;
    }

    private static boolean isFlyingWeapon(ItemStack stack) {
        return stack.is(ModItems.FLYING_SWORD.get())
                || stack.is(ModItems.DAO.get())
                || stack.is(ModItems.TAOMU_SWORD.get())
                || stack.is(ModItems.COPPER_COIN_SWORD.get());
    }
}
