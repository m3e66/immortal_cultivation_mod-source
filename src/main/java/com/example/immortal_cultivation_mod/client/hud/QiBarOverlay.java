package com.example.immortal_cultivation_mod.client.hud;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class QiBarOverlay implements LayeredDraw.Layer {
    public static final ResourceLocation HUD_ID =
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "qi_bar");

    public static final QiBarOverlay INSTANCE = new QiBarOverlay();

    private static final ResourceLocation FILL =
            ResourceLocation.fromNamespaceAndPath("autoforge_bricks", "textures/gui/fill_white.png");

    private QiBarOverlay() {}

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        var data = ClientData.cultivationData;
        if (data == null) return;

        var levelDef = CultivationLevels.getLevelDef(data.cultivationLevel());
        int maxQi = levelDef.maxQi() + data.maxQiBonus();
        if (maxQi <= 0) return;

        int barWidth = 81;
        int barHeight = 6;
        int x = g.guiWidth() / 2 + 10;
        int y = g.guiHeight() - 49;

        if (CultivationMethods.isBloodDemon(data.activeCultivationMethod())) {
            int maxBlood = maxQi * 10;
            int bloodY = y - 16;
            RenderSystem.setShaderColor(0.18f, 0.02f, 0.02f, 0.85f);
            g.blit(FILL, x, bloodY, 0, 0, barWidth, barHeight, 1, 1);
            float bloodRatio = Math.min(1.0f, (float) data.blood() / maxBlood);
            int bloodW = (int) (barWidth * bloodRatio);
            if (bloodW > 0) {
                RenderSystem.setShaderColor(0.9f, 0.02f, 0.04f, 1.0f);
                g.blit(FILL, x, bloodY, 0, 0, bloodW, barHeight, 1, 1);
            }
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            g.drawString(mc.font, Component.translatable("hud." + ImmortalCultivationMod.MODID + ".blood", data.blood(), maxBlood), x + 1, bloodY - 10, 0xFFFF4444, true);
        }

        RenderSystem.setShaderColor(0.2f, 0.2f, 0.2f, 0.8f);
        g.blit(FILL, x, y, 0, 0, barWidth, barHeight, 1, 1);

        float ratio = Math.min(1.0f, (float) data.qi() / maxQi);
        int fillW = (int) (barWidth * ratio);
        if (fillW > 0) {
            float[] rgb = getQiColor(ratio);
            RenderSystem.setShaderColor(rgb[0], rgb[1], rgb[2], 1.0f);
            g.blit(FILL, x, y, 0, 0, fillW, barHeight, 1, 1);
        }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        Component text = Component.translatable("hud." + ImmortalCultivationMod.MODID + ".qi", data.qi(), maxQi);
        int textColor = getTextColor(ratio);
        g.drawString(mc.font, text, x + 1, y - 10, textColor, true);
    }

    private float[] getQiColor(float ratio) {
        if (ratio > 0.5f) return new float[]{0.33f, 1.0f, 0.33f};
        if (ratio > 0.25f) return new float[]{1.0f, 1.0f, 0.33f};
        return new float[]{1.0f, 0.33f, 0.33f};
    }

    private int getTextColor(float ratio) {
        if (ratio > 0.5f) return 0xFF55FF55;
        if (ratio > 0.25f) return 0xFFFFFF55;
        return 0xFFFF5555;
    }
}
