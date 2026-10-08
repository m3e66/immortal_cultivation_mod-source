package com.example.immortal_cultivation_mod.event;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.worldgen.HotelStructureManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Loads the keeper belonging to a hotel when a player enters that hotel. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class HotelStructureEvents {
    private HotelStructureEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.tickCount % 20 == 0) {
            HotelStructureManager.ensureHotelkeeperNear(player);
        }
    }
}
