package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.client.ClientSpellCombos;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SpellWheelScreen extends Screen {
    private static final int RADIUS = 60;

    private String selectedSpell;

    public SpellWheelScreen() {
        super(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".spell_wheel"));
    }

    private List<WheelEntry> getEntries() {
        var data = ClientData.cultivationData;

        if (data == null) {
            return List.of();
        }

        ArrayList<String> spellIds = new ArrayList<>(data.knownSpells() == null ? List.of() : data.knownSpells());
        for (String spellId : ModSpells.innateSpellIds()) {
            if (ModSpells.isInnateKnown(spellId, data)) {
                spellIds.add(spellId);
            }
        }

        Map<String, WheelEntry> availableEntries = new LinkedHashMap<>();
        spellIds.stream()
                .map(ModSpells::normalizeId)
                .map(ModSpells::get)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(spell -> availableEntries.put(spell.id(), new WheelEntry(spell.id(), spell.name(),
                        Component.empty().append(spell.description())
                                .append("\n")
                                .append(ModSpells.spellProficiencyDescription(data, spell.id())),
                        0xFFFFFFFF)));

        for (ClientSpellCombos.SpellCombo combo : ClientSpellCombos.combos()) {
            availableEntries.put(combo.castId(), new WheelEntry(
                        combo.castId(),
                        Component.literal(combo.name()),
                        ClientSpellCombos.castModeDescription(combo),
                        0xFFFFDDAA));
        }

        ArrayList<WheelEntry> entries = new ArrayList<>();
        for (String selectedId : ClientData.selectedWheelSpellIds()) {
            WheelEntry entry = availableEntries.get(selectedId);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g, mx, my, pt);

        Minecraft mc = Minecraft.getInstance();

        int cx = width / 2;
        int cy = height / 2;

        List<WheelEntry> entries = getEntries();
        selectedSpell = null;

        g.drawString(
                mc.font,
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".spell_wheel"),
                cx - 40,
                cy - 90,
                0xFFFFAA,
                true
        );

        if (entries.isEmpty()) {
            Component none = Component.translatable("screen." + ImmortalCultivationMod.MODID + ".no_spells");
            g.drawString(mc.font, none, cx - mc.font.width(none) / 2, cy - 4, 0xCCCCCC, true);
            return;
        }

        double dx = mx - cx;
        double dy = my - cy;

        double mouseAngle = Math.atan2(dy, dx);
        mouseAngle += Math.PI / 2;
        if (mouseAngle < 0) {
            mouseAngle += Math.PI * 2;
        }

        double sliceSize = (Math.PI * 2) / entries.size();

        for (int i = 0; i < entries.size(); i++) {
            double angle = i * sliceSize;

            int sx = cx + (int)(Math.sin(angle) * RADIUS);
            int sy = cy - (int)(Math.cos(angle) * RADIUS);

            double diff = Math.abs(mouseAngle - angle);
            diff = Math.min(diff, Math.PI * 2 - diff);

            boolean isSelected = diff < (sliceSize / 2.0);
            WheelEntry entry = entries.get(i);

            if (isSelected) {
                selectedSpell = entry.castId();
            }

            int color = isSelected ? 0xAAFFD700 : 0x66444444;
            int cooldownTicks = ClientData.spellCooldownTicks(entry.castId());

            g.fill(sx - 20, sy - 20, sx + 20, sy + 20, color);
            g.renderOutline(sx - 20, sy - 20, 40, 40, isSelected ? 0xFFFFD700 : 0xFF888888);

            String name = entry.name().getString();
            g.drawString(mc.font, name, sx - mc.font.width(name) / 2, sy - 4, 0xFFFFFF, true);

            if (cooldownTicks > 0) {
                g.fill(sx - 20, sy - 20, sx + 20, sy + 20, 0xAA000000);
                String seconds = String.valueOf(Math.max(1, (cooldownTicks + 19) / 20));
                int textX = sx - mc.font.width(seconds) / 2;
                int textY = sy - 4;
                g.drawString(mc.font, seconds, textX + 1, textY + 1, 0xFF000000, false);
                g.drawString(mc.font, seconds, textX, textY, 0xFFFF5555, true);
            }

            if (isSelected) {
                g.drawString(mc.font, entry.description(), cx - 60, cy + RADIUS + 30, entry.descriptionColor(), true);
            }
        }
    }

    public String getSelectedSpell() {
        return selectedSpell;
    }

    private record WheelEntry(String castId, Component name, Component description, int descriptionColor) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
