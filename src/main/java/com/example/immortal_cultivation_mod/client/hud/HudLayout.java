package com.example.immortal_cultivation_mod.client.hud;

import com.example.immortal_cultivation_mod.config.HudElement;
import com.example.immortal_cultivation_mod.config.ModClientConfig;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;

public final class HudLayout {
    public record Box(int x, int y, int width, int height) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }
    public record Position(double x, double y) {}
    private record Measured(Box box, int width, int height) {}
    private static final Map<HudElement, Measured> MEASURED = new EnumMap<>(HudElement.class);

    private HudLayout() {}

    public static Box place(GuiGraphics graphics, HudElement element, int x, int y, int width, int height) {
        var fallback = new Box(x, y, width, height);
        MEASURED.put(element, new Measured(fallback, graphics.guiWidth(), graphics.guiHeight()));
        return resolve(element, fallback, graphics.guiWidth(), graphics.guiHeight());
    }

    private static Box resolve(HudElement element, Box fallback, int width, int height) {
        var value = ModClientConfig.HUD_POSITIONS.get(element);
        return new Box(HudPosition.resolve(value.x().get(), fallback.x(), width, fallback.width()),
                HudPosition.resolve(value.y().get(), fallback.y(), height, fallback.height()),
                fallback.width(), fallback.height());
    }

    public static Box preview(HudElement element, int width, int height) {
        var measured = MEASURED.get(element);
        Box fallback = defaults(element, width, height);
        if (measured != null && measured.width() == width && measured.height() == height) {
            fallback = measured.box();
        }
        return resolve(element, fallback, width, height);
    }

    public static void move(HudElement element, double x, double y, int width, int height, Box box) {
        set(element, new Position(HudPosition.normalize(x, width, box.width()),
                HudPosition.normalize(y, height, box.height())));
    }

    public static Map<HudElement, Position> snapshot() {
        var result = new EnumMap<HudElement, Position>(HudElement.class);
        ModClientConfig.HUD_POSITIONS.forEach((element, value) ->
                result.put(element, new Position(value.x().get(), value.y().get())));
        return result;
    }

    public static void set(HudElement element, Position position) {
        var value = ModClientConfig.HUD_POSITIONS.get(element);
        value.x().set(position.x());
        value.y().set(position.y());
    }

    public static void reset(HudElement element) { set(element, new Position(-1, -1)); }

    private static Box defaults(HudElement element, int w, int h) {
        return switch (element) {
            case HEALTH -> new Box(w / 2 - 91, h - 50, HealthBarOverlay.WIDTH, HealthBarOverlay.HEIGHT);
            case QI -> new Box(w / 2 + 10, h - 59, 110, 16);
            case METHOD -> new Box(w / 2 + 10, h - 75, 130, 16);
            case ZOMBIE -> new Box(w / 2 + 10, h - 91, 110, 16);
            case POSTURE -> new Box(w - 115, h - 55, 100, 16);
            case HEAVY_ATTACK, SPELL_CHARGE, SWORD_CHARGE -> new Box(w / 2 - ChargeReticleOverlay.CENTER_X,
                    h / 2 - ChargeReticleOverlay.CENTER_Y,ChargeReticleOverlay.WIDTH,ChargeReticleOverlay.HEIGHT);
            case PREPARED_SPELL -> new Box(w - 158, h - 30, 150, 13);
            case SHIELD -> new Box(w / 2 + 91, h - 55, 24, 54);
            case QI_RELEASE -> new Box(w - Math.min(238, w - 8) - 4, 4, Math.min(238, w - 8), 30);
            case GUIDANCE -> new Box(10, 34, Math.min(224, w), 61);
            case GUIDANCE_COMPLETE -> new Box(w / 2 - 154, Math.max(48, h / 4) - 9, Math.min(308, w), 53);
            case GUIDANCE_NOTICE -> new Box(w / 2 - 120, h - 78, Math.min(240, w), 27);
            case RING_DIALOGUE -> new Box(w / 2 - 140, h - 115, Math.min(280, w), 47);
            case TOXICITY -> new Box(w / 2 - 100, 12, Math.min(200, w), 9);
            case SENSE_COMPASS -> new Box(w / 2 - Math.min(190, w / 3) - 5, 26,
                    Math.min(190, w / 3) * 2 + 10, 20);
            case SENSE_DETAILS -> new Box(12, h - 79, Math.min(250, w), 59);
            case DEBUG_EYE -> new Box(w - Math.min(325, w - 16) - 6, 6, Math.min(325, w - 16), Math.min(150, h));
        };
    }
}
