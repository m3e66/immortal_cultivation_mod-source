package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.client.ClientData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CultivationSettingsScreen extends Screen {
    private static final String TRANSLATION_PREFIX =
            "screen." + ImmortalCultivationMod.MODID + ".settings.";
    private static final int PANEL_WIDTH = 430;
    private static final int PANEL_HEIGHT = 385;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_HEIGHT = 42;
    private CultivationGuiButton autoChannelButton;
    private CultivationGuiButton outwardQiButton;
    private CultivationGuiButton outwardQiBlockBreakingButton;
    private CultivationGuiButton creativeFlightButton;
    private CultivationGuiButton combatStateButton;
    private CultivationGuiButton chinesePoemButton;
    private CultivationGuiButton formationPlatePaymentButton;

    public CultivationSettingsScreen() {
        super(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".settings"));
    }

    @Override
    protected void init() {
        boolean showCreativeFlight = showCreativeFlightOption();
        int left = panelX();
        int top = panelY();
        int buttonX = left + Math.max(34, panelWidth() / 8);
        int buttonWidth = panelWidth() - (buttonX - left) * 2;

        autoChannelButton = addRenderableWidget(new CultivationGuiButton(
                buttonX, buttonY(0, showCreativeFlight), buttonWidth, BUTTON_HEIGHT,
                autoChannelLabel(), () -> {
                    ClientData.setAutoChannelWeaponQi(!ClientData.autoChannelWeaponQi());
                    autoChannelButton.setMessage(autoChannelLabel());
                }));
        outwardQiButton = addRenderableWidget(new CultivationGuiButton(
                buttonX, buttonY(1, showCreativeFlight), buttonWidth, BUTTON_HEIGHT,
                outwardQiLabel(), () -> {
                    ClientData.setOutwardQiRelease(!ClientData.outwardQiRelease());
                    outwardQiButton.setMessage(outwardQiLabel());
                }));
        outwardQiBlockBreakingButton = addRenderableWidget(new CultivationGuiButton(
                buttonX, buttonY(2, showCreativeFlight), buttonWidth, BUTTON_HEIGHT,
                outwardQiBlockBreakingLabel(), () -> {
                    ClientData.setOutwardQiBlockBreaking(!ClientData.outwardQiBlockBreaking());
                    outwardQiBlockBreakingButton.setMessage(outwardQiBlockBreakingLabel());
                }));
        combatStateButton = addRenderableWidget(new CultivationGuiButton(
                buttonX, buttonY(3, showCreativeFlight), buttonWidth, BUTTON_HEIGHT,
                combatStateLabel(), () -> {
                    ClientData.setCombatStateEnabled(!ClientData.combatStateEnabled());
                    combatStateButton.setMessage(combatStateLabel());
                }));
        chinesePoemButton = addRenderableWidget(new CultivationGuiButton(
                buttonX, buttonY(4, showCreativeFlight), buttonWidth, BUTTON_HEIGHT,
                chinesePoemLabel(), () -> {
                    ClientData.setChinesePoemDescriptions(!ClientData.chinesePoemDescriptions());
                    chinesePoemButton.setMessage(chinesePoemLabel());
                }));
        formationPlatePaymentButton = addRenderableWidget(new CultivationGuiButton(
                buttonX, buttonY(5, showCreativeFlight), buttonWidth, BUTTON_HEIGHT,
                formationPlatePaymentLabel(), () -> {
                    ClientData.setFormationPlateQiFirst(!ClientData.formationPlateQiFirst());
                    formationPlatePaymentButton.setMessage(formationPlatePaymentLabel());
                }));
        creativeFlightButton = null;
        if (showCreativeFlight) {
            creativeFlightButton = addRenderableWidget(new CultivationGuiButton(
                    buttonX, buttonY(6, true), buttonWidth, BUTTON_HEIGHT,
                    creativeFlightLabel(), () -> {
                        ClientData.setJindanCreativeFlight(!ClientData.jindanCreativeFlight());
                        creativeFlightButton.setMessage(creativeFlightLabel());
                    }));
        }
        addRenderableWidget(new CultivationGuiButton(
                left + panelWidth() / 2 - 50, top + panelHeight() - 31, 100, BUTTON_HEIGHT,
                Component.translatable(TRANSLATION_PREFIX + "done"), this::onClose));
    }

    private Component autoChannelLabel() {
        return Component.translatable(
                "screen." + ImmortalCultivationMod.MODID + ".settings.auto_channel",
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".settings."
                        + (ClientData.autoChannelWeaponQi() ? "on" : "off")));
    }

    private Component creativeFlightLabel() {
        return Component.translatable(
                "screen." + ImmortalCultivationMod.MODID + ".settings.jindan_flight",
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".settings."
                        + (ClientData.jindanCreativeFlight() ? "on" : "off")));
    }

    private Component outwardQiLabel() {
        return Component.translatable(
                "screen." + ImmortalCultivationMod.MODID + ".settings.outward_qi",
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".settings."
                        + (ClientData.outwardQiRelease() ? "on" : "off")));
    }

    private Component outwardQiBlockBreakingLabel() {
        return Component.translatable(
                TRANSLATION_PREFIX + "outward_qi_block_breaking",
                Component.translatable(TRANSLATION_PREFIX
                        + (ClientData.outwardQiBlockBreaking() ? "on" : "off")));
    }

    private Component combatStateLabel() {
        return Component.translatable(
                "screen." + ImmortalCultivationMod.MODID + ".settings.combat_state",
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".settings."
                        + (ClientData.combatStateEnabled() ? "on" : "off")));
    }

    private Component chinesePoemLabel() {
        return Component.translatable(
                TRANSLATION_PREFIX + "chinese_poems",
                Component.translatable(TRANSLATION_PREFIX
                        + (ClientData.chinesePoemDescriptions() ? "on" : "off")));
    }

    private Component formationPlatePaymentLabel() {
        return Component.translatable(
                TRANSLATION_PREFIX + "formation_plate_payment",
                Component.translatable(TRANSLATION_PREFIX
                        + (ClientData.formationPlateQiFirst()
                        ? "payment_qi_spirit_stone"
                        : "payment_spirit_stone_qi")));
    }

    private boolean showCreativeFlightOption() {
        return CultivationLevels.getRealmIndex(ClientData.cultivationData.cultivationLevel()) >= 3;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean showCreativeFlight = showCreativeFlightOption();
        int left = panelX();
        int top = panelY();

        graphics.fill(0, 0, width, height, 0xA0101716);
        CultivationGuiTextures.drawLandscape(graphics, left, top, panelWidth(), panelHeight());
        drawCenteredFitted(graphics, title, left + panelWidth() / 2, top + 20,
                panelWidth() - 150, 0xFF38352E);

        drawSettingRow(graphics, 0, showCreativeFlight);
        drawSettingRow(graphics, 1, showCreativeFlight);
        drawSettingRow(graphics, 2, showCreativeFlight);
        drawSettingRow(graphics, 3, showCreativeFlight);
        drawSettingRow(graphics, 4, showCreativeFlight);
        drawSettingRow(graphics, 5, showCreativeFlight);
        if (showCreativeFlight) {
            drawSettingRow(graphics, 6, true);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawSettingRow(GuiGraphics graphics, int index, boolean showCreativeFlight) {
        int left = panelX();
        int rowTop = rowTop(index, showCreativeFlight);
        int inset = Math.max(25, panelWidth() / 11);
        int rowHeight = rowVisualHeight(showCreativeFlight);
        graphics.fill(left + inset, rowTop, left + panelWidth() - inset,
                rowTop + rowHeight, 0x22A89B7D);
        graphics.renderOutline(left + inset, rowTop, panelWidth() - inset * 2,
                rowHeight, 0x2C897451);
    }

    private void drawCenteredFitted(GuiGraphics graphics, Component text, int centerX, int y,
                                    int availableWidth, int color) {
        int textWidth = font.width(text);
        if (textWidth <= availableWidth) {
            graphics.drawString(font, text, centerX - textWidth / 2, y, color, false);
            return;
        }

        float scale = Math.max(0.7F, availableWidth / (float) textWidth);
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        int scaledX = Math.round(centerX / scale - textWidth / 2.0F);
        int scaledY = Math.round(y / scale);
        graphics.drawString(font, text, scaledX, scaledY, color, false);
        graphics.pose().popPose();
    }

    private int buttonY(int index, boolean showCreativeFlight) {
        return rowTop(index, showCreativeFlight)
                + (rowVisualHeight(showCreativeFlight) - BUTTON_HEIGHT) / 2;
    }

    private int rowTop(int index, boolean showCreativeFlight) {
        int contentTop = panelY() + 44;
        int availableHeight = contentHeight();
        int rowCount = rowCount(showCreativeFlight);
        int rowPitch = rowPitch(showCreativeFlight);
        int firstRow = contentTop + Math.max(0, (availableHeight - rowPitch * rowCount) / 2);
        return firstRow + index * rowPitch;
    }

    private int rowVisualHeight(boolean showCreativeFlight) {
        return Math.min(ROW_HEIGHT, rowPitch(showCreativeFlight));
    }

    private int rowPitch(boolean showCreativeFlight) {
        return Math.min(57, contentHeight() / rowCount(showCreativeFlight));
    }

    private int rowCount(boolean showCreativeFlight) {
        return showCreativeFlight ? 7 : 6;
    }

    private int contentHeight() {
        int contentTop = panelY() + 44;
        int contentBottom = panelY() + panelHeight() - 39;
        return Math.max(1, contentBottom - contentTop);
    }

    private int panelWidth() {
        return Math.min(PANEL_WIDTH, Math.max(280, width - 20));
    }

    private int panelHeight() {
        return Math.min(PANEL_HEIGHT, Math.max(248, height - 20));
    }

    private int panelX() {
        return (width - panelWidth()) / 2;
    }

    private int panelY() {
        return (height - panelHeight()) / 2;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
