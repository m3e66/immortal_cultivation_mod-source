package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class CultivationGuiTextures {
    public static final int LANDSCAPE_WIDTH = 1328;
    public static final int LANDSCAPE_HEIGHT = 1062;
    public static final int NPC_WIDTH = 990;
    public static final int NPC_HEIGHT = 1443;

    private static final int BUTTON_WIDTH = 2030;
    private static final int BUTTON_HEIGHT = 307;
    private static final int BUTTON_CAP = 132;
    private static final int INNER_BOX_WIDTH = 32;
    private static final int INNER_BOX_HEIGHT = 32;
    private static final int INNER_BOX_CAP = 8;
    private static final int INNER_BOX_TILE = 16;
    private static final int PROGRESS_WIDTH = 476;
    private static final int PROGRESS_HEIGHT = 56;
    private static final int PROGRESS_SOURCE_CAP = 36;

    private static final ResourceLocation LANDSCAPE = texture("cultivation_background");
    private static final ResourceLocation NPC = texture("npc_background");
    private static final ResourceLocation BUTTON = texture("cultivation_button");
    private static final ResourceLocation INNER_BOX = elementTexture("cultivation_inner_box");
    private static final ResourceLocation PROGRESS = elementTexture("cultivation_progress_bar");

    private CultivationGuiTextures() {
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,
                "textures/gui/common/" + name + ".png");
    }

    private static ResourceLocation elementTexture(String name) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,
                "textures/gui/elements/" + name + ".png");
    }

    public static void drawLandscape(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.blit(LANDSCAPE, x, y, width, height,
                0.0F, 0.0F, LANDSCAPE_WIDTH, LANDSCAPE_HEIGHT,
                LANDSCAPE_WIDTH, LANDSCAPE_HEIGHT);
    }

    public static void drawNpc(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.blit(NPC, x, y, width, height,
                0.0F, 0.0F, NPC_WIDTH, NPC_HEIGHT, NPC_WIDTH, NPC_HEIGHT);
    }

    public static void drawButton(GuiGraphics graphics, int x, int y, int width, int height,
                                  boolean active, boolean hovered) {
        int capWidth = Math.min(width / 2,
                Math.max(1, Math.round(height * BUTTON_CAP / (float) BUTTON_HEIGHT)));
        int centerWidth = Math.max(0, width - capWidth * 2);

        graphics.blit(BUTTON, x, y, capWidth, height,
                0.0F, 0.0F, BUTTON_CAP, BUTTON_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT);
        if (centerWidth > 0) {
            graphics.blit(BUTTON, x + capWidth, y, centerWidth, height,
                    BUTTON_CAP, 0.0F, BUTTON_WIDTH - BUTTON_CAP * 2, BUTTON_HEIGHT,
                    BUTTON_WIDTH, BUTTON_HEIGHT);
        }
        graphics.blit(BUTTON, x + width - capWidth, y, capWidth, height,
                BUTTON_WIDTH - BUTTON_CAP, 0.0F, BUTTON_CAP, BUTTON_HEIGHT,
                BUTTON_WIDTH, BUTTON_HEIGHT);

        if (!active) {
            graphics.fill(x, y, x + width, y + height, 0x660D1311);
        } else if (hovered) {
            graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0x24FFFFFF);
        }
    }

    public static void drawButtonText(GuiGraphics graphics, Component message,
                                      int x, int y, int width, int height, boolean active) {
        var font = Minecraft.getInstance().font;
        int color = active ? 0xFFF8F3DF : 0xFFB8B5A8;
        int availableWidth = Math.max(1, width - 8);
        int textWidth = font.width(message);
        if (textWidth <= availableWidth) {
            graphics.drawString(font, message, x + (width - textWidth) / 2,
                    y + (height - 8) / 2, color, true);
            return;
        }

        float scale = Math.max(0.62F, availableWidth / (float) textWidth);
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        int scaledX = Math.round((x + width / 2.0F) / scale - textWidth / 2.0F);
        int scaledY = Math.round((y + (height - 8.0F * scale) / 2.0F) / scale);
        graphics.drawString(font, message, scaledX, scaledY, color, true);
        graphics.pose().popPose();
    }

    public static void drawInnerBox(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height) {
        int capX = Math.min(INNER_BOX_CAP, width / 2);
        int capY = Math.min(INNER_BOX_CAP, height / 2);
        int centerWidth = Math.max(0, width - capX * 2);
        int centerHeight = Math.max(0, height - capY * 2);

        blitUnscaled(graphics, INNER_BOX, x, y,
                0, 0, capX, capY,
                INNER_BOX_WIDTH, INNER_BOX_HEIGHT);
        blitUnscaled(graphics, INNER_BOX,
                x + width - capX, y,
                INNER_BOX_WIDTH - capX, 0,
                capX, capY,
                INNER_BOX_WIDTH, INNER_BOX_HEIGHT);
        blitUnscaled(graphics, INNER_BOX,
                x, y + height - capY,
                0, INNER_BOX_HEIGHT - capY,
                capX, capY,
                INNER_BOX_WIDTH, INNER_BOX_HEIGHT);
        blitUnscaled(graphics, INNER_BOX,
                x + width - capX, y + height - capY,
                INNER_BOX_WIDTH - capX,
                INNER_BOX_HEIGHT - capY,
                capX, capY,
                INNER_BOX_WIDTH, INNER_BOX_HEIGHT);

        tileRegion(graphics, INNER_BOX,
                x + capX, y,
                centerWidth, capY,
                INNER_BOX_CAP, 0,
                INNER_BOX_TILE, INNER_BOX_CAP,
                INNER_BOX_WIDTH, INNER_BOX_HEIGHT);
        tileRegion(graphics, INNER_BOX,
                x + capX, y + height - capY,
                centerWidth, capY,
                INNER_BOX_CAP,
                INNER_BOX_HEIGHT - INNER_BOX_CAP,
                INNER_BOX_TILE, INNER_BOX_CAP,
                INNER_BOX_WIDTH, INNER_BOX_HEIGHT);
        tileRegion(graphics, INNER_BOX,
                x, y + capY,
                capX, centerHeight,
                0, INNER_BOX_CAP,
                INNER_BOX_CAP, INNER_BOX_TILE,
                INNER_BOX_WIDTH, INNER_BOX_HEIGHT);
        tileRegion(graphics, INNER_BOX,
                x + width - capX, y + capY,
                capX, centerHeight,
                INNER_BOX_WIDTH - INNER_BOX_CAP,
                INNER_BOX_CAP,
                INNER_BOX_CAP, INNER_BOX_TILE,
                INNER_BOX_WIDTH, INNER_BOX_HEIGHT);
        tileRegion(graphics, INNER_BOX,
                x + capX, y + capY,
                centerWidth, centerHeight,
                INNER_BOX_CAP, INNER_BOX_CAP,
                INNER_BOX_TILE, INNER_BOX_TILE,
                INNER_BOX_WIDTH, INNER_BOX_HEIGHT);
    }

    public static void drawProgressFrame(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height) {
        int destinationCap = Math.min(
                width / 2,
                Math.max(4, Math.round(
                        height * PROGRESS_SOURCE_CAP
                                / (float) PROGRESS_HEIGHT)));
        int centerWidth = Math.max(0, width - destinationCap * 2);

        graphics.blit(
                PROGRESS,
                x,
                y,
                destinationCap,
                height,
                0.0F,
                0.0F,
                PROGRESS_SOURCE_CAP,
                PROGRESS_HEIGHT,
                PROGRESS_WIDTH,
                PROGRESS_HEIGHT);
        if (centerWidth > 0) {
            graphics.blit(
                    PROGRESS,
                    x + destinationCap,
                    y,
                    centerWidth,
                    height,
                    PROGRESS_SOURCE_CAP,
                    0.0F,
                    PROGRESS_WIDTH - PROGRESS_SOURCE_CAP * 2,
                    PROGRESS_HEIGHT,
                    PROGRESS_WIDTH,
                    PROGRESS_HEIGHT);
        }
        graphics.blit(
                PROGRESS,
                x + width - destinationCap,
                y,
                destinationCap,
                height,
                PROGRESS_WIDTH - PROGRESS_SOURCE_CAP,
                0.0F,
                PROGRESS_SOURCE_CAP,
                PROGRESS_HEIGHT,
                PROGRESS_WIDTH,
                PROGRESS_HEIGHT);
    }

    private static void tileRegion(
            GuiGraphics graphics,
            ResourceLocation texture,
            int x,
            int y,
            int width,
            int height,
            int sourceX,
            int sourceY,
            int tileWidth,
            int tileHeight,
            int textureWidth,
            int textureHeight) {
        for (int offsetY = 0; offsetY < height;
                offsetY += tileHeight) {
            int pieceHeight = Math.min(
                    tileHeight, height - offsetY);
            for (int offsetX = 0; offsetX < width;
                    offsetX += tileWidth) {
                int pieceWidth = Math.min(
                        tileWidth, width - offsetX);
                blitUnscaled(
                        graphics,
                        texture,
                        x + offsetX,
                        y + offsetY,
                        sourceX,
                        sourceY,
                        pieceWidth,
                        pieceHeight,
                        textureWidth,
                        textureHeight);
            }
        }
    }

    private static void blitUnscaled(
            GuiGraphics graphics,
            ResourceLocation texture,
            int x,
            int y,
            int sourceX,
            int sourceY,
            int width,
            int height,
            int textureWidth,
            int textureHeight) {
        if (width <= 0 || height <= 0) {
            return;
        }
        graphics.blit(
                texture,
                x,
                y,
                width,
                height,
                sourceX,
                sourceY,
                width,
                height,
                textureWidth,
                textureHeight);
    }
}
