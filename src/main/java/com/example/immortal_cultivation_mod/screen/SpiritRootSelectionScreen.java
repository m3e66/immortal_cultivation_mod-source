package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

public class SpiritRootSelectionScreen extends Screen {
    private static final int WINDOW_W = 260;
    private static final int WINDOW_H = 150;
    private static final int MAX_REFRESHES = 3;

    private final RandomSource random = RandomSource.create();
    private SpiritRoots.RootData selectedRoots;
    private int refreshesLeft = MAX_REFRESHES;

    public SpiritRootSelectionScreen() {
        super(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".spirit_root_selection"));
        rollRoots();
    }

    @Override
    protected void init() {
        clearWidgets();
        int x = (width - WINDOW_W) / 2;
        int y = (height - WINDOW_H) / 2;

        addRenderableWidget(new StyledButton(
                x + 20,
                y + WINDOW_H - 32,
                100,
                20,
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".refresh_count", refreshesLeft),
                b -> refreshRoots()
        )).active = refreshesLeft > 0;

        addRenderableWidget(new StyledButton(
                x + WINDOW_W - 120,
                y + WINDOW_H - 32,
                100,
                20,
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".continue_button"),
                b -> Minecraft.getInstance().setScreen(new BodyTypeSelectionScreen(selectedRoots.roots(), selectedRoots.grade()))
        ));
    }

    private void refreshRoots() {
        if (refreshesLeft <= 0) {
            return;
        }
        refreshesLeft--;
        rollRoots();
        init();
    }

    private void rollRoots() {
        selectedRoots = SpiritRoots.random(random);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = (width - WINDOW_W) / 2;
        int y = (height - WINDOW_H) / 2;
        Minecraft mc = Minecraft.getInstance();

        drawPanel(g, x, y, WINDOW_W, WINDOW_H);
        g.drawString(mc.font, title, x + 14, y + 12, 0x55DDFF, true);

        Component rootLabel = Component.translatable("screen." + ImmortalCultivationMod.MODID + ".spirit_roots");
        Component rootValue = Component.literal(SpiritRoots.format(selectedRoots.roots(), selectedRoots.grade()));
        g.drawString(mc.font, rootLabel.copy().append(": "), x + 18, y + 48, 0xCCCCCC, true);
        g.drawString(mc.font, rootValue, x + 18, y + 66, 0x88FFCC, true);

        Component tries = Component.translatable("screen." + ImmortalCultivationMod.MODID + ".refreshes_left", refreshesLeft);
        g.drawString(mc.font, tries, x + 18, y + 92, 0xAAAAAA, true);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    static void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xD0181818);
        g.renderOutline(x, y, w, h, 0xFF55DDFF);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0xAA8FFFFF);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0xAA1E6F7A);
    }

    static class StyledButton extends Button {
        StyledButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            int bg = active ? 0xCC202020 : 0x88404040;
            int top = isHoveredOrFocused() && active ? 0xFF8FFFFF : 0xFF55DDFF;
            int bottom = active ? 0xFF1E6F7A : 0xFF555555;
            int text = active ? 0xFFEAFDFF : 0xFF999999;

            g.fill(x, y, x + width, y + height, bg);
            g.fill(x, y, x + width, y + 1, top);
            g.fill(x, y, x + 1, y + height, top);
            g.fill(x, y + height - 1, x + width, y + height, bottom);
            g.fill(x + width - 1, y, x + width, y + height, bottom);

            var font = Minecraft.getInstance().font;
            Component message = getMessage();
            int tx = x + (width - font.width(message)) / 2;
            int ty = y + (height - 8) / 2;
            g.drawString(font, message, tx, ty, text, false);
        }
    }
}
