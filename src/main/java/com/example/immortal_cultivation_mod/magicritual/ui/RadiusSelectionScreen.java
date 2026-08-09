package com.example.immortal_cultivation_mod.magicritual.ui;

import com.example.immortal_cultivation_mod.magicritual.network.payload.ServerboundPlaceCenterPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;

public class RadiusSelectionScreen extends Screen {
   private static final int COLUMNS = 5;
   private static final int BUTTON_W = 56;
   private static final int BUTTON_H = 18;
   private static final int GAP = 2;
   private static final int[] RADII = {3, 5, 7, 9, 11};
   private static final int ROWS = (RADII.length + COLUMNS - 1) / COLUMNS;
   private static final int WINDOW_W = 316;
   private static final int WINDOW_H = 64 + ROWS * (BUTTON_H + GAP);

   protected RadiusSelectionScreen() {
      super(Component.translatable("gui.immortal_cultivation_mod.select_radius"));
   }

   protected void init() {
      super.init();
      int centerX = this.width / 2;
      int top = (this.height - WINDOW_H) / 2 + 52;
      int gridWidth = COLUMNS * BUTTON_W + (COLUMNS - 1) * GAP;
      int left = centerX - gridWidth / 2;
      for (int index = 0; index < RADII.length; index++) {
         int radius = RADII[index];
         int column = index / ROWS;
         int row = index % ROWS;
         this.addRenderableWidget(new MagicRitualGuiStyle.RitualButton(
               left + column * (BUTTON_W + GAP), top + row * (BUTTON_H + GAP),
               BUTTON_W, BUTTON_H,
               Component.translatable("gui.immortal_cultivation_mod.center_radius", radius),
               btn -> this.selectRadius(radius)));
      }
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      renderBackground(graphics, mouseX, mouseY, partialTick);
      int x = (width - WINDOW_W) / 2;
      int y = (height - WINDOW_H) / 2;
      MagicRitualGuiStyle.drawWindow(graphics, x, y, WINDOW_W, WINDOW_H);
      graphics.drawString(font, title, width / 2 - font.width(title) / 2,
            y + 20, MagicRitualGuiStyle.TEXT_COLOR, false);
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   private void selectRadius(int radius) {
      PacketDistributor.sendToServer(new ServerboundPlaceCenterPayload(radius), new CustomPacketPayload[0]);
      this.onClose();
   }

   @Override
   public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
   }
}
