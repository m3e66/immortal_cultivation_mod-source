package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.screen.SpellWheelScreen;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.event.ScreenEvent;

public final class ClientScreenFade {
    private static final long OPENING_FADE_DURATION_MS = 700L;
    private static final long INTERNAL_FADE_DURATION_MS = 280L;
    private static final int VEIL_RGB = 0x0C0F14;
    private static final int OPENING_VEIL_ALPHA = 220;
    private static final int INTERNAL_VEIL_ALPHA = 76;
    private static final int OPENING_EDGE_ALPHA = 76;
    private static final int INTERNAL_EDGE_ALPHA = 28;
    private static Screen fadingScreen;
    private static long fadeStartedAt = -1L;
    private static long fadeDurationMs = OPENING_FADE_DURATION_MS;
    private static int veilAlpha = OPENING_VEIL_ALPHA;
    private static int edgeAlpha = OPENING_EDGE_ALPHA;

    private ClientScreenFade() {
    }

    public static void onOpening(ScreenEvent.Opening event) {
        Screen screen = event.getNewScreen();
        if (screen instanceof SpellWheelScreen) {
            reset();
            return;
        }
        if (isCultivationScreen(screen)) {
            boolean internalTransition = isCultivationScreen(event.getCurrentScreen());
            fadingScreen = screen;
            fadeStartedAt = -1L;
            fadeDurationMs = internalTransition
                    ? INTERNAL_FADE_DURATION_MS
                    : OPENING_FADE_DURATION_MS;
            veilAlpha = internalTransition
                    ? INTERNAL_VEIL_ALPHA
                    : OPENING_VEIL_ALPHA;
            edgeAlpha = internalTransition
                    ? INTERNAL_EDGE_ALPHA
                    : OPENING_EDGE_ALPHA;
        } else {
            reset();
        }
    }

    public static void onRenderPost(ScreenEvent.Render.Post event) {
        if (event.getScreen() != fadingScreen) {
            return;
        }
        long now = Util.getMillis();
        if (fadeStartedAt < 0L) {
            fadeStartedAt = now;
        }
        long elapsed = now - fadeStartedAt;
        if (elapsed >= fadeDurationMs) {
            reset();
            return;
        }
        float progress = Math.max(0.0F, Math.min(1.0F, elapsed / (float) fadeDurationMs));
        float revealed = (float) Math.sin(progress * Math.PI * 0.5D);
        float remaining = 1.0F - revealed;
        int alpha = Math.round(veilAlpha * remaining);
        event.getGuiGraphics().fill(
                0,
                0,
                event.getScreen().width,
                event.getScreen().height,
                (alpha << 24) | VEIL_RGB);

        drawSoftVignette(event, remaining);
    }

    private static void drawSoftVignette(ScreenEvent.Render.Post event, float remaining) {
        int width = event.getScreen().width;
        int height = event.getScreen().height;
        int depth = Math.max(8, Math.min(28, Math.min(width, height) / 30));
        for (int inset = 0; inset < depth; inset++) {
            float normalized = 1.0F - inset / (float) depth;
            int alpha = Math.round(edgeAlpha * remaining * normalized * normalized);
            if (alpha <= 0) {
                continue;
            }
            int color = (alpha << 24) | 0x05070A;
            int right = width - inset;
            int bottom = height - inset;
            event.getGuiGraphics().fill(inset, inset, right, inset + 1, color);
            event.getGuiGraphics().fill(inset, bottom - 1, right, bottom, color);
            event.getGuiGraphics().fill(inset, inset + 1, inset + 1, bottom - 1, color);
            event.getGuiGraphics().fill(right - 1, inset + 1, right, bottom - 1, color);
        }
    }

    private static void reset() {
        fadingScreen = null;
        fadeStartedAt = -1L;
        fadeDurationMs = OPENING_FADE_DURATION_MS;
        veilAlpha = OPENING_VEIL_ALPHA;
        edgeAlpha = OPENING_EDGE_ALPHA;
    }

    private static boolean isCultivationScreen(Screen screen) {
        if (screen == null) {
            return false;
        }
        String className = screen.getClass().getName();
        return className.startsWith("com.example.immortal_cultivation_mod.screen.")
                || className.startsWith("com.example.immortal_cultivation_mod.magicritual.ui.");
    }
}
