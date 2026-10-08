package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.joml.Vector3f;

/**
 * Server-authoritative physical asteroid. Its 0.1x..5x size maps to a 1..50
 * block diameter, and every hit records a synchronized model-space crater.
 */
public final class AsteroidEntity extends Entity {
    public static final int MAX_CRATERS = 16;
    private static final Vec3[] SURFACE_DIRECTIONS = createSurfaceDirections();
    private static final int NATURAL_DESPAWN_GRACE_TICKS = 600;
    private static final int NATURAL_DESPAWN_CHECK_TICKS = 200;
    private static final double NATURAL_DESPAWN_DISTANCE = 224.0D;

    private static final ResourceKey<DamageType> ASTEROID_IMPACT = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID, "asteroid_impact"));

    private static final EntityDataAccessor<Float> DATA_SIZE =
            SynchedEntityData.defineId(AsteroidEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_RENDER_SEED =
            SynchedEntityData.defineId(AsteroidEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<CompoundTag> DATA_CRATERS =
            SynchedEntityData.defineId(
                    AsteroidEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Boolean> DATA_NATURAL =
            SynchedEntityData.defineId(AsteroidEntity.class, EntityDataSerializers.BOOLEAN);

    private final Set<UUID> impactContacts = new HashSet<>();
    private final ModelBonePartEntity<?>[] surfaceParts;
    private float dimensionsSize = 1.0F;
    private List<Crater> cachedCraters;
    private DamageSource lastDamageSource;
    private long lastDamageTick = Long.MIN_VALUE;
    private boolean strongMotion;

    public AsteroidEntity(EntityType<? extends AsteroidEntity> type, Level level) {
        super(type, level);
        dimensionsSize = sizeFactor();
        setNoGravity(true);
        noPhysics = true;
        refreshDimensions();
        if (!level.isClientSide) {
            entityData.set(DATA_RENDER_SEED, nonZeroSeed(random));
        }
        surfaceParts = new ModelBonePartEntity<?>[SURFACE_DIRECTIONS.length];
        for (int index = 0; index < surfaceParts.length; index++) {
            surfaceParts[index] = new ModelBonePartEntity<>(
                    this, "surface_" + index, 0.1F, 0.1F);
        }
        setId(ENTITY_COUNTER.getAndAdd(surfaceParts.length + 1) + 1);
        updateSurfaceParts();
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        if (surfaceParts != null) {
            for (int index = 0; index < surfaceParts.length; index++) {
                surfaceParts[index].setId(id + index + 1);
            }
        }
    }

    /** The position argument is the desired center, not Entity's feet position. */
    public static AsteroidEntity createRandom(
            ServerLevel level, Vec3 center, RandomSource random) {
        AsteroidEntity asteroid = ModEntities.ASTEROID.get().create(level);
        if (asteroid == null) {
            return null;
        }
        double curve = Math.pow(random.nextDouble(), 2.35D);
        float size = Math.round((AsteroidMechanics.MIN_SIZE
                + (AsteroidMechanics.MAX_SIZE - AsteroidMechanics.MIN_SIZE)
                * curve) * 10.0F) / 10.0F;
        if (random.nextFloat() < 0.025F) {
            size = AsteroidMechanics.MAX_SIZE;
        }
        asteroid.setSizeFactor(size);
        asteroid.moveCenterTo(center);
        asteroid.entityData.set(DATA_RENDER_SEED, nonZeroSeed(random));
        asteroid.entityData.set(DATA_NATURAL, true);
        asteroid.setDeltaMovement(randomUnit(random)
                .scale(AsteroidMechanics.DRIFT_SPEED_BLOCKS_PER_TICK));
        asteroid.hasImpulse = true;
        return asteroid;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SIZE, 1.0F);
        builder.define(DATA_RENDER_SEED, 1);
        builder.define(DATA_CRATERS, new CompoundTag());
        builder.define(DATA_NATURAL, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_SIZE.equals(key)) {
            dimensionsSize = AsteroidMechanics.clampActiveSize(entityData.get(DATA_SIZE));
            refreshDimensions();
            updateSurfaceParts();
        } else if (DATA_CRATERS.equals(key)) {
            cachedCraters = null;
        }
    }

    public float sizeFactor() {
        return AsteroidMechanics.clampActiveSize(entityData.get(DATA_SIZE));
    }

    public float diameterBlocks() {
        return AsteroidMechanics.diameter(sizeFactor());
    }

    public float radiusBlocks() {
        return diameterBlocks() * 0.5F;
    }

    /** Resizes around the current center despite Minecraft's bottom-anchored AABB. */
    public void setSizeFactor(float size) {
        Vec3 center = centerPosition();
        float clamped = AsteroidMechanics.clampActiveSize(size);
        dimensionsSize = clamped;
        entityData.set(DATA_SIZE, clamped);
        refreshDimensions();
        moveCenterTo(center);
        updateSurfaceParts();
    }

    public Vec3 centerPosition() {
        return new Vec3(getX(), getY() + diameterBlocks() * 0.5D, getZ());
    }

    public void moveCenterTo(Vec3 center) {
        setPos(center.x, center.y - diameterBlocks() * 0.5D, center.z);
        updateSurfaceParts();
    }

    public int renderSeed() {
        return entityData.get(DATA_RENDER_SEED);
    }

    public float spinXDegrees(float partialTick) {
        return seedAngle(renderSeed(), 0x51F2)
                + spinTime(partialTick) * 0.075F;
    }

    public float spinYDegrees(float partialTick) {
        return seedAngle(renderSeed(), 0x9E37)
                + spinTime(partialTick) * 0.115F;
    }

    public float spinZDegrees(float partialTick) {
        return seedAngle(renderSeed(), 0xC2B2)
                + spinTime(partialTick) * 0.052F;
    }

    private float spinTime(float partialTick) {
        return Math.floorMod(level().getGameTime(), 360_000L) + partialTick;
    }

    public List<Crater> craters() {
        if (cachedCraters != null) {
            return cachedCraters;
        }
        List<Crater> decoded = new ArrayList<>();
        ListTag entries = entityData.get(DATA_CRATERS)
                .getList("Hits", Tag.TAG_COMPOUND);
        for (int index = 0;
                index < entries.size() && decoded.size() < MAX_CRATERS;
                index++) {
            CompoundTag entry = entries.getCompound(index);
            Vector3f normal = new Vector3f(
                    entry.getFloat("X"), entry.getFloat("Y"), entry.getFloat("Z"));
            if (normal.lengthSquared() < 1.0E-6F) {
                continue;
            }
            normal.normalize();
            decoded.add(new Crater(
                    normal.x, normal.y, normal.z,
                    Mth.clamp(entry.getFloat("Radius"), 0.08F, 1.1F),
                    Mth.clamp(entry.getFloat("Depth"), 0.01F, 0.42F)));
        }
        cachedCraters = List.copyOf(decoded);
        return cachedCraters;
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        noPhysics = true;

        if (!level().isClientSide && renderSeed() == 0) {
            entityData.set(DATA_RENDER_SEED, nonZeroSeed(random));
        }
        if (!level().isClientSide) {
            enforceSpecifiedSpeed();
        }

        Vec3 previousCenter = centerPosition();
        Vec3 movement = getDeltaMovement();
        if (movement.lengthSqr() > 0.0D) {
            move(MoverType.SELF, movement);
            setDeltaMovement(movement);
        }
        updateSurfaceParts();

        if (!level().isClientSide) {
            damagePlayersOnImpact(previousCenter, centerPosition(), movement);
            discardDistantNaturalAsteroid();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!isAlive()
                || isRemoved()
                || level().isClientSide
                || !(source.getEntity() instanceof Player)
                || !Float.isFinite(amount)
                || amount <= 0.0F) {
            return false;
        }
        long damageTick = level().getGameTime();
        if (source == lastDamageSource && damageTick == lastDamageTick) {
            return false;
        }
        lastDamageSource = source;
        lastDamageTick = damageTick;

        float preHitSize = sizeFactor();
        Vec3 worldNormal = impactNormal(source);
        addOrMergeCrater(worldToLocalNormal(worldNormal), amount, preHitSize);

        if (AsteroidMechanics.isStrongHit(amount, preHitSize)) {
            strongMotion = true;
            setDeltaMovement(impactImpulseDirection(source, worldNormal)
                    .scale(AsteroidMechanics.STRONG_HIT_SPEED_BLOCKS_PER_TICK));
            hasImpulse = true;
            hurtMarked = true;
        }

        spawnHitEffects(amount, preHitSize, worldNormal);
        float remainingSize = preHitSize - AsteroidMechanics.sizeLost(amount);
        if (remainingSize < AsteroidMechanics.MIN_ACTIVE_SIZE) {
            breakApart();
        } else {
            setSizeFactor(remainingSize);
        }
        return true;
    }

    private void damagePlayersOnImpact(
            Vec3 previousCenter, Vec3 currentCenter, Vec3 movement) {
        double speed = AsteroidMechanics.blocksPerSecond(movement.length());
        if (!AsteroidMechanics.isDamagingImpact(speed)) {
            impactContacts.clear();
            return;
        }

        double radius = radiusBlocks();
        AABB search = new AABB(previousCenter, currentCenter).inflate(radius + 2.0D);
        Set<UUID> currentContacts = new HashSet<>();
        for (Player player : level().getEntitiesOfClass(
                Player.class, search,
                candidate -> candidate.isAlive() && !candidate.isSpectator())) {
            double startDistance = pointToBoundsDistanceSqr(
                    previousCenter, player.getBoundingBox());
            double endDistance = pointToBoundsDistanceSqr(
                    currentCenter, player.getBoundingBox());
            if (segmentToBoundsDistanceSqr(
                    previousCenter, currentCenter, player.getBoundingBox()) > radius * radius) {
                continue;
            }
            currentContacts.add(player.getUUID());
            boolean enteringOrApproaching = startDistance > radius * radius
                    || endDistance + 1.0E-6D < startDistance;
            if (!impactContacts.contains(player.getUUID()) && enteringOrApproaching) {
                player.hurt(asteroidImpactSource(),
                        AsteroidMechanics.collisionDamage(speed));
            }
        }
        impactContacts.retainAll(currentContacts);
        impactContacts.addAll(currentContacts);
    }

    /**
     * Asteroids have two intentional speed states. Reasserting the active state
     * before movement prevents vanilla explosion knockback from accidentally
     * turning an ordinary drifting asteroid into a damaging impact.
     */
    private void enforceSpecifiedSpeed() {
        double targetSpeed = strongMotion
                ? AsteroidMechanics.STRONG_HIT_SPEED_BLOCKS_PER_TICK
                : AsteroidMechanics.DRIFT_SPEED_BLOCKS_PER_TICK;
        Vec3 movement = getDeltaMovement();
        double speedSqr = movement.lengthSqr();
        Vec3 corrected;
        if (!Double.isFinite(speedSqr) || speedSqr < 1.0E-12D) {
            corrected = randomUnit(random).scale(targetSpeed);
        } else {
            double speed = Math.sqrt(speedSqr);
            if (Math.abs(speed - targetSpeed) < 1.0E-9D) {
                return;
            }
            corrected = movement.scale(targetSpeed / speed);
        }
        setDeltaMovement(corrected);
        hasImpulse = true;
        hurtMarked = true;
    }

    /** Exact-enough convex search for a swept sphere against an axis-aligned box. */
    private static double segmentToBoundsDistanceSqr(Vec3 start, Vec3 end, AABB bounds) {
        Vec3 movement = end.subtract(start);
        if (movement.lengthSqr() < 1.0E-12D) {
            return pointToBoundsDistanceSqr(start, bounds);
        }
        double low = 0.0D;
        double high = 1.0D;
        for (int iteration = 0; iteration < 24; iteration++) {
            double first = (low * 2.0D + high) / 3.0D;
            double second = (low + high * 2.0D) / 3.0D;
            double firstDistance = pointToBoundsDistanceSqr(
                    start.add(movement.scale(first)), bounds);
            double secondDistance = pointToBoundsDistanceSqr(
                    start.add(movement.scale(second)), bounds);
            if (firstDistance <= secondDistance) {
                high = second;
            } else {
                low = first;
            }
        }
        double middle = (low + high) * 0.5D;
        return Math.min(
                Math.min(pointToBoundsDistanceSqr(start, bounds),
                        pointToBoundsDistanceSqr(end, bounds)),
                pointToBoundsDistanceSqr(start.add(movement.scale(middle)), bounds));
    }

    private static double pointToBoundsDistanceSqr(Vec3 point, AABB bounds) {
        double dx = Math.max(Math.max(bounds.minX - point.x, 0.0D), point.x - bounds.maxX);
        double dy = Math.max(Math.max(bounds.minY - point.y, 0.0D), point.y - bounds.maxY);
        double dz = Math.max(Math.max(bounds.minZ - point.z, 0.0D), point.z - bounds.maxZ);
        return dx * dx + dy * dy + dz * dz;
    }

    private DamageSource asteroidImpactSource() {
        Holder<DamageType> type = level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(ASTEROID_IMPACT);
        return new DamageSource(type, this);
    }

    private void discardDistantNaturalAsteroid() {
        if (!entityData.get(DATA_NATURAL)
                || tickCount < NATURAL_DESPAWN_GRACE_TICKS
                || tickCount % NATURAL_DESPAWN_CHECK_TICKS != 0) {
            return;
        }
        Player nearest = level().getNearestPlayer(
                getX(), getY(), getZ(), NATURAL_DESPAWN_DISTANCE, false);
        if (nearest == null) {
            discard();
        }
    }

    private Vec3 impactNormal(DamageSource source) {
        if (source.getDirectEntity() instanceof Player player) {
            Vec3 eye = player.getEyePosition();
            Vec3 direction = player.getLookAngle();
            Vec3 offset = eye.subtract(centerPosition());
            double projection = offset.dot(direction);
            double discriminant = projection * projection
                    - (offset.lengthSqr() - radiusBlocks() * radiusBlocks());
            if (discriminant >= 0.0D) {
                double root = Math.sqrt(discriminant);
                double distance = -projection - root;
                if (distance < 0.0D) {
                    distance = -projection + root;
                }
                if (distance >= 0.0D) {
                    Vec3 hitNormal = eye.add(direction.scale(distance))
                            .subtract(centerPosition());
                    if (hitNormal.lengthSqr() > 1.0E-8D) {
                        return hitNormal.normalize();
                    }
                }
            }
            Vec3 eyeNormal = eye.subtract(centerPosition());
            if (eyeNormal.lengthSqr() > 1.0E-8D) {
                return eyeNormal.normalize();
            }
        }
        Vec3 sourcePosition = source.getSourcePosition();
        if (sourcePosition == null && source.getEntity() != null) {
            sourcePosition = source.getEntity().getEyePosition();
        }
        if (sourcePosition == null) {
            Vec3 motion = getDeltaMovement();
            return motion.lengthSqr() > 1.0E-8D
                    ? motion.normalize().scale(-1.0D)
                    : new Vec3(0.0D, 1.0D, 0.0D);
        }
        Vec3 normal = sourcePosition.subtract(centerPosition());
        return normal.lengthSqr() > 1.0E-8D
                ? normal.normalize() : new Vec3(0.0D, 1.0D, 0.0D);
    }

    private Vec3 impactImpulseDirection(DamageSource source, Vec3 outwardNormal) {
        Entity direct = source.getDirectEntity();
        if (direct instanceof Player player
                && player.getLookAngle().lengthSqr() > 1.0E-8D) {
            return player.getLookAngle().normalize();
        }
        if (direct != null
                && direct != source.getEntity()
                && direct.getDeltaMovement().lengthSqr() > 1.0E-8D) {
            return direct.getDeltaMovement().normalize();
        }
        return outwardNormal.scale(-1.0D).normalize();
    }

    private Vec3 worldToLocalNormal(Vec3 worldNormal) {
        Vector3f local = worldNormal.toVector3f();
        local.rotateY(-spinYDegrees(0.0F) * Mth.DEG_TO_RAD);
        local.rotateX(-spinXDegrees(0.0F) * Mth.DEG_TO_RAD);
        local.rotateZ(-spinZDegrees(0.0F) * Mth.DEG_TO_RAD);
        if (local.lengthSquared() < 1.0E-6F) {
            return new Vec3(0.0D, 1.0D, 0.0D);
        }
        local.normalize();
        return new Vec3(local.x, local.y, local.z);
    }

    private void addOrMergeCrater(Vec3 localNormal, float damage, float size) {
        float relativeDamage = damage / Math.max(
                1.0F, AsteroidMechanics.strongHitThreshold(size));
        float strength = Mth.clamp(
                (float) Math.sqrt(relativeDamage), 0.12F, 2.4F);
        Crater incoming = new Crater(
                (float) localNormal.x, (float) localNormal.y,
                (float) localNormal.z,
                Mth.clamp(0.14F + strength * 0.19F, 0.16F, 0.72F),
                Mth.clamp(0.018F + strength * 0.105F, 0.025F, 0.34F));

        List<Crater> updated = new ArrayList<>(craters());
        int mergeIndex = -1;
        float bestDot = -1.0F;
        for (int index = 0; index < updated.size(); index++) {
            Crater existing = updated.get(index);
            float dot = existing.x * incoming.x
                    + existing.y * incoming.y + existing.z * incoming.z;
            if (dot > bestDot && dot >= Mth.cos(Math.max(
                    existing.angularRadius, incoming.angularRadius) * 0.55F)) {
                bestDot = dot;
                mergeIndex = index;
            }
        }

        if (mergeIndex >= 0) {
            Crater existing = updated.get(mergeIndex);
            Vector3f normal = new Vector3f(existing.x, existing.y, existing.z)
                    .mul(existing.depth)
                    .add(incoming.x * incoming.depth,
                            incoming.y * incoming.depth,
                            incoming.z * incoming.depth)
                    .normalize();
            updated.set(mergeIndex, new Crater(
                    normal.x, normal.y, normal.z,
                    Math.max(existing.angularRadius, incoming.angularRadius),
                    Mth.clamp(existing.depth + incoming.depth * 0.62F,
                            0.025F, 0.42F)));
        } else if (updated.size() < MAX_CRATERS) {
            updated.add(incoming);
        } else {
            int shallowest = 0;
            for (int index = 1; index < updated.size(); index++) {
                if (updated.get(index).depth < updated.get(shallowest).depth) {
                    shallowest = index;
                }
            }
            updated.set(shallowest, incoming);
        }
        setCraters(updated);
    }

    private void setCraters(List<Crater> craters) {
        ListTag entries = new ListTag();
        for (Crater crater : craters) {
            CompoundTag entry = new CompoundTag();
            entry.putFloat("X", crater.x);
            entry.putFloat("Y", crater.y);
            entry.putFloat("Z", crater.z);
            entry.putFloat("Radius", crater.angularRadius);
            entry.putFloat("Depth", crater.depth);
            entries.add(entry);
        }
        CompoundTag root = new CompoundTag();
        root.put("Hits", entries);
        cachedCraters = null;
        entityData.set(DATA_CRATERS, root);
    }

    private void spawnHitEffects(float damage, float size, Vec3 worldNormal) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int count = Mth.clamp((int) (6.0F + damage / Math.max(
                1.0F, AsteroidMechanics.strongHitThreshold(size)) * 16.0F), 6, 72);
        Vec3 impactPoint = centerPosition().add(
                worldNormal.scale(radiusBlocks() * 0.96D));
        double spread = Math.min(
                2.5D, Math.max(0.2D, radiusBlocks() * 0.08D));
        serverLevel.sendParticles(new BlockParticleOption(
                        ParticleTypes.BLOCK, Blocks.DEEPSLATE.defaultBlockState()),
                impactPoint.x, impactPoint.y, impactPoint.z, count,
                spread, spread, spread, 0.18D);
        serverLevel.playSound(null, BlockPos.containing(impactPoint),
                SoundEvents.STONE_BREAK, SoundSource.NEUTRAL,
                Math.min(2.0F, 0.7F + size * 0.18F),
                0.72F + random.nextFloat() * 0.18F);
    }

    private void breakApart() {
        if (level() instanceof ServerLevel serverLevel) {
            Vec3 center = centerPosition();
            int count = Mth.clamp((int) (diameterBlocks() * 8.0F), 24, 280);
            serverLevel.sendParticles(new BlockParticleOption(
                            ParticleTypes.BLOCK, Blocks.DEEPSLATE.defaultBlockState()),
                    center.x, center.y, center.z, count,
                    radiusBlocks() * 0.55D, radiusBlocks() * 0.55D,
                    radiusBlocks() * 0.55D, 0.42D);
            serverLevel.sendParticles(
                    ParticleTypes.POOF, center.x, center.y, center.z,
                    Math.min(80, count / 2),
                    radiusBlocks() * 0.35D, radiusBlocks() * 0.35D,
                    radiusBlocks() * 0.35D, 0.12D);
            serverLevel.playSound(null, BlockPos.containing(center),
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL,
                    Math.min(4.0F, 1.0F + sizeFactor() * 0.5F), 0.65F);
        }
        discard();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        float diameter = AsteroidMechanics.diameter(dimensionsSize);
        return EntityDimensions.scalable(diameter, diameter);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return surfaceParts;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
    }

    public boolean isNaturalSpawn() {
        return entityData.get(DATA_NATURAL);
    }

    @Override
    public boolean shouldBeSaved() {
        return !isNaturalSpawn() && super.shouldBeSaved();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("AsteroidSize", sizeFactor());
        tag.putInt("RenderSeed", renderSeed());
        tag.put("Craters", entityData.get(DATA_CRATERS).copy());
        tag.putBoolean("NaturalSpawn", entityData.get(DATA_NATURAL));
        tag.putBoolean("StrongMotion", strongMotion);
        tag.putInt("SpinAge", tickCount);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        dimensionsSize = AsteroidMechanics.clampActiveSize(
                tag.contains("AsteroidSize", Tag.TAG_FLOAT)
                        ? tag.getFloat("AsteroidSize") : 1.0F);
        entityData.set(DATA_SIZE, dimensionsSize);
        refreshDimensions();
        entityData.set(DATA_RENDER_SEED,
                tag.contains("RenderSeed", Tag.TAG_INT)
                        ? tag.getInt("RenderSeed") : 1);
        entityData.set(DATA_CRATERS,
                tag.contains("Craters", Tag.TAG_COMPOUND)
                        ? tag.getCompound("Craters").copy() : new CompoundTag());
        entityData.set(DATA_NATURAL, tag.getBoolean("NaturalSpawn"));
        strongMotion = tag.getBoolean("StrongMotion");
        tickCount = Math.max(0, tag.getInt("SpinAge"));
        setNoGravity(true);
        noPhysics = true;
        updateSurfaceParts();
    }

    private void updateSurfaceParts() {
        if (surfaceParts == null) {
            return;
        }
        Vec3 center = centerPosition();
        double shellRadius = radiusBlocks() * 0.75D;
        for (int index = 0; index < surfaceParts.length; index++) {
            Vec3 direction = SURFACE_DIRECTIONS[index];
            double axisSum = Math.abs(direction.x)
                    + Math.abs(direction.y) + Math.abs(direction.z);
            float partSize = Math.max(0.01F,
                    (float) (radiusBlocks() * 0.40D / axisSum));
            surfaceParts[index].resize(partSize, partSize);
            surfaceParts[index].moveCenterTo(center.add(direction.scale(shellRadius)));
        }
    }

    private static Vec3[] createSurfaceDirections() {
        List<Vec3> directions = new ArrayList<>(98);
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    if ((x != 0 || y != 0 || z != 0)
                            && gcd(gcd(Math.abs(x), Math.abs(y)), Math.abs(z)) == 1) {
                        directions.add(new Vec3(x, y, z).normalize());
                    }
                }
            }
        }
        return directions.toArray(Vec3[]::new);
    }

    private static int gcd(int first, int second) {
        while (second != 0) {
            int remainder = first % second;
            first = second;
            second = remainder;
        }
        return first;
    }

    private static Vec3 randomUnit(RandomSource random) {
        for (int attempt = 0; attempt < 8; attempt++) {
            Vec3 direction = new Vec3(
                    random.nextDouble() * 2.0D - 1.0D,
                    random.nextDouble() * 2.0D - 1.0D,
                    random.nextDouble() * 2.0D - 1.0D);
            if (direction.lengthSqr() > 1.0E-5D) {
                return direction.normalize();
            }
        }
        return new Vec3(1.0D, 0.0D, 0.0D);
    }

    private static int nonZeroSeed(RandomSource random) {
        int seed = random.nextInt();
        return seed == 0 ? 1 : seed;
    }

    private static float seedAngle(int seed, int salt) {
        int mixed = seed ^ salt;
        mixed ^= mixed >>> 16;
        mixed *= 0x7FEB352D;
        mixed ^= mixed >>> 15;
        return Math.floorMod(mixed, 3600) * 0.1F;
    }

    public record Crater(
            float x, float y, float z, float angularRadius, float depth) {
    }
}
