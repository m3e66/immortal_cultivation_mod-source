package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.magicritual.center.behavior.ForcefieldCenterBehavior;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SuokongProjectileEntity extends ThrowableItemProjectile {
    private Vec3 origin;
    private double maxDistance = 10.0D;

    public SuokongProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public SuokongProjectileEntity(Level level, ServerPlayer owner) {
        super(ModEntities.SUOKONG_PROJECTILE.get(), owner, level);
        setNoGravity(true);
        origin = owner.getEyePosition();
        maxDistance = Math.max(1.0D, owner.blockInteractionRange() * 2.0D);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.AIR;
    }

    @Override
    public void tick() {
        setNoGravity(true);
        super.tick();
        if (level().isClientSide || isRemoved()) {
            return;
        }
        if (ForcefieldCenterBehavior.interceptProjectileImpact(this, position())) {
            return;
        }
        if (origin == null) {
            origin = position();
            if (getOwner() instanceof ServerPlayer owner) {
                maxDistance = Math.max(1.0D, owner.blockInteractionRange() * 2.0D);
            }
        }
        if (position().distanceToSqr(origin) >= maxDistance * maxDistance) {
            activate(position());
        } else if (tickCount > 200) {
            discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            activate(result.getLocation());
        }
    }

    private void activate(Vec3 impact) {
        if (!(level() instanceof ServerLevel serverLevel) || isRemoved()) {
            return;
        }
        int count = blockCount();
        BlockPos center = BlockPos.containing(impact);
        List<BlockPos> candidates = new ArrayList<>();
        for (int radius = 1; radius <= 8 && candidates.size() < count; radius++) {
            candidates.clear();
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius),
                    center.offset(radius, radius, radius))) {
                if (pos.distSqr(center) <= radius * radius
                        && serverLevel.isInWorldBounds(pos)
                        && serverLevel.getBlockState(pos).isAir()) {
                    candidates.add(pos.immutable());
                }
            }
        }
        candidates.sort(Comparator
                .comparingDouble((BlockPos pos) -> pos.distSqr(center))
                .thenComparingInt(BlockPos::getY)
                .thenComparingInt(BlockPos::getX)
                .thenComparingInt(BlockPos::getZ));
        for (int i = 0; i < Math.min(count, candidates.size()); i++) {
            serverLevel.setBlock(candidates.get(i), ModBlocks.SUOKONG_BARRIER.get().defaultBlockState(), 3);
        }
        discard();
    }

    private int blockCount() {
        if (!(getOwner() instanceof ServerPlayer owner)) {
            return 10;
        }
        String realm = CultivationLevels.getLevelDef(ModAttachments.getData(owner).cultivationLevel()).realm();
        if (CultivationLevels.REALM_JINDAN.equals(realm)
                || CultivationLevels.REALM_YUANYING.equals(realm)
                || CultivationLevels.REALM_HUASHEN.equals(realm)
                || CultivationLevels.REALM_LIANXU.equals(realm)
                || CultivationLevels.REALM_HETI.equals(realm)
                || CultivationLevels.REALM_DACHENG.equals(realm)
                || CultivationLevels.REALM_DUJIE.equals(realm)) {
            return 100;
        }
        return CultivationLevels.REALM_ZHUJI.equals(realm) ? 50 : 10;
    }
}
