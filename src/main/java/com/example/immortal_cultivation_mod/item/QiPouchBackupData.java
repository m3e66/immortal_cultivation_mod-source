package com.example.immortal_cultivation_mod.item;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * World-owned recovery copy of every occupied qi-pouch cell.
 *
 * <p>The ticking blocks still live in the dedicated pouch dimension. This
 * second copy makes a pouch recoverable when another mod delays/loses a remote
 * chunk during shutdown or reload, and it also avoids relying solely on a
 * player's inventory save for model-item cells.</p>
 */
final class QiPouchBackupData extends SavedData {
    private static final String DATA_NAME =
            "immortal_cultivation_qi_pouch_backups";
    private static final SavedData.Factory<QiPouchBackupData> FACTORY =
            new SavedData.Factory<>(QiPouchBackupData::new,
                    QiPouchBackupData::load);

    private final Map<UUID, Map<Integer, ItemStack>> pouches =
            new HashMap<>();
    /** Runtime-only: a loaded SavedData instance is new for each server run. */
    private final Set<UUID> recoveryChecked = new HashSet<>();

    static QiPouchBackupData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(FACTORY, DATA_NAME);
    }

    private static QiPouchBackupData load(
            CompoundTag tag, HolderLookup.Provider registries) {
        QiPouchBackupData data = new QiPouchBackupData();
        ListTag pouchEntries = tag.getList("Pouches", Tag.TAG_COMPOUND);
        for (int pouchIndex = 0;
             pouchIndex < pouchEntries.size(); pouchIndex++) {
            CompoundTag pouchEntry = pouchEntries.getCompound(pouchIndex);
            if (!pouchEntry.hasUUID("Pouch")) continue;
            UUID pouchId = pouchEntry.getUUID("Pouch");
            ListTag cellEntries = pouchEntry.getList(
                    "Cells", Tag.TAG_COMPOUND);
            Map<Integer, ItemStack> cells = new HashMap<>();
            for (int cellIndex = 0;
                 cellIndex < cellEntries.size(); cellIndex++) {
                CompoundTag cellEntry = cellEntries.getCompound(cellIndex);
                int cell = cellEntry.getInt("Cell");
                if (cell < 0 || cell >= QiPouchWorld.MAX_CELLS
                        || !cellEntry.contains("Stack")) continue;
                ItemStack stack = ItemStack.parse(
                                registries, cellEntry.get("Stack"))
                        .orElse(ItemStack.EMPTY);
                if (!stack.isEmpty()) {
                    cells.put(cell, stack.copyWithCount(1));
                }
            }
            if (!cells.isEmpty()) data.pouches.put(pouchId, cells);
        }
        return data;
    }

    /** True only on this pouch's first read after a server starts. */
    boolean beginRecovery(UUID pouchId) {
        return recoveryChecked.add(pouchId);
    }

    ItemStack get(UUID pouchId, int cell) {
        Map<Integer, ItemStack> cells = pouches.get(pouchId);
        if (cells == null) return ItemStack.EMPTY;
        ItemStack stack = cells.get(cell);
        return stack == null ? ItemStack.EMPTY : stack.copyWithCount(1);
    }

    void put(UUID pouchId, int cell, ItemStack stack) {
        if (cell < 0 || cell >= QiPouchWorld.MAX_CELLS
                || stack.isEmpty()) return;
        Map<Integer, ItemStack> cells = pouches.computeIfAbsent(
                pouchId, ignored -> new HashMap<>());
        ItemStack stored = stack.copyWithCount(1);
        ItemStack previous = cells.get(cell);
        if (previous == null || !ItemStack.matches(previous, stored)) {
            cells.put(cell, stored);
            setDirty();
        }
    }

    void remove(UUID pouchId, int cell) {
        Map<Integer, ItemStack> cells = pouches.get(pouchId);
        if (cells == null || cells.remove(cell) == null) return;
        if (cells.isEmpty()) pouches.remove(pouchId);
        setDirty();
    }

    @Override
    public CompoundTag save(
            CompoundTag tag, HolderLookup.Provider registries) {
        ListTag pouchEntries = new ListTag();
        pouches.forEach((pouchId, cells) -> {
            if (cells.isEmpty()) return;
            CompoundTag pouchEntry = new CompoundTag();
            pouchEntry.putUUID("Pouch", pouchId);
            ListTag cellEntries = new ListTag();
            cells.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        if (entry.getValue().isEmpty()) return;
                        CompoundTag cellEntry = new CompoundTag();
                        cellEntry.putInt("Cell", entry.getKey());
                        cellEntry.put("Stack",
                                entry.getValue().save(registries));
                        cellEntries.add(cellEntry);
                    });
            if (!cellEntries.isEmpty()) {
                pouchEntry.put("Cells", cellEntries);
                pouchEntries.add(pouchEntry);
            }
        });
        tag.put("Pouches", pouchEntries);
        return tag;
    }
}
