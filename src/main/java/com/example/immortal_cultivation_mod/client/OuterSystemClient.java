package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.screen.SystemBootScreen;
import com.example.immortal_cultivation_mod.screen.LifeSimulationScreen;
import com.example.immortal_cultivation_mod.screen.LifeEncounterScreen;
import com.example.immortal_cultivation_mod.screen.LifePlanningScreen;
import com.example.immortal_cultivation_mod.screen.LifeCardBattleScreen;
import com.example.immortal_cultivation_mod.screen.SystemScreen;
import com.example.immortal_cultivation_mod.network.OuterSystemPayloads;
import com.example.immortal_cultivation_mod.network.ShopCatalogPayloads;
import com.example.immortal_cultivation_mod.network.ShopCatalogTransfer;
import com.example.immortal_cultivation_mod.system.OuterSystemType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class OuterSystemClient {
    private static int transitionDepth;
    private static LifeSimulationScreen confirmationParent;
    private static Screen confirmationScreen;
    private static final ShopCatalogTransfer shopCatalog = new ShopCatalogTransfer();

    private OuterSystemClient() {
    }

    /** Internal life-system navigation keeps the same server screen subscription. */
    public static void transitionTo(Screen screen) {
        if (screen != confirmationScreen) {
            confirmationParent = null;
            confirmationScreen = null;
        }
        transitionDepth++;
        try {
            Minecraft.getInstance().setScreen(screen);
        } finally {
            transitionDepth--;
        }
    }

    public static boolean isTransitioning() {
        return transitionDepth > 0;
    }

    /** Periodic state updates must not dismiss a pending settlement confirmation. */
    public static void showLifeConfirmation(LifeSimulationScreen parent, Runnable confirmed) {
        confirmationParent = parent;
        confirmationScreen = new ConfirmScreen(answer -> {
            transitionTo(parent);
            if (answer) confirmed.run();
        }, Component.translatable("life.expansion.settle.confirm"),
                Component.translatable("life.expansion.settle.warning")) {
            @Override
            public void onClose() {
                transitionTo(parent);
            }

            @Override
            public void removed() {
                if (!isTransitioning() && Minecraft.getInstance().player != null) {
                    PacketDistributor.sendToServer(new OuterSystemPayloads.ServerboundClose());
                }
                super.removed();
            }
        };
        transitionTo(confirmationScreen);
    }

    public static void sync(String type, boolean equipped) {
        boolean typeChanged = ClientData.outerSystemEquipped()
                && !ClientData.outerSystemType().equals(type);
        ClientData.updateOuterSystem(type, equipped);
        if (!equipped || !OuterSystemType.SYSTEM_SHOP.id().equals(type)) clearShopCatalog();
        Minecraft minecraft = Minecraft.getInstance();
        if ((!equipped || typeChanged) && (minecraft.screen instanceof SystemScreen
                || minecraft.screen instanceof LifeSimulationScreen
                || minecraft.screen instanceof LifeEncounterScreen
                || minecraft.screen instanceof LifeCardBattleScreen
                || minecraft.screen instanceof LifePlanningScreen
                || minecraft.screen instanceof SystemBootScreen
                || minecraft.screen == confirmationScreen && confirmationScreen != null)) {
            if (minecraft.player != null) {
                PacketDistributor.sendToServer(new OuterSystemPayloads.ServerboundClose());
            }
            transitionTo(null);
        }
    }

    public static void openBoot(String type) {
        Minecraft.getInstance().setScreen(new SystemBootScreen(type));
    }

    public static void openOrUpdate(CompoundTag state) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean currentSystemScreen = minecraft.screen instanceof SystemScreen
                || minecraft.screen instanceof LifeSimulationScreen
                || minecraft.screen instanceof LifeEncounterScreen
                || minecraft.screen instanceof LifeCardBattleScreen
                || minecraft.screen instanceof LifePlanningScreen
                || minecraft.screen instanceof SystemBootScreen
                || minecraft.screen == confirmationScreen && confirmationScreen != null;
        if (state.contains("OpenRequested") && !state.getBoolean("OpenRequested") && !currentSystemScreen) {
            return;
        }
        if (OuterSystemType.SYSTEM_SHOP.id().equals(state.getString("Type")) && state.hasUUID("ShopCatalogId")) {
            shopCatalog.begin(state.getUUID("ShopCatalogId"), state.getInt("ShopCatalogSize"));
        } else {
            clearShopCatalog();
        }
        if (OuterSystemType.LIFE_SIMULATION.id().equals(state.getString("Type"))) {
            if (minecraft.screen == confirmationScreen && confirmationParent != null) {
                confirmationParent.updateState(state);
                return;
            }
            var life = state.getCompound("LifeSimulation");
            if (minecraft.screen instanceof LifePlanningScreen screen && screen.isJournal()
                    && life.getBoolean("Alive") && !life.getBoolean("Finished")) {
                // Reading completed entries never advances or resolves a pending encounter.
                screen.updateState(state);
            } else if (!life.getBoolean("Finished") && (life.getBoolean("Alive") || life.getBoolean("CombatDead") && life.contains("Battle")) && (life.contains("Story") || life.contains("Battle") || !life.getCompound("CurrentEvent").isEmpty())) {
                if(com.example.immortal_cultivation_mod.system.LifeCardBattle.enabled(life.getCompound("Battle"))){
                    if(minecraft.screen instanceof LifeCardBattleScreen screen)screen.updateState(state);
                    else transitionTo(new LifeCardBattleScreen(state));
                }else if (minecraft.screen instanceof LifeEncounterScreen screen) screen.updateState(state);
                else transitionTo(new LifeEncounterScreen(state));
            } else if (minecraft.screen instanceof LifePlanningScreen screen) {
                if (!life.getBoolean("Alive") || life.getBoolean("Finished")) {
                    transitionTo(new LifeSimulationScreen(state));
                } else screen.updateState(state);
            } else if (minecraft.screen instanceof LifeSimulationScreen screen) {
                screen.updateState(state);
            } else {
                transitionTo(new LifeSimulationScreen(state));
            }
        } else if (minecraft.screen instanceof SystemScreen screen) {
            screen.updateState(state);
        } else {
            transitionTo(new SystemScreen(state));
        }
    }

    public static void receiveShopCatalog(ShopCatalogPayloads.CatalogPart part) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof SystemScreen screen) || !screen.acceptsShopTransfer(part.transferId())) return;
        if (shopCatalog.accept(part)) screen.installShopCatalog(shopCatalog.entries());
    }

    public static void receiveShopStock(ShopCatalogPayloads.StockUpdate update) {
        Minecraft minecraft = Minecraft.getInstance();
        if (shopCatalog.complete() && shopCatalog.matches(update.transferId())
                && minecraft.screen instanceof SystemScreen screen && screen.acceptsShopTransfer(update.transferId()))
            screen.applyShopStock(update.entries());
    }

    public static void clearShopCatalog() { shopCatalog.clear(); }
}
