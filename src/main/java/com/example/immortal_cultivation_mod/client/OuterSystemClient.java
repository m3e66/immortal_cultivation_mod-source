package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.screen.SystemBootScreen;
import com.example.immortal_cultivation_mod.screen.LifeSimulationScreen;
import com.example.immortal_cultivation_mod.screen.SystemScreen;
import com.example.immortal_cultivation_mod.system.OuterSystemType;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

public final class OuterSystemClient {
    private OuterSystemClient() {
    }

    public static void sync(String type, boolean equipped) {
        ClientData.updateOuterSystem(type, equipped);
    }

    public static void openBoot(String type) {
        Minecraft.getInstance().setScreen(new SystemBootScreen(type));
    }

    public static void openOrUpdate(CompoundTag state) {
        Minecraft minecraft = Minecraft.getInstance();
        if (OuterSystemType.LIFE_SIMULATION.id().equals(state.getString("Type"))) {
            if (minecraft.screen instanceof LifeSimulationScreen screen) {
                screen.updateState(state);
            } else {
                minecraft.setScreen(new LifeSimulationScreen(state));
            }
        } else if (minecraft.screen instanceof SystemScreen screen) {
            screen.updateState(state);
        } else {
            minecraft.setScreen(new SystemScreen(state));
        }
    }
}
