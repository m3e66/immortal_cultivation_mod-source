package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class WantedOrderScreen extends Screen {
    private static final ResourceLocation POSTER_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ImmortalCultivationMod.MODID, "textures/gui/wanted_order_screen.png");
    // Native GUI artwork dimensions; the full image is independent of the item sprite.
    private static final int POSTER_TEXTURE_WIDTH = 1145;
    private static final int POSTER_TEXTURE_HEIGHT = 1374;
    private static final int POSTER_HEIGHT = 216;
    private static final int POSTER_WIDTH = Math.max(1, Math.round(
            (float) POSTER_HEIGHT * POSTER_TEXTURE_WIDTH / POSTER_TEXTURE_HEIGHT));
    private static final int VIEWPORT_MARGIN = 8;
    private static final int MAX_TEXT_LINES = 2;
    private static final Component ELLIPSIS = Component.literal("…");

    private final ResourceLocation skin;
    private final String wantedName;
    private final String realm;
    private final Component reward;
    private final int rewardCount;

    public WantedOrderScreen(String skinId, String wantedName, String realm, String rewardKey, int rewardCount) {
        super(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".wanted_order"));
        this.skin = ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "textures/entity/" + skinId + ".png");
        this.wantedName = wantedName;
        this.realm = realm;
        this.reward = Component.translatable(rewardKey);
        this.rewardCount = rewardCount;
    }

    @Override
    protected void init() {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float scale = Math.min(1.0F, Math.min(
                Math.max(1, width - VIEWPORT_MARGIN * 2) / (float) POSTER_WIDTH,
                Math.max(1, height - VIEWPORT_MARGIN * 2) / (float) POSTER_HEIGHT));
        float left = (width - POSTER_WIDTH * scale) / 2.0F;
        float top = (height - POSTER_HEIGHT * scale) / 2.0F;

        graphics.fill(0, 0, width, height, 0x66000000);
        graphics.pose().pushPose();
        graphics.pose().translate(left, top, 0);
        graphics.pose().scale(scale, scale, 1.0F);
        try {
            graphics.flush();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            try {
                // Plain blit does not enable blending; honor the parchment edge and skin overlay alpha.
                graphics.blit(POSTER_TEXTURE, 0, 0, POSTER_WIDTH, POSTER_HEIGHT,
                        0.0F, 0.0F, POSTER_TEXTURE_WIDTH, POSTER_TEXTURE_HEIGHT,
                        POSTER_TEXTURE_WIDTH, POSTER_TEXTURE_HEIGHT);

                int portraitSize = 86;
                int portraitX = (POSTER_WIDTH - portraitSize) / 2;
                int portraitY = 43;
                graphics.blit(skin, portraitX, portraitY, portraitSize, portraitSize,
                        8.0F, 8.0F, 8, 8, 64, 64);
                graphics.blit(skin, portraitX, portraitY, portraitSize, portraitSize,
                        40.0F, 8.0F, 8, 8, 64, 64);
            } finally {
                RenderSystem.disableBlend();
            }
            graphics.drawCenteredString(font, title, POSTER_WIDTH / 2, 18, 0xFF2C1710);

            int textWidth = Math.max(1, POSTER_WIDTH - 24);
            drawWrappedCentered(graphics, Component.literal(wantedName + "/" + realm),
                    143, textWidth, 0xFF2C1710);
            drawWrappedCentered(graphics, Component.translatable(
                    "screen." + ImmortalCultivationMod.MODID + ".wanted_reward", reward, rewardCount),
                    181, textWidth, 0xFF7A1717);
        } finally {
            graphics.pose().popPose();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawWrappedCentered(GuiGraphics graphics, Component text,
                                     int top, int maxWidth, int color) {
        List<FormattedText> lines = font.getSplitter().splitLines(text, maxWidth, Style.EMPTY);
        int lineCount = Math.min(MAX_TEXT_LINES, lines.size());
        for (int lineIndex = 0; lineIndex < lineCount; lineIndex++) {
            FormattedText line = lines.get(lineIndex);
            if (lines.size() > MAX_TEXT_LINES && lineIndex == MAX_TEXT_LINES - 1) {
                line = FormattedText.composite(font.substrByWidth(line,
                        Math.max(1, maxWidth - font.width(ELLIPSIS))), ELLIPSIS);
            }
            graphics.drawCenteredString(font, Language.getInstance().getVisualOrder(line),
                    POSTER_WIDTH / 2, top + lineIndex * font.lineHeight, color);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 || keyCode == 69) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
