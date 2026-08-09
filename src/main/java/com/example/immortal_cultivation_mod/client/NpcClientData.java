package com.example.immortal_cultivation_mod.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public record NpcClientData(
        int entityId,
        boolean openMenu,
        String statusKey,
        String archetype,
        String name,
        String identity,
        String personality,
        String realm,
        String bodyType,
        String gender,
        int age,
        int reputation,
        int qi,
        int maxQi,
        int thoughts,
        int maxThoughts,
        long cultivationProgress,
        float health,
        float maxHealth,
        float attack,
        float armor,
        List<String> spiritRoots,
        String method,
        List<String> spells,
        List<String> playerSpells,
        String playerMethod,
        List<String> playerMethods,
        int friendship,
        int trust,
        int affection,
        int hatred,
        int praiseChance,
        int jokeChance,
        boolean canManageInventory,
        boolean canTeach,
        Map<String, Integer> spellLearningTrustRequirements,
        int methodLearningTrustRequired,
        String commandMode,
        String questId,
        int questState,
        String questRequiredItem,
        int questRequired,
        int questProgress,
        String questRewardItem,
        int questRewardCount,
        boolean questRewardAvailable,
        List<NpcTradeOffer> tradeOffers,
        List<String> family,
        List<NpcInventorySlot> inventory
) {
    public static NpcClientData from(CompoundTag tag) {
        List<NpcInventorySlot> slots = new ArrayList<>();
        ListTag inventoryTag = tag.getList("Inventory", 10);
        for (int i = 0; i < inventoryTag.size(); i++) {
            CompoundTag slot = inventoryTag.getCompound(i);
            slots.add(new NpcInventorySlot(
                    slot.getInt("Slot"),
                    slot.getString("Item"),
                    slot.getInt("Count")));
        }
        while (slots.size() < 27) {
            slots.add(new NpcInventorySlot(slots.size(), "", 0));
        }
        slots.sort(java.util.Comparator.comparingInt(NpcInventorySlot::slot));

        List<NpcTradeOffer> offers = new ArrayList<>();
        ListTag tradeTag = tag.getList("TradeOffers", 10);
        for (int i = 0; i < tradeTag.size(); i++) {
            CompoundTag offer = tradeTag.getCompound(i);
            offers.add(new NpcTradeOffer(
                    offer.getInt("OfferIndex"),
                    offer.getInt("InventorySlot"),
                    offer.getString("CurrencyItem"),
                    offer.getInt("Price"),
                    offer.getString("ResultItem"),
                    offer.getInt("ResultCount"),
                    offer.getInt("AvailableCount")));
        }

        Map<String, Integer> spellTrustRequirements = new LinkedHashMap<>();
        CompoundTag spellTrustTag = tag.getCompound("SpellLearningTrustRequirements");
        for (String spellId : spellTrustTag.getAllKeys()) {
            spellTrustRequirements.put(spellId, spellTrustTag.getInt(spellId));
        }

        return new NpcClientData(
                tag.getInt("EntityId"),
                tag.getBoolean("OpenMenu"),
                tag.getString("StatusKey"),
                tag.getString("Archetype"),
                tag.getString("Name"),
                tag.getString("Identity"),
                tag.getString("Personality"),
                tag.getString("Realm"),
                tag.getString("BodyType"),
                tag.getString("Gender"),
                tag.getInt("Age"),
                tag.getInt("Reputation"),
                tag.getInt("Qi"),
                tag.getInt("MaxQi"),
                tag.getInt("Thoughts"),
                tag.getInt("MaxThoughts"),
                tag.getLong("CultivationProgress"),
                tag.getFloat("Health"),
                tag.getFloat("MaxHealth"),
                tag.getFloat("Attack"),
                tag.getFloat("Armor"),
                strings(tag.getList("SpiritRoots", 8)),
                tag.getString("Method"),
                strings(tag.getList("Spells", 8)),
                strings(tag.getList("PlayerSpells", 8)),
                tag.getString("PlayerMethod"),
                strings(tag.getList("PlayerMethods", 8)),
                tag.getInt("Friendship"),
                tag.getInt("Trust"),
                tag.getInt("Affection"),
                tag.getInt("Hatred"),
                tag.getInt("PraiseChance"),
                tag.getInt("JokeChance"),
                tag.getBoolean("CanManageInventory"),
                tag.getBoolean("CanTeach"),
                Map.copyOf(spellTrustRequirements),
                tag.getInt("MethodLearningTrustRequired"),
                tag.getString("CommandMode"),
                tag.getString("QuestId"),
                tag.getInt("QuestState"),
                tag.getString("QuestRequiredItem"),
                tag.getInt("QuestRequired"),
                tag.getInt("QuestProgress"),
                tag.getString("QuestRewardItem"),
                tag.getInt("QuestRewardCount"),
                tag.getBoolean("QuestRewardAvailable"),
                List.copyOf(offers),
                strings(tag.getList("Family", 8)),
                List.copyOf(slots));
    }

    public NpcClientData withoutStatus() {
        if (statusKey == null || statusKey.isBlank()) {
            return this;
        }
        return new NpcClientData(entityId, openMenu, "", archetype, name, identity, personality, realm, bodyType, gender,
                age, reputation, qi, maxQi, thoughts, maxThoughts, cultivationProgress, health, maxHealth, attack, armor, spiritRoots,
                method, spells, playerSpells, playerMethod, playerMethods, friendship, trust, affection, hatred,
                praiseChance, jokeChance,
                canManageInventory, canTeach, spellLearningTrustRequirements, methodLearningTrustRequired,
                commandMode, questId, questState, questRequiredItem, questRequired,
                questProgress, questRewardItem, questRewardCount, questRewardAvailable, tradeOffers, family, inventory);
    }

    private static List<String> strings(ListTag tag) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < tag.size(); i++) {
            values.add(tag.getString(i));
        }
        return List.copyOf(values);
    }

    public record NpcInventorySlot(int slot, String itemId, int count) {
        public boolean empty() {
            return itemId == null || itemId.isBlank() || count <= 0;
        }
    }

    public record NpcTradeOffer(int offerIndex, int inventorySlot, String currencyItemId, int price,
                                String resultItemId, int resultCount, int availableCount) {
    }
}
