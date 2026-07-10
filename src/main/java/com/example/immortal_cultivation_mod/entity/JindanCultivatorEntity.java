package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.effect.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class JindanCultivatorEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> CAST_TICKS =
            SynchedEntityData.defineId(JindanCultivatorEntity.class, EntityDataSerializers.INT);
    private static final int MAX_QI = 3000;
    private static final int YINLEI_JUE_COST = 40;
    private static final int REGENERATION_COST = 24;
    private static final int WIND_RIDING_COST = 30;
    private static final int ZHENSHAN_PALM_COST = 390;
    private static final float LOW_HP_RATIO = 0.45F;
    private static final float REGENERATION_HP_RATIO = 0.25F;
    private static final float LOW_QI_RATIO = 0.35F;
    private static final float HIGH_RATIO = 0.75F;

    private int spellCooldown;
    private int qi = MAX_QI;

    public JindanCultivatorEntity(EntityType<? extends JindanCultivatorEntity> entityType, Level level) {
        super(entityType, level);
        xpReward = 80;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1500.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.ARMOR, 14.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.85D));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CAST_TICKS, 0);
    }

    @Override
    public void tick() {
        super.tick();
        int castTicks = getCastingTicks();
        if (castTicks > 0) {
            entityData.set(CAST_TICKS, castTicks - 1);
        }
        if (!level().isClientSide && tickCount % 8 == 0 && hasEffect(ModEffects.QI_GATHERING)) {
            qi = Math.min(MAX_QI, qi + 80);
        }
        if (!level().isClientSide && hasEffect(ModEffects.YUFENG_JUE)) {
            setNoGravity(true);
            fallDistance = 0.0F;
            resetFallDistance();
            if (tickCount % 4 == 0 && level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                        getX(), getY() + 0.15D, getZ(),
                        8, 0.45D, 0.12D, 0.45D, 0.03D);
            }
        } else if (!level().isClientSide && isNoGravity()) {
            setNoGravity(false);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || !isAlive()) {
            return;
        }
        if (spellCooldown > 0) {
            spellCooldown--;
            return;
        }

        boolean hpVeryLow = healthRatio() <= REGENERATION_HP_RATIO;
        if (hpVeryLow && !hasEffect(MobEffects.REGENERATION) && castRegeneration()) {
            LivingEntity currentTarget = getTarget();
            if (currentTarget != null && currentTarget.isAlive()) {
                fleeFrom(currentTarget);
            }
            spellCooldown = 30;
            return;
        }

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || distanceToSqr(target) > 40.0D * 40.0D) {
            return;
        }

        double distance = Math.sqrt(distanceToSqr(target));
        boolean hpLow = healthRatio() < LOW_HP_RATIO;
        boolean hpHigh = healthRatio() >= HIGH_RATIO;
        boolean qiLow = qiRatio() < LOW_QI_RATIO;

        if (hasEffect(ModEffects.QI_GATHERING) && (!qiLow || distance < 15.0D)) {
            removeEffect(ModEffects.QI_GATHERING);
        }

        if (hpLow) {
            fleeFrom(target);
            if (!hasEffect(ModEffects.YUFENG_JUE) && castWindRiding()) {
                spellCooldown = 25;
                return;
            }
            spellCooldown = 15;
            return;
        }

        if (qiLow && distance >= 15.0D && !hasEffect(ModEffects.QI_GATHERING) && castQiGathering()) {
            spellCooldown = 40;
            return;
        }

        if (distance <= 5.0D && castYinleiJue(target)) {
            spellCooldown = 32;
            return;
        }

        if (distance <= 10.0D && castZhenshanPalm(target)) {
            spellCooldown = 70;
            return;
        }

        if (distance >= 11.0D) {
            if (hpHigh && !hasEffect(ModEffects.YUFENG_JUE) && castWindRiding()) {
                spellCooldown = 25;
                return;
            }
            getNavigation().moveTo(target, hasEffect(ModEffects.YUFENG_JUE) ? 1.45D : 1.0D);
            flyToward(target);
            spellCooldown = 12;
            return;
        }

        if (castYinleiJue(target)) {
            spellCooldown = 32;
        } else {
            getNavigation().moveTo(target, hasEffect(ModEffects.YUFENG_JUE) ? 1.35D : 1.0D);
            spellCooldown = 20;
        }
    }

    public int getCastingTicks() {
        return entityData.get(CAST_TICKS);
    }

    private float healthRatio() {
        return getMaxHealth() <= 0.0F ? 0.0F : getHealth() / getMaxHealth();
    }

    private float qiRatio() {
        return (float) qi / MAX_QI;
    }

    private boolean spendQi(int cost) {
        if (qi < cost) {
            return false;
        }
        qi -= cost;
        return true;
    }

    private void fleeFrom(LivingEntity target) {
        Vec3 away = position().subtract(target.position());
        if (away.lengthSqr() < 0.01D) {
            away = new Vec3(random.nextDouble() - 0.5D, 0.0D, random.nextDouble() - 0.5D);
        }
        Vec3 destination = position().add(away.normalize().scale(9.0D));
        getNavigation().moveTo(destination.x, destination.y, destination.z, hasEffect(ModEffects.YUFENG_JUE) ? 1.6D : 1.1D);
        if (hasEffect(ModEffects.YUFENG_JUE)) {
            Vec3 flight = away.normalize().scale(0.5D).add(0.0D, 0.18D, 0.0D);
            setDeltaMovement(getDeltaMovement().scale(0.35D).add(flight));
            hurtMarked = true;
        }
    }

    private void flyToward(LivingEntity target) {
        if (!hasEffect(ModEffects.YUFENG_JUE)) {
            return;
        }
        Vec3 toTarget = target.getEyePosition().subtract(position().add(0.0D, 1.0D, 0.0D));
        if (toTarget.lengthSqr() < 0.01D) {
            return;
        }
        Vec3 flight = toTarget.normalize().scale(0.42D);
        setDeltaMovement(getDeltaMovement().scale(0.35D).add(flight.x, Math.max(-0.06D, flight.y + 0.05D), flight.z));
        hurtMarked = true;
    }

    private void beginCast() {
        entityData.set(CAST_TICKS, 18);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    getX(), getY() + 1.2D, getZ(),
                    22, 0.4D, 0.5D, 0.4D, 0.04D);
        }
    }

    private boolean castQiGathering() {
        beginCast();
        addEffect(new MobEffectInstance(ModEffects.QI_GATHERING, 20 * 10, 0, false, false, true));
        return true;
    }

    private boolean castRegeneration() {
        if (!spendQi(REGENERATION_COST)) {
            return false;
        }
        beginCast();
        addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 8, 2, false, false, true));
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HEART,
                    getX(), getY() + 1.4D, getZ(),
                    10, 0.35D, 0.45D, 0.35D, 0.02D);
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    getX(), getY() + 1.0D, getZ(),
                    22, 0.4D, 0.55D, 0.4D, 0.04D);
        }
        return true;
    }

    private boolean castWindRiding() {
        if (!spendQi(WIND_RIDING_COST)) {
            return false;
        }
        beginCast();
        addEffect(new MobEffectInstance(ModEffects.YUFENG_JUE, 20 * 10, 0, false, false, true));
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 10, 1, false, false, true));
        addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 10, 0, false, false, true));
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    getX(), getY() + 0.15D, getZ(),
                    22, 0.5D, 0.15D, 0.5D, 0.04D);
        }
        return true;
    }

    private boolean castZhenshanPalm(LivingEntity target) {
        if (!spendQi(ZHENSHAN_PALM_COST)) {
            return false;
        }
        beginCast();
        lookAt(target, 30.0F, 30.0F);
        ZhenshanPalmEntity palm = new ZhenshanPalmEntity(level(), this);
        level().addFreshEntity(palm);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    getX(), getY() + 0.2D, getZ(),
                    25, 0.6D, 0.2D, 0.6D, 0.03D);
        }
        return true;
    }

    private boolean castYinleiJue(LivingEntity target) {
        if (!spendQi(YINLEI_JUE_COST)) {
            return false;
        }
        beginCast();
        lookAt(target, 30.0F, 30.0F);
        LightningProjectileEntity projectile = new LightningProjectileEntity(level(), this, false);
        double dx = target.getX() - getX();
        double dy = target.getEyeY() - projectile.getY();
        double dz = target.getZ() - getZ();
        projectile.shoot(dx, dy, dz, 2.4F, 0.4F);
        level().addFreshEntity(projectile);
        return true;
    }
}
