package com.example.immortal_cultivation_mod.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public final class CultivationGuiButton extends AbstractButton {
    private final Runnable action;

    public CultivationGuiButton(int x, int y, int width, int height,
                                Component message, Runnable action) {
        super(x, y, width, height, message);
        this.action = action;
    }

    @Override
    public void onPress() {
        action.run();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        CultivationGuiTextures.drawButton(graphics, getX(), getY(), width, height,
                active, isHoveredOrFocused());
        CultivationGuiTextures.drawButtonText(graphics, getMessage(), getX(), getY(),
                width, height, active);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
