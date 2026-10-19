package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class SystemGuiTextures {
    private record Slice(ResourceLocation texture, int width, int height) {
        Slice(String name, int width, int height) {
            this(ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,
                    "textures/gui/system/" + name + ".png"), width, height);
        }
        void draw(GuiGraphics g, int x, int y, int w, int h) {
            g.blit(texture, x, y, w, h, 0, 0, width, height, width, height);
        }
    }
    private static final Slice TITLE_LEFT = new Slice("title_left",20,69);
    private static final Slice TITLE_FILL = new Slice("title_fill",2,69);
    private static final Slice MINIMIZE = new Slice("title_minimize",46,69);
    private static final Slice MAXIMIZE = new Slice("title_maximize",46,69);
    private static final Slice CLOSE = new Slice("title_close",68,69);
    private static final Slice LEFT = new Slice("window_left",14,282);
    private static final Slice BODY = new Slice("window_body",482,282);
    private static final Slice RIGHT = new Slice("window_right",13,282);
    private static final Slice BOTTOM_LEFT = new Slice("window_bottom_left",14,15);
    private static final Slice BOTTOM = new Slice("window_bottom",482,15);
    private static final Slice BOTTOM_RIGHT = new Slice("window_bottom_right",13,15);
    private static final Slice BUTTON_LEFT = new Slice("button_left",13,58);
    private static final Slice BUTTON_FILL = new Slice("button_fill",2,58);
    private static final Slice BUTTON_RIGHT = new Slice("button_right",13,58);
    private static final int TITLE_CONTROLS_WIDTH = 80;
    private static final int TITLE_HEIGHT = 35;

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

    static void drawWindow(GuiGraphics g, int x, int y, int width, int height) {
        int w = Math.max(91, width);
        TITLE_LEFT.draw(g,x,y,10,TITLE_HEIGHT);
        TITLE_FILL.draw(g,x+10,y,w-90,TITLE_HEIGHT);
        MINIMIZE.draw(g,x+w-80,y,23,TITLE_HEIGHT);
        MAXIMIZE.draw(g,x+w-57,y,23,TITLE_HEIGHT);
        CLOSE.draw(g,x+w-34,y,34,TITLE_HEIGHT);
        if (height <= TITLE_HEIGHT) return;
        int h = Math.max(44,height), bodyHeight = h-TITLE_HEIGHT-8;
        LEFT.draw(g,x,y+TITLE_HEIGHT,7,bodyHeight);
        BODY.draw(g,x+7,y+TITLE_HEIGHT,w-14,bodyHeight);
        RIGHT.draw(g,x+w-7,y+TITLE_HEIGHT,7,bodyHeight);
        BOTTOM_LEFT.draw(g,x,y+h-8,7,8);
        BOTTOM.draw(g,x+7,y+h-8,w-14,8);
        BOTTOM_RIGHT.draw(g,x+w-7,y+h-8,7,8);
    }

    public static void drawButton(GuiGraphics g, int x, int y, int width, int height,
                                  boolean active, boolean hovered) {
        BUTTON_LEFT.draw(g,x,y,5,height);
        BUTTON_FILL.draw(g,x+5,y,Math.max(1,width-10),height);
        BUTTON_RIGHT.draw(g,x+width-5,y,5,height);
        if (!active) g.fill(x+2,y+2,x+width-2,y+height-2,0x66707070);
        else if (hovered) drawFocusRect(g,x+3,y+3,width-6,height-6);
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

}
