package com.example.immortal_cultivation_mod.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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

public class CrimsonScaleFlameLizardEntity extends RealmBeastMonster implements GeoEntity {
    // Visuals use synchronized BeastAttackVfx; server combat and sounds remain unchanged.
    public static final float MODEL_SCALE = 1.18F;
    public static final float RENDERER_YAW_OFFSET = -90.0F;

    private static final EntityDataAccessor<Byte> ACTION_STATE =
            SynchedEntityData.defineId(CrimsonScaleFlameLizardEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ACTION_TICKS =
            SynchedEntityData.defineId(CrimsonScaleFlameLizardEntity.class, EntityDataSerializers.INT);

    private static final byte ACTION_NONE = 0;
    private static final byte ACTION_BITE = 1;
    private static final byte ACTION_BREATH = 2;
    private static final byte ACTION_TAIL_SWEEP = 3;
    private static final byte ACTION_FIREBALL = 4;

    private static final int BREATH_ACTION_TICKS = 34;
    private static final int TAIL_SWEEP_ACTION_TICKS = 26;
    private static final int FIREBALL_ACTION_TICKS = 24;
    private static final int BITE_ACTION_TICKS = 12;

    private static final int BREATH_COOLDOWN_TICKS = 100;
    private static final int TAIL_SWEEP_COOLDOWN_TICKS = 80;
    private static final int FIREBALL_COOLDOWN_TICKS = 80;

    private static final float BREATH_PULSE_DAMAGE = 55.0F;
    private static final float TAIL_SWEEP_DAMAGE = 200.0F;
    private static final float HEATED_SCALE_DAMAGE = 40.0F;
    private static final double BREATH_RANGE = 6.5D;
    private static final double TAIL_SWEEP_RANGE = 3.35D;

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin()
            .thenLoop("animation.immortal_cultivation_mod.crimson_scale_flame_lizard.idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin()
            .thenLoop("animation.immortal_cultivation_mod.crimson_scale_flame_lizard.walk");
    private static final RawAnimation BITE_ANIM = RawAnimation.begin()
            .thenPlay("animation.immortal_cultivation_mod.crimson_scale_flame_lizard.bite");
    private static final RawAnimation BREATH_ANIM = RawAnimation.begin()
            .thenPlay("animation.immortal_cultivation_mod.crimson_scale_flame_lizard.breath");
    private static final RawAnimation TAIL_SWEEP_ANIM = RawAnimation.begin()
            .thenPlay("animation.immortal_cultivation_mod.crimson_scale_flame_lizard.tail_sweep");
    private static final RawAnimation FIREBALL_ANIM = RawAnimation.begin()
            .thenPlay("animation.immortal_cultivation_mod.crimson_scale_flame_lizard.core_blast");
    private static final RawAnimation DEATH_ANIM = RawAnimation.begin()
            .thenPlayAndHold("animation.immortal_cultivation_mod.crimson_scale_flame_lizard.death");

    private static final BoneHitboxAnimation.Clip IDLE_HITBOX_ANIMATION = clip(2.4F, true,
            positionAndRotation("body", ZERO, ZERO,
                    curve(0.0F, -0.8F, 1.2F, 0.8F, 2.4F, -0.8F),
                    ZERO, curve(0.0F, 0.0F, 1.2F, 0.35F, 2.4F, 0.0F), ZERO),
            rotation("head", ZERO, ZERO,
                    curve(0.0F, 1.5F, 1.2F, -2.5F, 2.4F, 1.5F)),
            rotation("jaw", ZERO, ZERO,
                    curve(0.0F, 0.0F, 1.2F, 3.0F, 2.4F, 0.0F)),
            rotation("tail_base", ZERO,
                    curve(0.0F, -7.0F, 1.2F, 7.0F, 2.4F, -7.0F),
                    curve(0.0F, 3.0F, 2.4F, 3.0F)),
            rotation("tail_mid", ZERO,
                    curve(0.0F, -11.0F, 1.2F, 11.0F, 2.4F, -11.0F),
                    curve(0.0F, 4.0F, 2.4F, 4.0F)),
            rotation("tail_tip", ZERO,
                    curve(0.0F, -17.0F, 1.2F, 17.0F, 2.4F, -17.0F),
                    curve(0.0F, 5.0F, 2.4F, 5.0F)));

    private static final BoneHitboxAnimation.Clip WALK_HITBOX_ANIMATION = clip(0.8F, true,
            positionAndRotation("body", ZERO, ZERO, ZERO,
                    ZERO, curve(0.0F, 0.0F, 0.2F, 0.45F, 0.4F, 0.0F,
                            0.6F, 0.45F, 0.8F, 0.0F), ZERO),
            rotation("tail_base", ZERO,
                    curve(0.0F, -10.0F, 0.4F, 10.0F, 0.8F, -10.0F),
                    curve(0.0F, 3.0F, 0.8F, 3.0F)),
            rotation("tail_mid", ZERO,
                    curve(0.0F, -16.0F, 0.4F, 16.0F, 0.8F, -16.0F),
                    curve(0.0F, 4.0F, 0.8F, 4.0F)),
            rotation("tail_tip", ZERO,
                    curve(0.0F, -24.0F, 0.4F, 24.0F, 0.8F, -24.0F),
                    curve(0.0F, 5.0F, 0.8F, 5.0F)),
            rotation("front_left_leg", ZERO, ZERO,
                    curve(0.0F, 25.0F, 0.4F, -25.0F, 0.8F, 25.0F)),
            rotation("front_right_leg", ZERO, ZERO,
                    curve(0.0F, -25.0F, 0.4F, 25.0F, 0.8F, -25.0F)),
            rotation("back_left_leg", ZERO, ZERO,
                    curve(0.0F, -22.0F, 0.4F, 22.0F, 0.8F, -22.0F)),
            rotation("back_right_leg", ZERO, ZERO,
                    curve(0.0F, 22.0F, 0.4F, -22.0F, 0.8F, 22.0F)));

    private static final BoneHitboxAnimation.Clip BITE_HITBOX_ANIMATION = clip(0.6F, false,
            rotation("neck", ZERO, ZERO,
                    curve(0.0F, 8.0F, 0.2F, -22.0F, 0.6F, 0.0F)),
            rotation("head", ZERO, ZERO,
                    curve(0.0F, -5.0F, 0.2F, 15.0F, 0.6F, 0.0F)),
            rotation("jaw", ZERO, ZERO,
                    curve(0.0F, 26.0F, 0.22F, 0.0F, 0.6F, 0.0F)));

    private static final BoneHitboxAnimation.Clip BREATH_HITBOX_ANIMATION = clip(1.7F, false,
            rotation("body", ZERO, ZERO,
                    curve(0.0F, 0.0F, 0.35F, 5.0F, 1.35F, 5.0F, 1.7F, 0.0F)),
            rotation("neck", ZERO, ZERO,
                    curve(0.0F, 0.0F, 0.35F, -16.0F, 1.35F, -16.0F, 1.7F, 0.0F)),
            rotation("head", ZERO, ZERO,
                    curve(0.0F, 0.0F, 0.35F, 11.0F, 1.35F, 11.0F, 1.7F, 0.0F)),
            rotation("jaw", ZERO, ZERO,
                    curve(0.0F, 0.0F, 0.3F, 33.0F, 1.4F, 33.0F, 1.7F, 0.0F)));

    private static final BoneHitboxAnimation.Clip TAIL_SWEEP_HITBOX_ANIMATION = clip(1.3F, false,
            rotation("root", ZERO,
                    curve(0.0F, 0.0F, 0.2F, -35.0F, 0.65F, 180.0F,
                            1.05F, 360.0F, 1.3F, 360.0F), ZERO),
            rotation("tail_base", ZERO,
                    curve(0.0F, -20.0F, 0.35F, 55.0F, 0.8F, -50.0F, 1.3F, 0.0F),
                    curve(0.0F, 3.0F, 1.3F, 3.0F)),
            rotation("tail_mid", ZERO,
                    curve(0.0F, -35.0F, 0.35F, 70.0F, 0.8F, -70.0F, 1.3F, 0.0F),
                    curve(0.0F, 4.0F, 1.3F, 4.0F)),
            rotation("tail_tip", ZERO,
                    curve(0.0F, -50.0F, 0.35F, 90.0F, 0.8F, -90.0F, 1.3F, 0.0F),
                    curve(0.0F, 5.0F, 1.3F, 5.0F)));

    private static final BoneHitboxAnimation.Clip FIREBALL_HITBOX_ANIMATION = clip(1.2F, false,
            rotation("body", ZERO, ZERO,
                    curve(0.0F, 0.0F, 0.4F, -7.0F, 0.75F, 8.0F, 1.2F, 0.0F)),
            rotation("neck", ZERO, ZERO,
                    curve(0.0F, 0.0F, 0.4F, 18.0F, 0.75F, -24.0F, 1.2F, 0.0F)),
            rotation("head", ZERO, ZERO,
                    curve(0.0F, 0.0F, 0.4F, -12.0F, 0.75F, 18.0F, 1.2F, 0.0F)),
            rotation("jaw", ZERO, ZERO,
                    curve(0.0F, 0.0F, 0.4F, 30.0F, 0.8F, 30.0F, 1.2F, 0.0F)));

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ModelBonePartEntity<CrimsonScaleFlameLizardEntity> bodyPart;
    private final ModelBonePartEntity<CrimsonScaleFlameLizardEntity> neckPart;
    private final ModelBonePartEntity<CrimsonScaleFlameLizardEntity> headPart;
    private final ModelBonePartEntity<CrimsonScaleFlameLizardEntity> jawPart;
    private final ModelBonePartEntity<CrimsonScaleFlameLizardEntity> tailBasePart;
    private final ModelBonePartEntity<CrimsonScaleFlameLizardEntity> tailMidPart;
    private final ModelBonePartEntity<CrimsonScaleFlameLizardEntity> tailTipPart;
    private final ModelBonePartEntity<CrimsonScaleFlameLizardEntity> frontLeftLegPart;
    private final ModelBonePartEntity<CrimsonScaleFlameLizardEntity> frontRightLegPart;
    private final ModelBonePartEntity<CrimsonScaleFlameLizardEntity> backLeftLegPart;
    private final ModelBonePartEntity<CrimsonScaleFlameLizardEntity> backRightLegPart;
    private final ModelBonePartEntity<?>[] boneParts;
    private int breathCooldown;
    private int tailSweepCooldown;
    private int fireballCooldown;
    private DamageSource lastExplosionDamageSource;
    private long lastExplosionDamageTick = Long.MIN_VALUE;

    public CrimsonScaleFlameLizardEntity(EntityType<? extends CrimsonScaleFlameLizardEntity> entityType,
                                         Level level) {
        super(entityType, level);
        bodyPart = new ModelBonePartEntity<>(this, "body", 1.45F, 0.9F);
        neckPart = new ModelBonePartEntity<>(this, "neck", 0.68F, 0.56F);
        headPart = new ModelBonePartEntity<>(this, "head", 0.76F, 0.58F);
        jawPart = new ModelBonePartEntity<>(this, "jaw", 0.58F, 0.32F);
        tailBasePart = new ModelBonePartEntity<>(this, "tail_base", 0.64F, 0.48F);
        tailMidPart = new ModelBonePartEntity<>(this, "tail_mid", 0.48F, 0.35F);
        tailTipPart = new ModelBonePartEntity<>(this, "tail_tip", 0.38F, 0.27F);
        frontLeftLegPart = new ModelBonePartEntity<>(this, "front_left_leg", 0.43F, 0.54F);
        frontRightLegPart = new ModelBonePartEntity<>(this, "front_right_leg", 0.43F, 0.54F);
        backLeftLegPart = new ModelBonePartEntity<>(this, "back_left_leg", 0.43F, 0.54F);
        backRightLegPart = new ModelBonePartEntity<>(this, "back_right_leg", 0.43F, 0.54F);
        boneParts = new ModelBonePartEntity<?>[]{
                bodyPart, neckPart, headPart, jawPart,
                tailBasePart, tailMidPart, tailTipPart,
                frontLeftLegPart, frontRightLegPart, backLeftLegPart, backRightLegPart
        };
        setId(ENTITY_COUNTER.getAndAdd(boneParts.length + 1) + 1);
        noCulling = true;
        xpReward = 350;
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
                .add(Attributes.MAX_HEALTH, 2000.0D)
                .add(Attributes.ATTACK_DAMAGE, 200.0D)
                .add(Attributes.ARMOR, 55.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.65D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.08D, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.82D));
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
    }

    @Override
    public void tick() {
        super.tick();
        updateBoneHitboxes();
        if (!level().isClientSide || !isAlive()
                || getHealth() > getMaxHealth() * 0.40F
                || tickCount % 3 != 0) {
            return;
        }
        double x = getX() + (random.nextDouble() - 0.5D) * getBbWidth() * 1.2D;
        double y = getY() + 0.2D + random.nextDouble() * getBbHeight() * 0.9D;
        double z = getZ() + (random.nextDouble() - 0.5D) * getBbWidth() * 1.2D;

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
        ModelBoneHitboxPose entityRoot = ModelBoneHitboxPose.root(
                position(), getYRot(), RENDERER_YAW_OFFSET, MODEL_SCALE);
        ModelBoneHitboxPose root = animatedBone(
                entityRoot, animation, "root", time,
                0.0D, 0.0D, 0.0D, 0.0F, 0.0F, 0.0F);
        ModelBoneHitboxPose body = animatedBone(
                root, animation, "body", time,
                0.0D, 9.0D, 0.0D, 0.0F, 0.0F, 0.0F);
        ModelBoneHitboxPose neck = animatedBone(
                body, animation, "neck", time,
                8.0D, 0.0D, 0.0D, 0.0F, 0.0F, 0.0F);
        ModelBoneHitboxPose head = animatedBone(
                neck, animation, "head", time,
                4.0D, 0.0D, 0.0D, 0.0F, 0.0F, 0.0F);
        ModelBoneHitboxPose jaw = animatedBone(
                head, animation, "jaw", time,
                5.0D, -2.0D, 0.0D, 0.0F, 0.0F, 0.0F);

        ModelBoneHitboxPose tailBase = animatedBone(
                body, animation, "tail_base", time,
                -8.0D, 0.0D, 0.0D, 0.0F, 0.0F, 3.0F);
        ModelBoneHitboxPose tailMid = animatedBone(
                tailBase, animation, "tail_mid", time,
                -6.0D, 0.0D, 0.0D, 0.0F, 0.0F, 4.0F);
        ModelBoneHitboxPose tailTip = animatedBone(
                tailMid, animation, "tail_tip", time,
                -4.0D, 0.0D, 0.0D, 0.0F, 0.0F, 5.0F);

        ModelBoneHitboxPose frontLeftLeg = animatedBone(
                body, animation, "front_left_leg", time,
                5.0D, -3.0D, -4.0D, 0.0F, 0.0F, 0.0F);
        ModelBoneHitboxPose frontRightLeg = animatedBone(
                body, animation, "front_right_leg", time,
                5.0D, -3.0D, 4.0D, 0.0F, 0.0F, 0.0F);
        ModelBoneHitboxPose backLeftLeg = animatedBone(
                body, animation, "back_left_leg", time,
                -5.0D, -3.0D, -4.0D, 0.0F, 0.0F, 0.0F);
        ModelBoneHitboxPose backRightLeg = animatedBone(
                body, animation, "back_right_leg", time,
                -5.0D, -3.0D, 4.0D, 0.0F, 0.0F, 0.0F);

        bodyPart.moveCenterTo(body.point(0.0D, 0.5D, 0.0D));
        neckPart.moveCenterTo(neck.point(2.0D, 0.0D, 0.0D));
        headPart.moveCenterTo(head.point(2.5D, 0.0D, 0.0D));
        jawPart.moveCenterTo(jaw.point(1.0D, -1.0D, 0.0D));
        tailBasePart.moveCenterTo(tailBase.point(-2.5D, 0.0D, 0.0D));
        tailMidPart.moveCenterTo(tailMid.point(-1.5D, 0.0D, 0.0D));
        tailTipPart.moveCenterTo(tailTip.point(-2.5D, 0.0D, 0.0D));
        frontLeftLegPart.moveCenterTo(frontLeftLeg.point(0.0D, -2.5D, -0.75D));
        frontRightLegPart.moveCenterTo(frontRightLeg.point(0.0D, -2.5D, 0.75D));
        backLeftLegPart.moveCenterTo(backLeftLeg.point(0.0D, -2.5D, -0.75D));
        backRightLegPart.moveCenterTo(backRightLeg.point(0.0D, -2.5D, 0.75D));
    }

    private BoneHitboxAnimation.Clip currentHitboxAnimation() {
        return switch (getActionState()) {
            case ACTION_BITE -> BITE_HITBOX_ANIMATION;
            case ACTION_BREATH -> BREATH_HITBOX_ANIMATION;
            case ACTION_TAIL_SWEEP -> TAIL_SWEEP_HITBOX_ANIMATION;
            case ACTION_FIREBALL -> FIREBALL_HITBOX_ANIMATION;
            default -> getDeltaMovement().horizontalDistanceSqr() > 0.0025D
                    ? WALK_HITBOX_ANIMATION
                    : IDLE_HITBOX_ANIMATION;
        };
    }

    private float currentHitboxAnimationTime() {
        int duration = switch (getActionState()) {
            case ACTION_BITE -> BITE_ACTION_TICKS;
            case ACTION_BREATH -> BREATH_ACTION_TICKS;
            case ACTION_TAIL_SWEEP -> TAIL_SWEEP_ACTION_TICKS;
            case ACTION_FIREBALL -> FIREBALL_ACTION_TICKS;
            default -> -1;
        };
        return duration < 0
                ? tickCount / 20.0F
                : Math.max(0, duration - getActionTicks()) / 20.0F;
    }

    private static ModelBoneHitboxPose animatedBone(
            ModelBoneHitboxPose parent,
            BoneHitboxAnimation.Clip animation,
            String bone,
            float time,
            double pivotX, double pivotY, double pivotZ,
            float baseRotationX, float baseRotationY, float baseRotationZ) {
        BoneHitboxAnimation.Motion motion = animation.motion(bone);
        return parent.child(
                pivotX + motion.positionX().sample(time),
                pivotY + motion.positionY().sample(time),
                pivotZ + motion.positionZ().sample(time),
                baseRotationX + motion.rotationX().sample(time),
                baseRotationY + motion.rotationY().sample(time),
                baseRotationZ + motion.rotationZ().sample(time),
                motion.scaleX().sample(time),
                motion.scaleY().sample(time),
                motion.scaleZ().sample(time));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || !isAlive()) {
            return;
        }
        if (BeastDazeControl.blocksMoves(this)) {
            cancelActionForDaze();
            return;
        }

        if (breathCooldown > 0) {
            breathCooldown--;
        }
        if (tailSweepCooldown > 0) {
            tailSweepCooldown--;
        }
        if (fireballCooldown > 0) {
            fireballCooldown--;
        }

        if (getActionState() != ACTION_NONE) {
            tickCurrentAction();
            return;
        }

        LivingEntity target = getTarget();
        if (!canUseAbilityOn(target)) {
            return;
        }

        double distanceSquared = distanceToSqr(target);
        if (getHealth() <= getMaxHealth() * 0.15F
                && fireballCooldown <= 0
                && distanceSquared <= 20.0D * 20.0D
                && hasLineOfSight(target)
                && random.nextInt(80) == 0) {
            beginAction(ACTION_FIREBALL, FIREBALL_ACTION_TICKS);
            fireballCooldown = FIREBALL_COOLDOWN_TICKS;
            return;
        }

        if (tailSweepCooldown <= 0
                && distanceSquared <= TAIL_SWEEP_RANGE * TAIL_SWEEP_RANGE) {
            beginAction(ACTION_TAIL_SWEEP, TAIL_SWEEP_ACTION_TICKS);
            tailSweepCooldown = TAIL_SWEEP_COOLDOWN_TICKS;
            return;
        }

        if (breathCooldown <= 0
                && distanceSquared >= 2.5D * 2.5D
                && distanceSquared <= BREATH_RANGE * BREATH_RANGE
                && hasLineOfSight(target)) {
            beginAction(ACTION_BREATH, BREATH_ACTION_TICKS);
            breathCooldown = BREATH_COOLDOWN_TICKS;
        }
    }

