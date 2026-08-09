package com.example.immortal_cultivation_mod.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

final class SystemGuiButton extends AbstractButton {
    private final Runnable action;
    private float visualScale = 1.0F;

    SystemGuiButton(
            int x,
            int y,
            int width,
            int height,
            Component message,
            Runnable action) {
        super(x, y, width, height, message);
        this.action = action;
    }

    @Override
    public void onPress() {
        action.run();
    }

    void setScaledBounds(
            int x,
            int y,
            int width,
            int height,
            float visualScale) {
        setX(x);
        setY(y);
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.visualScale = Math.max(0.1F, Math.min(1.0F, visualScale));
    }

    @Override
    protected void renderWidget(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick) {
        SystemGuiTextures.drawButton(
                graphics,
                getX(),
                getY(),
                width,
                height,
                active,
                isHoveredOrFocused());
        int color = active ? 0xFF101010 : 0xFF666666;
        var font = net.minecraft.client.Minecraft.getInstance().font;
        int textWidth = Math.max(1, font.width(getMessage()));
        float widthScale = Math.max(0.1F, (width - 8.0F) / textWidth);
        float heightScale = Math.max(
                0.1F, (height - 3.0F) / font.lineHeight);
        float textScale = Math.min(
                visualScale, Math.min(widthScale, heightScale));
        float centerX = getX() + width / 2.0F;
        float centerY = getY() + height / 2.0F;
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0.0F);
        graphics.pose().scale(textScale, textScale, 1.0F);
        graphics.drawCenteredString(
                font,
                getMessage(),
                0,
                -font.lineHeight / 2,
                color);
        graphics.pose().popPose();
    }

    @Override
    protected void updateWidgetNarration(
            NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
