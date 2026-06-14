package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class DebugStatEditorScreen extends Screen {
    private static final int WINDOW_W = 320;
    private static final int WINDOW_H = 356;

    public DebugStatEditorScreen() {
        super(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".debug_stat_editor"));
    }

    @Override
    protected void init() {
        int x = (width - WINDOW_W) / 2;
        int y = (height - WINDOW_H) / 2;
        addAdjustRow("cultivation_level", x + 12, y + 30, 1);
        addAdjustRow("age", x + 12, y + 52, 10);
        addAdjustRow("moral", x + 12, y + 74, 10);
        addAdjustRow("luck", x + 12, y + 96, 10);
        addAdjustRow("soul", x + 12, y + 118, 10);
        addAdjustRow("thoughts", x + 12, y + 140, 10);
        addAdjustRow("spirit_roots", x + 12, y + 162, 1);
        addAdjustRow("spirit_root_grade", x + 12, y + 184, 1);
        addAdjustRow("skill_points", x + 12, y + 206, 10);
        addAdjustRow("max_hp", x + 12, y + 228, 20);
        addAdjustRow("max_qi", x + 12, y + 250, 100);
        addAdjustRow("max_energy", x + 12, y + 272, 100);
        addAdjustRow("physical", x + 12, y + 294, 5);
        addAdjustRow("magic", x + 12, y + 316, 5);
        addAdjustRow("mental", x + 12, y + 338, 5);
    }

    private void addAdjustRow(String stat, int x, int y, int step) {
        addRenderableWidget(Button.builder(Component.literal("-"), b ->
                PacketDistributor.sendToServer(new ModPayloads.ServerboundDebugAdjustStatPayload(stat, -step))
        ).bounds(x + 238, y, 22, 18).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b ->
                PacketDistributor.sendToServer(new ModPayloads.ServerboundDebugAdjustStatPayload(stat, step))
        ).bounds(x + 264, y, 22, 18).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = (width - WINDOW_W) / 2;
        int y = (height - WINDOW_H) / 2;
        Minecraft mc = Minecraft.getInstance();
        var data = ClientData.cultivationData;

        g.fill(x, y, x + WINDOW_W, y + WINDOW_H, 0xD0181818);
        g.renderOutline(x, y, WINDOW_W, WINDOW_H, 0xFF55DDFF);
        g.drawString(mc.font, title, x + 12, y + 10, 0x55DDFF, true);

        if (data == null) {
            g.drawString(mc.font, Component.literal("No player data"), x + 12, y + 32, 0xFF5555, true);
            super.render(g, mouseX, mouseY, partialTick);
            return;
        }

        var levelDef = CultivationLevels.getLevelDef(data.cultivationLevel());
        drawValue(g, mc, x + 12, y + 34, "cultivation_level", data.cultivationLevel(), 0xFFFFFF);
        drawValue(g, mc, x + 12, y + 56, "age", Integer.toString(Math.max(1, levelDef.maxAge() - data.agePenalty())), 0xAAAAAA);
        drawValue(g, mc, x + 12, y + 78, "moral", Integer.toString(data.moral()), 0xDDFFAA);
        drawValue(g, mc, x + 12, y + 100, "luck", Integer.toString(data.luck()), 0xFFFFAA);
        drawValue(g, mc, x + 12, y + 122, "soul", Integer.toString(data.soul()), 0xDDAAFF);
        drawValue(g, mc, x + 12, y + 144, "thoughts", Integer.toString(data.thoughts()), 0xAAFFFF);
        drawValue(g, mc, x + 12, y + 166, "spirit_roots", String.join("", data.spiritRoots()), 0x88FFCC);
        drawValue(g, mc, x + 12, y + 188, "spirit_root_grade", data.spiritRootGrade(), 0xFFDDAA);
        drawValue(g, mc, x + 12, y + 210, "skill_points_debug", Integer.toString(data.skillPoints()), 0x55FFFF);
        drawValue(g, mc, x + 12, y + 232, "max_hp", Integer.toString(levelDef.maxHp() + data.maxHpBonus()), 0xFFFFFF);
        drawValue(g, mc, x + 12, y + 254, "max_qi", Integer.toString(levelDef.maxQi() + data.maxQiBonus()), 0x55FFFF);
        drawValue(g, mc, x + 12, y + 276, "max_energy", Integer.toString(data.maxEnergyBonus()), 0xFFFF55);
        drawValue(g, mc, x + 12, y + 298, "physical", Integer.toString(data.physicalAttack()), 0xFFAA66);
        drawValue(g, mc, x + 12, y + 320, "magic", Integer.toString(data.magicAttack()), 0xAA88FF);
        drawValue(g, mc, x + 12, y + 342, "mental", Integer.toString(data.mentalAttack()), 0x88FFAA);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawValue(GuiGraphics g, Minecraft mc, int x, int y, String key, String value, int color) {
        Component label = Component.translatable("screen." + ImmortalCultivationMod.MODID + "." + key).append(": ").append(value);
        g.drawString(mc.font, label, x, y, color, true);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
