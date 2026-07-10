package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public class DingScreen extends AbstractContainerScreen<DingMenu> {
    private static final ResourceLocation INVENTORY_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final ResourceLocation DING_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/ding_forge.png");

    private static final int DING_TEXTURE_WIDTH = 1518;
    private static final int DING_TEXTURE_HEIGHT = 1005;
    private static final int DING_WIDTH = 300;
    private static final int DING_HEIGHT = 199;

    private static final int TEMP_TEXT_X = 130;
    private static final int TEMP_TEXT_Y = 34;
    private static final int TEMP_BAR_X = 69;
    private static final int TEMP_BAR_Y = 45;
    private static final int TEMP_BAR_WIDTH = 162;
    private static final int TEMP_BAR_HEIGHT = 8;

    private static final int PROGRESS_X = 86;
    private static final int PROGRESS_Y = 176;
    private static final int PROGRESS_WIDTH = 104;
    private static final int PROGRESS_HEIGHT = 8;

    private static final int START_BUTTON_X = 197;
    private static final int START_BUTTON_Y = 169;
    private static final int START_BUTTON_WIDTH = 72;
    private static final int START_BUTTON_HEIGHT = 22;

    public DingScreen(DingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = DING_WIDTH;
        imageHeight = 312;
        titleLabelX = -1000;
        titleLabelY = -1000;
        inventoryLabelX = 70;
        inventoryLabelY = 202;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(DING_TEXTURE, x, y, DING_WIDTH, DING_HEIGHT,
                0.0F, 0.0F, DING_TEXTURE_WIDTH, DING_TEXTURE_HEIGHT,
                DING_TEXTURE_WIDTH, DING_TEXTURE_HEIGHT);
        guiGraphics.blit(INVENTORY_TEXTURE, x + 62, y + 204, 0, 126, 176, 96, 256, 256);

        int barX = x + TEMP_BAR_X;
        int barY = y + TEMP_BAR_Y;
        int pointerX = barX + (int) (TEMP_BAR_WIDTH * (Math.max(0, Math.min(10000, menu.temperature())) / 10000.0F));
        pointerX = Math.max(barX, Math.min(pointerX, barX + TEMP_BAR_WIDTH - 1));
        guiGraphics.fill(pointerX - 1, barY - 4, pointerX + 2, barY + TEMP_BAR_HEIGHT + 4, 0xFFFF3333);

        guiGraphics.fill(x + PROGRESS_X, y + PROGRESS_Y,
                x + PROGRESS_X + PROGRESS_WIDTH,
                y + PROGRESS_Y + PROGRESS_HEIGHT,
                0xFF4D4538);

        int filledProgressWidth = 0;
        if (menu.maxProgress() > 0) {
            filledProgressWidth = (int) (PROGRESS_WIDTH * (menu.progress() / (float) menu.maxProgress()));
        }

        guiGraphics.fill(x + PROGRESS_X, y + PROGRESS_Y,
                x + PROGRESS_X + filledProgressWidth,
                y + PROGRESS_Y + PROGRESS_HEIGHT,
                0xFFDB7B2B);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font,
                Component.literal(menu.temperature() + " C"),
                TEMP_TEXT_X,
                TEMP_TEXT_Y,
                0x3F3A35,
                false);

        super.renderLabels(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE) {
            PacketDistributor.sendToServer(new ModPayloads.ServerboundDingActionPayload(true));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0
                && mouseX >= leftPos + START_BUTTON_X
                && mouseX < leftPos + START_BUTTON_X + START_BUTTON_WIDTH
                && mouseY >= topPos + START_BUTTON_Y
                && mouseY < topPos + START_BUTTON_Y + START_BUTTON_HEIGHT) {
            PacketDistributor.sendToServer(new ModPayloads.ServerboundDingActionPayload(false));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
