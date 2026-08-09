package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.magicritual.center.CenterEffectSupport;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.ForcefieldCenterBehavior;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import com.example.immortal_cultivation_mod.spell.JindanCreativeFlight;
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
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
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

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Yuanying middle-stage fire boss that lives around deep lava lakes. */
public final class CrimsonSkyFlameFloodDragonEntity extends Monster implements GeoEntity {
    /** Must stay shared with the renderer so model-space bone positions and hitboxes cannot drift. */
    public static final float MODEL_SCALE = 3.0F;
    private static final double MODEL_UNIT = MODEL_SCALE / 16.0D;

    private static final EntityDataAccessor<Byte> ACTION_STATE = SynchedEntityData.defineId(
            CrimsonSkyFlameFloodDragonEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ACTION_TICKS = SynchedEntityData.defineId(
            CrimsonSkyFlameFloodDragonEntity.class, EntityDataSerializers.INT);

    private static final byte ACTION_NONE = 0;
    private static final byte ACTION_BREATH = 1;
    private static final byte ACTION_TAIL_SWEEP = 2;
    private static final byte ACTION_SCALE_BURST = 3;
    private static final byte ACTION_FIRE_RUSH = 4;
    private static final byte ACTION_FIRE_DOMAIN = 5;

    // These durations match the supplied GeckoLib animations.
    private static final int BREATH_ACTION_TICKS = 52;
    private static final int TAIL_SWEEP_ACTION_TICKS = 30;
    private static final int SCALE_BURST_ACTION_TICKS = 29;
    private static final int FIRE_RUSH_ACTION_TICKS = 22;
    private static final int FIRE_DOMAIN_ACTION_TICKS = 160;

    private static final double BREATH_RANGE = 14.0D;
    private static final double TAIL_SWEEP_RANGE = 6.0D;
    private static final double FIRE_DOMAIN_RANGE = 12.0D;
    private static final double FLIGHT_SPEED =
            JindanCreativeFlight.BASE_TRAVEL_SPEED * 3.0D;
    private static final double FIRE_RUSH_SPEED = FLIGHT_SPEED * 1.25D;
    private static final float BREATH_DAMAGE = 340.0F;
    private static final float TAIL_SWEEP_DAMAGE = 800.0F;
    private static final float FIRE_RUSH_DAMAGE = 650.0F;
    private static final float FIRE_DOMAIN_DAMAGE = 220.0F;
    private static final int BREATH_BLOCK_BREAK_LIMIT = 36;
    private static final int TAIL_SWEEP_BLOCK_BREAK_LIMIT = 96;
    private static final int FIRE_RUSH_BLOCK_BREAK_LIMIT = 24;

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin()
            .thenLoop("animation.crimson_sky_flame_flood_dragon.idle");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin()
            .thenLoop("animation.crimson_sky_flame_flood_dragon.walk");
    private static final RawAnimation BREATH_ANIMATION = RawAnimation.begin()
            .thenPlay("animation.crimson_sky_flame_flood_dragon.yan_jiao_tu_xi");
    private static final RawAnimation TAIL_SWEEP_ANIMATION = RawAnimation.begin()
            .thenPlay("animation.crimson_sky_flame_flood_dragon.chi_yan_shuai_wei");
    private static final RawAnimation SCALE_BURST_ANIMATION = RawAnimation.begin()
            .thenPlay("animation.crimson_sky_flame_flood_dragon.yan_lin_bao");
    private static final RawAnimation FIRE_RUSH_ANIMATION = RawAnimation.begin()
            .thenPlay("animation.crimson_sky_flame_flood_dragon.jiao_huo_dun");
    private static final RawAnimation FIRE_DOMAIN_ANIMATION = RawAnimation.begin()
            .thenLoop("animation.crimson_sky_flame_flood_dragon.fen_tian_nu");

    private static final Curve ZERO_CURVE = curve(0.0F, 0.0F);
    private static final BoneMotion NO_MOTION = new BoneMotion(
            ZERO_CURVE, ZERO_CURVE, ZERO_CURVE, ZERO_CURVE);

    /*
     * Server-side mirrors of the supplied GeckoLib keyframes. GeckoLib's evaluated
     * bones only exist in the client renderer, so authoritative combat hitboxes
     * must evaluate the same curves here instead of trusting client positions.
     */
    private static final SkeletonAnimation IDLE_SKELETON = animation(3.0F, true,
            bone(ModelBone.BODY_CORE,
                    curve(0.0F, 0.0F, 1.5F, 1.5F, 3.0F, 0.0F), ZERO_CURVE,
                    curve(0.0F, 0.0F, 1.5F, 0.4F, 3.0F, 0.0F), ZERO_CURVE),
            bone(ModelBone.NECK_1,
                    curve(0.0F, -4.0F, 1.5F, -1.0F, 3.0F, -4.0F),
                    curve(0.0F, -3.0F, 1.5F, 3.0F, 3.0F, -3.0F)),
            bone(ModelBone.NECK_2,
                    curve(0.0F, -7.0F, 1.5F, -4.0F, 3.0F, -7.0F),
                    curve(0.0F, 4.0F, 1.5F, -4.0F, 3.0F, 4.0F)),
            bone(ModelBone.HEAD,
                    curve(0.0F, 4.0F, 1.5F, 6.0F, 3.0F, 4.0F),
                    curve(0.0F, -5.0F, 1.5F, 5.0F, 3.0F, -5.0F)),
            bone(ModelBone.JAW,
                    curve(0.0F, 2.0F, 1.5F, 6.0F, 3.0F, 2.0F), ZERO_CURVE),
            yaw(ModelBone.TAIL_1, 0.0F, 5.0F, 1.5F, -6.0F, 3.0F, 5.0F),
            yaw(ModelBone.TAIL_2, 0.0F, 8.0F, 1.5F, -10.0F, 3.0F, 8.0F),
            yaw(ModelBone.TAIL_3, 0.0F, 12.0F, 1.5F, -14.0F, 3.0F, 12.0F),
            yaw(ModelBone.TAIL_4, 0.0F, 16.0F, 1.5F, -18.0F, 3.0F, 16.0F),
            yaw(ModelBone.TAIL_TIP, 0.0F, 22.0F, 1.5F, -24.0F, 3.0F, 22.0F));

    private static final SkeletonAnimation WALK_SKELETON = animation(1.5F, true,
            bone(ModelBone.BODY_CORE, ZERO_CURVE,
                    curve(0.0F, 1.5F, 0.75F, -1.5F, 1.5F, 1.5F),
                    curve(0.0F, 0.0F, 0.375F, 0.25F, 0.75F, 0.0F,
                            1.125F, 0.25F, 1.5F, 0.0F), ZERO_CURVE),
            bone(ModelBone.NECK_1,
                    curve(0.0F, -4.0F, 0.75F, -2.0F, 1.5F, -4.0F),
                    curve(0.0F, 2.0F, 0.75F, -2.0F, 1.5F, 2.0F)),
            bone(ModelBone.NECK_2,
                    curve(0.0F, -7.0F, 0.75F, -4.0F, 1.5F, -7.0F),
                    curve(0.0F, 4.0F, 0.75F, -4.0F, 1.5F, 4.0F)),
            bone(ModelBone.HEAD,
                    curve(0.0F, 5.0F, 0.75F, 3.0F, 1.5F, 5.0F),
                    curve(0.0F, 4.0F, 0.75F, -4.0F, 1.5F, 4.0F)),
            yaw(ModelBone.TAIL_1, 0.0F, 8.0F, 0.75F, -8.0F, 1.5F, 8.0F),
            yaw(ModelBone.TAIL_2, 0.0F, 12.0F, 0.75F, -12.0F, 1.5F, 12.0F),
            yaw(ModelBone.TAIL_3, 0.0F, 16.0F, 0.75F, -16.0F, 1.5F, 16.0F),
            yaw(ModelBone.TAIL_4, 0.0F, 20.0F, 0.75F, -20.0F, 1.5F, 20.0F),
            yaw(ModelBone.TAIL_TIP, 0.0F, 28.0F, 0.75F, -28.0F, 1.5F, 28.0F));

    private static final SkeletonAnimation BREATH_SKELETON = animation(2.6F, false,
            bone(ModelBone.BODY_CORE,
                    curve(0.0F, 0.0F, 0.5F, 6.0F, 1.35F, 4.0F,
                            2.15F, 4.0F, 2.6F, 0.0F),
                    curve(0.0F, 0.0F, 0.5F, -10.0F, 1.35F, 14.0F,
                            2.15F, -12.0F, 2.6F, 0.0F),
                    curve(0.0F, 0.0F, 0.5F, -1.2F, 2.15F, -1.2F, 2.6F, 0.0F),
                    curve(0.0F, 0.0F, 0.5F, -0.5F, 2.15F, -0.5F, 2.6F, 0.0F)),
            bone(ModelBone.NECK_1,
                    curve(0.0F, -4.0F, 0.5F, 10.0F, 1.35F, 12.0F,
                            2.15F, 12.0F, 2.6F, -4.0F),
                    curve(0.0F, 0.0F, 0.5F, -26.0F, 1.35F, 22.0F,
                            2.15F, -20.0F, 2.6F, 0.0F)),
            bone(ModelBone.NECK_2,
                    curve(0.0F, -7.0F, 0.5F, 18.0F, 1.35F, 20.0F,
                            2.15F, 20.0F, 2.6F, -7.0F),
                    curve(0.0F, 0.0F, 0.5F, -18.0F, 1.35F, 18.0F,
                            2.15F, -16.0F, 2.6F, 0.0F)),
            bone(ModelBone.HEAD,
                    curve(0.0F, 4.0F, 0.5F, 24.0F, 1.35F, 22.0F,
                            2.15F, 22.0F, 2.6F, 4.0F),
                    curve(0.0F, 0.0F, 0.5F, -8.0F, 1.35F, 16.0F,
                            2.15F, -14.0F, 2.6F, 0.0F)),
            pitch(ModelBone.JAW, 0.0F, 0.0F, 0.35F, 20.0F, 0.6F, 34.0F,
                    2.2F, 34.0F, 2.6F, 0.0F),
            yaw(ModelBone.TAIL_1, 0.0F, 0.0F, 0.5F, 8.0F, 1.35F, -12.0F,
                    2.15F, 10.0F, 2.6F, 0.0F),
            yaw(ModelBone.TAIL_2, 0.0F, 0.0F, 0.5F, 14.0F, 1.35F, -18.0F,
                    2.15F, 15.0F, 2.6F, 0.0F));

    private static final SkeletonAnimation TAIL_SWEEP_SKELETON = animation(1.5F, false,
            yaw(ModelBone.BODY_CORE, 0.0F, 0.0F, 0.35F, -24.0F,
                    0.75F, 20.0F, 1.05F, -18.0F, 1.5F, 0.0F),
            bone(ModelBone.HEAD,
                    curve(0.0F, 4.0F, 0.35F, 2.0F, 0.75F, 2.0F, 1.5F, 4.0F),
                    curve(0.0F, 0.0F, 0.35F, 18.0F, 0.75F, -14.0F, 1.5F, 0.0F)),
            yaw(ModelBone.TAIL_1, 0.0F, 8.0F, 0.35F, 40.0F,
                    0.75F, -80.0F, 1.05F, 55.0F, 1.5F, 8.0F),
            yaw(ModelBone.TAIL_2, 0.0F, 12.0F, 0.35F, 55.0F,
                    0.75F, -110.0F, 1.05F, 78.0F, 1.5F, 12.0F),
            yaw(ModelBone.TAIL_3, 0.0F, 16.0F, 0.35F, 72.0F,
                    0.75F, -138.0F, 1.05F, 96.0F, 1.5F, 16.0F),
            yaw(ModelBone.TAIL_4, 0.0F, 20.0F, 0.35F, 88.0F,
                    0.75F, -164.0F, 1.05F, 112.0F, 1.5F, 20.0F),
            yaw(ModelBone.TAIL_TIP, 0.0F, 26.0F, 0.35F, 110.0F,
                    0.75F, -180.0F, 1.05F, 136.0F, 1.5F, 26.0F));

    private static final SkeletonAnimation SCALE_BURST_SKELETON = animation(1.45F, false,
            pitch(ModelBone.BODY_CORE, 0.0F, 0.0F, 0.4F, -6.0F,
                    0.65F, -10.0F, 0.9F, 8.0F, 1.45F, 0.0F),
            pitch(ModelBone.NECK_1, 0.0F, -4.0F, 0.4F, -10.0F,
                    0.65F, -16.0F, 0.9F, 10.0F, 1.45F, -4.0F),
            pitch(ModelBone.HEAD, 0.0F, 4.0F, 0.4F, -8.0F,
                    0.65F, -14.0F, 0.9F, 12.0F, 1.45F, 4.0F),
            yaw(ModelBone.TAIL_1, 0.0F, 0.0F, 0.65F, 20.0F,
                    0.9F, -10.0F, 1.45F, 0.0F),
            yaw(ModelBone.TAIL_2, 0.0F, 0.0F, 0.65F, -20.0F,
                    0.9F, 10.0F, 1.45F, 0.0F));

    private static final SkeletonAnimation FIRE_RUSH_SKELETON = animation(1.1F, false,
            bone(ModelBone.BODY_CORE,
                    curve(0.0F, 0.0F, 0.25F, -8.0F, 0.55F, -14.0F,
                            0.8F, -6.0F, 1.1F, 0.0F), ZERO_CURVE,
                    curve(0.0F, 0.0F, 0.25F, -0.4F, 0.55F, -0.4F,
                            0.8F, -0.1F, 1.1F, 0.0F),
                    curve(0.0F, 0.0F, 0.25F, -1.5F, 0.55F, -4.0F,
                            0.8F, -5.0F, 1.1F, 0.0F)),
            pitch(ModelBone.NECK_1, 0.0F, -4.0F, 0.25F, 4.0F,
                    0.55F, 10.0F, 1.1F, -4.0F),
            pitch(ModelBone.NECK_2, 0.0F, -7.0F, 0.25F, 8.0F,
                    0.55F, 14.0F, 1.1F, -7.0F),
            pitch(ModelBone.HEAD, 0.0F, 4.0F, 0.25F, 12.0F,
                    0.55F, 18.0F, 1.1F, 4.0F),
            pitch(ModelBone.JAW, 0.0F, 0.0F, 0.25F, 10.0F,
                    0.8F, 10.0F, 1.1F, 0.0F),
            yaw(ModelBone.TAIL_1, 0.0F, 0.0F, 0.25F, -6.0F,
                    0.55F, -2.0F, 1.1F, 0.0F),
            yaw(ModelBone.TAIL_2, 0.0F, 0.0F, 0.25F, -4.0F,
                    0.55F, -1.0F, 1.1F, 0.0F),
            yaw(ModelBone.TAIL_3, 0.0F, 0.0F, 0.25F, -2.0F,
                    0.55F, 0.0F, 1.1F, 0.0F),
            yaw(ModelBone.TAIL_4, 0.0F, 0.0F, 0.25F, 1.0F,
                    0.55F, 2.0F, 1.1F, 0.0F),
            yaw(ModelBone.TAIL_TIP, 0.0F, 0.0F, 0.25F, 4.0F,
                    0.55F, 6.0F, 1.1F, 0.0F));

    private static final SkeletonAnimation FIRE_DOMAIN_SKELETON = animation(2.4F, true,
            bone(ModelBone.BODY_CORE,
                    curve(0.0F, 0.0F, 0.6F, -4.0F, 1.2F, 0.0F,
                            1.8F, -4.0F, 2.4F, 0.0F), ZERO_CURVE,
                    curve(0.0F, 0.0F, 0.6F, 0.35F, 1.2F, 0.0F,
                            1.8F, 0.35F, 2.4F, 0.0F), ZERO_CURVE),
            pitch(ModelBone.NECK_1, 0.0F, -4.0F, 0.3F, 8.0F,
                    1.2F, 12.0F, 2.4F, -4.0F),
            pitch(ModelBone.NECK_2, 0.0F, -7.0F, 0.3F, 16.0F,
                    1.2F, 22.0F, 2.4F, -7.0F),
            pitch(ModelBone.HEAD, 0.0F, 4.0F, 0.3F, 24.0F,
                    1.2F, 30.0F, 2.4F, 4.0F),
            pitch(ModelBone.JAW, 0.0F, 8.0F, 0.3F, 24.0F,
                    1.2F, 30.0F, 2.4F, 8.0F),
            yaw(ModelBone.TAIL_1, 0.0F, 8.0F, 1.2F, -8.0F, 2.4F, 8.0F),
            yaw(ModelBone.TAIL_2, 0.0F, 12.0F, 1.2F, -12.0F, 2.4F, 12.0F),
            yaw(ModelBone.TAIL_3, 0.0F, 16.0F, 1.2F, -16.0F, 2.4F, 16.0F),
            yaw(ModelBone.TAIL_4, 0.0F, 20.0F, 1.2F, -20.0F, 2.4F, 20.0F),
            yaw(ModelBone.TAIL_TIP, 0.0F, 24.0F, 1.2F, -24.0F, 2.4F, 24.0F));

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final Set<Integer> rushHitEntities = new HashSet<>();
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> bodyFront;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> bodyRear;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> neck1;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> neck2;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> headPart;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> jawPart;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> tail1Front;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> tail1Rear;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> tail2Front;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> tail2Rear;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> tail3Front;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> tail3Rear;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> tail4Front;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> tail4Rear;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> tailTipFront;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> tailTipRear;
    private final ModelBonePartEntity<?>[] boneParts;
    private int breathCooldown;
    private int tailSweepCooldown;
    private int scaleBurstCooldown;
    private int fireRushCooldown;
    private int fireDomainCooldown;
    private boolean coreReleased;
    private boolean revivedFromCore;
    private DamageSource lastExplosionDamageSource;
    private long lastExplosionDamageTick = Long.MIN_VALUE;

    public CrimsonSkyFlameFloodDragonEntity(
            EntityType<? extends CrimsonSkyFlameFloodDragonEntity> entityType, Level level) {
        super(entityType, level);
        bodyFront = new ModelBonePartEntity<>(this, "body_core_front", 3.6F, 2.7F);
        bodyRear = new ModelBonePartEntity<>(this, "body_core_rear", 3.6F, 2.7F);
        neck1 = new ModelBonePartEntity<>(this, "neck_1", 2.5F, 2.25F);
        neck2 = new ModelBonePartEntity<>(this, "neck_2", 2.3F, 1.95F);
        headPart = new ModelBonePartEntity<>(this, "head", 2.6F, 2.4F);
        jawPart = new ModelBonePartEntity<>(this, "jaw", 2.0F, 1.5F);
        tail1Front = new ModelBonePartEntity<>(this, "tail_1_front", 2.45F, 2.0F);
        tail1Rear = new ModelBonePartEntity<>(this, "tail_1_rear", 2.35F, 1.95F);
        tail2Front = new ModelBonePartEntity<>(this, "tail_2_front", 2.1F, 1.7F);
        tail2Rear = new ModelBonePartEntity<>(this, "tail_2_rear", 2.0F, 1.65F);
        tail3Front = new ModelBonePartEntity<>(this, "tail_3_front", 1.75F, 1.5F);
        tail3Rear = new ModelBonePartEntity<>(this, "tail_3_rear", 1.65F, 1.45F);
        tail4Front = new ModelBonePartEntity<>(this, "tail_4_front", 1.4F, 1.25F);
        tail4Rear = new ModelBonePartEntity<>(this, "tail_4_rear", 1.3F, 1.15F);
        tailTipFront = new ModelBonePartEntity<>(this, "tail_tip_front", 1.1F, 0.9F);
        tailTipRear = new ModelBonePartEntity<>(this, "tail_tip_rear", 0.9F, 0.8F);
        boneParts = new ModelBonePartEntity<?>[]{
                bodyFront, bodyRear, neck1, neck2, headPart, jawPart,
                tail1Front, tail1Rear, tail2Front, tail2Rear,
                tail3Front, tail3Rear, tail4Front, tail4Rear,
                tailTipFront, tailTipRear
        };

        // Multipart IDs must directly follow the parent's ID on both logical sides.
        setId(ENTITY_COUNTER.getAndAdd(boneParts.length + 1) + 1);
        moveControl = new FlyingMoveControl(this, 30, true);
        setNoGravity(true);
        noCulling = true;
        xpReward = 2_000;
        setPersistenceRequired();
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
                .add(Attributes.MAX_HEALTH, 18_000.0D)
                .add(Attributes.ARMOR, 400.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 40.0D)
                .add(Attributes.ATTACK_DAMAGE, 650.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.FLYING_SPEED, FLIGHT_SPEED)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.STEP_HEIGHT, 1.5D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(false);
        return navigation;
    }

    public static boolean checkDragonSpawnRules(
            EntityType<CrimsonSkyFlameFloodDragonEntity> type,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL
                || pos.getY() > 40
                || !Mob.checkMobSpawnRules(type, level, spawnType, pos, random)) {
            return false;
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -7; x <= 7; x++) {
            for (int z = -7; z <= 7; z++) {
                for (int y = -4; y <= 4; y++) {
                    cursor.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                    if (level.getFluidState(cursor).is(FluidTags.LAVA)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new DragonFlightAttackGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 0.65D));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this, LivingEntity.class, true,
                target -> target instanceof Player || target instanceof CultivationNpc));
    }

