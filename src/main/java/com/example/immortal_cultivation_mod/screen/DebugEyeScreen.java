package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.client.DebugEyeClient;
import com.example.immortal_cultivation_mod.client.ModKeyMappings;
import com.example.immortal_cultivation_mod.fate.DebugEyeDiagnostics;
import com.example.immortal_cultivation_mod.network.DebugEyePayloads;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Live read-only pages; closing the page leaves the Debug Eye HUD enabled. */
public final class DebugEyeScreen extends Screen {
    private static final String[] PAGES = {"luck_tab", "player_tab", "target_tab", "events_tab", "raw_tab"};
    private int page, scroll, maximumScroll;
    private boolean frozen;
    private DebugEyePayloads.Snapshot frozenSnapshot;
    public DebugEyeScreen() { super(DebugEyeDiagnostics.text("title")); }
    private DebugEyePayloads.Snapshot data() { return frozen ? frozenSnapshot : DebugEyeClient.latest(); }
    @Override protected void init() {
        int tabWidth = Math.min(105, (width - 16) / PAGES.length);
        for (int i = 0; i < PAGES.length; i++) {
            final int index = i;
            var button = Button.builder(DebugEyeDiagnostics.text(PAGES[i]), b -> {
                page = index; scroll = 0; rebuildWidgets();
            }).bounds(8 + i * tabWidth, 25, tabWidth - 2, 20).build();
            button.active = page != i;
            addRenderableWidget(button);
        }
        int buttonWidth = Math.min(110, (width - 24) / 3);
        addRenderableWidget(Button.builder(DebugEyeDiagnostics.text(frozen ? "resume" : "freeze"), b -> {
            if (!frozen) frozenSnapshot = DebugEyeClient.latest();
            frozen = !frozen; rebuildWidgets();
        }).bounds(8, height - 26, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(DebugEyeDiagnostics.text("copy"), b -> {
            if (minecraft != null) minecraft.keyboardHandler.setClipboard(String.join("\n", currentLines().stream().map(Component::getString).toList()));
        }).bounds(12 + buttonWidth, height - 26, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(width - buttonWidth - 8, height - 26, buttonWidth, 20).build());
    }
    private List<Component> currentLines() {
        var snapshot = data();
        if (snapshot == null) return List.of(DebugEyeDiagnostics.text("waiting"));
        return switch (page) {
            case 1 -> snapshot.player(); case 2 -> snapshot.target(); case 3 -> snapshot.events(); case 4 -> snapshot.raw();
            default -> snapshot.luck();
        };
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void tick() { if (!ClientData.debugEyeEnabled()) onClose(); }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xF010151B);
        graphics.drawString(font, title, 8, 9, 0xFFE5C888, false);
        var snapshot = data();
        Component status = frozen ? DebugEyeDiagnostics.text("frozen") : DebugEyeClient.stale() ? DebugEyeDiagnostics.text("stale")
                : DebugEyeDiagnostics.text("live", snapshot == null ? "—" : snapshot.tick());
        graphics.drawString(font, status, Math.max(140, width - font.width(status) - 8), 9, 0xFFB0BEC8, false);
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (Component line : currentLines()) wrapped.addAll(font.split(line, width - 32));
        int top = 53, bottom = height - 34;
        maximumScroll = Math.max(0, wrapped.size() * 12 - Math.max(1, bottom - top));
        scroll = Math.max(0, Math.min(scroll, maximumScroll));
        graphics.enableScissor(8, top, width - 12, bottom);
        for (int i = Math.max(0, scroll / 12); i < wrapped.size(); i++) {
            int y = top + i * 12 - scroll;
            if (y > bottom) break;
            graphics.drawString(font, wrapped.get(i), 10, y, 0xFFE0E5EA, false);
        }
        graphics.disableScissor();
        if (maximumScroll > 0) {
            int track = bottom - top, thumb = Math.max(12, track * track / (track + maximumScroll));
            int thumbY = top + (track - thumb) * scroll / maximumScroll;
            graphics.fill(width - 8, top, width - 5, bottom, 0xFF29343D);
            graphics.fill(width - 8, thumbY, width - 5, thumbY + thumb, 0xFFB3BDC5);
        }
        for (var widget : renderables) widget.render(graphics, mouseX, mouseY, partialTick);
    }
    @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
        scroll = Math.max(0, Math.min(maximumScroll, scroll - (int)(dy * 36))); return true;
    }
    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (ModKeyMappings.DEBUG_EYE.get().matches(key, scanCode)) {
            if (!hasShiftDown()) PacketDistributor.sendToServer(new ModPayloads.ServerboundToggleDebugEyePayload());
            onClose(); return true;
        }
        if (key == GLFW.GLFW_KEY_PAGE_DOWN) { scroll = Math.min(maximumScroll, scroll + height - 90); return true; }
        if (key == GLFW.GLFW_KEY_PAGE_UP) { scroll = Math.max(0, scroll - height + 90); return true; }
        if (key == GLFW.GLFW_KEY_HOME) { scroll = 0; return true; }
        if (key == GLFW.GLFW_KEY_END) { scroll = maximumScroll; return true; }
        return super.keyPressed(key, scanCode, modifiers);
    }
}
