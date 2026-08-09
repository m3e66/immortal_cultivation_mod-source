package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Comparator;
import java.util.List;

public class SteleSpellEditorScreen extends Screen {
    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 286;
    private static final int VISIBLE_ROWS = 9;
    private static final int ROW_HEIGHT = 21;

    private final BlockPos stelePos;
    private final String currentSpellId;
    private final List<ModSpells.SpellDef> spells;
    private int scrollOffset;

    public SteleSpellEditorScreen(BlockPos stelePos, String currentSpellId) {
        super(Component.translatable(
                "screen." + ImmortalCultivationMod.MODID + ".stele_spell_editor"));
        this.stelePos = stelePos;
        this.currentSpellId = ModSpells.normalizeId(currentSpellId);
        this.spells = ModSpells.all().stream()
                .sorted(Comparator.comparing(spell -> spell.name().getString()))
                .toList();
    }

    @Override
    protected void init() {
        rebuildButtons();
    }

    private void rebuildButtons() {
        clearWidgets();
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        int end = Math.min(spells.size(), scrollOffset + VISIBLE_ROWS);
        for (int index = scrollOffset; index < end; index++) {
            ModSpells.SpellDef spell = spells.get(index);
            int row = index - scrollOffset;
            Component label = spell.id().equals(currentSpellId)
                    ? Component.literal("[X] ").append(spell.name())
                    : spell.name();
            addRenderableWidget(Button.builder(label, button -> selectSpell(spell.id()))
                    .bounds(left + 24, top + 55 + row * ROW_HEIGHT, PANEL_WIDTH - 48, 19)
                    .build());
        }
        addRenderableWidget(Button.builder(
                        Component.translatable("screen." + ImmortalCultivationMod.MODID
                                + ".stele_spell_editor.clear"),
                        button -> selectSpell(""))
                .bounds(left + 24, top + PANEL_HEIGHT - 30, 135, 20)
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("screen." + ImmortalCultivationMod.MODID
                                + ".stele_spell_editor.close"),
                        button -> onClose())
                .bounds(left + PANEL_WIDTH - 159, top + PANEL_HEIGHT - 30, 135, 20)
                .build());
    }

    private void selectSpell(String spellId) {
        PacketDistributor.sendToServer(
                new ModPayloads.ServerboundSetSteleSpellPayload(stelePos, spellId));
        onClose();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxOffset = Math.max(0, spells.size() - VISIBLE_ROWS);
        int nextOffset = Math.clamp(scrollOffset - (int) Math.signum(scrollY), 0, maxOffset);
        if (nextOffset != scrollOffset) {
            scrollOffset = nextOffset;
            rebuildButtons();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        graphics.fill(left - 2, top - 2, left + PANEL_WIDTH + 2, top + PANEL_HEIGHT + 2,
                0xE61D211E);
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xF5E9E1C8);
        graphics.fill(left + 12, top + 35, left + PANEL_WIDTH - 12, top + 36, 0xFF879087);
        graphics.drawCenteredString(font, title, width / 2, top + 12, 0xFF394139);

        ModSpells.SpellDef current = ModSpells.get(currentSpellId);
        Component currentName = current == null
                ? Component.translatable("screen." + ImmortalCultivationMod.MODID
                        + ".stele_spell_editor.empty")
                : current.name();
        graphics.drawString(font, Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID + ".stele_spell_editor.current",
                        currentName),
                left + 24, top + 41, 0xFF555B52, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
