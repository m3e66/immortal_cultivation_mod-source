package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.combat.CombatStateManager;
import com.example.immortal_cultivation_mod.event.SmallSectProtection;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.ForcefieldCenterBehavior;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
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
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Ancient forging-sect treasury construct. It remains anchored while dormant,
 * wakes for intruders or protected treasure interactions, and cycles through
 * furnace pressure, overload, and a vulnerable venting window.
 */
public final class VaultGuardianEntity extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Byte> ACTION_STATE =
            SynchedEntityData.defineId(VaultGuardianEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ACTION_TICKS =
            SynchedEntityData.defineId(VaultGuardianEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FURNACE_PRESSURE =
            SynchedEntityData.defineId(VaultGuardianEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> FURNACE_PHASE =
            SynchedEntityData.defineId(VaultGuardianEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> ACTIVE =
            SynchedEntityData.defineId(VaultGuardianEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CORE_EXPOSED =
            SynchedEntityData.defineId(VaultGuardianEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> JINDAN_VARIANT =
            SynchedEntityData.defineId(VaultGuardianEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HEATED_ARMOR =
            SynchedEntityData.defineId(VaultGuardianEntity.class, EntityDataSerializers.BOOLEAN);

    public static final byte ACTION_NONE = 0;
    public static final byte ACTION_BASIC_STRIKE = 1;
    public static final byte ACTION_HAMMER_SMASH = 2;
    public static final byte ACTION_FURNACE_STREAM = 3;
    public static final byte ACTION_FORGING_DOMAIN = 4;
    public static final byte ACTION_SPIKE_VOLLEY = 5;
    public static final byte ACTION_GRAB = 6;
    public static final byte ACTION_HEATED_ARMOR = 7;
    public static final byte ACTION_OVERLOAD_START = 8;
    public static final byte ACTION_VENT = 9;
    public static final byte ACTION_STAGGER = 10;
    public static final byte ACTION_ACTIVATE = 11;

    public static final byte PHASE_NORMAL = 0;
    public static final byte PHASE_OVERLOAD = 1;
    public static final byte PHASE_VENTING = 2;

    private static final int PRESSURE_MAX = 100;
    private static final int OVERLOAD_DURATION = 8 * 20;
    private static final int VENT_DURATION = 4 * 20;
    private static final int HEATED_ARMOR_DURATION = 6 * 20;
    private static final int FORGING_DOMAIN_DURATION = 5 * 20;
    private static final double TREASURE_ACTIVATION_RADIUS = 10.0D;
    private static final double GUARD_RADIUS = 22.0D;

    private static final ResourceLocation OVERLOAD_SPEED_ID = id("vault_overload_speed");
    private static final ResourceLocation OVERLOAD_DAMAGE_ID = id("vault_overload_damage");
    private static final ResourceLocation OVERLOAD_ATTACK_SPEED_ID = id("vault_overload_attack_speed");
    private static final ResourceLocation VENT_SPEED_ID = id("vault_vent_speed");

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin()
            .thenLoop("animation.xuanzhen_automaton.idle");
    private static final RawAnimation ACTIVATE_ANIMATION = RawAnimation.begin()
            .thenPlayAndHold("animation.xuanzhen_automaton.activate");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin()
            .thenLoop("animation.xuanzhen_automaton.walk");
    private static final RawAnimation BASIC_ANIMATION = RawAnimation.begin()
            .thenPlayAndHold("animation.xuanzhen_automaton.hammer_smash");
    private static final RawAnimation HAMMER_ANIMATION = RawAnimation.begin()
            .thenPlayAndHold("animation.xuanzhen_automaton.hammer_smash");
    private static final RawAnimation FURNACE_ANIMATION = RawAnimation.begin()
            .thenPlayAndHold("animation.xuanzhen_automaton.furnace_breath");
    private static final RawAnimation DOMAIN_ANIMATION = RawAnimation.begin()
            .thenPlayAndHold("animation.xuanzhen_automaton.earth_anchor");
    private static final RawAnimation SPIKE_ANIMATION = RawAnimation.begin()
            .thenPlayAndHold("animation.xuanzhen_automaton.spike_volley");
    private static final RawAnimation GRAB_ANIMATION = RawAnimation.begin()
            .thenPlayAndHold("animation.xuanzhen_automaton.grasp_slam");
    private static final RawAnimation HEATED_ANIMATION = RawAnimation.begin()
            .thenPlayAndHold("animation.xuanzhen_automaton.heated_armor");
    private static final RawAnimation OVERLOAD_ANIMATION = RawAnimation.begin()
            .thenPlayAndHold("animation.xuanzhen_automaton.overload");
    private static final RawAnimation VENT_ANIMATION = RawAnimation.begin()
            .thenPlayAndHold("animation.xuanzhen_automaton.vent");
    private static final RawAnimation STAGGER_ANIMATION = RawAnimation.begin()
            .thenPlayAndHold("animation.xuanzhen_automaton.vent");
    private static final RawAnimation DEATH_ANIMATION = RawAnimation.begin()
            .thenPlayAndHold("animation.xuanzhen_automaton.vent");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    private BlockPos guardHome = BlockPos.ZERO;
    private boolean homeInitialized;
    private boolean overloadUsed;
    private int overloadTicks;
    private int ventTicks;
    private int heatedArmorTicks;
    private int forgingDomainTicks;
    private Vec3 forgingDomainCenter = Vec3.ZERO;
    private int hammerCooldown;
    private int furnaceCooldown = 80;
    private int domainCooldown = 120;
    private int spikeCooldown = 50;
    private int grabCooldown = 100;
    private int heatedArmorCooldown = 160;
    private int basicAttackCooldown;
    private int lostTargetTicks;
    private UUID actionTargetUuid;
    private UUID grabbedTargetUuid;
    private int grabHoldTicks;
    private boolean actionImpactDone;
    private boolean smallSectGuardian;
    private boolean smallSectRegistrationComplete;
    private String smallSectInstanceKey = "";

    public VaultGuardianEntity(
            EntityType<? extends VaultGuardianEntity> type, Level level,
            boolean jindanVariant) {
        super(type, level);
        entityData.set(JINDAN_VARIANT, jindanVariant);
        xpReward = jindanVariant ? 1_200 : 450;
        setPersistenceRequired();
        noCulling = true;
    }

    public static AttributeSupplier.Builder createZhujiAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 2_800.0D)
                .add(Attributes.ARMOR, 26.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 8.0D)
                .add(Attributes.ATTACK_DAMAGE, 110.0D)
                .add(Attributes.ATTACK_SPEED, 3.2D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.82D)
                .add(Attributes.STEP_HEIGHT, 1.1D);
    }

    public static AttributeSupplier.Builder createJindanAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6_800.0D)
                .add(Attributes.ARMOR, 42.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 14.0D)
                .add(Attributes.ATTACK_DAMAGE, 240.0D)
                .add(Attributes.ATTACK_SPEED, 3.6D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 38.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.96D)
                .add(Attributes.STEP_HEIGHT, 1.2D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new GuardianCombatGoal(this));
        goalSelector.addGoal(4, new ReturnToVaultGoal(this));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ACTION_STATE, ACTION_NONE);
        builder.define(ACTION_TICKS, 0);
        builder.define(FURNACE_PRESSURE, 0);
        builder.define(FURNACE_PHASE, PHASE_NORMAL);
        builder.define(ACTIVE, false);
        builder.define(CORE_EXPOSED, false);
        builder.define(JINDAN_VARIANT, false);
        builder.define(HEATED_ARMOR, false);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || !isAlive()) {
            return;
        }
        if (smallSectGuardian && !smallSectRegistrationComplete
                && level() instanceof ServerLevel serverLevel) {
            smallSectRegistrationComplete =
                    SmallSectProtection.registerGuardian(serverLevel, this);
        }
        initializeHome();
        if (smallSectGuardian
                && distanceToHomeSqr() > (GUARD_RADIUS + 8.0D) * (GUARD_RADIUS + 8.0D)) {
            returnToVaultHome();
        }
        decrementCooldowns();
        tickTimedStates();
        tickForgingDomain();

        if (!isActive()) {
            seekNearbyIntruder();
            if (tickCount % 30 == 0 && getFurnacePressure() > 0) {
                addPressure(-1);
            }
        } else {
            maintainIntruderTarget();
            if (tickCount % 20 == 0 && getTarget() != null) {
                addPressure(isJindanVariant() ? 2 : 1);
            }
            maybeTriggerPressurePhase();
        }

        if (getActionState() != ACTION_NONE) {
            tickCurrentAction();
        }
        emitAmbientFurnaceEffects();
    }

    private void initializeHome() {
        if (!homeInitialized) {
            guardHome = blockPosition();
            homeInitialized = true;
        }
    }

    private void decrementCooldowns() {
        hammerCooldown = decrement(hammerCooldown);
        furnaceCooldown = decrement(furnaceCooldown);
        domainCooldown = decrement(domainCooldown);
        spikeCooldown = decrement(spikeCooldown);
        grabCooldown = decrement(grabCooldown);
        heatedArmorCooldown = decrement(heatedArmorCooldown);
        basicAttackCooldown = decrement(basicAttackCooldown);
    }

    private void tickTimedStates() {
        if (heatedArmorTicks > 0 && --heatedArmorTicks == 0) {
            entityData.set(HEATED_ARMOR, false);
        }
        if (getFurnacePhase() == PHASE_OVERLOAD && overloadTicks > 0
                && --overloadTicks == 0) {
            beginVenting();
        } else if (getFurnacePhase() == PHASE_VENTING && ventTicks > 0
                && --ventTicks == 0) {
            setFurnacePhase(PHASE_NORMAL);
            setCoreExposed(false);
            finishAction();
        }
    }

    private void seekNearbyIntruder() {
        if (tickCount % 10 != 0) {
            return;
        }
        Player player = nearestValidPlayer(TREASURE_ACTIVATION_RADIUS);
        if (player != null) {
            activateAgainst(player);
        }
    }

    private void maintainIntruderTarget() {
        LivingEntity target = getTarget();
        if (!isValidIntruder(target) || !insideGuardRadius(target, GUARD_RADIUS + 4.0D)) {
            setTarget(null);
            target = null;
        }
        if (target == null && tickCount % 10 == 0) {
            Player replacement = nearestValidPlayer(GUARD_RADIUS);
            if (replacement != null) {
                setTarget(replacement);
                lostTargetTicks = 0;
                return;
            }
        }
        if (getTarget() == null) {
            lostTargetTicks++;
            if (lostTargetTicks >= 200) {
                returnToVaultHome();
            }
        } else {
            lostTargetTicks = 0;
        }
    }

    public void activateAgainst(@Nullable LivingEntity intruder) {
        if (level().isClientSide || !isAlive()) {
            return;
        }
        initializeHome();
        boolean firstActivation = !isActive();
        entityData.set(ACTIVE, true);
        if (isValidIntruder(intruder) && insideGuardRadius(intruder, GUARD_RADIUS + 6.0D)) {
            setTarget(intruder);
        }
        lostTargetTicks = 0;
        if (firstActivation && level() instanceof ServerLevel level) {
            startAction(ACTION_ACTIVATE, 52, intruder);
            level.playSound(null, blockPosition(), SoundEvents.COPPER_BULB_TURN_ON,
                    SoundSource.HOSTILE, 1.35F, isJindanVariant() ? 0.62F : 0.75F);
            level.playSound(null, blockPosition(), SoundEvents.IRON_GOLEM_REPAIR,
                    SoundSource.HOSTILE, 1.0F, 0.55F);
            emitActivationFlash(level);
        }
        if (intruder != null && level() instanceof ServerLevel level) {
            AABB nearby = getBoundingBox().inflate(12.0D);
            for (VaultGuardianEntity guardian : level.getEntitiesOfClass(
                    VaultGuardianEntity.class, nearby,
                    other -> other != this && !other.isActive())) {
                guardian.activateAgainst(intruder);
            }
        }
    }

    private Player nearestValidPlayer(double radius) {
        AABB area = new AABB(Vec3.atCenterOf(guardHome), Vec3.atCenterOf(guardHome))
                .inflate(radius, Math.min(8.0D, radius), radius);
        List<Player> players = level().getEntitiesOfClass(
                Player.class, area, this::isValidIntruder);
        Player nearest = null;
        double bestPriority = Double.MAX_VALUE;
        for (Player player : players) {
            double distance = distanceToSqr(player);
            double homeDistance = player.position().distanceToSqr(
                    Vec3.atCenterOf(guardHome));
            double priority = distance - (homeDistance <= 36.0D ? 1_000.0D : 0.0D);
            if (priority < bestPriority) {
                nearest = player;
                bestPriority = priority;
            }
        }
        return nearest;
    }

    private boolean isValidIntruder(@Nullable LivingEntity target) {
        return target != null && target.isAlive()
                && (!(target instanceof Player player)
                        || !player.isCreative() && !player.isSpectator())
                && !isAlliedTo(target)
                && !(target instanceof VaultGuardianEntity);
    }

    private boolean insideGuardRadius(LivingEntity target, double radius) {
        return target.distanceToSqr(Vec3.atCenterOf(guardHome)) <= radius * radius;
    }

    private double distanceToHomeSqr() {
        return position().distanceToSqr(Vec3.atBottomCenterOf(guardHome));
    }

    private void maybeTriggerPressurePhase() {
        if (getFurnacePhase() != PHASE_NORMAL || getActionState() == ACTION_STAGGER) {
            return;
        }
        boolean healthTrigger = !overloadUsed && getHealth() <= getMaxHealth() * 0.55F;
        if (!overloadUsed && (healthTrigger || getFurnacePressure() >= PRESSURE_MAX)) {
            beginOverload();
        } else if (overloadUsed && getFurnacePressure() >= PRESSURE_MAX) {
            beginVenting();
        }
    }

    private void beginOverload() {
        overloadUsed = true;
        overloadTicks = OVERLOAD_DURATION;
        setFurnacePhase(PHASE_OVERLOAD);
        addPressure(PRESSURE_MAX);
        startAction(ACTION_OVERLOAD_START, 30, getTarget());
        if (level() instanceof ServerLevel level) {
            level.playSound(null, blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE,
                    SoundSource.HOSTILE, 2.0F, 0.55F);
            emitPressureBurst(level, 4.0D, 60);
        }
    }

    private void beginVenting() {
        if (getFurnacePhase() == PHASE_VENTING) {
            return;
        }
        ventTicks = VENT_DURATION;
        overloadTicks = 0;
        setFurnacePhase(PHASE_VENTING);
        entityData.set(FURNACE_PRESSURE, 0);
        setCoreExposed(true);
        startAction(ACTION_VENT, VENT_DURATION, getTarget());
        getNavigation().stop();
        if (level() instanceof ServerLevel level) {
            level.playSound(null, blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.HOSTILE, 2.5F, 0.48F);
            emitPressureBurst(level, 5.0D, 90);
        }
    }

    private void setFurnacePhase(byte phase) {
        entityData.set(FURNACE_PHASE, phase);
        removeModifier(Attributes.MOVEMENT_SPEED, OVERLOAD_SPEED_ID);
        removeModifier(Attributes.ATTACK_DAMAGE, OVERLOAD_DAMAGE_ID);
        removeModifier(Attributes.ATTACK_SPEED, OVERLOAD_ATTACK_SPEED_ID);
        removeModifier(Attributes.MOVEMENT_SPEED, VENT_SPEED_ID);
        if (phase == PHASE_OVERLOAD) {
            addModifier(Attributes.MOVEMENT_SPEED, OVERLOAD_SPEED_ID, 0.15D);
            addModifier(Attributes.ATTACK_DAMAGE, OVERLOAD_DAMAGE_ID, 0.20D);
            addModifier(Attributes.ATTACK_SPEED, OVERLOAD_ATTACK_SPEED_ID, 0.25D);
        } else if (phase == PHASE_VENTING) {
            addModifier(Attributes.MOVEMENT_SPEED, VENT_SPEED_ID, -0.35D);
        }
    }

    private void addModifier(
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            ResourceLocation id, double amount) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null && instance.getModifier(id) == null) {
            instance.addTransientModifier(new AttributeModifier(
                    id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private void removeModifier(
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            ResourceLocation id) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null && instance.getModifier(id) != null) {
            instance.removeModifier(id);
        }
    }

    private void tickCurrentAction() {
        byte action = getActionState();
        int ticks = getActionTicks();
        LivingEntity target = resolveLiving(actionTargetUuid);
        getNavigation().stop();
        if (isValidIntruder(target) && action != ACTION_FORGING_DOMAIN
                && action != ACTION_VENT && action != ACTION_STAGGER) {
            getLookControl().setLookAt(target, 35.0F, 30.0F);
        }
        if (level() instanceof ServerLevel level) {
            switch (action) {
                case ACTION_BASIC_STRIKE -> tickBasicStrike(level, ticks, target);
                case ACTION_HAMMER_SMASH -> tickHammerSmash(level, ticks, target);
                case ACTION_FURNACE_STREAM -> tickFurnaceStream(level, ticks);
                case ACTION_FORGING_DOMAIN -> tickForgingDomainCast(level, ticks);
                case ACTION_SPIKE_VOLLEY -> tickSpikeVolley(level, ticks, target);
                case ACTION_GRAB -> tickGrab(level, ticks, target);
                case ACTION_HEATED_ARMOR -> tickHeatedArmorCast(level, ticks);
                case ACTION_OVERLOAD_START -> tickOverloadStart(level, ticks);
                case ACTION_VENT -> tickVenting(level, ticks);
                case ACTION_STAGGER -> tickStagger(level, ticks);
                case ACTION_ACTIVATE -> {
                }
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

    private void tickBasicStrike(ServerLevel level, int ticks, @Nullable LivingEntity target) {
        if (ticks == 5 && !actionImpactDone && target != null && distanceToSqr(target) <= 3.1D * 3.1D) {
            actionImpactDone = true;
            super.doHurtTarget(target);
            addPressure(2);
            Vec3 away = target.position().subtract(position());
            target.knockback(0.8D, -away.x, -away.z);
            level.playSound(null, blockPosition(), SoundEvents.IRON_GOLEM_ATTACK,
                    SoundSource.HOSTILE, 1.0F, 0.72F);
        }
    }

    private void tickHammerSmash(ServerLevel level, int ticks, @Nullable LivingEntity target) {
        if (ticks > 12 && (ticks & 1) == 0) {
            Vec3 joint = shoulderPoint(true);
            level.sendParticles(ModParticles.METHOD_QI_METAL.get(),
                    joint.x, joint.y, joint.z, 4, 0.24D, 0.24D, 0.24D, 0.03D);
            level.sendParticles(ParticleTypes.FLAME,
                    joint.x, joint.y, joint.z, 2, 0.16D, 0.16D, 0.16D, 0.015D);
        }
        if (ticks != 12 || actionImpactDone) {
            return;
        }
        actionImpactDone = true;
        swing(InteractionHand.MAIN_HAND, true);
        addPressure(9);
        float damage = isJindanVariant() ? 380.0F : 180.0F;
        Vec3 forward = horizontalLook();
        AABB area = getBoundingBox().inflate(3.7D, 1.6D, 3.7D);
        for (LivingEntity victim : level.getEntitiesOfClass(
                LivingEntity.class, area, this::canDamageIntruder)) {
            Vec3 delta = victim.getBoundingBox().getCenter().subtract(getBoundingBox().getCenter());
            double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
            if (horizontal > 3.5D || horizontal > 0.01D
                    && forward.dot(new Vec3(delta.x, 0.0D, delta.z).normalize()) < -0.05D) {
                continue;
            }
            float appliedDamage = isBlocking(victim) ? damage * 1.30F : damage;
            hurtAbilityTarget(victim, appliedDamage, true);
            victim.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, 24, 2, false, true, true), this);
            Vec3 away = victim.position().subtract(position());
            victim.knockback(2.4D, -away.x, -away.z);
        }
        Vec3 impact = position().add(forward.scale(1.6D));
        emitForgingImpact(level, impact, 3.4D);
        level.playSound(null, BlockPos.containing(impact), SoundEvents.ANVIL_LAND,
                SoundSource.HOSTILE, 1.9F, 0.55F);
        level.playSound(null, BlockPos.containing(impact), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 0.8F, 0.78F);
    }

    private void tickFurnaceStream(ServerLevel level, int ticks) {
        setCoreExposed(true);
        if (ticks <= 70 && ticks >= 5 && (ticks & 1) == 0) {
            emitFurnaceCone(level);
        }
        if ((ticks == 65 || ticks == 45 || ticks == 25 || ticks == 5)
                && !level().isClientSide) {
            damageFurnaceCone(level);
        }
        if (ticks == 70) {
            level.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT,
                    SoundSource.HOSTILE, 1.8F, 0.52F);
            addPressure(-30);
        }
    }

    private void damageFurnaceCone(ServerLevel level) {
        float damage = isJindanVariant() ? 140.0F : 75.0F;
        Vec3 origin = chestPoint();
        Vec3 forward = horizontalLook();
        AABB area = getBoundingBox().inflate(7.2D, 2.8D, 7.2D);
        for (LivingEntity victim : level.getEntitiesOfClass(
                LivingEntity.class, area, this::canDamageIntruder)) {
            Vec3 point = victim.getBoundingBox().getCenter();
            Vec3 delta = point.subtract(origin);
            double distance = delta.length();
            if (distance > 7.0D || distance < 0.01D
                    || forward.dot(delta.normalize()) < 0.72D
                    || !hasLineOfSight(victim)) {
                continue;
            }
            hurtAbilityTarget(victim, damage, false);
            victim.igniteForSeconds(4.0F);
            Vec3 away = victim.position().subtract(position());
            victim.knockback(0.35D, -away.x, -away.z);
        }
    }

    private void emitFurnaceCone(ServerLevel level) {
        Vec3 origin = chestPoint();
        Vec3 forward = horizontalLook();
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        for (int step = 1; step <= 8; step++) {
            double distance = step * 0.82D;
            double spread = step * 0.11D;
            for (int side = -1; side <= 1; side++) {
                Vec3 point = origin.add(forward.scale(distance))
                        .add(right.scale(side * spread));
                level.sendParticles(side == 0 ? ParticleTypes.FLAME : ParticleTypes.ASH,
                        point.x, point.y, point.z,
                        side == 0 ? 2 : 1,
                        spread * 0.25D, 0.14D + spread * 0.12D,
                        spread * 0.25D, side == 0 ? 0.035D : 0.015D);
                if (step % 3 == 0 && side == 0) {
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            point.x, point.y, point.z,
                            1, 0.12D, 0.12D, 0.12D, 0.05D);
                }
            }
        }
        emitVentParticles(level, 2, false);
    }

    private void tickForgingDomainCast(ServerLevel level, int ticks) {
        if (ticks > 18 && ticks % 3 == 0) {
            level.sendParticles(ModParticles.METHOD_QI_EARTH.get(),
                    getX(), getY() + 0.1D, getZ(),
                    8, 0.65D, 0.08D, 0.65D, 0.025D);
        }
        if (ticks == 18 && !actionImpactDone) {
            actionImpactDone = true;
            forgingDomainCenter = position();
            forgingDomainTicks = FORGING_DOMAIN_DURATION;
            addPressure(7);
            AABB impactArea = getBoundingBox().inflate(5.0D, 2.5D, 5.0D);
            for (LivingEntity victim : level.getEntitiesOfClass(
                    LivingEntity.class, impactArea, this::canDamageIntruder)) {
                if (victim.position().distanceToSqr(forgingDomainCenter) <= 25.0D) {
                    hurtAbilityTarget(victim,
                            isJindanVariant() ? 250.0F : 120.0F, false);
                    Vec3 away = victim.position().subtract(forgingDomainCenter);
                    if (away.horizontalDistanceSqr() > 0.001D) {
                        victim.knockback(0.55D, -away.x, -away.z);
                    }
                }
            }
            emitForgingSeal(level, forgingDomainCenter, 5.0D, true);
            level.playSound(null, blockPosition(), SoundEvents.IRON_GOLEM_STEP,
                    SoundSource.HOSTILE, 2.0F, 0.52F);
        }
    }

    private void tickForgingDomain() {
        if (forgingDomainTicks <= 0 || !(level() instanceof ServerLevel level)) {
            return;
        }
        forgingDomainTicks--;
        if (forgingDomainTicks % 10 == 0) {
            emitForgingSeal(level, forgingDomainCenter, 5.0D, false);
        }
        AABB area = new AABB(forgingDomainCenter, forgingDomainCenter)
                .inflate(5.0D, 2.5D, 5.0D);
        for (LivingEntity victim : level.getEntitiesOfClass(
                LivingEntity.class, area, this::canDamageIntruder)) {
            if (victim.position().distanceToSqr(forgingDomainCenter) > 25.0D) {
                continue;
            }
            victim.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, 16, 1, false, true, true), this);
            Vec3 motion = victim.getDeltaMovement();
            if (motion.y > 0.08D) {
                victim.setDeltaMovement(motion.x * 0.82D, 0.08D, motion.z * 0.82D);
            }
            if (forgingDomainTicks % 20 == 0) {
                hurtAbilityTarget(victim, isJindanVariant() ? 50.0F : 25.0F, false);
                level.sendParticles(ParticleTypes.ASH,
                        victim.getX(), victim.getY() + 0.15D, victim.getZ(),
                        6, 0.35D, 0.12D, 0.35D, 0.035D);
            }
        }
    }

    private void tickSpikeVolley(
            ServerLevel level, int ticks, @Nullable LivingEntity target) {
        if (ticks > 14 && ticks % 3 == 0) {
            Vec3 wrist = shoulderPoint(false).add(0.0D, -0.65D, 0.0D);
            level.sendParticles(ModParticles.METHOD_QI_METAL.get(),
                    wrist.x, wrist.y, wrist.z,
                    4, 0.18D, 0.18D, 0.18D, 0.035D);
        }
        if (ticks != 14 || actionImpactDone || !isValidIntruder(target)) {
            return;
        }
        actionImpactDone = true;
        addPressure(8);
        launchSpiritSpikes(level, target);
    }

    private void launchSpiritSpikes(ServerLevel level, LivingEntity target) {
        Vec3 origin = shoulderPoint(false).add(horizontalLook().scale(0.55D));
        Vec3 targetPoint = target.getBoundingBox().getCenter();
        Vec3 base = targetPoint.subtract(origin).normalize();
        Vec3 horizontalRight = new Vec3(-base.z, 0.0D, base.x).normalize();
        float damage = isJindanVariant() ? 80.0F : 45.0F;
        for (int index = 0; index < 8; index++) {
            double spread = (index - 3.5D) * 0.055D;
            Vec3 direction = base.add(horizontalRight.scale(spread))
                    .add(0.0D, (index % 2 == 0 ? 0.018D : -0.018D), 0.0D)
                    .normalize();
            boolean homing = index >= 6;
            VaultSpiritSpikeEntity spike = new VaultSpiritSpikeEntity(
                    level, this, target, damage, homing);
            spike.setPos(origin.x, origin.y, origin.z);
            spike.shoot(direction.x, direction.y, direction.z,
                    isJindanVariant() ? 1.45F : 1.25F, homing ? 0.5F : 1.4F);
            level.addFreshEntity(spike);
        }
        level.playSound(null, blockPosition(), SoundEvents.PISTON_EXTEND,
                SoundSource.HOSTILE, 1.2F, 1.35F);
        level.playSound(null, blockPosition(), SoundEvents.CROSSBOW_SHOOT,
                SoundSource.HOSTILE, 1.1F, 0.72F);
    }

    private void tickGrab(ServerLevel level, int ticks, @Nullable LivingEntity target) {
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
                slamGrabbedTarget(level, grabbed);
                finishAction();
            }
            return;
        }
        grabbedTargetUuid = null;
        int catchTick = isJindanVariant() ? 48 : 36;
        if (!isValidIntruder(target) || ticks > catchTick + 10 || ticks < catchTick - 4) {
            return;
        }
        Vec3 toward = target.position().subtract(position()).multiply(1.0D, 0.0D, 1.0D);
        if (toward.lengthSqr() > 0.01D) {
            Vec3 lunge = toward.normalize().scale(isJindanVariant() ? 0.72D : 0.58D);
            setDeltaMovement(lunge.x, getDeltaMovement().y, lunge.z);
            hasImpulse = true;
        }
        if (distanceToSqr(target) > 4.0D * 4.0D || actionImpactDone) {
            return;
        }
        actionImpactDone = true;
        grabbedTargetUuid = target.getUUID();
        int holdTicks = isJindanVariant() ? 36 : 24;
        if (com.example.immortal_cultivation_mod.combat.ThoughtControlGuard.resistForDuration(
                target, "vault_guardian_grab", this, holdTicks)) {
            grabbedTargetUuid = null;
            return;
        }
        grabHoldTicks = holdTicks;
        target.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN, grabHoldTicks + 6,
                10, false, false, true), this);
        addPressure(6);
        emitGrabSeal(level, target.getBoundingBox().getCenter());
        level.playSound(null, blockPosition(), SoundEvents.IRON_TRAPDOOR_CLOSE,
                SoundSource.HOSTILE, 1.4F, 0.62F);
    }

    private void holdGrabbedTarget(LivingEntity target) {
        Vec3 held = position().add(horizontalLook().scale(1.25D))
                .add(0.0D, getBbHeight() * 0.48D, 0.0D);
        target.teleportTo(held.x, held.y, held.z);
        target.setDeltaMovement(Vec3.ZERO);
        target.resetFallDistance();
    }

    private void slamGrabbedTarget(ServerLevel level, LivingEntity target) {
        float damage = isJindanVariant() ? 320.0F : 150.0F;
        hurtAbilityTarget(target, damage, true);
        target.teleportTo(target.getX(), Math.max(level.getMinBuildHeight(), getY() + 0.05D), target.getZ());
        target.setDeltaMovement(horizontalLook().scale(0.45D).add(0.0D, 0.18D, 0.0D));
        emitForgingImpact(level, target.position(), 1.65D);
        level.playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND,
                SoundSource.HOSTILE, 1.45F, 0.72F);
        grabbedTargetUuid = null;
    }

    private void tickHeatedArmorCast(ServerLevel level, int ticks) {
        if (ticks == 18 && !actionImpactDone) {
            actionImpactDone = true;
            heatedArmorTicks = HEATED_ARMOR_DURATION;
            entityData.set(HEATED_ARMOR, true);
            addPressure(8);
            level.playSound(null, blockPosition(), SoundEvents.FIRECHARGE_USE,
                    SoundSource.HOSTILE, 1.4F, 0.58F);
            emitPressureBurst(level, 2.2D, 42);
        }
    }

    private void tickOverloadStart(ServerLevel level, int ticks) {
        if (ticks % 3 == 0) {
            emitVentParticles(level, 4, true);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    getX(), getY() + getBbHeight() * 0.5D, getZ(),
                    5, 0.62D, 0.85D, 0.62D, 0.08D);
        }
    }

    private void tickVenting(ServerLevel level, int ticks) {
        setCoreExposed(true);
        if ((ticks & 1) == 0) {
            emitVentParticles(level, 7, true);
        }
        if (ticks % 20 == 0) {
            level.playSound(null, blockPosition(), SoundEvents.LAVA_EXTINGUISH,
                    SoundSource.HOSTILE, 0.75F, 0.75F + random.nextFloat() * 0.2F);
        }
    }

    private void tickStagger(ServerLevel level, int ticks) {
        setCoreExposed(true);
        if (ticks % 4 == 0) {
            Vec3 core = chestPoint();
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    core.x, core.y, core.z,
                    5, 0.22D, 0.22D, 0.22D, 0.09D);
        }
    }

    private void startAction(byte action, int ticks, @Nullable LivingEntity target) {
        entityData.set(ACTION_STATE, action);
        entityData.set(ACTION_TICKS, Math.max(1, ticks));
        actionTargetUuid = target == null ? null : target.getUUID();
        grabbedTargetUuid = null;
        grabHoldTicks = 0;
        actionImpactDone = false;
        if (action == ACTION_FURNACE_STREAM || action == ACTION_VENT
                || action == ACTION_STAGGER) {
            setCoreExposed(true);
        }
    }

    private void finishAction() {
        byte previous = getActionState();
        entityData.set(ACTION_STATE, ACTION_NONE);
        entityData.set(ACTION_TICKS, 0);
        actionTargetUuid = null;
        grabbedTargetUuid = null;
        grabHoldTicks = 0;
        actionImpactDone = false;
        if (previous == ACTION_FURNACE_STREAM || previous == ACTION_STAGGER) {
            setCoreExposed(getFurnacePhase() == PHASE_VENTING);
        }
    }

    private void emitAmbientFurnaceEffects() {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        int interval = isActive() ? 5 : 18;
        if (tickCount % interval == 0) {
            int count = getFurnacePhase() == PHASE_OVERLOAD ? 4
                    : getFurnacePressure() >= 70 ? 2 : 1;
            emitVentParticles(level, count, false);
        }
        if (getFurnacePhase() == PHASE_OVERLOAD && tickCount % 4 == 0
                && getDeltaMovement().horizontalDistanceSqr() > 0.005D) {
            level.sendParticles(ModParticles.METHOD_QI_FIRE.get(),
                    getX(), getY() + 0.08D, getZ(),
                    3, 0.45D, 0.05D, 0.45D, 0.015D);
        }
    }

    private void emitVentParticles(ServerLevel level, int count, boolean forceful) {
        Vec3 forward = horizontalLook();
        Vec3 back = position().subtract(forward.scale(0.48D))
                .add(0.0D, getBbHeight() * 0.72D, 0.0D);
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        for (int side : new int[]{-1, 1}) {
            Vec3 vent = back.add(right.scale(side * 0.42D));
            level.sendParticles(forceful ? ParticleTypes.CAMPFIRE_COSY_SMOKE : ParticleTypes.SMOKE,
                    vent.x, vent.y, vent.z,
                    Math.max(1, count / 2),
                    0.08D, 0.12D, 0.08D, forceful ? 0.11D : 0.025D);
            if (isActive()) {
                level.sendParticles(ParticleTypes.FLAME,
                        vent.x, vent.y, vent.z,
                        1, 0.05D, 0.06D, 0.05D, 0.015D);
            }
        }
    }

    private void emitActivationFlash(ServerLevel level) {
        Vec3 core = chestPoint();
        level.sendParticles(ModParticles.METHOD_QI_FIRE.get(),
                core.x, core.y, core.z,
                24, 0.42D, 0.42D, 0.25D, 0.055D);
        level.sendParticles(ModParticles.METHOD_QI_METAL.get(),
                getX(), getY() + getBbHeight() * 0.5D, getZ(),
                36, 0.8D, 1.0D, 0.8D, 0.045D);
    }

    private void emitPressureBurst(ServerLevel level, double radius, int points) {
        for (int index = 0; index < points; index++) {
            double angle = Math.PI * 2.0D * index / points;
            double r = radius * (0.35D + 0.65D * (index % 5) / 4.0D);
            double x = getX() + Math.cos(angle) * r;
            double z = getZ() + Math.sin(angle) * r;
            level.sendParticles(index % 3 == 0
                            ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.FLAME,
                    x, getY() + 0.2D + random.nextDouble() * getBbHeight(), z,
                    1, 0.02D, 0.04D, 0.02D, 0.04D);
        }
    }

    private void emitForgingImpact(ServerLevel level, Vec3 center, double radius) {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                center.x, center.y + 0.12D, center.z,
                30, 0.85D, 0.12D, 0.85D, 0.18D);
        level.sendParticles(ParticleTypes.POOF,
                center.x, center.y + 0.08D, center.z,
                24, 0.95D, 0.08D, 0.95D, 0.09D);
        emitForgingSeal(level, center, radius, true);
        int rays = 8;
        for (int ray = 0; ray < rays; ray++) {
            double angle = Math.PI * 2.0D * ray / rays;
            for (double distance = 0.4D; distance <= radius; distance += 0.35D) {
                double jitter = Math.sin(distance * 7.0D + ray) * 0.08D;
                double x = center.x + Math.cos(angle) * distance
                        + Math.cos(angle + Math.PI / 2.0D) * jitter;
                double z = center.z + Math.sin(angle) * distance
                        + Math.sin(angle + Math.PI / 2.0D) * jitter;
                level.sendParticles(ray % 2 == 0
                                ? ModParticles.METHOD_QI_FIRE.get()
                                : ModParticles.METHOD_QI_METAL.get(),
                        x, center.y + 0.045D, z,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    private void emitForgingSeal(
            ServerLevel level, Vec3 center, double radius, boolean dense) {
        int sides = 8;
        int segments = dense ? 8 : 4;
        for (int side = 0; side < sides; side++) {
            double a0 = Math.PI * 2.0D * side / sides + Math.PI / 8.0D;
            double a1 = Math.PI * 2.0D * (side + 1) / sides + Math.PI / 8.0D;
            Vec3 from = center.add(Math.cos(a0) * radius, 0.055D, Math.sin(a0) * radius);
            Vec3 to = center.add(Math.cos(a1) * radius, 0.055D, Math.sin(a1) * radius);
            for (int step = 0; step <= segments; step++) {
                Vec3 point = from.lerp(to, step / (double) segments);
                level.sendParticles(side % 2 == 0
                                ? ModParticles.METHOD_QI_FIRE.get()
                                : ModParticles.METHOD_QI_METAL.get(),
                        point.x, point.y, point.z,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        if (dense) {
            for (int ray = 0; ray < 4; ray++) {
                double angle = Math.PI * ray / 2.0D;
                for (double distance = -radius * 0.65D;
                        distance <= radius * 0.65D; distance += 0.45D) {
                    level.sendParticles(ModParticles.METHOD_QI_FIRE.get(),
                            center.x + Math.cos(angle) * distance,
                            center.y + 0.06D,
                            center.z + Math.sin(angle) * distance,
                            1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
        }
    }

    private void emitGrabSeal(ServerLevel level, Vec3 center) {
        double half = 0.72D;
        Vec3[] corners = {
                center.add(-half, 0.0D, -half),
                center.add(half, 0.0D, -half),
                center.add(half, 0.0D, half),
                center.add(-half, 0.0D, half)
        };
        for (int side = 0; side < corners.length; side++) {
            Vec3 from = corners[side];
            Vec3 to = corners[(side + 1) % corners.length];
            for (int step = 0; step <= 6; step++) {
                Vec3 point = from.lerp(to, step / 6.0D);
                level.sendParticles(side % 2 == 0
                                ? ModParticles.METHOD_QI_METAL.get()
                                : ModParticles.METHOD_QI_FIRE.get(),
                        point.x, point.y, point.z,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                center.x, center.y, center.z,
                14, 0.42D, 0.42D, 0.42D, 0.08D);
    }

    private boolean hurtAbilityTarget(LivingEntity target, float damage, boolean heavy) {
        if (!canDamageIntruder(target)
                || ForcefieldCenterBehavior.interceptAttackPath(
                        this, getBoundingBox().getCenter(),
                        target.getBoundingBox().getCenter(), damage)) {
            return false;
        }
        if (heavy) {
            return CombatStateManager.hurtAsHeavy(
                    this, target, damageSources().mobAttack(this), damage);
        }
        return target.hurt(damageSources().mobAttack(this), damage);
    }

    private boolean canDamageIntruder(LivingEntity target) {
        return target != this && isValidIntruder(target)
                && !(target instanceof VaultGuardianEntity);
    }

    private boolean isBlocking(LivingEntity target) {
        return target.isBlocking()
                || target instanceof ServerPlayer player
                && CombatStateManager.isBlocking(player);
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        if (getActionState() != ACTION_NONE || !(entity instanceof LivingEntity living)) {
            return false;
        }
        startAction(ACTION_BASIC_STRIKE, 12, living);
        basicAttackCooldown = overloadedCooldown(20);
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity attacker
                && isValidIntruder(attacker)) {
            activateAgainst(attacker);
        }
        float modified = amount;
        if (insideVaultBonusArea()) {
            modified *= 0.90F;
        }
        if (hasHeatedArmor()) {
            modified *= isJindanVariant() ? 0.65F : 0.75F;
        }
        if (getFurnacePhase() == PHASE_VENTING) {
            modified *= 1.30F;
        }
        if (isCoreExposed()) {
            modified *= 1.50F;
        }
        boolean hurt = super.hurt(source, modified);
        if (!hurt || level().isClientSide || !isAlive()) {
            return hurt;
        }

        int pressureGain = Mth.clamp(Mth.ceil(amount * 0.012F), 1, 12);
        if (source.is(DamageTypeTags.IS_FIRE)) {
            pressureGain += 3;
        }
        addPressure(pressureGain);

        Entity direct = source.getDirectEntity();
        Entity attackerEntity = source.getEntity();
        if (hasHeatedArmor() && attackerEntity instanceof LivingEntity attacker
                && attacker != this && direct == attackerEntity
                && distanceToSqr(attacker) <= 4.5D * 4.5D) {
            attacker.hurt(damageSources().mobAttack(this),
                    isJindanVariant() ? 70.0F : 35.0F);
            if (level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        attacker.getX(), attacker.getY() + attacker.getBbHeight() * 0.55D,
                        attacker.getZ(), 12, 0.3D, 0.4D, 0.3D, 0.11D);
            }
        }

        float staggerThreshold = isJindanVariant() ? 500.0F : 250.0F;
        if (isCoreExposed() && modified >= staggerThreshold
                && getActionState() != ACTION_STAGGER
                && getFurnacePhase() != PHASE_VENTING) {
            startAction(ACTION_STAGGER, 30, source.getEntity() instanceof LivingEntity living
                    ? living : null);
            addPressure(-18);
            if (level() instanceof ServerLevel level) {
                Vec3 core = chestPoint();
                level.sendParticles(ParticleTypes.FLASH,
                        core.x, core.y, core.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        core.x, core.y, core.z, 26, 0.35D, 0.35D, 0.25D, 0.14D);
                level.playSound(null, blockPosition(), SoundEvents.BELL_BLOCK,
                        SoundSource.HOSTILE, 1.8F, 0.58F);
            }
        }
        return true;
    }

    private boolean insideVaultBonusArea() {
        return homeInitialized && distanceToHomeSqr() <= 12.0D * 12.0D;
    }

    private void addPressure(int amount) {
        int pressure = Mth.clamp(getFurnacePressure() + amount, 0, PRESSURE_MAX);
        entityData.set(FURNACE_PRESSURE, pressure);
    }

    private int overloadedCooldown(int base) {
        return getFurnacePhase() == PHASE_OVERLOAD
                ? Math.max(1, Mth.ceil(base * 0.75F)) : base;
    }

    private Vec3 horizontalLook() {
        Vec3 look = getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        if (look.lengthSqr() < 0.001D) {
            float radians = getYRot() * Mth.DEG_TO_RAD;
            return new Vec3(-Mth.sin(radians), 0.0D, Mth.cos(radians));
        }
        return look.normalize();
    }

    private Vec3 chestPoint() {
        return position().add(0.0D, getBbHeight() * 0.67D, 0.0D)
                .add(horizontalLook().scale(0.63D));
    }

    private Vec3 shoulderPoint(boolean hammerSide) {
        Vec3 forward = horizontalLook();
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        return position().add(0.0D, getBbHeight() * 0.72D, 0.0D)
                .add(right.scale(hammerSide ? -0.62D : 0.62D));
    }

    private LivingEntity resolveLiving(@Nullable UUID uuid) {
        if (uuid == null || !(level() instanceof ServerLevel level)) {
            return null;
        }
        Entity entity = level.getEntity(uuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    public boolean isJindanVariant() {
        return entityData.get(JINDAN_VARIANT);
    }

    public String cultivationRealm() {
        return isJindanVariant()
                ? CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_MID
                : CultivationLevels.REALM_ZHUJI + CultivationLevels.STAGE_LATE;
    }

    public boolean isActive() {
        return entityData.get(ACTIVE);
    }

    public boolean isSmallSectGuardian() {
        return smallSectGuardian;
    }

    /** Marks a generation fallback as the unique guardian for one small sect. */
    public void configureAsSmallSectGuardian(String instanceKey) {
        smallSectGuardian = true;
        smallSectInstanceKey = instanceKey == null ? "" : instanceKey;
        smallSectRegistrationComplete = false;
        setPersistenceRequired();
    }

    public String getSmallSectInstanceKey() {
        return smallSectInstanceKey;
    }

    /** Returns this same persistent guardian to its assigned vault position. */
    public void returnToVaultHome() {
        if (level().isClientSide || !homeInitialized || !isAlive()) {
            return;
        }
        getNavigation().stop();
        setTarget(null);
        finishAction();
        entityData.set(ACTIVE, false);
        lostTargetTicks = 0;

        Vec3 current = position();
        BlockPos destination = guardHome;
        for (int rise = 0; rise <= 4; rise++) {
            BlockPos candidatePos = guardHome.above(rise);
            Vec3 candidate = Vec3.atBottomCenterOf(candidatePos);
            AABB targetBox = getBoundingBox().move(candidate.subtract(current));
            if (level().noCollision(this, targetBox)) {
                destination = candidatePos;
                break;
            }
        }
        Vec3 home = Vec3.atBottomCenterOf(destination);
        teleportTo(home.x, home.y, home.z);
        setDeltaMovement(Vec3.ZERO);
        resetFallDistance();
    }

    public void setSmallSectInstanceKey(String key) {
        smallSectInstanceKey = key == null ? "" : key;
    }

    public int getFurnacePressure() {
        return entityData.get(FURNACE_PRESSURE);
    }

    public byte getFurnacePhase() {
        return entityData.get(FURNACE_PHASE);
    }

    public boolean isCoreExposed() {
        return entityData.get(CORE_EXPOSED);
    }

    private void setCoreExposed(boolean exposed) {
        entityData.set(CORE_EXPOSED, exposed);
    }

    public boolean hasHeatedArmor() {
        return entityData.get(HEATED_ARMOR);
    }

    public byte getActionState() {
        return entityData.get(ACTION_STATE);
    }

    public int getActionTicks() {
        return entityData.get(ACTION_TICKS);
    }

    public int textureHeatStage() {
        if (getFurnacePhase() == PHASE_OVERLOAD || getFurnacePhase() == PHASE_VENTING) {
            return 2;
        }
        return getFurnacePressure() >= 55 || hasHeatedArmor() || isCoreExposed() ? 1 : 0;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("HomeInitialized", homeInitialized);
        tag.putInt("GuardHomeX", guardHome.getX());
        tag.putInt("GuardHomeY", guardHome.getY());
        tag.putInt("GuardHomeZ", guardHome.getZ());
        tag.putBoolean("Active", isActive());
        tag.putInt("FurnacePressure", getFurnacePressure());
        tag.putByte("FurnacePhase", getFurnacePhase());
        tag.putBoolean("OverloadUsed", overloadUsed);
        tag.putInt("OverloadTicks", overloadTicks);
        tag.putInt("VentTicks", ventTicks);
        tag.putInt("HeatedArmorTicks", heatedArmorTicks);
        tag.putInt("ForgingDomainTicks", forgingDomainTicks);
        tag.putDouble("ForgingDomainX", forgingDomainCenter.x);
        tag.putDouble("ForgingDomainY", forgingDomainCenter.y);
        tag.putDouble("ForgingDomainZ", forgingDomainCenter.z);
        tag.putInt("HammerCooldown", hammerCooldown);
        tag.putInt("FurnaceCooldown", furnaceCooldown);
        tag.putInt("DomainCooldown", domainCooldown);
        tag.putInt("SpikeCooldown", spikeCooldown);
        tag.putInt("GrabCooldown", grabCooldown);
        tag.putInt("HeatedArmorCooldown", heatedArmorCooldown);
        tag.putBoolean("SmallSectGuardian", smallSectGuardian);
        if (!smallSectInstanceKey.isEmpty()) {
            tag.putString("SmallSectInstance", smallSectInstanceKey);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        homeInitialized = tag.getBoolean("HomeInitialized");
        guardHome = new BlockPos(
                tag.getInt("GuardHomeX"), tag.getInt("GuardHomeY"),
                tag.getInt("GuardHomeZ"));
        entityData.set(ACTIVE, tag.getBoolean("Active"));
        entityData.set(FURNACE_PRESSURE,
                Mth.clamp(tag.getInt("FurnacePressure"), 0, PRESSURE_MAX));
        overloadUsed = tag.getBoolean("OverloadUsed");
        overloadTicks = Math.max(0, tag.getInt("OverloadTicks"));
        ventTicks = Math.max(0, tag.getInt("VentTicks"));
        heatedArmorTicks = Math.max(0, tag.getInt("HeatedArmorTicks"));
        entityData.set(HEATED_ARMOR, heatedArmorTicks > 0);
        forgingDomainTicks = Math.max(0, tag.getInt("ForgingDomainTicks"));
        forgingDomainCenter = new Vec3(
                tag.getDouble("ForgingDomainX"),
                tag.getDouble("ForgingDomainY"),
                tag.getDouble("ForgingDomainZ"));
        hammerCooldown = Math.max(0, tag.getInt("HammerCooldown"));
        furnaceCooldown = Math.max(0, tag.getInt("FurnaceCooldown"));
        domainCooldown = Math.max(0, tag.getInt("DomainCooldown"));
        spikeCooldown = Math.max(0, tag.getInt("SpikeCooldown"));
        grabCooldown = Math.max(0, tag.getInt("GrabCooldown"));
        heatedArmorCooldown = Math.max(0, tag.getInt("HeatedArmorCooldown"));
        smallSectGuardian = tag.getBoolean("SmallSectGuardian");
        smallSectInstanceKey = tag.getString("SmallSectInstance");
        smallSectRegistrationComplete = false;
        byte savedPhase = tag.getByte("FurnacePhase");
        if (savedPhase < PHASE_NORMAL || savedPhase > PHASE_VENTING) {
            savedPhase = PHASE_NORMAL;
        }
        setFurnacePhase(savedPhase);
        setCoreExposed(savedPhase == PHASE_VENTING);
        finishAction();
    }

    @Override
    public void die(DamageSource source) {
        if (smallSectGuardian && level() instanceof ServerLevel serverLevel) {
            SmallSectProtection.guardianDefeated(serverLevel, this);
        }
        super.die(source);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "vault_guardian", 3, state -> {
            if (isDeadOrDying()) {
                return state.setAndContinue(DEATH_ANIMATION);
            }
            if (getFurnacePhase() == PHASE_VENTING || getActionState() == ACTION_VENT) {
                return state.setAndContinue(VENT_ANIMATION);
            }
            if (getFurnacePhase() == PHASE_OVERLOAD
                    && getActionState() == ACTION_NONE) {
                return state.setAndContinue(OVERLOAD_ANIMATION);
            }
            return switch (getActionState()) {
                case ACTION_BASIC_STRIKE -> state.setAndContinue(BASIC_ANIMATION);
                case ACTION_HAMMER_SMASH -> state.setAndContinue(HAMMER_ANIMATION);
                case ACTION_FURNACE_STREAM -> state.setAndContinue(FURNACE_ANIMATION);
                case ACTION_FORGING_DOMAIN -> state.setAndContinue(DOMAIN_ANIMATION);
                case ACTION_SPIKE_VOLLEY -> state.setAndContinue(SPIKE_ANIMATION);
                case ACTION_GRAB -> state.setAndContinue(GRAB_ANIMATION);
                case ACTION_HEATED_ARMOR -> state.setAndContinue(HEATED_ANIMATION);
                case ACTION_OVERLOAD_START -> state.setAndContinue(OVERLOAD_ANIMATION);
                case ACTION_STAGGER -> state.setAndContinue(STAGGER_ANIMATION);
                case ACTION_ACTIVATE -> state.setAndContinue(ACTIVATE_ANIMATION);
                default -> state.isMoving() && isActive()
                        ? state.setAndContinue(WALK_ANIMATION)
                        : hasHeatedArmor()
                                ? state.setAndContinue(HEATED_ANIMATION)
                                : state.setAndContinue(IDLE_ANIMATION);
            };
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    private static int decrement(int value) {
        return value > 0 ? value - 1 : 0;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, path);
    }

    private static final class GuardianCombatGoal extends Goal {
        private final VaultGuardianEntity guardian;

        private GuardianCombatGoal(VaultGuardianEntity guardian) {
            this.guardian = guardian;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return guardian.isActive() && guardian.isValidIntruder(guardian.getTarget());
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() && guardian.insideGuardRadius(
                    guardian.getTarget(), GUARD_RADIUS + 4.0D);
        }

        @Override
        public void tick() {
            LivingEntity target = guardian.getTarget();
            if (!guardian.isValidIntruder(target)) {
                return;
            }
            guardian.getLookControl().setLookAt(target, 35.0F, 30.0F);
            if (guardian.getActionState() != ACTION_NONE
                    || guardian.getFurnacePhase() == PHASE_VENTING) {
                guardian.getNavigation().stop();
                return;
            }

            double distanceSqr = guardian.distanceToSqr(target);
            boolean treasureThreat = target.position().distanceToSqr(
                    Vec3.atCenterOf(guardian.guardHome)) <= 6.0D * 6.0D;

            if (guardian.heatedArmorCooldown == 0
                    && !guardian.hasHeatedArmor()
                    && (guardian.getFurnacePressure() >= 48
                            || guardian.getHealth() <= guardian.getMaxHealth() * 0.70F)) {
                guardian.startAction(ACTION_HEATED_ARMOR, 24, target);
                guardian.heatedArmorCooldown = guardian.overloadedCooldown(20 * 20);
                return;
            }
            if (guardian.grabCooldown == 0 && distanceSqr <= 4.0D * 4.0D
                    && guardian.random.nextFloat() < 0.42F) {
                guardian.startAction(ACTION_GRAB,
                        guardian.isJindanVariant() ? 62 : 50, target);
                guardian.grabCooldown = guardian.overloadedCooldown(16 * 20);
                return;
            }
            if (guardian.hammerCooldown == 0 && distanceSqr <= 3.5D * 3.5D) {
                guardian.startAction(ACTION_HAMMER_SMASH, 38, target);
                guardian.hammerCooldown = guardian.overloadedCooldown(6 * 20);
                return;
            }
            if (guardian.domainCooldown == 0 && distanceSqr <= 5.5D * 5.5D
                    && (treasureThreat || guardian.random.nextFloat() < 0.55F)) {
                guardian.startAction(ACTION_FORGING_DOMAIN, 46, target);
                guardian.domainCooldown = guardian.overloadedCooldown(14 * 20);
                return;
            }
            if (guardian.furnaceCooldown == 0
                    && distanceSqr >= 2.2D * 2.2D && distanceSqr <= 7.0D * 7.0D
                    && guardian.hasLineOfSight(target)) {
                guardian.startAction(ACTION_FURNACE_STREAM, 70, target);
                guardian.furnaceCooldown = guardian.overloadedCooldown(12 * 20);
                return;
            }
            if (guardian.spikeCooldown == 0 && distanceSqr <= 18.0D * 18.0D
                    && guardian.hasLineOfSight(target)) {
                guardian.startAction(ACTION_SPIKE_VOLLEY, 34, target);
                guardian.spikeCooldown = guardian.overloadedCooldown(10 * 20);
                return;
            }

            if (distanceSqr <= 2.85D * 2.85D && guardian.basicAttackCooldown == 0) {
                guardian.doHurtTarget(target);
                return;
            }

            double speed = guardian.isJindanVariant() ? 1.02D : 0.88D;
            Vec3 home = Vec3.atBottomCenterOf(guardian.guardHome);
            Vec3 homeToTarget = target.position().subtract(home)
                    .multiply(1.0D, 0.0D, 1.0D);
            if (homeToTarget.horizontalDistanceSqr() > 36.0D) {
                Vec3 guardPoint = home.add(homeToTarget.normalize().scale(4.0D));
                guardian.getNavigation().moveTo(
                        guardPoint.x, guardPoint.y, guardPoint.z, speed);
            } else {
                guardian.getNavigation().moveTo(target, speed);
            }
        }
    }

    private static final class ReturnToVaultGoal extends Goal {
        private final VaultGuardianEntity guardian;

        private ReturnToVaultGoal(VaultGuardianEntity guardian) {
            this.guardian = guardian;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return guardian.homeInitialized && !guardian.isActive()
                    && guardian.distanceToHomeSqr() > 1.4D * 1.4D;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            guardian.returnToVaultHome();
        }

        @Override
        public void stop() {
            guardian.getNavigation().stop();
        }
    }
}
