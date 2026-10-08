package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Owns destructible temporary terrain and restores only blocks that still match its placements. */
public final class TemporaryTerrainEntity extends Entity {
    private final Map<BlockPos, TrackedBlock> trackedBlocks = new LinkedHashMap<>();
    private long expireGameTime;

    public TemporaryTerrainEntity(
            EntityType<? extends TemporaryTerrainEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
        noCulling = true;
    }

    public static TemporaryTerrainEntity place(
            ServerLevel level, Map<BlockPos, BlockState> placements, int lifetimeTicks) {
        return place(level, placements, lifetimeTicks, false);
    }

    /** Temporarily replaces existing simple blocks and restores their prior block type on expiry. */
    public static TemporaryTerrainEntity replaceAndRestore(
            ServerLevel level, Map<BlockPos, BlockState> placements, int lifetimeTicks) {
        return place(level, placements, lifetimeTicks, true);
    }

    private static TemporaryTerrainEntity place(
            ServerLevel level, Map<BlockPos, BlockState> placements, int lifetimeTicks,
            boolean replaceExisting) {
        if (placements == null || placements.isEmpty()) {
            return null;
        }
        TemporaryTerrainEntity anchor = ModEntities.TEMPORARY_TERRAIN.get().create(level);
        if (anchor == null) {
            return null;
        }
        double centerX = 0.0D;
        double centerY = 0.0D;
        double centerZ = 0.0D;
        int placed = 0;
        var source = BlockBreakingSettings.source(
                BlockBreakingSettings.Category.TECHNIQUES, null);
        for (Map.Entry<BlockPos, BlockState> entry : placements.entrySet()) {
            BlockPos pos = entry.getKey().immutable();
            BlockState state = entry.getValue();
            BlockState original = level.getBlockState(pos);
            if (state == null
                    || replaceExisting && original.hasBlockEntity()
                    || !replaceExisting && !canReplace(level, pos)
                    || original.is(state.getBlock())) {
                continue;
            }
            boolean accepted = !original.isAir() && source.actor() instanceof net.minecraft.world.entity.LivingEntity actor
                    ? com.example.immortal_cultivation_mod.spell.TerrainTransforms.replace(level, pos, state, actor, source.category())
                    : level.setBlock(pos, state, Block.UPDATE_ALL);
            if (!accepted) {
                continue;
            }
            anchor.trackedBlocks.put(pos, new TrackedBlock(state.getBlock(), original.getBlock()));
            centerX += pos.getX() + 0.5D;
            centerY += pos.getY() + 0.5D;
            centerZ += pos.getZ() + 0.5D;
            placed++;
        }
        if (placed == 0) {
            return null;
        }
        anchor.setPos(centerX / placed, centerY / placed, centerZ / placed);
        anchor.expireGameTime = level.getGameTime() + Math.max(1, lifetimeTicks);
        level.addFreshEntity(anchor);
        return anchor;
    }

    public static boolean canReplace(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        noPhysics = true;
        if (!level().isClientSide && level().getGameTime() >= expireGameTime) {
            cleanupNow();
        }
    }

    public void cleanupNow() {
        if (level() instanceof ServerLevel level) {
            for (Map.Entry<BlockPos, TrackedBlock> entry : trackedBlocks.entrySet()) {
                BlockPos pos = entry.getKey();
                TrackedBlock tracked = entry.getValue();
                if (level.hasChunkAt(pos) && level.getBlockState(pos).is(tracked.expected())) {
                    if (tracked.original().defaultBlockState().isAir()) {
                        level.removeBlock(pos, false);
                    } else {
                        level.setBlock(pos, tracked.original().defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }
        trackedBlocks.clear();
        discard();
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        expireGameTime = tag.getLong("ExpireGameTime");
        trackedBlocks.clear();
        ListTag blocks = tag.getList("Blocks", Tag.TAG_COMPOUND);
        for (int index = 0; index < blocks.size(); index++) {
            CompoundTag entry = blocks.getCompound(index);
            Block block = BuiltInRegistries.BLOCK.get(
                    ResourceLocation.parse(entry.getString("Block")));
            Block original = entry.contains("OriginalBlock", Tag.TAG_STRING)
                    ? BuiltInRegistries.BLOCK.get(
                    ResourceLocation.parse(entry.getString("OriginalBlock")))
                    : net.minecraft.world.level.block.Blocks.AIR;
            if (block != null) {
                trackedBlocks.put(BlockPos.of(entry.getLong("Pos")),
                        new TrackedBlock(block, original));
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putLong("ExpireGameTime", expireGameTime);
        ListTag blocks = new ListTag();
        for (Map.Entry<BlockPos, TrackedBlock> tracked : trackedBlocks.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Pos", tracked.getKey().asLong());
            entry.putString("Block", BuiltInRegistries.BLOCK.getKey(
                    tracked.getValue().expected()).toString());
            entry.putString("OriginalBlock", BuiltInRegistries.BLOCK.getKey(
                    tracked.getValue().original()).toString());
            blocks.add(entry);
        }
        tag.put("Blocks", blocks);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    private record TrackedBlock(Block expected, Block original) {
    }
}