    /** Keeps combat movement airborne and a few blocks above grounded targets. */
    private static final class DragonFlightAttackGoal extends Goal {
        private final CrimsonSkyFlameFloodDragonEntity dragon;
        private int repathTicks;
        private int attackTicks;

        private DragonFlightAttackGoal(CrimsonSkyFlameFloodDragonEntity dragon) {
            this.dragon = dragon;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return isValidTarget(dragon.getTarget());
        }

        @Override
        public boolean canContinueToUse() {
            return isValidTarget(dragon.getTarget());
        }

        @Override
        public void start() {
            dragon.setAggressive(true);
            repathTicks = 0;
            attackTicks = 0;
        }

        @Override
        public void stop() {
            dragon.setAggressive(false);
            dragon.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = dragon.getTarget();
            if (!isValidTarget(target)) {
                return;
            }
            dragon.getLookControl().setLookAt(target, 35.0F, 45.0F);
            if (dragon.getActionState() != ACTION_NONE) {
                dragon.getNavigation().stop();
                repathTicks = 0;
                return;
            }

            if (--repathTicks <= 0) {
                double destinationY = target.onGround()
                        ? target.getY() + 3.0D
                        : target.getY() + target.getBbHeight() * 0.5D;
                dragon.getNavigation().moveTo(
                        target.getX(), destinationY, target.getZ(), 1.0D);
                repathTicks = 4 + dragon.getRandom().nextInt(4);
            }

            attackTicks = Math.max(0, attackTicks - 1);
            if (attackTicks <= 0
                    && dragon.isWithinMeleeAttackRange(target)
                    && dragon.hasLineOfSight(target)) {
                dragon.swing(InteractionHand.MAIN_HAND);
                dragon.doHurtTarget(target);
                attackTicks = 20;
            }
        }

