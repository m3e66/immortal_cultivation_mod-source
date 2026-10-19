package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.attachment.SpiritRoots;
import com.example.immortal_cultivation_mod.attribute.ModAttributes;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.client.ShentongClient;
import com.example.immortal_cultivation_mod.client.DaoGuidanceClient;
import com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent;
import com.example.immortal_cultivation_mod.network.DaoGuidancePayloads;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import com.example.immortal_cultivation_mod.spell.UndeadControl;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

public class StatMenuScreen extends FittedMenuScreen {
    private static final ResourceLocation JINDAN_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID,
                    "textures/gui/jindan_button.png");
    private static final ResourceLocation VOID_BREAK_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID,
                    "textures/gui/break_void.png");
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
    private AbstractButton shentongBtn;
    private AbstractButton voidBreakBtn;
    private AbstractButton rootsBtn;

    public StatMenuScreen() {
        super(Component.translatable(
                "screen." + ImmortalCultivationMod.MODID + ".stat_menu"), WINDOW_W + 58, WINDOW_H + 16);
    }

    private static ResourceLocation elementTexture(String name) {
        return ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID,
                "textures/gui/elements/" + name + ".png");
    }

    @Override
    protected void initMenu() {
        skillButtons.clear();
        int x = left();
        int y = top();

        rootsBtn = addRenderableWidget(new InvisibleButton(
                x + ROOT_X, y + ROOT_Y, ROOT_W, ROOT_H,
                Component.translatable("screen.immortal_cultivation_mod.root_details"),
                ignored -> minecraft.setScreen(new SpiritRootDetailsScreen(this))));
        addRenderableWidget(new InvisibleButton(
                x + TRAITS_X, y + TRAIT_START_Y, TRAITS_W, TRAIT_SPACING,
                Component.translatable("screen.immortal_cultivation_mod.body_details"),
                ignored -> minecraft.setScreen(new BodyTypeDetailsScreen(this))));
        addRenderableWidget(new CultivationGuiButton(x+TRAITS_X,y+310,TRAITS_W,20,
                Component.translatable("toxicity.title"),()->minecraft.setScreen(new PillToxicityScreen(this))));

        PacketDistributor.sendToServer(
                new DaoGuidancePayloads.ServerboundDaoGuidanceActionPayload(
                        DaoGuidanceContent.OPEN_STATS));

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

        shentongBtn = addRenderableWidget(new TexturedIconButton(
                x + PROFILE_X + 5, y + PROFILE_Y + 5, 22, 22,
                Component.translatable("screen.immortal_cultivation_mod.shentong_title"),
                ignored -> minecraft.setScreen(new ShentongDetailsScreen(this)),
                ShentongDetailsScreen.ICON,ShentongDetailsScreen.ICON_SIZE,ShentongDetailsScreen.ICON_SIZE));
        shentongBtn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("screen.immortal_cultivation_mod.shentong_open")));
        shentongBtn.active = shentongBtn.visible = ShentongClient.hasShentong();
        ShentongClient.request();

        voidBreakBtn = addRenderableWidget(new TexturedIconButton(
                x + PROFILE_X + PROFILE_W - 27,
                y + PROFILE_Y + 29,
                22,
                22,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".open_cosmic_void_portal"),
                ignored -> {
                    PacketDistributor.sendToServer(
                            new ModPayloads
                                    .ServerboundOpenCosmicVoidPortalPayload());
                    minecraft.setScreen(null);
                },
                VOID_BREAK_BUTTON_TEXTURE,
                64,
                64));
    }

    private int left() {
        return (width - WINDOW_W) / 2;
    }

    private int shentongRefreshTicks;
    @Override public void tick() {
        super.tick();
        if (++shentongRefreshTicks >= 20) { shentongRefreshTicks=0; ShentongClient.request(); }
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
    protected void renderMenu(
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
        renderGuidanceHighlights(graphics, x, y);
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
                    Component.translatable("screen.immortal_cultivation_mod.no_data"),
                    x + WINDOW_W / 2,
                    y + WINDOW_H / 2,
                    120,
                    0xFFAA3333,
                    false);
            super.renderMenu(graphics, mouseX, mouseY, partialTick);
            return;
        }

        setWidgetsVisible(true);
        String level = CultivationLevels.isMortal(
                data.cultivationLevel())
                ? CultivationLevels.REALM_MORTAL
                : data.cultivationLevel();
        var levelDef = CultivationLevels.getLevelDef(level);
        boolean livingCorpse = ClientData.livingCorpse()
                && minecraft != null
                && minecraft.player != null;
        long zombieGrudge = livingCorpse
                ? Math.max(0L, Math.round(
                        minecraft.player.getAttributeValue(ModAttributes.GRUDGE)))
                : 0L;
        Component displayedLevel = livingCorpse
                ? Component.translatable(
                        "zombie_rank." + ImmortalCultivationMod.MODID + "."
                                + UndeadControl.rankIdForGrudge(zombieGrudge))
                : CultivationLevels.displayName(level);
        int maxHp = minecraft.player == null
                ? Math.max(1, (int)Math.round((levelDef.maxHp() + data.maxHpBonus())
                        * com.example.immortal_cultivation_mod.attachment.BodyTypeEffects.healthMultiplier(data.bodyType())))
                : Math.max(1, Math.round(minecraft.player.getMaxHealth()));
        int maxQi = data.maxQi();
        boolean jindanOrHigher =
                CultivationLevels.getStageIndex(level)
                        >= CultivationLevels.getStageIndex(
                                CultivationLevels.REALM_JINDAN
                                        + CultivationLevels.STAGE_EARLY);
        boolean huashenOrHigher =
                CultivationLevels.getStageIndex(level)
                        >= CultivationLevels.getStageIndex(
                                CultivationLevels.REALM_HUASHEN
                                        + CultivationLevels.STAGE_EARLY);

        renderProfile(
                graphics, data, displayedLevel, jindanOrHigher, x, y);
        renderTraits(graphics, data, x, y);
        renderSkills(graphics, data, maxHp, maxQi, x, y);
        renderCultivationDetails(
                graphics, data, level, livingCorpse, zombieGrudge, x, y);

        String nextStage = CultivationLevels.getNextStage(level);
        long progressNeeded =
                CultivationLevels.getTotalQiNeeded(level);
        boolean fullProgress =
                !livingCorpse
                        && data.cultivationProgress() >= progressNeeded;
        boolean needsPill =
                CultivationLevels.needsBreakthroughPill(level);
        boolean canBreakthrough = nextStage != null
                && !livingCorpse
                && fullProgress
                && (!needsPill || hasBreakthroughPill(minecraft));

        for (AbstractButton button : skillButtons) {
            button.active = data.skillPoints() > 0;
            button.visible = true;
        }
        breakthroughBtn.active = canBreakthrough;
        breakthroughBtn.visible = nextStage != null && !livingCorpse;
        jindanBtn.active = jindanOrHigher;
        jindanBtn.visible = jindanOrHigher;
        voidBreakBtn.active = huashenOrHigher;
        voidBreakBtn.visible = huashenOrHigher;
        shentongBtn.active = shentongBtn.visible = ShentongClient.hasShentong();

        super.renderMenu(graphics, mouseX, mouseY, partialTick);
        if (mouseX >= x + TRAITS_X && mouseX < x + TRAITS_X + TRAITS_W
                && mouseY >= y + TRAIT_START_Y && mouseY < y + TRAIT_START_Y + TRAIT_SPACING) {
            var lines = new java.util.ArrayList<net.minecraft.util.FormattedCharSequence>();
            lines.add(com.example.immortal_cultivation_mod.attachment.BodyTypes.rarityName(data.bodyType()).getVisualOrderText());
            lines.addAll(minecraft.font.split(com.example.immortal_cultivation_mod.attachment.BodyTypes.description(data.bodyType()), 240));
            graphics.renderTooltip(minecraft.font, lines, mouseX, mouseY);
        }
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
        // The upper tab is decorative; only the lower tab opens proficiency.
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

    private void renderGuidanceHighlights(
            GuiGraphics graphics, int x, int y) {
        int pulse = 150 + (int) (Math.sin(System.currentTimeMillis() / 180.0D) * 60.0D);
        int color = (Math.max(70, Math.min(230, pulse)) << 24) | 0xE3B95F;
        if (DaoGuidanceClient.isCurrentAction(DaoGuidanceContent.OPEN_STATS)) {
            graphics.renderOutline(x + PROFILE_X - 2, y + PROFILE_Y - 2,
                    PROFILE_W + 4, PROFILE_H + 4, color);
            graphics.renderOutline(x + ROOT_X - 2, y + ROOT_Y - 2,
                    ROOT_W + 4, ROOT_H + 4, color);
            graphics.renderOutline(x + AGE_X - 2, y + AGE_Y - 2,
                    AGE_W + 4, AGE_H + 4, color);
        }
        if (DaoGuidanceClient.isCurrentAction(DaoGuidanceContent.ACTIVATE_METHOD)) {
            graphics.renderOutline(x + METHOD_X - 2, y + METHOD_Y - 2,
                    METHOD_W + 4, METHOD_H + 4, color);
        }
        if (DaoGuidanceClient.isCurrentAction(DaoGuidanceContent.BREAKTHROUGH_ATTEMPT)) {
            graphics.renderOutline(x + BREAKTHROUGH_X - 3,
                    y + BREAKTHROUGH_Y - 3,
                    BREAKTHROUGH_W + 6, BREAKTHROUGH_H + 6, color);
        }
    }

    private void renderProfile(
            GuiGraphics graphics,
            ModAttachments.CultivationData data,
            Component level,
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
        int levelLeftInset = ShentongClient.hasShentong() ? 30 : 6;
        int levelRightInset = jindanOrHigher ? 30 : 6;
        int levelTextWidth = PROFILE_W - levelLeftInset - levelRightInset;
        int levelTextCenter = x + PROFILE_X + levelLeftInset + levelTextWidth / 2;
        drawCenteredScaled(
                graphics,
                level,
                levelTextCenter,
                y + PROFILE_Y + 5,
                levelTextWidth,
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
        if (jindanOrHigher || CultivationLevels.REALM_HALF_JINDAN.equals(data.cultivationLevel())) {
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
                        com.example.immortal_cultivation_mod.attachment.BodyTypes.displayName(data.bodyType())),
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
                        ClientData.debugEyeEnabled() ? Component.literal(data.luck() + " · ")
                                .append(com.example.immortal_cultivation_mod.fate.LuckSystem.state(data.luck()))
                                : com.example.immortal_cultivation_mod.fate.LuckSystem.ownDisplay(
                                        minecraft == null ? null : minecraft.player, data.luck())),
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
        renderSkillPointsBadge(graphics, data.skillPoints(), x, y);

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
                10,
                data.maxThoughts(),
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
            boolean livingCorpse,
            long zombieGrudge,
            int x,
            int y) {
        if (rootsBtn.isHoveredOrFocused()) {
            graphics.fill(x + ROOT_X + 3, y + ROOT_Y + 3,
                    x + ROOT_X + ROOT_W - 3, y + ROOT_Y + ROOT_H - 3, 0x225CA994);
        }
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
                SpiritRoots.displayName(data.spiritRoots(), data.spiritRootGrade()),
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

        long progressNeeded = CultivationLevels.getTotalQiNeeded(level);
        float ratio;
        Component progressLabel;
        Component progressValue;
        if (livingCorpse) {
            long currentThreshold = UndeadControl.currentRankThreshold(zombieGrudge);
            long nextThreshold = UndeadControl.nextRankThreshold(zombieGrudge);
            ratio = nextThreshold < 0L
                    ? 1.0F
                    : Math.max(
                            0.0F,
                            Math.min(
                                    1.0F,
                                    (zombieGrudge - currentThreshold)
                                            / (float) Math.max(
                                                    1L,
                                                    nextThreshold - currentThreshold)));
            progressLabel = Component.translatable(
                    "screen." + ImmortalCultivationMod.MODID
                            + ".grudge_progress");
            progressValue = nextThreshold < 0L
                    ? Component.translatable(
                            "screen." + ImmortalCultivationMod.MODID
                                    + ".zombie_progress_value_max",
                            zombieGrudge)
                    : Component.literal(zombieGrudge + "/" + nextThreshold);
        } else {
            ratio = progressNeeded <= 0L
                    ? 1.0F
                    : Math.max(
                            0.0F,
                            Math.min(
                                    1.0F,
                                    data.cultivationProgress()
                                            / (float) progressNeeded));
            progressLabel = Component.translatable(
                    "screen." + ImmortalCultivationMod.MODID
                            + ".cultivation_progress");
            progressValue = Component.literal(
                    data.cultivationProgress() + "/" + progressNeeded);
        }
        drawScaled(
                graphics,
                progressLabel,
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
                progressValue,
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

    protected void renderSkillPointsBadge(GuiGraphics graphics, int points, int x, int y) {
        int left = x + 287, top = y + 39, width = 112, height = 20;
        Component value = Component.literal(Integer.toString(points));
        int valueWidth = Math.clamp(font.width(value) + 12, 28, 49);
        int valueX = left + width - valueWidth - 4;
        boolean available = points > 0;

        graphics.fill(left + 1, top + 2, left + width + 1, top + height + 2, 0x2038352E);
        graphics.fill(left, top, left + width, top + height, 0xFF99886A);
        graphics.fillGradient(left + 1, top + 1, left + width - 1, top + height - 1,
                0xFFF4EFDF, 0xFFE2D9C6);
        graphics.hLine(left + 2, left + width - 3, top + 1, 0xFFFFF9E9);
        graphics.hLine(left + 2, left + width - 3, top + height - 2, 0xFFCEC1A7);
        graphics.fill(left, top, left + 5, top + 1, GOLD);
        graphics.fill(left, top, left + 1, top + 5, GOLD);
        graphics.fill(left + width - 5, top + height - 1, left + width, top + height, GOLD);
        graphics.fill(left + width - 1, top + height - 5, left + width, top + height, GOLD);

        // A small gilded seal ties the counter to the surrounding manuscript frame.
        graphics.fill(left + 8, top + 7, left + 9, top + 12, GOLD);
        graphics.fill(left + 6, top + 9, left + 11, top + 10, GOLD);
        graphics.fill(left + 7, top + 8, left + 10, top + 11, GOLD);
        graphics.fill(left + 8, top + 9, left + 9, top + 10, 0xFFF9F0D3);
        drawScaled(graphics, Component.translatable("screen." + ImmortalCultivationMod.MODID
                + ".skill_points_label"), left + 17, top + 6, valueX - left - 23, INK, false);

        graphics.fill(valueX, top + 3, left + width - 4, top + height - 3,
                available ? 0xFF9B8352 : 0xFF7D8A79);
        graphics.fillGradient(valueX + 1, top + 4, left + width - 5, top + height - 4,
                available ? 0xFF426557 : 0xFF6A7A6F, available ? 0xFF2F4E42 : 0xFF53665A);
        drawCenteredScaled(graphics, value, valueX + valueWidth / 2, top + 6,
                valueWidth - 8, available ? 0xFFF9E3A8 : 0xFFECEADB, false);
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
        if (rootsBtn != null) rootsBtn.visible = visible;
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
        if (voidBreakBtn != null) {
            voidBreakBtn.visible = visible;
        }
        if (shentongBtn != null) shentongBtn.visible = visible && ShentongClient.hasShentong();
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
        private final int textureWidth;
        private final int textureHeight;

        private TexturedIconButton(
                int x,
                int y,
                int width,
                int height,
                Component message,
                OnPress onPress,
                ResourceLocation texture) {
            this(x, y, width, height, message, onPress,
                    texture, 138, 139);
        }

        private TexturedIconButton(
                int x,
                int y,
                int width,
                int height,
                Component message,
                OnPress onPress,
                ResourceLocation texture,
                int textureWidth,
                int textureHeight) {
            super(
                    x,
                    y,
                    width,
                    height,
                    message,
                    onPress,
                    DEFAULT_NARRATION);
            this.texture = texture;
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
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
                    textureWidth,
                    textureHeight,
                    textureWidth,
                    textureHeight);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
