package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

public class MethodLearningScreen extends Screen {
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

    private final String methodId;
    private final CultivationMethods.MethodDef method;

    public MethodLearningScreen(String methodId) {
        super(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".method_learning"));
        this.methodId = methodId;
        this.method = CultivationMethods.get(methodId);
    }

    @Override
    protected void init() {
        int x = (width - W) / 2;
        int y = (height - H) / 2;
        boolean active = ClientData.cultivationData.activeCultivationMethod().equals(methodId);

        if (method != null && !active) {
            addRenderableWidget(new StyledButton(
                    x + 20,
                    y + 170,
                    110,
                    20,
                    Component.translatable("screen." + ImmortalCultivationMod.MODID + ".activate_method_button"),
                    b -> activateMethod()
            ));
        }
    }

    private void activateMethod() {
        PacketDistributor.sendToServer(new ModPayloads.ServerboundActivateMethodPayload(methodId));
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
        int labelColor = 0xCCCCCC;
        int valueColor = 0xFFFFFF;

        Component title = Component.translatable("screen." + ImmortalCultivationMod.MODID + ".method_learning");
        g.drawString(mc.font, title, x + W / 2 - mc.font.width(title) / 2, ly, 0xEECC66, true);
        ly += 20;

        if (method == null) {
            g.drawString(mc.font, Component.translatable("screen." + ImmortalCultivationMod.MODID + ".unknown_method"), lx, ly, 0xFF5555, true);
            super.render(g, mx, my, pt);
            return;
        }

        g.drawString(mc.font, Component.literal("* ").append(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".method_name_label")), lx, ly, labelColor, true);
        g.drawString(mc.font, Component.translatable(method.nameKey()), lx + 100, ly, valueColor, true);
        ly += 16;

        g.drawString(mc.font, Component.literal("* ").append(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".method_limit_label")), lx, ly, labelColor, true);
        g.drawString(mc.font, Component.literal(method.limitLevel()), lx + 100, ly, ChatFormatting.YELLOW.getColor(), true);
        ly += 16;

        g.drawString(mc.font, Component.literal("* ").append(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".element_label")), lx, ly, labelColor, true);
        g.drawString(mc.font, Component.translatable("method_element." + ImmortalCultivationMod.MODID + "." + method.element()), lx + 100, ly, 0x88FFCC, true);
        ly += 16;

        g.drawString(mc.font, Component.literal("* ").append(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".method_tier_label")), lx, ly, labelColor, true);
        g.drawString(mc.font, Component.translatable("method_tier." + ImmortalCultivationMod.MODID + "." + method.tier()), lx + 100, ly, 0xFFDD88, true);
        ly += 22;

        g.drawString(mc.font, Component.translatable("screen." + ImmortalCultivationMod.MODID + ".special_effect_label"), lx, ly, labelColor, true);
        ly += 12;

        String effectKey = method.bloodDemon()
                ? "method_special." + ImmortalCultivationMod.MODID + ".blood_demon"
                : CultivationMethods.isReincarnationTrueArt(methodId)
                ? "method_special." + ImmortalCultivationMod.MODID + ".reincarnation_true_art"
                : CultivationMethods.isTuntianDemonArt(methodId)
                ? "method_special." + ImmortalCultivationMod.MODID + ".tuntian_demon_art"
                : CultivationMethods.isPokongJue(methodId)
                ? "method_special." + ImmortalCultivationMod.MODID + ".pokong_jue"
                : CultivationMethods.isChangqingJue(methodId)
                ? "method_special." + ImmortalCultivationMod.MODID + ".changqing_jue"
                : CultivationMethods.isFentianLifeRenewal(methodId)
                ? "method_special." + ImmortalCultivationMod.MODID + ".fentian_life_renewal"
                : "method_special." + ImmortalCultivationMod.MODID + ".none";

        g.drawString(mc.font, Component.translatable(effectKey), lx, ly, 0xAAAAAA, true);

        if (ClientData.cultivationData.activeCultivationMethod().equals(methodId)) {
            Component active = Component.translatable("screen." + ImmortalCultivationMod.MODID + ".method_active");
            g.drawString(mc.font, active, x + W / 2 - mc.font.width(active) / 2, y + 172, 0x55FF55, true);
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