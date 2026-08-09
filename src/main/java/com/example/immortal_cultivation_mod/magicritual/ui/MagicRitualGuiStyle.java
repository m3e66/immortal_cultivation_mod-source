package com.example.immortal_cultivation_mod.magicritual.ui;

import com.example.immortal_cultivation_mod.screen.CultivationGuiTextures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

final class MagicRitualGuiStyle {
    static final int TEXT_COLOR = 0xFF30362F;

    private MagicRitualGuiStyle() {
    }

    static void drawWindow(GuiGraphics graphics, int x, int y, int width, int height) {
        CultivationGuiTextures.drawLandscape(graphics, x, y, width, height);
        CultivationGuiTextures.drawInnerBox(graphics, x + 14, y + 40, width - 28, height - 54);
    }

    static final class RitualButton extends Button {
        RitualButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            CultivationGuiTextures.drawButton(
                    graphics, getX(), getY(), width, height, active, isHoveredOrFocused());
            CultivationGuiTextures.drawButtonText(
                    graphics, getMessage(), getX(), getY(), width, height, active);
        }
    }
}
