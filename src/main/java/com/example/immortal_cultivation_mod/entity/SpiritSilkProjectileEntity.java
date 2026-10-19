package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.magicritual.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class SpiritSilkProjectileEntity extends ThrowableItemProjectile {
    // Visuals use synchronized BeastAttackVfx; server combat and sounds remain unchanged.
    public SpiritSilkProjectileEntity(
            EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public SpiritSilkProjectileEntity(Level level, LivingEntity owner) {
        super(ModEntities.SPIRIT_SILK_PROJECTILE.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.SPIRIT_SILKWORM_SILK.get();
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide && tickCount > 80) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return ModelBonePartEntity.resolveParent(entity) != getOwner()
                && super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        LivingEntity target = ModelBonePartEntity.resolveLiving(result.getEntity());
        if (!level().isClientSide && target != null) {
            target.hurt(damageSources().thrown(this, getOwner()), 8.0F);
            target.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, 20 * 6, 2, false, true, true));
        }
        discardWithParticles(result);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level() instanceof ServerLevel serverLevel
                && getOwner() instanceof AzureSpiritSilkwormEntity silkworm) {
            BeastMoveBlockBreaker.breakSphere(serverLevel, silkworm,
                    result.getLocation(), 1.1D, 1.0D, 6, 2.0F);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (result.getType() != HitResult.Type.ENTITY) {
            discardWithParticles(result);
        }
    }

    private void discardWithParticles(HitResult result) {

        if (!level().isClientSide) {
            discard();
        }
    }
}
