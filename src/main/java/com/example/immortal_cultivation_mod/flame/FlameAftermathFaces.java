package com.example.immortal_cultivation_mod.flame;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/** Enumerates the crater boundary, rather than scanning every block in a large blast volume. */
public final class FlameAftermathFaces implements Iterator<FlameAftermathFaces.Face> {
    public record Face(BlockPos pos, Direction direction) {
        public BlockPos exposedNeighbor() { return pos.relative(direction); }
    }

    private final Vec3 center;
    private final double radius;
    private final long seed;
    private final BlockPos origin;
    private final int extent, width;
    private final ArrayDeque<Face> pending = new ArrayDeque<>();
    private int column;

    public FlameAftermathFaces(Vec3 center, double radius, long seed) {
        this.center = center;
        this.radius = radius;
        this.seed = seed;
        origin = BlockPos.containing(center);
        extent = (int)Math.ceil(radius);
        width = extent * 2 + 1;
    }

    @Override public boolean hasNext() {
        while (pending.isEmpty() && column < width * width) {
            int index = column++;
            BlockPos pos = origin.offset(index % width - extent, 0, index / width - extent);
            double reach = FlameCraterShape.reach(center, pos, radius, seed);
            if (!Double.isFinite(reach)) continue;
            int bottom = bottom(reach), top = top(reach);
            if (bottom > top) continue;
            pending.add(new Face(pos.atY(bottom - 1), Direction.UP));
            pending.add(new Face(pos.atY(top + 1), Direction.DOWN));
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos neighbor = pos.relative(direction);
                double adjacentReach = FlameCraterShape.reach(center, neighbor, radius, seed);
                // A wall is the surviving part of the neighboring column beside removed blocks.
                if (!Double.isFinite(adjacentReach)) addWall(neighbor, direction, bottom, top);
                else {
                    addWall(neighbor, direction, bottom, Math.min(top, bottom(adjacentReach) - 1));
                    addWall(neighbor, direction, Math.max(bottom, top(adjacentReach) + 1), top);
                }
            }
        }
        return !pending.isEmpty();
    }

    private void addWall(BlockPos pos, Direction fromCrater, int bottom, int top) {
        for (int y = bottom; y <= top; y++)
            pending.add(new Face(pos.atY(y), fromCrater.getOpposite()));
    }

    private int bottom(double reach) { return (int)Math.ceil(center.y - reach - .5); }
    private int top(double reach) { return (int)Math.floor(center.y + reach - .5); }

    @Override public Face next() {
        if (!hasNext()) throw new NoSuchElementException();
        return pending.removeFirst();
    }
}
