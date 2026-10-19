package com.example.immortal_cultivation_mod.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;

/** Reuse absolute flow cells across frames and retain overlapping cells while moving. */
final class AtmosphericQiCells {
    static final float HORIZONTAL_CELL = 4.0F / 2.9240177F;
    static final float VERTICAL_CELL = 3.0F / 2.9240177F;
    private final Map<GridCell, Cell> cells = new HashMap<>();
    private Bounds bounds;
    private BlockGetter world;
    private List<Cell> samples = List.of();

    List<Cell> samples(BlockGetter world, Vec3 center, float radius, float verticalRadius) {
        if (this.world != world) {
            clear();
            this.world = world;
        }
        Bounds next = new Bounds(Mth.floor((center.x - radius) / HORIZONTAL_CELL),
                Mth.floor((center.x + radius) / HORIZONTAL_CELL),
                Mth.floor((center.y - verticalRadius) / VERTICAL_CELL),
                Mth.floor((center.y + verticalRadius) / VERTICAL_CELL),
                Mth.floor((center.z - radius) / HORIZONTAL_CELL),
                Mth.floor((center.z + radius) / HORIZONTAL_CELL));
        if (next.equals(bounds)) return samples;
        cells.keySet().removeIf(key -> !next.contains(key));
        List<Cell> updated = new ArrayList<>();
        for (int x = next.minX; x <= next.maxX; x++) {
            for (int y = next.minY; y <= next.maxY; y++) {
                for (int z = next.minZ; z <= next.maxZ; z++) {
                    updated.add(cells.computeIfAbsent(new GridCell(x, y, z), Cell::new));
                }
            }
        }
        bounds = next;
        samples = List.copyOf(updated);
        return samples;
    }

    void clear() {
        cells.clear();
        samples = List.of();
        bounds = null;
        world = null;
    }

    private record GridCell(int x, int y, int z) {}
    private record Bounds(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        boolean contains(GridCell key) {
            return key.x >= minX && key.x <= maxX && key.y >= minY && key.y <= maxY
                    && key.z >= minZ && key.z <= maxZ;
        }
    }

    static final class Cell {
        final int seed;
        final Vec3 origin;
        final float selectionNoise;
        final float phaseNoise;
        final float lengthNoise;
        final float curveNoise;
        private final BlockPos block;
        private int collisionTick = Integer.MIN_VALUE;
        private boolean open;

        Cell(GridCell key) {
            seed = SpiritualSenseClient.mixVeinSeed(key.x * 0x632BE5AB
                    ^ key.y * 0x85157AF5 ^ key.z * 0x9E3779B9);
            origin = new Vec3((key.x + 0.18F + SpiritualSenseClient.veinNoise(seed, 0, 1) * 0.64F) * HORIZONTAL_CELL,
                    (key.y + 0.20F + SpiritualSenseClient.veinNoise(seed, 0, 2) * 0.60F) * VERTICAL_CELL,
                    (key.z + 0.18F + SpiritualSenseClient.veinNoise(seed, 0, 3) * 0.64F) * HORIZONTAL_CELL);
            selectionNoise = SpiritualSenseClient.veinNoise(seed, 0, 0);
            phaseNoise = SpiritualSenseClient.veinNoise(seed, 0, 4);
            lengthNoise = SpiritualSenseClient.veinNoise(seed, 0, 5);
            curveNoise = SpiritualSenseClient.veinNoise(seed, 0, 6);
            block = BlockPos.containing(origin);
        }

        boolean isOpen(BlockGetter level, int tick) {
            if (collisionTick != Integer.MIN_VALUE && tick >= collisionTick && tick - collisionTick < 4) return open;
            var state = level.getBlockState(block);
            open = state.isAir() || state.getCollisionShape(level, block).isEmpty();
            collisionTick = tick;
            return open;
        }
    }
}
