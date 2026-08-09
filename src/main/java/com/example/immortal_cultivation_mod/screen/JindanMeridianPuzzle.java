package com.example.immortal_cultivation_mod.screen;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Pure puzzle state for the 37-node Golden Core meridian formation. */
final class JindanMeridianPuzzle {
    static final int RADIUS = 3;
    static final int TILE_COUNT = 37;
    static final int DIRECTION_COUNT = 6;

    private static final int[][] DIRECTIONS = {
            {1, 0}, {0, 1}, {-1, 1}, {-1, 0}, {0, -1}, {1, -1}
    };

    private final List<Tile> tiles;
    private final Map<Long, Integer> indices;
    private final int[] rotations;
    private final List<Move> scrambleMoves = new ArrayList<>();
    private final int totalPathEnds;

    JindanMeridianPuzzle(long seed, boolean assisted) {
        Random random = new Random(seed);
        List<Coord> coordinates = buildCoordinates();
        indices = new HashMap<>();
        for (int i = 0; i < coordinates.size(); i++) {
            Coord coord = coordinates.get(i);
            indices.put(key(coord.q(), coord.r()), i);
        }

        int[] baseMasks = buildSolvedFormation(coordinates, random, assisted ? 5 : 8);
        boolean[] linked = chooseLinkedNodes(coordinates, random, assisted ? 3 : 5);
        List<Tile> builtTiles = new ArrayList<>(TILE_COUNT);
        int pathEnds = 0;
        for (int i = 0; i < coordinates.size(); i++) {
            Coord coord = coordinates.get(i);
            builtTiles.add(new Tile(coord.q(), coord.r(), baseMasks[i], linked[i]));
            pathEnds += Integer.bitCount(baseMasks[i]);
        }
        tiles = List.copyOf(builtTiles);
        rotations = new int[TILE_COUNT];
        totalPathEnds = pathEnds;

        scramble(random, assisted ? 44 : 62);
        if (!hasRecordedSolution()) {
            throw new IllegalStateException("Generated Golden Core formation has no recorded solution");
        }
    }

    List<Tile> tiles() {
        return tiles;
    }

    int rotation(int index) {
        return rotations[index];
    }

    int mask(int index, boolean solvedPreview) {
        Tile tile = tiles.get(index);
        return rotateMask(tile.baseMask(), solvedPreview ? 0 : rotations[index]);
    }

    int neighbor(int index, int direction) {
        Tile tile = tiles.get(index);
        int[] offset = DIRECTIONS[Math.floorMod(direction, DIRECTION_COUNT)];
        return indices.getOrDefault(key(tile.q() + offset[0], tile.r() + offset[1]), -1);
    }

    boolean hasPath(int index, int direction, boolean solvedPreview) {
        return (mask(index, solvedPreview) & (1 << Math.floorMod(direction, DIRECTION_COUNT))) != 0;
    }

    boolean pathMatches(int index, int direction, boolean solvedPreview) {
        if (!hasPath(index, direction, solvedPreview)) {
            return false;
        }
        int adjacent = neighbor(index, direction);
        return adjacent >= 0 && hasPath(adjacent, opposite(direction), solvedPreview);
    }

    void rotate(int index, int delta) {
        if (index <= 0 || index >= TILE_COUNT || delta == 0) {
            return;
        }
        applyMove(rotations, index, delta);
    }

    int leaks() {
        return leaks(rotations);
    }

    float integrity() {
        if (totalPathEnds == 0) {
            return 100.0F;
        }
        return 100.0F * (totalPathEnds - leaks()) / totalPathEnds;
    }

    boolean[] connectedToCore() {
        return connectedToCore(rotations);
    }

    boolean isSolved() {
        if (leaks() != 0) {
            return false;
        }
        boolean[] connected = connectedToCore();
        for (boolean value : connected) {
            if (!value) {
                return false;
            }
        }
        return true;
    }

    boolean hasRecordedSolution() {
        int[] proof = Arrays.copyOf(rotations, rotations.length);
        for (int i = scrambleMoves.size() - 1; i >= 0; i--) {
            Move move = scrambleMoves.get(i);
            applyMove(proof, move.index(), -move.delta());
        }
        return leaks(proof) == 0 && allConnected(proof);
    }

