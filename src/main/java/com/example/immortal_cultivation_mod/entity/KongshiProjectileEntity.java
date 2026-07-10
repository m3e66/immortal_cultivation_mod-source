package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.spell.UndeadControl;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class KongshiProjectileEntity extends ThrowableItemProjectile {
    public KongshiProjectileEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    public KongshiProjectileEntity(Level level, LivingEntity shooter) {
        super(ModEntities.KONGSHI_PROJECTILE.get(), shooter, level);
        setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.ECHO_SHARD;
    }

    @Override
    public void tick() {
        setNoGravity(true);
        super.tick();
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY(), getZ(),
                    -getDeltaMovement().x * 0.02D,
                    -getDeltaMovement().y * 0.02D,
                    -getDeltaMovement().z * 0.02D);
            level().addParticle(ParticleTypes.WITCH,
                    getX() + (random.nextDouble() - 0.5D) * 0.14D,
                    getY() + (random.nextDouble() - 0.5D) * 0.14D,
                    getZ() + (random.nextDouble() - 0.5D) * 0.14D,
                    0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity owner = getOwner();
        if (owner instanceof ServerPlayer caster
                && result.getEntity() instanceof LivingEntity target
                && UndeadControl.isUndeadServantType(target)) {
            UndeadControl.tame(caster, target);
        }
        discard();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide && !(result instanceof EntityHitResult)) {
            discard();
        }
    }
}
