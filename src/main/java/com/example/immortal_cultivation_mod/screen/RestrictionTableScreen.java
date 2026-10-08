package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.item.WeaponRestrictionSystem;
import com.example.immortal_cultivation_mod.network.RestrictionPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Arrays;
import java.util.List;

public class RestrictionTableScreen extends AbstractContainerScreen<RestrictionTableMenu> {
    private static final int INK = 0xFF38352E;
    private static final int MUTED_INK = 0xFF756F63;
    private static final int GOLD = 0xFF9B7C43;
    private static final int GOLD_BRIGHT = 0xFFB58A3E;
    private static final int GOLD_DARK = 0xFF7A5A28;
    private static final int JADE = 0xFF315E50;
    private static final int JADE_BRIGHT = 0xFF4A8A71;
    private static final int PARCHMENT = 0xDDEDE7DB;
    private static final int PARCHMENT_ROW = 0xC8F1ECE1;
    private static final int PARCHMENT_HOVER = 0xE8E6E0D4;
    private static final int PARCHMENT_SELECTED = 0xE8D6E2D7;
    private static final int PARCHMENT_BORDER = 0xFFAAA18F;

    private static final int LEFT_PANEL_X = 8;
    private static final int RIGHT_PANEL_X = 218;
    private static final int SIDE_PANEL_WIDTH = 134;
    private static final int[] ROW_Y = {34, 64, 94, 124};
    private static final int LEFT_SIGIL_X = 11;
    private static final int RIGHT_SIGIL_X = 323;
    private static final int SIGIL_SIZE = 26;
    private static final int SIGIL_FRAME_SIZE = 28;
    private static final int LEFT_COUNT_X = 48;
    private static final int RIGHT_COUNT_X = 311;

    // Reuse the existing carved sigil artwork for the new defensive formations.
    private ResourceLocation sigilFor(WeaponRestrictionSystem.RestrictionType type) {
        return sigil(switch (type) {
            case PROTECTION -> "sturdy";
            case FIRE_PROTECTION -> "spirit_devouring";
            case BLAST_PROTECTION -> "armor_breaking";
            case PROJECTILE_PROTECTION -> "sharpness";
            case FEATHER_FALLING -> "swift_flight";
            default -> type.id();
        });
    }

    private List<WeaponRestrictionSystem.RestrictionType> visibleTypes() {
        return WeaponRestrictionSystem.availableTypes(menu.getSlot(0).getItem());
    }

    private final int[] selected = new int[WeaponRestrictionSystem.RestrictionType.values().length];
    private CarvedButton carveButton;
    private CarvedButton evolveButton;

    public RestrictionTableScreen(RestrictionTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = RestrictionTableMenu.IMAGE_WIDTH;
        imageHeight = RestrictionTableMenu.IMAGE_HEIGHT;
        inventoryLabelX = RestrictionTableMenu.PLAYER_INV_X;
        inventoryLabelY = RestrictionTableMenu.PLAYER_INV_Y - 12;
        titleLabelX = 0;
        titleLabelY = 0;
    }

