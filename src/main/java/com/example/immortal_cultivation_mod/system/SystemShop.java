package com.example.immortal_cultivation_mod.system;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.item.CultivationItemRarity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.ShulkerBoxBlock;

public final class SystemShop {
    private static final String SHOP = "Shop";
    private static final String POINTS = "Points";
    private static final String DAY = "Day";
    private static final String NEXT_REFRESH_AT = "NextRefreshAt";
    private static final String PURCHASES = "Purchases";
    private static final String UNIQUE_BOUGHT = "UniqueBought";
    private static final String REQUESTS = "Requests";
    private static final String REQUEST_ITEM = "Item";
    private static final String REQUEST_COUNT = "Count";
    private static final String REQUEST_REWARD = "Reward";
    private static final String REQUEST_COMPLETE = "Complete";
    private static final int RECYCLE_PERCENT = 20;
    private static final int REQUEST_PERCENT = 35;
    private static final long REFRESH_INTERVAL_MILLIS = 20L * 60L * 1000L;

    private static final Set<String> UNIQUE_ITEM_IDS = Set.of(
            "immortal_cultivation_mod:reincarnation_true_art_method",
            "immortal_cultivation_mod:tuntian_demon_art_method",
            "minecraft:dragon_egg");
    private static final Set<String> DENIED_PATHS = Set.of(
            "air",
            "barrier",
            "bedrock",
            "command_block",
            "chain_command_block",
            "repeating_command_block",
            "command_block_minecart",
            "structure_block",
            "structure_void",
            "jigsaw",
            "debug_stick",
            "light",
            "knowledge_book",
            "end_portal_frame",
            "reinforced_deepslate",
            "spawner",
            "trial_spawner",
            "vault",
            "debug_stat_editor",
            "heavy_iron_bracer_chestplate_debug",
            "yaoyuan_fist_bone_chestplate_debug",
            "zhenshi_seal_helmet",
            "system",
            "lottery_ticket",
            "great_creation_shard",
            "infinite_spirit_stone",
            "mysterious_ring",
            "noob_hat",
            "small_level_up",
            "big_level_up",
            "wanted_order",
            "family_token",
            "royal_token",
            "zhuji_cultivator_spawn_egg",
            "jindan_cultivator_spawn_egg",
            "liusan_spawn_egg",
            "fanren_npc_spawn_egg",
            "crimson_scale_flame_lizard_spawn_egg",
            "crimson_sky_flame_flood_dragon_spawn_egg",
            "blood_ape_demon_general_spawn_egg",
            "azure_spirit_silkworm_spawn_egg",
            "magic_center_block",
            "magic_circle_block");
    private static volatile List<CatalogEntry> catalog;
    private static volatile Map<String, CatalogEntry> catalogById;

    private SystemShop() {
    }

    static void invalidateCatalog() {
        synchronized (SystemShop.class) {
            catalog = null;
            catalogById = null;
        }
    }

    public static void ensureDay(ServerPlayer player) {
        CompoundTag state = OuterSystemData.read(player);
        CompoundTag shop = state.getCompound(SHOP).copy();
        long now = System.currentTimeMillis();
        long nextRefreshAt = shop.getLong(NEXT_REFRESH_AT);
        boolean hasCurrentStock = shop.contains(DAY)
                && shop.contains(REQUESTS, Tag.TAG_LIST);
        boolean waitingForRefresh = nextRefreshAt > now
                && nextRefreshAt <= now + REFRESH_INTERVAL_MILLIS;
        if (hasCurrentStock && waitingForRefresh) {
            return;
        }
        long refreshSeed = now;
        shop.putLong(DAY, refreshSeed);
        shop.putLong(NEXT_REFRESH_AT, now + REFRESH_INTERVAL_MILLIS);
        shop.put(PURCHASES, new CompoundTag());
        shop.put(REQUESTS, generateRequests(player, refreshSeed));
        state.put(SHOP, shop);
        OuterSystemData.save(player, state);
    }

