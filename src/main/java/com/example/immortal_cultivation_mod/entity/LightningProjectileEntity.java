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
            int sparks = isGreater() ? 7 : 4;

            for (int i = 0; i < sparks; i++) {
                double spread = isGreater() ? 0.34D : 0.22D;

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
                    isGreater() ? 10 : 6,
                    isGreater() ? 0.24D : 0.14D,
                    isGreater() ? 0.24D : 0.14D,
                    isGreater() ? 0.24D : 0.14D,
                    isGreater() ? 0.13D : 0.08D
            );
            serverLevel.sendParticles(
                    isGreater() ? ParticleTypes.END_ROD : ParticleTypes.WAX_OFF,
                    getX() - movement.x * 0.18D,
                    getY() - movement.y * 0.18D,
                    getZ() - movement.z * 0.18D,
                    isGreater() ? 3 : 1,
                    0.06D,
                    0.06D,
                    0.06D,
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
            lightningStrikeParticles(serverLevel, hitPos, isGreater());
            summonLightning(serverLevel, hitPos, isGreater());
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
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("greater", isGreater());
        tag.putInt("lifeTicks", lifeTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setGreater(tag.getBoolean("greater"));
        lifeTicks = tag.getInt("lifeTicks");
    }

    private boolean isGreater() {
        return entityData.get(DATA_GREATER);
    }

    private void setGreater(boolean greater) {
        entityData.set(DATA_GREATER, greater);
    }

    private static void summonLightning(ServerLevel level, Vec3 pos, boolean greater) {
        int strikes = greater ? 7 : 1;
        double radius = greater ? 3.5D : 0.0D;

        for (int i = 0; i < strikes; i++) {
            double ox = greater ? (level.random.nextDouble() - 0.5D) * radius * 2.0D : 0.0D;
            double oz = greater ? (level.random.nextDouble() - 0.5D) * radius * 2.0D : 0.0D;

            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(pos.x + ox, pos.y, pos.z + oz);
                bolt.setVisualOnly(false);
                level.addFreshEntity(bolt);
            }
        }
    }

    private static void lightningStrikeParticles(ServerLevel level, Vec3 pos, boolean greater) {
        int columns = greater ? 5 : 2;
        int steps = greater ? 18 : 11;

        for (int column = 0; column < columns; column++) {
            double angle = column * Math.PI * 2.0D / columns;
            double radius = column == 0 ? 0.0D : (greater ? 0.65D : 0.35D);
            double ox = Math.cos(angle) * radius;
            double oz = Math.sin(angle) * radius;

            for (int i = 0; i < steps; i++) {
                double y = pos.y + 0.15D + i * (greater ? 0.42D : 0.32D);

                level.sendParticles(
                        ParticleTypes.ELECTRIC_SPARK,
                        pos.x + ox + (level.random.nextDouble() - 0.5D) * 0.18D,
                        y,
                        pos.z + oz + (level.random.nextDouble() - 0.5D) * 0.18D,
                        greater ? 4 : 2,
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
                greater ? 3 : 1,
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
                greater ? 70 : 30,
                greater ? 1.1D : 0.55D,
                0.45D,
                greater ? 1.1D : 0.55D,
                0.16D
        );

        level.sendParticles(
                ParticleTypes.WAX_OFF,
                pos.x,
                pos.y + 0.2D,
                pos.z,
                greater ? 55 : 25,
                greater ? 0.9D : 0.45D,
                0.35D,
                greater ? 0.9D : 0.45D,
                0.18D
        );

        SpellImpactParticles.lightning(level, pos);
    }
}
