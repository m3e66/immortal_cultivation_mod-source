package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class LightningProjectileEntity extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Boolean> DATA_GREATER =
            SynchedEntityData.defineId(LightningProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_CHARGE_SCALE =
            SynchedEntityData.defineId(LightningProjectileEntity.class, EntityDataSerializers.FLOAT);

    private int lifeTicks;

    public LightningProjectileEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    public LightningProjectileEntity(Level level, LivingEntity shooter, boolean greater) {
        super(ModEntities.LIGHTNING_PROJECTILE.get(), shooter, level);
        setGreater(greater);
        setNoGravity(true);

        Vec3 start = shooter.getEyePosition().add(shooter.getLookAngle().scale(1.15D));
        setPos(start.x, start.y - 0.1D, start.z);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.LIGHTNING_ROD;
    }

    @Override
    public void tick() {
        setNoGravity(true);
        super.tick();
        lifeTicks++;

        if (level().isClientSide) {
            Vec3 movement = getDeltaMovement();
            float scale = chargeScale();
            int sparks = Math.max(isGreater() ? 7 : 4, Math.round((isGreater() ? 7.0F : 4.0F) * scale * 1.5F));

            for (int i = 0; i < sparks; i++) {
                double spread = (isGreater() ? 0.34D : 0.22D) * scale;

                level().addParticle(
                        ParticleTypes.ELECTRIC_SPARK,
                        getX() + (random.nextDouble() - 0.5D) * spread,
                        getY() + (random.nextDouble() - 0.5D) * spread,
                        getZ() + (random.nextDouble() - 0.5D) * spread,
                        -movement.x * 0.08D + (random.nextDouble() - 0.5D) * 0.08D,
                        -movement.y * 0.08D + (random.nextDouble() - 0.5D) * 0.08D,
                        -movement.z * 0.08D + (random.nextDouble() - 0.5D) * 0.08D
                );
            }

            level().addParticle(
                    isGreater() ? ParticleTypes.END_ROD : ParticleTypes.WAX_OFF,
                    getX() - movement.x * 0.12D,
                    getY() - movement.y * 0.12D,
                    getZ() - movement.z * 0.12D,
                    movement.x * 0.01D,
                    movement.y * 0.01D,
                    movement.z * 0.01D
            );
        } else if (level() instanceof ServerLevel serverLevel) {
            Vec3 movement = getDeltaMovement();
            serverLevel.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    getX(),
                    getY(),
                    getZ(),
                    Math.max(isGreater() ? 10 : 6, Math.round((isGreater() ? 10.0F : 6.0F) * chargeScale() * 1.6F)),
                    (isGreater() ? 0.24D : 0.14D) * chargeScale(),
                    (isGreater() ? 0.24D : 0.14D) * chargeScale(),
                    (isGreater() ? 0.24D : 0.14D) * chargeScale(),
                    (isGreater() ? 0.13D : 0.08D) * chargeScale()
            );
            serverLevel.sendParticles(
                    isGreater() ? ParticleTypes.END_ROD : ParticleTypes.WAX_OFF,
                    getX() - movement.x * 0.18D,
                    getY() - movement.y * 0.18D,
                    getZ() - movement.z * 0.18D,
                    Math.max(isGreater() ? 3 : 1, Math.round((isGreater() ? 3.0F : 1.0F) * chargeScale() * 1.5F)),
                    0.06D * chargeScale(),
                    0.06D * chargeScale(),
                    0.06D * chargeScale(),
                    0.03D
            );
        }

        if (lifeTicks > 80) {
            discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);

        if (level().isClientSide) {
            return;
        }

        Vec3 hitPos = result.getLocation();

        if (level() instanceof ServerLevel serverLevel) {
            lightningStrikeParticles(serverLevel, hitPos, isGreater(), chargeScale());
            summonLightning(serverLevel, hitPos, isGreater(), chargeScale());
        }

        level().playSound(
                null,
                hitPos.x,
                hitPos.y,
                hitPos.z,
                isGreater() ? SoundEvents.LIGHTNING_BOLT_THUNDER : SoundEvents.TRIDENT_THUNDER.value(),
                SoundSource.PLAYERS,
                isGreater() ? 1.4F : 0.9F,
                isGreater() ? 1.45F : 1.75F
        );

        discard();
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        Entity owner = getOwner();
        return (owner == null || !entity.getUUID().equals(owner.getUUID())) && super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        String spellId = isGreater() ? ModSpells.WULEI_ZHENGFA : ModSpells.YINLEI_JUE;

        float damage = com.example.immortal_cultivation_mod.spell.SpellDamageHelper.damage(
                getOwner(),
                isGreater() ? 100.0F : 20.0F,
                ModSpells.get(spellId)
        );

        if (getOwner() instanceof net.minecraft.world.entity.player.Player player) {
            var data = ModAttachments.getData(player);
            damage += data.magicAttack();
            damage *= (float) SpiritRoots.damageMultiplier(data, ModSpells.get(spellId));
        }

        result.getEntity().hurt(damageSources().thrown(this, getOwner()), damage);

        if (result.getEntity() instanceof LivingEntity living) {
            living.setRemainingFireTicks(Math.max(living.getRemainingFireTicks(), isGreater() ? 60 : 30));
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_GREATER, false);
        builder.define(DATA_CHARGE_SCALE, 1.0F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("greater", isGreater());
        tag.putInt("lifeTicks", lifeTicks);
        tag.putFloat("ChargeScale", chargeScale());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setGreater(tag.getBoolean("greater"));
        lifeTicks = tag.getInt("lifeTicks");
        setChargeScale(tag.getFloat("ChargeScale"));
    }

    private boolean isGreater() {
        return entityData.get(DATA_GREATER);
    }

    private void setGreater(boolean greater) {
        entityData.set(DATA_GREATER, greater);
    }

    public void setChargeScale(float chargeScale) {
        float scale = Math.max(1.0F, Math.min(2.0F, chargeScale));
        entityData.set(DATA_CHARGE_SCALE, scale);
        getPersistentData().putFloat("ChargeScale", scale);
    }

    private static void summonLightning(ServerLevel level, Vec3 pos, boolean greater, float chargeScale) {
        int strikes = greater
                ? Math.max(7, Math.round(9.0F * chargeScale))
                : Math.max(1, Math.round(1.0F + 6.0F * (chargeScale - 1.0F)));
        double radius = (greater ? 4.2D : 1.45D) * chargeScale;

        for (int i = 0; i < strikes; i++) {
            double ox = strikes > 1 ? (level.random.nextDouble() - 0.5D) * radius * 2.0D : 0.0D;
            double oz = strikes > 1 ? (level.random.nextDouble() - 0.5D) * radius * 2.0D : 0.0D;

            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(pos.x + ox, pos.y, pos.z + oz);
                bolt.setVisualOnly(false);
                level.addFreshEntity(bolt);
            }
        }
    }

    private static void lightningStrikeParticles(ServerLevel level, Vec3 pos, boolean greater, float chargeScale) {
        int columns = Math.max(greater ? 7 : 3, Math.round((greater ? 7.0F : 3.0F) * chargeScale));
        int steps = Math.max(greater ? 22 : 14, Math.round((greater ? 22.0F : 14.0F) * (0.7F + 0.35F * chargeScale)));

        for (int column = 0; column < columns; column++) {
            double angle = column * Math.PI * 2.0D / columns;
            double radius = column == 0 ? 0.0D : (greater ? 0.9D : 0.5D) * chargeScale;
            double ox = Math.cos(angle) * radius;
            double oz = Math.sin(angle) * radius;

            for (int i = 0; i < steps; i++) {
                double y = pos.y + 0.15D + i * (greater ? 0.42D : 0.32D);

                level.sendParticles(
                        ParticleTypes.ELECTRIC_SPARK,
                        pos.x + ox + (level.random.nextDouble() - 0.5D) * 0.18D,
                        y,
                        pos.z + oz + (level.random.nextDouble() - 0.5D) * 0.18D,
                        Math.max(greater ? 4 : 2, Math.round((greater ? 4.0F : 2.0F) * chargeScale)),
                        0.05D,
                        0.03D,
                        0.05D,
                        greater ? 0.18D : 0.12D
                );
            }
        }

        level.sendParticles(
                ParticleTypes.FLASH,
                pos.x,
                pos.y + 0.35D,
                pos.z,
                Math.max(greater ? 3 : 1, Math.round((greater ? 3.0F : 1.0F) * chargeScale)),
                0.0D,
                0.0D,
                0.0D,
                0.0D
        );

        level.sendParticles(
                ParticleTypes.END_ROD,
                pos.x,
                pos.y + 0.5D,
                pos.z,
                Math.max(greater ? 70 : 30, Math.round((greater ? 70.0F : 30.0F) * chargeScale)),
                (greater ? 1.1D : 0.55D) * chargeScale,
                0.45D,
                (greater ? 1.1D : 0.55D) * chargeScale,
                0.16D
        );

        level.sendParticles(
                ParticleTypes.WAX_OFF,
                pos.x,
                pos.y + 0.2D,
                pos.z,
                Math.max(greater ? 55 : 25, Math.round((greater ? 55.0F : 25.0F) * chargeScale)),
                (greater ? 0.9D : 0.45D) * chargeScale,
                0.35D,
                (greater ? 0.9D : 0.45D) * chargeScale,
                0.18D
        );

        SpellImpactParticles.lightning(level, pos);
    }

    private float chargeScale() {
        return Math.max(1.0F, Math.min(2.0F, entityData.get(DATA_CHARGE_SCALE)));
    }
}
