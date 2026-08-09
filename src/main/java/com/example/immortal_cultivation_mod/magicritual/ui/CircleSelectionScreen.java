package com.example.immortal_cultivation_mod.magicritual.ui;

import com.example.immortal_cultivation_mod.magicritual.block.custom.CircleType;
import com.example.immortal_cultivation_mod.magicritual.network.payload.ServerboundPlaceCirclePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;

public class CircleSelectionScreen extends Screen {
   private static final int WINDOW_W = 260;
   private static final int WINDOW_H = 170;
   protected CircleSelectionScreen() {
      super(Component.translatable("gui.immortal_cultivation_mod.select_circle"));
   }

   protected void init() {
      super.init();
      int centerX = this.width / 2;
      int top = (this.height - WINDOW_H) / 2 + 58;
      int bw = 150;
      int bh = 24;
      int gap = 6;
      this.addRenderableWidget(
         new MagicRitualGuiStyle.RitualButton(centerX - bw / 2, top, bw, bh,
               Component.translatable("circle_type.immortal_cultivation_mod.attack"), btn -> this.selectType(CircleType.ATTACK))
      );
      this.addRenderableWidget(
         new MagicRitualGuiStyle.RitualButton(centerX - bw / 2, top + bh + gap, bw, bh,
               Component.translatable("circle_type.immortal_cultivation_mod.defense"), btn -> this.selectType(CircleType.DEFENSE))
      );
      this.addRenderableWidget(
         new MagicRitualGuiStyle.RitualButton(centerX - bw / 2, top + (bh + gap) * 2, bw, bh,
               Component.translatable("circle_type.immortal_cultivation_mod.regeneration"), btn -> this.selectType(CircleType.REGENERATION))
      );
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

   private void selectType(CircleType type) {
      PacketDistributor.sendToServer(new ServerboundPlaceCirclePayload(type.getSerializedName()), new CustomPacketPayload[0]);
      this.onClose();
   }

   @Override
   public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
   }
}
