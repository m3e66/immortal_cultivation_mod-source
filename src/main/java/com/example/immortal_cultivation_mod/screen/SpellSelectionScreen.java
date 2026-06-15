package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.effect.ModEffects;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SpellSelectionScreen extends Screen {
    private static final int ITEM_HEIGHT = 24;
    private static final int MARGIN = 20;

    private final List<ModSpells.SpellDef> allSpells;
    private SpellListWidget listWidget;
    private EditBox searchBox;
    private Button toggleButton;
    private ModSpells.SpellDef selectedSpell;
    private String lastSearch = "";

    public SpellSelectionScreen() {
        super(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".spell_selection"));
        this.allSpells = getKnownSpells();
    }

    @Override
    protected void init() {
        int listWidth = Math.min(460, width - MARGIN * 2);
        int left = (width - listWidth) / 2;

        searchBox = new EditBox(font, left, 30, listWidth, 20, Component.literal(""));
        searchBox.setResponder(this::onSearchChanged);
        addRenderableWidget(searchBox);

        listWidget = new SpellListWidget(minecraft, listWidth, height - 170, 58, ITEM_HEIGHT);
        listWidget.setX(left);
        populateList();
        addRenderableWidget(listWidget);

        toggleButton = addRenderableWidget(new StyledButton(
                width / 2 - 70,
                height - 56,
                140,
                20,
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".toggle_in_wheel"),
                btn -> {
                    if (selectedSpell != null) {
                        toggleSpell(selectedSpell);
                        updateToggleButton();
                    }
                }
        ));
        updateToggleButton();

        addRenderableWidget(new StyledButton(
                width / 2 - 50,
                height - 30,
                100,
                20,
                Component.translatable("gui.done"),
                btn -> onClose()
        ));
    }

    private static List<ModSpells.SpellDef> getKnownSpells() {
        var data = ClientData.cultivationData;
        if (data == null) {
            return List.of();
        }

        ArrayList<String> spellIds = new ArrayList<>(data.knownSpells() == null ? List.of() : data.knownSpells());
        for (String spellId : List.of(ModSpells.WEIYA, ModSpells.ABSORB_CULTIVATION, ModSpells.TUNTIAN, ModSpells.FENGYA)) {
            if (ModSpells.isInnateKnown(spellId, data)) {
                spellIds.add(spellId);
            }
        }

        return spellIds.stream()
                .map(ModSpells::normalizeId)
                .map(ModSpells::get)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private void populateList() {
        listWidget.children().clear();
        String search = lastSearch.toLowerCase();
        for (ModSpells.SpellDef spell : allSpells) {
            String name = spell.name().getString().toLowerCase();
            if (!search.isEmpty() && !spell.id().contains(search) && !name.contains(search)) {
                continue;
            }
            listWidget.children().add(new SpellListEntry(spell));
        }
    }

    private void onSearchChanged(String search) {
        lastSearch = search;
        populateList();
    }

    private void toggleSpell(ModSpells.SpellDef spell) {
        ClientData.toggleSpellVisibleInWheel(spell.id());
    }

    private void selectSpell(ModSpells.SpellDef spell) {
        selectedSpell = spell;
        updateToggleButton();
    }

    private void updateToggleButton() {
        if (toggleButton == null) {
            return;
        }

        toggleButton.active = selectedSpell != null;

        if (selectedSpell == null) {
            toggleButton.setMessage(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".toggle_in_wheel"));
            return;
        }

        String key = ClientData.isSpellVisibleInWheel(selectedSpell.id()) ? "remove_from_wheel" : "add_to_wheel";
        toggleButton.setMessage(Component.translatable("screen." + ImmortalCultivationMod.MODID + "." + key));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int panelWidth = Math.min(500, width - 16);
        int panelHeight = Math.min(height - 16, height - 8);
        int x = (width - panelWidth) / 2;
        int y = (height - panelHeight) / 2;

        guiGraphics.fill(x, y, x + panelWidth, y + panelHeight, 0xE0101010);
        guiGraphics.renderOutline(x, y, panelWidth, panelHeight, 0xFF55CCFF);
        guiGraphics.fill(x + 4, y + 4, x + panelWidth - 4, y + 24, 0x55225533);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        Component titleWithCount = title.copy().append(" " + ClientData.selectedWheelSpellCount() + "/" + ClientData.MAX_WHEEL_SPELLS);
        guiGraphics.drawString(font, titleWithCount, (width - font.width(titleWithCount)) / 2, 10, 0xFFFFAA, true);

        renderActiveToggleSpells(guiGraphics, x + 10, y + 28, panelWidth - 20);
        renderSpellDetails(guiGraphics, x + 12, height - 92, panelWidth - 24);

        if (allSpells.isEmpty()) {
            Component none = Component.translatable("screen." + ImmortalCultivationMod.MODID + ".no_spells");
            guiGraphics.drawString(font, none, (width - font.width(none)) / 2, height / 2 - 4, 0xCCCCCC, true);
        }
    }

    private void renderActiveToggleSpells(GuiGraphics guiGraphics, int x, int y, int maxWidth) {
        String names = activeToggleSpells().stream()
                .map(spell -> spell.name().getString())
                .reduce((a, b) -> a + ", " + b)
                .orElse(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".none_active_toggles").getString());

        Component line = Component.translatable("screen." + ImmortalCultivationMod.MODID + ".active_toggle_spells")
                .append(" ")
                .append(names);

        if (font.width(line) > maxWidth) {
            names = font.plainSubstrByWidth(names, Math.max(20, maxWidth - 120)) + "...";
            line = Component.translatable("screen." + ImmortalCultivationMod.MODID + ".active_toggle_spells")
                    .append(" ")
                    .append(names);
        }

        guiGraphics.drawString(font, line, x, y, 0xFFAAFFDD, false);
    }

    private List<ModSpells.SpellDef> activeToggleSpells() {
        if (minecraft == null || minecraft.player == null) {
            return List.of();
        }

        List<ModSpells.SpellDef> active = new ArrayList<>();

        addIfActive(active, ModSpells.EARTH_ESCAPE, ModEffects.EARTH_ESCAPE);
        addIfActive(active, ModSpells.QI_GATHERING, ModEffects.QI_GATHERING);
        addIfActive(active, ModSpells.SPIRIT_SIGHT, ModEffects.SPIRIT_SIGHT);
        addIfActive(active, ModSpells.WIND_STEP, ModEffects.WIND_STEP);
        addIfActive(active, ModSpells.YUFENG_JUE, ModEffects.YUFENG_JUE);
        addIfActive(active, ModSpells.WEIYA, ModEffects.WEIYA);
        addIfActive(active, ModSpells.FENGYA, ModEffects.FENGYA);

        return active;
    }

    private void addIfActive(List<ModSpells.SpellDef> active, String spellId, Holder<MobEffect> effect) {
        if (minecraft != null && minecraft.player != null && minecraft.player.hasEffect(effect)) {
            ModSpells.SpellDef spell = ModSpells.get(spellId);
            if (spell != null) {
                active.add(spell);
            }
        }
    }

    private void renderSpellDetails(GuiGraphics guiGraphics, int x, int y, int maxWidth) {
        guiGraphics.fill(x - 4, y - 6, x + maxWidth + 4, y + 34, 0x66000000);

        if (selectedSpell == null) {
            guiGraphics.drawString(font, Component.translatable("screen." + ImmortalCultivationMod.MODID + ".select_spell_hint"), x, y, 0xFF888888, false);
            return;
        }

        guiGraphics.drawString(font, selectedSpell.name(), x, y, 0xFFFFFF, true);

        Component desc = Component.translatable("spell." + ImmortalCultivationMod.MODID + "." + selectedSpell.translationKey() + ".description");
        int lineY = y + 12;

        for (var line : font.split(desc, maxWidth)) {
            guiGraphics.drawString(font, line, x, lineY, 0xFFCCCCCC, false);
            lineY += 10;

            if (lineY > y + 30) {
                break;
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class StyledButton extends Button {
        StyledButton(int x, int y, int width, int height, Component message, OnPress onPress) {
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

    private class SpellListWidget extends ObjectSelectionList<SpellListEntry> {
        public SpellListWidget(Minecraft mc, int width, int height, int y, int itemHeight) {
            super(mc, width, height, y, itemHeight);
        }

        @Override
        public int getRowWidth() {
            return Math.min(440, width - 10);
        }
    }

    private class SpellListEntry extends ObjectSelectionList.Entry<SpellListEntry> {
        private final ModSpells.SpellDef spell;

        SpellListEntry(ModSpells.SpellDef spell) {
            this.spell = spell;
        }

        @Override
        public Component getNarration() {
            return spell.name();
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovering, float partialTick) {
            boolean visible = ClientData.isSpellVisibleInWheel(spell.id());
            int rowColor = hovering ? 0x3322AAFF : 0x22000000;

            guiGraphics.fill(left, top - 1, left + width, top + height - 2, rowColor);

            boolean maxed = !visible && ClientData.selectedWheelSpellCount() >= ClientData.MAX_WHEEL_SPELLS;
            String marker = visible ? "[X]" : maxed ? "[8]" : "[ ]";
            int markerColor = visible ? 0xFF55FFAA : 0xFF888888;

            guiGraphics.drawString(font, marker, left + 4, top + 2, markerColor, true);
            guiGraphics.drawString(font, spell.name(), left + 34, top + 2, 0xFFFFFF, true);

            String requirement = ModSpells.WEIYA.equals(spell.id())
                    ? "Innate / 1% qi per second"
                    : spell.requiredLevel() + " / " + spell.qiCost() + " qi";

            guiGraphics.drawString(font, requirement, left + 34, top + 13, 0xFF888888, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 1) {
                toggleSpell(spell);
                updateToggleButton();
                return true;
            }

            selectSpell(spell);
            return true;
        }
    }
}