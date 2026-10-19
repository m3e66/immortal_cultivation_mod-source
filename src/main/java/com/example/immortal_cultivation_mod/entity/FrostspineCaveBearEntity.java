package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.ForcefieldCenterBehavior;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 霜脊穴熊: a Foundation Establishment middle-stage ice/earth territorial beast.
 * Four visible frost ridges independently absorb 70 upper-back damage.
 */
public final class FrostspineCaveBearEntity extends RealmBeastMonster implements GeoEntity {
    // Visuals use synchronized BeastAttackVfx; server combat and sounds remain unchanged.
    public static final float MODEL_SCALE = 2.0F;
    public static final float BODY_WIDTH = 2.8F * MODEL_SCALE;
    public static final float BODY_HEIGHT = 2.2F * MODEL_SCALE;

    private static final EntityDataAccessor<Byte> ACTION_STATE =
            SynchedEntityData.defineId(FrostspineCaveBearEntity.class,
                    EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ACTION_TICKS =
            SynchedEntityData.defineId(FrostspineCaveBearEntity.class,
                    EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RIDGE_MASK =
            SynchedEntityData.defineId(FrostspineCaveBearEntity.class,
                    EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FRENZIED =
            SynchedEntityData.defineId(FrostspineCaveBearEntity.class,
                    EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IN_FOG =
            SynchedEntityData.defineId(FrostspineCaveBearEntity.class,
                    EntityDataSerializers.BOOLEAN);

    public static final byte ACTION_NONE = 0;
    public static final byte ACTION_PAW_SLAM = 1;
    public static final byte ACTION_FROST_ROAR = 2;
    public static final byte ACTION_ICE_SPINE_LINE = 3;
    public static final byte ACTION_AVALANCHE_LEAP = 4;
    public static final byte ACTION_FOG = 5;
    public static final byte ACTION_RIDGE_BURST = 6;
    public static final byte ACTION_FRENZY = 7;
    public static final byte ACTION_STAGGER = 8;

    private static final int PAW_TICKS = 23;
    private static final int ROAR_TICKS = 34;
    private static final int SPINE_TICKS = 48;
    private static final int LEAP_TICKS = 52;
    private static final int FOG_ACTION_TICKS = 18;
    private static final int BURST_TICKS = 29;
    private static final int FRENZY_ACTION_TICKS = 32;

    private static final int PAW_COOLDOWN = 100;
    private static final int ROAR_COOLDOWN = 220;
    private static final int SPINE_COOLDOWN = 180;
    private static final int LEAP_COOLDOWN = 260;
    private static final int FOG_COOLDOWN = 320;
    private static final int BURST_COOLDOWN = 360;
    private static final int FOG_DURATION = 120;
    private static final int FRENZY_DURATION = 200;
    private static final int ALL_RIDGES = 0b1111;
    private static final float RIDGE_DURABILITY = 70.0F;
    private static final double BASE_SPEED = 0.22D;

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin()
            .thenLoop("animation.frostspine_cave_bear.idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin()
            .thenLoop("animation.frostspine_cave_bear.walk");
    private static final RawAnimation RUN_ANIM = RawAnimation.begin()
            .thenLoop("animation.frostspine_cave_bear.run");
    private static final RawAnimation PAW_ANIM = RawAnimation.begin()
            .thenPlay("animation.frostspine_cave_bear.paw_slam");
    private static final RawAnimation ROAR_ANIM = RawAnimation.begin()
            .thenPlay("animation.frostspine_cave_bear.frost_roar");
    private static final RawAnimation SPINE_ANIM = RawAnimation.begin()
            .thenPlay("animation.frostspine_cave_bear.ice_spine_line");
    private static final RawAnimation LEAP_ANIM = RawAnimation.begin()
            .thenPlay("animation.frostspine_cave_bear.avalanche_leap");
    private static final RawAnimation FOG_ANIM = RawAnimation.begin()
            .thenLoop("animation.frostspine_cave_bear.fog_stalk");
    private static final RawAnimation BURST_ANIM = RawAnimation.begin()
            .thenPlay("animation.frostspine_cave_bear.ridge_burst");
    private static final RawAnimation FRENZY_ANIM = RawAnimation.begin()
            .thenPlay("animation.frostspine_cave_bear.frenzy");
    private static final RawAnimation STAGGER_ANIM = RawAnimation.begin()
            .thenPlay("animation.frostspine_cave_bear.stagger");
    private static final RawAnimation DEATH_ANIM = RawAnimation.begin()
            .thenPlayAndHold("animation.frostspine_cave_bear.death");

    private final AnimatableInstanceCache animationCache =
            GeckoLibUtil.createInstanceCache(this);
    private final float[] ridgeDamage = new float[4];
    private final Set<UUID> spineVictims = new HashSet<>();

    private int pawCooldown;
    private int roarCooldown;
    private int spineCooldown;
    private int leapCooldown;
    private int fogCooldown;
    private int burstCooldown;
    private int fogTicks;
    private int ridgeArmorBuffTicks;
    private int frenzyTicks;
    private boolean frenzyTriggered;
    private boolean fogAmbushReady;
    private boolean leapLaunched;
    private boolean leapImpacted;
    private Vec3 leapDirection = Vec3.ZERO;
    private Vec3 frozenGroundCenter = Vec3.ZERO;
    private int frozenGroundTicks;

    public FrostspineCaveBearEntity(
            EntityType<? extends FrostspineCaveBearEntity> type, Level level) {
        super(type, level);
        xpReward = 135;
        noCulling = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 780.0D)
                .add(Attributes.ATTACK_DAMAGE, 65.0D)
                .add(Attributes.ARMOR, 20.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 5.0D)
                .add(Attributes.MOVEMENT_SPEED, BASE_SPEED)
                .add(Attributes.ATTACK_SPEED, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 17.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.65D);
    }

    public static boolean checkSpawnRules(
            EntityType<FrostspineCaveBearEntity> type,
            ServerLevelAccessor level, MobSpawnType spawnType,
            BlockPos pos, RandomSource random) {
        if (!Monster.checkAnyLightMonsterSpawnRules(type, level, spawnType, pos, random)) {
            return false;
        }
        BlockPos floor = pos.below();
        return level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                && level.getFluidState(pos).isEmpty()
                && level.getFluidState(pos.above()).isEmpty()
                // Surface populations stay in high cold terrain; lower spawns
                // are reserved for the frozen caves beneath those biomes.
                && (pos.getY() >= 72 || !level.canSeeSky(pos));
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.03D, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.65D));
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
        builder.define(RIDGE_MASK, ALL_RIDGES);
        builder.define(FRENZIED, false);
        builder.define(IN_FOG, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide || !isAlive()) return;

        // Constant heavy white breath and faint cold leakage from the ridges.
        if (tickCount % 5 == 0) {
            Vec3 forward = horizontalLook();
            Vec3 muzzle = position().add(forward.scale(1.45D * MODEL_SCALE))
                    .add(0.0D, 1.15D * MODEL_SCALE, 0.0D);

        }

    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || !isAlive()) return;
        if (BeastDazeControl.blocksMoves(this)) {
            finishAction();
            return;
        }

        decrementCooldowns();
        tickTimedStates();
        if (!frenzyTriggered && getHealth() <= getMaxHealth() * 0.25F) {
            activateFrenzy();
        }
        if (getActionState() != ACTION_NONE) {
            tickCurrentAction();
            return;
        }
        if ((tickCount + getId()) % 5 != 0) return;

        LivingEntity target = getTarget();
        if (!canAttackTarget(target)) return;
        double distance = distanceTo(target);
        int nearby = level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(6.0D), this::canAttackTarget).size();

        // Close: physical pressure and the non-consuming ridge counterburst.
        if (distance <= 4.0D) {
            if (burstCooldown <= 0 && hasAnyRidge()
                    && (nearby >= 2 || random.nextFloat() < 0.28F)) {
                burstCooldown = BURST_COOLDOWN;
                beginAction(ACTION_RIDGE_BURST, BURST_TICKS, target);
            } else if (pawCooldown <= 0) {
                pawCooldown = isFrenzied() ? 75 : PAW_COOLDOWN;
                beginAction(ACTION_PAW_SLAM, PAW_TICKS, target);
            }
            return;
        }

        // Medium: roar for control or the committed whole-body pounce.
        if (distance <= 8.0D) {
            if (roarCooldown <= 0 && (nearby >= 2 || random.nextFloat() < 0.38F)) {
                roarCooldown = ROAR_COOLDOWN;
                beginAction(ACTION_FROST_ROAR, ROAR_TICKS, target);
            } else if (leapCooldown <= 0 && hasLineOfSight(target)) {
                leapCooldown = LEAP_COOLDOWN;
                beginAction(ACTION_AVALANCHE_LEAP, LEAP_TICKS, target);
            }
            return;
        }

        // Long: advancing ground spikes or concealment against ranged targets.
        if (distance <= 12.0D) {
            if (fogCooldown <= 0 && (!hasLineOfSight(target)
                    || getHealth() < getMaxHealth() * 0.60F
                    || random.nextFloat() < 0.32F)) {
                fogCooldown = FOG_COOLDOWN;
                activateFog();
                beginAction(ACTION_FOG, FOG_ACTION_TICKS, target);
            } else if (spineCooldown <= 0 && hasLineOfSight(target)) {
                spineCooldown = SPINE_COOLDOWN;
                beginAction(ACTION_ICE_SPINE_LINE, SPINE_TICKS, target);
            }
        }
    }

