package com.example.immortal_cultivation_mod.item;

import com.example.immortal_cultivation_mod.attachment.CultivationMethods;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.event.ServerEvents;
import com.example.immortal_cultivation_mod.spell.UndeadControl;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** Shared behavior for the five cultivation accessories that use vanilla Curios slots. */
public final class CultivationAccessoryItem extends Item implements ICurioItem {
    public static final String LIVING_CORPSE_TAG = "icmLivingCorpsePlayer";
    private static final int AUTO_FEED_INTERVAL = 20;
    private static final int AUTO_FEED_BLOOD_PER_SECOND = 6_400;
    private static final int BLOOD_ITEMS_PER_CRYSTAL = 9;
    private static final int BLOOD_CRYSTAL_GRUDGE = 5;
    private static final Set<UUID> ALLOWED_LIVING_CORPSE_HEALING = new HashSet<>();

    private final Kind kind;

    public CultivationAccessoryItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return kind.slot.equals(slotContext.identifier());
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (kind != Kind.WATER_AVOIDING_PEARL
                || !(slotContext.entity() instanceof ServerPlayer player)
                || player.tickCount % 20 != 0
                || !player.isUnderWater()) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING,
                30, 0, true, false, true));
        int nextDamage = stack.getDamageValue() + 1;
        if (nextDamage >= stack.getMaxDamage()) {
            stack.shrink(1);
        } else {
            stack.setDamageValue(nextDamage);
        }
    }

    public static boolean isEquipped(LivingEntity entity, Item item) {
        return CuriosApi.getCuriosInventory(entity)
                .map(handler -> handler.isEquipped(item))
                .orElse(false);
    }

    public static boolean hasShieldBreaker(LivingEntity entity) {
        return entity != null && isEquipped(entity, ModItems.SHIELD_BREAKER_RING.get());
    }

    public static int proficiencyMultiplier(ServerPlayer player) {
        boolean equipped = isEquipped(player, ModItems.ENLIGHTENMENT_LEAF.get());
        if (equipped) { com.example.immortal_cultivation_mod.guidance.DaoGuidanceObservations.record(player, "leaf_triggered"); }
        return equipped ? 2 : 1;
    }

    public static boolean isLivingCorpse(LivingEntity entity) {
        return entity != null && entity.getPersistentData().getBoolean(LIVING_CORPSE_TAG);
    }

    public static boolean tryBecomeLivingCorpse(ServerPlayer player) {
        if (isLivingCorpse(player)) {
            return true;
        }
        Optional<SlotResult> pearl = CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(ModItems.SOUL_ANCHOR_PEARL.get()));
        if (pearl.isEmpty()) {
            return false;
        }
        pearl.get().stack().shrink(1);
        player.getPersistentData().putBoolean(LIVING_CORPSE_TAG, true);
        UndeadControl.initializeLivingCorpsePlayer(player);
        player.setHealth(Math.max(1.0F, Math.min(player.getMaxHealth(), player.getHealth())));
        ServerEvents.syncPlayerData(player);
        return true;
    }

    public static boolean hasSoulAnchorPearl(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(ModItems.SOUL_ANCHOR_PEARL.get()))
                .isPresent();
    }

    public static void clearLivingCorpse(ServerPlayer player) {
        UndeadControl.clearLivingCorpsePlayerAbilities(player);
        player.getPersistentData().remove(LIVING_CORPSE_TAG);
    }

    public static boolean blocksHealing(ServerPlayer player) {
        return isLivingCorpse(player)
                && !ALLOWED_LIVING_CORPSE_HEALING.contains(player.getUUID());
    }

    public static void healLivingCorpseFromUndeadAbility(ServerPlayer player, float amount) {
        ALLOWED_LIVING_CORPSE_HEALING.add(player.getUUID());
        try {
            player.heal(amount);
        } finally {
            ALLOWED_LIVING_CORPSE_HEALING.remove(player.getUUID());
        }
    }

    public static boolean consumeBloodItem(ServerPlayer player, ItemStack stack,
                                           int grudgeOrBloodPerItem, boolean shiftAll) {
        if (stack.isEmpty() || grudgeOrBloodPerItem <= 0) {
            return false;
        }
        if (isLivingCorpse(player)) {
            if (UndeadControl.feedLivingCorpsePlayer(
                    player, stack, grudgeOrBloodPerItem, shiftAll)) {
                return true;
            }
            return stack.is(ModItems.BLOOD.get())
                    && consumeCompressedHeldBlood(player, stack, shiftAll);
        }
        var data = ModAttachments.getData(player);
        if (!CultivationMethods.isBloodDemon(data.activeCultivationMethod())) {
            return false;
        }
        int maxBlood = ServerEvents.getMaxBlood(player);
        if (data.blood() >= maxBlood) {
            return false;
        }
        int requested = shiftAll ? stack.getCount() : 1;
        int perItem = Math.max(1, grudgeOrBloodPerItem);
        int usable = Math.min(requested,
                Math.max(1, (maxBlood - data.blood() + perItem - 1) / perItem));
        int gained = Math.min(maxBlood - data.blood(), usable * perItem);
        ModAttachments.setData(player, data.withBlood(data.blood() + gained));
        if (!player.getAbilities().instabuild) {
            stack.shrink(usable);
        }
        ServerEvents.syncPlayerData(player);
        return true;
    }

    public static void tickPlayer(ServerPlayer player) {
        if (player.tickCount % AUTO_FEED_INTERVAL != 0
                || !isEquipped(player, ModItems.BLOOD_FANG_PENDANT.get())) {
            return;
        }
        if (isLivingCorpse(player)) {
            autoFeedLivingCorpse(player);
            return;
        }

        var data = ModAttachments.getData(player);
        if (!CultivationMethods.isBloodDemon(data.activeCultivationMethod())) {
            return;
        }

        int capacity = Math.max(0, ServerEvents.getMaxBlood(player) - data.blood());
        int budget = Math.min(AUTO_FEED_BLOOD_PER_SECOND, capacity);
        int gained = consumeInventoryBloodValue(player, ModItems.BLOOD.get(), 1, budget);
        gained += consumeInventoryBloodValue(
                player, ModItems.BLOOD_CRYSTAL.get(), 5, budget - gained);
        if (gained > 0) {
            ModAttachments.setData(player, data.withBlood(data.blood() + gained));
            com.example.immortal_cultivation_mod.guidance.DaoGuidanceObservations.record(player, "pendant_triggered");
            ServerEvents.syncPlayerData(player);
        }
    }

    private static void autoFeedLivingCorpse(ServerPlayer player) {
        long currentGrudge = UndeadControl.grudge(player);
        int bloodBudget = (int) Math.min(
                AUTO_FEED_BLOOD_PER_SECOND, Math.max(0L, 1_000L - currentGrudge));
        int gained = consumeInventoryBloodValue(
                player, ModItems.BLOOD.get(), 1, bloodBudget);
        int remaining = AUTO_FEED_BLOOD_PER_SECOND - gained;
        gained += consumeCompressedInventoryBlood(player, remaining);
        remaining = AUTO_FEED_BLOOD_PER_SECOND - gained;
        remaining -= remaining % BLOOD_CRYSTAL_GRUDGE;
        gained += consumeInventoryBloodValue(
                player, ModItems.BLOOD_CRYSTAL.get(), BLOOD_CRYSTAL_GRUDGE, remaining);
        if (gained > 0) {
            UndeadControl.addLivingCorpseGrudge(player, gained);
            com.example.immortal_cultivation_mod.guidance.DaoGuidanceObservations.record(player, "pendant_triggered");
        }
    }

    private static boolean consumeCompressedHeldBlood(
            ServerPlayer player, ItemStack stack, boolean shiftAll) {
        int availableCrystals = stack.getCount() / BLOOD_ITEMS_PER_CRYSTAL;
        int crystalsToConsume = shiftAll ? availableCrystals : Math.min(1, availableCrystals);
        if (crystalsToConsume <= 0) {
            return false;
        }
        if (!UndeadControl.addLivingCorpseGrudge(
                player, (long) crystalsToConsume * BLOOD_CRYSTAL_GRUDGE)) {
            return false;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(crystalsToConsume * BLOOD_ITEMS_PER_CRYSTAL);
        }
        return true;
    }

    private static int consumeCompressedInventoryBlood(ServerPlayer player, int valueLimit) {
        int maxCrystals = valueLimit / BLOOD_CRYSTAL_GRUDGE;
        if (maxCrystals <= 0) {
            return 0;
        }

        int availableBlood = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ModItems.BLOOD.get())) {
                availableBlood += stack.getCount();
            }
        }
        int crystalsToConsume = Math.min(
                maxCrystals, availableBlood / BLOOD_ITEMS_PER_CRYSTAL);
        if (crystalsToConsume <= 0) {
            return 0;
        }

        int bloodToConsume = crystalsToConsume * BLOOD_ITEMS_PER_CRYSTAL;
        if (!player.getAbilities().instabuild) {
            for (int slot = 0;
                 slot < player.getInventory().getContainerSize() && bloodToConsume > 0;
                 slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (!stack.is(ModItems.BLOOD.get())) {
                    continue;
                }
                int amount = Math.min(stack.getCount(), bloodToConsume);
                stack.shrink(amount);
                bloodToConsume -= amount;
            }
        }
        return crystalsToConsume * BLOOD_CRYSTAL_GRUDGE;
    }

    private static int consumeInventoryBloodValue(
            ServerPlayer player, Item item, int valuePerItem, int valueLimit) {
        if (valueLimit <= 0 || valuePerItem <= 0) {
            return 0;
        }
        int itemsNeeded = (valueLimit + valuePerItem - 1) / valuePerItem;
        int selected = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(item)) {
                continue;
            }
            int amount = Math.min(stack.getCount(), itemsNeeded - selected);
            if (amount <= 0) {
                break;
            }
            selected += amount;
            if (!player.getAbilities().instabuild) {
                stack.shrink(amount);
            }
            if (selected >= itemsNeeded) {
                break;
            }
        }
        return Math.min(valueLimit, selected * valuePerItem);
    }

    public enum Kind {
        WATER_AVOIDING_PEARL("charm"),
        SOUL_ANCHOR_PEARL("charm"),
        SHIELD_BREAKER_RING("ring"),
        BLOOD_FANG_PENDANT("necklace"),
        ENLIGHTENMENT_LEAF("charm");

        private final String slot;

        Kind(String slot) {
            this.slot = slot;
        }
    }
}
