package com.example.immortal_cultivation_mod.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Keeps fixed menu artwork and its interactive widgets in the same fitted coordinate space. */
public abstract class FittedMenuScreen extends Screen {
    private final int requiredWidth;
    private final int requiredHeight;
    private MenuViewport viewport = new MenuViewport(1, 1, 1);

    protected FittedMenuScreen(Component title, int requiredWidth, int requiredHeight) {
        super(title);
        this.requiredWidth = requiredWidth;
        this.requiredHeight = requiredHeight;
    }

    @Override
    protected final void init() {
        // Read the real viewport on widget rebuilds too; width/height already contain
        // logical dimensions after the first initialization.
        viewport = MenuViewport.fit(minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight(), requiredWidth, requiredHeight, 1);
        width = viewport.logicalWidth();
        height = viewport.logicalHeight();
        initMenu();
    }

    protected abstract void initMenu();

    @Override
    public final void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.pose().pushPose();
        try {
            float scale = (float) viewport.scale();
            graphics.pose().scale(scale, scale, 1);
            renderMenu(graphics, (int) Math.floor(viewport.toMenu(mouseX)),
                    (int) Math.floor(viewport.toMenu(mouseY)), partialTick);
        } finally {
            graphics.pose().popPose();
        }
    }

    protected void renderMenu(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        return super.mouseClicked(viewport.toMenu(x), viewport.toMenu(y), button);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        return super.mouseReleased(viewport.toMenu(x), viewport.toMenu(y), button);
    }

    @Override
    public void mouseMoved(double x, double y) {
        super.mouseMoved(viewport.toMenu(x), viewport.toMenu(y));
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return super.mouseDragged(viewport.toMenu(x), viewport.toMenu(y), button,
                viewport.toMenu(dx), viewport.toMenu(dy));
    }

    @Override
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
        return super.mouseScrolled(viewport.toMenu(x), viewport.toMenu(y), dx, dy);
    }
}
