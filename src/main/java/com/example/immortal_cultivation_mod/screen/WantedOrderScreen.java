package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class WantedOrderScreen extends Screen {
    private static final ResourceLocation POSTER_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ImmortalCultivationMod.MODID, "textures/item/wanted_order.png");
    private static final int POSTER_SOURCE_X = 27;
    private static final int POSTER_SOURCE_Y = 18;
    private static final int POSTER_SOURCE_WIDTH = 60;
    private static final int POSTER_SOURCE_HEIGHT = 72;
    private static final int POSTER_WIDTH = POSTER_SOURCE_WIDTH * 3;
    private static final int POSTER_HEIGHT = POSTER_SOURCE_HEIGHT * 3;

    private final ResourceLocation skin;
    private final String wantedName;
    private final String realm;
    private final Component reward;
    private final int rewardCount;

    public WantedOrderScreen(String skinId, String wantedName, String realm, String rewardKey, int rewardCount) {
        super(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".wanted_order"));
        this.skin = ResourceLocation.fromNamespaceAndPath(
                ImmortalCultivationMod.MODID, "textures/entity/" + skinId + ".png");
        this.wantedName = wantedName;
        this.realm = realm;
        this.reward = Component.translatable(rewardKey);
        this.rewardCount = rewardCount;
    }

    @Override
    protected void init() {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int left = (width - POSTER_WIDTH) / 2;
        int top = (height - POSTER_HEIGHT) / 2;

        graphics.fill(0, 0, width, height, 0x66000000);
        graphics.blit(POSTER_TEXTURE, left, top, POSTER_WIDTH, POSTER_HEIGHT,
                POSTER_SOURCE_X, POSTER_SOURCE_Y, POSTER_SOURCE_WIDTH, POSTER_SOURCE_HEIGHT,
                110, 108);

        int portraitSize = 98;
        int portraitX = width / 2 - portraitSize / 2;
        int portraitY = top + 66;
        graphics.blit(skin, portraitX, portraitY, portraitSize, portraitSize,
                8.0F, 8.0F, 8, 8, 64, 64);
        graphics.blit(skin, portraitX, portraitY, portraitSize, portraitSize,
                40.0F, 8.0F, 8, 8, 64, 64);

        graphics.drawCenteredString(
                font,
                Component.literal(wantedName + "/" + realm),
                width / 2,
                top + 170,
                0xFF2C1710
        );
        graphics.drawCenteredString(font, Component.translatable(
                "screen." + ImmortalCultivationMod.MODID + ".wanted_reward", reward, rewardCount),
                width / 2, top + 200, 0xFF7A1717);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 || keyCode == 69) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
