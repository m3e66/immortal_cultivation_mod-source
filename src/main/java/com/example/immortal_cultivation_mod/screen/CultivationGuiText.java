package com.example.immortal_cultivation_mod.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public final class CultivationGuiText {
    private static final int LINE_HEIGHT = 10;
    private static final float MIN_SCALE = 0.32F;

    private CultivationGuiText() {
    }

    public static void drawFittedWrapped(
            GuiGraphics graphics,
            Font font,
            Component text,
            int x,
            int y,
            int maxWidth,
            int maxHeight,
            int color,
            boolean shadow) {
        drawFittedWrapped(graphics, font, text, x, y, maxWidth, maxHeight, color, shadow, LINE_HEIGHT);
    }

    public static void drawFittedWrapped(
            GuiGraphics graphics,
            Font font,
            Component text,
            int x,
            int y,
            int maxWidth,
            int maxHeight,
            int color,
            boolean shadow,
            int lineHeight) {
        if (maxWidth <= 0 || maxHeight <= 0) {
            return;
        }

        int safeLineHeight = Math.max(1, lineHeight);
        float scale = 1.0F;
        List<FormattedCharSequence> lines = wrap(font, text, maxWidth);
        while (scale > MIN_SCALE && lines.size() * safeLineHeight * scale > maxHeight) {
            scale = Math.max(MIN_SCALE, scale - 0.04F);
            lines = wrap(font, text, Math.max(20, Math.round(maxWidth / scale)));
        }

        if (!lines.isEmpty() && lines.size() * safeLineHeight * scale > maxHeight) {
            scale = Math.max(0.20F, maxHeight / (float) (lines.size() * safeLineHeight));
            lines = wrap(font, text, Math.max(20, Math.round(maxWidth / scale)));
        }

        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        for (int i = 0; i < lines.size(); i++) {
            graphics.drawString(font, lines.get(i), 0, i * safeLineHeight, color, shadow);
        }
        graphics.pose().popPose();
    }

    public static void drawFittedSingle(
            GuiGraphics graphics,
            Font font,
            Component text,
            int x,
            int y,
            int maxWidth,
            int color,
            boolean shadow) {
        int textWidth = font.width(text);
        if (textWidth <= maxWidth) {
            graphics.drawString(font, text, x, y, color, shadow);
            return;
        }

        float scale = Math.max(0.40F, maxWidth / (float) Math.max(1, textWidth));
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, 0, 0, color, shadow);
        graphics.pose().popPose();
    }

    private static List<FormattedCharSequence> wrap(Font font, Component text, int width) {
        List<FormattedCharSequence> result = new ArrayList<>();
        String[] paragraphs = text.getString().split("\\R", -1);
        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                result.add(Component.empty().withStyle(text.getStyle()).getVisualOrderText());
                continue;
            }
            result.addAll(font.split(Component.literal(paragraph).withStyle(text.getStyle()), width));
        }
        return result;
    }
}
