package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.NpcClientData;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class NpcRelationshipDebugScreen extends Screen implements NpcDataScreen {
    private static final int WINDOW_WIDTH = 240;
    private static final int WINDOW_HEIGHT = 350;
    private NpcClientData data;

    public NpcRelationshipDebugScreen(NpcClientData data) {
        super(Component.translatable("screen." + ImmortalCultivationMod.MODID + ".npc.debug_relationship.title"));
        this.data = data;
    }

    @Override
    protected void init() {
        clearWidgets();
        int x = (width - WINDOW_WIDTH) / 2;
        int y = (height - WINDOW_HEIGHT) / 2;
        addRowButtons(x, y + 86, "FRIENDSHIP");
        addRowButtons(x, y + 145, "TRUST");
        addRowButtons(x, y + 204, "AFFECTION");
        addRowButtons(x, y + 263, "HATRED");
        addRenderableWidget(new CultivationGuiButton(x + 78, y + 319, 84, 20,
                Component.translatable("gui.done"), this::onClose));
    }

    private void addRowButtons(int x, int y, String stat) {
        addRenderableWidget(new CultivationGuiButton(
                x + 28, y + 16, 40, 20, Component.literal("-10"), () -> adjust(stat, -10)));
        addRenderableWidget(new CultivationGuiButton(
                x + 72, y + 16, 36, 20, Component.literal("-1"), () -> adjust(stat, -1)));
        addRenderableWidget(new CultivationGuiButton(
                x + 112, y + 16, 36, 20, Component.literal("+1"), () -> adjust(stat, 1)));
        addRenderableWidget(new CultivationGuiButton(
                x + 152, y + 16, 40, 20, Component.literal("+10"), () -> adjust(stat, 10)));
    }

    private void adjust(String stat, int delta) {
        PacketDistributor.sendToServer(new ModPayloads.ServerboundNpcActionPayload(
                data.entityId(), "DEBUG_RELATION_" + stat, delta));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = (width - WINDOW_WIDTH) / 2;
        int y = (height - WINDOW_HEIGHT) / 2;
        graphics.fill(0, 0, width, height, 0x66000000);
        CultivationGuiTextures.drawNpc(graphics, x, y, WINDOW_WIDTH, WINDOW_HEIGHT);
        graphics.drawString(font, title, x + (WINDOW_WIDTH - font.width(title)) / 2,
                y + 20, 0xFF2C312D, false);
        graphics.drawString(font, data.name(), x + (WINDOW_WIDTH - font.width(data.name())) / 2,
                y + 47, 0xFF315B50, false);
        drawValue(graphics, x, y + 86, "friendship", data.friendship());
        drawValue(graphics, x, y + 145, "trust", data.trust());
        drawValue(graphics, x, y + 204, "affection", data.affection());
        drawValue(graphics, x, y + 263, "hatred", data.hatred());
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawValue(GuiGraphics graphics, int x, int y, String stat, int value) {
        Component label = Component.translatable(
                "screen." + ImmortalCultivationMod.MODID + ".npc.debug_relationship." + stat,
                value);
        graphics.drawString(font, label, x + 28, y, 0xFF2C312D, false);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public int npcEntityId() {
        return data.entityId();
    }

    @Override
    public void updateNpcData(NpcClientData data) {
        this.data = data;
        if (minecraft != null) {
            init(minecraft, width, height);
        }
    }
}
