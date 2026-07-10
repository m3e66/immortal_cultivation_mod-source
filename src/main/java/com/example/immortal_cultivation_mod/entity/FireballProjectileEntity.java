package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.effect.PhotonEffects;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class FireballProjectileEntity extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Float> DATA_CHARGE_SCALE =
            SynchedEntityData.defineId(FireballProjectileEntity.class, EntityDataSerializers.FLOAT);

    private int photonEffectAttempts;

    public FireballProjectileEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public FireballProjectileEntity(Level level, LivingEntity shooter) {
        super(ModEntities.FIREBALL_PROJECTILE.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.FIRE_CHARGE;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CHARGE_SCALE, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            float scale = chargeScale();
            int flameCount = Math.max(2, Math.round(3.0F * scale));
            int smokeCount = Math.max(1, Math.round(2.0F * scale));
            double flameSpread = 0.12D * scale;
            double smokeSpread = 0.18D * scale;
            for (int i = 0; i < smokeCount; i++) {
                level().addParticle(scale > 1.35F ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE,
                        getX() + (random.nextDouble() - 0.5D) * smokeSpread,
                        getY() + (random.nextDouble() - 0.5D) * smokeSpread,
                        getZ() + (random.nextDouble() - 0.5D) * smokeSpread,
                        -getDeltaMovement().x * 0.08D,
                        -getDeltaMovement().y * 0.08D,
                        -getDeltaMovement().z * 0.08D);
            }
            for (int i = 0; i < flameCount; i++) {
                level().addParticle(ParticleTypes.FLAME,
                        getX() + (random.nextDouble() - 0.5D) * flameSpread,
                        getY() + (random.nextDouble() - 0.5D) * flameSpread,
                        getZ() + (random.nextDouble() - 0.5D) * flameSpread,
                        -getDeltaMovement().x * 0.03D,
                        -getDeltaMovement().y * 0.03D,
                        -getDeltaMovement().z * 0.03D);
            }
            if (scale > 1.65F && random.nextInt(3) == 0) {
                level().addParticle(ParticleTypes.LAVA,
                        getX() + (random.nextDouble() - 0.5D) * flameSpread,
                        getY() + (random.nextDouble() - 0.5D) * flameSpread,
                        getZ() + (random.nextDouble() - 0.5D) * flameSpread,
                        0.0D,
                        0.0D,
                        0.0D);
            }
        }
        if (!level().isClientSide && photonEffectAttempts < 5) {
            photonEffectAttempts++;
            PhotonEffects.fireballProjectile(this);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide) return;
        if (level() instanceof ServerLevel serverLevel) {
            SpellImpactParticles.fire(serverLevel, result.getLocation());
        }
        level().explode(this, getX(), getY(), getZ(), 2.0f * chargeScale(), false, Level.ExplosionInteraction.NONE);
        discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        float damage = com.example.immortal_cultivation_mod.spell.SpellDamageHelper.damage(
                getOwner(),
                10.0F,
                ModSpells.get(ModSpells.FIREBALL)
        );
        if (getOwner() instanceof net.minecraft.world.entity.player.Player player) {
            var data = ModAttachments.getData(player);
            damage += data.magicAttack();
            damage *= (float) SpiritRoots.damageMultiplier(data, ModSpells.get(ModSpells.FIREBALL));
        }
        result.getEntity().hurt(damageSources().thrown(this, getOwner()), damage);
        result.getEntity().igniteForSeconds(3);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("ChargeScale", chargeScale());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setChargeScale(tag.getFloat("ChargeScale"));
    }

    public void setChargeScale(float chargeScale) {
        entityData.set(DATA_CHARGE_SCALE, Mth.clamp(chargeScale, 1.0F, 2.0F));
        getPersistentData().putFloat("ChargeScale", chargeScale());
    }

    public float chargeScale() {
        return Mth.clamp(entityData.get(DATA_CHARGE_SCALE), 1.0F, 2.0F);
    }
}
