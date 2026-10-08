package com.example.immortal_cultivation_mod.guidance;

import com.example.immortal_cultivation_mod.ImmortalCultivationMod;
import com.example.immortal_cultivation_mod.attachment.CultivationLevels;
import com.example.immortal_cultivation_mod.attachment.ModAttachments;
import com.example.immortal_cultivation_mod.flame.FlameVesselItem;
import com.example.immortal_cultivation_mod.flame.SpiritFlameEntity;
import com.example.immortal_cultivation_mod.hair.HairMenu;
import com.example.immortal_cultivation_mod.item.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import top.theillusivec4.curios.api.CuriosApi;

/** Bounded observations of existing gameplay. These do not perform tutorial actions. */
@EventBusSubscriber(modid = ImmortalCultivationMod.MODID)
public final class DaoGuidanceObservations {
    private DaoGuidanceObservations() {}

    public static void record(ServerPlayer player, String action) {
        DaoGuidanceSystem.record(player, "guide_" + action);
    }

    public static void notice(ServerPlayer player, String action) {
        String hint = switch (action) {
            case "guide_flame_seen" -> "new_flame";
            case "guide_special_forge" -> "forge_recorded";
            case "guide_restriction_cycle" -> "cycle_complete";
            case "guide_weapon_trial_started" -> "trial_coming";
            case "guide_core_failed" -> "core_failed";
            case "guide_core_ready" -> "core_ready";
            case "guide_control_pressure" -> "thought_resisting";
            case "guide_thought_restored" -> "thought_restoring";
            case "guide_life_started" -> "life_started";
            case "guide_life_choice" -> "life_choice";
            case "guide_life_settled" -> "life_finished";
            case "guide_reforge_ready" -> "reforge_ready";
            default -> "";
        };
        if (!hint.isBlank()) DaoGuidanceSystem.hint(player, hint);
        var accessory = switch (action) {
            case "guide_ring_triggered" -> ModItems.SHIELD_BREAKER_RING.get();
            case "guide_pendant_triggered" -> ModItems.BLOOD_FANG_PENDANT.get();
            case "guide_leaf_triggered" -> ModItems.ENLIGHTENMENT_LEAF.get();
            default -> null;
        };
        if (accessory != null) DaoGuidanceSystem.notice(player, "accessory_triggered", accessory.getDescription());
    }

    public static boolean observed(net.minecraft.nbt.CompoundTag state, String action, int target) {
        return action.startsWith("guide_") && state.getCompound("Observed").getInt(action) >= target;
    }

    public static void tick(ServerPlayer player) {
        List<ItemStack> items = new ArrayList<>(player.getInventory().items);
        items.addAll(player.getInventory().offhand);
        for (ItemStack stack : items) {
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof FlameVesselItem) {
                record(player, "vessel_obtained");
                if (FlameVesselItem.flame(stack) != null) {
                    record(player, "flame_seen");
                    DaoGuidanceSystem.discover(player, "giant_fireball");
                }
            }
            if (WeaponRestrictionSystem.isWeapon(stack)) DaoGuidanceSystem.discover(player, "weapon_growth");
            if (stack.getItem() instanceof com.example.immortal_cultivation_mod.alchemy.AlchemyPillItem
                    || stack.getItem() instanceof com.example.immortal_cultivation_mod.alchemy.LegacyPillItem)
                record(player, "pill_obtained");
            if (WeaponShardSystem.populated(stack)) record(player, "shard_obtained");
            if (WeaponRestrictionSystem.isEquipment(stack)) {
                if (WeaponRestrictionSystem.tierProgress(stack) == 72) record(player, "restriction_cycle");
                if (WeaponTribulation.active(stack)
                        && (stack == player.getMainHandItem() || stack == player.getOffhandItem()))
                    record(player, "weapon_trial_held");
            }
        }
        // Count only one source weapon's set; mixing unrelated shards never qualifies.
        items.stream().filter(WeaponShardSystem::populated).findFirst().ifPresent(first -> {
            var source = WeaponShardSystem.data(first).getUUID("source");
            List<ItemStack> set = items.stream().filter(s -> WeaponShardSystem.populated(s)
                    && source.equals(WeaponShardSystem.data(s).getUUID("source"))).toList();
            var payload = WeaponShardSystem.data(first);
            if (set.size() == payload.getInt("parts")
                    && set.stream().map(s -> WeaponShardSystem.data(s).getInt("index")).distinct().count() == set.size()
                    && set.stream().allMatch(s -> s.getCount() == 1 && WeaponShardSystem.data(s).getInt("parts") == payload.getInt("parts"))
                    && set.stream().allMatch(s -> payload.get("weapon").equals(WeaponShardSystem.data(s).get("weapon")))) {
                record(player, "shards_complete");
                List<ItemStack> withStones = new ArrayList<>(set);
                withStones.addAll(items.stream().filter(ForgingSystem::isRecipeSpiritStone).toList());
                if (WeaponShardSystem.canReforge(withStones)) record(player, "reforge_ready");
            }
        });
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            handler.getCurios().values().forEach(slots -> {
                for (int i = 0; i < slots.getStacks().getSlots(); i++) {
                    ItemStack stack = slots.getStacks().getStackInSlot(i);
                    String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
                    String action = switch (path) {
                        case "fortune_guard_coin" -> "coin_equipped";
                        case "shield_breaker_ring" -> "ring_equipped";
                        case "blood_fang_pendant" -> "pendant_equipped";
                        case "enlightenment_leaf" -> "leaf_equipped";
                        default -> "";
                    };
                    if (!action.isBlank()) {
                        DaoGuidanceSystem.discover(player, "accessories");
                        record(player, "accessory_equipped");
                        if (action.equals("coin_equipped")) record(player, action);
                    }
                }
            });
        });
        if (player.containerMenu instanceof HairMenu) record(player, "hair_opened");
        var data = ModAttachments.getData(player);
        if (data.cultivationLevel().equals(CultivationLevels.REALM_HALF_JINDAN)) {
            record(player, "core_failed");
            if (data.cultivationProgress() >= CultivationLevels.getTotalQiNeeded(data.cultivationLevel())
                    && data.qi() >= CultivationLevels.getLevelDef(data.cultivationLevel()).maxQi() + data.maxQiBonus())
                record(player, "core_ready");
        }
        if (player.tickCount % 40 == 0 && !player.serverLevel().getEntitiesOfClass(
                SpiritFlameEntity.class, player.getBoundingBox().inflate(8), e -> e.isAlive()
                        && player.hasLineOfSight(e)).isEmpty()) record(player, "flame_seen");
    }

    @SubscribeEvent
    public static void crafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getCrafting().getItem() instanceof FlameVesselItem) record(player, "vessel_obtained");
    }

    @SubscribeEvent
    public static void consumed(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player
                && (event.getItem().getItem() instanceof com.example.immortal_cultivation_mod.alchemy.LegacyPillItem
                || event.getItem().getItem() instanceof com.example.immortal_cultivation_mod.alchemy.AlchemyPillItem))
            record(player, "pill_used");
    }
}
