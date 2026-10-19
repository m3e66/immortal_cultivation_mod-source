package com.example.immortal_cultivation_mod.network;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Bounded primitive shop data; never puts the registry-sized catalog through NBT decoding. */
public final class ShopCatalogPayloads {
    public static final int PART_SIZE = 512;
    public static final int MAX_ENTRIES = 1_000_000;
    private static final String MOD_ID = "immortal_cultivation_mod";

    private ShopCatalogPayloads() {}

    public static int partCount(int total) {
        if (total < 0 || total > MAX_ENTRIES) throw new IllegalArgumentException("Invalid shop catalog size");
        return Math.max(1, (total + PART_SIZE - 1) / PART_SIZE);
    }

    public record Entry(int itemId, int price, int rarity, int category, int purchased, boolean uniqueBought) {
        public Entry {
            if (itemId < 0 || price <= 0 || rarity < 0 || rarity >= 4 || category < 0 || category >= 9 || purchased < 0)
                throw new IllegalArgumentException("Invalid shop catalog entry");
        }
    }

    public record Stock(int itemId, int purchased, boolean uniqueBought) {
        public Stock {
            if (itemId < 0 || purchased < 0) throw new IllegalArgumentException("Invalid shop stock");
        }
    }

    public record CatalogPart(UUID transferId, int index, int total, List<Entry> entries) implements CustomPacketPayload {
        public static final Type<CatalogPart> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "shop_catalog_part"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CatalogPart> STREAM_CODEC = StreamCodec.of(
                (buffer, value) -> {
                    buffer.writeUUID(value.transferId()); buffer.writeVarInt(value.index()); buffer.writeVarInt(value.total());
                    buffer.writeVarInt(value.entries().size());
                    for (Entry entry : value.entries()) {
                        buffer.writeVarInt(entry.itemId()); buffer.writeVarInt(entry.price());
                        buffer.writeByte(entry.rarity()); buffer.writeByte(entry.category());
                        buffer.writeVarInt(entry.purchased()); buffer.writeBoolean(entry.uniqueBought());
                    }
                }, buffer -> {
                    UUID transfer = buffer.readUUID(); int index = buffer.readVarInt(), total = buffer.readVarInt(), count = buffer.readVarInt();
                    validatePart(index, total, count);
                    List<Entry> entries = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) entries.add(new Entry(buffer.readVarInt(), buffer.readVarInt(),
                            buffer.readUnsignedByte(), buffer.readUnsignedByte(), buffer.readVarInt(), buffer.readBoolean()));
                    return new CatalogPart(transfer, index, total, entries);
                });

        public CatalogPart {
            validatePart(index, total, entries.size());
            entries = List.copyOf(entries);
        }

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static void validatePart(int index, int total, int count) {
        int parts = partCount(total);
        if (index < 0 || index >= parts || count != Math.min(PART_SIZE, total - index * PART_SIZE))
            throw new IllegalArgumentException("Invalid shop catalog part");
    }

    public record StockUpdate(UUID transferId, List<Stock> entries) implements CustomPacketPayload {
        public static final Type<StockUpdate> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "shop_stock_update"));
        public static final StreamCodec<RegistryFriendlyByteBuf, StockUpdate> STREAM_CODEC = StreamCodec.of(
                (buffer, value) -> {
                    buffer.writeUUID(value.transferId()); buffer.writeVarInt(value.entries().size());
                    for (Stock stock : value.entries()) {
                        buffer.writeVarInt(stock.itemId()); buffer.writeVarInt(stock.purchased()); buffer.writeBoolean(stock.uniqueBought());
                    }
                }, buffer -> {
                    UUID transfer = buffer.readUUID(); int count = buffer.readVarInt();
                    if (count < 0 || count > PART_SIZE) throw new IllegalArgumentException("Invalid shop stock update size");
                    List<Stock> entries = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) entries.add(new Stock(buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean()));
                    return new StockUpdate(transfer, entries);
                });

        public StockUpdate {
            if (entries.size() > PART_SIZE) throw new IllegalArgumentException("Shop stock update too large");
            entries = List.copyOf(entries);
        }

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
