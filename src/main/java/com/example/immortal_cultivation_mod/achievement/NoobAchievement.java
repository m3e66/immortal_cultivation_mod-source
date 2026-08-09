package com.example.immortal_cultivation_mod.achievement;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.item.ModItems;
import java.util.Arrays;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

public final class NoobAchievement {
    public static final String HEAD_SLOT = "head_accessory";

    private static final String REWARD_TAG = "ImmortalCultivationNoobAchievement";
    private static final String DEATH_TIMES_TAG = "ImmortalCultivationNoobDeathTimes";
    private static final long DEATH_WINDOW_TICKS = 20L * 60L * 20L;
    private static final int REQUIRED_DEATHS = 10;
    private static final ResourceLocation ADVANCEMENT_ID =
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "you_are_really_bad");
    private static final ResourceLocation EXTRA_HAT_SLOT_ID =
            ResourceLocation.fromNamespaceAndPath(ImmortalCultivationMod.MODID, "noob_hat_slot");

    private NoobAchievement() {
    }

    public static boolean hasReward(Player player) {
        if (player.getPersistentData().getBoolean(REWARD_TAG)) {
            return true;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            AdvancementHolder advancement = serverPlayer.getServer().getAdvancements().get(ADVANCEMENT_ID);
            if (advancement != null
                    && serverPlayer.getAdvancements().getOrStartProgress(advancement).isDone()) {
                player.getPersistentData().putBoolean(REWARD_TAG, true);
                return true;
            }
        }
        return false;
    }

    public static void recordDeath(ServerPlayer player) {
        if (hasReward(player)) {
            ensureReward(player);
            return;
        }

        long now = player.getServer().overworld().getGameTime();
        long cutoff = now - DEATH_WINDOW_TICKS;
        long[] previous = player.getPersistentData().getLongArray(DEATH_TIMES_TAG);
        long[] recent = Arrays.stream(previous).filter(time -> time >= cutoff && time <= now).toArray();
        long[] updated = Arrays.copyOf(recent, recent.length + 1);
        updated[updated.length - 1] = now;
        player.getPersistentData().putLongArray(DEATH_TIMES_TAG, updated);

        if (updated.length < REQUIRED_DEATHS) {
            return;
        }

        player.getPersistentData().putBoolean(REWARD_TAG, true);
        player.getPersistentData().remove(DEATH_TIMES_TAG);
        AdvancementHolder advancement = player.getServer().getAdvancements().get(ADVANCEMENT_ID);
        if (advancement != null) {
            player.getAdvancements().award(advancement, "died_ten_times");
        }
        ensureReward(player);
    }

    public static void copyPersistentData(Player original, Player replacement) {
        if (original.getPersistentData().getBoolean(REWARD_TAG)) {
            replacement.getPersistentData().putBoolean(REWARD_TAG, true);
        }
        if (original.getPersistentData().contains(DEATH_TIMES_TAG)) {
            replacement.getPersistentData().putLongArray(
                    DEATH_TIMES_TAG, original.getPersistentData().getLongArray(DEATH_TIMES_TAG));
        }
    }

    public static void tick(ServerPlayer player) {
        if (player.tickCount % 20 == 0) {
            ensureReward(player);
        }
    }

    public static void ensureReward(ServerPlayer player) {
        if (!hasReward(player)) {
            return;
        }

        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            boolean hasExtraSlot = handler.getModifiers().get(HEAD_SLOT).stream()
                    .anyMatch(modifier -> EXTRA_HAT_SLOT_ID.equals(modifier.id()));
            if (!hasExtraSlot) {
                handler.addPermanentSlotModifier(
                        HEAD_SLOT, EXTRA_HAT_SLOT_ID, 1.0D, AttributeModifier.Operation.ADD_VALUE);
                handler.processSlots();
            }

            handler.getStacksHandler(HEAD_SLOT).ifPresent(stacksHandler -> {
                var stacks = stacksHandler.getStacks();
                for (int slot = 0; slot < stacks.getSlots(); slot++) {
                    if (stacks.getStackInSlot(slot).is(ModItems.NOOB_HAT.get())) {
                        return;
                    }
                }
                for (int slot = 0; slot < stacks.getSlots(); slot++) {
                    if (stacks.getStackInSlot(slot).isEmpty()) {
                        handler.setEquippedCurio(HEAD_SLOT, slot, new ItemStack(ModItems.NOOB_HAT.get()));
                        return;
                    }
                }
            });
        });
    }
}
