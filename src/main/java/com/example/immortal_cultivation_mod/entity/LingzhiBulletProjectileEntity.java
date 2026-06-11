package com.example.immortal_cultivation_mod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class LingzhiBulletProjectileEntity extends ThrowableItemProjectile {
    public LingzhiBulletProjectileEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    public LingzhiBulletProjectileEntity(Level level, LivingEntity shooter) {
        super(ModEntities.LINGZHI_BULLET_PROJECTILE.get(), shooter, level);
        setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.FEATHER;
    }

    @Override
    public void tick() {
        setNoGravity(true);
        super.tick();
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.CRIT,
                    getX(), getY(), getZ(),
                    -getDeltaMovement().x * 0.05D,
                    -getDeltaMovement().y * 0.05D,
                    -getDeltaMovement().z * 0.05D);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        result.getEntity().hurt(damageSources().thrown(this, getOwner()), 5.0F);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (level().isClientSide) {
            return;
        }

        BlockPos pos = result.getBlockPos();
        BlockPos step = pos.relative(result.getDirection().getOpposite());
        breakBlock(pos);
        breakBlock(step);
        breakBlock(step.relative(result.getDirection().getOpposite()));
    }

    private void breakBlock(BlockPos pos) {
        var state = level().getBlockState(pos);
        if (!state.isAir() && state.getDestroySpeed(level(), pos) >= 0.0F) {
            level().destroyBlock(pos, true, getOwner());
        }
    }
}
