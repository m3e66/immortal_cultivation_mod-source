package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.entity.NpcSkinCatalog;
import com.example.immortal_cultivation_mod.item.DisguiseMaskItem;
import com.example.immortal_cultivation_mod.network.DisguiseMaskPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/** Portrait grid used to choose the humanoid NPC appearance stored by a mask. */
public final class DisguiseMaskSkinScreen extends Screen {
    private static final int MAX_COLUMNS = 6;
    private static final int MIN_COLUMNS = 3;
    private static final int CARD_WIDTH = 58;
    private static final int CARD_HEIGHT = 52;
    private static final int CARD_GAP = 6;
    private static final int HEADER_HEIGHT = 40;
    private static final int FOOTER_HEIGHT = 28;
    private static final float LANDSCAPE_TITLE_CENTER_Y = 66.0F;

    private final boolean mainHand;
    private String selectedSkin;
    private int columns;
    private int rows;
    private int windowWidth;
    private int windowHeight;
    private int left;
    private int top;

    public DisguiseMaskSkinScreen(
            boolean mainHand, String selectedSkin) {
        super(Component.translatable(
                "screen." + ImmortalCultivationMod.MODID
                        + ".disguise_mask_skins"));
        this.mainHand = mainHand;
        this.selectedSkin = selectedSkin;
    }

    @Override
    protected void init() {
        clearWidgets();
        int availableColumns = Math.max(
                MIN_COLUMNS,
                (width - 36 + CARD_GAP)
                        / (CARD_WIDTH + CARD_GAP));
        columns = Math.min(MAX_COLUMNS, availableColumns);
        rows = (NpcSkinCatalog.ALL_SKINS.size() + columns - 1)
                / columns;

        int gridWidth = columns * CARD_WIDTH
                + (columns - 1) * CARD_GAP;
        int gridHeight = rows * CARD_HEIGHT
                + (rows - 1) * CARD_GAP;
        windowWidth = gridWidth + 28;
        windowHeight = HEADER_HEIGHT + gridHeight + FOOTER_HEIGHT;
        left = (width - windowWidth) / 2;
        top = (height - windowHeight) / 2;

        int gridX = left + (windowWidth - gridWidth) / 2;
        int gridY = top + HEADER_HEIGHT;
        for (int index = 0;
             index < NpcSkinCatalog.ALL_SKINS.size();
             index++) {
            NpcSkinCatalog.Skin skin =
                    NpcSkinCatalog.ALL_SKINS.get(index);
            int column = index % columns;
            int row = index / columns;
            addRenderableWidget(new SkinButton(
                    gridX + column * (CARD_WIDTH + CARD_GAP),
                    gridY + row * (CARD_HEIGHT + CARD_GAP),
                    skin,
                    skin.texture().equals(selectedSkin),
                    button -> chooseSkin(skin.texture())));
        }
    }

    private void chooseSkin(String skin) {
        selectedSkin = skin;
        PacketDistributor.sendToServer(
                new DisguiseMaskPayloads.ServerboundSelectSkin(
                        mainHand, skin));
        onClose();
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick) {
        graphics.fill(0, 0, width, height, 0x9A0A1110);
        CultivationGuiTextures.drawLandscape(
                graphics, left, top, windowWidth, windowHeight);

        int gridHeight = rows * CARD_HEIGHT
                + (rows - 1) * CARD_GAP;
        CultivationGuiTextures.drawInnerBox(
                graphics,
                left + 8,
                top + HEADER_HEIGHT - 7,
                windowWidth - 16,
                gridHeight + 14);

        int titleCenterY = top + Math.round(
                windowHeight * LANDSCAPE_TITLE_CENTER_Y
                        / CultivationGuiTextures.LANDSCAPE_HEIGHT);
        graphics.drawCenteredString(
                font,
                title,
                left + windowWidth / 2,
                titleCenterY - font.lineHeight / 2,
                0xFF3D3020);
        graphics.drawCenteredString(
                font,
                Component.translatable(
                        "screen." + ImmortalCultivationMod.MODID
                                + ".disguise_mask_skins.hint"),
                left + windowWidth / 2,
                top + windowHeight - 18,
                0xFF675A45);

        super.render(graphics, mouseX, mouseY, partialTick);
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

    private static final class SkinButton extends Button {
        private static final int FACE_SIZE = 31;

        private final ResourceLocation skinTexture;
        private final boolean selected;

        private SkinButton(
                int x,
                int y,
                NpcSkinCatalog.Skin skin,
                boolean selected,
                OnPress onPress) {
            super(
                    x,
                    y,
                    CARD_WIDTH,
                    CARD_HEIGHT,
                    Component.translatable(
                            DisguiseMaskItem.skinNameKey(
                                    skin.texture())),
                    onPress,
                    DEFAULT_NARRATION);
            this.skinTexture = NpcSkinCatalog.texture(skin.texture());
            this.selected = selected;
        }

        @Override
        protected void renderWidget(
                GuiGraphics graphics,
                int mouseX,
                int mouseY,
                float partialTick) {
            int x = getX();
            int y = getY();
            int border = selected
                    ? 0xFFD5A63A
                    : (isHoveredOrFocused()
                            ? 0xFF4D998B
                            : 0xFF756A57);
            int background = selected
                    ? 0xE8FFF4D2
                    : 0xDDE8E1D2;

            graphics.fill(
                    x, y, x + width, y + height, background);
            graphics.renderOutline(
                    x, y, width, height, border);
            if (selected) {
                graphics.renderOutline(
                        x + 2, y + 2,
                        width - 4, height - 4,
                        0xFFB17B24);
                graphics.fill(
                        x + width - 7, y + 3,
                        x + width - 3, y + 7,
                        0xFFD5A63A);
            }

            int faceX = x + (width - FACE_SIZE) / 2;
            int faceY = y + 4;
            graphics.fill(
                    faceX - 1,
                    faceY - 1,
                    faceX + FACE_SIZE + 1,
                    faceY + FACE_SIZE + 1,
                    0xFF342F29);
            graphics.blit(
                    skinTexture,
                    faceX,
                    faceY,
                    FACE_SIZE,
                    FACE_SIZE,
                    8.0F,
                    8.0F,
                    8,
                    8,
                    64,
                    64);
            graphics.blit(
                    skinTexture,
                    faceX,
                    faceY,
                    FACE_SIZE,
                    FACE_SIZE,
                    40.0F,
                    8.0F,
                    8,
                    8,
                    64,
                    64);

            Minecraft minecraft = Minecraft.getInstance();
            Component label = getMessage();
            int textWidth = minecraft.font.width(label);
            int availableWidth = width - 4;
            if (textWidth <= availableWidth) {
                graphics.drawCenteredString(
                        minecraft.font,
                        label,
                        x + width / 2,
                        y + height - 11,
                        0xFF2F2A22);
            } else {
                float scale = Math.max(
                        0.62F,
                        availableWidth / (float) textWidth);
                graphics.pose().pushPose();
                graphics.pose().scale(scale, scale, 1.0F);
                int textX = Math.round(
                        (x + width / 2.0F) / scale
                                - textWidth / 2.0F);
                int textY = Math.round(
                        (y + height - 11) / scale);
                graphics.drawString(
                        minecraft.font,
                        label,
                        textX,
                        textY,
                        0xFF2F2A22,
                        false);
                graphics.pose().popPose();
            }
        }
    }
}
