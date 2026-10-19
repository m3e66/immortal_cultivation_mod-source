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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class FireballProjectileEntity extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Float> DATA_CHARGE_SCALE =
            SynchedEntityData.defineId(FireballProjectileEntity.class, EntityDataSerializers.FLOAT);


    public FireballProjectileEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public FireballProjectileEntity(Level level, LivingEntity shooter) {
        super(ModEntities.FIREBALL_PROJECTILE.get(), shooter, level);
        NpcProfile.markNpcOwnedEffect(this, shooter);
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
        if (!level().isClientSide) {
            Entity owner = getOwner();
            NpcProfile.markNpcOwnedEffect(this, owner);
            if (NpcProfile.isOrphanedNpcEffect(this, owner)) {
                discard();
                return;
            }
        }
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
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide) return;
        if (level() instanceof ServerLevel serverLevel) {
            SpellImpactParticles.fire(serverLevel, result.getLocation());
        }
        detonate();
    }

    private void detonate() {
        if (level().isClientSide || !isAlive()) {
            return;
        }
        Entity owner = getOwner();
        NpcProfile.markNpcOwnedEffect(this, owner);
        if (NpcProfile.isOrphanedNpcEffect(this, owner)) {
            discard();
            return;
        }
        float power = 2.0F * chargeScale();
        if (owner instanceof CultivationNpc && owner instanceof LivingEntity npcOwner
                && level() instanceof ServerLevel serverLevel) {
            serverLevel.explode(this, null, npcEnemyOnlyExplosion(npcOwner), position(),
                    power, false, Level.ExplosionInteraction.NONE);
        } else {
            level().explode(this, getX(), getY(), getZ(), power, false, Level.ExplosionInteraction.NONE);
        }
        if (level() instanceof ServerLevel serverLevel) {
            ElementalTechniqueAreaEntity.damageFireSensitiveAreas(
                    serverLevel, position(), 3.0D * chargeScale(), 28);
        }
        discard();
    }

    private static ExplosionDamageCalculator npcEnemyOnlyExplosion(LivingEntity owner) {
        return new ExplosionDamageCalculator() {
            private boolean canAffect(Entity entity) {
                return entity instanceof LivingEntity target && NpcProfile.isCombatEnemy(owner, target);
            }

            @Override
            public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
                return canAffect(entity);
            }

            @Override
            public float getKnockbackMultiplier(Entity entity) {
                return canAffect(entity) ? super.getKnockbackMultiplier(entity) : 0.0F;
            }
        };
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && NpcProfile.canNpcEffectHit(this, getOwner(), entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = ModelBonePartEntity.resolveParent(result.getEntity());
        float damage = com.example.immortal_cultivation_mod.spell.SpellDamageHelper.damage(
                getOwner(),
                10.0F,
                ModSpells.get(ModSpells.FIREBALL)
        );
        target.hurt(damageSources().thrown(this, getOwner()), damage);
        target.igniteForSeconds(3);
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