    private void decrementCooldowns() {
        if (pawCooldown > 0) pawCooldown--;
        if (roarCooldown > 0) roarCooldown--;
        if (spineCooldown > 0) spineCooldown--;
        if (leapCooldown > 0) leapCooldown--;
        if (fogCooldown > 0) fogCooldown--;
        if (burstCooldown > 0) burstCooldown--;
    }

    private void tickTimedStates() {
        if (fogTicks > 0) {
            fogTicks--;
            if (level() instanceof ServerLevel level) emitFog(level);
            if (fogTicks == 0) {
                entityData.set(IN_FOG, false);
                fogAmbushReady = false;
                updateCombatAttributes();
            }
        }
        if (ridgeArmorBuffTicks > 0 && --ridgeArmorBuffTicks == 0) {
            updateCombatAttributes();
        }
        if (frenzyTicks > 0 && --frenzyTicks == 0) {
            entityData.set(FRENZIED, false);
            updateCombatAttributes();
        }
        tickFrozenGround();
    }

    private void activateFog() {
        fogTicks = FOG_DURATION;
        fogAmbushReady = true;
        entityData.set(IN_FOG, true);
        updateCombatAttributes();
        if (level() instanceof ServerLevel level) {
            level.playSound(null, blockPosition(), SoundEvents.POWDER_SNOW_STEP,
                    SoundSource.HOSTILE, 1.2F, 0.58F);
        }
    }

