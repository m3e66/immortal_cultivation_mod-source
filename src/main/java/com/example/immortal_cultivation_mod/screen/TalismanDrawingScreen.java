package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.block.TalismanDrawingTableBlockEntity;
import com.example.immortal_cultivation_mod.client.ClientData;
import com.example.immortal_cultivation_mod.item.TalismanDrawingPatterns;
import com.example.immortal_cultivation_mod.item.TalismanItem;
import com.example.immortal_cultivation_mod.item.TalismanRecipes;
import com.example.immortal_cultivation_mod.network.TalismanPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Five-stage talisman workflow: formula, materials, tracing, Qi control, result. */
public final class TalismanDrawingScreen extends AbstractContainerScreen<TalismanDrawingMenu> {
    /** Slightly narrows the trace while preserving its full vertical height. */
    private static final float TRACE_HORIZONTAL_SCALE = 0.90F;
    private static final int INK = 0xFF38352E;
    private static final int MUTED = 0xFF756F63;
    private static final int GOLD = 0xFF9B7C43;
    private static final int GOLD_BRIGHT = 0xFFCA9B49;
    private static final int JADE = 0xFF315E50;
    private static final int PAPER = 0xE8EEE6D9;
    private static final int PAPER_DARK = 0xD8D8CDBB;
    private static final int TRACE = 0xFFD14A32;

    private static final int LIST_X = 12;
    private static final int LIST_Y = 30;
    private static final int LIST_W = 110;
    private static final int LIST_H = 206;
    private static final int ROW_H = 18;
    private static final int CANVAS_X = 132;
    private static final int CANVAS_Y = 30;
    private static final int CANVAS_W = 210;
    private static final int CANVAS_H = 206;
    private static final int RIGHT_X = 352;
    private static final int RIGHT_Y = 30;
    private static final int RIGHT_W = 116;
    private static final int RIGHT_H = 206;
    private static final int CLEAR_X = 356;
    private static final int ACTION_X = 410;
    private static final int BUTTON_Y = 212;
    private static final int BUTTON_W = 52;
    private static final int BUTTON_H = 20;
    private static final int QI_BAR_X = 360;
    private static final int QI_BAR_Y = 196;
    private static final int QI_BAR_W = 100;
    private static final int QI_BAR_H = 10;
    private static final int TRACE_X_OFFSET = 8;
    private static final int TRACE_Y_OFFSET = 40;
    private static final int TRACE_RIGHT_PAD = 8;
    private static final int TRACE_BOTTOM_PAD = 8;

    private final List<List<TalismanDrawingPatterns.Point>> strokes = new ArrayList<>();
    private List<TalismanDrawingPatterns.Point> activeStroke;
    private String selectedId = "";
    private String lastFormulaId = "";
    private boolean instantMode;
    private boolean drawingAttemptStarted;
    private int scroll;
    private Stage stage = Stage.DRAW;
    private int seenResultNonce;
    private Component status = Component.empty();

    public TalismanDrawingScreen(TalismanDrawingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = TalismanDrawingMenu.IMAGE_WIDTH;
        imageHeight = TalismanDrawingMenu.IMAGE_HEIGHT;
        titleLabelX = -1000;
        titleLabelY = -1000;
        inventoryLabelX = TalismanDrawingMenu.PLAYER_INV_X;
        inventoryLabelY = TalismanDrawingMenu.PLAYER_INV_Y - 12;
    }

    @Override
    protected void init() {
        super.init();
        seenResultNonce = menu.resultNonce();
        lastFormulaId = menu.formulaId();
        if (TalismanRecipes.byId(lastFormulaId) != null) {
            selectedId = lastFormulaId;
            instantMode = false;
            return;
        }
        List<TalismanRecipes.Def> recipes = availableRecipes();
        if (!recipes.isEmpty()) {
            selectedId = recipes.getFirst().id();
            instantMode = true;
        }
    }

    private List<TalismanRecipes.Def> availableRecipes() {
        if (minecraft == null || minecraft.player == null
                || ClientData.cultivationData == null) return List.of();
        var data = ClientData.cultivationData;
        return TalismanRecipes.all().stream()
                .filter(recipe -> TalismanRecipes.isMastered(data, recipe.id())).toList();
    }

    private TalismanRecipes.Def selected() {
        return TalismanRecipes.byId(selectedId);
    }

