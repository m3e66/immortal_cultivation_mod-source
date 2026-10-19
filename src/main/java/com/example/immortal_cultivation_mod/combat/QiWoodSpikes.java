package com.example.immortal_cultivation_mod.combat;

import com.example.immortal_cultivation_mod.entity.TemporaryTerrainEntity;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Evergreen pollen erupts into real, temporary Minecraft log blocks. */
public final class QiWoodSpikes {
    public static final int COUNT = 10;
    public static final int LIFETIME_TICKS = 40;
    public static final int MAX_BLOCKS = 256;
    public static final double BOUNDS_RADIUS = 8;

    /** Face-connected voxel branches, snapped to the world grid rather than a separate VFX mesh. */
    public static Map<BlockPos, BlockState> pattern(Vec3 center) {
        Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        if (!Double.isFinite(center.x) || !Double.isFinite(center.y) || !Double.isFinite(center.z)) return blocks;
        BlockPos root = BlockPos.containing(center);
        Vec3 origin = Vec3.atCenterOf(root);
        blocks.put(root, Blocks.OAK_LOG.defaultBlockState());
        for (int i = 0; i < COUNT && blocks.size() < MAX_BLOCKS; i++) {
            double y = 1 - 2 * (i + .5) / COUNT;
            double radius = Math.sqrt(1 - y * y), angle = i * 2.399963229728653;
            Vec3 direction = new Vec3(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
            Direction.Axis axis = Math.abs(y) >= Math.max(Math.abs(direction.x), Math.abs(direction.z))
                    ? Direction.Axis.Y : Math.abs(direction.x) > Math.abs(direction.z) ? Direction.Axis.X : Direction.Axis.Z;
            BlockState log = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
            double length = 5.2 + (i % 3) * .6;
            int samples = (int)Math.ceil(length * 4);
            BlockPos previous = root;
            for (int sample = 1; sample <= samples && blocks.size() < MAX_BLOCKS; sample++) {
                BlockPos next = BlockPos.containing(origin.add(direction.scale(length * sample / samples)));
                // Join diagonal steps through cube faces, so every spike is one connected branch.
                while (!previous.equals(next) && blocks.size() < MAX_BLOCKS) {
                    int dx = next.getX() - previous.getX(), dy = next.getY() - previous.getY(), dz = next.getZ() - previous.getZ();
                    if (dy != 0) previous = previous.offset(0, Integer.signum(dy), 0);
                    else if (dx != 0) previous = previous.offset(Integer.signum(dx), 0, 0);
                    else previous = previous.offset(0, 0, Integer.signum(dz));
                    blocks.putIfAbsent(previous, log);
                    // Thick square roots narrow into single-block tips along the outer branches.
                    if (sample / (double)samples <= .4) {
                        for (int u = -1; u <= 1; u++) for (int v = -1; v <= 1; v++) {
                            BlockPos thick = switch (axis) {
                                case X -> previous.offset(0, u, v);
                                case Y -> previous.offset(u, 0, v);
                                case Z -> previous.offset(u, v, 0);
                            };
                            if (blocks.size() < MAX_BLOCKS) blocks.putIfAbsent(thick, log);
                        }
                    }
                }
            }
        }
        return blocks;
    }

    /** Returns only accepted world placements; damage must not extend through protected or occupied terrain. */
    public static Map<BlockPos, BlockState> erupt(ServerLevel level, LivingEntity owner, Vec3 center) {
        return erupt(level, owner, center, owner == null ? 0 : QiDomainDamage.amount(owner, 1));
    }

    public static Map<BlockPos, BlockState> erupt(ServerLevel level, LivingEntity owner, Vec3 center, float damage) {
        return erupt(level,owner,center,damage,false);
    }
    public static Map<BlockPos, BlockState> eruptDomain(ServerLevel level,LivingEntity owner,Vec3 center,float damage) {
        return erupt(level,owner,center,damage,true);
    }
    private static Map<BlockPos, BlockState> erupt(ServerLevel level,LivingEntity owner,Vec3 center,float damage,boolean domain) {
        if (owner == null || owner.level() != level || !owner.isAlive()) return Map.of();
        var candidates = pattern(center);
        if(domain){var volume=QiDomainSystem.volume(owner);candidates.keySet().removeIf(pos->!volume.contains(Vec3.atCenterOf(pos)));}
        if (candidates.isEmpty()) return Map.of();
        var protectedEntities = level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(BOUNDS_RADIUS),
                entity -> !QiDomainSystem.canDomainAffect(owner, entity));
        var byChunk = new LinkedHashMap<ChunkPos, Map<BlockPos, BlockState>>();
        for (var entry : candidates.entrySet()) {
            BlockPos pos = entry.getKey();
            AABB cell = new AABB(pos);
            if (cell.intersects(owner.getBoundingBox().inflate(.15))
                    || protectedEntities.stream().anyMatch(entity -> cell.intersects(entity.getBoundingBox().inflate(.15)))) continue;
            byChunk.computeIfAbsent(new ChunkPos(pos), ignored -> new LinkedHashMap<>()).put(pos, entry.getValue());
        }
        Map<BlockPos, BlockState> placed = new LinkedHashMap<>();
        for (var chunk : byChunk.values()) {
            // Each persisted anchor shares a chunk with all its blocks, including after unload/reload.
            var terrain = TemporaryTerrainEntity.placeAir(level, owner, chunk, LIFETIME_TICKS);
            if (terrain != null) {
                placed.putAll(terrain.placedStates());
                terrain.enableContactDamage(owner, damage);
                terrain.enableWoodWithering();
            }
        }
        if(!placed.isEmpty())net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(level,null,center.x,center.y,center.z,96,
                new com.example.immortal_cultivation_mod.network.QiAuraPayloads.WoodBloom(level.dimension().location(),BlockPos.containing(center),new java.util.ArrayList<>(placed.keySet())));
        return Map.copyOf(placed);
    }

    public static boolean intersects(Map<BlockPos, BlockState> placed, AABB target) {
        for (BlockPos pos : placed.keySet()) if (new AABB(pos).intersects(target)) return true;
        return false;
    }

    private QiWoodSpikes() { }
}
