package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.item.WeaponRestrictionSystem;
import com.example.immortal_cultivation_mod.network.DebugWeaponEditorPayloads;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.List;

public class DebugWeaponEditorScreen extends AbstractContainerScreen<DebugWeaponEditorMenu> {
    private static final int LIST_Y = 112, ROW_HEIGHT = 22;
    private EditBox durability;
    private int scroll;
    private final Button[][] layerButtons = new Button[4][4];
    private Button rankMinus, rankPlus, apply;
    private static net.minecraft.network.chat.MutableComponent text(String key) {
        return Component.translatable("screen.immortal_cultivation_mod.debug_weapon_" + key);
    }

    public DebugWeaponEditorScreen(DebugWeaponEditorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = DebugStatEditorScreen.WINDOW_W;
        imageHeight = DebugStatEditorScreen.WINDOW_H;
    }
    private Button button(int x, int y, int width, int height, Component label, Button.OnPress press) {
        return addRenderableWidget(new DebugStatEditorScreen.DebugButton(leftPos + x, topPos + y, width, height, label, press));
    }
    @Override protected void init() {
        super.init();
        for (int i = 0; i < DebugStatEditorScreen.TAB_KEYS.length; i++) {
            int page = i;
            var label = Component.literal(i == 7 ? "[X] " : "").append(Component.translatable(
                    "screen.immortal_cultivation_mod.debug_tab_" + DebugStatEditorScreen.TAB_KEYS[i]));
            button(10 + i * 37, 30, 36, 18, label, b -> {
                if (page != 7) send(3, page, 0);
            });
        }
        button(264, 5, 22, 18, Component.literal("-"), b -> adjustHints(-1));
        button(290, 5, 22, 18, Component.literal("+"), b -> adjustHints(1));
        rankMinus = button(250, 60, 22, 18, Component.literal("-"), b -> send(0, 0, -1));
        rankPlus = button(276, 60, 22, 18, Component.literal("+"), b -> send(0, 0, 1));
        durability = new EditBox(font, leftPos + 118, topPos + 88, 94, 18, text("durability")) {
            @Override public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
                super.renderWidget(g, mouseX, mouseY, partial);
                g.renderOutline(getX() - 1, getY() - 1, getWidth() + 2, getHeight() + 2,
                        isFocused() ? 0xFF8FFFFF : 0xFF1E6F7A);
            }
        };
        durability.setMaxLength(10);
        durability.setFilter(s -> s.matches("[0-9]*"));
        durability.setTextColor(0xFFEAFDFF);
        addRenderableWidget(durability);
        apply = button(220, 88, 78, 18, text("apply"), b -> {
            try { send(2, 0, Integer.parseInt(durability.getValue())); }
            catch (NumberFormatException ignored) { }
        });
        int[] deltas = {-10, -1, 1, 10};
        for (int row = 0; row < 4; row++) for (int col = 0; col < 4; col++) {
            int r = row, delta = deltas[col];
            layerButtons[row][col] = button(184 + col * 29, LIST_Y + row * ROW_HEIGHT, 27, 18,
                    Component.literal(delta > 0 ? "+" + delta : "" + delta), b -> {
                        List<WeaponRestrictionSystem.RestrictionType> types = types();
                        if (scroll + r < types.size()) send(1, types.get(scroll + r).ordinal(), delta);
                    });
        }
    }
    private void adjustHints(int delta) {
        if (minecraft.player != null) PacketDistributor.sendToServer(new ModPayloads.ServerboundDebugAdjustStatPayload(
                minecraft.player.getUUID(), "restriction_hints", delta));
    }
    private List<WeaponRestrictionSystem.RestrictionType> types() { return WeaponRestrictionSystem.availableTypes(menu.weapon()); }
    private void send(int action, int restriction, int value) {
        PacketDistributor.sendToServer(new DebugWeaponEditorPayloads.Edit(menu.containerId, action, restriction, value));
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (x >= leftPos && x < leftPos + imageWidth && y >= topPos + LIST_Y && y < topPos + LIST_Y + ROW_HEIGHT * 4) {
            scroll = Math.clamp(scroll - (int)Math.signum(vertical), 0, Math.max(0, types().size() - 4));
            return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
    @Override public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        // Container screens draw their panel through this hook, unlike the old non-container tabs.
        renderBg(g, partial, mouseX, mouseY);
    }
    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xD0181818);
        g.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xFF55DDFF);
        for (var slot : menu.slots) {
            g.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, 0xCC202020);
            g.renderOutline(leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18, 0xFF1E6F7A);
        }
    }
    @Override protected void renderLabels(GuiGraphics g, int x, int y) {
        g.drawString(font, Component.translatable("screen.immortal_cultivation_mod.debug_stat_editor"), 12, 10, 0x55DDFF, true);
        if (minecraft.player != null) g.drawString(font, minecraft.player.getName(), 12, 20, 0xFFEAFDFF, true);
        g.drawString(font, Component.translatable("screen.immortal_cultivation_mod.restriction_hints")
                .copy().append(": " + ClientData.restrictionHints()), 128, 10, 0xFFEAFDFF, true);
        var stack = menu.weapon();
        boolean valid = WeaponRestrictionSystem.isWeapon(stack);
        boolean editable = valid && !com.example.immortal_cultivation_mod.item.WeaponTribulation.active(stack);
        rankMinus.active = editable && WeaponRestrictionSystem.tier(stack) > 0;
        rankPlus.active = editable && WeaponRestrictionSystem.tier(stack) < WeaponRestrictionSystem.MAX_TIER;
        durability.active = apply.active = editable && stack.isDamageableItem();
        g.drawString(font, text("rank").append(": ").append(Component.translatable(
                "weapon_tier.immortal_cultivation_mod." + WeaponRestrictionSystem.tier(stack))), 38, 65, 0xFFDDAA, true);
        g.drawString(font, text("durability"), 12, 94, 0xFFEAFDFF, true);
        if (!durability.isFocused()) durability.setValue(valid && stack.isDamageableItem()
                ? "" + (stack.getMaxDamage() - stack.getDamageValue()) : "");
        List<WeaponRestrictionSystem.RestrictionType> types = types();
        scroll = Math.clamp(scroll, 0, Math.max(0, types.size() - 4));
        for (int row = 0; row < 4; row++) {
            boolean present = row + scroll < types.size();
            for (Button button : layerButtons[row]) button.active = editable && present;
            if (!present) continue;
            var type = types.get(row + scroll);
            String label = Component.translatable("restriction.immortal_cultivation_mod." + type.id()).getString();
            g.drawString(font, font.plainSubstrByWidth(label, 132), 12, LIST_Y + 5 + row * ROW_HEIGHT, 0x88FFCC, true);
            g.drawString(font, "" + WeaponRestrictionSystem.effectiveCount(stack, type), 154, LIST_Y + 5 + row * ROW_HEIGHT, 0xFFEAFDFF, true);
        }
        g.drawString(font, valid ? text("layers").append(": " + WeaponRestrictionSystem.tierProgress(stack) + "/72") : text("insert"),
                12, 208, 0xAAAAAA, true);
        g.drawString(font, playerInventoryTitle, 79, 225, 0xFFEAFDFF, true);
    }
    @Override public void render(GuiGraphics g, int x, int y, float partial) {
        super.render(g, x, y, partial);
        renderTooltip(g, x, y);
    }
}