    public static void writeClientState(
            ServerPlayer player, CompoundTag result, boolean includeCatalog) {
        ensureDay(player);
        CompoundTag shop = OuterSystemData.read(player).getCompound(SHOP);
        result.putLong("ShopPoints", shop.getLong(POINTS));
        result.putLong("ShopDay", shop.getLong(DAY));
        long refreshMillis = Math.max(
                0L,
                shop.getLong(NEXT_REFRESH_AT) - System.currentTimeMillis());
        result.putInt(
                "ShopRefreshSeconds",
                (int)Math.min(Integer.MAX_VALUE,
                        (refreshMillis + 999L) / 1000L));
        result.put("ShopPurchases", shop.getCompound(PURCHASES).copy());
        result.put("ShopUniqueBought",
                shop.getList(UNIQUE_BOUGHT, Tag.TAG_STRING).copy());
        result.put("ShopRequests",
                shop.getList(REQUESTS, Tag.TAG_COMPOUND).copy());
        if (includeCatalog) {
            ListTag encoded = new ListTag();
            for (CatalogEntry entry : catalog()) {
                CompoundTag item = new CompoundTag();
                item.putString("Id", entry.id());
                item.putInt("Price", entry.price());
                item.putString("Rarity", entry.stockTier().id());
                item.putString("Category", entry.category().id());
                encoded.add(item);
            }
            result.put("ShopCatalog", encoded);
        }
    }

    public static void buy(ServerPlayer player, String itemId) {
        if (!isShopActive(player)) {
            return;
        }
        ensureDay(player);
        CatalogEntry entry = byId().get(itemId);
        if (entry == null) {
            fail(player, "message.immortal_cultivation_mod.system.shop.restricted");
            return;
        }

        CompoundTag state = OuterSystemData.read(player);
        CompoundTag shop = state.getCompound(SHOP).copy();
        long points = shop.getLong(POINTS);
        if (points < entry.price()) {
            fail(player, "message.immortal_cultivation_mod.system.shop.no_points");
            return;
        }
        if (remainingStock(player, shop, entry) == 0) {
            fail(player, "message.immortal_cultivation_mod.system.shop.no_stock");
            return;
        }

        ItemStack purchased = new ItemStack(entry.item());
        if (!player.getInventory().add(purchased)) {
            player.drop(purchased, false);
        }
        shop.putLong(POINTS, points - entry.price());
        if (entry.stockTier() == StockTier.UNIQUE) {
            ListTag bought = shop.getList(UNIQUE_BOUGHT, Tag.TAG_STRING).copy();
            if (!containsString(bought, entry.id())) {
                bought.add(StringTag.valueOf(entry.id()));
            }
            shop.put(UNIQUE_BOUGHT, bought);
        } else if (entry.stockTier() != StockTier.COMMON) {
            CompoundTag purchases = shop.getCompound(PURCHASES).copy();
            purchases.putInt(
                    entry.id(), purchases.getInt(entry.id()) + 1);
            shop.put(PURCHASES, purchases);
        }
        state.put(SHOP, shop);
        OuterSystemData.save(player, state);
        player.containerMenu.broadcastChanges();
        player.displayClientMessage(Component.translatable(
                "message.immortal_cultivation_mod.system.shop.bought",
                purchased.getHoverName(), entry.price()), true);
    }

    public static void recycle(ServerPlayer player, int inventorySlot) {
        if (!isShopActive(player)
                || inventorySlot < 0
                || inventorySlot >= player.getInventory().items.size()) {
            return;
        }
        ItemStack stack = player.getInventory().items.get(inventorySlot);
        CatalogEntry entry = stack.isEmpty()
                ? null
                : byId().get(itemId(stack.getItem()));
        if (entry == null || !isSellable(stack)) {
            fail(player, "message.immortal_cultivation_mod.system.shop.restricted");
            return;
        }

        int count = stack.getCount();
        long earned = Math.max(
                1L,
                (long) entry.price() * count * RECYCLE_PERCENT / 100L);
        stack.shrink(count);
        addPoints(player, earned);
        player.containerMenu.broadcastChanges();
        player.displayClientMessage(Component.translatable(
                "message.immortal_cultivation_mod.system.shop.recycled",
                earned), true);
    }

