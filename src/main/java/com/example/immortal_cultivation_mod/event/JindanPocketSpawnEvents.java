package com.example.immortal_cultivation_mod.event;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.world.JindanPocketWorlds;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

/** Prevents biome-driven mobs from appearing in temporary Jindan worlds. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class JindanPocketSpawnEvents {
    private JindanPocketSpawnEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void preventAutomaticMobSpawns(MobSpawnEvent.SpawnPlacementCheck event) {
        MobSpawnType spawnType = event.getSpawnType();
        if ((spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION)
                && JindanPocketWorlds.isJindanDimension(
                        event.getLevel().getLevel().dimension())) {
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
        }
    }
}
