package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.ForcefieldCenterBehavior;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 雷角岩獾: a Foundation Establishment middle-stage earth/thunder ambush beast.
 * Its three upper armor sections each absorb 90 damage and break independently.
 */
public final class ThunderhornRockBadgerEntity extends Monster implements GeoEntity {
    public static final float MODEL_SCALE = 1.0F;

    private static final EntityDataAccessor<Byte> ACTION_STATE =
            SynchedEntityData.defineId(ThunderhornRockBadgerEntity.class,
                    EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ACTION_TICKS =
            SynchedEntityData.defineId(ThunderhornRockBadgerEntity.class,
                    EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PLATE_MASK =
            SynchedEntityData.defineId(ThunderhornRockBadgerEntity.class,
                    EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FRENZIED =
            SynchedEntityData.defineId(ThunderhornRockBadgerEntity.class,
                    EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BURROWED =
            SynchedEntityData.defineId(ThunderhornRockBadgerEntity.class,
                    EntityDataSerializers.BOOLEAN);

    public static final byte ACTION_NONE = 0;
    public static final byte ACTION_CLAW_SLAM = 1;
    public static final byte ACTION_HORN_CHARGE = 2;
    public static final byte ACTION_STONE_STOMP = 3;
    public static final byte ACTION_ROCK_VOLLEY = 4;
    public static final byte ACTION_BURROW = 5;
    public static final byte ACTION_ARMOR_SHAKE = 6;
    public static final byte ACTION_FRENZY = 7;
    public static final byte ACTION_STUN = 8;

    private static final int CLAW_TICKS = 20;
    private static final int CHARGE_TICKS = 44;
    private static final int STOMP_TICKS = 28;
    private static final int VOLLEY_TICKS = 42;
    private static final int BURROW_TICKS = 56;
    private static final int SHAKE_TICKS = 26;
    private static final int FRENZY_ACTION_TICKS = 30;
    private static final int STUN_TICKS = 24;

    private static final int CLAW_COOLDOWN = 100;
    private static final int CHARGE_COOLDOWN = 180;
    private static final int STOMP_COOLDOWN = 220;
    private static final int VOLLEY_COOLDOWN = 200;
    private static final int BURROW_COOLDOWN = 300;
    private static final int SHAKE_COOLDOWN = 320;
    private static final int FRENZY_DURATION = 200;

    private static final float PLATE_DURABILITY = 90.0F;
    private static final int ALL_PLATES = 0b111;
    private static final double BASE_SPEED = 0.27D;

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin()
            .thenLoop("animation.thunderhorn_rock_badger.idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin()
            .thenLoop("animation.thunderhorn_rock_badger.walk");
    private static final RawAnimation RUN_ANIM = RawAnimation.begin()
            .thenLoop("animation.thunderhorn_rock_badger.run");
    private static final RawAnimation CLAW_ANIM = RawAnimation.begin()
            .thenPlay("animation.thunderhorn_rock_badger.claw_slam");
    private static final RawAnimation CHARGE_ANIM = RawAnimation.begin()
            .thenPlay("animation.thunderhorn_rock_badger.horn_charge");
    private static final RawAnimation STOMP_ANIM = RawAnimation.begin()
            .thenPlay("animation.thunderhorn_rock_badger.stone_stomp");
    private static final RawAnimation VOLLEY_ANIM = RawAnimation.begin()
            .thenPlay("animation.thunderhorn_rock_badger.rock_volley");
    private static final RawAnimation BURROW_ANIM = RawAnimation.begin()
            .thenPlay("animation.thunderhorn_rock_badger.burrow");
    private static final RawAnimation SHAKE_ANIM = RawAnimation.begin()
            .thenPlay("animation.thunderhorn_rock_badger.armor_shake");
    private static final RawAnimation FRENZY_ANIM = RawAnimation.begin()
            .thenPlay("animation.thunderhorn_rock_badger.frenzy");
    private static final RawAnimation STUN_ANIM = RawAnimation.begin()
            .thenPlay("animation.thunderhorn_rock_badger.stun");
    private static final RawAnimation DEATH_ANIM = RawAnimation.begin()
            .thenPlayAndHold("animation.thunderhorn_rock_badger.death");

    private final AnimatableInstanceCache animationCache =
            GeckoLibUtil.createInstanceCache(this);
    private final float[] plateDamage = new float[3];
    private final Set<UUID> chargeVictims = new HashSet<>();

    private int clawCooldown;
    private int chargeCooldown;
    private int stompCooldown;
    private int volleyCooldown;
    private int burrowCooldown;
    private int shakeCooldown;
    private int frenzyTicks;
    private boolean frenzyTriggered;
    private Vec3 chargeDirection = Vec3.ZERO;
    private Vec3 burrowDestination = Vec3.ZERO;

    public ThunderhornRockBadgerEntity(
            EntityType<? extends ThunderhornRockBadgerEntity> type, Level level) {
        super(type, level);
        xpReward = 120;
        noCulling = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 650.0D)
                .add(Attributes.ATTACK_DAMAGE, 55.0D)
                .add(Attributes.ARMOR, 16.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, BASE_SPEED)
                .add(Attributes.ATTACK_SPEED, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 18.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.45D);
    }

    public static boolean checkSpawnRules(
            EntityType<ThunderhornRockBadgerEntity> type,
            ServerLevelAccessor level, MobSpawnType spawnType,
            BlockPos pos, RandomSource random) {
        if (!Monster.checkMonsterSpawnRules(type, level, spawnType, pos, random)) {
            return false;
        }
        BlockPos floor = pos.below();
        return level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                && level.getFluidState(pos).isEmpty()
                && level.getFluidState(pos.above()).isEmpty();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.08D, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.72D));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this, LivingEntity.class, true,
                target -> target instanceof Player || target instanceof CultivationNpc));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ACTION_STATE, ACTION_NONE);
        builder.define(ACTION_TICKS, 0);
        builder.define(PLATE_MASK, ALL_PLATES);
        builder.define(FRENZIED, false);
        builder.define(BURROWED, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide || !isAlive()) {
            return;
        }
        if (hasAnyPlate() && tickCount % (isFrenzied() ? 2 : 8) == 0) {
            double x = getX() + (random.nextDouble() - 0.5D) * 1.8D;
            double y = getY() + 0.9D + random.nextDouble() * 0.65D;
            double z = getZ() + (random.nextDouble() - 0.5D) * 2.1D;
            level().addParticle(ParticleTypes.ELECTRIC_SPARK,
                    x, y, z, 0.0D, 0.02D, 0.0D);
            level().addParticle(ModParticles.METHOD_QI_THUNDER.get(),
                    x, y, z, 0.0D, 0.008D, 0.0D);
        }
        if (isFrenzied() && (tickCount & 1) == 0) {
            level().addParticle(ModParticles.METHOD_QI_EARTH.get(),
                    getX() + (random.nextDouble() - 0.5D) * 2.0D,
                    getY() + random.nextDouble() * 1.2D,
                    getZ() + (random.nextDouble() - 0.5D) * 2.0D,
                    0.0D, 0.025D, 0.0D);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || !isAlive()) {
            return;
        }
        if (BeastDazeControl.blocksMoves(this)) {
            finishAction();
            return;
        }

