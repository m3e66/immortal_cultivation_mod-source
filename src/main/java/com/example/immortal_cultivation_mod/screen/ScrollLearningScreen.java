package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ScrollLearningScreen extends Screen {
    private static final ResourceLocation BG_TL = ResourceLocation.fromNamespaceAndPath("autoforge_bricks", "textures/gui/border_corner_tl.png");
    private static final ResourceLocation BG_TR = ResourceLocation.fromNamespaceAndPath("autoforge_bricks", "textures/gui/border_corner_tr.png");
    private static final ResourceLocation BG_BL = ResourceLocation.fromNamespaceAndPath("autoforge_bricks", "textures/gui/border_corner_bl.png");
    private static final ResourceLocation BG_BR = ResourceLocation.fromNamespaceAndPath("autoforge_bricks", "textures/gui/border_corner_br.png");
    private static final ResourceLocation BG_ET = ResourceLocation.fromNamespaceAndPath("autoforge_bricks", "textures/gui/border_edge_top.png");
    private static final ResourceLocation BG_EB = ResourceLocation.fromNamespaceAndPath("autoforge_bricks", "textures/gui/border_edge_bottom.png");
    private static final ResourceLocation BG_EL = ResourceLocation.fromNamespaceAndPath("autoforge_bricks", "textures/gui/border_edge_left.png");
    private static final ResourceLocation BG_ER = ResourceLocation.fromNamespaceAndPath("autoforge_bricks", "textures/gui/border_edge_right.png");
    private static final ResourceLocation BG_FILL = ResourceLocation.fromNamespaceAndPath("autoforge_bricks", "textures/gui/fill_white.png");

    private static final int W = 260;
    private static final int H = 220;

    private final String spellId;
    private final ModSpells.SpellDef spell;
    private final boolean alreadyLearned;

    public ScrollLearningScreen(String spellId) {
        super(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".scroll_learning"));
        this.spellId = spellId;
        this.spell = ModSpells.get(spellId);
        this.alreadyLearned = ClientData.cultivationData.knownSpells().contains(spellId);
    }

    @Override
    protected void init() {
        int x = (width - W) / 2;
        int y = (height - H) / 2;

        if (!alreadyLearned) {
            addRenderableWidget(new StyledButton(
                    x + 20,
                    y + 170,
                    100,
                    20,
                    Component.translatable("screen." + ImmortalCultivationMod.MODID + ".learn_button"),
                    b -> learnSpell()
            ));
        }
    }

    private void learnSpell() {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new com.example.immortal_cultivation_mod.network.ModPayloads.ServerboundLearnSpellPayload(spellId));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        int x = (width - W) / 2;
        int y = (height - H) / 2;

        g.blit(BG_FILL, x + 5, y + 5, 0, 0, W - 10, H - 10, 1, 1);
        g.blit(BG_TL, x, y, 0, 0, 5, 5, 5, 5);
        g.blit(BG_TR, x + W - 5, y, 0, 0, 5, 5, 5, 5);
        g.blit(BG_BL, x, y + H - 5, 0, 0, 5, 5, 5, 5);
        g.blit(BG_BR, x + W - 5, y + H - 5, 0, 0, 5, 5, 5, 5);
        g.blit(BG_ET, x + 5, y, 0, 0, W - 10, 5, 1, 5);
        g.blit(BG_EB, x + 5, y + H - 5, 0, 0, W - 10, 5, 1, 5);
        g.blit(BG_EL, x, y + 5, 0, 0, 5, H - 10, 5, 1);
        g.blit(BG_ER, x + W - 5, y + 5, 0, 0, 5, H - 10, 5, 1);

        Minecraft mc = Minecraft.getInstance();
        int lx = x + 15;
        int ly = y + 12;

        g.drawString(mc.font, Component.translatable("screen." + ImmortalCultivationMod.MODID + ".scroll_learning"), x + W / 2 - 40, ly, 0xEECC66, true);
        ly += 20;

        int labelColor = 0xCCCCCC;
        int valueColor = 0xFFFFFF;

        g.drawString(mc.font, Component.literal("* ").append(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".spell_name_label")), lx, ly, labelColor, true);
        g.drawString(mc.font, spell.name(), lx + 100, ly, valueColor, true);
        ly += 16;

        g.drawString(mc.font, Component.literal("* ").append(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".requirement_label")), lx, ly, labelColor, true);
        g.drawString(mc.font, Component.literal(spell.requiredLevel()), lx + 100, ly, ChatFormatting.YELLOW.getColor(), true);
        ly += 16;

        g.drawString(mc.font, Component.literal("* ").append(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".element_label")), lx, ly, labelColor, true);
        g.drawString(mc.font, Component.literal(spell.element()), lx + 100, ly, 0x88FFCC, true);
        ly += 16;

        g.drawString(mc.font, Component.literal("* ").append(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".method_tier_label")), lx, ly, labelColor, true);
        g.drawString(mc.font, Component.translatable("method_tier." + ImmortalCultivationMod.MODID + "." + spell.tier()), lx + 100, ly, 0xFFDD88, true);
        ly += 16;

        g.drawString(mc.font, Component.literal("* ").append(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".qi_cost_label")), lx, ly, labelColor, true);
        g.drawString(mc.font, Component.literal(String.valueOf(spell.qiCost())), lx + 100, ly, ChatFormatting.AQUA.getColor(), true);
        ly += 22;

        g.drawString(mc.font, Component.translatable("screen." + ImmortalCultivationMod.MODID + ".description_label"), lx, ly, labelColor, true);
        ly += 12;
        g.drawString(mc.font, spell.description(), lx, ly, 0xAAAAAA, true);

        if (alreadyLearned) {
            Component learned = Component.translatable("screen." + ImmortalCultivationMod.MODID + ".spell_already_learned");
            g.drawString(mc.font, learned, x + W / 2 - mc.font.width(learned) / 2, y + 172, 0x55FF55, true);
        }

        super.render(g, mx, my, pt);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
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
}