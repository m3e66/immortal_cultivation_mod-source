package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.phys.Vec3;

public class AbsorbCultivationProjectileEntity extends ThrowableItemProjectile {
    public AbsorbCultivationProjectileEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    public AbsorbCultivationProjectileEntity(Level level, LivingEntity shooter) {
        super(ModEntities.ABSORB_CULTIVATION_PROJECTILE.get(), shooter, level);
        setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.ENDER_PEARL;
    }

    @Override
    public void tick() {
        setNoGravity(true);
        super.tick();
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.REVERSE_PORTAL,
                    getX(), getY(), getZ(),
                    -getDeltaMovement().x * 0.05D,
                    -getDeltaMovement().y * 0.05D,
                    -getDeltaMovement().z * 0.05D);
            level().addParticle(ParticleTypes.WITCH,
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
                darkImpact(serverLevel, result.getLocation());
            }
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity owner = getOwner();
        if (!(owner instanceof ServerPlayer caster) || !(result.getEntity() instanceof ServerPlayer target)) {
            return;
        }

        var casterData = ModAttachments.getData(caster);
        var targetData = ModAttachments.getData(target);
        if (CultivationLevels.getStageIndex(targetData.cultivationLevel()) >= CultivationLevels.getStageIndex(casterData.cultivationLevel())) {
            return;
        }

        long targetMaxProgress = Math.max(1L, CultivationLevels.getTotalQiNeeded(targetData.cultivationLevel()));
        long steal = Math.max(1L, targetMaxProgress / 10L);
        DrainResult drained = drainProgress(targetData, steal);
        if (drained.amount() <= 0L) {
            return;
        }

        long casterNeed = Math.max(1L, CultivationLevels.getTotalQiNeeded(casterData.cultivationLevel()));
        ModAttachments.setData(target, drained.data());
        ModAttachments.setData(caster, casterData.withCultivationProgress(Math.min(casterNeed, casterData.cultivationProgress() + drained.amount())));
        com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(target);
        com.example.immortal_cultivation_mod.event.ServerEvents.syncPlayerData(caster);

        if (level() instanceof ServerLevel serverLevel) {
            returnParticles(serverLevel, target.position().add(0.0D, target.getBbHeight() * 0.6D, 0.0D),
                    caster.position().add(0.0D, caster.getBbHeight() * 0.6D, 0.0D));
        }
    }

    private DrainResult drainProgress(ModAttachments.CultivationData data, long amount) {
        long remaining = amount;
        long drained = 0L;
        String level = data.cultivationLevel();
        long progress = data.cultivationProgress();

        while (remaining > 0L) {
            if (progress >= remaining) {
                progress -= remaining;
                drained += remaining;
                remaining = 0L;
                break;
            }

            drained += progress;
            remaining -= progress;
            String previous = CultivationLevels.getPreviousStage(level);
            if (previous == null) {
                level = CultivationLevels.REALM_MORTAL;
                progress = 0L;
                break;
            }
            level = previous;
            progress = CultivationLevels.getTotalQiNeeded(level);
        }

        return new DrainResult(data.withCultivationLevel(level).withCultivationProgress(progress), drained);
    }

    private void darkImpact(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.WITCH, pos.x, pos.y, pos.z, 24, 0.35D, 0.35D, 0.35D, 0.08D);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y, pos.z, 32, 0.4D, 0.4D, 0.4D, 0.12D);
    }

    private void returnParticles(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        for (int i = 0; i <= 18; i++) {
            double t = i / 18.0D;
            Vec3 pos = from.add(delta.scale(t));
            level.sendParticles(ParticleTypes.WITCH, pos.x, pos.y, pos.z, 2, 0.04D, 0.04D, 0.04D, 0.01D);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y, pos.z, 2, 0.03D, 0.03D, 0.03D, 0.02D);
        }
    }

    private record DrainResult(ModAttachments.CultivationData data, long amount) {
    }
}
