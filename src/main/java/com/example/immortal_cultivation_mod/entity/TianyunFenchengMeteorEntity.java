package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;

import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.magicritual.center.ForcefieldInterceptable;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.ForcefieldCenterBehavior;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import com.example.immortal_cultivation_mod.spell.SpellCharge;
import com.example.immortal_cultivation_mod.spell.SpellDamageHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Real, forcefield-aware meteor used by 天陨焚城. */
public final class TianyunFenchengMeteorEntity extends Projectile
        implements ForcefieldInterceptable {
    public static final int NORMAL_IMPACT_TICK = 50;
    public static final int FULL_IMPACT_TICK = 60;
    public static final int NORMAL_WARNING_START = 10;
    public static final int NORMAL_LINGER_TICKS = 120;
    public static final int FULL_LINGER_TICKS = 220;

    private static final EntityDataAccessor<Float> DATA_TARGET_Y =
            SynchedEntityData.defineId(
                    TianyunFenchengMeteorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_CHARGE =
            SynchedEntityData.defineId(
                    TianyunFenchengMeteorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_IMPACTED =
            SynchedEntityData.defineId(
                    TianyunFenchengMeteorEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_IMPACT_TICK =
            SynchedEntityData.defineId(
                    TianyunFenchengMeteorEntity.class, EntityDataSerializers.INT);

    private static final DustParticleOptions RED_ORANGE_DUST =
            new DustParticleOptions(new Vector3f(1.0F, 0.13F, 0.015F), 1.8F);
    private static final DustParticleOptions WHITE_CORE_DUST =
            new DustParticleOptions(new Vector3f(1.0F, 0.91F, 0.60F), 2.4F);
    private static final ExplosionDamageCalculator TERRAIN_ONLY = new ExplosionDamageCalculator() {
        @Override
        public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
            return false;
        }

        @Override
        public float getKnockbackMultiplier(Entity entity) {
            return 0.0F;
        }

        @Override
        public boolean shouldBlockExplode(
                Explosion explosion, BlockGetter level, BlockPos pos,
                BlockState state, float power) {
            return super.shouldBlockExplode(explosion, level, pos, state, power);
        }
    };

    private double startY;

    public TianyunFenchengMeteorEntity(
            EntityType<? extends TianyunFenchengMeteorEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
        noCulling = true;
    }

    public TianyunFenchengMeteorEntity(
            ServerLevel level, LivingEntity owner, Vec3 target, float chargeScale) {
        this(ModEntities.TIANYUN_FENCHENG_METEOR.get(), level);
        setOwner(owner);
        setChargeScale(chargeScale);
        setTargetY(target.y);
        startY = target.y + (fullyCharged() ? 44.0D : 36.0D);
        setPos(target.x, startY, target.z);
        NpcProfile.markNpcOwnedEffect(this, owner);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_TARGET_Y, 0.0F);
        builder.define(DATA_CHARGE, SpellCharge.NORMAL);
        builder.define(DATA_IMPACTED, false);
        builder.define(DATA_IMPACT_TICK, -1);
    }

    public float chargeScale() {
        return SpellCharge.clamp(entityData.get(DATA_CHARGE));
    }

    public void setChargeScale(float chargeScale) {
        entityData.set(DATA_CHARGE, SpellCharge.clamp(chargeScale));
    }

    public boolean fullyCharged() {
        return SpellCharge.isFull(chargeScale());
    }

    public double targetY() {
        return entityData.get(DATA_TARGET_Y);
    }

    private void setTargetY(double targetY) {
        entityData.set(DATA_TARGET_Y, (float) targetY);
    }

    public boolean impacted() {
        return entityData.get(DATA_IMPACTED);
    }

    public int impactAge() {
        int impactTick = entityData.get(DATA_IMPACT_TICK);
        return impactTick < 0 ? 0 : Math.max(0, tickCount - impactTick);
    }

    public int impactTick() {
        return fullyCharged() ? FULL_IMPACT_TICK : NORMAL_IMPACT_TICK;
    }

    public int warningStartTick() {
        return fullyCharged() ? 0 : NORMAL_WARNING_START;
    }

    public float blastRadius() {
        return fullyCharged() ? 26.0F : 20.0F;
    }

    public float meteorRadius() {
        return fullyCharged() ? 3.6F : 2.8F;
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        noPhysics = true;
        if (startY == 0.0D) {
            startY = targetY() + (fullyCharged() ? 44.0D : 36.0D);
        }

        Entity owner = getOwner();
        if (!level().isClientSide) {
            NpcProfile.markNpcOwnedEffect(this, owner);
            if (NpcProfile.isOrphanedNpcEffect(this, owner)) {
                discard();
                return;
            }
        }

        if (impacted()) {
            setDeltaMovement(Vec3.ZERO);
            tickLingeringEffects();
            if (!level().isClientSide
                    && impactAge() >= (fullyCharged() ? FULL_LINGER_TICKS : NORMAL_LINGER_TICKS)) {
                discard();
            }
            return;
        }

        if (level().isClientSide) {
            spawnClientApproachVfx();
            return;
        }

        int fallStart = warningStartTick();
        if (tickCount < fallStart) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }

        double progress = Mth.clamp(
                (tickCount - fallStart + 1.0D) / Math.max(1.0D, impactTick() - fallStart),
                0.0D, 1.0D);
        double eased = progress * progress * (3.0D - 2.0D * progress);
        double destinationY = targetY() + meteorRadius();
        Vec3 current = position();
        Vec3 desired = new Vec3(getX(), Mth.lerp(eased, startY, destinationY), getZ());
        Vec3 movement = desired.subtract(current);
        setDeltaMovement(movement);

        if (ForcefieldCenterBehavior.interceptProjectileImpact(this, desired)) {
            discard();
            return;
        }

        BlockHitResult blockHit = level().clip(new ClipContext(
                current, desired, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            impact(blockHit.getLocation());
            return;
        }

        setPos(desired.x, desired.y, desired.z);
        hasImpulse = true;
        hurtMarked = true;
        if (tickCount >= impactTick()) {
            impact(new Vec3(getX(), targetY(), getZ()));
        }
    }

    private void impact(Vec3 center) {
        if (impacted() || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Entity ownerEntity = getOwner();
        if (!(ownerEntity instanceof LivingEntity owner)
                || NpcProfile.isOrphanedNpcEffect(this, owner)) {
            discard();
            return;
        }

        entityData.set(DATA_IMPACTED, true);
        entityData.set(DATA_IMPACT_TICK, tickCount);
        setPos(center.x, center.y, center.z);
        setDeltaMovement(Vec3.ZERO);

        damageAndLaunch(serverLevel, owner, center);
        float explosionPower = fullyCharged() ? 24.0F : 18.0F;
        com.example.immortal_cultivation_mod.spell.SpellDestruction.explode(serverLevel,owner, null, TERRAIN_ONLY, center,
                explosionPower, true, Level.ExplosionInteraction.BLOCK);
        igniteCrater(serverLevel, center, fullyCharged() ? 110 : 70);
        spawnImpactVfx(serverLevel, center);
        serverLevel.playSound(null, BlockPos.containing(center),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS,
                fullyCharged() ? 4.0F : 3.2F, fullyCharged() ? 0.52F : 0.62F);
        serverLevel.playSound(null, BlockPos.containing(center),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS,
                2.4F, 0.48F);
    }

    private void damageAndLaunch(ServerLevel level, LivingEntity owner, Vec3 center) {
        float radius = blastRadius();
        float centerRadius = fullyCharged() ? 4.0F : 3.0F;
        float scale = chargeScale();
        AABB area = new AABB(center, center).inflate(radius, 18.0D, radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                candidate -> candidate != owner && candidate.isAlive()
                        && NpcProfile.isCombatEnemy(owner, candidate))) {
            double horizontalSqr = horizontalDistanceSqr(target.position(), center);
            if (horizontalSqr > radius * radius) {
                continue;
            }
            boolean central = horizontalSqr <= centerRadius * centerRadius;
            float baseDamage = (central ? 1600.0F : 700.0F) * scale;
            float damage = SpellDamageHelper.damage(owner, baseDamage,
                    ModSpells.get(ModSpells.TIANYUN_FENCHENG));
            target.hurt(damageSources().indirectMagic(this, owner), damage);
            target.igniteForSeconds(fullyCharged() ? 12.0F : 8.0F);

            Vec3 radial = new Vec3(
                    target.getX() - center.x, 0.0D, target.getZ() - center.z);
            radial = radial.lengthSqr() < 1.0E-5D
                    ? new Vec3(1.0D, 0.0D, 0.0D) : radial.normalize();
            double strength = (central ? 2.7D : 1.75D) * (fullyCharged() ? 1.3D : 1.0D);
            boolean airborne = !target.onGround() || target.getY() > center.y + 2.0D;
            double vertical = airborne ? (fullyCharged() ? -3.0D : -2.2D)
                    : (central ? 1.15D : 0.65D);
            target.setDeltaMovement(target.getDeltaMovement().scale(0.25D)
                    .add(radial.scale(strength)).add(0.0D, vertical, 0.0D));
            target.hasImpulse = true;
            target.hurtMarked = true;
            if (central) {
                target.addEffect(new MobEffectInstance(
                        ModEffects.DAZE, fullyCharged() ? 50 : 34,
                        0, false, true, true));
            }
        }
    }

    private void spawnImpactVfx(ServerLevel level, Vec3 center) {
        float radius = blastRadius();
        level.sendParticles(ParticleTypes.FLASH,
                center.x, center.y + 1.0D, center.z,
                fullyCharged() ? 7 : 5, 1.2D, 1.2D, 1.2D, 0.0D);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                center.x, center.y + 0.5D, center.z,
                fullyCharged() ? 6 : 4, 2.0D, 1.0D, 2.0D, 0.0D);
        level.sendParticles(WHITE_CORE_DUST,
                center.x, center.y + 0.8D, center.z,
                150, 2.4D, 2.4D, 2.4D, 0.22D);
        level.sendParticles(ModParticles.METHOD_QI_FIRE.get(),
                center.x, center.y + 0.6D, center.z,
                fullyCharged() ? 440 : 320,
                radius * 0.45D, 3.4D, radius * 0.45D, 0.34D);
        level.sendParticles(ModParticles.METHOD_QI_EARTH.get(),
                center.x, center.y + 0.3D, center.z,
                fullyCharged() ? 400 : 300,
                radius * 0.55D, 2.4D, radius * 0.55D, 0.30D);
        level.sendParticles(new BlockParticleOption(
                        ParticleTypes.BLOCK, Blocks.MAGMA_BLOCK.defaultBlockState()),
                center.x, center.y + 1.2D, center.z,
                fullyCharged() ? 520 : 380, radius * 0.52D, 4.8D,
                radius * 0.52D, 0.55D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                center.x, center.y + 2.0D, center.z,
                fullyCharged() ? 260 : 200,
                radius * 0.5D, 5.4D, radius * 0.5D, 0.13D);
        SpellImpactParticles.shockRing(level, center.add(0.0D, 0.2D, 0.0D),
                ModParticles.METHOD_QI_FIRE.get(), radius,
                fullyCharged() ? 132 : 104, 0.48D);
        SpellImpactParticles.shockRing(level, center.add(0.0D, 0.1D, 0.0D),
                ParticleTypes.CLOUD, radius,
                fullyCharged() ? 148 : 116, 0.58D);
    }

    private void tickLingeringEffects() {
        if (level().isClientSide) {
            spawnClientLingeringVfx();
            return;
        }
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (impactAge() % 8 == 0) {
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                    getX(), getY() + 1.0D, getZ(),
                    fullyCharged() ? 10 : 7,
                    blastRadius() * 0.28D, 1.0D, blastRadius() * 0.28D, 0.035D);
        }
        if (impactAge() % 20 == 0) {
            igniteCrater(serverLevel, position(), fullyCharged() ? 24 : 15);
        }
    }

    private void igniteCrater(ServerLevel level, Vec3 center, int attempts) {
        if (!BlockBreakingSettings.enabled(getOwner(),
                BlockBreakingSettings.Category.SPELLS)) return;
        double radius = blastRadius();
        for (int attempt = 0; attempt < attempts; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = Math.sqrt(random.nextDouble()) * radius;
            int x = Mth.floor(center.x + Math.cos(angle) * distance);
            int z = Mth.floor(center.z + Math.sin(angle) * distance);
            BlockPos cursor = new BlockPos(x, Mth.floor(center.y) + 5, z);
            int floor = Math.max(level.getMinBuildHeight(), Mth.floor(center.y) - 12);
            while (cursor.getY() > floor
                    && level.getBlockState(cursor).getCollisionShape(level, cursor).isEmpty()) {
                cursor = cursor.below();
            }
            BlockPos firePos = cursor.above();
            if (level.getBlockState(firePos).isAir()
                    && Blocks.FIRE.defaultBlockState().canSurvive(level, firePos)) {
                level.setBlock(firePos, Blocks.FIRE.defaultBlockState(), 3);
            }
        }
    }

    private void spawnClientApproachVfx() {
        if (tickCount >= warningStartTick()) {
            double warningRadius = blastRadius();
            int warningParticles = fullyCharged() ? 18 : 14;
            for (int index = 0; index < warningParticles; index++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double distance = Math.sqrt(random.nextDouble()) * warningRadius;
                level().addParticle(index % 3 == 0 ? RED_ORANGE_DUST
                                : ModParticles.METHOD_QI_FIRE.get(),
                        getX() + Math.cos(angle) * distance,
                        targetY() + 0.08D,
                        getZ() + Math.sin(angle) * distance,
                        0.0D, 0.02D, 0.0D);
            }
            for (int index = 0; index < 4; index++) {
                level().addParticle(new BlockParticleOption(
                                ParticleTypes.FALLING_DUST,
                                index % 2 == 0 ? Blocks.MAGMA_BLOCK.defaultBlockState()
                                        : Blocks.BLACKSTONE.defaultBlockState()),
                        getX() + random.nextGaussian() * blastRadius() * 0.45D,
                        targetY() + 12.0D + random.nextDouble() * 12.0D,
                        getZ() + random.nextGaussian() * blastRadius() * 0.45D,
                        0.0D, -0.35D, 0.0D);
            }
        }

        float meteorRadius = meteorRadius();
        for (int index = 0; index < 16; index++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double y = (random.nextDouble() * 2.0D - 1.0D) * meteorRadius;
            double ring = Math.sqrt(Math.max(0.0D,
                    meteorRadius * meteorRadius - y * y));
            level().addParticle(index % 3 == 0 ? ParticleTypes.FLAME
                            : ModParticles.METHOD_QI_FIRE.get(),
                    getX() + Math.cos(angle) * ring,
                    getY() + y,
                    getZ() + Math.sin(angle) * ring,
                    0.0D, 0.04D, 0.0D);
        }
        level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                getX() + random.nextGaussian() * 0.8D,
                getY() + meteorRadius,
                getZ() + random.nextGaussian() * 0.8D,
                0.0D, 0.28D, 0.0D);
    }

    private void spawnClientLingeringVfx() {
        if ((tickCount & 1) == 0) {
            level().addParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                    getX() + random.nextGaussian() * blastRadius() * 0.2D,
                    getY() + 0.4D + random.nextDouble() * 2.0D,
                    getZ() + random.nextGaussian() * blastRadius() * 0.2D,
                    random.nextGaussian() * 0.015D, 0.11D, random.nextGaussian() * 0.015D);
        }
        for (int index = 0; index < 3; index++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = random.nextDouble() * blastRadius();
            level().addParticle(index == 0 ? RED_ORANGE_DUST
                            : ModParticles.METHOD_QI_FIRE.get(),
                    getX() + Math.cos(angle) * distance,
                    getY() + 0.12D,
                    getZ() + Math.sin(angle) * distance,
                    0.0D, 0.012D, 0.0D);
        }
    }

    private static double horizontalDistanceSqr(Vec3 first, Vec3 second) {
        double x = first.x - second.x;
        double z = first.z - second.z;
        return x * x + z * z;
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
        return fullyCharged() ? 3200.0F : 1600.0F;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
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

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("TargetY", (float) targetY());
        tag.putFloat("Charge", chargeScale());
        tag.putBoolean("Impacted", impacted());
        tag.putInt("ImpactTick", entityData.get(DATA_IMPACT_TICK));
        tag.putInt("Age", tickCount);
        tag.putDouble("StartY", startY);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setTargetY(tag.getFloat("TargetY"));
        setChargeScale(tag.getFloat("Charge"));
        entityData.set(DATA_IMPACTED, tag.getBoolean("Impacted"));
        entityData.set(DATA_IMPACT_TICK, tag.getInt("ImpactTick"));
        tickCount = tag.getInt("Age");
        startY = tag.contains("StartY") ? tag.getDouble("StartY")
                : targetY() + (fullyCharged() ? 44.0D : 36.0D);
    }
}