    private void tickCurrentAction() {
        int ticks = getActionTicks();
        byte action = getActionState();
        LivingEntity target = getTarget();

        getNavigation().stop();
        Vec3 motion = getDeltaMovement();
        setDeltaMovement(motion.x * 0.25D, motion.y, motion.z * 0.25D);
        if (canUseAbilityOn(target)) {
            lookAt(target, 35.0F, 35.0F);
        }

        if (action == ACTION_BREATH && level() instanceof ServerLevel serverLevel) {
            if (ticks <= 27 && ticks >= 7) {
                emitBreathParticles(serverLevel);
            }
            if (ticks == 24 || ticks == 16 || ticks == 8) {
                damageBreathCone(serverLevel);
            }
        } else if (action == ACTION_TAIL_SWEEP && ticks == 13
                && level() instanceof ServerLevel serverLevel) {
            performTailSweep(serverLevel);
        } else if (action == ACTION_FIREBALL && ticks == 9
                && level() instanceof ServerLevel serverLevel
                && canUseAbilityOn(target)) {
            launchDemonCoreFireball(serverLevel, target);
        }

        if (ticks <= 1) {
            entityData.set(ACTION_STATE, ACTION_NONE);
            entityData.set(ACTION_TICKS, 0);
        } else {
            entityData.set(ACTION_TICKS, ticks - 1);
        }
    }

