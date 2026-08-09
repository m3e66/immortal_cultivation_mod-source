package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class JindanBreakthroughScreen extends Screen {
    private static final String KEY_PREFIX = "screen." + ImmortalCultivationMod.MODID + ".jindan_nexus.";
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ImmortalCultivationMod.MODID, "textures/gui/breakthrough.png");

    private static final int TEXTURE_W = 1404;
    private static final int TEXTURE_H = 1120;
    private static final int WINDOW_W = 430;
    private static final int WINDOW_H = 343;

    private static final int BOARD_CENTER_X = 215;
    private static final int BOARD_CENTER_Y = 169;
    private static final float HEX_RADIUS = 13.0F;
    private static final int PRESSURE_INTERVAL_TICKS = 24;
    private static final int RESULT_DISPLAY_TICKS = 80;

    private static final int CHARCOAL = 0xE6191714;
    private static final int CHARCOAL_SOFT = 0xC9211D18;
    private static final int GOLD_DARK = 0xFF80642D;
    private static final int GOLD = 0xFFE0B84F;
    private static final int GOLD_BRIGHT = 0xFFFFE29A;
    private static final int IVORY = 0xFFFFF3D1;
    private static final int MUTED = 0xFFBAAD90;
    private static final int LEAK_RED = 0xFF8E3029;
    private static final int LEAK_GLOW = 0x708E3029;

    private final Random visualRandom = new Random();
    private final List<ScreenParticle> particles = new ArrayList<>();
    private final boolean assisted;

    private JindanMeridianPuzzle puzzle;
    private FormationState state = FormationState.ACTIVE;
    private long puzzleSeed;
    private int generation;
    private int rotationCount;
    private int pressureTicks;
    private int stateTicks;
    private float pressure;
    private boolean resultSent;
    private int hoveredTile = -1;
    private FormationButton resetButton;
    private FormationButton debugSkipButton;

    public JindanBreakthroughScreen(boolean assisted) {
        super(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".jindan_breakthrough"));
        this.assisted = assisted;
    }

    @Override
    protected void init() {
        long playerSeed = minecraft != null && minecraft.player != null
                ? minecraft.player.getUUID().getMostSignificantBits()
                        ^ minecraft.player.getUUID().getLeastSignificantBits()
                : 0L;
        puzzleSeed = playerSeed ^ System.nanoTime();
        generation = 0;
        buildPuzzle();

        int x = left();
        int y = top();
        boolean showDebugSkip = ClientData.debugEyeEnabled();
        resetButton = addRenderableWidget(new FormationButton(
                x + 323, y + 237, 82, 18,
                Component.translatable(KEY_PREFIX + "reset"), this::resetFormation));
        debugSkipButton = null;
        if (showDebugSkip) {
            debugSkipButton = addRenderableWidget(new FormationButton(
                    x + 323, y + 260, 82, 18,
                    Component.translatable(KEY_PREFIX + "debug_skip"), this::debugSkipPuzzle));
        }
    }

    private void buildPuzzle() {
        puzzle = new JindanMeridianPuzzle(
                puzzleSeed + generation * 0x9E3779B97F4A7C15L, assisted);
        state = FormationState.ACTIVE;
        rotationCount = 0;
        pressureTicks = 0;
        stateTicks = 0;
        pressure = 0.0F;
        resultSent = false;
        hoveredTile = -1;
        particles.clear();
    }

    private void resetFormation() {
        if (state != FormationState.ACTIVE) {
            return;
        }
        generation++;
        buildPuzzle();
        refreshButtons();
    }

    private void debugSkipPuzzle() {
        if (state != FormationState.ACTIVE || !ClientData.debugEyeEnabled()) {
            return;
        }
        succeedFormation();
    }

    private void refreshButtons() {
        if (resetButton == null) {
            return;
        }
        boolean active = state == FormationState.ACTIVE;
        resetButton.active = active;
        if (debugSkipButton != null) {
            debugSkipButton.active = active;
        }
    }

    @Override
    public void tick() {
        tickParticles();
        if (state == FormationState.ACTIVE) {
            pressureTicks++;
            if (pressureTicks >= PRESSURE_INTERVAL_TICKS) {
                pressureTicks = 0;
                int leaks = puzzle.leaks();
                if (leaks > 0) {
                    pressure = Math.min(100.0F,
                            pressure + Math.min(1.2F, 0.10F + leaks * 0.008F));
                }
            }
            if (pressure >= 100.0F) {
                failFormation();
            }
            return;
        }

        stateTicks++;
        if (state == FormationState.SUCCESS) {
            spawnSuccessParticles();
        } else if (state == FormationState.FAILURE && stateTicks % 5 == 0) {
            spawnFailureParticles();
        }
        if (stateTicks >= RESULT_DISPLAY_TICKS && !resultSent) {
            sendResultAndClose();
        }
    }

    private void succeedFormation() {
        if (state != FormationState.ACTIVE) {
            return;
        }
        state = FormationState.SUCCESS;
        stateTicks = 0;
        refreshButtons();
        for (int i = 0; i < 80; i++) {
            spawnCoreParticle(true);
        }
    }

    private void failFormation() {
        if (state != FormationState.ACTIVE) {
            return;
        }
        state = FormationState.FAILURE;
        stateTicks = 0;
        refreshButtons();
        for (int i = 0; i < 45; i++) {
            spawnCoreParticle(false);
        }
    }

    private void sendResultAndClose() {
        resultSent = true;
        float qualityScale = state == FormationState.SUCCESS ? qualityScale() : -1.0F;
        PacketDistributor.sendToServer(new ModPayloads.ServerboundCompleteJindanChallengePayload(
                qualityScale, 0, 0));
        Minecraft.getInstance().setScreen(null);
    }

    private float qualityScale() {
        float pressurePenalty = pressure / 250.0F;
        float rotationPenalty = Math.max(0, rotationCount - 30) / 500.0F;
        return 1.0F + Mth.clamp(pressurePenalty + rotationPenalty, 0.0F, 0.75F);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (state != FormationState.ACTIVE || (button != 0 && button != 1)) {
            return false;
        }
        int tile = tileAt(mouseX, mouseY);
        if (tile <= 0) {
            return false;
        }

        puzzle.rotate(tile, button == 0 ? 1 : -1);
        rotationCount++;
        pressure = Math.min(100.0F, pressure + 0.20F);
        spawnRotationParticles(tile, button == 0 ? 1 : -1);
        if (puzzle.isSolved()) {
            succeedFormation();
        } else if (pressure >= 100.0F) {
            failFormation();
        }
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(graphics);
        int x = left();
        int y = top();
        graphics.blit(TEXTURE, x, y, WINDOW_W, WINDOW_H,
                0.0F, 0.0F, TEXTURE_W, TEXTURE_H, TEXTURE_W, TEXTURE_H);

        drawHud(graphics, x, y);
        drawSidePanels(graphics, x, y);
        drawBoardPanel(graphics, x, y);
        drawFormation(graphics, mouseX, mouseY, partialTick);
        drawParticles(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        if (state == FormationState.SUCCESS) {
            drawSuccessOverlay(graphics, x, y, partialTick);
        } else if (state == FormationState.FAILURE) {
            drawFailureOverlay(graphics, x, y);
        } else if (hoveredTile > 0 && puzzle.tiles().get(hoveredTile).linked()) {
            graphics.renderTooltip(font, Component.translatable(KEY_PREFIX + "linked_tooltip"), mouseX, mouseY);
        }
    }

    private void drawHud(GuiGraphics graphics, int x, int y) {
        fillPanel(graphics, x + 83, y + 43, 264, 24);
        int leaks = puzzle.leaks();
        drawCenteredShadow(graphics, Component.translatable(KEY_PREFIX + "rotations", rotationCount),
                x + 171, y + 51, IVORY);
        drawCenteredShadow(graphics, Component.translatable(KEY_PREFIX + "leaks", leaks),
                x + 259, y + 51, leaks == 0 ? GOLD_BRIGHT : 0xFFFF9586);
    }

    private void drawSidePanels(GuiGraphics graphics, int x, int y) {
        fillPanel(graphics, x + 22, y + 75, 92, 205);
        fillPanel(graphics, x + 316, y + 75, 92, 205);

        drawCenteredShadow(graphics, Component.translatable(KEY_PREFIX + "rules_title"),
                x + 68, y + 84, GOLD_BRIGHT);
        int ruleY = y + 102;
        for (int i = 1; i <= 5; i++) {
            Component rule = Component.translatable(KEY_PREFIX + "rule." + i);
            for (FormattedCharSequence line : font.split(rule, 78)) {
                graphics.drawString(font, line, x + 29, ruleY, MUTED, true);
                ruleY += 10;
            }
            ruleY += 3;
        }

        drawCenteredShadow(graphics, Component.translatable(KEY_PREFIX + "formation_status"),
                x + 362, y + 84, GOLD_BRIGHT);
        drawMeter(graphics, x + 326, y + 108, 72, 8, puzzle.integrity() / 100.0F,
                0xFFB98931, Component.translatable(KEY_PREFIX + "integrity"));
        drawMeter(graphics, x + 326, y + 145, 72, 8, pressure / 100.0F,
                pressure < 70.0F ? 0xFFC49439 : LEAK_RED,
                Component.translatable(KEY_PREFIX + "core_pressure"));

        Component status = Component.translatable(KEY_PREFIX + switch (state) {
            case ACTIVE -> "status.active";
            case SUCCESS -> "status.success";
            case FAILURE -> "status.failure";
        });
        int statusY = y + 183;
        for (FormattedCharSequence line : font.split(status, 72)) {
            graphics.drawString(font, line, x + 326, statusY,
                    state == FormationState.FAILURE ? 0xFFFF9586 : IVORY, true);
            statusY += 10;
        }
    }

    private void drawBoardPanel(GuiGraphics graphics, int x, int y) {
        graphics.fill(x + 119, y + 74, x + 311, y + 278, CHARCOAL);
        graphics.renderOutline(x + 119, y + 74, 192, 204, GOLD_DARK);
        graphics.renderOutline(x + 122, y + 77, 186, 198, 0x705F4B28);
    }

    private void drawFormation(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hoveredTile = tileAt(mouseX, mouseY);
        boolean[] coreConnections = puzzle.connectedToCore();
        for (int i = puzzle.tiles().size() - 1; i >= 0; i--) {
            JindanMeridianPuzzle.Tile tile = puzzle.tiles().get(i);
            float cx = tileCenterX(tile);
            float cy = tileCenterY(tile);
            boolean hovered = i == hoveredTile && i > 0 && state == FormationState.ACTIVE;
            drawHexTile(graphics, i, cx, cy, coreConnections[i], hovered, partialTick);
        }
    }

    private void drawHexTile(GuiGraphics graphics, int index, float centerX, float centerY,
                             boolean connectedToCore, boolean hovered, float partialTick) {
        JindanMeridianPuzzle.Tile tile = puzzle.tiles().get(index);
        int fill = index == 0 ? 0xFF4A3418
                : hovered ? 0xFF413722
                : connectedToCore ? 0xFF2A251B : 0xFF261C1A;
        drawHexFill(graphics, centerX, centerY, HEX_RADIUS - 0.8F, fill);
        drawHexOutline(graphics, centerX, centerY, HEX_RADIUS,
                hovered ? GOLD_BRIGHT : tile.linked() ? 0xFFB78B3D : GOLD_DARK, 1.0F);

        if (tile.linked()) {
            drawCircle(graphics, centerX, centerY, HEX_RADIUS * 0.66F,
                    0xBFC7A653, 24);
        }

        int mask = puzzle.mask(index, false);
        for (int direction = 0; direction < JindanMeridianPuzzle.DIRECTION_COUNT; direction++) {
            if ((mask & (1 << direction)) == 0) {
                continue;
            }
            // Axial neighbors leave a flat-top hex through edge centers (30, 90, ... degrees).
            double angle = Math.toRadians(30.0D + direction * 60.0D);
            float endX = centerX + (float) Math.cos(angle) * HEX_RADIUS * 0.88F;
            float endY = centerY + (float) Math.sin(angle) * HEX_RADIUS * 0.88F;
            boolean matches = puzzle.pathMatches(index, direction, false);
            int glow = matches ? 0x65D7A93D : LEAK_GLOW;
            int color = matches ? (connectedToCore ? GOLD : 0xFF9D7C3C) : LEAK_RED;
            drawLine(graphics, centerX, centerY, endX, endY, 4.0F, glow);
            drawLine(graphics, centerX, centerY, endX, endY, 1.35F, color);
        }

        if (index == 0) {
            float pulse = 0.75F + 0.25F * (float) Math.sin((stateTicks + partialTick) * 0.16F);
            drawCircle(graphics, centerX, centerY, 8.4F + pulse, 0x90E8B33E, 36);
            drawCircleFill(graphics, centerX, centerY, 6.7F, 0xFFFFD967);
            drawCircleFill(graphics, centerX - 1.4F, centerY - 1.8F, 2.1F, 0xFFFFF3C2);
        } else {
            drawCircleFill(graphics, centerX, centerY, 1.7F,
                    connectedToCore ? GOLD_BRIGHT : 0xFF7F3D34);
        }
    }

    private void drawMeter(GuiGraphics graphics, int x, int y, int width, int height,
                           float progress, int fillColor, Component label) {
        graphics.drawString(font, label, x, y - 12, MUTED, true);
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, GOLD_DARK);
        graphics.fill(x, y, x + width, y + height, 0xFF191612);
        graphics.fill(x, y, x + Math.round(width * Mth.clamp(progress, 0.0F, 1.0F)),
                y + height, fillColor);
        Component value = Component.literal(Math.round(progress * 100.0F) + "%");
        graphics.drawCenteredString(font, value, x + width / 2, y + height + 3, IVORY);
    }

    private void drawSuccessOverlay(GuiGraphics graphics, int x, int y, float partialTick) {
        int panelX = x + 128;
        int panelY = y + 105;
        int panelW = 174;
        int panelH = 128;
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xF21B160D);
        graphics.renderOutline(panelX, panelY, panelW, panelH, GOLD_BRIGHT);
        graphics.renderOutline(panelX + 3, panelY + 3, panelW - 6, panelH - 6, GOLD_DARK);

        float pulse = 15.0F + 2.0F * (float) Math.sin((stateTicks + partialTick) * 0.18F);
        drawCircle(graphics, x + BOARD_CENTER_X, panelY + 35, pulse + 5.0F, 0x90E7B448, 48);
        drawCircleFill(graphics, x + BOARD_CENTER_X, panelY + 35, pulse, 0xFFFFD568);
        drawCircleFill(graphics, x + BOARD_CENTER_X - 4, panelY + 30, 3.4F, 0xFFFFF5C7);

        drawCenteredShadow(graphics, Component.translatable(KEY_PREFIX + "success_title"),
                x + BOARD_CENTER_X, panelY + 58, GOLD_BRIGHT);
        drawCenteredShadow(graphics, Component.translatable(KEY_PREFIX + "success_line_1"),
                x + BOARD_CENTER_X, panelY + 75, IVORY);
        drawCenteredShadow(graphics, Component.translatable(KEY_PREFIX + "success_line_2"),
                x + BOARD_CENTER_X, panelY + 87, IVORY);
        drawCenteredShadow(graphics, Component.translatable(KEY_PREFIX + "success_footer"),
                x + BOARD_CENTER_X, panelY + 107, GOLD_BRIGHT);
    }

    private void drawFailureOverlay(GuiGraphics graphics, int x, int y) {
        int panelX = x + 134;
        int panelY = y + 126;
        int panelW = 162;
        int panelH = 80;
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xF2261210);
        graphics.renderOutline(panelX, panelY, panelW, panelH, LEAK_RED);
        graphics.renderOutline(panelX + 3, panelY + 3, panelW - 6, panelH - 6, 0xFF5E2B26);
        drawCenteredShadow(graphics, Component.translatable(KEY_PREFIX + "failure_title"),
                x + BOARD_CENTER_X, panelY + 15, 0xFFFF9586);
        int lineY = panelY + 34;
        for (FormattedCharSequence line : font.split(
                Component.translatable(KEY_PREFIX + "failure_body"), panelW - 18)) {
            graphics.drawString(font, line,
                    x + BOARD_CENTER_X - font.width(line) / 2, lineY, IVORY, true);
            lineY += 11;
        }
    }

    private void spawnRotationParticles(int tileIndex, int direction) {
        JindanMeridianPuzzle.Tile tile = puzzle.tiles().get(tileIndex);
        float centerX = tileCenterX(tile);
        float centerY = tileCenterY(tile);
        int count = tile.linked() ? 20 : 9;
        for (int i = 0; i < count; i++) {
            double angle = visualRandom.nextDouble() * Math.PI * 2.0D;
            float speed = 0.18F + visualRandom.nextFloat() * 0.35F;
            particles.add(new ScreenParticle(centerX, centerY,
                    (float) Math.cos(angle) * speed * direction,
                    (float) Math.sin(angle) * speed * direction,
                    18 + visualRandom.nextInt(15),
                    1.0F + visualRandom.nextFloat() * 1.5F,
                    0xFFE0B84F));
        }
    }

    private void spawnSuccessParticles() {
        for (int i = 0; i < 3; i++) {
            spawnCoreParticle(true);
        }
    }

    private void spawnFailureParticles() {
        for (int i = 0; i < 2; i++) {
            spawnCoreParticle(false);
        }
    }

    private void spawnCoreParticle(boolean success) {
        float centerX = left() + BOARD_CENTER_X;
        float centerY = top() + BOARD_CENTER_Y;
        double angle = visualRandom.nextDouble() * Math.PI * 2.0D;
        float radius = 8.0F + visualRandom.nextFloat() * 58.0F;
        float startX = centerX + (float) Math.cos(angle) * radius;
        float startY = centerY + (float) Math.sin(angle) * radius;
        float speed = 0.45F + visualRandom.nextFloat() * 0.75F;
        float vx = (centerX - startX) / radius * speed;
        float vy = (centerY - startY) / radius * speed;
        if (!success) {
            vx = -vx;
            vy = -vy;
        }
        particles.add(new ScreenParticle(startX, startY, vx, vy,
                26 + visualRandom.nextInt(24),
                1.0F + visualRandom.nextFloat() * 2.0F,
                success ? 0xFFFFD76A : 0xFF9C332C));
        if (particles.size() > 280) {
            particles.subList(0, particles.size() - 280).clear();
        }
    }

    private void tickParticles() {
        for (int i = particles.size() - 1; i >= 0; i--) {
            ScreenParticle particle = particles.get(i);
            particle.tick();
            if (particle.dead()) {
                particles.remove(i);
            }
        }
    }

    private void drawParticles(GuiGraphics graphics) {
        for (ScreenParticle particle : particles) {
            int alpha = Math.round(220.0F * particle.alpha());
            int color = (alpha << 24) | (particle.color() & 0x00FFFFFF);
            int size = Math.max(1, Math.round(particle.size()));
            graphics.fill(Math.round(particle.x()), Math.round(particle.y()),
                    Math.round(particle.x()) + size, Math.round(particle.y()) + size, color);
        }
    }

    private int tileAt(double mouseX, double mouseY) {
        if (puzzle == null) {
            return -1;
        }
        int closest = -1;
        double closestDistance = Double.MAX_VALUE;
        for (int i = 0; i < puzzle.tiles().size(); i++) {
            JindanMeridianPuzzle.Tile tile = puzzle.tiles().get(i);
            double dx = Math.abs(mouseX - tileCenterX(tile));
            double dy = Math.abs(mouseY - tileCenterY(tile));
            if (dx > HEX_RADIUS || dy > Math.sqrt(3.0D) * HEX_RADIUS * 0.5D
                    || Math.sqrt(3.0D) * dx + dy > Math.sqrt(3.0D) * HEX_RADIUS) {
                continue;
            }
            double distance = dx * dx + dy * dy;
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = i;
            }
        }
        return closest;
    }

    private float tileCenterX(JindanMeridianPuzzle.Tile tile) {
        return left() + BOARD_CENTER_X + HEX_RADIUS * 1.5F * tile.q();
    }

    private float tileCenterY(JindanMeridianPuzzle.Tile tile) {
        return top() + BOARD_CENTER_Y
                + HEX_RADIUS * 1.7320508F * (tile.r() + tile.q() * 0.5F);
    }

    private void fillPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, CHARCOAL_SOFT);
        graphics.renderOutline(x, y, width, height, GOLD_DARK);
        graphics.renderOutline(x + 2, y + 2, width - 4, height - 4, 0x705F4B28);
    }

    private void drawCenteredShadow(GuiGraphics graphics, Component text, int centerX, int y, int color) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, color, true);
    }

    private static void drawHexFill(GuiGraphics graphics, float centerX, float centerY,
                                    float radius, int color) {
        int halfHeight = Math.round(radius * 0.8660254F);
        for (int y = -halfHeight; y <= halfHeight; y++) {
            float extent = radius - Math.abs(y) / 1.7320508F;
            graphics.fill(Math.round(centerX - extent), Math.round(centerY + y),
                    Math.round(centerX + extent) + 1, Math.round(centerY + y) + 1, color);
        }
    }

    private static void drawHexOutline(GuiGraphics graphics, float centerX, float centerY,
                                       float radius, int color, float thickness) {
        for (int side = 0; side < 6; side++) {
            double first = Math.toRadians(side * 60.0D);
            double second = Math.toRadians((side + 1) * 60.0D);
            drawLine(graphics,
                    centerX + (float) Math.cos(first) * radius,
                    centerY + (float) Math.sin(first) * radius,
                    centerX + (float) Math.cos(second) * radius,
                    centerY + (float) Math.sin(second) * radius,
                    thickness, color);
        }
    }

    private static void drawCircle(GuiGraphics graphics, float centerX, float centerY,
                                   float radius, int color, int segments) {
        float previousX = centerX + radius;
        float previousY = centerY;
        for (int i = 1; i <= segments; i++) {
            double angle = Math.PI * 2.0D * i / segments;
            float nextX = centerX + (float) Math.cos(angle) * radius;
            float nextY = centerY + (float) Math.sin(angle) * radius;
            drawLine(graphics, previousX, previousY, nextX, nextY, 1.0F, color);
            previousX = nextX;
            previousY = nextY;
        }
    }

    private static void drawCircleFill(GuiGraphics graphics, float centerX, float centerY,
                                       float radius, int color) {
        int roundedRadius = Math.max(1, Math.round(radius));
        for (int y = -roundedRadius; y <= roundedRadius; y++) {
            int extent = Math.round((float) Math.sqrt(Math.max(0.0D,
                    radius * radius - y * y)));
            graphics.fill(Math.round(centerX) - extent, Math.round(centerY) + y,
                    Math.round(centerX) + extent + 1, Math.round(centerY) + y + 1, color);
        }
    }

    private static void drawLine(GuiGraphics graphics, float x1, float y1, float x2, float y2,
                                 float thickness, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = Mth.sqrt(dx * dx + dy * dy);
        if (length <= 0.01F) {
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(x1, y1, 0.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees((float) Math.toDegrees(Math.atan2(dy, dx))));
        int half = Math.max(1, Math.round(thickness * 0.5F));
        graphics.fill(0, -half, Math.max(1, Math.round(length)), half + 1, color);
        graphics.pose().popPose();
    }

    private int left() {
        return (width - WINDOW_W) / 2;
    }

    private int top() {
        return (height - WINDOW_H) / 2;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
        // A breakthrough must resolve as success or backlash before this screen closes.
    }

    private enum FormationState {
        ACTIVE,
        SUCCESS,
        FAILURE
    }

    private static final class FormationButton extends AbstractButton {
        private final Runnable action;

        private FormationButton(int x, int y, int width, int height, Component message,
                                Runnable action) {
            super(x, y, width, height, message);
            this.action = action;
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int background = !active ? 0xFF241B16
                    : isHoveredOrFocused() ? 0xFF5B4525 : 0xFF352A1D;
            int border = !active ? 0xFF514334
                    : isHoveredOrFocused() ? GOLD_BRIGHT : GOLD_DARK;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, background);
            graphics.renderOutline(getX(), getY(), width, height, border);
            graphics.renderOutline(getX() + 2, getY() + 2, width - 4, height - 4,
                    active ? 0xFF6E5730 : 0xFF35281E);
            graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(),
                    getX() + width / 2, getY() + (height - 8) / 2,
                    active ? IVORY : 0xFF786E60);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            defaultButtonNarrationText(narrationElementOutput);
        }
    }

    private static final class ScreenParticle {
        private float x;
        private float y;
        private final float vx;
        private final float vy;
        private final int lifetime;
        private final float size;
        private final int color;
        private int age;

        private ScreenParticle(float x, float y, float vx, float vy,
                               int lifetime, float size, int color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.lifetime = lifetime;
            this.size = size;
            this.color = color;
        }

        private void tick() {
            x += vx;
            y += vy;
            age++;
        }

        private float alpha() {
            return 1.0F - age / (float) lifetime;
        }

        private boolean dead() {
            return age >= lifetime;
        }

        private float x() {
            return x;
        }

        private float y() {
            return y;
        }

        private float size() {
            return size;
        }

        private int color() {
            return color;
        }
    }
}
