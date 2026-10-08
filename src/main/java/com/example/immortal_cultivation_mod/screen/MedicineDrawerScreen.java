package com.example.immortal_cultivation_mod.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Compact parchment GUI for one three-slot medicine drawer. */
public final class MedicineDrawerScreen extends AbstractContainerScreen<MedicineDrawerMenu> {
    private static final int INK = 0xFF3B3024;
    private static final int MUTED = 0xFF76634C;
    private static final int BRONZE = 0xFF846337;
    private static final int SLOT_FILL = 0xFFE7DDC8;

    public MedicineDrawerScreen(MedicineDrawerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 176;
        titleLabelX = -1000;
        inventoryLabelX = MedicineDrawerMenu.PLAYER_X;
        inventoryLabelY = MedicineDrawerMenu.PLAYER_Y - 13;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        CultivationGuiTextures.drawLandscape(
                graphics, leftPos, topPos, imageWidth, imageHeight);
        CultivationGuiTextures.drawInnerBox(
                graphics, leftPos + 49, topPos + 27, 78, 42);
        for (int slot = 0; slot < MedicineDrawerMenu.DRAWER_SLOT_COUNT; slot++) {
            slotFrame(graphics,
                    leftPos + MedicineDrawerMenu.DRAWER_X + slot * 18,
                    topPos + MedicineDrawerMenu.DRAWER_Y);
        }

        CultivationGuiTextures.drawInnerBox(
                graphics, leftPos, topPos + 74, imageWidth, 102);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                slotFrame(graphics,
                        leftPos + MedicineDrawerMenu.PLAYER_X + column * 18,
                        topPos + MedicineDrawerMenu.PLAYER_Y + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            slotFrame(graphics,
                    leftPos + MedicineDrawerMenu.PLAYER_X + column * 18,
                    topPos + MedicineDrawerMenu.PLAYER_Y + 58);
        }
    }

    private static void slotFrame(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, BRONZE);
        graphics.fill(x, y, x + 16, y + 16, SLOT_FILL);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title,
                imageWidth / 2 - font.width(title) / 2, 10, INK, false);
        graphics.drawString(font, playerInventoryTitle,
                inventoryLabelX, inventoryLabelY, MUTED, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}

