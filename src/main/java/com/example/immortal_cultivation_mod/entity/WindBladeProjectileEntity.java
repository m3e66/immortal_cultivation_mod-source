package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.spell.ModSpells;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class WindBladeProjectileEntity extends ThrowableItemProjectile {
    public WindBladeProjectileEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    public WindBladeProjectileEntity(Level level, LivingEntity shooter) {
        super(ModEntities.WIND_BLADE_PROJECTILE.get(), shooter, level);
        setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.IRON_SWORD;
    }

    @Override
    public void tick() {
        setNoGravity(true);
        super.tick();
        if (level().isClientSide) {
            var movement = getDeltaMovement();
            level().addParticle(ParticleTypes.SWEEP_ATTACK,
                    getX(), getY(), getZ(),
                    movement.x * 0.02D, movement.y * 0.02D, movement.z * 0.02D);
            level().addParticle(ParticleTypes.CLOUD,
                    getX() - movement.x * 0.16D,
                    getY() - movement.y * 0.16D,
                    getZ() - movement.z * 0.16D,
                    movement.x * 0.01D, movement.y * 0.01D, movement.z * 0.01D);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            if (level() instanceof ServerLevel serverLevel) {
                SpellImpactParticles.wind(serverLevel, result.getLocation());
            }
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        float damage = com.example.immortal_cultivation_mod.spell.SpellDamageHelper.damage(
                getOwner(),
                8.0F,
                ModSpells.get(ModSpells.WIND_BLADE)
        );
        result.getEntity().hurt(damageSources().thrown(this, getOwner()), damage);    }
}