    private void damageBreathCone(ServerLevel serverLevel) {
        Vec3 origin = breathOrigin();
        Vec3 forward = getLookAngle().normalize();
        AABB area = getBoundingBox().inflate(BREATH_RANGE, 3.0D, BREATH_RANGE);

        for (LivingEntity target : serverLevel.getEntitiesOfClass(
                LivingEntity.class, area, this::canUseAbilityOn)) {
            Vec3 targetPoint = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
            Vec3 toTarget = targetPoint.subtract(origin);
            double distance = toTarget.length();
            if (distance <= 0.001D || distance > BREATH_RANGE) {
                continue;
            }
            if (forward.dot(toTarget.scale(1.0D / distance)) < 0.82D || !hasLineOfSight(target)) {
                continue;
            }
            target.hurt(damageSources().mobAttack(this), BREATH_PULSE_DAMAGE);
            target.igniteForSeconds(5.0F);
        }
    }

    private void emitBreathParticles(ServerLevel serverLevel) {
        Vec3 origin = breathOrigin();
        Vec3 forward = getLookAngle().normalize();
        for (int step = 1; step <= 9; step++) {
            double distance = step * 0.68D;
            double spread = 0.045D + step * 0.055D;
            Vec3 point = origin.add(forward.scale(distance));

        }
        if (getActionTicks() == 27) {
            serverLevel.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT,
                    SoundSource.HOSTILE, 1.2F, 0.68F);
        }
    }

    private void performTailSweep(ServerLevel serverLevel) {
        AABB area = getBoundingBox().inflate(TAIL_SWEEP_RANGE, 1.5D, TAIL_SWEEP_RANGE);
        for (LivingEntity target : serverLevel.getEntitiesOfClass(
                LivingEntity.class, area, this::canUseAbilityOn)) {
            if (distanceToSqr(target) > TAIL_SWEEP_RANGE * TAIL_SWEEP_RANGE) {
                continue;
            }
            target.hurt(damageSources().mobAttack(this), TAIL_SWEEP_DAMAGE);
            Vec3 away = target.position().subtract(position());
            target.knockback(1.8D, -away.x, -away.z);
        }
        BeastMoveBlockBreaker.breakBox(serverLevel, this,
                new AABB(getX() - TAIL_SWEEP_RANGE, getY(),
                        getZ() - TAIL_SWEEP_RANGE,
                        getX() + TAIL_SWEEP_RANGE,
                        getY() + Math.min(2.0D, getBbHeight() + 0.5D),
                        getZ() + TAIL_SWEEP_RANGE),
                getBoundingBox().getCenter(), 28, 4.0F);
        serverLevel.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.HOSTILE, 1.4F, 0.55F);
    }

    private void launchDemonCoreFireball(ServerLevel serverLevel, LivingEntity target) {
        Vec3 forward = getLookAngle().normalize();
        Vec3 origin = breathOrigin().add(forward.scale(0.65D));
        Vec3 targetPoint = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
        Vec3 direction = targetPoint.subtract(origin);

        CrimsonFlameFireballEntity fireball = new CrimsonFlameFireballEntity(serverLevel, this);
        fireball.setPos(origin.x, origin.y, origin.z);
        fireball.shoot(direction.x, direction.y, direction.z, 1.05F, 1.5F);
        serverLevel.addFreshEntity(fireball);

        serverLevel.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT,
                SoundSource.HOSTILE, 1.5F, 0.52F);
    }

    private Vec3 breathOrigin() {
        return position()
                .add(0.0D, getBbHeight() * 0.72D, 0.0D)
                .add(getLookAngle().normalize().scale(0.85D));
    }

    private boolean canUseAbilityOn(LivingEntity target) {
        return target != null
                && target != this
                && target.isAlive()
                && !(target instanceof CrimsonScaleFlameLizardEntity)
                && !isAlliedTo(target);
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        if (BeastDazeControl.blocksMoves(this) || getActionState() != ACTION_NONE) {
            return false;
        }
        boolean hurt = super.doHurtTarget(entity);
        if (hurt) {
            beginAction(ACTION_BITE, BITE_ACTION_TICKS);
        }
        return hurt;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            long gameTime = level().getGameTime();
            if (lastExplosionDamageSource == source && lastExplosionDamageTick == gameTime) {
                return false;
            }
            lastExplosionDamageSource = source;
            lastExplosionDamageTick = gameTime;
        }
        boolean hurt = super.hurt(source, amount);
        if (!hurt || level().isClientSide || !isAlive()
                || getHealth() > getMaxHealth() * 0.40F) {
            return hurt;
        }

        Entity attackerEntity = source.getEntity();
        if (attackerEntity instanceof LivingEntity attacker
                && source.getDirectEntity() == attackerEntity
                && !(attacker instanceof CrimsonScaleFlameLizardEntity)
                && distanceToSqr(attacker) <= 4.5D * 4.5D) {
            attacker.hurt(damageSources().mobAttack(this), HEATED_SCALE_DAMAGE);
            attacker.igniteForSeconds(3.0F);

        }
        return true;
    }

    private void beginAction(byte action, int ticks) {
        if (BeastDazeControl.blocksMoves(this)) {
            return;
        }
        entityData.set(ACTION_STATE, action);
        entityData.set(ACTION_TICKS, Math.max(1, ticks));
    }

    private void cancelActionForDaze() {
        entityData.set(ACTION_STATE, ACTION_NONE);
        entityData.set(ACTION_TICKS, 0);
    }

    public byte getActionState() {
        return entityData.get(ACTION_STATE);
    }

    public int getActionTicks() {
        return entityData.get(ACTION_TICKS);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("BreathCooldown", breathCooldown);
        tag.putInt("TailSweepCooldown", tailSweepCooldown);
        tag.putInt("FireballCooldown", fireballCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        breathCooldown = Math.max(0, tag.getInt("BreathCooldown"));
        tailSweepCooldown = Math.max(0, tag.getInt("TailSweepCooldown"));
        fireballCooldown = Math.max(0, tag.getInt("FireballCooldown"));
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
                case ACTION_BREATH -> state.setAndContinue(BREATH_ANIM);
                case ACTION_TAIL_SWEEP -> state.setAndContinue(TAIL_SWEEP_ANIM);
                case ACTION_FIREBALL -> state.setAndContinue(FIREBALL_ANIM);
                default -> state.isMoving()
                        ? state.setAndContinue(WALK_ANIM)
                        : state.setAndContinue(IDLE_ANIM);
            };
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
