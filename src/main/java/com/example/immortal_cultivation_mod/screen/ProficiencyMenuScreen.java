package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class ProficiencyMenuScreen extends Screen {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "textures/gui/proficiency_menu.png");
    private static final int TEXTURE_W = 1547;
    private static final int TEXTURE_H = 1087;
    private static final int WINDOW_W = 490;
    private static final int WINDOW_H = 344;
    private static final int VALUE_COLOR = 0x4C4740;
    private static final int MUTED_COLOR = 0x6D675F;

    private static final List<ElementRow> ELEMENT_ROWS = List.of(
            new ElementRow(SpiritRoots.METAL, 264),
            new ElementRow(SpiritRoots.WOOD, 334),
            new ElementRow(SpiritRoots.WATER, 404),
            new ElementRow(SpiritRoots.FIRE, 474),
            new ElementRow(SpiritRoots.EARTH, 544),
            new ElementRow(SpiritRoots.THUNDER, 614),
            new ElementRow(SpiritRoots.ICE, 684),
            new ElementRow(SpiritRoots.WIND, 754),
            new ElementRow(SpiritRoots.LIGHT, 824),
            new ElementRow(SpiritRoots.DARK, 894)
    );

    private static final List<CraftRow> CRAFT_ROWS = List.of(
            new CraftRow("forging", 264),
            new CraftRow("alchemy", 334),
            new CraftRow("talisman", 404),
            new CraftRow("beast_taming", 474),
            new CraftRow("puppet", 544),
            new CraftRow("corpse_control", 614),
            new CraftRow("gu", 684),
            new CraftRow("formation", 754),
            new CraftRow("divination", 824),
            new CraftRow("ghost_control", 894)
    );

    private Button backButton;

    public ProficiencyMenuScreen() {
        super(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".proficiency_menu"));
    }

    @Override
    protected void init() {
        int x = left();
        int y = top();
        backButton = addRenderableWidget(new InvisibleButton(
                x,
                y + py(92),
                px(64),
                py(220),
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".stat_menu"),
                b -> minecraft.setScreen(new StatMenuScreen())
        ));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        int x = left();
        int y = top();
        g.blit(TEXTURE, x, y, WINDOW_W, WINDOW_H,
                0.0F, 0.0F, TEXTURE_W, TEXTURE_H, TEXTURE_W, TEXTURE_H);

        ModAttachments.CultivationData data = ClientData.cultivationData;
        if (data != null) {
            for (ElementRow row : ELEMENT_ROWS) {
                int value = elementProficiency(data, row.element());
                drawLeft(g, Component.literal(proficiencyText(value)), x + px(350), y + py(row.sourceY()), px(180), VALUE_COLOR);
            }
            for (CraftRow row : CRAFT_ROWS) {
                int value = craftProficiency(data, row.id());
                drawLeft(g, Component.literal(proficiencyText(value)), x + px(1030), y + py(row.sourceY()), px(210), value > 0 ? VALUE_COLOR : MUTED_COLOR);
            }
        }

        super.render(g, mx, my, pt);
    }

    private int elementProficiency(ModAttachments.CultivationData data, String element) {
        int total = 0;
        for (ModSpells.SpellDef spell : ModSpells.all()) {
            if (element.equals(spell.element())) {
                total += data.spellProficiency(spell.id());
            }
        }
        return total;
    }

    private int craftProficiency(ModAttachments.CultivationData data, String id) {
        return switch (id) {
            case "corpse_control" -> data.spellProficiency(ModSpells.KONGSHI_SHU);
            default -> data.methodProficiency("craft_" + id);
        };
    }

    private String proficiencyText(int uses) {
        String key;
        if (uses >= 10_000) {
            key = "spell_proficiency_peak";
        } else if (uses >= 1_000) {
            key = "spell_proficiency_perfected";
        } else if (uses >= 500) {
            key = "spell_proficiency_great";
        } else if (uses >= 100) {
            key = "spell_proficiency_minor";
        } else {
            key = "spell_proficiency_beginner";
        }
        return Component.translatable("screen." + ImmortalCultivationMod.MODID + "." + key).getString() + " " + uses;
    }

    private void drawLeft(GuiGraphics g, Component text, int x, int y, int maxWidth, int color) {
        Minecraft mc = Minecraft.getInstance();
        int textWidth = Math.max(1, mc.font.width(text));
        float scale = Math.min(1.0F, maxWidth / (float) textWidth);

        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        g.drawString(mc.font, text, 0, 0, color, false);
        g.pose().popPose();
    }

    private int left() {
        return (width - WINDOW_W) / 2;
    }

    private int top() {
        return (height - WINDOW_H) / 2;
    }

    private static int px(int sourceX) {
        return Math.round(sourceX * (WINDOW_W / (float) TEXTURE_W));
    }

    private static int py(int sourceY) {
        return Math.round(sourceY * (WINDOW_H / (float) TEXTURE_H));
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record ElementRow(String element, int sourceY) {
    }

    private record CraftRow(String id, int sourceY) {
    }

    private static class InvisibleButton extends Button {
        InvisibleButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        }
    }
}
