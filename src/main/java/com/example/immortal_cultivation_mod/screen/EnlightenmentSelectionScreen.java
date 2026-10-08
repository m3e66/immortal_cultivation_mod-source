package com.example.immortal_cultivation_mod.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Enlightenment is a mandatory flow; only its explicit progression buttons close a step. */
public abstract class EnlightenmentSelectionScreen extends Screen {
    protected EnlightenmentSelectionScreen(Component title) { super(title); }
    @Override public final boolean shouldCloseOnEsc() { return false; }
    @Override public final void onClose() { }
}
