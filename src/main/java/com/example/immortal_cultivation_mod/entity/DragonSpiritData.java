package com.example.immortal_cultivation_mod.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** One active escape per dragon, retained even when its body or spirit is unloaded. */
final class DragonSpiritData extends SavedData {
    private static final Factory<DragonSpiritData> FACTORY =
            new Factory<>(DragonSpiritData::new, DragonSpiritData::load);
    private record Escape(UUID spirit, boolean resolved) { }
    private final Map<UUID, Escape> escapes = new HashMap<>();

    static DragonSpiritData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, "immortal_dragon_spirits");
    }

    void begin(UUID dragon, UUID spirit) {
        escapes.put(dragon, new Escape(spirit, false));
        setDirty();
    }

    boolean owns(UUID dragon, UUID spirit) {
        // Adopt spirits from older saves, which predate this ownership record.
        if (!escapes.containsKey(dragon)) begin(dragon, spirit);
        Escape escape = escapes.get(dragon);
        return !escape.resolved() && escape.spirit().equals(spirit);
    }

    boolean finish(UUID dragon, UUID spirit) {
        if (!owns(dragon, spirit)) return false;
        escapes.put(dragon, new Escape(spirit, true));
        setDirty();
        return true;
    }

    static DragonSpiritData load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new DragonSpiritData();
        for (var entry : tag.getList("Escapes", 10)) {
            var saved = (CompoundTag) entry;
            if (saved.hasUUID("Dragon") && saved.hasUUID("Spirit"))
                data.escapes.put(saved.getUUID("Dragon"), new Escape(saved.getUUID("Spirit"), saved.getBoolean("Resolved")));
        }
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var list = new ListTag();
        escapes.forEach((dragon, escape) -> {
            var saved = new CompoundTag();
            saved.putUUID("Dragon", dragon);
            saved.putUUID("Spirit", escape.spirit());
            saved.putBoolean("Resolved", escape.resolved());
            list.add(saved);
        });
        tag.put("Escapes", list);
        return tag;
    }
}