    public static void submitRequest(ServerPlayer player, int requestIndex) {
        if (!isShopActive(player)) {
            return;
        }
        ensureDay(player);
        CompoundTag state = OuterSystemData.read(player);
        CompoundTag shop = state.getCompound(SHOP).copy();
        ListTag requests = shop.getList(REQUESTS, Tag.TAG_COMPOUND).copy();
        if (requestIndex < 0 || requestIndex >= requests.size()) {
            return;
        }
        CompoundTag request = requests.getCompound(requestIndex).copy();
        if (request.getBoolean(REQUEST_COMPLETE)) {
            return;
        }
        CatalogEntry entry = byId().get(request.getString(REQUEST_ITEM));
        int needed = request.getInt(REQUEST_COUNT);
        if (entry == null || countSellable(player, entry.item()) < needed) {
            fail(player, "message.immortal_cultivation_mod.system.shop.request_missing");
            return;
        }
        removeSellable(player, entry.item(), needed);
        long reward = request.getLong(REQUEST_REWARD);
        request.putBoolean(REQUEST_COMPLETE, true);
        requests.set(requestIndex, request);
        shop.put(REQUESTS, requests);
        shop.putLong(POINTS, safeAdd(shop.getLong(POINTS), reward));
        state.put(SHOP, shop);
        OuterSystemData.save(player, state);
        player.containerMenu.broadcastChanges();
        player.displayClientMessage(Component.translatable(
                "message.immortal_cultivation_mod.system.shop.request_paid",
                reward), true);
    }

    public static int remainingStock(
            ServerPlayer player, CompoundTag shop, CatalogEntry entry) {
        int maximum = maxStock(entry, shop.getLong(DAY));
        if (maximum < 0) {
            return -1;
        }
        if (entry.stockTier() == StockTier.UNIQUE) {
            return containsString(
                    shop.getList(UNIQUE_BOUGHT, Tag.TAG_STRING),
                    entry.id()) ? 0 : 1;
        }
        return Math.max(
                0,
                maximum - shop.getCompound(PURCHASES).getInt(entry.id()));
    }

    public static int maxStock(CatalogEntry entry, long day) {
        return switch (entry.stockTier()) {
            case COMMON -> -1;
            case UNCOMMON -> 16;
            case RARE -> 1 + Math.floorMod(
                    31 * entry.id().hashCode() + Long.hashCode(day), 4);
            case UNIQUE -> 1;
        };
    }

    public static List<CatalogEntry> catalog() {
        List<CatalogEntry> current = catalog;
        if (current != null) {
            return current;
        }
        synchronized (SystemShop.class) {
            if (catalog == null) {
                List<CatalogEntry> entries = new ArrayList<>();
                for (Item item : BuiltInRegistries.ITEM) {
                    ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                    if (id == null || !isCatalogAllowed(item, id)) {
                        continue;
                    }
                    ItemStack stack = item.getDefaultInstance();
                    int price = SystemShopPrices.price(stack);
                    if (price <= 0) {
                        continue;
                    }
                    entries.add(new CatalogEntry(
                            id.toString(),
                            item,
                            price,
                            stockTier(stack, id),
                            category(item, id)));
                }
                entries.sort(Comparator
                        .comparingInt((CatalogEntry entry) ->
                                categoryOrder(entry.category()))
                        .thenComparingInt(SystemShop::namespaceOrder)
                        .thenComparingInt(SystemShop::catalogGroup)
                        .thenComparing(CatalogEntry::id));
                catalog = List.copyOf(entries);
                Map<String, CatalogEntry> indexed = new HashMap<>();
                for (CatalogEntry entry : catalog) {
                    indexed.put(entry.id(), entry);
                }
                catalogById = Map.copyOf(indexed);
            }
            return catalog;
        }
    }

    public static boolean isSellable(ItemStack stack) {
        if (stack.isEmpty()
                || !byId().containsKey(itemId(stack.getItem()))
                || stack.has(DataComponents.CUSTOM_NAME)
                || stack.has(DataComponents.CUSTOM_DATA)
                || stack.has(DataComponents.CONTAINER)
                || stack.has(DataComponents.BUNDLE_CONTENTS)) {
            return false;
        }
        if (stack.isDamageableItem()
                && stack.getDamageValue() * 4 > stack.getMaxDamage() * 3) {
            return false;
        }
        return stack.getComponentsPatch().entrySet().stream()
                .allMatch(entry -> entry.getKey() == DataComponents.DAMAGE);
    }