    private TalismanDrawingPatterns.Pattern pattern() {
        return TalismanDrawingPatterns.forRecipe(selectedId);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (menu.resultNonce() != seenResultNonce) {
            seenResultNonce = menu.resultNonce();
            status = Component.translatable(resultKey(menu.result()), menu.accuracy());
            stage = Stage.DRAW;
            drawingAttemptStarted = false;
            strokes.clear();
            activeStroke = null;
        }
        String formulaId = menu.formulaId();
        if (!formulaId.equals(lastFormulaId)) {
            cancelDrawingAttempt();
            lastFormulaId = formulaId;
            if (TalismanRecipes.byId(formulaId) != null) {
                selectedId = formulaId;
                instantMode = false;
            } else if (!instantMode) {
                List<TalismanRecipes.Def> mastered = availableRecipes();
                selectedId = mastered.isEmpty() ? "" : mastered.getFirst().id();
                instantMode = !selectedId.isEmpty();
            }
            clearTrace();
            status = Component.empty();
        }
        List<TalismanRecipes.Def> available = availableRecipes();
        if (instantMode && !selectedId.isEmpty()
                && available.stream().noneMatch(r -> r.id().equals(selectedId))) {
            cancelDrawingAttempt();
            selectedId = available.isEmpty() ? "" : available.getFirst().id();
            clearTrace();
        } else if (!instantMode && !selectedId.equals(formulaId)) {
            cancelDrawingAttempt();
            selectedId = TalismanRecipes.byId(formulaId) == null ? "" : formulaId;
            clearTrace();
        }
        scroll = Mth.clamp(scroll, 0, Math.max(0, available.size() - LIST_H / ROW_H));
    }

    private static String resultKey(int result) {
        String suffix = switch (result) {
            case TalismanDrawingTableBlockEntity.RESULT_SUCCESS -> "success";
            case TalismanDrawingTableBlockEntity.RESULT_INVALID_FORMULA -> "invalid_formula";
            case TalismanDrawingTableBlockEntity.RESULT_NOT_LEARNED -> "not_learned";
            case TalismanDrawingTableBlockEntity.RESULT_PAPER_TOO_WEAK -> "paper";
            case TalismanDrawingTableBlockEntity.RESULT_INK_TOO_WEAK -> "ink";
            case TalismanDrawingTableBlockEntity.RESULT_PROFICIENCY_TOO_LOW -> "proficiency";
            case TalismanDrawingTableBlockEntity.RESULT_NOT_ENOUGH_QI -> "qi";
            case TalismanDrawingTableBlockEntity.RESULT_INACCURATE_DRAWING -> "accuracy";
            case TalismanDrawingTableBlockEntity.RESULT_OUTPUT_BLOCKED -> "output";
            case TalismanDrawingTableBlockEntity.RESULT_NOT_MASTERED -> "not_mastered";
            case TalismanDrawingTableBlockEntity.RESULT_DRAWING_NOT_STARTED -> "not_started";
            default -> "none";
        };
        return "screen." + ImmortalCultivationMod.MODID + ".talisman_drawing.result." + suffix;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        CultivationGuiTextures.drawLandscape(graphics, leftPos, topPos, imageWidth, imageHeight);
        drawPanel(graphics, LIST_X, LIST_Y, LIST_W, LIST_H);
        drawPanel(graphics, CANVAS_X, CANVAS_Y, CANVAS_W, CANVAS_H);
        drawPanel(graphics, RIGHT_X, RIGHT_Y, RIGHT_W, RIGHT_H);
        drawInventory(graphics);
        renderRecipeList(graphics, mouseX, mouseY);
        renderCanvas(graphics);
        renderMaterialSlots(graphics);
        renderButtons(graphics, mouseX, mouseY);
        if (stage == Stage.INJECT) renderQiBar(graphics);
    }

