package com.example.immortal_cultivation_mod.screen;

import net.minecraft.util.Mth;

/** Mutable desktop-window geometry shared by the outer-system screens. */
final class SystemWindowState {
    static final int TITLE_HEIGHT = 35;
    private static final int RESIZE_BORDER = 8;

    private final int initialWidth;
    private final int initialHeight;
    private final int minimumWidth;
    private final int minimumHeight;
    private int x;
    private int y;
    private int width;
    private int height;
    private int restoreX;
    private int restoreY;
    private int restoreWidth;
    private int restoreHeight;
    private int pressMouseX;
    private int pressMouseY;
    private int pressX;
    private int pressY;
    private int pressWidth;
    private int pressHeight;
    private DragMode dragMode = DragMode.NONE;
    private boolean initialized;
    private boolean minimized;
    private boolean maximized;

    SystemWindowState(int initialWidth, int initialHeight) {
        this(
                initialWidth,
                initialHeight,
                Math.max(220, initialWidth / 2),
                Math.max(140, initialHeight / 2));
    }

    SystemWindowState(
            int initialWidth,
            int initialHeight,
            int minimumWidth,
            int minimumHeight) {
        this.initialWidth = Math.max(1, initialWidth);
        this.initialHeight = Math.max(TITLE_HEIGHT, initialHeight);
        this.minimumWidth = Math.max(1, minimumWidth);
        this.minimumHeight = Math.max(TITLE_HEIGHT, minimumHeight);
        width = this.initialWidth;
        height = this.initialHeight;
    }

    void initializeCentered(int desktopWidth, int desktopHeight) {
        if (initialized) {
            clampToDesktop(desktopWidth, desktopHeight);
            return;
        }
        int maximumWidth = maximumDesktopWidth(desktopWidth);
        int maximumHeight = maximumDesktopHeight(desktopHeight);
        width = Math.min(initialWidth, maximumWidth);
        height = Math.min(initialHeight, maximumHeight);
        x = Math.max(0, (maximumWidth - width) / 2);
        y = Math.max(0, (maximumHeight - height) / 2);
        initialized = true;
    }

    void placeNextTo(
            SystemWindowState other, int desktopWidth, int desktopHeight) {
        initializeCentered(desktopWidth, desktopHeight);
        int maximumWidth = maximumDesktopWidth(desktopWidth);
        int maximumHeight = maximumDesktopHeight(desktopHeight);
        int right = other.x() + other.width() + 12;
        int left = other.x() - width - 12;
        if (right + width <= maximumWidth) {
            x = right;
            y = other.y();
        } else if (left >= 0) {
            x = left;
            y = other.y();
        } else {
            x = Mth.clamp(
                    other.x() + 26,
                    0,
                    Math.max(0, maximumWidth - width));
            y = Mth.clamp(
                    other.y() + 26,
                    0,
                    Math.max(0, maximumHeight - renderedHeight()));
        }
        clampToDesktop(desktopWidth, desktopHeight);
    }

    boolean beginDrag(
            double mouseX,
            double mouseY,
            int button,
            int desktopWidth,
            int desktopHeight) {
        if (button != 0 || maximized) {
            return false;
        }
        int mx = (int) Math.floor(mouseX);
        int my = (int) Math.floor(mouseY);
        if (!contains(mx, my)) {
            return false;
        }
        DragMode candidate = minimized ? DragMode.NONE : resizeMode(mx, my);
        if (candidate == DragMode.NONE
                && my >= y
                && my < y + TITLE_HEIGHT
                && mx < x + width - 82) {
            candidate = DragMode.MOVE;
        }
        if (candidate == DragMode.NONE) {
            return false;
        }
        dragMode = candidate;
        pressMouseX = mx;
        pressMouseY = my;
        pressX = x;
        pressY = y;
        pressWidth = width;
        pressHeight = height;
        clampToDesktop(desktopWidth, desktopHeight);
        return true;
    }

