package com.example.immortal_cultivation_mod.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

final class SystemGuiButton extends AbstractButton {
    private final Runnable action;
    private float visualScale = 1.0F;
    private boolean wrapped;
    private java.util.function.BiConsumer<GuiGraphics, Boolean> artwork;

    /** Optional canvas-sized artwork; keeps the normal button input and narration. */
    void setArtwork(java.util.function.BiConsumer<GuiGraphics, Boolean> artwork) { this.artwork = artwork; }

    void setWrapped(boolean wrapped) { this.wrapped = wrapped; }

    /** Widget bounds use desktop coordinates; tooltips render after the desktop scale is removed. */
    static void keepTooltipOnScreen(net.minecraft.client.gui.screens.Screen screen) {
        for (var child : screen.children()) {
            if (child instanceof SystemGuiButton button && button.visible
                    && button.isHoveredOrFocused() && button.getTooltip() != null) {
                screen.setTooltipForNextRenderPass(button.getTooltip(),
                        net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE, true);
            }
        }
    }

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
        if (artwork != null) {
            graphics.pose().pushPose();
            graphics.pose().translate(getX(), getY(), 0);
            graphics.pose().scale(visualScale, visualScale, 1);
            artwork.accept(graphics, active && isHoveredOrFocused());
            graphics.flush();
            graphics.pose().popPose();
            return;
        }
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
        if (wrapped) {
            var lines = font.split(getMessage(), Math.max(1, (int)((width - 10) / visualScale)));
            int rows = Math.min(lines.size(), Math.max(1, (int)((height - 4) / (font.lineHeight * visualScale))));
            graphics.pose().pushPose();
            graphics.pose().translate(getX() + 5, getY() + (height - rows * font.lineHeight * visualScale) / 2F, 0);
            graphics.pose().scale(visualScale, visualScale, 1);
            for (int i=0;i<rows;i++) graphics.drawString(font, lines.get(i), 0, i*font.lineHeight, color, false);
            graphics.pose().popPose();
            return;
        }
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