    private void activateFrenzy() {
        frenzyTriggered = true;
        frenzyTicks = FRENZY_DURATION;
        entityData.set(FRENZIED, true);
        updateCombatAttributes();
        beginAction(ACTION_FRENZY, FRENZY_ACTION_TICKS, getTarget());
        if (level() instanceof ServerLevel level) {
            level.playSound(null, blockPosition(), SoundEvents.POLAR_BEAR_WARNING,
                    SoundSource.HOSTILE, 1.5F, 0.68F);
        }
    }

    private void beginAction(byte action, int ticks, LivingEntity target) {
        if (BeastDazeControl.blocksMoves(this)) {
            return;
        }
        entityData.set(ACTION_STATE, action);
        entityData.set(ACTION_TICKS, ticks);
        getNavigation().stop();
        if (action == ACTION_ICE_SPINE_LINE) {
            spineVictims.clear();
        } else if (action == ACTION_AVALANCHE_LEAP) {
            leapDirection = horizontalDirectionTo(target);
            leapLaunched = false;
            leapImpacted = false;
        }
    }

    private void tickCurrentAction() {
        int ticks = getActionTicks();
        byte action = getActionState();
        LivingEntity target = getTarget();
        getNavigation().stop();

        if (action != ACTION_AVALANCHE_LEAP) {
            Vec3 motion = getDeltaMovement();
            setDeltaMovement(motion.x * 0.18D, motion.y, motion.z * 0.18D);
        }
        if (canAttackTarget(target) && action != ACTION_AVALANCHE_LEAP) {
            lookAt(target, 30.0F, 25.0F);
        }
        if (level() instanceof ServerLevel level) {
            switch (action) {
                case ACTION_PAW_SLAM -> tickPawSlam(level, ticks);
                case ACTION_FROST_ROAR -> tickFrostRoar(level, ticks);
                case ACTION_ICE_SPINE_LINE -> tickIceSpineLine(level, ticks);
                case ACTION_AVALANCHE_LEAP -> tickAvalancheLeap(level, ticks);
                case ACTION_RIDGE_BURST -> tickRidgeBurst(level, ticks);
                case ACTION_FRENZY -> tickFrenzyAction(level, ticks);
                case ACTION_STAGGER -> setDeltaMovement(Vec3.ZERO);
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

    private void tickPawSlam(ServerLevel level, int ticks) {
        if (ticks == 15) {
            emitPawFrost(level);
            level.playSound(null, blockPosition(), SoundEvents.POLAR_BEAR_WARNING,
                    SoundSource.HOSTILE, 1.0F, 0.82F);
        }
        if (ticks != 9) return;
        Vec3 center = position().add(horizontalLook().scale(1.65D)).add(0, .45D, 0);
        for (LivingEntity target : targetsIn(center, 2.8D, 1.7D)) {
            if (damageTarget(target, scaledDamage(95.0F, true))) {
                target.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true, true), this);
                target.addEffect(new MobEffectInstance(
                        ModEffects.DAZE, 10, 0, false, true, true), this);
                knockAway(target, 1.1D, .16D);
            }
        }
        emitFrostImpact(level, center, 2.4D, 28);
        level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 1.15F, .76F);
    }

