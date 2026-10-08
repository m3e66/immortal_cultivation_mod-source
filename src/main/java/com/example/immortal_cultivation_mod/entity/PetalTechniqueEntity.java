package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.magicritual.center.ForcefieldInterceptable;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.ForcefieldCenterBehavior;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import com.example.immortal_cultivation_mod.spell.SpellCharge;
import com.example.immortal_cultivation_mod.spell.SpellDamageHelper;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Real petal cluster and persistent flower field shared by the wood/wind spells. */
public final class PetalTechniqueEntity extends Projectile implements ForcefieldInterceptable {
    public enum Kind {
        LUOHUA_CHANSHEN,
        BAIHUA_ZANG;

        static Kind byId(int id) {
            Kind[] values = values();
            return values[Math.floorMod(id, values.length)];
        }
    }

    private static final EntityDataAccessor<Integer> DATA_KIND =
            SynchedEntityData.defineId(PetalTechniqueEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_CHARGE =
            SynchedEntityData.defineId(PetalTechniqueEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Optional<UUID>> DATA_TARGET =
            SynchedEntityData.defineId(
                    PetalTechniqueEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DATA_ORBIT_START =
            SynchedEntityData.defineId(PetalTechniqueEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LAST_WAVE =
            SynchedEntityData.defineId(PetalTechniqueEntity.class, EntityDataSerializers.INT);

    private static final DustParticleOptions PINK =
            new DustParticleOptions(new Vector3f(1.0F, 0.48F, 0.72F), 1.15F);
    private static final DustParticleOptions PALE_PINK =
            new DustParticleOptions(new Vector3f(1.0F, 0.84F, 0.91F), 0.92F);
    private static final DustParticleOptions PALE_PURPLE =
            new DustParticleOptions(new Vector3f(0.78F, 0.56F, 1.0F), 0.92F);
    private static final DustParticleOptions WHITE =
            new DustParticleOptions(new Vector3f(1.0F, 1.0F, 0.94F), 0.88F);

    private double launchX;
    private double launchY;
    private double launchZ;
    private double orbitAnchorX;
    private double orbitAnchorY;
    private double orbitAnchorZ;

    public PetalTechniqueEntity(
            EntityType<? extends PetalTechniqueEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
        noCulling = true;
    }

    public static PetalTechniqueEntity createLuohua(
            ServerLevel level, LivingEntity owner, LivingEntity target, float chargeScale) {
        PetalTechniqueEntity petals = ModEntities.PETAL_TECHNIQUE.get().create(level);
        if (petals == null) {
            return null;
        }
        petals.setOwner(owner);
        petals.setKind(Kind.LUOHUA_CHANSHEN);
        petals.setChargeScale(chargeScale);
        petals.setTarget(target);
        Vec3 right = owner.getLookAngle().cross(new Vec3(0.0D, 1.0D, 0.0D));
        right = right.lengthSqr() < 1.0E-6D
                ? new Vec3(1.0D, 0.0D, 0.0D) : right.normalize();
        Vec3 start = owner.getEyePosition().add(owner.getLookAngle().scale(0.55D))
                .add(right.scale(0.32D)).add(0.0D, -0.28D, 0.0D);
        petals.setPos(start.x, start.y, start.z);
        petals.setLaunchOrigin(start);
        NpcProfile.markNpcOwnedEffect(petals, owner);
        return petals;
    }

    public static PetalTechniqueEntity createBaihua(
            ServerLevel level, LivingEntity owner, Vec3 center, float chargeScale) {
        PetalTechniqueEntity field = ModEntities.PETAL_TECHNIQUE.get().create(level);
        if (field == null) {
            return null;
        }
        field.setOwner(owner);
        field.setKind(Kind.BAIHUA_ZANG);
        field.setChargeScale(chargeScale);
        field.setPos(center.x, center.y, center.z);
        field.setLaunchOrigin(center);
        NpcProfile.markNpcOwnedEffect(field, owner);
        return field;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, Kind.LUOHUA_CHANSHEN.ordinal());
        builder.define(DATA_CHARGE, SpellCharge.NORMAL);
        builder.define(DATA_TARGET, Optional.empty());
        builder.define(DATA_ORBIT_START, -1);
        builder.define(DATA_LAST_WAVE, 0);
    }

    public Kind kind() {
        return Kind.byId(entityData.get(DATA_KIND));
    }

    private void setKind(Kind kind) {
        entityData.set(DATA_KIND, (kind == null ? Kind.LUOHUA_CHANSHEN : kind).ordinal());
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

    public boolean orbiting() {
        return entityData.get(DATA_ORBIT_START) >= 0;
    }

    public int effectAge() {
        int start = entityData.get(DATA_ORBIT_START);
        return kind() == Kind.LUOHUA_CHANSHEN && start >= 0
                ? Math.max(0, tickCount - start) : tickCount;
    }

    public int lastWave() {
        return entityData.get(DATA_LAST_WAVE);
    }

    public float fieldRadius() {
        return 6.0F;
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        noPhysics = true;
        Entity ownerEntity = getOwner();
        if (!level().isClientSide) {
            NpcProfile.markNpcOwnedEffect(this, ownerEntity);
            if (!(ownerEntity instanceof LivingEntity owner)
                    || NpcProfile.isOrphanedNpcEffect(this, owner)) {
                discard();
                return;
            }
            if (kind() == Kind.LUOHUA_CHANSHEN) {
                tickLuohuaServer((ServerLevel) level(), owner);
            } else {
                tickBaihuaServer((ServerLevel) level(), owner);
            }
        } else {
            spawnClientVfx();
        }
    }

    private void tickLuohuaServer(ServerLevel level, LivingEntity owner) {
        LivingEntity target = resolveTarget(level);
        if (target == null || !target.isAlive()) {
            scatterAndDiscard(level);
            return;
        }

        if (!orbiting()) {
            if (tickCount < 8) {
                followCasterPalm(owner);
                setDeltaMovement(Vec3.ZERO);
                if (tickCount == 1) {
                    level.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                            SoundSource.PLAYERS, 0.65F, 1.62F);
                }
                return;
            }

            Vec3 targetCenter = target.getBoundingBox().getCenter();
            Vec3 desiredDirection = targetCenter.subtract(position());
            if (desiredDirection.lengthSqr() < 1.45D * 1.45D) {
                beginOrbit(target);
                return;
            }
            desiredDirection = desiredDirection.normalize();
            Vec3 currentDirection = getDeltaMovement().lengthSqr() < 1.0E-6D
                    ? desiredDirection : getDeltaMovement().normalize();
            Vec3 curvedDirection = currentDirection.scale(0.78D)
                    .add(desiredDirection.scale(0.22D)).normalize();
            double speed = 1.28D;
            Vec3 movement = curvedDirection.scale(speed);
            Vec3 desired = position().add(movement);
            setDeltaMovement(movement);

            if (ForcefieldCenterBehavior.interceptProjectileImpact(this, desired)) {
                discard();
                return;
            }
            BlockHitResult obstruction = level.clip(new ClipContext(
                    position(), desired, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, this));
            if (obstruction.getType() == HitResult.Type.BLOCK) {
                scatterAndDiscard(level);
                return;
            }
            setPos(desired.x, desired.y, desired.z);
            hasImpulse = true;
            if (distanceFromLaunchSqr() > 14.0D * 14.0D || tickCount > 48) {
                scatterAndDiscard(level);
            }
            return;
        }

        if (target.position().distanceToSqr(
                orbitAnchorX, orbitAnchorY, orbitAnchorZ) > 6.0D * 6.0D) {
            scatterAndDiscard(level);
            return;
        }
        Vec3 targetCenter = target.getBoundingBox().getCenter();
        setPos(targetCenter.x, targetCenter.y, targetCenter.z);
        setDeltaMovement(Vec3.ZERO);
        target.addEffect(new MobEffectInstance(
                ModEffects.TECHNIQUE_SLOW, 40, 19, false, true, true));

        int age = effectAge();
        int nextWave = switch (age) {
            case 3 -> 1;
            case 11 -> 2;
            case 19 -> 3;
            case 31 -> 4;
            default -> 0;
        };
        if (nextWave > entityData.get(DATA_LAST_WAVE)) {
            entityData.set(DATA_LAST_WAVE, nextWave);
            damageWave(level, owner, target, 25.0F, ModSpells.LUOHUA_CHANSHEN);
            emitLuohuaWave(level, target, nextWave);
            if (nextWave == 4) {
                Vec3 away = horizontal(target.position().subtract(owner.position()));
                if (away.lengthSqr() > 1.0E-6D) {
                    away = away.normalize();
                    target.push(away.x * 0.48D, 0.08D, away.z * 0.48D);
                    target.hurtMarked = true;
                }
            }
        }
        if (age >= 40) {
            scatterAndDiscard(level);
        }
    }

    private void tickBaihuaServer(ServerLevel level, LivingEntity owner) {
        if (tickCount == 1) {
            level.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 1.1F, 1.32F);
        }
        if (tickCount == 24) {
            SpellImpactParticles.shockRing(level, position().add(0.0D, 0.15D, 0.0D),
                    ModParticles.METHOD_QI_WIND.get(), 5.5D, 72, 0.20D);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.WIND_CHARGE_BURST,
                    SoundSource.PLAYERS, 1.4F, 0.82F);
        }

        if (tickCount >= 24 && tickCount <= 100) {
            for (LivingEntity target : flowerFieldTargets(level, owner)) {
                Vec3 source = position().add(0.0D, 0.55D, 0.0D);
                if (!hasClearPath(level, source, target.getBoundingBox().getCenter())
                        || ForcefieldCenterBehavior.interceptAttackPath(
                        owner, source, target.getBoundingBox().getCenter(), 1.0F)) {
                    continue;
                }
                target.addEffect(new MobEffectInstance(
                        ModEffects.TECHNIQUE_SLOW, 10, 29, false, false, true));
                Vec3 pull = horizontal(position().subtract(target.position()));
                if (pull.lengthSqr() > 0.16D) {
                    pull = pull.normalize().scale(0.055D);
                    target.push(pull.x, 0.0D, pull.z);
                    target.hurtMarked = true;
                }
            }
        }

        int wave = baihuaWaveAt(tickCount);
        if (wave > entityData.get(DATA_LAST_WAVE)) {
            entityData.set(DATA_LAST_WAVE, wave);
            for (LivingEntity target : flowerFieldTargets(level, owner)) {
                damageWave(level, owner, target, 80.0F, ModSpells.BAIHUA_ZANG);
            }
            emitBaihuaWave(level, wave);
        }
        if (tickCount >= 140) {
            discard();
        }
    }

    private static int baihuaWaveAt(int age) {
        return switch (age) {
            case 30 -> 1;
            case 40 -> 2;
            case 50 -> 3;
            case 60 -> 4;
            case 70 -> 5;
            case 80 -> 6;
            case 90 -> 7;
            case 100 -> 8;
            default -> 0;
        };
    }

    private void damageWave(
            ServerLevel level, LivingEntity owner, LivingEntity target,
            float baseDamage, String spellId) {
        Vec3 source = position().add(0.0D, 0.5D, 0.0D);
        Vec3 destination = target.getBoundingBox().getCenter();
        if (!hasClearPath(level, source, destination)
                || ForcefieldCenterBehavior.interceptAttackPath(
                owner, source, destination, baseDamage)) {
            return;
        }
        int priorInvulnerability = target.invulnerableTime;
        target.invulnerableTime = 0;
        float damage = SpellDamageHelper.damage(owner,
                SpellCharge.damage(baseDamage, chargeScale()), ModSpells.get(spellId));
        if (!target.hurt(damageSources().indirectMagic(this, owner), damage)) {
            target.invulnerableTime = priorInvulnerability;
        }
    }

    private java.util.List<LivingEntity> flowerFieldTargets(
            ServerLevel level, LivingEntity owner) {
        double radius = fieldRadius();
        AABB bounds = new AABB(position(), position()).inflate(radius, 4.0D, radius);
        return level.getEntitiesOfClass(LivingEntity.class, bounds,
                        target -> target != owner && target.isAlive()
                                && NpcProfile.isCombatEnemy(owner, target))
                .stream().filter(target -> horizontal(target.position().subtract(position()))
                        .lengthSqr() <= radius * radius).toList();
    }

    private void beginOrbit(LivingEntity target) {
        entityData.set(DATA_ORBIT_START, tickCount);
        Vec3 center = target.getBoundingBox().getCenter();
        orbitAnchorX = center.x;
        orbitAnchorY = target.getY();
        orbitAnchorZ = center.z;
        setPos(center.x, center.y, center.z);
        setDeltaMovement(Vec3.ZERO);
    }

    private void followCasterPalm(LivingEntity owner) {
        Vec3 right = owner.getLookAngle().cross(new Vec3(0.0D, 1.0D, 0.0D));
        right = right.lengthSqr() < 1.0E-6D
                ? new Vec3(1.0D, 0.0D, 0.0D) : right.normalize();
        Vec3 palm = owner.getEyePosition().add(owner.getLookAngle().scale(0.55D))
                .add(right.scale(0.32D)).add(0.0D, -0.28D, 0.0D);
        setPos(palm.x, palm.y, palm.z);
        setLaunchOrigin(palm);
    }

    private void emitLuohuaWave(ServerLevel level, LivingEntity target, int wave) {
        Vec3 center = target.getBoundingBox().getCenter();
        level.sendParticles(wave == 4 ? ParticleTypes.FLASH : PINK,
                center.x, center.y, center.z,
                wave == 4 ? 1 : 22, 0.55D, 0.75D, 0.55D, wave == 4 ? 0.0D : 0.12D);
        level.sendParticles(ModParticles.METHOD_QI_WIND.get(),
                center.x, center.y, center.z,
                18, 0.65D, 0.55D, 0.65D, 0.13D);
        level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, wave == 4 ? 1.2F : 0.72F,
                1.35F + wave * 0.09F);
    }

