package com.example.immortal_cultivation_mod.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.Util;

/** Enlightenment is a mandatory flow; only its explicit progression buttons close a step. */
public abstract class EnlightenmentSelectionScreen extends Screen {
    protected long animationStarted = Util.getMillis();
    protected EnlightenmentSelectionScreen(Component title) { super(title); }
    protected float animationSeconds() { return Math.max(0, (Util.getMillis() - animationStarted) / 1000F); }
    protected float revealSeconds() { return 0; }
    protected boolean animationReady() { return animationSeconds() >= revealSeconds(); }
    protected void restartAnimation() { animationStarted = Util.getMillis(); }
    private void revealNow() { animationStarted = Util.getMillis() - (long)(revealSeconds() * 1000) - 1; tick(); }
    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (!animationReady() && (key == 32 || key == 257)) { revealNow(); return true; }
        return super.keyPressed(key, scanCode, modifiers);
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        float s = AwakeningVisuals.scale(width, height);
        if (button == 0 && !animationReady() && Math.abs(x - width / 2.) < 260 * s && Math.abs(y - height / 2.) < 260 * s) {
            revealNow(); return true;
        }
        return super.mouseClicked(x, y, button);
    }
    @Override public final boolean shouldCloseOnEsc() { return false; }
    @Override public final void onClose() { }
}