    private void tickFrostRoar(ServerLevel level, int ticks) {
        if (ticks > 18 && ticks % 3 == 0) {
            Vec3 mouth = position().add(horizontalLook().scale(1.35D)).add(0, 1.2D, 0);
        }
        if (ticks != 18) return;
        for (LivingEntity target : targetsIn(position(), 6.0D, 3.0D)) {
            if (damageTarget(target, scaledDamage(55.0F, true))) {
                target.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, true, true), this);
                target.addEffect(new MobEffectInstance(
                        ModEffects.DAZE, 6, 0, false, true, true), this);
                knockAway(target, .48D, .04D);
            }
        }
        for (double radius = 1.0D; radius <= 6.0D; radius += .65D) {
            emitColdMistRing(level, position().add(0, .22D, 0), radius, 14);
        }
        level.playSound(null, blockPosition(), SoundEvents.POLAR_BEAR_WARNING,
                SoundSource.HOSTILE, 1.55F, .55F);
    }

    private void tickIceSpineLine(ServerLevel level, int ticks) {
        int index = switch (ticks) {
            case 30 -> 0;
            case 26 -> 1;
            case 22 -> 2;
            case 18 -> 3;
            case 14 -> 4;
            default -> -1;
        };
        if (index < 0) return;
        Vec3 center = position().add(horizontalLook().scale(2.0D + index * 2.0D));
        double radius = 1.25D + index * .08D;
        for (LivingEntity target : targetsIn(center, radius, 2.3D)) {
            if (!spineVictims.add(target.getUUID())) continue;
            if (damageTarget(target, scaledDamage(80.0F, true))) {
                target.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true, true), this);
                Vec3 motion = target.getDeltaMovement();
                target.setDeltaMovement(motion.x, Math.max(.42D, motion.y), motion.z);
            }
        }
        BeastMoveBlockBreaker.breakSphere(level, this,
                center.add(0.0D, 0.35D, 0.0D),
                1.3D, 1.5D, 8, 5.0F);
        emitIceEruption(level, center, index);
        level.playSound(null, BlockPos.containing(center), SoundEvents.GLASS_BREAK,
                SoundSource.HOSTILE, .95F, .78F + index * .06F);
    }

    private void tickAvalancheLeap(ServerLevel level, int ticks) {
        if (ticks > 40) {
            setDeltaMovement(Vec3.ZERO);
            if ((ticks & 1) == 0) emitTakeoffDust(level);
            return;
        }
        if (!leapLaunched) {
            leapLaunched = true;
            setDeltaMovement(leapDirection.scale(.64D).add(0, .76D, 0));
            hasImpulse = true;
            level.playSound(null, blockPosition(), SoundEvents.POLAR_BEAR_WARNING,
                    SoundSource.HOSTILE, 1.15F, .72F);
        }
        if (!leapImpacted && ((onGround() && ticks < 34) || ticks == 15)) {
            avalancheImpact(level);
        }
    }

    private void avalancheImpact(ServerLevel level) {
        leapImpacted = true;
        setDeltaMovement(Vec3.ZERO);
        for (LivingEntity target : targetsIn(position(), 4.0D, 2.6D)) {
            double distance = target.position().distanceTo(position());
            float damage = distance <= 1.65D ? 135.0F : 75.0F;
            if (damageTarget(target, scaledDamage(damage, true))) {
                knockAway(target, distance <= 1.65D ? 1.75D : 1.1D,
                        distance <= 1.65D ? .38D : .18D);
                if (distance <= 1.65D) {
                    target.addEffect(new MobEffectInstance(
                            ModEffects.DAZE, 20, 0, false, true, true), this);
                }
            }
        }
        BeastMoveBlockBreaker.breakSphere(level, this,
                position().add(0.0D, 0.35D, 0.0D),
                4.0D, 2.0D, 48, 8.0F);
        frozenGroundCenter = position();
        frozenGroundTicks = 80;
        emitFrostImpact(level, position(), 4.0D, 70);
        level.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 1.45F, .62F);
    }

    private void tickRidgeBurst(ServerLevel level, int ticks) {

        if (ticks != 14) return;
        for (LivingEntity target : targetsIn(position(), 5.0D, 2.7D)) {
            if (damageTarget(target, scaledDamage(65.0F, true))) {
                target.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN, 60, 0, false, true, true), this);
                knockAway(target, 1.65D, .14D);
            }
        }
        ridgeArmorBuffTicks = 80;
        updateCombatAttributes();
        int intensity = Math.max(1, ridgeCount());
        for (double radius = .8D; radius <= 5.0D; radius += .55D) {
            emitColdMistRing(level, position().add(0, .18D, 0), radius,
                    3 + intensity * 2);
        }
        level.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK,
                SoundSource.HOSTILE, 1.35F, .64F);
    }

    private void tickFrenzyAction(ServerLevel level, int ticks) {

    }

    private void tickFrozenGround() {
        if (frozenGroundTicks <= 0 || frozenGroundCenter == Vec3.ZERO) return;
        frozenGroundTicks--;
        for (LivingEntity target : targetsIn(frozenGroundCenter, 4.0D, 1.7D)) {
            target.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, 8, 0, false, false, true), this);
            Vec3 motion = target.getDeltaMovement();
            target.setDeltaMovement(motion.x * 1.035D, motion.y, motion.z * 1.035D);
            target.hurtMarked = true;
        }
        if (level() instanceof ServerLevel level && frozenGroundTicks % 4 == 0) {
            for (int i = 0; i < 12; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double radius = Math.sqrt(random.nextDouble()) * 4.0D;
                double x = frozenGroundCenter.x + Math.cos(angle) * radius;
                double z = frozenGroundCenter.z + Math.sin(angle) * radius;
            }
        }
        if (frozenGroundTicks == 0) frozenGroundCenter = Vec3.ZERO;
    }

    private void finishAction() {
        entityData.set(ACTION_STATE, ACTION_NONE);
        entityData.set(ACTION_TICKS, 0);
        spineVictims.clear();
        leapDirection = Vec3.ZERO;
        leapLaunched = false;
        leapImpacted = false;
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        LivingEntity target = ModelBonePartEntity.resolveLiving(entity);
        if (BeastDazeControl.blocksMoves(this)
                || !canAttackTarget(target) || getActionState() != ACTION_NONE) return false;
        float damage = 65.0F * (isFrenzied() ? 1.15F : 1.0F);
        if (fogAmbushReady) {
            damage += 40.0F;
            fogAmbushReady = false;
        }
        boolean hit = damageTarget(target, damage);
        if (hit) knockAway(target, .62D, .06D);
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Dense fog does not make the bear invisible; it makes ranged aim unreliable.
        if (isInFog() && source.is(DamageTypeTags.IS_PROJECTILE)
                && random.nextFloat() < .35F) {

            return false;
        }
        float adjusted = applyElementResistance(source, amount);
        int ridge = ridgeForHit(source);
        if (ridge >= 0 && hasRidge(ridge)) {
            float remaining = Math.max(0.0F, RIDGE_DURABILITY - ridgeDamage[ridge]);
            float absorbed = Math.min(remaining, adjusted);
            ridgeDamage[ridge] += absorbed;
            adjusted -= absorbed;
            emitRidgeHit(ridge, absorbed);
            if (ridgeDamage[ridge] >= RIDGE_DURABILITY - .001F) breakRidge(ridge);
            if (adjusted <= .001F) {
                if (source.getEntity() instanceof LivingEntity attacker) {
                    setLastHurtByMob(attacker);
                    setTarget(attacker);
                }
                level().broadcastEntityEvent(this, (byte) 2);
                return true;
            }
        }
        return super.hurt(source, adjusted);
    }

    private float applyElementResistance(DamageSource source, float amount) {
        Entity direct = source.getDirectEntity();
        if (source.is(DamageTypeTags.IS_FIRE)
                || direct instanceof FireballProjectileEntity
                || direct instanceof FireballTalismanProjectileEntity
                || direct instanceof CrimsonFlameFireballEntity
                || direct instanceof IgniteFlareProjectileEntity
                || direct instanceof XinliaoFireballEntity) {
            return amount * 1.20F;
        }
        if (direct instanceof FrostCryProjectileEntity
                || direct instanceof ElementalTechniqueProjectileEntity projectile
                && projectile.kind() == ElementalTechniqueProjectileEntity.Kind.BINGZHUI_YU
                || direct instanceof ElementalTechniqueAreaEntity area
                && (area.kind() == ElementalTechniqueAreaEntity.Kind.BINGZHUI_YU
                || area.kind() == ElementalTechniqueAreaEntity.Kind.HANYU_FENGDI)
                || direct instanceof MethodTechniqueAreaEntity methodArea
                && methodArea.kind() == MethodTechniqueAreaEntity.Kind.HANYUAN_DOMAIN) {
            return amount * .60F;
        }
        if (direct instanceof ZhenshanPalmEntity
                || direct instanceof ShanbengSealEntity
                || direct instanceof TemporaryTerrainEntity
                || direct instanceof ElementalTechniqueAreaEntity area
                && (area.kind() == ElementalTechniqueAreaEntity.Kind.JINGJI_TUSHENG
                || area.kind() == ElementalTechniqueAreaEntity.Kind.SHANBENG_YIN)) {
            return amount * .75F;
        }
        return amount;
    }

    private int ridgeForHit(DamageSource source) {
        Vec3 sourcePos = source.getSourcePosition();
        if (sourcePos == null || sourcePos.y < getY() + getBbHeight() * .56D) return -1;
        Vec3 offset = sourcePos.subtract(position());
        Vec3 forward = horizontalLook();
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        double longitudinal = offset.dot(forward);
        double lateral = Math.abs(offset.dot(right));
        // Low frontal attacks strike the face/chest, not the dorsal ridges.
        if (longitudinal > 1.1D * MODEL_SCALE && lateral < .85D * MODEL_SCALE) return -1;
        if (longitudinal > .46875D * MODEL_SCALE) return 0;
        if (longitudinal > -.09375D * MODEL_SCALE) return 1;
        if (longitudinal > -.65625D * MODEL_SCALE) return 2;
        return 3;
    }

    private void emitRidgeHit(int ridge, float absorbed) {
        if (!(level() instanceof ServerLevel level)) return;
        Vec3 point = ridgeOffset(ridge);
        int count = Mth.clamp(Mth.ceil(absorbed / 4.5F), 4, 20);
        level.playSound(null, blockPosition(), SoundEvents.GLASS_HIT,
                SoundSource.HOSTILE, .75F, .65F + random.nextFloat() * .2F);
    }

    private void breakRidge(int ridge) {
        entityData.set(RIDGE_MASK, getRidgeMask() & ~(1 << ridge));
        updateCombatAttributes();
        if (level() instanceof ServerLevel level) {
            Vec3 point = ridgeOffset(ridge);
            level.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK,
                    SoundSource.HOSTILE, 1.3F, .58F);
        }
    }

    private Vec3 ridgeOffset(int ridge) {
        double along = switch (ridge) {
            case 0 -> .75D;
            case 1 -> .1875D;
            case 2 -> -.375D;
            default -> -.9375D;
        };
        return position().add(horizontalLook().scale(along * MODEL_SCALE))
                .add(0, 1.65D * MODEL_SCALE, 0);
    }

    private void updateCombatAttributes() {
        double speed = BASE_SPEED * (1.0D + brokenRidgeCount() * .03D);
        if (isInFog()) speed *= 1.15D;
        if (isFrenzied()) speed *= 1.20D;
        var movement = getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) movement.setBaseValue(speed);

        var attackSpeed = getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) attackSpeed.setBaseValue(isFrenzied() ? 4.8D : 4.0D);

        double armorValue = 20.0D - brokenRidgeCount() * 2.0D;
        if (ridgeArmorBuffTicks > 0) armorValue += 4.0D;
        if (isFrenzied()) armorValue *= .80D;
        var armor = getAttribute(Attributes.ARMOR);
        if (armor != null) armor.setBaseValue(armorValue);
    }

    private float scaledDamage(float damage, boolean iceAbility) {
        float scaled = damage * (isFrenzied() ? 1.15F : 1.0F);
        if (iceAbility && getRidgeMask() == 0) scaled *= .85F;
        return scaled;
    }

    private boolean damageTarget(LivingEntity target, float damage) {
        if (!canAttackTarget(target)
                || ForcefieldCenterBehavior.interceptAttackPath(
                this, getBoundingBox().getCenter(),
                target.getBoundingBox().getCenter(), damage)) return false;
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
        return level().getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(radius, yRadius, radius),
                this::canAttackTarget);
    }

    private void knockAway(LivingEntity target, double strength, double upward) {
        Vec3 away = target.position().subtract(position());
        away = new Vec3(away.x, 0, away.z);
        if (away.lengthSqr() < .001D) away = horizontalLook();
        away = away.normalize().scale(strength);
        target.setDeltaMovement(target.getDeltaMovement().scale(.35D)
                .add(away.x, upward, away.z));
        target.hurtMarked = true;
    }

    private Vec3 horizontalDirectionTo(LivingEntity target) {
        if (target == null) return horizontalLook();
        Vec3 direction = target.getBoundingBox().getCenter()
                .subtract(getBoundingBox().getCenter());
        direction = new Vec3(direction.x, 0, direction.z);
        return direction.lengthSqr() < .001D ? horizontalLook() : direction.normalize();
    }

    private Vec3 horizontalLook() {
        Vec3 look = getLookAngle();
        look = new Vec3(look.x, 0, look.z);
        return look.lengthSqr() < .001D
                ? Vec3.directionFromRotation(0, getYRot()).normalize()
                : look.normalize();
    }

    private void emitPawFrost(ServerLevel level) {
        Vec3 point = position().add(horizontalLook().scale(1.25D * MODEL_SCALE))
                .add(0, .75D * MODEL_SCALE, 0);
    }

    private void emitFrostImpact(ServerLevel level, Vec3 center, double radius, int count) {
        emitColdMistRing(level, center.add(0, .12D, 0), radius, 28);
    }

    private void emitIceEruption(ServerLevel level, Vec3 center, int index) {
        int count = 18 + index * 7;
    }

    private void emitTakeoffDust(ServerLevel level) {
        BlockState floor = level.getBlockState(blockPosition().below());
    }

    private void emitFog(ServerLevel level) {
        if ((tickCount & 1) != 0) return;
        for (int i = 0; i < 18; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = Math.sqrt(random.nextDouble()) * 5.0D;
            double x = getX() + Math.cos(angle) * radius;
            double z = getZ() + Math.sin(angle) * radius;
            double y = getY() + .12D + random.nextDouble() * .75D;
        }
    }

    private void emitColdMistRing(ServerLevel level, Vec3 center,
                                  double radius, int points) {
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
        }
    }

    public byte getActionState() {
        return entityData.get(ACTION_STATE);
    }

    public int getActionTicks() {
        return entityData.get(ACTION_TICKS);
    }

    public int getRidgeMask() {
        return entityData.get(RIDGE_MASK);
    }

    public boolean hasRidge(int index) {
        return (getRidgeMask() & (1 << index)) != 0;
    }

    public boolean hasAnyRidge() {
        return getRidgeMask() != 0;
    }

    public int ridgeCount() {
        return Integer.bitCount(getRidgeMask() & ALL_RIDGES);
    }

    public int brokenRidgeCount() {
        return 4 - ridgeCount();
    }

    public boolean isFrenzied() {
        return entityData.get(FRENZIED);
    }

    public boolean isInFog() {
        return entityData.get(IN_FOG);
    }

    public String cultivationRealm() {
        return beastCultivationLevel();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("RidgeMask", getRidgeMask());
        for (int i = 0; i < 4; i++) tag.putFloat("RidgeDamage" + i, ridgeDamage[i]);
        tag.putInt("PawCooldown", pawCooldown);
        tag.putInt("RoarCooldown", roarCooldown);
        tag.putInt("SpineCooldown", spineCooldown);
        tag.putInt("LeapCooldown", leapCooldown);
        tag.putInt("FogCooldown", fogCooldown);
        tag.putInt("BurstCooldown", burstCooldown);
        tag.putInt("FogTicks", fogTicks);
        tag.putInt("RidgeArmorBuffTicks", ridgeArmorBuffTicks);
        tag.putInt("FrenzyTicks", frenzyTicks);
        tag.putBoolean("FrenzyTriggered", frenzyTriggered);
        tag.putBoolean("FogAmbushReady", fogAmbushReady);
        tag.putInt("FrozenGroundTicks", frozenGroundTicks);
        if (frozenGroundTicks > 0) {
            tag.putDouble("FrozenGroundX", frozenGroundCenter.x);
            tag.putDouble("FrozenGroundY", frozenGroundCenter.y);
            tag.putDouble("FrozenGroundZ", frozenGroundCenter.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(RIDGE_MASK, tag.contains("RidgeMask")
                ? tag.getInt("RidgeMask") & ALL_RIDGES : ALL_RIDGES);
        for (int i = 0; i < 4; i++) {
            ridgeDamage[i] = Mth.clamp(tag.getFloat("RidgeDamage" + i),
                    0.0F, RIDGE_DURABILITY);
        }
        pawCooldown = Math.max(0, tag.getInt("PawCooldown"));
        roarCooldown = Math.max(0, tag.getInt("RoarCooldown"));
        spineCooldown = Math.max(0, tag.getInt("SpineCooldown"));
        leapCooldown = Math.max(0, tag.getInt("LeapCooldown"));
        fogCooldown = Math.max(0, tag.getInt("FogCooldown"));
        burstCooldown = Math.max(0, tag.getInt("BurstCooldown"));
        fogTicks = Math.max(0, tag.getInt("FogTicks"));
        ridgeArmorBuffTicks = Math.max(0, tag.getInt("RidgeArmorBuffTicks"));
        frenzyTicks = Math.max(0, tag.getInt("FrenzyTicks"));
        frenzyTriggered = tag.getBoolean("FrenzyTriggered");
        fogAmbushReady = tag.getBoolean("FogAmbushReady") && fogTicks > 0;
        frozenGroundTicks = Math.max(0, tag.getInt("FrozenGroundTicks"));
        frozenGroundCenter = frozenGroundTicks > 0
                ? new Vec3(tag.getDouble("FrozenGroundX"),
                tag.getDouble("FrozenGroundY"), tag.getDouble("FrozenGroundZ"))
                : Vec3.ZERO;
        entityData.set(FRENZIED, frenzyTicks > 0);
        entityData.set(IN_FOG, fogTicks > 0);
        entityData.set(ACTION_STATE, ACTION_NONE);
        entityData.set(ACTION_TICKS, 0);
        updateCombatAttributes();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> {
            if (isDeadOrDying()) return state.setAndContinue(DEATH_ANIM);
            return switch (getActionState()) {
                case ACTION_PAW_SLAM -> state.setAndContinue(PAW_ANIM);
                case ACTION_FROST_ROAR -> state.setAndContinue(ROAR_ANIM);
                case ACTION_ICE_SPINE_LINE -> state.setAndContinue(SPINE_ANIM);
                case ACTION_AVALANCHE_LEAP -> state.setAndContinue(LEAP_ANIM);
                case ACTION_FOG -> state.setAndContinue(FOG_ANIM);
                case ACTION_RIDGE_BURST -> state.setAndContinue(BURST_ANIM);
                case ACTION_FRENZY -> state.setAndContinue(FRENZY_ANIM);
                case ACTION_STAGGER -> state.setAndContinue(STAGGER_ANIM);
                default -> isInFog() ? state.setAndContinue(FOG_ANIM)
                        : state.isMoving()
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