    private static ResourceLocation sigil(String name) {
        return ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID,
                "textures/gui/restriction/" + name + ".png");
    }

    @Override
    protected void init() {
        super.init();
        carveButton = addRenderableWidget(new CarvedButton(leftPos + 143, topPos + 129, 74, 20,
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".restriction_table.carve"),
                () -> PacketDistributor.sendToServer(new RestrictionPayloads.ServerboundStartCarving(
                        Arrays.stream(selected).boxed().toList()))));
        evolveButton = addRenderableWidget(new CarvedButton(leftPos + 143, topPos + 129, 74, 20,
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".restriction_table.evolve"),
                () -> PacketDistributor.sendToServer(new RestrictionPayloads.ServerboundEvolve())));
        refreshButtons();
    }

    private void adjust(int visibleIndex, int direction) {
        var types = visibleTypes();
        if (visibleIndex >= types.size()) return;
        int index = types.get(visibleIndex).ordinal();
        int step = Screen.hasShiftDown() ? 10 : 1;
        if (direction < 0) {
            selected[index] = Math.max(0, selected[index] - step);
        } else {
            int room = Math.max(0, 72 - menu.progress() - selectedTotal());
            selected[index] += Math.min(step, room);
        }
        refreshButtons();
    }

    private int selectedTotal() {
        return Arrays.stream(selected).sum();
    }

    private void refreshButtons() {
        for (var type : WeaponRestrictionSystem.RestrictionType.values()) {
            if (!WeaponRestrictionSystem.canApply(menu.getSlot(0).getItem(), type)) selected[type.ordinal()] = 0;
        }
        if (carveButton != null) {
            carveButton.visible = menu.progress() < 72;
            carveButton.active = !menu.active() && !menu.getSlot(0).getItem().isEmpty()
                    && selectedTotal() > 0 && selectedTotal() <= 72 - menu.progress();
        }
        if (evolveButton != null) {
            evolveButton.visible = menu.progress() >= 72 && menu.tier() < WeaponRestrictionSystem.MAX_TIER;
            evolveButton.active = evolveButton.visible && !menu.active() && !menu.getSlot(0).getItem().isEmpty();
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (menu.progress() >= 72 && selectedTotal() > 0) {
            Arrays.fill(selected, 0);
        }
        refreshButtons();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!menu.active() && (button == 0 || button == 1)) {
            for (int i = 0; i < visibleTypes().size(); i++) {
                if (isOverSigil(i, mouseX, mouseY)) {
                    adjust(i, button == 0 ? 1 : -1);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        CultivationGuiTextures.drawLandscape(graphics, x, y, imageWidth, imageHeight);

        for (int i = 0; i < visibleTypes().size(); i++) {
            renderRestrictionRow(graphics, i, mouseX, mouseY);
        }
        renderCoreRow(graphics, mouseX, mouseY);

        graphics.fill(x + 146, y + 32, x + 214, y + 153, PARCHMENT);
        graphics.renderOutline(x + 146, y + 32, 68, 121, PARCHMENT_BORDER);
        graphics.fill(x + 150, y + 54, x + 210, y + 96, 0xB8DCE5DA);
        graphics.renderOutline(x + 150, y + 54, 60, 42, JADE);
        graphics.fill(x + RestrictionTableMenu.WEAPON_X - 1,
                y + RestrictionTableMenu.WEAPON_Y - 1,
                x + RestrictionTableMenu.WEAPON_X + 17,
                y + RestrictionTableMenu.WEAPON_Y + 17, 0xFFDCD6C9);
        graphics.renderOutline(x + RestrictionTableMenu.WEAPON_X - 2,
                y + RestrictionTableMenu.WEAPON_Y - 2, 20, 20, GOLD);

        int inventoryLeft = x + RestrictionTableMenu.PLAYER_INV_X - 5;
        int inventoryTop = y + RestrictionTableMenu.PLAYER_INV_Y - 17;
        graphics.fill(inventoryLeft, inventoryTop,
                inventoryLeft + 172, inventoryTop + 96, PARCHMENT);
        graphics.renderOutline(inventoryLeft, inventoryTop, 172, 96, GOLD_DARK);
        renderInventorySlots(graphics, x, y);
    }

    private void renderRestrictionRow(GuiGraphics graphics, int index, int mouseX, int mouseY) {
        int row = index < 4 ? index : index - 4;
        int panelX = index < 4 ? LEFT_PANEL_X : RIGHT_PANEL_X;
        int sigilX = index < 4 ? LEFT_SIGIL_X : RIGHT_SIGIL_X;
        int rowY = ROW_Y[row];
        boolean hovered = isOverSigil(index, mouseX, mouseY);
        boolean chosen = selected[visibleTypes().get(index).ordinal()] > 0;
        int background = chosen ? PARCHMENT_SELECTED
                : hovered ? PARCHMENT_HOVER : PARCHMENT_ROW;
        int border = hovered ? GOLD_BRIGHT : chosen ? JADE_BRIGHT : PARCHMENT_BORDER;

        graphics.fill(leftPos + panelX, topPos + rowY,
                leftPos + panelX + SIDE_PANEL_WIDTH, topPos + rowY + 28, background);
        graphics.renderOutline(leftPos + panelX, topPos + rowY, SIDE_PANEL_WIDTH, 28, border);
        graphics.fill(leftPos + sigilX - 1, topPos + rowY,
                leftPos + sigilX + SIGIL_SIZE + 1, topPos + rowY + SIGIL_FRAME_SIZE, background);
        graphics.renderOutline(leftPos + sigilX - 1, topPos + rowY,
                SIGIL_FRAME_SIZE, SIGIL_FRAME_SIZE, border);
        graphics.setColor(0.19F, 0.35F, 0.31F, 1.0F);
        graphics.blit(sigilFor(visibleTypes().get(index)), leftPos + sigilX, topPos + rowY + 1,
                SIGIL_SIZE, SIGIL_SIZE, 0.0F, 0.0F, 128, 128, 128, 128);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        if (menu.active()) {
            graphics.fill(leftPos + panelX, topPos + rowY,
                    leftPos + panelX + SIDE_PANEL_WIDTH, topPos + rowY + 28, 0x559A968C);
        }
    }

    private void renderCoreRow(GuiGraphics graphics, int mouseX, int mouseY) {
        int rowY = ROW_Y[3];
        boolean hovered = isOverCore(mouseX, mouseY);
        int border = hovered ? GOLD_BRIGHT : PARCHMENT_BORDER;
        graphics.fill(leftPos + RIGHT_PANEL_X, topPos + rowY,
                leftPos + RIGHT_PANEL_X + SIDE_PANEL_WIDTH, topPos + rowY + 28,
                PARCHMENT_ROW);
        graphics.renderOutline(leftPos + RIGHT_PANEL_X, topPos + rowY,
                SIDE_PANEL_WIDTH, 28, border);
    }

    private void renderInventorySlots(GuiGraphics graphics, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                renderSlotFrame(graphics,
                        x + RestrictionTableMenu.PLAYER_INV_X + column * 18,
                        y + RestrictionTableMenu.PLAYER_INV_Y + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            renderSlotFrame(graphics,
                    x + RestrictionTableMenu.PLAYER_INV_X + column * 18,
                    y + RestrictionTableMenu.PLAYER_INV_Y + 58);
        }
    }

    private void renderSlotFrame(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF9F9788);
        graphics.fill(x, y, x + 16, y + 16, 0xFFE5DFD3);
        graphics.renderOutline(x - 1, y - 1, 18, 18, 0xFF7E786D);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, (imageWidth - font.width(title)) / 2, 14,
                0xFF2C2A24, false);
        Component tier = Component.translatable("weapon_tier." + ImmortalCultivationMod.MODID + "." + menu.tier());
        drawCenteredText(graphics, tier, imageWidth / 2, 38, GOLD_DARK);
        drawCenteredText(graphics,
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".restriction_table.progress",
                        menu.progress()),
                imageWidth / 2, 105, INK);

        var types = visibleTypes();
        for (int i = 0; i < types.size(); i++) {
            boolean left = i < 4;
            int row = left ? i : i - 4;
            Component name = Component.translatable("screen." + ImmortalCultivationMod.MODID
                    + ".restriction_table.short." + types.get(i).id());
            int nameX = left ? 61 : 221;
            drawFittedText(graphics, name, nameX, ROW_Y[row] + 10, 78,
                    selected[types.get(i).ordinal()] > 0 ? INK : MUTED_INK);
            int countX = left ? LEFT_COUNT_X : RIGHT_COUNT_X;
            drawCenteredText(graphics, Component.literal(Integer.toString(selected[types.get(i).ordinal()])), countX,
                    ROW_Y[row] + 10, selected[types.get(i).ordinal()] > 0 ? JADE : MUTED_INK);
        }

        Component layers = Component.translatable("screen." + ImmortalCultivationMod.MODID
                + ".restriction_table.total_layers");
        drawFittedText(graphics, layers, 238, ROW_Y[3] + 10, 58, MUTED_INK);
        drawCenteredText(graphics, Component.literal(Integer.toString(menu.progress() + selectedTotal())),
                320, ROW_Y[3] + 10, JADE);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY,
                0xFF4B3C29, false);
    }

    private void drawCenteredText(GuiGraphics graphics, Component text, int centerX, int y, int color) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    private void drawFittedText(GuiGraphics graphics, Component text, int x, int y,
                                int maxWidth, int color) {
        int width = font.width(text);
        if (width <= maxWidth) {
            graphics.drawString(font, text, x, y, color, false);
            return;
        }
        float scale = Math.max(0.55F, maxWidth / (float) width);
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, Math.round(x / scale), Math.round(y / scale), color, false);
        graphics.pose().popPose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        for (int i = 0; i < visibleTypes().size(); i++) {
            if (isOverRow(i, mouseX, mouseY)) {
                WeaponRestrictionSystem.RestrictionType type = visibleTypes().get(i);
                graphics.renderTooltip(font, List.of(
                        Component.translatable("restriction." + ImmortalCultivationMod.MODID + "." + type.id())
                                .getVisualOrderText(),
                        Component.translatable("restriction." + ImmortalCultivationMod.MODID + "." + type.id()
                                        + ".description").getVisualOrderText(),
                        Component.translatable("screen." + ImmortalCultivationMod.MODID
                                        + ".restriction_table.click_hint").getVisualOrderText()),
                        mouseX, mouseY);
                break;
            }
        }
        if (isOverCore(mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("screen." + ImmortalCultivationMod.MODID
                    + ".restriction_table.layer_status", menu.progress() + selectedTotal()), mouseX, mouseY);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    private boolean isOverSigil(int index, double mouseX, double mouseY) {
        int row = index < 4 ? index : index - 4;
        int x = leftPos + (index < 4 ? LEFT_SIGIL_X - 1 : RIGHT_SIGIL_X - 1);
        int y = topPos + ROW_Y[row];
        return mouseX >= x && mouseX < x + SIGIL_FRAME_SIZE
                && mouseY >= y && mouseY < y + SIGIL_FRAME_SIZE;
    }

    private boolean isOverRow(int index, double mouseX, double mouseY) {
        int row = index < 4 ? index : index - 4;
        int x = leftPos + (index < 4 ? LEFT_PANEL_X : RIGHT_PANEL_X);
        int y = topPos + ROW_Y[row];
        return mouseX >= x && mouseX < x + SIDE_PANEL_WIDTH
                && mouseY >= y && mouseY < y + 28;
    }

    private boolean isOverCore(double mouseX, double mouseY) {
        int x = leftPos + RIGHT_PANEL_X;
        int y = topPos + ROW_Y[3];
        return mouseX >= x && mouseX < x + SIDE_PANEL_WIDTH
                && mouseY >= y && mouseY < y + 28;
    }

    private static final class CarvedButton extends AbstractButton {
        private final Runnable action;

        private CarvedButton(int x, int y, int width, int height, Component message, Runnable action) {
            super(x, y, width, height, message);
            this.action = action;
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            CultivationGuiTextures.drawButton(graphics, getX(), getY(), width, height,
                    active, isHoveredOrFocused());
            CultivationGuiTextures.drawButtonText(graphics, getMessage(), getX(), getY(),
                    width, height, active);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            defaultButtonNarrationText(narrationElementOutput);
        }
    }
}
