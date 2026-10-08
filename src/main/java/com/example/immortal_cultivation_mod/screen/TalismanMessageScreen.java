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
        super(Component.literal(longRange ? "千里传讯符" : "传音符"));
        this.longRange = longRange;
    }

    @Override
    protected void init() {
        int panelX = width / 2 - 150;
        int panelY = height / 2 - 72;
        targetBox = new EditBox(font, panelX + 18, panelY + 34,
                264, 20, Component.literal("目标名字"));
        targetBox.setMaxLength(16);
        targetBox.setHint(Component.literal("目标玩家名字"));
        addRenderableWidget(targetBox);
        messageBox = new EditBox(font, panelX + 18, panelY + 70,
                264, 20, Component.literal("传音内容"));
        messageBox.setMaxLength(200);
        messageBox.setHint(Component.literal("输入一条短消息"));
        addRenderableWidget(messageBox);
        addRenderableWidget(Button.builder(
                Component.literal("发送"), button -> submit())
                .bounds(panelX + 18, panelY + 108, 124, 20).build());
        addRenderableWidget(Button.builder(
                Component.literal("取消"), button -> onClose())
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
        graphics.drawString(font, "目标", panelX + 18,
                panelY + 23, 0xFFD8C9A2, false);
        graphics.drawString(font, "内容", panelX + 18,
                panelY + 59, 0xFFD8C9A2, false);
        graphics.drawCenteredString(font,
                "范围：" + (longRange ? "1000" : "100") + " 格",
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
}
