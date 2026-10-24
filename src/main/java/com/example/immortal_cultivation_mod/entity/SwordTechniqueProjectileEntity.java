package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.magicritual.center.ForcefieldInterceptable;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.ForcefieldCenterBehavior;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import com.example.immortal_cultivation_mod.spell.SpellCharge;
import com.example.immortal_cultivation_mod.spell.SpellDamageHelper;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Delayed spatial sword scar and three-path spiritual sword projectile. */
public final class SwordTechniqueProjectileEntity extends ThrowableItemProjectile
        implements DefensePiercingProjectile, ForcefieldInterceptable {
    private static final int MAX_LIFETIME_TICKS = 20 * 30;
    public enum Kind {
        POKONG_JIANHEN,
        SANJIAN_ZHUIMING;

        static Kind byId(int id) {
            Kind[] values = values();
            return values[Math.floorMod(id, values.length)];
        }
    }

    private record FateState(UUID targetId, UUID markerId, int hits, long expiresAt) {
    }

    private static final EntityDataAccessor<Integer> DATA_KIND =
            SynchedEntityData.defineId(
                    SwordTechniqueProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_INDEX =
            SynchedEntityData.defineId(
                    SwordTechniqueProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_CHARGE =
            SynchedEntityData.defineId(
                    SwordTechniqueProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LAUNCH_DELAY =
            SynchedEntityData.defineId(
                    SwordTechniqueProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_LAUNCHED =
            SynchedEntityData.defineId(
                    SwordTechniqueProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_ROLL =
            SynchedEntityData.defineId(
                    SwordTechniqueProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIRECTION_X =
            SynchedEntityData.defineId(
                    SwordTechniqueProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIRECTION_Y =
            SynchedEntityData.defineId(
                    SwordTechniqueProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DIRECTION_Z =
            SynchedEntityData.defineId(
                    SwordTechniqueProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Optional<UUID>> DATA_TARGET =
            SynchedEntityData.defineId(
                    SwordTechniqueProjectileEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> DATA_CAST_ID =
            SynchedEntityData.defineId(
                    SwordTechniqueProjectileEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> DATA_MARKED =
            SynchedEntityData.defineId(
                    SwordTechniqueProjectileEntity.class, EntityDataSerializers.BOOLEAN);

    private static final Map<UUID, FateState> FATE_MARKS = new HashMap<>();

    private final Set<UUID> hitTargets = new HashSet<>();
    private int flightTicks;

    public SwordTechniqueProjectileEntity(
            EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static SwordTechniqueProjectileEntity createPokong(
            ServerLevel level,
            LivingEntity owner,
            LivingEntity target,
            float chargeScale,
            float roll) {
        SwordTechniqueProjectileEntity scar =
                new SwordTechniqueProjectileEntity(ModEntities.SWORD_TECHNIQUE_PROJECTILE.get(), level);
        scar.setOwner(owner);
        scar.setKind(Kind.POKONG_JIANHEN);
        scar.setChargeScale(chargeScale);
        scar.entityData.set(DATA_INDEX, 0);
        scar.entityData.set(DATA_LAUNCH_DELAY, 3);
        scar.entityData.set(DATA_ROLL, roll);
        scar.setTarget(target);
        Vec3 start = owner.getEyePosition().add(owner.getLookAngle().scale(0.85D));
        scar.setPos(start.x, start.y, start.z);
        Vec3 aim = target == null ? owner.getLookAngle()
                : target.getEyePosition().subtract(start);
        scar.setPlannedDirection(aim);
        scar.setDeltaMovement(Vec3.ZERO);
        NpcProfile.markNpcOwnedEffect(scar, owner);
        return scar;
    }

    public static SwordTechniqueProjectileEntity createPursuitSword(
            ServerLevel level,
            LivingEntity owner,
            LivingEntity target,
            float chargeScale,
            UUID castId,
            int index) {
        SwordTechniqueProjectileEntity sword =
                new SwordTechniqueProjectileEntity(ModEntities.SWORD_TECHNIQUE_PROJECTILE.get(), level);
        sword.setOwner(owner);
        sword.setKind(Kind.SANJIAN_ZHUIMING);
        sword.setChargeScale(chargeScale);
        sword.entityData.set(DATA_INDEX, Mth.clamp(index, 0, 2));
        sword.entityData.set(DATA_LAUNCH_DELAY, switch (index) {
            case 0 -> 5;
            case 1 -> 12;
            default -> 18;
        });
        sword.entityData.set(DATA_ROLL, index == 1 ? -24.0F : index == 2 ? 22.0F : 0.0F);
        sword.setTarget(target);
        sword.entityData.set(DATA_CAST_ID, Optional.of(castId));
        sword.followFormation(owner);
        sword.setDeltaMovement(Vec3.ZERO);
        NpcProfile.markNpcOwnedEffect(sword, owner);
        return sword;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_KIND, Kind.POKONG_JIANHEN.ordinal());
        builder.define(DATA_INDEX, 0);
        builder.define(DATA_CHARGE, SpellCharge.NORMAL);
        builder.define(DATA_LAUNCH_DELAY, 0);
        builder.define(DATA_LAUNCHED, false);
        builder.define(DATA_ROLL, 0.0F);
        builder.define(DATA_DIRECTION_X, 0.0F);
        builder.define(DATA_DIRECTION_Y, 0.0F);
        builder.define(DATA_DIRECTION_Z, 1.0F);
        builder.define(DATA_TARGET, Optional.empty());
        builder.define(DATA_CAST_ID, Optional.empty());
        builder.define(DATA_MARKED, false);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.IRON_SWORD;
    }

    private void setKind(Kind kind) {
        entityData.set(DATA_KIND, (kind == null ? Kind.POKONG_JIANHEN : kind).ordinal());
    }

    public Kind kind() {
        return Kind.byId(entityData.get(DATA_KIND));
    }

    public int swordIndex() {
        return Mth.clamp(entityData.get(DATA_INDEX), 0, 2);
    }

    public int launchDelay() {
        return Math.max(0, entityData.get(DATA_LAUNCH_DELAY));
    }

    public boolean launched() {
        return entityData.get(DATA_LAUNCHED);
    }

    public float roll() {
        return entityData.get(DATA_ROLL);
    }

    public boolean marked() {
        return entityData.get(DATA_MARKED);
    }

    public float chargeScale() {
        return SpellCharge.clamp(entityData.get(DATA_CHARGE));
    }

    private void setChargeScale(float chargeScale) {
        entityData.set(DATA_CHARGE, SpellCharge.clamp(chargeScale));
    }

    private void setTarget(LivingEntity target) {
        entityData.set(DATA_TARGET, target == null
                ? Optional.empty() : Optional.of(target.getUUID()));
    }

    private void setPlannedDirection(Vec3 direction) {
        Vec3 resolved = direction == null || direction.lengthSqr() < 1.0E-6D
                ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
        entityData.set(DATA_DIRECTION_X, (float) resolved.x);
        entityData.set(DATA_DIRECTION_Y, (float) resolved.y);
        entityData.set(DATA_DIRECTION_Z, (float) resolved.z);
    }

    public Vec3 plannedDirection() {
        Vec3 direction = new Vec3(
                entityData.get(DATA_DIRECTION_X),
                entityData.get(DATA_DIRECTION_Y),
                entityData.get(DATA_DIRECTION_Z));
        return direction.lengthSqr() < 1.0E-6D
                ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
    }

    @Override
    public void tick() {
        setNoGravity(true);
        Entity ownerEntity = getOwner();
        if (!level().isClientSide) {
            NpcProfile.markNpcOwnedEffect(this, ownerEntity);
            if (!(ownerEntity instanceof LivingEntity owner)
                    || NpcProfile.isOrphanedNpcEffect(this, owner)) {
                discard();
                return;
            }
        }

        if (!launched() && !level().isClientSide) {
            if (tickCount >= launchDelay()) {
                launch();
                entityData.set(DATA_LAUNCHED, true);
            } else {
                if (kind() == Kind.SANJIAN_ZHUIMING
                        && ownerEntity instanceof LivingEntity owner) {
                    followFormation(owner);
                }
                setDeltaMovement(Vec3.ZERO);
            }
        }

        if (!launched()) {
            // The server owns formation position and velocity. Resetting velocity on
            // the client can erase the launch packet just before entity data syncs,
            // leaving the rendered sword floating forever.
            super.tick();
            spawnClientTrail();
            return;
        }

        if (!level().isClientSide && getDeltaMovement().lengthSqr() < 1.0E-6D) {
            // Recover legacy entities or a launch whose velocity was cleared by an
            // effect on the exact release tick.
            launch();
        }

        if (!level().isClientSide && kind() == Kind.SANJIAN_ZHUIMING) {
            homePursuitSword();
        }
        Vec3 previous = position();
        super.tick();
        if (isRemoved()) {
            return;
        }
        flightTicks++;
        if (!level().isClientSide
                && ForcefieldCenterBehavior.interceptProjectileImpact(this, position())) {
            discard();
            return;
        }
        if (!level().isClientSide && kind() == Kind.POKONG_JIANHEN) {
            hitAdditionalTargets(previous, position());
        }
        spawnClientTrail();
        if (!level().isClientSide && flightTicks > MAX_LIFETIME_TICKS) {
            emitMissVfx();
            discard();
        }
    }

    private void launch() {
        Vec3 direction = plannedDirection();
        if (kind() == Kind.POKONG_JIANHEN) {
            setDeltaMovement(direction.scale(4.2D));
        } else {
            LivingEntity owner = getOwner() instanceof LivingEntity living ? living : null;
            LivingEntity target = resolveTarget();
            if (target != null) {
                direction = target.getEyePosition().subtract(position()).normalize();
            }
            Vec3 right = owner == null ? new Vec3(1.0D, 0.0D, 0.0D)
                    : owner.getLookAngle().cross(new Vec3(0.0D, 1.0D, 0.0D));
            right = right.lengthSqr() < 1.0E-6D
                    ? new Vec3(1.0D, 0.0D, 0.0D) : right.normalize();
            direction = switch (swordIndex()) {
                case 1 -> direction.scale(0.72D).add(right.scale(-0.72D));
                case 2 -> direction.scale(0.70D).add(0.0D, 0.92D, 0.0D)
                        .add(right.scale(0.18D));
                default -> direction;
            };
            direction = direction.normalize();
            setPlannedDirection(direction);
            setDeltaMovement(direction.scale(swordIndex() == 0 ? 2.25D : 1.72D));
        }
        hasImpulse = true;
        hurtMarked = true;
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.FLASH,
                    getX(), getY(), getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.TRIDENT_THROW,
                    SoundSource.PLAYERS, 0.8F, kind() == Kind.POKONG_JIANHEN ? 1.75F : 1.48F);
        }
    }

    private void followFormation(LivingEntity owner) {
        Vec3 look = owner.getLookAngle().normalize();
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
        right = right.lengthSqr() < 1.0E-6D
                ? new Vec3(1.0D, 0.0D, 0.0D) : right.normalize();
        Vec3 up = right.cross(look).normalize();
        Vec3 offset = switch (swordIndex()) {
            case 0 -> right.scale(0.92D).add(up.scale(0.48D)).add(look.scale(-0.18D));
            case 1 -> right.scale(-1.04D).add(up.scale(0.06D)).add(look.scale(-0.58D));
            default -> up.scale(1.22D).add(look.scale(-0.78D)).add(right.scale(0.18D));
        };
        Vec3 position = owner.getEyePosition().add(offset);
        setPos(position.x, position.y, position.z);
        LivingEntity target = resolveTarget();
        setPlannedDirection(target == null ? look
                : target.getEyePosition().subtract(position));
    }

    private void homePursuitSword() {
        if (flightTicks >= 18) {
            return;
        }
        LivingEntity target = resolveTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        Vec3 velocity = getDeltaMovement();
        if (velocity.lengthSqr() < 1.0E-6D) {
            return;
        }
        Vec3 aimPoint = target.getEyePosition().add(target.getDeltaMovement().scale(0.28D));
        Vec3 desired = aimPoint.subtract(position()).normalize();
        Vec3 current = velocity.normalize();
        if (current.dot(desired) < -0.15D) {
            return;
        }
        double strength = marked() ? (swordIndex() == 0 ? 0.20D : 0.31D)
                : (swordIndex() == 0 ? 0.045D : 0.13D);
        double speed = swordIndex() == 0 ? 2.25D : 1.72D;
        Vec3 corrected = current.scale(1.0D - strength)
                .add(desired.scale(strength)).normalize().scale(speed);
        setDeltaMovement(corrected);
        setPlannedDirection(corrected);
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        Entity resolved = ModelBonePartEntity.resolveParent(entity);
        return resolved instanceof LivingEntity
                && !hitTargets.contains(resolved.getUUID())
                && NpcProfile.canNpcEffectHit(this, getOwner(), resolved)
                && super.canHitEntity(entity);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide) {
            return;
        }
        if (result.getType() == HitResult.Type.BLOCK
                || kind() == Kind.SANJIAN_ZHUIMING
                || hitTargets.size() >= 2) {
            discard();
        } else if (result.getType() == HitResult.Type.ENTITY) {
            Vec3 direction = getDeltaMovement().normalize();
            setPos(position().add(direction.scale(0.34D)));
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity resolved = ModelBonePartEntity.resolveParent(result.getEntity());
        if (!(resolved instanceof LivingEntity target) || !hitTargets.add(target.getUUID())) {
            return;
        }
        if (!(level() instanceof ServerLevel level) || !(getOwner() instanceof LivingEntity owner)) {
            return;
        }
        if (kind() == Kind.POKONG_JIANHEN) {
            int hitNumber = hitTargets.size();
            float baseDamage = hitNumber == 1 ? 75.0F : 60.0F;
            boolean blocking = target.isBlocking();
            if (blocking) {
                baseDamage *= 1.25F;
            }
            SwordTechniqueMarkEntity mark = SwordTechniqueMarkEntity.createCut(
                    level, owner, target, kind(), baseDamage, chargeScale(), roll(),
                    2, blocking ? 1.25F : 1.0F);
            if (mark != null) {
                level.addFreshEntity(mark);
            }
            if (hitTargets.size() >= 2) {
                discard();
            }
            return;
        }

        int priorInvulnerability = target.invulnerableTime;
        target.invulnerableTime = 0;
        float damage = SpellDamageHelper.damage(owner,
                SpellCharge.damage(100.0F, chargeScale()),
                ModSpells.get(ModSpells.SANJIAN_ZHUIMING));
        boolean hurt = target.hurt(damageSources().thrown(this, owner), damage);
        if (!hurt) {
            target.invulnerableTime = priorInvulnerability;
        } else {
            recordFateHit(level, owner, target);
        }
        SwordTechniqueMarkEntity cut = SwordTechniqueMarkEntity.createCut(
                level, owner, target, kind(), 0.0F, chargeScale(), roll(), 0, 1.0F);
        if (cut != null) {
            level.addFreshEntity(cut);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ModParticles.METHOD_QI_METAL.get(),
                    result.getLocation().x, result.getLocation().y, result.getLocation().z,
                    20, 0.22D, 0.22D, 0.22D, 0.09D);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    result.getLocation().x, result.getLocation().y, result.getLocation().z,
                    12, 0.20D, 0.20D, 0.20D, 0.12D);
            level.playSound(null, result.getBlockPos(), SoundEvents.TRIDENT_HIT_GROUND,
                    SoundSource.BLOCKS, 0.75F, 1.72F);
        }
    }

    private void hitAdditionalTargets(Vec3 start, Vec3 uncappedEnd) {
        if (!isAlive() || getDeltaMovement().lengthSqr() < 1.0E-6D) {
            return;
        }
        Vec3 direction = getDeltaMovement().normalize();
        Vec3 extendedStart = start.subtract(direction.scale(0.72D));
        Vec3 end = uncappedEnd;
        BlockHitResult blockHit = level().clip(new ClipContext(
                extendedStart, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            end = blockHit.getLocation();
        }
        double contactMargin = com.example.immortal_cultivation_mod.spell.SpellProjectileCollision.margin(this, 0.16F);
        AABB path = new AABB(extendedStart, end).inflate(contactMargin);
        Vec3 finalEnd = end;
        List<LivingEntity> targets = level().getEntitiesOfClass(
                        LivingEntity.class, path, this::canHitEntity)
                .stream()
                .filter(target -> target.getBoundingBox().inflate(contactMargin)
                        .clip(extendedStart, finalEnd).isPresent())
                .sorted(Comparator.comparingDouble(target -> target.getBoundingBox().inflate(contactMargin)
                        .clip(extendedStart, finalEnd).orElse(finalEnd).distanceToSqr(extendedStart)))
                .toList();
        for (LivingEntity target : targets) {
            if (!isAlive() || hitTargets.size() >= 2) {
                break;
            }
            Vec3 hit = target.getBoundingBox().inflate(contactMargin)
                    .clip(extendedStart, finalEnd).orElse(target.getBoundingBox().getCenter());
            onHitEntity(new EntityHitResult(target, hit));
        }
    }

    private void recordFateHit(
            ServerLevel level, LivingEntity owner, LivingEntity target) {
        Optional<UUID> cast = entityData.get(DATA_CAST_ID);
        if (cast.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        FATE_MARKS.entrySet().removeIf(entry -> entry.getValue().expiresAt() < now);
        UUID castId = cast.get();
        FateState state = FATE_MARKS.get(castId);
        if (state == null || state.expiresAt() < now) {
            SwordTechniqueMarkEntity marker =
                    SwordTechniqueMarkEntity.createFate(level, owner, target, castId);
            if (marker == null || !level.addFreshEntity(marker)) {
                return;
            }
            state = new FateState(target.getUUID(), marker.getUUID(), 1, now + 70L);
            FATE_MARKS.put(castId, state);
        } else if (state.targetId().equals(target.getUUID())) {
            state = new FateState(state.targetId(), state.markerId(),
                    Math.min(3, state.hits() + 1), now + 70L);
            FATE_MARKS.put(castId, state);
        }

        LivingEntity markedTarget = level.getEntity(state.targetId()) instanceof LivingEntity living
                ? living : target;
        for (SwordTechniqueProjectileEntity sword : level.getEntitiesOfClass(
                SwordTechniqueProjectileEntity.class,
                new AABB(position(), position()).inflate(40.0D),
                candidate -> candidate != this
                        && candidate.entityData.get(DATA_CAST_ID).filter(castId::equals).isPresent())) {
            sword.setTarget(markedTarget);
            sword.entityData.set(DATA_MARKED, true);
        }
        if (level.getEntity(state.markerId()) instanceof SwordTechniqueMarkEntity marker) {
            marker.setHitCount(state.hits());
        }
    }

    private LivingEntity resolveTarget() {
        Optional<UUID> targetId = entityData.get(DATA_TARGET);
        return targetId.isPresent() && level() instanceof ServerLevel level
                && level.getEntity(targetId.get()) instanceof LivingEntity living ? living : null;
    }

    private void spawnClientTrail() {
        if (!level().isClientSide) {
            return;
        }
        if (!launched()) {
            level().addParticle(ModParticles.METHOD_QI_METAL.get(),
                    getX() + random.nextGaussian() * 0.14D,
                    getY() + random.nextGaussian() * 0.14D,
                    getZ() + random.nextGaussian() * 0.14D,
                    0.0D, 0.0D, 0.0D);
            return;
        }
        Vec3 motion = getDeltaMovement();
        Vec3 backward = motion.lengthSqr() < 1.0E-6D
                ? Vec3.ZERO : motion.normalize().reverse();
        int trail = kind() == Kind.POKONG_JIANHEN ? 9 : 5;
        for (int index = 0; index < trail; index++) {
            Vec3 point = position().add(backward.scale(index * 0.24D));
            level().addParticle(index % 3 == 0 ? ParticleTypes.END_ROD
                            : ModParticles.METHOD_QI_METAL.get(),
                    point.x, point.y, point.z,
                    backward.x * 0.012D, backward.y * 0.012D, backward.z * 0.012D);
        }
    }

    private void emitMissVfx() {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 motion = getDeltaMovement().lengthSqr() < 1.0E-6D
                ? plannedDirection() : getDeltaMovement().normalize();
        Vec3 side = motion.cross(new Vec3(0.0D, 1.0D, 0.0D));
        side = side.lengthSqr() < 1.0E-6D
                ? new Vec3(1.0D, 0.0D, 0.0D) : side.normalize();
        for (int thread = -2; thread <= 2; thread++) {
            Vec3 point = position().add(side.scale(thread * 0.13D));
            level.sendParticles(ModParticles.METHOD_QI_METAL.get(),
                    point.x, point.y, point.z, 4,
                    0.03D, 0.03D, 0.03D, 0.015D);
        }
    }

    @Override
    public float defenseBypass() {
        return 0.10F;
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
        return SpellCharge.damage(
                kind() == Kind.POKONG_JIANHEN ? 75.0F : 100.0F, chargeScale());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Kind", kind().ordinal());
        tag.putInt("Index", swordIndex());
        tag.putFloat("Charge", chargeScale());
        tag.putInt("LaunchDelay", launchDelay());
        tag.putBoolean("Launched", launched());
        tag.putFloat("Roll", roll());
        Vec3 direction = plannedDirection();
        tag.putDouble("DirectionX", direction.x);
        tag.putDouble("DirectionY", direction.y);
        tag.putDouble("DirectionZ", direction.z);
        entityData.get(DATA_TARGET).ifPresent(uuid -> tag.putUUID("Target", uuid));
        entityData.get(DATA_CAST_ID).ifPresent(uuid -> tag.putUUID("CastId", uuid));
        tag.putBoolean("Marked", marked());
        tag.putInt("FlightTicks", flightTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setKind(Kind.byId(tag.getInt("Kind")));
        entityData.set(DATA_INDEX, Mth.clamp(tag.getInt("Index"), 0, 2));
        setChargeScale(tag.contains("Charge") ? tag.getFloat("Charge") : SpellCharge.NORMAL);
        entityData.set(DATA_LAUNCH_DELAY, Math.max(0, tag.getInt("LaunchDelay")));
        entityData.set(DATA_LAUNCHED, tag.contains("Launched")
                ? tag.getBoolean("Launched")
                : tag.getDouble("Travelled") > 0.0D || tag.getInt("FlightTicks") > 0);
        entityData.set(DATA_ROLL, tag.getFloat("Roll"));
        setPlannedDirection(new Vec3(
                tag.getDouble("DirectionX"),
                tag.getDouble("DirectionY"),
                tag.getDouble("DirectionZ")));
        entityData.set(DATA_TARGET, tag.hasUUID("Target")
                ? Optional.of(tag.getUUID("Target")) : Optional.empty());
        entityData.set(DATA_CAST_ID, tag.hasUUID("CastId")
                ? Optional.of(tag.getUUID("CastId")) : Optional.empty());
        entityData.set(DATA_MARKED, tag.getBoolean("Marked"));
        flightTicks = Math.max(0, tag.getInt("FlightTicks"));
    }
}
