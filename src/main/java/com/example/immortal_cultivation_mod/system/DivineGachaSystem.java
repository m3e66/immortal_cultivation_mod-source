package com.example.immortal_cultivation_mod.system;

import com.example.immortal_cultivation_mod.block.ModBlocks;
import com.example.immortal_cultivation_mod.item.ModItems;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class DivineGachaSystem {
    private static final String ROOT = "DivineGacha";
    private static final String LAST_FREE_DAY = "LastFreeDay";
    private static final String FREE_DRAWS = "FreeDraws";
    private static final String TOTAL_DRAWS = "TotalDraws";
    private static final String NO_RARE = "NoRare";
    private static final String NO_EPIC = "NoEpic";
    private static final String NO_LEGENDARY = "NoLegendary";
    private static final String NO_DIVINE = "NoDivine";
    private static final String PENDING = "Pending";
    private static final String STACK = "Stack";
    private static final String RARITY = "Rarity";
    private static final String LAST_ITEM = "LastItem";
    private static final String LAST_RARITY = "LastRarity";
    private static final int MAX_FREE_DRAWS = 3;
    private static final int MAX_PENDING_SLOTS = 54;

    private DivineGachaSystem() {
    }

    public static void ensureDay(ServerPlayer player) {
        CompoundTag state = OuterSystemData.read(player);
        CompoundTag gacha = state.getCompound(ROOT).copy();
        long day = currentDay(player);
        boolean changed = false;
        if (!gacha.contains(LAST_FREE_DAY)) {
            gacha.putLong(LAST_FREE_DAY, day);
            gacha.putInt(
                    FREE_DRAWS,
                    Math.max(1, gacha.getInt(FREE_DRAWS)));
            changed = true;
        } else {
            long previous = gacha.getLong(LAST_FREE_DAY);
            if (day > previous) {
                long elapsed = Math.min(
                        MAX_FREE_DRAWS, day - previous);
                gacha.putInt(
                        FREE_DRAWS,
                        Math.min(
                                MAX_FREE_DRAWS,
                                gacha.getInt(FREE_DRAWS)
                                        + (int) elapsed));
                gacha.putLong(LAST_FREE_DAY, day);
                changed = true;
            } else if (day < previous) {
                gacha.putLong(LAST_FREE_DAY, day);
                changed = true;
            }
        }
        int storedDraws = gacha.getInt(FREE_DRAWS);
        int cappedDraws = Math.max(
                0, Math.min(MAX_FREE_DRAWS, storedDraws));
        if (storedDraws != cappedDraws) {
            gacha.putInt(FREE_DRAWS, cappedDraws);
            changed = true;
        }
        if (changed) {
            state.put(ROOT, gacha);
            OuterSystemData.save(player, state);
        }
    }

    public static void writeClientState(
            ServerPlayer player, CompoundTag result) {
        ensureDay(player);
        CompoundTag gacha =
                OuterSystemData.read(player).getCompound(ROOT);
        result.putInt("GachaFreeDraws", gacha.getInt(FREE_DRAWS));
        result.putInt(
                "GachaTickets",
                player.getInventory().countItem(
                        ModItems.LOTTERY_TICKET.get()));
        result.putLong("GachaTotalDraws", gacha.getLong(TOTAL_DRAWS));
        result.putInt("GachaNoRare", gacha.getInt(NO_RARE));
        result.putInt("GachaNoEpic", gacha.getInt(NO_EPIC));
        result.putInt(
                "GachaNoLegendary",
                gacha.getInt(NO_LEGENDARY));
        result.putInt("GachaNoDivine", gacha.getInt(NO_DIVINE));
        result.putString(
                "GachaLastItem", gacha.getString(LAST_ITEM));
        result.putString(
                "GachaLastRarity", gacha.getString(LAST_RARITY));
        result.put(
                "GachaPending",
                gacha.getList(PENDING, Tag.TAG_COMPOUND).copy());
    }

    public static void draw(ServerPlayer player) {
        if (!isActive(player)) {
            return;
        }
        ensureDay(player);
        CompoundTag state = OuterSystemData.read(player);
        CompoundTag gacha = state.getCompound(ROOT).copy();
        boolean useFree = gacha.getInt(FREE_DRAWS) > 0;
        if (!useFree && !hasTicket(player)) {
            message(
                    player,
                    "message.immortal_cultivation_mod.system.gacha.no_draw");
            return;
        }

        RarityTier rolled = roll(gacha, player.getRandom());
        ItemStack reward = reward(rolled, player.getRandom());
        ListTag pending =
                gacha.getList(PENDING, Tag.TAG_COMPOUND).copy();
        ListTag updated = pending.copy();
        if (!appendReward(player, updated, reward, rolled)) {
            message(
                    player,
                    "message.immortal_cultivation_mod.system.gacha.pending_full");
            return;
        }

        if (useFree) {
            gacha.putInt(
                    FREE_DRAWS, gacha.getInt(FREE_DRAWS) - 1);
        } else {
            consumeTicket(player);
        }
        updatePity(gacha, rolled);
        gacha.putLong(
                TOTAL_DRAWS, gacha.getLong(TOTAL_DRAWS) + 1L);
        gacha.putString(
                LAST_ITEM,
                reward.getHoverName().getString());
        gacha.putString(LAST_RARITY, rolled.id());
        gacha.put(PENDING, updated);
        state.put(ROOT, gacha);
        OuterSystemData.save(player, state);
        player.containerMenu.broadcastChanges();
        player.displayClientMessage(
                Component.translatable(
                        "message.immortal_cultivation_mod.system.gacha.drawn",
                        Component.translatable(rolled.translationKey()),
                        reward.getHoverName(),
                        reward.getCount()),
                true);
    }

    public static void claimPending(ServerPlayer player) {
        if (!isActive(player)) {
            return;
        }
        CompoundTag state = OuterSystemData.read(player);
        CompoundTag gacha = state.getCompound(ROOT).copy();
        ListTag pending =
                gacha.getList(PENDING, Tag.TAG_COMPOUND);
        if (pending.isEmpty()) {
            return;
        }

        ListTag remaining = new ListTag();
        int claimed = 0;
        for (int i = 0; i < pending.size(); i++) {
            CompoundTag entry = pending.getCompound(i);
            ItemStack reward = ItemStack.parseOptional(
                    player.registryAccess(),
                    entry.getCompound(STACK));
            if (reward.isEmpty()) {
                continue;
            }
            ItemStack remainder = reward.copy();
            int before = remainder.getCount();
            player.getInventory().add(remainder);
            claimed += before - remainder.getCount();
            if (!remainder.isEmpty()) {
                remaining.add(encodeReward(
                        player,
                        remainder,
                        entry.getString(RARITY)));
            }
        }
        gacha.put(PENDING, remaining);
        state.put(ROOT, gacha);
        OuterSystemData.save(player, state);
        player.containerMenu.broadcastChanges();
        if (claimed > 0) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.immortal_cultivation_mod.system.gacha.claimed",
                            claimed),
                    true);
        }
        if (!remaining.isEmpty()) {
            message(
                    player,
                    "message.immortal_cultivation_mod.system.gacha.inventory_full");
        }
    }

    private static RarityTier roll(
            CompoundTag gacha, RandomSource random) {
        RarityTier floor;
        if (gacha.getInt(NO_DIVINE) >= 500) {
            floor = RarityTier.DIVINE;
        } else if (gacha.getInt(NO_LEGENDARY) >= 200) {
            floor = RarityTier.LEGENDARY;
        } else if (gacha.getInt(NO_EPIC) >= 50) {
            floor = RarityTier.EPIC;
        } else if (gacha.getInt(NO_RARE) >= 10) {
            floor = RarityTier.RARE;
        } else {
            floor = RarityTier.ORDINARY;
        }

        int value = random.nextInt(1_000);
        RarityTier natural;
        if (value < 600) {
            natural = RarityTier.ORDINARY;
        } else if (value < 850) {
            natural = RarityTier.FINE;
        } else if (value < 950) {
            natural = RarityTier.RARE;
        } else if (value < 990) {
            natural = RarityTier.EPIC;
        } else if (value < 999) {
            natural = RarityTier.LEGENDARY;
        } else {
            natural = RarityTier.DIVINE;
        }
        return natural.ordinal() < floor.ordinal() ? floor : natural;
    }

    private static void updatePity(
            CompoundTag gacha, RarityTier rolled) {
        updateCounter(gacha, NO_RARE, rolled, RarityTier.RARE);
        updateCounter(gacha, NO_EPIC, rolled, RarityTier.EPIC);
        updateCounter(
                gacha, NO_LEGENDARY, rolled, RarityTier.LEGENDARY);
        updateCounter(
                gacha, NO_DIVINE, rolled, RarityTier.DIVINE);
    }

    private static void updateCounter(
            CompoundTag gacha,
            String key,
            RarityTier rolled,
            RarityTier threshold) {
        gacha.putInt(
                key,
                rolled.ordinal() >= threshold.ordinal()
                        ? 0
                        : gacha.getInt(key) + 1);
    }

    private static ItemStack reward(
            RarityTier tier, RandomSource random) {
        List<RewardOption> pool = rewardPool(tier);
        return pool.get(random.nextInt(pool.size())).create(random);
    }

    private static List<RewardOption> rewardPool(RarityTier tier) {
        return switch (tier) {
            case ORDINARY -> List.of(
                    option(ModItems.LOTTERY_TICKET, 1, 1),
                    option(ModItems.COPPER_COIN, 8, 24),
                    option(
                            com.example.immortal_cultivation_mod.magicritual.item
                                    .ModItems.SPIRIT_STONE,
                            4,
                            12),
                    option(() -> Items.BREAD, 3, 8),
                    option(
                            () -> ModBlocks.QINGLING_GRASS.get().asItem(),
                            3,
                            8),
                    option(ModItems.BLOOD_CRYSTAL, 2, 5),
                    option(ModItems.BIGU_PILL, 2, 4));
            case FINE -> List.of(
                    option(ModItems.LOW_QI_RECOVERY_PILL, 2, 5),
                    option(
                            () -> ModBlocks.CHIYAN_FLOWER.get().asItem(),
                            2,
                            5),
                    option(
                            () -> ModBlocks.NINGLU_ZHI.get().asItem(),
                            1,
                            3),
                    option(ModItems.ZHENSHI_TALISMAN, 2, 5),
                    option(ModItems.FIREBALL_TALISMAN, 1, 3),
                    option(ModItems.LOW_YANGJIAN_STONE, 1, 2),
                    option(
                            com.example.immortal_cultivation_mod.magicritual.item
                                    .ModItems.MEDIUM_SPIRIT_STONE,
                            1,
                            3));
            case RARE -> List.of(
                    option(ModItems.MIDDLE_QI_RECOVERY_PILL, 1, 3),
                    option(
                            () -> ModBlocks.JINGTIE_MOTHERSTONE.get().asItem(),
                            2,
                            4),
                    option(
                            () -> ModBlocks.YUNWEN_SILVER_CRYSTAL.get().asItem(),
                            1,
                            3),
                    option(ModItems.FLYING_SWORD, 1, 1),
                    option(ModItems.DAO, 1, 1),
                    option(ModItems.JINGTIE_SPEAR, 1, 1),
                    option(ModItems.FIREBALL_SCROLL, 1, 1),
                    option(ModItems.WIND_BLADE_SCROLL, 1, 1),
                    option(ModItems.RESTRICTION_INSIGHT, 1, 2),
                    option(
                            com.example.immortal_cultivation_mod.magicritual.item
                                    .ModItems.MEDIUM_SPIRIT_STONE,
                            4,
                            8));
            case EPIC -> List.of(
                    option(ModItems.HIGH_QI_RECOVERY_PILL, 1, 2),
                    option(ModItems.LINGJIAO_SCALE, 1, 2),
                    option(
                            () -> ModBlocks.HANPO_JADE.get().asItem(),
                            2,
                            4),
                    option(
                            () -> ModBlocks.MOXUAN_IRON.get().asItem(),
                            2,
                            4),
                    option(ModItems.WULEI_ZHENGFA_SCROLL, 1, 1),
                    option(ModItems.ZHENSHAN_PALM_SCROLL, 1, 1),
                    option(ModItems.FEIJIAN_YUJIAN, 1, 1),
                    option(ModItems.DAO_YUJIAN, 1, 1),
                    option(
                            com.example.immortal_cultivation_mod.magicritual.item
                                    .ModItems.HIGH_SPIRIT_STONE,
                            1,
                            3));
            case LEGENDARY -> List.of(
                    option(ModItems.POKONG_JUE_METHOD, 1, 1),
                    option(ModItems.CHANGQING_JUE_METHOD, 1, 1),
                    option(ModItems.CUIGU_LIANSHEN_METHOD, 1, 1),
                    option(ModItems.HANTI_BINGQIN_METHOD, 1, 1),
                    option(ModItems.PROTECTIVE_JADE_PENDANT, 1, 1),
                    option(ModItems.SHIXU_CHILIU_SCROLL, 1, 1),
                    option(
                            com.example.immortal_cultivation_mod.magicritual.item
                                    .ModItems.HIGH_SPIRIT_STONE,
                            4,
                            8));
            case DIVINE -> List.of(
                    option(ModItems.GREAT_CREATION_SHARD, 1, 1),
                    option(ModItems.REINCARNATION_TRUE_ART_METHOD, 1, 1),
                    option(ModItems.TUNTIAN_DEMON_ART_METHOD, 1, 1),
                    option(ModItems.JADE_PENDANT, 1, 1),
                    option(
                            com.example.immortal_cultivation_mod.magicritual.item
                                    .ModItems.HIGH_SPIRIT_STONE,
                            12,
                            24));
        };
    }

    private static RewardOption option(
            Supplier<? extends Item> item, int minimum, int maximum) {
        return new RewardOption(item, minimum, maximum);
    }

    private static boolean appendReward(
            ServerPlayer player,
            ListTag pending,
            ItemStack reward,
            RarityTier rarity) {
        ItemStack remaining = reward.copy();
        for (int i = 0; i < pending.size() && !remaining.isEmpty(); i++) {
            CompoundTag entry = pending.getCompound(i);
            if (!rarity.id().equals(entry.getString(RARITY))) {
                continue;
            }
            ItemStack stored = ItemStack.parseOptional(
                    player.registryAccess(),
                    entry.getCompound(STACK));
            if (!ItemStack.isSameItemSameComponents(stored, remaining)
                    || stored.getCount() >= stored.getMaxStackSize()) {
                continue;
            }
            int moved = Math.min(
                    remaining.getCount(),
                    stored.getMaxStackSize() - stored.getCount());
            stored.grow(moved);
            remaining.shrink(moved);
            pending.set(
                    i,
                    encodeReward(player, stored, rarity.id()));
        }
        while (!remaining.isEmpty()) {
            if (pending.size() >= MAX_PENDING_SLOTS) {
                return false;
            }
            int amount = Math.min(
                    remaining.getCount(), remaining.getMaxStackSize());
            ItemStack part = remaining.copyWithCount(amount);
            remaining.shrink(amount);
            pending.add(encodeReward(player, part, rarity.id()));
        }
        return true;
    }

    private static CompoundTag encodeReward(
            ServerPlayer player, ItemStack stack, String rarity) {
        CompoundTag entry = new CompoundTag();
        entry.put(
                STACK, stack.saveOptional(player.registryAccess()));
        entry.putString(RARITY, rarity);
        return entry;
    }

    private static boolean hasTicket(ServerPlayer player) {
        return player.getInventory().contains(
                new ItemStack(ModItems.LOTTERY_TICKET.get()));
    }

    private static void consumeTicket(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.LOTTERY_TICKET.get())) {
                stack.shrink(1);
                return;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(ModItems.LOTTERY_TICKET.get())) {
                stack.shrink(1);
                return;
            }
        }
    }

    private static boolean isActive(ServerPlayer player) {
        return OuterSystemData.equipped(player).isPresent()
                && OuterSystemData.type(player)
                        == OuterSystemType.DIVINE_GACHA;
    }

    private static long currentDay(ServerPlayer player) {
        return Math.floorDiv(
                player.serverLevel().getDayTime(), 24_000L);
    }

    private static void message(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    public enum RarityTier {
        ORDINARY("ordinary"),
        FINE("fine"),
        RARE("rare"),
        EPIC("epic"),
        LEGENDARY("legendary"),
        DIVINE("divine");

        private final String id;

        RarityTier(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public String translationKey() {
            return "rarity.immortal_cultivation_mod.gacha." + id;
        }
    }

    private record RewardOption(
            Supplier<? extends Item> item,
            int minimum,
            int maximum) {
        private ItemStack create(RandomSource random) {
            Item value = item.get();
            int count = minimum + random.nextInt(maximum - minimum + 1);
            return new ItemStack(
                    value,
                    Math.min(count, value.getDefaultMaxStackSize()));
        }
    }
}
