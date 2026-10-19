package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.spell.JindanCreativeFlight;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.item.ManifestedWeaponItem;
import com.example.immortal_cultivation_mod.item.QiPouchStorage;
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
import java.util.List;
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
    public static final double FLIGHT_BLADE_OFFSET = 0.08D;
    public static final float FLIGHT_SPEED_MULTIPLIER = 1.5F;

    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(FlyingSwordEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> SWORD_FLIGHT =
            SynchedEntityData.defineId(FlyingSwordEntity.class, EntityDataSerializers.BOOLEAN);
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
    private int swordHealth = 1;
    private int swordMaxHealth = 1;
    private int shotTick = -1;
    private int pouchRecallTicks;
    private int autoRecallTicks;
    private boolean recoveryManaged;
    private ItemStack storedSwordStack = ItemStack.EMPTY;

    public FlyingSwordEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
        setNoGravity(false);
        resetSwordHealth(new ItemStack(ModItems.FLYING_SWORD.get()));
    }

    public FlyingSwordEntity(Level level, ServerPlayer owner, Vec3 pos) {
        super(ModEntities.FLYING_SWORD.get(), owner, level);
        setOwner(owner);
        setOwnerUuid(owner.getUUID());
        setPos(pos.x, pos.y, pos.z);
        setNoGravity(false);
        resetSwordHealth(new ItemStack(ModItems.FLYING_SWORD.get()));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER_UUID, Optional.empty());
        builder.define(CONTROLLED, false);
        builder.define(SWORD_FLIGHT, false);
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
        com.example.immortal_cultivation_mod.item.WeaponBinding.prepare(stack);
        storedSwordStack = stack.isEmpty() ? new ItemStack(ModItems.FLYING_SWORD.get()) : stack.copyWithCount(1);
        setItem(storedSwordStack);
        setWeaponDamage(baseAttackDamage(storedSwordStack));
        resetSwordHealth(storedSwordStack);
    }

    @Override public ItemStack getWeaponItem() { return storedSwordStack; }

    public int getSwordHealth() {
        return swordHealth;
    }

    public int getSwordMaxHealth() {
        return swordMaxHealth;
    }
    public boolean isRecoveryManaged() { return recoveryManaged; }
    public void setRecoveryManaged() { recoveryManaged=true; }

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

    /** Exact forged/component-bearing weapon stack returned by a linked 储物袋. */
    public ItemStack pouchRecallStack() {
        return pickupSwordStack();
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
        if(!level().isClientSide) {
            if(!FlyingSwordRecovery.valid(this)){discard();return;}
            if(isControlled()&&!recoveryManaged)FlyingSwordRecovery.track(this);
        }
        // Migrate legacy vehicle swords: the player now flies using vanilla abilities.
        if (isVehicle() && !level().isClientSide) {
            var passengers = List.copyOf(getPassengers());
            ejectPassengers();
            for (var passenger : passengers) if (passenger instanceof ServerPlayer player
                    && !player.isCreative() && !player.isSpectator()) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.getAbilities().setFlyingSpeed(0.05F);
                player.onUpdateAbilities();
                JindanCreativeFlight.refreshFlightAbilities(player);
            }
        }
        // Old saves may contain a Dao in this entity type. Return the exact item instead of letting it fly.
        if (!level().isClientSide && !isFlyingWeapon(pickupSwordStack())) {
            ejectPassengers();
            entityData.set(SWORD_FLIGHT, false);
            FlyingSwordRecovery.complete(this);
            level().addFreshEntity(new ItemEntity(level(), getX(), getY(), getZ(), pickupSwordStack()));
            discard();
            return;
        }
        if (!level().isClientSide && ManifestedWeaponItem.expireFlyingSword(this)) {
            return;
        }
        if (!level().isClientSide && tickAutomaticPouchRecall()) {
            return;
        }
        if(!level().isClientSide&&tickAutomaticRecall())return;
        if (isSwordFlight()) {
            tickSwordFlight();
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

    private boolean tickAutomaticPouchRecall() {
        ServerPlayer recallOwner=ownerPlayer();
        if(recallOwner!=null&&!FlyingSwordRecovery.autoRecallEnabled(recallOwner)){pouchRecallTicks=0;return false;}
        if (isVehicle() || isControlled() || isShot()) {
            pouchRecallTicks = 0;
            return false;
        }
        pouchRecallTicks++;
        if (pouchRecallTicks < QiPouchStorage.RECALL_DELAY_TICKS
                || tickCount % 20 != 0
                || !QiPouchStorage.tryRecallFlyingSword(this)) {
            return false;
        }
        ejectPassengers();
        FlyingSwordRecovery.complete(this);
        discard();
        return true;
    }
    private boolean tickAutomaticRecall() {
        if(isControlled()||isVehicle()){autoRecallTicks=0;return false;}
        ServerPlayer owner=ownerPlayer();
        if(owner==null||!owner.isAlive()||!FlyingSwordRecovery.autoRecallEnabled(owner)){autoRecallTicks=0;return false;}
        if(++autoRecallTicks<40)return false;
        if(QiPouchStorage.tryRecallFlyingSword(this)){FlyingSwordRecovery.complete(this);discard();return true;}
        if(owner.level()!=level()){if(recoveryManaged){FlyingSwordRecovery.park(this);return true;}return false;}
        if(!isDisabled()&&controlledSwords(owner).size()<maxControlledSwords(owner)){
            control(owner);setPos(orbitPosition(owner,0,1));return true;
        }
        return FlyingSwordRecovery.returnToInventory(this,owner);
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
        baseTick();
        ServerPlayer owner = ownerPlayer();
        if (owner == null || !owner.isAlive()) {
            FlyingSwordRecovery.park(this);
            return;
        }
        if(owner.level()!=level()){FlyingSwordRecovery.park(this);return;}
        if (tooFarFromOwner(owner)) {
            setPos(orbitPosition(owner,0,1));
        }

        qiTickCounter++;
        if (qiTickCounter >= 20) {
            qiTickCounter = 0;
            var data = ModAttachments.getData(owner);
            int cost = TalismanWardEntity.adjustFlightQiCost(
                    this, CONTROL_QI_COST_PER_SECOND);
            if (!ServerEvents.spendSustainedQiOrBlood(owner, data, cost)) {
                FlyingSwordRecovery.park(this);
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
        if(toTarget.lengthSqr()>64){setPos(target);setDeltaMovement(Vec3.ZERO);}
        else if (toTarget.lengthSqr() > 0.0025D) {
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

    public boolean isSwordFlight() {
        return entityData.get(SWORD_FLIGHT);
    }

    public void catchUpToOwner(ServerPlayer owner) {
        setPos(isSwordFlight()?owner.position().add(0,-FLIGHT_BLADE_OFFSET,0):orbitPosition(owner,0,1));
    }

    /** A real owned blade follows the player; it never carries or moves the player. */
    private void tickSwordFlight() {
        baseTick();
        noPhysics = true;
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
        Player owner = getOwner() instanceof Player player ? player
                : ownerUuid().map(level()::getPlayerByUUID).orElse(null);
        if (owner == null || !owner.isAlive() || !isControlled() || isDisabled()) {
            if (!level().isClientSide) FlyingSwordRecovery.park(this);
            return;
        }
        if (owner instanceof ServerPlayer serverPlayer) {
            if(owner.level()!=level()){FlyingSwordRecovery.park(this);return;}
            if (!JindanCreativeFlight.canUseSwordFlight(serverPlayer)) {
                entityData.set(SWORD_FLIGHT, false);
                FlyingSwordRecovery.track(this);
                return;
            }
            if (++qiTickCounter >= 20) {
                qiTickCounter = 0;
                if (!ServerEvents.spendSustainedQiOrBlood(serverPlayer, ModAttachments.getData(serverPlayer),
                        TalismanWardEntity.adjustFlightQiCost(this, CONTROL_QI_COST_PER_SECOND))) {
                    FlyingSwordRecovery.park(this);
                    JindanCreativeFlight.refreshFlightAbilities(serverPlayer);
                    ServerEvents.syncPlayerData(serverPlayer);
                    return;
                }
                ServerEvents.syncPlayerData(serverPlayer);
            }
        }
        setPos(owner.getX(), owner.getY() - FLIGHT_BLADE_OFFSET, owner.getZ());
        setYRot(owner.getYRot());
        setXRot(0.0F);
    }

    public static FlyingSwordEntity flightSword(Player owner) {
        if (owner == null) return null;
        if(owner instanceof ServerPlayer serverPlayer){
            var live=FlyingSwordRecovery.flightSword(serverPlayer);if(live!=null)return live;
        }
        return owner.level().getEntitiesOfClass(FlyingSwordEntity.class, owner.getBoundingBox().inflate(16),
                sword -> sword.isSwordFlight() && isFlyingWeapon(sword.pickupSwordStack()) && sword.isControlled() && !sword.isDisabled()
                        && !sword.isRemoved() && owner.getUUID().equals(sword.getOwnerUuid()))
                .stream().findFirst().orElse(null);
    }

    public static void stopSwordFlight(Player owner) {
        if (owner == null) return;
        if(owner instanceof ServerPlayer serverPlayer){
            var live=FlyingSwordRecovery.flightSword(serverPlayer);
            if(live!=null){live.entityData.set(SWORD_FLIGHT,false);FlyingSwordRecovery.track(live);}
        }
        for (FlyingSwordEntity sword : owner.level().getEntitiesOfClass(FlyingSwordEntity.class,
                owner.getBoundingBox().inflate(32), sword -> sword.isSwordFlight()
                        && owner.getUUID().equals(sword.getOwnerUuid()))) {
            sword.entityData.set(SWORD_FLIGHT, false);
            if(!sword.level().isClientSide)FlyingSwordRecovery.track(sword);
        }
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
        return com.example.immortal_cultivation_mod.item.WeaponBinding.canUse(storedSwordStack,player)&&FlyingSwordRecovery.valid(this)&&isFlyingWeapon(pickupSwordStack()) && !isDisabled() && !isControlled() && owner != null && owner.equals(player.getUUID());
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
        setNoGravity(true);
        noPhysics = true;
        setDeltaMovement(Vec3.ZERO);
        qiTickCounter = 0;
        autoRecallTicks=0;
        FlyingSwordRecovery.track(this);
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
        setNoGravity(true);
        noPhysics = true;
        setPos(owner.getEyePosition().add(look.scale(BLADE_LENGTH * 0.55D)));
        setShotRotation(look);
        float speed = (float) (SHOOT_SPEED
                * com.example.immortal_cultivation_mod.item.WeaponRestrictionSystem
                .flyingSpeedMultiplier(storedSwordStack));
        shoot(look.x, look.y, look.z, speed, 0.0F);
        autoRecallTicks=0;
        FlyingSwordRecovery.track(this);
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        UUID owner = getOwnerUuid();
        Entity target = ModelBonePartEntity.resolveParent(entity);
        if (!isShot()) {
            return false;
        }
        if (owner != null && target.getUUID().equals(owner)) {
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
        Entity target = ModelBonePartEntity.resolveParent(result.getEntity());
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
        boolean damaged = com.example.immortal_cultivation_mod.flame.FlameWeaponEffects.hurtWithWeapon(
                storedSwordStack, target, source, damageAgainst(target));
        if (damaged && target instanceof LivingEntity living) {
            com.example.immortal_cultivation_mod.item.ForgingSystem.applyWeaponHitEffects(storedSwordStack, living);
            com.example.immortal_cultivation_mod.item.WeaponRestrictionSystem
                    .applyWeaponHitEffects(storedSwordStack, living);
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
        entityData.set(SWORD_FLIGHT, false);
        entityData.set(CONTROLLED, false);
        entityData.set(SHOT, false);
        entityData.set(STUCK, false);
        noPhysics = false;
        setNoGravity(false);
        setDeltaMovement(Vec3.ZERO);
        Vec3 openPos = level().isClientSide ? pos : findOpenLooseSwordPosition(pos);
        setPos(openPos.x, openPos.y + 0.02D, openPos.z);
        if(recoveryManaged)FlyingSwordRecovery.track(this);
    }

    private void dropStraightDown() {
        entityData.set(SWORD_FLIGHT, false);
        if (!level().isClientSide) {
            ejectPassengers();
        }
        entityData.set(CONTROLLED, false);
        entityData.set(SHOT, false);
        entityData.set(STUCK, false);
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
        entityData.set(SWORD_FLIGHT, false);
        entityData.set(CONTROLLED, false);
        entityData.set(SHOT, false);
        entityData.set(STUCK, true);
        noPhysics = true;
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
        if (shotMotion.lengthSqr() > 0.0001D) {
            setShotRotation(shotMotion.normalize());
        }
        setPos(pos.x, pos.y, pos.z);
        autoRecallTicks=0;
        if(recoveryManaged)FlyingSwordRecovery.track(this);
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
        if(!FlyingSwordRecovery.valid(this)){discard();return InteractionResult.CONSUME;}
        UUID owner = getOwnerUuid();
        if (owner != null && !owner.equals(player.getUUID())) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown() || isVehicle() || isSwordFlight()) {
            return InteractionResult.CONSUME;
        }
        if(player instanceof ServerPlayer serverPlayer){FlyingSwordRecovery.returnToInventory(this,serverPlayer);JindanCreativeFlight.refreshFlightAbilities(serverPlayer);return InteractionResult.CONSUME;}
        ItemStack stack = pickupSwordStack();
        if (!player.getInventory().add(stack)) {
            level().addFreshEntity(new ItemEntity(level(), getX(), getY(), getZ(), stack));
        }
        if (player instanceof ServerPlayer serverPlayer) {
            entityData.set(SWORD_FLIGHT, false);
            JindanCreativeFlight.refreshFlightAbilities(serverPlayer);
        }
        FlyingSwordRecovery.complete(this);
        discard();
        return InteractionResult.CONSUME;
    }

    @Override
    public void playerTouch(Player player) {
        // Right-click pickup only. Walking over the weapon entity must not collect it.
    }

    public static boolean interactLookingAt(ServerPlayer player, boolean ride) {
        if (ride) return toggleRiding(player);
        FlyingSwordEntity sword = findInteractableSword(player);
        if (sword == null) {
            return false;
        }
        return sword.handlePlayerInteract(player, ride);
    }

    /** Selects an owned blade for Golden Core creative flight, without a passenger mount. */
    public static boolean toggleRiding(ServerPlayer player) {
        if (flightSword(player) != null) {
            stopSwordFlight(player);
            JindanCreativeFlight.refreshFlightAbilities(player);
            return true;
        }
        if (player.isPassenger() || player.isSpectator() || player.isCreative()
                || !player.isAlive() || player.isShiftKeyDown()) return false;
        if (CultivationLevels.getRealmIndex(ModAttachments.getData(player).cultivationLevel()) < 3) {
            player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.sword_ride.realm"), true);
            return false;
        }
        var candidates = controlledSwords(player).stream()
                .filter(sword -> com.example.immortal_cultivation_mod.item.WeaponBinding.canUse(sword.storedSwordStack,player) && !sword.isShot() && !sword.isStuck()
                        && sword.distanceToSqr(player) <= 64 && player.hasLineOfSight(sword))
                .sorted(Comparator.comparingDouble(sword -> sword.distanceToSqr(player))).toList();
        if (candidates.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.sword_ride.no_sword"), true);
            return false;
        }
        JindanCreativeFlight.setEnabled(player, true);
        JindanCreativeFlight.refreshFlightAbilities(player);
        if (!JindanCreativeFlight.canUseSwordFlight(player)) return false;
        var sword = candidates.getFirst();
        sword.entityData.set(SWORD_FLIGHT, true);
        sword.tickSwordFlight();
        JindanCreativeFlight.refreshFlightAbilities(player);
        if (!player.getAbilities().mayfly) {
            sword.entityData.set(SWORD_FLIGHT, false);
            return false;
        }
        FlyingSwordRecovery.track(sword);
        player.displayClientMessage(Component.translatable("message.immortal_cultivation_mod.sword_ride.controls",
                Component.keybind("key.immortal_cultivation_mod.ride_flying_sword"), Component.keybind("key.sneak")), true);
        return true;
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
        if (ride) return toggleRiding(player);
        if (isVehicle() || isSwordFlight() || player.isShiftKeyDown()) return false;
        if(!FlyingSwordRecovery.valid(this)){discard();return false;}
        boolean returned=FlyingSwordRecovery.returnToInventory(this,player);
        JindanCreativeFlight.refreshFlightAbilities(player);
        return returned;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved()) {
            return true;
        }
        if(!FlyingSwordRecovery.valid(this)){discard();return true;}
        swordHealth -= Math.max(1, (int) Math.ceil(amount));
        if (swordHealth <= 0) {
            entityData.set(DISABLED, true);
            landAt(position());
        }
        if(recoveryManaged)FlyingSwordRecovery.track(this);
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
        tag.putBoolean("SwordFlight", isSwordFlight());
        tag.putBoolean("Disabled", isDisabled());
        tag.putBoolean("Shot", isShot());
        tag.putBoolean("Stuck", isStuck());
        tag.putInt("SwordHealth", swordHealth);
        tag.putInt("SwordMaxHealth", swordMaxHealth);
        tag.putFloat("WeaponDamage", getWeaponDamage());
        tag.putInt("SwordIndex", getSwordIndex());
        tag.putInt("TotalControlledSwords", getTotalControlledSwords());
        tag.putFloat("FormationAngle", getFormationAngle());
        tag.putFloat("StuckYaw", getStuckYaw());
        tag.putFloat("StuckPitch", getStuckPitch());
        tag.putInt("PouchRecallTicks", pouchRecallTicks);
        tag.putInt("AutoRecallTicks",autoRecallTicks);tag.putBoolean("RecoveryManaged",recoveryManaged);
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
        entityData.set(SWORD_FLIGHT,tag.getBoolean("SwordFlight")&&isControlled()&&!isDisabled()&&!isShot()&&!isStuck());
        entityData.set(WEAPON_DAMAGE, tag.contains("WeaponDamage") ? tag.getFloat("WeaponDamage") : 10.0F);
        entityData.set(SWORD_INDEX, tag.getInt("SwordIndex"));
        entityData.set(TOTAL_CONTROLLED_SWORDS, Math.max(1, tag.getInt("TotalControlledSwords")));
        entityData.set(FORMATION_ANGLE, tag.getFloat("FormationAngle"));
        entityData.set(STUCK_YAW, tag.getFloat("StuckYaw"));
        entityData.set(STUCK_PITCH, tag.getFloat("StuckPitch"));
        pouchRecallTicks = Math.max(0, tag.getInt("PouchRecallTicks"));
        autoRecallTicks=Math.max(0,tag.getInt("AutoRecallTicks"));recoveryManaged=tag.getBoolean("RecoveryManaged");
        if (tag.contains("SwordStack")) {
            storedSwordStack = ItemStack.parseOptional(registryAccess(), tag.getCompound("SwordStack"));
        }
        if (storedSwordStack.isEmpty()) {
            storedSwordStack = new ItemStack(ModItems.FLYING_SWORD.get());
        }
        setItem(storedSwordStack);
        swordMaxHealth = weaponEntityHealth(storedSwordStack);
        if (tag.contains("SwordMaxHealth") && tag.contains("SwordHealth")) {
            swordHealth = Math.max(0, Math.min(swordMaxHealth, tag.getInt("SwordHealth")));
        } else {
            // Old entities used a fixed 20 HP pool; migrate them to the durability-derived maximum.
            swordHealth = swordMaxHealth;
        }
    }

    public static boolean releaseFromHand(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        InteractionHand hand = InteractionHand.MAIN_HAND;
        if (!isFlyingWeapon(stack)) {
            stack = player.getOffhandItem();
            hand = InteractionHand.OFF_HAND;
        }
        if (!isFlyingWeapon(stack)) {
            boolean recalled = recallNearby(player, 30.0D) > 0;
            if (recalled) {
                com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.record(
                        player,
                        com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent.RECALL_WEAPON);
            }
            return recalled;
        }

        if (!com.example.immortal_cultivation_mod.item.WeaponBinding.checkUse(stack, player)) return false;
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

    public static int releaseControlledFromCase(ServerPlayer player, List<ItemStack> weapons) {
        if (!(player.level() instanceof ServerLevel level) || weapons.isEmpty()) {
            return 0;
        }
        Vec3 forward = horizontal(player.getLookAngle());
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        Vec3 center = player.position().subtract(forward.scale(1.1D)).add(0.0D, 1.35D, 0.0D);
        int released = 0;
        for (ItemStack weapon : weapons) {
            if (!isFlyingWeapon(weapon) || !com.example.immortal_cultivation_mod.item.WeaponBinding.checkUse(weapon, player)) {
                continue;
            }
            double sideOffset = (released - (weapons.size() - 1) * 0.5D) * 0.18D;
            FlyingSwordEntity sword = new FlyingSwordEntity(level, player, center.add(right.scale(sideOffset)));
            sword.setStoredSwordStack(weapon);
            if (!level.addFreshEntity(sword)) {
                break;
            }
            sword.control(player);
            released++;
        }
        return released;
    }

    public static List<ItemStack> collectControlledForCase(ServerPlayer player) {
        List<ItemStack> collected = new ArrayList<>();
        for (FlyingSwordEntity sword : controlledSwords(player)) {
            ItemStack stack = sword.pickupSwordStack();
            if (isFlyingWeapon(stack)) {
                collected.add(stack);
                sword.ejectPassengers();
                FlyingSwordRecovery.complete(sword);
                sword.discard();
            }
        }
        if (!collected.isEmpty()) {
            com.example.immortal_cultivation_mod.guidance.DaoGuidanceSystem.record(
                    player,
                    com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent.RECALL_WEAPON);
        }
        return collected;
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
        List<FlyingSwordEntity> swords = controlledSwords(player).stream().filter(sword -> !sword.isSwordFlight() && !sword.isVehicle()).toList();
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
            if (com.example.immortal_cultivation_mod.item.WeaponBinding.canUse(sword.storedSwordStack,owner)&&FlyingSwordRecovery.valid(sword)&&sword.isControlled() && isFlyingWeapon(sword.pickupSwordStack()) && !sword.isDisabled() && owner.getUUID().equals(sword.getOwnerUuid())) {
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
        return false;
    }

    private ServerPlayer ownerPlayer() {
        UUID owner = getOwnerUuid();
        if (owner == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        ServerPlayer online=serverLevel.getServer().getPlayerList().getPlayer(owner);
        return online!=null?online:getOwner() instanceof ServerPlayer player&&!player.isRemoved()&&owner.equals(player.getUUID())?player:null;
    }
    public static FlyingSwordEntity restoreControlled(ServerPlayer owner,ItemStack stack,int health,boolean swordFlight) {
        if(stack.isEmpty()||health<=0||!isFlyingWeapon(stack)||!com.example.immortal_cultivation_mod.item.WeaponBinding.canUse(stack,owner)||controlledSwords(owner).size()>=maxControlledSwords(owner))return null;
        var sword=new FlyingSwordEntity(owner.level(),owner,orbitPosition(owner,0,1));sword.setStoredSwordStack(stack);
        sword.swordHealth=Math.min(sword.swordMaxHealth,health);
        sword.entityData.set(SWORD_FLIGHT,swordFlight&&JindanCreativeFlight.canUseSwordFlight(owner));
        sword.catchUpToOwner(owner);
        if(!owner.level().addFreshEntity(sword))return null;
        sword.control(owner);return sword;
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

    private void resetSwordHealth(ItemStack stack) {
        swordMaxHealth = weaponEntityHealth(stack);
        swordHealth = swordMaxHealth;
    }

    private static int weaponEntityHealth(ItemStack stack) {
        int durability = stack.isDamageableItem() ? stack.getMaxDamage() : 10;
        return Math.max(1, durability / 10);
    }

    private float damageAgainst(Entity target) {
        float damage = getWeaponDamage();
        if (target instanceof LivingEntity living) {
            damage = com.example.immortal_cultivation_mod.item.ForgingSystem.adjustWeaponDamage(storedSwordStack, living, damage);
            damage = com.example.immortal_cultivation_mod.item.WeaponRestrictionSystem
                    .adjustWeaponDamage(storedSwordStack, living, damage);
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

    public static boolean isFlyingWeapon(ItemStack stack) {
        if (stack.isEmpty() || com.example.immortal_cultivation_mod.item.RigidSheathItem.isDaoWeapon(stack)) return false;
        if (stack.getItem() instanceof com.example.immortal_cultivation_mod.item.FlyingSwordItem
                || com.example.immortal_cultivation_mod.item.RigidSheathItem.isSwordWeapon(stack)) return true;
        // Several mod fans, spears and bracers inherit SwordItem for melee attributes.
        // Only actual mod swords pass above; vanilla/other mods use their sword class or tag.
        if (net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace()
                .equals(com.example.immortal_cultivation_mod.ImmortalCultivationMod.MODID)) return false;
        return stack.is(net.minecraft.tags.ItemTags.SWORDS)
                || stack.getItem() instanceof net.minecraft.world.item.SwordItem;
    }
}