    boolean dragTo(
            double mouseX,
            double mouseY,
            int desktopWidth,
            int desktopHeight) {
        if (dragMode == DragMode.NONE) {
            return false;
        }
        int maximumWidth = maximumDesktopWidth(desktopWidth);
        int maximumHeight = maximumDesktopHeight(desktopHeight);
        int effectiveMinimumWidth = Math.min(minimumWidth, maximumWidth);
        int effectiveMinimumHeight = Math.min(minimumHeight, maximumHeight);
        int dx = (int) Math.floor(mouseX) - pressMouseX;
        int dy = (int) Math.floor(mouseY) - pressMouseY;

        if (dragMode == DragMode.MOVE) {
            x = pressX + dx;
            y = pressY + dy;
            clampToDesktop(desktopWidth, desktopHeight);
            return true;
        }

        if (dragMode.left) {
            int fixedRight = Math.min(maximumWidth, pressX + pressWidth);
            int maximumX = Math.max(0, fixedRight - effectiveMinimumWidth);
            int nextX = Mth.clamp(pressX + dx, 0, maximumX);
            x = nextX;
            width = Math.max(effectiveMinimumWidth, fixedRight - nextX);
        }
        if (dragMode.right) {
            int maximumResizableWidth = Math.max(
                    effectiveMinimumWidth, maximumWidth - pressX);
            x = Math.min(pressX, Math.max(0, maximumWidth - effectiveMinimumWidth));
            width = Mth.clamp(
                    pressWidth + dx,
                    effectiveMinimumWidth,
                    maximumResizableWidth);
        }
        if (dragMode.top) {
            int fixedBottom = Math.min(maximumHeight, pressY + pressHeight);
            int maximumY = Math.max(0, fixedBottom - effectiveMinimumHeight);
            int nextY = Mth.clamp(pressY + dy, 0, maximumY);
            y = nextY;
            height = Math.max(effectiveMinimumHeight, fixedBottom - nextY);
        }
        if (dragMode.bottom) {
            int maximumResizableHeight = Math.max(
                    effectiveMinimumHeight, maximumHeight - pressY);
            y = Math.min(pressY, Math.max(0, maximumHeight - effectiveMinimumHeight));
            height = Mth.clamp(
                    pressHeight + dy,
                    effectiveMinimumHeight,
                    maximumResizableHeight);
        }
        clampToDesktop(desktopWidth, desktopHeight);
        return true;
    }

    void endDrag() {
        dragMode = DragMode.NONE;
    }

    void toggleMinimize() {
        minimized = !minimized;
        endDrag();
    }

    void toggleMaximize(int desktopWidth, int desktopHeight) {
        if (minimized) {
            minimized = false;
        }
        endDrag();
        if (maximized) {
            x = restoreX;
            y = restoreY;
            width = restoreWidth;
            height = restoreHeight;
            maximized = false;
            clampToDesktop(desktopWidth, desktopHeight);
            return;
        }
        restoreX = x;
        restoreY = y;
        restoreWidth = width;
        restoreHeight = height;
        x = 0;
        y = 0;
        width = maximumDesktopWidth(desktopWidth);
        height = maximumDesktopHeight(desktopHeight);
        maximized = true;
    }

    ContentLayout contentLayout(int referenceWidth, int referenceHeight) {
        float safeReferenceWidth = Math.max(1.0F, referenceWidth);
        float safeReferenceBodyHeight = Math.max(
                1.0F, referenceHeight - TITLE_HEIGHT);
        float availableWidth = Math.max(1.0F, width);
        float availableBodyHeight = Math.max(1.0F, height - TITLE_HEIGHT);
        float scale = Math.min(
                1.0F,
                Math.min(
                        availableWidth / safeReferenceWidth,
                        availableBodyHeight / safeReferenceBodyHeight));
        scale = Math.max(0.01F, scale);
        float renderedWidth = safeReferenceWidth * scale;
        float renderedBodyHeight = safeReferenceBodyHeight * scale;
        float originX = x + (availableWidth - renderedWidth) / 2.0F;
        float bodyTop = y
                + TITLE_HEIGHT
                + (availableBodyHeight - renderedBodyHeight) / 2.0F;
        float originY = bodyTop - TITLE_HEIGHT * scale;
        return new ContentLayout(x, y, originX, originY, scale);
    }

    boolean contains(double mouseX, double mouseY) {
        return mouseX >= x
                && mouseX < x + width
                && mouseY >= y
                && mouseY < y + renderedHeight();
    }