        decrementCooldowns();
        tickFrenzy();
        if (!frenzyTriggered && getHealth() <= getMaxHealth() * 0.30F) {
            activateFrenzy();
        }

        if (getActionState() != ACTION_NONE) {
            tickCurrentAction();
            return;
        }
        if ((tickCount + getId()) % 5 != 0) {
            return;
        }

        LivingEntity target = getTarget();
        if (!canAttackTarget(target)) {
            return;
        }
        double distance = distanceTo(target);
        int nearby = level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(4.0D), this::canAttackTarget).size();

        if (distance <= 4.0D) {
            if (shakeCooldown <= 0 && hasAnyPlate() && nearby >= 2) {
                shakeCooldown = SHAKE_COOLDOWN;
                beginAction(ACTION_ARMOR_SHAKE, SHAKE_TICKS, target);
            } else if (stompCooldown <= 0 && random.nextFloat() < 0.32F) {
                stompCooldown = STOMP_COOLDOWN;
                beginAction(ACTION_STONE_STOMP, STOMP_TICKS, target);
            } else if (clawCooldown <= 0) {
                clawCooldown = CLAW_COOLDOWN;
                beginAction(ACTION_CLAW_SLAM, CLAW_TICKS, target);
            }
            return;
        }

        if (distance >= 5.0D && distance <= 10.0D
                && chargeCooldown <= 0 && hasLineOfSight(target)) {
            chargeCooldown = CHARGE_COOLDOWN;
            beginAction(ACTION_HORN_CHARGE, CHARGE_TICKS, target);
            return;
        }

        if (distance >= 8.0D && distance <= 14.0D) {
            if (burrowCooldown <= 0 && canStartBurrow(target)
                    && random.nextFloat() < 0.45F) {
                burrowCooldown = BURROW_COOLDOWN;
                beginAction(ACTION_BURROW, BURROW_TICKS, target);
            } else if (volleyCooldown <= 0 && hasLineOfSight(target)) {
                volleyCooldown = VOLLEY_COOLDOWN;
                beginAction(ACTION_ROCK_VOLLEY, VOLLEY_TICKS, target);
            }
        }
    }

    private void decrementCooldowns() {
        if (clawCooldown > 0) clawCooldown--;
        if (chargeCooldown > 0) {
            chargeCooldown--;
            if (isFrenzied() && tickCount % 10 < 3) chargeCooldown--;
        }
        if (stompCooldown > 0) stompCooldown--;
        if (volleyCooldown > 0) volleyCooldown--;
        if (burrowCooldown > 0) burrowCooldown--;
        if (shakeCooldown > 0) shakeCooldown--;
    }

    private void tickFrenzy() {
        if (!isFrenzied()) {
            return;
        }
        if (frenzyTicks > 0) {
            frenzyTicks--;
        }
        if (frenzyTicks <= 0) {
            entityData.set(FRENZIED, false);
            updateCombatAttributes();
        }
    }

    private void activateFrenzy() {
        frenzyTriggered = true;
        frenzyTicks = FRENZY_DURATION;
        entityData.set(FRENZIED, true);
        updateCombatAttributes();
        beginAction(ACTION_FRENZY, FRENZY_ACTION_TICKS, getTarget());
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    getX(), getY() + 0.9D, getZ(),
                    45, 1.1D, 0.8D, 1.1D, 0.16D);
            level.sendParticles(ModParticles.METHOD_QI_THUNDER.get(),
                    getX(), getY() + 0.8D, getZ(),
                    35, 1.0D, 0.65D, 1.0D, 0.09D);
            level.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR,
                    SoundSource.HOSTILE, 1.35F, 0.78F);
        }
    }

    private void beginAction(byte action, int ticks, LivingEntity target) {
        if (BeastDazeControl.blocksMoves(this)) {
            return;
        }
        entityData.set(ACTION_STATE, action);
        entityData.set(ACTION_TICKS, ticks);
        getNavigation().stop();
        if (action == ACTION_HORN_CHARGE) {
            chargeVictims.clear();
            chargeDirection = horizontalDirectionTo(target);
        } else if (action == ACTION_BURROW && target != null) {
            Vec3 toward = target.position().subtract(position());
            toward = new Vec3(toward.x, 0.0D, toward.z);
            if (toward.lengthSqr() > 100.0D) {
                toward = toward.normalize().scale(10.0D);
            }
            burrowDestination = position().add(toward);
        }
    }

    private void tickCurrentAction() {
        int ticks = getActionTicks();
        byte action = getActionState();
        LivingEntity target = getTarget();
        getNavigation().stop();

        if (action != ACTION_HORN_CHARGE && action != ACTION_BURROW) {
            Vec3 motion = getDeltaMovement();
            setDeltaMovement(motion.x * 0.18D, motion.y, motion.z * 0.18D);
        }
        if (canAttackTarget(target) && action != ACTION_HORN_CHARGE
                && action != ACTION_BURROW) {
            lookAt(target, 30.0F, 25.0F);
        }

        if (level() instanceof ServerLevel level) {
            switch (action) {
                case ACTION_CLAW_SLAM -> tickClawSlam(level, ticks);
                case ACTION_HORN_CHARGE -> tickHornCharge(level, ticks);
                case ACTION_STONE_STOMP -> tickStoneStomp(level, ticks);
                case ACTION_ROCK_VOLLEY -> tickRockVolley(level, ticks, target);
                case ACTION_BURROW -> tickBurrow(level, ticks);
                case ACTION_ARMOR_SHAKE -> tickArmorShake(level, ticks);
                case ACTION_FRENZY -> tickFrenzyAction(level, ticks);
                case ACTION_STUN -> tickStun(level, ticks);
                default -> {
                }
            }
        }

        if (ticks <= 1) {
            finishAction();
        } else if (getActionState() == action) {
            entityData.set(ACTION_TICKS, ticks - 1);
        }
    }

    private void tickClawSlam(ServerLevel level, int ticks) {
        if (ticks == 15) {
            level.playSound(null, blockPosition(), SoundEvents.RAVAGER_ATTACK,
                    SoundSource.HOSTILE, 1.0F, 0.72F);
        }
        if (ticks != 9) return;

        Vec3 forward = horizontalLook();
        Vec3 center = position().add(forward.scale(1.5D)).add(0.0D, 0.45D, 0.0D);
        for (LivingEntity target : targetsIn(center, 2.35D, 1.5D)) {
            float damage = target instanceof Player player && player.isBlocking()
                    ? 106.25F : 85.0F;
            if (damageTarget(target, damage)) {
                target.addEffect(new MobEffectInstance(
                        ModEffects.DAZE, 10, 0, false, true, true), this);
                knockAway(target, 1.15D, 0.18D);
            }
        }
        emitJaggedCracks(level, center, 3.0D, 3);
        level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 1.1F, 0.76F);
    }

    private void tickHornCharge(ServerLevel level, int ticks) {
        if (ticks > 30) {
            setDeltaMovement(Vec3.ZERO);
            if ((ticks & 1) == 0) {
                emitHornCharge(level);
            }
            if (ticks == 31) {
                level.playSound(null, blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                        SoundSource.HOSTILE, 0.85F, 1.35F);
            }
            return;
        }
        if (ticks >= 18) {
            if (chargeDirection.lengthSqr() < 0.01D) {
                chargeDirection = horizontalLook();
            }
            destroyChargeObstructions(level);
            setDeltaMovement(chargeDirection.scale(0.75D));
            hasImpulse = true;
            emitChargeTrail(level);

            AABB hitbox = getBoundingBox().expandTowards(chargeDirection.scale(0.8D))
                    .inflate(0.25D);
            for (LivingEntity target : level.getEntitiesOfClass(
                    LivingEntity.class, hitbox, this::canAttackTarget)) {
                if (!chargeVictims.add(target.getUUID())) continue;
                if (damageTarget(target, 110.0F)) {
                    target.addEffect(new MobEffectInstance(
                            ModEffects.DAZE, 10, 0, false, true, true), this);
                    knockAway(target, 1.65D, 0.28D);
                }
            }
            if (horizontalCollision || chargeWallAhead(level)) {
                crashIntoWall(level);
            }
        } else {
            setDeltaMovement(Vec3.ZERO);
        }
    }

    private void crashIntoWall(ServerLevel level) {
        super.hurt(damageSources().generic(), 35.0F);
        setDeltaMovement(Vec3.ZERO);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                getX(), getY() + 0.8D, getZ(), 30,
                0.9D, 0.65D, 0.9D, 0.18D);
        BlockState floor = level.getBlockState(blockPosition().below());
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor),
                getX(), getY() + 0.5D, getZ(), 35,
                0.8D, 0.6D, 0.8D, 0.14D);
        level.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND,
                SoundSource.HOSTILE, 1.3F, 0.62F);
        entityData.set(ACTION_STATE, ACTION_STUN);
        entityData.set(ACTION_TICKS, STUN_TICKS);
    }

    private void tickStoneStomp(ServerLevel level, int ticks) {
        if (ticks != 13) return;
        for (LivingEntity target : targetsIn(position(), 4.0D, 2.2D)) {
            if (damageTarget(target, 70.0F)) {
                target.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN, 50, 0, false, true, true), this);
                Vec3 velocity = target.getDeltaMovement();
                target.setDeltaMovement(velocity.x, Math.max(0.32D, velocity.y), velocity.z);
            }
        }
        BeastMoveBlockBreaker.breakBox(level, this,
                new AABB(getX() - 4.0D, getY() - 0.65D,
                        getZ() - 4.0D, getX() + 4.0D,
                        getY() + 0.75D, getZ() + 4.0D),
                position().add(0.0D, 0.1D, 0.0D), 28, 6.0F);
        emitJaggedCracks(level, position(), 4.0D, 7);
        level.playSound(null, blockPosition(), SoundEvents.RAVAGER_STEP,
                SoundSource.HOSTILE, 1.45F, 0.65F);
    }

    private void tickRockVolley(ServerLevel level, int ticks, LivingEntity target) {
        if (ticks == 35) {
            emitRisingRocks(level);
        }
        int index = switch (ticks) {
            case 30 -> 0;
            case 24 -> 1;
            case 18 -> 2;
            case 12 -> 3;
            default -> -1;
        };
        if (index < 0 || !canAttackTarget(target)) return;

        ThunderRockProjectileEntity projectile = new ThunderRockProjectileEntity(
                level, this, target, index == 3);
        projectile.setPos(getX(), getY() + 0.65D,
                getZ());
        Vec3 aimPoint = target.getBoundingBox().getCenter();
        if (index == 3) {
            aimPoint = aimPoint.add(target.getDeltaMovement().scale(7.0D));
        }
        Vec3 aim = aimPoint.subtract(projectile.position());
        projectile.shoot(aim.x, aim.y + 0.1D, aim.z, 1.28F,
                index == 3 ? 1.0F : 4.5F);
        level.addFreshEntity(projectile);
        level.playSound(null, blockPosition(), SoundEvents.TRIDENT_THROW.value(),
                SoundSource.HOSTILE, 0.72F, 0.72F + index * 0.08F);
    }

    private void tickBurrow(ServerLevel level, int ticks) {
        if (ticks == 46) {
            setBurrowed(true);
            level.playSound(null, blockPosition(), SoundEvents.ROOTED_DIRT_BREAK,
                    SoundSource.HOSTILE, 1.0F, 0.72F);
        }
        if (ticks <= 45 && ticks >= 13 && isBurrowed()) {
            Vec3 direction = burrowDestination.subtract(position());
            direction = new Vec3(direction.x, 0.0D, direction.z);
            if (direction.lengthSqr() > 0.08D) {
                Vec3 next = position().add(direction.normalize().scale(0.34D));
                if (canBurrowAt(level, next)) {
                    setPos(next.x, getY(), next.z);
                } else {
                    burrowDestination = position();
                }
            }
            emitBurrowTrail(level);
        }
        if (ticks == 12) {
            emitEruptionWarning(level, burrowDestination);
        }
        if (ticks == 6) {
            setBurrowed(false);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    Mth.floor(getX()), Mth.floor(getZ()));
            setPos(getX(), y, getZ());
            for (LivingEntity target : targetsIn(position(), 2.65D, 2.5D)) {
                if (damageTarget(target, 95.0F)) {
                    Vec3 velocity = target.getDeltaMovement();
                    target.setDeltaMovement(velocity.x, Math.max(0.65D, velocity.y), velocity.z);
                }
            }
            emitEruption(level);
        }
    }

    private void tickArmorShake(ServerLevel level, int ticks) {
        if (ticks != 12) return;
        double radius = 5.0D - brokenPlateCount() * 0.5D;
        for (LivingEntity target : targetsIn(position(), radius, 2.4D)) {
            if (damageTarget(target, 50.0F)) {
                knockAway(target, 1.65D, 0.12D);
            }
        }
        AABB area = getBoundingBox().inflate(radius);
        for (Projectile projectile : level.getEntitiesOfClass(
                Projectile.class, area, this::isWeakPhysicalProjectile)) {
            Vec3 away = projectile.position().subtract(position()).normalize();
            double speed = Math.max(0.5D, projectile.getDeltaMovement().length());
            projectile.setDeltaMovement(away.scale(speed * 1.15D).add(0.0D, 0.12D, 0.0D));
            projectile.setOwner(this);
            projectile.hasImpulse = true;
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                getX(), getY() + 0.8D, getZ(),
                32, radius * 0.55D, 0.65D, radius * 0.55D, 0.16D);
        level.sendParticles(ModParticles.METHOD_QI_EARTH.get(),
                getX(), getY() + 0.35D, getZ(),
                38, radius * 0.6D, 0.35D, radius * 0.6D, 0.12D);
        level.playSound(null, blockPosition(), SoundEvents.IRON_GOLEM_DAMAGE,
                SoundSource.HOSTILE, 1.25F, 0.7F);
    }

    private void tickFrenzyAction(ServerLevel level, int ticks) {
        if (ticks % 4 == 0) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    getX(), getY() + 0.8D, getZ(),
                    7, 0.9D, 0.65D, 0.9D, 0.12D);
        }
    }

    private void tickStun(ServerLevel level, int ticks) {
        setDeltaMovement(Vec3.ZERO);
        if (ticks % 5 == 0) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    getX(), getY() + 1.45D, getZ(),
                    4, 0.35D, 0.15D, 0.35D, 0.05D);
        }
    }

    private void finishAction() {
        setBurrowed(false);
        entityData.set(ACTION_STATE, ACTION_NONE);
        entityData.set(ACTION_TICKS, 0);
        chargeVictims.clear();
        chargeDirection = Vec3.ZERO;
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        LivingEntity target = ModelBonePartEntity.resolveLiving(entity);
        if (BeastDazeControl.blocksMoves(this)
                || !canAttackTarget(target) || getActionState() != ACTION_NONE) {
            return false;
        }
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE)
                + (isFrenzied() ? 15.0F : 0.0F);
        boolean hit = damageTarget(target, damage);
        if (hit) {
            knockAway(target, 0.55D, 0.06D);
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isBurrowed()) {
            return false;
        }
        float adjusted = applyElementResistance(source, amount);
        int plate = plateForHit(source);
        if (plate >= 0 && hasPlate(plate)) {
            float remaining = Math.max(0.0F, PLATE_DURABILITY - plateDamage[plate]);
            float absorbed = Math.min(remaining, adjusted);
            plateDamage[plate] += absorbed;
            adjusted -= absorbed;
            emitPlateHit(plate, absorbed);
            if (plateDamage[plate] >= PLATE_DURABILITY - 0.001F) {
                breakPlate(plate);
            }
            if (adjusted <= 0.001F) {
                if (source.getEntity() instanceof LivingEntity attacker) {
                    setLastHurtByMob(attacker);
                    setTarget(attacker);
                }
                level().broadcastEntityEvent(this, (byte) 2);
                return true;
            }
        } else if (isBellyOrFlankHit(source)) {
            adjusted *= 1.20F;
        }
        return super.hurt(source, adjusted);
    }

    private float applyElementResistance(DamageSource source, float amount) {
        Entity direct = source.getDirectEntity();
        if (source.is(DamageTypes.LIGHTNING_BOLT)
                || direct instanceof LightningProjectileEntity
                || direct instanceof ElementalTechniqueAreaEntity area
                && (area.kind() == ElementalTechniqueAreaEntity.Kind.WULEI_HONGDING
                || area.kind() == ElementalTechniqueAreaEntity.Kind.JIUXIAO_FENGLEI_YU)) {
            return amount * 0.75F;
        }
        if (direct instanceof ZhenshanPalmEntity
                || direct instanceof ShanbengSealEntity
                || direct instanceof TemporaryTerrainEntity
                || direct instanceof ElementalTechniqueAreaEntity area
                && area.kind() == ElementalTechniqueAreaEntity.Kind.JINGJI_TUSHENG) {
            return amount * 0.70F;
        }
        if (direct instanceof SlidingWaterProjectileEntity
                || direct instanceof ElementalTechniqueProjectileEntity projectile
                && projectile.kind() == ElementalTechniqueProjectileEntity.Kind.SHUIREN_LIANZHAN) {
            return amount * 1.15F;
        }
        return amount;
    }

    private int plateForHit(DamageSource source) {
        Vec3 sourcePos = source.getSourcePosition();
        if (sourcePos == null || sourcePos.y < getY() + getBbHeight() * 0.54D) {
            return -1;
        }
        Vec3 offset = sourcePos.subtract(position());
        Vec3 forward = horizontalLook();
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        double longitudinal = offset.dot(forward);
        double lateral = Math.abs(offset.dot(right));
        if (longitudinal > lateral * 0.62D) {
            return -1;
        }
        if (longitudinal < -lateral * 0.62D) {
            return 2;
        }
        if (longitudinal > 0.35D) {
            return 0;
        }
        return 1;
    }

    private boolean isBellyOrFlankHit(DamageSource source) {
        Vec3 sourcePos = source.getSourcePosition();
        if (sourcePos == null) return false;
        if (sourcePos.y < getY() + getBbHeight() * 0.52D) return true;
        Vec3 offset = sourcePos.subtract(position());
        Vec3 forward = horizontalLook();
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        return Math.abs(offset.dot(right)) > Math.abs(offset.dot(forward)) * 1.2D;
    }

    private void emitPlateHit(int plate, float absorbed) {
        if (!(level() instanceof ServerLevel level)) return;
        Vec3 offset = plateOffset(plate);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                offset.x, offset.y, offset.z,
                Mth.clamp(Mth.ceil(absorbed / 5.0F), 4, 18),
                0.45D, 0.35D, 0.45D, 0.09D);
        BlockState state = level.getBlockState(blockPosition().below());
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                offset.x, offset.y, offset.z,
                Mth.clamp(Mth.ceil(absorbed / 6.0F), 3, 14),
                0.4D, 0.3D, 0.4D, 0.08D);
        level.playSound(null, blockPosition(), SoundEvents.STONE_HIT,
                SoundSource.HOSTILE, 0.7F, 0.72F + random.nextFloat() * 0.2F);
    }

    private void breakPlate(int plate) {
        entityData.set(PLATE_MASK, getPlateMask() & ~(1 << plate));
        updateCombatAttributes();
        if (level() instanceof ServerLevel level) {
            Vec3 offset = plateOffset(plate);
            BlockState state = level.getBlockState(blockPosition().below());
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    offset.x, offset.y, offset.z,
                    42, 0.7D, 0.55D, 0.7D, 0.18D);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    offset.x, offset.y, offset.z,
                    28, 0.65D, 0.5D, 0.65D, 0.16D);
            level.playSound(null, blockPosition(), SoundEvents.DEEPSLATE_BREAK,
                    SoundSource.HOSTILE, 1.25F, 0.62F);
        }
    }

    private Vec3 plateOffset(int plate) {
        Vec3 forward = horizontalLook();
        double along = switch (plate) {
            case 0 -> 0.65D;
            case 2 -> -0.65D;
            default -> 0.0D;
        };
        return position().add(forward.scale(along)).add(0.0D, 1.25D, 0.0D);
    }

    private void updateCombatAttributes() {
        double brokenBonus = brokenPlateCount() * 0.04D;
        double frenzyBonus = isFrenzied() ? 1.25D : 1.0D;
        var movement = getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) {
            movement.setBaseValue(BASE_SPEED * (1.0D + brokenBonus) * frenzyBonus);
        }
        var attackSpeed = getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.setBaseValue(isFrenzied() ? 4.8D : 4.0D);
        }
        var armor = getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.setBaseValue(isFrenzied() ? 12.8D : 16.0D);
        }
    }

    private boolean damageTarget(LivingEntity target, float damage) {
        if (!canAttackTarget(target)
                || ForcefieldCenterBehavior.interceptAttackPath(
                this, getBoundingBox().getCenter(),
                target.getBoundingBox().getCenter(), damage)) {
            return false;
        }
        return target.hurt(damageSources().mobAttack(this), damage);
    }

    private boolean canAttackTarget(LivingEntity target) {
        if (target == null || target == this || !target.isAlive()) return false;
        if (target instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }
        return target instanceof CultivationNpc;
    }

    private java.util.List<LivingEntity> targetsIn(Vec3 center, double radius, double yRadius) {
        AABB area = new AABB(center, center).inflate(radius, yRadius, radius);
        return level().getEntitiesOfClass(LivingEntity.class, area, this::canAttackTarget);
    }

    private void knockAway(LivingEntity target, double strength, double upward) {
        Vec3 away = target.position().subtract(position());
        away = new Vec3(away.x, 0.0D, away.z);
        if (away.lengthSqr() < 0.001D) away = horizontalLook();
        away = away.normalize().scale(strength);
        target.setDeltaMovement(target.getDeltaMovement().scale(0.35D)
                .add(away.x, upward, away.z));
        target.hurtMarked = true;
    }

    private Vec3 horizontalDirectionTo(LivingEntity target) {
        if (target == null) return horizontalLook();
        Vec3 direction = target.getBoundingBox().getCenter()
                .subtract(getBoundingBox().getCenter());
        direction = new Vec3(direction.x, 0.0D, direction.z);
        return direction.lengthSqr() < 0.001D ? horizontalLook() : direction.normalize();
    }

    private Vec3 horizontalLook() {
        Vec3 look = getLookAngle();
        look = new Vec3(look.x, 0.0D, look.z);
        return look.lengthSqr() < 0.001D
                ? Vec3.directionFromRotation(0.0F, getYRot()).normalize()
                : look.normalize();
    }

    private boolean canStartBurrow(LivingEntity target) {
        return target != null && !target.isInWaterOrBubble()
                && canBurrowAt((ServerLevel) level(), target.position());
    }

    private boolean canBurrowAt(ServerLevel level, Vec3 point) {
        BlockPos pos = BlockPos.containing(point);
        if (!level.getFluidState(pos).isEmpty()
                || !level.getFluidState(pos.below()).isEmpty()) {
            return false;
        }
        BlockState floor = level.getBlockState(pos.below());
        String id = BuiltInRegistries.BLOCK.getKey(floor.getBlock()).getPath();
        boolean metalOrReinforced = id.contains("iron") || id.contains("copper")
                || id.contains("metal") || id.contains("reinforced")
                || id.contains("vault") || floor.getDestroySpeed(level, pos.below()) < 0.0F;
        return !metalOrReinforced;
    }

    private boolean chargeWallAhead(ServerLevel level) {
        Vec3 point = position().add(chargeDirection.scale(getBbWidth() * 0.7D + 0.35D))
                .add(0.0D, 0.55D, 0.0D);
        BlockPos pos = BlockPos.containing(point);
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && !isChargeBreakable(state);
    }

    private void destroyChargeObstructions(ServerLevel level) {
        Vec3 origin = getBoundingBox().getCenter();
        AABB sweep = getBoundingBox()
                .expandTowards(chargeDirection.scale(1.55D))
                .inflate(0.18D, 0.08D, 0.18D);
        BeastMoveBlockBreaker.breakBox(level, this, sweep, origin,
                16, 6.0F);
    }

    private static boolean isChargeBreakable(BlockState state) {
        return state.is(BlockTags.LEAVES)
                || BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().contains("glass")
                || state.canBeReplaced();
    }

    private boolean isWeakPhysicalProjectile(Projectile projectile) {
        return projectile.getOwner() != this
                && (projectile instanceof AbstractArrow || projectile instanceof Snowball);
    }

    private void emitHornCharge(ServerLevel level) {
        Vec3 horn = position().add(horizontalLook().scale(1.15D)).add(0.0D, 1.45D, 0.0D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                horn.x, horn.y, horn.z, 7, 0.24D, 0.22D, 0.24D, 0.08D);
        level.sendParticles(ModParticles.METHOD_QI_THUNDER.get(),
                horn.x, horn.y, horn.z, 4, 0.18D, 0.18D, 0.18D, 0.04D);
    }

    private void emitChargeTrail(ServerLevel level) {
        Vec3 behind = position().subtract(chargeDirection.scale(0.9D));
        BlockState floor = level.getBlockState(blockPosition().below());
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor),
                behind.x, getY() + 0.2D, behind.z,
                7, 0.5D, 0.18D, 0.5D, 0.08D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                getX(), getY() + 0.85D, getZ(),
                4, 0.6D, 0.35D, 0.6D, 0.04D);
    }

    private void emitJaggedCracks(ServerLevel level, Vec3 center,
                                   double radius, int rayCount) {
        BlockState floor = level.getBlockState(BlockPos.containing(center).below());
        for (int ray = 0; ray < rayCount; ray++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            for (double distance = 0.4D; distance <= radius; distance += 0.42D) {
                angle += (random.nextDouble() - 0.5D) * 0.22D;
                double x = center.x + Math.cos(angle) * distance;
                double z = center.z + Math.sin(angle) * distance;
                double y = center.y + 0.08D;
                level.sendParticles(ModParticles.METHOD_QI_EARTH.get(),
                        x, y, z, 1, 0.03D, 0.02D, 0.03D, 0.0D);
                if (random.nextBoolean()) {
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor),
                            x, y, z, 1, 0.08D, 0.08D, 0.08D, 0.04D);
                }
            }
        }
    }

    private void emitRisingRocks(ServerLevel level) {
        BlockState floor = level.getBlockState(blockPosition().below());
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor),
                getX(), getY() + 0.35D, getZ(),
                26, 1.0D, 0.25D, 1.0D, 0.18D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                getX(), getY() + 0.65D, getZ(),
                16, 0.8D, 0.4D, 0.8D, 0.09D);
    }

    private void emitBurrowTrail(ServerLevel level) {
        BlockState floor = level.getBlockState(blockPosition().below());
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor),
                getX(), getY() + 0.12D, getZ(),
                8, 0.45D, 0.15D, 0.45D, 0.1D);
        if ((tickCount & 1) == 0) {
            level.sendParticles(ModParticles.METHOD_QI_EARTH.get(),
                    getX(), getY() + 0.1D, getZ(),
                    3, 0.35D, 0.08D, 0.35D, 0.03D);
        }
    }

    private void emitEruptionWarning(ServerLevel level, Vec3 point) {
        emitJaggedCracks(level, point, 2.2D, 5);
        level.playSound(null, BlockPos.containing(point), SoundEvents.POINTED_DRIPSTONE_LAND,
                SoundSource.HOSTILE, 0.9F, 0.65F);
    }

    private void emitEruption(ServerLevel level) {
        BlockState floor = level.getBlockState(blockPosition().below());
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor),
                getX(), getY() + 0.2D, getZ(),
                58, 1.35D, 0.45D, 1.35D, 0.28D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                getX(), getY() + 0.8D, getZ(),
                22, 0.9D, 0.75D, 0.9D, 0.14D);
        level.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 1.2F, 0.82F);
    }

    private void setBurrowed(boolean value) {
        entityData.set(BURROWED, value);
        setInvisible(value);
        noPhysics = value;
        setNoGravity(value);
        if (!value) {
            noPhysics = false;
            setNoGravity(false);
        }
    }

    public byte getActionState() {
        return entityData.get(ACTION_STATE);
    }

    public int getActionTicks() {
        return entityData.get(ACTION_TICKS);
    }

    public int getPlateMask() {
        return entityData.get(PLATE_MASK);
    }

    public boolean hasPlate(int index) {
        return (getPlateMask() & (1 << index)) != 0;
    }

    public boolean hasAnyPlate() {
        return getPlateMask() != 0;
    }

    public int brokenPlateCount() {
        return 3 - Integer.bitCount(getPlateMask() & ALL_PLATES);
    }

    public boolean isFrenzied() {
        return entityData.get(FRENZIED);
    }

    public boolean isBurrowed() {
        return entityData.get(BURROWED);
    }

    public String cultivationRealm() {
        return CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_MID;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("PlateMask", getPlateMask());
        tag.putFloat("FrontPlateDamage", plateDamage[0]);
        tag.putFloat("MiddlePlateDamage", plateDamage[1]);
        tag.putFloat("RearPlateDamage", plateDamage[2]);
        tag.putInt("ClawCooldown", clawCooldown);
        tag.putInt("ChargeCooldown", chargeCooldown);
        tag.putInt("StompCooldown", stompCooldown);
        tag.putInt("VolleyCooldown", volleyCooldown);
        tag.putInt("BurrowCooldown", burrowCooldown);
        tag.putInt("ShakeCooldown", shakeCooldown);
        tag.putBoolean("FrenzyTriggered", frenzyTriggered);
        tag.putInt("FrenzyTicks", frenzyTicks);
        tag.putBoolean("Frenzied", isFrenzied());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(PLATE_MASK, tag.contains("PlateMask")
                ? tag.getInt("PlateMask") & ALL_PLATES : ALL_PLATES);
        plateDamage[0] = Mth.clamp(tag.getFloat("FrontPlateDamage"), 0.0F, PLATE_DURABILITY);
        plateDamage[1] = Mth.clamp(tag.getFloat("MiddlePlateDamage"), 0.0F, PLATE_DURABILITY);
        plateDamage[2] = Mth.clamp(tag.getFloat("RearPlateDamage"), 0.0F, PLATE_DURABILITY);
        clawCooldown = Math.max(0, tag.getInt("ClawCooldown"));
        chargeCooldown = Math.max(0, tag.getInt("ChargeCooldown"));
        stompCooldown = Math.max(0, tag.getInt("StompCooldown"));
        volleyCooldown = Math.max(0, tag.getInt("VolleyCooldown"));
        burrowCooldown = Math.max(0, tag.getInt("BurrowCooldown"));
        shakeCooldown = Math.max(0, tag.getInt("ShakeCooldown"));
        frenzyTriggered = tag.getBoolean("FrenzyTriggered");
        frenzyTicks = Math.max(0, tag.getInt("FrenzyTicks"));
        entityData.set(FRENZIED, tag.getBoolean("Frenzied") && frenzyTicks > 0);
        entityData.set(BURROWED, false);
        entityData.set(ACTION_STATE, ACTION_NONE);
        entityData.set(ACTION_TICKS, 0);
        setInvisible(false);
        noPhysics = false;
        setNoGravity(false);
        updateCombatAttributes();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> {
            if (isDeadOrDying()) {
                return state.setAndContinue(DEATH_ANIM);
            }
            return switch (getActionState()) {
                case ACTION_CLAW_SLAM -> state.setAndContinue(CLAW_ANIM);
                case ACTION_HORN_CHARGE -> state.setAndContinue(CHARGE_ANIM);
                case ACTION_STONE_STOMP -> state.setAndContinue(STOMP_ANIM);
                case ACTION_ROCK_VOLLEY -> state.setAndContinue(VOLLEY_ANIM);
                case ACTION_BURROW -> state.setAndContinue(BURROW_ANIM);
                case ACTION_ARMOR_SHAKE -> state.setAndContinue(SHAKE_ANIM);
                case ACTION_FRENZY -> state.setAndContinue(FRENZY_ANIM);
                case ACTION_STUN -> state.setAndContinue(STUN_ANIM);
                default -> state.isMoving()
                        ? state.setAndContinue(isFrenzied() ? RUN_ANIM : WALK_ANIM)
                        : state.setAndContinue(IDLE_ANIM);
            };
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
