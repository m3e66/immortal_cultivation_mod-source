package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.attachment.BodyTypes;
import com.example.immortal_cultivation_mod.client.ClientData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Read-only explanation of the player's saved physique. */
public final class BodyTypeDetailsScreen extends FittedMenuScreen {
    private static final int WINDOW_W = 360, WINDOW_H = 270;
    private static final int CONTENT_H = 144, LINE_H = 13;
    private final Screen parent;
    private int scroll, maxScroll;

    public BodyTypeDetailsScreen(Screen parent) {
        super(Component.translatable("screen.immortal_cultivation_mod.body_details"), WINDOW_W + 12, WINDOW_H + 12);
        this.parent = parent;
    }

    @Override protected void initMenu() {
        addRenderableWidget(new CultivationGuiButton((width - 110) / 2,
                (height - WINDOW_H) / 2 + 239, 110, 20,
                Component.translatable("gui.back"), this::onClose));
    }

    @Override protected void renderMenu(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = (width - WINDOW_W) / 2, y = (height - WINDOW_H) / 2;
        g.fill(0, 0, width, height, 0xA0101716);
        CultivationGuiTextures.drawLandscape(g, x, y, WINDOW_W, WINDOW_H);
        g.drawCenteredString(font, title, x + WINDOW_W / 2, y + 17, 0xFF38352E);
        var data = ClientData.cultivationData;
        if (data != null) {
            g.drawCenteredString(font, BodyTypes.displayName(data.bodyType()), x + WINDOW_W / 2, y + 42, 0xFF4F645B);
            g.drawCenteredString(font, BodyTypes.rarityName(data.bodyType()), x + WINDOW_W / 2, y + 58, 0xFF9B7C43);
            g.fill(x + 24, y + 76, x + WINDOW_W - 24, y + 77, 0xFFAAA18F);
            var lines = font.split(BodyTypes.description(data.bodyType()), WINDOW_W - 64);
            maxScroll = Math.max(0, lines.size() * LINE_H - CONTENT_H);
            scroll = Math.clamp(scroll, 0, maxScroll);
            g.enableScissor(x + 26, y + 84, x + WINDOW_W - 26, y + 84 + CONTENT_H);
            for (int i = 0; i < lines.size(); i++)
                g.drawString(font, lines.get(i), x + 32, y + 86 + i * LINE_H - scroll, 0xFF38352E, false);
            g.disableScissor();
            if (maxScroll > 0) {
                int thumb = Math.max(18, CONTENT_H * CONTENT_H / (maxScroll + CONTENT_H));
                int thumbY = y + 84 + (CONTENT_H - thumb) * scroll / maxScroll;
                g.fill(x + WINDOW_W - 27, y + 84, x + WINDOW_W - 25, y + 84 + CONTENT_H, 0x3080796A);
                g.fill(x + WINDOW_W - 27, thumbY, x + WINDOW_W - 25, thumbY + thumb, 0xFF728779);
            }
        } else {
            g.drawCenteredString(font, Component.translatable("screen.immortal_cultivation_mod.no_data"),
                    x + WINDOW_W / 2, y + 130, 0xFFAA3333);
        }
        super.renderMenu(g, mouseX, mouseY, partialTick);
    }

    @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
        int next = Math.clamp(scroll - (int)Math.signum(dy) * LINE_H * 3, 0, maxScroll);
        if (next == scroll) return super.mouseScrolled(x, y, dx, dy);
        scroll = next;
        return true;
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}
    @Override public boolean isPauseScreen() { return false; }
}
