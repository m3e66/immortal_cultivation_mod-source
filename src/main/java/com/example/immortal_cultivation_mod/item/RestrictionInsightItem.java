package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.network.ModPayloads;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class RestrictionInsightItem extends Item {
    public static final int LEARNING_GOAL = 10_000;
    public static final int MAX_HINTS = 999;
    private static final String HINT_COUNT_TAG = "ImmortalCultivationRestrictionHints";

    public RestrictionInsightItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (player instanceof ServerPlayer serverPlayer
                && ModPayloads.beginRestrictionInsightLearning(serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, false);
        }
        return InteractionResultHolder.fail(stack);
    }

    public static int hintCount(Player player) {
        CompoundTag root = player.getPersistentData();
        if (root.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
            if (persisted.contains(HINT_COUNT_TAG, Tag.TAG_INT)) {
                return Math.max(0, persisted.getInt(HINT_COUNT_TAG));
            }
        }
        return Math.max(0, root.getInt(HINT_COUNT_TAG));
    }

    public static void addHint(Player player) {
        adjustHintCount(player, 1);
    }

    public static int setHintCount(Player player, int hints) {
        int clamped = Math.max(0, Math.min(MAX_HINTS, hints));
        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)
                ? root.getCompound(Player.PERSISTED_NBT_TAG)
                : new CompoundTag();
        persisted.putInt(HINT_COUNT_TAG, clamped);
        root.put(Player.PERSISTED_NBT_TAG, persisted);
        root.putInt(HINT_COUNT_TAG, clamped);
        return clamped;
    }

    public static int adjustHintCount(Player player, int delta) {
        long adjusted = (long) hintCount(player) + delta;
        return setHintCount(player, (int) Math.max(0L, Math.min(MAX_HINTS, adjusted)));
    }

    public static boolean consumeHint(Player player) {
        int hints = hintCount(player);
        if (hints <= 0) {
            return false;
        }
        setHintCount(player, hints - 1);
        return true;
    }

    public static boolean completeLearning(ServerPlayer player) {
        if (!player.getAbilities().instabuild && !consumeInsightItem(player)) {
            player.displayClientMessage(Component.translatable(
                    "message." + ImmortalCultivationMod.MODID + ".restriction_insight.missing"), true);
            return false;
        }
        addHint(player);
        player.displayClientMessage(Component.translatable(
                "message." + ImmortalCultivationMod.MODID + ".restriction_insight.learned",
                hintCount(player)), true);
        return true;
    }

    public static void copyHints(Player original, Player clone) {
        setHintCount(clone, hintCount(original));
    }

    private static boolean consumeInsightItem(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ModItems.RESTRICTION_INSIGHT.get())) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }
}