    private void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        CultivationGuiTextures.drawInnerBox(graphics, leftPos + x, topPos + y, width, height);
    }

    private void renderRecipeList(GuiGraphics graphics, int mouseX, int mouseY) {
        List<TalismanRecipes.Def> recipes = availableRecipes();
        int visible = LIST_H / ROW_H;
        for (int row = 0; row < visible && row + scroll < recipes.size(); row++) {
            TalismanRecipes.Def recipe = recipes.get(row + scroll);
            int x = leftPos + LIST_X + 5;
            int y = topPos + LIST_Y + 4 + row * ROW_H;
            boolean selected = instantMode && recipe.id().equals(selectedId);
            boolean hovered = mouseX >= x && mouseX < x + LIST_W - 10
                    && mouseY >= y && mouseY < y + ROW_H - 1;
            graphics.fill(x, y, x + LIST_W - 10, y + ROW_H - 1,
                    selected ? 0xD8CAD8CB : hovered ? 0xD8E4DED1 : 0xB8EEE8DC);
            graphics.renderOutline(x, y, LIST_W - 10, ROW_H - 1,
                    selected ? JADE : hovered ? GOLD_BRIGHT : 0xFFAAA18F);
            Component name = recipe.result().getHoverName();
            graphics.drawString(font, font.plainSubstrByWidth(name.getString(), LIST_W - 23),
                    x + 4, y + 5, selected ? JADE : INK, false);
            graphics.drawString(font, Integer.toString(recipe.strengthTier()),
                    x + LIST_W - 19, y + 5, GOLD, false);
        }
    }

    private void renderCanvas(GuiGraphics graphics) {
        int paperX = leftPos + CANVAS_X + 8;
        int paperY = topPos + CANVAS_Y + 8;
        int paperW = CANVAS_W - 16;
        int paperH = CANVAS_H - 16;
        graphics.fill(paperX, paperY, paperX + paperW, paperY + paperH, PAPER);
        graphics.renderOutline(paperX, paperY, paperW, paperH, 0xFFB9AA91);
        if (selectedId.isEmpty()) return;

        int x = leftPos + CANVAS_X + traceXOffset();
        int y = topPos + CANVAS_Y + TRACE_Y_OFFSET;
        int w = traceWidth();
        int h = CANVAS_H - TRACE_Y_OFFSET - TRACE_BOTTOM_PAD;

        TalismanDrawingPatterns.Pattern pattern = pattern();
        for (int i = 0; i < pattern.strokes().size(); i++) {
            TalismanDrawingPatterns.Stroke guide = pattern.strokes().get(i);
            int color = i == strokes.size() && stage == Stage.DRAW
                    ? 0xD0B33529
                    : 0x468A281F;
            drawPath(graphics, guide.points(), x, y, w, h, color, 1);
        }
        if (!instantMode) {
            for (List<TalismanDrawingPatterns.Point> stroke : strokes) {
                drawPath(graphics, stroke, x, y, w, h, TRACE, 2);
            }
            if (activeStroke != null) drawPath(graphics, activeStroke, x, y, w, h, TRACE, 2);
        }
    }

    private void drawPath(GuiGraphics graphics, List<TalismanDrawingPatterns.Point> points,
                          int x, int y, int width, int height, int color, int thickness) {
        for (int i = 0; i + 1 < points.size(); i++) {
            TalismanDrawingPatterns.Point a = points.get(i);
            TalismanDrawingPatterns.Point b = points.get(i + 1);
            int ax = x + Math.round(a.x() * width / 1000.0F);
            int ay = y + Math.round(a.y() * height / 1000.0F);
            int bx = x + Math.round(b.x() * width / 1000.0F);
            int by = y + Math.round(b.y() * height / 1000.0F);
            int steps = Math.max(Math.abs(bx - ax), Math.abs(by - ay));
            for (int step = 0; step <= steps; step++) {
                float t = steps == 0 ? 0.0F : step / (float) steps;
                int px = Math.round(Mth.lerp(t, ax, bx));
                int py = Math.round(Mth.lerp(t, ay, by));
                graphics.fill(px, py, px + thickness, py + thickness, color);
            }
        }
    }

    private void renderMaterialSlots(GuiGraphics graphics) {
        slotFrame(graphics, TalismanDrawingMenu.FORMULA_X, TalismanDrawingMenu.FORMULA_Y, JADE);
        slotFrame(graphics, TalismanDrawingMenu.PAPER_X, TalismanDrawingMenu.PAPER_Y, GOLD);
        slotFrame(graphics, TalismanDrawingMenu.INK_X, TalismanDrawingMenu.INK_Y, TRACE);
        slotFrame(graphics, TalismanDrawingMenu.OUTPUT_X, TalismanDrawingMenu.OUTPUT_Y, JADE);
    }

    private void slotFrame(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(leftPos + x - 2, topPos + y - 2,
                leftPos + x + 18, topPos + y + 18, color);
        graphics.fill(leftPos + x - 1, topPos + y - 1,
                leftPos + x + 17, topPos + y + 17, PAPER_DARK);
    }

    private void renderButtons(GuiGraphics graphics, int mouseX, int mouseY) {
        button(graphics, CLEAR_X, BUTTON_Y, Component.translatable(
                "screen." + ImmortalCultivationMod.MODID + ".talisman_drawing.clear"),
                !instantMode && (drawingAttemptStarted
                        || !strokes.isEmpty() || activeStroke != null), mouseX, mouseY);
        boolean ready = selected() != null && (instantMode
                || strokes.size() >= pattern().strokes().size());
        Component action = Component.translatable("screen." + ImmortalCultivationMod.MODID
                + (instantMode ? ".talisman_drawing.instant_create"
                : stage == Stage.INJECT ? ".talisman_drawing.seal_qi"
                : ".talisman_drawing.inject_qi"));
        button(graphics, ACTION_X, BUTTON_Y, action,
                stage != Stage.WAITING && (instantMode && ready
                        || stage == Stage.INJECT || stage == Stage.DRAW && ready), mouseX, mouseY);
    }

    private void button(GuiGraphics graphics, int x, int y, Component label, boolean active,
                        int mouseX, int mouseY) {
        boolean hovered = mouseX >= leftPos + x && mouseX < leftPos + x + BUTTON_W
                && mouseY >= topPos + y && mouseY < topPos + y + BUTTON_H;
        CultivationGuiTextures.drawButton(graphics, leftPos + x, topPos + y,
                BUTTON_W, BUTTON_H, active, hovered);
        CultivationGuiTextures.drawButtonText(graphics, label, leftPos + x, topPos + y,
                BUTTON_W, BUTTON_H, active);
    }

    private void renderQiBar(GuiGraphics graphics) {
        int x = leftPos + QI_BAR_X;
        int y = topPos + QI_BAR_Y;
        graphics.fill(x, y, x + QI_BAR_W, y + QI_BAR_H, 0xFF433E36);
        graphics.fill(x + QI_BAR_W / 2 - 8, y + 1,
                x + QI_BAR_W / 2 + 8, y + QI_BAR_H - 1, 0xFF4A8A71);
        int marker = x + Math.round(injectionPosition() * (QI_BAR_W - 2));
        graphics.fill(marker, y - 2, marker + 2, y + QI_BAR_H + 2, 0xFFFFE49A);
        graphics.renderOutline(x, y, QI_BAR_W, QI_BAR_H, GOLD);
    }

    private static float injectionPosition() {
        float phase = (System.currentTimeMillis() % 1800L) / 900.0F;
        return phase <= 1.0F ? phase : 2.0F - phase;
    }

    private void drawInventory(GuiGraphics graphics) {
        int x = leftPos + TalismanDrawingMenu.PLAYER_INV_X - 8;
        int y = topPos + TalismanDrawingMenu.PLAYER_INV_Y - 18;
        CultivationGuiTextures.drawInnerBox(graphics, x, y, 178, 98);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                slotFrame(graphics, TalismanDrawingMenu.PLAYER_INV_X + column * 18,
                        TalismanDrawingMenu.PLAYER_INV_Y + row * 18, 0xFFAAA18F);
            }
        }
        for (int column = 0; column < 9; column++) {
            slotFrame(graphics, TalismanDrawingMenu.PLAYER_INV_X + column * 18,
                    TalismanDrawingMenu.PLAYER_INV_Y + 58, 0xFFAAA18F);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        drawCentered(graphics, title, imageWidth / 2, 10, INK);
        graphics.drawString(font, Component.translatable("screen." + ImmortalCultivationMod.MODID
                + ".talisman_drawing.mastered_formulas"), LIST_X + 5, 19, GOLD, false);
        TalismanRecipes.Def recipe = selected();
        if (recipe != null) {
            drawCentered(graphics, Component.translatable(TalismanRecipes.patternNameKey(recipe)),
                    CANVAS_X + CANVAS_W / 2, CANVAS_Y + 10, TRACE);
            if (instantMode) {
                drawCentered(graphics, Component.translatable("screen."
                                + ImmortalCultivationMod.MODID
                                + ".talisman_drawing.instant_ready"),
                        CANVAS_X + CANVAS_W / 2, CANVAS_Y + 23, JADE);
            } else {
                drawCentered(graphics, Component.translatable("screen."
                                + ImmortalCultivationMod.MODID
                                + ".talisman_drawing.stroke_progress", strokes.size(),
                        pattern().strokes().size()), CANVAS_X + CANVAS_W / 2,
                        CANVAS_Y + 22, INK);
            }
            List<net.minecraft.util.FormattedCharSequence> requirements = font.split(
                    Component.translatable("screen." + ImmortalCultivationMod.MODID
                                    + ".talisman_drawing.requirements", recipe.minPaperTier(),
                            recipe.minInkTier(), recipe.masterTier()), RIGHT_W - 16);
            for (int index = 0; index < Math.min(2, requirements.size()); index++) {
                graphics.drawString(font, requirements.get(index), RIGHT_X + 8,
                        RIGHT_Y + 8 + index * 10, MUTED, false);
            }
            graphics.drawString(font, Component.translatable("screen." + ImmortalCultivationMod.MODID
                            + ".talisman_drawing.qi_cost", recipe.qiCost()), RIGHT_X + 8,
                    RIGHT_Y + 30, INK, false);
        } else {
            drawCentered(graphics, Component.translatable("screen." + ImmortalCultivationMod.MODID
                            + ".talisman_drawing.insert_formula"),
                    CANVAS_X + CANVAS_W / 2, CANVAS_Y + 18, MUTED);
        }
        if (availableRecipes().isEmpty()) {
            graphics.drawString(font, Component.translatable("screen." + ImmortalCultivationMod.MODID
                    + ".talisman_drawing.no_mastered"), LIST_X + 8, 48, MUTED, false);
        }
        drawCentered(graphics, Component.translatable("screen." + ImmortalCultivationMod.MODID
                + ".talisman_drawing.formula"), TalismanDrawingMenu.FORMULA_X + 8,
                TalismanDrawingMenu.FORMULA_Y + 23, MUTED);
        drawCentered(graphics, Component.translatable("screen." + ImmortalCultivationMod.MODID
                + ".talisman_drawing.paper"), TalismanDrawingMenu.PAPER_X + 8,
                TalismanDrawingMenu.PAPER_Y + 23, MUTED);
        drawCentered(graphics, Component.translatable("screen." + ImmortalCultivationMod.MODID
                + ".talisman_drawing.ink"), TalismanDrawingMenu.INK_X + 8,
                TalismanDrawingMenu.INK_Y + 23, MUTED);
        if (stage != Stage.INJECT) {
            drawCentered(graphics, Component.translatable("screen."
                    + ImmortalCultivationMod.MODID + ".talisman_drawing.output"),
                    TalismanDrawingMenu.OUTPUT_X + 8,
                    TalismanDrawingMenu.OUTPUT_Y + 23, MUTED);
        }
        if (!status.getString().isEmpty()) {
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(status, RIGHT_W - 16);
            for (int i = 0; i < Math.min(2, lines.size()); i++) {
                graphics.drawString(font, lines.get(i), RIGHT_X + 8, 174 + i * 10,
                        menu.result() == TalismanDrawingTableBlockEntity.RESULT_SUCCESS ? JADE : TRACE, false);
            }
        }
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, INK, false);
    }

    private void drawCentered(GuiGraphics graphics, Component text, int centerX, int y, int color) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            List<TalismanRecipes.Def> recipes = availableRecipes();
            if (inside(mouseX, mouseY, LIST_X + 5, LIST_Y + 4, LIST_W - 10, LIST_H - 8)) {
                int row = (int) ((mouseY - topPos - LIST_Y - 4) / ROW_H);
                int index = scroll + row;
                if (index >= 0 && index < recipes.size()) {
                    cancelDrawingAttempt();
                    selectedId = recipes.get(index).id();
                    instantMode = true;
                    clearTrace();
                    status = Component.empty();
                    return true;
                }
            }
            if (inside(mouseX, mouseY, CLEAR_X, BUTTON_Y, BUTTON_W, BUTTON_H)) {
                cancelDrawingAttempt();
                clearTrace();
                return true;
            }
            if (inside(mouseX, mouseY, ACTION_X, BUTTON_Y, BUTTON_W, BUTTON_H)) {
                if (instantMode && stage != Stage.WAITING && selected() != null) submit(true);
                else if (stage == Stage.INJECT) submit(false);
                else if (stage == Stage.DRAW && selected() != null
                        && strokes.size() >= pattern().strokes().size()) {
                    stage = Stage.INJECT;
                    status = Component.translatable("screen." + ImmortalCultivationMod.MODID
                            + ".talisman_drawing.inject_hint");
                }
                return true;
            }
            if (stage == Stage.INJECT
                    && inside(mouseX, mouseY, QI_BAR_X, QI_BAR_Y, QI_BAR_W, QI_BAR_H)) {
                submit(false);
                return true;
            }
            if (!instantMode && stage == Stage.DRAW && selected() != null
                    && strokes.size() < pattern().strokes().size()
                    && insideCanvas(mouseX, mouseY)) {
                if (!drawingAttemptStarted) {
                    PacketDistributor.sendToServer(
                            new TalismanPayloads.ServerboundDrawingAttempt(selectedId, true));
                    drawingAttemptStarted = true;
                }
                activeStroke = new ArrayList<>();
                appendTracePoint(mouseX, mouseY);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double dragX, double dragY) {
        if (button == 0 && activeStroke != null) {
            appendTracePoint(mouseX, mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && activeStroke != null) {
            appendTracePoint(mouseX, mouseY);
            if (activeStroke.size() >= 2) strokes.add(List.copyOf(activeStroke));
            activeStroke = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (inside(mouseX, mouseY, LIST_X, LIST_Y, LIST_W, LIST_H)) {
            int max = Math.max(0, availableRecipes().size() - LIST_H / ROW_H);
            scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, max);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void appendTracePoint(double mouseX, double mouseY) {
        if (activeStroke == null || !insideCanvas(mouseX, mouseY)) return;
        int canvasX = leftPos + CANVAS_X + traceXOffset();
        int canvasY = topPos + CANVAS_Y + TRACE_Y_OFFSET;
        int canvasW = traceWidth();
        int canvasH = CANVAS_H - TRACE_Y_OFFSET - TRACE_BOTTOM_PAD;
        int x = Mth.clamp((int) Math.round((mouseX - canvasX) * 1000.0D / canvasW), 0, 1000);
        int y = Mth.clamp((int) Math.round((mouseY - canvasY) * 1000.0D / canvasH), 0, 1000);
        TalismanDrawingPatterns.Point next = new TalismanDrawingPatterns.Point(x, y);
        if (activeStroke.isEmpty()) {
            activeStroke.add(next);
            return;
        }
        TalismanDrawingPatterns.Point last = activeStroke.getLast();
        if (Math.hypot(last.x() - x, last.y() - y) >= 12.0D && activeStroke.size() < 128) {
            activeStroke.add(next);
        }
    }

    private void submit(boolean instant) {
        TalismanRecipes.Def recipe = selected();
        if (recipe == null) return;
        float position = injectionPosition();
        int control = instant ? 100 : Mth.clamp(
                Math.round(100.0F - Math.abs(position - 0.5F) * 200.0F), 0, 100);
        List<Integer> encoded = new ArrayList<>();
        if (!instant) {
            for (List<TalismanDrawingPatterns.Point> stroke : strokes) {
                for (TalismanDrawingPatterns.Point point : stroke) {
                    encoded.add(point.x());
                    encoded.add(point.y());
                }
                encoded.add(-1);
                encoded.add(-1);
            }
        }
        PacketDistributor.sendToServer(new TalismanPayloads.ServerboundDrawTalisman(
                recipe.id(), control, List.copyOf(encoded), instant));
        stage = Stage.WAITING;
        status = Component.translatable("screen." + ImmortalCultivationMod.MODID
                + ".talisman_drawing.waiting");
    }

    private boolean insideCanvas(double mouseX, double mouseY) {
        return inside(mouseX, mouseY, CANVAS_X + traceXOffset(),
                CANVAS_Y + TRACE_Y_OFFSET,
                traceWidth(),
                CANVAS_H - TRACE_Y_OFFSET - TRACE_BOTTOM_PAD);
    }

    private static int traceWidth() {
        int available = CANVAS_W - TRACE_X_OFFSET - TRACE_RIGHT_PAD;
        return Math.max(1, Math.round(available * TRACE_HORIZONTAL_SCALE));
    }

    private static int traceXOffset() {
        int available = CANVAS_W - TRACE_X_OFFSET - TRACE_RIGHT_PAD;
        return TRACE_X_OFFSET + (available - traceWidth()) / 2;
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY >= topPos + y && mouseY < topPos + y + height;
    }

    private void clearTrace() {
        strokes.clear();
        activeStroke = null;
        drawingAttemptStarted = false;
        stage = Stage.DRAW;
    }

    private void cancelDrawingAttempt() {
        if (!drawingAttemptStarted) return;
        PacketDistributor.sendToServer(
                new TalismanPayloads.ServerboundDrawingAttempt(selectedId, false));
        drawingAttemptStarted = false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private enum Stage {
        DRAW,
        INJECT,
        WAITING
    }
}
