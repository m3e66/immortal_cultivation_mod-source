package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.block.RestrictionTableBlockEntity;
import com.example.immortal_cultivation_mod.item.RestrictionPuzzleLogic;
import com.example.immortal_cultivation_mod.network.RestrictionPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class RestrictionPuzzleScreen extends Screen {
    private static final int PANEL_WIDTH = 430;
    private static final int PANEL_HEIGHT = 343;
    private static final int ABACUS_TOP = 108;
    private static final int ANIMATION_TICKS = 8;

    private static final int WOOD_DARK = 0xFF160D09;
    private static final int WOOD = 0xFF2E1B10;
    private static final int WOOD_LIGHT = 0xFF51331D;
    private static final int GOLD_DARK = 0xFF705426;
    private static final int GOLD = 0xFFE0B84F;
    private static final int GOLD_BRIGHT = 0xFFFFE398;
    private static final int JADE_DARK = 0xFF155849;
    private static final int JADE = 0xFF3CA987;
    private static final int JADE_LIGHT = 0xFF9DE5C8;
    private static final int INK = 0xFF1C1712;
    private static final int IVORY = 0xFFF3E8C8;
    private static final int DANGER = 0xFFD05B43;

    private final BlockPos tablePos;
    private final long token;
    private final int firstLayer;
    private final int attemptedLayers;
    private final RestrictionPuzzleLogic.Difficulty difficulty;
    private final int moveLimit;
    private final int instabilityInterval;
    private final ItemStack artifact;
    private final int[] targets;
    private final int[] flags;
    private final List<RestrictionPuzzleLogic.Link> links;

    private int[] values;
    private int[] previousValues;
    private int movesUsed;
    private int selectedNode;
    private int animationTicks = ANIMATION_TICKS;
    private int statusTicks;
    private int finishedTicks;
    private int visualTicks;
    private int hintsRemaining;
    private boolean waitingForServer;
    private boolean completed;
    private boolean failed;
    private Component status = Component.empty();
    private CarvedButton moveLeftButton;
    private CarvedButton moveRightButton;
    private CarvedButton hintButton;
    private CarvedButton withdrawButton;

    public RestrictionPuzzleScreen(BlockPos tablePos, long token, int firstLayer,
                                   int attemptedLayers, int difficulty, int moveLimit,
                                   int instabilityInterval, ResourceLocation artifactId,
                                   int hintsRemaining,
                                   List<Integer> values, List<Integer> targets,
                                   List<Integer> flags, List<Integer> flattenedLinks) {
        super(Component.translatable("screen." + ImmortalCultivationMod.MODID
                + ".restriction_puzzle.title"));
        this.tablePos = tablePos;
        this.token = token;
        this.firstLayer = Math.max(1, firstLayer);
        this.attemptedLayers = Math.max(1, attemptedLayers);
        RestrictionPuzzleLogic.Difficulty[] difficulties = RestrictionPuzzleLogic.Difficulty.values();
        this.difficulty = difficulties[Mth.clamp(difficulty, 0, difficulties.length - 1)];
        this.moveLimit = Math.max(0, moveLimit);
        this.instabilityInterval = Math.max(0, instabilityInterval);
        this.hintsRemaining = Math.max(0, hintsRemaining);
        Item item = BuiltInRegistries.ITEM.get(artifactId);
        this.artifact = item == null ? ItemStack.EMPTY : new ItemStack(item);

        int size = Math.max(1, Math.min(12, Math.min(values.size(), targets.size())));
        this.values = toArray(values, size, 0);
        this.previousValues = this.values.clone();
        this.targets = toArray(targets, size, 0);
        this.flags = toArray(flags, size, 0);
        this.links = decodeLinks(flattenedLinks, size);
        this.selectedNode = firstMovableNode();
    }

    @Override
    protected void init() {
        int panelX = (width - PANEL_WIDTH) / 2;
        int panelY = (height - PANEL_HEIGHT) / 2;
        moveLeftButton = addRenderableWidget(new CarvedButton(panelX + 81, panelY + 306,
                52, 20, Component.literal("<"), () -> move(-1)));
        moveRightButton = addRenderableWidget(new CarvedButton(panelX + 141, panelY + 306,
                52, 20, Component.literal(">"), () -> move(1)));
        hintButton = addRenderableWidget(new CarvedButton(panelX + 201, panelY + 306,
                64, 20, hintMessage(), this::requestHint));
        withdrawButton = addRenderableWidget(new CarvedButton(panelX + 273, panelY + 306,
                76, 20, Component.translatable("screen." + ImmortalCultivationMod.MODID
                + ".restriction_puzzle.withdraw"), this::onClose));
        refreshButtons();
    }

    private void move(int direction) {
        if (waitingForServer || completed || failed || selectedNode < 0
                || selectedNode >= values.length || isFlagged(selectedNode,
                RestrictionPuzzleLogic.FLAG_LOCKED)) {
            return;
        }
        waitingForServer = true;
        refreshButtons();
        PacketDistributor.sendToServer(new RestrictionPayloads.ServerboundPuzzleMove(
                tablePos, token, selectedNode, direction));
    }

    private void requestHint() {
        if (waitingForServer || completed || failed || hintsRemaining <= 0) {
            return;
        }
        waitingForServer = true;
        refreshButtons();
        PacketDistributor.sendToServer(new RestrictionPayloads.ServerboundPuzzleHint(tablePos, token));
    }

    public void applyServerState(long stateToken, List<Integer> newValues, int newMovesUsed,
                                 int result, boolean instabilityShifted,
                                 int hintsRemaining, boolean hintUsed) {
        if (stateToken != token || newValues.size() != values.length) {
            return;
        }
        waitingForServer = false;
        movesUsed = Math.max(0, newMovesUsed);
        this.hintsRemaining = Math.max(0, hintsRemaining);

        if (result == RestrictionPayloads.RESULT_NOT_ENOUGH_QI) {
            setStatus("not_enough_qi", 70);
            refreshButtons();
            return;
        }
        if (result == RestrictionPayloads.RESULT_INVALID_MOVE) {
            setStatus("invalid_move", 55);
            refreshButtons();
            return;
        }
        if (result == RestrictionPayloads.RESULT_NO_HINTS) {
            setStatus("no_hints", 55);
            refreshButtons();
            return;
        }
        if (result == RestrictionPayloads.RESULT_NO_HINT_MOVE) {
            setStatus("no_hint_move", 55);
            refreshButtons();
            return;
        }

        int[] oldValues = values;
        previousValues = oldValues;
        values = toArray(newValues, newValues.size(), 0);
        if (hintUsed) {
            selectCorrectedNode(oldValues, values);
        }
        animationTicks = 0;
        if (result == RestrictionPayloads.RESULT_SOLVED) {
            completed = true;
            setStatus("completed", 80);
        } else if (result == RestrictionPayloads.RESULT_FAILED_MINOR
                || result == RestrictionPayloads.RESULT_FAILED_MAJOR
                || result == RestrictionPayloads.RESULT_FAILED_CATASTROPHIC
                || result == RestrictionPayloads.RESULT_FAILED_LAYER_DAMAGED) {
            failed = true;
            String suffix = switch (result) {
                case RestrictionPayloads.RESULT_FAILED_MAJOR -> "failed_major";
                case RestrictionPayloads.RESULT_FAILED_CATASTROPHIC -> "failed_catastrophic";
                case RestrictionPayloads.RESULT_FAILED_LAYER_DAMAGED -> "failed_layer";
                default -> "failed_minor";
            };
            setStatus(suffix, 100);
        } else if (hintUsed) {
            setStatus("hint_used", 55);
        } else if (instabilityShifted) {
            setStatus("unstable_shift", 55);
        } else {
            status = Component.empty();
            statusTicks = 0;
        }
        refreshButtons();
    }

    private void setStatus(String suffix, int ticks) {
        status = Component.translatable("message." + ImmortalCultivationMod.MODID
                + ".restriction." + suffix);
        statusTicks = ticks;
    }

    private void refreshButtons() {
        boolean movable = !waitingForServer && !completed && !failed && selectedNode >= 0
                && selectedNode < values.length
                && !isFlagged(selectedNode, RestrictionPuzzleLogic.FLAG_LOCKED);
        if (moveLeftButton != null) {
            moveLeftButton.active = movable;
        }
        if (moveRightButton != null) {
            moveRightButton.active = movable;
        }
        if (hintButton != null) {
            hintButton.setMessage(hintMessage());
            hintButton.active = !waitingForServer && !completed && !failed && hintsRemaining > 0;
        }
        if (withdrawButton != null) {
            withdrawButton.active = !completed && !failed;
        }
    }

    private Component hintMessage() {
        return Component.translatable("screen." + ImmortalCultivationMod.MODID
                + ".restriction_puzzle.hint", hintsRemaining);
    }

    private void selectCorrectedNode(int[] oldValues, int[] newValues) {
        for (int node = 0; node < Math.min(oldValues.length, newValues.length); node++) {
            if (oldValues[node] != newValues[node]
                    && Math.abs(newValues[node] - targets[node])
                    < Math.abs(oldValues[node] - targets[node])) {
                selectedNode = node;
                return;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != 0 || completed || failed) {
            return false;
        }
        int panelX = (width - PANEL_WIDTH) / 2;
        int panelY = (height - PANEL_HEIGHT) / 2;
        int spacing = rowSpacing();
        for (int node = 0; node < values.length; node++) {
            int rowY = panelY + ABACUS_TOP + node * spacing;
            if (mouseY < rowY - 7 || mouseY > rowY + 7
                    || mouseX < panelX + 32 || mouseX > panelX + 398) {
                continue;
            }
            selectedNode = node;
            refreshButtons();
            if (isFlagged(node, RestrictionPuzzleLogic.FLAG_LOCKED)) {
                setStatus("locked_node", 45);
                return true;
            }
            int currentX = beadX(panelX, renderedValue(node, 1.0F));
            if (mouseX < currentX - 7) {
                move(-1);
            } else if (mouseX > currentX + 7) {
                move(1);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_A) {
            move(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_D) {
            move(1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_W) {
            selectedNode = Math.floorMod(selectedNode - 1, values.length);
            refreshButtons();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_S) {
            selectedNode = Math.floorMod(selectedNode + 1, values.length);
            refreshButtons();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        super.tick();
        visualTicks++;
        if (animationTicks < ANIMATION_TICKS) {
            animationTicks++;
        }
        if (statusTicks > 0) {
            statusTicks--;
        }
        if ((completed || failed) && ++finishedTicks >= (completed ? 42 : 65)
                && minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    @Override
    public void onClose() {
        if (!completed && !failed) {
            PacketDistributor.sendToServer(new RestrictionPayloads.ServerboundCancelPuzzle(
                    tablePos, token));
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int panelX = (width - PANEL_WIDTH) / 2;
        int panelY = (height - PANEL_HEIGHT) / 2;
        graphics.fill(0, 0, width, height, 0xB5000000);
        CultivationGuiTextures.drawLandscape(graphics, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);
        renderHeader(graphics, panelX, panelY);
        renderArtifactFocus(graphics, panelX + PANEL_WIDTH / 2, panelY + 65, partialTick);
        renderAbacus(graphics, panelX, panelY, partialTick);
        renderFooter(graphics, panelX, panelY);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderStateFlash(graphics, panelX, panelY, partialTick);
        renderNodeTooltip(graphics, panelX, panelY, mouseX, mouseY);
    }

    private void renderWoodPanel(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 6, y - 6, x + PANEL_WIDTH + 6, y + PANEL_HEIGHT + 6, 0xB0000000);
        graphics.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, WOOD_DARK);
        graphics.renderOutline(x, y, PANEL_WIDTH, PANEL_HEIGHT, GOLD_DARK);
        graphics.renderOutline(x + 4, y + 4, PANEL_WIDTH - 8, PANEL_HEIGHT - 8, WOOD_LIGHT);
        for (int strip = 0; strip < 7; strip++) {
            int stripY = y + 5 + strip * 49;
            graphics.fill(x + 5, stripY, x + PANEL_WIDTH - 5, stripY + 1,
                    strip % 2 == 0 ? 0xFF3C2415 : 0xFF25150D);
        }
        drawCornerCarving(graphics, x + 14, y + 14, 1, 1);
        drawCornerCarving(graphics, x + PANEL_WIDTH - 14, y + 14, -1, 1);
        drawCornerCarving(graphics, x + 14, y + PANEL_HEIGHT - 14, 1, -1);
        drawCornerCarving(graphics, x + PANEL_WIDTH - 14, y + PANEL_HEIGHT - 14, -1, -1);
    }

    private void renderHeader(GuiGraphics graphics, int x, int y) {
        graphics.drawCenteredString(font, title, x + PANEL_WIDTH / 2, y + 20, INK);

        Component difficultyText = Component.translatable("screen."
                + ImmortalCultivationMod.MODID + ".restriction_puzzle.difficulty."
                + difficulty.name().toLowerCase());
        graphics.drawString(font, difficultyText, x + 24, y + 49, JADE_DARK, false);
        graphics.drawString(font, Component.translatable("screen."
                        + ImmortalCultivationMod.MODID + ".restriction_puzzle.layers",
                firstLayer, firstLayer + attemptedLayers - 1), x + 24, y + 62, GOLD_DARK, false);
    }

    private void renderArtifactFocus(GuiGraphics graphics, int centerX, int centerY,
                                     float partialTick) {
        int pulse = (int) (3.0D + Math.sin((visualTicks + partialTick) * 0.12D) * 2.0D);
        drawDiamond(graphics, centerX, centerY, 31 + pulse, 0x222ED09E);
        drawCircle(graphics, centerX, centerY, 27, GOLD_DARK);
        drawCircle(graphics, centerX, centerY, 23, JADE_DARK);
        drawCircle(graphics, centerX, centerY, 19, 0xFF102920);
        double rotation = (visualTicks + partialTick) * 0.018D;
        for (int rune = 0; rune < 8; rune++) {
            double angle = rotation + Math.PI * 2.0D * rune / 8.0D;
            int runeX = centerX + (int) Math.round(Math.cos(angle) * 35.0D);
            int runeY = centerY + (int) Math.round(Math.sin(angle) * 35.0D);
            drawDiamond(graphics, runeX, runeY, 2, rune % 2 == 0 ? GOLD : JADE_LIGHT);
        }
        for (int i = 0; i < 4; i++) {
            double angle = rotation * -1.5D + Math.PI * 2.0D * i / 4.0D;
            int outerX = centerX + (int) Math.round(Math.cos(angle) * 43.0D);
            int outerY = centerY + (int) Math.round(Math.sin(angle) * 43.0D);
            drawLine(graphics, centerX, centerY, outerX, outerY, 0x887D6232);
        }
        if (!artifact.isEmpty()) {
            graphics.pose().pushPose();
            graphics.pose().translate(centerX, centerY, 120.0F);
            graphics.pose().scale(1.65F, 1.65F, 1.0F);
            graphics.renderItem(artifact, -8, -8);
            graphics.pose().popPose();
        }
    }

    private void renderAbacus(GuiGraphics graphics, int panelX, int panelY, float partialTick) {
        int top = panelY + ABACUS_TOP - 12;
        int bottom = panelY + ABACUS_TOP + (values.length - 1) * rowSpacing() + 12;
        graphics.renderOutline(panelX + 27, top - 1, 376, bottom - top + 2, GOLD_DARK);
        graphics.renderOutline(panelX + 32, top + 3, 366, bottom - top - 6, WOOD_LIGHT);

        renderFormationLinks(graphics, panelX, panelY, partialTick);
        for (int node = 0; node < values.length; node++) {
            renderNode(graphics, panelX, panelY, node, partialTick);
        }
    }

    private void renderFormationLinks(GuiGraphics graphics, int panelX, int panelY,
                                      float partialTick) {
        float time = visualTicks + partialTick;
        for (int index = 0; index < links.size(); index++) {
            RestrictionPuzzleLogic.Link link = links.get(index);
            int x1 = beadX(panelX, renderedValue(link.first(), partialTick));
            int y1 = rowY(panelY, link.first());
            int x2 = beadX(panelX, renderedValue(link.second(), partialTick));
            int y2 = rowY(panelY, link.second());
            boolean visibleCorrect = nodeShowsCorrect(link.first()) && nodeShowsCorrect(link.second());
            int color = completed || visibleCorrect
                    ? GOLD_BRIGHT
                    : link.polarity() > 0 ? 0xAA9D742E : 0xAA3C9F83;
            drawLine(graphics, x1, y1, x2, y2, color);
            float travel = (time * 0.035F + index * 0.19F) % 1.0F;
            int sparkX = Math.round(Mth.lerp(travel, x1, x2));
            int sparkY = Math.round(Mth.lerp(travel, y1, y2));
            drawDiamond(graphics, sparkX, sparkY, 2,
                    link.polarity() > 0 ? GOLD_BRIGHT : JADE_LIGHT);
            if (link.polarity() < 0) {
                graphics.drawString(font, Component.literal("-"),
                        (x1 + x2) / 2 - 1, (y1 + y2) / 2 - 4, JADE_LIGHT, true);
            }
        }
    }

    private void renderNode(GuiGraphics graphics, int panelX, int panelY, int node,
                            float partialTick) {
        int y = rowY(panelY, node);
        int railStart = railStart(panelX);
        int railEnd = railEnd(panelX);
        boolean selected = node == selectedNode;
        boolean hidden = isFlagged(node, RestrictionPuzzleLogic.FLAG_HIDDEN);
        boolean locked = isFlagged(node, RestrictionPuzzleLogic.FLAG_LOCKED);
        boolean unstable = isFlagged(node, RestrictionPuzzleLogic.FLAG_UNSTABLE);
        boolean correct = nodeShowsCorrect(node);

        graphics.fill(railStart - 5, y - 1, railEnd + 5, y + 2, 0xFF765033);
        graphics.fill(railStart, y, railEnd, y + 1, 0xFFC08A4B);
        int currentX = beadX(panelX, renderedValue(node, partialTick));
        graphics.fill(railStart, y - 1, currentX, y + 2,
                correct ? 0xCCF0C95D : 0xAA2B8C72);
        for (int value = RestrictionPuzzleLogic.MIN_QI;
             value <= RestrictionPuzzleLogic.MAX_QI; value++) {
            int tickX = beadX(panelX, value);
            graphics.fill(tickX, y - 3, tickX + 1, y + 4,
                    value == targets[node] && !hidden ? GOLD : 0xFF5A4934);
        }

        if (!hidden) {
            int targetX = beadX(panelX, targets[node]);
            drawDiamond(graphics, targetX, y - 6, 2, GOLD_BRIGHT);
        }

        int beadColor = correct ? GOLD : JADE;
        if (locked) {
            beadColor = 0xFF5B625C;
        } else if (unstable) {
            double pulse = (Math.sin((visualTicks + partialTick) * 0.25D + node) + 1.0D) * 0.5D;
            beadColor = pulse > 0.5D ? DANGER : JADE;
        }
        if (selected) {
            graphics.renderOutline(currentX - 10, y - 7, 20, 14, GOLD_BRIGHT);
        }
        graphics.fill(currentX - 8, y - 5, currentX + 9, y + 6, JADE_DARK);
        graphics.fill(currentX - 6, y - 4, currentX + 7, y + 4, beadColor);
        graphics.fill(currentX - 5, y - 3, currentX + 5, y - 1,
                locked ? 0xFF8B9289 : JADE_LIGHT);
        graphics.drawCenteredString(font, Integer.toString(values[node]), currentX, y - 4, INK);
        if (locked) {
            graphics.fill(currentX - 3, y - 7, currentX + 4, y - 5, 0xFF252A28);
            graphics.renderOutline(currentX - 4, y - 8, 8, 4, 0xFF929A90);
        }

        graphics.drawString(font, Integer.toString(node + 1), panelX + 42, y - 4,
                selected ? GOLD_BRIGHT : IVORY, true);
        Component targetText = Component.translatable("screen." + ImmortalCultivationMod.MODID
                + ".restriction_puzzle.target", hidden ? "?" : Integer.toString(targets[node]));
        graphics.drawString(font, targetText, panelX + 365, y - 4,
                hidden ? DANGER : GOLD_BRIGHT, true);

        drawTriangle(graphics, panelX + 64, y, false,
                selected && !locked ? JADE_LIGHT : GOLD_DARK);
        drawTriangle(graphics, panelX + 366, y, true,
                selected && !locked ? JADE_LIGHT : GOLD_DARK);
    }

    private void renderFooter(GuiGraphics graphics, int x, int y) {
        graphics.fill(x + 20, y + 286, x + PANEL_WIDTH - 20, y + 300, 0xBB0D241E);
        graphics.renderOutline(x + 19, y + 285, PANEL_WIDTH - 38, 16, GOLD_DARK);
        Component moveText = moveLimit > 0
                ? Component.translatable("screen." + ImmortalCultivationMod.MODID
                + ".restriction_puzzle.moves_limited", movesUsed, moveLimit)
                : Component.translatable("screen." + ImmortalCultivationMod.MODID
                + ".restriction_puzzle.moves", movesUsed);
        graphics.drawString(font, moveText, x + 27, y + 289,
                moveLimit > 0 && movesUsed >= moveLimit - 5 ? DANGER : IVORY, true);
        graphics.drawCenteredString(font, Component.translatable("screen."
                        + ImmortalCultivationMod.MODID + ".restriction_puzzle.cost",
                RestrictionTableBlockEntity.MOVE_QI_COST), x + PANEL_WIDTH / 2, y + 289, GOLD_BRIGHT);
        if (instabilityInterval > 0) {
            graphics.drawString(font, Component.translatable("screen."
                            + ImmortalCultivationMod.MODID + ".restriction_puzzle.instability",
                    instabilityInterval), x + 355, y + 289, DANGER, true);
        }
        if ((statusTicks > 0 || completed || failed) && !status.getString().isBlank()) {
            graphics.drawCenteredString(font, status, x + PANEL_WIDTH / 2, y + 334,
                    completed ? GOLD_BRIGHT : failed ? DANGER : IVORY);
        }
    }

    private void renderStateFlash(GuiGraphics graphics, int x, int y, float partialTick) {
        if (!completed && !failed) {
            return;
        }
        float phase = Mth.clamp((finishedTicks + partialTick) / 40.0F, 0.0F, 1.0F);
        int alpha = (int) ((1.0F - phase) * (completed ? 150 : 115));
        int rgb = completed ? 0x00F5C85B : 0x00B5231C;
        graphics.fill(x + 5, y + 5, x + PANEL_WIDTH - 5, y + PANEL_HEIGHT - 5,
                (alpha << 24) | rgb);
        int radius = 18 + (int) (phase * 145.0F);
        drawCircle(graphics, x + PANEL_WIDTH / 2, y + 174, radius,
                completed ? GOLD_BRIGHT : DANGER);
    }

    private void renderNodeTooltip(GuiGraphics graphics, int panelX, int panelY,
                                   int mouseX, int mouseY) {
        int spacing = rowSpacing();
        for (int node = 0; node < values.length; node++) {
            int y = panelY + ABACUS_TOP + node * spacing;
            if (mouseX < panelX + 32 || mouseX > panelX + 398
                    || mouseY < y - 7 || mouseY > y + 7) {
                continue;
            }
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("screen." + ImmortalCultivationMod.MODID
                    + ".restriction_puzzle.node", node + 1));
            if (isFlagged(node, RestrictionPuzzleLogic.FLAG_LOCKED)) {
                tooltip.add(Component.translatable("screen." + ImmortalCultivationMod.MODID
                        + ".restriction_puzzle.locked"));
            }
            if (isFlagged(node, RestrictionPuzzleLogic.FLAG_UNSTABLE)) {
                tooltip.add(Component.translatable("screen." + ImmortalCultivationMod.MODID
                        + ".restriction_puzzle.unstable"));
            }
            if (isFlagged(node, RestrictionPuzzleLogic.FLAG_HIDDEN)) {
                tooltip.add(Component.translatable("screen." + ImmortalCultivationMod.MODID
                        + ".restriction_puzzle.hidden"));
            }
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            return;
        }
    }

    private float renderedValue(int node, float partialTick) {
        float progress = Mth.clamp((animationTicks + partialTick) / ANIMATION_TICKS, 0.0F, 1.0F);
        progress = progress * progress * (3.0F - 2.0F * progress);
        return Mth.lerp(progress, previousValues[node], values[node]);
    }

    private boolean nodeShowsCorrect(int node) {
        return !isFlagged(node, RestrictionPuzzleLogic.FLAG_HIDDEN)
                && values[node] == targets[node];
    }

    private boolean isFlagged(int node, int flag) {
        return node >= 0 && node < flags.length && (flags[node] & flag) != 0;
    }

    private int firstMovableNode() {
        for (int i = 0; i < flags.length; i++) {
            if (!isFlagged(i, RestrictionPuzzleLogic.FLAG_LOCKED)) {
                return i;
            }
        }
        return 0;
    }

    private int rowSpacing() {
        return values.length <= 8 ? 20 : 15;
    }

    private int rowY(int panelY, int node) {
        return panelY + ABACUS_TOP + node * rowSpacing();
    }

    private static int railStart(int panelX) {
        return panelX + 84;
    }

    private static int railEnd(int panelX) {
        return panelX + 345;
    }

    private static int beadX(int panelX, float value) {
        float progress = Mth.clamp(value, RestrictionPuzzleLogic.MIN_QI,
                RestrictionPuzzleLogic.MAX_QI) / (float) RestrictionPuzzleLogic.MAX_QI;
        return Math.round(Mth.lerp(progress, railStart(panelX), railEnd(panelX)));
    }

    private static int[] toArray(List<Integer> source, int size, int fallback) {
        int[] result = new int[size];
        for (int i = 0; i < size; i++) {
            result[i] = i < source.size() ? source.get(i) : fallback;
        }
        return result;
    }

    private static List<RestrictionPuzzleLogic.Link> decodeLinks(List<Integer> flattened,
                                                                  int nodeCount) {
        List<RestrictionPuzzleLogic.Link> result = new ArrayList<>();
        for (int index = 0; index + 2 < flattened.size(); index += 3) {
            int first = flattened.get(index);
            int second = flattened.get(index + 1);
            int polarity = flattened.get(index + 2);
            if (first >= 0 && first < nodeCount && second >= 0 && second < nodeCount
                    && first != second) {
                result.add(new RestrictionPuzzleLogic.Link(first, second,
                        polarity < 0 ? -1 : 1));
            }
        }
        return List.copyOf(result);
    }

    private static void drawCornerCarving(GuiGraphics graphics, int x, int y,
                                          int xDirection, int yDirection) {
        for (int step = 0; step < 12; step++) {
            int horizontalX = x + step * xDirection;
            int verticalY = y + step * yDirection;
            graphics.fill(Math.min(horizontalX, horizontalX + xDirection),
                    Math.min(y, y + yDirection),
                    Math.max(horizontalX, horizontalX + xDirection) + 1,
                    Math.max(y, y + yDirection) + 1, GOLD_DARK);
            graphics.fill(Math.min(x, x + xDirection),
                    Math.min(verticalY, verticalY + yDirection),
                    Math.max(x, x + xDirection) + 1,
                    Math.max(verticalY, verticalY + yDirection) + 1, GOLD_DARK);
        }
        drawDiamond(graphics, x + 12 * xDirection, y + 12 * yDirection, 3, GOLD);
    }

    private static void drawTriangle(GuiGraphics graphics, int centerX, int centerY,
                                     boolean right, int color) {
        for (int offset = -4; offset <= 4; offset++) {
            int width = 4 - Math.abs(offset);
            if (right) {
                graphics.fill(centerX, centerY + offset,
                        centerX + width + 1, centerY + offset + 1, color);
            } else {
                graphics.fill(centerX - width, centerY + offset,
                        centerX + 1, centerY + offset + 1, color);
            }
        }
    }

    private static void drawLine(GuiGraphics graphics, int x0, int y0, int x1, int y1,
                                 int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int stepX = x0 < x1 ? 1 : -1;
        int stepY = y0 < y1 ? 1 : -1;
        int error = dx - dy;
        while (true) {
            graphics.fill(x0, y0, x0 + 2, y0 + 2, color);
            if (x0 == x1 && y0 == y1) {
                return;
            }
            int twiceError = error * 2;
            if (twiceError > -dy) {
                error -= dy;
                x0 += stepX;
            }
            if (twiceError < dx) {
                error += dx;
                y0 += stepY;
            }
        }
    }

    private static void drawCircle(GuiGraphics graphics, int centerX, int centerY,
                                   int radius, int color) {
        int steps = Math.max(24, radius * 5);
        for (int step = 0; step < steps; step++) {
            double angle = Math.PI * 2.0D * step / steps;
            int x = centerX + (int) Math.round(Math.cos(angle) * radius);
            int y = centerY + (int) Math.round(Math.sin(angle) * radius);
            graphics.fill(x, y, x + 2, y + 2, color);
        }
    }

    private static void drawDiamond(GuiGraphics graphics, int centerX, int centerY,
                                    int radius, int color) {
        for (int y = -radius; y <= radius; y++) {
            int halfWidth = radius - Math.abs(y);
            graphics.fill(centerX - halfWidth, centerY + y,
                    centerX + halfWidth + 1, centerY + y + 1, color);
        }
    }

    private static final class CarvedButton extends AbstractButton {
        private final Runnable action;

        private CarvedButton(int x, int y, int width, int height, Component message,
                             Runnable action) {
            super(x, y, width, height, message);
            this.action = action;
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY,
                                    float partialTick) {
            CultivationGuiTextures.drawButton(graphics, getX(), getY(), width, height,
                    active, isHoveredOrFocused());
            CultivationGuiTextures.drawButtonText(graphics, getMessage(), getX(), getY(),
                    width, height, active);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            defaultButtonNarrationText(narrationElementOutput);
        }
    }
}
