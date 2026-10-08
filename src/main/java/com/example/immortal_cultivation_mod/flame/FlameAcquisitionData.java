package com.example.immortal_cultivation_mod.flame;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** One-time natural flame opportunities, including unsuccessful rare rolls. */
final class FlameAcquisitionData extends SavedData {
    private static final Factory<FlameAcquisitionData> FACTORY =
            new Factory<>(FlameAcquisitionData::new, FlameAcquisitionData::load);
    private final Set<String> claims = new HashSet<>();

    static FlameAcquisitionData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, "immortal_flame_sources");
    }

    boolean claim(String source) {
        if (!claims.add(source)) return false;
        setDirty();
        return true;
    }

    boolean contains(String source) { return claims.contains(source); }

    static FlameAcquisitionData load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new FlameAcquisitionData();
        for (var entry : tag.getList("Claims", 8)) data.claims.add(entry.getAsString());
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var list = new ListTag();
        claims.stream().sorted().forEach(source -> list.add(StringTag.valueOf(source)));
        tag.put("Claims", list);
        return tag;
    }
}
