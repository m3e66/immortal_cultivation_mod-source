package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;

import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.combat.CombatStateManager;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.magicritual.center.ForcefieldInterceptable;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.ForcefieldCenterBehavior;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class FireballTalismanProjectileEntity extends ThrowableItemProjectile
        implements ForcefieldInterceptable {
    public static final int IMPACT_VFX_TICKS = 14;
    private static final double SPIRIT_SEAL_IMPACT_RADIUS = 3.0D;
    private static final double CURVE_DURATION_TICKS = 20.0D;
    private static final double OUTWARD_CURVE_BLOCKS = 8.0D;
    private static final String SPARK_COUNT =
            "immortal_cultivation_mod:spark_talisman_count";
    private static final String SPARK_WINDOW =
            "immortal_cultivation_mod:spark_talisman_window";
    private static final String SPARK_MARK_UNTIL =
            "immortal_cultivation_mod:spark_talisman_mark_until";
    private static final String BINDING_STRANDS =
            "immortal_cultivation_mod:binding_talisman_strands";
    private static final String BINDING_UNTIL =
            "immortal_cultivation_mod:binding_talisman_until";
    private static final DustParticleOptions SILVER_GREEN =
            new DustParticleOptions(new Vector3f(0.65F, 1.0F, 0.78F), 0.75F);
    private static final EntityDataAccessor<Boolean> DATA_IMPACTED =
            SynchedEntityData.defineId(
                    FireballTalismanProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_IMPACT_TICK =
            SynchedEntityData.defineId(
                    FireballTalismanProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ATTACK_TYPE =
            SynchedEntityData.defineId(
                    FireballTalismanProjectileEntity.class, EntityDataSerializers.INT);
    private static final ExplosionDamageCalculator TERRAIN_ONLY =
            new ExplosionDamageCalculator() {
                @Override
                public boolean shouldDamageEntity(
                        Explosion explosion, Entity entity) {
                    return false;
                }

                @Override
                public float getKnockbackMultiplier(Entity entity) {
                    return 0.0F;
                }

                @Override
                public boolean shouldBlockExplode(
                        Explosion explosion, BlockGetter level,
                        net.minecraft.core.BlockPos pos,
                        net.minecraft.world.level.block.state.BlockState state,
                        float power) {
                    return super.shouldBlockExplode(
                            explosion, level, pos, state, power);
                }
            };

    private double startX;
    private double startY;
    private double startZ;
    private double targetX;
    private double targetY;
    private double targetZ;
    private double outwardX;
    private double outwardY;
    private double outwardZ;
    private double speed = 1.45D;
    private Optional<UUID> targetEntity = Optional.empty();
    private int launchDelayTicks;
    private int flightTicks;
    private boolean delayedImpactApplied;

    public FireballTalismanProjectileEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
        noCulling = true;
    }

    public FireballTalismanProjectileEntity(Level level, LivingEntity shooter) {
        super(ModEntities.FIREBALL_TALISMAN_PROJECTILE.get(), shooter, level);
        setNoGravity(true);
        noCulling = true;
    }

    @Override
    protected Item getDefaultItem() {
        return switch (attackType()) {
            case SPARK -> ModItems.XINGHUO_TALISMAN.get();
            case WIND_BLADE -> ModItems.FENGREN_TALISMAN.get();
            case BINDING_SILK -> ModItems.CHANSI_KUN_TALISMAN.get();
            case FIVE_THUNDER -> ModItems.WULEI_XIAOFA_TALISMAN.get();
            case FIRE_PRISON -> ModItems.HUOYU_FENXIE_TALISMAN.get();
            case BARRIER_BREAKER -> ModItems.POZHANG_KAISHAN_TALISMAN.get();
            case BLAST_WIND -> ModItems.BAOFENG_TALISMAN.get();
            case SPIRIT_SEAL -> ModItems.FENGLING_TALISMAN.get();
            case BLACK_FLAME -> ModItems.XUANYAN_FENJIE_TALISMAN.get();
            default -> ModItems.FIREBALL_TALISMAN.get();
        };
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_IMPACTED, false);
        builder.define(DATA_IMPACT_TICK, -1);
        builder.define(DATA_ATTACK_TYPE, AttackType.FIREBALL.ordinal());
    }

    @Override
    public void tick() {
        setNoGravity(true);
        if (impacted()) {
            setDeltaMovement(Vec3.ZERO);
            noPhysics = true;
            super.tick();
            if (level().isClientSide) {
                spawnImpactVfx();
            } else {
                if (attackType() == AttackType.BARRIER_BREAKER
                        && !delayedImpactApplied && impactAge() >= 6) {
                    delayedImpactApplied = true;
                    applyImpactEffect(position(), null);
                }
                if (attackType() == AttackType.BLAST_WIND
                        && !delayedImpactApplied && impactAge() >= 15) {
                    delayedImpactApplied = true;
                    applyImpactEffect(position(), null);
                }
                if (impactAge() >= impactVfxTicks()) {
                    discard();
                }
            }
            return;
        }
        if (!level().isClientSide && launchDelayTicks > 0) {
            launchDelayTicks--;
            setDeltaMovement(Vec3.ZERO);
            super.tick();
            return;
        }
        if (!level().isClientSide) {
            steer();
            if (isRemoved() || impacted()) {
                return;
            }
        }
        super.tick();
        if (impacted()) {
            return;
        }
        if (level().isClientSide) {
            spawnFlightQiVfx();
        }
    }

    private void steer() {
        flightTicks++;
        Vec3 target = currentTargetPosition();
        if (flightTicks > 12 && position().distanceToSqr(target) < 1.25D) {
            if (ForcefieldCenterBehavior.interceptProjectileImpact(this, target)) {
                return;
            }
            hitLockedTarget();
            return;
        }

        double phase = Math.min(1.0D, flightTicks / CURVE_DURATION_TICKS);
        Vec3 desired = attackType() == AttackType.SPIRIT_SEAL
                ? target.subtract(position())
                : curvePoint(target, phase).subtract(position());
        if (phase >= 1.0D || desired.lengthSqr() < 0.001D) {
            desired = target.subtract(position());
        }
        if (desired.lengthSqr() > 0.001D) {
            double stepSpeed = Math.min(speed, Math.max(0.08D, desired.length()));
            setDeltaMovement(desired.normalize().scale(stepSpeed));
        }

        if (flightTicks > 100) {
            discard();
        }
    }

    private Vec3 curvePoint(Vec3 target, double phase) {
        Vec3 start = new Vec3(startX, startY, startZ);
        Vec3 outward = new Vec3(outwardX, outwardY, outwardZ);
        Vec3 firstControl = start.add(outward);
        Vec3 secondControl = target.add(outward.scale(0.35D));
        double inverse = 1.0D - phase;
        return start.scale(inverse * inverse * inverse)
                .add(firstControl.scale(3.0D * inverse * inverse * phase))
                .add(secondControl.scale(3.0D * inverse * phase * phase))
                .add(target.scale(phase * phase * phase));
    }

    private Vec3 currentTargetPosition() {
        if (level() instanceof ServerLevel serverLevel && targetEntity.isPresent()) {
            Entity target = serverLevel.getEntity(targetEntity.get());
            if (target instanceof LivingEntity living && living.isAlive()) {
                Vec3 pos = living.position().add(0.0D, living.getBbHeight() * 0.55D, 0.0D);
                targetX = pos.x;
                targetY = pos.y;
                targetZ = pos.z;
            }
        }
        return new Vec3(targetX, targetY, targetZ);
    }

    private void hitLockedTarget() {
        if (level() instanceof ServerLevel serverLevel && targetEntity.isPresent()) {
            Entity target = serverLevel.getEntity(targetEntity.get());
            if (target instanceof LivingEntity living && living.isAlive() && living != getOwner()) {
                hitEntity(living);
                return;
            }
        }
        explodeAndDiscard(new Vec3(targetX, targetY, targetZ));
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!level().isClientSide) {
            LivingEntity living = ModelBonePartEntity.resolveLiving(result.getEntity());
            if (living != null && living != getOwner()) {
                hitEntity(living);
            } else {
                discard();
            }
        }
    }

    private void hitEntity(LivingEntity target) {
        AttackType type = attackType();
        float damage = type == AttackType.FIREBALL
                || type == AttackType.FIRE_PRISON
                || type == AttackType.BARRIER_BREAKER
                || type == AttackType.BLAST_WIND
                || type == AttackType.SPIRIT_SEAL
                || type == AttackType.BLACK_FLAME
                ? 0.0F : type.damage();
        if (damage > 0.0F) {
            target.hurt(damageSources().thrown(this, getOwner()), damage);
        }
        switch (type) {
            case FIREBALL -> target.igniteForSeconds(4);
            case SPARK -> {
                target.igniteForSeconds(4);
                recordSparkHit(target);
            }
            case WIND_BLADE -> {
                Vec3 push = getDeltaMovement().normalize().scale(1.15D);
                target.push(push.x, 0.18D, push.z);
            }
            case BINDING_SILK -> {
                addBindingStrand(target);
            }
            case FIVE_THUNDER -> target.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, 35, 2));
            case FIRE_PRISON -> target.igniteForSeconds(8);
            case BARRIER_BREAKER -> {
                target.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN, 12, 1));
            }
            case BLAST_WIND -> {
                // The delayed radial release handles damage, interruption, and force.
            }
            case SPIRIT_SEAL -> {
                if (getOwner() instanceof LivingEntity caster) {
                    com.example.immortal_cultivation_mod.spell.SpiritSealSystem
                            .apply(caster, target);
                }
            }
            case BLACK_FLAME -> {
                // The persistent domain applies its own 100 damage/second.
            }
        }
        Vec3 impact = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        if (type != AttackType.BARRIER_BREAKER
                && type != AttackType.BLAST_WIND) {
            applyImpactEffect(impact, target);
        }
        beginImpact(impact);
    }

    private void explodeAndDiscard(Vec3 position) {
        if (impacted()) {
            return;
        }
        if (attackType() != AttackType.BARRIER_BREAKER
                && attackType() != AttackType.BLAST_WIND) {
            applyImpactEffect(position, null);
        }
        beginImpact(position);
    }

    private void applyImpactEffect(Vec3 position, LivingEntity directTarget) {
        switch (attackType()) {
            case FIREBALL -> {
                terrainExplosion(position, 2.4F);
                if (level() instanceof ServerLevel serverLevel) {
                    damageArea(serverLevel, position, 2.4D,
                            AttackType.FIREBALL.damage(), true);
                }
            }
            case FIVE_THUNDER -> {
                spawnVisualLightning(position);
                if (level() instanceof ServerLevel serverLevel) {
                    TalismanWardEntity.spawnLightningStain(
                            serverLevel, groundPosition(serverLevel, position));
                }
            }
            case FIRE_PRISON -> {
                terrainExplosion(position, 10.0F);
                if (level() instanceof ServerLevel serverLevel) {
                    damageArea(serverLevel, position, 10.0D,
                            AttackType.FIRE_PRISON.damage(), true);
                }
            }
            case BARRIER_BREAKER -> {
                terrainExplosion(position, 20.0F);
                if (level() instanceof ServerLevel serverLevel) {
                    damageArea(serverLevel, position, 20.0D,
                            AttackType.BARRIER_BREAKER.damage(), false);
                }
            }
            case BLAST_WIND -> {
                if (level() instanceof ServerLevel serverLevel) {
                    releaseBlastWind(serverLevel, position);
                }
            }
            case SPIRIT_SEAL -> {
                if (level() instanceof ServerLevel serverLevel
                        && getOwner() instanceof LivingEntity caster) {
                    applySpiritSealArea(
                            serverLevel, caster, position, directTarget);
                }
            }
            case BLACK_FLAME -> {
                if (level() instanceof ServerLevel serverLevel) {
                    BlackFlameDomainEntity.spawn(serverLevel,
                            getOwner() instanceof LivingEntity living ? living : null,
                            position);
                }
            }
            default -> {
                // The remaining talismans release their real hit effect without
                // inheriting the fireball's block explosion.
            }
        }
    }

    /**
     * A small impact catch radius makes the single-target seal reliable when
     * the projectile clips the target's feet or the block immediately behind it.
     */
    private void applySpiritSealArea(
            ServerLevel level,
            LivingEntity caster,
            Vec3 center,
            LivingEntity directTarget) {
        double radiusSqr = SPIRIT_SEAL_IMPACT_RADIUS
                * SPIRIT_SEAL_IMPACT_RADIUS;
        net.minecraft.world.phys.AABB area =
                new net.minecraft.world.phys.AABB(center, center)
                        .inflate(SPIRIT_SEAL_IMPACT_RADIUS);
        for (LivingEntity living : level.getEntitiesOfClass(
                LivingEntity.class, area,
                target -> target != caster
                        && target != directTarget
                        && target.isAlive()
                        && target.getBoundingBox().getCenter()
                        .distanceToSqr(center) <= radiusSqr)) {
            com.example.immortal_cultivation_mod.spell.SpiritSealSystem
                    .apply(caster, living);
        }
    }

    private void releaseBlastWind(ServerLevel level, Vec3 center) {
        double radius = 4.0D;
        net.minecraft.world.phys.AABB area =
                new net.minecraft.world.phys.AABB(center, center).inflate(radius);
        for (LivingEntity living : level.getEntitiesOfClass(
                LivingEntity.class, area,
                target -> target != getOwner() && target.isAlive()
                        && target.position().distanceToSqr(center) <= radius * radius)) {
            Vec3 away = living.position().add(0.0D, living.getBbHeight() * 0.35D, 0.0D)
                    .subtract(center);
            double distance = Math.max(0.05D, away.length());
            Vec3 horizontal = new Vec3(away.x, 0.0D, away.z);
            if (horizontal.lengthSqr() < 0.001D) {
                horizontal = new Vec3(
                        level.random.nextDouble() - 0.5D, 0.0D,
                        level.random.nextDouble() - 0.5D);
            }
            horizontal = horizontal.normalize();
            double strength = distance <= 1.5D ? 3.15D
                    : distance <= 3.0D ? 2.05D : 1.15D;
            double upward = distance <= 1.5D ? 0.90D
                    : distance <= 3.0D ? 0.58D : 0.32D;
            double resistance = blastResistance(living);
            Vec3 current = living.getDeltaMovement();
            living.setDeltaMovement(
                    current.x * 0.15D + horizontal.x * strength * resistance,
                    Math.max(current.y * 0.15D, upward * resistance),
                    current.z * 0.15D + horizontal.z * strength * resistance);
            living.hurtMarked = true;
            living.hurt(damageSources().thrown(this, getOwner()),
                    AttackType.BLAST_WIND.damage());
            living.addEffect(new MobEffectInstance(
                    ModEffects.DAZE, Math.max(4, Mth.ceil(12 * resistance)),
                    0, false, false, true));
            if (living instanceof ServerPlayer player) {
                player.setSprinting(false);
                CombatStateManager.interruptSpellPreparation(player);
            } else if (living instanceof CultivationNpc npc) {
                npc.endNpcCast();
            }
        }

        for (Projectile projectile : level.getEntitiesOfClass(
                Projectile.class, area,
                projectile -> projectile != this && projectile.isAlive()
                        && projectile.position().distanceToSqr(center) <= radius * radius)) {
            Vec3 away = projectile.position().subtract(center);
            if (away.lengthSqr() < 0.01D) {
                away = new Vec3(level.random.nextDouble() - 0.5D,
                        0.15D, level.random.nextDouble() - 0.5D);
            }
            double retained = Math.min(2.1D,
                    Math.max(0.65D, projectile.getDeltaMovement().length()));
            projectile.setDeltaMovement(away.normalize().scale(retained));
            projectile.hurtMarked = true;
        }
        emitBlastWindRelease(level, center);
    }

    private static double blastResistance(LivingEntity target) {
        double multiplier = target.getMaxHealth() >= 1000.0F
                || target.getArmorValue() >= 40 ? 0.42D : 1.0D;
        if (target instanceof Player player) {
            var data = ModAttachments.getData(player);
            if (data.spiritRoots() != null
                    && data.spiritRoots().contains(SpiritRoots.EARTH)) {
                multiplier *= 0.65D;
            }
            if (com.example.immortal_cultivation_mod.attachment.CultivationMethods
                    .XUANYUE_SUIFENG_JUE.equals(
                    data.activeCultivationMethod())) {
                multiplier *= 0.55D;
            }
        }
        return multiplier;
    }

    private static void emitBlastWindRelease(ServerLevel level, Vec3 center) {
        DustParticleOptions cyan = new DustParticleOptions(
                new Vector3f(0.46F, 0.94F, 1.0F), 0.82F);
        for (int ring = 0; ring < 3; ring++) {
            double radius = 1.5D + ring * 1.2D;
            int points = 32 + ring * 12;
            for (int i = 0; i < points; i++) {
                double angle = Mth.TWO_PI * i / points;
                level.sendParticles(cyan,
                        center.x + Math.cos(angle) * radius,
                        center.y + 0.08D + ring * 0.07D,
                        center.z + Math.sin(angle) * radius,
                        1, Math.cos(angle) * 0.03D, 0.02D,
                        Math.sin(angle) * 0.03D, 0.12D);
            }
        }
        level.sendParticles(ParticleTypes.POOF, center.x, center.y, center.z,
                70, 2.6D, 0.35D, 2.6D, 0.22D);
        level.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 0.2D, center.z,
                45, 1.8D, 0.7D, 1.8D, 0.25D);
    }

    private void terrainExplosion(Vec3 position, float radius) {
        BlockBreakingSettings.runWithSource(
                BlockBreakingSettings.Category.SPECIAL_ITEMS, getOwner(),
                () -> com.example.immortal_cultivation_mod.spell.SpellDestruction.explode(level(),this, null, TERRAIN_ONLY, position,
                        radius, false, Level.ExplosionInteraction.BLOCK));
    }

    private void damageArea(
            ServerLevel level, Vec3 position, double radius,
            float damage, boolean ignite) {
        for (LivingEntity living : level.getEntitiesOfClass(
                LivingEntity.class,
                new net.minecraft.world.phys.AABB(position, position)
                        .inflate(radius),
                living -> living != getOwner() && living.isAlive()
                        && living.position().distanceToSqr(position)
                        <= radius * radius)) {
            living.hurt(damageSources().thrown(this, getOwner()), damage);
            if (ignite) living.igniteForSeconds(8);
        }
    }

    private static Vec3 groundPosition(
            ServerLevel level, Vec3 position) {
        net.minecraft.core.BlockPos cursor =
                net.minecraft.core.BlockPos.containing(position);
        for (int down = 0; down < 8; down++) {
            if (!level.getBlockState(cursor.below()).isAir()) {
                return new Vec3(position.x, cursor.getY(), position.z);
            }
            cursor = cursor.below();
        }
        return position;
    }

    private void recordSparkHit(LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        CompoundTag data = target.getPersistentData();
        long now = serverLevel.getGameTime();
        int count = data.getLong(SPARK_WINDOW) >= now
                ? data.getInt(SPARK_COUNT) : 0;
        count++;
        data.putInt(SPARK_COUNT, count);
        data.putLong(SPARK_WINDOW, now + 14L);
        if (count >= 2) {
            data.putLong(SPARK_MARK_UNTIL, now + 24L);
            emitStarFireMark(serverLevel, target);
        }
    }

    private void addBindingStrand(LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        CompoundTag data = target.getPersistentData();
        int strands = Math.min(6,
                Math.max(0, data.getInt(BINDING_STRANDS)) + 1);
        data.putInt(BINDING_STRANDS, strands);
        data.putLong(BINDING_UNTIL, serverLevel.getGameTime() + 5 * 20L);
        emitBindingThreads(serverLevel, target, strands);
    }

    /** Ticks the star-fire mark and real multi-strand restraint on any living target. */
    public static void tickTalismanEffects(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel level)) return;
        CompoundTag data = target.getPersistentData();
        long now = level.getGameTime();
        long starUntil = data.getLong(SPARK_MARK_UNTIL);
        if (starUntil > now) {
            if ((target.tickCount & 1) == 0) {
                emitStarFireMark(level, target);
            }
        } else if (starUntil != 0L) {
            data.remove(SPARK_MARK_UNTIL);
            data.remove(SPARK_COUNT);
            data.remove(SPARK_WINDOW);
        }

        long bindingUntil = data.getLong(BINDING_UNTIL);
        if (bindingUntil <= now) {
            if (bindingUntil != 0L) {
                data.remove(BINDING_UNTIL);
                data.remove(BINDING_STRANDS);
            }
            return;
        }
        int strands = Mth.clamp(data.getInt(BINDING_STRANDS), 1, 6);
        target.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN, 8,
                Math.min(4, Math.max(0, strands - 1)),
                false, false, true));
        if (strands >= 4) {
            target.addEffect(new MobEffectInstance(
                    ModEffects.DINGSHEN, 5, 0, false, false, true));
            target.setDeltaMovement(target.getDeltaMovement()
                    .multiply(0.18D, 0.20D, 0.18D));
        }
        if ((target.tickCount & 1) == 0) {
            emitBindingThreads(level, target, strands);
        }
        boolean strong = target.getMaxHealth() >= 100.0F
                || target.getArmorValue() >= 20;
        if (strong && target.tickCount % 20 == 0) {
            strands--;
            level.sendParticles(ParticleTypes.END_ROD,
                    target.getX(),
                    target.getY() + target.getBbHeight() * 0.55D,
                    target.getZ(), 9, 0.55D, 0.55D, 0.55D, 0.06D);
            if (strands <= 0) {
                data.remove(BINDING_UNTIL);
                data.remove(BINDING_STRANDS);
            } else {
                data.putInt(BINDING_STRANDS, strands);
            }
        }
    }

    private static void emitBindingThreads(
            ServerLevel level, LivingEntity target, int strands) {
        double bodyRadius = Math.max(0.22D, target.getBbWidth() * 0.42D);
        for (int strand = 0; strand < strands; strand++) {
            double bodyAngle = Mth.TWO_PI * strand / Math.max(1, strands)
                    + target.getId() * 0.19D;
            double bodyY = target.getY() + target.getBbHeight()
                    * (0.22D + 0.58D * ((strand % 3) / 2.0D));
            Vec3 body = new Vec3(
                    target.getX() + Math.cos(bodyAngle) * bodyRadius,
                    bodyY,
                    target.getZ() + Math.sin(bodyAngle) * bodyRadius);
            double anchorAngle = bodyAngle + 0.65D
                    + (strand % 2 == 0 ? 0.45D : -0.45D);
            Vec3 anchor = new Vec3(
                    target.getX() + Math.cos(anchorAngle) * (1.1D + strand * 0.08D),
                    target.getY() + 0.06D,
                    target.getZ() + Math.sin(anchorAngle) * (1.1D + strand * 0.08D));
            anchor = bindingGroundAnchor(level, anchor);
            for (int segment = 0; segment <= 7; segment++) {
                double t = segment / 7.0D;
                Vec3 point = body.lerp(anchor, t).add(
                        Math.cos(bodyAngle) * Math.sin(Math.PI * t) * 0.12D,
                        -Math.sin(Math.PI * t) * 0.18D,
                        Math.sin(bodyAngle) * Math.sin(Math.PI * t) * 0.12D);
                level.sendParticles(SILVER_GREEN,
                        point.x, point.y, point.z,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    private static Vec3 bindingGroundAnchor(
            ServerLevel level, Vec3 candidate) {
        net.minecraft.core.BlockPos cursor =
                net.minecraft.core.BlockPos.containing(candidate);
        for (int down = 0; down < 7; down++) {
            if (!level.getBlockState(cursor.below()).isAir()) {
                return new Vec3(candidate.x, cursor.getY() + 0.04D,
                        candidate.z);
            }
            cursor = cursor.below();
        }
        return candidate;
    }

    private static void emitStarFireMark(
            ServerLevel level, LivingEntity target) {
        double centerY = target.getY() + target.getBbHeight() * 0.58D;
        double outer = Math.max(0.42D, target.getBbWidth() * 0.58D);
        DustParticleOptions starDust = new DustParticleOptions(
                new Vector3f(1.0F, 0.28F, 0.045F), 0.9F);
        Vec3[] points = new Vec3[5];
        for (int point = 0; point < 5; point++) {
            double angle = -Math.PI * 0.5D
                    + Mth.TWO_PI * point / 5.0D;
            points[point] = new Vec3(
                    target.getX() + Math.cos(angle) * outer,
                    centerY + Math.sin(angle) * outer,
                    target.getZ() + 0.03D);
        }
        int[] order = {0, 2, 4, 1, 3, 0};
        for (int edge = 0; edge < order.length - 1; edge++) {
            Vec3 from = points[order[edge]];
            Vec3 to = points[order[edge + 1]];
            for (int step = 0; step <= 4; step++) {
                Vec3 point = from.lerp(to, step / 4.0D);
                level.sendParticles(starDust,
                        point.x, point.y, point.z,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    private void spawnFlightQiVfx() {
        if ((tickCount & 1) != 0 || getDeltaMovement().lengthSqr() < 0.01D) {
            return;
        }
        Vec3 back = getDeltaMovement().normalize().scale(-0.22D);
        ParticleOptions trail = attackType() == AttackType.BLACK_FLAME
                ? (tickCount % 4 == 0
                        ? ModParticles.BLACK_FLAME_CORE.get()
                        : ModParticles.BLACK_FLAME_EDGE.get())
                : coloredDust(0.72F);
        level().addParticle(trail,
                getX() + back.x, getY() + back.y, getZ() + back.z,
                back.x * 0.06D, back.y * 0.06D, back.z * 0.06D);
    }

    private DustParticleOptions coloredDust(float scale) {
        return new DustParticleOptions(
                new Vector3f(trailRed(), trailGreen(), trailBlue()), scale);
    }

    private void spawnVisualLightning(Vec3 position) {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        DustParticleOptions thread = new DustParticleOptions(
                new Vector3f(0.72F, 0.88F, 1.0F), 0.72F);
        for (int step = 0; step <= 24; step++) {
            serverLevel.sendParticles(thread,
                    position.x, position.y + step * 0.65D, position.z,
                    1, 0.012D, 0.0D, 0.012D, 0.0D);
        }
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                position.x, position.y + 0.12D, position.z,
                34, 2.4D, 0.10D, 2.4D, 0.12D);
        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(serverLevel);
        if (lightning == null) return;
        lightning.moveTo(position.x, position.y, position.z);
        lightning.setVisualOnly(true);
        serverLevel.addFreshEntity(lightning);
    }

    private void beginImpact(Vec3 position) {
        if (impacted()) {
            return;
        }
        setPos(position.x, position.y, position.z);
        setDeltaMovement(Vec3.ZERO);
        noPhysics = true;
        entityData.set(DATA_IMPACTED, true);
        entityData.set(DATA_IMPACT_TICK, tickCount);
    }

    private void spawnImpactVfx() {
        int age = impactAge();
        if (age >= impactVfxTicks()) {
            return;
        }
        if (attackType() == AttackType.BLAST_WIND) {
            spawnBlastWindImpactVfx(age);
            return;
        }
        if (attackType() == AttackType.BLACK_FLAME) {
            spawnBlackFlameImpactVfx(age);
            return;
        }
        float fade = 1.0F - age / (float) IMPACT_VFX_TICKS;
        int sparks = age < 4 ? 5 : 2;
        for (int i = 0; i < sparks; i++) {
            double theta = random.nextDouble() * Mth.TWO_PI;
            double y = random.nextDouble() * 1.5D - 0.25D;
            double horizontal = Math.sqrt(Math.max(0.08D, 1.0D - Math.min(1.0D, y * y * 0.35D)));
            double speed = (0.08D + random.nextDouble() * 0.24D) * fade;
            double vx = Math.cos(theta) * horizontal * speed;
            double vy = y * speed + 0.025D;
            double vz = Math.sin(theta) * horizontal * speed;
            level().addParticle(coloredDust(i % 2 == 0 ? 0.95F : 0.62F),
                    getX(), getY(), getZ(), vx, vy, vz);
        }
        if (age < 5 && (attackType() == AttackType.FIREBALL
                || attackType() == AttackType.SPARK
                || attackType() == AttackType.FIRE_PRISON)) {
            level().addParticle(ParticleTypes.FLAME,
                    getX() + (random.nextDouble() - 0.5D) * 0.5D,
                    getY() + (random.nextDouble() - 0.5D) * 0.5D,
                    getZ() + (random.nextDouble() - 0.5D) * 0.5D,
                    0.0D, 0.025D, 0.0D);
        }
    }

    private void spawnBlackFlameImpactVfx(int age) {
        float fade = 1.0F - age / (float) IMPACT_VFX_TICKS;
        int tongues = age < 5 ? 8 : 3;
        for (int index = 0; index < tongues; index++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double speed = (0.035D + random.nextDouble() * 0.15D) * fade;
            ParticleOptions particle = index % 4 == 0
                    ? ModParticles.BLACK_FLAME_SPARK.get()
                    : index % 2 == 0
                    ? ModParticles.BLACK_FLAME_CORE.get()
                    : ModParticles.BLACK_FLAME_EDGE.get();
            level().addParticle(particle, getX(), getY(), getZ(),
                    Math.cos(angle) * speed,
                    0.035D + random.nextDouble() * 0.09D,
                    Math.sin(angle) * speed);
        }
        if ((age & 1) == 0) {
            level().addParticle(ModParticles.BLACK_FLAME_ASH.get(),
                    getX(), getY(), getZ(),
                    (random.nextDouble() - 0.5D) * 0.025D,
                    0.015D,
                    (random.nextDouble() - 0.5D) * 0.025D);
        }
    }

    private void spawnBlastWindImpactVfx(int age) {
        DustParticleOptions cyan = coloredDust(0.64F);
        if (age < 7) {
            if ((age & 1) == 0) {
                level().addParticle(ParticleTypes.CLOUD,
                        getX(), getY() + 0.05D, getZ(),
                        0.0D, 0.005D, 0.0D);
            }
            return;
        }
        if (age < 15) {
            double progress = (age - 7) / 8.0D;
            int count = 5 + age - 7;
            for (int i = 0; i < count; i++) {
                double angle = random.nextDouble() * Mth.TWO_PI;
                double radius = 3.5D - progress * 2.8D
                        + random.nextDouble() * 0.45D;
                double y = (random.nextDouble() - 0.5D) * (1.1D - progress * 0.5D);
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Vec3 inward = new Vec3(-x, -y * 0.35D, -z)
                        .normalize().scale(0.06D + progress * 0.12D);
                level().addParticle(cyan,
                        getX() + x, getY() + y, getZ() + z,
                        inward.x, inward.y, inward.z);
            }
            return;
        }
        double progress = (age - 15) / Math.max(1.0D, impactVfxTicks() - 15.0D);
        int count = progress < 0.35D ? 16 : 5;
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double speed = 0.16D + random.nextDouble() * 0.34D;
            level().addParticle(i % 3 == 0 ? ParticleTypes.POOF : cyan,
                    getX(), getY() + random.nextDouble() * 0.35D, getZ(),
                    Math.cos(angle) * speed,
                    0.02D + random.nextDouble() * 0.10D,
                    Math.sin(angle) * speed);
        }
    }

    public boolean impacted() {
        return entityData.get(DATA_IMPACTED);
    }

    public int impactAge() {
        int impactTick = entityData.get(DATA_IMPACT_TICK);
        return impactTick < 0 ? 0 : Math.max(0, tickCount - impactTick);
    }

    public int impactVfxTicks() {
        return attackType() == AttackType.BLAST_WIND ? 27 : IMPACT_VFX_TICKS;
    }

    @Override
    protected void onHit(HitResult result) {
        if (result.getType() == HitResult.Type.BLOCK) {
            if (!level().isClientSide) {
                explodeAndDiscard(result.getLocation());
            }
            return;
        }
        super.onHit(result);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!level().isClientSide) {
            explodeAndDiscard(result.getLocation());
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble("StartX", startX);
        tag.putDouble("StartY", startY);
        tag.putDouble("StartZ", startZ);
        tag.putDouble("TargetX", targetX);
        tag.putDouble("TargetY", targetY);
        tag.putDouble("TargetZ", targetZ);
        tag.putDouble("OutwardX", outwardX);
        tag.putDouble("OutwardY", outwardY);
        tag.putDouble("OutwardZ", outwardZ);
        tag.putDouble("Speed", speed);
        tag.putInt("LaunchDelayTicks", launchDelayTicks);
        tag.putInt("FlightTicks", flightTicks);
        tag.putInt("AttackType", attackType().ordinal());
        tag.putBoolean("Impacted", impacted());
        tag.putInt("ImpactTick", entityData.get(DATA_IMPACT_TICK));
        tag.putBoolean("DelayedImpactApplied", delayedImpactApplied);
        targetEntity.ifPresent(uuid -> tag.putUUID("TargetEntity", uuid));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        startX = tag.getDouble("StartX");
        startY = tag.getDouble("StartY");
        startZ = tag.getDouble("StartZ");
        targetX = tag.getDouble("TargetX");
        targetY = tag.getDouble("TargetY");
        targetZ = tag.getDouble("TargetZ");
        outwardX = tag.getDouble("OutwardX");
        outwardY = tag.getDouble("OutwardY");
        outwardZ = tag.getDouble("OutwardZ");
        speed = tag.contains("Speed") ? tag.getDouble("Speed") : 1.45D;
        launchDelayTicks = tag.getInt("LaunchDelayTicks");
        flightTicks = tag.getInt("FlightTicks");
        setAttackType(AttackType.byOrdinal(tag.getInt("AttackType")));
        entityData.set(DATA_IMPACTED, tag.getBoolean("Impacted"));
        entityData.set(DATA_IMPACT_TICK, tag.contains("ImpactTick") ? tag.getInt("ImpactTick") : -1);
        delayedImpactApplied = tag.getBoolean("DelayedImpactApplied");
        targetEntity = tag.hasUUID("TargetEntity") ? Optional.of(tag.getUUID("TargetEntity")) : Optional.empty();
        setNoGravity(true);
        noPhysics = impacted();
        noCulling = true;
    }

    public void configure(Vec3 target, Vec3 outward, double speed, int launchDelayTicks, LivingEntity targetEntity) {
        configure(target, outward, speed, launchDelayTicks,
                targetEntity, AttackType.FIREBALL);
    }

    public void configure(
            Vec3 target, Vec3 outward, double speed, int launchDelayTicks,
            LivingEntity targetEntity, AttackType attackType) {
        startX = getX();
        startY = getY();
        startZ = getZ();
        targetX = target.x;
        targetY = target.y;
        targetZ = target.z;
        Vec3 fixedOutward = outward.lengthSqr() < 0.001D
                ? new Vec3(0.0D, OUTWARD_CURVE_BLOCKS, 0.0D)
                : outward.normalize().scale(OUTWARD_CURVE_BLOCKS);
        outwardX = fixedOutward.x;
        outwardY = fixedOutward.y;
        outwardZ = fixedOutward.z;
        this.speed = Math.max(0.5D, speed);
        this.launchDelayTicks = Math.max(0, launchDelayTicks);
        this.targetEntity = targetEntity == null ? Optional.empty() : Optional.of(targetEntity.getUUID());
        setAttackType(attackType);
        setNoGravity(true);
    }

    public AttackType attackType() {
        // ThrowableItemProjectile asks getDefaultItem() while Entity is still
        // constructing, before SynchedEntityData has been assigned. The
        // projectile is configured with its real type immediately afterward.
        if (entityData == null) {
            return AttackType.FIREBALL;
        }
        return AttackType.byOrdinal(entityData.get(DATA_ATTACK_TYPE));
    }

    public void setAttackType(AttackType attackType) {
        entityData.set(DATA_ATTACK_TYPE,
                (attackType == null ? AttackType.FIREBALL : attackType).ordinal());
    }

    public float trailRed() {
        return attackType().red();
    }

    public float trailGreen() {
        return attackType().green();
    }

    public float trailBlue() {
        return attackType().blue();
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
        return Math.max(1.0F, attackType().damage());
    }

    public enum AttackType {
        // Eight fireballs, each dealing 10 damage in its impact radius.
        FIREBALL(10.0F, 1.00F, 0.35F, 0.025F),
        // Five separate flames, each dealing 30 damage.
        SPARK(30.0F, 1.00F, 0.22F, 0.035F),
        WIND_BLADE(11.0F, 0.24F, 0.92F, 1.00F),
        BINDING_SILK(1.0F, 0.65F, 1.00F, 0.78F),
        FIVE_THUNDER(40.0F, 0.42F, 0.62F, 1.00F),
        FIRE_PRISON(50.0F, 1.00F, 0.08F, 0.015F),
        BARRIER_BREAKER(500.0F, 1.00F, 0.82F, 0.28F),
        BLAST_WIND(50.0F, 0.46F, 0.94F, 1.00F),
        SPIRIT_SEAL(0.0F, 0.70F, 0.48F, 0.10F),
        BLACK_FLAME(100.0F, 0.58F, 0.035F, 1.00F);

        private final float damage;
        private final float red;
        private final float green;
        private final float blue;

        AttackType(float damage, float red, float green, float blue) {
            this.damage = damage;
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        public float damage() {
            return damage;
        }

        public float red() {
            return red;
        }

        public float green() {
            return green;
        }

        public float blue() {
            return blue;
        }

        public static AttackType byOrdinal(int ordinal) {
            AttackType[] values = values();
            return ordinal >= 0 && ordinal < values.length
                    ? values[ordinal] : FIREBALL;
        }
    }
}
