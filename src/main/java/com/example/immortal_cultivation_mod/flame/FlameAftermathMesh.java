package com.example.immortal_cultivation_mod.flame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Keeps every boundary face; adjacent coplanar cells share a cached tint quad. */
public final class FlameAftermathMesh {
    public static final int TILE_SIZE = 4;
    public static final int MAX_FACES = 262144;
    private final Map<Key, Tile> tiles = new HashMap<>();
    private int faces;

    private record Key(Direction face, int plane, int u, int v) { }
    public record Quad(int u, int v, int width, int height) { }

    public static final class Tile {
        private final Key key;
        private final BlockPos origin;
        private int cells;
        private boolean dirty = true;
        private List<Quad> quads = List.of();

        private Tile(Key key) {
            this.key = key;
            origin = FlameAftermathMesh.position(key.face, key.plane, key.u * TILE_SIZE, key.v * TILE_SIZE);
        }

        public BlockPos origin() { return origin; }
        public Direction direction() { return key.face; }
        public boolean empty() { return cells == 0; }
        public int index(BlockPos pos) {
            return Math.floorMod(u(key.face, pos), TILE_SIZE)
                    + Math.floorMod(v(key.face, pos), TILE_SIZE) * TILE_SIZE;
        }
        public BlockPos position(int index) {
            return FlameAftermathMesh.position(key.face, key.plane,
                    key.u * TILE_SIZE + index % TILE_SIZE, key.v * TILE_SIZE + index / TILE_SIZE);
        }
        public void remove(int index) {
            cells &= ~(1 << index);
            dirty = true;
        }

        public List<Quad> quads() {
            if (!dirty) return quads;
            var result = new ArrayList<Quad>();
            int remaining = cells;
            while (remaining != 0) {
                int index = Integer.numberOfTrailingZeros(remaining);
                int u = index % TILE_SIZE, v = index / TILE_SIZE;
                int width = 1;
                while (u + width < TILE_SIZE && (remaining & (1 << (index + width))) != 0) width++;
                int row = ((1 << width) - 1) << u;
                int height = 1;
                while (v + height < TILE_SIZE
                        && ((remaining >>> ((v + height) * TILE_SIZE)) & row) == row) height++;
                for (int y = v; y < v + height; y++) remaining &= ~(row << (y * TILE_SIZE));
                result.add(new Quad(u, v, width, height));
            }
            quads = List.copyOf(result);
            dirty = false;
            return quads;
        }
    }

    public Tile add(FlameAftermathFaces.Face face) {
        Direction direction = face.direction();
        BlockPos pos = face.pos();
        int plane = switch (direction.getAxis()) {
            case X -> pos.getX(); case Y -> pos.getY(); case Z -> pos.getZ();
        };
        Key key = new Key(direction, plane, Math.floorDiv(u(direction, pos), TILE_SIZE),
                Math.floorDiv(v(direction, pos), TILE_SIZE));
        Tile tile = tiles.computeIfAbsent(key, Tile::new);
        int bit = 1 << tile.index(pos);
        if ((tile.cells & bit) == 0) {
            tile.cells |= bit;
            tile.dirty = true;
            faces++;
        }
        return tile;
    }

    public int faceCount() { return faces; }

    private static int u(Direction face, BlockPos pos) {
        return face.getAxis() == Direction.Axis.X ? pos.getZ() : pos.getX();
    }
    private static int v(Direction face, BlockPos pos) {
        return face.getAxis() == Direction.Axis.Y ? pos.getZ() : pos.getY();
    }
    private static BlockPos position(Direction face, int plane, int u, int v) {
        return switch (face.getAxis()) {
            case X -> new BlockPos(plane, v, u);
            case Y -> new BlockPos(u, plane, v);
            case Z -> new BlockPos(u, v, plane);
        };
    }
}
