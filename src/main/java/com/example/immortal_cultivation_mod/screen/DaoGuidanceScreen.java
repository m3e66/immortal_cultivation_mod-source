package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.client.DaoGuidanceClient;
import com.example.immortal_cultivation_mod.client.ModKeyMappings;
import com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent;
import com.example.immortal_cultivation_mod.network.DaoGuidancePayloads;
import com.example.immortal_cultivation_mod.item.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

/** Interactive chapter browser for 引道. */
public final class DaoGuidanceScreen extends Screen {
    private static final int WINDOW_W = 520;
    private static final int WINDOW_H = 340;
    private static final int LIST_X = 22;
    private static final int LIST_Y = 54;
    private static final int LIST_W = 188;
    private static final int LIST_H = 238;
    private static final int DETAIL_X = 220;
    private static final int DETAIL_Y = 54;
    private static final int DETAIL_W = 278;
    private static final int DETAIL_H = 238;
    private static final int ROW_H = 23;
    private static final int INK = 0xFF38352E;
    private static final int MUTED = 0xFF81796A;
    private static final int JADE = 0xFF315E50;
    private static final int GOLD = 0xFF9B7C43;
    private static final int COMPLETE = 0xFF4F785A;
    private static final int CURRENT = 0xFF9A7336;

    private final Screen parent;
    private final List<DaoGuidanceContent.Chapter> visibleChapters =
            new ArrayList<>();
    private String selectedChapter = "";
    private int scrollRows;
    private AbstractButton trackButton;
    private AbstractButton cancelButton;
    private AbstractButton understoodButton;
    private int detailScroll;
    private int detailMaximumScroll;
    private int lastObjectiveIndex = -1;

    public DaoGuidanceScreen() {
        this(null);
    }

