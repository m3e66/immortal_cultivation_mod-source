package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.block.DingBlockEntity;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import com.example.immortal_cultivation_mod.client.DaoGuidanceClient;
import com.example.immortal_cultivation_mod.client.ForgingVisualState;
import com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public class DingScreen extends AbstractContainerScreen<DingMenu> {
    private static final ResourceLocation TEMPERATURE_BAR_TEXTURE =
            elementTexture("ding_temperature_bar");

    private static final int TEMPERATURE_SOURCE_WIDTH = 872;
    private static final int TEMPERATURE_SOURCE_HEIGHT = 68;

    private static final int INK = 0xFF38352E;
    private static final int MUTED_INK = 0xFF756F63;
    private static final int JADE = 0xFF315E50;
    private static final int GOLD = 0xFF9B7C43;
    private static final int PARCHMENT = 0xC8F1ECE1;
    private static final int PANEL_BORDER = 0xFFAAA18F;

    private static final int HEAT_BAR_X = 55;
    private static final int HEAT_BAR_Y = 50;
    private static final int HEAT_BAR_WIDTH = 250;
    private static final int HEAT_BAR_HEIGHT = 20;
    private static final int HEAT_FILL_INSET = 6;

    private static final int PROGRESS_X = 104;
    private static final int PROGRESS_Y = 163;
    private static final int PROGRESS_WIDTH = 124;
    private static final int PROGRESS_HEIGHT = 15;

    private static final int START_BUTTON_X = 238;
    private static final int START_BUTTON_Y = 158;
    private static final int START_BUTTON_WIDTH = 76;
    private static final int START_BUTTON_HEIGHT = 20;

    public DingScreen(
            DingMenu menu,
            Inventory inventory,
            Component title) {
        super(menu, inventory, title);
        imageWidth = DingMenu.IMAGE_WIDTH;
        imageHeight = DingMenu.IMAGE_HEIGHT;
        titleLabelX = -1000;
        titleLabelY = -1000;
        inventoryLabelX = DingMenu.PLAYER_INV_X;
        inventoryLabelY = 193;
    }

    @Override
    public void removed() {
        ForgingVisualState.clear(minecraft == null ? null : minecraft.player);
        super.removed();
    }

    private static ResourceLocation elementTexture(String name) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID,
                "textures/gui/elements/" + name + ".png");
    }

    @Override
    protected void renderBg(
            GuiGraphics graphics,
            float partialTick,
            int mouseX,
            int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        CultivationGuiTextures.drawLandscape(
                graphics, x, y, imageWidth, imageHeight);

        CultivationGuiTextures.drawInnerBox(
                graphics, x + 31, y + 39, 298, 145);

        renderTemperatureBar(graphics, x, y);
        renderForgeSectionPanels(graphics, x, y);
        renderForgeSlots(graphics, x, y);
        renderProgressBar(graphics, x, y);
        renderStartButton(graphics, x, y, mouseX, mouseY);
        renderInventoryPanel(graphics, x, y);
    }

    private void renderTemperatureBar(
            GuiGraphics graphics,
            int x,
            int y) {
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(
                TEMPERATURE_BAR_TEXTURE,
                x + HEAT_BAR_X,
                y + HEAT_BAR_Y,
                HEAT_BAR_WIDTH,
                HEAT_BAR_HEIGHT,
                0.0F,
                0.0F,
                TEMPERATURE_SOURCE_WIDTH,
                TEMPERATURE_SOURCE_HEIGHT,
                TEMPERATURE_SOURCE_WIDTH,
                TEMPERATURE_SOURCE_HEIGHT);

        int trackX = x + HEAT_BAR_X + HEAT_FILL_INSET;
        int trackWidth = HEAT_BAR_WIDTH - HEAT_FILL_INSET * 2;
        drawTemperatureMarker(
                graphics,
                trackX,
                y + HEAT_BAR_Y,
                trackWidth,
                menu.targetTemperature(),
                0xFFD43D31,
                3);
        drawTemperatureMarker(
                graphics,
                trackX,
                y + HEAT_BAR_Y,
                trackWidth,
                menu.temperature(),
                0xFFF8F3DF,
                4);
    }

    private void renderForgeSectionPanels(
            GuiGraphics graphics,
            int x,
            int y) {
        CultivationGuiTextures.drawInnerBox(
                graphics, x + 39, y + 82, 34, 62);
        CultivationGuiTextures.drawInnerBox(
                graphics, x + 83, y + 82, 190, 62);
        CultivationGuiTextures.drawInnerBox(
                graphics, x + 287, y + 82, 34, 62);
    }

    private void drawTemperatureMarker(
            GuiGraphics graphics,
            int trackX,
            int barY,
            int trackWidth,
            int temperature,
            int color,
            int extension) {
        int markerX = trackX + Math.round(
                (trackWidth - 1)
                        * clampTemperature(temperature)
                        / 10000.0F);
        graphics.fill(
                markerX - 1,
                barY - extension,
                markerX + 2,
                barY + HEAT_BAR_HEIGHT + extension,
                0xB0201D19);
        graphics.fill(
                markerX,
                barY - extension,
                markerX + 1,
                barY + HEAT_BAR_HEIGHT + extension,
                color);
    }

    private void renderForgeSlots(
            GuiGraphics graphics,
            int x,
            int y) {
        renderSlotFrame(
                graphics,
                x + DingMenu.YUJIAN_SLOT_X,
                y + DingMenu.YUJIAN_SLOT_Y,
                GOLD);
        renderSlotFrame(
                graphics,
                x + DingMenu.RESULT_SLOT_X,
                y + DingMenu.RESULT_SLOT_Y,
                JADE);
        for (int index = 0;
                index < DingBlockEntity.MATERIAL_COUNT;
                index++) {
            if (!menu.getSlot(DingBlockEntity.MATERIAL_START + index).isActive()) continue;
            int slotX = DingMenu.MATERIAL_START_X
                    + index % 10 * DingMenu.MATERIAL_SLOT_SPACING;
            int slotY = DingMenu.MATERIAL_START_Y
                    + index / 10 * DingMenu.MATERIAL_SLOT_SPACING;
            renderSlotFrame(
                    graphics,
                    x + slotX,
                    y + slotY,
                    PANEL_BORDER);
        }
    }

    private void renderProgressBar(
            GuiGraphics graphics,
            int x,
            int y) {
        int progressX = x + PROGRESS_X;
        int progressY = y + PROGRESS_Y;
        CultivationGuiTextures.drawProgressFrame(
                graphics,
                progressX,
                progressY,
                PROGRESS_WIDTH,
                PROGRESS_HEIGHT);
        int insetX = 5;
        int insetY = 4;
        int fillWidth = PROGRESS_WIDTH - insetX * 2;
        int fillHeight = PROGRESS_HEIGHT - insetY * 2;
        graphics.fill(
                progressX + insetX,
                progressY + insetY,
                progressX + insetX + fillWidth,
                progressY + insetY + fillHeight,
                0xFF343C39);
        int filled = Math.round(
                fillWidth
                        * Math.max(
                                0.0F,
                                Math.min(
                                        1.0F,
                                        menu.progress()
                                                / (float) menu.maxProgress())));
        graphics.fill(
                progressX + insetX,
                progressY + insetY,
                progressX + insetX + filled,
                progressY + insetY + fillHeight,
                menu.forging() ? 0xFFD77A2C : 0xFF8E7447);
    }

    private void renderStartButton(
            GuiGraphics graphics,
            int x,
            int y,
            int mouseX,
            int mouseY) {
        boolean active = !menu.forging();
        boolean hovered = active
                && mouseX >= x + START_BUTTON_X
                && mouseX < x + START_BUTTON_X
                        + START_BUTTON_WIDTH
                && mouseY >= y + START_BUTTON_Y
                && mouseY < y + START_BUTTON_Y
                        + START_BUTTON_HEIGHT;
        CultivationGuiTextures.drawButton(
                graphics,
                x + START_BUTTON_X,
                y + START_BUTTON_Y,
                START_BUTTON_WIDTH,
                START_BUTTON_HEIGHT,
                active,
                hovered);
        CultivationGuiTextures.drawButtonText(
                graphics,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".ding.start"),
                x + START_BUTTON_X,
                y + START_BUTTON_Y,
                START_BUTTON_WIDTH,
                START_BUTTON_HEIGHT,
                active);
    }

    private void renderInventoryPanel(
            GuiGraphics graphics,
            int x,
            int y) {
        int panelX = x + DingMenu.PLAYER_INV_X - 8;
        int panelY = y + 189;
        CultivationGuiTextures.drawInnerBox(
                graphics, panelX, panelY, 178, 96);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                renderSlotFrame(
                        graphics,
                        x + DingMenu.PLAYER_INV_X + column * 18,
                        y + DingMenu.PLAYER_INV_Y + row * 18,
                        PANEL_BORDER);
            }
        }
        for (int column = 0; column < 9; column++) {
            renderSlotFrame(
                    graphics,
                    x + DingMenu.PLAYER_INV_X + column * 18,
                    y + DingMenu.PLAYER_INV_Y + 58,
                    PANEL_BORDER);
        }
    }

    private void renderSlotFrame(
            GuiGraphics graphics,
            int x,
            int y,
            int border) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, border);
        graphics.fill(x, y, x + 16, y + 16, 0xFFE7E1D5);
    }

    @Override
    protected void renderLabels(
            GuiGraphics graphics,
            int mouseX,
            int mouseY) {
        drawCentered(
                graphics, title, imageWidth / 2, 16, INK);
        drawCentered(
                graphics,
                Component.literal(menu.temperature() + " C"),
                imageWidth / 2,
                35,
                INK);
        graphics.drawString(
                font,
                Component.literal("0 C"),
                34,
                56,
                MUTED_INK,
                false);
        graphics.drawString(
                font,
                Component.literal("10000 C"),
                307,
                56,
                MUTED_INK,
                false);
        drawCentered(
                graphics,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".ding.yujian"),
                DingMenu.YUJIAN_SLOT_X + 8,
                78,
                MUTED_INK);
        drawCentered(
                graphics,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".ding.materials"),
                imageWidth / 2,
                75,
                MUTED_INK);
        drawCentered(
                graphics,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".ding.result"),
                DingMenu.RESULT_SLOT_X + 8,
                78,
                MUTED_INK);

        int percentage = Math.round(
                100.0F
                        * Math.max(
                                0.0F,
                                Math.min(
                                        1.0F,
                                        menu.progress()
                                                / (float) menu.maxProgress())));
        graphics.drawString(
                font,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".ding.progress",
                        percentage),
                35,
                164,
                INK,
                false);
        graphics.drawString(
                font,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".ding.fire_cost",
                        menu.fireCost()),
                35,
                176,
                MUTED_INK,
                false);
        graphics.drawString(
                font,
                playerInventoryTitle,
                inventoryLabelX,
                inventoryLabelY,
                INK,
                false);
    }

    private void drawCentered(
            GuiGraphics graphics,
            Component text,
            int centerX,
            int y,
            int color) {
        graphics.drawString(
                font,
                text,
                centerX - font.width(text) / 2,
                y,
                color,
                false);
    }

    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE) {
            PacketDistributor.sendToServer(
                    new ModPayloads.ServerboundDingActionPayload(true));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button) {
        if (button == 0
                && !menu.forging()
                && mouseX >= leftPos + START_BUTTON_X
                && mouseX < leftPos + START_BUTTON_X
                        + START_BUTTON_WIDTH
                && mouseY >= topPos + START_BUTTON_Y
                && mouseY < topPos + START_BUTTON_Y
                        + START_BUTTON_HEIGHT) {
            PacketDistributor.sendToServer(
                    new ModPayloads.ServerboundDingActionPayload(false));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderGuidanceHighlight(graphics);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderGuidanceHighlight(GuiGraphics graphics) {
        int pulse = 145 + (int) (Math.sin(System.currentTimeMillis() / 180.0D) * 65.0D);
        int color = (Math.max(70, Math.min(230, pulse)) << 24) | 0xF0C66B;
        if (DaoGuidanceClient.isCurrentAction(DaoGuidanceContent.FORGE_MATERIAL)) {
            graphics.renderOutline(leftPos + DingMenu.MATERIAL_START_X - 3,
                    topPos + DingMenu.MATERIAL_START_Y - 3,
                    DingMenu.MATERIAL_SLOT_SPACING * 5,
                    DingMenu.MATERIAL_SLOT_SPACING * 2, color);
        } else if (DaoGuidanceClient.isCurrentAction(DaoGuidanceContent.FORGE_MOLD)) {
            graphics.renderOutline(leftPos + DingMenu.YUJIAN_SLOT_X - 3,
                    topPos + DingMenu.YUJIAN_SLOT_Y - 3, 24, 24, color);
        } else if (DaoGuidanceClient.isCurrentAction(DaoGuidanceContent.FORGE_HEAT)
                || DaoGuidanceClient.isCurrentAction(DaoGuidanceContent.FORGE_MELT)) {
            graphics.renderOutline(leftPos + START_BUTTON_X - 3,
                    topPos + START_BUTTON_Y - 3,
                    START_BUTTON_WIDTH + 6, START_BUTTON_HEIGHT + 6, color);
            graphics.renderOutline(leftPos + HEAT_BAR_X - 3,
                    topPos + HEAT_BAR_Y - 3,
                    HEAT_BAR_WIDTH + 6, HEAT_BAR_HEIGHT + 6, color);
        } else if (DaoGuidanceClient.isCurrentAction(DaoGuidanceContent.FORGE_COMPLETE)) {
            graphics.renderOutline(leftPos + DingMenu.RESULT_SLOT_X - 3,
                    topPos + DingMenu.RESULT_SLOT_Y - 3, 24, 24, color);
        }
    }

    private static int clampTemperature(int temperature) {
        return Math.max(0, Math.min(10000, temperature));
    }
}
