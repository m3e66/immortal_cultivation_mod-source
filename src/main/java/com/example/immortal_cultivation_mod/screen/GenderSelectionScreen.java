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

/** Required symbol-only gender choice on the shared black awakening stage. */
public final class GenderSelectionScreen extends EnlightenmentSelectionScreen {
    private final List<String> roots;
    private final String grade, bodyType;
    private String gender;
    private Button confirm;

    public GenderSelectionScreen(List<String> roots, String grade, String bodyType) {
        super(Component.translatable("screen.immortal_cultivation_mod.gender_selection"));
        this.roots = SpiritRoots.sanitizeRootList(roots);
        this.grade = SpiritRoots.sanitizeGrade(grade);
        this.bodyType = BodyTypes.sanitize(bodyType);
    }

    @Override protected void init() {
        clearWidgets();
        float s = AwakeningVisuals.scale(width, height);
        for (int i = 0; i < 2; i++) {
            String value = i == 0 ? "male" : "female";
            int cx = AwakeningVisuals.screenX(width, height, i == 0 ? 520 : 760);
            int y = AwakeningVisuals.screenY(width, height, 340) - Math.round(44 * s);
            int w = Math.round(88 * s), h = Math.round(88 * s);
            addRenderableWidget(new GenderTarget(cx - w / 2, y, w, h,
                    Component.translatable("screen.immortal_cultivation_mod.npc.gender." + value), button -> {
                        gender = value; init();
                    })).active = !value.equals(gender);
        }
        int size = Math.max(12, Math.round(48 * s));
        confirm = addRenderableWidget(new AwakeningVisuals.IconButton(
                width / 2 - size / 2, AwakeningVisuals.screenY(width, height, 445) - size / 2, size,
                Component.translatable("screen.immortal_cultivation_mod.confirm_button"), button -> complete(), false, () -> 0, 0x8FFFE3));
        confirm.active = gender != null;
    }

    private void complete() {
        if (gender == null || !confirm.active) return;
        confirm.active = false;
        PacketDistributor.sendToServer(new ModPayloads.ServerboundCompleteEnlightenmentPayload(roots, grade, bodyType, gender));
        Minecraft.getInstance().setScreen(null);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        AwakeningVisuals.gender(graphics, width, height, gender);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}
    @Override public boolean isPauseScreen() { return false; }

    private static final class GenderTarget extends Button {
        GenderTarget(int x, int y, int width, int height, Component message, OnPress press) {
            super(x, y, width, height, message, press, DEFAULT_NARRATION);
        }
        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}
    }
}
