package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.effect.PhotonEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ZhujiCultivatorEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> CAST_TICKS =
            SynchedEntityData.defineId(ZhujiCultivatorEntity.class, EntityDataSerializers.INT);
    private static final int MAX_QI = 900;
    private static final int FIREBALL_COST = 10;
    private static final int LINGBENG_COST = 85;
    private static final int REGENERATION_COST = 24;
    private static final int WIND_STEP_COST = 5;
    private static final int DIELANG_SHIELD_COST = 100;
    private static final float DIELANG_SHIELD_AMOUNT = 108.0F;
    private static final float SHIELD_HP_RATIO = 0.5F;
    private static final float LOW_HP_RATIO = 0.45F;
    private static final float REGENERATION_HP_RATIO = 0.25F;
    private static final float LOW_QI_RATIO = 0.35F;
    private static final float HIGH_RATIO = 0.75F;

    private int spellCooldown;
    private int qi = MAX_QI;
    private float dielangShield;

    public ZhujiCultivatorEntity(EntityType<? extends ZhujiCultivatorEntity> entityType, Level level) {
        super(entityType, level);
        xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 400.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 8.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
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
            qi = Math.min(MAX_QI, qi + 35);
        }
        if (!level().isClientSide && dielangShield > 0.0F && tickCount % 2 == 0 && level() instanceof ServerLevel serverLevel) {
            PhotonEffects.waterShield(serverLevel, this, 3);
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
        if (target == null || !target.isAlive() || distanceToSqr(target) > 34.0D * 34.0D) {
            return;
        }

        double distance = Math.sqrt(distanceToSqr(target));
        boolean hpLow = healthRatio() < LOW_HP_RATIO;
        boolean shouldShield = healthRatio() <= SHIELD_HP_RATIO && dielangShield <= 0.0F;
        boolean hpHigh = healthRatio() >= HIGH_RATIO;
        boolean qiLow = qiRatio() < LOW_QI_RATIO;
        boolean qiHigh = qiRatio() >= HIGH_RATIO;

        if (hasEffect(ModEffects.QI_GATHERING) && (!qiLow || distance < 15.0D)) {
            removeEffect(ModEffects.QI_GATHERING);
        }

        if (shouldShield && castDielangShield()) {
            spellCooldown = 25;
            return;
        }

        if (hpLow) {
            if (hpVeryLow && !hasEffect(MobEffects.REGENERATION) && castRegeneration()) {
                fleeFrom(target);
                spellCooldown = 30;
                return;
            }
            fleeFrom(target);
            if (!hasEffect(ModEffects.WIND_STEP) && castWindStep()) {
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

        if (distance <= 5.0D) {
            getNavigation().moveTo(target, hasEffect(ModEffects.WIND_STEP) ? 1.2D : 0.9D);
            if (shouldCastLingbengBeforePunch(qiHigh, hpHigh) && castLingbeng()) {
                spellCooldown = 18;
                return;
            }
            punchWithLingbeng(target);
            spellCooldown = 24;
            return;
        }

        if (distance <= 10.0D && castFireball(target)) {
            spellCooldown = 35;
            return;
        }

        if (distance >= 11.0D) {
            if (hpHigh && !hasEffect(ModEffects.WIND_STEP) && castWindStep()) {
                spellCooldown = 25;
                return;
            }
            getNavigation().moveTo(target, hasEffect(ModEffects.WIND_STEP) ? 1.35D : 0.95D);
            spellCooldown = 12;
            return;
        }

        if (castFireball(target)) {
            spellCooldown = 35;
        } else {
            getNavigation().moveTo(target, hasEffect(ModEffects.WIND_STEP) ? 1.25D : 0.95D);
            spellCooldown = 20;
        }
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        boolean hurt = super.doHurtTarget(entity);
        if (hurt && entity instanceof LivingEntity target && hasEffect(ModEffects.LINGBENG)) {
            releaseLingbeng(target);
        }
        return hurt;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && amount > 0.0F && dielangShield > 0.0F) {
            float absorbed = Math.min(amount, dielangShield);
            dielangShield -= absorbed;
            amount -= absorbed;
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SPLASH,
                        getX(), getY() + 1.0D, getZ(),
                        18, 0.45D, 0.55D, 0.45D, 0.04D);
                if (dielangShield <= 0.0F) {
                    PhotonEffects.removeWaterShield(serverLevel, this);
                    serverLevel.sendParticles(ParticleTypes.BUBBLE_POP,
                            getX(), getY() + 1.0D, getZ(),
                            28, 0.55D, 0.65D, 0.55D, 0.05D);
                }
            }
            if (amount <= 0.0F) {
                hurtTime = 10;
                invulnerableTime = 5;
                return true;
            }
        }
        return super.hurt(source, amount);
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

    private boolean shouldCastLingbengBeforePunch(boolean qiHigh, boolean hpHigh) {
        MobEffectInstance current = getEffect(ModEffects.LINGBENG);
        if (current == null) {
            return qi >= LINGBENG_COST;
        }
        return qiHigh && hpHigh && current.getAmplifier() < 3 && random.nextFloat() < 0.45F;
    }

    private void fleeFrom(LivingEntity target) {
        Vec3 away = position().subtract(target.position());
        if (away.lengthSqr() < 0.01D) {
            away = new Vec3(random.nextDouble() - 0.5D, 0.0D, random.nextDouble() - 0.5D);
        }
        Vec3 destination = position().add(away.normalize().scale(8.0D));
        getNavigation().moveTo(destination.x, destination.y, destination.z, hasEffect(ModEffects.WIND_STEP) ? 1.45D : 1.05D);
    }

    private void punchWithLingbeng(LivingEntity target) {
        lookAt(target, 30.0F, 30.0F);
        doHurtTarget(target);
    }

    private void releaseLingbeng(LivingEntity target) {
        MobEffectInstance lingbeng = getEffect(ModEffects.LINGBENG);
        int amplifier = lingbeng == null ? 0 : lingbeng.getAmplifier();
        removeEffect(ModEffects.LINGBENG);
        float radius = 2.0F + Math.min(6, amplifier);
        float damage = 10.0F + amplifier * 4.0F;
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    target.getX(), target.getY() + 0.8D, target.getZ(),
                    45, radius * 0.25D, 0.45D, radius * 0.25D, 0.08D);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    target.getX(), target.getY() + 0.4D, target.getZ(),
                    2, 0.1D, 0.1D, 0.1D, 0.0D);
        }
        for (LivingEntity nearby : level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(radius))) {
            if (nearby == this || !nearby.isAlive()) {
                continue;
            }
            nearby.hurt(damageSources().mobAttack(this), damage);
        }
    }

    private void beginCast() {
        entityData.set(CAST_TICKS, 18);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    getX(), getY() + 1.2D, getZ(),
                    18, 0.35D, 0.45D, 0.35D, 0.04D);
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
        addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 6, 1, false, false, true));
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HEART,
                    getX(), getY() + 1.4D, getZ(),
                    8, 0.35D, 0.45D, 0.35D, 0.02D);
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    getX(), getY() + 1.0D, getZ(),
                    18, 0.4D, 0.55D, 0.4D, 0.04D);
        }
        return true;
    }

    private boolean castWindStep() {
        if (!spendQi(WIND_STEP_COST)) {
            return false;
        }
        beginCast();
        addEffect(new MobEffectInstance(ModEffects.WIND_STEP, 20 * 10, 0, false, false, true));
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 10, 1, false, false, true));
        return true;
    }

    private boolean castDielangShield() {
        if (!spendQi(DIELANG_SHIELD_COST)) {
            return false;
        }
        beginCast();
        dielangShield = DIELANG_SHIELD_AMOUNT;
        if (level() instanceof ServerLevel serverLevel) {
            PhotonEffects.waterShield(serverLevel, this, 3);
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    getX(), getY() + 1.1D, getZ(),
                    36, 0.55D, 0.7D, 0.55D, 0.05D);
            serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                    getX(), getY() + 1.3D, getZ(),
                    24, 0.45D, 0.65D, 0.45D, 0.02D);
        }
        return true;
    }

    private boolean castLingbeng() {
        if (!spendQi(LINGBENG_COST)) {
            return false;
        }
        beginCast();
        MobEffectInstance current = getEffect(ModEffects.LINGBENG);
        int amplifier = current == null ? 0 : Math.min(9, current.getAmplifier() + 1);
        int duration = Math.max(20 * 8, current == null ? 0 : current.getDuration() + 20 * 6);
        addEffect(new MobEffectInstance(ModEffects.LINGBENG, duration, amplifier, false, false, true));
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    getX(), getY() + 1.0D, getZ(),
                    28, 0.35D, 0.55D, 0.35D, 0.035D);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    getX(), getY() + 1.0D, getZ(),
                    18, 0.3D, 0.45D, 0.3D, 0.08D);
        }
        return true;
    }

    private boolean castFireball(LivingEntity target) {
        if (!spendQi(FIREBALL_COST)) {
            return false;
        }
        beginCast();
        lookAt(target, 30.0F, 30.0F);
        FireballProjectileEntity projectile = new FireballProjectileEntity(level(), this);
        double dx = target.getX() - getX();
        double dy = target.getEyeY() - projectile.getY();
        double dz = target.getZ() - getZ();
        projectile.shoot(dx, dy, dz, 1.15F, 0.4F);
        level().addFreshEntity(projectile);
        return true;
    }
}
