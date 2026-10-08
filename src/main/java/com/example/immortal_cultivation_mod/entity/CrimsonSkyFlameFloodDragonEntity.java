package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;

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
import com.example.immortal_cultivation_mod.physics.DragonLookPose;

/** Yuanying middle-stage fire boss that lives around deep lava lakes. */
public final class CrimsonSkyFlameFloodDragonEntity extends Monster implements GeoEntity {
    /** Must stay shared with the renderer so model-space bone positions and hitboxes cannot drift. */
    public static final float MODEL_SCALE = 3.0F;
    private static final double MODEL_UNIT = MODEL_SCALE / 16.0D;
    private static final int NATURAL_SPAWN_RARITY = 100;
    private static final double NATURAL_SPAWN_EXCLUSION_RADIUS = 256.0D;
    private static final double NATURAL_SPAWN_EXCLUSION_HEIGHT = 128.0D;

    private static final EntityDataAccessor<Byte> ACTION_STATE = SynchedEntityData.defineId(
            CrimsonSkyFlameFloodDragonEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ACTION_TICKS = SynchedEntityData.defineId(
            CrimsonSkyFlameFloodDragonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> LOOK_PITCH = SynchedEntityData.defineId(
            CrimsonSkyFlameFloodDragonEntity.class, EntityDataSerializers.FLOAT);

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
     * Generated alongside the dragon model from its GeckoLib keyframes. GeckoLib's evaluated
     * bones only exist in the client renderer, so authoritative combat hitboxes
     * must evaluate the same curves here instead of trusting client positions.
     */
    private static final SkeletonAnimation IDLE_SKELETON = createIdleSkeleton();
    private static final SkeletonAnimation WALK_SKELETON = createWalkSkeleton();
    private static final SkeletonAnimation BREATH_SKELETON = createBreathSkeleton();
    private static final SkeletonAnimation TAIL_SWEEP_SKELETON = createTailSweepSkeleton();
    private static final SkeletonAnimation SCALE_BURST_SKELETON = createScaleBurstSkeleton();
    private static final SkeletonAnimation FIRE_RUSH_SKELETON = createFireRushSkeleton();
    private static final SkeletonAnimation FIRE_DOMAIN_SKELETON = createFireDomainSkeleton();

    private static SkeletonAnimation createIdleSkeleton() {
        return animation(3.0F, true,
            bone(ModelBone.BODY_CORE,
                    ZERO_CURVE,
                    ZERO_CURVE,
                    curve(0.0F, -0.0F, 0.025F, 0.00027F, 0.05F, 0.0011F, 0.075F, 0.00246F,
                            0.1F, 0.00437F, 0.125F, 0.00681F, 0.15F, 0.00979F, 0.175F, 0.01328F,
                            0.2F, 0.01729F, 0.225F, 0.0218F, 0.25F, 0.02679F, 0.275F, 0.03227F,
                            0.3F, 0.0382F, 0.325F, 0.04457F, 0.35F, 0.05137F, 0.375F, 0.05858F,
                            0.4F, 0.06617F, 0.425F, 0.07414F, 0.45F, 0.08244F, 0.475F, 0.09107F,
                            0.5F, 0.1F, 0.525F, 0.1092F, 0.55F, 0.11865F, 0.575F, 0.12833F,
                            0.6F, 0.1382F, 0.625F, 0.14824F, 0.65F, 0.15842F, 0.675F, 0.16871F,
                            0.7F, 0.17909F, 0.725F, 0.18953F, 0.75F, 0.2F, 0.775F, 0.21047F,
                            0.8F, 0.22091F, 0.825F, 0.23129F, 0.85F, 0.24158F, 0.875F, 0.25176F,
                            0.9F, 0.2618F, 0.925F, 0.27167F, 0.95F, 0.28135F, 0.975F, 0.2908F,
                            1.0F, 0.3F, 1.025F, 0.30893F, 1.05F, 0.31756F, 1.075F, 0.32586F,
                            1.1F, 0.33383F, 1.125F, 0.34142F, 1.15F, 0.34863F, 1.175F, 0.35543F,
                            1.2F, 0.3618F, 1.225F, 0.36773F, 1.25F, 0.37321F, 1.275F, 0.3782F,
                            1.3F, 0.38271F, 1.325F, 0.38672F, 1.35F, 0.39021F, 1.375F, 0.39319F,
                            1.4F, 0.39563F, 1.425F, 0.39754F, 1.45F, 0.3989F, 1.475F, 0.39973F,
                            1.5F, 0.4F, 1.525F, 0.39973F, 1.55F, 0.3989F, 1.575F, 0.39754F,
                            1.6F, 0.39563F, 1.625F, 0.39319F, 1.65F, 0.39021F, 1.675F, 0.38672F,
                            1.7F, 0.38271F, 1.725F, 0.3782F, 1.75F, 0.37321F, 1.775F, 0.36773F,
                            1.8F, 0.3618F, 1.825F, 0.35543F, 1.85F, 0.34863F, 1.875F, 0.34142F,
                            1.9F, 0.33383F, 1.925F, 0.32586F, 1.95F, 0.31756F, 1.975F, 0.30893F,
                            2.0F, 0.3F, 2.025F, 0.2908F, 2.05F, 0.28135F, 2.075F, 0.27167F,
                            2.1F, 0.2618F, 2.125F, 0.25176F, 2.15F, 0.24158F, 2.175F, 0.23129F,
                            2.2F, 0.22091F, 2.225F, 0.21047F, 2.25F, 0.2F, 2.275F, 0.18953F,
                            2.3F, 0.17909F, 2.325F, 0.16871F, 2.35F, 0.15842F, 2.375F, 0.14824F,
                            2.4F, 0.1382F, 2.425F, 0.12833F, 2.45F, 0.11865F, 2.475F, 0.1092F,
                            2.5F, 0.1F, 2.525F, 0.09107F, 2.55F, 0.08244F, 2.575F, 0.07414F,
                            2.6F, 0.06617F, 2.625F, 0.05858F, 2.65F, 0.05137F, 2.675F, 0.04457F,
                            2.7F, 0.0382F, 2.725F, 0.03227F, 2.75F, 0.02679F, 2.775F, 0.0218F,
                            2.8F, 0.01729F, 2.825F, 0.01328F, 2.85F, 0.00979F, 2.875F, 0.00681F,
                            2.9F, 0.00437F, 2.925F, 0.00246F, 2.95F, 0.0011F, 2.975F, 0.00027F,
                            3.0F, -0.0F),
                    ZERO_CURVE),
            bone(ModelBone.NECK_1,
                    curve(0.0F, 0.23642F, 0.025F, 0.27609F, 0.05F, 0.31501F, 0.075F, 0.35306F,
                            0.1F, 0.39015F, 0.125F, 0.42617F, 0.15F, 0.46102F, 0.175F, 0.4946F,
                            0.2F, 0.52683F, 0.225F, 0.55762F, 0.25F, 0.58688F, 0.275F, 0.61453F,
                            0.3F, 0.64049F, 0.325F, 0.6647F, 0.35F, 0.68709F, 0.375F, 0.70759F,
                            0.4F, 0.72616F, 0.425F, 0.74273F, 0.45F, 0.75727F, 0.475F, 0.76973F,
                            0.5F, 0.78008F, 0.525F, 0.7883F, 0.55F, 0.79435F, 0.575F, 0.79823F,
                            0.6F, 0.79992F, 0.625F, 0.79942F, 0.65F, 0.79672F, 0.675F, 0.79184F,
                            0.7F, 0.78479F, 0.725F, 0.77559F, 0.75F, 0.76427F, 0.775F, 0.75085F,
                            0.8F, 0.73537F, 0.825F, 0.71788F, 0.85F, 0.69841F, 0.875F, 0.67704F,
                            0.9F, 0.65381F, 0.925F, 0.62878F, 0.95F, 0.60204F, 0.975F, 0.57364F,
                            1.0F, 0.54367F, 1.025F, 0.51221F, 1.05F, 0.47934F, 1.075F, 0.44517F,
                            1.1F, 0.40977F, 1.125F, 0.37325F, 1.15F, 0.3357F, 1.175F, 0.29724F,
                            1.2F, 0.25796F, 1.225F, 0.21798F, 1.25F, 0.17739F, 1.275F, 0.13632F,
                            1.3F, 0.09488F, 1.325F, 0.05318F, 1.35F, 0.01133F, 1.375F, -0.03055F,
                            1.4F, -0.07235F, 1.425F, -0.11395F, 1.45F, -0.15523F, 1.475F, -0.19609F,
                            1.5F, -0.23642F, 1.525F, -0.27609F, 1.55F, -0.31501F, 1.575F, -0.35306F,
                            1.6F, -0.39015F, 1.625F, -0.42617F, 1.65F, -0.46102F, 1.675F, -0.4946F,
                            1.7F, -0.52683F, 1.725F, -0.55762F, 1.75F, -0.58688F, 1.775F, -0.61453F,
                            1.8F, -0.64049F, 1.825F, -0.6647F, 1.85F, -0.68709F, 1.875F, -0.70759F,
                            1.9F, -0.72616F, 1.925F, -0.74273F, 1.95F, -0.75727F, 1.975F, -0.76973F,
                            2.0F, -0.78008F, 2.025F, -0.7883F, 2.05F, -0.79435F, 2.075F, -0.79823F,
                            2.1F, -0.79992F, 2.125F, -0.79942F, 2.15F, -0.79672F, 2.175F, -0.79184F,
                            2.2F, -0.78479F, 2.225F, -0.77559F, 2.25F, -0.76427F, 2.275F, -0.75085F,
                            2.3F, -0.73537F, 2.325F, -0.71788F, 2.35F, -0.69841F, 2.375F, -0.67704F,
                            2.4F, -0.65381F, 2.425F, -0.62878F, 2.45F, -0.60204F, 2.475F, -0.57364F,
                            2.5F, -0.54367F, 2.525F, -0.51221F, 2.55F, -0.47934F, 2.575F, -0.44517F,
                            2.6F, -0.40977F, 2.625F, -0.37325F, 2.65F, -0.3357F, 2.675F, -0.29724F,
                            2.7F, -0.25796F, 2.725F, -0.21798F, 2.75F, -0.17739F, 2.775F, -0.13632F,
                            2.8F, -0.09488F, 2.825F, -0.05318F, 2.85F, -0.01133F, 2.875F, 0.03055F,
                            2.9F, 0.07235F, 2.925F, 0.11395F, 2.95F, 0.15523F, 2.975F, 0.19609F,
                            3.0F, 0.23642F),
                    curve(0.0F, 0.86297F, 0.025F, 0.94446F, 0.05F, 1.02336F, 0.075F, 1.09945F,
                            0.1F, 1.17254F, 0.125F, 1.2424F, 0.15F, 1.30887F, 0.175F, 1.37174F,
                            0.2F, 1.43086F, 0.225F, 1.48605F, 0.25F, 1.53717F, 0.275F, 1.58408F,
                            0.3F, 1.62665F, 0.325F, 1.66476F, 0.35F, 1.6983F, 0.375F, 1.72719F,
                            0.4F, 1.75134F, 0.425F, 1.7707F, 0.45F, 1.7852F, 0.475F, 1.79481F,
                            0.5F, 1.7995F, 0.525F, 1.79926F, 0.55F, 1.79408F, 0.575F, 1.78399F,
                            0.6F, 1.76901F, 0.625F, 1.74918F, 0.65F, 1.72455F, 0.675F, 1.6952F,
                            0.7F, 1.6612F, 0.725F, 1.62265F, 0.75F, 1.57965F, 0.775F, 1.53232F,
                            0.8F, 1.48079F, 0.825F, 1.4252F, 0.85F, 1.36571F, 0.875F, 1.30247F,
                            0.9F, 1.23566F, 0.925F, 1.16547F, 0.95F, 1.09208F, 0.975F, 1.0157F,
                            1.0F, 0.93653F, 1.025F, 0.8548F, 1.05F, 0.77072F, 1.075F, 0.68454F,
                            1.1F, 0.59647F, 1.125F, 0.50677F, 1.15F, 0.41568F, 1.175F, 0.32345F,
                            1.2F, 0.23034F, 1.225F, 0.13659F, 1.25F, 0.04247F, 1.275F, -0.05176F,
                            1.3F, -0.14586F, 1.325F, -0.23955F, 1.35F, -0.33259F, 1.375F, -0.42472F,
                            1.4F, -0.51568F, 1.425F, -0.60523F, 1.45F, -0.69312F, 1.475F, -0.77911F,
                            1.5F, -0.86297F, 1.525F, -0.94446F, 1.55F, -1.02336F, 1.575F, -1.09945F,
                            1.6F, -1.17254F, 1.625F, -1.2424F, 1.65F, -1.30887F, 1.675F, -1.37174F,
                            1.7F, -1.43086F, 1.725F, -1.48605F, 1.75F, -1.53717F, 1.775F, -1.58408F,
                            1.8F, -1.62665F, 1.825F, -1.66476F, 1.85F, -1.6983F, 1.875F, -1.72719F,
                            1.9F, -1.75134F, 1.925F, -1.7707F, 1.95F, -1.7852F, 1.975F, -1.79481F,
                            2.0F, -1.7995F, 2.025F, -1.79926F, 2.05F, -1.79408F, 2.075F, -1.78399F,
                            2.1F, -1.76901F, 2.125F, -1.74918F, 2.15F, -1.72455F, 2.175F, -1.6952F,
                            2.2F, -1.6612F, 2.225F, -1.62265F, 2.25F, -1.57965F, 2.275F, -1.53232F,
                            2.3F, -1.48079F, 2.325F, -1.4252F, 2.35F, -1.36571F, 2.375F, -1.30247F,
                            2.4F, -1.23566F, 2.425F, -1.16547F, 2.45F, -1.09208F, 2.475F, -1.0157F,
                            2.5F, -0.93653F, 2.525F, -0.8548F, 2.55F, -0.77072F, 2.575F, -0.68454F,
                            2.6F, -0.59647F, 2.625F, -0.50677F, 2.65F, -0.41568F, 2.675F, -0.32345F,
                            2.7F, -0.23034F, 2.725F, -0.13659F, 2.75F, -0.04247F, 2.775F, 0.05176F,
                            2.8F, 0.14586F, 2.825F, 0.23955F, 2.85F, 0.33259F, 2.875F, 0.42472F,
                            2.9F, 0.51568F, 2.925F, 0.60523F, 2.95F, 0.69312F, 2.975F, 0.77911F,
                            3.0F, 0.86297F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.NECK_2,
                    curve(0.0F, -0.4673F, 0.025F, -0.40882F, 0.05F, -0.34921F, 0.075F, -0.28865F,
                            0.1F, -0.22729F, 0.125F, -0.16531F, 0.15F, -0.10289F, 0.175F, -0.04017F,
                            0.2F, 0.02265F, 0.225F, 0.0854F, 0.25F, 0.14793F, 0.275F, 0.21006F,
                            0.3F, 0.2716F, 0.325F, 0.33241F, 0.35F, 0.3923F, 0.375F, 0.45111F,
                            0.4F, 0.50869F, 0.425F, 0.56489F, 0.45F, 0.61951F, 0.475F, 0.67245F,
                            0.5F, 0.72353F, 0.525F, 0.77265F, 0.55F, 0.81965F, 0.575F, 0.8644F,
                            0.6F, 0.90677F, 0.625F, 0.94666F, 0.65F, 0.98395F, 0.675F, 1.01856F,
                            0.7F, 1.05037F, 0.725F, 1.07929F, 0.75F, 1.10526F, 0.775F, 1.12822F,
                            0.8F, 1.14806F, 0.825F, 1.16476F, 0.85F, 1.17828F, 0.875F, 1.18856F,
                            0.9F, 1.19558F, 0.925F, 1.19933F, 0.95F, 1.19978F, 0.975F, 1.19696F,
                            1.0F, 1.19085F, 1.025F, 1.18147F, 1.05F, 1.16886F, 1.075F, 1.15304F,
                            1.1F, 1.13405F, 1.125F, 1.11198F, 1.15F, 1.08685F, 1.175F, 1.05873F,
                            1.2F, 1.02771F, 1.225F, 0.99389F, 1.25F, 0.95732F, 1.275F, 0.91815F,
                            1.3F, 0.87646F, 1.325F, 0.83236F, 1.35F, 0.78598F, 1.375F, 0.73744F,
                            1.4F, 0.68689F, 1.425F, 0.63445F, 1.45F, 0.58027F, 1.475F, 0.5245F,
                            1.5F, 0.4673F, 1.525F, 0.40881F, 1.55F, 0.34921F, 1.575F, 0.28865F,
                            1.6F, 0.22729F, 1.625F, 0.1653F, 1.65F, 0.10288F, 1.675F, 0.04017F,
                            1.7F, -0.02265F, 1.725F, -0.08541F, 1.75F, -0.14794F, 1.775F, -0.21007F,
                            1.8F, -0.27162F, 1.825F, -0.33241F, 1.85F, -0.3923F, 1.875F, -0.45111F,
                            1.9F, -0.50869F, 1.925F, -0.56488F, 1.95F, -0.61951F, 1.975F, -0.67246F,
                            2.0F, -0.72354F, 2.025F, -0.77265F, 2.05F, -0.81965F, 2.075F, -0.8644F,
                            2.1F, -0.90678F, 2.125F, -0.94667F, 2.15F, -0.98396F, 2.175F, -1.01856F,
                            2.2F, -1.05037F, 2.225F, -1.0793F, 2.25F, -1.10527F, 2.275F, -1.12822F,
                            2.3F, -1.14806F, 2.325F, -1.16478F, 2.35F, -1.17829F, 2.375F, -1.18856F,
                            2.4F, -1.19559F, 2.425F, -1.19933F, 2.45F, -1.19979F, 2.475F, -1.19696F,
                            2.5F, -1.19085F, 2.525F, -1.18148F, 2.55F, -1.16887F, 2.575F, -1.15305F,
                            2.6F, -1.13407F, 2.625F, -1.11199F, 2.65F, -1.08685F, 2.675F, -1.05873F,
                            2.7F, -1.02772F, 2.725F, -0.99389F, 2.75F, -0.95733F, 2.775F, -0.91815F,
                            2.8F, -0.87647F, 2.825F, -0.83236F, 2.85F, -0.78598F, 2.875F, -0.73744F,
                            2.9F, -0.68689F, 2.925F, -0.63445F, 2.95F, -0.58027F, 2.975F, -0.52451F,
                            3.0F, -0.4673F),
                    curve(0.0F, -0.49667F, 0.025F, -0.36776F, 0.05F, -0.23784F, 0.075F, -0.10727F,
                            0.1F, 0.0236F, 0.125F, 0.1544F, 0.15F, 0.28478F, 0.175F, 0.41438F,
                            0.2F, 0.54284F, 0.225F, 0.66981F, 0.25F, 0.79495F, 0.275F, 0.91791F,
                            0.3F, 1.03835F, 0.325F, 1.15595F, 0.35F, 1.27038F, 0.375F, 1.38133F,
                            0.4F, 1.48849F, 0.425F, 1.59157F, 0.45F, 1.69029F, 0.475F, 1.78437F,
                            0.5F, 1.87357F, 0.525F, 1.95763F, 0.55F, 2.03632F, 0.575F, 2.10944F,
                            0.6F, 2.17677F, 0.625F, 2.23813F, 0.65F, 2.29336F, 0.675F, 2.3423F,
                            0.7F, 2.38483F, 0.725F, 2.42081F, 0.75F, 2.45017F, 0.775F, 2.4728F,
                            0.8F, 2.48866F, 0.825F, 2.4977F, 0.85F, 2.49989F, 0.875F, 2.49523F,
                            0.9F, 2.48373F, 0.925F, 2.46542F, 0.95F, 2.44035F, 0.975F, 2.4086F,
                            1.0F, 2.37024F, 1.025F, 2.32539F, 1.05F, 2.27416F, 1.075F, 2.2167F,
                            1.1F, 2.15317F, 1.125F, 2.08373F, 1.15F, 2.00858F, 1.175F, 1.92793F,
                            1.2F, 1.84199F, 1.225F, 1.751F, 1.25F, 1.65521F, 1.275F, 1.55489F,
                            1.3F, 1.45031F, 1.325F, 1.34175F, 1.35F, 1.22951F, 1.375F, 1.1139F,
                            1.4F, 0.99524F, 1.425F, 0.87385F, 1.45F, 0.75006F, 1.475F, 0.62422F,
                            1.5F, 0.49667F, 1.525F, 0.36776F, 1.55F, 0.23784F, 1.575F, 0.10727F,
                            1.6F, -0.0236F, 1.625F, -0.1544F, 1.65F, -0.28478F, 1.675F, -0.41438F,
                            1.7F, -0.54284F, 1.725F, -0.66981F, 1.75F, -0.79495F, 1.775F, -0.91791F,
                            1.8F, -1.03835F, 1.825F, -1.15595F, 1.85F, -1.27038F, 1.875F, -1.38133F,
                            1.9F, -1.48849F, 1.925F, -1.59157F, 1.95F, -1.69029F, 1.975F, -1.78437F,
                            2.0F, -1.87357F, 2.025F, -1.95763F, 2.05F, -2.03632F, 2.075F, -2.10944F,
                            2.1F, -2.17677F, 2.125F, -2.23813F, 2.15F, -2.29336F, 2.175F, -2.3423F,
                            2.2F, -2.38483F, 2.225F, -2.42081F, 2.25F, -2.45017F, 2.275F, -2.4728F,
                            2.3F, -2.48866F, 2.325F, -2.4977F, 2.35F, -2.49989F, 2.375F, -2.49523F,
                            2.4F, -2.48373F, 2.425F, -2.46542F, 2.45F, -2.44035F, 2.475F, -2.4086F,
                            2.5F, -2.37024F, 2.525F, -2.32539F, 2.55F, -2.27416F, 2.575F, -2.2167F,
                            2.6F, -2.15317F, 2.625F, -2.08373F, 2.65F, -2.00858F, 2.675F, -1.92793F,
                            2.7F, -1.84199F, 2.725F, -1.751F, 2.75F, -1.65521F, 2.775F, -1.55489F,
                            2.8F, -1.45031F, 2.825F, -1.34175F, 2.85F, -1.22951F, 2.875F, -1.1139F,
                            2.9F, -0.99524F, 2.925F, -0.87385F, 2.95F, -0.75006F, 2.975F, -0.62422F,
                            3.0F, -0.49667F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.HEAD,
                    curve(0.0F, 0.35048F, 0.025F, 0.30661F, 0.05F, 0.26191F, 0.075F, 0.21648F,
                            0.1F, 0.17047F, 0.125F, 0.12399F, 0.15F, 0.07716F, 0.175F, 0.03013F,
                            0.2F, -0.01699F, 0.225F, -0.06406F, 0.25F, -0.11096F, 0.275F, -0.15755F,
                            0.3F, -0.20371F, 0.325F, -0.24931F, 0.35F, -0.29422F, 0.375F, -0.33834F,
                            0.4F, -0.38152F, 0.425F, -0.42366F, 0.45F, -0.46462F, 0.475F, -0.50434F,
                            0.5F, -0.54266F, 0.525F, -0.57949F, 0.55F, -0.61474F, 0.575F, -0.6483F,
                            0.6F, -0.68008F, 0.625F, -0.71F, 0.65F, -0.73797F, 0.675F, -0.76392F,
                            0.7F, -0.78777F, 0.725F, -0.80948F, 0.75F, -0.82895F, 0.775F, -0.84616F,
                            0.8F, -0.86105F, 0.825F, -0.87358F, 0.85F, -0.88371F, 0.875F, -0.89142F,
                            0.9F, -0.89669F, 0.925F, -0.8995F, 0.95F, -0.89984F, 0.975F, -0.89772F,
                            1.0F, -0.89313F, 1.025F, -0.8861F, 1.05F, -0.87664F, 1.075F, -0.86477F,
                            1.1F, -0.85055F, 1.125F, -0.83398F, 1.15F, -0.81513F, 1.175F, -0.79405F,
                            1.2F, -0.77078F, 1.225F, -0.74542F, 1.25F, -0.71801F, 1.275F, -0.68861F,
                            1.3F, -0.65734F, 1.325F, -0.62427F, 1.35F, -0.58948F, 1.375F, -0.55307F,
                            1.4F, -0.51516F, 1.425F, -0.47584F, 1.45F, -0.43521F, 1.475F, -0.39338F,
                            1.5F, -0.35048F, 1.525F, -0.30661F, 1.55F, -0.26191F, 1.575F, -0.21648F,
                            1.6F, -0.17047F, 1.625F, -0.12398F, 1.65F, -0.07716F, 1.675F, -0.03013F,
                            1.7F, 0.01699F, 1.725F, 0.06406F, 1.75F, 0.11097F, 1.775F, 0.15755F,
                            1.8F, 0.20371F, 1.825F, 0.24931F, 1.85F, 0.29422F, 1.875F, 0.33834F,
                            1.9F, 0.38152F, 1.925F, 0.42366F, 1.95F, 0.46463F, 1.975F, 0.50434F,
                            2.0F, 0.54266F, 2.025F, 0.57948F, 2.05F, 0.61474F, 2.075F, 0.6483F,
                            2.1F, 0.68008F, 2.125F, 0.71F, 2.15F, 0.73798F, 2.175F, 0.76392F,
                            2.2F, 0.78778F, 2.225F, 0.80948F, 2.25F, 0.82895F, 2.275F, 0.84616F,
                            2.3F, 0.86105F, 2.325F, 0.87359F, 2.35F, 0.88371F, 2.375F, 0.89142F,
                            2.4F, 0.89669F, 2.425F, 0.8995F, 2.45F, 0.89984F, 2.475F, 0.89772F,
                            2.5F, 0.89313F, 2.525F, 0.8861F, 2.55F, 0.87664F, 2.575F, 0.86478F,
                            2.6F, 0.85055F, 2.625F, 0.83398F, 2.65F, 0.81513F, 2.675F, 0.79405F,
                            2.7F, 0.77079F, 2.725F, 0.74542F, 2.75F, 0.718F, 2.775F, 0.68861F,
                            2.8F, 0.65733F, 2.825F, 0.62426F, 2.85F, 0.58948F, 2.875F, 0.55308F,
                            2.9F, 0.51517F, 2.925F, 0.47584F, 2.95F, 0.43521F, 2.975F, 0.39338F,
                            3.0F, 0.35048F),
                    curve(0.0F, 0.43707F, 0.025F, 0.32363F, 0.05F, 0.2093F, 0.075F, 0.0944F,
                            0.1F, -0.02077F, 0.125F, -0.13587F, 0.15F, -0.25061F, 0.175F, -0.36465F,
                            0.2F, -0.4777F, 0.225F, -0.58944F, 0.25F, -0.69956F, 0.275F, -0.80776F,
                            0.3F, -0.91375F, 0.325F, -1.01724F, 0.35F, -1.11794F, 0.375F, -1.21557F,
                            0.4F, -1.30987F, 0.425F, -1.40058F, 0.45F, -1.48745F, 0.475F, -1.57025F,
                            0.5F, -1.64874F, 0.525F, -1.72271F, 0.55F, -1.79196F, 0.575F, -1.8563F,
                            0.6F, -1.91555F, 0.625F, -1.96955F, 0.65F, -2.01816F, 0.675F, -2.06123F,
                            0.7F, -2.09865F, 0.725F, -2.13032F, 0.75F, -2.15615F, 0.775F, -2.17607F,
                            0.8F, -2.19002F, 0.825F, -2.19797F, 0.85F, -2.1999F, 0.875F, -2.1958F,
                            0.9F, -2.18568F, 0.925F, -2.16957F, 0.95F, -2.14751F, 0.975F, -2.11957F,
                            1.0F, -2.08581F, 1.025F, -2.04634F, 1.05F, -2.00126F, 1.075F, -1.9507F,
                            1.1F, -1.89479F, 1.125F, -1.83368F, 1.15F, -1.76755F, 1.175F, -1.69658F,
                            1.2F, -1.62095F, 1.225F, -1.54088F, 1.25F, -1.45659F, 1.275F, -1.3683F,
                            1.3F, -1.27627F, 1.325F, -1.18074F, 1.35F, -1.08197F, 1.375F, -0.98023F,
                            1.4F, -0.87581F, 1.425F, -0.76899F, 1.45F, -0.66006F, 1.475F, -0.54932F,
                            1.5F, -0.43707F, 1.525F, -0.32363F, 1.55F, -0.2093F, 1.575F, -0.0944F,
                            1.6F, 0.02077F, 1.625F, 0.13587F, 1.65F, 0.25061F, 1.675F, 0.36465F,
                            1.7F, 0.4777F, 1.725F, 0.58944F, 1.75F, 0.69956F, 1.775F, 0.80776F,
                            1.8F, 0.91375F, 1.825F, 1.01724F, 1.85F, 1.11794F, 1.875F, 1.21557F,
                            1.9F, 1.30987F, 1.925F, 1.40058F, 1.95F, 1.48745F, 1.975F, 1.57025F,
                            2.0F, 1.64874F, 2.025F, 1.72271F, 2.05F, 1.79196F, 2.075F, 1.8563F,
                            2.1F, 1.91555F, 2.125F, 1.96955F, 2.15F, 2.01816F, 2.175F, 2.06123F,
                            2.2F, 2.09865F, 2.225F, 2.13032F, 2.25F, 2.15615F, 2.275F, 2.17607F,
                            2.3F, 2.19002F, 2.325F, 2.19797F, 2.35F, 2.1999F, 2.375F, 2.1958F,
                            2.4F, 2.18568F, 2.425F, 2.16957F, 2.45F, 2.14751F, 2.475F, 2.11957F,
                            2.5F, 2.08581F, 2.525F, 2.04634F, 2.55F, 2.00126F, 2.575F, 1.9507F,
                            2.6F, 1.89479F, 2.625F, 1.83368F, 2.65F, 1.76755F, 2.675F, 1.69658F,
                            2.7F, 1.62095F, 2.725F, 1.54088F, 2.75F, 1.45659F, 2.775F, 1.3683F,
                            2.8F, 1.27627F, 2.825F, 1.18074F, 2.85F, 1.08197F, 2.875F, 0.98023F,
                            2.9F, 0.87581F, 2.925F, 0.76899F, 2.95F, 0.66006F, 2.975F, 0.54932F,
                            3.0F, 0.43707F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.JAW,
                    curve(0.0F, 0.97731F, 0.025F, 1.00707F, 0.05F, 1.03626F, 0.075F, 1.0648F,
                            0.1F, 1.09261F, 0.125F, 1.11963F, 0.15F, 1.14576F, 0.175F, 1.17095F,
                            0.2F, 1.19512F, 0.225F, 1.21821F, 0.25F, 1.24016F, 0.275F, 1.26089F,
                            0.3F, 1.28037F, 0.325F, 1.29853F, 0.35F, 1.31532F, 0.375F, 1.33069F,
                            0.4F, 1.34462F, 0.425F, 1.35705F, 0.45F, 1.36795F, 0.475F, 1.3773F,
                            0.5F, 1.38506F, 0.525F, 1.39122F, 0.55F, 1.39577F, 0.575F, 1.39867F,
                            0.6F, 1.39994F, 0.625F, 1.39956F, 0.65F, 1.39754F, 0.675F, 1.39388F,
                            0.7F, 1.38861F, 0.725F, 1.3817F, 0.75F, 1.3732F, 0.775F, 1.36314F,
                            0.8F, 1.35153F, 0.825F, 1.33841F, 0.85F, 1.32381F, 0.875F, 1.30778F,
                            0.9F, 1.29035F, 0.925F, 1.27159F, 0.95F, 1.25153F, 0.975F, 1.23023F,
                            1.0F, 1.20775F, 1.025F, 1.18416F, 1.05F, 1.15951F, 1.075F, 1.13388F,
                            1.1F, 1.10733F, 1.125F, 1.07994F, 1.15F, 1.05178F, 1.175F, 1.02293F,
                            1.2F, 0.99347F, 1.225F, 0.96348F, 1.25F, 0.93304F, 1.275F, 0.90224F,
                            1.3F, 0.87116F, 1.325F, 0.83988F, 1.35F, 0.8085F, 1.375F, 0.77709F,
                            1.4F, 0.74574F, 1.425F, 0.71454F, 1.45F, 0.68358F, 1.475F, 0.65293F,
                            1.5F, 0.62269F, 1.525F, 0.59293F, 1.55F, 0.56374F, 1.575F, 0.5352F,
                            1.6F, 0.50739F, 1.625F, 0.48037F, 1.65F, 0.45424F, 1.675F, 0.42905F,
                            1.7F, 0.40488F, 1.725F, 0.38179F, 1.75F, 0.35984F, 1.775F, 0.33911F,
                            1.8F, 0.31963F, 1.825F, 0.30147F, 1.85F, 0.28468F, 1.875F, 0.26931F,
                            1.9F, 0.25538F, 1.925F, 0.24295F, 1.95F, 0.23205F, 1.975F, 0.2227F,
                            2.0F, 0.21494F, 2.025F, 0.20878F, 2.05F, 0.20423F, 2.075F, 0.20133F,
                            2.1F, 0.20006F, 2.125F, 0.20044F, 2.15F, 0.20246F, 2.175F, 0.20612F,
                            2.2F, 0.2114F, 2.225F, 0.2183F, 2.25F, 0.2268F, 2.275F, 0.23686F,
                            2.3F, 0.24847F, 2.325F, 0.26159F, 2.35F, 0.27619F, 2.375F, 0.29222F,
                            2.4F, 0.30965F, 2.425F, 0.32841F, 2.45F, 0.34847F, 2.475F, 0.36977F,
                            2.5F, 0.39225F, 2.525F, 0.41584F, 2.55F, 0.44049F, 2.575F, 0.46612F,
                            2.6F, 0.49267F, 2.625F, 0.52006F, 2.65F, 0.54822F, 2.675F, 0.57707F,
                            2.7F, 0.60653F, 2.725F, 0.63652F, 2.75F, 0.66696F, 2.775F, 0.69776F,
                            2.8F, 0.72884F, 2.825F, 0.76012F, 2.85F, 0.7915F, 2.875F, 0.82291F,
                            2.9F, 0.85426F, 2.925F, 0.88546F, 2.95F, 0.91642F, 2.975F, 0.94707F,
                            3.0F, 0.97731F),
                    ZERO_CURVE,
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_1,
                    curve(0.0F, -0.0F, 0.025F, 0.01832F, 0.05F, 0.03658F, 0.075F, 0.05475F,
                            0.1F, 0.07277F, 0.125F, 0.09059F, 0.15F, 0.10816F, 0.175F, 0.12543F,
                            0.2F, 0.14236F, 0.225F, 0.1589F, 0.25F, 0.175F, 0.275F, 0.19062F,
                            0.3F, 0.20572F, 0.325F, 0.22026F, 0.35F, 0.2342F, 0.375F, 0.24749F,
                            0.4F, 0.2601F, 0.425F, 0.272F, 0.45F, 0.28316F, 0.475F, 0.29353F,
                            0.5F, 0.30311F, 0.525F, 0.31185F, 0.55F, 0.31974F, 0.575F, 0.32675F,
                            0.6F, 0.33287F, 0.625F, 0.33807F, 0.65F, 0.34235F, 0.675F, 0.34569F,
                            0.7F, 0.34808F, 0.725F, 0.34952F, 0.75F, 0.35F, 0.775F, 0.34952F,
                            0.8F, 0.34808F, 0.825F, 0.34569F, 0.85F, 0.34235F, 0.875F, 0.33807F,
                            0.9F, 0.33287F, 0.925F, 0.32675F, 0.95F, 0.31974F, 0.975F, 0.31185F,
                            1.0F, 0.30311F, 1.025F, 0.29353F, 1.05F, 0.28316F, 1.075F, 0.272F,
                            1.1F, 0.2601F, 1.125F, 0.24749F, 1.15F, 0.2342F, 1.175F, 0.22026F,
                            1.2F, 0.20572F, 1.225F, 0.19062F, 1.25F, 0.175F, 1.275F, 0.1589F,
                            1.3F, 0.14236F, 1.325F, 0.12543F, 1.35F, 0.10816F, 1.375F, 0.09059F,
                            1.4F, 0.07277F, 1.425F, 0.05475F, 1.45F, 0.03658F, 1.475F, 0.01832F,
                            1.5F, -0.0F, 1.525F, -0.01832F, 1.55F, -0.03658F, 1.575F, -0.05475F,
                            1.6F, -0.07277F, 1.625F, -0.09059F, 1.65F, -0.10816F, 1.675F, -0.12543F,
                            1.7F, -0.14236F, 1.725F, -0.1589F, 1.75F, -0.175F, 1.775F, -0.19062F,
                            1.8F, -0.20572F, 1.825F, -0.22026F, 1.85F, -0.2342F, 1.875F, -0.24749F,
                            1.9F, -0.2601F, 1.925F, -0.272F, 1.95F, -0.28316F, 1.975F, -0.29353F,
                            2.0F, -0.30311F, 2.025F, -0.31185F, 2.05F, -0.31974F, 2.075F, -0.32675F,
                            2.1F, -0.33287F, 2.125F, -0.33807F, 2.15F, -0.34235F, 2.175F, -0.34569F,
                            2.2F, -0.34808F, 2.225F, -0.34952F, 2.25F, -0.35F, 2.275F, -0.34952F,
                            2.3F, -0.34808F, 2.325F, -0.34569F, 2.35F, -0.34235F, 2.375F, -0.33807F,
                            2.4F, -0.33287F, 2.425F, -0.32675F, 2.45F, -0.31974F, 2.475F, -0.31185F,
                            2.5F, -0.30311F, 2.525F, -0.29353F, 2.55F, -0.28316F, 2.575F, -0.272F,
                            2.6F, -0.2601F, 2.625F, -0.24749F, 2.65F, -0.2342F, 2.675F, -0.22026F,
                            2.7F, -0.20572F, 2.725F, -0.19062F, 2.75F, -0.175F, 2.775F, -0.1589F,
                            2.8F, -0.14236F, 2.825F, -0.12543F, 2.85F, -0.10816F, 2.875F, -0.09059F,
                            2.9F, -0.07277F, 2.925F, -0.05475F, 2.95F, -0.03658F, 2.975F, -0.01832F,
                            3.0F, -0.0F),
                    curve(0.0F, -0.0F, 0.025F, 0.11514F, 0.05F, 0.22996F, 0.075F, 0.34416F,
                            0.1F, 0.45741F, 0.125F, 0.5694F, 0.15F, 0.67984F, 0.175F, 0.78841F,
                            0.2F, 0.89482F, 0.225F, 0.99878F, 0.25F, 1.1F, 0.275F, 1.19821F,
                            0.3F, 1.29313F, 0.325F, 1.3845F, 0.35F, 1.47209F, 0.375F, 1.55563F,
                            0.4F, 1.63492F, 0.425F, 1.70972F, 0.45F, 1.77984F, 0.475F, 1.84508F,
                            0.5F, 1.90526F, 0.525F, 1.96021F, 0.55F, 2.0098F, 0.575F, 2.05388F,
                            0.6F, 2.09232F, 0.625F, 2.12504F, 0.65F, 2.15192F, 0.675F, 2.17291F,
                            0.7F, 2.18795F, 0.725F, 2.19698F, 0.75F, 2.2F, 0.775F, 2.19698F,
                            0.8F, 2.18795F, 0.825F, 2.17291F, 0.85F, 2.15192F, 0.875F, 2.12504F,
                            0.9F, 2.09232F, 0.925F, 2.05388F, 0.95F, 2.0098F, 0.975F, 1.96021F,
                            1.0F, 1.90526F, 1.025F, 1.84508F, 1.05F, 1.77984F, 1.075F, 1.70972F,
                            1.1F, 1.63492F, 1.125F, 1.55563F, 1.15F, 1.47209F, 1.175F, 1.3845F,
                            1.2F, 1.29313F, 1.225F, 1.19821F, 1.25F, 1.1F, 1.275F, 0.99878F,
                            1.3F, 0.89482F, 1.325F, 0.78841F, 1.35F, 0.67984F, 1.375F, 0.5694F,
                            1.4F, 0.45741F, 1.425F, 0.34416F, 1.45F, 0.22996F, 1.475F, 0.11514F,
                            1.5F, -0.0F, 1.525F, -0.11514F, 1.55F, -0.22996F, 1.575F, -0.34416F,
                            1.6F, -0.45741F, 1.625F, -0.5694F, 1.65F, -0.67984F, 1.675F, -0.78841F,
                            1.7F, -0.89482F, 1.725F, -0.99878F, 1.75F, -1.1F, 1.775F, -1.19821F,
                            1.8F, -1.29313F, 1.825F, -1.3845F, 1.85F, -1.47209F, 1.875F, -1.55563F,
                            1.9F, -1.63492F, 1.925F, -1.70972F, 1.95F, -1.77984F, 1.975F, -1.84508F,
                            2.0F, -1.90526F, 2.025F, -1.96021F, 2.05F, -2.0098F, 2.075F, -2.05388F,
                            2.1F, -2.09232F, 2.125F, -2.12504F, 2.15F, -2.15192F, 2.175F, -2.17291F,
                            2.2F, -2.18795F, 2.225F, -2.19698F, 2.25F, -2.2F, 2.275F, -2.19698F,
                            2.3F, -2.18795F, 2.325F, -2.17291F, 2.35F, -2.15192F, 2.375F, -2.12504F,
                            2.4F, -2.09232F, 2.425F, -2.05388F, 2.45F, -2.0098F, 2.475F, -1.96021F,
                            2.5F, -1.90526F, 2.525F, -1.84508F, 2.55F, -1.77984F, 2.575F, -1.70972F,
                            2.6F, -1.63492F, 2.625F, -1.55563F, 2.65F, -1.47209F, 2.675F, -1.3845F,
                            2.7F, -1.29313F, 2.725F, -1.19821F, 2.75F, -1.1F, 2.775F, -0.99878F,
                            2.8F, -0.89482F, 2.825F, -0.78841F, 2.85F, -0.67984F, 2.875F, -0.5694F,
                            2.9F, -0.45741F, 2.925F, -0.34416F, 2.95F, -0.22996F, 2.975F, -0.11514F,
                            3.0F, -0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_2,
                    curve(0.0F, -0.28444F, 0.025F, -0.26447F, 0.05F, -0.24377F, 0.075F, -0.2224F,
                            0.1F, -0.20043F, 0.125F, -0.17791F, 0.15F, -0.15489F, 0.175F, -0.13146F,
                            0.2F, -0.10766F, 0.225F, -0.08357F, 0.25F, -0.05925F, 0.275F, -0.03477F,
                            0.3F, -0.01019F, 0.325F, 0.01442F, 0.35F, 0.03898F, 0.375F, 0.06344F,
                            0.4F, 0.08773F, 0.425F, 0.11177F, 0.45F, 0.13551F, 0.475F, 0.15888F,
                            0.5F, 0.18181F, 0.525F, 0.20425F, 0.55F, 0.22612F, 0.575F, 0.24737F,
                            0.6F, 0.26795F, 0.625F, 0.28779F, 0.65F, 0.30685F, 0.675F, 0.32506F,
                            0.7F, 0.34238F, 0.725F, 0.35876F, 0.75F, 0.37416F, 0.775F, 0.38853F,
                            0.8F, 0.40184F, 0.825F, 0.41405F, 0.85F, 0.42512F, 0.875F, 0.43503F,
                            0.9F, 0.44374F, 0.925F, 0.45124F, 0.95F, 0.4575F, 0.975F, 0.46251F,
                            1.0F, 0.46625F, 1.025F, 0.46871F, 1.05F, 0.46989F, 1.075F, 0.46978F,
                            1.1F, 0.46838F, 1.125F, 0.4657F, 1.15F, 0.46174F, 1.175F, 0.45652F,
                            1.2F, 0.45004F, 1.225F, 0.44233F, 1.25F, 0.43341F, 1.275F, 0.4233F,
                            1.3F, 0.41203F, 1.325F, 0.39963F, 1.35F, 0.38614F, 1.375F, 0.37159F,
                            1.4F, 0.35601F, 1.425F, 0.33947F, 1.45F, 0.32199F, 1.475F, 0.30363F,
                            1.5F, 0.28444F, 1.525F, 0.26447F, 1.55F, 0.24377F, 1.575F, 0.2224F,
                            1.6F, 0.20043F, 1.625F, 0.17791F, 1.65F, 0.15489F, 1.675F, 0.13146F,
                            1.7F, 0.10766F, 1.725F, 0.08357F, 1.75F, 0.05925F, 1.775F, 0.03477F,
                            1.8F, 0.01019F, 1.825F, -0.01442F, 1.85F, -0.03898F, 1.875F, -0.06344F,
                            1.9F, -0.08773F, 1.925F, -0.11177F, 1.95F, -0.13551F, 1.975F, -0.15888F,
                            2.0F, -0.18181F, 2.025F, -0.20425F, 2.05F, -0.22612F, 2.075F, -0.24737F,
                            2.1F, -0.26795F, 2.125F, -0.28779F, 2.15F, -0.30685F, 2.175F, -0.32506F,
                            2.2F, -0.34238F, 2.225F, -0.35876F, 2.25F, -0.37416F, 2.275F, -0.38853F,
                            2.3F, -0.40184F, 2.325F, -0.41405F, 2.35F, -0.42512F, 2.375F, -0.43503F,
                            2.4F, -0.44374F, 2.425F, -0.45124F, 2.45F, -0.4575F, 2.475F, -0.46251F,
                            2.5F, -0.46625F, 2.525F, -0.46871F, 2.55F, -0.46989F, 2.575F, -0.46978F,
                            2.6F, -0.46838F, 2.625F, -0.4657F, 2.65F, -0.46174F, 2.675F, -0.45652F,
                            2.7F, -0.45004F, 2.725F, -0.44233F, 2.75F, -0.43341F, 2.775F, -0.4233F,
                            2.8F, -0.41203F, 2.825F, -0.39963F, 2.85F, -0.38614F, 2.875F, -0.37159F,
                            2.9F, -0.35601F, 2.925F, -0.33947F, 2.95F, -0.32199F, 2.975F, -0.30363F,
                            3.0F, -0.28444F),
                    curve(0.0F, -2.17597F, 0.025F, -2.04314F, 0.05F, -1.90472F, 0.075F, -1.76107F,
                            0.1F, -1.6126F, 0.125F, -1.45971F, 0.15F, -1.30281F, 0.175F, -1.14235F,
                            0.2F, -0.97875F, 0.225F, -0.81247F, 0.25F, -0.64397F, 0.275F, -0.47369F,
                            0.3F, -0.30213F, 0.325F, -0.12973F, 0.35F, 0.04303F, 0.375F, 0.21566F,
                            0.4F, 0.3877F, 0.425F, 0.55869F, 0.45F, 0.72814F, 0.475F, 0.89559F,
                            0.5F, 1.06059F, 0.525F, 1.22268F, 0.55F, 1.38142F, 0.575F, 1.53638F,
                            0.6F, 1.68712F, 0.625F, 1.83324F, 0.65F, 1.97433F, 0.675F, 2.11002F,
                            0.7F, 2.23992F, 0.725F, 2.36368F, 0.75F, 2.48096F, 0.775F, 2.59144F,
                            0.8F, 2.69482F, 0.825F, 2.79081F, 0.85F, 2.87915F, 0.875F, 2.9596F,
                            0.9F, 3.03194F, 0.925F, 3.09597F, 0.95F, 3.15152F, 0.975F, 3.19842F,
                            1.0F, 3.23656F, 1.025F, 3.26583F, 1.05F, 3.28614F, 1.075F, 3.29745F,
                            1.1F, 3.29972F, 1.125F, 3.29295F, 1.15F, 3.27715F, 1.175F, 3.25236F,
                            1.2F, 3.21867F, 1.225F, 3.17615F, 1.25F, 3.12492F, 1.275F, 3.06513F,
                            1.3F, 2.99694F, 1.325F, 2.92054F, 1.35F, 2.83613F, 1.375F, 2.74394F,
                            1.4F, 2.64424F, 1.425F, 2.53729F, 1.45F, 2.42338F, 1.475F, 2.30283F,
                            1.5F, 2.17597F, 1.525F, 2.04314F, 1.55F, 1.90472F, 1.575F, 1.76107F,
                            1.6F, 1.6126F, 1.625F, 1.45971F, 1.65F, 1.30281F, 1.675F, 1.14235F,
                            1.7F, 0.97875F, 1.725F, 0.81247F, 1.75F, 0.64397F, 1.775F, 0.47369F,
                            1.8F, 0.30213F, 1.825F, 0.12973F, 1.85F, -0.04303F, 1.875F, -0.21566F,
                            1.9F, -0.3877F, 1.925F, -0.55869F, 1.95F, -0.72814F, 1.975F, -0.89559F,
                            2.0F, -1.06059F, 2.025F, -1.22268F, 2.05F, -1.38142F, 2.075F, -1.53638F,
                            2.1F, -1.68712F, 2.125F, -1.83324F, 2.15F, -1.97433F, 2.175F, -2.11002F,
                            2.2F, -2.23992F, 2.225F, -2.36368F, 2.25F, -2.48096F, 2.275F, -2.59144F,
                            2.3F, -2.69482F, 2.325F, -2.79081F, 2.35F, -2.87915F, 2.375F, -2.9596F,
                            2.4F, -3.03195F, 2.425F, -3.09597F, 2.45F, -3.15152F, 2.475F, -3.19842F,
                            2.5F, -3.23656F, 2.525F, -3.26583F, 2.55F, -3.28614F, 2.575F, -3.29745F,
                            2.6F, -3.29972F, 2.625F, -3.29295F, 2.65F, -3.27715F, 2.675F, -3.25236F,
                            2.7F, -3.21867F, 2.725F, -3.17615F, 2.75F, -3.12492F, 2.775F, -3.06513F,
                            2.8F, -2.99694F, 2.825F, -2.92054F, 2.85F, -2.83613F, 2.875F, -2.74394F,
                            2.9F, -2.64424F, 2.925F, -2.53729F, 2.95F, -2.42338F, 2.975F, -2.30283F,
                            3.0F, -2.17597F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_3,
                    curve(0.0F, -0.5685F, 0.025F, -0.55946F, 0.05F, -0.54889F, 0.075F, -0.53681F,
                            0.1F, -0.52326F, 0.125F, -0.50828F, 0.15F, -0.4919F, 0.175F, -0.47418F,
                            0.2F, -0.45516F, 0.225F, -0.43489F, 0.25F, -0.41342F, 0.275F, -0.39083F,
                            0.3F, -0.36716F, 0.325F, -0.34248F, 0.35F, -0.31687F, 0.375F, -0.29039F,
                            0.4F, -0.26311F, 0.425F, -0.23512F, 0.45F, -0.20647F, 0.475F, -0.17726F,
                            0.5F, -0.14757F, 0.525F, -0.11747F, 0.55F, -0.08705F, 0.575F, -0.05639F,
                            0.6F, -0.02558F, 0.625F, 0.00531F, 0.65F, 0.03618F, 0.675F, 0.06695F,
                            0.7F, 0.09754F, 0.725F, 0.12786F, 0.75F, 0.15782F, 0.775F, 0.18736F,
                            0.8F, 0.21638F, 0.825F, 0.24481F, 0.85F, 0.27257F, 0.875F, 0.29959F,
                            0.9F, 0.32578F, 0.925F, 0.35107F, 0.95F, 0.37541F, 0.975F, 0.39872F,
                            1.0F, 0.42093F, 1.025F, 0.44199F, 1.05F, 0.46184F, 1.075F, 0.48042F,
                            1.1F, 0.49769F, 1.125F, 0.51359F, 1.15F, 0.52808F, 1.175F, 0.54113F,
                            1.2F, 0.55269F, 1.225F, 0.56274F, 1.25F, 0.57125F, 1.275F, 0.57819F,
                            1.3F, 0.58354F, 1.325F, 0.5873F, 1.35F, 0.58945F, 1.375F, 0.58998F,
                            1.4F, 0.58889F, 1.425F, 0.58619F, 1.45F, 0.58188F, 1.475F, 0.57598F,
                            1.5F, 0.5685F, 1.525F, 0.55946F, 1.55F, 0.54889F, 1.575F, 0.53681F,
                            1.6F, 0.52326F, 1.625F, 0.50828F, 1.65F, 0.4919F, 1.675F, 0.47418F,
                            1.7F, 0.45516F, 1.725F, 0.43489F, 1.75F, 0.41342F, 1.775F, 0.39083F,
                            1.8F, 0.36716F, 1.825F, 0.34248F, 1.85F, 0.31687F, 1.875F, 0.29039F,
                            1.9F, 0.26311F, 1.925F, 0.23512F, 1.95F, 0.20647F, 1.975F, 0.17726F,
                            2.0F, 0.14757F, 2.025F, 0.11747F, 2.05F, 0.08705F, 2.075F, 0.05639F,
                            2.1F, 0.02558F, 2.125F, -0.00531F, 2.15F, -0.03618F, 2.175F, -0.06695F,
                            2.2F, -0.09754F, 2.225F, -0.12786F, 2.25F, -0.15782F, 2.275F, -0.18736F,
                            2.3F, -0.21638F, 2.325F, -0.24481F, 2.35F, -0.27257F, 2.375F, -0.29959F,
                            2.4F, -0.32578F, 2.425F, -0.35107F, 2.45F, -0.37541F, 2.475F, -0.39872F,
                            2.5F, -0.42093F, 2.525F, -0.44199F, 2.55F, -0.46184F, 2.575F, -0.48042F,
                            2.6F, -0.49769F, 2.625F, -0.51359F, 2.65F, -0.52808F, 2.675F, -0.54113F,
                            2.7F, -0.55269F, 2.725F, -0.56274F, 2.75F, -0.57125F, 2.775F, -0.57819F,
                            2.8F, -0.58354F, 2.825F, -0.5873F, 2.85F, -0.58945F, 2.875F, -0.58998F,
                            2.9F, -0.58889F, 2.925F, -0.58619F, 2.95F, -0.58188F, 2.975F, -0.57598F,
                            3.0F, -0.5685F),
                    curve(0.0F, -4.36242F, 0.025F, -4.3264F, 0.05F, -4.27853F, 0.075F, -4.21894F,
                            0.1F, -4.14777F, 0.125F, -4.06524F, 0.15F, -3.97157F, 0.175F, -3.86701F,
                            0.2F, -3.75185F, 0.225F, -3.62641F, 0.25F, -3.49103F, 0.275F, -3.34608F,
                            0.3F, -3.19196F, 0.325F, -3.02909F, 0.35F, -2.85792F, 0.375F, -2.67891F,
                            0.4F, -2.49256F, 0.425F, -2.29938F, 0.45F, -2.0999F, 0.475F, -1.89466F,
                            0.5F, -1.68423F, 0.525F, -1.46918F, 0.55F, -1.2501F, 0.575F, -1.0276F,
                            0.6F, -0.80228F, 0.625F, -0.57477F, 0.65F, -0.34567F, 0.675F, -0.11563F,
                            0.7F, 0.11472F, 0.725F, 0.34477F, 0.75F, 0.57386F, 0.775F, 0.80139F,
                            0.8F, 1.02672F, 0.825F, 1.24923F, 0.85F, 1.46832F, 0.875F, 1.68339F,
                            0.9F, 1.89384F, 0.925F, 2.0991F, 0.95F, 2.29861F, 0.975F, 2.49181F,
                            1.0F, 2.67819F, 1.025F, 2.85723F, 1.05F, 3.02843F, 1.075F, 3.19133F,
                            1.1F, 3.34549F, 1.125F, 3.49048F, 1.15F, 3.6259F, 1.175F, 3.75138F,
                            1.2F, 3.86658F, 1.225F, 3.97118F, 1.25F, 4.0649F, 1.275F, 4.14747F,
                            1.3F, 4.21868F, 1.325F, 4.27832F, 1.35F, 4.32624F, 1.375F, 4.3623F,
                            1.4F, 4.3864F, 1.425F, 4.39848F, 1.45F, 4.3985F, 1.475F, 4.38647F,
                            1.5F, 4.36242F, 1.525F, 4.3264F, 1.55F, 4.27853F, 1.575F, 4.21894F,
                            1.6F, 4.14777F, 1.625F, 4.06524F, 1.65F, 3.97157F, 1.675F, 3.86701F,
                            1.7F, 3.75185F, 1.725F, 3.62641F, 1.75F, 3.49103F, 1.775F, 3.34608F,
                            1.8F, 3.19196F, 1.825F, 3.02909F, 1.85F, 2.85792F, 1.875F, 2.67891F,
                            1.9F, 2.49256F, 1.925F, 2.29938F, 1.95F, 2.0999F, 1.975F, 1.89466F,
                            2.0F, 1.68423F, 2.025F, 1.46918F, 2.05F, 1.2501F, 2.075F, 1.0276F,
                            2.1F, 0.80228F, 2.125F, 0.57477F, 2.15F, 0.34567F, 2.175F, 0.11563F,
                            2.2F, -0.11472F, 2.225F, -0.34477F, 2.25F, -0.57386F, 2.275F, -0.80139F,
                            2.3F, -1.02672F, 2.325F, -1.24923F, 2.35F, -1.46832F, 2.375F, -1.68339F,
                            2.4F, -1.89384F, 2.425F, -2.0991F, 2.45F, -2.29861F, 2.475F, -2.49181F,
                            2.5F, -2.67819F, 2.525F, -2.85723F, 2.55F, -3.02843F, 2.575F, -3.19133F,
                            2.6F, -3.34549F, 2.625F, -3.49048F, 2.65F, -3.6259F, 2.675F, -3.75138F,
                            2.7F, -3.86658F, 2.725F, -3.97118F, 2.75F, -4.0649F, 2.775F, -4.14747F,
                            2.8F, -4.21868F, 2.825F, -4.27832F, 2.85F, -4.32624F, 2.875F, -4.3623F,
                            2.9F, -4.3864F, 2.925F, -4.39848F, 2.95F, -4.3985F, 2.975F, -4.38646F,
                            3.0F, -4.36242F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_4,
                    curve(0.0F, -0.65956F, 0.025F, -0.67241F, 0.05F, -0.68342F, 0.075F, -0.69256F,
                            0.1F, -0.69979F, 0.125F, -0.70511F, 0.15F, -0.7085F, 0.175F, -0.70994F,
                            0.2F, -0.70944F, 0.225F, -0.707F, 0.25F, -0.70261F, 0.275F, -0.6963F,
                            0.3F, -0.68808F, 0.325F, -0.67798F, 0.35F, -0.66602F, 0.375F, -0.65223F,
                            0.4F, -0.63665F, 0.425F, -0.61933F, 0.45F, -0.60031F, 0.475F, -0.57965F,
                            0.5F, -0.5574F, 0.525F, -0.53362F, 0.55F, -0.50837F, 0.575F, -0.48174F,
                            0.6F, -0.45378F, 0.625F, -0.42458F, 0.65F, -0.39422F, 0.675F, -0.36277F,
                            0.7F, -0.33033F, 0.725F, -0.29699F, 0.75F, -0.26283F, 0.775F, -0.22795F,
                            0.8F, -0.19245F, 0.825F, -0.15641F, 0.85F, -0.11995F, 0.875F, -0.08317F,
                            0.9F, -0.04615F, 0.925F, -0.00901F, 0.95F, 0.02816F, 0.975F, 0.06525F,
                            1.0F, 0.10216F, 1.025F, 0.1388F, 1.05F, 0.17505F, 1.075F, 0.21082F,
                            1.1F, 0.24601F, 1.125F, 0.28053F, 1.15F, 0.31428F, 1.175F, 0.34717F,
                            1.2F, 0.37911F, 1.225F, 0.41001F, 1.25F, 0.43978F, 1.275F, 0.46835F,
                            1.3F, 0.49564F, 1.325F, 0.52156F, 1.35F, 0.54606F, 1.375F, 0.56906F,
                            1.4F, 0.5905F, 1.425F, 0.61033F, 1.45F, 0.62848F, 1.475F, 0.6449F,
                            1.5F, 0.65956F, 1.525F, 0.67241F, 1.55F, 0.68342F, 1.575F, 0.69256F,
                            1.6F, 0.69979F, 1.625F, 0.70511F, 1.65F, 0.7085F, 1.675F, 0.70994F,
                            1.7F, 0.70944F, 1.725F, 0.707F, 1.75F, 0.70261F, 1.775F, 0.6963F,
                            1.8F, 0.68808F, 1.825F, 0.67798F, 1.85F, 0.66602F, 1.875F, 0.65223F,
                            1.9F, 0.63665F, 1.925F, 0.61933F, 1.95F, 0.60031F, 1.975F, 0.57965F,
                            2.0F, 0.5574F, 2.025F, 0.53362F, 2.05F, 0.50837F, 2.075F, 0.48174F,
                            2.1F, 0.45378F, 2.125F, 0.42458F, 2.15F, 0.39422F, 2.175F, 0.36277F,
                            2.2F, 0.33033F, 2.225F, 0.29699F, 2.25F, 0.26283F, 2.275F, 0.22795F,
                            2.3F, 0.19245F, 2.325F, 0.15641F, 2.35F, 0.11995F, 2.375F, 0.08317F,
                            2.4F, 0.04615F, 2.425F, 0.00901F, 2.45F, -0.02816F, 2.475F, -0.06525F,
                            2.5F, -0.10216F, 2.525F, -0.1388F, 2.55F, -0.17505F, 2.575F, -0.21082F,
                            2.6F, -0.24601F, 2.625F, -0.28053F, 2.65F, -0.31428F, 2.675F, -0.34717F,
                            2.7F, -0.37911F, 2.725F, -0.41001F, 2.75F, -0.43978F, 2.775F, -0.46835F,
                            2.8F, -0.49564F, 2.825F, -0.52156F, 2.85F, -0.54606F, 2.875F, -0.56906F,
                            2.9F, -0.5905F, 2.925F, -0.61033F, 2.95F, -0.62848F, 2.975F, -0.6449F,
                            3.0F, -0.65956F),
                    curve(0.0F, -4.57261F, 0.025F, -4.7263F, 0.05F, -4.86703F, 0.075F, -4.99443F,
                            0.1F, -5.10814F, 0.125F, -5.20784F, 0.15F, -5.29327F, 0.175F, -5.36419F,
                            0.2F, -5.42041F, 0.225F, -5.46178F, 0.25F, -5.48817F, 0.275F, -5.49952F,
                            0.3F, -5.49579F, 0.325F, -5.47701F, 0.35F, -5.4432F, 0.375F, -5.39449F,
                            0.4F, -5.33098F, 0.425F, -5.25286F, 0.45F, -5.16035F, 0.475F, -5.05369F,
                            0.5F, -4.93318F, 0.525F, -4.79914F, 0.55F, -4.65196F, 0.575F, -4.49202F,
                            0.6F, -4.31977F, 0.625F, -4.13568F, 0.65F, -3.94026F, 0.675F, -3.73403F,
                            0.7F, -3.51757F, 0.725F, -3.29147F, 0.75F, -3.05635F, 0.775F, -2.81284F,
                            0.8F, -2.56163F, 0.825F, -2.3034F, 0.85F, -2.03886F, 0.875F, -1.76872F,
                            0.9F, -1.49374F, 0.925F, -1.21467F, 0.95F, -0.93226F, 0.975F, -0.6473F,
                            1.0F, -0.36057F, 1.025F, -0.07285F, 1.05F, 0.21508F, 1.075F, 0.50241F,
                            1.1F, 0.78837F, 1.125F, 1.07216F, 1.15F, 1.35302F, 1.175F, 1.63016F,
                            1.2F, 1.90284F, 1.225F, 2.17031F, 1.25F, 2.43182F, 1.275F, 2.68667F,
                            1.3F, 2.93416F, 1.325F, 3.1736F, 1.35F, 3.40435F, 1.375F, 3.62576F,
                            1.4F, 3.83724F, 1.425F, 4.03819F, 1.45F, 4.22808F, 1.475F, 4.40639F,
                            1.5F, 4.57261F, 1.525F, 4.7263F, 1.55F, 4.86703F, 1.575F, 4.99443F,
                            1.6F, 5.10813F, 1.625F, 5.20784F, 1.65F, 5.29327F, 1.675F, 5.36419F,
                            1.7F, 5.42041F, 1.725F, 5.46178F, 1.75F, 5.48817F, 1.775F, 5.49952F,
                            1.8F, 5.49579F, 1.825F, 5.47701F, 1.85F, 5.4432F, 1.875F, 5.39449F,
                            1.9F, 5.33097F, 1.925F, 5.25286F, 1.95F, 5.16035F, 1.975F, 5.05369F,
                            2.0F, 4.93318F, 2.025F, 4.79914F, 2.05F, 4.65196F, 2.075F, 4.49202F,
                            2.1F, 4.31977F, 2.125F, 4.13568F, 2.15F, 3.94026F, 2.175F, 3.73403F,
                            2.2F, 3.51757F, 2.225F, 3.29147F, 2.25F, 3.05635F, 2.275F, 2.81284F,
                            2.3F, 2.56163F, 2.325F, 2.3034F, 2.35F, 2.03886F, 2.375F, 1.76872F,
                            2.4F, 1.49374F, 2.425F, 1.21467F, 2.45F, 0.93226F, 2.475F, 0.6473F,
                            2.5F, 0.36057F, 2.525F, 0.07285F, 2.55F, -0.21508F, 2.575F, -0.50241F,
                            2.6F, -0.78837F, 2.625F, -1.07216F, 2.65F, -1.35303F, 2.675F, -1.63016F,
                            2.7F, -1.90284F, 2.725F, -2.17031F, 2.75F, -2.43182F, 2.775F, -2.68667F,
                            2.8F, -2.93416F, 2.825F, -3.1736F, 2.85F, -3.40435F, 2.875F, -3.62577F,
                            2.9F, -3.83724F, 2.925F, -4.03819F, 2.95F, -4.22808F, 2.975F, -4.40639F,
                            3.0F, -4.57261F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_TIP,
                    curve(0.0F, -0.42787F, 0.025F, -0.4645F, 0.05F, -0.49986F, 0.075F, -0.53386F,
                            0.1F, -0.56639F, 0.125F, -0.59736F, 0.15F, -0.6267F, 0.175F, -0.65433F,
                            0.2F, -0.68015F, 0.225F, -0.70412F, 0.25F, -0.72615F, 0.275F, -0.7462F,
                            0.3F, -0.76419F, 0.325F, -0.7801F, 0.35F, -0.79386F, 0.375F, -0.80545F,
                            0.4F, -0.81484F, 0.425F, -0.82198F, 0.45F, -0.82688F, 0.475F, -0.82951F,
                            0.5F, -0.82987F, 0.525F, -0.82795F, 0.55F, -0.82376F, 0.575F, -0.81731F,
                            0.6F, -0.80863F, 0.625F, -0.79772F, 0.65F, -0.78463F, 0.675F, -0.76939F,
                            0.7F, -0.75205F, 0.725F, -0.73264F, 0.75F, -0.71122F, 0.775F, -0.68785F,
                            0.8F, -0.6626F, 0.825F, -0.63553F, 0.85F, -0.60672F, 0.875F, -0.57624F,
                            0.9F, -0.54419F, 0.925F, -0.51065F, 0.95F, -0.4757F, 0.975F, -0.43945F,
                            1.0F, -0.402F, 1.025F, -0.36344F, 1.05F, -0.32389F, 1.075F, -0.28346F,
                            1.1F, -0.24224F, 1.125F, -0.20036F, 1.15F, -0.15793F, 1.175F, -0.11507F,
                            1.2F, -0.07189F, 1.225F, -0.02852F, 1.25F, 0.01493F, 1.275F, 0.05835F,
                            1.3F, 0.1016F, 1.325F, 0.14457F, 1.35F, 0.18715F, 1.375F, 0.22921F,
                            1.4F, 0.27065F, 1.425F, 0.31134F, 1.45F, 0.35118F, 1.475F, 0.39006F,
                            1.5F, 0.42787F, 1.525F, 0.4645F, 1.55F, 0.49986F, 1.575F, 0.53386F,
                            1.6F, 0.56639F, 1.625F, 0.59736F, 1.65F, 0.6267F, 1.675F, 0.65433F,
                            1.7F, 0.68015F, 1.725F, 0.70412F, 1.75F, 0.72615F, 1.775F, 0.7462F,
                            1.8F, 0.76419F, 1.825F, 0.7801F, 1.85F, 0.79386F, 1.875F, 0.80545F,
                            1.9F, 0.81484F, 1.925F, 0.82198F, 1.95F, 0.82688F, 1.975F, 0.82951F,
                            2.0F, 0.82987F, 2.025F, 0.82795F, 2.05F, 0.82376F, 2.075F, 0.81731F,
                            2.1F, 0.80863F, 2.125F, 0.79772F, 2.15F, 0.78463F, 2.175F, 0.76939F,
                            2.2F, 0.75205F, 2.225F, 0.73264F, 2.25F, 0.71122F, 2.275F, 0.68785F,
                            2.3F, 0.6626F, 2.325F, 0.63553F, 2.35F, 0.60672F, 2.375F, 0.57624F,
                            2.4F, 0.54419F, 2.425F, 0.51065F, 2.45F, 0.4757F, 2.475F, 0.43945F,
                            2.5F, 0.402F, 2.525F, 0.36344F, 2.55F, 0.32389F, 2.575F, 0.28346F,
                            2.6F, 0.24224F, 2.625F, 0.20036F, 2.65F, 0.15793F, 2.675F, 0.11507F,
                            2.7F, 0.07189F, 2.725F, 0.02852F, 2.75F, -0.01493F, 2.775F, -0.05835F,
                            2.8F, -0.1016F, 2.825F, -0.14457F, 2.85F, -0.18715F, 2.875F, -0.22921F,
                            2.9F, -0.27065F, 2.925F, -0.31134F, 2.95F, -0.35118F, 2.975F, -0.39006F,
                            3.0F, -0.42787F),
                    curve(0.0F, -1.70689F, 0.025F, -2.03821F, 0.05F, -2.36395F, 0.075F, -2.68322F,
                            0.1F, -2.99512F, 0.125F, -3.29882F, 0.15F, -3.59347F, 0.175F, -3.87828F,
                            0.2F, -4.15245F, 0.225F, -4.41525F, 0.25F, -4.66594F, 0.275F, -4.90384F,
                            0.3F, -5.1283F, 0.325F, -5.33871F, 0.35F, -5.53448F, 0.375F, -5.71509F,
                            0.4F, -5.88002F, 0.425F, -6.02884F, 0.45F, -6.16114F, 0.475F, -6.27655F,
                            0.5F, -6.37476F, 0.525F, -6.45549F, 0.55F, -6.51853F, 0.575F, -6.5637F,
                            0.6F, -6.59088F, 0.625F, -6.60001F, 0.65F, -6.59103F, 0.675F, -6.56399F,
                            0.7F, -6.51896F, 0.725F, -6.45606F, 0.75F, -6.37546F, 0.775F, -6.27739F,
                            0.8F, -6.16212F, 0.825F, -6.02995F, 0.85F, -5.88126F, 0.875F, -5.71645F,
                            0.9F, -5.53597F, 0.925F, -5.34031F, 0.95F, -5.13002F, 0.975F, -4.90567F,
                            1.0F, -4.66787F, 1.025F, -4.41728F, 1.05F, -4.15457F, 1.075F, -3.88049F,
                            1.1F, -3.59576F, 1.125F, -3.30118F, 1.15F, -2.99755F, 1.175F, -2.68572F,
                            1.2F, -2.3665F, 1.225F, -2.04081F, 1.25F, -1.70952F, 1.275F, -1.37355F,
                            1.3F, -1.03382F, 1.325F, -0.69124F, 1.35F, -0.34678F, 1.375F, -0.00136F,
                            1.4F, 0.34405F, 1.425F, 0.68853F, 1.45F, 1.03112F, 1.475F, 1.37088F,
                            1.5F, 1.70689F, 1.525F, 2.03821F, 1.55F, 2.36395F, 1.575F, 2.68322F,
                            1.6F, 2.99512F, 1.625F, 3.29882F, 1.65F, 3.59347F, 1.675F, 3.87828F,
                            1.7F, 4.15245F, 1.725F, 4.41525F, 1.75F, 4.66594F, 1.775F, 4.90384F,
                            1.8F, 5.1283F, 1.825F, 5.33871F, 1.85F, 5.53448F, 1.875F, 5.71509F,
                            1.9F, 5.88002F, 1.925F, 6.02884F, 1.95F, 6.16114F, 1.975F, 6.27655F,
                            2.0F, 6.37476F, 2.025F, 6.45549F, 2.05F, 6.51853F, 2.075F, 6.5637F,
                            2.1F, 6.59088F, 2.125F, 6.6F, 2.15F, 6.59102F, 2.175F, 6.56399F,
                            2.2F, 6.51896F, 2.225F, 6.45606F, 2.25F, 6.37546F, 2.275F, 6.27739F,
                            2.3F, 6.16212F, 2.325F, 6.02995F, 2.35F, 5.88126F, 2.375F, 5.71645F,
                            2.4F, 5.53597F, 2.425F, 5.34031F, 2.45F, 5.13002F, 2.475F, 4.90567F,
                            2.5F, 4.66787F, 2.525F, 4.41728F, 2.55F, 4.15457F, 2.575F, 3.88049F,
                            2.6F, 3.59576F, 2.625F, 3.30118F, 2.65F, 2.99755F, 2.675F, 2.68571F,
                            2.7F, 2.3665F, 2.725F, 2.04081F, 2.75F, 1.70952F, 2.775F, 1.37355F,
                            2.8F, 1.03382F, 2.825F, 0.69124F, 2.85F, 0.34678F, 2.875F, 0.00136F,
                            2.9F, -0.34405F, 2.925F, -0.68853F, 2.95F, -1.03112F, 2.975F, -1.37088F,
                            3.0F, -1.70689F),
                    ZERO_CURVE,
                    ZERO_CURVE));
    }

    private static SkeletonAnimation createWalkSkeleton() {
        return animation(1.5F, true,
            bone(ModelBone.BODY_CORE,
                    ZERO_CURVE,
                    ZERO_CURVE,
                    curve(0.0F, -0.0F, 0.025F, 0.00306F, 0.05F, 0.0121F, 0.075F, 0.02674F,
                            0.1F, 0.04632F, 0.125F, 0.07F, 0.15F, 0.09674F, 0.175F, 0.12537F,
                            0.2F, 0.15463F, 0.225F, 0.18326F, 0.25F, 0.21F, 0.275F, 0.23368F,
                            0.3F, 0.25326F, 0.325F, 0.2679F, 0.35F, 0.27694F, 0.375F, 0.28F,
                            0.4F, 0.27694F, 0.425F, 0.2679F, 0.45F, 0.25326F, 0.475F, 0.23368F,
                            0.5F, 0.21F, 0.525F, 0.18326F, 0.55F, 0.15463F, 0.575F, 0.12537F,
                            0.6F, 0.09674F, 0.625F, 0.07F, 0.65F, 0.04632F, 0.675F, 0.02674F,
                            0.7F, 0.0121F, 0.725F, 0.00306F, 0.75F, -0.0F, 0.775F, 0.00306F,
                            0.8F, 0.0121F, 0.825F, 0.02674F, 0.85F, 0.04632F, 0.875F, 0.07F,
                            0.9F, 0.09674F, 0.925F, 0.12537F, 0.95F, 0.15463F, 0.975F, 0.18326F,
                            1.0F, 0.21F, 1.025F, 0.23368F, 1.05F, 0.25326F, 1.075F, 0.2679F,
                            1.1F, 0.27694F, 1.125F, 0.28F, 1.15F, 0.27694F, 1.175F, 0.2679F,
                            1.2F, 0.25326F, 1.225F, 0.23368F, 1.25F, 0.21F, 1.275F, 0.18326F,
                            1.3F, 0.15463F, 1.325F, 0.12537F, 1.35F, 0.09674F, 1.375F, 0.07F,
                            1.4F, 0.04632F, 1.425F, 0.02674F, 1.45F, 0.0121F, 1.475F, 0.00306F,
                            1.5F, -0.0F),
                    ZERO_CURVE),
            bone(ModelBone.NECK_1,
                    curve(0.0F, 0.23642F, 0.025F, 0.31501F, 0.05F, 0.39015F, 0.075F, 0.46102F,
                            0.1F, 0.52683F, 0.125F, 0.58688F, 0.15F, 0.64049F, 0.175F, 0.68709F,
                            0.2F, 0.72616F, 0.225F, 0.75727F, 0.25F, 0.78008F, 0.275F, 0.79435F,
                            0.3F, 0.79992F, 0.325F, 0.79672F, 0.35F, 0.78479F, 0.375F, 0.76427F,
                            0.4F, 0.73537F, 0.425F, 0.69841F, 0.45F, 0.65381F, 0.475F, 0.60204F,
                            0.5F, 0.54367F, 0.525F, 0.47934F, 0.55F, 0.40977F, 0.575F, 0.3357F,
                            0.6F, 0.25796F, 0.625F, 0.17739F, 0.65F, 0.09488F, 0.675F, 0.01133F,
                            0.7F, -0.07235F, 0.725F, -0.15523F, 0.75F, -0.23642F, 0.775F, -0.31501F,
                            0.8F, -0.39015F, 0.825F, -0.46102F, 0.85F, -0.52683F, 0.875F, -0.58688F,
                            0.9F, -0.64049F, 0.925F, -0.68709F, 0.95F, -0.72616F, 0.975F, -0.75727F,
                            1.0F, -0.78008F, 1.025F, -0.79435F, 1.05F, -0.79992F, 1.075F, -0.79672F,
                            1.1F, -0.78479F, 1.125F, -0.76427F, 1.15F, -0.73537F, 1.175F, -0.69841F,
                            1.2F, -0.65381F, 1.225F, -0.60204F, 1.25F, -0.54367F, 1.275F, -0.47934F,
                            1.3F, -0.40977F, 1.325F, -0.3357F, 1.35F, -0.25796F, 1.375F, -0.17739F,
                            1.4F, -0.09488F, 1.425F, -0.01133F, 1.45F, 0.07235F, 1.475F, 0.15523F,
                            1.5F, 0.23642F),
                    curve(0.0F, 0.86297F, 0.025F, 1.02336F, 0.05F, 1.17254F, 0.075F, 1.30887F,
                            0.1F, 1.43086F, 0.125F, 1.53717F, 0.15F, 1.62665F, 0.175F, 1.6983F,
                            0.2F, 1.75134F, 0.225F, 1.7852F, 0.25F, 1.7995F, 0.275F, 1.79408F,
                            0.3F, 1.76901F, 0.325F, 1.72455F, 0.35F, 1.6612F, 0.375F, 1.57965F,
                            0.4F, 1.48079F, 0.425F, 1.36571F, 0.45F, 1.23566F, 0.475F, 1.09208F,
                            0.5F, 0.93653F, 0.525F, 0.77072F, 0.55F, 0.59647F, 0.575F, 0.41568F,
                            0.6F, 0.23034F, 0.625F, 0.04247F, 0.65F, -0.14586F, 0.675F, -0.33259F,
                            0.7F, -0.51568F, 0.725F, -0.69312F, 0.75F, -0.86297F, 0.775F, -1.02336F,
                            0.8F, -1.17254F, 0.825F, -1.30887F, 0.85F, -1.43086F, 0.875F, -1.53717F,
                            0.9F, -1.62665F, 0.925F, -1.6983F, 0.95F, -1.75134F, 0.975F, -1.7852F,
                            1.0F, -1.7995F, 1.025F, -1.79408F, 1.05F, -1.76901F, 1.075F, -1.72455F,
                            1.1F, -1.6612F, 1.125F, -1.57965F, 1.15F, -1.48079F, 1.175F, -1.36571F,
                            1.2F, -1.23566F, 1.225F, -1.09208F, 1.25F, -0.93653F, 1.275F, -0.77072F,
                            1.3F, -0.59647F, 1.325F, -0.41568F, 1.35F, -0.23034F, 1.375F, -0.04247F,
                            1.4F, 0.14586F, 1.425F, 0.33259F, 1.45F, 0.51568F, 1.475F, 0.69312F,
                            1.5F, 0.86297F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.NECK_2,
                    curve(0.0F, -0.4673F, 0.025F, -0.34921F, 0.05F, -0.22729F, 0.075F, -0.10289F,
                            0.1F, 0.02265F, 0.125F, 0.14793F, 0.15F, 0.2716F, 0.175F, 0.3923F,
                            0.2F, 0.50869F, 0.225F, 0.61951F, 0.25F, 0.72353F, 0.275F, 0.81965F,
                            0.3F, 0.90677F, 0.325F, 0.98395F, 0.35F, 1.05037F, 0.375F, 1.10526F,
                            0.4F, 1.14806F, 0.425F, 1.17828F, 0.45F, 1.19558F, 0.475F, 1.19978F,
                            0.5F, 1.19085F, 0.525F, 1.16886F, 0.55F, 1.13405F, 0.575F, 1.08685F,
                            0.6F, 1.02771F, 0.625F, 0.95732F, 0.65F, 0.87646F, 0.675F, 0.78598F,
                            0.7F, 0.68689F, 0.725F, 0.58027F, 0.75F, 0.4673F, 0.775F, 0.34921F,
                            0.8F, 0.22729F, 0.825F, 0.10288F, 0.85F, -0.02265F, 0.875F, -0.14794F,
                            0.9F, -0.27162F, 0.925F, -0.3923F, 0.95F, -0.50869F, 0.975F, -0.61951F,
                            1.0F, -0.72354F, 1.025F, -0.81965F, 1.05F, -0.90678F, 1.075F, -0.98396F,
                            1.1F, -1.05037F, 1.125F, -1.10527F, 1.15F, -1.14806F, 1.175F, -1.17829F,
                            1.2F, -1.19559F, 1.225F, -1.19979F, 1.25F, -1.19085F, 1.275F, -1.16887F,
                            1.3F, -1.13407F, 1.325F, -1.08685F, 1.35F, -1.02772F, 1.375F, -0.95733F,
                            1.4F, -0.87647F, 1.425F, -0.78598F, 1.45F, -0.68689F, 1.475F, -0.58027F,
                            1.5F, -0.4673F),
                    curve(0.0F, -0.49667F, 0.025F, -0.23784F, 0.05F, 0.0236F, 0.075F, 0.28478F,
                            0.1F, 0.54284F, 0.125F, 0.79495F, 0.15F, 1.03835F, 0.175F, 1.27038F,
                            0.2F, 1.48849F, 0.225F, 1.69029F, 0.25F, 1.87357F, 0.275F, 2.03632F,
                            0.3F, 2.17677F, 0.325F, 2.29336F, 0.35F, 2.38483F, 0.375F, 2.45017F,
                            0.4F, 2.48866F, 0.425F, 2.49989F, 0.45F, 2.48373F, 0.475F, 2.44035F,
                            0.5F, 2.37024F, 0.525F, 2.27416F, 0.55F, 2.15317F, 0.575F, 2.00858F,
                            0.6F, 1.84199F, 0.625F, 1.65521F, 0.65F, 1.45031F, 0.675F, 1.22951F,
                            0.7F, 0.99524F, 0.725F, 0.75006F, 0.75F, 0.49667F, 0.775F, 0.23784F,
                            0.8F, -0.0236F, 0.825F, -0.28478F, 0.85F, -0.54284F, 0.875F, -0.79495F,
                            0.9F, -1.03835F, 0.925F, -1.27038F, 0.95F, -1.48849F, 0.975F, -1.69029F,
                            1.0F, -1.87357F, 1.025F, -2.03632F, 1.05F, -2.17677F, 1.075F, -2.29336F,
                            1.1F, -2.38483F, 1.125F, -2.45017F, 1.15F, -2.48866F, 1.175F, -2.49989F,
                            1.2F, -2.48373F, 1.225F, -2.44035F, 1.25F, -2.37024F, 1.275F, -2.27416F,
                            1.3F, -2.15317F, 1.325F, -2.00858F, 1.35F, -1.84199F, 1.375F, -1.65521F,
                            1.4F, -1.45031F, 1.425F, -1.22951F, 1.45F, -0.99524F, 1.475F, -0.75006F,
                            1.5F, -0.49667F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.HEAD,
                    curve(0.0F, 0.35048F, 0.025F, 0.26191F, 0.05F, 0.17047F, 0.075F, 0.07716F,
                            0.1F, -0.01699F, 0.125F, -0.11096F, 0.15F, -0.20371F, 0.175F, -0.29422F,
                            0.2F, -0.38152F, 0.225F, -0.46462F, 0.25F, -0.54266F, 0.275F, -0.61474F,
                            0.3F, -0.68008F, 0.325F, -0.73797F, 0.35F, -0.78777F, 0.375F, -0.82895F,
                            0.4F, -0.86105F, 0.425F, -0.88371F, 0.45F, -0.89669F, 0.475F, -0.89984F,
                            0.5F, -0.89313F, 0.525F, -0.87664F, 0.55F, -0.85055F, 0.575F, -0.81513F,
                            0.6F, -0.77078F, 0.625F, -0.71801F, 0.65F, -0.65734F, 0.675F, -0.58948F,
                            0.7F, -0.51516F, 0.725F, -0.43521F, 0.75F, -0.35048F, 0.775F, -0.26191F,
                            0.8F, -0.17047F, 0.825F, -0.07716F, 0.85F, 0.01699F, 0.875F, 0.11097F,
                            0.9F, 0.20371F, 0.925F, 0.29422F, 0.95F, 0.38152F, 0.975F, 0.46463F,
                            1.0F, 0.54266F, 1.025F, 0.61474F, 1.05F, 0.68008F, 1.075F, 0.73798F,
                            1.1F, 0.78778F, 1.125F, 0.82895F, 1.15F, 0.86105F, 1.175F, 0.88371F,
                            1.2F, 0.89669F, 1.225F, 0.89984F, 1.25F, 0.89313F, 1.275F, 0.87664F,
                            1.3F, 0.85055F, 1.325F, 0.81513F, 1.35F, 0.77079F, 1.375F, 0.718F,
                            1.4F, 0.65733F, 1.425F, 0.58948F, 1.45F, 0.51517F, 1.475F, 0.43521F,
                            1.5F, 0.35048F),
                    curve(0.0F, 0.43707F, 0.025F, 0.2093F, 0.05F, -0.02077F, 0.075F, -0.25061F,
                            0.1F, -0.4777F, 0.125F, -0.69956F, 0.15F, -0.91375F, 0.175F, -1.11794F,
                            0.2F, -1.30987F, 0.225F, -1.48745F, 0.25F, -1.64874F, 0.275F, -1.79196F,
                            0.3F, -1.91555F, 0.325F, -2.01816F, 0.35F, -2.09865F, 0.375F, -2.15615F,
                            0.4F, -2.19002F, 0.425F, -2.1999F, 0.45F, -2.18568F, 0.475F, -2.14751F,
                            0.5F, -2.08581F, 0.525F, -2.00126F, 0.55F, -1.89479F, 0.575F, -1.76755F,
                            0.6F, -1.62095F, 0.625F, -1.45659F, 0.65F, -1.27627F, 0.675F, -1.08197F,
                            0.7F, -0.87581F, 0.725F, -0.66006F, 0.75F, -0.43707F, 0.775F, -0.2093F,
                            0.8F, 0.02077F, 0.825F, 0.25061F, 0.85F, 0.4777F, 0.875F, 0.69956F,
                            0.9F, 0.91375F, 0.925F, 1.11794F, 0.95F, 1.30987F, 0.975F, 1.48745F,
                            1.0F, 1.64874F, 1.025F, 1.79196F, 1.05F, 1.91555F, 1.075F, 2.01816F,
                            1.1F, 2.09865F, 1.125F, 2.15615F, 1.15F, 2.19002F, 1.175F, 2.1999F,
                            1.2F, 2.18568F, 1.225F, 2.14751F, 1.25F, 2.08581F, 1.275F, 2.00126F,
                            1.3F, 1.89479F, 1.325F, 1.76755F, 1.35F, 1.62095F, 1.375F, 1.45659F,
                            1.4F, 1.27627F, 1.425F, 1.08197F, 1.45F, 0.87581F, 1.475F, 0.66006F,
                            1.5F, 0.43707F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.JAW,
                    curve(0.0F, 0.97731F, 0.025F, 1.03626F, 0.05F, 1.09261F, 0.075F, 1.14576F,
                            0.1F, 1.19512F, 0.125F, 1.24016F, 0.15F, 1.28037F, 0.175F, 1.31532F,
                            0.2F, 1.34462F, 0.225F, 1.36795F, 0.25F, 1.38506F, 0.275F, 1.39577F,
                            0.3F, 1.39994F, 0.325F, 1.39754F, 0.35F, 1.38861F, 0.375F, 1.3732F,
                            0.4F, 1.35153F, 0.425F, 1.32381F, 0.45F, 1.29035F, 0.475F, 1.25153F,
                            0.5F, 1.20775F, 0.525F, 1.15951F, 0.55F, 1.10733F, 0.575F, 1.05178F,
                            0.6F, 0.99347F, 0.625F, 0.93304F, 0.65F, 0.87116F, 0.675F, 0.8085F,
                            0.7F, 0.74574F, 0.725F, 0.68358F, 0.75F, 0.62269F, 0.775F, 0.56374F,
                            0.8F, 0.50739F, 0.825F, 0.45424F, 0.85F, 0.40488F, 0.875F, 0.35984F,
                            0.9F, 0.31963F, 0.925F, 0.28468F, 0.95F, 0.25538F, 0.975F, 0.23205F,
                            1.0F, 0.21494F, 1.025F, 0.20423F, 1.05F, 0.20006F, 1.075F, 0.20246F,
                            1.1F, 0.2114F, 1.125F, 0.2268F, 1.15F, 0.24847F, 1.175F, 0.27619F,
                            1.2F, 0.30965F, 1.225F, 0.34847F, 1.25F, 0.39225F, 1.275F, 0.44049F,
                            1.3F, 0.49267F, 1.325F, 0.54822F, 1.35F, 0.60653F, 1.375F, 0.66696F,
                            1.4F, 0.72884F, 1.425F, 0.7915F, 1.45F, 0.85426F, 1.475F, 0.91642F,
                            1.5F, 0.97731F),
                    ZERO_CURVE,
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_1,
                    curve(0.0F, -0.0F, 0.025F, 0.03658F, 0.05F, 0.07277F, 0.075F, 0.10816F,
                            0.1F, 0.14236F, 0.125F, 0.175F, 0.15F, 0.20572F, 0.175F, 0.2342F,
                            0.2F, 0.2601F, 0.225F, 0.28316F, 0.25F, 0.30311F, 0.275F, 0.31974F,
                            0.3F, 0.33287F, 0.325F, 0.34235F, 0.35F, 0.34808F, 0.375F, 0.35F,
                            0.4F, 0.34808F, 0.425F, 0.34235F, 0.45F, 0.33287F, 0.475F, 0.31974F,
                            0.5F, 0.30311F, 0.525F, 0.28316F, 0.55F, 0.2601F, 0.575F, 0.2342F,
                            0.6F, 0.20572F, 0.625F, 0.175F, 0.65F, 0.14236F, 0.675F, 0.10816F,
                            0.7F, 0.07277F, 0.725F, 0.03658F, 0.75F, -0.0F, 0.775F, -0.03658F,
                            0.8F, -0.07277F, 0.825F, -0.10816F, 0.85F, -0.14236F, 0.875F, -0.175F,
                            0.9F, -0.20572F, 0.925F, -0.2342F, 0.95F, -0.2601F, 0.975F, -0.28316F,
                            1.0F, -0.30311F, 1.025F, -0.31974F, 1.05F, -0.33287F, 1.075F, -0.34235F,
                            1.1F, -0.34808F, 1.125F, -0.35F, 1.15F, -0.34808F, 1.175F, -0.34235F,
                            1.2F, -0.33287F, 1.225F, -0.31974F, 1.25F, -0.30311F, 1.275F, -0.28316F,
                            1.3F, -0.2601F, 1.325F, -0.2342F, 1.35F, -0.20572F, 1.375F, -0.175F,
                            1.4F, -0.14236F, 1.425F, -0.10816F, 1.45F, -0.07277F, 1.475F, -0.03658F,
                            1.5F, -0.0F),
                    curve(0.0F, -0.0F, 0.025F, 0.36585F, 0.05F, 0.72769F, 0.075F, 1.08156F,
                            0.1F, 1.42358F, 0.125F, 1.75F, 0.15F, 2.05725F, 0.175F, 2.34196F,
                            0.2F, 2.60101F, 0.225F, 2.83156F, 0.25F, 3.03109F, 0.275F, 3.19741F,
                            0.3F, 3.3287F, 0.325F, 3.42352F, 0.35F, 3.48083F, 0.375F, 3.5F,
                            0.4F, 3.48083F, 0.425F, 3.42352F, 0.45F, 3.3287F, 0.475F, 3.19741F,
                            0.5F, 3.03109F, 0.525F, 2.83156F, 0.55F, 2.60101F, 0.575F, 2.34196F,
                            0.6F, 2.05725F, 0.625F, 1.75F, 0.65F, 1.42358F, 0.675F, 1.08156F,
                            0.7F, 0.72769F, 0.725F, 0.36585F, 0.75F, -0.0F, 0.775F, -0.36585F,
                            0.8F, -0.72769F, 0.825F, -1.08156F, 0.85F, -1.42358F, 0.875F, -1.75F,
                            0.9F, -2.05725F, 0.925F, -2.34196F, 0.95F, -2.60101F, 0.975F, -2.83156F,
                            1.0F, -3.03109F, 1.025F, -3.19741F, 1.05F, -3.3287F, 1.075F, -3.42352F,
                            1.1F, -3.48083F, 1.125F, -3.5F, 1.15F, -3.48083F, 1.175F, -3.42352F,
                            1.2F, -3.3287F, 1.225F, -3.19741F, 1.25F, -3.03109F, 1.275F, -2.83156F,
                            1.3F, -2.60101F, 1.325F, -2.34196F, 1.35F, -2.05725F, 1.375F, -1.75F,
                            1.4F, -1.42358F, 1.425F, -1.08156F, 1.45F, -0.72769F, 1.475F, -0.36585F,
                            1.5F, -0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_2,
                    curve(0.0F, -0.28444F, 0.025F, -0.24377F, 0.05F, -0.20043F, 0.075F, -0.15489F,
                            0.1F, -0.10766F, 0.125F, -0.05925F, 0.15F, -0.01019F, 0.175F, 0.03898F,
                            0.2F, 0.08773F, 0.225F, 0.13551F, 0.25F, 0.18181F, 0.275F, 0.22612F,
                            0.3F, 0.26795F, 0.325F, 0.30685F, 0.35F, 0.34238F, 0.375F, 0.37416F,
                            0.4F, 0.40184F, 0.425F, 0.42512F, 0.45F, 0.44374F, 0.475F, 0.4575F,
                            0.5F, 0.46625F, 0.525F, 0.46989F, 0.55F, 0.46838F, 0.575F, 0.46174F,
                            0.6F, 0.45004F, 0.625F, 0.43341F, 0.65F, 0.41203F, 0.675F, 0.38614F,
                            0.7F, 0.35601F, 0.725F, 0.32199F, 0.75F, 0.28444F, 0.775F, 0.24377F,
                            0.8F, 0.20043F, 0.825F, 0.15489F, 0.85F, 0.10766F, 0.875F, 0.05925F,
                            0.9F, 0.01019F, 0.925F, -0.03898F, 0.95F, -0.08773F, 0.975F, -0.13551F,
                            1.0F, -0.18181F, 1.025F, -0.22612F, 1.05F, -0.26795F, 1.075F, -0.30685F,
                            1.1F, -0.34238F, 1.125F, -0.37416F, 1.15F, -0.40184F, 1.175F, -0.42512F,
                            1.2F, -0.44374F, 1.225F, -0.4575F, 1.25F, -0.46625F, 1.275F, -0.46989F,
                            1.3F, -0.46838F, 1.325F, -0.46174F, 1.35F, -0.45004F, 1.375F, -0.43341F,
                            1.4F, -0.41203F, 1.425F, -0.38614F, 1.45F, -0.35601F, 1.475F, -0.32199F,
                            1.5F, -0.28444F),
                    curve(0.0F, -3.03317F, 0.025F, -2.65506F, 0.05F, -2.24787F, 0.075F, -1.81604F,
                            0.1F, -1.36432F, 0.125F, -0.89765F, 0.15F, -0.42114F, 0.175F, 0.05997F,
                            0.2F, 0.54044F, 0.225F, 1.01498F, 0.25F, 1.4784F, 0.275F, 1.92562F,
                            0.3F, 2.35174F, 0.325F, 2.7521F, 0.35F, 3.12231F, 0.375F, 3.45831F,
                            0.4F, 3.75641F, 0.425F, 4.01337F, 0.45F, 4.22635F, 0.475F, 4.39302F,
                            0.5F, 4.51157F, 0.525F, 4.58068F, 0.55F, 4.59961F, 0.575F, 4.56814F,
                            0.6F, 4.48663F, 0.625F, 4.35596F, 0.65F, 4.17756F, 0.675F, 3.95339F,
                            0.7F, 3.68591F, 0.725F, 3.37804F, 0.75F, 3.03317F, 0.775F, 2.65506F,
                            0.8F, 2.24787F, 0.825F, 1.81604F, 0.85F, 1.36432F, 0.875F, 0.89765F,
                            0.9F, 0.42114F, 0.925F, -0.05997F, 0.95F, -0.54044F, 0.975F, -1.01498F,
                            1.0F, -1.4784F, 1.025F, -1.92562F, 1.05F, -2.35174F, 1.075F, -2.7521F,
                            1.1F, -3.12231F, 1.125F, -3.45831F, 1.15F, -3.75641F, 1.175F, -4.01337F,
                            1.2F, -4.22635F, 1.225F, -4.39302F, 1.25F, -4.51157F, 1.275F, -4.58068F,
                            1.3F, -4.59961F, 1.325F, -4.56814F, 1.35F, -4.48663F, 1.375F, -4.35596F,
                            1.4F, -4.17756F, 1.425F, -3.95339F, 1.45F, -3.68591F, 1.475F, -3.37804F,
                            1.5F, -3.03317F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_3,
                    curve(0.0F, -0.5685F, 0.025F, -0.54889F, 0.05F, -0.52326F, 0.075F, -0.4919F,
                            0.1F, -0.45516F, 0.125F, -0.41342F, 0.15F, -0.36716F, 0.175F, -0.31687F,
                            0.2F, -0.26311F, 0.225F, -0.20647F, 0.25F, -0.14757F, 0.275F, -0.08705F,
                            0.3F, -0.02558F, 0.325F, 0.03618F, 0.35F, 0.09754F, 0.375F, 0.15782F,
                            0.4F, 0.21638F, 0.425F, 0.27257F, 0.45F, 0.32578F, 0.475F, 0.37541F,
                            0.5F, 0.42093F, 0.525F, 0.46184F, 0.55F, 0.49769F, 0.575F, 0.52808F,
                            0.6F, 0.55269F, 0.625F, 0.57125F, 0.65F, 0.58354F, 0.675F, 0.58945F,
                            0.7F, 0.58889F, 0.725F, 0.58188F, 0.75F, 0.5685F, 0.775F, 0.54889F,
                            0.8F, 0.52326F, 0.825F, 0.4919F, 0.85F, 0.45516F, 0.875F, 0.41342F,
                            0.9F, 0.36716F, 0.925F, 0.31687F, 0.95F, 0.26311F, 0.975F, 0.20647F,
                            1.0F, 0.14757F, 1.025F, 0.08705F, 1.05F, 0.02558F, 1.075F, -0.03618F,
                            1.1F, -0.09754F, 1.125F, -0.15782F, 1.15F, -0.21638F, 1.175F, -0.27257F,
                            1.2F, -0.32578F, 1.225F, -0.37541F, 1.25F, -0.42093F, 1.275F, -0.46184F,
                            1.3F, -0.49769F, 1.325F, -0.52808F, 1.35F, -0.55269F, 1.375F, -0.57125F,
                            1.4F, -0.58354F, 1.425F, -0.58945F, 1.45F, -0.58889F, 1.475F, -0.58188F,
                            1.5F, -0.5685F),
                    curve(0.0F, -5.65131F, 0.025F, -5.54265F, 0.05F, -5.37324F, 0.075F, -5.14499F,
                            0.1F, -4.86036F, 0.125F, -4.52246F, 0.15F, -4.13504F, 0.175F, -3.7023F,
                            0.2F, -3.229F, 0.225F, -2.72032F, 0.25F, -2.18184F, 0.275F, -1.61945F,
                            0.3F, -1.03932F, 0.325F, -0.4478F, 0.35F, 0.14862F, 0.375F, 0.74342F,
                            0.4F, 1.33007F, 0.425F, 1.90214F, 0.45F, 2.45338F, 0.475F, 2.97774F,
                            0.5F, 3.46947F, 0.525F, 3.92319F, 0.55F, 4.33393F, 0.575F, 4.69719F,
                            0.6F, 5.00898F, 0.625F, 5.26589F, 0.65F, 5.46511F, 0.675F, 5.60445F,
                            0.7F, 5.68238F, 0.725F, 5.69806F, 0.75F, 5.65131F, 0.775F, 5.54265F,
                            0.8F, 5.37325F, 0.825F, 5.14499F, 0.85F, 4.86036F, 0.875F, 4.52247F,
                            0.9F, 4.13504F, 0.925F, 3.7023F, 0.95F, 3.229F, 0.975F, 2.72032F,
                            1.0F, 2.18184F, 1.025F, 1.61945F, 1.05F, 1.03932F, 1.075F, 0.4478F,
                            1.1F, -0.14862F, 1.125F, -0.74342F, 1.15F, -1.33007F, 1.175F, -1.90214F,
                            1.2F, -2.45338F, 1.225F, -2.97774F, 1.25F, -3.46947F, 1.275F, -3.92319F,
                            1.3F, -4.33393F, 1.325F, -4.69719F, 1.35F, -5.00898F, 1.375F, -5.26589F,
                            1.4F, -5.46511F, 1.425F, -5.60445F, 1.45F, -5.68238F, 1.475F, -5.69806F,
                            1.5F, -5.65131F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_4,
                    curve(0.0F, -0.65956F, 0.025F, -0.68342F, 0.05F, -0.69979F, 0.075F, -0.7085F,
                            0.1F, -0.70944F, 0.125F, -0.70261F, 0.15F, -0.68808F, 0.175F, -0.66602F,
                            0.2F, -0.63665F, 0.225F, -0.60031F, 0.25F, -0.5574F, 0.275F, -0.50837F,
                            0.3F, -0.45378F, 0.325F, -0.39422F, 0.35F, -0.33033F, 0.375F, -0.26283F,
                            0.4F, -0.19245F, 0.425F, -0.11995F, 0.45F, -0.04615F, 0.475F, 0.02816F,
                            0.5F, 0.10216F, 0.525F, 0.17505F, 0.55F, 0.24601F, 0.575F, 0.31428F,
                            0.6F, 0.37911F, 0.625F, 0.43978F, 0.65F, 0.49564F, 0.675F, 0.54606F,
                            0.7F, 0.5905F, 0.725F, 0.62848F, 0.75F, 0.65956F, 0.775F, 0.68342F,
                            0.8F, 0.69979F, 0.825F, 0.7085F, 0.85F, 0.70944F, 0.875F, 0.70261F,
                            0.9F, 0.68808F, 0.925F, 0.66602F, 0.95F, 0.63665F, 0.975F, 0.60031F,
                            1.0F, 0.5574F, 1.025F, 0.50837F, 1.05F, 0.45378F, 1.075F, 0.39422F,
                            1.1F, 0.33033F, 1.125F, 0.26283F, 1.15F, 0.19245F, 1.175F, 0.11995F,
                            1.2F, 0.04615F, 1.225F, -0.02816F, 1.25F, -0.10216F, 1.275F, -0.17505F,
                            1.3F, -0.24601F, 1.325F, -0.31428F, 1.35F, -0.37911F, 1.375F, -0.43978F,
                            1.4F, -0.49564F, 1.425F, -0.54606F, 1.45F, -0.5905F, 1.475F, -0.62848F,
                            1.5F, -0.65956F),
                    curve(0.0F, -5.65341F, 0.025F, -6.01742F, 0.05F, -6.31551F, 0.075F, -6.54441F,
                            0.1F, -6.70161F, 0.125F, -6.78537F, 0.15F, -6.7948F, 0.175F, -6.72978F,
                            0.2F, -6.59103F, 0.225F, -6.38007F, 0.25F, -6.0992F, 0.275F, -5.75151F,
                            0.3F, -5.34081F, 0.325F, -4.87159F, 0.35F, -4.349F, 0.375F, -3.77875F,
                            0.4F, -3.16711F, 0.425F, -2.52077F, 0.45F, -1.84681F, 0.475F, -1.15262F,
                            0.5F, -0.44579F, 0.525F, 0.26591F, 0.55F, 0.97471F, 0.575F, 1.67282F,
                            0.6F, 2.35261F, 0.625F, 3.00662F, 0.65F, 3.62769F, 0.675F, 4.20901F,
                            0.7F, 4.74422F, 0.725F, 5.22745F, 0.75F, 5.65341F, 0.775F, 6.01742F,
                            0.8F, 6.31551F, 0.825F, 6.54441F, 0.85F, 6.7016F, 0.875F, 6.78537F,
                            0.9F, 6.7948F, 0.925F, 6.72978F, 0.95F, 6.59103F, 0.975F, 6.38007F,
                            1.0F, 6.0992F, 1.025F, 5.75151F, 1.05F, 5.34081F, 1.075F, 4.87158F,
                            1.1F, 4.349F, 1.125F, 3.77875F, 1.15F, 3.16711F, 1.175F, 2.52077F,
                            1.2F, 1.84681F, 1.225F, 1.15262F, 1.25F, 0.44579F, 1.275F, -0.26591F,
                            1.3F, -0.97471F, 1.325F, -1.67282F, 1.35F, -2.35261F, 1.375F, -3.00662F,
                            1.4F, -3.62769F, 1.425F, -4.20901F, 1.45F, -4.74422F, 1.475F, -5.22746F,
                            1.5F, -5.65341F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_TIP,
                    curve(0.0F, -0.42787F, 0.025F, -0.49986F, 0.05F, -0.56639F, 0.075F, -0.6267F,
                            0.1F, -0.68015F, 0.125F, -0.72615F, 0.15F, -0.76419F, 0.175F, -0.79386F,
                            0.2F, -0.81484F, 0.225F, -0.82688F, 0.25F, -0.82987F, 0.275F, -0.82376F,
                            0.3F, -0.80863F, 0.325F, -0.78463F, 0.35F, -0.75205F, 0.375F, -0.71122F,
                            0.4F, -0.6626F, 0.425F, -0.60672F, 0.45F, -0.54419F, 0.475F, -0.4757F,
                            0.5F, -0.402F, 0.525F, -0.32389F, 0.55F, -0.24224F, 0.575F, -0.15793F,
                            0.6F, -0.07189F, 0.625F, 0.01493F, 0.65F, 0.1016F, 0.675F, 0.18715F,
                            0.7F, 0.27065F, 0.725F, 0.35118F, 0.75F, 0.42787F, 0.775F, 0.49986F,
                            0.8F, 0.56639F, 0.825F, 0.6267F, 0.85F, 0.68015F, 0.875F, 0.72615F,
                            0.9F, 0.76419F, 0.925F, 0.79386F, 0.95F, 0.81484F, 0.975F, 0.82688F,
                            1.0F, 0.82987F, 1.025F, 0.82376F, 1.05F, 0.80863F, 1.075F, 0.78463F,
                            1.1F, 0.75205F, 1.125F, 0.71122F, 1.15F, 0.6626F, 1.175F, 0.60672F,
                            1.2F, 0.54419F, 1.225F, 0.4757F, 1.25F, 0.402F, 1.275F, 0.32389F,
                            1.3F, 0.24224F, 1.325F, 0.15793F, 1.35F, 0.07189F, 1.375F, -0.01493F,
                            1.4F, -0.1016F, 1.425F, -0.18715F, 1.45F, -0.27065F, 1.475F, -0.35118F,
                            1.5F, -0.42787F),
                    curve(0.0F, -2.04309F, 0.025F, -2.82958F, 0.05F, -3.58507F, 0.075F, -4.30128F,
                            0.1F, -4.97036F, 0.125F, -5.58499F, 0.15F, -6.13843F, 0.175F, -6.62461F,
                            0.2F, -7.03821F, 0.225F, -7.3747F, 0.25F, -7.63039F, 0.275F, -7.80248F,
                            0.3F, -7.88909F, 0.325F, -7.88926F, 0.35F, -7.80299F, 0.375F, -7.63124F,
                            0.4F, -7.37587F, 0.425F, -7.03969F, 0.45F, -6.62639F, 0.475F, -6.14048F,
                            0.5F, -5.5873F, 0.525F, -4.9729F, 0.55F, -4.30402F, 0.575F, -3.58798F,
                            0.6F, -2.83263F, 0.625F, -2.04625F, 0.65F, -1.23745F, 0.675F, -0.41509F,
                            0.7F, 0.41182F, 0.725F, 1.23422F, 0.75F, 2.0431F, 0.775F, 2.82958F,
                            0.8F, 3.58508F, 0.825F, 4.30128F, 0.85F, 4.97036F, 0.875F, 5.58499F,
                            0.9F, 6.13843F, 0.925F, 6.62461F, 0.95F, 7.03821F, 0.975F, 7.37469F,
                            1.0F, 7.63039F, 1.025F, 7.80247F, 1.05F, 7.88909F, 1.075F, 7.88925F,
                            1.1F, 7.80299F, 1.125F, 7.63124F, 1.15F, 7.37587F, 1.175F, 7.03969F,
                            1.2F, 6.62639F, 1.225F, 6.14048F, 1.25F, 5.5873F, 1.275F, 4.9729F,
                            1.3F, 4.30402F, 1.325F, 3.58798F, 1.35F, 2.83263F, 1.375F, 2.04625F,
                            1.4F, 1.23745F, 1.425F, 0.41509F, 1.45F, -0.41182F, 1.475F, -1.23422F,
                            1.5F, -2.04309F),
                    ZERO_CURVE,
                    ZERO_CURVE));
    }

    private static SkeletonAnimation createBreathSkeleton() {
        return animation(2.6F, false,
            bone(ModelBone.NECK_1,
                    curve(0.0F, -0.0F, 0.025F, -0.05831F, 0.05F, -0.22157F, 0.075F, -0.4723F,
                            0.1F, -0.793F, 0.125F, -1.16618F, 0.15F, -1.57434F, 0.175F, -2.0F,
                            0.2F, -2.42566F, 0.225F, -2.83382F, 0.25F, -3.207F, 0.275F, -3.5277F,
                            0.3F, -3.77843F, 0.325F, -3.94169F, 0.35F, -4.0F, 0.375F, -3.65625F,
                            0.4F, -2.75F, 0.425F, -1.46875F, 0.45F, -0.0F, 0.475F, 1.46875F,
                            0.5F, 2.75F, 0.525F, 3.65625F, 0.55F, 4.0F, 0.575F, 4.0F,
                            0.6F, 4.0F, 0.625F, 4.0F, 0.65F, 4.0F, 0.675F, 4.0F,
                            0.7F, 4.0F, 0.725F, 4.0F, 0.75F, 4.0F, 0.775F, 4.0F,
                            0.8F, 4.0F, 0.825F, 4.0F, 0.85F, 4.0F, 0.875F, 4.0F,
                            0.9F, 4.0F, 0.925F, 4.0F, 0.95F, 4.0F, 0.975F, 4.0F,
                            1.0F, 4.0F, 1.025F, 4.0F, 1.05F, 4.0F, 1.075F, 4.0F,
                            1.1F, 4.0F, 1.125F, 4.0F, 1.15F, 4.0F, 1.175F, 4.0F,
                            1.2F, 4.0F, 1.225F, 4.0F, 1.25F, 4.0F, 1.275F, 4.0F,
                            1.3F, 4.0F, 1.325F, 4.0F, 1.35F, 4.0F, 1.375F, 4.0F,
                            1.4F, 4.0F, 1.425F, 4.0F, 1.45F, 4.0F, 1.475F, 4.0F,
                            1.5F, 4.0F, 1.525F, 4.0F, 1.55F, 4.0F, 1.575F, 4.0F,
                            1.6F, 4.0F, 1.625F, 4.0F, 1.65F, 4.0F, 1.675F, 4.0F,
                            1.7F, 4.0F, 1.725F, 4.0F, 1.75F, 4.0F, 1.775F, 4.0F,
                            1.8F, 4.0F, 1.825F, 4.0F, 1.85F, 4.0F, 1.875F, 4.0F,
                            1.9F, 4.0F, 1.925F, 3.98506F, 1.95F, 3.94169F, 1.975F, 3.87208F,
                            2.0F, 3.77843F, 2.025F, 3.6629F, 2.05F, 3.5277F, 2.075F, 3.375F,
                            2.1F, 3.207F, 2.125F, 3.02587F, 2.15F, 2.83382F, 2.175F, 2.63302F,
                            2.2F, 2.42566F, 2.225F, 2.21392F, 2.25F, 2.0F, 2.275F, 1.78608F,
                            2.3F, 1.57434F, 2.325F, 1.36698F, 2.35F, 1.16618F, 2.375F, 0.97413F,
                            2.4F, 0.793F, 2.425F, 0.625F, 2.45F, 0.4723F, 2.475F, 0.3371F,
                            2.5F, 0.22157F, 2.525F, 0.12792F, 2.55F, 0.05831F, 2.575F, 0.01494F,
                            2.6F, -0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.NECK_2,
                    curve(0.0F, 0.0F, 0.025F, -0.08746F, 0.05F, -0.33236F, 0.075F, -0.70845F,
                            0.1F, -1.1895F, 0.125F, -1.74927F, 0.15F, -2.36153F, 0.175F, -3.0F,
                            0.2F, -3.63848F, 0.225F, -4.25073F, 0.25F, -4.8105F, 0.275F, -5.29156F,
                            0.3F, -5.66764F, 0.325F, -5.91254F, 0.35F, -6.0F, 0.375F, -5.48437F,
                            0.4F, -4.12501F, 0.425F, -2.20313F, 0.45F, 0.0F, 0.475F, 2.20312F,
                            0.5F, 4.125F, 0.525F, 5.48437F, 0.55F, 6.0F, 0.575F, 6.0F,
                            0.6F, 6.0F, 0.625F, 6.0F, 0.65F, 6.0F, 0.675F, 6.0F,
                            0.7F, 6.0F, 0.725F, 6.0F, 0.75F, 6.0F, 0.775F, 6.0F,
                            0.8F, 6.0F, 0.825F, 6.0F, 0.85F, 6.0F, 0.875F, 6.0F,
                            0.9F, 6.0F, 0.925F, 6.0F, 0.95F, 6.0F, 0.975F, 6.0F,
                            1.0F, 6.0F, 1.025F, 6.0F, 1.05F, 6.0F, 1.075F, 6.0F,
                            1.1F, 6.0F, 1.125F, 6.0F, 1.15F, 6.0F, 1.175F, 6.0F,
                            1.2F, 6.0F, 1.225F, 6.0F, 1.25F, 6.0F, 1.275F, 6.0F,
                            1.3F, 6.0F, 1.325F, 6.0F, 1.35F, 6.0F, 1.375F, 6.0F,
                            1.4F, 6.0F, 1.425F, 6.0F, 1.45F, 6.0F, 1.475F, 6.0F,
                            1.5F, 6.0F, 1.525F, 6.0F, 1.55F, 6.0F, 1.575F, 6.0F,
                            1.6F, 6.0F, 1.625F, 6.0F, 1.65F, 6.0F, 1.675F, 6.0F,
                            1.7F, 6.0F, 1.725F, 6.0F, 1.75F, 6.0F, 1.775F, 6.0F,
                            1.8F, 6.0F, 1.825F, 6.0F, 1.85F, 6.0F, 1.875F, 6.0F,
                            1.9F, 6.0F, 1.925F, 5.97759F, 1.95F, 5.91254F, 1.975F, 5.80813F,
                            2.0F, 5.66764F, 2.025F, 5.49435F, 2.05F, 5.29154F, 2.075F, 5.0625F,
                            2.1F, 4.8105F, 2.125F, 4.53881F, 2.15F, 4.25073F, 2.175F, 3.94953F,
                            2.2F, 3.63848F, 2.225F, 3.32088F, 2.25F, 3.0F, 2.275F, 2.67911F,
                            2.3F, 2.36152F, 2.325F, 2.05047F, 2.35F, 1.74927F, 2.375F, 1.46119F,
                            2.4F, 1.1895F, 2.425F, 0.9375F, 2.45F, 0.70844F, 2.475F, 0.50564F,
                            2.5F, 0.33236F, 2.525F, 0.19186F, 2.55F, 0.08746F, 2.575F, 0.0224F,
                            2.6F, 0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.HEAD,
                    curve(0.0F, -0.0F, 0.025F, 0.05832F, 0.05F, 0.22157F, 0.075F, 0.4723F,
                            0.1F, 0.793F, 0.125F, 1.16618F, 0.15F, 1.57434F, 0.175F, 2.0F,
                            0.2F, 2.42566F, 0.225F, 2.83382F, 0.25F, 3.207F, 0.275F, 3.5277F,
                            0.3F, 3.77843F, 0.325F, 3.94169F, 0.35F, 4.0F, 0.375F, 3.65625F,
                            0.4F, 2.75F, 0.425F, 1.46875F, 0.45F, -0.0F, 0.475F, -1.46875F,
                            0.5F, -2.75F, 0.525F, -3.65625F, 0.55F, -4.0F, 0.575F, -4.0F,
                            0.6F, -4.0F, 0.625F, -4.0F, 0.65F, -4.0F, 0.675F, -4.0F,
                            0.7F, -4.0F, 0.725F, -4.0F, 0.75F, -4.0F, 0.775F, -4.0F,
                            0.8F, -4.0F, 0.825F, -4.0F, 0.85F, -4.0F, 0.875F, -4.0F,
                            0.9F, -4.0F, 0.925F, -4.0F, 0.95F, -4.0F, 0.975F, -4.0F,
                            1.0F, -4.0F, 1.025F, -4.0F, 1.05F, -4.0F, 1.075F, -4.0F,
                            1.1F, -4.0F, 1.125F, -4.0F, 1.15F, -4.0F, 1.175F, -4.0F,
                            1.2F, -4.0F, 1.225F, -4.0F, 1.25F, -4.0F, 1.275F, -4.0F,
                            1.3F, -4.0F, 1.325F, -4.0F, 1.35F, -4.0F, 1.375F, -4.0F,
                            1.4F, -4.0F, 1.425F, -4.0F, 1.45F, -4.0F, 1.475F, -4.0F,
                            1.5F, -4.0F, 1.525F, -4.0F, 1.55F, -4.0F, 1.575F, -4.0F,
                            1.6F, -4.0F, 1.625F, -4.0F, 1.65F, -4.0F, 1.675F, -4.0F,
                            1.7F, -4.0F, 1.725F, -4.0F, 1.75F, -4.0F, 1.775F, -4.0F,
                            1.8F, -4.0F, 1.825F, -4.0F, 1.85F, -4.0F, 1.875F, -4.0F,
                            1.9F, -4.0F, 1.925F, -3.98506F, 1.95F, -3.94169F, 1.975F, -3.87208F,
                            2.0F, -3.77843F, 2.025F, -3.6629F, 2.05F, -3.5277F, 2.075F, -3.375F,
                            2.1F, -3.207F, 2.125F, -3.02587F, 2.15F, -2.83382F, 2.175F, -2.63302F,
                            2.2F, -2.42566F, 2.225F, -2.21392F, 2.25F, -2.0F, 2.275F, -1.78608F,
                            2.3F, -1.57434F, 2.325F, -1.36698F, 2.35F, -1.16618F, 2.375F, -0.97413F,
                            2.4F, -0.79299F, 2.425F, -0.625F, 2.45F, -0.47229F, 2.475F, -0.33709F,
                            2.5F, -0.22157F, 2.525F, -0.12792F, 2.55F, -0.05831F, 2.575F, -0.01494F,
                            2.6F, -0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.JAW,
                    curve(0.0F, 0.0F, 0.025F, -0.0F, 0.05F, -0.0F, 0.075F, -0.0F,
                            0.1F, 0.0F, 0.125F, 0.0F, 0.15F, -0.0F, 0.175F, 0.0F,
                            0.2F, 0.0F, 0.225F, -0.0F, 0.25F, 0.0F, 0.275F, -0.0F,
                            0.3F, 0.0F, 0.325F, 0.0F, 0.35F, -0.0F, 0.375F, -0.0F,
                            0.4F, 0.0F, 0.425F, -0.0F, 0.45F, 0.0F, 0.475F, 8.8125F,
                            0.5F, 16.5F, 0.525F, 21.9375F, 0.55F, 24.0F, 0.575F, 24.0F,
                            0.6F, 24.0F, 0.625F, 24.0F, 0.65F, 24.0F, 0.675F, 24.0F,
                            0.7F, 24.0F, 0.725F, 24.0F, 0.75F, 24.0F, 0.775F, 24.0F,
                            0.8F, 24.0F, 0.825F, 24.0F, 0.85F, 24.0F, 0.875F, 24.0F,
                            0.9F, 24.0F, 0.925F, 24.0F, 0.95F, 24.0F, 0.975F, 24.0F,
                            1.0F, 24.0F, 1.025F, 24.0F, 1.05F, 24.0F, 1.075F, 24.0F,
                            1.1F, 24.0F, 1.125F, 24.0F, 1.15F, 24.0F, 1.175F, 24.0F,
                            1.2F, 24.0F, 1.225F, 24.0F, 1.25F, 24.0F, 1.275F, 24.0F,
                            1.3F, 24.0F, 1.325F, 24.0F, 1.35F, 24.0F, 1.375F, 24.0F,
                            1.4F, 24.0F, 1.425F, 24.0F, 1.45F, 24.0F, 1.475F, 24.0F,
                            1.5F, 24.0F, 1.525F, 24.0F, 1.55F, 24.0F, 1.575F, 24.0F,
                            1.6F, 24.0F, 1.625F, 24.0F, 1.65F, 24.0F, 1.675F, 24.0F,
                            1.7F, 24.0F, 1.725F, 24.0F, 1.75F, 24.0F, 1.775F, 24.0F,
                            1.8F, 24.0F, 1.825F, 24.0F, 1.85F, 24.0F, 1.875F, 24.0F,
                            1.9F, 24.0F, 1.925F, 23.91035F, 1.95F, 23.65015F, 1.975F, 23.23251F,
                            2.0F, 22.67055F, 2.025F, 21.97741F, 2.05F, 21.16618F, 2.075F, 20.25F,
                            2.1F, 19.24198F, 2.125F, 18.15525F, 2.15F, 17.00293F, 2.175F, 15.7981F,
                            2.2F, 14.55394F, 2.225F, 13.28353F, 2.25F, 12.0F, 2.275F, 10.71647F,
                            2.3F, 9.44606F, 2.325F, 8.2019F, 2.35F, 6.99708F, 2.375F, 5.84475F,
                            2.4F, 4.75802F, 2.425F, 3.75F, 2.45F, 2.83382F, 2.475F, 2.02259F,
                            2.5F, 1.32945F, 2.525F, 0.76749F, 2.55F, 0.34985F, 2.575F, 0.08965F,
                            2.6F, 0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_1,
                    ZERO_CURVE,
                    curve(0.0F, -0.0F, 0.025F, -0.00264F, 0.05F, -0.02003F, 0.075F, -0.06385F,
                            0.1F, -0.14233F, 0.125F, -0.26021F, 0.15F, -0.4187F, 0.175F, -0.61562F,
                            0.2F, -0.84544F, 0.225F, -1.09953F, 0.25F, -1.36634F, 0.275F, -1.6317F,
                            0.3F, -1.87917F, 0.325F, -2.0904F, 0.35F, -2.24553F, 0.375F, -2.1586F,
                            0.4F, -1.6974F, 0.425F, -0.9427F, 0.45F, -0.0F, 0.475F, 1.00451F,
                            0.5F, 1.92847F, 0.525F, 2.61802F, 0.55F, 2.91283F, 0.575F, 2.95086F,
                            0.6F, 2.97813F, 0.625F, 2.99453F, 0.65F, 3.0F, 0.675F, 2.99453F,
                            0.7F, 2.97813F, 0.725F, 2.95086F, 0.75F, 2.91283F, 0.775F, 2.86416F,
                            0.8F, 2.80505F, 0.825F, 2.7357F, 0.85F, 2.65637F, 0.875F, 2.56734F,
                            0.9F, 2.46895F, 0.925F, 2.36155F, 0.95F, 2.24553F, 0.975F, 2.12132F,
                            1.0F, 1.98937F, 1.025F, 1.85016F, 1.05F, 1.70419F, 1.075F, 1.55201F,
                            1.1F, 1.39417F, 1.125F, 1.23124F, 1.15F, 1.06381F, 1.175F, 0.89251F,
                            1.2F, 0.71795F, 1.225F, 0.54077F, 1.25F, 0.36161F, 1.275F, 0.18114F,
                            1.3F, -0.0F, 1.325F, -0.18114F, 1.35F, -0.36161F, 1.375F, -0.54077F,
                            1.4F, -0.71795F, 1.425F, -0.89251F, 1.45F, -1.06381F, 1.475F, -1.23124F,
                            1.5F, -1.39417F, 1.525F, -1.55201F, 1.55F, -1.70419F, 1.575F, -1.85016F,
                            1.6F, -1.98937F, 1.625F, -2.12132F, 1.65F, -2.24553F, 1.675F, -2.36155F,
                            1.7F, -2.46895F, 1.725F, -2.56734F, 1.75F, -2.65637F, 1.775F, -2.7357F,
                            1.8F, -2.80505F, 1.825F, -2.86416F, 1.85F, -2.91283F, 1.875F, -2.95086F,
                            1.9F, -2.97813F, 1.925F, -2.98334F, 1.95F, -2.95627F, 1.975F, -2.89877F,
                            2.0F, -2.81316F, 2.025F, -2.70218F, 2.05F, -2.56889F, 2.075F, -2.41664F,
                            2.1F, -2.24895F, 2.125F, -2.06947F, 2.15F, -1.88192F, 2.175F, -1.68997F,
                            2.2F, -1.49721F, 2.225F, -1.30707F, 2.25F, -1.12277F, 2.275F, -0.94721F,
                            2.3F, -0.78299F, 2.325F, -0.63228F, 2.35F, -0.49685F, 2.375F, -0.37796F,
                            2.4F, -0.2764F, 2.425F, -0.19238F, 2.45F, -0.12561F, 2.475F, -0.07522F,
                            2.5F, -0.03977F, 2.525F, -0.01729F, 2.55F, -0.00527F, 2.575F, -0.00068F,
                            2.6F, -0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_2,
                    ZERO_CURVE,
                    curve(0.0F, -0.0F, 0.025F, 0.02247F, 0.05F, 0.07662F, 0.075F, 0.14404F,
                            0.1F, 0.20859F, 0.125F, 0.25674F, 0.15F, 0.27781F, 0.175F, 0.26425F,
                            0.2F, 0.21178F, 0.225F, 0.11952F, 0.25F, -0.00999F, 0.275F, -0.17071F,
                            0.3F, -0.35326F, 0.325F, -0.54495F, 0.35F, -0.73004F, 0.375F, -0.82667F,
                            0.4F, -0.73937F, 0.425F, -0.45626F, 0.45F, -0.0F, 0.475F, 0.57379F,
                            0.5F, 1.17867F, 0.525F, 1.70011F, 0.55F, 1.99868F, 0.575F, 2.13011F,
                            0.6F, 2.25377F, 0.625F, 2.36921F, 0.65F, 2.47601F, 0.675F, 2.57377F,
                            0.7F, 2.66213F, 0.725F, 2.74079F, 0.75F, 2.80944F, 0.775F, 2.86784F,
                            0.8F, 2.91578F, 0.825F, 2.95308F, 0.85F, 2.9796F, 0.875F, 2.99525F,
                            0.9F, 2.99997F, 0.925F, 2.99375F, 0.95F, 2.9766F, 0.975F, 2.94859F,
                            1.0F, 2.90982F, 1.025F, 2.86043F, 1.05F, 2.80061F, 1.075F, 2.73056F,
                            1.1F, 2.65056F, 1.125F, 2.56088F, 1.15F, 2.46185F, 1.175F, 2.35385F,
                            1.2F, 2.23725F, 1.225F, 2.11249F, 1.25F, 1.98003F, 1.275F, 1.84033F,
                            1.3F, 1.69393F, 1.325F, 1.54134F, 1.35F, 1.38313F, 1.375F, 1.21987F,
                            1.4F, 1.05216F, 1.425F, 0.88061F, 1.45F, 0.70585F, 1.475F, 0.52851F,
                            1.5F, 0.34924F, 1.525F, 0.1687F, 1.55F, -0.01246F, 1.575F, -0.19357F,
                            1.6F, -0.37397F, 1.625F, -0.55301F, 1.65F, -0.73004F, 1.675F, -0.90439F,
                            1.7F, -1.07545F, 1.725F, -1.24259F, 1.75F, -1.40519F, 1.775F, -1.56266F,
                            1.8F, -1.71443F, 1.825F, -1.85995F, 1.85F, -1.99868F, 1.875F, -2.13011F,
                            1.9F, -2.25377F, 1.925F, -2.36036F, 1.95F, -2.43991F, 1.975F, -2.49146F,
                            2.0F, -2.51467F, 2.025F, -2.50981F, 2.05F, -2.47771F, 2.075F, -2.41974F,
                            2.1F, -2.33773F, 2.125F, -2.23391F, 2.15F, -2.11091F, 2.175F, -1.97164F,
                            2.2F, -1.81923F, 2.225F, -1.65698F, 2.25F, -1.4883F, 2.275F, -1.3166F,
                            2.3F, -1.14526F, 2.325F, -0.97754F, 2.35F, -0.8165F, 2.375F, -0.66498F,
                            2.4F, -0.52547F, 2.425F, -0.40014F, 2.45F, -0.29069F, 2.475F, -0.19837F,
                            2.5F, -0.12393F, 2.525F, -0.06756F, 2.55F, -0.02886F, 2.575F, -0.00687F,
                            2.6F, -0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_3,
                    ZERO_CURVE,
                    curve(0.0F, 0.0F, 0.025F, 0.03973F, 0.05F, 0.14651F, 0.075F, 0.30161F,
                            0.1F, 0.48665F, 0.125F, 0.684F, 0.15F, 0.87728F, 0.175F, 1.05182F,
                            0.2F, 1.19503F, 0.225F, 1.29681F, 0.25F, 1.34985F, 0.275F, 1.34991F,
                            0.3F, 1.29606F, 0.325F, 1.19086F, 0.35F, 1.04048F, 0.375F, 0.79404F,
                            0.4F, 0.47694F, 0.425F, 0.18956F, 0.45F, 0.0F, 0.475F, -0.05738F,
                            0.5F, 0.01713F, 0.525F, 0.1883F, 0.55F, 0.38633F, 0.575F, 0.56525F,
                            0.6F, 0.74211F, 0.625F, 0.91626F, 0.65F, 1.08707F, 0.675F, 1.25392F,
                            0.7F, 1.41618F, 0.725F, 1.57328F, 0.75F, 1.72464F, 0.775F, 1.8697F,
                            0.8F, 2.00795F, 0.825F, 2.13886F, 0.85F, 2.26198F, 0.875F, 2.37683F,
                            0.9F, 2.48302F, 0.925F, 2.58014F, 0.95F, 2.66785F, 0.975F, 2.74583F,
                            1.0F, 2.81379F, 1.025F, 2.87148F, 1.05F, 2.91869F, 1.075F, 2.95525F,
                            1.1F, 2.98103F, 1.125F, 2.99593F, 1.15F, 2.9999F, 1.175F, 2.99292F,
                            1.2F, 2.97502F, 1.225F, 2.94627F, 1.25F, 2.90676F, 1.275F, 2.85665F,
                            1.3F, 2.79612F, 1.325F, 2.72538F, 1.35F, 2.6447F, 1.375F, 2.55437F,
                            1.4F, 2.45471F, 1.425F, 2.3461F, 1.45F, 2.22893F, 1.475F, 2.10363F,
                            1.5F, 1.97065F, 1.525F, 1.83049F, 1.55F, 1.68363F, 1.575F, 1.53064F,
                            1.6F, 1.37206F, 1.625F, 1.20848F, 1.65F, 1.04048F, 1.675F, 0.86869F,
                            1.7F, 0.69373F, 1.725F, 0.51624F, 1.75F, 0.33686F, 1.775F, 0.15626F,
                            1.8F, -0.02491F, 1.825F, -0.206F, 1.85F, -0.38633F, 1.875F, -0.56525F,
                            1.9F, -0.74211F, 1.925F, -0.91284F, 1.95F, -1.07123F, 1.975F, -1.21382F,
                            2.0F, -1.33773F, 2.025F, -1.44069F, 2.05F, -1.521F, 2.075F, -1.57756F,
                            2.1F, -1.60987F, 2.125F, -1.61798F, 2.15F, -1.60251F, 2.175F, -1.56456F,
                            2.2F, -1.50574F, 2.225F, -1.42806F, 2.25F, -1.33393F, 2.275F, -1.22607F,
                            2.3F, -1.10747F, 2.325F, -0.9813F, 2.35F, -0.85093F, 2.375F, -0.7197F,
                            2.4F, -0.59099F, 2.425F, -0.46811F, 2.45F, -0.35422F, 2.475F, -0.25223F,
                            2.5F, -0.1648F, 2.525F, -0.09422F, 2.55F, -0.04237F, 2.575F, -0.01067F,
                            2.6F, 0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_4,
                    ZERO_CURVE,
                    curve(0.0F, -0.0F, 0.025F, 0.04311F, 0.05F, 0.16521F, 0.075F, 0.35382F,
                            0.1F, 0.59471F, 0.125F, 0.87231F, 0.15F, 1.17029F, 0.175F, 1.47195F,
                            0.2F, 1.76081F, 0.225F, 2.0211F, 0.25F, 2.23815F, 0.275F, 2.39897F,
                            0.3F, 2.49262F, 0.325F, 2.51067F, 0.35F, 2.44753F, 0.375F, 2.13737F,
                            0.4F, 1.52665F, 0.425F, 0.76916F, 0.45F, -0.0F, 0.475F, -0.6685F,
                            0.5F, -1.1504F, 0.525F, -1.38929F, 0.55F, -1.36097F, 0.575F, -1.19706F,
                            0.6F, -1.02879F, 0.625F, -0.85676F, 0.65F, -0.68161F, 0.675F, -0.50396F,
                            0.7F, -0.32448F, 0.725F, -0.14382F, 0.75F, 0.03737F, 0.775F, 0.21842F,
                            0.8F, 0.39868F, 0.825F, 0.57748F, 0.85F, 0.75418F, 0.875F, 0.92812F,
                            0.9F, 1.09867F, 0.925F, 1.26522F, 0.95F, 1.42715F, 0.975F, 1.58387F,
                            1.0F, 1.73482F, 1.025F, 1.87943F, 1.05F, 2.01719F, 1.075F, 2.14758F,
                            1.1F, 2.27014F, 1.125F, 2.38441F, 1.15F, 2.48999F, 1.175F, 2.58648F,
                            1.2F, 2.67353F, 1.225F, 2.75082F, 1.25F, 2.81808F, 1.275F, 2.87506F,
                            1.3F, 2.92154F, 1.325F, 2.95737F, 1.35F, 2.9824F, 1.375F, 2.99655F,
                            1.4F, 2.99977F, 1.425F, 2.99204F, 1.45F, 2.97339F, 1.475F, 2.94389F,
                            1.5F, 2.90366F, 1.525F, 2.85282F, 1.55F, 2.79159F, 1.575F, 2.72015F,
                            1.6F, 2.63879F, 1.625F, 2.54781F, 1.65F, 2.44753F, 1.675F, 2.33832F,
                            1.7F, 2.22058F, 1.725F, 2.09474F, 1.75F, 1.96124F, 1.775F, 1.8206F,
                            1.8F, 1.67331F, 1.825F, 1.51991F, 1.85F, 1.36097F, 1.875F, 1.19706F,
                            1.9F, 1.02879F, 1.925F, 0.85356F, 1.95F, 0.67167F, 1.975F, 0.48785F,
                            2.0F, 0.30651F, 2.025F, 0.1317F, 2.05F, -0.03296F, 2.075F, -0.1843F,
                            2.1F, -0.31964F, 2.125F, -0.43685F, 2.15F, -0.5343F, 2.175F, -0.61094F,
                            2.2F, -0.66625F, 2.225F, -0.70028F, 2.25F, -0.71358F, 2.275F, -0.70723F,
                            2.3F, -0.6828F, 2.325F, -0.64229F, 2.35F, -0.5881F, 2.375F, -0.523F,
                            2.4F, -0.45006F, 2.425F, -0.37256F, 2.45F, -0.29401F, 2.475F, -0.21797F,
                            2.5F, -0.1481F, 2.525F, -0.08797F, 2.55F, -0.04108F, 2.575F, -0.01074F,
                            2.6F, -0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_TIP,
                    ZERO_CURVE,
                    curve(0.0F, -0.0F, 0.025F, 0.03143F, 0.05F, 0.1262F, 0.075F, 0.28243F,
                            0.1F, 0.49502F, 0.125F, 0.75591F, 0.15F, 1.05448F, 0.175F, 1.37789F,
                            0.2F, 1.7115F, 0.225F, 2.03935F, 0.25F, 2.34459F, 0.275F, 2.61F,
                            0.3F, 2.81844F, 0.325F, 2.95343F, 0.35F, 2.99959F, 0.375F, 2.73406F,
                            0.4F, 2.04304F, 0.425F, 1.08007F, 0.45F, -0.0F, 0.475F, -1.0461F,
                            0.5F, -1.91606F, 0.525F, -2.48156F, 0.55F, -2.63285F, 0.575F, -2.54121F,
                            0.6F, -2.44031F, 0.625F, -2.3305F, 0.65F, -2.21218F, 0.675F, -2.08579F,
                            0.7F, -1.9518F, 0.725F, -1.81068F, 0.75F, -1.66295F, 0.775F, -1.50916F,
                            0.8F, -1.34986F, 0.825F, -1.18563F, 0.85F, -1.01708F, 0.875F, -0.84482F,
                            0.9F, -0.66947F, 0.925F, -0.49168F, 0.95F, -0.3121F, 0.975F, -0.13137F,
                            1.0F, 0.04983F, 1.025F, 0.23085F, 1.05F, 0.41102F, 1.075F, 0.5897F,
                            1.1F, 0.76623F, 1.125F, 0.93996F, 1.15F, 1.11026F, 1.175F, 1.27651F,
                            1.2F, 1.4381F, 1.225F, 1.59444F, 1.25F, 1.74497F, 1.275F, 1.88912F,
                            1.3F, 2.02639F, 1.325F, 2.15626F, 1.35F, 2.27826F, 1.375F, 2.39195F,
                            1.4F, 2.49692F, 1.425F, 2.59277F, 1.45F, 2.67916F, 1.475F, 2.75576F,
                            1.5F, 2.82233F, 1.525F, 2.87859F, 1.55F, 2.92435F, 1.575F, 2.95943F,
                            1.6F, 2.98372F, 1.625F, 2.99712F, 1.65F, 2.99959F, 1.675F, 2.99111F,
                            1.7F, 2.97171F, 1.725F, 2.94147F, 1.75F, 2.9005F, 1.775F, 2.84894F,
                            1.8F, 2.78699F, 1.825F, 2.71487F, 1.85F, 2.63285F, 1.875F, 2.54121F,
                            1.9F, 2.44031F, 1.925F, 2.32179F, 1.95F, 2.17993F, 1.975F, 2.01909F,
                            2.0F, 1.84368F, 2.025F, 1.65808F, 2.05F, 1.4666F, 2.075F, 1.27335F,
                            2.1F, 1.08225F, 2.125F, 0.89689F, 2.15F, 0.72055F, 2.175F, 0.5561F,
                            2.2F, 0.40598F, 2.225F, 0.27214F, 2.25F, 0.15605F, 2.275F, 0.05866F,
                            2.3F, -0.01961F, 2.325F, -0.07889F, 2.35F, -0.11983F, 2.375F, -0.14361F,
                            2.4F, -0.15191F, 2.425F, -0.14687F, 2.45F, -0.13109F, 2.475F, -0.10758F,
                            2.5F, -0.07966F, 2.525F, -0.05099F, 2.55F, -0.02544F, 2.575F, -0.00706F,
                            2.6F, -0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE));
    }

    private static SkeletonAnimation createTailSweepSkeleton() {
        return animation(1.5F, false,
            bone(ModelBone.BODY_CORE,
                    ZERO_CURVE,
                    curve(0.0F, 0.0F, 0.025F, -0.14898F, 0.05F, -0.5686F, 0.075F, -1.21783F,
                            0.1F, -2.05569F, 0.125F, -3.04117F, 0.15F, -4.13326F, 0.175F, -5.29095F,
                            0.2F, -6.47325F, 0.225F, -7.63914F, 0.25F, -8.74763F, 0.275F, -9.75771F,
                            0.3F, -10.62837F, 0.325F, -11.31861F, 0.35F, -11.78743F, 0.375F, -11.99382F,
                            0.4F, -11.78031F, 0.425F, -10.93979F, 0.45F, -9.5603F, 0.475F, -7.7381F,
                            0.5F, -5.56942F, 0.525F, -3.15051F, 0.55F, -0.57762F, 0.575F, 2.05303F,
                            0.6F, 4.64517F, 0.625F, 7.10258F, 0.65F, 9.329F, 0.675F, 11.2282F,
                            0.7F, 12.70393F, 0.725F, 13.65994F, 0.75F, 14.0F, 0.775F, 13.70574F,
                            0.8F, 12.88556F, 0.825F, 11.63336F, 0.85F, 10.04308F, 0.875F, 8.20862F,
                            0.9F, 6.22389F, 0.925F, 4.18282F, 0.95F, 2.17931F, 0.975F, 0.30729F,
                            1.0F, -1.33934F, 1.025F, -2.66667F, 1.05F, -3.58077F, 1.075F, -3.98773F,
                            1.1F, -3.97365F, 1.125F, -3.87208F, 1.15F, -3.7037F, 1.175F, -3.47863F,
                            1.2F, -3.207F, 1.225F, -2.89892F, 1.25F, -2.56452F, 1.275F, -2.21392F,
                            1.3F, -1.85725F, 1.325F, -1.50463F, 1.35F, -1.16618F, 1.375F, -0.85203F,
                            1.4F, -0.57229F, 1.425F, -0.3371F, 1.45F, -0.15657F, 1.475F, -0.04083F,
                            1.5F, 0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.NECK_1,
                    ZERO_CURVE,
                    curve(0.0F, 0.0F, 0.025F, 0.10107F, 0.05F, 0.38672F, 0.075F, 0.83057F,
                            0.1F, 1.40625F, 0.125F, 2.0874F, 0.15F, 2.84766F, 0.175F, 3.66064F,
                            0.2F, 4.5F, 0.225F, 5.33936F, 0.25F, 6.15234F, 0.275F, 6.9126F,
                            0.3F, 7.59375F, 0.325F, 8.16943F, 0.35F, 8.61328F, 0.375F, 8.89893F,
                            0.4F, 9.0F, 0.425F, 8.70845F, 0.45F, 7.89213F, 0.475F, 6.63848F,
                            0.5F, 5.03499F, 0.525F, 3.1691F, 0.55F, 1.12828F, 0.575F, -1.0F,
                            0.6F, -3.12828F, 0.625F, -5.1691F, 0.65F, -7.03499F, 0.675F, -8.63848F,
                            0.7F, -9.89213F, 0.725F, -10.70845F, 0.75F, -11.0F, 0.775F, -10.96415F,
                            0.8F, -10.85985F, 0.825F, -10.692F, 0.85F, -10.46548F, 0.875F, -10.18519F,
                            0.9F, -9.856F, 0.925F, -9.48281F, 0.95F, -9.07052F, 0.975F, -8.624F,
                            1.0F, -8.14815F, 1.025F, -7.64785F, 1.05F, -7.128F, 1.075F, -6.59348F,
                            1.1F, -6.04919F, 1.125F, -5.5F, 1.15F, -4.95081F, 1.175F, -4.40652F,
                            1.2F, -3.872F, 1.225F, -3.35215F, 1.25F, -2.85185F, 1.275F, -2.376F,
                            1.3F, -1.92948F, 1.325F, -1.51719F, 1.35F, -1.144F, 1.375F, -0.81481F,
                            1.4F, -0.53452F, 1.425F, -0.308F, 1.45F, -0.14015F, 1.475F, -0.03585F,
                            1.5F, 0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.HEAD,
                    curve(0.0F, 0.0F, 0.025F, 0.0F, 0.05F, -0.0F, 0.075F, 0.0F,
                            0.1F, -0.0F, 0.125F, 0.0F, 0.15F, -0.0F, 0.175F, -0.0F,
                            0.2F, 0.0F, 0.225F, 0.0F, 0.25F, 0.0F, 0.275F, 1e-05F,
                            0.3F, 0.0F, 0.325F, 0.0F, 0.35F, -0.0F, 0.375F, -0.0F,
                            0.4F, -0.0F, 0.425F, 0.0F, 0.45F, 0.0F, 0.475F, -0.0F,
                            0.5F, 0.0F, 0.525F, -0.0F, 0.55F, 0.0F, 0.575F, -0.0F,
                            0.6F, 0.0F, 0.625F, 1e-05F, 0.65F, 0.0F, 0.675F, -0.0F,
                            0.7F, 0.0F, 0.725F, -0.0F, 0.75F, 0.0F, 0.775F, -0.0F,
                            0.8F, -0.0F, 0.825F, -0.0F, 0.85F, 0.0F, 0.875F, 0.0F,
                            0.9F, 1e-05F, 0.925F, 0.0F, 0.95F, -0.0F, 0.975F, 0.0F,
                            1.0F, 0.0F, 1.025F, -0.0F, 1.05F, -0.0F, 1.075F, -1e-05F,
                            1.1F, -0.0F, 1.125F, -0.0F, 1.15F, -0.0F, 1.175F, 0.0F,
                            1.2F, -0.0F, 1.225F, 1e-05F, 1.25F, 0.0F, 1.275F, 0.0F,
                            1.3F, -0.0F, 1.325F, 0.0F, 1.35F, 0.0F, 1.375F, 1e-05F,
                            1.4F, -0.0F, 1.425F, -1e-05F, 1.45F, 0.0F, 1.475F, -0.0F,
                            1.5F, 0.0F),
                    curve(0.0F, 0.0F, 0.025F, 0.06738F, 0.05F, 0.25781F, 0.075F, 0.55371F,
                            0.1F, 0.9375F, 0.125F, 1.3916F, 0.15F, 1.89844F, 0.175F, 2.44043F,
                            0.2F, 3.0F, 0.225F, 3.55957F, 0.25F, 4.10156F, 0.275F, 4.6084F,
                            0.3F, 5.0625F, 0.325F, 5.44629F, 0.35F, 5.74219F, 0.375F, 5.93262F,
                            0.4F, 6.0F, 0.425F, 5.79592F, 0.45F, 5.22449F, 0.475F, 4.34694F,
                            0.5F, 3.22449F, 0.525F, 1.91837F, 0.55F, 0.4898F, 0.575F, -1.0F,
                            0.6F, -2.4898F, 0.625F, -3.91837F, 0.65F, -5.22449F, 0.675F, -6.34694F,
                            0.7F, -7.22449F, 0.725F, -7.79592F, 0.75F, -8.0F, 0.775F, -7.97393F,
                            0.8F, -7.89807F, 0.825F, -7.776F, 0.85F, -7.61126F, 0.875F, -7.40741F,
                            0.9F, -7.168F, 0.925F, -6.89659F, 0.95F, -6.59674F, 0.975F, -6.272F,
                            1.0F, -5.92593F, 1.025F, -5.56207F, 1.05F, -5.184F, 1.075F, -4.79526F,
                            1.1F, -4.39941F, 1.125F, -4.0F, 1.15F, -3.60059F, 1.175F, -3.20474F,
                            1.2F, -2.816F, 1.225F, -2.43793F, 1.25F, -2.07407F, 1.275F, -1.728F,
                            1.3F, -1.40326F, 1.325F, -1.10341F, 1.35F, -0.832F, 1.375F, -0.59259F,
                            1.4F, -0.38874F, 1.425F, -0.224F, 1.45F, -0.10193F, 1.475F, -0.02607F,
                            1.5F, 0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_1,
                    ZERO_CURVE,
                    curve(0.0F, -0.0F, 0.025F, 0.21594F, 0.05F, 0.81926F, 0.075F, 1.74314F,
                            0.1F, 2.92082F, 0.125F, 4.28551F, 0.15F, 5.77041F, 0.175F, 7.30873F,
                            0.2F, 8.83371F, 0.225F, 10.27853F, 0.25F, 11.57643F, 0.275F, 12.66061F,
                            0.3F, 13.46428F, 0.325F, 13.92066F, 0.35F, 13.89386F, 0.375F, 12.76894F,
                            0.4F, 10.58545F, 0.425F, 7.55509F, 0.45F, 3.88959F, 0.475F, -0.19935F,
                            0.5F, -4.5F, 0.525F, -8.80065F, 0.55F, -12.88959F, 0.575F, -16.55509F,
                            0.6F, -19.58545F, 0.625F, -21.76894F, 0.65F, -22.89386F, 0.675F, -22.85822F,
                            0.7F, -22.03978F, 0.725F, -20.5912F, 0.75F, -18.625F, 0.775F, -16.25371F,
                            0.8F, -13.58985F, 0.825F, -10.74595F, 0.85F, -7.83453F, 0.875F, -4.96813F,
                            0.9F, -2.25926F, 0.925F, 0.17955F, 0.95F, 2.23577F, 0.975F, 3.79687F,
                            1.0F, 4.75034F, 1.025F, 4.99838F, 1.05F, 4.94385F, 1.075F, 4.8181F,
                            1.1F, 4.62963F, 1.125F, 4.3869F, 1.15F, 4.0984F, 1.175F, 3.77259F,
                            1.2F, 3.41797F, 1.225F, 3.043F, 1.25F, 2.65616F, 1.275F, 2.26593F,
                            1.3F, 1.88079F, 1.325F, 1.50921F, 1.35F, 1.15967F, 1.375F, 0.84065F,
                            1.4F, 0.56062F, 1.425F, 0.32806F, 1.45F, 0.15146F, 1.475F, 0.03928F,
                            1.5F, -0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_2,
                    ZERO_CURVE,
                    curve(0.0F, -0.0F, 0.025F, 0.21221F, 0.05F, 0.80824F, 0.075F, 1.72716F,
                            0.1F, 2.90805F, 0.125F, 4.28998F, 0.15F, 5.81203F, 0.175F, 7.41329F,
                            0.2F, 9.03282F, 0.225F, 10.6097F, 0.25F, 12.08302F, 0.275F, 13.39184F,
                            0.3F, 14.47525F, 0.325F, 15.27233F, 0.35F, 15.72214F, 0.375F, 15.68485F,
                            0.4F, 14.46517F, 0.425F, 12.09983F, 0.45F, 8.82077F, 0.475F, 4.8599F,
                            0.5F, 0.44913F, 0.525F, -4.17964F, 0.55F, -8.7945F, 0.575F, -13.16353F,
                            0.6F, -17.05483F, 0.625F, -20.23649F, 0.65F, -22.47658F, 0.675F, -23.54321F,
                            0.7F, -23.40256F, 0.725F, -22.52528F, 0.75F, -21.04243F, 0.775F, -19.05951F,
                            0.8F, -16.68198F, 0.825F, -14.01541F, 0.85F, -11.16526F, 0.875F, -8.23704F,
                            0.9F, -5.33625F, 0.925F, -2.56839F, 0.95F, -0.03896F, 0.975F, 2.14653F,
                            1.0F, 3.88259F, 1.025F, 5.06372F, 1.05F, 5.5844F, 1.075F, 5.56708F,
                            1.1F, 5.43979F, 1.125F, 5.22789F, 1.15F, 4.94331F, 1.175F, 4.59796F,
                            1.2F, 4.20376F, 1.225F, 3.77262F, 1.25F, 3.31646F, 1.275F, 2.84719F,
                            1.3F, 2.37673F, 1.325F, 1.91699F, 1.35F, 1.47989F, 1.375F, 1.07735F,
                            1.4F, 0.72128F, 1.425F, 0.42359F, 1.45F, 0.19621F, 1.475F, 0.05104F,
                            1.5F, -0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_3,
                    ZERO_CURVE,
                    curve(0.0F, 0.0F, 0.025F, 0.20769F, 0.05F, 0.79367F, 0.075F, 1.70232F,
                            0.1F, 2.878F, 0.125F, 4.26508F, 0.15F, 5.80792F, 0.175F, 7.4509F,
                            0.2F, 9.13839F, 0.225F, 10.81475F, 0.25F, 12.42435F, 0.275F, 13.91156F,
                            0.3F, 15.22076F, 0.325F, 16.2963F, 0.35F, 17.08255F, 0.375F, 17.5239F,
                            0.4F, 17.47551F, 0.425F, 16.15775F, 0.45F, 13.60459F, 0.475F, 10.06918F,
                            0.5F, 5.80468F, 0.525F, 1.06424F, 0.55F, -3.89897F, 0.575F, -8.83181F,
                            0.6F, -13.4811F, 0.625F, -17.59371F, 0.65F, -20.91647F, 0.675F, -23.19623F,
                            0.7F, -24.17982F, 0.725F, -23.93986F, 0.75F, -23.0077F, 0.775F, -21.4928F,
                            0.8F, -19.49422F, 0.825F, -17.11112F, 0.85F, -14.44256F, 0.875F, -11.58771F,
                            0.9F, -8.64561F, 0.925F, -5.71543F, 0.95F, -2.89625F, 0.975F, -0.28717F,
                            1.0F, 2.01269F, 1.025F, 3.90421F, 1.05F, 5.2883F, 1.075F, 6.06585F,
                            1.1F, 6.18912F, 1.125F, 6.07217F, 1.15F, 5.84053F, 1.175F, 5.51106F,
                            1.2F, 5.10062F, 1.225F, 4.62609F, 1.25F, 4.10434F, 1.275F, 3.55222F,
                            1.3F, 2.98661F, 1.325F, 2.42437F, 1.35F, 1.88237F, 1.375F, 1.37748F,
                            1.4F, 0.92657F, 1.425F, 0.5465F, 1.45F, 0.25413F, 1.475F, 0.06634F,
                            1.5F, 0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_4,
                    ZERO_CURVE,
                    curve(0.0F, -0.0F, 0.025F, 0.20272F, 0.05F, 0.77697F, 0.075F, 1.67184F,
                            0.1F, 2.83644F, 0.125F, 4.21988F, 0.15F, 5.77127F, 0.175F, 7.43972F,
                            0.2F, 9.17433F, 0.225F, 10.9242F, 0.25F, 12.63845F, 0.275F, 14.26618F,
                            0.3F, 15.7565F, 0.325F, 17.05851F, 0.35F, 18.12133F, 0.375F, 18.89405F,
                            0.4F, 19.3258F, 0.425F, 19.26584F, 0.45F, 17.84658F, 0.475F, 15.09935F,
                            0.5F, 11.29966F, 0.525F, 6.723F, 0.55F, 1.64494F, 0.575F, -3.65906F,
                            0.6F, -8.91347F, 0.625F, -13.84276F, 0.65F, -18.17144F, 0.675F, -21.62399F,
                            0.7F, -23.92489F, 0.725F, -24.79863F, 0.75F, -24.47077F, 0.775F, -23.48733F,
                            0.8F, -21.94236F, 0.825F, -19.92916F, 0.85F, -17.54099F, 0.875F, -14.87111F,
                            0.9F, -12.01283F, 0.925F, -9.05941F, 0.95F, -6.10411F, 0.975F, -3.24022F,
                            1.0F, -0.56103F, 1.025F, 1.84021F, 1.05F, 3.8702F, 1.075F, 5.43568F,
                            1.1F, 6.44337F, 1.125F, 6.8F, 1.15F, 6.71336F, 1.175F, 6.46957F,
                            1.2F, 6.0928F, 1.225F, 5.60723F, 1.25F, 5.03704F, 1.275F, 4.4064F,
                            1.3F, 3.73949F, 1.325F, 3.0605F, 1.35F, 2.3936F, 1.375F, 1.76296F,
                            1.4F, 1.19277F, 1.425F, 0.7072F, 1.45F, 0.33043F, 1.475F, 0.08664F,
                            1.5F, -0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_TIP,
                    ZERO_CURVE,
                    curve(0.0F, -0.0F, 0.025F, 0.19754F, 0.05F, 0.75906F, 0.075F, 1.6379F,
                            0.1F, 2.78738F, 0.125F, 4.16085F, 0.15F, 5.71164F, 0.175F, 7.39308F,
                            0.2F, 9.15853F, 0.225F, 10.9613F, 0.25F, 12.75474F, 0.275F, 14.49219F,
                            0.3F, 16.12697F, 0.325F, 17.61244F, 0.35F, 18.9019F, 0.375F, 19.94873F,
                            0.4F, 20.70624F, 0.425F, 21.12777F, 0.45F, 21.05582F, 0.475F, 19.53148F,
                            0.5F, 16.58372F, 0.525F, 12.51154F, 0.55F, 7.61399F, 0.575F, 2.19013F,
                            0.6F, -3.46101F, 0.625F, -9.04037F, 0.65F, -14.24891F, 0.675F, -18.7876F,
                            0.7F, -22.35739F, 0.725F, -24.65923F, 0.75F, -25.3977F, 0.775F, -24.99586F,
                            0.8F, -23.96439F, 0.825F, -22.39122F, 0.85F, -20.36427F, 0.875F, -17.97151F,
                            0.9F, -15.30084F, 0.925F, -12.44023F, 0.95F, -9.47761F, 0.975F, -6.50092F,
                            1.0F, -3.5981F, 1.025F, -0.85709F, 1.05F, 1.63418F, 1.075F, 3.78775F,
                            1.1F, 5.51571F, 1.125F, 6.73009F, 1.15F, 7.34297F, 1.175F, 7.35805F,
                            1.2F, 7.11683F, 1.225F, 6.69204F, 1.25F, 6.11897F, 1.275F, 5.43294F,
                            1.3F, 4.66924F, 1.325F, 3.86319F, 1.35F, 3.05007F, 1.375F, 2.2652F,
                            1.4F, 1.54386F, 1.425F, 0.92138F, 1.45F, 0.43303F, 1.475F, 0.11414F,
                            1.5F, -0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE));
    }

    private static SkeletonAnimation createScaleBurstSkeleton() {
        return animation(1.45F, false,
            bone(ModelBone.NECK_1,
                    curve(0.0F, -0.0F, 0.025F, 0.05207F, 0.05F, 0.19684F, 0.075F, 0.41714F,
                            0.1F, 0.6958F, 0.125F, 1.01566F, 0.15F, 1.35956F, 0.175F, 1.71032F,
                            0.2F, 2.05078F, 0.225F, 2.36378F, 0.25F, 2.63214F, 0.275F, 2.83871F,
                            0.3F, 2.96631F, 0.325F, 2.99149F, 0.35F, 2.71219F, 0.375F, 2.09465F,
                            0.4F, 1.21574F, 0.425F, 0.15234F, 0.45F, -1.01868F, 0.475F, -2.22045F,
                            0.5F, -3.37609F, 0.525F, -4.40874F, 0.55F, -5.24153F, 0.575F, -5.79757F,
                            0.6F, -6.0F, 0.625F, -5.84075F, 0.65F, -5.40333F, 0.675F, -4.74821F,
                            0.7F, -3.93586F, 0.725F, -3.02675F, 0.75F, -2.08136F, 0.775F, -1.16016F,
                            0.8F, -0.32362F, 0.825F, 0.36779F, 0.85F, 0.85359F, 0.875F, 1.07331F,
                            0.9F, 1.0761F, 0.925F, 1.06087F, 0.95F, 1.03514F, 0.975F, 1.0F,
                            1.0F, 0.95655F, 1.025F, 0.90589F, 1.05F, 0.8491F, 1.075F, 0.78729F,
                            1.1F, 0.72153F, 1.125F, 0.65294F, 1.15F, 0.58259F, 1.175F, 0.51159F,
                            1.2F, 0.44103F, 1.225F, 0.37199F, 1.25F, 0.30558F, 1.275F, 0.24289F,
                            1.3F, 0.18501F, 1.325F, 0.13304F, 1.35F, 0.08806F, 1.375F, 0.05117F,
                            1.4F, 0.02347F, 1.425F, 0.00605F, 1.45F, -0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.NECK_2,
                    curve(0.0F, 0.0F, 0.025F, 0.07811F, 0.05F, 0.29526F, 0.075F, 0.62571F,
                            0.1F, 1.0437F, 0.125F, 1.52349F, 0.15F, 2.03934F, 0.175F, 2.56548F,
                            0.2F, 3.07617F, 0.225F, 3.54567F, 0.25F, 3.9482F, 0.275F, 4.25805F,
                            0.3F, 4.44946F, 0.325F, 4.48724F, 0.35F, 4.06828F, 0.375F, 3.14196F,
                            0.4F, 1.82362F, 0.425F, 0.22851F, 0.45F, -1.52803F, 0.475F, -3.33067F,
                            0.5F, -5.06414F, 0.525F, -6.61312F, 0.55F, -7.86229F, 0.575F, -8.69635F,
                            0.6F, -9.0F, 0.625F, -8.76113F, 0.65F, -8.105F, 0.675F, -7.12232F,
                            0.7F, -5.90379F, 0.725F, -4.54013F, 0.75F, -3.12205F, 0.775F, -1.74023F,
                            0.8F, -0.48542F, 0.825F, 0.55168F, 0.85F, 1.28038F, 0.875F, 1.60996F,
                            0.9F, 1.61416F, 0.925F, 1.5913F, 0.95F, 1.5527F, 0.975F, 1.5F,
                            1.0F, 1.43483F, 1.025F, 1.35884F, 1.05F, 1.27366F, 1.075F, 1.18093F,
                            1.1F, 1.0823F, 1.125F, 0.9794F, 1.15F, 0.87389F, 1.175F, 0.76738F,
                            1.2F, 0.66154F, 1.225F, 0.55798F, 1.25F, 0.45838F, 1.275F, 0.36433F,
                            1.3F, 0.27752F, 1.325F, 0.19955F, 1.35F, 0.13208F, 1.375F, 0.07676F,
                            1.4F, 0.03521F, 1.425F, 0.00907F, 1.45F, 0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.HEAD,
                    curve(0.0F, -0.0F, 0.025F, 0.02604F, 0.05F, 0.09843F, 0.075F, 0.20857F,
                            0.1F, 0.3479F, 0.125F, 0.50783F, 0.15F, 0.67978F, 0.175F, 0.85516F,
                            0.2F, 1.02539F, 0.225F, 1.18189F, 0.25F, 1.31607F, 0.275F, 1.41935F,
                            0.3F, 1.48315F, 0.325F, 1.49575F, 0.35F, 1.3561F, 0.375F, 1.04732F,
                            0.4F, 0.60787F, 0.425F, 0.07617F, 0.45F, -0.50934F, 0.475F, -1.11023F,
                            0.5F, -1.68806F, 0.525F, -2.20437F, 0.55F, -2.62076F, 0.575F, -2.89878F,
                            0.6F, -3.0F, 0.625F, -2.92038F, 0.65F, -2.70167F, 0.675F, -2.37411F,
                            0.7F, -1.96793F, 0.725F, -1.51338F, 0.75F, -1.04068F, 0.775F, -0.58008F,
                            0.8F, -0.16181F, 0.825F, 0.1839F, 0.85F, 0.42679F, 0.875F, 0.53665F,
                            0.9F, 0.53805F, 0.925F, 0.53043F, 0.95F, 0.51757F, 0.975F, 0.5F,
                            1.0F, 0.47828F, 1.025F, 0.45295F, 1.05F, 0.42455F, 1.075F, 0.39364F,
                            1.1F, 0.36077F, 1.125F, 0.32647F, 1.15F, 0.2913F, 1.175F, 0.2558F,
                            1.2F, 0.22052F, 1.225F, 0.186F, 1.25F, 0.15279F, 1.275F, 0.12145F,
                            1.3F, 0.09251F, 1.325F, 0.06652F, 1.35F, 0.04403F, 1.375F, 0.0256F,
                            1.4F, 0.01174F, 1.425F, 0.00303F, 1.45F, -0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.JAW,
                    curve(0.0F, 0.0F, 0.025F, 0.0F, 0.05F, -0.0F, 0.075F, 0.0F,
                            0.1F, -0.0F, 0.125F, 0.0F, 0.15F, -0.0F, 0.175F, 0.0F,
                            0.2F, 0.0F, 0.225F, -0.0F, 0.25F, 0.0F, 0.275F, -0.0F,
                            0.3F, -0.0F, 0.325F, -0.0F, 0.35F, 0.0F, 0.375F, 0.0F,
                            0.4F, 0.0F, 0.425F, -0.0F, 0.45F, 1.52802F, 0.475F, 3.33067F,
                            0.5F, 5.06414F, 0.525F, 6.61312F, 0.55F, 7.86229F, 0.575F, 8.69635F,
                            0.6F, 9.0F, 0.625F, 8.76113F, 0.65F, 8.105F, 0.675F, 7.12232F,
                            0.7F, 5.90379F, 0.725F, 4.54013F, 0.75F, 3.12204F, 0.775F, 1.74023F,
                            0.8F, 0.48542F, 0.825F, 0.0F, 0.85F, 0.0F, 0.875F, -0.0F,
                            0.9F, -0.0F, 0.925F, -0.0F, 0.95F, -0.0F, 0.975F, 0.0F,
                            1.0F, -0.0F, 1.025F, 0.0F, 1.05F, -0.0F, 1.075F, -0.0F,
                            1.1F, -0.0F, 1.125F, 0.0F, 1.15F, 0.0F, 1.175F, -0.0F,
                            1.2F, -0.0F, 1.225F, 0.0F, 1.25F, -0.0F, 1.275F, -0.0F,
                            1.3F, -0.0F, 1.325F, -0.0F, 1.35F, 0.0F, 1.375F, -0.0F,
                            1.4F, 0.0F, 1.425F, 0.0F, 1.45F, 0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_1,
                    ZERO_CURVE,
                    curve(0.0F, -0.0F, 0.025F, -0.00281F, 0.05F, -0.02116F, 0.075F, -0.0666F,
                            0.1F, -0.14608F, 0.125F, -0.26181F, 0.15F, -0.41138F, 0.175F, -0.58809F,
                            0.2F, -0.78151F, 0.225F, -0.97824F, 0.25F, -1.16276F, 0.275F, -1.31855F,
                            0.3F, -1.42909F, 0.325F, -1.47604F, 0.35F, -1.35411F, 0.375F, -1.04579F,
                            0.4F, -0.59986F, 0.425F, -0.0734F, 0.45F, 0.47316F, 0.475F, 0.9809F,
                            0.5F, 1.39718F, 0.525F, 1.68009F, 0.55F, 1.8023F, 0.575F, 1.75427F,
                            0.6F, 1.54666F, 0.625F, 1.22623F, 0.65F, 0.86265F, 0.675F, 0.51036F,
                            0.7F, 0.21277F, 0.725F, -0.0F, 0.75F, -0.11252F, 0.775F, -0.1247F,
                            0.8F, -0.05167F, 0.825F, 0.07722F, 0.85F, 0.22004F, 0.875F, 0.32477F,
                            0.9F, 0.37002F, 0.925F, 0.40428F, 0.95F, 0.42839F, 0.975F, 0.44176F,
                            1.0F, 0.44431F, 1.025F, 0.43644F, 1.05F, 0.41896F, 1.075F, 0.39307F,
                            1.1F, 0.36024F, 1.125F, 0.32217F, 1.15F, 0.28068F, 1.175F, 0.23763F,
                            1.2F, 0.19483F, 1.225F, 0.15395F, 1.25F, 0.11645F, 1.275F, 0.08352F,
                            1.3F, 0.05598F, 1.325F, 0.03429F, 1.35F, 0.01849F, 1.375F, 0.00817F,
                            1.4F, 0.00252F, 1.425F, 0.00033F, 1.45F, -0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_2,
                    ZERO_CURVE,
                    curve(0.0F, -0.0F, 0.025F, 0.01229F, 0.05F, 0.03681F, 0.075F, 0.05664F,
                            0.1F, 0.05772F, 0.125F, 0.02961F, 0.15F, -0.03396F, 0.175F, -0.13482F,
                            0.2F, -0.27019F, 0.225F, -0.43287F, 0.25F, -0.61159F, 0.275F, -0.7916F,
                            0.3F, -0.95544F, 0.325F, -1.0816F, 0.35F, -1.07614F, 0.375F, -0.89514F,
                            0.4F, -0.55062F, 0.425F, -0.07208F, 0.45F, 0.49697F, 0.475F, 1.1032F,
                            0.5F, 1.68803F, 0.525F, 2.19243F, 0.55F, 2.56182F, 0.575F, 2.75089F,
                            0.6F, 2.72797F, 0.625F, 2.50862F, 0.65F, 2.1576F, 0.675F, 1.7304F,
                            0.7F, 1.28027F, 0.725F, 0.85452F, 0.75F, 0.4913F, 0.775F, 0.21696F,
                            0.8F, 0.04394F, 0.825F, -0.03051F, 0.85F, -0.02489F, 0.875F, 0.02681F,
                            0.9F, 0.08483F, 0.925F, 0.13977F, 0.95F, 0.18956F, 0.975F, 0.23236F,
                            1.0F, 0.26675F, 1.025F, 0.29178F, 1.05F, 0.307F, 1.075F, 0.31238F,
                            1.1F, 0.30835F, 1.125F, 0.29572F, 1.15F, 0.27566F, 1.175F, 0.24958F,
                            1.2F, 0.21912F, 1.225F, 0.18599F, 1.25F, 0.15196F, 1.275F, 0.11872F,
                            1.3F, 0.08779F, 1.325F, 0.06049F, 1.35F, 0.03782F, 1.375F, 0.02043F,
                            1.4F, 0.00855F, 1.425F, 0.00197F, 1.45F, -0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_3,
                    ZERO_CURVE,
                    curve(0.0F, 0.0F, 0.025F, 0.0231F, 0.05F, 0.08193F, 0.075F, 0.16009F,
                            0.1F, 0.24135F, 0.125F, 0.3107F, 0.15F, 0.35532F, 0.175F, 0.36555F,
                            0.2F, 0.33552F, 0.225F, 0.26371F, 0.25F, 0.15323F, 0.275F, 0.01187F,
                            0.3F, -0.14802F, 0.325F, -0.30932F, 0.35F, -0.42224F, 0.375F, -0.4318F,
                            0.4F, -0.30902F, 0.425F, -0.04559F, 0.45F, 0.34717F, 0.475F, 0.84013F,
                            0.5F, 1.38921F, 0.525F, 1.93889F, 0.55F, 2.42643F, 0.575F, 2.78654F,
                            0.6F, 2.95632F, 0.625F, 2.91467F, 0.65F, 2.69883F, 0.675F, 2.34596F,
                            0.7F, 1.90053F, 0.725F, 1.41053F, 0.75F, 0.9235F, 0.775F, 0.48283F,
                            0.8F, 0.1242F, 0.825F, -0.12758F, 0.85F, -0.26112F, 0.875F, -0.28051F,
                            0.9F, -0.23F, 0.925F, -0.17357F, 0.95F, -0.11548F, 0.975F, -0.05821F,
                            1.0F, -0.004F, 1.025F, 0.04521F, 1.05F, 0.0878F, 1.075F, 0.12257F,
                            1.1F, 0.14874F, 1.125F, 0.16597F, 1.15F, 0.17434F, 1.175F, 0.17436F,
                            1.2F, 0.16687F, 1.225F, 0.15307F, 1.25F, 0.13439F, 1.275F, 0.11244F,
                            1.3F, 0.08892F, 1.325F, 0.06555F, 1.35F, 0.04394F, 1.375F, 0.02556F,
                            1.4F, 0.0116F, 1.425F, 0.00292F, 1.45F, 0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_4,
                    ZERO_CURVE,
                    curve(0.0F, -0.0F, 0.025F, 0.02585F, 0.05F, 0.09841F, 0.075F, 0.20761F,
                            0.1F, 0.34068F, 0.125F, 0.48323F, 0.15F, 0.62048F, 0.175F, 0.73822F,
                            0.2F, 0.82402F, 0.225F, 0.86817F, 0.25F, 0.86452F, 0.275F, 0.81119F,
                            0.3F, 0.7111F, 0.325F, 0.57102F, 0.35F, 0.37915F, 0.375F, 0.18239F,
                            0.4F, 0.04052F, 0.425F, -0.00317F, 0.45F, 0.07609F, 0.475F, 0.28358F,
                            0.5F, 0.60509F, 0.525F, 1.00804F, 0.55F, 1.44342F, 0.575F, 1.84878F,
                            0.6F, 2.15194F, 0.625F, 2.30254F, 0.65F, 2.29729F, 0.675F, 2.14201F,
                            0.7F, 1.85689F, 0.725F, 1.4738F, 0.75F, 1.03309F, 0.775F, 0.58003F,
                            0.8F, 0.16107F, 0.825F, -0.18008F, 0.85F, -0.40613F, 0.875F, -0.48984F,
                            0.9F, -0.46448F, 0.925F, -0.42628F, 0.95F, -0.38019F, 0.975F, -0.32845F,
                            1.0F, -0.27335F, 1.025F, -0.21717F, 1.05F, -0.16208F, 1.075F, -0.11006F,
                            1.1F, -0.06283F, 1.125F, -0.02176F, 1.15F, 0.01212F, 1.175F, 0.03821F,
                            1.2F, 0.05632F, 1.225F, 0.06667F, 1.25F, 0.06987F, 1.275F, 0.06689F,
                            1.3F, 0.059F, 1.325F, 0.04771F, 1.35F, 0.03471F, 1.375F, 0.02176F,
                            1.4F, 0.01059F, 1.425F, 0.00285F, 1.45F, -0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_TIP,
                    ZERO_CURVE,
                    curve(0.0F, -0.0F, 0.025F, 0.01955F, 0.05F, 0.08053F, 0.075F, 0.18261F,
                            0.1F, 0.32099F, 0.125F, 0.48698F, 0.15F, 0.66889F, 0.175F, 0.85301F,
                            0.2F, 1.02467F, 0.225F, 1.16935F, 0.25F, 1.27381F, 0.275F, 1.32714F,
                            0.3F, 1.32182F, 0.325F, 1.25188F, 0.35F, 1.0481F, 0.375F, 0.73286F,
                            0.4F, 0.37591F, 0.425F, 0.04036F, 0.45F, -0.22158F, 0.475F, -0.37204F,
                            0.5F, -0.3904F, 0.525F, -0.27495F, 0.55F, -0.04382F, 0.575F, 0.26517F,
                            0.6F, 0.59583F, 0.625F, 0.88607F, 0.65F, 1.09324F, 0.675F, 1.18979F,
                            0.7F, 1.16458F, 0.725F, 1.02223F, 0.75F, 0.78179F, 0.775F, 0.47461F,
                            0.8F, 0.14167F, 0.825F, -0.16967F, 0.85F, -0.40927F, 0.875F, -0.52806F,
                            0.9F, -0.5367F, 0.925F, -0.53006F, 0.95F, -0.51208F, 0.975F, -0.48394F,
                            1.0F, -0.44721F, 1.025F, -0.40367F, 1.05F, -0.35533F, 1.075F, -0.30424F,
                            1.1F, -0.25244F, 1.125F, -0.20189F, 1.15F, -0.15433F, 1.175F, -0.11127F,
                            1.2F, -0.07389F, 1.225F, -0.04302F, 1.25F, -0.01906F, 1.275F, -0.00203F,
                            1.3F, 0.00846F, 1.325F, 0.01321F, 1.35F, 0.01336F, 1.375F, 0.01035F,
                            1.4F, 0.00588F, 1.425F, 0.00179F, 1.45F, -0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE));
    }

    private static SkeletonAnimation createFireRushSkeleton() {
        return animation(1.1F, false,
            bone(ModelBone.BODY_CORE,
                    ZERO_CURVE,
                    ZERO_CURVE,
                    curve(0.0F, -0.0F, 0.025F, -0.0F, 0.05F, -0.0F, 0.075F, 0.03523F,
                            0.1F, 0.26616F, 0.125F, 0.47957F, 0.15F, 0.64135F, 0.175F, 0.71867F,
                            0.2F, 0.60196F, 0.225F, 0.17005F, 0.25F, -0.0F, 0.275F, 0.42026F,
                            0.3F, 1.08419F, 0.325F, 1.59359F, 0.35F, 1.86813F, 0.375F, 1.89343F,
                            0.4F, 1.8844F, 0.425F, 1.86822F, 0.45F, 1.84589F, 0.475F, 1.81843F,
                            0.5F, 1.78686F, 0.525F, 1.75221F, 0.55F, 1.71552F, 0.575F, 1.67783F,
                            0.6F, 1.64021F, 0.625F, 1.60376F, 0.65F, 1.56955F, 0.675F, 1.53872F,
                            0.7F, 1.51238F, 0.725F, 1.49165F, 0.75F, 1.47766F, 0.775F, 1.47154F,
                            0.8F, 1.45191F, 0.825F, 1.37794F, 0.85F, 1.25713F, 0.875F, 1.09821F,
                            0.9F, 0.9101F, 0.925F, 0.70213F, 0.95F, 0.48428F, 0.975F, 0.26721F,
                            1.0F, 0.06226F, 1.025F, -0.0F, 1.05F, -0.0F, 1.075F, -0.0F,
                            1.1F, -0.0F),
                    ZERO_CURVE),
            bone(ModelBone.NECK_1,
                    curve(0.0F, -0.0F, 0.025F, -0.09452F, 0.05F, -0.33951F, 0.075F, -0.67708F,
                            0.1F, -1.04938F, 0.125F, -1.39853F, 0.15F, -1.66667F, 0.175F, -1.79591F,
                            0.2F, -1.6011F, 0.225F, -0.89375F, 0.25F, 0.14925F, 0.275F, 1.34142F,
                            0.3F, 2.4963F, 0.325F, 3.42741F, 0.35F, 3.94829F, 0.375F, 3.99701F,
                            0.4F, 3.97961F, 0.425F, 3.94845F, 0.45F, 3.90554F, 0.475F, 3.85291F,
                            0.5F, 3.79259F, 0.525F, 3.7266F, 0.55F, 3.65697F, 0.575F, 3.58572F,
                            0.6F, 3.51487F, 0.625F, 3.44645F, 0.65F, 3.38249F, 0.675F, 3.325F,
                            0.7F, 3.27602F, 0.725F, 3.23756F, 0.75F, 3.21166F, 0.775F, 3.20034F,
                            0.8F, 3.16406F, 0.825F, 3.02795F, 0.85F, 2.80762F, 0.875F, 2.52136F,
                            0.9F, 2.1875F, 0.925F, 1.82434F, 0.95F, 1.4502F, 0.975F, 1.08337F,
                            1.0F, 0.74219F, 1.025F, 0.44495F, 1.05F, 0.20996F, 1.075F, 0.05554F,
                            1.1F, -0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.NECK_2,
                    curve(0.0F, 0.0F, 0.025F, -0.14178F, 0.05F, -0.50927F, 0.075F, -1.01562F,
                            0.1F, -1.57407F, 0.125F, -2.0978F, 0.15F, -2.5F, 0.175F, -2.69388F,
                            0.2F, -2.40165F, 0.225F, -1.34062F, 0.25F, 0.22386F, 0.275F, 2.01212F,
                            0.3F, 3.74444F, 0.325F, 5.14111F, 0.35F, 5.92243F, 0.375F, 5.99552F,
                            0.4F, 5.96941F, 0.425F, 5.92267F, 0.45F, 5.85831F, 0.475F, 5.77937F,
                            0.5F, 5.68889F, 0.525F, 5.58991F, 0.55F, 5.48545F, 0.575F, 5.37858F,
                            0.6F, 5.2723F, 0.625F, 5.16967F, 0.65F, 5.07373F, 0.675F, 4.9875F,
                            0.7F, 4.91403F, 0.725F, 4.85635F, 0.75F, 4.81749F, 0.775F, 4.80051F,
                            0.8F, 4.74609F, 0.825F, 4.54193F, 0.85F, 4.21143F, 0.875F, 3.78204F,
                            0.9F, 3.28125F, 0.925F, 2.73651F, 0.95F, 2.17528F, 0.975F, 1.62506F,
                            1.0F, 1.11327F, 1.025F, 0.66742F, 1.05F, 0.31494F, 1.075F, 0.08331F,
                            1.1F, 0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.HEAD,
                    curve(0.0F, -0.0F, 0.025F, 0.09452F, 0.05F, 0.33951F, 0.075F, 0.67708F,
                            0.1F, 1.04938F, 0.125F, 1.39853F, 0.15F, 1.66667F, 0.175F, 1.79591F,
                            0.2F, 1.6011F, 0.225F, 0.89375F, 0.25F, -0.14925F, 0.275F, -1.34142F,
                            0.3F, -2.4963F, 0.325F, -3.42741F, 0.35F, -3.94829F, 0.375F, -3.99701F,
                            0.4F, -3.97961F, 0.425F, -3.94845F, 0.45F, -3.90554F, 0.475F, -3.85291F,
                            0.5F, -3.79259F, 0.525F, -3.7266F, 0.55F, -3.65697F, 0.575F, -3.58572F,
                            0.6F, -3.51487F, 0.625F, -3.44645F, 0.65F, -3.38249F, 0.675F, -3.325F,
                            0.7F, -3.27602F, 0.725F, -3.23756F, 0.75F, -3.21166F, 0.775F, -3.20034F,
                            0.8F, -3.16406F, 0.825F, -3.02796F, 0.85F, -2.80762F, 0.875F, -2.52136F,
                            0.9F, -2.1875F, 0.925F, -1.82434F, 0.95F, -1.4502F, 0.975F, -1.08337F,
                            1.0F, -0.74219F, 1.025F, -0.44495F, 1.05F, -0.20996F, 1.075F, -0.05554F,
                            1.1F, -0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.JAW,
                    curve(0.0F, 0.0F, 0.025F, -0.0F, 0.05F, -0.0F, 0.075F, -0.0F,
                            0.1F, -0.0F, 0.125F, -0.0F, 0.15F, 0.0F, 0.175F, -0.0F,
                            0.2F, -0.0F, 0.225F, 0.0F, 0.25F, 0.3358F, 0.275F, 3.01819F,
                            0.3F, 5.61667F, 0.325F, 7.71167F, 0.35F, 8.88364F, 0.375F, 8.99328F,
                            0.4F, 8.95413F, 0.425F, 8.88401F, 0.45F, 8.78746F, 0.475F, 8.66905F,
                            0.5F, 8.53333F, 0.525F, 8.38486F, 0.55F, 8.22818F, 0.575F, 8.06786F,
                            0.6F, 7.90845F, 0.625F, 7.75451F, 0.65F, 7.61059F, 0.675F, 7.48125F,
                            0.7F, 7.37104F, 0.725F, 7.28452F, 0.75F, 7.22624F, 0.775F, 7.20076F,
                            0.8F, 7.11914F, 0.825F, 6.8129F, 0.85F, 6.31714F, 0.875F, 5.67307F,
                            0.9F, 4.92188F, 0.925F, 4.10477F, 0.95F, 3.26294F, 0.975F, 2.43759F,
                            1.0F, 1.66992F, 1.025F, 1.00113F, 1.05F, 0.47241F, 1.075F, 0.12497F,
                            1.1F, 0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_1,
                    ZERO_CURVE,
                    curve(0.0F, -0.0F, 0.025F, -0.01009F, 0.05F, -0.07174F, 0.075F, -0.21095F,
                            0.1F, -0.4255F, 0.125F, -0.68688F, 0.15F, -0.94469F, 0.175F, -1.13311F,
                            0.2F, -1.09231F, 0.225F, -0.64316F, 0.25F, 0.11079F, 0.275F, 1.00606F,
                            0.3F, 1.85317F, 0.325F, 2.46643F, 0.35F, 2.69361F, 0.375F, 2.52188F,
                            0.4F, 2.25569F, 0.425F, 1.93926F, 0.45F, 1.58362F, 0.475F, 1.20042F,
                            0.5F, 0.80137F, 0.525F, 0.39776F, 0.55F, -0.0F, 0.575F, -0.38273F,
                            0.6F, -0.74269F, 0.625F, -1.07378F, 0.65F, -1.37153F, 0.675F, -1.63306F,
                            0.7F, -1.85689F, 0.725F, -2.04271F, 0.75F, -2.19107F, 0.775F, -2.30303F,
                            0.8F, -2.34889F, 0.825F, -2.27097F, 0.85F, -2.08428F, 0.875F, -1.81442F,
                            0.9F, -1.49236F, 0.925F, -1.15105F, 0.95F, -0.82199F, 0.975F, -0.53209F,
                            1.0F, -0.30094F, 1.025F, -0.13863F, 1.05F, -0.04436F, 1.075F, -0.00593F,
                            1.1F, -0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_2,
                    ZERO_CURVE,
                    curve(0.0F, -0.0F, 0.025F, 0.03129F, 0.05F, 0.07874F, 0.075F, 0.08671F,
                            0.1F, 0.02266F, 0.125F, -0.11931F, 0.15F, -0.31748F, 0.175F, -0.52402F,
                            0.2F, -0.61985F, 0.225F, -0.42419F, 0.25F, 0.08245F, 0.275F, 0.83034F,
                            0.3F, 1.67993F, 0.325F, 2.44455F, 0.35F, 2.91772F, 0.375F, 2.99652F,
                            0.4F, 2.96534F, 0.425F, 2.86423F, 0.45F, 2.69839F, 0.475F, 2.47494F,
                            0.5F, 2.20244F, 0.525F, 1.89037F, 0.55F, 1.54866F, 0.575F, 1.18715F,
                            0.6F, 0.81522F, 0.625F, 0.44139F, 0.65F, 0.07305F, 0.675F, -0.28367F,
                            0.7F, -0.62404F, 0.725F, -0.94468F, 0.75F, -1.24337F, 0.775F, -1.51894F,
                            0.8F, -1.74793F, 0.825F, -1.87431F, 0.85F, -1.88944F, 0.875F, -1.79833F,
                            0.9F, -1.61653F, 0.925F, -1.36769F, 0.95F, -1.08059F, 0.975F, -0.78589F,
                            1.0F, -0.51279F, 1.025F, -0.28581F, 1.05F, -0.12193F, 1.075F, -0.02817F,
                            1.1F, -0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_3,
                    ZERO_CURVE,
                    curve(0.0F, 0.0F, 0.025F, 0.06176F, 0.05F, 0.20172F, 0.075F, 0.35409F,
                            0.1F, 0.46292F, 0.125F, 0.48994F, 0.15F, 0.42063F, 0.175F, 0.26813F,
                            0.2F, 0.06913F, 0.225F, -0.05704F, 0.25F, 0.0253F, 0.275F, 0.36455F,
                            0.3F, 0.91985F, 0.325F, 1.56872F, 0.35F, 2.12258F, 0.375F, 2.42439F,
                            0.4F, 2.6391F, 0.425F, 2.78864F, 0.45F, 2.87053F, 0.475F, 2.88489F,
                            0.5F, 2.83413F, 0.525F, 2.72262F, 0.55F, 2.55633F, 0.575F, 2.34232F,
                            0.6F, 2.08835F, 0.625F, 1.80237F, 0.65F, 1.49212F, 0.675F, 1.16482F,
                            0.7F, 0.82679F, 0.725F, 0.48336F, 0.75F, 0.13867F, 0.775F, -0.20425F,
                            0.8F, -0.53637F, 0.825F, -0.8229F, 0.85F, -1.03456F, 0.875F, -1.15403F,
                            0.9F, -1.17599F, 0.925F, -1.10655F, 0.95F, -0.96171F, 0.975F, -0.76515F,
                            1.0F, -0.5455F, 1.025F, -0.33316F, 1.05F, -0.1569F, 1.075F, -0.04058F,
                            1.1F, 0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_4,
                    ZERO_CURVE,
                    curve(0.0F, -0.0F, 0.025F, 0.07063F, 0.05F, 0.25422F, 0.075F, 0.49777F,
                            0.1F, 0.74146F, 0.125F, 0.92804F, 0.15F, 1.0118F, 0.175F, 0.96661F,
                            0.2F, 0.73397F, 0.225F, 0.33004F, 0.25F, -0.04069F, 0.275F, -0.22858F,
                            0.3F, -0.16157F, 0.325F, 0.14489F, 0.35F, 0.58597F, 0.375F, 1.00535F,
                            0.4F, 1.39095F, 0.425F, 1.73889F, 0.45F, 2.03991F, 0.475F, 2.28707F,
                            0.5F, 2.47578F, 0.525F, 2.60378F, 0.55F, 2.671F, 0.575F, 2.67926F,
                            0.6F, 2.63196F, 0.625F, 2.53373F, 0.65F, 2.38995F, 0.675F, 2.2064F,
                            0.7F, 1.98881F, 0.725F, 1.74254F, 0.75F, 1.47228F, 0.775F, 1.1818F,
                            0.8F, 0.86256F, 0.825F, 0.51597F, 0.85F, 0.18172F, 0.875F, -0.10659F,
                            0.9F, -0.32465F, 0.925F, -0.45887F, 0.95F, -0.50687F, 0.975F, -0.47712F,
                            1.0F, -0.38765F, 1.025F, -0.26412F, 1.05F, -0.13706F, 1.075F, -0.03881F,
                            1.1F, -0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_TIP,
                    ZERO_CURVE,
                    curve(0.0F, -0.0F, 0.025F, 0.05484F, 0.05F, 0.21792F, 0.075F, 0.46757F,
                            0.1F, 0.76099F, 0.125F, 1.04195F, 0.15F, 1.24952F, 0.175F, 1.32743F,
                            0.2F, 1.14241F, 0.225F, 0.60182F, 0.25F, -0.09246F, 0.275F, -0.74186F,
                            0.3F, -1.18654F, 0.325F, -1.32955F, 0.35F, -1.15535F, 0.375F, -0.76488F,
                            0.4F, -0.34309F, 0.425F, 0.08171F, 0.45F, 0.4967F, 0.475F, 0.89031F,
                            0.5F, 1.25256F, 0.525F, 1.57536F, 0.55F, 1.85261F, 0.575F, 2.08024F,
                            0.6F, 2.25615F, 0.625F, 2.37998F, 0.65F, 2.4529F, 0.675F, 2.47722F,
                            0.7F, 2.45608F, 0.725F, 2.39301F, 0.75F, 2.29157F, 0.775F, 2.155F,
                            0.8F, 1.96018F, 0.825F, 1.6746F, 0.85F, 1.33452F, 0.875F, 0.97808F,
                            0.9F, 0.64011F, 0.925F, 0.34911F, 0.95F, 0.12503F, 0.975F, -0.02242F,
                            1.0F, -0.09439F, 1.025F, -0.10282F, 1.05F, -0.06934F, 1.075F, -0.02348F,
                            1.1F, -0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE));
    }

    private static SkeletonAnimation createFireDomainSkeleton() {
        return animation(2.4F, true,
            bone(ModelBone.NECK_1,
                    curve(0.0F, -0.0F, 0.025F, -0.02828F, 0.05F, -0.1092F, 0.075F, -0.23692F,
                            0.1F, -0.4056F, 0.125F, -0.60938F, 0.15F, -0.8424F, 0.175F, -1.09882F,
                            0.2F, -1.3728F, 0.225F, -1.65848F, 0.25F, -1.95F, 0.275F, -2.24153F,
                            0.3F, -2.5272F, 0.325F, -2.80118F, 0.35F, -3.0576F, 0.375F, -3.29063F,
                            0.4F, -3.4944F, 0.425F, -3.66308F, 0.45F, -3.7908F, 0.475F, -3.87173F,
                            0.5F, -3.9F, 0.525F, -3.90784F, 0.55F, -3.93061F, 0.575F, -3.96716F,
                            0.6F, -4.01633F, 0.625F, -4.07698F, 0.65F, -4.14796F, 0.675F, -4.22813F,
                            0.7F, -4.31633F, 0.725F, -4.41142F, 0.75F, -4.51224F, 0.775F, -4.61767F,
                            0.8F, -4.72653F, 0.825F, -4.83769F, 0.85F, -4.95F, 0.875F, -5.06231F,
                            0.9F, -5.17347F, 0.925F, -5.28233F, 0.95F, -5.38776F, 0.975F, -5.48858F,
                            1.0F, -5.58367F, 1.025F, -5.67188F, 1.05F, -5.75204F, 1.075F, -5.82302F,
                            1.1F, -5.88367F, 1.125F, -5.93284F, 1.15F, -5.96939F, 1.175F, -5.99216F,
                            1.2F, -6.0F, 1.225F, -5.99216F, 1.25F, -5.96939F, 1.275F, -5.93284F,
                            1.3F, -5.88367F, 1.325F, -5.82302F, 1.35F, -5.75204F, 1.375F, -5.67188F,
                            1.4F, -5.58367F, 1.425F, -5.48858F, 1.45F, -5.38776F, 1.475F, -5.28233F,
                            1.5F, -5.17347F, 1.525F, -5.06231F, 1.55F, -4.95F, 1.575F, -4.83769F,
                            1.6F, -4.72653F, 1.625F, -4.61767F, 1.65F, -4.51224F, 1.675F, -4.41142F,
                            1.7F, -4.31633F, 1.725F, -4.22812F, 1.75F, -4.14796F, 1.775F, -4.07698F,
                            1.8F, -4.01633F, 1.825F, -3.96716F, 1.85F, -3.93061F, 1.875F, -3.90784F,
                            1.9F, -3.9F, 1.925F, -3.87173F, 1.95F, -3.7908F, 1.975F, -3.66307F,
                            2.0F, -3.4944F, 2.025F, -3.29063F, 2.05F, -3.0576F, 2.075F, -2.80117F,
                            2.1F, -2.5272F, 2.125F, -2.24152F, 2.15F, -1.95F, 2.175F, -1.65848F,
                            2.2F, -1.3728F, 2.225F, -1.09882F, 2.25F, -0.8424F, 2.275F, -0.60938F,
                            2.3F, -0.4056F, 2.325F, -0.23692F, 2.35F, -0.1092F, 2.375F, -0.02828F,
                            2.4F, -0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.NECK_2,
                    curve(0.0F, 0.0F, 0.025F, -0.04241F, 0.05F, -0.1638F, 0.075F, -0.35539F,
                            0.1F, -0.6084F, 0.125F, -0.91406F, 0.15F, -1.2636F, 0.175F, -1.64825F,
                            0.2F, -2.0592F, 0.225F, -2.48772F, 0.25F, -2.925F, 0.275F, -3.36229F,
                            0.3F, -3.7908F, 0.325F, -4.20177F, 0.35F, -4.5864F, 0.375F, -4.93595F,
                            0.4F, -5.2416F, 0.425F, -5.49461F, 0.45F, -5.68621F, 0.475F, -5.80759F,
                            0.5F, -5.85F, 0.525F, -5.86177F, 0.55F, -5.89593F, 0.575F, -5.95074F,
                            0.6F, -6.02449F, 0.625F, -6.11547F, 0.65F, -6.22194F, 0.675F, -6.3422F,
                            0.7F, -6.4745F, 0.725F, -6.61712F, 0.75F, -6.76837F, 0.775F, -6.9265F,
                            0.8F, -7.08981F, 0.825F, -7.25655F, 0.85F, -7.425F, 0.875F, -7.59346F,
                            0.9F, -7.7602F, 0.925F, -7.92351F, 0.95F, -8.08164F, 0.975F, -8.23288F,
                            1.0F, -8.37552F, 1.025F, -8.50781F, 1.05F, -8.62806F, 1.075F, -8.73453F,
                            1.1F, -8.82552F, 1.125F, -8.89928F, 1.15F, -8.95409F, 1.175F, -8.98823F,
                            1.2F, -9.0F, 1.225F, -8.98823F, 1.25F, -8.95409F, 1.275F, -8.89928F,
                            1.3F, -8.82552F, 1.325F, -8.73453F, 1.35F, -8.62806F, 1.375F, -8.50781F,
                            1.4F, -8.37552F, 1.425F, -8.23288F, 1.45F, -8.08164F, 1.475F, -7.92351F,
                            1.5F, -7.7602F, 1.525F, -7.59346F, 1.55F, -7.425F, 1.575F, -7.25655F,
                            1.6F, -7.08981F, 1.625F, -6.9265F, 1.65F, -6.76837F, 1.675F, -6.61712F,
                            1.7F, -6.4745F, 1.725F, -6.34219F, 1.75F, -6.22194F, 1.775F, -6.11547F,
                            1.8F, -6.02449F, 1.825F, -5.95074F, 1.85F, -5.89593F, 1.875F, -5.86177F,
                            1.9F, -5.85F, 1.925F, -5.80759F, 1.95F, -5.68621F, 1.975F, -5.49461F,
                            2.0F, -5.2416F, 2.025F, -4.93595F, 2.05F, -4.5864F, 2.075F, -4.20176F,
                            2.1F, -3.7908F, 2.125F, -3.3623F, 2.15F, -2.925F, 2.175F, -2.48772F,
                            2.2F, -2.0592F, 2.225F, -1.64825F, 2.25F, -1.2636F, 2.275F, -0.91406F,
                            2.3F, -0.6084F, 2.325F, -0.35539F, 2.35F, -0.1638F, 2.375F, -0.04241F,
                            2.4F, 0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.HEAD,
                    curve(0.0F, -0.0F, 0.025F, -0.01414F, 0.05F, -0.0546F, 0.075F, -0.11846F,
                            0.1F, -0.20279F, 0.125F, -0.30469F, 0.15F, -0.4212F, 0.175F, -0.54941F,
                            0.2F, -0.6864F, 0.225F, -0.82924F, 0.25F, -0.97499F, 0.275F, -1.12076F,
                            0.3F, -1.2636F, 0.325F, -1.40059F, 0.35F, -1.5288F, 0.375F, -1.64531F,
                            0.4F, -1.7472F, 0.425F, -1.83154F, 0.45F, -1.8954F, 0.475F, -1.93585F,
                            0.5F, -1.94999F, 0.525F, -1.95392F, 0.55F, -1.96531F, 0.575F, -1.98358F,
                            0.6F, -2.00816F, 0.625F, -2.03849F, 0.65F, -2.07398F, 0.675F, -2.11407F,
                            0.7F, -2.15816F, 0.725F, -2.20571F, 0.75F, -2.25612F, 0.775F, -2.30883F,
                            0.8F, -2.36327F, 0.825F, -2.41885F, 0.85F, -2.475F, 0.875F, -2.53115F,
                            0.9F, -2.58673F, 0.925F, -2.64117F, 0.95F, -2.69388F, 0.975F, -2.7443F,
                            1.0F, -2.79184F, 1.025F, -2.83594F, 1.05F, -2.87602F, 1.075F, -2.91151F,
                            1.1F, -2.94185F, 1.125F, -2.96642F, 1.15F, -2.98469F, 1.175F, -2.99608F,
                            1.2F, -3.0F, 1.225F, -2.99608F, 1.25F, -2.98469F, 1.275F, -2.96642F,
                            1.3F, -2.94185F, 1.325F, -2.91151F, 1.35F, -2.87602F, 1.375F, -2.83594F,
                            1.4F, -2.79184F, 1.425F, -2.7443F, 1.45F, -2.69388F, 1.475F, -2.64117F,
                            1.5F, -2.58673F, 1.525F, -2.53115F, 1.55F, -2.475F, 1.575F, -2.41885F,
                            1.6F, -2.36327F, 1.625F, -2.30883F, 1.65F, -2.25612F, 1.675F, -2.20571F,
                            1.7F, -2.15816F, 1.725F, -2.11407F, 1.75F, -2.07398F, 1.775F, -2.03849F,
                            1.8F, -2.00816F, 1.825F, -1.98358F, 1.85F, -1.96531F, 1.875F, -1.95392F,
                            1.9F, -1.94999F, 1.925F, -1.93585F, 1.95F, -1.8954F, 1.975F, -1.83154F,
                            2.0F, -1.7472F, 2.025F, -1.64531F, 2.05F, -1.5288F, 2.075F, -1.40059F,
                            2.1F, -1.2636F, 2.125F, -1.12076F, 2.15F, -0.97499F, 2.175F, -0.82924F,
                            2.2F, -0.6864F, 2.225F, -0.54941F, 2.25F, -0.4212F, 2.275F, -0.30469F,
                            2.3F, -0.20279F, 2.325F, -0.11846F, 2.35F, -0.0546F, 2.375F, -0.01414F,
                            2.4F, -0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.JAW,
                    curve(0.0F, 0.0F, 0.025F, 0.08011F, 0.05F, 0.3094F, 0.075F, 0.67129F,
                            0.1F, 1.1492F, 0.125F, 1.72656F, 0.15F, 2.3868F, 0.175F, 3.11334F,
                            0.2F, 3.8896F, 0.225F, 4.69901F, 0.25F, 5.525F, 0.275F, 6.35099F,
                            0.3F, 7.1604F, 0.325F, 7.93666F, 0.35F, 8.6632F, 0.375F, 9.32344F,
                            0.4F, 9.9008F, 0.425F, 10.37871F, 0.45F, 10.7406F, 0.475F, 10.96989F,
                            0.5F, 11.05F, 0.525F, 11.07223F, 0.55F, 11.13673F, 0.575F, 11.24027F,
                            0.6F, 11.37959F, 0.625F, 11.55143F, 0.65F, 11.75255F, 0.675F, 11.97969F,
                            0.7F, 12.22959F, 0.725F, 12.49901F, 0.75F, 12.78469F, 0.775F, 13.08339F,
                            0.8F, 13.39184F, 0.825F, 13.70679F, 0.85F, 14.025F, 0.875F, 14.34321F,
                            0.9F, 14.65816F, 0.925F, 14.96661F, 0.95F, 15.26531F, 0.975F, 15.55099F,
                            1.0F, 15.82041F, 1.025F, 16.07032F, 1.05F, 16.29745F, 1.075F, 16.49857F,
                            1.1F, 16.67041F, 1.125F, 16.80973F, 1.15F, 16.91328F, 1.175F, 16.97778F,
                            1.2F, 17.0F, 1.225F, 16.97778F, 1.25F, 16.91328F, 1.275F, 16.80973F,
                            1.3F, 16.67041F, 1.325F, 16.49857F, 1.35F, 16.29745F, 1.375F, 16.07032F,
                            1.4F, 15.82041F, 1.425F, 15.55099F, 1.45F, 15.26531F, 1.475F, 14.96661F,
                            1.5F, 14.65816F, 1.525F, 14.34321F, 1.55F, 14.025F, 1.575F, 13.70679F,
                            1.6F, 13.39184F, 1.625F, 13.08339F, 1.65F, 12.78469F, 1.675F, 12.49901F,
                            1.7F, 12.22959F, 1.725F, 11.97969F, 1.75F, 11.75255F, 1.775F, 11.55143F,
                            1.8F, 11.37959F, 1.825F, 11.24027F, 1.85F, 11.13673F, 1.875F, 11.07223F,
                            1.9F, 11.05F, 1.925F, 10.96989F, 1.95F, 10.7406F, 1.975F, 10.37871F,
                            2.0F, 9.9008F, 2.025F, 9.32344F, 2.05F, 8.6632F, 2.075F, 7.93666F,
                            2.1F, 7.1604F, 2.125F, 6.35099F, 2.15F, 5.525F, 2.175F, 4.69901F,
                            2.2F, 3.8896F, 2.225F, 3.11334F, 2.25F, 2.3868F, 2.275F, 1.72656F,
                            2.3F, 1.1492F, 2.325F, 0.67129F, 2.35F, 0.3094F, 2.375F, 0.08011F,
                            2.4F, 0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_1,
                    ZERO_CURVE,
                    curve(0.0F, -0.0F, 0.025F, 0.00092F, 0.05F, 0.00713F, 0.075F, 0.02311F,
                            0.1F, 0.05249F, 0.125F, 0.09794F, 0.15F, 0.16119F, 0.175F, 0.243F,
                            0.2F, 0.3432F, 0.225F, 0.4607F, 0.25F, 0.59354F, 0.275F, 0.73897F,
                            0.3F, 0.8935F, 0.325F, 1.05302F, 0.35F, 1.21288F, 0.375F, 1.36803F,
                            0.4F, 1.51312F, 0.425F, 1.64266F, 0.45F, 1.75112F, 0.475F, 1.83313F,
                            0.5F, 1.88356F, 0.525F, 1.91638F, 0.55F, 1.94849F, 0.575F, 1.97933F,
                            0.6F, 2.00816F, 0.625F, 2.03412F, 0.65F, 2.05624F, 0.675F, 2.07344F,
                            0.7F, 2.08463F, 0.725F, 2.08865F, 0.75F, 2.08439F, 0.775F, 2.07073F,
                            0.8F, 2.04665F, 0.825F, 2.0112F, 0.85F, 1.96355F, 0.875F, 1.90302F,
                            0.9F, 1.8291F, 0.925F, 1.74144F, 0.95F, 1.63993F, 0.975F, 1.52465F,
                            1.0F, 1.39592F, 1.025F, 1.2543F, 1.05F, 1.10061F, 1.075F, 0.93587F,
                            1.1F, 0.7614F, 1.125F, 0.57872F, 1.15F, 0.38958F, 1.175F, 0.19595F,
                            1.2F, -0.0F, 1.225F, -0.19595F, 1.25F, -0.38958F, 1.275F, -0.57872F,
                            1.3F, -0.7614F, 1.325F, -0.93587F, 1.35F, -1.10061F, 1.375F, -1.2543F,
                            1.4F, -1.39592F, 1.425F, -1.52465F, 1.45F, -1.63993F, 1.475F, -1.74144F,
                            1.5F, -1.8291F, 1.525F, -1.90302F, 1.55F, -1.96355F, 1.575F, -2.0112F,
                            1.6F, -2.04665F, 1.625F, -2.07073F, 1.65F, -2.08439F, 1.675F, -2.08865F,
                            1.7F, -2.08463F, 1.725F, -2.07344F, 1.75F, -2.05624F, 1.775F, -2.03412F,
                            1.8F, -2.00816F, 1.825F, -1.97933F, 1.85F, -1.94849F, 1.875F, -1.91638F,
                            1.9F, -1.88356F, 1.925F, -1.83313F, 1.95F, -1.75112F, 1.975F, -1.64266F,
                            2.0F, -1.51312F, 2.025F, -1.36803F, 2.05F, -1.21288F, 2.075F, -1.05302F,
                            2.1F, -0.8935F, 2.125F, -0.73897F, 2.15F, -0.59354F, 2.175F, -0.4607F,
                            2.2F, -0.3432F, 2.225F, -0.243F, 2.25F, -0.16119F, 2.275F, -0.09794F,
                            2.3F, -0.05249F, 2.325F, -0.02311F, 2.35F, -0.00713F, 2.375F, -0.00092F,
                            2.4F, -0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_2,
                    ZERO_CURVE,
                    curve(0.0F, -0.0F, 0.025F, -0.0072F, 0.05F, -0.02468F, 0.075F, -0.04653F,
                            0.1F, -0.06729F, 0.125F, -0.08208F, 0.15F, -0.08669F, 0.175F, -0.07767F,
                            0.2F, -0.05239F, 0.225F, -0.00908F, 0.25F, 0.05311F, 0.275F, 0.13411F,
                            0.3F, 0.23293F, 0.325F, 0.34766F, 0.35F, 0.47553F, 0.375F, 0.61295F,
                            0.4F, 0.75556F, 0.425F, 0.89834F, 0.45F, 1.03571F, 0.475F, 1.16159F,
                            0.5F, 1.26959F, 0.525F, 1.36642F, 0.55F, 1.46332F, 0.575F, 1.56036F,
                            0.6F, 1.65741F, 0.625F, 1.75412F, 0.65F, 1.84994F, 0.675F, 1.94416F,
                            0.7F, 2.03591F, 0.725F, 2.12417F, 0.75F, 2.20782F, 0.775F, 2.28564F,
                            0.8F, 2.35637F, 0.825F, 2.4187F, 0.85F, 2.47133F, 0.875F, 2.51297F,
                            0.9F, 2.54241F, 0.925F, 2.5585F, 0.95F, 2.56024F, 0.975F, 2.54674F,
                            1.0F, 2.51729F, 1.025F, 2.47138F, 1.05F, 2.40868F, 1.075F, 2.32913F,
                            1.1F, 2.2329F, 1.125F, 2.12042F, 1.15F, 1.9924F, 1.175F, 1.84982F,
                            1.2F, 1.69393F, 1.225F, 1.52636F, 1.25F, 1.34933F, 1.275F, 1.16515F,
                            1.3F, 0.97607F, 1.325F, 0.78431F, 1.35F, 0.59194F, 1.375F, 0.40093F,
                            1.4F, 0.21309F, 1.425F, 0.03005F, 1.45F, -0.14674F, 1.475F, -0.31604F,
                            1.5F, -0.47683F, 1.525F, -0.6283F, 1.55F, -0.76985F, 1.575F, -0.90112F,
                            1.6F, -1.02197F, 1.625F, -1.13245F, 1.65F, -1.23282F, 1.675F, -1.32351F,
                            1.7F, -1.40512F, 1.725F, -1.47841F, 1.75F, -1.54423F, 1.775F, -1.60355F,
                            1.8F, -1.65741F, 1.825F, -1.70686F, 1.85F, -1.753F, 1.875F, -1.79689F,
                            1.9F, -1.83954F, 1.925F, -1.8643F, 1.95F, -1.85482F, 1.975F, -1.81314F,
                            2.0F, -1.7421F, 2.025F, -1.64521F, 2.05F, -1.52653F, 2.075F, -1.39052F,
                            2.1F, -1.24195F, 2.125F, -1.08569F, 2.15F, -0.92663F, 2.175F, -0.76954F,
                            2.2F, -0.6189F, 2.225F, -0.47879F, 2.25F, -0.35276F, 2.275F, -0.24374F,
                            2.3F, -0.15393F, 2.325F, -0.08468F, 2.35F, -0.03645F, 2.375F, -0.00873F,
                            2.4F, -0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_3,
                    ZERO_CURVE,
                    curve(0.0F, 0.0F, 0.025F, -0.01281F, 0.05F, -0.04787F, 0.075F, -0.09992F,
                            0.1F, -0.16356F, 0.125F, -0.23342F, 0.15F, -0.30428F, 0.175F, -0.37121F,
                            0.2F, -0.42967F, 0.225F, -0.47569F, 0.25F, -0.50588F, 0.275F, -0.5176F,
                            0.3F, -0.50901F, 0.325F, -0.47914F, 0.35F, -0.42793F, 0.375F, -0.35625F,
                            0.4F, -0.26594F, 0.425F, -0.15979F, 0.45F, -0.04151F, 0.475F, 0.08427F,
                            0.5F, 0.21212F, 0.525F, 0.33913F, 0.55F, 0.46696F, 0.575F, 0.59631F,
                            0.6F, 0.72767F, 0.625F, 0.86134F, 0.65F, 0.9974F, 0.675F, 1.13573F,
                            0.7F, 1.27599F, 0.725F, 1.41766F, 0.75F, 1.55999F, 0.775F, 1.70212F,
                            0.8F, 1.84295F, 0.825F, 1.98128F, 0.85F, 2.1158F, 0.875F, 2.24506F,
                            0.9F, 2.36758F, 0.925F, 2.48181F, 0.95F, 2.58619F, 0.975F, 2.67919F,
                            1.0F, 2.75931F, 1.025F, 2.82512F, 1.05F, 2.87533F, 1.075F, 2.90875F,
                            1.1F, 2.92438F, 1.125F, 2.9214F, 1.15F, 2.89922F, 1.175F, 2.85749F,
                            1.2F, 2.79612F, 1.225F, 2.71548F, 1.25F, 2.61688F, 1.275F, 2.50199F,
                            1.3F, 2.37258F, 1.325F, 2.23051F, 1.35F, 2.0777F, 1.375F, 1.91611F,
                            1.4F, 1.74766F, 1.425F, 1.57425F, 1.45F, 1.39771F, 1.475F, 1.21976F,
                            1.5F, 1.042F, 1.525F, 0.86591F, 1.55F, 0.69278F, 1.575F, 0.52374F,
                            1.6F, 0.35971F, 1.625F, 0.20143F, 1.65F, 0.04941F, 1.675F, -0.09602F,
                            1.7F, -0.23477F, 1.725F, -0.36692F, 1.75F, -0.49278F, 1.775F, -0.61282F,
                            1.8F, -0.72767F, 1.825F, -0.83814F, 1.85F, -0.94514F, 1.875F, -1.0497F,
                            1.9F, -1.15292F, 1.925F, -1.24422F, 1.95F, -1.31058F, 1.975F, -1.35024F,
                            2.0F, -1.36252F, 2.025F, -1.34768F, 2.05F, -1.30692F, 2.075F, -1.24228F,
                            2.1F, -1.15653F, 2.125F, -1.05314F, 2.15F, -0.93603F, 2.175F, -0.80957F,
                            2.2F, -0.67839F, 2.225F, -0.54731F, 2.25F, -0.4211F, 2.275F, -0.3044F,
                            2.3F, -0.2016F, 2.325F, -0.11666F, 2.35F, -0.05304F, 2.375F, -0.01348F,
                            2.4F, 0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_4,
                    ZERO_CURVE,
                    curve(0.0F, -0.0F, 0.025F, -0.01395F, 0.05F, -0.05434F, 0.075F, -0.1184F,
                            0.1F, -0.20269F, 0.125F, -0.30322F, 0.15F, -0.41558F, 0.175F, -0.53508F,
                            0.2F, -0.65687F, 0.225F, -0.77613F, 0.25F, -0.88814F, 0.275F, -0.98849F,
                            0.3F, -1.07314F, 0.325F, -1.13857F, 0.35F, -1.1819F, 0.375F, -1.201F,
                            0.4F, -1.19454F, 0.425F, -1.1621F, 0.45F, -1.10423F, 0.475F, -1.02248F,
                            0.5F, -0.91945F, 0.525F, -0.80663F, 0.55F, -0.69252F, 0.575F, -0.57605F,
                            0.6F, -0.45626F, 0.625F, -0.33232F, 0.65F, -0.20355F, 0.675F, -0.06944F,
                            0.7F, 0.07033F, 0.725F, 0.21591F, 0.75F, 0.36722F, 0.775F, 0.52399F,
                            0.8F, 0.68573F, 0.825F, 0.85175F, 0.85F, 1.02116F, 0.875F, 1.19289F,
                            0.9F, 1.36569F, 0.925F, 1.53814F, 0.95F, 1.70871F, 0.975F, 1.87572F,
                            1.0F, 2.03741F, 1.025F, 2.19198F, 1.05F, 2.33755F, 1.075F, 2.47226F,
                            1.1F, 2.59429F, 1.125F, 2.70185F, 1.15F, 2.79326F, 1.175F, 2.86696F,
                            1.2F, 2.92154F, 1.225F, 2.956F, 1.25F, 2.97028F, 1.275F, 2.96482F,
                            1.3F, 2.94027F, 1.325F, 2.89753F, 1.35F, 2.83767F, 1.375F, 2.76194F,
                            1.4F, 2.67173F, 1.425F, 2.56852F, 1.45F, 2.4539F, 1.475F, 2.32946F,
                            1.5F, 2.19684F, 1.525F, 2.05763F, 1.55F, 1.9134F, 1.575F, 1.76564F,
                            1.6F, 1.61573F, 1.625F, 1.46494F, 1.65F, 1.31438F, 1.675F, 1.16501F,
                            1.7F, 1.0176F, 1.725F, 0.87274F, 1.75F, 0.73081F, 1.775F, 0.59199F,
                            1.8F, 0.45626F, 1.825F, 0.32337F, 1.85F, 0.19289F, 1.875F, 0.06418F,
                            1.9F, -0.06355F, 1.925F, -0.1895F, 1.95F, -0.30851F, 1.975F, -0.41567F,
                            2.0F, -0.50697F, 2.025F, -0.57936F, 2.05F, -0.63077F, 2.075F, -0.66007F,
                            2.1F, -0.66713F, 2.125F, -0.6527F, 2.15F, -0.61844F, 2.175F, -0.56678F,
                            2.2F, -0.50092F, 2.225F, -0.42466F, 2.25F, -0.34234F, 2.275F, -0.25872F,
                            2.3F, -0.17884F, 2.325F, -0.1079F, 2.35F, -0.0511F, 2.375F, -0.01353F,
                            2.4F, -0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE),
            bone(ModelBone.TAIL_TIP,
                    ZERO_CURVE,
                    curve(0.0F, -0.0F, 0.025F, -0.01021F, 0.05F, -0.04182F, 0.075F, -0.09552F,
                            0.1F, -0.17102F, 0.125F, -0.2671F, 0.15F, -0.38171F, 0.175F, -0.51202F,
                            0.2F, -0.6546F, 0.225F, -0.80544F, 0.25F, -0.96016F, 0.275F, -1.11408F,
                            0.3F, -1.26239F, 0.325F, -1.40026F, 0.35F, -1.52301F, 0.375F, -1.62621F,
                            0.4F, -1.70585F, 0.425F, -1.75846F, 0.45F, -1.78121F, 0.475F, -1.77205F,
                            0.5F, -1.72983F, 0.525F, -1.67061F, 0.55F, -1.61008F, 0.575F, -1.54718F,
                            0.6F, -1.48081F, 0.625F, -1.4099F, 0.65F, -1.3334F, 0.675F, -1.25036F,
                            0.7F, -1.15989F, 0.725F, -1.06125F, 0.75F, -0.95383F, 0.775F, -0.83718F,
                            0.8F, -0.71104F, 0.825F, -0.57533F, 0.85F, -0.4302F, 0.875F, -0.27599F,
                            0.9F, -0.11328F, 0.925F, 0.05716F, 0.95F, 0.23432F, 0.975F, 0.417F,
                            1.0F, 0.60379F, 1.025F, 0.79311F, 1.05F, 0.98319F, 1.075F, 1.17214F,
                            1.1F, 1.35794F, 1.125F, 1.53846F, 1.15F, 1.71153F, 1.175F, 1.87491F,
                            1.2F, 2.02639F, 1.225F, 2.1639F, 1.25F, 2.28608F, 1.275F, 2.39195F,
                            1.3F, 2.48085F, 1.325F, 2.55236F, 1.35F, 2.60635F, 1.375F, 2.64294F,
                            1.4F, 2.66248F, 1.425F, 2.66553F, 1.45F, 2.65287F, 1.475F, 2.62542F,
                            1.5F, 2.58425F, 1.525F, 2.53056F, 1.55F, 2.46562F, 1.575F, 2.39076F,
                            1.6F, 2.30732F, 1.625F, 2.21671F, 1.65F, 2.12019F, 1.675F, 2.01906F,
                            1.7F, 1.91449F, 1.725F, 1.80753F, 1.75F, 1.69911F, 1.775F, 1.59001F,
                            1.8F, 1.48081F, 1.825F, 1.37192F, 1.85F, 1.26353F, 1.875F, 1.15564F,
                            1.9F, 1.04802F, 1.925F, 0.93142F, 1.95F, 0.80133F, 1.975F, 0.66411F,
                            2.0F, 0.52568F, 2.025F, 0.39134F, 2.05F, 0.26573F, 2.075F, 0.15272F,
                            2.1F, 0.05533F, 2.125F, -0.02426F, 2.15F, -0.08481F, 2.175F, -0.12601F,
                            2.2F, -0.14845F, 2.225F, -0.15365F, 2.25F, -0.14399F, 2.275F, -0.12266F,
                            2.3F, -0.09361F, 2.325F, -0.06144F, 2.35F, -0.03131F, 2.375F, -0.00885F,
                            2.4F, -0.0F),
                    ZERO_CURVE,
                    ZERO_CURVE));
    }

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final Set<Integer> rushHitEntities = new HashSet<>();
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> bodyFront;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> bodyMiddle;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> bodyRear;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> neck1;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> neck2;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> headPart;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> jawPart;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> tail1Front;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> tail1Middle;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> tail1Rear;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> tail2Front;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> tail2Middle;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> tail2Rear;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> tail3Front;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> tail3Middle;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> tail3Rear;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> tail4Front;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> tail4Middle;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> tail4Rear;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> tailTipFront;
    private final ModelBonePartEntity<CrimsonSkyFlameFloodDragonEntity> tailTipMiddle;
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
    private float previousLookPitch,lookPitch;

    public CrimsonSkyFlameFloodDragonEntity(
            EntityType<? extends CrimsonSkyFlameFloodDragonEntity> entityType, Level level) {
        super(entityType, level);
        bodyFront = new ModelBonePartEntity<>(this, "body_core_front", 3.8F, 2.4F);
        bodyMiddle = new ModelBonePartEntity<>(this, "body_core_middle", 3.0F, 2.3F);
        bodyRear = new ModelBonePartEntity<>(this, "body_core_rear", 3.4F, 2.3F);
        neck1 = new ModelBonePartEntity<>(this, "neck_1", 3.2F, 2.1F);
        neck2 = new ModelBonePartEntity<>(this, "neck_2", 2.7F, 1.8F);
        headPart = new ModelBonePartEntity<>(this, "head", 2.4F, 1.8F);
        jawPart = new ModelBonePartEntity<>(this, "jaw", 2.0F, 1.0F);
        tail1Front = new ModelBonePartEntity<>(this, "tail_1_front", 2.45F, 2.0F);
        tail1Middle = new ModelBonePartEntity<>(this, "tail_1_middle", 2.4F, 1.95F);
        tail1Rear = new ModelBonePartEntity<>(this, "tail_1_rear", 2.35F, 1.95F);
        tail2Front = new ModelBonePartEntity<>(this, "tail_2_front", 2.1F, 1.7F);
        tail2Middle = new ModelBonePartEntity<>(this, "tail_2_middle", 2.05F, 1.7F);
        tail2Rear = new ModelBonePartEntity<>(this, "tail_2_rear", 2.0F, 1.65F);
        tail3Front = new ModelBonePartEntity<>(this, "tail_3_front", 1.75F, 1.5F);
        tail3Middle = new ModelBonePartEntity<>(this, "tail_3_middle", 1.7F, 1.5F);
        tail3Rear = new ModelBonePartEntity<>(this, "tail_3_rear", 1.65F, 1.45F);
        tail4Front = new ModelBonePartEntity<>(this, "tail_4_front", 1.4F, 1.25F);
        tail4Middle = new ModelBonePartEntity<>(this, "tail_4_middle", 1.35F, 1.2F);
        tail4Rear = new ModelBonePartEntity<>(this, "tail_4_rear", 1.3F, 1.15F);
        tailTipFront = new ModelBonePartEntity<>(this, "tail_tip_front", 1.1F, 0.9F);
        tailTipMiddle = new ModelBonePartEntity<>(this, "tail_tip_middle", 1.0F, 0.85F);
        tailTipRear = new ModelBonePartEntity<>(this, "tail_tip_rear", 0.9F, 0.8F);
        boneParts = new ModelBonePartEntity<?>[]{
                bodyFront, bodyMiddle, bodyRear, neck1, neck2, headPart, jawPart,
                tail1Front, tail1Middle, tail1Rear, tail2Front, tail2Middle, tail2Rear,
                tail3Front, tail3Middle, tail3Rear, tail4Front, tail4Middle, tail4Rear,
                tailTipFront, tailTipMiddle, tailTipRear
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

        boolean naturalSpawn = spawnType == MobSpawnType.NATURAL
                || spawnType == MobSpawnType.CHUNK_GENERATION;
        if (naturalSpawn) {
            if (random.nextInt(NATURAL_SPAWN_RARITY) != 0) {
                return false;
            }
            AABB exclusionArea = new AABB(pos).inflate(
                    NATURAL_SPAWN_EXCLUSION_RADIUS,
                    NATURAL_SPAWN_EXCLUSION_HEIGHT,
                    NATURAL_SPAWN_EXCLUSION_RADIUS);
            if (!level.getEntitiesOfClass(
                    CrimsonSkyFlameFloodDragonEntity.class,
                    exclusionArea,
                    LivingEntity::isAlive).isEmpty()) {
                return false;
            }
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
            if (BeastDazeControl.blocksMoves(dragon)) {
                dragon.getNavigation().stop();
                return;
            }
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
        builder.define(LOOK_PITCH, 0F);
    }

    @Override
    public void tick() {
        super.tick();
        previousLookPitch=lookPitch;
        if(!level().isClientSide) {
            // Attack clips retain their authored silhouettes; tracking eases out when an attack starts.
            lookPitch=DragonLookPose.advance(lookPitch,isAlive() && getActionState()==ACTION_NONE?getXRot():0);
            entityData.set(LOOK_PITCH,lookPitch);
        } else lookPitch=entityData.get(LOOK_PITCH);
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
        BonePose body = root.child(0.0D,
                13.0D + bodyMotion.positionY().sample(time),
                2.8D + bodyMotion.positionZ().sample(time),
                anglePitch(animation, ModelBone.BODY_CORE, time),
                angleYaw(animation, ModelBone.BODY_CORE, time));

        BonePose neckOne = body.child(0.0D, 2.0D, -14.0D,
                anglePitch(animation, ModelBone.NECK_1, time)+DragonLookPose.rotation("neck_1",lookPitch),
                angleYaw(animation, ModelBone.NECK_1, time));

        BonePose neckTwo = neckOne.child(0.0D, 1.0D, -16.8D,
                anglePitch(animation, ModelBone.NECK_2, time)+DragonLookPose.rotation("neck_2",lookPitch),
                angleYaw(animation, ModelBone.NECK_2, time));

        BonePose head = neckTwo.child(0.0D, 0.99999D, -18.2D,
                anglePitch(animation, ModelBone.HEAD, time)+DragonLookPose.rotation("head",lookPitch),
                angleYaw(animation, ModelBone.HEAD, time));

        BonePose jaw = head.child(0.0D, -3.3D, -7.00001D,
                anglePitch(animation, ModelBone.JAW, time),
                angleYaw(animation, ModelBone.JAW, time));

        BonePose tailOne = body.child(0.0D, 0.0D, 12.6D,
                anglePitch(animation, ModelBone.TAIL_1, time),
                angleYaw(animation, ModelBone.TAIL_1, time));

        BonePose tailTwo = tailOne.child(0.0D, 0.0D, 22.4D,
                anglePitch(animation, ModelBone.TAIL_2, time),
                angleYaw(animation, ModelBone.TAIL_2, time));

        BonePose tailThree = tailTwo.child(-0.0D, 0.0D, 23.8D,
                anglePitch(animation, ModelBone.TAIL_3, time),
                angleYaw(animation, ModelBone.TAIL_3, time));

        BonePose tailFour = tailThree.child(0.0D, -1e-05D, 23.8D,
                anglePitch(animation, ModelBone.TAIL_4, time),
                angleYaw(animation, ModelBone.TAIL_4, time));

        BonePose tailTip = tailFour.child(-0.0D, -1e-05D, 22.4D,
                anglePitch(animation, ModelBone.TAIL_TIP, time),
                angleYaw(animation, ModelBone.TAIL_TIP, time));

        // Two samples plus a middle sample keep the longer body and tail covered.
        bodyFront.moveCenterTo(body.point(0.0D, 2.7D, -17.0D));
        bodyMiddle.moveCenterTo(body.point(0.0D, 2.7D, 0.0D));
        bodyRear.moveCenterTo(body.point(0.0D, 2.7D, 15.0D));
        neck1.moveCenterTo(neckOne.point(0.0D, 0.2D, -16.8D));
        neck2.moveCenterTo(neckTwo.point(0.0D, -0.2D, -11.2D));
        headPart.moveCenterTo(head.point(0.0D, 0.7D, -6.0D));
        jawPart.moveCenterTo(jaw.point(0.0D, -1.0D, -7.0D));

        tail1Front.moveCenterTo(tailOne.point(0.0D, 0.0D, 7.0D));
        tail1Middle.moveCenterTo(tailOne.point(0.0D, 0.0D, 16.8D));
        tail1Rear.moveCenterTo(tailOne.point(0.0D, 0.0D, 26.6D));
        tail2Front.moveCenterTo(tailTwo.point(0.0D, 0.0D, 7.0D));
        tail2Middle.moveCenterTo(tailTwo.point(0.0D, 0.0D, 16.1D));
        tail2Rear.moveCenterTo(tailTwo.point(0.0D, 0.0D, 25.2D));
        tail3Front.moveCenterTo(tailThree.point(0.0D, 0.0D, 5.6D));
        tail3Middle.moveCenterTo(tailThree.point(0.0D, 0.0D, 15.4D));
        tail3Rear.moveCenterTo(tailThree.point(0.0D, 0.0D, 25.2D));
        tail4Front.moveCenterTo(tailFour.point(0.0D, 0.0D, 5.6D));
        tail4Middle.moveCenterTo(tailFour.point(0.0D, 0.0D, 14.7D));
        tail4Rear.moveCenterTo(tailFour.point(0.0D, 0.0D, 23.8D));
        tailTipFront.moveCenterTo(tailTip.point(0.0D, 0.0D, 5.6D));
        tailTipMiddle.moveCenterTo(tailTip.point(0.0D, 0.0D, 16.1D));
        tailTipRear.moveCenterTo(tailTip.point(0.0D, 0.0D, 26.6D));
    }

    public float getLookPitch(float partialTick) {
        return Mth.lerp(Mth.clamp(partialTick,0,1),previousLookPitch,lookPitch);
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
            case NECK_1 -> -16.00001F;
            case NECK_2 -> -17.00002F;
            case HEAD -> 17.00006F;
            case JAW -> 3.99998F;
            case TAIL_1 -> 4.0F;
            case TAIL_2 -> 1.0F;
            case TAIL_3 -> -1.99995F;
            case TAIL_4 -> -3.00002F;
            case TAIL_TIP -> -4e-05F;
            default -> 0.0F;
        };
    }

    private static float baseYaw(ModelBone bone) {
        return switch (bone) {
            case TAIL_1 -> 12.0F;
            case TAIL_2 -> -19.99999F;
            case TAIL_3 -> -14.00001F;
            case TAIL_4 -> 22.00001F;
            case TAIL_TIP -> 15.99999F;
            default -> 0.0F;
        };
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
        if (!BlockBreakingSettings.enabled(this,
                BlockBreakingSettings.Category.COMPANIONS)) return;
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
            BeastMoveBlockBreaker.breakSphere(level, this, position(), 5, 2.5, 96, 50);
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
        if (ticks % 4 == 0) destroyAbilityBlocks(level,
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
        BeastMoveBlockBreaker.breakBreath(level, this, breathOrigin(forward), forward, BREATH_RANGE);
    }

    private int destroyAbilityBlocks(ServerLevel level, AABB area,
                                     int maximumBlocks, Vec3 attackOrigin) {
        return BeastMoveBlockBreaker.breakBox(level, this, area, attackOrigin, maximumBlocks, 50);
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
        return !BeastDazeControl.blocksMoves(this)
                && getActionState() == ACTION_NONE
                && super.doHurtTarget(target);
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
        if (BeastDazeControl.blocksMoves(this)) {
            return;
        }
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
