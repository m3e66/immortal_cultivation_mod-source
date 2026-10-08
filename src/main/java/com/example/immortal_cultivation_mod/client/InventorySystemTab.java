package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.network.OuterSystemPayloads;
import com.example.immortal_cultivation_mod.guidance.DaoGuidanceContent;
import com.example.immortal_cultivation_mod.screen.SystemGuiTextures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.common.inventory.CurioSlot;

public final class InventorySystemTab {
    private static final ResourceLocation SYSTEM_ICON =
            ResourceLocation.fromNamespaceAndPath(
                    ImmortalCultivationMod.MODID,
                    "textures/item/system.png");
    private static final int TAB_SIZE = 18;
    private static final int ICON_SIZE = 12;

    private InventorySystemTab() {
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen inventory)
                || !ClientData.outerSystemEquipped()) {
            return;
        }
        Component label = Component.translatable(
                "screen.immortal_cultivation_mod.system.inventory_tab");
        SystemTabButton button = new SystemTabButton(
                inventory.getGuiLeft() - TAB_SIZE + 1,
                inventory.getGuiTop() + 7,
                label);
        button.setTooltip(Tooltip.create(label));
        event.addListener(button);
    }

    /** Points at both vanilla armor and the real Curios clothing/accessory slots. */
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!DaoGuidanceClient.isCurrentAction(
                DaoGuidanceContent.EQUIP_ITEM)
                || !(event.getScreen()
                instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        int pulse = 145 + (int) (Math.sin(
                System.currentTimeMillis() / 180.0D) * 65.0D);
        int color = (Math.max(70, Math.min(230, pulse)) << 24)
                | 0xF0C66B;
        for (int index = 0; index < screen.getMenu().slots.size(); index++) {
            var slot = screen.getMenu().slots.get(index);
            boolean vanillaArmor = screen instanceof InventoryScreen
                    && index >= 5 && index <= 8;
            if (!vanillaArmor && !(slot instanceof CurioSlot)
                    || !slot.isActive()) {
                continue;
            }
            event.getGuiGraphics().renderOutline(
                    screen.getGuiLeft() + slot.x - 2,
                    screen.getGuiTop() + slot.y - 2,
                    20, 20, color);
        }
    }

    private static final class SystemTabButton extends AbstractButton {
        private SystemTabButton(int x, int y, Component message) {
            super(x, y, TAB_SIZE, TAB_SIZE, message);
        }

        @Override
        public void onPress() {
            PacketDistributor.sendToServer(
                    new OuterSystemPayloads.ServerboundOpen());
        }

        @Override
        protected void renderWidget(
                GuiGraphics graphics,
                int mouseX,
                int mouseY,
                float partialTick) {
            SystemGuiTextures.drawButton(
                    graphics,
                    getX(),
                    getY(),
                    width,
                    height,
                    active,
                    isHoveredOrFocused());
            graphics.blit(
                    SYSTEM_ICON,
                    getX() + (width - ICON_SIZE) / 2,
                    getY() + (height - ICON_SIZE) / 2,
                    ICON_SIZE,
                    ICON_SIZE,
                    0.0F,
                    0.0F,
                    64,
                    64,
                    64,
                    64);
        }

        @Override
        protected void updateWidgetNarration(
                NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
