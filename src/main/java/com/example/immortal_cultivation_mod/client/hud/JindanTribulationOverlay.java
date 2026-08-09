package com.example.immortal_cultivation_mod.client.hud;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class JindanTribulationOverlay implements LayeredDraw.Layer {
    public static final ResourceLocation HUD_ID = ResourceLocation.fromNamespaceAndPath(
            ImmortalCultivationMod.MODID, "jindan_tribulation");
    public static final JindanTribulationOverlay INSTANCE = new JindanTribulationOverlay();

    public static final int EVENT_START = 0;
    public static final int EVENT_STRIKE_FLASH = 1;
    public static final int EVENT_FINAL_FLASH = 2;
    public static final int EVENT_GOLD_PULSE = 3;
    public static final int EVENT_CALM = 4;
    public static final int EVENT_END = 5;
    public static final int EVENT_PURPLE_FLASH = 6;

    private static boolean active;
    private static int elapsedTicks;
    private static int totalTicks;
    private static int calmTicks = -1;
    private static int flashTicks;
    private static int flashDuration;
    private static float flashStrength;
    private static int flashColor;
    private static int goldPulseTicks;
    private static int goldPulseDuration;
    private static float goldPulseStrength;

    private JindanTribulationOverlay() {
    }

    public static void handleEvent(int event, int durationTicks, float intensity) {
        switch (event) {
            case EVENT_START -> {
                active = true;
                elapsedTicks = 0;
                totalTicks = Math.max(1, durationTicks);
                calmTicks = -1;
                flashTicks = 0;
                goldPulseTicks = 0;
            }
            case EVENT_STRIKE_FLASH -> startFlash(durationTicks, intensity, 0xFFEAF6FF);
            case EVENT_FINAL_FLASH -> startFlash(durationTicks, intensity, 0xFFFFFFFF);
            case EVENT_PURPLE_FLASH -> startFlash(durationTicks, intensity, 0xFFD8A0FF);
            case EVENT_GOLD_PULSE -> {
                goldPulseDuration = Math.max(1, durationTicks);
                goldPulseTicks = goldPulseDuration;
                goldPulseStrength = Mth.clamp(intensity, 0.0F, 1.0F);
            }
            case EVENT_CALM -> calmTicks = 0;
            case EVENT_END -> clear();
            default -> {
            }
        }
    }

    private static void startFlash(int durationTicks, float intensity, int color) {
        flashDuration = Math.max(1, durationTicks);
        flashTicks = flashDuration;
        flashStrength = Mth.clamp(intensity, 0.0F, 1.0F);
        flashColor = color;
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            clear();
            return;
        }
        if (active) {
            elapsedTicks++;
            if (calmTicks >= 0) {
                calmTicks++;
            }
            if (elapsedTicks > totalTicks + 40) {
                clear();
            }
        }
        if (flashTicks > 0) {
            flashTicks--;
        }
        if (goldPulseTicks > 0) {
            goldPulseTicks--;
        }
    }

    public static void clear() {
        active = false;
        elapsedTicks = 0;
        totalTicks = 0;
        calmTicks = -1;
        flashTicks = 0;
        flashDuration = 0;
        goldPulseTicks = 0;
        goldPulseDuration = 0;
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!active && flashTicks <= 0 && goldPulseTicks <= 0) {
            return;
        }

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        if (active) {
            float buildup = Mth.clamp(elapsedTicks / 80.0F, 0.0F, 1.0F);
            float fade = calmTicks < 0 ? 1.0F : 1.0F - Mth.clamp(calmTicks / 90.0F, 0.0F, 1.0F);
            float darkness = 0.68F * buildup * fade;
            fillScreen(graphics, width, height, withAlpha(0xFF05070B, darkness));

            if (calmTicks >= 0) {
                float opening = Mth.clamp(calmTicks / 75.0F, 0.0F, 1.0F);
                int centerX = width / 2;
                int centerY = Math.max(18, height / 5);
                int radius = Math.round((20.0F + Math.max(width, height) * 0.55F * opening));
                drawSunBreak(graphics, centerX, centerY, radius, opening);
            }
        }

        if (goldPulseTicks > 0) {
            float remaining = goldPulseTicks / (float) Math.max(1, goldPulseDuration);
            float wave = (float) Math.sin((1.0F - remaining) * Math.PI);
            float alpha = goldPulseStrength * (0.14F + wave * 0.40F);
            fillScreen(graphics, width, height, withAlpha(0xFFFFC84B, alpha));
        }

        if (flashTicks > 0) {
            float remaining = flashTicks / (float) Math.max(1, flashDuration);
            float alpha = flashStrength * remaining * remaining;
            fillScreen(graphics, width, height, withAlpha(flashColor, alpha));
        }
    }

    private static void drawSunBreak(GuiGraphics graphics, int centerX, int centerY,
                                     int radius, float intensity) {
        if (radius <= 0 || intensity <= 0.0F) {
            return;
        }
        int bands = 12;
        for (int band = bands; band >= 1; band--) {
            float ratio = band / (float) bands;
            int bandRadius = Math.max(1, Math.round(radius * ratio));
            float alpha = intensity * (1.0F - ratio) * 0.14F + intensity * 0.015F;
            int color = withAlpha(0xFFFFE7A0, alpha);
            int verticalRadius = Math.max(1, Math.round(bandRadius * 0.58F));
            graphics.fill(centerX - bandRadius, centerY - verticalRadius,
                    centerX + bandRadius + 1, centerY + verticalRadius + 1, color);
        }
    }

    private static void fillScreen(GuiGraphics graphics, int width, int height, int color) {
        graphics.fill(0, 0, width, height, color);
    }

    private static int withAlpha(int color, float alpha) {
        int a = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
        return (a << 24) | (color & 0x00FFFFFF);
    }
}
