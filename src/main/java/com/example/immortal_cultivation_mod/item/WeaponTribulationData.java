package com.example.immortal_cultivation_mod.item;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

/** Persistent sequence state; missing items are never recreated from snapshots. */
public final class WeaponTribulationData extends SavedData {
    private static final Factory<WeaponTribulationData> FACTORY = new Factory<>(WeaponTribulationData::new, WeaponTribulationData::load);
    public final Map<UUID, Trial> trials = new LinkedHashMap<>();
    public static final class Trial {
        public UUID id, owner, entity;
        public ResourceLocation dimension;
        public Vec3 position;
        public int originalTier, age, hits, strikes, duration, missingTicks;
        public double power;
        public float damage;
        /** Forced-drop state, cleared by live recovery without resetting the lightning sequence. */
        public boolean groundOnly;
        /** A respawn cannot turn a fatal trial into a heavenly-flame survival reward. */
        public boolean ownerDied;
        public Trial(UUID id, UUID owner, ResourceLocation dimension, Vec3 position) {
            this.id = id; this.owner = owner; this.dimension = dimension; this.position = position;
        }
        CompoundTag save() {
            var t = new CompoundTag(); t.putUUID("id", id); t.putUUID("owner", owner);
            if (entity != null) t.putUUID("entity", entity);
            t.putString("dimension", dimension.toString()); t.putDouble("x", position.x); t.putDouble("y", position.y); t.putDouble("z", position.z);
            t.putInt("tier", originalTier); t.putInt("age", age); t.putInt("hits", hits); t.putInt("strikes", strikes);
            t.putInt("duration", duration); t.putDouble("power", power); t.putFloat("damage", damage); t.putBoolean("groundOnly", groundOnly);
            t.putBoolean("ownerDied", ownerDied);
            t.putInt("damage_version", 2);
            return t;
        }
        static Trial load(CompoundTag t) {
            var trial = new Trial(t.getUUID("id"), t.getUUID("owner"), ResourceLocation.parse(t.getString("dimension")),
                    new Vec3(t.getDouble("x"), t.getDouble("y"), t.getDouble("z")));
            if (t.hasUUID("entity")) trial.entity = t.getUUID("entity");
            trial.originalTier = Math.clamp(t.getInt("tier"), 0, 6); trial.age = Math.max(0, t.getInt("age"));
            trial.strikes = Math.clamp(t.getInt("strikes"), 6, 16); trial.hits = Math.clamp(t.getInt("hits"), 0, trial.strikes);
            trial.duration = Math.clamp(t.getInt("duration"), 200, 2400); trial.power = t.getDouble("power");
            float savedDamage = t.getFloat("damage");
            // Older formulas migrate from saved power and rank; current snapshots never scale twice.
            trial.damage = t.getInt("damage_version") >= 2 && Float.isFinite(savedDamage) && savedDamage > 0
                    ? Math.clamp(savedDamage, 4, WeaponTribulation.MAX_STRIKE_DAMAGE)
                    : WeaponTribulation.strikeDamage(trial.power, trial.originalTier);
            trial.groundOnly = t.getBoolean("groundOnly");
            trial.ownerDied = t.getBoolean("ownerDied");
            return trial;
        }
    }
    public static WeaponTribulationData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "immortal_weapon_tribulations");
    }
    public static WeaponTribulationData load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new WeaponTribulationData();
        for (var entry : tag.getList("trials", Tag.TAG_COMPOUND)) {
            var trial = Trial.load((CompoundTag)entry); data.trials.put(trial.id, trial);
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var list = new ListTag(); trials.values().forEach(t -> list.add(t.save())); tag.put("trials", list); return tag;
    }
}
