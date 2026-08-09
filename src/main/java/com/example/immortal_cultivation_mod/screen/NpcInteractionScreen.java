package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.NpcClientData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class NpcInteractionScreen extends AbstractNpcScreen {
    public NpcInteractionScreen(NpcClientData data) {
        super(data.withoutStatus(), Component.translatable("screen." + ImmortalCultivationMod.MODID + ".npc.title"));
    }

    @Override
    protected void buildContent() {
        int x = contentX();
        int y = contentY() + 22;
        int gap = 8;
        int buttonWidth = (contentWidth() - gap) / 2;
        int buttonHeight = 25;

        addPageButton(x, y, buttonWidth, buttonHeight, "talk", NpcOptionScreen.Page.TALK);
        addPageButton(x + buttonWidth + gap, y, buttonWidth, buttonHeight, "interact", NpcOptionScreen.Page.INTERACT);
        addPageButton(x, y + 32, buttonWidth, buttonHeight, "trade", NpcOptionScreen.Page.TRADE);
        addPageButton(x + buttonWidth + gap, y + 32, buttonWidth, buttonHeight, "quest", NpcOptionScreen.Page.QUEST);
        Component travelLabel = Component.translatable("screen." + ImmortalCultivationMod.MODID
                + ("following".equals(data.commandMode()) ? ".npc.travel.stop" : ".npc.action.travel"));
        addNpcButton(x, y + 64, buttonWidth, buttonHeight, travelLabel, () -> sendAction("TOGGLE_FOLLOW"));
        addNpcButton(x + buttonWidth + gap, y + 64, buttonWidth, buttonHeight,
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".npc.action.stay"),
                () -> sendAction("STAY"));
        addPageButton(x, y + 96, buttonWidth, buttonHeight, "teach", NpcOptionScreen.Page.TEACH);
        addPageButton(x + buttonWidth + gap, y + 96, buttonWidth, buttonHeight, "inventory", NpcOptionScreen.Page.INVENTORY);
        addPageButton(x, y + 128, contentWidth(), buttonHeight, "about", NpcOptionScreen.Page.ABOUT);
    }

    private void addPageButton(int x, int y, int width, int height, String key, NpcOptionScreen.Page page) {
        addNpcButton(x, y, width, height,
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".npc.action." + key),
                () -> minecraft.setScreen(new NpcOptionScreen(data, page)));
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        drawSectionTitle(graphics,
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".npc.choose_action"), contentY());
        int relationY = contentY() + 180;
        graphics.drawString(font,
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".npc.relation_summary",
                        data.friendship(), data.trust(), data.affection(), data.hatred()),
                contentX(), relationY, COLOR_MUTED, false);
        graphics.drawString(font,
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".npc.command." + data.commandMode()),
                contentX(), relationY + 13, COLOR_GREEN, false);
    }

    @Override
    protected boolean shouldRenderStatus(String statusKey) {
        return statusKey.equals("npc.status.following")
                || statusKey.equals("npc.status.not_following")
                || statusKey.equals("npc.status.staying");
    }

    @Override
    public void onClose() {
        closeInteraction();
    }
}