    public DaoGuidanceScreen(Screen parent) {
        super(Component.translatable(
                "screen." + ImmortalCultivationMod.MODID + ".dao_guidance"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        refreshVisibleChapters();
        if (selectedChapter.isBlank()) {
            selectedChapter = DaoGuidanceClient.trackedChapter();
        }
        if (visibleChapters.stream().noneMatch(
                chapter -> chapter.id().equals(selectedChapter))) {
            selectedChapter = visibleChapters.isEmpty()
                    ? "" : visibleChapters.getFirst().id();
        }

        int x = left();
        int y = top();
        understoodButton = addRenderableWidget(new CultivationGuiButton(
                x + DETAIL_X + 8, y + WINDOW_H - 65, DETAIL_W - 16, 20,
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".dao_guidance.understood"),
                () -> {
                    var objective = selectedObjective();
                    if (objective != null) PacketDistributor.sendToServer(
                            new DaoGuidancePayloads.ServerboundDaoGuidanceActionPayload(objective.action()));
                }));
        trackButton = addRenderableWidget(new CultivationGuiButton(
                x + DETAIL_X,
                y + WINDOW_H - 37,
                150,
                20,
                Component.empty(),
                this::toggleTracking));
        cancelButton = addRenderableWidget(new CultivationGuiButton(
                x + LIST_X + (LIST_W - 130) / 2,
                y + WINDOW_H - 37,
                130,
                20,
                Component.translatable("screen."
                        + ImmortalCultivationMod.MODID
                        + ".dao_guidance.cancel"),
                this::cancelTutorial));
        addRenderableWidget(new CultivationGuiButton(
                x + DETAIL_X + DETAIL_W - 82,
                y + WINDOW_H - 37,
                82,
                20,
                Component.translatable("gui.back"),
                () -> minecraft.setScreen(parent)));
        updateTrackButton();
    }

    private void cancelTutorial() {
        if (DaoGuidanceClient.trackedChapter().isBlank()) return;
        PacketDistributor.sendToServer(
                new DaoGuidancePayloads.ServerboundTrackDaoGuidancePayload(""));
    }

    private void refreshVisibleChapters() {
        visibleChapters.clear();
        for (DaoGuidanceContent.Chapter chapter : DaoGuidanceContent.chapters()) {
            if (!chapter.secret() || DaoGuidanceClient.discovered(chapter.id())) {
                visibleChapters.add(chapter);
            }
        }
    }

    private void toggleTracking() {
        if (selectedChapter.isBlank()
                || !DaoGuidanceClient.discovered(selectedChapter)) return;
        String next = DaoGuidanceClient.trackedChapter().equals(selectedChapter)
                ? "" : selectedChapter;
        PacketDistributor.sendToServer(
                new DaoGuidancePayloads.ServerboundTrackDaoGuidancePayload(next));
    }

    private void updateTrackButton() {
        if (trackButton == null) return;
        boolean discovered = !selectedChapter.isBlank()
                && DaoGuidanceClient.discovered(selectedChapter);
        boolean complete = discovered
                && DaoGuidanceClient.complete(selectedChapter);
        boolean tracked = selectedChapter.equals(
                DaoGuidanceClient.trackedChapter());
        var objective = selectedObjective();
        understoodButton.visible = understoodButton.active = objective != null
                && com.example.immortal_cultivation_mod.guidance.DaoGuidanceExpansion.isReading(objective.action());
        int index = DaoGuidanceClient.state(selectedChapter).objectiveIndex();
        if (index != lastObjectiveIndex) { detailScroll = 0; lastObjectiveIndex = index; }
        trackButton.active = discovered && !complete;
        trackButton.setMessage(Component.translatable(
                "screen." + ImmortalCultivationMod.MODID
                        + ".dao_guidance."
                        + (tracked ? "untrack" : "track")));
        if (cancelButton != null) {
            cancelButton.active = !DaoGuidanceClient.trackedChapter().isBlank();
        }
    }

    @Override
    public void tick() {
        super.tick();
        refreshVisibleChapters();
        updateTrackButton();
    }

    @Override
    public void render(
            GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        // Screen.render() applies Minecraft's framebuffer blur before drawing
        // its widgets. Calling it after our panel blurred the entire panel and
        // all manually drawn text, while leaving only the buttons sharp.
        // Apply that background effect first, then draw every 引道 layer on top.
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int x = left();
        int y = top();
        graphics.fill(0, 0, width, height, 0xA0101716);
        CultivationGuiTextures.drawLandscape(
                graphics, x, y, WINDOW_W, WINDOW_H);
        CultivationGuiTextures.drawInnerBox(
                graphics, x + LIST_X, y + LIST_Y, LIST_W, LIST_H);
        CultivationGuiTextures.drawInnerBox(
                graphics, x + DETAIL_X, y + DETAIL_Y, DETAIL_W, DETAIL_H);

        graphics.drawCenteredString(font, title,
                x + WINDOW_W / 2, y + 19, INK);
        graphics.drawString(font, Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".dao_guidance.chapters"),
                x + LIST_X + 8, y + 40, GOLD, false);
        renderChapterList(graphics, x, y, mouseX, mouseY);
        renderDetails(graphics, x, y);
        for (net.minecraft.client.gui.components.Renderable renderable
                : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void renderChapterList(
            GuiGraphics graphics, int x, int y,
            int mouseX, int mouseY) {
        int maxVisible = Math.max(1, (LIST_H - 10) / ROW_H);
        scrollRows = Mth.clamp(scrollRows, 0,
                Math.max(0, visibleChapters.size() - maxVisible));
        int end = Math.min(visibleChapters.size(), scrollRows + maxVisible);
        for (int index = scrollRows; index < end; index++) {
            DaoGuidanceContent.Chapter chapter = visibleChapters.get(index);
            int rowY = y + LIST_Y + 5 + (index - scrollRows) * ROW_H;
            boolean selected = chapter.id().equals(selectedChapter);
            boolean hovered = mouseX >= x + LIST_X + 5
                    && mouseX < x + LIST_X + LIST_W - 5
                    && mouseY >= rowY && mouseY < rowY + ROW_H - 2;
            if (selected || hovered) {
                graphics.fill(x + LIST_X + 5, rowY,
                        x + LIST_X + LIST_W - 5, rowY + ROW_H - 2,
                        selected ? 0x447E765F : 0x227E765F);
            }
            boolean discovered = DaoGuidanceClient.discovered(chapter.id());
            boolean complete = DaoGuidanceClient.complete(chapter.id());
            boolean tracked = chapter.id().equals(
                    DaoGuidanceClient.trackedChapter());
            String marker = !discovered ? "🔒" : complete ? "✓" : "●";
            Component name = discovered
                    ? Component.translatable(
                            DaoGuidanceContent.chapterKey(chapter.id(), "name"))
                    : Component.translatable(
                            "screen." + ImmortalCultivationMod.MODID
                                    + ".dao_guidance.unknown");
            int color = complete ? COMPLETE
                    : tracked ? CURRENT : discovered ? INK : MUTED;
            graphics.drawString(font, marker,
                    x + LIST_X + 10, rowY + 7, color, false);
            drawFitted(graphics, name,
                    x + LIST_X + 25, rowY + 7,
                    LIST_W - 37, color);
        }
        if (visibleChapters.size() > maxVisible) {
            int trackHeight = LIST_H - 12;
            int thumbHeight = Math.max(18,
                    trackHeight * maxVisible / visibleChapters.size());
            int maximumScroll = Math.max(1,
                    visibleChapters.size() - maxVisible);
            int thumbY = y + LIST_Y + 6
                    + (trackHeight - thumbHeight) * scrollRows / maximumScroll;
            graphics.fill(x + LIST_X + LIST_W - 5, y + LIST_Y + 6,
                    x + LIST_X + LIST_W - 3,
                    y + LIST_Y + LIST_H - 6, 0x44807768);
            graphics.fill(x + LIST_X + LIST_W - 6, thumbY,
                    x + LIST_X + LIST_W - 2,
                    thumbY + thumbHeight, 0xAA806D4A);
        }
    }

    private DaoGuidanceContent.Objective selectedObjective() {
        var chapter = DaoGuidanceContent.chapter(selectedChapter);
        if (chapter == null || !DaoGuidanceClient.discovered(selectedChapter)
                || DaoGuidanceClient.complete(selectedChapter)) return null;
        int index = DaoGuidanceClient.state(selectedChapter).objectiveIndex();
        return index >= 0 && index < chapter.objectives().size() ? chapter.objectives().get(index) : null;
    }

    private void renderDetails(GuiGraphics graphics, int x, int y) {
        graphics.enableScissor(x + DETAIL_X + 4, y + DETAIL_Y + 4,
                x + DETAIL_X + DETAIL_W - 4, y + WINDOW_H - 69);
        try { renderDetailContents(graphics, x, y); }
        finally { graphics.disableScissor(); }
    }

    private void renderDetailContents(GuiGraphics graphics, int x, int y) {
        DaoGuidanceContent.Chapter chapter =
                DaoGuidanceContent.chapter(selectedChapter);
        if (chapter == null) return;
        boolean discovered = DaoGuidanceClient.discovered(chapter.id());
        int textX = x + DETAIL_X + 13;
        int textY = y + DETAIL_Y + 12 - detailScroll;
        int textWidth = DETAIL_W - 26;
        if (!discovered) {
            graphics.drawCenteredString(font,
                    Component.translatable(
                            "screen." + ImmortalCultivationMod.MODID
                                    + ".dao_guidance.unknown"),
                    x + DETAIL_X + DETAIL_W / 2,
                    textY + 60, MUTED);
            graphics.drawCenteredString(font,
                    Component.translatable(
                            "screen." + ImmortalCultivationMod.MODID
                                    + ".dao_guidance.not_reached"),
                    x + DETAIL_X + DETAIL_W / 2,
                    textY + 78, MUTED);
            return;
        }

        Component chapterName = Component.translatable(
                DaoGuidanceContent.chapterKey(chapter.id(), "name"));
        graphics.drawString(font, chapterName, textX, textY, GOLD, false);
        textY += 16;
        textY = drawWrapped(graphics, Component.translatable(
                        DaoGuidanceContent.chapterKey(chapter.id(), "intro")),
                textX, textY, textWidth, MUTED, 100);
        if (DaoGuidanceContent.BREAKTHROUGH.equals(chapter.id())) {
            textY = renderBreakthroughStatus(
                    graphics, textX, textY + 4, textWidth);
        }
        textY += 7;
        graphics.fill(textX, textY, textX + textWidth, textY + 1,
                0x66877B65);
        textY += 7;

        DaoGuidancePayloads.ChapterState state =
                DaoGuidanceClient.state(chapter.id());
        for (int index = 0; index < chapter.objectives().size(); index++) {
            DaoGuidanceContent.Objective objective =
                    chapter.objectives().get(index);
            boolean done = state.complete()
                    || index < state.objectiveIndex();
            boolean current = !state.complete()
                    && index == state.objectiveIndex();
            int color = done ? COMPLETE : current ? CURRENT : MUTED;
            String marker = done ? "✓ " : current ? "● " : "· ";
            Component objectiveName = Component.literal(marker).append(
                    Component.translatable(
                            DaoGuidanceContent.objectiveKey(
                                    chapter.id(), objective.id(), "name")));
            drawFitted(graphics, objectiveName,
                    textX, textY, textWidth, color);
            textY += 13;
            if (current) {
                Component detail = objectiveDetail(chapter, objective);
                int detailLines = DaoGuidanceContent.FIND_ENLIGHTENMENT_PILL
                        .equals(objective.action()) ? 3 : 2;
                textY = drawWrapped(graphics, detail,
                        textX + 12, textY, textWidth - 12,
                        INK, 100);
                if (objective.target() > 1) {
                    textY += 3;
                    int progress = Math.min(objective.target(), state.progress());
                    int barWidth = textWidth - 24;
                    graphics.fill(textX + 12, textY,
                            textX + 12 + barWidth, textY + 7,
                            0x553E453D);
                    int filled = barWidth * progress / objective.target();
                    graphics.fill(textX + 12, textY,
                            textX + 12 + filled, textY + 7,
                            0xCC63816D);
                    Component amount = Component.literal(
                            progress + " / " + objective.target());
                    graphics.drawString(font, amount,
                            textX + textWidth - font.width(amount),
                            textY - 1, INK, false);
                    textY += 11;
                }
            }
            textY += 4;
        }
        detailMaximumScroll = Math.max(0, textY + detailScroll - (y + WINDOW_H - 74));
    }

    private int renderBreakthroughStatus(
            GuiGraphics graphics, int x, int y, int width) {
        var data = ClientData.cultivationData;
        if (data == null) return y;
        String current = data.cultivationLevel();
        String next = CultivationLevels.getNextStage(current);
        var level = CultivationLevels.getLevelDef(current);
        if (next == null || level == null) return y;

        long requiredProgress =
                CultivationLevels.getTotalQiNeeded(current);
        int maximumQi = Math.max(1,
                data.maxQi());
        boolean progressReady =
                data.cultivationProgress() >= requiredProgress;
        boolean qiReady = data.qi() >= maximumQi;
        boolean methodReady = !CultivationMethods.isAtOrPastLimit(
                data.activeCultivationMethod(), current);
        boolean pillRequired = CultivationLevels.needsBreakthroughPill(current);
        boolean pillReady = !pillRequired
                || minecraft.player != null
                && minecraft.player.getInventory().items.stream()
                .anyMatch(stack -> stack.is(ModItems.BREAKTHROUGH_PILL.get()));

        graphics.drawString(font, Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".dao_guidance.current_state"),
                x, y, GOLD, false);
        y += 12;
        y = drawRequirement(graphics, x, y, width, progressReady,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".dao_guidance.requirement.progress",
                        data.cultivationProgress(), requiredProgress));
        y = drawRequirement(graphics, x, y, width, qiReady,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".dao_guidance.requirement.qi",
                        data.qi(), maximumQi));
        y = drawRequirement(graphics, x, y, width, methodReady,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".dao_guidance.requirement.method"));
        if (pillRequired) {
            y = drawRequirement(graphics, x, y, width, pillReady,
                    Component.translatable(
                            "screen." + ImmortalCultivationMod.MODID
                                    + ".dao_guidance.requirement.pill"));
        }
        int success = Mth.clamp(Math.round((1.0F
                - CultivationLevels.getBreakthroughFailChance(
                data.luck(), current)) * 100.0F), 0, 100);
        drawFitted(graphics, Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".dao_guidance.success_chance", success),
                x, y, width, MUTED);
        return y + 12;
    }

    private int drawRequirement(
            GuiGraphics graphics, int x, int y, int width,
            boolean ready, Component text) {
        Component line = Component.literal(ready ? "✓ " : "✕ ")
                .append(text);
        drawFitted(graphics, line, x, y, width,
                ready ? COMPLETE : 0xFF9A493E);
        return y + 11;
    }

    private Component objectiveDetail(
            DaoGuidanceContent.Chapter chapter,
            DaoGuidanceContent.Objective objective) {
        String key = DaoGuidanceContent.objectiveKey(
                chapter.id(), objective.id(), "detail");
        return switch (objective.action()) {
            case DaoGuidanceContent.FIND_ENLIGHTENMENT_PILL ->
                    caveObjectiveDetail(key);
            case DaoGuidanceContent.OPEN_STATS -> Component.translatable(
                    key, ModKeyMappings.OPEN_STAT_MENU.get()
                            .getTranslatedKeyMessage());
            case DaoGuidanceContent.MEDITATE_QI -> Component.translatable(
                    key, ModKeyMappings.MEDITATE.get()
                            .getTranslatedKeyMessage());
            case DaoGuidanceContent.EQUIP_SPELL -> Component.translatable(
                    key, ModKeyMappings.OPEN_SPELL_SELECTION.get()
                            .getTranslatedKeyMessage());
            case DaoGuidanceContent.CAST_SPELL -> chapter.id().equals(
                    DaoGuidanceContent.FIRST_SPELLS)
                    ? Component.translatable(key,
                    ModKeyMappings.OPEN_SPELL_WHEEL.get()
                            .getTranslatedKeyMessage(),
                    ModKeyMappings.CAST_PREPARED_SPELL.get()
                            .getTranslatedKeyMessage())
                    : Component.translatable(key,
                    ModKeyMappings.CAST_PREPARED_SPELL.get()
                            .getTranslatedKeyMessage());
            case DaoGuidanceContent.CHARGED_SPELL -> Component.translatable(
                    key, ModKeyMappings.CAST_PREPARED_SPELL.get()
                            .getTranslatedKeyMessage());
            case DaoGuidanceContent.ACTIVATE_SENSE, DaoGuidanceContent.DETECT_HIDDEN_LIVING -> Component.translatable(
                    key, ModKeyMappings.SPIRITUAL_SENSE.get()
                            .getTranslatedKeyMessage());
            case DaoGuidanceContent.WEAPON_ATTACK -> Component.translatable(
                    key, ModKeyMappings.TOGGLE_COMBAT_MODE.get()
                            .getTranslatedKeyMessage());
            case DaoGuidanceContent.EXTERNAL_QI -> Component.translatable(
                    key, ModKeyMappings.INJECT_QI.get()
                            .getTranslatedKeyMessage());
            default -> Component.translatable(key);
        };
    }

    private Component caveObjectiveDetail(String key) {
        if (!DaoGuidanceClient.hasCaveTarget()) {
            return Component.translatable(key + ".searching");
        }
        if (minecraft == null || minecraft.level == null
                || minecraft.player == null
                || !DaoGuidanceClient.caveTargetDimension().equals(
                minecraft.level.dimension().location().toString())) {
            return Component.translatable(key + ".overworld");
        }
        return Component.translatable(key,
                DaoGuidanceClient.caveTargetDirection(
                        minecraft.player.getX(), minecraft.player.getZ()),
                DaoGuidanceClient.caveTargetDistance(
                        minecraft.player.getX(), minecraft.player.getZ()),
                DaoGuidanceClient.caveTargetX(),
                DaoGuidanceClient.caveTargetZ());
    }

    private int drawWrapped(
            GuiGraphics graphics, Component text,
            int x, int y, int width, int color, int maximumLines) {
        List<net.minecraft.util.FormattedCharSequence> lines =
                font.split(text, width);
        int count = Math.min(maximumLines, lines.size());
        for (int index = 0; index < count; index++) {
            graphics.drawString(font, lines.get(index), x,
                    y + index * 10, color, false);
        }
        return y + count * 10;
    }

    private void drawFitted(
            GuiGraphics graphics, Component text,
            int x, int y, int width, int color) {
        int textWidth = font.width(text);
        if (textWidth <= width) {
            graphics.drawString(font, text, x, y, color, false);
            return;
        }
        String ellipsis = "…";
        int available = Math.max(1, width - font.width(ellipsis));
        String fitted = font.plainSubstrByWidth(text.getString(), available);
        graphics.drawString(font, Component.literal(fitted + ellipsis),
                x, y, color, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int x = left();
            int y = top();
            int relative = (int) mouseY - (y + LIST_Y + 5);
            if (mouseX >= x + LIST_X + 5
                    && mouseX < x + LIST_X + LIST_W - 5
                    && relative >= 0 && relative < LIST_H - 10) {
                int index = scrollRows + relative / ROW_H;
                if (index >= 0 && index < visibleChapters.size()) {
                    selectedChapter = visibleChapters.get(index).id();
                    detailScroll = 0;
                    lastObjectiveIndex = -1;
                    updateTrackButton();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(
            double mouseX, double mouseY,
            double scrollX, double scrollY) {
        int x = left();
        int y = top();
        if (mouseX >= x + DETAIL_X && mouseX < x + DETAIL_X + DETAIL_W
                && mouseY >= y + DETAIL_Y && mouseY < y + WINDOW_H - 69) {
            detailScroll = Mth.clamp(detailScroll + (scrollY < 0 ? 24 : -24), 0, detailMaximumScroll);
            return true;
        }
        if (mouseX >= x + LIST_X && mouseX < x + LIST_X + LIST_W
                && mouseY >= y + LIST_Y && mouseY < y + LIST_Y + LIST_H) {
            int maximum = Math.max(0, visibleChapters.size()
                    - Math.max(1, (LIST_H - 10) / ROW_H));
            scrollRows = Mth.clamp(scrollRows
                    + (scrollY < 0.0D ? 1 : -1), 0, maximum);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int left() {
        return (width - WINDOW_W) / 2;
    }

    private int top() {
        return (height - WINDOW_H) / 2;
    }
}