    boolean isDragging() {
        return dragMode != DragMode.NONE;
    }

    boolean isResizing() {
        return dragMode != DragMode.NONE && dragMode != DragMode.MOVE;
    }

    boolean isMinimized() {
        return minimized;
    }

    boolean isMaximized() {
        return maximized;
    }

    int x() {
        return x;
    }

    int y() {
        return y;
    }

    int width() {
        return width;
    }

    int height() {
        return height;
    }

    int renderedHeight() {
        return minimized ? TITLE_HEIGHT : height;
    }

    private DragMode resizeMode(int mouseX, int mouseY) {
        boolean left = mouseX < x + RESIZE_BORDER;
        boolean right = mouseX >= x + width - RESIZE_BORDER;
        boolean top = mouseY < y + RESIZE_BORDER;
        boolean bottom = mouseY >= y + height - RESIZE_BORDER;
        if (left && top) {
            return DragMode.TOP_LEFT;
        }
        if (right && top) {
            return DragMode.TOP_RIGHT;
        }
        if (left && bottom) {
            return DragMode.BOTTOM_LEFT;
        }
        if (right && bottom) {
            return DragMode.BOTTOM_RIGHT;
        }
        if (left) {
            return DragMode.LEFT;
        }
        if (right) {
            return DragMode.RIGHT;
        }
        if (top) {
            return DragMode.TOP;
        }
        if (bottom) {
            return DragMode.BOTTOM;
        }
        return DragMode.NONE;
    }

    private void clampToDesktop(int desktopWidth, int desktopHeight) {
        int maximumWidth = maximumDesktopWidth(desktopWidth);
        int maximumHeight = maximumDesktopHeight(desktopHeight);
        if (maximized) {
            x = 0;
            y = 0;
            width = maximumWidth;
            height = maximumHeight;
            return;
        }
        int effectiveMinimumWidth = Math.min(minimumWidth, maximumWidth);
        int effectiveMinimumHeight = Math.min(minimumHeight, maximumHeight);
        width = Mth.clamp(width, effectiveMinimumWidth, maximumWidth);
        height = Mth.clamp(height, effectiveMinimumHeight, maximumHeight);
        x = Mth.clamp(x, 0, Math.max(0, maximumWidth - width));
        y = Mth.clamp(
                y,
                0,
                Math.max(0, maximumHeight - renderedHeight()));
    }

    private static int maximumDesktopWidth(int desktopWidth) {
        return Math.max(1, desktopWidth);
    }

    private static int maximumDesktopHeight(int desktopHeight) {
        return Math.max(TITLE_HEIGHT, desktopHeight);
    }

    static record ContentLayout(
            int baseX,
            int baseY,
            float originX,
            float originY,
            float scale) {
        float translationX() {
            return originX - baseX * scale;
        }

        float translationY() {
            return originY - baseY * scale;
        }

        int mapX(int value) {
            return Math.round(originX + (value - baseX) * scale);
        }

        int mapY(int value) {
            return Math.round(originY + (value - baseY) * scale);
        }

        int mapWidth(int value) {
            return Math.max(1, Math.round(value * scale));
        }

        int mapHeight(int value) {
            return Math.max(1, Math.round(value * scale));
        }

        double virtualX(double value) {
            return baseX + (value - originX) / scale;
        }

        double virtualY(double value) {
            return baseY + (value - originY) / scale;
        }
    }

    private enum DragMode {
        NONE(false, false, false, false),
        MOVE(false, false, false, false),
        LEFT(true, false, false, false),
        RIGHT(false, true, false, false),
        TOP(false, false, true, false),
        BOTTOM(false, false, false, true),
        TOP_LEFT(true, false, true, false),
        TOP_RIGHT(false, true, true, false),
        BOTTOM_LEFT(true, false, false, true),
        BOTTOM_RIGHT(false, true, false, true);

        private final boolean left;
        private final boolean right;
        private final boolean top;
        private final boolean bottom;

        DragMode(boolean left, boolean right, boolean top, boolean bottom) {
            this.left = left;
            this.right = right;
            this.top = top;
            this.bottom = bottom;
        }
    }
}
