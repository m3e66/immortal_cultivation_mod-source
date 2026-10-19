package com.example.immortal_cultivation_mod.client.hud;

/** Resolution-independent positions within the space available to a widget. */
public final class HudPosition {
    private HudPosition() {}

    public static int resolve(double fraction, int fallback, int screenSize, int widgetSize) {
        int available = Math.max(0, screenSize - widgetSize);
        int position = !Double.isFinite(fraction) || fraction < 0
                ? fallback : (int) Math.round(Math.min(1, fraction) * available);
        return Math.max(0, Math.min(available, position));
    }

    public static double normalize(double position, int screenSize, int widgetSize) {
        int available = Math.max(0, screenSize - widgetSize);
        return available == 0 || !Double.isFinite(position) ? 0
                : Math.max(0, Math.min(1, position / available));
    }
}
