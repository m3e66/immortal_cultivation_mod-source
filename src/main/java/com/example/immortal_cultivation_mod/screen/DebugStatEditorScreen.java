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

import java.util.ArrayList;
import java.util.List;

public class DebugStatEditorScreen extends Screen {
    private static final int WINDOW_W = 320;
    private static final int WINDOW_H = 320;

    private final List<ElementToggle> elementToggles = new ArrayList<>();
    private final List<GradeToggle> gradeToggles = new ArrayList<>();

    private int page = 0;

    public DebugStatEditorScreen() {
        super(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".debug_stat_editor"));
    }

    @Override
    protected void init() {
        elementToggles.clear();
        gradeToggles.clear();
        clearWidgets();

        int x = (width - WINDOW_W) / 2;
        int y = (height - WINDOW_H) / 2;

        addPageButton(0, x + 12, y + 30, "Stats");
        addPageButton(1, x + 90, y + 30, "Spirit Roots");

        if (page == 1) {
            addSpiritRootButtons(x + 12, y + 98);
            addGradeButtons(x + 12, y + 158);
            return;
        }

        addAdjustRow("cultivation_level", x + 12, y + 58, 1);
        addAdjustRow("age", x + 12, y + 76, 10);
        addAdjustRow("moral", x + 12, y + 94, 10);
        addAdjustRow("luck", x + 12, y + 112, 10);
        addAdjustRow("soul", x + 12, y + 130, 10);
        addAdjustRow("thoughts", x + 12, y + 148, 10);
        addAdjustRow("skill_points", x + 12, y + 184, 10);
        addAdjustRow("max_hp", x + 12, y + 202, 20);
        addAdjustRow("max_qi", x + 12, y + 220, 100);
        addAdjustRow("max_energy", x + 12, y + 238, 100);
        addAdjustRow("physical", x + 12, y + 256, 5);
        addAdjustRow("magic", x + 12, y + 274, 5);
        addAdjustRow("mental", x + 12, y + 292, 5);
    }

    private void addPageButton(int targetPage, int x, int y, String label) {
        addRenderableWidget(new DebugButton(x, y, 72, 18, Component.literal((page == targetPage ? "[X] " : "") + label), b -> {
            page = targetPage;
            init();
        }));
    }

    private void addAdjustRow(String stat, int x, int y, int step) {
        addRenderableWidget(new DebugButton(x + 238, y, 22, 18, Component.literal("-"), b ->
                PacketDistributor.sendToServer(new ModPayloads.ServerboundDebugAdjustStatPayload(stat, -step))));
        addRenderableWidget(new DebugButton(x + 264, y, 22, 18, Component.literal("+"), b ->
                PacketDistributor.sendToServer(new ModPayloads.ServerboundDebugAdjustStatPayload(stat, step))));
    }

    private void addSpiritRootButtons(int x, int y) {
        for (int i = 0; i < SpiritRoots.ALL_ELEMENTS.size(); i++) {
            String element = SpiritRoots.ALL_ELEMENTS.get(i);
            int bx = x + (i % 5) * 58;
            int by = y + (i / 5) * 22;
            Button button = new DebugButton(bx, by, 52, 18, Component.literal(element), b -> toggleRoot(element));
            elementToggles.add(new ElementToggle(element, button));
            addRenderableWidget(button);
        }
    }

    private void addGradeButtons(int x, int y) {
        for (int i = 0; i < SpiritRoots.GRADES.size(); i++) {
            String grade = SpiritRoots.GRADES.get(i);
            Button button = new DebugButton(x + i * 58, y, 52, 18, Component.literal(grade), b -> setGrade(grade));
            gradeToggles.add(new GradeToggle(grade, button));
            addRenderableWidget(button);
        }
    }

    private void toggleRoot(String element) {
        var data = ClientData.cultivationData;
        if (data == null) return;

        List<String> roots = new ArrayList<>(SpiritRoots.sanitizeRootList(data.spiritRoots()));

        if (roots.remove(element)) {
            sendSpiritRoots(roots, data.spiritRootGrade());
            return;
        }

        if (roots.size() < 5) {
            roots.add(element);
            sendSpiritRoots(roots, data.spiritRootGrade());
        }
    }

    private void setGrade(String grade) {
        var data = ClientData.cultivationData;
        if (data != null) {
            sendSpiritRoots(SpiritRoots.sanitizeRootList(data.spiritRoots()), grade);
        }
    }

    private void sendSpiritRoots(List<String> roots, String grade) {
        PacketDistributor.sendToServer(new ModPayloads.ServerboundDebugSetSpiritRootsPayload(roots, SpiritRoots.sanitizeGrade(grade)));
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
            g.drawString(mc.font, Component.literal("No player data"), x + 12, y + 66, 0xFF5555, true);
            super.render(g, mouseX, mouseY, partialTick);
            return;
        }
        if (page == 1) {
            drawValue(g, mc, x + 12, y + 62, "spirit_roots", SpiritRoots.format(data.spiritRoots(), data.spiritRootGrade()), 0x88FFCC);
            g.drawString(mc.font, Component.translatable("screen." + ImmortalCultivationMod.MODID + ".spirit_root_elements"), x + 12, y + 88, 0x88FFCC, true);
            g.drawString(mc.font, Component.translatable("screen." + ImmortalCultivationMod.MODID + ".spirit_root_grade"), x + 12, y + 150, 0xFFDDAA, true);
            updateSpiritRootButtons(data.spiritRoots(), data.spiritRootGrade());
        } else {
            var levelDef = CultivationLevels.getLevelDef(data.cultivationLevel());

            drawValue(g, mc, x + 12, y + 62, "cultivation_level", data.cultivationLevel(), 0xFFFFFF);
            drawValue(g, mc, x + 12, y + 80, "age", Integer.toString(Math.max(1, levelDef.maxAge() - data.agePenalty())), 0xAAAAAA);
            drawValue(g, mc, x + 12, y + 98, "moral", Integer.toString(data.moral()), 0xDDFFAA);
            drawValue(g, mc, x + 12, y + 116, "luck", Integer.toString(data.luck()), 0xFFFFAA);
            drawValue(g, mc, x + 12, y + 134, "soul", Integer.toString(data.soul()), 0xDDAAFF);
            drawValue(g, mc, x + 12, y + 152, "thoughts", Integer.toString(data.thoughts()), 0xAAFFFF);
            drawValue(g, mc, x + 12, y + 170, "spirit_roots", SpiritRoots.format(data.spiritRoots(), data.spiritRootGrade()), 0x88FFCC);
            drawValue(g, mc, x + 12, y + 188, "skill_points_debug", Integer.toString(data.skillPoints()), 0x55FFFF);
            drawValue(g, mc, x + 12, y + 206, "max_hp", Integer.toString(levelDef.maxHp() + data.maxHpBonus()), 0xFFFFFF);
            drawValue(g, mc, x + 12, y + 224, "max_qi", Integer.toString(levelDef.maxQi() + data.maxQiBonus()), 0x55FFFF);
            drawValue(g, mc, x + 12, y + 242, "max_energy", Integer.toString(data.maxEnergyBonus()), 0xFFFF55);
            drawValue(g, mc, x + 12, y + 260, "physical", Integer.toString(data.physicalAttack()), 0xFFAA66);
            drawValue(g, mc, x + 12, y + 278, "magic", Integer.toString(data.magicAttack()), 0xAA88FF);
            drawValue(g, mc, x + 12, y + 296, "mental", Integer.toString(data.mentalAttack()), 0x88FFAA);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void updateSpiritRootButtons(List<String> roots, String grade) {
        List<String> selected = SpiritRoots.sanitizeRootList(roots);
        boolean full = selected.size() >= 5;

        for (ElementToggle toggle : elementToggles) {
            boolean active = selected.contains(toggle.element());
            toggle.button().setMessage(Component.literal((active ? "[X] " : "[ ] ") + toggle.element()));
            toggle.button().active = active || !full;
        }

        String cleanGrade = SpiritRoots.sanitizeGrade(grade);

        for (GradeToggle toggle : gradeToggles) {
            boolean active = cleanGrade.equals(toggle.grade());
            toggle.button().setMessage(Component.literal((active ? "[X] " : "[ ] ") + toggle.grade()));
        }
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

    private record ElementToggle(String element, Button button) {
    }

    private record GradeToggle(String grade, Button button) {
    }

    private static class DebugButton extends Button {
        DebugButton(int x, int y, int width, int height, Component message, OnPress onPress) {
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
