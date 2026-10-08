package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ring.RingAiConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

public final class RingAiSettingsScreen extends FittedMenuScreen {
    private final Screen parent;
    private EditBox endpoint, model, key;
    private CultivationGuiButton toggle, provider;
    private boolean enabled;
    private RingAiConfig.Protocol protocol;
    private int scroll;
    private final java.util.Map<net.minecraft.client.gui.components.AbstractWidget, Integer> rowWidgets = new java.util.LinkedHashMap<>();
    private Component status = Component.empty();
    public RingAiSettingsScreen(Screen parent) { super(Component.translatable("ring.ai.settings"), 510, 360); this.parent = parent; }
    public static boolean hostMayConfigure() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        return mc.getCurrentServer() == null;
    }
    @Override protected void initMenu() {
        rowWidgets.clear();
        var settings = RingAiConfig.get();
        String oldEndpoint = endpoint == null ? settings.endpoint() : endpoint.getValue();
        String oldModel = model == null ? settings.model() : model.getValue();
        String oldKey = key == null ? settings.apiKey() : key.getValue();
        if (endpoint == null) { enabled = settings.enabled(); protocol = settings.protocol(); }
        int w = Math.max(100, Math.min(400, width - 60));
        int x = (width - w) / 2;
        toggle = addRenderableWidget(new CultivationGuiButton(x, 56 - scroll, w, 22, toggleLabel(), () -> {
            enabled = !enabled; toggle.setMessage(toggleLabel());
        }).setTextScale(1.0F));
        toggle.active = hostMayConfigure();
        provider = addRenderableWidget(new CultivationGuiButton(x, 84 - scroll, w, 22,
                Component.translatable("ring.ai.provider", protocol.name()), () -> {
            protocol = RingAiConfig.Protocol.values()[(protocol.ordinal() + 1) % RingAiConfig.Protocol.values().length];
            provider.setMessage(Component.translatable("ring.ai.provider", protocol.name()));
            endpoint.setValue(switch (protocol) {
                case OPENAI -> "https://api.openai.com/v1/chat/completions";
                case ANTHROPIC -> "https://api.anthropic.com/v1/messages";
                case GEMINI -> "https://generativelanguage.googleapis.com/v1beta";
            });
            // Prevent sending a previous provider's key to the newly selected endpoint.
            key.setValue(""); model.setValue("");
        }).setTextScale(1.0F));
        provider.active = hostMayConfigure();
        endpoint = field(x, 136 - scroll, w, 512, oldEndpoint, "endpoint");
        model = field(x, 192 - scroll, w, 128, oldModel, "model");
        key = field(x, 248 - scroll, w, 512, oldKey, "key");
        key.setFormatter((s, cursor) -> FormattedCharSequence.forward("*".repeat(s.length()), Style.EMPTY));
        for (var child : children()) if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget)
            rowWidgets.put(widget, widget.getY() + scroll);
        addRenderableWidget(new CultivationGuiButton(x, height - 34, w / 2 - 4, 22,
                Component.translatable("ring.ai.save"), this::save).setTextScale(1.0F)).active = hostMayConfigure();
        addRenderableWidget(new CultivationGuiButton(x + w / 2 + 4, height - 34, w / 2 - 4, 22,
                Component.translatable("gui.back"), this::onClose).setTextScale(1.0F));
        scroll = net.minecraft.util.Mth.clamp(scroll, 0, Math.max(0, 286 - (height - 64)));
        updateVisibility();
    }
    private EditBox field(int x, int y, int w, int limit, String value, String name) {
        EditBox field = new EditBox(font, x, y, w, 24, Component.translatable("ring.ai." + name));
        field.setMaxLength(limit); field.setValue(value); field.setEditable(hostMayConfigure());
        return addRenderableWidget(field);
    }
    private Component toggleLabel() { return Component.translatable("ring.ai.toggle", Component.translatable(enabled ? "options.on" : "options.off")); }
    private void save() {
        if (!hostMayConfigure()) return;
        try {
            RingAiConfig.save(new RingAiConfig.Settings(enabled, endpoint.getValue(), model.getValue(), key.getValue(), protocol));
            status = Component.translatable("ring.ai.saved");
        } catch (Exception ignored) { status = Component.translatable("ring.ai.config_error"); }
    }
    private void updateVisibility() {
        rowWidgets.forEach((w, baseY) -> {
            w.setY(baseY - scroll);
            w.visible = w.getY() >= 48 && w.getY() + w.getHeight() <= height - 64;
            if (!w.visible && getFocused() == w) setFocused(null);
        });
    }
    @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
        int next = net.minecraft.util.Mth.clamp(scroll - (int)(dy * 32), 0, Math.max(0, 286 - (height - 64)));
        if (next != scroll) { scroll = next; updateVisibility(); }
        return true;
    }
    @Override protected void renderMenu(GuiGraphics g, int mouseX, int mouseY, float partial) {
        g.fill(0, 0, width, height, 0xA0101716);
        CultivationGuiTextures.drawLandscape(g, (width - Math.min(510, width - 20)) / 2, 8,
                Math.min(510, width - 20), height - 16);
        g.drawCenteredString(font, title, width / 2, 18, 0xFF38352E);
        g.drawCenteredString(font, Component.translatable(hostMayConfigure() ? "ring.ai.notice" : "ring.ai.host_only"), width / 2, 34, 0xFF645C4D);
        g.enableScissor(0, 48, width, height - 64);
        int x = Math.max(20, (width - Math.min(400, width - 60)) / 2);
        for (int i = 0; i < 3; i++) g.drawString(font, Component.translatable("ring.ai." + new String[]{"endpoint", "model", "key"}[i]), x, 122 + i * 56 - scroll, 0xFF38352E);
        g.disableScissor();
        int maxScroll = Math.max(0, 286 - (height - 64));
        if (maxScroll > 0) {
            int track = height - 112;
            int thumb = Math.max(18, track * track / (track + maxScroll));
            int y = 48 + scroll * (track - thumb) / maxScroll;
            g.fill(width - 12, y, width - 8, y + thumb, 0xAA897451);
        }
        super.renderMenu(g, mouseX, mouseY, partial);
        g.drawCenteredString(font, status, width / 2, height - 58, 0xFF52643F);
    }
    // Match cultivation menus: vanilla Screen.render must not blur already-drawn labels/background.
    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
