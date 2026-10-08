package com.example.immortal_cultivation_mod.flame;

import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;

import com.example.immortal_cultivation_mod.spell.SpellDestruction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/** Completes the entire loaded crater during impact; small event batches only bound temporary memory. */
public final class FlameTerrainDestruction {
    private static final int BATCH_SIZE = 128;
    private FlameTerrainDestruction() { }

    static final class Columns {
        private final int count;
        private int visited, x, z, dx, dz = -1;

        Columns(double radius) {
            int width = (int) Math.ceil(radius) * 2 + 1;
            count = width * width;
        }

        boolean hasNext() { return visited < count; }

        BlockPos next(BlockPos origin) {
            BlockPos result = origin.offset(x, 0, z);
            if (x == z || x < 0 && x == -z || x > 0 && x == 1 - z) {
                int previous = dx; dx = -dz; dz = previous;
            }
            x += dx; z += dz; visited++;
            return result;
        }
    }

    private record Candidate(BlockPos pos, BlockState expected) { }

    static void destroy(ServerLevel level, ServerPlayer owner, FlameShotEntity shot, Vec3 center, float radius) {
        if (!BlockBreakingSettings.enabled(owner,
                BlockBreakingSettings.Category.FLAME_VESSELS)) return;
        if (!Float.isFinite(radius) || radius <= 0 || radius > 60 || !owner.isAlive() || owner.isRemoved()
                || owner.isSpectator() || owner.level() != level || !owner.getAbilities().mayBuild) return;
        var explosion = new Explosion(level, shot, level.damageSources().source(DamageTypes.FIREBALL, shot, owner),
                null, center.x, center.y, center.z, radius, false, Explosion.BlockInteraction.DESTROY,
                ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER, SoundEvents.GENERIC_EXPLODE);
        var columns = new Columns(radius);
        var batch = new ArrayList<Candidate>(BATCH_SIZE);
        var lining = new ArrayList<Candidate>();
        BlockPos origin = BlockPos.containing(center);
        double radiusSquared = radius * radius;
        while (columns.hasNext()) {
            BlockPos column = columns.next(origin);
            double x = column.getX() + .5 - center.x, z = column.getZ() + .5 - center.z;
            double remaining = radiusSquared - x * x - z * z;
            if (remaining < 0 || !level.hasChunkAt(column) || !level.getWorldBorder().isWithinBounds(column)) continue;
            double reach = Math.sqrt(remaining);
            int floor = Math.max(level.getMinBuildHeight(), (int) Math.ceil(center.y - reach - .5));
            int top = Math.min(level.getMaxBuildHeight() - 1, (int) Math.floor(center.y + reach - .5));
            // WORLD_SURFACE includes liquids, unlike ocean-floor heightmaps.
            top = Math.min(top, level.getHeight(Heightmap.Types.WORLD_SURFACE, column.getX(), column.getZ()) - 1);
            // Keep the crater empty; only recolor its surviving solid floor, one block below the cut.
            if (floor > level.getMinBuildHeight()) {
                BlockPos ground = new BlockPos(column.getX(), floor - 1, column.getZ());
                BlockState surface = level.getBlockState(ground);
                if (!surface.isAir() && surface.getFluidState().isEmpty() && !surface.hasBlockEntity()
                        && surface.getDestroySpeed(level, ground) >= 0) {
                    lining.add(new Candidate(ground, surface));
                }
            }
            for (int y = top; y >= floor; y--) {
                BlockPos pos = new BlockPos(column.getX(), y, column.getZ());
                BlockState state = level.getBlockState(pos);
                if (!state.isAir() && !state.hasBlockEntity() && state.getDestroySpeed(level, pos) >= 0) {
                    batch.add(new Candidate(pos, state));
                    if (batch.size() == BATCH_SIZE) eraseBatch(level, owner, explosion, batch);
                }
            }
        }
        eraseBatch(level, owner, explosion, batch);
        for (int start = 0; start < lining.size(); start += BATCH_SIZE) {
            transformFloor(level, owner, explosion, shot.kind(),
                    lining.subList(start, Math.min(lining.size(), start + BATCH_SIZE)));
        }
    }

