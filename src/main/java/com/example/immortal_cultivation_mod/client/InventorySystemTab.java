package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.network.OuterSystemPayloads;
import com.example.immortal_cultivation_mod.screen.SystemGuiTextures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

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
