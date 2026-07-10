package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.spell.HantiBingqin;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public class FrostCryProjectileEntity extends ThrowableItemProjectile {
    public FrostCryProjectileEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    public FrostCryProjectileEntity(Level level, LivingEntity shooter) {
        super(ModEntities.FROST_CRY_PROJECTILE.get(), shooter, level);
        setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SNOWBALL;
    }

    @Override
    public void tick() {
        setNoGravity(true);
        super.tick();
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.SNOWFLAKE, getX(), getY(), getZ(),
                    -getDeltaMovement().x * 0.03D, -getDeltaMovement().y * 0.03D, -getDeltaMovement().z * 0.03D);
            level().addParticle(ParticleTypes.CLOUD, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            Entity owner = getOwner();
            if (owner instanceof ServerPlayer caster) {
                HantiBingqin.applyCryImpact(caster, result.getLocation(), Math.max(1.0F, getPersistentData().getFloat("ChargeScale")));
            }
            discard();
        }
    }
}
