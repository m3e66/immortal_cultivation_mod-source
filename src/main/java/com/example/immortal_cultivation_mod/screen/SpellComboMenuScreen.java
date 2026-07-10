package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.client.ClientSpellCombos;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SpellComboMenuScreen extends Screen {
    private static final int WINDOW_W = 360;
    private static final int WINDOW_H = 260;
    private static final int ROW_H = 22;

    public SpellComboMenuScreen() {
        super(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".spell_combo_menu"));
    }

    @Override
    protected void init() {
        clearWidgets();
        int x = (width - WINDOW_W) / 2;
        int y = (height - WINDOW_H) / 2;

        addRenderableWidget(new ComboButton(x + 104, y + 28, 18, 18,
                Component.literal("+"),
                b -> minecraft.setScreen(new SpellComboEditScreen(null, "", new ArrayList<>(), ClientSpellCombos.CastMode.SEQUENTIAL))));

        List<ClientSpellCombos.SpellCombo> combos = ClientSpellCombos.combos();
        for (int i = 0; i < Math.min(8, combos.size()); i++) {
            ClientSpellCombos.SpellCombo combo = combos.get(i);
            int rowY = y + 58 + i * ROW_H;
            addRenderableWidget(new ComboButton(x + WINDOW_W - 58, rowY, 18, 18, Component.literal("+"),
                    b -> minecraft.setScreen(new SpellComboEditScreen(combo.id(), combo.name(), new ArrayList<>(combo.spellIds()), combo.castMode()))));
            addRenderableWidget(new ComboButton(x + WINDOW_W - 34, rowY, 18, 18, Component.literal("-"),
                    b -> minecraft.setScreen(new SpellComboConfirmScreen(this,
                            Component.translatable("screen." + ImmortalCultivationMod.MODID + ".remove_combo_confirm", combo.name()),
                            () -> {
                                ClientSpellCombos.remove(combo.id());
                                minecraft.setScreen(new SpellComboMenuScreen());
                            }))));
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = (width - WINDOW_W) / 2;
        int y = (height - WINDOW_H) / 2;
        drawPanel(g, x, y, WINDOW_W, WINDOW_H);
        g.drawString(font, title, x + 14, y + 10, 0x55DDFF, true);
        g.drawString(font, Component.translatable("screen." + ImmortalCultivationMod.MODID + ".create_spell"),
                x + 14, y + 33, 0xFFFFFFFF, true);

        List<ClientSpellCombos.SpellCombo> combos = ClientSpellCombos.combos();
        if (combos.isEmpty()) {
            g.drawString(font, Component.translatable("screen." + ImmortalCultivationMod.MODID + ".no_spell_combos"),
                    x + 14, y + 68, 0xFF888888, false);
        }

        for (int i = 0; i < Math.min(8, combos.size()); i++) {
            ClientSpellCombos.SpellCombo combo = combos.get(i);
            int rowY = y + 58 + i * ROW_H;
            int bg = (mouseY >= rowY && mouseY < rowY + 18 && mouseX >= x + 12 && mouseX <= x + WINDOW_W - 12) ? 0x3322AAFF : 0x22000000;
            g.fill(x + 12, rowY - 1, x + WINDOW_W - 12, rowY + 19, bg);
            g.drawString(font, combo.name(), x + 18, rowY + 5, 0xFFFFFFFF, true);
            g.drawString(font, ClientSpellCombos.topElements(combo), x + 160, rowY + 5, 0xFFFFDDAA, true);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    static void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xE0101010);
        g.renderOutline(x, y, w, h, 0xFF55DDFF);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0xAA8FFFFF);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0xAA1E6F7A);
    }
}

class SpellComboEditScreen extends Screen {
    private static final int WINDOW_W = 360;
    private static final int WINDOW_H = 320;
    private static final int ROW_H = 22;

    private final String comboId;
    private final String comboName;
    private final List<String> spellIds;
    private ClientSpellCombos.CastMode castMode;
    private int draggingIndex = -1;