        private static boolean isValidTarget(LivingEntity target) {
            return target != null
                    && target.isAlive()
                    && (!(target instanceof Player player)
                            || (!player.isCreative() && !player.isSpectator()));
        }
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
        if (isAlive()) {
            if (!isNoGravity()) {
                setNoGravity(true);
            }
            fallDistance = 0.0F;
            resetFallDistance();
        }
        if (level().isClientSide && isAlive() && tickCount % 2 == 0) {
            boolean lowHealth = getHealth() <= getMaxHealth() * 0.5F;
            boolean flameForm = getActionState() == ACTION_FIRE_RUSH;
            if (lowHealth || flameForm) {
                int count = flameForm ? 5 : 1;
                for (int i = 0; i < count; i++) {
                    double x = getX() + (random.nextDouble() - 0.5D) * getBbWidth();
                    double y = getY() + random.nextDouble() * getBbHeight();
                    double z = getZ() + (random.nextDouble() - 0.5D) * getBbWidth();
                    level().addParticle(
                            i % 4 == 0
                                    ? ParticleTypes.LAVA
                                    : ModParticles.DRAGON_EMBER.get(),
                            x, y, z,
                            (random.nextDouble() - 0.5D) * 0.025D,
                            0.018D + random.nextDouble() * 0.025D,
                            (random.nextDouble() - 0.5D) * 0.025D);
                }
            }
        }
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return boneParts;
    }

