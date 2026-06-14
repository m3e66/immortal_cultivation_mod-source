package com.example.immortal_cultivation_mod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class IgniteFlareProjectileEntity extends ThrowableItemProjectile {
    public IgniteFlareProjectileEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    public IgniteFlareProjectileEntity(Level level, LivingEntity shooter) {
        super(ModEntities.IGNITE_FLARE_PROJECTILE.get(), shooter, level);
        setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.BLAZE_POWDER;
    }

    @Override
    public void tick() {
        setNoGravity(true);
        super.tick();
        if (level().isClientSide) {
            var movement = getDeltaMovement();
            for (int i = 0; i < 3; i++) {
                double back = i * 0.22D;
                level().addParticle(ParticleTypes.FLAME,
                        getX() - movement.x * back,
                        getY() - movement.y * back,
                        getZ() - movement.z * back,
                        movement.x * 0.02D,
                        movement.y * 0.02D,
                        movement.z * 0.02D);
                if (i == 0) {
                    level().addParticle(ParticleTypes.SMALL_FLAME,
                            getX() - movement.x * back,
                            getY() - movement.y * back,
                            getZ() - movement.z * back,
                            0.0D, 0.01D, 0.0D);
                }
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            if (level() instanceof ServerLevel serverLevel) {
                SpellImpactParticles.flare(serverLevel, result.getLocation());
            }
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        result.getEntity().igniteForSeconds(5);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (level().isClientSide) {
            return;
        }

        Direction direction = result.getDirection();
        BlockPos firePos = result.getBlockPos().relative(direction);
        if (level().getBlockState(firePos).isAir()) {
            level().setBlockAndUpdate(firePos, Blocks.FIRE.defaultBlockState());
        }
    }
}
