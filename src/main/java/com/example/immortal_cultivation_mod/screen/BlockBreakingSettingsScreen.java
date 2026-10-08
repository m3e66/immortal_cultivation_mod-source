package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.config.BlockBreakingSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.LinkedHashMap;
import java.util.Map;

/** Per-player terrain damage choices, using the same fitted layout as cultivation settings. */
public final class BlockBreakingSettingsScreen extends FittedMenuScreen {
    private static final String PREFIX = "screen." + ImmortalCultivationMod.MODID + ".settings.block_breaking";
    private static final String SETTINGS_PREFIX = "screen." + ImmortalCultivationMod.MODID + ".settings.";
    private static final int PANEL_WIDTH = 430;
    private static final int PANEL_HEIGHT = 385;
    private static final int ROW_PITCH = 30;
    private static final int BUTTON_HEIGHT = 22;
    private final Screen parent;
    private final Map<AbstractWidget, Integer> rowWidgets = new LinkedHashMap<>();
    private int scroll;

    public BlockBreakingSettingsScreen(Screen parent) {
        super(Component.translatable(PREFIX), PANEL_WIDTH + 20, PANEL_HEIGHT + 20);
        this.parent = parent;
    }

    @Override
    protected void initMenu() {
        rowWidgets.clear();
        int x = panelX() + 50;
        int buttonWidth = panelWidth() - 100;
        int index = 0;
        for (BlockBreakingSettings.Category category : BlockBreakingSettings.Category.values()) {
            int y = contentTop() + index++ * ROW_PITCH + 2;
            CultivationGuiButton button = new CultivationGuiButton(x, y, buttonWidth, BUTTON_HEIGHT,
                    label(category), () -> {
                        ClientData.setBlockBreaking(category, !ClientData.blockBreaking(category));
                        updateLabels();
                    }).setTextScale(1.0F);
            button.setTooltip(Tooltip.create(Component.translatable(PREFIX + "." + category.id() + ".description")));
            addRenderableWidget(button);
            rowWidgets.put(button, y);
        }
        addRenderableWidget(new CultivationGuiButton(panelX() + panelWidth() / 2 - 55,
                panelY() + panelHeight() - 29, 110, BUTTON_HEIGHT,
                Component.translatable("gui.back"), this::onClose).setTextScale(1.0F));
        updateScroll();
    }

    private Component label(BlockBreakingSettings.Category category) {
        return Component.translatable(PREFIX + "." + category.id(),
                Component.translatable(SETTINGS_PREFIX + (ClientData.blockBreaking(category) ? "on" : "off")));
    }

    private void updateLabels() {
        int index = 0;
        for (AbstractWidget widget : rowWidgets.keySet()) {
            widget.setMessage(label(BlockBreakingSettings.Category.values()[index++]));
        }
    }

    private void updateScroll() {
        scroll = Mth.clamp(scroll, 0, maxScroll());
        int bottom = contentTop() + contentHeight();
        rowWidgets.forEach((widget, y) -> {
            widget.setY(y - scroll);
            widget.visible = widget.getY() >= contentTop()
                    && widget.getY() + widget.getHeight() <= bottom;
            if (!widget.visible && getFocused() == widget) setFocused(null);
        });
    }

    @Override
    protected void renderMenu(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int left = panelX();
        int top = panelY();
        graphics.fill(0, 0, width, height, 0xA0101716);
        CultivationGuiTextures.drawLandscape(graphics, left, top, panelWidth(), panelHeight());
        drawCentered(graphics, title, top + 20, 0xFF38352E);
        var notice = font.split(Component.translatable(PREFIX + ".notice"), panelWidth() - 80);
        for (int i = 0; i < Math.min(2, notice.size()); i++) {
            var line = notice.get(i);
            graphics.drawString(font, line, left + panelWidth() / 2 - font.width(line) / 2,
                    top + 38 + i * 10, 0xFF645C4D, false);
        }
        graphics.enableScissor(left + 20, contentTop(), left + panelWidth() - 20,
                contentTop() + contentHeight());
        for (int i = 0; i < BlockBreakingSettings.Category.values().length; i++) {
            int y = contentTop() + i * ROW_PITCH - scroll;
            graphics.fill(left + 39, y, left + panelWidth() - 39, y + 26, 0x22A89B7D);
            graphics.renderOutline(left + 39, y, panelWidth() - 78, 26, 0x2C897451);
        }
        graphics.disableScissor();
        if (maxScroll() > 0) {
            int viewport = contentHeight();
            int full = viewport + maxScroll();
            int thumb = Math.max(18, viewport * viewport / full);
            int y = contentTop() + scroll * (viewport - thumb) / maxScroll();
            graphics.fill(left + panelWidth() - 22, y,
                    left + panelWidth() - 18, y + thumb, 0xAA897451);
        }
        super.renderMenu(graphics, mouseX, mouseY, partialTick);
    }

    private void drawCentered(GuiGraphics graphics, Component text, int y, int color) {
        int textWidth = font.width(text);
        float scale = Math.min(1.0F, (panelWidth() - 70) / (float) Math.max(1, textWidth));
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text,
                Math.round((panelX() + panelWidth() / 2.0F) / scale - textWidth / 2.0F),
                Math.round(y / scale), color, false);
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
        scroll -= (int) (dy * ROW_PITCH);
        updateScroll();
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == 266 || key == 267 || key == 268 || key == 269) {
            scroll = key == 268 ? 0 : key == 269 ? Integer.MAX_VALUE
                    : scroll + (key == 266 ? -1 : 1) * contentHeight();
            updateScroll();
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    private int contentTop() { return panelY() + 66; }
    private int contentHeight() { return Math.max(1, panelHeight() - 118); }
    private int maxScroll() {
        return Math.max(0, BlockBreakingSettings.Category.values().length * ROW_PITCH - contentHeight());
    }
    private int panelWidth() { return Math.min(PANEL_WIDTH, Math.max(280, width - 20)); }
    private int panelHeight() { return Math.min(PANEL_HEIGHT, Math.max(248, height - 20)); }
    private int panelX() { return (width - panelWidth()) / 2; }
    private int panelY() { return (height - panelHeight()) / 2; }
    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