    /** The core box remains for navigation and physical collision; attacks select bone parts. */
    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Explosions query every multipart segment. Count one blast once, not once per bone.
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            long gameTime = level().getGameTime();
            if (lastExplosionDamageSource == source && lastExplosionDamageTick == gameTime) {
                return false;
            }
            lastExplosionDamageSource = source;
            lastExplosionDamageTick = gameTime;
        }
        return super.hurt(source, amount);
    }

    private void updateBoneHitboxes() {
        SkeletonAnimation animation = currentSkeletonAnimation();
        float time = animation.normalizedTime(currentSkeletonTime());

        BoneMotion bodyMotion = animation.motion(ModelBone.BODY_CORE);
        BonePose root = new BonePose(position(), 0.0F, 0.0F, getYRot());
        BonePose body = root.child(
                0.0D,
                13.0D + bodyMotion.positionY().sample(time),
                2.0D + bodyMotion.positionZ().sample(time),
                anglePitch(animation, ModelBone.BODY_CORE, time),
                angleYaw(animation, ModelBone.BODY_CORE, time));

        BonePose neckOne = body.child(0.0D, 2.0D, -10.0D,
                anglePitch(animation, ModelBone.NECK_1, time),
                angleYaw(animation, ModelBone.NECK_1, time));
        BonePose neckTwo = neckOne.child(0.0D, 1.0D, -12.0D,
                anglePitch(animation, ModelBone.NECK_2, time),
                angleYaw(animation, ModelBone.NECK_2, time));
        BonePose head = neckTwo.child(0.0D, 1.0D, -13.0D,
                anglePitch(animation, ModelBone.HEAD, time),
                angleYaw(animation, ModelBone.HEAD, time));
        BonePose jaw = head.child(0.0D, -4.5D, -14.0D,
                anglePitch(animation, ModelBone.JAW, time),
                angleYaw(animation, ModelBone.JAW, time));

        BonePose tailOne = body.child(0.0D, 0.0D, 9.0D,
                anglePitch(animation, ModelBone.TAIL_1, time),
                angleYaw(animation, ModelBone.TAIL_1, time));
        BonePose tailTwo = tailOne.child(0.0D, 0.0D, 16.0D,
                anglePitch(animation, ModelBone.TAIL_2, time),
                angleYaw(animation, ModelBone.TAIL_2, time));
        BonePose tailThree = tailTwo.child(0.0D, 0.0D, 17.0D,
                anglePitch(animation, ModelBone.TAIL_3, time),
                angleYaw(animation, ModelBone.TAIL_3, time));
        BonePose tailFour = tailThree.child(0.0D, 0.0D, 17.0D,
                anglePitch(animation, ModelBone.TAIL_4, time),
                angleYaw(animation, ModelBone.TAIL_4, time));
        BonePose tailTip = tailFour.child(0.0D, 0.0D, 16.0D,
                anglePitch(animation, ModelBone.TAIL_TIP, time),
                angleYaw(animation, ModelBone.TAIL_TIP, time));

        // Two samples on long bones form a close capsule-like chain of AABBs.
        bodyFront.moveCenterTo(body.point(0.0D, 1.0D, -6.0D));
        bodyRear.moveCenterTo(body.point(0.0D, 1.0D, 6.0D));
        neck1.moveCenterTo(neckOne.point(0.0D, 0.0D, -6.0D));
        neck2.moveCenterTo(neckTwo.point(0.0D, 0.0D, -7.0D));
        headPart.moveCenterTo(head.point(0.0D, 0.0D, -7.0D));
        jawPart.moveCenterTo(jaw.point(0.0D, -1.0D, -5.5D));

        tail1Front.moveCenterTo(tailOne.point(0.0D, 0.0D, 5.0D));
        tail1Rear.moveCenterTo(tailOne.point(0.0D, 0.0D, 14.0D));
        tail2Front.moveCenterTo(tailTwo.point(0.0D, 0.0D, 5.0D));
        tail2Rear.moveCenterTo(tailTwo.point(0.0D, 0.0D, 13.0D));
        tail3Front.moveCenterTo(tailThree.point(0.0D, 0.0D, 4.0D));
        tail3Rear.moveCenterTo(tailThree.point(0.0D, 0.0D, 12.0D));
        tail4Front.moveCenterTo(tailFour.point(0.0D, 0.0D, 4.0D));
        tail4Rear.moveCenterTo(tailFour.point(0.0D, 0.0D, 12.0D));
        tailTipFront.moveCenterTo(tailTip.point(0.0D, 0.0D, 4.0D));
        tailTipRear.moveCenterTo(tailTip.point(0.0D, 0.0D, 11.5D));
    }

    private SkeletonAnimation currentSkeletonAnimation() {
        return switch (getActionState()) {
            case ACTION_BREATH -> BREATH_SKELETON;
            case ACTION_TAIL_SWEEP -> TAIL_SWEEP_SKELETON;
            case ACTION_SCALE_BURST -> SCALE_BURST_SKELETON;
            case ACTION_FIRE_RUSH -> FIRE_RUSH_SKELETON;
            case ACTION_FIRE_DOMAIN -> FIRE_DOMAIN_SKELETON;
            default -> getDeltaMovement().horizontalDistanceSqr() > 0.0025D
                    ? WALK_SKELETON
                    : IDLE_SKELETON;
        };
    }

    private float currentSkeletonTime() {
        int duration = switch (getActionState()) {
            case ACTION_BREATH -> BREATH_ACTION_TICKS;
            case ACTION_TAIL_SWEEP -> TAIL_SWEEP_ACTION_TICKS;
            case ACTION_SCALE_BURST -> SCALE_BURST_ACTION_TICKS;
            case ACTION_FIRE_RUSH -> FIRE_RUSH_ACTION_TICKS;
            case ACTION_FIRE_DOMAIN -> FIRE_DOMAIN_ACTION_TICKS;
            default -> -1;
        };
        return duration < 0
                ? tickCount / 20.0F
                : Math.max(0, duration - getActionTicks()) / 20.0F;
    }

    private static float anglePitch(SkeletonAnimation animation, ModelBone bone, float time) {
        // GeckoLib converts Bedrock/Blockbench X and Y rotations to their negatives.
        return -(basePitch(bone) + animation.motion(bone).pitch().sample(time));
    }

    private static float angleYaw(SkeletonAnimation animation, ModelBone bone, float time) {
        return -(baseYaw(bone) + animation.motion(bone).yaw().sample(time));
    }

    private static float basePitch(ModelBone bone) {
        return switch (bone) {
            case NECK_1 -> -4.0F;
            case NECK_2 -> -7.0F;
            case HEAD -> 4.0F;
            case TAIL_1 -> 2.0F;
            case TAIL_2 -> 1.0F;
            case TAIL_4 -> -1.0F;
            case TAIL_TIP -> -2.0F;
            default -> 0.0F;
        };
    }

    private static float baseYaw(ModelBone bone) {
        return switch (bone) {
            case TAIL_1 -> 4.0F;
            case TAIL_2 -> -7.0F;
            case TAIL_3 -> 9.0F;
            case TAIL_4 -> -11.0F;
            case TAIL_TIP -> 8.0F;
            default -> 0.0F;
        };
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || !isAlive()) {
            return;
        }

        breathCooldown = decrement(breathCooldown);
        tailSweepCooldown = decrement(tailSweepCooldown);
        scaleBurstCooldown = decrement(scaleBurstCooldown);
        fireRushCooldown = decrement(fireRushCooldown);
        fireDomainCooldown = decrement(fireDomainCooldown);

        if (getActionState() != ACTION_NONE) {
            tickCurrentAction();
            return;
        }

        LivingEntity target = getTarget();
        if (!canAttackWithAbility(target) || tickCount % 5 != 0) {
            return;
        }

        double distanceSquared = distanceToSqr(target);
        if (getHealth() <= getMaxHealth() * 0.20F && fireDomainCooldown <= 0) {
            beginAction(ACTION_FIRE_DOMAIN, FIRE_DOMAIN_ACTION_TICKS);
            fireDomainCooldown = 20 * 18;
            return;
        }
        if (getHealth() <= getMaxHealth() * 0.50F
                && scaleBurstCooldown <= 0
                && distanceSquared <= 16.0D * 16.0D
                && random.nextFloat() < 0.35F) {
            beginAction(ACTION_SCALE_BURST, SCALE_BURST_ACTION_TICKS);
            scaleBurstCooldown = 20 * 8;
            return;
        }
        if (tailSweepCooldown <= 0
                && distanceSquared <= TAIL_SWEEP_RANGE * TAIL_SWEEP_RANGE) {
            beginAction(ACTION_TAIL_SWEEP, TAIL_SWEEP_ACTION_TICKS);
            tailSweepCooldown = 20 * 5;
            return;
        }
        if (breathCooldown <= 0
                && distanceSquared >= 4.0D * 4.0D
                && distanceSquared <= BREATH_RANGE * BREATH_RANGE
                && hasLineOfSight(target)) {
            beginAction(ACTION_BREATH, BREATH_ACTION_TICKS);
            breathCooldown = 20 * 7;
            return;
        }
        if (fireRushCooldown <= 0
                && distanceSquared >= 6.0D * 6.0D
                && distanceSquared <= 28.0D * 28.0D
                && hasLineOfSight(target)) {
            beginAction(ACTION_FIRE_RUSH, FIRE_RUSH_ACTION_TICKS);
            fireRushCooldown = 20 * 6;
        }
    }

    private static int decrement(int value) {
        return value > 0 ? value - 1 : 0;
    }

    private void tickCurrentAction() {
        int ticks = getActionTicks();
        byte action = getActionState();
        LivingEntity target = getTarget();
        getNavigation().stop();

        if (canAttackWithAbility(target) && action != ACTION_TAIL_SWEEP
                && action != ACTION_SCALE_BURST && action != ACTION_FIRE_DOMAIN) {
            lookAt(target, action == ACTION_FIRE_RUSH ? 60.0F : 30.0F, 40.0F);
        }

        if (level() instanceof ServerLevel serverLevel) {
            switch (action) {
                case ACTION_BREATH -> tickBreath(serverLevel, ticks);
                case ACTION_TAIL_SWEEP -> tickTailSweep(serverLevel, ticks);
                case ACTION_SCALE_BURST -> tickScaleBurst(serverLevel, ticks);
                case ACTION_FIRE_RUSH -> tickFireRush(serverLevel, ticks, target);
                case ACTION_FIRE_DOMAIN -> tickFireDomain(serverLevel, ticks);
                default -> {
                }
            }
        }

        if (action != ACTION_FIRE_RUSH) {
            Vec3 motion = getDeltaMovement();
            setDeltaMovement(motion.x * 0.18D, motion.y, motion.z * 0.18D);
        }

        if (ticks <= 1) {
            finishAction();
        } else {
            entityData.set(ACTION_TICKS, ticks - 1);
        }
    }

    private void tickBreath(ServerLevel level, int ticks) {
        if (ticks == BREATH_ACTION_TICKS) {
            emitFireSigil(level, position().add(0.0D, 0.08D, 0.0D),
                    2.8D, 0.12D, 28);
        }
        if (ticks > 45) {
            Vec3 charge = breathOrigin(horizontalDirection(getYRot()));
            level.sendParticles(ModParticles.DRAGON_EMBER.get(),
                    charge.x, charge.y, charge.z,
                    8, 0.35D, 0.28D, 0.35D, 0.035D);
        }
        if (ticks <= 45 && ticks >= 7) {
            double sweep = Math.sin((BREATH_ACTION_TICKS - ticks) * 0.22D) * 27.0D;
            Vec3 forward = horizontalDirection(getYRot() + (float) sweep);
            emitBreathParticles(level, forward);
            if (ticks == 42 || ticks == 32 || ticks == 22 || ticks == 12) {
                damageBreathCone(level, forward);
                destroyBreathPath(level, forward);
                igniteBreathPath(level, forward);
            }
        }
        if (ticks == 45) {
            level.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL,
                    SoundSource.HOSTILE, 2.5F, 0.7F);
        }
    }

    private void damageBreathCone(ServerLevel level, Vec3 forward) {
        Vec3 origin = breathOrigin(forward);
        AABB area = getBoundingBox().inflate(BREATH_RANGE, 5.0D, BREATH_RANGE);
        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class, area, this::canAttackWithAbility)) {
            Vec3 targetPoint = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            Vec3 toTarget = targetPoint.subtract(origin);
            double distance = toTarget.length();
            if (distance < 0.01D || distance > BREATH_RANGE
                    || forward.dot(toTarget.scale(1.0D / distance)) < 0.73D
                    || !hasLineOfSight(target)) {
                continue;
            }
            if (ForcefieldCenterBehavior.interceptAttackPath(
                    this, origin, targetPoint, BREATH_DAMAGE)) {
                continue;
            }
            target.hurt(damageSources().mobAttack(this), BREATH_DAMAGE);
            target.igniteForSeconds(8.0F);
        }
    }

    private void emitBreathParticles(ServerLevel level, Vec3 forward) {
        Vec3 origin = breathOrigin(forward);
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        for (int step = 1; step <= 16; step++) {
            double distance = step * 0.84D;
            double width = 0.08D + step * 0.075D;
            double spiral = tickCount * 0.48D + step * 0.92D;
            Vec3 point = origin.add(forward.scale(distance))
                    .add(side.scale(Math.cos(spiral) * width))
                    .add(up.scale(Math.sin(spiral) * width * 0.55D));
            if (CenterEffectSupport.isForcefieldRouteBlocked(this, origin, point)) {
                break;
            }
            Vec3 velocity = forward.scale(0.10D + step * 0.003D)
                    .add(side.scale(-Math.sin(spiral) * 0.035D))
                    .add(up.scale(Math.cos(spiral) * 0.018D));
            sendExactParticle(level, ModParticles.DRAGON_EMBER.get(), point, velocity);
            if (step % 4 == 0) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, point.x, point.y, point.z,
                        1, width * 0.45D, width * 0.25D, width * 0.45D, 0.01D);
            }
        }
    }

    private void igniteBreathPath(ServerLevel level, Vec3 forward) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
        for (int step = 2; step <= 14; step++) {
            double width = step * 0.17D;
            for (int lane = -1; lane <= 1; lane++) {
                Vec3 point = position().add(forward.scale(step))
                        .add(side.scale(lane * width));
                placeFireOnGround(level, BlockPos.containing(point), breathOrigin(forward));
            }
        }
    }

    private void placeFireOnGround(ServerLevel level, BlockPos around, Vec3 attackOrigin) {
        for (int offset = 2; offset >= -4; offset--) {
            BlockPos firePos = around.offset(0, offset, 0);
            BlockPos below = firePos.below();
            if (level.getBlockState(firePos).isAir()
                    && level.getFluidState(firePos).isEmpty()
                    && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                if (CenterEffectSupport.isForcefieldRouteBlocked(
                        this, attackOrigin, Vec3.atCenterOf(firePos))) {
                    return;
                }
                level.setBlockAndUpdate(firePos, Blocks.FIRE.defaultBlockState());
                return;
            }
        }
    }

    private void tickTailSweep(ServerLevel level, int ticks) {
        if (ticks == TAIL_SWEEP_ACTION_TICKS) {
            emitFireSigil(level, position().add(0.0D, 0.10D, 0.0D),
                    3.1D, -0.18D, TAIL_SWEEP_ACTION_TICKS);
        }
        if (ticks > 15 && ticks % 3 == 0) {
            emitTailFireArc(level, 2.2D + (TAIL_SWEEP_ACTION_TICKS - ticks) * 0.18D, 10);
        }
        if (ticks == 15) {
            AABB area = getBoundingBox().inflate(TAIL_SWEEP_RANGE, 3.0D, TAIL_SWEEP_RANGE);
            for (LivingEntity target : level.getEntitiesOfClass(
                    LivingEntity.class, area, this::canAttackWithAbility)) {
                if (distanceToSqr(target) > TAIL_SWEEP_RANGE * TAIL_SWEEP_RANGE) {
                    continue;
                }
                if (ForcefieldCenterBehavior.interceptAttackPath(
                        this, bodyCenter(), target.getBoundingBox().getCenter(),
                        TAIL_SWEEP_DAMAGE)) {
                    continue;
                }
                target.hurt(damageSources().mobAttack(this), TAIL_SWEEP_DAMAGE);
                Vec3 away = target.position().subtract(position());
                target.knockback(3.2D, -away.x, -away.z);
            }
            destroyAbilityBlocks(level, new AABB(
                    getX() - TAIL_SWEEP_RANGE, getY() - 0.25D,
                    getZ() - TAIL_SWEEP_RANGE,
                    getX() + TAIL_SWEEP_RANGE, getY() + getBbHeight() + 1.25D,
                    getZ() + TAIL_SWEEP_RANGE),
                    TAIL_SWEEP_BLOCK_BREAK_LIMIT, bodyCenter());
            emitTailFireArc(level, TAIL_SWEEP_RANGE, 32);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    getX(), getY() + 1.1D, getZ(),
                    12, 4.5D, 0.65D, 4.5D, 0.0D);
            level.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.HOSTILE, 2.0F, 0.45F);
        }
    }

    private void tickScaleBurst(ServerLevel level, int ticks) {
        if (ticks == 22) {
            emitFireSigil(level, position().add(0.0D, 0.12D, 0.0D),
                    2.7D, 0.20D, 22);
            level.sendParticles(ModParticles.DRAGON_EMBER.get(),
                    getX(), getY() + getBbHeight() * 0.55D, getZ(),
                    30, 1.7D, 1.0D, 1.7D, 0.09D);
            level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 1.2D, getZ(),
                    14, 1.7D, 1.0D, 1.7D, 0.08D);
        }
        if (ticks != 14) {
            return;
        }
        for (int ring = 0; ring < 3; ring++) {
            for (int index = 0; index < 10; index++) {
                double angle = Math.PI * 2.0D * index / 10.0D + ring * 0.19D;
                Vec3 direction = new Vec3(
                        Math.cos(angle), -0.08D + ring * 0.18D, Math.sin(angle)).normalize();
                CrimsonDragonScaleProjectileEntity scale =
                        new CrimsonDragonScaleProjectileEntity(level, this);
                Vec3 launchPoint = new Vec3(
                        getX() + direction.x * 1.4D,
                        getY() + getBbHeight() * 0.58D + direction.y,
                        getZ() + direction.z * 1.4D);
                scale.setPos(launchPoint.x, launchPoint.y, launchPoint.z);
                scale.shoot(direction.x, direction.y, direction.z, 1.15F, 2.0F);
                level.addFreshEntity(scale);
                sendExactParticle(level, ModParticles.DRAGON_SCALE_SPARK.get(),
                        launchPoint, direction.scale(0.52D));
            }
        }
        emitFireSigil(level, position().add(0.0D, 0.14D, 0.0D),
                3.7D, -0.24D, 26);
        level.sendParticles(ModParticles.DRAGON_EMBER.get(),
                getX(), getY() + getBbHeight() * 0.55D, getZ(),
                42, 2.2D, 1.2D, 2.2D, 0.14D);
        level.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 1.8F, 1.35F);
    }

    private void tickFireRush(ServerLevel level, int ticks, LivingEntity target) {
        if (ticks == FIRE_RUSH_ACTION_TICKS) {
            rushHitEntities.clear();
            level.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT,
                    SoundSource.HOSTILE, 2.0F, 0.5F);
        }
        if (ticks > 3 && canAttackWithAbility(target)) {
            Vec3 aim = target.position().add(0.0D, target.getBbHeight() * 0.35D, 0.0D)
                    .subtract(position().add(0.0D, getBbHeight() * 0.35D, 0.0D));
            if (aim.lengthSqr() > 0.01D) {
                Vec3 velocity = aim.normalize().scale(FIRE_RUSH_SPEED);
                setDeltaMovement(velocity.x,
                        Mth.clamp(velocity.y, -FLIGHT_SPEED, FLIGHT_SPEED),
                        velocity.z);
                hasImpulse = true;
            }
        }

        Vec3 motion = getDeltaMovement();
        Vec3 trailDirection = motion.lengthSqr() > 0.01D
                ? motion.normalize()
                : horizontalDirection(getYRot());
        destroyAbilityBlocks(level,
                getBoundingBox().expandTowards(trailDirection.scale(2.0D)).inflate(0.45D),
                FIRE_RUSH_BLOCK_BREAK_LIMIT, bodyCenter());
        Vec3 trailCenter = bodyCenter().subtract(trailDirection.scale(1.8D));
        level.sendParticles(ModParticles.DRAGON_EMBER.get(),
                trailCenter.x, trailCenter.y, trailCenter.z,
                16, 1.25D, 0.75D, 1.25D, 0.10D);
        if (ticks % 3 == 0) {
            emitFireSigil(level, trailCenter, 2.15D,
                    ticks % 2 == 0 ? 0.32D : -0.32D, 12);
        }
        if (ticks % 2 == 0) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                    trailCenter.x, trailCenter.y, trailCenter.z,
                    4, 0.8D, 0.45D, 0.8D, 0.035D);
        }

        AABB hitArea = getBoundingBox().inflate(1.1D);
        for (LivingEntity hit : level.getEntitiesOfClass(
                LivingEntity.class, hitArea, this::canAttackWithAbility)) {
            if (!rushHitEntities.add(hit.getId())) {
                continue;
            }
            if (ForcefieldCenterBehavior.interceptAttackPath(
                    this, bodyCenter(), hit.getBoundingBox().getCenter(),
                    FIRE_RUSH_DAMAGE)) {
                continue;
            }
            hit.hurt(damageSources().mobAttack(this), FIRE_RUSH_DAMAGE);
            hit.igniteForSeconds(7.0F);
            Vec3 away = hit.position().subtract(position());
            hit.knockback(1.8D, -away.x, -away.z);
        }
    }

    private void tickFireDomain(ServerLevel level, int ticks) {
        if (ticks == FIRE_DOMAIN_ACTION_TICKS) {
            level.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL,
                    SoundSource.HOSTILE, 3.0F, 0.45F);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    getX(), getY() + 1.0D, getZ(), 2, 0.0D, 0.0D, 0.0D, 0.0D);
            Vec3 sigilCenter = position().add(0.0D, 0.10D, 0.0D);
            emitFireSigil(level, sigilCenter, 4.3D, 0.028D, FIRE_DOMAIN_ACTION_TICKS);
            emitFireSigil(level, sigilCenter.add(0.0D, 0.025D, 0.0D),
                    2.9D, -0.052D, FIRE_DOMAIN_ACTION_TICKS);
            level.sendParticles(ModParticles.DRAGON_EMBER.get(),
                    getX(), getY() + 1.0D, getZ(),
                    64, 3.0D, 1.7D, 3.0D, 0.15D);
        }
        emitDomainRing(level, ticks);
        if (ticks % 10 != 0) {
            return;
        }
        AABB area = getBoundingBox().inflate(FIRE_DOMAIN_RANGE, 6.0D, FIRE_DOMAIN_RANGE);
        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class, area, this::canAttackWithAbility)) {
            if (distanceToSqr(target) > FIRE_DOMAIN_RANGE * FIRE_DOMAIN_RANGE) {
                continue;
            }
            if (ForcefieldCenterBehavior.interceptAttackPath(
                    this, bodyCenter(), target.getBoundingBox().getCenter(),
                    FIRE_DOMAIN_DAMAGE)) {
                continue;
            }
            target.hurt(damageSources().mobAttack(this), FIRE_DOMAIN_DAMAGE);
            target.igniteForSeconds(4.0F);
        }
        if (level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            for (int i = 0; i < 10; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double radius = 2.0D + random.nextDouble() * (FIRE_DOMAIN_RANGE - 2.0D);
                placeFireOnGround(level, BlockPos.containing(
                        getX() + Math.cos(angle) * radius,
                        getY(),
                        getZ() + Math.sin(angle) * radius), bodyCenter());
            }
        }
    }

    private void emitDomainRing(ServerLevel level, int ticks) {
        if ((ticks & 1) != 0) {
            return;
        }
        double phase = ticks * 0.09D;
        for (int index = 0; index < 24; index++) {
            double angle = Math.PI * 2.0D * index / 24.0D + phase;
            double radius = FIRE_DOMAIN_RANGE * (0.92D + 0.08D * Math.sin(phase + index));
            double x = getX() + Math.cos(angle) * radius;
            double z = getZ() + Math.sin(angle) * radius;
            if (CenterEffectSupport.isForcefieldRouteBlocked(
                    this, bodyCenter(), new Vec3(x, getY() + 0.35D, z))) {
                continue;
            }
            Vec3 point = new Vec3(x, getY() + 0.35D, z);
            Vec3 tangent = new Vec3(-Math.sin(angle), 0.08D, Math.cos(angle))
                    .scale(0.055D);
            sendExactParticle(level,
                    index % 6 == 0
                            ? ModParticles.DRAGON_SCALE_SPARK.get()
                            : ModParticles.DRAGON_EMBER.get(),
                    point,
                    tangent);
        }
        if (ticks % 8 == 0) {
            level.sendParticles(ModParticles.DRAGON_EMBER.get(),
                    getX(), getY() + 1.0D, getZ(),
                    10, FIRE_DOMAIN_RANGE * 0.42D, 0.5D,
                    FIRE_DOMAIN_RANGE * 0.42D, 0.045D);
        }
    }

    private void destroyBreathPath(ServerLevel level, Vec3 forward) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        Vec3 origin = breathOrigin(forward);
        int remaining = BREATH_BLOCK_BREAK_LIMIT;
        for (double distance = 0.75D;
             distance <= BREATH_RANGE && remaining > 0;
             distance += 0.75D) {
            Vec3 point = origin.add(forward.scale(distance));
            int broken = destroyAbilityBlocks(level,
                    new AABB(point, point).inflate(0.95D, 0.75D, 0.95D),
                    remaining, origin);
            remaining -= broken;
        }
    }

    private int destroyAbilityBlocks(ServerLevel level, AABB area,
                                     int maximumBlocks, Vec3 attackOrigin) {
        if (maximumBlocks <= 0
                || !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return 0;
        }
        int minX = Mth.floor(area.minX);
        int minY = Mth.floor(area.minY);
        int minZ = Mth.floor(area.minZ);
        int maxX = Mth.floor(area.maxX);
        int maxY = Mth.floor(area.maxY);
        int maxZ = Mth.floor(area.maxZ);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int destroyed = 0;

        for (int y = minY; y <= maxY && destroyed < maximumBlocks; y++) {
            for (int x = minX; x <= maxX && destroyed < maximumBlocks; x++) {
                for (int z = minZ; z <= maxZ && destroyed < maximumBlocks; z++) {
                    cursor.set(x, y, z);
                    if (!level.hasChunkAt(cursor)) {
                        continue;
                    }
                    BlockPos pos = cursor.immutable();
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()
                            || !state.getFluidState().isEmpty()
                            || state.is(BlockTags.DRAGON_IMMUNE)
                            || state.getDestroySpeed(level, pos) < 0.0F
                            || level.getBlockEntity(pos) != null
                            || CenterEffectSupport.isForcefieldRouteBlocked(
                                    this, attackOrigin, Vec3.atCenterOf(pos))) {
                        continue;
                    }
                    if (level.destroyBlock(pos, false, this)) {
                        destroyed++;
                    }
                }
            }
        }
        return destroyed;
    }

    private void emitTailFireArc(ServerLevel level, double radius, int points) {
        Vec3 origin = bodyCenter();
        double phase = Math.toRadians(getYRot()) + tickCount * 0.18D;
        for (int index = 0; index < points; index++) {
            double angle = Math.PI * 2.0D * index / points + phase;
            Vec3 point = new Vec3(
                    getX() + Math.cos(angle) * radius,
                    getY() + getBbHeight() * 0.42D
                            + Math.sin(angle * 2.0D) * 0.28D,
                    getZ() + Math.sin(angle) * radius);
            if (CenterEffectSupport.isForcefieldRouteBlocked(this, origin, point)) {
                continue;
            }
            Vec3 tangent = new Vec3(-Math.sin(angle), 0.035D, Math.cos(angle))
                    .scale(0.28D);
            sendExactParticle(level, ModParticles.DRAGON_SCALE_SPARK.get(), point, tangent);
        }
    }

    private static void emitFireSigil(
            ServerLevel level,
            Vec3 center,
            double radius,
            double spin,
            int lifetime) {
        // count=0 transfers these three values as exact particle parameters.
        level.sendParticles(ModParticles.DRAGON_FIRE_SIGIL.get(),
                center.x, center.y, center.z,
                0, radius, spin, lifetime, 1.0D);
    }

    private static void sendExactParticle(
            ServerLevel level,
            net.minecraft.core.particles.SimpleParticleType particle,
            Vec3 position,
            Vec3 velocity) {
        level.sendParticles(particle,
                position.x, position.y, position.z,
                0, velocity.x, velocity.y, velocity.z, 1.0D);
    }

    private Vec3 breathOrigin(Vec3 forward) {
        return position().add(0.0D, getBbHeight() * 0.78D, 0.0D)
                .add(forward.scale(2.0D));
    }

    private Vec3 bodyCenter() {
        return position().add(0.0D, getBbHeight() * 0.5D, 0.0D);
    }

    private static Vec3 horizontalDirection(float yawDegrees) {
        double radians = Math.toRadians(yawDegrees);
        return new Vec3(-Math.sin(radians), 0.0D, Math.cos(radians)).normalize();
    }

    private boolean canAttackWithAbility(LivingEntity target) {
        return target != null
                && target != this
                && target.isAlive()
                && !(target instanceof CrimsonSkyFlameFloodDragonEntity)
                && !(target instanceof CrimsonScaleFlameLizardEntity)
                && !(target instanceof EscapedYuanyingCoreEntity)
                && !isAlliedTo(target);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return getActionState() == ACTION_NONE && super.doHurtTarget(target);
    }

    @Override
    public void die(DamageSource source) {
        // LivingEntity calls die only after health has reached zero, so isAlive()
        // is already false here. Guard only against duplicate die calls.
        boolean releaseCore = !level().isClientSide && !coreReleased;
        coreReleased = true;
        super.die(source);
        if (releaseCore && level() instanceof ServerLevel serverLevel) {
            EscapedYuanyingCoreEntity core = new EscapedYuanyingCoreEntity(serverLevel, this);
            serverLevel.addFreshEntity(core);
        }
    }

    /** Marks a body rebuilt by its escaped Yuanying so its blood cannot be farmed again. */
    public void markRevivedFromCore() {
        revivedFromCore = true;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    private void beginAction(byte action, int ticks) {
        entityData.set(ACTION_STATE, action);
        entityData.set(ACTION_TICKS, Math.max(1, ticks));
        if (action == ACTION_FIRE_RUSH) {
            rushHitEntities.clear();
        }
    }

    private void finishAction() {
        entityData.set(ACTION_STATE, ACTION_NONE);
        entityData.set(ACTION_TICKS, 0);
        rushHitEntities.clear();
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
        tag.putInt("ScaleBurstCooldown", scaleBurstCooldown);
        tag.putInt("FireRushCooldown", fireRushCooldown);
        tag.putInt("FireDomainCooldown", fireDomainCooldown);
        tag.putBoolean("CoreReleased", coreReleased);
        tag.putBoolean("RevivedFromCore", revivedFromCore);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        breathCooldown = Math.max(0, tag.getInt("BreathCooldown"));
        tailSweepCooldown = Math.max(0, tag.getInt("TailSweepCooldown"));
        scaleBurstCooldown = Math.max(0, tag.getInt("ScaleBurstCooldown"));
        fireRushCooldown = Math.max(0, tag.getInt("FireRushCooldown"));
        fireDomainCooldown = Math.max(0, tag.getInt("FireDomainCooldown"));
        coreReleased = tag.getBoolean("CoreReleased");
        revivedFromCore = tag.getBoolean("RevivedFromCore");
        finishAction();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> switch (getActionState()) {
            case ACTION_BREATH -> state.setAndContinue(BREATH_ANIMATION);
            case ACTION_TAIL_SWEEP -> state.setAndContinue(TAIL_SWEEP_ANIMATION);
            case ACTION_SCALE_BURST -> state.setAndContinue(SCALE_BURST_ANIMATION);
            case ACTION_FIRE_RUSH -> state.setAndContinue(FIRE_RUSH_ANIMATION);
            case ACTION_FIRE_DOMAIN -> state.setAndContinue(FIRE_DOMAIN_ANIMATION);
            default -> state.isMoving()
                    ? state.setAndContinue(WALK_ANIMATION)
                    : state.setAndContinue(IDLE_ANIMATION);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    private static Curve curve(float... keyframes) {
        return new Curve(keyframes);
    }

    private static BoneKeyframe bone(ModelBone bone, Curve pitch, Curve yaw) {
        return new BoneKeyframe(bone,
                new BoneMotion(pitch, yaw, ZERO_CURVE, ZERO_CURVE));
    }

    private static BoneKeyframe bone(ModelBone bone, Curve pitch, Curve yaw,
                                     Curve positionY, Curve positionZ) {
        return new BoneKeyframe(bone, new BoneMotion(pitch, yaw, positionY, positionZ));
    }

    private static BoneKeyframe pitch(ModelBone bone, float... keyframes) {
        return bone(bone, curve(keyframes), ZERO_CURVE);
    }

    private static BoneKeyframe yaw(ModelBone bone, float... keyframes) {
        return bone(bone, ZERO_CURVE, curve(keyframes));
    }

    private static SkeletonAnimation animation(float duration, boolean loop,
                                               BoneKeyframe... keyframes) {
        EnumMap<ModelBone, BoneMotion> motions = new EnumMap<>(ModelBone.class);
        for (BoneKeyframe keyframe : keyframes) {
            motions.put(keyframe.bone(), keyframe.motion());
        }
        return new SkeletonAnimation(duration, loop, Map.copyOf(motions));
    }

    private enum ModelBone {
        BODY_CORE,
        NECK_1,
        NECK_2,
        HEAD,
        JAW,
        TAIL_1,
        TAIL_2,
        TAIL_3,
        TAIL_4,
        TAIL_TIP
    }

    private record Curve(float[] keyframes) {
        private Curve {
            if (keyframes.length < 2 || (keyframes.length & 1) != 0) {
                throw new IllegalArgumentException("Bone curve needs time/value pairs");
            }
        }

        float sample(float time) {
            if (time <= keyframes[0]) {
                return keyframes[1];
            }
            for (int index = 2; index < keyframes.length; index += 2) {
                float endTime = keyframes[index];
                if (time <= endTime) {
                    float startTime = keyframes[index - 2];
                    float progress = endTime == startTime
                            ? 1.0F
                            : (time - startTime) / (endTime - startTime);
                    return Mth.lerp(progress, keyframes[index - 1], keyframes[index + 1]);
                }
            }
            return keyframes[keyframes.length - 1];
        }
    }

    private record BoneMotion(Curve pitch, Curve yaw, Curve positionY, Curve positionZ) {
    }

    private record BoneKeyframe(ModelBone bone, BoneMotion motion) {
    }

    private record SkeletonAnimation(float duration, boolean loop,
                                     Map<ModelBone, BoneMotion> motions) {
        float normalizedTime(float time) {
            if (!loop) {
                return Mth.clamp(time, 0.0F, duration);
            }
            float wrapped = time % duration;
            return wrapped < 0.0F ? wrapped + duration : wrapped;
        }

        BoneMotion motion(ModelBone bone) {
            return motions.getOrDefault(bone, NO_MOTION);
        }
    }

    /** A model bone pivot plus its accumulated X/Y rotations in model space. */
    private record BonePose(Vec3 pivot, float pitch, float yaw, float entityYaw) {
        BonePose child(double modelX, double modelY, double modelZ,
                       float localPitch, float localYaw) {
            return new BonePose(point(modelX, modelY, modelZ),
                    pitch + localPitch, yaw + localYaw, entityYaw);
        }

        Vec3 point(double modelX, double modelY, double modelZ) {
            double x = modelX * MODEL_UNIT;
            double y = modelY * MODEL_UNIT;
            double z = modelZ * MODEL_UNIT;

            double pitchRadians = Math.toRadians(pitch);
            double pitchCos = Math.cos(pitchRadians);
            double pitchSin = Math.sin(pitchRadians);
            double pitchedY = y * pitchCos - z * pitchSin;
            double pitchedZ = y * pitchSin + z * pitchCos;

            double yawRadians = Math.toRadians(yaw);
            double yawCos = Math.cos(yawRadians);
            double yawSin = Math.sin(yawRadians);
            double rotatedX = x * yawCos + pitchedZ * yawSin;
            double rotatedZ = pitchedZ * yawCos - x * yawSin;

            // Gecko models face model -Z. Minecraft yaw 0 faces world +Z.
            double entityRadians = Math.toRadians(entityYaw);
            double entityCos = Math.cos(entityRadians);
            double entitySin = Math.sin(entityRadians);
            double worldX = -rotatedX * entityCos + rotatedZ * entitySin;
            double worldZ = -rotatedX * entitySin - rotatedZ * entityCos;
            return pivot.add(worldX, pitchedY, worldZ);
        }
    }
}
