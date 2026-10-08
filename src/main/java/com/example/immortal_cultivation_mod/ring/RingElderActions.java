package com.example.immortal_cultivation_mod.ring;

import com.example.immortal_cultivation_mod.magicritual.item.ModItems;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

/** Bounded world actions run only on the server after session, ownership and enabled-state checks. */
final class RingElderActions {
    static final long TEN_MINUTES = 20L * 60 * 10;
    private RingElderActions() { }

    static JsonObject context(RingElderProfiles profiles, UUID ring, UUID player, long tick) {
        CompoundTag bond = profiles.relationship(ring, player);
        JsonObject facts = new JsonObject();
        facts.addProperty("trust", bond.getInt("Trust"));
        facts.addProperty("rescueRefusalSecondsRemaining", Math.max(0, bond.getLong("RescueRefusedUntil") - tick) / 20);
        facts.addProperty("giftEligible", canGift(profiles, ring, player, tick));
        facts.addProperty("giftCooldownSecondsRemaining", Math.max(0, bond.getLong("NextChatGiftTick") - tick) / 20);
        facts.addProperty("onlyGiftItem", "immortal_cultivation_mod:spirit_stone");
        facts.addProperty("lastActualOutcome", bond.getString("LastActionOutcome"));
        return facts;
    }

    static boolean canGift(RingElderProfiles profiles, UUID ring, UUID player, long tick) {
        CompoundTag bond = profiles.relationship(ring, player);
        return bond.getInt("Trust") >= 10 && tick >= bond.getLong("NextChatGiftTick")
                && !profiles.rescueRefused(ring, player, tick)
                && profiles.profile(ring).getCompound("GiftStock").getInt("spirit_stone") > 0;
    }

    static String apply(ServerPlayer player, UUID ring, RingElderDecision.Action action) {
        RingElderProfiles profiles = RingElderProfiles.get(player.server);
        long tick = player.server.overworld().getGameTime();
        CompoundTag bond = profiles.relationship(ring, player.getUUID());
        String outcome = "";
        if (action == RingElderDecision.Action.REFUSE_RESCUE) {
            if (!profiles.rescueRefused(ring, player.getUUID(), tick)) {
                bond.putLong("RescueRefusedUntil", tick + TEN_MINUTES);
                bond.putInt("Trust", Math.max(0, bond.getInt("Trust") - 15));
                bond.putInt("Offenses", bond.getInt("Offenses") + 1);
                outcome = "老者动怒：十分钟内不再出手救你。";
            } else outcome = "老者仍在生气，拒绝救援的时限未延长。";
        } else if (action == RingElderDecision.Action.GIFT) {
            if (!canGift(profiles, ring, player.getUUID(), tick)) outcome = "老者此次未赠物，需缓和关系或等候片刻。";
            else if (!hasRoom(player, new ItemStack(ModItems.SPIRIT_STONE.get()), 6)) outcome = "背包已满，此次未领取赠物。";
            else {
                int count = profiles.reserveGift(ring, "spirit_stone", 5);
                if (count > 0) {
                    ItemStack gift = new ItemStack(ModItems.SPIRIT_STONE.get(), count);
                    player.getInventory().add(gift);
                    profiles.delivered(ring, "spirit_stone", count - gift.getCount());
                    player.inventoryMenu.broadcastChanges();
                    bond.putLong("NextChatGiftTick", tick + TEN_MINUTES);
                    bond.putInt("ChatGifts", bond.getInt("ChatGifts") + 1);
                    outcome = "已收到老者赠予的灵石：" + (count - gift.getCount()) + "颗。";
                } else outcome = "老者的灵石存量已尽。";
            }
        }
        if (!outcome.isEmpty()) {
            bond.putString("LastActionOutcome", outcome);
            profiles.rememberLasting(ring, player.getUUID(), outcome);
            profiles.setDirty();
        }
        return outcome;
    }

    private static boolean hasRoom(ServerPlayer player, ItemStack gift, int count) {
        int room = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) room += gift.getMaxStackSize();
            else if (ItemStack.isSameItemSameComponents(stack, gift)) room += Math.max(0, stack.getMaxStackSize() - stack.getCount());
            if (room >= count) return true;
        }
        return false;
    }
}
