package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class LingqiTrajectoryHelpScreen extends Screen {
    private static final String PREFIX = "screen." + ImmortalCultivationMod.MODID + ".trajectory.help.";
    private static final int PANEL_WIDTH = 430;
    private static final int PANEL_HEIGHT = 343;
    private static final int ROW_GAP = 3;
    private static final List<LingqiTrajectoryConfig.NodeType> NODE_TYPES = List.of(
            LingqiTrajectoryConfig.NodeType.NORMAL,
            LingqiTrajectoryConfig.NodeType.START,
            LingqiTrajectoryConfig.NodeType.CORE,
            LingqiTrajectoryConfig.NodeType.SUB_CORE,
            LingqiTrajectoryConfig.NodeType.BLOCKED,
            LingqiTrajectoryConfig.NodeType.ELEMENT,
            LingqiTrajectoryConfig.NodeType.GATHER,
            LingqiTrajectoryConfig.NodeType.DRAIN,
            LingqiTrajectoryConfig.NodeType.STABILIZE,
            LingqiTrajectoryConfig.NodeType.SPLIT,
            LingqiTrajectoryConfig.NodeType.REVERSE,
            LingqiTrajectoryConfig.NodeType.UNSTABLE,
            LingqiTrajectoryConfig.NodeType.CONVERTER,
            LingqiTrajectoryConfig.NodeType.QI_CHANNEL,
            LingqiTrajectoryConfig.NodeType.TIMED,
            LingqiTrajectoryConfig.NodeType.TIMED_TARGET,
            LingqiTrajectoryConfig.NodeType.ROTATING,
            LingqiTrajectoryConfig.NodeType.LINKED,
            LingqiTrajectoryConfig.NodeType.SWITCH,
            LingqiTrajectoryConfig.NodeType.GATE,
            LingqiTrajectoryConfig.NodeType.FALSE_CORE);

    private final Screen parent;
    private int scroll;

    public LingqiTrajectoryHelpScreen(Screen parent) {
        super(Component.translatable(PREFIX + "title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(new CultivationGuiButton(
                panelX() + panelWidth() / 2 - 43,
                panelY() + panelHeight() - 27, 86, 20,
                Component.translatable(PREFIX + "back"), this::onClose));
        scroll = Mth.clamp(scroll, 0, maxScroll());
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= panelX() + 10 && mouseX < panelX() + panelWidth() - 10
                && mouseY >= listTop() && mouseY < listBottom()) {
            scroll = Mth.clamp(scroll - (int) Math.signum(scrollY) * 18, 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int panelX = panelX();
        int panelY = panelY();
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        Minecraft mc = Minecraft.getInstance();

        graphics.fill(0, 0, width, height, 0xA0101716);
        CultivationGuiTextures.drawLandscape(graphics, panelX, panelY, panelWidth, panelHeight);
        graphics.drawCenteredString(mc.font, title, panelX + panelWidth / 2,
                panelY + 20, 0xFF38352E);

        List<net.minecraft.util.FormattedCharSequence> intro = mc.font.split(
                Component.translatable(PREFIX + "intro"), panelWidth - 28);
        int introY = panelY + 46;
        for (net.minecraft.util.FormattedCharSequence line : intro) {
            graphics.drawString(mc.font, line, panelX + 14, introY, 0xFF5B4A35, false);
            introY += 9;
        }

        graphics.enableScissor(panelX + 9, listTop(), panelX + panelWidth - 9, listBottom());
        int y = listTop() - scroll;
        int rowIndex = 0;
        for (String key : List.of("difficulty", "realm_examples")) {
            int rowHeight = specialRowHeight(key);
            if (y + rowHeight >= listTop() && y < listBottom())
                drawSpecialEntry(graphics, key, "?", 0xFF80642D, y, rowHeight, rowIndex);
            y += rowHeight + ROW_GAP;
            rowIndex++;
        }
        for (LingqiTrajectoryConfig.NodeType type : NODE_TYPES) {
            int rowHeight = rowHeight(type);
            if (y + rowHeight >= listTop() && y < listBottom()) {
                drawNodeEntry(graphics, type, y, rowHeight, rowIndex);
            }
            y += rowHeight + ROW_GAP;
            rowIndex++;
        }
        int hazardHeight = specialRowHeight("moving_hazard");
        if (y + hazardHeight >= listTop() && y < listBottom()) {
            drawSpecialEntry(graphics, "moving_hazard", "!", 0xFFB62528,
                    y, hazardHeight, rowIndex);
        }
        graphics.disableScissor();
        drawScrollbar(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawNodeEntry(GuiGraphics graphics, LingqiTrajectoryConfig.NodeType type,
                               int y, int rowHeight, int rowIndex) {
        String key = type.name().toLowerCase(Locale.ROOT);
        drawSpecialEntry(graphics, key, nodeSymbol(type), nodeColor(type), y, rowHeight, rowIndex);
    }

    private void drawSpecialEntry(GuiGraphics graphics, String key, String symbol, int color,
                                  int y, int rowHeight, int rowIndex) {
        Minecraft mc = Minecraft.getInstance();
        int left = panelX() + 13;
        int right = panelX() + panelWidth() - 18;
        graphics.fill(left, y, right, y + rowHeight,
                (rowIndex & 1) == 0 ? 0x45D7CBAE : 0x35BDAF91);
        graphics.renderOutline(left, y, right - left, rowHeight, 0x66897451);

        int iconX = left + 16;
        int iconY = y + rowHeight / 2;
        graphics.fill(iconX - 7, iconY - 7, iconX + 8, iconY + 8, 0xFF171E1B);
        graphics.fill(iconX - 5, iconY - 5, iconX + 6, iconY + 6, color);
        graphics.drawCenteredString(mc.font, symbol, iconX, iconY - 4, 0xFFF9F1D5);

        graphics.drawString(mc.font, Component.translatable(PREFIX + "node." + key + ".name"),
                left + 30, y + 4, 0xFF284C43, false);
        List<net.minecraft.util.FormattedCharSequence> description = mc.font.split(
                Component.translatable(PREFIX + "node." + key + ".description"), right - left - 38);
        int textY = y + 15;
        for (net.minecraft.util.FormattedCharSequence line : description) {
            graphics.drawString(mc.font, line, left + 30, textY, 0xFF5B4A35, false);
            textY += 9;
        }
    }

    private void drawScrollbar(GuiGraphics graphics) {
        int maximum = maxScroll();
        if (maximum <= 0) {
            return;
        }
        int trackX = panelX() + panelWidth() - 14;
        int trackHeight = listBottom() - listTop();
        int thumbHeight = Math.max(18, trackHeight * trackHeight / (trackHeight + maximum));
        int thumbY = listTop() + (trackHeight - thumbHeight) * scroll / maximum;
        graphics.fill(trackX, listTop(), trackX + 3, listBottom(), 0x554F4638);
        graphics.fill(trackX - 1, thumbY, trackX + 4, thumbY + thumbHeight, 0xFF8B744B);
    }

    private int rowHeight(LingqiTrajectoryConfig.NodeType type) {
        String key = type.name().toLowerCase(Locale.ROOT);
        return specialRowHeight(key);
    }

    private int specialRowHeight(String key) {
        int lines = Minecraft.getInstance().font.split(
                Component.translatable(PREFIX + "node." + key + ".description"),
                panelWidth() - 69).size();
        return Math.max(30, 20 + lines * 9);
    }

    private int contentHeight() {
        int height = specialRowHeight("difficulty") + ROW_GAP
                + specialRowHeight("realm_examples") + ROW_GAP;
        for (LingqiTrajectoryConfig.NodeType type : NODE_TYPES) {
            height += rowHeight(type) + ROW_GAP;
        }
        height += specialRowHeight("moving_hazard") + ROW_GAP;
        return Math.max(0, height - ROW_GAP);
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - (listBottom() - listTop()));
    }

    private int nodeColor(LingqiTrajectoryConfig.NodeType type) {
        return switch (type) {
            case START -> 0xFF2B9B68;
            case CORE -> 0xFFC48B24;
            case SUB_CORE -> 0xFFB69239;
            case FALSE_CORE -> 0xFF7A622B;
            case BLOCKED -> 0xFF5A2D2B;
            case ELEMENT -> 0xFFB94432;
            case GATHER -> 0xFF297F72;
            case DRAIN -> 0xFF70543D;
            case STABILIZE -> 0xFF477EAA;
            case SPLIT -> 0xFF8A5AA4;
            case REVERSE -> 0xFF9A6A34;
            case UNSTABLE -> 0xFF9A3A3A;
            case CONVERTER, QI_CHANNEL, LINKED -> 0xFF6551B8;
            case TIMED, TIMED_TARGET -> 0xFF8C6FAD;
            case ROTATING -> 0xFF3F8B91;
            case SWITCH -> 0xFF3A8F60;
            case GATE -> 0xFF55443C;
            default -> 0xFF56655B;
        };
    }

    private String nodeSymbol(LingqiTrajectoryConfig.NodeType type) {
        return switch (type) {
            case START -> "S";
            case CORE -> "C";
            case SUB_CORE -> "c";
            case FALSE_CORE -> "?";
            case BLOCKED -> "X";
            case ELEMENT -> "E";
            case GATHER -> "+";
            case DRAIN -> "-";
            case STABILIZE -> "O";
            case SPLIT -> "Y";
            case REVERSE -> "R";
            case UNSTABLE -> "!";
            case CONVERTER -> "T";
            case QI_CHANNEL -> "=";
            case TIMED -> "t";
            case TIMED_TARGET -> "T";
            case ROTATING -> "@";
            case LINKED -> "L";
            case SWITCH -> "K";
            case GATE -> "G";
            default -> ".";
        };
    }

    private int panelWidth() {
        return Math.min(PANEL_WIDTH, Math.max(300, width - 20));
    }

    private int panelHeight() {
        return Math.min(PANEL_HEIGHT, Math.max(230, height - 20));
    }

    private int panelX() {
        return (width - panelWidth()) / 2;
    }

    private int panelY() {
        return (height - panelHeight()) / 2;
    }

    private int listTop() {
        return panelY() + 69;
    }

    private int listBottom() {
        return panelY() + panelHeight() - 34;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
