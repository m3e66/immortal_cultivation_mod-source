package com.example.immortal_cultivation_mod.entity;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Decoded runtime inventory; the existing NBT remains the authoritative, compatible save format. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class NpcInventoryCache {
    private static final int SIZE = 27;
    // Values never retain the entity. Unloaded NPCs can be collected without retaining their inventory.
    private static final Map<PathfinderMob, Entry> SERVER = new WeakHashMap<>();
    private static final Map<PathfinderMob, Entry> CLIENT = new WeakHashMap<>();
    private record Entry(CompoundTag profile, ListTag snapshot, List<ItemStack> stacks) {}
    private static long decodedStacks, encodedStacks;

    private NpcInventoryCache() {}

    private static Map<PathfinderMob, Entry> cache(PathfinderMob mob) {
        return mob.level().isClientSide ? CLIENT : SERVER;
    }

    private static Entry current(PathfinderMob mob, CompoundTag profile) {
        var entries = cache(mob);
        var cached = entries.get(mob);
        ListTag raw = profile.getList("Inventory", 10);
        // Also detect direct NBT edits, including in-place stack changes made by commands/mods.
        if (cached != null && cached.profile == profile && cached.snapshot.equals(raw)) return cached;
        var stacks = new ArrayList<ItemStack>(SIZE);
        for (int slot = 0; slot < SIZE; slot++) stacks.add(ItemStack.EMPTY);
        for (int i = 0; i < raw.size(); i++) {
            var entry = raw.getCompound(i);
            int slot = entry.getInt("Slot");
            if (slot >= 0 && slot < SIZE && entry.contains("Stack")) {
                stacks.set(slot, ItemStack.parseOptional(mob.registryAccess(), entry.getCompound("Stack")));
                decodedStacks++;
            }
        }
        cached = new Entry(profile, raw.copy(), stacks);
        entries.put(mob, cached);
        return cached;
    }

    /** Keep the former detached-edit contract: callers commit mutations through writeInventory. */
    static synchronized List<ItemStack> read(PathfinderMob mob, CompoundTag profile) {
        return copy(current(mob, profile).stacks);
    }

    static synchronized void write(PathfinderMob mob, CompoundTag profile, List<ItemStack> inventory) {
        var old = current(mob, profile);
        boolean changed = false;
        for (int i = 0; i < SIZE; i++) {
            ItemStack next = i < inventory.size() ? inventory.get(i) : ItemStack.EMPTY;
            if (!ItemStack.matches(old.stacks.get(i), next)) { changed = true; break; }
        }
        if (!changed) return;
        ListTag raw = new ListTag();
        var stacks = new ArrayList<ItemStack>(SIZE);
        for (int slot = 0; slot < SIZE; slot++) {
            ItemStack stack = slot < inventory.size() ? inventory.get(slot).copy() : ItemStack.EMPTY;
            stacks.add(stack);
            if (stack.isEmpty()) continue;
            var entry = new CompoundTag();
            entry.putInt("Slot", slot);
            entry.put("Stack", stack.saveOptional(mob.registryAccess()));
            encodedStacks++;
            raw.add(entry);
        }
        profile.put("Inventory", raw);
        cache(mob).put(mob, new Entry(profile, raw.copy(), stacks));
    }

    private static List<ItemStack> copy(List<ItemStack> stacks) {
        var result = new ArrayList<ItemStack>(SIZE);
        for (ItemStack stack : stacks) result.add(stack.copy());
        return result;
    }

    static synchronized void invalidate(PathfinderMob mob) { cache(mob).remove(mob); }
    static synchronized long decodedStacks() { return decodedStacks; }
    static synchronized long encodedStacks() { return encodedStacks; }

    @SubscribeEvent public static synchronized void stop(ServerStoppedEvent event) {
        SERVER.clear(); CLIENT.clear(); decodedStacks = 0; encodedStacks = 0;
    }
}
