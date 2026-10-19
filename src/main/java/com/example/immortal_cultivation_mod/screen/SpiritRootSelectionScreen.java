package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

/** Ten element seals scan once, then reveal the actual rolled spirit roots. */
public class SpiritRootSelectionScreen extends EnlightenmentSelectionScreen {
    private static final int MAX_REFRESHES = 3;
    private final RandomSource random = RandomSource.create();
    private SpiritRoots.RootData selectedRoots;
    private int refreshesLeft = MAX_REFRESHES;
    private Button refresh, proceed;

    public SpiritRootSelectionScreen() {
        super(Component.translatable("screen.immortal_cultivation_mod.spirit_root_selection"));
        selectedRoots = SpiritRoots.random(random);
    }

    @Override protected float revealSeconds() { return AwakeningVisuals.ROOT_REVEAL; }

    @Override protected void init() {
        clearWidgets();
        int size = Math.max(12, Math.round(48 * AwakeningVisuals.scale(width, height)));
        int y = AwakeningVisuals.screenY(width, height, 665) - size / 2;
        refresh = addRenderableWidget(new AwakeningVisuals.IconButton(
                AwakeningVisuals.screenX(width, height, 582) - size / 2, y, size,
                Component.translatable("screen.immortal_cultivation_mod.refresh_count", refreshesLeft),
                button -> refreshRoots(), true, () -> refreshesLeft));
        proceed = addRenderableWidget(new AwakeningVisuals.IconButton(
                AwakeningVisuals.screenX(width, height, 698) - size / 2, y, size,
                Component.translatable("screen.immortal_cultivation_mod.continue_button"),
                button -> Minecraft.getInstance().setScreen(new BodyTypeSelectionScreen(selectedRoots.roots(), selectedRoots.grade())),
                false, () -> 0));
        tick();
    }

    @Override public void tick() {
        if (refresh != null) refresh.active = animationReady() && refreshesLeft > 0;
        if (proceed != null) proceed.active = animationReady();
    }

    private void refreshRoots() {
        if (!animationReady() || refreshesLeft <= 0) return;
        refreshesLeft--;
        selectedRoots = SpiritRoots.random(random);
        restartAnimation(); init();
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float time = animationSeconds();
        AwakeningVisuals.roots(g, width, height, time, selectedRoots.roots(), selectedRoots.grade());
        super.render(g, mouseX, mouseY, partialTick);
    }
    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}
    @Override public boolean isPauseScreen() { return false; }
}
