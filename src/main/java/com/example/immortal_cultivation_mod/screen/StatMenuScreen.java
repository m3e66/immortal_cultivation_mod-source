package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class StatMenuScreen extends Screen {
    private static final int WINDOW_W = 340;
    private static final int WINDOW_H = 292;

    private final List<Button> skillButtons = new ArrayList<>();
    private Button breakthroughBtn;

    public StatMenuScreen() {
        super(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".stat_menu"));
    }

    @Override
    protected void init() {
        skillButtons.clear();

        int x = (width - WINDOW_W) / 2;
        int y = (height - WINDOW_H) / 2;

        addSkillButton("hp", x + 12, y + 52);
        addSkillButton("qi", x + 12, y + 74);
        addSkillButton("energy", x + 12, y + 96);
        addSkillButton("physical", x + 12, y + 118);
        addSkillButton("magic", x + 12, y + 140);
        addSkillButton("mental", x + 12, y + 162);

        breakthroughBtn = new StyledButton(
                x + 110,
                y + 260,
                120,
                20,
                Component.literal(""),
                b -> PacketDistributor.sendToServer(new ModPayloads.ServerboundRequestBreakthroughPayload())
        );

        addRenderableWidget(breakthroughBtn);
    }

    private void addSkillButton(String stat, int x, int y) {
        Button button = new StyledButton(
                x,
                y,
                18,
                18,
                Component.literal("+"),
                b -> PacketDistributor.sendToServer(new ModPayloads.ServerboundSpendSkillPointPayload(stat))
        );
        skillButtons.add(button);
        addRenderableWidget(button);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        int x = (width - WINDOW_W) / 2;
        int y = (height - WINDOW_H) / 2;

        g.fill(x, y, x + WINDOW_W, y + WINDOW_H, 0xD0101010);
        g.renderOutline(x, y, WINDOW_W, WINDOW_H, 0xFFCC8800);

        Minecraft mc = Minecraft.getInstance();
        var data = ClientData.cultivationData;

        if (data == null) {
            g.drawString(mc.font, Component.literal("No data"), x + 10, y + 10, 0xFF5555, true);
            if (breakthroughBtn != null) {
                breakthroughBtn.visible = false;
            }
            skillButtons.forEach(b -> b.visible = false);
            super.render(g, mx, my, pt);
            return;
        }

        String level = CultivationLevels.isMortal(data.cultivationLevel()) ? CultivationLevels.REALM_MORTAL : data.cultivationLevel();
        var levelDef = CultivationLevels.getLevelDef(level);
        int maxHp = Math.max(1, levelDef.maxHp() + data.maxHpBonus());
        int maxQi = levelDef.maxQi() + data.maxQiBonus();
        int maxAge = Math.max(1, levelDef.maxAge() - data.agePenalty());

        g.drawString(mc.font, Component.translatable("screen." + ImmortalCultivationMod.MODID + ".stat_menu"), x + 12, y + 10, 0xFFFFAA, true);
        g.drawString(mc.font, Component.translatable("screen." + ImmortalCultivationMod.MODID + ".skill_points", data.skillPoints()), x + 190, y + 10, 0x55FFFF, true);
        g.drawString(mc.font, Component.translatable("screen." + ImmortalCultivationMod.MODID + ".cultivation_level").append(": ").append(level), x + 12, y + 28, 0xFFFFFF, true);

        drawSkillLine(g, mc, x, y + 54, "hp", 4, maxHp, data.maxHpBonus() / 4, 0xFFFFFF);
        drawSkillLine(g, mc, x, y + 76, "qi", 40, maxQi, data.maxQiBonus() / 40, 0x55FFFF);
        drawSkillLine(g, mc, x, y + 98, "energy", 40, data.maxEnergyBonus(), data.maxEnergyBonus() / 40, 0xFFFF55);
        drawSkillLine(g, mc, x, y + 120, "physical", 1, data.physicalAttack(), data.physicalAttack(), 0xFFAA66);
        drawSkillLine(g, mc, x, y + 142, "magic", 1, data.magicAttack(), data.magicAttack(), 0xAA88FF);
        drawSkillLine(g, mc, x, y + 164, "mental", 1, data.mentalAttack(), data.mentalAttack(), 0x88FFAA);

        g.drawString(mc.font, Component.translatable("screen." + ImmortalCultivationMod.MODID + ".body_type").append(": ").append(data.bodyType()), x + 12, y + 186, 0xAAFFDD, true);
        g.drawString(mc.font, Component.translatable("screen." + ImmortalCultivationMod.MODID + ".moral").append(": " + data.moral()), x + 180, y + 186, 0xDDFFAA, true);
        g.drawString(mc.font, Component.translatable("screen." + ImmortalCultivationMod.MODID + ".luck").append(": " + data.luck()), x + 12, y + 202, 0xFFFFAA, true);
        g.drawString(mc.font, Component.translatable("screen." + ImmortalCultivationMod.MODID + ".soul").append(": " + data.soul()), x + 115, y + 202, 0xDDAAFF, true);
        g.drawString(mc.font, Component.translatable("screen." + ImmortalCultivationMod.MODID + ".thoughts").append(": " + data.thoughts()), x + 220, y + 202, 0xAAFFFF, true);
        g.drawString(mc.font, Component.translatable("screen." + ImmortalCultivationMod.MODID + ".spirit_roots").append(": ").append(SpiritRoots.format(data.spiritRoots(), data.spiritRootGrade())), x + 12, y + 218, 0x88FFCC, true);

        var method = CultivationMethods.get(data.activeCultivationMethod());
        Component methodName = method == null ? Component.translatable("screen." + ImmortalCultivationMod.MODID + ".none") : Component.translatable(method.nameKey());
        g.drawString(mc.font, Component.translatable("screen." + ImmortalCultivationMod.MODID + ".cultivation_method").append(": ").append(methodName), x + 12, y + 234, 0xFFDD88, true);

        g.drawString(mc.font, Component.translatable("screen." + ImmortalCultivationMod.MODID + ".max_age").append(": " + maxAge), x + 12, y + 250, 0xAAAAAA, true);

        long progressNeeded = CultivationLevels.getTotalQiNeeded(level);
        g.drawString(mc.font, Component.translatable("screen." + ImmortalCultivationMod.MODID + ".cultivation_progress").append(": " + data.cultivationProgress() + "/" + progressNeeded), x + 145, y + 250, 0xFFAA55, true);

        String nextStage = CultivationLevels.getNextStage(level);
        boolean fullProgress = data.cultivationProgress() >= progressNeeded;
        boolean needsPill = CultivationLevels.needsBreakthroughPill(level);
        boolean hasPill = hasBreakthroughPill(mc);
        boolean canBreakthrough = nextStage != null && fullProgress && (!needsPill || hasPill);

        for (Button button : skillButtons) {
            button.active = data.skillPoints() > 0;
            button.visible = true;
        }

        if (nextStage != null) {
            breakthroughBtn.setMessage(needsPill && !hasPill
                    ? Component.translatable("message." + ImmortalCultivationMod.MODID + ".need_breakthrough_pill")
                    : Component.translatable("screen." + ImmortalCultivationMod.MODID + ".breakthrough_button"));
            breakthroughBtn.active = canBreakthrough;
            breakthroughBtn.visible = true;
            breakthroughBtn.setPosition(x + 110, y + 260);
        } else {
            breakthroughBtn.visible = false;
        }

        super.render(g, mx, my, pt);
    }

    private boolean hasBreakthroughPill(Minecraft mc) {
        if (mc.player == null) {
            return false;
        }
        for (var stack : mc.player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() == com.example.immortal_cultivation_mod.item.ModItems.BREAKTHROUGH_PILL.get()) {
                return true;
            }
        }
        return false;
    }

    private void drawSkillLine(GuiGraphics g, Minecraft mc, int x, int y, String key, int increase, int total, int invested, int color) {
        Component label = Component.translatable("screen." + ImmortalCultivationMod.MODID + ".skill_" + key, increase, total, invested);
        g.drawString(mc.font, label, x + 36, y + 4, color, true);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
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
