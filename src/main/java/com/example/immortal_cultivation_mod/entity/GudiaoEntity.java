package com.example.immortal_cultivation_mod.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class GudiaoEntity extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Integer> ATTACK_TICKS =
            SynchedEntityData.defineId(GudiaoEntity.class, EntityDataSerializers.INT);
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("animation.immortal_cultivation_mod.gudiao.walk");
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.immortal_cultivation_mod.gudiao.idle");
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("animation.immortal_cultivation_mod.gudiao.attack");
    private static final RawAnimation COLLAPSE_ANIM = RawAnimation.begin().thenPlayAndHold("animation.immortal_cultivation_mod.gudiao.collapse");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int scratchCooldown;

    public GudiaoEntity(EntityType<? extends GudiaoEntity> entityType, Level level) {
        super(entityType, level);
        xpReward = 55;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 700.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.ATTACK_DAMAGE, 18.0D)
                .add(Attributes.FOLLOW_RANGE, 36.0D)
                .add(Attributes.ARMOR, 8.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ATTACK_TICKS, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (getAttackTicks() > 0) {
            entityData.set(ATTACK_TICKS, getAttackTicks() - 1);
        }
        if (!level().isClientSide && scratchCooldown > 0) {
            scratchCooldown--;
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || !isAlive() || scratchCooldown > 0) {
            return;
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || distanceToSqr(target) > 18.0D * 18.0D) {
            return;
        }
        lookAt(target, 35.0F, 35.0F);
        entityData.set(ATTACK_TICKS, 18);
        GudiaoScratchEntity scratch = new GudiaoScratchEntity(level(), this, target);
        level().addFreshEntity(scratch);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 1.1D, getZ(), 4, 0.45D, 0.35D, 0.45D, 0.0D);
        }
        scratchCooldown = 55;
    }

    public int getAttackTicks() {
        return entityData.get(ATTACK_TICKS);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, state -> {
            if (isDeadOrDying()) {
                return state.setAndContinue(COLLAPSE_ANIM);
            }
            if (getAttackTicks() > 0) {
                return state.setAndContinue(ATTACK_ANIM);
            }
            if (state.isMoving()) {
                return state.setAndContinue(WALK_ANIM);
            }
            return state.setAndContinue(IDLE_ANIM);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