    private static Map<String, CatalogEntry> byId() {
        catalog();
        return catalogById;
    }

    private static ListTag generateRequests(ServerPlayer player, long day) {
        List<CatalogEntry> pool = catalog().stream()
                .filter(entry -> entry.stockTier() != StockTier.UNIQUE)
                .filter(entry -> !entry.item().getDefaultInstance()
                        .isDamageableItem())
                .toList();
        List<CatalogEntry> choices = new ArrayList<>(pool);
        RandomSource random = RandomSource.create(
                day * 31L ^ player.getUUID().getMostSignificantBits()
                        ^ player.getUUID().getLeastSignificantBits());
        for (int i = choices.size() - 1; i > 0; i--) {
            int other = random.nextInt(i + 1);
            CatalogEntry value = choices.get(i);
            choices.set(i, choices.get(other));
            choices.set(other, value);
        }

        ListTag requests = new ListTag();
        Set<String> used = new HashSet<>();
        for (CatalogEntry entry : choices) {
            if (requests.size() >= 3 || !used.add(entry.id())) {
                continue;
            }
            int count = switch (entry.stockTier()) {
                case COMMON -> 12 + random.nextInt(21);
                case UNCOMMON -> 4 + random.nextInt(9);
                case RARE -> 1 + random.nextInt(3);
                case UNIQUE -> 1;
            };
            count = Math.min(
                    count,
                    Math.max(1, entry.item().getDefaultMaxStackSize()));
            long normalRecycle = Math.max(
                    1L,
                    (long) entry.price() * count * RECYCLE_PERCENT / 100L);
            long reward = Math.max(
                    normalRecycle + 1L,
                    (long) entry.price() * count * REQUEST_PERCENT / 100L);
            CompoundTag request = new CompoundTag();
            request.putString(REQUEST_ITEM, entry.id());
            request.putInt(REQUEST_COUNT, count);
            request.putLong(REQUEST_REWARD, reward);
            request.putBoolean(REQUEST_COMPLETE, false);
            requests.add(request);
        }
        return requests;
    }

    private static boolean isCatalogAllowed(Item item, ResourceLocation id) {
        String path = id.getPath();
        if (item == Items.AIR
                || item instanceof SpawnEggItem
                || item instanceof BundleItem
                || path.endsWith("_spawn_egg")
                || path.contains("debug")
                || DENIED_PATHS.contains(path)) {
            return false;
        }
        if (item instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ShulkerBoxBlock) {
            return false;
        }
        ItemStack stack = item.getDefaultInstance();
        return !stack.has(DataComponents.CONTAINER)
                && !stack.has(DataComponents.BUNDLE_CONTENTS)
                && !stack.has(DataComponents.CUSTOM_DATA);
    }

    private static int categoryOrder(Category category) {
        return switch (category) {
            case PILL -> 0;
            case MATERIAL -> 1;
            case WEAPON -> 2;
            case SPELL -> 3;
            case BLOCK -> 4;
        };
    }

