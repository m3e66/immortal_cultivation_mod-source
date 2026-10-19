package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.item.ModItems;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.ForcefieldCenterBehavior;
import com.example.immortal_cultivation_mod.particle.ModParticles;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Shared physical projectile for deer feathers, tortoise pellets, and owl fire feathers. */
public final class CultivationBeastProjectileEntity extends ThrowableItemProjectile {
    // Visuals use synchronized BeastAttackVfx; server combat and sounds remain unchanged.
    public enum Kind { WIND_FEATHER, STONE_PELLET, FIRE_FEATHER }

    private static final EntityDataAccessor<Integer> DATA_KIND =
            SynchedEntityData.defineId(CultivationBeastProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_DAMAGE =
            SynchedEntityData.defineId(CultivationBeastProjectileEntity.class, EntityDataSerializers.FLOAT);

    public CultivationBeastProjectileEntity(
            EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    public void configure(LivingEntity owner, Kind kind, float damage) {
        setOwner(owner);
        entityData.set(DATA_KIND, kind.ordinal());
        entityData.set(DATA_DAMAGE, Math.max(0.0F, damage));
        setItem(new ItemStack(itemForKind(kind)));
        setNoGravity(kind != Kind.STONE_PELLET);
    }

    public Kind projectileKind() {
        // ThrowableItemProjectile asks getDefaultItem() while Entity is still
        // constructing, before SynchedEntityData has been assigned.
        if (entityData == null) return Kind.WIND_FEATHER;
        int ordinal = entityData.get(DATA_KIND);
        return Kind.values()[Math.max(0, Math.min(Kind.values().length - 1, ordinal))];
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_KIND, 0);
        builder.define(DATA_DAMAGE, 0.0F);
    }

    @Override
    protected Item getDefaultItem() {
        // Construction cannot depend on DATA_KIND. configure() replaces this
        // safe default with the item matching the selected projectile kind.
        return itemForKind(Kind.WIND_FEATHER);
    }

    private static Item itemForKind(Kind kind) {
        return switch (kind) {
            case WIND_FEATHER -> ModItems.WIND_FEATHER.get();
            case STONE_PELLET -> ModItems.MINERAL_MARROW.get();
            case FIRE_FEATHER -> ModItems.EMBER_FEATHER.get();
        };
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > 100) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        Entity resolved = ModelBonePartEntity.resolveParent(entity);
        return resolved != getOwner() && !(resolved instanceof CultivationBestiaryBeastEntity)
                && super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!(level() instanceof ServerLevel server)) return;
        LivingEntity target = ModelBonePartEntity.resolveLiving(result.getEntity());
        float damage = entityData.get(DATA_DAMAGE);
        if (target != null && canDamage(target, damage)
                && target.hurt(damageSources().indirectMagic(this, getOwner()), damage)) {
            switch (projectileKind()) {
                case WIND_FEATHER -> {
                    Vec3 push = getDeltaMovement().normalize().scale(0.28D);
                    target.push(push.x, 0.08D, push.z);
                }
                case STONE_PELLET -> {
                    Vec3 push = getDeltaMovement().normalize().scale(0.42D);
                    target.push(push.x, 0.15D, push.z);
                }
                case FIRE_FEATHER -> target.igniteForSeconds(3.0F);
            }
        }
        impact(server, result.getLocation());
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (level() instanceof ServerLevel server) impact(server, result.getLocation());
        discard();
    }

    private boolean canDamage(LivingEntity target, float damage) {
        Entity owner = getOwner();
        Entity source = owner == null ? this : owner;
        Vec3 origin = owner == null ? position() : owner.getBoundingBox().getCenter();
        return !ForcefieldCenterBehavior.interceptAttackPath(
                source, origin, target.getBoundingBox().getCenter(), damage);
    }

    private void impact(ServerLevel server, Vec3 point) {
        switch (projectileKind()) {
            case WIND_FEATHER -> {
                server.playSound(null, blockPosition(), SoundEvents.BREEZE_SHOOT,
                        SoundSource.HOSTILE, 0.7F, 1.35F);
            }
            case STONE_PELLET -> {
                server.playSound(null, blockPosition(), SoundEvents.DEEPSLATE_BREAK,
                        SoundSource.HOSTILE, 0.8F, 0.9F);
            }
            case FIRE_FEATHER -> {
                if (getOwner() instanceof CultivationBestiaryBeastEntity owl)
                    BeastMoveBlockBreaker.breakSphere(server, owl, point, 1.3, 1.1, 16, 8);
                CultivationBeastFirePatchEntity patch = ModEntities.CULTIVATION_BEAST_FIRE_PATCH.get().create(server);
                if (patch != null) {
                    patch.setPos(point.x, point.y + 0.05D, point.z);
                    patch.configure(getOwner(), 20.0F, 60, 1.35D);
                    server.addFreshEntity(patch);
                }
                server.playSound(null, blockPosition(), SoundEvents.FIRECHARGE_USE,
                        SoundSource.HOSTILE, 0.8F, 1.1F);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("BestiaryProjectileKind", projectileKind().ordinal());
        tag.putFloat("BestiaryProjectileDamage", entityData.get(DATA_DAMAGE));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_KIND, Math.max(0, Math.min(Kind.values().length - 1,
                tag.getInt("BestiaryProjectileKind"))));
        entityData.set(DATA_DAMAGE, Math.max(0.0F, tag.getFloat("BestiaryProjectileDamage")));
        setItem(new ItemStack(itemForKind(projectileKind())));
        setNoGravity(projectileKind() != Kind.STONE_PELLET);
    }
}
