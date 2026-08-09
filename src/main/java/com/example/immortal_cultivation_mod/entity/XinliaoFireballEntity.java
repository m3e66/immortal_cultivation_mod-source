package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.effect.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class XinliaoFireballEntity extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Float> VISUAL_SCALE =
            SynchedEntityData.defineId(XinliaoFireballEntity.class, EntityDataSerializers.FLOAT);
    private static final int EFFECT_TICKS = 20 * 30;
    private static final int MAX_LIFETIME_TICKS = 20 * 20;
    private static final double BASE_SPEED = 1.35D;
    private UUID targetUuid;
    private int observedHurtTimestamp;
    private int activeFlightTicks;

    public XinliaoFireballEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public XinliaoFireballEntity(Level level, LivingEntity owner, LivingEntity target) {
        super(ModEntities.XINLIAO_FIREBALL.get(), owner, level);
        targetUuid = target == null ? null : target.getUUID();
        observedHurtTimestamp = owner.getLastHurtByMobTimestamp();
        setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.AIR;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VISUAL_SCALE, 1.0F);
    }

    @Override
    public void tick() {
        setNoGravity(true);
        if (!level().isClientSide) {
            if (!(getOwner() instanceof LivingEntity owner) || !owner.isAlive() || owner.level() != level()) {
                discard();
                return;
            }
            LivingEntity target = resolveTarget();
            if (target == null || !target.isAlive() || target.level() != level()) {
                targetUuid = null;
                target = acquireNewAttacker(owner);
            }
            if (target == null) {
                setDeltaMovement(Vec3.ZERO);
            } else {
                steerToward(target);
                activeFlightTicks++;
                if (activeFlightTicks > MAX_LIFETIME_TICKS) {
                    discard();
                    return;
                }
            }
        }
        super.tick();
        if (level().isClientSide && !isRemoved()) {
            spawnClientFireball();
        }
    }

    private LivingEntity acquireNewAttacker(LivingEntity owner) {
        int hurtTimestamp = owner.getLastHurtByMobTimestamp();
        if (hurtTimestamp <= observedHurtTimestamp) {
            return null;
        }
        observedHurtTimestamp = hurtTimestamp;
        LivingEntity attacker = owner.getLastHurtByMob();
        if (attacker == null || attacker == owner || !attacker.isAlive() || attacker.level() != level()) {
            return null;
        }
        targetUuid = attacker.getUUID();
        activeFlightTicks = 0;
        return attacker;
    }

    private LivingEntity resolveTarget() {
        if (targetUuid == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(targetUuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    private void steerToward(LivingEntity target) {
        Vec3 targetPoint = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
        Vec3 toTarget = targetPoint.subtract(position());
        if (toTarget.lengthSqr() < 0.001D) {
            return;
        }
        double speed = BASE_SPEED + Math.min(0.65D, Math.sqrt(toTarget.lengthSqr()) * 0.025D);
        Vec3 desired = toTarget.normalize().scale(speed);
        Vec3 current = getDeltaMovement();
        Vec3 steered = current.lengthSqr() < 0.001D
                ? desired
                : current.scale(0.68D).add(desired.scale(0.32D));
        if (steered.lengthSqr() > 0.001D) {
            steered = steered.normalize().scale(speed);
        }
        setDeltaMovement(steered);
        hasImpulse = true;
    }

    private void spawnClientFireball() {
        Vec3 motion = getDeltaMovement();
        double phase = tickCount * 0.55D;
        float scale = visualScale();
        int particleCount = Math.max(10, Math.round(10.0F * scale));
        for (int i = 0; i < particleCount; i++) {
            double angle = phase + Math.PI * 2.0D * i / particleCount;
            double radius = 0.28D * scale;
            double x = getX() + Math.cos(angle) * radius;
            double y = getY() + Math.sin(angle * 1.7D) * 0.22D * scale;
            double z = getZ() + Math.sin(angle) * radius;
            level().addParticle(ParticleTypes.FLAME, x, y, z,
                    -motion.x * 0.025D, -motion.y * 0.025D, -motion.z * 0.025D);
        }
        level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        level().addParticle(ParticleTypes.SMOKE, getX() - motion.x * 0.2D,
                getY() - motion.y * 0.2D, getZ() - motion.z * 0.2D, 0.0D, 0.0D, 0.0D);
        if (tickCount % 3 == 0) {
            level().addParticle(ParticleTypes.LAVA, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return targetUuid != null
                && targetUuid.equals(ModelBonePartEntity.resolveParent(entity).getUUID())
                && super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        LivingEntity target = ModelBonePartEntity.resolveLiving(result.getEntity());
        if (!level().isClientSide && target != null) {
            target.addEffect(new MobEffectInstance(ModEffects.XINLIAO, EFFECT_TICKS, 0, false, true, true), getOwner());
            target.igniteForSeconds(30.0F);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        result.getLocation().x, result.getLocation().y, result.getLocation().z,
                        90, 0.65D, 0.65D, 0.65D, 0.12D);
                serverLevel.sendParticles(ParticleTypes.LAVA,
                        result.getLocation().x, result.getLocation().y, result.getLocation().z,
                        20, 0.45D, 0.45D, 0.45D, 0.08D);
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                        result.getLocation().x, result.getLocation().y, result.getLocation().z,
                        32, 0.55D, 0.55D, 0.55D, 0.06D);
            }
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (targetUuid != null) {
            tag.putUUID("Target", targetUuid);
        }
        tag.putFloat("VisualScale", visualScale());
        tag.putInt("ObservedHurtTimestamp", observedHurtTimestamp);
        tag.putInt("ActiveFlightTicks", activeFlightTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        targetUuid = tag.hasUUID("Target") ? tag.getUUID("Target") : null;
        setVisualScale(tag.getFloat("VisualScale"));
        observedHurtTimestamp = tag.getInt("ObservedHurtTimestamp");
        activeFlightTicks = tag.getInt("ActiveFlightTicks");
    }

    public void setVisualScale(float scale) {
        entityData.set(VISUAL_SCALE, Mth.clamp(scale, 1.0F, 2.0F));
    }

    public float visualScale() {
        return Mth.clamp(entityData.get(VISUAL_SCALE), 1.0F, 2.0F);
    }
}
