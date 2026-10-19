package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class LingqiTrajectoryScreen extends Screen {
    private static final String PREFIX = "screen." + ImmortalCultivationMod.MODID + ".trajectory.";
    private static final int PANEL_WIDTH = 430;
    private static final int PANEL_HEIGHT = 343;
    private static final int HEADER_SIDE_INSET = 38;
    private static final int MAX_CELL_PITCH = 20;

    private final String learningType;
    private final String targetId;
    private final int requiredPoints;
    private final long token;
    private final LingqiTrajectoryConfig config;
    private final List<Integer> path = new ArrayList<>();
    private final Set<Integer> visited = new HashSet<>();
    private final Set<Integer> activatedRequired = new HashSet<>();
    private final Set<Integer> activatedCores = new HashSet<>();
    private final Set<Integer> activatedNodes = new HashSet<>();
    private final Set<Integer> disabledLinkedNodes = new HashSet<>();
    private final Set<Integer> openedGateGroups = new HashSet<>();
    private final Set<Integer> completedTimers = new HashSet<>();
    private final Set<LingqiTrajectoryConfig.Edge> collapsedEdges = new HashSet<>();
    private final Set<LingqiTrajectoryConfig.Edge> branchEdges = new HashSet<>();
    private final Map<Integer, Integer> timerDeadlines = new HashMap<>();
    private final Map<String, Integer> elementActivations = new HashMap<>();

    private CultivationGuiButton waitButton;
    private CultivationGuiButton completeButton;
    private int currentIndex;
    private int steps;
    private int stability;
    private int strength;
    private int expectedOrder;
    private int lastDx;
    private int lastDy;
    private int completedTicks;
    private int animationTicks;
    private int optionalScore;
    private int splitStrength;
    private int firstBranchCore;
    private int firstBranchStrength;
    private boolean mustTurn;
    private boolean orderBroken;
    private boolean failed;
    private boolean ready;
    private boolean submitted;
    private boolean branchComplete;
    private LingqiTrajectoryConfig.QiType qiType;
    private LingqiTrajectoryConfig.QiType splitQiType;
    private Component status;

    public LingqiTrajectoryScreen(String learningType, String targetId, int requiredPoints,
                                  float difficulty, float talent, boolean assisted, String playerLevel, long token) {
        super(Component.translatable(PREFIX + "title"));
        this.learningType = learningType;
        this.targetId = targetId;
        this.requiredPoints = Math.max(1, requiredPoints);
        this.token = token;
        this.config = LingqiTrajectoryConfig.create(
                learningType, targetId, requiredPoints, difficulty, talent, assisted, playerLevel, token);
        resetPuzzle();
    }

    @Override
    protected void init() {
        int panelX = panelX();
        int buttonY = panelY() + panelHeight() - 27;
        addRenderableWidget(new CultivationGuiButton(
                panelX + panelWidth() - 25, panelY() + 7, 18, 18,
                Component.literal("?"), () -> {
                    if (minecraft != null) {
                        minecraft.setScreen(new LingqiTrajectoryHelpScreen(this));
                    }
                }));
        addRenderableWidget(new CultivationGuiButton(
                panelX + panelWidth() / 2 - 138, buttonY, 86, 20,
                Component.translatable(PREFIX + "reset"), this::resetPuzzle));
        waitButton = addRenderableWidget(new CultivationGuiButton(
                panelX + panelWidth() / 2 - 43, buttonY, 86, 20,
                Component.translatable(PREFIX + "wait"), this::waitAction));
        completeButton = addRenderableWidget(new CultivationGuiButton(
                panelX + panelWidth() / 2 + 52, buttonY, 86, 20,
                Component.translatable(PREFIX + "complete"), this::completePuzzle));
        updateButtons();
    }

    private void resetPuzzle() {
        path.clear();
        visited.clear();
        activatedRequired.clear();
        activatedCores.clear();
        activatedNodes.clear();
        disabledLinkedNodes.clear();
        openedGateGroups.clear();
        completedTimers.clear();
        collapsedEdges.clear();
        branchEdges.clear();
        timerDeadlines.clear();
        elementActivations.clear();
        currentIndex = config.startIndex();
        path.add(currentIndex);
        visited.add(currentIndex);
        activatedNodes.add(currentIndex);
        steps = 0;
        stability = config.startingStability();
        strength = config.startingStrength();
        qiType = LingqiTrajectoryConfig.QiType.NORMAL;
        splitQiType = qiType;
        splitStrength = strength;
        firstBranchCore = -1;
        firstBranchStrength = -1;
        expectedOrder = 0;
        lastDx = 0;
        lastDy = 0;
        optionalScore = 0;
        mustTurn = false;
        orderBroken = false;
        failed = false;
        ready = false;
        submitted = false;
        branchComplete = config.branch() == null;
        completedTicks = 0;
        status = Component.translatable(PREFIX + "status.guide");
        updateButtons();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != 0 || submitted || ready || failed) {
            return false;
        }
        int index = nodeAt(mouseX, mouseY);
        if (index < 0) {
            return false;
        }
        extendPath(index);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE && !submitted && !ready && !failed) {
            waitAction();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void extendPath(int index) {
        if (!config.adjacent(currentIndex, index)) {
            status = Component.translatable(PREFIX + "status.adjacent");
            return;
        }

        LingqiTrajectoryConfig.BranchDefinition branch = config.branch();
        if (branch != null && branch.isBranchCore(index) && currentIndex != branch.splitNode()) {
            status = Component.translatable(PREFIX + "status.branch_entry");
            return;
        }
        if (branch != null && currentIndex == branch.splitNode() && !branchComplete
                && !branch.isBranchCore(index)) {
            status = Component.translatable(PREFIX + "status.branch_missing");
            return;
        }

        LingqiTrajectoryConfig.Edge edge = config.edge(currentIndex, index);
        if (collapsedEdges.contains(edge)) {
            status = Component.translatable(PREFIX + "status.collapsed");
            return;
        }
        LingqiTrajectoryConfig.DirectedEdge directional = config.directionalRule(currentIndex, index);
        if (directional != null && (directional.from() != currentIndex || directional.to() != index)) {
            changeStability(-10);
            status = Component.translatable(PREFIX + "status.directional");
            checkStabilityFailure();
            return;
        }
        if (!canLeaveRotatingNode(index)) {
            status = Component.translatable(PREFIX + "status.rotating");
            return;
        }

        LingqiTrajectoryConfig.Node node = config.node(index);
        if (node.type() == LingqiTrajectoryConfig.NodeType.BLOCKED) {
            changeStability(-14);
            status = Component.translatable(PREFIX + "status.blocked");
            checkStabilityFailure();
            return;
        }
        if (disabledLinkedNodes.contains(index)) {
            status = Component.translatable(PREFIX + "status.linked_blocked");
            return;
        }
        if (node.type() == LingqiTrajectoryConfig.NodeType.GATE
                && !openedGateGroups.contains(node.group())) {
            status = Component.translatable(PREFIX + "status.gate_closed");
            return;
        }
        if (node.type() == LingqiTrajectoryConfig.NodeType.QI_CHANNEL && node.qiType() != qiType) {
            status = Component.translatable(PREFIX + "status.qi_mismatch", qiName(node.qiType()));
            return;
        }
        if (isHazardAt(index, steps)) {
            status = Component.translatable(PREFIX + "status.hazard_blocked");
            return;
        }
        if (visited.contains(index)) {
            changeStability(-16);
            status = Component.translatable(PREFIX + "status.revisited");
            checkStabilityFailure();
            return;
        }

        int previous = currentIndex;
        int dx = config.column(index) - config.column(previous);
        int dy = config.row(index) - config.row(previous);
        if (mustTurn) {
            if (dx == lastDx && dy == lastDy) {
                changeStability(-22);
                status = Component.translatable(PREFIX + "status.reverse_failed");
            }
            mustTurn = false;
        }

        currentIndex = index;
        path.add(index);
        visited.add(index);
        steps++;
        applyStepPressure();
        if (failed) {
            return;
        }
        if (config.collapsingEdges().contains(edge)) {
            collapsedEdges.add(edge);
        }

        activateNode(node, index, previous);
        if (failed) {
            return;
        }
        if (currentIndex == index) {
            lastDx = dx;
            lastDy = dy;
        }
        checkTimedDeadlines();
        checkHazardCollision();
        checkStabilityFailure();
        if (!failed && index == config.finalCoreIndex()) {
            evaluateCore();
        }
        updateButtons();
    }

    private boolean canLeaveRotatingNode(int destination) {
        LingqiTrajectoryConfig.Node current = config.node(currentIndex);
        if (current.type() != LingqiTrajectoryConfig.NodeType.ROTATING) {
            return true;
        }
        int direction = Math.floorMod(current.rotation() + steps, 4);
        int dx = config.column(destination) - config.column(currentIndex);
        int dy = config.row(destination) - config.row(currentIndex);
        return switch (direction) {
            case 0 -> dx == 0 && dy == -1;
            case 1 -> dx == 1 && dy == 0;
            case 2 -> dx == 0 && dy == 1;
            default -> dx == -1 && dy == 0;
        };
    }

    private void waitAction() {
        if (submitted || ready || failed) {
            return;
        }
        steps++;
        status = Component.translatable(PREFIX + "status.wait");
        applyStepPressure();
        if (!failed) {
            checkTimedDeadlines();
            checkHazardCollision();
            checkStabilityFailure();
        }
        updateButtons();
    }

    private void applyStepPressure() {
        if (steps > config.idealSteps()) {
            changeStability(-7);
            status = Component.translatable(PREFIX + "status.over_ideal");
        }
        if (steps > config.maximumSteps()) {
            fail(PREFIX + "status.too_many_steps");
        }
    }

    private void activateNode(LingqiTrajectoryConfig.Node node, int index, int previous) {
        activatedNodes.add(index);
        switch (node.type()) {
            case ELEMENT -> activateElementNode(node, index);
            case GATHER -> {
                strength = Mth.clamp(strength + node.amount(), 0, 150);
                if (!node.required()) optionalScore++;
                status = Component.translatable(PREFIX + "status.gathered", node.amount());
            }
            case DRAIN -> {
                strength = Math.max(0, strength - node.amount());
                status = Component.translatable(PREFIX + "status.drained", node.amount());
            }
            case STABILIZE -> {
                changeStability(node.amount());
                if (!node.required()) optionalScore++;
                status = Component.translatable(PREFIX + "status.stabilized", node.amount());
            }
            case SPLIT -> activateSplit(index);
            case REVERSE -> {
                changeStability(-Math.max(0, node.amount()));
                mustTurn = true;
                status = Component.translatable(PREFIX + "status.reverse");
            }
            case UNSTABLE -> {
                changeStability(-node.amount());
                status = Component.translatable(PREFIX + "status.unstable", node.amount());
            }
            case CONVERTER -> {
                qiType = node.qiType();
                status = Component.translatable(PREFIX + "status.converted", qiName(qiType));
            }
            case QI_CHANNEL -> status = Component.translatable(
                    PREFIX + "status.channel", qiName(node.qiType()));
            case TIMED -> activateTimer(node);
            case TIMED_TARGET -> completeTimer(node);
            case LINKED -> activateLinked(index);
            case SWITCH -> {
                openedGateGroups.add(node.group());
                status = Component.translatable(PREFIX + "status.switch_open");
            }
            case GATE -> status = Component.translatable(PREFIX + "status.gate_passed");
            case SUB_CORE -> activateSubCore(index);
            case CORE -> {
                activatedCores.add(index);
                status = Component.translatable(PREFIX + "status.core");
            }
            case FALSE_CORE -> activateFalseCore(node, index, previous);
            case ROTATING -> status = Component.translatable(PREFIX + "status.rotating_entered");
            default -> status = Component.translatable(PREFIX + "status.flowing");
        }
    }

    private void activateElementNode(LingqiTrajectoryConfig.Node node, int index) {
        int count = elementActivations.merge(node.element(), 1, Integer::sum);
        int requiredCount = requiredElementCount(node.element());
        if (count > requiredCount) {
            fail(PREFIX + "status.element_overload");
            return;
        }
        if (node.required()) {
            if (config.strictOrder() && node.order() != expectedOrder) {
                orderBroken = true;
                changeStability(-28);
                fail(PREFIX + "status.wrong_order");
                return;
            }
            activatedRequired.add(index);
            if (config.strictOrder()) {
                expectedOrder++;
            }
        }
        status = Component.translatable(PREFIX + "status.element", elementName(node.element()));
    }

    private int requiredElementCount(String element) {
        int count = 0;
        for (String required : config.requiredElements()) {
            if (required.equals(element)) count++;
        }
        return count;
    }

    private void activateSplit(int index) {
        if (config.branch() != null && config.branch().splitNode() == index && firstBranchCore < 0) {
            splitStrength = strength;
            splitQiType = qiType;
        }
        status = Component.translatable(PREFIX + "status.split");
    }

    private void activateSubCore(int index) {
        activatedCores.add(index);
        LingqiTrajectoryConfig.BranchDefinition branch = config.branch();
        if (branch == null || !branch.isBranchCore(index)) {
            status = Component.translatable(PREFIX + "status.core");
            return;
        }

        branchEdges.add(config.edge(branch.splitNode(), index));
        if (firstBranchCore < 0) {
            firstBranchCore = index;
            firstBranchStrength = strength;
            status = Component.translatable(PREFIX + "status.branch_first", strength);
            returnToSplit(branch);
            return;
        }
        int difference = Math.abs(firstBranchStrength - strength);
        if (difference > branch.balanceTolerance()) {
            fail(PREFIX + "status.branch_unbalanced");
            return;
        }
        branchComplete = true;
        status = Component.translatable(PREFIX + "status.branch_complete", difference);
        returnToSplit(branch);
    }

    private void returnToSplit(LingqiTrajectoryConfig.BranchDefinition branch) {
        path.add(branch.splitNode());
        currentIndex = branch.splitNode();
        strength = splitStrength;
        qiType = splitQiType;
        lastDx = 0;
        lastDy = 0;
        mustTurn = false;
    }

    private void activateTimer(LingqiTrajectoryConfig.Node node) {
        timerDeadlines.put(node.group(), steps + node.amount());
        completedTimers.remove(node.group());
        status = Component.translatable(PREFIX + "status.timer_started", node.amount());
    }

    private void completeTimer(LingqiTrajectoryConfig.Node node) {
        Integer deadline = timerDeadlines.get(node.group());
        if (deadline == null || steps > deadline) {
            fail(PREFIX + "status.timer_inactive");
            return;
        }
        completedTimers.add(node.group());
        timerDeadlines.remove(node.group());
        status = Component.translatable(PREFIX + "status.timer_complete");
    }

    private void checkTimedDeadlines() {
        for (Map.Entry<Integer, Integer> entry : List.copyOf(timerDeadlines.entrySet())) {
            if (steps > entry.getValue()) {
                fail(PREFIX + "status.timer_expired");
                return;
            }
        }
    }

    private void activateLinked(int index) {
        for (LingqiTrajectoryConfig.LinkedPair pair : config.linkedPairs()) {
            if (pair.first() == index || pair.second() == index) {
                disabledLinkedNodes.add(pair.other(index));
                status = Component.translatable(PREFIX + "status.linked");
                return;
            }
        }
        status = Component.translatable(PREFIX + "status.flowing");
    }

    private void activateFalseCore(LingqiTrajectoryConfig.Node node, int index, int previous) {
        changeStability(-Math.max(1, node.amount()));
        currentIndex = previous;
        path.add(previous);
        lastDx = 0;
        lastDy = 0;
        status = Component.translatable(PREFIX + "status.false_core", node.amount());
    }

    private void checkHazardCollision() {
        if (isHazardAt(currentIndex, steps)) {
            changeStability(-25);
            status = Component.translatable(PREFIX + "status.hazard_hit");
        }
    }

    private boolean isHazardAt(int index, int action) {
        for (LingqiTrajectoryConfig.MovingHazard hazard : config.movingHazards()) {
            if (hazard.positionAt(action) == index) return true;
        }
        return false;
    }

    private void evaluateCore() {
        if (orderBroken || !exactElementCounts() || activatedRequired.size() < config.requiredNodes().size()) {
            fail(PREFIX + "status.missing_nodes");
            return;
        }
        if (!activatedCores.containsAll(config.coreNodes())) {
            fail(PREFIX + "status.missing_cores");
            return;
        }
        if (!allRequiredSystemsActivated()) {
            fail(PREFIX + "status.missing_systems");
            return;
        }
        if (completedTimers.size() < config.timedRequirements().size()) {
            fail(PREFIX + "status.missing_timers");
            return;
        }
        if (!branchComplete) {
            fail(PREFIX + "status.branch_missing");
            return;
        }
        if (strength < config.minimumStrength()) {
            fail(PREFIX + "status.weak_qi");
            return;
        }
        if (strength > config.maximumStrength()) {
            fail(PREFIX + "status.overloaded_qi");
            return;
        }
        ready = true;
        status = Component.translatable(PREFIX + "status.ready");
        updateButtons();
    }

    private boolean exactElementCounts() {
        Map<String, Integer> requiredCounts = new HashMap<>();
        for (String element : config.requiredElements()) {
            requiredCounts.merge(element, 1, Integer::sum);
        }
        for (Map.Entry<String, Integer> entry : requiredCounts.entrySet()) {
            if (elementActivations.getOrDefault(entry.getKey(), 0).intValue() != entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    private boolean allRequiredSystemsActivated() {
        for (int index = 0; index < config.nodes().size(); index++) {
            LingqiTrajectoryConfig.Node node = config.node(index);
            if (node.required() && !activatedNodes.contains(index)) {
                return false;
            }
        }
        return true;
    }

    private int requiredSystemCount() {
        int count = 0;
        for (LingqiTrajectoryConfig.Node node : config.nodes()) {
            if (node.required() && node.type() != LingqiTrajectoryConfig.NodeType.ELEMENT
                    && node.type() != LingqiTrajectoryConfig.NodeType.CORE
                    && node.type() != LingqiTrajectoryConfig.NodeType.SUB_CORE) {
                count++;
            }
        }
        return count;
    }

    private int activatedSystemCount() {
        int count = 0;
        for (int index : activatedNodes) {
            LingqiTrajectoryConfig.Node node = config.node(index);
            if (node.required() && node.type() != LingqiTrajectoryConfig.NodeType.ELEMENT
                    && node.type() != LingqiTrajectoryConfig.NodeType.CORE
                    && node.type() != LingqiTrajectoryConfig.NodeType.SUB_CORE) {
                count++;
            }
        }
        return count;
    }

    private void completePuzzle() {
        if (!ready || failed || submitted) {
            return;
        }
        submitted = true;
        status = Component.translatable(PREFIX + "success");
        PacketDistributor.sendToServer(
                new ModPayloads.ServerboundCompleteLearningGamePayload(token, requiredPoints));
        updateButtons();
    }

    private void fail(String translationKey) {
        failed = true;
        ready = false;
        status = Component.translatable(translationKey);
        updateButtons();
    }

    private void changeStability(int amount) {
        stability = Mth.clamp(stability + amount, 0, config.startingStability());
    }

    private void checkStabilityFailure() {
        if (stability <= 0) {
            fail(PREFIX + "status.no_stability");
        }
    }

    private void updateButtons() {
        if (waitButton != null) {
            waitButton.active = !ready && !failed && !submitted;
        }
        if (completeButton != null) {
            completeButton.active = ready && !failed && !submitted;
        }
    }

    @Override
    public void tick() {
        super.tick();
        animationTicks++;
        if (submitted && ++completedTicks >= 24 && minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int panelX = panelX();
        int panelY = panelY();
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        Minecraft mc = Minecraft.getInstance();

        graphics.fill(0, 0, width, height, 0xA0101716);
        CultivationGuiTextures.drawLandscape(graphics, panelX, panelY, panelWidth, panelHeight);
        graphics.drawCenteredString(mc.font, title, panelX + panelWidth / 2,
                panelY + 20, 0xFF38352E);

        drawCenteredFitted(graphics, targetName(), panelX + panelWidth / 2, panelY + 46,
                panelWidth - 30, 0xFF392F24);
        drawLeftRightLine(graphics,
                Component.translatable(PREFIX + "realm", realmName(config.realm())),
                Component.translatable(PREFIX + "stability", stability), panelY + 59,
                0xFF5B4A35, stability > 35 ? 0xFF2D7354 : 0xFF9A342C);
        drawLeftRightLine(graphics,
                Component.translatable(PREFIX + "steps", steps, config.maximumSteps()),
                Component.translatable(PREFIX + "strength_range", strength,
                        config.minimumStrength(), config.maximumStrength()),
                panelY + 71, 0xFF5B4A35, strengthInRange() ? 0xFF2D7354 : 0xFF9A342C);
        drawLeftRightLine(graphics,
                Component.translatable(PREFIX + "qi_current", qiName(qiType)),
                Component.translatable(PREFIX + "optional_score", optionalScore),
                panelY + 83, qiColor(qiType), 0xFF6E5533);
        drawCenteredFitted(graphics,
                Component.translatable(PREFIX + "requirements", requiredNodesText()),
                panelX + panelWidth / 2, panelY + 95, panelWidth - 24, 0xFF6E5533);

        renderGrid(graphics, mouseX, mouseY, partialTick);
        drawCenteredFitted(graphics, status, panelX + panelWidth / 2,
                panelY + panelHeight - 43, panelWidth - 26,
                failed ? 0xFF9A342C : ready || submitted ? 0xFF28704A : 0xFF5B4A35);

        super.render(graphics, mouseX, mouseY, partialTick);
        int hovered = nodeAt(mouseX, mouseY);
        if (hovered >= 0) {
            graphics.renderTooltip(mc.font, nodeTooltip(config.node(hovered), hovered), mouseX, mouseY);
        }
    }

    private void drawLeftRightLine(GuiGraphics graphics, Component left, Component right, int y,
                                   int leftColor, int rightColor) {
        Minecraft mc = Minecraft.getInstance();
        graphics.drawString(mc.font, left, panelX() + HEADER_SIDE_INSET, y, leftColor, false);
        graphics.drawString(mc.font, right,
                panelX() + panelWidth() - HEADER_SIDE_INSET - mc.font.width(right),
                y, rightColor, false);
    }

    private boolean strengthInRange() {
        return strength >= config.minimumStrength() && strength <= config.maximumStrength();
    }

    private void renderGrid(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int pitch = cellPitch();
        int gridX = gridX();
        int gridY = gridY();
        int gridWidth = (config.width() - 1) * pitch;
        int gridHeight = (config.height() - 1) * pitch;
        graphics.renderOutline(gridX - 12, gridY - 12, gridWidth + 25, gridHeight + 25, 0xFF9A7B3F);

        for (int index = 0; index < config.nodes().size(); index++) {
            if (config.node(index).type() == LingqiTrajectoryConfig.NodeType.BLOCKED) {
                continue;
            }
            int column = config.column(index);
            int row = config.row(index);
            if (column + 1 < config.width()) drawBaseConnection(graphics, index, index + 1);
            if (row + 1 < config.height()) drawBaseConnection(graphics, index, index + config.width());
        }

        int pathColor = ((animationTicks / 6) & 1) == 0 ? 0xFFE5BC4A : 0xFFFFDA72;
        for (int i = 1; i < path.size(); i++) {
            if (config.adjacent(path.get(i - 1), path.get(i))) {
                drawConnection(graphics, path.get(i - 1), path.get(i), pathColor, 3);
            }
        }
        for (LingqiTrajectoryConfig.Edge edge : branchEdges) {
            drawConnection(graphics, edge.first(), edge.second(), 0xFFFFD76A, 3);
        }

        for (int index = 0; index < config.nodes().size(); index++) {
            drawNode(graphics, index, index == nodeAt(mouseX, mouseY), partialTick);
        }
        drawHazards(graphics, partialTick);
    }

    private void drawBaseConnection(GuiGraphics graphics, int first, int second) {
        if (config.node(second).type() == LingqiTrajectoryConfig.NodeType.BLOCKED
                || config.node(first).type() == LingqiTrajectoryConfig.NodeType.BLOCKED
                || collapsedEdges.contains(config.edge(first, second))) {
            return;
        }
        int color = connectionColor(first, second);
        drawConnection(graphics, first, second, color, 1);
        LingqiTrajectoryConfig.DirectedEdge directional = config.directionalRule(first, second);
        if (directional != null) {
            drawDirectionArrow(graphics, directional);
        }
    }

    private int connectionColor(int first, int second) {
        LingqiTrajectoryConfig.Node firstNode = config.node(first);
        LingqiTrajectoryConfig.Node secondNode = config.node(second);
        if (firstNode.type() == LingqiTrajectoryConfig.NodeType.QI_CHANNEL) return qiColor(firstNode.qiType());
        if (secondNode.type() == LingqiTrajectoryConfig.NodeType.QI_CHANNEL) return qiColor(secondNode.qiType());
        if (secondNode.type() == LingqiTrajectoryConfig.NodeType.GATE
                && !openedGateGroups.contains(secondNode.group())) return 0xFF473E39;
        return 0xFF6D6B48;
    }

    private void drawDirectionArrow(GuiGraphics graphics, LingqiTrajectoryConfig.DirectedEdge edge) {
        int firstX = gridX() + config.column(edge.from()) * cellPitch();
        int firstY = gridY() + config.row(edge.from()) * cellPitch();
        int secondX = gridX() + config.column(edge.to()) * cellPitch();
        int secondY = gridY() + config.row(edge.to()) * cellPitch();
        String arrow;
        if (secondX > firstX) arrow = ">";
        else if (secondX < firstX) arrow = "<";
        else if (secondY > firstY) arrow = "v";
        else arrow = "^";
        graphics.drawCenteredString(Minecraft.getInstance().font, arrow,
                (firstX + secondX) / 2, (firstY + secondY) / 2 - 4, 0xFFFFD76A);
    }

    private void drawConnection(GuiGraphics graphics, int first, int second, int color, int thickness) {
        int firstX = gridX() + config.column(first) * cellPitch();
        int firstY = gridY() + config.row(first) * cellPitch();
        int secondX = gridX() + config.column(second) * cellPitch();
        int secondY = gridY() + config.row(second) * cellPitch();
        if (firstY == secondY) {
            graphics.fill(Math.min(firstX, secondX), firstY - thickness / 2,
                    Math.max(firstX, secondX) + 1, firstY + (thickness + 1) / 2, color);
        } else {
            graphics.fill(firstX - thickness / 2, Math.min(firstY, secondY),
                    firstX + (thickness + 1) / 2, Math.max(firstY, secondY) + 1, color);
        }
    }

    private void drawNode(GuiGraphics graphics, int index, boolean hovered, float partialTick) {
        LingqiTrajectoryConfig.Node node = config.node(index);
        int centerX = gridX() + config.column(index) * cellPitch();
        int centerY = gridY() + config.row(index) * cellPitch();
        int color = nodeColor(node, index);
        boolean active = index == currentIndex;
        boolean used = visited.contains(index) || activatedNodes.contains(index);
        int half = node.type() == LingqiTrajectoryConfig.NodeType.CORE
                || node.type() == LingqiTrajectoryConfig.NodeType.FALSE_CORE ? 7 : 6;

        if (active) {
            float pulse = 0.5F + 0.5F * Mth.sin((animationTicks + partialTick) * 0.25F);
            int glow = pulse > 0.5F ? 0xFFFFE18A : 0xFFE0B23D;
            graphics.renderOutline(centerX - half - 3, centerY - half - 3,
                    half * 2 + 7, half * 2 + 7, glow);
        }
        if (hovered) {
            graphics.renderOutline(centerX - half - 2, centerY - half - 2,
                    half * 2 + 5, half * 2 + 5, 0xFFFFFFFF);
        }

        int outline = used ? 0xFFFFD66A : disabledLinkedNodes.contains(index) ? 0xFF602D36 : 0xFF171E1B;
        graphics.fill(centerX - half + 1, centerY - half, centerX + half, centerY + half + 1, outline);
        graphics.fill(centerX - half, centerY - half + 1, centerX + half + 1, centerY + half, outline);
        graphics.fill(centerX - half + 2, centerY - half + 1, centerX + half - 1, centerY + half, color);
        graphics.fill(centerX - half + 1, centerY - half + 2, centerX + half, centerY + half - 1, color);

        Component symbol = nodeSymbol(node);
        int symbolColor = node.type() == LingqiTrajectoryConfig.NodeType.BLOCKED ? 0xFFFFB4A8 : 0xFFF9F1D5;
        graphics.drawCenteredString(Minecraft.getInstance().font, symbol, centerX, centerY - 4, symbolColor);
    }

    private void drawHazards(GuiGraphics graphics, float partialTick) {
        for (LingqiTrajectoryConfig.MovingHazard hazard : config.movingHazards()) {
            int index = hazard.positionAt(steps);
            if (index < 0) continue;
            int centerX = gridX() + config.column(index) * cellPitch();
            int centerY = gridY() + config.row(index) * cellPitch();
            float pulse = 0.5F + 0.5F * Mth.sin((animationTicks + partialTick) * 0.4F);
            int color = pulse > 0.5F ? 0xFFFF4E45 : 0xFFB62528;
            graphics.renderOutline(centerX - 8, centerY - 8, 17, 17, color);
            graphics.drawCenteredString(Minecraft.getInstance().font, "!", centerX, centerY - 4, color);
        }
    }

    private int nodeColor(LingqiTrajectoryConfig.Node node, int index) {
        if (disabledLinkedNodes.contains(index)) return 0xFF49333A;
        return switch (node.type()) {
            case START -> 0xFF2B9B68;
            case CORE -> 0xFFC48B24;
            case SUB_CORE -> 0xFFB69239;
            case FALSE_CORE -> 0xFF7A622B;
            case BLOCKED -> 0xFF5A2D2B;
            case ELEMENT -> elementColor(node.element());
            case GATHER -> 0xFF297F72;
            case DRAIN -> 0xFF70543D;
            case STABILIZE -> 0xFF477EAA;
            case SPLIT -> 0xFF8A5AA4;
            case REVERSE -> 0xFF9A6A34;
            case UNSTABLE -> 0xFF9A3A3A;
            case CONVERTER, QI_CHANNEL, LINKED -> qiColor(node.qiType());
            case TIMED, TIMED_TARGET -> 0xFF8C6FAD;
            case ROTATING -> 0xFF3F8B91;
            case SWITCH -> 0xFF3A8F60;
            case GATE -> openedGateGroups.contains(node.group()) ? 0xFF3A8F60 : 0xFF55443C;
            default -> 0xFF56655B;
        };
    }

    private Component nodeSymbol(LingqiTrajectoryConfig.Node node) {
        if (node.type() == LingqiTrajectoryConfig.NodeType.ROTATING) {
            return Component.literal(switch (Math.floorMod(node.rotation() + steps, 4)) {
                case 0 -> "^";
                case 1 -> ">";
                case 2 -> "v";
                default -> "<";
            });
        }
        String key = switch (node.type()) {
            case START -> "start";
            case CORE -> "core";
            case SUB_CORE -> "sub_core";
            case FALSE_CORE -> "false_core";
            case BLOCKED -> "blocked";
            case GATHER -> "gather";
            case DRAIN -> "drain";
            case STABILIZE -> "stabilize";
            case SPLIT -> "split";
            case REVERSE -> "reverse";
            case UNSTABLE -> "unstable";
            case CONVERTER -> "converter";
            case QI_CHANNEL -> "qi_channel";
            case TIMED -> "timed";
            case TIMED_TARGET -> "timed_target";
            case LINKED -> "linked";
            case SWITCH -> "switch";
            case GATE -> "gate";
            case ELEMENT -> "element." + elementKey(node.element());
            default -> "normal";
        };
        return Component.translatable(PREFIX + "symbol." + key);
    }

    private Component nodeTooltip(LingqiTrajectoryConfig.Node node, int index) {
        if (isHazardAt(index, steps)) {
            return Component.translatable(PREFIX + "node.moving_hazard");
        }
        if (node.type() == LingqiTrajectoryConfig.NodeType.ELEMENT) {
            if (config.strictOrder() && node.order() >= 0) {
                return Component.translatable(PREFIX + "node.element_ordered",
                        node.order() + 1, elementName(node.element()));
            }
            return Component.translatable(PREFIX + "node.element", elementName(node.element()));
        }
        return switch (node.type()) {
            case GATHER, DRAIN, STABILIZE, UNSTABLE, FALSE_CORE -> Component.translatable(
                    PREFIX + "node." + node.type().name().toLowerCase(Locale.ROOT), node.amount());
            case CONVERTER, QI_CHANNEL, LINKED -> Component.translatable(
                    PREFIX + "node." + node.type().name().toLowerCase(Locale.ROOT), qiName(node.qiType()));
            case TIMED -> Component.translatable(PREFIX + "node.timed", node.amount());
            case ROTATING -> Component.translatable(PREFIX + "node.rotating",
                    nodeSymbol(node));
            default -> Component.translatable(PREFIX + "node." + node.type().name().toLowerCase(Locale.ROOT));
        };
    }

    private Component requiredNodesText() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String element : config.requiredElements()) {
            counts.merge(element, 1, Integer::sum);
        }
        MutableComponent text = Component.empty();
        int index = 0;
        if (counts.isEmpty()) {
            text.append(Component.translatable(PREFIX + "none"));
        }
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (index++ > 0) text.append(Component.literal("  "));
            text.append(elementName(entry.getKey()));
            text.append(Component.literal(" " + elementActivations.getOrDefault(entry.getKey(), 0)
                    + "/" + entry.getValue()));
        }
        if (config.strictOrder() && !counts.isEmpty()) {
            text.append(Component.literal("  ")).append(Component.translatable(PREFIX + "ordered"));
        }
        text.append(Component.literal("  ")).append(Component.translatable(
                PREFIX + "requirement.strength_range", strength,
                config.minimumStrength(), config.maximumStrength()));
        text.append(Component.literal("  ")).append(Component.translatable(
                PREFIX + "requirement.cores", activatedCores.size(), config.coreNodes().size()));
        int systemCount = requiredSystemCount();
        if (systemCount > 0) {
            text.append(Component.literal("  ")).append(Component.translatable(
                    PREFIX + "requirement.systems", activatedSystemCount(), systemCount));
        }
        if (!config.timedRequirements().isEmpty()) {
            text.append(Component.literal("  ")).append(Component.translatable(
                    PREFIX + "requirement.timers", completedTimers.size(), config.timedRequirements().size()));
            int remaining = timerDeadlines.values().stream()
                    .mapToInt(deadline -> Math.max(0, deadline - steps))
                    .min()
                    .orElse(-1);
            if (remaining >= 0) {
                text.append(Component.literal(" ")).append(Component.translatable(
                        PREFIX + "requirement.timer_active", remaining));
            }
        }
        if (config.branch() != null) {
            text.append(Component.literal("  ")).append(Component.translatable(
                    PREFIX + (branchComplete ? "requirement.branch_done" : "requirement.branch")));
        }
        return text;
    }

    private Component targetName() {
        if ("spell".equals(learningType)) {
            ModSpells.SpellDef spell = ModSpells.get(targetId);
            return spell == null ? Component.literal(targetId) : spell.name();
        }
        CultivationMethods.MethodDef method = CultivationMethods.get(targetId);
        return method == null ? Component.literal(targetId) : Component.translatable(method.nameKey());
    }

    private static Component realmName(String realm) {
        String key;
        if (CultivationLevels.REALM_ZHUJI.equals(realm)) key = "zhuji";
        else if (CultivationLevels.REALM_JINDAN.equals(realm)) key = "jindan";
        else if (CultivationLevels.REALM_YUANYING.equals(realm)) key = "yuanying";
        else if (CultivationLevels.REALM_HUASHEN.equals(realm)) key = "huashen";
        else if (CultivationLevels.REALM_LIANXU.equals(realm)) key = "lianxu";
        else if (CultivationLevels.REALM_HETI.equals(realm)) key = "heti";
        else if (CultivationLevels.REALM_DACHENG.equals(realm)) key = "dacheng";
        else if (CultivationLevels.REALM_DUJIE.equals(realm)) key = "dujie";
        else key = "lianqi";
        return Component.translatable(PREFIX + "realm." + key);
    }

    private static Component elementName(String element) {
        return Component.translatable(PREFIX + "element." + elementKey(element));
    }

    private static Component qiName(LingqiTrajectoryConfig.QiType type) {
        return Component.translatable(PREFIX + "qi." + type.name().toLowerCase(Locale.ROOT));
    }

    private static int qiColor(LingqiTrajectoryConfig.QiType type) {
        return switch (type) {
            case FIRE -> 0xFFCA4938;
            case WATER -> 0xFF3E82C4;
            case THUNDER -> 0xFF8063D1;
            case YIN -> 0xFF6B4A78;
            case YANG -> 0xFFD5AE3C;
            default -> 0xFF74AFA1;
        };
    }

    private static int elementColor(String element) {
        if (SpiritRoots.METAL.equals(element)) return 0xFF8E9AA3;
        if (SpiritRoots.WOOD.equals(element)) return 0xFF3F8A49;
        if (SpiritRoots.WATER.equals(element)) return 0xFF3476B9;
        if (SpiritRoots.FIRE.equals(element)) return 0xFFB94432;
        if (SpiritRoots.EARTH.equals(element)) return 0xFFA87932;
        if (SpiritRoots.THUNDER.equals(element)) return 0xFF6551B8;
        if (SpiritRoots.ICE.equals(element)) return 0xFF6CB9CC;
        if (SpiritRoots.WIND.equals(element)) return 0xFF3C9B8C;
        if (SpiritRoots.YANG.equals(element)) return 0xFFD5AE3C;
        if (SpiritRoots.YIN.equals(element)) return 0xFF59405F;
        return 0xFF56655B;
    }

    private static String elementKey(String element) {
        if (SpiritRoots.METAL.equals(element)) return "metal";
        if (SpiritRoots.WOOD.equals(element)) return "wood";
        if (SpiritRoots.WATER.equals(element)) return "water";
        if (SpiritRoots.FIRE.equals(element)) return "fire";
        if (SpiritRoots.EARTH.equals(element)) return "earth";
        if (SpiritRoots.THUNDER.equals(element)) return "thunder";
        if (SpiritRoots.ICE.equals(element)) return "ice";
        if (SpiritRoots.WIND.equals(element)) return "wind";
        if (SpiritRoots.YANG.equals(element)) return "yang";
        if (SpiritRoots.YIN.equals(element)) return "yin";
        return "none";
    }

    private void drawCenteredFitted(GuiGraphics graphics, Component text, int centerX, int y,
                                    int availableWidth, int color) {
        int textWidth = Minecraft.getInstance().font.width(text);
        if (textWidth <= availableWidth) {
            graphics.drawCenteredString(Minecraft.getInstance().font, text, centerX, y, color);
            return;
        }
        float scale = availableWidth / (float) textWidth;
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawCenteredString(Minecraft.getInstance().font, text, 0, 0, color);
        graphics.pose().popPose();
    }

    private int nodeAt(double mouseX, double mouseY) {
        int pitch = cellPitch();
        for (int index = 0; index < config.nodes().size(); index++) {
            int centerX = gridX() + config.column(index) * pitch;
            int centerY = gridY() + config.row(index) * pitch;
            if (Math.abs(mouseX - centerX) <= 8 && Math.abs(mouseY - centerY) <= 8) {
                return index;
            }
        }
        return -1;
    }

    private int panelWidth() {
        return Math.min(PANEL_WIDTH, Math.max(300, width - 12));
    }

    private int panelHeight() {
        return Math.min(PANEL_HEIGHT, Math.max(278, height - 12));
    }

    private int panelX() {
        return (width - panelWidth()) / 2;
    }

    private int panelY() {
        return (height - panelHeight()) / 2;
    }

    private int cellPitch() {
        return Math.max(16, Math.min(MAX_CELL_PITCH,
                Math.min((panelWidth() - 44) / config.width(),
                        (panelHeight() - 154) / config.height())));
    }

    private int gridX() {
        int gridWidth = (config.width() - 1) * cellPitch();
        return panelX() + (panelWidth() - gridWidth) / 2;
    }

    private int gridY() {
        int gridHeight = (config.height() - 1) * cellPitch();
        return panelY() + 100 + (172 - gridHeight) / 2;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
