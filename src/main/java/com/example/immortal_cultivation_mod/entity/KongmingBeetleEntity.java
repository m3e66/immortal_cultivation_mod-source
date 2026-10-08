package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.ForcefieldCenterBehavior;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import static com.example.immortal_cultivation_mod.entity.BoneHitboxAnimation.ZERO;
import static com.example.immortal_cultivation_mod.entity.BoneHitboxAnimation.clip;
import static com.example.immortal_cultivation_mod.entity.BoneHitboxAnimation.curve;
import static com.example.immortal_cultivation_mod.entity.BoneHitboxAnimation.positionAndRotation;
import static com.example.immortal_cultivation_mod.entity.BoneHitboxAnimation.rotation;
import static com.example.immortal_cultivation_mod.entity.BoneHitboxAnimation.scaleAndRotation;

/**
 * 空冥甲虫: a subterranean spatial beast with serialized, one-action-at-a-time combat.
 */
public final class KongmingBeetleEntity extends Monster implements GeoEntity {
    public static final float MODEL_SCALE = 1.0F;

    private static final EntityDataAccessor<Byte> ACTION_STATE =
            SynchedEntityData.defineId(KongmingBeetleEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ACTION_TICKS =
            SynchedEntityData.defineId(KongmingBeetleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ENRAGED =
            SynchedEntityData.defineId(KongmingBeetleEntity.class, EntityDataSerializers.BOOLEAN);

    private static final byte ACTION_NONE = 0;
    private static final byte ACTION_BITE = 1;
    private static final byte ACTION_RIFT = 3;
    private static final byte ACTION_CRYSTAL_RAIN = 4;
    private static final byte ACTION_TELEPORT = 5;

    private static final int BITE_TICKS = 12;
    private static final int RIFT_TICKS = 32;
    private static final int CRYSTAL_RAIN_TICKS = 42;
    private static final int TELEPORT_TICKS = 16;

    private static final double BASE_MOVE_SPEED = 0.22D;
    private static final double ENRAGED_MOVE_SPEED = 0.30D;
    private static final float RIFT_DAMAGE = 115.0F;

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin()
            .thenLoop("animation.immortal_cultivation_mod.kongming_beetle.idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin()
            .thenLoop("animation.immortal_cultivation_mod.kongming_beetle.walk");
    private static final RawAnimation BITE_ANIM = RawAnimation.begin()
            .thenPlay("animation.immortal_cultivation_mod.kongming_beetle.bite");
    private static final RawAnimation RIFT_ANIM = RawAnimation.begin()
            .thenPlay("animation.immortal_cultivation_mod.kongming_beetle.rift_clamp");
    private static final RawAnimation CRYSTAL_RAIN_ANIM = RawAnimation.begin()
            .thenPlay("animation.immortal_cultivation_mod.kongming_beetle.crystal_rain");
    private static final RawAnimation TELEPORT_ANIM = RawAnimation.begin()
            .thenPlay("animation.immortal_cultivation_mod.kongming_beetle.teleport");
    private static final RawAnimation DEATH_ANIM = RawAnimation.begin()
            .thenPlayAndHold("animation.immortal_cultivation_mod.kongming_beetle.death");

    private static final BoneHitboxAnimation.Clip IDLE_HITBOX = clip(2.0F, true,
            positionAndRotation("body", ZERO, ZERO, ZERO,
                    ZERO, curve(0.0F, 0.0F, 1.0F, 0.2F, 2.0F, 0.0F), ZERO),
            rotation("crystal_cluster", ZERO,
                    curve(0.0F, -4.0F, 1.0F, 4.0F, 2.0F, -4.0F), ZERO));
    private static final BoneHitboxAnimation.Clip WALK_HITBOX = clip(0.8F, true,
            positionAndRotation("body", ZERO, ZERO, ZERO,
                    ZERO, curve(0.0F, 0.0F, 0.2F, 0.35F, 0.4F, 0.0F,
                            0.6F, 0.35F, 0.8F, 0.0F), ZERO),
            rotation("front_left_leg", ZERO, ZERO,
                    curve(0.0F, 24.0F, 0.4F, -24.0F, 0.8F, 24.0F)),
            rotation("front_right_leg", ZERO, ZERO,
                    curve(0.0F, -24.0F, 0.4F, 24.0F, 0.8F, -24.0F)),
            rotation("mid_left_leg", ZERO, ZERO,
                    curve(0.0F, -20.0F, 0.4F, 20.0F, 0.8F, -20.0F)),
            rotation("mid_right_leg", ZERO, ZERO,
                    curve(0.0F, 20.0F, 0.4F, -20.0F, 0.8F, 20.0F)),
            rotation("back_left_leg", ZERO, ZERO,
                    curve(0.0F, 24.0F, 0.4F, -24.0F, 0.8F, 24.0F)),
            rotation("back_right_leg", ZERO, ZERO,
                    curve(0.0F, -24.0F, 0.4F, 24.0F, 0.8F, -24.0F)));
    private static final BoneHitboxAnimation.Clip RIFT_HITBOX = clip(1.6F, false,
            rotation("left_mandible", ZERO,
                    curve(0.0F, 0.0F, 0.45F, -38.0F, 1.1F, -38.0F, 1.6F, 0.0F), ZERO),
            rotation("right_mandible", ZERO,
                    curve(0.0F, 0.0F, 0.45F, 38.0F, 1.1F, 38.0F, 1.6F, 0.0F), ZERO));
    private static final BoneHitboxAnimation.Clip CRYSTAL_HITBOX = clip(2.1F, false,
            rotation("body",
                    curve(0.0F, 0.0F, 0.25F, -5.0F, 0.5F, 5.0F,
                            0.75F, -5.0F, 1.0F, 5.0F, 1.3F, 0.0F),
                    ZERO, ZERO),
            rotation("crystal_cluster", ZERO,
                    curve(0.0F, 0.0F, 1.1F, 180.0F, 2.1F, 360.0F), ZERO));
    private static final BoneHitboxAnimation.Clip TELEPORT_HITBOX = clip(0.8F, false,
            scaleAndRotation("root", ZERO, ZERO, ZERO,
                    curve(0.0F, 1.0F, 0.35F, 0.08F, 0.45F, 0.08F, 0.8F, 1.0F),
                    curve(0.0F, 1.0F, 0.35F, 1.18F, 0.45F, 1.18F, 0.8F, 1.0F),
                    curve(0.0F, 1.0F, 0.35F, 1.0F, 0.45F, 1.0F, 0.8F, 1.0F)));

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final ModelBonePartEntity<KongmingBeetleEntity> bodyPart;
    private final ModelBonePartEntity<KongmingBeetleEntity> headPart;
    private final ModelBonePartEntity<KongmingBeetleEntity> crystalPart;
    private final ModelBonePartEntity<KongmingBeetleEntity> leftShellPart;
    private final ModelBonePartEntity<KongmingBeetleEntity> rightShellPart;
    private final ModelBonePartEntity<KongmingBeetleEntity> frontLeftLegPart;
    private final ModelBonePartEntity<KongmingBeetleEntity> frontRightLegPart;
    private final ModelBonePartEntity<KongmingBeetleEntity> midLeftLegPart;
    private final ModelBonePartEntity<KongmingBeetleEntity> midRightLegPart;
    private final ModelBonePartEntity<KongmingBeetleEntity> backLeftLegPart;
    private final ModelBonePartEntity<KongmingBeetleEntity> backRightLegPart;
    private final ModelBonePartEntity<?>[] boneParts;

    private int riftCooldown;
    private int crystalRainCooldown;
    private int teleportCooldown;
    private boolean teleportRequested;
    private Vec3 teleportDestination;
    private DamageSource lastExplosionDamageSource;
    private long lastExplosionDamageTick = Long.MIN_VALUE;

    public KongmingBeetleEntity(
            EntityType<? extends KongmingBeetleEntity> type, Level level) {
        super(type, level);
        bodyPart = new ModelBonePartEntity<>(this, "body", 1.45F, 0.72F);
        headPart = new ModelBonePartEntity<>(this, "head", 0.86F, 0.58F);
        crystalPart = new ModelBonePartEntity<>(this, "crystal_cluster", 0.62F, 0.62F);
        leftShellPart = new ModelBonePartEntity<>(this, "left_shell", 0.76F, 0.62F);
        rightShellPart = new ModelBonePartEntity<>(this, "right_shell", 0.76F, 0.62F);
        frontLeftLegPart = new ModelBonePartEntity<>(this, "front_left_leg", 0.62F, 0.25F);
        frontRightLegPart = new ModelBonePartEntity<>(this, "front_right_leg", 0.62F, 0.25F);
        midLeftLegPart = new ModelBonePartEntity<>(this, "mid_left_leg", 0.66F, 0.25F);
        midRightLegPart = new ModelBonePartEntity<>(this, "mid_right_leg", 0.66F, 0.25F);
        backLeftLegPart = new ModelBonePartEntity<>(this, "back_left_leg", 0.62F, 0.25F);
        backRightLegPart = new ModelBonePartEntity<>(this, "back_right_leg", 0.62F, 0.25F);
        boneParts = new ModelBonePartEntity<?>[]{
                bodyPart, headPart, crystalPart, leftShellPart, rightShellPart,
                frontLeftLegPart, frontRightLegPart, midLeftLegPart, midRightLegPart,
                backLeftLegPart, backRightLegPart
        };
        setId(ENTITY_COUNTER.getAndAdd(boneParts.length + 1) + 1);
        noCulling = true;
        xpReward = 180;
        updateBoneHitboxes();
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        if (boneParts != null) {
            for (int index = 0; index < boneParts.length; index++) {
                boneParts[index].setId(id + index + 1);
            }
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1200.0D)
                .add(Attributes.ATTACK_DAMAGE, 90.0D)
                .add(Attributes.ARMOR, 22.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, BASE_MOVE_SPEED)
                .add(Attributes.FOLLOW_RANGE, 42.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85D);
    }

    public static boolean checkKongmingSpawnRules(
            EntityType<KongmingBeetleEntity> type,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random) {
        if (!Mob.checkMobSpawnRules(type, level, spawnType, pos, random)) {
            return false;
        }
        if (spawnType != MobSpawnType.SPAWNER
                && spawnType != MobSpawnType.SPAWN_EGG
                && spawnType != MobSpawnType.COMMAND) {
            if (pos.getY() > level.getSeaLevel() - 10
                    || level.getMaxLocalRawBrightness(pos) > 7
                    || random.nextInt(5) != 0) {
                return false;
            }
        }
        BlockPos floor = pos.below();
        return level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                && level.getFluidState(pos).isEmpty()
                && level.getFluidState(pos.above()).isEmpty();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.68D));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 14.0F));
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
        builder.define(ENRAGED, false);
    }

    @Override
    public void tick() {
        super.tick();
        updateBoneHitboxes();
        if (!level().isClientSide || !isAlive()) {
            return;
        }
        if ((tickCount & 3) == 0) {
            double orbit = tickCount * (isEnraged() ? 0.33D : 0.18D);
            double radius = isEnraged() ? 0.95D : 0.72D;
            level().addParticle(ModParticles.METHOD_QI_DARK.get(),
                    getX() + Math.cos(orbit) * radius,
                    getY() + 0.72D + Math.sin(orbit * 0.5D) * 0.2D,
                    getZ() + Math.sin(orbit) * radius,
                    0.0D, 0.012D, 0.0D);
        }
        if (isEnraged() && (tickCount & 1) == 0) {
            level().addParticle(ParticleTypes.REVERSE_PORTAL,
                    getX() + (random.nextDouble() - 0.5D) * 1.8D,
                    getY() + 0.25D + random.nextDouble() * 0.9D,
                    getZ() + (random.nextDouble() - 0.5D) * 1.8D,
                    0.0D, 0.02D, 0.0D);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || !isAlive()) {
            return;
        }
        if (BeastDazeControl.blocksMoves(this)) {
            finishAction(getActionState());
            teleportRequested = false;
            return;
        }

        decrementCooldowns();
        if (!isEnraged() && getHealth() <= getMaxHealth() * 0.35F) {
            enterEnragedState();
        }
        if (getActionState() != ACTION_NONE) {
            tickCurrentAction();
            return;
        }
        if ((tickCount + getId()) % 6 != 0) {
            return;
        }

        LivingEntity target = getTarget();
        if (!canUseAbilityOn(target)) {
            return;
        }
        double distanceSquared = distanceToSqr(target);

        if (teleportRequested && teleportCooldown <= 0) {
            Vec3 destination = findTeleportDestination(target);
            if (destination != null) {
                teleportDestination = destination;
                teleportRequested = false;
                teleportCooldown = isEnraged() ? 75 : 120;
                beginAction(ACTION_TELEPORT, TELEPORT_TICKS);
                return;
            }
            teleportRequested = false;
        }

        if (riftCooldown <= 0
                && distanceSquared >= 2.0D * 2.0D
                && distanceSquared <= 8.5D * 8.5D
                && hasLineOfSight(target)
                && random.nextFloat() < 0.28F) {
            riftCooldown = isEnraged() ? 78 : 120;
            beginAction(ACTION_RIFT, RIFT_TICKS);
            return;
        }

        if (crystalRainCooldown <= 0
                && distanceSquared >= 3.0D * 3.0D
                && distanceSquared <= 16.0D * 16.0D
                && hasLineOfSight(target)
                && random.nextFloat() < 0.24F) {
            crystalRainCooldown = isEnraged() ? 105 : 155;
            beginAction(ACTION_CRYSTAL_RAIN, CRYSTAL_RAIN_TICKS);
            return;
        }

    }

    private void decrementCooldowns() {
        if (riftCooldown > 0) riftCooldown--;
        if (crystalRainCooldown > 0) crystalRainCooldown--;
        if (teleportCooldown > 0) teleportCooldown--;
    }

    private void tickCurrentAction() {
        int ticks = getActionTicks();
        byte action = getActionState();
        LivingEntity target = getTarget();

        getNavigation().stop();
        Vec3 motion = getDeltaMovement();
        setDeltaMovement(motion.x * 0.2D, motion.y, motion.z * 0.2D);
        if (canUseAbilityOn(target)) {
            lookAt(target, 28.0F, 28.0F);
        }

        if (level() instanceof ServerLevel level) {
            switch (action) {
                case ACTION_RIFT -> tickRiftClamp(level, target, ticks);
                case ACTION_CRYSTAL_RAIN -> tickCrystalRain(level, target, ticks);
                case ACTION_TELEPORT -> tickTeleport(level, ticks);
                default -> {
                }
            }
        }

        if (ticks <= 1) {
            finishAction(action);
        } else {
            entityData.set(ACTION_TICKS, ticks - 1);
        }
    }

    private void tickRiftClamp(ServerLevel level, LivingEntity target, int ticks) {
        if (!canUseAbilityOn(target)) {
            return;
        }
        Vec3 center = target.getBoundingBox().getCenter();
        Vec3 toTarget = center.subtract(getBoundingBox().getCenter());
        Vec3 right = new Vec3(-toTarget.z, 0.0D, toTarget.x);
        if (right.lengthSqr() < 0.001D) right = new Vec3(1.0D, 0.0D, 0.0D);
        right = right.normalize();
        double separation = Math.max(0.25D, ticks / (double) RIFT_TICKS) * 2.25D;
        Vec3 leftRift = center.add(right.scale(separation));
        Vec3 rightRift = center.subtract(right.scale(separation));
        emitVerticalRift(level, leftRift, target.getBbHeight() + 0.45D);
        emitVerticalRift(level, rightRift, target.getBbHeight() + 0.45D);

        Vec3 pull = center.subtract(target.getBoundingBox().getCenter());
        target.setDeltaMovement(target.getDeltaMovement().scale(0.45D).add(pull.scale(0.25D)));
        if (ticks == 10) {
            performRiftCollapse(level, center);
        }
    }

    private void performRiftCollapse(ServerLevel level, Vec3 center) {
        AABB area = new AABB(center, center).inflate(2.6D, 1.8D, 2.6D);
        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class, area, this::canUseAbilityOn)) {
            if (!damageAcrossForcefield(target, RIFT_DAMAGE)) {
                continue;
            }
            target.hurt(damageSources().mobAttack(this), RIFT_DAMAGE);
            Vec3 pull = center.subtract(target.getBoundingBox().getCenter());
            if (pull.lengthSqr() > 0.001D) {
                target.setDeltaMovement(target.getDeltaMovement().scale(0.2D)
                        .add(pull.normalize().scale(0.78D)));
            }
            target.addEffect(new MobEffectInstance(ModEffects.TECHNIQUE_SLOW,
                    32, 4, false, true, true));
            target.addEffect(new MobEffectInstance(ModEffects.DAZE,
                    12, 0, false, true, true));
        }
        BeastMoveBlockBreaker.breakSphere(level, this, center,
                2.4D, 1.6D, 24, 5.0F);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                center.x, center.y, center.z,
                65, 1.6D, 1.0D, 1.6D, 0.42D);
        level.sendParticles(ModParticles.METHOD_QI_DARK.get(),
                center.x, center.y, center.z,
                36, 1.2D, 0.8D, 1.2D, 0.13D);
        level.playSound(null, BlockPos.containing(center),
                SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                SoundSource.HOSTILE, 1.2F, 0.62F);
    }

    private void tickCrystalRain(ServerLevel level, LivingEntity target, int ticks) {
        Vec3 shell = position().add(0.0D, 1.05D, 0.0D);
        if (ticks >= 27 && ticks % 2 == 0) {
            level.sendParticles(ModParticles.METHOD_QI_DARK.get(),
                    shell.x, shell.y, shell.z,
                    10, 0.72D, 0.25D, 0.72D, 0.06D);
            level.sendParticles(ParticleTypes.ENCHANT,
                    shell.x, shell.y, shell.z,
                    8, 0.55D, 0.2D, 0.55D, 0.18D);
        }
        if (ticks == 27) {
            level.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.HOSTILE, 1.3F, 0.52F);
        }
        if (ticks == 20 && canUseAbilityOn(target)) {
            launchCrystalRain(level, target);
        }
    }

    private void launchCrystalRain(ServerLevel level, LivingEntity target) {
        int count = 4 + random.nextInt(5);
        Vec3 center = target.position();
        for (int index = 0; index < count; index++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double radius = index == 0 ? 0.0D : 0.7D + random.nextDouble() * 2.6D;
            double x = center.x + Math.cos(angle) * radius;
            double y = center.y + 5.0D + random.nextDouble() * 3.0D;
            double z = center.z + Math.sin(angle) * radius;
            Vec3 rift = new Vec3(x, y + 0.4D, z);
            emitVerticalRift(level, rift, 1.1D);

            KongmingCrystalProjectileEntity projectile =
                    new KongmingCrystalProjectileEntity(level, this);
            projectile.setPos(x, y, z);
            Vec3 aim = center.add(
                    (random.nextDouble() - 0.5D) * 2.2D,
                    target.getBbHeight() * 0.35D,
                    (random.nextDouble() - 0.5D) * 2.2D);
            Vec3 direction = aim.subtract(projectile.position());
            projectile.shoot(direction.x, direction.y, direction.z,
                    0.95F, 4.0F);
            level.addFreshEntity(projectile);
        }
        level.playSound(null, target.blockPosition(), SoundEvents.SHULKER_SHOOT,
                SoundSource.HOSTILE, 1.0F, 0.62F);
    }

    private void tickTeleport(ServerLevel level, int ticks) {
        Vec3 center = getBoundingBox().getCenter();
        if (ticks >= 9) {
            double squeeze = Math.max(0.05D, (ticks - 8) / 8.0D);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    center.x, center.y, center.z,
                    12, 0.55D * squeeze, 0.55D, 0.55D * squeeze, 0.08D);
        }
        if (ticks != 8 || teleportDestination == null) {
            return;
        }
        if (CenterEffectSupport.isForcefieldRouteBlocked(
                this, center, teleportDestination)
                || !canOccupy(level, teleportDestination)) {
            teleportDestination = null;
            return;
        }
        Vec3 old = position();
        teleportTo(teleportDestination.x, teleportDestination.y, teleportDestination.z);
        emitVerticalRift(level, old.add(0.0D, 0.55D, 0.0D), 1.75D);
        emitVerticalRift(level, teleportDestination.add(0.0D, 0.55D, 0.0D), 1.75D);
        level.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.HOSTILE, 1.05F, 0.72F);
        teleportDestination = null;
    }

    private Vec3 findTeleportDestination(LivingEntity target) {
        Vec3 toTarget = horizontalDirectionTo(target);
        Vec3 side = new Vec3(-toTarget.z, 0.0D, toTarget.x);
        if (side.lengthSqr() < 0.5D) side = new Vec3(1.0D, 0.0D, 0.0D);
        if (random.nextBoolean()) side = side.scale(-1.0D);

        for (int attempt = 0; attempt < 12; attempt++) {
            double distance = 5.0D + random.nextDouble() * 3.0D;
            double angle = (random.nextDouble() - 0.5D) * 1.2D;
            Vec3 direction = side.yRot((float) angle).normalize();
            double x = getX() + direction.x * distance;
            double z = getZ() + direction.z * distance;
            int originY = blockPosition().getY();
            for (int yOffset = 2; yOffset >= -3; yOffset--) {
                BlockPos feet = BlockPos.containing(x, originY + yOffset, z);
                BlockPos floor = feet.below();
                if (!level().getBlockState(floor).isFaceSturdy(level(), floor, Direction.UP)
                        || !level().getFluidState(feet).isEmpty()
                        || !level().getFluidState(feet.above()).isEmpty()) {
                    continue;
                }
                Vec3 destination = Vec3.atBottomCenterOf(feet);
                if (canOccupy(level(), destination)
                        && !CenterEffectSupport.isForcefieldRouteBlocked(
                                this, getBoundingBox().getCenter(), destination)) {
                    return destination;
                }
            }
        }
        return null;
    }

    private boolean canOccupy(Level level, Vec3 destination) {
        AABB box = new AABB(
                destination.x - 0.82D, destination.y,
                destination.z - 0.82D,
                destination.x + 0.82D, destination.y + 1.15D,
                destination.z + 0.82D);
        return level.noCollision(this, box);
    }

    private void emitVerticalRift(ServerLevel level, Vec3 center, double height) {
        int points = Math.max(5, Mth.ceil(height * 7.0D));
        for (int index = 0; index < points; index++) {
            double y = center.y - height * 0.5D
                    + height * index / Math.max(1, points - 1);
            level.sendParticles(ModParticles.METHOD_QI_DARK.get(),
                    center.x, y, center.z,
                    1, 0.025D, 0.02D, 0.025D, 0.0D);
            if ((index & 2) == 0) {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                        center.x, y, center.z,
                        1, 0.04D, 0.02D, 0.04D, 0.01D);
            }
        }
    }

    private boolean damageAcrossForcefield(LivingEntity target, float damage) {
        return !ForcefieldCenterBehavior.interceptAttackPath(
                this, getBoundingBox().getCenter(),
                target.getBoundingBox().getCenter(), damage);
    }

    private boolean canUseAbilityOn(LivingEntity target) {
        return target != null
                && target.isAlive()
                && target != this
                && (target instanceof Player || target instanceof CultivationNpc)
                && !isAlliedTo(target);
    }

    private Vec3 horizontalDirectionTo(LivingEntity target) {
        Vec3 direction = target.position().subtract(position()).multiply(1.0D, 0.0D, 1.0D);
        if (direction.lengthSqr() < 0.001D) {
            direction = getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        }
        return direction.lengthSqr() < 0.001D
                ? new Vec3(0.0D, 0.0D, 1.0D)
                : direction.normalize();
    }

    private void enterEnragedState() {
        entityData.set(ENRAGED, true);
        var movement = getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) {
            movement.setBaseValue(ENRAGED_MOVE_SPEED);
        }
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ModParticles.METHOD_QI_DARK.get(),
                    getX(), getY() + 0.65D, getZ(),
                    90, 1.3D, 0.8D, 1.3D, 0.2D);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    getX(), getY() + 0.65D, getZ(),
                    75, 1.25D, 0.85D, 1.25D, 0.32D);
            level.playSound(null, blockPosition(), SoundEvents.WARDEN_ROAR,
                    SoundSource.HOSTILE, 0.9F, 1.55F);
        }
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        if (BeastDazeControl.blocksMoves(this) || getActionState() != ACTION_NONE) {
            return false;
        }
        boolean hurt = super.doHurtTarget(entity);
        if (hurt) {
            beginAction(ACTION_BITE, BITE_TICKS);
        }
        return hurt;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            long time = level().getGameTime();
            if (source == lastExplosionDamageSource && time == lastExplosionDamageTick) {
                return false;
            }
            lastExplosionDamageSource = source;
            lastExplosionDamageTick = time;
        }

        Entity direct = source.getDirectEntity();
        boolean projectile = direct instanceof Projectile;
        boolean weakPoint = isBackCrystalHit(source);
        if (projectile) {
            if (weakPoint) {
                amount *= isEnraged() ? 1.55F : 1.35F;
            } else {
                amount *= isEnraged() ? 0.70F : 0.45F;
                if (random.nextFloat() < (isEnraged() ? 0.12F : 0.22F)) {
                    redirectProjectile((Projectile) direct);
                    shellImpactVfx(source, true);
                    return false;
                }
            }
        } else if (weakPoint) {
            amount *= isEnraged() ? 1.45F : 1.25F;
        }

        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && isAlive()) {
            shellImpactVfx(source, false);
            if (!BeastDazeControl.blocksMoves(this)
                    && teleportCooldown <= 0 && random.nextFloat() < 0.22F) {
                teleportRequested = true;
            }
        }
        return hurt;
    }

    private boolean isBackCrystalHit(DamageSource source) {
        Entity sourceEntity = source.getEntity();
        Entity direct = source.getDirectEntity();
        Vec3 origin = sourceEntity != null
                ? sourceEntity.getBoundingBox().getCenter()
                : direct != null ? direct.getBoundingBox().getCenter() : null;
        if (origin == null || origin.y < getY() + getBbHeight() * 0.45D) {
            return false;
        }
        Vec3 forward = getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        Vec3 incoming = origin.subtract(getBoundingBox().getCenter())
                .multiply(1.0D, 0.0D, 1.0D);
        return forward.lengthSqr() > 0.001D
                && incoming.lengthSqr() > 0.001D
                && forward.normalize().dot(incoming.normalize()) < -0.30D;
    }

    private void redirectProjectile(Projectile projectile) {
        Vec3 movement = projectile.getDeltaMovement();
        if (movement.lengthSqr() < 0.001D) {
            return;
        }
        float turn = random.nextBoolean() ? 0.65F : -0.65F;
        Vec3 redirected = movement.yRot(turn).scale(0.72D).add(0.0D, 0.12D, 0.0D);
        projectile.setDeltaMovement(redirected);
        projectile.setPos(projectile.getX() + redirected.x * 0.35D,
                projectile.getY() + 0.12D,
                projectile.getZ() + redirected.z * 0.35D);
    }

    private void shellImpactVfx(DamageSource source, boolean redirected) {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        Entity direct = source.getDirectEntity();
        Vec3 point = direct == null
                ? getBoundingBox().getCenter()
                : direct.getBoundingBox().getCenter();
        for (int copy = 0; copy < 4; copy++) {
            double offset = (copy - 1.5D) * 0.12D;
            level.sendParticles(ModParticles.METHOD_QI_DARK.get(),
                    point.x + offset, point.y + random.nextDouble() * 0.18D,
                    point.z - offset,
                    redirected ? 5 : 3, 0.12D, 0.12D, 0.12D, 0.02D);
        }
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                point.x, point.y, point.z,
                redirected ? 18 : 9, 0.28D, 0.24D, 0.28D, 0.08D);
    }

    private void beginAction(byte action, int ticks) {
        if (BeastDazeControl.blocksMoves(this)) {
            return;
        }
        getNavigation().stop();
        entityData.set(ACTION_STATE, action);
        entityData.set(ACTION_TICKS, Math.max(1, ticks));
    }

    private void finishAction(byte action) {
        if (action == ACTION_TELEPORT) {
            teleportDestination = null;
        }
        entityData.set(ACTION_STATE, ACTION_NONE);
        entityData.set(ACTION_TICKS, 0);
    }

    public byte getActionState() {
        return entityData.get(ACTION_STATE);
    }

    public int getActionTicks() {
        return entityData.get(ACTION_TICKS);
    }

    public boolean isEnraged() {
        return entityData.get(ENRAGED);
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return boneParts;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    private void updateBoneHitboxes() {
        BoneHitboxAnimation.Clip animation = currentHitboxAnimation();
        float time = animation.normalizedTime(currentHitboxAnimationTime());
        ModelBoneHitboxPose root = ModelBoneHitboxPose.root(
                position(), getYRot(), 0.0F, MODEL_SCALE)
                .animatedChild(animation.motion("root"), time,
                        0.0D, 0.0D, 0.0D, 0.0F, 0.0F, 0.0F);
        ModelBoneHitboxPose body = root.animatedChild(animation.motion("body"), time,
                0.0D, 6.0D, 0.0D, 0.0F, 0.0F, 0.0F);
        ModelBoneHitboxPose head = body.animatedChild(animation.motion("head"), time,
                0.0D, -1.0D, -7.0D, 0.0F, 0.0F, 0.0F);
        ModelBoneHitboxPose crystal = body.animatedChild(
                animation.motion("crystal_cluster"), time,
                0.0D, 3.0D, 1.0D, 0.0F, 0.0F, 0.0F);
        ModelBoneHitboxPose leftShell = body.animatedChild(
                animation.motion("left_shell"), time,
                -3.5D, 1.0D, 1.0D, 0.0F, 0.0F, 0.0F);
        ModelBoneHitboxPose rightShell = body.animatedChild(
                animation.motion("right_shell"), time,
                3.5D, 1.0D, 1.0D, 0.0F, 0.0F, 0.0F);
        ModelBoneHitboxPose frontLeft = body.animatedChild(
                animation.motion("front_left_leg"), time,
                -5.5D, -2.0D, -4.0D, 0.0F, 0.0F, 0.0F);
        ModelBoneHitboxPose frontRight = body.animatedChild(
                animation.motion("front_right_leg"), time,
                5.5D, -2.0D, -4.0D, 0.0F, 0.0F, 0.0F);
        ModelBoneHitboxPose midLeft = body.animatedChild(
                animation.motion("mid_left_leg"), time,
                -6.0D, -2.0D, 1.0D, 0.0F, 0.0F, 0.0F);
        ModelBoneHitboxPose midRight = body.animatedChild(
                animation.motion("mid_right_leg"), time,
                6.0D, -2.0D, 1.0D, 0.0F, 0.0F, 0.0F);
        ModelBoneHitboxPose backLeft = body.animatedChild(
                animation.motion("back_left_leg"), time,
                -5.5D, -2.0D, 6.0D, 0.0F, 0.0F, 0.0F);
        ModelBoneHitboxPose backRight = body.animatedChild(
                animation.motion("back_right_leg"), time,
                5.5D, -2.0D, 6.0D, 0.0F, 0.0F, 0.0F);

        bodyPart.moveCenterTo(body.point(0.0D, -1.0D, 1.0D));
        headPart.moveCenterTo(head.point(0.0D, 0.0D, -2.0D));
        crystalPart.moveCenterTo(crystal.point(0.0D, 2.2D, 0.0D));
        leftShellPart.moveCenterTo(leftShell.point(0.0D, 0.0D, 0.0D));
        rightShellPart.moveCenterTo(rightShell.point(0.0D, 0.0D, 0.0D));
        frontLeftLegPart.moveCenterTo(frontLeft.point(-2.0D, -1.0D, 0.0D));
        frontRightLegPart.moveCenterTo(frontRight.point(2.0D, -1.0D, 0.0D));
        midLeftLegPart.moveCenterTo(midLeft.point(-2.0D, -1.0D, 0.0D));
        midRightLegPart.moveCenterTo(midRight.point(2.0D, -1.0D, 0.0D));
        backLeftLegPart.moveCenterTo(backLeft.point(-2.0D, -1.0D, 0.0D));
        backRightLegPart.moveCenterTo(backRight.point(2.0D, -1.0D, 0.0D));
    }

    private BoneHitboxAnimation.Clip currentHitboxAnimation() {
        return switch (getActionState()) {
            case ACTION_RIFT -> RIFT_HITBOX;
            case ACTION_CRYSTAL_RAIN -> CRYSTAL_HITBOX;
            case ACTION_TELEPORT -> TELEPORT_HITBOX;
            default -> getDeltaMovement().horizontalDistanceSqr() > 0.002D
                    ? WALK_HITBOX : IDLE_HITBOX;
        };
    }

    private float currentHitboxAnimationTime() {
        int duration = switch (getActionState()) {
            case ACTION_BITE -> BITE_TICKS;
            case ACTION_RIFT -> RIFT_TICKS;
            case ACTION_CRYSTAL_RAIN -> CRYSTAL_RAIN_TICKS;
            case ACTION_TELEPORT -> TELEPORT_TICKS;
            default -> -1;
        };
        return duration < 0
                ? tickCount / 20.0F
                : Math.max(0, duration - getActionTicks()) / 20.0F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("RiftCooldown", riftCooldown);
        tag.putInt("CrystalRainCooldown", crystalRainCooldown);
        tag.putInt("TeleportCooldown", teleportCooldown);
        tag.putBoolean("Enraged", isEnraged());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        riftCooldown = Math.max(0, tag.getInt("RiftCooldown"));
        crystalRainCooldown = Math.max(0, tag.getInt("CrystalRainCooldown"));
        teleportCooldown = Math.max(0, tag.getInt("TeleportCooldown"));
        entityData.set(ENRAGED, tag.getBoolean("Enraged"));
        var movement = getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) {
            movement.setBaseValue(isEnraged() ? ENRAGED_MOVE_SPEED : BASE_MOVE_SPEED);
        }
        entityData.set(ACTION_STATE, ACTION_NONE);
        entityData.set(ACTION_TICKS, 0);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> {
            if (isDeadOrDying()) {
                return state.setAndContinue(DEATH_ANIM);
            }
            return switch (getActionState()) {
                case ACTION_BITE -> state.setAndContinue(BITE_ANIM);
                case ACTION_RIFT -> state.setAndContinue(RIFT_ANIM);
                case ACTION_CRYSTAL_RAIN -> state.setAndContinue(CRYSTAL_RAIN_ANIM);
                case ACTION_TELEPORT -> state.setAndContinue(TELEPORT_ANIM);
                default -> state.isMoving()
                        ? state.setAndContinue(WALK_ANIM)
                        : state.setAndContinue(IDLE_ANIM);
            };
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
