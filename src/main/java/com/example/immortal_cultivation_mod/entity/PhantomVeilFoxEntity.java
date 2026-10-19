package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.item.PhantomVeilFurItem;
import com.example.immortal_cultivation_mod.spiritual.SpiritualSenseSystem;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Realm-scaled illusion controller and assassin. */
public final class PhantomVeilFoxEntity extends RealmBeastMonster implements GeoEntity {
    // Visuals use synchronized BeastAttackVfx; server combat and sounds remain unchanged.
    private static final EntityDataAccessor<Integer> MASKS =
            SynchedEntityData.defineId(PhantomVeilFoxEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> ACTION_STATE =
            SynchedEntityData.defineId(PhantomVeilFoxEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ACTION_TICKS =
            SynchedEntityData.defineId(PhantomVeilFoxEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> HUNDRED_FACES =
            SynchedEntityData.defineId(PhantomVeilFoxEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> REVEALED_TICKS =
            SynchedEntityData.defineId(PhantomVeilFoxEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> VISUAL_MOVE =
            SynchedEntityData.defineId(PhantomVeilFoxEntity.class, EntityDataSerializers.INT);
    private static final byte ACTION_NONE = 0;
    private static final byte ACTION_POUNCE = 1;
    private static final byte ACTION_VANISH = 2;
    private static final byte ACTION_MAJOR_ILLUSION = 3;
    private static final byte ACTION_ULTIMATE = 4;

    private static final int SHIFT_COOLDOWN = 7 * 20;
    private static final int FOXFIRE_COOLDOWN = 10 * 20;
    private static final int SEVEN_ASPECTS_COOLDOWN = 16 * 20;
    private static final int FOG_COOLDOWN = 22 * 20;
    private static final int TAIL_STRIKE_COOLDOWN = 9 * 20;
    private static final int REFLECTION_COOLDOWN = 15 * 20;
    private static final int SEIZE_SENSE_COOLDOWN = 20 * 20;
    private static final int CORRIDOR_COOLDOWN = 28 * 20;
    private static final int ULTIMATE_COOLDOWN = 35 * 20;
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin()
            .thenLoop("animation.phantom_veil_fox.idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin()
            .thenLoop("animation.phantom_veil_fox.walk");
    private static final RawAnimation RUN_ANIM = RawAnimation.begin()
            .thenLoop("animation.phantom_veil_fox.run");
    private static final RawAnimation POUNCE_ANIM = RawAnimation.begin()
            .thenPlay("animation.phantom_veil_fox.pounce");
    private static final RawAnimation VANISH_ANIM = RawAnimation.begin()
            .thenPlay("animation.phantom_veil_fox.vanish");
    private static final RawAnimation DEATH_ANIM = RawAnimation.begin()
            .thenPlayAndHold("animation.phantom_veil_fox.death");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final Map<UUID, Integer> foxfireHitCounts = new HashMap<>();
    private int maskEvadeCooldown;
    private int untargetedTicks;
    private int combatDecisionCooldown = 25;
    private int untargetableTicks;
    private int cloneSpeedBoostTicks;
    private int cloneSpeedBoostStacks;
    private int shiftCooldown = 35;
    private int foxfireCooldown = 80;
    private int sevenAspectsCooldown = 150;
    private int fogCooldown = 220;
    private int tailStrikeCooldown = 100;
    private int reflectionCooldown = 180;
    private int seizeSenseCooldown = 260;
    private int corridorCooldown = 360;
    private int ultimateCooldown = 500;
    private boolean hundredFacesTriggered;
    private int hundredFacesTicks;
    private Vec3 fogCenter;
    private int fogTicks;
    private boolean wasInsideFog;
    private boolean fogExitBonusReady;
    private Vec3 corridorCenter;
    private int corridorTicks;
    private UUID pendingCastTarget;
    private int pendingCastTicks;
    private PendingTechnique pendingTechnique = PendingTechnique.NONE;
    private int pendingUltimateDamage;

    public PhantomVeilFoxEntity(EntityType<? extends PhantomVeilFoxEntity> type, Level level) {
        super(type, level);
        xpReward = realmTier().experienceReward();
        noCulling = true;

    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAttributes(PhantomVeilFoxTier.YUANYING);
    }

    public static AttributeSupplier.Builder createAttributes(PhantomVeilFoxTier tier) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, tier.maxHealth())
                .add(Attributes.ATTACK_DAMAGE, tier.attackDamage())
                .add(Attributes.ARMOR, tier.armor())
                .add(Attributes.ARMOR_TOUGHNESS, tier.armorToughness())
                .add(Attributes.MOVEMENT_SPEED, tier.movementSpeed())
                .add(Attributes.ATTACK_SPEED, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.STEP_HEIGHT, 1.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, tier.knockbackResistance());
    }

    public PhantomVeilFoxTier realmTier() {
        return switch (beastRealm()) {
            case 2 -> PhantomVeilFoxTier.ZHUJI;
            case 3 -> PhantomVeilFoxTier.JINDAN;
            default -> PhantomVeilFoxTier.YUANYING;
        };
    }

    public String cultivationRealm() {
        return beastCultivationLevel();
    }

    @Override
    public ItemEntity spawnAtLocation(ItemStack stack, float yOffset) {
        if (stack.is(ModItems.PHANTOM_VEIL_FUR.get())) {
            stack = stack.copy();
            PhantomVeilFurItem.setRealm(stack, cultivationRealm());
        }
        return super.spawnAtLocation(stack, yOffset);
    }

    public float techniqueDamage(float yuanyingDamage) {
        return realmTier().scaleTechniqueDamage(yuanyingDamage);
    }

    public static boolean checkSpawnRules(EntityType<PhantomVeilFoxEntity> type,
                                          ServerLevelAccessor level,
                                          MobSpawnType spawnType,
                                          BlockPos pos,
                                          net.minecraft.util.RandomSource random) {
        if (!Monster.checkAnyLightMonsterSpawnRules(type, level, spawnType, pos, random)) return false;
        BlockPos floor = pos.below();
        return level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                && level.getFluidState(pos).isEmpty()
                && level.getFluidState(pos.above()).isEmpty();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.22D, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.88D));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this, LivingEntity.class, true,
                target -> target instanceof Player || target instanceof CultivationNpc));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MASKS, 3);
        builder.define(VISUAL_MOVE, 0);
        builder.define(ACTION_STATE, ACTION_NONE);
        builder.define(ACTION_TICKS, 0);
        builder.define(HUNDRED_FACES, false);
        builder.define(REVEALED_TICKS, 0);
    }

    public int visualMove() { return entityData.get(ACTION_STATE) == ACTION_NONE ? 0 : entityData.get(VISUAL_MOVE); }
    public int visualActionTicks() { return entityData.get(ACTION_TICKS); }

    public int maskCount() {
        return entityData.get(MASKS);
    }

    public boolean isSpiritMaskVisible() {
        return entityData.get(ACTION_STATE) >= ACTION_MAJOR_ILLUSION
                || entityData.get(HUNDRED_FACES);
    }

    public boolean isRevealed() {
        return entityData.get(REVEALED_TICKS) > 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientIllusionEffects();
            return;
        }
        if (!(level() instanceof ServerLevel serverLevel)) return;
        tickSyncedState();
        tickCooldowns();
        updateSpiritualSenseReveal(serverLevel);
        updateDynamicAttributes(serverLevel);
        tickFogDomain(serverLevel);
        tickThousandIllusionCorridor(serverLevel);
        if (BeastDazeControl.blocksMoves(this)) {
            interruptMovesForDaze();
            return;
        }
        redirectMobTargetsToClones(serverLevel);
        updateUntargetedMaskGain(serverLevel);
        triggerHundredFaces(serverLevel);
        tickPendingTechnique(serverLevel);
        if (pendingTechnique == PendingTechnique.NONE && --combatDecisionCooldown <= 0) {
            combatDecisionCooldown = 8 + random.nextInt(8);
            chooseTechnique(serverLevel);
        }
    }

    private void tickSyncedState() {
        int actionTicks = entityData.get(ACTION_TICKS);
        if (actionTicks > 0) {
            entityData.set(ACTION_TICKS, actionTicks - 1);
            if (actionTicks == 1) entityData.set(ACTION_STATE, ACTION_NONE);
        }
        int revealed = entityData.get(REVEALED_TICKS);
        if (revealed > 0) entityData.set(REVEALED_TICKS, revealed - 1);
        if (maskEvadeCooldown > 0) maskEvadeCooldown--;
        if (untargetableTicks > 0) untargetableTicks--;
        if (cloneSpeedBoostTicks > 0 && --cloneSpeedBoostTicks == 0) {
            cloneSpeedBoostStacks = 0;
        }
        if (hundredFacesTicks > 0 && --hundredFacesTicks == 0) {
            entityData.set(HUNDRED_FACES, false);
        }
        setGlowingTag(isRevealed());
    }

    private void tickCooldowns() {
        shiftCooldown = decrement(shiftCooldown);
        foxfireCooldown = decrement(foxfireCooldown);
        sevenAspectsCooldown = decrement(sevenAspectsCooldown);
        fogCooldown = decrement(fogCooldown);
        tailStrikeCooldown = decrement(tailStrikeCooldown);
        reflectionCooldown = decrement(reflectionCooldown);
        seizeSenseCooldown = decrement(seizeSenseCooldown);
        corridorCooldown = decrement(corridorCooldown);
        ultimateCooldown = decrement(ultimateCooldown);
        if (entityData.get(HUNDRED_FACES) && tickCount % 10 < 3) {
            shiftCooldown = decrement(shiftCooldown);
            foxfireCooldown = decrement(foxfireCooldown);
            sevenAspectsCooldown = decrement(sevenAspectsCooldown);
            fogCooldown = decrement(fogCooldown);
            tailStrikeCooldown = decrement(tailStrikeCooldown);
            reflectionCooldown = decrement(reflectionCooldown);
            seizeSenseCooldown = decrement(seizeSenseCooldown);
            corridorCooldown = decrement(corridorCooldown);
            ultimateCooldown = decrement(ultimateCooldown);
        }
    }

    private static int decrement(int value) {
        return value > 0 ? value - 1 : 0;
    }

    private void chooseTechnique(ServerLevel level) {
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;
        double distance = distanceTo(target);
        if (ultimateCooldown == 0 && maskCount() >= 3 && distance <= 15.0D
                && (getHealth() <= getMaxHealth() * 0.65F || random.nextFloat() < 0.24F)) {
            beginMyriadMasks(target);
        } else if (fogCooldown == 0 && distance <= 10.0D && maskCount() <= 3) {
            useIllusionFog(level);
        } else if (sevenAspectsCooldown == 0 && distance <= 18.0D
                && activeClones(level).size() < 3) {
            useSevenAspects(level, target);
        } else if (corridorCooldown == 0 && distance <= 12.0D
                && random.nextFloat() < 0.35F) {
            useThousandIllusionCorridor(level, target);
        } else if (seizeSenseCooldown == 0 && distance <= 15.0D
                && random.nextFloat() < 0.40F) {
            beginSeizeSense(target);
        } else if (foxfireCooldown == 0 && distance <= 22.0D && distance >= 5.0D) {
            useBewilderingFoxfire(level, target);
        } else if (tailStrikeCooldown == 0 && distance <= extendedTailStrikeRange()) {
            useSoulSeveringTail(level, target);
        } else if (reflectionCooldown == 0 && distance <= 18.0D) {
            useSpiritReflection(level, target);
        } else if (shiftCooldown == 0 && distance <= 12.0D) {
            usePhantomExchange(level, target);
        }
    }

    /** 1. 幻影换位 */
    private void usePhantomExchange(ServerLevel level, LivingEntity target) {
        Vec3 oldPosition = position();
        Vec3 destination = findSafePositionNear(target.position(), 5.0D, 12.0D);
        if (destination == null) {
            shiftCooldown = 25;
            return;
        }
        setAction(ACTION_VANISH, 16);
        entityData.set(VISUAL_MOVE, 1);
        burst(level, ParticleTypes.REVERSE_PORTAL, oldPosition.add(0.0D, 0.8D, 0.0D),
                34, 0.8D, 0.09D);
        teleportTo(destination.x, destination.y, destination.z);
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        removeEffect(MobEffects.DIG_SLOWDOWN);
        untargetableTicks = 6;
        addMasks(1);
        int cloneCount = realmTier().exchangeClones()
                + (entityData.get(HUNDRED_FACES) ? 1 : 0);
        for (int i = 0; i < cloneCount; i++) {
            spawnClone(level, target, i == 0 ? oldPosition : destination, 80);
        }
        burst(level, ParticleTypes.WITCH, destination.add(0.0D, 0.8D, 0.0D),
                28, 0.7D, 0.05D);
        level.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.HOSTILE, 0.75F, 1.35F);
        shiftCooldown = SHIFT_COOLDOWN;
    }

    /** 2. 迷心狐火 */
    private void useBewilderingFoxfire(ServerLevel level, LivingEntity target) {
        setAction(ACTION_MAJOR_ILLUSION, 18);
        entityData.set(VISUAL_MOVE, 2);
        UUID castId = UUID.randomUUID();
        foxfireHitCounts.put(castId, 0);
        Vec3 forward = target.getEyePosition().subtract(getEyePosition()).normalize();
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x).normalize();
        for (int i = 0; i < 5; i++) {
            PhantomFoxfireProjectileEntity flame =
                    new PhantomFoxfireProjectileEntity(level, this);
            double side = (i - 2) * 0.38D;
            Vec3 origin = position().add(right.scale(side))
                    .add(0.0D, 0.9D + Math.abs(i - 2) * 0.12D, 0.0D);
            flame.setPos(origin.x, origin.y, origin.z);
            flame.configure(target, castId, Integer.compare(i - 2, 0));
            Vec3 aim = target.getEyePosition().subtract(origin).normalize()
                    .add(right.scale(side * 0.10D)).normalize();
            flame.shoot(aim.x, aim.y, aim.z, 1.15F, 2.0F);
            level.addFreshEntity(flame);
        }
        burst(level, ParticleTypes.SOUL_FIRE_FLAME, position().add(0.0D, 1.0D, 0.0D),
                25, 1.0D, 0.03D);
        level.playSound(null, blockPosition(), SoundEvents.SOUL_ESCAPE.value(),
                SoundSource.HOSTILE, 0.9F, 1.4F);
        foxfireCooldown = FOXFIRE_COOLDOWN;
    }

    public void recordFoxfireHit(LivingEntity target, UUID castId) {
        if (castId == null || !(level() instanceof ServerLevel level)) return;
        int hits = foxfireHitCounts.merge(castId, 1, Integer::sum);
        if (hits == 3) {
            long until = level.getGameTime() + 120L;
            target.getPersistentData().putLong("PhantomVeilBewilderedUntil", until);
            target.getPersistentData().putLong("PhantomVeilLockOnDisruptedUntil", until);
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0,
                    false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0,
                    false, true, true));
            burst(level, ParticleTypes.WITCH,
                    target.position().add(0.0D, target.getBbHeight() * 0.65D, 0.0D),
                    30, 0.65D, 0.05D);
        }
        if (hits >= 5) foxfireHitCounts.remove(castId);
    }

    /** 3. 镜身七相 */
    private void useSevenAspects(ServerLevel level, LivingEntity target) {
        setAction(ACTION_MAJOR_ILLUSION, 28);
        entityData.set(VISUAL_MOVE, 3);
        int count = realmTier().sevenAspectsClones()
                + (entityData.get(HUNDRED_FACES) ? 2 : 0);
        mistCurtain(level, position(), 4.0D);
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count;
            Vec3 around = target.position().add(
                    Math.cos(angle) * 4.2D, 0.0D, Math.sin(angle) * 4.2D);
            spawnClone(level, target, around, 160);
        }
        level.playSound(null, blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE,
                SoundSource.HOSTILE, 1.0F, 0.85F);
        sevenAspectsCooldown = SEVEN_ASPECTS_COOLDOWN;
    }

    /** 4. 幻雾迷域 */
    private void useIllusionFog(ServerLevel level) {
        setAction(ACTION_MAJOR_ILLUSION, 25);
        entityData.set(VISUAL_MOVE, 4);
        fogCenter = position();
        fogTicks = 200;
        wasInsideFog = true;
        fogExitBonusReady = false;
        addMasks(1);
        mistCurtain(level, fogCenter, 10.0D);
        fogCooldown = FOG_COOLDOWN;
    }

    private void tickFogDomain(ServerLevel level) {
        if (fogTicks <= 0 || fogCenter == null) {
            if (wasInsideFog) {
                wasInsideFog = false;
                fogExitBonusReady = true;
            }
            return;
        }
        fogTicks--;
        boolean inside = isInsideFog(position());
        if (wasInsideFog && !inside) fogExitBonusReady = true;
        wasInsideFog = inside;
        if (inside && tickCount % 4 == 0) shiftCooldown = decrement(shiftCooldown);
        if (tickCount % 50 == 0) {
            addMasks(1);
            maskGainParticles(level);
        }
        if (tickCount % 20 == 0) {
            for (LivingEntity target : combatTargets(level, fogCenter, 10.0D)) {
                target.hurt(damageSources().indirectMagic(this, this), techniqueDamage(90.0F));
                target.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN, 35, 0, false, true, true));
                target.addEffect(new MobEffectInstance(
                        MobEffects.DARKNESS, 35, 0, false, false, true));
                target.getPersistentData().putLong(
                        "PhantomVeilLockOnDisruptedUntil", level.getGameTime() + 35L);
            }
        }
        if (tickCount % 3 == 0) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = Math.sqrt(random.nextDouble()) * 10.0D;
            Vec3 point = fogCenter.add(Math.cos(angle) * radius,
                    random.nextDouble() * 2.2D, Math.sin(angle) * radius);
            burst(level, random.nextBoolean() ? ParticleTypes.CAMPFIRE_COSY_SMOKE
                            : ParticleTypes.WITCH,
                    point, 2, 0.25D, 0.015D);
        }
    }

    private boolean isInsideFog(Vec3 point) {
        return fogTicks > 0 && fogCenter != null
                && horizontalDistanceSqr(point, fogCenter) <= 100.0D;
    }

    /** 5. 影尾断魂 */
    private void useSoulSeveringTail(ServerLevel level, LivingEntity target) {
        double distance = distanceTo(target);
        if (distance > 12.0D && maskCount() > 0) setMasks(maskCount() - 1);
        Vec3 targetLook = horizontalLook(target);
        Vec3 behind = target.position().subtract(targetLook.scale(2.0D));
        Vec3 safe = findSafePositionNear(behind, 0.0D, 1.8D);
        boolean backAttack = safe != null;
        if (safe != null) {
            setAction(ACTION_VANISH, 11);
        entityData.set(VISUAL_MOVE, 5);
            teleportTo(safe.x, safe.y, safe.z);
            getLookControl().setLookAt(target, 90.0F, 90.0F);
        }
        float damage = techniqueDamage(backAttack ? 1_050.0F : 780.0F);
        if (entityData.get(HUNDRED_FACES)) damage += techniqueDamage(250.0F);
        hurtWithTechnique(target, damage);
        if (BeastMoveBlockBreaker.destructiveRealm(this) >= 3)
            BeastMoveBlockBreaker.breakSphere(level, this, target.position(), 2.8, 1.8, 48, 8);
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 80, 0,
                false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0,
                false, false, true));
        target.getPersistentData().putLong(
                "PhantomVeilSenseShockUntil", level.getGameTime() + 80L);
        crescentParticles(level,
                target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D),
                targetLook);
        level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.HOSTILE, 1.2F, 0.55F);
        tailStrikeCooldown = TAIL_STRIKE_COOLDOWN;
    }

    private double extendedTailStrikeRange() {
        return maskCount() > 0 ? 16.0D : 12.0D;
    }

    /** 6. 灵狐照影 */
    private void useSpiritReflection(ServerLevel level, LivingEntity target) {
        setAction(ACTION_MAJOR_ILLUSION, 20);
        entityData.set(VISUAL_MOVE, 6);
        hurtWithTechnique(target, techniqueDamage(420.0F));
        long until = level.getGameTime() + 140L;
        target.getPersistentData().putLong("PhantomVeilWrongDistanceUntil", until);
        target.getPersistentData().putLong("PhantomVeilLockOnDisruptedUntil", until);
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 140, 0,
                false, true, true));
        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.7D, 0.0D);
        for (int i = 0; i < 24; i++) {
            double angle = Math.PI * 2.0D * i / 24.0D;
            Vec3 point = center.add(Math.cos(angle) * 1.4D,
                    Math.sin(angle * 2.0D) * 0.6D,
                    Math.sin(angle) * 0.45D);
            burst(level, ParticleTypes.REVERSE_PORTAL, point, 1, 0.02D, 0.0D);
        }
        level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.HOSTILE, 1.0F, 0.7F);
        reflectionCooldown = REFLECTION_COOLDOWN;
    }

    /** 7. 幻面夺识, including its real 1.2-second cast. */
    private void beginSeizeSense(LivingEntity target) {
        setAction(ACTION_MAJOR_ILLUSION, 28);
        entityData.set(VISUAL_MOVE, 7);
        pendingTechnique = PendingTechnique.SEIZE_SENSE;
        pendingCastTarget = target.getUUID();
        pendingCastTicks = 24;
        getNavigation().stop();
        seizeSenseCooldown = SEIZE_SENSE_COOLDOWN;
    }

    private void finishSeizeSense(ServerLevel level, LivingEntity target) {
        hurtWithTechnique(target, techniqueDamage(600.0F));
        int duration = isHigherRealmOrBoss(target) ? 20 : 50;
        long until = level.getGameTime() + duration;
        target.getPersistentData().putLong("PhantomVeilSenseLostUntil", until);
        target.getPersistentData().putLong("PhantomVeilLockOnDisruptedUntil", until);
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, 0,
                false, true, true));
        Vec3 forehead = target.getEyePosition();
        for (int i = 0; i < 32; i++) {
            double angle = Math.PI * 2.0D * i / 32.0D;
            Vec3 point = forehead.add(Math.cos(angle) * 0.85D,
                    Math.sin(angle) * 0.55D,
                    Math.sin(angle * 2.0D) * 0.35D);
            burst(level, ParticleTypes.WITCH, point, 1, 0.01D, 0.0D);
        }
        level.playSound(null, target.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.HOSTILE, 0.45F, 1.75F);
    }

    private boolean isHigherRealmOrBoss(LivingEntity target) {
        int foxRealm = CultivationLevels.getStageIndex(cultivationRealm());
        if (target instanceof ServerPlayer player) {
            return CultivationLevels.getStageIndex(
                    ModAttachments.getData(player).cultivationLevel()) > foxRealm;
        }
        if (target instanceof CultivationNpc npc) {
            return CultivationLevels.getStageIndex(npc.npcRealm()) > foxRealm;
        }
        return target.getMaxHealth() > getMaxHealth();
    }

    /** 8. 千幻回廊 */
    private void useThousandIllusionCorridor(ServerLevel level, LivingEntity target) {
        setAction(ACTION_MAJOR_ILLUSION, 30);
        entityData.set(VISUAL_MOVE, 8);
        corridorCenter = target.position();
        corridorTicks = 160;
        for (int i = 0; i < 3; i++) {
            double angle = Math.PI * 2.0D * i / 3.0D;
            Vec3 point = corridorCenter.add(
                    Math.cos(angle) * 8.0D, 0.0D, Math.sin(angle) * 8.0D);
            spawnClone(level, target, point, 160);
        }
        corridorSheets(level);
        corridorCooldown = CORRIDOR_COOLDOWN;
    }

    private void tickThousandIllusionCorridor(ServerLevel level) {
        if (corridorTicks <= 0 || corridorCenter == null) return;
        corridorTicks--;
        if (tickCount % 20 == 0) {
            for (LivingEntity target : combatTargets(level, corridorCenter, 12.0D)) {
                target.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN, 30, 0, false, true, true));
                target.getPersistentData().putLong(
                        "PhantomVeilDirectionDisruptedUntil", level.getGameTime() + 30L);
            }
        }
        if (tickCount % 5 == 0) corridorSheets(level);
    }

    private void corridorSheets(ServerLevel level) {
        for (int i = 0; i < 7; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = 3.0D + random.nextDouble() * 9.0D;
            Vec3 base = corridorCenter.add(
                    Math.cos(angle) * radius, 0.2D, Math.sin(angle) * radius);
            burst(level, ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    base.add(0.0D, random.nextDouble() * 3.0D, 0.0D),
                    2, 0.15D, 0.005D);
            BlockParticleOption fakeSurface = new BlockParticleOption(
                    ParticleTypes.BLOCK,
                    random.nextBoolean() ? Blocks.STONE.defaultBlockState()
                            : Blocks.OAK_PLANKS.defaultBlockState());
            burst(level, fakeSurface, base.add(0.0D, 1.0D, 0.0D),
                    2, 0.3D, 0.01D);
        }
    }

    /** 9. 百面遁形 */
    private void triggerHundredFaces(ServerLevel level) {
        if (hundredFacesTriggered || getHealth() > getMaxHealth() * 0.30F) return;
        hundredFacesTriggered = true;
        hundredFacesTicks = 240;
        entityData.set(HUNDRED_FACES, true);
        setMasks(realmTier().maxMasks());
        setAction(ACTION_MAJOR_ILLUSION, 40);
        entityData.set(VISUAL_MOVE, 9);
        mistCurtain(level, position(), 7.0D);
        burst(level, ParticleTypes.END_ROD, position().add(0.0D, 1.1D, 0.0D),
                70, 1.8D, 0.08D);
        level.playSound(null, blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL,
                SoundSource.HOSTILE, 1.5F, 0.55F);
    }

    /** 10. 元婴技 — 万面归一 */
    private void beginMyriadMasks(LivingEntity target) {
        int consumed = maskCount();
        if (consumed < 3) return;
        setMasks(0);
        pendingUltimateDamage = Math.round(techniqueDamage(900 + consumed * 300));
        pendingTechnique = PendingTechnique.MYRIAD_MASKS;
        pendingCastTarget = target.getUUID();
        pendingCastTicks = 12;
        setAction(ACTION_ULTIMATE, 28);
        entityData.set(VISUAL_MOVE, 10);
        getNavigation().stop();
        if (level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < consumed; i++) {
                double angle = Math.PI * 2.0D * i / consumed;
                Vec3 point = target.position().add(
                        Math.cos(angle) * 5.5D, 0.0D, Math.sin(angle) * 5.5D);
                spawnClone(serverLevel, target, point, 22);
            }
            level().playSound(null, target.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON,
                    SoundSource.HOSTILE, 1.3F, 0.65F);
        }
        ultimateCooldown = ULTIMATE_COOLDOWN;
    }

    private void finishMyriadMasks(ServerLevel level, LivingEntity target) {
        hurtWithTechnique(target, pendingUltimateDamage);
        Vec3 impact = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
        burst(level, ParticleTypes.END_ROD, impact, 55, 1.2D, 0.18D);
        burst(level, ParticleTypes.WITCH, impact, 80, 1.5D, 0.12D);
        burst(level, ParticleTypes.REVERSE_PORTAL, impact, 60, 1.0D, 0.18D);
        level.playSound(null, target.blockPosition(), SoundEvents.TOTEM_USE,
                SoundSource.HOSTILE, 1.4F, 0.45F);
        pendingUltimateDamage = 0;
    }

    private void tickPendingTechnique(ServerLevel level) {
        if (pendingTechnique == PendingTechnique.NONE) return;
        LivingEntity target = pendingCastTarget != null
                && level.getEntity(pendingCastTarget) instanceof LivingEntity living
                ? living : null;
        if (target == null || !target.isAlive()) {
            clearPendingTechnique();
            return;
        }
        getLookControl().setLookAt(target, 80.0F, 80.0F);
        getNavigation().stop();
        if (pendingTechnique == PendingTechnique.SEIZE_SENSE && tickCount % 3 == 0) {
            drawMaskBeam(level, target);
        } else if (pendingTechnique == PendingTechnique.MYRIAD_MASKS && tickCount % 2 == 0) {
            inwardUltimateParticles(level, target);
        }
        if (--pendingCastTicks <= 0) {
            if (pendingTechnique == PendingTechnique.SEIZE_SENSE) {
                finishSeizeSense(level, target);
            } else if (pendingTechnique == PendingTechnique.MYRIAD_MASKS) {
                finishMyriadMasks(level, target);
            }
            clearPendingTechnique();
        }
    }

    private void clearPendingTechnique() {
        pendingTechnique = PendingTechnique.NONE;
        pendingCastTarget = null;
        pendingCastTicks = 0;
    }

    private void interruptMovesForDaze() {
        clearPendingTechnique();
        pendingUltimateDamage = 0;
        untargetableTicks = 0;
        entityData.set(ACTION_STATE, ACTION_NONE);
        entityData.set(ACTION_TICKS, 0);
        getNavigation().stop();
    }



    private void updateSpiritualSenseReveal(ServerLevel level) {
        if (tickCount % 5 != 0) return;
        int foxRealm = CultivationLevels.getStageIndex(cultivationRealm());
        for (ServerPlayer player : level.getEntitiesOfClass(
                ServerPlayer.class, getBoundingBox().inflate(32.0D),
                player -> player.isAlive() && SpiritualSenseSystem.isActive(player))) {
            int viewerRealm = CultivationLevels.getStageIndex(
                    ModAttachments.getData(player).cultivationLevel());
            if (viewerRealm >= foxRealm && player.hasLineOfSight(this)) {
                entityData.set(REVEALED_TICKS, Math.max(entityData.get(REVEALED_TICKS), 8));
            }
        }
    }

    private void updateDynamicAttributes(ServerLevel level) {
        if (tickCount % 5 != 0) return;
        boolean vulnerable = maskCount() == 0;
        int cloneCount = activeClones(level).size();
        boolean inFog = isInsideFog(position());
        PhantomVeilFoxTier tier = realmTier();
        double speed = tier.movementSpeed();
        if (vulnerable) speed *= 0.90D;
        if (entityData.get(HUNDRED_FACES)) speed *= 1.35D;
        if (cloneCount >= 3) speed *= 1.25D;
        if (inFog) speed *= 1.25D;
        speed *= 1.0D + cloneSpeedBoostStacks * 0.05D;
        double armor = tier.armor();
        if (vulnerable) armor *= 0.80D;
        if (entityData.get(HUNDRED_FACES)) armor *= 0.75D;
        AttributeInstance speedAttribute = getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance armorAttribute = getAttribute(Attributes.ARMOR);
        if (speedAttribute != null) speedAttribute.setBaseValue(speed);
        if (armorAttribute != null) armorAttribute.setBaseValue(armor);
    }

    private void updateUntargetedMaskGain(ServerLevel level) {
        if (tickCount % 10 != 0 || maskCount() >= realmTier().maxMasks()) return;
        boolean mobTargeting = !level.getEntitiesOfClass(
                Mob.class, getBoundingBox().inflate(20.0D),
                mob -> mob != this && mob.getTarget() == this).isEmpty();
        boolean playerAiming = !level.getEntitiesOfClass(
                ServerPlayer.class, getBoundingBox().inflate(32.0D),
                this::isLookingAtFox).isEmpty();
        if (mobTargeting || playerAiming || hurtTime > 0) {
            untargetedTicks = 0;
            return;
        }
        untargetedTicks += 10;
        if (untargetedTicks >= 60) {
            addMasks(1);
            untargetedTicks = 0;
            maskGainParticles(level);
        }
    }

    private boolean isLookingAtFox(ServerPlayer player) {
        if (!player.isAlive() || !player.hasLineOfSight(this)) return false;
        Vec3 toFox = getEyePosition().subtract(player.getEyePosition());
        return toFox.lengthSqr() < 1.0E-5D
                || player.getViewVector(1.0F).dot(toFox.normalize()) > 0.965D;
    }

    private void redirectMobTargetsToClones(ServerLevel level) {
        if (tickCount % 10 != 0) return;
        List<PhantomVeilFoxCloneEntity> clones = activeClones(level);
        if (clones.isEmpty()) return;
        for (Mob mob : level.getEntitiesOfClass(
                Mob.class, getBoundingBox().inflate(24.0D),
                mob -> mob != this && mob.getTarget() == this)) {
            mob.setTarget(clones.get(random.nextInt(clones.size())));
        }
        LivingEntity target = getTarget();
        if (target instanceof Mob mob
                && level.getGameTime()
                < mob.getPersistentData().getLong("PhantomVeilLockOnDisruptedUntil")) {
            mob.setTarget(clones.get(random.nextInt(clones.size())));
        }
    }

    public void onCloneDestroyed(Entity attacker) {
        cloneSpeedBoostStacks = Math.min(8, cloneSpeedBoostStacks + 1);
        cloneSpeedBoostTicks = 20;
        addMasks(1);
        if (level() instanceof ServerLevel level) maskGainParticles(level);
        if (attacker instanceof LivingEntity living
                && level().getGameTime()
                < living.getPersistentData().getLong("PhantomVeilWrongDistanceUntil")) {
            shiftCooldown = Math.max(0, shiftCooldown - 20);
        }
    }

    private void spawnClone(ServerLevel level, LivingEntity target, Vec3 preferred, int lifetime) {
        PhantomVeilFoxCloneEntity clone = ModEntities.PHANTOM_VEIL_FOX_CLONE.get().create(level);
        if (clone == null) return;
        Vec3 safe = findSafePositionNear(preferred, 0.0D, 2.5D);
        if (safe == null) safe = position();
        clone.moveTo(safe.x, safe.y, safe.z, getYRot(), 0.0F);
        clone.configure(this, target, lifetime);
        level.addFreshEntity(clone);
        burst(level, ParticleTypes.WITCH, safe.add(0.0D, 0.8D, 0.0D),
                12, 0.4D, 0.03D);
    }

    private List<PhantomVeilFoxCloneEntity> activeClones(ServerLevel level) {
        return level.getEntitiesOfClass(
                PhantomVeilFoxCloneEntity.class, getBoundingBox().inflate(64.0D),
                clone -> clone.ownerUuid().filter(getUUID()::equals).isPresent());
    }

    private boolean hurtWithTechnique(LivingEntity target, float amount) {
        boolean hurt = target.hurt(damageSources().indirectMagic(this, this), amount);
        if (hurt && fogExitBonusReady) {
            target.hurt(damageSources().indirectMagic(this, this), techniqueDamage(300.0F));
            fogExitBonusReady = false;
        }
        return hurt;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (BeastDazeControl.blocksMoves(this)) return false;
        setAction(ACTION_POUNCE, 15);
        entityData.set(VISUAL_MOVE, 11);
        boolean hurt = super.doHurtTarget(target);
        if (hurt && target instanceof LivingEntity living) {
            if (fogExitBonusReady) {
                living.hurt(damageSources().indirectMagic(this, this), techniqueDamage(300.0F));
                fogExitBonusReady = false;
            }
            living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0,
                    false, true, true));
        }
        return hurt;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return super.hurt(source, amount);
        if (untargetableTicks > 0) return false;

        boolean lightOrPurification = isLightOrPurification(source);
        boolean strongSense = isStrongSenseStrike(source, amount);
        if (lightOrPurification || strongSense) {
            entityData.set(REVEALED_TICKS, Math.max(entityData.get(REVEALED_TICKS), 80));
        }

        if (!BeastDazeControl.blocksMoves(this)
                && source.is(DamageTypeTags.IS_PROJECTILE)
                && corridorTicks > 0 && corridorCenter != null
                && horizontalDistanceSqr(position(), corridorCenter) <= 144.0D
                && random.nextFloat() < 0.30F) {
            evadeWithoutMask();
            return false;
        }

        boolean directHit = source.getEntity() instanceof LivingEntity;
        if (!BeastDazeControl.blocksMoves(this)
                && directHit && !lightOrPurification && !strongSense && maskEvadeCooldown == 0) {
            if (maskCount() > 0) {
                setMasks(maskCount() - 1);
                maskEvadeCooldown = 40;
                shatterMaskAndEvade();
                return false;
            }
            if (random.nextFloat() < 0.16F) {
                addMasks(1);
                maskEvadeCooldown = 40;
                shatterMaskAndEvade();
                return false;
            }
        }
        return super.hurt(source, adjustedIncomingDamage(source, amount));
    }

    private float adjustedIncomingDamage(DamageSource source, float amount) {
        String element = recentSpellElement(source);
        if (SpiritRoots.elementMatches(element, SpiritRoots.YANG)) return amount * 1.20F;
        if (SpiritRoots.elementMatches(element, SpiritRoots.WIND)
                || SpiritRoots.elementMatches(element, SpiritRoots.YIN)) {
            return amount * 0.70F;
        }
        if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)) {
            return amount * 0.40F;
        }
        return amount;
    }

    private boolean isLightOrPurification(DamageSource source) {
        return source.getDirectEntity() instanceof LightBeamProjectileEntity
                || SpiritRoots.elementMatches(recentSpellElement(source), SpiritRoots.YANG);
    }

    private boolean isStrongSenseStrike(DamageSource source, float amount) {
        if (source.getEntity() instanceof ServerPlayer player
                && SpiritualSenseSystem.isActive(player)) {
            int foxRealm = CultivationLevels.getStageIndex(cultivationRealm());
            int playerRealm = CultivationLevels.getStageIndex(
                    ModAttachments.getData(player).cultivationLevel());
            return playerRealm >= foxRealm || amount >= techniqueDamage(700.0F);
        }
        return (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC))
                && amount >= techniqueDamage(900.0F);
    }

    private String recentSpellElement(DamageSource source) {
        if (!(source.getEntity() instanceof ServerPlayer player)
                || source.getDirectEntity() == player) return "";
        long elapsed = player.level().getGameTime()
                - player.getPersistentData().getLong("MagicCenterLastSpellTime");
        return elapsed <= 100L
                ? player.getPersistentData().getString("MagicCenterLastSpellElement")
                : "";
    }

    private void shatterMaskAndEvade() {
        if (!(level() instanceof ServerLevel level)) return;
        Vec3 old = position();
        Vec3 safe = findSafePositionNear(old, 4.0D, 7.0D);
        if (safe != null) teleportTo(safe.x, safe.y, safe.z);
        setAction(ACTION_VANISH, 14);
        entityData.set(VISUAL_MOVE, 12);
        burst(level, ParticleTypes.WITCH, old.add(0.0D, 0.8D, 0.0D),
                24, 0.75D, 0.08D);
        burst(level, ParticleTypes.REVERSE_PORTAL, position().add(0.0D, 0.8D, 0.0D),
                22, 0.65D, 0.08D);
        level.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK,
                SoundSource.HOSTILE, 0.7F, 1.65F);
    }

    private void evadeWithoutMask() {
        if (!(level() instanceof ServerLevel level)) return;
        Vec3 safe = findSafePositionNear(position(), 3.0D, 5.5D);
        if (safe != null) teleportTo(safe.x, safe.y, safe.z);
        setAction(ACTION_VANISH, 10);
        entityData.set(VISUAL_MOVE, 12);
        burst(level, ParticleTypes.WITCH, position().add(0.0D, 0.8D, 0.0D),
                12, 0.5D, 0.04D);
    }

    private void addMasks(int amount) {
        setMasks(maskCount() + Math.max(0, amount));
    }

    private void setMasks(int masks) {
        entityData.set(MASKS, Math.max(0, Math.min(realmTier().maxMasks(), masks)));
    }

    private void setAction(byte action, int ticks) {
        entityData.set(ACTION_STATE, action);
        entityData.set(ACTION_TICKS, Math.max(1, ticks));
    }



    private Vec3 findSafePositionNear(Vec3 center, double minRadius, double maxRadius) {
        for (int attempt = 0; attempt < 18; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = minRadius + random.nextDouble()
                    * Math.max(0.2D, maxRadius - minRadius);
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            for (int yOffset : new int[] {0, 1, -1, 2, -2}) {
                Vec3 candidate = new Vec3(x, center.y + yOffset, z);
                if (canStandAt(candidate)) return candidate;
            }
        }
        return null;
    }

    private boolean canStandAt(Vec3 candidate) {
        Vec3 offset = candidate.subtract(position());
        AABB movedBox = getBoundingBox().move(offset);
        BlockPos feet = BlockPos.containing(candidate);
        BlockPos floor = feet.below();
        return level().getWorldBorder().isWithinBounds(feet)
                && level().noCollision(this, movedBox)
                && level().getFluidState(feet).isEmpty()
                && level().getFluidState(feet.above()).isEmpty()
                && level().getBlockState(floor).isFaceSturdy(level(), floor, Direction.UP);
    }

    private List<LivingEntity> combatTargets(ServerLevel level, Vec3 center, double radius) {
        AABB bounds = new AABB(center, center).inflate(radius, 4.0D, radius);
        return level.getEntitiesOfClass(LivingEntity.class, bounds,
                target -> target.isAlive() && target != this
                        && !(target instanceof PhantomVeilFoxCloneEntity)
                        && (target instanceof Player || target instanceof CultivationNpc));
    }

    private void clientIllusionEffects() {
        if (!isAlive()) return;


        int masks = maskCount();
        if (masks > 0 && tickCount % 5 == 0) {
            for (int i = 0; i < masks; i++) {
                double angle = tickCount * 0.08D + Math.PI * 2.0D * i / masks;
            }
        }
    }

    private void maskGainParticles(ServerLevel level) {
        burst(level, ParticleTypes.END_ROD, position().add(0.0D, 1.1D, 0.0D),
                12, 0.7D, 0.03D);
    }

    private void mistCurtain(ServerLevel level, Vec3 center, double radius) {
        for (int i = 0; i < 72; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double r = radius * (0.55D + random.nextDouble() * 0.45D);
            Vec3 point = center.add(Math.cos(angle) * r,
                    random.nextDouble() * 3.2D, Math.sin(angle) * r);
            burst(level, i % 3 == 0 ? ParticleTypes.WITCH
                            : ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    point, 1, 0.08D, 0.01D);
        }
    }

    private void crescentParticles(ServerLevel level, Vec3 center, Vec3 forward) {
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x).normalize();
        for (int i = -12; i <= 12; i++) {
            double t = i / 12.0D;
            Vec3 point = center.add(right.scale(t * 3.6D))
                    .add(0.0D, (1.0D - t * t) * 1.6D, 0.0D);
            burst(level, i % 2 == 0 ? ParticleTypes.WITCH : ParticleTypes.REVERSE_PORTAL,
                    point, 2, 0.04D, 0.01D);
        }
    }

    private void drawMaskBeam(ServerLevel level, LivingEntity target) {
        Vec3 start = getEyePosition();
        Vec3 end = target.getEyePosition();
        Vec3 delta = end.subtract(start);
        int steps = Math.max(2, (int) (delta.length() * 3.0D));
        for (int i = 0; i <= steps; i++) {
            Vec3 point = start.add(delta.scale(i / (double) steps));
            burst(level, ParticleTypes.WITCH, point, 1, 0.01D, 0.0D);
        }
    }

    private void inwardUltimateParticles(ServerLevel level, LivingEntity target) {
        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        for (int i = 0; i < 10; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            Vec3 outer = center.add(Math.cos(angle) * 5.5D,
                    (random.nextDouble() - 0.5D) * 2.0D,
                    Math.sin(angle) * 5.5D);
            Vec3 velocity = center.subtract(outer).normalize().scale(0.18D);
        }
    }

    private static void burst(ServerLevel level, ParticleOptions particle,
                              Vec3 position, int count, double spread, double speed) {}

    private static Vec3 horizontalLook(LivingEntity entity) {
        Vec3 look = entity.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        return horizontal.lengthSqr() < 1.0E-5D
                ? new Vec3(0.0D, 0.0D, 1.0D) : horizontal.normalize();
    }

    private static double horizontalDistanceSqr(Vec3 first, Vec3 second) {
        double x = first.x - second.x;
        double z = first.z - second.z;
        return x * x + z * z;
    }



    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Masks", maskCount());
        tag.putBoolean("HundredFacesTriggered", hundredFacesTriggered);
        tag.putInt("HundredFacesTicks", hundredFacesTicks);
        tag.putInt("ShiftCooldown", shiftCooldown);
        tag.putInt("FoxfireCooldown", foxfireCooldown);
        tag.putInt("SevenAspectsCooldown", sevenAspectsCooldown);
        tag.putInt("FogCooldown", fogCooldown);
        tag.putInt("TailStrikeCooldown", tailStrikeCooldown);
        tag.putInt("ReflectionCooldown", reflectionCooldown);
        tag.putInt("SeizeSenseCooldown", seizeSenseCooldown);
        tag.putInt("CorridorCooldown", corridorCooldown);
        tag.putInt("UltimateCooldown", ultimateCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setMasks(tag.contains("Masks") ? tag.getInt("Masks") : 3);
        hundredFacesTriggered = tag.getBoolean("HundredFacesTriggered");
        hundredFacesTicks = tag.getInt("HundredFacesTicks");
        entityData.set(HUNDRED_FACES, hundredFacesTicks > 0);
        shiftCooldown = tag.getInt("ShiftCooldown");
        foxfireCooldown = tag.getInt("FoxfireCooldown");
        sevenAspectsCooldown = tag.getInt("SevenAspectsCooldown");
        fogCooldown = tag.getInt("FogCooldown");
        tailStrikeCooldown = tag.getInt("TailStrikeCooldown");
        reflectionCooldown = tag.getInt("ReflectionCooldown");
        seizeSenseCooldown = tag.getInt("SeizeSenseCooldown");
        corridorCooldown = tag.getInt("CorridorCooldown");
        ultimateCooldown = tag.getInt("UltimateCooldown");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> {
            if (isDeadOrDying()) return state.setAndContinue(DEATH_ANIM);
            return switch (entityData.get(ACTION_STATE)) {
                case ACTION_POUNCE -> state.setAndContinue(POUNCE_ANIM);
                case ACTION_VANISH, ACTION_MAJOR_ILLUSION, ACTION_ULTIMATE ->
                        state.setAndContinue(VANISH_ANIM);
                default -> state.isMoving()
                        ? state.setAndContinue(getDeltaMovement().horizontalDistanceSqr() > 0.08D
                                ? RUN_ANIM : WALK_ANIM)
                        : state.setAndContinue(IDLE_ANIM);
            };
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    private enum PendingTechnique {
        NONE,
        SEIZE_SENSE,
        MYRIAD_MASKS
    }
}
