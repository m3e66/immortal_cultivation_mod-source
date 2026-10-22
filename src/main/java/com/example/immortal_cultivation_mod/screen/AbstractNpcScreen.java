package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.client.NpcClientData;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

abstract class AbstractNpcScreen extends Screen implements NpcDataScreen {
    protected static final int PANEL_WIDTH = 240;
    protected static final int PANEL_HEIGHT = 350;
    protected static final int HEADER_HEIGHT = 82;
    protected static final int COLOR_INK = 0xFF2C312D;
    protected static final int COLOR_MUTED = 0xFF69706A;
    protected static final int COLOR_GOLD = 0xFFC6A867;
    protected static final int COLOR_GREEN = 0xFF315B50;

    protected NpcClientData data;
    private int heartbeatTicks;
    private int statusTicks;

    protected AbstractNpcScreen(NpcClientData data, Component title) {
        super(title);
        this.data = data;
        this.statusTicks = hasStatus(data) ? 80 : 0;
    }

    @Override
    protected final void init() {
        clearWidgets();
        buildContent();
        addRenderableWidget(new NpcButton(
                panelX() + panelWidth() - 25,
                panelY() + 8,
                17,
                17,
                Component.literal("X"),
                this::closeInteraction,
                true));
    }

    protected abstract void buildContent();

