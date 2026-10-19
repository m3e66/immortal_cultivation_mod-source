package com.example.immortal_cultivation_mod.item;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Canonical ownership also reaches weapons transferred while a delayed attack is in flight. */
public final class WeaponBindingData extends SavedData {
    public record Owner(UUID uuid, String name) {}
    private static final Factory<WeaponBindingData> FACTORY = new Factory<>(WeaponBindingData::new, WeaponBindingData::load);
    private final Map<UUID, Owner> owners = new HashMap<>();

    public static WeaponBindingData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "immortal_weapon_bindings");
    }
    public Owner owner(UUID weapon) { return owners.get(weapon); }
    public Owner bind(UUID weapon, Owner proposed) {
        Owner existing = owners.putIfAbsent(weapon, proposed);
        if (existing == null) setDirty();
        return existing == null ? proposed : existing;
    }
    public static WeaponBindingData load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new WeaponBindingData();
        for (var entry : tag.getList("bindings", Tag.TAG_COMPOUND)) {
            var value = (CompoundTag) entry;
            if (value.hasUUID("weapon") && value.hasUUID("owner"))
                data.owners.put(value.getUUID("weapon"), new Owner(value.getUUID("owner"), value.getString("name")));
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var list = new ListTag();
        owners.forEach((weapon, owner) -> {
            var value = new CompoundTag(); value.putUUID("weapon", weapon); value.putUUID("owner", owner.uuid());
            value.putString("name", owner.name()); list.add(value);
        });
        tag.put("bindings", list); return tag;
    }
}
