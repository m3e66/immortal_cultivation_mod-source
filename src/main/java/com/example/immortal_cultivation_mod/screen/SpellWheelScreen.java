package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SpellWheelScreen extends Screen {
    private static final int RADIUS = 60;

    private String selectedSpell;

    public SpellWheelScreen() {
        super(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".spell_wheel"));
    }

    private List<ModSpells.SpellDef> getSpells() {
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
                .filter(ClientData::isSpellVisibleInWheel)
                .map(ModSpells::get)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g, mx, my, pt);

        Minecraft mc = Minecraft.getInstance();

        int cx = width / 2;
        int cy = height / 2;

        List<ModSpells.SpellDef> spells = getSpells();
        selectedSpell = null;

        g.drawString(
                mc.font,
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".spell_wheel"),
                cx - 40,
                cy - 90,
                0xFFFFAA,
                true
        );

        if (spells.isEmpty()) {
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

        double sliceSize = (Math.PI * 2) / spells.size();

        for (int i = 0; i < spells.size(); i++) {
            double angle = i * sliceSize;

            int sx = cx + (int)(Math.sin(angle) * RADIUS);
            int sy = cy - (int)(Math.cos(angle) * RADIUS);

            double diff = Math.abs(mouseAngle - angle);
            diff = Math.min(diff, Math.PI * 2 - diff);

            boolean isSelected = diff < (sliceSize / 2.0);
            ModSpells.SpellDef spell = spells.get(i);

            if (isSelected) {
                selectedSpell = spell.id();
            }

            int color = isSelected ? 0xAAFFD700 : 0x66444444;

            g.fill(sx - 20, sy - 20, sx + 20, sy + 20, color);
            g.renderOutline(sx - 20, sy - 20, 40, 40, isSelected ? 0xFFFFD700 : 0xFF888888);

            String name = spell.name().getString();
            g.drawString(mc.font, name, sx - mc.font.width(name) / 2, sy - 4, 0xFFFFFF, true);

            if (isSelected) {
                g.drawString(mc.font, spell.description(), cx - 60, cy + RADIUS + 30, 0xCCCCCC, true);
            }
        }
    }

    public String getSelectedSpell() {
        return selectedSpell;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