    private static int namespaceOrder(CatalogEntry entry) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(entry.item());
        return id != null && ImmortalCultivationMod.MODID.equals(
                id.getNamespace()) ? 0 : 1;
    }

    private static int catalogGroup(CatalogEntry entry) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(entry.item());
        if (id == null) {
            return 99;
        }
        String path = id.getPath();
        ItemStack stack = entry.item().getDefaultInstance();
        return switch (entry.category()) {
            case PILL -> path.contains("potion") ? 0 : 1;
            case MATERIAL -> {
                if (stack.has(DataComponents.FOOD)) {
                    yield 0;
                }
                if (containsAny(
                        path,
                        "_ingot",
                        "_nugget",
                        "raw_",
                        "diamond",
                        "emerald",
                        "coal",
                        "redstone",
                        "lapis",
                        "quartz",
                        "amethyst")) {
                    yield 1;
                }
                if (path.endsWith("_dye")) {
                    yield 2;
                }
                yield path.contains("bucket") ? 3 : 4;
            }
            case WEAPON -> {
                if (containsAny(
                        path,
                        "sword",
                        "spear",
                        "dao",
                        "knife",
                        "whip",
                        "trident",
                        "mace",
                        "bow")) {
                    yield 0;
                }
                if (containsAny(
                        path,
                        "helmet",
                        "chestplate",
                        "leggings",
                        "boots")) {
                    yield 1;
                }
                yield 2;
            }
            case SPELL -> 0;
            case BLOCK -> {
                if (containsAny(path, "log", "wood", "planks", "stem")) {
                    yield 0;
                }
                if (containsAny(
                        path,
                        "stone",
                        "deepslate",
                        "granite",
                        "diorite",
                        "andesite")) {
                    yield 1;
                }
                if (containsAny(
                        path,
                        "dirt",
                        "grass",
                        "sand",
                        "gravel",
                        "clay",
                        "mud")) {
                    yield 2;
                }
                if (path.contains("glass")) {
                    yield 3;
                }
                yield 4;
            }
        };
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static StockTier stockTier(ItemStack stack, ResourceLocation id) {
        if (UNIQUE_ITEM_IDS.contains(id.toString())) {
            return StockTier.UNIQUE;
        }
        return switch (CultivationItemRarity.of(stack)) {
            case COMMON -> StockTier.COMMON;
            case UNCOMMON -> StockTier.UNCOMMON;
            case RARE -> StockTier.RARE;
            // The explicit list above is now the only way to receive unique stock.
            case EPIC -> StockTier.RARE;
        };
    }

    private static Category category(Item item, ResourceLocation id) {
        String path = id.getPath();
        if (path.equals("pill") || path.endsWith("_pill")
                || path.contains("potion") || path.contains("elixir")) {
            return Category.PILL;
        }
        if (path.endsWith("_scroll") || path.endsWith("_method")
                || path.contains("manual")) {
            return Category.SPELL;
        }
        if (item instanceof BlockItem) {
            return Category.BLOCK;
        }
        if (item instanceof ArmorItem
                || item.getDefaultInstance().isDamageableItem()) {
            return Category.WEAPON;
        }
        return Category.MATERIAL;
    }

    private static int countSellable(ServerPlayer player, Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item) && isSellable(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void removeSellable(
            ServerPlayer player, Item item, int amount) {
        int remaining = amount;
        for (ItemStack stack : player.getInventory().items) {
            if (remaining <= 0) {
                break;
            }
            if (!stack.is(item) || !isSellable(stack)) {
                continue;
            }
            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            remaining -= removed;
        }
    }

    private static void addPoints(ServerPlayer player, long amount) {
        CompoundTag state = OuterSystemData.read(player);
        CompoundTag shop = state.getCompound(SHOP).copy();
        shop.putLong(POINTS, safeAdd(shop.getLong(POINTS), amount));
        state.put(SHOP, shop);
        OuterSystemData.save(player, state);
    }

    private static long safeAdd(long current, long amount) {
        if (amount <= 0L) {
            return current;
        }
        return current > Long.MAX_VALUE - amount
                ? Long.MAX_VALUE
                : current + amount;
    }

    private static boolean isShopActive(ServerPlayer player) {
        return OuterSystemData.equipped(player).isPresent()
                && OuterSystemData.type(player) == OuterSystemType.SYSTEM_SHOP;
    }

    private static String itemId(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id == null ? "" : id.toString();
    }

    private static boolean containsString(ListTag list, String value) {
        for (int i = 0; i < list.size(); i++) {
            if (value.equals(list.getString(i))) {
                return true;
            }
        }
        return false;
    }

    private static void fail(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    public enum StockTier {
        COMMON("common"),
        UNCOMMON("uncommon"),
        RARE("rare"),
        UNIQUE("unique");

        private final String id;

        StockTier(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    public enum Category {
        MATERIAL("material"),
        PILL("pill"),
        WEAPON("weapon"),
        SPELL("spell"),
        BLOCK("block");

        private final String id;

        Category(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    public record CatalogEntry(
            String id,
            Item item,
            int price,
            StockTier stockTier,
            Category category) {
    }
}
