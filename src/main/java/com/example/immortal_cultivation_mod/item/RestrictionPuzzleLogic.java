package com.example.immortal_cultivation_mod.item;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RestrictionPuzzleLogic {
    public static final int MIN_QI = 0;
    public static final int MAX_QI = 6;
    public static final int FLAG_LOCKED = 1;
    public static final int FLAG_UNSTABLE = 1 << 1;
    public static final int FLAG_HIDDEN = 1 << 2;

    public enum Difficulty {
        SIMPLE,
        LINKED,
        COMPLEX,
        EXTREME;

        public static Difficulty forLayer(int layer) {
            if (layer <= 9) {
                return SIMPLE;
            }
            if (layer <= 36) {
                return LINKED;
            }
            if (layer <= 72) {
                return COMPLEX;
            }
            return EXTREME;
        }
    }

    public record Link(int first, int second, int polarity) {
    }

    public record MoveOutcome(boolean accepted, boolean solved, boolean exhausted,
                              boolean instabilityShifted) {
        private static final MoveOutcome INVALID = new MoveOutcome(false, false, false, false);
    }

    public record HintMove(int node, int direction, MoveOutcome outcome) {
    }

    public static final class Puzzle {
        private final Difficulty difficulty;
        private final int firstLayer;
        private final int attemptedLayers;
        private final int[] values;
        private final int[] targets;
        private final int[] flags;
        private final List<Link> links;
        private final List<TrackedMove> moveHistory = new ArrayList<>();
        private final int moveLimit;
        private final int instabilityInterval;
        private int movesUsed;
        private int unstableCursor;

        private Puzzle(Difficulty difficulty, int firstLayer, int attemptedLayers,
                       int[] values, int[] targets, int[] flags, List<Link> links,
                       int moveLimit, int instabilityInterval) {
            this.difficulty = difficulty;
            this.firstLayer = firstLayer;
            this.attemptedLayers = attemptedLayers;
            this.values = values;
            this.targets = targets;
            this.flags = flags;
            this.links = List.copyOf(links);
            this.moveLimit = moveLimit;
            this.instabilityInterval = instabilityInterval;
        }

        public boolean canMove(int node, int direction) {
            if (node < 0 || node >= values.length || (direction != -1 && direction != 1)
                    || isFlagged(node, FLAG_LOCKED)) {
                return false;
            }
            int[] deltas = movementDeltas(node, direction);
            for (int i = 0; i < values.length; i++) {
                int next = values[i] + deltas[i];
                if (next < MIN_QI || next > MAX_QI) {
                    return false;
                }
            }
            return true;
        }

        public MoveOutcome move(int node, int direction) {
            if (!canMove(node, direction)) {
                return MoveOutcome.INVALID;
            }
            applyTrackedMove(node, direction);
            movesUsed++;
            boolean shifted = instabilityInterval > 0
                    && movesUsed % instabilityInterval == 0
                    && shiftUnstableNode();
            boolean solved = isSolved();
            boolean exhausted = !solved && moveLimit > 0 && movesUsed >= moveLimit;
            return new MoveOutcome(true, solved, exhausted, shifted);
        }

        public HintMove hintMove() {
            if (moveHistory.isEmpty() || isSolved()) {
                return null;
            }

            int lastIndex = moveHistory.size() - 1;
            TrackedMove previous = moveHistory.get(lastIndex);
            int direction = -previous.direction();
            if (!canMove(previous.node(), direction)) {
                return null;
            }

            applyMoveUnchecked(previous.node(), direction);
            moveHistory.remove(lastIndex);
            boolean solved = isSolved();
            return new HintMove(previous.node(), direction,
                    new MoveOutcome(true, solved, false, false));
        }

        public Difficulty difficulty() {
            return difficulty;
        }

        public int firstLayer() {
            return firstLayer;
        }

        public int attemptedLayers() {
            return attemptedLayers;
        }

        public int finalLayer() {
            return firstLayer + attemptedLayers - 1;
        }

        public int moveLimit() {
            return moveLimit;
        }

        public int movesUsed() {
            return movesUsed;
        }

        public int instabilityInterval() {
            return instabilityInterval;
        }

        public int size() {
            return values.length;
        }

        public int value(int node) {
            return values[node];
        }

        public int target(int node) {
            return targets[node];
        }

        public int flags(int node) {
            return flags[node];
        }

        public List<Link> links() {
            return links;
        }

        public List<Integer> valuesList() {
            return Arrays.stream(values).boxed().toList();
        }

        public List<Integer> targetsList() {
            return Arrays.stream(targets).boxed().toList();
        }

        public List<Integer> flagsList() {
            return Arrays.stream(flags).boxed().toList();
        }

        public double mismatchRatio() {
            int mismatch = 0;
            for (int i = 0; i < values.length; i++) {
                mismatch += Math.abs(values[i] - targets[i]);
            }
            return mismatch / (double) (values.length * MAX_QI);
        }

        public boolean isSolved() {
            return Arrays.equals(values, targets);
        }

        private boolean isFlagged(int node, int flag) {
            return (flags[node] & flag) != 0;
        }

        private int[] movementDeltas(int node, int direction) {
            int[] deltas = new int[values.length];
            deltas[node] = direction;
            for (Link link : links) {
                if (link.first == node) {
                    deltas[link.second] += direction * link.polarity;
                } else if (link.second == node) {
                    deltas[link.first] += direction * link.polarity;
                }
            }
            return deltas;
        }

        private void applyMoveUnchecked(int node, int direction) {
            int[] deltas = movementDeltas(node, direction);
            for (int i = 0; i < values.length; i++) {
                values[i] += deltas[i];
            }
        }

        private void applyTrackedMove(int node, int direction) {
            applyMoveUnchecked(node, direction);
            if (isSolved()) {
                moveHistory.clear();
                return;
            }
            for (int i = moveHistory.size() - 1; i >= 0; i--) {
                if (Arrays.equals(values, moveHistory.get(i).stateAfter())) {
                    while (moveHistory.size() > i + 1) {
                        moveHistory.removeLast();
                    }
                    return;
                }
            }
            moveHistory.add(new TrackedMove(node, direction, Arrays.copyOf(values, values.length)));
        }

        private boolean shiftUnstableNode() {
            List<Integer> unstable = new ArrayList<>();
            for (int i = 0; i < flags.length; i++) {
                if (isFlagged(i, FLAG_UNSTABLE)) {
                    unstable.add(i);
                }
            }
            if (unstable.isEmpty()) {
                return false;
            }
            for (int attempt = 0; attempt < unstable.size(); attempt++) {
                int cursor = unstableCursor + attempt;
                int node = unstable.get(Math.floorMod(cursor, unstable.size()));
                int direction = ((cursor * 31 + firstLayer) & 1) == 0 ? 1 : -1;
                if (!canMove(node, direction)) {
                    direction = -direction;
                }
                if (canMove(node, direction)) {
                    applyTrackedMove(node, direction);
                    unstableCursor = cursor + 1;
                    return true;
                }
            }
            unstableCursor++;
            return false;
        }
    }

    private RestrictionPuzzleLogic() {
    }

    public static Puzzle create(RandomSource random, int existingLayers, int attemptedLayers) {
        return create(random, existingLayers, attemptedLayers, false);
    }

    public static Puzzle create(
            RandomSource random, int existingLayers, int attemptedLayers, boolean assisted) {
        int safeExisting = Math.max(0, existingLayers);
        int safeAttempted = Math.max(1, attemptedLayers);
        int firstLayer = safeExisting + 1;
        int finalLayer = safeExisting + safeAttempted;
        int complexityLayer = assisted ? assistedLayer(finalLayer) : finalLayer;
        int complexityAttempted = assisted ? Math.max(1, (safeAttempted + 1) / 2) : safeAttempted;
        Difficulty difficulty = Difficulty.forLayer(complexityLayer);

        int nodeCount = nodeCount(complexityLayer, complexityAttempted);
        int linkCount = linkCount(complexityLayer, nodeCount);
        int lockedCount = lockedCount(complexityLayer, nodeCount);
        int unstableCount = unstableCount(complexityLayer, nodeCount);
        int hiddenCount = hiddenCount(complexityLayer, nodeCount);

        int[] targets = new int[nodeCount];
        int[] flags = new int[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            targets[i] = 2 + random.nextInt(3);
        }

        List<Integer> order = shuffledIndices(random, nodeCount);
        Set<Integer> locked = new HashSet<>();
        for (int i = 0; i < lockedCount && i < order.size(); i++) {
            int node = order.get(i);
            flags[node] |= FLAG_LOCKED;
            locked.add(node);
        }

        List<Link> links = generateLinks(random, nodeCount, linkCount, locked);
        int cursor = lockedCount;
        int markedUnstable = 0;
        while (markedUnstable < unstableCount && cursor < order.size()) {
            int node = order.get(cursor++);
            if (!locked.contains(node)) {
                flags[node] |= FLAG_UNSTABLE;
                markedUnstable++;
            }
        }
        int markedHidden = 0;
        for (int i = order.size() - 1; i >= 0 && markedHidden < hiddenCount; i--) {
            int node = order.get(i);
            if ((flags[node] & FLAG_HIDDEN) == 0) {
                flags[node] |= FLAG_HIDDEN;
                markedHidden++;
            }
        }

        int scrambleMoves = scrambleMoves(complexityLayer, complexityAttempted);
        int moveLimit = moveLimit(complexityLayer, scrambleMoves, nodeCount);
        int instabilityInterval = instabilityInterval(complexityLayer, unstableCount);
        int[] values = Arrays.copyOf(targets, targets.length);
        Puzzle puzzle = new Puzzle(difficulty, firstLayer, safeAttempted, values, targets, flags,
                links, moveLimit, instabilityInterval);

        List<Integer> movable = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            if ((flags[i] & FLAG_LOCKED) == 0) {
                movable.add(i);
            }
        }
        int applied = 0;
        int attempts = 0;
        List<ScrambleMove> scrambleHistory = new ArrayList<>();
        while (applied < scrambleMoves && attempts++ < scrambleMoves * 30) {
            int node = movable.get(random.nextInt(movable.size()));
            int direction = random.nextBoolean() ? 1 : -1;
            if (puzzle.canMove(node, direction)) {
                puzzle.applyTrackedMove(node, direction);
                scrambleHistory.add(new ScrambleMove(node, direction));
                applied++;
            }
        }
        if (puzzle.isSolved()) {
            for (int node : movable) {
                if (puzzle.canMove(node, 1)) {
                    puzzle.applyTrackedMove(node, 1);
                    scrambleHistory.add(new ScrambleMove(node, 1));
                    break;
                }
                if (puzzle.canMove(node, -1)) {
                    puzzle.applyTrackedMove(node, -1);
                    scrambleHistory.add(new ScrambleMove(node, -1));
                    break;
                }
            }
        }
        if (!verifyScramble(puzzle, scrambleHistory)) {
            System.arraycopy(targets, 0, values, 0, targets.length);
            puzzle.moveHistory.clear();
            for (int node : movable) {
                if (puzzle.canMove(node, 1)) {
                    puzzle.applyTrackedMove(node, 1);
                    break;
                }
                if (puzzle.canMove(node, -1)) {
                    puzzle.applyTrackedMove(node, -1);
                    break;
                }
            }
        }
        return puzzle;
    }

    private static int assistedLayer(int finalLayer) {
        if (finalLayer <= 9) {
            return Math.max(1, finalLayer - 4);
        }
        if (finalLayer <= 36) {
            return 9;
        }
        if (finalLayer <= 72) {
            return 36;
        }
        return 72;
    }

    private static boolean verifyScramble(Puzzle source, List<ScrambleMove> history) {
        Puzzle verifier = new Puzzle(
                source.difficulty,
                source.firstLayer,
                source.attemptedLayers,
                Arrays.copyOf(source.values, source.values.length),
                Arrays.copyOf(source.targets, source.targets.length),
                Arrays.copyOf(source.flags, source.flags.length),
                source.links,
                0,
                0);
        for (int index = history.size() - 1; index >= 0; index--) {
            ScrambleMove move = history.get(index);
            int inverse = -move.direction;
            if (!verifier.canMove(move.node, inverse)) {
                return false;
            }
            verifier.applyMoveUnchecked(move.node, inverse);
        }
        return verifier.isSolved();
    }

    private static int nodeCount(int finalLayer, int attemptedLayers) {
        int base = 3 + Math.min(7, Math.max(0, finalLayer - 1) / 10);
        int batchBonus = Math.min(2, Math.max(0, attemptedLayers - 1) / 24);
        return Math.min(12, base + batchBonus);
    }

    private static int linkCount(int finalLayer, int nodeCount) {
        if (finalLayer < 10) {
            return 0;
        }
        int links = 1 + (finalLayer - 10) / 10;
        return Math.min(nodeCount + 1, links);
    }

    private static int lockedCount(int finalLayer, int nodeCount) {
        if (finalLayer < 46) {
            return 0;
        }
        return Math.min(Math.max(1, nodeCount / 3), 1 + (finalLayer - 46) / 18);
    }

    private static int unstableCount(int finalLayer, int nodeCount) {
        if (finalLayer < 55) {
            return 0;
        }
        return Math.min(Math.max(1, nodeCount / 3), 1 + (finalLayer - 55) / 24);
    }

    private static int hiddenCount(int finalLayer, int nodeCount) {
        if (finalLayer <= 72) {
            return 0;
        }
        return Math.min(Math.max(1, nodeCount / 3), 1 + (finalLayer - 73) / 36);
    }

    private static int scrambleMoves(int finalLayer, int attemptedLayers) {
        int layerMoves = 2 + Math.min(22, Math.max(0, finalLayer - 1) / 5);
        int batchMoves = Math.min(10, Math.max(0, attemptedLayers - 1) / 8);
        return layerMoves + batchMoves;
    }

    private static int moveLimit(int finalLayer, int scrambleMoves, int nodeCount) {
        if (finalLayer <= 72) {
            return 0;
        }
        int gradualPressure = Math.min(scrambleMoves, Math.max(0, finalLayer - 73) / 6);
        return Math.max(42, scrambleMoves * 4 + nodeCount * 2 - gradualPressure);
    }

    private static int instabilityInterval(int finalLayer, int unstableCount) {
        if (unstableCount == 0) {
            return 0;
        }
        return Math.max(4, 10 - Math.max(0, finalLayer - 55) / 12);
    }

    private static List<Link> generateLinks(RandomSource random, int nodeCount, int wanted,
                                             Set<Integer> locked) {
        List<Link> links = new ArrayList<>();
        Set<Long> used = new HashSet<>();
        List<Integer> unlocked = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            if (!locked.contains(i)) {
                unlocked.add(i);
            }
        }
        for (int lockedNode : locked) {
            if (unlocked.isEmpty()) {
                break;
            }
            int other = unlocked.get(random.nextInt(unlocked.size()));
            addLink(links, used, lockedNode, other, random.nextBoolean() ? 1 : -1);
        }
        int attempts = 0;
        while (links.size() < wanted && attempts++ < wanted * 30 + 30) {
            int first = random.nextInt(nodeCount);
            int second = random.nextInt(nodeCount);
            if (first == second || (locked.contains(first) && locked.contains(second))) {
                continue;
            }
            addLink(links, used, first, second, random.nextBoolean() ? 1 : -1);
        }
        return links;
    }

    private static void addLink(List<Link> links, Set<Long> used, int first, int second, int polarity) {
        int low = Math.min(first, second);
        int high = Math.max(first, second);
        long key = ((long) low << 32) | (high & 0xFFFFFFFFL);
        if (used.add(key)) {
            links.add(new Link(low, high, polarity < 0 ? -1 : 1));
        }
    }

    private static List<Integer> shuffledIndices(RandomSource random, int size) {
        List<Integer> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            values.add(i);
        }
        for (int i = values.size() - 1; i > 0; i--) {
            int swap = random.nextInt(i + 1);
            int value = values.get(i);
            values.set(i, values.get(swap));
            values.set(swap, value);
        }
        return values;
    }

    private record ScrambleMove(int node, int direction) {
    }

    private record TrackedMove(int node, int direction, int[] stateAfter) {
    }
}
