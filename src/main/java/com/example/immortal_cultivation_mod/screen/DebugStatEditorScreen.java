package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.BodyTypes;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.item.TalismanRecipes;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import com.example.immortal_cultivation_mod.spell.ModSpells;
import com.example.immortal_cultivation_mod.system.OuterSystemType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class DebugStatEditorScreen extends Screen {
    static final int WINDOW_W = 320;
    static final int WINDOW_H = 320;
    static final String[] TAB_KEYS = {"stats", "roots", "methods", "jindan", "proficiency", "system", "talisman_mastery", "weapon"};
    private static final int VISIBLE_METHOD_ROWS = 11;
    private static final int VISIBLE_TALISMAN_ROWS = 11;
    private static final String METHOD_PROFICIENCY_PREFIX = "method_proficiency:";
    private static final String CRAFT_PROFICIENCY_PREFIX = "craft_proficiency:";
    private static final String TALISMAN_MASTERY_PREFIX = "talisman_mastery:";
    private static final List<String> CRAFT_PROFICIENCIES = List.of(
            "forging",
            "alchemy",
            "talisman",
            "beast_taming",
            "puppet",
            "corpse_control",
            "gu",
            "formation",
            "divination",
            "ghost_control");
    private static final List<String> JINDAN_QUALITIES = List.of(
            "liedan", "xiapin", "zhongpin", "shangpin", "jipin", "yuanman", "jiuzhuan");

    private final List<ElementToggle> elementToggles = new ArrayList<>();
    private final List<GradeToggle> gradeToggles = new ArrayList<>();
    private final List<QualityToggle> qualityToggles = new ArrayList<>();
    private final List<SystemToggle> systemToggles = new ArrayList<>();
    private final UUID targetId;
    private final String targetName;
    private String selectedSystemId;

    private int page = 0;
    private int methodScroll = 0;
    private int talismanScroll = 0;
    private boolean closeSyncSent;

    public DebugStatEditorScreen(UUID targetId, String targetName, String systemType) {
        this(targetId, targetName, systemType, 0);
    }

    public DebugStatEditorScreen(UUID targetId, String targetName, String systemType, int initialPage) {
        super(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".debug_stat_editor"));
        this.page = Math.clamp(initialPage, 0, 6);
        this.targetId = targetId;
        this.targetName = targetName;
        this.selectedSystemId = OuterSystemType.byId(systemType).id();
    }

    public UUID targetId() {
        return targetId;
    }

    @Override
    protected void init() {
        elementToggles.clear();
        gradeToggles.clear();
        qualityToggles.clear();
        systemToggles.clear();
        clearWidgets();

        int x = (width - WINDOW_W) / 2;
        int y = (height - WINDOW_H) / 2;

        for (int i = 0; i < TAB_KEYS.length; i++) addPageButton(i, x + 10 + i * 37, y + 30, TAB_KEYS[i]);
        addHintControls(x + 264, y + 5);
        adjustStat("restriction_hints", 0);

        if (page == 1) {
            addSpiritRootButtons(x + 12, y + 98);
            addGradeButtons(x + 12, y + 158);
            addRenderableWidget(new DebugButton(x + 238, y + 204, 22, 18, Component.literal("<"), b ->
                    adjustStat("body_type", -1)));
            addRenderableWidget(new DebugButton(x + 264, y + 204, 22, 18, Component.literal(">"), b ->
                    adjustStat("body_type", 1)));
            return;
        }
        if (page == 2) {
            addMethodProficiencyButtons(x + 12, y + 62);
            return;
        }
        if (page == 3) {
            addJindanQualityButtons(x + 12, y + 92);
            return;
        }
        if (page == 4) {
            addCraftProficiencyButtons(x + 12, y + 62);
            return;
        }
        if (page == 5) {
            addSystemButtons(x + 12, y + 92);
            return;
        }
        if (page == 6) {
            addTalismanMasteryButtons(x + 12, y + 62);
            return;
        }

        addAdjustRow("cultivation_level", x + 12, y + 58, 1);
        addAdjustRow("age", x + 12, y + 76, 10);
        addAdjustRow("moral", x + 12, y + 94, 10);
        addAdjustRow("luck", x + 12, y + 112, 10);
        addAdjustRow("soul", x + 12, y + 130, 10);
        addAdjustRow("thoughts", x + 12, y + 148, 10);
        addAdjustRow("talent", x + 12, y + 166, 1);
        addAdjustRow("skill_points", x + 12, y + 184, 10);
        addAdjustRow("max_hp", x + 12, y + 202, 20);
        addAdjustRow("max_qi", x + 12, y + 220, 100);
        addAdjustRow("max_energy", x + 12, y + 238, 40);
        addAdjustRow("physical", x + 12, y + 256, 5);
        addAdjustRow("magic", x + 12, y + 274, 5);
        addAdjustRow("mental", x + 12, y + 292, 5);
    }

    private void addPageButton(int targetPage, int x, int y, String key) {
        Component label = Component.literal(page == targetPage ? "[X] " : "")
                .append(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".debug_tab_" + key));
        addRenderableWidget(new DebugButton(x, y, 36, 18, label, b -> {
            if (targetPage == 7) {
                PacketDistributor.sendToServer(new com.example.immortal_cultivation_mod.network.DebugWeaponEditorPayloads.Open());
                return;
            }
            page = targetPage;
            init();
        }));
    }

    private void addAdjustRow(String stat, int x, int y, int step) {
        addRenderableWidget(new DebugButton(x + 238, y, 22, 18, Component.literal("-"), b ->
                adjustStat(stat, -step)));
        addRenderableWidget(new DebugButton(x + 264, y, 22, 18, Component.literal("+"), b ->
                adjustStat(stat, step)));
    }

    private void addHintControls(int x, int y) {
        addRenderableWidget(new DebugButton(x, y, 22, 18, Component.literal("-"), b ->
                adjustStat("restriction_hints", -1)));
        addRenderableWidget(new DebugButton(x + 26, y, 22, 18, Component.literal("+"), b ->
                adjustStat("restriction_hints", 1)));
    }

    private void addMethodProficiencyButtons(int x, int y) {
        List<CultivationMethods.MethodDef> methods = learnedMethods();
        methodScroll = Math.max(0, Math.min(methodScroll, Math.max(0, methods.size() - VISIBLE_METHOD_ROWS)));
        int visible = Math.min(VISIBLE_METHOD_ROWS, methods.size() - methodScroll);
        for (int i = 0; i < visible; i++) {
            String stat = METHOD_PROFICIENCY_PREFIX + methods.get(methodScroll + i).id();
            int rowY = y + i * 22;
            addRenderableWidget(new DebugButton(x + 218, rowY, 32, 18, Component.literal("-100"), b ->
                    adjustStat(stat, -100)));
            addRenderableWidget(new DebugButton(x + 254, rowY, 32, 18, Component.literal("+100"), b ->
                    adjustStat(stat, 100)));
        }
    }

    private void addJindanQualityButtons(int x, int y) {
        for (int i = 0; i < JINDAN_QUALITIES.size(); i++) {
            String quality = JINDAN_QUALITIES.get(i);
            int bx = x + (i % 2) * 148;
            int by = y + (i / 2) * 32;
            Button button = new DebugButton(
                    bx,
                    by,
                    140,
                    22,
                    Component.translatable("jindan_quality." + ImmortalCultivationMod.MODID + "." + quality),
                    b -> PacketDistributor.sendToServer(
                            new ModPayloads.ServerboundDebugSetJindanQualityPayload(targetId, quality)));
            qualityToggles.add(new QualityToggle(quality, button));
            addRenderableWidget(button);
        }
    }

    private void addCraftProficiencyButtons(int x, int y) {
        for (int i = 0; i < CRAFT_PROFICIENCIES.size(); i++) {
            String stat = CRAFT_PROFICIENCY_PREFIX + CRAFT_PROFICIENCIES.get(i);
            int rowY = y + i * 22;
            addRenderableWidget(new DebugButton(x + 174, rowY, 32, 18, Component.literal("-100"), b ->
                    adjustStat(stat, -100)));
            addRenderableWidget(new DebugButton(x + 210, rowY, 22, 18, Component.literal("-1"), b ->
                    adjustStat(stat, -1)));
            addRenderableWidget(new DebugButton(x + 236, rowY, 22, 18, Component.literal("+1"), b ->
                    adjustStat(stat, 1)));
            addRenderableWidget(new DebugButton(x + 262, rowY, 36, 18, Component.literal("+100"), b ->
                    adjustStat(stat, 100)));
        }
    }

    private void addTalismanMasteryButtons(int x, int y) {
        List<TalismanRecipes.Def> recipes = TalismanRecipes.all();
        talismanScroll = Math.max(0, Math.min(
                talismanScroll, Math.max(0, recipes.size() - VISIBLE_TALISMAN_ROWS)));
        int visible = Math.min(VISIBLE_TALISMAN_ROWS, recipes.size() - talismanScroll);
        for (int i = 0; i < visible; i++) {
            TalismanRecipes.Def recipe = recipes.get(talismanScroll + i);
            String stat = TALISMAN_MASTERY_PREFIX + recipe.id();
            int rowY = y + i * 22;
            addRenderableWidget(new DebugButton(x + 174, rowY, 32, 18,
                    Component.literal("-1"), b -> adjustStat(stat, -1)));
            addRenderableWidget(new DebugButton(x + 210, rowY, 32, 18,
                    Component.literal("+1"), b -> adjustStat(stat, 1)));
            addRenderableWidget(new DebugButton(x + 246, rowY, 52, 18,
                    Component.translatable(
                            "screen." + ImmortalCultivationMod.MODID + ".debug_master"),
                    b -> masterTalisman(recipe)));
        }
    }

    private void masterTalisman(TalismanRecipes.Def recipe) {
        var data = ClientData.cultivationData;
        if (data == null) {
            return;
        }
        int remaining = TalismanRecipes.masterySuccesses(recipe)
                - TalismanRecipes.successCount(data, recipe.id());
        if (remaining > 0) {
            adjustStat(TALISMAN_MASTERY_PREFIX + recipe.id(), remaining);
        }
    }

    private void addSystemButtons(int x, int y) {
        OuterSystemType[] types = OuterSystemType.values();
        for (int i = 0; i < types.length; i++) {
            OuterSystemType type = types[i];
            int bx = i == types.length - 1 ? x : x + (i % 2) * 148;
            int by = y + (i / 2) * 34;
            int buttonWidth = i == types.length - 1 ? 296 : 140;
            Button button = new DebugButton(
                    bx, by, buttonWidth, 24,
                    Component.translatable(type.translationKey()),
                    b -> setSystem(type));
            systemToggles.add(new SystemToggle(type, button));
            addRenderableWidget(button);
        }
    }

    private void setSystem(OuterSystemType type) {
        selectedSystemId = type.id();
        updateSystemButtons();
        PacketDistributor.sendToServer(new ModPayloads.ServerboundDebugSetOuterSystemPayload(
                targetId, type.id()));
    }

    private List<CultivationMethods.MethodDef> learnedMethods() {
        var data = ClientData.cultivationData;
        if (data == null) {
            return List.of();
        }
        Set<String> learned = data.methodProficiencies().keySet();
        return CultivationMethods.allMethods().stream()
                .filter(method -> learned.contains(method.id()) || method.id().equals(data.activeCultivationMethod()))
                .toList();
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
        PacketDistributor.sendToServer(new ModPayloads.ServerboundDebugSetSpiritRootsPayload(
                targetId, roots, SpiritRoots.sanitizeGrade(grade)));
    }

    private void adjustStat(String stat, int delta) {
        PacketDistributor.sendToServer(new ModPayloads.ServerboundDebugAdjustStatPayload(
                targetId, stat, delta));
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
        g.drawString(mc.font, Component.literal(targetName), x + 12, y + 20, 0xFFEAFDFF, true);
        drawValue(g, mc, x + 128, y + 10, "restriction_hints",
                Integer.toString(ClientData.restrictionHints()), 0xFFEAFDFF);

        if (data == null) {
            g.drawString(mc.font, Component.literal("No player data"), x + 12, y + 66, 0xFF5555, true);
            super.render(g, mouseX, mouseY, partialTick);
            return;
        }
        if (page == 1) {
            drawValue(g, mc, x + 12, y + 62, "spirit_roots", SpiritRoots.format(data.spiritRoots(), data.spiritRootGrade()), 0x88FFCC);
            g.drawString(mc.font, Component.translatable("screen." + ImmortalCultivationMod.MODID + ".spirit_root_elements"), x + 12, y + 88, 0x88FFCC, true);
            g.drawString(mc.font, Component.translatable("screen." + ImmortalCultivationMod.MODID + ".spirit_root_grade"), x + 12, y + 150, 0xFFDDAA, true);
            drawValue(g, mc, x + 12, y + 208, "body_type",
                    BodyTypes.sanitize(data.bodyType()), 0xFFD0B8FF);
            updateSpiritRootButtons(data.spiritRoots(), data.spiritRootGrade());
        } else if (page == 2) {
            drawMethodProficiencies(g, mc, data, x + 12, y + 66);
        } else if (page == 3) {
            Component current = Component.translatable(
                    "jindan_quality." + ImmortalCultivationMod.MODID + "." + ClientData.jindanQuality);
            g.drawString(mc.font,
                    Component.translatable("screen." + ImmortalCultivationMod.MODID + ".debug_jindan_quality", current),
                    x + 12, y + 66, 0xFFD76A, true);
            updateQualityButtons();
        } else if (page == 4) {
            drawCraftProficiencies(g, mc, data, x + 12, y + 66);
        } else if (page == 5) {
            Component selected = Component.translatable(
                    OuterSystemType.byId(selectedSystemId).translationKey());
            g.drawString(mc.font, Component.translatable(
                            "screen." + ImmortalCultivationMod.MODID + ".debug_system_selected",
                            selected),
                    x + 12, y + 66, 0xFFE1A8, true);
            g.drawString(mc.font, Component.translatable(
                            "screen." + ImmortalCultivationMod.MODID + ".debug_system_note"),
                    x + 12, y + 78, 0xAAAAAA, false);
            updateSystemButtons();
        } else if (page == 6) {
            drawTalismanMasteries(g, mc, data, x + 12, y + 66);
        } else {
            var levelDef = CultivationLevels.getLevelDef(data.cultivationLevel());

            drawValue(g, mc, x + 12, y + 62, "cultivation_level", CultivationLevels.displayName(data.cultivationLevel()), 0xFFFFFF);
            drawValue(g, mc, x + 12, y + 80, "age", Integer.toString(Math.max(1, levelDef.maxAge() - data.agePenalty())), 0xAAAAAA);
            drawValue(g, mc, x + 12, y + 98, "moral", Integer.toString(data.moral()), 0xDDFFAA);
            drawValue(g, mc, x + 12, y + 116, "luck", Integer.toString(data.luck()), 0xFFFFAA);
            drawValue(g, mc, x + 12, y + 134, "soul", Integer.toString(data.soul()), 0xDDAAFF);
            drawValue(g, mc, x + 12, y + 152, "thoughts", Integer.toString(data.thoughts()), 0xAAFFFF);
            drawValue(g, mc, x + 12, y + 170, "talent",
                    String.format(java.util.Locale.ROOT, "%.1f", data.talent()), 0xFFD76A);
            drawValue(g, mc, x + 12, y + 188, "skill_points_debug", Integer.toString(data.skillPoints()), 0x55FFFF);
            drawValue(g, mc, x + 12, y + 206, "max_hp", Integer.toString(levelDef.maxHp() + data.maxHpBonus()), 0xFFFFFF);
            drawValue(g, mc, x + 12, y + 224, "max_qi", Integer.toString(levelDef.maxQi() + data.maxQiBonus()), 0x55FFFF);
            drawValue(g, mc, x + 12, y + 242, "max_energy", Integer.toString(data.thoughtSkillBonus()), 0xFFFF55);
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
        drawValue(g, mc, x, y, key, Component.literal(value), color);
    }

    private void drawValue(GuiGraphics g, Minecraft mc, int x, int y, String key, Component value, int color) {
        Component label = Component.translatable("screen." + ImmortalCultivationMod.MODID + "." + key)
                .append(": ")
                .append(value);
        g.drawString(mc.font, label, x, y, color, true);
    }

    private void drawMethodProficiencies(GuiGraphics g, Minecraft mc, com.example.immortal_cultivation_mod.attachment.ModAttachments.CultivationData data, int x, int y) {
        List<CultivationMethods.MethodDef> methods = learnedMethods();
        if (methods.isEmpty()) {
            g.drawString(mc.font,
                    Component.translatable("screen." + ImmortalCultivationMod.MODID + ".debug_no_learned_methods"),
                    x, y, 0xAAAAAA, false);
            return;
        }
        methodScroll = Math.max(0, Math.min(methodScroll, Math.max(0, methods.size() - VISIBLE_METHOD_ROWS)));
        int visible = Math.min(VISIBLE_METHOD_ROWS, methods.size() - methodScroll);
        for (int i = 0; i < visible; i++) {
            CultivationMethods.MethodDef method = methods.get(methodScroll + i);
            int value = data.methodProficiency(method.id());
            Component label = Component.translatable(method.nameKey())
                    .append(": ")
                    .append(Integer.toString(value))
                    .append(" / ")
                    .append(CultivationMethods.methodProficiencyLevel(data, method.id()));
            g.drawString(mc.font, label, x, y + i * 22, 0xE6D0FF, true);
        }
    }

    private void drawCraftProficiencies(GuiGraphics g, Minecraft mc,
                                         com.example.immortal_cultivation_mod.attachment.ModAttachments.CultivationData data,
                                         int x, int y) {
        for (int i = 0; i < CRAFT_PROFICIENCIES.size(); i++) {
            String id = CRAFT_PROFICIENCIES.get(i);
            int value = craftProficiency(data, id);
            Component label = Component.translatable(
                            "screen." + ImmortalCultivationMod.MODID + ".debug_proficiency_" + id)
                    .append(": ")
                    .append(Integer.toString(value))
                    .append(" / ")
                    .append(proficiencyTier(value));
            drawFittedLabel(g, mc, label, x, y + i * 22, 168, 0xFFE1A8);
        }
    }

    private void drawTalismanMasteries(GuiGraphics g, Minecraft mc,
                                        com.example.immortal_cultivation_mod.attachment.ModAttachments.CultivationData data,
                                        int x, int y) {
        List<TalismanRecipes.Def> recipes = TalismanRecipes.all();
        talismanScroll = Math.max(0, Math.min(
                talismanScroll, Math.max(0, recipes.size() - VISIBLE_TALISMAN_ROWS)));
        int visible = Math.min(VISIBLE_TALISMAN_ROWS, recipes.size() - talismanScroll);
        for (int i = 0; i < visible; i++) {
            TalismanRecipes.Def recipe = recipes.get(talismanScroll + i);
            int successes = TalismanRecipes.successCount(data, recipe.id());
            int required = TalismanRecipes.masterySuccesses(recipe);
            Component label = recipe.result().getHoverName().copy()
                    .append(": ")
                    .append(Integer.toString(successes))
                    .append(" / ")
                    .append(Integer.toString(required));
            drawFittedLabel(g, mc, label, x, y + i * 22, 168,
                    successes >= required ? 0xFF88CC : 0xFFE1A8);
        }
    }

    private void drawFittedLabel(GuiGraphics g, Minecraft mc, Component label,
                                 int x, int y, int maxWidth, int color) {
        int textWidth = Math.max(1, mc.font.width(label));
        float scale = Math.min(1.0F, maxWidth / (float) textWidth);
        g.pose().pushPose();
        g.pose().translate(x, y + (8.0F - 8.0F * scale) / 2.0F, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        g.drawString(mc.font, label, 0, 0, color, true);
        g.pose().popPose();
    }

    private int craftProficiency(
            com.example.immortal_cultivation_mod.attachment.ModAttachments.CultivationData data,
            String id) {
        return "corpse_control".equals(id)
                ? data.spellProficiency(ModSpells.KONGSHI_SHU)
                : data.methodProficiency("craft_" + id);
    }

    private Component proficiencyTier(int value) {
        String key;
        if (value >= 10_000) {
            key = "spell_proficiency_peak";
        } else if (value >= 1_000) {
            key = "spell_proficiency_perfected";
        } else if (value >= 500) {
            key = "spell_proficiency_great";
        } else if (value >= 100) {
            key = "spell_proficiency_minor";
        } else {
            key = "spell_proficiency_beginner";
        }
        return Component.translatable("screen." + ImmortalCultivationMod.MODID + "." + key);
    }

    private void updateQualityButtons() {
        for (QualityToggle toggle : qualityToggles) {
            boolean selected = toggle.quality().equals(ClientData.jindanQuality);
            toggle.button().setMessage(Component.literal(selected ? "[X] " : "[ ] ")
                    .append(Component.translatable(
                            "jindan_quality." + ImmortalCultivationMod.MODID + "." + toggle.quality())));
        }
    }

    private void updateSystemButtons() {
        for (SystemToggle toggle : systemToggles) {
            boolean selected = toggle.type().id().equals(selectedSystemId);
            toggle.button().setMessage(Component.literal(selected ? "[X] " : "[ ] ")
                    .append(Component.translatable(toggle.type().translationKey())));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (page == 6) {
            int max = Math.max(0, TalismanRecipes.all().size() - VISIBLE_TALISMAN_ROWS);
            int next = Math.max(0,
                    Math.min(max, talismanScroll - (int) Math.signum(scrollY)));
            if (next != talismanScroll) {
                talismanScroll = next;
                init();
                return true;
            }
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        if (page != 2) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        int max = Math.max(0, learnedMethods().size() - VISIBLE_METHOD_ROWS);
        int next = Math.max(0, Math.min(max, methodScroll - (int) Math.signum(scrollY)));
        if (next == methodScroll) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        methodScroll = next;
        init();
        return true;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void removed() {
        super.removed();
        if (!closeSyncSent && Minecraft.getInstance().getConnection() != null) {
            closeSyncSent = true;
            PacketDistributor.sendToServer(new ModPayloads.ServerboundCloseDebugStatEditorPayload());
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record ElementToggle(String element, Button button) {
    }

    private record GradeToggle(String grade, Button button) {
    }

    private record QualityToggle(String quality, Button button) {
    }

    private record SystemToggle(OuterSystemType type, Button button) {
    }

    static class DebugButton extends Button {
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
            int textWidth = Math.max(1, font.width(message));
            float scale = Math.min(1.0F, Math.max(0.1F, (width - 6) / (float) textWidth));
            float tx = x + (width - textWidth * scale) / 2.0F;
            float ty = y + (height - 8.0F * scale) / 2.0F;

            g.pose().pushPose();
            g.pose().translate(tx, ty, 0.0F);
            g.pose().scale(scale, scale, 1.0F);
            g.drawString(font, message, 0, 0, text, false);
            g.pose().popPose();
        }
    }
}
