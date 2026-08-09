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
    private static final int WINDOW_H = 287;
    private static final int ROW_H = 22;

    public SpellComboMenuScreen() {
        super(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".spell_combo_menu"));
    }

    @Override
    protected void init() {
        clearWidgets();
        int x = (width - WINDOW_W) / 2;
        int y = (height - WINDOW_H) / 2;

        addRenderableWidget(new ComboButton(x + 132, y + 45, 18, 18,
                Component.literal("+"),
                b -> minecraft.setScreen(new SpellComboEditScreen(null, "", new ArrayList<>(), ClientSpellCombos.CastMode.SEQUENTIAL))));

        List<ClientSpellCombos.SpellCombo> combos = ClientSpellCombos.combos();
        for (int i = 0; i < Math.min(8, combos.size()); i++) {
            ClientSpellCombos.SpellCombo combo = combos.get(i);
            int rowY = y + 76 + i * ROW_H;
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
        g.drawString(font, title, x + (WINDOW_W - font.width(title)) / 2, y + 16,
                0xFF38352E, false);
        g.drawString(font, Component.translatable("screen." + ImmortalCultivationMod.MODID + ".create_spell"),
                x + 42, y + 50, 0xFF31584F, false);

        List<ClientSpellCombos.SpellCombo> combos = ClientSpellCombos.combos();
        if (combos.isEmpty()) {
            g.drawString(font, Component.translatable("screen." + ImmortalCultivationMod.MODID + ".no_spell_combos"),
                    x + 42, y + 86, 0xFF756D60, false);
        }

        for (int i = 0; i < Math.min(8, combos.size()); i++) {
            ClientSpellCombos.SpellCombo combo = combos.get(i);
            int rowY = y + 76 + i * ROW_H;
            int bg = (mouseY >= rowY && mouseY < rowY + 18 && mouseX >= x + 34 && mouseX <= x + WINDOW_W - 18)
                    ? 0x403C6B5D : 0x18A89B7D;
            g.fill(x + 34, rowY - 1, x + WINDOW_W - 18, rowY + 19, bg);
            g.drawString(font, combo.name(), x + 42, rowY + 5, 0xFF332F28, false);
            g.drawString(font, ClientSpellCombos.topElements(combo), x + 180, rowY + 5, 0xFF7A5B2D, false);
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
        CultivationGuiTextures.drawLandscape(g, x, y, w, h);
    }
}

class SpellComboEditScreen extends Screen {
    private static final int WINDOW_W = 400;
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

        addRenderableWidget(new ComboButton(x + 24, y + 54, 166, 18,
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".combo_mode_sequential"),
                b -> {
                    castMode = ClientSpellCombos.CastMode.SEQUENTIAL;
                    init();
                }));
        addRenderableWidget(new ComboButton(x + 210, y + 54, 166, 18,
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
        g.drawString(font, title, x + (WINDOW_W - font.width(title)) / 2, y + 16,
                0xFF38352E, false);
        g.drawString(font, Component.translatable("screen." + ImmortalCultivationMod.MODID + ".order"),
                x + 24, y + 33, 0xFF31584F, false);
        g.drawString(font, Component.translatable("screen." + ImmortalCultivationMod.MODID + ".combo_cast_mode"),
                x + 100, y + 33, 0xFF756D60, false);
        int selectedX = castMode == ClientSpellCombos.CastMode.SEQUENTIAL ? x + 24 : x + 210;
        g.renderOutline(selectedX, y + 54, 166, 18, 0xFF8B6A35);

        if (spellIds.isEmpty()) {
            g.drawString(font, Component.translatable("screen." + ImmortalCultivationMod.MODID + ".empty_combo_hint"),
                    x + 24, y + 68, 0xFF756D60, false);
        }

        for (int i = 0; i < Math.min(9, spellIds.size()); i++) {
            int rowY = y + 82 + i * ROW_H;
            var spell = ModSpells.get(spellIds.get(i));
            String name = spell == null ? spellIds.get(i) : spell.name().getString();
            int bg = draggingIndex == i ? 0x483C6B5D : 0x18A89B7D;
            g.fill(x + 22, rowY - 1, x + WINDOW_W - 18, rowY + 19, bg);
            g.drawString(font, "|||", x + 28, rowY + 5, 0xFF756D60, false);
            g.drawString(font, Integer.toString(i + 1), x + 56, rowY + 5, 0xFF7A5B2D, false);
            g.drawString(font, name, x + 80, rowY + 5, 0xFF332F28, false);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int row = rowAt(mouseX, mouseY);
            int x = (width - WINDOW_W) / 2;
            if (row >= 0 && mouseX >= x + 24 && mouseX <= x + 52) {
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
        if (mouseX < x + 22 || mouseX > x + WINDOW_W - 18) {
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
    private static final int WINDOW_H = 287;
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
        g.drawString(font, title, x + (WINDOW_W - font.width(title)) / 2, y + 16,
                0xFF38352E, false);

        List<ModSpells.SpellDef> spells = filteredSpells();
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, spells.size() - VISIBLE_ROWS)));
        for (int i = 0; i < Math.min(VISIBLE_ROWS, spells.size() - scrollOffset); i++) {
            ModSpells.SpellDef spell = spells.get(i + scrollOffset);
            int rowY = y + 62 + i * 24;
            boolean hover = mouseX >= x + 14 && mouseX <= x + WINDOW_W - 14 && mouseY >= rowY && mouseY <= rowY + 20;
            g.fill(x + 14, rowY, x + WINDOW_W - 14, rowY + 20,
                    hover ? 0x403C6B5D : 0x18A89B7D);
            g.drawString(font, spell.name(), x + 20, rowY + 6, 0xFF332F28, false);
            g.drawString(font, spell.element(), x + WINDOW_W - 58, rowY + 6, 0xFF7A5B2D, false);
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
    private static final int WINDOW_H = 224;
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
        nameBox = new EditBox(font, x + 24, y + 96, WINDOW_W - 48, 20, Component.literal(""));
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
        g.drawString(font, title, x + (WINDOW_W - font.width(title)) / 2, y + 17,
                0xFF38352E, false);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }
}

class SpellComboConfirmScreen extends Screen {
    private static final int WINDOW_W = 280;
    private static final int WINDOW_H = 224;
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
        g.drawCenteredString(font, message, x + WINDOW_W / 2, y + 94, 0xFF38352E);
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
        CultivationGuiTextures.drawButton(g, getX(), getY(), width, height,
                active, isHoveredOrFocused());
        CultivationGuiTextures.drawButtonText(g, getMessage(), getX(), getY(),
                width, height, active);
    }
}
