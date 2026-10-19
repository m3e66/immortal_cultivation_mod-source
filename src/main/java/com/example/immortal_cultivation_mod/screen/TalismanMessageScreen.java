package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.network.TalismanPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Small two-field composer shared by both message talismans. */
public final class TalismanMessageScreen extends Screen {
    private final boolean longRange;
    private EditBox targetBox;
    private EditBox messageBox;

    public TalismanMessageScreen(boolean longRange) {
        super(Component.translatable(longRange ? "item.immortal_cultivation_mod.qianli_chuanxun_talisman"
                : "item.immortal_cultivation_mod.chuanyin_talisman"));
        this.longRange = longRange;
    }

    @Override
    protected void init() {
        int panelX = width / 2 - 150;
        int panelY = height / 2 - 72;
        targetBox = new EditBox(font, panelX + 18, panelY + 34,
                264, 20, text("target"));
        targetBox.setMaxLength(16);
        targetBox.setHint(text("target_hint"));
        addRenderableWidget(targetBox);
        messageBox = new EditBox(font, panelX + 18, panelY + 70,
                264, 20, text("content"));
        messageBox.setMaxLength(200);
        messageBox.setHint(text("content_hint"));
        addRenderableWidget(messageBox);
        addRenderableWidget(Button.builder(
                text("send"), button -> submit())
                .bounds(panelX + 18, panelY + 108, 124, 20).build());
        addRenderableWidget(Button.builder(
                Component.translatable("gui.cancel"), button -> onClose())
                .bounds(panelX + 158, panelY + 108, 124, 20).build());
        setInitialFocus(targetBox);
    }

    private void submit() {
        if (targetBox.getValue().trim().isEmpty()
                || messageBox.getValue().trim().isEmpty()) {
            return;
        }
        PacketDistributor.sendToServer(
                new TalismanPayloads.ServerboundSendMessage(
                        targetBox.getValue(), messageBox.getValue(), longRange));
        onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            submit();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(
            GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int panelX = width / 2 - 150;
        int panelY = height / 2 - 72;
        graphics.fill(panelX, panelY, panelX + 300, panelY + 146,
                0xE015120C);
        graphics.renderOutline(panelX, panelY, 300, 146, 0xFFD9B967);
        graphics.drawCenteredString(font, title,
                width / 2, panelY + 12, 0xFFF5D982);
        graphics.drawString(font, text("target"), panelX + 18,
                panelY + 23, 0xFFD8C9A2, false);
        graphics.drawString(font, text("content"), panelX + 18,
                panelY + 59, 0xFFD8C9A2, false);
        graphics.drawCenteredString(font,
                text("range", longRange ? 1000 : 100),
                width / 2, panelY + 94, 0xFFB9AA86);

        // Screen.render() reapplies Minecraft's framebuffer blur. Rendering it
        // here would blur the panel and its labels while leaving only the
        // widgets sharp. The background blur has already been drawn above, so
        // render the widgets directly on top of the crisp foreground instead.
        for (net.minecraft.client.gui.components.Renderable renderable
                : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static Component text(String suffix, Object... args) {
        return Component.translatable("screen.immortal_cultivation_mod.talisman_message." + suffix, args);
    }
}
