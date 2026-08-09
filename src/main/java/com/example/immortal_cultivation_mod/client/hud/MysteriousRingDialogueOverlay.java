package com.example.immortal_cultivation_mod.client.hud;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import java.util.ArrayDeque;
import java.util.List;
import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

public final class MysteriousRingDialogueOverlay implements LayeredDraw.Layer {
    public static final ResourceLocation HUD_ID = ResourceLocation.fromNamespaceAndPath(
            ImmortalCultivationMod.MODID, "mysterious_ring_dialogue");
    public static final MysteriousRingDialogueOverlay INSTANCE =
            new MysteriousRingDialogueOverlay();

    private static final int MAX_QUEUED_MESSAGES = 24;
    private static final long FADE_IN_MILLIS = 260L;
    private static final long FADE_OUT_MILLIS = 520L;
    private static final ArrayDeque<DialogueMessage> QUEUE = new ArrayDeque<>();

    private static DialogueMessage activeMessage;
    private static long activeStartedAt;
    private static long activeEndsAt;
    private static UUID playerId;

    private MysteriousRingDialogueOverlay() {
    }

    public static void enqueue(Component message, boolean elder, int durationTicks) {
        if (message == null || message.getString().isBlank()) {
            return;
        }
        while (QUEUE.size() >= MAX_QUEUED_MESSAGES) {
            QUEUE.removeFirst();
        }
        QUEUE.addLast(new DialogueMessage(
                message.copy(), elder, Math.max(40, durationTicks)));
        startNext(Util.getMillis());
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            clear();
            return;
        }

        UUID currentPlayerId = minecraft.player.getUUID();
        if (playerId == null) {
            playerId = currentPlayerId;
        } else if (!playerId.equals(currentPlayerId)) {
            clear();
            playerId = currentPlayerId;
        }

        long now = Util.getMillis();
        if (activeMessage != null && now >= activeEndsAt) {
            activeMessage = null;
            startNext(now);
        }
        if (activeMessage == null || minecraft.options.hideGui) {
            return;
        }

        int maxTextWidth = Math.max(120, Math.min(420, graphics.guiWidth() - 64));
        List<FormattedCharSequence> lines = minecraft.font.split(
                activeMessage.text(), maxTextWidth);
        if (lines.isEmpty()) {
            return;
        }

        int textWidth = 0;
        for (FormattedCharSequence line : lines) {
            textWidth = Math.max(textWidth, minecraft.font.width(line));
        }
        int panelWidth = Math.min(maxTextWidth + 28, textWidth + 28);
        int panelHeight = lines.size() * 11 + 14;
        int left = (graphics.guiWidth() - panelWidth) / 2;
        int top = Math.max(18, graphics.guiHeight() - 68 - panelHeight);
        int right = left + panelWidth;
        int bottom = top + panelHeight;

        float alpha = messageAlpha(now);
        int alphaByte = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
        int background = (Mth.clamp(Math.round(alpha * 188.0F), 0, 255) << 24)
                | 0x11100D;
        int border = (alphaByte << 24)
                | (activeMessage.elder() ? 0xB58A42 : 0x87939A);
        int innerBorder = (Mth.clamp(Math.round(alpha * 110.0F), 0, 255) << 24)
                | (activeMessage.elder() ? 0xE0C27D : 0xC5CDD1);

        graphics.fill(left, top, right, bottom, background);
        graphics.fill(left, top, right, top + 1, border);
        graphics.fill(left, bottom - 1, right, bottom, border);
        graphics.fill(left, top, left + 1, bottom, border);
        graphics.fill(right - 1, top, right, bottom, border);
        graphics.fill(left + 4, top + 3, right - 4, top + 4, innerBorder);

        int textY = top + 8;
        int fallbackColor = (alphaByte << 24)
                | (activeMessage.elder() ? 0xF5D88A : 0xE0E5E7);
        for (FormattedCharSequence line : lines) {
            int textX = (graphics.guiWidth() - minecraft.font.width(line)) / 2;
            graphics.drawString(minecraft.font, line, textX, textY,
                    fallbackColor, true);
            textY += 11;
        }
    }

    private static float messageAlpha(long now) {
        long fadeInElapsed = now - activeStartedAt;
        long fadeOutRemaining = activeEndsAt - now;
        float fadeIn = smootherStep(Mth.clamp(
                fadeInElapsed / (float) FADE_IN_MILLIS, 0.0F, 1.0F));
        float fadeOut = smootherStep(Mth.clamp(
                fadeOutRemaining / (float) FADE_OUT_MILLIS, 0.0F, 1.0F));
        return Math.min(fadeIn, fadeOut);
    }

    private static float smootherStep(float value) {
        return value * value * value
                * (value * (value * 6.0F - 15.0F) + 10.0F);
    }

    private static void startNext(long now) {
        if (activeMessage != null || QUEUE.isEmpty()) {
            return;
        }
        activeMessage = QUEUE.removeFirst();
        activeStartedAt = now;
        activeEndsAt = now + activeMessage.durationTicks() * 50L;
    }

    private static void clear() {
        QUEUE.clear();
        activeMessage = null;
        activeStartedAt = 0L;
        activeEndsAt = 0L;
        playerId = null;
    }

    private record DialogueMessage(Component text, boolean elder, int durationTicks) {
    }
}
