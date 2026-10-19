package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.config.ModClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Local visual preferences do not send gameplay changes to the server. */
public final class PillToxicitySettingsScreen extends FittedMenuScreen {
    private final Screen parent;
    private CultivationGuiButton enabled;
    public PillToxicitySettingsScreen(Screen parent) {
        super(Component.translatable("toxicity.config.title"), 436, 320);
        this.parent = parent;
    }
    @Override protected void initMenu() {
        int x = (width - 424) / 2, y = (height - 308) / 2;
        enabled = new CultivationGuiButton(x + 32, y + 63, 360, 24, enabledLabel(), () -> {
            ModClientConfig.setPillToxicityOverlay(!ModClientConfig.PILL_TOXICITY_OVERLAY.get());
            enabled.setMessage(enabledLabel());
        });
        enabled.setTooltip(Tooltip.create(Component.translatable("toxicity.config.enabled.description")));
        addRenderableWidget(enabled);
        addRenderableWidget(new OpacitySlider(x + 32, y + 100));
        addRenderableWidget(new CultivationGuiButton((width - 100) / 2, y + 276, 100, 20,
                Component.translatable("gui.back"), this::onClose));
    }
    private Component enabledLabel() {
        return Component.translatable("toxicity.config.enabled.value", Component.translatable(
                "screen.immortal_cultivation_mod.settings." + (ModClientConfig.PILL_TOXICITY_OVERLAY.get() ? "on" : "off")));
    }
    @Override protected void renderMenu(GuiGraphics g, int mx, int my, float partial) {
        int x = (width - 424) / 2, y = (height - 308) / 2;
        g.fill(0, 0, width, height, 0xA0101716);
        CultivationGuiTextures.drawLandscape(g, x, y, 424, 308);
        g.drawString(font, title, x + 212 - font.width(title) / 2, y + 17, 0xFF554A61, false);
        int lineY = y + 139;
        for (var line : font.split(Component.translatable("toxicity.config.opacity.description"), 360)) {
            g.drawString(font, line, x + 32, lineY, 0xFF554A61, false);
            lineY += 12;
        }
        super.renderMenu(g, mx, my, partial);
    }
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float partial) {}
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }

    private static final class OpacitySlider extends AbstractSliderButton {
        OpacitySlider(int x, int y) {
            super(x, y, 360, 24, Component.empty(), ModClientConfig.PILL_TOXICITY_OPACITY.get());
            updateMessage();
            setTooltip(Tooltip.create(Component.translatable("toxicity.config.opacity.description")));
        }
        @Override protected void updateMessage() {
            setMessage(Component.translatable("toxicity.config.opacity.value", Math.round(value * 100)));
        }
        @Override protected void applyValue() { ModClientConfig.setPillToxicityOpacity(value); }
        @Override public void renderWidget(GuiGraphics g, int mx, int my, float partial) {
            CultivationGuiTextures.drawButton(g, getX(), getY(), width, height, active, isHoveredOrFocused());
            var font = net.minecraft.client.Minecraft.getInstance().font;
            g.drawString(font, getMessage(), getX() + (width - font.width(getMessage())) / 2,
                    getY() + 7, 0xFFF8F3DF, false);
            int position = getX() + 8 + (int)((width - 16) * value);
            g.fill(getX() + 8, getY() + height - 5, position, getY() + height - 3, 0xFFB9ABD0);
        }
    }
}
