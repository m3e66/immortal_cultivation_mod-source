package com.example.immortal_cultivation_mod.system;

import com.example.immortal_cultivation_mod.network.ShopCatalogPayloads;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server snapshots and stock deltas, scoped to the currently open shop. */
final class SystemShopSync {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private SystemShopSync() {}

    static Update prepare(ServerPlayer player, boolean explicitOpen, CompoundTag state) {
        List<SystemShop.CatalogEntry> catalog = SystemShop.catalog();
        CompoundTag shop = OuterSystemData.read(player).getCompound("Shop");
        CompoundTag purchases = shop.getCompound("Purchases");
        Set<String> unique = new HashSet<>();
        for (var tag : shop.getList("UniqueBought", Tag.TAG_STRING)) unique.add(tag.getAsString());
        long day = shop.getLong("Day");
        Session session = SESSIONS.get(player.getUUID());
        boolean replace = explicitOpen || session == null || session.catalog != catalog || session.day != day;
        List<ShopCatalogPayloads.Entry> entries = List.of();
        List<ShopCatalogPayloads.Stock> stocks = new ArrayList<>();
        if (replace) {
            ShopCatalogPayloads.partCount(catalog.size());
            session = new Session(UUID.randomUUID(), catalog, day);
            SESSIONS.put(player.getUUID(), session);
            List<ShopCatalogPayloads.Entry> snapshot = new ArrayList<>(catalog.size());
            for (var entry : catalog) snapshot.add(new ShopCatalogPayloads.Entry(
                    BuiltInRegistries.ITEM.getId(entry.item()), entry.price(), entry.stockTier().ordinal(), entry.category().ordinal(),
                    Math.max(0, purchases.getInt(entry.id())), unique.contains(entry.id())));
            entries = List.copyOf(snapshot);
        } else if (!purchases.equals(session.purchases) || !unique.equals(session.unique)) {
            Set<String> changed = new HashSet<>(purchases.getAllKeys());
            changed.addAll(session.purchases.getAllKeys()); changed.addAll(unique); changed.addAll(session.unique);
            for (String id : changed) {
                int count = Math.max(0, purchases.getInt(id));
                boolean bought = unique.contains(id);
                if (count == session.purchases.getInt(id) && bought == session.unique.contains(id)) continue;
                var entry = SystemShop.catalogEntry(id);
                if (entry != null) stocks.add(new ShopCatalogPayloads.Stock(BuiltInRegistries.ITEM.getId(entry.item()), count, bought));
            }
        }
        session.purchases = purchases.copy(); session.unique = Set.copyOf(unique);
        state.putUUID("ShopCatalogId", session.id);
        state.putInt("ShopCatalogSize", catalog.size());
        return new Update(session.id, replace, entries, List.copyOf(stocks));
    }

    static void clear(UUID playerId) { SESSIONS.remove(playerId); }

    record Update(UUID id, boolean replace, List<ShopCatalogPayloads.Entry> catalog, List<ShopCatalogPayloads.Stock> stocks) {
        void send(ServerPlayer player) {
            if (replace) {
                for (int i = 0; i < ShopCatalogPayloads.partCount(catalog.size()); i++) {
                    int from = i * ShopCatalogPayloads.PART_SIZE;
                    PacketDistributor.sendToPlayer(player, new ShopCatalogPayloads.CatalogPart(id, i, catalog.size(),
                            catalog.subList(from, Math.min(from + ShopCatalogPayloads.PART_SIZE, catalog.size()))));
                }
            }
            for (int from = 0; from < stocks.size(); from += ShopCatalogPayloads.PART_SIZE)
                PacketDistributor.sendToPlayer(player, new ShopCatalogPayloads.StockUpdate(id,
                        stocks.subList(from, Math.min(from + ShopCatalogPayloads.PART_SIZE, stocks.size()))));
        }
    }

    private static final class Session {
        final UUID id;
        final List<SystemShop.CatalogEntry> catalog;
        final long day;
        CompoundTag purchases = new CompoundTag();
        Set<String> unique = Set.of();

        Session(UUID id, List<SystemShop.CatalogEntry> catalog, long day) {
            this.id = id; this.catalog = catalog; this.day = day;
        }
    }
}