    SpellComboEditScreen(String comboId, String comboName, List<String> spellIds, ClientSpellCombos.CastMode castMode) {
        super(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".spell_combo_editor"));
        this.comboId = comboId;
        this.comboName = comboName;
        this.spellIds = spellIds;
        this.castMode = castMode;
    }

    @Override
    protected void init() {
        clearWidgets();
        int x = (width - WINDOW_W) / 2;
        int y = (height - WINDOW_H) / 2;

        addRenderableWidget(new ComboButton(x + 58, y + 28, 18, 18,
                Component.literal("+"),
                b -> minecraft.setScreen(new SpellComboSpellPickerScreen(this))));

        addRenderableWidget(new ComboButton(x + 14, y + 54, 156, 18,
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".combo_mode_sequential"),
                b -> {
                    castMode = ClientSpellCombos.CastMode.SEQUENTIAL;
                    init();
                }));
        addRenderableWidget(new ComboButton(x + 190, y + 54, 156, 18,
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".combo_mode_simultaneous"),
                b -> {
                    castMode = ClientSpellCombos.CastMode.SIMULTANEOUS;
                    init();
                }));

        for (int i = 0; i < Math.min(9, spellIds.size()); i++) {
            int index = i;
            int rowY = y + 82 + i * ROW_H;
            addRenderableWidget(new ComboButton(x + WINDOW_W - 34, rowY, 18, 18, Component.literal("-"),
                    b -> minecraft.setScreen(new SpellComboConfirmScreen(this,
                            Component.translatable("screen." + ImmortalCultivationMod.MODID + ".remove_spell_confirm"),
                            () -> {
                                if (index >= 0 && index < spellIds.size()) {
                                    spellIds.remove(index);
                                }
                                minecraft.setScreen(this);
                            }))));
        }

        Button done = addRenderableWidget(new ComboButton(x + WINDOW_W / 2 - 50, y + WINDOW_H - 32, 100, 20,
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".done_button"),
                b -> minecraft.setScreen(new SpellComboNameScreen(comboId, comboName, spellIds, castMode))));
        done.active = !spellIds.isEmpty();
    }

    void addSpell(String spellId) {
        spellIds.add(ModSpells.normalizeId(spellId));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = (width - WINDOW_W) / 2;
        int y = (height - WINDOW_H) / 2;
        SpellComboMenuScreen.drawPanel(g, x, y, WINDOW_W, WINDOW_H);
        g.drawString(font, title, x + 14, y + 10, 0x55DDFF, true);
        g.drawString(font, Component.translatable("screen." + ImmortalCultivationMod.MODID + ".order"),
                x + 14, y + 33, 0xFFFFFFFF, true);
        g.drawString(font, Component.translatable("screen." + ImmortalCultivationMod.MODID + ".combo_cast_mode"),
                x + 90, y + 33, 0xFFAAAAAA, false);
        int selectedX = castMode == ClientSpellCombos.CastMode.SEQUENTIAL ? x + 14 : x + 190;
        g.renderOutline(selectedX, y + 54, 156, 18, 0xFFFFDDAA);

        if (spellIds.isEmpty()) {
            g.drawString(font, Component.translatable("screen." + ImmortalCultivationMod.MODID + ".empty_combo_hint"),
                    x + 14, y + 68, 0xFF888888, false);
        }

        for (int i = 0; i < Math.min(9, spellIds.size()); i++) {
            int rowY = y + 82 + i * ROW_H;
            var spell = ModSpells.get(spellIds.get(i));
            String name = spell == null ? spellIds.get(i) : spell.name().getString();
            int bg = draggingIndex == i ? 0x5533CCFF : 0x22000000;
            g.fill(x + 12, rowY - 1, x + WINDOW_W - 12, rowY + 19, bg);
            g.drawString(font, "|||", x + 18, rowY + 5, 0xFFAAAAAA, false);
            g.drawString(font, Integer.toString(i + 1), x + 46, rowY + 5, 0xFFFFDDAA, true);
            g.drawString(font, name, x + 70, rowY + 5, 0xFFFFFFFF, true);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int row = rowAt(mouseX, mouseY);
            int x = (width - WINDOW_W) / 2;
            if (row >= 0 && mouseX >= x + 14 && mouseX <= x + 42) {
                draggingIndex = row;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingIndex >= 0) {
            int target = rowAt(mouseX, mouseY);
            if (target >= 0 && target < spellIds.size() && target != draggingIndex) {
                String moved = spellIds.remove(draggingIndex);
                spellIds.add(target, moved);
                init();
            }
            draggingIndex = -1;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private int rowAt(double mouseX, double mouseY) {
        int x = (width - WINDOW_W) / 2;
        int y = (height - WINDOW_H) / 2;
        if (mouseX < x + 12 || mouseX > x + WINDOW_W - 12) {
            return -1;
        }
        int row = (int) ((mouseY - (y + 82)) / ROW_H);
        return row >= 0 && row < Math.min(9, spellIds.size()) ? row : -1;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

class SpellComboSpellPickerScreen extends Screen {
    private static final int WINDOW_W = 360;
    private static final int WINDOW_H = 280;
    private static final int VISIBLE_ROWS = 8;
    private final SpellComboEditScreen parent;
    private EditBox searchBox;
    private String search = "";
    private int scrollOffset = 0;

    SpellComboSpellPickerScreen(SpellComboEditScreen parent) {
        super(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".spell_combo_picker"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        int x = (width - WINDOW_W) / 2;
        int y = (height - WINDOW_H) / 2;
        searchBox = new EditBox(font, x + 14, y + 30, WINDOW_W - 28, 20, Component.literal(""));
        searchBox.setValue(search);
        searchBox.setResponder(value -> search = value);
        addRenderableWidget(searchBox);
    }

    private List<ModSpells.SpellDef> filteredSpells() {
        String needle = search.toLowerCase();
        return ClientSpellCombos.comboEligibleSpells(ClientData.cultivationData).stream()
                .filter(spell -> needle.isBlank()
                        || spell.id().contains(needle)
                        || spell.name().getString().toLowerCase().contains(needle)
                        || spell.element().contains(search))
                .toList();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = (width - WINDOW_W) / 2;
        int y = (height - WINDOW_H) / 2;
        SpellComboMenuScreen.drawPanel(g, x, y, WINDOW_W, WINDOW_H);
        g.drawString(font, title, x + 14, y + 10, 0x55DDFF, true);

        List<ModSpells.SpellDef> spells = filteredSpells();
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, spells.size() - VISIBLE_ROWS)));
        for (int i = 0; i < Math.min(VISIBLE_ROWS, spells.size() - scrollOffset); i++) {
            ModSpells.SpellDef spell = spells.get(i + scrollOffset);
            int rowY = y + 62 + i * 24;
            boolean hover = mouseX >= x + 14 && mouseX <= x + WINDOW_W - 14 && mouseY >= rowY && mouseY <= rowY + 20;
            g.fill(x + 14, rowY, x + WINDOW_W - 14, rowY + 20, hover ? 0x3322AAFF : 0x22000000);
            g.drawString(font, spell.name(), x + 20, rowY + 6, 0xFFFFFFFF, true);
            g.drawString(font, spell.element(), x + WINDOW_W - 58, rowY + 6, 0xFFFFDDAA, true);
        }
        if (spells.size() > VISIBLE_ROWS) {
            String scrollText = (scrollOffset + 1) + "-" + Math.min(spells.size(), scrollOffset + VISIBLE_ROWS) + "/" + spells.size();
            g.drawString(font, scrollText, x + WINDOW_W - font.width(scrollText) - 14, y + WINDOW_H - 20, 0xFF888888, false);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0) {
            int x = (width - WINDOW_W) / 2;
            int y = (height - WINDOW_H) / 2;
            if (mouseX >= x + 14 && mouseX <= x + WINDOW_W - 14) {
                int row = (int) ((mouseY - (y + 62)) / 24);
                List<ModSpells.SpellDef> spells = filteredSpells();
                int index = row + scrollOffset;
                if (row >= 0 && row < VISIBLE_ROWS && index < spells.size()) {
                    parent.addSpell(spells.get(index).id());
                    minecraft.setScreen(parent);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = Math.max(0, filteredSpells().size() - VISIBLE_ROWS);
        if (max <= 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        scrollOffset = Math.max(0, Math.min(max, scrollOffset - (int) Math.signum(scrollY)));
        return true;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

class SpellComboNameScreen extends Screen {
    private static final int WINDOW_W = 280;
    private static final int WINDOW_H = 120;
    private final String comboId;
    private final String existingName;
    private final List<String> spellIds;
    private final ClientSpellCombos.CastMode castMode;
    private EditBox nameBox;

    SpellComboNameScreen(String comboId, String existingName, List<String> spellIds, ClientSpellCombos.CastMode castMode) {
        super(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".spell_combo_name"));
        this.comboId = comboId == null ? UUID.randomUUID().toString() : comboId;
        this.existingName = existingName == null ? "" : existingName;
        this.spellIds = List.copyOf(spellIds);
        this.castMode = castMode;
    }

    @Override
    protected void init() {
        int x = (width - WINDOW_W) / 2;
        int y = (height - WINDOW_H) / 2;
        nameBox = new EditBox(font, x + 16, y + 42, WINDOW_W - 32, 20, Component.literal(""));
        nameBox.setMaxLength(32);
        nameBox.setValue(existingName.isBlank() ? "New Combo" : existingName);
        addRenderableWidget(nameBox);
        addRenderableWidget(new ComboButton(x + WINDOW_W / 2 - 50, y + WINDOW_H - 30, 100, 20,
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".save_button"),
                b -> {
                    ClientSpellCombos.save(comboId, nameBox.getValue(), spellIds, castMode);
                    minecraft.setScreen(new SpellComboMenuScreen());
                }));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = (width - WINDOW_W) / 2;
        int y = (height - WINDOW_H) / 2;
        SpellComboMenuScreen.drawPanel(g, x, y, WINDOW_W, WINDOW_H);
        g.drawString(font, title, x + 16, y + 14, 0x55DDFF, true);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }
}

class SpellComboConfirmScreen extends Screen {
    private static final int WINDOW_W = 280;
    private static final int WINDOW_H = 110;
    private final Screen previous;
    private final Component message;
    private final Runnable confirm;

    SpellComboConfirmScreen(Screen previous, Component message, Runnable confirm) {
        super(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".confirm"));
        this.previous = previous;
        this.message = message;
        this.confirm = confirm;
    }

    @Override
    protected void init() {
        int x = (width - WINDOW_W) / 2;
        int y = (height - WINDOW_H) / 2;
        addRenderableWidget(new ComboButton(x + 42, y + WINDOW_H - 30, 80, 20,
                Component.translatable("gui.yes"), b -> confirm.run()));
        addRenderableWidget(new ComboButton(x + WINDOW_W - 122, y + WINDOW_H - 30, 80, 20,
                Component.translatable("gui.no"), b -> minecraft.setScreen(previous)));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = (width - WINDOW_W) / 2;
        int y = (height - WINDOW_H) / 2;
        SpellComboMenuScreen.drawPanel(g, x, y, WINDOW_W, WINDOW_H);
        g.drawString(font, message, x + 16, y + 28, 0xFFFFFFFF, true);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }
}

class ComboButton extends Button {
    ComboButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int bg = active ? 0xCC202020 : 0x88404040;
        int top = isHoveredOrFocused() && active ? 0xFF8FFFFF : 0xFF55DDFF;
        int bottom = active ? 0xFF1E6F7A : 0xFF555555;
        int text = active ? 0xFFEAFDFF : 0xFF999999;

        g.fill(x, y, x + width, y + height, bg);
        g.fill(x, y, x + width, y + 1, top);
        g.fill(x, y, x + 1, y + height, top);
        g.fill(x, y + height - 1, x + width, y + height, bottom);
        g.fill(x + width - 1, y, x + width, y + height, bottom);

        var font = Minecraft.getInstance().font;
        Component message = getMessage();
        int tx = x + (width - font.width(message)) / 2;
        int ty = y + (height - 8) / 2;
        g.drawString(font, message, tx, ty, text, false);
    }
}
