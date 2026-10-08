package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.combat.CombatStateManager;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.ForcefieldCenterBehavior;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Yuanying early-stage blood/earth martial beast with deliberate, stateful combat. */
public final class BloodApeDemonGeneralEntity extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Byte> ACTION_STATE = SynchedEntityData.defineId(
            BloodApeDemonGeneralEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ACTION_TICKS = SynchedEntityData.defineId(
            BloodApeDemonGeneralEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> BERSERK = SynchedEntityData.defineId(
            BloodApeDemonGeneralEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> YUANYING_GUARD = SynchedEntityData.defineId(
            BloodApeDemonGeneralEntity.class, EntityDataSerializers.BOOLEAN);

    public static final byte ACTION_NONE = 0;
    public static final byte ACTION_HEAVY_PUNCH = 1;
    public static final byte ACTION_EARTH_STOMP = 2;
    public static final byte ACTION_GRAB = 3;
    public static final byte ACTION_LEAP_KILL = 4;
    public static final byte ACTION_BERSERK_TRANSFORM = 5;

    private static final int HEAVY_PUNCH_TICKS = 26;
    private static final int EARTH_STOMP_TICKS = 29;
    private static final int GRAB_TICKS = 44;
    private static final int LEAP_KILL_TICKS = 43;
    private static final int BERSERK_TRANSFORM_TICKS = 38;
    private static final int GRAB_HOLD_TICKS = 20;
    private static final int YUANYING_GUARD_TICKS = 8 * 20;

    private static final float HEAVY_PUNCH_DAMAGE = 1_200.0F;
    private static final float HEAVY_PUNCH_SHOCKWAVE_DAMAGE = 360.0F;
    private static final float EARTH_STOMP_DAMAGE = 650.0F;
    private static final float GRAB_DAMAGE = 300.0F;
    private static final float THROW_DAMAGE = 450.0F;
    private static final float WALL_IMPACT_DAMAGE = 700.0F;
    private static final float LEAP_DAMAGE = 1_000.0F;

    private static final ResourceLocation BERSERK_MOVEMENT_ID = ResourceLocation.fromNamespaceAndPath(
            ImmortalCultivationMod.MODID, "blood_ape_berserk_movement");
    private static final ResourceLocation BERSERK_ATTACK_SPEED_ID = ResourceLocation.fromNamespaceAndPath(
            ImmortalCultivationMod.MODID, "blood_ape_berserk_attack_speed");

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin()
            .thenLoop("animation.xueyuan_yaojiang_remake.idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin()
            .thenLoop("animation.xueyuan_yaojiang_remake.walk");
    private static final RawAnimation RUN_ANIMATION = RawAnimation.begin()
            .thenLoop("animation.xueyuan_yaojiang_remake.run");
    private static final RawAnimation HEAVY_PUNCH_ANIMATION = RawAnimation.begin()
            .thenPlay("animation.xueyuan_yaojiang_remake.xuesha_zhongquan");
    private static final RawAnimation EARTH_STOMP_ANIMATION = RawAnimation.begin()
            .thenPlay("animation.xueyuan_yaojiang_remake.liedi_ta");
    private static final RawAnimation GRAB_ANIMATION = RawAnimation.begin()
            .thenPlay("animation.xueyuan_yaojiang_remake.yaoyuan_qinna");
    private static final RawAnimation LEAP_KILL_ANIMATION = RawAnimation.begin()
            .thenPlay("animation.xueyuan_yaojiang_remake.xueying_yuesha");
    private static final RawAnimation BERSERK_TRANSFORM_ANIMATION = RawAnimation.begin()
            .thenPlay("animation.xueyuan_yaojiang_remake.ranxue_kuanghua");
    private static final RawAnimation BERSERK_IDLE_ANIMATION = RawAnimation.begin()
            .thenLoop("animation.xueyuan_yaojiang_remake.berserk_idle");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int heavyPunchCooldown;
    private int earthStompCooldown;
    private int grabCooldown;
    private int leapCooldown;
    private int dodgeCooldown;
    private boolean yuanyingGuardUsed;
    private int yuanyingGuardTicks;
    private UUID actionTargetUuid;
    private UUID grabbedTargetUuid;
    private int grabHoldTicks;
    private UUID thrownTargetUuid;
    private int thrownCollisionTicks;
    private Vec3 leapAfterimagePosition;
    private float leapAfterimageYaw;
    private int leapAfterimageTicks;
    private boolean leapLaunched;
    private boolean leapWasAirborne;
    private boolean leapImpacted;

    public BloodApeDemonGeneralEntity(
            EntityType<? extends BloodApeDemonGeneralEntity> entityType, Level level) {
        super(entityType, level);
        xpReward = 2_500;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20_000.0D)
                .add(Attributes.ARMOR, 400.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 32.0D)
                .add(Attributes.ATTACK_DAMAGE, 550.0D)
                .add(Attributes.ATTACK_SPEED, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.FOLLOW_RANGE, 56.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9D)
                .add(Attributes.STEP_HEIGHT, 1.25D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new BloodApeCombatGoal(this));
        goalSelector.addGoal(6, new RandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));

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
        builder.define(BERSERK, false);
        builder.define(YUANYING_GUARD, false);
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
        heavyPunchCooldown = decrement(heavyPunchCooldown);
        earthStompCooldown = decrement(earthStompCooldown);
        grabCooldown = decrement(grabCooldown);
        leapCooldown = decrement(leapCooldown);
        dodgeCooldown = decrement(dodgeCooldown);

        updateBerserk();
        updateYuanyingGuard();
        tickThrownWallImpact();
        tickLeapAfterimage();
        if (getActionState() != ACTION_NONE) {
            tickCurrentAction();
        }
    }

    private void updateBerserk() {
        if (!isBerserk() && getHealth() <= getMaxHealth() * 0.40F) {
            setBerserk(true);
            finishAction();
            startAction(ACTION_BERSERK_TRANSFORM, BERSERK_TRANSFORM_TICKS, null);
            if (level() instanceof ServerLevel level) {
                level.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR,
                        SoundSource.HOSTILE, 2.2F, 0.65F);
                emitShockwave(level, position(), 4.0D, 48, true);
            }
        }
        if (!isBerserk()) {
            return;
        }
        if (tickCount % 20 == 0) {
            float bloodLoss = getMaxHealth() * 0.0015F;
            setHealth(Math.max(1.0F, getHealth() - bloodLoss));
        }
        if (level() instanceof ServerLevel level && (tickCount & 1) == 0) {
            level.sendParticles(ModParticles.METHOD_QI_BLOOD.get(),
                    getX(), getY() + getBbHeight() * 0.55D, getZ(),
                    4, 0.55D, 0.9D, 0.55D, 0.025D);
        }
    }

    private void updateYuanyingGuard() {
        if (!yuanyingGuardUsed && getHealth() <= getMaxHealth() * 0.12F) {
            activateYuanyingGuard();
        }
        if (!hasYuanyingGuard()) {
            return;
        }
        yuanyingGuardTicks = Math.max(0, yuanyingGuardTicks - 1);
        if (level() instanceof ServerLevel level && tickCount % 4 == 0) {
            Vec3 behind = position().subtract(horizontalDirection(getYRot()).scale(1.45D));
            emitApeSilhouette(level, behind, getYRot(), 1.75D, true);
        }
        if (yuanyingGuardTicks == 0) {
            entityData.set(YUANYING_GUARD, false);
        }
    }

    private void activateYuanyingGuard() {
        yuanyingGuardUsed = true;
        yuanyingGuardTicks = YUANYING_GUARD_TICKS;
        entityData.set(YUANYING_GUARD, true);
        if (level() instanceof ServerLevel level) {
            level.playSound(null, blockPosition(), SoundEvents.TOTEM_USE,
                    SoundSource.HOSTILE, 1.8F, 0.55F);
            Vec3 behind = position().subtract(horizontalDirection(getYRot()).scale(1.45D));
            emitApeSilhouette(level, behind, getYRot(), 1.75D, true);
        }
    }

    private void setBerserk(boolean berserk) {
        entityData.set(BERSERK, berserk);
        updateBerserkModifier(Attributes.MOVEMENT_SPEED, BERSERK_MOVEMENT_ID, berserk, 0.45D);
        updateBerserkModifier(Attributes.ATTACK_SPEED, BERSERK_ATTACK_SPEED_ID, berserk, 0.55D);
    }

    private void updateBerserkModifier(
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            ResourceLocation id, boolean active, double amount) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(id);
        if (active && existing == null) {
            instance.addTransientModifier(new AttributeModifier(
                    id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else if (!active && existing != null) {
            instance.removeModifier(id);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean guardWasActive = hasYuanyingGuard();
        boolean shouldActivateGuard = !level().isClientSide && !yuanyingGuardUsed
                && getHealth() - amount <= getMaxHealth() * 0.12F
                && getHealth() > 1.0F;
        boolean hurt = super.hurt(source, guardWasActive ? amount * 0.55F : amount);
        if (hurt && shouldActivateGuard && isAlive()) {
            activateYuanyingGuard();
        }
        return hurt;
    }

    private void tickCurrentAction() {
        byte action = getActionState();
        int ticks = getActionTicks();
        LivingEntity target = resolveLiving(actionTargetUuid);
        getNavigation().stop();
        if (target != null && target.isAlive() && action != ACTION_EARTH_STOMP
                && action != ACTION_LEAP_KILL) {
            getLookControl().setLookAt(target, 45.0F, 45.0F);
        }
        if (level() instanceof ServerLevel level) {
            switch (action) {
                case ACTION_HEAVY_PUNCH -> tickHeavyPunch(level, ticks, target);
                case ACTION_EARTH_STOMP -> tickEarthStomp(level, ticks);
                case ACTION_GRAB -> tickGrab(level, ticks, target);
                case ACTION_LEAP_KILL -> tickLeapKill(level, ticks, target);
                case ACTION_BERSERK_TRANSFORM -> tickBerserkTransform(level, ticks);
                default -> finishAction();
            }
        }
        if (getActionState() != action) {
            return;
        }
        if (ticks <= 1) {
            finishAction();
        } else {
            entityData.set(ACTION_TICKS, ticks - 1);
        }
    }

    private void tickHeavyPunch(ServerLevel level, int ticks, LivingEntity target) {
        Vec3 forward = horizontalDirection(getYRot());
        Vec3 fist = position().add(0.0D, getBbHeight() * 0.66D, 0.0D)
                .add(forward.scale(0.95D));
        if (ticks > 11 && (ticks & 1) == 0) {
            level.sendParticles(ModParticles.METHOD_QI_BLOOD.get(),
                    fist.x, fist.y, fist.z, 8, 0.28D, 0.28D, 0.28D, 0.025D);
        }
        if (ticks != 12) {
            return;
        }
        swing(InteractionHand.MAIN_HAND, true);
        double reach = isBerserk() ? 5.2D : 4.0D;
        if (canHitAbilityTarget(target, reach, HEAVY_PUNCH_DAMAGE)) {
            CombatStateManager.hurtAsHeavy(
                    this, target, damageSources().mobAttack(this), HEAVY_PUNCH_DAMAGE);
            Vec3 away = target.position().subtract(position());
            target.knockback(3.4D, -away.x, -away.z);
        }
        AABB shockwave = getBoundingBox().inflate(3.0D, 1.4D, 3.0D);
        for (LivingEntity nearby : level.getEntitiesOfClass(
                LivingEntity.class, shockwave, this::canAttackWithAbility)) {
            if (nearby == target || distanceToSqr(nearby) > 9.0D
                    || !canHitAbilityTarget(nearby, 3.0D, HEAVY_PUNCH_SHOCKWAVE_DAMAGE)) {
                continue;
            }
            nearby.hurt(damageSources().mobAttack(this), HEAVY_PUNCH_SHOCKWAVE_DAMAGE);
            Vec3 away = nearby.position().subtract(position());
            nearby.knockback(1.3D, -away.x, -away.z);
        }
        emitShockwave(level, position(), 3.0D, 36, true);
        level.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 1.5F, 0.62F);
    }

    private void tickEarthStomp(ServerLevel level, int ticks) {
        if (ticks > 14 && ticks % 3 == 0) {
            level.sendParticles(ModParticles.METHOD_QI_EARTH.get(),
                    getX(), getY() + 0.15D, getZ(), 10,
                    0.75D, 0.15D, 0.75D, 0.02D);
        }
        if (ticks != 14) {
            return;
        }
        swing(InteractionHand.MAIN_HAND, true);
        AABB area = getBoundingBox().inflate(6.0D, 2.5D, 6.0D);
        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class, area, this::canAttackWithAbility)) {
            if (distanceToSqr(target) > 36.0D
                    || !canHitAbilityTarget(target, 6.0D, EARTH_STOMP_DAMAGE)) {
                continue;
            }
            target.hurt(damageSources().mobAttack(this), EARTH_STOMP_DAMAGE);
            Vec3 away = target.position().subtract(position());
            target.knockback(1.15D, -away.x, -away.z);
            Vec3 motion = target.getDeltaMovement();
            target.setDeltaMovement(motion.x, Math.max(1.0D, motion.y), motion.z);
            target.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, true, true), this);
        }
        BeastMoveBlockBreaker.breakBox(level, this,
                new AABB(getX() - 6.0D, getY() - 0.75D,
                        getZ() - 6.0D, getX() + 6.0D,
                        getY() + 0.8D, getZ() + 6.0D),
                position().add(0.0D, 0.1D, 0.0D), 32, 6.0F);
        emitShockwave(level, position(), 6.0D, 72, false);
        level.playSound(null, blockPosition(), SoundEvents.RAVAGER_STEP,
                SoundSource.HOSTILE, 2.2F, 0.5F);
    }

    private void tickGrab(ServerLevel level, int ticks, LivingEntity target) {
        LivingEntity grabbed = resolveLiving(grabbedTargetUuid);
        if (grabbed != null && grabbed.isAlive()) {
            if (com.example.immortal_cultivation_mod.combat.ThoughtControlGuard.hasProtection(grabbed)) {
                grabbedTargetUuid = null;
                grabHoldTicks = 0;
                finishAction();
                return;
            }
            holdGrabbedTarget(grabbed);
            if (--grabHoldTicks <= 0) {
                throwGrabbedTarget(level, grabbed);
                finishAction();
            }
            return;
        }
        grabbedTargetUuid = null;
        if (target == null || !target.isAlive() || ticks <= 22) {
            return;
        }
        Vec3 aim = target.position().subtract(position()).multiply(1.0D, 0.0D, 1.0D);
        if (aim.lengthSqr() > 0.01D) {
            Vec3 rush = aim.normalize().scale(isBerserk() ? 0.92D : 0.74D);
            setDeltaMovement(rush.x, getDeltaMovement().y, rush.z);
            hasImpulse = true;
        }
        if (distanceToSqr(target) > 2.8D * 2.8D
                || !canHitAbilityTarget(target, 3.0D, GRAB_DAMAGE)) {
            return;
        }
        target.hurt(damageSources().mobAttack(this), GRAB_DAMAGE);
        if (com.example.immortal_cultivation_mod.combat.ThoughtControlGuard.resistForDuration(
                target, "blood_ape_grab", this, GRAB_HOLD_TICKS)) {
            finishAction();
            return;
        }
        grabbedTargetUuid = target.getUUID();
        grabHoldTicks = GRAB_HOLD_TICKS;
        target.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN, GRAB_HOLD_TICKS + 5,
                10, false, false, true), this);
        level.playSound(null, blockPosition(), SoundEvents.IRON_GOLEM_ATTACK,
                SoundSource.HOSTILE, 1.4F, 0.7F);
    }

    private void holdGrabbedTarget(LivingEntity target) {
        Vec3 forward = horizontalDirection(getYRot());
        Vec3 held = position().add(forward.scale(1.25D))
                .add(0.0D, getBbHeight() * 0.43D, 0.0D);
        target.teleportTo(held.x, held.y, held.z);
        target.setDeltaMovement(Vec3.ZERO);
        target.resetFallDistance();
    }

    private void throwGrabbedTarget(ServerLevel level, LivingEntity target) {
        Vec3 forward = horizontalDirection(getYRot());
        target.hurt(damageSources().mobAttack(this), THROW_DAMAGE);
        target.setDeltaMovement(forward.scale(1.75D).add(0.0D, 0.62D, 0.0D));
        target.resetFallDistance();
        thrownTargetUuid = target.getUUID();
        thrownCollisionTicks = 20;
        grabbedTargetUuid = null;
        level.sendParticles(ModParticles.METHOD_QI_BLOOD.get(),
                target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                24, 0.45D, 0.6D, 0.45D, 0.12D);
        level.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                SoundSource.HOSTILE, 1.5F, 0.62F);
    }

    private void tickThrownWallImpact() {
        if (thrownTargetUuid == null || thrownCollisionTicks-- <= 0) {
            thrownTargetUuid = null;
            return;
        }
        LivingEntity target = resolveLiving(thrownTargetUuid);
        if (target == null || !target.isAlive()) {
            thrownTargetUuid = null;
            return;
        }
        if (!target.horizontalCollision) {
            return;
        }
        target.hurt(damageSources().mobAttack(this), WALL_IMPACT_DAMAGE);
        target.setDeltaMovement(target.getDeltaMovement().scale(0.18D));
        if (level() instanceof ServerLevel level) {
            BlockState state = level.getBlockState(target.blockPosition());
            level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(
                            net.minecraft.core.particles.ParticleTypes.BLOCK,
                            state.isAir() ? net.minecraft.world.level.block.Blocks.STONE.defaultBlockState() : state),
                    target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    36, 0.55D, 0.8D, 0.55D, 0.18D);
            level.playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.HOSTILE, 1.4F, 0.78F);
        }
        thrownTargetUuid = null;
    }

    private void tickLeapKill(ServerLevel level, int ticks, LivingEntity target) {
        if (!leapLaunched) {
            leapLaunched = true;
            leapWasAirborne = false;
            leapImpacted = false;
            leapAfterimagePosition = position();
            leapAfterimageYaw = getYRot();
            leapAfterimageTicks = 22;
            Vec3 targetPoint = target == null ? position().add(getLookAngle().scale(7.0D))
                    : target.position().add(target.getDeltaMovement().scale(7.0D));
            Vec3 horizontal = targetPoint.subtract(position()).multiply(1.0D, 0.0D, 1.0D);
            double distance = Math.max(1.0D, horizontal.length());
            double speed = Mth.clamp(distance / 15.0D, 0.45D, 1.05D);
            Vec3 direction = horizontal.normalize();
            setDeltaMovement(direction.x * speed, 1.05D, direction.z * speed);
            hasImpulse = true;
            level.playSound(null, blockPosition(), SoundEvents.HORSE_JUMP,
                    SoundSource.HOSTILE, 1.5F, 0.55F);
            return;
        }
        leapWasAirborne |= !onGround();
        if (target != null && target.isAlive() && ticks > 10) {
            Vec3 desired = target.position().subtract(position()).multiply(1.0D, 0.0D, 1.0D);
            if (desired.lengthSqr() > 0.25D) {
                Vec3 guided = desired.normalize().scale(
                        Math.max(0.48D, getDeltaMovement().horizontalDistance()));
                Vec3 motion = getDeltaMovement();
                setDeltaMovement(Mth.lerp(0.16D, motion.x, guided.x),
                        motion.y, Mth.lerp(0.16D, motion.z, guided.z));
            }
        }
        if (!leapImpacted && (leapWasAirborne && onGround() || ticks <= 11)) {
            leapImpacted = true;
            impactLeap(level);
            finishAction();
        }
    }

    private void impactLeap(ServerLevel level) {
        AABB area = getBoundingBox().inflate(4.25D, 2.5D, 4.25D);
        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class, area, this::canAttackWithAbility)) {
            if (distanceToSqr(target) > 4.25D * 4.25D
                    || !canHitAbilityTarget(target, 4.25D, LEAP_DAMAGE)) {
                continue;
            }
            CombatStateManager.hurtAsHeavy(
                    this, target, damageSources().mobAttack(this), LEAP_DAMAGE);
            Vec3 away = target.position().subtract(position());
            target.knockback(2.1D, -away.x, -away.z);
        }
        BeastMoveBlockBreaker.breakSphere(level, this,
                position().add(0.0D, 0.35D, 0.0D),
                4.0D, 2.0D, 48, 8.0F);
        emitShockwave(level, position(), 4.5D, 64, true);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                getX(), getY() + 0.4D, getZ(), 6, 1.2D, 0.35D, 1.2D, 0.05D);
        level.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 2.0F, 0.52F);
    }

    private void tickBerserkTransform(ServerLevel level, int ticks) {
        if ((ticks & 1) == 0) {
            double progress = 1.0D - ticks / (double) BERSERK_TRANSFORM_TICKS;
            level.sendParticles(ModParticles.METHOD_QI_BLOOD.get(),
                    getX(), getY() + getBbHeight() * (0.2D + progress * 0.55D), getZ(),
                    10, 0.65D + progress, 0.45D, 0.65D + progress, 0.06D);
        }
    }

    private void tickLeapAfterimage() {
        if (leapAfterimagePosition == null || leapAfterimageTicks-- <= 0) {
            leapAfterimagePosition = null;
            return;
        }
        if (level() instanceof ServerLevel level && leapAfterimageTicks % 3 == 0) {
            emitApeSilhouette(level, leapAfterimagePosition, leapAfterimageYaw, 1.0D, false);
        }
    }

    private void emitApeSilhouette(ServerLevel level, Vec3 base, float yaw,
                                   double scale, boolean spectralGuard) {
        Vec3 forward = horizontalDirection(yaw);
        Vec3 right = new Vec3(forward.z, 0.0D, -forward.x);
        for (double y = 0.25D; y <= 2.25D; y += 0.32D) {
            emitSilhouettePoint(level, base, right, forward, 0.0D, y * scale, 0.0D,
                    spectralGuard);
        }
        for (int index = 0; index < 10; index++) {
            double angle = Math.PI * 2.0D * index / 10.0D;
            emitSilhouettePoint(level, base, right, forward,
                    Math.cos(angle) * 0.34D * scale,
                    2.52D * scale + Math.sin(angle) * 0.34D * scale,
                    0.0D, spectralGuard);
        }
        for (int side = -1; side <= 1; side += 2) {
            for (int segment = 1; segment <= 5; segment++) {
                emitSilhouettePoint(level, base, right, forward,
                        side * segment * 0.24D * scale,
                        (2.05D - segment * 0.16D) * scale,
                        0.0D, spectralGuard);
                emitSilhouettePoint(level, base, right, forward,
                        side * segment * 0.14D * scale,
                        (0.35D + segment * 0.26D) * scale,
                        0.0D, spectralGuard);
            }
        }
    }

    private void emitSilhouettePoint(ServerLevel level, Vec3 base,
                                     Vec3 right, Vec3 forward,
                                     double localX, double localY, double localZ,
                                     boolean spectralGuard) {
        Vec3 point = base.add(right.scale(localX)).add(forward.scale(localZ))
                .add(0.0D, localY, 0.0D);
        level.sendParticles(ModParticles.METHOD_QI_BLOOD.get(),
                point.x, point.y, point.z, 1,
                spectralGuard ? 0.055D : 0.025D,
                spectralGuard ? 0.055D : 0.025D,
                spectralGuard ? 0.055D : 0.025D,
                spectralGuard ? 0.012D : 0.003D);
    }

    private void emitShockwave(ServerLevel level, Vec3 center,
                               double radius, int points, boolean blood) {
        for (int index = 0; index < points; index++) {
            double angle = Math.PI * 2.0D * index / points;
            Vec3 point = center.add(
                    Math.cos(angle) * radius, 0.18D,
                    Math.sin(angle) * radius);
            level.sendParticles(blood && index % 3 == 0
                            ? ModParticles.METHOD_QI_BLOOD.get()
                            : ModParticles.METHOD_QI_EARTH.get(),
                    point.x, point.y, point.z, 1,
                    0.04D, 0.08D, 0.04D, 0.025D);
        }
    }

    private boolean canHitAbilityTarget(LivingEntity target, double range, float damage) {
        return target != null && target.isAlive()
                && distanceToSqr(target) <= range * range
                && hasLineOfSight(target)
                && !ForcefieldCenterBehavior.interceptAttackPath(
                        this, getBoundingBox().getCenter(),
                        target.getBoundingBox().getCenter(), damage);
    }

    private boolean canAttackWithAbility(LivingEntity target) {
        if (target == null || target == this || !target.isAlive() || isAlliedTo(target)) {
            return false;
        }
        if (target instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }
        return target == getTarget();
    }

    private LivingEntity resolveLiving(UUID uuid) {
        if (uuid == null || !(level() instanceof ServerLevel level)) {
            return null;
        }
        Entity entity = level.getEntity(uuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    private void startAction(byte action, int duration, LivingEntity target) {
        if (BeastDazeControl.blocksMoves(this) || getActionState() != ACTION_NONE) {
            return;
        }
        entityData.set(ACTION_STATE, action);
        entityData.set(ACTION_TICKS, Math.max(1, duration));
        actionTargetUuid = target == null ? null : target.getUUID();
        getNavigation().stop();
        if (action == ACTION_LEAP_KILL) {
            leapLaunched = false;
            leapWasAirborne = false;
            leapImpacted = false;
        }
    }

    private void finishAction() {
        entityData.set(ACTION_STATE, ACTION_NONE);
        entityData.set(ACTION_TICKS, 0);
        actionTargetUuid = null;
        grabbedTargetUuid = null;
        grabHoldTicks = 0;
        leapLaunched = false;
        leapWasAirborne = false;
        leapImpacted = false;
    }

    private static int decrement(int value) {
        return value > 0 ? value - 1 : 0;
    }

    private static Vec3 horizontalDirection(float yawDegrees) {
        double radians = Math.toRadians(yawDegrees);
        return new Vec3(-Math.sin(radians), 0.0D, Math.cos(radians)).normalize();
    }

    private boolean isDangerousAction(LivingEntity target) {
        return target != null && (target.isUsingItem()
                || target instanceof ServerPlayer player
                && (CombatStateManager.isPreparingSpell(player)
                || CombatStateManager.isPreparingHeavyAttack(player)));
    }

    private boolean isBlocking(LivingEntity target) {
        return target != null && (target.isBlocking()
                || target instanceof ServerPlayer player
                && CombatStateManager.isBlocking(player));
    }

    private boolean tryDodge(LivingEntity target) {
        if (BeastDazeControl.blocksMoves(this) || dodgeCooldown > 0 || target == null) {
            return false;
        }
        Vec3 away = position().subtract(target.position()).multiply(1.0D, 0.0D, 1.0D);
        if (away.lengthSqr() < 0.01D) {
            away = horizontalDirection(getYRot()).scale(-1.0D);
        } else {
            away = away.normalize();
        }
        Vec3 side = new Vec3(-away.z, 0.0D, away.x)
                .scale(random.nextBoolean() ? 1.0D : -1.0D);
        Vec3 movement = side.scale(0.82D).add(away.scale(0.28D));
        if (!isSafeDodge(position().add(movement.scale(2.2D)))) {
            movement = side.scale(-0.82D).add(away.scale(0.28D));
        }
        if (!isSafeDodge(position().add(movement.scale(2.2D)))) {
            movement = away.scale(0.72D);
        }
        if (!isSafeDodge(position().add(movement.scale(2.0D)))) {
            return false;
        }
        setDeltaMovement(movement.x, onGround() ? 0.24D : getDeltaMovement().y, movement.z);
        hasImpulse = true;
        dodgeCooldown = isBerserk() ? 16 : 25;
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ModParticles.METHOD_QI_BLOOD.get(),
                    getX(), getY() + getBbHeight() * 0.5D, getZ(),
                    18, 0.45D, 0.75D, 0.45D, 0.08D);
        }
        return true;
    }

    private boolean retreatFrom(LivingEntity target) {
        Vec3 destination = DefaultRandomPos.getPosAway(this, 12, 5, target.position());
        if (destination == null) {
            return tryDodge(target);
        }
        getNavigation().moveTo(destination.x, destination.y, destination.z,
                isBerserk() ? 1.35D : 1.12D);
        return true;
    }

    private boolean isSafeDodge(Vec3 destination) {
        AABB moved = getBoundingBox().move(destination.subtract(position()));
        BlockPos below = BlockPos.containing(destination).below();
        return level().noCollision(this, moved)
                && level().getBlockState(below).isFaceSturdy(
                        level(), below, net.minecraft.core.Direction.UP);
    }

    private int nearbyEnemyCount() {
        return level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(6.0D), this::canAttackWithAbility).size();
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return !BeastDazeControl.blocksMoves(this)
                && getActionState() == ACTION_NONE
                && super.doHurtTarget(target);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    public byte getActionState() {
        return entityData.get(ACTION_STATE);
    }

    public int getActionTicks() {
        return entityData.get(ACTION_TICKS);
    }

    public boolean isBerserk() {
        return entityData.get(BERSERK);
    }

    public boolean hasYuanyingGuard() {
        return entityData.get(YUANYING_GUARD);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> switch (getActionState()) {
            case ACTION_HEAVY_PUNCH -> state.setAndContinue(HEAVY_PUNCH_ANIMATION);
            case ACTION_EARTH_STOMP -> state.setAndContinue(EARTH_STOMP_ANIMATION);
            case ACTION_GRAB -> state.setAndContinue(GRAB_ANIMATION);
            case ACTION_LEAP_KILL -> state.setAndContinue(LEAP_KILL_ANIMATION);
            case ACTION_BERSERK_TRANSFORM -> state.setAndContinue(BERSERK_TRANSFORM_ANIMATION);
            default -> {
                if (state.isMoving()) {
                    boolean running = isBerserk()
                            || getDeltaMovement().horizontalDistanceSqr() > 0.0225D;
                    yield state.setAndContinue(running ? RUN_ANIMATION : WALK_ANIMATION);
                }
                yield state.setAndContinue(isBerserk()
                        ? BERSERK_IDLE_ANIMATION : IDLE_ANIMATION);
            }
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("HeavyPunchCooldown", heavyPunchCooldown);
        tag.putInt("EarthStompCooldown", earthStompCooldown);
        tag.putInt("GrabCooldown", grabCooldown);
        tag.putInt("LeapCooldown", leapCooldown);
        tag.putInt("DodgeCooldown", dodgeCooldown);
        tag.putBoolean("Berserk", isBerserk());
        tag.putBoolean("YuanyingGuardUsed", yuanyingGuardUsed);
        tag.putInt("YuanyingGuardTicks", yuanyingGuardTicks);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        heavyPunchCooldown = Math.max(0, tag.getInt("HeavyPunchCooldown"));
        earthStompCooldown = Math.max(0, tag.getInt("EarthStompCooldown"));
        grabCooldown = Math.max(0, tag.getInt("GrabCooldown"));
        leapCooldown = Math.max(0, tag.getInt("LeapCooldown"));
        dodgeCooldown = Math.max(0, tag.getInt("DodgeCooldown"));
        setBerserk(tag.getBoolean("Berserk"));
        yuanyingGuardUsed = tag.getBoolean("YuanyingGuardUsed");
        yuanyingGuardTicks = Math.max(0, tag.getInt("YuanyingGuardTicks"));
        entityData.set(YUANYING_GUARD, yuanyingGuardTicks > 0);
        finishAction();
    }

    private static final class BloodApeCombatGoal extends Goal {
        private final BloodApeDemonGeneralEntity ape;
        private int decisionTicks;
        private int meleeTicks;
        private int flankTicks;
        private int retreatTicks;

        private BloodApeCombatGoal(BloodApeDemonGeneralEntity ape) {
            this.ape = ape;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return validTarget(ape.getTarget());
        }

        @Override
        public boolean canContinueToUse() {
            return validTarget(ape.getTarget());
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            ape.setAggressive(true);
            decisionTicks = 0;
            meleeTicks = 0;
            flankTicks = 20 + ape.random.nextInt(25);
            retreatTicks = 0;
        }

        @Override
        public void stop() {
            ape.setAggressive(false);
            ape.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = ape.getTarget();
            if (!validTarget(target)) {
                return;
            }
            ape.getLookControl().setLookAt(target, 45.0F, 45.0F);
            meleeTicks = Math.max(0, meleeTicks - 1);
            flankTicks = Math.max(0, flankTicks - 1);
            retreatTicks = Math.max(0, retreatTicks - 1);
            if (ape.getActionState() != ACTION_NONE) {
                ape.getNavigation().stop();
                return;
            }
            if (--decisionTicks > 0) {
                return;
            }
            decisionTicks = (ape.isBerserk() ? 3 : 5) + ape.random.nextInt(4);

            double distance = Math.sqrt(ape.distanceToSqr(target));
            boolean lineOfSight = ape.hasLineOfSight(target);
            boolean dangerous = ape.isDangerousAction(target);

            if (retreatTicks > 0) {
                if (distance < 15.0D) {
                    ape.retreatFrom(target);
                } else {
                    ape.getNavigation().stop();
                }
                return;
            }

            if (ape.getHealth() <= ape.getMaxHealth() * 0.20F
                    && ape.hasYuanyingGuard()
                    && (dangerous || ape.hurtTime > 0)
                    && ape.random.nextFloat() < 0.55F) {
                retreatTicks = 25 + ape.random.nextInt(26);
                ape.retreatFrom(target);
                return;
            }

            if (dangerous && distance <= 9.0D && ape.grabCooldown <= 0 && lineOfSight) {
                ape.startAction(ACTION_GRAB, GRAB_TICKS, target);
                ape.grabCooldown = ape.isBerserk() ? 90 : 130;
                return;
            }
            if (dangerous && distance <= 18.0D && ape.tryDodge(target)) {
                return;
            }
            if (ape.hurtTime > 0 && ape.getHealth() <= ape.getMaxHealth() * 0.20F
                    && ape.hasYuanyingGuard() && ape.random.nextFloat() < 0.35F
                    && ape.tryDodge(target)) {
                return;
            }
            if (ape.earthStompCooldown <= 0 && ape.onGround()
                    && distance <= 6.0D
                    && (ape.nearbyEnemyCount() >= 2 || ape.random.nextFloat() < 0.30F)) {
                ape.startAction(ACTION_EARTH_STOMP, EARTH_STOMP_TICKS, target);
                ape.earthStompCooldown = ape.isBerserk() ? 105 : 145;
                return;
            }
            if (ape.leapCooldown <= 0 && distance >= 7.0D && distance <= 20.0D
                    && lineOfSight && ape.onGround()) {
                ape.startAction(ACTION_LEAP_KILL, LEAP_KILL_TICKS, target);
                ape.leapCooldown = ape.isBerserk() ? 95 : 135;
                return;
            }
            if (ape.grabCooldown <= 0 && distance >= 3.0D && distance <= 9.0D
                    && lineOfSight && ape.random.nextFloat() < 0.32F) {
                ape.startAction(ACTION_GRAB, GRAB_TICKS, target);
                ape.grabCooldown = ape.isBerserk() ? 90 : 130;
                return;
            }
            double punchReach = ape.isBerserk() ? 5.2D : 4.0D;
            if (ape.heavyPunchCooldown <= 0 && distance <= punchReach && lineOfSight
                    && (ape.isBlocking(target) || ape.random.nextFloat() < 0.52F)) {
                ape.startAction(ACTION_HEAVY_PUNCH, HEAVY_PUNCH_TICKS, target);
                ape.heavyPunchCooldown = ape.isBerserk() ? 50 : 75;
                return;
            }

            double meleeReach = ape.isBerserk() ? 4.35D : 3.35D;
            if (distance <= meleeReach && lineOfSight && meleeTicks <= 0) {
                ape.swing(InteractionHand.MAIN_HAND, true);
                ape.doHurtTarget(target);
                meleeTicks = ape.isBerserk() ? 11 : 18;
                if (ape.random.nextFloat() < 0.32F) {
                    ape.tryDodge(target);
                }
                return;
            }

            if (flankTicks <= 0 && distance <= 14.0D) {
                Vec3 targetForward = horizontalDirection(target.getYRot());
                Vec3 side = new Vec3(-targetForward.z, 0.0D, targetForward.x)
                        .scale(ape.random.nextBoolean() ? 1.0D : -1.0D);
                Vec3 flank = target.position().add(side.scale(3.8D))
                        .subtract(targetForward.scale(1.5D));
                ape.getNavigation().moveTo(flank.x, flank.y, flank.z,
                        ape.isBerserk() ? 1.35D : 1.05D);
                flankTicks = 32 + ape.random.nextInt(35);
                return;
            }
            ape.getNavigation().moveTo(target,
                    ape.isBerserk() ? 1.3D : 1.0D);
        }

        private static boolean validTarget(LivingEntity target) {
            return target != null && target.isAlive()
                    && (!(target instanceof Player player)
                    || (!player.isCreative() && !player.isSpectator()));
        }
    }
}
