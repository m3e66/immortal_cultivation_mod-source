package com.example.immortal_cultivation_mod.screen;

import java.util.List;
import com.example.immortal_cultivation_mod.attachment.BodyTypes;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class GenderSelectionScreen extends EnlightenmentSelectionScreen {
    private static final int WIDTH = 260, HEIGHT = 146;
    private final List<String> roots;
    private final String grade, bodyType;
    private String gender;
    private Button confirm;
    public GenderSelectionScreen(List<String> roots, String grade, String bodyType) {
        super(Component.translatable("screen.immortal_cultivation_mod.gender_selection"));
        this.roots = SpiritRoots.sanitizeRootList(roots); this.grade = SpiritRoots.sanitizeGrade(grade);
        this.bodyType = BodyTypes.sanitize(bodyType);
    }
    @Override protected void init() {
        clearWidgets(); int x = (width - WIDTH) / 2, y = (height - HEIGHT) / 2;
        for (int i = 0; i < 2; i++) {
            String value = i == 0 ? "male" : "female";
            addRenderableWidget(new SpiritRootSelectionScreen.StyledButton(x + 20 + i * 120, y + 66, 100, 20,
                    Component.translatable("screen.immortal_cultivation_mod.npc.gender." + value), b -> {
                        gender = value; init();
                    })).active = !value.equals(gender);
        }
        confirm = addRenderableWidget(new SpiritRootSelectionScreen.StyledButton(x + 80, y + 112, 100, 20,
                Component.translatable("screen.immortal_cultivation_mod.confirm_button"), b -> complete()));
        confirm.active = gender != null;
    }
    private void complete() {
        if (gender == null) return;
        confirm.active = false;
        PacketDistributor.sendToServer(new ModPayloads.ServerboundCompleteEnlightenmentPayload(roots, grade, bodyType, gender));
        Minecraft.getInstance().setScreen(null);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = (width - WIDTH) / 2, y = (height - HEIGHT) / 2;
        SpiritRootSelectionScreen.drawPanel(graphics, x, y, WIDTH, HEIGHT);
        graphics.drawString(font, title, x + 14, y + 12, 0x55ddff, true);
        graphics.drawString(font, Component.translatable("screen.immortal_cultivation_mod.gender_required"), x + 18, y + 41, 0xcccccc, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { }
    @Override public boolean isPauseScreen() { return false; }
}
