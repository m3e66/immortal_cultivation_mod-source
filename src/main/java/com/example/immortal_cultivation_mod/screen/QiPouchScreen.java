package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class QiPouchScreen extends AbstractContainerScreen<QiPouchMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int TEXTURE_ROWS = 6;

    public QiPouchScreen(QiPouchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageHeight = 114 + QiPouchMenu.ROWS * 18;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, 17, 256, 256);

        int renderedRows = 0;
        while (renderedRows < QiPouchMenu.ROWS) {
            int rows = Math.min(TEXTURE_ROWS, QiPouchMenu.ROWS - renderedRows);
            guiGraphics.blit(TEXTURE, x, y + 17 + renderedRows * 18, 0, 17, imageWidth, rows * 18, 256, 256);
            renderedRows += rows;
        }

        guiGraphics.blit(TEXTURE, x, y + 17 + QiPouchMenu.ROWS * 18, 0, 126, imageWidth, 96, 256, 256);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
