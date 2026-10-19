package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.attachment.BodyTypes;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

/** Dantian ignition and twelve traveling meridians reveal the actual rolled physique. */
public class BodyTypeSelectionScreen extends EnlightenmentSelectionScreen {
    private static final int MAX_REFRESHES = 3;
    private final RandomSource random = RandomSource.create();
    private final List<String> roots;
    private final String grade;
    private String selectedBodyType;
    private int refreshesLeft = MAX_REFRESHES;
    private Button refresh, proceed;

    public BodyTypeSelectionScreen(List<String> roots, String grade) {
        super(Component.translatable("screen.immortal_cultivation_mod.body_type_selection"));
        this.roots = SpiritRoots.sanitizeRootList(roots);
        this.grade = SpiritRoots.sanitizeGrade(grade);
        selectedBodyType = BodyTypes.random(random);
    }

    @Override protected float revealSeconds() { return AwakeningVisuals.BODY_REVEAL; }

    @Override protected void init() {
        clearWidgets();
        int size = Math.max(12, Math.round(48 * AwakeningVisuals.scale(width, height)));
        int y = AwakeningVisuals.screenY(width, height, 665) - size / 2;
        refresh = addRenderableWidget(new AwakeningVisuals.IconButton(
                AwakeningVisuals.screenX(width, height, 582) - size / 2, y, size,
                Component.translatable("screen.immortal_cultivation_mod.refresh_count", refreshesLeft),
                button -> refreshBodyType(), true, () -> refreshesLeft));
        proceed = addRenderableWidget(new AwakeningVisuals.IconButton(
                AwakeningVisuals.screenX(width, height, 698) - size / 2, y, size,
                Component.translatable("screen.immortal_cultivation_mod.continue_button"),
                button -> Minecraft.getInstance().setScreen(new GenderSelectionScreen(roots, grade, selectedBodyType)),
                false, () -> 0));
        tick();
    }

    @Override public void tick() {
        if (refresh != null) refresh.active = animationReady() && refreshesLeft > 0;
        if (proceed != null) proceed.active = animationReady();
    }

    private void refreshBodyType() {
        if (!animationReady() || refreshesLeft <= 0) return;
        refreshesLeft--;
        selectedBodyType = BodyTypes.random(random);
        restartAnimation(); init();
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float time = animationSeconds();
        AwakeningVisuals.body(g, width, height, time, roots, selectedBodyType);
        super.render(g, mouseX, mouseY, partialTick);
    }
    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}
    @Override public boolean isPauseScreen() { return false; }
}
