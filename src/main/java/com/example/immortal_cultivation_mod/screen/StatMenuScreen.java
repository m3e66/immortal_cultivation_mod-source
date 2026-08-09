package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

public class StatMenuScreen extends Screen {
    private static final ResourceLocation JINDAN_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID,
                    "textures/gui/jindan_button.png");
    private static final ResourceLocation STAT_ICONS =
            elementTexture("stat_icons");
    private static final ResourceLocation PERSON =
            elementTexture("stat_person");
    private static final ResourceLocation TOP_TAB =
            elementTexture("stat_tab_top");
    private static final ResourceLocation BOTTOM_TAB =
            elementTexture("stat_tab_bottom");

    private static final int ICON_ATLAS_WIDTH = 360;
    private static final int ICON_SOURCE_SIZE = 72;
    private static final int PERSON_WIDTH = 305;
    private static final int PERSON_HEIGHT = 311;
    private static final int TOP_TAB_WIDTH = 70;
    private static final int TOP_TAB_HEIGHT = 160;
    private static final int BOTTOM_TAB_WIDTH = 70;
    private static final int BOTTOM_TAB_HEIGHT = 160;

    private static final int WINDOW_W = 430;
    private static final int WINDOW_H = 344;

    private static final int INK = 0xFF38352E;
    private static final int MUTED_INK = 0xFF756F63;
    private static final int VALUE_COLOR = 0xFF465A52;
    private static final int ACCENT_COLOR = 0xFF5F4A82;
    private static final int JADE = 0xFF315E50;
    private static final int GOLD = 0xFF9B7C43;
    private static final int PANEL = 0xB8F1ECE1;
    private static final int PANEL_ROW = 0x54E6E0D4;
    private static final int PANEL_BORDER = 0xFFAAA18F;

    private static final int PROFILE_X = 32;
    private static final int PROFILE_Y = 62;
    private static final int PROFILE_W = 121;
    private static final int PROFILE_H = 123;

    private static final int TRAITS_X = 32;
    private static final int TRAITS_Y = 190;
    private static final int TRAITS_W = 121;
    private static final int TRAITS_H = 115;
    private static final int TRAIT_ICON_X = 39;
    private static final int TRAIT_TEXT_X = 62;
    private static final int TRAIT_START_Y = 195;
    private static final int TRAIT_SPACING = 21;
    private static final int TRAIT_ICON_SIZE = 18;
    private static final int TRAIT_TEXT_WIDTH = 84;

    private static final int SKILL_PANEL_X = 163;
    private static final int SKILL_PANEL_Y = 62;
    private static final int SKILL_PANEL_W = 235;
    private static final int SKILL_PANEL_H = 154;
    private static final int SKILL_BUTTON_X = 171;
    private static final int SKILL_BUTTON_Y = 72;
    private static final int SKILL_BUTTON_SIZE = 18;
    private static final int SKILL_ROW_SPACING = 23;
    private static final int SKILL_TEXT_X = 196;
    private static final int SKILL_TEXT_WIDTH = 194;

    private static final int ROOT_X = 163;
    private static final int ROOT_Y = 224;
    private static final int ROOT_W = 117;
    private static final int ROOT_H = 36;
    private static final int AGE_X = 286;
    private static final int AGE_Y = 224;
    private static final int AGE_W = 112;
    private static final int AGE_H = 36;
    private static final int METHOD_X = 163;
    private static final int METHOD_Y = 266;
    private static final int METHOD_W = 235;
    private static final int METHOD_H = 35;

    private static final int PROGRESS_X = 163;
    private static final int PROGRESS_Y = 312;
    private static final int PROGRESS_W = 139;
    private static final int PROGRESS_H = 16;
    private static final int BREAKTHROUGH_X = 309;
    private static final int BREAKTHROUGH_Y = 310;
    private static final int BREAKTHROUGH_W = 89;
    private static final int BREAKTHROUGH_H = 20;

    private static final int TOP_TAB_X = -21;
    private static final int TOP_TAB_Y = 27;
    private static final int BOTTOM_TAB_X = -21;
    private static final int BOTTOM_TAB_Y = 81;
    private static final int TAB_DRAW_W = 25;
    private static final int TAB_DRAW_H = 52;

    private final List<AbstractButton> skillButtons =
            new ArrayList<>();
    private AbstractButton breakthroughBtn;
    private AbstractButton proficiencyBtn;
    private AbstractButton jindanBtn;

    public StatMenuScreen() {
        super(Component.translatable(
                "screen." + ImmortalCultivationMod.MODID + ".stat_menu"));
    }

    private static ResourceLocation elementTexture(String name) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID,
                "textures/gui/elements/" + name + ".png");
    }

    @Override
    protected void init() {
        skillButtons.clear();
        int x = left();
        int y = top();

        addSkillButton("hp", x + SKILL_BUTTON_X, y + SKILL_BUTTON_Y);
        addSkillButton(
                "qi",
                x + SKILL_BUTTON_X,
                y + SKILL_BUTTON_Y + SKILL_ROW_SPACING);
        addSkillButton(
                "energy",
                x + SKILL_BUTTON_X,
                y + SKILL_BUTTON_Y + SKILL_ROW_SPACING * 2);
        addSkillButton(
                "physical",
                x + SKILL_BUTTON_X,
                y + SKILL_BUTTON_Y + SKILL_ROW_SPACING * 3);
        addSkillButton(
                "magic",
                x + SKILL_BUTTON_X,
                y + SKILL_BUTTON_Y + SKILL_ROW_SPACING * 4);
        addSkillButton(
                "mental",
                x + SKILL_BUTTON_X,
                y + SKILL_BUTTON_Y + SKILL_ROW_SPACING * 5);

        proficiencyBtn = addRenderableWidget(new InvisibleButton(
                x + BOTTOM_TAB_X,
                y + BOTTOM_TAB_Y,
                TAB_DRAW_W,
                TAB_DRAW_H,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".proficiency_menu"),
                ignored -> minecraft.setScreen(
                        new ProficiencyMenuScreen())));

        breakthroughBtn = addRenderableWidget(
                new CultivationGuiButton(
                        x + BREAKTHROUGH_X,
                        y + BREAKTHROUGH_Y,
                        BREAKTHROUGH_W,
                        BREAKTHROUGH_H,
                        Component.translatable(
                                "screen." + ImmortalCultivationMod.MODID
                                        + ".breakthrough_button"),
                        () -> PacketDistributor.sendToServer(
                                new ModPayloads
                                        .ServerboundRequestBreakthroughPayload())));

        jindanBtn = addRenderableWidget(new TexturedIconButton(
                x + PROFILE_X + PROFILE_W - 27,
                y + PROFILE_Y + 5,
                22,
                22,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".enter_jindan"),
                ignored -> {
                    PacketDistributor.sendToServer(
                            new ModPayloads
                                    .ServerboundEnterJindanPayload());
                    minecraft.setScreen(null);
                },
                JINDAN_BUTTON_TEXTURE));
    }

    private int left() {
        return (width - WINDOW_W) / 2;
    }

    private int top() {
        return (height - WINDOW_H) / 2;
    }

    private void addSkillButton(String stat, int x, int y) {
        AbstractButton button = new CultivationGuiButton(
                x,
                y,
                SKILL_BUTTON_SIZE,
                SKILL_BUTTON_SIZE,
                Component.literal("+"),
                () -> PacketDistributor.sendToServer(
                        new ModPayloads
                                .ServerboundSpendSkillPointPayload(stat)));
        skillButtons.add(button);
        addRenderableWidget(button);
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick) {
        int x = left();
        int y = top();
        graphics.fill(0, 0, width, height, 0xA0101716);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        CultivationGuiTextures.drawLandscape(
                graphics, x, y, WINDOW_W, WINDOW_H);
        renderPanels(graphics, x, y);
        renderTabs(graphics, x, y, mouseX, mouseY);

        drawCenteredScaled(
                graphics,
                title,
                x + WINDOW_W / 2,
                y + 18,
                190,
                INK,
                false);

        Minecraft minecraft = Minecraft.getInstance();
        var data = ClientData.cultivationData;
        if (data == null) {
            setWidgetsVisible(false);
            drawCenteredScaled(
                    graphics,
                    Component.literal("No data"),
                    x + WINDOW_W / 2,
                    y + WINDOW_H / 2,
                    120,
                    0xFFAA3333,
                    false);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        setWidgetsVisible(true);
        String level = CultivationLevels.isMortal(
                data.cultivationLevel())
                ? CultivationLevels.REALM_MORTAL
                : data.cultivationLevel();
        var levelDef = CultivationLevels.getLevelDef(level);
        int maxHp = Math.max(
                1,
                levelDef.maxHp() + data.maxHpBonus());
        int maxQi = levelDef.maxQi() + data.maxQiBonus();
        boolean jindanOrHigher =
                CultivationLevels.getStageIndex(level)
                        >= CultivationLevels.getStageIndex(
                                CultivationLevels.REALM_JINDAN
                                        + CultivationLevels.STAGE_EARLY);

        renderProfile(
                graphics, data, level, jindanOrHigher, x, y);
        renderTraits(graphics, data, x, y);
        renderSkills(graphics, data, maxHp, maxQi, x, y);
        renderCultivationDetails(graphics, data, level, x, y);

        String nextStage = CultivationLevels.getNextStage(level);
        long progressNeeded =
                CultivationLevels.getTotalQiNeeded(level);
        boolean fullProgress =
                data.cultivationProgress() >= progressNeeded;
        boolean needsPill =
                CultivationLevels.needsBreakthroughPill(level);
        boolean canBreakthrough = nextStage != null
                && fullProgress
                && (!needsPill || hasBreakthroughPill(minecraft));

        for (AbstractButton button : skillButtons) {
            button.active = data.skillPoints() > 0;
            button.visible = true;
        }
        breakthroughBtn.active = canBreakthrough;
        breakthroughBtn.visible = nextStage != null;
        jindanBtn.active = jindanOrHigher;
        jindanBtn.visible = jindanOrHigher;

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderPanels(
            GuiGraphics graphics,
            int x,
            int y) {
        CultivationGuiTextures.drawInnerBox(
                graphics,
                x + TRAITS_X,
                y + TRAITS_Y,
                TRAITS_W,
                TRAITS_H);
        CultivationGuiTextures.drawInnerBox(
                graphics,
                x + SKILL_PANEL_X,
                y + SKILL_PANEL_Y,
                SKILL_PANEL_W,
                SKILL_PANEL_H);
        CultivationGuiTextures.drawInnerBox(
                graphics,
                x + ROOT_X,
                y + ROOT_Y,
                ROOT_W,
                ROOT_H);
        CultivationGuiTextures.drawInnerBox(
                graphics,
                x + AGE_X,
                y + AGE_Y,
                AGE_W,
                AGE_H);
        CultivationGuiTextures.drawInnerBox(
                graphics,
                x + METHOD_X,
                y + METHOD_Y,
                METHOD_W,
                METHOD_H);
    }

    private void renderTabs(
            GuiGraphics graphics,
            int x,
            int y,
            int mouseX,
            int mouseY) {
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(
                TOP_TAB,
                x + TOP_TAB_X,
                y + TOP_TAB_Y,
                TAB_DRAW_W,
                TAB_DRAW_H,
                0.0F,
                0.0F,
                TOP_TAB_WIDTH,
                TOP_TAB_HEIGHT,
                TOP_TAB_WIDTH,
                TOP_TAB_HEIGHT);
        graphics.blit(
                BOTTOM_TAB,
                x + BOTTOM_TAB_X,
                y + BOTTOM_TAB_Y,
                TAB_DRAW_W,
                TAB_DRAW_H,
                0.0F,
                0.0F,
                BOTTOM_TAB_WIDTH,
                BOTTOM_TAB_HEIGHT,
                BOTTOM_TAB_WIDTH,
                BOTTOM_TAB_HEIGHT);
        if (isInside(
                mouseX,
                mouseY,
                x + BOTTOM_TAB_X,
                y + BOTTOM_TAB_Y,
                TAB_DRAW_W,
                TAB_DRAW_H)) {
            graphics.fill(
                    x + BOTTOM_TAB_X,
                    y + BOTTOM_TAB_Y,
                    x + BOTTOM_TAB_X + TAB_DRAW_W,
                    y + BOTTOM_TAB_Y + TAB_DRAW_H,
                    0x22FFFFFF);
        }
    }

    private void renderProfile(
            GuiGraphics graphics,
            ModAttachments.CultivationData data,
            String level,
            boolean jindanOrHigher,
            int x,
            int y) {
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(
                PERSON,
                x + PROFILE_X,
                y + PROFILE_Y,
                PROFILE_W,
                PROFILE_H,
                0.0F,
                0.0F,
                PERSON_WIDTH,
                PERSON_HEIGHT,
                PERSON_WIDTH,
                PERSON_HEIGHT);
        drawCenteredScaled(
                graphics,
                Component.literal(level),
                x + PROFILE_X + PROFILE_W / 2,
                y + PROFILE_Y + 5,
                jindanOrHigher ? PROFILE_W - 38 : PROFILE_W - 12,
                VALUE_COLOR,
                false);

        drawCenteredScaled(
                graphics,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".player_identity",
                        Component.translatable(
                                "screen." + ImmortalCultivationMod.MODID
                                        + ".npc.identity."
                                        + ClientData.playerIdentity)),
                x + PROFILE_X + PROFILE_W / 2,
                y + PROFILE_Y + 91,
                PROFILE_W - 10,
                INK,
                false);
        if (jindanOrHigher) {
            drawCenteredScaled(
                    graphics,
                    Component.translatable(
                            "screen." + ImmortalCultivationMod.MODID
                                    + ".jindan_quality",
                            Component.translatable(
                                    "jindan_quality."
                                            + ImmortalCultivationMod.MODID
                                            + "."
                                            + ClientData.jindanQuality)),
                    x + PROFILE_X + PROFILE_W / 2,
                    y + PROFILE_Y + 106,
                    PROFILE_W - 10,
                    GOLD,
                    false);
        }
    }

    private void renderTraits(
            GuiGraphics graphics,
            ModAttachments.CultivationData data,
            int x,
            int y) {
        renderTrait(
                graphics,
                0,
                labelled(
                        "body_type",
                        Component.literal(data.bodyType())),
                x,
                y,
                VALUE_COLOR);
        renderTrait(
                graphics,
                1,
                labelled(
                        "moral",
                        Component.literal(
                                Integer.toString(data.moral()))),
                x,
                y,
                VALUE_COLOR);
        renderTrait(
                graphics,
                2,
                labelled(
                        "luck",
                        Component.literal(
                                Integer.toString(data.luck()))),
                x,
                y,
                VALUE_COLOR);
        renderTrait(
                graphics,
                3,
                labelled(
                        "soul",
                        Component.literal(
                                Integer.toString(data.soul()))),
                x,
                y,
                ACCENT_COLOR);
        renderTrait(
                graphics,
                4,
                labelled(
                        "thoughts",
                        Component.literal(
                                Integer.toString(data.thoughts()))),
                x,
                y,
                VALUE_COLOR);
    }

    private void renderTrait(
            GuiGraphics graphics,
            int index,
            Component text,
            int x,
            int y,
            int color) {
        int rowY = y + TRAIT_START_Y + index * TRAIT_SPACING;
        if (index > 0) {
            graphics.fill(
                    x + TRAITS_X + 5,
                    rowY - 4,
                    x + TRAITS_X + TRAITS_W - 5,
                    rowY - 3,
                    0x448E8472);
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(
                STAT_ICONS,
                x + TRAIT_ICON_X,
                rowY,
                TRAIT_ICON_SIZE,
                TRAIT_ICON_SIZE,
                index * (float) ICON_SOURCE_SIZE,
                0.0F,
                ICON_SOURCE_SIZE,
                ICON_SOURCE_SIZE,
                ICON_ATLAS_WIDTH,
                ICON_SOURCE_SIZE);
        drawScaled(
                graphics,
                text,
                x + TRAIT_TEXT_X,
                rowY + 5,
                TRAIT_TEXT_WIDTH,
                color,
                false);
    }

    private void renderSkills(
            GuiGraphics graphics,
            ModAttachments.CultivationData data,
            int maxHp,
            int maxQi,
            int x,
            int y) {
        Component points = Component.translatable(
                "screen." + ImmortalCultivationMod.MODID
                        + ".skill_points",
                data.skillPoints());
        graphics.fill(
                x + 300,
                y + 42,
                x + 399,
                y + 58,
                0xCC315E50);
        graphics.renderOutline(
                x + 300, y + 42, 99, 16, 0xFF203F36);
        drawCenteredScaled(
                graphics,
                points,
                x + 349,
                y + 46,
                91,
                0xFFF8F3DF,
                true);

        for (int row = 0; row < 6; row++) {
            int rowY = y + SKILL_BUTTON_Y
                    + row * SKILL_ROW_SPACING;
            CultivationGuiTextures.drawInnerBox(
                    graphics,
                    x + SKILL_PANEL_X + 5,
                    rowY - 2,
                    SKILL_PANEL_W - 10,
                    SKILL_BUTTON_SIZE + 4);
        }

        drawSkillLine(
                graphics,
                data,
                x,
                y,
                0,
                "hp",
                4,
                maxHp,
                data.maxHpBonus() / 4,
                INK);
        drawSkillLine(
                graphics,
                data,
                x,
                y,
                1,
                "qi",
                40,
                maxQi,
                data.maxQiBonus() / 40,
                0xFF355F6C);
        drawSkillLine(
                graphics,
                data,
                x,
                y,
                2,
                "energy",
                40,
                data.maxEnergyBonus(),
                data.maxEnergyBonus() / 40,
                0xFF735F2D);
        drawSkillLine(
                graphics,
                data,
                x,
                y,
                3,
                "physical",
                1,
                data.physicalAttack(),
                data.physicalAttack(),
                0xFF794D39);
        drawSkillLine(
                graphics,
                data,
                x,
                y,
                4,
                "magic",
                1,
                data.magicAttack(),
                data.magicAttack(),
                ACCENT_COLOR);
        drawSkillLine(
                graphics,
                data,
                x,
                y,
                5,
                "mental",
                1,
                data.mentalAttack(),
                data.mentalAttack(),
                0xFF3B6A57);
    }

    private void renderCultivationDetails(
            GuiGraphics graphics,
            ModAttachments.CultivationData data,
            String level,
            int x,
            int y) {
        drawCenteredScaled(
                graphics,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".spirit_roots"),
                x + ROOT_X + ROOT_W / 2,
                y + ROOT_Y + 5,
                ROOT_W - 10,
                MUTED_INK,
                false);
        drawCenteredScaled(
                graphics,
                Component.literal(SpiritRoots.format(
                        data.spiritRoots(),
                        data.spiritRootGrade())),
                x + ROOT_X + ROOT_W / 2,
                y + ROOT_Y + 19,
                ROOT_W - 10,
                VALUE_COLOR,
                false);

        int maxAge = Math.max(
                1,
                CultivationLevels.getLevelDef(level).maxAge()
                        - data.agePenalty());
        drawCenteredScaled(
                graphics,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".max_age"),
                x + AGE_X + AGE_W / 2,
                y + AGE_Y + 5,
                AGE_W - 10,
                MUTED_INK,
                false);
        drawCenteredScaled(
                graphics,
                Component.literal(Integer.toString(maxAge)),
                x + AGE_X + AGE_W / 2,
                y + AGE_Y + 19,
                AGE_W - 10,
                VALUE_COLOR,
                false);

        drawCenteredScaled(
                graphics,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".cultivation_method"),
                x + METHOD_X + METHOD_W / 2,
                y + METHOD_Y + 5,
                METHOD_W - 12,
                MUTED_INK,
                false);
        var method = CultivationMethods.get(
                data.activeCultivationMethod());
        Component methodName = method == null
                ? Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".none")
                : Component.translatable(method.nameKey());
        MutableComponent methodLine = methodName.copy();
        if (method != null) {
            methodLine = methodLine
                    .append(" / ")
                    .append(CultivationMethods.methodProficiencyLevel(
                            data,
                            data.activeCultivationMethod()));
        }
        drawCenteredScaled(
                graphics,
                methodLine,
                x + METHOD_X + METHOD_W / 2,
                y + METHOD_Y + 19,
                METHOD_W - 12,
                VALUE_COLOR,
                false);

        long progressNeeded =
                CultivationLevels.getTotalQiNeeded(level);
        float ratio = progressNeeded <= 0L
                ? 1.0F
                : Math.max(
                        0.0F,
                        Math.min(
                                1.0F,
                                data.cultivationProgress()
                                        / (float) progressNeeded));
        drawScaled(
                graphics,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".cultivation_progress"),
                x + PROGRESS_X,
                y + PROGRESS_Y - 11,
                PROGRESS_W,
                MUTED_INK,
                false);
        CultivationGuiTextures.drawProgressFrame(
                graphics,
                x + PROGRESS_X,
                y + PROGRESS_Y,
                PROGRESS_W,
                PROGRESS_H);
        int progressInsetX = 5;
        int progressInsetY = 4;
        int progressFillWidth =
                PROGRESS_W - progressInsetX * 2;
        int progressFillHeight =
                PROGRESS_H - progressInsetY * 2;
        graphics.fill(
                x + PROGRESS_X + progressInsetX,
                y + PROGRESS_Y + progressInsetY,
                x + PROGRESS_X + progressInsetX
                        + progressFillWidth,
                y + PROGRESS_Y + progressInsetY
                        + progressFillHeight,
                0xFF343C39);
        graphics.fill(
                x + PROGRESS_X + progressInsetX,
                y + PROGRESS_Y + progressInsetY,
                x + PROGRESS_X + progressInsetX
                        + Math.round(progressFillWidth * ratio),
                y + PROGRESS_Y + progressInsetY
                        + progressFillHeight,
                JADE);
        drawCenteredScaled(
                graphics,
                Component.literal(
                        data.cultivationProgress()
                                + "/"
                                + progressNeeded),
                x + PROGRESS_X + PROGRESS_W / 2,
                y + PROGRESS_Y + 4,
                PROGRESS_W - 8,
                0xFFF8F3DF,
                true);
    }

    private Component labelled(String key, Component value) {
        return Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + "." + key)
                .copy()
                .append(": ")
                .append(value);
    }

    private void drawSkillLine(
            GuiGraphics graphics,
            ModAttachments.CultivationData data,
            int x,
            int y,
            int row,
            String key,
            int increase,
            int total,
            int invested,
            int color) {
        Component label = Component.translatable(
                "screen." + ImmortalCultivationMod.MODID
                        + ".skill_" + key,
                increase,
                total,
                invested);
        drawScaled(
                graphics,
                label,
                x + SKILL_TEXT_X,
                y + SKILL_BUTTON_Y
                        + row * SKILL_ROW_SPACING
                        + 5,
                SKILL_TEXT_WIDTH,
                color,
                false);
    }

    private void drawPanel(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height) {
        graphics.fill(x, y, x + width, y + height, PANEL);
        graphics.renderOutline(
                x, y, width, height, PANEL_BORDER);
    }

    private void drawCenteredScaled(
            GuiGraphics graphics,
            Component text,
            int centerX,
            int y,
            int maxWidth,
            int color,
            boolean shadow) {
        int textWidth = font.width(text);
        if (textWidth <= maxWidth) {
            graphics.drawString(
                    font,
                    text,
                    centerX - textWidth / 2,
                    y,
                    color,
                    shadow);
            return;
        }
        float scale = Math.max(
                0.55F,
                maxWidth / (float) textWidth);
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(
                font,
                text,
                Math.round(centerX / scale - textWidth / 2.0F),
                Math.round(y / scale),
                color,
                shadow);
        graphics.pose().popPose();
    }

    private void drawScaled(
            GuiGraphics graphics,
            Component text,
            int x,
            int y,
            int maxWidth,
            int color,
            boolean shadow) {
        int textWidth = font.width(text);
        if (textWidth <= maxWidth) {
            graphics.drawString(
                    font, text, x, y, color, shadow);
            return;
        }
        float scale = Math.max(
                0.55F,
                maxWidth / (float) textWidth);
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(
                font,
                text,
                Math.round(x / scale),
                Math.round(y / scale),
                color,
                shadow);
        graphics.pose().popPose();
    }

    private void setWidgetsVisible(boolean visible) {
        skillButtons.forEach(button -> button.visible = visible);
        if (breakthroughBtn != null) {
            breakthroughBtn.visible = visible;
        }
        if (proficiencyBtn != null) {
            proficiencyBtn.visible = visible;
        }
        if (jindanBtn != null) {
            jindanBtn.visible = visible;
        }
    }

    private boolean hasBreakthroughPill(
            Minecraft minecraft) {
        if (minecraft.player == null) {
            return false;
        }
        for (var stack : minecraft.player.getInventory().items) {
            if (!stack.isEmpty()
                    && stack.getItem()
                            == com.example.immortal_cultivation_mod.item
                                    .ModItems.BREAKTHROUGH_PILL.get()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInside(
            double mouseX,
            double mouseY,
            int x,
            int y,
            int width,
            int height) {
        return mouseX >= x
                && mouseX < x + width
                && mouseY >= y
                && mouseY < y + height;
    }

    @Override
    public void renderBackground(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class InvisibleButton extends Button {
        private InvisibleButton(
                int x,
                int y,
                int width,
                int height,
                Component message,
                OnPress onPress) {
            super(
                    x,
                    y,
                    width,
                    height,
                    message,
                    onPress,
                    DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(
                GuiGraphics graphics,
                int mouseX,
                int mouseY,
                float partialTick) {
        }
    }

    private static final class TexturedIconButton extends Button {
        private final ResourceLocation texture;

        private TexturedIconButton(
                int x,
                int y,
                int width,
                int height,
                Component message,
                OnPress onPress,
                ResourceLocation texture) {
            super(
                    x,
                    y,
                    width,
                    height,
                    message,
                    onPress,
                    DEFAULT_NARRATION);
            this.texture = texture;
        }

        @Override
        protected void renderWidget(
                GuiGraphics graphics,
                int mouseX,
                int mouseY,
                float partialTick) {
            int color = isHoveredOrFocused()
                    ? 0xFFFFFFFF
                    : 0xFFEFE6C8;
            graphics.setColor(
                    ((color >> 16) & 0xFF) / 255.0F,
                    ((color >> 8) & 0xFF) / 255.0F,
                    (color & 0xFF) / 255.0F,
                    1.0F);
            graphics.blit(
                    texture,
                    getX(),
                    getY(),
                    getWidth(),
                    getHeight(),
                    0.0F,
                    0.0F,
                    138,
                    139,
                    138,
                    139);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
