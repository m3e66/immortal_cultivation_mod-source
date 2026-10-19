package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.client.hud.HudLayout;
import com.example.immortal_cultivation_mod.config.HudElement;
import com.example.immortal_cultivation_mod.config.ModClientConfig;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Edit local HUD positions without requiring every combat/status bar to be active. */
public final class HudLayoutScreen extends Screen {
    // Legacy spell/sword position settings remain loadable, but the shared dial has one control.
    private static final HudElement[] EDITABLE = java.util.Arrays.stream(HudElement.values())
            .filter(element -> element != HudElement.SPELL_CHARGE && element != HudElement.SWORD_CHARGE
                    && element != HudElement.QI_RELEASE)
            .toArray(HudElement[]::new);
    private final Screen parent;
    private final Map<HudElement, HudLayout.Position> original = HudLayout.snapshot();
    private HudElement selected = HudElement.QI;
    private boolean dragging;
    private boolean showAll;
    private double grabX, grabY;

    public HudLayoutScreen(Screen parent) {
        super(Component.translatable("hud.layout.title"));
        this.parent = parent;
    }

    @Override protected void init() {
        dragging = false;
        int center = width / 2;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> cycle(-1))
                .bounds(center - 122, 25, 20, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("hud.layout.reset_selected"), b -> HudLayout.reset(selected))
                .bounds(center - 98, 25, 96, 20).build());
        addRenderableWidget(Button.builder(previewLabel(), b -> {
            showAll = !showAll; b.setMessage(previewLabel());
        }).bounds(center + 2, 25, 96, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> cycle(1))
                .bounds(center + 102, 25, 20, 20).build());
        int buttonWidth = Math.min(100, (width - 24) / 3);
        addRenderableWidget(Button.builder(Component.translatable("hud.layout.reset_all"), b -> {
            for (var element : HudElement.values()) HudLayout.reset(element);
        }).bounds(center - buttonWidth * 3 / 2 - 4, height - 25, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(center - buttonWidth / 2, height - 25, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> {
            ModClientConfig.SPEC.save();
            minecraft.setScreen(parent);
        }).bounds(center + buttonWidth / 2 + 4, height - 25, buttonWidth, 20).build());
    }

    private void cycle(int direction) {
        selected = EDITABLE[Math.floorMod(java.util.Arrays.asList(EDITABLE).indexOf(selected) + direction, EDITABLE.length)];
        dragging = false;
    }

    private Component previewLabel() {
        return Component.translatable("hud.layout.preview_all", Component.translatable(showAll ? "options.on" : "options.off"));
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0x60101820);
        if (showAll) for (var element : EDITABLE) if (element != selected) drawPreview(g, element, false);
        drawPreview(g, selected, true);
        // Draw controls above previews so even overlapping widgets remain selectable.
        g.fill(0, 0, width, 61, 0xD0101820);
        g.fill(0, height - 28, width, height, 0xD0101820);
        var box = HudLayout.preview(selected, width, height);
        drawFitted(g, Component.translatable("hud.layout.selected",
                Component.translatable(selected.translationKey()), box.x(), box.y()), 5, 0xFFF2D78B);
        drawFitted(g, Component.translatable("hud.layout.help"), 49, 0xFFD0DFE9);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawFitted(GuiGraphics g, Component text, int y, int color) {
        float scale = Math.min(1, (width - 12F) / Math.max(1, font.width(text)));
        g.pose().pushPose();
        g.pose().translate(width / 2F, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawCenteredString(font, text, 0, 0, color);
        g.pose().popPose();
    }

    private void drawPreview(GuiGraphics g, HudElement element, boolean active) {
        var box = HudLayout.preview(element, width, height);
        int x = box.x(), y = box.y(), w = box.width(), h = box.height();
        g.fill(x, y, x + w, y + h, active ? 0xC02C434B : 0x88202730);
        g.renderOutline(x, y, w, h, active ? 0xFFFFD66A : 0x887EB0BD);
        if (h >= 15 && w > 35) {
            g.fill(x + 2, y + h - 5, x + w - 2, y + h - 2, 0xAA314B4A);
            g.fill(x + 2, y + h - 5, x + (w - 2) * 2 / 3, y + h - 2, 0xFF65CEA0);
        }
        g.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
        g.drawString(font, Component.translatable(element.translationKey()), x + 3, y + 2,
                active ? 0xFFFFFFD0 : 0xFFCCDDE2, true);
        g.disableScissor();
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        if (super.mouseClicked(x, y, button)) return true;
        if (button != 0 || y < 61 || y >= height - 28) return false;
        if (!HudLayout.preview(selected, width, height).contains(x, y)) {
            boolean found = false;
            var values = EDITABLE;
            for (int i = showAll ? values.length - 1 : -1; i >= 0; i--) {
                if (HudLayout.preview(values[i], width, height).contains(x, y)) {
                    selected = values[i]; found = true; break;
                }
            }
            if (!found) {
                var box = HudLayout.preview(selected, width, height);
                HudLayout.move(selected, x - box.width() / 2.0, y - box.height() / 2.0, width, height, box);
            }
        }
        var box = HudLayout.preview(selected, width, height);
        grabX = x - box.x(); grabY = y - box.y(); dragging = true;
        setFocused(null);
        return true;
    }

    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (dragging && button == 0) {
            HudLayout.move(selected, x - grabX, y - grabY, width, height, HudLayout.preview(selected, width, height));
            return true;
        }
        return super.mouseDragged(x, y, button, dx, dy);
    }

    @Override public boolean mouseReleased(double x, double y, int button) {
        if (button == 0 && dragging) { dragging = false; return true; }
        return super.mouseReleased(x, y, button);
    }

    @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
        if (dy != 0) cycle(dy > 0 ? -1 : 1);
        return true;
    }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        int dx = key == GLFW.GLFW_KEY_LEFT ? -1 : key == GLFW.GLFW_KEY_RIGHT ? 1 : 0;
        int dy = key == GLFW.GLFW_KEY_UP ? -1 : key == GLFW.GLFW_KEY_DOWN ? 1 : 0;
        if (dx != 0 || dy != 0) {
            var box = HudLayout.preview(selected, width, height);
            int step = hasShiftDown() ? 10 : 1;
            HudLayout.move(selected, box.x() + dx * step, box.y() + dy * step, width, height, box);
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override public void onClose() {
        original.forEach(HudLayout::set);
        minecraft.setScreen(parent);
    }
    @Override public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}
    @Override public boolean isPauseScreen() { return true; }
}
