package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.item.FlyingNeedleItem;
import com.example.immortal_cultivation_mod.item.ForgingSystem;
import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.item.QiInfusedWeapon;
import com.example.immortal_cultivation_mod.item.WeaponRestrictionSystem;
import com.example.immortal_cultivation_mod.particle.MethodQiParticleEffects;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class FlyingNeedleEntity extends ThrowableItemProjectile {
    private ItemStack weaponStack = ItemStack.EMPTY;
    private float injectedQiDamage;
    private double launchQi;
    private boolean explosive;

    public FlyingNeedleEntity(
            EntityType<? extends ThrowableItemProjectile> type,
            Level level) {
        super(type, level);
    }

    public FlyingNeedleEntity(
            ServerLevel level,
            ServerPlayer owner,
            ItemStack stack,
            float injectedQiDamage,
            double launchQi,
            boolean explosive) {
        super(ModEntities.FLYING_NEEDLE.get(), owner, level);
        setOwner(owner);
        weaponStack = stack.copyWithCount(1);
        setItem(weaponStack);
        this.injectedQiDamage = Math.max(0.0F, injectedQiDamage);
        this.launchQi = Math.max(0.0D, launchQi);
        this.explosive = explosive;
        Vec3 origin = owner.getEyePosition()
                .add(owner.getLookAngle().normalize().scale(0.65D))
                .add(0.0D, -0.08D, 0.0D);
        setPos(origin.x, origin.y, origin.z);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.FLYING_NEEDLE.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel serverLevel
                && launchQi > 0.0D) {
            emitQiTrail(serverLevel);
        }
        if (!level().isClientSide && tickCount > 200) {
            discard();
        }
    }

    private void emitQiTrail(ServerLevel level) {
        List<SimpleParticleType> particles =
                getOwner() instanceof ServerPlayer player
                        ? MethodQiParticleEffects.particlesForPlayer(player)
                        : List.of(ModParticles.METHOD_QI_NEUTRAL.get());
        Vec3 movement = getDeltaMovement();
        Vec3 direction = movement.lengthSqr() < 0.0001D
                ? new Vec3(0.0D, 0.0D, 1.0D)
                : movement.normalize();
        for (int index = 0; index < 4; index++) {
            Vec3 point = position()
                    .subtract(direction.scale(index * 0.11D));
            SimpleParticleType particle =
                    particles.get((tickCount + index) % particles.size());
            level.sendParticles(
                    particle,
                    point.x,
                    point.y,
                    point.z,
                    1,
                    0.012D,
                    0.012D,
                    0.012D,
                    0.004D);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide) {
            return;
        }
        Entity target = ModelBonePartEntity.resolveParent(result.getEntity());
        if (target == getOwner()) {
            return;
        }

        float damage = (float) Math.max(
                FlyingNeedleItem.BASE_DAMAGE,
                QiInfusedWeapon.baseAttackDamage(weaponStack))
                + injectedQiDamage;
        if (target instanceof LivingEntity living) {
            damage = ForgingSystem.adjustWeaponDamage(
                    weaponStack, living, damage);
            damage = WeaponRestrictionSystem.adjustWeaponDamage(
                    weaponStack, living, damage);
        }
        boolean damaged = target.hurt(
                damageSources().thrown(this, getOwner()), damage);
        if (damaged && target instanceof LivingEntity living) {
            ForgingSystem.applyWeaponHitEffects(weaponStack, living);
            WeaponRestrictionSystem.applyWeaponHitEffects(
                    weaponStack, living);
        }
        finishImpact(result.getLocation());
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!level().isClientSide) {
            finishImpact(result.getLocation());
        }
    }

    private void finishImpact(Vec3 location) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            discard();
            return;
        }
        if (explosive && launchQi > 0.0D) {
            float radius = Mth.clamp(
                    1.5F + (float) Math.sqrt(launchQi) / 8.0F,
                    1.5F,
                    8.0F);
            serverLevel.sendParticles(
                    ParticleTypes.EXPLOSION_EMITTER,
                    location.x,
                    location.y,
                    location.z,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D);
            Entity source = getOwner() == null ? this : getOwner();
            serverLevel.explode(
                    source,
                    location.x,
                    location.y,
                    location.z,
                    radius,
                    false,
                    Level.ExplosionInteraction.BLOCK);
        } else {
            serverLevel.sendParticles(
                    ParticleTypes.CRIT,
                    location.x,
                    location.y,
                    location.z,
                    8,
                    0.15D,
                    0.15D,
                    0.15D,
                    0.05D);
        }
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("InjectedQiDamage", injectedQiDamage);
        tag.putDouble("LaunchQi", launchQi);
        tag.putBoolean("Explosive", explosive);
        if (!weaponStack.isEmpty()) {
            tag.put(
                    "NeedleStack",
                    weaponStack.saveOptional(registryAccess()));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        injectedQiDamage = tag.getFloat("InjectedQiDamage");
        launchQi = tag.getDouble("LaunchQi");
        explosive = tag.getBoolean("Explosive");
        if (tag.contains("NeedleStack")) {
            weaponStack = ItemStack.parseOptional(
                    registryAccess(), tag.getCompound("NeedleStack"));
        }
        if (weaponStack.isEmpty()) {
            weaponStack = new ItemStack(ModItems.FLYING_NEEDLE.get());
        }
        setItem(weaponStack);
    }
}
