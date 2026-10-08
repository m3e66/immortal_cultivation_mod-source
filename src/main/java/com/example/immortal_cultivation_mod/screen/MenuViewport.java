package com.example.immortal_cultivation_mod.screen;

/** One transform for menu drawing, hit testing, and pointer drag distances. */
public record MenuViewport(double scale, int logicalWidth, int logicalHeight) {
    public static MenuViewport fit(int width, int height, int requiredWidth, int requiredHeight, double maximumScale) {
        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);
        double scale = Math.min(maximumScale, Math.min(
                safeWidth / (double) Math.max(1, requiredWidth),
                safeHeight / (double) Math.max(1, requiredHeight)));
        return new MenuViewport(scale,
                Math.max(1, (int) Math.floor(safeWidth / scale + 1.0E-7)),
                Math.max(1, (int) Math.floor(safeHeight / scale + 1.0E-7)));
    }

    public double toMenu(double coordinate) {
        return coordinate / scale;
    }

    public double toScreen(double coordinate) {
        return coordinate * scale;
    }
}
