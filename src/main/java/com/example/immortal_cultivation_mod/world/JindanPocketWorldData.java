package com.example.immortal_cultivation_mod.world;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

final class JindanPocketWorldData extends SavedData {
    private static final String DATA_NAME = "immortal_cultivation_jindan_pockets";
    private static final SavedData.Factory<JindanPocketWorldData> FACTORY =
            new SavedData.Factory<>(JindanPocketWorldData::new, JindanPocketWorldData::load);

    private final Map<UUID, Integer> activeGenerations = new HashMap<>();
    private final Set<String> migratedPockets = new HashSet<>();

    static JindanPocketWorldData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    private static JindanPocketWorldData load(CompoundTag tag, HolderLookup.Provider registries) {
        JindanPocketWorldData data = new JindanPocketWorldData();
        CompoundTag generations = tag.getCompound("ActiveGenerations");
        for (String rawUuid : generations.getAllKeys()) {
            try {
                data.activeGenerations.put(UUID.fromString(rawUuid), generations.getInt(rawUuid));
            } catch (IllegalArgumentException ignored) {
            }
        }

        CompoundTag migrated = tag.getCompound("MigratedPockets");
        for (String pocketId : migrated.getAllKeys()) {
            if (migrated.getBoolean(pocketId)) {
                data.migratedPockets.add(pocketId);
            }
        }
        return data;
    }

    Map<UUID, Integer> activeGenerations() {
        return Map.copyOf(activeGenerations);
    }

    void setActiveGeneration(UUID owner, int generation) {
        int normalized = Math.max(0, generation);
        Integer previous = activeGenerations.put(owner, normalized);
        if (previous == null || previous != normalized) {
            setDirty();
        }
    }

    boolean isMigrated(String pocketId) {
        return migratedPockets.contains(pocketId);
    }

    void markMigrated(String pocketId) {
        if (migratedPockets.add(pocketId)) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag generations = new CompoundTag();
        activeGenerations.forEach((owner, generation) ->
                generations.putInt(owner.toString(), generation));
        tag.put("ActiveGenerations", generations);

        CompoundTag migrated = new CompoundTag();
        migratedPockets.forEach(pocketId -> migrated.putBoolean(pocketId, true));
        tag.put("MigratedPockets", migrated);
        return tag;
    }
}
