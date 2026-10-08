package com.example.immortal_cultivation_mod.screen;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.client.DaoGuidanceClient;
import com.example.immortal_cultivation_mod.client.NpcClientData;
import com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class NpcInteractionScreen extends AbstractNpcScreen {
    private NpcButton talkButton;
    private NpcButton interactButton;
    private NpcButton tradeButton;
    private NpcButton questButton;

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

        talkButton = addPageButton(x, y, buttonWidth, buttonHeight,
                "talk", NpcOptionScreen.Page.TALK);
        interactButton = addPageButton(x + buttonWidth + gap, y,
                buttonWidth, buttonHeight,
                "interact", NpcOptionScreen.Page.INTERACT);
        tradeButton = addPageButton(x, y + 32, buttonWidth, buttonHeight,
                "trade", NpcOptionScreen.Page.TRADE);
        questButton = addPageButton(x + buttonWidth + gap, y + 32,
                buttonWidth, buttonHeight, "quest", NpcOptionScreen.Page.QUEST);
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

    private NpcButton addPageButton(int x, int y, int width, int height, String key, NpcOptionScreen.Page page) {
        return addNpcButton(x, y, width, height,
                Component.translatable("screen." + ImmortalCultivationMod.MODID + ".npc.action." + key),
                () -> minecraft.setScreen(new NpcOptionScreen(data, page)));
    }

    @Override
    protected void renderForeground(
            GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        NpcButton target = DaoGuidanceClient.isCurrentAction(
                DaoGuidanceContent.TALK_NPC) ? talkButton
                : DaoGuidanceClient.isCurrentAction(
                DaoGuidanceContent.WEAPON_ATTACK) ? interactButton
                : DaoGuidanceClient.isCurrentAction(
                DaoGuidanceContent.TRADE_NPC) ? tradeButton
                : DaoGuidanceClient.isCurrentAction(
                DaoGuidanceContent.ACCEPT_QUEST) ? questButton : null;
        if (target == null) return;
        int pulse = 145 + (int) (Math.sin(
                System.currentTimeMillis() / 180.0D) * 65.0D);
        int color = (Math.max(70, Math.min(230, pulse)) << 24)
                | 0xF0C66B;
        graphics.renderOutline(target.getX() - 3, target.getY() - 3,
                target.getWidth() + 6, target.getHeight() + 6, color);
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
