package com.example.immortal_cultivation_mod.network;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.UUID;

/** One screen generation. Partial, duplicate and stale transfers cannot replace a visible catalog. */
public final class ShopCatalogTransfer {
    private UUID id;
    private int total, received;
    private BitSet parts;
    private ShopCatalogPayloads.Entry[] entries;
    private List<ShopCatalogPayloads.Entry> completed;

    public void begin(UUID transferId, int size) {
        ShopCatalogPayloads.partCount(size);
        if (transferId.equals(id) && size == total) return;
        id = transferId; total = size; received = 0;
        parts = new BitSet(ShopCatalogPayloads.partCount(size));
        entries = new ShopCatalogPayloads.Entry[size]; completed = null;
    }

    public boolean accept(ShopCatalogPayloads.CatalogPart part) {
        if (!matches(part.transferId()) || part.total() != total || completed != null || parts.get(part.index())) return false;
        for (int i = 0; i < part.entries().size(); i++) entries[part.index() * ShopCatalogPayloads.PART_SIZE + i] = part.entries().get(i);
        parts.set(part.index()); received++;
        if (received != ShopCatalogPayloads.partCount(total)) return false;
        completed = List.copyOf(Arrays.asList(entries)); entries = null;
        return true;
    }

    public boolean matches(UUID transferId) { return transferId != null && transferId.equals(id); }
    public boolean complete() { return completed != null; }
    public List<ShopCatalogPayloads.Entry> entries() { return completed == null ? List.of() : completed; }
    public void clear() { id = null; parts = null; entries = null; completed = null; total = received = 0; }
}
