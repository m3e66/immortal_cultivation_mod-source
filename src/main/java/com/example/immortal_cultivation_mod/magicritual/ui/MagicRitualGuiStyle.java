package com.example.immortal_cultivation_mod.magicritual.ui;

import com.example.immortal_cultivation_mod.screen.CultivationGuiTextures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

final class MagicRitualGuiStyle {
   static final int TEXT_COLOR = -13617617;

   private MagicRitualGuiStyle() {
   }

   static void drawWindow(GuiGraphics graphics, int x, int y, int width, int height) {
      CultivationGuiTextures.drawLandscape(graphics, x, y, width, height);
      CultivationGuiTextures.drawInnerBox(graphics, x + 14, y + 40, width - 28, height - 54);
   }

   static final class RitualButton extends Button {
      RitualButton(int x, int y, int width, int height, Component message, Button.OnPress onPress) {
         super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
      }

      protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
         CultivationGuiTextures.drawButton(graphics, this.getX(), this.getY(), this.width, this.height, this.active, this.isHoveredOrFocused());
         CultivationGuiTextures.drawButtonText(graphics, this.getMessage(), this.getX(), this.getY(), this.width, this.height, this.active);
      }
   }
}
