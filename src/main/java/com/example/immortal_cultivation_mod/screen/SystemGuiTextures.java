package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class SystemGuiTextures {
    static final ResourceLocation WINDOWS =
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID,
                    "textures/gui/system_windows.png");

    private static final int TEXTURE_SIZE = 1254;

    private static final int WINDOW_X = 40;
    private static final int WINDOW_Y = 61;
    private static final int WINDOW_RIGHT = 549;
    private static final int WINDOW_BODY_Y = 130;
    private static final int WINDOW_BOTTOM_Y = 412;
    private static final int WINDOW_BOTTOM = 427;
    private static final int WINDOW_LEFT_INNER_X = 54;
    private static final int WINDOW_RIGHT_INNER_X = 536;

    private static final int TITLE_LEFT_SOURCE_WIDTH = 20;
    private static final int TITLE_BLANK_X = 300;
    private static final int TITLE_CONTROLS_X = 389;
    private static final int TITLE_SOURCE_HEIGHT =
            WINDOW_BODY_Y - WINDOW_Y;

    private static final int TITLE_LEFT_WIDTH = 10;
    private static final int TITLE_CONTROLS_WIDTH = 80;
    private static final int TITLE_HEIGHT = 35;
    private static final int BORDER_LEFT_WIDTH = 7;
    private static final int BORDER_RIGHT_WIDTH = 7;
    private static final int BORDER_BOTTOM_HEIGHT = 8;

    private static final int BUTTON_X = 41;
    private static final int BUTTON_Y = 808;
    private static final int BUTTON_SOURCE_HEIGHT = 58;
    private static final int BUTTON_LEFT_SOURCE_WIDTH = 13;
    private static final int BUTTON_RIGHT_X = 223;
    private static final int BUTTON_RIGHT_SOURCE_WIDTH = 13;
    private static final int BUTTON_BLANK_X = 62;
    private static final int BUTTON_CAP_WIDTH = 5;

    private static final int FACE = 0xFFC0C0C0;
    private static final int LIGHT = 0xFFDFDFDF;
    private static final int HIGHLIGHT = 0xFFFFFFFF;
    private static final int SHADOW = 0xFF808080;
    private static final int DARK_SHADOW = 0xFF202020;
    private static final int CONTENT = 0xFFF7F7F7;
    private static final int SELECTION = 0xFF0A4DBF;
    private static final int SELECTION_HOVER = 0xFFD8E7FA;

    private SystemGuiTextures() {
    }

    static void drawWindow(
            GuiGraphics graphics, int x, int y, int width, int height) {
        int safeWidth = Math.max(
                TITLE_LEFT_WIDTH + TITLE_CONTROLS_WIDTH + 1,
                width);
        drawTitleBar(graphics, x, y, safeWidth);
        if (height <= TITLE_HEIGHT) {
            return;
        }

        int safeHeight = Math.max(
                TITLE_HEIGHT + BORDER_BOTTOM_HEIGHT + 1,
                height);
        int bodyHeight = Math.max(
                1, safeHeight - TITLE_HEIGHT - BORDER_BOTTOM_HEIGHT);
        int bodyWidth = Math.max(
                1, safeWidth - BORDER_LEFT_WIDTH - BORDER_RIGHT_WIDTH);

        blit(
                graphics,
                x,
                y + TITLE_HEIGHT,
                BORDER_LEFT_WIDTH,
                bodyHeight,
                WINDOW_X,
                WINDOW_BODY_Y,
                WINDOW_LEFT_INNER_X - WINDOW_X,
                WINDOW_BOTTOM_Y - WINDOW_BODY_Y);
        blit(
                graphics,
                x + BORDER_LEFT_WIDTH,
                y + TITLE_HEIGHT,
                bodyWidth,
                bodyHeight,
                WINDOW_LEFT_INNER_X,
                WINDOW_BODY_Y,
                WINDOW_RIGHT_INNER_X - WINDOW_LEFT_INNER_X,
                WINDOW_BOTTOM_Y - WINDOW_BODY_Y);
        blit(
                graphics,
                x + safeWidth - BORDER_RIGHT_WIDTH,
                y + TITLE_HEIGHT,
                BORDER_RIGHT_WIDTH,
                bodyHeight,
                WINDOW_RIGHT_INNER_X,
                WINDOW_BODY_Y,
                WINDOW_RIGHT - WINDOW_RIGHT_INNER_X,
                WINDOW_BOTTOM_Y - WINDOW_BODY_Y);

        blit(
                graphics,
                x,
                y + safeHeight - BORDER_BOTTOM_HEIGHT,
                BORDER_LEFT_WIDTH,
                BORDER_BOTTOM_HEIGHT,
                WINDOW_X,
                WINDOW_BOTTOM_Y,
                WINDOW_LEFT_INNER_X - WINDOW_X,
                WINDOW_BOTTOM - WINDOW_BOTTOM_Y);
        blit(
                graphics,
                x + BORDER_LEFT_WIDTH,
                y + safeHeight - BORDER_BOTTOM_HEIGHT,
                bodyWidth,
                BORDER_BOTTOM_HEIGHT,
                WINDOW_LEFT_INNER_X,
                WINDOW_BOTTOM_Y,
                WINDOW_RIGHT_INNER_X - WINDOW_LEFT_INNER_X,
                WINDOW_BOTTOM - WINDOW_BOTTOM_Y);
        blit(
                graphics,
                x + safeWidth - BORDER_RIGHT_WIDTH,
                y + safeHeight - BORDER_BOTTOM_HEIGHT,
                BORDER_RIGHT_WIDTH,
                BORDER_BOTTOM_HEIGHT,
                WINDOW_RIGHT_INNER_X,
                WINDOW_BOTTOM_Y,
                WINDOW_RIGHT - WINDOW_RIGHT_INNER_X,
                WINDOW_BOTTOM - WINDOW_BOTTOM_Y);
    }

    private static void drawTitleBar(
            GuiGraphics graphics, int x, int y, int width) {
        blit(
                graphics,
                x,
                y,
                TITLE_LEFT_WIDTH,
                TITLE_HEIGHT,
                WINDOW_X,
                WINDOW_Y,
                TITLE_LEFT_SOURCE_WIDTH,
                TITLE_SOURCE_HEIGHT);
        blit(
                graphics,
                x + TITLE_LEFT_WIDTH,
                y,
                Math.max(
                        1,
                        width - TITLE_LEFT_WIDTH - TITLE_CONTROLS_WIDTH),
                TITLE_HEIGHT,
                TITLE_BLANK_X,
                WINDOW_Y,
                2,
                TITLE_SOURCE_HEIGHT);
        blit(
                graphics,
                x + width - TITLE_CONTROLS_WIDTH,
                y,
                TITLE_CONTROLS_WIDTH,
                TITLE_HEIGHT,
                TITLE_CONTROLS_X,
                WINDOW_Y,
                WINDOW_RIGHT - TITLE_CONTROLS_X,
                TITLE_SOURCE_HEIGHT);
    }

    public static void drawButton(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            boolean active,
            boolean hovered) {
        int centerWidth = Math.max(1, width - BUTTON_CAP_WIDTH * 2);
        blit(
                graphics,
                x,
                y,
                BUTTON_CAP_WIDTH,
                height,
                BUTTON_X,
                BUTTON_Y,
                BUTTON_LEFT_SOURCE_WIDTH,
                BUTTON_SOURCE_HEIGHT);
        blit(
                graphics,
                x + BUTTON_CAP_WIDTH,
                y,
                centerWidth,
                height,
                BUTTON_BLANK_X,
                BUTTON_Y,
                2,
                BUTTON_SOURCE_HEIGHT);
        blit(
                graphics,
                x + width - BUTTON_CAP_WIDTH,
                y,
                BUTTON_CAP_WIDTH,
                height,
                BUTTON_RIGHT_X,
                BUTTON_Y,
                BUTTON_RIGHT_SOURCE_WIDTH,
                BUTTON_SOURCE_HEIGHT);
        if (!active) {
            graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2,
                    0x66707070);
        } else if (hovered) {
            drawFocusRect(graphics, x + 3, y + 3, width - 6, height - 6);
        }
    }

    public static void drawInsetPanel(
            GuiGraphics graphics, int x, int y, int width, int height) {
        drawInsetPanel(graphics, x, y, width, height, CONTENT);
    }

    public static void drawInsetPanel(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int fillColor) {
        if (width < 4 || height < 4) {
            graphics.fill(x, y, x + width, y + height, fillColor);
            return;
        }
        graphics.fill(x, y, x + width, y + height, FACE);
        graphics.fill(x, y, x + width - 1, y + 1, DARK_SHADOW);
        graphics.fill(x, y, x + 1, y + height - 1, DARK_SHADOW);
        graphics.fill(x + 1, y + 1, x + width - 2, y + 2, SHADOW);
        graphics.fill(x + 1, y + 1, x + 2, y + height - 2, SHADOW);
        graphics.fill(x + 1, y + height - 1, x + width, y + height, HIGHLIGHT);
        graphics.fill(x + width - 1, y + 1, x + width, y + height, HIGHLIGHT);
        graphics.fill(x + 2, y + height - 2, x + width - 1, y + height - 1, LIGHT);
        graphics.fill(x + width - 2, y + 2, x + width - 1, y + height - 1, LIGHT);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, fillColor);
    }

    public static void drawRaisedPanel(
            GuiGraphics graphics, int x, int y, int width, int height) {
        if (width < 4 || height < 4) {
            graphics.fill(x, y, x + width, y + height, FACE);
            return;
        }
        graphics.fill(x, y, x + width, y + height, FACE);
        graphics.fill(x, y, x + width - 1, y + 1, HIGHLIGHT);
        graphics.fill(x, y, x + 1, y + height - 1, HIGHLIGHT);
        graphics.fill(x + 1, y + 1, x + width - 2, y + 2, LIGHT);
        graphics.fill(x + 1, y + 1, x + 2, y + height - 2, LIGHT);
        graphics.fill(x + 1, y + height - 1, x + width, y + height, DARK_SHADOW);
        graphics.fill(x + width - 1, y + 1, x + width, y + height, DARK_SHADOW);
        graphics.fill(x + 2, y + height - 2, x + width - 1, y + height - 1, SHADOW);
        graphics.fill(x + width - 2, y + 2, x + width - 1, y + height - 1, SHADOW);
    }

    public static void drawTextField(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            boolean focused) {
        drawInsetPanel(graphics, x, y, width, height, 0xFFFFFFFF);
        if (focused) {
            drawFocusRect(graphics, x + 3, y + 3, width - 6, height - 6);
        }
    }

    public static void drawProgressBar(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            float progress) {
        drawInsetPanel(graphics, x, y, width, height, 0xFFE8E8E8);
        int innerWidth = Math.max(0, width - 6);
        int filledWidth = Math.round(innerWidth * Math.max(0.0F, Math.min(1.0F, progress)));
        for (int offset = 0; offset < filledWidth; offset += 9) {
            int segmentWidth = Math.min(7, filledWidth - offset);
            graphics.fill(
                    x + 3 + offset,
                    y + 3,
                    x + 3 + offset + segmentWidth,
                    y + height - 3,
                    SELECTION);
        }
    }

    static void drawResizeGrip(
            GuiGraphics graphics, int x, int y, int width, int height) {
        int right = x + width - 3;
        int bottom = y + height - 3;
        for (int diagonal = 0; diagonal < 3; diagonal++) {
            int length = 3 + diagonal * 3;
            int startX = right - length;
            int startY = bottom;
            for (int pixel = 0; pixel <= length; pixel++) {
                int px = startX + pixel;
                int py = startY - pixel;
                graphics.fill(px, py, px + 1, py + 1, DARK_SHADOW);
                if (py + 1 < y + height - 1) {
                    graphics.fill(px, py + 1, px + 1, py + 2, HIGHLIGHT);
                }
            }
        }
    }

    public static void drawListRow(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            boolean selected,
            boolean hovered) {
        if (selected) {
            graphics.fill(x, y, x + width, y + height, SELECTION);
        } else if (hovered) {
            graphics.fill(x, y, x + width, y + height, SELECTION_HOVER);
        } else {
            graphics.fill(x, y, x + width, y + height, 0xFFFFFFFF);
        }
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFFE2E2E2);
    }

    public static void drawVerticalScrollbar(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int offset,
            int visible,
            int total) {
        if (width < 8 || height < 30) {
            return;
        }
        int buttonHeight = Math.min(width, height / 3);
        drawRaisedPanel(graphics, x, y, width, buttonHeight);
        drawRaisedPanel(
                graphics,
                x,
                y + height - buttonHeight,
                width,
                buttonHeight);
        drawArrow(graphics, x + width / 2, y + buttonHeight / 2, true);
        drawArrow(
                graphics,
                x + width / 2,
                y + height - buttonHeight / 2 - 1,
                false);

        int trackY = y + buttonHeight;
        int trackHeight = height - buttonHeight * 2;
        drawInsetPanel(
                graphics, x, trackY, width, trackHeight, 0xFFD4D4D4);
        int maximum = Math.max(0, total - visible);
        int innerHeight = Math.max(1, trackHeight - 4);
        int thumbHeight = maximum == 0
                ? innerHeight
                : Math.max(9, innerHeight * visible / Math.max(visible, total));
        thumbHeight = Math.min(innerHeight, thumbHeight);
        int travel = Math.max(0, innerHeight - thumbHeight);
        int thumbY = trackY + 2 + (maximum == 0
                ? 0
                : Math.round(travel * Math.max(0, Math.min(maximum, offset))
                        / (float) maximum));
        drawRaisedPanel(
                graphics, x + 2, thumbY, width - 4, thumbHeight);
    }

    private static void drawArrow(
            GuiGraphics graphics, int centerX, int centerY, boolean up) {
        int color = 0xFF101010;
        for (int row = 0; row < 3; row++) {
            int halfWidth = up ? row : 2 - row;
            int arrowY = centerY + row - 1;
            graphics.fill(
                    centerX - halfWidth,
                    arrowY,
                    centerX + halfWidth + 1,
                    arrowY + 1,
                    color);
        }
    }

    private static void drawFocusRect(
            GuiGraphics graphics, int x, int y, int width, int height) {
        if (width < 2 || height < 2) {
            return;
        }
        int color = 0xFF303030;
        for (int offset = 0; offset < width; offset += 2) {
            graphics.fill(x + offset, y, x + offset + 1, y + 1, color);
            graphics.fill(
                    x + offset,
                    y + height - 1,
                    x + offset + 1,
                    y + height,
                    color);
        }
        for (int offset = 2; offset < height - 1; offset += 2) {
            graphics.fill(x, y + offset, x + 1, y + offset + 1, color);
            graphics.fill(
                    x + width - 1,
                    y + offset,
                    x + width,
                    y + offset + 1,
                    color);
        }
    }

    static int titleTextRight(int windowX, int windowWidth) {
        return windowX + windowWidth - TITLE_CONTROLS_WIDTH - 4;
    }

    static int closeX(int windowX, int windowWidth) {
        return windowX + windowWidth - 30;
    }

    static int maximizeX(int windowX, int windowWidth) {
        return windowX + windowWidth - 53;
    }

    static int minimizeX(int windowX, int windowWidth) {
        return windowX + windowWidth - 76;
    }

    static int closeY(int windowY) {
        return windowY + 8;
    }

    static int closeWidth() {
        return 22;
    }

    static int closeHeight() {
        return 20;
    }

    static boolean isOverClose(
            double mouseX,
            double mouseY,
            int windowX,
            int windowY,
            int windowWidth) {
        int closeX = closeX(windowX, windowWidth);
        int closeY = closeY(windowY);
        return mouseX >= closeX
                && mouseX < closeX + closeWidth()
                && mouseY >= closeY
                && mouseY < closeY + closeHeight();
    }

    static boolean isOverMaximize(
            double mouseX,
            double mouseY,
            int windowX,
            int windowY,
            int windowWidth) {
        return isOverTitleControl(
                mouseX, mouseY, maximizeX(windowX, windowWidth), windowY);
    }

    static boolean isOverMinimize(
            double mouseX,
            double mouseY,
            int windowX,
            int windowY,
            int windowWidth) {
        return isOverTitleControl(
                mouseX, mouseY, minimizeX(windowX, windowWidth), windowY);
    }

    private static boolean isOverTitleControl(
            double mouseX, double mouseY, int x, int windowY) {
        int y = closeY(windowY);
        return mouseX >= x
                && mouseX < x + closeWidth()
                && mouseY >= y
                && mouseY < y + closeHeight();
    }

    private static void blit(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int sourceX,
            int sourceY,
            int sourceWidth,
            int sourceHeight) {
        graphics.blit(
                WINDOWS,
                x,
                y,
                width,
                height,
                sourceX,
                sourceY,
                sourceWidth,
                sourceHeight,
                TEXTURE_SIZE,
                TEXTURE_SIZE);
    }
}
