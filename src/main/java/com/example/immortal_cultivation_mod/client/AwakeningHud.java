package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.screen.EnlightenmentSelectionScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** Keep gameplay HUD out of the full-screen awakening stage. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID, value = Dist.CLIENT)
public final class AwakeningHud {
    private AwakeningHud() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void render(RenderGuiEvent.Pre event) {
        if (Minecraft.getInstance().screen instanceof EnlightenmentSelectionScreen
                || Minecraft.getInstance().screen instanceof com.example.immortal_cultivation_mod.screen.HudLayoutScreen) {
            event.setCanceled(true);
        }
    }
}
