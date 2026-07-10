package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.event.ServerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class YihenCiProjectileEntity extends ThrowableItemProjectile {
    public YihenCiProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public YihenCiProjectileEntity(Level level, LivingEntity owner) {
        super(ModEntities.YIHEN_CI_PROJECTILE.get(), owner, level);
        setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.AIR;
    }

    @Override
    public void tick() {
        setNoGravity(true);
        super.tick();
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.WITCH, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
            level().addParticle(ParticleTypes.SCULK_SOUL, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        } else if (tickCount > 80) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity hit = result.getEntity();
        if (hit instanceof Player player) {
            ServerEvents.adjustThoughts(player, -20);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            if (level() instanceof ServerLevel level) {
                spawnGroundSpike(level, result.getLocation());
            }
            discard();
        }
    }

    private void spawnGroundSpike(ServerLevel level, Vec3 hitLocation) {
        BlockPos ground = BlockPos.containing(hitLocation);
        if (level.getBlockState(ground).isAir()) {
            while (ground.getY() > level.getMinBuildHeight() && level.getBlockState(ground).isAir()) {
                ground = ground.below();
            }
        } else {
            while (ground.getY() < level.getMaxBuildHeight() - 1 && !level.getBlockState(ground.above()).isAir()) {
                ground = ground.above();
            }
        }

        Vec3 base = Vec3.atBottomCenterOf(ground.above());
        BlockState shardState = Blocks.DEEPSLATE.defaultBlockState();
        for (int i = 0; i < 18; i++) {
            double y = base.y + 0.08D + i * 0.24D;
            double spread = Math.max(0.05D, 0.72D - i * 0.035D);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, shardState),
                    base.x, y, base.z, 24, spread, 0.08D, spread, 0.14D);
            level.sendParticles(ParticleTypes.SCULK_SOUL,
                    base.x, y + 0.08D, base.z, 18, spread * 0.55D, 0.08D, spread * 0.55D, 0.06D);
            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.WITCH,
                        base.x, y + 0.12D, base.z, 10, spread * 0.35D, 0.1D, spread * 0.35D, 0.03D);
            }
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BLACKSTONE.defaultBlockState()),
                base.x, base.y + 0.2D, base.z, 80, 1.25D, 0.25D, 1.25D, 0.22D);
        level.sendParticles(ParticleTypes.SCULK_SOUL,
                base.x, base.y + 1.9D, base.z, 70, 0.8D, 1.3D, 0.8D, 0.09D);
        level.sendParticles(ParticleTypes.WITCH,
                base.x, base.y + 2.2D, base.z, 46, 0.65D, 1.2D, 0.65D, 0.05D);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                base.x, base.y + 0.75D, base.z, 4, 0.45D, 0.15D, 0.45D, 0.0D);
    }
}
