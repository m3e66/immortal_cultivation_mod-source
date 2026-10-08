package com.example.immortal_cultivation_mod.spell;

import java.util.Iterator;
import java.util.NoSuchElementException;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

/** Visits complete columns across the impact footprint, without a growing radial front. */
final class SpellFractureTraversal implements Iterator<BlockPos> {
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    private final int minX, minY, minZ, maxY, width, columns, stride;
    private int column, visited, y;

    SpellFractureTraversal(AABB bounds, long seed) {
        BlockPos center = BlockPos.containing(bounds.getCenter());
        minX = Math.max(center.getX() - 64, (int)Math.ceil(bounds.minX - .5));
        minY = Math.max(center.getY() - 128, (int)Math.ceil(bounds.minY - .5));
        minZ = Math.max(center.getZ() - 64, (int)Math.ceil(bounds.minZ - .5));
        int maxX = Math.min(center.getX() + 64, (int)Math.ceil(bounds.maxX - .5) - 1);
        maxY = Math.min(center.getY() + 128, (int)Math.ceil(bounds.maxY - .5) - 1);
        int maxZ = Math.min(center.getZ() + 64, (int)Math.ceil(bounds.maxZ - .5) - 1);
        width = Math.max(0, maxX - minX + 1);
        columns = maxY < minY ? 0 : width * Math.max(0, maxZ - minZ + 1);
        int step = Math.max(1, (int)(columns * .61803398875));
        while (gcd(step, Math.max(1, columns)) != 1) step++;
        stride = step;
        column = (int)Math.floorMod(seed, Math.max(1, columns));
        y = maxY;
    }

    private static int gcd(int a, int b) {
        while (b != 0) { int remainder = a % b; a = b; b = remainder; }
        return a;
    }

    @Override public boolean hasNext() { return visited < columns; }

    @Override public BlockPos next() {
        if (!hasNext()) throw new NoSuchElementException();
        cursor.set(minX + column % width, y, minZ + column / width);
        if (--y < minY) {
            y = maxY;
            visited++;
            column = (column + stride) % columns;
        }
        return cursor;
    }
}
