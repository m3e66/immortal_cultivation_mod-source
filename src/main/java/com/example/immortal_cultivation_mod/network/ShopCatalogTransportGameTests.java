package com.example.immortal_cultivation_mod.network;

import com.mojang.logging.LogUtils;
import com.example.immortal_cultivation_mod.system.OuterSystemData;
import com.example.immortal_cultivation_mod.system.SystemShop;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounterException;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;

/** Exercises the actual codecs and transfer lifecycle behind large, modded system shops. */
@GameTestHolder("immortal_cultivation_system_transport")
@PrefixGameTestTemplate(false)
public final class ShopCatalogTransportGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SAFE_PACKET_BYTES = 16 * 1024;

    private ShopCatalogTransportGameTests() {}

    @GameTest(template = "fate_test")
    public static void legacyFullCatalogReproducesNbtQuotaFailure(GameTestHelper helper) {
        CompoundTag state = new CompoundTag();
        state.putString("Type", "system_shop");
        ListTag catalog = new ListTag();
        for (int i = 0; i < 10_000; i++) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Id", "synthetic_mod_" + i / 100 + ":refined_material_" + i);
            entry.putInt("Price", 1 + i * 997);
            entry.putString("Rarity", switch (i % 4) {
                case 1 -> "uncommon";
                case 2 -> "rare";
                case 3 -> "unique";
                default -> "common";
            });
            entry.putString("Category", "material");
            catalog.add(entry);
        }
        state.put("ShopCatalog", catalog);
        var buffer = buffer(helper);
        try {
            OuterSystemPayloads.ClientboundOpen.STREAM_CODEC.encode(buffer,
                    new OuterSystemPayloads.ClientboundOpen(state));
            int bytes = buffer.readableBytes();
            helper.assertTrue(bytes > 0, "Legacy fixture is serialized with the production open codec");
            RuntimeException failure = null;
            try {
                OuterSystemPayloads.ClientboundOpen.STREAM_CODEC.decode(buffer);
            } catch (RuntimeException exception) {
                failure = exception;
            }
            helper.assertTrue(hasCause(failure, NbtAccounterException.class),
                    "10,000 catalog compounds reproduce the actual 2 MiB NBT allocation failure");
            LOGGER.info("Shop transport regression: legacy 10,000-entry catalog uses {} wire bytes and exceeds the 2,097,152-byte NBT allocation quota", bytes);
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void largeCatalogRoundTripsOutOfOrderWithoutLosingEconomicFields(GameTestHelper helper) {
        List<ShopCatalogPayloads.Entry> expected = entries(10_000);
        UUID id = UUID.randomUUID();
        var transfer = new ShopCatalogTransfer();
        transfer.begin(id, expected.size());
        int maximumBytes = 0;
        int parts = ShopCatalogPayloads.partCount(expected.size());
        for (int index = parts - 1; index >= 0; index--) {
            var part = part(id, index, expected);
            var buffer = buffer(helper);
            try {
                ShopCatalogPayloads.CatalogPart.STREAM_CODEC.encode(buffer, part);
                maximumBytes = Math.max(maximumBytes, buffer.readableBytes());
                helper.assertTrue(buffer.readableBytes() < SAFE_PACKET_BYTES,
                        "Every production catalog part stays below 16 KiB");
                var decoded = ShopCatalogPayloads.CatalogPart.STREAM_CODEC.decode(buffer);
                helper.assertTrue(part.equals(decoded) && !buffer.isReadable(),
                        "Catalog codec retains its complete header and all entry fields");
                helper.assertTrue(transfer.accept(decoded) == (index == 0),
                        "Catalog becomes visible only after every distinct part arrives");
                if (index != 0) {
                    helper.assertTrue(!transfer.complete() && transfer.entries().isEmpty(),
                            "Partial transfer does not expose an incomplete purchasable catalog");
                }
            } finally {
                buffer.release();
            }
        }
        helper.assertTrue(transfer.complete() && expected.equals(transfer.entries()),
                "Reversed packet order restores exact item order, prices, rarity, categories, purchases and unique flags");
        LOGGER.info("Shop transport regression: 10,000 entries decoded in {} parts; largest part {} bytes", parts, maximumBytes);
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void maximumWidthFieldsStayBelowSixteenKiB(GameTestHelper helper) {
        List<ShopCatalogPayloads.Entry> values = new ArrayList<>();
        for (int i = 0; i < ShopCatalogPayloads.PART_SIZE; i++) {
            values.add(new ShopCatalogPayloads.Entry(Integer.MAX_VALUE - i,
                    Integer.MAX_VALUE, 3, 8, Integer.MAX_VALUE, true));
        }
        // The final full page near the protocol's upper bound exercises multi-byte headers too.
        int total = ShopCatalogPayloads.MAX_ENTRIES / ShopCatalogPayloads.PART_SIZE
                * ShopCatalogPayloads.PART_SIZE;
        int index = ShopCatalogPayloads.partCount(total) - 1;
        var expected = new ShopCatalogPayloads.CatalogPart(UUID.randomUUID(), index, total, values);
        var buffer = buffer(helper);
        try {
            ShopCatalogPayloads.CatalogPart.STREAM_CODEC.encode(buffer, expected);
            int bytes = buffer.readableBytes();
            helper.assertTrue(bytes < SAFE_PACKET_BYTES,
                    "512 entries with worst-case positive VarInts and large headers remain below 16 KiB");
            helper.assertTrue(expected.equals(ShopCatalogPayloads.CatalogPart.STREAM_CODEC.decode(buffer))
                            && !buffer.isReadable(),
                    "Maximum-width values round-trip without truncation");
            LOGGER.info("Shop transport regression: worst-case 512-entry catalog part uses {} bytes", bytes);
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void duplicateStaleAndCanceledTransfersCannotReplaceReopenedCatalog(GameTestHelper helper) {
        var transfer = new ShopCatalogTransfer();
        List<ShopCatalogPayloads.Entry> old = entries(1_024);
        UUID oldId = UUID.randomUUID();
        transfer.begin(oldId, old.size());
        helper.assertTrue(!transfer.accept(part(oldId, 0, old)), "One page cannot finish a two-page transfer");
        transfer.begin(oldId, old.size());
        helper.assertTrue(!transfer.accept(part(oldId, 0, old)), "Repeated metadata preserves and ignores a duplicate page");
        var inconsistent = new ShopCatalogPayloads.CatalogPart(oldId, 1, 1_025,
                old.subList(512, 1_024));
        helper.assertTrue(!transfer.accept(inconsistent) && !transfer.complete(),
                "Matching UUID cannot override the catalog's authorized total");
        helper.assertTrue(transfer.accept(part(oldId, 1, old)) && old.equals(transfer.entries()),
                "Duplicate and inconsistent packets do not discard valid progress");
        helper.assertTrue(!transfer.accept(part(oldId, 1, old)) && old.equals(transfer.entries()),
                "Completed transfer remains immutable after replay");

        transfer.clear();
        helper.assertTrue(!transfer.complete() && transfer.entries().isEmpty()
                        && !transfer.matches(oldId) && !transfer.accept(part(oldId, 0, old)),
                "Closing discards authorization and ignores queued catalog packets");
        UUID reopenedId = UUID.randomUUID();
        List<ShopCatalogPayloads.Entry> reopened = entries(513);
        transfer.begin(reopenedId, reopened.size());
        helper.assertTrue(!transfer.accept(part(oldId, 1, old)),
                "Previous screen's packets cannot finish a newly opened catalog");
        helper.assertTrue(!transfer.accept(part(reopenedId, 1, reopened)),
                "Reopened catalog independently tracks its last partial page");
        helper.assertTrue(transfer.accept(part(reopenedId, 0, reopened))
                        && reopened.equals(transfer.entries()),
                "Fresh generation completes from only its own ordered data");
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void malformedCatalogHeadersFailBeforeReadingEntryData(GameTestHelper helper) {
        int[][] invalid = {
                {0, -1, 0},
                {0, ShopCatalogPayloads.MAX_ENTRIES + 1, 0},
                {0, Integer.MAX_VALUE, Integer.MAX_VALUE},
                {-1, 512, 512},
                {1, 512, 0},
                {Integer.MAX_VALUE, 512, 512},
                {0, 512, -1},
                {0, 512, 513},
                {0, 512, Integer.MAX_VALUE},
                {0, 512, 511},
                {1, 513, 512},
                {0, 0, 1}
        };
        for (int[] header : invalid) {
            var buffer = buffer(helper);
            try {
                buffer.writeUUID(UUID.randomUUID());
                buffer.writeVarInt(header[0]);
                buffer.writeVarInt(header[1]);
                buffer.writeVarInt(header[2]);
                int headerBytes = buffer.writerIndex();
                expectIllegal(helper, () -> ShopCatalogPayloads.CatalogPart.STREAM_CODEC.decode(buffer),
                        "Invalid index/total/count rejects header before allocating or decoding entries");
                helper.assertTrue(buffer.readerIndex() == headerBytes && !buffer.isReadable(),
                        "Only bounded header fields were consumed from malicious catalog packet");
            } finally {
                buffer.release();
            }
        }
        var transfer = new ShopCatalogTransfer();
        UUID id = UUID.randomUUID();
        List<ShopCatalogPayloads.Entry> pending = entries(1);
        transfer.begin(id, pending.size());
        expectIllegal(helper, () -> transfer.begin(UUID.randomUUID(), -1), "Negative assembly size is rejected");
        expectIllegal(helper, () -> transfer.begin(UUID.randomUUID(), Integer.MAX_VALUE), "Unbounded assembly size is rejected");
        helper.assertTrue(transfer.matches(id) && transfer.accept(part(id, 0, pending)),
                "Rejected allocation requests leave the previously authorized transfer intact");
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void emptyCatalogCompletesOnlyWithItsAuthorizedEmptyPart(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        var transfer = new ShopCatalogTransfer();
        transfer.begin(id, 0);
        helper.assertTrue(!transfer.complete(), "Metadata alone does not complete an empty catalog");
        var expected = new ShopCatalogPayloads.CatalogPart(id, 0, 0, List.of());
        var buffer = buffer(helper);
        try {
            ShopCatalogPayloads.CatalogPart.STREAM_CODEC.encode(buffer, expected);
            var decoded = ShopCatalogPayloads.CatalogPart.STREAM_CODEC.decode(buffer);
            helper.assertTrue(expected.equals(decoded) && !buffer.isReadable()
                            && transfer.accept(decoded) && transfer.complete() && transfer.entries().isEmpty(),
                    "Empty shop round-trips and resolves its loading state with no item loss");
            helper.assertTrue(!transfer.accept(decoded), "Empty completion replay is ignored");
            transfer.clear();
            helper.assertTrue(!transfer.accept(decoded) && !transfer.complete(), "Late empty completion cannot revive a closed shop");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void stockDeltasAreBoundedAndPreserveExtremePurchaseCounts(GameTestHelper helper) {
        List<ShopCatalogPayloads.Stock> stocks = new ArrayList<>();
        for (int i = 0; i < ShopCatalogPayloads.PART_SIZE; i++) {
            stocks.add(new ShopCatalogPayloads.Stock(Integer.MAX_VALUE - i,
                    Integer.MAX_VALUE - i * 3, i % 2 == 0));
        }
        var expected = new ShopCatalogPayloads.StockUpdate(UUID.randomUUID(), stocks);
        var buffer = buffer(helper);
        try {
            ShopCatalogPayloads.StockUpdate.STREAM_CODEC.encode(buffer, expected);
            helper.assertTrue(buffer.readableBytes() < SAFE_PACKET_BYTES,
                    "Worst-case purchase updates never approach Minecraft packet limits");
            helper.assertTrue(expected.equals(ShopCatalogPayloads.StockUpdate.STREAM_CODEC.decode(buffer))
                            && !buffer.isReadable(),
                    "Stock codec preserves item IDs, extreme purchase counts and unique-purchase flags");
        } finally {
            buffer.release();
        }
        for (int count : new int[]{-1, ShopCatalogPayloads.PART_SIZE + 1, Integer.MAX_VALUE}) {
            var malformed = buffer(helper);
            try {
                malformed.writeUUID(UUID.randomUUID());
                malformed.writeVarInt(count);
                expectIllegal(helper, () -> ShopCatalogPayloads.StockUpdate.STREAM_CODEC.decode(malformed),
                        "Oversized stock update fails before reading or allocating entry data");
                helper.assertTrue(!malformed.isReadable(), "Invalid stock packet consumes only its header");
            } finally {
                malformed.release();
            }
        }
        stocks.add(new ShopCatalogPayloads.Stock(1, 0, false));
        expectIllegal(helper, () -> new ShopCatalogPayloads.StockUpdate(UUID.randomUUID(), stocks),
                "Local producers cannot create a stock update exceeding the wire limit");
        helper.succeed();
    }

    @GameTest(template = "fate_test")
    public static void productionMetadataNeverIncludesRegistrySizedStockTables(GameTestHelper helper) {
        var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "shop-transport"), ClientInformation.createDefault());
        CompoundTag saved = new CompoundTag();
        CompoundTag shop = new CompoundTag();
        long points = 987_654_321_987L;
        shop.putLong("Points", points);
        shop.putLong("Day", 321_987L);
        shop.putLong("NextRefreshAt", System.currentTimeMillis() + 10 * 60 * 1_000L);
        CompoundTag purchases = new CompoundTag();
        ListTag unique = new ListTag();
        for (int i = 0; i < 10_000; i++) {
            String id = "synthetic_mod_" + i / 100 + ":refined_material_" + i;
            purchases.putInt(id, i + 1);
            unique.add(StringTag.valueOf(id));
        }
        shop.put("Purchases", purchases);
        shop.put("UniqueBought", unique);
        ListTag requests = new ListTag();
        for (int i = 0; i < 3; i++) {
            CompoundTag request = new CompoundTag();
            request.putString("Item", "minecraft:stone");
            request.putInt("Count", 16 + i);
            request.putLong("Reward", 1_000L + i);
            request.putBoolean("Complete", i == 1);
            requests.add(request);
        }
        shop.put("Requests", requests);
        saved.put("Shop", shop);
        OuterSystemData.save(player, saved);
        try {
            for (boolean opening : new boolean[]{true, false}) {
                CompoundTag state = new CompoundTag();
                state.putString("Type", "system_shop");
                SystemShop.writeClientState(player, state, opening);
                helper.assertTrue(!state.contains("ShopCatalog") && !state.contains("ShopPurchases")
                                && !state.contains("ShopUniqueBought"),
                        "Explicit opening and periodic updates both exclude all unbounded catalog and stock NBT");
                helper.assertTrue(state.getLong("ShopPoints") == points && state.getLong("ShopDay") == 321_987L
                                && state.getInt("ShopRefreshSeconds") > 0
                                && state.getList("ShopRequests", Tag.TAG_COMPOUND).equals(requests),
                        "Compact metadata preserves balance, stock day, refresh clock and all three request rewards");
                var buffer = buffer(helper);
                try {
                    OuterSystemPayloads.ClientboundOpen.STREAM_CODEC.encode(buffer,
                            new OuterSystemPayloads.ClientboundOpen(state));
                    helper.assertTrue(buffer.readableBytes() < 2 * 1024,
                            "Production open metadata stays below 2 KiB with 10,000 purchase and unique records");
                    helper.assertTrue(OuterSystemPayloads.ClientboundOpen.STREAM_CODEC.decode(buffer).state().equals(state)
                                    && !buffer.isReadable(),
                            "Production metadata decodes under the unchanged safe NBT quota");
                } finally {
                    buffer.release();
                }
            }
            helper.assertTrue(OuterSystemData.read(player).equals(saved),
                    "Network size fix preserves every saved purchase and unique-item entitlement");
        } finally {
            player.discard();
        }
        helper.succeed();
    }

    private static RegistryFriendlyByteBuf buffer(GameTestHelper helper) {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
    }

    private static List<ShopCatalogPayloads.Entry> entries(int size) {
        List<ShopCatalogPayloads.Entry> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            values.add(new ShopCatalogPayloads.Entry(i + 1, 1 + i * 997,
                    i % 4, i % 9, i * 13, i % 7 == 0));
        }
        return values;
    }

    private static ShopCatalogPayloads.CatalogPart part(UUID id, int index,
                                                       List<ShopCatalogPayloads.Entry> values) {
        int first = index * ShopCatalogPayloads.PART_SIZE;
        return new ShopCatalogPayloads.CatalogPart(id, index, values.size(),
                values.subList(first, Math.min(values.size(), first + ShopCatalogPayloads.PART_SIZE)));
    }

    private static void expectIllegal(GameTestHelper helper, Runnable operation, String message) {
        boolean rejected = false;
        try {
            operation.run();
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        helper.assertTrue(rejected, message);
    }

    private static boolean hasCause(Throwable failure, Class<? extends Throwable> type) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (type.isInstance(current)) return true;
        }
        return false;
    }
}