    private List<Coord> buildCoordinates() {
        List<Coord> result = new ArrayList<>(TILE_COUNT);
        for (int q = -RADIUS; q <= RADIUS; q++) {
            for (int r = -RADIUS; r <= RADIUS; r++) {
                if (hexDistance(q, r) <= RADIUS) {
                    result.add(new Coord(q, r));
                }
            }
        }
        result.sort(Comparator
                .comparingInt((Coord coord) -> hexDistance(coord.q(), coord.r()))
                .thenComparingDouble(coord -> Math.atan2(
                        Math.sqrt(3.0D) * (coord.r() + coord.q() * 0.5D),
                        1.5D * coord.q())));
        if (result.size() != TILE_COUNT || result.getFirst().q() != 0 || result.getFirst().r() != 0) {
            throw new IllegalStateException("Golden Core formation must contain 37 tiles with the core first");
        }
        return result;
    }

    private int[] buildSolvedFormation(List<Coord> coordinates, Random random, int loopCount) {
        int[] masks = new int[TILE_COUNT];

        // A radial spanning tree guarantees every outer meridian reaches the Golden Core.
        for (int index = 1; index < coordinates.size(); index++) {
            Coord coord = coordinates.get(index);
            int ring = hexDistance(coord.q(), coord.r());
            List<Integer> inwardDirections = new ArrayList<>(3);
            for (int direction = 0; direction < DIRECTION_COUNT; direction++) {
                int[] offset = DIRECTIONS[direction];
                if (hexDistance(coord.q() + offset[0], coord.r() + offset[1]) == ring - 1) {
                    inwardDirections.add(direction);
                }
            }
            int direction = inwardDirections.get(random.nextInt(inwardDirections.size()));
            int adjacent = indices.get(key(
                    coord.q() + DIRECTIONS[direction][0],
                    coord.r() + DIRECTIONS[direction][1]));
            connect(masks, index, adjacent, direction);
        }

        // A few closed loops make the formation richer without introducing unsolvable leaks.
        List<Edge> optionalEdges = new ArrayList<>();
        for (int index = 1; index < coordinates.size(); index++) {
            Coord coord = coordinates.get(index);
            int ring = hexDistance(coord.q(), coord.r());
            for (int direction = 0; direction < DIRECTION_COUNT; direction++) {
                int adjacent = neighbor(coord, direction);
                if (adjacent > index
                        && hexDistance(coordinates.get(adjacent).q(), coordinates.get(adjacent).r()) == ring
                        && (masks[index] & (1 << direction)) == 0) {
                    optionalEdges.add(new Edge(index, adjacent, direction));
                }
            }
        }
        Collections.shuffle(optionalEdges, random);
        int loops = Math.min(loopCount, optionalEdges.size());
        for (int i = 0; i < loops; i++) {
            Edge edge = optionalEdges.get(i);
            connect(masks, edge.from(), edge.to(), edge.direction());
        }
        return masks;
    }

    private boolean[] chooseLinkedNodes(List<Coord> coordinates, Random random, int linkedCount) {
        boolean[] linked = new boolean[TILE_COUNT];
        List<Integer> candidates = new ArrayList<>();
        for (int i = 1; i < coordinates.size(); i++) {
            int ring = hexDistance(coordinates.get(i).q(), coordinates.get(i).r());
            if (ring >= 2) {
                candidates.add(i);
            }
        }
        Collections.shuffle(candidates, random);
        int wanted = linkedCount;
        for (int candidate : candidates) {
            boolean touchesLinked = false;
            for (int direction = 0; direction < DIRECTION_COUNT; direction++) {
                int adjacent = neighbor(coordinates.get(candidate), direction);
                if (adjacent >= 0 && linked[adjacent]) {
                    touchesLinked = true;
                    break;
                }
            }
            if (!touchesLinked) {
                linked[candidate] = true;
                wanted--;
                if (wanted == 0) {
                    break;
                }
            }
        }
        return linked;
    }

