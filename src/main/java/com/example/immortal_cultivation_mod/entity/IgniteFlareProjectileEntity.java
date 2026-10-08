package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
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
        NpcProfile.markNpcOwnedEffect(this, shooter);
        setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.BLAZE_POWDER;
    }

    @Override
    public void tick() {
        setNoGravity(true);
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
            var movement = getDeltaMovement();
            for (int i = 0; i < 2; i++) {
                double back = i * 0.22D;
                level().addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                        getX() - movement.x * back,
                        getY() - movement.y * back,
                        getZ() - movement.z * back,
                        movement.x * 0.02D,
                        movement.y * 0.02D,
                        movement.z * 0.02D);
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            if (level() instanceof ServerLevel serverLevel) {
                com.example.immortal_cultivation_mod.network.SpellRibbonPayload.send(serverLevel,"ignite_flare",result.getLocation(),new net.minecraft.world.phys.Vec3(0,0,1),-1,18,.55F);
                SpellImpactParticles.flare(serverLevel, result.getLocation());
                ElementalTechniqueAreaEntity.damageFireSensitiveAreas(
                        serverLevel, result.getLocation(), 3.0D, 24);
            }
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return NpcProfile.canNpcEffectHit(this, getOwner(), entity) && super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide) return;
        Entity target = ModelBonePartEntity.resolveParent(result.getEntity());
        target.igniteForSeconds(5);
        if (target instanceof LivingEntity living)
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    com.example.immortal_cultivation_mod.effect.ModEffects.QI_FIRE_BURN, 100, 0, false, false, true));
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (level().isClientSide) {
            return;
        }

        Direction direction = result.getDirection();
        BlockPos firePos = result.getBlockPos().relative(direction);
        int radius = Math.max(0, Math.round(Math.max(1.0F, getPersistentData().getFloat("ChargeScale")) - 1.0F));
        for (BlockPos pos : BlockPos.betweenClosed(firePos.offset(-radius, 0, -radius), firePos.offset(radius, 0, radius))) {
            if (level().getBlockState(pos).isAir()) {
                level().setBlockAndUpdate(pos, ModBlocks.LING_FIRE.get().defaultBlockState());
            }
        }
    }
}