    private void emitBaihuaWave(ServerLevel level, int wave) {
        double radius = fieldRadius();
        level.sendParticles(wave == 8 ? ParticleTypes.FLASH : PINK,
                getX(), getY() + 1.2D, getZ(),
                wave == 8 ? 3 : 70,
                radius * 0.65D, 1.7D, radius * 0.65D,
                wave == 8 ? 0.0D : 0.18D);
        SpellImpactParticles.shockRing(level, position().add(0.0D, 0.35D, 0.0D),
                wave == 8 ? ModParticles.METHOD_QI_WOOD.get() : ModParticles.METHOD_QI_WIND.get(),
                radius, wave == 8 ? 96 : 56, wave == 8 ? 0.40D : 0.23D);
        level.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, wave == 8 ? 1.7F : 0.9F,
                wave == 8 ? 0.72F : 1.25F + wave * 0.035F);
    }

    private void scatterAndDiscard(ServerLevel level) {
        level.sendParticles(PALE_PINK, getX(), getY(), getZ(),
                kind() == Kind.BAIHUA_ZANG ? 120 : 34,
                kind() == Kind.BAIHUA_ZANG ? 4.0D : 0.8D,
                kind() == Kind.BAIHUA_ZANG ? 2.0D : 0.8D,
                kind() == Kind.BAIHUA_ZANG ? 4.0D : 0.8D, 0.08D);
        discard();
    }

    private void spawnClientVfx() {
        if (kind() == Kind.LUOHUA_CHANSHEN) {
            int count = orbiting() ? 5 : 3;
            for (int i = 0; i < count; i++) {
                double phase = tickCount * 0.31D + i * Math.PI * 2.0D / count;
                double radius = orbiting() ? 0.85D : 0.28D;
                level().addParticle(i % 2 == 0 ? PINK : PALE_PINK,
                        getX() + Math.cos(phase) * radius,
                        getY() + Math.sin(phase * 1.7D) * (orbiting() ? 0.75D : 0.24D),
                        getZ() + Math.sin(phase) * radius,
                        0.0D, 0.012D, 0.0D);
            }
            level().addParticle(ModParticles.METHOD_QI_WIND.get(),
                    getX(), getY(), getZ(), 0.0D, 0.015D, 0.0D);
            return;
        }

        int age = effectAge();
        int count = age < 24 ? 5 : age <= 100 ? 12 : 6;
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = Math.sqrt(random.nextDouble()) * fieldRadius();
            double y = age < 24 ? 0.12D + random.nextDouble() * 0.65D
                    : age <= 100 ? 0.2D + random.nextDouble() * 4.2D
                    : 2.0D + random.nextDouble() * 4.0D;
            var particle = switch (i % 4) {
                case 0 -> PINK;
                case 1 -> PALE_PINK;
                case 2 -> PALE_PURPLE;
                default -> WHITE;
            };
            level().addParticle(particle,
                    getX() + Math.cos(angle) * radius,
                    getY() + y,
                    getZ() + Math.sin(angle) * radius,
                    -Math.sin(angle) * 0.018D,
                    age > 100 ? -0.035D : 0.012D,
                    Math.cos(angle) * 0.018D);
        }
        if ((tickCount & 1) == 0) {
            level().addParticle(ModParticles.METHOD_QI_WOOD.get(),
                    getX() + random.nextGaussian() * 2.5D,
                    getY() + 0.08D,
                    getZ() + random.nextGaussian() * 2.5D,
                    0.0D, 0.018D, 0.0D);
        }
    }

    private LivingEntity resolveTarget(ServerLevel level) {
        Optional<UUID> target = entityData.get(DATA_TARGET);
        return target.isPresent() && level.getEntity(target.get()) instanceof LivingEntity living
                ? living : null;
    }

    private boolean hasClearPath(ServerLevel level, Vec3 start, Vec3 end) {
        BlockHitResult hit = level.clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.MISS
                || hit.getLocation().distanceToSqr(end) < 0.35D * 0.35D;
    }

    private void setLaunchOrigin(Vec3 origin) {
        launchX = origin.x;
        launchY = origin.y;
        launchZ = origin.z;
    }

    private double distanceFromLaunchSqr() {
        return position().distanceToSqr(launchX, launchY, launchZ);
    }

    private static Vec3 horizontal(Vec3 vector) {
        return new Vec3(vector.x, 0.0D, vector.z);
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
        float base = kind() == Kind.BAIHUA_ZANG ? 640.0F : 100.0F;
        return SpellCharge.damage(base, chargeScale());
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
        super.addAdditionalSaveData(tag);
        tag.putInt("Kind", kind().ordinal());
        tag.putFloat("Charge", chargeScale());
        entityData.get(DATA_TARGET).ifPresent(uuid -> tag.putUUID("Target", uuid));
        tag.putInt("OrbitStart", entityData.get(DATA_ORBIT_START));
        tag.putInt("LastWave", entityData.get(DATA_LAST_WAVE));
        tag.putDouble("LaunchX", launchX);
        tag.putDouble("LaunchY", launchY);
        tag.putDouble("LaunchZ", launchZ);
        tag.putDouble("OrbitAnchorX", orbitAnchorX);
        tag.putDouble("OrbitAnchorY", orbitAnchorY);
        tag.putDouble("OrbitAnchorZ", orbitAnchorZ);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setKind(Kind.byId(tag.getInt("Kind")));
        setChargeScale(tag.contains("Charge") ? tag.getFloat("Charge") : SpellCharge.NORMAL);
        entityData.set(DATA_TARGET, tag.hasUUID("Target")
                ? Optional.of(tag.getUUID("Target")) : Optional.empty());
        entityData.set(DATA_ORBIT_START, tag.getInt("OrbitStart"));
        entityData.set(DATA_LAST_WAVE, tag.getInt("LastWave"));
        launchX = tag.getDouble("LaunchX");
        launchY = tag.getDouble("LaunchY");
        launchZ = tag.getDouble("LaunchZ");
        orbitAnchorX = tag.getDouble("OrbitAnchorX");
        orbitAnchorY = tag.getDouble("OrbitAnchorY");
        orbitAnchorZ = tag.getDouble("OrbitAnchorZ");
    }
}
