package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SpellSelectionScreen extends Screen {
    private static final int ITEM_HEIGHT = 24;
    private static final int MARGIN = 20;

    private final List<ModSpells.SpellDef> allSpells;
    private SpellListWidget listWidget;
    private EditBox searchBox;
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

        listWidget = new SpellListWidget(minecraft, listWidth, height - 126, 58, ITEM_HEIGHT);
        listWidget.setX(left);
        populateList();
        addRenderableWidget(listWidget);

        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                btn -> onClose()
        ).bounds(width / 2 - 50, height - 30, 100, 20).build());
    }

    private static List<ModSpells.SpellDef> getKnownSpells() {
        var data = ClientData.cultivationData;
        if (data == null) {
            return List.of();
        }

        ArrayList<String> spellIds = new ArrayList<>(data.knownSpells() == null ? List.of() : data.knownSpells());
        for (String spellId : List.of(ModSpells.WEIYA, ModSpells.ABSORB_CULTIVATION, ModSpells.TUNTIAN)) {
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
        if (allSpells.isEmpty()) {
            Component none = Component.translatable("screen." + ImmortalCultivationMod.MODID + ".no_spells");
            guiGraphics.drawString(font, none, (width - font.width(none)) / 2, height / 2 - 4, 0xCCCCCC, true);
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
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

            String requirement = ModSpells.WEIYA.equals(spell.id()) ? "Innate / 1% qi per second" : spell.requiredLevel() + " / " + spell.qiCost() + " qi";
            guiGraphics.drawString(font, requirement, left + 34, top + 13, 0xFF888888, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            toggleSpell(spell);
            return true;
        }
    }
}
