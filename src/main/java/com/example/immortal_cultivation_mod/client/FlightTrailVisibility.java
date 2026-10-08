package com.example.immortal_cultivation_mod.client;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;

@EventBusSubscriber(modid = ImmortalCultivationMod.MODID, value = Dist.CLIENT)
public final class FlightTrailVisibility {
    private FlightTrailVisibility() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hideHands(RenderHandEvent event) {
        var player = Minecraft.getInstance().player;
        if (player != null && CultivationFlightTrailRenderer.shouldHidePlayer(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hideArms(RenderArmEvent event) {
        if (CultivationFlightTrailRenderer.shouldHidePlayer(event.getPlayer())) {
            event.setCanceled(true);
        }
    }
}
