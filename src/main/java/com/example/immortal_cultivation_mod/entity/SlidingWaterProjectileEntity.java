package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.spell.SlidingWater;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class SlidingWaterProjectileEntity extends ThrowableItemProjectile {
    public SlidingWaterProjectileEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    public SlidingWaterProjectileEntity(Level level, LivingEntity shooter) {
        super(ModEntities.SLIDING_WATER_PROJECTILE.get(), shooter, level);
        setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.WATER_BUCKET;
    }

    @Override
    public void tick() {
        setNoGravity(true);
        super.tick();
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.SPLASH,
                    getX(), getY(), getZ(),
                    -getDeltaMovement().x * 0.04D,
                    -getDeltaMovement().y * 0.04D,
                    -getDeltaMovement().z * 0.04D);
            level().addParticle(ParticleTypes.FALLING_WATER,
                    getX() + (random.nextDouble() - 0.5D) * 0.12D,
                    getY() + (random.nextDouble() - 0.5D) * 0.12D,
                    getZ() + (random.nextDouble() - 0.5D) * 0.12D,
                    0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            if (level() instanceof ServerLevel serverLevel) {
                SpellImpactParticles.water(serverLevel, result.getLocation());
            }
            discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (level() instanceof ServerLevel serverLevel) {
            SlidingWater.createPuddle(serverLevel, result.getBlockPos().above(), Math.max(1.0F, getPersistentData().getFloat("ChargeScale")));
        }
    }
}
