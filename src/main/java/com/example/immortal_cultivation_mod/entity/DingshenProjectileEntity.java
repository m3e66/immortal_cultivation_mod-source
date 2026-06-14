package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class DingshenProjectileEntity extends ThrowableItemProjectile {
    public DingshenProjectileEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    public DingshenProjectileEntity(Level level, LivingEntity shooter) {
        super(ModEntities.DINGSHEN_PROJECTILE.get(), shooter, level);
        setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.GOLD_NUGGET;
    }

    @Override
    public void tick() {
        setNoGravity(true);
        super.tick();
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.END_ROD,
                    getX(), getY(), getZ(),
                    -getDeltaMovement().x * 0.03D,
                    -getDeltaMovement().y * 0.03D,
                    -getDeltaMovement().z * 0.03D);
            level().addParticle(ParticleTypes.ELECTRIC_SPARK,
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
                SpellImpactParticles.light(serverLevel, result.getLocation());
            }
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity owner = getOwner();
        if (!(owner instanceof ServerPlayer caster) || !(result.getEntity() instanceof LivingEntity target)) {
            return;
        }

        BindingStrength strength = bindingStrength(ModAttachments.getData(caster).cultivationLevel(), targetLevel(target));
        if (strength == BindingStrength.NONE) {
            return;
        }

        target.addEffect(new MobEffectInstance(ModEffects.DINGSHEN, strength.durationTicks(), 0, false, false, true));
        if (strength.slownessAmplifier() >= 0) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, strength.durationTicks(), strength.slownessAmplifier(), false, false, true));
        }
    }

    private static String targetLevel(LivingEntity target) {
        if (target instanceof Player player) {
            return ModAttachments.getData(player).cultivationLevel();
        }
        return CultivationLevels.REALM_MORTAL;
    }

    private static BindingStrength bindingStrength(String casterLevel, String targetLevel) {
        int casterStage = CultivationLevels.getStageIndex(casterLevel);
        int targetStage = CultivationLevels.getStageIndex(targetLevel);
        if (targetStage >= casterStage) {
            return BindingStrength.NONE;
        }

        int realmDiff = CultivationLevels.getRealmIndex(casterLevel) - CultivationLevels.getRealmIndex(targetLevel);
        if (realmDiff == 1) {
            return BindingStrength.BIG_LEVEL;
        }
        if (realmDiff == 0 && casterStage - targetStage == 1) {
            return BindingStrength.SMALL_LEVEL;
        }
        return BindingStrength.OVERWHELMING;
    }

    private enum BindingStrength {
        NONE(0, -1),
        SMALL_LEVEL(20 * 3, -1),
        BIG_LEVEL(20 * 5, -1),
        OVERWHELMING(20 * 10, 9);

        private final int durationTicks;
        private final int slownessAmplifier;

        BindingStrength(int durationTicks, int slownessAmplifier) {
            this.durationTicks = durationTicks;
            this.slownessAmplifier = slownessAmplifier;
        }

        private int durationTicks() {
            return durationTicks;
        }

        private int slownessAmplifier() {
            return slownessAmplifier;
        }
    }
}
