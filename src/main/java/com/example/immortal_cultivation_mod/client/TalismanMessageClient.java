package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.screen.TalismanMessageScreen;
import net.minecraft.client.Minecraft;

public final class TalismanMessageClient {
    private TalismanMessageClient() {
    }

    public static void open(boolean longRange) {
        Minecraft.getInstance().setScreen(
                new TalismanMessageScreen(longRange));
    }
}
