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

        renderPreparedSpell(g, mc);
        renderPreparedCharge(g, mc);
        renderShieldBar(g, mc);
    }

    private void renderPreparedSpell(GuiGraphics g, Minecraft mc) {
        Component spellName = ClientData.preparedSpellName();
        if (spellName.getString().isEmpty()) {
            return;
        }
        Component text = Component.translatable("hud." + ImmortalCultivationMod.MODID + ".prepared_spell", spellName);
        int textWidth = mc.font.width(text);
        int x = g.guiWidth() / 2 + 112;
        x = Math.min(x, g.guiWidth() - textWidth - 8);
        int y = g.guiHeight() - 27;
        g.fill(x - 5, y - 3, x + textWidth + 5, y + 10, 0x99000000);
        g.drawString(mc.font, text, x, y, 0xFFAAEEFF, true);
    }

    private void renderPreparedCharge(GuiGraphics g, Minecraft mc) {
        if (ClientData.preparedCharging()) {
            float progress = Math.max(0.0F, Math.min(1.0F, ClientData.preparedChargeScale() - 1.0F));
            int barWidth = 102;
            int barHeight = 6;
            int fill = Math.max(1, (int) (barWidth * progress));
            int x = g.guiWidth() / 2 - barWidth / 2;
            int barY = g.guiHeight() - 67;
            String percent = ClientData.preparedChargePercent() + "%";
            g.fill(x - 5, barY - 12, x + barWidth + 5, barY + barHeight + 4, 0x99000000);
            g.fill(x, barY, x + barWidth, barY + barHeight, 0xFF252525);
            g.fill(x, barY, x + fill, barY + barHeight, 0xFF44CCFF);
            g.drawString(mc.font, percent, x + barWidth / 2 - mc.font.width(percent) / 2, barY - 10, 0xFFAAEEFF, true);
        }
    }

    private void renderShieldBar(GuiGraphics g, Minecraft mc) {
        float shield = ClientData.shieldAmount();
        float shieldMax = ClientData.shieldMax();
        if (shield <= 0.0F || shieldMax <= 0.0F) {
            return;
        }

        int barWidth = 8;
        int barHeight = 44;
        int x = g.guiWidth() / 2 + 95;
        int y = g.guiHeight() - 45;
        float ratio = Math.min(1.0F, shield / shieldMax);
        int fillHeight = Math.max(1, (int) (barHeight * ratio));

        RenderSystem.setShaderColor(0.05F, 0.12F, 0.18F, 0.85F);
        g.blit(FILL, x, y, 0, 0, barWidth, barHeight, 1, 1);
        RenderSystem.setShaderColor(0.25F, 0.78F, 1.0F, 1.0F);
        g.blit(FILL, x, y + barHeight - fillHeight, 0, 0, barWidth, fillHeight, 1, 1);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        String text = String.valueOf(Math.round(shield));
        int textX = x + barWidth / 2 - mc.font.width(text) / 2;
        g.drawString(mc.font, text, textX, y - 10, 0xFF66DDFF, true);
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
