package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class LearningMatchScreen extends Screen {
    private static final int BOARD_SIZE = 8;
    private static final int CELL_SIZE = 8;
    private static final int BOARD_PIXELS = BOARD_SIZE * CELL_SIZE;
    private static final int PANEL_WIDTH = 224;
    private static final int PANEL_HEIGHT = 190;
    private static final int SWAP_DURATION_TICKS = 6;
    private static final int CLEAR_DURATION_TICKS = 5;
    private static final int FALL_DURATION_TICKS = 8;
    private static final int[] BALL_COLORS = {
            0xFFE45B55,
            0xFF4D91D9,
            0xFFE0B83F,
            0xFF63AD72
    };

    private final String learningType;
    private final String targetId;
    private final int requiredPoints;
    private final float difficulty;
    private final float talent;
    private final boolean assisted;
    private final long token;
    private final int[][] board = new int[BOARD_SIZE][BOARD_SIZE];
    private final Random random = new Random();

    private int selectedRow = -1;
    private int selectedColumn = -1;
    private int score;
    private int lastGain;
    private int lastGainTicks;
    private int completedTicks;
    private int animationTicks;
    private int swapFirstRow;
    private int swapFirstColumn;
    private int swapSecondRow;
    private int swapSecondColumn;
    private int moveGain;
    private boolean initialized;
    private boolean completed;
    private AnimationPhase animationPhase = AnimationPhase.IDLE;
    private MatchResult clearingMatch;
    private List<FallingBall> fallingBalls = List.of();

    public LearningMatchScreen(String learningType, String targetId, int requiredPoints,
                               float difficulty, float talent, boolean assisted, long token) {
        super(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".learning_game.title"));
        this.learningType = learningType;
        this.targetId = targetId;
        this.requiredPoints = Math.max(1, requiredPoints);
        this.difficulty = difficulty;
        this.talent = talent;
        this.assisted = assisted;
        this.token = token;
    }

    @Override
    protected void init() {
        if (!initialized) {
            initialized = true;
            createPlayableBoard();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || completed) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (animationPhase != AnimationPhase.IDLE) {
            return true;
        }
        int boardX = boardX();
        int boardY = boardY();
        if (mouseX < boardX || mouseX >= boardX + BOARD_PIXELS
                || mouseY < boardY || mouseY >= boardY + BOARD_PIXELS) {
            selectedRow = -1;
            selectedColumn = -1;
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int column = (int) (mouseX - boardX) / CELL_SIZE;
        int row = (int) (mouseY - boardY) / CELL_SIZE;
        if (selectedRow < 0) {
            selectedRow = row;
            selectedColumn = column;
            return true;
        }

        int distance = Math.abs(selectedRow - row) + Math.abs(selectedColumn - column);
        if (distance != 1) {
            selectedRow = row;
            selectedColumn = column;
            return true;
        }

        swapFirstRow = selectedRow;
        swapFirstColumn = selectedColumn;
        swapSecondRow = row;
        swapSecondColumn = column;
        selectedRow = -1;
        selectedColumn = -1;
        animationTicks = 0;
        animationPhase = AnimationPhase.SWAPPING;
        return true;
    }

    private void beginClearing(MatchResult match) {
        clearingMatch = match;
        int points = assisted ? saturatingAdd(match.points(), match.points()) : match.points();
        moveGain = saturatingAdd(moveGain, points);
        animationTicks = 0;
        animationPhase = AnimationPhase.CLEARING;
    }

    private void beginFalling(Set<Integer> matched) {
        int[][] collapsed = new int[BOARD_SIZE][BOARD_SIZE];
        List<FallingBall> movements = new ArrayList<>(BOARD_SIZE * BOARD_SIZE);
        for (int column = 0; column < BOARD_SIZE; column++) {
            int writeRow = BOARD_SIZE - 1;
            for (int row = BOARD_SIZE - 1; row >= 0; row--) {
                if (matched.contains(row * BOARD_SIZE + column)) {
                    continue;
                }
                int color = board[row][column];
                collapsed[writeRow][column] = color;
                movements.add(new FallingBall(color, column, row, writeRow));
                writeRow--;
            }

            int spawnOffset = 0;
            while (writeRow >= 0) {
                int color = random.nextInt(BALL_COLORS.length);
                collapsed[writeRow][column] = color;
                movements.add(new FallingBall(color, column, -1 - spawnOffset, writeRow));
                spawnOffset++;
                writeRow--;
            }
        }

        for (int row = 0; row < BOARD_SIZE; row++) {
            System.arraycopy(collapsed[row], 0, board[row], 0, BOARD_SIZE);
        }
        clearingMatch = null;
        fallingBalls = List.copyOf(movements);
        animationTicks = 0;
        animationPhase = AnimationPhase.FALLING;
    }

    private void finishMove() {
        score = saturatingAdd(score, moveGain);
        lastGain = moveGain;
        moveGain = 0;
        lastGainTicks = 40;
        animationPhase = AnimationPhase.IDLE;
        fallingBalls = List.of();

        if (score >= requiredPoints) {
            completed = true;
            PacketDistributor.sendToServer(new ModPayloads.ServerboundCompleteLearningGamePayload(token, score));
            return;
        }
        if (!hasPossibleMove()) {
            createPlayableBoard();
        }
    }

    private MatchResult findMatches() {
        Set<Integer> cells = new HashSet<>();
        int points = 0;

        for (int row = 0; row < BOARD_SIZE; row++) {
            int start = 0;
            while (start < BOARD_SIZE) {
                int color = board[row][start];
                int end = start + 1;
                while (end < BOARD_SIZE && color >= 0 && board[row][end] == color) {
                    end++;
                }
                int length = end - start;
                if (color >= 0 && length >= 3) {
                    points = saturatingAdd(points, pointsForRun(length));
                    for (int column = start; column < end; column++) {
                        cells.add(row * BOARD_SIZE + column);
                    }
                }
                start = end;
            }
        }

        for (int column = 0; column < BOARD_SIZE; column++) {
            int start = 0;
            while (start < BOARD_SIZE) {
                int color = board[start][column];
                int end = start + 1;
                while (end < BOARD_SIZE && color >= 0 && board[end][column] == color) {
                    end++;
                }
                int length = end - start;
                if (color >= 0 && length >= 3) {
                    points = saturatingAdd(points, pointsForRun(length));
                    for (int row = start; row < end; row++) {
                        cells.add(row * BOARD_SIZE + column);
                    }
                }
                start = end;
            }
        }
        return new MatchResult(cells, points);
    }

    private void createPlayableBoard() {
        for (int attempt = 0; attempt < 256; attempt++) {
            for (int row = 0; row < BOARD_SIZE; row++) {
                for (int column = 0; column < BOARD_SIZE; column++) {
                    int color;
                    do {
                        color = random.nextInt(BALL_COLORS.length);
                    } while ((column >= 2 && board[row][column - 1] == color && board[row][column - 2] == color)
                            || (row >= 2 && board[row - 1][column] == color && board[row - 2][column] == color));
                    board[row][column] = color;
                }
            }
            if (hasPossibleMove()) {
                return;
            }
        }
    }

    private boolean hasPossibleMove() {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int column = 0; column < BOARD_SIZE; column++) {
                if (column + 1 < BOARD_SIZE && createsMatchAfterSwap(row, column, row, column + 1)) {
                    return true;
                }
                if (row + 1 < BOARD_SIZE && createsMatchAfterSwap(row, column, row + 1, column)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean createsMatchAfterSwap(int firstRow, int firstColumn, int secondRow, int secondColumn) {
        swap(firstRow, firstColumn, secondRow, secondColumn);
        boolean matched = !findMatches().cells().isEmpty();
        swap(firstRow, firstColumn, secondRow, secondColumn);
        return matched;
    }

    private void swap(int firstRow, int firstColumn, int secondRow, int secondColumn) {
        int value = board[firstRow][firstColumn];
        board[firstRow][firstColumn] = board[secondRow][secondColumn];
        board[secondRow][secondColumn] = value;
    }

    private static int pointsForRun(int length) {
        if (length >= 5) {
            return 10_000;
        }
        return length == 4 ? 1_000 : 100;
    }

    private static int saturatingAdd(int current, int added) {
        long sum = (long) current + added;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, sum));
    }

    @Override
    public void tick() {
        super.tick();
        tickAnimation();
        if (lastGainTicks > 0) {
            lastGainTicks--;
        }
        if (completed && ++completedTicks >= 20 && minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    private void tickAnimation() {
        if (animationPhase == AnimationPhase.IDLE) {
            return;
        }
        animationTicks++;
        if (animationTicks < animationDuration()) {
            return;
        }

        switch (animationPhase) {
            case SWAPPING -> {
                swap(swapFirstRow, swapFirstColumn, swapSecondRow, swapSecondColumn);
                MatchResult match = findMatches();
                animationTicks = 0;
                if (match.cells().isEmpty()) {
                    animationPhase = AnimationPhase.SWAPPING_BACK;
                } else {
                    beginClearing(match);
                }
            }
            case SWAPPING_BACK -> {
                swap(swapFirstRow, swapFirstColumn, swapSecondRow, swapSecondColumn);
                animationTicks = 0;
                animationPhase = AnimationPhase.IDLE;
            }
            case CLEARING -> beginFalling(clearingMatch.cells());
            case FALLING -> {
                MatchResult cascade = findMatches();
                if (cascade.cells().isEmpty()) {
                    finishMove();
                } else {
                    beginClearing(cascade);
                }
            }
            case IDLE -> {
            }
        }
    }

    private int animationDuration() {
        return switch (animationPhase) {
            case SWAPPING, SWAPPING_BACK -> SWAP_DURATION_TICKS;
            case CLEARING -> CLEAR_DURATION_TICKS;
            case FALLING -> FALL_DURATION_TICKS;
            case IDLE -> 1;
        };
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int panelX = (width - PANEL_WIDTH) / 2;
        int panelY = (height - PANEL_HEIGHT) / 2;
        int boardX = boardX();
        int boardY = boardY();
        Minecraft minecraft = Minecraft.getInstance();

        graphics.fill(panelX - 3, panelY + 3, panelX + PANEL_WIDTH + 3, panelY + PANEL_HEIGHT + 4, 0x66000000);
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFFF0E8CF);
        graphics.renderOutline(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFF8D7753);
        graphics.renderOutline(panelX + 3, panelY + 3, PANEL_WIDTH - 6, PANEL_HEIGHT - 6, 0xFFC9B98D);
        graphics.fill(panelX + 4, panelY + 4, panelX + PANEL_WIDTH - 4, panelY + 27, 0xFF31554C);

        graphics.drawCenteredString(minecraft.font, title, panelX + PANEL_WIDTH / 2, panelY + 11, 0xFFF4EAC7);
        graphics.drawCenteredString(minecraft.font, targetName(), panelX + PANEL_WIDTH / 2, panelY + 35, 0xFF43382D);
        graphics.drawCenteredString(minecraft.font,
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".learning_game.modifiers",
                        formatDecimal(talent), formatDecimal(difficulty)),
                panelX + PANEL_WIDTH / 2, panelY + 48, 0xFF6C604D);

        int barX = panelX + 22;
        int barY = panelY + 62;
        int barWidth = PANEL_WIDTH - 44;
        graphics.fill(barX, barY, barX + barWidth, barY + 8, 0xFF645B4B);
        int progress = Math.min(barWidth - 2,
                Math.round((barWidth - 2) * Math.min(1.0F, score / (float) requiredPoints)));
        graphics.fill(barX + 1, barY + 1, barX + 1 + progress, barY + 7, 0xFFD5A62D);
        graphics.renderOutline(barX, barY, barWidth, 8, 0xFF332E27);
        graphics.drawCenteredString(minecraft.font,
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".learning_game.score",
                        score, requiredPoints),
                panelX + PANEL_WIDTH / 2, panelY + 73, 0xFF43382D);

        graphics.fill(boardX - 2, boardY - 2, boardX + BOARD_PIXELS + 2, boardY + BOARD_PIXELS + 2, 0xFF473E31);
        graphics.renderOutline(boardX - 2, boardY - 2, BOARD_PIXELS + 4, BOARD_PIXELS + 4, 0xFFB79A5A);
        renderBoard(graphics, boardX, boardY, partialTick);
        if (selectedRow >= 0) {
            graphics.renderOutline(
                    boardX + selectedColumn * CELL_SIZE,
                    boardY + selectedRow * CELL_SIZE,
                    CELL_SIZE,
                    CELL_SIZE,
                    0xFFFFFFFF);
        }

        if (completed) {
            graphics.drawCenteredString(minecraft.font,
                    Component.translatable("screen." + ImmortalCultivationMod.MODID + ".learning_game.complete"),
                    panelX + PANEL_WIDTH / 2, panelY + 165, 0xFF287045);
        } else if (lastGainTicks > 0 && lastGain > 0) {
            graphics.drawCenteredString(minecraft.font, Component.literal("+" + lastGain),
                    panelX + PANEL_WIDTH / 2, panelY + 165, 0xFFB36A22);
        } else {
            graphics.drawCenteredString(minecraft.font,
                    Component.translatable("screen." + ImmortalCultivationMod.MODID + ".learning_game.hint"),
                    panelX + PANEL_WIDTH / 2, panelY + 165, 0xFF6C604D);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderBoard(GuiGraphics graphics, int boardX, int boardY, float partialTick) {
        graphics.enableScissor(boardX, boardY, boardX + BOARD_PIXELS, boardY + BOARD_PIXELS);
        switch (animationPhase) {
            case SWAPPING, SWAPPING_BACK -> renderSwappingBoard(graphics, boardX, boardY, partialTick);
            case CLEARING -> renderClearingBoard(graphics, boardX, boardY, partialTick);
            case FALLING -> renderFallingBoard(graphics, boardX, boardY, partialTick);
            case IDLE -> renderStaticBoard(graphics, boardX, boardY, -1, -1);
        }
        graphics.disableScissor();
    }

    private void renderStaticBoard(GuiGraphics graphics, int boardX, int boardY, int skipFirst, int skipSecond) {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int column = 0; column < BOARD_SIZE; column++) {
                int cell = row * BOARD_SIZE + column;
                if (cell == skipFirst || cell == skipSecond) {
                    continue;
                }
                drawBall(graphics, boardX + column * CELL_SIZE, boardY + row * CELL_SIZE,
                        BALL_COLORS[board[row][column]], 1.0F);
            }
        }
    }

    private void renderSwappingBoard(GuiGraphics graphics, int boardX, int boardY, float partialTick) {
        int firstCell = swapFirstRow * BOARD_SIZE + swapFirstColumn;
        int secondCell = swapSecondRow * BOARD_SIZE + swapSecondColumn;
        renderStaticBoard(graphics, boardX, boardY, firstCell, secondCell);

        float progress = easedAnimationProgress(partialTick);
        float firstX = lerp(swapFirstColumn, swapSecondColumn, progress);
        float firstY = lerp(swapFirstRow, swapSecondRow, progress);
        float secondX = lerp(swapSecondColumn, swapFirstColumn, progress);
        float secondY = lerp(swapSecondRow, swapFirstRow, progress);
        drawBall(graphics, boardX + firstX * CELL_SIZE, boardY + firstY * CELL_SIZE,
                BALL_COLORS[board[swapFirstRow][swapFirstColumn]], 1.0F);
        drawBall(graphics, boardX + secondX * CELL_SIZE, boardY + secondY * CELL_SIZE,
                BALL_COLORS[board[swapSecondRow][swapSecondColumn]], 1.0F);
    }

    private void renderClearingBoard(GuiGraphics graphics, int boardX, int boardY, float partialTick) {
        float scale = Math.max(0.12F, 1.0F - easedAnimationProgress(partialTick));
        Set<Integer> matched = clearingMatch == null ? Set.of() : clearingMatch.cells();
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int column = 0; column < BOARD_SIZE; column++) {
                int cell = row * BOARD_SIZE + column;
                drawBall(graphics, boardX + column * CELL_SIZE, boardY + row * CELL_SIZE,
                        BALL_COLORS[board[row][column]], matched.contains(cell) ? scale : 1.0F);
            }
        }
    }

    private void renderFallingBoard(GuiGraphics graphics, int boardX, int boardY, float partialTick) {
        float progress = easedAnimationProgress(partialTick);
        for (FallingBall ball : fallingBalls) {
            float row = lerp(ball.sourceRow(), ball.targetRow(), progress);
            drawBall(graphics, boardX + ball.column() * CELL_SIZE, boardY + row * CELL_SIZE,
                    BALL_COLORS[ball.color()], 1.0F);
        }
    }

    private float easedAnimationProgress(float partialTick) {
        float progress = Math.min(1.0F, (animationTicks + partialTick) / animationDuration());
        return progress * progress * (3.0F - 2.0F * progress);
    }

    private static float lerp(float start, float end, float progress) {
        return start + (end - start) * progress;
    }

    private void drawBall(GuiGraphics graphics, float x, float y, int color, float scale) {
        graphics.pose().pushPose();
        graphics.pose().translate(x + CELL_SIZE / 2.0F, y + CELL_SIZE / 2.0F, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        int half = CELL_SIZE / 2;
        graphics.fill(-half, -half, half, half, 0xFF292822);
        graphics.fill(-2, -3, 2, 3, color);
        graphics.fill(-3, -2, 3, 2, color);
        graphics.fill(-2, -2, 0, 0, 0x66FFFFFF);
        graphics.pose().popPose();
    }

    private Component targetName() {
        if ("restriction_insight".equals(learningType)) {
            return Component.translatable("item." + ImmortalCultivationMod.MODID + ".restriction_insight");
        }
        if ("spell".equals(learningType)) {
            ModSpells.SpellDef spell = ModSpells.get(targetId);
            return spell == null ? Component.literal(targetId) : spell.name();
        }
        CultivationMethods.MethodDef method = CultivationMethods.get(targetId);
        return method == null ? Component.literal(targetId) : Component.translatable(method.nameKey());
    }

    private int boardX() {
        return (width - BOARD_PIXELS) / 2;
    }

    private int boardY() {
        return (height - PANEL_HEIGHT) / 2 + 94;
    }

    private static String formatDecimal(float value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (!completed) {
            PacketDistributor.sendToServer(new ModPayloads.ServerboundCancelLearningGamePayload(token));
        }
        super.onClose();
    }

    private record MatchResult(Set<Integer> cells, int points) {
    }

    private record FallingBall(int color, int column, int sourceRow, int targetRow) {
    }

    private enum AnimationPhase {
        IDLE,
        SWAPPING,
        SWAPPING_BACK,
        CLEARING,
        FALLING
    }
}
