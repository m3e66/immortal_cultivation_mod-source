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
   private static final int[] RADII = new int[]{3, 5, 7, 9, 11};
   private static final int ROWS;
   private static final int WINDOW_W = 316;
   private static final int WINDOW_H;

   protected RadiusSelectionScreen() {
      super(Component.translatable("gui.immortal_cultivation_mod.select_radius"));
   }

   protected void init() {
      super.init();
      int centerX = this.width / 2;
      int top = (this.height - WINDOW_H) / 2 + 52;
      int gridWidth = 288;
      int left = centerX - gridWidth / 2;

      for(int index = 0; index < RADII.length; ++index) {
         int radius = RADII[index];
         int column = index / ROWS;
         int row = index % ROWS;
         this.addRenderableWidget(new MagicRitualGuiStyle.RitualButton(left + column * 58, top + row * 20, 56, 18, Component.translatable("gui.immortal_cultivation_mod.center_radius", new Object[]{radius}), (btn) -> this.selectRadius(radius)));
      }

   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics, mouseX, mouseY, partialTick);
      int x = (this.width - 316) / 2;
      int y = (this.height - WINDOW_H) / 2;
      MagicRitualGuiStyle.drawWindow(graphics, x, y, 316, WINDOW_H);
      graphics.drawString(this.font, this.title, this.width / 2 - this.font.width(this.title) / 2, y + 20, -13617617, false);
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   private void selectRadius(int radius) {
      PacketDistributor.sendToServer(new ServerboundPlaceCenterPayload(radius), new CustomPacketPayload[0]);
      this.onClose();
   }

   public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
   }

   static {
      ROWS = (RADII.length + 5 - 1) / 5;
      WINDOW_H = 64 + ROWS * 20;
   }
}
