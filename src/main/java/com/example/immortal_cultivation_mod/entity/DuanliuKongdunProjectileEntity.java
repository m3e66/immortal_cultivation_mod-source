package com.example.immortal_cultivation_mod.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public class DuanliuKongdunProjectileEntity extends ThrowableItemProjectile {
    public DuanliuKongdunProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public DuanliuKongdunProjectileEntity(Level level, LivingEntity owner) {
        super(ModEntities.DUANLIU_KONGDUN_PROJECTILE.get(), owner, level);
        setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.BLUE_DYE;
    }

    @Override
    public void tick() {
        setNoGravity(true);
        super.tick();
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.SPLASH, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide && getOwner() instanceof ServerPlayer owner && level() instanceof ServerLevel level) {
            level.addFreshEntity(new DuanliuKongdunDomeEntity(level, owner, result.getLocation()));
            SpellImpactParticles.water(level, result.getLocation());
            discard();
        }
    }
}