    protected abstract void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);

    protected void renderForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    protected boolean shouldRenderStatus(String statusKey) {
        return true;
    }

    protected final NpcButton addNpcButton(int x, int y, int width, int height, Component label, Runnable action) {
        NpcButton button = new NpcButton(x, y, width, height, label, action, false);
        addRenderableWidget(button);
        return button;
    }

    protected final void sendAction(String action) {
        sendAction(action, -1);
    }

    protected final void sendAction(String action, int index) {
        PacketDistributor.sendToServer(new ModPayloads.ServerboundNpcActionPayload(data.entityId(), action, index));
    }

    protected final void closeInteraction() {
        sendAction("END");
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    protected final int panelWidth() {
        return PANEL_WIDTH;
    }

    protected final int panelHeight() {
        return PANEL_HEIGHT;
    }

    // Keep the complete layout (including option pages) intact on small GUI viewports.
    // Input must use this same transform; shrinking only the background clips controls.
    private float panelScale() {
        return Math.min(1.0F, Math.min(Math.max(1, width - 16) / (float) PANEL_WIDTH,
                Math.max(1, height - 16) / (float) PANEL_HEIGHT));
    }

    protected final int panelX() {
        return (int) ((width - 8) / panelScale()) - panelWidth();
    }

    protected final int panelY() {
        return (int) ((height / panelScale() - panelHeight()) / 2);
    }

    protected final int contentX() {
        return panelX() + 16;
    }

    protected final int contentWidth() {
        return panelWidth() - 32;
    }

    protected final int contentY() {
        return panelY() + HEADER_HEIGHT + 13;
    }

    protected final int contentBottom() {
        return panelY() + panelHeight() - 36;
    }

    @Override
    public final void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float scale = panelScale();
        int localMouseX = (int) Math.floor(mouseX / scale);
        int localMouseY = (int) Math.floor(mouseY / scale);
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        try {
            renderPanel(graphics);
            renderContent(graphics, localMouseX, localMouseY, partialTick);
            super.render(graphics, localMouseX, localMouseY, partialTick);
            renderForeground(graphics, localMouseX, localMouseY, partialTick);
            renderStatus(graphics);
        } finally {
            graphics.pose().popPose();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX / panelScale(), mouseY / panelScale(), button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX / panelScale(), mouseY / panelScale(), button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX / panelScale(), mouseY / panelScale());
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        float scale = panelScale();
        return super.mouseDragged(mouseX / scale, mouseY / scale, button, dragX / scale, dragY / scale);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return super.mouseScrolled(mouseX / panelScale(), mouseY / panelScale(), scrollX, scrollY);
    }

    private void renderPanel(GuiGraphics graphics) {
        int x = panelX();
        int y = panelY();
        int w = panelWidth();
        int h = panelHeight();

        graphics.fill(x - 3, y + 3, x + w + 3, y + h + 4, 0x66000000);
        CultivationGuiTextures.drawNpc(graphics, x, y, w, h);
        graphics.fill(x + 10, y + 10, x + w - 10, y + HEADER_HEIGHT, 0xDDF0EBD9);
        graphics.renderOutline(x + 10, y + 10, w - 20, HEADER_HEIGHT - 10, 0xFF9A8E75);

        int portraitX = x + 16;
        int portraitY = y + 18;
        graphics.fill(portraitX, portraitY, portraitX + 40, portraitY + 40, 0xFFDFD4B7);
        graphics.renderOutline(portraitX, portraitY, 40, 40, COLOR_GOLD);
        String initial = data.name().isBlank() ? "?" : data.name().substring(0, 1);
        drawCenteredNoShadow(graphics, Component.literal(initial), portraitX + 20, portraitY + 16, 0xFF315B50);

        int detailsX = x + 62;
        int detailsWidth = w - 77;
        drawFittedNoShadow(graphics, Component.literal(data.name()), detailsX, y + 16,
                detailsWidth, COLOR_INK);
        Component identity = Component.translatable("screen." + ImmortalCultivationMod.MODID + ".npc.identity." + data.identity());
        Component personality = Component.translatable("screen." + ImmortalCultivationMod.MODID + ".npc.personality." + data.personality());
        drawFittedNoShadow(graphics, CultivationLevels.displayName(data.realm()).copy().append("  ").append(personality),
                detailsX, y + 29, detailsWidth, COLOR_GREEN);
        drawFittedNoShadow(graphics, identity, detailsX, y + 42, detailsWidth, COLOR_MUTED);

        drawBar(graphics, detailsX, y + 55, detailsWidth, 7,
                data.maxHealth() <= 0.0F ? 0.0F : data.health() / data.maxHealth(), 0xFF9F3F3F);
        drawBar(graphics, detailsX, y + 64, detailsWidth, 5,
                data.maxQi() <= 0 ? 0.0F : (float) data.qi() / data.maxQi(), 0xFF3F83A4);
        drawBar(graphics, detailsX, y + 71, detailsWidth, 5,
                data.maxThoughts() <= 0 ? 0.0F : (float) data.thoughts() / data.maxThoughts(), 0xFF76558F);
    }

    private void drawBar(GuiGraphics graphics, int x, int y, int width, int height, float ratio, int color) {
        graphics.fill(x, y, x + width, y + height, 0xFF17231F);
        graphics.fill(x + 1, y + 1, x + 1 + Math.round((width - 2) * Math.max(0.0F, Math.min(1.0F, ratio))),
                y + height - 1, color);
        graphics.renderOutline(x, y, width, height, 0xFF927F58);
    }

    private void renderStatus(GuiGraphics graphics) {
        if (statusTicks <= 0 || data.statusKey() == null || data.statusKey().isBlank()
                || !shouldRenderStatus(data.statusKey())) {
            return;
        }
        int x = contentX();
        int w = contentWidth();
        Component message = Component.translatable("screen." + ImmortalCultivationMod.MODID + "." + data.statusKey());
        var lines = font.split(message, w - 10);
        int boxHeight = Math.max(20, lines.size() * 10 + 10);
        int y = panelY() + panelHeight() - 9 - boxHeight;
        graphics.fill(x, y, x + w, y + boxHeight, 0xFFE1D8BD);
        graphics.renderOutline(x, y, w, boxHeight, 0xFFB5A681);
        int lineY = y + 5;
        for (var line : lines) {
            graphics.drawString(font, line, x + (w - font.width(line)) / 2, lineY, COLOR_INK, false);
            lineY += 10;
        }
    }

    protected final void drawSectionTitle(GuiGraphics graphics, Component title, int y) {
        int x = contentX();
        int w = contentWidth();
        graphics.fill(x, y + 4, x + w, y + 5, 0xFFCEC3A7);
        int textWidth = font.width(title) + 18;
        graphics.fill(x + (w - textWidth) / 2, y, x + (w + textWidth) / 2, y + 11, 0xFFF0EBD9);
        drawCenteredNoShadow(graphics, title, x + w / 2, y + 1, COLOR_GREEN);
    }

    protected final void drawCenteredNoShadow(GuiGraphics graphics, Component text, int centerX, int y, int color) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    protected final void drawFittedNoShadow(GuiGraphics graphics, Component text, int x, int y,
                                            int maxWidth, int color) {
        int textWidth = font.width(text);
        if (textWidth <= maxWidth) {
            graphics.drawString(font, text, x, y, color, false);
            return;
        }
        float scale = Math.max(0.62F, maxWidth / (float) textWidth);
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, Math.round(x / scale), Math.round(y / scale), color, false);
        graphics.pose().popPose();
    }

    protected final void drawWrapped(GuiGraphics graphics, Component text, int x, int y, int width, int color, int maxLines) {
        int lineY = y;
        int lines = 0;
        for (var line : font.split(text, width)) {
            graphics.drawString(font, line, x, lineY, color, false);
            lineY += 11;
            if (++lines >= maxLines) {
                break;
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (statusTicks > 0) {
            statusTicks--;
        }
        heartbeatTicks++;
        if (heartbeatTicks >= 40) {
            heartbeatTicks = 0;
            sendAction("HEARTBEAT");
        }
        if (minecraft != null && minecraft.level != null && minecraft.level.getEntity(data.entityId()) == null) {
            closeInteraction();
        }
    }

    @Override
    public int npcEntityId() {
        return data.entityId();
    }

    @Override
    public void updateNpcData(NpcClientData updated) {
        data = updated;
        statusTicks = hasStatus(updated) ? 80 : 0;
        if (minecraft != null) {
            init(minecraft, width, height);
        }
    }

    private static boolean hasStatus(NpcClientData data) {
        return data.statusKey() != null && !data.statusKey().isBlank();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    protected static final class NpcButton extends AbstractButton {
        private final Runnable action;
        private final boolean closeButton;

        private NpcButton(int x, int y, int width, int height, Component message, Runnable action, boolean closeButton) {
            super(x, y, width, height, message);
            this.action = action;
            this.closeButton = closeButton;
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            CultivationGuiTextures.drawButton(graphics, getX(), getY(), width, height,
                    active, isHoveredOrFocused());
            CultivationGuiTextures.drawButtonText(graphics, getMessage(), getX(), getY(),
                    width, height, active);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
