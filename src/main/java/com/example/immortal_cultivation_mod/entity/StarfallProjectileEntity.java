package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.ForcefieldCenterBehavior;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class StarfallProjectileEntity extends ThrowableItemProjectile {
    public static final double BLAST_RADIUS = 30.0D;
    private static final float IMPACT_DAMAGE = 300.0F;
    // Terrain damage is deliberately smaller than the spell's combat area.
    private static final float TERRAIN_EXPLOSION_POWER = 6.0F;
    private static final int MAX_LIFETIME_TICKS = 20 * 8;
    private static final DustParticleOptions GOLD_DUST =
            new DustParticleOptions(new Vector3f(1.0F, 0.72F, 0.12F), 1.4F);
    private static final DustParticleOptions PALE_DUST =
            new DustParticleOptions(new Vector3f(0.78F, 0.88F, 1.0F), 0.95F);
    private static final ExplosionDamageCalculator TERRAIN_ONLY_EXPLOSION = new ExplosionDamageCalculator() {
        @Override
        public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
            return false;
        }

        @Override
        public float getKnockbackMultiplier(Entity entity) {
            return 0.0F;
        }

        @Override
        public boolean shouldBlockExplode(Explosion explosion, BlockGetter level, BlockPos pos,
                                          BlockState state, float power) {
            return !state.is(ModBlocks.STAR_MARK.get())
                    && !level.getBlockState(pos.above()).is(ModBlocks.STAR_MARK.get())
                    && super.shouldBlockExplode(explosion, level, pos, state, power);
        }
    };
    private boolean impacted;

    public StarfallProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public StarfallProjectileEntity(Level level, LivingEntity owner) {
        super(ModEntities.STARFALL_PROJECTILE.get(), owner, level);
        NpcProfile.markNpcOwnedEffect(this, owner);
        setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.AIR;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && NpcProfile.canNpcEffectHit(this, getOwner(), entity);
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
        if (isRemoved()) {
            return;
        }
        if (!level().isClientSide
                && ForcefieldCenterBehavior.interceptProjectileImpact(this, position())) {
            return;
        }
        if (level().isClientSide && !isRemoved()) {
            spawnTrail();
        } else if (!level().isClientSide && tickCount > MAX_LIFETIME_TICKS) {
            impact(position());
        }
    }

    private void spawnTrail() {
        Vec3 motion = getDeltaMovement();
        Vec3 backward = motion.lengthSqr() < 0.001D
                ? new Vec3(0.0D, 1.0D, 0.0D)
                : motion.normalize().reverse();
        double phase = tickCount * 0.72D;
        for (int i = 0; i < 14; i++) {
            double angle = phase + Math.PI * 2.0D * i / 14.0D;
            double radius = 0.72D + (i & 1) * 0.18D;
            level().addParticle(i % 3 == 0 ? GOLD_DUST : PALE_DUST,
                    getX() + Math.cos(angle) * radius,
                    getY() + Math.sin(angle * 1.5D) * 0.58D,
                    getZ() + Math.sin(angle) * radius,
                    0.0D, 0.0D, 0.0D);
        }
        level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        level().addParticle(ParticleTypes.GLOW, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        for (int tail = 1; tail <= 7; tail++) {
            double distance = tail * 0.72D;
            Vec3 tailPosition = position().add(backward.scale(distance));
            double spread = tail * 0.025D;
            level().addParticle(tail % 2 == 0 ? GOLD_DUST : PALE_DUST,
                    tailPosition.x + random.nextGaussian() * spread,
                    tailPosition.y + random.nextGaussian() * spread,
                    tailPosition.z + random.nextGaussian() * spread,
                    backward.x * 0.025D, backward.y * 0.025D, backward.z * 0.025D);
            level().addParticle(tail % 3 == 0 ? ParticleTypes.FLAME : ParticleTypes.END_ROD,
                    tailPosition.x, tailPosition.y, tailPosition.z,
                    backward.x * 0.04D, backward.y * 0.04D, backward.z * 0.04D);
            if ((tail & 1) == 0) {
                level().addParticle(ParticleTypes.FIREWORK,
                        tailPosition.x, tailPosition.y, tailPosition.z,
                        backward.x * 0.03D, backward.y * 0.03D, backward.z * 0.03D);
            }
        }
        level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(),
                random.nextGaussian() * 0.08D, 0.04D, random.nextGaussian() * 0.08D);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            impact(result.getLocation());
        }
    }

    private void impact(Vec3 center) {
        if (impacted || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Entity owner = getOwner();
        NpcProfile.markNpcOwnedEffect(this, owner);
        if (NpcProfile.isOrphanedNpcEffect(this, owner)) {
            discard();
            return;
        }
        impacted = true;
        com.example.immortal_cultivation_mod.spell.SpellDestruction.explode(serverLevel,owner, null, TERRAIN_ONLY_EXPLOSION, center,
                TERRAIN_EXPLOSION_POWER, false, Level.ExplosionInteraction.BLOCK);
        damageArea(serverLevel, center);
        spawnImpactParticles(serverLevel, center);
        placeStarMark(serverLevel, center);
        discard();
    }

    private void damageArea(ServerLevel level, Vec3 center) {
        Entity owner = getOwner();
        AABB area = new AABB(center, center).inflate(BLAST_RADIUS);
        var source = damageSources().indirectMagic(this, owner);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                target -> target.isAlive() && target != owner
                        && NpcProfile.canNpcEffectHit(this, owner, target)
                        && target.position().distanceToSqr(center) <= BLAST_RADIUS * BLAST_RADIUS)) {
            target.hurt(source, IMPACT_DAMAGE);
        }
    }

    private void spawnImpactParticles(ServerLevel level, Vec3 center) {
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                center.x, center.y, center.z, 3, 1.5D, 1.0D, 1.5D, 0.0D);
        level.sendParticles(ParticleTypes.FLASH,
                center.x, center.y + 0.5D, center.z, 4, 0.8D, 0.8D, 0.8D, 0.0D);
        level.sendParticles(GOLD_DUST,
                center.x, center.y + 0.4D, center.z, 130, 7.5D, 3.5D, 7.5D, 0.08D);
        level.sendParticles(PALE_DUST,
                center.x, center.y + 0.8D, center.z, 105, 6.0D, 4.5D, 6.0D, 0.11D);
        level.sendParticles(ParticleTypes.END_ROD,
                center.x, center.y + 0.7D, center.z, 85, 5.5D, 4.0D, 5.5D, 0.28D);
        level.sendParticles(ParticleTypes.FIREWORK,
                center.x, center.y + 0.7D, center.z, 70, 6.5D, 3.5D, 6.5D, 0.22D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                center.x, center.y + 0.5D, center.z, 95, 7.0D, 2.5D, 7.0D, 0.34D);
        level.sendParticles(ParticleTypes.GLOW,
                center.x, center.y + 1.0D, center.z, 65, 5.0D, 3.0D, 5.0D, 0.14D);
    }

    private void placeStarMark(ServerLevel level, Vec3 center) {
        BlockPos.MutableBlockPos cursor = BlockPos.containing(center).mutable();
        int minimumY = level.getMinBuildHeight();
        while (cursor.getY() > minimumY && level.getBlockState(cursor).isAir()) {
            cursor.move(Direction.DOWN);
        }
        if (level.getBlockState(cursor).isAir()) {
            return;
        }
        BlockPos markPos = cursor.above().immutable();
        BlockState markState = ModBlocks.STAR_MARK.get().defaultBlockState();
        if ((level.getBlockState(markPos).isAir() || level.getBlockState(markPos).canBeReplaced())
                && markState.canSurvive(level, markPos)) {
            level.setBlock(markPos, markState, 3);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Impacted", impacted);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        impacted = tag.getBoolean("Impacted");
    }
}
