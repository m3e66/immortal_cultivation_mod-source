package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.minecraft.util.Mth;

/** Reusable, deterministic definition for a Lingqi Trajectory learning puzzle. */
public record LingqiTrajectoryConfig(
        int width,
        int height,
        int startIndex,
        int finalCoreIndex,
        List<Node> nodes,
        List<Integer> requiredNodes,
        List<Integer> coreNodes,
        List<String> requiredElements,
        int idealSteps,
        int maximumSteps,
        int startingStability,
        int startingStrength,
        int minimumStrength,
        int maximumStrength,
        boolean strictOrder,
        String realm,
        List<DirectedEdge> directionalEdges,
        Set<Edge> collapsingEdges,
        List<TimedRequirement> timedRequirements,
        List<MovingHazard> movingHazards,
        List<LinkedPair> linkedPairs,
        List<SwitchGate> switchGates,
        BranchDefinition branch
) {
    private static final int BASE_LEARNING_POINTS = 5_000;
    private static final int MAX_GENERATION_ATTEMPTS = 24;
    private static final long RETRY_SEED_STEP = 0x9E3779B97F4A7C15L;
    private static final Map<String, Template> SPELL_TEMPLATES = Map.of(
            ModSpells.FIREBALL, new Template(5, 10, 13, 1),
            ModSpells.YINLEI_JUE, new Template(5, 12, 15, 1),
            ModSpells.WULEI_ZHENGFA, new Template(6, 22, 27, 5)
    );

    public LingqiTrajectoryConfig {
        nodes = List.copyOf(nodes);
        requiredNodes = List.copyOf(requiredNodes);
        coreNodes = List.copyOf(coreNodes);
        requiredElements = List.copyOf(requiredElements);
        directionalEdges = List.copyOf(directionalEdges);
        collapsingEdges = Set.copyOf(collapsingEdges);
        timedRequirements = List.copyOf(timedRequirements);
        movingHazards = List.copyOf(movingHazards);
        linkedPairs = List.copyOf(linkedPairs);
        switchGates = List.copyOf(switchGates);
    }

    public Node node(int index) {
        return nodes.get(index);
    }

    public int column(int index) {
        return index % width;
    }

    public int row(int index) {
        return index / width;
    }

    public boolean adjacent(int first, int second) {
        return Math.abs(column(first) - column(second)) + Math.abs(row(first) - row(second)) == 1;
    }

    public Edge edge(int first, int second) {
        return new Edge(first, second);
    }

    public DirectedEdge directionalRule(int first, int second) {
        Edge edge = edge(first, second);
        for (DirectedEdge rule : directionalEdges) {
            if (rule.edge().equals(edge)) {
                return rule;
            }
        }
        return null;
    }

    public static LingqiTrajectoryConfig create(String learningType, String targetId,
                                                 int requiredPoints, float difficulty,
                                                 float talent, boolean assisted, String playerLevel, long seed) {
        return generate(learningType, targetId, requiredPoints, difficulty, talent, assisted, playerLevel, seed).config();
    }

    static GeneratedPuzzle generate(String learningType, String targetId,
                                    int requiredPoints, float difficulty,
                                    float talent, boolean assisted, String playerLevel, long seed) {
        GeneratedPuzzle firstCandidate = null;
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            GeneratedPuzzle candidate = createCandidate(
                    learningType, targetId, requiredPoints, difficulty, talent, assisted, playerLevel,
                    seed + RETRY_SEED_STEP * attempt);
            if (firstCandidate == null) {
                firstCandidate = candidate;
            }
            if (hasGuaranteedRoute(candidate.config(), candidate.route())) {
                return candidate;
            }
        }
        return new GeneratedPuzzle(guaranteedFallback(firstCandidate.config(), firstCandidate.route()), firstCandidate.route());
    }

    private static GeneratedPuzzle createCandidate(String learningType, String targetId,
                                                    int requiredPoints, float difficulty,
                                                    float talent, boolean assisted, String playerLevel, long seed) {
        String safeType = learningType == null ? "" : learningType;
        String safeTarget = targetId == null ? "" : targetId;
        Target target = resolveTarget(safeType, safeTarget);
        boolean spellLearning = "spell".equals(safeType);
        boolean starterMethod = "method".equals(safeType) && isStarterMethod(safeTarget);
        int realmRank = starterMethod ? 1 : learningPuzzleRealmRank(playerLevel, target.level());
        String puzzleRealm = realmName(realmRank);
        int pointDifficulty = pointDifficulty(requiredPoints, difficulty, talent);
        int puzzleDifficulty = starterMethod ? -1 : Math.max(
                -1, earlyRealmDifficulty(realmRank, pointDifficulty) - (assisted ? 1 : 0));
        // Keep authored layouts at their own tier; a lowered realm must use the lower-tier rules.
        Template template = spellLearning
                && realmRank == Mth.clamp(CultivationLevels.getRealmIndex(target.level()), 1, 9)
                ? SPELL_TEMPLATES.get(safeTarget) : null;

        int difficultyRouteBonus = Math.max(0, puzzleDifficulty) * 2;
        int difficultySizeBonus = puzzleDifficulty >= 3 ? 1 : 0;
        int baseSize = template == null ? 4 + realmRank : template.size();
        int size = Mth.clamp(baseSize + difficultySizeBonus, 5, 9);
        int cellCount = size * size;
        int baseIdealSteps = template == null ? switch (realmRank) {
            case 1 -> 10;
            case 2 -> 17;
            case 3 -> 25;
            default -> 34 + (spellLearning ? Math.max(0, realmRank - 4) * 6 : 0);
        } : template.idealSteps();
        int idealSteps = Mth.clamp(baseIdealSteps + difficultyRouteBonus, size + 2, cellCount - 3);
        int baseMaximumSteps = template == null ? baseIdealSteps + (realmRank >= 4 ? 5 : 3)
                : template.maximumSteps();
        int maximumSteps = Mth.clamp(
                baseMaximumSteps + difficultyRouteBonus - Math.min(2, Math.max(0, puzzleDifficulty)),
                idealSteps + 1, cellCount - 1);

        Random random = new Random(seed ^ safeTarget.hashCode() ^ ((long) safeType.hashCode() << 32));
        List<Integer> solution = randomWindingPath(size, idealSteps, random);
        int start = solution.getFirst();
        int finalCore = solution.getLast();
        List<Node> nodes = new ArrayList<>(Collections.nCopies(cellCount, Node.normal()));
        Set<Integer> solutionSet = Set.copyOf(solution);
        addSealedNodes(nodes, solutionSet, realmRank, puzzleDifficulty, random);

        Set<Integer> occupied = new HashSet<>();
        occupied.add(0);
        occupied.add(solution.size() - 1);
        List<Integer> coreNodes = new ArrayList<>();
        List<DirectedEdge> directionalEdges = new ArrayList<>();
        Set<Edge> collapsingEdges = new HashSet<>();
        List<TimedRequirement> timedRequirements = new ArrayList<>();
        List<MovingHazard> movingHazards = new ArrayList<>();
        List<LinkedPair> linkedPairs = new ArrayList<>();
        List<SwitchGate> switchGates = new ArrayList<>();

        List<QiType> advancedQiTypes = advancedQiTypes(target.elements(), seed);
        addQiConversionSystems(nodes, solution, occupied, realmRank, advancedQiTypes);

        BranchDefinition branch = addCoreStructure(nodes, solution, occupied, realmRank, coreNodes);
        addTimedSystem(nodes, solution, occupied, realmRank, timedRequirements);
        addSwitchGate(nodes, solution, occupied, realmRank, switchGates);
        addRotatingNodes(nodes, solution, occupied, realmRank, random);
        addLinkedChoice(nodes, solution, solutionSet, occupied, realmRank, linkedPairs, random);

        int talentElementBonus = Math.max(0, puzzleDifficulty) / 2;
        int requiredElementCount = target.elements().isEmpty()
                ? 0
                : template == null ? switch (realmRank) {
                    case 1 -> 1;
                    case 2 -> Math.max(3, target.elements().size());
                    case 3 -> Math.max(4, target.elements().size());
                    default -> Math.max(spellLearning ? realmRank + 1 : 5, target.elements().size());
                } + talentElementBonus : template.requiredElementCount() + talentElementBonus;
        List<String> requestedElements = repeatElements(target.elements(), requiredElementCount);
        List<Integer> requiredNodes = addRequiredElements(
                nodes, solution, occupied, requestedElements);
        List<String> requiredElements = requiredNodes.stream()
                .map(index -> nodes.get(index).element())
                .toList();

        int[] strengthRange = strengthRange(spellLearning ? realmRank : Math.min(5, realmRank));
        int startingStrength = 20;
        int drainCount = realmRank < 3 ? 0 : realmRank >= 5 ? 2 : 1;
        int gatherCount = Math.max(0,
                (int) Math.ceil((strengthRange[0] - startingStrength + drainCount * 15) / 20.0D));
        addPathNodes(nodes, solution, occupied, NodeType.GATHER, gatherCount,
                QiType.NORMAL, 20, -1, random, true);
        addPathNodes(nodes, solution, occupied, NodeType.DRAIN, drainCount,
                QiType.NORMAL, 15, -1, random, true);
        addPathNodes(nodes, solution, occupied, NodeType.STABILIZE,
                realmRank < 2 ? 0 : 1 + realmRank / 3,
                QiType.NORMAL, 15, -1, random, false);
        addPathNodes(nodes, solution, occupied, NodeType.UNSTABLE,
                realmRank < 3 ? 0 : Math.min(4, realmRank - 1 + Math.max(0, puzzleDifficulty)),
                QiType.NORMAL, 15, -1, random, false);
        addReverseNodes(nodes, solution, occupied,
                realmRank < 3 ? 0 : 1 + (realmRank >= 5 ? 1 : 0),
                10, random);
        balancePathStrength(nodes, solution, startingStrength, strengthRange[0], strengthRange[1]);

        addDirectionalEdges(solution, realmRank, directionalEdges);
        addCollapsingEdges(solution, realmRank, collapsingEdges);
        addOptionalNodes(nodes, solutionSet, realmRank, target.elements(), random);
        normalizeElementNodes(nodes, requiredNodes, target.elements());
        requiredElements = requiredNodes.stream()
                .map(index -> nodes.get(index).element())
                .toList();
        addFalseCores(nodes, solutionSet, realmRank, random);
        addMovingHazards(nodes, solution, realmRank, movingHazards, random);

        nodes.set(start, Node.simple(NodeType.START));
        nodes.set(finalCore, Node.simple(NodeType.CORE));
        coreNodes.add(finalCore);

        int branchMoves = branch == null ? 0 : 2;
        int rotatingAllowance = (int) nodes.stream()
                .filter(node -> node.type() == NodeType.ROTATING)
                .count() * 3;
        int hazardAllowance = movingHazards.size() * 2;
        int adjustedIdealSteps = idealSteps + branchMoves + rotatingAllowance;
        int minimumSlack = starterMethod ? 6 : puzzleDifficulty >= 3 ? 1 : puzzleDifficulty >= 1 ? 2 : 3;
        int adjustedMaximumSteps = Math.max(adjustedIdealSteps + minimumSlack,
                maximumSteps + branchMoves + rotatingAllowance + hazardAllowance);
        int unavoidableStabilityLoss = unavoidableStabilityLoss(nodes, solution);
        int baseStartingStability = 100 - Math.max(0, puzzleDifficulty) * 3;
        int safeStartingStability = Mth.clamp(
                Math.max(baseStartingStability, unavoidableStabilityLoss + 20), 85, 100);

        LingqiTrajectoryConfig config = new LingqiTrajectoryConfig(
                size,
                size,
                start,
                finalCore,
                nodes,
                requiredNodes,
                coreNodes,
                requiredElements,
                adjustedIdealSteps,
                adjustedMaximumSteps,
                safeStartingStability,
                startingStrength,
                strengthRange[0],
                strengthRange[1],
                realmRank >= 2,
                puzzleRealm,
                directionalEdges,
                collapsingEdges,
                timedRequirements,
                movingHazards,
                linkedPairs,
                switchGates,
                branch);
        return new GeneratedPuzzle(config, solution);
    }

    static boolean hasGuaranteedRoute(LingqiTrajectoryConfig config, List<Integer> route) {
        if (route == null || route.size() < 2
                || route.getFirst() != config.startIndex()
                || route.getLast() != config.finalCoreIndex()) {
            return false;
        }
        Set<Integer> routeNodes = new HashSet<>(route);
        if (routeNodes.size() != route.size()) {
            return false;
        }
        for (int position = 1; position < route.size(); position++) {
            if (!config.adjacent(route.get(position - 1), route.get(position))) {
                return false;
            }
        }

        Set<Integer> protectedNodes = new HashSet<>(routeNodes);
        if (config.branch() != null) {
            protectedNodes.add(config.branch().leftCore());
            protectedNodes.add(config.branch().rightCore());
        }
        for (MovingHazard hazard : config.movingHazards()) {
            for (int index : hazard.path()) {
                if (protectedNodes.contains(index)) {
                    return false;
                }
            }
        }

        GuaranteedRouteState state = new GuaranteedRouteState(config);
        state.activatedNodes.add(config.startIndex());
        int lastDx = 0;
        int lastDy = 0;
        for (int position = 1; position < route.size(); position++) {
            int previous = route.get(position - 1);
            int index = route.get(position);
            Node previousNode = config.node(previous);
            if (previousNode.type() == NodeType.ROTATING) {
                int waits = 0;
                while (!rotatingExitMatches(config, previousNode, previous, index, state.steps)) {
                    if (waits++ >= 3 || !advanceGuaranteedClock(config, state)
                            || !deadlinesValid(state)) {
                        return false;
                    }
                }
            }

            DirectedEdge directional = config.directionalRule(previous, index);
            if (directional != null && (directional.from() != previous || directional.to() != index)) {
                return false;
            }
            Node node = config.node(index);
            if (node.type() == NodeType.BLOCKED || node.type() == NodeType.FALSE_CORE
                    || state.disabledLinkedNodes.contains(index)) {
                return false;
            }
            if (node.type() == NodeType.GATE && !state.openedGateGroups.contains(node.group())) {
                return false;
            }
            if (node.type() == NodeType.QI_CHANNEL && node.qiType() != state.qiType) {
                return false;
            }

            int dx = config.column(index) - config.column(previous);
            int dy = config.row(index) - config.row(previous);
            if (state.mustTurn && dx == lastDx && dy == lastDy) {
                return false;
            }
            state.mustTurn = false;
            if (!advanceGuaranteedClock(config, state)
                    || !activateGuaranteedNode(config, state, node, index)) {
                return false;
            }
            lastDx = dx;
            lastDy = dy;

            BranchDefinition branch = config.branch();
            if (branch != null && index == branch.splitNode() && !state.branchComplete) {
                if (!activateGuaranteedBranches(config, state, branch, routeNodes)) {
                    return false;
                }
                lastDx = 0;
                lastDy = 0;
            }
            if (!deadlinesValid(state) || state.stability <= 0) {
                return false;
            }
        }

        if (!state.activatedNodes.containsAll(config.requiredNodes())
                || !state.activatedCores.containsAll(config.coreNodes())
                || state.completedTimers.size() < config.timedRequirements().size()
                || !state.branchComplete
                || state.strength < config.minimumStrength()
                || state.strength > config.maximumStrength()) {
            return false;
        }
        for (int index = 0; index < config.nodes().size(); index++) {
            if (config.node(index).required() && !state.activatedNodes.contains(index)) {
                return false;
            }
        }
        Map<String, Integer> requiredCounts = new HashMap<>();
        for (String element : config.requiredElements()) {
            requiredCounts.merge(element, 1, Integer::sum);
        }
        for (Map.Entry<String, Integer> requirement : requiredCounts.entrySet()) {
            if (state.elementActivations.getOrDefault(requirement.getKey(), 0)
                    .intValue() != requirement.getValue()) {
                return false;
            }
        }
        return true;
    }

    private static boolean rotatingExitMatches(LingqiTrajectoryConfig config, Node node,
                                               int current, int destination, int steps) {
        int direction = Math.floorMod(node.rotation() + steps, 4);
        int dx = config.column(destination) - config.column(current);
        int dy = config.row(destination) - config.row(current);
        return switch (direction) {
            case 0 -> dx == 0 && dy == -1;
            case 1 -> dx == 1 && dy == 0;
            case 2 -> dx == 0 && dy == 1;
            default -> dx == -1 && dy == 0;
        };
    }

    private static boolean advanceGuaranteedClock(LingqiTrajectoryConfig config,
                                                  GuaranteedRouteState state) {
        state.steps++;
        if (state.steps > config.idealSteps()) {
            state.stability -= 7;
        }
        return state.steps <= config.maximumSteps() && state.stability > 0;
    }

    private static boolean deadlinesValid(GuaranteedRouteState state) {
        for (int deadline : state.timerDeadlines.values()) {
            if (state.steps > deadline) {
                return false;
            }
        }
        return true;
    }

    private static boolean activateGuaranteedNode(LingqiTrajectoryConfig config,
                                                   GuaranteedRouteState state,
                                                   Node node, int index) {
        state.activatedNodes.add(index);
        switch (node.type()) {
            case ELEMENT -> {
                int count = state.elementActivations.merge(node.element(), 1, Integer::sum);
                int required = 0;
                for (String element : config.requiredElements()) {
                    if (element.equals(node.element())) {
                        required++;
                    }
                }
                if (count > required) {
                    return false;
                }
                if (node.required() && config.strictOrder()) {
                    if (node.order() != state.expectedOrder) {
                        return false;
                    }
                    state.expectedOrder++;
                }
            }
            case GATHER -> state.strength = Mth.clamp(state.strength + node.amount(), 0, 150);
            case DRAIN -> state.strength = Math.max(0, state.strength - node.amount());
            case STABILIZE -> state.stability = Mth.clamp(
                    state.stability + node.amount(), 0, config.startingStability());
            case REVERSE -> {
                state.stability -= Math.max(0, node.amount());
                state.mustTurn = true;
            }
            case UNSTABLE -> state.stability -= node.amount();
            case CONVERTER -> state.qiType = node.qiType();
            case TIMED -> {
                state.timerDeadlines.put(node.group(), state.steps + node.amount());
                state.completedTimers.remove(node.group());
            }
            case TIMED_TARGET -> {
                Integer deadline = state.timerDeadlines.get(node.group());
                if (deadline == null || state.steps > deadline) {
                    return false;
                }
                state.completedTimers.add(node.group());
                state.timerDeadlines.remove(node.group());
            }
            case LINKED -> {
                for (LinkedPair pair : config.linkedPairs()) {
                    if (pair.first() == index || pair.second() == index) {
                        state.disabledLinkedNodes.add(pair.other(index));
                    }
                }
            }
            case SWITCH -> state.openedGateGroups.add(node.group());
            case CORE, SUB_CORE -> state.activatedCores.add(index);
            case BLOCKED, FALSE_CORE -> {
                return false;
            }
            default -> {
            }
        }
        return state.stability > 0;
    }

    private static boolean activateGuaranteedBranches(LingqiTrajectoryConfig config,
                                                       GuaranteedRouteState state,
                                                       BranchDefinition branch,
                                                       Set<Integer> routeNodes) {
        if (routeNodes.contains(branch.leftCore()) || routeNodes.contains(branch.rightCore())
                || !config.adjacent(branch.splitNode(), branch.leftCore())
                || !config.adjacent(branch.splitNode(), branch.rightCore())) {
            return false;
        }
        for (int core : List.of(branch.leftCore(), branch.rightCore())) {
            Node node = config.node(core);
            if (node.type() != NodeType.SUB_CORE || !advanceGuaranteedClock(config, state)) {
                return false;
            }
            state.activatedNodes.add(core);
            state.activatedCores.add(core);
            if (!deadlinesValid(state)) {
                return false;
            }
        }
        state.branchComplete = true;
        return true;
    }

    private static LingqiTrajectoryConfig guaranteedFallback(LingqiTrajectoryConfig original,
                                                              List<Integer> route) {
        List<Node> nodes = new ArrayList<>(original.nodes());
        Set<Integer> routeNodes = Set.copyOf(route);
        for (int index = 0; index < nodes.size(); index++) {
            Node node = nodes.get(index);
            if (!routeNodes.contains(index)) {
                if (node.required() || node.type() == NodeType.SUB_CORE) {
                    nodes.set(index, Node.normal());
                }
                continue;
            }
            if (node.type() != NodeType.ELEMENT && node.type() != NodeType.GATHER
                    && node.type() != NodeType.DRAIN && node.type() != NodeType.STABILIZE) {
                nodes.set(index, Node.normal());
            }
        }
        nodes.set(original.startIndex(), Node.simple(NodeType.START));
        nodes.set(original.finalCoreIndex(), Node.simple(NodeType.CORE));
        balancePathStrength(nodes, route, original.startingStrength(),
                original.minimumStrength(), original.maximumStrength());

        List<Integer> requiredNodes = original.requiredNodes().stream()
                .filter(routeNodes::contains)
                .toList();
        List<String> requiredElements = requiredNodes.stream()
                .map(index -> nodes.get(index).element())
                .toList();
        int idealSteps = route.size() - 1;
        return new LingqiTrajectoryConfig(
                original.width(), original.height(), original.startIndex(), original.finalCoreIndex(),
                nodes, requiredNodes, List.of(original.finalCoreIndex()), requiredElements,
                idealSteps, idealSteps + 3, 100, original.startingStrength(),
                original.minimumStrength(), original.maximumStrength(), original.strictOrder(),
                original.realm(), List.of(), Set.of(), List.of(), List.of(), List.of(), List.of(), null);
    }

    private static Target resolveTarget(String learningType, String targetId) {
        if ("spell".equals(learningType)) {
            ModSpells.SpellDef spell = ModSpells.get(targetId);
            if (spell != null) {
                String level = spell.requiredLevel();
                if (level == null || level.isBlank()) {
                    level = inferredLevel(spell.tier());
                }
                return new Target(level, CultivationLevels.getLevelDef(level).realm(),
                        parseElements(spell.element()));
            }
        } else if ("method".equals(learningType)) {
            CultivationMethods.MethodDef method = CultivationMethods.get(targetId);
            if (method != null) {
                String level = method.limitLevel();
                return new Target(level, CultivationLevels.getLevelDef(level).realm(),
                        parseElements(method.element()));
            }
        }
        String fallback = CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY;
        return new Target(fallback, CultivationLevels.REALM_LIANQI, List.of());
    }

    static boolean isStarterMethod(String methodId) {
        return CultivationMethods.BASIC_BREATHING.equals(methodId)
                || CultivationMethods.CLEAR_HEART.equals(methodId);
    }

    /** Same-realm learning starts at Foundation difficulty; each realm gap adds one tier. */
    static int learningPuzzleRealmRank(String playerLevel, String contentLevel) {
        if (playerLevel == null || playerLevel.isBlank()) return 1;
        int contentRealm = Mth.clamp(CultivationLevels.getRealmIndex(contentLevel), 1, 9);
        int playerRealm = Mth.clamp(CultivationLevels.getRealmIndex(playerLevel), 0, 9);
        return Mth.clamp(2 + contentRealm - playerRealm, 1, 9);
    }

    private static String realmName(int rank) {
        return switch (rank) {
            case 2 -> CultivationLevels.REALM_ZHUJI;
            case 3 -> CultivationLevels.REALM_JINDAN;
            case 4 -> CultivationLevels.REALM_YUANYING;
            case 5 -> CultivationLevels.REALM_HUASHEN;
            case 6 -> CultivationLevels.REALM_LIANXU;
            case 7 -> CultivationLevels.REALM_HETI;
            case 8 -> CultivationLevels.REALM_DACHENG;
            case 9 -> CultivationLevels.REALM_DUJIE;
            default -> CultivationLevels.REALM_LIANQI;
        };
    }

    private static String inferredLevel(String tier) {
        return switch (tier == null ? "" : tier) {
            case "earth" -> CultivationLevels.REALM_JINDAN + CultivationLevels.STAGE_EARLY;
            case "heaven" -> CultivationLevels.REALM_HUASHEN + CultivationLevels.STAGE_EARLY;
            case "immortal", "divine" -> CultivationLevels.REALM_LIANXU + CultivationLevels.STAGE_EARLY;
            default -> CultivationLevels.REALM_LIANQI + CultivationLevels.STAGE_EARLY;
        };
    }

    private static List<String> parseElements(String value) {
        if (value == null || value.isBlank()
                || "any".equalsIgnoreCase(value) || "none".equalsIgnoreCase(value)) {
            return List.of();
        }
        LinkedHashSet<String> elements = new LinkedHashSet<>();
        for (String element : value.split(",")) {
            String clean = element.trim();
            if (SpiritRoots.ALL_ELEMENTS.contains(clean)) {
                elements.add(clean);
            }
        }
        return List.copyOf(elements);
    }

    private static int pointDifficulty(int requiredPoints, float difficulty, float talent) {
        float safeTalent = Mth.clamp(talent, 0.1F, 1.0F);
        float ratio = requiredPoints > 0
                ? requiredPoints / (float) BASE_LEARNING_POINTS
                : difficulty / safeTalent;
        int scoreDifficulty;
        if (ratio >= 8.0F) scoreDifficulty = 4;
        else if (ratio >= 5.0F) scoreDifficulty = 3;
        else if (ratio >= 2.5F) scoreDifficulty = 2;
        else if (ratio >= 1.5F) scoreDifficulty = 1;
        else scoreDifficulty = ratio <= 0.5F ? -1 : 0;

        int talentDifficulty;
        if (safeTalent <= 0.2F) talentDifficulty = 4;
        else if (safeTalent <= 0.4F) talentDifficulty = 3;
        else if (safeTalent <= 0.6F) talentDifficulty = 2;
        else if (safeTalent <= 0.8F) talentDifficulty = 1;
        else talentDifficulty = 0;
        return Math.max(scoreDifficulty, talentDifficulty);
    }

    private static int earlyRealmDifficulty(int realmRank, int difficulty) {
        return switch (realmRank) {
            case 1 -> Math.min(difficulty, 1);
            case 2 -> Math.min(difficulty, 2);
            default -> difficulty;
        };
    }

    private static int[] strengthRange(int realmRank) {
        return switch (realmRank) {
            case 1 -> new int[]{20, 100};
            case 2 -> new int[]{40, 100};
            case 3 -> new int[]{60, 80};
            case 4 -> new int[]{65, 85};
            default -> new int[]{70 + (realmRank - 5) * 2, 90 - (realmRank - 5) * 2};
        };
    }

    private static List<QiType> advancedQiTypes(List<String> elements, long seed) {
        LinkedHashSet<QiType> types = new LinkedHashSet<>();
        for (String element : elements) {
            QiType type = QiType.fromElement(element);
            if (type != QiType.NORMAL) {
                types.add(type);
            }
        }
        if (types.isEmpty()) {
            types.add((seed & 1L) == 0L ? QiType.YIN : QiType.YANG);
        }
        if (types.size() < 2) {
            QiType first = types.getFirst();
            types.add(first == QiType.YIN ? QiType.YANG
                    : first == QiType.FIRE ? QiType.WATER : QiType.THUNDER);
        }
        return List.copyOf(types);
    }

    private static void addQiConversionSystems(List<Node> nodes, List<Integer> solution,
                                               Set<Integer> occupied, int realmRank,
                                               List<QiType> qiTypes) {
        if (realmRank < 4) {
            return;
        }
        int pairs = realmRank >= 5 ? 2 : 1;
        for (int i = 0; i < pairs; i++) {
            int converterPosition = claimPosition(solution, occupied,
                    Math.round(solution.size() * (i == 0 ? 0.12F : 0.42F)), 1, solution.size() - 3);
            if (converterPosition < 0) {
                continue;
            }
            int channelPosition = claimPosition(solution, occupied,
                    Math.round(solution.size() * (i == 0 ? 0.24F : 0.54F)),
                    Math.max(2, converterPosition + 1), solution.size() - 2);
            if (channelPosition < 0) {
                continue;
            }
            QiType type = qiTypes.get(i % qiTypes.size());
            nodes.set(solution.get(converterPosition),
                    new Node(NodeType.CONVERTER, "", -1, true, type, 0, i, 0));
            nodes.set(solution.get(channelPosition),
                    new Node(NodeType.QI_CHANNEL, "", -1, true, type, 0, i, 0));
        }
    }

    private static BranchDefinition addCoreStructure(List<Node> nodes, List<Integer> solution,
                                                     Set<Integer> occupied, int realmRank,
                                                     List<Integer> coreNodes) {
        if (realmRank < 3) {
            return null;
        }
        if (realmRank == 3) {
            int firstPosition = claimPosition(solution, occupied,
                    Math.round(solution.size() * 0.55F), 2, solution.size() - 4);
            if (firstPosition < 0) {
                return null;
            }
            int core = solution.get(firstPosition);
            nodes.set(core, new Node(NodeType.SUB_CORE, "", -1, true,
                    QiType.NORMAL, 0, 0, 0));
            coreNodes.add(core);
            int secondPosition = claimPosition(solution, occupied,
                    Math.round(solution.size() * 0.76F), firstPosition + 1, solution.size() - 2);
            if (secondPosition >= 0) {
                int secondCore = solution.get(secondPosition);
                nodes.set(secondCore, new Node(NodeType.SUB_CORE, "", -1, true,
                        QiType.NORMAL, 0, 1, 0));
                coreNodes.add(secondCore);
            }
            return null;
        }

        int splitPosition = findBranchSplitPosition(solution, occupied, nodes.size());
        if (splitPosition < 0) {
            return null;
        }
        int split = solution.get(splitPosition);
        Set<Integer> solutionSet = Set.copyOf(solution);
        List<Integer> branchCores = allNeighbors(split, (int) Math.sqrt(nodes.size())).stream()
                .filter(index -> !solutionSet.contains(index))
                .sorted()
                .toList();
        if (branchCores.size() < 2) {
            return null;
        }
        occupied.add(splitPosition);
        int leftCore = branchCores.get(0);
        int rightCore = branchCores.get(1);
        nodes.set(split, Node.simple(NodeType.SPLIT));
        nodes.set(leftCore, new Node(NodeType.SUB_CORE, "", -1, true,
                QiType.NORMAL, 0, 0, 0));
        nodes.set(rightCore, new Node(NodeType.SUB_CORE, "", -1, true,
                QiType.NORMAL, 0, 1, 0));
        coreNodes.add(leftCore);
        coreNodes.add(rightCore);
        return new BranchDefinition(split, leftCore, rightCore, 10);
    }

    private static void addTimedSystem(List<Node> nodes, List<Integer> solution,
                                       Set<Integer> occupied, int realmRank,
                                       List<TimedRequirement> timedRequirements) {
        if (realmRank < 3) {
            return;
        }
        int triggerPosition = claimPosition(solution, occupied,
                Math.round(solution.size() * (realmRank >= 4 ? 0.56F : 0.38F)),
                2, solution.size() - 7);
        if (triggerPosition < 0) {
            return;
        }
        int targetPosition = claimPosition(solution, occupied,
                triggerPosition + 4, triggerPosition + 3, Math.min(solution.size() - 2, triggerPosition + 6));
        if (targetPosition < 0) {
            return;
        }
        int group = 0;
        int trigger = solution.get(triggerPosition);
        int target = solution.get(targetPosition);
        int rotationWaitAllowance = realmRank >= 6 ? 6 : realmRank >= 4 ? 3 : 0;
        int moveLimit = Math.max(realmRank >= 5 ? 4 : 5,
                targetPosition - triggerPosition + rotationWaitAllowance + 1);
        nodes.set(trigger, new Node(NodeType.TIMED, "", -1, true,
                QiType.NORMAL, moveLimit, group, 0));
        nodes.set(target, new Node(NodeType.TIMED_TARGET, "", -1, true,
                QiType.NORMAL, 0, group, 0));
        timedRequirements.add(new TimedRequirement(trigger, target, moveLimit, group));
    }

    private static void addSwitchGate(List<Node> nodes, List<Integer> solution,
                                      Set<Integer> occupied, int realmRank,
                                      List<SwitchGate> switchGates) {
        if (realmRank < 4) {
            return;
        }
        int switchPosition = claimPosition(solution, occupied,
                Math.round(solution.size() * 0.18F), 1, solution.size() - 6);
        if (switchPosition < 0) {
            return;
        }
        int gatePosition = claimPosition(solution, occupied,
                Math.round(solution.size() * 0.48F), switchPosition + 2, solution.size() - 2);
        if (gatePosition < 0) {
            return;
        }
        int switchNode = solution.get(switchPosition);
        int gateNode = solution.get(gatePosition);
        nodes.set(switchNode, new Node(NodeType.SWITCH, "", -1, true,
                QiType.NORMAL, 0, 0, 0));
        nodes.set(gateNode, new Node(NodeType.GATE, "", -1, true,
                QiType.NORMAL, 0, 0, 0));
        switchGates.add(new SwitchGate(switchNode, gateNode, 0));
    }

    private static void addRotatingNodes(List<Node> nodes, List<Integer> solution,
                                         Set<Integer> occupied, int realmRank, Random random) {
        if (realmRank < 4) {
            return;
        }
        int count = realmRank >= 6 ? 2 : 1;
        for (int i = 0; i < count; i++) {
            int position = claimPosition(solution, occupied,
                    Math.round(solution.size() * (0.62F + i * 0.08F)), 2, solution.size() - 2);
            if (position >= 0) {
                nodes.set(solution.get(position), new Node(NodeType.ROTATING, "", -1, true,
                        QiType.NORMAL, 0, i, random.nextInt(4)));
            }
        }
    }

    private static void addLinkedChoice(List<Node> nodes, List<Integer> solution,
                                        Set<Integer> solutionSet, Set<Integer> occupied,
                                        int realmRank, List<LinkedPair> linkedPairs, Random random) {
        if (realmRank < 4) {
            return;
        }
        int requiredPosition = claimPosition(solution, occupied,
                Math.round(solution.size() * 0.68F), 2, solution.size() - 2);
        int optional = randomOutsideNode(nodes, solutionSet, random);
        if (requiredPosition < 0 || optional < 0) {
            return;
        }
        int required = solution.get(requiredPosition);
        nodes.set(required, new Node(NodeType.LINKED, "", -1, true,
                QiType.YANG, 0, 0, 0));
        nodes.set(optional, new Node(NodeType.LINKED, "", -1, false,
                QiType.YIN, 0, 0, 0));
        linkedPairs.add(new LinkedPair(required, optional, 0));
    }

    private static List<Integer> addRequiredElements(List<Node> nodes, List<Integer> solution,
                                                     Set<Integer> occupied, List<String> elements) {
        if (elements.isEmpty()) {
            return List.of();
        }
        List<Integer> positions = new ArrayList<>();
        for (int order = 0; order < elements.size(); order++) {
            int preferred = 1 + Math.round((order + 1) * (solution.size() - 3) / (float) (elements.size() + 1));
            int position = claimPosition(solution, occupied, preferred, 1, solution.size() - 2);
            if (position >= 0) {
                positions.add(position);
            }
        }
        Collections.sort(positions);
        List<Integer> required = new ArrayList<>();
        for (int order = 0; order < positions.size(); order++) {
            int nodeIndex = solution.get(positions.get(order));
            String element = elements.get(order % elements.size());
            nodes.set(nodeIndex, new Node(NodeType.ELEMENT, element, order, true,
                    QiType.fromElement(element), 0, -1, 0));
            required.add(nodeIndex);
        }
        return List.copyOf(required);
    }

    private static void addPathNodes(List<Node> nodes, List<Integer> solution,
                                     Set<Integer> occupied, NodeType type, int count,
                                     QiType qiType, int amount, int group,
                                     Random random, boolean required) {
        List<Integer> positions = freePathPositions(solution, occupied);
        Collections.shuffle(positions, random);
        for (int i = 0; i < Math.min(count, positions.size()); i++) {
            int position = positions.get(i);
            occupied.add(position);
            nodes.set(solution.get(position), new Node(type, "", -1, required,
                    qiType, amount, group, 0));
        }
    }

    private static void addReverseNodes(List<Node> nodes, List<Integer> solution,
                                        Set<Integer> occupied, int count, int amount,
                                        Random random) {
        List<Integer> turns = new ArrayList<>();
        for (int position = 1; position < solution.size() - 1; position++) {
            if (occupied.contains(position)) {
                continue;
            }
            int previous = solution.get(position - 1);
            int current = solution.get(position);
            int next = solution.get(position + 1);
            int incomingX = current % gridSize(nodes) - previous % gridSize(nodes);
            int incomingY = current / gridSize(nodes) - previous / gridSize(nodes);
            int outgoingX = next % gridSize(nodes) - current % gridSize(nodes);
            int outgoingY = next / gridSize(nodes) - current / gridSize(nodes);
            if (incomingX != outgoingX || incomingY != outgoingY) {
                turns.add(position);
            }
        }
        Collections.shuffle(turns, random);
        for (int i = 0; i < Math.min(count, turns.size()); i++) {
            int position = turns.get(i);
            occupied.add(position);
            nodes.set(solution.get(position), new Node(NodeType.REVERSE, "", -1, false,
                    QiType.NORMAL, amount, -1, 0));
        }
    }

    private static void balancePathStrength(List<Node> nodes, List<Integer> solution,
                                            int startingStrength, int minimumStrength,
                                            int maximumStrength) {
        int finalStrength = pathStrength(nodes, solution, startingStrength);
        if (finalStrength >= minimumStrength && finalStrength <= maximumStrength) {
            return;
        }

        NodeType adjustmentType = finalStrength < minimumStrength ? NodeType.GATHER : NodeType.DRAIN;
        int adjustment = finalStrength < minimumStrength
                ? minimumStrength - finalStrength
                : finalStrength - maximumStrength;
        for (int index : solution) {
            Node node = nodes.get(index);
            if (node.type() == adjustmentType) {
                nodes.set(index, new Node(node.type(), node.element(), node.order(), node.required(),
                        node.qiType(), node.amount() + adjustment, node.group(), node.rotation()));
                return;
            }
        }

        for (int position = 1; position < solution.size() - 1; position++) {
            int index = solution.get(position);
            if (nodes.get(index).type() == NodeType.NORMAL) {
                nodes.set(index, new Node(adjustmentType, "", -1, true,
                        QiType.NORMAL, adjustment, -1, 0));
                return;
            }
        }
    }

    private static int pathStrength(List<Node> nodes, List<Integer> solution, int startingStrength) {
        int strength = startingStrength;
        for (int index : solution) {
            Node node = nodes.get(index);
            if (node.type() == NodeType.GATHER) {
                strength += node.amount();
            } else if (node.type() == NodeType.DRAIN) {
                strength = Math.max(0, strength - node.amount());
            }
        }
        return strength;
    }

    private static int unavoidableStabilityLoss(List<Node> nodes, List<Integer> solution) {
        int loss = 0;
        for (int index : solution) {
            Node node = nodes.get(index);
            if (node.type() == NodeType.UNSTABLE || node.type() == NodeType.REVERSE) {
                loss += Math.max(0, node.amount());
            }
        }
        return loss;
    }

    private static int gridSize(List<Node> nodes) {
        return Math.max(1, (int) Math.sqrt(nodes.size()));
    }

    private static void addDirectionalEdges(List<Integer> solution, int realmRank,
                                            List<DirectedEdge> directionalEdges) {
        if (realmRank < 2) {
            return;
        }
        int count = Math.min(realmRank >= 4 ? 7 : realmRank == 3 ? 5 : 3, solution.size() - 1);
        for (int i = 0; i < count; i++) {
            int position = 1 + Math.round((i + 1) * (solution.size() - 3) / (float) (count + 1));
            position = Mth.clamp(position, 0, solution.size() - 2);
            directionalEdges.add(new DirectedEdge(solution.get(position), solution.get(position + 1)));
        }
    }

    private static void addCollapsingEdges(List<Integer> solution, int realmRank,
                                           Set<Edge> collapsingEdges) {
        if (realmRank < 3) {
            return;
        }
        int count = realmRank >= 4 ? 6 : 3;
        for (int i = 0; i < count; i++) {
            int position = 1 + Math.round((i + 1) * (solution.size() - 3) / (float) (count + 1));
            position = Mth.clamp(position, 0, solution.size() - 2);
            collapsingEdges.add(new Edge(solution.get(position), solution.get(position + 1)));
        }
    }

    private static void addOptionalNodes(List<Node> nodes, Set<Integer> solution,
                                         int realmRank, List<String> elements, Random random) {
        if (realmRank < 2) {
            return;
        }
        int gather = randomOutsideNode(nodes, solution, random);
        if (gather >= 0) {
            nodes.set(gather, new Node(NodeType.GATHER, "", -1, false,
                    QiType.NORMAL, 20, -1, 0));
        }
        int stabilize = randomOutsideNode(nodes, solution, random);
        if (stabilize >= 0) {
            nodes.set(stabilize, new Node(NodeType.STABILIZE, "", -1, false,
                    QiType.NORMAL, 15, -1, 0));
        }
        if (realmRank >= 3) {
            int drain = randomOutsideNode(nodes, solution, random);
            if (drain >= 0) {
                nodes.set(drain, new Node(NodeType.DRAIN, "", -1, false,
                        QiType.NORMAL, 15, -1, 0));
            }
            if (!elements.isEmpty()) {
                int decoy = randomOutsideNode(nodes, solution, random);
                if (decoy >= 0) {
                    String element = elements.getFirst();
                    nodes.set(decoy, new Node(NodeType.ELEMENT, element, -1, false,
                            QiType.fromElement(element), 0, -1, 0));
                }
            }
        }
    }

    private static void normalizeElementNodes(List<Node> nodes, List<Integer> requiredNodes,
                                              List<String> declaredElements) {
        Set<Integer> required = Set.copyOf(requiredNodes);
        if (declaredElements.isEmpty()) {
            for (int index = 0; index < nodes.size(); index++) {
                if (nodes.get(index).type() == NodeType.ELEMENT) {
                    nodes.set(index, Node.normal());
                }
            }
            return;
        }

        for (int order = 0; order < requiredNodes.size(); order++) {
            int index = requiredNodes.get(order);
            Node node = nodes.get(index);
            String element = declaredElements.get(order % declaredElements.size());
            nodes.set(index, new Node(NodeType.ELEMENT, element, node.order(), true,
                    QiType.fromElement(element), node.amount(), node.group(), node.rotation()));
        }

        int optionalOrder = 0;
        for (int index = 0; index < nodes.size(); index++) {
            Node node = nodes.get(index);
            if (node.type() != NodeType.ELEMENT || required.contains(index)) {
                continue;
            }
            String element = declaredElements.get(optionalOrder++ % declaredElements.size());
            nodes.set(index, new Node(NodeType.ELEMENT, element, node.order(), false,
                    QiType.fromElement(element), node.amount(), node.group(), node.rotation()));
        }
    }

    private static void addFalseCores(List<Node> nodes, Set<Integer> solution,
                                      int realmRank, Random random) {
        if (realmRank < 4) {
            return;
        }
        int count = realmRank >= 6 ? 2 : 1;
        for (int i = 0; i < count; i++) {
            int falseCore = randomOutsideNode(nodes, solution, random);
            if (falseCore >= 0) {
                nodes.set(falseCore, new Node(NodeType.FALSE_CORE, "", -1, false,
                        QiType.NORMAL, 30, i, 0));
            }
        }
    }

    private static void addMovingHazards(List<Node> nodes, List<Integer> solution,
                                         int realmRank, List<MovingHazard> hazards, Random random) {
        if (realmRank < 4 || solution.size() < 8) {
            return;
        }
        int center = solution.get(Math.round(solution.size() * 0.52F));
        int size = (int) Math.sqrt(nodes.size());
        int centerColumn = center % size;
        int centerRow = center / size;
        Set<Integer> protectedRoute = Set.copyOf(solution);
        List<Integer> candidates = new ArrayList<>();
        for (int index = 0; index < nodes.size(); index++) {
            if (!protectedRoute.contains(index) && nodes.get(index).type() == NodeType.NORMAL) {
                candidates.add(index);
            }
        }
        candidates.sort((first, second) -> {
            int firstDistance = Math.abs(first % size - centerColumn) + Math.abs(first / size - centerRow);
            int secondDistance = Math.abs(second % size - centerColumn) + Math.abs(second / size - centerRow);
            return Integer.compare(firstDistance, secondDistance);
        });
        int localPoolSize = Math.min(8, candidates.size());
        List<Integer> localPool = new ArrayList<>(candidates.subList(0, localPoolSize));
        Collections.shuffle(localPool, random);
        List<Integer> path = new ArrayList<>(localPool.subList(0, Math.min(4, localPool.size())));
        if (path.size() >= 2) {
            hazards.add(new MovingHazard(List.copyOf(path), realmRank >= 5 ? 1 : 2));
        }
    }

    private static int claimPosition(List<Integer> solution, Set<Integer> occupied,
                                     int preferred, int minimum, int maximum) {
        if (minimum > maximum) {
            return -1;
        }
        preferred = Mth.clamp(preferred, minimum, maximum);
        for (int radius = 0; radius < solution.size(); radius++) {
            int forward = preferred + radius;
            if (forward <= maximum && !occupied.contains(forward)) {
                occupied.add(forward);
                return forward;
            }
            int backward = preferred - radius;
            if (backward >= minimum && !occupied.contains(backward)) {
                occupied.add(backward);
                return backward;
            }
        }
        return -1;
    }

    private static int findBranchSplitPosition(List<Integer> solution, Set<Integer> occupied,
                                               int cellCount) {
        int size = Math.max(1, (int) Math.sqrt(cellCount));
        Set<Integer> solutionSet = Set.copyOf(solution);
        int preferred = Math.round(solution.size() * 0.38F);
        for (int radius = 0; radius < solution.size(); radius++) {
            int[] positions = {preferred + radius, preferred - radius};
            for (int position : positions) {
                if (position < 2 || position >= solution.size() - 3 || occupied.contains(position)) {
                    continue;
                }
                long sideNodes = allNeighbors(solution.get(position), size).stream()
                        .filter(index -> !solutionSet.contains(index))
                        .count();
                if (sideNodes >= 2) {
                    return position;
                }
            }
        }
        return -1;
    }

    private static List<Integer> freePathPositions(List<Integer> solution, Set<Integer> occupied) {
        List<Integer> positions = new ArrayList<>();
        for (int position = 1; position < solution.size() - 1; position++) {
            if (!occupied.contains(position)) {
                positions.add(position);
            }
        }
        return positions;
    }

    private static int randomOutsideNode(List<Node> nodes, Set<Integer> solution, Random random) {
        List<Integer> candidates = new ArrayList<>();
        for (int index = 0; index < nodes.size(); index++) {
            if (!solution.contains(index) && nodes.get(index).type() == NodeType.NORMAL) {
                candidates.add(index);
            }
        }
        return candidates.isEmpty() ? -1 : candidates.get(random.nextInt(candidates.size()));
    }

    private static List<String> repeatElements(List<String> elements, int count) {
        if (elements.isEmpty() || count <= 0) {
            return List.of();
        }
        List<String> repeated = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            repeated.add(elements.get(i % elements.size()));
        }
        return List.copyOf(repeated);
    }

    private static void addSealedNodes(List<Node> nodes, Set<Integer> solution,
                                       int realmRank, int pointDifficulty, Random random) {
        List<Integer> candidates = new ArrayList<>();
        for (int index = 0; index < nodes.size(); index++) {
            if (!solution.contains(index)) {
                candidates.add(index);
            }
        }
        Collections.shuffle(candidates, random);
        float ratio = Mth.clamp(0.22F + realmRank * 0.035F + pointDifficulty * 0.04F, 0.20F, 0.55F);
        int count = Math.min(candidates.size(), Math.round(candidates.size() * ratio));
        for (int i = 0; i < count; i++) {
            nodes.set(candidates.get(i), Node.simple(NodeType.BLOCKED));
        }
    }

    private static List<Integer> randomWindingPath(int size, int steps, Random random) {
        List<Integer> boundary = new ArrayList<>();
        for (int index = 0; index < size * size; index++) {
            int column = index % size;
            int row = index / size;
            if (column == 0 || column == size - 1 || row == 0 || row == size - 1) {
                boundary.add(index);
            }
        }
        for (int attempt = 0; attempt < 768; attempt++) {
            boolean[] visited = new boolean[size * size];
            List<Integer> path = new ArrayList<>(steps + 1);
            int current = boundary.get(random.nextInt(boundary.size()));
            visited[current] = true;
            path.add(current);
            while (path.size() <= steps) {
                List<Integer> candidates = unvisitedNeighbors(current, size, visited);
                if (candidates.isEmpty()) break;
                Collections.shuffle(candidates, random);
                candidates.sort((first, second) -> Integer.compare(
                        unvisitedNeighborCount(second, size, visited),
                        unvisitedNeighborCount(first, size, visited)));
                current = candidates.get(random.nextInt(Math.min(2, candidates.size())));
                visited[current] = true;
                path.add(current);
            }
            if (path.size() == steps + 1) {
                return List.copyOf(path);
            }
        }
        List<Integer> fallback = transformedSerpentine(size, random.nextInt(8));
        return List.copyOf(fallback.subList(0, steps + 1));
    }

    private static List<Integer> transformedSerpentine(int size, int transform) {
        List<Integer> path = new ArrayList<>(size * size);
        for (int row = 0; row < size; row++) {
            for (int offset = 0; offset < size; offset++) {
                int column = (row & 1) == 0 ? offset : size - 1 - offset;
                int x;
                int y;
                switch (transform) {
                    case 1 -> { x = size - 1 - column; y = row; }
                    case 2 -> { x = column; y = size - 1 - row; }
                    case 3 -> { x = size - 1 - column; y = size - 1 - row; }
                    case 4 -> { x = row; y = column; }
                    case 5 -> { x = size - 1 - row; y = column; }
                    case 6 -> { x = row; y = size - 1 - column; }
                    case 7 -> { x = size - 1 - row; y = size - 1 - column; }
                    default -> { x = column; y = row; }
                }
                path.add(y * size + x);
            }
        }
        return path;
    }

    private static List<Integer> unvisitedNeighbors(int index, int size, boolean[] visited) {
        List<Integer> neighbors = new ArrayList<>(4);
        for (int neighbor : allNeighbors(index, size)) {
            if (!visited[neighbor]) neighbors.add(neighbor);
        }
        return neighbors;
    }

    private static int unvisitedNeighborCount(int index, int size, boolean[] visited) {
        return unvisitedNeighbors(index, size, visited).size();
    }

    private static List<Integer> allNeighbors(int index, int size) {
        int column = index % size;
        int row = index / size;
        List<Integer> neighbors = new ArrayList<>(4);
        if (column > 0) neighbors.add(index - 1);
        if (column + 1 < size) neighbors.add(index + 1);
        if (row > 0) neighbors.add(index - size);
        if (row + 1 < size) neighbors.add(index + size);
        return neighbors;
    }

    public enum NodeType {
        NORMAL,
        START,
        CORE,
        SUB_CORE,
        BLOCKED,
        ELEMENT,
        GATHER,
        DRAIN,
        STABILIZE,
        SPLIT,
        REVERSE,
        UNSTABLE,
        CONVERTER,
        QI_CHANNEL,
        TIMED,
        TIMED_TARGET,
        ROTATING,
        LINKED,
        SWITCH,
        GATE,
        FALSE_CORE
    }

    public enum QiType {
        NORMAL,
        FIRE,
        WATER,
        THUNDER,
        YIN,
        YANG;

        public static QiType fromElement(String element) {
            element = SpiritRoots.normalizeElement(element);
            if (SpiritRoots.FIRE.equals(element)) return FIRE;
            if (SpiritRoots.WATER.equals(element) || SpiritRoots.ICE.equals(element)) return WATER;
            if (SpiritRoots.THUNDER.equals(element)) return THUNDER;
            if (SpiritRoots.YIN.equals(element)) return YIN;
            if (SpiritRoots.YANG.equals(element)) return YANG;
            return NORMAL;
        }
    }

    public record Node(NodeType type, String element, int order, boolean required,
                       QiType qiType, int amount, int group, int rotation) {
        private static Node normal() {
            return simple(NodeType.NORMAL);
        }

        private static Node simple(NodeType type) {
            return new Node(type, "", -1, false, QiType.NORMAL, 0, -1, 0);
        }
    }

    public record Edge(int first, int second) {
        public Edge {
            if (first > second) {
                int swap = first;
                first = second;
                second = swap;
            }
        }
    }

    public record DirectedEdge(int from, int to) {
        public Edge edge() {
            return new Edge(from, to);
        }
    }

    public record TimedRequirement(int trigger, int target, int moveLimit, int group) {
    }

    public record MovingHazard(List<Integer> path, int interval) {
        public MovingHazard {
            path = List.copyOf(path);
            interval = Math.max(1, interval);
        }

        public int positionAt(int action) {
            if (path.isEmpty()) return -1;
            int cycleLength = path.size() == 1 ? 1 : path.size() * 2 - 2;
            int cycle = Math.floorMod(action / interval, cycleLength);
            int index = cycle < path.size() ? cycle : cycleLength - cycle;
            return path.get(index);
        }
    }

    public record LinkedPair(int first, int second, int group) {
        public int other(int index) {
            return index == first ? second : first;
        }
    }

    public record SwitchGate(int switchNode, int gateNode, int group) {
    }

    public record BranchDefinition(int splitNode, int leftCore, int rightCore, int balanceTolerance) {
        public boolean isBranchCore(int index) {
            return index == leftCore || index == rightCore;
        }
    }

    private static final class GuaranteedRouteState {
        private final Set<Integer> activatedNodes = new HashSet<>();
        private final Set<Integer> activatedCores = new HashSet<>();
        private final Set<Integer> disabledLinkedNodes = new HashSet<>();
        private final Set<Integer> openedGateGroups = new HashSet<>();
        private final Set<Integer> completedTimers = new HashSet<>();
        private final Map<Integer, Integer> timerDeadlines = new java.util.HashMap<>();
        private final Map<String, Integer> elementActivations = new java.util.HashMap<>();
        private int steps;
        private int stability;
        private int strength;
        private int expectedOrder;
        private boolean mustTurn;
        private boolean branchComplete;
        private QiType qiType = QiType.NORMAL;

        private GuaranteedRouteState(LingqiTrajectoryConfig config) {
            stability = config.startingStability();
            strength = config.startingStrength();
            branchComplete = config.branch() == null;
        }
    }

    record GeneratedPuzzle(LingqiTrajectoryConfig config, List<Integer> route) {
        GeneratedPuzzle {
            route = List.copyOf(route);
        }
    }

    private record Target(String level, String realm, List<String> elements) {
    }

    private record Template(int size, int idealSteps, int maximumSteps, int requiredElementCount) {
    }

}
