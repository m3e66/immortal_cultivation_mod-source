package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.BodyTypes;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class BodyTypeSelectionScreen extends Screen {
    private static final int WINDOW_W = 260;
    private static final int WINDOW_H = 160;
    private static final int MAX_REFRESHES = 3;

    private final RandomSource random = RandomSource.create();
    private final List<String> roots;
    private final String grade;
    private String selectedBodyType;
    private int refreshesLeft = MAX_REFRESHES;

    public BodyTypeSelectionScreen(List<String> roots, String grade) {
        super(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".body_type_selection"));
        this.roots = SpiritRoots.sanitizeRootList(roots);
        this.grade = SpiritRoots.sanitizeGrade(grade);
        rollBodyType();
    }

    @Override
    protected void init() {
        clearWidgets();
        int x = (width - WINDOW_W) / 2;
        int y = (height - WINDOW_H) / 2;

        addRenderableWidget(new SpiritRootSelectionScreen.StyledButton(
                x + 20,
                y + WINDOW_H - 32,
                100,
                20,
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".refresh_count", refreshesLeft),
                b -> refreshBodyType()
        )).active = refreshesLeft > 0;

        addRenderableWidget(new SpiritRootSelectionScreen.StyledButton(
                x + WINDOW_W - 120,
                y + WINDOW_H - 32,
                100,
                20,
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".confirm_button"),
                b -> confirm()
        ));
    }

    private void refreshBodyType() {
        if (refreshesLeft <= 0) {
            return;
        }
        refreshesLeft--;
        rollBodyType();
        init();
    }

    private void rollBodyType() {
        selectedBodyType = BodyTypes.random(random);
    }

    private void confirm() {
        PacketDistributor.sendToServer(new ModPayloads.ServerboundCompleteEnlightenmentPayload(roots, grade, selectedBodyType));
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = (width - WINDOW_W) / 2;
        int y = (height - WINDOW_H) / 2;
        Minecraft mc = Minecraft.getInstance();

        SpiritRootSelectionScreen.drawPanel(g, x, y, WINDOW_W, WINDOW_H);
        g.drawString(mc.font, title, x + 14, y + 12, 0x55DDFF, true);

        g.drawString(mc.font,
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".spirit_roots").append(": ")
                        .append(SpiritRoots.format(roots, grade)),
                x + 18, y + 46, 0x88FFCC, true);
        g.drawString(mc.font,
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".body_type").append(": "),
                x + 18, y + 70, 0xCCCCCC, true);
        g.drawString(mc.font, Component.literal(selectedBodyType), x + 18, y + 88, 0xFFDDAA, true);

        Component tries = Component.translatable("screen." + ImmortalCultivationMod.MODID + ".refreshes_left", refreshesLeft);
        g.drawString(mc.font, tries, x + 18, y + 112, 0xAAAAAA, true);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