    /** Stable mixed palettes avoid a flat sheet of identical blocks or valuable ore generation. */
    static BlockState aftermath(FlameKind kind, BlockPos pos) {
        int pattern = Math.floorMod(pos.getX() * 73428767 ^ pos.getZ() * 912931 ^ pos.getY() * 193, 16);
        return (switch (kind) {
            case CRIMSON -> pattern == 0 ? Blocks.LAVA : pattern < 10 ? Blocks.MAGMA_BLOCK : Blocks.BLACKSTONE;
            case EARTH -> pattern < 2 ? Blocks.LAVA : pattern < 8 ? Blocks.MAGMA_BLOCK : Blocks.BASALT;
            case WOOD -> pattern < 10 ? Blocks.MOSS_BLOCK : Blocks.ROOTED_DIRT;
            case SOLAR -> pattern < 10 ? Blocks.CALCITE : Blocks.YELLOW_TERRACOTTA;
            case NETHER -> pattern < 10 ? Blocks.SOUL_SOIL : Blocks.CRYING_OBSIDIAN;
            case THUNDER -> pattern < 10 ? Blocks.OBSIDIAN : Blocks.TINTED_GLASS;
            case DRAGON -> pattern < 2 ? Blocks.LAVA : pattern < 11 ? Blocks.MAGMA_BLOCK : Blocks.POLISHED_BASALT;
            case HEAVENLY -> pattern < 5 ? Blocks.AMETHYST_BLOCK : Blocks.CALCITE;
            default -> Blocks.BLACKSTONE;
        }).defaultBlockState();
    }

    private static void transformFloor(ServerLevel level, ServerPlayer owner, Explosion explosion,
            FlameKind kind, List<Candidate> batch) {
        var affected = explosion.getToBlow();
        affected.clear();
        batch.forEach(candidate -> affected.add(candidate.pos));
        NeoForge.EVENT_BUS.post(new ExplosionEvent.Detonate(level, explosion, new ArrayList<>()));
        var permitted = new HashSet<>(affected);
        for (var candidate : batch) {
            if (!permitted.contains(candidate.pos) || !level.hasChunkAt(candidate.pos)
                    || !level.getBlockState(candidate.pos.above()).isAir()
                    || level.getBlockState(candidate.pos) != candidate.expected
                    || !level.mayInteract(owner, candidate.pos)) continue;
            BlockBreakingSettings.runWithSource(BlockBreakingSettings.Category.FLAME_VESSELS, owner, () -> {
                // Use the same break-event and claim checks as the crater; a denied floor stays intact.
                if (SpellDestruction.eraseImpactBlockNow(level, candidate.pos, owner)
                        && level.getBlockState(candidate.pos).isAir()) {
                    level.setBlock(candidate.pos, aftermath(kind, candidate.pos), 3);
                }
            });
        }
        affected.clear();
    }

    private static void eraseBatch(ServerLevel level, ServerPlayer owner, Explosion explosion, List<Candidate> batch) {
        if (batch.isEmpty()) return;
        var affected = explosion.getToBlow();
        affected.clear();
        batch.forEach(candidate -> affected.add(candidate.pos));
        NeoForge.EVENT_BUS.post(new ExplosionEvent.Detonate(level, explosion, new ArrayList<>()));
        var permitted = new HashSet<>(affected);
        for (var candidate : batch) {
            if (permitted.contains(candidate.pos) && level.hasChunkAt(candidate.pos)
                    && level.mayInteract(owner, candidate.pos) && level.getBlockState(candidate.pos) == candidate.expected)
                BlockBreakingSettings.runWithSource(
                        BlockBreakingSettings.Category.FLAME_VESSELS, owner,
                        () -> SpellDestruction.eraseImpactBlockNow(level, candidate.pos, owner));
        }
        affected.clear();
        batch.clear();
    }
}
