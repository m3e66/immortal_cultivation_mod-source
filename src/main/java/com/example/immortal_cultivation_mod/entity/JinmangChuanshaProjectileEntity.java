package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.particle.ModParticles;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import java.util.HashSet;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** A very narrow metal-qi spike that loses 20% damage after each penetration. */
public final class JinmangChuanshaProjectileEntity extends ThrowableItemProjectile
        implements DefensePiercingProjectile {
    public static final float BASE_DAMAGE = 420.0F;
    public static final int MAX_PENETRATIONS = 3;
    private static final int MAX_LIFETIME_TICKS = 20 * 30;

    private final Set<UUID> hitTargets = new HashSet<>();
    private int penetrations;
    private float remainingDamageScale = 1.0F;

    public JinmangChuanshaProjectileEntity(
            EntityType<? extends ThrowableItemProjectile> entityType,
            Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    public JinmangChuanshaProjectileEntity(Level level, LivingEntity caster) {
        super(ModEntities.JINMANG_CHUANSHA_PROJECTILE.get(), caster, level);
        NpcProfile.markNpcOwnedEffect(this, caster);
        setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.GOLD_NUGGET;
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

        Vec3 previous = position();
        super.tick();
        if (!level().isClientSide && isAlive()) {
            hitAdditionalTargets(previous, position());
        }
        if (!level().isClientSide && tickCount > MAX_LIFETIME_TICKS) {
            discard();
            return;
        }

        if (level().isClientSide) {
            Vec3 movement = getDeltaMovement();
            Vec3 back = movement.lengthSqr() < 1.0E-6D
                    ? Vec3.ZERO : movement.normalize().scale(-0.34D);
            for (int i = 0; i < 8; i++) {
                Vec3 point = position().add(back.scale(i));
                level().addParticle(ModParticles.METHOD_QI_METAL.get(),
                        point.x, point.y, point.z, 0.0D, 0.0D, 0.0D);
            }
            level().addParticle(ParticleTypes.ELECTRIC_SPARK,
                    getX(), getY(), getZ(),
                    -movement.x * 0.02D, -movement.y * 0.02D, -movement.z * 0.02D);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide && result.getType() == HitResult.Type.BLOCK) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        Entity target = ModelBonePartEntity.resolveParent(entity);
        return target instanceof LivingEntity
                && !hitTargets.contains(target.getUUID())
                && NpcProfile.canNpcEffectHit(this, getOwner(), target)
                && super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = ModelBonePartEntity.resolveParent(result.getEntity());
        if (!(target instanceof LivingEntity living) || !hitTargets.add(target.getUUID())) {
            return;
        }

        float damage = com.example.immortal_cultivation_mod.spell.SpellDamageHelper.damage(
                getOwner(), BASE_DAMAGE * remainingDamageScale,
                ModSpells.get(ModSpells.JINMANG_CHUANSHA));
        living.hurt(damageSources().thrown(this, getOwner()), damage);
        penetrations++;
        remainingDamageScale *= 0.80F;

        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ModParticles.METHOD_QI_METAL.get(),
                    living.getX(), living.getY() + living.getBbHeight() * 0.5D, living.getZ(),
                    22, 0.25D, 0.35D, 0.25D, 0.12D);
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    living.getX(), living.getY() + living.getBbHeight() * 0.5D, living.getZ(),
                    12, 0.22D, 0.30D, 0.22D, 0.08D);
            serverLevel.playSound(null, living.blockPosition(), SoundEvents.TRIDENT_HIT_GROUND,
                    getOwner() instanceof net.minecraft.server.level.ServerPlayer
                            ? SoundSource.PLAYERS : SoundSource.HOSTILE,
                    1.0F, 1.75F);
        }

        if (penetrations >= MAX_PENETRATIONS) {
            discard();
        } else {
            Vec3 direction = getDeltaMovement().normalize();
            setPos(getX() + direction.x * 0.20D,
                    getY() + direction.y * 0.20D,
                    getZ() + direction.z * 0.20D);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ModParticles.METHOD_QI_METAL.get(),
                    result.getLocation().x, result.getLocation().y, result.getLocation().z,
                    18, 0.18D, 0.18D, 0.18D, 0.08D);
            serverLevel.playSound(null, result.getBlockPos(), SoundEvents.TRIDENT_HIT_GROUND,
                    SoundSource.BLOCKS, 0.9F, 1.8F);
        }
    }

    private void hitAdditionalTargets(Vec3 start, Vec3 uncappedEnd) {
        Vec3 end = uncappedEnd;
        BlockHitResult blockHit = level().clip(new ClipContext(
                start, uncappedEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            end = blockHit.getLocation();
        }
        AABB path = new AABB(start, end).inflate(0.08D);
        Vec3 finalEnd = end;
        List<LivingEntity> targets = level().getEntitiesOfClass(
                        LivingEntity.class, path, this::canHitEntity)
                .stream()
                .filter(target -> target.getBoundingBox().inflate(0.06D).clip(start, finalEnd).isPresent())
                .sorted(Comparator.comparingDouble(target -> target.getBoundingBox().inflate(0.06D)
                        .clip(start, finalEnd).orElse(finalEnd).distanceToSqr(start)))
                .toList();
        for (LivingEntity target : targets) {
            if (!isAlive() || penetrations >= MAX_PENETRATIONS) {
                break;
            }
            Vec3 hit = target.getBoundingBox().inflate(0.06D)
                    .clip(start, finalEnd).orElse(target.getBoundingBox().getCenter());
            onHitEntity(new EntityHitResult(target, hit));
        }
    }

    public static boolean isArmorPiercing(DamageSource source) {
        return source != null && source.getDirectEntity() instanceof JinmangChuanshaProjectileEntity;
    }

    @Override
    public float defenseBypass() {
        return 0.30F;
    }

    @Override
    public float shieldDamageMultiplier() {
        return 1.50F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Penetrations", penetrations);
        tag.putFloat("RemainingDamageScale", remainingDamageScale);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        penetrations = Math.max(0, tag.getInt("Penetrations"));
        remainingDamageScale = tag.contains("RemainingDamageScale")
                ? Math.max(0.0F, tag.getFloat("RemainingDamageScale")) : 1.0F;
    }
}
