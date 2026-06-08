package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.effect.PhotonEffects;
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
    public void tick() {
        super.tick();
        if (!level().isClientSide && photonEffectAttempts < 5) {
            photonEffectAttempts++;
            PhotonEffects.fireballProjectile(this);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide) return;
        level().explode(this, getX(), getY(), getZ(), 2.0f, false, Level.ExplosionInteraction.NONE);
        discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        float damage = 10.0f;
        if (getOwner() instanceof net.minecraft.world.entity.player.Player player) {
            damage += ModAttachments.getData(player).magicAttack();
        }
        result.getEntity().hurt(damageSources().thrown(this, getOwner()), damage);
        result.getEntity().igniteForSeconds(3);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
    }
}