    private void scramble(Random random, int moves) {
        List<Integer> movable = new ArrayList<>(TILE_COUNT - 1);
        for (int i = 1; i < TILE_COUNT; i++) {
            movable.add(i);
        }
        for (int i = 0; i < moves; i++) {
            int index = movable.get(random.nextInt(movable.size()));
            int delta = random.nextBoolean() ? 1 : -1;
            applyMove(rotations, index, delta);
            scrambleMoves.add(new Move(index, delta));
        }
        if (isSolved()) {
            int index = movable.stream().filter(i -> !tiles.get(i).linked()).findFirst().orElse(1);
            applyMove(rotations, index, 1);
            scrambleMoves.add(new Move(index, 1));
        }
    }

    private void applyMove(int[] target, int index, int delta) {
        target[index] = Math.floorMod(target[index] + delta, DIRECTION_COUNT);
        if (!tiles.get(index).linked()) {
            return;
        }
        for (int direction = 0; direction < DIRECTION_COUNT; direction++) {
            int adjacent = neighbor(index, direction);
            if (adjacent > 0) {
                target[adjacent] = Math.floorMod(target[adjacent] + delta, DIRECTION_COUNT);
            }
        }
    }

    private int leaks(int[] state) {
        int result = 0;
        for (int index = 0; index < TILE_COUNT; index++) {
            int mask = rotateMask(tiles.get(index).baseMask(), state[index]);
            for (int direction = 0; direction < DIRECTION_COUNT; direction++) {
                if ((mask & (1 << direction)) == 0) {
                    continue;
                }
                int adjacent = neighbor(index, direction);
                if (adjacent < 0) {
                    result++;
                    continue;
                }
                int adjacentMask = rotateMask(tiles.get(adjacent).baseMask(), state[adjacent]);
                if ((adjacentMask & (1 << opposite(direction))) == 0) {
                    result++;
                }
            }
        }
        return result;
    }

    private boolean[] connectedToCore(int[] state) {
        boolean[] visited = new boolean[TILE_COUNT];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        visited[0] = true;
        queue.add(0);
        while (!queue.isEmpty()) {
            int index = queue.removeFirst();
            int mask = rotateMask(tiles.get(index).baseMask(), state[index]);
            for (int direction = 0; direction < DIRECTION_COUNT; direction++) {
                if ((mask & (1 << direction)) == 0) {
                    continue;
                }
                int adjacent = neighbor(index, direction);
                if (adjacent < 0 || visited[adjacent]) {
                    continue;
                }
                int adjacentMask = rotateMask(tiles.get(adjacent).baseMask(), state[adjacent]);
                if ((adjacentMask & (1 << opposite(direction))) != 0) {
                    visited[adjacent] = true;
                    queue.addLast(adjacent);
                }
            }
        }
        return visited;
    }

    private boolean allConnected(int[] state) {
        for (boolean connected : connectedToCore(state)) {
            if (!connected) {
                return false;
            }
        }
        return true;
    }

    private static void connect(int[] masks, int from, int to, int direction) {
        masks[from] |= 1 << direction;
        masks[to] |= 1 << opposite(direction);
    }

    private static int rotateMask(int mask, int rotation) {
        int normalized = Math.floorMod(rotation, DIRECTION_COUNT);
        return ((mask << normalized) | (mask >>> (DIRECTION_COUNT - normalized))) & 0x3F;
    }

    private static int opposite(int direction) {
        return Math.floorMod(direction + 3, DIRECTION_COUNT);
    }

    private static int hexDistance(int q, int r) {
        return (Math.abs(q) + Math.abs(r) + Math.abs(q + r)) / 2;
    }

    private static long key(int q, int r) {
        return ((long) q << 32) ^ (r & 0xFFFFFFFFL);
    }

    private int neighbor(Coord coord, int direction) {
        int[] offset = DIRECTIONS[Math.floorMod(direction, DIRECTION_COUNT)];
        return indices.getOrDefault(key(coord.q() + offset[0], coord.r() + offset[1]), -1);
    }

    record Tile(int q, int r, int baseMask, boolean linked) {
    }

    private record Coord(int q, int r) {
    }

    private record Edge(int from, int to, int direction) {
    }

    private record Move(int index, int delta) {
    }
}
